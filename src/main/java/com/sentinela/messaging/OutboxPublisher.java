package com.sentinela.messaging;

import com.sentinela.service.OutboxClaimService;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Profile("api")
@ConditionalOnProperty(name = "sentinela.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {

    private static final int BATCH_SIZE = 10;
    private static final Duration CLAIM_LEASE = Duration.ofMinutes(2);

    private final OutboxClaimService claims;
    private final RabbitTemplate rabbitTemplate;

    public OutboxPublisher(OutboxClaimService claims, RabbitTemplate rabbitTemplate, MeterRegistry meterRegistry,
                           com.sentinela.persistence.OutboxEventRepository outbox) {
        this.claims = claims;
        this.rabbitTemplate = rabbitTemplate;
        Gauge.builder("sentinela.outbox.pending", outbox, com.sentinela.persistence.OutboxEventRepository::countByPublishedAtIsNull)
                .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${sentinela.outbox.delay-ms:1000}")
    public void publishPending() throws Exception {
        List<OutboxClaimService.ClaimedEvent> events = claims.claimBatch(BATCH_SIZE, Instant.now().plus(CLAIM_LEASE));
        for (int index = 0; index < events.size(); index++) {
            OutboxClaimService.ClaimedEvent event = events.get(index);
            try {
                CorrelationData confirmation = new CorrelationData(event.id().toString());
                rabbitTemplate.convertAndSend(RabbitTopology.EXCHANGE, RabbitTopology.ROUTING_KEY,
                        event.transactionId(), confirmation);
                CorrelationData.Confirm confirm = confirmation.getFuture().get(5, TimeUnit.SECONDS);
                if (!confirm.ack() || confirmation.getReturned() != null) {
                    throw new IllegalStateException("RabbitMQ did not route outbox event " + event.id());
                }
                claims.markPublished(event.id());
            } catch (Exception exception) {
                for (int remaining = index; remaining < events.size(); remaining++) {
                    claims.releaseClaim(events.get(remaining).id());
                }
                throw exception;
            }
        }
    }
}
