# LC 115. Distinct Subsequences | String Matching (Pattern)

Key Concept: Count ways t appears as subsequence in s
Solution: https://www.youtube.com/watch?v=nVG7eTiD2bY&ab_channel=takeUforward
Status: Done

# LC 115. Distinct Subsequences

*(First problem of Pattern 8: String DP — the pattern shifts from LCS-style "longest match" to "count distinct ways one string appears as a subsequence of another")*

---

# Stage 1: Identification

## **Step 1 — Which topic?**

You're given two strings, `s` and `t`. You need to count **how many distinct subsequences of `s` equal `t`**.

```
s = "babgbag"
t = "bag"
```

Some ways `"bag"` appears as a subsequence inside `"babgbag"` (picking characters in order, not necessarily contiguous):

```
b(0) a(1) g(3)   → "bag"
b(0) a(1) g(6)   → "bag"
b(0) a(4) g(6)   → "bag"
b(3) a(4) g(6)   → "bag"
b(2) a(4) g(6)   → "bag"
```

Five distinct ways. Answer = **5**.

## **Step 2 — Which pattern?**

We're comparing two strings, character by character, walking through both with a pair of indices. That structural shape — two strings, two pointers, position-by-position comparison — is still the broad DP-on-strings family. But there's a genuine shift happening here compared to Pattern 7 (LCS):

> Every problem in Pattern 7 asked "what's the longest/shortest shared structure between two strings?" This problem asks something different: **"in how many distinct ways does one string's characters appear, in order, inside the other?"** This is **string matching** — you're not measuring overlap, you're counting alignments.
> 

This is **Pattern 8: String DP**, and this is the pattern's foundational "counting via matching" template — the same skeleton that Word Break, Interleaving String, and the wildcard/regex matching problems will all build on.

## **Step 3 — Which key concept?**

**Count ways → recursion returns 1 or 0 at the base case, and sums (not maxes) across all valid choices.**

This is a direct callback to the Recursion series (lecture 6–7 territory, as Striver references): whenever a problem says **"count the number of ways,"** the recursive function's job is to explore every possible path, and at the base case, return exactly `1` if that path represents a *valid* completed count, or `0` if it represents a failed/incomplete one. Every recursive call then **adds up** — never takes a max, never takes a min — the results of its sub-calls.

```
Step 1: Express in terms of two indices — i for s, j for t
Step 2: Explore all possibilities at (i, j)
Step 3: Question says "count distinct ways" → SUM all valid results
Step 4: Base case returns 1 or 0
```

---

# Stage 2: Intuition Building

## Why Try Every Alignment? — The Core Difficulty

The instinct might be: "just scan `s` left to right and greedily match `t`'s characters as you go." But that only finds **one** occurrence, and stops. The problem wants **all distinct occurrences counted** — and different occurrences use *different* characters from `s` for the same position in `t`.

Look again at `s = "babgbag"`, `t = "bag"`. When you're trying to match the `'g'` at the end of `t`, there are **two** `'g'`s available in `s` (at index 3 and index 6) — either one is a legitimate choice, and each choice branches into a separate valid occurrence. You cannot know in advance which `'g'` (or which `'b'`, or which `'a'`) leads to more total matches downstream — so, exactly as Striver frames it, **you have to try every possible way of matching, and add up the results.** That "try every way and sum" is the signature of recursion applied to a counting problem.

### Step 1 — Represent in Terms of (i, j)

Define:

```
f(i, j) = number of distinct subsequences of s[0..i] that equal t[0..j]
```

The answer we want is `f(n-1, m-1)`, where `n = s.length()`, `m = t.length()`.

### Step 2 — Explore All Possibilities at (i, j)

Look at the **last characters** of the current prefixes: `s[i]` and `t[j]`.

**Case 1 — Characters match (`s[i] == t[j]`):**

This is where the real branching happens. If the current character of `s` matches the current character of `t`, you have **two genuinely different options** — and both are valid, so both must be explored and their results **added together**:

