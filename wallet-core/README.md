# wallet-core

Spring Boot 3 (Java 17) REST API for the digital wallet — the source of truth for accounts, balances, and transactions. See the [root README](../README.md) for the project overview and how the mobile app fits in.

## Package layout

```
src/main/java/com/digitalwallet/
  api/          REST controllers + request/response DTOs
  common/       shared response envelope (ApiResponse/ErrorInfo), request metadata
  config/       Spring config: Security, JWT, Redis, Async, WebSocket, MoMo, rate limiting
  domain/       JPA entities, enums, Spring Data repositories
  exception/    business exceptions + GlobalExceptionHandler (maps to ErrorCode)
  security/     JWT filter/provider, token blacklist, rate-limit filter, @CurrentUser
  service/      business logic — Auth, Wallet, Fraud, Audit, Notification, Momo...
  util/         ReferenceNumberGenerator, IdempotencyService

src/main/resources/
  application.yml     config (reads everything from .env via Spring's config import)
  db/migration/        Flyway SQL migrations (V1...V8) — schema changes go here, not ddl-auto
```

## Feature areas

- **Auth** — register/login with email OTP verification, Google Sign-In (`POST /api/auth/google` verifies the client's ID token — see `service/GoogleTokenVerifierService.java`), JWT access + refresh rotation with reuse detection, optional MFA (email OTP), forgot/reset password, session management, account lockout after repeated failed logins.
- **Wallet** — deposit/withdraw/transfer with a double-entry ledger, idempotency keys, pessimistic row locking (deadlock-safe ordering on transfers), OTP confirmation on large transfers/withdrawals, daily money-movement limits.
- **Fraud** — rule-based scoring (challenge/block by amount threshold) with an admin review queue for challenged transactions.
- **MoMo** — top-up via the MoMo AIO v2 gateway; only the signed IPN callback (never the browser return URL) can finalize a payment. See `service/Momo*.java`.
- **Admin** — user management (role/lock), audit log with filtering + CSV export, dashboard overview/volume metrics — all under `/api/admin/**`, role-gated.
- **Notifications** — in-app notifications + email (transaction confirmations, security alerts), pushed over WebSocket/STOMP.
- **Ops** — Actuator health/metrics (role-gated except `/actuator/health`), Prometheus scrape endpoint, structured logging with a request-id on every log line.

## Running it

```bash
docker-compose up -d postgres redis mailhog   # + prometheus, grafana if you want dashboards
cp .env.example .env                          # fill in JWT_SECRET at minimum
mvn spring-boot:run                           # http://localhost:8089
```

`GET /actuator/health` is public; everything else requires a JWT except `/api/auth/**` and `/api/momo/**` (signature-verified instead of JWT).

## Testing

```bash
mvn test
```

33 test files covering auth (token rotation, lockout, MFA), the money-movement flows (idempotency, limits, fraud), admin endpoints (permission checks via `@WithMockUser`), and the MoMo signature service (valid signature, tampered-field rejection).

## Local dev notes

- Schema changes go through Flyway migrations only (`ddl-auto: validate` — Hibernate will refuse to start if the schema and entities disagree, which has caught real mapping bugs, e.g. `@Lob` mapping a `text` column to Postgres `oid` instead).
- MoMo's sandbox calls your `MOMO_REDIRECT_URL`/`MOMO_IPN_URL` directly — `localhost` won't work for real sandbox testing; tunnel with `ngrok http 8089` and point those two vars at the tunnel URL.
- If a migration's checksum drifts from what Flyway recorded (e.g. you edited an already-applied migration locally), `mvn flyway:repair` fixes the recorded checksum without touching data.
- Google Sign-In only needs `GOOGLE_CLIENT_ID` (a "Web application" OAuth client — create one at console.cloud.google.com) — no client secret or redirect URI, since the backend only verifies an ID token the mobile client already obtained, it doesn't do the authorization-code exchange itself. Leave it blank to leave the feature disabled.
