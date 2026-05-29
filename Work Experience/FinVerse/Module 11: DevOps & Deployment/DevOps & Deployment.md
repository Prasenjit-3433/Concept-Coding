Writing Step 11 now.

---

# FinVerse — Step 11: CI/CD & DevOps

## Part 1 — Why This Step Matters in an Interview

When an interviewer asks "explain your CI/CD process," they are not just asking about tools. They are asking:

- Do you understand why automation exists?
- Do you know what can go wrong during deployment in a production financial system?
- Can you talk about rollback, zero-downtime deployments, and environment promotion like an engineer who has lived through incidents?

This step builds that understanding from first principles — what the pipeline does, why each step exists, and how FinVerse's specific constraints (regulated fintech, small team, AWS infrastructure) shaped the decisions.

---

## Part 2 — The Big Picture: What CI/CD Actually Is

Before the pipeline steps, let's be precise about what the terms mean.

```
┌─────────────────────────────────────────────────────────────────┐
│                 CI / CD — PRECISE DEFINITIONS                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  CI — Continuous Integration                                    │
│  ─────────────────────────────                                  │
│  Every code change is automatically:                            │
│    1. Merged into a shared branch                               │
│    2. Built into a runnable artifact                            │
│    3. Tested (unit, integration)                                │
│    4. Validated (lint, type check, security scan)               │
│                                                                 │
│  Goal: detect problems as early as possible —                   │
│  before they reach production.                                  │
│                                                                 │
│  CD — Continuous Delivery / Continuous Deployment               │
│  ─────────────────────────────────────────────────              │
│  Continuous Delivery: the artifact produced by CI               │
│  can be deployed to production AT ANY TIME with                 │
│  a manual approval step.                                        │
│                                                                 │
│  Continuous Deployment: every artifact that passes CI           │
│  is AUTOMATICALLY deployed to production with no                │
│  manual step.                                                   │
│                                                                 │
│  FinVerse uses: Continuous Delivery                             │
│  (manual approval required for production deployment)           │
│  Why: regulated fintech — deliberate human sign-off             │
│  required before production changes, especially for             │
│  schema migrations and payment-adjacent changes.                │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Part 3 — FinVerse's Infrastructure Before the Pipeline

To understand the pipeline, you first need to know what it is deploying to.

```
AWS INFRASTRUCTURE — CORE PRODUCT TEAM'S SERVICES

┌─────────────────────────────────────────────────────────────────────┐
│                         AWS ACCOUNT                                 │
│                                                                     │
│  ┌────────────────────┐  ┌──────────────────────────────────────┐   │
│  │    AWS ECR         │  │          AWS ECS (Fargate)           │   │
│  │                    │  │                                      │   │
│  │  Container         │  │  Services:                           │   │
│  │  Registry          │  │  - core-product-api                  │   │
│  │                    │  │  - tx-sync-worker                    │   │
│  │  Stores Docker     │  │  - budget-check-worker               │   │
│  │  images tagged     │  │  - tax-report-worker                 │   │
│  │  by git SHA        │  │  - outbox-publisher-worker           │   │
│  │                    │  │  - notification-service              │   │
│  └────────────────────┘  │  - payment-service                   │   │
│                          └──────────────────────────────────────┘   │
│                                                                     │
│  ┌─────────────────┐  ┌────────────────┐  ┌─────────────────────┐   │
│  │   AWS RDS       │  │ ElastiCache    │  │    Amazon MQ        │   │
│  │  PostgreSQL     │  │ (Redis)        │  │   (RabbitMQ)        │   │
│  └─────────────────┘  └────────────────┘  └─────────────────────┘   │
│                                                                     │
│  ┌─────────────────┐  ┌────────────────┐  ┌─────────────────────┐   │
│  │   AWS ALB       │  │  Secrets       │  │    AWS S3           │   │
│  │  (Load          │  │  Manager       │  │  (Tax report PDFs,  │   │
│  │  Balancer)      │  │  (credentials) │  │   static assets)    │   │
│  └─────────────────┘  └────────────────┘  └─────────────────────┘   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