```
Option A — USE this s[i] as the match for t[j].
    Both strings shrink: recurse on f(i-1, j-1).
    (This is exactly "b(0) a(1) g(3)" using this specific 'g'.)

Option B — DON'T use this s[i] for t[j], even though it matches.
    Keep looking for a DIFFERENT occurrence of t[j] earlier in s.
    Only s shrinks: recurse on f(i-1, j).
    (This is exactly "keep searching — maybe there's another 'g'
     somewhere before this one that also works.")
```

```
f(i, j) = f(i-1, j-1)  +  f(i-1, j)        [only when s[i] == t[j]]
```

Both options must be added — not compared with `max` — because **each represents a genuinely distinct occurrence of `t` inside `s`**, and the problem wants the total count across all of them.

**Case 2 — Characters do NOT match (`s[i] != t[j]`):**

There is no legitimate way to use `s[i]` as a match for `t[j]` here. The only option is to keep searching for `t[j]` earlier in `s` — shrink `s` alone.

```
f(i, j) = f(i-1, j)                         [when s[i] != t[j]]
```

### Step 3 — Base Cases: When Does Recursion Return 1, and When 0?

Since this is a **counting** problem, the base case must return `1` (a valid, complete match found) or `0` (an incomplete/failed match). Ask: *what does it mean for the pointers to run out?*

**Base Case 1 — `t` is fully matched (`j < 0`):**

If `j` has gone negative, every character of `t` has already been successfully matched somewhere in `s`. This is a **complete, valid occurrence** — count it.

```
if j < 0:  return 1
```

**Base Case 2 — `s` runs out first, while `t` still has characters left (`i < 0`, but `j >= 0`):**

If `s` is exhausted but `t` still has unmatched characters remaining, this path **failed** — there's nothing left in `s` to match the rest of `t`.

```
if i < 0:  return 0
```

