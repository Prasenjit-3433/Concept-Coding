# FinVerse - Technical Architecture & Stack (Revised for Series A Reality)

## Tech Stack Overview

### **Backend**

- **Node.js + NestJS**: Primary framework for most services (user management, accounts, notifications, analytics)
- **Go (Golang)**: Critical financial services requiring high performance and true concurrency (transaction processing, investment calculations, portfolio rebalancing)
- **PostgreSQL**: Primary relational database (user data, transactions, accounts, audit logs)
- **MongoDB**: Document store for unstructured data (user activities, analytics events, educational content)
- **Redis**: Caching layer + session management + real-time data
- **RabbitMQ**: Message broker for async communication between services
- **BullMQ**: Job queue for scheduled/delayed tasks (built on Redis)

### **Mobile (Primary User Interface)**

- **React Native**: Cross-platform (iOS + Android) to maintain single codebase
- **TypeScript**: Type safety across mobile app
- **Redux Toolkit**: State management
- **React Query**: Server state management and caching

### **Internal Tools (Support/Operations Teams)**

- **React + TypeScript**: Web-based dashboards
- **TanStack Table**: Data grids for transaction/user management
- **Recharts**: Analytics visualizations
- **Tailwind CSS**: Quick UI development

### **Infrastructure & DevOps**

- **AWS**: Primary cloud provider (widely adopted, mature ecosystem)
- **Docker**: Containerization
- **AWS ECS (Elastic Container Service)**: Container orchestration (NOT Kubernetes)
- **AWS Elastic Beanstalk**: For simple monolith deployments
- **GitHub Actions**: CI/CD pipelines
- **Terraform**: Infrastructure as Code
- **AWS CloudWatch + Sentry**: Monitoring, logging, error tracking

### **Third-Party Integrations**

- **Plaid / TrueLayer**: Bank account aggregation (PSD2 compliance)
- **Stripe**: Payment processing for subscriptions
- **Twilio SendGrid**: Transactional emails
- **AWS SNS**: Push notifications
- **Sentry**: Error tracking

---

## Realistic Team Size for Series A (Late Stage)

**Total Engineering: 18-22 people**

Breakdown:

- **Backend Engineers**: 8-10 (Node.js + Go)
- **Mobile Engineers**: 4-5 (React Native - iOS/Android)
- **Frontend Engineers**: 2-3 (React - internal tools)
- **DevOps/Infrastructure**: 1-2 (often shared with backend)
- **QA/Testing**: 2-3
- **Engineering Manager + Tech Lead**: 1-2

**Why this is realistic:**

- Series A funding (~€18M) typically supports 15-25 engineers
- ~85 total company size means ~25% engineering (rest is ops, support, marketing, legal/compliance)
- Small enough to move fast, large enough to handle critical systems

---

## Architecture Decision: Modular Monolith → Selective Microservices

### **Why NOT Full Microservices?**

At Series A with 18-22 engineers, full microservices would be **over-engineering**:

- Team is too small to manage 15+ independent services
- Each microservice needs ownership, monitoring, deployment pipeline
- Distributed transactions and data consistency become nightmares
- Network latency overhead between services
- Debugging becomes exponentially harder

### **Why NOT Pure Monolith?**

A single monolith would create bottlenecks:

- Different scaling needs (investment engine vs user API)
- Risk of entire platform going down from one bug
- Harder to use different languages (Go for finance, Node for APIs)
- Deployment of small changes requires redeploying everything
- No isolation for critical financial processing

---

## **Chosen Architecture: Modular Monolith + Strategic Microservices**

### **Core Modular Monolith (Node.js + NestJS)**

One main application with clear internal module boundaries:

**Modules inside the monolith:**

1. **User Module**: Authentication, profiles, KYC verification
2. **Account Module**: Bank account aggregation, balance syncing
3. **Budget Module**: Expense categorization, budget creation, alerts
4. **Goal Module**: Savings goals, progress tracking
5. **Education Module**: Courses, lessons, user progress
6. **Subscription Module**: Freemium/premium management, billing

