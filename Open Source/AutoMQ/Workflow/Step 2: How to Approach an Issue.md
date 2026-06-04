# Step 2: How to Approach an Issue

This is the thinking process that happens **between** seeing an issue and writing the first line of code. Most beginners skip this entirely and pay for it later with rejected PRs or wasted effort.

---

## 2.1 The Wrong Way vs The Right Way

```
WRONG WAY (what most beginners do):
─────────────────────────────────────────────────────────────
See issue → immediately start coding → submit PR →
maintainer says "this isn't what we wanted" →
you've wasted days of work

RIGHT WAY:
─────────────────────────────────────────────────────────────
See issue → understand it deeply → confirm approach →
THEN code → submit PR →
maintainer says "LGTM, merging" ✅

The difference is entirely in the preparation phase.
It takes 1-2 hours extra but saves days of rework.
```

---

## 2.2 The 5-Layer Issue Reading Framework

Every time you look at an issue, go through these 5 layers in order. Don't skip any.

```
┌─────────────────────────────────────────────────────────────┐
│                5-LAYER ISSUE READING                        │
│                                                             │
│  Layer 1: STATUS CHECK                                      │
│  Is this issue still valid and unowned?                     │
│                                                             │
│  Layer 2: PROBLEM UNDERSTANDING                             │
│  What exactly is broken or missing?                         │
│                                                             │
│  Layer 3: CODEBASE ARCHAEOLOGY                              │
│  Where does the relevant code live?                         │
│                                                             │
│  Layer 4: SOLUTION DESIGN                                   │
│  How should it be fixed? What are the tradeoffs?            │
│                                                             │
│  Layer 5: RISK ASSESSMENT                                   │
│  What could go wrong? What edge cases exist?                │
└─────────────────────────────────────────────────────────────┘
```

Let's go through each layer in detail.

---

## 2.3 Layer 1: Status Check

Before reading a single line of the issue body, answer these questions:

```
QUESTION 1: Is it still open?
──────────────────────────────
  → Look at the issue state: Open / Closed
  → If Closed: read WHY it was closed
    - "Fixed in PR #XXX" → already done, move on
    - "Won't fix" → not wanted, move on
    - "Duplicate of #YYY" → read that one instead

QUESTION 2: Is anyone assigned?
─────────────────────────────────
  → GitHub shows "Assignees" on the right sidebar
  → If assigned to someone: check if they're active
    - Look at their last comment date
    - If no activity in 3+ weeks, it may be abandoned
    - You can ask: "Is this still being worked on?"

QUESTION 3: Are there open PRs for it?
────────────────────────────────────────
  → Check if any PR mentions this issue number
  → Search: "1244" in the PRs tab
  → If an open PR exists: don't duplicate it
    - You could review it instead
    - Or pick a different issue

QUESTION 4: Did maintainers add important context in comments?
───────────────────────────────────────────────────────────────
  → Read EVERY comment, not just the issue body
  → Maintainers often add:
    - "Actually, do it this way instead..."
    - "This is blocked on X being merged first"
    - "The approach should use Y class, not Z"
  → Missing this = building the wrong thing
```

---

## 2.4 Layer 2: Problem Understanding

Now read the issue body carefully. Your goal is to answer:

```
3 Questions to answer from the issue body:
────────────────────────────────────────────────────────────

Q1: What is the CURRENT behavior?
  (what happens today that is wrong or missing)

Q2: What is the DESIRED behavior?
  (what should happen after the fix)

Q3: WHY does this matter?
  (what user problem does it solve)


Let's apply this to your 4 issues right now:

───────────────────────────────────────────────────────────
Issue #1244 — --broker param
───────────────────────────────────────────────────────────
Current:  kafka-producer-perf-test.sh sends messages to
          ALL brokers round-robin, no way to target specific ones

Desired:  Add --broker 1,3 param → messages only go to
          partitions whose leader is broker 1 or 3

Why:      To create hotspots and demo AutoMQ's
          partition self-balancing feature


───────────────────────────────────────────────────────────
Issue #666 — JMX metrics
───────────────────────────────────────────────────────────
Current:  JMX metrics don't work properly in AutoMQ's
          modified broker

Desired:  JMX metrics exposed correctly so operators
          can monitor AutoMQ the standard Kafka way

Why:      Teams migrating from Kafka to AutoMQ use
          existing JMX-based monitoring dashboards.
          Breaking JMX breaks their observability.


───────────────────────────────────────────────────────────
Issue #835 — OTel logs to server.log
───────────────────────────────────────────────────────────
Current:  OpenTelemetry SDK internal logs go to their own
          output, separate from Kafka's server.log

Desired:  OTel SDK logs routed INTO server.log via SLF4J
          bridge, all logs in one place

Why:      Operators watch server.log. Separate OTel logs
          get missed. Errors go unnoticed.


───────────────────────────────────────────────────────────
Issue #1842 — Metadata cleanup on topic deletion
───────────────────────────────────────────────────────────
Current:  When a topic is deleted, ElasticLog.destroy()
          deletes the streams but leaves behind the
          Partition→MetaStream KV mapping in the KV store

Desired:  destroy() should also delete the KV entry

Why:      Stale metadata accumulates in long-running
          clusters with frequent topic creation/deletion.
          Memory leak + correctness issue.
```

---

## 2.5 Layer 3: Codebase Archaeology

This is the most time-consuming layer. You need to find the code BEFORE designing a solution. Don't design in a vacuum.

```
Your archaeology toolkit:
────────────────────────────────────────────────────────────

TOOL 1: GitHub's search (fastest for finding files)
  → Go to the repo on GitHub
  → Press "T" to open file search
  → Type: "ProducerPerformance" → finds the file instantly
  → Type: "ElasticLog" → finds ElasticLog.scala instantly

TOOL 2: GitHub's code search (finding where things are called)
  → Press "/" on the repo page
  → Search: "BlockWALService" → shows all usages
  → Search: "destroy" in ElasticLog.scala → finds the method

TOOL 3: git log (understanding history of a file)
  git log --oneline tools/src/main/java/org/apache/kafka/tools/ProducerPerformance.java
  → Shows all past changes to this file
  → Helps understand why things are the way they are

TOOL 4: git blame (who wrote what line and when)
  git blame core/src/main/scala/kafka/log/streamaspect/ElasticLog.scala
  → Shows the author and commit for every line
  → Find the person who wrote the relevant code
  → That person is often a good reviewer for your PR

TOOL 5: grep (finding all related references)
  grep -r "ProducerPerformance" --include="*.java" .
  → Find everywhere this class is used or tested
```

### Applying Archaeology to Issue #1244:

```
What to look for in ProducerPerformance.java:
──────────────────────────────────────────────

Step 1: Find how existing args are added
  Search: "addArgument" in ProducerPerformance.java
  → You'll find the pattern for --topic, --num-records etc.
  → Your --broker follows the exact same pattern

Step 2: Find where the producer is created
  Search: "createKafkaProducer" 
  → This is where you inject the custom partitioner

Step 3: Find existing tests
  Search: ProducerPerformanceTest.java
  → Read existing tests to understand the test pattern
  → Your tests should follow the same structure

Step 4: Find if Partitioner interface is already used anywhere
  grep -r "implements Partitioner" --include="*.java" .
  → See examples of custom partitioners in the codebase
  → Model yours after them
```

---

## 2.6 Layer 4: Solution Design

Now that you understand the problem and the codebase, design the solution. **Write this down before coding.** This is what you'll put in your issue comment to get maintainer approval.

