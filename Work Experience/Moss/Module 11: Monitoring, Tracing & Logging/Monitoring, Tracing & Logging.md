Let me start writing Step 10 now.

---

# Step 10: Monitoring, Tracing & Logging

---

## The Big Picture — Why This Layer Exists

```
Without monitoring, tracing & logging:
────────────────────────────────────────
"The expense approval is slow."
→ Which endpoint? How slow? Since when? 
  For which companies? Under what load?
  No idea. SSH into server, grep logs, 
  pray.

"Something broke in production."
→ Which service? Which request? 
  What was the chain of events?
  No idea. Dig through logs manually
  for 2 hours.

"I improved this by 60%."
→ How do you know? Compared to what?
  When? Under what conditions?
  No answer. Interviewer stops trusting you.

With monitoring, tracing & logging:
─────────────────────────────────────
Every question above has a specific,
measurable, reproducible answer.
In Datadog. In under 30 seconds.
```

**Three distinct layers — each solving a different problem:**

```
LAYER 1: METRICS (Actuator + Micrometer → Datadog)
────────────────────────────────────────────────────
Answers: "What is happening right now?"
         "How does it compare to last week?"
Numbers over time. Aggregated.
JVM memory, HTTP latency, DB pool, 
cache hit rate, error rates.
Good for dashboards, alerts, trends.

LAYER 2: TRACING (OpenTelemetry + Jaeger)
───────────────────────────────────────────
Answers: "Where did THIS specific request go?
          Which service was slow? Which failed?"
End-to-end path of one request 
across multiple services.
Good for debugging distributed failures,
finding bottlenecks in the call chain.

LAYER 3: LOGGING (SLF4J + Logback → Datadog Logs)
───────────────────────────────────────────────────
Answers: "What exactly happened inside 
          a specific service for a specific request?"
Detailed narrative of events.
Good for root cause analysis, 
compliance audit trail.

These three layers are COMPLEMENTARY:
──────────────────────────────────────
Metrics alerts you something is wrong.
Tracing tells you where in the system.
Logs tell you exactly what happened there.
```

---

## Part 1 — Metrics Layer (Actuator + Micrometer + Datadog)

### Setup in Our Services

Both Expense Service and Invoice & AP Service have identical monitoring setup.

```xml
<!-- pom.xml dependencies -->

<!-- Spring Boot Actuator — exposes metrics endpoints -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Micrometer Datadog registry — pushes metrics to Datadog -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-datadog</artifactId>
</dependency>
```

```properties
# application.properties

# Actuator endpoint exposure
management.endpoints.web.base-path=/manage
management.endpoints.web.exposure.include=health,info,metrics,prometheus

# Health check detail
management.endpoint.health.show-details=always

# Datadog push config
management.datadog.metrics.export.api-key=${DATADOG_API_KEY}
management.datadog.metrics.export.enabled=true
management.datadog.metrics.export.step=30s

# Every metric gets these tags — 
# critical for filtering in Datadog dashboards
management.metrics.tags.service=expense-service
management.metrics.tags.env=${SPRING_PROFILES_ACTIVE}
management.metrics.tags.team=expense-ap
```

**Why the tags matter:**

```
Without tags:
─────────────
You see: http.server.requests p99 = 450ms
For which service? Which environment?
No idea.

With tags:
──────────
service=expense-service, env=prod
http.server.requests p99 = 450ms

Now you can filter:
"Show me only expense-service in prod"
"Compare expense-service vs invoice-service"
"How does staging compare to prod?"
Tags make metrics actionable.
```

---

### Health Checks — Custom Indicators

Both services have custom health indicators beyond the default Spring ones.

