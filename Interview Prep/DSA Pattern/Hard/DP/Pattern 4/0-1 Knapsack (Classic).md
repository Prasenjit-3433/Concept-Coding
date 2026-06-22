# 0/1 Knapsack (Classic)

---

# Stage 1: Identification

**Step 1 — Which topic?**

You are given `n` items, each with a weight `wt[i]` and a value `val[i]`. You have a bag (knapsack) with a maximum weight capacity `W`. You must select items to put in the bag such that the **total weight does not exceed W** and the **total value is maximized**. Each item can be used **at most once**.

The moment you see **"maximize value subject to a weight constraint, each item used at most once"** — that is the signal. To find the maximum, you must **try all possible combinations**. And whenever you try all combinations, **Recursion** is your first thought. And when that recursion has overlapping subproblems — it becomes **Dynamic Programming**.

**Why doesn't Greedy work here?**

The greedy instinct says: always pick the most valuable item first. But consider:

```
weights = [5, 3, 2],  values = [30, 40, 60],  W = 6
```

Greedy picks the costliest item first: value 60 (weight 2) → bag has 4 left. Next costliest is 40 (weight 3) → bag has 1 left. Can't pick weight 5 or weight 3 (both exceed 1). Total = **100**.

Wait, that actually works here. Let's find a case where it fails:

```
weights = [5, 3, 2],  values = [60, 40, 30],  W = 6
```

Greedy picks value 60 (weight 5) → bag has 1 left. Can't pick weight 3 or weight 2. Total = **60**.

But the optimal is to pick weight 3 (value 40) + weight 2 (value 30) = total **70**.

Greedy failed because there is **no uniformity** — picking the highest-value item now can block access to a better combination of smaller items. You must try all combinations.

**Step 2 — Which pattern?**

Each item is either taken or not taken — at most once. There is a capacity constraint. You want to maximize total value.

That is **Pattern 4: 0/1 Knapsack** — the canonical problem of this entire pattern.

**Step 3 — Which key concept?**

Apply Striver's **3-step shortcut**:

```
Step 1: Represent in terms of (index, W)
        index = current item being considered
        W     = remaining bag capacity
Step 2: Explore all possibilities — Pick or Not Pick
Step 3: Question says maximize → take max of all choices
```

The new element compared to previous knapsack problems: you only pick an item if its **weight fits in the remaining bag capacity**. This is the knapsack constraint that did not exist in subset sum problems.

---

# Stage 2: Intuition Building

### The Problem Setup

```
weights = [3, 4, 5],  values = [30, 50, 60],  W = 8
```

Three items. Bag can carry at most weight 8.

```
Item 0: weight=3, value=30
Item 1: weight=4, value=50
Item 2: weight=5, value=60
```

What combinations are possible?

```
{Item 0}         → weight=3, value=30
{Item 1}         → weight=4, value=50
{Item 2}         → weight=5, value=60
{Item 0, Item 1} → weight=7, value=80
{Item 0, Item 2} → weight=8, value=90  ← fits in bag, value=90
{Item 1, Item 2} → weight=9, value=110 ← exceeds bag capacity W=8, INVALID
{Item 0,1,2}     → weight=12, INVALID
```

Maximum valid value = **90** (take Item 0 and Item 2).

### Step 1 — Represent in terms of (index, W)

Define:

```
f(index, W) = maximum value achievable by selecting from items
              arr[0...index], with remaining bag capacity W
```

So the answer we want is `f(n-1, W)`.

### Step 2 — Do all possible things at (index, W)

At any item, exactly two choices:

**Choice 1 — Not Take:**
Don't put item `index` in the bag. Move to the previous item. Bag capacity unchanged.
```
notTake = 0 + f(index - 1, W)
```

**Choice 2 — Take:**
Put item `index` in the bag. Only valid if `wt[index] <= W` (item must fit).
Add `val[index]` to the total value. Remaining capacity shrinks by `wt[index]`.
```
take = Integer.MIN_VALUE   (initially — acts as "invalid" sentinel)
if wt[index] <= W:
    take = val[index] + f(index - 1, W - wt[index])
```

