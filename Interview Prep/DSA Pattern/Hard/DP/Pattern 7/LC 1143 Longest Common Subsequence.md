# LC 1143. Longest Common Subsequence

Key Concept: Core LCS template
Solution: https://www.youtube.com/watch?v=NPZn9jBrX8U&ab_channel=takeUforward
Status: Done

# Stage 1: Identification

**Step 1 — Which topic?**

You are given two strings. You need to find the **length of their longest common subsequence** — the longest sequence of characters that appears in both strings, in the same relative order, but not necessarily contiguously.

**Step 1b — Subsequence, once again, is the word that matters:**

```
"abcde"
a, b, c, d, e, ab, bd, ace, abcde, "" — all valid subsequences
(order preserved, characters can be skipped, need not be contiguous)
```

The moment a problem says **"common subsequence between two strings"** — that is the signal. To find the longest one, you must in principle **try every subsequence of string 1 against every subsequence of string 2**. And whenever you try all possibilities with overlapping subproblems, that becomes **Dynamic Programming**.

**Step 2 — Which pattern?**

This is the first problem where the input is **two sequences**, not one. The answer depends on comparing characters from both strings simultaneously. No grid in the geometric sense, no single array, no ordering constraint on one sequence alone — **two strings, character-by-character comparison**.

That is **Pattern 7: LCS (Longest Common Subsequence)**.

**Step 3 — Which key concept?**

Apply the **3-step shortcut**, with one structural addition compared to every pattern so far:

```
Step 1: Express everything in terms of TWO indices — one per string
Step 2: Explore all possibilities at (i, j) — MATCH or NOT MATCH
Step 3: Question says longest → take the best among all results
```

**Why two indices, and not one?**

In every 1D DP and even in 2D Grid DP, a single index (or a pair like row/column of one grid) was enough to describe "where am I in this problem." Here, you have two completely independent strings. A single index can describe a position in *one* string — it cannot simultaneously describe a position in the other. So you are forced to carry **two pointers, one per string** — call them `i` and `j`.

This is the defining structural signature of the entire LCS pattern:

> **Whenever a problem compares or aligns two sequences, the state needs two indices — one walking through each sequence.**
> 

The second new idea, replacing "pick / not pick" from earlier patterns, is **match / no-match**:

```
If the characters at the current two positions MATCH:
    → this character is definitely part of some common subsequence,
      count it, and shrink BOTH strings by one

If they DO NOT MATCH:
    → you cannot use both characters together,
      so try shrinking string 1 by one (keep string 2 as is),
      OR shrinking string 2 by one (keep string 1 as is),
      and take whichever gives the better answer
```

This **match / no-match** rule is the universal building block for every problem in Pattern 7 — LCS, Edit Distance, Longest Palindromic Subsequence, Shortest Common Supersequence, all of it.

---

# Stage 2: Intuition Building

### The Problem Setup

```
s1 = "abcde"
s2 = "ace"
```

Some common subsequences: `"a"`, `"c"`, `"e"`, `"ac"`, `"ae"`, `"ce"`, `"ace"`. The longest one is `"ace"`, length 3.

### Why You Cannot Just Generate All Subsequences

A string of length `n` has `2^n` subsequences. To brute-force this problem by generating every subsequence of `s1` and every subsequence of `s2`, then comparing them all, costs roughly `O(2^n × 2^m)` — completely impractical even for moderately sized strings. You need a recurrence that walks through both strings together instead of generating subsequences explicitly.

### Step 1 — Represent in terms of (i, j)

Define:

```
f(i, j) = length of the longest common subsequence
          between s1[0...i] and s2[0...j]
```

The answer we want is `f(n-1, m-1)` — the whole of both strings.

### Step 2 — Explore all possibilities at (i, j)

Look at the **last characters** of the two current substrings — `s1[i]` and `s2[j]`. Exactly two cases arise.

**Case 1 — Characters match (`s1[i] == s2[j]`):**

