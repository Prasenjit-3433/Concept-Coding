# GFG: Print LCS

*(Continuing directly from LC 1143 — same two strings Striver used: `s1 = "abcde"`, `s2 = "bdgek"`)*

---

# Stage 1: Identification

**Step 1 — Which topic?**

You already solved LC 1143 — you know *how long* the longest common subsequence between two strings is. Now the question changes: **don't just tell me the length, show me the actual subsequence.**

**Step 2 — Which pattern?**

Still **Pattern 7: LCS**. Two strings, two indices, match/no-match recurrence — nothing about the *pattern* changes.

**Step 3 — Which key concept?**

This is where it gets interesting, and it's exactly the caveat you ran into. There are two ways to think about "printing" a DP answer:

```
Idea A: Make the DP itself return the STRING, not the length.
        (build the answer bottom-up, string by string, at every cell)

Idea B: Make the DP return the LENGTH only (exactly like LC 1143).
        Then, as a completely separate second step, BACKTRACK
        through the finished length-table to reconstruct the string.
```

Your instinct — Idea A — is the natural one. It's literally what every earlier pattern taught you: recursion → memoization → tabulation → space optimization, applied directly to the thing you want to return. So let's actually build Idea A properly first, the same rigorous way we built LC 1143. Then we'll see exactly where and why it breaks down — and that failure is the whole reason Idea B (Striver's approach) exists.

---

# Stage 2: Building Idea A — "Let the DP Return the String Directly"

### The Setup

This is a direct copy of the LC 1143 recurrence. The only change: instead of returning a **length** (`1 + f(i-1,j-1)` vs `max(...)`), we return the **string itself**.

```
f(i, j) = the longest common subsequence STRING of s1[0..i] and s2[0..j]

if s1[i] == s2[j]:
    f(i, j) = f(i-1, j-1) + s1[i]              (append the matched char)
else:
    f(i, j) = whichever of f(i-1, j) / f(i, j-1) is LONGER (as a string)

Base case: f(i, j) = ""  when either string is exhausted
```

This is *exactly* the same shape as LC 1143's recurrence — `max()` on integers is replaced by "pick the longer string." On paper, this looks completely correct. And it is correct, in the sense that it produces *a* valid maximum-length common subsequence. Let's build all four approaches on it, precisely the way we did for LC 1143.

---

## Approach 1 — Pure Recursion

```java
private static String func(int i, int j, String s1, String s2) {
    // Base case: either string exhausted — nothing left to match
    if (i < 0 || j < 0) return "";

    // Case 1: characters match — extend the LCS string by this character
    if (s1.charAt(i) == s2.charAt(j)) {
        return func(i - 1, j - 1, s1, s2) + s1.charAt(i);
    }

    // Case 2: characters don't match — try dropping either side, keep the LONGER string
    String option1 = func(i - 1, j, s1, s2);
    String option2 = func(i, j - 1, s1, s2);

    return option1.length() > option2.length() ? option1 : option2;
}
```

This mirrors LC 1143's pure recursion exactly. Same base case, same two branches. The only structural difference: `Math.max(int, int)` has become `option1.length() > option2.length() ? option1 : option2`.

**Time Complexity — O(2^(n+m)):** identical branching pattern to LC 1143. But there is now a **hidden extra cost per call**: string concatenation. `func(...) + s1.charAt(i)` creates a *brand new string object* every time it's called — copying every character of the shorter string so far. So each call node in that exponential tree doesn't do O(1) work anymore — it does O(k) work, where k is the length of the string being built. This inflates an already-exponential cost even further, but exponential is already unusable, so this matters more once we memoize.

---

## Approach 2 — Memoization

```java
private static String func(int i, int j, String s1, String s2, String[][] dp) {
    if (i < 0 || j < 0) return "";

    if (dp[i][j] != null) return dp[i][j];

    String result;
    if (s1.charAt(i) == s2.charAt(j)) {
        result = func(i - 1, j - 1, s1, s2, dp) + s1.charAt(i);
    } else {
        String option1 = func(i - 1, j, s1, s2, dp);
        String option2 = func(i, j - 1, s1, s2, dp);
        result = option1.length() > option2.length() ? option1 : option2;
    }

    return dp[i][j] = result;
}
```

