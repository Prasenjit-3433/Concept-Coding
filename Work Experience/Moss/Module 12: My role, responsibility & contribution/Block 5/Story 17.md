# Story 17: Backward-Compatible Migration Proposal — First Time You Influenced a Product Decision With Technical Reasoning

---

## Context — Where You Were at Month 14

```
Month 13 had ended with the incident.
You had led it. You had fixed it.
You had written the postmortem.
Arjun had formally noted your growth 
in a permanent document.

Month 14 started quietly.

But something had changed in how 
people interacted with you 
in planning meetings.

Before month 10: 
  You listened in sprint planning.
  You raised concerns occasionally.
  You estimated your own tickets.
  
After month 10 (the caching design meeting):
  You raised concerns more often.
  People sometimes asked your opinion.
  But mostly about your own tickets.

Month 14:
  Something shifted again.
  
  In sprint planning, Lukas started 
  directing questions at you 
  that weren't about YOUR ticket.
  
  "What does the data migration 
   look like for this feature?"
  
  "Would this approach cause any 
   issues for existing records?"
  
  "How would this affect the 
   Invoice Service schema?"
  
  These were questions he used to 
  ask Elena or Arjun.
  Now he was asking you.
  
  You were becoming the person 
  in the room who was expected 
  to know the Invoice Service schema.
```

This mattered because of what happened in sprint planning, week 2 of month 14.

---

## The Situation

Sprint planning. Elena, Arjun, Sophie, Tomás, you, Lukas, and the PM — Priya from the product team (the same PM from Story 7, the bulk approval discussion).

Priya presented a new feature:

```
PM's request in sprint planning:
──────────────────────────────────
"We're getting consistent feedback 
 from finance teams that they want 
 to see a cleaner breakdown of 
 invoice line items on the approval 
 screen.
 
 Currently each line item has 
 a 'description' field — 
 free text, up to 500 characters.
 
 Finance teams want to categorize 
 line items with a standardized 
 code — like a service type or 
 product category.
 
 The proposal:
 
 Rename the 'description' field 
 in invoice_line_items to 'notes' 
 (it's more accurate to what 
 finance teams actually use it for),
 AND add a new required 'category_code' 
 field alongside it.
 
 The category codes would come from 
 a reference table we'd build.
 
 Story points estimate?
 Timeline: this sprint if possible."
```

Lukas looked around the room. Then directly at you:

```
Lukas:
───────
"[Your name] — you know the 
 Invoice Service schema well.
 What does this look like 
 from an implementation standpoint?"
```

The room waited.

You knew the schema. You knew what "rename the description field" meant in production terms. And you knew it was not simple.

```
You (in the meeting):
──────────────────────
"Before we estimate — 
 I want to flag a significant 
 concern with the rename.
 
 Renaming the 'description' column 
 to 'notes' is a breaking database 
 migration.
 
 I'd like to explain what that 
 means and then propose an approach 
 that gets us the same outcome 
 safely.
 
 Can I take 5 minutes?"
```

Lukas: "Go ahead."

---

## The Explanation — What You Said in the Meeting

You opened your laptop and pulled up the migration in Confluence.

```
You:
─────
"The invoice_line_items table 
 currently has a 'description' 
 column. That column is read 
 and written by the application 
 code in many places.
 
 If we rename it in one migration:

 ALTER TABLE invoice_line_items 
 RENAME COLUMN description TO notes;

 Here's what happens:

 Step 1: Migration runs in production.
         Column is now called 'notes'.
         
 Step 2: The new application code 
         that reads 'notes' deploys.
         Everything works.

 But what about the window between 
 Step 1 and Step 2?
 
 Or what if Step 2 has a bug 
 and we need to rollback to 
 the previous version of the 
 application code?

 The previous version of our code 
 reads 'description'.
 That column no longer exists.
 Every InvoiceLineItem query fails.
 Every invoice-related endpoint 
 returns 500 errors.
 We cannot roll back without 
 manually reversing the DB migration.
 
 That's a production incident 
 caused by our own rollback.
 
 This is why we have a team rule: 
 migrations must be backward compatible.
 Every migration we write must 
 be safe to run while the previous 
 version of the code is still 
 running — because there's always 
 a window where the migration 
 has run but the new code 
 hasn't deployed yet."
```

The room was quiet. Priya was frowning slightly — not defensively, but thinking.

