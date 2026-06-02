# Month 12: Finishing Strong

---

## Foundational Knowledge: What You Need Before These Stories

Month 12 is the final month. The technical content is lighter than previous months — the heavy implementation work is behind you. What this month is really about is a quality of character: choosing to finish well when the natural instinct is to wind down.

One concept is worth understanding before the stories, because it shapes every decision you make in Month 12.

---

### Concept 1: The Difference Between Finishing a Contract and Finishing Well

When a contract is ending, there is a natural psychological shift. The work stops feeling like yours. The next sprint feels less urgent. The documentation that needs writing can wait. After all, you are leaving — why invest heavily in something you will not see the outcome of?

This instinct is understandable. It is also exactly wrong.

```
TWO WAYS TO END A CONTRACT

WAY 1: Coast to the finish line
  Stop taking initiative in the final month.
  Deliver what is assigned, nothing more.
  Leave the codebase in roughly the same state you found it.
  
  What this signals:
  → Your investment was conditional on your own stake
  → The work was about you, not the system or the team
  → The last impression you leave is one of declining effort
  
  What engineers remember:
  "They were good earlier on but kind of checked out at the end."

WAY 2: Finish strong
  Continue taking initiative in the final month.
  Ship one more meaningful feature cleanly.
  Leave the system in better shape than you found it.
  Make sure the knowledge you carry walks out the door
  in written form, not only in your head.
  
  What this signals:
  → Your investment was in the work itself, not your tenure
  → You understood that your responsibility extended to
    the people who come after you
  → The last impression you leave is one of consistent quality
  
  What engineers remember:
  "They were good all the way through. We're still using
  what they built and what they documented."
```

The difference between these two paths is not about heroics. It is not about working extra hours or doing something dramatic. It is simply about maintaining the same standard of care in the last sprint that you maintained in the first.

Month 12 is the test of whether the habits you developed over eleven months are genuinely yours — or whether they were a performance contingent on being observed.

---

## The Stories

---

### Story 1: The Consent Re-Authentication Flow — Shipping Something Real in the Final Sprint

**Background:**

For six months, a ticket has been sitting in the backlog. The title: "Improve consent re-authentication experience for expired connections." It was written by Céline after several user interviews where people described the reconnection flow as confusing — they received a notification telling them to reconnect their bank, tapped through to the app, and could not immediately tell which bank needed reconnecting or what to do next.

The current flow: a push notification arrives saying "Your [Bank Name] connection needs renewal." The user opens the app. They land on the accounts screen. The account shows a grey indicator. They tap it. A modal appears with a "Reconnect" button. They tap the button. The GoCardless consent flow opens in a WebView.

The problem: the notification arrives, but the accounts screen does not surface the urgency visually unless the user knows to look for the grey indicator. Users who dismiss the notification without acting on it often do not notice the problem until sync has been broken for days.

The product team's request: make the re-authentication need surface more prominently in the UI. The backend work: the `GET /v1/accounts` response already includes `needsReconnection` and `daysUntilExpiry` fields (which you added in Month 4). But it does not include enough information for the app to generate a clear call-to-action message, and the consent-check job that publishes the expiry notification needs to be updated to also publish a `bank.connection.reconnection.required` event that triggers a more prominent in-app prompt rather than just a push notification.

Lucas assigns it to you in the final sprint planning. "This one has been waiting for the right moment. It's yours — finish the contract with something clean."

---

**S — Situation:**

It is the first day of the final sprint. Two weeks left on your contract. You have one substantial ticket and two small ones. The substantial ticket — the consent re-auth improvement — touches the Accounts module, the outbox pattern, the BullMQ consent-check job, and the Notification Service consumer. All familiar territory. All things you have built or modified before.

The small tickets are routine: a minor API response change requested by the mobile team (adding `institutionLogoUrl` to the single account endpoint) and a documentation update to the runbook following a minor change in GoCardless's API response format for German banks.

---

**T — Task:**

Design, implement, and ship the consent re-authentication improvement in the final sprint. No drama. Clean code. One round of review.

---

**A — Action:**

