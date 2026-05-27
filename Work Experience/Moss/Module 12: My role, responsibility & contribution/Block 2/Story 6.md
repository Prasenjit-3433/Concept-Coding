# Story 6: The `@Transactional` Private Method Mistake

---

## Context — Why This Mistake Happened When It Did

```
Month 6. Two months into Block 2.

You had just shipped two significant 
features — the dashboard query 
(with the N+1 fix) and the multi-level 
approval workflow.

Confidence was up.
Elena's PR comments were fewer.
You were asking fewer questions 
before starting tickets.
You were moving faster.

And that's exactly when this 
kind of mistake happens.

Not in month 1 when you were 
reading every line carefully,
second-guessing every decision,
asking before acting.

In month 6 when you felt 
like you knew what you were doing
and started making assumptions.
```

This story is not dramatic. No production incident. No data corruption. No customer impact. Elena caught it in code review before it reached staging.

But it's one of the most important stories in your 18 months — because the lesson it taught changed how you thought about frameworks permanently.

---

## The Situation

Sprint planning, month 6. You were assigned a refactoring ticket:

```
Ticket: EXP-248
Title:  Refactor Expense Audit Logging 
        to Use Dedicated Service

Background:
────────────
Currently, audit log entries are 
written inline inside ExpenseService 
methods. This means:
  - Audit logic is scattered across 
    multiple service methods
  - Hard to change audit format 
    without touching core business logic
  - No way to test audit logging 
    independently

Goal:
──────
Extract audit logging into a dedicated 
ExpenseAuditService.
All methods that write audit logs 
should delegate to this service.

Story points: 3
```

Clean refactoring ticket. Three story points. You had done refactoring before. You understood the goal — take scattered inline code and centralize it.

This felt straightforward.

---

## What You Built

You started with the audit log writes that were scattered in `ExpenseService`. Here is one example — the approval method:

```java
// BEFORE — audit log written inline
@Transactional
public ExpenseResponse processApprovalAction(
        UUID expenseId,
        UUID approverId,
        ApprovalAction action,
        String comment) {

    Expense expense = expenseRepository
        .findById(expenseId).orElseThrow();

    // ... approval logic ...

    expense.setStatus(ExpenseStatus.APPROVED);
    expenseRepository.save(expense);

    // Audit log written INLINE — scattered
    ExpenseAuditLog auditLog = ExpenseAuditLog.builder()
        .expenseId(expenseId)
        .action("APPROVED")
        .performedById(approverId)
        .oldStatus(ExpenseStatus.PENDING_APPROVAL)
        .newStatus(ExpenseStatus.APPROVED)
        .createdAt(Instant.now())
        .build();
    expenseAuditLogRepository.save(auditLog);

    return ExpenseResponse.from(expense);
}
```

You created `ExpenseAuditService` and moved all audit log writing into it. So far, correct.

Then you decided to add a method for writing audit logs for the outbox poller — a separate concern that needed its own audit behavior. And this is where the mistake happened:

```java
@Service
public class ExpenseAuditService {

    private final ExpenseAuditLogRepository 
        auditLogRepository;

    // Public method — called from ExpenseService
    // This works correctly
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logApprovalAction(
            UUID expenseId,
            UUID approverId,
            ApprovalAction action,
            Integer stepOrder) {

        ExpenseAuditLog log = ExpenseAuditLog.builder()
            .expenseId(expenseId)
            .action(action.name())
            .performedById(approverId)
            .metadata(Map.of("stepOrder", stepOrder))
            .createdAt(Instant.now())
            .build();

        auditLogRepository.save(log);
    }

    // ❌ THE MISTAKE
    // Private helper method with @Transactional
    // Used internally by multiple public methods
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void writeAuditEntry(
            UUID expenseId,
            String action,
            UUID performedById,
            ExpenseStatus oldStatus,
            ExpenseStatus newStatus) {

        ExpenseAuditLog log = ExpenseAuditLog.builder()
            .expenseId(expenseId)
            .action(action)
            .performedById(performedById)
            .oldStatus(oldStatus)
            .newStatus(newStatus)
            .createdAt(Instant.now())
            .build();

        auditLogRepository.save(log);
    }

    // Public methods calling the private helper
    public void logExpenseSubmitted(
            UUID expenseId, UUID employeeId) {
        // Calls private method internally
        writeAuditEntry(
            expenseId, "SUBMITTED", 
            employeeId, 
            ExpenseStatus.DRAFT,
            ExpenseStatus.PENDING_APPROVAL
        );
    }

    public void logExpenseRejected(
            UUID expenseId, UUID rejectedById,
            ExpenseStatus previousStatus) {
        writeAuditEntry(
            expenseId, "REJECTED",
            rejectedById,
            previousStatus,
            ExpenseStatus.REJECTED
        );
    }
}
```