```
Priya:
───────
"Okay — so we can't rename the column.
 But we need the field to be called 
 'notes' in the UI and the API.
 Does the DB column name have to 
 match the API field name?"
```

```
You:
─────
"No — it doesn't.
 
 The API returns JSON.
 The JSON field name is controlled 
 by the DTO, not the DB column.
 
 If we rename the Java field 
 in InvoiceLineItemResponse from 
 'description' to 'notes' with 
 @JsonProperty, the API starts 
 returning 'notes' without touching 
 the DB at all.
 
 But — I'd recommend against 
 that too, because it's a 
 breaking API change.
 Any client that currently reads 
 the 'description' field from 
 the response would break.
 
 What I'd actually propose is 
 a 5-step migration."
```

---

## The Proposal — What You Presented

You had thought about this quickly during Priya's presentation. You had seen the pattern in the module notes Elena had shared in month 3. You had seen it applied in a Flyway migration Arjun wrote in month 9. Now you were articulating it in a room of eight people as the solution to a product request.

```
You (presenting):
──────────────────
"Here's the approach I'd recommend.
 We call it 'expand then contract.'
 
 Instead of one breaking migration,
 we do it in stages across sprints:

 ─────────────────────────────────────
 SPRINT 47 (THIS SPRINT):
 ─────────────────────────────────────
 Step 1: Add the new column.
   ALTER TABLE invoice_line_items 
   ADD COLUMN notes TEXT;
   (nullable — backward compatible)
   
   The 'description' column stays.
   Both columns exist.
   Old code reads 'description'.
   New code writes to BOTH columns 
   during the transition period.

 Step 2: Deploy code that writes 
         to both 'description' 
         and 'notes' simultaneously.
   When a line item is created 
   or updated, write the value 
   to BOTH columns.
   When reading, prefer 'notes' 
   if it's populated, fall back 
   to 'description'.

 RESULT: Fully backward compatible.
   Rollback to old code still works 
   — old code reads 'description', 
   which is still there and still 
   being written.

 ─────────────────────────────────────
 SPRINT 48 (NEXT SPRINT):
 ─────────────────────────────────────
 Step 3: Migrate existing data.
   UPDATE invoice_line_items 
   SET notes = description 
   WHERE notes IS NULL;
   
   All historical records now have 
   'notes' populated.
   
 Step 4: Deploy code that reads 
         from 'notes' only.
   (Remove the fallback to 'description')

 ─────────────────────────────────────
 SPRINT 49 (TWO SPRINTS FROM NOW):
 ─────────────────────────────────────
 Step 5: Drop the old column.
   ALTER TABLE invoice_line_items 
   DROP COLUMN description;
   
   Now it's safe — all code 
   reads 'notes', no code reads 
   'description' anymore.

 ─────────────────────────────────────
 
 5 steps instead of 1.
 3 sprints instead of 1.
 But each step is safely reversible.
 
 And — importantly — the 'category_code' 
 field Priya mentioned can go into 
 Sprint 47 as a new nullable column.
 That's always safe to add.
 No migration risk at all."
```

The room was quiet for a few seconds. Elena spoke first:

```
Elena:
───────
"This is the right approach.
 We've done this before for 
 the expense_date column rename 
 two years ago.
 5 steps, 3 sprints.
 Zero production issues.
 
 The one-step rename would have 
 been a production risk."
```

Arjun:

```
Arjun:
───────
"Agreed. The expand-then-contract 
 pattern is the only safe way 
 to rename a column in a live system.
 
 I'd add: for Step 3 (the data 
 migration), if invoice_line_items 
 has a large number of rows, 
 we should batch the UPDATE 
 to avoid locking the table.
 
 How many rows are we talking about?"
```

You didn't know offhand. You checked quickly:

```sql
-- Quick check in staging DB
SELECT COUNT(*) FROM invoice_line_items;
-- Result: 847,293 rows
```

```
You:
─────
"847,000 rows.
 We should batch the UPDATE 
 in chunks — maybe 10,000 rows 
 per batch — to avoid a long 
 table lock during the migration."
```

Arjun: "Exactly. Add that to the plan."

Priya:

```
Priya:
───────
"Three sprints feels like a long time.
 Can we do Sprint 47 and Sprint 48 
 in the same sprint by treating 
 them as one deployment?"
```

