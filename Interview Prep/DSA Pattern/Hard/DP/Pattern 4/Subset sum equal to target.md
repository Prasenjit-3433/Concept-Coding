I've gone through the transcript carefully and all the existing solution files to match Striver's style precisely.

---

# Subset Sum Equal to Target (GFG / Coding Ninjas)

---

# Stage 1: Identification

**Step 1 — Which topic?**

You are given an array of `n` positive integers and an integer `k`. Your task is to check if there exists a **subset** of the array whose elements sum to exactly `k`. Return `true` if such a subset exists, `false` otherwise.

The moment you see **"does there exist a subset with a given sum"** — that is the signal. To check if such a subset exists, you must **try all possible subsets**. And whenever you try all possible subsets, **Recursion** is your first thought. And when that recursion has overlapping subproblems — it becomes **Dynamic Programming**.

**Why doesn't Greedy work here?**

Greedy would say — pick the largest element first, keep picking until you hit the target or overshoot. But this fails immediately. Consider:

```
arr = [3, 1, 4, 2],  k = 5
```

Greedy picks 4, then picks... 3 would overshoot (4+3=7), picks 1 → total = 5. Okay, that worked here. But consider:

```
arr = [3, 4, 2],  k = 5
```

Greedy picks 4, then 3 would overshoot, picks 2 → total = 6. Wrong. But the actual answer is `3 + 2 = 5` — true. Greedy missed it. **Values are not uniform** — there is no guarantee that taking the largest first closes in on the target correctly. You must try all subsets.

**Step 2 — Which pattern?**

This is the **foundation problem** of Pattern 4: 0/1 Knapsack.

The key structural signal for all problems in this pattern:

```
You have a list of items (the array elements).
Each item can be used AT MOST ONCE — you either take it or skip it.
There is a target constraint (sum = k).
You want to check feasibility, maximize, minimize, or count.
```

Every time you see this structure — fixed items, each used at most once, target constraint — that is **0/1 Knapsack**.

**Step 3 — Which key concept?**

Striver gives the **universal template** for all DP on subsequences problems — it works here and in every variation that follows:

```
Step 1: Express everything in terms of (index, target)
Step 2: Explore all possibilities at that index
        → Take it OR Not take it
Step 3: Return true if ANY possibility gives true
        (OR of both choices — since we're checking existence)
```

This **Take / Not-Take technique** is the core mechanism of 0/1 Knapsack. Every problem in this pattern is a variation of this exact structure.

---

# Stage 2: Intuition Building

### The Problem Setup

```
arr = [1, 2, 3, 4],  k = 4
```

Can we form a subset summing to 4?
- Pick just `{4}` → sum = 4 ✓
- Pick `{1, 3}` → sum = 4 ✓

Answer: **true**

### Why Think in Terms of Subsequences?

A subset here means any combination of elements — they don't have to be contiguous, and order doesn't matter. This is exactly the definition of a subsequence. So we're asking: does any subsequence of the array sum to `k`?

The key insight Striver teaches:

> Instead of generating ALL subsets and checking each one, we use recursion to try both choices at every index — take or not take — and stop the moment we find a valid subset.

### Step 1 — Represent in terms of (index, target)

This is the thumb rule for all DP on subsequences:

> **Always express in terms of index AND target.**

Define:

```
f(index, target) = true if there exists a subset in arr[0...index]
                   that sums to exactly 'target'
```

So the answer we want is `f(n-1, k)` — in the entire array (from index 0 to n-1), does any subset sum to k?

### Base Cases

**When target == 0:**
We've found a valid subset — the elements we've already taken sum to exactly k. No matter which index we're at, if target has become 0, return `true`.
```
if target == 0 → return true
```

**When index == 0:**
We're at the first element of the array. The only way to achieve the remaining target is if `arr[0]` equals it exactly — one element, one chance.
```
if index == 0 → return arr[0] == target
```

