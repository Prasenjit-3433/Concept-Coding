# Month 10: Working Under Pressure

---

## Theme: *The first time someone counted on you during a fire*

---

## Foundational Knowledge: What You Need Before These Stories

Two concepts come up directly in Month 10 that are worth understanding before the stories. Without this context, the actions will read as routine technical work. With it, you will understand why the stakes felt different this time.

---

### Concept 1: The Thundering Herd Problem — Why Retry Backoff Without Jitter Is Dangerous

You learned retry backoff in the BullMQ module. Exponential backoff means: wait longer between each retry attempt. First retry after 5 seconds, second after 10, third after 20. The idea is to give the failing system time to recover before hitting it again.

The problem is what happens when hundreds or thousands of jobs all fail at exactly the same moment — which is precisely what happens during a downstream outage.

```
THUNDERING HERD — THE PROBLEM VISUALISED

Scenario: GoCardless goes down at 14:00:00

T+0s:   80 concurrent jobs hit GoCardless simultaneously
         All receive 503
         All fail and enter retry logic

Exponential backoff — fixed intervals:
T+5s:   All 80 jobs retry simultaneously (attempt 2)
         GoCardless still recovering — 80 requests at once
         All receive 503 again — GoCardless overloaded again

T+10s:  All 80 jobs retry simultaneously (attempt 3)
         GoCardless almost recovered — 80 requests hit at once
         GoCardless tips back over the edge — 503 again

T+15s:  All 80 jobs exhausted retries → 80 permanent failures
         GoCardless is now healthy but nobody is talking to it

THE PROBLEM:
  Synchronised retries recreate the original burst
  that caused the problem in the first place.
  You are repeatedly hitting the system at exactly
  the moment it is trying to recover.
```

Jitter solves this by randomising the retry window. Instead of every job waiting exactly 5 seconds, each job waits a random amount between 0 and 5 seconds. The burst of 80 simultaneous retries spreads out into a trickle of individual requests arriving over a 5-second window. The recovering system handles a trickle. It would be overwhelmed by a burst.

```
EXPONENTIAL BACKOFF WITH JITTER — THE FIX

T+0s:   80 concurrent jobs fail

Each job calculates its own random delay:
  Job 1:  waits 1.2s → retries at T+1.2s
  Job 2:  waits 3.7s → retries at T+3.7s
  Job 3:  waits 0.8s → retries at T+0.8s
  Job 4:  waits 4.1s → retries at T+4.1s
  ...

Result: instead of 80 requests at T+5s,
        you get ~16 requests per second for 5 seconds
        A trickle, not a burst
        GoCardless recovers and handles the trickle cleanly
```

This is not a theoretical concern. It is one of the most common patterns behind cascading failures in distributed systems. When you read about major cloud outages — AWS, Stripe, Twilio — the post-mortems frequently describe this exact pattern: a system partially recovers, gets hit by a retry burst, tips back over, and the cycle repeats.

---

### Concept 2: The Incident Mental Model — Stabilise Before You Diagnose

In Month 8, you learned Lucas's first-response mental model for incidents: queue depth before logs, producer before consumer. Month 10 adds a layer on top of that.

The most important rule when a live production system is actively failing:

**Stabilise the system before you try to understand it.**

This sounds obvious. In practice, it is the thing most engineers get wrong in their first few incidents. The instinct is to understand — to read logs, find the root cause, fix the bug. That instinct is exactly right for every other engineering problem. In a live incident, it is wrong.

```
WHY "UNDERSTAND FIRST" FAILS IN LIVE INCIDENTS

System is actively failing right now.
Users are getting errors right now.
Every second you spend reading logs is another second of failures.

If you spend 20 minutes finding the root cause,
then deploy a fix,
then wait for deployment to complete:
  Total downtime: 25-30 minutes

If you spend 2 minutes stabilising (pause the queue, reduce load),
then spend 20 minutes finding the root cause in a stable system,
then deploy the actual fix:
  Total downtime: 2-5 minutes

Stabilisation is not giving up on understanding.
It is creating the space to understand clearly
without the pressure of live failures compounding.
```