```
SOLUTION DESIGN TEMPLATE:
────────────────────────────────────────────────────────────

1. APPROACH: What is your high-level approach?
   (1-3 sentences)

2. FILES CHANGED: Which files will you modify?
   (be specific - file names and why)

3. NEW CODE: What new classes/methods will you add?
   (brief description, not full code)

4. TESTS: What will you test?
   (specific scenarios)

5. ALTERNATIVES CONSIDERED: What else could work?
   Why did you reject those?


Applied to Issue #1244:
────────────────────────────────────────────────────────────

APPROACH:
  Add a --broker CLI parameter to ProducerPerformance.java.
  When specified, inject a custom BrokerBoundPartitioner
  that fetches cluster metadata and routes messages only
  to partitions whose leader is in the specified broker list.

FILES CHANGED:
  tools/src/main/java/org/apache/kafka/tools/
    ProducerPerformance.java       ← add arg + partitioner class
  tools/src/test/java/org/apache/kafka/tools/
    ProducerPerformanceTest.java   ← add tests

NEW CODE:
  → --broker argument in argParser()
  → BrokerBoundPartitioner static inner class
    implementing org.apache.kafka.clients.producer.Partitioner
  → Partitioner injection logic in start()

TESTS:
  → Messages go only to eligible partitions when --broker used
  → Correct error when specified broker IDs don't exist
  → Normal behavior unchanged when --broker not specified

ALTERNATIVES CONSIDERED:
  → Using a custom ProducerInterceptor instead of Partitioner
    Rejected: Interceptors can't control partition routing.
    Partitioner is the correct hook for this.
  → Adding broker filtering at the topic creation level
    Rejected: Out of scope and modifies topic behavior,
    not just the perf tool.
```

---

## 2.7 Layer 5: Risk Assessment

This is what separates a good PR from a great PR. Think about what can go wrong.

```
EDGE CASES TO THINK ABOUT:
────────────────────────────────────────────────────────────

For Issue #1244:

Risk 1: Specified broker doesn't exist in cluster
  → eligiblePartitions will be empty
  → producer.send() will fail with confusing error
  → Your fix: throw a clear RuntimeException with
    helpful message: "No partitions found on brokers: [1,3].
    Available brokers: [1,2,4]"

Risk 2: Specified broker exists but has no partition leadership
  → Same as above — empty eligible list
  → Same fix

Risk 3: Cluster metadata not yet available when partitioner runs
  → cluster.partitionsForTopic() might return empty initially
  → Fix: don't cache eligiblePartitions on first empty result,
    only cache when non-empty

Risk 4: --broker used with --topic that has only 1 partition
  → Only one eligible partition — that's fine, it still works
  → No special handling needed, just document it

Risk 5: --broker not specified — existing behavior must be unchanged
  → Only inject partitioner if brokerIds != null
  → Test this explicitly


For Issue #1842 (to think about for later):

Risk 1: What if destroy() fails halfway through?
  → Streams deleted but KV entry not yet deleted
  → Or KV entry deleted but stream deletion failed
  → Need to think about atomicity / ordering

Risk 2: Is the KV key format consistent?
  → Before writing delete code, confirm the exact
    key format used when it was written
  → Wrong key = silent failure (no error, just doesn't delete)
```

---

## 2.8 The Issue Comment — What to Post

After going through all 5 layers, this is the comment you post on GitHub:

```markdown
Hi team! I'd like to work on this issue.

**My understanding of the problem:**
`kafka-producer-perf-test.sh` currently sends messages to 
all brokers via round-robin partitioning. There's no way to 
restrict messages to partitions on specific brokers, making 
it impossible to create controlled hotspots to trigger 
AutoMQ's partition self-balancing feature.

**My proposed approach:**
1. Add a `--broker <id1,id2,...>` parameter to 
   `ProducerPerformance.java`'s argument parser
2. When specified, inject a `BrokerBoundPartitioner` that 
   queries cluster metadata and routes messages only to 
   partitions whose leader matches the specified broker IDs
3. Add unit tests for normal case, missing broker, and 
   fallback when `--broker` is not specified

**Files I plan to modify:**
- `tools/src/main/java/org/apache/kafka/tools/ProducerPerformance.java`
- `tools/src/test/java/org/apache/kafka/tools/ProducerPerformanceTest.java`

Please let me know if this approach aligns with your 
expectations or if there's anything I should do differently 
before I start coding. Thank you!
```

