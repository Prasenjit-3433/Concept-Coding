# Step 13: Full 4-Quarter Plan — Role, Responsibility & Contribution

---

## Overarching Narrative

You join FinVerse in August 2023 as a junior-mid NestJS backend engineer via Turing.com. You have theoretical knowledge — NestJS from a course, two portfolio projects, solid DSA — but zero production experience. You have never worked in a team, never touched a real financial system, never dealt with on-call incidents, migrations, or code reviews from senior engineers.

The 12 months are a transformation story: from someone who knew how to write code to someone who knows how to build software in a team, in production, with real consequences. The contributions stay honest to your level — you are not leading the architecture, not making high-stakes decisions alone, not on-call for a fintech system in your first year. But you are growing steadily, earning trust, and leaving a real mark.

---

## Quarter 1 (August – October 2023)
### Theme: *Drinking from the firehose*

---

### Context & Starting Point

You join a 6-person Core Product team. The codebase is already running in production. Real users, real money, real consequences. Your first day you clone the repo and it takes you two hours just to get the local Docker environment running. Lucas spends 30 minutes on a call walking you through the architecture. You take notes furiously. You understand maybe 40% of what he says.

The stack is familiar on paper — NestJS, TypeScript, PostgreSQL — but the application of it is nothing like your tutorial projects. BullMQ is something you have never heard of. GoCardless is entirely new. The Prisma schema alone has more models than everything you have ever built combined. The domain — PSD2, consent flows, MiFID II compliance — is a foreign language.

---

### Month 1: Orientation & First Contribution

**How onboarding actually works:**

Lucas runs a 2-hour recorded walkthrough of the Core Product Service codebase. You rewatch it three times. He assigns you a Notion doc — "Week 1 reading list" — covering the ADRs, the module ownership map, the PR checklist, and the team's coding conventions. You read everything. You understand about half of it on the first pass.

**First ticket: Transaction categorisation bug**

Your first ticket is deliberately small and self-contained. A transaction is occasionally showing the wrong category in the spending dashboard — a rule in the `MerchantRule` table has a regex that is too greedy and is matching merchant names it should not. The fix itself is four lines of code. But to get there, you have to:

- Understand how the categorisation engine works
- Learn how to write a Prisma query with a `where` clause on a related table
- Write a unit test for the fixed rule
- Write a PR description explaining what changed and why
- Go through your first real code review

**The first code review experience:**

Lucas reviews your PR. He approves the fix but leaves six comments. Not rejections — educational observations. One about test naming convention. One about using `findFirst` vs `findUnique` and knowing the semantic difference. One about the PR description not including reproduction steps. You feel slightly embarrassed but you learn more from those six comments than from hours of reading.

You merge your first PR in week 2.

**Second ticket: Adding a field to the SyncLog model**

Tomasz asks you to add a `transactionsDuplicate` field to the `SyncLog` Prisma model. Simple schema change. But this introduces you to Prisma migrations for the first time. You run `prisma migrate dev`, see the generated SQL, and then accidentally break your local database by running `migrate reset` when you did not need to. Tomasz helps you recover in five minutes. He explains calmly the difference between `migrate dev`, `migrate deploy`, and `migrate reset`. You write it all down in a personal Notion page you start keeping called "Things I learned the hard way."

**What you learn in Month 1:**
- How to read and navigate a large unfamiliar codebase
- How Prisma migrations work in practice versus in tutorials
- The PR review culture on the team — educational, not judgemental
- How to ask a good question: context first, what you tried, what you are stuck on
- The difference between knowing a technology and using it in production

---

### Month 2: Expanding Scope

**Third ticket: Building the sync-status endpoint**

You are assigned `GET /v1/accounts/:accountId/sync-status` — your first endpoint written mostly from scratch. It is not complex but it is real: ownership check, database query, response mapping. You have to make sure it never returns sensitive fields and never exposes one user's data to another.

Lucas reviews it. Two rounds of feedback.

First round: you forgot the ownership check — a user could query any account ID and get data back. This is a security bug. Lucas flags it in review before it merges. He does not make a big deal of it — just: "this is missing the userId filter, which means any authenticated user can query any account. Always filter by userId on every query that returns user-specific data." You add the `where: { userId }` clause and immediately understand why it is non-negotiable in a financial application.

