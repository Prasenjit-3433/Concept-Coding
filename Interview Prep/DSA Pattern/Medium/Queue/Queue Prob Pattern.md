

---

# Queue DSA Patterns

*"Problems are infinite, but patterns are finite!"*

---

## The Core Mental Model — Before Any Pattern

A queue enforces **FIFO order** — the first thing enqueued is the first thing processed. Every queue pattern exploits this in a specific way:

- **Simulation problems:** The queue models the actual process — people in line, cards being dealt, a circular game. You enqueue and dequeue following the exact rules of the problem.
- **Monotonic deque:** A double-ended queue that maintains a window of useful candidates, discarding elements that can never be the answer. When a new element arrives, you pop from the back to maintain monotonicity. When the window slides, you pop from the front to discard out-of-range elements.

**The monotonic deque insight — say this before every sliding window problem:**
> *"I maintain a deque of indices. The front always holds the best candidate for the current window. I pop from the back when the new element dominates older ones. I pop from the front when older elements fall outside the window."*

Everything in Patterns 3 and 4 flows from this.

---

## Pattern Identification Quick Reference

| Pattern | Trigger |
|---|---|
| Queue Implementation & Design | "implement queue", "circular queue", "implement stack using queue" |
| Standard Queue Simulation | "circular game", "reveal cards", "tickets", "simulate process step by step with ordering" |
| Monotonic Deque — Sliding Window | "sliding window maximum/minimum", "window with bounded difference", "most competitive subsequence" |
| Monotonic Deque — Advanced | "shortest subarray with sum ≥ K", "max value of equation", "constrained subsequence sum" |

---

## Pattern 1: Queue Implementation & Design

**Identify:** The problem asks you to build a queue or implement one data structure using another. These are warm-up problems that establish the FIFO mechanics and circular buffer internals before any pattern work.

**Why this is its own pattern:** Before sliding window or simulation, you must be comfortable with `enqueue`, `dequeue`, front/rear pointer arithmetic, and the circular buffer trick for fixed-size queues. Implementing LC 622 from scratch forces you to handle all these edge cases once — cleanly.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 232. Implement Queue Using Stacks | Two stacks simulate FIFO — lazy transfer on dequeue |
| 2 | LC 225. Implement Stack Using Queues | Two queues simulate LIFO — rotate on push |
| 3 | LC 622. Design Circular Queue | Fixed-size circular buffer — `(rear + 1) % capacity` pointer arithmetic |
| 4 | LC 641. Design Circular Deque | Same circular buffer extended to both ends — front and rear pointers move in both directions |

---

## Pattern 2: Standard Queue Simulation

**Identify:** The problem describes a process — a game, a dealing procedure, a message spreading chain — and the queue models the **ordering** of that process directly. There is no sliding window, no monotonicity. You enqueue elements, process them in order, and re-enqueue them when the rules say so.

**Key insight — when to re-enqueue:** Many simulation problems (circular game, ticket buying) require moving a processed element back to the end of the queue. The stopping condition is either a count or an external constraint being met. Recognizing the re-enqueue step is the entire problem.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 933. Number of Recent Calls | Enqueue timestamp, dequeue when outside [t-3000, t] window — fixed sliding window via queue |
| 2 | LC 2073. Time Needed to Buy Tickets | Simulate queue: dequeue each person, decrement tickets, re-enqueue if not done — stop when target person finishes |
| 3 | LC 1823. Find the Winner of the Circular Game | Josephus problem — simulate k-step elimination using queue; dequeue front, re-enqueue k-1 times, permanently remove on kth step |
| 4 | LC 950. Reveal Cards in Increasing Order | Sort cards; simulate dealer's queue of indices to find the target arrangement |
| 5 | LC 346. Moving Average from Data Stream | Fixed-size queue — maintain running sum; dequeue oldest when size exceeds window |
| 6 | LC 2327. Number of People Aware of a Secret | BFS-like spreading — queue tracks who knows secret and on which day; process day by day |
| 7 | LC 936. Stamping the Sequence *(Hard — Optional)* | Reverse stamping simulation — work backward, use queue to track stampable positions; technique is isolated and rarely asked |

---

## Pattern 3: Monotonic Deque — Sliding Window

**Identify:** You have a sliding window of size k (or a variable-size window with a constraint), and you need the maximum or minimum of the current window efficiently. The deque discards elements from the back when they are dominated by the new arrival, and from the front when they slide out of the window.