**Why this comment works:**
```
✅ Shows you READ the issue carefully
✅ Shows you FOUND the relevant code already
✅ Shows you have a CONCRETE plan, not vague ideas
✅ Asks for feedback BEFORE wasting time on wrong approach
✅ Professional and respectful tone
✅ Short enough that maintainers will actually read it
```

---

## 2.9 Reading the Maintainer's Response

After you post, the maintainer may respond in different ways:

```
RESPONSE TYPE 1: "Looks good, go ahead!"
─────────────────────────────────────────
  → Start coding immediately
  → You're on the right track


RESPONSE TYPE 2: "Good idea, but do it this way instead..."
────────────────────────────────────────────────────────────
  → They've given you a better approach
  → Update your design before coding
  → Reply: "Thanks! I'll go with that approach."


RESPONSE TYPE 3: No response for 5+ days
──────────────────────────────────────────
  → Ping once: "Just checking in — is it okay to proceed
    with the approach described above?"
  → If still no response after 3 more days:
    → Either proceed with your approach (low risk for clear issues)
    → Or pick a different issue


RESPONSE TYPE 4: "This is already being worked on"
────────────────────────────────────────────────────
  → Thank them and move to the next issue
  → Don't take it personally — it happens
```

---

## 2.10 Putting It All Together — Your Per-Issue Checklist

Print this out. Use it for every single issue you work on:

```
┌────────────────────────────────────────────────────────────┐
│           PRE-CODING CHECKLIST (use for every issue)       │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  LAYER 1 — STATUS                                          │
│  □ Issue is still Open                                     │
│  □ No one is currently assigned                            │
│  □ No open PR already exists for it                        │
│  □ Read ALL comments, not just the issue body              │
│                                                            │
│  LAYER 2 — UNDERSTANDING                                   │
│  □ I can state the current behavior                        │
│  □ I can state the desired behavior                        │
│  □ I understand WHY this matters                           │
│                                                            │
│  LAYER 3 — CODEBASE                                        │
│  □ Found the exact file(s) to change                       │
│  □ Read the relevant code (not just the method, the file)  │
│  □ Found the existing tests for this area                  │
│  □ Used git log/blame to understand history                │
│                                                            │
│  LAYER 4 — DESIGN                                          │
│  □ Written down approach, files, new code, tests           │
│  □ Considered at least one alternative                     │
│                                                            │
│  LAYER 5 — RISK                                            │
│  □ Listed at least 3 edge cases                            │
│  □ Have a plan for each edge case                          │
│                                                            │
│  CLAIM                                                     │
│  □ Posted comment on GitHub issue                          │
│  □ Got acknowledgment from maintainer                      │
│  □ Created branch: feature/issue-XXXX-description          │
│                                                            │
│  NOW YOU CAN START CODING ✅                                │
└────────────────────────────────────────────────────────────┘
```

---

## Step 2 Summary

```
┌────────────────────────────────────────────────────────────┐
│         HOW TO APPROACH AN ISSUE — KEY PRINCIPLES          │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  Never code before understanding the full context          │
│                                                            │
│  5 layers: Status → Problem → Codebase →                   │
│            Design → Risk                                   │
│                                                            │
│  Always comment on the issue and get a green light         │
│  before writing code — saves days of wasted effort         │
│                                                            │
│  Your issue comment = proof you've done your homework      │
│                                                            │
│  Edge cases in your design = what separates beginners      │
│  from contributors maintainers trust                       │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

---

## What's Next

**Step 3: Writing Tests in AutoMQ Style** — understanding how Kafka/AutoMQ tests are structured, what coverage is expected, and how to write tests that reviewers won't ask you to redo.

Ready? Say **"next"**!