If the last characters of both substrings are the same character, that character is always safe to include in the common subsequence — there is never a reason to throw it away. Count it, and shrink **both** pointers by one, since this character has now been "used up" on both sides.

```
f(i, j) = 1 + f(i - 1, j - 1)
```

**Case 2 — Characters do not match:**

Now you cannot use both `s1[i]` and `s2[j]` together. But you don't know in advance which one is "useless" — maybe `s1[i]` will match something earlier in `s2`, or maybe `s2[j]` will match something earlier in `s1`. Since you cannot tell upfront, **try both possibilities** and keep the better one:

```
Option A: drop s1[i], keep s2[j] as is   → f(i - 1, j)
Option B: drop s2[j], keep s1[i] as is   → f(i, j - 1)

f(i, j) = max(f(i - 1, j), f(i, j - 1))
```

### Step 3 — Take the best

```
f(i, j) = if s1[i] == s2[j]:  1 + f(i-1, j-1)
          else:               max(f(i-1, j), f(i, j-1))
```

### Base Case

When either pointer goes below 0, that string has been completely exhausted — there is nothing left to compare. The longest common subsequence of an empty piece with anything is 0.

```
if i < 0 OR j < 0 → return 0
```

### Visualizing the Recursion Tree

Use a smaller pair so the tree stays readable: `s1 = "acd"`, `s2 = "ced"`.

```
s1: a c d        s2: c e d
    0 1 2            0 1 2

f(2,2):  s1[2]='d', s2[2]='d' → MATCH
         = 1 + f(1,1)

f(1,1):  s1[1]='c', s2[1]='e' → NO MATCH
         = max( f(0,1), f(1,0) )

f(0,1):  s1[0]='a', s2[1]='e' → NO MATCH
         = max( f(-1,1), f(0,0) )

f(-1,1) = 0   (out of bounds)

f(0,0):  s1[0]='a', s2[0]='c' → NO MATCH
         = max( f(-1,0), f(0,-1) ) = max(0, 0) = 0

so f(0,1) = max(0, 0) = 0

f(1,0):  s1[1]='c', s2[0]='c' → MATCH
         = 1 + f(0,-1) = 1 + 0 = 1

f(1,1) = max( f(0,1)=0, f(1,0)=1 ) = 1

f(2,2) = 1 + f(1,1) = 1 + 1 = 2
```

**Answer = 2** — the longest common subsequence is `"cd"`. ✓

### Are There Overlapping Subproblems?

For larger strings, states like `f(0,0)` or `f(1,1)` get reached from multiple different branches of the recursion — once through a match path, once through a no-match path. As `n` and `m` grow, this repetition explodes. **Overlapping subproblems confirmed** → DP is needed.

### DP Table Size

Two parameters:

- `i`: 0 to n-1 → **n values**
- `j`: 0 to m-1 → **m values**

dp table: **n × m**

---

# Stage 3: Coding

## Approach 1 — Pure Recursion (Brute Force)

```java
class Solution {
    public int longestCommonSubsequence(String text1, String text2) {
        int n = text1.length();
        int m = text2.length();
        return solve(n - 1, m - 1, text1, text2);
    }

    private int solve(int i, int j, String s1, String s2) {
        // Base case: either string is exhausted — nothing left to match
        // WHY return 0: an empty piece of a string shares no characters
        //               with anything
        if (i < 0 || j < 0) return 0;

        // Case 1: characters match — this character is always safe to use
        // WHY 1 + f(i-1, j-1): count this character, shrink BOTH pointers
        //     since it has now been consumed on both sides
        if (s1.charAt(i) == s2.charAt(j)) {
            return 1 + solve(i - 1, j - 1, s1, s2);
        }

        // Case 2: characters do not match — try dropping either one
        // WHY try both: we cannot know in advance which character is
        //     "useless" — it might match something earlier in the other string
        int dropS1 = solve(i - 1, j, s1, s2);
        int dropS2 = solve(i, j - 1, s1, s2);

        // Take the best of the two options
        return Math.max(dropS1, dropS2);
    }
}
```