```
You:
─────
"Steps 1 and 2 — yes, same sprint.
 They go together.
 
 Step 3 (data migration) also 
 in Sprint 47 if we're disciplined 
 about the deploy order:
   1. Add column (migration runs)
   2. Deploy app that writes both columns
   3. Data migration for existing rows
   4. Deploy app that reads 'notes' only
 
 The key is that step 4 must 
 happen AFTER step 3.
 We can't read from 'notes' only 
 if some rows still have null there.
 
 So Sprint 47 can do steps 1-4 
 if we carefully sequence the 
 deploys within the sprint.
 
 Sprint 48 or 49 would drop 
 the old column.
 
 Two sprints total, not three."
```

Priya: "That works."

Lukas: "Good. Estimating this."

---

## After the Meeting — What Elena Said

After sprint planning ended, Elena sent you a Slack message:

```
Elena (DM):
────────────
"Good catch in planning.
 
 Priya's original ask would have 
 been a production incident 
 waiting to happen.
 
 The expand-then-contract explanation 
 was clear — non-technical people 
 in the room understood the risk 
 without needing a technical 
 education on database internals.
 
 That's the skill: translating 
 technical risk into business terms.
 'If we rename the column and need 
 to rollback, every invoice endpoint 
 returns 500 errors' is something 
 a PM understands.
 'This violates backward migration 
 compatibility' is not."
```

```
You read this carefully.

Elena wasn't just saying 
"good job catching the issue."
She was naming something specific —
the translation from technical 
to business terms.

You had said "every invoice-related 
endpoint returns 500 errors."
Not "it would break the migration 
compatibility contract."

That framing was deliberate.
You had thought, in the moment, 
"how do I make Priya understand 
why this matters?"

Elena noticed.
```

---

## The Implementation — What You Actually Built

You owned the implementation across the two sprints.

### Sprint 47, Part 1 — The Schema Migration

```sql
-- V18__add_notes_column_to_invoice_line_items.sql
-- 
-- Part of the expand-then-contract rename 
-- of invoice_line_items.description → notes.
--
-- See ADR discussion in sprint 47 planning.
-- Related: INV-318 (rename), 
--          INV-319 (category_code)
--
-- STEP 1 of 4:
-- Add 'notes' column (nullable — backward compatible).
-- 'description' column remains unchanged.
-- Both columns will coexist during the transition period.
-- 'description' will be dropped in a future migration
-- after all reads have migrated to 'notes'.

ALTER TABLE invoice_line_items
ADD COLUMN notes TEXT;

-- ALSO: Add category_code (new feature, always safe)
-- category_code references a new reference table.
-- Nullable initially — existing line items 
-- won't have a category until retroactively 
-- assigned or updated.

ALTER TABLE invoice_line_items
ADD COLUMN category_code VARCHAR(50);

ALTER TABLE invoice_line_items
ADD CONSTRAINT fk_line_item_category
    FOREIGN KEY (category_code)
    REFERENCES line_item_categories(code)
    ON DELETE RESTRICT;

-- Index for category_code lookups
-- (finance dashboard filters by category)
CREATE INDEX idx_line_items_category_code
    ON invoice_line_items(category_code)
    WHERE category_code IS NOT NULL;
```

```
Why nullable category_code?
────────────────────────────
847,000 existing line items have 
no category_code.
If we made it NOT NULL immediately,
the migration would require providing 
a default value for all existing rows.
What default? We don't know yet —
finance teams will categorize 
their own historical line items 
over time.

Nullable is the correct choice 
for a new field on existing data.
We can add a NOT NULL constraint 
later once all rows have been 
populated — that's a future 
expand-then-contract step if needed.
```

### Sprint 47, Part 2 — The Application Code (Write Both)

```java
// InvoiceLineItem entity — 
// reads and writes BOTH columns
@Entity
@Table(name = "invoice_line_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceLineItem {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    // Original column — still written during 
    // transition. Will be dropped in Sprint 49.
    // @Deprecated annotation signals to developers
    // that this column is being phased out.
    @Deprecated
    @Column(name = "description")
    private String description;

    // New column — the rename target.
    // Currently nullable (some historical rows 
    // may not have this populated until 
    // the data migration runs in step 3).
    @Column(name = "notes")
    private String notes;

    // New field — category classification
    @Column(name = "category_code")
    private String categoryCode;

    @Column(name = "quantity", nullable = false)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "vat_rate", nullable = false)
    private BigDecimal vatRate;

    @Column(name = "vat_amount", nullable = false)
    private BigDecimal vatAmount;

    @Column(name = "line_total", nullable = false)
    private BigDecimal lineTotal;

    @Column(name = "gl_code")
    private String glCode;
}
```