**Understanding the full scope before writing a line:**

You spend the first morning of the sprint reading through the existing consent expiry flow end to end — the BullMQ job in `check-expiring-consents.worker.ts`, the outbox event structure, the Notification Service consumer that handles `bank.connection.expiring.soon`, and the current `GET /v1/accounts` response shape.

You identify four changes required:

```
CONSENT RE-AUTH IMPROVEMENT — SCOPE

Change 1: New outbox event type
  Publish 'bank.connection.reconnection.required' alongside
  the existing 'bank.connection.expiring.soon' event.
  
  Why a new event type and not reuse the existing one?
  The existing event triggers a push notification.
  The new event triggers an in-app prompt — a different
  UI treatment. Separating them gives Notification Service
  flexibility to handle each independently.

Change 2: Update the consent-check worker
  The worker currently publishes one event per expiring 
  connection: 'bank.connection.expiring.soon'.
  
  Add a second event: 'bank.connection.reconnection.required'
  with richer payload — the information the app needs to
  generate a specific call-to-action:
  
  { userId, connectionId, institutionName, institutionLogoUrl,
    daysUntilExpiry, affectedAccountCount }
  
  The 'affectedAccountCount' is new — tells the app how many
  accounts will stop syncing so the UI can say
  "2 accounts will stop syncing in 3 days" rather than
  a generic message.

Change 3: Update GET /v1/accounts response
  Add 'reconnectionMessage' — a pre-composed string
  the app can display directly without needing business
  logic on the client side:
  "Reconnect in 3 days to keep your 2 accounts syncing"
  
  Computing this server-side means when the copy changes,
  all app versions benefit immediately.

Change 4: Notification Service consumer (coordinate with Tomasz)
  Notification Service needs a new consumer for
  'bank.connection.reconnection.required'.
  This is Tomasz's territory — you write the proposed
  consumer code, he reviews and merges it.
```

You write this up in a Notion page and share it with Lucas and Tomasz before writing any code. Lucas reads it the same afternoon and responds: "looks complete. One thing — make sure the `reconnectionMessage` handles pluralisation correctly. '1 account' not '1 accounts'." You add a note to the implementation plan.

Tomasz responds within the hour: "I'll handle the Notification Service consumer once you have the event structure finalised. Send me the payload shape first and I'll write the consumer in parallel."

You send Tomasz the payload shape that afternoon. He starts on the consumer while you build the backend.

**Implementation — four PRs:**

**PR 1 — The outbox event structure (schema only):**

```prisma
// No schema change needed — outbox_events uses Json payload,
// new event type is just a new string value.
// But you add the new event type to the shared constants file
// so both Core Product and Notification Service reference
// the same string rather than hardcoding it separately.
```

```typescript
// src/common/constants/event-types.ts

export const EventTypes = {
  // Existing events
  BANK_CONNECTION_COMPLETED:    'bank.connection.completed',
  BANK_CONNECTION_DISCONNECTED: 'bank.connection.disconnected',
  BANK_CONNECTION_EXPIRING_SOON: 'bank.connection.expiring.soon',
  BUDGET_THRESHOLD_EXCEEDED:    'budget.threshold.exceeded',

  // New event
  BANK_CONNECTION_RECONNECTION_REQUIRED:
    'bank.connection.reconnection.required',
} as const
```

Small PR — one file changed. Lucas approves in 20 minutes with no comments.

**PR 2 — The consent-check worker update:**

