# LC 1547. Minimum Cost to Cut a Stick

Key Concept: Sorting the input, so that sub-problems don’t depend on each other.
Problem: https://leetcode.com/problems/minimum-cost-to-cut-a-stick/description/
Solution: https://www.youtube.com/watch?v=xwomavsC86c&ab_channel=takeUforward
Status: Done

# Stage 1: Identification

### First — My Own Attempt, and Why It Felt Right😑

Before looking at Striver's approach, the natural first instinct is to model this exactly like every other Interval DP problem so far: **partition on the stick itself**.

```java
private int func(int i, int j, int[] cuts) {
    if (i >= j) return 0;

    int minCost = Integer.MAX_VALUE;
    for (int cut : cuts) {
        if (i < cut && cut < j) {
            int cost = (j - i) + func(i, cut, cuts) + func(cut, j, cuts);
            minCost = Math.min(minCost, cost);
        }
    }
    return minCost == Integer.MAX_VALUE ? 0 : minCost;
}

public int minCost(int n, int[] cuts) {
    return func(0, n, cuts);
}
```

This is `f(i, j)` = minimum cost to fully cut the stick segment from position `i` to position `j`, trying every cut in the `cuts` array that lies strictly inside `(i, j)` as the *first* cut for this segment, recursing on the two resulting pieces, and taking the cost of this segment (`j - i`) plus the best split.

This is a completely faithful, correct application of the three-rule Partition DP template — start with the whole block, try every partition, take the best. It even **passes on small inputs**. The genuine appeal here is that it maps directly onto how you'd narrate the problem out loud: *"the stick from i to j costs (j−i) to cut once, so try every valid cut position and recurse on both halves."* Nothing about this reasoning is wrong.

Memoizing it is equally natural:

```java
int[][] dp = new int[n + 1][n + 1];
```

And this is exactly where it falls apart — **silently, at the level of constraints, not logic.**

### Why It `TLEs` — The Lesson That Matters Most Here

Look at LeetCode's actual constraints for this problem: `n` (the stick length) can be up to **10⁵**, while `cuts.length` is capped at **100**. The recursive state here is `(i, j)`, and `i, j` range over **positions on the stick** — meaning the state space is `O(n²)`, i.e., up to `(10⁵)² = 10¹⁰` possible `(i, j)` pairs. A `dp` array of that size cannot even be *allocated*, let alone filled. Worse, even ignoring memory, the inner loop tries every element of `cuts` at every state — so total work is `O(n² × cuts.length)`, catastrophically large.

**The number of cuts (≤100) is small. The stick length (≤10⁵) is large.** Any correct solution has to key its state off the *small* quantity, not the *large* one. This is the exact lesson: **check the constraints before committing to a state representation** — they tell you which quantity is allowed to appear in your `O(state²)` or `O(state³)` complexity, and which one absolutely cannot. Partitioning on stick positions `i, j` puts the *large* number (up to 10⁵) into both dimensions of the DP table — a guaranteed TLE/MLE regardless of how well the recursion itself is written. The logic wasn't broken; the **choice of what to index the DP by** was.

### The Reframe — Partition on the *Cuts*, Not the Stick

Striver's insight is to stop thinking of the state as "a range of stick positions" and instead think of it as **"a range of cuts, in sorted order."** Since `cuts.length ≤ 100`, indexing the DP by *positions within the cuts array* gives a state space of at most `100 × 100` — completely tractable, regardless of how long the stick itself is.

This genuinely isn't the first thing that comes to mind — it requires noticing that the *cuts themselves*, once sorted, form the sequence you should be partitioning, not the raw stick length. It's worth sitting with **why** this reframing is valid before jumping into it, because the validity isn't obvious on first glance.

---

# Stage 2: Intuition Building

### Setting Up the Example

```
Stick length = 7
cuts = [1, 3, 4, 5]
```

Cutting order matters for the total cost — cost of a single cut equals the length of the piece being cut at that moment. Try the order `[1, 3, 4, 5]` directly:

```
Cut at 1: stick is [0,7], length 7           → cost 7  → pieces [0,1], [1,7]
Cut at 3: piece [1,7] has length 6           → cost 6  → pieces [1,3], [3,7]
Cut at 4: piece [3,7] has length 4           → cost 4  → pieces [3,4], [4,7]
Cut at 5: piece [4,7] has length 3           → cost 3  → pieces [4,5], [5,7]

Total = 7 + 6 + 4 + 3 = 20
```

Now try reordering to `[3, 5, 1, 4]`:

```
Cut at 3: stick [0,7], length 7              → cost 7
Cut at 5: piece [3,7], length 4              → cost 4
Cut at 1: piece [0,3], length 3              → cost 3
Cut at 4: piece [3,5], length 2              → cost 2

Total = 7 + 4 + 3 + 2 = 16
```

Same cuts, same stick — a better order gives **16 instead of 20**. The problem asks for the minimum over all valid orderings.

### Why Sorting the Cuts Array Makes Sub-Problems Independent

![image.png](LC%201547%20Minimum%20Cost%20to%20Cut%20a%20Stick/image.png)

Suppose the first cut made is at position `4`. This immediately splits the stick into `[0, 4]` and `[4, 7]`. Every remaining cut in `[0, 4]` (like `1` and `3`) is now **physically disconnected** from every remaining cut in `[4, 7]` (like `5`). No matter what order you cut `1` and `3` in, it can never interact with or affect the cost of cutting `5` — they live on separate, severed pieces of wood.

![image.png](LC%201547%20Minimum%20Cost%20to%20Cut%20a%20Stick/image%201.png)

![image.png](LC%201547%20Minimum%20Cost%20to%20Cut%20a%20Stick/image%202.png)

This is only true because the cuts are considered **in sorted order** relative to the split point. If the cuts array were *not* sorted, you couldn't cleanly say "everything before this cut belongs to the left piece, everything after belongs to the right piece" — you'd have to explicitly check which side each cut falls on. Sorting guarantees a clean division: cuts with a smaller value automatically belong to the left sub-problem, cuts with a larger value automatically belong to the right sub-problem, **just by their position in the sorted array**. This is exactly why sorting is a prerequisite here, mirroring the same reasoning behind sorting in LC 368 (Largest Divisible Subset) and LC 1048 (Longest String Chain) — sorting converts an unordered choice problem into a clean left/right recursive split.

### Reframing the State — Partition on `Cut` *Indices*, Not Stick Positions

Instead of `f(i, j)` = "cost to cut stick segment from position i to position j," redefine:

```
f(i, j) = minimum cost to perform ALL cuts in cuts[i..j]
          (i and j are INDICES into the sorted cuts array, not stick positions)
```

To make the boundary math clean, **augment the sorted cuts array** by inserting `0` at the very front and `n` (the full stick length) at the very end:

![image.png](LC%201547%20Minimum%20Cost%20to%20Cut%20a%20Stick/image%203.png)

```
Original cuts (sorted): [1, 3, 4, 5]
Augmented:               [**0**, 1, 3, 4, 5, **7**]
                          ↑              ↑
                       left boundary  right boundary (stick length)
```

The actual cuts to perform are now at indices `1` through `4` of this augmented array (the `0` and `7` are boundary markers, not cuts themselves). The initial call is `f(1, 4)` — solve the entire range of real cuts.

### Step 1 — Represent as f(i, j) Over Cut Indices

```
f(i, j) = minimum cost to make all the cuts cuts[i], cuts[i+1], ..., cuts[j]
          (using the augmented array, where index i-1 and index j+1
           tell you the boundaries of the CURRENT uncut piece)
```

### Step 2 — Try Every Cut in [i, j] as the First Cut for This Range

Say you pick some index `k` between `i` and `j` to be cut **first**, within this range. What does cutting at `cuts[k]` cost, right now? It costs the length of the **current piece being cut** — and the current piece's boundaries are exactly `cuts[i-1]` (the last cut made to the left, or `0` if none) and `cuts[j+1]` (the last cut made to the right, or `n` if none).

