# FinFlow --- Implementation & Project Handbook

> **Purpose:** This document is the working source of truth for
> FinFlow's architecture, implementation status, service contracts,
> development workflow, AI scope, testing strategy, and remaining
> roadmap.
>
> **Status convention:**\
> - **Implemented / verified** means it was implemented and explicitly
> tested or observed in the development history.\
> - **Implemented, verification incomplete** means code/configuration
> exists but the latest end-to-end verification is not established.\
> - **Planned** means it is part of the target design, but should not be
> assumed to exist yet.
>
> This is a development project, not a production banking system. Do not
> use real money, real credentials, or real customer data.

**Last consolidated:** 10 October 2026\
**Repository:** `FinFlow/`\
**Primary language:** Java 21\
**Primary backend framework:** Spring Boot 4.1.1\
**Cloud framework:** Spring Cloud 2025.1.3\
**Development environment:** macOS, Apple Silicon, IntelliJ IDEA, Maven
Wrapper

------------------------------------------------------------------------

## 1. Project overview

FinFlow is an intelligent digital banking and payments platform being
developed as a set of independently runnable Spring Boot microservices.
Its core journey is user authentication, account access, money movement
between accounts, transaction history, and asynchronous downstream
processing.

The project is intended to demonstrate practical backend engineering
skills: API design, authentication and authorization, service discovery,
gateway routing, database-per-service ownership, transactional
consistency, compensating actions, Kafka event-driven communication,
observability, containerization, and a carefully bounded AI assistant.

### Product goals

1.  Users can register/sign in and obtain access tokens.
2.  Authenticated users can view their account and balance.
3.  Users can deposit, withdraw, and transfer funds in the development
    environment.
4.  Transfers record sender, receiver, amount, currency, reference,
    status, and timestamp.
5.  Users can view incoming and outgoing transaction history.
6.  Completed transactions publish events to Kafka.
7.  Notification, audit, and fraud services react asynchronously to
    events.
8.  A user-facing frontend presents account and transaction information.
9.  An AI assistant helps users understand activity and navigate the
    product, without being authorized to move money.
10. Services can be run locally and eventually packaged using Docker
    Compose.

### Non-goals and safety boundaries

-   FinFlow is not currently a regulated or production-ready banking
    system.
-   Current payment flows use development/test data and do not establish
    real payment-rail integration.
-   AI must never authorize, initiate, approve, reverse, or execute
    money movement.
-   AI must not be treated as the source of truth for balances,
    transaction status, account ownership, or fraud decisions.
-   A model response is not a security boundary. Backend authorization
    and business rules must enforce every sensitive action.
-   Secrets, tokens, database passwords, and personal financial data
    must not be committed to source control or copied into
    documentation.

------------------------------------------------------------------------

## 2. Technology stack

  -----------------------------------------------------------------------
  Area                    Technology              Current role
  ----------------------- ----------------------- -----------------------
  Language                Java 21 (Temurin)       Backend services

  Framework               Spring Boot 4.1.1       Service runtime and
                                                  dependency management

  Service communication / Spring Cloud 2025.1.3,  Service registration
  discovery               Eureka                  and discovery

  Edge routing            Spring Cloud Gateway    Routes external API
                          (WebFlux)               requests to services

  Security                Spring Security, JWT,   Authentication and
                          BCrypt                  endpoint authorization

  Data access             Spring Data JPA,        Persistence in
                          Hibernate               relational databases

  Database                MySQL                   Separate
                                                  schema/database per
                                                  service

  Messaging               Apache Kafka 4.0.0 in   Asynchronous events
                          Docker                  

  Kafka integration       Spring Kafka 4.1.1      Event producer and
                                                  consumer integration

  Cache                   Redis                   Candidate for caching
  (planned/integration to                         and short-lived data
  verify)                                         

  Frontend                React                   User-facing application
  (planned/current build                          
  status to verify)                               

  Build                   Maven Wrapper           Repeatable service
                                                  builds

  Local infrastructure    Docker / Docker Compose Kafka now; broader
                                                  containerization
                                                  planned

  IDE                     IntelliJ IDEA           Editing, running,
                                                  debugging

  AI                      LLM-powered assistant   Explanations,
                          (planned)               summaries, guidance; no
                                                  money movement
  -----------------------------------------------------------------------

Versions above describe the current development baseline, not a claim
that every service has identical dependencies or that every listed
technology is already integrated.

------------------------------------------------------------------------

## 3. High-level architecture

### 3.1 Target architecture

``` text
                          ┌───────────────────────────┐
                          │      React Frontend       │
                          │ Dashboard / Accounts / AI  │
                          └─────────────┬─────────────┘
                                        │ HTTPS in deployment
                                        ▼
                          ┌───────────────────────────┐
                          │ API Gateway :8080         │
                          │ routing / edge policies   │
                          └─────────────┬─────────────┘
                                        │
              ┌─────────────────────────┼─────────────────────────┐
              ▼                         ▼                         ▼
     ┌────────────────┐        ┌────────────────┐        ┌────────────────┐
     │ Auth Service   │        │ Account Service│        │ Transaction    │
     │ :8081          │        │ :8082          │◄───────►│ Service :8083  │
     │ tokens / users │        │ accounts       │        │ transfers      │
     └───────┬────────┘        └───────┬────────┘        └───────┬────────┘
             │                         │                         │
       finflow_auth              finflow_account          finflow_transaction
             │                         │                         │
             └─────────────────────────┴─────────────────────────┘
                                        │
                                        │ completed transaction event
                                        ▼
                              ┌──────────────────────┐
                              │ Kafka                │
                              │ finflow.transaction.  │
                              │ events               │
                              └──────────┬───────────┘
                                         │
                  ┌──────────────────────┼─────────────────────┐
                  ▼                      ▼                     ▼
        ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
        │ Notification     │  │ Fraud Service    │  │ Audit Service    │
        │ Service :8084    │  │ planned          │  │ planned          │
        └──────────────────┘  └──────────────────┘  └──────────────────┘

       Eureka Discovery Server :8761 supports service discovery.
       Redis is a planned supporting component; exact cache use is not finalized.
       AI Assistant communicates with authorized APIs/read models, not money-moving internals.
```

