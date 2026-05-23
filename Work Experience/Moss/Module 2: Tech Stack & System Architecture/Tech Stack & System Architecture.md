# Step 1: Tech Stack & System Architecture

Let me build this layer by layer, starting from the big picture and drilling down.

---

## The Big Picture — What Kind of System Are We Building?

A corporate spend management platform at Series B/C stage serving 5,000+ SMEs. This means:

- **Multiple distinct business domains** (Cards, Expenses, Invoices, Payments, Users) that need to evolve independently → Microservices make sense
- **Not FAANG scale** — we're not handling millions of requests per second. But we need reliability because this is **financial data** — money movement, accounting exports, audit trails
- **Speed of iteration matters** — Series B/C means the product is still being shaped. Teams need to ship independently without stepping on each other
- **EU-based** — GDPR compliance, data residency requirements influence infrastructure choices

So the architecture is: **microservices, but pragmatic ones**. Not 50 nano-services. Maybe 10-15 well-scoped services. Each owned by a team.

---

## System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENTS                                 │
│          Web App (React)  │  Mobile App (iOS/Android)           │
└─────────────────┬───────────────────────────────────────────────┘
                  │ HTTPS
                  ▼
┌──────────────────────────────────────────────────────────────────┐
│                      API GATEWAY                                 │
│   - Single entry point for all clients                           │
│   - JWT validation (once, centrally)                             │
│   - Request routing to downstream services                       │
│   - Rate limiting                                                │
│   - Spring Cloud Gateway                                         │
└──────┬──────────┬──────────┬──────────┬──────────┬───────────────┘
       │          │          │          │          │
       ▼          ▼          ▼          ▼          ▼
┌──────────┐ ┌─────────┐ ┌────────┐ ┌───────┐ ┌──────────────┐
│  Auth    │ │  Card   │ │Expense │ │Invoice│ │  Payment     │
│ Service  │ │ Service │ │   &    │ │  &    │ │  Service     │
│          │ │         │ │Reimb.  │ │  AP   │ │              │
│          │ │         │ │Service │ │Service│ │              │
└──────────┘ └─────────┘ └────────┘ └───────┘ └──────────────┘
       │          │          │          │          │
       └──────────┴──────────┴──────────┴──────────┘
                             │
              ┌──────────────┼──────────────┐
              ▼              ▼              ▼
        ┌──────────┐  ┌──────────┐  ┌──────────────┐
        │  Kafka   │  │  Redis   │  │Config Server │
        │ (Events) │  │ (Cache)  │  │(Spring Cloud)│
        └──────────┘  └──────────┘  └──────────────┘
              │
    ┌─────────┴──────────┐
    ▼                    ▼
