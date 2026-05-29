# Story 10: Cross-Team Schema Discussion — First Time You Spoke for Your Team

---

## Context — What "Cross-Team" Means at Series B

```
By month 8, you understood 
your team's services well.

Expense & Reimbursement Service.
Invoice & AP Service.
You knew the schemas.
You knew the Kafka topics.
You knew who owned what.

But here's something that becomes 
real only after you've been in 
production for a while:

Your service doesn't exist in isolation.

Payment Service publishes 
payment.completed.
You consume it.

User & Org Service owns 
employee and approval policy data.
You FeignClient into it.

If any of those services change 
something — a field renamed, 
a topic partition count changed,
a new required field added to 
an event payload —
your service breaks.

Or vice versa:
if YOU change something about 
how you produce or consume events,
THEIR service might break.

This interdependency is managed 
through coordination.
And coordination requires someone 
from your team to show up in 
a conversation with someone 
from their team.

Month 8. That someone was you.
With Arjun behind you.
```

---

## The Situation

It was the end of month 8, two weeks after the production incident (Story 9). The Kafka consumer lag incident had been resolved and the postmortem was published. You were back to normal sprint work.

Then a message appeared in `#engineering-all`:

```
Slack #engineering-all:
────────────────────────
From: Ravi Menon (Payment Service Team Lead)

"Hey all — Payment Service is planning 
 to evolve the payment.completed event 
 schema in our next sprint.

 We want to add two new required fields:
   - processingFee (BigDecimal)
   - processingFeecurrency (String)

 These are needed for our new multi-currency 
 fee calculation feature.

 Any teams consuming payment.completed 
 need to be aware and update their 
 consumers accordingly.

 Timeline: we plan to ship the schema 
 change in 2 weeks.

 Please reach out if you have concerns 
 or questions."
```

You read this and your stomach dropped slightly.

```
Your immediate reaction:
─────────────────────────
"We consume payment.completed.
 I just wrote that consumer 
 two months ago.
 
 If they add REQUIRED fields and 
 we don't update our consumer,
 what happens?
 
 Will deserialization fail?
 Will the consumer crash?
 Will messages go to the DLT?
 
 And wait — they said 'required.'
 But our PaymentCompletedEvent class 
 doesn't have those fields.
 When we deserialize a message 
 with new fields our class 
 doesn't know about...
 what does Jackson do?"
```

You forwarded the Slack message to Arjun:

```
You (Slack DM to Arjun):
──────────────────────────
"Saw this in #engineering-all.
 We consume payment.completed 
 in expense-service and invoice-service.
 
 If they add required fields and 
 we don't update our consumer class,
 what actually happens?
 Will it break?"
```

Arjun replied within 10 minutes:

```
Arjun:
───────
"Good catch — yes, this affects us.
 
 Quick answer: Jackson by default 
 ignores unknown fields when 
 deserializing. So new fields 
 in the Kafka message that our 
 class doesn't know about: 
 silently ignored. Not a crash.
 
 BUT — the real question is whether 
 'required' means the field has 
 a non-null constraint somewhere 
 that would cause validation failures 
 in our processing logic.
 Probably not, since we don't 
 validate those fields currently.
 
 The bigger concern: 
 should we be reading processingFee? 
 For reimbursements and invoice payments,
 do we need to record the fee 
 in our audit trail or 
 reimbursement record?
 
 That's a product question, 
 not just a technical one.
 
 I think we need to join the 
 conversation with Payment Service.
 
 I'll ask Ravi if we can have 
 a quick sync.
 But I want you in that meeting —
 you built the consumer, 
 you know it best.
 
 I'll be there too.
 But you're going to speak 
 for our team on the technical side."
```

```
"You're going to speak for our team 
 on the technical side."

That sentence sat with you for a moment.

Not "I'll speak and you can observe."
Not "sit in the background in case 
 I need to ask you something."

Arjun was going to be there.
But the expectation was that YOU 
would explain the impact on 
your consumer.

Month 8. First real cross-team 
technical conversation as a 
representative of your team.
```

---

## Your Task

