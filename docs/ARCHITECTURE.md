# Architecture

## Components

- **API service (Spring Boot, Java)** — receives transaction submissions over REST, publishes transaction events, and serves transaction lookups.
- **Kafka** — event backbone. Transaction submissions are published as events so fraud evaluation can proceed asynchronously from the initial request path.
- **Fraud evaluation service** — consumes transaction events from Kafka and evaluates them for fraud in real time. In this skeleton it is a logical component, not an implemented service.
- **Redis** — fast lookups used by fraud evaluation, for example velocity / recent-activity checks.
- **PostgreSQL** — persistent storage for transactions and their evaluation decisions.
- **Docker** — local development and packaging of the services.
- **AWS** — cloud deployment target (see deployment sketch below).

## Event Flow with Kafka

```
Client
  -> POST /transactions (API service)
  -> publish transaction event to Kafka topic (e.g. `transactions`)
  -> Fraud evaluation service consumes the event
  -> Redis lookups for recent activity
  -> Evaluation decision made
  -> Decision + transaction persisted to PostgreSQL
  -> Decision available via GET /transactions/{id}
```

Topic and consumer-group names are placeholders for local development; they are not production configuration.

## Fraud Evaluation Sketch

Illustrative only — not production rules, and no accuracy or latency claims are made.

1. **Rule checks** — evaluate the transaction against a set of configurable rules (for example, amount or other transaction attributes). The specific rules are not defined in the resume description and are not implemented here.
2. **Redis velocity checks** — look up recent activity for the relevant account / card / device in Redis to detect unusual frequency or patterns in a short window.
3. **Decision** — combine the rule and velocity check outcomes into a decision, store it with the transaction in PostgreSQL, and make it retrievable via the API.

## Data Model

Illustrative, minimal shape for the starter skeleton:

**Transaction**
- `id` — unique identifier
- `amount` — transaction amount
- `currency` — transaction currency
- `status` / `decision` — evaluation outcome (values to be defined when implemented)
- `created_at` — submission timestamp

Additional fields (account, merchant, device, etc.) are not specified in the resume description and are intentionally left out of this skeleton.

PostgreSQL stores transactions and decisions. Redis stores short-lived, fast-lookup data for velocity checks only, not as the system of record.

## AWS Deployment Sketch

- Package the Spring Boot service as a Docker image.
- Run the service on AWS container infrastructure (for example, ECS / EKS — Kubernetes experience is listed on the resume; the specific target is a sketch, not a deployed setup).
- Use managed Kafka, Redis, and PostgreSQL services where available, or self-managed equivalents in Docker for local development.
- Use GitHub Actions for CI/CD, as listed in the author's experience.
- No production deployment is claimed for this repository. Secrets, networking, scaling, and observability configuration are out of scope for this starter skeleton.