┌────────────┐    ┌────────────┐
│Notification│    │ Accounting │
│  Service   │    │Integration │
│            │    │  Service   │
└────────────┘    └────────────┘
```

---

## Now Let's Go Layer by Layer

---

### Layer 1 — Frontend (Not Your Concern, But Good to Know)

| What | Technology | Why |
|------|-----------|-----|
| Web App | React + TypeScript | Industry standard for fintech dashboards |
| Mobile | React Native or native iOS/Android | Cross-platform for smaller team |

You don't work here. But knowing it helps when an interviewer asks "how does a finance manager approve an invoice?" — you can trace it from click to database.

---

### Layer 2 — API Gateway

**Technology: Spring Cloud Gateway**

This is the single front door. Every request from web/mobile hits here first.

**What it does:**
- Routes `/api/expenses/**` → Expense Service
- Routes `/api/invoices/**` → Invoice & AP Service
- Routes `/api/cards/**` → Card Service
- **Validates JWT once centrally** — individual services don't re-validate, they just read the `X-User-Id` and `X-User-Role` headers that gateway attaches
- Rate limiting per company/user
- Request logging for audit trail

**Why not just let clients call services directly?**
- Every service would need its own auth logic — duplicated everywhere
- Clients would need to know 10 different service URLs — nightmare for frontend
- You can't rate-limit or log centrally

This maps directly to what you studied in your Microservices notes — Lecture 7.

---

### Layer 3 — Core Microservices

Here are the services, who owns them, and what they do:

```
┌──────────────────────────────────────────────────────────┐
│ SERVICE          │ RESPONSIBILITY                        │
├──────────────────────────────────────────────────────────┤
│ Auth Service     │ Login, JWT generation, user mgmt      │
│ Card Service     │ Card issuance, limits, transactions   │
│ Expense &        │ ← YOUR TEAM                           │
│ Reimbursement    │ Expense submission, receipts,         │
│ Service          │ approval workflows, reimbursements    │
│ Invoice & AP     │ ← YOUR TEAM                           │
│ Service          │ Supplier invoices, AP workflows,      │
│                  │ payment runs, e-invoicing             │
│ Payment Service  │ SEPA transfers, multi-currency,       │
│                  │ actual money movement                 │
│ Notification     │ Email, in-app, Slack alerts           │
│ Service          │                                       │
│ Accounting       │ DATEV, Xero, NetSuite exports,        │
│ Integration Svc  │ 2-way sync                            │
│ User & Org       │ Company setup, teams, roles,          │
│ Service          │ budget centers                        │
└──────────────────────────────────────────────────────────┘
```

**Why two services for your team (Expense + Invoice)?**

At Series B/C, you don't split every tiny thing into its own service — that creates too much operational overhead for the team size. Expense reimbursements and accounts payable share:
- Similar approval workflow logic
- Same currency handling
- Same accounting export requirements
- Similar notification patterns

Splitting them later is easy. Combining two prematurely split services is painful. This is a pragmatic Series B decision.

---

### Layer 4 — Technology Stack Per Service

Every backend service follows this stack:

```
┌─────────────────────────────────────────────┐
│           EACH MICROSERVICE                 │
│                                             │
│  Language:     Java 17                      │
│  Framework:    Spring Boot 3.x              │
│  ORM:          Spring Data JPA + Hibernate  │
│  Database:     PostgreSQL (per service)     │
│  Migrations:   Flyway                       │
│  Build:        Maven                        │
│  Container:    Docker                       │
└─────────────────────────────────────────────┘
```

**Why Java + Spring Boot?**
- Strong typing — critical for financial data (you don't want loose types around money)
- Spring's transaction management (`@Transactional`) is battle-tested for financial operations
- Large ecosystem — integrations with DATEV, accounting systems already exist
- EU fintech companies heavily use Java — easier to hire

**Why Java 17 specifically?**
- LTS (Long Term Support) release — stable, supported until 2029
- Records, sealed classes — cleaner code
- Not Java 21 yet because at Series B, you don't chase the latest — you chase stability

**Why PostgreSQL per service?**
Each service has its **own database** — this is the database-per-service pattern in microservices. A Card Service database and an Expense Service database are completely separate. No shared tables. No cross-service joins at DB level.

Why? If they share a database:
- One team's bad migration can break another team's service
- You can't scale databases independently
- Schema changes require cross-team coordination

We'll go much deeper into this in Step 4 (Database Design).

**Why Flyway for migrations?**
- Every database schema change is a versioned SQL script (`V1__create_expense_table.sql`, `V2__add_currency_column.sql`)
- Checked into Git — reviewed in PRs like code
- Flyway runs on startup and applies any pending migrations automatically
- You never manually alter a production table

This is critical in financial systems — you need a full audit trail of every schema change.

---

### Layer 5 — Communication Between Services

Two types of communication happen:

```
SYNCHRONOUS (direct call, caller waits)
─────────────────────────────────────
When: You need an immediate answer
Example: Expense Service calling User Service 
         to check "is this user allowed to 
         submit expenses?"
How: REST via FeignClient (from your notes,
     Lecture 3 of Microservices)

ASYNCHRONOUS (event-driven, fire and move on)
─────────────────────────────────────────────
When: You don't need immediate response,
      or multiple services care about 
      the same event
Example: Invoice approved → 
         Payment Service needs to know +
         Notification Service needs to know +
         Accounting Service needs to export
How: Kafka
```

**Why not just use REST for everything?**

Imagine an invoice gets approved. Synchronously, you'd have to call Payment Service, wait, then call Notification Service, wait, then call Accounting Service, wait. If Notification Service is down, your whole approval fails. That's wrong — notification failure shouldn't block a payment.

With Kafka: publish one event `invoice.approved`, and all three services consume it independently. If Notification Service is down, it catches up when it restarts. Invoice approval succeeds regardless.

---

### Layer 6 — Supporting Infrastructure

| Component | Technology | Purpose |
|-----------|-----------|---------|
| Message Broker | Apache Kafka | Async event streaming between services |
| Cache | Redis | Distributed caching (approval states, rate limits, session data) |
| Config Management | Spring Cloud Config Server + Git | Centralized config, no hardcoded values |
| Service Discovery | Netflix Eureka | Services find each other by name, not hardcoded URLs |
| Secret Management | AWS Secrets Manager | DB passwords, API keys — never in code |
| Object Storage | AWS S3 | Receipt images, invoice PDFs |
| Email | SendGrid | Transactional emails (approval requests, reimbursement confirmations) |

---

### Layer 7 — Infrastructure & Cloud

```
Cloud Provider: AWS (most common for EU Series B fintechs)

Per Environment:
├── Development  → lightweight, shared resources
├── Staging      → mirrors production, used for QA
└── Production   → full setup, isolated, monitored

Containers: Docker
Orchestration: AWS ECS (Elastic Container Service)
              ← NOT Kubernetes yet. Here's why:
              Kubernetes is powerful but operationally 
              complex. At Series B with a small DevOps 
              team (2-3 people), ECS is simpler to manage
              and still gives you container orchestration.
              Kubernetes comes later when you have 
              dedicated platform engineers.

CI/CD: GitHub Actions
Monitoring: Datadog (metrics + APM + logs)
Tracing: OpenTelemetry → Jaeger
```

---

### Layer 8 — Security Considerations (Important for Fintech)

```
┌─────────────────────────────────────────────┐
│ SECURITY LAYERS                             │
│                                             │
│ 1. Transport: HTTPS everywhere, TLS 1.2+    │
│                                             │
│ 2. Auth: JWT tokens                         │
│    - Short-lived access token (15 min)      │
│    - Long-lived refresh token (7 days,      │
│      HttpOnly cookie)                       │
│    - Validated at Gateway only              │
│                                             │
│ 3. Authorization: RBAC                      │
│    Roles: ADMIN, FINANCE_MANAGER,           │
│           EMPLOYEE, ACCOUNTANT              │
│    @PreAuthorize at method level            │
│    (from your Spring Security notes)        │
│                                             │
│ 4. Data: Sensitive fields encrypted         │
│    at rest (bank account numbers, etc.)     │
│                                             │
│ 5. Audit: Every state change logged         │
│    with who did it and when                 │
│                                             │
│ 6. GDPR: PII masked in logs                 │
│    (MaskingJsonGeneratorDecorator           │
│    from your logging notes)                 │
└─────────────────────────────────────────────┘
```

---

## Summary — Tech Stack Quick Reference

```
┌─────────────────────────────────────────────────┐
│ CATEGORY          │ TECHNOLOGY                  │
├─────────────────────────────────────────────────┤
│ Language          │ Java 17                     │
│ Framework         │ Spring Boot 3.x             │
│ API Gateway       │ Spring Cloud Gateway        │
│ Service Discovery │ Netflix Eureka              │
│ Inter-service     │ FeignClient (sync)          │
│ Communication     │ Kafka (async)               │
│ ORM               │ Spring Data JPA + Hibernate │
│ Database          │ PostgreSQL (per service)    │
│ DB Migrations     │ Flyway                      │
│ Caching           │ Redis (distributed)         │
│                   │ Caffeine (local, L1)        │
│ Config            │ Spring Cloud Config         │
│ Secrets           │ AWS Secrets Manager         │
│ File Storage      │ AWS S3                      │
│ Containers        │ Docker                      │
│ Orchestration     │ AWS ECS                     │
│ CI/CD             │ GitHub Actions              │
│ Monitoring        │ Datadog                     │
│ Tracing           │ OpenTelemetry + Jaeger      │
│ Logging           │ SLF4J + Logback + JSON      │
│ Email             │ SendGrid                    │
│ Build Tool        │ Maven                       │
└─────────────────────────────────────────────────┘
```

---

This is Step 1 complete. Everything here is justified by business need, team size, and Series B/C pragmatism — no buzzwords without reason.

If you're good, we move to Step 2.