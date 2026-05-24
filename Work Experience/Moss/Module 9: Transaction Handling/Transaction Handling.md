# Step 8: Transaction Handling

---

## Start With The Foundation — What Is A Transaction And Why Does It Matter Here?

A transaction is a group of operations that either **all succeed or all fail together**. No partial states.

In a spend management platform, this matters constantly:

```
Example 1 — Expense Approval:
──────────────────────────────
When a manager approves an expense:
  1. UPDATE expenses SET status = 'APPROVED'
  2. INSERT INTO approval_steps (action = 'APPROVED')
  3. INSERT INTO expense_audit_logs
  4. INSERT INTO outbox_events (expense.approved event)

If step 2 fails after step 1 succeeded:
- Expense shows APPROVED in DB
- But no approval record exists
- Audit trail is incomplete
- Kafka event never fires
- Payment never triggers

This is a partial state — financially and 
legally dangerous. Transactions prevent this.

Example 2 — Payment Run Creation:
───────────────────────────────────
When a finance manager creates a payment run:
  1. INSERT INTO payment_runs
  2. UPDATE invoices SET payment_run_id = ?
     WHERE id IN (selected invoice IDs)
  3. UPDATE invoices SET status = 'PAYMENT_PENDING'

If step 3 fails after step 2:
- Invoices linked to payment run
- But still showing APPROVED status
- Finance manager sees inconsistent state
- Double payment risk on next run

Again — transaction prevents this.
```

---

## Part 1 — How Spring's `@Transactional` Actually Works

Most developers use `@Transactional` without understanding what's happening underneath. Interviewers go here.

```
WITHOUT @Transactional:
────────────────────────
Each repository call = its own connection 
from HikariCP pool, its own transaction,
auto-committed immediately.

Step 1: connection acquired, UPDATE committed, connection returned
Step 2: new connection acquired, INSERT committed, connection returned
Step 3: fails — nothing rolls back step 1 or step 2.

WITH @Transactional:
─────────────────────
Spring intercepts the method call 
(via AOP proxy — important detail below).
One connection acquired from HikariCP.
All operations share this connection.
At method end:
  - No exception → COMMIT
  - Exception thrown → ROLLBACK (all steps)
Connection returned to pool after commit/rollback.
```

### The AOP Proxy — Why Self-Calls Break Transactions

This is the most common `@Transactional` trap. Interviewers love this.

```
Spring doesn't modify your class.
It wraps it in a proxy object.
The proxy intercepts external calls 
and manages the transaction.

EXTERNAL CALL (works correctly):
──────────────────────────────────
Controller → [Spring Proxy] → ExpenseService.approveExpense()
             ↑ proxy starts transaction here

SELF-CALL (transaction NOT applied):
─────────────────────────────────────
Within ExpenseService:
this.approveExpense() 
↑ calls the real object directly,
  bypasses the proxy,
  NO transaction started.
```

**Concrete example:**

```java
@Service
public class ExpenseService {

    // ✅ Works — called from outside (controller)
    @Transactional
    public void approveExpense(UUID expenseId) {
        updateExpenseStatus(expenseId);  // self-call below
        insertAuditLog(expenseId);
    }

    // ❌ @Transactional here has NO effect
    // when called from approveExpense() above
    @Transactional
    public void updateExpenseStatus(UUID expenseId) {
        // This runs in the SAME transaction as 
        // approveExpense() — not its own.
        // If updateExpenseStatus() had 
        // REQUIRES_NEW, it would be IGNORED here.
    }
}
```

**The fix — extract into a separate bean:**

```java
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseStatusUpdater statusUpdater;

    @Transactional
    public void approveExpense(UUID expenseId) {
        // External call to a different bean — 
        // proxy IS applied
        statusUpdater.updateStatus(expenseId);
        insertAuditLog(expenseId);
    }
}

@Service
public class ExpenseStatusUpdater {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatus(UUID expenseId) {
        // Now runs in its OWN transaction
    }
}
```

---

## Part 2 — Transaction Propagation

Propagation defines what happens when one `@Transactional` method calls another.

