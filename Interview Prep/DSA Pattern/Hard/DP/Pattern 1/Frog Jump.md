# Frog Jump (GFG / Coding Ninjas)

---

# Stage 1: Identification

**Step 1 — Which topic?**

A frog starts at stair 0 and wants to reach stair n-1. At each stair, it can jump either 1 step or 2 steps. Each jump has a cost — the absolute difference in heights of the two stairs. You need to find the **minimum total energy** to reach the last stair.

The moment you see **"find the minimum among all possible ways"** — that is the signal. To find the best among all ways, you must **try all possible ways**. And whenever you try all possible ways, **Recursion** is your first thought. And when that recursion has overlapping subproblems — it becomes **Dynamic Programming**.

**Why doesn't Greedy work here?**

You might think — at each stair, just pick the cheaper jump. But greedy fails. Consider:

```
heights = [10, 60, 30, 60, 30, 60]
```

Greedily from index 0:
- 0→1 costs |10-60| = 50, 0→2 costs |10-30| = 20 → pick 0→2 (cost 20)
- 2→3 costs |30-60| = 30, 2→4 costs |30-30| = 0 → pick 2→4 (cost 0)
- 4→5 costs |30-60| = 30 → forced → total = 50

But the optimal path 0→1→2→3→4→5 might give less. The point: **a greedy choice now can close off a cheaper path later**. You must try all ways and take the minimum.

**Step 2 — Which pattern?**

Input is a single array. You're computing a single value that depends on previous values in that array. No grid, no two sequences.

That is **Pattern 1: 1D DP (Linear)**.

**Step 3 — Which key concept?**

Apply Striver's **3-step shortcut**:

```
Step 1: Represent problem in terms of index
Step 2: Do all possible things on that index
Step 3: Question says minimum → take min of all results
```

This gives the recurrence. Then: Recursion → Memoization → Tabulation → Space Optimization.

---

# Stage 2: Intuition Building

### Step 1 — Represent the problem in terms of index

Treat each stair as an index: 0, 1, 2, ... n-1.

Define your recursion as:

```
f(index) = minimum energy to reach index from index 0
```

So the answer you want is `f(n-1)`.

### Step 2 — Do all possible things on that index

The frog can jump 1 step or 2 steps. So from `index`, it could have come from:
- `index - 1` (1-step jump), costing `|height[index] - height[index-1]|`
- `index - 2` (2-step jump), costing `|height[index] - height[index-2]|`

So the number of distinct choices at each index is exactly 2.

### Step 3 — Take minimum because question asks for minimum energy

```
f(index) = min(
    f(index-1) + |height[index] - height[index-1]|,
    f(index-2) + |height[index] - height[index-2]|   ← only if index >= 2
)
```

### Base case

```
f(0) = 0
```
You are already at index 0. No energy needed to "reach" it.

### Verify with example

```
heights = [10, 20, 30, 10]
indices =   0    1    2   3
```

- `f(0) = 0`
- `f(1) = f(0) + |20-10| = 0 + 10 = 10`
- `f(2) = min(f(1) + |30-20|, f(0) + |30-10|) = min(10+10, 0+20) = min(20, 20) = 20`
- `f(3) = min(f(2) + |10-30|, f(1) + |10-20|) = min(20+20, 10+10) = min(40, 20) = 20`

Answer = 20 ✓ (matches the example from the problem)

### Are there overlapping subproblems?

Draw the recursion tree for n=6:

```
                        f(5)
                      /      \
                  f(4)          f(3)
                 /    \        /    \
             f(3)     f(2)  f(2)   f(1)
            /    \
          f(2)  f(1)
```

`f(3)` is computed twice. `f(2)` is computed three times. **Overlapping subproblems confirmed** → DP is needed.

---

# Stage 3: Coding

## Approach 1 — Pure Recursion (Brute Force)

```java
class Solution {
    public int frogJump(int n, int heights[]) {
        return solve(n - 1, heights);
    }
    
    private int solve(int index, int[] heights) {
        // Base case: already at stair 0, zero energy needed
        if (index == 0) return 0;
        
        // Option 1: came from index-1 (1-step jump)
        // WHY: always valid since index >= 1 here
        int left = solve(index - 1, heights)
                   + Math.abs(heights[index] - heights[index - 1]);
        
        // Option 2: came from index-2 (2-step jump)
        // WHY: only valid if index >= 2, otherwise we'd go to index -1
        int right = Integer.MAX_VALUE;
        if (index >= 2) {
            right = solve(index - 2, heights)
                    + Math.abs(heights[index] - heights[index - 2]);
        }
        
        // Take minimum because question asks for minimum energy
        return Math.min(left, right);
    }
}
```