The sequence Lucas follows — and what you will internalise in Month 10 — is:

```
INCIDENT RESPONSE SEQUENCE

Step 1: STOP THE BLEEDING
  Reduce or eliminate the immediate harm to users.
  Pause the queue. Reduce concurrency. Roll back.
  Whatever stops new failures from accumulating.
  This step takes minutes, not hours.

Step 2: UNDERSTAND
  Now investigate with a stable system.
  Read logs. Look at traces. Find the root cause.
  You have time. The bleeding has stopped.

Step 3: FIX PROPERLY
  Deploy the real fix — not a rushed guess.
  Verify in staging first if possible.
  Deploy with monitoring watching.

Step 4: RESTORE
  Resume the queue. Replay failed jobs.
  Verify the system is healthy.
  Document what happened.
```

You will watch Lucas execute this sequence with remarkable calmness during the GoCardless rate storm. What will strike you most is not the technical knowledge — it is the deliberateness. There is no panic. There is a sequence, and he follows it.

---

## The Stories

---

### Story 1: The GoCardless Rate Storm

**Background:**

In Month 10, FinVerse runs a marketing campaign that acquires 8,000 new users over 48 hours — significantly faster than any previous growth period. Each new user connects their bank account, triggering an `INITIAL_SYNC` BullMQ job. The `transaction-sync` queue, which normally holds 50 to 200 jobs at any given time, climbs to 8,000 within hours.

ECS Auto Scaling sees the queue depth metric breach its threshold. It launches additional worker containers — from the usual single container to eight. What happens next is a failure mode the team had discussed but never experienced at this scale.

You are in the middle of writing documentation for the module when the `#incidents` Slack channel fires.

---

**S — Situation:**

It is a Tuesday afternoon in Month 10. The `#incidents` Slack channel notification appears on your screen. The message is from Datadog: "ALERT: GoCardless API error rate 23.4% — threshold 5%." Lucas, who is on-call this week, responds within two minutes: "on it." Then, thirty seconds later, a direct message to you: "this involves your module. Want to watch? Join the call."

You join within a minute. Lucas has already opened Datadog and is sharing his screen.

The situation when you arrive: the `transaction-sync` queue has 7,200 jobs still waiting. Eight worker containers are running, each with concurrency 10 — 80 jobs processing simultaneously. GoCardless is returning 429 errors on almost every API call. The retry storm has begun — failed jobs are retrying on fixed exponential backoff intervals, which means waves of 80 simultaneous retry attempts are hitting GoCardless every few seconds. GoCardless, still recovering, gets hit by each wave and fails again.

The failed job counter in Datadog is climbing. Users who connected their banks during the campaign are not seeing their transactions appear.

---

**T — Task:**

You are there to observe. But Lucas makes it collaborative almost immediately — he narrates his reasoning out loud, asks you questions to confirm his diagnosis, and explicitly assigns you the code changes while he handles the infrastructure configuration. You are not a bystander. You are a contributor under supervision.

---

**A — Action:**

**Lucas's first move — stabilise, do not diagnose:**

Lucas opens Bull Board before he looks at a single log line. He navigates to the `transaction-sync` queue. He says: "first thing — stop the bleeding. The queue can wait. The jobs are not going anywhere. But every retry we send right now is making GoCardless worse."

He pauses the `transaction-sync` queue from Bull Board. The UI shows "Paused" within seconds. All workers stop picking up new jobs. No new GoCardless calls are being made.

You watch the Datadog error rate graph. Within 90 seconds of the pause, GoCardless error rate drops from 23.4% to zero. The system stabilises.

