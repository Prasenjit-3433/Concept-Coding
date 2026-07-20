# LC 44. Wildcard Matching

Key Concept: ? and * matching — careful * transition
Solution: https://www.youtube.com/watch?v=ZmlQ3vgAOMo&ab_channel=takeUforward
Status: Pending

# LC 44. Wildcard Matching

*(Pattern 8, continuing from LC 115 — the pattern shifts from "count distinct ways" back to "can they match at all," but now with a wildcard that can consume ANY number of characters)*

---

# Stage 1: Identification

**Step 1 — Which topic?**

You're given a pattern `p` (which may contain lowercase letters, `?`, and `*`) and a text `s` (lowercase letters only). Determine whether `p` **matches** `s` completely.

```
'?'  matches exactly ONE character (any single character)
'*'  matches a sequence of ZERO OR MORE characters (any length, including empty)
```

```
p = "?ey"    s = "rey"    → '?' matches 'r', 'e' matches 'e', 'y' matches 'y' → TRUE

p = "ab*cd"  s = "abdefcd" → 'a'='a', 'b'='b', '*' swallows "def", 'c'='c', 'd'='d' → TRUE

p = "**"     s = "abcd"    → each '*' can match zero characters, or together
                              swallow the whole string → TRUE

p = "b?d"    s = "abcc"    → 'a' vs 'b' mismatch immediately → FALSE
```

**Step 2 — Which pattern?**

Two strings, walked with two pointers, character-by-character comparison — still **Pattern 8: String DP**. The `?` case is trivial (behaves exactly like a guaranteed match). The genuine new difficulty is the `*` — it doesn't correspond to a fixed number of characters, so matching against it isn't a single deterministic step, it's a **choice of how many characters to consume**.

**Step 3 — Which key concept?**

**True/False recursion where a `*` forces trying every possible consumption length — collapsed into two recursive calls instead of a loop.**

This continues directly from the recursion series' true/false template: build a recursive function `f(i, j)` that explores every possibility, and if **any single path** returns `true`, the overall answer is `true`.

```
Step 1: Express in terms of i (pattern index) and j (text index)
Step 2: Explore all possibilities — match, wildcard-match, or star-match
Step 3: Question asks "can it match" → return true if ANY path returns true
Step 4: Base cases return true or false directly
```

The star is where this problem earns its difficulty. Naively, you might think you need a `for` loop trying "star matches 0 chars," "star matches 1 char," "star matches 2 chars," and so on. The key insight is that **you never need that loop** — the same effect can be achieved with exactly two recursive branches, and those two branches, applied repeatedly, implicitly explore every possible consumption length.

---

# Stage 2: Intuition Building

### Why the Match and `?` Cases Are Trivial

If `p[i] == s[j]`, or `p[i] == '?'`, there is nothing to decide — this position is settled. Shrink both pointers and recurse on the smaller problem, exactly like every match case seen so far in this pattern.

```
f(i, j) = f(i-1, j-1)      when p[i] == s[j]  OR  p[i] == '?'
```

### Why  Needs Real Thought — Building It From a Concrete Example

Take `p = "ab*cd"`, `s = "abdefcd"`. Once `d` matches `d`, `c` matches `c`, we're comparing the `*` in `p` against the substring `"def"` in `s`. The `*` could be standing in for:

```
"" (nothing)     → then the very next pattern char must match 'f' directly
"f"              → then the next pattern char must match 'e'
"ef"             → then the next pattern char must match 'd'
"def"            → then the next pattern char must match whatever's before 'd' in s
```

We genuinely don't know in advance which length is correct — so, exactly as the transcript frames it, **you have to try them all**. The naive way to "try them all" is a loop: attempt length 0, length 1, length 2, ... But there's a much cleaner way to get the exact same effect using pure recursion, with no loop at all.

### The Two-Branch Trick — Collapsing the Loop Into Recursion

At any point where `p[i] == '*'`, there are only ever **two fundamentally different decisions** to make, and every possible consumption length falls out of applying these two decisions repeatedly:

```
Decision A — "This star consumes NOTHING, right now."
    → The star itself is discarded. Move past it in the pattern.
    → Nothing is consumed from the text.
    → f(i-1, j)

Decision B — "This star consumes ONE MORE character than it already has."
    → Stay on the SAME star (it might need to consume even more).
    → Consume one character from the text.
    → f(i, j-1)
```

