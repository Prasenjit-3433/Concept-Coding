# Story 5: Multi-Level Approval Feature — First End-to-End Feature Ownership

---

## Context — Where You Were at Month 4-5

```
Something important had changed 
by month 4.

Month 1-3: Every ticket was a piece 
            of someone else's design.
            You implemented. 
            Elena or Arjun had 
            already thought through 
            the approach.
            You executed.

Month 4-5: The tickets were getting larger.
────────────────────────────────────────────
EXP-219 (the dashboard feature) 
was the first sign.
5 story points. 
Elena gave guidance upfront 
but didn't design it for you.
You made an architectural choice 
(the wrong one initially),
saw the consequence,
and fixed it.

That's a different kind of work.
That's ownership starting to emerge.

Then came EXP-231.
```

---

## The Situation

Sprint planning, month 5. Lukas presented the next sprint's tickets. One of them was larger than anything you had been assigned before:

```
Ticket: EXP-231
Title:  Multi-Level Approval Workflow 
        for High-Value Expenses

Background:
────────────
Currently, ALL expenses go through 
a single-level approval — 
the employee's direct manager 
approves, done.

Finance teams at our larger customers 
have started requesting a second level:
for expenses above €2,000, 
the Finance Manager should also 
approve after the direct manager.

This is a compliance requirement 
for some of our enterprise customers.

Acceptance Criteria:
─────────────────────
1. Approval policy in User & Org Service 
   defines thresholds per company:
     - Amount < €50:    no approval needed
     - €50 - €2,000:   manager approval only
     - > €2,000:        manager → then finance 
                        manager (two steps)

2. When expense is submitted,
   approval steps are created based 
   on the policy and the amount

3. Approval steps must be completed 
   in order — step 2 cannot be 
   completed before step 1

4. Expense status should reflect 
   where in the workflow it is:
   PENDING_APPROVAL (waiting on 
   current step)

5. Finance dashboard shows which 
   step each expense is at

6. If any approver rejects, 
   the whole expense is rejected —
   no further steps run

7. Full audit trail of each step

Story points: 8
(Largest ticket assigned to you so far)
```

Eight story points. The previous largest had been five.

Lukas said in the planning meeting:

```
Lukas in sprint planning:
──────────────────────────
"This one touches the approval 
 workflow pretty deeply.
 [Your name] — I want to assign 
 this to you. 
 
 Before you start writing code, 
 I'd like you to spend a day 
 thinking through the design 
 and then walk Elena through it.
 
 Not asking you to design it 
 perfectly — asking you to think 
 it through first, then get 
 Elena's input.
 
 That's how we should be working 
 at this point."
```

This was different from before. Lukas wasn't handing you a design. He was asking you to come up with one and then validate it. That's a meaningful shift in expectation.

---

## Your Task

```
What you owned end-to-end:
───────────────────────────
1. Design the approval step creation 
   logic (how to determine steps 
   based on policy + amount)

2. DB schema changes 
   (approval_steps already existed,
    but needed changes)

3. Service layer — approval routing 
   logic, step ordering enforcement,
   state transitions

4. API changes — new response fields,
   status endpoint updates

5. Flyway migration

6. Unit tests + integration tests

7. PR — write, address review, merge

What you did NOT own:
──────────────────────
The approval policy data structure 
itself — that lived in User & Org Service.
You consumed it via FeignClient.
Sophie had already mapped out 
the response format in a previous 
design session.
```

---

## Day 1 — Thinking It Through Before Writing Code

You spent the first day not touching the keyboard for code. Instead, you drew out the problem.

This was something Elena had said in a tech sync earlier that month:

```
Elena (from a previous tech sync):
────────────────────────────────────
"Before you write any code for 
 a feature that touches workflow 
 or state — draw the state machine.
 
 Every expense status transition 
 should be on paper first.
 If you can't draw it clearly,
 you don't understand it clearly.
 And if you don't understand it,
 your code will have gaps."
```

So you drew the state machine first:

```
EXPENSE STATUS STATE MACHINE
(with multi-level approval)
─────────────────────────────────────────────────────

         Submit
DRAFT ──────────────▶ PENDING_APPROVAL
                            │
                     ┌──────┴──────┐
                     │             │
              Step 1 pending  All steps done
                     │             │
                     │    Approved ▼
              ┌──────┘      APPROVED ──────▶ (payment)
              │
              ▼ Approver acts on Step 1
         ┌────────────────────────┐
         │                        │
    Approved                  Rejected
         │                        │
         ▼                        ▼
  Step 2 pending?           REJECTED
         │
    ┌────┴────┐
    │         │
   Yes        No
    │         │
    ▼         ▼
PENDING_   APPROVED
APPROVAL   (only 1 step
(waiting   needed)
on step 2)
    │
    ▼ Approver acts on Step 2
┌────────────────────────┐
│                        │
Approved             Rejected
│                        │
▼                        ▼
APPROVED             REJECTED
```

Then you drew the approval step creation logic:

```
APPROVAL STEP CREATION
(when expense is submitted)
────────────────────────────────────────

Input:
  expense.amount = €2,500
  expense.employeeId = emp-123
  company approval policy = {
    rules: [
      { maxAmount: 50,    approverRole: NONE },
      { maxAmount: 2000,  approverRole: MANAGER },
      { maxAmount: null,  approverRole: FINANCE_MANAGER,
                          requiresPrevious: true }
    ]
  }

Logic:
  1. Find matching rule(s) for €2,500
     → rule 2 matches (> 2000, no max)
     → rule 2 requires manager first (step 1)
     → then finance manager (step 2)

  2. Look up manager for emp-123
     → FeignClient to User & Org Service
     → returns manager: emp-456 (Sarah Chen)

  3. Look up finance manager for company
     → FeignClient to User & Org Service
     → returns finance manager: emp-789 (Lisa Wang)

  4. Create approval steps:
     Step 1: { order: 1, approverId: emp-456, 
               action: PENDING }
     Step 2: { order: 2, approverId: emp-789,
               action: PENDING }

Output:
  expense.status = PENDING_APPROVAL
  approval_steps: [step1, step2]
  (step 2 is pending but locked 
   until step 1 completes)
```

The next morning you walked Elena through this in your weekly tech sync. You shared your screen, showed the diagrams, and talked through the logic.

```
Elena's feedback:
──────────────────
"Good start. Two things to think about.

 First: what happens if an employee 
 submits an expense and their 
 manager is the finance manager?
 Same person would need to approve twice.
 Should the system merge those steps 
 or create duplicate steps?

 Second: the approval_steps table 
 already has a UNIQUE constraint 
 on (expense_id, step_order).
 Your Flyway migration needs to 
 check if any existing data 
 conflicts before you add 
 new columns.
 
 The rest of the design looks solid.
 Start writing."
```

```
What you did with Elena's feedback:
──────────────────────────────────────
First point (same person, two steps):
  You added a deduplication check 
  in the approval step creation logic.
  If the manager and finance manager 
  resolve to the same person,
  create only one step (the higher one).
  You confirmed this decision with 
  Lukas as a product question 
  before implementing.
  Lukas: "Yes — one approval is enough 
           if they're the same person."

Second point (migration safety):
  You ran a SELECT on the staging DB 
  before writing the migration to 
  check if any existing approval_steps 
  rows would conflict with schema changes.
  None did — the table was relatively new.
  But the habit of checking before 
  migrating was something you 
  carried forward.
```

---

## The Implementation

### Step 1 — DB Schema Changes (Flyway Migration)

The existing `approval_steps` table needed two new columns and one new index:

```sql
-- V7__update_approval_steps_for_multi_level.sql

-- Add step_type to distinguish 
-- what kind of approver this step needs
ALTER TABLE approval_steps
ADD COLUMN approver_role VARCHAR(50);

-- Track when the step becomes active
-- (i.e., when the previous step completed)
ALTER TABLE approval_steps
ADD COLUMN activated_at TIMESTAMPTZ;

-- Index for approver dashboard query:
-- "Show me all expenses waiting for MY approval"
-- This query runs on every dashboard load
-- for every finance manager
CREATE INDEX idx_approval_steps_active_approver
    ON approval_steps(approver_id, action)
    WHERE action = 'PENDING';
-- Partial index — only indexes PENDING rows
-- APPROVED/REJECTED rows rarely queried
-- Keeps index small and fast
```

