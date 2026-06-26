# LC 300. Longest Increasing Subsequence

Key Concept: Core template — DP gives O(n²) 
Solution: https://www.youtube.com/watch?v=ekcwMsSIzVc&ab_channel=takeUforward
Status: Done

# Stage 1: Identification

## **Step 1 — Which topic?**

You are given an array of integers. Find the **length** of the longest strictly increasing subsequence.

The moment you see **"find the *longest subsequence* satisfying some *ordering condition*"** — that is the signal. To find the longest, you must **try all possible subsequences**. And whenever you try all possible subsequences, **Recursion** is your first thought. And when that recursion has overlapping subproblems — it becomes **Dynamic Programming**.

### 💡**Step 1b — Subsequence vs Subarray — the critical distinction:**

Before anything else, burn this into memory:

```
Subarray  → elements must be **CONTIGUOUS**
            [10, 9, 2, 5, 3] contains subarray [9, 2, 5]

Subsequence → elements can be **NON-CONTIGUOUS**
              but they must follow the **ORIGINAL ORDER**
              [10, 9, 2, 5, 3, 7, 101, 18]
              {2, 5, 7, 101} is a valid subsequence (follows order)
              {5, 2, 7}     is NOT (5 comes before 2 in original)
```

#### **Why does this matter for pattern recognition?**

Whenever a problem says `*"subarray"*` and involves **maximum/minimum** over a contiguous chunk — that points to **`Kadane Algo`**. Whenever a problem says `*"subsequence"*` with an ordering or constraint — that points to **`LIS`**. Getting this wrong in an interview means applying the wrong pattern entirely.

## **Step 2 — Which pattern?**

Input is a *single array*. You are looking for the longest subsequence where each element is strictly greater than the previous. There is an *ordering constraint* on which elements can be picked together.

That is **`Pattern 6: LIS (Longest Increasing Subsequence)`**.

## **Step 3 — Which key concept?**

Apply Striver's **3-step shortcut** — but with a twist compared to earlier patterns:

```
**Step 1**: Express in terms of index AND previous index
        (not just index alone — this is the new element)
**Step 2**: Explore all possibilities — Take or Not Take
**Step 3**: Question says longest → take max of both choices
```

**Why do you need TWO parameters here?**

In 1D DP problems like House Robber or Climbing Stairs, you only needed `index` because the constraint ("no adjacent houses", "jump 1 or 2") could be enforced purely from position. Here, the constraint is ***"current element must be strictly greater than the previous chosen element"***. To check this, you must know which element was previously chosen — which means tracking the `previous index` as a second parameter.

This is the key concept:

> **Whenever a subsequence problem has a constraint that depends on what you previously picked, you need a `previous index` as a second parameter alongside `index`.**
> 

---

# Stage 2: Intuition Building

### The Problem Setup

```
nums = [10, 9, 2, 5, 3, 7, 101, 18]
```

Some valid increasing subsequences:

```
{10, 101}       → length 2
{2, 3, 7, 101}  → length 4
{2, 5, 7, 101}  → length 4
{2, 5, 7, 18}   → length 4
{2, 3, 7, 18}   → length 4
```

All give length 4. That is the answer.

## Step 1 — Represent in terms of (`index`, `prevIndex`)

Define:

```
f(index, prevIndex) = length of the longest increasing subsequence
                      starting from **'index'**, given that the last
                      element included in the subsequence so far
                      was at **'prevIndex'**
```

Initially call `f(0, -1)` — start from index 0, and since no element has been picked yet,     `prevIndex = -1` (meaning "no previous element").

**Why start from the front, not the back?**

In Grid DP problems, we often went backward (start from destination). Here, Striver goes **forward** from index 0. The reason: at index 0, there's no previous element and the state is clean — `prevIndex = -1`. Starting from the back would require knowing all the end states first, which is less natural for this problem.

## Step 2 — Do all possible things at (index, prevIndex)

At each index, exactly two choices:

**Choice 1 — Not Take:**

Skip the current element. Move to the next index. The `prevIndex` doesn't change — whoever was the previous stays as the previous.

```
notTake = 0 + f(index + 1, prevIndex)
```

Adding 0 because we did not include this element — the length doesn't grow.

**Choice 2 — Take:**

Include the current element in our subsequence. But this is only valid if:

- `prevIndex == -1` (no previous element, so any element can be the first), OR
- `nums[index] > nums[prevIndex]` (current element strictly exceeds the previous)

If valid:

- Length grows by **+1**
- The current index becomes the new prevIndex
- Move to the next index

```
take = Integer.MIN_VALUE   (initially — if not valid)
if prevIndex == -1 OR nums[index] > nums[prevIndex]:
    take = 1 + f(index + 1, index)
                              ↑
              current index becomes the new prevIndex
```

