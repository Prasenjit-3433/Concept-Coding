# Story 12: Caching Proposal in Design Discussion — First Time You Spoke Up Architecturally

---

## Context — Where You Were at Month 10

```
By month 10, something had quietly shifted.

Month 1-3:   You asked where files were.
Month 4-6:   You owned features end-to-end 
             but always within someone 
             else's design.
Month 7-9:   You touched Kafka consumers,
             shadowed Priya on Redis basics,
             survived your first production 
             incident war room.

Month 10:    You had opinions.

Not loud opinions.
Not "I know better than everyone" opinions.
But when you looked at a problem,
you had a direction you thought was right —
and you had enough context to explain why.

The question was whether you'd say it.
```

Something else had changed too. Elena had started occasionally asking you direct questions in technical discussions — not just "does everyone agree?" to the room, but specifically: *"What do you think about this?"*

That was new. And it was uncomfortable in a good way.

---

## The Situation

It was week 2 of month 10. Sprint planning had finished and the team had a technical design session scheduled for Thursday afternoon — 90 minutes, the whole engineering team, to discuss a problem that had been quietly growing.

```
The problem:
─────────────
Every time an employee submits an expense,
Expense Service calls User & Org Service
via FeignClient to fetch the approval policy.

"What are the approval rules 
 for this company?"
  → amount < €50: no approval needed
  → €50-€2,000: manager approves
  → > €2,000: manager, then finance manager

This policy almost never changes.
A company sets it once when they 
onboard onto Moss. Maybe updates it 
once a quarter at most.

But it's fetched on every single 
expense submission.

At small scale: fine.
At current scale: 
  5,000 companies.
  Some companies have 200+ employees.
  Monday morning: hundreds of expenses 
  submitted simultaneously.
  User & Org Service gets hammered 
  with hundreds of FeignClient calls
  for data that hasn't changed 
  since last Tuesday.

Arjun had flagged this in a retro 
two months earlier. Marta had noticed 
it when she was onboarding 
(back in month 3).
It had been on the backlog ever since.
Now it was time to fix it.
```

Lukas sent a calendar invite:

```
Technical Design Session — 
Approval Policy Caching

Agenda:
1. Current state (10 min)
2. Options discussion (40 min)
3. Decision + approach (20 min)
4. Owner assignment (10 min)

Attendees:
  Elena Müller (Tech Lead)
  Arjun Sharma (Senior)
  Sophie Laurent (Mid-level)
  Tomás Novák (Mid-level)
  Priya Nair (Mid-level)
  [Your name] (Junior)
  Kemal Aydin (Junior)
  Lukas Becker (EM)
```

You were included. That alone meant something — junior engineers weren't always in architecture discussions. But you had touched Redis basics with Priya for the past two months. Lukas and Elena knew that.

The meeting was Thursday. It was Monday now.

---

## What You Did Before the Meeting

This is the part that mattered most — what happened before Thursday.

You spent Tuesday evening going through your notes from Priya's Redis sessions. You re-read the caching section from the module notes you had been building. You looked at the actual `ApprovalPolicyService` code:

```java
// Current state — no caching
@Service
@RequiredArgsConstructor
public class ApprovalPolicyService {

    private final UserOrgFeignClient userOrgFeignClient;

    public ApprovalPolicy getApprovalPolicy(UUID companyId) {
        // Direct FeignClient call every single time
        return userOrgFeignClient.getApprovalPolicy(companyId);
    }
}
```

You looked at how many times this was called in a typical request flow. You traced through `ExpenseService.submitExpense()` — one call. Through `ApprovalRoutingService.determineApprovalSteps()` — called from within submitExpense, also one call. Same underlying method, same company, same data.

Then you opened Datadog and looked at the FeignClient call metrics for `userOrgFeignClient.getApprovalPolicy`. You found something concrete:

```
Datadog query:
───────────────
Metric: http.client.requests
Filter: uri=/api/v1/companies/{id}/approval-policy
        service=expense-service

Result (Monday morning peak, last 4 weeks):
  P50 latency:  18ms
  P99 latency:  145ms
  Calls/minute: ~340 during peak (9-10am CET)

User & Org Service CPU during same window:
  Normal baseline: 15-20%
  Monday 9-10am:   68-72%
  Spike duration:  ~45 minutes
```

You wrote this down. These were real numbers from real data. You now had something to bring to the meeting — not just "I think we should cache this" but "here's what the current situation actually looks like."

Then you thought through the solution. You had learned Cache-Aside with Priya. You understood what Redis could do here. You sketched out the approach on paper:

```
YOUR SKETCH (Tuesday evening, Notion):
────────────────────────────────────────

Problem:
  FeignClient to User & Org on every 
  expense submission.
  340 calls/min at peak.
  Most are for the same 50-100 active 
  companies submitting expenses.

Solution: Cache-Aside with Redis

On approval policy fetch:
1. Check Redis: 
   key = "approval_policy:{companyId}"
2. HIT → return cached policy
   (no FeignClient call)
3. MISS → call User & Org FeignClient
           store result in Redis with TTL
           return result

TTL question:
  How often does approval policy change?
  Realistically: rarely.
  But if we cache for 24 hours and 
  a company changes their policy,
  expenses submitted in the next 24 hours
  get routed through wrong approval flow.
  That's a compliance issue.
  
  Safer TTL: 15 minutes.
  15 min staleness acceptable.
  If policy changes, worst case:
  next 15 minutes uses old rules.
  After that, fresh data.

Invalidation:
  When does User & Org Service 
  update an approval policy?
  That service publishes an event 
  on policy change.
  We should consume it and delete 
  the Redis key immediately.
  Then TTL is just a safety net,
  not the primary mechanism.

L1 consideration:
  Can we also use Caffeine (local)?
  Each instance caches its own copy.
  Pros: zero network, faster than Redis.
  Cons: 
    - Multiple instances each have 
      separate local caches.
    - If policy changes, L1 invalidation 
      is harder — need Redis Pub/Sub 
      to notify all instances.
    - More complexity.
  
  Recommendation:
  Start with Redis only (L2).
  Add Caffeine (L1) later if Redis 
  latency becomes a bottleneck.
  Don't over-engineer on first pass.

Expected impact:
  Cache hit rate for approval policy: 
  estimated 85-90%
  (same 50-100 companies hitting it repeatedly)
  
  Reduction in FeignClient calls: ~85-90%
  User & Org Service CPU: back to baseline
  Expense submission latency: 
  reduce by ~15-20ms per request
  (the FeignClient call cost removed for hits)
```

By Wednesday, you had this written out clearly. You sent it to Priya on Slack:

```
You (Slack to Priya):
──────────────────────
"Hey Priya — preparing for Thursday's 
 design session on approval policy caching.
 I sketched out a Cache-Aside approach 
 with Redis, 15-minute TTL, and event-driven 
 invalidation via the user.policy_updated 
 Kafka topic.
 
 Would you be willing to take 5 minutes 
 to read it before the meeting? 
 I want to make sure I'm not missing 
 something obvious before I bring it up."
```

Priya replied in an hour:

```
Priya:
───────
"Looked at it. The direction is right.
 Two things to think about:
 
 1. The TTL stampede risk — if 5,000 
    companies all have their cache 
    expire at the same time, 
    you get a thundering herd to 
    User & Org Service. 
    Add jitter to the TTL 
    (e.g., 15 min + random 0-5 min).
 
 2. The Kafka topic for invalidation —
    check with Arjun whether 
    user.policy_updated actually exists.
    User & Org team might not publish 
    this event yet. If not, your 
    invalidation plan needs a fallback.
 
 Good prep. Bring it up Thursday."
```

You checked with Arjun on Slack:

```
You (Slack to Arjun):
──────────────────────
"Quick question before Thursday —
 does User & Org Service publish 
 a Kafka event when an approval 
 policy is updated?
 
 Thinking about cache invalidation 
 strategy for the design session."
```

Arjun:

```
Arjun:
───────
"No, they don't publish that event yet.
 That service is a bit behind on 
 event publishing.
 
 It's on their roadmap but 
 probably 2-3 months away.
 
 For now: TTL-only invalidation 
 is the realistic option.
 Design for event-driven invalidation 
 eventually — but don't block 
 on it for the initial implementation."
```

You updated your notes. Event-driven invalidation: planned, not yet available. TTL is the primary mechanism for now. You added a note to mention this in the meeting — it's the kind of honest constraint that shows you've actually investigated, not just theorized.

---

## The Meeting — Thursday Afternoon

The Google Meet started at 2pm CET (5:30pm IST for you). 8 people. Elena shared her screen with the Confluence page showing the current state of the problem.

Elena opened:

```
Elena:
───────
"Okay — everyone knows the context.
 Approval policy fetched on every 
 expense submission, no caching, 
 User & Org Service is feeling it 
 on Monday mornings.
 
 I want to open the floor before 
 I share my thinking.
 
 What are we actually dealing with?
 Does anyone have data on the 
 current state?"
```