**Important — check target == 0 BEFORE index == 0:**
If we check index first, we might return `arr[0] == 0` which is incorrect when target is already 0. Always check target first.

### Step 2 — Explore all possibilities at (index, target)

At any index, exactly two choices exist:

**Choice 1 — Not Take:**
Skip `arr[index]`. The target doesn't change. Move to the previous index.
```
notTake = f(index - 1, target)
```

**Choice 2 — Take:**
Include `arr[index]` in our subset. The remaining target shrinks by `arr[index]`. Move to the previous index.

But we can only take `arr[index]` if it doesn't exceed the current target — taking something larger than what we need is pointless.
```
take = false
if arr[index] <= target:
    take = f(index - 1, target - arr[index])
```

### Step 3 — OR of both choices

We return true if **either** choice leads to a valid subset:
```
f(index, target) = notTake OR take
```

### The Full Recurrence

```
f(index, target):
    if target == 0       → return true
    if index == 0        → return arr[0] == target
    notTake = f(index-1, target)
    take = false
    if arr[index] <= target:
        take = f(index-1, target - arr[index])
    return notTake OR take
```

### Verifying with an Example

```
arr = [1, 2, 3, 4],  k = 4
Call: f(3, 4)

f(3, 4):
  notTake = f(2, 4)
  take    = f(2, 0) → target==0, return true ← immediately returns true

So f(3, 4) = true ✓
```

```
arr = [1, 2, 3],  k = 7
Call: f(2, 7)

f(2, 7):
  notTake = f(1, 7)
    f(1, 7): notTake = f(0, 7) → arr[0]=1 ≠ 7 → false
             take: arr[1]=2 ≤ 7 → f(0, 5) → arr[0]=1 ≠ 5 → false
             f(1, 7) = false
  take: arr[2]=3 ≤ 7 → f(1, 4)
    f(1, 4): notTake = f(0, 4) → arr[0]=1 ≠ 4 → false
             take: arr[1]=2 ≤ 4 → f(0, 2) → arr[0]=1 ≠ 2 → false
             f(1, 4) = false
  f(2, 7) = false ✓  (1+2+3=6, cannot reach 7)
```

### Overlapping Subproblems

```
arr = [1, 2, 1, 3],  k = 5
Call: f(3, 5)

f(3, 5):
  notTake → f(2, 5)
    f(2, 5): take → f(1, 4)
               f(1, 4): take → f(0, 2) ...
  take    → f(2, 2)
    f(2, 2): take → f(1, 1)
               f(1, 1): notTake → f(0, 1)
                         f(0, 1) ← ALSO called from other branches
```

`f(0, 1)` gets called from multiple branches — same subproblem, different paths to get there. **Overlapping subproblems confirmed** → DP is needed.

### DP Table Size

Two parameters:
- `index`: 0 to n-1 → **n values**
- `target`: 0 to k → **k+1 values**

dp table: **n × (k+1)**

---

# Stage 3: Coding

## Approach 1 — Pure Recursion (Brute Force)

```java
class Solution {
    public boolean isSubsetSum(int[] arr, int k) {
        int n = arr.length;
        return solve(n - 1, k, arr);
    }

    private boolean solve(int index, int target, int[] arr) {
        // Base case 1: target achieved — valid subset found
        // WHY check this first: if target is 0, we've succeeded
        //     regardless of which index we're at
        if (target == 0) return true;

        // Base case 2: only one element left
        // Can this single element give us the remaining target?
        if (index == 0) return arr[0] == target;

        // Choice 1: Not take arr[index]
        // Target stays the same, move to previous index
        boolean notTake = solve(index - 1, target, arr);

        // Choice 2: Take arr[index]
        // Only valid if arr[index] <= target
        // WHY: taking something larger than target overshoots — invalid
        boolean take = false;
        if (arr[index] <= target) {
            take = solve(index - 1, target - arr[index], arr);
        }

        // Return true if EITHER choice leads to a valid subset
        return notTake || take;
    }
}
```

