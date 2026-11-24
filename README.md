# Real-Time Payment and Fraud Detection Platform

> Portfolio documentation and starter skeleton based on the resume description; not the original production code.

## What it does

A payment-processing system where users submit transactions and the system evaluates them for fraud in real time. It is built as a cloud-native, event-driven backend using Java, Spring Boot, Kafka, Redis, PostgreSQL, Docker, and AWS.

## Tech Stack

| Area | Technology |
| --- | --- |
| Language | Java |
| Framework | Spring Boot |
| Messaging / Events | Kafka |
| Cache / Fast lookups | Redis |
| Database | PostgreSQL |
| Containers | Docker |
| Cloud | AWS |
| CI/CD | GitHub Actions |
| Orchestration | Kubernetes |

## Architecture Overview

```
Submit transaction
  -> REST API (Spring Boot)
  -> Kafka event
  -> Fraud evaluation service
  -> Redis lookups (velocity / recent-activity checks)
  -> PostgreSQL persistence
  -> Decision (returned to the caller / stored with the transaction)
```

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for components, event flow, the data model, and an AWS deployment sketch.

## Project Structure

```
.
├── README.md
├── docs/
│   ├── ARCHITECTURE.md
│   └── SETUP.md
├── docker-compose.yml
├── pom.xml
└── src/
    └── main/
        ├── java/com/nikhil/payment/
        │   ├── PaymentFraudApplication.java
        │   └── TransactionController.java
        └── resources/
            └── application.yml
```

## Local Setup with Docker Compose

Prerequisites: Java 17+, Docker, and Maven. Full steps are in [docs/SETUP.md](docs/SETUP.md).

```bash
docker compose up -d
```

This starts PostgreSQL, Redis, and Kafka (single-node, KRaft mode, for local development), plus a placeholder app service.

Then run the application:

```bash
./mvnw spring-boot:run
```

The API listens on port `8080` by default.

## API Endpoints

Starter API design — endpoints are stubs in this skeleton and return `501 Not Implemented` until implemented.

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/transactions` | Submit a transaction for real-time fraud evaluation. |
| `GET` | `/transactions/{id}` | Retrieve a transaction by ID. |

Example submit request (shape is illustrative):

```bash
curl -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{"amount": 100.00, "currency": "USD"}'
```

## Environment Variables

| Variable | Description | Default / Example |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/payments` |
| `SPRING_DATASOURCE_USERNAME` | PostgreSQL username | `payments` |
| `SPRING_DATASOURCE_PASSWORD` | PostgreSQL password | Set via `.env` — do not commit secrets |
| `SPRING_DATA_REDIS_HOST` | Redis host | `localhost` |
| `SPRING_DATA_REDIS_PORT` | Redis port | `6379` |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap servers | `localhost:9092` |
| `SERVER_PORT` | Application port | `8080` |

Copy `.env.example` to `.env` if one is added later, and keep real credentials out of version control. See `.gitignore`.

## How to Run

1. Start dependencies: `docker compose up -d`
2. Run the app: `./mvnw spring-boot:run`
3. Submit a transaction: `POST /transactions`
4. Stop dependencies: `docker compose down`

## Roadmap / Limitations

- The REST endpoints in this skeleton are stubs; fraud evaluation logic is not implemented.
- The fraud checks described in `docs/ARCHITECTURE.md` are illustrative sketches, not production rules.
- No production deployment, transaction volumes, accuracy, or latency numbers are claimed.
- Planned next steps, if this skeleton is developed further: implement transaction persistence, Kafka producers/consumers, Redis velocity checks, fraud-rule evaluation, tests, and CI with GitHub Actions.

## Author

Nikhil M — Software Engineer. MS Computer Science, University of Texas at Arlington (2023–2025). Experience in Java, Spring Boot, Kafka, AWS, microservices, and CI/CD with GitHub Actions, Docker, and Kubernetes at JPMorgan Chase and ACS Solutions.
