Let me think through everything cleanly before writing a single word.

One question first — because this affects the architecture document directly:User's selection: You decide what fits best for this stage

Good. Here is my decision and reasoning — then the full document.

**Internal communication → REST over HTTP.** At Series A with a small team, gRPC adds real overhead — you need protobuf schema management, code generation pipelines, and engineers who understand it deeply. The performance gain over REST on internal AWS VPC networking is negligible at FinVerse's current scale. REST is the right call. gRPC becomes worth revisiting if internal call volume grows significantly at Series B.

---

# FinVerse — Tech Stack & System Architecture

## 1. Guiding Principles

Every decision in this architecture flows from five realities about what FinVerse actually is:

**Stage-appropriate complexity.** FinVerse is Series A — 85 people, 45 in engineering. The architecture must be sophisticated enough to scale across 8 EU markets and handle regulated financial data, but not so fragmented that the team spends more time on infrastructure than product. Every service boundary is justified by a real business need, not engineering taste.

**Financial correctness over raw speed.** This is a regulated fintech — MiFID II, PSD2, GDPR, country-specific tax rules across 8 EU jurisdictions. Data consistency, auditability, and transactional integrity are non-negotiable. Every architecture decision reflects this constraint.

**Mobile-first, but not mobile-only.** React Native is the primary client. APIs are designed around mobile constraints — payload efficiency, bandwidth, battery. The React web dashboard is secondary, used for detailed analytics, account management, and internal tooling.

**Selective decomposition.** A module becomes a separately deployed service only when it has a genuinely different scaling profile, a compliance isolation requirement, a clear team ownership boundary, or a fundamentally different runtime need. Not because microservices sound impressive.

**Operational simplicity at this stage.** A small engineering team cannot afford to spend cycles maintaining complex infrastructure. Every infrastructure choice must be the simplest thing that meets the requirement — with a clear upgrade path as the company grows.

---

## 2. Service Map

```
┌──────────────────────────────────────────────────────────────┐
│                        CLIENT LAYER                          │
│          React Native (primary)  │  React Web (secondary)    │
└─────────────────────┬────────────────────────────────────────┘
                      │ HTTPS / REST
          ┌───────────▼──────────────┐
          │       API GATEWAY        │
          │    (NestJS — standalone) │
          │  Auth · Rate Limiting    │
          │  Routing · Versioning    │
          └───┬──────────┬───────────┘
              │          │  REST over HTTP (internal VPC)
    ┌─────────▼───┐  ┌───▼──────────────────────────────┐
    │   Payment   │  │       Core Product Service        │
    │   Service   │  │  (NestJS — Modular Monolith)      │
    │  (NestJS)   │  │                                   │
    └─────────────┘  └──────────────────────────────────┘
          │                        │
          │         ┌──────────────┴──────────────┐
          │         │                             │
    ┌─────▼─────────▼──┐              ┌───────────▼──────────┐
    │    RabbitMQ       │              │   Market Data        │
    │  (Async Event Bus)│              │   Service  (Go)      │
    └──────────┬────────┘              └───────────┬──────────┘
               │                                   │
    ┌──────────▼────────┐                ┌─────────▼─────────┐
    │   Notification    │                │      Redis         │
    │   Service(NestJS) │                │  (ETF price cache) │
    └───────────────────┘                └───────────────────┘
               │
   ┌───────────┴───────────┐
   │  SendGrid  │  Twilio  │
   └───────────────────────┘

┌──────────────────────────────────────────────────────┐
│                 INFRASTRUCTURE LAYER                  │
│  PostgreSQL · Redis · RabbitMQ · BullMQ Workers       │
│  AWS ECS (Fargate) · AWS ECR · AWS RDS · AWS ALB      │
│  ElastiCache · Amazon MQ · GitHub Actions             │
│  OpenTelemetry → Datadog                              │
└──────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────┐
│               THIRD-PARTY INTEGRATIONS               │
│  GoCardless Bank Account Data  (Open Banking / PSD2) │
│  EODHD                         (ETF & market data)   │
│  Stripe                        (payments)            │
│  SendGrid                      (email)               │
│  Twilio                        (SMS / OTP)           │
│  EU State Pension APIs         (retirement planning) │
└──────────────────────────────────────────────────────┘
```

---

## 3. Every Service — What, Why, and When It Scales

### Service 1 — API Gateway (NestJS)

**What it does:**
The single entry point for all client traffic — React Native app and React web dashboard both hit this first. Responsibilities:

- JWT validation on every inbound request
- Per-user and per-endpoint rate limiting (sliding window counters stored in Redis)
- Request routing to downstream services (Core Product or Payment Service)
- API versioning — `/v1/`, `/v2/` — so the mobile app can be versioned independently of backend deployments
- Response envelope normalisation — consistent `{ data, meta, error }` shape for all mobile API responses
- Request logging — every inbound request gets a `traceId` injected here, which propagates through all downstream services for distributed tracing

