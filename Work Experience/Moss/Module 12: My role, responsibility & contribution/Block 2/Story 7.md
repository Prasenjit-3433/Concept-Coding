# Story 7: Pushing Back on the PM Request — First Time You Disagreed Upward

---

## Context — Why This Story Matters

```
The first six stories in this 
18-month arc have been technical.

N+1 queries. Transaction boundaries.
Approval workflows. Proxy mechanics.

This story is different.

It's about the moment you first 
disagreed with someone who had 
more organizational authority 
than you — and said so.

Not rudely. Not passively.
Not by staying quiet and doing 
what you were told.

But by raising a technical concern 
clearly, in a meeting, with 
justification, in front of the team.

At month 6 as a junior engineer 
on a contract via Turing.
```

```
Why junior engineers often 
don't push back:
──────────────────
Fear of looking difficult.
Fear of being wrong.
Fear of overstepping.
"I'm new here — who am I 
 to question the PM?"

What actually happens when 
you don't push back:
──────────────────────────
You implement something technically 
problematic because you didn't 
want to say anything.
It breaks or causes problems later.
Now the PM AND the team are dealing 
with a bigger problem.
And nobody benefited from your silence.

The skill isn't disagreeing loudly.
The skill is disagreeing correctly —
with evidence, with alternatives,
at the right moment.
```

---

## The Situation

Sprint planning, end of month 6. The PM — Priya from the product team, not Priya Nair from engineering — presented a new requirement:

```
PM's request in sprint planning:
──────────────────────────────────
"We need to add a bulk expense 
 approval feature.

 Finance managers are spending too 
 much time approving expenses one 
 by one. They want to select 
 multiple expenses and approve them 
 all in a single click.

 The API should accept a list 
 of expense IDs and approve all 
 of them in one request.

 Like this:

 PUT /api/v1/expenses/bulk-approve
 Body: {
   'expenseIds': [
     'uuid-1',
     'uuid-2',
     'uuid-3',
     ...
   ],
   'comment': 'Batch approved'
 }

 Story points estimate?
 Timeline: this sprint if possible."
```

The PM had a legitimate problem. Finance managers at larger customers had 50-100 expenses pending approval every Monday morning. Clicking through each one individually was genuinely painful. This was real user feedback.

The room started discussing story points. Lukas was nodding. Sophie was writing notes.

You stayed quiet for about 30 seconds.

Then you said something.

---

## The Action — Raising the Concern

```
You (in sprint planning):
──────────────────────────
"Before we estimate — 
 I want to raise a concern 
 about the implementation.

 If we approve a list of expense IDs 
 in a single transaction, 
 what happens if one of them fails?

 For example — one expense in the 
 list is already approved, 
 or one belongs to a different company,
 or one has an invalid state.

 Do all of them get rolled back?
 Or do we approve the valid ones 
 and skip the invalid ones?

 The answer changes the implementation 
 significantly."
```

The room was quiet for a moment.

The PM responded:

```
PM:
────
"Good question. 
 I think approve the valid ones 
 and skip the invalid ones.
 Users wouldn't want a single 
 bad expense to block 40 valid ones."
```

You continued:

```
You:
─────
"Okay — that's the harder case 
 to implement correctly.

 If we run all approvals in a 
 single transaction and one fails,
 the whole thing rolls back.
 That's standard @Transactional behavior.

 To approve valid ones and skip 
 invalid ones, we need to run 
 each approval in its own transaction.
 If expense A fails, expense B 
 still commits independently.

 That's possible — we can use 
 REQUIRES_NEW propagation for each 
 individual approval within the loop.

 But there's a second problem:
 we have a multi-level approval workflow.
 An expense above €2,000 requires 
 manager approval first, 
 then finance manager.

 If a finance manager uses bulk approve
 on a list that includes some 
 single-level expenses AND some 
 that are waiting for step 2,
 the logic gets complex.
 
 Should bulk approve only work 
 for the finance manager's 
 currently active step?
 Or should it also trigger 
 step 1 approvals they happen 
 to also be assigned to?

 I don't know the answer —
 that's a product decision.
 But if we don't define it now,
 we'll be guessing during implementation
 and probably getting it wrong."
```