Second round: you used `include` instead of `select` in the Prisma query — returning 20 fields when the mobile app needs 6. Lucas explains payload efficiency for mobile clients, referencing the bandwidth constraints of a user on 3G in rural Germany.

**Fourth ticket: Budget alert deduplication fix**

A bug in production — users are getting duplicate budget alert emails when two transactions arrive in quick succession for the same category. You are asked to investigate and propose a fix.

This is the first time you use Datadog to investigate a real bug. You filter logs by the affected user's ID and find the pattern — two `budget.threshold.exceeded` events published within milliseconds of each other, both processed by Notification Service before the deduplication logic could catch either. The fix involves adding a Redis-based deduplication key in the Notification Service consumer.

But the Notification Service is not your codebase — Tomasz maintains it informally. So you have to coordinate: you explain the bug to Tomasz on Slack, propose the fix, and he reviews and approves it before you open the PR. You open a PR on a codebase you have never touched before. The fix is 15 lines. The coordination teaches you that in a team, the social process around a fix is often more complex than the fix itself.

**What you learn in Month 2:**
- Security-first thinking — ownership checks are non-negotiable in financial data
- Mobile-first API design — payload efficiency matters
- Cross-team coordination — how to propose a fix in someone else's codebase
- Using Datadog logs to investigate a real production bug for the first time

---

### Month 3: Module Handover

**The Accounts & Open Banking handover:**

Daniel and Lucas decide to give you formal ownership of the Accounts & Open Banking module. Elena has been informally maintaining it alongside her Investing module work. She runs a dedicated 90-minute handover session covering:

- The GoCardless requisition and consent flow end to end
- How BullMQ sync jobs are structured — your first real BullMQ exposure in production context
- Known edge cases: banks that return malformed IBAN data, GoCardless rate limits, accounts that fail reconnection after consent expiry
- The `consentExpiresAt` tracking and why it matters for PSD2 compliance
- The `SyncLog` model and how to read it when users report sync issues

You shadow the module for one week — Elena handles anything that comes up but you observe everything, ask questions, and read every related PR.

Then you take ownership.

**First ownership investigation:**

Within days of taking the module, a user reports their Deutsche Bank accounts stopped syncing. You investigate in Datadog — find sync logs showing `status: FAILED` and `errorMessage: "GoCardless: account access revoked"`. The consent window expired and the user was never notified to reconnect.

You trace the issue: a BullMQ repeatable job that should check for expiring consents was not re-registered after a recent deployment. You bring the finding to Lucas — you explain what you found, walk him through the Datadog evidence, and propose the fix. He reviews your proposal and approves it. You fix the repeatable job registration and write a test to verify it fires on schedule. You do not deploy this alone — Lucas reviews and approves the PR, and the deployment happens with him watching the Datadog dashboard.

**The deployment incident:**

In month 3, during a routine deployment, you push a change to the `handleCallbackSuccess` method in the Accounts module. The change looks correct in isolation. But you did not test the full GoCardless callback flow locally because setting up a GoCardless sandbox is tedious.

In staging, two users who connect their bank accounts after your deployment get stuck — their connections stay in `PENDING` status forever. Lucas notices the `bank.connection.completed` event is missing from the outbox in the staging database. He pings you on Slack. You investigate together — your change accidentally removed the `outboxEvent.create()` call inside the `$transaction` block. The event was never written, so the outbox publisher never picked it up, and Notification Service never received the connection completion event.

Lucas does not fix it for you. He says: "you found the cause — now write the fix, write a test that would have caught this, and we will deploy it together." You do exactly that. The fix goes to production the next day. Afterwards, Lucas runs a brief retrospective with you: "always test the full callback flow end to end, not just the code path you changed." You write a personal checklist that you follow for every PR in this module from that point forward.

**What you learn in Month 3:**
- What real module ownership feels like
- How to bring a finding to a senior engineer — come with evidence and a proposed fix, not just the problem
- Testing the full flow, not just the changed code path
- That production deployments in fintech are deliberate, supervised events — not solo pushes

---

