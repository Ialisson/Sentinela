package com.sentinela.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record TransactionRequest(
        @NotBlank String transactionId,
        @NotBlank String userId,
        @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal amount,
        @NotBlank @Pattern(regexp = "[A-Za-z]{2}", message = "must be a two-letter country code") String country,
        String ipAddress,
        @NotNull @Min(1) Integer cardAttempts,
        @Min(0) Integer emailAgeDays
) {
}