The PM looked at Lukas.

```
PM:
────
"I hadn't thought through 
 the multi-level case.
 Let me think about this."

Lukas:
───────
"Good flags. Let's not estimate 
 this sprint.
 
 [PM name] — can you come back 
 with a more detailed spec 
 that addresses these cases?
 The questions raised are valid 
 and need answers before we 
 start building.
 
 [Your name] — after this meeting,
 can you write up the edge cases 
 you're thinking about so the PM 
 has a clear picture of what 
 needs to be decided?"
```

---

## After the Meeting — Writing the Edge Case Document

You spent an hour after the sprint planning meeting writing a clear document in Confluence. Not a technical spec — a list of questions and scenarios that needed product decisions before engineering could start.

```
Document you wrote:
────────────────────
Title: Bulk Expense Approval — 
       Open Questions Before Implementation

─────────────────────────────────────────

1. PARTIAL SUCCESS BEHAVIOR
────────────────────────────
Scenario: 50 expenses submitted for 
          bulk approval. 3 are invalid 
          (wrong state, wrong company, 
           already approved).

Question: Should the API:
  A) Approve all 47 valid ones, 
     return a summary of 3 skipped?
  B) Reject the whole request if 
     any expense is invalid?

Implication of A:
  Requires each approval to run 
  in its own transaction (REQUIRES_NEW).
  Response must include per-expense 
  success/failure breakdown.
  More complex to implement.
  Better UX.

Implication of B:
  Simpler — single transaction, 
  any failure rolls everything back.
  Finance manager needs to clean 
  the list before bulk approving.
  Simpler implementation.
  Worse UX for large lists.

─────────────────────────────────────────

2. MULTI-LEVEL APPROVAL INTERACTION
──────────────────────────────────────
Context: Some expenses require 
         two approval steps.
         Bulk approve is most useful 
         for finance managers who 
         approve step 2.

Scenario A: Finance manager bulk-approves 
            a list where SOME expenses 
            are at step 1 (waiting for 
            manager — not this user) 
            and SOME are at step 2 
            (waiting for finance manager 
            — this user).

Question: What should happen?
  Option 1: Only approve the ones 
            where this user is 
            the active approver.
            Skip the rest silently.
  Option 2: Return an error for 
            expenses where this user 
            is not the current approver.
  Option 3: Approve expenses at step 2 
            only — filter out others 
            before processing.

─────────────────────────────────────────

3. AUDIT TRAIL
────────────────
Scenario: 50 expenses approved in bulk.

Question: Should each approval have 
          its own audit log entry 
          (50 entries) or one 
          bulk-operation entry?

Implication: 
  50 individual entries = cleaner 
  per-expense audit trail.
  Better for compliance review.
  (Recommended — consistent 
   with individual approvals)

─────────────────────────────────────────

4. RESPONSE FORMAT
────────────────────
Scenario: 50 submitted, 47 approved, 
          3 skipped.

Question: What does the API return?
  Option A: 200 OK with summary:
    { 
      approved: 47, 
      skipped: 3,
      skippedDetails: [...] 
    }
  Option B: 207 Multi-Status 
    (HTTP spec for partial success)
    with per-expense status

  207 is more semantically correct 
  but less commonly used.
  200 with summary is simpler 
  for frontend to handle.

─────────────────────────────────────────

5. SIZE LIMIT
──────────────
Question: Should there be a maximum 
          number of expenses 
          per bulk request?

Implication: 
  No limit = a finance manager could 
  submit 500 expenses.
  500 individual transactions, 
  500 Kafka events, 
  500 audit log entries in one request.
  Potential performance issue.
  Recommend: max 100 per request.
  If more needed, call multiple times.

─────────────────────────────────────────

Recommendation from engineering:
──────────────────────────────────
If the answer to question 1 is Option A 
(partial success), estimated complexity: 
5-8 story points.
One sprint at current team velocity.

If the answer is Option B (all-or-nothing),
estimated complexity: 3 story points.
Could fit in current sprint.

Waiting for product decisions before 
estimating formally.
```

You shared this in the team Slack channel and tagged the PM.

The PM responded in Slack two hours later:

```
PM (Slack):
────────────
"This is really helpful — 
 I hadn't thought through 
 several of these.
 
 Decisions:
 1. Partial success (Option A).
    Return summary with skipped.
 2. Only approve expenses where 
    the caller is the current approver.
    Skip others silently.
 3. Individual audit log per expense.
 4. 200 with summary for now.
    We can revisit 207 later.
 5. Max 100 per request.
 
 Can you add the ticket with this 
 spec for next sprint?"
```

Lukas added a comment:

```
Lukas (Slack):
───────────────
"Good work on this. 
 This is exactly the kind of 
 thinking we need before tickets 
 get into planning.
 
 [Your name] — I'll note this 
 for your performance review.
 This is L2 behavior."
```

```
That last sentence stopped you.
"L2 behavior."

You were hired as L1.
That was month 6.
```

---

## The Implementation — Next Sprint

The ticket was properly spec'd and landed in the next sprint. You implemented it.

The most interesting part technically was the partial success pattern — approving valid expenses and skipping invalid ones within a single HTTP request, where each approval ran in its own transaction.

```java
@Service
@RequiredArgsConstructor
public class BulkApprovalService {

    private final ExpenseRepository expenseRepository;
    private final ApprovalStepRepository 
        approvalStepRepository;
    private final SingleApprovalService 
        singleApprovalService;
    // Extracted to its own bean so 
    // @Transactional(REQUIRES_NEW) works —
    // remember the proxy lesson from Story 6

    public BulkApprovalResponse bulkApprove(
            List<UUID> expenseIds,
            UUID approverId,
            String comment) {

        // Enforce size limit
        if (expenseIds.size() > 100) {
            throw new ValidationException(
                "Bulk approval limit is 100 expenses " +
                "per request. Received: " + 
                expenseIds.size()
            );
        }

        // Deduplicate IDs — 
        // don't process the same expense twice
        List<UUID> deduplicated = expenseIds.stream()
            .distinct()
            .collect(Collectors.toList());

        List<BulkApprovalResult> results = 
            new ArrayList<>();

        for (UUID expenseId : deduplicated) {

            BulkApprovalResult result = 
                singleApprovalService
                    .attemptSingleApproval(
                        expenseId, 
                        approverId, 
                        comment
                    );

            results.add(result);
        }

        long approved = results.stream()
            .filter(r -> r.getStatus() == 
                BulkApprovalStatus.APPROVED)
            .count();

        long skipped = results.stream()
            .filter(r -> r.getStatus() == 
                BulkApprovalStatus.SKIPPED)
            .count();

        return BulkApprovalResponse.builder()
            .totalRequested(deduplicated.size())
            .approved(approved)
            .skipped(skipped)
            .results(results)
            .build();
    }
}
```

