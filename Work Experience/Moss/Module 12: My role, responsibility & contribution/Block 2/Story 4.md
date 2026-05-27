# Story 4: The N+1 Query Bug — First Real Performance Story

---

## Context — Where You Were at Month 5

By month 5, something had shifted.

```
Month 1-3: You were navigating.
            Finding files, understanding 
            patterns, asking basic questions,
            following what others had built.

Month 4-5: You were building.
────────────────────────────────
You had shipped 3 small features 
independently. You knew the codebase 
well enough to navigate without help.
Elena's PR reviews had dropped from 
8 comments to 3-4. 
You were writing tests without 
being reminded.
You felt like a contributor, 
not just a learner.

But "building" and "building well" 
are different things.
And month 5 was where you found out 
the difference.
```

This story is about a bug you didn't introduce. It was already in the codebase when you joined. But you were the one who touched the code that made it visible — and then you were the one who had to understand it, explain it, and fix it.

---

## The Setup — What You Were Building

In month 4, Lukas assigned you a feature:

```
Ticket: EXP-219
Title:  Finance Dashboard — Pending 
        Approvals List

Description:
─────────────
The finance manager dashboard needs 
a "Pending Approvals" section that 
shows all expenses currently waiting 
for approval.

For each expense, the dashboard 
must show:
  - Employee name
  - Amount + currency
  - Category
  - Submission date
  - Assigned approver name

Acceptance Criteria:
─────────────────────
1. GET /api/v1/expenses?status=PENDING_APPROVAL
   returns paginated list
2. Each item includes employee name 
   and approver name (not just IDs)
3. Sorted by submittedAt ascending 
   (oldest first — needs attention first)
4. Page size default 20, max 100
5. Response time should be acceptable 
   for a dashboard page load

Story points: 5
(Your biggest ticket so far)
```

Five story points. The largest ticket you had been assigned. Elena spent 30 minutes with you in a tech sync walking through the design before you started.

```
Elena's guidance before you started:
──────────────────────────────────────
"The tricky part here is the 
 employee name and approver name.
 
 Our Expense entity has:
   employeeId (UUID)
   assignedApproverId (UUID)
 
 But the dashboard needs names.
 Names live in the employees table.
 
 You have two options:
 
 Option A: Load the expense, then 
           for each expense load 
           the employee and approver 
           separately.
 
 Option B: Load everything in 
           one query using JOIN FETCH.
 
 Think about which one makes more 
 sense before you start writing code."
```

Elena was pointing you toward Option B. You understood this conceptually — JOIN FETCH loads related entities in a single SQL query instead of separate queries. You had read about it in your Spring Data JPA notes.

But when you sat down to implement, you chose Option A.

Not because you forgot what Elena said. Because Option A was simpler to write. And you were a bit overconfident from the last three successful tickets.

---

## The Implementation — What You Built

```java
// What you built — Option A approach

// Repository — simple query
public interface ExpenseRepository 
        extends JpaRepository<Expense, UUID> {

    Page<Expense> findByCompanyIdAndStatus(
        UUID companyId, 
        ExpenseStatus status, 
        Pageable pageable
    );
}

// Service — loading related data 
// for each expense
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional(readOnly = true)
    public PagedResponse<ExpenseResponse> 
            getPendingApprovals(
            UUID companyId, 
            Pageable pageable) {

        Page<Expense> expenses = expenseRepository
            .findByCompanyIdAndStatus(
                companyId,
                ExpenseStatus.PENDING_APPROVAL,
                pageable
            );
        // → 1 SQL query, returns 20 expenses

        List<ExpenseResponse> responses = 
            expenses.getContent()
                .stream()
                .map(expense -> {

                    // Load employee for this expense
                    Employee employee = 
                        employeeRepository
                            .findById(
                                expense.getEmployeeId()
                            )
                            .orElse(null);
                    // → 1 SQL query per expense

                    // Load approver for this expense
                    Employee approver = 
                        employeeRepository
                            .findById(
                                expense.getAssignedApproverId()
                            )
                            .orElse(null);
                    // → 1 SQL query per expense

                    return ExpenseResponse.builder()
                        .expenseId(expense.getId())
                        .amount(expense.getAmount())
                        .currency(expense.getCurrency())
                        .category(expense.getCategory())
                        .employeeName(
                            employee != null 
                                ? employee.getFullName() 
                                : "Unknown"
                        )
                        .approverName(
                            approver != null 
                                ? approver.getFullName() 
                                : "Unknown"
                        )
                        .submittedAt(
                            expense.getSubmittedAt()
                        )
                        .build();
                })
                .collect(Collectors.toList());

        return PagedResponse.from(
            expenses, responses
        );
    }
}
```