```
The ones we actually use:
─────────────────────────

REQUIRED (default):
───────────────────
If a transaction exists → join it.
If no transaction → start a new one.

This is what 90% of our methods use.
approveExpense() starts a transaction.
insertAuditLog() (also @Transactional REQUIRED) 
joins the same transaction.
One commit at the end covers everything.


REQUIRES_NEW:
─────────────
Always start a NEW, independent transaction.
Suspend the current one if exists.

We use this for audit logging.

Why?
────
Scenario:
approveExpense() runs in Transaction A.
Something fails after approval but before 
the method ends → Transaction A rolls back.
But we STILL want the audit log entry 
("approval attempted, failed at step X").

If audit log is in same transaction:
rollback removes the audit log too.
No trace of what happened. Bad for compliance.

If audit log is in REQUIRES_NEW:
independent transaction, committed immediately.
Even if outer transaction rolls back,
audit log survives.

NOT_SUPPORTED:
──────────────
Suspend any current transaction.
Run without a transaction.

We use this for read-heavy reporting queries 
that don't modify data and don't need 
the overhead of transaction management.
Example: fetching dashboard statistics.
```

**Audit log with REQUIRES\_NEW:**

```java
@Service
@RequiredArgsConstructor
public class ExpenseAuditService {

    private final ExpenseAuditLogRepository auditLogRepository;

    // Runs in its OWN transaction, independent of caller
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(UUID expenseId, String action,
                          ExpenseStatus oldStatus,
                          ExpenseStatus newStatus,
                          UUID performedById) {

        ExpenseAuditLog log = ExpenseAuditLog.builder()
            .expenseId(expenseId)
            .action(action)
            .oldStatus(oldStatus)
            .newStatus(newStatus)
            .performedById(performedById)
            .createdAt(Instant.now())
            .build();

        auditLogRepository.save(log);
        // Committed immediately — survives outer rollback
    }
}
```

---

## Part 3 — Isolation Levels

Isolation defines what a transaction can **see** from other concurrent transactions. This is where most engineers' knowledge stops at "just use the default." Interviewers go deeper.

### The Four Problems Isolation Levels Solve

```
DIRTY READ:
───────────
Transaction A updates a row but hasn't committed.
Transaction B reads that updated row.
Transaction A rolls back.
Transaction B is working with data 
that never officially existed.

PHANTOM READ:
─────────────
Transaction A queries: 
  SELECT * FROM expenses WHERE status = 'PENDING'
  → Returns 10 rows

Transaction B inserts a new PENDING expense 
and commits.

Transaction A queries again (same query):
  → Returns 11 rows

The "phantom" row appeared between reads 
in the same transaction.

NON-REPEATABLE READ:
─────────────────────
Transaction A reads expense amount: €85.
Transaction B updates that expense to €100 
and commits.
Transaction A reads the same expense again: €100.

Same row, same transaction, different value.

LOST UPDATE:
─────────────
Transaction A reads count = 10.
Transaction B reads count = 10.
Transaction A updates count to 11, commits.
Transaction B updates count to 11, commits.
(B overwrites A's update — A's increment is lost)
```

### Isolation Levels and What They Prevent

```
LEVEL                 DIRTY   NON-REPEAT  PHANTOM   LOST
                      READ    READ        READ      UPDATE
──────────────────────────────────────────────────────────
READ UNCOMMITTED      ❌ No    ❌ No        ❌ No      ❌ No
READ COMMITTED        ✅ Yes   ❌ No        ❌ No      ❌ No
REPEATABLE READ       ✅ Yes   ✅ Yes       ❌ No      ✅ Yes
SERIALIZABLE          ✅ Yes   ✅ Yes       ✅ Yes     ✅ Yes

PostgreSQL default: READ COMMITTED
Spring default:     whatever the DB default is
                    (so READ COMMITTED for us)
```

### What We Use and Why

