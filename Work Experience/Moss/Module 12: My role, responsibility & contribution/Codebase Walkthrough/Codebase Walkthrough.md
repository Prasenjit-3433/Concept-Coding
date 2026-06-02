Looking at all the work done across the modules, your team owns two services:

1. **Expense & Reimbursement Service**
2. **Invoice & AP Service**

Let me build out the complete file and folder structure for both, with explanations of what each folder/file does and why it exists that way.

---

## Expense & Reimbursement Service — Full Structure

```
expense-service/
│
├── pom.xml                          ← Maven build file. Lists all dependencies
│                                       (Spring Boot, JPA, Kafka, Redis, etc.)
│                                       and defines build configuration.
│
├── Dockerfile                       ← Instructions to package this service
│                                       into a Docker image for deployment.
│                                       (Multi-stage build: builder → runtime)
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── moss/
│   │   │           └── expense/
│   │   │               │
│   │   │               ├── ExpenseServiceApplication.java
│   │   │               │       ← Entry point. Contains main() method.
│   │   │               │         @SpringBootApplication annotation here.
│   │   │               │
│   │   │               ├── controller/
│   │   │               │   │   ← HTTP layer. Receives requests, calls service,
│   │   │               │   │     returns responses. NO business logic here.
│   │   │               │   │
│   │   │               │   ├── ExpenseController.java
│   │   │               │   │       ← Handles: POST /api/v1/expenses
│   │   │               │   │                  GET  /api/v1/expenses
│   │   │               │   │                  GET  /api/v1/expenses/{id}
│   │   │               │   │                  PUT  /api/v1/expenses/{id}/submit
│   │   │               │   │                  PUT  /api/v1/expenses/{id}/approve
│   │   │               │   │                  PUT  /api/v1/expenses/{id}/reject
│   │   │               │   │                  PUT  /api/v1/expenses/bulk-approve
│   │   │               │   │
│   │   │               │   ├── ReimbursementController.java
│   │   │               │   │       ← Handles: GET /api/v1/reimbursements
│   │   │               │   │                  GET /api/v1/reimbursements/{id}
│   │   │               │   │
│   │   │               │   └── advice/
│   │   │               │       └── GlobalExceptionHandler.java
│   │   │               │               ← @RestControllerAdvice
│   │   │               │                 Catches all exceptions across ALL controllers.
│   │   │               │                 Maps them to HTTP responses:
│   │   │               │                   ExpenseNotFoundException → 404
│   │   │               │                   InvalidExpenseStateException → 409
│   │   │               │                   UnauthorizedActionException → 403
│   │   │               │                   MethodArgumentNotValidException → 400
│   │   │               │                   LockTimeoutException → 409
│   │   │               │
│   │   │               ├── service/
│   │   │               │   │   ← Business logic lives here. Annotated @Transactional.
│   │   │               │   │     Calls repositories, FeignClients, and event publishers.
│   │   │               │   │
│   │   │               │   ├── ExpenseService.java
│   │   │               │   │       ← Core business logic:
│   │   │               │   │         createExpense(), submitExpense(),
│   │   │               │   │         approveExpense(), rejectExpense(),
│   │   │               │   │         getExpense(), getExpenses()
│   │   │               │   │
│   │   │               │   ├── BulkApprovalService.java
│   │   │               │   │       ← Handles bulk approve requests.
│   │   │               │   │         Calls SingleApprovalService in a loop.
│   │   │               │   │         (Story 7 — bulk approval feature)
│   │   │               │   │
│   │   │               │   ├── SingleApprovalService.java
│   │   │               │   │       ← @Transactional(REQUIRES_NEW)
│   │   │               │   │         Separate bean so REQUIRES_NEW works correctly.
│   │   │               │   │         Called by BulkApprovalService for each expense.
│   │   │               │   │         (Story 6 proxy lesson applied here)
│   │   │               │   │
│   │   │               │   ├── ApprovalRoutingService.java
│   │   │               │   │       ← Determines approval steps for an expense.
│   │   │               │   │         Reads approval policy from User & Org Service.
│   │   │               │   │         Creates step definitions based on amount + policy.
│   │   │               │   │         (Story 5 — multi-level approval)
│   │   │               │   │
│   │   │               │   ├── ApprovalPolicyService.java
│   │   │               │   │       ← Fetches approval policy from User & Org Service.
│   │   │               │   │         Has Redis caching with mutex lock.
│   │   │               │   │         (Story 12 — caching proposal)
│   │   │               │   │         (Story 13 — stampede fix added here)
│   │   │               │   │
│   │   │               │   ├── ReimbursementService.java
│   │   │               │   │       ← Handles reimbursement lifecycle.
│   │   │               │   │         processPaymentCompleted() called by Kafka consumer.
│   │   │               │   │         (Story 8 — Kafka consumer)
│   │   │               │   │
│   │   │               │   ├── ReceiptStorageService.java
│   │   │               │   │       ← Uploads receipts to AWS S3.
│   │   │               │   │         Returns S3 URL stored in expense record.
│   │   │               │   │
│   │   │               │   ├── ReceiptOcrService.java
│   │   │               │   │       ← Calls external OCR API (async).
│   │   │               │   │         Has manual tracing span for monitoring.
│   │   │               │   │         (Module 11 — custom spans)
│   │   │               │   │
│   │   │               │   └── ExpenseAuditService.java
│   │   │               │           ← @Transactional(REQUIRES_NEW) on each public method.
│   │   │               │             Writes audit log entries independently.
│   │   │               │             Survives outer transaction rollback.
│   │   │               │             (Story 6 — @Transactional private method lesson)
│   │   │               │
│   │   │               ├── repository/
│   │   │               │   │   ← JPA repositories. Spring Data generates SQL from method names.
│   │   │               │   │     Custom @Query annotations for complex queries.
│   │   │               │   │
│   │   │               │   ├── ExpenseRepository.java
│   │   │               │   │       ← findByCompanyIdAndStatus() — paginated list
│   │   │               │   │         findWithEmployeesByCompanyIdAndStatus() — JOIN FETCH
│   │   │               │   │           (Story 4 — fixed N+1 with JOIN FETCH)
│   │   │               │   │         existsDuplicateSubmission() — duplicate check
│   │   │               │   │           (Story 9 incident — missing index caused lag spike)
│   │   │               │   │
│   │   │               │   ├── ApprovalStepRepository.java
│   │   │               │   │       ← findCurrentActiveStep() — finds step with action=PENDING
│   │   │               │   │                                     AND activatedAt IS NOT NULL
│   │   │               │   │         findNextPendingStep() — finds next step to activate
│   │   │               │   │         existsByExpenseIdAndApproverIdAndActionNot() — idempotency
│   │   │               │   │
│   │   │               │   ├── ReimbursementRepository.java
│   │   │               │   │       ← Standard CRUD for reimbursements
│   │   │               │   │
│   │   │               │   ├── OutboxEventRepository.java
│   │   │               │   │       ← findTop100ByPublishedFalseOrderByCreatedAtAsc()
│   │   │               │   │         countByPublishedFalse() — used by health indicator
│   │   │               │   │
│   │   │               │   └── ExpenseAuditLogRepository.java
│   │   │               │           ← findByExpenseId() — for viewing audit trail
│   │   │               │
│   │   │               ├── entity/
│   │   │               │   │   ← JPA entities. Maps Java objects to DB tables.
│   │   │               │   │     Each class = one DB table.
│   │   │               │   │
│   │   │               │   ├── Expense.java
│   │   │               │   │       ← Maps to `expenses` table.
│   │   │               │   │         Has @Version for optimistic locking.
│   │   │               │   │         Has @ManyToOne for employee and assignedApprover.
│   │   │               │   │
│   │   │               │   ├── ApprovalStep.java
│   │   │               │   │       ← Maps to `approval_steps` table.
│   │   │               │   │         stepOrder, approverId, action, activatedAt etc.
│   │   │               │   │
│   │   │               │   ├── Reimbursement.java
│   │   │               │   │       ← Maps to `reimbursements` table.
│   │   │               │   │
│   │   │               │   ├── Employee.java
│   │   │               │   │       ← Maps to `employees` table.
│   │   │               │   │         Used via @ManyToOne from Expense for JOIN FETCH.
│   │   │               │   │
│   │   │               │   ├── OutboxEvent.java
│   │   │               │   │       ← Maps to `outbox_events` table.
│   │   │               │   │         aggregateType, aggregateId, eventType, payload,
│   │   │               │   │         published, publishedAt.
│   │   │               │   │
│   │   │               │   └── ExpenseAuditLog.java
│   │   │               │           ← Maps to `expense_audit_logs` table.
│   │   │               │
│   │   │               ├── dto/
│   │   │               │   │   ← Data Transfer Objects. What the API actually receives
│   │   │               │   │     (request) and returns (response). NOT the same as entities.
│   │   │               │   │     Entities = DB shape. DTOs = API shape.
│   │   │               │   │
│   │   │               │   ├── request/
│   │   │               │   │   ├── CreateExpenseRequest.java
│   │   │               │   │   │       ← @NotNull, @Positive, @PastOrPresent etc.
│   │   │               │   │   │         Validated by @Valid in controller.
│   │   │               │   │   │         (Story 1 — first PR, added validation)
│   │   │               │   │   │
│   │   │               │   │   ├── ApproveExpenseRequest.java
│   │   │               │   │   │       ← Just { comment: String }
│   │   │               │   │   │
│   │   │               │   │   ├── RejectExpenseRequest.java
│   │   │               │   │   │       ← { reason: String } — @NotBlank, rejection needs reason
│   │   │               │   │   │
│   │   │               │   │   └── BulkApproveRequest.java
│   │   │               │   │           ← { expenseIds: List<UUID>, comment: String }
│   │   │               │   │             (Story 7 — bulk approval)
│   │   │               │   │
│   │   │               │   └── response/
│   │   │               │       ├── ExpenseResponse.java
│   │   │               │       │       ← What GET /expenses returns for each expense.
│   │   │               │       │         Includes employeeName, approverName (from JOIN FETCH).
│   │   │               │       │
│   │   │               │       ├── ReimbursementResponse.java
│   │   │               │       │
│   │   │               │       ├── PagedResponse.java
│   │   │               │       │       ← Generic wrapper: { content: List<T>, pagination: {...} }
│   │   │               │       │         Used by all paginated endpoints.
│   │   │               │       │
│   │   │               │       ├── BulkApprovalResponse.java
│   │   │               │       │       ← { totalRequested, approved, skipped, results: [...] }
│   │   │               │       │
│   │   │               │       ├── BulkApprovalResult.java
│   │   │               │       │       ← Per-expense result in bulk response.
│   │   │               │       │         { expenseId, status: APPROVED/SKIPPED, reason? }
│   │   │               │       │
│   │   │               │       └── ErrorResponse.java
│   │   │               │               ← Standard error shape: timestamp, status, error,
│   │   │               │                 message, path, traceId.
│   │   │               │
│   │   │               ├── exception/
│   │   │               │   │   ← Custom exception classes. Each maps to a specific HTTP status
│   │   │               │   │     via GlobalExceptionHandler.
│   │   │               │   │
│   │   │               │   ├── ExpenseNotFoundException.java       ← 404
│   │   │               │   ├── InvalidExpenseStateException.java   ← 409
│   │   │               │   ├── UnauthorizedActionException.java    ← 403
│   │   │               │   ├── ExpenseAmountExceededException.java ← 400
│   │   │               │   ├── DuplicateExpenseException.java      ← 409
│   │   │               │   ├── TransientException.java             ← Used in Kafka consumers
│   │   │               │   │                                          (Story 8 — retry)
│   │   │               │   └── PermanentException.java             ← Used in Kafka consumers
│   │   │               │                                              (Story 8 — route to DLT)
│   │   │               │
│   │   │               ├── kafka/
│   │   │               │   │   ← Everything Kafka-related. Separated from service layer
│   │   │               │   │     because Kafka is infrastructure, not business logic.
│   │   │               │   │
│   │   │               │   ├── consumer/
│   │   │               │   │   ├── PaymentCompletedConsumer.java
│   │   │               │   │   │       ← @KafkaListener(topics = "payment.completed")
│   │   │               │   │   │         Calls ReimbursementService.processPaymentCompleted()
│   │   │               │   │   │         Manual ack. Catches DataAccessException separately.
│   │   │               │   │   │         (Story 8 — your first Kafka consumer)
│   │   │               │   │   │
│   │   │               │   │   └── UserDeactivatedConsumer.java
│   │   │               │   │           ← @KafkaListener(topics = "user.deactivated")
│   │   │               │   │             Reassigns pending approvals when user is removed.
│   │   │               │   │
│   │   │               │   ├── OutboxPoller.java
│   │   │               │   │       ← @Scheduled(fixedDelay = 100)
│   │   │               │   │         Polls outbox_events table every 100ms.
│   │   │               │   │         Publishes unpublished events to Kafka.
│   │   │               │   │         Marks events as published after confirmation.
│   │   │               │   │
│   │   │               │   └── config/
│   │   │               │       └── KafkaConsumerConfig.java
│   │   │               │               ← Configures DefaultErrorHandler with:
│   │   │               │                   ExponentialBackOffWithMaxRetries(3)
│   │   │               │                   addNotRetryableExceptions(PermanentException.class)
│   │   │               │                   DeadLetterPublishingRecoverer
│   │   │               │                 (Story 18 — DLQ implementation)
│   │   │               │
│   │   │               ├── client/
│   │   │               │   │   ← FeignClient interfaces. Define how we call other services.
│   │   │               │   │     Spring generates the HTTP client from the interface.
│   │   │               │   │
│   │   │               │   └── UserOrgFeignClient.java
│   │   │               │           ← @FeignClient(name = "user-org-service")
│   │   │               │             getApprovalPolicy(UUID companyId)
│   │   │               │             getManager(UUID employeeId)
│   │   │               │             getFinanceManager(UUID companyId)
│   │   │               │             healthCheck()
│   │   │               │
│   │   │               ├── metrics/
│   │   │               │   │   ← Custom Micrometer metrics. These are what back up
│   │   │               │   │     resume claims like "reduced by X%".
│   │   │               │   │
│   │   │               │   └── ExpenseMetrics.java
│   │   │               │           ← recordExpenseSubmitted(currency, category)
│   │   │               │             recordExpenseDecision(decision, companyId)
│   │   │               │             recordApprovalDuration(duration, decision)
│   │   │               │             registerPendingGauge(supplier)
│   │   │               │             recordOcrDuration(duration, success)
│   │   │               │
│   │   │               ├── health/
│   │   │               │   │   ← Custom Spring Boot Actuator health indicators.
│   │   │               │   │     Appear in /manage/health response.
│   │   │               │   │
│   │   │               │   ├── DatabaseHealthIndicator.java
│   │   │               │   │       ← Checks DB connection + HikariCP pool stats.
│   │   │               │   │
│   │   │               │   ├── KafkaHealthIndicator.java
│   │   │               │   │       ← Checks if Kafka producer can reach brokers.
│   │   │               │   │
│   │   │               │   └── OutboxHealthIndicator.java
│   │   │               │           ← countByPublishedFalse() > 500 → DOWN
│   │   │               │             (Written in month 10, first real trigger in Story 16)
│   │   │               │
│   │   │               ├── filter/
│   │   │               │   └── CorrelationIdFilter.java
│   │   │               │           ← OncePerRequestFilter.
│   │   │               │             Reads traceId from Micrometer.
│   │   │               │             Puts it in MDC for log correlation.
│   │   │               │             Adds X-CorrelationId to response headers.
│   │   │               │
│   │   │               ├── security/
│   │   │               │   ├── JwtAuthenticationFilter.java
│   │   │               │   │       ← Reads X-User-Id, X-Company-Id, X-User-Role headers.
│   │   │               │   │         Populates Spring SecurityContext.
│   │   │               │   │         This is the bridge header → SecurityContext.
│   │   │               │   │         (Story 14 — teaching Léa @PreAuthorize)
│   │   │               │   │
│   │   │               │   └── SecurityConfig.java
│   │   │               │           ← @EnableWebSecurity @EnableMethodSecurity
│   │   │               │             Registers JwtAuthenticationFilter.
│   │   │               │             @EnableMethodSecurity makes @PreAuthorize work.
│   │   │               │             Without this annotation → @PreAuthorize does nothing!
│   │   │               │
│   │   │               ├── config/
│   │   │               │   │   ← Spring configuration beans. Wires up infrastructure components.
│   │   │               │   │
│   │   │               │   ├── RedisConfig.java
│   │   │               │   │       ← Creates RedisTemplate<String, ApprovalPolicy> bean.
│   │   │               │   │         JSON serializer for values.
│   │   │               │   │         String serializer for keys.
│   │   │               │   │
│   │   │               │   ├── AsyncConfig.java
│   │   │               │   │       ← @EnableAsync
│   │   │               │   │         Registers MdcTaskDecorator so MDC (correlationId)
│   │   │               │   │         propagates into @Async threads.
│   │   │               │   │         (Module 11 — logging, @Async + MDC)
│   │   │               │   │
│   │   │               │   └── FeatureFlagConfig.java
│   │   │               │           ← @ConfigurationProperties(prefix = "moss.feature")
│   │   │               │             Maps feature flag properties to a Java object.
│   │   │               │             (Module 10 — feature flags)
│   │   │               │
│   │   │               └── model/
│   │   │                   │   ← Value objects and enums. No JPA, no HTTP. Just data.
│   │   │                   │
│   │   │                   ├── ApprovalPolicy.java
│   │   │                   │       ← Deserialized from User & Org FeignClient response.
│   │   │                   │         Also stored/retrieved from Redis cache.
│   │   │                   │
│   │   │                   ├── ApprovalRule.java
│   │   │                   │       ← minAmount, maxAmount, approverRole — inside policy.
│   │   │                   │
│   │   │                   ├── ApprovalStepDefinition.java
│   │   │                   │       ← Intermediate object returned by ApprovalRoutingService.
│   │   │                   │         Not persisted. Used to create ApprovalStep entities.
│   │   │                   │
│   │   │                   └── enums/
│   │   │                       ├── ExpenseStatus.java
│   │   │                       │       ← DRAFT, SUBMITTED, PENDING_APPROVAL,
│   │   │                       │         APPROVED, REJECTED, REIMBURSED, CANCELLED
│   │   │                       │
│   │   │                       ├── ExpenseCategory.java
│   │   │                       │       ← TRAVEL, ACCOMMODATION, CLIENT_ENTERTAINMENT, etc.
│   │   │                       │
│   │   │                       ├── ApprovalAction.java
│   │   │                       │       ← PENDING, APPROVED, REJECTED, DELEGATED
│   │   │                       │
│   │   │                       ├── ApproverRole.java
│   │   │                       │       ← MANAGER, FINANCE_MANAGER, SELF
│   │   │                       │
│   │   │                       └── BulkApprovalStatus.java
│   │   │                               ← APPROVED, SKIPPED
│   │   │
│   │   └── resources/
│   │       │   ← Configuration files. NOT Java code. Read at startup.
│   │       │
│   │       ├── application.properties
│   │       │       ← Base config. Shared across ALL environments.
│   │       │         spring.application.name, JPA settings,
│   │       │         actuator endpoints, Kafka consumer group,
│   │       │         Micrometer tags (service name, team name).
│   │       │
│   │       ├── application-dev.properties
│   │       │       ← Local development overrides.
│   │       │         DB credentials (localhost:5432),
│   │       │         Redis (localhost:6379),
│   │       │         Kafka (localhost:9092),
│   │       │         WireMock URL for User & Org Service,
│   │       │         spring.jpa.show-sql=true
│   │       │         (Story 2 — this file was where onboarding confusion came from)
│   │       │
│   │       ├── logback-spring.xml
│   │       │       ← Logging configuration.
│   │       │         JSON format output via LogstashEncoder.
│   │       │         Masks sensitive fields (IBAN, password, token).
│   │       │         Async appender for non-ERROR, sync for ERROR.
│   │       │
│   │       └── db/
│   │           └── migration/
│   │               │   ← Flyway migration scripts. SQL files that create/alter the DB.
│   │               │     Named V{number}__{description}.sql
│   │               │     Once applied to any environment: NEVER modify them.
│   │               │
│   │               ├── V1__create_companies_and_employees.sql
│   │               ├── V2__create_expenses_table.sql
│   │               ├── V3__add_expense_audit_logs.sql
│   │               ├── V4__create_approval_steps.sql
│   │               ├── V5__create_reimbursements.sql
│   │               ├── V6__create_outbox_events.sql
│   │               ├── V7__update_approval_steps_for_multi_level.sql
│   │               │       ← Added approver_role, activated_at columns.
│   │               │         Added partial index on (approver_id, action) WHERE PENDING.
│   │               │         (Story 5 — multi-level approval)
│   │               │
│   │               └── V8__add_index_for_duplicate_expense_check.sql
│   │                       ← Added after Story 9 incident.
│   │                         Non-transactional migration (CONCURRENTLY).
│   │
│   └── test/
│       └── java/
│           └── com/
│               └── moss/
│                   └── expense/
│                       │
│                       ├── controller/
│                       │   └── ExpenseControllerTest.java
│                       │           ← @WebMvcTest — tests HTTP layer only.
│                       │             Mocks the service layer.
│                       │             Tests: validation, status codes, request mapping.
│                       │
│                       ├── service/
│                       │   ├── ExpenseServiceTest.java
│                       │   │       ← @ExtendWith(MockitoExtension.class)
│                       │   │         Unit tests for business logic.
│                       │   │         Mocks repositories and clients.
│                       │   │
│                       │   └── ReimbursementServiceTest.java
│                       │           ← Tests processPaymentCompleted().
│                       │             Tests idempotency (already COMPLETED → skip).
│                       │             Tests PermanentException for not-found.
│                       │             (Story 8 — unit tests for Kafka consumer service)
│                       │
│                       ├── integration/
│                       │   ├── ExpenseServiceIntegrationTest.java
│                       │   │       ← @SpringBootTest @Testcontainers
│                       │   │         Real PostgreSQL + Kafka containers.
│                       │   │         Tests full flow: submit → approve → outbox event.
│                       │   │         (Module 10 — integration test example)
│                       │   │
│                       │   ├── PaymentCompletedConsumerIntegrationTest.java
│                       │   │       ← Tests Kafka consumer wiring.
│                       │   │         Publishes event, waits (Awaitility), asserts DB.
│                       │   │         (Story 8 — integration test)
│                       │   │
│                       │   └── ApprovalPolicyServiceIntegrationTest.java
│                       │           ← Tests Redis cache behavior.
│                       │             Tests fallback when Redis is stopped.
│                       │             Tests concurrent miss with mutex lock.
│                       │             (Story 12, Story 13)
│                       │
│                       └── repository/
│                           └── CreateExpenseRequestTest.java
│                                   ← Tests validation annotations directly.
│                                     Uses Validator, no Spring context needed.
│                                     (Story 1 — first PR, learning test patterns)
```