**Three environments — staging mirrors production exactly:**

```
ENVIRONMENTS

dev (local)
  ↓ code is pushed
staging (AWS)
  ↓ human approval
production (AWS)

Staging = identical infrastructure to production,
but with test data and a fraction of the scale.
If it works in staging, it will work in production.
This is the guarantee staging is supposed to provide.
```

---

## Part 4 — The Tool: GitHub Actions

FinVerse uses **GitHub Actions** for all CI/CD automation. Every pipeline is defined as a YAML file inside `.github/workflows/` in the repository. These files are version-controlled alongside the application code — if the pipeline needs to change, it goes through the same PR review process as any other code change.

```
REPOSITORY STRUCTURE (simplified)

core-product/
├── .github/
│   └── workflows/
│       ├── ci.yml              ← runs on every PR
│       ├── deploy-staging.yml  ← runs on merge to develop
│       └── deploy-production.yml ← runs on merge to main
├── src/
├── prisma/
├── Dockerfile
├── docker-compose.yml          ← local development only
└── package.json
```

---

## Part 5 — The Full Pipeline, Stage by Stage

Here is the complete pipeline, from a developer pushing code to production deployment.

```
COMPLETE CI/CD PIPELINE

Developer
    │
    │  git push → opens Pull Request to develop branch
    │
    ▼
┌──────────────────────────────────────────────────────────────┐
│  STAGE 1: CI PIPELINE (triggered on every PR)                │
│  GitHub Actions: ci.yml                                      │
│                                                              │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  Step 1: Checkout code                                  │ │
│  │  Step 2: Set up Node.js (version pinned in .nvmrc)      │ │
│  │  Step 3: Install dependencies (npm ci)                  │ │
│  │  Step 4: TypeScript type check (tsc --noEmit)           │ │
│  │  Step 5: Lint (ESLint)                                  │ │
│  │  Step 6: Unit tests (Jest)                              │ │
│  │  Step 7: Integration tests (Jest + test database)       │ │
│  │  Step 8: Security scan (npm audit --audit-level=high)   │ │
│  │  Step 9: Prisma schema validation                       │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                              │
│  If ANY step fails → PR cannot be merged                     │
│  Status posted to GitHub PR as check                         │
└─────────────────────┬────────────────────────────────────────┘
                      │ all checks pass
                      │ PR reviewed and approved by Lucas
                      │ PR merged to develop
                      ▼
┌──────────────────────────────────────────────────────────────┐
│  STAGE 2: BUILD & PUSH (triggered on merge to develop)       │
│  GitHub Actions: deploy-staging.yml                          │
│                                                              │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  Step 1: Checkout code                                  │ │
│  │  Step 2: Configure AWS credentials (via OIDC — no       │ │
│  │          long-lived secrets stored in GitHub)           │ │
│  │  Step 3: Login to AWS ECR                               │ │
│  │  Step 4: Build Docker image                             │ │
│  │          Tag with git SHA: abc1234                      │ │
│  │  Step 5: Run container security scan (Trivy)            │ │
│  │  Step 6: Push image to ECR                              │ │
│  │          ECR URI: 123456.dkr.ecr.eu-west-1.amazonaws    │ │
│  │          .com/core-product:abc1234                      │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────┬────────────────────────────────────────┘
                      │ image in ECR
                      ▼
┌──────────────────────────────────────────────────────────────┐
│  STAGE 3: STAGING DEPLOYMENT                                 │
│                                                              │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  Step 1: Run database migrations against staging DB     │ │
│  │          npx prisma migrate deploy                      │ │
│  │                                                         │ │
│  │  Step 2: Update ECS task definitions with new image tag │ │
│  │          (all services: api, workers)                   │ │
│  │                                                         │ │
│  │  Step 3: Trigger ECS rolling deployment on staging      │ │
│  │                                                         │ │
│  │  Step 4: Wait for ECS health checks to pass             │ │
│  │          (ECS polls /health endpoint on each container) │ │
│  │                                                         │ │
│  │  Step 5: Run smoke tests against staging                │ │
│  │          (basic API calls to verify key endpoints work) │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                              │
│  Notification posted to #deployments Slack channel           │
└─────────────────────┬────────────────────────────────────────┘
                      │ staging deployment confirmed
                      │ MANUAL APPROVAL required
                      │ (Lucas or senior engineer)
                      ▼
┌──────────────────────────────────────────────────────────────┐
│  STAGE 4: PRODUCTION DEPLOYMENT                              │
│  Triggered by: merge to main (after staging approval)        │
│  GitHub Actions: deploy-production.yml                       │
│                                                              │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  Step 1: Run database migrations against production DB  │ │
│  │          prisma migrate deploy                          │ │
│  │                                                         │ │
│  │  Step 2: Update ECS task definitions (production)       │ │
│  │                                                         │ │
│  │  Step 3: ECS rolling deployment (production)            │ │
│  │                                                         │ │
│  │  Step 4: Wait for health checks                         │ │
│  │                                                         │ │
│  │  Step 5: Smoke tests against production                 │ │
│  │          (non-destructive read-only checks only)        │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                              │
│  Notification posted to #deployments Slack channel           │
└──────────────────────────────────────────────────────────────┘
```