```java
// InvoiceLineItemMapper — 
// reads from 'notes' with fallback to 'description'
// Writes to BOTH during transition
@Component
public class InvoiceLineItemMapper {

    /**
     * Maps API request to entity.
     * During transition: writes the 'notes' 
     * value to BOTH 'notes' and 'description' 
     * columns for backward compatibility.
     *
     * When 'description' column is dropped 
     * (Sprint 49), remove the setDescription() call.
     */
    public InvoiceLineItem toEntity(
            CreateLineItemRequest request,
            UUID invoiceId,
            int lineNumber) {

        // The API field is called 'notes' 
        // (new name going forward)
        String notesValue = request.getNotes();

        return InvoiceLineItem.builder()
            .invoiceId(invoiceId)
            .lineNumber(lineNumber)
            .notes(notesValue)
            .description(notesValue)   // ← write both
            // Remove this line in Sprint 49
            // when description column is dropped
            .categoryCode(request.getCategoryCode())
            .quantity(request.getQuantity())
            .unitPrice(request.getUnitPrice())
            .vatRate(request.getVatRate())
            .vatAmount(request.getVatAmount())
            .lineTotal(request.getLineTotal())
            .glCode(request.getGlCode())
            .build();
    }

    /**
     * Maps entity to API response.
     * During transition: prefer 'notes' column.
     * Fall back to 'description' for rows 
     * that haven't been migrated yet 
     * (historical records created before 
     * the data migration in step 3).
     */
    public InvoiceLineItemResponse toResponse(
            InvoiceLineItem entity) {

        // Prefer 'notes' — fall back to 'description'
        String notesValue = entity.getNotes() != null
            ? entity.getNotes()
            : entity.getDescription();

        return InvoiceLineItemResponse.builder()
            .id(entity.getId())
            .lineNumber(entity.getLineNumber())
            .notes(notesValue)          // API returns 'notes'
            .categoryCode(entity.getCategoryCode())
            .quantity(entity.getQuantity())
            .unitPrice(entity.getUnitPrice())
            .vatRate(entity.getVatRate())
            .vatAmount(entity.getVatAmount())
            .lineTotal(entity.getLineTotal())
            .glCode(entity.getGlCode())
            .build();
    }
}
```

```java
// API DTO — uses 'notes' (new name)
@Data
public class CreateLineItemRequest {

    @NotBlank(message = "Notes are required")
    @Size(max = 500,
        message = "Notes cannot exceed 500 characters")
    private String notes;   // ← was 'description'

    @Size(max = 50,
        message = "Category code cannot exceed 50 characters")
    private String categoryCode;  // ← new field, optional

    @NotNull
    @Positive
    private BigDecimal quantity;

    @NotNull
    @Positive
    private BigDecimal unitPrice;

    @NotNull
    @PositiveOrZero
    private BigDecimal vatRate;

    @NotNull
    @PositiveOrZero
    private BigDecimal vatAmount;

    @NotNull
    @Positive
    private BigDecimal lineTotal;

    private String glCode;
}
```

```
Important: API field renamed from 
'description' to 'notes'.

This IS a breaking API change 
for existing clients.

How did you handle this?
────────────────────────────────
Two things.

First, you checked with Sophie 
who owned the frontend:
"Is there any mobile app or 
web app currently reading the 
'description' field from line items?"

Sophie: "Yes — the invoice detail 
        view reads 'description'.
        We'll need to deploy the 
        frontend update at the same 
        time or accept a brief window 
        where the field shows empty."

You coordinated with Sophie's team 
to deploy the frontend update 
simultaneously with the backend.
This is a minor coordination effort — 
both teams deploy in the same 
release window.

Second, for the API contract formally —
you added both field names to the 
response DTO for one sprint using 
@JsonProperty and deprecation:

// Response DTO during transition:
@Schema(description = "Line item notes (formerly 'description')")
private String notes;

@Schema(deprecated = true,
    description = "Deprecated: use 'notes' instead")
@JsonProperty("description")
@Deprecated
private String descriptionLegacy;
// Returns same value as 'notes' for 
// backward compat with any external 
// API clients during transition

This was removed in Sprint 48 
after confirming no external 
clients were using the old field name.
```

### Sprint 47, Part 3 — The Data Migration

This was the trickiest part. 847,000 rows. You couldn't run a single UPDATE that touched all of them — it would lock the table for too long.

