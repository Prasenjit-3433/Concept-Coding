# GFG: Longest Common Substring

*(Building on LC 1143 and Print LCS — same problem family, one crucial rule change)*

---

# Stage 1: Identification

**Step 1 — Which topic?**

Two strings again. But read the question carefully this time: **substring**, not **subsequence**. That single word changes everything.

```
subsequence → characters can be picked non-contiguously, order preserved
              e.g. from "acjkp": "ajp", "cjk", "ap" are all valid subsequences

substring   → characters MUST be contiguous, no gaps allowed
              e.g. from "acjkp": "cjk" is a substring, "ajp" is NOT
              (a and p are not adjacent in the original string)
```

**Step 2 — Which pattern?**

Still two strings, still comparing them character by character, still asking for a "longest match." This is **Pattern 7: LCS** — but this problem is the one place in the pattern where the *"drop one side and try both"* branch from the classic template becomes actively wrong, and has to be thrown out.

**Step 3 — Which key concept?**

**Contiguity forbids the "no-match" branch entirely.**

In LC 1143, when characters didn't match, we had the freedom to drop either side and keep exploring — because a subsequence is allowed to skip characters. A substring is *not* allowed to skip characters. The moment two characters fail to match, **the chain is broken, full stop** — you cannot patch around the mismatch and pretend the run continued. This is the single conceptual shift that drives every difference in this problem.

---

# Stage 2: Intuition Building

### The Naive Instinct — Just Reuse the LCS Recurrence

It's tempting to think: "I already have the LCS recurrence — surely I can just run it and read off something clever from the table." Let's actually try this and watch exactly where it breaks, using a small example: `s1 = "acd"`, `s2 = "axd"`.

Recall the LCS recurrence:

```
if s1[i] == s2[j]:   f(i,j) = 1 + f(i-1,j-1)
else:                f(i,j) = max( f(i-1,j), f(i,j-1) )
```

Walk through comparing the two strings. `d == d` — match, length grows to 1. Shrink both strings: now compare `"ac"` vs `"ax"`. Here `c != x` — **no match**. The classic LCS recurrence's answer to this is: *"fine, drop one character from either side and keep trying."* So it tries dropping `x` (compare `"ac"` vs `"a"`) and dropping `c` (compare `"a"` vs `"ax"`). Both of those eventually land on `a == a` — a match of length 1. So the recurrence reports: `1 (from a) + 1 (from d) = 2` as if `"a...d"` were a valid common substring of length 2.

**But it isn't.** In `s1 = "acd"`, the `a` and the `d` are *not* adjacent — there's a `c` sitting between them. The LCS recurrence was allowed to "jump over" that `c` because subsequences permit gaps. A substring does not. What actually happened: the mismatch (`c` vs `x`) should have been a hard stop — the moment it occurred, any run that was building should have been thrown away completely, not patched around by dropping one character and continuing to add to the same accumulated length.

This is exactly the caveat Striver highlights: **the `max(f(i-1,j), f(i,j-1))` branch is what silently reintroduces "skipping," and skipping is precisely what a substring forbids.**

### The Fix — No Match Means Reset to Zero, Not "Try Both Sides"

The correct rule replaces that entire `else` branch with something much simpler:

```
if s1[i] == s2[j]:
    f(i, j) = 1 + f(i-1, j-1)      ← still extend the run, same as LCS
else:
    f(i, j) = 0                     ← the run is broken — nothing to carry forward
```

Why does resetting to `0` make sense? Because `dp[i][j]` is no longer answering *"what's the longest common subsequence considering everything up to here"* — it's answering a narrower, more literal question:

> **"If a common substring must END exactly at `s1[i]` and `s2[j]`, how long can that run possibly be?"**

If `s1[i] != s2[j]`, there is no common substring ending exactly at this pair of positions — the answer to that narrow question is unambiguously `0`. There is nothing from a neighboring cell worth "carrying forward," because carrying anything forward would mean allowing a gap — which is exactly the bug we just diagnosed.

### Why the Final Answer Is No Longer "the Last Cell" — It's "the Maximum Cell"

In LC 1143, the answer always lived at `dp[n][m]` — the LCS of the *entire* two strings necessarily ends by considering every character, so the bottom-right corner naturally holds the final answer.

Here, that's no longer true. A substring can end **anywhere** in the middle of either string — the longest common run doesn't have to reach all the way to the last characters of both strings. So instead of reading one fixed cell, you must **scan every cell in the entire table and take the maximum** — the same "answer isn't fixed at one corner" idea you already saw in LIS, where the answer could end at any index.

### Walking Through the Real Example

Using Striver's example: `s1 = "abcd"` (n = 4), `s2 = "abzd"` (m = 4).

```
s1: a  b  c  d
s2: a  b  z  d
```