```
READ COMMITTED (default — most operations):
────────────────────────────────────────────
Expense submission, invoice ingestion, 
notification queries.

Why this is fine for most cases:
Each operation reads committed data.
For a simple "submit expense" flow,
we're not re-reading the same row twice.
No phantom read or non-repeatable read risk.
Overhead is minimal.


REPEATABLE READ (concurrent approval scenarios):
─────────────────────────────────────────────────
When do we need it?

Scenario: Two finance managers 
simultaneously try to approve the 
same invoice (multi-level approval bug).

Manager A: reads invoice → status = PENDING_APPROVAL
Manager B: reads invoice → status = PENDING_APPROVAL
Manager A: updates → status = APPROVED
Manager B: updates → status = APPROVED (overwrites!)

With READ COMMITTED:
Both reads see PENDING_APPROVAL.
Both proceed. Double approval.

With REPEATABLE READ:
PostgreSQL uses MVCC (Multi-Version Concurrency Control).
Manager A's transaction holds a snapshot.
When Manager B tries to update the same row
that Manager A has already updated:
PostgreSQL detects the conflict →
throws SerializationFailureException →
Manager B's transaction is rolled back.
We catch this and return 409 Conflict.

Implementation:
```

```java
@Transactional(isolation = Isolation.REPEATABLE_READ)
public InvoiceResponse approveInvoice(
        UUID invoiceId, UUID approverId, String comment) {

    Invoice invoice = invoiceRepository
        .findById(invoiceId)
        .orElseThrow(() -> 
            new InvoiceNotFoundException(invoiceId));

    // Validate state AFTER acquiring snapshot
    if (invoice.getStatus() != InvoiceStatus.PENDING_APPROVAL) {
        throw new InvalidInvoiceStateException(
            "Invoice is not pending approval. " +
            "Current status: " + invoice.getStatus()
        );
    }

    // Validate this person is the assigned approver
    if (!invoice.getAssignedApproverId().equals(approverId)) {
        throw new UnauthorizedActionException(
            "You are not the assigned approver for this invoice"
        );
    }

    invoice.setStatus(InvoiceStatus.APPROVED);
    invoice.setApprovedById(approverId);
    invoice.setApprovedAt(Instant.now());
    invoiceRepository.save(invoice);

    // Outbox event written in same transaction
    outboxEventRepository.save(OutboxEvent.builder()
        .aggregateType("INVOICE")
        .aggregateId(invoiceId)
        .eventType("invoice.approved")
        .payload(buildPayload(invoice))
        .build());

    return InvoiceResponse.from(invoice);
}
```

```
SERIALIZABLE (we don't use in hot paths):
──────────────────────────────────────────
Strongest isolation. Completely eliminates
all concurrency anomalies.

Cost: significant performance overhead.
Transactions are queued almost serially.

We don't use this in our team's services.
The scenarios where phantom reads matter
(complex financial reporting) happen in 
a read-only reporting service, 
not in our transactional services.

If needed: REPEATABLE READ + 
           optimistic locking (below) 
           gives similar safety at 
           lower cost.
```

---

## Part 4 — Optimistic vs Pessimistic Locking

Two strategies to handle concurrent modifications to the same row.

### Optimistic Locking — What We Use by Default

```
Assumption: conflicts are RARE.
Don't lock anything upfront.
Detect conflicts at commit time.

HOW IT WORKS:
──────────────
Each row has a VERSION column (integer).
When you read a row, you read its version.
When you update, you check:
  UPDATE expenses 
  SET status = 'APPROVED', version = version + 1
  WHERE id = ? AND version = ? (the version you read)

If version changed since you read it 
(someone else updated meanwhile):
  → UPDATE affects 0 rows
  → JPA throws OptimisticLockException
  → Transaction rolls back
  → Client gets 409 Conflict

IMPLEMENTATION:
```

```java
@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    private UUID id;

    @Version  // ← JPA handles everything automatically
    private Long version;

    private ExpenseStatus status;
    // ... other fields
}
```

```
JPA automatically:
- Reads version when loading entity
- Adds WHERE version = ? to UPDATE
- Increments version on successful update
- Throws OptimisticLockException if 0 rows updated

We catch OptimisticLockException in our 
GlobalExceptionHandler and return 409 Conflict.
```

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(
            OptimisticLockException ex,
            HttpServletRequest request) {

        return ResponseEntity.status(409).body(
            ErrorResponse.builder()
                .status(409)
                .error("CONCURRENT_MODIFICATION")
                .message("This record was modified by " +
                    "another request. Please refresh and retry.")
                .path(request.getRequestURI())
                .traceId(getCurrentTraceId())
                .build()
        );
    }
}
```

### Pessimistic Locking — When We Use It

```
Assumption: conflicts are LIKELY.
Lock the row when you read it.
Nobody else can modify it until 
your transaction commits or rolls back.

