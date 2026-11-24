package com.nikhil.payment.service;

import com.nikhil.payment.model.FraudDecision;
import com.nikhil.payment.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Illustrative, rule-based fraud evaluation.
 *
 * <p><b>These are NOT production fraud rules.</b> They are simple,
 * explainable portfolio rules, chosen so the behaviour is easy to
 * demonstrate and test:</p>
 * <ul>
 *   <li>amount is null or {@code <= 0} → REJECTED</li>
 *   <li>amount {@code > 10,000} → REVIEW ("high amount")</li>
 *   <li>more than 3 transactions from the same account in 60 seconds
 *       (Redis counter) → REVIEW ("velocity")</li>
 *   <li>otherwise → APPROVED</li>
 * </ul>
 * <p>Velocity is tracked in Redis with a 60-second TTL so the counter
 * resets automatically; Redis is never the system of record — PostgreSQL is.</p>
 */
@Service
public class FraudEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(FraudEvaluationService.class);

    /** Illustrative high-amount review threshold (currency units, e.g. USD). */
    public static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("10000");

    /** Illustrative velocity limit: more than this many in the window → REVIEW. */
    public static final long VELOCITY_LIMIT = 3;

    /** Velocity window in seconds. */
    public static final long VELOCITY_WINDOW_SECONDS = 60;

    private final StringRedisTemplate redisTemplate;

    public FraudEvaluationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Pure rule evaluation — no Redis, no I/O. This is what the unit tests
     * exercise directly.
     *
     * @param amount               transaction amount, may be null
     * @param recentTransactionCount transactions from the same account
     *                             in the last 60 seconds, including this one
     */
    public FraudDecision evaluate(BigDecimal amount, long recentTransactionCount) {
        if (amount == null) {
            return FraudDecision.rejected(List.of("Amount is required"));
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return FraudDecision.rejected(
                    List.of("Amount must be greater than zero"));
        }
        List<String> reviewReasons = new ArrayList<>();
        if (amount.compareTo(HIGH_AMOUNT_THRESHOLD) > 0) {
            reviewReasons.add("Amount exceeds 10000 review threshold");
        }
        if (recentTransactionCount > VELOCITY_LIMIT) {
            reviewReasons.add("More than 3 transactions from this account in 60 seconds");
        }
        if (!reviewReasons.isEmpty()) {
            return FraudDecision.review(reviewReasons);
        }
        return FraudDecision.approved();
    }

    /**
     * Increment the per-account Redis velocity counter for the last
     * 60 seconds and return the new count (including this transaction).
     * The key expires after the window, so the count resets automatically.
     * If Redis is unavailable, log and return 1 so a Redis outage does
     * not block transaction submission.
     */
    public long incrementVelocityCount(String accountId) {
        String key = "velocity:account:" + accountId;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, Duration.ofSeconds(VELOCITY_WINDOW_SECONDS));
            }
            return count == null ? 1L : count;
        } catch (Exception e) {
            log.warn("Redis velocity check unavailable for account {}: {}", accountId, e.getMessage());
            return 1L;
        }
    }

    /** Evaluate a transaction, using Redis for the velocity check. */
    public FraudDecision evaluateTransaction(Transaction transaction) {
        long recentCount = incrementVelocityCount(transaction.getAccountId());
        return evaluate(transaction.getAmount(), recentCount);
    }

    /** Cache a decision in Redis for fast lookup (1 hour TTL). */
    public void cacheDecision(Transaction transaction) {
        if (transaction.getId() == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(
                    "decision:transaction:" + transaction.getId(),
                    transaction.getStatus().name(),
                    Duration.ofHours(1));
        } catch (Exception e) {
            log.warn("Could not cache decision for transaction {}: {}",
                    transaction.getId(), e.getMessage());
        }
    }
}
