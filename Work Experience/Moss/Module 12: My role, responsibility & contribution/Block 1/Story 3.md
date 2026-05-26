# Story 3: Helping Marta Onboard — The First Time You Knew More Than Someone Else

---

## Context — Where You Were at Month 3

Before getting into Marta's story, it's important to understand where you were mentally and technically at the point she joined.

```
Month 1: Completely overwhelmed.
          Didn't know where anything was.
          Asked questions about everything.
          Felt like everyone knew more 
          than you about everything.

Month 2: Slowly stabilizing.
          First PR merged.
          Starting to understand the 
          codebase structure.
          Still asking lots of questions 
          but better questions now.
          Still felt junior in every room.

Month 3: A quiet shift happening.
──────────────────────────────────
You knew where things were.
You could navigate the codebase 
without getting lost.
You understood the sprint process.
You knew which Slack channel to use 
for what.
You knew that docker compose down -v 
fixes the Flyway dirty state.
You knew to read errors from the top.
You knew Elena's review style.
You knew Tomás was the person to ask 
about day-to-day codebase questions.

You didn't feel senior.
But you felt like you belonged.
That's different from month 1.
```

Then Marta joined.

---

## The Situation

Marta Kowalski joined the team at the start of month 3 — two months after you. Her background was stronger than yours in some ways: 4 years of experience, solid API design knowledge, strong testing instincts. But she was new to this specific codebase, this team's processes, and this company's way of doing things.

```
Lukas sent a message in #expense-ap-team:
──────────────────────────────────────────
"Team — Marta is joining us today.
 She'll be ramping up on the Expense 
 Service this week.
 
 [Your name] — since you went through 
 onboarding most recently, could you 
 be Marta's first point of contact 
 this week for codebase questions? 
 Elena and Arjun are heads-down on 
 the approval workflow feature.
 
 You know where everything hurts 
 from onboarding — fresh memory 
 is useful here."
```

This was unexpected. You had been at the company for two months. You were still figuring things out yourself. And now you were being asked to help someone onboard.

Your immediate internal reaction was honest:

```
"What if she asks me something 
 I don't know?
 She has 4 years of experience.
 What if she thinks I'm incompetent?"
```

This feeling is important to name because it's real and because how you handled it matters.

---

## Your Task

```
Lukas's ask was specific:
──────────────────────────
Be Marta's first point of contact 
for codebase questions this week.

What that meant in practice:
─────────────────────────────
- Help her get the service 
  running locally
- Walk her through the codebase 
  structure
- Explain the team processes
  (sprint, PR conventions, 
   Slack channels, standup format)
- Answer questions where you can
- Redirect to Elena or Arjun 
  where you can't

What it did NOT mean:
──────────────────────
- Teaching her Spring Boot
  (she knew Spring Boot)
- Making all her decisions
- Pretending to know things 
  you don't know
```

The boundary was clear in your head before you started. You were not her technical mentor. You were her guide to this specific system, this specific team, this specific way of working. That was something you actually could do — because you had just been through it.

---

## The Action — Day by Day

### Day 1 — The Welcome and the Local Setup

You sent Marta a direct message on her first morning:

```
You (Slack DM to Marta):
─────────────────────────
"Hey Marta, welcome! 
 I'm [name] — I joined about 
 two months ago so I went through 
 onboarding pretty recently.
 
 Lukas mentioned I should be your 
 first stop for codebase questions 
 this week. Happy to help where I can.
 
 Have you started on the local setup yet? 
 I hit a few non-obvious issues 
 when I did it — happy to jump 
 on a call if you want a guide 
 through it rather than figuring 
 it out alone."
```

Marta replied:

```
Marta:
───────
"That would be great actually — 
 yes, just starting now. 
 Call in 15?"
```

You jumped on a Google Meet. You shared your screen and walked her through the setup while she followed along on her own machine. You narrated everything you did and why — not just the commands, but the reasoning:

```
What you walked her through:
─────────────────────────────

Step 1: docker compose up -d
        Explanation: 
        "This starts 6 containers —
         Postgres, Redis, Kafka, 
         Zookeeper, WireMock, and 
         Kafka UI. WireMock is the 
         important one — it replaces 
         the real User & Org Service 
         calls in local dev. 
         If you ever see a FeignClient 
         call failing locally, check 
         the wiremock/mappings/ folder 
         first — there might be a 
         missing stub."

Step 2: mvn spring-boot:run 
          -pl expense-service 
          -Dspring-boot.run.profiles=dev
        Explanation:
        "The -pl flag means 'this project 
         only' — the repo is a monorepo 
         with multiple services, you don't 
         want to build all of them.
         The dev profile loads 
         application-dev.properties on top 
         of application.properties.
         Without it, Spring can't find 
         the DB credentials and fails 
         immediately."

Step 3: Check health endpoint
        GET http://localhost:8080/manage/health
        Explanation:
        "We check this after every startup.
         All components should be UP.
         If userOrgService shows DOWN, 
         restart WireMock:
         docker compose restart moss-wiremock
         I hit this in week 1 — 
         the stub for the health endpoint 
         was missing, I added it, 
         it's in the repo now."
```

Marta's local setup worked on the first try — including the WireMock stub you had added in month 1.

```
A small but real moment:
─────────────────────────
The PR you submitted in week 2 —
the WireMock stub and README update —
just made someone else's onboarding 
easier two months later.

You felt that.
It was a small thing.
But it was the first time you 
felt like your contribution had 
an effect beyond the immediate task.
```

---

### Day 2 — Walking Through the Codebase

The next day you spent 90 minutes on a call with Marta doing a codebase walkthrough. You had prepared for this — the night before, you drew a rough diagram of how the Expense Service was structured:

```
EXPENSE SERVICE — INTERNAL STRUCTURE
──────────────────────────────────────

HTTP Request
     │
     ▼
┌─────────────────────────────┐
│        CONTROLLER LAYER     │
│  ExpenseController.java     │
│  ReimbursementController    │
│                             │
│  - Maps URLs to methods     │
│  - Reads headers            │
│    (X-User-Id, X-Company-Id │
│     added by API Gateway)   │
│  - @Valid on request body   │
│  - Returns ResponseEntity   │
│  - NO business logic here   │
└──────────┬──────────────────┘
           │ calls
           ▼
┌─────────────────────────────┐
│        SERVICE LAYER        │
│  ExpenseService.java        │
│  ApprovalPolicyService      │
│  ExpenseValidationService   │
│  ExpenseEventPublisher      │
│                             │
│  - ALL business logic here  │
│  - @Transactional lives here│
│  - Calls repositories       │
│  - Calls FeignClients       │
│  - Publishes outbox events  │
└──────────┬──────────────────┘
           │ calls
     ┌─────┴──────┐
     ▼            ▼
┌─────────┐  ┌──────────────┐
│REPOSITORY│  │FEIGN CLIENTS │
│  Layer   │  │              │
│          │  │UserOrgFeign  │
│Expense   │  │Client.java   │
│Repository│  │              │
│Approval  │  │Calls User &  │
│StepRepo  │  │Org Service   │
│Outbox    │  │(WireMock in  │
│EventRepo │  │local dev)    │
└─────┬────┘  └──────────────┘
      │
      ▼
┌─────────────────────────────┐
│       POSTGRESQL DB         │
│                             │
│  expenses                   │
│  approval_steps             │
│  expense_audit_logs         │
│  outbox_events              │
│  reimbursements             │
└─────────────────────────────┘

SEPARATELY — async event flow:
──────────────────────────────
outbox_events table
      │
      │ (OutboxPoller runs every 100ms)
      ▼
   KAFKA
      │
      ▼
Consumed by:
  Notification Service
  Audit Service
  Payment Service
```

You walked Marta through this diagram on the call. You explained each layer and why it existed — not just what it was.

When you got to the outbox events table, Marta asked a question you didn't expect:

```
Marta:
───────
"Why write to an outbox table 
 instead of publishing directly 
 to Kafka from the service method?"
```

You knew the answer — you had learned it from reading the codebase and from a conversation with Arjun in week 4. But you noticed something interesting about the question: Marta had 4 years of experience and she was asking it too. This wasn't a beginner question. It was a good question. And knowing the answer made you feel genuinely useful — not just a guide to the file structure, but someone who understood a real architectural decision.

You explained:

```
Your explanation to Marta:
───────────────────────────
"The short answer is: you can't wrap 
 a Kafka publish inside a DB transaction.

 Think about what happens if you do 
 both directly:
 
 @Transactional
 public void approveExpense(UUID id) {
     // 1. Update DB — status = APPROVED
     expense.setStatus(APPROVED);
     expenseRepository.save(expense);
     
     // 2. Publish to Kafka directly
     kafkaTemplate.send('expense.approved', event);
 }
 
 If step 2 fails — Kafka is briefly 
 unavailable, network blip, whatever —
 your DB says APPROVED but Kafka 
 never got the event.
 Payment Service never knows.
 The expense sits approved forever 
 with no payment triggered.
 
 The outbox pattern solves this 
 by making BOTH writes go to the 
 same database in the same transaction:
 
 @Transactional
 public void approveExpense(UUID id) {
     // 1. Update DB
     expense.setStatus(APPROVED);
     expenseRepository.save(expense);
     
     // 2. Write to outbox — same transaction
     outboxEventRepository.save(outboxEvent);
     
     // Both commit together OR both rollback.
     // Atomically. PostgreSQL guarantees this.
 }
 
 Then the OutboxPoller — a separate 
 scheduled job — reads unpublished 
 events and sends them to Kafka.
 If Kafka is down, the poller retries.
 Nothing is lost because the event 
 is safely in the DB until it's published.
 
 It's a bit more complexity in the system
 but it eliminates a whole class of 
 silent data inconsistency bugs."
```

Marta nodded (you could see on the call):

```
Marta:
───────
"That makes sense. 
 So the outbox table is basically 
 a reliable buffer between your 
 transactional DB operations 
 and Kafka."

You:
─────
"Exactly. That's the cleanest way 
 to think about it."
```

This exchange mattered. Not because you taught Marta something she couldn't have figured out on her own — she would have. But because explaining something clearly to another person is the best test of whether you actually understand it yourself.

```
The teaching test:
───────────────────
If you can explain it clearly 
to someone else without notes,
you understand it.

If you can answer their follow-up 
questions without hesitation,
you really understand it.

If you get confused or vague 
when they push back,
you only thought you understood it.

You passed the test on the outbox pattern.
That told you something about 
your own understanding.
```

---

### Day 3 — The Question You Couldn't Answer

On day 3, Marta asked you something you didn't know:

```
Marta:
───────
"The approval policy is fetched via 
 FeignClient from User & Org Service 
 on every expense submission. 
 That seems like it could be slow 
 under load. Is there any caching 
 here?"
```

You looked at `ApprovalPolicyService.java`:

```java
@Service
@RequiredArgsConstructor
public class ApprovalPolicyService {

    private final UserOrgFeignClient userOrgFeignClient;

    public ApprovalPolicy getApprovalPolicy(
            UUID companyId) {

        // Direct FeignClient call — no caching
        return userOrgFeignClient
            .getApprovalPolicy(companyId);
    }
}
```

No caching. Marta was right. Every expense submission made a network call to User & Org Service to fetch the approval policy. Under load — say, a company where 200 employees all submit expenses on Monday morning — that was 200 FeignClient calls for data that almost never changed.

You didn't know why there was no caching. You didn't know if this was a known issue, a deliberate decision, or something nobody had gotten around to yet.

You told Marta honestly:

```
You:
─────
"Good question — I honestly don't know 
 if that's a known gap or a deliberate 
 decision. I can see there's no caching 
 here. Let me ask Arjun — he's been 
 here longer and would know the context."
```

You sent a message to Arjun:

```
You (Slack to Arjun):
──────────────────────
"Hey Arjun — Marta noticed that 
 ApprovalPolicyService fetches 
 from User & Org on every call 
 with no caching. Is that a known 
 gap or was there a reason caching 
 wasn't added here?"
```

Arjun replied:

```
Arjun:
───────
"Known gap. We talked about adding 
 Redis caching for approval policies 
 a few months ago but it got 
 deprioritized. It's on the backlog.
 
 For now the FeignClient call is fast 
 enough because User & Org Service 
 has its own DB cache and the payload 
 is tiny. But you're right that at 
 scale it's worth fixing.
 
 Good that Marta spotted it — 
 worth raising in the next retro 
 as a backlog item."
```

You relayed this to Marta and suggested she add it as a comment on the relevant JIRA epic.