**Why a dedicated gateway and not just middleware inside Core Product?**
Centralising auth and rate limiting here means every downstream service can trust that any request it receives is already authenticated and within quota. It also means routing rules, API versioning, and rate limit policies can change without touching Core Product or Payment Service. As FinVerse expands to new countries, regional routing logic lives here — not scattered across services.

**Scales when:** Raw inbound traffic grows — more users, more countries, higher API call frequency per session.

---

### Service 2 — Core Product Service (NestJS — Modular Monolith)

**What it does:**
The heart of FinVerse. A single deployable NestJS application, internally organised into strict domain-separated modules. Each module has its own controllers, services, repositories, and DTOs. Modules never access each other's database tables directly — if Module A needs data from Module B, it calls Module B's exported service interface. This boundary is enforced by code convention and enforced in code review.

**Internal modules:**

| Module | Responsibility |
|---|---|
| Users & Auth | Registration, login, JWT issuance, profile, KYC status |
| Accounts & Open Banking | Bank connections via GoCardless, account aggregation, balance sync |
| Transactions & Categorisation | Transaction ingestion, rule-based auto-categorisation, manual overrides |
| Budgeting | Monthly budgets, spend alerts, category limits |
| Goals & Savings | Savings goals, automated transfers to savings pockets, progress tracking |
| Investing | ETF portfolio management, risk profiles, automated monthly investments |
| Retirement Planning | Pension gap calculator, state pension integration, private pension recommendations |
| Tax & Reporting | Year-end tax reports, country-specific deduction logic, loss harvesting |
| Education Hub | Course content, lesson progress tracking, learning paths |

**Why a modular monolith and not separate services per domain?**
These modules are deeply interconnected by business logic. A budget alert needs transaction data. An investment recommendation needs income data from connected accounts. A tax report needs both investment history and transaction history across the year. If each were a separately deployed service, you would have constant cross-service HTTP calls, distributed transaction complexity, and enormous operational overhead for a 45-person team. The modular monolith gives clean domain boundaries in code without any of that operational cost. Individual modules can be extracted into separate services later — but only when there is evidence that a specific module needs independent scaling or a team ownership split. That decision is driven by data, not upfront assumption.

**Scales when:** The whole service scales horizontally behind an AWS ALB. If a specific module later proves to need independent scaling — for example Tax & Reporting during EU tax season — it gets extracted at that point.

---

### Service 3 — Payment Service (NestJS)

**What it does:**
Handles all money movement through Stripe:

- Premium and Family subscription billing — initial charge and recurring monthly
- Investment deposits — moving user funds into their ETF portfolio
- Goal-based automated transfers — scheduled contributions to savings pockets
- Refunds and cancellations — subscription downgrades, investment withdrawals

Maintains a strict internal financial ledger — every payment event is recorded with a full audit trail including Stripe event IDs, amounts, currency, status, timestamps, and user IDs. No payment event is ever deleted or mutated — only new records appended.

**Why separate from Core Product?**

**Fault isolation.** A bug or deployment issue in payment processing must never affect expense tracking or investing. These have completely different risk profiles — a Payment Service restart should be invisible to a user browsing their budget screen.

**Compliance isolation.** Payment handling carries PCI-DSS requirements on top of MiFID II. Keeping it separate means compliance audits are scoped cleanly — auditors do not need to review the entire Core Product codebase to verify payment handling.

**Deployment independence.** Stripe API version upgrades, new EU payment regulations, updated 3DS2 authentication flows — these changes deploy independently with no risk to the rest of the product.

**Scales when:** Payment volume grows with new countries or new payment product types.

---

### Service 4 — Notification Service (NestJS)

**What it does:**
A pure event consumer. It subscribes to RabbitMQ and listens for domain events published by other services:

| Event | Outbound Action |
|---|---|
| `budget.threshold.exceeded` | Push notification + email to user |
| `investment.completed` | Push notification confirming investment |
| `payment.succeeded` | Email receipt |
| `payment.failed` | Push notification + SMS alert |
| `goal.milestone.reached` | Push notification celebrating progress |
| `transaction.large.detected` | Push notification flagging unusual spend |

Manages user notification preferences (which channels, which event types), delivery status tracking, retry logic for failed sends, and a notification history log.

**Why separate?**

**Radically different scaling profile.** During end-of-month budget cycles, market volatility events, or a marketing campaign, notification volume can spike 30–50x independently of everything else. Scaling this without touching Core Product or Payment Service is only possible if it is its own service.

**Zero business logic coupling.** This service only consumes events and sends outbound messages. It shares no business logic with any other service. The boundary is as clean as it gets.