```
Why a partial index here:
──────────────────────────
The most common query for approval steps is:
"Show me all steps where I'm the approver 
 AND action = PENDING"

Full index on (approver_id, action):
  Would index ALL rows — 
  including APPROVED and REJECTED,
  which are historical and rarely queried.
  Index grows forever.

Partial index WHERE action = 'PENDING':
  Only indexes active, pending steps.
  As steps are resolved, they 
  fall out of the index automatically.
  Index stays small regardless of 
  how many historical approvals exist.
  
This is something Finn pointed out 
when he reviewed the migration.
He was good at this kind of thing.
```

---

### Step 2 — The ApprovalStep Entity

```java
@Entity
@Table(name = "approval_steps",
    uniqueConstraints = {
        @UniqueConstraint(
            columnNames = {"expense_id", "step_order"}
        )
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalStep {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "expense_id", nullable = false)
    private UUID expenseId;

    // 1 = first approver, 2 = second, etc.
    // Steps must complete in order
    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(name = "approver_id", nullable = false)
    private UUID approverId;

    // What role this approver holds
    // Stored at step creation time —
    // role might change later, 
    // but we record what it was
    // at the time of the approval request
    @Column(name = "approver_role")
    private String approverRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalAction action;

    private String comment;

    @Column(name = "acted_at")
    private Instant actedAt;

    // When this step became the active step
    // null = not yet active (waiting for 
    //        previous step to complete)
    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

public enum ApprovalAction {
    PENDING,
    APPROVED,
    REJECTED,
    DELEGATED  // future use
}
```

---

### Step 3 — The Approval Routing Service

This was the most complex piece. The logic that determined how many approval steps to create and who the approvers were.

```java
@Service
@RequiredArgsConstructor
public class ApprovalRoutingService {

    private final UserOrgFeignClient userOrgFeignClient;

    /**
     * Determines approval steps for an expense
     * based on the company's approval policy 
     * and the expense amount.
     *
     * Returns an ordered list of steps to create.
     * Caller is responsible for persisting them.
     */
    public List<ApprovalStepDefinition> 
            determineApprovalSteps(
            UUID companyId,
            UUID employeeId,
            BigDecimal amount) {

        // Fetch approval policy
        // (will be cached in month 10 — 
        //  for now it's a direct FeignClient call)
        ApprovalPolicy policy = userOrgFeignClient
            .getApprovalPolicy(companyId);

        // Find which rules apply for this amount
        List<ApprovalRule> matchingRules = 
            policy.getRules().stream()
                .filter(rule -> 
                    amountMatchesRule(amount, rule))
                .sorted(Comparator.comparingInt(
                    ApprovalRule::getStepOrder))
                .collect(Collectors.toList());

        // No rules match → no approval needed
        // (amount below minimum threshold)
        if (matchingRules.isEmpty()) {
            return Collections.emptyList();
        }

        List<ApprovalStepDefinition> steps = 
            new ArrayList<>();
        Set<UUID> assignedApprovers = new HashSet<>();
        // Track to prevent duplicate approvers

        for (ApprovalRule rule : matchingRules) {

            UUID approverId = resolveApprover(
                rule.getApproverRole(),
                employeeId,
                companyId
            );

            if (approverId == null) {
                // No one with this role found
                // Skip this step — 
                // log a warning, don't fail
                log.warn("No approver found for " +
                    "role {} in company {}. " +
                    "Skipping step.",
                    rule.getApproverRole(), companyId);
                continue;
            }

            // Deduplication: if this approver 
            // already has a step, skip
            // (manager IS the finance manager case)
            if (assignedApprovers.contains(approverId)) {
                log.info("Approver {} already assigned" +
                    " to a step. Skipping duplicate.",
                    approverId);
                continue;
            }

            assignedApprovers.add(approverId);
            steps.add(ApprovalStepDefinition.builder()
                .stepOrder(steps.size() + 1)
                .approverId(approverId)
                .approverRole(
                    rule.getApproverRole().name())
                .build());
        }

        return steps;
    }

    /**
     * Resolves the actual employee ID 
     * for a given role.
     */
    private UUID resolveApprover(
            ApproverRole role,
            UUID employeeId,
            UUID companyId) {

        return switch (role) {
            case MANAGER -> userOrgFeignClient
                .getManager(employeeId)
                .map(EmployeeDto::getId)
                .orElse(null);

            case FINANCE_MANAGER -> userOrgFeignClient
                .getFinanceManager(companyId)
                .map(EmployeeDto::getId)
                .orElse(null);

            case SELF -> employeeId;

            default -> null;
        };
    }

    /**
     * Checks if an amount falls within 
     * a rule's range.
     */
    private boolean amountMatchesRule(
            BigDecimal amount, ApprovalRule rule) {

        boolean aboveMin = rule.getMinAmount() == null
            || amount.compareTo(
                rule.getMinAmount()) >= 0;

        boolean belowMax = rule.getMaxAmount() == null
            || amount.compareTo(
                rule.getMaxAmount()) < 0;

        return aboveMin && belowMax;
    }
}
```

