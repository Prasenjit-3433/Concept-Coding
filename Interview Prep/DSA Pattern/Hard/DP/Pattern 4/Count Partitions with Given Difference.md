# Count Partitions with Given Difference (GFG / Coding Ninjas)

---

# Stage 1: Identification

**Step 1 — Which topic?**

You are given an array of non-negative integers and a difference `d`. You need to partition the array into two subsets S1 and S2 such that S1 ≥ S2 and S1 - S2 = d. Count the number of such partitions.

The moment you see **"count the number of partitions"** — that is the signal. To count all valid partitions, you must **try all possible ways**. And whenever you try all possible ways, **Recursion** is your first thought. And when that recursion has overlapping subproblems — it becomes **Dynamic Programming**.

**Step 2 — Which pattern?**

Each element goes into exactly one of two subsets — either taken into S1 or left for S2. Each element used exactly once. There is a target-related constraint. This is the **0/1 Knapsack** pick/not-pick structure.

That is **Pattern 4: 0/1 Knapsack**.

**Step 3 — Which key concept?**

**Problem Reduction — reduce to Count Subsets with Sum K.**

This problem does not require any new recurrence. The key is recognizing the mathematical equivalence and converting the difference constraint into a target sum constraint. Once reduced, it becomes the previous problem (Count Subsets with Sum K) with a modified target.

---

# Stage 2: Intuition Building

### The Mathematical Reduction

You have two subsets with sums S1 and S2. You know:

```
S1 + S2 = totalSum     ... (1)   (both subsets together = full array)
S1 - S2 = d            ... (2)   (given condition)
```

Add equations (1) and (2):

```
2 × S1 = totalSum + d
S1 = (totalSum + d) / 2
```

Subtract equation (2) from (1):

```
2 × S2 = totalSum - d
S2 = (totalSum - d) / 2
```

Since S1 ≥ S2, and the problem asks us to count partitions with S1 - S2 = d, it is sufficient to **find the number of subsets summing to S2 = (totalSum - d) / 2**. Once S2 is fixed, S1 is automatically determined as the remaining elements.

So the entire problem reduces to:

```
Count Subsets with Sum K  (where K = (totalSum - d) / 2)
```

This is exactly the previous problem — with one modified target.

### Two Edge Cases Before Any DP

**Edge Case 1 — (totalSum - d) must be non-negative:**

The array contains non-negative integers, so the sum of any subset is ≥ 0. This means S2 ≥ 0, which means `(totalSum - d) / 2 ≥ 0`, which means `totalSum - d ≥ 0` i.e. `totalSum ≥ d`. If `totalSum < d`, no valid partition exists — return 0.

**Edge Case 2 — (totalSum - d) must be even:**

All array elements are integers, so all subset sums are integers. S2 must be an integer, so `(totalSum - d)` must be even. If it is odd, no valid partition exists — return 0.

### Handling Zeros in the Array

This problem explicitly states elements can be **≥ 0** (unlike the previous problem which guaranteed elements ≥ 1). This breaks the earlier base case logic.

**Why zeros break the previous solution:**

In Count Subsets with Sum K, we had:
```
if (sum == 0) return 1;   ← stops recursion immediately
```

But if `arr[index] = 0` and `sum = 0`, there are **two** valid choices — pick the zero or don't pick it — but the early return catches only one. We miss the other.

**The fix Striver teaches — remove the early `sum == 0` return and let recursion reach index 0:**

New base cases at index == 0:
```
if arr[0] == 0 AND sum == 0 → return 2   (pick zero OR don't pick zero)
if sum == 0                  → return 1   (don't pick arr[0], which is non-zero)
if arr[0] == sum             → return 1   (pick arr[0] exactly)
return 0                                  (no valid single-element subset)
```

Why return 2 in the first case: if `arr[0] = 0` and `sum = 0`, picking zero keeps sum at 0 (valid), and not picking gives empty subset with sum 0 (also valid). Two separate subsets.

### Verifying with Example

