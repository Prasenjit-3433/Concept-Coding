No worries! Let me write the complete solution note for **Count Subsets with Sum K** now.

---

# Count Subsets with Sum K (GFG / Coding Ninjas)

---

# Stage 1: Identification

**Step 1 — Which topic?**

You are given an array of positive integers and a target `k`. You need to count the **number of subsets** whose elements sum to exactly `k`.

The moment you see **"count the number of ways"** — that is the signal. To count all ways, you must **try all possible ways**. And whenever you try all possible ways, **Recursion** is your first thought. And when that recursion has overlapping subproblems — it becomes **Dynamic Programming**.

**Step 2 — Which pattern?**

You have a list of items (array elements), each used **at most once** — you either take it or skip it. There is a target constraint (sum = k). You want to **count** the number of valid selections.

That is **Pattern 4: 0/1 Knapsack**.

**Step 3 — Which key concept?**

This is a **direct variation of Subset Sum Equal to Target** (the previous problem). The entire structure — state definition, recurrence, base cases, memoization, tabulation, space optimization — is identical. The **only change** is:

```
Subset Sum Equal to Target  →  return true / false   (existence check)
Count Subsets with Sum K    →  return count           (counting check)
```

This change propagates to exactly two places:
- **Base case:** instead of returning `true`, return `1` (one valid way found)
- **Combining choices:** instead of `notTake || take` (OR), use `notTake + take` (ADD)

Striver's rule from Recursion Lecture 7:
> Whenever a problem says **"count the number of ways"**, in the base case return `1` when the condition is satisfied and `0` otherwise. Then **add up** all choices instead of OR-ing them.

---

# Stage 2: Intuition Building

### The Problem Setup

```
arr = [1, 2, 2, 3],  target k = 3
```

How many subsets sum to 3?

- `{1, 2}` — picking index 0 and index 1 → sum = 3 ✓
- `{1, 2}` — picking index 0 and index 2 → sum = 3 ✓ (different elements, same values)
- `{3}` — picking index 3 → sum = 3 ✓

Total = **3 subsets**

Even though the first two look the same as values, they are formed by picking **different elements** at different indices. The problem counts them as separate.

### Step 1 — Represent in terms of (index, sum)

Define:

```
f(index, sum) = number of subsets in arr[0...index]
                that sum to exactly 'sum'
```

So the answer we want is `f(n-1, k)`.

### Step 2 — Do all possible things at (index, sum)

At every index, exactly two choices:

**Choice 1 — Not Pick:**
Skip `arr[index]`. The sum doesn't change. Move to the previous index.
```
notTake = f(index - 1, sum)
```

**Choice 2 — Pick:**
Take `arr[index]` into the subset. The remaining sum shrinks. Move to the previous index.

But we can only pick `arr[index]` if it does not exceed the current sum.
```
take = 0
if arr[index] <= sum:
    take = f(index - 1, sum - arr[index])
```

### Step 3 — Add because we are counting ways

We **add** both choices (not OR — we are counting, not checking existence):

```
f(index, sum) = notTake + take
```

### Base Cases

**When sum == 0:**
The remaining target is zero — we have found a valid subset. Count it as **1**.

```
if sum == 0 → return 1
```

This must be checked **before** the index check. Even if we are at index 5 with sum = 0, it is a valid subset — we simply stopped picking.

**When index == 0:**
Only one element left. It can form a valid subset only if it exactly equals the remaining sum.

```
if index == 0 → return arr[0] == sum ? 1 : 0
```

### Visualizing the Recursion Tree

```
arr = [1, 3, 2],  k = 3

f(2, 3)
├── NOT PICK arr[2]=2  → f(1, 3)
│   ├── NOT PICK arr[1]=3 → f(0, 3)
│   │       arr[0]=1 ≠ 3 → return 0
│   └── PICK arr[1]=3   → f(0, 0)
│               sum==0  → return 1     ✓  {3}
│   → 0 + 1 = 1
│
└── PICK arr[2]=2       → f(1, 1)
    ├── NOT PICK arr[1]=3 → f(0, 1)
    │       arr[0]=1 == 1 → return 1   ✓  {1, 2}
    └── PICK arr[1]=3   → 3 > 1, CANNOT PICK → 0
    → 1 + 0 = 1

f(2, 3) = 1 + 1 = 2
```