---

## Part 6 — The Dockerfile: What Gets Built

Every service in Core Product has its own Dockerfile. Here is the one for the main API service — annotated with exactly why each line exists.

```dockerfile
# Dockerfile — Core Product API Service

# Stage 1: Builder
# Use a specific Node.js version — never "latest"
# Pinning prevents surprise breakage when a new Node version drops
FROM node:20-alpine AS builder

WORKDIR /app

# Copy dependency files first — before source code
# Docker layer caching: if package.json hasn't changed,
# npm ci is skipped on rebuild (layers are cached)
# This makes rebuilds significantly faster
COPY package.json package-lock.json ./
COPY prisma ./prisma/

# npm ci = clean install, uses package-lock.json exactly
# Much faster and more deterministic than npm install
RUN npm ci

# Generate Prisma client (required before build)
# Prisma client is generated from the schema — must be done
# before TypeScript compilation which imports it
RUN npx prisma generate

# Copy source code AFTER dependencies
# Dependency layer only rebuilds when package.json changes
# Source layer rebuilds on every code change — much smaller layer
COPY src ./src
COPY tsconfig.json ./

# Compile TypeScript to JavaScript
RUN npm run build
# Output: dist/ directory containing compiled JS


# Stage 2: Production image
# Multi-stage build: only the compiled output goes into the
# final image — not dev dependencies, not TypeScript source,
# not the build tools
# This keeps the production image small and has fewer 
# attack surface (no dev packages)
FROM node:20-alpine AS production

WORKDIR /app

# Only install production dependencies — not devDependencies
# Reduces image size significantly
COPY package.json package-lock.json ./
COPY prisma ./prisma/
RUN npm ci --omit=dev
RUN npx prisma generate

# Copy compiled output from builder stage
COPY --from=builder /app/dist ./dist

# Do NOT run as root — security best practice
# If the container is ever compromised, the attacker
# has user-level privileges, not root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Document which port the service listens on
# This is documentation — Docker doesn't actually expose it
EXPOSE 3000

# Health check — ECS uses this to know if the container is healthy
# ECS won't send traffic to a container that fails this check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:3000/health || exit 1

# Start the compiled application
CMD ["node", "dist/main.js"]
```

**Why multi-stage builds matter:**

```
WITHOUT MULTI-STAGE:
  Image contains: Node.js + all npm packages (incl. devDeps) 
                  + TypeScript source + TypeScript compiler
                  + build tools
  Size: ~800MB–1.2GB

WITH MULTI-STAGE:
  Image contains: Node.js + production npm packages only
                  + compiled JS
  Size: ~180MB–250MB

Benefits:
  Faster ECS deployments (smaller image to pull)
  Reduced attack surface (no build tools, no source)
  Lower ECR storage costs
```