You were pleased with this. It was clean. It worked. All tests passed. You opened the PR.

---

## What the PR Looked Like

Elena reviewed it the next day. Her first comment:

```
Elena's PR comment:
────────────────────
"This will work, but take a look 
 at what's happening with the 
 SQL queries here.

 With a page size of 20:
 - 1 query to fetch expenses
 - 20 queries to fetch employees
 - 20 queries to fetch approvers
 = 41 queries per page load

 With page size 100:
 = 201 queries per page load

 A finance manager's dashboard 
 loading 100 expenses = 201 
 database round-trips.
 
 What do you think the p99 latency 
 looks like for that?
 
 I want you to think through the 
 implications before we discuss 
 the fix."
```

You read this and your first reaction was defensive — *"But it works, the tests pass."*

Then you actually thought about what Elena was saying.

```
The math that hit you:
───────────────────────
Page of 20 expenses:
  1 query (expenses)
  + 20 queries (employees)
  + 20 queries (approvers)
  = 41 database queries

Each query: ~5-10ms 
  (assuming fast connection, 
   no lock contention)

41 × 8ms = 328ms just from DB queries.
Plus network, serialization, etc.
P99 latency: 500-800ms for one page load.

Page of 100 expenses:
  201 queries × 8ms = 1,600ms
  P99 latency: 2,000ms+
  
  That's over 2 seconds for 
  a dashboard to load.
  Users would notice immediately.
```

You replied to Elena's comment:

```
Your reply:
────────────
"I see the problem now.
 Each expense triggers two separate 
 DB queries — one for the employee, 
 one for the approver.
 For 20 expenses that's 41 queries.
 For 100 it's 201.
 
 The fix would be to load the 
 employees in the initial query 
 using JOIN FETCH, so we get 
 everything in one query.
 
 Is that the right direction?"
```

Elena:

```
Elena:
───────
"Yes. But before you implement it —
 deploy what you have to staging 
 and look at what Datadog shows.
 It will make the problem more 
 concrete. Then fix it and compare.
 
 This is a lesson worth seeing 
 with real numbers, not just math."
```

---

## The Staging Deploy — Seeing It in Datadog

You merged the PR as-is (Elena approved it for staging, not production) and it went to staging. Then Elena sent you a link to the Datadog APM traces for the endpoint.

This was your first real look at Datadog APM.

```
What you saw in Datadog APM:
──────────────────────────────

Trace for GET /api/v1/expenses
(page=0, size=20, status=PENDING_APPROVAL)

Total duration: 847ms

Spans:
────────────────────────────────────────
expense-service (847ms)
├── DB: findByCompanyIdAndStatus (45ms)
│   SQL: SELECT e.* FROM expenses e 
│        WHERE e.company_id = ?
│        AND e.status = ?
│        LIMIT 20
│
├── DB: findById [employee] (12ms)
├── DB: findById [approver] (11ms)
├── DB: findById [employee] (13ms)
├── DB: findById [approver] (10ms)
├── DB: findById [employee] (11ms)
├── DB: findById [approver] (14ms)
├── DB: findById [employee] (12ms)
├── DB: findById [approver] (11ms)
├── DB: findById [employee] (13ms)
├── DB: findById [approver] (12ms)
├── DB: findById [employee] (11ms)
├── DB: findById [approver] (13ms)
├── DB: findById [employee] (12ms)
├── DB: findById [approver] (10ms)
├── DB: findById [employee] (14ms)
├── DB: findById [approver] (11ms)
├── DB: findById [employee] (12ms)
├── DB: findById [approver] (13ms)
├── DB: findById [employee] (11ms)
└── DB: findById [approver] (12ms)

Total DB spans: 41
Total DB time: ~757ms out of 847ms
```

```
What this showed you visually:
───────────────────────────────
The single expenses query: 45ms
Everything else: 40 separate queries,
  each taking 10-14ms,
  all for the same employees table,
  many of them REPEATED 
  (the same approver appearing 
   across multiple expenses).

The Jaeger-style waterfall made 
it impossible to not see the problem.
41 thin bars, almost all identical,
stacked vertically.
That's what an N+1 problem looks like
when you visualize it.
```