```
Before the meeting, you needed to:
────────────────────────────────────
1. Understand exactly how the 
   schema change would affect 
   your consumer — technically

2. Know what questions to ask 
   Payment Service

3. Know what information they'd 
   need from you

4. Understand the options and 
   have a preference

Arjun told you this explicitly:
────────────────────────────────
"Before the meeting, answer 
 three questions:
 
 1. Does the change break us 
    as written today?
 2. Do we need to do anything 
    to handle it?
 3. Do we need the new data 
    for our own logic, or is 
    it just pass-through?"
```

---

## Preparing for the Meeting

You spent the evening before the call going through the consumer code carefully. You started from the actual class that deserialized the event:

```java
// What your PaymentCompletedEvent looks like currently
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent {

    private String paymentId;
    private String reimbursementId;
    private String employeeId;
    private BigDecimal amount;
    private String currency;
    private String paymentReference;
    private Instant completedAt;
    private PaymentType paymentType;
}
```

No `processingFee`. No `processingFeeCurrency`.

**Question 1: Does it break?**

You wrote a small test locally to understand Jackson's behavior:

```java
// You wrote this test to verify your 
// understanding before the meeting

@Test
void jacksonIgnoresUnknownFieldsByDefault() 
        throws Exception {

    // Simulate a message with NEW fields 
    // that our class doesn't have
    String messageWithNewFields = """
        {
          "paymentId": "pay-123",
          "reimbursementId": "reimb-456",
          "employeeId": "emp-789",
          "amount": 85.00,
          "currency": "EUR",
          "paymentReference": "MOSS-001",
          "completedAt": "2025-03-15T10:30:00Z",
          "paymentType": "REIMBURSEMENT",
          "processingFee": 0.50,
          "processingFeeCurrency": "EUR"
        }
        """;

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(
        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, 
        false  // ← Spring Boot default
    );

    // Should NOT throw
    PaymentCompletedEvent event = mapper.readValue(
        messageWithNewFields, 
        PaymentCompletedEvent.class
    );

    assertThat(event.getPaymentId()).isEqualTo("pay-123");
    assertThat(event.getAmount())
        .isEqualByComparingTo("85.00");
    // processingFee is silently ignored
}
```

It passed. No exception. Jackson ignored the unknown fields silently.

```
Answer to Question 1:
──────────────────────
Does the schema change break us 
as written today?

NO — as long as:
1. Spring Boot's Jackson default 
   (FAIL_ON_UNKNOWN_PROPERTIES = false) 
   is in effect — it is.
2. We don't have any schema 
   validation that checks for 
   unexpected fields.

The consumer will continue to 
deserialize the message successfully.
The new fields will be silently ignored.
No crash. No DLT.
```

**Question 2: Do we need to do anything?**

This was the more nuanced question. Even if it didn't break, should you update the class?

You thought through three scenarios:

```
Scenario A: We DON'T update our class.
──────────────────────────────────────
Messages arrive with processingFee.
Our class silently ignores it.
We process the reimbursement normally.
processingFee is never stored, never logged.

Risk: if we later need processingFee 
      for audit, reporting, or compliance,
      we have no historical data.
      We'd need to replay Kafka events 
      to backfill — complex, error-prone.

Scenario B: We update our class to 
            include the new fields.
────────────────────────────────────
Messages arrive with processingFee.
Our class captures it.
We have the data available.
We can decide later whether to 
store it or ignore it in logic.

Risk: code change needed now.
      We're carrying data we don't 
      currently use.

Scenario C: We update and STORE 
            the fee in our records.
────────────────────────────────────
Requires a DB migration too.
Changes our reimbursement schema.
Biggest scope but most complete.
```

You wrote your assessment:

```
YOUR NOTES (pre-meeting):
──────────────────────────
Recommendation:

Do Scenario B at minimum — 
update our class to capture 
the new fields.

Reason: data you don't capture 
today you can't get back tomorrow.
Kafka retention is 7 days.
If next quarter compliance requires 
fee tracking, we can't retroactively 
recover it from messages that 
already expired.

Whether to store in DB (Scenario C):
this is a product decision.
Do we need processing fees 
in the reimbursement record 
for compliance or reporting?

I don't know the answer.
That's a question for the meeting.
```

**Question 3: Do we need the data?**

You didn't know. That was honest. But you could frame the question correctly.

You wrote down what you'd ask in the meeting:

```
Questions for Payment Service:
────────────────────────────────
1. Is processingFee always present 
   or only for some payment types?
   (If only for certain currencies 
    or payment methods, we need to 
    handle null gracefully.)

2. Does Moss have any compliance 
   or reporting requirement to 
   track processing fees per 
   reimbursement?
   (This determines whether we 
    store it or just capture and ignore.)

3. What's the backward compatibility plan?
   (Will old messages without processingFee 
    still be published during transition?
    Or will all messages immediately 
    have the field?)
```

That third question was the most important technically. You had learned about schema evolution in the Kafka module. You knew that "required" in the context of an existing Kafka topic was more nuanced than it sounded.

---

## The Meeting

Three people: Ravi Menon (Payment Service Team Lead), Arjun, and you. Google Meet, 30 minutes.

Ravi opened:

```
Ravi:
──────
"Thanks for jumping on this.
 I know the timeline is tight.
 We want to ship the change 
 in sprint 47 — that's two weeks.
 
 The new fields are for our 
 multi-currency fee tracking feature.
 Compliance needs to see processing 
 fees per payment for FCA reporting."
```

Arjun nodded. He looked at you:

```
Arjun:
───────
"[Your name] has been through 
 the consumer code. 
 Walk Ravi through what we found."
```

You took a breath and went through it:

```
You:
─────
"We consume payment.completed in 
 two places — Expense Service 
 for reimbursements and Invoice Service 
 for invoice payments.
 
 I ran a test against our deserialization 
 logic with the new fields added.
 
 Short answer: our consumer won't break 
 as written today.
 Jackson ignores unknown fields by default,
 so messages with processingFee and 
 processingFeeCurrency will deserialize 
 correctly into our existing class 
 — the new fields are silently dropped.
 
 But I have three questions before 
 we decide what to do on our end."
```

You went through your prepared questions.

**On question 1 — is the field always present:**

```
Ravi:
──────
"Good question.
 processingFee will be present on 
 all messages going forward.
 But it CAN be zero — specifically 
 for SEPA payments within the EU 
 where we don't charge a fee.
 
 So: always present, sometimes zero.
 Never null."
```

You made a note. Always present, sometimes zero. This simplified handling — you didn't need to worry about null.

**On question 2 — compliance requirement to store fees:**

```
Ravi:
──────
"For reimbursements — that's 
 between you and your compliance team.
 I can tell you our side: we're 
 storing it for FCA reporting.
 Whether Expense Service also needs 
 to store it is your product call.
 
 I'd recommend checking with 
 your PM or Finance team."
```

Arjun: "We'll check with Lukas.
But for the technical discussion —
let's assume we want to capture it
even if we don't store it yet."

**On question 3 — backward compatibility:**

This was where the conversation got most interesting. You asked it directly:

```
You:
─────
"One more thing — how will you 
 handle the transition period?
 
 If you ship the new schema on 
 Monday and we ship our consumer 
 update on Wednesday, there's 
 a two-day window where your 
 messages have processingFee 
 but our class doesn't know about it.
 
 We already established that 
 doesn't break us — Jackson ignores it.
 But the reverse worries me more:
 after we update our class to 
 include processingFee, what happens 
 if for some reason you roll back 
 your change and send messages 
 WITHOUT processingFee?
 
 Our updated class would expect 
 the field — would it fail?"
```

Ravi paused.

```
Ravi:
──────
"That's a good edge case.
 
 If we send messages without 
 processingFee after you've updated 
 your class to include it...
 depends on how you annotate 
 the field in Java.
 
 If you mark it @NotNull, 
 Jackson would fail on null.
 If you leave it nullable, 
 Jackson would set it to null 
 and your logic handles it."
```

```
You:
─────
"So we should annotate it as 
 nullable — BigDecimal processingFee 
 with no @NotNull — and handle 
 null explicitly in our processing.
 
 That way:
 - Old messages without the field: 
   deserialize with processingFee = null
 - New messages with the field: 
   deserialize correctly
 - If you roll back: 
   we still work, just process with null
 
 Is that how you'd recommend we do it?"
```

```
Ravi:
──────
"Yes — exactly. 
 That's backward compatible on your side.
 
 And actually, I should mention:
 we're planning to add Avro + Schema Registry 
 for this topic in Q1 next year precisely 
 to manage this kind of evolution cleanly.
 For now, JSON + annotation discipline 
 is the right approach."
```