Exactly LC 1143's memoization skeleton — a `dp[i][j]` cache, lookup before compute, store after compute. This part is legitimate: each of the `n × m` unique `(i, j)` states is now computed exactly once.

**But look closely at what is being cached.** `dp[i][j]` in LC 1143 stored a single `int` — 4 bytes, O(1) to store, O(1) to read. Here, `dp[i][j]` stores a **String object**, and that string can be up to `min(n, m)` characters long. So:

**Time Complexity — O(n · m · min(n, m)):** there are `n × m` unique states, which is the same count as before. But computing *each* state now involves a string concatenation whose cost is proportional to the length of the string being built — which, in the worst case (near the end of the strings, where the LCS built so far is long), can be up to `min(n, m)` characters. So the true cost is `n × m` states **times** up to `min(n, m)` work per state.

**Space Complexity — O(n · m · min(n, m)):** this is the real killer. LC 1143's `dp` array held `n × m` integers. This `dp` array holds `n × m` **strings**, and many of those strings are close to `min(n, m)` characters long. For strings of length ~1000 each (a completely ordinary constraint), this is roughly `1000 × 1000 × 1000` = **one billion characters** of memory just sitting in the cache. That is not a small constant-factor slowdown — it's an asymptotic blow-up that LC 1143's version never had.

---

## Approach 3 — Tabulation

```java
String[][] dp = new String[n + 1][m + 1];

for (int j = 0; j <= m; j++) dp[0][j] = "";
for (int i = 0; i <= n; i++) dp[i][0] = "";

for (int i = 1; i <= n; i++) {
    for (int j = 1; j <= m; j++) {
        if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
            dp[i][j] = dp[i - 1][j - 1] + s1.charAt(i - 1);
        } else {
            String option1 = dp[i - 1][j];
            String option2 = dp[i][j - 1];
            dp[i][j] = option1.length() > option2.length() ? option1 : option2;
        }
    }
}

return dp[n][m];
```

Same conversion rules as always — Model B index-shifting, base case is the first row/column, loops run forward. Structurally this is a perfect, faithful translation of LC 1143's tabulation. The bug isn't in the *logic* anywhere — it's entirely in *what data type sits inside every cell*.

**Time & Space — O(n · m · min(n, m))**, for the exact same reason as memoization: `n × m` cells, each potentially holding and rebuilding a string of length up to `min(n, m)`.

---

## Approach 4 — Space Optimization

```java
String[] prev = new String[m + 1];
for (int j = 0; j <= m; j++) prev[j] = "";

for (int i = 1; i <= n; i++) {
    String[] curr = new String[m + 1];
    curr[0] = "";

    for (int j = 1; j <= m; j++) {
        if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
            curr[j] = prev[j - 1] + s1.charAt(i - 1);
        } else {
            String option1 = prev[j];
            String option2 = curr[j - 1];
            curr[j] = option1.length() > option2.length() ? option1 : option2;
        }
    }
    prev = curr;
}
return prev[m];
```

This correctly reduces the *number of arrays* from `n+1` rows down to 2 rows — following exactly the same "row `i` only needs row `i-1`" logic as LC 1143. But it does **nothing** to fix the underlying problem, because the problem was never about how many rows we keep. It was about **what we're storing inside each cell**. Each of the `2 × (m+1)` cells can still individually hold a string up to `min(n,m)` characters long, and we still pay the concatenation cost at every single cell, every single time. This "space optimization" shrinks the array dimension from `n` down to a constant — but the *string storage* dimension (`min(n,m)` characters per cell) was never touched. So:

**Time — still O(n · m · min(n, m)). Space — still O(m · min(n, m))** — better than full tabulation's `O(n·m·min(n,m))`, but still asymptotically far worse than LC 1143's clean `O(m)`.