```
arr = [5, 2, 6, 4],  d = 3
totalSum = 5 + 2 + 6 + 4 = 17
target S2 = (17 - 3) / 2 = 14 / 2 = 7

Find subsets of arr summing to 7:
{5, 2} = 7    → S2 = {5,2}, S1 = {6,4} = 10, diff = 10-7 = 3 ✓
{7}    = ?    → no element 7
{2,... nope
{6,... nope  
{3,4}  = nope

Wait let me recheck: {5,2}=7 ✓, {3}? no 3, {1,6}? no 1...

Only {5, 2} gives sum 7

Answer = 1... but expected = 3.

Let me re-examine with arr = [5, 2, 6, 4] more carefully:
Subsets summing to 7: {5,2}, {3}? no, {7}? no.
Only {5,2}. Hmm.

Let me try d=2: arr = [1,1,2,3], d=2
totalSum = 7, target = (7-2)/2 = 2.5 → odd, return 0.

Let me use the example from the transcript more carefully:
arr = [5,2,6,4], d=3 → answer should be 1 subset {5,2}
pairing: S2={5,2}=7, S1={6,4}=10, diff=3 ✓
```

---

# Stage 3: Coding

## Approach 1 — Memoization (Top-Down DP)

Since the problem reduces to Count Subsets with Sum K, we take that solution and apply:
1. The zero-element fix to the base cases
2. The mathematical reduction to compute the target
3. A modulo operation since the count can be very large

```java
class Solution {
    static final int MOD = (int) 1e9 + 7;

    public int countPartitions(int n, int d, int[] arr) {
        // Step 1: Compute total sum
        int totalSum = 0;
        for (int num : arr) totalSum += num;

        // Edge case 1: (totalSum - d) must be non-negative
        // WHY: S2 = (totalSum - d) / 2 must be >= 0
        //      since all elements are non-negative
        if (totalSum - d < 0) return 0;

        // Edge case 2: (totalSum - d) must be even
        // WHY: S2 must be an integer — no fractional subsets
        if ((totalSum - d) % 2 != 0) return 0;

        int target = (totalSum - d) / 2;

        // Delegate to Count Subsets with modified target
        int[][] dp = new int[n][target + 1];
        for (int[] row : dp) Arrays.fill(row, -1);
        return solve(n - 1, target, arr, dp);
    }

    private int solve(int index, int sum, int[] arr, int[][] dp) {
        // Base case: reached index 0 — evaluate the single remaining element
        // WHY handle all sub-cases here instead of early sum==0 return:
        //     if arr[0]==0 and sum==0, we have TWO valid choices (pick or not pick)
        //     an early return at sum==0 would count only ONE of them
        if (index == 0) {
            if (arr[0] == 0 && sum == 0) return 2;  // pick zero OR don't pick zero
            if (sum == 0 || arr[0] == sum) return 1; // exactly one valid way
            return 0;                                 // no valid way
        }

        // Lookup: already computed?
        if (dp[index][sum] != -1) return dp[index][sum];

        // Choice 1: Not Pick arr[index]
        int notPick = solve(index - 1, sum, arr, dp) % MOD;

        // Choice 2: Pick arr[index] — only if it doesn't exceed sum
        int pick = 0;
        if (arr[index] <= sum) {
            pick = solve(index - 1, sum - arr[index], arr, dp) % MOD;
        }

        // Store and return — ADD because we are counting
        dp[index][sum] = (notPick + pick) % MOD;
        return dp[index][sum];
    }
}
```

**Time Complexity — O(n × target):**
Each unique (index, sum) pair is computed exactly once. There are n × (target+1) unique states where target = (totalSum - d) / 2. For each state, O(1) work is done. Total: O(n × target).

**Space Complexity — O(n × target) + O(n):**
Two sources of space. First, the dp array of size n × (target+1) — that is O(n × target). Second, the recursion call stack which goes n levels deep. Total: O(n × target) + O(n).

---

## Approach 2 — Tabulation (Bottom-Up DP)

**Direction:** Recursion goes from index n-1 downward to index 0. Tabulation goes the **opposite direction** — from index 0 upward to index n-1.

**Base case analysis with zeros:**

At index 0, four scenarios to handle for the tabulation table:

```
arr[0] == 0:
    dp[0][0] = 2   (pick zero OR don't pick zero — two ways to form sum 0)

arr[0] != 0:
    dp[0][0] = 1           (don't pick arr[0] — one way to form sum 0)
    dp[0][arr[0]] = 1      (pick arr[0]        — one way to form sum arr[0])
```

Notice that when `arr[0] != 0`, both `dp[0][0] = 1` AND `dp[0][arr[0]] = 1` apply simultaneously. When `arr[0] == 0`, the two cases collapse into a single cell `dp[0][0] = 2`.