```
f(i, j) = f(i-1, j)   OR   f(i, j-1)      when p[i] == '*'
```

Here's why this small pair of branches is equivalent to a full loop over "star matches 0, 1, 2, 3, ... characters":

```
Star wants to match "def" (3 characters) before giving up the position:

f(star, "def")
  → Decision B: consume 'f', STAY on star → f(star, "de")
       → Decision B: consume 'e', STAY on star → f(star, "d")
            → Decision B: consume 'd', STAY on star → f(star, "")
                 → Decision A: star gives up, consumed nothing further
                 → star has now swallowed "def" in total

Star wants to match "" (0 characters) instead:

f(star, "def")
  → Decision A: star gives up IMMEDIATELY, consumed nothing
  → move past star, "def" still needs to be matched by whatever comes next
```

Every possible consumption length — 0, 1, 2, 3, or more — is reachable by choosing Decision B enough times before eventually choosing Decision A. **Two branches, applied recursively, silently replace an entire loop.** This is the single most important realization in this problem.

### Base Cases — What Happens When One String Runs Out

**Both exhausted (`i < 0` and `j < 0`):** Every character on both sides has been successfully accounted for. A complete, valid match.

```
if i < 0 && j < 0:  return true
```

**Pattern exhausted, text still has characters (`i < 0`, `j >= 0`):** Nothing left in the pattern to match the remaining text. Impossible.

```
if i < 0 && j >= 0:  return false
```

**Text exhausted, pattern still has characters (`j < 0`, `i >= 0`):** This is the subtle one. The text is fully consumed, but the pattern isn't — is that automatically a failure? **Not necessarily.** If every single remaining character in the pattern is `*`, each of them can legitimately consume **zero** characters, and the whole remaining pattern collapses to nothing — a valid match against the now-empty text. But if even **one** remaining pattern character is a literal letter or a `?`, there's no text left for it to consume — failure.

```
if j < 0 && i >= 0:
    check every remaining character in p[0..i] — ALL must be '*'
    if any is not '*' → return false
    if all are '*'    → return true
```

### Visualizing on `p = "**"`, `s = "abcd"`

```
f(1, 3):  p[1]='*' → Decision A: f(0, 3)  OR  Decision B: f(1, 2)

Following Decision B repeatedly (second star eats the whole string):
f(1,3) → f(1,2) → f(1,1) → f(1,0) → f(1,-1)
                                       ↑
                              j<0, i>=0 → check p[0..1]:
                              both characters are '*' → return TRUE
```

Confirmed — the two-branch trick correctly lets a single `*` swallow an entire string.

### Overlapping Subproblems

The same `(i, j)` state gets reached from multiple parent branches — a star's "consume one more" chain from one path can land on the same `(i, j)` that a different combination of decisions also reaches. **Overlapping subproblems confirmed** → DP applies.

### DP Table Size

- `i`: pattern index, 0 to n-1 → **n values**
- `j`: text index, 0 to m-1 → **m values**

dp table: **n × m**

---

## The Recurrence, Summarized

```
f(i, j) = does p[0..i] match s[0..j]?

Base cases:
    i < 0 && j < 0            → true   (both exhausted, valid)
    i < 0 && j >= 0           → false  (pattern exhausted, text remains)
    j < 0 && i >= 0           → true only if p[0..i] is ALL '*', else false

Recurrence:
    if p[i] == s[j] OR p[i] == '?':
        f(i,j) = f(i-1, j-1)                    ← settled, shrink both
    elif p[i] == '*':
        f(i,j) = f(i-1, j)  OR  f(i, j-1)        ← give up star / consume one more
    else:
        f(i,j) = false                            ← no way to match, dead end
```

```
┌──────────────────────────────────────────────────────────────────┐
│  The idea that makes '*' tractable without a loop:                     │
│                                                                        │
│  "Star consumes nothing, move on"  (i-1, j)                            │
│      OR                                                                │
│  "Star consumes one more, stay put"  (i, j-1)                          │
│                                                                        │
│  Chaining "consume one more" k times before finally choosing           │
│  "consume nothing" is EXACTLY equivalent to the star matching          │
│  a run of k characters — no explicit loop over lengths needed.         │
└──────────────────────────────────────────────────────────────────┘
```