```typescript
// check-expiring-consents.worker.ts

private async handleExpiringConnection(
  connection: ExpiringConnection
): Promise<void> {

  if (!this.shouldNotifyToday(connection.consentExpiresAt)) {
    return
  }

  const todayISO = new Date().toISOString().split('T')[0]
  const dedupKey = `consent:expiry:notif:${connection.id}:${todayISO}`

  const alreadyNotified = await this.redis.get(dedupKey)
  if (alreadyNotified) return

  // Count affected accounts for the richer payload
  const affectedAccountCount = await this.prisma.bankAccount.count({
    where: {
      bankConnectionId: connection.id,
      isActive: true,
    }
  })

  // Compose the reconnection message server-side
  // Handles pluralisation correctly
  const accountWord = affectedAccountCount === 1 ? 'account' : 'accounts'
  const daysUntilExpiry = this.getDaysUntilExpiry(connection.consentExpiresAt)
  const dayWord = daysUntilExpiry === 1 ? 'day' : 'days'

  const reconnectionMessage =
    `Reconnect in ${daysUntilExpiry} ${dayWord} to keep ` +
    `your ${affectedAccountCount} ${accountWord} syncing`

  // Write BOTH outbox events in the same transaction
  await this.prisma.outboxEvent.createMany({
    data: [
      // Existing event — triggers push notification
      {
        eventType: EventTypes.BANK_CONNECTION_EXPIRING_SOON,
        payload: {
          userId:           connection.userId,
          connectionId:     connection.id,
          institutionName:  connection.institutionName,
          consentExpiresAt: connection.consentExpiresAt.toISOString(),
        }
      },
      // New event — triggers in-app reconnection prompt
      {
        eventType: EventTypes.BANK_CONNECTION_RECONNECTION_REQUIRED,
        payload: {
          userId:               connection.userId,
          connectionId:         connection.id,
          institutionName:      connection.institutionName,
          institutionLogoUrl:   connection.institutionLogoUrl ?? null,
          daysUntilExpiry,
          affectedAccountCount,
          reconnectionMessage,
          consentExpiresAt:     connection.consentExpiresAt.toISOString(),
        }
      }
    ]
  })

  try {
    await this.redis.set(dedupKey, '1', 'EX', 86_400)
  } catch (redisError) {
    this.logger.warn(
      'Redis dedup key write failed after outbox write',
      {
        connectionId: connection.id,
        userId:       connection.userId,
        error:        redisError.message,
      }
    )
  }

  this.logger.info('Consent re-authentication notification scheduled', {
    userId:               connection.userId,
    connectionId:         connection.id,
    daysUntilExpiry,
    affectedAccountCount,
    expiresAt:            connection.consentExpiresAt.toISOString(),
  })
}
```

One thing Lucas flags in review: the `createMany` call writes both events but is not inside a `$transaction` block. If the second event write fails, the first has already committed — you end up with a push notification event but no in-app prompt event.

```typescript
// Lucas's comment:
// "wrap both writes in $transaction — if either fails,
//  neither should commit. Inconsistent event state
//  is harder to debug than a clean failure."

// Your fix:
await this.prisma.$transaction([
  this.prisma.outboxEvent.create({
    data: {
      eventType: EventTypes.BANK_CONNECTION_EXPIRING_SOON,
      payload: { /* ... */ }
    }
  }),
  this.prisma.outboxEvent.create({
    data: {
      eventType: EventTypes.BANK_CONNECTION_RECONNECTION_REQUIRED,
      payload: { /* ... */ }
    }
  })
])
```

You make the change. Lucas approves on the second pass with one positive comment: "the pluralisation handling is clean — good that it is server-side."

**PR 3 — The GET /v1/accounts response update:**

Adding `reconnectionMessage` to the account DTO. The field is only populated when `needsReconnection` is true — otherwise it is null:

```typescript
// account.service.ts — updated mapToDto

private mapToDto(account: BankAccountWithConnection): AccountDto {
  const daysUntilExpiry = this.getDaysUntilExpiry(
    account.bankConnection.consentExpiresAt
  )
  const needsReconnection = daysUntilExpiry !== null && daysUntilExpiry <= 7

  return {
    // ... all existing fields unchanged ...

    connection: {
      id:               account.bankConnection.id,
      status:           account.bankConnection.status,
      consentExpiresAt: account.bankConnection.consentExpiresAt
        ?.toISOString() ?? null,
      needsReconnection,
      daysUntilExpiry,

      // New field — null when no action needed
      reconnectionMessage: needsReconnection
        ? this.composeReconnectionMessage(
            account.bankConnection,
            daysUntilExpiry!
          )
        : null,
    }
  }
}

private composeReconnectionMessage(
  connection: BankConnectionWithAccounts,
  daysUntilExpiry: number
): string {
  const accountCount = connection.bankAccounts
    .filter(a => a.isActive)
    .length

  const accountWord = accountCount === 1 ? 'account' : 'accounts'
  const dayWord     = daysUntilExpiry === 1 ? 'day' : 'days'

  return (
    `Reconnect in ${daysUntilExpiry} ${dayWord} to keep ` +
    `your ${accountCount} ${accountWord} syncing`
  )
}
```