**Why monolith for these?**

- These features share a lot of data (user context, permissions)
- Tight coupling is natural (e.g., budget needs account data)
- Easier to maintain consistency (single database transaction)
- Faster development (no inter-service communication overhead)
- Small team can manage one codebase efficiently

**Database**: Single PostgreSQL instance with read replicas

---

### **Independent Microservices (Strategically Separated)**

### **1. Investment Engine Service (Go)**

**What**: Portfolio creation, ETF selection, rebalancing calculations, dividend processing

**Why Go?**

- True concurrency for handling thousands of portfolio calculations simultaneously
- High performance for financial math (compound interest, tax calculations)
- Goroutines are perfect for parallel processing of user portfolios
- Compiled language = faster execution than interpreted Node.js

**Why separate service?**

- Different scaling needs (heavy computation during market hours)
- Isolated failures won't crash user-facing features
- Can be deployed independently during market off-hours
- Requires different performance optimization than API services
- CPU-intensive workload vs I/O-intensive API

**Tech**: Go, PostgreSQL (read replica for calculations), Redis (cache market data)

**Team ownership**: 2-3 backend engineers

---

### **2. Transaction Processing Service (Go)**

**What**: Buy/sell orders, fund transfers, settlement processing, ledger management

**Why Go?**

- Financial transactions need atomic operations with high throughput
- Concurrent order processing during peak times
- Critical path where performance = money (delayed trades = unhappy users)

**Why separate service?**

- Zero tolerance for downtime (money movement is sacred)
- Heavy audit/compliance requirements (isolated codebase easier to audit)
- Needs different security hardening than other services
- Regulatory requirement: clear separation of financial logic
- Can be locked down with stricter access controls

**Tech**: Go, PostgreSQL (with strict ACID guarantees), Redis (idempotency keys)

**Team ownership**: 2-3 backend engineers (*most senior*)

---

### **3. Notification Service (Node.js + NestJS)**

**What**: Push notifications, emails, SMS, in-app messages

**Why separate service?**

