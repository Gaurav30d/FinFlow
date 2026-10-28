# FinFlow — Phase A Implementation & Execution Plan

> **Purpose:** This is the working guide for completing FinFlow efficiently with LLM assistance. It records the known implementation, architecture rules, remaining roadmap, acceptance criteria, and a repeatable implementation workflow.
>
> **Approach:** Let the LLM handle boilerplate and repetitive implementation, but understand each feature's purpose, data flow, risks, and tests. Implement one logical chunk at a time.

## 1. Project Goal

FinFlow is a user-facing digital wallet/banking application built with Java and Spring Boot. It should demonstrate sound software engineering and provide a usable frontend—not just backend services.

Target capabilities:
- User registration, login, JWT access tokens, and refresh tokens
- Wallet/account creation and balance retrieval
- Deposits, withdrawals, transfers, and transaction history
- Safe money movement with concurrency and idempotency protections
- Transaction notifications
- Audit events and a foundation for fraud checks
- Frontend screens for the main wallet flows
- Docker-based local setup, tests, and clear documentation

Prioritize a reliable end-to-end flow and correctness over adding technologies solely for a resume.

## 2. Technology Stack

Use the project's existing versions and dependencies unless a concrete compatibility issue requires a change.

- Java 21
- Spring Boot 4.1.1 (current known version)
- Spring Cloud 2025.1.3 (current known version)
- Spring Security, JWT, refresh tokens, BCrypt
- Spring Data JPA / Hibernate
- MySQL, database per service
- Spring Cloud Gateway and Eureka
- Apache Kafka
- Redis where justified
- Docker / Docker Compose
- Existing HTML/CSS/JavaScript frontend
- Maven Wrapper (`./mvnw`)

**Compatibility rule:** Inspect the relevant `pom.xml` before adding dependencies. Do not casually upgrade/downgrade Spring Boot or Spring Cloud.

## 3. Current Known Architecture

| Component | Port | Responsibility | Known status |
|---|---:|---|---|
| Eureka Discovery Server | 8761 | Service registration/discovery | Implemented; verify at runtime |
| API Gateway | 8080 | Routing and JWT validation | Implemented; routes and JWT filter exist |
| Auth Service | 8081 | Registration/login, JWT and refresh-token concerns | Implemented in part; verify complete flows |
| Account Service | 8082 | Accounts, wallet balances, account operations | Implemented in part; verify invariants |
| Transaction Service | 8083 | Deposits, withdrawals, transfers, state/history | Implemented in part; verify full flow |
| Notification Service | 8084 | Kafka consumer and notification APIs | Implemented; duplicate/DLT checks performed |
| Audit Service | TBD | Consume and persist audit events | Planned/status to inspect |
| Fraud component/service | TBD | Risk assessment before final transaction approval | Planned/status to inspect |
| Frontend | TBD | User-facing screens and API integration | Inspect current implementation |

This table is based on the known project state, not a substitute for inspecting the repository. Before coding, verify whether a component already exists; never create a duplicate service.

### Communication rules

- Client/browser → API Gateway over HTTP.
- Gateway → backend services using existing routes/service discovery.
- Synchronous REST for operations needing an immediate result.
- Kafka for asynchronous transaction, notification, and audit events.
- Each service owns its own database.
- **No service may query or modify another service's database directly.**
- Keep secrets out of source control; use environment variables or ignored local configuration.

## 4. Database Ownership

Known local databases:
- `finflow_auth` — authentication/credential data
- `finflow_account` — accounts and balances
- `finflow_transaction` — transaction records and transaction-owned data
- `finflow_notification` — notification records

Create additional databases only if the corresponding service is actually implemented and needs persistence.

Rules:
1. Auth owns credentials and authentication state.
2. Account owns accounts and balances.
3. Transaction owns transaction records and transaction state.
4. Notification owns notifications.
5. Audit owns audit records.
6. Do not share JPA entities/repositories across services.
7. Cross-service IDs may be stored as references, but never use them as a reason to join another service's database.

Inspect current entities/schema before making migrations or DDL changes. Avoid destructive schema edits.

## 5. Known Implemented Work

Recheck these facts in the repository before changing anything.

### 5.1 Kafka and Notification Service