HOW IT WORKS IN SQL:
─────────────────────
SELECT * FROM invoices 
WHERE id = ? 
FOR UPDATE;   ← row is locked immediately

Other transactions trying to 
SELECT ... FOR UPDATE on same row:
→ BLOCK and wait until lock released.

WHEN WE USE IT:
────────────────
Payment run creation.

Scenario:
Finance manager A selects invoices 
[inv-1, inv-2, inv-3] for a payment run.
Finance manager B simultaneously selects
[inv-2, inv-3, inv-4] for another run.

inv-2 and inv-3 would be in TWO payment runs.
Double payment.

With pessimistic lock:
Manager A locks [inv-1, inv-2, inv-3].
Manager B's query for inv-2 → BLOCKS.
Manager A commits → locks released.
Manager B now sees inv-2 has payment_run_id set.
Manager B's logic: "inv-2 already in a run, skip it."
No double payment.
```

```java
public interface InvoiceRepository 
        extends JpaRepository<Invoice, UUID> {

    // Pessimistic write lock — 
    // locks rows for duration of transaction
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Invoice i " +
           "WHERE i.id IN :ids " +
           "AND i.status = 'APPROVED' " +
           "AND i.paymentRunId IS NULL")
    List<Invoice> findAndLockForPaymentRun(
        @Param("ids") List<UUID> invoiceIds
    );
}
```

```java
@Transactional
public PaymentRun createPaymentRun(
        List<UUID> invoiceIds, 
        LocalDate scheduledDate,
        UUID createdById) {

    // Lock selected invoices immediately
    List<Invoice> invoices = invoiceRepository
        .findAndLockForPaymentRun(invoiceIds);

    // Validate all requested invoices were found 
    // and are eligible
    if (invoices.size() != invoiceIds.size()) {
        throw new InvalidPaymentRunException(
            "Some invoices are not eligible " +
            "(already in a payment run or not approved)"
        );
    }

    // Create payment run
    PaymentRun run = PaymentRun.builder()
        .companyId(invoices.get(0).getCompanyId())
        .status(PaymentRunStatus.SCHEDULED)
        .scheduledDate(scheduledDate)
        .totalAmount(invoices.stream()
            .map(Invoice::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add))
        .invoiceCount(invoices.size())
        .createdById(createdById)
        .build();

    paymentRunRepository.save(run);

    // Link invoices to this run
    invoices.forEach(inv -> {
        inv.setPaymentRunId(run.getId());
        inv.setStatus(InvoiceStatus.PAYMENT_PENDING);
    });
    invoiceRepository.saveAll(invoices);

    return run;
}
// Locks released automatically when 
// @Transactional method returns
```

### Optimistic vs Pessimistic — Decision Rule

```
USE OPTIMISTIC LOCKING WHEN:
─────────────────────────────
- Conflicts are rare (most cases)
- Read-heavy with occasional writes
- Short transactions
- Better user experience to 
  "retry on conflict" than to wait

Examples in our system:
- Expense approval (one approver at a time, 
  conflicts rare)
- Invoice status updates
- Expense submission

USE PESSIMISTIC LOCKING WHEN:
──────────────────────────────
- Conflicts are LIKELY if not prevented
- Data integrity risk is high
- Users can tolerate slight wait

Examples in our system:
- Payment run creation 
  (double payment = money gone)
- Any operation where the same rows
  are predictably contested