### Q1 Key Milestones
- 4 PRs merged across Core Product and Notification Service
- First real code review cycle — learned from 6 structured comments
- First production bug investigated using Datadog logs
- First security vulnerability caught in review before production
- Accounts & Open Banking module ownership handed over
- First post-deployment incident — caused, investigated with Lucas, fixed

### Q1 Challenges
- Feeling overwhelmed by codebase size and financial domain complexity
- Not knowing what you do not know — asking questions that were too vague early on
- The deployment incident — first taste of real production consequences

### Q1 STAR Stories
1. The first code review — six comments that taught more than hours of reading
2. The ownership check bug — security vulnerability caught in review, not in production
3. The missing outbox event incident — what happened, what you did together with Lucas, what you learned

---

## Quarter 2 (November 2023 – January 2024)
### Theme: *First real ownership — building, not just fixing*

---

### Context

You now officially own the Accounts & Open Banking module. The initial nervousness is fading but you remain aware that real users and real money depend on this module. Tomasz is your primary daily collaborator. Lucas is your reviewer but gives you more space — he is seeing whether you can hold up the module independently, with him as a safety net rather than a guide rail.

---

### Month 4: Building Features Under Ownership

**Feature: Proactive consent expiry notifications**

Your first significant feature built end to end. The product requirement: users whose GoCardless consent is expiring within 7 days should receive a reminder notification so they can reconnect before sync stops working.

You own the design. You propose the approach in a Slack thread — a BullMQ repeatable job running daily, scanning `bank_connections` for `consentExpiresAt` within 7 days, and publishing a `bank.connection.expiring.soon` event via the outbox pattern.

Lucas reviews your proposal asynchronously. He asks one question: "what happens if the same user gets notified on consecutive days?" You had not thought of this. You update the proposal — add a Redis deduplication key (`consent:expiry:notif:{connectionId}:{expiryDate}` with a 24-hour TTL) so a user gets at most one notification per expiring connection per day.

You build it across four PRs. Lucas approves all four with minor comments. The feature ships to production.

**Sprint planning estimation:**

During sprint planning for this feature, you are asked to estimate story points. You say 3. Lucas gently asks: "does that include the migration, the staging smoke test, and the Notification Service coordination?" You realise you only estimated the happy-path code. You revise to 5. It takes 6 days. From this point you always count the full lifecycle of a change when estimating — not just the lines of code.

**The retry logic discussion with Tomasz:**

Tomasz proposes reducing max retries for sync jobs from 3 to 2 for GoCardless errors during off-peak hours, to reduce noise in the failed jobs queue. You read the PR and have a concern. You leave a comment: "if GoCardless has a 5-minute outage at 2am, a job with 2 retries and 10 second backoff fails permanently, but 3 retries would survive it — especially for users with slow bank APIs."

Tomasz responds respectfully: "fair point technically, but the noise makes monitoring harder." You both bring it to Lucas on Slack. You present both perspectives. Lucas agrees with your technical concern but validates Tomasz's operational point. The outcome: keep 3 retries but add an `error_type` tag to the failed job metric so the Datadog monitor can distinguish transient from persistent failures — reducing noise without reducing safety.

This is your first technical disagreement handled professionally. You made your case with a specific example, not emotion. The resolution was better than either original proposal.

**What you learn in Month 4:**
- End-to-end feature ownership — from Slack proposal to production
- Sprint estimation includes the full lifecycle, not just the code
- Technical disagreement handled with evidence and collaboration

---

### Month 5: Performance Investigation

**The stalled sync jobs investigation:**

A Datadog alert fires: p95 for `INITIAL_SYNC` duration is 28.4 seconds — dangerously close to the 30-second BullMQ lock TTL. Stalled jobs are accumulating at a rate of around 12% for users with 3 or more connected bank accounts.

This is your module. You own the investigation — but you do not own the fix alone.

You open Datadog APM, filter for failed `INITIAL_SYNC` spans, and find the pattern: every stalled job has `bullmq.attempts_made: 2` and `error.message: "Job stalled"`. Not a GoCardless error. A BullMQ lock expiry. You look at the correlated logs for a specific stalled job — accounts are being processed sequentially. Account 1 finishes, then account 2 starts. A user with 4 accounts is paying for four sequential GoCardless calls plus four sequential bulk inserts. Total time: 28 to 34 seconds.