```
cost of cutting at k right now = cuts[j+1] - cuts[i-1]
```

This is the critical realization Striver's approach hinges on: **no matter which of the cuts in `[i, j]` you pick to cut first, the length of the piece you're cutting is always the same** — `cuts[j+1] - cuts[i-1]` — because none of the cuts *inside* `[i, j]` have happened yet at this point; only the boundary cuts (just outside this range) have already happened. This is exactly why the earlier bigger-example walkthrough showed `j+1` minus `i-1` always recovering the current piece's length regardless of which cut inside the range came first.

After cutting at `k`, the range splits into two independent sub-ranges of cuts: `f(i, k-1)` (cuts strictly left of `k`) and `f(k+1, j)` (cuts strictly right of `k`).

```
f(i, j) = min over all k in [i, j] of:
              (cuts[j+1] - cuts[i-1]) + f(i, k-1) + f(k+1, j)
```

### Step 3 — Base Case

If `i > j`, there are no cuts left to make in this range — nothing to do, zero cost:

```
if i > j → return 0
```

### Visualizing on the Bigger Example (Stick Length 12)

```
Augmented cuts: [0, 3, 5, 7, 8, 12]
Real cut indices: 1 through 4 (values 3, 5, 7, 8)
```

Suppose the first cut chosen for the whole range is at index for value `3`:

```
Left sub-range: nothing between 0 and 3 (empty)
Right sub-range: cuts at 5, 7, 8 — this is f(i, j) for those indices
Cost of this cut = cuts[j+1] - cuts[i-1] = 12 - 0 = 12
```

Now, within that right sub-range `[5, 7, 8]`, suppose the next cut chosen is at `7`:

```
Left piece boundary: last cut was at 3 (to the left), next cut inside is 5
Right piece boundary: 8 on one side, 12 on the other

Cost of cutting at 7 here = cuts[j+1] - cuts[i-1] = 12 - 3 = 9
```

And so on — every time, the cost formula `cuts[j+1] - cuts[i-1]` correctly recovers the length of whatever piece currently contains the range being solved, because the two boundary values are exactly the *most recent* cuts made on either side.

### Are There Overlapping Subproblems?

Yes — the same `(i, j)` range of cut-indices gets reached through different choices of "which cut to do first," exactly the same overlap pattern seen in MCM. **DP applies.**

### DP Table Size

Both `i` and `j` range over indices into the augmented cuts array, which has size `cuts.length + 2`. So:

```
dp table: (cuts.length + 2) × (cuts.length + 2)
```

This is bounded by `102 × 102` in the worst case — **completely independent of the stick length `n`**, which is exactly what fixes the original TLE. This is the entire lesson made concrete: the state must be indexed by the *small* input (number of cuts), never the *large* one (stick length), even though the stick length still appears inside the cost formula as data.

---

# Stage 3: Coding

## Approach 1 — Pure Recursion (Brute Force)

```java
class Solution {
    public int minCost(int n, int[] cuts) {
        int c = cuts.length;

        // Build the augmented array: 0, all sorted cuts, n
        Arrays.sort(cuts);
        int[] newCuts = new int[c + 2];
        newCuts[0] = 0;
        for (int i = 0; i < c; i++) {
            newCuts[i + 1] = cuts[i];
        }
        newCuts[c + 1] = n;

        // Real cuts occupy indices 1 through c in newCuts
        return solve(1, c, newCuts);
    }

    private int solve(int i, int j, int[] cuts) {
        // Base case: no cuts left in this range
        if (i > j) return 0;

        int mini = Integer.MAX_VALUE;

        // Try every cut in [i, j] as the FIRST cut made in this range
        for (int k = i; k <= j; k++) {

            // WHY cuts[j+1] - cuts[i-1]: no cut strictly inside (i, j) has
            // happened yet, so the piece currently being cut is bounded
            // exactly by the last cut to the left (i-1) and the last
            // cut to the right (j+1) — regardless of which k is chosen
            int cost = cuts[j + 1] - cuts[i - 1]
                       + solve(i, k - 1, cuts)
                       + solve(k + 1, j, cuts);

            mini = Math.min(mini, cost);
        }

        return mini;
    }
}
```

