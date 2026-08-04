# Intervals DSA Patterns

*"Problems are infinite, but patterns are finite!"*

---

## The Core Mental Model — Before Any Pattern

Every interval problem starts with the same question: **sort by what?** Almost all the difficulty in this topic collapses once you pick the right sort key — usually start time, sometimes end time, occasionally both separately. After sorting, the actual logic is almost always a single linear pass.

**The three sort choices, and what each unlocks:**
- **Sort by start time** → merging, insertion, most greedy selection problems. This is the default until a specific reason says otherwise.
- **Sort by end time** → problems where you're *selecting* a subset of intervals and want to keep the option space as open as possible for whatever comes next (classic exchange argument — same one from your Greedy sheet's Pattern 3).
- **Sort starts and ends *separately*** → sweep-line problems, where you care about "how many intervals are active at once," not which specific intervals overlap which.

**The one-line test for which pattern you're in:**
> Are you *combining/selecting* intervals (→ Merge or Greedy Selection)? Are you asking *how many are active at a point in time* (→ Sweep Line)? Are you *maximizing weighted value* under a non-overlap constraint (→ Weighted Interval Scheduling — this is DP wearing an interval costume)?

---

## Pattern Identification Quick Reference

| Pattern | Trigger |
|---|---|
| Interval Basics & Merge Fundamentals | "merge overlapping", "can attend all meetings", "summarize ranges" |
| Insert, Remove & Prune Intervals | "insert a new interval", "remove/split an interval", "remove covered intervals" |
| Greedy Interval Selection | "maximum non-overlapping", "minimum arrows/removals", "partition into segments" |
| Two-Pointer Interval Intersection | "intersection of two interval lists", "common free time between two schedules" |
| Sweep Line / Event-Based Counting | "minimum rooms/platforms", "max concurrent", "capacity over time", "population at year Y" |
| Weighted Interval Scheduling | "maximum profit", "at most k non-overlapping", "weighted job scheduling" |

---

## Pattern 1: Interval Basics & Merge Fundamentals

**Identify:** You're given a list of intervals and need to combine the ones that overlap into a canonical, non-overlapping form — or verify none of them overlap at all. Sort by **start time** first, always. Once sorted, one linear pass is enough: compare each interval to the last one you kept, and merge if `current.start <= kept.end`.

**Why sort by start and not end here:** Merging requires processing intervals in the order they *begin*, because you need to know whether the next interval starts before the previous one you're building has finished. Sorting by end would scatter intervals that should merge together across the pass.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 228. Summary Ranges | Sorted input already — single pass grouping consecutive numbers into ranges |
| 2 | LC 252. Meeting Rooms | Sort by start — if any `interval[i].start < interval[i-1].end`, cannot attend all |
| 3 | LC 495. Teemo Attacking | Fixed-duration merge — sum non-overlapping poison windows without building the merged list explicitly |
| 4 | LC 56. Merge Intervals | The anchor problem — sort by start, merge while `current.start <= merged.end` |

---

## Pattern 2: Insert, Remove & Prune Intervals

**Identify:** The interval list is already sorted and non-overlapping, and you need to modify it — insert a new interval and re-merge, remove a specific range (possibly splitting an existing interval in two), or discard intervals fully contained in others. Because the list starts sorted, you almost never need to re-sort from scratch — one linear scan does the job.

**LC 57's three-zone scan:** Since the list is already sorted, walk it in three phases: intervals ending entirely before the new one starts (copy as-is), intervals overlapping the new one (merge into a single growing interval), intervals starting entirely after the new one ends (copy as-is). No full re-sort needed — this is O(n), not O(n log n).

**LC 1272's split case — the one new mechanic in this pattern:** Removing a range can leave *two* pieces of a single original interval if the removed range sits strictly inside it. Every other operation in this pattern produces at most one output piece per input interval; this is the one that can produce two.

**LC 1288's sort-by-start-then-end-descending trick:** To detect "interval B is fully covered by interval A," sort by start ascending, and for ties, by end **descending**. This ordering guarantees that if a later interval could possibly be covered by an earlier one, the earlier one is checked first — a single running `maxEnd` then does all the covering detection.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 57. Insert Interval | Three-zone linear scan — before / merging / after, no re-sort needed |
| 2 | LC 1272. Remove Interval | Same scan, but the middle zone can produce a split (two pieces) instead of a merge |
| 3 | LC 1288. Remove Covered Intervals | Sort start asc, end desc — track running max end to detect coverage |

---

## Pattern 3: Greedy Interval Selection

**Identify:** You're choosing a subset of intervals under a constraint — maximize how many you keep, minimize how many you remove, or minimize the number of "markers" needed to hit every interval at least once. Sort by **end time**, not start. The exchange argument: whichever interval ends earliest leaves the most room for everything that comes after, so it's always safe to greedily keep it.

**Why end time and not start time — the thing that trips people up:** Sorting by start time optimizes for "which interval begins soonest," which says nothing about how much room it leaves you afterward. Sorting by end time directly optimizes for "which choice keeps my options most open going forward" — that's the actual goal in every problem in this pattern.