```java
class Solution {
    static final int MOD = (int) 1e9 + 7;

    public int countPartitions(int n, int d, int[] arr) {
        int totalSum = 0;
        for (int num : arr) totalSum += num;

        // Edge case 1: target must be non-negative
        if (totalSum - d < 0) return 0;

        // Edge case 2: target must be an integer (even difference)
        if ((totalSum - d) % 2 != 0) return 0;

        int target = (totalSum - d) / 2;

        // Step 1: Declare dp array
        // dp[i][s] = number of subsets in arr[0..i] summing to exactly s
        int[][] dp = new int[n][target + 1];

        // Step 2: Fill base cases for index = 0
        // WHY separate arr[0]==0 case: if arr[0] is zero, both
        //     "pick" and "not pick" give sum 0 → two valid subsets for sum=0
        if (arr[0] == 0) {
            dp[0][0] = 2;   // pick zero OR don't pick zero
        } else {
            dp[0][0] = 1;   // don't pick arr[0] → sum 0 achieved in 1 way
            // pick arr[0] → sum arr[0] achieved in 1 way (if within bounds)
            if (arr[0] <= target) {
                dp[0][arr[0]] = 1;
            }
        }

        // Step 3: Fill from index=1 to n-1
        for (int i = 1; i < n; i++) {
            for (int s = 0; s <= target; s++) {

                // Choice 1: Not Pick arr[i]
                // WHY dp[i-1][s]: skip arr[i], look at previous row same sum
                int notPick = dp[i - 1][s];

                // Choice 2: Pick arr[i] — only if it doesn't exceed s
                // WHY dp[i-1][s - arr[i]]: took arr[i], remaining sum shrinks,
                //     check previous row for that reduced sum
                int pick = 0;
                if (arr[i] <= s) {
                    pick = dp[i - 1][s - arr[i]];
                }

                dp[i][s] = (notPick + pick) % MOD;
            }
        }

        // Answer: count of subsets in full array summing to target
        return dp[n - 1][target];
    }
}
```

**Dry Run:**

```
arr = [5, 2, 6, 4],  d = 3
totalSum = 17,  target = (17 - 3) / 2 = 7

Step 1 — Fill index=0 (arr[0]=5, non-zero):
dp[0][0] = 1
dp[0][5] = 1
dp[0] = [1, 0, 0, 0, 0, 1, 0, 0]
          0  1  2  3  4  5  6  7

Step 2 — Fill i=1 (arr[1]=2):
s=0: notPick=1, pick=N/A(2>0) → 1
s=1: notPick=0, pick=N/A(2>1) → 0
s=2: notPick=0, pick=dp[0][0]=1 → 1
s=3: notPick=0, pick=dp[0][1]=0 → 0
s=4: notPick=0, pick=dp[0][2]=0 → 0
s=5: notPick=1, pick=dp[0][3]=0 → 1
s=6: notPick=0, pick=dp[0][4]=0 → 0
s=7: notPick=0, pick=dp[0][5]=1 → 1

dp[1] = [1, 0, 1, 0, 0, 1, 0, 1]

Step 3 — Fill i=2 (arr[2]=6):
s=0: notPick=1, pick=N/A → 1
s=1: notPick=0, pick=N/A → 0
s=2: notPick=1, pick=N/A → 1
s=3: notPick=0, pick=N/A → 0
s=4: notPick=0, pick=N/A → 0
s=5: notPick=1, pick=N/A → 1
s=6: notPick=0, pick=dp[1][0]=1 → 1
s=7: notPick=1, pick=dp[1][1]=0 → 1

dp[2] = [1, 0, 1, 0, 0, 1, 1, 1]

Step 4 — Fill i=3 (arr[3]=4):
s=0: notPick=1, pick=N/A → 1
s=1: notPick=0, pick=N/A → 0
s=2: notPick=1, pick=N/A → 1
s=3: notPick=0, pick=N/A → 0
s=4: notPick=0, pick=dp[2][0]=1 → 1
s=5: notPick=1, pick=dp[2][1]=0 → 1
s=6: notPick=1, pick=dp[2][2]=1 → 2
s=7: notPick=1, pick=dp[2][3]=0 → 1

dp[3] = [1, 0, 1, 0, 1, 1, 2, 1]

Answer = dp[3][7] = 1 ✓
```

**Time Complexity — O(n × target):**
Two nested loops — outer runs n-1 times, inner runs target+1 times. Each cell does O(1) work. Total: O(n × target) where target = (totalSum - d) / 2.

