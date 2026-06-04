# Step 1: Git & Open Source Contribution Workflow

This is not just Git commands. This is the **entire professional process** of how open source contribution works — from first seeing an issue to getting your PR merged.

---

## 1.1 The Big Picture — Full Lifecycle of One Contribution

```
┌─────────────────────────────────────────────────────────────┐
│           LIFECYCLE OF ONE OPEN SOURCE CONTRIBUTION         │
│                                                             │
│  PHASE 1: PREPARE                                           │
│  ─────────────────                                          │
│  Fork repo → Clone locally → Set up build → Read codebase   │
│                                                             │
│  PHASE 2: CLAIM                                             │
│  ────────────────                                           │
│  Comment on issue → Wait for acknowledgment                 │
│                                                             │
│  PHASE 3: BUILD                                             │
│  ────────────────                                           │
│  Create branch → Write code → Write tests → Test locally    │
│                                                             │
│  PHASE 4: SUBMIT                                            │
│  ────────────────                                           │
│  Push branch → Open PR → Write PR description               │
│                                                             │
│  PHASE 5: ITERATE                                           │
│  ────────────────                                           │
│  Respond to reviews → Fix → Push again                      │
│                                                             │
│  PHASE 6: MERGED ✅                                          │
│  ──────────────────                                         │
│  Maintainer merges → Your code is in production!            │
└─────────────────────────────────────────────────────────────┘
```

Each phase has specific rules. Let's go through them one by one.

---

## 1.2 Phase 1: Prepare — Fork, Clone, Remote Setup

This is a one-time setup per repo.

```
STEP 1: Fork the repo on GitHub
─────────────────────────────────────────────────────────────
Go to: https://github.com/AutoMQ/automq
Click the "Fork" button (top right)

Result:
  Original repo:  github.com/AutoMQ/automq        ← "upstream"
  Your fork:      github.com/prasenjit/automq      ← "origin"

Why fork?
  You don't have write access to AutoMQ's repo.
  A fork is YOUR copy where you can push freely.
  PRs go FROM your fork TO the original.


STEP 2: Clone YOUR fork locally
─────────────────────────────────────────────────────────────
  git clone https://github.com/prasenjit/automq.git
  cd automq


STEP 3: Add the original repo as "upstream"
─────────────────────────────────────────────────────────────
  git remote add upstream https://github.com/AutoMQ/automq.git

Verify:
  git remote -v

  Output:
  origin    https://github.com/prasenjit/automq.git   (fetch)
  origin    https://github.com/prasenjit/automq.git   (push)
  upstream  https://github.com/AutoMQ/automq.git      (fetch)
  upstream  https://github.com/AutoMQ/automq.git      (push)

Why two remotes?
  origin   = your fork     → you push TO this
  upstream = AutoMQ's repo → you pull FROM this (to stay updated)
```

This remote setup is the foundation of everything. Never skip it.

---

## 1.3 Understanding the Two Remotes Visually

```
                    GITHUB (cloud)
┌──────────────────────────────────────────────────────────┐
│                                                          │
│  AutoMQ/automq (upstream)      prasenjit/automq (origin) │
│  ──────────────────────────    ────────────────────────  │
│  main branch                   main branch               │
│  (official, protected)         (your fork)               │
│                                                          │
│         You NEVER push          You push freely here     │
│         directly here           and open PRs from here   │
└──────────────────────┬───────────────────────────────────┘
                       │                    ▲
              pull to stay                  │ push your
              up to date                    │ changes
                       │                    │
                       ▼                    │
              ┌─────────────────────────────┴──┐
              │     YOUR LOCAL MACHINE         │
              │                                │
              │  git clone (one time)          │
              │  git fetch upstream (regular)  │
              │  git push origin (your work)   │
              └────────────────────────────────┘
```

---

## 1.4 Phase 2: Claim — Commenting on the Issue

This step is **critical** and most beginners skip it. If you just submit a PR without claiming the issue first, two things can happen:

```
What can go wrong without claiming first:
───────────────────────────────────────────────────────────

Problem 1: Someone else is already working on it
  → You both submit PRs
  → Maintainer picks one → yours is wasted effort
  → Very demoralizing

Problem 2: The issue is outdated
  → Maintainer might say "we already solved this internally"
  → Or "the requirements changed, don't do it this way"
  → Again, wasted effort

The right process:
───────────────────────────────────────────────────────────

1. Go to the issue page
2. Read ALL comments top to bottom
   → Is anyone already assigned?
   → Did maintainer add more context in comments?
   → Did they say "won't fix" or "blocked on X"?

3. Post a comment like this:

   ─────────────────────────────────────────────────────
   Hi, I'd like to work on this issue.

   My understanding of the problem:
   [your understanding in 2-3 lines]

   My proposed approach:
   [1-2 sentences on how you plan to fix it]

   Please let me know if this aligns with expectations
   before I start coding.
   ─────────────────────────────────────────────────────

4. Wait for maintainer response (usually 1-3 days)
5. Only start coding after you get a green light
```