```java
// Separate bean — so REQUIRES_NEW works
// (lesson from Story 6 applied here)
@Service
@RequiredArgsConstructor
public class SingleApprovalService {

    private final ExpenseRepository expenseRepository;
    private final ApprovalStepRepository 
        approvalStepRepository;
    private final ExpenseAuditService auditService;
    private final OutboxEventRepository 
        outboxEventRepository;

    // REQUIRES_NEW = each expense approval 
    // runs in its own independent transaction.
    // If this one fails, others are not affected.
    @Transactional(
        propagation = Propagation.REQUIRES_NEW
    )
    public BulkApprovalResult attemptSingleApproval(
            UUID expenseId,
            UUID approverId,
            String comment) {

        try {
            Expense expense = expenseRepository
                .findById(expenseId)
                .orElse(null);

            // Skip if not found
            if (expense == null) {
                return BulkApprovalResult.skipped(
                    expenseId, 
                    "Expense not found"
                );
            }

            // Skip if not in correct state
            if (expense.getStatus() != 
                    ExpenseStatus.PENDING_APPROVAL) {
                return BulkApprovalResult.skipped(
                    expenseId,
                    "Not pending approval. " +
                    "Status: " + expense.getStatus()
                );
            }

            // Find current active step
            Optional<ApprovalStep> currentStepOpt =
                approvalStepRepository
                    .findCurrentActiveStep(expenseId);

            if (currentStepOpt.isEmpty()) {
                return BulkApprovalResult.skipped(
                    expenseId,
                    "No active approval step found"
                );
            }

            ApprovalStep currentStep = 
                currentStepOpt.get();

            // Skip if this user is not 
            // the current approver
            if (!currentStep.getApproverId()
                    .equals(approverId)) {
                return BulkApprovalResult.skipped(
                    expenseId,
                    "Not your step to approve"
                );
            }

            // All checks passed — approve
            Instant now = Instant.now();

            currentStep.setAction(
                ApprovalAction.APPROVED);
            currentStep.setComment(comment);
            currentStep.setActedAt(now);
            approvalStepRepository.save(currentStep);

            // Check for next step
            Optional<ApprovalStep> nextStep =
                approvalStepRepository
                    .findNextPendingStep(
                        expenseId,
                        currentStep.getStepOrder()
                    );

            if (nextStep.isPresent()) {
                ApprovalStep next = nextStep.get();
                next.setActivatedAt(now);
                approvalStepRepository.save(next);
                expense.setAssignedApproverId(
                    next.getApproverId());
            } else {
                expense.setStatus(
                    ExpenseStatus.APPROVED);
                expense.setApprovedAt(now);
            }

            expenseRepository.save(expense);

            // Outbox event — same REQUIRES_NEW tx
            outboxEventRepository.save(
                buildOutboxEvent(expense)
            );

            // Audit log — its own REQUIRES_NEW tx
            // (called on separate bean — works)
            auditService.logApprovalAction(
                expenseId, approverId,
                ApprovalAction.APPROVED,
                currentStep.getStepOrder()
            );

            return BulkApprovalResult.approved(
                expenseId);

        } catch (Exception e) {
            // Unexpected error — 
            // this transaction rolls back,
            // others are unaffected
            log.error("Unexpected error during " +
                "bulk approval of expense {}",
                expenseId, e);

            return BulkApprovalResult.skipped(
                expenseId,
                "Unexpected error: " + e.getMessage()
            );
        }
    }
}
```

**The API response for a bulk approval:**

```json
HTTP 200 OK
{
  "totalRequested": 10,
  "approved": 8,
  "skipped": 2,
  "results": [
    {
      "expenseId": "uuid-1",
      "status": "APPROVED"
    },
    {
      "expenseId": "uuid-2",
      "status": "APPROVED"
    },
    {
      "expenseId": "uuid-3",
      "status": "SKIPPED",
      "reason": "Not your step to approve"
    },
    {
      "expenseId": "uuid-4",
      "status": "SKIPPED",
      "reason": "Not pending approval. Status: APPROVED"
    },
    ...
  ]
}
```

---

## The PR — And the Moment Elena Noticed Something

Elena reviewed the implementation. Three comments, all minor. But she added a note at the end that wasn't a change request — it was an observation:

```
Elena's closing note on the PR:
─────────────────────────────────
"Good implementation. 
 I want to call out something 
 for the record:
 
 The reason this is well-structured 
 is that the product questions 
 were answered before the code 
 was written.
 
 The SingleApprovalService separation 
 shows you remembered the proxy 
 lesson from last month —
 you applied it proactively 
 instead of making the mistake 
 and fixing it.
 
 That's the difference between 
 learning and internalizing."
```

```
"That's the difference between 
 learning and internalizing."

You read that a few times.

In Story 6 you had put @Transactional 
on a private method.
Elena caught it in review.
You fixed it.
You raised the SonarQube rule.

Now, one month later, you were 
building a feature that had the 
exact same requirement —
each individual approval needs 
its own transaction —
and you instinctively created 
a separate bean to make it work.

You didn't have to think about it.
You didn't look up the proxy rule.
It was just... the obvious approach.

That's what internalization looks like.
Elena named it.
That naming mattered.
```

---

## The Broader Reflection — What This Story Was Really About

