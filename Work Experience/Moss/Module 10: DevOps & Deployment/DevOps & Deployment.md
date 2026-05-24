# Step 9: CI/CD & DevOps

---

## Start With The Context — What Does "Shipping Code" Look Like at Series B?

Before any pipeline details, understand the operating reality:

```
Series B/C realities that shape our CI/CD:
────────────────────────────────────────────
- Small DevOps team (2-3 people) — 
  can't maintain complex infrastructure
- ~180 engineers shipping code daily —
  pipeline must be fast and reliable
- Financial data — can't afford broken 
  deployments to production
- Fully remote, EU timezone — 
  deployments happen during business hours,
  not 2am like big tech

What this means in practice:
──────────────────────────────
- GitHub Actions (not Jenkins, not 
  a custom build farm) — hosted, 
  no server to maintain
- AWS ECS (not Kubernetes) — 
  simpler to operate for small DevOps team
- 3 environments: dev, staging, prod —
  not 6, not 1
- Feature flags over risky big-bang releases —
  ship often, safely
```

---

## Part 1 — Branch Strategy

Everything in CI/CD flows from how your team manages branches. Ours is straightforward.

```
BRANCH STRATEGY: GitHub Flow (simplified)
──────────────────────────────────────────

main branch:
────────────
Always deployable.
Represents what's in production (or about to be).
Direct pushes blocked — PRs only.
Requires 2 approvals + all CI checks green.

feature branches:
─────────────────
Created from main for every piece of work.
Naming convention:
  feature/EXP-234-add-expense-validation
  fix/EXP-289-fix-amount-rounding
  chore/EXP-301-update-flyway-migration

Short-lived — merged within 1-2 sprints.
Long-lived branches = merge conflicts = pain.

release branches (when needed):
────────────────────────────────
For coordinated releases across services
(e.g., a new Kafka topic that requires 
 producer and consumer to deploy together).
rare — most releases are independent.

NO gitflow:
────────────
Gitflow (develop, release, hotfix branches)
is over-engineered for our team size.
GitHub Flow is simpler and works at our scale.
```

```
BRANCH LIFECYCLE:
──────────────────
Developer creates branch from main
  │
  │ work, commits, pushes
  ▼
PR opened → CI pipeline triggers
  │
  │ review, feedback, fixes
  ▼
2 approvals + CI green
  │
  ▼
Squash merge to main
(squash = all commits become 1 clean commit)
  │
  ▼
Feature branch deleted
  │
  ▼
main auto-deploys to staging
  │
  ▼ (manual approval gate)
  ▼
main deployed to production
```

**Why squash merge?**

```
During feature development, you commit often:
"WIP", "fix typo", "add test", "fix review comment"

These are noise in git history.
Squash merge collapses them into one 
meaningful commit on main:
"EXP-234: Add expense amount validation"

main branch history stays clean and readable.
Git bisect works properly on clean history.
```

---

## Part 2 — The Full CI/CD Pipeline

### Overview

```
                    DEVELOPER PUSHES TO FEATURE BRANCH
                                │
                                ▼
                    ┌───────────────────────┐
                    │   PR CI PIPELINE      │
                    │   (runs on every push │
                    │    to a PR)           │
                    └───────────┬───────────┘
                                │
              ┌─────────────────┼─────────────────┐
              ▼                 ▼                 ▼
         Unit Tests      Integration         Static
         (JUnit 5,       Tests               Analysis
         Mockito)        (Testcontainers)    (SonarQube,
                                             SpotBugs)
              │                 │                 │
              └─────────────────┴─────────────────┘
                                │
                         All green? ✅
                                │
                    PR reviewed + approved
                                │
                    Squash merge to main
                                │
                                ▼
                    ┌───────────────────────┐
                    │   MAIN CI PIPELINE    │
                    │   (runs on merge      │
                    │    to main)           │
                    └───────────┬───────────┘
                                │
              ┌─────────────────┼──────────────────┐
              ▼                 ▼                  ▼
         Full test         Docker image        Security
         suite again       build               scan
         (safety net)      (tagged with        (Trivy)
                           git SHA)
                                │
                                ▼
                    Push image to AWS ECR
                    (Elastic Container Registry)
                                │
                                ▼
                    Auto-deploy to STAGING
                                │
                                ▼
                    Smoke tests on staging
                                │
                                ▼
                    ┌───────────────────────┐
                    │   MANUAL APPROVAL     │
                    │   (one person from    │
                    │    team approves)     │
                    └───────────┬───────────┘
                                │
                                ▼
                    Deploy to PRODUCTION
                                │
                                ▼
                    Post-deploy health checks
```