**Time Complexity — O(2^n):**
At every index, the function makes 2 recursive calls — one for not-take and one for take (when valid). The recursion tree is a binary tree of depth n. The total number of nodes grows as 2^n. For n = 20, that's already over a million calls. Completely impractical for large inputs.

**Space Complexity — O(n):**
No dp array is allocated. The **recursion call stack** holds one frame per index. The deepest chain goes from index n-1 all the way down to index 0 — that is n frames simultaneously on the stack. Stack uses O(n) space.

---

## Approach 2 — Memoization (Top-Down DP)

The same subproblem like `f(0, 1)` gets called from multiple branches of the recursion tree. Store each result the first time it is computed.

**3 steps to convert recursion to memoization:**
```
Step 0: Declare dp array of size n × (k+1), initialize all to -1
        WHY int and not boolean: -1 marks "not yet computed"
        boolean has no such sentinel value — we need int (0=false, 1=true)
Step 1: Before computing, check — if dp[index][target] != -1, return it  (lookup)
Step 2: After computing, store — dp[index][target] = result              (store)
```

```java
class Solution {
    public boolean isSubsetSum(int[] arr, int k) {
        int n = arr.length;
        // Step 0: Use int dp, not boolean
        // WHY: -1 is the "not computed" sentinel; boolean can only be true/false
        int[][] dp = new int[n][k + 1];
        for (int[] row : dp) {
            Arrays.fill(row, -1);
        }
        return solve(n - 1, k, arr, dp);
    }

    private boolean solve(int index, int target, int[] arr, int[][] dp) {
        // Base case 1: target achieved
        if (target == 0) return true;

        // Base case 2: first element only
        if (index == 0) return arr[0] == target;

        // Step 1: Already computed? Return stored value instantly
        // WHY: f(2, 3) = "can arr[0..2] sum to 3?" — this never changes
        //      no matter which branch of the recursion triggered this call
        if (dp[index][target] != -1) return dp[index][target] == 1;

        // Not take
        boolean notTake = solve(index - 1, target, arr, dp);

        // Take (only if valid)
        boolean take = false;
        if (arr[index] <= target) {
            take = solve(index - 1, target - arr[index], arr, dp);
        }

        // Step 2: Store before returning
        // WHY store as int: dp is int[], so convert boolean to 0 or 1
        dp[index][target] = (notTake || take) ? 1 : 0;
        return notTake || take;
    }
}
```

**Time Complexity — O(n × k):**
Each unique (index, target) pair is computed exactly once. There are n × (k+1) unique states. For each state, O(1) work is done — compute notTake and take, then OR them. Total: O(n × k).

**Space Complexity — O(n × k) + O(n):**
Two sources of space. First, the dp array of size n × (k+1). Second, the recursion call stack which goes n levels deep before hitting a base case. Total: O(n × k) + O(n).

---

## Approach 3 — Tabulation (Bottom-Up DP)

**Direction observation:**
The recursion goes from index n-1 down to index 0. So tabulation goes the **opposite direction** — from index 0 up to index n-1. Fill small subproblems first, use them to fill bigger ones.

**Recipe:**
```
Step 1: Declare boolean dp array of size n × (k+1)
        WHY boolean now: no -1 sentinel needed in tabulation
Step 2: Fill base cases directly
        Base case 1: target == 0 → dp[i][0] = true for ALL i
        Base case 2: index == 0  → dp[0][arr[0]] = true (if arr[0] <= k)
Step 3: Fill from index=1 to n-1, target=1 to k using the recurrence
```

