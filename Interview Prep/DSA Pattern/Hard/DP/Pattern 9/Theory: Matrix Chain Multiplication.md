# GFG: Matrix Chain Multiplication

Key Concept: Pure Interval DP template
Problem: https://www.geeksforgeeks.org/problems/matrix-chain-multiplication0303/1?utm_source=youtube&utm_medium=collab_striver_ytdescription&utm_campaign=matrix-chain-multiplication
Solution: https://www.youtube.com/watch?v=vRVfmbCFW7Y&ab_channel=takeUforward
Status: Done

# Stage 1: Identification

## **Step 1 — Which topic?**

You are given the dimensions of a chain of matrices. You need to multiply all of them together, in order, but matrix multiplication is **associative** — `(A×B)×C` gives the same final matrix as `A×(B×C)`. What changes is not the answer, but the **number of scalar multiplication operations** needed to get there. You need to find the **order of multiplication (placement of parentheses)** that minimizes the total number of operations.

## **Step 2 — Which pattern?**

Here is the identification signal Striver gives, and it's worth internalizing precisely because it doesn't look like anything you've seen in Patterns 1–8:

![image.png](GFG%20Matrix%20Chain%20Multiplication/image.png)

> Whenever a problem can be solved by trying a value **in different positions**, and putting it in different positions changes the final answer, and you are asked for the **best** such placement — that is **`Partition DP`**.
> 

![image.png](GFG%20Matrix%20Chain%20Multiplication/image%201.png)

**Example 1**: `1 + 2 + 3 × 5`. Depending on where you place parentheses —

> `(1+2+3)×5` vs. `(1+2)+(3×5)`
> 

— you get a different final value. The *elements* stay the same; only *where you split the expression* changes the outcome. 

**Example 2**: Matrix multiplication has this exact same character: `A, B, C, D` stay the same four matrices, but

> `(AB)(CD)` vs. `A(B(CD))` vs. `((AB)C)D`
> 

— all cost a different number of multiplications.

This is **Pattern 9: `Interval DP`**, also called **`Partition DP`** — because the decision at every step is *where to place a partition (a split point) inside a range*.

## **Step 3 — Which key concept?**

Striver gives three rules that apply to **every** problem in this entire pattern — not just this one. Memorize these three rules as the template for the whole pattern:

![image.png](GFG%20Matrix%20Chain%20Multiplication/image%202.png)

```
**Rule 1**: Start with the ENTIRE block/array, represented as f(i, j)
        i = start point, j = end point of the range you're solving

**Rule 2**: Try ALL partitions — run a loop over every possible split point k
        inside the range, splitting it into f(i, k) and f(k+1, j)

**Rule 3**: Return the BEST among all those partitions
```

Every problem in Pattern 9 — MCM, Burst Balloons, Palindrome Partitioning, Merge Stones — is a variation of exactly these three rules. What changes between problems is **what "the best" means** (minimum here, could be maximum or count elsewhere) and **what "the cost of this partition" means** (a multiplication cost here, could be something else elsewhere).

---

# Stage 2: Intuition Building

### First, the Matrix Multiplication Mechanics

![image.png](GFG%20Matrix%20Chain%20Multiplication/image%203.png)

Before any DP, you need one piece of pure math: if matrix `A` has dimensions `p × q`, and matrix `B` has dimensions `q × r`, they can be multiplied **only if the inner dimensions match** (`q == q`). The resultant matrix has dimensions `p × r`, and the number of scalar multiplications required is:

![image.png](GFG%20Matrix%20Chain%20Multiplication/image%204.png)

```
cost = p × q × r
```

Take a concrete example: 

- `A` is `10 × 30`
- `B` is `30 × 5`

Multiplying gives a `10 × 5` result, costing `10 × 30 × 5 = 1500` operations.

### The Same Three Matrices, Two Different Costs

