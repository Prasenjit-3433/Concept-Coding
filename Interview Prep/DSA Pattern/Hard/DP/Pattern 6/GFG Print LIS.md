# GFG: Print LIS

Key Concept: Reconstruct the actual subsequence via backtracking
Solution: https://www.youtube.com/watch?v=IFfYfonAFGc&ab_channel=takeUforward
Status: Done

## Fresh Approach — The Algorithmic Tabulation (dp[i] = LIS ending at index i)

---

### A Fresh Way to Think About It

The memoization and tabulation we did in the last lecture worked — but they came with a heavy cost: an `n×n` dp table. The constraints on LeetCode say n can be up to 10⁵. An n×n array at that scale is 10¹⁰ cells — that's a memory limit exceeded before you even start.

So we need a completely different angle.

Striver introduces a new definition of the dp array that makes everything cleaner:

```
dp[i] = length of the longest increasing subsequence
         that ENDS at index i
```

This single change in perspective unlocks a much simpler algorithm — and also makes it possible to print the actual subsequence later.

---

### Why "Ends At" Is the Right Framing

Think about it this way. If you're standing at index `i` and you want to know the longest increasing subsequence that ends here, you only need to ask one question:

> **Among all the indices `j` that come before `i` — if `nums[j] < nums[i]`, can I extend the LIS that ended at `j` by appending `nums[i]`?**
> 

If yes, then the LIS ending at `i` could be `dp[j] + 1`.

You try this for every valid `j` before `i`, and you take the maximum. That's it. That's the entire algorithm.

---

### Building the Intuition with an Example

```
nums = [5, 4, 11, 1, 16, 8]
index:  0  1   2  3   4  5
```

**One rule that's always true before we start:**

No matter what, every element can form a subsequence of length 1 — just itself. So we initialize every `dp[i] = 1`.

```
dp = [1, 1, 1, 1, 1, 1]
```

Now fill left to right. For each index `i`, look at every `j < i`:

**i = 0 (value 5):** No previous elements. `dp[0] = 1`.

**i = 1 (value 4):** Previous: `j=0`, value 5. Is 5 < 4? No. Nothing updates. `dp[1] = 1`.

**i = 2 (value 11):**

- `j=0`, value 5 < 11 ✓ → `dp[0] + 1 = 2`
- `j=1`, value 4 < 11 ✓ → `dp[1] + 1 = 2`

Best is 2. `dp[2] = 2`.

**i = 3 (value 1):**

- `j=0`, value 5 < 1? No
- `j=1`, value 4 < 1? No
- `j=2`, value 11 < 1? No

Nothing updates. `dp[3] = 1`.

**i = 4 (value 16):**

- `j=0`, value 5 < 16 ✓ → `dp[0] + 1 = 2`
- `j=1`, value 4 < 16 ✓ → `dp[1] + 1 = 2`
- `j=2`, value 11 < 16 ✓ → `dp[2] + 1 = 3` ← best so far
- `j=3`, value 1 < 16 ✓ → `dp[3] + 1 = 2`

Best is 3. `dp[4] = 3`. This means the LIS ending at 16 is `[5, 11, 16]` or `[4, 11, 16]` — length 3.

**i = 5 (value 8):**

- `j=0`, value 5 < 8 ✓ → `dp[0] + 1 = 2`
- `j=1`, value 4 < 8 ✓ → `dp[1] + 1 = 2`
- `j=2`, value 11 < 8? No
- `j=3`, value 1 < 8 ✓ → `dp[3] + 1 = 2`
- `j=4`, value 16 < 8? No

Best is 2. `dp[5] = 2`.

```
Final dp = [1, 1, 2, 1, 3, 2]
```

The answer is the **maximum value in the entire dp array** — which is `3`.

---

### Why the Answer Is Max of All dp[i]

Unlike the fixed-endpoint problems we've seen before, the LIS can **end anywhere** in the array. It doesn't have to end at the last index. So we can't just return `dp[n-1]`. We scan all `dp[i]` and return the maximum.

---

### The Code

```java
class Solution {
    public int lengthOfLIS(int[] nums) {
        int n = nums.length;

        // dp[i] = length of LIS ending at index i
        // Every element alone is a valid subsequence of length 1
        int[] dp = new int[n];
        Arrays.fill(dp, 1);

        int maxLen = 1;

        for (int i = 0; i < n; i++) {
            // Check every previous index j
            for (int j = 0; j < i; j++) {

                // Can nums[j] come before nums[i] in an increasing subsequence?
                if (nums[j] < nums[i]) {
                    // WHY dp[j] + 1: extend the LIS that ended at j by one more element
                    // WHY Math.max: multiple valid j's may exist, take the best
                    dp[i] = Math.max(dp[i], dp[j] + 1);
                }
            }

            // Track the overall maximum across all ending positions
            maxLen = Math.max(maxLen, dp[i]);
        }

        return maxLen;
    }
}
```