So there are 2 subsets: `{3}` and `{1, 2}` ✓

### Are there overlapping subproblems?

With a larger array, the same `f(index, sum)` state gets called from multiple branches. For example, `f(1, 1)` might be reached via taking index 3, or via taking index 2, or via any combination that reduces the sum to 1 at index 1. **Overlapping subproblems confirmed** → DP is needed.

### DP Table Size

Two parameters:
- `index`: 0 to n-1 → **n values**
- `sum`: 0 to k → **k+1 values**

dp table: **n × (k+1)**

---

# Stage 3: Coding

## Approach 1 — Pure Recursion (Brute Force)

```java
class Solution {
    public int countSubsets(int[] arr, int k) {
        int n = arr.length;
        return solve(n - 1, k, arr);
    }

    private int solve(int index, int sum, int[] arr) {
        // Base case 1: sum achieved — one valid subset found
        // WHY check sum first: even if index > 0, if sum is 0
        // we have a valid subset. Must check before index == 0
        // otherwise f(0, 0) would return arr[0]==0 which could be wrong
        if (sum == 0) return 1;

        // Base case 2: only one element left
        // Can this single element exactly form the remaining sum?
        if (index == 0) return arr[0] == sum ? 1 : 0;

        // Choice 1: Not Pick — skip arr[index]
        // Target stays the same, move to previous index
        int notPick = solve(index - 1, sum, arr);

        // Choice 2: Pick — take arr[index] into the subset
        // Only valid if arr[index] <= sum (cannot overshoot target)
        // WHY: if arr[index] > sum, picking it makes sum go negative
        //      which is meaningless for positive integer arrays
        int pick = 0;
        if (arr[index] <= sum) {
            pick = solve(index - 1, sum - arr[index], arr);
        }

        // ADD both choices — we are counting, not checking existence
        // WHY add and not OR: every valid path through notPick
        // and every valid path through pick are separate subsets
        return notPick + pick;
    }
}
```

**Time Complexity — O(2^n):**
At every index the function makes 2 recursive calls — one for notPick and one for pick (when valid). The recursion tree is a binary tree of depth n. Total nodes grow as 2^n. For n = 20 that is already over a million calls — completely impractical.

**Space Complexity — O(n):**
No dp array is allocated. The **recursion call stack** holds one frame per index. The deepest chain goes from index n-1 all the way down to index 0 — that is n frames simultaneously. Stack uses O(n) space.

---

## Approach 2 — Memoization (Top-Down DP)

The same state `f(index, sum)` gets called from multiple branches. Store each result the first time it is computed.

**3 steps to convert recursion to memoization:**
```
Step 0: Declare dp array of size n × (k+1), initialize all to -1
Step 1: Before computing, check — if dp[index][sum] != -1, return it  (lookup)
Step 2: After computing, store — dp[index][sum] = result              (store)
```

```java
class Solution {
    public int countSubsets(int[] arr, int k) {
        int n = arr.length;
        // Step 0: dp[index][sum] = -1 means not yet computed
        int[][] dp = new int[n][k + 1];
        for (int[] row : dp) {
            Arrays.fill(row, -1);
        }
        return solve(n - 1, k, arr, dp);
    }

    private int solve(int index, int sum, int[] arr, int[][] dp) {
        // Base case 1: sum achieved — one valid subset found
        if (sum == 0) return 1;

        // Base case 2: only one element left
        if (index == 0) return arr[0] == sum ? 1 : 0;

        // Step 1: Already computed? Return stored value instantly
        // WHY: f(1, 2) = count of subsets in arr[0..1] summing to 2
        //      This never changes no matter which branch triggered this call
        if (dp[index][sum] != -1) return dp[index][sum];

        // Choice 1: Not Pick
        int notPick = solve(index - 1, sum, arr, dp);

        // Choice 2: Pick (only if valid)
        int pick = 0;
        if (arr[index] <= sum) {
            pick = solve(index - 1, sum - arr[index], arr, dp);
        }

        // Step 2: Store before returning
        // WHY: next time solve(index, sum) is called, answer is ready in O(1)
        dp[index][sum] = notPick + pick;
        return dp[index][sum];
    }
}
```

