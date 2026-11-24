# Setup

## Prerequisites

- Java 17+
- Docker and Docker Compose
- Maven (the project includes the Maven Wrapper, `./mvnw`, so a local Maven install is optional)

## 1. Start dependencies

```bash
docker compose up -d
```

This starts, for local development only:

- PostgreSQL 16 on port `5432`
- Redis 7 on port `6379`
- Kafka (single node, KRaft mode) on port `9092`

Check they are running:

```bash
docker compose ps
```

## 2. Run the application

```bash
./mvnw spring-boot:run
```

The application listens on port `8080` by default (see `src/main/resources/application.yml`).

To run with a different port:

```bash
SERVER_PORT=8081 ./mvnw spring-boot:run
```

## 3. Test steps

1. Confirm the app starts without errors.
2. Confirm PostgreSQL, Redis, and Kafka are reachable from the app (see `docker compose ps` and the app logs).
3. Submit a transaction (starter endpoint — currently returns `501 Not Implemented`):

```bash
curl -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{"amount": 100.00, "currency": "USD"}'
```

4. Look up a transaction (starter endpoint — currently returns `501 Not Implemented`):

```bash
curl http://localhost:8080/transactions/1
```

5. Run the build / tests when tests are added:

```bash
./mvnw test
```

Note: no tests are included in this starter skeleton yet.

## Stop

```bash
docker compose down
```

Use `docker compose down -v` to also remove the local PostgreSQL volume.