Why `Integer.MIN_VALUE` for take? When the item doesn't fit in the bag, the take option is impossible. Setting it to `Integer.MIN_VALUE` ensures `max(notTake, take)` never picks an impossible choice.

### Step 3 — Take maximum

```
f(index, W) = max(notTake, take)
```

### Base Case

**When index == 0:**
Only one item left (Item 0). If it fits in the remaining bag (`wt[0] <= W`), take it for `val[0]`. Otherwise return 0.

```
if index == 0:
    if wt[0] <= W → return val[0]
    else          → return 0
```

### Visualizing the Recursion

```
weights=[3,4,5], values=[30,50,60], W=8

f(2, 8):
├── NOT TAKE item2(wt=5)  → f(1, 8)
│   ├── NOT TAKE item1(wt=4) → f(0, 8)
│   │       wt[0]=3 ≤ 8 → return val[0]=30
│   └── TAKE item1(wt=4)    → 50 + f(0, 4)
│               wt[0]=3 ≤ 4 → return val[0]=30
│           = 50 + 30 = 80
│   → max(30, 80) = 80
│
└── TAKE item2(wt=5)       → 60 + f(1, 3)
        ├── NOT TAKE item1(wt=4) → f(0, 3)
        │       wt[0]=3 ≤ 3 → return val[0]=30
        └── TAKE item1(wt=4)    → wt[1]=4 > W=3, CANNOT TAKE
        → max(30, Integer.MIN_VALUE) = 30
    = 60 + 30 = 90

f(2, 8) = max(80, 90) = 90 ✓
```

### Are there overlapping subproblems?

With more items and larger W, the same `f(index, W)` state gets called from multiple branches. For example, `f(0, 3)` can be reached via several different combinations of taking/not-taking earlier items. **Overlapping subproblems confirmed** → DP is needed.

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
    public int knapsack(int W, int[] wt, int[] val, int n) {
        return solve(n - 1, W, wt, val);
    }

    private int solve(int index, int w, int[] wt, int[] val) {
        // Base case: only item 0 remains
        // If it fits, take it; otherwise nothing can be done
        if (index == 0) {
            if (wt[0] <= w) return val[0];
            else return 0;
        }

        // Choice 1: Not Take item at index
        // Value added = 0, bag capacity unchanged, move to previous item
        int notTake = solve(index - 1, w, wt, val);

        // Choice 2: Take item at index — only if it fits in remaining capacity
        // WHY Integer.MIN_VALUE: if item doesn't fit, mark this choice as
        //     impossible so max() never picks it
        int take = Integer.MIN_VALUE;
        if (wt[index] <= w) {
            take = val[index] + solve(index - 1, w - wt[index], wt, val);
        }

        // Take maximum — we want maximum value
        return Math.max(notTake, take);
    }
}
```

**Time Complexity — O(2^n):**
At every item, the function makes 2 recursive calls — one for notTake and one for take (when valid). The recursion tree is a binary tree of depth n. Total nodes grow as 2^n. For n = 30 items that is over a billion calls — completely impractical.

**Space Complexity — O(n):**
No dp array is allocated. The **recursion call stack** holds one frame per item. The deepest chain goes from index n-1 down to index 0 — that is n frames simultaneously on the stack. Stack uses O(n) space.

---

## Approach 2 — Memoization (Top-Down DP)

The same state `f(index, w)` is reached from multiple branches. Store each result the first time it is computed.

**3 steps to convert recursion to memoization:**
```
Step 0: Declare dp array of size n × (W+1), initialize all to -1
Step 1: Before computing, check — if dp[index][w] != -1, return it   (lookup)
Step 2: After computing, store — dp[index][w] = result               (store)
```

```java
class Solution {
    public int knapsack(int W, int[] wt, int[] val, int n) {
        // Step 0: dp[index][w] = -1 means not yet computed
        int[][] dp = new int[n][W + 1];
        for (int[] row : dp) {
            Arrays.fill(row, -1);
        }
        return solve(n - 1, W, wt, val, dp);
    }

