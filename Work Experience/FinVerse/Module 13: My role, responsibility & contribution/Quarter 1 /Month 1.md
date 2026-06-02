# Quarter 1 — Month 1: First Days in a Real Codebase

---

## Foundational Knowledge: What You Need Before These Stories

Before the stories, two concepts are worth understanding clearly. Both come up directly in Month 1. Without this context, the stories will feel like a list of events. With it, you will understand why each moment mattered.

---

### Concept 1: What a Code Review Actually Is — And Why It Feels Uncomfortable at First

In your personal projects and tutorial builds, you wrote code and it either worked or it didn't. Nobody else looked at it. There was no second opinion. You were the only judge.

In a professional engineering team, that changes completely. Every single piece of code that goes into the production codebase — no matter how small — is reviewed by at least one other engineer before it is merged. This process is called a **code review** or a **PR review** (PR stands for Pull Request — the mechanism on GitHub where you propose your changes and others comment before approving).

Here is why it exists:

```
WHY CODE REVIEW EXISTS

1. Catch bugs before they reach production
   Two pairs of eyes catch things one pair misses.
   A reviewer might spot a security hole, a performance
   issue, or a logic error the author was too close to see.

2. Spread knowledge across the team
   When Lucas reviews your code, you learn how he thinks.
   When you read his review comments, you absorb patterns
   you would have taken months to discover on your own.

3. Maintain consistency
   6 engineers writing code independently will drift in style,
   naming conventions, and patterns. Reviews enforce a shared
   standard so the codebase stays readable for everyone.

4. Build shared ownership
   Reviewed code is never just "your" code. The reviewer
   understands what changed and why. When you are on leave
   and something breaks, someone else can investigate.
```

When you are new, code review feels uncomfortable. Someone experienced is formally evaluating your work and telling you what is wrong with it. The instinctive reaction is defensiveness — "I tested this, it works."

The mature response — which you learn in Month 1 — is to treat every review comment as a free lesson. The reviewer is not judging you as a person. They are helping you write better software. The engineers who grow fastest are the ones who treat review comments like gold, not criticism.

**What a PR comment looks like in practice:**

Lucas does not just write "approve" or "reject." He leaves specific comments on specific lines of code. Each comment might be:

- A question: *"Why did you use `findFirst` here instead of `findUnique`?"*
- A suggestion: *"Consider using `select` instead of `include` — it reduces the payload size"*
- A correction: *"This is missing the `userId` filter — any authenticated user can query any account"*
- An educational observation: *"Test names should describe the expected behaviour, not the implementation"*

Your job is to read each one carefully, understand it, implement the change, and ask a follow-up question if something is unclear.

---

### Concept 2: Prisma Migrations — The Three Commands You Must Never Confuse

When you built your portfolio projects, you probably created your database tables by writing SQL directly or running a setup script once. That works fine when you are the only person working on the project and the data does not matter.

In a production team, that approach breaks down immediately. Multiple engineers are touching the same codebase. The database schema evolves over time. You need a reliable, repeatable, version-controlled way to evolve that schema — so every developer's local environment, every staging environment, and production all stay in sync. That is what **database migrations** are.

**The simple mental model:**

Think of migrations as a version history for your database schema. Every time the schema needs to change — a new table, a new column, a modified constraint — you create a migration file that describes exactly what needs to change. These files live in the Git repository alongside your code. Every environment applies these migration files in order. Everyone ends up with the exact same schema.

**How Prisma handles this:**

FinVerse uses Prisma as the ORM (the tool that connects NestJS to PostgreSQL). Prisma has a migration system built in. Here is the workflow:

```
PRISMA MIGRATION WORKFLOW

Step 1: You edit schema.prisma
        For example, you add a new field to the SyncLog model:
        transactionsDuplicate Int @default(0)

Step 2: You run a command
        Prisma compares your current schema.prisma against
        the last known migration state, computes the difference,
        and generates a SQL file that captures the change.

Step 3: Prisma applies that SQL to your local database

Step 4: You commit both the schema change AND the generated
        SQL migration file to Git

Step 5: Other developers pull your changes and run a command
        to apply the pending migration to their local database

Step 6: When you deploy to staging or production, the same
        migration file is applied there automatically via CI/CD
```

**The three commands — and this is critical:**