- Kafka runs in Docker using `apache/kafka:4.0.0`; broker is available locally at `localhost:9092`.
- Topic `finflow.transaction.events`: 3 partitions, replication factor 1.
- DLT topic `finflow.transaction.events.DLT`: 3 partitions, replication factor 1.
- Notification Service consumes `finflow.transaction.events`.
- Gateway endpoints used:
  - `GET /api/notifications`
  - `PATCH /api/notifications/{id}/read`
  - `GET /api/notifications/unread-count`
- Notification data is persisted in MySQL.
- Notification entity fields include UUID `id`, UUID `eventId`, UUID `transactionReference`, `Long recipientUserId`, `message`, `type`, boolean `read` mapped to `is_read`, and `Instant createdAt`.
- Unique constraint on `(event_id, recipient_user_id)`, named `uk_notification_event_recipient`.
- Repository methods include `existsByEventIdAndRecipientUserId`, `findByRecipientUserIdOrderByCreatedAtDesc`, `findByIdAndRecipientUserId`, and `countByRecipientUserIdAndReadFalse`.
- `NotificationPersistenceService.persistNotifications(...)` is `@Transactional` and saves sender/receiver notifications together.
- `NotificationService.processTransactionCompleted(...)` checks for existing sender/receiver notifications, skips duplicates, and handles partial/concurrent duplicate cases.
- Replaying an existing event was tested: the consumer logged that the event was already processed and skipped it. SQL showed one notification per recipient for that event.
- Consumer should ignore event types other than `TRANSACTION_COMPLETED`.
- The temporary `DLT_TEST` failure-injection code in `TransactionEventConsumer.java` is commented out. Do not re-enable it during normal operation.

### 5.2 Dead Letter Topic (DLT)

- Consumer uses `ErrorHandlingDeserializer` around Jackson JSON deserialization.
- `DefaultErrorHandler` uses `DeadLetterPublishingRecoverer` to route failed records to `<original-topic>.DLT`, preserving the partition where possible.
- Configured retry backoff is `FixedBackOff(1000L, 2L)`: initial processing plus two retries for retryable listener failures, subject to error-handler/deserialization behavior.
- A malformed payload `{invalid-json` appeared in DLT output as `e2ludmFsaWQtanNvbg==`, its Base64 representation.
- A previous `DLT_TEST` event was also found in DLT.
- DLT routing therefore has evidence of working. Do not keep publishing malformed events or change Kafka configuration without a new, specific failure.
- A console consumer may print `TimeoutException` after consuming available records and waiting for more. This alone does not prove DLT failure.
- Current serialization may represent malformed byte-array payloads as Base64 strings. Review this only if improving DLT inspection/replay is a separate task; do not destabilize the current setup during unrelated work.

### 5.3 Consumer behavior

Expected production behavior for `TransactionEventConsumer`:

```java
@KafkaListener(
    topics = "finflow.transaction.events",
    groupId = "finflow-notification-group"
)
public void consume(TransactionCompletedEvent event) {
    if (!"TRANSACTION_COMPLETED".equals(event.getEventType())) {
        log.info("Ignoring event type: {}", event.getEventType());
        return;
    }

    notificationService.processTransactionCompleted(event);
}
```

This is a behavior reference, not an instruction to overwrite the file blindly. Preserve actual package names, imports, and project-specific annotations.

## 6. Non-Negotiable Architecture and Reliability Rules

### Money and transaction integrity

- Validate source/destination accounts, positive amount, currency, and same-account transfers.
- Prevent overdrafts.
- Use `BigDecimal`, never `double`, for money.
- Protect concurrent balance changes with the existing lock strategy or a justified equivalent.
- Use idempotency keys for client-submitted money-moving requests.
- Persist transaction status and enough information to explain failure/success.
- Do not report `SUCCESS` until the money movement is actually complete according to the agreed flow.
- Define recovery for partial failures; do not silently leave one side debited and the other uncredited.

### State machine and distributed consistency

Earlier design discussed `INITIATED`, `PENDING`/`FRAUD_CHECK`, `APPROVED`, `PROCESSING`, `SUCCESS`, and `FAILED`. Inspect the current implementation before finalizing states. Define legal transitions and reject invalid ones.

A local `@Transactional` protects one service's local database transaction; it does not create a distributed transaction across Account and Transaction services. Document the cross-service sequence, timeouts, retry policy, compensating actions, and reconciliation for stuck transactions. Never compensate by blindly applying another balance change.