You wrote the migration as a batched script:

```sql
-- V19__migrate_description_to_notes.sql
-- flyway:executeInTransaction=false
--
-- STEP 3 of 4:
-- Migrate existing data from 'description' to 'notes'.
-- Runs in batches of 10,000 rows to avoid 
-- long table locks.
--
-- Why non-transactional:
-- Batched updates across multiple transactions
-- cannot be wrapped in a single Flyway transaction.
-- Each batch commits independently.
-- If migration fails midway, already-migrated
-- rows retain their 'notes' value.
-- Re-running the migration is safe because 
-- WHERE notes IS NULL filters out already-migrated rows.

DO $$
DECLARE
    batch_size INTEGER := 10000;
    rows_updated INTEGER;
BEGIN
    LOOP
        UPDATE invoice_line_items
        SET notes = description
        WHERE notes IS NULL
          AND description IS NOT NULL
          AND id IN (
              SELECT id 
              FROM invoice_line_items
              WHERE notes IS NULL
                AND description IS NOT NULL
              LIMIT batch_size
              FOR UPDATE SKIP LOCKED
          );

        GET DIAGNOSTICS rows_updated = ROW_COUNT;

        -- If no rows updated, we're done
        EXIT WHEN rows_updated = 0;

        -- Commit this batch
        COMMIT;

        -- Brief pause between batches
        -- Reduces pressure on DB during migration
        PERFORM pg_sleep(0.1);
    END LOOP;
END $$;
```

```
Key details in the batched migration:
───────────────────────────────────────

FOR UPDATE SKIP LOCKED:
  When the batch grabs rows to update,
  it skips any rows currently locked 
  by other transactions.
  This prevents the migration from 
  blocking application writes to 
  the same rows.
  Application continues operating 
  normally while migration runs in 
  the background.

WHERE notes IS NULL:
  Idempotent — if migration is interrupted 
  and re-run, already-migrated rows 
  (where notes is already populated) 
  are skipped automatically.
  No double-writes.

COMMIT after each batch:
  Each batch of 10,000 rows is 
  committed independently.
  No single long-running transaction.
  If something fails at row 500,000,
  rows 1-499,999 are already committed 
  and won't need to be re-migrated.

pg_sleep(0.1):
  100ms pause between batches.
  At 10,000 rows per batch and 
  ~847 batches needed, total 
  migration time: ~85 seconds plus 
  processing time.
  The pause prevents the migration 
  from saturating the DB during 
  business hours.
```

You ran a timing test on staging before proposing production:

```
Staging test (4,000 rows):
  Total time: 0.8 seconds
  
Estimated production time (847,000 rows):
  847 batches × (process time + 100ms pause)
  Estimated: 2-3 minutes

Acceptable — runs during a maintenance 
window or off-hours deploy.
```

Arjun reviewed the migration script:

```
Arjun's comment:
─────────────────
"The SKIP LOCKED approach is correct.
 It prevents migration from 
 blocking application writes 
 and vice versa.
 
 One thing to add: monitor the 
 migration progress via a Datadog 
 metric or at minimum a DB query 
 you can run to check how many 
 rows remain.
 
 If the migration takes longer 
 than expected in production, 
 you want to know how far it's 
 gotten before deciding to 
 intervene."
```

You added a monitoring query to the runbook:

```sql
-- Runbook query: check migration progress
-- Run this during migration to monitor progress

SELECT 
    COUNT(*) FILTER (WHERE notes IS NOT NULL) 
        AS migrated_rows,
    COUNT(*) FILTER (WHERE notes IS NULL) 
        AS remaining_rows,
    COUNT(*) AS total_rows,
    ROUND(
        COUNT(*) FILTER (WHERE notes IS NOT NULL) * 100.0 
        / COUNT(*), 
        1
    ) AS percent_complete
FROM invoice_line_items;
```

### Sprint 47, Part 4 — Read from 'notes' Only

After the data migration confirmed 100% completion (0 rows with `notes IS NULL`), you deployed the final code change for this sprint:

```java
// Updated mapper — no more fallback to 'description'
// (all rows now have 'notes' populated)
public InvoiceLineItemResponse toResponse(
        InvoiceLineItem entity) {

    // Transition period over — read from 'notes' only
    // All rows migrated in V19 migration
    return InvoiceLineItemResponse.builder()
        .id(entity.getId())
        .lineNumber(entity.getLineNumber())
        .notes(entity.getNotes())    // no fallback needed
        .categoryCode(entity.getCategoryCode())
        // ... other fields ...
        .build();
}
```