**Time Complexity — O(n × k):**
Each unique (index, sum) pair is computed exactly once. There are n × (k+1) unique states. For each state, O(1) work is done — compute notPick and pick, then add them. Total: O(n × k).

**Space Complexity — O(n × k) + O(n):**
Two sources of space. First, the dp array of size n × (k+1) — that is O(n × k). Second, the **recursion call stack** — even with memoization, the first chain of calls still goes n levels deep before hitting a base case. So stack uses another O(n). Total: O(n × k) + O(n).

---

## Approach 3 — Tabulation (Bottom-Up DP)

**Direction observation:**
The recursion goes from index n-1 **downward** to index 0. So tabulation goes the **opposite direction** — from index 0 **upward** to index n-1. Fill base cases first, use them to fill bigger answers.

**The recipe:**
```
Step 1: Declare dp array of size n × (k+1)
Step 2: Fill base cases
        Base case 1: sum == 0 → dp[i][0] = 1 for ALL i
        Base case 2: index == 0 → dp[0][arr[0]] = 1 (if arr[0] <= k)
Step 3: Fill from index=1 to n-1, sum=1 to k using the recurrence
```

**One subtle point about the two base cases overlapping:**

What if `arr[0] == 0`? Then `dp[0][0]` should be 2 — once for picking the zero element and once for not picking it. However, the constraints say positive integers, so `arr[0] >= 1` and this overlap doesn't arise here. We will handle the zero-element case separately when discussing the negative numbers extension at the end.

```java
class Solution {
    public int countSubsets(int[] arr, int k) {
        int n = arr.length;

        // Step 1: Declare dp array
        // dp[i][s] = number of subsets in arr[0..i] summing to exactly s
        int[][] dp = new int[n][k + 1];

        // Step 2a: Base case — sum == 0
        // For every index, the empty subset always gives sum 0
        // WHY all rows: regardless of which prefix you look at,
        //               not picking any element gives sum 0 — that's 1 way
        for (int i = 0; i < n; i++) {
            dp[i][0] = 1;
        }

        // Step 2b: Base case — index == 0
        // Only arr[0] is available; it forms sum arr[0] in exactly 1 way
        // WHY guard arr[0] <= k: if arr[0] > k, it exceeds our dp table width
        if (arr[0] <= k) {
            dp[0][arr[0]] = 1;
        }

        // Step 3: Fill from index=1 to n-1
        // WHY start at 1: index 0 is already filled as base case
        for (int i = 1; i < n; i++) {
            // WHY sum starts at 0: sum=0 column is already filled,
            //     but running the loop from 0 is harmless (notPick of dp[i-1][0]
            //     is 1, pick path is not taken since arr[i] > 0)
            //     Starting from 1 is cleaner to avoid overwriting base cases
            for (int s = 0; s <= k; s++) {

                // Choice 1: Not Pick arr[i]
                // WHY dp[i-1][s]: skip arr[i], look at the row above
                int notPick = dp[i - 1][s];

                // Choice 2: Pick arr[i] — only if it fits within s
                // WHY dp[i-1][s - arr[i]]: took arr[i], remaining sum
                //     shrinks, check row above for that reduced sum
                int pick = 0;
                if (arr[i] <= s) {
                    pick = dp[i - 1][s - arr[i]];
                }

                dp[i][s] = notPick + pick;
            }
        }

        // Answer: count of subsets in the full array summing to k
        return dp[n - 1][k];
    }
}
```

**Dry Run:**

