# Month 1: Orientation & First Contribution

---

## Theme: *Drinking from the firehose*

---

## Foundational Knowledge: What You Need to Understand Before This Month's Stories

Before we get into the stories, there are two concepts that come up in Month 1 that you need to understand clearly. If you skip this section, the stories will feel like they are just describing events. With this section, you will understand *why* each moment mattered.

---

### Concept 1: What a Code Review Actually Is (and Why It Feels Uncomfortable at First)

In your tutorial projects and personal projects, you wrote code and it either worked or it did not. Nobody else looked at it. There was no second opinion.

In a professional team, every single piece of code that goes into the production codebase is reviewed by at least one other engineer before it is merged. This is called a **code review** or a **PR review** (PR stands for Pull Request — the mechanism on GitHub where you propose your changes and others can comment on them before approving).

Here is why code review exists:

```
WHY CODE REVIEW EXISTS

1. Catch bugs before they reach production
   The reviewer might spot something the author missed.
   Two pairs of eyes are better than one.

2. Share knowledge across the team
   When you review someone's code, you learn how they think.
   When they review yours, they teach you things you did not know.

3. Maintain consistency
   A team of 6 engineers needs to write code that looks and behaves
   consistently. Code review enforces the team's standards.

4. Build shared ownership
   Code review means no one person "owns" a piece of code alone.
   The whole team is familiar with changes. This matters when
   the author is on leave and something breaks.
```

When you are new, code review feels uncomfortable. Someone more experienced is looking at your work and pointing out what is wrong with it. The natural reaction is defensiveness or embarrassment.

The mature way to receive code review — which you will learn in Month 1 — is to treat every comment as a free lesson. The reviewer is not judging you as a person. They are helping you write better software.

**What a PR comment looks like in practice:**

When Lucas reviews your PR, he does not just say "approve" or "reject." He leaves specific comments on specific lines of code. Each comment might be:

- A question: "why did you use `findFirst` here instead of `findUnique`?"
- A suggestion: "consider using `select` instead of `include` here — it reduces the payload size"
- A correction: "this is missing the `userId` filter, which means any user can query any account"
- An educational observation: "test names should describe the behaviour, not the implementation"

Your job when receiving these is to read each one carefully, understand it, implement the change if it makes sense, and ask a follow-up question if you do not understand.

---

### Concept 2: Prisma Migrations — What They Are and the Three Commands You Must Understand

When you built your own projects, you probably created database tables manually — either by writing SQL directly or by running some setup script once. In a production team, you cannot do that. Multiple engineers are working on the same codebase. The database schema changes over time. You need a reliable, repeatable, version-controlled way to evolve the database schema. That is what **database migrations** are.

**The simplest mental model:**

Think of migrations like a version history for your database schema. Every time the schema needs to change — a new table, a new column, a changed constraint — you create a migration file that describes exactly what needs to change. These files are committed to the Git repository. Every developer, every staging environment, and every production environment applies these migration files in order. Everyone ends up with the exact same database schema.

**How Prisma handles migrations:**

FinVerse uses Prisma as the ORM (the tool that connects NestJS code to PostgreSQL). Prisma has a migration system built in. Here is how it works:

```
PRISMA MIGRATION WORKFLOW

Step 1: You change the Prisma schema file (schema.prisma)
        For example, you add a new field to the SyncLog model:
        syncRetryCount Int @default(0)

Step 2: You run a command to generate the migration
        This command compares your current schema file
        against the last known migration state,
        computes the difference,
        and generates a SQL file that captures the change.

Step 3: Prisma applies the SQL file to your local database.

Step 4: You commit both the schema change AND the generated
        SQL migration file to Git.

Step 5: When other developers pull your changes,
        they run a command to apply the pending migration
        to their local database.

Step 6: When you deploy to production, the same migration
        file is applied to the production database.
```

**The three commands — and this is critical:**

There are three Prisma migration commands. Confusing them in production can cause serious problems. You confuse two of them in Month 1. Here is what each one does:

```
npx prisma migrate dev
────────────────────────────────────────────────
Used in: LOCAL DEVELOPMENT ONLY. Never in production.
What it does:
  - Compares your schema.prisma against the last migration state
  - Generates a new migration SQL file
  - Applies it to your local development database immediately
  - Also runs prisma generate (updates the Prisma client types)

When to use it:
  Whenever you change schema.prisma during development
  and want to apply the change to your local database.

npx prisma migrate deploy
────────────────────────────────────────────────
Used in: STAGING and PRODUCTION. This is the safe one.
What it does:
  - Does NOT generate new migrations
  - Reads the list of migration files already committed to Git
  - Applies only the ones that have not been applied yet
  - Does nothing if everything is already up to date

When to use it:
  In your CI/CD pipeline when deploying to staging or production.
  This is the command that GitHub Actions runs before starting
  the new application containers.

npx prisma migrate reset
────────────────────────────────────────────────
Used in: LOCAL DEVELOPMENT ONLY. NEVER in production.
What it does:
  WARNING: This is the dangerous one.
  - Drops your entire database
  - Recreates it from scratch
  - Replays ALL migration files from the beginning
  - You lose all your local data

When to use it:
  Only when your local database is in a broken state
  and you want to start completely fresh.
  Never run this when connected to staging or production.
```

You will run `migrate reset` when you meant to run `migrate dev` in Month 1. It is one of the most common mistakes new engineers make with Prisma. The good news: on your local machine, the only thing you lose is your local test data. The bad news: if you somehow ran it against a production database, you would lose everything. This is why the connection string for production is a secret stored in AWS Secrets Manager and never visible to engineers directly.

---

## The Stories

---

### Story 1: The First Code Review

**Background:**

It is your first week. Lucas has assigned you your first real ticket: a bug where a transaction is showing up in the wrong spending category. A rule in the `MerchantRule` table has a regular expression (a pattern-matching string) that is too broad — it is matching merchant names it should not match.

For example: the rule meant to match `NETFLIX` is accidentally also matching `NETFLORIST` because the regex is written as `NET.*` which means "anything starting with NET." You need to narrow it.

The fix is four lines. But the process of fixing it and getting it through review teaches you more than the fix itself.

---

**S — Situation:**

It is week 2 of your contract. You have just fixed the merchant categorisation regex bug — four lines of code change, one updated unit test. You have opened your first Pull Request on GitHub and requested Lucas's review. This is the first time a professional engineer will formally evaluate your code. You are nervous. You are convinced the code is fine.

---

**T — Task:**

Get the PR approved and merged. In the process, learn how the team's code review culture works.

---

**A — Action:**

Lucas reviews the PR within a few hours. He approves the functional fix — the bug is indeed corrected. But he leaves six comments. Here is what each one was and what you learned from it:

**Comment 1 — Test naming:**

Your test was named `it('should fix the regex')`. Lucas's comment: "test names should describe the expected behaviour from a user's perspective, not describe the implementation. Something like: `it('should not categorise NETFLORIST as Entertainment')`."

*What you learned:* A good test name tells you what broke when the test fails. "should fix the regex" tells you nothing about what the system is supposed to do. "should not categorise NETFLORIST as Entertainment" tells you exactly what guarantee the test is making.

**Comment 2 — `findFirst` vs `findUnique`:**

In the test setup you used `prisma.merchantRule.findFirst({ where: { pattern: '...' } })` to fetch a rule you had just created. Lucas's comment: "use `findUnique` when you are fetching by a field that is guaranteed to be unique (like `id` or a `@unique` field). Use `findFirst` when the field is not unique and you just want the first match. Using `findFirst` on a unique field works but implies to the reader that duplicates might exist."

*What you learned:* The choice between `findFirst` and `findUnique` is not just functional — it communicates intent to the reader of the code.

**Comment 3 — PR description missing reproduction steps:**

Your PR description said: "Fixed the regex in MerchantRule." Lucas's comment: "PR descriptions should include: what changed, why it changed, and how to verify it locally. Add reproduction steps — what merchant name was being miscategorised, what category it incorrectly got, what it should get."

*What you learned:* A PR description is documentation. Six months from now, when someone is trying to understand why a regex was changed, your PR description is all they have. Make it useful.

**Comment 4 — Missing edge case in test:**

You tested that `NETFLORIST` no longer matched the `NETFLIX` rule. Lucas's comment: "also add a test confirming that `NETFLIX` still matches correctly — verify that fixing the over-match did not break the intended match."

*What you learned:* When fixing a bug, test both the bug fix AND that you did not break the original intended behaviour. Both directions matter.

**Comment 5 — Unused import:**

You had imported a Prisma type that you ended up not using in the test file. Lucas's comment: "remove unused imports — they add noise and suggest the code was written without care."

*What you learned:* Clean code is a signal. Small things like unused imports tell reviewers whether the author was paying attention.

**Comment 6 — Positive observation:**

Lucas also left one positive comment: "the regex fix itself is correct and clean — good instinct to use a word boundary anchor instead of just narrowing the wildcard."