### 3.2 Architectural principles

-   **Database per service:** each service owns its data. Other services
    must not directly query another service's database.
-   **API ownership:** a service's API is the contract for other
    services; avoid shared database coupling.
-   **Gateway at the edge:** frontend requests should normally enter
    through the gateway. Internal endpoints must not be accidentally
    exposed through gateway routes.
-   **JWT identity:** user-facing APIs derive the current user identity
    from a verified token, not a user ID supplied in the request body.
-   **Internal service authentication:** privileged service-to-service
    endpoints require a distinct internal authentication mechanism, not
    merely a user token.
-   **Events for downstream reactions:** transaction completion is
    published to Kafka so notification, audit, and fraud processing can
    happen independently.
-   **Saga with compensation:** a cross-service transfer is not a single
    ACID transaction. The current flow uses a compensating refund if the
    receiver credit fails.
-   **AI is advisory:** all account facts and actions are checked by
    deterministic backend services.

### 3.3 Implemented gateway routes

The API Gateway runs on port `8080`. Current routes documented in the
development setup:

  -----------------------------------------------------------------------------
  Public route             Destination                  Path rewrite
  ------------------------ ---------------------------- -----------------------
  `/api/auth/**`           `lb://auth-service`          `/auth/...`

  `/api/transactions/**`   `lb://transaction-service`   `/transactions/...`

  `/api/accounts/**`       `lb://account-service`       `/accounts/...`
  -----------------------------------------------------------------------------

Internal Account Service routes under `/accounts/internal/**` are
intended for authenticated service-to-service calls and must not be
exposed as public gateway routes. Re-check gateway configuration
whenever adding a route.

------------------------------------------------------------------------

## 4. Service catalogue and contracts

The following sections describe current responsibilities and the
expected contract shape. A route or schema described as planned is not a
claim that the implementation already exists. Verify exact endpoint
mappings and response DTOs in each service before treating this table as
a formal OpenAPI contract.

### 4.1 Discovery Server

**Status:** Implemented / verified.

-   **Port:** `8761`
-   **Role:** Eureka server; service registry.
-   **Inputs:** Registration/heartbeat requests from service instances.
-   **Outputs:** Registry of available service instances and a
    dashboard.
-   **Own data:** Runtime registry state; no business database.
-   **Verification:** Dashboard was accessed and services registered.
    Transaction Service registered as `TRANSACTION-SERVICE` with status
    `UP`.
-   **Remaining:** Add health checks and containerized startup ordering
    for the full local stack.

### 4.2 API Gateway

**Status:** Implemented / verified for the routes listed above.

-   **Port:** `8080`
-   **Role:** External entry point and service routing.
-   **Inputs:** HTTP requests from frontend/API clients, usually
    carrying a user access token.
-   **Outputs:** Forwarded service responses.
-   **Own data:** None.
-   **Key behavior:** Routes requests to registered services using
    Eureka/LoadBalancer.
-   **Remaining:** Review centralized error handling, rate limiting,
    CORS, request correlation IDs, and production TLS. Do not move core
    business authorization exclusively into the gateway.

### 4.3 Auth Service

**Status:** Authentication and JWT/refresh-token work implemented; exact
complete endpoint inventory should be verified from controllers.

-   **Port:** `8081`
-   **Database:** `finflow_auth`
-   **Role:** Registration/login, password hashing, token
    creation/refresh, identity-related security.
-   **Technology:** Spring MVC, Spring Security, JPA, MySQL, BCrypt,
    JWT/JJWT.
-   **Inputs (conceptual):**
    -   Registration: user details and credentials validated by the
        service.
    -   Login: credentials.
    -   Refresh: a valid refresh token according to the implemented
        refresh flow.
-   **Outputs (conceptual):**
    -   Registration/login result.
    -   Short-lived access JWT and refresh token where appropriate.
    -   Authentication errors for invalid credentials/tokens.
-   **Security notes:** Roles include `CUSTOMER`, `ADMIN`, and `SUPPORT`
    in the existing design. Passwords must be stored as BCrypt hashes,
    never plaintext.
-   **Configuration:** uses environment variables for secrets/passwords.
    Do not copy their values into docs.
-   **Remaining:** Verify refresh-token rotation/revocation behavior,
    logout semantics, rate limiting, and role-specific endpoint
    policies.

### 4.4 User Service

**Status:** Planned / presence and exact current implementation not
verified in the latest status.

-   **Role:** User profile and user-facing profile information,
    separated from authentication concerns.
-   **Expected inputs:** Authenticated profile read/update requests.
-   **Expected outputs:** A minimal profile DTO; never password hashes
    or secrets.
-   **Expected data ownership:** A user/profile store if implemented as
    an independent service.
-   **Remaining:** Confirm whether this service is needed as a separate
    deployable component or whether profile functionality remains inside
    Auth for the first release. Avoid duplicating user identity data
    without a defined ownership rule.