Given `A (10×30)`, `B (30×5)`, `C (5×60)` — multiply all three. There are exactly two ways to place parentheses for 3 matrices:

![image.png](GFG%20Matrix%20Chain%20Multiplication/image%205.png)

```
Way 1: (A × B) × C

  A × B → cost = 10 × 30 × 5 = 1500, result is 10 × 5
  (AB) × C → cost = 10 × 5 × 60 = 3000
  Total = 1500 + 3000 = 4500
```

![image.png](GFG%20Matrix%20Chain%20Multiplication/image%206.png)

```
Way 2: A × (B × C)

  B × C → cost = 30 × 5 × 60 = 9000, result is 30 × 60
  A × (BC) → cost = 10 × 30 × 60 = 18000
  Total = 9000 + 18000 = 27000
```

Same three matrices, same final product — but **4500 vs. 27000** operations depending purely on *where you place the parentheses*. This is the entire problem, made completely concrete: find the placement that minimizes total cost.

### Representing the Input as a Single Array

![image.png](GFG%20Matrix%20Chain%20Multiplication/image%207.png)

You're not handed the matrices directly — you're handed an array of dimensions. If there are `n` matrices, you need `n+1` numbers to describe all their dimensions, because consecutive matrices share a dimension (the column count of one equals the row count of the next).

```
arr = [10, 20, 30, 40, 50]     (n = 5 numbers → describes 4 matrices)

Matrix 1: arr[0] × arr[1]  =  10 × 20
Matrix 2: arr[1] × arr[2]  =  20 × 30
Matrix 3: arr[2] × arr[3]  =  30 × 40
Matrix 4: arr[3] × arr[4]  =  40 × 50
```

![image.png](GFG%20Matrix%20Chain%20Multiplication/image%208.png)

The general rule: **the i-th matrix has dimensions `arr[i-1] × arr[i]`**. This single formula is what lets you reconstruct any matrix's shape purely from its position in the array — you never need to store the matrices themselves, only this dimension array.

### Step 1 — Represent the Problem as f(i, j)

![image.png](GFG%20Matrix%20Chain%20Multiplication/image%209.png)

Following Rule 1 — start with the entire block. If there are 4 matrices (indices 1 through 4, using 1-based matrix numbering to match the array), define:

![image.png](GFG%20Matrix%20Chain%20Multiplication/image%2010.png)

```
f(i, j) = minimum number of scalar multiplications required
          to multiply matrices numbered i through j together
```

The overall answer you want is `f(1, n-1)` — where `n` is the length of the dimension array, so `n-1` is the number of matrices. Every problem in this pattern needs this same first move: figure out what the "entire block" actually represents for this problem, and call the function on it.

### Step 2 — Try All Partitions (Rule 2)

Say you're solving `f(1, 4)` — matrices A, B, C, D. There are exactly three ways to place the **outermost** split (the split doesn't need to consider every possible full parenthesization at once — recursion handles the rest):

![image.png](GFG%20Matrix%20Chain%20Multiplication/image%2011.png)

```
Split at k=1:  (A) × (B C D)
Split at k=2:  (A B) × (C D)
Split at k=3:  (A B C) × (D)
```

Each of these splits divides the problem into two smaller, independent sub-chains: `f(i, k)` and `f(k+1, j)`. You don't need to manually enumerate every possible full parenthesization — you only need to try every possible position for **one** split, and trust that the recursive calls on each side will themselves try every split for their own sub-ranges. This recursive delegation is what makes Rule 2 tractable: a loop over `k` from `i` to `j-1`.

```
for k = i to **j-1**:
    try splitting into f(i, k) and f(k+1, j)
```

**Why does `k` stop at `j-1` and not `j`?** Because the right partition is `k+1` to `j` — if `k` were allowed to reach `j`, the right side would become `j+1` to `j`, an empty and invalid range. The loop must always leave room for a non-empty right side.