---

## Invoice & AP Service — Full Structure

```
invoice-service/
│
├── pom.xml
├── Dockerfile
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── moss/
│   │   │           └── invoice/
│   │   │               │
│   │   │               ├── InvoiceServiceApplication.java
│   │   │               │
│   │   │               ├── controller/
│   │   │               │   ├── InvoiceController.java
│   │   │               │   │       ← POST /api/v1/invoices
│   │   │               │   │         GET  /api/v1/invoices
│   │   │               │   │         GET  /api/v1/invoices/{id}
│   │   │               │   │         PUT  /api/v1/invoices/{id}/verify
│   │   │               │   │         PUT  /api/v1/invoices/{id}/approve
│   │   │               │   │         PUT  /api/v1/invoices/{id}/reject
│   │   │               │   │
│   │   │               │   ├── SupplierController.java
│   │   │               │   │       ← POST /api/v1/suppliers
│   │   │               │   │         GET  /api/v1/suppliers
│   │   │               │   │         GET  /api/v1/suppliers/{id}
│   │   │               │   │
│   │   │               │   ├── PaymentRunController.java
│   │   │               │   │       ← POST /api/v1/payment-runs
│   │   │               │   │         GET  /api/v1/payment-runs/{id}
│   │   │               │   │
│   │   │               │   └── advice/
│   │   │               │       └── GlobalExceptionHandler.java
│   │   │               │               ← Same pattern as expense-service.
│   │   │               │                 Also handles LockTimeoutException → 409.
│   │   │               │                 Added after Story 16 incident.
│   │   │               │
│   │   │               ├── service/
│   │   │               │   ├── InvoiceService.java
│   │   │               │   │       ← createInvoice(), processOcrCompletion(),
│   │   │               │   │         verifyInvoice(), approveInvoice(), rejectInvoice(),
│   │   │               │   │         processPaymentCompleted(), processUserDeactivated()
│   │   │               │   │         processVerificationRequested()
│   │   │               │   │
│   │   │               │   ├── PaymentRunService.java
│   │   │               │   │       ← createPaymentRun()
│   │   │               │   │         Uses pessimistic locking on invoices.
│   │   │               │   │         (Story 16 incident — lock contention here)
│   │   │               │   │
│   │   │               │   ├── InvoiceLineItemMapper.java
│   │   │               │   │       ← toEntity() and toResponse() for line items.
│   │   │               │   │         During migration: writes to BOTH description AND notes.
│   │   │               │   │         Reads from notes with fallback to description.
│   │   │               │   │         (Story 17 — expand-then-contract migration)
│   │   │               │   │
│   │   │               │   └── InvoiceAuditService.java
│   │   │               │           ← @Transactional(REQUIRES_NEW) on each method.
│   │   │               │             Same pattern as ExpenseAuditService.
│   │   │               │
│   │   │               ├── repository/
│   │   │               │   ├── InvoiceRepository.java
│   │   │               │   │       ← findByCompanyIdAndStatus() — paginated
│   │   │               │   │         findAndLockForPaymentRun() — pessimistic lock
│   │   │               │   │           @Lock(PESSIMISTIC_WRITE), SELECT FOR UPDATE
│   │   │               │   │           (Story 16 — this is where the lock contention was)
│   │   │               │   │
│   │   │               │   ├── InvoiceLineItemRepository.java
│   │   │               │   │
│   │   │               │   ├── SupplierRepository.java
│   │   │               │   │
│   │   │               │   ├── PaymentRunRepository.java
│   │   │               │   │
│   │   │               │   ├── InvoiceApprovalStepRepository.java
│   │   │               │   │       ← Same pattern as expense ApprovalStepRepository.
│   │   │               │   │         findCurrentActiveStep(), findNextPendingStep()
│   │   │               │   │
│   │   │               │   ├── OutboxEventRepository.java
│   │   │               │   │       ← Same outbox pattern as expense-service.
│   │   │               │   │
│   │   │               │   └── InvoiceAuditLogRepository.java
│   │   │               │
│   │   │               ├── entity/
│   │   │               │   ├── Invoice.java
│   │   │               │   │       ← Maps to `invoices` table.
│   │   │               │   │         @Version for optimistic locking.
│   │   │               │   │
│   │   │               │   ├── InvoiceLineItem.java
│   │   │               │   │       ← Maps to `invoice_line_items` table.
│   │   │               │   │         Has BOTH description and notes fields during migration.
│   │   │               │   │         description is @Deprecated.
│   │   │               │   │         (Story 17 — expand-then-contract)
│   │   │               │   │
│   │   │               │   ├── Supplier.java
│   │   │               │   │       ← Maps to `suppliers` table.
│   │   │               │   │         isTrusted boolean — determines if verification skipped.
│   │   │               │   │         (Story 11 — trusted supplier feature, Tomás conflict)
│   │   │               │   │
│   │   │               │   ├── PaymentRun.java
│   │   │               │   │
│   │   │               │   ├── InvoiceApprovalStep.java
│   │   │               │   │
│   │   │               │   ├── OutboxEvent.java
│   │   │               │   │
│   │   │               │   └── InvoiceAuditLog.java
│   │   │               │
│   │   │               ├── dto/
│   │   │               │   ├── request/
│   │   │               │   │   ├── CreateInvoiceRequest.java
│   │   │               │   │   │       ← Has @Valid @NotEmpty List<CreateLineItemRequest>
│   │   │               │   │   │
│   │   │               │   │   ├── CreateLineItemRequest.java
│   │   │               │   │   │       ← Field is now called notes (was description).
│   │   │               │   │   │         categoryCode field added.
│   │   │               │   │   │         (Story 17 — migration)
│   │   │               │   │   │
│   │   │               │   │   ├── CreateSupplierRequest.java
│   │   │               │   │   │       ← IBAN regex validation, BIC validation.
│   │   │               │   │   │
│   │   │               │   │   └── CreatePaymentRunRequest.java
│   │   │               │   │           ← invoiceIds: List<UUID>, scheduledDate: LocalDate
│   │   │               │   │
│   │   │               │   └── response/
│   │   │               │       ├── InvoiceResponse.java
│   │   │               │       ├── InvoiceLineItemResponse.java
│   │   │               │       │       ← Returns notes field (not description).
│   │   │               │       │
│   │   │               │       ├── SupplierResponse.java
│   │   │               │       ├── PaymentRunResponse.java
│   │   │               │       └── ErrorResponse.java
│   │   │               │
│   │   │               ├── exception/
│   │   │               │   ├── InvoiceNotFoundException.java        ← 404
│   │   │               │   ├── InvalidInvoiceStateException.java    ← 409
│   │   │               │   ├── SupplierNotFoundException.java       ← 404
│   │   │               │   ├── InvalidPaymentRunException.java      ← 400
│   │   │               │   ├── TransientException.java              ← Kafka retry
│   │   │               │   └── PermanentException.java              ← Kafka DLT
│   │   │               │
│   │   │               ├── kafka/
│   │   │               │   ├── consumer/
│   │   │               │   │   ├── PaymentCompletedConsumer.java
│   │   │               │   │   │       ← Marks invoices as PAID when payment runs complete.
│   │   │               │   │   │         Thin consumer — delegates to InvoiceService.
│   │   │               │   │   │
│   │   │               │   │   ├── UserDeactivatedConsumer.java
│   │   │               │   │   │
│   │   │               │   │   └── InvoiceVerificationRequestedConsumer.java
│   │   │               │   │           ← Assigns verifier to invoice.
│   │   │               │   │
│   │   │               │   ├── OutboxPoller.java
│   │   │               │   │       ← Same as expense-service.
│   │   │               │   │         (Story 16 — this is what broke during the incident)
│   │   │               │   │
│   │   │               │   └── config/
│   │   │               │       ├── KafkaConsumerConfig.java
│   │   │               │       │       ← DefaultErrorHandler with DLT routing.
│   │   │               │       │         (Story 18 — DLQ implementation)
│   │   │               │       │
│   │   │               │       └── KafkaTopicConfig.java
│   │   │               │               ← Declares all DLT topics:
│   │   │               │                   payment.completed.DLT
│   │   │               │                   user.deactivated.DLT
│   │   │               │                   invoice.verification.requested.DLT
│   │   │               │                 30-day retention on DLT topics.
│   │   │               │                 (Story 18 — DLQ implementation)
│   │   │               │
│   │   │               ├── client/
│   │   │               │   └── UserOrgFeignClient.java
│   │   │               │           ← Same as expense-service.
│   │   │               │
│   │   │               ├── metrics/
│   │   │               │   └── InvoiceMetrics.java
│   │   │               │           ← recordInvoiceLifecycleDuration()
│   │   │               │             recordPaymentRunCompleted()
│   │   │               │             recordOutboxPublish()
│   │   │               │             startPaymentRunTimer() / recordPaymentRunDuration()
│   │   │               │               (Added after Story 16 — long-running tx alert)
│   │   │               │
│   │   │               ├── health/
│   │   │               │   ├── DatabaseHealthIndicator.java
│   │   │               │   ├── KafkaHealthIndicator.java
│   │   │               │   └── OutboxHealthIndicator.java
│   │   │               │           ← This fired in Story 16 production incident.
│   │   │               │             countByPublishedFalse() > 500 → DOWN.
│   │   │               │
│   │   │               ├── filter/
│   │   │               │   └── CorrelationIdFilter.java
│   │   │               │
│   │   │               ├── security/
│   │   │               │   ├── JwtAuthenticationFilter.java
│   │   │               │   └── SecurityConfig.java
│   │   │               │
│   │   │               └── config/
│   │   │                   ├── RedisConfig.java
│   │   │                   ├── AsyncConfig.java
│   │   │                   └── FeatureFlagConfig.java
│   │   │
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       ├── logback-spring.xml
│   │       └── db/
│   │           └── migration/
│   │               ├── V1__create_suppliers_table.sql
│   │               ├── V2__create_invoices_table.sql
│   │               ├── V3__create_invoice_line_items.sql
│   │               ├── V4__create_invoice_approval_steps.sql
│   │               ├── V5__create_payment_runs.sql
│   │               ├── V6__create_invoice_audit_logs.sql
│   │               ├── V7__create_outbox_events.sql
│   │               ├── V8__add_trusted_flag_to_suppliers.sql
│   │               │       ← Added for Story 11 trusted supplier feature.
│   │               │
│   │               ├── V9__add_lock_timeout_note.sql
│   │               │       ← Reminder migration added after Story 16.
│   │               │         Actual lock_timeout set in HikariCP connectionInitSql.
│   │               │
│   │               ├── V10__add_index_invoices_company_status.sql
│   │               │
│   │               ├── V11__add_index_invoice_approval_approver.sql
│   │               │
│   │               ├── V12__add_index_invoices_due_date.sql
│   │               │       (flyway:executeInTransaction=false — CONCURRENTLY)
│   │               │       ← Story 9 incident fix (missing index on due_date).
│   │               │
│   │               ├── V18__add_notes_column_to_invoice_line_items.sql
│   │               │       ← Story 17 — expand step. Adds notes + category_code.
│   │               │         description still there.
│   │               │
│   │               ├── V19__migrate_description_to_notes.sql
│   │               │       (flyway:executeInTransaction=false — batched UPDATE)
│   │               │       ← Story 17 — data migration. 847k rows, SKIP LOCKED.
│   │               │
│   │               ├── V20__add_line_item_categories_table.sql
│   │               │       ← Reference table for category codes.
│   │               │
│   │               ├── V21__add_composite_index_invoices_list_query.sql
│   │               │       (flyway:executeInTransaction=false — CONCURRENTLY)
│   │               │       ← Story 19 — proactive P99 fix. Index on
│   │               │         (company_id, status, due_date) WHERE NOT IN (PAID, CANCELLED).
│   │               │
│   │               └── V22__drop_description_column_from_invoice_line_items.sql
│   │                       ← Story 17 — contract step. Safe to drop after
│   │                         all reads migrated to notes.
│   │
│   └── test/
│       └── java/
│           └── com/
│               └── moss/
│                   └── invoice/
│                       ├── service/
│                       │   ├── InvoiceServiceTest.java
│                       │   └── PaymentRunServiceTest.java
│                       │
│                       ├── integration/
│                       │   ├── InvoiceServiceIntegrationTest.java
│                       │   └── DLTRoutingIntegrationTest.java
│                       │           ← Tests PermanentException → DLT immediately.
│                       │             Tests DataAccessException → retry → DLT.
│                       │             Tests success on retry → no DLT.
│                       │             (Story 18 — DLQ tests)
│                       │
│                       └── repository/
│                           └── InvoiceRepositoryTest.java
```

