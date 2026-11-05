# Interview guide: Secure Banking Platform

## 30-second version

"It's a banking web app with a Spring Boot REST API and an Angular front end. Login issues a signed
JWT; Spring Security reads it on every request, so the API is stateless, and roles in the token drive
access control: customers only touch their own accounts, admin routes need the ADMIN role. Account
numbers and phone numbers are encrypted at rest with AES-GCM through a JPA converter. Transfers lock
both account rows in a fixed order inside one transaction, and an idempotency key makes retries safe."

## Request walkthrough: a transfer

1. Angular sends `POST /api/accounts/transfer` with `Authorization: Bearer <jwt>` (added by the HTTP
   interceptor) and a fresh `Idempotency-Key`.
2. `JwtAuthFilter` verifies the signature and expiry, puts the username and `ROLE_CUSTOMER` in the
   security context. Invalid token: no authentication, so the entry point returns 401.
3. `SecurityConfig` allows the route for any authenticated user. Ownership is checked in the service,
   because "is this your account" depends on data, not just role.
4. `AccountService.transfer` validates the amount, locks the lower account id first, then the higher one
   (`SELECT ... FOR UPDATE`), checks ownership and balance, debits, credits, and writes two ledger
   entries, all in one `@Transactional` method. Any exception rolls the whole thing back.
5. If the same idempotency key arrives again, the existing TRANSFER_OUT entry is found and no money moves.

## Likely questions

**Why lock in id order?** Two transfers A→B and B→A could each lock one row and wait for the other.
Always locking the lower id first means they queue instead of deadlocking.

**Why pessimistic locking, not just @Version?** Money moves need the balance check and update to be
atomic under contention. A row lock serialises the two transfers. `@Version` is still on the entity as a
safety net for other updates.

**Why is the JWT stored in memory in Angular, not localStorage?** localStorage is readable by any
script on the page, so an XSS bug would leak a long-lived token. In memory, the token disappears on
refresh. The production version would use a short-lived access token plus an HttpOnly refresh cookie.

**Why disable CSRF?** CSRF exploits cookies the browser sends automatically. This API authenticates
with a header the attacker's page can't set, so there is nothing for CSRF to ride on. If I moved the
token to a cookie, CSRF protection would have to come back.

**Why AES-GCM, and why a new IV each time?** GCM gives confidentiality and an authentication tag, so
tampered ciphertext fails to decrypt. Reusing an IV with the same key breaks GCM, so every value gets 12
random bytes, stored with the ciphertext. Side effect: equal values encrypt differently, so you can't
query by encrypted column; that's why `last4` is stored separately in clear.

**Why 404 instead of 403 for someone else's account?** 403 confirms the account exists. 404 reveals nothing.

**How would you scale or harden it?** Move secrets to a secret manager, rotate keys with a key id
prefix on ciphertext, add rate limiting on login, refresh tokens, audit logging, and run on PostgreSQL
with Flyway migrations instead of `ddl-auto`.
