# Month 8: Learning to Think at Production Scale

---

## Theme: *Watching how senior engineers handle the unexpected*

---

## Foundational Knowledge: What You Need Before These Stories

Month 8 has two major threads running through it — an incident you shadow, and a design problem you own but do not fully solve on the first try. Before either story, two concepts are worth understanding properly.

---

### Concept 1: How a Senior Engineer Thinks Through a Production Incident

When something goes wrong in production, there is a specific mental model experienced engineers use to navigate quickly to the root cause. Junior engineers tend to jump to the most recent change they made, or to the most visible symptom. Senior engineers start with questions that narrow the search space before looking at any specific system.

The two most important questions are:

```
INCIDENT TRIAGE — TWO QUESTIONS THAT COME FIRST

Question 1: Is this a producer problem or a consumer problem?

  If you have a queue and messages are backing up:
  
  Producer problem: the system creating messages is
  creating too many too fast, or is broken and flooding
  the queue with garbage.
  
  Consumer problem: the system consuming messages cannot
  keep up — it is too slow, crashing, or in a retry loop.
  
  Why ask this first?
  The queue depth metric answers it immediately.
  
  Queue depth HIGH and GROWING:
    → Consumer cannot keep up (consumer problem)
    → Check consumer logs, consumer error rates
    
  Queue depth HIGH but STABLE or FALLING SLOWLY:
    → Producer is still sending, consumer is recovering
    → May resolve itself
    
  Queue depth ZERO but notifications still late:
    → Consumer is processing but slowly
    → Check individual job duration, not queue depth

Question 2: Is this a code problem or a dependency problem?

  Code problem: our code has a bug or a regression
  → Correlates with a recent deployment
  → Affects all users equally, all the time
  
  Dependency problem: GoCardless, SendGrid, RabbitMQ
  → Does NOT correlate with a deployment
  → May affect only some users or some operations
  → External status page will often confirm it
  
  Why ask this second?
  Because the fix is completely different.
  A code problem needs a rollback or a hotfix.
  A dependency problem needs you to wait,
  reduce load on the dependency, and drain the backlog.
```

This mental model — producer vs consumer, code vs dependency — takes seconds to apply if you have it memorised. Without it, engineers spend twenty minutes reading code looking for a bug that does not exist, when the real answer is on a status page.

You observe Lucas apply this model in real time during Month 8. By the end of the incident, you have it permanently.

---

### Concept 2: Why Multi-Container Systems Break Designs That Work in Single Containers

In Month 7 you built the Outbox pattern. In Month 8, Lucas catches a gap in your design that stems from a very specific misconception — one that is easy to have if you have mostly developed against a single running instance of a service.

When you develop locally, Core Product runs as one container. One process, one event loop, one Node.js process. A `setInterval` that runs every 5 seconds runs once, in that one process.

In production at FinVerse, Core Product runs as three containers behind an ALB during normal operation. During peak periods, ECS scales to more. This means your `setInterval` runs in three separate processes simultaneously.

```
THE MULTI-CONTAINER PROBLEM — VISUALISED

LOCAL DEVELOPMENT:
  [Core Product Container 1]
    setInterval(pollOutbox, 5000)
    → fires once every 5 seconds
    → processes pending outbox events
    ✓ correct

PRODUCTION — THREE CONTAINERS:
  [Core Product Container 1]    [Core Product Container 2]    [Core Product Container 3]
    setInterval(pollOutbox, 5000)  setInterval(pollOutbox, 5000)  setInterval(pollOutbox, 5000)
    → fires every 5 seconds        → fires every 5 seconds        → fires every 5 seconds

  All three fire at roughly the same time.
  All three read the same PENDING outbox events.
  All three try to publish the same events to RabbitMQ.
  All three mark the same events as PUBLISHED.

  User receives three push notifications for one budget alert.
  Notification Service receives the same event three times.
```