---

### The PR Pipeline in Detail

This runs on every push to a PR branch. Fast feedback is the goal — engineers shouldn't wait 20 minutes to know if tests pass.

```yaml
# .github/workflows/pr-checks.yml

name: PR Checks

on:
  pull_request:
    branches: [ main ]

jobs:

  unit-tests:
    name: Unit Tests
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4

      - name: Set up Java 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: 'maven'    # cache Maven dependencies
                            # speeds up from 4min to 90sec

      - name: Run unit tests
        run: mvn test -pl expense-service
             # -pl = only build the changed service
             # not the whole monorepo

      - name: Upload test results
        if: always()        # upload even if tests fail
        uses: actions/upload-artifact@v4
        with:
          name: unit-test-results
          path: expense-service/target/surefire-reports/

  integration-tests:
    name: Integration Tests
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4

      - name: Set up Java 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: 'maven'

      - name: Run integration tests
        run: mvn verify -P integration-tests
             -pl expense-service
        # Testcontainers spins up real PostgreSQL
        # and Kafka in Docker — tests run against them

  static-analysis:
    name: Static Analysis
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0    # SonarQube needs full history

      - name: Set up Java 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: 'maven'

      - name: SonarQube Analysis
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        run: mvn sonar:sonar
             -Dsonar.projectKey=expense-service
             -Dsonar.host.url=${{ secrets.SONAR_URL }}

      # SonarQube Quality Gate:
      # - Code coverage > 80%
      # - No new critical bugs
      # - No new security vulnerabilities
      # - Technical debt ratio < 5%
      # If gate fails → PR cannot merge
```

---

### Integration Tests With Testcontainers

This deserves its own explanation — it's one of the more impressive parts of our test setup.

```
THE PROBLEM WITH MOCKING DATABASES:
─────────────────────────────────────
Unit tests mock the repository layer.
You test your service logic, not SQL.

But bugs often live at the SQL level:
- Wrong JOIN type
- Missing index causes timeout
- Flyway migration has wrong column name
- JPA query produces unexpected results

Mocking PostgreSQL doesn't catch these.

TESTCONTAINERS SOLUTION:
─────────────────────────
Testcontainers is a Java library.
It spins up REAL Docker containers 
during test execution.
Real PostgreSQL. Real Kafka.
Tests run against actual infrastructure.
Container is torn down after tests.
No shared state. Reproducible every time.
```

```java
// Integration test setup
@SpringBootTest
@Testcontainers
class ExpenseServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("expense_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka =
        new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.4.0")
        );

    @DynamicPropertySource
    static void configureProperties(
            DynamicPropertyRegistry registry) {

        // Override application.properties with 
        // container connection details
        registry.add("spring.datasource.url",
            postgres::getJdbcUrl);
        registry.add("spring.datasource.username",
            postgres::getUsername);
        registry.add("spring.datasource.password",
            postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers",
            kafka::getBootstrapServers);
    }

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Test
    void approveExpense_shouldUpdateStatusAndWriteOutboxEvent() {

        // Arrange — create real DB records
        Company company = companyRepository.save(
            Company.builder()
                .name("Test Company")
                .countryCode("DE")
                .currency("EUR")
                .build()
        );

        Expense expense = expenseRepository.save(
            Expense.builder()
                .companyId(company.getId())
                .employeeId(UUID.randomUUID())
                .amount(new BigDecimal("85.00"))
                .currency("EUR")
                .category(ExpenseCategory.TRAVEL)
                .status(ExpenseStatus.PENDING_APPROVAL)
                .assignedApproverId(UUID.randomUUID())
                .build()
        );

        // Act
        expenseService.approveExpense(
            expense.getId(),
            expense.getAssignedApproverId(),
            "Approved"
        );

        // Assert — verify in real DB
        Expense updated = expenseRepository
            .findById(expense.getId()).orElseThrow();
        assertThat(updated.getStatus())
            .isEqualTo(ExpenseStatus.APPROVED);

        // Verify outbox event was written
        List<OutboxEvent> events = outboxEventRepository
            .findByAggregateId(expense.getId());
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getEventType())
            .isEqualTo("expense.approved");
    }
}
```