**Third-party dependency isolation.** SendGrid outages, Twilio API changes, push notification provider updates — these affect only this service. Everything else is unaware.

**Scales when:** Notification event volume spikes — triggered by user growth, market events, or campaigns — independently of user-facing API traffic.

---

### Service 5 — Market Data Service (Go)

**What it does:**
Two distinct responsibilities that both justify the same service:

**Market data ingestion and caching.** Polls EODHD during market hours for current ETF prices across all instruments in the FinVerse catalogue. Normalises the raw API response into a consistent internal format and writes it to Redis with a TTL aligned to the polling interval. During off-market hours, serves the last known prices from cache.

**Portfolio valuation.** For each user with an active portfolio, computes current portfolio value, absolute return, percentage return, and per-holding attribution. These computations run on a scheduled basis and are also triggered on-demand when a user opens their portfolio screen.

**Why Go and not NestJS?**

This decision comes down to the nature of the workload:

Node.js is single-threaded. It handles thousands of concurrent I/O operations brilliantly via the event loop — that is precisely what it is designed for. But portfolio valuation is CPU-bound numeric computation — tight arithmetic loops over price series, computing returns across potentially hundreds of thousands of user portfolios, each with multiple ETF holdings. Running that inside Node.js means it competes directly with the event loop. While requests are being processed, the CPU is occupied with valuation math — degrading response times for all other operations in that process. Worker threads partially address this but add significant complexity and still operate within V8's memory constraints.

Go handles CPU-bound concurrent work naturally. Goroutines are lightweight — you can run thousands of them concurrently with minimal memory overhead. The market data polling pattern — fan out concurrent HTTP calls to EODHD for hundreds of ETF tickers, normalise responses, write to Redis — is something Go does elegantly and efficiently. Portfolio valuation loops run in parallel goroutines without any of the event loop contention that Node.js would face.

Additionally, this is a cleanly isolated service. It has a well-defined API surface, no shared database with other services, and no NestJS framework dependencies. It is the ideal candidate for a different runtime precisely because extracting it creates no coupling problems.

**Why EODHD for market data?**
EODHD covers 150,000+ tickers across 60+ exchanges, including all major European ETFs on Euronext, Xetra, and Borsa Italiana. It provides over 30 years of historical end-of-day data, which is sufficient for the portfolio performance charts and retirement projections FinVerse shows users. Pricing starts at €19.99 per month for end-of-day global data, which is commercially viable at Series A. Refinitiv/LSEG and Bloomberg are enterprise-grade with enterprise pricing — appropriate for large asset managers, not a startup at €220M AUM. EODHD gives the right data quality and coverage at the right cost, with a clear path to upgrading when AUM justifies it.

**Why GoCardless Bank Account Data (formerly Nordigen) for Open Banking?**
GoCardless Bank Account Data is a licensed Account Information Service Provider regulated and authorised in 31 European countries, with its API connecting to more than 2,300 banks across the UK and Europe. Operating across 8 EU markets — Germany, France, Netherlands, Spain, Italy, Poland, Belgium, Austria — requires a provider with both deep bank coverage and regulatory standing in each jurisdiction. It provides a unified API format for information from various banks, ensuring reliable and PSD2-compliant data, which is exactly what the Accounts and Transactions modules in Core Product need. Tink (Visa-owned) offers deeper data enrichment but at significantly higher cost — appropriate at Series B scale, not Series A.

**Scales when:** AUM grows, more users have active portfolios, more ETFs are added to the catalogue, and peak market hours drive polling and valuation spikes.

---

## 4. Infrastructure Layer

### PostgreSQL — Primary Database
Every service with persistent structured data uses PostgreSQL. Financial data is deeply relational — users have accounts, accounts have transactions, transactions belong to categories, portfolios have holdings, holdings reference instruments, instruments have prices that feed into tax calculations. Foreign key constraints, referential integrity, joins, and full ACID transaction guarantees are all required. This is non-negotiable in a regulated fintech.

Each service owns its own isolated PostgreSQL schema. Services never share tables directly. If Service A needs data from Service B's domain, it calls Service B's API or receives it via a RabbitMQ event. Cross-schema queries between services are not permitted.

**Hosting:** AWS RDS for PostgreSQL — managed, automated backups, multi-AZ for production, point-in-time recovery.

### Redis
Redis serves multiple distinct purposes across the system:

| Service | Redis Usage |
|---|---|
| API Gateway | Sliding window rate limiting counters per user and endpoint |
| Market Data (Go) | ETF price cache and portfolio valuation cache — TTL-based |
| Core Product | OTP and verification codes, session tokens, frequently accessed user preferences |
| BullMQ | Job queue state, job metadata, retry tracking — BullMQ's entire backend |

