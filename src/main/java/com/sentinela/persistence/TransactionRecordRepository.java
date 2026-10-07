package com.sentinela.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, UUID> {
    Optional<TransactionRecord> findByTransactionId(String transactionId);
    Optional<TransactionRecord> findByIdempotencyKey(String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TransactionRecord t where t.transactionId = :transactionId")
    Optional<TransactionRecord> lockByTransactionId(@Param("transactionId") String transactionId);
}