    private int solve(int index, int w, int[] wt, int[] val, int[][] dp) {
        // Base case: only item 0 remains
        if (index == 0) {
            if (wt[0] <= w) return val[0];
            else return 0;
        }

        // Step 1: Already computed? Return stored value instantly
        // WHY: f(1, 4) = max value using items 0..1 with capacity 4
        //      This never changes no matter which path reached this state
        if (dp[index][w] != -1) return dp[index][w];

        // Choice 1: Not Take
        int notTake = solve(index - 1, w, wt, val, dp);

        // Choice 2: Take (only if item fits)
        int take = Integer.MIN_VALUE;
        if (wt[index] <= w) {
            take = val[index] + solve(index - 1, w - wt[index], wt, val, dp);
        }

        // Step 2: Store before returning
        dp[index][w] = Math.max(notTake, take);
        return dp[index][w];
    }
}
```

**Time Complexity — O(n × W):**
Each unique (index, w) pair is computed exactly once. There are n × (W+1) unique states. For each state, O(1) work is done — compute notTake and take, then take max. Total: O(n × W).

**Space Complexity — O(n × W) + O(n):**
Two sources of space. First, the dp array of size n × (W+1) — that is O(n × W). Second, the **recursion call stack** — the deepest chain goes n levels deep. Total: O(n × W) + O(n).

---

## Approach 3 — Tabulation (Bottom-Up DP)

**Direction:** Recursion goes from index n-1 **downward** to index 0. Tabulation goes the **opposite direction** — from index 0 **upward** to index n-1.

**Base case analysis:**

When `index == 0`, item 0 is the only item available. For every capacity w from 0 to W:
- If `wt[0] <= w` → we can take item 0 → `dp[0][w] = val[0]`
- Otherwise → we cannot take it → `dp[0][w] = 0`

```java
class Solution {
    public int knapsack(int W, int[] wt, int[] val, int n) {
        // Step 1: Declare dp array
        // dp[i][w] = max value using items 0..i with bag capacity w
        int[][] dp = new int[n][W + 1];

        // Step 2: Fill base case — index = 0 (only item 0 available)
        // For every capacity w, if item 0 fits, take it
        // WHY loop over all w: at index 0, the answer depends on whether
        //     item 0 fits in the current capacity, which varies with w
        for (int w = wt[0]; w <= W; w++) {
            dp[0][w] = val[0];
        }
        // For w < wt[0], dp[0][w] stays 0 (item 0 doesn't fit)

        // Step 3: Fill from index=1 to n-1
        // WHY start at 1: index 0 is already filled as base case
        for (int i = 1; i < n; i++) {
            // WHY w starts at 0: capacity 0 always gives value 0
            //     (no item can fit in a bag of capacity 0)
            for (int w = 0; w <= W; w++) {

                // Choice 1: Not Take item i
                // WHY dp[i-1][w]: skip item i, best value from previous items
                //     with same capacity
                int notTake = dp[i - 1][w];

                // Choice 2: Take item i — only if it fits in current capacity
                // WHY dp[i-1][w - wt[i]]: took item i, remaining capacity
                //     shrinks, look at previous items with reduced capacity
                int take = Integer.MIN_VALUE;
                if (wt[i] <= w) {
                    take = val[i] + dp[i - 1][w - wt[i]];
                }

                dp[i][w] = Math.max(notTake, take);
            }
        }

        // Answer: max value using all n items with full capacity W
        return dp[n - 1][W];
    }
}
```

**Dry Run:**

```
weights=[3,4,5], values=[30,50,60], W=8, n=3