Two seconds of silence. Then you spoke:

```
You:
─────
"I pulled some numbers from Datadog 
 before the meeting.
 
 During peak — Monday 9-10am — 
 we're making about 340 FeignClient 
 calls per minute to User & Org 
 for approval policy.
 P99 latency on those calls is 145ms.
 
 And on the User & Org Service side,
 CPU spikes from their normal 15-20% 
 up to 68-72% during that same window.
 That lasts about 45 minutes.
 
 So this is real — it's not 
 a theoretical problem."
```

Arjun: "Good data. Yeah, that lines up with
what the User & Org team has been
flagging to us."

Elena nodded. "Okay. Options. What are we
thinking?"

You had decided beforehand to let others speak first. You waited.

Tomás spoke first:

```
Tomás:
───────
"Simplest fix: just cache in Caffeine 
 locally. Each instance has its own copy.
 No Redis setup needed. 
 Faster than network round-trip."
```

Sophie:

```
Sophie:
────────
"The problem with local-only is 
 invalidation. If a company changes 
 their policy, we can't tell the 
 other instances to clear their cache.
 Each instance expires independently.
 Maximum staleness is whatever 
 the TTL is."
```

Tomás: "Is 15 minutes of staleness actually a problem in practice?"

Sophie: "For approval routing — potentially yes.
If they lower the threshold from €2,000
to €500, expenses submitted in the next
15 minutes skip a required approval step."

That was the right framing. You had thought about this too. You added:

```
You:
─────
"I sketched out a Cache-Aside approach 
 with Redis before the meeting.
 
 The idea is:
 On every getApprovalPolicy() call,
 check Redis first.
 Key: 'approval_policy:{companyId}'
 Hit → return cached, no FeignClient call.
 Miss → call User & Org, store in Redis, return.
 
 TTL at 15 minutes — with jitter.
 If we set exactly 15 minutes for all 
 5,000 companies, they all expire 
 simultaneously and we get a thundering 
 herd hitting User & Org at once.
 Adding a random 0-5 minute jitter 
 spreads the expirations out.
 
 For invalidation — I checked with Arjun,
 User & Org doesn't publish a 
 policy_updated event yet.
 That's on their roadmap but 
 probably 2-3 months out.
 
 So short term: TTL is the 
 primary invalidation mechanism.
 We design the code to be ready 
 for event-driven invalidation 
 when that event becomes available —
 just wire up a Kafka consumer 
 that does a Redis DELETE on the key.
 The structure is already there."
```

The room was quiet for a moment. Elena was writing something in Confluence.

Arjun: "The jitter point is good.
What TTL are you proposing exactly?"

```
You:
─────
"15 minutes base, plus a random 
 0 to 5 minutes. So each key 
 expires somewhere between 
 15 and 20 minutes.
 
 For approval policy, I think 
 15 minutes is acceptable.
 A company changing their threshold 
 mid-day is unusual. And if they do,
 worst case is 20 minutes before 
 the cache clears and new policy applies.
 
 If Elena or Arjun think that's 
 too long, we could go to 10 minutes — 
 but I wanted to propose something 
 that doesn't result in too many 
 Redis round-trips either."
```

Elena: "15 to 20 minutes is fine for this data.
It's not order-critical.
Even 30 minutes would be acceptable."

Priya spoke up:

```
Priya:
───────
"One thing to consider —
 should we also add a local Caffeine 
 layer on top of Redis?
 The approval policy is requested 
 on every expense submission.
 Even a Redis call adds a network 
 round-trip — 1-3ms.
 At 340 calls/minute peak, 
 that's still adding latency 
 even on cache hits."
```

You had thought about this. You had a position:

```
You:
─────
"I thought about this too.
 My feeling is — start with Redis only.
 
 At 1-3ms per Redis call, 
 we're removing ~145ms of FeignClient 
 latency and replacing it with 1-3ms.
 That's still a massive improvement.
 
 Adding Caffeine on top means 
 we need to think about L1/L2 
 synchronization — if policy changes,
 Redis event-driven invalidation 
 won't automatically clear each 
 instance's Caffeine cache.
 We'd need Redis Pub/Sub for that.
 
 That's more complexity for 
 a problem we don't have yet.
 
 I'd say: ship Redis-only first,
 measure, and add Caffeine 
 if Redis latency is still 
 showing up as a bottleneck."
```

Priya: "That's reasonable. Agreed — don't over-engineer upfront."