The longest common **subsequence** here would be `"abd"` (length 3, skipping over `c`/`z`). But the longest common **substring** must be unbroken — and the only unbroken run shared by both strings is `"ab"` (length 2). `c` and `z` break the chain, and even though `d` matches `d` afterward, that `d` cannot "reconnect" to the earlier `"ab"` run because of the broken link — it can only start a fresh run of length 1.

---

# Stage 3: Coding

### A Note on Recursion — Why We Skip Straight to Tabulation

For every other Pattern 7 problem so far, we built recursion → memoization → tabulation → space optimization in order. Striver deliberately breaks that habit here, and it's worth understanding why.

The recurrence above needs `dp[i][j]` to mean *"length of the common run ending exactly at `i, j`"* — but the **final answer** needs the *maximum over the whole table*, which is a completely separate piece of information from any single `f(i,j)` call. A pure recursive function returns one value for one state; it can't simultaneously return "my own value" and "the best value seen anywhere in the whole exploration" without introducing a third piece of state (an accumulator threaded through every call) — which is exactly the "three states, not recommended" comment in the transcript. It's not that recursion is impossible here; it's that the clean, single-purpose recursion → memoization progression you've used everywhere else stops being the most natural fit, because the thing you actually want (a running maximum across all cells) doesn't live inside a single top-down call's return value at all — it lives naturally in a bottom-up scan. So we go directly to tabulation, exactly as Striver does.

---

## Approach — Tabulation (Bottom-Up DP)

```java
class Solution {
    public int longCommSubstr(String s1, String s2) {
        int n = s1.length(), m = s2.length();

        // dp[i][j] = length of the common substring RUN that ends
        //            exactly at s1[i-1] and s2[j-1] — not "best so far",
        //            literally the run ending here, or 0 if no such run exists
        int[][] dp = new int[n + 1][m + 1];

        // Base case: row 0 and column 0 represent an empty prefix —
        // no run can possibly end here. Java already zero-initializes
        // int arrays, so this is implicit, but written explicitly for clarity.
        for (int j = 0; j <= m; j++) dp[0][j] = 0;
        for (int i = 0; i <= n; i++) dp[i][0] = 0;

        // Track the best run length seen ANYWHERE in the table —
        // WHY: unlike LCS, the answer is not guaranteed to live at dp[n][m]
        int ans = 0;

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {

                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    // Characters match — extend whatever run was ending
                    // at the diagonal neighbor by exactly one character
                    dp[i][j] = 1 + dp[i - 1][j - 1];

                    // Update the global answer as we go
                    ans = Math.max(ans, dp[i][j]);
                } else {
                    // Characters DON'T match — the run is broken.
                    // WHY 0 and not max(dp[i-1][j], dp[i][j-1]):
                    // carrying forward a neighbor's value here would mean
                    // allowing a gap, which contradicts "substring must
                    // be contiguous." A broken run contributes nothing.
                    dp[i][j] = 0;
                }
            }
        }

        return ans;
    }
}
```

---

## Full Cell-by-Cell DP Table Trace

`s1 = "abcd"` (n = 4), `s2 = "abzd"` (m = 4)

```
                j=0    j=1(a)  j=2(b)   j=3(z)   j=4(d)
i=0         [   0   ,   0    ,   0    ,   0    ,   0    ]
i=1(a)      [   0   ,   1    ,   0    ,   0    ,   0    ]
i=2(b)      [   0   ,   0    ,   2    ,   0    ,   0    ]
i=3(c)      [   0   ,   0    ,   0    ,   0    ,   0    ]
i=4(d)      [   0   ,   0    ,   0    ,   0    ,   1    ]
```

Walking through the key cells:

- `dp[1][1]`: `s1[0]='a'`, `s2[0]='a'` → match → `1 + dp[0][0] = 1 + 0 = 1`. `ans = 1`.
- `dp[1][2]`: `s1[0]='a'`, `s2[1]='b'` → no match → `dp[1][2] = 0` (**not** `max(dp[0][2], dp[1][1])` — that would be the LCS bug)
- `dp[2][2]`: `s1[1]='b'`, `s2[1]='b'` → match → `1 + dp[1][1] = 1 + 1 = 2`. `ans = 2`.
- `dp[3][3]`: `s1[2]='c'`, `s2[2]='z'` → no match → `dp[3][3] = 0`. This is the break — the `"ab"` run dies here completely.
- `dp[4][4]`: `s1[3]='d'`, `s2[3]='d'` → match → `1 + dp[3][3] = 1 + 0 = 1`. This `d` can only start a *fresh* run of length 1 — it has no memory of the earlier `"ab"` run, exactly because `dp[3][3]` was reset to 0.

Every other cell is either a mismatch (reset to 0) or a base-case row/column (already 0), so the entire rest of the table stays empty.

**Final answer = `max(all cells) = 2`**, found at `dp[2][2]` — corresponding to the substring **`"ab"`** ✓, exactly matching Striver's result.