### 4.5 Account Service

**Status:** Implemented / verified for core account operations and
internal service security.

-   **Port:** `8082`
-   **Database:** `finflow_account`
-   **Role:** Own accounts, account numbers, balances, currency, status,
    and account operations.
-   **Account fields known to exist:** `id`, `accountNumber`, `userId`,
    `balance`, `currency`, `status`, `createdAt`.
-   **Account statuses:** `ACTIVE`, `BLOCKED`, `CLOSED`.
-   **Operations implemented in the service history:** create account,
    retrieve account, retrieve account by user, deposit, withdraw,
    deposit to an account number, and refund to a user account.
-   **Inputs (conceptual):**
    -   Authenticated account read.
    -   Deposit/withdraw request with amount.
    -   Internal credit request with receiver account number and amount.
    -   Internal compensation/refund request with user ID and amount.
-   **Outputs (conceptual):** account DTO containing account number,
    balance, currency, status and other non-secret account metadata;
    operation result or a business error.
-   **Concurrency:** repository includes a pessimistic-lock lookup for
    updating an account balance.
-   **Security:** ordinary JWT authentication is used for user-facing
    endpoints. `/accounts/internal/**` is protected with `ROLE_SERVICE`
    and an internal JWT secret. The ordinary JWT filter skips these
    internal paths. A user token was tested against an internal endpoint
    and received `403`.
-   **Important hardening item:** the internal refund operation accepts
    a user ID. Production-grade design should constrain compensation by
    a transfer/operation reference and authorization policy, not permit
    arbitrary user-targeted refunds.

### 4.6 Transaction Service

**Status:** Core transfers/history and event producer implemented /
verified.

-   **Port:** `8083`
-   **Database:** `finflow_transaction`
-   **Role:** Orchestrate deposits/withdrawals/transfers, persist
    transaction records, expose transaction history, and publish
    completed-transaction events.
-   **Core transaction fields:**
    -   `id`
    -   `transactionReference` (UUID, unique and immutable)
    -   `senderUserId`, `senderAccountNumber`
    -   `receiverUserId`, `receiverAccountNumber`
    -   `type` (`DEPOSIT`, `WITHDRAW`, `TRANSFER`)
    -   `amount`, `currency`
    -   `status` (`PENDING`, `COMPLETED`, `FAILED`)
    -   `createdAt`
-   **Transfer input:** `receiverAccountNumber`, `amount`, `currency`.
    The sender is determined from the authenticated user and their
    account; callers must not choose their own sender user ID.
-   **Transfer behavior currently implemented:**
    1.  Resolve the sender's account.
    2.  Resolve the receiver's owner using the receiver account number.
    3.  Reject transfer to the sender's own account.
    4.  Save a transaction in `PENDING`.
    5.  Withdraw from the sender.
    6.  Credit the receiver using an internal service token.
    7.  If receiver credit fails, attempt to refund the sender and mark
        the transaction `FAILED`.
    8.  On success, save `COMPLETED`.
    9.  Publish a `TRANSACTION_COMPLETED` event to Kafka.
-   **History input:** authenticated request; user ID comes from the
    verified authentication principal.
-   **History output:** a user-facing transaction response with
    `transactionReference`, `type`, `direction` (`OUTGOING`/`INCOMING`),
    `amount`, `currency`, masked `counterpartyAccount`, `status`, and
    `createdAt`.
-   **Verified history behavior:** sender sees outgoing transfer;
    receiver sees incoming transfer; both are associated with the same
    transaction reference.
-   **Event producer:** topic `finflow.transaction.events`, Kafka key is
    the transaction reference string. Event DTO includes `eventId`,
    `transactionReference`, `eventType`, sender/receiver user IDs and
    account numbers, amount, currency, transaction type, and
    `occurredAt`.
-   **Remaining/high priority:**
    -   Add idempotency keys for transfer requests and enforce
        uniqueness.
    -   Add a transactional outbox so a committed database transaction
        cannot silently lose its Kafka event if publication fails.
    -   Make compensation retryable and observable.
    -   Replace broad exception handling with explicit domain errors.
    -   Validate amount positivity, currency support, account status,
        and account currency compatibility.
    -   Reconcile transaction status when a saga step or compensation
        has an ambiguous result.
    -   Avoid exposing unmasked account numbers in logs or user
        responses.
    -   Review deposit/withdraw transaction semantics; early code
        represented sender and receiver as the same account for some
        non-transfer operations.

### 4.7 Notification Service

**Status:** In progress; build failed at the last reported attempt on 10
October 2026. Do not treat it as running yet.

-   **Target port:** `8084`
-   **Target topic:** `finflow.transaction.events`
-   **Target consumer group:** `finflow-notification-group`
-   **Role:** Consume transaction-completed events and produce
    notifications for sender and receiver.
-   **Expected input:** JSON representation of
    `TransactionCompletedEvent`.
-   **Expected output for initial version:** structured log entries such
    as transfer successful for sender and amount received for receiver.
-   **Files created:** `TransactionCompletedEvent.java`,
    `KafkaConsumerConfig.java`, and `TransactionEventConsumer.java` were
    created during the current implementation attempt.
-   **Last known errors:**
    -   `JacksonJsonDeserializer` compilation cannot resolve Jackson 3
        classes such as `tools.jackson.core.type.TypeReference` and
        `tools.jackson.databind.JavaType`.
    -   Lombok-generated getters and the `@Slf4j` logger were also
        reported missing in that build.