### Step 3 — Compute the Cost of Each Partition, Return the Best

![image.png](GFG%20Matrix%20Chain%20Multiplication/image%2012.png)

For any split at position `k`, the *left* sub-chain (matrices `i` through `k`) multiplies down into a single resultant matrix, and the *right* sub-chain (matrices `k+1` through `j`) does the same. Then those two resultant matrices get multiplied together — and **that final multiplication's cost is what makes this problem non-trivial.**

What are the dimensions of that final multiplication? The left resultant matrix has dimensions `arr[i-1] × arr[k]` (its row count comes from the start of the left chain, its column count from where the split happens). The right resultant matrix has dimensions `arr[k] × arr[j]`. Multiplying them costs:

```
cost of this split = arr[i-1] × arr[k] × arr[j]
```

This is true **no matter how the left and right sub-chains were internally multiplied** — the shape of their resultant matrices is fixed by the dimension array alone, regardless of the order of internal operations. That's what makes the recursive decomposition valid: the sub-problems can be solved completely independently, and their internal cost simply adds to this final joining cost.

```
f(i, j) = min over all k in [i, j-1] of:
              f(i, k) + f(k+1, j) + arr[i-1] × arr[k] × arr[j]
```

### Base Case

If `i == j`, the range contains exactly one matrix — nothing to multiply, zero cost:

```
if i == j → return 0
```

### Visualizing the Recursion — Where Overlap Comes From

![image.png](GFG%20Matrix%20Chain%20Multiplication/image%2013.png)

```
f(1, 4)  splits into three tries:
    k=1: f(1,1) + f(2,4) + cost
    k=2: f(1,2) + f(3,4) + cost
    k=3: f(1,3) + f(4,4) + cost

f(2,4) itself splits further, and inside it you'll call f(3,4) again
f(1,3) itself splits further, and inside it you'll call f(1,2) again
```

`f(2,4)` gets computed once as part of the `k=1` branch, but `f(3,4)` (needed inside `f(2,4)`) also independently appears in the `k=2` branch directly. The same `(i, j)` sub-range gets asked for repeatedly through different paths in the recursion tree. **Overlapping subproblems confirmed** → DP applies.

### DP Table Size

Two parameters, `i` and `j`, both ranging across matrix indices `1` to `n-1`:

```
dp table: n × n   (using 1-indexed matrix numbers, sized generously to n × n)
```

---

# Stage 3: Coding

## Approach 1 — Pure Recursion (Brute Force)

```java
class Solution {
    public int matrixMultiplication(int[] arr, int n) {
        // n is the size of the dimension array; matrices are numbered 1 to n-1
        return solve(1, n - 1, arr);
    }

    private int solve(int i, int j, int[] arr) {
        // Base case: a single matrix — no multiplication needed
        if (i == j) return 0;

        int mini = Integer.MAX_VALUE;

        // Try every possible split point k inside [i, j-1]
        // WHY j-1 and not j: the right partition is (k+1, j) — k must
        //     leave at least one matrix for the right side
        for (int k = i; k <= j - 1; k++) {

            // Cost of this split:
            //   left sub-chain cost (recursively solved)
            // + right sub-chain cost (recursively solved)
            // + cost of multiplying the two resultant matrices together
            // WHY arr[i-1] * arr[k] * arr[j]: the left chain's resultant
            //     matrix is arr[i-1] x arr[k], the right chain's resultant
            //     is arr[k] x arr[j] — multiplying them costs this product
            int steps = solve(i, k, arr) + solve(k + 1, j, arr)
                        + arr[i - 1] * arr[k] * arr[j];

            mini = Math.min(mini, steps);
        }

        return mini;
    }
}
```

**Time Complexity — Exponential:**
At every state, the function tries every split point in its range, and each split spawns two more recursive calls. The branching factor grows with the range size itself, not a fixed constant, so the overall growth is exponential — roughly on the order of a Catalan-number recursion tree. For even moderately sized inputs, this is completely impractical.

