# Story 11: The Tomás Conflict — First Time You Disagreed With a Peer

---

## Context — Your Relationship With Tomás Up to Month 9

```
Tomás Novák joined the team
4 months before you.
Czech Republic, remote.
3.5 years of experience.
Full-stack leaning backend.
Spring Boot + some React.

When you joined in month 1,
Tomás was the person who
showed you "how things are done here."

Which Slack channel to use.
How to pick up a ticket.
How the Docker Compose setup worked.
Practical, day-to-day navigation.

He was pragmatic.
Moved fast.
Sometimes skipped documentation
(caused friction with Elena occasionally).
But generally good to work with.

Your relationship through month 8:
────────────────────────────────────
Friendly. Collaborative. Easy.
You worked alongside each other
on the same service.
You never directly disagreed on
anything that mattered.

Month 9 changed that.
```

---

## The Situation

It was week 2 of month 9. Sprint 46. You had just wrapped Story 10 (the Payment Service schema discussion) and were back on regular sprint work.

The team had a feature on the board:

```
Ticket: EXP-284
Title:  Invoice Approval — Skip Verification
        Step for Trusted Suppliers

Background:
────────────
Currently ALL invoices go through
three steps before payment:
1. Finance manager uploads and reviews
2. Assigned verifier confirms goods/services
   received (VERIFIED status)
3. Finance manager approves for payment

Customers have been requesting the ability
to designate certain suppliers as "trusted"
— long-standing vendors where the
verification step is unnecessary overhead.

For trusted suppliers, the flow should be:
1. Finance manager uploads
2. Directly to approval (skip verification)

Story points: 5
Not yet assigned.
```

In sprint planning, Lukas asked who wanted to take it. Tomás spoke up:

```
Tomás (sprint planning):
─────────────────────────
"I'll take it. I know the
 invoice workflow well."
```

Lukas: "Good. [Your name] — you've been
going deep on Invoice Service lately.
Want to pair with Tomás on this one?
Two sets of eyes on the state machine."

You: "Sure."

That was the arrangement.
You and Tomás would work on it together.

---

## Day 1 — Looking at the Existing Code Together

You and Tomás jumped on a call Monday morning to review the existing invoice workflow before writing anything.

You opened `InvoiceService.java` together. The current state machine was:

```
DRAFT
  → PENDING_REVIEW
  → VERIFIED
  → PENDING_APPROVAL
  → APPROVED
  → PAYMENT_PENDING
  → PAID
```

The ticket required that for trusted suppliers, the flow become:

```
DRAFT
  → PENDING_REVIEW
  → PENDING_APPROVAL   ← skip VERIFIED
  → APPROVED
  → PAYMENT_PENDING
  → PAID
```

Tomás's immediate proposal:

```
Tomás:
───────
"Simple. When a finance manager
 uploads an invoice for a trusted
 supplier, we just don't create
 the verification step at all.

 In InvoiceService.createInvoice():
 check if supplier is trusted.
 If yes: set status directly to
 PENDING_APPROVAL instead of
 PENDING_REVIEW.
 If no: current flow, set to
 PENDING_REVIEW as normal.

 One if-statement. Done in a day."
```

You read through the code while he was talking. You understood what he was proposing. And you had an immediate concern.

```
You:
─────
"One question — where does
 the trusted supplier flag live?

 Is it in our DB or in User & Org Service?"
```

Tomás:

```
Tomás:
───────
"Doesn't matter for now.
 We can add a trusted flag
 to the suppliers table.
 Just a boolean column.
 Flyway migration, done."
```

You read the `suppliers` table schema. Then you looked at the `InvoiceService.createInvoice()` method more carefully. You noticed something:

```java
// Current createInvoice() — simplified
@Transactional
public InvoiceResponse createInvoice(
        CreateInvoiceRequest request,
        MultipartFile document,
        UUID uploadedById,
        UUID companyId) {

    // ... save to S3, create invoice record ...

    invoice.setStatus(InvoiceStatus.DRAFT);
    invoice.setUploadedById(uploadedById);

    // Trigger OCR asynchronously
    ocrService.triggerOcrAsync(invoice.getId());

    // Status moves to PENDING_REVIEW
    // AFTER OCR completes (via event)
    // Not here — that's handled in
    // OcrCompletedConsumer

    invoiceRepository.save(invoice);
    return InvoiceResponse.from(invoice);
}
```