-   **Next implementation step:** keep Lombok if desired, verify
    annotation processing/dependency setup, add the correct Jackson 3
    dependency compatible with Spring Boot 4.1.1 / Spring Kafka 4.1.1,
    and rebuild. Confirm dependency compatibility rather than changing
    serializers at random.
-   **Future work:** persistent notification records, retry/dead-letter
    handling, email or SSE delivery, user notification preferences, and
    duplicate-event handling.

### 4.8 Fraud Service

**Status:** Planned.

-   **Role:** Consume transaction events and calculate risk signals or
    flag suspicious behavior.
-   **Expected input:** transaction events and, later, authorized
    read-only context.
-   **Expected output:** risk score/reason codes and a fraud alert or
    review event.
-   **Important rule:** an AI/ML risk signal should not independently
    move funds or bypass deterministic policy. Define the policy and
    human/automated decision boundary explicitly.
-   **Remaining:** define rule-based baseline first, event schema,
    thresholds, persistence, review flow, and false-positive handling.

### 4.9 Audit Service

**Status:** Planned.

-   **Role:** Consume relevant domain/security events and maintain an
    audit trail.
-   **Expected input:** transaction lifecycle events and selected
    security/business events.
-   **Expected output:** append-only audit records with event time,
    actor/service, action, target reference, and outcome.
-   **Requirements:** audit records should be tamper-resistant by
    design, access controlled, and avoid storing credentials/tokens.
    Define retention and access policies before claiming compliance.
-   **Remaining:** event catalogue, schema, append-only storage, search
    API and tests.

### 4.10 AI Assistant Service / capability

**Status:** Planned; no production AI integration should be assumed
complete.

-   **Role:** Natural-language explanation, navigation, transaction
    summaries, and financial activity insights using authorized data.
-   **Inputs:** user's natural-language question plus the minimum
    authorized, relevant data returned by backend APIs.
-   **Outputs:** explanatory text, summaries, categorized observations,
    or links/actions that the user can choose to follow.
-   **Must not do:** call internal balance mutation endpoints, decide
    authentication/authorization, fabricate transaction records,
    guarantee fraud detection, or initiate/approve/execute transfers.
-   **Integration approach:** backend-owned tools/read-only APIs; strict
    allowlist; schema-validated arguments; user identity and
    authorization enforced by backend; redact data before model calls;
    record safe audit metadata; require explicit user confirmation for
    any future user-initiated action and keep money movement in
    deterministic APIs.
-   **Remaining:** define user stories, tool schemas, privacy policy,
    model/provider selection, prompt-injection defenses,
    refusal/uncertainty behavior, evaluation set, latency/cost limits,
    and tests.

------------------------------------------------------------------------

## 5. Important API contract snapshots

These are current design/implementation snapshots, not a replacement for
OpenAPI specifications. Confirm exact HTTP methods, status codes,
validation annotations, and error DTOs in source before using as an
external API contract.

### 5.1 Transfer

-   **Gateway route:** `POST /api/transactions/transfer`
-   **Gateway rewrite:** `/transactions/transfer`
-   **Authentication:** user access token in
    `Authorization: Bearer <token>`
-   **Request body:**

``` json
{
  "receiverAccountNumber": "receiver-account-number",
  "amount": 100.00,
  "currency": "INR"
}
```

-   **Sender identity:** taken from the authenticated principal, not
    from the request.
-   **Successful response shape:** a `TransactionResponse` with
    transaction reference, type, direction, amount, currency, masked
    counterparty account, status, and creation time.
-   **Expected successful HTTP status in current controller:**
    `201 Created`.
-   **Expected business failures:** invalid/unknown receiver,
    own-account transfer, insufficient funds, blocked/closed account,
    invalid amount, receiver credit failure. Exact error mappings need a
    consolidated exception-handling review.

### 5.2 Transaction history

-   **Gateway route:** `GET /api/transactions/me`
-   **Authentication:** user access token.
-   **Input:** no caller-supplied user ID is required; identity comes
    from the authenticated principal.
-   **Output:** list of the current user's incoming and outgoing
    transactions, newest first, with direction and masked counterparty
    account.

### 5.3 Account operations

-   **Gateway route family:** `/api/accounts/**`
-   **Account service route family:** `/accounts/**`
-   **User inputs/outputs:** inspect current controller/request/response
    DTOs for exact paths and fields. Account responses include account
    metadata such as account number, balance, currency and status.
-   **Internal endpoints:** `/accounts/internal/**` require an internal
    service JWT with `ROLE_SERVICE` and must not be exposed through the
    public gateway.

### 5.4 Authentication

-   **Gateway route family:** `/api/auth/**`
-   **Auth service route family:** `/auth/**`
-   **Expected inputs:** credentials for login/registration and refresh
    token for refresh flow.
-   **Expected outputs:** authentication result and JWT/refresh token
    fields according to the current DTOs.
-   **Exact schemas:** inspect `AuthController` and its request/response
    classes before publishing API documentation.

### 5.5 Kafka event contract

**Topic:** `finflow.transaction.events`\
**Key:** transaction reference string\
**Event type:** `TRANSACTION_COMPLETED`

Example based on a real development consumer result:

``` json
{
  "amount": 100,
  "currency": "INR",
  "eventId": "example-event-uuid",
  "eventType": "TRANSACTION_COMPLETED",
  "occurredAt": "2026-10-05T15:21:39.351122Z",
  "receiverAccountNumber": "receiver-account-number",
  "receiverUserId": 5,
  "senderAccountNumber": "sender-account-number",
  "senderUserId": 4,
  "transactionReference": "example-transaction-uuid",
  "transactionType": "TRANSFER"
}
```