You bring the full finding to Lucas: here is the Datadog evidence, here is the root cause, here is my proposed fix — extend `lockDuration` to 60 seconds, add `job.updateProgress()` between accounts to trigger lock renewal, and refactor the loop to use `Promise.all()` for concurrent account fetching.

Lucas asks one good question: "if you use `Promise.all()`, how do you stay within GoCardless's rate limit?" You had already thought about this — the `limiter` config on the BullMQ worker handles it. He approves the approach.

You implement the fix. Lucas reviews and approves. After deployment:

- Stalled job rate drops from 12% to under 0.3%
- p95 sync duration drops from 28.4 seconds to 11.2 seconds
- Measured in Datadog over a 24-hour window before and after, using the `finverse.transaction_sync.duration.ms` histogram

This is the performance improvement story you tell in interviews: specific metric, specific before number, specific root cause found through Datadog, specific fix, specific after number, measured properly.

**What you learn in Month 5:**
- How to own a performance investigation end to end
- How to use Datadog APM traces and metrics together to find root cause
- How to present a finding to a senior with a proposed solution — not just a problem

---

### Month 6: Cross-Module Work & Growing Code Review Skill

**Feature: Net worth balance aggregation**

Product asks for a net worth dashboard showing the user's total cash position across all connected accounts. This requires coordination between your Accounts module (owns balance data) and the Goals module (Arjun's ownership, which shows savings balances).

You work with Arjun directly for the first time on a cross-module feature. You agree on an API contract — your module exposes `GET /v1/accounts/net-worth` which the frontend consumes for the cash component. You write the endpoint. Arjun writes the aggregation logic on his side. You review each other's PRs.

Reading Arjun's code teaches you something new: he uses cursor-based pagination for the goal contributions list — a pattern you have not used before. You ask him to walk you through it on a quick call. He does. You use the same pattern in a subsequent endpoint you build. Learning from peers' code becomes a deliberate habit.

**Code review growth:**

By month 6, Lucas includes you in the review pool for Arjun's smaller PRs. Your early reviews are surface-level — style, naming, obvious issues. But you catch one real bug: Arjun queries a user's goals without a `userId` filter, relying solely on the `goalId` from the URL. This is the exact same ownership check mistake you made in month 2. You flag it. Arjun adds the fix. You realise your own mistakes became your most reliable checklist items.

**What you learn in Month 6:**
- Cross-module API contract design and coordination
- Learning deliberately from reading peers' code
- How your own past mistakes become pattern recognition in reviews

---

### Q2 Key Milestones
- First significant feature built end to end — consent expiry notifications
- Performance investigation owned independently — p95 28.4s to 11.2s, measured in Datadog
- First technical disagreement handled with evidence and collaboration
- Started giving code reviews that catch real security bugs

### Q2 Challenges
- Underestimating story points in sprint planning — learned the full lifecycle counts
- The performance investigation — ownership anxiety when the stalled job alert fired on your module
- The Tomasz discussion — learning that "pushing back" is not the same as being difficult

### Q2 STAR Stories
1. The performance investigation — stalled jobs, Datadog trace analysis, concurrent fix, before and after metrics
2. The retry logic discussion — technical pushback with evidence, collaborative resolution
3. The consent expiry feature — first end-to-end feature ownership with a design proposal

---

## Quarter 3 (February – April 2024)
### Theme: *Thinking beyond your own ticket*

---

### Context

You are seven months in. The module ownership anxiety has mostly faded. You know the Accounts & Open Banking module deeply. You are the person Tomasz asks first when something in the sync pipeline looks odd. Lucas's review comments on your PRs are getting shorter — not because he is less thorough, but because there is genuinely less to correct.

Something shifts in Q3. You start noticing things outside your assigned ticket and saying something about them.

---

### Month 7: Proactive Contribution

**Proactively surfacing the Redis stale balance bug:**

While working on an unrelated task, you notice something odd. The Redis cache key for account balances has a 1-hour TTL. But GoCardless balance data updates every time a sync runs — potentially every 4 hours. In the worst case a user's balance in the cache is stale for up to 1 hour even after a fresh sync. The sync worker updates the balance in PostgreSQL but does not invalidate the Redis cache. The next read serves the stale cached value.