---

### The Main Branch Pipeline — Build and Deploy

```yaml
# .github/workflows/main-deploy.yml

name: Build and Deploy

on:
  push:
    branches: [ main ]

env:
  AWS_REGION: eu-central-1
  ECR_REGISTRY: ${{ secrets.ECR_REGISTRY }}
  IMAGE_NAME: expense-service

jobs:

  build-and-push:
    name: Build Docker Image
    runs-on: ubuntu-latest
    outputs:
      image-tag: ${{ steps.tag.outputs.tag }}

    steps:
      - uses: actions/checkout@v4

      - name: Generate image tag
        id: tag
        # Tag = git SHA (first 8 chars)
        # Every build is uniquely identifiable
        # Can trace any running container
        # back to exact commit
        run: |
          echo "tag=$(git rev-parse --short HEAD)" \
          >> $GITHUB_OUTPUT

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ env.AWS_REGION }}

      - name: Log in to Amazon ECR
        uses: aws-actions/amazon-ecr-login@v2

      - name: Set up Java 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: 'maven'

      - name: Run full test suite
        run: mvn verify -pl expense-service

      - name: Build Docker image
        run: |
          docker build \
            -t $ECR_REGISTRY/$IMAGE_NAME:${{ steps.tag.outputs.tag }} \
            -t $ECR_REGISTRY/$IMAGE_NAME:latest \
            -f expense-service/Dockerfile \
            expense-service/

      - name: Security scan with Trivy
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: >
            ${{ env.ECR_REGISTRY }}/${{ env.IMAGE_NAME }}:
            ${{ steps.tag.outputs.tag }}
          format: 'sarif'
          severity: 'CRITICAL,HIGH'
          exit-code: '1'   # fail pipeline on critical CVEs

      - name: Push to ECR
        run: |
          docker push \
            $ECR_REGISTRY/$IMAGE_NAME:${{ steps.tag.outputs.tag }}
          docker push \
            $ECR_REGISTRY/$IMAGE_NAME:latest

  deploy-staging:
    name: Deploy to Staging
    needs: build-and-push
    runs-on: ubuntu-latest
    environment: staging

    steps:
      - name: Deploy to ECS Staging
        uses: aws-actions/amazon-ecs-deploy-task-definition@v1
        with:
          task-definition: expense-service-staging
          service: expense-service-staging
          cluster: moss-staging
          image: >
            ${{ env.ECR_REGISTRY }}/${{ env.IMAGE_NAME }}:
            ${{ needs.build-and-push.outputs.image-tag }}
          wait-for-service-stability: true
          # Waits until ECS reports service 
          # as stable (health checks passing)

      - name: Run smoke tests
        run: |
          # Hit the health endpoint
          curl --fail \
            https://staging-api.moss.internal/actuator/health
          
          # Hit a key business endpoint
          curl --fail \
            -H "Authorization: Bearer ${{ secrets.STAGING_TEST_TOKEN }}" \
            https://staging-api.moss.internal/api/v1/expenses?page=0&size=1

  deploy-production:
    name: Deploy to Production
    needs: deploy-staging
    runs-on: ubuntu-latest
    environment: production   
    # 'production' environment in GitHub 
    # requires manual approval from 
    # a configured reviewer before running

    steps:
      - name: Deploy to ECS Production
        uses: aws-actions/amazon-ecs-deploy-task-definition@v1
        with:
          task-definition: expense-service-prod
          service: expense-service-prod
          cluster: moss-production
          image: >
            ${{ env.ECR_REGISTRY }}/${{ env.IMAGE_NAME }}:
            ${{ needs.build-and-push.outputs.image-tag }}
          wait-for-service-stability: true

      - name: Post-deploy health check
        run: |
          sleep 30   # give service time to warm up
          curl --fail \
            https://api.getmoss.com/actuator/health

      - name: Notify Slack on success
        uses: slackapi/slack-github-action@v1
        with:
          channel-id: ${{ secrets.SLACK_DEPLOY_CHANNEL }}
          slack-bot-token: ${{ secrets.SLACK_BOT_TOKEN }}
          payload: |
            {
              "text": "✅ expense-service deployed to production",
              "blocks": [{
                "type": "section",
                "text": {
                  "type": "mrkdwn",
                  "text": "✅ *expense-service* deployed\nCommit: ${{ github.sha }}\nBy: ${{ github.actor }}"
                }
              }]
            }
```

