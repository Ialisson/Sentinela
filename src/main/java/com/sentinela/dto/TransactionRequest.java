package com.sentinela.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TransactionRequest(
        @NotBlank @Size(max = 100) @Pattern(regexp = "[A-Za-z0-9._:-]+") String transactionId,
        @NotBlank @Size(max = 100) String userId,
        @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal amount,
        @NotBlank @Pattern(regexp = "[A-Za-z]{2}", message = "must be a two-letter country code") String country,
        @Size(max = 45) String ipAddress,
        @NotNull @Min(1) @jakarta.validation.constraints.Max(10000) Integer cardAttempts,
        @Min(0) Integer emailAgeDays
) {
}