The UUIDs and account numbers in this example are illustrative. Do not
put real credentials or customer details in example events.

**Event delivery caveat:** Kafka send is asynchronous and is not yet
protected by a transactional outbox. A successful HTTP transfer response
and a successful database write do not, by themselves, prove durable
event publication under every failure condition.

------------------------------------------------------------------------

## 6. Data ownership and current databases

  ---------------------------------------------------------------------------------
  Database/schema         Owner                   Known responsibility
  ----------------------- ----------------------- ---------------------------------
  `finflow_auth`          Auth Service            Authentication/identity-related
                                                  persistence

  `finflow_account`       Account Service         Accounts and balances

  `finflow_transaction`   Transaction Service     Transaction records and history

  Notification store      Notification Service    Planned; not yet established

  Audit store             Audit Service           Planned

  Fraud store             Fraud Service           Planned
  ---------------------------------------------------------------------------------

Do not allow one service to connect directly to another service's
database to perform business operations. Use API calls or domain events.
Each service's schema should evolve through that service's migrations.
`ddl-auto=update` is currently used in development configuration; a
production-like setup should move to controlled schema migrations such
as Flyway or Liquibase.

------------------------------------------------------------------------

## 7. Saga, consistency, and money-movement safeguards

### Current transfer saga

``` text
Create PENDING transaction
          |
          v
Withdraw sender funds
          |
          v
Credit receiver account
     /           \
  success        failure
    |               |
    v               v
Mark COMPLETED   Attempt compensation/refund
    |               |
    v               v
Publish event    Mark FAILED if refund succeeds
                  otherwise retain unresolved state
```

### What this does and does not guarantee

-   It coordinates a cross-service workflow but does **not** create a
    distributed ACID transaction.
-   The receiver-credit failure path attempts to refund the sender.
-   If the refund also fails, the transfer may remain `PENDING` and
    needs reconciliation/retry.
-   A process crash between a database write and a remote API call can
    leave ambiguous state.
-   A process crash after saving `COMPLETED` but before Kafka
    publication can leave a completed transaction without an event.
-   Repeated HTTP requests or Kafka delivery can create duplicates
    unless idempotency is enforced.

### Required hardening before any production claim

1.  Idempotency key on transfer requests and durable uniqueness.
2.  Idempotent Account Service operations tied to a stable
    operation/reference ID.
3.  Durable saga state machine and retryable compensations.
4.  Transactional outbox for publishing committed domain events.
5.  Consumer deduplication using `eventId` or another stable event
    identity.
6.  Reconciliation job for stuck `PENDING` operations.
7.  Currency/scale validation, positive amounts, account status
    validation and authorization checks.
8.  Explicit domain exceptions and stable API error schema.
9.  Audit trail for money movement and privileged actions.
10. Integration and failure-injection tests for each saga step.

------------------------------------------------------------------------

## 8. Kafka and Docker state

### Kafka broker

-   **Container:** `finflow-kafka`
-   **Image:** `apache/kafka:4.0.0`
-   **Local bootstrap server:** `localhost:9092`
-   **Topic:** `finflow.transaction.events`
-   **Partitions:** 3
-   **Replication factor:** 1
-   **Persistence:** named Docker volume `kafka_data`
-   **Status:** broker was started and topic created; a console
    producer/consumer test succeeded.

Current root `docker-compose.yml` was created for Kafka. Its important
shape is:

``` yaml
services:
  kafka:
    image: apache/kafka:4.0.0
    container_name: finflow-kafka
    ports:
      - "9092:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_NUM_PARTITIONS: 3
    volumes:
      - kafka_data:/var/lib/kafka/data

volumes:
  kafka_data:
```

This setup is intended for local development. A single broker with
replication factor 1 is not highly available. The advertised listener
`localhost:9092` is convenient for host-run Spring services;
containerized application services will need an internal advertised
listener/appropriate bootstrap address.

### Verified producer result

The Transaction Service published a JSON event to
`finflow.transaction.events`. A console consumer using
`--from-beginning` received both an earlier manual test message and a
`TRANSACTION_COMPLETED` event from a successful transfer. This verified
the producer-to-broker path. It did **not** yet verify the new
Notification Service consumer because that service's build was failing
at the last report.

------------------------------------------------------------------------

## 9. Security model

### Current security mechanisms

-   User authentication with JWT.
-   Refresh-token flow implemented in Auth Service.
-   Password hashing with BCrypt.
-   Account operations protected by Spring Security.
-   Internal Account Service endpoints protected by a separate internal
    JWT secret and `ROLE_SERVICE`.
-   User JWT rejected by an internal endpoint in a test (`403`).
-   User identity for transaction history is taken from authentication,
    not an arbitrary request parameter.
-   Account numbers are masked in transaction history responses.

### Security requirements still to complete

-   Keep all secrets in environment variables or a secret manager; never
    commit them.
-   Rotate any credential that was ever committed or exposed.
-   Verify JWT issuer, audience, expiry, signing algorithm and key
    management as appropriate to the design.
-   Define refresh token rotation, revocation, logout and reuse
    detection.
-   Validate ownership for every account read/write.
-   Ensure internal endpoints are not gateway-routable.
-   Use TLS in deployed environments and restrict CORS.
-   Add rate limits and brute-force protection to authentication.
-   Redact authorization headers, tokens and sensitive account data from
    logs.
-   Add authorization and negative tests for CUSTOMER/ADMIN/SUPPORT.
-   Treat AI tool calls as untrusted input; authorize every tool
    operation server-side.