---

## Part 3 — The Dockerfile

```dockerfile
# expense-service/Dockerfile

# ── Stage 1: Build ─────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy POM first — Docker layer caching.
# If only source code changed (not dependencies),
# Maven doesn't re-download all dependencies.
# This alone saves 2-3 minutes per build.
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Now copy source code
COPY src ./src

# Build the jar, skip tests 
# (tests already ran in CI step)
RUN mvn package -DskipTests -q

# ── Stage 2: Runtime ───────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime
# Alpine = minimal Linux image (~5MB base)
# JRE not JDK — no compiler in production
# Smaller image = faster pulls, smaller attack surface

WORKDIR /app

# Create non-root user for security
# Never run Java apps as root in containers
RUN addgroup -S moss && adduser -S moss -G moss
USER moss

# Copy only the built jar from stage 1
COPY --from=builder /app/target/expense-service-*.jar \
     app.jar

# Expose port
EXPOSE 8080

# JVM tuning for containers:
# -XX:+UseContainerSupport  → JVM respects 
#   container memory limits (not host memory)
# -XX:MaxRAMPercentage=75   → use 75% of 
#   container memory for heap
# -XX:+ExitOnOutOfMemoryError → crash fast 
#   on OOM rather than limping along
#   (ECS will restart the container)
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-jar", "app.jar"]
```

**Why multi-stage build?**

```
Stage 1 (builder) image contains:
- Maven
- Full JDK
- All source code
- All build tools
~600MB image

Stage 2 (runtime) image contains:
- Only the compiled JAR
- JRE (not JDK)
- Alpine Linux
~120MB image

We ship Stage 2 to production.
Stage 1 is discarded after build.
Smaller image = faster ECS pulls = 
faster deployments and less ECR storage cost.
```

---

## Part 4 — AWS ECS Setup

```
HOW ECS WORKS (simply):
─────────────────────────

Task Definition:
────────────────
A blueprint describing how to run a container.
Defines:
- Which Docker image (from ECR)
- CPU and memory allocation
- Environment variables
- IAM role (what AWS resources it can access)
- Log configuration (CloudWatch)

Service:
─────────
Keeps N copies of a Task Definition running.
If a task crashes → ECS starts a replacement.
Handles rolling deployments automatically.

Cluster:
─────────
Group of compute resources (EC2 or Fargate)
where tasks run.
We use Fargate — serverless containers.
No EC2 instances to manage.
Pay per task runtime.

Our setup:
──────────
expense-service:
  Staging:    1 task, 0.5 vCPU, 1GB RAM
  Production: 2 tasks, 1 vCPU, 2GB RAM
              (2 tasks for redundancy —
               if one crashes, one serves traffic)
```

