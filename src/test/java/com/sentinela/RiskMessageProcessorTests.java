package com.sentinela;

import com.sentinela.dto.TransactionRequest;
import com.sentinela.persistence.OutboxEvent;
import com.sentinela.persistence.OutboxEventRepository;
import com.sentinela.persistence.TransactionRecord;
import com.sentinela.persistence.TransactionRecordRepository;
import com.sentinela.persistence.TransactionStatus;
import com.sentinela.service.RiskMessageProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles({"worker", "test"})
class RiskMessageProcessorTests {

    @Autowired
    private RiskMessageProcessor processor;

    @Autowired
    private TransactionRecordRepository transactions;

    @Autowired
    private OutboxEventRepository outbox;

    @BeforeEach
    void clearDatabase() {
        outbox.deleteAll();
        transactions.deleteAll();
    }

    @Test
    void processesMessageOnceAndIgnoresRedelivery() {
        TransactionRequest request = new TransactionRequest("TXN100", "USER100", new BigDecimal("9500.00"),
                "NG", "192.168.1.100", 5, 2);
        transactions.saveAndFlush(new TransactionRecord("key-100", request));
        outbox.save(new OutboxEvent("TXN100"));

        assertTrue(processor.process("TXN100"));
        assertFalse(processor.process("TXN100"));

        TransactionRecord result = transactions.findByTransactionId("TXN100").orElseThrow();
        assertEquals(TransactionStatus.COMPLETED, result.status());
        assertEquals(100, result.riskScore());
        assertEquals("BLOCK", result.recommendedAction());
        assertEquals(4, result.triggeredRules().size());
    }

    @Test
    void concurrentRedeliveryCompletesTransactionOnlyOnce() throws Exception {
        TransactionRequest request = new TransactionRequest("TXN101", "USER101", new BigDecimal("9500.00"),
                "NG", null, 5, 2);
        transactions.saveAndFlush(new TransactionRecord("key-101", request));
        int callers = 2;
        CountDownLatch ready = new CountDownLatch(callers);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(callers)) {
            List<java.util.concurrent.Future<Boolean>> results = new ArrayList<>();
            for (int index = 0; index < callers; index++) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting for concurrent start.");
                    }
                    return processor.process("TXN101");
                }));
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            int processedCount = 0;
            for (var result : results) {
                if (result.get(10, TimeUnit.SECONDS)) {
                    processedCount++;
                }
            }
            assertEquals(1, processedCount);
        }
        assertEquals(TransactionStatus.COMPLETED,
                transactions.findByTransactionId("TXN101").orElseThrow().status());
    }
}