Your logic was: the private method does the actual writing with `REQUIRES_NEW`, the public methods just call it with the right parameters. Reuse the transaction behavior from one place.

The code compiled. The unit tests passed. All tests were green.

You opened the PR.

---

## The Review — Elena's Comment

Elena reviewed the PR the next morning. One comment. Direct:

```
Elena's PR comment:
────────────────────
"The @Transactional on writeAuditEntry() 
 will never execute.
 
 Spring's @Transactional works through 
 AOP proxying — Spring wraps your bean 
 in a proxy object. When external code 
 calls a method on the proxy, the proxy 
 intercepts the call and applies the 
 transaction behavior.
 
 But when a method inside the same class 
 calls another method in the same class 
 (like your public logExpenseSubmitted() 
 calling private writeAuditEntry()),
 the call goes directly to the real 
 object — it bypasses the proxy entirely.
 
 Result: the @Transactional annotation 
 on writeAuditEntry() is silently ignored.
 
 The method runs in whatever transaction 
 context already existed — not in a 
 new REQUIRES_NEW transaction.
 
 So if logExpenseSubmitted() is called 
 from inside a @Transactional method 
 in ExpenseService, writeAuditEntry() 
 joins that outer transaction 
 instead of running independently.
 
 If that outer transaction rolls back,
 your audit log rolls back with it —
 which defeats the entire purpose 
 of using REQUIRES_NEW.
 
 The annotation compiles and 
 tests pass because the tests 
 don't exercise the transaction 
 boundary behavior.
 
 Fix: move writeAuditEntry() to a 
 separate bean, or make all audit 
 write methods public directly 
 without the private helper."
```

You read this twice.

Then you went and looked at your unit test for `writeAuditEntry()`:

```java
// Your unit test — why it passed
@ExtendWith(MockitoExtension.class)
class ExpenseAuditServiceTest {

    @Mock
    private ExpenseAuditLogRepository 
        auditLogRepository;

    @InjectMocks
    private ExpenseAuditService auditService;

    @Test
    void logExpenseSubmitted_shouldWriteAuditEntry() {

        UUID expenseId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        // This test just calls the method directly
        // on the real object (no proxy involved)
        // and verifies the repository was called
        auditService.logExpenseSubmitted(
            expenseId, employeeId);

        verify(auditLogRepository).save(
            any(ExpenseAuditLog.class));
    }
}
```

```
Why the test passed — and why 
that's the trap:
──────────────────────────────────

In unit tests with @ExtendWith(MockitoExtension.class):
  - You get the REAL object, not a proxy
  - @InjectMocks creates ExpenseAuditService directly
  - No Spring context, no AOP, no proxying
  - @Transactional annotations are completely invisible

So your test called logExpenseSubmitted()
on the real object.
It called writeAuditEntry() internally.
The repository was called.
Test passed.

But in production:
  Spring wraps ExpenseAuditService in a proxy.
  When ExpenseService calls 
  auditService.logExpenseSubmitted(),
  it hits the PROXY.
  The proxy intercepts the call 
  and manages the transaction.
  
  But when logExpenseSubmitted() 
  internally calls writeAuditEntry(),
  it calls it on THE REAL OBJECT —
  not the proxy.
  The proxy has been bypassed.
  @Transactional on writeAuditEntry() = ignored.
```

---

## Understanding the Proxy — What Elena Explained

You asked Elena in your next tech sync to explain the proxy mechanism in more depth. She drew it out:

```
HOW SPRING @Transactional ACTUALLY WORKS:
───────────────────────────────────────────

What you THINK the object graph looks like:
────────────────────────────────────────────
ExpenseService ──calls──▶ ExpenseAuditService
                          (your real class)

What Spring ACTUALLY creates:
───────────────────────────────
ExpenseService ──calls──▶ [Spring Proxy]
                               │
                               │ proxy intercepts call
                               │ starts transaction
                               ▼
                          ExpenseAuditService
                          (your real class)
                               │
                               │ internal call 
                               │ this.writeAuditEntry()
                               │
                               ▼
                          writeAuditEntry()
                          (called on real object,
                           proxy bypassed,
                           @Transactional ignored)

The proxy wraps the BEAN.
It intercepts calls that come FROM OUTSIDE.
It cannot intercept calls that happen 
INSIDE the same object.

This is fundamental to how Java works —
not a Spring limitation.
The proxy IS a different object.
But this in Java always refers to 
the current object, not the proxy.
The proxy never sees internal calls.
```

