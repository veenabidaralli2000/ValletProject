# Vallet

A small wallet service: Spring Boot + PostgreSQL + Liquibase, with a React UI.
Supports deposit / withdraw / read-balance via REST. Built so that many
concurrent requests against the same wallet don't lose updates and don't
fail with 5xx.

## Endpoints

```
POST /api/v1/wallet           body: { walletId, operationType, amount }
GET  /api/v1/wallets/{uuid}
POST /api/v1/wallets          optional helper to create a wallet
```

Operation types: `DEPOSIT`, `WITHDRAW`.

Error responses follow a simple shape:

```json
{
  "timestamp": "...",
  "status": 404,
  "error": "Wallet Not Found",
  "message": "Wallet not found: ...",
  "path": "/api/v1/wallets/..."
}
```

Main error cases:

- 404 — wallet not found
- 400 — insufficient funds / malformed JSON / unknown operationType / invalid UUID / validation failure (bad amount, missing field, etc.)

## Concurrency

Each `Wallet` has a JPA `@Version` column. The service method runs in
`REQUIRES_NEW` and is wrapped with `@Retryable` on
`OptimisticLockException` so collisions under load get retried with
jittered exponential back-off instead of returning a 5xx.

Defaults are fine for most cases. If you expect a sustained hot wallet,
bump `WALLET_RETRY_MAX_ATTEMPTS` and `DB_POOL_MAX_SIZE` in `.env`.

## Running (Docker)

```
cp .env.example .env
docker compose up --build -d
```

Then:

```
curl http://localhost:8080/api/v1/wallets/11111111-1111-1111-1111-111111111111
```

A demo wallet is seeded by Liquibase with id `11111111-1111-1111-1111-111111111111`
and balance `1000.00`.

To change pool size, retry attempts, CORS origins, DB credentials, etc., edit
`.env` and `docker compose up -d --no-deps app` — no rebuild needed.

## Running the UI

```
cd frontend
yarn install
yarn start
```

UI at http://localhost:3000, talks to http://localhost:8080 by default.
Override with `REACT_APP_BACKEND_URL` if your backend is on a different host.

## Tests

```
cd wallet-service
mvn test
```

Covers the service, the controller (via MockMvc, including the error paths),
a full-stack integration test on H2 in Postgres mode, and a concurrency test
that fires many threads against one wallet and checks nothing is lost and
nothing failed.

## Layout

```
wallet-service/     Spring Boot app
  src/main/java/com/wallet/
    controller/     WalletController
    service/        WalletService (optimistic-lock + retry)
    repository/     WalletRepository (JpaRepository)
    entity/         Wallet (@Version)
    dto/            request/response DTOs
    exception/      domain exceptions + @RestControllerAdvice
    config/         CORS
  src/main/resources/
    application.yml
    db/changelog/   Liquibase changelogs
  src/test/...      unit + MockMvc + integration + concurrency
frontend/           React UI (CRA + Tailwind + shadcn/ui)
docker-compose.yml  app + postgres
.env.example        runtime settings (no rebuild required)
scripts/smoke.sh    curl-based smoke test
```