------------------------------------------------------------------------

## 10. AI integration plan

AI is intended to make FinFlow more useful and explainable, not to
replace its deterministic backend.

### 10.1 Proposed user-facing AI features

1.  **Transaction explanation:** explain a transaction's recorded
    status, amount, date and counterparty using authorized transaction
    data.
2.  **Spending summaries:** summarize categories/trends once reliable
    transaction categorization and data are available.
3.  **Natural-language search:** answer questions such as "show my
    recent transfers" by calling a read-only, authenticated API tool.
4.  **Financial literacy:** explain common banking concepts and the
    meaning of FinFlow UI statuses.
5.  **Fraud awareness:** explain why a transaction was flagged using a
    reason code from the fraud service; do not independently declare a
    transaction fraudulent.
6.  **Support/navigation:** guide users to relevant product screens or
    help content.
7.  **Developer-only assistance:** help developers analyze logs, explain
    architecture, generate tests, and document implementation.

These are target capabilities; they should not be presented as already
implemented.

### 10.2 Example AI flow

``` text
User asks: "Why did my last transfer fail?"
          |
          v
AI orchestration layer
          |
          v
Select an allowed read-only tool
          |
          v
Backend validates JWT and user ownership
          |
          v
Transaction Service returns authorized transaction/status/error data
          |
          v
AI explains the returned facts in plain language
          |
          v
User receives explanation with uncertainty where appropriate
```

### 10.3 Tool and permission policy

**Potential read-only tools:** - `getMyAccountSummary` -
`getMyRecentTransactions` - `getTransactionDetails(reference)` with
ownership checks - `getMySpendingSummary` when the data pipeline
exists - `getFraudFlagExplanation(reference)` once Fraud Service exists

**Forbidden AI tools:** - `withdrawMoney` - `transferMoney` -
`depositMoney` - `approveTransfer` - `refundTransfer` - direct database
access - arbitrary internal HTTP calls

If the product later offers an AI-assisted transfer form, the assistant
may help prefill a draft for the user to review, but the user must
submit through the normal authenticated API and all normal validation,
authorization, idempotency, and confirmation rules must apply. The model
itself must not execute the transfer.

### 10.4 AI safety, privacy and reliability

-   Send only the minimum data needed to answer.
-   Never send passwords, JWTs, refresh tokens, secret keys or full
    unnecessary account identifiers to the model.
-   Mask account numbers and redact PII where feasible.
-   Never trust instructions found in transaction descriptions, uploaded
    documents, or other user-controlled content; treat them as data, not
    system instructions.
-   Use an allowlist of typed tools with strict schemas and server-side
    authorization.
-   Keep the backend as the source of truth. The model may summarize
    facts but cannot invent missing transactions or alter records.
-   State when data is unavailable or uncertain.
-   Log tool names, authorization outcomes and safe request metadata,
    not raw secrets or unnecessary prompt/customer content.
-   Add evaluations for factuality, authorization leakage, prompt
    injection, unsafe tool selection, and incorrect financial advice.
-   Add latency, token-cost, timeout and provider-failure handling.
-   AI-generated insights must not be presented as guaranteed
    investment, credit, fraud or financial advice.

------------------------------------------------------------------------

## 11. Development methodology and working style

FinFlow is developed in small, understandable, testable increments
rather than generating the entire system at once.

### Standard development loop

1.  **Define the feature:** state the user-visible behavior and
    acceptance criteria.
2.  **Inspect current implementation:** check existing services, DTOs,
    configuration and tests before adding duplicates.
3.  **Design the contract:** identify input, output, ownership, errors,
    service dependencies and security.
4.  **Implement a logical chunk:** group related files/configuration
    changes so the feature is coherent.
5.  **Compile once:** after the logical chunk, run Maven build rather
    than compiling after every tiny edit.
6.  **Start required dependencies:** MySQL, Kafka, Eureka and upstream
    services as needed.
7.  **Run the service:** inspect startup and registration logs.
8.  **Test the full flow:** use a real local API request and inspect
    database/event/consumer results where relevant.
9.  **Test failures:** invalid inputs, unauthorized calls, unavailable
    dependencies and compensation behavior.
10. **Review and document:** explain what changed, why, trade-offs and
    remaining gaps.
11. **Commit a coherent change:** stage only intended files; inspect
    `git status` and diff before committing.

### How AI is used in development

-   AI helps plan, explain, implement and review, but the developer runs
    and verifies the code locally.
-   Generated code must be inspected for compatibility with the
    project's actual Spring Boot/Spring Cloud/Spring Kafka versions.
-   Logs and stack traces are the basis for debugging; do not blindly
    apply unrelated fixes.
-   Preserve learning: understand why a class, dependency, annotation or
    configuration exists.
-   Prefer one complete test after a logical batch over repeated partial
    test cycles, unless a failure requires narrowing the issue.
-   Record implementation status truthfully: code written is not the
    same as a feature verified end-to-end.

### Definition of done for a backend feature

A feature is not "done" just because it compiles. Minimum completion
means: - compiles; - service starts; - expected happy path passes; -
relevant negative/authorization path passes; - data/state/event behavior
is verified; - errors are understandable; - secrets are not exposed; -
documentation and Git status are updated.

------------------------------------------------------------------------

## 12. Current implementation status