---

### Step 4 — The Expense Submission Flow (Updated)

```java
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ApprovalStepRepository 
        approvalStepRepository;
    private final ApprovalRoutingService 
        approvalRoutingService;
    private final OutboxEventRepository 
        outboxEventRepository;

    @Transactional
    public ExpenseResponse submitExpense(
            UUID expenseId, UUID employeeId) {

        Expense expense = expenseRepository
            .findById(expenseId)
            .orElseThrow(() -> 
                new ExpenseNotFoundException(expenseId));

        // Validate state
        if (expense.getStatus() != ExpenseStatus.DRAFT) {
            throw new InvalidExpenseStateException(
                "Expense must be in DRAFT state " +
                "to submit. Current: " + 
                expense.getStatus()
            );
        }

        // Determine approval steps
        List<ApprovalStepDefinition> stepDefinitions =
            approvalRoutingService.determineApprovalSteps(
                expense.getCompanyId(),
                employeeId,
                expense.getAmount()
            );

        Instant now = Instant.now();

        if (stepDefinitions.isEmpty()) {
            // No approval needed —
            // auto-approve immediately
            // (amount below minimum threshold)
            expense.setStatus(ExpenseStatus.APPROVED);
            expense.setApprovedAt(now);
            log.info("Expense {} auto-approved " +
                "(below approval threshold)",
                expenseId);

        } else {
            // Create approval steps
            List<ApprovalStep> steps = new ArrayList<>();

            for (int i = 0; 
                    i < stepDefinitions.size(); 
                    i++) {

                ApprovalStepDefinition def = 
                    stepDefinitions.get(i);
                boolean isFirstStep = (i == 0);

                ApprovalStep step = ApprovalStep.builder()
                    .expenseId(expenseId)
                    .stepOrder(def.getStepOrder())
                    .approverId(def.getApproverId())
                    .approverRole(def.getApproverRole())
                    .action(ApprovalAction.PENDING)
                    // Only activate the first step
                    // Others wait for previous 
                    // step to complete
                    .activatedAt(
                        isFirstStep ? now : null)
                    .createdAt(now)
                    .build();

                steps.add(step);
            }

            approvalStepRepository.saveAll(steps);

            expense.setStatus(
                ExpenseStatus.PENDING_APPROVAL);
            expense.setSubmittedAt(now);
            expense.setAssignedApproverId(
                stepDefinitions.get(0).getApproverId()
                // First approver is "current" approver
            );
        }

        expenseRepository.save(expense);

        // Outbox event — same transaction
        outboxEventRepository.save(
            OutboxEvent.builder()
                .aggregateType("EXPENSE")
                .aggregateId(expenseId)
                .eventType("expense.submitted")
                .payload(buildSubmitPayload(expense))
                .build()
        );

        return ExpenseResponse.from(expense);
    }
}
```

---

### Step 5 — The Approval Action Flow

This was the most critical method — and the one with the most edge cases.