**Space Complexity — O(n) for the recursion stack:**
No dp array is allocated. The deepest chain of recursive calls shrinks the range by at least one matrix per level, so the stack goes at most `n` levels deep.

---

## Approach 2 — Memoization (Top-Down DP)

The same `(i, j)` state — as shown in the overlap analysis — gets computed repeatedly from different branches. Store each result the first time it's computed.

```java
class Solution {
    public int matrixMultiplication(int[] arr, int n) {
        // dp[i][j] = -1 means not yet computed
        // WHY size n x n: i and j both range from 1 to n-1, so n covers it comfortably
        int[][] dp = new int[n][n];
        for (int[] row : dp) {
            Arrays.fill(row, -1);
        }
        return solve(1, n - 1, arr, dp);
    }

    private int solve(int i, int j, int[] arr, int[][] dp) {
        // Base case: a single matrix
        if (i == j) return 0;

        // Step 1: Already computed? Return stored value instantly
        // WHY: f(i, j) = min cost to multiply matrices i..j — this never
        //      changes no matter which branch of the recursion reached it
        if (dp[i][j] != -1) return dp[i][j];

        int mini = Integer.MAX_VALUE;

        for (int k = i; k <= j - 1; k++) {
            int steps = solve(i, k, arr, dp) + solve(k + 1, j, arr, dp)
                        + arr[i - 1] * arr[k] * arr[j];

            mini = Math.min(mini, steps);
        }

        // Step 2: Store before returning
        dp[i][j] = mini;
        return dp[i][j];
    }
}
```

**Time Complexity — O(n² × n) = O(n³):**
There are `n × n` unique `(i, j)` states. For each state, the inner loop over `k` can run up to `n` times in the worst case. Total: roughly `O(n³)`.

**Space Complexity — O(n²) + O(n):**
The `dp` array of size `n × n`, plus the recursion call stack going up to `n` levels deep.

**Why this still gets Time Limit Exceeded on stricter judges:** Striver flags this explicitly — memoization removes the *repeated work*, but the recursive call overhead (function calls, stack frames) is still heavy enough that some judges won't accept it. Tabulation is needed to fully clear the constraints.

---

You're right — apologies, I wrote a generic "length-ordered" tabulation instead of the one Striver actually taught in this lecture. His approach is different: he goes `i` from `n-1` down to `1`, and `j` from `i+1` onward — not by increasing range length. Let me redo it faithfully.

---

## Approach 3 — Tabulation (*New Approach & Concept*)

**Striver's rule for converting any memoization solution to tabulation — always the same three steps:**

```
**Step 1**: Copy the base case, don't overthink it
**Step 2**: Write down the changing parameters (the states)
**Step 3**: Copy the recurrence as-is
```

### Step 1 — Copy the Base Case

![image.png](GFG%20Matrix%20Chain%20Multiplication/image%2014.png)

In recursion, the base case was `if (i == j) return 0`. Translating this directly:

```
dp[i][i] = 0   for every i
```

This means: the *cost to multiply a single matrix with itself* is zero — no multiplication ever needed.

### 🧠Step 2 — The Changing Parameters, and Which Direction They Move

The two changing states are `i` and `j` — exactly as in recursion. Now think about **what top-down and bottom-up actually mean**, because that's what tells you which direction to loop.

![image.png](GFG%20Matrix%20Chain%20Multiplication/image%2015.png)

**Top-down** (what recursion does): you start with the **bigger problem** — the entire block of matrices — and break it down into smaller and smaller problems, until you hit the base case.

![image.png](GFG%20Matrix%20Chain%20Multiplication/image%2016.png)

**Bottom-up** (what tabulation does): the exact opposite. You start from the **smaller problem** (the base case) and build your way up to the bigger one.

![image.png](GFG%20Matrix%20Chain%20Multiplication/image%2017.png)