```
Arjun (speaking up for the first time):
─────────────────────────────────────────
"Agreed. One more thing:
 What's your rollout plan?
 Blue-green? Canary? 
 
 If you're doing a hard cutover 
 where all messages immediately 
 have the new schema,
 we want to make sure our consumer 
 update deploys before or simultaneously —
 not after."
```

```
Ravi:
──────
"We'll deploy the producer change 
 on Monday.
 Since consumers handle the 
 new fields gracefully even 
 without updating their class 
 (as [your name] confirmed),
 you can take a day or two to 
 ship your consumer update.
 No hard dependency on simultaneous deploy.
 
 But we'd appreciate your update 
 being in by Wednesday at the latest."
```

Arjun: "That works. We'll have it in by Tuesday."

Meeting ended. 30 minutes, all three questions answered.

---

## After the Meeting — What Arjun Said

You and Arjun stayed on the call after Ravi dropped:

```
Arjun:
───────
"Good job in there.
 
 The question about roll-back 
 compatibility — I'm glad you 
 asked that because I wasn't 
 going to.
 
 Most engineers only think 
 about forward compatibility —
 'if they add fields, will we break?'
 
 You also thought about 
 backward compatibility —
 'if they remove fields after 
 we've updated, will we break?'
 
 That's the harder direction 
 to think about.
 
 And you framed it correctly:
 not as a blocker, but as a 
 'here's how we handle this 
 safely on our side.'"
```

```
You:
─────
"I wasn't sure whether to raise it.
 Didn't want to sound like 
 I was creating problems 
 where there weren't any."

Arjun:
───────
"That's the right instinct to 
 question, actually.
 
 In cross-team discussions,
 your job is to surface risks 
 your team will have to handle.
 Not to block their work.
 Not to make yourself sound smart.
 
 You framed it perfectly:
 'here's the edge case,
  here's the simple fix on our side,
  does this approach work for you?'
 
 That's constructive.
 You're not asking them to change 
 their plan — you're clarifying 
 your own approach."
```

---

## The Implementation

After the meeting, you updated the `PaymentCompletedEvent` class in the `common-events` module:

```java
// Updated PaymentCompletedEvent
// Backward compatible — nullable new fields

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent {

    private String paymentId;
    private String reimbursementId;
    private String employeeId;
    private BigDecimal amount;
    private String currency;
    private String paymentReference;
    private Instant completedAt;
    private PaymentType paymentType;

    // New fields — nullable for backward compatibility.
    // Payment Service adds these in sprint 47.
    // Always present going forward (may be zero 
    // for EU SEPA payments with no fee).
    // Nullable here handles the case where 
    // Payment Service rolls back or sends 
    // pre-schema-change messages.
    //
    // See ADR discussion in #engineering-all 
    // Slack thread [date].
    private BigDecimal processingFee;
    private String processingFeeCurrency;
}
```

Then you updated the consumer in `ReimbursementService` to handle the fee data:

```java
@Transactional
public void processPaymentCompleted(
        PaymentCompletedEvent event) {

    UUID reimbursementId = UUID.fromString(
        event.getReimbursementId());

    Reimbursement reimbursement = 
        reimbursementRepository
            .findById(reimbursementId)
            .orElse(null);

    if (reimbursement == null) {
        throw new PermanentException(
            "Reimbursement not found: " + reimbursementId);
    }

    if (reimbursement.getStatus() == 
            ReimbursementStatus.COMPLETED) {
        log.warn("Reimbursement {} already COMPLETED. " +
            "Duplicate event ignored.",
            reimbursementId);
        return;
    }

    if (reimbursement.getStatus() == 
            ReimbursementStatus.PROCESSING) {

        Instant now = Instant.now();

        reimbursement.setStatus(
            ReimbursementStatus.COMPLETED);
        reimbursement.setPaymentReference(
            event.getPaymentReference());
        reimbursement.setCompletedAt(now);

        // Store processing fee if present.
        // Currently captured but not stored 
        // in DB — pending product decision 
        // on whether we need it for compliance.
        // If processingFee is null (old message 
        // format or pre-feature message):
        // log at DEBUG, treat as zero fee.
        if (event.getProcessingFee() != null 
                && event.getProcessingFee()
                        .compareTo(BigDecimal.ZERO) > 0) {
            log.debug("Processing fee captured: {} {}",
                event.getProcessingFee(),
                event.getProcessingFeeCurrency());
            // TODO: Store in reimbursement record 
            //       after product decision.
            //       Tracked in EXP-291.
        }

        reimbursementRepository.save(reimbursement);

        outboxEventRepository.save(
            OutboxEvent.builder()
                .aggregateType("REIMBURSEMENT")
                .aggregateId(reimbursementId)
                .eventType("reimbursement.completed")
                .payload(buildPayload(reimbursement, event))
                .build()
        );

        auditService.logReimbursementCompleted(
            reimbursementId,
            event.getPaymentReference(),
            event.getAmount()
        );

        return;
    }

    throw new PermanentException(
        "Unexpected reimbursement state: " + 
        reimbursement.getStatus()
    );
}
```