The fix is the same BullMQ singleton job pattern you have seen in Chapter 8 of the BullMQ module — use a repeatable job with a stable `jobId` instead of `setInterval`. BullMQ coordinates across containers via Redis, ensuring only one container executes the job at any given time.

But the important lesson is not the fix. The lesson is the habit: any time you write code that runs on a schedule or fires automatically, ask yourself — "what happens when three containers do this simultaneously?" The answer shapes your design before you write the first line.

---

## The Stories

---

### Story 1: Shadowing the RabbitMQ Consumer Lag Incident

**Background:**

On a Thursday afternoon in Month 8, the `#incidents` Slack channel fires. Multiple users are reporting budget alert notifications arriving 20 to 30 minutes late. The reports are coming in through the support channel — not a massive volume, but enough to be systematic. Lucas is on-call. He sends a Slack message to the Core Product team channel: "investigating budget notification lag, will update shortly." Then he pings you directly: "want to shadow this one? Good learning opportunity."

You join the incident thread immediately.

---

**S — Situation:**

It is 14:17. You have no idea yet whether this is your module, Tomasz's module, the Notification Service, RabbitMQ, or SendGrid. You open your laptop alongside Lucas in a shared Datadog session. Lucas narrates everything he does. You take notes in real time.

---

**T — Task:**

Understand how Lucas navigates a live production incident from first symptom to root cause — and contribute where you can, including writing the monitoring improvement that comes out of it.

---

**A — Action:**

**Lucas's first move — queue depth, not logs:**

Lucas opens the Datadog dashboard for RabbitMQ before he looks at a single log line. He says, without preamble: "queue depth first. Tells us whether it is a consumer problem or a publisher problem before we read anything."

He opens the `budget-notifications-queue` depth graph. The current value: 4,823 messages. The baseline for this time of day: near zero. The graph shows it started climbing at 13:52 — 25 minutes ago.

```
LUCAS'S NARRATION:

"Queue is deep — 4,800 messages when it should be near zero.
This means the consumer is not keeping up.
Not a publisher problem — Core Product is successfully
publishing events, they are arriving in RabbitMQ.
The problem is on the Notification Service side.
Now I go to Notification Service logs, not Core Product logs."
```

You write this down verbatim. Not the facts — the reasoning. "Queue deep → consumer problem → look at consumer logs."

**Lucas's second move — filter by error type, not by time:**

Lucas opens Datadog Log Explorer. He does not scroll through recent logs looking for something red. He adds a filter immediately: `service:notification-service status:error`. Then he adds a second filter: `@error.type:*`. He clicks "Group by: error.type" to see the error distribution.

Within 30 seconds, the results are clear: 847 error log lines, all with the same `error.type: TemplateRenderError`. The timestamp range: 13:51 to now.

```
LUCAS'S NARRATION:

"847 errors, all the same type, starting at 13:51.
That is not random failures — that is a systematic failure
triggered by something that changed at 13:51.
Check: was there a deployment at 13:51?"
```

He opens the `#deployments` Slack channel. At 13:49, a GitHub Actions notification: `notification-service deployed to production`. Two minutes before the errors started.

```
LUCAS'S NARRATION:

"Deployment at 13:49. Errors start at 13:51.
Probably a bad change in the Notification Service.
Let me check the specific error message."
```

He clicks one error log line. The full message: `Error rendering template budget_alert: Cannot read property 'spent' of undefined`. The template is expecting a `spent` field on the payload. It is not present.

**Lucas's diagnosis:**

At this point Lucas says: "someone changed the template but the event payload they are expecting does not match what Core Product is publishing." He pulls up the recent diff on the notification-service repository. The template was updated to use `payload.spent` directly instead of `payload.data.spent` — but Core Product's outbox publisher still wraps the data under a `data` key.

He calls Tomasz on a quick voice call — three minutes. Tomasz confirms: the outbox payload structure was intentionally nested under `data` for consistency with the response envelope format. The Notification Service change assumed it was flat. A coordination miss.