---

# Stage 3: Coding

## Approach 1 — Pure Recursion (Brute Force)

Two equivalent versions exist depending on whether you index with the original `-1`-based convention, or shift to one-based indexing immediately. Both are shown since the transition between them is worth seeing side by side.

**Version A — original (`-1`)-based indexing:**

```java
class Solution {
    private boolean func(int i, int j, String s1, String s2) {
        // base cases
        if (i < 0 && j < 0) return true;
        if (i < 0 && j >= 0) return false;
        if (j < 0 && i >= 0) {
            // text exhausted — pattern can only match if everything left is '*'
            for (int k = i; k >= 0; k--) {
                if (s1.charAt(k) != '*') return false;
            }
            return true;
        }

        // explore possibilities
        if (s1.charAt(i) == s2.charAt(j) || s1.charAt(i) == '?') {
            return func(i - 1, j - 1, s1, s2);
        } else if (s1.charAt(i) == '*') {
            return func(i - 1, j, s1, s2) || func(i, j - 1, s1, s2);
        } else {
            return false;
        }
    }

    public boolean isMatch(String s, String p) {
        int m = s.length(), n = p.length();
        return func(n - 1, m - 1, p, s);
    }
}
```

**Version B — shifted to one-based indexing (sets up tabulation cleanly):**

```java
class Solution {
    private boolean func(int i, int j, String s1, String s2) {
        // base cases
        if (i == 0 && j == 0) return true;
        if (i == 0 && j >= 1) return false;
        if (j == 0 && i >= 1) {
            for (int k = i; k >= 1; k--) {
                if (s1.charAt(k - 1) != '*') return false;
            }
            return true;
        }

        // explore possibilities
        if (s1.charAt(i - 1) == s2.charAt(j - 1) || s1.charAt(i - 1) == '?') {
            return func(i - 1, j - 1, s1, s2);
        } else if (s1.charAt(i - 1) == '*') {
            return func(i - 1, j, s1, s2) || func(i, j - 1, s1, s2);
        } else {
            return false;
        }
    }

    public boolean isMatch(String s, String p) {
        int m = s.length(), n = p.length();
        return func(n, m, p, s);
    }
}
```

**Time Complexity — Exponential:**
At every `*` position, the function branches into 2 recursive calls. Since a `*` can appear many times and each occurrence can trigger a long chain of "consume one more" calls, the recursion tree grows exponentially in the worst case.

**Space Complexity — O(n + m):**
No dp array. The recursion call stack — the deepest chain (repeatedly choosing "consume one more" on a star) can go up to `n + m` frames deep. Plus, each base-case hit on the `j < 0` branch does an extra O(n) scan for the "all stars" check.

---

## Approach 2 — Memoization (Top-Down DP)

```java
class Solution {
    private boolean func(int i, int j, String s1, String s2, int[][] dp) {
        // base cases
        if (i == 0 && j == 0) return true;
        if (i == 0 && j >= 1) return false;
        if (j == 0 && i >= 1) {
            for (int k = i; k >= 1; k--) {
                if (s1.charAt(k - 1) != '*') return false;
            }
            return true;
        }

        if (dp[i][j] != -1) return dp[i][j] == 1;

        // explore possibilities
        if (s1.charAt(i - 1) == s2.charAt(j - 1) || s1.charAt(i - 1) == '?') {
            dp[i][j] = func(i - 1, j - 1, s1, s2, dp) ? 1 : 0;
        } else if (s1.charAt(i - 1) == '*') {
            dp[i][j] = (func(i - 1, j, s1, s2, dp) || func(i, j - 1, s1, s2, dp)) ? 1 : 0;
        } else {
            dp[i][j] = 0;
        }

        return dp[i][j] == 1;
    }

    public boolean isMatch(String s, String p) {
        int m = s.length(), n = p.length();

        int[][] dp = new int[n + 1][m + 1];
        for (int[] row : dp) {
            Arrays.fill(row, -1);
        }

        return func(n, m, p, s, dp);
    }
}
```

**Why `int` instead of `boolean` for `dp`:** `-1` is needed as a "not yet computed" sentinel, and `boolean` has no third state — same reasoning used throughout this pattern.

**Time Complexity — O(n × m):**
Each unique `(i, j)` computed once. For each state, O(1) work — except the rare base-case hits, which cost O(n) for the "all stars" scan (this only happens along `j == 0`, not for every state).