You also raised the product question with Lukas, as Arjun had suggested:

```
You (Slack to Lukas):
──────────────────────
"Quick product question from the 
 Payment Service schema discussion.
 
 They're adding processingFee to 
 payment.completed events.
 
 Do we need to store processing fees 
 per reimbursement in our system?
 E.g., for compliance, audit trail, 
 or customer-facing receipts?
 
 Currently capturing the field in 
 code but not storing in DB.
 Created EXP-291 to track this.
 
 Wanted to make sure this is the 
 right call before it slips through."
```

Lukas replied the next day:

```
Lukas:
───────
"Good catch to flag this.
 
 Spoke with the PM — yes, 
 we should store processing fees.
 Not immediately critical 
 but needed for the SEPA fee 
 transparency feature coming Q2.
 
 Add EXP-291 to sprint 48 backlog.
 You can scope it then."
```

```
This was a small thing.
But it was a product decision 
that might otherwise have been 
missed for months.

You didn't just implement the 
technical change.
You identified that a new data field 
had product implications,
raised it explicitly,
and got a decision before 
the data was silently lost.
```

---

## The PR

You opened the PR. Arjun reviewed it.

```
PR: EXP-288 — Update consumer 
              for payment.completed 
              schema change

What changed:
  - Added processingFee and 
    processingFeeCurrency to 
    PaymentCompletedEvent
  - Both fields nullable 
    (backward compatible)
  - Consumer handles null gracefully 
    (pre-schema-change messages)
  - TODO comment for EXP-291 
    (store fee in DB)
  - Test updated for new fields

Backward compatibility:
  Tested with both old format 
  (no processingFee) and new format.
  Both deserialize correctly.
  Consumer processes both without error.

JIRA: EXP-288
Related: EXP-291 (store processingFee, sprint 48)
```

Arjun's review: 2 comments.

```
Arjun Comment 1:
─────────────────
"The TODO for EXP-291 is fine 
 but add more context.
 
 Someone reading this six months 
 from now should understand:
 1. Why it's a TODO and not done now
 2. What they need to do to complete it
 
 Add: 'To complete: add processingFee 
 and processingFeeCurrency columns 
 to reimbursements table (Flyway migration),
 store in reimbursement.setProcessingFee(),
 include in outbox event payload.'"
```

Good catch. You updated the comment:

```java
// TODO: Store processingFee in reimbursement record.
// Tracked in EXP-291 (sprint 48 backlog).
//
// Deferred because: requires DB migration
// (processingFee, processingFeeCurrency columns)
// and product confirmation received after 
// this PR was opened.
//
// To complete:
//   1. Add Flyway migration for new columns
//   2. Set reimbursement.setProcessingFee(
//          event.getProcessingFee())
//      and reimbursement.setProcessingFeeCurrency(...)
//   3. Include fee in outbox event payload 
//      for reimbursement.completed downstream consumers
```

```
Arjun Comment 2:
─────────────────
"The log level for null processingFee 
 should be DEBUG not INFO.
 
 null processingFee means an old 
 message format — expected during 
 transition period and for any 
 message that predates the schema change.
 
 At INFO level this fills the logs 
 with noise during the transition.
 DEBUG only logs in dev profile.
 
 After the transition is complete 
 (say, 2 weeks post-deploy), 
 you could remove the log entirely 
 or keep it at TRACE."
```