**LC 452 as the sibling of LC 435 — same technique, different framing:** Non-overlapping Intervals asks "how many must I *remove* to eliminate all overlaps." Burst Balloons asks "how many *points* (arrows) do I need so every interval contains at least one." Both reduce to the identical greedy: sort by end, and every time the current interval doesn't overlap the last "kept" reference point, that's a new removal (LC 435) or a new arrow (LC 452).

**LC 763's disguise — no explicit intervals given:** Partition Labels doesn't hand you an interval list, but "each character's first-to-last occurrence" *is* an implicit interval. The greedy: track the last occurrence of the current window's characters as you scan; when your scan position reaches that running max, you've found a valid partition boundary. This is the exact same "extend reach, cut when exhausted" instinct as your Greedy sheet's Pattern 2 (Jump Game family) — just applied to implicit rather than explicit intervals.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 435. Non-overlapping Intervals *(cross-ref: Greedy sheet, Pattern 3)* | Sort by end — greedily keep earliest-ending, count removals |
| 2 | LC 452. Minimum Number of Arrows to Burst Balloons | Same sort-by-end greedy — count new arrows instead of removals |
| 3 | LC 763. Partition Labels | Implicit intervals via last-occurrence tracking — extend and cut when window closes |

---

## Pattern 4: Two-Pointer Interval Intersection

**Identify:** You have **two separate, independently sorted** interval lists and need to find where they overlap, or find the first gap that satisfies some constraint across both. Because both lists are already sorted by start time, a single two-pointer sweep — advancing whichever list's current interval ends first — finds every intersection in one O(m+n) pass, no heap or sorting needed.

**The core invariant:** At each step, the interval that ends earlier between the two pointers can never overlap anything *later* in the other list (everything later starts even further along). So once you've checked it against the current interval on the other side, you're done with it — advance that pointer.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 986. Interval List Intersections | Two pointers — overlap = `[max(starts), min(ends)]`; advance the pointer with the smaller end |
| 2 | LC 1229. Meeting Scheduler | Two pointers — same overlap check, but looking for the first gap ≥ required duration |

---

## Pattern 5: Sweep Line / Event-Based Counting

**Identify:** The question is "how many intervals are simultaneously active at some point in time" — minimum rooms needed, minimum platforms, current population, current bookings. You do **not** care which specific intervals overlap which; you only care about the *count* at each moment. The technique: convert every interval into two events (`+1` at start, `-1` at end), sort all events by time, sweep left to right accumulating a running counter, and track its maximum (or answer queries against it).

**Why this is a fundamentally different tool from Pattern 1's merge, even though both start with "sort intervals":** Merge Intervals cares about the *boundaries* of combined ranges. Sweep Line cares about *simultaneous overlap count* at every instant. Confusing the two is the most common conceptual error in this entire topic — if the answer involves a number of rooms/platforms/simultaneous events rather than a set of merged ranges, you're in this pattern, not Pattern 1.

**The min-heap variant (LC 253, LC 2406):** Instead of separate start/end event arrays, sort intervals by start and push end-times onto a min-heap as you go. If the new interval's start is ≥ the heap's smallest end, that room/group is free — pop and reuse it. Otherwise push a new one. The heap size at any point is the answer.

**LC 1854's difference-array version — the simplest possible entry point to this pattern:** For each person, `+1` at their birth year and `-1` the year *after* their death year. Prefix-summing this difference array gives the population at every year directly — no explicit event sorting needed since years are small and bounded.

**LC 732's TreeMap sweep — online version of the same idea:** My Calendar III needs the answer *after every single booking*, not just once at the end. A `TreeMap<time, delta>` storing `+1`/`-1` events, walked in sorted key order on every query, tracks the running max overlap — same mechanism as the offline sweep, just re-run incrementally. This is the natural hard-tier extension of Calendar I/II (cross-ref: BST sheet, Pattern 8) once "does this overlap anything" becomes "how many things does this overlap."

**LC 759 — sweep across multiple people's schedules simultaneously:** Employee Free Time is a Pattern 1 merge (flatten everyone's intervals, sort, merge) immediately followed by a Pattern-5-flavored scan: the gaps *between* consecutive merged intervals are the answer. It's the two patterns chained, not a new mechanism.