**Time Complexity — O(2^n):**
At every index, the function makes 2 recursive calls — one for `index-1` and one for `index-2`. The recursion tree is a binary tree of depth n. Total nodes in this tree grow as 2^n. For n=100, that's 2^100 operations — completely impractical.

**Space Complexity — O(n):**
No array is used, but the **recursion call stack** holds frames. The deepest chain of calls goes `f(n-1)` → `f(n-2)` → ... → `f(0)` — that is n frames on the stack at once. So the call stack uses O(n) space.

---

## Approach 2 — Memoization (Top-Down DP)

The same subproblems are solved repeatedly. Store each result the first time it's computed. Three steps:

```
Step 0: Declare dp array of size n, fill with -1
Step 1: If dp[index] != -1, return it immediately  (lookup)
Step 2: Before returning, store result in dp[index] (store)
```

```java
class Solution {
    public int frogJump(int n, int heights[]) {
        // Step 0: dp array, -1 means not yet computed
        int[] dp = new int[n];
        Arrays.fill(dp, -1);
        return solve(n - 1, heights, dp);
    }
    
    private int solve(int index, int[] heights, int[] dp) {
        // Base case
        if (index == 0) return 0;
        
        // Step 1: Already computed? Return stored value instantly
        // WHY: f(3) is the minimum energy to reach stair 3 — it never changes.
        //      No reason to recompute it if we've seen it before.
        if (dp[index] != -1) return dp[index];
        
        // Option 1: 1-step jump
        int left = solve(index - 1, heights, dp)
                   + Math.abs(heights[index] - heights[index - 1]);
        
        // Option 2: 2-step jump (only if index >= 2)
        int right = Integer.MAX_VALUE;
        if (index >= 2) {
            right = solve(index - 2, heights, dp)
                    + Math.abs(heights[index] - heights[index - 2]);
        }
        
        // Step 2: Store before returning
        // WHY: Next time solve(index) is called, the answer is ready in O(1)
        dp[index] = Math.min(left, right);
        
        return dp[index];
    }
}
```

**Time Complexity — O(n):**
Without memoization, `f(3)` might be recomputed many times. With memoization, each unique index from 0 to n-1 is computed **exactly once** — the first time it's encountered. Every subsequent call for the same index hits the `dp[index] != -1` check and returns instantly in O(1). There are n unique subproblems, each doing O(1) work. Total: O(n).

**Space Complexity — O(n) + O(n):**
Two sources of space here. First, the `dp` array of size n — that is O(n). Second, the **recursion call stack** — even with memoization, the first time you call `f(n-1)`, it calls `f(n-2)`, which calls `f(n-3)`, and so on until `f(0)`. That chain puts n frames on the stack simultaneously. So total space is O(n) array + O(n) stack = O(n) overall, but with a larger constant than tabulation.

---

## Approach 3 — Tabulation (Bottom-Up DP)

Flip the direction. Instead of starting at `f(n-1)` and going down, start from the base case `f(0)` and build up to `f(n-1)`. No recursion stack at all.

The recipe:
```
Step 1: Declare same dp array as memoization
Step 2: Fill in base case directly — dp[0] = 0
Step 3: Look at recurrence — figure out starting index
        (needs index-1 and index-2, so valid from index=1 onwards)
Step 4: Fill from index=1 to n-1 using the recurrence
```

```java
class Solution {
    public int frogJump(int n, int heights[]) {
        // Step 1: Declare dp array
        int[] dp = new int[n];
        
        // Step 2: Base case — energy to reach stair 0 is 0
        dp[0] = 0;
        
        // Step 3 & 4: Fill from index=1 to n-1
        for (int i = 1; i < n; i++) {
            
            // Option 1: came from i-1 (always valid since i >= 1)
            int left = dp[i - 1]
                       + Math.abs(heights[i] - heights[i - 1]);
            
            // Option 2: came from i-2 (only valid if i >= 2)
            // WHY: if i=1, i-2 = -1, which is out of bounds
            int right = Integer.MAX_VALUE;
            if (i >= 2) {
                right = dp[i - 2]
                        + Math.abs(heights[i] - heights[i - 2]);
            }
            
            // Store minimum of both choices
            dp[i] = Math.min(left, right);
        }
        
        // Answer to the original problem is at the last index
        return dp[n - 1];
    }
}
```

**Time Complexity — O(n):**
A single `for` loop from `i = 1` to `i = n-1`. That is n-1 iterations, each doing O(1) work — two option computations and a min. Total: O(n). Same as memoization but with no function call overhead — cleaner in practice.

**Space Complexity — O(n):**
Only the `dp` array of size n. No recursion stack at all — everything is iterative. Already an improvement over memoization which needed O(n) array + O(n) stack.

---

