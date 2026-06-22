# Unbounded Knapsack (Classic)

---

# Stage 1: Identification

**Step 1 — Which topic?**

You are given `n` items, each with a weight `wt[i]` and a value `val[i]`. You have a knapsack with maximum weight capacity `W`. You must select items to maximize total value. Unlike 0/1 Knapsack, **each item has infinite supply** — you can pick the same item as many times as you want.

The moment you see **"maximize value subject to a weight constraint, items reusable unlimited times"** — that is the signal. To find the maximum, you must **try all possible combinations**. And whenever you try all combinations, **Recursion** is your first thought. And when that recursion has overlapping subproblems — it becomes **Dynamic Programming**.

**Why doesn't Greedy work here?**

Same reasoning as 0/1 Knapsack — there is no uniformity in the value-to-weight ratio across items. Picking the highest value-per-weight item first might block a better combination. You must try all choices.

**Step 2 — Which pattern?**

Items can be reused unlimited times. There is a capacity constraint. You want to maximize total value.

That is **Pattern 5: Unbounded Knapsack** — the canonical problem of this pattern.

**Step 3 — Which key concept?**

Apply Striver's 3-step shortcut — almost identical to 0/1 Knapsack:

```
Step 1: Represent in terms of (index, W)
Step 2: Explore all possibilities — Pick or Not Pick
Step 3: Question says maximize → take max of all choices
```

**The single critical difference from 0/1 Knapsack:**

```
0/1 Knapsack: when you PICK item at index, move to index-1
              (item used once, cannot reuse)

Unbounded Knapsack: when you PICK item at index, STAY at index
                    (item has infinite supply, can pick again)
```

This one change — `index-1` becomes `index` in the pick case — is the entire difference between the two patterns.

---

# Stage 2: Intuition Building

### The Problem Setup

```
weights = [2, 4, 6],  values = [5, 11, 13],  W = 10
```

Three items, infinite supply each. Bag capacity = 10.

```
Item 0: weight=2, value=5
Item 1: weight=4, value=11
Item 2: weight=6, value=13
```

Some valid combinations:

```
{Item2, Item1}     → weight=10, value=24
{Item2, Item0, Item0} → weight=10, value=23
{Item1, Item1, Item0} → weight=10, value=27  ← maximum
{Item0×5}          → weight=10, value=25
```

Maximum value = **27** (two weight-4 items + one weight-2 item).

### Step 1 — Represent in terms of (index, W)

Define:

```
f(index, W) = maximum value achievable by selecting from items
              arr[0...index] with remaining bag capacity W,
              where each item can be picked any number of times
```

So the answer we want is `f(n-1, W)`.

### Step 2 — Do all possible things at (index, W)

At any item, exactly two choices:

**Choice 1 — Not Take:**
Don't pick item `index`. Move to the previous item. Bag capacity unchanged.
```
notTake = 0 + f(index - 1, W)
```

**Choice 2 — Take:**
Pick item `index`. Only valid if `wt[index] <= W`.
Add `val[index]`. Remaining capacity shrinks by `wt[index]`.
**Stay at the same index** — the item has infinite supply, so we can pick it again.

```
take = Integer.MIN_VALUE
if wt[index] <= W:
    take = val[index] + f(index, W - wt[index])
                              ↑
                    SAME index — not index-1
                    This is the ONLY difference from 0/1 Knapsack
```

### Step 3 — Take maximum

```
f(index, W) = max(notTake, take)
```

### Base Case

**When index == 0:**
Only item 0 is available with infinite supply. The thief has remaining capacity `W`. How many times can he take item 0? As long as `W / wt[0]` times — integer division gives the count. Each pick gives `val[0]`.

```
if index == 0:
    return (W / wt[0]) * val[0]
```

Why not a simple `if wt[0] <= W return val[0]`? Because item 0 has infinite supply. If the bag still has capacity after one pick, he picks again. The maximum picks are `W / wt[0]` times.

### Visualizing the Recursion

```
weights=[2,4,6], values=[5,11,13], W=10

f(2, 10):
├── NOT TAKE item2(wt=6) → f(1, 10)
│   ├── NOT TAKE item1(wt=4) → f(0, 10)
│   │       (10/2)*5 = 25
│   └── TAKE item1(wt=4)    → 11 + f(1, 6)    ← stays at index 1
│           ├── NOT TAKE → f(0, 6) = (6/2)*5 = 15
│           └── TAKE     → 11 + f(1, 2)
│                   ├── NOT TAKE → f(0, 2) = (2/2)*5 = 5
│                   └── TAKE: 4 > 2, CANNOT TAKE
│                   f(1,2) = max(5, MIN_VALUE) = 5
│               = 11 + 5 = 16
│           f(1,6) = max(15, 16) = 16
│       = 11 + 16 = 27
│   f(1,10) = max(25, 27) = 27
│
└── TAKE item2(wt=6)     → 13 + f(2, 4)    ← stays at index 2
        ├── NOT TAKE → f(1, 4) = max(f(0,4), 11+f(1,0)) = max(10, 11) = 11
        └── TAKE: 6 > 4, CANNOT TAKE
        f(2,4) = 11
    = 13 + 11 = 24

f(2,10) = max(27, 24) = 27 ✓
```