This is not breaking anything visibly right now. But it is a latent inconsistency waiting to become a user complaint. You raise it in a Slack message to Lucas and Tomasz: "noticed something that is not breaking now but will cause confusion — wanted to flag it before it becomes a support ticket." You describe the issue clearly and propose the fix: invalidate the relevant Redis key in the sync worker when the balance update completes.

Lucas responds: "good catch. Write the ticket, assign it to yourself, fix it." You do. The fix is 3 lines. But the act of noticing and raising something outside your assigned work matters. Lucas mentions it in your mid-year check-in: "you are starting to think like someone who owns the system, not just their module."

**Priya joins the team:**

Priya Nair joins as a junior engineer in month 7. She is officially mentored by Lucas and Isabelle. But she naturally gravitates toward you and Arjun because you are closer in seniority and in timezone.

She asks you questions on Slack. You do not give answers immediately. You start doing what Lucas did with you: ask what she has tried, what she has read, what she thinks the problem is. You realise without planning it that you have absorbed Lucas's mentoring approach through 7 months of receiving it.

Her first PR touches familiar territory. You are added as a reviewer. Your review catches a missing DTO validation, a query that could be simplified, and a place where she is using `include` instead of `select` — the same pattern Lucas caught on you in month 2. You write structured comments explaining the reasoning behind each suggestion, not just flagging the issue. You also leave one positive comment on a clean piece of logic she wrote. You remember that balanced feedback is more useful than only critical feedback.

**What you learn in Month 7:**
- The difference between doing your ticket and owning the system
- Mentoring instinct develops naturally from having been well mentored
- Proactive communication is a deliberate choice you can make every sprint

---

### Month 8: Incident Shadowing & Cross-Team Collaboration

**Shadowing Lucas during the RabbitMQ consumer lag incident:**

On a Tuesday afternoon the #incidents Slack channel fires. Multiple users report budget alerts arriving 20 to 30 minutes late. Lucas is on-call. He immediately invites you to shadow him — "good learning opportunity, come watch."

You join the incident call and watch everything Lucas does. He opens Datadog, checks queue depth first — `budget-notifications-queue` is at 4,800 messages when it should be near zero. He checks the Notification Service consumer logs next — filters by `error.type` as a Datadog facet. Within two minutes he surfaces 847 matching error lines. The error: a SendGrid API change broke the email template rendering, causing each attempt to throw and retry. The consumer was spending most of its time in retry backoff rather than processing.

You observe Lucas's thought process out loud. He narrates: "I check queue depth first because that tells me if it is a consumer problem or a publisher problem. Queue is deep, consumers are running but slow — so the problem is inside the consumer, not upstream." You write this mental model down.

Tomasz patches the SendGrid template. Queue drains within 15 minutes. Total incident duration: 40 minutes.

After the incident, Lucas runs a brief debrief with you: "what would you have done differently?" You say you would have looked at the consumer logs first, skipping queue depth. He explains why queue depth first is faster — it immediately separates producer problems from consumer problems without reading a single log line. You remember this permanently.

The post-mortem action item is to add a Datadog monitor for `budget-notifications-queue` depth exceeding 500. Lucas asks you to write the monitor configuration as a follow-up task. You write it, he reviews it, it gets added to the production monitoring setup.

**The notification race condition — your investigation:**

Lucas assigns you to investigate a support ticket pattern he flagged: users occasionally receive budget alert push notifications before the triggering transactions appear in their transaction list. The investigation involves your Accounts module and the notification pipeline — you are well-placed for it.

You trace the issue through Datadog correlated logs. You find the race condition: the sync worker publishes the `budget.threshold.exceeded` event directly to RabbitMQ immediately after the bulk insert commits, but the PostgreSQL read replica has not caught up. Users who open the app within 1 to 2 seconds of receiving the notification see stale data from the replica.

You propose the Outbox pattern as the fix — write the event to `outbox_events` in the same transaction as the bulk insert. The outbox publisher polls every 5 seconds, by which time the replica has caught up.

