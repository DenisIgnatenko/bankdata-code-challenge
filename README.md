# Bankdata Code Challenge — Mini Banking System (Quarkus)

This repo contains a small multi-service system built with Quarkus. 

It’s intentionally simple, but still shows “real life” patterns:
- a transactional service (Account)
- an event consumer + audit storage (Analytics)
- a 3rd-party API integration (FX)
- a shared `contracts` module for cross-service event DTOs

If you just want to run it: jump to **Quick start**.

---

## Breif Overview

There are three services and one shared library:

- **account-service** (REST + DB)
    - creates accounts, deposits, transfers, balance
    - publishes `AccountEvent` to Kafka on every state change

- **analytics-service** (Kafka consumer + DB + REST)
    - consumes `AccountEvent`
    - stores events as an append-only audit log (idempotent by `eventId`)
    - provides a simple REST endpoint to trigger latest events

- **fx-service** (REST only)
    - converts DKK → USD using a public exchange-rate provider API
    - requires `EXCHANGE_RATE_API_KEY` env var

- **contracts** (shared jar)
    - contains `AccountEvent` and related enums

---

## Architecture (data flow)

When you call Account endpoints, it writes to its DB and emits events to Kafka.  
Analytics listens to Kafka and stores those events.

FX is separate and just calls a 3rd party API. To be done here: Bonus task - Historical data

## Start (Docker Compose)

Prerequisites:
- Docker
- Docker Compose

From repository root:

```bash
docker compose up --build
```

Then check containers:
```bash
docker compose ps
```

**Expected ports:**
-	Account: http://localhost:8081
-	FX: http://localhost:8082
-	Analytics: http://localhost:8083

**Swagger UI**

Each service exposes OpenAPI and Swagger UI:
•	Account Service Swagger: http://localhost:8081/swagger/
•	FX Service Swagger: http://localhost:8082/swagger/
•	Analytics Service Swagger: http://localhost:8083/swagger/

## Try it quickly

### Account service

Base URL: http://localhost:8081

Health check
```
curl http://localhost:8081/health
```

Create account:
```
curl -i -X POST http://localhost:8081/accounts \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Denis","lastName":"Ignatenko","initialDeposit":10.00}'
```

Deposit:
```
curl -i -X POST http://localhost:8081/accounts/{accountNumber}/deposit \
  -H "Content-Type: application/json" \
  -d '{"amount":5.00}'
```

Transfer (create two accounts first to copy their numbers):
```
curl -i -X POST http://localhost:8081/accounts/transfer \
  -H "Content-Type: application/json" \
  -d '{"fromAccountNumber":"{acc1}","toAccountNumber":"{acc2}","amount":1.00}'
```

Balance:
```
curl -i http://localhost:8081/accounts/{accountNumber}/balance'
```

### Analytics Service

Base URL: http://localhost:8083

Latest events:
```
curl -s "http://localhost:8083/analytics/events?limit=50" | jq 
```

## FX Service

Base URL: http://localhost:8082

Convert DKK -> USD:
```
curl -i "http://localhost:8082/fx/dkk-usd?amount=100.00"
```
If amount is missing, it defaults to 100.00.

**Configuration** 

FX service reads API key from env var:
```
EXCHANGE_RATE_API_KEY=your_api_key
```
Service will start but will not work without API key in .env in root

## Domain rules and edge cases (what the API expects)

**Money rules:**
- amount must be non-null
- amount must have max 2 decimals
-	negative amounts are rejected
-	deposit/withdraw in the domain require positive (> 0)

**Transfer rules:**
-	fromAccountNumber != toAccountNumber (otherwise 400)
-	locks are taken in stable order to reduce deadlocks

**Account creation:**
- account numbers are generated
-	uniqueness is enforced by DB constraint
-	service retries generation a limited number of times if it hits a duplicate

**Analytics:**
-	ingestion is idempotent by eventId
-	duplicates are ignored (even if Kafka re-delivers)

## Important code is here

**Account Service:**
-	REST: services/account-service/src/main/java/.../api
-	business logic: .../application/AccountService
-	domain: .../domain/AccountEntity
-	persistence: .../persistence
-	Kafka publishing: .../messaging

**Analytics Service:**
-	consumer: .../messaging
-	ingestion: .../application/AccountEventIngestionService
-	DB entity/repo: .../persistence
-	read REST: .../api/AnalyticsResource

**FX Service:**
-	REST: .../api/FxResource
-	integration: .../integration/ExchangeRateApiGateway

**Shared contracts:**
-	libs/contracts/src/main/java/.../contracts/events

## Tests (minimal but useful)
Run everything:
```
mvn test
```
Or separately
```
mvn -pl services/account-service test
mvn -pl services/fx-service test
mvn -pl services/analytics-service test
```
Approach
-	Unit tests for AccountService (Mockito)
-	Quarkus integration tests for REST endpoints (RestAssured)
-	Analytics/FX tests can stay minimal but can be extended

## Diagrams

Runtime overview
![Runtime overview (services + ports).png](docs/diagrams/Runtime%20overview%20%28services%20%2B%20ports%29.png)

Account create flow (with retry on unique constraint)
![Account create flow (with retry on unique constraint).png](docs/diagrams/Account%20create%20flow%20%28with%20retry%20on%20unique%20constraint%29.png)

Account service - happy end
![Account service — happy.png](docs/diagrams/Account%20service%20%E2%80%94%20happy.png)

Deposit money flow
![Deposit flow.png](docs/diagrams/Deposit%20flow.png)

Money transfer flow
![Transfer flow (stable lock order).png](docs/diagrams/Transfer%20flow%20%28stable%20lock%20order%29.png)

Analytics service ingestion pipeline
![Analytics-service ingestion pipeline.png](docs/diagrams/Analytics-service%20ingestion%20pipeline.png)

FX-Service request flow
![FX-service request flow.png](docs/diagrams/FX-service%20request%20flow.png)

💛