**Time Complexity — Exponential:**
At every state, the loop tries every index `k` in range, and each choice spawns two recursive calls on shrinking ranges. This is structurally identical to MCM's exponential blow-up — the same Catalan-like explosion.

**Space Complexity — O(c) for the recursion stack**, where `c = cuts.length`. The range shrinks by at least one cut-index per level.

---

## Approach 2 — Memoization (Top-Down DP)

The same `(i, j)` range of cut-indices is reached from multiple branches — exactly the overlap pattern shown in Stage 2. Store each result once computed.

```java
class Solution {
    public int minCost(int n, int[] cuts) {
        int c = cuts.length;

        Arrays.sort(cuts);
        int[] newCuts = new int[c + 2];
        newCuts[0] = 0;
        for (int i = 0; i < c; i++) {
            newCuts[i + 1] = cuts[i];
        }
        newCuts[c + 1] = n;

        // dp[i][j] = -1 means not yet computed
        // WHY size (c+1) x (c+1): i and j are indices into the real-cut
        // range 1..c — sized generously to avoid any boundary issues
        int[][] dp = new int[c + 1][c + 1];
        for (int[] row : dp) {
            Arrays.fill(row, -1);
        }

        return solve(1, c, newCuts, dp);
    }

    private int solve(int i, int j, int[] cuts, int[][] dp) {
        // Base case: no cuts left in this range
        if (i > j) return 0;

        // Step 1: Already computed? Return stored value instantly
        // WHY: f(i, j) = min cost to make cuts[i..j] — never changes
        //      no matter which branch of the recursion reached it
        if (dp[i][j] != -1) return dp[i][j];

        int mini = Integer.MAX_VALUE;

        for (int k = i; k <= j; k++) {
            int cost = cuts[j + 1] - cuts[i - 1]
                       + solve(i, k - 1, cuts, dp)
                       + solve(k + 1, j, cuts, dp);

            mini = Math.min(mini, cost);
        }

        // Step 2: Store before returning
        dp[i][j] = mini;
        return dp[i][j];
    }
}
```

**Why this now actually clears the judge, unlike the position-based attempt:** the state space is `O(c²)` where `c ≤ 100`, giving at most `~10,000` states — utterly trivial, compared to the `O(n²)` blow-up (`n` up to `10⁵`) from partitioning on stick positions directly.

**Time Complexity — O(c³):**
There are `O(c²)` unique `(i, j)` states; each does up to `O(c)` work trying every `k`. Total: **O(c³)**, where `c = cuts.length ≤ 100` — trivially fast.

**Space Complexity — O(c²) + O(c):**
The `dp` array plus the recursion call stack.

---

## Approach 3 — Tabulation (Bottom-Up DP)

**Step 1 — Copy the base case.** `if (i > j) return 0` translates to: any cell where `i > j` is simply never touched and stays at its default `0` — Java's zero-initialization handles this automatically, no explicit fill needed.

**Step 2 — Write the changing parameters, in the opposite direction of recursion.** Recursion's `i` moved forward conceptually as it shrank ranges from the left; here — following the same practical correction learned from MCM — `i` must run **backward** (from `c` down to `1`), and `j`, since it must always stay `≥ i` in any valid non-empty range (mirroring the "`j` starts at `i+1`, not 0" lesson from MCM), runs from `i` upward to `c`.

**Step 3 — Copy the recurrence.**

