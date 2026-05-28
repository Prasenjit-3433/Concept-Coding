# Story 13: Cache Stampede — The First Time You Solved Something Without Asking for Help First

---

## Context — Why This Story Is Different From the Others

```
Every technical mistake in this arc 
so far followed the same pattern:

You made a mistake.
Someone else found it.
You fixed it with their guidance.

Story 4 (N+1):    Elena found it in PR.
                  Finn helped you debug.
Story 5 (approval concurrency): 
                  Elena found it in PR.
Story 6 (@Transactional private):
                  Elena found it in PR.

Each time, someone more experienced 
caught what you missed.

Story 13 is different.

You found it yourself.
In staging. Before anyone else saw it.
You researched the fix independently.
You implemented it.
You brought it to Elena 
already solved — not as a question,
but as a completed PR with an explanation.

That's a different thing entirely.
Not "I made a mistake and fixed it 
with help."
But "I found a production risk,
understood it, fixed it, 
and here's why."

That shift — from being caught 
to catching — is what month 10 
was actually about.
```

---

## The Situation

It was the Tuesday after Story 12's implementation went to production. The caching was working. Hit rate was sitting at 88%. You had shared the Monday morning numbers with the team. Everything looked good.

You had one more thing on your checklist — something you had written in your design doc but hadn't finished yet:

```
From your design doc:
──────────────────────
"Monitor hit rate in Datadog for 
 48 hours after deploy."

You had done that.

But you had also written, almost as 
an afterthought:

"Run a load test against staging 
 to verify behavior under concurrent load."

You hadn't done that yet.
It wasn't in the acceptance criteria 
of the ticket.
Nobody had asked for it.
But you had written it down 
because something in the back of 
your mind said: concurrent load is 
where caching goes wrong.

You had read about stampede during 
your preparation for the design meeting.
You had proposed jitter as the fix.
But jitter addresses the TTL expiry 
stampede — all keys expiring simultaneously.

There was a different scenario you 
hadn't fully thought through.
The scenario where a single key expires 
and multiple concurrent requests 
hit the cache miss at the same time.
```

You sat down on Tuesday afternoon and ran a manual load test against the staging environment.

---

## Discovering the Problem

You used a simple bash script to simulate concurrent expense submissions for the same company — the scenario where a large company has 50 employees all submitting expenses in the same minute on Monday morning:

```bash
# Simulating 50 concurrent submissions 
# for the same company
# All hitting the cache at the same time

for i in $(seq 1 50); do
  curl -s -X PUT \
    http://staging-api.moss.internal/api/v1/expenses/test-uuid-$i/submit \
    -H "X-User-Id: emp-uuid-$i" \
    -H "X-Company-Id: company-uuid-fixed" \
    -H "Authorization: Bearer $TEST_TOKEN" &
done
wait
echo "All requests sent"
```

You then watched Datadog in real time. Specifically:

```
Metrics you were watching:
───────────────────────────
approval_policy.cache.result (HIT/MISS/FALLBACK)
http.client.requests 
  filtered to: uri=/api/v1/companies/.../approval-policy
               service=user-org-service
```

What you saw was not what you expected:

```
Datadog output during load test:
──────────────────────────────────

Before requests start:
  Cache key exists for company-uuid-fixed

The test triggered a Redis TTL expiry first
(you deleted the key manually to simulate 
 cache miss for all 50 requests simultaneously)

Then sent 50 concurrent requests.

Result:
  approval_policy.cache.result:
    HIT:  18
    MISS: 32     ← 32 FeignClient calls, 
                    not 1

  http.client.requests to User & Org:
    32 calls within a 200ms window
    All for the same company
    All returning the same data
```

```
You stared at this for a moment.

32 FeignClient calls.
Not 1. Thirty-two.

All for the same company.
All for the same data that 
hadn't changed.
All because 50 requests hit 
the cache miss at the same time,
and all 32 that saw the miss
raced to call User & Org 
before any of them had stored 
the result back in Redis.

The other 18 got lucky — they 
arrived slightly after one of 
the 32 had already stored the result.

This is the cache stampede.
Not the TTL expiry version 
(which jitter fixes).
The concurrent miss version.
The one you hadn't fully thought through.
```