The status transition to `PENDING_REVIEW` didn't happen in `createInvoice()` at all. It happened in `OcrCompletedConsumer` — after the async OCR job finished extracting data from the PDF.

You pointed this out:

```
You:
─────
"Status doesn't move to PENDING_REVIEW
 in createInvoice().
 It happens in OcrCompletedConsumer,
 after the OCR job finishes.

 So if we want to skip PENDING_REVIEW
 for trusted suppliers, the check
 can't go in createInvoice().
 It needs to go in OcrCompletedConsumer —
 where the actual transition happens."
```

Tomás:

```
Tomás:
───────
"Oh. Yeah, you're right.
 Then we do the check in the consumer.
 Same idea — if trusted supplier,
 go directly to PENDING_APPROVAL
 instead of PENDING_REVIEW.
 One if-statement in the consumer.
 Still simple."
```

---

## Where the Disagreement Started

This is where it got harder.

You thought about the full flow more carefully. If you put the trusted supplier check in `OcrCompletedConsumer`, you were adding business logic to a Kafka consumer. The consumer's job was to handle OCR completion. Was "should we skip verification for trusted suppliers" really the consumer's concern?

More importantly: `OcrCompletedConsumer` already had a specific, narrow responsibility. You had been in the codebase long enough to recognize the pattern — consumers handled technical transitions, service methods handled business decisions.

You said:

```
You:
─────
"I'm not sure I'm comfortable
 putting the trusted supplier
 check in the consumer.

 The consumer handles OCR completion —
 that's a technical event.
 Whether to skip verification based
 on supplier trust level is
 a business rule.

 If we put business rules in consumers,
 they become harder to test and
 harder to understand independently.

 My instinct is: the consumer fires
 a 'OCR completed' event or calls
 a service method. The service method
 has the business logic about
 what status to assign based on
 the supplier type.

 The consumer stays thin."
```

Tomás pushed back immediately:

```
Tomás:
───────
"That's overcomplicating it.
 We're adding one if-statement.
 Service method, consumer —
 it doesn't matter at this scale.

 We're not Google.
 The whole point of Series B
 is to ship fast.
 One if-statement in the consumer
 is the simplest thing that works.
 We test the consumer end-to-end,
 it covers the business logic too.

 Let's not over-engineer."
```

```
You felt the friction land.

"We're not Google."
"Don't over-engineer."

These weren't wrong statements
in general. You had heard them
used correctly many times.
At Series B you don't build
for FAANG scale.

But this wasn't about scale.
This was about where to put
business logic.
And you thought Tomás was
conflating "simple to write now"
with "simple to maintain later."

The question was: do you push back?
Or do you defer, since Tomás
has more experience?
```

You took a breath and tried to make your concern more concrete instead of abstract:

```
You:
─────
"Let me try to make my concern
 more specific.

 If the business rule lives in
 the consumer, when do we test it?

 Unit testing a Kafka consumer
 requires either mocking Kafka
 or running a Testcontainers instance.
 That's a lot of setup to test
 one if-statement about supplier
 trust level.

 If the rule lives in a service method,
 we can test it with a plain unit test —
 mock the supplier repository,
 call the method, assert the status.
 Two lines.

 That's the practical difference
 I'm worried about.
 Not architecture purity — testability."
```

Tomás was quiet for a few seconds. Then:

```
Tomás:
───────
"That's a fair point about testing.
 But I still think you're making
 this bigger than it needs to be.

 Why don't we just go with my approach,
 ship it, and if it becomes a problem
 we refactor it?
 Spending 30 minutes arguing about
 where to put one if-statement
 is itself a waste of time."
```

The conversation had shifted. It wasn't just about the code anymore. There was an edge in "spending 30 minutes arguing" that wasn't fully technical.

You felt the conversation getting uncomfortable. And you noticed the discomfort and chose not to back down — but also not to escalate.