Lucas reviews the design. He asks: "what happens when we scale to 3 API containers?" You had not thought through the multi-container scenario fully. Three containers each running a `setInterval` polling loop would publish the same event three times. Lucas walks you through it. The fix: use a BullMQ repeatable job with a singleton `jobId` instead of `setInterval`. Only one container wins the job at a time.

You go back, revise the design, implement it. Lucas reviews and approves. Zero recurrences of the race condition in the two weeks post-deployment, confirmed through Datadog log filtering.

**What you learn in Month 8:**
- How a senior engineer thinks through an incident — queue depth before logs, producer before consumer
- Incident shadowing is one of the most efficient ways to learn production thinking
- Multi-container design considerations — you cannot assume a single process anymore
- The importance of revising a design when a gap is pointed out rather than defending the original

---

### Month 9: Growing Confidence & Giving Back

**Contributing to the Series B preparation session:**

In month 9, Sophie runs an engineering-wide session on technical preparation for Series B. Lucas asks you to contribute two slides on the Accounts & Open Banking module — current sync reliability metrics, known limitations, and what would need to change to handle 1.5 million users.

This is the first time you present technical content in a broader forum beyond your immediate team. You prepare the slides. Lucas reviews them. He makes one significant change — your original framing was "we have these problems." He reframes it as "here is our current baseline and here is what we would invest in." You understand the difference immediately: one is a complaint, the other is a plan.

You present for 8 minutes. Sophie asks one question: "how confident are you in the sync reliability numbers?" You say: "very — these come directly from Datadog, this is 30-day p95 across all user cohorts." She nods. After the meeting Lucas says: "that was good." Not excessive praise — just a clear signal that you handled it well.

**Giving a structured review to Priya:**

Priya submits her first non-trivial PR — around 180 lines for a lesson progress tracking endpoint. You are asked to review it alongside Isabelle. Your review takes 45 minutes. You find: a missing composite index on `(userId, courseId)` that would cause a slow query at scale, a DTO validation gap, and an incorrect HTTP status code (200 instead of 201 for resource creation).

You write structured review comments. Not "this is wrong" but "this is wrong because X, here is how to think about it." You reference the same index reasoning Lucas explained to you from the database module. You also leave a positive comment on the clean structure of her response mapping.

The PR merges after one revision round. Priya messages you on Slack: "your review was really helpful, especially the index explanation." You remember Lucas leaving you a similar message after your first thorough review. The cycle continues.

**What you learn in Month 9:**
- How to frame technical content for leadership — problems as plans, not complaints
- Giving structured educational reviews is a skill that compounds — what you learned becomes what others learn
- Seeing your own growth through watching someone else start where you started

---

### Q3 Key Milestones
- Proactively identified and fixed a latent Redis cache consistency bug outside your assigned work
- Shadowed Lucas through a full incident — learned the mental model for production debugging
- Designed and implemented the Outbox pattern fix for notification race condition
- Contributed to Series B preparation — presented metrics to VP Engineering
- Became an informal mentor to Priya — giving structured educational code reviews

### Q3 Challenges
- The Outbox multi-container design gap — thinking the design was complete, discovering a flaw, revising without defensiveness
- The incident shadowing — absorbing a lot of new production thinking very quickly
- The Series B presentation — framing technical content for leadership for the first time

### Q3 STAR Stories
1. The notification race condition — investigation, Outbox design, multi-container gap, revised design, zero recurrences
2. Incident shadowing with Lucas — the RabbitMQ consumer lag, the mental model learned, the monitoring improvement contributed
3. The Series B presentation — reframing problems as plans, presenting real metrics to leadership

---

## Quarter 4 (May – August 2024)
### Theme: *From executor to contributor — earning your place*

---

### Context

You are in the final three months of your contract. You are not the newest engineer anymore — Priya joined 8 months ago. Your PRs merge faster. Your reviews catch real issues. Your module runs reliably. Lucas still reviews your significant PRs but his comments are mostly approvals with small suggestions rather than structural corrections.

You consciously choose to finish strong — to leave the module in better shape than you found it, to be someone the team would remember as a net positive.

---

### Month 10: Handling Production Pressure — Shadowing the GoCardless Rate Storm