```java
// Elena's illustration of what happens 
// at the bytecode level

// What you wrote:
public void logExpenseSubmitted(...) {
    writeAuditEntry(...);
    // In Java, this is equivalent to:
    // this.writeAuditEntry(...)
    // "this" = the real ExpenseAuditService object
    // NOT the proxy that wraps it
}

// The proxy Spring creates looks like:
public class ExpenseAuditServiceProxy 
        extends ExpenseAuditService {

    @Override
    public void logExpenseSubmitted(...) {
        // Start transaction
        TransactionManager.begin();
        try {
            // Call the REAL method 
            // on the REAL object
            super.logExpenseSubmitted(...);
            TransactionManager.commit();
        } catch (Exception e) {
            TransactionManager.rollback();
            throw e;
        }
    }

    // writeAuditEntry is private —
    // proxy can't even override it.
    // Even if it were public,
    // super.logExpenseSubmitted() 
    // would call the real object's version,
    // which calls this.writeAuditEntry()
    // on the real object — not the proxy.
}
```

This clicked for you in a way that just reading documentation never had.

```
The mental model that stuck:
──────────────────────────────
Spring's @Transactional is not 
a property of your method.
It is a behavior applied by 
an external interceptor.

That interceptor only activates 
when the call comes from OUTSIDE 
your class — because the proxy 
only wraps external calls.

If you call your own methods internally,
you're calling the real object.
The interceptor is never in the picture.
```

---

## The Fix

You had two options. Elena had mentioned both in her comment.

**Option A — Remove the private helper, make each method self-contained:**

```java
@Service
@RequiredArgsConstructor
public class ExpenseAuditService {

    private final ExpenseAuditLogRepository 
        auditLogRepository;

    // Each public method is fully self-contained
    // @Transactional is on the public method —
    // proxy CAN intercept this
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logExpenseSubmitted(
            UUID expenseId, UUID employeeId) {

        auditLogRepository.save(
            ExpenseAuditLog.builder()
                .expenseId(expenseId)
                .action("SUBMITTED")
                .performedById(employeeId)
                .oldStatus(ExpenseStatus.DRAFT)
                .newStatus(ExpenseStatus.PENDING_APPROVAL)
                .createdAt(Instant.now())
                .build()
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logExpenseRejected(
            UUID expenseId, UUID rejectedById,
            ExpenseStatus previousStatus) {

        auditLogRepository.save(
            ExpenseAuditLog.builder()
                .expenseId(expenseId)
                .action("REJECTED")
                .performedById(rejectedById)
                .oldStatus(previousStatus)
                .newStatus(ExpenseStatus.REJECTED)
                .createdAt(Instant.now())
                .build()
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logApprovalAction(
            UUID expenseId, UUID approverId,
            ApprovalAction action,
            Integer stepOrder) {

        auditLogRepository.save(
            ExpenseAuditLog.builder()
                .expenseId(expenseId)
                .action(action.name())
                .performedById(approverId)
                .metadata(Map.of("stepOrder", stepOrder))
                .createdAt(Instant.now())
                .build()
        );
    }
}
```

**Option B — Extract the private helper into a separate Spring bean:**

```java
// Separate bean — Spring manages its lifecycle
// Calls from ExpenseAuditService to 
// AuditEntryWriter go through the proxy
@Component
@RequiredArgsConstructor
class AuditEntryWriter {

    private final ExpenseAuditLogRepository 
        auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(ExpenseAuditLog entry) {
        auditLogRepository.save(entry);
    }
}

@Service
@RequiredArgsConstructor
public class ExpenseAuditService {

    private final AuditEntryWriter auditEntryWriter;
    // External bean — calls go through proxy ✅

    public void logExpenseSubmitted(
            UUID expenseId, UUID employeeId) {

        auditEntryWriter.write(
            ExpenseAuditLog.builder()
                .expenseId(expenseId)
                .action("SUBMITTED")
                .performedById(employeeId)
                .oldStatus(ExpenseStatus.DRAFT)
                .newStatus(ExpenseStatus.PENDING_APPROVAL)
                .createdAt(Instant.now())
                .build()
        );
    }

    // ... other methods calling auditEntryWriter.write()
}
```