This one habit separates contributors who get PRs merged from those who don't.

---

## 1.5 Phase 3: Build — Branching Strategy

Never work on the `main` branch. Ever. Here's why and how:

```
WRONG WAY (don't do this):
───────────────────────────
  git checkout main
  # make changes directly on main
  git push origin main
  # open PR from main

  Problem: If your PR is rejected or needs big changes,
  your main branch is now "dirty" and out of sync.
  Getting back to a clean state is painful.


RIGHT WAY:
───────────────────────────
  # First, sync your local main with upstream
  git checkout main
  git fetch upstream
  git merge upstream/main

  # Then create a dedicated branch for this issue
  git checkout -b feature/issue-1244-broker-param

  # All your work goes here
  # main stays clean always
```

### Branch Naming Convention

```
Pattern:  <type>/<issue-number>-<short-description>

Examples:
  feature/issue-1244-broker-param
  fix/issue-1842-metadata-cleanup-on-delete
  enhancement/issue-666-jmx-metrics
  fix/issue-835-otel-logs-to-server-log

Why this naming?
  → Anyone reading the branch knows what it does
  → Links back to the issue number
  → "feature" vs "fix" signals the type of change
```

---

## 1.6 The Daily Development Loop

Once your branch is created, this is the rhythm:

```
Daily loop while working on your contribution:
────────────────────────────────────────────────────────────

Morning (sync with upstream — do this every single day):
  git fetch upstream
  git rebase upstream/main
  
  Why rebase and not merge?
  → Rebase puts your commits ON TOP of upstream changes
  → Keeps your branch history clean and linear
  → Maintainers prefer clean linear history in PRs

During the day (commit often, commit small):
  # Make a small focused change
  git add <specific files>      ← never use "git add ."
  git commit -m "your message"

  # Push to your fork regularly (backup + visible progress)
  git push origin feature/issue-1244-broker-param

Never:
  git add .                     ← adds unintended files
  git commit -m "fix"           ← useless commit message
  git commit -m "wip"           ← don't push WIP commits to PR
```

---

## 1.7 Writing Good Commit Messages

This is taken seriously in open source. A bad commit message is a red flag to maintainers.

```
FORMAT:
────────────────────────────────────────────────────────────

<type>(<scope>): <short summary in present tense>

<blank line>

<body: what and WHY, not how — optional but good practice>

<blank line>

Resolves: #<issue-number>


TYPES:
  feat     → new feature
  fix      → bug fix
  test     → adding or fixing tests
  docs     → documentation only
  refactor → code change without behavior change
  chore    → build scripts, configs


REAL EXAMPLES for your issues:

✅ Good:
  feat(tools): add --broker parameter to ProducerPerformance

  Add a new --broker <id1,id2,...> CLI parameter to
  kafka-producer-perf-test.sh that restricts message
  sending to partitions on the specified brokers.

  This creates partition hotspots which trigger AutoMQ's
  partition self-balancing feature for demonstration.

  Resolves: #1244

✅ Good:
  fix(elastic-log): delete KV metadata entry on topic deletion

  ElasticLog.destroy() was deleting stream data but leaving
  behind the Partition→MetaStream KV mapping, causing stale
  metadata to accumulate after repeated topic creation and
  deletion.

  Resolves: #1842

❌ Bad:
  fix bug

❌ Bad:
  WIP: working on broker param stuff

❌ Bad:
  added feature for issue 1244 and also fixed some tests
  (one commit should do ONE thing)
```

---

## 1.8 Phase 4: Submit — Opening the PR

When your code and tests are ready:

```
STEP 1: Final rebase before opening PR
────────────────────────────────────────
  git fetch upstream
  git rebase upstream/main
  git push origin feature/issue-1244-broker-param

  # If rebase causes conflicts:
  # Fix the conflict in the file
  # git add <conflicted file>
  # git rebase --continue


STEP 2: Open the PR on GitHub
────────────────────────────────────────
  Go to: github.com/prasenjit/automq
  GitHub will show a banner: "Compare & Pull Request"
  Click it
  Target: AutoMQ/automq → main branch


STEP 3: Write the PR description
────────────────────────────────────────
  This is as important as the code itself.
```

### PR Description Template (use this for all 4 issues):