```
You:
─────
"I hear you.
 I don't want to over-discuss
 a small decision either.

 Can we do this:
 I'll take 20 minutes and sketch
 both approaches in code.
 Not full implementation —
 just the structure.
 Then we can look at them
 side by side and decide.

 If yours is clearly simpler
 when I actually write it out,
 I'll go with it.
 Fair?"
```

Tomás:

```
Tomás:
───────
"Fine. But don't take all day."
```

---

## Writing It Out — The Sketch

You spent 25 minutes writing the two approaches side by side. Not full implementations — just the key structure.

**Option A — Tomás's approach (logic in consumer):**

```java
// OcrCompletedConsumer.java
@KafkaListener(topics = "ocr.completed",
    groupId = "invoice-service")
public void handleOcrCompleted(
        OcrCompletedEvent event,
        Acknowledgment acknowledgment) {

    try {
        Invoice invoice = invoiceRepository
            .findById(UUID.fromString(
                event.getInvoiceId()))
            .orElseThrow();

        invoice.setOcrExtractedData(
            event.getExtractedData());

        // BUSINESS LOGIC IN CONSUMER:
        Supplier supplier = supplierRepository
            .findById(invoice.getSupplierId())
            .orElseThrow();

        if (supplier.isTrusted()) {
            invoice.setStatus(
                InvoiceStatus.PENDING_APPROVAL);
            // Set approver, notify...
        } else {
            invoice.setStatus(
                InvoiceStatus.PENDING_REVIEW);
            // Set verifier, notify...
        }

        invoiceRepository.save(invoice);
        acknowledgment.acknowledge();

    } catch (Exception e) {
        log.error("OCR completed handling failed", e);
    }
}
```

**Option B — Your approach (logic in service):**

```java
// OcrCompletedConsumer.java — stays thin
@KafkaListener(topics = "ocr.completed",
    groupId = "invoice-service")
public void handleOcrCompleted(
        OcrCompletedEvent event,
        Acknowledgment acknowledgment) {

    try {
        invoiceService.processOcrCompletion(
            UUID.fromString(event.getInvoiceId()),
            event.getExtractedData()
        );
        acknowledgment.acknowledge();

    } catch (Exception e) {
        log.error("OCR completed handling failed", e);
    }
}

// InvoiceService.java — business logic lives here
@Transactional
public void processOcrCompletion(
        UUID invoiceId,
        OcrExtractedData extractedData) {

    Invoice invoice = invoiceRepository
        .findById(invoiceId).orElseThrow();

    invoice.setOcrExtractedData(extractedData);

    Supplier supplier = supplierRepository
        .findById(invoice.getSupplierId())
        .orElseThrow();

    // BUSINESS LOGIC IN SERVICE:
    if (supplier.isTrusted()) {
        invoice.setStatus(
            InvoiceStatus.PENDING_APPROVAL);
        assignApprover(invoice);
    } else {
        invoice.setStatus(
            InvoiceStatus.PENDING_REVIEW);
        assignVerifier(invoice);
    }

    invoiceRepository.save(invoice);
}
```

You wrote the unit tests for both options side by side:

```java
// UNIT TEST — Option A (consumer with logic):
// Requires Spring Kafka test setup or Testcontainers
@SpringBootTest
@Testcontainers
class OcrCompletedConsumerTest {

    @Container
    static KafkaContainer kafka =
        new KafkaContainer(...);

    // ... 30+ lines of setup ...

    @Test
    void whenTrustedSupplier_shouldSkipVerification() {
        // Need to publish event to Kafka,
        // wait for consumer to process,
        // then assert DB state.
        // Async. Slow. Complex.
    }
}

// UNIT TEST — Option B (service with logic):
// Plain JUnit, no Kafka needed
@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @InjectMocks
    private InvoiceService invoiceService;

    @Test
    void whenTrustedSupplier_shouldSkipVerification() {

        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = buildInvoice(invoiceId);
        Supplier trustedSupplier =
            buildSupplier(true); // trusted = true

        given(invoiceRepository.findById(invoiceId))
            .willReturn(Optional.of(invoice));
        given(supplierRepository
            .findById(invoice.getSupplierId()))
            .willReturn(Optional.of(trustedSupplier));

        invoiceService.processOcrCompletion(
            invoiceId,
            new OcrExtractedData()
        );

        assertThat(invoice.getStatus())
            .isEqualTo(InvoiceStatus.PENDING_APPROVAL);
    }
}
// 10 lines to test the exact business rule.
// No Kafka. No containers. Runs in milliseconds.
```