```
npx prisma migrate dev
────────────────────────────────────────────────
Used in: LOCAL DEVELOPMENT ONLY. Never in production.

What it does:
  Compares your schema.prisma to the last migration state,
  generates a new SQL migration file, and applies it to
  your local database immediately.
  Also runs prisma generate to update the Prisma client types.

When to use:
  Whenever you change schema.prisma during development
  and want to see the change reflected in your local database.

──────────────────────────────────────────────────────────────

npx prisma migrate deploy
────────────────────────────────────────────────
Used in: STAGING and PRODUCTION. This is the safe one.

What it does:
  Does NOT generate new migrations.
  Reads the migration files already committed to Git,
  applies only the ones not yet applied to this database,
  and does nothing if everything is already up to date.

When to use:
  In your CI/CD pipeline when deploying to staging or production.
  This is what GitHub Actions runs before starting new containers.

──────────────────────────────────────────────────────────────

npx prisma migrate reset
────────────────────────────────────────────────
Used in: LOCAL DEVELOPMENT ONLY. NEVER in production.

What it does:
  WARNING — this is the dangerous one.
  Drops your entire database.
  Recreates it from scratch.
  Replays ALL migration files from the beginning.
  You lose all your local data.

When to use:
  Only when your local database is completely broken
  and you want a clean slate.
  Never run this when connected to staging or production.
```

You will confuse `migrate dev` and `migrate reset` in Month 1. It is one of the most common mistakes new engineers make with Prisma. On your local machine, the only consequence is losing your local test data — annoying but harmless. If you somehow ran it against a production database, you would lose everything. This is why production database credentials are stored as secrets in AWS Secrets Manager and are never visible to engineers directly.

---

## The Stories

---

### Story 1: The Codebase Is Nothing Like Your Projects

**Background:**

It is your first week. You have cloned the Core Product repository and run the setup command in the README. The Docker containers started — PostgreSQL, Redis, and the NestJS application — but you are staring at a folder structure with 40 files in it and you understand maybe a third of what you are looking at.

Your tutorial NestJS course had a project with 5 modules. This codebase has 9 modules, each with its own controllers, services, repositories, and DTOs. There is a BullMQ worker you have never heard of. There is a GoCardless integration you have never touched. The Prisma schema has 23 models. The domain language — PSD2, consent flows, MiFID II, requisitions, holdings — is a foreign language to you.

---

**S — Situation:**

It is day two. Lucas has scheduled a 2-hour onboarding call — a recorded walkthrough of the entire Core Product Service codebase. You join the call with a notebook open. He walks through the folder structure, the module boundaries, how BullMQ workers are registered, how RabbitMQ producers are wired up, why the modular monolith pattern was chosen. He speaks clearly and does not rush.

You understand about 40% of what he says. You write down the rest as questions for later.

After the call, he sends you a Notion document — "Week 1 Reading List" — with four items: the Architecture Decision Records explaining why key technical choices were made, the module ownership map, the team's PR checklist, and the coding conventions document. He says: "read all of this before picking up your first ticket. Questions are welcome."

---

**T — Task:**

Understand the codebase well enough to make your first real contribution without breaking something. Get oriented. Ask good questions.

---

**A — Action:**

You spend the first three days reading. Not writing code — reading.

You read the ADRs (Architecture Decision Records) in Notion. These are short documents that explain why specific decisions were made — why PostgreSQL over MongoDB, why a modular monolith instead of separate services, why Prisma instead of TypeORM. You understand maybe half of the reasoning at this stage, but the documents give you vocabulary. You know what the choices are, even if you do not yet fully understand why.

You read the module ownership map and start mentally connecting names to responsibilities. Lucas owns Users and Auth. Tomasz owns Transactions and Budgeting. You are about to take ownership of Accounts and Open Banking, but Elena is still covering it for now. Understanding who owns what means you know who to ask when you are confused about something specific.

You read the PR checklist. It is nine items long. Most of them are obvious in hindsight but you would have missed several of them without reading it explicitly: always include the `userId` filter on user-specific queries, always use `select` instead of `include` in mobile-facing endpoints, always write a test for the edge case not just the happy path. These feel abstract right now. They will become very concrete soon.

You ask Lucas three questions across Slack over the three days, formatted exactly as he suggested during the call: context first, what you have already tried to understand, and then the specific thing you are stuck on. He answers all three within the day. None of the answers are dismissive. Each one is a short explanation followed by "does that make sense? Ask again if not."

On day four, you pick up your first ticket.

---

**R — Result:**

By the end of week one, you have not written a single line of production code. But you have a working mental map of the system. You know the module structure. You know who owns what. You know the team's conventions. You know the vocabulary.

This investment pays off immediately. When your first ticket involves a model in the Transactions module, you already know it is Tomasz's domain and that you should run significant changes by him. When you write your first Prisma query, you already know to use `select` instead of `include`. You made fewer rookie mistakes in your first PR than you would have if you had jumped straight to coding on day one.

What you took from week one: reading a professional codebase is a skill in itself. It is not glamorous. It does not feel like progress. But it is the foundation everything else builds on.