### Kafka reliability

- Consumers must be idempotent; records can be redelivered.
- Use stable event IDs.
- Do not acknowledge a failed event as successfully processed before the recovery strategy is applied.
- Route exhausted failures and deserialization failures to DLT.
- Keep event schemas consistent across producers/consumers.
- Do not put secrets or unnecessary personal data in events.

### Security

- Gateway must overwrite/remove client-supplied `X-User-Id` before forwarding its trusted identity header.
- Verify resource ownership for account, transaction, and notification endpoints.
- Hash passwords using BCrypt or the existing approved encoder.
- Define refresh-token expiry, rotation/revocation, and storage.
- Never log passwords, JWTs, secrets, or sensitive payment data.
- Use environment variables for credentials/secrets. Never commit `.env` or real credentials.

## 7. Remaining Roadmap (Verify Before Marking Anything Missing)

First inspect the repository and mark each item `DONE`, `IN PROGRESS`, `BLOCKED`, or `NOT STARTED`. This roadmap is not a claim that every item is currently missing.

### Phase A1 — Baseline repository audit

1. Inspect service directories, `pom.xml` files, application properties, controllers, services, entities, repositories, Kafka config, Docker files, and frontend.
2. List existing endpoints per service.
3. Identify build failures, TODOs, duplicate code, insecure settings, and missing tests.
4. Confirm Java/framework versions, ports, databases, Kafka topics, and Gateway routes.
5. Build each service with its Maven Wrapper.
6. Update the service/status table in documentation.

**Acceptance:** no duplicate services; every service's responsibility, port, database, endpoints, and build result are documented; no secrets are exposed.

### Phase A2 — Authentication and Gateway

1. Test registration and login through Gateway.
2. Test access-token validation, expiry, and malformed tokens.
3. Verify refresh-token issuance, rotation/revocation, and invalid-token handling as implemented.
4. Test routes to all services.
5. Ensure identity headers cannot be spoofed.
6. Test authorization/ownership.
7. Add tests for valid, missing, malformed, and expired JWTs.

**Acceptance:** auth flows work through Gateway; protected APIs reject invalid/missing tokens; users cannot access other users' resources.

### Phase A3 — Account/wallet

1. Verify account creation and user-to-account mapping.
2. Verify account/balance endpoints.
3. Check amount/currency validation.
4. Test ownership and missing-account behavior.
5. Confirm Account Service alone owns balances.

**Acceptance:** users see only their own account; invalid requests return consistent 4xx responses; no other service writes account tables.

### Phase A4 — Transactions and money movement

1. Document the actual transfer/deposit/withdrawal sequence before editing.
2. Define state transitions and failure cases.
3. Validate amounts, currencies, accounts, same-account transfers, and overdraft rules.
4. Verify pessimistic locking or a justified concurrency strategy.
5. Enforce idempotency keys for retried client requests.
6. Test concurrent transfers and ensure balances stay consistent.
7. Store transaction status and expose incoming/outgoing history to the owner.
8. Test insufficient funds, duplicate requests, invalid accounts, and failures between service calls.

**Acceptance:** successful transfers update balances correctly and create transaction records; retries do not move money twice; concurrency cannot cause overdrafts/lost updates; failed flows do not leave unresolved partial changes.

### Phase A5 — Kafka and notifications

1. Verify the transaction event is published only after the correct business state is reached.
2. Validate event schema and required fields.
3. Verify Notification Service consumes and persists events.
4. Keep sender/receiver notification persistence atomic.
5. Preserve uniqueness on `(event_id, recipient_user_id)` and duplicate handling.
6. Test notification list, mark-read, and unread-count via Gateway.
7. Verify notification ownership.
8. Add automated tests for duplicates, retries, and DLT where practical.

**Acceptance:** one completed transfer produces the expected notifications; replay does not duplicate them; malformed/repeated failures route to DLT; users cannot read/update others' notifications.

### Phase A6 — Audit trail

1. Inspect whether Audit Service already exists.
2. Define a safe event schema: event ID/type, timestamp, actor where appropriate, correlation ID, and minimal metadata.
3. Publish events for important actions (login outcome without credentials/tokens, account changes, transaction lifecycle).
4. Consume and persist events in an audit-owned database if needed.
5. Make audit records append-only through application APIs.
6. Define what happens if audit is unavailable without corrupting a completed transfer.