Elena: "Okay. I think the direction is clear.
Cache-Aside with Redis, 15-20 minute TTL with jitter,
design for event-driven invalidation later.
Redis-only for now, Caffeine added if needed."

She looked at you:

```
Elena:
───────
"You've clearly thought this through.
 Do you want to own the implementation?"
```

```
You:
─────
"Yes — I can take it."
```

```
Elena:
───────
"Good. I'll want to review the 
 implementation design before 
 you start coding — not because 
 I don't trust your approach,
 but because caching is one of 
 those areas where the details 
 matter. Write up a short doc,
 share it with me and Priya 
 before you write the first line.
 
 One hour should be enough."
```

The meeting wrapped. Lukas closed with:

```
Lukas:
───────
"Good session. Action items:
 [Your name] owns the implementation.
 Elena and Priya review the design doc.
 Target: merged before end of sprint."
```

---

## After the Meeting — What Elena Said Privately

That evening, Elena sent you a Slack message:

```
Elena (Slack DM):
──────────────────
"Good contribution today.
 The Datadog numbers before the meeting
 were exactly the right thing to do.
 You weren't just theorizing —
 you had evidence.
 
 That's how technical proposals 
 should be made.
 
 The jitter point was also solid.
 Tomás and I have both forgotten 
 about stampede scenarios in 
 the past. Good that you caught it.
 
 Looking forward to the design doc."
```

```
You read this three times.

Not because you needed the validation.
But because it told you something 
you hadn't been sure of:

That the way you prepared,
the way you brought data instead of opinion,
the way you proposed and then 
listened before pushing your view —
that was the right approach.

Not just lucky. Actually right.
```

---

## The Design Document

Before writing any code, you wrote the one-hour design doc Elena had asked for. This was your first time writing something like this for a real system decision.

```
DESIGN DOC: Approval Policy Caching
Author: [Your name]
Date: [Month 10, Week 2]
Reviewers: Elena Müller, Priya Nair
Status: DRAFT
────────────────────────────────────

PROBLEM
────────
ApprovalPolicyService.getApprovalPolicy() 
makes a synchronous FeignClient call to 
User & Org Service on every expense 
submission. This data changes rarely 
(estimated: monthly at most per company).

Current cost (from Datadog):
- 340 calls/min at peak
- P99 latency: 145ms per call
- User & Org Service CPU: 68-72% 
  during Monday peak

PROPOSED SOLUTION
──────────────────
Cache-Aside pattern using Redis.

READ FLOW:
  1. Check Redis key: approval_policy:{companyId}
  2. HIT  → return cached ApprovalPolicy object
  3. MISS → call FeignClient
             serialize and store in Redis
             return result

WRITE/INVALIDATION FLOW:
  TTL-based (primary, available now):
    Key expires after 15min + random(0-5min)
    Next request triggers fresh fetch
  
  Event-driven (planned, ~Q1 next year):
    User & Org publishes user.policy_updated
    We consume it → DELETE Redis key immediately
    TTL becomes safety net only

REDIS KEY DESIGN:
  Key:    "approval_policy:{companyId}"
  Value:  JSON-serialized ApprovalPolicy object
  TTL:    15 minutes + random 0-300 seconds (jitter)

WHY JITTER:
  5,000 companies, all cached at startup.
  Without jitter: all expire simultaneously.
  With jitter: expirations spread over 20 minutes.
  Prevents thundering herd to User & Org.

WHY NOT CAFFEINE (L1):
  Redis-only first.
  Adding L1 requires Redis Pub/Sub 
  for cross-instance invalidation.
  Complexity not justified yet.
  Redis hit rate improvement alone 
  (~85% estimated) sufficient.

EXPECTED IMPACT:
  FeignClient calls/min: 340 → ~50 (estimated)
  P99 latency per submission: -~145ms 
  (for cache hits — most submissions)
  User & Org CPU during peak: back to baseline

RISKS:
  Staleness: max 20 min (acceptable for policy data)
  Stampede: mitigated by jitter
  Redis down: fallback to FeignClient 
              (must handle gracefully in code)
  Data structure size: ApprovalPolicy object ~1KB,
              5,000 companies = ~5MB Redis memory
              (negligible)

IMPLEMENTATION PLAN:
  1. Update ApprovalPolicyService to add 
     Redis check/store
  2. Add error handling for Redis failure 
     (fallback to FeignClient)
  3. Add jitter to TTL calculation
  4. Add Datadog metrics: cache hit/miss counter
  5. Update integration tests
  6. Monitor hit rate in Datadog for 48 hours 
     after deploy
```