You sent Tomás both sketches in Slack with a short message:

```
You (Slack to Tomás):
──────────────────────
"Here are both approaches.
 The main practical difference
 ends up being in testing.

 Option A — business logic in consumer:
 Testing the trusted supplier rule
 requires Kafka (Testcontainers).
 Slower test, more setup.

 Option B — logic in service:
 Testing the rule is a plain unit test.
 10 lines. No Kafka needed.
 Consumer test just verifies
 it calls the service.

 I think Option B is genuinely simpler
 when you factor in test writing.
 But I could be wrong —
 what's your read?"
```

Tomás didn't reply for 20 minutes. Then:

```
Tomás:
───────
"I see what you mean about the tests.
 Option B is cleaner for that.

 Fine. Let's go with your approach.
 But I want to write the implementation —
 it's my ticket."
```

```
You:
─────
"Of course. It's yours.
 I'll review the PR."
```

---

## What Happened With Elena — The Unexpected Mention

You hadn't planned to tell Elena about the disagreement. It wasn't a big deal — you and Tomás had worked it out. But it came up naturally in your weekly tech sync when Elena asked how the pairing with Tomás was going.

You told her briefly — you had disagreed on where to put the business logic, you wrote out both approaches with tests, Tomás decided to go with Option B.

Elena's reaction was measured:

```
Elena:
───────
"Two things.

 First: you made the right call.
 Business logic in a Kafka consumer
 is one of those things that feels
 harmless the first time and
 becomes a maintenance problem
 by the tenth time.

 Second — and this is more important —
 how you handled it mattered.

 You could have just agreed with Tomás
 to avoid friction.
 You could have gone to Lukas to
 resolve it.

 Instead you made it concrete.
 'Here are both approaches written out.
  Here is the specific difference.
  What do you think?'

 That's the right way to handle
 a peer disagreement.
 Not arguing about principles.
 Not escalating to a manager.
 Making the tradeoff visible
 so the other person can evaluate it.

 Tomás is pragmatic — once he
 could see the testing difference
 clearly, he changed his mind.
 That's also good judgment on his part.

 Remember this for future disagreements.
 Abstract arguments go in circles.
 Concrete examples resolve them."
```

```
"Abstract arguments go in circles.
 Concrete examples resolve them."

You wrote this down immediately.
```

---

## The Implementation — What Tomás Built

Tomás implemented Option B over the next two days. You reviewed the PR on Thursday.

The implementation was clean. You left two comments — both minor, both addressed quickly. PR merged Thursday afternoon.

The key service method Tomás wrote:

```java
// InvoiceService.java
@Transactional
public void processOcrCompletion(
        UUID invoiceId,
        OcrExtractedData extractedData) {

    Invoice invoice = invoiceRepository
        .findById(invoiceId)
        .orElseThrow(() ->
            new InvoiceNotFoundException(invoiceId));

    if (invoice.getStatus() != InvoiceStatus.DRAFT) {
        log.warn("Unexpected status {} for invoice {} " +
            "during OCR completion. Skipping.",
            invoice.getStatus(), invoiceId);
        return;
    }

    invoice.setOcrExtractedData(extractedData);

    // Trusted supplier check —
    // determines whether to skip
    // verification step
    Supplier supplier = supplierRepository
        .findById(invoice.getSupplierId())
        .orElseThrow(() ->
            new SupplierNotFoundException(
                invoice.getSupplierId()));

    if (supplier.isTrusted()) {
        // Skip verification — go directly
        // to approval workflow
        invoice.setStatus(
            InvoiceStatus.PENDING_APPROVAL);
        assignApproverForInvoice(invoice);

        log.info("Invoice {} for trusted supplier {} " +
            "skipped verification step. " +
            "Moving directly to approval.",
            invoiceId, supplier.getName());

    } else {
        // Standard flow — requires verification
        invoice.setStatus(
            InvoiceStatus.PENDING_REVIEW);
        assignVerifierForInvoice(invoice);
    }

    invoiceRepository.save(invoice);

    // Outbox event — same transaction
    outboxEventRepository.save(
        OutboxEvent.builder()
            .aggregateType("INVOICE")
            .aggregateId(invoiceId)
            .eventType("invoice.status.updated")
            .payload(buildStatusPayload(invoice))
            .build()
    );
}
```

