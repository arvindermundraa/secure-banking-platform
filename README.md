# Secure Banking Platform

A multi-tier banking web app: a Spring Boot REST API secured with Spring Security and JWT,
role-based access control, AES-GCM encryption for sensitive data at rest, and an Angular front end.

```
Angular 18 (standalone components, signals)
   │  Authorization: Bearer <JWT>   Idempotency-Key: <uuid>
   ▼
Spring Boot 3.3 REST API ── Spring Security filter chain (stateless JWT, RBAC)
   │
   ▼
JPA / Hibernate ── H2 (dev, tests) or PostgreSQL (DATABASE_URL)
```

## Features

| Area | What it does | Where |
|---|---|---|
| Authentication | Register and log in; passwords hashed with BCrypt (cost 12); HS256 JWT with 30-minute expiry | `security/JwtService.java`, `service/AuthService.java` |
| Authorization (RBAC) | `CUSTOMER` and `ADMIN` roles from the JWT; `/api/admin/**` is admin-only; customers can only see and move money from their own accounts | `security/SecurityConfig.java`, `service/AccountService.java` |
| Encryption at rest | Full account numbers and phone numbers encrypted with AES-256-GCM through a JPA converter (fresh IV per value, tamper detection); the API only ever returns `****1234` | `crypto/` |
| Safe transfers | One DB transaction; both account rows locked `FOR UPDATE` in id order (no deadlocks); balance check; two ledger entries | `service/AccountService.java` |
| Idempotency | `Idempotency-Key` header; a retried deposit or transfer is applied once | `AccountService`, `LedgerEntry` unique (reference, type) |
| Input validation | Bean Validation on every request; amounts must be positive, at most 2 decimals, under a per-transaction limit | `web/Dtos.java` |
| No information leaks | Same error for unknown user and wrong password; another customer's account returns 404, not 403 | `AuthService`, `AccountService` |
| Front end | Login / register, accounts, deposit, transfer, history; JWT kept in memory, HTTP interceptor adds it, route guard protects pages | `frontend/src/app/` |

## Run it

```bash
# API on :8080 (H2 in memory)
cd backend
ADMIN_USERNAME=admin ADMIN_PASSWORD=change-me-please mvn spring-boot:run

# UI on :4200
cd frontend && npm install && npx ng serve
```

For PostgreSQL set `DATABASE_URL=jdbc:postgresql://localhost:5432/bank`, `DATABASE_USER`,
`DATABASE_PASSWORD`. Always set your own `JWT_SECRET` and `ENCRYPTION_KEY` (32 random bytes, base64:
`openssl rand -base64 32`).

## Tests

`cd backend && mvn verify` runs integration tests against the full Spring context with MockMvc:
unauthenticated access, identical login errors, weak passwords, RBAC on admin endpoints, transfers
writing both ledger entries, overdraft rejection with no partial writes, cross-customer access,
idempotent retries, invalid amounts, and a check that phone and account numbers are not stored in
plain text. Unit tests cover AES-GCM round trips, unique IVs and tamper detection.
GitHub Actions runs the backend tests and the Angular production build on every push.

## API

| Method | Path | Auth |
|---|---|---|
| POST | `/api/auth/register` | public |
| POST | `/api/auth/login` | public |
| GET / POST | `/api/accounts` | customer |
| POST | `/api/accounts/{id}/deposit` | owner |
| POST | `/api/accounts/transfer` | owner of source |
| GET | `/api/accounts/{id}/transactions` | owner |
| GET | `/api/admin/users` | admin |