You sent a message to Finn — the mid-level engineer you knew was strong on query optimization:

```
You (Slack to Finn):
─────────────────────
"Hey Finn — Elena pointed me at 
 a query issue on EXP-219. 
 I can see in Datadog that the 
 endpoint is doing 41 DB queries 
 for a page of 20 expenses.
 I know the fix is JOIN FETCH but 
 I wanted to talk through how 
 to write the JPQL correctly 
 before I implement it.
 Do you have 20 minutes?"
```

Finn replied:

```
Finn:
──────
"Sure — call in 10."
```

---

## The Fix — Pair Debugging With Finn

Finn shared his screen and opened the JPA documentation alongside your code. He explained the problem and solution methodically:

```
Finn's explanation:
────────────────────

"The N+1 problem happens because 
 of how JPA loads related entities 
 by default.

 When you call:
 expenseRepository
   .findByCompanyIdAndStatus(...)
 
 JPA runs ONE query to get expenses:
   SELECT * FROM expenses 
   WHERE company_id = ?
   AND status = ?

 But then when your code does:
   employeeRepository.findById(
     expense.getEmployeeId()
   )
 
 JPA runs a SEPARATE query for 
 each expense:
   SELECT * FROM employees 
   WHERE id = ?
 
 So for 20 expenses:
 1 expense query + 
 20 employee queries + 
 20 approver queries 
 = 41 queries.
 
 N expenses → N+1 queries.
 That's the N+1 problem.

 The fix is JOIN FETCH — 
 tell JPA in the initial query:
 'also load the related entities 
  in the same SQL JOIN.'
 
 Instead of 41 queries, 
 you get 1 query with JOINs."
```

Then Finn showed you how to write the JPQL:

```java
// The fix — what Finn showed you

// In Expense entity — 
// define the relationships
@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    private UUID id;

    // Employee who submitted the expense
    // ManyToOne because many expenses 
    // can belong to one employee
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "employee_id", 
        insertable = false, 
        updatable = false
    )
    private Employee employee;

    // The assigned approver
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "assigned_approver_id",
        insertable = false,
        updatable = false
    )
    private Employee assignedApprover;

    // These stay as plain UUID columns
    // for writes — the @ManyToOne 
    // relationships are read-only
    @Column(name = "employee_id")
    private UUID employeeId;

    @Column(name = "assigned_approver_id")
    private UUID assignedApproverId;

    // ... other fields
}
```

```java
// Updated repository — 
// JPQL with JOIN FETCH
public interface ExpenseRepository 
        extends JpaRepository<Expense, UUID> {

    // JOIN FETCH tells JPA:
    // "load the employee and 
    //  assignedApprover relationships 
    //  in the same SQL query"
    @Query(
        value = """
            SELECT e FROM Expense e
            LEFT JOIN FETCH e.employee
            LEFT JOIN FETCH e.assignedApprover
            WHERE e.companyId = :companyId
            AND e.status = :status
            """,
        countQuery = """
            SELECT COUNT(e) FROM Expense e
            WHERE e.companyId = :companyId
            AND e.status = :status
            """
    )
    Page<Expense> findWithEmployeesByCompanyIdAndStatus(
        @Param("companyId") UUID companyId,
        @Param("status") ExpenseStatus status,
        Pageable pageable
    );
}
```

```
Why we need a separate countQuery:
─────────────────────────────────────
When Spring Data JPA returns a Page<T>,
it needs to know the total number of 
results to calculate totalPages.

It runs a COUNT query for this.

If you don't provide a countQuery,
Spring generates one automatically
by wrapping your query:
SELECT COUNT(*) FROM (your JOIN FETCH query)

But JOIN FETCH in a count query 
causes problems — the JOINs produce 
duplicate rows, COUNT gives wrong numbers.

The countQuery is a plain COUNT 
without the JOIN FETCH.
Fast. Correct.
```