```
ROLLING DEPLOYMENT PROCESS:
─────────────────────────────
GitHub Actions triggers ECS deployment.

ECS rolling update:
1. Start new task with new image
   (now: 2 old + 1 new = 3 tasks briefly)
2. Health check passes on new task
3. Stop one old task
   (now: 1 old + 1 new)
4. Start another new task
   (now: 1 old + 2 new)
5. Health check passes
6. Stop last old task
   (now: 0 old + 2 new)

Result: zero downtime deployment.
Traffic always being served.
Old and new versions briefly coexist —
this matters for API compatibility.

Minimum healthy percent: 50%
Maximum healthy percent: 150%
These ECS settings control the 
overlap during deployment.
```

---

## Part 5 — Environment Strategy

```
THREE ENVIRONMENTS:
────────────────────

┌──────────┬──────────────────────────────────────────────┐
│ ENV      │ PURPOSE                                      │
├──────────┼──────────────────────────────────────────────┤
│ dev      │ Developers' personal testing.                │
│          │ Docker Compose on local machine.             │
│          │ Not a shared environment.                    │
│          │ No CI/CD — developer runs manually.          │
├──────────┼──────────────────────────────────────────────┤
│ staging  │ Shared pre-production environment.           │
│          │ Auto-deployed on every merge to main.        │
│          │ QA testing (David runs his tests here).      │
│          │ Integration tests with other services.       │
│          │ Same infrastructure as prod (smaller scale). │
│          │ Real Kafka, real PostgreSQL, real Redis.     │
│          │ No real money movement (Payment Service      │
│          │ uses sandbox mode).                          │
├──────────┼──────────────────────────────────────────────┤
│ prod     │ Live customers.                              │
│          │ Manual approval required before deploy.      │
│          │ Only deployable during business hours        │
│          │ (team rule — no Friday afternoon deploys).   │
│          │ Full monitoring + alerting.                  │
└──────────┴──────────────────────────────────────────────┘
```

**Environment-specific configuration:**

```
Config is managed via Spring Cloud Config Server.
Same code, different configuration per environment.
Secrets from AWS Secrets Manager — never in code.

expense-service.yml (common):
  spring.jpa.show-sql: false
  moss.expense.max-amount: 50000

expense-service-staging.yml:
  spring.jpa.show-sql: true   (helpful for debugging)
  logging.level.com.moss: DEBUG
  moss.feature.ai-coding: true   (test new features)

expense-service-prod.yml:
  spring.jpa.show-sql: false
  logging.level.com.moss: INFO
  moss.feature.ai-coding: false  (until fully tested)

Secrets (never in config files):
  spring.datasource.password  → AWS Secrets Manager
  spring.kafka.ssl.key        → AWS Secrets Manager
  redis.password              → AWS Secrets Manager
```

---

## Part 6 — Local Development Setup

```
Every developer can run the full 
service locally with one command.
Docker Compose handles all dependencies.
```

```yaml
# docker-compose.yml (in repo root)

version: '3.8'

services:

  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: expense_db
      POSTGRES_USER: moss
      POSTGRES_PASSWORD: localdev
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  kafka:
    image: confluentinc/cp-kafka:7.4.0
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: >
        PLAINTEXT://localhost:9092
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
    ports:
      - "9092:9092"
    depends_on:
      - zookeeper

  zookeeper:
    image: confluentinc/cp-zookeeper:7.4.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  # Mock for User & Org Service
  # (we don't run other teams' services locally)
  wiremock:
    image: wiremock/wiremock:3.3.1
    ports:
      - "8090:8080"
    volumes:
      - ./wiremock:/home/wiremock
      # contains JSON stubs for User & Org API

volumes:
  postgres_data:
```

```
Developer workflow:
────────────────────
docker compose up -d        → starts all dependencies
mvn spring-boot:run         → starts expense-service locally
                              connects to local postgres/redis/kafka

FeignClient calls to User & Org Service 
are routed to WireMock stubs locally.
No need to run other teams' services.

Why WireMock instead of mocking in code?
──────────────────────────────────────────
WireMock runs as a real HTTP server.
Your FeignClient makes actual HTTP calls.
Tests/local dev behave identically 
to production in terms of HTTP layer.
Timeouts, retries, error responses 
all behave realistically.
```