You shared it on Slack with Elena and Priya, tagged them, and went to sleep.

The next morning:

```
Elena (Confluence comment):
────────────────────────────
"Approved with one change:
 Add a section on what happens 
 if Redis is completely unavailable 
 (network partition, Redis crash).
 
 The service must not break — 
 it should fall back to FeignClient.
 Redis is a performance optimization,
 not a hard dependency.
 This must be explicit in the doc 
 and in the code."

Priya (Confluence comment):
────────────────────────────
"Looks good. +1 on Elena's point.
 Also: mention the metric you'll 
 add to measure hit rate.
 Without a metric, how will you 
 know if the caching is actually 
 working as expected?"
```

You updated the doc. Two additions:

```
REDIS FAILURE HANDLING:
  If Redis is unavailable, 
  getApprovalPolicy() must still work.
  Implementation: try Redis first,
  catch RedisConnectionException,
  fall through to FeignClient.
  Log the fallback as WARN.
  
  Redis is a performance optimization.
  The service must not have Redis 
  as a hard dependency.

MONITORING:
  Custom Micrometer counter:
    approval_policy.cache.result
    tag: result = HIT / MISS / FALLBACK
  
  Datadog dashboard panel:
    Hit rate = HIT / (HIT + MISS) × 100
  Target: >85% hit rate within 
  24 hours of deploy.
```

Elena: "Good. Go build it."

---

## The Implementation

You built it over two days. This is the actual code:

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
    private static final Duration BASE_TTL = 
        Duration.ofMinutes(15);
    private static final int JITTER_MAX_SECONDS = 300;
    // 300 seconds = 5 minutes max jitter

    public ApprovalPolicy getApprovalPolicy(UUID companyId) {

        String cacheKey = CACHE_KEY_PREFIX + companyId;

        // Step 1: Try Redis (L2 cache)
        try {
            ApprovalPolicy cached = redisTemplate
                .opsForValue()
                .get(cacheKey);

            if (cached != null) {
                recordCacheResult("HIT");
                log.debug("Approval policy cache HIT " +
                    "for company: {}", companyId);
                return cached;
            }

            // Step 2: Cache MISS — fetch from upstream
            ApprovalPolicy policy = fetchFromUpstream(companyId);

            // Step 3: Store in Redis with jittered TTL
            Duration ttl = getTTLWithJitter();
            redisTemplate.opsForValue().set(
                cacheKey, policy, ttl);

            recordCacheResult("MISS");
            log.debug("Approval policy cache MISS " +
                "for company: {}. " +
                "Cached with TTL: {}",
                companyId, ttl);

            return policy;

        } catch (RedisConnectionFailureException 
                 | QueryTimeoutException e) {

            // Redis is unavailable — fall back to FeignClient
            // Log as WARN — this is degraded mode, 
            // not a failure
            log.warn("Redis unavailable for approval policy " +
                "lookup. Falling back to FeignClient. " +
                "Company: {}", companyId, e);

            recordCacheResult("FALLBACK");
            return fetchFromUpstream(companyId);
        }
    }

    /**
     * Fetches approval policy directly from 
     * User & Org Service.
     * Called on cache miss or Redis fallback.
     */
    private ApprovalPolicy fetchFromUpstream(UUID companyId) {
        return userOrgFeignClient.getApprovalPolicy(companyId);
    }

    /**
     * Called when approval policy is invalidated.
     * Will be triggered by Kafka consumer when
     * user.policy_updated event becomes available.
     * For now: called manually if needed.
     */
    public void invalidateApprovalPolicy(UUID companyId) {
        String cacheKey = CACHE_KEY_PREFIX + companyId;
        Boolean deleted = redisTemplate.delete(cacheKey);
        log.info("Approval policy cache invalidated " +
            "for company: {}. Key existed: {}",
            companyId, deleted);
    }

    /**
     * Returns TTL with random jitter to prevent
     * cache stampede when many keys expire simultaneously.
     *
     * Base: 15 minutes
     * Jitter: 0-5 minutes random
     * Total: 15-20 minutes
     */
    private Duration getTTLWithJitter() {
        int jitterSeconds = ThreadLocalRandom.current()
            .nextInt(0, JITTER_MAX_SECONDS);
        return BASE_TTL.plusSeconds(jitterSeconds);
    }

    /**
     * Records cache result to Micrometer 
     * for Datadog monitoring.
     */
    private void recordCacheResult(String result) {
        Counter.builder("approval_policy.cache.result")
            .tag("result", result)
            .register(meterRegistry)
            .increment();
    }
}
```

**The Redis configuration:**

```java
@Configuration
public class RedisConfig {

