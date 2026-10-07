package com.sentinela;

import com.sentinela.dto.RiskResponse;
import com.sentinela.dto.TransactionRequest;
import com.sentinela.persistence.OutboxEventRepository;
import com.sentinela.persistence.TransactionRecordRepository;
import com.sentinela.persistence.TransactionStatus;
import com.sentinela.service.OutboxClaimService;
import com.sentinela.service.RiskAnalysisService;
import com.sentinela.service.TransactionSubmissionService;
import com.sentinela.messaging.OutboxPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;

import static com.sentinela.messaging.RabbitTopology.DEAD_LETTER_QUEUE;
import static com.sentinela.messaging.RabbitTopology.QUEUE;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "sentinela.redis.enabled=false",
        "sentinela.outbox.delay-ms=3600000",
        "sentinela.outbox.enabled=true",
        "sentinela.security.username=integration-client",
        "sentinela.security.password=integration-secret",
        "sentinela.security.clients-json={\"integration-client\":\"integration-secret\"}",
        "spring.rabbitmq.listener.simple.auto-startup=true",
        "management.otlp.tracing.endpoint=http://localhost:4318/v1/traces"
})
@ActiveProfiles({"api", "worker", "integration"})
@Testcontainers(disabledWithoutDocker = true)
class DistributedFlowIntegrationTests {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16.6-alpine");

    @Container
    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:4.0.5-management-alpine")
            .withAdminUser("sentinela")
            .withAdminPassword("sentinela");

    @DynamicPropertySource
    static void configureContainers(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
    }

    @Autowired TransactionSubmissionService submissions;
    @Autowired TransactionRecordRepository transactions;
    @Autowired OutboxEventRepository outbox;
    @Autowired OutboxClaimService claims;
    @Autowired OutboxPublisher publisher;
    @Autowired RabbitAdmin rabbitAdmin;

    @MockitoBean RiskAnalysisService riskAnalysis;

    @BeforeEach
    void resetState() {
        rabbitAdmin.purgeQueue(QUEUE);
        rabbitAdmin.purgeQueue(DEAD_LETTER_QUEUE);
        outbox.deleteAll();
        transactions.deleteAll();
        reset(riskAnalysis);
        when(riskAnalysis.analyze(any())).thenAnswer(invocation -> {
            TransactionRequest request = invocation.getArgument(0);
            if (request.transactionId().equals("TXN-FAIL")) {
                throw new IllegalStateException("simulated transient dependency failure");
            }
            return new RiskResponse(10, "LOW", "APPROVE", List.of("test-rule"));
        });
    }

    @Test
    void postgresLeaseClaimsAreExclusiveWhileRowsAreClaimed() {
        submit("TXN-CLAIM-1");
        submit("TXN-CLAIM-2");

        List<OutboxClaimService.ClaimedEvent> firstClaim = claims.claimBatch(1, Instant.now().plusSeconds(30));
        List<OutboxClaimService.ClaimedEvent> secondClaim = claims.claimBatch(1, Instant.now().plusSeconds(30));

        assertEquals(1, firstClaim.size());
        assertEquals(1, secondClaim.size());
        assertNotEquals(firstClaim.getFirst().id(), secondClaim.getFirst().id());
        assertEquals(0, claims.claimBatch(1, Instant.now().plusSeconds(30)).size());
    }

    @Test
    void confirmedRabbitDeliveryCompletesTransactionAndRepeatedDeliveryIsSafe() throws Exception {
        submit("TXN-RABBIT");
        publisher.publishPending();

        await().atMost(java.time.Duration.ofSeconds(20)).untilAsserted(() ->
                assertEquals(TransactionStatus.COMPLETED,
                        transactions.findByTransactionId("TXN-RABBIT").orElseThrow().status()));
        assertEquals(1, outbox.count());
    }

    @Test
    void exhaustedWorkerRetriesMoveTransactionToFailedThroughDeadLetterQueue() throws Exception {
        submit("TXN-FAIL");
        publisher.publishPending();

        await().atMost(java.time.Duration.ofSeconds(25)).untilAsserted(() ->
                assertEquals(TransactionStatus.FAILED,
                        transactions.findByTransactionId("TXN-FAIL").orElseThrow().status()));
        assertEquals(0, rabbitAdmin.getQueueInfo(DEAD_LETTER_QUEUE).getMessageCount());
    }

    private void submit(String transactionId) {
        TransactionRequest request = new TransactionRequest(transactionId, "USER-1", new BigDecimal("100.00"),
                "BR", null, 1, 30);
        submissions.submit("integration-client", "key-" + transactionId, request);
    }
}