```java
// Updated service — clean and simple now
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    // EmployeeRepository removed — 
    // no longer needed here

    @Transactional(readOnly = true)
    public PagedResponse<ExpenseResponse> 
            getPendingApprovals(
            UUID companyId,
            Pageable pageable) {

        Page<Expense> expenses = expenseRepository
            .findWithEmployeesByCompanyIdAndStatus(
                companyId,
                ExpenseStatus.PENDING_APPROVAL,
                pageable
            );
        // → 1 SQL query with LEFT JOINs
        // Employee and approver already loaded
        // No additional queries

        return PagedResponse.from(
            expenses,
            expense -> ExpenseResponse.builder()
                .expenseId(expense.getId())
                .amount(expense.getAmount())
                .currency(expense.getCurrency())
                .category(expense.getCategory())
                .employeeName(
                    expense.getEmployee() != null
                        ? expense.getEmployee()
                               .getFullName()
                        : "Unknown"
                )
                .approverName(
                    expense.getAssignedApprover() != null
                        ? expense.getAssignedApprover()
                               .getFullName()
                        : "Unknown"
                )
                .submittedAt(expense.getSubmittedAt())
                .build()
        );
    }
}
```

**The SQL that Hibernate now generates:**

```sql
-- What Hibernate generates from JOIN FETCH query
-- ONE query instead of 41

SELECT 
    e.id,
    e.amount,
    e.currency,
    e.category,
    e.status,
    e.submitted_at,
    e.employee_id,
    e.assigned_approver_id,
    emp.id,
    emp.full_name,
    emp.email,
    approver.id,
    approver.full_name,
    approver.email
FROM expenses e
LEFT JOIN employees emp 
    ON e.employee_id = emp.id
LEFT JOIN employees approver 
    ON e.assigned_approver_id = approver.id
WHERE e.company_id = ?
AND e.status = 'PENDING_APPROVAL'
ORDER BY e.submitted_at ASC
LIMIT 20 OFFSET 0
```

```
Why LEFT JOIN and not INNER JOIN:
───────────────────────────────────
An employee might theoretically be 
deleted from the system after 
submitting an expense.
With INNER JOIN: that expense 
would disappear from the results.
With LEFT JOIN: expense appears, 
employee fields are null.
LEFT JOIN is the safe default 
for optional relationships.
```

Finn also pointed out one more thing — something subtle:

```
Finn's additional observation:
────────────────────────────────
"Notice you're fetching the same 
 employee twice when the same person 
 submitted and is their own approver —
 which won't happen for normal expenses 
 (you can't approve your own expense)
 but could happen for ADMIN-level 
 manual entries.

 More importantly: if 10 expenses 
 all have the same approver 
 (one finance manager approving 
  for their whole team), 
 your original code was querying 
 that same approver 10 times.
 
 JOIN FETCH fixes this because 
 PostgreSQL deduplicates automatically 
 at the SQL level."
```

---

## The Result — Measuring the Improvement

You deployed the fix to staging. Then you looked at Datadog again.

```
Datadog APM trace AFTER the fix:

Trace for GET /api/v1/expenses
(page=0, size=20, status=PENDING_APPROVAL)

Total duration: 47ms

Spans:
──────────────────────────────────────
expense-service (47ms)
└── DB: findWithEmployeesByCompanyIdAndStatus 
    (38ms)
    SQL: SELECT e.*, emp.*, approver.*
         FROM expenses e
         LEFT JOIN employees emp ON ...
         LEFT JOIN employees approver ON ...
         WHERE ...

Total DB spans: 1
Total DB time: 38ms out of 47ms
```

```
Before vs After:
─────────────────
BEFORE:
  Total request duration: 847ms
  DB queries: 41
  DB time: ~757ms

AFTER:
  Total request duration: 47ms
  DB queries: 1
  DB time: 38ms

Improvement:
  Request time: 847ms → 47ms
  = ~94% reduction

  DB queries: 41 → 1
  = 97.5% fewer queries

The 800ms → 45ms numbers 
came from p99 latency measured 
in Datadog Metrics over a 
one-hour window of staging load.
(p99 means 99% of requests 
 completed within this time)
```

You opened a new PR with the fix. Elena's review:

```
Elena's review of the fix PR:
───────────────────────────────
2 comments (down from 8 on your 
first PR — a signal you were improving).

Comment 1:
"Good. The countQuery is correct —
 a lot of people forget this and 
 end up with wrong pagination totals.

 One thing to add: 
 spring.jpa.show-sql=true in 
 application-dev.properties means 
 you can see the generated SQL 
 in your local console log.
 Use this to verify your queries 
 actually look like what you expect.
 Don't trust that JOIN FETCH 
 worked just because there's no error."

Comment 2:
"Add a comment above the @Query 
 explaining WHY the JOIN FETCH is there.
 Someone reading this in 6 months 
 should immediately understand the 
 intent. Something like:
 // JOIN FETCH to avoid N+1 queries
 // when loading employee and 
 // approver names for the dashboard"
```