```
THE PAUSE EFFECT ON DATADOG

GoCardless error rate graph:

25% ┤  ╭─╮╭─╮╭─╮
20% ┤ ╭╯ ╰╯ ╰╯ ╰╮
15% ┤╭╯            ╰╮
10% ┤╯               ╰╮
 5% ┤                  ╰╮
 0% ┼─────────────────────╰──────────────────────►
    14:00              14:07
                              ↑
                         Queue paused here
                         90 seconds later: zero errors
```

Lucas says: "good. Now we can think." The contrast with what you were expecting — immediate investigation, frantic log reading — is striking. He paused first. Deliberately. Now the system is quiet.

**Diagnosis — what actually happened:**

Lucas pulls up the Datadog APM view and filters for failed `INITIAL_SYNC` spans in the hour before the pause. He looks at two things in sequence: the error type distribution, and the timing pattern of retry attempts.

The error type is uniformly `gocardless_rate_limit` — GoCardless 429. No database errors, no infrastructure errors. Pure rate limiting. Lucas opens the GoCardless status page in a separate tab: no active incidents. GoCardless was not down — FinVerse was exceeding the API rate limit.

Then he looks at the retry timing. He pulls up the BullMQ configuration file and reads the backoff settings:

```typescript
// Current configuration — no jitter
backoff: {
  type: 'exponential',
  delay: 5000,   // 5s, 10s, 20s between retries
}
```

Lucas: "no jitter. Eight containers with concurrency 10. All 80 jobs failed at the same moment. All 80 retry at exactly T+5 seconds. GoCardless gets hit with 80 requests simultaneously. It rate limits again. All 80 retry at T+10 seconds. Same result. Every retry wave is a burst. We are not giving GoCardless time to breathe."

You ask: "so the backoff is actually making it worse?"

Lucas: "the backoff without jitter is making the retries synchronised. Which means every retry attempt is another coordinated attack on a system that is trying to recover. Jitter spreads the retries out. Instead of 80 simultaneous requests, you get roughly 16 per second for 5 seconds. That is a trickle. GoCardless can handle a trickle."

He also pulls up the ECS console. Eight containers are running. He looks at the rate limiter configuration in the worker code — there is none. No `limiter` config on the BullMQ processor. Eight containers at concurrency 10, each processing 10 jobs simultaneously, each job making multiple GoCardless API calls. At peak, the system was sending well over 50 GoCardless requests per second — exceeding the API limit.

**The fix — two changes:**

Lucas says: "you are going to write the code changes. I will handle the ECS scaling configuration and the queue resume. Here is what we need."

He describes the two changes verbally. You open your editor and implement them while he configures ECS.

**Your change 1 — rate limiter on the worker:**

```typescript
// transaction-sync.worker.ts — add rate limiter config
@Processor('transaction-sync', {
  concurrency: 10,
  
  // ADD THIS: hard ceiling on API call rate per container
  limiter: {
    max: 30,           // max 30 jobs processed
    duration: 10_000,  // per 10-second window per container
  },
  
  stalledInterval: 30_000,
  maxStalledCount: 2,
  lockDuration: 60_000,
  lockRenewTime: 30_000,
})
```