```
What this moment taught you:
──────────────────────────────
Two things.

First: saying "I don't know, 
let me find out" is not weakness.
It is honest and efficient.
The alternative — guessing or 
making something up — would have 
given Marta wrong information 
about a system she was learning.
That's worse than admitting 
you don't know.

Second: Marta's question was better 
than anything you had asked 
in your first week.
She had 4 years of experience.
She spotted a performance gap 
in her first three days.
That was a reminder that being 
the guide this week didn't mean 
being the most knowledgeable person.
It meant knowing the system better 
than someone who just arrived.
Those are very different things.
And being honest about that 
boundary is part of doing the 
job well.
```

---

### Day 4 — Sprint Process and PR Conventions

On day 4, Marta had her first ticket assigned. Before she started, she asked you about the PR process:

```
Marta:
───────
"What's the PR process here? 
 Any specific conventions I should 
 know before I open my first one?"
```

You walked her through everything Elena had taught you — but now from memory, as things you had genuinely internalized:

```
What you told Marta:
─────────────────────

PR size:
─────────
"Maximum 400 lines changed.
 If your feature is bigger, 
 split it into multiple PRs.
 Reviewers can't give good feedback 
 on a 1,500-line PR.
 Elena is strict about this — 
 she'll ask you to split it 
 before she reviews."

PR description:
────────────────
"Three sections, always:
 1. What changed and why
 2. How to test it manually
 3. Link to the JIRA ticket
 
 The 'why' is the most important part.
 Don't just describe what you changed —
 explain the reason behind the decision.
 Reviewers shouldn't have to read 
 your mind."

Branch naming:
───────────────
"Pattern is: feature/EXP-{ticket}-{short-description}
 For example: feature/EXP-234-add-expense-validation
 The EXP- prefix is our team's JIRA prefix."

Review assignments:
────────────────────
"Your PRs will be reviewed by Elena 
 for the first few months — that's 
 standard for everyone joining.
 She leaves detailed comments.
 Don't be discouraged if there are 
 a lot of them on the first one.
 She's thorough, not harsh.
 
 You need 2 approvals to merge.
 One from Elena or Arjun (senior),
 one from anyone else on the team."

CI checks:
───────────
"The pipeline runs automatically 
 when you push. Three things must 
 pass before you can merge:
 unit tests, integration tests 
 (Testcontainers), and SonarQube 
 quality gate (80% coverage minimum).
 
 If SonarQube fails, it's usually 
 coverage. Write the missing tests 
 before asking for review — 
 don't waste Elena's time 
 on a PR that will fail CI."
```

Marta listened, asked a few clarifying questions, and then said something that stayed with you:

```
Marta:
───────
"This is really helpful.
 In my last company there was 
 no written process for any of this.
 You just figured it out by 
 watching others and making 
 mistakes. 
 
 Having someone who went through it 
 recently explain it directly 
 saves a lot of wasted energy."
```

---

### Day 5 — The First Code Review You Left

On day 5, Marta opened her first PR. Lukas asked you to review it alongside Elena:

```
Ticket: EXP-201
Title:  Add pagination to expense list 
        for finance dashboard

PR changes:
────────────
- Updated GET /api/v1/expenses 
  to accept page/size parameters
- Added Pageable to repository method
- Updated ExpenseResponse to include 
  pagination metadata
- Unit tests for new parameters
```

You reviewed it. It was clean code — better than your first PR had been. But you noticed two things:

---

**Thing 1: Missing default value handling**

```java
// Marta's controller code:
@GetMapping
public ResponseEntity<PagedResponse<ExpenseResponse>> 
        getExpenses(
        @RequestParam int page,
        @RequestParam int size,
        @RequestHeader("X-Company-Id") UUID companyId) {

    // ...
}
```

No default values on `page` and `size`. If a client called `GET /api/v1/expenses` without query parameters, Spring would throw a `MissingServletRequestParameterException` — a 400 error — because the params were required but absent.

You left a comment:

```
Your comment on Marta's PR:
────────────────────────────
"If a client calls this endpoint 
 without page/size params, Spring 
 will return a 400 because @RequestParam 
 is required=true by default.

 Should these have defaults?
 Looking at our other paginated endpoints 
 (like GET /api/v1/invoices), 
 they use defaultValue:

 @RequestParam(defaultValue = "0") int page,
 @RequestParam(defaultValue = "20") int size

 Is the intent here to require pagination 
 params explicitly, or should they be 
 optional with sensible defaults?"
```

This was your first code review comment. Notice what you did:

```
You did NOT say:
────────────────
"This is wrong."
"You need to add defaultValue."
"Bug here."

You DID say:
────────────
"Here's what will happen if..."
"Looking at how other endpoints do it..."
"Is the intent X or Y?"

Why this matters:
──────────────────
Marta had 4 years of experience.
The missing default might have been 
intentional — maybe the API contract 
required explicit pagination.
Maybe she had a reason.

A code review comment should open 
a conversation, not declare a verdict.
Especially when you're junior.
Especially on your first review.
```

Marta responded:

```
Marta:
───────
"Good catch — definitely should 
 have defaults. Didn't notice 
 this was inconsistent with 
 the other endpoints. Fixing."
```

---

**Thing 2: Something you weren't sure about**

```java
// Marta's service code:
public PagedResponse<ExpenseResponse> getExpenses(
        UUID companyId, 
        int page, 
        int size) {

    Pageable pageable = PageRequest.of(page, size);
    Page<Expense> expenses = expenseRepository
        .findByCompanyId(companyId, pageable);

    return PagedResponse.from(expenses, 
        ExpenseResponse::from);
}
```

You noticed there was no upper limit on `size`. A client could request `size=10000` and get 10,000 rows in a single response. That seemed like it could be a problem — both for performance and for memory.

But you weren't sure if this was actually an issue or if there was something else in the stack (like an API Gateway limit) that already handled it.

You left a comment — but phrased it as a question rather than a correction:

```
Your comment:
──────────────
"Not sure if this is handled 
 elsewhere, but is there an 
 upper bound on `size`?
 If a client passes size=10000, 
 that would be 10,000 rows 
 in one query and response.
 
 I see @Max(100) on the size param 
 in some other endpoints — 
 should we add that here too, 
 or is there a reason not to?"
```

Elena responded to this comment (not Marta):

```
Elena:
───────
"Good spot. Yes — we should have 
 @Max(100) here. It's our standard.
 Marta, can you add:
 
 @RequestParam(defaultValue = '20') 
 @Min(1) @Max(100) int size
 
 This validates at the controller level 
 before anything hits the service."
```

```
What this moment showed you:
──────────────────────────────
You left a comment on something 
you weren't sure about.
You framed it as a question, 
not an assertion.
You referenced what you'd seen 
in other parts of the codebase.
Elena confirmed it was valid.

This is important because:
- You didn't stay silent about 
  something that felt off
- You didn't pretend to be certain
  when you weren't
- You contributed a useful observation
  from a junior position
  without overstepping

That's the balance to strike 
when you're reviewing code as 
a junior engineer.
```

---

## The Result

```
What happened during Marta's first week:
─────────────────────────────────────────
- Local setup: done in 1 day 
  (vs your 2 days — the README 
   update and WireMock stub helped)
- First ticket: picked up day 4, 
  PR opened day 5
- PR merged end of week 1 with 
  minor fixes
- Marta settled into the team 
  without any major blockers

What you got from the experience:
───────────────────────────────────
1. You understood the outbox pattern 
   well enough to explain it clearly 
   to an experienced engineer.
   That was the real test.

2. You left two meaningful code 
   review comments on your first 
   ever review of someone else's PR.
   Neither was wrong. 
   Both improved the code.

3. You said "I don't know, 
   let me find out" clearly and 
   without embarrassment when 
   Marta asked about caching.
   That was the right call.

4. You understood the difference 
   between "guiding someone through 
   this system" and "being their 
   technical mentor."
   You operated correctly within 
   that boundary.

5. Marta thanked you at the end 
   of the week in a Slack message:
   "Thanks for the help this week —
    made a huge difference to 
    hit the ground running."
   That was the first time someone 
   thanked you for helping them 
   at work. It felt good.

What you still didn't know:
─────────────────────────────
Why there was no caching on 
the approval policy fetch.
That answer came 7 months later 
when you were the one who 
implemented it.
But that's a later story.
```

---

## The "Tricky Question" Preparation

---

**Q1: "You were two months into the job when you were asked to help onboard someone. Weren't you too junior to be doing that?"**

