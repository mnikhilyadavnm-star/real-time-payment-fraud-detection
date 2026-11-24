package com.nikhil.payment;

import com.nikhil.payment.model.FraudDecision;
import com.nikhil.payment.service.FraudEvaluationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the illustrative fraud rules in {@link FraudEvaluationService}.
 * Redis is mocked — no Kafka, PostgreSQL, or Redis server is needed.
 */
@ExtendWith(MockitoExtension.class)
class FraudEvaluationServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private FraudEvaluationService service;

    @BeforeEach
    void setUp() {
        // No Redis stubbing here: the pure-rule tests never touch Redis,
        // and Mockito's strict stubs would fail on an unused stub.
        service = new FraudEvaluationService(redisTemplate);
    }

    @Test
    void normalAmountWithNoVelocityIsApproved() {
        FraudDecision decision = service.evaluate(new BigDecimal("100.00"), 1);

        assertEquals(FraudDecision.Outcome.APPROVED, decision.outcome());
        assertTrue(decision.reasons().stream().anyMatch(r -> r.contains("No illustrative rule")));
    }

    @Test
    void amountExactlyAtThresholdIsApproved() {
        FraudDecision decision = service.evaluate(new BigDecimal("10000"), 1);

        assertEquals(FraudDecision.Outcome.APPROVED, decision.outcome());
    }

    @Test
    void amountOverThresholdIsReview() {
        FraudDecision decision = service.evaluate(new BigDecimal("10000.01"), 1);

        assertEquals(FraudDecision.Outcome.REVIEW, decision.outcome());
        assertTrue(decision.reasons().stream().anyMatch(r -> r.contains("10000")));
    }

    @Test
    void zeroAmountIsRejected() {
        FraudDecision decision = service.evaluate(BigDecimal.ZERO, 1);

        assertEquals(FraudDecision.Outcome.REJECTED, decision.outcome());
        assertTrue(decision.reasons().stream().anyMatch(r -> r.contains("greater than zero")));
    }

    @Test
    void negativeAmountIsRejected() {
        FraudDecision decision = service.evaluate(new BigDecimal("-5.00"), 1);

        assertEquals(FraudDecision.Outcome.REJECTED, decision.outcome());
    }

    @Test
    void nullAmountIsRejected() {
        FraudDecision decision = service.evaluate(null, 1);

        assertEquals(FraudDecision.Outcome.REJECTED, decision.outcome());
    }

    @Test
    void moreThanThreeTransactionsInSixtySecondsIsReview() {
        FraudDecision decision = service.evaluate(new BigDecimal("50.00"), 4);

        assertEquals(FraudDecision.Outcome.REVIEW, decision.outcome());
        assertTrue(decision.reasons().stream().anyMatch(r -> r.contains("3 transactions")));
    }

    @Test
    void exactlyThreeTransactionsInSixtySecondsIsApproved() {
        FraudDecision decision = service.evaluate(new BigDecimal("50.00"), 3);

        assertEquals(FraudDecision.Outcome.APPROVED, decision.outcome());
    }

    @Test
    void velocityCounterIncrementsAndSetsTtlOnFirstHit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        long count = service.incrementVelocityCount("acct-123");

        assertEquals(1L, count);
        verify(valueOperations).increment("velocity:account:acct-123");
        verify(redisTemplate).expire("velocity:account:acct-123",
                java.time.Duration.ofSeconds(60));
    }

    @Test
    void velocityCounterReturnsCountWithoutResettingTtlOnLaterHits() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(4L);

        long count = service.incrementVelocityCount("acct-123");

        assertEquals(4L, count);
        verify(valueOperations).increment("velocity:account:acct-123");
    }
}