```java
@Transactional(isolation = Isolation.REPEATABLE_READ)
public ExpenseResponse processApprovalAction(
        UUID expenseId,
        UUID approverId,
        ApprovalAction action,  // APPROVED or REJECTED
        String comment) {

    Expense expense = expenseRepository
        .findById(expenseId)
        .orElseThrow(() -> 
            new ExpenseNotFoundException(expenseId));

    // Must be in PENDING_APPROVAL
    if (expense.getStatus() != 
            ExpenseStatus.PENDING_APPROVAL) {
        throw new InvalidExpenseStateException(
            "Expense is not pending approval. " +
            "Current status: " + expense.getStatus()
        );
    }

    // Find the current active step
    // (step with action=PENDING and 
    //  activatedAt is not null)
    ApprovalStep currentStep = 
        approvalStepRepository
            .findCurrentActiveStep(expenseId)
            .orElseThrow(() -> 
                new IllegalStateException(
                    "No active approval step found " +
                    "for expense " + expenseId
                ));

    // Verify the caller is the assigned approver
    if (!currentStep.getApproverId()
                    .equals(approverId)) {
        throw new UnauthorizedActionException(
            "You are not the assigned approver " +
            "for the current step"
        );
    }

    Instant now = Instant.now();

    // Record the action on this step
    currentStep.setAction(action);
    currentStep.setComment(comment);
    currentStep.setActedAt(now);
    approvalStepRepository.save(currentStep);

    // Write audit log
    // (REQUIRES_NEW — survives outer rollback)
    auditService.logApprovalAction(
        expenseId, approverId, action, 
        currentStep.getStepOrder()
    );

    if (action == ApprovalAction.REJECTED) {
        // Rejection — end the workflow
        expense.setStatus(ExpenseStatus.REJECTED);
        expense.setRejectedAt(now);
        expense.setRejectedById(approverId);

        expenseRepository.save(expense);

        outboxEventRepository.save(
            OutboxEvent.builder()
                .aggregateType("EXPENSE")
                .aggregateId(expenseId)
                .eventType("expense.rejected")
                .payload(buildRejectedPayload(
                    expense, currentStep))
                .build()
        );

    } else {
        // APPROVED — check if there's a next step
        Optional<ApprovalStep> nextStep = 
            approvalStepRepository
                .findNextPendingStep(
                    expenseId, 
                    currentStep.getStepOrder()
                );

        if (nextStep.isPresent()) {
            // Activate the next step
            ApprovalStep next = nextStep.get();
            next.setActivatedAt(now);
            approvalStepRepository.save(next);

            // Update expense's current approver
            expense.setAssignedApproverId(
                next.getApproverId());
            expenseRepository.save(expense);

            // Notify next approver
            outboxEventRepository.save(
                OutboxEvent.builder()
                    .aggregateType("EXPENSE")
                    .aggregateId(expenseId)
                    .eventType("expense.step.completed")
                    .payload(buildStepPayload(
                        expense, next))
                    .build()
            );

        } else {
            // No more steps — fully approved
            expense.setStatus(ExpenseStatus.APPROVED);
            expense.setApprovedAt(now);
            expenseRepository.save(expense);

            outboxEventRepository.save(
                OutboxEvent.builder()
                    .aggregateType("EXPENSE")
                    .aggregateId(expenseId)
                    .eventType("expense.approved")
                    .payload(buildApprovedPayload(
                        expense))
                    .build()
            );
        }
    }

    return ExpenseResponse.from(expense);
}
```

**The repository queries supporting this:**

```java
public interface ApprovalStepRepository 
        extends JpaRepository<ApprovalStep, UUID> {

    // Find the step currently waiting for action
    // activatedAt is not null = it's the active step
    // action = PENDING = not yet acted on
    @Query("""
        SELECT s FROM ApprovalStep s
        WHERE s.expenseId = :expenseId
        AND s.action = 'PENDING'
        AND s.activatedAt IS NOT NULL
        ORDER BY s.stepOrder ASC
        """)
    Optional<ApprovalStep> findCurrentActiveStep(
        @Param("expenseId") UUID expenseId
    );

    // Find the next step after the current one
    @Query("""
        SELECT s FROM ApprovalStep s
        WHERE s.expenseId = :expenseId
        AND s.action = 'PENDING'
        AND s.stepOrder > :currentOrder
        ORDER BY s.stepOrder ASC
        """)
    Optional<ApprovalStep> findNextPendingStep(
        @Param("expenseId") UUID expenseId,
        @Param("currentOrder") Integer currentOrder
    );

    // For the approver's dashboard —
    // all expenses waiting for a specific approver
    @Query("""
        SELECT s FROM ApprovalStep s
        WHERE s.approverId = :approverId
        AND s.action = 'PENDING'
        AND s.activatedAt IS NOT NULL
        """)
    List<ApprovalStep> findActiveStepsForApprover(
        @Param("approverId") UUID approverId
    );
}
```