This status is consolidated from the latest recorded development
history. Recheck the repository before relying on it as an exact live
inventory.

  -------------------------------------------------------------------------------
  Component / capability  Status                  Evidence / notes
  ----------------------- ----------------------- -------------------------------
  Repository and          Implemented             `FinFlow/` monorepo,
  architecture docs                               `architecture.md` exists

  Eureka Discovery Server Implemented / verified  Port `8761`; registry responded
                                                  successfully

  API Gateway             Implemented / verified  Port `8080`; auth, transaction,
                                                  account routes

  Auth Service            Implemented / verified  JWT, refresh flow, BCrypt;
                                                  secrets moved to environment
                                                  variables

  Account Service         Implemented / verified  Account operations, pessimistic
                          for core flow           locking, internal JWT security

  Transaction Service     Implemented / verified  Transfer saga, transaction
                          for core                persistence, incoming/outgoing
                          transfer/history        history

  Sender/receiver history Implemented / verified  User 4 → User 5 transfer showed
  mapping                                         OUTGOING/INCOMING

  Internal service        Implemented / verified  Internal endpoint rejects user
  authentication                                  token with `403`

  Kafka broker and topic  Implemented / verified  Docker Kafka;
                                                  `finflow.transaction.events`, 3
                                                  partitions

  Transaction event       Implemented / verified  Console consumer received
  producer                                        `TRANSACTION_COMPLETED` JSON

  Notification Service    In progress / build     Jackson 3 classes missing;
                          failing at last report  Lombok symbols also reported
                                                  missing

  User Service as         Needs inventory check   Planned in target architecture
  separate service                                

  Fraud Service           Planned                 No verified implementation

  Audit Service           Planned                 No verified implementation

  Redis integration       Planned / verify actual Technology selected, current
                          usage                   use not established here

  React frontend          Planned / verify        User-facing UI is a target
                          current state           

  AI assistant            Planned                 Read-only, permissioned tools;
                                                  no money movement

  Transactional outbox    Not implemented / high  Needed for DB-to-Kafka
                          priority                reliability

  Idempotency and         Incomplete / high       Needed for robust transfer
  reconciliation          priority                workflow

  Full Docker Compose     In progress             Compose currently includes
  stack                                           Kafka; other services not
                                                  confirmed containerized

  CI/CD and deployment    Planned                 Not established

  Comprehensive automated Incomplete / needs      End-to-end manual tests exist
  tests                   inventory               for core transfer
  -------------------------------------------------------------------------------

### Verified development transfer scenario

A successful development transfer from User 4 to User 5 was tested: -
Transfer amount: `100 INR`. - Sender and receiver balances changed as
expected. - Transaction status was `COMPLETED`. - Sender history showed
`OUTGOING`; receiver history showed `INCOMING`. - Kafka console consumer
received a matching `TRANSACTION_COMPLETED` event.

Old malformed test rows existed in the transaction database from earlier
experiments. Do not treat all historic rows as valid test fixtures; use
clean, controlled test data for future automated tests.

------------------------------------------------------------------------

## 13. Remaining roadmap and suggested order

### Phase A --- Finish Kafka consumer

-   [ ] Resolve Notification Service Jackson 3 dependency mismatch.
-   [ ] Verify Lombok annotation processing or use explicit methods if
    needed.
-   [ ] Build and start Notification Service on `8084`.
-   [ ] Verify Eureka registration.
-   [ ] Perform a new transfer and confirm the consumer logs
    sender/receiver notifications.
-   [ ] Test invalid/unknown event types and consumer restart behavior.

### Phase B --- Make event processing reliable

-   [ ] Add `eventId` deduplication strategy.
-   [ ] Add consumer retry/backoff and dead-letter topic strategy.
-   [ ] Implement transactional outbox in Transaction Service.
-   [ ] Define event schema evolution/versioning.
-   [ ] Add correlation IDs and safe structured logs.

### Phase C --- Harden money movement

-   [ ] Add transfer idempotency keys.
-   [ ] Make account operations idempotent per operation reference.
-   [ ] Add saga state transitions and recovery/reconciliation.
-   [ ] Improve compensation safety and authorization.
-   [ ] Add domain-specific errors and centralized exception responses.
-   [ ] Test simultaneous transfers, insufficient funds, invalid
    receiver, receiver failure, refund failure, and duplicate request.

### Phase D --- Build downstream services

-   [ ] Notification persistence and delivery channels (email/SSE,
    chosen deliberately).
-   [ ] Audit consumer and append-only audit storage.
-   [ ] Rule-based Fraud Service and risk/review event.
-   [ ] Define cross-service event contracts and ownership.

### Phase E --- User experience

-   [ ] Confirm/create React application structure.
-   [ ] Sign-in/registration flow.
-   [ ] Account overview and balances.
-   [ ] Transfer form with validation and clear status.
-   [ ] Transaction history with incoming/outgoing indicators.
-   [ ] Notification view.
-   [ ] Accessible error/loading/empty states.

### Phase F --- AI assistant

-   [ ] Define first narrow use case (recommended starting point:
    explain a transaction or summarize recent activity).
-   [ ] Build a backend AI orchestration layer with read-only tools.
-   [ ] Add authentication, ownership enforcement, data minimization and
    redaction.
-   [ ] Add prompt-injection and tool-authorization tests.
-   [ ] Evaluate factuality, refusal, latency and cost.
-   [ ] Ensure no money-moving tool is available to the model.

### Phase G --- Production-like engineering

-   [ ] Complete Docker Compose for dependencies and services.
-   [ ] Use database migrations.
-   [ ] Add health/readiness probes and metrics.
-   [ ] Add centralized logs, trace/correlation IDs and dashboards.
-   [ ] Add CI build/test pipeline.
-   [ ] Add integration/contract tests.
-   [ ] Configure deployment secrets and TLS.
-   [ ] Document backup/restore, retention, threat model and operational
    runbooks.