```java
class Solution {
    public boolean isSubsetSum(int[] arr, int k) {
        int n = arr.length;

        // Step 1: Declare boolean dp array
        // dp[i][t] = true means arr[0..i] contains a subset summing to t
        boolean[][] dp = new boolean[n][k + 1];

        // Step 2a: Base case — target == 0
        // For every index, if target is 0 we've succeeded (empty subset)
        // WHY all rows: target 0 is achievable from ANY prefix of the array
        for (int i = 0; i < n; i++) {
            dp[i][0] = true;
        }

        // Step 2b: Base case — index == 0
        // Only arr[0] is available; it achieves exactly its own value
        // WHY check arr[0] <= k: arr[0] might exceed k, then it's unreachable
        if (arr[0] <= k) {
            dp[0][arr[0]] = true;
        }

        // Step 3: Fill from index=1 to n-1
        // WHY start at 1: index 0 is already handled as base case
        for (int i = 1; i < n; i++) {
            // Target goes from 1 to k
            // WHY start at 1: target=0 is already filled as true
            for (int target = 1; target <= k; target++) {

                // Choice 1: Not take arr[i]
                // Outcome = whatever was achievable without arr[i]
                // WHY dp[i-1][target]: we skip arr[i], look at i-1
                boolean notTake = dp[i - 1][target];

                // Choice 2: Take arr[i]
                // Only valid if arr[i] <= target
                // WHY dp[i-1][target - arr[i]]: we use arr[i], remaining
                //     target shrinks, look at previous index
                boolean take = false;
                if (arr[i] <= target) {
                    take = dp[i - 1][target - arr[i]];
                }

                dp[i][target] = notTake || take;
            }
        }

        // Answer: can arr[0..n-1] form a subset summing to k?
        return dp[n - 1][k];
    }
}
```

**Dry Run:**
```
arr = [1, 2, 3, 4],  k = 4

Step 1 — Fill target=0 column:
dp[0][0]=T, dp[1][0]=T, dp[2][0]=T, dp[3][0]=T

Step 2 — Fill index=0 row:
arr[0]=1, so dp[0][1]=T
dp[0] = [T, T, F, F, F]

Step 3 — Fill i=1 (arr[1]=2):
target=1: notTake=dp[0][1]=T, take=impossible(2>1) → T
target=2: notTake=dp[0][2]=F, take=dp[0][0]=T → T
target=3: notTake=dp[0][3]=F, take=dp[0][1]=T → T
target=4: notTake=dp[0][4]=F, take=dp[0][2]=F → F
dp[1] = [T, T, T, T, F]

Step 4 — Fill i=2 (arr[2]=3):
target=1: notTake=dp[1][1]=T → T
target=2: notTake=dp[1][2]=T → T
target=3: notTake=dp[1][3]=T → T
target=4: notTake=dp[1][4]=F, take=dp[1][1]=T → T
dp[2] = [T, T, T, T, T]

Step 5 — Fill i=3 (arr[3]=4):
All targets already reachable from dp[2], so all stay T
dp[3] = [T, T, T, T, T]

Answer = dp[3][4] = true ✓
```

**Time Complexity — O(n × k):**
Two nested loops — outer runs n-1 times (index 1 to n-1), inner runs k times (target 1 to k). Each cell does O(1) work. Total: O(n × k). Same as memoization but with no function call overhead and no recursion stack.

**Space Complexity — O(n × k):**
Only the dp array of size n × (k+1). No recursion stack at all.

---

## Approach 4 — Space Optimization (The Final Form)

Look at the tabulation recurrence:

```
dp[i][target] = dp[i-1][target] || dp[i-1][target - arr[i]]
```

To compute row `i`, we only ever look at row `i-1`. After computing row `i`, row `i-1` is never needed again.

So instead of the full n × (k+1) array, keep just **one boolean array of size k+1** — the "previous" row. Compute the current row into a new array. After each row, current becomes the new previous.

**One critical subtlety — loop direction:**

When we compute `dp[i][target]`, we need `dp[i-1][target - arr[i]]`. If we update in-place (left to right on the same array), by the time we compute `dp[target]`, we might have already overwritten `dp[target - arr[i]]` with the current row's value — accidentally allowing the same element to be used twice.