    // Generic template for most keys
    @Bean
    public RedisTemplate<String, ApprovalPolicy> 
            approvalPolicyRedisTemplate(
            RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, ApprovalPolicy> template = 
            new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // String serializer for keys
        template.setKeySerializer(
            new StringRedisSerializer());

        // JSON serializer for values
        // Stores ApprovalPolicy as JSON — 
        // readable in Redis UI for debugging
        template.setValueSerializer(
            new GenericJackson2JsonRedisSerializer());

        return template;
    }
}
```

**The integration test:**

```java
@SpringBootTest
@Testcontainers
class ApprovalPolicyServiceIntegrationTest {

    @Container
    static GenericContainer<?> redis = 
        new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(
            DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port",
            () -> redis.getMappedPort(6379));
    }

    @Autowired
    private ApprovalPolicyService approvalPolicyService;

    @MockBean
    private UserOrgFeignClient userOrgFeignClient;

    @Autowired
    private RedisTemplate<String, ApprovalPolicy> 
        redisTemplate;

    @Test
    void getApprovalPolicy_shouldReturnCachedResult_onSecondCall() {

        UUID companyId = UUID.randomUUID();
        ApprovalPolicy policy = buildTestPolicy(companyId);

        // FeignClient returns policy on first call
        given(userOrgFeignClient.getApprovalPolicy(companyId))
            .willReturn(policy);

        // First call — should hit FeignClient
        ApprovalPolicy first = approvalPolicyService
            .getApprovalPolicy(companyId);

        // Second call — should hit Redis, 
        // NOT FeignClient
        ApprovalPolicy second = approvalPolicyService
            .getApprovalPolicy(companyId);

        // FeignClient called exactly once
        verify(userOrgFeignClient, times(1))
            .getApprovalPolicy(companyId);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void getApprovalPolicy_shouldFallbackToFeignClient_whenRedisUnavailable() {

        // Stop the Redis container to simulate 
        // Redis being unavailable
        redis.stop();

        UUID companyId = UUID.randomUUID();
        ApprovalPolicy policy = buildTestPolicy(companyId);

        given(userOrgFeignClient.getApprovalPolicy(companyId))
            .willReturn(policy);

        // Should NOT throw — should fallback gracefully
        ApprovalPolicy result = approvalPolicyService
            .getApprovalPolicy(companyId);

        assertThat(result).isEqualTo(policy);
        verify(userOrgFeignClient, times(1))
            .getApprovalPolicy(companyId);

        // Restart for other tests
        redis.start();
    }

    @Test
    void invalidateApprovalPolicy_shouldDeleteCacheKey() {

        UUID companyId = UUID.randomUUID();
        String cacheKey = "approval_policy:" + companyId;

        // Pre-populate cache
        redisTemplate.opsForValue().set(
            cacheKey, buildTestPolicy(companyId));

        // Invalidate
        approvalPolicyService
            .invalidateApprovalPolicy(companyId);

        // Key should be gone
        assertThat(redisTemplate.hasKey(cacheKey))
            .isFalse();
    }

    private ApprovalPolicy buildTestPolicy(UUID companyId) {
        return ApprovalPolicy.builder()
            .companyId(companyId)
            .rules(List.of(
                ApprovalRule.builder()
                    .minAmount(BigDecimal.ZERO)
                    .maxAmount(new BigDecimal("50"))
                    .approverRole(ApproverRole.SELF)
                    .build(),
                ApprovalRule.builder()
                    .minAmount(new BigDecimal("50"))
                    .maxAmount(new BigDecimal("2000"))
                    .approverRole(ApproverRole.MANAGER)
                    .build()
            ))
            .build();
    }
}
```

---

## The Result — 48 Hours After Deploy

You deployed to staging on Thursday. Elena reviewed the PR — 3 comments, all minor, all addressed in 30 minutes. Merged Friday morning. Deployed to production Friday afternoon.

By Sunday evening you opened Datadog:

```
BEFORE (Monday peak, previous 4 weeks avg):
  FeignClient calls/min: 340
  P99 latency (expense submission): ~165ms
  User & Org CPU at peak: 68-72%

