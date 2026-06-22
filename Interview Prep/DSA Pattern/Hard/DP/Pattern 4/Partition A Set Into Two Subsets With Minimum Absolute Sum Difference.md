I've gone through the transcript carefully and all the existing solution files to match Striver's style precisely.

---

# Partition A Set Into Two Subsets With Minimum Absolute Sum Difference (GFG / Coding Ninjas)

---

# Stage 1: Identification

**Step 1 — Which topic?**

You are given an array of `n` non-negative integers. Partition all elements into two subsets S1 and S2. Find the partition that **minimizes |S1 - S2|** — the absolute difference between their sums.

The moment you see **"partition into two subsets with minimum difference"** — your brain should immediately connect this to the Subset Sum pattern. You are splitting elements between two groups and optimizing their sums. That is the 0/1 Knapsack Take/Not-Take structure.

**Step 2 — Which pattern?**

This is **Pattern 4: 0/1 Knapsack**.

The signal: every element goes into exactly one of two subsets (either taken into S1 or left for S2). Each element used exactly once. There is a target-related optimization. This is the 0/1 structure.

**Step 3 — Which key concept?**

**Tabulation reveals all reachable subset sums at once.**

This is the insight that makes this problem beautiful. Memoization computes only the states that are actually needed for a specific target. But **tabulation fills the entire dp table upfront** — and the last row of that table tells you, for every possible sum from 0 to totalSum, whether that sum is achievable as a subset sum.

Once you have that information, finding the minimum difference becomes a simple linear scan — no extra DP needed.

---

# Stage 2: Intuition Building

### The Core Mathematical Observation

If we split the array into two subsets with sums S1 and S2:

```
S1 + S2 = totalSum
S2 = totalSum - S1

|S1 - S2| = |S1 - (totalSum - S1)| = |2×S1 - totalSum|
```

To **minimize** this, we want S1 to be **as close to totalSum/2 as possible** — but S1 must be a sum actually achievable by some subset of the array.

### What Does the Subset Sum Tabulation Table Give Us?

Recall from the previous problem (Subset Sum Equal to Target): when we run tabulation with target = totalSum, the last row `dp[n-1][0..totalSum]` tells us:

```
dp[n-1][s] = true  →  some subset of the array sums to exactly s
dp[n-1][s] = false →  no subset sums to exactly s
```

This single row gives us **all achievable subset sums in one shot**. We don't need to run a separate DP for each candidate S1.

### The Algorithm

```
Step 1: Compute totalSum
Step 2: Run Subset Sum tabulation with target = totalSum
        (this fills the entire dp table)
Step 3: Read off dp[n-1][0..totalSum]
        For every s from 0 to totalSum/2:
            if dp[n-1][s] == true:
                S1 = s
                S2 = totalSum - s
                diff = |S2 - S1| = totalSum - 2*s
                update minimum
Step 4: Return minimum
```

**Why iterate only up to totalSum/2?**

Because |S1 - S2| = |S2 - S1|. If you check S1 = 3 and get difference 4, checking S1 = totalSum - 3 gives the same difference 4. The answers mirror each other. Iterating to totalSum/2 avoids duplicate work and is sufficient to find the minimum.

### Why This Problem Uniquely Shows the Power of Tabulation

In every previous problem, memoization and tabulation both produced the same answer. The choice between them was about recursion stack overhead — not about what they computed.

**This problem is different.**

Memoization only computes `f(index, target)` for the specific target you pass in. If you call it with target = totalSum, it fills states along the path to that target — but it does NOT fill states for target = 3, target = 7, etc. unless those happen to appear as subproblems.

Tabulation fills **every single cell** `dp[i][0..totalSum]` systematically. After it runs, you have a complete map of all achievable subset sums — which is exactly what you need to find the minimum difference.

This is the difference Striver is pointing at:

> "Memoization only computes necessary states. Tabulation computes all possible states upfront."

For this problem, you **need** all states. Tabulation is not just a style choice here — it is the right tool.

### Verifying with the Example