---

## Part 7 — Feature Flags

```
WHY WE USE FEATURE FLAGS:
──────────────────────────
Sometimes a feature is too big to 
ship all at once safely.

Or we want to test with a subset of 
companies before rolling out to all 5,000.

Or we want to be able to instantly 
turn off something that's causing 
production issues without a rollback.

HOW WE DO IT (simple approach at Series B):
────────────────────────────────────────────
Configuration-based flags, 
managed via Spring Cloud Config.

expense-service-prod.yml:
  moss.feature.ai-precoding: false
  moss.feature.new-approval-ui: true
  moss.feature.multi-entity: false

In code:
```

```java
@Component
@ConfigurationProperties(prefix = "moss.feature")
@Data
public class FeatureFlags {
    private boolean aiPrecoding;
    private boolean newApprovalUi;
    private boolean multiEntity;
}

// Usage in service:
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final FeatureFlags featureFlags;

    public ExpenseResponse createExpense(...) {

        // Core logic always runs

        if (featureFlags.isAiPrecoding()) {
            // Try AI category suggestion
            // Only for companies in feature flag cohort
            aiCodingService.suggestCategory(expense);
        }

        // Rest of logic
    }
}
```

```
Turning off a feature:
───────────────────────
Update Spring Cloud Config:
  moss.feature.ai-precoding: false

Services refresh config automatically 
(Spring Cloud Config + @RefreshScope).
No redeployment needed.
Feature disabled within seconds.

This is important during incidents.
If ai-precoding is causing slowdowns,
we flip the flag — problem gone in seconds.
Without feature flags: we'd need 
a full rollback deployment (5-10 minutes).
```

---

## Part 8 — Rollback Strategy

```
WHEN THINGS GO WRONG:
──────────────────────
Every deployment can be rolled back.
Because we tag images with git SHA,
every version is in ECR permanently
(last 30 images per service).

ROLLBACK OPTIONS (fastest to most thorough):
──────────────────────────────────────────────

Option 1 — Feature Flag (seconds):
If the issue is in a flagged feature:
  Flip the flag in Spring Cloud Config.
  No deployment needed.

Option 2 — ECS Task Rollback (2-3 minutes):
ECS keeps the previous task definition.
In AWS Console or CLI:
  aws ecs update-service \
    --cluster moss-production \
    --service expense-service-prod \
    --task-definition expense-service-prod:PREVIOUS_REVISION
ECS does a rolling rollback.

Option 3 — Redeploy Previous Image (5-10 minutes):
Find the previous git SHA from Slack deploy notification.
Trigger GitHub Actions with that SHA as the image tag.
Full pipeline runs, deploys old image.

WHAT ABOUT DATABASE MIGRATIONS?
──────────────────────────────────
This is the hardest part of rollback.

Rule: migrations must be backward compatible.
─────────────────────────────────────────────
We never write a migration that 
breaks the previous version of the code.

Example:
Sprint N:   Add optional column `gl_code` (nullable)
            Code writes to gl_code if present
            Migration: ALTER TABLE expenses 
                       ADD COLUMN gl_code VARCHAR(50)

If we rollback to Sprint N-1 code:
Old code doesn't know about gl_code.
But it doesn't break — 
nullable column, old code ignores it.
PostgreSQL is fine.

Counter-example (wrong approach):
Sprint N:   Rename column `description` to `notes`
            Migration: ALTER TABLE expenses 
                       RENAME COLUMN description TO notes

Rollback to Sprint N-1 code:
Old code tries to read `description`.
Column doesn't exist anymore.
Service crashes. Unrecoverable without 
manual DB intervention.

Our rule: expand then contract.
────────────────────────────────
Step 1: Add new column (backward compatible)
Step 2: Deploy code that writes to both columns
Step 3: Migrate data to new column
Step 4: Deploy code that reads only new column
Step 5: Drop old column (safe now)

5 steps instead of 1.
But each step is safely reversible.
```

---

## Part 9 — Pipeline Performance