---

## The Diagnosis — Why This Gets "Partially Accepted"

Here's the picture side by side:

```
┌─────────────────────────────┬───────────────────────┬─────────────────────────┐
│                             │  LC 1143 (length only)│  Idea A (string DP)     │
├─────────────────────────────┼───────────────────────┼─────────────────────────┤
│  What's stored in dp[i][j]  │  one int (4 bytes)    │  a whole String         │
│  Cost to compute one cell   │  O(1)                 │  O(min(n,m)) worst case │
│  Total time                 │  O(n·m)               │  O(n·m·min(n,m))        │
│  Total space                │  O(n·m) → O(m) opt.   │  O(n·m·min(n,m))        │
└─────────────────────────────┴───────────────────────┴─────────────────────────┘
```

For small test cases (say, strings of length 10–50), `n · m · min(n,m)` is small enough that this runs fast and passes fine — which is exactly why your submission got **partially** accepted. But GFG (like every judge) always includes larger stress-test cases, often with strings of length several hundred to a couple thousand characters. At `n = m = 1000`, LC 1143's approach does about a million operations. Idea A does about **a billion** — and that billion isn't just operations, it's a billion *characters sitting in memory* across the whole table. That's a Time Limit Exceeded and/or Memory Limit Exceeded waiting to happen, and it happens exactly on the larger hidden test cases — hence "partial" acceptance.

The deeper lesson, and this is the one worth remembering forever:

```
┌──────────────────────────────────────────────────────────────────┐
│  A DP table should store the SMALLEST possible unit of           │
│  information needed to make the next decision — not the          │
│  final answer itself, if the final answer is expensive to carry. │
│                                                                  │
│  An integer length is cheap: O(1) to store, O(1) to compare.     │
│  A string is expensive: O(k) to store, O(k) to copy at every     │
│  single cell that touches it.                                    │
│                                                                  │
│  Whenever the "natural" DP value is an expensive object          │
│  (a string, a list, a set...), ask: can I compute a cheap        │
│  SUMMARY (like a length, or a boolean) inside the DP table,      │
│  and reconstruct the expensive object ONCE at the end by         │
│  walking back through that cheap table?                          │
│                                                                  │
│  That reconstruction-by-backtracking is Striver's approach —     │
│  and it's coming up next.                                        │
└──────────────────────────────────────────────────────────────────┘
```

---

That's the full picture of *why* the direct approach fails — this was the important missing piece. 

# GFG: Print LCS — Part 2: Striver's Backtracking Approach

---

## Step 1 — Build the *Cheap* DP Table First

Since we now understand that storing full strings inside the DP table is what caused the blow-up, the fix is immediate: **go back to storing exactly what LC 1143 stored — plain integers (lengths).** Nothing about the recurrence changes from LC 1143 at all. We build the *exact same* `dp[i][j]` table, and only *after* it is fully filled do we think about extracting a string from it.

```
dp[i][j] = length of LCS between s1[0..i-1] and s2[0..j-1]   (Model B shift)

if s1[i-1] == s2[j-1]:   dp[i][j] = 1 + dp[i-1][j-1]
else:                    dp[i][j] = max(dp[i-1][j], dp[i][j-1])

Base case: dp[0][*] = dp[*][0] = 0
```

This is word-for-word LC 1143's tabulation. No new code is needed for this part — you've already written it once.

---

## Step 2 — The Full Table, Filled In

Using Striver's exact example: `s1 = "abcde"` (n = 5), `s2 = "bdgek"` (m = 5).

Remember the indexing convention (Model B): row `i` corresponds to `s1`'s first `i` characters, column `j` corresponds to `s2`'s first `j` characters. Row 0 and column 0 are the empty-prefix base cases.