```java
class Solution {
    public int minCost(int n, int[] cuts) {
        int c = cuts.length;

        Arrays.sort(cuts);
        int[] newCuts = new int[c + 2];
        newCuts[0] = 0;
        for (int i = 0; i < c; i++) {
            newCuts[i + 1] = cuts[i];
        }
        newCuts[c + 1] = n;

        // dp[i][j] = min cost to make cuts at indices i..j
        // Base case (i > j) needs no explicit fill — those cells are
        // simply never written to, staying at Java's default 0
        int[][] dp = new int[c + 2][c + 2];

        // i moves backward (opposite of how ranges shrink in recursion)
        for (int i = c; i >= 1; i--) {

            // j must stay >= i for a valid non-empty range — same
            // practical correction as MCM's tabulation
            for (int j = i; j <= c; j++) {

                int mini = Integer.MAX_VALUE;

                for (int k = i; k <= j; k++) {
                    int cost = newCuts[j + 1] - newCuts[i - 1]
                               + dp[i][k - 1]
                               + dp[k + 1][j];

                    mini = Math.min(mini, cost);
                }

                dp[i][j] = mini;
            }
        }

        return dp[1][c];
    }
}
```

### Full Cell-by-Cell DP Table Trace

`n = 7`, `cuts = [1, 3, 4, 5]` → `c = 4`, augmented array `[0, 1, 3, 4, 5, 7]`

```
Real cut indices: 1(→1), 2(→3), 3(→4), 4(→5)
```

**Length-1 ranges (i == j):**

```
dp[1][1]: only k=1 → cost = newCuts[2]-newCuts[0] + dp[1][0] + dp[2][1]
                    = 3 - 0 + 0 + 0 = 3
dp[2][2]: k=2 → cost = newCuts[3]-newCuts[1] + dp[2][1] + dp[3][2]
                    = 4 - 1 + 0 + 0 = 3
dp[3][3]: k=3 → cost = newCuts[4]-newCuts[2] + dp[3][2] + dp[4][3]
                    = 5 - 3 + 0 + 0 = 2
dp[4][4]: k=4 → cost = newCuts[5]-newCuts[3] + dp[4][3] + dp[5][4]
                    = 7 - 4 + 0 + 0 = 3
```

**Length-2 ranges:**

```
dp[1][2] (cuts at 1,3):
  k=1: newCuts[3]-newCuts[0] + dp[1][0] + dp[2][2] = 4-0+0+3 = 7
  k=2: newCuts[3]-newCuts[0] + dp[1][1] + dp[3][2] = 4-0+3+0 = 7
  dp[1][2] = 7

dp[2][3] (cuts at 3,4):
  k=2: newCuts[4]-newCuts[1] + dp[2][1] + dp[3][3] = 5-1+0+2 = 6
  k=3: newCuts[4]-newCuts[1] + dp[2][2] + dp[4][3] = 5-1+3+0 = 7
  dp[2][3] = 6

dp[3][4] (cuts at 4,5):
  k=3: newCuts[5]-newCuts[2] + dp[3][2] + dp[4][4] = 7-3+0+3 = 7
  k=4: newCuts[5]-newCuts[2] + dp[3][3] + dp[5][4] = 7-3+2+0 = 6
  dp[3][4] = 6
```

**Length-3 ranges:**

```
dp[1][3] (cuts at 1,3,4):
  k=1: newCuts[4]-newCuts[0] + dp[1][0] + dp[2][3] = 5-0+0+6 = 11
  k=2: newCuts[4]-newCuts[0] + dp[1][1] + dp[3][3] = 5-0+3+2 = 10
  k=3: newCuts[4]-newCuts[0] + dp[1][2] + dp[4][3] = 5-0+7+0 = 12
  dp[1][3] = 10

dp[2][4] (cuts at 3,4,5):
  k=2: newCuts[5]-newCuts[1] + dp[2][1] + dp[3][4] = 7-1+0+6 = 12
  k=3: newCuts[5]-newCuts[1] + dp[2][2] + dp[4][4] = 7-1+3+3 = 12
  k=4: newCuts[5]-newCuts[1] + dp[2][3] + dp[5][4] = 7-1+6+0 = 12
  dp[2][4] = 12
```

**Length-4 (full range):**