```
arr = [1, 2, 2, 3],  k = 3

Step 1 — Fill sum=0 column (all 1s):
dp[0][0]=1, dp[1][0]=1, dp[2][0]=1, dp[3][0]=1

Step 2 — Fill index=0 row (arr[0]=1):
dp[0][1] = 1
dp[0] = [1, 1, 0, 0]

Step 3 — Fill i=1 (arr[1]=2):
s=0: notPick=dp[0][0]=1, pick=N/A(2>0) → 1
s=1: notPick=dp[0][1]=1, pick=N/A(2>1) → 1
s=2: notPick=dp[0][2]=0, pick=dp[0][0]=1 → 1
s=3: notPick=dp[0][3]=0, pick=dp[0][1]=1 → 1
dp[1] = [1, 1, 1, 1]

Step 4 — Fill i=2 (arr[2]=2):
s=0: notPick=dp[1][0]=1, pick=N/A → 1
s=1: notPick=dp[1][1]=1, pick=N/A → 1
s=2: notPick=dp[1][2]=1, pick=dp[1][0]=1 → 2
s=3: notPick=dp[1][3]=1, pick=dp[1][1]=1 → 2
dp[2] = [1, 1, 2, 2]

Step 5 — Fill i=3 (arr[3]=3):
s=0: notPick=dp[2][0]=1, pick=N/A → 1
s=1: notPick=dp[2][1]=1, pick=N/A → 1
s=2: notPick=dp[2][2]=2, pick=N/A → 2
s=3: notPick=dp[2][3]=2, pick=dp[2][0]=1 → 3
dp[3] = [1, 1, 2, 3]

Answer = dp[3][3] = 3 ✓
```

**Time Complexity — O(n × k):**
Two nested loops — outer runs n-1 times, inner runs k+1 times. Each cell does O(1) work. Total: O(n × k). Same as memoization but with no function call overhead and no recursion stack.

**Space Complexity — O(n × k):**
Only the dp array of size n × (k+1). No recursion stack at all.

---

## Approach 4 — Space Optimization (The Final Form)

Look at the tabulation recurrence:

```
dp[i][s] = dp[i-1][s] + dp[i-1][s - arr[i]]
```

To compute row `i`, we only ever look at row `i-1`. After computing row `i`, row `i-1` is never needed again.

So instead of the full n × (k+1) array, keep just **one array of size k+1** — the "previous" row. Compute the current row into a fresh array. After each row, current becomes the new previous.

```java
class Solution {
    public int countSubsets(int[] arr, int k) {
        int n = arr.length;

        // prev[s] = number of subsets in the "previous" elements
        //           that sum to exactly s
        // Initialize with base case for index=0
        int[] prev = new int[k + 1];

        // Base case: sum=0 is always achievable (empty subset)
        prev[0] = 1;

        // Base case: index=0, arr[0] achieves exactly arr[0]
        // WHY guard arr[0] <= k: arr[0] might exceed k causing OOB
        if (arr[0] <= k) {
            prev[arr[0]] = 1;
        }

        // Fill from index=1 to n-1
        for (int i = 1; i < n; i++) {
            // curr[s] = number of subsets in arr[0..i] summing to s
            int[] curr = new int[k + 1];

            // sum=0 is always achievable (empty subset)
            curr[0] = 1;

            for (int s = 1; s <= k; s++) {
                // Not Pick arr[i]
                // WHY prev[s]: skip arr[i], look at previous row
                int notPick = prev[s];

                // Pick arr[i] — only if it fits
                // WHY prev[s - arr[i]]: used arr[i], check previous row
                //     for whether remaining sum was achievable
                int pick = 0;
                if (arr[i] <= s) {
                    pick = prev[s - arr[i]];
                }

                curr[s] = notPick + pick;
            }

            // Slide forward: current row becomes new previous row
            // WHY: next iteration needs this row as its "row above"
            prev = curr;
        }

        // prev now holds the answers for the full array
        return prev[k];
    }
}
```

**Time Complexity — O(n × k):**
Two nested loops with the same iteration count as tabulation. No change in time complexity — O(n × k).

**Space Complexity — O(k):**
No n × (k+1) array. No recursion stack. Just two arrays of size k+1 — `prev` and `curr`. Memory depends only on the target k, not the number of elements n. That is the key win here.