```
              j=0   j=1(b)  j=2(d)  j=3(g)  j=4(e)  j=5(k)
i=0         [   0  ,   0   ,   0   ,   0   ,   0   ,   0   ]
i=1(a)      [   0  ,   0   ,   0   ,   0   ,   0   ,   0   ]
i=2(b)      [   0  ,   1   ,   1   ,   1   ,   1   ,   1   ]
i=3(c)      [   0  ,   1   ,   1   ,   1   ,   1   ,   1   ]
i=4(d)      [   0  ,   1   ,   2   ,   2   ,   2   ,   2   ]
i=5(e)      [   0  ,   1   ,   2   ,   2   ,   3   ,   3   ]
```

A few cells worth walking through by hand, so the pattern is obvious:

- `dp[2][1]`: `s1[1]='b'`, `s2[0]='b'` → **match** → `1 + dp[1][0] = 1 + 0 = 1`
- `dp[4][2]`: `s1[3]='d'`, `s2[1]='d'` → **match** → `1 + dp[3][1] = 1 + 1 = 2`
- `dp[5][4]`: `s1[4]='e'`, `s2[3]='e'` → **match** → `1 + dp[4][3] = 1 + 2 = 3`
- `dp[5][5]`: `s1[4]='e'`, `s2[4]='k'` → **no match** → `max(dp[4][5]=2, dp[5][4]=3) = 3`

Final answer sits at `dp[5][5] = 3` — same as LC 1143 would have told you. Nothing new so far. The new work starts now.

---

## Step 3 — The Core Idea: Walk *Backwards* Through the Finished Table

Here is the mental leap. The table above already tells you a story — it just doesn't tell it to you in plain English yet. Every single cell was filled using one of exactly two moves:

```
A "match" move    →  dp[i][j] = 1 + dp[i-1][j-1]     (came from the DIAGONAL)
A "no-match" move →  dp[i][j] = max(dp[i-1][j], dp[i][j-1])   (came from UP or LEFT)
```

If you start at the *final* cell `dp[n][m]` and ask "which of these two moves produced this exact value?" — you can figure it out just by comparing characters and neighboring cell values. And once you know which move produced a cell, you know exactly which cell to jump to next. Repeat this all the way back to a cell where either string ran out — and every "match" move you passed through along the way is one character of the actual LCS.

This is the same idea as the `hash[]` predecessor-array trick from LIS reconstruction — except here we don't need a separate array at all, because the **finished dp table itself already encodes every predecessor link**. We just have to know how to read it.

---

## Step 4 — The Backtracking Rule, Precisely

Start two pointers `i = n`, `j = m` — the very last cell, holding the final answer's length.

At every step, ask one question: **do `s1[i-1]` and `s2[j-1]` match?**

```
┌──────────────────────────────────────────────────────────────────┐
│  CASE 1 — Characters match                                       │
│  ────────────────────────────                                    │
│  This character was DEFINITELY used in the LCS (this is exactly  │
│  the same reasoning as the recursion's match branch).            │
│                                                                  │
│  → Record s1[i-1] as part of the answer                          │
│  → Move diagonally:  i-- , j--                                   │
│                                                                  │
│                                                                  │
│  CASE 2 — Characters do NOT match                                │
│  ─────────────────────────────────                               │
│  This cell's value came from whichever neighbor was LARGER —     │
│  dp[i-1][j] (moving up) or dp[i][j-1] (moving left).             │
│  Nothing gets recorded here — no character was used.             │
│                                                                  │
│  → If dp[i-1][j] > dp[i][j-1]:  move UP    → i--                 │
│  → Else:                        move LEFT  → j--                 │
│                                                                  │
│  Stop when:  i == 0  OR  j == 0                                  │
│  (one of the strings has been fully exhausted)                   │
└──────────────────────────────────────────────────────────────────┘
```

Every "diagonal" step contributes one letter. Every "up" or "left" step contributes nothing — it's just navigation. Because we walk from the **end** of the LCS toward its **start**, the letters get collected in *reverse* order, so the very last step is reversing the collected string.

---

## Step 5 — Tracing the Backtrack, Step by Step

Start at `i = 5, j = 5` (bottom-right corner, value 3).