**Space Complexity — O(n × m) + O(n + m):**
The dp array, plus the recursion stack.

---

## Approach 3 — Tabulation (Bottom-Up DP)

### Translating the Base Cases

```
i == 0 && j == 0         →  dp[0][0] = true
i == 0 && j >= 1         →  dp[0][j] = false, for all j >= 1
j == 0 && i >= 1         →  dp[i][0] = true only if p[0..i-1] is ALL '*'
```

```java
class Solution {
    public boolean isMatch(String s, String p) {
        int m = s.length(), n = p.length();

        boolean[][] dp = new boolean[n + 1][m + 1];

        // i == 0 && j == 0
        dp[0][0] = true;

        // i == 0 && j >= 1 — empty pattern can't match a non-empty text
        for (int j = 1; j <= m; j++) {
            dp[0][j] = false;
        }

        // j == 0 && i >= 1 — empty text, pattern must be all '*' to match
        for (int i = 1; i <= n; i++) {
            dp[i][0] = true;
            for (int k = i; k >= 1; k--) {
                if (p.charAt(k - 1) != '*') {
                    dp[i][0] = false;
                    break;
                }
            }
        }

        // fill the rest
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                if (p.charAt(i - 1) == s.charAt(j - 1) || p.charAt(i - 1) == '?') {
                    dp[i][j] = dp[i - 1][j - 1];
                } else if (p.charAt(i - 1) == '*') {
                    dp[i][j] = dp[i - 1][j] || dp[i][j - 1];
                } else {
                    dp[i][j] = false;
                }
            }
        }

        return dp[n][m];
    }
}
```

### Full Cell-by-Cell DP Table Trace

`s = "abdefcd"` (m = 7), `p = "ab*cd"` (n = 5)

```
                j=0    j=1(a)  j=2(b)  j=3(d)  j=4(e)  j=5(f)  j=6(c)  j=7(d)
i=0("")       [  T   ,   F   ,   F   ,   F   ,   F   ,   F   ,   F   ,   F   ]
i=1(a)        [  F   ,   T   ,   F   ,   F   ,   F   ,   F   ,   F   ,   F   ]
i=2(b)        [  F   ,   F   ,   T   ,   F   ,   F   ,   F   ,   F   ,   F   ]
i=3(*)        [  T   ,   F   ,   T   ,   T   ,   T   ,   T   ,   T   ,   T   ]
i=4(c)        [  F   ,   F   ,   F   ,   F   ,   F   ,   F   ,   T   ,   F   ]
i=5(d)        [  F   ,   F   ,   F   ,   F   ,   F   ,   F   ,   F   ,   T   ]
```

Walking through key cells (row `i` reads `p.charAt(i-1)`, column `j` reads `s.charAt(j-1)`):

- `dp[3][2]`: `p[2]='*'` → `dp[2][2] || dp[3][1] = T || F = T`
- `dp[3][3]`: `p[2]='*'` → `dp[2][3] || dp[3][2] = F || T = T` (star keeps swallowing)
- `dp[4][6]`: `p[3]='c'`, `s[5]='c'` → match → `dp[3][5] = T`
- `dp[5][7]`: `p[4]='d'`, `s[6]='d'` → match → `dp[4][6] = T`

**Answer = `dp[5][7] = true`** ✓ — matches Striver's hand-derived result.

**Time Complexity — O(n × m + n²)** in the worst case (the base-case row scan costs up to O(n) per row, over n rows), but this simplifies in practice to **O(n × m)** since the main double loop dominates for typical inputs.

**Space Complexity — O(n × m):**
Only the dp array. No recursion stack.

---

## Approach 4 — Space Optimization (The Final Form)

```
dp[i][j] depends on: dp[i-1][j-1]   (diagonal)
                       dp[i-1][j]     (same column, row above)
                       dp[i][j-1]     (same row, already computed this pass)
```

Row `i` only needs row `i-1` plus values already filled in the current row — same shape as Edit Distance. Two rolling 1D arrays suffice.

