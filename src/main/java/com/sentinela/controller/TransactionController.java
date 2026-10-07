package com.sentinela.controller;

import com.sentinela.dto.TransactionAccepted;
import com.sentinela.dto.TransactionRequest;
import com.sentinela.dto.TransactionStatusResponse;
import com.sentinela.persistence.TransactionRecordRepository;
import com.sentinela.service.TransactionSubmissionService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.net.URI;
import java.security.Principal;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/v2/transactions")
@SecurityRequirement(name = "basicAuth")
@Profile("api")
public class TransactionController {

    private final TransactionSubmissionService submissionService;
    private final TransactionRecordRepository transactions;

    public TransactionController(TransactionSubmissionService submissionService,
                                 TransactionRecordRepository transactions) {
        this.submissionService = submissionService;
        this.transactions = transactions;
    }

    @PostMapping
    @Operation(summary = "Submit a transaction for asynchronous risk analysis")
    @ApiResponse(responseCode = "202", description = "Transaction accepted for analysis",
            content = @Content(schema = @Schema(implementation = TransactionAccepted.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request or idempotency key")
    @ApiResponse(responseCode = "409", description = "Idempotency key conflicts with another request")
    public ResponseEntity<TransactionAccepted> submit(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TransactionRequest request,
            Principal principal) {
        if (idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Idempotency-Key must contain between 1 and 100 characters.");
        }
        TransactionAccepted accepted = submissionService.submit(principal.getName(), idempotencyKey, request);
        return ResponseEntity.accepted()
                .location(URI.create("/api/v2/transactions/" + accepted.transactionId()))
                .body(accepted);
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Get the risk analysis status of a transaction")
    @ApiResponse(responseCode = "200", description = "Transaction status",
            content = @Content(schema = @Schema(implementation = TransactionStatusResponse.class)))
    @ApiResponse(responseCode = "404", description = "Transaction not found for this client")
    public TransactionStatusResponse status(@PathVariable String transactionId, Principal principal) {
        return transactions.findByTransactionIdAndClientId(transactionId, principal.getName())
                .map(TransactionStatusResponse::from)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Transaction not found."));
    }
}