You asked Elena which option she preferred:

```
Elena:
───────
"Option A for this case.
 Option B adds a bean just to 
 avoid code duplication — 
 that's not a good enough reason 
 to introduce a new class.
 
 The duplication in Option A is 
 building the log entry object.
 You can reduce that with a 
 private builder helper that 
 returns an ExpenseAuditLog — 
 not a @Transactional method,
 just a pure data construction helper.
 The @Transactional stays on 
 the public methods where it works.
 
 Option B would be correct if 
 AuditEntryWriter had its own 
 meaningful logic beyond just saving.
 Right now it doesn't."
```

You implemented Option A with a private builder helper for the log object:

```java
@Service
@RequiredArgsConstructor
public class ExpenseAuditService {

    private final ExpenseAuditLogRepository 
        auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logExpenseSubmitted(
            UUID expenseId, UUID employeeId) {

        auditLogRepository.save(
            buildEntry(expenseId, "SUBMITTED",
                employeeId,
                ExpenseStatus.DRAFT,
                ExpenseStatus.PENDING_APPROVAL,
                null)
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logExpenseRejected(
            UUID expenseId, UUID rejectedById,
            ExpenseStatus previousStatus) {

        auditLogRepository.save(
            buildEntry(expenseId, "REJECTED",
                rejectedById,
                previousStatus,
                ExpenseStatus.REJECTED,
                null)
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logApprovalAction(
            UUID expenseId, UUID approverId,
            ApprovalAction action,
            Integer stepOrder) {

        auditLogRepository.save(
            buildEntry(expenseId, action.name(),
                approverId,
                null, null,
                Map.of("stepOrder", stepOrder))
        );
    }

    // Private helper — builds the log object ONLY
    // No @Transactional here — no transaction needed
    // Pure data construction, no side effects
    private ExpenseAuditLog buildEntry(
            UUID expenseId,
            String action,
            UUID performedById,
            ExpenseStatus oldStatus,
            ExpenseStatus newStatus,
            Map<String, Object> metadata) {

        return ExpenseAuditLog.builder()
            .expenseId(expenseId)
            .action(action)
            .performedById(performedById)
            .oldStatus(oldStatus)
            .newStatus(newStatus)
            .metadata(metadata)
            .createdAt(Instant.now())
            .build();
    }
}
```

PR updated. Elena approved it.

---

## What Happened After — The SonarQube Rule

After this PR merged, you raised it in the team Slack channel:

```
You in #expense-ap-dev:
────────────────────────
"Caught something in my last PR 
 that's worth sharing — I had 
 @Transactional on a private method.
 
 The annotation silently does nothing 
 on private methods because of how 
 Spring's AOP proxy works — 
 the proxy can only intercept 
 public method calls from outside 
 the bean.
 
 Unit tests pass because Mockito 
 uses the real object without a proxy.
 You only see the problem at runtime 
 when Spring is managing the bean.
 
 Is there a SonarQube rule we can 
 add to catch this?"
```

Arjun replied:

```
Arjun:
───────
"There is actually — 
 SonarQube rule java:S3753.
 It flags @Transactional on 
 private methods as a bug.
 
 But it might not be in our 
 active ruleset.
 I'll check and enable it."
```

Arjun enabled the rule in the SonarQube configuration. Two days later it flagged one other instance of the same pattern — in a different method in `InvoiceService`, written by Tomás six months earlier.

```
Two things happened from this:
────────────────────────────────
1. A real bug in InvoiceService 
   was found and fixed — 
   a @Transactional private method 
   that had been silently doing 
   nothing for six months.

2. The SonarQube rule now prevents 
   this class of mistake permanently —
   for you, for Tomás, for Kemal,
   for Léa, for anyone who joins 
   the team in the future.

Your mistake → fixed your code.
Your raising it → fixed someone 
                  else's code too.
Your follow-up → prevents it 
                 from happening again.

That's the full loop.
Not just learning from a mistake —
making the system better because 
of what you learned.
```

---

## The Integration Test — What Would Have Caught This Earlier

After the SonarQube rule was added, you asked Elena a question in your next tech sync:

```
You:
─────
"Is there a way to write a test 
 that would have caught this 
 before code review?
 
 My unit tests passed because 
 Mockito doesn't use proxies.
 The bug only shows up in the 
 real Spring context.
 How do you test transaction 
 behavior correctly?"
```

Elena:

```
Elena:
───────
"You need a Spring integration test —
 @SpringBootTest or @DataJpaTest.
 
 These tests start a real (or partial)
 Spring application context.
 Spring creates the proxy.
 @Transactional actually applies.
 
 The specific thing you'd test:
 
 Scenario: outer method throws an exception.
 Expected: REQUIRES_NEW method committed 
           its data despite the rollback.
 
 If @Transactional on the private method 
 was working correctly:
   → audit log is written 
     even when outer tx rolls back
 
 If it was silently ignored:
   → audit log is rolled back 
     along with the outer tx
 
 You can assert which one happened 
 by checking the DB state 
 after the rollback."
```

You wrote this test after the story — as a habit to add for any future audit logging work:

```java
// Integration test that catches 
// transaction boundary behavior
@SpringBootTest
@Testcontainers
class ExpenseAuditServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("expense_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(
            DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
            postgres::getJdbcUrl);
        registry.add("spring.datasource.username",
            postgres::getUsername);
        registry.add("spring.datasource.password",
            postgres::getPassword);
    }

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private ExpenseAuditLogRepository auditLogRepository;

    @Test
    void auditLog_shouldBePersisted_evenWhenOuterTransactionRollsBack() {

        UUID expenseId = UUID.randomUUID();
        UUID approverId = UUID.randomUUID();

        // Trigger approval that will fail
        // AFTER the audit log is written
        // (simulate a DB error on expense save)
        assertThrows(RuntimeException.class, () ->
            expenseService
                .processApprovalActionWithForcedRollback(
                    expenseId, approverId
                )
        );

        // If REQUIRES_NEW is working:
        // audit log committed independently
        // before the outer tx rolled back
        List<ExpenseAuditLog> logs = 
            auditLogRepository
                .findByExpenseId(expenseId);

        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getAction())
            .isEqualTo("APPROVED");

        // If REQUIRES_NEW was silently ignored:
        // audit log rolled back with outer tx
        // assertThat(logs).isEmpty() — this 
        // would have been the result before the fix
    }
}
```

```
Why this test matters:
───────────────────────
It tests behavior, not just code.
It verifies that the transaction 
boundary actually works as intended —
not just that the method runs 
without throwing an exception.

Unit tests with Mockito verify 
logic and flow.
Integration tests with Testcontainers 
verify transaction boundaries, 
SQL correctness, and framework behavior.

Both are necessary.
They test different things.
```

---

## The Result

```
What shipped:
──────────────
Refactored ExpenseAuditService 
with correct @Transactional behavior.
Audit logs now genuinely independent 
of the outer transaction.

Side effects:
──────────────
SonarQube rule java:S3753 enabled —
  prevents @Transactional on 
  private methods across the 
  entire codebase permanently.

Found and fixed an existing instance 
of the same pattern in InvoiceService 
(Tomás's code from 6 months earlier).

Integration test pattern established 
for testing transaction boundaries —
  used in future audit-related features.

What you learned:
──────────────────
1. @Transactional works through AOP proxying.
   The proxy intercepts EXTERNAL calls.
   Internal calls bypass the proxy.
   Private methods are never interceptable.

2. Unit tests with Mockito don't test 
   transaction behavior —
   they use the real object, not the proxy.
   For transaction boundary testing,
   you need a Spring context 
   (integration test).

3. "The tests pass" is not the same as 
   "the code is correct."
   Some behaviors are invisible to 
   unit tests but visible at runtime.

4. When you find a mistake, 
   raise it — don't just fix it silently.
   Someone else almost certainly 
   has the same mistake somewhere.
   Arjun found one in InvoiceService 
   because you raised it.

PR review count trend:
───────────────────────
Story 1 (month 2):   8 comments
Story 5 (month 5):   4 comments
This story (month 6): 1 comment
  (Elena found the bug,
   but the rest of the 
   implementation was clean)
```

---

## The "Tricky Question" Preparation

---

**Q1: "Explain exactly why @Transactional on a private method doesn't work."**