```

---

## Part 5 — Transactions With `@Async`

This is the most critical and tricky part. Interviewers specifically ask about this because most developers get it wrong.

### The Problem

```
SCENARIO:
──────────
approveExpense() is @Transactional.
Inside it, we trigger OCR re-processing 
of the receipt @Async (fire and forget,
doesn't need to block the approval).

@Transactional
public void approveExpense(UUID expenseId) {

    // DB operations...
    expense.setStatus(APPROVED);
    expenseRepository.save(expense);

    // Trigger async re-OCR 
    // (updates merchant name in background)
    ocrService.reprocessReceiptAsync(expenseId);

    // Insert outbox event...
}

@Async
public void reprocessReceiptAsync(UUID expenseId) {

    // THE BUG:
    // This method runs in a DIFFERENT thread.
    // @Transactional on approveExpense() 
    // is bound to the ORIGINAL thread.

    // When this async method tries to read 
    // the expense:
    Expense expense = expenseRepository
        .findById(expenseId).get();

    // If approveExpense() hasn't committed yet
    // (which it likely hasn't — async runs 
    // in parallel), this read sees the 
    // OLD status (before APPROVED).

    // The async operation works on stale data.
}
```

### Why This Happens

```
Spring's transaction context is stored 
in a ThreadLocal variable.
ThreadLocal = data bound to a specific thread.

When @Async spawns a new thread:
- New thread has NO transaction context
- It starts with no transaction at all
- Reads are outside the parent transaction
- Sees only committed data

The parent transaction hasn't committed yet.
So async child sees the old, pre-approval state.
```

### Solution 1 — Execute Async AFTER Transaction Commits

```
The async work should only start 
AFTER the main transaction commits.
Then async reads will see committed, 
correct data.

Spring provides: 
@TransactionalEventListener(phase = AFTER_COMMIT)
```

```java
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void approveExpense(UUID expenseId, UUID approverId) {

        Expense expense = expenseRepository
            .findById(expenseId).orElseThrow();

        expense.setStatus(ExpenseStatus.APPROVED);
        expense.setApprovedById(approverId);
        expense.setApprovedAt(Instant.now());
        expenseRepository.save(expense);

        outboxEventRepository.save(/* outbox entry */);

        // Publish an APPLICATION event (not Kafka)
        // This is Spring's internal event system
        eventPublisher.publishEvent(
            new ExpenseApprovedEvent(expenseId)
        );

        // @Transactional commits here
        // THEN the event listener fires
    }
}

@Component
public class ExpenseApprovedEventListener {

    private final OcrService ocrService;

    // Only fires AFTER the transaction commits
    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT
    )
    @Async  // runs in a new thread
    public void onExpenseApproved(ExpenseApprovedEvent event) {

        // Transaction is committed.
        // Reading expense now gives correct APPROVED status.
        ocrService.reprocessReceiptAsync(event.getExpenseId());
    }
}
```

```
Timeline with this approach:
─────────────────────────────
T=1: approveExpense() starts transaction
T=2: expense.status = APPROVED (in transaction)
T=3: outbox event written (in transaction)
T=4: Spring event published (queued, not fired yet)
T=5: @Transactional commits → DB has APPROVED status
T=6: AFTER_COMMIT fires → async reprocessing starts
T=7: async reads expense → sees APPROVED ✓
```

---

### Solution 2 — Pass Data to Async Method Directly

```
Instead of having the async method 
read from DB (where timing is uncertain),
pass the data it needs as parameters.

@Transactional
public void approveExpense(UUID expenseId) {

    Expense expense = expenseRepository
        .findById(expenseId).orElseThrow();

    expense.setStatus(APPROVED);
    expenseRepository.save(expense);

    // Pass data directly — no DB read needed in async
    ocrService.reprocessReceiptAsync(
        expenseId,
        expense.getReceiptS3Key(),  // already loaded
        expense.getMerchantName()   // already loaded
    );
}

@Async
public void reprocessReceiptAsync(
        UUID expenseId, 
        String s3Key, 
        String currentMerchant) {

    // No DB read — uses passed data
    // Safe regardless of transaction timing
}
```

```
When to use which:
───────────────────
Solution 1 (AFTER_COMMIT listener):
- When async work genuinely needs 
  to read fresh DB state
- When async work must only happen 
  if transaction succeeded 
  (e.g., send email only if approval committed)

Solution 2 (pass data):
- When async work only needs 
  data already loaded in the transaction
- Simpler, less infrastructure
- Preferred when it's sufficient
```

---

### The Most Critical Case — Outbox + Transaction

```
This is where @Transactional and 
Kafka come together.

The outbox pattern works because:
───────────────────────────────────
@Transactional covers:
  1. UPDATE expenses → APPROVED
  2. INSERT INTO outbox_events