**The fix:**

The Notification Service template is reverted. Tomasz deploys the revert in nine minutes. The queue begins draining immediately — by 14:45, it is back to zero. All 4,823 queued messages are processed. The delayed notifications go out. Total incident duration from first report to queue drained: 28 minutes.

**Your contribution — the monitoring improvement:**

After the incident resolves, Lucas runs a five-minute retrospective in the incident Slack thread. He lists two action items. The first: add coordination documentation for the outbox payload format. The second: "we had no monitor on the budget-notifications-queue depth. We only knew about this because users reported it. We should have been alerted before users noticed."

He asks you to write the Datadog monitor configuration for the queue depth. This is directly connected to the observability work from Chapter 6 — you know exactly how Datadog monitors work from the module content.

```typescript
// datadog-monitor-config.ts
// Budget notifications queue depth monitor

const budgetNotificationsQueueMonitor = {
  name: 'RabbitMQ — Budget Notifications Queue Depth High',
  type: 'metric alert',

  query: `
    avg(last_10m):
      avg:rabbitmq.queue.messages{
        queue:budget-notifications-queue,
        env:production
      } > 500
  `,

  message: `
    Budget notifications queue depth is {{value}} messages
    (threshold: 500).

    This typically means the Notification Service consumer
    is not processing messages — check for:
    1. Notification Service deployment issues
    2. SendGrid / FCM dependency outages
    3. Consumer error rate spike

    Runbook: https://notion.so/finverse/runbooks/budget-notifications-lag
    Dashboard: https://app.datadoghq.eu/dashboard/...
    
    @slack-incidents @pagerduty-oncall
  `,

  thresholds: {
    critical: 500,    // fire alert → PagerDuty page
    warning: 100,     // informational → Slack only
  },

  notify_no_data: true,
  no_data_timeframe: 10,   // alert if no data for 10 minutes
                            // (means the consumer may have crashed
                            //  and stopped emitting metrics entirely)
}
```

You also write the runbook page in Notion — four sections covering the producer vs consumer diagnosis flow, the specific log filters that found the root cause in today's incident, the revert procedure for Notification Service deployments, and the contact list for cross-team escalation.

Lucas reviews both. The monitor configuration gets one edit — he changes the `no_data_timeframe` from 10 to 5 minutes. "Five minutes of no metrics is enough to know something is broken." The runbook is approved without changes. Both ship the following Tuesday.

---

**R — Result:**

The monitor goes live. Three weeks later during a different deployment, the same queue depth alert fires at 14:02. The on-call engineer — not Lucas this time, it is Elena — opens the runbook, follows the producer-vs-consumer logic, checks the Notification Service logs, finds a different template error, and has it resolved by 14:19. Seventeen minutes. No user reports.

The runbook you wrote handled an incident you did not even know would happen. That is the kind of contribution that outlasts the sprint it was created in.

What you took from the incident itself — from watching Lucas work — is more important than the runbook. You now have the mental model permanently: queue depth before logs, producer vs consumer, code vs dependency. When the next incident comes that involves your module, you will start in the same place Lucas did.

---

### Story 2: The Notification Race Condition — Designing, Missing Something, Revising

**Background:**

In the second week of Month 8, Lucas assigns you a ticket that has been sitting in the backlog for two months. The title: "Investigate: budget alert notifications sometimes arrive before transactions appear in transaction list."

The support team has been collecting these reports — users who receive a push notification saying "You have spent 94% of your Dining Out budget" and then open the app to find the transaction list unchanged. The new transactions that triggered the alert are not there. They appear a few minutes later. A handful of users have filed support tickets assuming the notification was wrong.

This is an investigation ticket, not an implementation ticket. Lucas frames it clearly: "find the root cause, propose a fix, come back before you build anything."

---

**S — Situation:**

You are given the ticket, access to Datadog, and no other guidance. The symptom is clear. The cause is not. You have never investigated a timing issue across an async boundary before.