---

## Final Comparison

| Approach | Time | Space | Notes |
|---|---|---|---|
| Pure Recursion | O(2^n) | O(n) stack | Exponential — never use |
| Memoization | O(n × k) | O(n×k) + O(n) stack | Good interview starting point |
| Tabulation | O(n × k) | O(n × k) | Better — eliminates stack |
| Space Optimization | O(n × k) | **O(k)** | Best — submit this |

---

## What If the Array Contains Zeros?

Striver raises an important edge case in the lecture. The current solution handles positive integers. If `arr[i] = 0` is possible, the base case logic breaks down.

**Why it breaks:** When `arr[0] = 0` and `sum = 0`, both base cases fire simultaneously — the `sum == 0` base case returns 1, but we can also choose to pick or not pick the zero element, giving 2 valid subsets for `sum = 0`. The current code only returns 1.

**The fix — modify the base cases:**

```
if index == 0:
    if sum == 0:
        return 2   (pick the zero OR not pick it — both are valid)
    if arr[0] == sum:
        return 1   (only valid if we pick this element)
    return 0
```

This only matters when the problem explicitly states the array can contain zeros. For this problem with positive integers, the current solution is correct.

---

## What If the Array Contains Negative Numbers?

Striver raises this as a challenge in the lecture. The issue is with the dp array — its second dimension is indexed by `sum`, which assumes `sum >= 0`. If elements can be negative, `sum` can go negative and you cannot use it as an array index.

**The fix:** Replace the 2D array with a **HashMap** as the DP table.

```
Map<Integer, Integer> dp = new HashMap<>()
Key = sum (can be negative)
Value = count of subsets achieving that sum
```

```java
// With negative numbers — memoization using HashMap
private Map<String, Integer> memo = new HashMap<>();

private int solve(int index, int sum, int[] arr) {
    if (sum == 0) return 1;
    if (index == 0) return arr[0] == sum ? 1 : 0;

    String key = index + "," + sum;
    if (memo.containsKey(key)) return memo.get(key);

    int notPick = solve(index - 1, sum, arr);
    int pick = solve(index - 1, sum - arr[index], arr);
    // WHY no "arr[index] <= sum" guard here:
    // with negatives, arr[index] can be negative,
    // meaning picking it can INCREASE the remaining sum,
    // so the guard no longer applies

    int result = notPick + pick;
    memo.put(key, result);
    return result;
}
```

The key insight: with negative numbers, the `arr[i] <= sum` guard is no longer valid — picking a negative element can bring you closer to the target, not farther. You must explore both choices unconditionally.

---

## The Key Takeaway

```
┌──────────────────────────────────────────────────────────────────┐
│  Count Subsets with Sum K = Subset Sum Equal to Target           │
│  with exactly TWO substitutions:                                 │
│                                                                  │
│  1. Base case: return true/false  →  return 1/0                  │
│  2. Combine choices: notTake || take  →  notTake + take          │
│                                                                  │
│  Striver's universal rule for counting problems:                 │
│  "Whenever the problem says count the number of ways,            │
│   return 1 when condition is satisfied, 0 otherwise.             │
│   Then ADD all choices instead of OR-ing them."                  │
│                                                                  │
│  The general pattern across 0/1 Knapsack variations:             │
│                                                                  │
│  Subset Sum (existence) → return true/false, use OR              │
│  Count Subsets (count)  → return 1/0,        use ADD             │
│  Min/Max variant        → return value,       use MIN/MAX        │
│                                                                  │
│  What changes between problems is ONLY Step 3 of the             │
│  shortcut. The state, the recurrence structure, the              │
│  base case triggers — all stay the same.                         │
│                                                                  │
│  Important edge cases:                                           │
│  Zeros in array → modify base case to return 2 when              │
│      index==0, sum==0 (pick zero OR don't pick it)               │
│  Negative numbers → replace dp array with HashMap                │
│      and remove the arr[i] <= sum guard                          │
└──────────────────────────────────────────────────────────────────┘
```