**The universal template — memorize this:**
```
dq = deque()   # stores indices
result = []

for i in range(len(arr)):
    # Remove from front if out of window
    while dq and dq[0] < i - k + 1:
        dq.popleft()
    # Remove from back if current element dominates (for max deque)
    while dq and arr[dq[-1]] <= arr[i]:
        dq.pop()
    dq.append(i)
    # Front of deque is the max of current window
    if i >= k - 1:
        result.append(arr[dq[0]])
```

For **minimum deque**, flip the comparison: `arr[dq[-1]] >= arr[i]`.

**Variable window extension (LC 2762, LC 1438):** When the window size is not fixed but bounded by a constraint (e.g., `max - min ≤ limit`), maintain **two deques** — one for max, one for min. Shrink the window from the left whenever `max_deque[0] - min_deque[0] > limit`.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 239. Sliding Window Maximum | Canonical monotonic deque template — fixed window, max of each window |
| 2 | LC 1438. Longest Continuous Subarray with Abs Diff ≤ Limit | Two simultaneous deques (max + min) — variable window bounded by difference constraint |
| 3 | LC 2762. Continuous Subarrays | Same two-deque pattern — shrink window when max - min > 2 |
| 4 | LC 1673. Find the Most Competitive Subsequence | Greedy monotonic deque — maintain increasing deque, pop when better candidate arrives and budget allows |

---

## Pattern 4: Monotonic Deque — Advanced Applications

**Identify:** The deque is not maintaining a raw sliding window maximum/minimum but is instead optimizing a **DP recurrence** or a **prefix sum query** where the best previous candidate must be found in O(1). These are harder — the deque is a tool inside a larger algorithm, not the algorithm itself.

**Why this is its own tier:** In Pattern 3, the deque is the whole solution. In Pattern 4, you must first recognize the underlying structure (prefix sum, DP recurrence, linear function optimization), and then recognize that a deque efficiently maintains the best candidate for that structure. The deque insight is the second step, not the first.

**LC 862 — the key insight:** Naively, "shortest subarray with sum ≥ K" looks like a two-pointer problem, but negative numbers break the monotone property two-pointers need. The fix: compute prefix sums, then use a monotonic deque to find the leftmost valid prefix in O(log n) amortized. This is the hardest and most important problem in this pattern.

**Cross-reference note:** LC 1696 Jump Game VI and LC 1425 Constrained Subsequence Sum both use this exact technique — sliding window DP with a monotonic deque. Their **primary home is the DP sheet** (Sliding Window DP inside Pattern 1). If you have done them there, the deque mechanics here will feel familiar. If not, do LC 239 first, then revisit them.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 862. Shortest Subarray with Sum at Least K | Prefix sum + monotonic deque — find leftmost prefix where `prefix[i] - prefix[j] ≥ K`; deque maintains increasing prefix sums |
| 2 | LC 1499. Max Value of Equation | Rewrite as `(y[j] + x[j]) + (y[i] - x[i])` — deque maintains max of `y[j] + x[j]` within window `x[i] - x[j] ≤ k` |
| 3 | LC 1696. Jump Game VI *(cross-ref from DP sheet)* | Sliding window DP — `dp[i] = arr[i] + max(dp[i-k..i-1])`; deque maintains max of previous k states |
| 4 | LC 1425. Constrained Subsequence Sum *(cross-ref from DP sheet)* | Sliding window DP — Kadane's variant; deque maintains max of previous k DP values |

---

## Final Summary

| Pattern | Problems | Core Mechanism |
|---|---|---|
| Queue Implementation & Design | 4 | FIFO mechanics, circular buffer pointer arithmetic |
| Standard Queue Simulation | 7 | Re-enqueue, process in order, stop on condition |
| Monotonic Deque — Sliding Window | 4 | Two-pointer deque: pop back on dominance, pop front on window expiry |
| Monotonic Deque — Advanced | 4 | Deque inside prefix sum or DP recurrence optimization |
| **Total** | **~19 problems** | |

---

## How to Use This Sheet

**Pattern 1 is the starting point.** LC 622 Design Circular Queue must be implementable from scratch — the circular buffer is the internal mechanism of every fixed-size window queue you will use later.

**Pattern 2 before Pattern 3.** Simulation problems establish that a queue models a process faithfully. Jumping to sliding window without this intuition leads to mechanical template application without understanding.

**LC 239 is the gateway to all of Pattern 3 and Pattern 4.** Do not attempt LC 1438, LC 862, or LC 1499 without LC 239 completely automatic. Every advanced deque problem is this template with an additional layer.

**Pattern 4 requires DP Sheet Pattern 1 (Sliding Window DP) to be solid first.** LC 862 in particular requires prefix sum understanding. LC 1696 and LC 1425 require the DP recurrence to be clear — the deque is an optimization on top, not the core idea.