```
arr = [3, 2, 7],  totalSum = 12

Run Subset Sum tabulation with target = 12.
Last row dp[2][0..12]:

s=0:  true   (empty subset)
s=1:  false
s=2:  true   (just {2})
s=3:  true   (just {3})
s=4:  false
s=5:  true   ({3,2})
s=6:  false
s=7:  true   (just {7})
s=8:  false
s=9:  true   ({2,7})
s=10: true   ({3,7})
s=11: false
s=12: true   ({3,2,7})

Iterate s from 0 to 6 (totalSum/2):
s=0:  diff = 12 - 2*0 = 12
s=2:  diff = 12 - 2*2 = 8
s=3:  diff = 12 - 2*3 = 6
s=5:  diff = 12 - 2*5 = 2   ← minimum

Answer = 2 ✓
(S1 = {3,2} = 5, S2 = {7} = 7, |7-5| = 2)
```

---

# Stage 3: Coding

## Approach 1 — Memoization (Top-Down DP)

**Important note before writing:** Memoization here is less natural than tabulation. You would need to run it once for each candidate S1 (each possible subset sum), which defeats the purpose. The natural memoization approach runs the Subset Sum solution for a single target (totalSum), but then you don't automatically know which other sums are achievable.

The correct memoization approach runs the full subset sum tabulation anyway — so there is no meaningful memoization-only solution for this specific problem. We present it here as a starting point only, using the Subset Sum memoization as a subroutine called once per candidate S1:

```java
class Solution {
    int[][] dp;

    public int minimumDifference(int[] nums) {
        int n = nums.length;
        int totalSum = 0;
        for (int num : nums) totalSum += num;

        // For every candidate S1 from 0 to totalSum/2,
        // check if it's achievable using the Subset Sum memoization
        // WHY: memoization here checks one target at a time
        //      This is less efficient than tabulation for this problem
        //      because we need ALL achievable sums, not just one
        int mini = Integer.MAX_VALUE;
        for (int s1 = 0; s1 <= totalSum / 2; s1++) {
            dp = new int[n][totalSum + 1];
            for (int[] row : dp) Arrays.fill(row, -1);
            if (solve(n - 1, s1, nums)) {
                int s2 = totalSum - s1;
                mini = Math.min(mini, Math.abs(s2 - s1));
            }
        }
        return mini;
    }

    private boolean solve(int index, int target, int[] arr) {
        if (target == 0) return true;
        if (index == 0) return arr[0] == target;
        if (dp[index][target] != -1) return dp[index][target] == 1;

        boolean notTake = solve(index - 1, target, arr);
        boolean take = false;
        if (arr[index] <= target) {
            take = solve(index - 1, target - arr[index], arr);
        }

        dp[index][target] = (notTake || take) ? 1 : 0;
        return notTake || take;
    }
}
```

**Time Complexity — O(n × totalSum²):**
We call the memoization function once per candidate S1 — that is totalSum/2 calls. Each call takes O(n × totalSum) in the worst case. Total: O(n × totalSum²) — significantly worse than tabulation. This reinforces why tabulation is the right approach here.

**Space Complexity — O(n × totalSum) + O(n):**
dp array plus recursion stack. Reinitialised for each candidate S1.

---

## Approach 2 — Tabulation (The Natural and Correct Approach)

This is where the problem truly belongs. Run the Subset Sum tabulation **once** for target = totalSum. The last row immediately gives you everything you need.