------------------------------------------------------------------------

## 14. Testing strategy

### Unit tests

-   Amount/currency validation.
-   Transaction mapping and masking.
-   State transitions and business rules.
-   Event mapping.
-   Fraud rules when implemented.

### Integration tests

-   Repository locking and persistence.
-   JWT authentication/authorization.
-   Account internal endpoint security.
-   Kafka producer serialization and consumer deserialization.
-   Consumer behavior for valid, invalid, duplicate and malformed
    events.

### End-to-end tests

1.  Register/login and obtain token.
2.  Read own account.
3.  Transfer from account A to account B.
4.  Verify sender and receiver balances.
5.  Verify transaction history for both users.
6.  Verify a `COMPLETED` transaction event is published.
7.  Verify Notification Service consumes it.
8.  Verify unauthorized users cannot read or mutate another user's
    account.
9.  Simulate receiver credit failure and verify compensation.
10. Simulate compensation failure and verify recovery/reconciliation
    behavior.

### Reliability and security tests

-   Duplicate transfer request.
-   Kafka unavailable at event-publication time.
-   Notification consumer restart/rebalance.
-   Invalid/expired user JWT.
-   User JWT sent to internal endpoint.
-   Account blocked/closed.
-   Negative, zero, oversized or unsupported-currency amount.
-   Concurrent transfers from the same account.
-   Secrets absent from logs and Git history.

------------------------------------------------------------------------

## 15. Local development and operations

### Typical local dependencies

-   MySQL available on `localhost:3306`.
-   Kafka available on `localhost:9092`.
-   Eureka on `localhost:8761`.
-   Services use ports `8080`--`8084` as listed above.

### Startup order (manual development)

1.  Start MySQL.
2.  Start Kafka using the root `docker-compose.yml`.
3.  Start Discovery Server.
4.  Start Auth and Account Services.
5.  Start Transaction Service.
6.  Start Notification Service after its build is fixed.
7.  Start API Gateway.
8.  Start frontend when implemented/available.

Services may retry registration or dependency connections; check actual
logs and health rather than assuming a process is ready solely because
the IDE launched it.

### Useful commands

From the repository root:

``` bash
docker compose up -d kafka
docker ps
```

Build an individual service from its directory:

``` bash
./mvnw clean package -DskipTests
```

Inspect Kafka messages:

``` bash
docker exec -it finflow-kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic finflow.transaction.events \
  --from-beginning
```

Before committing:

``` bash
git status
git diff
```

Do not paste secret values into chat or terminal screenshots. Redact
access tokens, refresh tokens, passwords, internal secrets and sensitive
personal data from logs.

------------------------------------------------------------------------

## 16. Decisions that must remain explicit

1.  **User Service boundary:** decide whether profile management is a
    separate service now or remains inside Auth until needed.
2.  **Event reliability:** use an outbox before describing DB writes and
    Kafka events as atomic/reliable.
3.  **Notification delivery:** choose persistent in-app notifications,
    SSE, email or a staged combination. Logging alone is only a
    prototype.
4.  **Fraud decisions:** define whether a score triggers alert, review
    or block. AI must not silently override policy.
5.  **AI deployment:** choose provider/model, data retention policy,
    tool protocol and privacy boundary only after defining the first use
    case.
6.  **API contract:** publish OpenAPI specifications from the actual
    controllers and DTOs rather than relying on this overview.
7.  **Database migrations:** move from development schema auto-update to
    versioned migrations before deployment.
8.  **Production readiness:** a working demo is not proof of
    financial-grade correctness, security, resilience or regulatory
    compliance.

------------------------------------------------------------------------

## 17. Glossary

-   **JWT:** signed token used to convey authenticated identity/claims.
-   **Refresh token:** credential used to obtain a new access token
    according to the auth design.
-   **Eureka:** service registry for discovery.
-   **API Gateway:** edge service routing external requests.
-   **Kafka topic:** named event stream.
-   **Consumer group:** group of consumers coordinated to read a topic's
    partitions.
-   **Idempotency:** repeating the same logical request does not
    duplicate its effect.
-   **Saga:** sequence of local transactions across services with
    compensating actions for failures.
-   **Compensation:** a follow-up action intended to counteract a prior
    successful step; not the same as a database rollback.
-   **Transactional outbox:** pattern that records a domain change and
    an event in the same local database transaction, then publishes the
    event asynchronously.
-   **Dead-letter topic:** destination for events that cannot be
    processed successfully after the retry policy.
-   **Database per service:** each service owns its schema/data and
    exposes it through contracts.
-   **Read-only AI tool:** backend operation that retrieves authorized
    data without mutating financial state.

------------------------------------------------------------------------

## 18. How to maintain this document

Update this file whenever a feature meaningfully changes.

For each feature, record: - date and status; - service/files changed; -
endpoint/event contract; - input and output examples with fake data; -
security and authorization behavior; - tests run and their result; -
known limitations/failure cases; - next step and Git commit reference,
if committed.

Do not mark a feature **Implemented / verified** until its meaningful
behavior has been run and checked. Keep planned design separate from
verified implementation.

**FinFlow's guiding rule:** build a small, secure, observable slice;
understand it; test the whole flow; then expand it. AI accelerates
development and improves user explanations, but deterministic backend
code remains responsible for identity, authorization, balances,
transaction state, and all money movement.