AFTER (Saturday, Sunday — first 48 hours):
  approval_policy.cache.result:
    HIT:      87.3%
    MISS:     12.5%
    FALLBACK: 0.2%  (Redis briefly unavailable 
                     Saturday night — maintenance)
  
  FeignClient calls/min: ~43
    (340 × 12.5% misses = ~42.5)
  
  P99 latency (expense submission):
    Pending Monday data — but on Saturday:
    P99 dropped from ~165ms to ~22ms
    (FeignClient call removed for hits)
  
  User & Org CPU:
    No Monday data yet.
    But Saturday's traffic: flat at 18%.
    Previously: 18% on Saturday too 
    (Monday is the real test).
```

You sent a message in `#expense-ap-dev`:

```
You (Slack):
─────────────
"Approval policy caching deployed.
 First 48 hours:
 
 Cache hit rate: 87.3%
 FeignClient calls reduced: ~87%
 
 Real Monday test tomorrow.
 Will share User & Org CPU 
 numbers then.
 
 Datadog dashboard panel added:
 [link to panel]"
```

Monday came. The dashboard showed:

```
Monday 9-10am peak:
─────────────────────
FeignClient calls/min: 44 
  (was 340 — 87% reduction)

P99 latency (expense submission): 23ms
  (was 165ms — 86% reduction)

User & Org Service CPU: 19%
  (was 68-72% — back to baseline)

Cache hit rate: 88.1%
  (higher than weekend — 
   more repeat companies 
   submitting Monday morning)
```

Arjun sent a message:

```
Arjun (Slack #expense-ap-dev):
────────────────────────────────
"The User & Org team just messaged me —
 they noticed their CPU is flat this morning.
 They asked what changed.
 
 I told them about the caching.
 They said thank you.
 
 Nice work."
```

Elena added one more comment in the thread:

```
Elena:
───────
"87% reduction in FeignClient calls.
 This is what 'I did X and it improved Y%'
 looks like when you actually have 
 the numbers to back it up.
 
 Good job."
```

---

## What This Story Was Actually About

```
The technical part — Cache-Aside, 
Redis, TTL jitter — was important.
But it wasn't the main thing.

The main thing was the meeting.

You were a junior engineer.
10 months in.
First Java production role.
In a room with 2 senior engineers,
a tech lead, and an EM.

And you spoke up.
Not loudly.
Not defensively.
Not to show off.

You had prepared.
You had numbers.
You had thought through objections 
before the meeting (stampede, 
invalidation, L1 complexity).
You proposed clearly.
You listened when Priya and Sophie 
added things.
You acknowledged the constraints 
(User & Org event not available yet)
instead of pretending they didn't exist.

And Elena asked if you wanted 
to own the implementation.

That wasn't an accident.
That's what preparation and 
honest participation looks like.
```

---

## The "Tricky Question" Preparation

---

**Q1: "You mentioned TTL jitter to prevent stampede. Can you explain that concretely?"**

```
Without jitter:
────────────────
At startup, we fetch and cache 
approval policies for 200 active companies.
All cached with TTL = 15 minutes exactly.
At T+15 minutes, all 200 expire simultaneously.
All 200 next requests hit FeignClient at once.
User & Org Service gets 200 concurrent calls.
That's the stampede — worse than no caching.

With jitter:
─────────────
Each key gets: 15 min + random(0-5 min).
Company A: expires at T+15:43
Company B: expires at T+17:22
Company C: expires at T+19:05

Expirations spread over 20 minutes.
At any given moment, maybe 10-15 keys 
expire, not 200 all at once.
FeignClient sees a steady 10-15 calls 
per minute instead of 200 simultaneous.
User & Org Service barely notices.

Implementation:
int jitter = ThreadLocalRandom.current()
    .nextInt(0, 300); // 0-300 seconds
Duration ttl = Duration.ofMinutes(15)
    .plusSeconds(jitter);

ThreadLocalRandom is better than 
Random here because it's designed 
for concurrent use — no lock contention.
```

---

**Q2: "Why didn't you add Caffeine (local L1 cache) on top of Redis? Wouldn't that be faster?"**

```
Yes, it would be faster.
Redis requires a network round-trip: 
1-3ms per call.
Caffeine is in-memory: nanoseconds.

But I decided against it for the 
initial implementation for three reasons.

First: the improvement without Caffeine 
was already massive.
Replacing a 145ms FeignClient call 
with a 1-3ms Redis call is a 97% 
reduction in that cost.
At 1-3ms, Redis is not a bottleneck.

Second: adding Caffeine creates an 
invalidation problem.
If a company changes their approval policy,
we can delete the Redis key.
But each service instance has its own 
Caffeine cache — we'd need Redis Pub/Sub 
to broadcast the invalidation to 
every instance simultaneously.
That's meaningful additional complexity.

Third: over-engineering on first pass 
creates maintenance burden.
Build the simplest thing that works,
measure it, add complexity only if 
measurements show it's needed.

The monitoring showed 87% hit rate 
and approval submission latency 
dropping from 165ms to 23ms.
Redis is not the bottleneck.
Caffeine would buy maybe 1-2ms.
Not worth the additional complexity.

If the picture changed — if we scaled 
to 50,000 companies, submission volume 
increased 10x, and Redis latency started 
showing up in traces — then I'd revisit.
```