```
dp[1][4] (all cuts 1,3,4,5):
  k=1: newCuts[5]-newCuts[0] + dp[1][0] + dp[2][4] = 7-0+0+12 = 19
  k=2: newCuts[5]-newCuts[0] + dp[1][1] + dp[3][4] = 7-0+3+6  = 16
  k=3: newCuts[5]-newCuts[0] + dp[1][2] + dp[4][4] = 7-0+7+3  = 17
  k=4: newCuts[5]-newCuts[0] + dp[1][3] + dp[5][4] = 7-0+10+0 = 17

  dp[1][4] = min(19, 16, 17, 17) = 16
```

**Answer = `dp[1][4] = 16`** ✓ — matches the hand-computed minimum from Stage 2 exactly (the order `3, 5, 1, 4` gave 16 by direct simulation).

**Time Complexity — O(c³):**
Same as memoization — `O(c²)` states, `O(c)` work each.

**Space Complexity — O(c²):**
Only the `dp` array. No recursion stack.

---

## Why This Problem Has No Meaningful Space Optimization

Exactly the same reasoning as MCM: `dp[i][j]` depends on `dp[i][k-1]` and `dp[k+1][j]` for every `k` in the range — dependencies scattered across many different lengths and positions in the table, not confined to one adjacent row or column. The full table is the practical floor, same as every other problem in this pattern so far.

---

## Final Comparison

| Approach | Time | Space | Notes |
| --- | --- | --- | --- |
| Pure Recursion (on stick positions) | Exponential, `O(n²)` state space | Infeasible — `n` up to 10⁵ | **TLE/MLE regardless of pruning** |
| Pure Recursion (on cut indices) | Exponential | O(c) stack | Correct shape, still too slow without memo |
| Memoization (on cut indices) | O(c³) | O(c²) + O(c) stack | Clears the judge — `c ≤ 100` |
| Tabulation (on cut indices) | O(c³) | **O(c²)** | Best — submit this |

---

## The Key Takeaway

```
┌────────────────────────────────────────────────────────────────────────┐
│  THE lesson of this problem: constraints determine the state.          │
│                                                                        │
│  Partitioning on STICK POSITIONS (i, j as positions 0..n) felt         │
│  the most intuitive — it directly mirrors the problem statement        │
│  ("cut a stick of length n"). It is logically correct. It TLEs         │
│  anyway, because n can be up to 10⁵, making the state space            │
│  O(n²) = up to 10¹⁰ — impossible regardless of code quality.           │
│                                                                        │
│  The fix has nothing to do with the RECURRENCE logic — it's a          │
│  pure choice of WHAT TO INDEX THE DP BY. Since cuts.length is          │
│  capped at 100 (much smaller than n), partition on CUT INDICES         │
│  instead of stick positions. State space becomes O(c²) ≤ 10⁴ —         │
│  trivial.                                                              │
│                                                                        │
│  Practical rule going forward: whenever a problem gives you two        │
│  quantities of very different size caps (here: n ≤ 10⁵ vs.             │
│  cuts.length ≤ 100), and your natural DP state would be indexed        │
│  by the LARGE one, actively ask whether the SMALL one can serve        │
│  as the index instead — even if it requires re-deriving what           │
│  "the current segment" means in terms of that smaller quantity.        │
│                                                                        │
│  The trick that makes cut-index partitioning work: augment the         │
│  sorted cuts array with 0 at the front and n at the back. Then         │
│  cuts[j+1] - cuts[i-1] always recovers the length of whatever          │
│  piece currently contains the range [i, j], because no cut             │
│  strictly inside (i, j) has happened yet — only the boundary           │
│  cuts just outside the range have.                                     │
│                                                                        │
│  Sorting the cuts is what makes the left and right sub-ranges          │
│  provably independent — a cut with a smaller value can never           │
│  interact with a cut of larger value once a split has been made,       │
│  because they end up on physically separate pieces of the stick.       │
└────────────────────────────────────────────────────────────────────────┘
```