```
Step 1:  i=5, j=5
         s1[4]='e', s2[4]='k' → NO MATCH
         compare dp[4][5]=2  vs  dp[5][4]=3  →  LEFT is bigger  →  move LEFT
         j = 4

Step 2:  i=5, j=4
         s1[4]='e', s2[3]='e' → MATCH!
         record 'e'  →  collected so far: "e"
         move diagonally: i=4, j=3

Step 3:  i=4, j=3
         s1[3]='d', s2[2]='g' → NO MATCH
         compare dp[3][3]=1  vs  dp[4][2]=2  →  LEFT is bigger  →  move LEFT
         j = 2

Step 4:  i=4, j=2
         s1[3]='d', s2[1]='d' → MATCH!
         record 'd'  →  collected so far: "ed"
         move diagonally: i=3, j=1

Step 5:  i=3, j=1
         s1[2]='c', s2[0]='b' → NO MATCH
         compare dp[2][1]=1  vs  dp[3][0]=0  →  UP is bigger  →  move UP
         i = 2

Step 6:  i=2, j=1
         s1[1]='b', s2[0]='b' → MATCH!
         record 'b'  →  collected so far: "edb"
         move diagonally: i=1, j=0

Step 7:  j == 0  →  STOP (string s2 exhausted)
```

Collected string (in reverse): `"edb"`. Reverse it → **`"bde"`** ✓ — length 3, matches `dp[5][5] = 3` exactly, and matches Striver's answer from the transcript.

Here's the same walk drawn as a path through the grid, so you can see it visually:

```
              j=0   j=1(b)  j=2(d)  j=3(g)  j=4(e)  j=5(k)
i=0         [   0  ,   0   ,   0   ,   0   ,   0   ,   0   ]
i=1(a)      [   0  ,   0   ,   0   ,   0   ,   0   ,   0   ]
i=2(b)      [   0  ,  [1]  ,   1   ,   1   ,   1   ,   1   ]
                 ↖   (match: b)
i=3(c)      [   0  ,  [1]  ,   1   ,   1   ,   1   ,   1   ]
                  ↑ (no match, UP is bigger)
i=4(d)      [   0  ,   1   ,  [2]  ,   2   ,   2   ,   2   ]
                        ↖   (match: d)
i=5(e)      [   0  ,   1   ,   2   ,  [2]  ,  [3]  ,  (3) ]
                                       ←         ↖      ←
                                (no match,     (match: e)  START
                                 LEFT bigger)
```

Read the arrows from right to left: **START → LEFT → diagonal(e) → LEFT → diagonal(d) → UP → diagonal(b) → STOP**. Every diagonal arrow drops a letter; every straight arrow is just navigation. Reading the dropped letters bottom-to-top-right-to-left and reversing gives `"bde"`.

---

## Step 6 — The Code

```java
public class Solution {

    public static String findLCS(int n, int m, String s1, String s2) {
        // Step 1: build the plain integer dp table — identical to LC 1143's tabulation
        int[][] dp = new int[n + 1][m + 1];

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = 1 + dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        // Step 2: the length of the LCS tells us the exact size of the answer string
        int len = dp[n][m];

        // We'll fill this array from the BACK forward, since backtracking
        // discovers characters in reverse order (last character of LCS first)
        char[] lcs = new char[len];
        int index = len - 1;

        // Step 3: backtrack from the bottom-right corner
        int i = n, j = m;

        while (i > 0 && j > 0) {

            if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                // MATCH — this character is part of the LCS
                // WHY place it at 'index' and decrement: we are walking
                // backward through the LCS, so the first match we find
                // is actually the LAST character of the final answer
                lcs[index] = s1.charAt(i - 1);
                index--;

                // Move diagonally — both strings consumed this character
                i--;
                j--;
            } else if (dp[i - 1][j] > dp[i][j - 1]) {
                // NO MATCH — this cell's value came from the row above
                // WHY: dp[i][j] = max(dp[i-1][j], dp[i][j-1]), and the
                //      "up" neighbor was the larger of the two
                i--;
            } else {
                // NO MATCH — this cell's value came from the column to the left
                j--;
            }
        }

        // Step 4: characters were placed from the back forward, so the
        // char array is already in correct left-to-right order — no
        // explicit reverse needed, unlike a string built via concatenation
        return new String(lcs);
    }
}
```