---

## The Shared Module — common-events

```
common-events/                       ← Shared Maven module. Both services depend on it.
│                                       Contains event classes published/consumed via Kafka.
│                                       When Payment Service adds a field, they update this module.
│
├── pom.xml
│
└── src/main/java/com/moss/events/
    │
    ├── payment/
    │   └── PaymentCompletedEvent.java
    │           ← paymentId, reimbursementId, employeeId,
    │             amount, currency, paymentReference, completedAt,
    │             paymentType (REIMBURSEMENT or INVOICE_PAYMENT)
    │             processingFee, processingFeeCurrency  ← Added in Story 10
    │               (both nullable for backward compatibility)
    │
    ├── user/
    │   └── UserDeactivatedEvent.java
    │           ← userId, companyId, deactivatedAt
    │
    ├── ocr/
    │   └── OcrCompletedEvent.java
    │           ← invoiceId, extractedData: OcrExtractedData
    │
    └── invoice/
        └── InvoiceVerificationRequestedEvent.java
                ← invoiceId, companyId, requestedAt
```

---

## How the Folder Structure Connects to the Stories

Here is a quick reference so when you re-read a story you know exactly which file it lives in:

```
STORY → FILE
──────────────────────────────────────────────────────────────────

Story 1  (First PR, validation)
  → expense-service/...dto/request/CreateExpenseRequest.java
  → expense-service/...service/ExpenseService.java
     (max-amount validation moved here, @Value from config)
  → expense-service/.../db/migration/V2__create_expenses_table.sql

Story 2  (Onboarding, WireMock)
  → expense-service/.../resources/application-dev.properties
  → wiremock/mappings/health-check.json  (you added this)

Story 3  (Helping Marta, outbox explanation)
  → expense-service/.../kafka/OutboxPoller.java
  → expense-service/.../repository/OutboxEventRepository.java

Story 4  (N+1 fix, JOIN FETCH)
  → expense-service/.../repository/ExpenseRepository.java
     (findWithEmployeesByCompanyIdAndStatus — the JOIN FETCH query)
  → expense-service/.../entity/Expense.java
     (@ManyToOne employee and assignedApprover)
  → expense-service/.../service/ExpenseService.java
     (getPendingApprovals — the fixed service method)

Story 5  (Multi-level approval)
  → expense-service/.../service/ApprovalRoutingService.java
  → expense-service/.../repository/ApprovalStepRepository.java
  → expense-service/.../entity/ApprovalStep.java
  → expense-service/.../db/migration/V7__update_approval_steps_for_multi_level.sql

Story 6  (@Transactional private method)
  → expense-service/.../service/ExpenseAuditService.java
     (the fix: @Transactional only on public methods, private builder helper)

Story 7  (Bulk approval, pushing back on PM)
  → expense-service/.../service/BulkApprovalService.java
  → expense-service/.../service/SingleApprovalService.java
     (separate bean — REQUIRES_NEW works correctly)
  → expense-service/.../controller/ExpenseController.java
     (PUT /api/v1/expenses/bulk-approve endpoint)

Story 8  (Kafka consumer, payment.completed)
  → expense-service/.../kafka/consumer/PaymentCompletedConsumer.java
  → expense-service/.../service/ReimbursementService.java
     (processPaymentCompleted — idempotency check, outbox event)
  → common-events/.../payment/PaymentCompletedEvent.java

Story 9  (Production incident, missing index)
  → expense-service/.../db/migration/V8__add_index_for_duplicate_expense_check.sql
  → expense-service/.../repository/ExpenseRepository.java
     (existsDuplicateSubmission — the slow query that caused the incident)

Story 10 (Cross-team schema, payment.completed)
  → common-events/.../payment/PaymentCompletedEvent.java
     (processingFee, processingFeeCurrency added as nullable)

Story 11 (Tomás conflict, trusted supplier)
  → invoice-service/.../entity/Supplier.java  (isTrusted flag)
  → invoice-service/.../service/InvoiceService.java
     (processOcrCompletion — the if(supplier.isTrusted()) check)
  → invoice-service/.../kafka/consumer/OcrCompletedConsumer.java  (thin consumer)
  → invoice-service/.../db/migration/V8__add_trusted_flag_to_suppliers.sql

Story 12 (Caching proposal, Redis)
  → expense-service/.../service/ApprovalPolicyService.java
     (Cache-Aside implementation, getApprovalPolicy with Redis)
  → expense-service/.../config/RedisConfig.java

Story 13 (Cache stampede fix, mutex lock)
  → expense-service/.../service/ApprovalPolicyService.java
     (mutex lock added: setIfAbsent + waitAndRetryFromCache)

Story 14 (Teaching Léa, @PreAuthorize)
  → expense-service/.../security/JwtAuthenticationFilter.java
  → expense-service/.../security/SecurityConfig.java
  → expense-service/.../controller/ExpenseController.java
     (@PreAuthorize annotations on endpoints)
  → expense-service/.../service/ExpenseService.java
     (SecurityContextHolder.getContext() for ownership check)

Story 15 (ADR)
  → docs/adr/ADR-012-approval-policy-caching.md  (Confluence, not in code)

Story 16 (Production incident, lock contention)
  → invoice-service/.../repository/InvoiceRepository.java
     (findAndLockForPaymentRun — SELECT FOR UPDATE)
  → invoice-service/.../service/PaymentRunService.java
  → invoice-service/.../health/OutboxHealthIndicator.java
     (fired the CRITICAL alert that started the investigation)
  → invoice-service/.../controller/advice/GlobalExceptionHandler.java
     (LockTimeoutException → 409 added after incident)
  → invoice-service/.../metrics/InvoiceMetrics.java
     (payment run timer added after incident)
  → invoice-service/.../resources/application.properties
     (spring.datasource.hikari.connection-init-sql=SET lock_timeout='10s' added)

Story 17 (Backward-compatible migration)
  → invoice-service/.../entity/InvoiceLineItem.java
     (description @Deprecated, notes field added)
  → invoice-service/.../service/InvoiceLineItemMapper.java
     (writes to both during transition, reads from notes with fallback)
  → invoice-service/.../dto/request/CreateLineItemRequest.java
     (field renamed from description to notes)
  → invoice-service/.../db/migration/V18, V19, V22

Story 18 (DLQ implementation)
  → invoice-service/.../kafka/config/KafkaConsumerConfig.java
  → invoice-service/.../kafka/config/KafkaTopicConfig.java
     (DLT topic declarations with 30-day retention)
  → invoice-service/.../kafka/consumer/PaymentCompletedConsumer.java
     (simplified — PermanentException now just thrown, not manually acked)

Story 19 (Proactive P99 fix, composite index)
  → invoice-service/.../db/migration/V21__add_composite_index_invoices_list_query.sql
     (flyway:executeInTransaction=false, CONCURRENTLY, partial index)

Story 20 (Wiki documentation)
  → Confluence: "How to Debug Kafka Consumer Lag in Expense & AP Services"
     (Not in the codebase — in team knowledge base)

Story 21 (Lukas's acknowledgment)
  → No code. The performance review conversation.
```

---

This gives you the complete map. Every file in every story now has a home. When you re-read Story 4 about the N+1 fix, you know it lives in `ExpenseRepository.java` and `Expense.java`. When Story 16 talks about the outbox health indicator firing, you know that's `invoice-service/.../health/OutboxHealthIndicator.java` calling `OutboxEventRepository.countByPublishedFalse()`.