Arjun reviews this PR — you have been reviewing each other's PRs throughout the contract. He catches one thing: the `connection.bankAccounts` relation is not currently included in the `getAccountsForUser` query — you are using `select` rather than `include`, and `bankAccounts` is not in the select list. Fetching it separately for every account would be an N+1 query.

```typescript
// Arjun's comment:
// "bankAccounts isn't in the select — you'll get an
//  undefined error at runtime. Also don't add it to select
//  without thinking about the N+1 — this query runs for
//  every accounts screen load."

// Your fix:
// Instead of loading all accounts via a relation,
// use the affectedAccountCount you already have stored
// from the sync worker context — but that's not available here.
// 
// Better approach: add a subquery count directly in the select

const accounts = await this.prisma.bankAccount.findMany({
  where: { userId, isActive: true },
  select: {
    // ... existing fields ...
    bankConnection: {
      select: {
        id:               true,
        status:           true,
        consentExpiresAt: true,
        institutionId:    true,
        // Add count of active accounts under this connection
        _count: {
          select: { bankAccounts: { where: { isActive: true } } }
        }
      }
    }
  },
  orderBy: { createdAt: 'asc' }
})
```

You update `composeReconnectionMessage` to use `connection._count.bankAccounts` instead of loading the relation. Arjun approves. You thank him in the PR comment — it is a genuine catch, the kind that would have caused a runtime error in staging.