```java
class Solution {
    public int minimumDifference(int[] nums) {
        int n = nums.length;

        // Step 1: Compute total sum
        int totalSum = 0;
        for (int num : nums) totalSum += num;

        // Step 2: Run Subset Sum tabulation with target = totalSum
        // This fills dp[i][s] = can nums[0..i] form a subset summing to s?
        // WHY target = totalSum: S1 can range from 0 to totalSum
        //     so we need to check ALL possible S1 values
        boolean[][] dp = new boolean[n][totalSum + 1];

        // Base case: target = 0 is always achievable (empty subset)
        for (int i = 0; i < n; i++) {
            dp[i][0] = true;
        }

        // Base case: index = 0, only nums[0] is available
        // WHY guard: nums[0] might exceed totalSum (impossible here since
        //     nums[0] <= totalSum always, but good practice)
        if (nums[0] <= totalSum) {
            dp[0][nums[0]] = true;
        }

        // Fill from index=1 to n-1
        for (int i = 1; i < n; i++) {
            for (int s = 1; s <= totalSum; s++) {

                // Not take nums[i]: look at previous row, same target
                boolean notTake = dp[i - 1][s];

                // Take nums[i]: only if it doesn't exceed s
                // WHY dp[i-1][s - nums[i]]: we consumed nums[i],
                //     check if previous prefix achieves remaining target
                boolean take = false;
                if (nums[i] <= s) {
                    take = dp[i - 1][s - nums[i]];
                }

                dp[i][s] = notTake || take;
            }
        }

        // Step 3: Scan last row to find minimum |S1 - S2|
        // dp[n-1][s] tells us if subset sum s is achievable
        // WHY only scan up to totalSum/2:
        //     |S1 - S2| = |S2 - S1| — the differences mirror each other
        //     so scanning half is sufficient to find the minimum
        int mini = Integer.MAX_VALUE;
        for (int s1 = 0; s1 <= totalSum / 2; s1++) {
            if (dp[n - 1][s1]) {
                int s2 = totalSum - s1;
                // |S1 - S2| = |(totalSum - s1) - s1| = totalSum - 2*s1
                mini = Math.min(mini, Math.abs(s2 - s1));
            }
        }

        return mini;
    }
}
```

**Dry Run:**
```
nums = [1, 2, 3, 4],  totalSum = 10,  target = 10

Step 1 — Fill column 0 (all true):
dp[i][0] = true for all i

Step 2 — Fill row 0 (nums[0]=1):
dp[0][1] = true
dp[0] = [T, T, F, F, F, F, F, F, F, F, F]
          0  1  2  3  4  5  6  7  8  9  10

Step 3 — Fill row 1 (nums[1]=2):
s=1: notTake=dp[0][1]=T → T
s=2: notTake=F, take=dp[0][0]=T → T
s=3: notTake=F, take=dp[0][1]=T → T
dp[1] = [T, T, T, T, F, F, F, F, F, F, F]

Step 4 — Fill row 2 (nums[2]=3):
s=3: notTake=dp[1][3]=T → T
s=4: notTake=F, take=dp[1][1]=T → T
s=5: notTake=F, take=dp[1][2]=T → T
s=6: notTake=F, take=dp[1][3]=T → T
dp[2] = [T, T, T, T, T, T, T, F, F, F, F]

Step 5 — Fill row 3 (nums[3]=4):
s=4: notTake=dp[2][4]=T → T
s=5: notTake=dp[2][5]=T → T
s=6: notTake=dp[2][6]=T → T
s=7: notTake=F, take=dp[2][3]=T → T
s=8: notTake=F, take=dp[2][4]=T → T
s=9: notTake=F, take=dp[2][5]=T → T
s=10:notTake=F, take=dp[2][6]=T → T
dp[3] = [T, T, T, T, T, T, T, T, T, T, T]

Step 6 — Scan dp[3][0..5] (totalSum/2 = 5):
s1=0: diff=10-0=10
s1=1: diff=10-2=8
s1=2: diff=10-4=6
s1=3: diff=10-6=4
s1=4: diff=10-8=2
s1=5: diff=10-10=0   ← minimum

Answer = 0 ✓
({1,4} = 5, {2,3} = 5, |5-5| = 0)
```

**Time Complexity — O(n × totalSum):**
Two nested loops — outer runs n-1 times, inner runs totalSum times. Each cell does O(1) work. One final linear scan of totalSum/2. Total: O(n × totalSum) + O(totalSum) = O(n × totalSum).

**Space Complexity — O(n × totalSum):**
The dp array of size n × (totalSum+1). No recursion stack.

---

## Approach 3 — Space Optimization (The Final Form)

```
dp[i][s] = dp[i-1][s] || dp[i-1][s - nums[i]]
```

