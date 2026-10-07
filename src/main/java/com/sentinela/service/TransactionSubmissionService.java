package com.sentinela.service;

import com.sentinela.dto.TransactionAccepted;
import com.sentinela.dto.TransactionRequest;
import com.sentinela.persistence.OutboxEvent;
import com.sentinela.persistence.OutboxEventRepository;
import com.sentinela.persistence.TransactionRecord;
import com.sentinela.persistence.TransactionRecordRepository;
import com.sentinela.persistence.TransactionStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.CONFLICT;

@Service
@Profile("api")
public class TransactionSubmissionService {

    private final TransactionRecordRepository transactions;
    private final OutboxEventRepository outbox;
    private final TransactionTemplate transactionTemplate;

    public TransactionSubmissionService(TransactionRecordRepository transactions, OutboxEventRepository outbox,
                                        TransactionTemplate transactionTemplate) {
        this.transactions = transactions;
        this.outbox = outbox;
        this.transactionTemplate = transactionTemplate;
    }

    public TransactionAccepted submit(String clientId, String idempotencyKey, TransactionRequest request) {
        try {
            return transactionTemplate.execute(status -> submitInTransaction(clientId, idempotencyKey, request));
        } catch (org.springframework.dao.DataIntegrityViolationException concurrentRequest) {
            TransactionRecord winner = transactions.findByClientIdAndIdempotencyKey(clientId, idempotencyKey).orElse(null);
            if (winner != null && winner.hasSameRequest(request)) {
                return new TransactionAccepted(winner.transactionId(), winner.status());
            }
            throw new ResponseStatusException(CONFLICT,
                    "The transaction or idempotency key has already been submitted.", concurrentRequest);
        }
    }

    private TransactionAccepted submitInTransaction(String clientId, String idempotencyKey, TransactionRequest request) {
        TransactionRecord previous = transactions.findByClientIdAndIdempotencyKey(clientId, idempotencyKey).orElse(null);
        if (previous != null) {
            if (!previous.hasSameRequest(request)) {
                throw new ResponseStatusException(CONFLICT, "Idempotency-Key was already used for a different request.");
            }
            return new TransactionAccepted(previous.transactionId(), previous.status());
        }

        TransactionRecord transactionWithSameId = transactions.findByTransactionId(request.transactionId()).orElse(null);
        if (transactionWithSameId != null) {
            if (transactionWithSameId.idempotencyKey().equals(idempotencyKey)
                    && transactionWithSameId.hasSameRequest(request)) {
                return new TransactionAccepted(transactionWithSameId.transactionId(), transactionWithSameId.status());
            }
            throw new ResponseStatusException(CONFLICT, "transactionId was already submitted with a different idempotency key.");
        }

        TransactionRecord record = transactions.saveAndFlush(new TransactionRecord(clientId, idempotencyKey, request));
        outbox.save(new OutboxEvent(record.transactionId()));
        return new TransactionAccepted(record.transactionId(), TransactionStatus.PENDING);
    }
}