## Step 3 — Take maximum

```
f(index, prevIndex) = max(notTake, take)
```

## Base Case

**When `index == n`:**

We have *run out of elements*. No more choices. The length of any subsequence starting from here is 0.

```
if index == n → return 0
```

## Visualizing the Recursion Tree (small example)

```
nums = [3, 1, 2]

f(0, -1):   prevIndex=-1, so can take 3 OR not take 3
├── NOT TAKE → f(1, -1)
│   ├── NOT TAKE → f(2, -1)
│   │   ├── NOT TAKE → f(3, -1) → return 0
│   │   └── TAKE 2  → 1 + f(3, 2) → 1 + 0 = 1
│   │   → max(0, 1) = 1
│   └── TAKE 1 → 1 + f(2, 1)
│       ├── NOT TAKE → f(3, 1) → return 0
│       └── TAKE 2: nums[2]=2 > nums[1]=1 ✓
│           → 1 + f(3, 2) → 1 + 0 = 1
│       → max(0, 1) = 1
│       = 1 + 1 = 2
│   → max(1, 2) = 2
│
└── TAKE 3 → 1 + f(1, 0)
    ├── NOT TAKE → f(2, 0)
    │   ├── NOT TAKE → f(3, 0) → 0
    │   └── TAKE 2: nums[2]=2 < nums[0]=3, CANNOT TAKE
    │   → max(0, 0) = 0
    └── TAKE 1: nums[1]=1 < nums[0]=3, CANNOT TAKE
    → max(0, 0) = 0
    = 1 + 0 = 1

f(0, -1) = max(2, 1) = 2  → LIS is {1, 2}
```

## Are there overlapping subproblems?

For larger arrays, the same `f(index, prevIndex)` state is reached from multiple parent states. The state space is n × (n+1) unique (index, prevIndex) pairs. Without memoization, the same state gets recomputed repeatedly.

**Overlapping subproblems confirmed** → DP is needed.

## 💡`Coordinate Shift` — A Critical Implementation Detail

In `f(index, prevIndex)` , 

- `index` has ranges 0, 1, 2,…$n-1$ & base case index == n
- `prevIndex` has ranges -1, 0, …..$n-1$

But `prevIndex` starts at **-1**, and you cannot use **-1** as an array index in Java.

**The fix:** Shift `prevIndex` by +1 everywhere.

```
prevIndex = -1  →  store at index 0
prevIndex =  0  →  store at index 1
prevIndex =  1  →  store at index 2
...
prevIndex = n-1 →  store at index n
```

Therefore, the value of `f(index, prevIndex)` will be stored/fetched from `dp[index][prevIndex+1]`

## DP Table Size

- `index`: 0 to n-1 & idx = n → 0, 1, 2, ….n → we need an array of size $n+1$
- `prevIndex`: -1 to n-1 → after shift: 0 to n → we need an array of size $n+1$

Hence, the dp table: (**n+1) × (n+1)**

---

# Stage 3: Coding

## Approach 1 — Pure Recursion (Brute Force)

```java
class Solution {
    public int lengthOfLIS(int[] nums) {
        int n = nums.length;
        // Start from index 0 with no previous element (-1)
        return solve(0, -1, nums, n);
    }

    private int solve(int index, int prevIndex, int[] nums, int n) {
        // Base case: ran out of elements — no more can be added
        // WHY return 0: length of LIS starting from here is 0
        if (index == n) return 0;

        // Choice 1: Not Take — skip nums[index]
        // prevIndex unchanged — whoever was previous stays previous
        // Length doesn't increase since we didn't include this element
        int notTake = solve(index + 1, prevIndex, nums, n);

        // Choice 2: Take — include nums[index]
        // Only valid if: no previous element (prevIndex == -1)
        //             OR current element strictly exceeds previous
        // WHY nums[index] > nums[prevIndex]: strictly increasing required
        int take = Integer.MIN_VALUE;
        if (prevIndex == -1 || nums[index] > nums[prevIndex]) {
            // Length grows by 1, current index becomes the new previous
            take = 1 + solve(index + 1, index, nums, n);
        }

        // Take the maximum — we want the longest
        return Math.max(notTake, take);
    }
}
```

**Time Complexity — O(2^n):**
At every index, the function makes 2 recursive calls — one for notTake and one for take (when valid). The recursion tree is a binary tree of depth n. The total number of nodes grows as 2^n. For n = 10^5 (the actual constraint on this problem), that's impossible.

**Space Complexity — O(n):**
No dp array is allocated. The **recursion call stack** holds one frame per level — the deepest chain goes from index 0 all the way to index n. That is n frames simultaneously on the stack. Stack uses O(n) space.