---

## Part 7 — ECS Rolling Deployment: Zero-Downtime Deploys

This is one of the most important concepts in the DevOps section for interviews. "How do you deploy without downtime?"

### The Problem

If you simply stop all running containers and start new ones:

```
WITHOUT ROLLING DEPLOYMENT

Time ──────────────────────────────────────────────────────────►

Old containers: ██████████████████████████░░░░░░░░░░░░░░░░░░░░░
                (running, serving traffic)  │
                                            │ All stopped simultaneously
                                            │
New containers:                             ░░░░░░░░░██████████████
                                            │         (starting up)
                                            │
Downtime window:                            ████████░
                                            │       │
                                         ~30s gap: no containers running
                                         All requests fail
                                         Mobile app shows errors
```

For a financial application, even 30 seconds of downtime is unacceptable and would generate support tickets.

### ECS Rolling Deployment

ECS performs a rolling deployment — it replaces containers gradually, keeping the service available throughout.

```
ECS ROLLING DEPLOYMENT CONFIGURATION

Service: core-product-api
Desired count: 3 containers (always)

Deployment settings:
  minimumHealthyPercent: 50   ← at least 50% of desired count
                               must be healthy at all times
                               (= at least 1 of 3 containers)
  maximumPercent: 150         ← can temporarily run up to 150% of
                               desired count during deployment
                               (= up to 4 containers temporarily)
```

How it plays out:

```
ROLLING DEPLOYMENT — TIMELINE

Initial state: 3 old containers running (v1), all healthy
               ALB distributing traffic evenly across all 3

Step 1: ECS launches 1 new container (v2)
  Old: [v1] [v1] [v1]   New: [v2 starting]
  Total: 4 containers (150% of desired — within maximum)

Step 2: New container passes health check
  ECS /health endpoint check: 200 OK
  ALB adds new container to target group
  Old: [v1] [v1] [v1]   New: [v2 ✅]
  Traffic now goes to 4 containers

Step 3: ECS stops one old container
  ALB drains connections from the stopped container
  (waits for in-flight requests to complete — typically 30s)
  Old: [v1] [v1]   New: [v2 ✅]
  Total: 3 containers (100% of desired)

Step 4: Repeat — launch another v2, stop another v1
  Old: [v1]   New: [v2 ✅] [v2 starting]
  → v2 becomes healthy
  Old: [v1]   New: [v2 ✅] [v2 ✅]
  → old v1 drained and stopped
  New: [v2 ✅] [v2 ✅]

Step 5: Final v2 launched, final v1 stopped
  New: [v2 ✅] [v2 ✅] [v2 ✅]

Total time: ~5-8 minutes depending on container startup time
Downtime: ZERO
```

The key mechanism is **connection draining** — before ECS stops an old container, the ALB stops sending new requests to it and waits for all in-flight requests to complete. No request is dropped mid-flight.

---

## Part 8 — Database Migrations in the Pipeline

This is the part most tutorials skip — and the part most likely to cause a production incident if done wrong.

### Why Migrations Are the Riskiest Part of Deployment

Application code and database schema must be compatible at all times. During a rolling deployment, both the old version and new version of the application run simultaneously for a few minutes. If your migration makes a schema change that breaks the old version, you cause errors during the overlap window.

```
DANGEROUS MIGRATION EXAMPLE

Migration: rename column "description" to "merchantDescription"

Old app (v1): SELECT description FROM transactions
New app (v2): SELECT merchantDescription FROM transactions

During rolling deployment:
  Old v1 containers still running
  Migration runs: column renamed to merchantDescription
  v1 containers: SELECT description → column not found → 500 ERROR
  Requests served by old containers fail until they are stopped

This is a BREAKING MIGRATION — it breaks old code.
```

### FinVerse's Safe Migration Approach: Expand-Contract

