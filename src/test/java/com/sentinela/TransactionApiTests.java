package com.sentinela;

import com.sentinela.persistence.OutboxEventRepository;
import com.sentinela.persistence.TransactionRecordRepository;
import com.sentinela.dto.TransactionRequest;
import com.sentinela.service.TransactionSubmissionService;
import com.sentinela.service.DeadLetterFailureHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"api", "test"})
@WithMockUser(username = "test-client", roles = "CLIENT")
class TransactionApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransactionRecordRepository transactions;

    @Autowired
    private OutboxEventRepository outbox;

    @Autowired
    private TransactionSubmissionService submissionService;

    @Autowired
    private DeadLetterFailureHandler deadLetterFailureHandler;

    @BeforeEach
    void clearDatabase() {
        outbox.deleteAll();
        transactions.deleteAll();
    }

    @Test
    void acceptsTransactionAndPersistsOutboxAtomically() throws Exception {
        mockMvc.perform(post("/api/v2/transactions")
                        .header("Idempotency-Key", "key-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("TXN001")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.transactionId").value("TXN001"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(get("/api/v2/transactions/TXN001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        org.junit.jupiter.api.Assertions.assertEquals(1, transactions.count());
        org.junit.jupiter.api.Assertions.assertEquals(1, outbox.count());
    }

    @Test
    void deadLetterMovesPendingTransactionToTerminalFailedState() throws Exception {
        mockMvc.perform(post("/api/v2/transactions")
                .header("Idempotency-Key", "key-dlq")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequest("TXN-DLQ"))).andExpect(status().isAccepted());

        deadLetterFailureHandler.markFailed("TXN-DLQ");

        mockMvc.perform(get("/api/v2/transactions/TXN-DLQ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").exists());
    }

    @Test
    void idempotentRetryReturnsExistingTransactionWithoutAddingAnotherOutboxEvent() throws Exception {
        mockMvc.perform(post("/api/v2/transactions")
                .header("Idempotency-Key", "key-002")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequest("TXN002"))).andExpect(status().isAccepted());

        mockMvc.perform(post("/api/v2/transactions")
                        .header("Idempotency-Key", "key-002")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("TXN002")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.transactionId").value("TXN002"));

        org.junit.jupiter.api.Assertions.assertEquals(1, transactions.count());
        org.junit.jupiter.api.Assertions.assertEquals(1, outbox.count());
    }

    @Test
    void rejectsInvalidPayloadAndConflictingUseOfIdempotencyKey() throws Exception {
        mockMvc.perform(post("/api/v2/transactions")
                        .header("Idempotency-Key", "key-003")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transactionId\":\"TXN003\",\"amount\":-10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists());

        mockMvc.perform(post("/api/v2/transactions")
                .header("Idempotency-Key", "key-004")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequest("TXN004"))).andExpect(status().isAccepted());

        mockMvc.perform(post("/api/v2/transactions")
                        .header("Idempotency-Key", "key-004")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("TXN005")))
                .andExpect(status().isConflict());
    }

    @Test
    void concurrentRetriesWithSameIdempotencyKeyCreateOneTransaction() throws Exception {
        TransactionRequest request = new TransactionRequest("TXN006", "USER123", new BigDecimal("9500.00"),
                "NG", null, 5, 2);
        int callers = 8;
        CountDownLatch ready = new CountDownLatch(callers);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(callers)) {
            List<java.util.concurrent.Future<com.sentinela.dto.TransactionAccepted>> submissions = new ArrayList<>();
            for (int index = 0; index < callers; index++) {
                submissions.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting for concurrent start.");
                    }
                    return submissionService.submit("test-client", "key-006", request);
                }));
            }
            org.junit.jupiter.api.Assertions.assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            for (var submission : submissions) {
                org.junit.jupiter.api.Assertions.assertEquals("TXN006", submission.get(10, TimeUnit.SECONDS).transactionId());
            }
        }
        org.junit.jupiter.api.Assertions.assertEquals(1, transactions.count());
        org.junit.jupiter.api.Assertions.assertEquals(1, outbox.count());
    }

    @Test
    @WithAnonymousUser
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v2/transactions/TXN001"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithAnonymousUser
    void acceptsConfiguredBasicAuthentication() throws Exception {
        mockMvc.perform(post("/api/v2/transactions")
                        .with(httpBasic("test-client", "test-secret"))
                        .header("Idempotency-Key", "basic-auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("TXN-BASIC")))
                .andExpect(status().isAccepted());
    }

    @Test
    void transactionsAreScopedToTheAuthenticatedClient() {
        TransactionRequest request = new TransactionRequest("TXN-TENANT", "USER123", new BigDecimal("100.00"),
                "BR", null, 1, 30);
        submissionService.submit("test-client", "same-key", request);

        org.junit.jupiter.api.Assertions.assertTrue(
                transactions.findByTransactionIdAndClientId("TXN-TENANT", "another-client").isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(
                transactions.findByClientIdAndIdempotencyKey("another-client", "same-key").isEmpty());
    }

    @Test
    @WithMockUser(username = "other-client", roles = "CLIENT")
    void anotherAuthenticatedClientCannotReadTheTransaction() throws Exception {
        submissionService.submit("test-client", "owner-only", new TransactionRequest("TXN-PRIVATE", "USER123",
                new BigDecimal("100.00"), "BR", null, 1, 30));

        mockMvc.perform(get("/api/v2/transactions/TXN-PRIVATE"))
                .andExpect(status().isNotFound());
    }

    @Test
    void publishesOpenApiContractForAuthenticatedTransactionEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.paths['/api/v2/transactions'].post.responses['202']").exists())
                .andExpect(jsonPath("$.paths['/api/v2/transactions/{transactionId}'].get.responses['200']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.basicAuth.type").value("http"))
                .andExpect(jsonPath("$.paths['/api/v2/transactions'].post.security[0].basicAuth").exists());
    }

    private String validRequest(String transactionId) {
        return """
                {
                  "transactionId": "%s",
                  "userId": "USER123",
                  "amount": 9500.00,
                  "country": "NG",
                  "ipAddress": "192.168.1.100",
                  "cardAttempts": 5,
                  "emailAgeDays": 2
                }
                """.formatted(transactionId);
    }
}