**Time Complexity — O(2^(n+m)):**
At every state where characters don't match, the function makes 2 recursive calls. In the worst case (no matching characters at all), the recursion tree branches into 2 at every level, with depth up to `n + m` (the combined length you can shrink through). The total number of nodes grows as 2^(n+m). For strings of length 1000 each, this is astronomically impractical.

**Space Complexity — O(n+m):**
No dp array is allocated. The **recursion call stack** holds one frame per level. The deepest chain of calls — alternately shrinking `i` and `j` one at a time — goes at most `n + m` frames deep before hitting a base case. Stack uses O(n+m) space.

---

## Approach 2 — Memoization (Top-Down DP)

The same state `f(i, j)` is reached from multiple branches — for instance, through a "drop s1" path from one parent and a "drop s2" path from another. Store each result the first time it is computed.

**3 steps to convert recursion to memoization:**

```
Step 0: Declare dp array of size n × m, initialize all to -1
Step 1: Before computing, check — if dp[i][j] != -1, return it   (lookup)
Step 2: After computing, store — dp[i][j] = result                (store)
```

**One important practicality:** strings should always be passed by reference (which Java does automatically for `String` objects), never recreated as substrings at each call — recreating substrings repeatedly would silently blow up both time and memory.

```java
class Solution {
    public int longestCommonSubsequence(String text1, String text2) {
        int n = text1.length();
        int m = text2.length();

        // Step 0: dp[i][j] = -1 means not yet computed
        int[][] dp = new int[n][m];
        for (int[] row : dp) {
            Arrays.fill(row, -1);
        }
        return solve(n - 1, m - 1, text1, text2, dp);
    }

    private int solve(int i, int j, String s1, String s2, int[][] dp) {
        // Base case: either string exhausted
        if (i < 0 || j < 0) return 0;

        // Step 1: Already computed? Return stored value instantly
        // WHY: f(i, j) = LCS length of s1[0..i] and s2[0..j] —
        //      this never changes no matter which branch reached this state
        if (dp[i][j] != -1) return dp[i][j];

        int result;

        // Case 1: characters match
        if (s1.charAt(i) == s2.charAt(j)) {
            result = 1 + solve(i - 1, j - 1, s1, s2, dp);
        } else {
            // Case 2: characters don't match — try both drops, take the best
            int dropS1 = solve(i - 1, j, s1, s2, dp);
            int dropS2 = solve(i, j - 1, s1, s2, dp);
            result = Math.max(dropS1, dropS2);
        }

        // Step 2: Store before returning
        dp[i][j] = result;
        return dp[i][j];
    }
}
```

**Time Complexity — O(n × m):**
Each unique `(i, j)` pair is computed exactly once. There are `n × m` unique states. For each state, O(1) work is done — one character comparison and at most one `max()`. Total: O(n × m).

**Space Complexity — O(n × m) + O(n + m):**
Two sources of space. First, the dp array of size `n × m`. Second, the recursion call stack, which goes up to `n + m` levels deep in the worst case (alternating drops). Total: O(n × m) + O(n + m).

---

# Approach 3 — Tabulation (Bottom-Up DP)

You're right — and this is an important distinction worth getting exactly right, since it sets the mental model for every space optimization in this entire pattern. Let me redo Approach 3 properly, showing both ways of writing the shift, and explaining clearly why one of them leads naturally into space optimization and the other one doesn't.

---

## The Problem We're Solving First: Index `-1` Cannot Exist in an Array

The recursion's base case is `i < 0 || j < 0`. Java arrays cannot be indexed with `-1`. So before writing any loop, we need a way to represent the index `-1` as a real, valid array position.

The fix is always the same idea: **shift every index by +1**. But there are two different ways to actually write this shift in code, and only one of them sets you up cleanly for space optimization later. Let's see both, side by side.

## The Two Ways to Shift — Same Idea, Two Different Codings

Picture the original recursion's indices on a number line:

```
Original i:    -1   0   1   2   3   4
                ↓   ↓   ↓   ↓   ↓   ↓   (shift each one right by 1)
Shifted i:      0   1   2   3   4   5
```

So `i = -1` becomes `0`, `i = 0` becomes `1`, `i = 1` becomes `2`, and so on. The rule is simple: **shifted index = original index + 1**.

Now — when you write the `for` loop in code, which variable do you actually loop over? This is where the two approaches diverge.

---

### **`Way 1` — Loop over the ORIGINAL index, write into the SHIFTED position**

The recursion's base case is `i < 0 || j < 0`. Arrays in Java cannot be indexed with `-1`. In every prior pattern, base cases lived at clean non-negative boundaries (`index == 0`, `i == 0 && j == 0`, etc.), so this never came up. Here, the recursion genuinely needs to represent "one before the start" — and that is exactly `-1`.

**The fix — shift every index by `+1` when building the table.**

```
Original i:    -1   0   1   .   .   m-1
                ↓   ↓   ↓   ↓   ↓   ↓   (shift each one right by 1)
Shifted i:      0   1   2   .   .   m
```

```
Original **j**:    -1   0   1   .   .   n-1
                ↓   ↓   ↓   ↓   ↓   ↓   (shift each one right by 1)
Shifted j:      0   1   2   .   .   n
```

Define a new table `dp` of size `(m+1) × (n+1)`, where:

```
The computed value of f(i, j) will be stored at dp[i+1][j+1]

When the value of f(i, j) is need, it will be fetched from dp[i+1][j+1]
```

Under this shift:

- `f(-1, j)` (string 1 exhausted) becomes `dp[0][j+1]`
- `f(i, -1)` (string 2 exhausted) becomes `dp[i+1][0]`

So the entire **first row and first column of the new table are the base case**, and they are all 0 — exactly what `f(-1, anything)` and `f(anything, -1)` returned. Java already zero-initializes integer arrays, so this base case requires no extra code at all.

**Translating the recurrence under the shift:**

```
Original recurrence:
    if s1[i] == s2[j]:   f(i, j) = 1 + f(i-1, j-1)
    else:                f(i, j) = max(f(i-1, j), f(i, j-1))

Substituting dp[i+1][j+1] for f(i, j):
    if s1[i] == s2[j]:   dp[i+1][j+1] = 1 + dp[i][j]
    else:                dp[i+1][j+1] = max(dp[i][j+1], dp[i+1][j])
```

Notice the character check itself still uses the **original, unshifted** indices `i` and `j` directly into `s1` and `s2` — only the dp array indices carry the +1 shift.

```java
// Base
for (int j=-1; j<n; j++) {   // when i=-1
	dp[0][j+1] = 0;
}

for (int i=-1; i<n; i++) {   // when j=-1
	dp[i+1][0] = 0;
}

for (int i = 0; i < m; i++) {
    for (int j = 0; j < n; j++) {
        if (text1.charAt(i) == text2.charAt(j)) {
            dp[i + 1][j + 1] = 1 + dp[i][j];
        } else {
            dp[i + 1][j + 1] = Math.max(dp[i][j + 1], dp[i + 1][j]);
        }
    }
}
return dp[n1][n2];
```

Here the range of the indices `i` and `j` stays as the **original, unshifted** string positions — exactly what it was in the ***recursion***. That’s why we read `text1.charAt(idx1)` directly, no `idx-1` anywhere. 

But every time we write into the `dp` array, we have to remember to add `+1`. The result is stored at `dp[idx1+1][idx2+1]`, and the cell it depends on is `dp[idx1][idx2]` — one row up, one column left, in the *shifted* array.

### **`Way 2` — Loop over the SHIFTED index directly (this is what *Striver* taught)**

![Screenshot from 2026-06-30 13-37-48.png](LC%201143%20Longest%20Common%20Subsequence/Screenshot_from_2026-06-30_13-37-48.png)