The consumer Tomás wrote:

```java
// OcrCompletedConsumer.java — thin, as agreed
@KafkaListener(
    topics = "ocr.completed",
    groupId = "invoice-service"
)
public void handleOcrCompleted(
        OcrCompletedEvent event,
        Acknowledgment acknowledgment) {

    log.info("Received ocr.completed for invoiceId: {}",
        event.getInvoiceId());

    try {
        invoiceService.processOcrCompletion(
            UUID.fromString(event.getInvoiceId()),
            event.getExtractedData()
        );
        acknowledgment.acknowledge();

    } catch (DataAccessException e) {
        // Transient — retry
        log.error("DB failure processing ocr.completed " +
            "for invoiceId: {}. Will retry.",
            event.getInvoiceId(), e);

    } catch (PermanentException e) {
        log.error("Permanent failure processing " +
            "ocr.completed for invoiceId: {}.",
            event.getInvoiceId(), e);
        acknowledgment.acknowledge();

    } catch (Exception e) {
        log.error("Unexpected failure processing " +
            "ocr.completed for invoiceId: {}. " +
            "Will retry.",
            event.getInvoiceId(), e);
    }
}
```

Clean. Exactly the structure you had proposed. Tomás had implemented it well.

---

## Your Review Comment — One Thing You Added

You left one substantive comment in the PR:

```
Your PR comment on InvoiceService:
────────────────────────────────────
"The log.info for trusted supplier
 skip is good. One addition:

 Should we also log when a supplier
 IS going through the verification step?

 Right now we only log the trusted path.
 If someone is debugging 'why did
 this invoice skip verification?'
 they can search the logs.

 But 'why is this invoice in
 PENDING_REVIEW?' has no corresponding
 log entry.

 Suggest adding:

 log.debug('Invoice {} for standard supplier {} ' +
   'entering verification step.',
   invoiceId, supplier.getName());

 DEBUG level since it's the normal path —
 no need to INFO log the expected case."
```

Tomás: "Good catch. Added."

```
This was a small moment.
But it showed something:
you were reviewing Tomás's PR
with the same care you'd want
someone to review yours.
Not rubber-stamping because
he was more experienced.
Not nitpicking to prove a point.
Genuinely reading the code
and thinking about debuggability.

That's what peer review is for.
```

---

## What Happened Three Weeks Later

Three weeks after the ticket shipped, a bug report came in:

```
#expense-ap-alerts:
────────────────────
Customer reported: invoice for
a trusted supplier is still
showing verification step in
the UI even after marking
supplier as trusted.
```

Tomás investigated. He found the issue within 20 minutes:

```
Tomás (in #expense-ap-dev):
─────────────────────────────
"Found it. The trusted flag is
 checked at OCR completion time
 (when the invoice is created).

 If a supplier was marked as
 trusted AFTER the invoice was
 already in PENDING_REVIEW
 (already through OCR),
 the flag is never re-evaluated.
 The invoice stays in PENDING_REVIEW.

 Fix: add a separate endpoint to
 move an invoice from PENDING_REVIEW
 to PENDING_APPROVAL manually,
 for cases where the supplier
 trust status changed after upload.

 Or alternatively: check trust
 status in the verification step
 itself and skip if trusted.

 Elena — what's the right fix?"
```

Elena: "The manual transition endpoint
is cleaner. Add it to the backlog.
This is a known edge case for
retroactive trust changes."

You read this and noted something quietly: because the business logic was in `InvoiceService.processOcrCompletion()` and not in the Kafka consumer, Tomás was able to identify exactly where the logic lived and explain the bug clearly in two sentences.

If the logic had been in the consumer, the debugging path would have been less obvious — "why is the consumer not skipping verification?" rather than "why does the service not skip verification when trust status changes after invoice creation?"