You added both. PR merged.

---

## What Happened After — The SonarQube Rule

After the PR was merged, you brought this up in the next sprint retrospective. Not to highlight your mistake — but to prevent the same issue from happening silently in the future.

```
You in the retro:
──────────────────
"I had an N+1 query in my dashboard 
 feature that made it to staging.
 It wasn't caught in code review 
 because the code looked correct —
 it only showed up in Datadog traces.

 Is there a way to catch this earlier?
 Like a SonarQube rule or a test 
 pattern that detects N+1 issues 
 before they reach staging?"
```

Elena responded:

```
Elena:
───────
"Good question. SonarQube doesn't 
 catch N+1 directly — it's a 
 runtime behavior, not a static 
 code issue.

 The right approach is two things:

 1. Test your repository queries 
    in integration tests with 
    Testcontainers. 
    If you add an assertion on 
    the number of SQL queries executed,
    the test fails when N+1 regresses.
    Hibernate Statistics can give 
    you query counts.

 2. Review discipline:
    When reviewing any service method
    that loads related entities,
    ask: 'How many DB queries does 
    this generate?'
    Make it a mental checklist item."
```

Arjun added:

```
Arjun:
───────
"In code review, a signal to look for:
 any loop or stream that calls 
 a repository inside it.
 
 expenses.stream()
     .map(expense -> 
         employeeRepository.findById(...)
     )
 
 That pattern is almost always N+1.
 Not always fixable with JOIN FETCH —
 sometimes you need a different 
 approach — but it's always worth 
 questioning."
```

You took this away and added it to your personal mental checklist. From month 5 onward, every time you wrote a service method, you consciously asked: **how many DB queries does this generate?**

---

## The Broader Lesson — What This Changed in How You Code

```
Before this story:
───────────────────
You thought about code correctness.
Does it return the right data?
Does it handle null cases?
Do the tests pass?

After this story:
──────────────────
You started thinking about 
code behavior at scale.

"This works for 1 expense.
 What does it do for 20?
 What does it do for 100?
 How many DB queries does it generate?
 What does the Jaeger trace look like?"

This is not senior thinking —
senior engineers think about this 
automatically, by instinct.
But at month 5, you started 
asking these questions deliberately.
That's the beginning of the instinct.
```

```
What the Datadog trace taught you 
that reading documentation couldn't:
────────────────────────────────────
You had read about N+1 queries.
You knew what it meant conceptually.
But seeing 40 identical DB spans 
stacked in a Jaeger waterfall — 
all for the same data — 
made it visceral in a way 
that words couldn't.

Some things don't become real 
until you see them in production data.
This was one of them.
```

---

## The "Tricky Question" Preparation

---

**Q1: "You said you chose Option A even though Elena pointed you toward Option B. Why?"**

```
Honestly? Option A was simpler to write.
I understood it immediately.
JOIN FETCH felt more complex —
I had read about it but hadn't 
used it in a real scenario yet.

And I was slightly overconfident 
from the previous three tickets 
going smoothly.

That's the honest answer.

What I learned from it is that 
"simpler to write" and "simpler 
in production" are different things.
Code that avoids complexity at 
write time often creates complexity 
at runtime.

The discipline Elena was pointing 
me toward — think about the query 
count before writing the code —
is something I internalized after this.
Not just as a rule, but as an instinct,
because I'd seen concretely what 
happens when you skip it.
```

---

**Q2: "How does JOIN FETCH actually work at the SQL level? What query does Hibernate generate?"**

```
When you write:

SELECT e FROM Expense e
LEFT JOIN FETCH e.employee
LEFT JOIN FETCH e.assignedApprover
WHERE e.companyId = :companyId

Hibernate translates this into 
a single SQL query with two LEFT JOINs:

SELECT 
    e.*, emp.*, approver.*
FROM expenses e
LEFT JOIN employees emp 
    ON e.employee_id = emp.id
LEFT JOIN employees approver 
    ON e.assigned_approver_id = approver.id
WHERE e.company_id = ?
AND e.status = ?

This fetches the expense data 
and both related employee records 
in a single round-trip to the database.
PostgreSQL executes one plan, 
returns one result set.
Hibernate then maps the result 
back into Expense objects with 
their employee and assignedApprover 
fields already populated.

No additional queries needed.
```

---

**Q3: "Why do you need a separate countQuery when using JOIN FETCH with pagination?"**