The team follows the expand-contract pattern for any non-additive change. Additive changes (new columns, new tables, new indexes) are safe and deploy normally. Breaking changes require a multi-step process.

```
EXPAND-CONTRACT PATTERN — RENAMING A COLUMN

Deploy 1 — EXPAND:
  Migration: ADD COLUMN merchantDescription TEXT
  App code: reads description AND writes to both columns
  Both old and new code work — old reads old column, new reads new

Deploy 2 — MIGRATE (background job):
  Backfill existing rows:
  UPDATE transactions 
  SET merchantDescription = description 
  WHERE merchantDescription IS NULL

Deploy 3 — SWITCH:
  Migration: make description nullable
  App code: reads only merchantDescription
  Old code: still reads description (still exists, still populated)

Deploy 4 — CONTRACT:
  Migration: DROP COLUMN description
  Old code: not running anymore — only new code in production
```

This means any single deployment is backward-compatible with the previous version. Rollback is always safe.

### How Migrations Run in the Pipeline

```yaml
# deploy-staging.yml — migration step
- name: Run database migrations
  run: |
    # prisma migrate deploy applies only PENDING migrations
    # It does NOT regenerate or create new migrations
    # Safe to run in CI — read-only if nothing is pending
    npx prisma migrate deploy
  env:
    DATABASE_URL: ${{ secrets.STAGING_DATABASE_URL }}
```

**Why migrations run before the application containers update:**

```
MIGRATION ORDER MATTERS

Wrong order:
  1. Deploy new application containers (referencing new column)
  2. Run migrations (column doesn't exist yet)
  
  Time window between 1 and 2:
  New app tries to SELECT newColumn → column not found → crashes

Correct order:
  1. Run migrations (new column added to schema)
  2. Deploy new application containers

  Database always has what the application expects.
  Old containers continue to work (additive migration = backward compatible).
  New containers start up and find the new column ready.
```

---

## Part 9 — The Health Check Endpoint

ECS uses the `/health` endpoint to decide if a container is healthy and ready to receive traffic. If this endpoint returns a non-200 response, ECS will:

- Not add the container to the ALB target group
- Eventually stop the container and try to replace it

Here is what FinVerse's health check endpoint checks:

```typescript
// health.controller.ts
@Controller('health')
export class HealthController {

  constructor(
    private readonly health: HealthCheckService,
    private readonly prismaHealth: PrismaHealthIndicator,
    private readonly redisHealth: RedisHealthIndicator,
  ) {}

  @Get()
  @HealthCheck()
  async check() {
    return this.health.check([

      // Can we reach PostgreSQL?
      () => this.prismaHealth.pingCheck('database'),

      // Can we reach Redis?
      () => this.redisHealth.pingCheck('redis'),

      // Basic memory check — alert if Node.js heap is near limit
      () => this.health.check([
        async () => {
          const used = process.memoryUsage().heapUsed / 1024 / 1024
          const limit = 1536  // 1.5GB — container has 2GB
          return {
            memory: {
              status: used < limit ? 'up' : 'down',
              heapUsedMb: Math.round(used),
            }
          }
        }
      ]),
    ])
  }
}
```

**What returns from `/health` on success:**

```json
{
  "status": "ok",
  "info": {
    "database": { "status": "up" },
    "redis": { "status": "up" },
    "memory": { "status": "up", "heapUsedMb": 245 }
  }
}
```

**What the pipeline waits for:**

```yaml
# deploy-staging.yml
- name: Wait for ECS deployment to stabilise
  run: |
    aws ecs wait services-stable \
      --cluster finverse-staging \
      --services core-product-api
    # This command blocks until ECS reports the service as stable
    # (all desired tasks running and passing health checks)
    # Times out after 10 minutes — fails the pipeline if exceeded
```

---

## Part 10 — Secrets Management

All sensitive values — database passwords, GoCardless API keys, Stripe keys, Redis auth tokens — are stored in **AWS Secrets Manager**, not in environment variables hardcoded into the pipeline or the Docker image.