*What you learned:* Good reviewers balance critical observations with genuine positive feedback. This also taught you what "good instinct" looked like — so you could repeat it.

---

**R — Result:**

You addressed all six comments, pushed the updated code, and Lucas approved on the second pass. The PR merged on day 10 of your contract.

More importantly: you now had a mental checklist from a single PR review that shaped every PR you wrote afterwards:

- Test names describe expected behaviour, not implementation
- `findFirst` vs `findUnique` — choose the one that communicates the right intent
- PR descriptions include what, why, and how to verify
- Test both the fix and that the original behaviour still works
- Remove unused imports
- Read your own code as if you are the reviewer — would you approve it?

---

### Story 2: The Prisma Migration Mistake

**Background:**

Tomasz asks you to add a new field to the `SyncLog` model — a `transactionsDuplicate` integer field that tracks how many transactions were skipped during a sync because they already existed in the database (duplicates coming from GoCardless returning overlapping date windows). This is a small, self-contained task. It introduces you to Prisma migrations for the first time in a real codebase.

---

**S — Situation:**

It is week 3. You have been assigned to add `transactionsDuplicate Int @default(0)` to the `SyncLog` Prisma model. You make the change in `schema.prisma`. Now you need to apply it to your local database. You look at the Prisma documentation and see two commands that look similar: `migrate dev` and `migrate reset`. You are not sure which one to use. You pick the wrong one.

---

**T — Task:**

Add the new field to the Prisma schema, generate the migration, apply it to your local database, and open a PR.

---

**A — Action:**

You edit `schema.prisma` and add the field:

```
model SyncLog {
  id                   String     @id @default(uuid())
  bankAccountId        String
  syncType             String
  status               SyncStatus
  transactionsFetched  Int        @default(0)
  transactionsInserted Int        @default(0)
  transactionsDuplicate Int       @default(0)   // ← the new field
  errorMessage         String?
  startedAt            DateTime   @default(now())
  completedAt          DateTime?
}
```

You then run `npx prisma migrate reset` instead of `npx prisma migrate dev`.

Your terminal shows:

```
? Are you sure you want to reset your database? All data will be lost. › (y/N)
```

You type `y` without fully reading the warning.

Your local database is wiped. All your local test data — the bank accounts, transactions, and categories you had seeded for development — is gone. The database is recreated from scratch by replaying all migration files from the beginning.

You stare at the terminal for a moment. Then you Slack Tomasz: "I think I made a mistake. I ran `migrate reset` instead of `migrate dev`. My local database got wiped."

Tomasz responds within two minutes: "ah, classic first-time mistake. Don't worry — on local it just means your test data is gone. Your actual work is fine. Run `migrate dev` now to generate the migration properly, then re-seed your local data. I'll send you the seed command."

He walks you through:

```bash
# Step 1: Generate the migration properly
npx prisma migrate dev --name add_transactions_duplicate_to_sync_log

# Step 2: Re-seed your local database with test data
npm run db:seed
```

You run both commands. Your local database is back. The migration file is generated correctly. You open the PR.

Tomasz reviews the PR and approves it. One comment: "going forward — `migrate dev` for local development, `migrate deploy` for staging and production, `migrate reset` only when your local DB is completely broken. Keep that list somewhere you can see it."

You write it in your personal Notion page under "Things I learned the hard way."

---

**R — Result:**

The PR merged with the correct migration file. No production impact — you were only on your local machine. Tomasz did not make you feel foolish about it. He framed it as a rite of passage.

What you took away:

- Three migration commands, three different purposes, never confuse them again
- When you make a mistake, say so immediately and clearly. "I think I made a mistake" is always better than trying to quietly fix it without telling anyone
- Tomasz's calm response taught you something about team culture — mistakes on local are fine, mistakes in production are serious, but blaming people for either helps nobody

---

## What Month 1 Taught You Overall

By the end of October — your third month — you had:

- Merged your first PR in a production codebase
- Experienced your first real code review and internalized six concrete lessons from it
- Made your first real mistake (the migration reset) and handled it correctly
- Started building a personal "lessons learned" document that you would add to every month

The most important shift in Month 1 was not technical. It was learning how to receive feedback without flinching, and how to ask for help without pretending you already knew the answer.

Lucas said something in your first 1:1 that stuck with you: "the engineers who grow fastest here are not the ones who make the fewest mistakes. They are the ones who make mistakes openly, fix them quickly, and never make the same one twice."

---