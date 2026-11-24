package com.nikhil.payment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A payment transaction persisted in PostgreSQL.
 *
 * <p>{@code externalReference} is a client-supplied idempotency key.
 * Submitting the same value twice is treated as a duplicate and is
 * rejected with HTTP 409 instead of creating a second transaction.</p>
 */
@Entity
@Table(name = "transactions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_transactions_external_reference",
                columnNames = "external_reference")
})
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Client-supplied idempotency key, e.g. an order / payment reference. */
    @Column(name = "external_reference", unique = true, length = 128)
    private String externalReference;

    @Column(name = "account_id", nullable = false, length = 128)
    private String accountId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "USD";

    @Column(name = "merchant_id", length = 128)
    private String merchantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private FraudDecision.Outcome status = FraudDecision.Outcome.PENDING;

    /** Human-readable reasons for the decision, joined with "; ". */
    @Column(name = "decision_reasons", columnDefinition = "TEXT")
    private String decisionReasons;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Transaction() {
        // JPA
    }

    public Transaction(String externalReference, String accountId,
                       BigDecimal amount, String currency, String merchantId) {
        this.externalReference = externalReference;
        this.accountId = accountId;
        this.amount = amount;
        this.currency = currency == null || currency.isBlank() ? "USD" : currency;
        this.merchantId = merchantId;
        this.status = FraudDecision.Outcome.PENDING;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public FraudDecision.Outcome getStatus() {
        return status;
    }

    public void setStatus(FraudDecision.Outcome status) {
        this.status = status;
    }

    public String getDecisionReasons() {
        return decisionReasons;
    }

    public void setDecisionReasons(String decisionReasons) {
        this.decisionReasons = decisionReasons;
    }

    /** Store the decision and its reasons on this transaction. */
    public void applyDecision(FraudDecision decision) {
        this.status = decision.outcome();
        this.decisionReasons = decision.reasons() == null
                ? null : String.join("; ", decision.reasons());
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