```
HOW SECRETS REACH THE CONTAINER

AWS Secrets Manager
  secret: finverse/production/core-product
  value: {
    DATABASE_URL: "postgresql://...",
    REDIS_PASSWORD: "...",
    GOCARDLESS_SECRET_ID: "...",
    GOCARDLESS_SECRET_KEY: "...",
    STRIPE_SECRET_KEY: "...",
    JWT_SECRET: "..."
  }
                │
                │ ECS task definition references the secret ARN
                │
                ▼
ECS Fargate Container
  At container start, ECS injects secrets as environment variables
  Application reads: process.env.DATABASE_URL etc.
  Secrets never appear in Dockerfile, docker-compose, or GitHub Actions logs
```

The GitHub Actions pipeline itself uses **OIDC (OpenID Connect)** to authenticate to AWS — no long-lived AWS access keys stored as GitHub secrets. The pipeline assumes an IAM role temporarily for the duration of the deployment:

```yaml
# deploy-production.yml
- name: Configure AWS credentials via OIDC
  uses: aws-actions/configure-aws-credentials@v4
  with:
    role-to-assume: arn:aws:iam::123456789:role/github-actions-deploy-role
    aws-region: eu-west-1
    # No ACCESS_KEY_ID or SECRET_ACCESS_KEY stored anywhere
    # GitHub generates a short-lived token via OIDC
    # AWS verifies the token and grants the role temporarily
```

---

## Part 11 — Branch Strategy and the Deployment Flow

```
BRANCH STRATEGY

main
  ↑ merged from develop (weekly, after staging approval)
  → triggers production deployment
  
develop
  ↑ merged from feature/* branches (via PR, after CI passes)
  → triggers staging deployment automatically

feature/{ticket-id}-{short-description}
  ↑ created by engineers for each ticket
  → CI runs on every push
  → merged to develop after:
      1. CI passes (all automated checks)
      2. At least 1 approval (Lucas or senior engineer)

fix/{ticket-id}-{short-description}
  ↑ same as feature — bug fixes follow the same path

hotfix/{ticket-id}-{short-description}
  ↑ branches from main directly
  → CI runs
  → merged to BOTH main AND develop after approval
  → triggers production deployment immediately
  (used for critical production bugs that cannot wait for
   the normal weekly release cycle)
```

The weekly release cadence:

```
Monday:    Sprint Review → Lucas + PM sign off on what's in develop
Thursday:  develop merged to main → production deployment triggered
           Deployment happens in the morning (EU time, before peak traffic)
           All-hands: no new merges to develop 2 hours before/after deployment

Friday:    Buffer day — no merges, no releases
           Unwritten rule: never deploy on Friday afternoon
           (nobody wants to be on-call over the weekend for a deployment issue)
```

---

## Part 12 — What the Smoke Tests Check

After each deployment (staging and production), the pipeline runs a set of smoke tests — lightweight API calls that verify the application is actually working, not just running.

```typescript
// smoke-tests/index.ts
// Runs against the deployed environment after deployment

const BASE_URL = process.env.SMOKE_TEST_URL  // staging or prod base URL

async function runSmokeTests() {
  const results = []

  // Test 1: Health endpoint
  const health = await fetch(`${BASE_URL}/health`)
  results.push({
    name: 'health_check',
    passed: health.status === 200
  })

  // Test 2: Auth endpoint reachable
  // (uses a dedicated smoke-test service account token)
  const token = await getServiceAccountToken()
  const accounts = await fetch(`${BASE_URL}/v1/accounts`, {
    headers: { Authorization: `Bearer ${token}` }
  })
  results.push({
    name: 'accounts_endpoint_auth',
    passed: accounts.status === 200
  })

  // Test 3: Database connectivity (via health response)
  const healthBody = await health.json()
  results.push({
    name: 'database_healthy',
    passed: healthBody.info.database.status === 'up'
  })

  // Test 4: Redis connectivity
  results.push({
    name: 'redis_healthy',
    passed: healthBody.info.redis.status === 'up'
  })

  const failed = results.filter(r => !r.passed)
  if (failed.length > 0) {
    console.error('Smoke tests failed:', failed)
    process.exit(1)   // non-zero exit = pipeline fails
  }

  console.log('All smoke tests passed')
}
```