```java
// Expense Service — checks DB connection pool
@Component
public class DatabaseHealthIndicator 
        implements HealthIndicator {

    private final DataSource dataSource;

    @Override
    public Health health() {
        try (Connection conn = dataSource.getConnection()) {
            // Simple query to verify DB is responding
            conn.prepareStatement("SELECT 1").execute();

            // Also check pool stats via HikariCP
            HikariDataSource hikari = 
                (HikariDataSource) dataSource;
            int activeConnections = 
                hikari.getHikariPoolMXBean()
                      .getActiveConnections();
            int maxPoolSize = hikari.getMaximumPoolSize();

            return Health.up()
                .withDetail("activeConnections", 
                    activeConnections)
                .withDetail("maxPoolSize", maxPoolSize)
                .withDetail("utilizationPercent",
                    (activeConnections * 100) / maxPoolSize)
                .build();

        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

```java
// Expense Service — checks Kafka producer
@Component
public class KafkaHealthIndicator 
        implements HealthIndicator {

    private final KafkaTemplate<String, Object> 
        kafkaTemplate;

    @Override
    public Health health() {
        try {
            // Check if producer can reach brokers
            kafkaTemplate.getProducerFactory()
                .createProducer()
                .partitionsFor("expense.submitted");

            return Health.up()
                .withDetail("brokers", 
                    "reachable")
                .build();

        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

```java
// Invoice Service — checks outbox backlog
// If outbox has too many unpublished events,
// something is wrong with the poller
@Component
@RequiredArgsConstructor
public class OutboxHealthIndicator 
        implements HealthIndicator {

    private final OutboxEventRepository outboxRepository;
    private static final int BACKLOG_THRESHOLD = 500;

    @Override
    public Health health() {

        long unpublishedCount = outboxRepository
            .countByPublishedFalse();

        if (unpublishedCount > BACKLOG_THRESHOLD) {
            return Health.down()
                .withDetail("unpublishedEvents", 
                    unpublishedCount)
                .withDetail("threshold", 
                    BACKLOG_THRESHOLD)
                .withDetail("message",
                    "Outbox backlog too high — " +
                    "poller may be stuck")
                .build();
        }

        return Health.up()
            .withDetail("unpublishedEvents", 
                unpublishedCount)
            .build();
    }
}
```

```
/manage/health response:
─────────────────────────
{
  "status": "UP",
  "components": {
    "database": {
      "status": "UP",
      "details": {
        "activeConnections": 3,
        "maxPoolSize": 10,
        "utilizationPercent": 30
      }
    },
    "kafka": {
      "status": "UP",
      "details": { "brokers": "reachable" }
    },
    "outbox": {
      "status": "UP",
      "details": { "unpublishedEvents": 2 }
    },
    "redis": {
      "status": "UP"
    },
    "diskSpace": {
      "status": "UP"
    }
  }
}

If ANY component is DOWN → 
overall status = DOWN →
AWS ECS health check fails →
ECS stops sending traffic to that task →
Triggers alert in Datadog.
```

---

### Custom Business Metrics — The Most Important Part

Out-of-the-box Micrometer metrics (JVM, HTTP, DB pool) are useful but generic. The metrics that actually tell you about **business health** are custom ones. These are also what back up resume claims.

```java
// Custom metrics registry for Expense Service
@Component
@RequiredArgsConstructor
public class ExpenseMetrics {

    private final MeterRegistry meterRegistry;

    // Counter — total expense submissions
    // Use: track volume trends, 
    //      detect sudden drops (ingestion broken?)
    public void recordExpenseSubmitted(
            String currency, String category) {

        Counter.builder("expense.submitted.total")
            .tag("currency", currency)
            .tag("category", category)
            .description("Total expenses submitted")
            .register(meterRegistry)
            .increment();
    }

    // Counter — expenses approved vs rejected
    // Use: approval rate trends per company
    public void recordExpenseDecision(
            String decision, String companyId) {

        Counter.builder("expense.decision.total")
            .tag("decision", decision)   // APPROVED/REJECTED
            .tag("companyId", companyId)
            .register(meterRegistry)
            .increment();
    }

    // Timer — how long approval takes 
    // (from submission to approval/rejection)
    // Use: SLA monitoring, bottleneck detection
    public void recordApprovalDuration(
            Duration duration, String decision) {

        Timer.builder("expense.approval.duration")
            .tag("decision", decision)
            .description(
                "Time from submission to approval decision")
            .register(meterRegistry)
            .record(duration);
    }

    // Gauge — current pending approval count
    // Use: workflow health, 
    //      detect if approvals are stuck
    public void registerPendingGauge(
            Supplier<Number> pendingCountSupplier) {

        Gauge.builder("expense.pending.count",
                pendingCountSupplier)
            .description(
                "Current number of pending expenses")
            .register(meterRegistry);
    }

    // Timer — OCR processing duration
    // Use: detect OCR service degradation
    public void recordOcrDuration(
            Duration duration, boolean success) {

        Timer.builder("expense.ocr.duration")
            .tag("success", String.valueOf(success))
            .register(meterRegistry)
            .record(duration);
    }
}
```

```java
// Custom metrics for Invoice & AP Service
@Component
@RequiredArgsConstructor
public class InvoiceMetrics {

    private final MeterRegistry meterRegistry;

    // Timer — full invoice lifecycle duration
    // (upload to paid)
    // Use: end-to-end process efficiency tracking
    public void recordInvoiceLifecycleDuration(
            Duration duration, String supplierCountry) {

        Timer.builder("invoice.lifecycle.duration")
            .tag("supplierCountry", supplierCountry)
            .description(
                "Time from invoice upload to payment")
            .register(meterRegistry)
            .record(duration);
    }

    // Counter — payment run totals
    // Use: financial volume tracking
    public void recordPaymentRunCompleted(
            int invoiceCount, 
            BigDecimal totalAmount,
            String currency) {

        Counter.builder("payment.run.completed.total")
            .tag("currency", currency)
            .register(meterRegistry)
            .increment();

        // Also track total value processed
        DistributionSummary.builder(
                "payment.run.amount")
            .tag("currency", currency)
            .register(meterRegistry)
            .record(totalAmount.doubleValue());
    }

    // Counter — outbox publish success vs failure
    // Use: detect Kafka connectivity issues
    public void recordOutboxPublish(boolean success) {

        Counter.builder("outbox.publish.total")
            .tag("success", String.valueOf(success))
            .register(meterRegistry)
            .increment();
    }
}
```

**How these metrics are used in service layer:**

```java
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseMetrics metrics;
    private final ExpenseRepository expenseRepository;

    @Transactional
    public ExpenseResponse approveExpense(
            UUID expenseId, 
            UUID approverId,
            String comment) {

        Expense expense = expenseRepository
            .findById(expenseId).orElseThrow();

        Instant submittedAt = expense.getSubmittedAt();

        expense.setStatus(ExpenseStatus.APPROVED);
        expense.setApprovedAt(Instant.now());
        expense.setApprovedById(approverId);
        expenseRepository.save(expense);

        // Record approval decision metric
        metrics.recordExpenseDecision(
            "APPROVED", 
            expense.getCompanyId().toString()
        );

        // Record how long approval took
        metrics.recordApprovalDuration(
            Duration.between(submittedAt, Instant.now()),
            "APPROVED"
        );

        return ExpenseResponse.from(expense);
    }
}
```

---

### Key Metrics We Monitor in Datadog

```
HTTP LAYER:
───────────
http.server.requests (auto from Actuator)
  Tags: uri, method, status, outcome
  Queries we use:
    P50/P95/P99 latency per endpoint
    Error rate (5xx) per endpoint
    Requests per second

  Alert: P99 > 2000ms for 5 min → PagerDuty

JVM:
─────
jvm.memory.used / jvm.memory.max
  Alert: heap > 85% for 10 min → warning

jvm.gc.pause (COUNT, TOTAL_TIME)
  Alert: GC pause total > 500ms/min → warning

jvm.threads.live / jvm.threads.peak
  Alert: threads > 200 → warning
  (thread leak indicator)

DATABASE:
──────────
jdbc.connections.active / jdbc.connections.max
  Alert: active > 80% of max for 5 min → critical
  (pool exhaustion — requests start queuing)

  This is one of the most common 
  production issues at Series B scale.
  Pool of 10, you get a spike of traffic,
  9/10 connections active → 
  next request waits → latency spikes.

KAFKA:
───────
kafka.consumer.fetch-latency-avg
kafka.consumer.records-lag (consumer lag)
  Alert: lag > 5000 for consumer group 
         for > 10 min → warning

outbox.publish.total (our custom metric)
  success=false rate suddenly increases →
  Kafka broker connectivity issue

CACHE:
───────
cache.gets (Caffeine — auto from Actuator)
  Tags: name, result (hit/miss)
  Hit rate = hits / (hits + misses) × 100
  Alert: hit rate < 70% for approval_policy →
         investigate TTL or invalidation issue

redis.commands.latency (if using Redis metrics)
  P99 > 10ms → Redis performance degradation

BUSINESS METRICS (our custom):
────────────────────────────────
expense.submitted.total
  Sudden drop → submission flow broken?
  Sudden spike → legitimate surge or abuse?

expense.pending.count
  Growing continuously without decreasing →
  approvers not acting → SLA breach risk

expense.approval.duration (P50/P95)
  P95 growing → approval workflow bottleneck

outbox.publish.total success=false
  Kafka publish failures → 
  payments not triggering

invoice.lifecycle.duration
  P95 growing → AP process getting slower
```

---

## Part 2 — Tracing Layer (OpenTelemetry + Jaeger)

### Setup in Both Services

```xml
<!-- pom.xml -->

<!-- Micrometer — tracing API interface -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- OpenTelemetry SDK — implementation -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>

<!-- OTLP exporter — sends spans to Jaeger -->
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

```properties
# application.properties

# Sample every request in staging/dev
# In prod: 0.1 (10%) to reduce overhead
management.tracing.sampling.probability=1.0

# Jaeger OTLP endpoint
management.otlp.tracing.endpoint=\
  http://jaeger:4318/v1/traces

# Auto-propagate trace context in 
# outgoing HTTP calls (RestClient)
# This is automatic when using RestClient.Builder
```

### How Trace Propagation Works Across Our Services

```
Request flow with tracing:
───────────────────────────

1. Client hits API Gateway
   Gateway creates: TraceId=abc123, SpanId=span1
   
2. Gateway calls Expense Service
   Adds header: traceparent: abc123-span1
   
3. Expense Service receives request
   ServerHttpObservationFilter reads traceparent
   Creates: SpanId=span2, ParentSpanId=span1
   Same TraceId: abc123
   
4. Expense Service calls User & Org Service
   (via FeignClient — auto-propagated)
   Adds header: traceparent: abc123-span2
   
5. User & Org Service
   Creates: SpanId=span3, ParentSpanId=span2
   Same TraceId: abc123

Result in Jaeger:
─────────────────
TraceId: abc123
├── Gateway (span1): 12ms
│   └── Expense Service (span2): 10ms
│       ├── DB query - findById (span4): 3ms
│       ├── User & Org FeignClient (span3): 4ms
│       └── outbox insert (span5): 1ms

You can see EXACTLY where time was spent.
```

### Critical Rule — RestClient.Builder, Not RestClient.create()

```java
// ✅ CORRECT — trace propagation works
@Bean
public RestClient userOrgClient(
        RestClient.Builder builder) {

    return builder
        .baseUrl("http://user-org-service")
        .build();
    // Builder auto-adds tracing interceptor
    // All outgoing calls carry traceparent header
}

// ❌ WRONG — trace propagation BROKEN
@Bean  
public RestClient userOrgClient() {

    return RestClient.create("http://user-org-service");
    // No interceptor → no traceparent header →
    // User & Org Service creates new TraceId →
    // Broken chain — can't follow request 
    // across services in Jaeger
}
```

**This is a real mistake I made at month 8. FeignClient works automatically — but when I added a direct RestClient for a one-off call to an external OCR service, I used `RestClient.create()` and traces were disconnected. Arjun spotted it during a debugging session.**

### Manual Spans for Specific Operations

```java
// Adding custom spans for operations
// not auto-instrumented by Spring
@Service
@RequiredArgsConstructor
public class ReceiptOcrService {

    private final Tracer tracer;
    private final OcrApiClient ocrClient;

    public OcrResult processReceipt(
            String s3Key, UUID expenseId) {

        // Step 1: Get current span 
        //         (the HTTP request span)
        Span parentSpan = tracer.currentSpan();

        // Step 2: Create child span for OCR call
        Span ocrSpan = tracer
            .nextSpan(parentSpan)
            .name("ocr.process-receipt");

        ocrSpan.start();

        // Step 3: Make this the current span
        try (Tracer.SpanInScope scope = 
                tracer.withSpan(ocrSpan)) {

            ocrSpan.tag("expenseId", 
                expenseId.toString());
            ocrSpan.tag("s3Key", s3Key);

            // Step 4: Do the work
            OcrResult result = ocrClient
                .processImage(s3Key);

            ocrSpan.tag("confidence", 
                String.valueOf(result.getConfidence()));
            ocrSpan.tag("success", "true");

            return result;

        } catch (Exception e) {
            ocrSpan.tag("success", "false");
            ocrSpan.tag("error", e.getMessage());
            throw e;

        } finally {
            // Step 5: Always close scope 
            //         and end span
            ocrSpan.end();
        }
    }
}
```

```
In Jaeger, this shows as:
──────────────────────────
POST /api/v1/expenses (110ms)
├── expense-service.createExpense (105ms)
│   ├── DB: save expense draft (8ms)
│   ├── S3: upload receipt (45ms)    ← auto-instrumented
│   └── ocr.process-receipt (48ms)  ← our custom span
│       tags: expenseId=..., 
│             s3Key=..., 
│             confidence=0.94

Now you know: OCR is taking 48ms of the 110ms.
That's where to optimize if needed.
```

---

## Part 3 — Logging Layer (SLF4J + Logback + Datadog)

### Logback Configuration

```xml
<!-- src/main/resources/logback-spring.xml -->

<configuration>

  <!-- JSON encoder for structured logging -->
  <!-- All logs go to Datadog as structured JSON -->
  <!-- Datadog agent running as sidecar 
       on ECS task reads from STDOUT -->

  <appender name="JSON_CONSOLE" 
    class="ch.qos.logback.core.ConsoleAppender">
    <encoder 
      class="net.logstash.logback.encoder.LogstashEncoder">

      <!-- Mask sensitive fields -->
      <jsonGeneratorDecorator
        class="net.logstash.logback.mask
               .MaskingJsonGeneratorDecorator">
        <defaultMask>*****</defaultMask>
        <path>iban</path>
        <path>bankAccountIban</path>
        <path>password</path>
        <path>token</path>
      </jsonGeneratorDecorator>

    </encoder>
  </appender>

  <!-- Async wrapper — non-critical logs -->
  <appender name="ASYNC_JSON" 
    class="ch.qos.logback.classic.AsyncAppender">
    <queueSize>1000</queueSize>
    <discardingThreshold>10</discardingThreshold>
    <neverBlock>false</neverBlock>
    <filter 
      class="ch.qos.logback.classic.filter.LevelFilter">
      <level>ERROR</level>
      <onMatch>DENY</onMatch>
      <onMismatch>NEUTRAL</onMismatch>
    </filter>
    <appender-ref ref="JSON_CONSOLE"/>
  </appender>

  <!-- Sync for ERROR — never lose critical logs -->
  <appender name="SYNC_ERROR_CONSOLE" 
    class="ch.qos.logback.core.ConsoleAppender">
    <filter 
      class="ch.qos.logback.classic.filter
             .ThresholdFilter">
      <level>ERROR</level>
    </filter>
    <encoder 
      class="net.logstash.logback.encoder
             .LogstashEncoder"/>
  </appender>

  <!-- Our service packages -->
  <logger name="com.moss.expense" 
          level="INFO" 
          additivity="false">
    <appender-ref ref="ASYNC_JSON"/>
    <appender-ref ref="SYNC_ERROR_CONSOLE"/>
  </logger>

  <!-- Root — catches everything else -->
  <root level="WARN">
    <appender-ref ref="ASYNC_JSON"/>
    <appender-ref ref="SYNC_ERROR_CONSOLE"/>
  </root>

</configuration>
```

### Correlation ID Filter — Connecting Logs to Traces

```java
@Component
@RequiredArgsConstructor
public class CorrelationIdFilter 
        extends OncePerRequestFilter {

    private final Tracer tracer;

    public static final String CORRELATION_ID = 
        "correlationId";
    public static final String CORRELATION_HEADER = 
        "X-CorrelationId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws ServletException, IOException {

        String correlationId;

        // Use Micrometer's TraceId as CorrelationId
        // Same ID appears in Jaeger traces AND Datadog logs
        // One ID to search both tools — critical
        if (tracer.currentSpan() != null) {
            correlationId = tracer.currentSpan()
                .context().traceId();
        } else {
            // Fallback if tracing disabled
            correlationId = request.getHeader(
                CORRELATION_HEADER);
            if (correlationId == null 
                    || correlationId.isBlank()) {
                correlationId = UUID.randomUUID()
                    .toString();
            }
        }

        try {
            MDC.put(CORRELATION_ID, correlationId);

            // Return ID in response header
            // Client includes this when reporting issues
            response.setHeader(
                CORRELATION_HEADER, correlationId);

            chain.doFilter(request, response);

        } finally {
            MDC.clear(); // prevent thread pollution
        }
    }
}
```

### MDC — Propagating Context Through Logs

```java
// In ExpenseService — adding business context to MDC
@Service
@RequiredArgsConstructor
public class ExpenseService {

    @Transactional
    public ExpenseResponse approveExpense(
            UUID expenseId,
            UUID approverId,
            String comment) {

        // Add business context to MDC
        // Every log.info() below automatically 
        // includes these fields in JSON output
        MDC.put("expenseId", expenseId.toString());
        MDC.put("approverId", approverId.toString());

        try {
            Expense expense = expenseRepository
                .findById(expenseId).orElseThrow();

            MDC.put("companyId", 
                expense.getCompanyId().toString());
            MDC.put("amount", 
                expense.getAmount().toString());

            log.info("Starting expense approval");
            // Logs: {..., "expenseId": "...", 
            //        "approverId": "...", 
            //        "companyId": "...",
            //        "correlationId": "abc123",
            //        "traceId": "abc123"}

            expense.setStatus(ExpenseStatus.APPROVED);
            expenseRepository.save(expense);

            log.info("Expense approved successfully");
            // Same MDC fields in every log line
            // Zero extra parameters needed

            return ExpenseResponse.from(expense);

        } catch (Exception e) {
            log.error("Expense approval failed", e);
            // Stack trace + all MDC fields
            throw e;

        } finally {
            // Clear service-specific MDC 
            // (correlation ID cleared by filter)
            MDC.remove("expenseId");
            MDC.remove("approverId");
            MDC.remove("companyId");
            MDC.remove("amount");
        }
    }
}
```

**What a log line looks like in Datadog:**

```json
{
  "@timestamp": "2025-03-15T10:30:45.123Z",
  "level": "INFO",
  "message": "Expense approved successfully",
  "logger_name": "com.moss.expense.service.ExpenseService",
  "thread_name": "http-nio-8080-exec-3",
  "service": "expense-service",
  "env": "prod",
  "correlationId": "9e7d21299f4ea8a1",
  "traceId": "9e7d21299f4ea8a1",
  "spanId": "2c3655fc800de28b",
  "expenseId": "uuid-123",
  "approverId": "uuid-456",
  "companyId": "uuid-789",
  "amount": "85.00"
}
```

```
Power of this structure:
─────────────────────────
Search in Datadog Logs:
  correlationId:9e7d21299f4ea8a1
  → All logs for this ONE request 
    across ALL services

  expenseId:uuid-123
  → Complete history of this expense
    across all state changes

  level:ERROR AND service:expense-service
  → All errors in expense service today

  companyId:uuid-789 AND level:ERROR
  → All errors affecting one company

  amount:>50000
  → All high-value expense operations
    (compliance monitoring)
```

### MDC With @Async — Task Decorator

```java
// Without this, @Async threads have empty MDC
// CorrelationId disappears from async logs
@Component
public class MdcTaskDecorator 
        implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // Capture MDC from PARENT thread
        // decorate() runs on parent thread
        Map<String, String> contextMap = 
            MDC.getCopyOfContextMap();

        return () -> {
            // Restore MDC in CHILD thread
            if (contextMap != null) {
                MDC.setContextMap(contextMap);
            }
            try {
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}

// Registration in AsyncConfig
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class AsyncConfig implements AsyncConfigurer {

    private final MdcTaskDecorator mdcTaskDecorator;

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = 
            new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.setTaskDecorator(mdcTaskDecorator);
        executor.initialize();
        return executor;
    }
}
```

---

## Part 4 — Datadog Dashboards We Built

```
We built three dashboards in Datadog.
These are what interviewers mean when 
they ask "how did you measure that?"

DASHBOARD 1: Service Health (always open)
──────────────────────────────────────────
Panels:
  - HTTP request rate (req/sec)
  - HTTP error rate (5xx %)
  - P50 / P95 / P99 latency per endpoint
  - JVM heap usage %
  - DB connection pool utilization %
  - Active Kafka consumer lag
  - Redis cache hit rate

Used for: real-time health monitoring,
          detecting incidents within minutes

DASHBOARD 2: Business Metrics
───────────────────────────────
Panels:
  - Expense submission rate per hour
  - Approval rate (approved / total decided)
  - Pending expense count over time
  - Approval duration P50/P95 
    (SLA monitoring)
  - Invoice upload rate
  - Payment run total value per week
  - Outbox publish success rate

Used for: product health, SLA compliance,
          business trend visibility

DASHBOARD 3: Performance Deep-Dive
────────────────────────────────────
Panels:
  - Per-endpoint breakdown of P50/P99
  - DB query duration (from Micrometer)
  - Cache hit rate breakdown by cache name
  - Kafka consumer throughput
  - OCR service latency (custom span)
  - JVM GC pause duration

Used for: before/after comparison 
          when optimizing, 
          capacity planning
```

---

## Part 5 — Alerting

```
CRITICAL ALERTS (PagerDuty, wakes someone up):
────────────────────────────────────────────────
- Service health check = DOWN for > 2 min
- HTTP 5xx error rate > 5% for > 5 min
- DB connection pool > 90% for > 3 min
- Kafka consumer lag > 10,000 for > 10 min
- JVM heap > 90% for > 5 min
- Outbox poller failure rate > 0 
  (any outbox failure is critical)

WARNING ALERTS (Slack #expense-ap-alerts):
────────────────────────────────────────────
- P99 latency > 1000ms for > 5 min
- Cache hit rate < 60% for > 10 min
- DB pool > 75% for > 5 min
- Kafka consumer lag > 1,000 for > 5 min
- Pending expense count not decreasing 
  for > 2 hours (approvers inactive?)
- Outbox backlog > 500 events

NON-PROD ALERTS (email only):
───────────────────────────────
- Staging deployment failed
- Integration test failures
- SonarQube quality gate failed
```

---

## Part 6 — The Measurement Reference Table

**This is the table that backs every resume claim.**

When an interviewer asks *"how did you measure that?"* — your answer comes from this table.

```
CLAIM TYPE                    TOOL + METRIC + HOW TO MEASURE
───────────────────────────────────────────────────────────────────────

"Reduced API latency by X%"
─────────────────────────────
Tool:    Datadog (Metrics)
Metric:  http.server.requests
         tag: uri=/api/v1/expenses/{expenseId}/approve
         aggregation: p99
How:     Compare p99 latency in Datadog 
         between two time windows:
         BEFORE: week before the fix
         AFTER: week after the fix
         Use Datadog's "Compare" feature
         or export CSV and calculate % diff

"Fixed N+1 query — reduced endpoint 
 response time from X ms to Y ms"
───────────────────────────────────────
Tool:    Datadog (Metrics + APM)
Metrics: http.server.requests (p99 for that endpoint)
         db.client.connections.usage (query count)
How:     1. Datadog APM traces show number of 
            DB spans per request 
            (before fix: 51 spans, after fix: 1 span)
         2. p99 latency dropped from 800ms to 45ms
            (measured via Datadog Metrics, same metric above)
         3. Jaeger trace shows: 
            before = 50 child DB spans,
            after = 1 DB span (JOIN FETCH)

"Improved cache hit rate from X% to Y%"
─────────────────────────────────────────
Tool:    Datadog (Metrics)
Metric:  cache.gets tag: result=hit / result=miss
         (auto-exposed by Spring Boot Actuator 
          via Caffeine's recordStats())
How:     Hit rate = hits / (hits + misses) × 100
         Measured before adding cache:
           hit rate = 0% (no cache existed)
         Measured after adding cache:
           hit rate = 87% (steady state)
         Dashboard panel shows this over time

"Reduced DB connection pool exhaustion"
────────────────────────────────────────
Tool:    Datadog (Metrics)
Metric:  jdbc.connections.active
         jdbc.connections.max
How:     Pool utilization = active/max × 100
         Before fix: spikes to 95-100% 
                     during load → requests queue
         After fix (connection leak resolved 
                    + pool size tuned): 
                     stays < 60% during same load
         Measured via Datadog dashboard panel

"Reduced Kafka consumer lag"
─────────────────────────────
Tool:    Datadog (Metrics)
Metric:  kafka.consumer.records-lag
         tag: group=expense-service
How:     Lag before fix: growing to 5,000+ 
                          during peak hours
         Lag after fix: stays < 100 at all times
         Measured by comparing lag trend 
         lines in Datadog before and after deploy

"Eliminated production incident — 
 outbox events stuck"
────────────────────────────────
Tool:    Datadog (Metrics + Logs)
Metric:  outbox.publish.total tag: success=false
         Custom metric we built
How:     Alert fired when success=false rate > 0
         Root cause: found in Datadog Logs by 
         searching level:ERROR AND 
         logger:OutboxPoller
         After fix: success=false rate = 0
         Measured by comparing alert frequency
         (before fix: 2-3 times/week, after: 0)

"Improved approval workflow speed"
───────────────────────────────────
Tool:    Datadog (Metrics)
Metric:  expense.approval.duration 
         (our custom Timer metric)
How:     P95 approval duration before improvement:
           (e.g., approver not notified promptly 
            due to Kafka consumer lag) = 4+ hours
         P95 after fixing consumer lag:
           < 30 minutes
         This is a BUSINESS metric backed by 
         our custom Micrometer timer

"Service uptime / reliability improvement"
───────────────────────────────────────────
Tool:    Datadog (SLO monitoring)
Metric:  http.server.requests outcome=SERVER_ERROR
         (5xx rate)
How:     SLO defined: 5xx rate < 1% over 30 days
         Before stability work: 
           2-3 incidents/month, 99.2% uptime
         After health checks + proper error 
         handling + DLQ setup:
           0 incidents, 99.9% uptime
         Measured via Datadog SLO dashboard

"Reduced memory usage / GC pressure"
─────────────────────────────────────
Tool:    Datadog (Metrics)
Metrics: jvm.memory.used
         jvm.gc.pause (TOTAL_TIME)
How:     Before fix (e.g., large object 
         kept in memory unnecessarily):
           Heap usage 75-80%, 
           GC pause total 400ms/min
         After fix:
           Heap usage 45-55%,
           GC pause total 80ms/min
         Measured via Datadog dashboard
         over same traffic volume

"Identified and resolved a hot query"
───────────────────────────────────────
Tool:    Datadog APM + Jaeger
How:     Datadog APM "Top Database Queries" view
         shows queries ranked by total time consumed
         Found: a query running 200ms, 
                called 50 times/minute
                = 10 seconds of DB time per minute
         After adding index:
                same query: 5ms
         Measured: Datadog APM before/after,
                   Jaeger trace shows DB span 
                   duration dropped
```

---

## Summary — How The Three Layers Work Together

```
SCENARIO: Finance manager reports 
"invoice approval is slow today"

STEP 1 — Metrics (Datadog)
─────────────────────────────
Open Dashboard 1 (Service Health).
Check: http.server.requests p99 
       for PUT /api/v1/invoices/{id}/approve
Find: p99 = 3,200ms (normally 180ms)
      DB connection pool: 88% utilized
Conclusion: DB pool pressure causing slowness.
Time to find this: 2 minutes.

STEP 2 — Tracing (Jaeger)
──────────────────────────
Search traces for this endpoint 
during the slow period.
Find a representative slow trace.
Open trace: 
  Invoice Service (3,180ms)
  ├── DB: findAndLockForPaymentRun (2,950ms) ← problem
  └── DB: save invoice (180ms)

Conclusion: pessimistic lock is waiting.
Someone else holds the lock — concurrent payment run?

STEP 3 — Logging (Datadog Logs)
─────────────────────────────────
Search: service:invoice-service 
        AND level:WARN 
        AND message:*lock*
        (time window: during the incident)

Find log entry:
{
  "message": "Payment run creation taking > 2s",
  "companyId": "uuid-789",
  "invoiceIds": ["inv-1", "inv-2"],
  "lockWaitMs": 2900
}

Root cause: company uuid-789 had two finance 
managers simultaneously creating payment runs.
Pessimistic lock contention.

Fix: Return clear 409 error faster, 
     add UI-level guard to prevent 
     simultaneous payment run creation.

TOTAL TIME TO ROOT CAUSE: ~15 minutes.
Without this stack: hours of guessing.
```

---

Step 10 complete. This is saved as **Monitoring, Tracing & Logging.md** in **Module 11: Monitoring, Tracing & Logging**.

Now we have everything — all 10 preparatory steps done. Every system component is understood. Every measurement tool is set up. Every resume claim has a measurement methodology.

**We are ready for the final step — Step 11: Your Role, Responsibilities, Contributions & Ownership across 18 months in 6 × 3-month blocks using STAR methodology.**