So ask: in recursion, where did `i` start, and where did it end up moving toward? The initial call was `f(1, n-1)`, and `i` kept moving forward toward `j` as the recursion went deeper. 

- `i` was going from `1` to `n-1`
- `j` was moving from `n-1` to `1`

Since tabulation must go in the **opposite** direction of recursion, `i` should run **backward** — starting from `n-1` and moving down toward `1`.

```
for i = n-1 to 1
	for j = 1 to n-1
```

This is **totally wrong!** (why?)

![image.png](GFG%20Matrix%20Chain%20Multiplication/image%2018.png)

**Now, think about `j`?** The naive instinct was: recursion's `j` started at `n-1` and moved backward, so tabulation's `j` should go forward from `1` to `n-1` — the direct opposite. 

But **this is wrong**, and Striver flags this explicitly as the one place you have to be practical rather than mechanical: `i` is *always* to the **left** of `j` — that's a structural fact of the problem, not just a coincidence of how recursion happened to run. `j` can never be smaller than `i`. So `j` cannot start at `1` — it must start at `i + 1`.

![image.png](GFG%20Matrix%20Chain%20Multiplication/image%2019.png)

```
for j = **i+1** to n-1
```

### Step 3 — Copy the Recurrence

The `k` loop and the cost formula are copied over exactly as they were in recursion — only `solve(...)` calls become `dp[...]` lookups.

```java
class Solution {
    public int matrixMultiplication(int[] arr, int n) {
        // dp[i][j] = min cost to multiply matrices i..j
        int[][] dp = new int[n][n];

        // Step 1: copy the base case directly — no need to overthink it
        for (int i = 1; i < n; i++) {
            dp[i][i] = 0;
        }

        // Step 2: i and j move in the OPPOSITE direction of recursion.
        // Recursion's i moved forward from 1 → tabulation's i moves
        // backward from n-1 down to 1.
        for (int i = n - 1; i >= 1; i--) {

            // WHY j starts at i+1, not 0: i is always to the left of j —
            // this is a structural fact, not something you can invert
            // just because it's "the opposite direction." Be practical
            // here, not purely mechanical.
            for (int j = i + 1; j < n; j++) {

                // Step 3: copy the recurrence exactly, replacing
                // recursive calls with dp lookups
                int mini = Integer.MAX_VALUE;

                for (int k = i; k <= j - 1; k++) {
                    int steps = dp[i][k] + dp[k + 1][j]
                                + arr[i - 1] * arr[k] * arr[j];

                    mini = Math.min(mini, steps);
                }

                dp[i][j] = mini;
            }
        }

        // Answer: the full chain, matrix 1 through matrix n-1
        return dp[1][n - 1];
    }
}
```

### Why This Loop Order Is Actually Safe

It's worth confirming that `dp[i][k]` and `dp[k+1][j]` are always already filled by the time `dp[i][j]` needs them, given this particular loop order:

```
dp[i][k]:    same row i, but k < j — already computed earlier in this
             same inner j-loop pass (j increases from i+1 upward)

dp[k+1][j]:  row k+1, where k+1 > i — since the OUTER loop runs i from
             n-1 DOWN to 1, every row with index greater than i has
             already been fully completed in an earlier outer iteration
```