---

## The PR — And Elena's Most Important Comment

You opened the PR. Elena reviewed it the next day. Four comments total — the fewest you'd received on a significant feature.

Three were minor. The fourth was the most important one:

```
Elena's comment on processApprovalAction():
─────────────────────────────────────────────
"Look at this method:

 @Transactional(isolation = Isolation.REPEATABLE_READ)
 public ExpenseResponse processApprovalAction(...)

 Two concurrent requests for the same expense:
 Request A (manager approves at 10:00:01)
 Request B (same manager double-clicks — 
           sends duplicate approve at 10:00:01)

 Both read the expense — status = PENDING_APPROVAL
 Both find the current active step — step 1
 Both verify the approver is correct
 Both try to set step 1 action = APPROVED

 With REPEATABLE_READ:
 PostgreSQL uses MVCC — 
 Request A commits first.
 Request B then tries to update 
 the same row Request A already updated.
 PostgreSQL detects the conflict and 
 throws a SerializationFailureException.
 We catch this and return 409.

 But there's another case:
 What if Request B reads AFTER 
 Request A has committed 
 (not truly concurrent — just fast)?
 Then Request B sees step 1 
 already APPROVED.
 findCurrentActiveStep() would 
 return step 2 (if it exists).
 Request B would activate step 2 
 as if it was a legitimate 
 second approval.

 How does your code handle this?"
```

You read this three times.

Then you understood what Elena was pointing at. The `findCurrentActiveStep()` query found the step with `action = PENDING AND activatedAt IS NOT NULL`. If step 1 was already approved, that query would return step 2 (if it exists) — and the caller (the manager who was supposed to approve step 1) would accidentally activate step 2.

This was a real bug. And Elena had found it in review before it reached production.

You replied on the PR:

```
Your reply:
────────────
"I see the bug.

 The authorization check verifies:
 currentStep.approverId == caller's approverId

 But findCurrentActiveStep() might 
 return step 2 after step 1 is approved —
 and step 2's approver is the 
 finance manager, not the manager.
 
 So the authorization check would 
 CORRECTLY reject the duplicate 
 manager approval — it would see 
 that step 2's assignedApprover 
 is NOT the manager and throw 
 UnauthorizedException.
 
 But the error message would be confusing:
 'You are not the assigned approver 
 for the current step'
 
 The manager would see a confusing 403 
 instead of understanding 
 'you already approved this'.
 
 The fix: before the authorization check,
 verify that the step the caller 
 is trying to act on is actually 
 a step they WERE assigned to —
 not just the current active step.
 
 Is this the issue you were pointing at,
 or is there a deeper problem I'm missing?"
```

Elena:

```
Elena:
───────
"Yes — that's it. 
 The authorization check accidentally 
 saves you here, but for the 
 wrong reason and with a confusing error.
 
 Cleaner fix: add an explicit check 
 before the authorization check:
 
 // Check if this approver already 
 // acted on a step for this expense
 boolean alreadyActed = 
   approvalStepRepository
     .existsByExpenseIdAndApproverIdAndActionNot(
       expenseId, approverId, ApprovalAction.PENDING
     );
 
 if (alreadyActed) {
   throw new InvalidExpenseStateException(
     'You have already acted on this expense.'
   );
 }
 
 This gives a clear, correct error 
 instead of a confusing 403."
```

You added this check. PR approved and merged.

```
What this code review moment taught you:
──────────────────────────────────────────
You had thought through the 
happy path carefully.
You had thought through the 
obvious error paths (wrong status,
wrong approver).

What you hadn't thought through:
concurrent or near-concurrent 
duplicate requests — the "what if 
someone double-clicks" scenario.

This is something senior engineers 
think about automatically.
They've seen it break in production 
before and it's burned into their 
mental model.

For you it was a lesson:
after testing the happy path,
ask yourself:
"What does this look like if the 
 same request comes in twice?"
That question alone would have 
caught this before the PR.
```

---

## The Demo — Sprint Demo, Month 5