```
Original **i** in recursion:    -1   0   1   .   .   m-1
Original **j** in ****recursion:    -1   0   1   .   .   n-1
               
Previously, we called **f(m-1, n-1)** to get the final answer.

Now we're gonna call **f(m, n)** to get the final answer.

That means, the new range of i & j are -
New **i**:   0   1   2   .  .  .   m
New **j**:   0   1   2   .  .  .   n
			
But inside the function call, we're gonna treat i as i-1 & j as j-1.
This is because a string ***str*** of length m is still accessed by *idx = 0, 1, ...m-1*

That means, 
				f(1) correspons to f(0)
				f(0) correspons to f(-1)
				........................
				
🎉**Advantage**: 
	 now the value of **f(i, j)** can directly be stored/fetched from **dp[i][j]**
	 
👎**Disadvantage:**
		while accessing the strings, do idx-1 inside function call f(i, j)
		- when accessing str1 for idx i - str1.charAt(i-1)
		- when accessing str2 for idx j - str2.charAt(j-1)
```

Therefore, understand the pros & cons of these 2 approaches. The approach is used in the            `LIS Pattern in DP`.

```java
// Base
for (int j=0; j<n; j++) {   // when i=0
	dp[0][j] = 0;
}

for (int i=0; i<m; i++) {   // when j=0
	dp[i][0] = 0;
}

for (int i = 1; i <= n; i++) {
    for (int j = 1; j <= m; j++) {
        if (text1.charAt(i - 1) == text2.charAt(j - 1)) {
            dp[i][j] = 1 + dp[i - 1][j - 1];
        } else {
            dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
        }
    }
}
return dp[n][m];
```

Here `i` and `j` are **already the dp array's own indices** — they go from `1` up to `n` and `m`. Whenever we need the actual character in the string, we subtract 1 (`i - 1`, `j - 1`). The cell we are filling is `dp[i][j]`, and the cell it depends on is `dp[i-1][...]` or `dp[...][j-1]` — written using the **same loop variable**, just minus one.

### Summary:

- `Approach 1`:
    - We kept the range of `i` & `j` the same as the original recursion
    - The shifting logic applied to the `DP[][]` matrix
    - Accessing any character in a String is simple - no shifting!
- **`Approach 2`**:
    - We *change* the range of `i` & `j` - shifted by `+1`
    - The states of `f(i, j)` are saved/fetched directly to the `DP[i][j]` matrix - no shifting needed!
    - Accessing any character in a String requires shifting - `*str.charAt(idx-1)*`

## 🕯️Why `Way 2` Is the One You Want

Look at Way 2's recurrence again:

```
dp[i][j] = ... **dp[i-1]**[j-1] ...    or    ... **dp[i-1]**[j] ... **dp[i]**[j-1] ...
```

Just by reading this line, without thinking at all, you can immediately see: **row `i` only ever needs row `i-1`.**

That single observation — 

> In order to calculate `dp[i][j]`, we just need it’s previous row `dp[i-1][..]`
> 

is the exact trigger that tells you ***space optimization*** is possible. It is the same pattern you've already seen in every **Grid DP** and **Knapsack problem**: 

> `dp[i]` depends on `dp[i-1]`, so two rows are enough, you don't need the whole table.
> 

Now look at `Way 1`'s recurrence:

```
dp[i + 1][j + 1] = ... dp[i][j] ...
```

The cell being written (`i + 1`) and the cell being read (`i`) don't share the same variable name on the page — one is `i+1`, the other is `i`. To notice that this is "current row depends on previous row," you first have to do the mental translation: "oh wait, `i+1` is the row I'm filling, and `i` is one less than that, so it IS the previous row." It's correct, but it costs you an extra mental step every single time you read the line — and in a harder problem with three or four terms in the recurrence, that extra step is exactly where bugs creep in during space optimization.

### The Rule Going Forward