You didn't say this out loud. It didn't need to be said. But you remembered it.

---

## The Conversation With Tomás — A Month Later

A month after the feature shipped, you and Tomás were pairing again on a different ticket. At some point during the call, with no particular context, Tomás said:

```
Tomás:
───────
"That thing with the consumer
 and the trusted supplier logic.
 You were right.

 I wasn't wrong about 'don't
 over-engineer.' But in that case,
 putting the logic in the service
 was actually simpler when you
 wrote the tests.

 I pushed back harder than I
 should have.

 I was annoyed because you were
 the junior questioning my design
 and I'd already decided.

 That was the wrong reason
 to push back."
```

```
You weren't expecting this.

You said:
──────────
"I appreciate you saying that.
 I wasn't sure whether to push
 back at all.

 Honestly, if you'd said no
 after I wrote the sketches,
 I probably would have gone
 with yours.
 The difference wasn't
 big enough to escalate."
```

Tomás:

```
Tomás:
───────
"That's the right instinct.
 A disagreement between peers
 on something this small —
 the worst outcome is both
 people dig in and nothing
 gets built.

 You made it easy to change
 my mind because you showed
 me something concrete.
 Not 'this is the right way.'
 'Here's both ways written out.
 What do you think?'

 Keep doing that."
```

```
This conversation meant something.

Not because Tomás validated you.
But because of what it showed
about how disagreements can end.

You hadn't been right because
you were smarter.
You hadn't won because you
argued harder.
You made it easy for Tomás
to see what you saw.
He looked at it and updated.

That's what good technical
disagreement looks like.
Not victory. Not submission.
Two people who both care about
the code and can say so directly.
```

---

## The Broader Reflection — What This Story Was About

```
By month 9, you had been
corrected many times.

Elena found bugs in your PRs.
Arjun explained things you
didn't know.
Sophie spotted gaps in your tests.
Finn showed you about N+1 queries.

Each time: someone more experienced
pointing at something you missed.

Story 11 was the first time
you pointed at something.
Not to someone junior.
Not to get credit.
To a peer with more experience
who had already made a decision.

And the way you did it mattered
more than being right.

You didn't say "your approach is wrong."
You said "let me write out both
and we can compare."

You didn't appeal to principles.
You showed a concrete difference
in how the two options tested.

You didn't escalate to Elena
when Tomás pushed back.
You stayed in the conversation
and made your concern more specific.

And when Tomás said "fine,
let's go with yours" —
you didn't gloat.
You said "it's your ticket,
you write it."

Technical disagreements between
peers are not about being right.
They're about finding the best
outcome together without
the relationship breaking.

You learned that in month 9.
```

---

## The "Tricky Question" Preparation

---

**Q1: "You disagreed with a more experienced peer. How did you decide it was worth pushing back rather than just going with his approach?"**

```
Two things made me decide
it was worth pushing back.

First: I had a specific concern,
not a vague feeling.
"Business logic in Kafka consumers
is harder to test" is concrete.
It's not "I think the architecture
is wrong" — it's "here is a
specific practical consequence
of this choice."

If I only had a vague discomfort —
"something feels off about this" —
I probably wouldn't have pushed back.
At junior level, vague discomfort
is more often inexperience than
genuine insight.
Specific concerns are different.

Second: the cost of being wrong
was low.
We weren't deciding on something
irreversible — like a DB schema
or a public API contract.
We were deciding where to put
a small piece of logic.
Even if Tomás's approach had shipped,
the refactor to move it to the
service layer later would have
been a one-hour PR.

So the downside of pushing back
and being wrong: slight friction
with Tomás, minor time spent.
The downside of not pushing back
when I was right: a pattern that
makes future similar tickets
harder to test.

Those asymmetries made pushing
back the right call.
```

---

**Q2: "Tomás said 'spending 30 minutes arguing about one if-statement is a waste of time.' Was he right?"**