---

## Approach 2 — Memoization (Top-Down DP)

The same state `f(index, prevIndex)` is reached from multiple branches of the recursion tree. Store each result the first time it is computed.

**The coordinate shift in practice:**

When we store: `dp[index][prevIndex + 1] = result`

When we read: `if dp[index][prevIndex + 1] != -1, return it`

This shift of +1 maps the range `[-1, n-1]` to `[0, n]`, which fits cleanly in an array.

```java
class Solution {
    public int lengthOfLIS(int[] nums) {
        int n = nums.length;
        // dp[index][prevIndex + 1] = length of LIS from this state
        // Size: n rows (index 0..n-1), n+1 columns (prevIndex -1..n-1, shifted)
        int[][] dp = new int[n][n + 1];
        for (int[] row : dp) {
            Arrays.fill(row, -1);
        }
        return solve(0, -1, nums, n, dp);
    }

    private int solve(int index, int prevIndex, int[] nums, int n, int[][] dp) {
        // Base case: ran out of elements
        if (index == n) return 0;

        // Step 1: Already computed? Return stored value instantly
        // WHY prevIndex + 1: coordinate shift to avoid negative index
        // f(2, 1) means "LIS length starting from index 2, prev was index 1"
        // This never changes regardless of which path reached this state
        if (dp[index][prevIndex + 1] != -1) return dp[index][prevIndex + 1];

        // Choice 1: Not Take
        int notTake = solve(index + 1, prevIndex, nums, n, dp);

        // Choice 2: Take (only if valid)
        int take = 0;
        if (prevIndex == -1 || nums[index] > nums[prevIndex]) {
            take = 1 + solve(index + 1, index, nums, n, dp);
        }

        // Step 2: Store before returning — apply coordinate shift when writing
        dp[index][prevIndex + 1] = Math.max(notTake, take);
        return dp[index][prevIndex + 1];
    }
}
```

**Why does this get a Runtime Error on LeetCode?**

The constraint says n can be up to **2500** (the actual constraint). But if you misread it as 10^5 like Striver mentions in the lecture, the dp table would be 10^5 × 10^5 = **10^10 cells** — a table that large cannot even be allocated. Even at n = 2500, the dp table is 2500 × 2501 = ~6.25 million cells, which is fine. The memoization approach works correctly within the actual constraints.

**Time Complexity — O(n²):**
Each unique (index, prevIndex) pair is computed exactly once. The state space is n × (n+1). For each state, O(1) work is done — compute notTake and take, then max them. Total: **O(n²)**.

**Space Complexity — O(n²) + O(n):**
Two sources of space. First, the dp array of size n × (n+1) — that is O(n²). Second, the recursion call stack which goes n levels deep. Total: **O(n²) + O(n)**.

---

## Approach 3 — Tabulation (Bottom-Up DP)

---

### **From Memoization to Tabulation — The Conversion Rules**

If you've been following this series, you already know the drill. Every time we convert memoization to tabulation, the same three rules apply:

```
Rule 1: Declare the dp array of the same size as memoization
Rule 2: Fill the base cases directly into the array
Rule 3: Write the changing parameters as nested loops
        — but in the OPPOSITE direction from recursion
        Then copy the recurrence as-is
```

Let's apply all three here.

---

### **Rule 1 — Declare the DP Array**

In memoization, we used a `dp[n][n+1]` array because of the coordinate shift — `prev` ranged from `-1` to `n-1`, so we shifted it by `+1` to make it `0` to `n`.

Same size here:

```
int[][] dp = new int[n+1][n+1];
```

---

### Rule 2 — Copy the Base Case

In recursion, the base case was:

```
if (index == n) return 0;
```

This means: when we've run out of elements, the LIS length contribution is 0.

In the `dp` table, `dp[n][anything]` should be `0`. Since Java initializes int arrays to `0` by default, this base case is already handled — we don't need to write anything extra.

---

### Rule 3 — Loops in the Opposite Direction

In recursion, `index` went from `0` down to `n` (hitting the base case at `n`).

So in tabulation, `index` goes from `n-1` down to `0`.

In recursion, `prev` went from `-1` (coordinate-shifted to `0`) up toward `n-1` (coordinate-shifted to `n`).

So in tabulation, `prev` goes from `n-1` (coordinate-shifted: `n`) down to `-1` (coordinate-shifted: `0`).

```java
for (int index = n - 1; index >= 0; index--) {
    for (int prev = index - 1; prev >= -1; prev--) {
        // recurrence goes here
    }
}
```

---

### Copying the Recurrence

From memoization, the recurrence was:

```
// Not Take
int notTake = dp[index + 1][prev + 1];

// Take (only if current element is greater than previous)
int take = 0;
if (prev == -1 || nums[index] > nums[prev]) {
    take = 1 + dp[index + 1][index + 1];
}

dp[index][prev + 1] = Math.max(notTake, take);
```

The coordinate shift `(prev + 1)` is applied everywhere `prev` is used as an index — exactly as in memoization.

---

### Final Tabulation Code

```java
class Solution {
    public int lengthOfLIS(int[] nums) {
        int n = nums.length;

        // dp[index][prev + 1] = LIS length from 'index' onward,
        //                        given 'prev' was the last taken index
        // Java initializes to 0, so base case (index == n) is handled
        int[][] dp = new int[n + 1][n + 1];

        // Fill from index = n-1 down to 0 (opposite of recursion)
        for (int index = n - 1; index >= 0; index--) {

            // prev ranges from index-1 down to -1 (opposite of recursion)
            // Coordinate shift: store at (prev + 1)
            for (int prev = index - 1; prev >= -1; prev--) {

                // Choice 1: Not Take
                // WHY dp[index+1][prev+1]: skip current, move to next index
                //     previous doesn't change
                int notTake = dp[index + 1][prev + 1];

                // Choice 2: Take — only valid if current element > previous element
                // WHY prev == -1: no previous element yet, always allowed to take
                // WHY nums[index] > nums[prev]: enforces the "increasing" constraint
                int take = 0;
                if (prev == -1 || nums[index] > nums[prev]) {
                    // WHY dp[index+1][index+1]: moved to next index,
                    //     current index is now the new "previous"
                    //     coordinate shift: index+1 maps to (index)+1
                    take = 1 + dp[index + 1][index + 1];
                }

                // Store with coordinate shift on prev
                dp[index][prev + 1] = Math.max(notTake, take);
            }
        }

        // Answer: start from index 0, no previous element (prev = -1 → stored at 0)
        return dp[0][0];
    }
}
```

---

### Complexity

**Time — O(n²):**
Two nested loops. The outer runs `n` times. The inner runs up to `n` times per outer iteration. Each cell does O(1) work. Total: O(n²). This is a massive improvement over the exponential recursion.

**Space — O(n²):**
The dp array has size `(n+1) × (n+1)`. The recursion stack is gone — everything is iterative now.

---

## Approach 4 — Space Optimization

### Notice the recurrence:

```
dp[index][...] depends only on dp[index + 1][...]
```

Row `index` only ever looks at row `index + 1`. That means we don't need the full 2D table — just two 1D arrays: one for the "next" row (already computed) and one for the "current" row being filled.

```java
class Solution {
    public int lengthOfLIS(int[] nums) {
        int n = nums.length;

        // next[prev+1] = LIS length from the row below (index+1)
        int[] next = new int[n + 1];
        int[] curr = new int[n + 1];

        for (int index = n - 1; index >= 0; index--) {
            for (int prev = index - 1; prev >= -1; prev--) {

                int notTake = next[prev + 1];

                int take = 0;
                if (prev == -1 || nums[index] > nums[prev]) {
                    take = 1 + next[index + 1];
                }

                curr[prev + 1] = Math.max(notTake, take);
            }

            // Slide: current row becomes the new "next" row
            // WHY: when index decreases by 1, what was curr becomes
            //      the row that the next iteration reads from
            int[] temp = next;
            next = curr;
            curr = temp;
        }

        // Answer lives at next[0] after the loop
        // (the last swap made next point to the most recently filled row)
        return next[0];
    }
}
```

**Time — O(n²):** No change. Same two nested loops.

**Space — O(n):** No n×n array. Just two arrays of size n+1. The recursion stack is also gone.

---

### Key Takeaway

```
┌──────────────────────────────────────────────────────────────────┐
│  Converting memoization to tabulation — same rules every time:         │
│                                                                        │
│  1. Same dp array size as memoization                                  │
│  2. Base case (index == n → return 0) is already 0 in Java             │
│  3. Loops run in OPPOSITE direction from recursion                     │
│     index: n-1 → 0  (recursion went 0 → n)                             │
│     prev:  index-1 → -1  (recursion went -1 → index-1)                 │
│  4. Coordinate shift stays exactly the same as memoization             │
│     prev is stored at (prev + 1) everywhere                            │
│                                                                        │
│  Space optimization:                                                   │
│  dp[index] depends only on dp[index+1]                                 │
│  → replace n×n array with two arrays of size n+1                       │
│  → slide: after each row, next = curr                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

Ready for **Part 4** whenever you are — the cleaner O(n²) "algorithmic" tabulation where `dp[i]` = LIS length ending at index `i`, which is also the foundation for printing the actual subsequence.