**Critical ordering note (directly from the transcript's reasoning):** always check `j < 0` *before* checking `i < 0`. If both `i` and `j` happen to hit `-1` simultaneously, that means `t` was matched exactly using up all of `s` — a valid, complete match. Checking `j < 0` first correctly returns `1` in that case; checking `i < 0` first would incorrectly return `0`.

## Visualizing the Recursion — Confirming the Branching

Using `s = "bag"`, a small slice, and imagining `t = "ba"`:

```
f(2, 1):  s[2]='g', t[1]='a' → NO MATCH
          f(2,1) = f(1,1)

f(1,1):   s[1]='a', t[1]='a' → MATCH
          f(1,1) = f(0,0) + f(0,1)
                     ↑           ↑
              use this 'a'   skip this 'a', look for
                              another 'a' earlier in s

f(0,0):   s[0]='b', t[0]='b' → MATCH
          f(0,0) = f(-1,-1) + f(-1,0)
                       ↑            ↑
                  j<0 → return 1   j<0 → return 1
          f(0,0) = 1 + 1 = 2  ...

(actually f(-1,0): j=0, not <0 yet on this branch — walk carefully,
 but the shape shown is exactly right: every match spawns TWO
 branches that both get added)
```

The point isn't to trace every number by hand here — it's to see the **structural signature**: every match spawns two recursive calls whose results get **added**, and this branching means the same `(i, j)` states get revisited from many different paths as strings grow. **Overlapping subproblems confirmed** → DP applies.

## DP Table Size

Two parameters:

- `i`: 0 to n-1 → **n values**
- `j`: 0 to m-1 → **m values**

dp table: **n × m**

---

## The Recurrence, Summarized

```
f(i, j) = number of distinct subsequences of s[0..i] equal to t[0..j]

Base cases:
    if j < 0:  return 1     ← t fully matched — valid occurrence, count it
    if i < 0:  return 0     ← s exhausted, t still has leftover — invalid

Recurrence:
    if s[i] == t[j]:
        f(i, j) = f(i-1, j-1) + f(i-1, j)      ← use this match, OR skip it
    else:
        f(i, j) = f(i-1, j)                     ← forced to skip, no choice
```

```
┌────────────────────────────────────────────────────────────────────────┐
│  The one idea that makes this problem click:                           │
│                                                                        │
│  A "match" between s[i] and t[j] is NOT automatically consumed.        │
│  You have the FREEDOM to use it (advance both pointers) or             │
│  deliberately ignore it and keep hunting for a different,              │
│  earlier occurrence of the same character in s.                        │
│                                                                        │
│  Both choices are legitimate, independent occurrences —                │
│  so their counts get ADDED, never compared with max/min.               │
│  This "use it or search elsewhere" freedom at every match              │
│  is exactly what produces multiple distinct subsequences.              │
└────────────────────────────────────────────────────────────────────────┘
```

---

# Stage 3: Coding

## Approach 1 — Pure Recursion (Brute Force)

Direct translation of the recurrence from Stage 2.

```java
class Solution {
    public int numDistinct(String s, String t) {
        int n = s.length();
        int m = t.length();
        return (int) solve(n - 1, m - 1, s, t);
    }

    private long solve(int i, int j, String s, String t) {
        // Base case 1: t fully matched — valid occurrence found
        // WHY check this BEFORE i < 0: if both i and j hit -1
        // simultaneously, that means t was matched exactly using up
        // all of s — still a valid match, must return 1
        if (j < 0) return 1;

        // Base case 2: s exhausted but t still has leftover characters
        // WHY 0: nothing left in s to match the rest of t — this path failed
        if (i < 0) return 0;

        if (s.charAt(i) == t.charAt(j)) {
            // Option A: use this s[i] as the match for t[j] — both shrink
            long useIt = solve(i - 1, j - 1, s, t);

            // Option B: don't use it — keep searching for a DIFFERENT
            // occurrence of t[j] earlier in s — only s shrinks
            long skipIt = solve(i - 1, j, s, t);

            // ADD — both represent genuinely distinct occurrences
            return useIt + skipIt;
        } else {
            // No match possible here — forced to keep searching in s
            return solve(i - 1, j, s, t);
        }
    }
}
```

**Time Complexity — O(2^n):**
At every character-match state, the function branches into 2 recursive calls; on a mismatch, only 1. In the worst case (many repeated characters, lots of matches), the recursion tree grows exponentially — roughly `O(2^n)`, since the branching happens along `s`'s length. For `n` in the hundreds (an ordinary constraint), this is completely impractical.

**Space Complexity — O(n + m):**
No dp array is allocated. The recursion call stack holds one frame per level — the deepest chain shrinks `i` (and sometimes `j`) one at a time, going at most `n + m` frames deep before hitting a base case. Stack uses O(n+m) space.

---

## Approach 2 — Memoization (Top-Down DP)

The same state `f(i, j)` gets reached from multiple branches — confirmed in the recursion-tree walkthrough in Stage 2. Store each result the first time it's computed.

```java
class Solution {
    public int numDistinct(String s, String t) {
        int n = s.length();
        int m = t.length();

        // dp[i][j] = -1 means not yet computed
        int[][] dp = new int[n][m];
        for (int[] row : dp) {
            Arrays.fill(row, -1);
        }
        return (int) solve(n - 1, m - 1, s, t, dp);
    }

    private long solve(int i, int j, String s, String t, int[][] dp) {
        // Base cases — identical to pure recursion
        if (j < 0) return 1;
        if (i < 0) return 0;

        // Step 1: Already computed? Return stored value instantly
        // WHY: f(i,j) = distinct subsequence count for s[0..i] vs t[0..j]
        //      This never changes no matter which branch reached this state
        if (dp[i][j] != -1) return dp[i][j];

        long result;
        if (s.charAt(i) == t.charAt(j)) {
            long useIt = solve(i - 1, j - 1, s, t, dp);
            long skipIt = solve(i - 1, j, s, t, dp);
            result = useIt + skipIt;
        } else {
            result = solve(i - 1, j, s, t, dp);
        }

        // Step 2: Store before returning
        dp[i][j] = (int) result;
        return result;
    }
}
```

**Why this gets Time Limit Exceeded on LeetCode:** even though every unique `(i, j)` state is now computed exactly once, the *total number* of states — `n × m` — combined with the recursion overhead (function call stack, repeated lookups) is still too slow for LeetCode's larger hidden test cases (strings up to length 1000). This mirrors exactly what happened in the transcript — memoization alone doesn't clear the judge; **tabulation is needed** to remove the call-stack overhead entirely.

**Time Complexity — O(n × m):**
Each unique `(i, j)` pair computed exactly once. For each, O(1) work — one comparison, at most one addition. Total: **O(n × m)**.

**Space Complexity — O(n × m) + O(n + m):**
The dp array, plus the recursion call stack going up to `n + m` levels deep. Total: **O(n × m) + O(n + m)**.

---

## Approach 3 — Tabulation (Bottom-Up DP)

### The One-Based Index Shift

Same shift used throughout Pattern 7 (LCS) — the recursion's base cases hit negative indices (`i < 0`, `j < 0`), which Java arrays cannot represent. Shift everything by `+1`, loop directly over the shifted index, and subtract 1 only when touching the actual strings (`s.charAt(i-1)`, `t.charAt(j-1)`).

```
Original i: -1  0  1  ...  n-1        Original j: -1  0  1  ...  m-1
Shifted i:   0  1  2  ...  n          Shifted j:   0  1  2  ...  m
```

### Translating the Base Cases

```
Recursion:  if j < 0:  return 1      →  Tabulation:  dp[i][0] = 1   for ALL i
Recursion:  if i < 0:  return 0      →  Tabulation:  dp[0][j] = 0   for j >= 1
```

Why `dp[i][0] = 1` for **every** `i`, regardless of what `i` is? Because under the shift, `dp[i][0]` represents `f(i-1, -1)` — and `f(anything, -1) = 1` always (the base case doesn't care what `i` was, only that `j` reached `-1`). So the entire **first column** is `1`.

Why `dp[0][j] = 0` for `j >= 1`? Because `dp[0][j]` represents `f(-1, j-1)` — `s` is empty but `t` still has characters left (`j-1 >= 0`) — a failed match. Note `dp[0][0]` is **not** set by this rule; it falls under the first-column rule above and is `1` (both strings simultaneously empty — a valid, trivial match).

```java
class Solution {
    public int numDistinct(String s, String t) {
        int n = s.length();
        int m = t.length();

        // dp[i][j] = distinct subsequence count, shifted indexing
        // Use double to avoid overflow issues on large LeetCode test cases
        // (the transcript hits this exact issue — counts can exceed int range
        // during intermediate additions even though the final answer fits)
        double[][] dp = new double[n + 1][m + 1];

        // Base case: for EVERY row i, column 0 is 1
        // WHY: dp[i][0] represents f(i-1, -1) — t is fully matched
        //      regardless of how much of s remains
        for (int i = 0; i <= n; i++) {
            dp[i][0] = 1;
        }

        // Base case: for row 0, columns j >= 1 are 0
        // WHY: dp[0][j] represents f(-1, j-1) — s is empty but t still
        //      has leftover characters — no valid match possible
        // (dp[0][0] is already set to 1 by the loop above — do not overwrite)
        for (int j = 1; j <= m; j++) {
            dp[0][j] = 0;
        }

        // Fill the rest — loop directly over shifted indices
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {

                if (s.charAt(i - 1) == t.charAt(j - 1)) {
                    // Match: use it (diagonal) OR skip it (same column, row above)
                    dp[i][j] = dp[i - 1][j - 1] + dp[i - 1][j];
                } else {
                    // No match: forced to skip, only carry forward
                    dp[i][j] = dp[i - 1][j];
                }
            }
        }

        // Cast back to int at the very end — final answer always fits in int
        return (int) dp[n][m];
    }
}
```

### Full Cell-by-Cell DP Table Trace

`s = "babgbag"` (n = 7), `t = "bag"` (m = 3)

```
                 j=0   j=1(b)  j=2(a)  j=3(g)
i=0 ("")      [   1  ,   0   ,   0   ,   0   ]
i=1(b)        [   1  ,   1   ,   0   ,   0   ]
i=2(a)        [   1  ,   1   ,   1   ,   0   ]
i=3(b)        [   1  ,   2   ,   1   ,   0   ]
i=4(g)        [   1  ,   2   ,   1   ,   1   ]
i=5(b)        [   1  ,   3   ,   1   ,   1   ]
i=6(a)        [   1  ,   3   ,   4   ,   1   ]
i=7(g)        [   1  ,   3   ,   4   ,   5   ]
```

Walking through key cells (row `i` reads `s.charAt(i-1)`, column `j` reads `t.charAt(j-1)`):

- `dp[1][1]`: `s[0]='b'`, `t[0]='b'` → match → `dp[0][0] + dp[0][1] = 1 + 0 = 1`
- `dp[3][1]`: `s[2]='b'`, `t[0]='b'` → match → `dp[2][0] + dp[2][1] = 1 + 1 = 2` (two `'b'`s seen so far, each a valid start)
- `dp[6][2]`: `s[5]='a'`, `t[1]='a'` → match → `dp[5][1] + dp[5][2] = 3 + 1 = 4`
- `dp[7][3]`: `s[6]='g'`, `t[2]='g'` → match → `dp[6][2] + dp[6][3] = 4 + 1 = 5`

**Answer = `dp[7][3] = 5`** ✓ — matches the hand-counted 5 occurrences exactly.

**Time Complexity — O(n × m):**
Two nested loops, `n` and `m` respectively. Each cell O(1) work. Total: **O(n × m)**.

**Space Complexity — O(n × m):**
Only the dp array of size `(n+1) × (m+1)`. No recursion stack.

---

## Approach 4 — Space Optimization (The Final Form)

### Two-Row Version First

```
dp[i][j] depends on: dp[i-1][j-1]   (prev row, some left column)
                       dp[i-1][j]     (prev row, same column)
```

Row `i` only ever needs row `i-1`. Two 1D arrays — `prev`, `curr` — suffice.

### Collapsing to a Single Array — Why It Works Here (Unlike 0/1 Knapsack's Directional Rule)

Look closely at what each cell needs:

```
curr[j] = prev[j-1] + prev[j]      (on a match)
curr[j] = prev[j]                  (on a mismatch)
```

Both terms needed — `prev[j-1]` and `prev[j]` — come from the **row above**, never from the current row being built. This is different from 0/1 Knapsack, where the "take" case needed a value **from the row currently being filled** (which forced the right-to-left fill rule to avoid double-counting). Here, there's no such same-row dependency at all — so we can simply overwrite one rolling array **in place**, and the fill direction genuinely doesn't matter for correctness. The transcript settles on filling **right to left** anyway (as a matter of habit/safety), but the underlying reason it's safe is: nothing on the current row ever gets read while computing the current row.

```java
class Solution {
    public int numDistinct(String s, String t) {
        int n = s.length();
        int m = t.length();

        // prev[j] doubles as the "previous row", then gets overwritten
        // in place to become the "current row" — safe because the
        // recurrence never reads a same-row value, only row-above values
        double[] prev = new double[m + 1];

        // Base case: column 0 is always 1 (t fully matched regardless of s)
        prev[0] = 1;

        // Base case: row 0, columns >= 1 are 0 (s empty, t has leftover)
        // (prev starts as row 0 here — already all zeros by default,
        //  except index 0 which we just set to 1)

        for (int i = 1; i <= n; i++) {
            // Fill right to left — matches the transcript's approach
            // WHY it's safe in EITHER direction: every value read
            // (prev[j] and prev[j-1]) comes from the row ABOVE,
            // never from a value already overwritten in this same pass
            for (int j = m; j >= 1; j--) {
                if (s.charAt(i - 1) == t.charAt(j - 1)) {
                    prev[j] = prev[j - 1] + prev[j];
                }
                // else: prev[j] stays exactly as it was (= dp[i-1][j]),
                // which is precisely the value we want for a mismatch —
                // no assignment needed at all
            }
            // prev[0] never changes — it's always 1, correctly preserved
        }

        return (int) prev[m];
    }
}
```

**Time Complexity — O(n × m):**
Same two nested loops, same iteration count. No change.

**Space Complexity — O(m):**
A single array of size `m + 1`. This is the tightest possible space bound for this problem.

---

## The Overflow Subtlety (Directly From the Transcript)

Using `int` (or even `long`, in some edge cases the transcript encountered) for the dp values can trigger a runtime/overflow error on LeetCode's larger hidden test cases — intermediate sums of subsequence counts can grow surprisingly large during computation even though the problem guarantees the **final** answer fits within a 32-bit integer. The transcript's fix — and the one used above — is to use `double` for all intermediate storage (which has enough range headroom to absorb the large intermediate sums safely) and cast back to `int` only at the very end, once the final answer is guaranteed to be back within range.

---

## Final Comparison

| Approach | Time | Space | Notes |
| --- | --- | --- | --- |
| Pure Recursion | O(2^n) | O(n+m) stack | Exponential — never use |
| Memoization | O(n × m) | O(n×m) + O(n+m) stack | Passes conceptually, TLEs on LeetCode |
| Tabulation | O(n × m) | O(n × m) | Clears TLE — eliminates stack |
| Space Optimization | O(n × m) | **O(m)** | Best — submit this |

---

## The Key Takeaway

```
┌─────────────────────────────────────────────────────────────────────┐
│  LC 115 introduces Pattern 8's foundational template: STRING        │
│  MATCHING with counting, as opposed to Pattern 7's "measure the     │
│  overlap" (LCS-style) template.                                     │
│                                                                     │
│  The recurrence:                                                    │
│                                                                     │
│  if s[i] == t[j]:                                                   │
│      f(i,j) = f(i-1, j-1)  +  f(i-1, j)                             │
│                    ↑               ↑                                │
│               USE this match   SKIP it, search for a                │
│                                 DIFFERENT occurrence                │
│  else:                                                              │
│      f(i,j) = f(i-1, j)          ← forced to skip, no choice        │
│                                                                     │
│  Base cases (counting rule — return 1 or 0, never anything else):   │
│      j < 0  → return 1   (t fully matched — count this occurrence)  │
│      i < 0  → return 0   (s exhausted, t incomplete — invalid)      │
│      CHECK j < 0 BEFORE i < 0 — simultaneous exhaustion is valid.   │
│                                                                     │
│  The critical insight that makes this problem click: a character    │
│  match is a FORK, not an automatic consumption. Both "use it" and   │
│  "search elsewhere for another occurrence" are legitimate,          │
│  independently-countable paths — so their results ADD.              │
│                                                                     │
│  Space optimization note — this problem does NOT need a             │
│  directional fill rule like 0/1 Knapsack: the recurrence only       │
│  ever reads values from the ROW ABOVE, never from the row           │
│  currently being built, so a single rolling array can be updated    │
│  in either direction safely.                                        │
│                                                                     │
│  Practical implementation note: watch for overflow on large         │
│  inputs — use double for intermediate dp values, cast to int        │
│  only at the final return.                                          │
└─────────────────────────────────────────────────────────────────────┘
```