**Hosting:** AWS ElastiCache for Redis — managed, cluster mode for production.

### RabbitMQ — Async Event Bus
The communication backbone between the distributed services. Services publish domain events when something significant happens. Consumers react asynchronously. Core Product does not know that Notification Service exists — it publishes `budget.threshold.exceeded` and moves on. This decoupling is what allows services to scale, deploy, and fail independently of each other.

**Hosting:** Amazon MQ for RabbitMQ — fully managed, no broker to maintain.

### BullMQ — Background Job Processing
BullMQ runs inside NestJS services — primarily Core Product and Payment Service. It handles work that must not block the request-response cycle:

- Polling GoCardless for new bank transactions (scheduled, every few hours per user)
- Running monthly automated ETF investment orders
- Generating year-end tax reports (computationally heavy, runs as background job)
- Syncing state pension estimates
- Sending end-of-month portfolio summaries

BullMQ uses Redis as its backend store for all job state.

### AWS ECS with Fargate — Container Orchestration
Each service runs as a Docker container deployed on AWS ECS with Fargate. Fargate is serverless container execution — AWS manages the underlying compute entirely. The team defines task definitions and service configurations; ECS handles scheduling, health checks, automatic restarts, and auto-scaling based on CPU and memory metrics.

**Why ECS Fargate and not Kubernetes?**
Kubernetes has serious operational overhead — cluster management, networking configuration, RBAC, ingress controllers, debugging node and pod issues. Running Kubernetes well requires dedicated platform engineering expertise. At Series A with 45 engineers and no dedicated DevOps team, that overhead would consume meaningful engineering capacity. ECS Fargate gives everything FinVerse needs right now — per-service independent scaling, rolling deployments with zero downtime, health checks and automatic restarts, integration with AWS ALB — without requiring anyone to babysit a cluster. Kubernetes is the right conversation at Series B, when the team and scale justify a platform investment.

---

## 5. Third-Party Integrations

| Integration | Provider | Purpose | Consumed By |
|---|---|---|---|
| Open Banking / PSD2 | GoCardless Bank Account Data | Bank account aggregation, transaction sync across 2,300+ EU banks | Core Product — Accounts & Transactions modules |
| ETF & Market Data | EODHD | Real-time and historical ETF prices, NAV, instrument metadata | Market Data Service (Go) |
| Payment Processing | Stripe | Subscription billing, investment deposits, goal transfers, refunds | Payment Service |
| Email | SendGrid | Transactional emails — receipts, alerts, reports, onboarding sequences | Notification Service |
| SMS & OTP | Twilio | OTP verification codes, critical account security alerts | Notification Service |
| State Pension Data | Per-country EU government APIs | State pension estimate data for retirement gap calculations | Core Product — Retirement module |

---

## 6. Full Tech Stack Reference

| Layer | Technology | Why |
|---|---|---|
| API Gateway | NestJS (TypeScript) | I/O routing, auth, rate limiting — no computation, team expertise |
| Core Backend | NestJS (TypeScript) | Modular DI, decorator-based, well-suited for domain-rich fintech logic |
| Market Data & Portfolio Valuation | Go | CPU-bound valuation loops, concurrent ETF polling, memory-efficient |
| ORM | Prisma | Type-safe queries, migration management, excellent NestJS integration |
| Primary Database | PostgreSQL | ACID, relational, regulated financial data |
| Cache & Queue Backend | Redis | Multi-purpose: caching, rate limiting, BullMQ job store |
| Async Messaging | RabbitMQ | Reliable event-driven communication, flexible routing between services |
| Background Jobs | BullMQ | Scheduled jobs, retries, delayed and recurring processing |
| External API Style | REST (JSON) | Mobile-first clients, simple, well-understood, easy to version |
| Internal API Style | REST over HTTP | Simple, team already fluent, negligible latency on internal VPC at this scale |
| Mobile Client | React Native | Cross-platform iOS + Android, single codebase |
| Web Dashboard | React + Tailwind CSS | Analytics, account management, internal tooling |
| Containerisation | Docker | Consistent environments, local dev parity with production |
| Orchestration | AWS ECS with Fargate | Managed containers, auto-scaling, zero cluster overhead at Series A |
| Container Registry | AWS ECR | Native ECS integration |
| Cloud | AWS | RDS, ElastiCache, Amazon MQ, ECS, ECR, ALB, Secrets Manager |
| CI/CD | GitHub Actions | Build, test, deploy pipelines |
| Observability | OpenTelemetry → Datadog | Distributed tracing, centralised logs, APM, dashboards, alerting |

---

That is the complete rewritten Step 2. Clean, justified, and defensible at every layer.

**Ready for Step 3: System Design & Data Flows?**