| # | Problem | Key Concept |
|---|---|---|
| 1 | GFG: Minimum Platforms *(cross-ref: Greedy sheet, Pattern 3)* | Separate sorted start/end arrays, two-pointer sweep |
| 2 | LC 1854. Maximum Population Year | Difference array on birth/death events — simplest intro to the technique |
| 3 | LC 253. Meeting Rooms II | Min-heap of end times — reuse a room if its meeting has ended |
| 4 | LC 2406. Divide Intervals Into Minimum Number of Groups | Identical technique to Meeting Rooms II, different framing |
| 5 | LC 1094. Car Pooling | Difference array on passenger count over trip ranges — check capacity never exceeded |
| 6 | LC 352. Data Stream as Disjoint Intervals | Online merge via TreeMap — insert-and-merge on every call, not a batch operation |
| 7 | LC 732. My Calendar III | TreeMap of +1/-1 events — running max overlap queried after every booking |
| 8 | LC 759. Employee Free Time | Merge everyone's intervals (Pattern 1), then scan gaps between merged results |
| — | LC 729, LC 731 *(cross-ref)* | BST sheet, Pattern 8 — My Calendar I/II, simpler overlap-only versions of LC 732 |
| — | LC 2402, LC 218 *(cross-ref)* | Heap sheet, Pattern 5 — Meeting Rooms III, Skyline — sweep line + heap combined |

---

## Pattern 6: Weighted Interval Scheduling (DP + Binary Search)

**Identify:** Each interval now carries a **value**, and you're maximizing total value subject to a non-overlap constraint — optionally with a cap on how many intervals you can pick. This is the one pattern in this entire sheet that is **not greedy** — picking the highest-value interval first can block two lower-value intervals worth more combined, so the exchange argument that powers Pattern 3 does not apply here. This is Interval DP's cousin, expressed as 1D DP with binary search replacing a linear scan for speed.

**The core recurrence — memorize this shape:** Sort by end time. `dp[i]` = best value achievable using only the first `i` intervals (by end time). For each interval, you either skip it (`dp[i-1]`) or take it (`interval[i].value + dp[j]`, where `j` is the largest index whose end time is ≤ this interval's start time). Finding `j` by linear scan is O(n²) total; finding it by **binary search on end times** brings it to O(n log n) — this is exactly why the pattern's name pairs DP with binary search.

**Why this was flagged as a "DP trap" in the Greedy sheet, and belongs here instead:** LC 1235 *looks* greedy — "pick the most profitable non-overlapping jobs" sounds like Pattern 3's sort-by-end-and-select. But value isn't tied to duration, so the greedy exchange argument breaks: a short, low-value job ending early doesn't necessarily "leave more room" in any useful sense once profit is unequal across jobs. The fix is DP over sorted end times, not a direct greedy pick.

**LC 1751's k-dimension extension:** Same recurrence, but `dp[i][k]` also tracks how many events you've attended so far, since you're capped at `k` total. **LC 2054** is the special case `k = 2`, solvable more simply — sort by start, then for each event binary-search the best value achievable entirely before it starts, track a running prefix max.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 1235. Maximum Profit in Job Scheduling | Sort by end + binary search for `dp[j]` — the anchor problem for this pattern |
| 2 | LC 2054. Two Best Non-Overlapping Events | Special case k=2 — sort by start, binary search + running prefix max |
| 3 | LC 1751. Maximum Number of Events That Can Be Attended II | General case, k events — `dp[i][k]` with binary search per transition |

---

## Final Summary

| Pattern | Problems (new) | Core Mechanism |
|---|---|---|
| Interval Basics & Merge Fundamentals | 4 | Sort by start, single-pass merge |
| Insert, Remove & Prune Intervals | 3 | Already-sorted list, linear scan, no re-sort |
| Greedy Interval Selection | 3 (1 cross-ref) | Sort by end — exchange argument, keep max options open |
| Two-Pointer Interval Intersection | 2 | Two independently sorted lists, advance the earlier-ending pointer |
| Sweep Line / Event-Based Counting | 8 (4 cross-ref) | Convert to +1/-1 events, sweep, track running count |
| Weighted Interval Scheduling | 3 | Sort by end + binary search inside a DP recurrence — not greedy |
| **Total** | **~23 new + 5 cross-referenced** | |

---

## How to Use This Sheet

**Pattern 1 is non-negotiable first.** The start-vs-end sort distinction that drives every later pattern only becomes intuitive after LC 56 is completely automatic.

**Pattern 2 depends only on Pattern 1.** These are linear-scan variations on the same merge logic — do them right after, while the merge mental model is fresh.

**Pattern 3 requires you to *unlearn* Pattern 1's default.** The instinct to sort by start is strong after Patterns 1–2; Pattern 3 is where you deliberately override it. Say the exchange argument out loud before each problem: "keeping the earliest-ending option leaves me the most room for what's next."

**Pattern 4 can run in parallel with Pattern 3** — no shared dependency, just a different two-list setup.

**Pattern 5 is the largest and most cross-referenced pattern here — expect that.** It's genuinely the highest-frequency interval pattern at FAANG-level interviews (Meeting Rooms variants are extremely common), which is why it absorbs contributions from three other sheets. Do LC 1854 first as the gentlest possible entry point before touching the heap or TreeMap variants.

**Pattern 6 last, and only after your DP sheet's 1D DP and Binary Search sheet's Pattern 1 are both solid.** This pattern is real proof that "looks greedy" and "is greedy" are different claims — treat LC 1235 as the moment that lesson gets reinforced with a concrete counterexample, not just an abstract warning.