Both are DB operations in the same transaction.
Either both commit → outbox poller 
  picks up event → Kafka publish happens
OR both rollback → no stale event, 
  no ghost payment triggered.

The async Outbox Poller:
─────────────────────────
Runs in its OWN transaction 
(@Transactional on pollAndPublish()).
Reads committed outbox events only.
Never sees uncommitted events.
Correct by design.

THE WRONG PATTERN (what not to do):
─────────────────────────────────────
@Transactional
public void approveExpense(UUID expenseId) {

    // DB update...

    // DON'T DO THIS:
    kafkaTemplate.send("expense.approved", event);
    // Kafka doesn't participate in @Transactional.
    // If DB rolls back after this line,
    // Kafka event is already sent.
    // Payment triggered for an expense 
    // that was never approved.
}

THE RIGHT PATTERN (what we do):
─────────────────────────────────
@Transactional
public void approveExpense(UUID expenseId) {

    // DB update...

    // DO THIS:
    outboxEventRepository.save(outboxEvent);
    // DB write — participates in @Transactional.
    // Kafka publish happens separately 
    // via outbox poller AFTER commit.
}
```

---

## Part 6 — Common Transaction Mistakes We Fixed

These make for great interview stories because they're real, specific, and show growth.

### Mistake 1 — N+1 Inside a Transaction (Month 5)

```
WHAT HAPPENED:
───────────────
Building the finance dashboard — 
a list of 50 pending expenses with 
approver names.

Code (naive):
─────────────
@Transactional(readOnly = true)
public List<ExpenseResponse> getPendingExpenses(UUID companyId) {

    List<Expense> expenses = expenseRepository
        .findByCompanyIdAndStatus(companyId, PENDING_APPROVAL);
    // → 1 query, returns 50 expenses

    return expenses.stream()
        .map(expense -> {
            Employee approver = employeeRepository
                .findById(expense.getAssignedApproverId())
                .orElse(null);
            // ↑ 1 query PER expense = 50 queries!
            return ExpenseResponse.from(expense, approver);
        })
        .collect(toList());
}

Total: 1 + 50 = 51 queries for one page load.
Datadog showed this endpoint taking 800ms.

THE FIX:
─────────
Use a JOIN FETCH to load approvers 
in the initial query:

@Query("SELECT e FROM Expense e " +
       "LEFT JOIN FETCH e.assignedApprover " +
       "WHERE e.companyId = :companyId " +
       "AND e.status = :status")
List<Expense> findWithApprover(
    @Param("companyId") UUID companyId,
    @Param("status") ExpenseStatus status
);

Total: 1 query. 
Endpoint dropped from 800ms to 45ms.
Finn (the mid-level engineer who was 
good at query optimization) spotted 
this in code review at month 5 
and helped me fix it.
```

### Mistake 2 — `@Transactional` on a Private Method (Month 3)

```
WHAT HAPPENED:
───────────────
I added @Transactional to a private 
helper method inside ExpenseService.
Code compiled fine. Tests passed.
But in production, partial updates 
were not rolling back correctly.

THE BUG:
─────────
@Transactional uses AOP proxying.
Spring's proxy can only intercept 
calls to PUBLIC methods.
Private methods are called directly 
on the real object — proxy bypassed.
@Transactional on private methods 
is silently ignored.

THE FIX:
─────────
Move logic to a public method,
or restructure into a separate bean.

Elena caught this in code review 
and explained the proxy mechanism.
After that, I added a SonarQube rule 
to our pipeline that flags 
@Transactional on private methods.
Never happened again.
```

### Mistake 3 — Catching Exceptions Inside `@Transactional` (Month 6)

```
WHAT HAPPENED:
───────────────
I had this pattern:

@Transactional
public void processInvoice(UUID invoiceId) {

    try {
        updateInvoiceStatus(invoiceId);
        notifyApprover(invoiceId);    
        // this threw a RuntimeException
    } catch (Exception e) {
        log.error("Failed to notify approver", e);
        // swallowed the exception — 
        // returned normally
    }
}

Expected: transaction rolls back 
          if anything fails.
Actual:   transaction COMMITTED 
          even though notifyApprover() failed.

