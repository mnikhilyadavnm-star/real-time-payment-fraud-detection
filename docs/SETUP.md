# Setup

## Prerequisites

- Java 17+
- Docker and Docker Compose
- Maven — `mvn` on your PATH, or the Maven Wrapper `./mvnw` if present in the repo

## 1. Run everything with Docker Compose (recommended)

```bash
docker compose up --build
```

This starts, for local development only:

- PostgreSQL 16 on port `5432`
- Redis 7 on port `6379`
- Kafka (single node, KRaft mode) on ports `9092` (host) / `29092` (Docker network)
- The app, built from `Dockerfile`, on port `8080`

Check they are running:

```bash
docker compose ps
docker compose logs -f app
```

## 2. Or run the app on the host (dependencies via Compose)

```bash
docker compose up -d postgres redis kafka
mvn spring-boot:run
# or ./mvnw spring-boot:run
```

The application listens on port `8080` by default (see `src/main/resources/application.yml`).

To run with a different port:

```bash
SERVER_PORT=8081 mvn spring-boot:run
```

## 3. Try it

Submit a transaction — returns 201 with the decision:

```bash
curl -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{"accountId": "acct-123", "amount": 100.00, "currency": "USD", "merchantId": "shop-1", "externalReference": "order-001"}'
```

Try the illustrative rules:

```bash
# High amount -> REVIEW
curl -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{"accountId": "acct-123", "amount": 15000, "currency": "USD", "externalReference": "order-002"}'

# Invalid amount -> REJECTED
curl -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{"accountId": "acct-123", "amount": 0, "currency": "USD", "externalReference": "order-003"}'

# Duplicate externalReference -> 409
curl -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{"accountId": "acct-123", "amount": 10, "currency": "USD", "externalReference": "order-001"}'
```

Look up a transaction, and list recent ones:

```bash
curl http://localhost:8080/transactions/1
curl "http://localhost:8080/transactions?limit=20"
```

## 4. Run the tests

```bash
mvn test
# or ./mvnw test
```

Unit tests (`FraudEvaluationServiceTest`, 10 tests) mock Redis — no PostgreSQL, Kafka, or Redis server is needed to run them.

## Stop

```bash
docker compose down
```

Use `docker compose down -v` to also remove the local PostgreSQL volume.