Row `i` only depends on row `i-1`. Replace the full 2D table with two boolean arrays of size totalSum+1.

After the loop completes, `prev` holds the last row — identical to `dp[n-1]` in tabulation. The final scan over `prev` gives the answer.

```java
class Solution {
    public int minimumDifference(int[] nums) {
        int n = nums.length;

        int totalSum = 0;
        for (int num : nums) totalSum += num;

        // prev[s] = true means some subset of the "previous" elements
        //           sums to exactly s
        boolean[] prev = new boolean[totalSum + 1];

        // Base case: target=0 always achievable (empty subset)
        prev[0] = true;

        // Base case: index=0, nums[0] achieves its own value
        if (nums[0] <= totalSum) {
            prev[nums[0]] = true;
        }

        // Fill from index=1 to n-1
        for (int i = 1; i < n; i++) {
            boolean[] curr = new boolean[totalSum + 1];

            // target=0 always achievable
            curr[0] = true;

            for (int s = 1; s <= totalSum; s++) {
                // Not take nums[i]
                // WHY prev[s]: skip nums[i], look at previous row
                boolean notTake = prev[s];

                // Take nums[i] — only if it fits within s
                // WHY prev[s - nums[i]]: used nums[i], check previous row
                //     for whether remaining sum was achievable
                boolean take = false;
                if (nums[i] <= s) {
                    take = prev[s - nums[i]];
                }

                curr[s] = notTake || take;
            }

            // Slide forward: current row becomes new previous row
            // WHY: next index needs this row as its "previous"
            prev = curr;
        }

        // Scan prev[0..totalSum/2] for minimum difference
        // WHY prev: after the loop, prev holds the last row's answers
        //     equivalent to dp[n-1] in full tabulation
        int mini = Integer.MAX_VALUE;
        for (int s1 = 0; s1 <= totalSum / 2; s1++) {
            if (prev[s1]) {
                int s2 = totalSum - s1;
                mini = Math.min(mini, Math.abs(s2 - s1));
            }
        }

        return mini;
    }
}
```

**Time Complexity — O(n × totalSum):**
Same two nested loops as tabulation. Final scan of totalSum/2. Total: O(n × totalSum). No change.

**Space Complexity — O(totalSum):**
No n × (totalSum+1) array. No recursion stack. Just two boolean arrays of size totalSum+1 — `prev` and `curr`. Memory depends only on totalSum, not n.

---

## Final Comparison

| Approach | Time | Space | Notes |
|---|---|---|---|
| Memoization (per-target) | O(n × totalSum²) | O(n×totalSum) + O(n) | Wrong tool — runs DP once per candidate S1 |
| Tabulation | O(n × totalSum) | O(n × totalSum) | Correct tool — fills all states in one pass |
| Space Optimization | O(n × totalSum) | **O(totalSum)** | Best — submit this |

---

## The Key Takeaway

```
┌──────────────────────────────────────────────────────────────────┐
│  The insight that makes this problem click:                      │
│                                                                  │
│  Memoization computes only what you ask for.                     │
│  Tabulation computes everything upfront.                         │
│                                                                  │
│  When you need just one answer → memoization is efficient.       │
│  When you need ALL answers simultaneously → tabulation wins.     │
│                                                                  │
│  Here, you need to know which subset sums are achievable         │
│  for every value from 0 to totalSum/2.                           │
│  Tabulation gives you all of this in one pass.                   │
│  Memoization would require totalSum/2 separate calls.            │
│                                                                  │
│  The formula once you have the last row:                         │
│  For each achievable S1:                                         │
│      S2 = totalSum - S1                                          │
│      diff = totalSum - 2×S1                                      │
│  Scan only S1 from 0 to totalSum/2 — differences mirror          │
│  each other above and below the midpoint.                        │
│                                                                  │
│  This problem is the clearest example in the entire              │
│  0/1 Knapsack pattern of when the DP table itself                │
│  — not just the final cell — carries the answer.                 │
└──────────────────────────────────────────────────────────────────┘
```