---

### Complexity

**Time — O(n²):**
Two nested loops. The outer runs `n` times. The inner runs up to `i` times for each `i`. In the worst case this is `0 + 1 + 2 + ... + (n-1)` = n(n-1)/2 iterations — which is O(n²). Each iteration does O(1) work.

**Space — O(n):**
Just the single `dp` array of size `n`. No recursion stack, no 2D table. A dramatic improvement over the memoization approach.

---

### Why This Approach Matters Beyond Just Length

This `dp[i]` definition does something the previous approach couldn't — it stores **which position** the longest subsequence ends at. And if you also remember **which index before `i` gave you the best extension**, you can walk backwards through those links and reconstruct the actual subsequence.

That backtracking idea is exactly what Part 5 is about.

---

### Key Takeaway

```
┌──────────────────────────────────────────────────────────────────┐
│  The key insight:                                                      │
│                                                                        │
│  dp[i] = LIS length ending at index i                                  │
│                                                                        │
│  For each i, look at all j < i:                                        │
│      if nums[j] < nums[i]:                                             │
│          dp[i] = max(dp[i], dp[j] + 1)                                 │
│                                                                        │
│  Initialize all dp[i] = 1                                              │
│  (every element is a valid subsequence of length 1)                    │
│                                                                        │
│  Final answer = max(dp[0], dp[1], ..., dp[n-1])                        │
│  WHY not just dp[n-1]: the LIS can end at any position                 │
│                                                                        │
│  This approach opens the door to PRINTING the LIS                      │
│  because it naturally tracks where the best extension came from        │
└──────────────────────────────────────────────────────────────────┘
```

---

# Printing the Actual LIS

---

### The Core Question

We now know **how long** the LIS is. But what if the interviewer says — great, now print the actual subsequence?

The answer lies in something Striver always says about backtracking through dp:

> **If you know how each dp[i] was built, you can walk backwards through those decisions and reconstruct the path.**
> 

In our `dp[i]` table, every time we updated `dp[i] = dp[j] + 1`, we made a decision — *"the element before `nums[i]` in the LIS is `nums[j]`"*. If we remember that `j` for every `i`, we can trace the entire subsequence backwards.

That's the only new thing we need — a **parent tracking array**.

---

### Introducing the Hash Array

Alongside the `dp` array, we maintain a second array called `hash`:

```
hash[i] = the index j that gave dp[i] its best value
```

In other words, `hash[i]` tells you — *"the element that came just before `nums[i]` in the LIS ending at `i` was at index `hash[i]`"*.

**One special rule for initialization:**

Every element starts as its own predecessor — `hash[i] = i`. This means *"I am the start of my own subsequence, nobody came before me"*. This is how we know when to stop while backtracking.

---

### How the Backtracking Works

Once the dp table and hash array are fully filled:

**Step 1:** Find the index where `dp[i]` is maximum. Call it `lastIndex`. This is where the LIS ends.

**Step 2:** Start at `lastIndex`. Read `nums[lastIndex]` — that's the last element of the LIS.

**Step 3:** Move to `hash[lastIndex]`. That's the index of the element that came just before in the LIS.

**Step 4:** Keep moving — `lastIndex = hash[lastIndex]` — until you reach a point where `hash[lastIndex] == lastIndex`. That means you've hit the starting element of the LIS — no one came before it.

**Step 5:** Reverse the collected elements — because you walked backwards from the end.

---

### Tracing Through the Example

```
nums  = [5,  4,  11,  1,  16,  8]
index:   0   1    2   3    4   5
```

**Fill dp and hash together:**

Initialize:

```
dp   = [1, 1, 1, 1, 1, 1]
hash = [0, 1, 2, 3, 4, 5]   ← every element points to itself
```

**i = 0:** No previous elements. `dp[0] = 1`, `hash[0] = 0`.

**i = 1 (value 4):**

- j=0, value 5 < 4? No.

`dp[1] = 1`, `hash[1] = 1`.

**i = 2 (value 11):**

- j=0, value 5 < 11 ✓ → `dp[0] + 1 = 2` > current dp[2]=1 → update. `dp[2] = 2`, `hash[2] = 0`.
- j=1, value 4 < 11 ✓ → `dp[1] + 1 = 2` = current dp[2]=2 → not strictly better, no update.

`dp[2] = 2`, `hash[2] = 0`. *(11 was preceded by index 0, which is 5)*

**i = 3 (value 1):** No valid j. `dp[3] = 1`, `hash[3] = 3`.

**i = 4 (value 16):**