```
PIPELINE TIMING (what we aim for):
────────────────────────────────────
PR checks:
  Unit tests:          ~3 minutes
  Integration tests:   ~6 minutes
  Static analysis:     ~4 minutes
  Total PR pipeline:   ~8 min (parallel)

Main pipeline:
  Full test suite:     ~8 minutes
  Docker build:        ~4 minutes
  Security scan:       ~2 minutes
  Push to ECR:         ~1 minute
  Deploy to staging:   ~3 minutes
  Smoke tests:         ~1 minute
  Total to staging:    ~15 minutes from merge

Total from merge to production:
  (after manual approval)
  ~20 minutes end to end

This means a developer can merge code 
at 10am and have it in production by 10:30am,
with a proper staging checkpoint in between.
That's fast enough for a Series B team
without being reckless.
```

**Where we optimized pipeline speed:**

```
1. Maven dependency caching
   ─────────────────────────
   actions/setup-java with cache: 'maven'
   Dependencies cached between runs.
   Saved ~2-3 minutes per pipeline run.

2. Parallel jobs
   ──────────────
   Unit tests, integration tests, 
   and static analysis run in parallel.
   Would be 13 minutes sequential.
   Takes 8 minutes parallel.

3. Run only affected service
   ───────────────────────────
   Monorepo with multiple services.
   mvn test -pl expense-service
   Only tests the changed service.
   Not all services on every PR.

4. Docker layer caching
   ──────────────────────
   COPY pom.xml → RUN mvn dependency:go-offline
   as a separate layer before copying source.
   If source changes but POM doesn't,
   Docker reuses the dependency layer.
   Saved ~3 minutes per Docker build.
```

---

## Part 10 — Security in the Pipeline

```
WHAT WE CHECK AUTOMATICALLY:
──────────────────────────────

1. SonarQube
   ──────────
   Static code analysis on every PR.
   Catches:
   - Common vulnerability patterns 
     (SQL injection, XXE, etc.)
   - Code smells and maintainability issues
   - Test coverage below threshold (80%)
   Quality gate blocks merge if it fails.

2. Trivy (container image scanning)
   ───────────────────────────────────
   Scans our Docker image for known CVEs
   (Common Vulnerabilities and Exposures)
   in base image and dependencies.
   Blocks deployment on CRITICAL severity.
   HIGH severity: warning, doesn't block
                  (we fix in next sprint).

3. Secrets scanning
   ──────────────────
   GitHub's built-in secret scanning.
   Detects if anyone accidentally commits
   API keys, passwords, tokens.
   PR blocked immediately.
   We've never had this trigger in production —
   but it's the safety net.

4. Dependency updates
   ────────────────────
   Dependabot configured on the repo.
   Creates PRs automatically when 
   dependencies have security updates.
   We review and merge these weekly.
```

---

## Summary — Quick Reference

```
WHAT              HOW
───────────────────────────────────────────────────────
Branch strategy   GitHub Flow — feature branches → main
PR requirements   2 approvals + CI green + SonarQube gate
CI tool           GitHub Actions (hosted, no maintenance)
Image registry    AWS ECR (tagged with git SHA)
Orchestration     AWS ECS with Fargate
Deployment        Rolling update (zero downtime)
Environments      dev (local) → staging (auto) → prod (manual)
Config            Spring Cloud Config + AWS Secrets Manager
Local dev         Docker Compose + WireMock stubs
Feature flags     Config-based (@ConfigurationProperties)
Rollback          Flag flip → ECS rollback → redeploy old image
Security scans    SonarQube + Trivy + GitHub secret scanning
Pipeline time     ~8 min to staging, ~20 min to production
```

---

Step 9 complete. This is saved as **CI/CD & DevOps.md** in **Module 10: CI/CD & DevOps**.

**Next is Step 10: Monitoring, Tracing & Logging** — covering Spring Boot Actuator, Micrometer metrics, Datadog dashboards, distributed tracing with OpenTelemetry, structured logging with Logback, and how we actually measure the improvements we claim on a resume. Ready?