---

**T — Task:**

Find the root cause of the race condition, propose a fix that is architecturally sound, and present it to Lucas for review before writing any code.

---

**A — Action:**

**Step 1: Find a concrete example in the logs**

You search the support tickets for a specific user and timestamp. You find one — `usr_789`, who reported the issue at 14:22 on a Wednesday two weeks ago. You open Datadog Log Explorer and filter by `@userId:usr_789` with a time window around 14:22.

The log sequence:

```
14:19:23.421  INFO  [TransactionSyncWorker]
  Sync completed — 18 transactions inserted
  { userId: usr_789, accountId: acc_001,
    transactionsFetched: 18, transactionsInserted: 18 }

14:19:23.435  INFO  [BudgetService]
  Budget threshold exceeded — publishing event
  { userId: usr_789, category: Dining Out,
    spent: 187.50, limit: 200.00 }

14:19:23.437  INFO  [RabbitMQPublisher]
  Event published to RabbitMQ
  { eventType: budget.threshold.exceeded,
    userId: usr_789 }
```

Then, in the Notification Service logs, filtered by the same `correlationId`:

```
14:19:23.441  INFO  [BudgetAlertConsumer]
  Received budget.threshold.exceeded event
  { userId: usr_789 }

14:19:23.447  INFO  [NotificationService]
  Push notification sent
  { userId: usr_789, channel: push }
```

The push notification went out at `14:19:23.447` — 26 milliseconds after the sync worker logged completion. That is extraordinarily fast. RabbitMQ delivery plus Notification Service processing in 26 milliseconds.

Then you look at what happened when `usr_789` opened the app:

```
14:19:25.812  INFO  [TransactionsController]
  GET /v1/transactions — request received
  { userId: usr_789 }

14:19:25.891  INFO  [TransactionsController]
  GET /v1/transactions — response returned
  { userId: usr_789, transactionCount: 143 }
```

The user opened the app at `14:19:25` — two seconds after the sync completed. The transaction list returned 143 transactions. But 18 new ones were just inserted. The user should have seen 161. They saw 143.

**Step 2: Form the hypothesis**

You stare at the numbers. The sync worker inserted 18 transactions at `14:19:23.421`. The user queried transactions at `14:19:25.812` — 2.4 seconds later. PostgreSQL committed the insert before the sync worker log line appeared. The transactions should be there.

Unless they are not being read from the primary. You check the Prisma configuration for the transactions query. You find it:

```typescript
// transactions.service.ts
async getTransactions(userId: string, ...): Promise<TransactionListResponse> {
  // Uses read replica for all SELECT queries
  const transactions = await this.prisma.$replica().transaction.findMany({
    where: { userId },
    ...
  })
  ...
}
```

The `$replica()` extension routes read queries to the PostgreSQL read replica. The replica receives a replication stream from the primary. Replication is asynchronous. There is a lag between when the primary commits a write and when the replica receives and applies it.

At 14:19:23, 18 transactions are committed to the primary. The replication lag for this replica — you check the RDS metrics in the Datadog infrastructure view — is typically 1 to 4 seconds during business hours.

At 14:19:25 — 2 seconds later — the user's query hits the replica. The replica has not yet received the replication for these 18 rows. The query returns the pre-sync state.

The push notification, however, does not go through the replica. It goes through the event pipeline: sync worker → RabbitMQ → Notification Service → push. That pipeline completes in 26 milliseconds. The notification arrives before the replica has caught up.

**Your initial fix proposal:**

You draft a Slack message to Lucas. The diagnosis is correct. The fix you propose: add a short delay to the RabbitMQ publish — wait 5 seconds after the sync completes before publishing the `budget.threshold.exceeded` event. By then, the replica will have caught up.