**Acceptance:** selected actions produce audit records; duplicate events are safe; ordinary users cannot modify audit history; no sensitive secrets are logged.

### Phase A7 — Fraud/risk checks

Do this only after the basic transaction flow is reliable.

1. Inspect existing fraud code and transaction states.
2. Start with deterministic configurable rules rather than claiming an unbuilt ML model is production-ready.
3. Define `APPROVE`, `REVIEW`, and `REJECT` results with reason codes.
4. Define timeout and unavailable-service behavior.
5. Ensure a rejected transaction cannot move funds.
6. Test each rule and failure mode.

**Acceptance:** decisions are explainable; rejected transactions do not debit/credit; risk checks cannot be bypassed through a downstream endpoint.

### Phase A8 — Frontend integration

1. Inspect and retain the current frontend stack/style.
2. Complete signup/login, dashboard/balance, deposit/transfer form, transaction history, notifications, and logout.
3. Call APIs through Gateway, not direct service URLs.
4. Handle loading, empty, success, and error states.
5. Choose token storage based on the actual auth architecture and document the trade-off.
6. Keep business/security validation on the backend.
7. Add responsive layout and accessible form labels.

**Acceptance:** a user can log in, view balance/history, submit a supported transaction, and see notifications; errors are clear; frontend contains no secrets.

### Phase A9 — Redis and performance

Use Redis only where it has a clear purpose.

1. Inspect current Redis integration.
2. Prefer suitable uses such as rate limiting, short-lived state, or cacheable non-financial data.
3. Never make Redis the source of truth for wallet balances.
4. Define TTL, invalidation, and behavior when Redis is unavailable.
5. Add a test or measurement for the intended benefit.

**Acceptance:** cached data has an invalidation policy; stale cache cannot break financial correctness.

### Phase A10 — Docker/local setup

1. Inspect existing Dockerfiles and Compose files first.
2. Containerize services needed for the chosen local run mode.
3. Remember that `localhost` inside a container refers to that container.
4. Configure Docker service hostnames, health checks, and startup dependencies.
5. Keep secrets in environment variables/ignored local files.
6. Document prerequisites, ports, startup/shutdown, DB initialization, and troubleshooting.
7. Verify MySQL, Kafka, and Eureka connections in the actual run mode.

**Acceptance:** documented setup works from a clean local environment without accidental IDE-only configuration.

### Phase A11 — Automated tests

Prioritize:
- Unit tests for validation and state transitions
- Repository tests for constraints
- Controller/security tests for auth and ownership
- Integration tests for transaction flows
- Kafka tests for events, duplicates, and failures
- Concurrency/idempotency tests for money movement
- API smoke tests for the main user journey

Use the existing test stack; do not add a large framework without a clear need.

**Acceptance:** core happy paths and important failure paths have repeatable tests and documented commands.

### Phase A12 — Observability, documentation, release readiness

1. Add consistent logs and correlation/request IDs where practical.
2. Never log secrets or sensitive payment details.
3. Add health checks and basic metrics where supported.
4. Update README with architecture, services, setup, environment variables, APIs, tests, and demo journey.
5. Document architecture and event flow.
6. State demo limitations honestly.
7. Check Git history for secrets; rotate any exposed secret. Deleting a secret from the latest file alone is insufficient.

**Acceptance:** a new developer can run and understand the project; failures have useful logs; no known credentials remain in tracked configuration.

## 8. Standard LLM Workflow

Use this workflow for every task to accelerate work without losing understanding.

### Step 1 — Provide context

Give the LLM this `phaseA.md`, the relevant source files, the exact task, and any actual error/log output.

### Step 2 — Inspect before editing

Ask the LLM to:
1. Identify the current implementation and relevant files.
2. State exact gaps.
3. Propose the smallest logical change.
4. List files it plans to create/modify.
5. Explain data flow and trade-offs.
6. Call out risks, especially money, schema, security, and service-boundary risks.

### Step 3 — Implement one logical chunk

Require exact file paths and complete files/precise patches. Follow existing packages and naming. Avoid unrelated refactors, duplicate classes/services/endpoints, speculative dependencies, and unverified framework upgrades. Add tests where appropriate.

### Step 4 — Understand the change