```java
// Also removed the legacy 'description' from response:
// No more @JsonProperty("description") in DTO.
// Frontend was already updated to read 'notes'.
```

### Sprint 49 — Dropping the Old Column

Two sprints later, the final step:

```sql
-- V22__drop_description_column_from_invoice_line_items.sql
--
-- STEP 4 (final) of the description → notes rename.
-- Safe to run because:
--   - All reads use 'notes' column (since Sprint 47 step 4)
--   - All writes use 'notes' column (since Sprint 47 step 2)
--   - All historical data migrated (Sprint 47 step 3)
--   - No application code references 'description' column
--     (verified by grep across codebase)
--
-- See: INV-318, ADR discussion in planning,
--      postmortem from original proposal review

ALTER TABLE invoice_line_items
DROP COLUMN description;
```

```
The grep verification you ran 
before writing this migration:
─────────────────────────────────
grep -r "\.description" \
  --include="*.java" \
  src/main/java/com/moss/invoice/

-- Result: 0 matches
-- No Java code references .description 
-- on invoice line item objects anymore.

grep -r "\"description\"" \
  --include="*.java" \
  src/main/java/com/moss/invoice/

-- Result: 0 matches  
-- No Java code uses "description" as 
-- a JSON or query field name in this service.

Safe to drop.
```

---

## The Result

```
What shipped:
──────────────
Sprint 47:
  - V18: notes + category_code columns added
  - Code writes to both columns
  - Code reads from 'notes' with fallback
  - Data migration (847,000 rows, 
    ~2.5 minutes, no table lock)
  - Code reads from 'notes' only
  - Frontend updated simultaneously

Sprint 49:
  - V22: 'description' column dropped
  - Clean schema, no dead columns

Zero production incidents 
across the 3-sprint migration.
No rollback needed at any step.

Finance teams got:
  - 'notes' field (same data, 
    better name from their perspective)
  - 'category_code' field for 
    invoice line item classification
  - Dashboard filters by category_code
    (within sprint 47 scope)
```

```
What you learned:
──────────────────

1. "Expand then contract" is a 
   concrete pattern, not a vague 
   principle.
   
   Add column → Write both → 
   Migrate data → Read new only → 
   Drop old.
   
   5 steps. Always in this order.
   Never skip steps.

2. SKIP LOCKED is the key to 
   safe concurrent migrations.
   The migration and the application 
   run simultaneously without blocking 
   each other.

3. Batched migrations with progress 
   monitoring are production-safe.
   Single large UPDATEs are not.

4. Translate technical risk into 
   business terms in the meeting.
   "Every invoice endpoint returns 
   500 errors if we rollback" 
   is a sentence a PM understands.

5. The moment to raise a technical 
   concern is BEFORE estimation, 
   not after tickets are in the sprint.
   Raising it after means the team 
   has already planned around it.
   Raising it before means the plan 
   can change.

What Lukas said in the next retro:
────────────────────────────────────
"The migration approach this sprint 
 is a good example of what we mean 
 by technical ownership.
 
 [Your name] caught a risk in 
 sprint planning, proposed a safer 
 alternative, coordinated with Sophie's 
 team for the API change, and 
 implemented all 4 migration steps 
 across the sprints.
 
 Not just flagging a problem.
 Driving the solution."
```

---

## The Broader Lesson — What This Story Was Really About

```
Story 7 (month 6):
  PM asked for bulk approval 
  with unclear spec.
  You raised edge cases BEFORE 
  the ticket was estimated.
  Lukas said: "this is L2 behavior."

Story 17 (month 14):
  PM asked for a column rename 
  that would have been a 
  production incident.
  You caught it in the meeting.
  You proposed a safe alternative.
  You owned the implementation.

Eight months apart.
Both about raising concerns 
before the decision was made.

But the nature of the contribution 
was different.

Story 7: you raised a question.
  "What happens in the multi-level 
   approval case?"
  You identified ambiguity.

Story 17: you proposed a solution.
  "Here's the 5-step approach 
   that gets the same outcome safely."
  You resolved the risk, not just 
  named it.

That's the difference between 
month 6 and month 14.
Not the instinct to raise concerns —
you had that in month 6.
The ability to arrive at the concern 
with a solution already formed.

In month 6, you wrote the edge 
case document after the meeting.
In month 14, you proposed the 
approach IN the meeting.

The speed came from depth.
By month 14, you had seen enough 
migrations to know the pattern 
before anyone asked.
```

