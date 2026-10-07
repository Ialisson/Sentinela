package com.sentinela.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    interface ClaimedOutboxEvent {
        UUID getEventId();
        String getTransactionId();
    }

    long countByPublishedAtIsNull();

    @Transactional
    @Query(value = "WITH picked AS (SELECT id FROM outbox_events WHERE published_at IS NULL " +
            "AND (locked_until IS NULL OR locked_until < CURRENT_TIMESTAMP) " +
            "ORDER BY created_at LIMIT :batchSize FOR UPDATE SKIP LOCKED) " +
            "UPDATE outbox_events e SET locked_until = :leaseUntil FROM picked " +
            "WHERE e.id = picked.id RETURNING e.id AS eventId, e.transaction_id AS transactionId",
            nativeQuery = true)
    List<ClaimedOutboxEvent> claimPending(@Param("leaseUntil") java.time.Instant leaseUntil,
                                         @Param("batchSize") int batchSize);
}
