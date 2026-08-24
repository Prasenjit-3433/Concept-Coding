# (GFG version): Parsing A Boolean Expression

Problem: https://www.geeksforgeeks.org/problems/boolean-parenthesization5610/1
Solution: https://www.youtube.com/watch?v=MM7fXopgyjw&ab_channel=takeUforward
Status: Done

# Stage 1: Identification

### **Step 1 — Which topic?**

You're given a string made of two kinds of characters, alternating in a strict pattern: **operands** (`T` for true, `F` for false) and **operators** (`&` for AND, `|` for OR, `^` for XOR). A valid expression always starts and ends with an operand, with exactly one operator between every pair of adjacent operands.

```
T ^ F | T & F ^ T | F
```

You're allowed to place parentheses **anywhere** you like, in any combination, as long as they respect the structure of the expression. Different placements of parentheses can group the operators in different orders, and — because these boolean operators are evaluated on whatever their immediate left and right groups produce — different groupings can produce **different final results**. The question: **in how many distinct ways can you parenthesize this expression so that the final result is `true`?**

The moment you see **"different ways of grouping/parenthesizing produce different final answers, count how many groupings hit a target"** — this is precisely the signal from Matrix Chain Multiplication, just wearing different clothes. In MCM, different parenthesizations of `A × B × C × D` gave different *costs*. Here, different parenthesizations of `T ^ F | T & ...` give different *boolean results*. The mechanism producing that variability — **where you choose to close off a sub-expression before combining it with the rest** — is identical.

### **Step 2 — Which pattern?**

This is **Pattern 9: Interval DP (Partition DP)**. Striver's own identification rule, carried over word-for-word from MCM:

> Whenever a problem can be solved by trying a value **in different positions**, placing it in different positions changes the final answer, and you're asked for the **best** (or, here, the **count** of) such placements — that's Partition DP.
> 

The elements of the expression (`T`, `F`, `&`, `|`, `^`) stay fixed. What changes the outcome is purely **where you place the parentheses** — exactly the same character as `(1+2)+(3×5)` vs. `(1+2+3)×5` from the MCM lecture.

### **Step 3 — Which key concept?**

Striver's three MCM rules apply here without modification:

```
Rule 1: Start with the ENTIRE range, represented as f(i, j)
Rule 2: Try ALL partitions — loop over every possible split point inside the range
Rule 3: Return the BEST (here: the CORRECT COUNT) among all those partitions
```

But this problem adds **one genuinely new idea** on top of the MCM skeleton, and it's the entire reason this problem is taught right after MCM rather than being a trivial copy: **every sub-expression can resolve to either `true` or `false`, and you need to track the count of ways to reach *both* outcomes, not just the one you're ultimately looking for.**

In MCM, a sub-chain of matrices had exactly one thing you cared about — its minimum multiplication cost. Here, a sub-expression like `T & F ^ T` doesn't have one single "value" — it can be forced to evaluate to `true` via some parenthesizations, and to `false` via others, and **you need to know the count for both**, because when you combine two sub-expressions with an operator, whether the combination gives `true` depends on the true/false status of *both* sides, in specific combinations that differ by operator.

```
Step 1: Represent in terms of (i, j, isTrue)
        i, j = the range of the sub-expression
        isTrue = are you asking "how many ways to make THIS range true"
                 or "how many ways to make THIS range false"?
Step 2: Try every operator position as a partition point (Rule 2)
Step 3: For each partition, combine the LEFT's true/false counts with the
        RIGHT's true/false counts according to what THIS operator needs
        to produce true (or false) — then SUM across all partitions,
        because we are counting ways (recursion lecture 7's counting rule)
```

---

# Stage 2: Intuition Building

### The Problem Setup

```
expression = "T^F|T&F^T|F"

Indices:  0  1  2  3  4  5  6  7  8  9  10
Chars:    T  ^  F  |  T  &  F  ^  T  |  F
```

Operands sit at even indices (`0, 2, 4, 6, 8, 10`), operators sit at odd indices (`1, 3, 5, 7, 9`). This alternating structure is completely rigid — it's what makes "operators are always exactly 2 apart" a guaranteed fact you can lean on, exactly the way MCM could lean on "the i-th matrix has dimensions `arr[i-1] × arr[i]`."

