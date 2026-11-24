package com.nikhil.payment.model;

import java.util.List;

/**
 * Result of fraud evaluation for one transaction.
 *
 * <p>These are ILLUSTRATIVE portfolio rules only — not production fraud
 * rules. They are deliberately simple and explainable so a reader can see
 * how a rule + velocity check combine into a decision. No accuracy,
 * latency, or production-effectiveness claim is made.</p>
 */
public record FraudDecision(Outcome outcome, List<String> reasons) {

    public enum Outcome {
        /** Transaction looks fine under the illustrative rules. */
        APPROVED,
        /** Needs a human / further review under the illustrative rules. */
        REVIEW,
        /** Rejected under the illustrative rules (e.g. invalid amount). */
        REJECTED,
        /** Saved before evaluation has run. */
        PENDING
    }

    public static FraudDecision approved() {
        return new FraudDecision(Outcome.APPROVED, List.of("No illustrative rule triggered"));
    }

    public static FraudDecision approved(String reason) {
        return new FraudDecision(Outcome.APPROVED, List.of(reason));
    }

    public static FraudDecision review(List<String> reasons) {
        return new FraudDecision(Outcome.REVIEW, List.copyOf(reasons));
    }

    public static FraudDecision rejected(List<String> reasons) {
        return new FraudDecision(Outcome.REJECTED, List.copyOf(reasons));
    }
}