**The GoCardless rate limit storm — shadowing Lucas:**

A marketing campaign brings 8,000 new users over 48 hours. Each connects their bank account, triggering an `INITIAL_SYNC` job. The queue depth spikes to 8,000. ECS auto-scaling launches 8 worker containers. Within two minutes, the GoCardless error rate monitor fires at 23%.

Lucas is on-call. He pulls you in again — "this one involves your module directly, come watch."

You join the incident channel and watch Lucas work. First action: pause the `transaction-sync` queue from Bull Board immediately. GoCardless errors drop to zero within 30 seconds. Lucas narrates: "pause first, diagnose second — never try to fix a running fire."

He then diagnoses: 80 concurrent jobs across 8 containers are all hitting GoCardless simultaneously, and the retry backoff has no jitter — so all 80 failed jobs retry at exactly the same intervals, creating synchronised burst waves that immediately saturate the GoCardless rate limit again.

Lucas proposes two fixes: a worker-level rate limiter (`max: 30 per 10 seconds per container`) and exponential backoff with jitter. He also lowers the ECS auto-scaling maximum container count from 10 to 3 — with the rate limiter, 3 containers stay comfortably within GoCardless rate limits.

He asks you to write the code changes for the rate limiter and jitter backoff while he handles the ECS scaling config. You implement both. He reviews your changes on a video call, approves them, and deploys with you watching.

Queue resumes. The 8,000-job backlog drains over 6 hours with zero further GoCardless errors. Lucas writes the post-mortem. He asks you to write the "technical root cause" section since you understood and coded the fix. You write it. He approves it with minor edits.

This is the first time you contribute meaningfully to writing a post-mortem. Not owning it — contributing a section under Lucas's guidance.

**What you learn in Month 10:**
- Pause first, diagnose second — the production incident mental model from Lucas
- The thundering herd problem — why jitter in retry backoff matters
- Contributing to a post-mortem section — clear, blameless, precise technical writing
- Watching how a senior engineer stays calm and methodical under live production pressure

---

### Month 11: Technical Depth & Knowledge Sharing

**Observability improvements — self-directed:**

You notice gaps in the existing Datadog dashboard for the sync pipeline. There is no metric for per-bank error rates, no alert for users who have not synced in more than 8 hours despite having active connections, and no visibility into which GoCardless institution IDs have the highest failure rates.

You raise it in the team Slack: "I want to spend a couple of days improving the sync observability dashboard — I think it will help us catch issues faster and reduce time-to-root-cause during incidents. Can I take this as a self-assigned task?" Lucas approves.

You spend three days adding the metrics, building the new dashboard section, and writing two new Datadog monitors. You present it at the sprint review. The product manager immediately asks: "can we use the per-bank error rate data to proactively contact users whose sync is failing?" Lucas says: "yes — that is exactly what this unlocks."

The dashboard section you built becomes part of the standard weekly engineering health check that the team reviews every Monday. It is still running after you leave.

**Writing the module documentation:**

You write the first comprehensive internal documentation for the Accounts & Open Banking module — a Notion page covering:

- The GoCardless integration flow end to end
- Known edge cases: banks that return malformed IBAN data, consent expiry handling, accounts that fail to reconnect
- How to read sync logs and interpret `SyncLog` records
- How to replay failed BullMQ jobs from Bull Board
- A runbook for the three most common alert scenarios

You write it asking yourself: "what would I have needed in week 1?" Lucas reviews the document and adds one comment: "this is exactly what was missing. Good work." The document gets linked in the team's onboarding Notion page. When a new engineer joins after you leave, this document is part of their week 1 reading list.

**What you learn in Month 11:**
- Taking initiative on improvements without being asked — and asking permission framed as a proposal
- Observability improvements compound: one dashboard change unlocks a product idea
- Documentation is a contribution that multiplies the impact of everything else you did

---

### Month 12: Finishing Strong

**Final sprint: Consent re-authentication flow improvement:**

You pick up a ticket that has been in the backlog for months — improving the consent re-authentication experience. When a user's GoCardless consent expires, the current flow requires them to navigate to settings, find the bank, and manually trigger reconnection. The product team wants a proactive inline prompt in the app.