To avoid this, iterate **right to left** (from target k down to arr[i]). This ensures that when we compute `dp[target]`, the value `dp[target - arr[i]]` still reflects the previous row.

```java
class Solution {
    public boolean isSubsetSum(int[] arr, int k) {
        int n = arr.length;

        // prev[t] = true means some subset of the "previous" row's elements
        //           can sum to exactly t
        // Initialize with the base case for index=0
        boolean[] prev = new boolean[k + 1];

        // Base case: target=0 is always achievable (empty subset)
        prev[0] = true;

        // Base case: index=0, arr[0] achieves exactly arr[0]
        if (arr[0] <= k) {
            prev[arr[0]] = true;
        }

        // Fill from index=1 to n-1
        for (int i = 1; i < n; i++) {
            // curr[t] = can arr[0..i] form a subset summing to t?
            boolean[] curr = new boolean[k + 1];

            // target=0 is always achievable
            curr[0] = true;

            for (int target = 1; target <= k; target++) {
                // Not take arr[i]
                // WHY prev[target]: skip arr[i], look at previous row
                boolean notTake = prev[target];

                // Take arr[i] (only if valid)
                // WHY prev[target - arr[i]]: use arr[i], reduce target,
                //     look at previous row's answer for reduced target
                boolean take = false;
                if (arr[i] <= target) {
                    take = prev[target - arr[i]];
                }

                curr[target] = notTake || take;
            }

            // Slide forward: current row becomes new previous
            // WHY: next iteration needs this row as its "previous"
            prev = curr;
        }

        // prev now holds the last computed row (index n-1)
        return prev[k];
    }
}
```

**Time Complexity — O(n × k):**
Still two nested loops with the same iteration count. No change in time complexity — O(n × k).

**Space Complexity — O(k):**
No n × (k+1) array. No recursion stack. Just two boolean arrays of size k+1 — `prev` and `curr`. Memory depends only on the target value k, not the number of elements n.

---

## Final Comparison

| Approach | Time | Space | Notes |
|---|---|---|---|
| Pure Recursion | O(2^n) | O(n) stack | Exponential — never use |
| Memoization | O(n × k) | O(n×k) + O(n) stack | Good interview starting point |
| Tabulation | O(n × k) | O(n × k) | Better — eliminates stack |
| Space Optimization | O(n × k) | **O(k)** | Best — submit this |

---

## The Key Takeaway

```
┌──────────────────────────────────────────────────────────────────┐
│  The universal template for ALL 0/1 Knapsack problems:           │
│                                                                  │
│  State: (index, target)                                          │
│  At every index, exactly two choices:                            │
│    Not Take: f(index-1, target)                                  │
│    Take:     f(index-1, target - arr[index])                     │
│              — only valid if arr[index] <= target                │
│                                                                  │
│  What changes across problems in this pattern:                   │
│    Subset Sum      → return true/false (existence check)         │
│    Target Sum      → count the ways (sum of both choices)        │
│    0/1 Knapsack    → maximize value (max of both choices)        │
│    Partition Equal → reduce to subset sum with target = sum/2    │
│    Last Stone II   → minimize difference (same reduction)        │
│                                                                  │
│  Base case rules for 0/1 Knapsack:                               │
│    target == 0   → always return true (empty subset valid)       │
│    index == 0    → return arr[0] == target                       │
│    Check target BEFORE index — order matters.                    │
│                                                                  │
│  Memoization: use int dp (not boolean) because -1 is the         │
│  "not computed" sentinel — boolean has no such third value.      │
│  Tabulation: switch back to boolean — no sentinel needed.        │
│                                                                  │
│  Space optimization: use prev + curr arrays of size k+1.         │
│  prev[target - arr[i]] safely references the previous row        │
│  because we always read from prev, write to curr.                │
└──────────────────────────────────────────────────────────────────┘
```