- Different scaling pattern (notification spikes don't match API usage)
- Non-critical path (if notifications are delayed, core platform still works)
- Multiple delivery channels (push, email, SMS) with different retry logic
- Heavy third-party integrations (Twilio, SendGrid, SNS)
- Failures shouldn't impact core platform

**Why Node.js?**

- I/O-heavy workload (sending messages, not computation)
- Good support for async operations
- Easy integration with third-party APIs

**Tech**: NestJS, MongoDB (notification templates, delivery logs), RabbitMQ, BullMQ

**Team ownership**: 1-2 backend engineers

---

## **Deployment Strategy (Series A Appropriate)**

### **Why NOT Kubernetes at This Stage?**

Kubernetes (EKS) is **overkill** for Series A:

- **Complexity**: Requires dedicated DevOps expertise (1-2 full-time engineers just for K8s)
- **Learning curve**: Steep for small teams, slows down development
- **Cost**: EKS control plane costs ~$73/month + worker nodes (expensive for startup)
- **Overhead**: Managing pods, services, ingress, ConfigMaps, secrets adds operational burden
- **Overkill features**: Auto-scaling, self-healing, rolling updates can be achieved with simpler tools

**When to adopt K8s?** Series B+ (5M+ users, 50+ engineers, complex multi-region deployments)

---

### **Chosen Deployment Strategy: AWS ECS Fargate + Elastic Beanstalk**

### **AWS ECS Fargate (For Microservices)**

**What it is**:

- Serverless container orchestration (no EC2 instances to manage)
- Pay only for containers you run
- AWS manages underlying infrastructure

**Used for:**

- **`Investment Engine Service`** (Go)
- **`Transaction Processing Service`** (Go)
- `Notification Service` (NestJS)

**Why ECS Fargate?**

- **Simpler than K8s**: Define task (container) → run on Fargate → done
- **No server management**: No EC2 patching, scaling, monitoring
- **Cost-effective**: Pay per second of container runtime (no idle servers)
- **Easy scaling**: Set min/max tasks, ECS auto-scales based on CPU/memory
- **Native AWS integration**: Works seamlessly with ALB, CloudWatch, Secrets Manager
- **Docker-based**: Simple Dockerfile → push to ECR → deploy

**Setup per service:**

```tsx
1. Build Docker image
2. Push to AWS ECR (Elastic Container Registry)
3. Define ECS Task Definition (CPU, memory, environment vars)
4. Create ECS Service (how many tasks, auto-scaling rules)
5. Attach to Application Load Balancer
```

**Example ECS Configuration (Investment Engine):**

- **Min tasks**: 1 (always running)
- **Max tasks**: 4 (scales during market hours 9 AM - 5 PM)
- **Auto-scaling trigger**: CPU > 70% for 2 minutes
- **Health check**: HTTP `/health` endpoint every 30 seconds
- **Deployment**: Rolling update (launch new task, drain old task, zero downtime)

---

### **AWS Elastic Beanstalk (For Core Monolith)**

**What it is**:

- Platform-as-a-Service for deploying web apps
- Handles provisioning, load balancing, auto-scaling, monitoring automatically
- You upload code, AWS manages infrastructure

**Used for:**

- Core API Monolith (NestJS)

**Why Elastic Beanstalk?**

- **Zero DevOps overhead**: Upload zip file or Docker image → AWS does the rest
- **Built-in features**: Auto-scaling, load balancing, SSL, health monitoring
- **Easy rollbacks**: One-click rollback to previous version
- **Environment management**: Separate dev/staging/production environments
- **Cost-effective**: Uses EC2 under the hood, but manages everything automatically
- **Perfect for monoliths**: Designed for single application deployments

**Setup:**

```sql
1. Create Elastic Beanstalk application
2. Create environment (e.g., "production")
3. Configure:
   - Platform: Node.js 20
   - Instance type: t3.medium (2 vCPU, 4 GB RAM)
   - Auto-scaling: Min 2, Max 6 instances
   - Load balancer: Application Load Balancer
4. Deploy via GitHub Actions (zip code → upload → EB deploys)
```

**Auto-scaling rules (Core API):**

- **Min instances**: 2 (high availability)
- **Max instances**: 6 (handles 450K users)
- **Scale up trigger**: Average CPU > 70% for 5 minutes
- **Scale down trigger**: Average CPU < 30% for 10 minutes

---

### **Infrastructure Components**

### **1. PostgreSQL (AWS RDS)**

- **Instance**: db.t3.large (2 vCPU, 8 GB RAM)
- **Multi-AZ deployment**: Automatic failover to standby in different availability zone
- **Read replica**: 1 replica for read-heavy queries (analytics, reports)
- **Automated backups**: Daily snapshots, 7-day retention
- **Storage**: 500 GB SSD (auto-scales up to 1 TB)

**Why RDS over self-managed PostgreSQL?**

- Automated backups, patching, failover
- 1-2 DevOps engineers can't manage database reliability at scale
- RDS handles 99.95% uptime SLA

---

### **2. MongoDB (AWS DocumentDB or MongoDB Atlas)**

**Option A: MongoDB Atlas (Recommended for Series A)**

- **Tier**: M10 (2 GB RAM, shared vCPU)
- **Managed service**: MongoDB Inc. handles everything
- **Why**: Easier than managing DocumentDB, better MongoDB compatibility
- **Cost**: ~$60/month

**Used for:**

- Analytics events (high write volume, flexible schema)
- Educational content (courses, lessons)
- Notification templates

---

### **3. Redis (AWS ElastiCache)**

- **Instance**: cache.t3.medium (2 vCPU, 3.09 GB RAM)
- **Cluster mode disabled** (simpler for Series A scale)
- **Replication**: Primary + 1 replica (automatic failover)
- **Used for**:
    - Session management
    - Caching (ETF prices, user portfolios)
    - BullMQ job queue storage
    - Rate limiting

---

### **4. RabbitMQ (AWS Amazon MQ)**

**What**: Managed RabbitMQ service by AWS

**Configuration:**

- **Broker type**: Single-instance broker (sufficient for 450K users)
- **Instance**: mq.t3.micro (1 vCPU, 1 GB RAM)
- **Deployment**: Single-AZ (for cost savings at Series A)

**Why Amazon MQ over self-managed?**

- No need to manage RabbitMQ cluster, patching, monitoring
- Small team can't afford RabbitMQ expertise
- Automatic backups and failover

**Upgrade path (Series B):**

- Move to multi-AZ active/standby brokers
- Or migrate to AWS SQS + SNS (fully serverless, cheaper at scale)

---

### **CI/CD Pipeline (GitHub Actions)**

**Deployment flow:**

```tsx
													┌───────────────┐
													│ Developer      │
													│ pushes code    │
													└───────┬───────┘
													         │
														       ▼
								┌─────────────────────────────────────┐
								│  GitHub Actions Workflow Triggers      │
								└─────────────────┬───────────────────┘
								                    │
								                    ▼
								┌─────────────────────────────────────┐
								│  1. Run Tests (Jest, Go tests)         │
								│  2. Lint code (ESLint, golangci)       │
								│  3. Build Docker image                 │
								└──────────────────┬──────────────────┘
								                     │
								                     ▼
								┌─────────────────────────────────────┐
								│  Push Docker image to AWS ECR           │
								└──────────────────┬──────────────────┘
								                     │
								       ┌──────────────┬──────────────┐
								       ▼               ▼                ▼
								┌──────────────┐ ┌─────────┐ ┌─────────────┐
								│ Deploy to EB  │ │Deploy to │ │  Deploy to   │
								│ (Monolith)    │ │ECS (Go)  │ │ ECS (Notif)  │
								└──────────────┘ └─────────┘ └─────────────┘
								       │               │               │
								       └──────────────┴──────────────┘
								                       │
								                       ▼
								       ┌──────────────────────────────┐
								       │  Run smoke tests (Playwright)   │
								       │  Notify team (Slack)            │
								       └──────────────────────────────┘
```

**Deployment frequency:**

- **Core monolith**: 2-3 times/week (stable, well-tested)
- **Microservices**: As needed (isolated changes, safer to deploy frequently)

**Rollback strategy:**

- Elastic Beanstalk: One-click rollback to previous version
- ECS: Update service to previous task definition revision
- Database: Migrations are backward-compatible (no breaking schema changes)

---

### **Monitoring & Alerting (Low Overhead)**

**AWS CloudWatch:**

- Application logs from all services
- Metrics (CPU, memory, request count, latency)
- Alarms (e.g., if API latency > 1s for 5 minutes, alert on-call engineer)

**Sentry:**

- Error tracking across all services
- Real-time alerts for crashes, exceptions
- Stack traces with context (user ID, request data)

**On-call rotation:**

- 1-2 senior engineers rotate weekly
- PagerDuty integration for critical alerts (database down, payment processing failed)

---

## ✨**Technology Choices - The "Why" Behind Each**

### **RabbitMQ - Event-Driven Communication**

**Use Cases:**

1. **Order placement flow**:
    - User places investment order → Core API publishes `order.created` event → Investment Engine picks up → processes → Transaction Service executes → publishes `order.completed` event → Notification Service sends confirmation
2. **Bank sync events**:
    - Account aggregation syncs transactions → publishes `transactions.synced` event → Budget module updates categories → Analytics logs activity
3. **Decoupling services**:
    - If Notification Service is down, events queue up and are processed when it's back (no data loss)

**Why RabbitMQ over Kafka?**

- Kafka is overkill for Series A scale (designed for millions of events/sec)
- RabbitMQ is simpler to operate (Amazon MQ = fully managed)
- Better for traditional queue patterns (reliable delivery, not streaming analytics)
- Lower cost (~$20/month for mq.t3.micro vs $200+/month for MSK)

**Why RabbitMQ over AWS SQS?**

- RabbitMQ has better routing (topic exchanges, fanout, direct)
- Better fit for complex event-driven patterns
- Can migrate to SQS later if needed (Series B optimization)

---

### **BullMQ - Job Queue for Scheduled/Delayed Tasks**

**Use Cases:**

1. **Recurring investments**:
    - User sets up monthly €200 auto-invest → BullMQ schedules job for 1st of every month → triggers investment order
2. **Portfolio rebalancing**:
    - Every Saturday at 2 AM → BullMQ triggers batch job → Investment Engine rebalances all portfolios needing adjustment
3. **Delayed notifications**:
    - User misses budget by 20% → schedule reminder for next week → BullMQ delays job 7 days → Notification Service sends nudge
4. **Tax report generation**:
    - End of year → BullMQ queues tax report generation for all users → processes in batches to avoid overload
5. **Retry failed tasks**:
    - Bank sync fails → BullMQ retries with exponential backoff (3 attempts over 6 hours)

**Why BullMQ over basic cron jobs?**

- Distributed (any service can add jobs, not tied to one server)
- Persistent (jobs stored in Redis, survive server restarts)
- Built-in retry logic, priority queues, delayed jobs
- Web UI dashboard to monitor job failures

**Why BullMQ over RabbitMQ for jobs?**

- BullMQ is specialized for job scheduling (cron-like, delays, retries)
- RabbitMQ is better for real-time event streaming between services
- Both complement each other (RabbitMQ = events, BullMQ = scheduled work)

**Implementation:**

- BullMQ runs inside Core API monolith and Notification Service
- Shares Redis instance (cost optimization)
- Separate queues for different job types (investments, notifications, reports)

---

### **Redis - Multiple Critical Roles**

**Use Cases:**

1. **Caching**:
    - ETF prices (update every 15 min, cached in Redis)
    - User session data (fast auth checks)
    - Frequently accessed user portfolios
2. **Rate limiting**:
    - Prevent API abuse (100 requests/min per user)
    - `Token bucket algorithm` using Redis ***counters***
3. **Real-time data**:
    - Live portfolio value updates during market hours
    - WebSocket connection state
4. **BullMQ storage**:
    - Stores job queue data

**Why Redis?**

- In-memory = blazing fast (sub-millisecond reads)
- Simple data structures (strings, hashes, sorted sets)
- Persistence options (RDB snapshots + AOF log)
- Industry standard, mature ecosystem
- ElastiCache = fully managed (no ops overhead)

**Cost optimization:**

- Single Redis instance handles caching + BullMQ (no need for separate instances)
- Upgrade to cluster mode when hitting 50 GB data or 100K ops/sec (not needed at Series A)

---

### **PostgreSQL - Primary Database**

**What's stored:**

- User accounts, profiles, KYC data
- Bank account connections
- Budgets, transactions, goals
- Investment orders, portfolio holdings
- Subscription data
- Audit logs (compliance requirement)

**Why PostgreSQL?**

- ACID guarantees (critical for financial data)
- Mature, battle-tested for transactional workloads
- Excellent support for complex queries (joins, aggregations)
- Strong consistency (no eventual consistency issues)
- JSONB support for flexible fields (user preferences)
- Open-source, no vendor lock-in

**Why RDS?**

- Automated backups, patching, monitoring
- Multi-AZ failover (99.95% uptime SLA)
- Read replicas for scaling read-heavy queries
- Small team can't manage database reliability manually

**Scaling strategy:**

- **Now**: db.t3.large (handles 450K users)
- **Series B**: db.r5.xlarge (4 vCPU, 32 GB RAM for 1.5M users)
- **Later**: Horizontal sharding by geography (EU West, EU Central)

---

### **MongoDB - Secondary Database**

**What's stored:**

- User activity logs (clicks, page views)
- Educational content (courses, lessons, rich text)
- Analytics events (flexible schema as analytics evolve)
- Notification templates

**Why MongoDB?**

- Flexible schema for evolving analytics needs
- Fast writes for high-volume activity logging
- Document model fits content management (courses with nested lessons)
- Good for read-heavy analytics queries

**Why MongoDB Atlas over DocumentDB?**

- Better MongoDB compatibility (DocumentDB has limitations)
- Easier to migrate to self-hosted later if needed
- Better developer experience (familiar tools, drivers)
- Cost: ~$60/month (M10 tier) vs ~$200/month for DocumentDB

**Why not use MongoDB for everything?**

- Lacks ACID guarantees across documents (risky for financial data)
- Joins are weak (not good for relational data)
- PostgreSQL is better for transactional, relational data

---

### **React Native - Mobile App**

**Why React Native?**

- Single codebase for iOS + Android (50% less development time)
- JavaScript/TypeScript (same language as backend → easier hiring)
- Large community, mature ecosystem
- Good performance for financial app (not gaming-level requirements)
- Faster iteration (hot reload, OTA updates via CodePush)
- Can ship features to both platforms simultaneously

**Why NOT native (Swift/Kotlin)?**

- At Series A, speed matters more than 5% performance gains
- Limited engineering resources (4-5 mobile engineers)
- Can always rewrite critical screens in native later if needed
- Most fintech apps (Revolut, N26, Klarna early days) started with React Native

**Deployment:**

- **App stores**: Standard Apple App Store + Google Play Store releases
- **OTA updates**: CodePush for minor updates (bug fixes, UI tweaks) without app store review
- **Release cycle**: Bi-weekly releases to app stores

---

### **React for Internal Tools**

**Why React (not admin templates)?**

- Reusable components across dashboards
- TypeScript = safer for complex data operations
- Internal tools need custom workflows (templates are too rigid)
- Same stack knowledge as mobile team (easy to contribute)

**Use Cases:**

- Customer support dashboard (view user accounts, troubleshoot issues)
- Operations dashboard (monitor failed transactions, approve KYC manually)
- Analytics dashboard (revenue metrics, user growth, funnel analysis)
- Content management (upload educational courses, manage lessons)

**Deployment:**

- **AWS Amplify**: Dead simple deployment (connect GitHub repo → auto-deploy on push)
- **Why Amplify?**: Zero DevOps overhead, built-in CI/CD, SSL, CDN
- **Cost**: ~$10-15/month for all internal tools

---

## **Data Flow Example: User Makes an Investment**

1. **User action**: Taps *"Invest €500 in Growth Portfolio"* in React Native app
2. **API Gateway (ALB)**: Routes request to Core API monolith
3. **Core API (NestJS)**:
    - Validates request (user auth, sufficient balance)
    - Creates pending order in PostgreSQL
    - Publishes `order.created` event to RabbitMQ
4. **Investment Engine (Go)**:
    - Consumes event from RabbitMQ
    - Calculates ETF allocations (60% stocks, 40% bonds)
    - Publishes `allocation.calculated` event
5. **Transaction Service (Go)**:
    - Consumes event
    - Executes buy orders with broker API
    - Updates ledger in PostgreSQL
    - Debits user account
    - Publishes `order.completed` event
6. **Notification Service (NestJS)**:
    - Consumes event
    - Sends push notification via AWS SNS: "Your €500 investment is complete!"
7. **BullMQ**:
    - Schedules job to generate updated portfolio report (delayed 1 hour to batch multiple trades)
8. **Analytics (inside Core API)**:
    - Logs investment event to MongoDB for reporting

**Total time**: ~2-3 seconds for user (async processing happens in background)

---

## **Cost Breakdown (Monthly AWS Bill)**

| Service | Configuration | Cost |
| --- | --- | --- |
| **Elastic Beanstalk (Core API)** | 2x t3.medium instances (avg) | $60 |
| **ECS Fargate (Investment Engine)** | 2x 0.5 vCPU, 1 GB RAM tasks | $30 |
| **ECS Fargate (Transaction Service)** | 2x 0.5 vCPU, 1 GB RAM tasks | $30 |
| **ECS Fargate (Notification Service)** | 1x 0.5 vCPU, 1 GB RAM task | $15 |
| **RDS PostgreSQL** | db.t3.large + 1 read replica | $200 |
| **MongoDB Atlas** | M10 tier | $60 |
| **ElastiCache Redis** | cache.t3.medium + replica | $70 |
| **Amazon MQ (RabbitMQ)** | mq.t3.micro | $20 |
| **Application Load Balancer** | 1 ALB | $25 |
| **S3 + CloudFront** | Static assets, backups | $30 |
| **CloudWatch + Logs** | Monitoring, logging | $40 |
| **Amplify (Internal Tools)** | 3-4 React apps | $15 |
| **Data Transfer** | Outbound traffic | $50 |
| **Misc (Secrets Manager, ECR, etc.)** | - | $30 |
| **Total** |  | **~$675/month** |

**At Series B scale (1.5M users)**: ~$1,800-2,200/month (3x increase, but still manageable)

---

## **Scaling Strategy (Series A → Series B)**

### **Current Setup (450K users, 180K MAU)**

- **Core API**: 2-4 instances (auto-scaling)
- **Investment Engine**: 1-2 instances (scaled during market hours)
- **Transaction Service**: 2 instances (high availability)
- **Notification Service**: 1 instance
- **PostgreSQL**: db.t3.large + 1 read replica
- **Redis**: cache.t3.medium + replica
- **RabbitMQ**: Single-instance broker

**Handles:**

- ~500 requests/second peak
- ~10K concurrent users
- ~2K investment orders/day

---

### **Series B Target (1.5M users, 600K MAU)**

- **Core API**: 4-8 instances
- **Investment Engine**: 3-5 instances
- **Transaction Service**: 3-4 instances
- **Notification Service**: 2 instances
- **PostgreSQL**: db.r5.xlarge + 2 read replicas
- **Redis**: cache.r5.large (cluster mode if needed)
- **RabbitMQ**: Multi-AZ broker (or migrate to SQS/SNS)

**Handles:**

- ~1,500 requests/second peak
- ~30K concurrent users
- ~7K investment orders/day

**Infrastructure changes needed:**

- Add CloudFront CDN for API responses (cache frequent reads)
- Implement database connection pooling (PgBouncer)
- Add APM tool (DataDog or New Relic) for deeper insights
- Consider moving to ECS clusters (vs Fargate) for cost optimization

---

## **Why This Architecture Works for Series A**

1. **Right-sized complexity**: Not over-engineered (Kubernetes, 20 microservices) or under-engineered (single monolith)
2. **Language strengths**: Go where performance matters (finance calculations), Node.js for I/O and APIs
3. **Managed services**: RDS, ElastiCache, Amazon MQ, Amplify reduce DevOps overhead (critical for 1-2 DevOps engineers)
4. **Cost-effective**: ~$675/month AWS bill (vs $2K+ with Kubernetes + self-managed databases)
5. **Developer productivity**:
    - Monolith for fast feature development (most features)
    - Microservices for isolation (critical financial systems)
    - Small team can ship features quickly
6. **Battle-tested stack**: PostgreSQL, Redis, RabbitMQ, React Native are industry standards (easy hiring, lots of resources)
7. **Clear scaling path**: Can grow 3-5x (to Series B scale) without major re-architecture
8. **Operational simplicity**:
    - Elastic Beanstalk handles monolith automatically
    - ECS Fargate handles microservices without managing servers
    - Managed databases/queues reduce operational burden

---

## **When to Revisit Architecture (Series B+ Signals)**

You'll know it's time to re-architect when:

1. **ECS Fargate costs > $1K/month** → Move to ECS on EC2 clusters (reserved instances = 50% savings)
2. **Database connections exhausted** → Implement read/write splitting, connection pooling at scale
3. **Monolith deployment takes > 10 minutes** → Break monolith into domain-based services
4. **PostgreSQL single instance hitting limits** → Implement sharding by geography or user ID
5. **RabbitMQ becomes bottleneck** → Migrate to AWS SQS/SNS (fully serverless, infinite scale)
6. **Team grows to 50+ engineers** → Time to adopt Kubernetes for standardized deployments

---

This architecture can **comfortably handle 1-2 million users** before needing major changes. The focus is on **reliability, performance, cost-efficiency, and team velocity** — not chasing trends or over-engineering.