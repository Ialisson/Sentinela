package com.sentinela.service;

import com.sentinela.persistence.OutboxEvent;
import com.sentinela.persistence.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OutboxClaimService {
    public record ClaimedEvent(UUID id, String transactionId) { }

    private final OutboxEventRepository outbox;

    public OutboxClaimService(OutboxEventRepository outbox) {
        this.outbox = outbox;
    }

    @Transactional
    public List<ClaimedEvent> claimBatch(int batchSize, Instant leaseUntil) {
        return outbox.claimPending(leaseUntil, batchSize).stream()
                .map(event -> new ClaimedEvent(event.getEventId(), event.getTransactionId())).toList();
    }

    @Transactional
    public void markPublished(UUID eventId) {
        outbox.findById(eventId).ifPresent(OutboxEvent::markPublished);
    }

    @Transactional
    public void releaseClaim(UUID eventId) {
        outbox.findById(eventId).ifPresent(OutboxEvent::releaseClaim);
    }
}