You hadn't thought about log noise during the transition window. You updated it and added a note in the PR:

```
Updated log level to DEBUG per Arjun's 
review. Will revisit removal of this log 
in sprint 49 once transition is confirmed 
complete (all messages have processingFee).
```

PR approved. Merged Tuesday, as promised to Ravi.

---

## What Happened After — The Thank You Message

Three days after Payment Service deployed their schema change and your consumer update was in production, Ravi sent a message:

```
Ravi (Slack DM to Arjun, 
      copied you):
─────────────────────────────
"Hey — just wanted to say the 
 rollout went smoothly.
 
 No issues on your consumer side.
 Zero DLT messages.
 
 The backward compatibility approach 
 [your name] proposed worked perfectly.
 
 We'll be adopting the same pattern 
 for our next schema evolution."
```

```
Arjun forwarded it to you 
with one line:

"This is what good cross-team 
 collaboration looks like."
```

---

## The Broader Reflection — What This Story Was About

```
The technical content here was 
simpler than many of the 
previous stories.

Jackson ignoring unknown fields.
Nullable fields for backward compat.
A TODO with enough context to 
be actionable.

None of that was particularly complex.

The difficult part was something else:

Being in a room — even a virtual one —
with engineers from another team,
and speaking clearly about 
what your code does,
what it needs,
and what risks you see on your side.

Not deferring to Arjun to answer 
every question.
Not staying quiet and letting 
the senior engineers talk.
But preparing specific questions,
asking them clearly,
and proposing concrete solutions.

"We should annotate it nullable 
 and handle null explicitly.
 Does that approach work for you?"

That's not a junior hedging.
That's someone who has thought 
through the problem and is 
proposing a path forward.

Arjun's comment after the meeting — 
"most engineers only think about 
forward compatibility" — was the 
clearest evidence that you had 
contributed something real.

Not just attended the meeting.
Contributed.
```

---

## The "Tricky Question" Preparation

---

**Q1: "You said Jackson ignores unknown fields by default. But what if you wanted to enforce that no unknown fields exist — how would you do that?"**

```
You'd set FAIL_ON_UNKNOWN_PROPERTIES 
to true in the ObjectMapper.

Either globally:
@Bean
public ObjectMapper objectMapper() {
    return new ObjectMapper()
        .configure(
            DeserializationFeature
                .FAIL_ON_UNKNOWN_PROPERTIES, 
            true);
}

Or per class:
@JsonIgnoreProperties(
    ignoreUnknown = false
)
public class PaymentCompletedEvent { ... }

When would you want to enforce this?
In a scenario where you want to 
be explicit about any schema drift —
if a producer adds a field you 
don't know about, you want to 
FAIL fast rather than silently 
ignore it. This is the strict 
contract approach.

In our case, we want the opposite —
we want resilience to schema evolution,
so we keep the default 
(ignoreUnknown = true effectively).

For Kafka consumers specifically,
failing on unknown fields is 
generally a bad pattern.
It means any producer field addition 
breaks all consumers immediately.
The Schema Registry + Avro approach 
is the right long-term solution 
for enforcing contracts while 
still allowing controlled evolution.
Ravi mentioned that was on their 
roadmap for Q1.
```

---

**Q2: "You raised a question about rollback compatibility — if they removed the field after you'd updated your class, would you break? How did you ensure you wouldn't?"**

```
The risk was: we update our class 
to include processingFee (not null).
Payment Service ships the field.
Two days later, they roll back 
their change — messages go back 
to not having processingFee.

If our class had processingFee 
annotated as @NotNull or with 
a primitive type (like double), 
Jackson would fail to deserialize 
a message without the field.
That's a crash. Messages go to DLT.

The fix was simple: 
annotate the field as nullable.

private BigDecimal processingFee;   
// nullable — no @NotNull

When a message arrives without 
the field, Jackson sets it to null.
Our code checks:
if (event.getProcessingFee() != null ...) 

Null-safe, handles both old and 
new message formats identically.

This is the additive backward compat 
pattern for JSON events:
new fields should always be nullable 
in consumers until the producer 
has been stable for long enough 
that rollback is effectively off the table.

The Schema Registry + Avro approach 
that Ravi mentioned for Q1 manages 
this more formally — it enforces 
backward compatibility rules 
at schema registration time 
and prevents breaking changes 
from being published.
```