**Space Complexity — O(n × target):**
Only the dp array of size n × (target+1). No recursion stack at all.

---

## Approach 3 — Space Optimization (The Final Form)

```
dp[i][s] = dp[i-1][s] + dp[i-1][s - arr[i]]
```

Row `i` only depends on row `i-1`. Replace the full 2D table with two 1D arrays of size target+1.

```java
class Solution {
    static final int MOD = (int) 1e9 + 7;

    public int countPartitions(int n, int d, int[] arr) {
        int totalSum = 0;
        for (int num : arr) totalSum += num;

        if (totalSum - d < 0) return 0;
        if ((totalSum - d) % 2 != 0) return 0;

        int target = (totalSum - d) / 2;

        // prev[s] = number of subsets in "previous" elements summing to s
        // Initialize with base case for index=0
        int[] prev = new int[target + 1];

        // Base case: index=0
        // WHY separate zero check: picking a zero element or not both
        //     give sum=0 → two valid subsets instead of one
        if (arr[0] == 0) {
            prev[0] = 2;
        } else {
            prev[0] = 1;
            if (arr[0] <= target) {
                prev[arr[0]] = 1;
            }
        }

        // Fill from index=1 to n-1
        for (int i = 1; i < n; i++) {
            // curr[s] = number of subsets in arr[0..i] summing to s
            int[] curr = new int[target + 1];

            for (int s = 0; s <= target; s++) {
                // Not Pick arr[i]
                // WHY prev[s]: skip arr[i], answer from previous row
                int notPick = prev[s];

                // Pick arr[i] — only if it fits within s
                // WHY prev[s - arr[i]]: used arr[i], check previous row
                //     for whether remaining sum was achievable
                int pick = 0;
                if (arr[i] <= s) {
                    pick = prev[s - arr[i]];
                }

                curr[s] = (notPick + pick) % MOD;
            }

            // Slide forward: current row becomes new previous row
            // WHY: next iteration needs this row as its "row above"
            prev = curr;
        }

        // prev now holds answers for the full array
        return prev[target];
    }
}
```

**Time Complexity — O(n × target):**
Two nested loops with the same iteration count as tabulation. Total: O(n × target). No change.

**Space Complexity — O(target):**
No n × (target+1) array. No recursion stack. Just two arrays of size target+1 — `prev` and `curr`. Memory depends only on the target value, not n.

---

## Final Comparison

| Approach | Time | Space | Notes |
|---|---|---|---|
| Memoization | O(n × target) | O(n×target) + O(n) stack | Good interview starting point |
| Tabulation | O(n × target) | O(n × target) | Better — eliminates stack |
| Space Optimization | O(n × target) | **O(target)** | Best — submit this |

where target = (totalSum - d) / 2

---

## The Key Takeaway

```
┌──────────────────────────────────────────────────────────────────┐
│  The reduction chain in Pattern 4 so far:                        │
│                                                                  │
│  Subset Sum Equal to Target     → existence check                │
│      ↓ change OR to ADD                                          │
│  Count Subsets with Sum K       → counting                       │
│      ↓ change target mathematically                              │
│  Count Partitions with Diff D   → counting with derived target   │
│                                                                  │
│  The mathematical reduction:                                     │
│  S1 - S2 = d  and  S1 + S2 = totalSum                            │
│      → S2 = (totalSum - d) / 2                                   │
│  Now just count subsets summing to S2.                           │
│                                                                  │
│  Two elimination checks before any DP:                           │
│  1. (totalSum - d) must be ≥ 0                                   │
│     (subset sums cannot be negative with non-negative arrays)    │
│  2. (totalSum - d) must be even                                  │
│     (subset sums are always integers — no fractions)             │
│                                                                  │
│  The zero-element fix (critical when arr[i] >= 0):               │
│  Remove early "if sum == 0 return 1" base case.                  │
│  Let recursion reach index 0, then handle:                       │
│    arr[0]==0 AND sum==0 → return 2 (pick or not pick zero)       │
│    sum==0 OR arr[0]==sum → return 1 (exactly one valid way)      │
│    otherwise             → return 0 (no valid way)               │
│                                                                  │
│  In tabulation, the same zero fix becomes:                       │
│    arr[0]==0 → prev[0] = 2                                       │
│    arr[0]!=0 → prev[0] = 1, prev[arr[0]] = 1 (if in bounds)      │
└──────────────────────────────────────────────────────────────────┘
```