---

**Q3: "What happens if Redis goes down completely?"**

```
The service continues to work.
It degrades gracefully.

In the code:
─────────────
try {
    // Check Redis
    ApprovalPolicy cached = redisTemplate
        .opsForValue().get(cacheKey);
    // ...
} catch (RedisConnectionFailureException 
         | QueryTimeoutException e) {
    // Log as WARN
    log.warn("Redis unavailable. 
              Falling back to FeignClient.");
    recordCacheResult("FALLBACK");
    return userOrgFeignClient
        .getApprovalPolicy(companyId);
}

What happens operationally:
─────────────────────────────
FeignClient calls go back to 340/min.
User & Org Service CPU spikes again.
Our submission latency returns to 165ms.

This is degraded performance, not failure.
The service still works.
Expenses can still be submitted.
Approvals still route correctly.

We'd get a Datadog alert because:
  - FALLBACK metric counter spikes
  - User & Org CPU alert fires
  
On-call engineer investigates Redis.
Usually Redis recovers quickly 
(maintenance, brief network partition).
When it recovers, caching 
resumes automatically — 
next MISS after recovery repopulates 
the cache. No manual intervention needed.

This is why the design doc explicitly 
says: "Redis is a performance optimization,
not a hard dependency."
Elena made sure that was clear.
```

---

**Q4: "How did you measure the 87% improvement? What exactly are you comparing?"**

```
Two measurements, both from Datadog.

Measurement 1 — Cache hit rate:
  Custom Micrometer counter:
    approval_policy.cache.result
    tag: result = HIT / MISS / FALLBACK
  
  In Datadog:
    Hit rate = 
      HIT count / (HIT + MISS + FALLBACK count) × 100
    Measured over the first 48 hours post-deploy.
    Result: 87.3%

Measurement 2 — FeignClient call reduction:
  Before: 340 calls/min at peak
    (from http.client.requests metric, 
     filtered by uri and service, 
     before deploy)
  After: ~43 calls/min at peak
    (same metric, same filter, 
     after deploy)
  Reduction: (340-43)/340 = 87.4%
  
  These numbers cross-validate each other —
  87.3% hit rate means ~12.7% miss rate,
  which means 12.7% of 340 = ~43 FeignClient 
  calls per minute. They match.

Measurement 3 — latency improvement:
  http.server.requests metric
    filter: uri = /api/v1/expenses/{id}/submit
    aggregation: p99
  
  Before: ~165ms p99
  After: ~23ms p99
  (for cache hits — the FeignClient 
   call removed from the critical path)

All three measurements are in Datadog.
I compared the same time window 
(Monday 9-10am) one week before 
and one week after the deploy.
Same traffic pattern, same day of week —
controls for normal variation.
```

---

Story 12 complete.

```
What this story demonstrates:
───────────────────────────────

Technical:
  - Cache-Aside pattern — 
    understood and implemented correctly
  - TTL jitter — why it exists, 
    how to implement it
  - Redis failure handling — 
    graceful degradation, not hard dependency
  - Custom Micrometer metrics — 
    how you measure impact
  - Integration testing with 
    Testcontainers (Redis container)

Behavioral:
  - Prepared before the meeting 
    (Datadog data, sketched solution, 
     validated with Priya, 
     checked constraint with Arjun)
  - Proposed clearly and concisely
  - Acknowledged real constraints 
    (invalidation event not available)
  - Listened and incorporated 
    others' input (Priya on FALLBACK)
  - Had a specific position on L1 
    and defended it with reasoning
  - Measured the outcome and 
    shared it with the team

Growth marker:
  In month 5 (N+1 story), 
  you fixed a problem someone 
  else found.
  
  In month 10, you found a problem,
  proposed the solution, got buy-in,
  implemented it, and measured it.
  
  That's the difference.
```

Ready for Story 13 — the cache stampede bug you found in staging, and the first time you solved something significant without asking for help first. Shall I begin?