Step 1 — Fill index=0 (wt[0]=3, val[0]=30):
w=0,1,2: dp[0][w] = 0   (weight 3 doesn't fit)
w=3..8:  dp[0][w] = 30  (weight 3 fits, take item 0)

dp[0] = [0, 0, 0, 30, 30, 30, 30, 30, 30]
         0  1  2   3   4   5   6   7   8

Step 2 — Fill i=1 (wt[1]=4, val[1]=50):
w=0: notTake=0,  take=INVALID    → 0
w=1: notTake=0,  take=INVALID    → 0
w=2: notTake=0,  take=INVALID    → 0
w=3: notTake=30, take=INVALID    → 30
w=4: notTake=30, take=50+dp[0][0]=50+0=50 → max(30,50)=50
w=5: notTake=30, take=50+dp[0][1]=50+0=50 → max(30,50)=50
w=6: notTake=30, take=50+dp[0][2]=50+0=50 → max(30,50)=50
w=7: notTake=30, take=50+dp[0][3]=50+30=80 → max(30,80)=80
w=8: notTake=30, take=50+dp[0][4]=50+30=80 → max(30,80)=80

dp[1] = [0, 0, 0, 30, 50, 50, 50, 80, 80]

Step 3 — Fill i=2 (wt[2]=5, val[2]=60):
w=0..4: notTake=dp[1][w], take=INVALID → same as dp[1]
w=5: notTake=50, take=60+dp[1][0]=60+0=60 → max(50,60)=60
w=6: notTake=50, take=60+dp[1][1]=60+0=60 → max(50,60)=60
w=7: notTake=80, take=60+dp[1][2]=60+0=60 → max(80,60)=80
w=8: notTake=80, take=60+dp[1][3]=60+30=90 → max(80,90)=90

dp[2] = [0, 0, 0, 30, 50, 60, 60, 80, 90]

Answer = dp[2][8] = 90 ✓
```

**Time Complexity — O(n × W):**
Two nested loops — outer runs n-1 times, inner runs W+1 times. Each cell does O(1) work. Total: O(n × W). Same as memoization but no function call overhead and no recursion stack.

**Space Complexity — O(n × W):**
Only the dp array of size n × (W+1). No recursion stack at all.

---

## Approach 4 — Space Optimization (The Final Form)

### Two-Row Optimization First

Look at the tabulation recurrence:

```
dp[i][w] = max(dp[i-1][w],  val[i] + dp[i-1][w - wt[i]])
```

To compute row `i`, we only ever look at row `i-1`. Keep just two 1D arrays — `prev` (row i-1) and `curr` (row i). After each row, `curr` becomes the new `prev`.

### One-Row Optimization — The Key Insight

Striver teaches that we can go further — using **just one array**. Here is why it works:

When filling row `i` left to right (w from 0 to W), the take case needs `dp[i-1][w - wt[i]]`. Since `wt[i] > 0`, `w - wt[i] < w` — the value we need is always **to the left** of the current position.

If we fill **right to left** (w from W down to 0), by the time we compute `prev[w]`, all positions to the right of `w` have already been updated to the current row, but `prev[w - wt[i]]` (to the left) still holds the previous row's value — exactly what we need.

This means we can update `prev` in place from right to left, effectively using `prev` as both the "previous row" and the "current row" simultaneously.

```
Visualizing right-to-left fill on prev:

prev = [0, 0, 0, 30, 50, 50, 50, 80, 80]  ← after row i=1

Computing row i=2 (wt[2]=5, val[2]=60) right to left:

w=8: take=60+prev[8-5]=60+prev[3]=60+30=90, notTake=prev[8]=80 → 90
     prev[8] = 90   ← updated

w=7: take=60+prev[7-5]=60+prev[2]=60+0=60,  notTake=prev[7]=80 → 80
     prev[7] = 80   ← unchanged

w=6: take=60+prev[6-5]=60+prev[1]=60+0=60,  notTake=prev[6]=50 → 60
     prev[6] = 60   ← updated

... and so on

WHY right to left works: prev[3] used when computing w=8
    was NOT overwritten yet (we haven't reached w=3 going right to left)
    so it still holds the row i=1 value — exactly what we need.

WHY left to right would FAIL: if we fill w=3 first, prev[3] gets updated
    to the row i=2 value. Then when we compute w=8, prev[8-5]=prev[3]
    gives the NEW row i=2 value instead of row i=1 — same item counted twice!
```

```java
class Solution {
    public int knapsack(int W, int[] wt, int[] val, int n) {
        // prev[w] = max value using items seen so far with capacity w
        // Initialize with base case for index=0
        int[] prev = new int[W + 1];

        // Base case: only item 0 available
        // For every capacity w >= wt[0], item 0 can be taken
        for (int w = wt[0]; w <= W; w++) {
            prev[w] = val[0];
        }
        // For w < wt[0], prev[w] stays 0

        // Fill from index=1 to n-1
        for (int i = 1; i < n; i++) {
            // Fill RIGHT TO LEFT — critical for correctness
            // WHY right to left: ensures prev[w - wt[i]] still holds
            //     the previous row's value when we read it.
            //     Left to right would let the same item be picked twice.
            for (int w = W; w >= 0; w--) {

                // Choice 1: Not Take item i
                // WHY prev[w]: prev still holds row i-1 for this position
                //     (we haven't overwritten it yet in this pass)
                int notTake = prev[w];

                // Choice 2: Take item i — only if it fits
                // WHY prev[w - wt[i]]: w - wt[i] < w, so it's to our left.
                //     Since we fill right to left, this position hasn't been
                //     overwritten yet → still holds the row i-1 value. Correct.
                int take = Integer.MIN_VALUE;
                if (wt[i] <= w) {
                    take = val[i] + prev[w - wt[i]];
                }

                // Update prev in place — this IS the current row now
                prev[w] = Math.max(notTake, take);
            }
        }

        // prev[W] now holds the answer for all n items with full capacity W
        return prev[W];
    }
}
```

**Time Complexity — O(n × W):**
Two nested loops — outer runs n-1 times, inner runs W+1 times (right to left). Each iteration does O(1) work. Total: O(n × W). No change from tabulation.

**Space Complexity — O(W):**
No n × (W+1) array. No recursion stack. Just one array of size W+1 — `prev`. This is updated in place for each item. Memory is completely independent of n.

---

## Final Comparison

| Approach | Time | Space | Notes |
|---|---|---|---|
| Pure Recursion | O(2^n) | O(n) stack | Exponential — never use |
| Memoization | O(n × W) | O(n×W) + O(n) stack | Good interview starting point |
| Tabulation | O(n × W) | O(n × W) | Better — eliminates stack |
| Two-Row Space Opt | O(n × W) | O(W) | Good |
| **One-Row Space Opt** | O(n × W) | **O(W)** | **Best — submit this** |

---

## The Key Takeaway

```
┌──────────────────────────────────────────────────────────────────┐
│  0/1 Knapsack = the canonical pattern for "each item used        │
│  at most once, maximize value subject to capacity constraint."   │
│                                                                  │
│  The new element vs. subset sum problems:                        │
│  The WEIGHT CHECK — you can only take an item if it fits.        │
│      if wt[i] <= w: take = val[i] + f(i-1, w - wt[i])            │
│  Without this guard, you'd allow items that exceed the bag.      │
│                                                                  │
│  Base case:                                                      │
│  When only item 0 remains:                                       │
│      if wt[0] <= w → return val[0]  (take it, it fits)           │
│      else          → return 0       (can't take it)              │
│                                                                  │
│  The one-row space optimization — the critical insight:          │
│  Fill the single prev[] array RIGHT TO LEFT.                     │
│  Why: take = val[i] + prev[w - wt[i]]                            │
│       w - wt[i] < w → always to the left of current w            │
│       Filling right to left guarantees prev[w - wt[i]] still     │
│       holds the previous row's value when we read it.            │
│       Filling left to right would overwrite it first →           │
│       same item picked twice → Unbounded Knapsack behavior.      │
│                                                                  │
│  This direction rule is the single most important                │
│  distinction between 0/1 Knapsack and Unbounded Knapsack:        │
│      0/1 Knapsack:       fill RIGHT TO LEFT  (prevent reuse)     │
│      Unbounded Knapsack: fill LEFT TO RIGHT  (allow reuse)       │
└──────────────────────────────────────────────────────────────────┘
```