---

## The "Tricky Question" Preparation

---

**Q1: "What is a backward-compatible database migration and why does it matter?"**

```
A backward-compatible migration is one 
that can run safely while the previous 
version of the application code 
is still running.

This matters because in any deployment,
there's always a window where the 
database migration has run but 
the new application code hasn't 
deployed yet.

During that window, the previous version 
of the code is still serving requests.
If the migration breaks the schema 
in a way the previous code doesn't understand,
requests start failing.

Example of a non-backward-compatible migration:
  RENAME COLUMN description TO notes
  
  Previous code reads 'description'.
  After the migration, 'description' doesn't exist.
  Previous code fails on every query.

Example of a backward-compatible migration:
  ADD COLUMN notes TEXT
  
  Previous code doesn't read 'notes'.
  Previous code still reads 'description'.
  'description' still exists.
  Previous code works exactly as before.

The rule we follow: every migration 
must be safe to run while the 
previous version of the code is active.
This means you can always rollback 
the application code without needing 
to reverse the database migration.

If you need to roll back and your 
migration is backward compatible:
  Redeploy the previous code version.
  Database schema still works for it.
  Done.

If your migration is NOT backward compatible:
  You'd need to reverse the migration manually,
  which is risky and slow.
  This is a production incident.
```

---

**Q2: "Explain the expand-then-contract pattern for renaming a column."**

```
The expand-then-contract pattern is 
the safe way to rename a database column 
in a live production system.

The name comes from what you do to 
the schema: expand it first 
(add the new column), then contract it 
(drop the old column) after the 
transition is complete.

For renaming 'description' to 'notes':

Step 1 — EXPAND:
  Add 'notes' column (nullable).
  Schema now has BOTH columns.
  Old column unchanged.

Step 2 — WRITE TO BOTH:
  Deploy code that writes to 
  BOTH 'description' and 'notes'.
  Reads prefer 'notes', fall back 
  to 'description'.
  
  Rollback safe: previous code 
  reads 'description', which is 
  still being written.

Step 3 — MIGRATE EXISTING DATA:
  UPDATE invoice_line_items 
  SET notes = description 
  WHERE notes IS NULL
  (batched to avoid table lock)

Step 4 — READ NEW ONLY:
  Deploy code that reads 'notes' only.
  All rows have 'notes' populated.
  No fallback needed.

Step 5 — CONTRACT:
  DROP COLUMN description.
  Schema is clean.
  No dead columns.

Why 5 steps instead of 1?
Because at every step, rollback 
to the previous application version 
is safe.

After Step 1: rollback reads 'description' — still there.
After Step 2: rollback writes 'description' — still being written.
After Step 3: rollback reads 'description' — has all data.
After Step 4: rollback needs 'description' — still exists.
After Step 5: rollback needs 'description' — dropped. 
              But at this point you've verified 
              no code reads it, so rollback 
              to code that references 'description' 
              should not happen.

Step 5 is the only step that creates 
a rollback constraint — which is why 
you only run it after thoroughly verifying 
that no application code references 
the old column name.
```

---

**Q3: "Why did you use FOR UPDATE SKIP LOCKED in the data migration? What does that do?"**

```
The data migration runs in batches:
  UPDATE invoice_line_items 
  SET notes = description 
  WHERE notes IS NULL 
  AND id IN (
      SELECT id FROM invoice_line_items
      WHERE notes IS NULL
      LIMIT 10000
      FOR UPDATE SKIP LOCKED   ← this
  )

FOR UPDATE tells PostgreSQL:
"Lock the selected rows so I can 
update them safely."

SKIP LOCKED tells PostgreSQL:
"If a row is already locked by 
another transaction, skip it 
and grab the next available row."

Why this matters:
The migration runs while the application 
is serving live traffic.
Finance managers are creating and 
updating invoice line items 
as the migration runs.

Without SKIP LOCKED:
Migration tries to lock a row.
Application has that row locked 
for an in-progress update.
Migration WAITS for the application 
to release the lock.
This chains — migration holds locks 
that block the next application 
operation, which is waiting on 
the migration, which is waiting 
on the application.
Deadlock or severe slowdown.

With SKIP LOCKED:
Migration tries to lock a row.
Application has that row locked.
Migration skips that row and 
moves to the next unlocked one.
Both the migration and the application 
proceed independently without blocking.
The migration might miss some rows 
in a given batch — but they'll be 
picked up in the next batch cycle 
since WHERE notes IS NULL is re-evaluated 
each iteration.

The result: migration and production 
traffic coexist with no contention.
Zero additional latency for users.
Migration completes in 2.5 minutes.
```