Smoke tests are intentionally lightweight. They verify the application is up and connected to its dependencies — not that every feature works correctly. That is what the CI test suite (unit + integration) handles before the deployment.

**Production smoke tests are read-only.** They never write data to production. No creating test users, no triggering syncs, no sending notifications. Just reading existing data via a dedicated service account.

---

## Part 13 — Rollback: What Happens When a Deployment Goes Wrong

This is the question interviewers almost always ask: "What do you do if something breaks after a deployment?"

### Option 1 — Automatic Rollback (ECS Health Check Failure)

If the new containers fail their health checks, ECS never routes traffic to them. The old containers continue running. From the user's perspective, nothing changed.

```
HEALTH CHECK FAILURE ROLLBACK

Deployment starts:
  New container (v2) launches
  Health check fires: GET /health → 500 (something is wrong)
  
ECS behaviour:
  Container marked UNHEALTHY
  NOT added to ALB target group
  OLD containers (v1) continue serving all traffic
  
  After 3 consecutive failed health checks:
  ECS stops the new container
  Tries to launch a replacement → same result
  
  After hitting the circuit breaker threshold:
  ECS marks the deployment as FAILED
  Pipeline fails
  Alert fires → #incidents Slack
  
  Outcome: production untouched, old version still running
```

### Option 2 — Manual Rollback (Bug Discovered After Deployment)

If the new containers pass health checks but a bug is discovered in production after deployment, the rollback is:

```
MANUAL ROLLBACK STEPS

1. Engineer runs:
   aws ecs update-service \
     --cluster finverse-production \
     --service core-product-api \
     --task-definition core-product-api:PREVIOUS_REVISION
   
   (Each deployment creates a new task definition revision.
    PREVIOUS_REVISION points to the last known-good image tag.)

2. ECS performs a rolling update — same process as deployment,
   but in reverse (new = previous version, old = broken version)

3. Takes ~5 minutes to complete

4. Notifications posted to #deployments Slack
```

**Why rollback is fast at FinVerse:** the previous Docker image is already in ECR — no rebuild required. ECS just points to the old image tag. The rollback is a simple configuration change.

### The Critical Caveat — Migrations

Rolling back application code is straightforward. Rolling back database migrations is not.

```
MIGRATION ROLLBACK PROBLEM

Deployment deployed a new migration:
  ALTER TABLE bank_accounts ADD COLUMN syncRetryCount INT DEFAULT 0

Bug discovered → application code rolled back to previous version.

The previous version of the code does NOT reference syncRetryCount.
But the column now exists in the database.

Is this a problem? 
  NO — the old code simply ignores the extra column.
  PostgreSQL happily returns it; the old ORM ignores fields it
  doesn't know about.
  
  ADDITIVE MIGRATIONS are backward compatible.
  Rolling back the application code is safe.
  The column stays. It does no harm.

What about a DROP COLUMN migration?
  Old code: SELECT description FROM transactions
  Migration: DROP COLUMN description
  Rollback to old code: SELECT description → column not found → ERROR

  This is why DROP COLUMN is a Stage 4 operation in the 
  expand-contract pattern — only after the old code is 
  completely gone from production and staging.
  By the time you drop the column, rollback to the old code
  is no longer on the table.
```

---

## Part 14 — The CI YAML: What It Actually Looks Like

Here is an abbreviated but realistic version of the CI workflow file:

```yaml
# .github/workflows/ci.yml

name: CI

on:
  pull_request:
    branches: [develop, main]
  push:
    branches: [develop, main]

jobs:
  ci:
    runs-on: ubuntu-latest

    services:
      # Spin up PostgreSQL and Redis for integration tests
      postgres:
        image: postgres:15-alpine
        env:
          POSTGRES_DB: finverse_test
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

      redis:
        image: redis:7-alpine
        ports:
          - 6379:6379
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version-file: '.nvmrc'  # reads from .nvmrc — version pinned
          cache: 'npm'

      - name: Install dependencies
        run: npm ci

      - name: Generate Prisma client
        run: npx prisma generate

      - name: TypeScript type check
        run: npx tsc --noEmit

      - name: Lint
        run: npx eslint src --ext .ts

      - name: Run database migrations (test database)
        run: npx prisma migrate deploy
        env:
          DATABASE_URL: postgresql://test:test@localhost:5432/finverse_test

      - name: Run unit tests
        run: npx jest --testPathPattern="\.unit\.spec\.ts$" --coverage

      - name: Run integration tests
        run: npx jest --testPathPattern="\.integration\.spec\.ts$"
        env:
          DATABASE_URL: postgresql://test:test@localhost:5432/finverse_test
          REDIS_URL: redis://localhost:6379

      - name: Security audit
        run: npm audit --audit-level=high
        # Fails CI if any high or critical severity vulnerabilities found

      - name: Upload test coverage
        uses: codecov/codecov-action@v3
        if: success()
```

---

## Part 15 — How the Team Talks About This in Practice

In sprint planning, deployment is not a separate task — it is the natural end of every feature ticket. When a ticket moves from "In Review" to "Done," the deployment to staging has happened. Production deployment happens at the Thursday release.

**What Lucas monitors on release day:**

```
THURSDAY DEPLOYMENT CHECKLIST (informal, mental model)

Before triggering production deployment:
  □ staging has been running the new version for ≥ 24 hours
  □ no Datadog alerts fired in staging since deployment
  □ any migration that ran in staging has been verified
    (checked that no query plans broke, checked error logs)

During production deployment:
  □ Datadog dashboard open — watching error rate, latency
  □ ECS console open — watching task health
  □ #deployments Slack channel open

After deployment completes:
  □ smoke tests passed (automated)
  □ error rate in Datadog: same as before deployment
  □ p95 latency: same as before deployment
  □ no new alerts fired
  □ spot check 2-3 key API endpoints manually

If anything looks wrong: manual rollback immediately.
Do not debug a production issue in production.
Rollback first, investigate in staging.
```

That last line — "rollback first, investigate in staging" — is a culture point. It is repeated by Lucas in retrospectives whenever a deployment causes even a minor incident.

---

## Summary

```
┌─────────────────────────────────────────────────────────────────┐
│                    STEP 11 SUMMARY                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  TOOL: GitHub Actions — YAML files version-controlled           │
│  in the repository                                              │
│                                                                 │
│  PIPELINE STAGES:                                               │
│  1. CI on every PR — type check, lint, unit + integration       │
│     tests, security audit, Prisma validation                    │
│  2. Build & push Docker image to ECR (tagged by git SHA)        │
│  3. Staging deployment — migrations first, then rolling         │
│     ECS update, then smoke tests                                │
│  4. Manual approval → production deployment (same steps)        │
│                                                                 │
│  DOCKER: multi-stage builds — production image is small,        │
│  contains only compiled JS and production dependencies          │
│                                                                 │
│  DEPLOYMENT: ECS rolling deployment with connection draining    │
│  — zero downtime, old containers drain before stopping          │
│                                                                 │
│  MIGRATIONS: run before container updates, always additive,     │
│  expand-contract for breaking changes                           │
│                                                                 │
│  SECRETS: AWS Secrets Manager, OIDC authentication from         │
│  GitHub Actions — no long-lived credentials anywhere            │
│                                                                 │
│  ROLLBACK: ECS task definition revision rollback in ~5 min      │
│  for code; migration rollback only needed for DROP operations   │
│  (avoided by expand-contract pattern)                           │
│                                                                 │
│  BRANCH STRATEGY: feature → develop (staging) → main (prod)     │
│  Weekly Thursday release cadence, never deploy Friday PM        │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