WHY:
─────
Spring rolls back @Transactional on 
UNCAUGHT RuntimeException (by default).
I caught the exception and swallowed it.
Spring saw a normal method return.
Transaction committed.
Invoice status updated even though 
notification failed.

THE FIX:
─────────
Option 1: Don't catch — let it propagate.
          Transaction rolls back.

Option 2: If you need to log but still rollback:

@Transactional
public void processInvoice(UUID invoiceId) {

    try {
        updateInvoiceStatus(invoiceId);
        notifyApprover(invoiceId);
    } catch (Exception e) {
        log.error("Failed", e);
        TransactionAspectSupport
            .currentTransactionStatus()
            .setRollbackOnly();
        // Marks transaction for rollback 
        // without throwing
        throw e; // or re-throw a custom exception
    }
}

Option 3 (better): 
Move notification outside the transaction.
Notification failure shouldn't affect 
invoice status update.
Use @TransactionalEventListener(AFTER_COMMIT)
to notify AFTER DB commit.
Status update and notification 
are decoupled concerns.

Lukas pointed this out in a 1:1 
after I reported a data inconsistency.
We fixed the pattern across the service.
```

---

## Part 7 — `readOnly = true` Transactions

```
@Transactional(readOnly = true)
────────────────────────────────
Used on methods that only READ data.
No inserts, no updates, no deletes.

What it does:
─────────────
1. Tells Hibernate: don't track entity changes
   (dirty checking disabled).
   Hibernate normally tracks every loaded entity
   to detect changes at flush time.
   For read-only, this tracking is wasted work.
   readOnly=true skips it → less memory, 
   faster execution.

2. Tells the DB driver: read-only hint.
   PostgreSQL can route to a read replica 
   if configured (we use this for 
   reporting/dashboard queries).

3. Prevents accidental writes.
   If you accidentally call save() inside 
   a readOnly transaction, Hibernate throws.
   Catches bugs early.

WHERE WE USE IT:
─────────────────
All GET endpoints:
  - getExpense(UUID id)
  - listExpenses(filters, pagination)
  - getDashboardStats(UUID companyId)
  - listInvoices(filters, pagination)

WHERE WE DON'T:
─────────────────
Any method that writes — approval, 
rejection, submission, creation.
```

```java
// Example:
@Transactional(readOnly = true)
public PagedResponse<ExpenseResponse> getExpenses(
        UUID companyId,
        ExpenseStatus status,
        Pageable pageable) {

    Page<Expense> expenses = expenseRepository
        .findByCompanyIdAndStatus(
            companyId, status, pageable);

    return PagedResponse.from(expenses,
        ExpenseResponse::from);
}
```

---

## Summary — Quick Reference

```
TOPIC                    WHAT WE DO
─────────────────────────────────────────────────────────────
Basic transactions       @Transactional on service methods
                         (not controllers, not repositories)

Self-call trap           Extract to separate bean if 
                         different propagation needed

Propagation
  REQUIRED               Default — join or start transaction
  REQUIRES_NEW           Audit logs — survive outer rollback
  NOT_SUPPORTED          Read-only reporting queries

Isolation
  READ_COMMITTED         Default — most operations
  REPEATABLE_READ        Concurrent approval scenarios

Locking
  Optimistic (@Version)  Default — expense/invoice updates
  Pessimistic (FOR UPDATE) Payment run creation

@Async + @Transactional  Use @TransactionalEventListener
                         (AFTER_COMMIT) — never call async
                         inside an uncommitted transaction

Outbox + transactions    Outbox write in SAME transaction
                         as business update — always together

readOnly = true          All GET/query methods

Common mistakes fixed:
  N+1 queries            JOIN FETCH in JPQL
  Private @Transactional Move to public method
  Swallowed exceptions   Don't catch inside @Transactional
                         or use setRollbackOnly()
```

---

Step 8 complete. This is saved as **Transaction Handling.md** in **Module 9: Transaction Handling**.

**Next is Step 9: CI/CD & DevOps** — covering GitHub Actions pipeline, environment strategy (dev/staging/prod), Docker build and deploy to AWS ECS, branch strategy, and how our team ships code safely. Ready?