The backend work is yours: modify `GET /v1/accounts` to include `consentExpiresAt` and a `needsReconnection` boolean flag with a 7-day warning threshold. Update the existing consent-check BullMQ job to publish a `bank.connection.reconnection.required` event when the threshold is crossed, so Notification Service can send the reminder push.

This is familiar territory — the same patterns you have been building all year: outbox event, BullMQ repeatable job, Notification Service coordination. You build and ship it in the final week of your contract. No drama. Clean PR. One round of Lucas's review. Merged.

**The final 1:1 with Daniel:**

Daniel runs your closing 1:1. He says: "the feedback from Lucas and the team is consistently positive. You came in knowing NestJS from tutorials. You are leaving having owned a production module in a regulated fintech system, contributed to performance improvements that are still running, and helped build observability that the team uses every week." He asks if you would consider renewing if budget opens. You say yes.

Lucas sends a short Slack message on your last day: "Good working with you. Keep building things."

**What you learn in Month 12:**
- Finishing strong is a choice — it signals the kind of engineer you are
- Leaving things better than you found them is the most honest measure of a good contract
- The work you are proudest of — the documentation, the observability dashboard, the race condition fix — none of it was on your original job description

---

### Q4 Key Milestones
- Shadowed Lucas through the GoCardless rate storm — contributed the code fix for rate limiter and jitter backoff
- Wrote the technical root cause section of the post-mortem under Lucas's guidance
- Built observability improvements self-directed — became part of the team's weekly health check
- Wrote comprehensive module documentation — became part of onboarding for future engineers
- Shipped the consent re-auth flow improvement in the final sprint

### Q4 Challenges
- The rate storm — working fast under live production pressure while staying accurate
- Post-mortem writing — precise, blameless, technical language under time pressure
- Finishing strong when the natural instinct is to wind down

### Q4 STAR Stories
1. The GoCardless rate storm — shadowing Lucas, contributing the code fix, writing the post-mortem section
2. Observability improvements — self-directed, presented at sprint review, unlocked a product idea
3. Module documentation — writing for the engineer who comes after you

---

## Summary: The Full 12-Month Arc

| Quarter | Theme | Defining Moment | Growth Evidence |
|---|---|---|---|
| Q1 | Onboarding and finding footing | Deployment incident — caused, investigated with Lucas, fixed | Went from following instructions to owning an investigation |
| Q2 | First real ownership | Performance investigation — p95 28s to 11s | Went from fixing things to improving systems with measured results |
| Q3 | Thinking beyond your ticket | Notification race condition fix and incident shadowing | Went from module owner to system thinker |
| Q4 | Earning your place | Shadowing the rate storm, writing the post-mortem section, documentation | Went from contributor to someone the team relied on and remembered |

---

## Cross-Cutting Threads — How They Evolve Across All 4 Quarters

| Thread | Q1 | Q2 | Q3 | Q4 |
|---|---|---|---|---|
| **Mentorship received** | Lucas reviews every PR, Elena hands over module | Lucas gives more space, Tomasz daily collaborator | Lucas reviews designs, walks through gaps | Lucas brings you into incidents, asks you to contribute sections |
| **Mentorship given** | None — too new | Informal tips to Arjun | Structured reviews for Priya, answering questions | Priya relies on you, documentation helps future engineers |
| **Mistakes and recovery** | Deployment incident — missing outbox event | Sprint estimation too low — learned full lifecycle | Outbox multi-container gap — caught by Lucas, revised without defensiveness | Post-mortem writing — learned blameless precision |
| **Technical depth** | Bug fixes, schema changes, first endpoint | Feature ownership, performance investigation | Cross-module design, race condition fix | Incident contribution, observability, documentation |
| **Production thinking** | None — everything supervised | Starting to think about edge cases | Thinking about multi-container scenarios | Thinking about the team after you leave |
| **Soft skills** | Asking better questions, first cross-team coordination | Sprint planning, technical disagreement | Incident shadowing, leadership presentation | Post-mortem writing, self-directed proposals |
| **Code reviews** | Receiving and learning | Giving surface-level, catching ownership bug | Giving structured educational reviews | Reviews that catch issues Priya carries forward |

---