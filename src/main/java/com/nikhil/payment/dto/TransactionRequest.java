package com.nikhil.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request body for {@code POST /transactions}.
 *
 * <p>Note: {@code amount} is {@code @NotNull} only — not {@code @Positive}.
 * An amount of zero or less is a business decision (REJECTED by the
 * illustrative fraud rules), not a 400 validation error, so it must reach
 * the service layer to be recorded.</p>
 */
public record TransactionRequest(
        @NotBlank(message = "accountId is required") String accountId,
        @NotNull(message = "amount is required") BigDecimal amount,
        String currency,
        String merchantId,
        /** Optional client-supplied idempotency key. Duplicates are rejected (409). */
        String externalReference
) {
}