```typescript
// PROPOSED FIX (first attempt)

// In the sync worker, after inserting transactions:
await this.transactionService.bulkInsert(userId, accountId, transactions)
await this.budgetService.checkThresholds(userId)

// Wait for replica lag before publishing
await new Promise(resolve => setTimeout(resolve, 5000))
await this.rabbitMQPublisher.publish('budget.threshold.exceeded', payload)
```

You send the proposal to Lucas before writing any code.

**Lucas's response — the gap:**

Lucas replies within the hour. He reads the diagnosis: "correct root cause." Then he reads the proposed fix and asks a question: "what happens when three containers are all running this sync worker and all publish the event 5 seconds after sync?"

You think about it. Three containers. Each one runs the sync worker for different users. If all three of them call `setTimeout(5000)` and then `rabbitMQPublisher.publish()`, they each publish the event independently. For a single user's sync, only one container picks up the job — BullMQ deduplication ensures this. So the event is published once, delayed by 5 seconds.

The 5-second delay is not wrong. But it is fragile. If the replication lag spikes to 7 seconds, the delay is insufficient. If the replication lag drops to 0.5 seconds, the 5-second delay is unnecessary and slows down every notification.

More importantly, Lucas says: "the right fix is not a sleep. The right fix is to stop publishing directly to RabbitMQ from the sync worker at all. Use the Outbox pattern — the event is written to `outbox_events` in the same PostgreSQL transaction as the bulk insert. The outbox publisher polls every 5 seconds. By the time the event reaches RabbitMQ, the replica has had at minimum 5 seconds to catch up — and probably more, because the outbox publisher has its own processing time. No sleep, no fragility, no guessing about replica lag."

You had learned the Outbox pattern in Module 10 and seen it used in the accounts module. You had not connected it to this problem.

Lucas adds: "but there is a second thing to think about. The outbox publisher runs as a BullMQ repeatable job. If three containers are each running the outbox publisher, they will all pick up the same pending events and publish them three times. How do you prevent that?"

This is the second gap. You think for a moment. The answer is what you now know from Month 7 and from the BullMQ module: a singleton repeatable job with a stable `jobId`. BullMQ's `LMOVE` operation is atomic — only one container wins the job, the others get nothing. You had seen this in Chapter 8. You had not applied it in this context yet.

**Revised proposal:**

You write up the full revised design — two pages in Notion. The key changes:

First, the sync worker stops publishing to RabbitMQ directly. Instead, it writes a `budget.threshold.exceeded` event to the `outbox_events` table inside the same `$transaction` block as the bulk insert:

```typescript
// REVISED FIX — Outbox pattern

await this.prisma.$transaction(async (tx) => {
  // Write transactions
  await tx.transaction.createMany({
    data: newTransactions,
    skipDuplicates: true,
  })

  // Update budget spent amount
  await tx.budget.update({
    where: { id: budget.id },
    data: { spent: newSpentAmount }
  })

  // Write outbox event — in the same transaction
  // If this fails, the whole transaction rolls back
  // No orphaned budget update without a corresponding event
  await tx.outboxEvent.create({
    data: {
      eventType: 'budget.threshold.exceeded',
      payload: {
        userId,
        category:    budget.categoryName,
        spent:       newSpentAmount.toNumber(),
        limit:       budget.amount.toNumber(),
        percentage:  (newSpentAmount.div(budget.amount)).times(100).toNumber(),
      }
    }
  })
})
```

Second, the outbox publisher is moved from a `setInterval` in the API service to a BullMQ singleton repeatable job:

```typescript
// outbox-publisher.scheduler.ts
@Injectable()
export class OutboxPublisherScheduler implements OnModuleInit {

  constructor(
    @InjectQueue('outbox-publisher')
    private readonly outboxQueue: Queue,
  ) {}

  async onModuleInit(): Promise<void> {
    await this.outboxQueue.add(
      'POLL_OUTBOX',
      {},
      {
        repeat: { every: 5_000 },
        jobId: 'outbox-publisher-recurring',   // singleton — only one ever exists
        removeOnComplete: { count: 1 },
      }
    )
  }
}
```