- j=0, value 5 < 16 ✓ → `dp[0] + 1 = 2` > 1 → update. `dp[4] = 2`, `hash[4] = 0`.
- j=1, value 4 < 16 ✓ → `dp[1] + 1 = 2` = 2 → no update.
- j=2, value 11 < 16 ✓ → `dp[2] + 1 = 3` > 2 → update. `dp[4] = 3`, `hash[4] = 2`. *(16 was preceded by index 2, which is 11)*
- j=3, value 1 < 16 ✓ → `dp[3] + 1 = 2` < 3 → no update.

`dp[4] = 3`, `hash[4] = 2`.

**i = 5 (value 8):**

- j=0, value 5 < 8 ✓ → `dp[0] + 1 = 2` > 1 → update. `dp[5] = 2`, `hash[5] = 0`.
- j=1, value 4 < 8 ✓ → `dp[1] + 1 = 2` = 2 → no update.
- j=2, value 11 < 8? No.
- j=3, value 1 < 8 ✓ → `dp[3] + 1 = 2` = 2 → no update.
- j=4, value 16 < 8? No.

`dp[5] = 2`, `hash[5] = 0`.

**Final state:**

```
dp   = [1, 1, 2, 1, 3, 2]
hash = [0, 1, 0, 3, 2, 0]
```

**Backtrack:**

Maximum dp value is 3, found at `lastIndex = 4`.

```
Step 1: lastIndex = 4  → nums[4] = 16.  hash[4] = 2.  (2 ≠ 4, keep going)
Step 2: lastIndex = 2  → nums[2] = 11.  hash[2] = 0.  (0 ≠ 2, keep going)
Step 3: lastIndex = 0  → nums[0] = 5.   hash[0] = 0.  (0 == 0, STOP)
```

Collected in reverse order: `[16, 11, 5]`

After reversing: `[5, 11, 16]` ✓

---

### The Code

```java
class Solution {
    public int lengthOfLIS(int[] nums) {
        int n = nums.length;

        // dp[i] = length of LIS ending at index i
        int[] dp = new int[n];
        Arrays.fill(dp, 1);

        // hash[i] = index of the element just before nums[i] in the LIS
        // Initialize to self — meaning "I am my own starting point"
        int[] hash = new int[n];
        for (int i = 0; i < n; i++) hash[i] = i;

        int maxLen = 1;
        int lastIndex = 0;  // tracks where the best LIS ends

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < i; j++) {

                // Can nums[j] extend into nums[i]?
                if (nums[j] < nums[i] && dp[j] + 1 > dp[i]) {
                    dp[i] = dp[j] + 1;

                    // Remember: the element before nums[i] in this LIS
                    // is nums[j], at index j
                    hash[i] = j;
                }
            }

            // Track the overall best ending position
            if (dp[i] > maxLen) {
                maxLen = dp[i];
                lastIndex = i;
            }
        }

        // Backtrack using hash to reconstruct the LIS
        List<Integer> lis = new ArrayList<>();

        // Walk backwards from lastIndex until we hit a self-pointing element
        // WHY hash[lastIndex] != lastIndex: self-pointer means this is the start
        while (hash[lastIndex] != lastIndex) {
            lis.add(nums[lastIndex]);
            lastIndex = hash[lastIndex];
        }

        // Add the starting element (the one that points to itself)
        lis.add(nums[lastIndex]);

        // Reverse because we collected from end to start
        Collections.reverse(lis);

        // Print the LIS
        System.out.println("LIS: " + lis);

        return maxLen;
    }
}
```

---

### Complexity

**Time — O(n²):**
The two nested loops for filling `dp` and `hash` are the dominant cost — O(n²). The backtracking walk is at most O(n) — it visits each element at most once. So total time is O(n²).

**Space — O(n):**
Two arrays of size `n` — `dp` and `hash`. The result list is at most size `n`. No recursion stack. Total: O(n).

---

### Key Takeaway

```
┌──────────────────────────────────────────────────────────────────┐
│  Printing LIS = dp[i] table + one extra "parent" array                 │
│                                                                        │
│  hash[i] = index j that gave dp[i] its best value                      │
│  Initialize: hash[i] = i  (every element starts as its own root)       │
│                                                                        │
│  Update rule — only update hash when dp actually improves:             │
│      if nums[j] < nums[i] AND dp[j] + 1 > dp[i]:                       │
│          dp[i] = dp[j] + 1                                             │
│          hash[i] = j                                                   │
│                                                                        │
│  Backtracking:                                                         │
│      Start at lastIndex (where max dp was found)                       │
│      Walk: lastIndex = hash[lastIndex]                                 │
│      Stop when: hash[lastIndex] == lastIndex (self-pointer)            │
│      Reverse the collected elements                                    │
│                                                                        │
│  WARNING: This O(n²) approach is needed if you want to PRINT           │
│  the LIS. The O(n log n) approach we'll see next can only give         │
│  you the LENGTH — it cannot reconstruct the subsequence.               │
└──────────────────────────────────────────────────────────────────┘
```

---

This completes the full O(n²) treatment of LIS — recursion, memoization, tabulation, and printing.