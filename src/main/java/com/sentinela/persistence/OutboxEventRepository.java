package com.sentinela.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    long countByPublishedAtIsNull();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from OutboxEvent e where e.publishedAt is null order by e.createdAt")
    List<OutboxEvent> lockPending(Pageable pageable);
}