```java
class Solution {
    public boolean isMatch(String s, String p) {
        int m = s.length(), n = p.length();

        boolean[] lastRow = new boolean[m + 1];

        // i == 0 && j == 0
        lastRow[0] = true;

        // i == 0 && j >= 1
        for (int j = 1; j <= m; j++) {
            lastRow[j] = false;
        }

        for (int i = 1; i <= n; i++) {
            boolean[] currRow = new boolean[m + 1];

            // j == 0 && i >= 1 — pattern so far must be all '*'
            currRow[0] = true;
            for (int k = i; k >= 1; k--) {
                if (p.charAt(k - 1) != '*') {
                    currRow[0] = false;
                    break;
                }
            }

            for (int j = 1; j <= m; j++) {
                if (p.charAt(i - 1) == s.charAt(j - 1) || p.charAt(i - 1) == '?') {
                    currRow[j] = lastRow[j - 1];
                } else if (p.charAt(i - 1) == '*') {
                    currRow[j] = lastRow[j] || currRow[j - 1];
                } else {
                    currRow[j] = false;
                }
            }

            lastRow = currRow;
        }

        return lastRow[m];
    }
}
```

**Time Complexity — O(n × m)** (dominant term; base-case scans add lower-order cost).

**Space Complexity — O(m):**
Two arrays of size `m + 1` — `lastRow` and `currRow`. Down from `O(n × m)`.

---

## A Note on the Base-Case Loop's Cost

The `for (k = i; k >= 1; k--)` scan inside the `dp[i][0]` base case technically makes the worst-case time complexity `O(n × m + n²)` rather than a clean `O(n × m)` — each row's base case can cost up to O(n). This matches Striver's own approach exactly and is what's presented here for fidelity to the lecture. **A tighter version exists:** since `dp[i][0]` is true only when `p[0..i-1]` is all stars, you can compute it incrementally — `dp[i][0] = dp[i-1][0] && (p.charAt(i-1) == '*')` — collapsing the whole row-0 computation to O(n) total instead of O(n) *per row*. This is a valid follow-up optimization but isn't required to pass, and isn't what the lecture teaches, so it's flagged here only as a footnote.

---

## Final Comparison

| Approach | Time | Space | Notes |
| --- | --- | --- | --- |
| Pure Recursion | Exponential | O(n+m) stack | Never use |
| Memoization | O(n × m) | O(n×m) + O(n+m) stack | Good interview starting point |
| Tabulation | O(n × m) | O(n × m) | Better — eliminates stack |
| Space Optimization | O(n × m) | **O(m)** | Best — submit this |

---

## How LC 44 Differs From LC 115

| Property | LC 115 (Distinct Subsequences) | LC 44 (Wildcard Matching) |
| --- | --- | --- |
| Return type | Count (integer) | **Boolean (true/false)** |
| Combine sub-results | ADD (`+`) | **OR (`||`)** |
| Special characters | None | **`?` (single char), `*` (any-length run)** |
| Base case at `j < 0` / `j == 0` | Returns `1` unconditionally | **Depends on whether remaining pattern is all `*`** |
| Core new idea | Match = fork into "use it" / "skip it" | **`*` = fork into "consume nothing" / "consume one more"** |

---

## The Key Takeaway

```
┌───────────────────────────────────────────────────────────────┐
│  LC 44's defining insight: a wildcard that can match "zero or      │
│  more of anything" does NOT require a loop trying every length.    │
│  Two recursive branches suffice:                                   │
│                                                                    │
│      f(i,j) = f(i-1, j)   OR   f(i, j-1)                           │
│                  ↑                  ↑                              │
│           star gives up      star consumes one more,               │
│           right now          stays on itself                       │
│                                                                    │
│  Chaining "consume one more" k times before "giving up" is         │
│  exactly equivalent to the star matching a run of k characters.    │
│  This same two-branch trick generalizes to ANY "unbounded          │
│  repetition" construct — it will reappear in LC 10 (Regular        │
│  Expression Matching) for the `*` quantifier there too.            │
│                                                                    │
│  The trickiest base case: when the TEXT is exhausted but the       │
│  PATTERN still has characters left, it is not automatically a      │
│  failure — check whether every remaining pattern character is      │
│  '*' (each can consume zero characters and vanish).                │
│                                                                    │
│  True/false counting rule (from the recursion series):             │
│  combine sub-results with OR, not sum or max — any single          │
│  successful path is enough to declare the whole match valid.       │
└───────────────────────────────────────────────────────────────┘
```