```
┌─────────────────────────────────────────────────────────────────┐
│  When shifting indices to avoid negative array access:          │
│                                                                 │
│  Always loop over the SHIFTED index directly (i from 1 to n).   │
│  Subtract 1 only at the one place you touch the original        │
│  string/array: text1.charAt(i - 1).                             │
│  Everywhere else — including the recurrence itself — write      │
│  dp[i][...] and dp[i-1][...] using the SAME loop variable.      │
│                                                                 │
│  This way, "row i depends on row i-1" is visible directly in    │
│  the code, and turning it into a two-array space optimization   │
│  becomes a mechanical, almost copy-paste step.                  │
└─────────────────────────────────────────────────────────────────┘
```

So from here on, every tabulation in this pattern will be written using Way 2.

### The Final Tabulation Code

```java
class Solution {
    public int longestCommonSubsequence(String text1, String text2) {
        int n = text1.length();
        int m = text2.length();

        // dp[i][j] represents f(i-1, j-1) from the recursion — shifted by +1
        // i goes from 0 to n, j goes from 0 to m
        // dp[0][*] and dp[*][0] are the base cases (a string fully exhausted)
        // Java already zero-initializes int arrays, so these don't need
        // to be written explicitly — they are already 0
        int[][] dp = new int[n + 1][m + 1];

        // Loop directly over the SHIFTED index i and j (Way 2)
        // i = 1 corresponds to f(0, ...), i = n corresponds to f(n-1, ...)
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {

                // To read the actual character, subtract 1 — this is the
                // ONLY place where the shift needs to be "undone"
                if (text1.charAt(i - 1) == text2.charAt(j - 1)) {
                    // Match: f(i-1,j-1) → in shifted terms this is dp[i-1][j-1]
                    dp[i][j] = 1 + dp[i - 1][j - 1];
                } else {
                    // No match: max(f(i-1,j), f(i,j-1)) → dp[i-1][j], dp[i][j-1]
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        // Answer: f(n-1, m-1) → dp[n][m] under the shift
        return dp[n][m];
    }
}
```

### Full Cell-by-Cell DP Table Trace

```
s1 = "abcde"   (n = 5)
s2 = "ace"     (m = 3)
```

|  | j=0 | j=1 (a) | j=2 (c) | j=3 (e) |
| --- | --- | --- | --- | --- |
| i=0 | 0 | 0 | 0 | 0 |
| i=1 (a) | 0 | 1 | 1 | 1 |
| i=2 (b) | 0 | 1 | 1 | 1 |
| i=3 (c) | 0 | 1 | 2 | 2 |
| i=4 (d) | 0 | 1 | 2 | 2 |
| i=5 (e) | 0 | 1 | 2 | 3 |

Walking through a few key cells (remember: row `i` reads `s1.charAt(i-1)`, column `j` reads `s2.charAt(j-1)`):

- `dp[1][1]`: `s1[0]='a'`, `s2[0]='a'` → match → `1 + dp[0][0] = 1 + 0 = 1`
- `dp[2][1]`: `s1[1]='b'`, `s2[0]='a'` → no match → `max(dp[1][1]=1, dp[2][0]=0) = 1`
- `dp[3][2]`: `s1[2]='c'`, `s2[1]='c'` → match → `1 + dp[2][1] = 1 + 1 = 2`
- `dp[4][2]`: `s1[3]='d'`, `s2[1]='c'` → no match → `max(dp[3][2]=2, dp[4][1]=1) = 2`
- `dp[5][3]`: `s1[4]='e'`, `s2[2]='e'` → match → `1 + dp[4][2] = 1 + 2 = 3`

**Answer = `dp[5][3] = 3`** ✓ — the LCS is `"ace"`, length 3.

**Time Complexity — O(n × m):**
Two nested loops — outer runs `n` times, inner runs `m` times. Each cell does O(1) work — one character comparison, one addition or one `max()`. Total: O(n × m).

**Space Complexity — O(n × m):**
Only the dp array of size `(n+1) × (m+1)`. No recursion stack at all.

---

## Approach 4 — Space Optimization (The Final Form)