---

### Story 2: The First Code Review — Six Comments That Taught More Than Weeks of Reading

**Background:**

Your first ticket is deliberately scoped to be small and self-contained. A transaction is occasionally showing in the wrong spending category in the budgeting dashboard. The cause is a rule in the `MerchantRule` table — a regex pattern that is too broad and is matching merchant names it should not.

The specific problem: the rule meant to categorise NETFLIX under Entertainment has the pattern `NET.*` — which means "anything starting with NET." This accidentally also matches NETFLORIST, a flower delivery service, which gets categorised as Entertainment instead of Shopping. The fix is simple: narrow the regex to `NETFLIX.*` or better yet, use a word boundary anchor so it only matches the exact word NETFLIX.

The fix itself is four lines of code. Getting it through review teaches you more than the fix.

---

**S — Situation:**

It is week two. You have understood the categorisation engine — how MerchantRule patterns are applied against transaction descriptions in order of priority, and how `isCategoryManual` protects user-overridden categories from being overwritten. The fix is clear to you. You write it, write a test, and open your first Pull Request.

You write a PR description: "Fixed the regex in MerchantRule." You request Lucas's review. You are quietly confident the code is fine.

---

**T — Task:**

Get the PR merged. Learn how the team's review culture actually works.

---

**A — Action:**

Lucas reviews within a few hours. He approves the functional fix — the bug is correctly resolved. But he leaves six comments. Not rejections. Educational observations. Here is what each one was and what you learned from it:

**Comment 1 — Test naming:**

Your test: `it('should fix the regex')`

Lucas's comment: *"Test names should describe the expected behaviour from a user's perspective, not describe the implementation. Something like: `it('should not categorise NETFLORIST as Entertainment')`"*

What you learned: A good test name tells you what broke when the test fails. "Should fix the regex" tells you nothing about what the system is supposed to do. "Should not categorise NETFLORIST as Entertainment" tells you exactly what guarantee is being made. If this test fails in six months, the name tells you immediately what is broken without reading the test body.

**Comment 2 — `findFirst` vs `findUnique`:**

In your test setup, you fetched the merchant rule you had just created using `prisma.merchantRule.findFirst({ where: { pattern: '...' } })`. Lucas's comment: *"Use `findUnique` when fetching by a field guaranteed to be unique. Use `findFirst` when the field is not unique and you just want the first match. Using `findFirst` on a unique field works — but it implies to the reader that duplicates might exist. Choose the one that communicates the right intent."*

What you learned: The choice between `findFirst` and `findUnique` is not just functional — it communicates intent. Code is read far more often than it is written. Every choice you make is a signal to the next person who reads it.

**Comment 3 — PR description missing context:**

Lucas's comment: *"PR descriptions should include: what changed, why it changed, and how to verify it locally. Add reproduction steps — what merchant name was being miscategorised, what category it incorrectly got, what it should get instead."*

What you learned: A PR description is documentation. Six months from now, when someone is trying to understand why a regex was changed, your PR description is all they have. "Fixed the regex" is useless. A proper description with reproduction steps is useful to anyone investigating a related issue in the future.

**Comment 4 — Missing edge case in test:**

You tested that NETFLORIST no longer matched the NETFLIX rule. Lucas's comment: *"Also add a test confirming NETFLIX still matches correctly. Verify that fixing the over-match did not break the intended match."*

What you learned: When fixing a bug, test both directions — the thing that was broken, and the thing that was working correctly. A fix that stops the wrong behaviour but also stops the right behaviour is not a fix.

**Comment 5 — Unused import:**

You had imported a Prisma type that you ended up not using. Lucas's comment: *"Remove unused imports — they add noise and suggest the code was written without careful attention."*

What you learned: Clean code is a signal. Small things like unused imports tell reviewers whether the author was paying attention to the whole file or just the lines they changed.

**Comment 6 — Positive observation:**

Lucas also left one positive comment: *"The regex fix itself is correct and clean — good instinct to use a word boundary anchor rather than just narrowing the wildcard."*

What you learned: Good reviewers balance critical observations with genuine positive feedback. This also told you what "good instinct" looked like in this context — so you could recognise and repeat it.

---

**R — Result:**

You addressed all six comments, pushed the updated code, and Lucas approved on the second pass. The PR merged on day ten of your contract.

More importantly, you now had a mental checklist drawn from a single review cycle:

- Test names describe expected behaviour, not implementation
- `findFirst` vs `findUnique` — choose the one that signals the right intent
- PR descriptions need: what changed, why, and how to verify
- Test both the fix and that the original behaviour still works
- Remove unused imports
- Read your own code as a reviewer would — would you approve it?