At the end of the sprint, you demonstrated the feature in the sprint demo. Lukas, the PM (product manager), and two other team members were present.

```
What you showed:
─────────────────
1. Submitted an expense of €2,500
   → Two approval steps created
   → Status: PENDING_APPROVAL
   → Dashboard showed: 
     "Step 1 of 2 — waiting for Sarah Chen"

2. Logged in as Sarah (manager)
   → Approved step 1
   → Step 2 activated automatically
   → Dashboard showed:
     "Step 2 of 2 — waiting for Lisa Wang (Finance)"

3. Logged in as Lisa (finance manager)
   → Approved step 2
   → Expense status changed to APPROVED
   → Payment trigger visible in outbox events

4. Showed the rejection path:
   → Lisa rejects at step 2
   → Expense status: REJECTED
   → Employee gets notified

5. Showed the deduplication case:
   → Submitted expense where manager 
     IS the finance manager
   → Only one approval step created
```

The PM asked a question during the demo:

```
PM:
────
"What happens if the assigned approver 
 leaves the company mid-approval?
 The expense would be stuck forever."
```

You didn't have an answer. This was not in the acceptance criteria. You hadn't thought about it.

You said:

```
You:
─────
"That's a good edge case and 
 honestly not something I 
 considered during implementation.
 
 Currently it would get stuck —
 the expense would sit in 
 PENDING_APPROVAL with no way 
 to progress.
 
 The right fix would be a 
 reassignment flow — when an 
 employee is deactivated in the 
 system, any pending approval steps 
 assigned to them get reassigned 
 to their manager or the admin.
 
 That would need to be a separate 
 ticket and a separate sprint though."
```

Lukas nodded:

```
Lukas:
───────
"Good answer. Create a follow-up 
 ticket after the demo.
 That's a real edge case we 
 should handle."
```

```
What this moment taught you:
──────────────────────────────
Two things.

First: saying "I didn't think of that,
here's what would need to happen to fix it"
is a correct and confident answer 
in a product demo.
It's not a failure.
It's honest. And it shows you can 
reason through a problem on the spot
even when it wasn't in your original design.

Second: the PM's question was valid 
and you should have thought of it.
After this, you started asking 
yourself one more question 
after designing any workflow:
"What happens to in-progress instances 
 of this workflow if the key actors 
 disappear?"
It's not always relevant.
But when it is, it's critical.
```

---

## The Result

```
What shipped:
──────────────
Multi-level approval workflow in 
production by end of month 5.
- Expenses > €2,000 routed through 
  two approval steps
- Steps enforced in order
- Deduplication for same-person 
  scenarios working
- Full audit trail per step
- Dashboard showing current step 
  and who needs to act

Metrics after shipping:
─────────────────────────
No production bugs from this feature 
in the first two weeks.
(0 bug reports, 0 error alerts 
 related to this feature in Datadog)

For an 8-point ticket in month 5,
that was meaningful.

What you learned:
──────────────────
1. Drawing the state machine before 
   writing code prevented gaps 
   in your design

2. The concurrent duplicate request 
   scenario — Elena found it 
   in review, you learned to 
   ask "what if this comes in twice?"

3. Saying "I don't know, 
   here's what would fix it" 
   in a product demo is correct

4. Owned the full feature:
   - Schema migration
   - Routing logic
   - State transitions
   - API response changes
   - Unit tests
   - Integration tests
   - PR written and addressed
   This was your first genuine 
   end-to-end ownership.
   It felt different from 
   the previous tickets.

PR review count:
─────────────────
Story 1 (month 2): 8 comments
Story 4 (month 5): ~6 comments (N+1 fix)
This feature (month 5): 4 comments

The downward trend was real.
Not because Elena was being easier —
because your code was getting 
consistently better.
```

---

## The "Tricky Question" Preparation

---

**Q1: "Walk me through how you determine which approval steps to create for a given expense."**

