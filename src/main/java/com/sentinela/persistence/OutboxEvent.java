package com.sentinela.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events", indexes = @Index(name = "idx_outbox_pending", columnList = "published_at,created_at"))
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transaction_id", nullable = false, unique = true, length = 100)
    private String transactionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected OutboxEvent() {
    }

    public OutboxEvent(String transactionId) {
        this.transactionId = transactionId;
        this.createdAt = Instant.now();
    }

    public UUID id() { return id; }
    public String transactionId() { return transactionId; }
    public boolean published() { return publishedAt != null; }
    public void markPublished() { publishedAt = Instant.now(); }
}
