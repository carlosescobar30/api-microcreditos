# API Microcréditos

*[Versión en español](README.md)*

Core of a microlending API: user registration and authentication, loan
origination, amortization schedules, and payment collection with cascading
allocation.

Personal project, work in progress.

## Stack

Java 21 · Spring Boot 4 · Spring Modulith · PostgreSQL · Flyway · Testcontainers · Docker

## Modules

| Module | Responsibility |
|---|---|
| `iam` | Users, roles, JWT authentication and refresh token rotation |
| `operational` | Loan products, origination, payment schedule and collection |
| `common` | Base entity, error handling and the shared principal |

Module boundaries are enforced by a Spring Modulith test: `operational` reaches
`iam` only through a published interface, never through its repositories.

## Loan lifecycle

1. The user browses the product catalog.
2. They request a product. It is rejected if their identity is unverified, if
   they are in arrears, if their credit score is too low, or if they already
   have a request in progress.
3. If it passes, the loan is pre-approved.
4. Accepting it materialises the full amortization schedule.
5. The user registers a payment against the loan, not against a specific
   installment.
6. Once confirmed, the payment is allocated in cascade: arrears first, then
   interest, then principal, spilling over into later installments.

A daily job moves installments to current or overdue, accrues penalty interest
and updates the loan status.

## Design decisions

- **Refresh tokens are stored hashed** (SHA-256). A database dump does not let
  an attacker impersonate anyone.
- **Rotation is serialised with a pessimistic lock**, with a grace window so two
  legitimate concurrent requests are not mistaken for an attack. Reusing a token
  outside that window revokes every token the user holds.
- **German amortization** (constant principal, declining installment). The last
  installment absorbs the rounding remainder so the principal adds up exactly.
- **A payment does not belong to an installment.** It is split across concepts
  and installments, and every slice is recorded as an allocation.
- **A payment leaves `PENDING` only once.** `PENDING → APPROVED` or
  `PENDING → DECLINED`; both are final. Repeating the same validation returns the
  original result, and asking for the opposite one answers 409.
- **Validation locks the payment and then its loan** (`SELECT ... FOR UPDATE`),
  always in that order. Two simultaneous confirmations of the same payment do not
  allocate it twice, and two payments of the same loan do not overwrite each
  other's installments.
- Modules reference each other by a public UUID, never by the internal primary
  key.
- Errors are returned as `ProblemDetail` (RFC 7807) with a stable error code.

## Running it

```bash
cp .env.example .env     # database credentials and JWT_KEY (256-bit Base64)
docker compose up -d --build
```

This starts PostgreSQL and the application. The API is available at
`http://localhost:8080` and the documentation at `/swagger-ui.html`.

To run the application from your IDE, start the database only with
`docker compose up -d db` and point the datasource at `localhost:5432`.

## Tests

```bash
./mvnw verify            # requires Docker
```

61 tests across three levels:

- **Unit** — JWT signing and parsing, token hashing, and every branch of the
  rotation logic, including the exact grace period boundary, and every status
  transition of a payment.
- **Integration** (Testcontainers) — the queries against a real Postgres, the
  revocation surviving the exception, and two concurrent rotations of the same
  token resolving to exactly one new token. In `operational`, two concurrent
  validations of the same payment allocating it only once, and the loan balance
  matching the principal left in its installments.
- **Web slice** — the HTTP contract of `/auth` and the security chain: missing,
  expired and forged tokens, plus role based access.

## Known limitations

- In `operational`, only payment validation is covered.
- No rate limiting on login.
- No real payment gateway. `/payment/validate` stands in for the provider
  confirmation.
- No principal prepayment with re-amortization: overpaying covers upcoming
  installments instead.
- Surplus is recorded as an allocation but is not yet a usable credit balance.
- The scheduled jobs read the system clock instead of the injected `Clock`, which
  makes them hard to test deterministically.

## History

- [#1 — IAM module](https://github.com/carlosescobar30/api-microcreditos/pull/1)
- [#2 — Operational module](https://github.com/carlosescobar30/api-microcreditos/pull/2)
