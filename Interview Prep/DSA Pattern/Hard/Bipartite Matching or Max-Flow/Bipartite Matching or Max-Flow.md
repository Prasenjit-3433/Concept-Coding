# Bipartite Matching / Max-Flow DSA Patterns

*"Problems are infinite, but patterns are finite!"*

---

## The Core Mental Model — Before Any Pattern

Every pattern in this sheet is really the same question asked in different clothing: **how much "stuff" can simultaneously flow from a set of sources to a set of sinks, subject to capacity constraints on the connections in between?** Bipartite matching is the special case where every capacity is 1 and the graph has exactly two layers; general max-flow is the same question with arbitrary capacities and arbitrary graph shape.

**Why this deserves to be its own sheet, not a Graph sub-pattern:** Your Graph sheet's existing patterns (BFS, DFS, Dijkstra, MST, SCC) all answer "what does *a* path look like" or "how are components connected." Matching and flow answer a fundamentally different question — "what is the maximum simultaneous assignment/throughput," which requires an algorithm that can **undo a bad earlier decision** (via augmenting paths) rather than committing to choices greedily or via a single traversal. That undo-and-retry mechanic is the one genuinely new idea this entire topic teaches.

**The augmenting path — the single idea underlying everything below:** Start with nothing matched/flowing. Repeatedly find a path from an unmatched/source node to an unmatched/sink node that can carry additional flow — critically, this path is allowed to walk **backward** along an already-used edge to free up a node that was previously committed elsewhere, then re-route that freed capacity somewhere better. Each such path strictly increases the total matching/flow by at least 1. Stop when no augmenting path exists — at that point, by the **max-flow min-cut theorem**, the current flow is provably maximum.

**The recognition ladder, from simplest to most general:**
1. **Bipartite Matching (unweighted, capacity 1 everywhere):** "assign workers to jobs," "each item matches at most one of a limited set of options" → Kuhn's Algorithm / Hungarian-style augmenting paths.
2. **Bipartite Matching, but you only need to know IF a perfect matching exists, not construct it, and the structure is a specific shape (like intervals or a grid):** → Hall's Marriage Theorem reasoning, or a direct greedy/DP substitute that avoids running full matching at all.
3. **Max-Flow / Min-Cut (weighted, general graph, capacities matter):** "maximum throughput," "minimum number of edges/nodes to remove to disconnect," "maximum number of edge-disjoint or vertex-disjoint paths" → Ford-Fulkerson / Edmonds-Karp / Dinic's.
4. **Min-Cost Max-Flow:** maximize flow first, but among all maximum flows, minimize total cost → assignment problems with actual weights, not just yes/no compatibility.

**The one-line test for choosing between "plain matching" and "full max-flow":**
> If every edge has capacity exactly 1 and the graph is naturally two-sided (left set, right set, edges only between the sides) → Bipartite Matching (Pattern 1–2). If capacities vary, or the graph isn't naturally two-sided, or you need edge/vertex-disjoint path counts → Max-Flow (Pattern 3–4).

---

## Pattern Identification Quick Reference

| Pattern | Trigger |
|---|---|
| Bipartite Matching Fundamentals | "assign each X to a distinct Y", "maximum matching", "each item can pair with a limited set of others" |
| Matching via Hall's Theorem / Structural Shortcuts | perfect matching existence only, special graph shape (intervals, bipartite with structure) — avoid running full matching |
| Max-Flow / Min-Cut Fundamentals | "maximum flow", "minimum cut", "minimum edges/nodes to disconnect", "maximum edge-disjoint/vertex-disjoint paths" |
| Flow Modeling — Reductions to Max-Flow | problem doesn't look like flow at all, but decomposes into sources, sinks, and capacity constraints once modeled |
| Min-Cost Max-Flow | maximize an assignment count first, then minimize total cost among ties — weighted bipartite assignment |

---

## Pattern 1: Bipartite Matching Fundamentals