```
Spring implements @Transactional 
using AOP — Aspect Oriented Programming.

When Spring sees a bean with 
@Transactional methods, it doesn't 
modify your class.
Instead, it creates a proxy object 
that wraps your class.

The proxy intercepts method calls 
and adds transaction behavior:
start a transaction before the method,
commit after it returns,
rollback if it throws.

The key constraint: the proxy only 
intercepts calls that come from 
OUTSIDE your class.

When code outside your class calls 
auditService.logExpenseSubmitted(),
it's calling the proxy.
The proxy intercepts it, 
starts the transaction,
then delegates to your real method.
Transaction works.

But when logExpenseSubmitted() 
internally calls writeAuditEntry(),
it calls this.writeAuditEntry() —
"this" is the real object, not the proxy.
The proxy is completely bypassed.
No interception. No transaction.
@Transactional is ignored silently.

Private methods make this worse 
because the proxy can't even 
override them — private methods 
aren't visible to subclasses,
and the proxy is a subclass.
So even if the call happened to 
go through the proxy somehow,
it couldn't intercept a private method.

The fix: either put @Transactional 
only on public methods that are 
called from outside the class,
or extract the logic that needs 
independent transaction behavior 
into a separate Spring bean.
```

---

**Q2: "Why did your unit tests pass if the code was broken?"**

```
Because unit tests with Mockito 
don't involve Spring at all.

When you use @ExtendWith(MockitoExtension.class) 
and @InjectMocks, Mockito creates 
your class directly — 
new ExpenseAuditService(mockRepository).

This is the real object.
No Spring context.
No proxy.
No AOP.
@Transactional annotations are 
invisible to Mockito.

So when the test called 
auditService.logExpenseSubmitted(),
it called the real method on the real object.
It internally called writeAuditEntry() 
on the real object.
The repository was called.
Test passed.

But this test never verified whether 
REQUIRES_NEW actually ran in a 
separate transaction.
It only verified that the repository 
save was called.

To test transaction boundary behavior,
you need a Spring integration test —
@SpringBootTest or @DataJpaTest —
which starts a real Spring context,
creates real proxies,
and applies @Transactional for real.

Then you can verify:
"if the outer transaction rolls back,
does the REQUIRES_NEW method's 
data still get committed?"

That test would have caught the bug.
I added it after this incident.
```

---

**Q3: "You mentioned Tomás had the same bug in InvoiceService. How did you feel about raising that publicly?"**

```
Honestly I thought about it 
before sending the Slack message.

Tomás had been at the company 
for 10 months at that point —
longer than me.
He was mid-level.
I didn't want it to seem like 
I was pointing out his mistake 
publicly to make myself look good.

But the way I framed it mattered.
I didn't say "I found a bug 
and I think Tomás might have 
the same issue."
I described the pattern in general —
what it looks like, why it fails,
why tests don't catch it.
And I asked if we could add a 
SonarQube rule to prevent it.

Arjun ran the rule across the codebase.
It found Tomás's instance automatically.
Nobody was singled out.
Tomás fixed it, no drama.

The goal was never to flag Tomás.
The goal was to prevent the pattern 
from existing in the codebase.
The SonarQube rule achieved that 
more permanently than any 
manual call-out would have.

That's how I think about it now —
if you find a class of mistake,
fix the system that allowed the mistake,
not the person who made it.
```

---

**Q4: "Is there ever a valid reason to put @Transactional on a private method?"**

```
No.

It compiles. It doesn't throw an error.
But it has zero effect.
It's dead code — an annotation 
that signals intent but does nothing.

If you find @Transactional on a 
private method, it means one of 
two things:

Either the developer didn't understand 
how proxying works and thought 
the annotation would apply —
in which case it's a bug because 
the transaction behavior they 
intended isn't happening.

Or the developer added it as 
documentation — to communicate 
"this method should run in a 
new transaction" —
but the actual transaction control 
is happening elsewhere.
That's confusing and should be 
replaced with a comment instead.

Either way, SonarQube rule java:S3753 
is correct to flag it.
There's no valid reason to keep it.
```

---

Story 6 complete.

```
What Stories 4, 5, and 6 together 
show about Block 2:
─────────────────────────────────────

Story 4 (N+1 bug):
  A runtime performance mistake 
  visible only in production data.
  Fixed with SQL knowledge.
  Measured with Datadog.

Story 5 (multi-level approval):
  First genuinely complex feature 
  owned end-to-end.
  Design before code.
  Concurrency edge case caught in review.
  Handled an unexpected product question.

Story 6 (@Transactional private method):
  A subtle framework mistake 
  invisible to unit tests.
  Fixed with deeper understanding 
  of how Spring actually works.
  Raised it → fixed someone else's 
  code too → prevented it permanently.

The pattern across all three:
  You're not making the same 
  mistakes twice.
  Each mistake is different.
  Each teaches something fundamental.
  And each time, you go beyond 
  just fixing your own code.
```

Ready for Story 7 — pushing back on the PM request. Shall I begin?