You wrote this in a Notion note immediately:

```
YOUR NOTES (Tuesday, staging):
────────────────────────────────
PROBLEM FOUND:
When cache key expires, all concurrent 
requests that arrive before any one of 
them repopulates the cache will ALL 
call FeignClient simultaneously.

With 50 concurrent submissions:
- Key expires
- 50 requests check Redis simultaneously
- 32 see MISS (before any stores result)
- 32 call User & Org simultaneously
- Eventually one stores result
- Remaining 18 see HIT

At production scale with a large company:
- Could be 100+ concurrent submissions
- 100+ simultaneous FeignClient calls 
  for same data
- User & Org Service spikes
- Worse than no caching for that moment

JITTER doesn't fix this.
Jitter spreads expiry times across 
different keys.
This is multiple requests hitting 
the SAME key expiry concurrently.
Different problem. Different fix needed.

POSSIBLE FIXES:
1. Mutex lock on cache miss
   - First request acquires a Redis lock
   - Other requests wait briefly
   - First request fetches and stores result
   - Others read from cache on retry
   
2. Probabilistic early expiration
   - Before key expires, start refreshing 
     with some probability
   - More complex to implement
   
3. Stale-while-revalidate
   - Serve stale data while refreshing 
     in background
   - Requires keeping stale data beyond TTL
   - More state to manage

Lean toward option 1 (mutex lock).
Simpler. Clear semantics.
Fits Cache-Aside pattern well.
```

You sat with this for 30 minutes. You read through how Redis locks work. You looked at the existing Redis operations in the codebase. You looked at how other teams had solved this pattern in distributed systems articles you found.

The mutex lock approach was clear:

```
WITH MUTEX LOCK:
─────────────────
50 concurrent requests hit cache miss.

Request 1: 
  Tries to acquire lock: 
  SET lock:approval_policy:company-uuid-fixed 1 
      NX EX 10
  (NX = only if not exists, EX = expire in 10s)
  
  Succeeds → acquires lock.
  Fetches from FeignClient.
  Stores result in Redis.
  Releases lock.

Requests 2-50:
  Try to acquire lock → FAIL (already held).
  Wait 50ms.
  Retry Redis read.
  Find the result (Request 1 stored it).
  Return cached result.
  
Result: 1 FeignClient call instead of 32.
```

You understood the solution. You were confident in it. But before implementing it, you did one thing — you verified your understanding was correct by reading the existing Redis template code to make sure `setIfAbsent` was available and worked the way you thought.

```java
// You checked this in the Redis docs and 
// found it was already in the codebase:
redisTemplate.opsForValue().setIfAbsent(
    lockKey, 
    "1", 
    Duration.ofSeconds(10)
);
// Returns: true if lock acquired, 
//          false if already existed
// This is what NX EX translates to 
// in Spring Redis
```

Good. You didn't need any new dependencies. The tool was already there.

You also thought about the failure modes:

```
FAILURE MODE 1: Lock holder crashes
──────────────────────────────────────
Request 1 acquires lock.
Request 1 crashes before storing result 
or releasing lock.

With EX 10 (10-second expiry):
Lock auto-expires after 10 seconds.
Next request can acquire lock.
Worst case: 10-second delay.
Acceptable for approval policy.

FAILURE MODE 2: Very slow FeignClient call
────────────────────────────────────────────
FeignClient call takes 8 seconds 
(network issue, User & Org slow).
Lock expires after 10 seconds.
Another request acquires lock and 
also calls FeignClient.
Two concurrent FeignClient calls.

Still better than 32.
And this scenario (8+ second FeignClient 
call) would trigger separate alerts anyway.
The FeignClient has a 5-second timeout — 
so this case would actually throw an 
exception, not delay 8 seconds.
Lock expiry is really the safety net 
for crashes, not slow calls.

FAILURE MODE 3: Redis down
────────────────────────────
setIfAbsent throws RedisConnectionFailureException.
Caught by existing fallback in getApprovalPolicy().
Falls through to FeignClient directly.
Same behavior as before the caching — 
graceful degradation.
```