```
The logic lives in ApprovalRoutingService.

When an expense is submitted,
we fetch the company's approval policy 
via FeignClient from User & Org Service.
The policy contains rules — each rule 
defines a range of expense amounts 
and which approver role is needed.

For example:
  Below €50: no approval
  €50 to €2,000: manager
  Above €2,000: manager, then finance manager

For a given expense amount, 
we find all matching rules,
sort them by step order,
and for each rule we resolve 
the actual approver:
  MANAGER → call User & Org Service 
             to get this employee's manager
  FINANCE_MANAGER → call User & Org Service 
                    to get the company's 
                    finance manager

We also deduplicate — if the manager 
and finance manager resolve to 
the same person, we create only one step.

The result is an ordered list of 
step definitions, each with an 
approver ID and role.

In the ExpenseService, we take this list,
create ApprovalStep entities, 
activate only the first one 
(setting activatedAt),
and leave the rest inactive until 
the previous step completes.
```

---

**Q2: "Why did you use REPEATABLE_READ isolation instead of the default READ_COMMITTED for the approval action method?"**

```
The approval action is a method 
where two concurrent requests 
could cause data corruption.

Imagine a manager double-clicks 
"Approve" — two requests arrive 
within milliseconds of each other.

Both read the expense, 
both see PENDING_APPROVAL,
both find the current active step,
both verify they're the assigned approver.
Without isolation protection,
both could update the step to APPROVED —
and possibly trigger the next step twice,
or both try to set the final 
APPROVED status simultaneously.

With REPEATABLE_READ isolation:
PostgreSQL uses MVCC — 
multi-version concurrency control.
Each transaction sees a consistent 
snapshot of the data as it was 
when the transaction started.

When both transactions try to update 
the same row, the second one detects 
that the row was already modified 
by the first and throws 
SerializationFailureException.

We catch this in the GlobalExceptionHandler 
and return 409 Conflict.

The client — typically a web UI — 
handles 409 by showing the user 
"this was already processed" 
rather than retrying.
```

---

**Q3: "You mentioned writing the audit log with REQUIRES_NEW propagation. Why?"**

```
The audit log for each approval step 
action must be written regardless 
of whether the outer transaction 
succeeds or fails.

If the approval transaction fails 
for any reason — say, a DB error 
while saving the expense status —
and the outer transaction rolls back,
we still want to know:
"Manager Sarah Chen attempted to 
 approve expense X at 10:30:02 
 but the system encountered an error."

If the audit log was in the same 
transaction, it would roll back 
along with everything else.
No trace of the attempt.
That's bad for compliance.

REQUIRES_NEW starts a completely 
independent transaction for the 
audit log write.
It commits immediately, 
regardless of what happens to 
the outer transaction.

So even in an error scenario, 
the audit trail is preserved.
Elena introduced me to this pattern 
and explained exactly this scenario.
From that point on, any time I 
write an audit log, it goes in 
REQUIRES_NEW.
```

---

**Q4: "The PM found an edge case in the demo that you hadn't thought about. How did you handle that and what did you change in your process?"**

```
In the demo, the PM asked what happens 
if an approver leaves the company 
mid-approval.
I hadn't thought about it.
The honest answer was: 
the expense gets stuck.

I didn't pretend I'd handled it.
I explained what would need to happen 
to fix it — a reassignment flow 
triggered when an employee is 
deactivated — and said it needed 
a separate ticket.

Lukas asked me to create that ticket 
after the demo, which I did.
It went on the backlog 
and got picked up in a later sprint.

What changed in my process:
After this, whenever I designed 
a workflow feature — anything 
involving state machines and 
multiple actors — I started asking 
explicitly: "what happens to 
in-progress instances of this 
workflow if the key actors 
become unavailable?"

It's not always applicable.
But for approval workflows,
for anything that requires a human 
to act to progress — it always is.
```

---

Story 5 complete.

```
What Stories 4 and 5 together show:
──────────────────────────────────────
Story 4 (N+1 bug):
  You made a mistake, 
  saw it in real data,
  understood it at the SQL level,
  fixed it with the right tool,
  have specific numbers to back it up.

Story 5 (multi-level approval):
  You designed before coding,
  took genuine end-to-end ownership,
  handled a subtle concurrency bug 
  found in review,
  presented in a sprint demo,
  handled an unexpected question 
  with honesty and composure.

Both together show the "Building 
Confidence" theme of Block 2 —
not perfect execution, 
but real ownership with real learning.
```

Ready for Story 6 — the `@Transactional` on a private method mistake. Shall I begin?