```
The technical part of this story —
bulk approval with REQUIRES_NEW —
is interesting but not the main point.

The main point is what happened 
in sprint planning.

You were the most junior person 
in that room.
You had been at the company 
6 months.
You were on a contract via Turing.
The PM had more organizational 
authority than you.
Lukas was nodding along.
Sophie was taking notes.

And you said:
"Before we estimate — 
 I want to raise a concern."

Not "I'm not sure but..."
Not raising your hand timidly.
Not staying quiet and hoping 
someone else would notice.

A clear sentence.
A specific technical concern.
Evidence: the multi-level approval 
workflow you had just built 
would make this more complex.
A question that needed an answer 
before estimating.

You were right.
The PM hadn't thought it through.
Lukas hadn't caught it either.

Not because they were incompetent —
because you were the closest to 
that part of the codebase.
You had just built the multi-level 
approval workflow.
You knew exactly where the edge 
cases would land.

Proximity to the code gave you 
knowledge nobody else in that 
room had.
The junior engineer knew something 
the PM and the EM didn't.

And you used it.
```

```
The lesson that stayed with you:
──────────────────────────────────
Your value in planning meetings 
is not your seniority.
It's your proximity to the system.

You know what's in that codebase.
You know where the edge cases are.
You know what will break.
That knowledge is worth something —
but only if you say it.

Silence in a planning meeting 
is not modesty.
It's waste.
You're withholding information 
that the team needs to make 
a good decision.

The skill is HOW you say it.
Not "that's a bad idea."
Not "I don't think we can do that."

"Before we estimate — 
 I want to raise a concern."

Then the technical facts.
Then the question.
Then let the room decide.
```

---

## The Result

```
What happened:
───────────────
Sprint planning: PM's request 
  deferred to next sprint.
  Edge cases defined in Confluence doc.
  Product decisions made in 24 hours.

Following sprint: 
  Bulk approval implemented correctly,
  first attempt, 0 bugs in staging.
  
  "0 bugs in staging, first attempt" —
  because the edge cases were defined 
  before the code was written.
  Not because you're perfect.
  Because ambiguity was removed 
  before it became a bug.

Lukas's comment:
  "This is L2 behavior."
  Month 6. First time a senior person 
  called out your work as above 
  your current level.

What you learned:
──────────────────
1. Junior engineers have unique 
   knowledge — proximity to the code.
   Senior people have broader context.
   Neither is complete without the other.

2. Raising a concern in a meeting 
   is not the same as pushing back.
   Pushing back says "no."
   Raising a concern says "here's 
   what we need to decide before 
   we can move forward."
   One closes conversation.
   The other opens it.

3. A Confluence doc that converts 
   ambiguity into explicit decisions
   is more valuable than the code 
   that implements those decisions.
   The code is a mechanical 
   consequence of the decisions.
   The decisions are where the 
   real work happens.

4. Applying Story 6's lesson 
   (SingleApprovalService as 
   a separate bean) proactively
   was noticed by Elena.
   That observation —
   "learning vs internalizing" —
   was one of the clearest pieces 
   of feedback you received 
   in 18 months.
```

---

## The "Tricky Question" Preparation

---

**Q1: "You said you raised the concern in sprint planning. Were you worried about how it would land?"**

```
Yes, honestly.

I was the most junior person in 
that room. Six months in, 
on a contract via Turing.
The PM had been working on 
this product for over a year.
Lukas was nodding.

My first instinct was to stay 
quiet and raise it with Lukas 
after the meeting — 
a safer, private channel.

What made me say it in the meeting 
instead:

If I waited until after, 
the ticket might already be estimated 
and in the sprint.
Changing it later is more expensive 
— the team has already planned 
around it, expectations are set.

The cheapest time to surface 
a concern is before the decision 
is made.
After is always more expensive.

So I said it in the room.
The framing mattered —
"Before we estimate, 
 I want to raise a concern."
Not a declaration. Not a correction.
A flag that said: there's something 
here worth pausing on.

The PM received it well.
Lukas supported it.
And the outcome was better 
for having said it.
```

---

**Q2: "Why did you use REQUIRES_NEW for each approval in the bulk operation instead of a single transaction?"**