You felt ready to implement. You had found the problem, understood it, reasoned through the solution, considered the failure modes, verified the tool was available. You hadn't asked anyone yet.

This was intentional. You wanted to see if you could solve it yourself first.

---

## The Implementation

You updated `ApprovalPolicyService` with the mutex lock pattern:

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalPolicyService {

    private final UserOrgFeignClient userOrgFeignClient;
    private final RedisTemplate<String, ApprovalPolicy> 
        redisTemplate;
    private final MeterRegistry meterRegistry;

    private static final String CACHE_KEY_PREFIX = 
        "approval_policy:";
    private static final String LOCK_KEY_PREFIX = 
        "lock:approval_policy:";
    private static final Duration BASE_TTL = 
        Duration.ofMinutes(15);
    private static final int JITTER_MAX_SECONDS = 300;
    private static final Duration LOCK_TTL = 
        Duration.ofSeconds(10);
    private static final long LOCK_WAIT_MS = 50;
    private static final int MAX_LOCK_RETRIES = 5;

    public ApprovalPolicy getApprovalPolicy(UUID companyId) {

        String cacheKey = CACHE_KEY_PREFIX + companyId;
        String lockKey  = LOCK_KEY_PREFIX + companyId;

        try {
            // Step 1: Try Redis — fast path
            ApprovalPolicy cached = redisTemplate
                .opsForValue()
                .get(cacheKey);

            if (cached != null) {
                recordCacheResult("HIT");
                return cached;
            }

            // Step 2: Cache MISS — try to acquire lock
            Boolean lockAcquired = redisTemplate
                .opsForValue()
                .setIfAbsent(lockKey, "1", LOCK_TTL);

            if (Boolean.TRUE.equals(lockAcquired)) {
                // WE hold the lock — fetch from upstream
                try {
                    ApprovalPolicy policy = 
                        fetchFromUpstream(companyId);

                    Duration ttl = getTTLWithJitter();
                    redisTemplate.opsForValue()
                        .set(cacheKey, policy, ttl);

                    recordCacheResult("MISS");
                    log.debug("Approval policy fetched " +
                        "and cached for company: {}. " +
                        "TTL: {}", companyId, ttl);

                    return policy;

                } finally {
                    // ALWAYS release the lock —
                    // even if FeignClient throws
                    redisTemplate.delete(lockKey);
                }

            } else {
                // Someone else holds the lock —
                // wait briefly and retry from cache
                return waitAndRetryFromCache(
                    companyId, cacheKey, lockKey);
            }

        } catch (RedisConnectionFailureException
                 | QueryTimeoutException e) {

            log.warn("Redis unavailable for approval " +
                "policy lookup. Falling back to " +
                "FeignClient. Company: {}", companyId, e);
            recordCacheResult("FALLBACK");
            return fetchFromUpstream(companyId);
        }
    }

    /**
     * Called when another request holds the mutex lock.
     * Waits briefly and retries reading from cache.
     * Falls back to direct FeignClient if retries 
     * are exhausted.
     *
     * Why retry from cache and not just call FeignClient?
     * Because calling FeignClient directly would defeat
     * the purpose of the lock — we'd still have N 
     * concurrent FeignClient calls. The lock holder 
     * is fetching the data; we should wait for it.
     */
    private ApprovalPolicy waitAndRetryFromCache(
            UUID companyId,
            String cacheKey,
            String lockKey) {

        for (int attempt = 0; 
                attempt < MAX_LOCK_RETRIES; 
                attempt++) {

            try {
                Thread.sleep(LOCK_WAIT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            // Try to read from cache — 
            // lock holder may have stored result
            ApprovalPolicy retried = redisTemplate
                .opsForValue()
                .get(cacheKey);

            if (retried != null) {
                recordCacheResult("HIT");
                log.debug("Approval policy retrieved " +
                    "from cache after lock wait. " +
                    "Company: {}, attempt: {}",
                    companyId, attempt + 1);
                return retried;
            }

            // Cache still empty — 
            // lock holder might still be fetching.
            // Try again.
        }

        // All retries exhausted.
        // Lock holder may have crashed.
        // Fall back to direct FeignClient call.
        log.warn("Lock wait exhausted for company: {}. " +
            "Falling back to direct FeignClient call.",
            companyId);
        recordCacheResult("MISS");
        return fetchFromUpstream(companyId);
    }

    private ApprovalPolicy fetchFromUpstream(UUID companyId) {
        return userOrgFeignClient.getApprovalPolicy(companyId);
    }

    public void invalidateApprovalPolicy(UUID companyId) {
        String cacheKey = CACHE_KEY_PREFIX + companyId;
        redisTemplate.delete(cacheKey);
    }

    private Duration getTTLWithJitter() {
        int jitter = ThreadLocalRandom.current()
            .nextInt(0, JITTER_MAX_SECONDS);
        return BASE_TTL.plusSeconds(jitter);
    }

    private void recordCacheResult(String result) {
        Counter.builder("approval_policy.cache.result")
            .tag("result", result)
            .register(meterRegistry)
            .increment();
    }
}
```

You updated the integration test to cover the concurrent scenario:

```java
@Test
void getApprovalPolicy_shouldMakeOneFeignClientCall_underConcurrentLoad()
        throws InterruptedException {

    UUID companyId = UUID.randomUUID();
    ApprovalPolicy policy = buildTestPolicy(companyId);

    // Simulate FeignClient taking 100ms
    // (gives time for concurrent requests to arrive)
    given(userOrgFeignClient.getApprovalPolicy(companyId))
        .willAnswer(invocation -> {
            Thread.sleep(100);
            return policy;
        });

    int concurrentRequests = 20;
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch finishLatch = 
        new CountDownLatch(concurrentRequests);
    List<ApprovalPolicy> results = 
        Collections.synchronizedList(new ArrayList<>());

    // Create 20 threads, all waiting to start at 
    // exactly the same time (simulate concurrent load)
    for (int i = 0; i < concurrentRequests; i++) {
        Thread thread = new Thread(() -> {
            try {
                startLatch.await(); // Wait for signal
                ApprovalPolicy result = 
                    approvalPolicyService
                        .getApprovalPolicy(companyId);
                results.add(result);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                finishLatch.countDown();
            }
        });
        thread.start();
    }

    // Release all threads simultaneously
    startLatch.countDown();

    // Wait for all to complete
    finishLatch.await(10, TimeUnit.SECONDS);

    // All 20 requests should have gotten a result
    assertThat(results).hasSize(concurrentRequests);
    assertThat(results).allMatch(
        r -> r.getCompanyId().equals(companyId));

    // FeignClient should have been called 
    // at most 2 times
    // (1 for the lock holder, possibly 1 
    //  for fallback if timing is tight)
    // Definitely NOT 20 times
    verify(userOrgFeignClient, 
        atMost(2)).getApprovalPolicy(companyId);
}
```

---

## Opening the PR — Without Being Asked

You ran all the tests. They passed. You wrote the PR description:

```
PR: EXP-276 — Fix concurrent cache miss stampede 
              in ApprovalPolicyService

WHAT:
──────
Found a cache stampede scenario during 
manual load testing of the caching 
implementation from last week (EXP-271).

WHAT I FOUND:
──────────────
When the approval policy cache key expires,
multiple concurrent requests hitting the 
cache miss simultaneously all call 
User & Org FeignClient before any of them 
stores the result back in Redis.

Reproduced in staging with 50 concurrent 
submissions for the same company:
  Before fix: 32 FeignClient calls
              (for identical data)
  After fix:  1 FeignClient call

This is a different problem from the 
TTL jitter I added last week.
Jitter prevents different keys 
from expiring simultaneously.
This fix prevents multiple requests 
for the SAME key from all calling 
upstream on concurrent miss.

THE FIX:
─────────
Mutex lock using Redis setIfAbsent 
(NX EX pattern).

When cache miss:
  First request acquires lock → 
    fetches from FeignClient → 
    stores in Redis → releases lock
  
  Other concurrent requests:
    Wait 50ms and retry from cache
    (up to 5 retries = 250ms total wait)
    
  If lock holder crashes:
    Lock expires after 10 seconds
    Next retry acquires lock
    
  If all retries exhausted:
    Falls back to direct FeignClient
    (safe, just more expensive)

WHAT DIDN'T CHANGE:
────────────────────
- TTL jitter (still there)
- Redis failure fallback (still there)
- Metric recording (still there)
- Invalidation method (still there)

HOW I FOUND IT:
────────────────
Manual load test in staging.
Script in /scripts/load-test-cache.sh 
(added to repo).

TESTS:
───────
New concurrent load test added:
  20 threads, simultaneous cache miss,
  verifies FeignClient called at most 2 times.

JIRA: EXP-276
```

You tagged Elena and Priya as reviewers. Then you sent a Slack message:

```
You (#expense-ap-dev):
───────────────────────
"Found a concurrent cache miss issue 
 during load testing on the approval 
 policy caching. 
 
 32 FeignClient calls instead of 1 
 when 50 concurrent requests hit 
 the same cache miss.
 
 Fixed with Redis mutex lock pattern.
 PR up for review: [link]
 
 The load test script is in the repo 
 if anyone wants to reproduce."
```

---

## Elena's Review

Elena reviewed it the next morning. Two comments.

Comment 1 — on the lock wait logic:

```
Elena's comment:
─────────────────
"The MAX_LOCK_RETRIES = 5 and 
 LOCK_WAIT_MS = 50 gives a 250ms 
 max wait before fallback.
 
 Is 250ms acceptable here?
 
 Think about what happens in the 
 worst case: request comes in,
 misses cache, can't get lock,
 waits 5 × 50ms = 250ms, 
 then calls FeignClient anyway.
 
 That request's latency is now:
 original FeignClient time (145ms P99)
 + 250ms lock wait
 = ~395ms in the worst case.
 
 That's higher than before caching.
 
 For the typical case (lock holder 
 completes in <100ms), it's fine.
 But I want you to think about 
 whether the worst-case is acceptable,
 and add a comment explaining the 
 tradeoff in the code."
```

You thought about this. She was right — the worst case latency was higher than before. But you had an answer:

```
Your reply:
────────────
"Good point on the worst case.
 
 I think it's still acceptable for 
 these reasons:
 
 1. The worst case (250ms wait + 
    FeignClient call) only happens if:
    - Cache misses (12% of requests)
    - AND concurrent requests arrive 
      at the exact same time (rare)
    - AND the lock holder is still 
      fetching after 250ms
    
    The lock holder timing out is 
    the edge of the edge case.
    User & Org FeignClient has a 
    5-second timeout, but P99 is 145ms.
    So the lock holder would normally 
    complete in under 145ms — well 
    within the first 50ms wait.
    
 2. The alternative — no lock — 
    means 32 FeignClient calls 
    for 50 concurrent requests 
    hitting the same miss.
    That's worse for the overall 
    system even if it's better 
    for a single request's latency.
 
 I'll add a comment explaining this 
 tradeoff explicitly."
```

You added the comment to the code:

```java
/**
 * Waits briefly and retries reading from cache.
 *
 * TRADEOFF: In the worst case, a request that 
 * cannot acquire the lock waits up to 
 * MAX_LOCK_RETRIES × LOCK_WAIT_MS (250ms)
 * before falling back to a direct FeignClient call.
 * 
 * This means worst-case latency for unlucky requests
 * is higher than before caching (250ms + FeignClient 
 * time vs FeignClient time alone).
 *
 * This is acceptable because:
 * 1. It only happens on concurrent cache misses —
 *    a rare scenario (most requests hit the cache).
 * 2. The P99 FeignClient latency is 145ms, 
 *    so the lock holder typically completes within
 *    the first 50ms wait — most waiters need 
 *    only 1 retry.
 * 3. The alternative (no lock) results in 
 *    N concurrent FeignClient calls, which 
 *    degrades the upstream service for everyone.
 *
 * If the FeignClient P99 were to increase 
 * significantly (>250ms), revisit this value.
 */
private ApprovalPolicy waitAndRetryFromCache(...) {
```

Elena: "Good reasoning. Approved."

Comment 2 — on the test:

```
Elena's comment:
─────────────────
"The test uses atMost(2) for 
 FeignClient calls.
 
 Why 2 and not 1?
 Is there a legitimate case 
 where 2 calls happen?"
```

```
Your reply:
────────────
"Yes — there's a timing edge case.
 
 Thread 1 acquires lock, fetches, 
 stores in Redis, releases lock.
 
 Thread 2 sees MISS, tries to get lock,
 fails (lock held), starts waiting.
 Thread 1 completes and releases lock.
 Thread 2's 50ms wait finishes —
 checks cache — finds result — returns.
 Only 1 FeignClient call.
 
 BUT: if Thread 1 completes and 
 releases the lock in under 50ms 
 (which is possible), AND Thread 2's 
 lock acquisition attempt happens 
 to fall in that exact window after 
 Thread 1 releases...
 Thread 2 could acquire the lock 
 and make its own FeignClient call 
 before checking the cache first.
 
 This is an extremely tight race.
 atMost(2) is the conservative bound.
 In practice it's almost always 1.
 
 If you prefer, I can tighten 
 the test to atMost(1) and accept 
 that it might be flaky in rare 
 cases. But I'd rather have a 
 slightly loose assertion than a 
 flaky test in CI."
```

Elena:

```
Elena:
───────
"atMost(2) is correct.
 
 And the explanation is exactly 
 what I wanted to see — you 
 thought through the race condition 
 in your own test.
 
 The comment about accepting a 
 loose assertion over a flaky test 
 is the right call.
 Flaky tests are worse than 
 imprecise tests.
 
 Approved."
```

Arjun left one comment — not a change request, just an observation:

```
Arjun:
───────
"This is the right pattern.
 
 Worth noting for the team:
 this is the 'mutex lock on 
 cache miss' solution to 
 thundering herd.
 
 There's also a 'probabilistic 
 early expiration' approach 
 (refresh before expiry with 
 some probability), which avoids 
 the lock entirely but is 
 more complex.
 
 For this use case, the mutex 
 approach is cleaner.
 
 Good find. Good fix."
```

---

## What Happened After — The Conversation With Priya

After the PR merged, Priya sent you a Slack message:

```
Priya:
───────
"I saw the PR. 
 I want to be honest with you —
 I didn't catch the concurrent 
 miss scenario when you shared 
 the design doc with me last week.
 
 I focused on the TTL stampede 
 and the fallback handling.
 The concurrent miss version 
 didn't cross my mind.
 
 You caught something I missed.
 That's not nothing."
```

```
You (reply):
─────────────
"Honestly — I almost didn't 
 test for it either.
 I had written 'run a load test' 
 in the design doc almost as 
 an afterthought.
 
 If I hadn't done it, this would 
 have made it to production.
 
 I think the lesson for me is:
 'run a load test' should never 
 be an afterthought for caching 
 code. It should be required."
```

Priya:

```
Priya:
───────
"Agreed. I'm going to add that 
 to our team norms for caching work —
 concurrent load testing before 
 merge is expected, not optional.
 
 You found something that makes 
 the whole team better.
 That's what good engineering 
 looks like."
```

Two days later, Lukas mentioned it in the sprint retrospective:

```
Lukas (retro):
───────────────
"One thing I want to call out 
 from this sprint:
 
 [Your name] found and fixed a 
 cache stampede bug that wasn't 
 in the acceptance criteria, 
 wasn't caught in review,
 and wasn't something anyone 
 had asked about.
 
 They found it through proactive 
 load testing and fixed it 
 independently before bringing 
 it to the team.
 
 That's the kind of ownership 
 we want to see.
 
 And as a result of it,
 Priya is adding concurrent load 
 testing to our caching work norms.
 
 Good example of learning 
 that sticks."
```

---

## The Measurement

Two weeks after the fix deployed to production, you pulled the numbers:

```
Monitoring period: 2 weeks post-deploy
Monday morning peak (9-10am CET):
──────────────────────────────────────

Concurrent cache miss events detected:
  (cases where lock was acquired 
   and waiting occurred)
  Average per peak hour: 8-12 events

FeignClient calls per concurrent miss event:
  Before fix: 32 (from load test data)
  After fix: 1.02 average
    (occasionally 2 — the edge case 
     Elena's comment explained)

Lock wait time for non-holders:
  P50: 38ms (within first retry)
  P95: 67ms (within second retry)
  P99: 142ms (within third retry)
  
  Meaning: 99% of waiting requests 
  get the result within 142ms.
  No request needed all 5 retries 
  in the 2-week observation period.

Fallback rate (all retries exhausted):
  0 occurrences in 2 weeks.
  
  The lock holder always completed 
  before waiters exhausted retries.
  The fallback is there for safety —
  it was never needed.

Cache hit rate: 88.4% (unchanged)
FeignClient calls/min peak: 44 (unchanged)
User & Org CPU: 19% (unchanged)

The original improvements held.
The stampede fix added protection 
without changing the overall numbers.
```

---

## What This Story Was Actually About

```
The technical content — mutex locks, 
setIfAbsent, thundering herd — 
was real and correct.

But the story isn't about Redis.

It's about a shift in how you operated.

Before month 10:
  You found problems when they were 
  pointed out to you.
  You fixed them with guidance.

Month 10:
  You ran a test nobody asked you to run.
  You found something nobody had caught.
  You researched the fix yourself.
  You wrote it before asking for validation.
  You brought it to the team 
  already solved.

That's not because you became 
a different person.
It's because you had enough 
understanding of the system —
of caching, of Redis, of concurrent 
access patterns — that when 
something didn't look right,
you could investigate it yourself.

Knowledge accumulates slowly 
and then tips into capability.

Month 10 was where the tip happened.
```

---

## The "Tricky Question" Preparation

---

**Q1: "You said 32 FeignClient calls instead of 1. How did you actually measure that?"**

```
Two ways — both in Datadog.

First: the approval_policy.cache.result 
counter I had added in Story 12.
During the load test, I watched 
the MISS counter increment in near 
real-time. 32 MISS events for 
50 concurrent requests hitting 
the same company's policy.

Second: the http.client.requests metric 
filtered to User & Org Service's 
approval-policy endpoint.
32 outbound HTTP calls from 
Expense Service to User & Org 
within a 200ms window.
All for the same company UUID.

The two numbers matched — 32 MISS events, 
32 outbound FeignClient calls.
Cross-validated.

After the fix, same load test:
MISS counter: 1
FeignClient calls: 1
(occasionally 2 due to the timing race 
Elena's review identified)
```

---

**Q2: "Why use Redis for the lock and not Java's synchronized or a ReentrantLock?"**

```
Because we run multiple instances 
of Expense Service.

Java's synchronized and ReentrantLock 
are in-process locks.
They only prevent concurrent access 
within a SINGLE JVM.

If Instance A handles Request 1 
(acquires Java lock, calls FeignClient)
and Instance B handles Request 2 
(also acquires its own Java lock, 
calls FeignClient too)...
both call FeignClient simultaneously 
because they're in different JVMs.
A Java lock doesn't reach across instances.

Redis is a shared external system.
All instances of Expense Service 
connect to the same Redis.
When Instance A sets the lock key in Redis,
Instance B can see it.
The lock works across the entire cluster.

That's why it has to be Redis.
Not a Java primitive.
Distributed lock for a distributed system.
```

---

**Q3: "What's the difference between what jitter fixes and what the mutex lock fixes?"**

```
Different problems. Same category (stampede).
But different scenarios.

Jitter fixes: the TTL expiry stampede.
──────────────────────────────────────
Scenario: 5,000 companies all 
cached at the same time with 
the same TTL (15 minutes).
At T+15 minutes: all 5,000 keys 
expire simultaneously.
All 5,000 next requests call 
FeignClient simultaneously.
That's 5,000 concurrent calls.

Jitter: each key gets 
15min + random(0-5min).
Expirations spread over 20 minutes.
Never 5,000 simultaneous calls.
Maybe 10-15 at any given moment.

Mutex lock fixes: the concurrent miss stampede.
──────────────────────────────────────────────
Scenario: One company's cache key expires.
50 employees from that company 
submit expenses simultaneously.
All 50 check Redis, all see MISS,
all call FeignClient before any of 
them stores the result.

Mutex lock: first request gets 
the lock and fetches.
Other 49 wait and get result 
from cache after first stores it.
1 FeignClient call instead of 49.

Summary:
Jitter: prevents multiple different 
keys from expiring simultaneously.
Mutex: prevents multiple concurrent 
requests for the SAME key from 
all calling upstream on miss.

Both are needed.
They address different dimensions 
of the same class of problem.
```

---

**Q4: "You said this was the first time you solved something significant without asking for help first. Is that actually true? What does that mean to you?"**

```
It's true in a specific sense.

I had been getting better at solving 
technical problems — the N+1 fix,
the @Transactional proxy issue,
the approval workflow concurrency.
But in each of those, someone caught 
the problem first and I fixed it,
often with their guidance on the solution.

This was different because:
I ran a test nobody asked me to run.
I identified the problem myself.
I researched the fix myself.
I implemented it and wrote the PR 
before anyone knew there was 
a problem to fix.

What it meant to me:
It was evidence that my understanding 
of the system had reached a point 
where I could identify non-obvious 
problems independently.

Before, I was in reactive mode —
seeing problems when they were 
pointed out.
Month 10 was the first time I was 
in proactive mode — creating the 
conditions to find a problem 
before it found me.

That's a real shift.
Not about being smarter.
About having enough context to 
know what questions to ask 
and where to look.

Priya's comment — "you caught 
something I missed" — 
wasn't about me being better 
than Priya. She's been doing 
this much longer than me.
It was about me being close 
enough to this specific piece 
of code to notice something 
she didn't because she wasn't 
running the same test.

Proximity to a problem gives you 
a different kind of vision than 
experience does.
Sometimes junior and senior 
see different things.
That's not hierarchy.
That's just perspective.
```

---

Story 13 complete.

```
What Stories 12 and 13 together show:
──────────────────────────────────────

Story 12 (design meeting):
  You proposed a solution 
  with data and evidence.
  You got buy-in.
  You were assigned ownership.
  87% reduction in FeignClient calls.

Story 13 (stampede fix):
  You found a problem in your 
  own implementation.
  You solved it before anyone 
  asked you to.
  You brought it to the team 
  already fixed.
  It became a team norm 
  (concurrent load testing 
  for caching work).

Together: this is what it looks 
like when someone transitions 
from "implementing what's designed" 
to "finding what's missing."

That's the Block 4 theme —
"Trusted Contributor."
Not just trusted to implement.
Trusted to notice.
```

Ready for Story 14 — teaching Léa @PreAuthorize, your first clear mentorship moment. Shall I begin?