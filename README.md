# Real-Time Payment and Fraud Detection Platform

> Working portfolio implementation rebuilt from the resume description; not the original production code.

## What it does

A payment-processing system where users submit transactions and the system evaluates them for fraud in real time. It is built as a cloud-native, event-driven backend using Java, Spring Boot, Kafka, Redis, PostgreSQL, Docker, and AWS.

Flow: `POST /transactions` → validate → persist as PENDING in PostgreSQL → publish an event to the Kafka `transactions` topic → evaluate fraud synchronously with rule + Redis velocity checks → store the decision (APPROVED / REVIEW / REJECTED) with reasons → return it to the caller. A Kafka consumer also processes each event (idempotently — already-decided transactions are skipped on re-delivery).

## Tech Stack

| Area | Technology |
| --- | --- |
| Language | Java 17 |
| Framework | Spring Boot 3.3.5 |
| Messaging / Events | Kafka (spring-kafka) |
| Cache / Fast lookups | Redis |
| Database | PostgreSQL 16 |
| Containers | Docker / Docker Compose |
| Cloud | AWS (deployment sketch only — nothing is deployed) |
| CI/CD | GitHub Actions |
| Orchestration | Kubernetes |

## Architecture Overview

```
Submit transaction
  -> REST API (Spring Boot)
  -> PostgreSQL (persist as PENDING)
  -> Kafka event (topic: transactions)
  -> Fraud evaluation service (rules + Redis velocity)
  -> Redis (velocity counter, 60s TTL; decision cache, 1h TTL)
  -> PostgreSQL (persist decision)
  -> Decision returned to the caller / via GET /transactions/{id}
```

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for components, event flow, the data model, and an AWS deployment sketch.

## Project Structure

```
.
├── Dockerfile
├── docker-compose.yml
├── .env.example
├── README.md
├── docs/
│   ├── ARCHITECTURE.md
│   └── SETUP.md
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/nikhil/payment/
    │   │   ├── PaymentFraudApplication.java
    │   │   ├── TransactionController.java
    │   │   ├── dto/
    │   │   │   ├── TransactionRequest.java
    │   │   │   └── TransactionResponse.java
    │   │   ├── kafka/
    │   │   │   ├── TransactionProducer.java
    │   │   │   └── TransactionConsumer.java
    │   │   ├── model/
    │   │   │   ├── Transaction.java
    │   │   │   └── FraudDecision.java
    │   │   ├── repository/
    │   │   │   └── TransactionRepository.java
    │   │   └── service/
    │   │       ├── FraudEvaluationService.java
    │   │       └── TransactionService.java
    │   └── resources/
    │       └── application.yml
    └── test/
        └── java/com/nikhil/payment/
            └── FraudEvaluationServiceTest.java
```

## Run Locally with Docker Compose

Prerequisites: Docker and Docker Compose. No AWS account or deployment is needed.

```bash
docker compose up --build
```

This starts PostgreSQL 16, Redis 7, Kafka (single node, KRaft mode), and builds and runs the app on port `8080`.

Check it is up, then submit a transaction:

```bash
curl -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{"accountId": "acct-123", "amount": 100.00, "currency": "USD", "merchantId": "shop-1", "externalReference": "order-001"}'
```

Example response (201):

```json
{
  "id": 1,
  "externalReference": "order-001",
  "accountId": "acct-123",
  "amount": 100.00,
  "currency": "USD",
  "merchantId": "shop-1",
  "status": "APPROVED",
  "reasons": ["No illustrative rule triggered"],
  "createdAt": "2026-11-24T07:00:00Z"
}
```

Look it up, and list recent transactions:

```bash
curl http://localhost:8080/transactions/1
curl "http://localhost:8080/transactions?limit=20"
```

Stop everything:

```bash
docker compose down
# docker compose down -v  # also removes the local PostgreSQL volume
```

To run the app outside Docker instead (dependencies still via Compose, app on the host), see [docs/SETUP.md](docs/SETUP.md).

## API Endpoints

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/transactions` | Submit a transaction for real-time fraud evaluation. Returns 201 with the decision. Duplicate `externalReference` returns 409. Missing `accountId` / `amount` returns 400. |
| `GET` | `/transactions/{id}` | Retrieve a transaction by id, with its decision. Unknown id returns 404. |
| `GET` | `/transactions?limit=20` | List the most recent transactions, newest first (default 20, max 100). |

## Fraud Rules — Illustrative Only

These are simple, explainable portfolio rules, **not production fraud rules**, and no accuracy or latency claim is made:

- Amount is null or `<= 0` → **REJECTED** ("Amount must be greater than zero")
- Amount `> 10000` → **REVIEW** ("Amount exceeds 10000 review threshold")
- More than 3 transactions from the same account in 60 seconds (Redis counter, 60s TTL) → **REVIEW**
- Otherwise → **APPROVED**

The Kafka consumer is idempotent: re-delivered events for already-decided transactions are logged and skipped.

## Tests

```bash
mvn test
# or, if Maven is not installed locally:
./mvnw test
```

Tests are JUnit 5 + Mockito unit tests for `FraudEvaluationService` (10 tests: approve / review / reject boundaries, velocity counter TTL behaviour). They mock Redis — no PostgreSQL, Kafka, or Redis server is needed to run them.

## Environment Variables

| Variable | Description | Default / Example |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/payments` |
| `SPRING_DATASOURCE_USERNAME` | PostgreSQL username | `payments` |
| `SPRING_DATASOURCE_PASSWORD` | PostgreSQL password | Set via `.env` — do not commit secrets |
| `SPRING_DATA_REDIS_HOST` | Redis host | `localhost` |
| `SPRING_DATA_REDIS_PORT` | Redis port | `6379` |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap servers | `localhost:9092` (host) / `kafka:29092` (in Docker) |
| `SERVER_PORT` | Application port | `8080` |

Copy `.env.example` to `.env` for local development, and keep real credentials out of version control. See `.gitignore`.

## Limitations

- Fraud rules are illustrative only, not production rules.
- No production deployment, transaction volumes, accuracy, or latency numbers are claimed. Nothing in this repo deploys to AWS — the AWS notes are a deployment sketch in `docs/ARCHITECTURE.md`.
- Local PostgreSQL schema is created by Hibernate (`ddl-auto: update`); for production, use migrations and `ddl-auto: validate`.

## Author

Nikhil M — Software Engineer. MS Computer Science, University of Texas at Arlington (2023–2025). Experience in Java, Spring Boot, Kafka, AWS, microservices, and CI/CD with GitHub Actions, Docker, and Kubernetes at JPMorgan Chase and ACS Solutions.