```
Because of the partial success 
requirement.

The PM's decision was: approve the 
valid ones, skip the invalid ones.

If we ran all 50 approvals in 
a single @Transactional method,
any failure would roll back 
ALL 50 approvals.
The 49 valid ones would be lost 
because of the 1 invalid one.

To allow each approval to commit 
independently — so that a failure 
on expense 3 doesn't affect 
expenses 1, 2, 4, 5... —
each approval needs its own transaction.

REQUIRES_NEW suspends any existing 
transaction and starts a completely 
new, independent one.
If this one commits, it commits.
If this one fails and rolls back,
it rolls back alone.
The outer method continues 
to the next expense.

The implementation detail:
REQUIRES_NEW must be on a method 
in a SEPARATE Spring bean —
not a private method in the same class.
I learned that in Story 6 
(the @Transactional proxy lesson).
Here I applied it proactively 
by putting the single approval 
logic in SingleApprovalService,
a separate bean from BulkApprovalService.
Each call to attemptSingleApproval() 
goes through the proxy,
and REQUIRES_NEW works correctly.
```

---

**Q3: "Your Confluence doc had five open questions. How did you decide what to include?"**

```
I focused on questions where 
the answer would change 
the implementation.

Not every edge case is worth 
writing down — some have 
obvious defaults.

But for each item I included,
I asked: "if the PM answers A, 
the code looks like X.
If they answer B, 
the code looks completely different."

Partial success behavior: 
  A vs B changed the entire 
  transaction strategy.
  
Multi-level interaction: 
  Unclear what "bulk approve" 
  means when some expenses 
  are at different steps.
  Without an answer, I'd be guessing.

Size limit:
  Without a limit, a single request 
  could generate 500 Kafka events.
  That's a performance question 
  with a concrete recommendation.

Response format:
  Frontend needed to know 
  what shape to expect.

I didn't include things like:
"What if the comment is empty?"
That has an obvious default — allow it.
It doesn't change the architecture.

The filter was: 
"Does this need a product decision,
or can engineering decide it?"
If engineering can decide it,
decide it and note it.
If product needs to decide it,
put it in the document.
```

---

**Q4: "You mentioned Lukas said 'this is L2 behavior' in Slack. What did that mean to you at month 6?"**

```
It was the first time someone 
senior explicitly connected 
something I did to a level above 
where I was hired.

I was hired as L1.
L2 is the next step.
Lukas wasn't saying I was being 
promoted — he was saying 
this specific behavior was 
what he expected from L2 engineers.

What I took from it:
  Not "I'm already L2."
  But "this is what L2 looks like."
  
  Raising concerns before estimation.
  Converting ambiguity into 
  explicit decisions.
  Writing documentation that 
  makes the team more productive.

  Not just implementing what 
  you're told.
  Understanding why you're building 
  something, what could go wrong,
  and saying it before it does.

It gave me a concrete example 
of what growth looked like —
not in abstract terms like 
"take more ownership" or 
"be more proactive,"
but in a real action that 
happened in a real meeting 
that had a real positive outcome.

That's more useful than 
any generic feedback.
```

---

Block 2 complete. All four stories done.

```
BLOCK 2 SUMMARY — 
"Building Confidence" (Months 4-6):
──────────────────────────────────────────

Story 4: N+1 bug
  Technical mistake visible 
  only in Datadog.
  800ms → 45ms.
  First time seeing performance 
  data change your code.

Story 5: Multi-level approval feature
  First 8-point ticket.
  Full end-to-end ownership.
  Design before code.
  Concurrency edge case caught 
  in review.
  Unexpected product question 
  handled with composure.

Story 6: @Transactional private method
  Subtle framework mistake 
  invisible to unit tests.
  Fixed, raised, prevented 
  system-wide via SonarQube.
  Tomás's 6-month-old bug 
  found and fixed as a side effect.

Story 7: Pushing back on PM
  First time raising a technical 
  concern upward, in a meeting,
  in front of the team.
  Evidence-based.
  Respectful.
  Right.
  Led to better outcome.
  First "L2 behavior" comment 
  from Lukas.

The arc across Block 2:
  Month 4: Making technical mistakes 
           with confidence.
  Month 5: Owning complex features 
           with gaps.
  Month 6: Preventing mistakes 
           and influencing decisions.

That's what "Building Confidence" 
actually looks like.
Not smooth. Not linear.
But genuinely forward.
```