So the "opposite direction" instinct is correct for `i` (that's what drives the outer loop downward, guaranteeing larger-indexed rows are done first) — it just needed the practical correction for `j`'s starting point.

### Full Cell-by-Cell Trace (Same Example)

`arr = [10, 20, 30, 40, 50]` (n = 5)

Since `i` runs from `4` down to `1`, and `j` runs from `i+1` to `4`, the fill order visits cells in this sequence: `(4,-)` has none since j must exceed i and j<n, so first real row is `i=3`:

```
i=3: j=4
    k=3: dp[3][3] + dp[4][4] + arr[2]*arr[3]*arr[4] = 0+0+30*40*50 = 60000
    dp[3][4] = 60000

i=2: j=3, then j=4
    j=3, k=2: dp[2][2]+dp[3][3]+arr[1]*arr[2]*arr[3] = 0+0+20*30*40 = 24000
    dp[2][3] = 24000

    j=4:
    k=2: dp[2][2]+dp[3][4]+arr[1]*arr[2]*arr[4] = 0+60000+20*30*50 = 90000
    k=3: dp[2][3]+dp[4][4]+arr[1]*arr[3]*arr[4] = 24000+0+20*40*50 = 64000
    dp[2][4] = min(90000, 64000) = 64000

i=1: j=2, j=3, then j=4
    j=2, k=1: dp[1][1]+dp[2][2]+arr[0]*arr[1]*arr[2] = 0+0+10*20*30 = 6000
    dp[1][2] = 6000

    j=3:
    k=1: dp[1][1]+dp[2][3]+arr[0]*arr[1]*arr[3] = 0+24000+10*20*40 = 32000
    k=2: dp[1][2]+dp[3][3]+arr[0]*arr[2]*arr[3] = 6000+0+10*30*40 = 18000
    dp[1][3] = min(32000, 18000) = 18000

    j=4:
    k=1: dp[1][1]+dp[2][4]+arr[0]*arr[1]*arr[4] = 0+64000+10*20*50 = 74000
    k=2: dp[1][2]+dp[3][4]+arr[0]*arr[2]*arr[4] = 6000+60000+10*30*50 = 81000
    k=3: dp[1][3]+dp[4][4]+arr[0]*arr[3]*arr[4] = 18000+0+10*40*50 = 38000
    dp[1][4] = min(74000, 81000, 38000) = 38000
```

**Answer = `dp[1][4] = 38000`** — same result as the recursive trace, confirming the tabulation matches.

**Time Complexity — O(n³):**
Two nested loops for `i` and `j` give `O(n²)` states, and for each state the inner `k` loop adds another `O(n)` factor.

**Space Complexity — O(n²):**
Only the `dp` array. No recursion stack, no auxiliary stack space — this is exactly the improvement over memoization that clears the stricter judges.

---

## Final Comparison

| Approach | Time | Space | Notes |
| --- | --- | --- | --- |
| Pure Recursion | Exponential | O(n) stack | Never use |
| Memoization | O(n³) | O(n²) + O(n) stack | Removes repeated work, but the auxiliary stack space still gets partially accepted on some judges |
| Tabulation | O(n³) | O(n²) | No auxiliary space — best, submit this |

---

## The Key Takeaway

```
┌────────────────────────────────────────────────────────────────────────┐
│  Converting memoization to tabulation — the same 3 steps as            │
│  always:                                                               │
│      1. Copy the base case                                             │
│      2. Write down the changing parameters (i, j)                      │
│      3. Copy the recurrence                                            │
│                                                                        │
│  Top-down vs bottom-up, in plain terms:                                │
│      Top-down (recursion): bigger problem → broken into smaller        │
│      Bottom-up (tabulation): smaller problem → built up to bigger      │
│                                                                        │
│  Direction of the loops — mostly "opposite of recursion,"              │
│  but be PRACTICAL, not purely mechanical:                              │
│      i: recursion moved forward from 1 → tabulation moves              │
│         backward, n-1 down to 1                                        │
│      j: naive instinct says "opposite of recursion" would mean         │
│         starting from 0 — but i is ALWAYS to the left of j,            │
│         a structural fact of the problem. So j must start at           │
│         i+1, not 0.                                                    │
│                                                                        │
│  This is the one place in this pattern's tabulation where              │
│  blindly inverting recursion's direction would break correctness —     │
│  you have to reason about what the indices actually MEAN, not          │
│  just mirror the loop bounds.                                          │
└────────────────────────────────────────────────────────────────────────┘
```