Because Approach 3 was written with `dp[i][j]` depending only on `dp[i-1][...]`, this step is now almost automatic. Row `i` only ever needs row `i-1` — the entire history before that is irrelevant. So instead of the full `(n+1) × (m+1)` table, keep just **two 1D arrays of size m+1**: `prev` (row `i-1`, already computed) and `curr` (row `i`, being computed).

```java
class Solution {
    public int longestCommonSubsequence(String text1, String text2) {
        int n = text1.length();
        int m = text2.length();

        // prev[j] = dp value for the previous row (i-1), shifted column j
        // Base case: row 0 (string 1 empty) — all zeros, already default
        int[] prev = new int[m + 1];

        for (int i = 1; i <= n; i++) {
            // curr[j] = dp value for the current row i, shifted column j
            int[] curr = new int[m + 1];

            // curr[0] stays 0 — this is the base case (string 2 empty)
            for (int j = 1; j <= m; j++) {

                if (text1.charAt(i - 1) == text2.charAt(j - 1)) {
                    // WHY prev[j-1]: this is dp[i-1][j-1] — diagonal value
                    //     from the row above, one column to the left
                    curr[j] = 1 + prev[j - 1];
                } else {
                    // WHY prev[j]: dp[i-1][j] — same column, row above
                    // WHY curr[j-1]: dp[i][j-1] — same row, already computed
                    //     this iteration, one column to the left
                    curr[j] = Math.max(prev[j], curr[j - 1]);
                }
            }

            // Slide forward: current row becomes new previous row
            prev = curr;
        }

        // prev now holds the last computed row (row n)
        return prev[m];
    }
}
```

**Time Complexity — O(n × m):**
Same two nested loops, same iteration count as tabulation. Each cell does O(1) work. No change.

**Space Complexity — O(m):**
No `(n+1) × (m+1)` array. No recursion stack. Just two arrays of size `m+1` — `prev` and `curr`. Memory depends only on the length of one string, not on `n` at all.

---

## Final Comparison

| Approach | Time | Space | Notes |
| --- | --- | --- | --- |
| Pure Recursion | O(2^(n+m)) | O(n+m) stack | Exponential — never use |
| Memoization | O(n × m) | O(n×m) + O(n+m) stack | Good interview starting point |
| Tabulation | O(n × m) | O(n × m) | Better — eliminates stack, no negative-index issue |
| Space Optimization | O(n × m) | **O(m)** | Best — submit this |

---

## The Key Takeaway

```
┌────────────────────────────────────────────────────────────────────────┐
│  LCS is the foundation template for ALL of Pattern 7.                  │
│  Every future problem — Edit Distance, Longest Palindromic             │
│  Subsequence, Shortest Common Supersequence — reuses this exact        │
│  skeleton with small twists.                                           │
│                                                                        │
│  The universal Pattern 7 recurrence:                                   │
│                                                                        │
│  f(i, j) = LCS-related answer for s1[0..i] and s2[0..j]                │
│                                                                        │
│  if s1[i] == s2[j]:                                                    │
│      MATCH → 1 + f(i-1, j-1)   (consume both pointers)                 │
│  else:                                                                 │
│      NO MATCH → max(f(i-1, j), f(i, j-1))  (drop one side, try both)   │
│                                                                        │
│  Base case: f(-1, anything) = f(anything, -1) = 0                      │
│                                                                        │
│  THE critical implementation rule for shifting negative indices:       │
│  Two ways exist to write the shift, and BOTH are correct.              │
│    Way 1: loop over the original index, write into idx+1.              │
│    Way 2: loop directly over the shifted index i (1 to n),             │
│           subtract 1 only when touching the actual string.             │
│  Always prefer Way 2. When dp[i][j] is written using dp[i-1][...]      │
│  with the SAME loop variable, the "row i depends only on row i-1"      │
│  fact is visible directly on the page — and space optimization         │
│  becomes a near copy-paste step (prev/curr arrays). Way 1 hides        │
│  this fact behind an extra "+1" translation, which is correct          │
│  but harder to read and easier to get wrong when optimizing.           │
└────────────────────────────────────────────────────────────────────────┘
```