**A small but important implementation detail:** instead of building the answer with string concatenation (`result = char + result`, which — just like in Idea A — silently costs O(k) per operation and reintroduces the exact blow-up we just diagnosed), we allocate a fixed-size `char[]` of length `len` up front and fill it from the last index backward. Since we already know the exact final length (`dp[n][m]`), there is no need to grow anything or reverse anything at the end — placing each discovered character straight into its correct final position (`index--` after every match) does the reversal *for free*, with zero extra string-copy cost.

---

## Complexity Analysis

**Time Complexity — O(n × m + n + m):**

Building the `dp` table is the same two nested loops as LC 1143 — `O(n × m)`. The backtracking phase is a single `while` loop where, at every iteration, at least one of `i` or `j` decreases by exactly 1, and neither ever increases. So the walk can take at most `n + m` steps before one of them hits 0. Total: `O(n × m) + O(n + m)`, which simplifies to **O(n × m)** since the table-building term dominates.

**Space Complexity — O(n × m):**

The `dp` table is `(n+1) × (m+1)` integers — same as LC 1143's tabulation, `O(n × m)`. The `char[]` answer array is `O(min(n, m))` at most (an LCS can never be longer than the shorter string) — negligible next to the table. No recursion stack, since everything here is iterative. Total: **O(n × m)**.

Compare this to Idea A's `O(n · m · min(n,m))` time and space — the difference is exactly the factor of `min(n,m)` that came from carrying full strings through every cell instead of a single integer.

---

## Final Comparison — Both Approaches Side by Side

| Approach | Time | Space | Verdict |
|---|---|---|---|
| Idea A — DP directly returns strings (all 4 stages) | O(n·m·min(n,m)) | O(n·m·min(n,m)) | Correct logic, but blows up on larger inputs — partial accept |
| **Idea B — plain-integer DP + backtrack (Striver's)** | **O(n × m)** | **O(n × m)** | **Correct and efficient — full accept** |

---

## The Key Takeaway

```
┌───────────────────────────────────────────────────────────────────┐
│  The instinct "just make the DP return what I want directly"      │
│  is usually right — LC 1143 → length worked perfectly by          │
│  returning an int at every cell. But it silently breaks the       │
│  moment the "thing you want" is expensive to build and copy       │
│  (a string, a list, a set of paths...).                           │
│                                                                   │
│  The fix is always the same two-step shape:                       │
│                                                                   │
│  1. Build the CHEAPEST possible DP table that still lets you      │
│     answer the ORIGINAL simpler question (length, boolean,        │
│     count...). This is identical to whatever you already built    │
│     for the "give me just the length/count/existence" version     │
│     of the problem.                                               │
│                                                                   │
│  2. Once that table is fully filled, BACKTRACK through it once,   │
│     from the final cell to a base-case cell, re-deriving which    │
│     branch (match vs. no-match, take vs. not-take, etc.) produced │
│     each cell along the way. Every "productive" branch you pass   │
│     through contributes one piece to the final answer.            │
│                                                                   │
│  This exact two-step shape — cheap DP first, backtrack to         │
│  reconstruct second — is the same idea behind Print LIS's         │
│  hash[] array. The only difference here is that LCS's own         │
│  dp[][] table is already rich enough to backtrack through         │
│  directly, with no separate predecessor array needed.             │
└───────────────────────────────────────────────────────────────────┘
```

---

That completes **GFG: Print LCS**. Ready to move on to the next problem in Pattern 7 — **LC 516: Longest Palindromic Subsequence** (`LCS(s, reverse(s))`) — whenever you are.