**Identify:** Two distinct groups of items (workers/jobs, students/schools, rows/columns), each edge represents "these two are compatible," and you need the **maximum number of pairs** you can form such that no item is used twice. Every edge has capacity exactly 1 — there is no weight or throughput to optimize, only a count to maximize.

**Kuhn's Algorithm — the anchor, and the direct generalization of augmenting paths to bipartite matching:** For each left-side node in turn, try to find it a match via DFS: walk to a compatible right-side node; if that right-side node is unmatched, take it; if it's already matched, try to recursively **re-match** its current partner to some other compatible option, freeing it up for the current node. This recursive "bump and retry" is exactly the augmenting-path idea from the Core Mental Model, specialized to unweighted bipartite graphs. Runs in O(V·E) in the naive form — fine for typical interview-scale bipartite graphs.

**LC 1349 (Maximum Students Taking Exam) — the connection to your DP sheet, made explicit:** This problem already appears in your DP sheet's Bitmask DP pattern and your Recursion & Backtracking sheet's Hard Combinations phase, solved via bitmask DP over rows. It is *also* a bipartite matching problem in disguise — treat odd-parity seats and even-parity seats (by checkerboard coloring) as the two sides of a bipartite graph, with edges between seats that would conflict. The maximum independent set in this conflict graph equals `total valid seats - maximum matching` (König's theorem territory). This is flagged specifically as a **cross-pattern recognition exercise**: the same problem, two entirely different correct techniques, and recognizing which one a given constraint size favors (small N → bitmask DP; larger N → matching) is itself a transferable skill.

**LC 1947 (Maximum Compatibility Score Sum) — bipartite matching with weights, a bridge toward Pattern 5:** Also already in your DP sheet's Bitmask DP pattern. This is bipartite matching where you want to maximize total *compatibility score*, not just the count of pairs — technically a small-scale assignment problem, solvable by bitmask DP at this problem's scale (N ≤ 8) or by the Hungarian Algorithm / Min-Cost Max-Flow at larger scale (Pattern 5). Included here specifically to show that "matching" and "assignment" are the same family at different weight-richness levels.

**Job Assignment Problem (classic, GFG) — the plain, undisguised anchor problem:** N workers, N jobs, a compatibility (or cost) matrix — find the maximum matching, or the minimum-cost perfect matching if every worker must be assigned. The unweighted existence version is pure Kuhn's; the weighted version is where Pattern 5's Hungarian Algorithm becomes necessary instead.

| # | Problem | Key Concept |
|---|---|---|
| 1 | Theory: Kuhn's Algorithm (Bipartite Matching via Augmenting Paths) | DFS-based augmenting path — recursively re-match an occupied right-side node to free it up for the current left-side node |
| 2 | GFG: Job Assignment Problem (unweighted / existence version) | Direct, undisguised application of Kuhn's — maximum matching between workers and jobs |
| 3 | LC 1349. Maximum Students Taking Exam *(cross-ref: DP sheet, Bitmask DP; Recursion & Backtracking sheet, Phase 5)* | Same problem, two valid techniques — bitmask DP at small N, bipartite matching (checkerboard coloring + König's theorem) at larger N |
| 4 | LC 1947. Maximum Compatibility Score Sum *(cross-ref: DP sheet, Bitmask DP)* | Weighted bipartite matching / small-scale assignment problem — bridge toward Pattern 5's Hungarian Algorithm |

---

## Pattern 2: Matching via Hall's Theorem / Structural Shortcuts

**Identify:** You only need to answer **"does a perfect matching exist"** (or "what's the maximum matching size"), not construct the matching itself, and the bipartite graph has enough structure (interval-based compatibility, a grid, a specific combinatorial shape) that running full Kuhn's algorithm is more machinery than the problem actually needs.

**Hall's Marriage Theorem — the existence check, stated once:** A perfect matching saturating the left side exists if and only if **every subset of left-side nodes has a combined neighborhood (union of all their compatible right-side nodes) at least as large as the subset itself.** This is a *feasibility* criterion; it doesn't directly hand you the matching, but it's frequently faster to reason about the *bound* it implies than to run matching machinery, especially in proof-style ("show that such an assignment always exists") interview questions.

**Why this pattern exists separately from Pattern 1:** Pattern 1 is "run the algorithm." This pattern is "recognize when the structure of the problem lets you skip running the algorithm entirely," either because Hall's condition gives a closed-form existence answer, or because the compatibility structure (e.g., intervals, where compatibility is "does this interval overlap this slot") reduces to a much simpler greedy or two-pointer technique you already know from the Intervals or Greedy sheets.

**Course Schedule-style capacity-matching-via-Hall's reasoning:** When a problem asks "can every one of these N groups be assigned a distinct valid resource from their allowed set," and the allowed sets have a nice structure (nested, interval-based, or a nested containment chain), checking Hall's condition on that structure directly (often via a simple sort-and-greedy scan) avoids building a matching graph and running Kuhn's algorithm at all.

**Task Scheduling with Deadlines-as-Matching (cross-ref: Greedy sheet, Job Sequencing Problem):** "Assign each job to a distinct time slot on or before its deadline" is literally bipartite matching between jobs and time slots — but your Greedy sheet already solves this optimally via sort-by-profit-descending plus greedy latest-available-slot assignment, in O(n log n), without ever building a bipartite graph. This is the clearest example in this entire sheet of "recognize the matching structure, then recognize you don't need matching machinery to solve it" — flagged here specifically so the connection is visible, not to duplicate the Greedy sheet's treatment.

| # | Problem | Key Concept |
|---|---|---|
| 1 | Theory: Hall's Marriage Theorem | Perfect matching exists iff every subset of one side's combined neighborhood is at least as large as the subset — existence check, not construction |
| 2 | GFG: Job Sequencing Problem *(cross-ref: Greedy sheet, Pattern 3)* | Matching-shaped problem solvable by greedy sort + latest-slot assignment — no matching graph needed at all |
| 3 | Bipartite Matching on Interval-Structured Compatibility | When compatibility is interval-based, a two-pointer or interval-greedy technique often replaces full Kuhn's |

---

## Pattern 3: Max-Flow / Min-Cut Fundamentals

**Identify:** Edges have **capacities** (not just a binary compatible/incompatible), the graph is not necessarily two-sided, and the question is about maximum simultaneous throughput from a designated source to a designated sink, or the minimum "cost" (in edges or nodes removed) to sever all paths between them.

**Ford-Fulkerson / Edmonds-Karp — the direct generalization of Kuhn's augmenting paths to weighted, general graphs:** Repeatedly find *any* path from source to sink with remaining capacity (BFS for Edmonds-Karp specifically, which guarantees polynomial time by always finding the *shortest* augmenting path), push flow equal to the path's bottleneck capacity, and update a **residual graph** — every forward edge gets its capacity reduced by the pushed amount, and a **backward edge** of equal capacity is added, which is what allows a later augmenting path to "undo" an earlier suboptimal routing decision. Stop when no augmenting path exists in the residual graph.

**The Max-Flow Min-Cut Theorem — why "stop when no augmenting path exists" is provably optimal, not just a heuristic stopping point:** The maximum possible flow from source to sink always exactly equals the minimum total capacity of edges that, if removed, would disconnect the source from the sink (a "cut"). This is the theoretical justification for the whole algorithm family, and it's also directly useful: many problems that *sound* like they're asking for a minimum cut (minimum number of edges to remove to disconnect two nodes) are most easily solved by computing max-flow instead and using the theorem to translate the answer.

**LC 1591 / edge-disjoint and vertex-disjoint path counting — a direct corollary of max-flow, not a separate algorithm:** "What is the maximum number of edge-disjoint paths from `s` to `t`" is exactly max-flow with every edge capacity set to 1. "Vertex-disjoint" instead of "edge-disjoint" is the same question after a standard graph transformation: split every node `v` into `v_in` and `v_out` connected by a capacity-1 edge, forcing any flow through that node to use up its one unit of "node capacity" — this vertex-splitting trick is worth knowing as a named transformation, since it recurs any time a flow problem's constraint is on *nodes* rather than *edges*.

**LC 1102 (Path With Maximum Minimum Value) — a useful contrast, not a flow problem:** Already correctly placed in your Binary Search sheet (via binary search on the answer) and cross-referenced in your Heap sheet as a max-heap Dijkstra variant. It is *not* a flow problem despite superficially resembling one ("maximize the minimum capacity along a path") — flagged here explicitly as a **near-miss** worth naming, since "maximize the bottleneck along a single path" and "maximize total simultaneous throughput across all possible paths" sound similar but are entirely different questions; only the second is flow.

| # | Problem | Key Concept |
|---|---|---|
| 1 | Theory: Ford-Fulkerson / Edmonds-Karp | Repeatedly find an augmenting path via BFS in the residual graph, push bottleneck flow, add backward edges to allow future undoing |
| 2 | Theory: Max-Flow Min-Cut Theorem | Maximum flow equals minimum cut capacity — the reason the augmenting-path stopping condition is provably optimal |
| 3 | Maximum Number of Edge-Disjoint Paths (GFG / CSES: Download Speed) | Direct max-flow with every edge capacity 1 |
| 4 | Maximum Number of Vertex-Disjoint Paths | Node-splitting transformation (`v_in` → `v_out`, capacity 1) reduces to the edge-disjoint case above |
| 5 | Minimum Cut / Minimum Edges to Disconnect Two Nodes (GFG / CSES: Police Chase) | Compute max-flow, invoke the min-cut theorem directly for the answer |
| — | LC 1102. Path With Maximum Minimum Value *(cross-ref: Binary Search sheet Pattern 5; Heap sheet Pattern 1)* | Near-miss — sounds like flow, is actually single-path bottleneck optimization, not simultaneous throughput |

---

## Pattern 4: Flow Modeling — Reductions to Max-Flow

**Identify:** The problem gives no hint that it's a flow problem at all — no explicit graph, no mention of "capacity" or "flow." The actual skill in this pattern is **recognizing that a problem decomposes into sources, sinks, and capacity-constrained intermediate steps**, and then building that graph yourself before any algorithm from Pattern 3 can even be applied. This is the single hardest recognition skill in this entire sheet, closer in spirit to the Math & Geometry sheet's "translate a spatial claim into algebra" instinct than to anything mechanical.

**The general modeling recipe:**
1. Identify what's being "produced" (a source) and what's being "consumed" (a sink) — sometimes these are literal, sometimes you must invent a single super-source connected to many real sources, and a single super-sink connected to many real sinks, to collapse a multi-source/multi-sink problem into the single-source/single-sink shape every flow algorithm expects.
2. Identify every constraint that caps how much can pass through a given connection or entity — that constraint becomes an edge capacity.
3. If a *node itself* (not an edge) has a capacity, apply the vertex-splitting trick from Pattern 3.

**LC 1349 revisited (cross-ref) — reduction-as-bipartite-matching, already covered in Pattern 1:** Worth restating here as the template for this pattern's mental move: a seating/conflict problem was reduced to a *matching* graph by identifying which entities needed to be "consumed" (seats) and which pairs conflicted (edges). Every problem in this pattern applies that same reduction instinct, but to general flow rather than 1-to-1 matching.

**Project Selection / Maximum Profit with Dependencies (the "closure problem," classic reduction):** Given projects with profits (possibly negative) and prerequisite dependencies, select a subset maximizing total profit such that every selected project's prerequisites are also selected. Model as: super-source connects to every positive-profit project with capacity equal to its profit; every negative-profit project connects to the super-sink with capacity equal to the absolute value of its (negative) profit; a directed edge of infinite capacity from each project to its prerequisites enforces "if you take this, you must also take its prerequisite" (an infinite-capacity edge can never be part of a finite min-cut, forcing both endpoints to end up on the same side of the cut). The answer is `total positive profit - min cut`. This is the single most valuable reduction to know in the entire sheet, because the "infinite-capacity edge enforces a must-go-together constraint" trick recurs across many disguised flow problems.

**Bipartite matching as a special case of flow, made explicit (tying Pattern 1 back into this sheet's general framework):** Every bipartite matching problem from Pattern 1 can *also* be solved by building a flow network — super-source to every left node (capacity 1), every compatible edge between sides (capacity 1), every right node to a super-sink (capacity 1) — and running max-flow. Kuhn's algorithm is simply a specialized, faster implementation of exactly this flow network's max-flow computation. Worth knowing this equivalence explicitly: it means everything in Pattern 1 is not a separate algorithm family, it's max-flow specialized to an all-capacity-1 bipartite shape.

| # | Problem | Key Concept |
|---|---|---|
| 1 | Theory: Super-Source / Super-Sink Construction | Collapse multiple real sources or sinks into one, so single-source/single-sink algorithms apply |
| 2 | Project Selection Problem / Maximum Profit Closure (classic reduction) | Infinite-capacity edges enforce "must be selected together"; answer = total positive profit minus min cut |
| 3 | Theory: Bipartite Matching as a Special Case of Max-Flow | Super-source/sink + capacity-1 edges reduces Pattern 1 entirely to Pattern 3's machinery — Kuhn's is a specialized fast path, not a separate algorithm |

---

## Pattern 5: Min-Cost Max-Flow

**Identify:** You need the **maximum matching or maximum flow**, but among all ways of achieving that maximum, you additionally need the one with **minimum total cost** (or maximum total weight). This is the natural endpoint of the progression from Pattern 1 (existence/count only) through Pattern 3 (capacity-aware throughput) — now every edge has both a capacity *and* a cost per unit of flow through it.

**The Hungarian Algorithm — the specialized version for weighted bipartite assignment specifically:** When the problem is exactly "N workers, N jobs, a full cost matrix, assign every worker to exactly one job minimizing total cost," the Hungarian Algorithm solves this in O(N³) without needing general min-cost-flow machinery — it's worth knowing as the named, specialized tool for this exact shape, the same way Dijkstra is a specialized, faster tool than Bellman-Ford for the non-negative-weight case.

**General Min-Cost Max-Flow (successive shortest augmenting paths, weighted by cost instead of unweighted BFS):** For problems that don't fit the clean N-workers-N-jobs shape (unequal sides, additional capacity constraints beyond 1-per-edge, multi-stage assignment), replace Edmonds-Karp's BFS-for-shortest-path with a shortest-path algorithm that accounts for edge *cost* (Bellman-Ford or SPFA, since cost-augmenting residual edges can be negative) — find the cheapest augmenting path first, always, and repeat until max flow is reached. This is a direct generalization of Pattern 3's algorithm, with "shortest by hop count" replaced by "shortest by cost."

**LC 1947 revisited (cross-ref, closing the loop from Pattern 1):** At small N this is bitmask DP; at larger N, this is exactly the Hungarian Algorithm's target shape — maximize total compatibility (equivalently, minimize total negative-compatibility "cost") over a complete bipartite assignment. Flagged here as the natural place this problem's *general* solution lives, with Pattern 1 covering only its small-N special case.

| # | Problem | Key Concept |
|---|---|---|
| 1 | Theory: Hungarian Algorithm (Assignment Problem) | O(N³) specialized minimum-cost perfect bipartite matching — workers to jobs, full cost matrix |
| 2 | Theory: General Min-Cost Max-Flow (SPFA/Bellman-Ford augmenting paths) | Generalizes Ford-Fulkerson — find the cheapest augmenting path each iteration instead of just any/shortest-by-hops one |
| 3 | LC 1947. Maximum Compatibility Score Sum, general-N version *(cross-ref: Pattern 1)* | Large-N version of the same problem — Hungarian Algorithm territory once N exceeds bitmask DP's reach |

---

## Final Summary

| Pattern | Problems | Core Mechanism |
|---|---|---|
| Bipartite Matching Fundamentals | 4 (2 cross-ref) | Kuhn's algorithm — DFS augmenting paths that recursively re-match occupied nodes |
| Matching via Hall's Theorem / Structural Shortcuts | 3 (1 cross-ref) | Existence-only reasoning, or structural greedy substitutes that avoid running matching at all |
| Max-Flow / Min-Cut Fundamentals | 5 (1 cross-ref) | Ford-Fulkerson/Edmonds-Karp augmenting paths in a residual graph; min-cut theorem for disconnection questions |
| Flow Modeling — Reductions to Max-Flow | 3 | Recognizing sources/sinks/capacities in a disguised problem; infinite-capacity edges enforce grouping constraints |
| Min-Cost Max-Flow | 3 (1 cross-ref) | Hungarian Algorithm for clean assignment shape; general cost-augmenting paths otherwise |
| **Total** | **~18 problems + theory items** | |

---

## How to Use This Sheet

**Pattern 1 is mandatory first, and Kuhn's algorithm must be implementable cold before anything else here makes sense.** Every later pattern either specializes it (Pattern 2), generalizes it (Pattern 3), or reduces back to it (Pattern 4's explicit equivalence). If the "DFS, try to re-match the occupant" mechanic isn't automatic, Ford-Fulkerson in Pattern 3 will look like unrelated new machinery instead of "the same idea, with capacities."

**Pattern 2 is a recognition drill, not a mechanics drill — treat it the way the Sliding Window sheet treats its Monotonicity section.** The actual skill is knowing when *not* to reach for matching machinery at all, because the Greedy or Intervals sheet's tools already solve the same underlying question faster and more simply. Cross-reading the Job Sequencing Problem entry here alongside its full treatment in the Greedy sheet is the point.

**Pattern 3 requires Pattern 1 conceptually solid, even though the code is different.** The residual-graph "backward edge undoes a bad decision" idea is exactly Kuhn's "re-match the occupant" idea, generalized past capacity-1 — say that connection out loud before writing Ford-Fulkerson from scratch.

**Pattern 4 is the hardest and highest-value pattern in this entire sheet, and should be done last, deliberately.** The Project Selection reduction (infinite-capacity edges enforcing "must be selected together") is the single most reusable modeling trick here — once it clicks, a wide class of "select a subset under dependency and profit constraints" problems stop looking like search problems and start looking like flow problems on sight.

**Pattern 5 only after Pattern 3 is solid, and only if your interview prep timeline has room for it.** Worth an honest note here, more than anywhere else in this sheet: min-cost max-flow is genuinely rare at standard FAANG interview loops — it shows up occasionally in "senior/staff" system-design-adjacent algorithmic rounds or in competitive programming, but Patterns 1–4 cover the overwhelming majority of what "matching/flow" questions actually get asked in practice. If time is tight, Pattern 5's theory items are safe to deprioritize relative to everything above them.

**A broader honesty note for this sheet, same as LCA and Meet-in-the-Middle:** Bipartite matching shows up more often than pure max-flow in interviews (usually disguised as a DP or greedy problem, as LC 1349/1947 demonstrate), while general max-flow and min-cost flow are closer to "good to recognize exists" than "expect to be asked to implement Dinic's from scratch." Calibrate your practice time accordingly — Pattern 1 and Pattern 4's modeling instinct are worth real drilling; Pattern 3's algorithm internals and Pattern 5 are worth understanding, not memorizing cold.

---