Third, the outbox publisher worker itself:

```typescript
// outbox-publisher.worker.ts
@Processor('outbox-publisher', {
  concurrency: 1,   // order matters — sequential, not concurrent
})
export class OutboxPublisherWorker extends WorkerHost {

  async process(job: Job): Promise<void> {
    const pendingEvents = await this.prisma.outboxEvent.findMany({
      where: { status: 'PENDING' },
      orderBy: { createdAt: 'asc' },   // oldest first — preserve order
      take: 100,
    })

    for (const event of pendingEvents) {
      await this.rabbitMQChannel.publish(
        this.getExchange(event.eventType),
        event.eventType,
        Buffer.from(JSON.stringify(event.payload)),
        {
          persistent: true,
          messageId: event.id,
        }
      )

      await this.prisma.outboxEvent.update({
        where: { id: event.id },
        data: {
          status:      'PUBLISHED',
          publishedAt: new Date(),
        }
      })
    }
  }
}
```

You send the revised design to Lucas. He reviews it and comes back with one comment: "correct. Build it."

**The build:**

You implement it across three PRs. The first removes the direct RabbitMQ publish from the sync worker and adds the outbox write. The second adds the outbox publisher worker and scheduler. The third removes the old `setInterval` polling code from the API service and cleans up the related module registrations.

Lucas reviews all three. Total comments across all three PRs: four. One clarifying question about the `concurrency: 1` choice on the outbox worker (your answer: order matters — events must publish in the order they were written, concurrent processing would break ordering guarantees). One suggestion to add a TTL to the Datadog log filter in the runbook you update. Two positive observations — one noting the clean separation of the outbox write from the business logic, one noting the singleton `jobId` pattern used correctly.

The feature ships to production in the third week of Month 8.

---

**R — Result:**

After deployment, you filter Datadog logs for the pattern that previously indicated the race condition — `budget.threshold.exceeded` event published within 1 second of sync completion. The pattern disappears entirely. The minimum latency from sync to notification is now the outbox poll interval: 5 seconds. In practice it averages 6 to 8 seconds, depending on when in the 5-second window the sync completed. The replica lag is never an issue — by the time 5 to 8 seconds have passed, even a 4-second replica lag window has closed.

Support tickets for "notification before transactions" drop to zero in the two weeks following deployment. You confirm this by searching the support ticket system — you ask Daniel to pull the count. The category had three to five reports per week before the fix. Zero after.

The more important result: you proposed a fix, had a gap in it caught by Lucas, understood why the gap existed, revised the design correctly, and built it cleanly. You did not defend the first proposal. You absorbed the feedback, understood the deeper principle — that multi-container systems break assumptions that work in single containers — and produced something better. That sequence matters more than getting the design right on the first attempt.

In the next 1:1 with Lucas, he says one thing about the race condition work: "the first proposal was reasonable given what you knew. The second proposal showed you understood the system. That is the progression I want to see."

---

## What Month 8 Taught You Overall

Two distinct lessons, both lasting:

**From the incident shadowing:** The mental model Lucas demonstrated — queue depth before logs, producer before consumer, dependency before code — is not about being clever. It is about reducing the search space quickly so you spend your time looking in the right place. A 28-minute resolution time instead of a 3-hour one is the direct result of applying these two questions in the right order. You have them now. They cost nothing to carry and they pay off every time something breaks.

**From the race condition investigation:** Getting a design wrong on the first attempt is not a failure — it is a normal part of doing unfamiliar work. What matters is how you respond when the gap is pointed out. You did not defend the sleep-based fix. You understood Lucas's question, worked through the implication, and arrived at a fundamentally better solution. The Outbox pattern was not new to you — you had read it in the transaction handling module. What was new was recognising it as the right tool for this specific problem. That recognition is what growing engineers develop: not more tools, but better pattern matching.