Ask:
- What changed and why?
- What is the happy path?
- What happens on failure, retry, and duplicate requests?
- What security/consistency property does it protect?
- What remains unverified?

The developer needs to understand behavior and risks, not memorize every generated line.

### Step 5 — Compile/test once per chunk

From the relevant service directory:

```bash
./mvnw clean compile
```

When tests are added or relevant:

```bash
./mvnw test
```

Fix compile errors before starting multiple services or editing unrelated code.

### Step 6 — Run one complete flow

Start only the affected services. Test via curl/Postman/frontend. Capture request, status code, response, and relevant logs.

### Step 7 — Fix from evidence

Give the LLM exact errors and relevant code/logs. Ask for the smallest root-cause fix. Avoid speculative Kafka/security/schema/version changes.

### Step 8 — Update and commit

After acceptance criteria pass, update the status/progress log and commit the logical chunk with a clear message.

### Copy/paste task prompt

> You are implementing FinFlow. Read `phaseA.md` and inspect the existing code before editing. Work only on **[TASK]**. Do not assume a component is missing until you verify the repository.
>
> First report: (1) current implementation, (2) exact gaps, (3) files to change, (4) proposed flow, (5) risks/edge cases, and (6) acceptance tests. Then implement the smallest complete logical chunk with exact file paths and tests where appropriate. Preserve Java 21, the current Spring Boot/Cloud versions, database-per-service ownership, and existing API conventions. Do not change unrelated files, create duplicate services, expose secrets, or claim tests passed unless they were actually run.
>
> Explain the change in plain English, including the happy path, failure behavior, and what I should test. Stop after this task; do not proceed until I confirm the build/test result.

## 9. Definition of Done

- [ ] Expected behavior is documented.
- [ ] Existing code was inspected first.
- [ ] Code compiles.
- [ ] Relevant tests pass, or missing tests are explicitly recorded.
- [ ] Happy path was exercised.
- [ ] At least one important failure/validation path was exercised.
- [ ] Authorization/ownership was checked where relevant.
- [ ] Logs do not expose secrets/sensitive data.
- [ ] Database ownership and transaction boundaries remain correct.
- [ ] Documentation is updated.
- [ ] No unrelated files were changed unnecessarily.

## 10. Critical Do-Not-Do List

- Never let one service directly access another service's database.
- Never use `double` for money.
- Never trust client-supplied user identity headers.
- Never assume service-to-service calls share a database transaction.
- Never report success before money movement is complete.
- Never rely on Kafka exactly-once delivery; make consumers idempotent.
- Never remove constraints protecting uniqueness/concurrency without a reviewed replacement.
- Never publish secrets/tokens in logs or events.
- Never drop databases/topics or run destructive SQL without explicit need and backup.
- Never upgrade Spring versions to fix an unrelated problem.
- Never duplicate a service before checking what already exists.
- Never treat a timeout after consuming available Kafka records as proof of DLT failure.
- Never claim a test works without observed evidence.

## 11. Immediate Next Actions

1. **Baseline audit:** inspect the repository and mark roadmap items by actual status. Do not rebuild working services.
2. **End-to-end smoke test:** register/login → authenticated Gateway call → account/balance → transfer → transaction history → notification.
3. Fix the highest-impact gap in that journey before adding new architecture.
4. Verify transaction idempotency, concurrency, failure recovery, and ownership.
5. Complete audit and frontend integration based on actual gaps.
6. Add fraud rules only after the transaction flow is reliable.
7. Finish Docker setup, automated tests, observability, and documentation.

**Prompt for the next LLM session:**

> Inspect the current FinFlow repository and produce a status report against Sections 3 and 7 of `phaseA.md`. Do not modify files yet. List each service, its port/database, existing endpoints, compile status, and the single highest-priority missing item for the end-to-end user journey. Then recommend one logical implementation chunk.

## 12. Progress Log

Update after each accepted task with date, task, files changed, build/test result, and known limitation.

| Date | Task | Result | Next |
|---|---|---|---|
| 2026-10-15 | Notification idempotency and DLT checks | Replayed duplicate event was skipped; DLT contained malformed payload and prior `DLT_TEST` record; Notification Service restarted | Run baseline audit and end-to-end smoke test |

This log records evidence reported during development. Re-run relevant tests if code or configuration changes later.