**PR 4 — Notification Service consumer (Tomasz's PR, your review):**

Tomasz writes the consumer for `bank.connection.reconnection.required`. He sends it for your review. You catch one thing: the consumer is checking user notification preferences for push only — not checking whether the user has opted out of in-app prompts specifically. You leave a comment explaining the preference key to check. Tomasz adds it and thanks you.

**Staging verification:**

You test the full flow in staging manually: modify a test account's `consentExpiresAt` to be 3 days from now, trigger the consent-check job manually, verify both outbox events are written, verify both are published to RabbitMQ, verify the Notification Service consumer fires, verify the `GET /v1/accounts` response includes `reconnectionMessage` with the correct copy.

Everything works on the first try. You write a staging verification note in the PR description so Lucas has it when he does the production review.

**Production deployment:**

The feature ships on Thursday of week two — two days before your contract ends. Clean deployment, no issues. Lucas monitors Datadog for 30 minutes after deployment. No error rate changes, no latency spikes. The consent-check job will not trigger real user notifications until the next 7-day expiry window hits, but the infrastructure is in place and verified.

---

**R — Result:**

The feature is in production before your contract ends. You did not rush it — four PRs, proper review, staging verification, clean deployment. The mobile team confirms they have what they need to build the UI treatment in their next sprint.

The Notification Service consumer Tomasz built will start firing within the week as users' consent windows approach the 7-day threshold. The `reconnectionMessage` field in the accounts response is available immediately.

You shipped something real in the final sprint. The same quality as month four, month eight, month ten. No coasting.

---

### Story 2: The Handover Conversation With Tomasz

**Background:**

In the final week, you spend an hour with Tomasz going through the Accounts module live — not a formal handover meeting, just a conversation. Tomasz will be the most likely person to pick up module ownership after you leave, or to answer questions about it until a replacement is found.

This story is short. It is worth telling because the handover conversation is the final act of genuine ownership.

---

**S — Situation:**

Thursday of the final week. Your last PR has merged. The deployment is stable. You message Tomasz: "want to do a quick walkthrough of the accounts module before I leave? Thirty minutes, no agenda, just questions."

Tomasz: "yes — tomorrow morning?"

---

**T — Task:**

Transfer the tacit knowledge that the documentation could not fully capture. The things you know from experience that cannot easily be written down — the judgment calls, the instincts, the "I would look here first" knowledge.

---

**A — Action:**

You open the module in your editor and walk Tomasz through five things in 45 minutes:

**First — the GoCardless callback handler.** You explain why `handleCallbackSuccess` does the GoCardless API calls before opening the `$transaction` block — a decision from the Month 3 incident that you fixed after accidentally removing the outbox event write. Tomasz was not on the team then. The comment in the code explains what to do, but not why the ordering matters. You explain it directly.

**Second — the sync lock duration.** You explain why `lockDuration` is 60 seconds and not the BullMQ default of 30 — the stall incident from Month 5, the profiling that showed large accounts taking 35 to 45 seconds, the decision to extend the lock rather than split jobs. Tomasz nods: "I remember the stall alerts. I never knew the full story."

**Third — Deutsche Bank's non-standard IBAN format.** The workaround Elena wrote 14 months ago, now documented, but still worth pointing at directly so Tomasz knows which function handles it and what to expect if another German bank has the same issue.

**Fourth — the consent deduplication key pattern.** Why the key uses today's date rather than the expiry date — the edge case you and Lucas worked through during the Month 4 review, where the expiry-date key would have caused a 24-hour dedup window that was actually 7 days by accident.

**Fifth — how to read Bull Board during a sync incident.** The specific things to look at first, the difference between a stalled job and a failed job in the UI, and the rule about waiting 2 minutes before manually intervening in a stalling job.

Tomasz takes notes throughout. At the end he says: "this was more useful than the documentation. Not because the documentation is bad — it's good. But hearing you explain why the decisions were made is different from reading that they were made."

You say: "that is why I wanted to do this rather than just send you a link."

---

**R — Result:**

Three weeks after you leave, Tomasz handles a sync incident solo — a GoCardless token refresh issue affecting a subset of German bank users. He resolves it in 18 minutes. He messages you on LinkedIn afterwards: "used the debugging steps from your docs. Deutsche Bank IBAN thing came up too — good I knew about it."

You had already left. The knowledge stayed.

---

### Story 3: The Final 1:1 With Daniel

**Background:**

The last Friday of your contract. Daniel runs a closing 1:1 — the same calendar slot as every monthly 1:1 for the past year, but different in feel.

---

**S — Situation:**

You join the Google Meet at 15:00 IST. Daniel is in his home office in Hamburg. He says: "let me start with the feedback from the team, and then I want to hear from you."

---

**T — Task:**

Receive feedback honestly, reflect on the year accurately, and leave the conversation with a clear sense of what you built and where you are going.

---

**A — Action:**

Daniel shares feedback from Lucas, Tomasz, and Arjun. He does not read it verbatim — he synthesises it:

"From Lucas: technically solid, grew noticeably in the second half of the contract, started thinking about the system rather than just the module, post-mortem writing showed good engineering judgment. Room for growth: estimating cross-module work still sometimes underestimates coordination overhead."

"From Tomasz: collaborative, caught real issues in reviews, the module documentation is genuinely useful and already being referenced. Mentioned specifically that you proactively flagged the retry logic concern in Month 4 with data rather than just opinion — that stood out to him."

"From Arjun: reliable reviewer, caught the N+1 issue in the final sprint which he appreciated. Natural to work with."

Daniel adds his own observation: "what I've seen over twelve months is someone who came in knowing how to write code and left knowing how to build software in a team. Those are different things. You learned when to ask for help, how to take feedback without flinching, how to own a module and what that actually means beyond just being the one who fixes it when it breaks. That is not a small thing for a first production contract."

You say something you have been thinking about for weeks: "the hardest part was the first time I caused a problem in production. The deployment incident in Month 3. I remember expecting Lucas to be frustrated and instead he just said 'write the fix, write the test, deploy it together.' That set the tone for how I thought about mistakes for the rest of the year."

Daniel: "Lucas is good at that. And so were you — you didn't spend three days feeling bad about it. You fixed it and moved on. That is exactly the right response."

He asks if you would consider renewing if budget opens. You say yes without hesitation.

The call ends after 35 minutes. You close your laptop and sit for a moment.

---

**R — Result:**

The result of the closing 1:1 is not a metric. It is the confirmation that the year was real — that the work mattered, that the growth was visible, that the care you brought to the final sprint was noticed and valued in the same way as everything that came before it.

Lucas sends a Slack message on your last day. Seven words: "Good working with you. Keep building things."

You screenshot it and save it.

---

## What Month 12 Taught You Overall

Three things, each distinct:

**From the final sprint feature:** Quality is a habit, not an effort. You did not work harder in Month 12 than in Month 6. You worked the same way. The consent re-auth feature got four PRs, proper review, staging verification, and a clean deployment — not because you were trying to make a good impression on your way out, but because that is how you had learned to work. Habits maintained under low stakes are real habits. Habits that only appear when someone is watching are performances.

**From the handover conversation:** Documentation is necessary but not sufficient. The written record preserves facts and procedures. The conversation preserves judgment — the reasoning behind decisions, the instincts built from experience, the "I would look here first" knowledge that is impossible to fully encode in prose. Both matter. Neither replaces the other.

**From the closing 1:1:** The most important things you learned in twelve months were not technical. The technical knowledge was real and valuable — BullMQ, distributed tracing, transaction handling, API design. But the more important learning was how to work: how to receive feedback, how to take ownership, how to disagree with evidence rather than emotion, how to write a post-mortem, how to mentor someone, how to stay consistent when nobody is watching. These are the things that will travel with you to every job after this one. The specific tools will change. These will not.

---

## Quarter 4 Complete

```
┌──────────────────────────────────────────────────────────────────┐
│              QUARTER 4 — THE COMPLETE ARC                        │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Month 10: Working Under Pressure                                │
│  → Shadowed the GoCardless rate storm                            │
│  → Contributed code under live incident conditions               │
│  → Learned: stabilise first, always                              │
│  → Wrote the first post-mortem section                           │
│  → Learned: blameless means explanatory, not vague               │
│                                                                  │
│  Month 11: Leaving Things Better Than You Found Them             │
│  → Built self-directed observability improvements                │
│  → Per-bank error rate, stale user metric, dashboard panels      │
│  → Wrote comprehensive module documentation                      │
│  → Learned: unrequested improvement work is ownership            │
│  → Learned: writing forces precision you did not know you lacked │
│                                                                  │
│  Month 12: Finishing Strong                                      │
│  → Shipped consent re-auth flow in the final sprint              │
│  → Four PRs, proper review, clean deployment                     │
│  → Handover conversation with Tomasz                             │
│  → Closing 1:1 with Daniel                                       │
│  → Learned: quality is a habit, not an effort                    │
│  → Learned: the most important things learned were not technical │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## The Full 12-Month Arc — Complete

```
┌────────────────────────────────────────────────────────────────────────┐
│                     FINVERSE — 12 MONTHS                               │
├───────────────────┬────────────────────────────────────────────────────┤
│  QUARTER 1        │  From knowing code to using it in production       │
│  Aug – Oct 2023   │  First PR, first code review, first mistake,       │
│                   │  first module ownership                            │
├───────────────────┼────────────────────────────────────────────────────┤
│  QUARTER 2        │  From fixing things to improving systems           │
│  Nov 2023         │  First feature designed end to end,                │
│  – Jan 2024       │  performance investigation with measured results,  │
│                   │  first technical disagreement handled well         │
├───────────────────┼────────────────────────────────────────────────────┤
│  QUARTER 3        │  From module owner to system thinker               │
│  Feb – Apr 2024   │  Proactive bug discovery, incident shadowing,      │
│                   │  notification race condition fix, Series B prep    │
├───────────────────┼────────────────────────────────────────────────────┤
│  QUARTER 4        │  From executor to contributor                      │
│  May – Aug 2024   │  Live incident contribution, post-mortem writing,  │
│                   │  self-directed observability, module documentation,│
│                   │  final sprint shipped cleanly                      │
└───────────────────┴────────────────────────────────────────────────────┘
```