Notice something important in this trace: `dp[3][1]` and `dp[4][1]`, etc., all stay 0 even though `a` appears in both strings — because `a` at `s1[2]` (`'c'`… wait, `s1[2]='c'`) never matches `s2[0]='a'` at that position pair, so those cells are correctly 0. The table only lights up exactly where a contiguous run is actively in progress, and nowhere else — that sparsity is the visual signature of "substring DP" versus the denser, monotonically-non-decreasing look of an LCS table.

---

## Approach — Space Optimization

The recurrence only ever touches `dp[i-1][j-1]` — the single diagonal neighbor. It never needs `dp[i-1][j]` or `dp[i][j-1]` at all (unlike LCS's `else` branch, which needed both the row above *and* the same row to the left). This makes the space reduction even more straightforward than in LC 1143.

```java
class Solution {
    public int longCommSubstr(String s1, String s2) {
        int n = s1.length(), m = s2.length();

        // prev[j] = dp value for row i-1, column j (the diagonal source)
        int[] prev = new int[m + 1];
        int ans = 0;

        for (int i = 1; i <= n; i++) {
            int[] curr = new int[m + 1];
            // curr[0] stays 0 — base case, no run can end at column 0

            for (int j = 1; j <= m; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    // WHY prev[j-1]: this is dp[i-1][j-1], the diagonal
                    // neighbor from the row above — the run's continuation
                    curr[j] = 1 + prev[j - 1];
                    ans = Math.max(ans, curr[j]);
                } else {
                    // Broken run — no carry-forward, stays default 0
                    curr[j] = 0;
                }
            }

            // Slide forward: current row becomes next iteration's "previous"
            prev = curr;
        }

        return ans;
    }
}
```

One subtlety worth flagging explicitly: because the recurrence *only* reads the diagonal (`prev[j-1]`), there's no risk of a same-row, left-to-right ordering issue the way there was in 0/1 Knapsack's right-to-left rule — every value we read here always comes from the *previous* row's array, which is never mutated during the current row's fill. So the fill direction (left to right) doesn't matter at all for correctness — it's simply the natural order.

---

## Complexity Analysis

**Time Complexity — O(n × m):**

A single pass of two nested loops — outer runs `n` times, inner runs `m` times. Each cell does O(1) work: one character comparison, and either an addition or a reset to 0. No backtracking phase is needed here (the question only asks for the *length*, not the actual substring), so unlike Print LCS, this is the complete cost. Total: **O(n × m)**.

**Space Complexity — O(n × m) for tabulation, O(m) after space optimization:**

The full table is `(n+1) × (m+1)` integers. After noticing the recurrence only ever needs the single row directly above, this collapses to two 1D arrays of size `m+1` — **O(m)**.

---

## Final Comparison

| Approach | Time | Space | Notes |
|---|---|---|---|
| Tabulation | O(n × m) | O(n × m) | Correct — direct application of the reset rule |
| Space Optimization | O(n × m) | **O(m)** | Best — submit this |

---

## How This Differs From LC 1143 (LCS)

| Property | LC 1143 — LCS (subsequence) | GFG — Longest Common Substring |
|---|---|---|
| Gaps allowed? | Yes — that's the whole point of "subsequence" | **No — must be contiguous** |
| No-match rule | `max(dp[i-1][j], dp[i][j-1])` — carry forward the better option | **`0` — hard reset, nothing carries forward** |
| Where the answer lives | Always the fixed corner `dp[n][m]` | **Anywhere in the table — must scan for the max** |
| Recursion → Memo → Tab → Space Opt? | Yes, clean fit at every stage | **Skipped straight to tabulation** — the "running max across all cells" doesn't fit naturally into a single recursive call's return value |
| Cells needed for recurrence | Diagonal, up, AND left | **Diagonal only** |

---

## The Key Takeaway

```
┌───────────────────────────────────────────────────────────────────┐
│  The single word that changes everything: "substring" forbids     │
│  gaps, and "subsequence" allows them. Every difference in this    │
│  problem traces back to that one fact.                            │
│                                                                   │
│  The LCS recurrence's else-branch — max(dp[i-1][j], dp[i][j-1])   │
│  — is precisely the mechanism that lets subsequences skip         │
│  characters. The moment contiguity is required, that branch       │
│  must be replaced with a hard reset to 0. There is no partial     │
│  credit for "almost adjacent" — a broken run contributes          │
│  nothing to what comes after it.                                  │
│                                                                   │
│  Because the answer can end at ANY cell (not just the final       │
│  corner), the extraction step also changes: track a running       │
│  maximum across the entire table instead of reading one fixed     │
│  cell at the end.                                                 │
│                                                                   │
│  General pattern-recognition signal for future problems:          │
│  "Longest/most common X" + the word "substring", "consecutive",   │
│  "contiguous", or "adjacent" → whatever DP recurrence you'd       │
│  normally write for the subsequence version, replace its          │
│  "carry forward on mismatch" step with a hard reset to the        │
│  base value (0 for length, empty for a string, etc.), and scan    │
│  the whole table for the answer instead of reading one cell.      │
└───────────────────────────────────────────────────────────────────┘
```