```markdown
## Summary
Brief 2-3 sentence description of what this PR does.

## Problem
What was broken or missing? Link to the issue.
Closes #1244

## Solution
How did you solve it? 2-4 sentences.
Why this approach over alternatives?

## Changes
- Added `--broker` parameter to `argParser()` in ProducerPerformance.java
- Implemented `BrokerBoundPartitioner` inner class
- Added unit tests in ProducerPerformanceTest.java

## Testing
How did you test this?
- Unit tests: `./gradlew :tools:test`
- Manual test: ran against local AutoMQ + MinIO cluster
  with `--broker 1` and verified messages only went to
  partitions on broker 1

## Screenshots / Logs (if applicable)
[paste terminal output showing it works]
```

---

## 1.9 Phase 5: Iterate — Handling Review Comments

This is where most beginners get anxious. Here's the reality:

```
What review comments look like:
────────────────────────────────────────────────────────────

Maintainer: "Can you add a check for when --broker specifies
             a broker ID that doesn't exist in the cluster?"

Maintainer: "Nit: variable name should be targetBrokerIds
             not brokerIds for clarity"

Maintainer: "Please add a test for the edge case where
             all partitions are on the excluded broker"


How to respond:
────────────────────────────────────────────────────────────

Rule 1: Respond to EVERY comment, even if just "Done ✅"
Rule 2: If you disagree, explain WHY politely
Rule 3: Never argue — they know the codebase better than you
Rule 4: Group all fixes into new commits on same branch
Rule 5: Never force-push during review (it disrupts review)


After making changes:
  git add <files>
  git commit -m "fix: address review comments"
  git push origin feature/issue-1244-broker-param

  # GitHub PR updates automatically — no need to close/reopen
  # Reply to each comment thread: "Done in latest commit ✅"


When reviewers say "LGTM" (Looks Good To Me):
  → Maintainer will merge it
  → You don't merge your own PR
  → Just wait
```

---

## 1.10 Keeping Your Fork Healthy Long-Term

```
After your PR is merged:
────────────────────────────────────────────────────────────

  # Sync your local main with upstream (your code is in there now!)
  git checkout main
  git fetch upstream
  git merge upstream/main
  git push origin main

  # Delete the feature branch (it's merged, no longer needed)
  git branch -d feature/issue-1244-broker-param
  git push origin --delete feature/issue-1244-broker-param

  # Start fresh for next issue
  git checkout -b feature/issue-666-jmx-metrics


Staying in sync BETWEEN issues (do weekly):
────────────────────────────────────────────────────────────

  git checkout main
  git fetch upstream
  git merge upstream/main
  git push origin main

  Why this matters:
  AutoMQ is actively developed. If you fall 2 weeks behind
  upstream, your next PR will have merge conflicts.
  5 minutes of syncing weekly saves hours of conflict fixing.
```

---

## 1.11 The Complete Command Reference Card

```
┌────────────────────────────────────────────────────────────┐
│              YOUR CHEAT SHEET                              │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  ONE-TIME SETUP                                            │
│  git clone https://github.com/prasenjit/automq.git         │
│  git remote add upstream https://github.com/AutoMQ/automq  │
│                                                            │
│  START NEW ISSUE                                           │
│  git checkout main                                         │
│  git fetch upstream                                        │
│  git merge upstream/main                                   │
│  git checkout -b feature/issue-XXXX-description            │
│                                                            │
│  DAILY WORK                                                │
│  git fetch upstream                                        │
│  git rebase upstream/main    ← every morning               │
│  git add <specific files>                                  │
│  git commit -m "feat(scope): description"                  │
│  git push origin <branch>                                  │
│                                                            │
│  BEFORE OPENING PR                                         │
│  git fetch upstream                                        │
│  git rebase upstream/main                                  │
│  git push origin <branch>                                  │
│                                                            │
│  AFTER PR MERGED                                           │
│  git checkout main                                         │
│  git fetch upstream                                        │
│  git merge upstream/main                                   │
│  git branch -d <your-branch>                               │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

---

## Step 1 Summary

```
┌────────────────────────────────────────────────────────────┐
│         OPEN SOURCE WORKFLOW — KEY PRINCIPLES              │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  1. Fork → clone → add upstream remote (one time)          │
│                                                            │
│  2. ALWAYS claim the issue first — comment before coding   │
│                                                            │
│  3. NEVER work on main — always a dedicated branch         │
│                                                            │
│  4. Rebase upstream/main every morning                     │
│                                                            │
│  5. Commit messages matter — use the format                │
│                                                            │
│  6. PR description is as important as the code             │
│                                                            │
│  7. Respond to every review comment — never ghost          │
│                                                            │
│  8. After merge: sync main, delete branch, start fresh     │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

---

## What's Next

**Step 2: How to Approach an Issue** — the process of reading an issue properly, confirming your understanding with maintainers, and planning the implementation before writing any code.

Ready? Say **"next"**!