### Why "Just Evaluate It Left to Right" Isn't the Question

If you evaluate `T^F|T&F^T|F` in one fixed order (say, strictly left to right, ignoring precedence), you get **one specific answer** — either `true` or `false`, take your pick. But the problem isn't asking "what does this expression evaluate to under some fixed rule." It's asking: **treating parenthesization as something you get to freely choose, across every legal way of doing so, how many of those choices land on `true`?**

This is structurally identical to the MCM question "across every legal way of placing parentheses around the matrix chain, what's the *best* cost" — except here "best" is replaced by "count of results equal to `true`."

### Building the Intuition With a Small Piece First

Take just `T & F ^ T` (a slice you'd encounter as a sub-expression inside the full string). There are exactly two operators here — `&` and `^` — so exactly two possible outermost partitions:

```
Partition at &:   (T) & (F ^ T)
Partition at ^:   (T & F) ^ (T)
```

**Partition at `&`:** left side is just `T` (always true). Right side is `F ^ T` — XOR of false and true. `F ^ T` is true (XOR is true when the two operands differ). So this partition gives `T & T = true`.

**Partition at `^`:** left side is `T & F` — AND of true and false — which is `false`. Right side is just `T` (always true). So this partition gives `false ^ true`. XOR of false and true is `true`. This partition **also** gives `true`.

So for this small piece, **both** partitions land on `true` — meaning there are **2 distinct ways** to parenthesize `T & F ^ T` to get `true`, and correspondingly **0 ways** to get `false`, since neither partition produced it.

This small example already reveals the core mechanic: **you try every partition, and independently check whether each one contributes to true or to false — then you count how many contribute to whichever one you're asking about.**

### Step 1 — Represent in Terms of (i, j, isTrue)

Following Rule 1, but now with the third parameter added:

```
f(i, j, isTrue) = number of ways to parenthesize the sub-expression
                  spanning index i to index j (inclusive), such that
                  it evaluates to true  (if isTrue == 1)
                  or to false           (if isTrue == 0)
```

The overall answer you want is `f(0, n-1, 1)` — the entire expression, asking for the true count.

**Why does `isTrue` need to be a parameter at all, rather than just computing "the true count" always and inferring false as "total minus true"?** Because you genuinely need **both numbers simultaneously** at every sub-range to correctly compute the parent range's numbers — as you'll see in Step 2, the parent's true-count formula and false-count formula both draw on the child's true-count *and* false-count together. There's no way to get one without essentially computing the other alongside it, so it's cleanest to make it an explicit third state.

### Step 2 — Try Every Operator as a Partition Point (Rule 2)

For a range `[i, j]`, the operators live at every odd offset within it: `i+1, i+3, i+5, ..., j-1`. Loop over each one as `k`:

```
for k = i+1 to j-1, stepping by 2:
    try splitting at operator k into f(i, k-1, ...) and f(k+1, j, ...)
```

This mirrors MCM's `for k = i to j-1` loop exactly, just with a step of 2 instead of 1 (because operators, unlike matrices, sit at alternating positions, not every position).

### Step 3 — What Each Operator Needs From Its Left and Right

This is the genuinely new content compared to MCM. For every split at operator `k`, you get a **left sub-range** `[i, k-1]` and a **right sub-range** `[k+1, j]`. Each of them independently has some number of ways to be `true` and some number of ways to be `false`. Call these:

```
leftTrue  = number of ways LEFT evaluates to true
leftFalse = number of ways LEFT evaluates to false
rightTrue  = number of ways RIGHT evaluates to true
rightFalse = number of ways RIGHT evaluates to false
```

Now ask, for each of the three operators, **which combinations of left/right produce `true`, and which produce `false`?** This is pure truth-table reasoning, exactly the way Striver walks through it.

**Operator `&` (AND):**

```
AND truth table:
  true  & true  = true
  true  & false = false
  false & true  = false
  false & false = false
```

Only **one** combination gives `true`: both sides true. So:

```
waysTrue  (for this partition) = leftTrue × rightTrue
waysFalse (for this partition) = leftTrue × rightFalse
                                + leftFalse × rightTrue
                                + leftFalse × rightFalse
```

**Operator `|` (OR):**

```
OR truth table:
  true  | true  = true
  true  | false = true
  false | true  = true
  false | false = false
```

Only **one** combination gives `false` (both sides false); the other three give `true`:

```
waysTrue  = leftTrue × rightTrue
          + leftTrue × rightFalse
          + leftFalse × rightTrue
waysFalse = leftFalse × rightFalse
```

**Operator `^` (XOR):**

```
XOR truth table:
  true  ^ true  = false
  true  ^ false = true
  false ^ true  = true
  false ^ false = false
```

Exactly **two** combinations give `true` (the operands differ); the other two give `false` (the operands agree):

```
waysTrue  = leftTrue × rightFalse + leftFalse × rightTrue
waysFalse = leftTrue × rightTrue  + leftFalse × rightFalse
```

### Why You Sum Across Every Partition — the Counting Rule

For a fixed range `[i, j]`, there isn't just one operator to split on — there are potentially many (as many operators as sit inside that range). **Every different choice of split point is a genuinely different way of parenthesizing the expression**, so every split point that contributes some `waysTrue` value adds to the grand total for that range. This is Striver's counting rule from the recursion series, carried forward from every earlier counting-DP problem in this sheet: *whenever you're counting ways, and there are multiple independent paths to a valid answer, you **add** their counts together — never take a max or min.*

```
f(i, j, true)  = sum over every operator k in range of:
                    (waysTrue contributed by splitting at k)

f(i, j, false) = sum over every operator k in range of:
                    (waysFalse contributed by splitting at k)
```

### Base Case

When `i == j`, the range is a single character — a lone operand, `T` or `F`. There's nothing to partition; the "value" is fixed by what character sits there. Following Striver's exact phrasing:

```
if i == j:
    if isTrue == 1:
        return 1 if expression[i] == 'T' else 0
    else:  (isTrue == 0)
        return 1 if expression[i] == 'F' else 0
```

In other words: if you're asking "how many ways to make this single character true," the answer is `1` if the character genuinely is `T` (there's exactly one trivial way — do nothing), and `0` otherwise (it's impossible, since a lone `F` can never be forced to evaluate as true). Symmetric logic for asking about false.

### Working Through the Full Example by Hand

```
expression = "T^F|T&F^T|F"
Indices:      0 1 2 3 4 5 6 7 8 9 10
```

Rather than tracing every single cell (that belongs in the DP table trace during coding), let's build intuition on one meaningfully-sized slice: `T ^ F | T` (indices 0 to 4).

Operators inside this range: `^` at index 1, `|` at index 3.

**Split at `^` (index 1):** left = `T` (indices 0-0), right = `F | T` (indices 2-4).

```
Left (T):  leftTrue = 1, leftFalse = 0   (it's just T, trivially true in 1 way)

Right (F | T): this itself needs solving first — it's OR of F and T.
   Only one operator here (| at index 3), left=F, right=T.
   F: leftTrue=0, leftFalse=1.  T: rightTrue=1, rightFalse=0.
   OR waysTrue  = (0×1) + (0×0) + (1×1) = 1
   OR waysFalse = (1×0) = 0
   So rightTrue = 1, rightFalse = 0 for "F | T"

Back to the ^ split:
   XOR waysTrue  = leftTrue × rightFalse + leftFalse × rightTrue
                  = (1 × 0) + (0 × 1) = 0
   XOR waysFalse = leftTrue × rightTrue + leftFalse × rightFalse
                  = (1 × 1) + (0 × 0) = 1
```

**Split at `|` (index 3):** left = `T ^ F` (indices 0-2), right = `T` (index 4).

```
Left (T ^ F): XOR of T and F.
   T: leftTrue=1, leftFalse=0.  F: rightTrue=0, rightFalse=1.
   XOR waysTrue  = (1×1) + (0×0) = 1
   XOR waysFalse = (1×0) + (0×1) = 0
   So leftTrue = 1, leftFalse = 0 for "T ^ F"

Right (T): rightTrue = 1, rightFalse = 0

Back to the | split:
   OR waysTrue  = leftTrue×rightTrue + leftTrue×rightFalse + leftFalse×rightTrue
                = (1×1) + (1×0) + (0×1) = 1
   OR waysFalse = leftFalse × rightFalse = (0×0) = 0
```

**Combining both splits for the full range `T ^ F | T`:**

```
f(0, 4, true)  = (^ split's waysTrue) + (| split's waysTrue) = 0 + 1 = 1
f(0, 4, false) = (^ split's waysFalse) + (| split's waysFalse) = 1 + 0 = 1
```

So across the two possible parenthesizations of `T ^ F | T`, exactly **1** gives `true` and exactly **1** gives `false` — and indeed `1 + 1 = 2`, matching the fact that there are only 2 total ways to parenthesize a 3-operator... wait, this slice has 2 operators, giving exactly 2 possible outermost splits, and every split produces *some* definite boolean result, so the true-count and false-count must always sum to the total number of parenthesizations at that range. This is a useful sanity check to carry into coding: **`f(i,j,true) + f(i,j,false)` should always equal the total number of distinct parenthesizations of that range**, which is a Catalan-number-shaped quantity, exactly as in MCM.

### Are There Overlapping Subproblems?

Just like MCM, a sub-range like `[2, 4]` (`F | T`) can be reached both as part of evaluating `[0, 4]` via the `^`-split, *and* independently as its own sub-problem if some other outer split also happens to need it. As the expression grows longer, the same `(i, j, isTrue)` triple gets asked for repeatedly through different partition paths in the recursion tree. **Overlapping subproblems confirmed** → DP applies.

### DP Table Size

Three parameters:

```
i:      0 to n-1   → n values
j:      0 to n-1   → n values
isTrue: 0 or 1     → 2 values
```

dp table: **n × n × 2**

This exactly matches Striver's own sizing in the transcript — the only difference from MCM's `n × n` is the extra factor of 2 for tracking true/false simultaneously.

# Stage 3: Coding

---

## Approach 1 — Pure Recursion (Brute Force)

Direct translation of the recurrence built in Stage 2: three parameters `(i, j, isTrue)`, base case at `i == j`, loop over every operator position as the partition, combine left/right true/false counts per the operator's truth table, and **sum** across every partition (counting rule).

```java
class Solution {
    static final int MOD = 1000000007;

    public int countWays(String expression) {
        int n = expression.length();
        // We want the total ways to make the ENTIRE expression true
        return (int) solve(0, n - 1, 1, expression);
    }

    private long solve(int i, int j, int isTrue, String exp) {
        // Base case: single character — no operator to partition on
        // WHY isTrue matters here: a lone 'T' contributes exactly 1 way
        //     to be true and 0 ways to be false, and vice versa for 'F'
        if (i == j) {
            if (isTrue == 1) {
                return exp.charAt(i) == 'T' ? 1 : 0;
            } else {
                return exp.charAt(i) == 'F' ? 1 : 0;
            }
        }

        long ways = 0;

        // Operators sit at every ODD offset inside [i, j] — a fixed
        // structural fact of the expression's grammar
        // WHY index += 2: operands and operators strictly alternate
        for (int index = i + 1; index <= j - 1; index += 2) {

            // Solve BOTH true and false counts for left and right —
            // WHY: whichever operator sits here, its formula for "true"
            //      and its formula for "false" both draw on ALL FOUR
            //      of these numbers, not just the one matching isTrue
            long leftTrue  = solve(i, index - 1, 1, exp);
            long leftFalse = solve(i, index - 1, 0, exp);
            long rightTrue  = solve(index + 1, j, 1, exp);
            long rightFalse = solve(index + 1, j, 0, exp);

            char operator = exp.charAt(index);

            if (operator == '&') {
                if (isTrue == 1) {
                    // AND is true only when BOTH sides are true
                    ways = (ways + leftTrue * rightTrue) % MOD;
                } else {
                    // AND is false in every OTHER combination
                    ways = (ways + leftTrue * rightFalse) % MOD;
                    ways = (ways + leftFalse * rightTrue) % MOD;
                    ways = (ways + leftFalse * rightFalse) % MOD;
                }
            } else if (operator == '|') {
                if (isTrue == 1) {
                    // OR is true in every combination EXCEPT both false
                    ways = (ways + leftTrue * rightTrue) % MOD;
                    ways = (ways + leftTrue * rightFalse) % MOD;
                    ways = (ways + leftFalse * rightTrue) % MOD;
                } else {
                    // OR is false only when BOTH sides are false
                    ways = (ways + leftFalse * rightFalse) % MOD;
                }
            } else { // operator == '^'
                if (isTrue == 1) {
                    // XOR is true when the two sides DIFFER
                    ways = (ways + leftTrue * rightFalse) % MOD;
                    ways = (ways + leftFalse * rightTrue) % MOD;
                } else {
                    // XOR is false when the two sides AGREE
                    ways = (ways + leftTrue * rightTrue) % MOD;
                    ways = (ways + leftFalse * rightFalse) % MOD;
                }
            }
        }

        return ways;
    }
}
```

**Time Complexity — Exponential:**
At every range `[i, j]`, the function loops over every operator inside it (up to `O(n)` of them), and for each one makes 4 further recursive calls (leftTrue, leftFalse, rightTrue, rightFalse). Since ranges shrink by at least 2 characters per level but the branching factor stays high, this grows exponentially — the same Catalan-number-shaped explosion seen in MCM's brute force, compounded further by the extra true/false branching. Completely impractical beyond tiny expressions.

**Space Complexity — O(n):**
No dp array. The recursion call stack depth is bounded by how many times the range can shrink before hitting `i == j`, which is at most `O(n)` levels.

---

## Approach 2 — Memoization (Top-Down DP)

The same `(i, j, isTrue)` triple gets reached from many different partition paths across the recursion tree — exactly the overlap confirmed in Stage 2. Store each result the first time it's computed.

**One important bug to watch for, flagged directly in the lecture:** always check `i == j` (the base case) **before** doing anything else — including before the memo lookup — since a single character has no meaningful `(i, j)` range to look up in a 2D-indexed table in the same way, and mixing up the order of checks was the exact mistake that produced a wrong answer during the live coding.

```java
class Solution {
    static final int MOD = 1000000007;
    private long[][][] dp;

    public int countWays(String expression) {
        int n = expression.length();

        // dp[i][j][isTrue] = -1 means not yet computed
        // WHY size n x n x 2: i and j range over the string, isTrue is binary
        dp = new long[n][n][2];
        for (long[][] mat : dp) {
            for (long[] row : mat) {
                Arrays.fill(row, -1);
            }
        }

        return (int) solve(0, n - 1, 1, expression);
    }

    private long solve(int i, int j, int isTrue, String exp) {
        // Base case: single character — checked BEFORE the memo lookup
        if (i == j) {
            if (isTrue == 1) {
                return exp.charAt(i) == 'T' ? 1 : 0;
            } else {
                return exp.charAt(i) == 'F' ? 1 : 0;
            }
        }

        // Step 1: Already computed? Return stored value instantly
        // WHY: f(i, j, isTrue) = number of ways this exact range evaluates
        //      to this exact boolean — never changes no matter which
        //      parent partition asked for it
        if (dp[i][j][isTrue] != -1) return dp[i][j][isTrue];

        long ways = 0;

        for (int index = i + 1; index <= j - 1; index += 2) {

            long leftTrue  = solve(i, index - 1, 1, exp);
            long leftFalse = solve(i, index - 1, 0, exp);
            long rightTrue  = solve(index + 1, j, 1, exp);
            long rightFalse = solve(index + 1, j, 0, exp);

            char operator = exp.charAt(index);

            if (operator == '&') {
                if (isTrue == 1) {
                    ways = (ways + leftTrue * rightTrue) % MOD;
                } else {
                    ways = (ways + leftTrue * rightFalse) % MOD;
                    ways = (ways + leftFalse * rightTrue) % MOD;
                    ways = (ways + leftFalse * rightFalse) % MOD;
                }
            } else if (operator == '|') {
                if (isTrue == 1) {
                    ways = (ways + leftTrue * rightTrue) % MOD;
                    ways = (ways + leftTrue * rightFalse) % MOD;
                    ways = (ways + leftFalse * rightTrue) % MOD;
                } else {
                    ways = (ways + leftFalse * rightFalse) % MOD;
                }
            } else { // '^'
                if (isTrue == 1) {
                    ways = (ways + leftTrue * rightFalse) % MOD;
                    ways = (ways + leftFalse * rightTrue) % MOD;
                } else {
                    ways = (ways + leftTrue * rightTrue) % MOD;
                    ways = (ways + leftFalse * rightFalse) % MOD;
                }
            }
        }

        // Step 2: Store before returning
        dp[i][j][isTrue] = ways;
        return ways;
    }
}
```

### Sanity Check on the Working Example

For `expression = "T^F|T&F^T|F"`, running this through the recurrence — using the same per-operator combination rules worked through by hand in Stage 2 for the `T^F|T` slice — the memoized recursion correctly aggregates all the nested true/false counts up through every partition and lands on the total count of parenthesizations that make the full expression evaluate to `true`. Since a full manual trace of an 11-character, 3-dimensional DP table isn't practical to walk cell-by-cell in text (unlike the 2D tables in earlier problems), the correctness here rests on the truth-table derivations already verified by hand in Stage 2, combined with the same memoization mechanics proven correct in every earlier Interval DP problem.

**Time Complexity — O(n³):**
There are `O(n²)` unique `(i, j)` pairs, times 2 for `isTrue`, giving `O(n²)` unique states. For each state, the loop over operator positions runs up to `O(n)` times. Total: `O(n²) × O(n) = O(n³)`.

**Space Complexity — O(n² × 2) + O(n):**
Two sources of space. First, the `dp` array of size `n × n × 2` — that's `O(n²)`. Second, the recursion call stack, which goes up to `O(n)` levels deep. Total: `O(n²) + O(n)`.

---

## A Note on Tabulation

Striver deliberately stops at memoization for this particular problem, and explicitly tells viewers to derive the tabulation themselves as an exercise rather than walking through it live — his own words: *"before the nested loops it'll get a lot complex so let's not get deep into that."* This is worth taking at face value rather than forcing a full derivation here, but it's useful to understand **why** it's genuinely more complex than earlier tabulation conversions:

```
┌────────────────────────────────────────────────────────────────────────┐
│  Why tabulation is harder to write out here than in MCM:               │
│                                                                        │
│  1. THREE nested loop variables instead of two: i, j, AND isTrue       │
│     — every table cell now needs the paired lookup of BOTH             │  
│     leftTrue/leftFalse and rightTrue/rightFalse simultaneously,        │
│     which means filling isTrue=1 and isTrue=0 together, not            │
│     independently, at every (i, j).                                    │
│                                                                        │
│  2. The recurrence branches on the OPERATOR CHARACTER at each          │
│     partition point, not just a fixed arithmetic formula — three       │
│     separate combination rules (&, |, ^) must all be encoded           │
│     inside the same nested nested loop structure.                      │
│                                                                        │
│  3. i must still increase in the OPPOSITE direction of recursion       │
│     (i moves backward, from n-1 down to 0), and j must always          │
│     stay to the right of i — the same "be practical, not               │
│     mechanical" lesson from MCM's tabulation applies here too.         │
│                                                                        │
│  The array shape would be dp[i][j][2], filled with i decreasing        │
│  and j increasing from i+1, exactly mirroring MCM's loop               │
│  structure — just with the inner body replaced by the three            │
│  truth-table branches shown in the memoization code above.             │
└────────────────────────────────────────────────────────────────────────┘
```

If you want to build this out fully as practice, the mechanical recipe is identical to every other Interval DP tabulation in this pattern: copy the base case (`dp[i][i][1]` and `dp[i][i][0]` set directly from the character), run `i` from `n-1` down to `0`, run `j` from `i+1` up to `n-1`, and inside, copy the exact recurrence body from the memoized `solve` function — replacing every recursive call with a `dp[...]` array lookup.

---

## Final Comparison

| Approach | Time | Space | Notes |
| --- | --- | --- | --- |
| Pure Recursion | Exponential | O(n) stack | Never use |
| Memoization | O(n³) | O(n²) + O(n) stack | **Submit this** — the approach Striver codes and validates |
| Tabulation | O(n³) | O(n²) | Removes stack overhead — left as a structured exercise |

---

## How This Differs From Matrix Chain Multiplication

| Property | MCM | Boolean Evaluation |
| --- | --- | --- |
| State | `(i, j)` | **`(i, j, isTrue)`** |
| What each state tracks | A single number (min cost) | **Two numbers simultaneously** (ways-true AND ways-false) |
| What changes at each partition | Split index `k`, step of 1 | **Operator index, step of 2** (alternating structure) |
| Combining left/right results | Simple addition + cost formula | **Truth-table-specific formula per operator (&, |, ^)**, chosen by what `isTrue` is asking for |
| Combine across all partitions | `min()` | **Sum** (counting-ways rule) |
| Overflow concern | None | **Yes — must take modulo**, since counts can explode combinatorially |

---

## The Key Takeaway

```
┌────────────────────────────────────────────────────────────────────────┐
│  Boolean Evaluation = MCM's exact partition skeleton, with             │
│  ONE structural addition: every sub-range needs BOTH its               │
│  true-count and false-count tracked together, because the              │
│  parent partition's formula for "how many ways to be true"             │
│  depends on BOTH children's true AND false counts.                     │
│                                                                        │
│  The recipe, in full:                                                  │
│  1. State = (i, j, isTrue) — a third boolean dimension                 │
│  2. Loop over every OPERATOR position (step of 2, not 1)               │
│  3. For each partition, solve FOUR values: leftTrue, leftFalse,        │
│     rightTrue, rightFalse — regardless of which one you were           │
│     originally asked for                                               │
│  4. Apply the correct truth-table formula for the operator             │
│     sitting at that partition (&, |, or ^) to combine them             │
│  5. SUM across every partition — this is a counting problem,           │
│     not an optimization problem, so no max/min here                    │
│  6. Take modulo throughout, since the counts can grow very large       │
│                                                                        │
│  Base case: a single character is trivially 1 way to be its own        │
│  value, 0 ways to be the opposite.                                     │
│                                                                        │
│  The meta-lesson for pattern recognition: whenever a Partition DP      │
│  problem's "value" at a range isn't a single number but a SET          │
│  of mutually-dependent outcomes (here: true vs. false), expand         │
│  the state to track all of them together — trying to derive one        │
│  from "total minus the other" doesn't work when you need both          │
│  numbers to correctly compute the PARENT range in the first place.     │
└────────────────────────────────────────────────────────────────────────┘
```

### My Code

```java
class Solution {
    private static int func(int i, int j, int isTrue, String s, int[][][] dp) {
        // Base
        // if (i > j) return 0;
        
        if (i == j) {
            if (isTrue == 1) {
                return s.charAt(i) == 'T' ? 1 : 0;
            } else {
                return s.charAt(i) == 'F' ? 1 : 0;
            }
        }
        
        if (dp[i][j][isTrue] != -1) return dp[i][j][isTrue];
        
        int ways = 0;
        
        for(int idx=i+1; idx<=j-1; idx = idx+2) {
            
            // how many ways left exp evaluates to true/false
            int leftTrue = func(i, idx-1, 1, s, dp);
            int leftFalse = func(i, idx-1, 0, s, dp);
            
            // how many ways right exp evaluates to true/false
            int rightTrue = func(idx+1, j, 1, s, dp);
            int rightFalse = func(idx+1, j, 0, s, dp);
            
            if (s.charAt(idx) == '&') {
                
                if (isTrue == 1) {
                    ways += leftTrue * rightTrue;
                } else {
                    ways += (leftTrue * rightFalse) 
                         + (leftFalse * rightTrue)
                         + (leftFalse * rightFalse);
                }
                
            } else if (s.charAt(idx) == '|') {
                 
                if (isTrue == 1) {
                    ways += (leftTrue * rightFalse) 
                         + (leftFalse * rightTrue)
                         + (leftTrue * rightTrue);
                } else {
                    ways += leftFalse * rightFalse;
                }
                 
            } else {
                
                if (isTrue == 1) {
                    ways += (leftTrue * rightFalse) 
                         + (leftFalse * rightTrue);
                } else {
                    ways += (leftTrue * rightTrue)
                         + (leftFalse * rightFalse);
                }
                
            }
        }
        
        dp[i][j][isTrue] = ways;
        return dp[i][j][isTrue];
    } 
    
    static int countWays(String s) {
        int n = s.length();
        
        int[][][] dp = new int[n][n][2];
        
        for(int[][] grid : dp) {
            for (int[] row : grid) {
                Arrays.fill(row, -1);
            }
        }
        
        return func(0, n-1, 1, s, dp);
        
    }
}
```