## Approach 4 — Space Optimization (The Final Form)

Look at the recurrence at any index `i`:

```
dp[i] = min(dp[i-1] + cost1,  dp[i-2] + cost2)
```

To compute `dp[i]`, you only need **dp[i-1]** and **dp[i-2]**. You do not need anything before that. So why keep the entire array?

Watch the two variables slide forward:

```
Before loop:
    prev2 = dp[0] = 0      (represents dp[i-2])
    prev1 = dp[1] = |h[1]-h[0]|   (represents dp[i-1])

i = 2:
    left  = prev1 + |h[2]-h[1]|
    right = prev2 + |h[2]-h[0]|
    curr  = min(left, right)
    
    → slide window:
    prev2 = prev1
    prev1 = curr

i = 3:
    left  = prev1 + |h[3]-h[2]|
    right = prev2 + |h[3]-h[1]|
    curr  = min(left, right)
    
    → slide window:
    prev2 = prev1
    prev1 = curr

... and so on
```

At the end of the loop, `prev1` holds the answer for index `n-1`.

```java
class Solution {
    public int frogJump(int n, int heights[]) {
        // Handle edge case
        if (n == 1) return 0;
        
        // prev2 = dp[i-2], prev1 = dp[i-1]
        // Initialize for i=1: prev2 = dp[0] = 0, prev1 = dp[1]
        int prev2 = 0;  // dp[0]
        int prev1 = Math.abs(heights[1] - heights[0]);  // dp[1]
        
        for (int i = 2; i < n; i++) {
            // Option 1: 1-step jump from i-1
            int left = prev1 + Math.abs(heights[i] - heights[i - 1]);
            
            // Option 2: 2-step jump from i-2
            // WHY: i >= 2 is always true here since loop starts at i=2
            int right = prev2 + Math.abs(heights[i] - heights[i - 2]);
            
            int curr = Math.min(left, right);
            
            // Slide the window forward
            prev2 = prev1;
            // WHY: what was dp[i-1] becomes dp[i-2] in the next iteration
            
            prev1 = curr;
            // WHY: what we just computed becomes dp[i-1] in the next iteration
        }
        
        // prev1 now holds dp[n-1] — the answer
        return prev1;
    }
}
```

**Time Complexity — O(n):**
Still a single loop from `i = 2` to `i = n-1`. Same O(n) as tabulation. The sliding of two variables per iteration is O(1). No change in time complexity — but we already can't do better than O(n) because we must at least look at every stair once.

**Space Complexity — O(1):**
This is the key win. No array of any kind is allocated. Just three integer variables — `prev2`, `prev1`, and `curr` — regardless of how large n is. Whether n is 10 or 10 million, the memory used is the same constant amount. That is what O(1) space means.

---

## Dry Run — Space Optimized on Example

```
heights = [10, 20, 30, 10]
n = 4
```

```
prev2 = 0                          (dp[0] = 0)
prev1 = |20 - 10| = 10             (dp[1] = 10)

i = 2:
    left  = prev1 + |30-20| = 10 + 10 = 20
    right = prev2 + |30-10| = 0  + 20 = 20
    curr  = min(20, 20) = 20
    prev2 = 10, prev1 = 20

i = 3:
    left  = prev1 + |10-30| = 20 + 20 = 40
    right = prev2 + |10-20| = 10 + 10 = 20
    curr  = min(40, 20) = 20
    prev2 = 20, prev1 = 20

Answer = prev1 = 20 ✓
```

---

## Final Comparison

| Approach | Time | Space | Notes |
|---|---|---|---|
| Pure Recursion | O(2^n) | O(n) stack | Exponential — never use |
| Memoization | O(n) | O(n) array + O(n) stack | Good interview starting point |
| Tabulation | O(n) | O(n) array | Better — eliminates stack |
| Space Optimization | O(n) | **O(1)** | Best — submit this |

---

## The Key Takeaway

```
┌──────────────────────────────────────────────────────────────────┐
│  This problem introduces the most important DP lesson:           │
│                                                                  │
│  "Minimum/Maximum" problems → Try all ways, take best            │
│  This is NOT greedy — greedy fails when a cheap choice now       │
│  blocks a cheaper path later.                                    │
│                                                                  │
│  The 3-step shortcut applied here:                               │
│  Index  = stair number                                           │
│  Choices = jump 1 step OR jump 2 steps                           │
│  Goal   = minimum → take min of both choices                     │
│                                                                  │
│  Space optimization rule of thumb:                               │
│  Whenever recurrence uses only (i-1) and (i-2),                  │
│  you can ALWAYS replace the dp array with two variables.         │
│  This pattern will repeat in every 1D DP problem.                │
└──────────────────────────────────────────────────────────────────┘
```