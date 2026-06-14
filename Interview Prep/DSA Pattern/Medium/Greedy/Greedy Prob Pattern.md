# Greedy DSA Patterns

*"Problems are infinite, but patterns are finite!"*

---

## The Core Mental Model — Before Any Pattern

Greedy works when a **locally optimal choice at each step leads to a globally optimal solution**. The proof is always an exchange argument: *"if you deviate from the greedy choice, you cannot do better."*

**The three questions to ask before deciding greedy:**
1. Can I define a clear ordering or priority at each step?
2. Is making the locally best choice now safe — does it never foreclose a better global option?
3. Is there a proof by exchange argument? (If your greedy is wrong, you'll usually find a counterexample quickly.)

**When NOT to use greedy:**
> If the problem asks "how many ways" or "what is the optimal value over all subsets/combinations" with overlapping subproblems — that is DP, not greedy. The clearest greedy-vs-DP signal: if you ever need to undo a choice you made earlier, the problem is DP.

**Greedy vs Binary Search on Answer:**
> Some problems that look greedy ("minimize the maximum") are actually Binary Search on Answer with a greedy feasibility check inside. If your greedy simulation needs to verify a parameter rather than directly produce the answer, it's Pattern 4 or 5 of the Binary Search sheet — not greedy.

---

## Pattern Identification Quick Reference

| Pattern | Trigger |
|---|---|
| Selection & Assignment Greedy | "assign", "match", "pair", "distribute" — sort both sides, greedily satisfy |
| Jump Game & Interval Coverage | "minimum jumps", "reach the end", "cover all points", "minimum taps/intervals" |
| Interval Scheduling & Sorting | "non-overlapping", "maximum events", "sequence tasks by deadline", "minimum platforms" |
| Array & String Greedy | "maximum number by swap", "remove k digits", "minimum adds to balance" |
| Greedy + Data Structure | "greedy choice requires dynamic best candidate" — heap or ordered set inside the loop |
| Hard Greedy | exchange argument non-obvious, two-variable sort, contribution counting |

---

## Pattern 1: Selection & Assignment Greedy

**Identify:** You have two groups of elements — children and cookies, workers and jobs, cities and costs — and you need to optimally match or assign them. Sorting one or both groups and greedily satisfying from one direction is always the technique. The exchange argument: if you skip a satisfying element now, using a larger one later can only waste resources.

**Fractional Knapsack** lives here because it is the purest "sort by ratio, take greedily" problem. It is also an important contrast with 0/1 Knapsack — fractional is greedy; 0/1 is DP. Know why.

**LC 179 (Largest Number)** lives here because the recognition signal is sorting — you need a custom comparator to define the right ordering. The exchange argument (`a+b vs b+a`) is the *proof* of why that comparator is correct, not the recognition signal itself. Exchange-argument-as-proof appears in Pattern 1, 3, and 6 alike; what distinguishes Pattern 6 is that the greedy rule itself is non-obvious. For LC 179, the rule is immediately statable: "sort such that the larger concatenation comes first." The difficulty is implementation, not recognition.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 860. Lemonade Change | Greedy warm-up — prefer using $10 before $5 for $15 change; exchange argument is immediate |
| 2 | LC 455. Assign Cookies | Sort both arrays — smallest satisfying cookie assigned to smallest unsatisfied child |
| 3 | GFG: Fractional Knapsack | Sort by value/weight ratio descending — take greedily until capacity; contrast with 0/1 Knapsack (DP) |
| 4 | LC 1029. Two City Scheduling | Sort by cost difference `(costA - costB)` — send first half to A, second half to B |
| 5 | LC 179. Largest Number | Custom comparator sort — compare `a+b` vs `b+a` as strings; rule is statable immediately, proof is the exchange argument |

---

## Pattern 2: Jump Game & Interval Coverage

**Identify:** You need to cover a range (array indices, a garden, a number line) using jumps or intervals with minimum count, or check if the end is reachable at all. The greedy insight: always extend coverage as far right as possible with the current resource.

**The unifying mental model:** Track `currentReach` (the farthest point reachable so far). At each position within reach, ask "how far can I get from here?" Update `maxReach`. When you exhaust `currentReach`, you must use one more jump — set `currentReach = maxReach`.

**LC 1326 is Jump Game II in disguise** — tap coverage intervals replace jump ranges. Recognizing this transfer is the pattern identification skill.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 55. Jump Game | Track max reachable index — return false if current position exceeds reach |
| 2 | LC 45. Jump Game II | Greedy BFS layers — extend reach at each step, increment jumps when layer exhausted |
| 3 | LC 1326. Minimum Number of Taps to Open to Water a Garden | Convert taps to intervals, apply Jump Game II greedy — minimum intervals to cover [0, n] |
| 4 | LC 1024. Video Stitching | Same interval coverage greedy as LC 1326 — minimum clips to cover [0, T] |

---

## Pattern 3: Interval Scheduling & Sorting

**Identify:** You have a set of intervals (meetings, events, jobs) and need to select or sequence them optimally. The critical insight: **sort by end time** to maximize the number of non-overlapping intervals selected. Sort by **start time** for sweep-based problems (minimum platforms). Sort by **deadline** for job sequencing.

**The exchange argument for sort-by-end-time:** If you ever choose an interval that ends later when one that ends earlier is available and non-overlapping, you can always swap — the later-ending one never gives you strictly more options afterward.

| # | Problem | Key Concept |
|---|---|---|
| 1 | GFG: Minimum Platforms | Sort arrivals and departures separately — sweep to find max simultaneous trains |
| 2 | LC 435. Non-overlapping Intervals | Sort by end time — greedily keep intervals ending earliest; count removals |
| 3 | GFG: Job Sequencing Problem | Sort by profit descending — assign to latest available slot before deadline |
| 4 | LC 826. Most Profit Assigning Work | Sort jobs by difficulty + workers by skill — two-pointer greedy with running max profit |
| 5 | LC 406. Queue Reconstruction by Height | Sort by height desc (then index asc) — insert each person at their index position |
| 6 | LC 2136. Earliest Possible Day of Full Bloom | Sort by grow time descending — plant longest-growing flowers first; minimizes total wait |
| 7 | LC 1353. Maximum Number of Events That Can Be Attended | Sort by start + min-heap of end times — each day attend soonest-ending available event *(cross-ref: Heap Pattern 5)* |
| 8 | LC 621. Task Scheduler | Max-heap on frequencies + cooldown math — always schedule most frequent available task *(cross-ref: Heap Pattern 1)* |

---

## Pattern 4: Array & String Greedy

**Identify:** Problems operating on a single array or string where a greedy scan — often two-pass, or with a running state variable — produces the answer. These do not require sorting or a dynamic data structure. The technique is recognizing what invariant to maintain during the scan.

**Two-pass greedy (Candy):** Some constraints flow left-to-right (each child gets more than left neighbor if higher rating) while others flow right-to-left (each child gets more than right neighbor if higher rating). One pass per direction, then combine — taking the max at each position.

**Running deficit / surplus (Gas Station):** If total gas ≥ total cost, a solution always exists. The starting point is the position after the last point where the running sum goes negative.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 921. Minimum Add to Make Parentheses Valid | Counter greedy — track unmatched open and close counts in one pass |
| 2 | LC 1717. Maximum Score from Removing Substrings | Greedy removal order — remove higher-value pair first; one pass per pair type |
| 3 | LC 134. Gas Station | Running sum — start after last point where prefix sum goes negative |
| 4 | LC 135. Candy | Two-pass greedy — left-to-right for left-neighbor constraint, right-to-left for right-neighbor |
| 5 | LC 670. Maximum Swap | From right, track last occurrence of each digit — swap current digit with largest digit to its right |
| 6 | LC 330. Patching Array | Greedy reach extension — if current number > reach+1, patch reach+1; else extend reach by current number |

---

## Pattern 5: Greedy + Data Structure

**Identify:** The greedy decision at each step is clear (always pick the best available option), but "best available" changes dynamically as elements are added or removed. A heap or ordered set maintains the current best candidate efficiently. The greedy insight is the *what*; the data structure is the *how*.

**Key distinction from pure heap problems:** In Heap sheet patterns, the data structure is the algorithm. Here, the greedy logic drives the algorithm and the data structure is a tool enabling O(log n) greedy choices. The recognition signal: you can describe the greedy rule in plain English without mentioning a heap — then realize a heap is needed to implement that rule efficiently.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 1642. Furthest Building You Can Reach | Greedy: use ladders for biggest jumps — min-heap of size k tracks which jumps got a ladder; swap out smallest when needed |
| 2 | LC 2599. Make the Prefix Sum Non-negative | Greedy: when prefix sum goes negative, retroactively convert the most negative number — max-heap of past negatives |
| 3 | LC 1383. Maximum Performance of a Team | Sort engineers by efficiency descending — min-heap of size k maintains speed sum; update answer at each step *(cross-ref: Heap Pattern 1)* |
| 4 | LC 630. Course Schedule III | Sort courses by deadline — greedily take each course; if total time exceeds deadline, swap out longest course (max-heap) |
| 5 | LC 1488. Avoid Flood in the City | Greedy: dry the lake that will fill soonest — ordered set of available dry days; lower-bound query per rain day |

---

## Pattern 6: Hard Greedy — Exchange Argument & Two-Variable Sorting

**Identify:** The greedy rule is not immediately obvious. The proof requires a non-trivial exchange argument or a two-variable sort where the correct ordering criterion takes thought to derive. These problems cannot be solved by instinct alone — you must reason about what happens when you swap two adjacent elements in your ordering.

**Two-variable sort pattern:** When you have two properties per element and need to sort by a derived criterion, ask: "if I swap element A and element B in my sequence, does the total cost increase or decrease?" The answer to that question gives you the comparator.

**Patching Array (LC 330)** is the hardest problem in this sheet. The insight — maintain a reachable range `[1, reach]` and decide whether to use the current number or patch — is a greedy algorithm that takes most people multiple exposures to internalize. Do it last.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 757. Set Intersection Size At Least Two | Sort intervals by end (then start desc) — greedily pick last two elements of each interval not already covered |
| 2 | LC 45. Jump Game II | Already in Pattern 2; revisit here if the BFS layer argument did not click — understand the exchange argument proof |
| 3 | LC 2439. Minimize Maximum of Array `[Optional]` | Greedy insight: answer = ceiling of prefix average — first instinct is binary search on answer; this problem teaches when that instinct is wrong and a direct mathematical greedy observation suffices instead |
| 4 | LC 330. Patching Array | Greedy reach extension — maintain `[1, reach]` fully reachable; decide patch vs use current number by whether `arr[i] <= reach+1` |

---

## DP Traps — Problems That Look Greedy But Are Not

Recognizing when NOT to use greedy is itself tested:

| Problem | Why not greedy |
|---|---|
| LC 0/1 Knapsack (any variant) | Each item can only be used once — local ratio-based greedy fails; use DP |
| LC 1235. Maximum Profit in Job Scheduling | Optimal job selection with overlap — DP + binary search, not greedy |
| LC 312. Burst Balloons | Order of bursting matters globally — Interval DP, not greedy |
| LC 646. Maximum Length of Pair Chain | Greedy works here (sort by end) — important to verify greedy is valid vs assuming DP needed |

---

## Final Summary

| Pattern | Problems | Core Technique |
|---|---|---|
| Selection & Assignment Greedy | 5 | Sort both sides, greedily match or assign |
| Jump Game & Interval Coverage | 4 | Extend reach greedily; count mandatory extensions |
| Interval Scheduling & Sorting | 8 | Sort by end/start/deadline; exchange argument proof |
| Array & String Greedy | 6 | Two-pass or running-state scan |
| Greedy + Data Structure | 5 | Greedy rule clear; heap/ordered set enables efficient execution |
| Hard Greedy | 4 | Exchange argument or two-variable sort — derive the comparator |
| **Total** | **~32 problems** | |

---

## How to Use This Sheet

**Pattern 1 first.** Assign Cookies and Fractional Knapsack give the clearest exchange argument proofs. Practice writing out the proof in plain English — this is the skill that makes all later patterns click.

**Patterns 2 and 3 can run in parallel** once Pattern 1 is solid. Jump Game II and Non-overlapping Intervals are the anchor problems of their respective patterns.

**Pattern 4 is standalone** — no dependency on 2 or 3. Gas Station and Candy are classic FAANG problems with deceptively simple greedy rules that require careful proof.

**Pattern 5 after Heap sheet Pattern 1 is solid.** The greedy insight must be clear before introducing the data structure layer. If you jump to Pattern 5 without Heap fluency, you end up memorizing code rather than understanding it.

**Pattern 6 last.** LC 330 Patching Array in particular is one of the most deceptively hard greedy problems in the entire canon. Attempt it only after all other patterns are clean.