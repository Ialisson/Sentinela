package com.sentinela.messaging;

import com.sentinela.persistence.OutboxEvent;
import com.sentinela.persistence.OutboxEventRepository;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Component
@Profile("api")
@ConditionalOnProperty(name = "sentinela.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {

    private final OutboxEventRepository outbox;
    private final RabbitTemplate rabbitTemplate;

    public OutboxPublisher(OutboxEventRepository outbox, RabbitTemplate rabbitTemplate, MeterRegistry meterRegistry) {
        this.outbox = outbox;
        this.rabbitTemplate = rabbitTemplate;
        Gauge.builder("sentinela.outbox.pending", outbox, OutboxEventRepository::countByPublishedAtIsNull)
                .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${sentinela.outbox.delay-ms:1000}")
    @Transactional
    public void publishPending() throws Exception {
        for (OutboxEvent event : outbox.lockPending(PageRequest.of(0, 50))) {
            CorrelationData confirmation = new CorrelationData(event.id().toString());
            rabbitTemplate.convertAndSend(RabbitTopology.EXCHANGE, RabbitTopology.ROUTING_KEY,
                    event.transactionId(), confirmation);
            CorrelationData.Confirm confirm = confirmation.getFuture().get(5, TimeUnit.SECONDS);
            if (!confirm.ack() || confirmation.getReturned() != null) {
                throw new IllegalStateException("RabbitMQ did not route outbox event " + event.id());
            }
            event.markPublished();
        }
    }
}