### Are there overlapping subproblems?

`f(1, 6)` and `f(0, 2)` are reached from multiple paths. With larger inputs the explosion is massive. **Overlapping subproblems confirmed** → DP is needed.

### DP Table Size

Two parameters:
- `index`: 0 to n-1 → **n values**
- `W`: 0 to W → **W+1 values**

dp table: **n × (W+1)**

---

# Stage 3: Coding

## Approach 1 — Pure Recursion (Brute Force)

```java
class Solution {
    public int unboundedKnapsack(int W, int[] wt, int[] val, int n) {
        return solve(n - 1, W, wt, val);
    }

    private int solve(int index, int w, int[] wt, int[] val) {
        // Base case: only item 0 available, infinite supply
        // Take as many as the remaining capacity allows
        // WHY (w / wt[0]) * val[0]: integer division gives count of picks,
        //     each pick adds val[0] — this covers all picks in one formula
        if (index == 0) {
            return (w / wt[0]) * val[0];
        }

        // Choice 1: Not Take item at index
        // Move to previous item, capacity unchanged
        int notTake = solve(index - 1, w, wt, val);

        // Choice 2: Take item at index — only if it fits
        // WHY Integer.MIN_VALUE: if item doesn't fit, mark as impossible
        //     so max() never picks this choice
        int take = Integer.MIN_VALUE;
        if (wt[index] <= w) {
            // WHY index (not index-1): infinite supply means we stay
            //     at the same item — we can pick it again next call
            take = val[index] + solve(index, w - wt[index], wt, val);
        }

        // Return maximum of both choices
        return Math.max(notTake, take);
    }
}
```

**Time Complexity — O(W^n) in the worst case:**
The recursion does not simply halve at each level. Because the pick case stays at the same index and only reduces `W`, the depth for a single item can go up to `W / wt[index]` before moving on. The total branching makes this exponential and impractical.

**Space Complexity — O(W):**
The deepest call chain is when we keep picking item 0 (weight 1) starting from capacity W, going down to 0. That is W frames deep on the stack. So auxiliary stack space is O(W).

---

## Approach 2 — Memoization (Top-Down DP)

```java
class Solution {
    public int unboundedKnapsack(int W, int[] wt, int[] val, int n) {
        // dp[index][w] = -1 means not yet computed
        int[][] dp = new int[n][W + 1];
        for (int[] row : dp) {
            Arrays.fill(row, -1);
        }
        return solve(n - 1, W, wt, val, dp);
    }

    private int solve(int index, int w, int[] wt, int[] val, int[][] dp) {
        // Base case: only item 0, infinite supply
        if (index == 0) {
            return (w / wt[0]) * val[0];
        }

        // Step 1: Already computed? Return stored value instantly
        // WHY: f(1, 6) = max value using items 0..1 with capacity 6
        //      This never changes regardless of which path reached this state
        if (dp[index][w] != -1) return dp[index][w];

        // Choice 1: Not Take
        int notTake = solve(index - 1, w, wt, val, dp);

        // Choice 2: Take (only if fits)
        // WHY stay at same index: infinite supply
        int take = Integer.MIN_VALUE;
        if (wt[index] <= w) {
            take = val[index] + solve(index, w - wt[index], wt, val, dp);
        }

        // Step 2: Store before returning
        dp[index][w] = Math.max(notTake, take);
        return dp[index][w];
    }
}
```

**Time Complexity — O(n × W):**
Each unique (index, w) pair is computed exactly once. There are n × (W+1) unique states. For each state, O(1) work is done. Total: O(n × W).

**Space Complexity — O(n × W) + O(W):**
Two sources of space. First, the dp array of size n × (W+1). Second, the recursion call stack — the deepest chain is O(W) deep (repeatedly picking item 0). Total: O(n × W) + O(W).

---

## Approach 3 — Tabulation (Bottom-Up DP)

**Direction:** Recursion goes from index n-1 **downward** to index 0. Tabulation goes the **opposite direction** — from index 0 **upward** to index n-1.

**Base case in tabulation:**
For `index == 0`, every capacity `w` from 0 to W gets `dp[0][w] = (w / wt[0]) * val[0]`.