```
Partially right.

He was right that the decision
itself wasn't worth 30 minutes
of abstract debate.
If we had spent that time
trading principles back and forth —
"clean architecture says..."
"pragmatism says..." —
he would have been completely right.

Where I'd push back on his framing:
making the tradeoff visible
isn't a waste of time.
It's the efficient path.

Spending 20 minutes writing
both approaches side by side,
with tests, removed the abstraction.
Instead of arguing about
"which is simpler in principle,"
we were looking at "which is
simpler in practice."

That 20 minutes saved us from
potentially 2 hours of circular
argument — or from shipping
something neither of us was
happy with.

The waste in disagreements isn't
taking time to look at the problem.
It's spending that time in debate
mode instead of evidence mode.
```

---

**Q3: "You said you would have gone with Tomás's approach if he'd said no after you showed the sketches. Why? If you believed Option B was better, why would you have dropped it?"**

```
Because the difference wasn't
large enough to be worth
further friction.

If the tradeoff had been
more significant — if Option A
caused actual test coverage gaps
that would let bugs through,
or if it would have caused
the kind of problem we saw
three weeks later —
I would have escalated to Elena
before dropping it.

But the concrete difference
was testability convenience.
Option A's tests would have been
slower and more complex.
That's a real cost — but not
a correctness problem.
The feature would have worked
either way.

In that situation, peer judgment
matters.
Tomás has more experience than me.
If he looked at both options
and still preferred his,
maybe he was seeing something
I wasn't.
Maybe his intuition about
maintenance complexity over time
was calibrated differently.
I don't always know better.

The line for me is:
if it's a correctness or
integrity issue — push hard, escalate.
If it's a style or approach
preference where reasonable
people can differ — make your
case clearly once, accept the
outcome if it goes the other way.
```

---

**Q4: "Three weeks later there was a bug — trusted suppliers marked as trusted AFTER invoice upload didn't get the skip. Does that mean the design was wrong?"**

```
No — but it showed a gap in
the requirements, not the design.

The design correctly implemented
the requirement as written:
"For trusted suppliers, skip
the verification step."

What the ticket didn't specify:
"What happens when a supplier's
trust status changes after
an invoice is already in flight?"

That's a separate, harder requirement.
And importantly — it would have
been a gap REGARDLESS of whether
we put the logic in the consumer
or the service.

What the service approach made better:
when the bug was found, Tomás
could say immediately and clearly:
"The check happens at OCR completion
in processOcrCompletion().
If trust status changes after that point,
the check isn't re-evaluated."

That's a clean, precise diagnosis.
Because the logic is in one place
with a clear name.

If the logic had been in the consumer,
the diagnosis might have been:
"The consumer checks trust at
OCR completion... or wait,
is it checked somewhere else too?
Let me trace through the consumer code."
Harder to navigate.

The design being testable and
clearly located made the bug
faster to find and explain.
That's the practical value of
the approach — not architecture purity,
but debuggability in production.
```

---

Block 3 complete. All four stories done.

```
BLOCK 3 SUMMARY —
"Expanding Horizon" (Months 7-9):
──────────────────────────────────────────

Story 8 (Kafka consumer):
  First Kafka consumer in production.
  Transient vs permanent failures.
  Manual offset commits.
  Idempotency.
  847 stuck reimbursements resolved.

Story 9 (Production incident):
  First time inside a live incident.
  Watched Arjun debug by elimination.
  Wrote postmortem.
  Flyway non-transactional migration.

Story 10 (Cross-team schema discussion):
  First time representing your team
  to another team.
  Forward AND backward compatibility.
  Product implication flagged and escalated.
  Ravi adopted the pattern.

Story 11 (Tomás conflict):
  First peer disagreement.
  Made it concrete instead of abstract.
  Let Tomás write the implementation.
  Three weeks later: the design's
  clarity helped diagnose a bug faster.
  A month later: Tomás said you were right —
  and explained why he had pushed back.

The arc across Block 3:
  Months 7-9 were about expanding
  your operational radius.

  Month 7: working within your team
           on new technology.
  Month 8: in the incident room,
           watching and contributing.
  Month 9: across team boundaries,
           and disagreeing with a peer
           without the relationship breaking.

  Each story required a different
  kind of courage than the one before.
  Technical courage. Situational courage.
  Professional courage.

Ready for Block 4 when you are.
```