With 3 containers maximum (Lucas's ECS configuration change) and `max: 30` per 10 seconds: 3 × 30 = 90 jobs per 10 seconds. Each job makes roughly 2 to 3 GoCardless calls: 90 × 2.5 = 225 calls per 10 seconds = 22.5 calls per second. GoCardless's limit is 50 per second. Comfortable headroom.

**Your change 2 — exponential backoff with jitter:**

```typescript
// In BullMQ queue defaultJobOptions — replace the backoff config

// BEFORE (no jitter — synchronised retries):
backoff: {
  type: 'exponential',
  delay: 5000,
}

// AFTER (with jitter — randomised retries):
backoff: {
  type: 'custom',
  delay: (attemptsMade: number) => {
    const baseDelay    = 5_000
    const maxDelay     = 60_000
    const exponential  = baseDelay * Math.pow(2, attemptsMade)
    const capped       = Math.min(maxDelay, exponential)
    
    // Random jitter: between 0% and 100% of the capped delay
    // Each job gets a different wait time
    return Math.floor(Math.random() * capped)
  },
}
```

You finish both changes in eleven minutes. Lucas reviews them on a brief screen share — he reads through each one, asks one question ("why Math.floor and not Math.ceil?"), you explain it does not matter but floor is convention for delay values. He approves.

You push the changes to a hotfix branch. Lucas has already updated the ECS Auto Scaling maximum container count from 10 to 3 in the AWS console — no code change needed, just a configuration update.

The branch goes through CI. All checks pass in six minutes — the changes are small, unit tests cover the backoff function directly. Lucas merges and triggers a manual deployment to production.

**Restoring the system:**

While the deployment runs, Lucas monitors ECS. The new containers come up with the rate limiter and jitter configured. Old containers drain. Within eight minutes, all containers are running the new code.

Lucas resumes the queue from Bull Board. You both watch Datadog together.

```
QUEUE DRAIN — POST-FIX

Queue depth graph after resume:

7200 ┤╮
6000 ┤╰╮
4800 ┤  ╰╮
3600 ┤    ╰╮
2400 ┤      ╰╮
1200 ┤        ╰╮
   0 ┼──────────╰──────────────────────────────►
     14:07      17:42
     (resume)   (empty)

Time to drain 7,200 jobs: ~3.5 hours
GoCardless error rate during drain: 0.1%
(occasional transient 429s, all recovered on retry)
```

The queue drains cleanly. No waves of failures. No retry storms. The jitter is working — retries are spreading out over their individual random windows, arriving at GoCardless as a steady trickle instead of coordinated bursts.

---

**Writing the Post-Mortem:**

The following morning, Lucas runs a brief post-mortem in the team channel. He writes the structure himself — timeline, impact, root cause, immediate fix, long-term actions — and asks you to write the "Technical Root Cause Analysis" section. This is the first time you have written a post-mortem section.

You spend an hour on it. The challenge is not technical accuracy — you understand exactly what happened. The challenge is the tone. Your first draft reads like a bug report: "the system did not have a rate limiter and the backoff did not have jitter." Lucas reads it and sends one comment: "good facts, wrong frame. Post-mortems are blameless and explanatory. Tell me *why* the system behaved the way it did, not what was missing."

You revise it. The second version:

```
TECHNICAL ROOT CAUSE ANALYSIS
(excerpt from post-mortem)

The rate storm occurred due to the interaction of three 
individually reasonable configuration choices that 
combined unexpectedly at scale:

1. ECS Auto Scaling was correctly configured to launch 
   additional worker containers when queue depth exceeded 
   the threshold. This is the intended behaviour — scaling 
   out to drain a backlog faster. The campaign brought 
   8,000 simultaneous new users, which is 4× larger than 
   any previous growth event. The Auto Scaling responded 
   correctly to the queue depth signal.

2. The worker concurrency (10 per container) was sized 
   for normal operation — a reasonable choice that kept 
   GoCardless API call rates within limits under typical 
   load. Under the 8× container scale-out, the aggregate 
   concurrent calls exceeded the GoCardless rate limit.

3. The exponential backoff without jitter caused retry 
   attempts to be synchronised across all in-flight jobs. 
   When all 80 concurrent jobs received 429 errors 
   simultaneously, they all scheduled retries at the same 
   intervals, creating burst waves that prevented GoCardless 
   from recovering between attempts.

The root cause is not any one of these three decisions — 
each was reasonable in isolation. The root cause is that 
the system had not been tested or modelled at 8× normal 
scale, and the interaction between these three behaviours 
was not anticipated.
```

Lucas reads the revision and responds: "much better. This is what a post-mortem should sound like." He makes two small edits to the wording and publishes it to the engineering Notion page.

The post-mortem also identifies two long-term actions: adding a campaign-aware rate limiting strategy so future user acquisition events can be configured in advance, and adding the GoCardless rate limit monitor to Datadog so the team is alerted before users are affected rather than after. You are assigned the Datadog monitor — a straightforward task that you complete the following day.

---

**R — Result:**

The queue drains fully in 3.5 hours. All 8,000 new users eventually see their transactions appear in the app. Zero permanent data loss — BullMQ's durability guarantees meant every job was either successfully processed or safely retried.

The GoCardless error rate during the drain sits at 0.1% — the jitter is demonstrably working. You can see it in the APM trace view: retry attempts are arriving at GoCardless distributed across time rather than in waves.

The follow-up campaign two months later — 5,000 new users over 72 hours — runs with no incidents. The Datadog monitor fires a warning at one point when queue depth climbs, but the rate limiter keeps GoCardless calls within bounds, and the warning resolves on its own within 20 minutes as the queue drains.

What the incident taught you is harder to quantify than metrics. Lucas's deliberateness under pressure — the pause, the calm diagnosis, the clear task assignment — gave you a template for how to behave in incidents that no documentation could have provided. You had read about incident response. You had watched it from a distance in Month 8. This time you were inside it, writing code that went to production within the hour, watching the result in Datadog in real time. That is a qualitatively different kind of learning.

In your personal Notion page you write one line after the incident: "stabilise first. Always stabilise first."

---

### Story 2: Writing the Post-Mortem Section — What Blameless Actually Means

This is a short story, but worth telling separately because the lesson is distinct from the technical content of the incident.

---

**S — Situation:**

The morning after the rate storm, Lucas asks you to write the technical root cause section. You have never written a post-mortem before. You understand what happened technically. You sit down to write and produce what feels like a clear explanation.

---

**T — Task:**

Write a post-mortem section that is technically accurate, blameless, and genuinely useful for understanding the system — not just for documenting what went wrong.

---

**A — Action:**

Your first draft, as described above, lists what was missing: no rate limiter, no jitter. Lucas's comment — "wrong frame" — prompts you to think about what a post-mortem is actually for.

You realise: a post-mortem is not a bug report. A bug report says "this is broken." A post-mortem says "here is why a reasonable system behaved in a way that caused harm, and here is what we can change so it does not happen again."

The difference matters because the first framing implies someone made a mistake. The second framing implies the system was not designed for a condition it encountered. One creates defensiveness. The other creates learning.

The absence of jitter in the backoff configuration was not a mistake. Nobody looked at that configuration and decided to skip jitter maliciously or carelessly. It was not configured because the system had never encountered conditions where it mattered. The post-mortem should explain that — so the next engineer who reads it understands the system better, not so they know who to blame.

You rewrite with that framing. The revision takes 40 minutes and three passes. Lucas approves it with two small edits.

---

**R — Result:**

The post-mortem is published. Six months later — after you have left FinVerse — Priya joins the team and is given a reading list for her first week. The post-mortem is on it. She messages you on LinkedIn: "just read the GoCardless incident post-mortem — really helped me understand the rate limiting setup."

You did not know the document would outlast your contract when you wrote it. That is what good documentation does.

---

### Story 3: The Datadog Monitor — A Small Task Done With Care

**Background:**

The post-mortem assigned you one follow-up task: create a Datadog monitor for GoCardless API error rate so the team is alerted before users notice problems, not after. This is a small task — an hour of work. But it is the kind of task that demonstrates whether an engineer brings the same care to everything or only to the things that feel important.

---

**S — Situation:**

It is the day after the rate storm. You have a Linear ticket: "Add Datadog monitor for GoCardless API error rate." You could write a simple threshold monitor, assign it to Lucas for review, and close the ticket in an hour. That would be correct. You choose to do slightly more.

---

**T — Task:**

Create a Datadog monitor that accurately distinguishes between a real GoCardless problem requiring immediate action and expected transient errors that resolve on their own.

---

**A — Action:**

Before writing any monitor configuration, you think about what the monitor should actually tell the on-call engineer. The yesterday's incident had a 23% error rate — clearly a problem. But GoCardless regularly returns occasional 429s during normal operation when individual jobs hit brief rate limit windows. These resolve on their own within seconds and never cause user-facing impact. If the monitor fires on those, it will produce noise.

You look at Datadog's historical data for `finverse.gocardless.api.errors` over the past 30 days. Normal operation: 0.05% to 0.2% error rate. The rate storm: 23%. You need a threshold that catches real problems without firing on normal noise.

You also think about the two different alert levels you need:

```typescript
// Datadog monitor configuration — GoCardless API error rate

{
  name: 'GoCardless API — Error Rate Elevated',
  type: 'metric alert',

  query: `
    avg(last_5m):
      sum:finverse.gocardless.api.errors{
        env:production,
        status_code:429
      }.as_rate()
      /
      sum:finverse.gocardless.api.calls{env:production}.as_rate()
      * 100
  `,

  thresholds: {
    critical: 5,     // > 5% for 5 minutes → page on-call
    warning:  1,     // > 1% for 5 minutes → Slack only
  },

  // Use 5-minute average to filter out transient spikes
  // A single burst of 429s that resolves in under 5 minutes
  // will not trigger the alert — only sustained problems do
  evaluation_delay: 0,
  new_host_delay: 300,

  message: `
    GoCardless API error rate is {{value}}% 
    (threshold: 5% critical, 1% warning).
    
    During rate limit incidents:
    1. Check if this is triggered by high queue depth
       (scale-out may be causing aggregate call rate to exceed limit)
    2. If queue depth > 2,000: consider pausing queue from Bull Board
    3. Check GoCardless status: https://status.gocardless.com
    
    Runbook: https://notion.so/finverse/runbooks/gocardless-rate-limit
    
    @slack-incidents
  `,

  notify_no_data: false,
  // Don't alert on missing data — GoCardless errors are a subset
  // of all GoCardless calls. If calls stop (queue paused),
  // the error metric will also stop — that is not a problem.
}
```

You also write a short runbook section in Notion — three paragraphs covering the diagnosis flow, the stabilisation step (pause the queue), and the link to the post-mortem from yesterday's incident.

You submit it as a PR. Lucas reviews it. One comment: "smart choice to use `notify_no_data: false` — if the queue is paused for maintenance, you don't want an alert saying GoCardless is down. Good thinking." He approves.

---

**R — Result:**

The monitor goes live. It does not fire for the rest of your contract — GoCardless has no further incidents at the scale that would trigger it. But the monitor from the budget notifications incident (which you also contributed to in Month 8) does fire three weeks later, catches a Notification Service deployment issue in 14 minutes instead of 30.

The GoCardless monitor becomes part of the alerting runbook that is handed to the next engineer who joins the Core Product team after your contract ends.

---

## What Month 10 Taught You Overall

Three things, each distinct:

**From the rate storm itself:** Stabilise first. Always. The instinct to understand is correct — but the timing matters. In a live incident where users are actively affected, every minute of investigation without stabilisation is a minute of ongoing harm. Pausing the queue cost two minutes. It bought an hour of calm diagnosis. That trade is always worth it.

**From the post-mortem writing:** Blameless is not the same as vague. A blameless post-mortem still names exactly what happened and exactly why. The difference is in the framing — the question is not "who made the mistake" but "why did a reasonable system behave in a harmful way." That framing is not just kinder. It is more accurate. Systems fail because of unexpected interactions between reasonable decisions, not because one person did something careless.

**From the Datadog monitor:** Small tasks done with care compound. The monitor took an hour. The `notify_no_data: false` detail took two minutes of thinking. But it prevented false alarms that would have eroded trust in the alerting system. Every small piece of work either adds signal or noise to the system. Choosing to add signal — even on a small task — is a habit, not a special effort.