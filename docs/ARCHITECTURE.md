# Architecture

## Components

- **API service (Spring Boot, Java 17)** — receives transaction submissions over REST, persists them, publishes transaction events, and serves transaction lookups. Entry point: `PaymentFraudApplication`, controller: `TransactionController`.
- **TransactionService** — submit flow: duplicate check on `externalReference` (409) → save as PENDING → publish Kafka event → evaluate fraud synchronously → save decision → cache decision in Redis.
- **FraudEvaluationService** — illustrative rule + Redis velocity evaluation (see below).
- **Kafka** — event backbone. Transaction submissions are published to topic `transactions` (`TransactionProducer`); `TransactionConsumer` (group `fraud-detection`) logs/processes each event idempotently.
- **Redis** — velocity counter `velocity:account:{accountId}` with a 60-second TTL, and decision cache `decision:transaction:{id}` with a 1-hour TTL. Fast lookups only; PostgreSQL is the system of record.
- **PostgreSQL** — persistent storage for transactions and their evaluation decisions (JPA entity `Transaction`, repository `TransactionRepository`).
- **Docker** — local development and packaging (`Dockerfile` multi-stage Maven build, `docker-compose.yml`).
- **AWS** — cloud deployment target (see deployment sketch below). Nothing is deployed from this repo.

## Event Flow with Kafka

```
Client
  -> POST /transactions (TransactionController)
  -> TransactionService.submit()
     -> TransactionRepository.save() as PENDING (PostgreSQL)
     -> TransactionProducer.publish() to Kafka topic `transactions`
     -> FraudEvaluationService.evaluateTransaction()
        -> Redis INCR velocity:account:{accountId} (60s TTL)
        -> rule evaluation (amount / velocity)
     -> TransactionRepository.save() with decision (PostgreSQL)
     -> Redis SET decision:transaction:{id} (1h TTL)
  -> 201 + TransactionResponse (decision + reasons)
  -> TransactionConsumer (group fraud-detection) receives the event,
     logs it, and skips it if the transaction is already decided
  -> GET /transactions/{id} reads the decision from PostgreSQL
```

## Fraud Evaluation — Illustrative Rules Only

Not production rules, and no accuracy or latency claims are made.

1. **Amount check** — null or `<= 0` → REJECTED. `> 10000` → REVIEW.
2. **Redis velocity check** — `INCR velocity:account:{accountId}`; the key is created with a 60-second TTL on first use. If the count (including this transaction) is `> 3` → REVIEW.
3. **Decision** — REJECTED wins over REVIEW; REVIEW wins over APPROVED. Reasons are stored with the transaction (`decisionReasons`) and returned in the API response.

If Redis is unavailable, the velocity check logs a warning and returns a count of 1, so a Redis outage does not block submission.

## Data Model

**Transaction** (`transactions` table)
- `id` — Long, generated, primary key
- `external_reference` — unique, client-supplied idempotency key (409 on duplicate)
- `account_id` — String, required
- `amount` — BigDecimal (19,2), required
- `currency` — String, default `USD`
- `merchant_id` — String, optional
- `status` — APPROVED / REVIEW / REJECTED / PENDING
- `decision_reasons` — TEXT, reasons joined with `"; "`
- `created_at` — Instant

**FraudDecision** — a record (`outcome`, `reasons`), not a table. `Outcome` is `APPROVED | REVIEW | REJECTED | PENDING`.

PostgreSQL stores transactions and decisions. Redis stores short-lived velocity counters and decision cache only.

## AWS Deployment Sketch

- Package the Spring Boot service as a Docker image (`Dockerfile`).
- Run the service on AWS container infrastructure (for example, ECS / EKS — Kubernetes experience is listed on the resume; the specific target is a sketch, not a deployed setup).
- Use managed Kafka, Redis, and PostgreSQL services where available, or self-managed equivalents in Docker for local development.
- Use GitHub Actions for CI/CD, as listed in the author's experience.
- No production deployment is claimed for this repository, and nothing here provisions AWS resources. Secrets, networking, scaling, and observability configuration are out of scope.