---

**Q4: "You said 'translate technical risk into business terms.' What does that mean specifically?"**

```
When I explained the column rename risk 
in sprint planning, I had two ways 
to say it.

Technical version:
"Renaming a column is a non-backward-compatible 
migration. It would violate our migration 
compatibility contract and create a 
deployment dependency between the schema 
change and the code change."

That's accurate. Priya would have nodded 
politely and not really understood 
the severity.

Business version:
"If we rename the column and then need 
to rollback the application code for 
any reason — a bug in the new feature, 
a performance regression — every invoice 
endpoint would return 500 errors until 
we manually reversed the database change.
That's a production incident caused 
by our own rollback."

Now Priya understood. 500 errors on 
invoice endpoints during a payment run 
Thursday means finance managers can't 
approve invoices. That's real business impact.

The translation is: 
"What does this technical risk actually 
LOOK LIKE when it fails?"
Not "what is the technical violation?"

Elena pointed this out specifically 
in Slack after the meeting.
She said the framing — "every invoice 
endpoint returns 500 errors" — was 
what made the room understand the 
severity, not the words 
"backward compatibility."

I think the skill is asking yourself:
"If this goes wrong, what does a 
finance manager see on their screen?"
Then saying that in the meeting 
instead of the abstract principle.
```

---

**Q5: "The PM's original request was 'this sprint if possible.' You ended up taking two sprints. How did you handle that conversation?"**

```
I didn't frame it as "this will take 
two sprints instead of one."
I framed it as "here's how we do 
this safely."

In the meeting, I laid out the approach:
Sprint 47: steps 1-4 
  (add column, write both, migrate, read new)
Sprint 49: step 5 
  (drop old column)

Then I said: "Steps 1-4 can all 
happen in Sprint 47 if we sequence 
the deploys carefully within the sprint.
Step 5 is a single migration in 
a later sprint — maybe 30 minutes 
of engineering time."

Priya's reaction was: "That works."

She was reasonable. She wasn't trying 
to force a risky approach because 
she wanted it fast. She didn't know 
the rename was risky until I explained it.
Once she understood the risk, 
she didn't insist on the single-sprint approach.

The framing that helped:
"Two sprints instead of one" sounds 
like a delay.
"Sprint 47 delivers everything you need,
Sprint 49 is just cleanup" sounds 
like the plan is Sprint 47 
with a small follow-up.

Both are technically true.
But one sounds like a problem 
and the other sounds like an approach.

In retrospect, I think this is 
what Elena meant by 
"translating technical concerns 
into business terms" —
not just explaining the risk 
but also shaping the solution 
in a way the PM can work with.
```

---

Block 5, Story 17 complete.

```
What this story demonstrates:
───────────────────────────────

Technical:
  Backward-compatible migrations —
    what they are and why they matter.
  Expand-then-contract pattern —
    the 5 steps and what each 
    one protects.
  SKIP LOCKED in batched migrations —
    how to run migrations concurrently 
    with live traffic.
  Batched UPDATEs vs single large updates —
    why table lock duration matters.
  Non-transactional Flyway migrations —
    when and why (reinforced from 
    Story 9 where you first saw this).
  Idempotent migration design — 
    WHERE notes IS NULL means 
    safe to re-run if interrupted.

Behavioral:
  Raised concern before estimation 
    (same instinct as Story 7, 
    but with a solution ready).
  Translated technical risk into 
    business terms Priya could act on.
  Owned the full implementation 
    across two sprints.
  Coordinated with Sophie for 
    simultaneous frontend deploy.
  Verified safety before each step 
    (grep before drop, 
     progress monitoring query, 
     timing test on staging).

Growth marker:
  Story 7 (month 6):
    Raised edge cases in planning.
    PM thanked you. Lukas said 
    "L2 behavior."
    
  Story 17 (month 14):
    Raised risk in planning 
    WITH a solution already formed.
    Drove the implementation.
    Zero production incidents 
    across 3-sprint migration.
    Lukas said: "Not just flagging 
    a problem. Driving the solution."
    
  Eight months. Same instinct.
  Different depth.
```