```
When Spring Data JPA returns a Page<T>,
it needs to know the total record count
to calculate totalPages and totalElements.

It does this by running a separate 
COUNT query.

If you don't provide countQuery,
Spring auto-generates one by 
wrapping your JPQL in a count:
SELECT COUNT(*) FROM (your original query)

But JOIN FETCH in a count query 
creates a problem:
the JOINs can produce duplicate rows 
in the result set 
(one row per joined record),
and COUNT gives inflated numbers.

For example, if an expense has 
2 approvers (hypothetically),
JOIN FETCH would return 2 rows 
for that expense.
COUNT would count it twice.

The countQuery avoids this:

countQuery = """
    SELECT COUNT(e) FROM Expense e
    WHERE e.companyId = :companyId
    AND e.status = :status
    """

Plain COUNT, no JOINs.
Correct number. Fast.

This is a detail a lot of people 
miss when they first use JOIN FETCH 
with pagination — and it causes 
wrong pagination data in production.
Elena specifically called it out 
in the review, which is why I know it.
```

---

**Q4: "You said 800ms dropped to 45ms. How did you measure that exactly?"**

```
Two tools, both from our monitoring stack.

First: Datadog APM traces.
I looked at individual traces for 
the endpoint before and after the fix
in the staging environment.
The trace view in Datadog shows 
each DB span as a separate bar 
with its duration.
Before: 41 spans, total ~757ms DB time.
After: 1 span, 38ms DB time.
This is qualitative — it shows 
the structural change.

Second: Datadog Metrics.
The metric http.server.requests 
with tag uri=/api/v1/expenses 
gives you latency percentiles 
over time.
I compared the p99 latency 
for a one-hour window before 
the fix was deployed
against a one-hour window 
after it was deployed —
same time of day, similar traffic.

Before: p99 = ~800ms
After:  p99 = ~45ms

The Datadog "Compare" feature 
lets you overlay two time windows 
directly — that's how I got 
the specific numbers.

I used staging data, not production,
because we caught this before 
it reached production.
But the traffic pattern in staging 
mirrors production closely enough 
that the numbers are representative.
```

---

**Q5: "What if JOIN FETCH isn't possible — are there other solutions to N+1?"**

```
JOIN FETCH is the cleanest solution 
when you control the JPQL and 
the relationships are simple.

But there are two other approaches 
we discussed after this incident:

First: @BatchSize annotation.
You put @BatchSize(size = 20) 
on the relationship in the entity.
Hibernate then fetches related 
entities in batches instead of 
one by one.

Instead of 20 individual 
SELECT WHERE id = ? queries,
it runs one:
SELECT WHERE id IN (?, ?, ?, ... 20 IDs)

Not as good as JOIN FETCH —
it's still a second query —
but much better than N separate queries.
Useful when JOIN FETCH would produce 
a Cartesian product 
(e.g., loading a list of expenses 
 where each expense has multiple 
 approval steps — that would 
 duplicate rows in JOIN FETCH).

Second: @EntityGraph.
Works like JOIN FETCH but 
can be applied to derived query methods
without writing custom JPQL.

@EntityGraph(
    attributePaths = {"employee", 
                      "assignedApprover"}
)
Page<Expense> findByCompanyIdAndStatus(...)

Same effect as JOIN FETCH in the query,
but you keep the derived method 
naming convention.

After this incident, Arjun mentioned 
that the right tool depends on 
the relationship:
- One-to-one or many-to-one: 
  JOIN FETCH is clean
- One-to-many: be careful with 
  JOIN FETCH (Cartesian product risk),
  consider @BatchSize instead
```

---

Story 4 complete.

```
What this story demonstrates 
for an interviewer:
─────────────────────────────

Technical:
  - You understand N+1 queries 
    at the SQL level, not just 
    the concept level
  - You can explain JOIN FETCH, 
    countQuery, and why both matter
  - You know how to read Datadog 
    APM traces and use them to 
    diagnose problems
  - You know alternative solutions 
    (@BatchSize, @EntityGraph)

Behavioral:
  - You made a mistake with 
    full awareness of the better 
    approach being available
  - You owned it without defensiveness
  - You asked for help from the 
    right person (Finn) after 
    understanding the problem yourself
  - You raised it in the retro 
    to prevent recurrence
  - You have specific, measurable 
    numbers (800ms → 45ms, 41 → 1 query)
    backed by a real measurement tool
```

Ready for Story 5 — the multi-level approval feature ownership. Shall I begin?