You wrote these six points in a personal Notion page you titled "Things I Learned." You added to it every week for the rest of the contract.

---

### Story 3: The Prisma Migration Mistake

**Background:**

Tomasz asks you to add a new field to the `SyncLog` model — a `transactionsDuplicate` integer field that tracks how many transactions were skipped during a sync because they already existed in the database. GoCardless sometimes returns overlapping date windows on consecutive syncs, and some transactions come back twice. The deduplication logic silently skips them, but there is currently no record of how many were skipped. Tomasz wants visibility.

This is your first schema change in a real codebase. It introduces you to Prisma migrations in practice — not from a tutorial, but from a real task with real consequences if you get it wrong.

---

**S — Situation:**

It is week three. You open `schema.prisma` and add the field:

```prisma
model SyncLog {
  id                    String     @id @default(uuid())
  bankAccountId         String
  syncType              String
  status                SyncStatus
  transactionsFetched   Int        @default(0)
  transactionsInserted  Int        @default(0)
  transactionsDuplicate Int        @default(0)   // ← new field
  errorMessage          String?
  startedAt             DateTime   @default(now())
  completedAt           DateTime?
}
```

Now you need to apply this change to your local database. You look at the Prisma documentation. You see three commands that sound similar. You are not sure which one applies here.

---

**T — Task:**

Apply the schema change to your local database, generate the migration file, and open a PR.

---

**A — Action:**

You read through the three commands quickly. `migrate dev` says "create and apply migrations." `migrate deploy` says "apply existing migrations." `migrate reset` says "reset the database."

You think: I want to reset the schema to include the new field. You run `migrate reset`.

Your terminal immediately shows:

```
? Are you sure you want to reset your database?
  All data will be lost. › (y/N)
```

You type `y` without fully registering the warning.

Your entire local database is wiped. Every bank account, transaction, budget, user, and category you had seeded for local development testing is gone. The database is recreated from scratch by replaying all migration files. The schema is correct now — but all your data is gone, and you never generated the new migration file for the field you added.

You stare at the terminal for a moment. Then you open Slack and message Tomasz directly:

*"I think I made a mistake. I ran `migrate reset` instead of `migrate dev`. My local database got wiped. I don't think I generated the migration properly either. What should I do?"*

Tomasz replies within two minutes:

*"Ah, classic first-time mistake. Don't worry — on local it just means your test data is gone. Nothing production-affecting. Here's what to do:*
*1. Run `npx prisma migrate dev --name add_transactions_duplicate_to_sync_log` — this generates the migration properly*
*2. Run `npm run db:seed` — this restores your local test data*
*Send me the PR when it's done."*

You follow both steps. The migration is generated correctly:

```sql
-- prisma/migrations/20231120_add_transactions_duplicate_to_sync_log/migration.sql
ALTER TABLE "banking"."sync_logs"
ADD COLUMN "transactionsDuplicate" INTEGER NOT NULL DEFAULT 0;
```

Your local database is back. You open the PR.

Tomasz reviews and approves it. One comment: *"Going forward — `migrate dev` for local development, `migrate deploy` for staging and production, `migrate reset` only when your local DB is completely broken. Keep that list somewhere."*

You add all three to your "Things I Learned" Notion page. With a note beside `migrate reset`: *"Never run this if you are not 100% sure which database you are connected to."*

---

**R — Result:**

The PR merged with the correct migration file. No production impact — you were on your local machine the entire time. Tomasz did not make you feel stupid about it. He treated it as a rite of passage.

Three things you took from this:

First — the technical lesson: three migration commands, three very different purposes, never confuse them again.

Second — the communication lesson: when you make a mistake, say so immediately and clearly. *"I think I made a mistake"* is always better than trying to quietly fix it without telling anyone. Tomasz knew exactly what had happened the moment you described it, and he had you back to a working state in ten minutes because you were direct.

Third — the culture lesson: Tomasz's calm, practical response taught you something about how this team operates. Mistakes on local are fine. Mistakes in production are serious. But blaming people for either one helps nobody. The question is always: what do you do next?

---

## What Month 1 Taught You Overall

By the end of your first month, you had merged two PRs, made one mistake that cost you an hour of local setup, and received a code review that taught you more in one afternoon than days of reading.

The most important shift was not technical. It was learning to receive feedback without flinching, and to ask for help without pretending you already knew the answer.

Lucas said something in your first 1:1 that you wrote down and kept:

*"The engineers who grow fastest here are not the ones who make the fewest mistakes. They are the ones who make mistakes openly, fix them quickly, and never make the same one twice."*

You did not fully understand what that meant in week one. By week four, you understood it completely.

---