```
For technical mentorship — yes, 
probably too junior.
If Marta had needed someone to 
explain Spring Boot internals 
or system design decisions, 
I was not the right person.
Elena and Arjun were.

But Lukas was deliberate about 
what he asked me to do.
He asked me to be her first point 
of contact for codebase questions —
meaning: where is this file, 
how does the local setup work, 
what's the PR process, 
which Slack channel do I use.

For that specific role, 
being two months in was actually 
an advantage.
I remembered exactly what was 
confusing because I had just 
been confused by it.
Elena had been here two years — 
she no longer remembered what 
it felt like to not know where 
the WireMock stubs were.
I did.

The key is I knew my boundary.
When Marta asked about caching 
and I didn't know, I said so 
and went to Arjun.
I didn't guess or make something up.
That boundary is what made 
the arrangement work.
```

---

**Q2: "You mentioned you left code review comments. How did you feel qualified to review a more experienced engineer's code?"**

```
I wasn't reviewing whether her 
architecture was correct.
I wasn't evaluating her design decisions.
I was reading the code and asking 
questions about things that 
looked inconsistent with what 
I'd seen elsewhere in the codebase.

"Other endpoints use defaultValue 
here — should this one too?"

That's not a senior engineer 
question or a junior engineer question.
That's just a careful reader 
noticing an inconsistency.

Anyone who knows the codebase 
can do that — regardless of 
years of experience.

And the way I phrased it mattered.
I didn't say "this is wrong."
I said "is the intent X or Y?"
That opened a conversation.
It turned out to be a real issue,
but even if it hadn't — if Marta 
had said "yes, that's intentional 
because of X" — the conversation 
would have taught me something.

Code review is a conversation,
not a verdict.
I understood that early 
because I'd been on the receiving 
end of Elena's reviews.
```

---

**Q3: "What did helping Marta teach you about your own understanding of the system?"**

```
The clearest thing was the 
outbox pattern explanation.

When Marta asked why we wrote 
to an outbox table instead of 
publishing directly to Kafka,
I explained it from first principles —
the dual-write problem, why 
@Transactional doesn't cover Kafka,
how the outbox poller works.

I hadn't written that explanation 
before. I hadn't practiced it.
It just came out clearly because 
I genuinely understood it.

That was the first moment where 
I thought: 
"I actually know this. 
Not because I memorized it. 
Because I understand it."

And there's a difference.
If you memorize an explanation,
the first follow-up question 
breaks it.
If you understand it,
you can answer follow-ups 
you've never heard before.

Marta asked good follow-up questions.
I answered them without hesitation.
That told me more about my own 
understanding than any test would have.
```

---

**Q4: "You said Marta spotted a caching gap in her first three days that you hadn't noticed in two months. How did that make you feel?"**

```
Honestly? It was humbling 
in a healthy way.

My first reaction was a flash of 
"how did I miss that?"

But then I thought about it more carefully.

Marta came in with 4 years of experience,
specifically including API performance 
and caching patterns. She was primed 
to look for that kind of thing.
I was two months in, still learning 
where the files were and how 
@Transactional worked in practice.

We were looking at different things 
because we were at different stages.

What it taught me is that 
experience gives you a different 
kind of vision.
Senior engineers don't just know 
more things — they notice different 
things when they read code.
They see performance implications 
and architectural gaps that juniors 
walk past.

That's not a deficiency in the junior.
It's what growth looks like over time.
And it gave me a concrete thing 
to develop: the habit of reading 
code and asking "what happens 
to this under load?"

That question started showing up 
in my thinking from that point on.
```

---

Block 1 complete. All three stories done.

```
BLOCK 1 SUMMARY — What these three 
stories show together:
──────────────────────────────────────

Story 1 (First PR):
  How you approach feedback,
  how you read existing patterns,
  how you separate DTO vs 
  service layer concerns.

Story 2 (Onboarding shock):
  How you handle ambiguity,
  how you ask for help correctly,
  how you contribute even 
  when you're brand new.

Story 3 (Helping Marta):
  How you know your own boundaries,
  how you teach what you know,
  how you handle someone more 
  experienced than you,
  how you do code review 
  from a junior position.

Together they paint the picture 
of someone who:
- Was genuinely junior
- Handled it honestly
- Grew systematically
- Never pretended to know 
  more than they did
- Left things better than 
  they found them

That's Block 1 done.
```

---

**Ready for Block 2 — Months 4 to 6: "Building Confidence"**

The first story in Block 2 is the N+1 query bug — your first real performance story with actual numbers, Datadog evidence, and a lesson that changed how you read code from that point on.

Shall I begin?