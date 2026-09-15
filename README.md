# Digital Wallet System

A production-style fintech platform built end-to-end: a Spring Boot ledger-backed wallet API and a React Native mobile client, plus a MoMo payment gateway integration — built to practice the concepts real fintech backends run on (double-entry ledgers, idempotency, fraud rules, JWT session security, audit trails) rather than a CRUD toy app.

## What it does

- **Accounts & security** — register/login with email OTP verification, Google Sign-In (backend verifies the mobile client's ID token, auto-links to an existing email or creates a pre-verified account), JWT access + refresh token rotation with reuse detection ("logout everywhere" on password change or token replay), optional MFA via email OTP, session management (view/revoke active devices), rate limiting per route group.
- **Wallet & money movement** — deposit, withdraw, and transfer between users, backed by a real **double-entry ledger** (every transaction posts two offsetting ledger rows), **idempotency keys** so retried requests can't double-charge, and **pessimistic row locking** (with deterministic lock ordering on transfers to avoid deadlocks).
- **Fraud & limits** — rule-based fraud scoring (challenge/block thresholds), OTP confirmation gate on large transfers/withdrawals, daily money-movement limits, and an admin review queue for challenged transactions.
- **MoMo payment gateway** — top up a wallet via MoMo (`captureWallet`), with HMAC-SHA256 request signing, constant-time signature verification on the callback, and a strict rule that only the server-to-server IPN callback (never the browser redirect) can finalize a payment and credit the ledger.
- **Admin & observability** — user management (role/lock), full audit log with filtering and CSV export, an admin dashboard (overview + volume metrics), and Spring Actuator health/metrics wired behind role-based access, with Prometheus + Grafana in the local stack.
- **Mobile app** — a full Expo/React Native client (18 screens: auth, home, transfer, deposit/withdraw with OTP, history, notifications, profile, session management) talking to the real API, not mock data.

## Architecture

```
wallet-core/     Spring Boot 3 (Java 17) REST API — the source of truth
  api/           controllers, request/response DTOs
  domain/        entities, enums, repositories (Spring Data JPA)
  service/       business logic (auth, wallet, fraud, audit, MoMo, notifications...)
  security/      JWT filter, token blacklist, rate limiting
  config/        Spring config (security, Redis, JWT, MoMo...)
  db/migration/  Flyway migrations (schema is migration-only — ddl-auto: validate)

wallet-mobile/   Expo (React Native + TypeScript) client
  src/screens/   19 screens, 1:1 with the app's design system
  src/api/       typed API client (axios), token storage, snake_case<->camelCase transform
  src/auth/      session state (login/refresh/logout)
```

Infrastructure: PostgreSQL (Flyway-migrated), Redis (OTP, token blacklist, rate limiting), SMTP (OTP email, dev via MailHog), Prometheus + Grafana. See `wallet-core/docker-compose.yml`.

## Tech stack

| | |
|---|---|
| Backend | Spring Boot 3.3, Java 17, PostgreSQL, Redis, Flyway, Spring Security (JWT), WebSocket |
| Mobile | Expo SDK 57, React Native, TypeScript (strict), NativeWind (Tailwind for RN), React Navigation, TanStack Query |
| Payments | MoMo AIO v2 gateway (sandbox) |
| Testing | JUnit 5 + Mockito + AssertJ (33 backend test files), Jest (mobile) |
| Ops | Docker Compose, Prometheus, Grafana, GitHub Actions CI, Dependabot, gitleaks secret scanning |

## Running it locally

```bash
# 1. Infra
cd wallet-core
docker-compose up -d postgres redis mailhog
cp .env.example .env   # fill in JWT_SECRET etc.
mvn spring-boot:run    # http://localhost:8089

# 2. Mobile (Expo Web is the fastest way to try it)
cd wallet-mobile
npm install
npm run web             # http://localhost:8081
```

MailHog (`http://localhost:8025`) catches OTP emails in dev, so the full register → verify → transact flow works without a real mailbox.

Google Sign-In needs a Google Cloud OAuth Client ID (Web application type) — put the same value in `wallet-core/.env`'s `GOOGLE_CLIENT_ID` and `wallet-mobile/.env`'s `EXPO_PUBLIC_GOOGLE_CLIENT_ID`. Leave both blank to just disable the button; everything else works without it.

## Notable engineering details

- **Idempotency + locking are load-bearing, not decorative** — every money-movement endpoint takes a client-supplied idempotency key with a unique DB constraint, and wallet rows are locked with `SELECT ... FOR UPDATE` (transfers lock both wallets in a fixed UUID order to prevent deadlocks).
- **Failed transactions are kept, not rolled back** — money-movement services use `@Transactional(noRollbackFor = BusinessException.class)` so a failed/blocked transaction still leaves an audit trail instead of vanishing.
- **MoMo integration treats the browser redirect as untrusted** — only the signed server-to-server IPN can move money; the return URL is read-only. The MoMo payment request is a separate table (not bolted onto the core `transactions` schema) so other payment gateways can be added the same way later.
- **Security headers, IDOR tests, and secret scanning are part of CI**, not an afterthought (see `.github/workflows`).

## Status

Both the backend and the mobile app are feature-complete for the scope above and covered by automated tests (see `wallet-core/src/test`, `wallet-mobile/src/api/__tests__`). Native (App Store/Play Store) builds haven't been produced yet — the mobile app has been exercised through Expo Web and Expo Go. A web admin dashboard frontend hasn't been built (the backend API for it already exists under `/api/admin/**`).