---

**Q3: "You raised a product question about storing the processing fee. How did you decide to raise it rather than just implement what was obvious?"**

```
The technical change was clear — 
add the fields, handle nulls, done.
I could have shipped that and moved on.

What made me raise the product question 
was one specific thought:
"Data you don't capture today, 
you can't recover tomorrow."

Kafka retains events for 7 days.
If we silently ignore processingFee 
for a month, and then compliance 
asks "can you give us all 
reimbursement processing fees 
from the last 3 months" —
we have nothing.

The events are gone.
The data was in our messages 
and we threw it away.

That's an irreversible decision 
made by silence.

Raising it to Lukas turned silence 
into an explicit decision:
"Yes, store it — we'll need it 
for the SEPA fee transparency 
feature in Q2."

Now we have a ticket, a plan, 
and we're capturing the data 
in the current messages even 
if we're not storing it in DB yet.

The principle: when a new data field 
appears in an event your service consumes,
always ask "do we need this?"
before deciding to ignore it.
The cost of asking is low.
The cost of not asking and 
being wrong is high.
```

---

**Q4: "This was your first cross-team discussion. What was different about it compared to your normal team conversations?"**

```
Two things were different.

First: the stakes of being wrong 
were higher.
In a normal team conversation,
if I say something incorrect,
Elena or Arjun corrects it in the moment.
Everyone already knows the codebase.
The correction is easy.

In a cross-team conversation,
if I say something incorrect about 
how our consumer works —
for example, if I had said 
"we'd need to update our consumer 
before you ship the change" 
when we actually didn't —
Ravi might have delayed a sprint 
unnecessarily.

That's a real coordination cost 
to another team based on a 
mistake I made.
So I verified everything I was 
going to say before I said it.
The test I wrote to confirm 
Jackson's behavior — I ran that 
specifically so I wouldn't 
walk into the meeting and 
guess under pressure.

Second: I was representing a position.
In normal team conversations,
I'm an individual with a view.
In this meeting, I was speaking 
on behalf of Expense Service 
and Invoice Service.
When I said "our consumer won't break,"
that was a statement about 
our team's code that Ravi 
would rely on.

That felt different.
More weight.
But Arjun's advice — 
"surface risks, propose solutions, 
don't create problems 
where there aren't any" — 
was the right framing.
I wasn't there to protect turf 
or make the meeting complicated.
I was there to make sure 
the schema change went smoothly 
for everyone.
```

---

Story 10 complete.

```
What this story demonstrates:
───────────────────────────────

Technical:
  Jackson FAIL_ON_UNKNOWN_PROPERTIES 
    default behavior — why it matters
    for Kafka schema evolution.
  Backward compatibility in JSON consumers —
    nullable fields, null-safe handling.
  Forward vs backward compatibility —
    the difference and why both matter.
  TODO comments with enough context 
    to be actionable.
  Log level choices during transition 
    periods (Arjun's comment).

Behavioral:
  Prepared with data before the meeting —
    ran an actual test to verify 
    Jackson behavior rather than guessing.
  Asked specific questions, 
    not vague ones.
  Proposed concrete solutions 
    ("annotate nullable and handle null 
    explicitly — does that work for you?")
  Identified a product implication 
    (processingFee storage) that 
    went beyond the technical ticket.
  Raised it explicitly rather 
    than making a silent decision.

Relationship:
  First real interaction with a 
    senior engineer from another team.
  Arjun trusted you to speak 
    for the team technically.
  Ravi adopted the backward compat 
    pattern for his next schema evolution.
  Cross-team reputation starts here.

Block 3 arc so far:
  Story 8: First Kafka consumer — 
    technical foundation laid.
  Story 9: Production incident — 
    seen how senior engineers 
    debug under pressure.
  Story 10: Cross-team discussion — 
    first time representing your 
    team to another team.
  
  Each story expanded your 
  operational radius.
  Month 7: within your team.
  Month 8-9: within the incident process.
  Month 9: across team boundaries.
```