```java
class Solution {
    public int unboundedKnapsack(int W, int[] wt, int[] val, int n) {
        // dp[i][w] = max value using items 0..i with bag capacity w
        int[][] dp = new int[n][W + 1];

        // Step 2: Fill base case — index = 0, infinite supply of item 0
        // For every capacity w, take as many item 0 as possible
        // WHY (w / wt[0]) * val[0]: the count of picks × value per pick
        for (int w = 0; w <= W; w++) {
            dp[0][w] = (w / wt[0]) * val[0];
        }

        // Step 3: Fill from index=1 to n-1
        for (int i = 1; i < n; i++) {
            // WHY w starts at 0: capacity 0 always gives value 0
            for (int w = 0; w <= W; w++) {

                // Choice 1: Not Take item i
                // WHY dp[i-1][w]: skip item i, look at previous row
                int notTake = dp[i - 1][w];

                // Choice 2: Take item i — only if it fits
                // WHY dp[i][w - wt[i]] (not dp[i-1][...]): infinite supply
                //     means we stay at the same index row i, not i-1
                // This is THE KEY difference from 0/1 Knapsack tabulation
                int take = Integer.MIN_VALUE;
                if (wt[i] <= w) {
                    take = val[i] + dp[i][w - wt[i]];
                }

                dp[i][w] = Math.max(notTake, take);
            }
        }

        return dp[n - 1][W];
    }
}
```

**Dry Run:**

```
weights=[2,4,6], values=[5,11,13], W=10

Step 1 — Fill index=0 (wt[0]=2, val[0]=5):
dp[0][w] = (w/2)*5
w=0→0, w=1→0, w=2→5, w=3→5, w=4→10, w=5→10,
w=6→15, w=7→15, w=8→20, w=9→20, w=10→25

dp[0] = [0, 0, 5, 5, 10, 10, 15, 15, 20, 20, 25]

Step 2 — Fill i=1 (wt[1]=4, val[1]=11):
w=0: notTake=0, take=N/A    → 0
w=1: notTake=0, take=N/A    → 0
w=2: notTake=5, take=N/A    → 5
w=3: notTake=5, take=N/A    → 5
w=4: notTake=10, take=11+dp[1][0]=11+0=11 → max(10,11)=11
w=5: notTake=10, take=11+dp[1][1]=11+0=11 → max(10,11)=11
w=6: notTake=15, take=11+dp[1][2]=11+5=16 → max(15,16)=16
w=7: notTake=15, take=11+dp[1][3]=11+5=16 → max(15,16)=16
w=8: notTake=20, take=11+dp[1][4]=11+11=22 → max(20,22)=22
w=9: notTake=20, take=11+dp[1][5]=11+11=22 → max(20,22)=22
w=10: notTake=25, take=11+dp[1][6]=11+16=27 → max(25,27)=27

dp[1] = [0, 0, 5, 5, 11, 11, 16, 16, 22, 22, 27]

Step 3 — Fill i=2 (wt[2]=6, val[2]=13):
w=0..5: notTake=dp[1][w], take=N/A → same as dp[1]
w=6: notTake=16, take=13+dp[2][0]=13+0=13 → max(16,13)=16
w=7: notTake=16, take=13+dp[2][1]=13+0=13 → max(16,13)=16
w=8: notTake=22, take=13+dp[2][2]=13+5=18 → max(22,18)=22
w=9: notTake=22, take=13+dp[2][3]=13+5=18 → max(22,18)=22
w=10: notTake=27, take=13+dp[2][4]=13+11=24 → max(27,24)=27

dp[2] = [0, 0, 5, 5, 11, 11, 16, 16, 22, 22, 27]

Answer = dp[2][10] = 27 ✓
```

**Time Complexity — O(n × W):**
Two nested loops — outer runs n-1 times, inner runs W+1 times. Each cell does O(1) work. Total: O(n × W).

**Space Complexity — O(n × W):**
Only the dp array of size n × (W+1). No recursion stack.

---

## Approach 4 — Space Optimization (The Final Form)

### Why One Array Works — The Key Insight

In 0/1 Knapsack, we had:
```
dp[i][w] = max(dp[i-1][w],  val[i] + dp[i-1][w - wt[i]])
```
Both the notTake and take cases used **row i-1**. To avoid using the same item twice, we had to fill **right to left**.

In Unbounded Knapsack, we have:
```
dp[i][w] = max(dp[i-1][w],  val[i] + dp[i][w - wt[i]])
```
The notTake case uses **row i-1**, but the take case uses **row i** itself (same row, to the left). This means:

- When computing `prev[w]`, the take case needs `prev[w - wt[i]]` — a position to the **left** of `w` on the **current row**.
- If we fill **left to right**, by the time we compute `prev[w]`, the value at `prev[w - wt[i]]` has **already been updated to the current row** — exactly what we need for infinite reuse.
- Unlike 0/1 Knapsack where left-to-right caused the same item to be picked twice (a bug), here it is the **correct behavior** — we want to allow picking the same item again.

```
Direction rule summary:
    0/1 Knapsack:       fill RIGHT TO LEFT  (prevent reuse — prev[w-wt] must be old row)
    Unbounded Knapsack: fill LEFT TO RIGHT  (allow reuse   — prev[w-wt] should be new row)
```

```java
class Solution {
    public int unboundedKnapsack(int W, int[] wt, int[] val, int n) {
        // prev[w] = max value using items seen so far with capacity w
        // Initialize with base case for index=0
        int[] prev = new int[W + 1];

        // Base case: only item 0 available, infinite supply
        for (int w = 0; w <= W; w++) {
            prev[w] = (w / wt[0]) * val[0];
        }

        // Fill from index=1 to n-1
        for (int i = 1; i < n; i++) {
            // Fill LEFT TO RIGHT — critical for correctness
            // WHY left to right: take case uses dp[i][w - wt[i]]
            //     which is the CURRENT row to the left.
            //     Filling left to right means prev[w - wt[i]] is already
            //     updated to current row i when we read it — allowing
            //     the same item to be picked multiple times. This is correct
            //     for unbounded knapsack.
            //     If we filled right to left, prev[w - wt[i]] would still
            //     hold the old row i-1 value — effectively treating this
            //     as 0/1 Knapsack (each item used at most once).
            for (int w = 0; w <= W; w++) {

                // Choice 1: Not Take item i
                // WHY prev[w]: before this iteration, prev[w] still holds
                //     row i-1 value at this position (not yet overwritten)
                int notTake = prev[w];

                // Choice 2: Take item i — only if it fits
                // WHY prev[w - wt[i]]: w - wt[i] < w, so we already
                //     updated it in this left-to-right pass → holds
                //     current row i value → allows picking item i again
                int take = Integer.MIN_VALUE;
                if (wt[i] <= w) {
                    take = val[i] + prev[w - wt[i]];
                }

                // Update prev in place — this IS the current row now
                prev[w] = Math.max(notTake, take);
            }
        }

        // prev[W] holds the answer for all n items with full capacity W
        return prev[W];
    }
}
```

**Time Complexity — O(n × W):**
Two nested loops — outer runs n-1 times, inner runs W+1 times left to right. Each iteration does O(1) work. Total: O(n × W). No change from tabulation.

**Space Complexity — O(W):**
No n × (W+1) array. No recursion stack. Just one array of size W+1. Memory is completely independent of n.

---

## Final Comparison

| Approach | Time | Space | Notes |
|---|---|---|---|
| Pure Recursion | Exponential | O(W) stack | Never use |
| Memoization | O(n × W) | O(n×W) + O(W) stack | Good interview starting point |
| Tabulation | O(n × W) | O(n × W) | Better — eliminates stack |
| Space Optimization | O(n × W) | **O(W)** | Best — submit this |

---

## The Key Takeaway

```
┌──────────────────────────────────────────────────────────────────┐
│  Unbounded Knapsack = 0/1 Knapsack with ONE change:              │
│                                                                  │
│  0/1 Knapsack pick:  val[i] + f(index-1, W - wt[i])              │
│  Unbounded pick:     val[i] + f(index,   W - wt[i])              │
│                                ↑                                 │
│                      Stay at same index — reuse allowed          │
│                                                                  │
│  Base case difference:                                           │
│  0/1: if wt[0] <= W → val[0], else 0                             │
│       (item 0 used at most once)                                 │
│  Unbounded: (W / wt[0]) * val[0]                                 │
│       (item 0 used as many times as it fits)                     │
│                                                                  │
│  The space optimization direction rule:                          │
│                                                                  │
│  0/1 Knapsack (prevent reuse):                                   │
│      Fill RIGHT TO LEFT                                          │
│      prev[w - wt[i]] must still be the OLD row value             │
│      → same item cannot be picked twice                          │
│                                                                  │
│  Unbounded Knapsack (allow reuse):                               │
│      Fill LEFT TO RIGHT                                          │
│      prev[w - wt[i]] is already updated to CURRENT row           │
│      → same item can be picked again                             │
│                                                                  │
│  This direction flip is the single most important                │
│  distinction between Pattern 4 and Pattern 5.                    │
│  The code looks almost identical — the bug is invisible          │
│  unless you understand WHY the direction matters.                │
└──────────────────────────────────────────────────────────────────┘
```