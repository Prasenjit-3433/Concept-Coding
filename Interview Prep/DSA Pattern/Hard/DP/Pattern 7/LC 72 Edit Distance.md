# LC 72. Edit Distance

Key Concept: LCS + insert/delete/replace costs
Solution: https://www.youtube.com/watch?v=fJaKO8FbDdo&ab_channel=takeUforward
Status: Done

# LC 72. Edit Distance

*(The final problem in Pattern 7 — and the one that finally **breaks** the "reduce to LCS formula" habit the last few problems trained into us)*

---

# Stage 1: Identification

## **Step 1 — Which topic?**

You're given two strings `word1` and `word2`. You are allowed exactly three operations, each costing 1 step, applicable **any number of times, anywhere in the string**:

```
Insert   → insert any character at any position
Delete   → remove any character from any position
Replace  → change any character into any other character
```

Find the **minimum number of operations** to convert `word1` into `word2`.

### 💣**Step 1b — The trap this problem is designed to catch**

We just finished **`LC 583 (Delete Operation for Two Strings)`**, where the *only* operations allowed waere deletion & insertion, and the entire problem collapsed into one clean formula:

```
minimum deletions = n + m - 2 × LCS(word1, word2)
```

The natural (and *wrong*) instinct after solving that is to assume this problem is "the same thing, just with one more operation bolted on" — maybe something like `n + m - 3×LCS` or some other tweak to the formula. **This instinct needs to be actively killed before we go further.**

Here's why the LCS-formula approach cannot survive the introduction of `replace`:

> In **LC 583**, every character not part of the shared LCS backbone had to be **deleted** — there was no other option. The formula worked because *"characters outside the LCS"* and *"characters that must be paid for"* were exactly the same set.
> 
> 
> The moment `replace` exists, that's no longer true. Two *mismatched* characters — one from `word1`, one from `word2` — do **not** need to go through "delete the first one, insert the second one" (2 operations). They can instead be fixed with a **single replace** (1 operation). A mismatch is no longer automatically a 2-operation cost; it can be a 1-operation cost.
> 

Let's make this concrete with Striver's own example: `word1 = "horse"`, `word2 = "ros"`.

```
LCS("horse", "ros") = "rs"   (length 2)

If we blindly reused the LC 583-style formula (treat everything outside
LCS as "must be removed and rebuilt"):
    "wasted" characters in horse = h, o, e  → 3 characters
    "wasted" characters in ros   = o        → 1 character
    A delete-and-reinsert-style formula would suggest something like
    3 deletions + 1 insertion = 4 operations

But the ACTUAL minimum is 3 operations:
    replace 'h' → 'r'   (horse → rorse)
    delete  'r'  (the original one)  → (rorse → rose)
    delete  'e'                      → (rose → ros)
```

Three operations, not four — because the mismatch at the very first position (`h` vs `r`) was resolved with a single `replace` instead of a delete-then-insert pair. **This is the whole reason this problem needs a brand-new recurrence, not a formula bolted onto LC 1143 or LC 583.** Replace fundamentally changes what "optimal" looks like at a mismatch, so we cannot reuse the shortcut — we have to go back to first principles and build the recursion from scratch, the same way we originally built LC 1143 itself.

## **Step 2 — Which pattern?**

Two strings, two pointers walking through them, comparing characters — this is still **Pattern 7: LCS / DP on Strings**. The *state definition* (two indices, one per string) doesn't change. What changes is the *transition* — the LCS "match / no-match" rule needs a third branch added to it.

## **Step 3 — Which key concept?**

**`String matching with three recovery operations at every mismatch`.**

Just like every problem in this pattern, we compare `s1[i]` and `s2[j]`:

```
If they MATCH:
    → no operation needed at all. Just shrink both pointers and move on.
    → this is identical to LCS's match case.

If they DON'T match:
    → in LCS, we had exactly two options (drop from s1, or drop from s2).
    → here, we have THREE options: insert, delete, or replace.
    → we cannot know upfront which is cheapest, so we TRY ALL THREE
      and take whichever gives the minimum, then add 1 for the
      operation we just performed.
```

This "try all options, take the minimum, add the cost of this step" is the standard recursion shortcut — the moment you see "minimum number of operations to transform," recursion (trying every option) is the natural first move, exactly the way Striver frames it: *whenever it's "try every possibility and pick the best," that's recursion, and once we see overlapping subproblems in that recursion tree, it becomes DP.*

---

# Stage 2: Intuition Building

## Is It Always Possible? — Establishing an Upper Bound First

Before hunting for the *minimum*, it's worth confirming the *maximum* you'd ever need — this tells you the recursion always terminates with a sane answer, and gives you a sanity check to compare against later.

You can always convert any `word1` into any `word2` by:

```
Step 1: Delete every character of word1 one at a time     → n deletions
Step 2: Insert every character of word2 one at a time      → m insertions

Total: n + m operations, guaranteed to work, always
```

So the answer is always **at most `n + m`**. The real question is how much smaller than `n + m` we can get by being clever about matches, replaces, and reuse.

## Walking Through Example 1 by Hand: `"horse"` → `"ros"`

![image.png](LC%2072%20Edit%20Distance/image.png)

```
horse → rorse    (replace h → r)
rorse → rose     (delete the extra r)
rose  → ros      (delete the e)

3 operations total.
```

Notice: we never needed to *insert* anything here — the characters that were "useful" in `word1` (the `r`, `o`, `s` buried inside `horse`) were already present and in the right relative order; we just had to clear out what didn't belong, and fix the one character where a straight replace was cheaper than a delete-and-reinsert pair.

## Walking Through Example 2 by Hand: `"intention"` → `"execution"`

```
intention  (9 letters)
execution  (9 letters)
```

Both strings are the same length here, which is a strong hint that a good solution will lean heavily on **replace** — same-length strings rarely need many net inserts or deletes, since the character *count* already matches; what's misaligned is which characters sit where.

```
Rough manual walk (not necessarily the unique optimal sequence,
but illustrates the mix of operations):

intention → inention    (delete a duplicated character to fix alignment)
inention  → enention    (replace i → e)
enention  → exention    (replace n → x)
exention  → exection    (replace n → c)
exection  → execution   (insert u)
```

The exact sequence of intermediate strings isn't the point — different valid sequences exist. What matters is: **this problem is going to mix all three operations freely**, and unlike LC 583, we can't predict in advance which mix is cheapest just by computing one number (like LCS length). We need the recursion to actually explore the mix at every mismatch.

## 🧠Building the Recursion — The `String Matching` Lens

![image.png](LC%2072%20Edit%20Distance/image%201.png)

Take a smaller, cleaner pair to build the intuition cleanly: comparing the tails of `"horse"` and `"ros"` — think of two pointers `i` (walking backward through `word1`) and `j` (walking backward through `word2`), exactly like every other Pattern 7 problem.

```
**word1**: h  o  r  s  e
                       ↑
                       i
**word2**: r  o  s      (imagine comparing against a near-identical prefix)
                 ↑              
                 j               
```

### **Case A — the characters at `i` and `j` match:**

If `word1[i] == word2[j]`, there is *zero* reason to touch this character. Any operation here (insert, delete, replace) would be wasted work — you already have a match, spending an operation on it can only make things worse or, at absolute best, be redundant. So: **do nothing, shrink both pointers, and recurse on the smaller problem.**

```
f(i, j) = 0 + f(i - 1, j - 1)      ← no operation cost, just shrink both
```

### **Case B — the characters at `i` and `j` do NOT match:**

This is where the three operations come in. Since we don't know upfront which is optimal, we try all three, and separately reason through what each one *means* for how the pointers move.

#### **Option 1 — Insert:**

![image.png](LC%2072%20Edit%20Distance/image%202.png)

Imagine inserting `word2[j]` into `word1`, positioned exactly where `word1[i]` currently sits. This new character is chosen to match `word2[j]` (that's the whole point of inserting it — to create a match right now). Since it matches, `word2[j]` is now "accounted for" — we can move `j` backward. But `word1[i]` itself hasn't gone anywhere; it's still sitting there waiting to be dealt with, so `i` stays put.

```
        we hypothetically slide in a new character here, matching word2[j]
                              ↓
word1:  h  o  r  s  e  [inserted char]
                    ↑
                    i
word2:  r  o  s                    
              ↑              
              j                       ↑
                  i doesn't move — the original word1[i] is untouched
                  j moves back — word2[j] has been "used up" by the insert

Cost: 1 (for the insert) + f(i, j - 1)
```

![image.png](LC%2072%20Edit%20Distance/image%203.png)

#### **Option 2 — Delete:**

![image.png](LC%2072%20Edit%20Distance/image%204.png)

Delete `word1[i]` outright. It no longer exists in the string, so `i` moves backward. But we haven't matched anything for `word2[j]` yet — it's still waiting, so `j` stays put.

```
word1:  h  o  r  s  e         ← *delete* this character entirely
                    ↑
                    i moves back (this character is gone)
word2:  r  o  s                    
              ↑              
              j                ← j stays — word2[j] still needs a match

Cost: 1 (for the delete) + f(i - 1, j)
```

![image.png](LC%2072%20Edit%20Distance/image%205.png)

#### **Option 3 — Replace:**

![image.png](LC%2072%20Edit%20Distance/image%206.png)

Overwrite `word1[i]` with `word2[j]` directly. Now they match by construction — both characters are "used up" simultaneously, so both pointers move backward together, exactly like the match case, except we had to pay for it.

```
word1:  h  o  r  s  [e→s]     ← replaced in place, now equals word2[j]
                     ↑
                     i
word2:  r  o  s
              ↑              
              j 
                       both i and j move back together

Cost: 1 (for the replace) + f(i - 1, j - 1)
```

![image.png](LC%2072%20Edit%20Distance/image%207.png)

### Step 3 — Take the Minimum

Since we don't know which of the three recovers from a mismatch most cheaply, we compute all three and keep the best:

```
f(i, j) = 1 + min( f(i, j-1),        ← insert
                    f(i-1, j),        ← delete
                    f(i-1, j-1)  )    ← replace
```

## The Full Recurrence, Side by Side

```
f(i, j) = minimum operations to convert word1[0..i] into word2[0..j]

if word1[i] == word2[j]:
    f(i, j) = f(i-1, j-1)                                    ← free, no cost

else:
    f(i, j) = 1 + min( f(i, j-1),      // insert
                        f(i-1, j),      // delete
                        f(i-1, j-1) )   // replace
```

## Base Cases — "When It's Over"

Following the same instinct as every other Pattern 7 problem: **the base cases live at the point where one string runs out of characters.**

**Case 1 — `word1` is exhausted (`i < 0`):**

Picture `word1` fully consumed, but `word2` still has `j + 1` characters left standing (indices `0` through `j`). Since there's nothing left in `word1` to match against, delete, or replace, the *only* possible move is to **insert** every remaining character of `word2`, one at a time.

```
word1: (nothing left)
word2:  r  o           ← j = 1, meaning 2 characters remain (indices 0, 1)

To build "ro" out of nothing: insert 'r', insert 'o' → 2 operations = j + 1
```

```
if i < 0:  return j + 1
```

**Case 2 — `word2` is exhausted (`j < 0`):**

Symmetric reasoning. `word2` has nothing left, but `word1` still has `i + 1` characters remaining. The only way to make `word1`'s remainder disappear is to **delete** every one of them.

```
word1:  h  o  r        ← i = 2, meaning 3 characters remain (indices 0,1,2)
word2: (nothing left)

To reduce "hor" to nothing: delete 'h', delete 'o', delete 'r' → 3 = i + 1
```

```
if j < 0:  return i + 1
```

## Visualizing the Recursion Tree — Confirming Overlap

Take a small slice: `word1 = "ab"`, `word2 = "ac"`.

```
f(1, 1):  word1[1]='b', word2[1]='c' → NO MATCH
          f(1,1) = 1 + min( f(1,0), f(0,1), f(0,0) )

f(1,0):   word1[1]='b', word2[0]='a' → NO MATCH
          f(1,0) = 1 + min( f(1,-1), f(0,0), f(0,-1) )

f(0,1):   word1[0]='a', word2[1]='c' → NO MATCH
          f(0,1) = 1 + min( f(0,0), f(-1,1), f(-1,0) )
                                       ↑
                        NOTICE: f(0,0) appears in BOTH f(1,0) and f(0,1) —
                        reached via two completely different branches
```

`f(0,0)` gets computed from more than one parent. As `n` and `m` grow, states like this get revisited constantly across the branching tree of insert/delete/replace choices. **Overlapping subproblems confirmed** → Dynamic Programming applies.

## DP Table Size

Two parameters, same shape as every LCS-family problem:

- `i`: 0 to n-1 → **n values**
- `j`: 0 to m-1 → **m values**

dp table: **n × m** (plus the usual Model B shift to handle the `-1` base cases cleanly, exactly as we did for LC 1143).

---

## Where This Leaves Us

```
┌────────────────────────────────────────────────────────────────────────┐
│  LC 72 is the point in Pattern 7 where "reduce to a formula"           │
│  stops working, and we go back to building a fresh recurrence          │
│  from first principles — exactly the way we first built LC 1143.       │
│                                                                        │
│  The match case is identical to every LCS problem:                     │
│      match → shrink both pointers, no cost                             │
│                                                                        │
│  The mismatch case grows from LCS's 2 options to 3:                    │
│      LCS mismatch:  drop from s1  OR  drop from s2                     │
│      Edit Distance: insert, delete, OR replace — try all three,        │
│                      take the minimum, pay 1 for whichever we use      │
│                                                                        │
│  Base cases mirror LCS's shape but change meaning:                     │
│      word1 exhausted → must INSERT the rest of word2 → j + 1           │
│      word2 exhausted → must DELETE the rest of word1 → i + 1           │
└────────────────────────────────────────────────────────────────────────┘
```

---

# Stage 3: Coding

## Approach 1 — Pure Recursion (Brute Force)

Direct translation of the recurrence built in Stage 2. Nothing new conceptually — just code it exactly as derived.

```java
class Solution {
    public int minDistance(String word1, String word2) {
        int n = word1.length();
        int m = word2.length();
        return solve(n - 1, m - 1, word1, word2);
    }

    private int solve(int i, int j, String s1, String s2) {
        // Base case 1: word1 exhausted — must INSERT the rest of word2
        // WHY j + 1: if j = 1, indices 0 and 1 remain in word2 → 2 chars → j+1
        if (i < 0) return j + 1;

        // Base case 2: word2 exhausted — must DELETE the rest of word1
        // WHY i + 1: symmetric reasoning to base case 1
        if (j < 0) return i + 1;

        // Case A: characters match — no operation needed, shrink both
        if (s1.charAt(i) == s2.charAt(j)) {
            return solve(i - 1, j - 1, s1, s2);
        }

        // Case B: mismatch — try all three operations, take the cheapest
        // Insert: word2[j] gets matched right now, i stays, j shrinks
        int insertOp = solve(i, j - 1, s1, s2);

        // Delete: word1[i] is removed, i shrinks, j stays
        int deleteOp = solve(i - 1, j, s1, s2);

        // Replace: word1[i] becomes word2[j], both shrink together
        int replaceOp = solve(i - 1, j - 1, s1, s2);

        // Pay 1 for whichever operation we use, plus the best sub-result
        return 1 + Math.min(insertOp, Math.min(deleteOp, replaceOp));
    }
}
```

**Time Complexity — Exponential, roughly O(3^(n+m)):**
At every mismatched state, the function branches into 3 recursive calls instead of LCS's 2. The recursion tree's branching factor is 3, and its depth is bounded by `n + m` (each call shrinks at least one of `i`/`j`, and in the replace case, both). So in the worst case the tree grows as 3^(n+m) — for strings of length even 15–20 each, this is already impractical.

**Space Complexity — O(n+m):**
No dp array is allocated. The recursion call stack holds one frame per level. The deepest chain — repeatedly hitting insert or delete, which only shrinks one pointer at a time — can go as deep as `n + m` frames before hitting a base case. Stack uses O(n+m) space.

---

## Approach 2 — Memoization (Top-Down DP)

The same state `f(i, j)` gets reached from multiple parent branches — confirmed in the recursion tree walkthrough in Stage 2. Store each result the first time it's computed.

```java
class Solution {
    public int minDistance(String word1, String word2) {
        int n = word1.length();
        int m = word2.length();

        // dp[i][j] = -1 means not yet computed
        int[][] dp = new int[n][m];
        for (int[] row : dp) {
            Arrays.fill(row, -1);
        }
        return solve(n - 1, m - 1, word1, word2, dp);
    }

    private int solve(int i, int j, String s1, String s2, int[][] dp) {
        // Base cases — identical to pure recursion
        if (i < 0) return j + 1;
        if (j < 0) return i + 1;

        // Step 1: Already computed? Return stored value instantly
        // WHY: f(i,j) = min ops to convert s1[0..i] to s2[0..j] —
        //      this never changes no matter which branch reached it
        if (dp[i][j] != -1) return dp[i][j];

        int result;

        if (s1.charAt(i) == s2.charAt(j)) {
            result = solve(i - 1, j - 1, s1, s2, dp);
        } else {
            int insertOp = solve(i, j - 1, s1, s2, dp);
            int deleteOp = solve(i - 1, j, s1, s2, dp);
            int replaceOp = solve(i - 1, j - 1, s1, s2, dp);
            result = 1 + Math.min(insertOp, Math.min(deleteOp, replaceOp));
        }

        // Step 2: Store before returning
        dp[i][j] = result;
        return dp[i][j];
    }
}
```

**Time Complexity — O(n × m):**
Each unique `(i, j)` pair is computed exactly once. There are `n × m` unique states. For each state, O(1) work is done — one character comparison, at most a `min()` over 3 values. Total: **O(n × m)**.

**Space Complexity — O(n × m) + O(n + m):**
Two sources — the dp array of size `n × m`, and the recursion call stack, which can still go up to `n + m` levels deep on the first exploration. Total: **O(n × m) + O(n + m)**.

---

## Approach 3 — Tabulation (Bottom-Up DP)

### Handling the Negative Indices — the One-Based Shift

Just like LC 1143, the recursion's base cases use `i < 0` and `j < 0` — indices that don't exist in a Java array. Striver's fix, applied consistently across this whole pattern (Way 2 from LC 1143): **loop directly over the shifted index**, and subtract 1 only at the one place where we actually touch the string.

```
Original i:  -1   0   1   ...   n-1
Shifted i:    0   1   2   ...   n

Original j:  -1   0   1   ...   m-1
Shifted j:    0   1   2   ...   m
```

Define `dp[i][j]` (using the shifted indices directly) to mean: *minimum operations to convert `word1`'s first `i` characters into `word2`'s first `j` characters.* So `dp[0][j]` means "word1 is empty," and `dp[i][0]` means "word2 is empty" — exactly the two base cases from recursion.

### Translating the Base Cases

```
Recursion:  if i < 0:  return j + 1     →  Tabulation:  dp[0][j] = j   for all j
Recursion:  if j < 0:  return i + 1     →  Tabulation:  dp[i][0] = i   for all i
```

Why `dp[0][j] = j` and not `j + 1`? Because under the shift, `dp[0][j]` represents `f(-1, j-1)` in the original recursion's terms — and `f(-1, j-1) = (j-1) + 1 = j`. The shift absorbs the `+1` automatically. Same reasoning for `dp[i][0] = i`.

```java
class Solution {
    public int minDistance(String word1, String word2) {
        int n = word1.length();
        int m = word2.length();

        // dp[i][j] = min ops to convert word1's first i chars
        //            into word2's first j chars (shifted indexing)
        int[][] dp = new int[n + 1][m + 1];

        // Base case 1: word1 empty (i=0) — must insert all j characters
        // WHY dp[0][j] = j: matches f(-1, j-1) = j from recursion
        for (int j = 0; j <= m; j++) {
            dp[0][j] = j;
        }

        // Base case 2: word2 empty (j=0) — must delete all i characters
        for (int i = 0; i <= n; i++) {
            dp[i][0] = i;
        }

        // Fill the rest — loop directly over shifted indices (Way 2)
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {

                // Subtract 1 ONLY when touching the actual strings
                if (word1.charAt(i - 1) == word2.charAt(j - 1)) {
                    // Match — no cost, take the diagonal
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    // Mismatch — try all three operations, take the cheapest
                    int insertOp  = dp[i][j - 1];       // word2 side shrinks
                    int deleteOp  = dp[i - 1][j];        // word1 side shrinks
                    int replaceOp = dp[i - 1][j - 1];    // both shrink

                    dp[i][j] = 1 + Math.min(insertOp, Math.min(deleteOp, replaceOp));
                }
            }
        }

        // Answer: full word1 converted to full word2
        return dp[n][m];
    }
}
```

### Full Cell-by-Cell DP Table Trace

`word1 = "horse"` (n = 5), `word2 = "ros"` (m = 3) — Striver's own example.

```
                  j=0   j=1(r)  j=2(o)  j=3(s)
i=0 ("")       [   0  ,   1   ,   2   ,   3   ]
i=1(h)         [   1  ,   1   ,   2   ,   3   ]
i=2(o)         [   2  ,   2   ,   1   ,   2   ]
i=3(r)         [   3  ,   2   ,   2   ,   2   ]
i=4(s)         [   4  ,   3   ,   3   ,   2   ]
i=5(e)         [   5  ,   4   ,   4   ,   3   ]
```

Walking through key cells (row `i` reads `word1.charAt(i-1)`, column `j` reads `word2.charAt(j-1)`):

- `dp[1][1]`: `word1[0]='h'`, `word2[0]='r'` → mismatch → `1 + min(dp[1][0]=1, dp[0][1]=1, dp[0][0]=0) = 1 + 0 = 1`
- `dp[2][2]`: `word1[1]='o'`, `word2[1]='o'` → match → `dp[1][1] = 1`
- `dp[3][1]`: `word1[2]='r'`, `word2[0]='r'` → match → `dp[2][0] = 2`
- `dp[4][3]`: `word1[3]='s'`, `word2[2]='s'` → match → `dp[3][2] = 2`
- `dp[5][3]`: `word1[4]='e'`, `word2[2]='s'` → mismatch → `1 + min(dp[5][2]=4, dp[4][3]=2, dp[4][2]=3) = 1 + 2 = 3`

**Answer = `dp[5][3] = 3`** ✓ — matches Striver's hand-derived answer of 3 operations exactly (`replace h→r`, `delete r`, `delete e`).

**Time Complexity — O(n × m):**
Two nested loops — outer runs `n` times, inner runs `m` times. Each cell does O(1) work — one character comparison, at most a `min()` over 3 values. Total: **O(n × m)**.

**Space Complexity — O(n × m):**
Only the dp array of size `(n+1) × (m+1)`. No recursion stack at all.

---

## Approach 4 — Space Optimization

### What the Recurrence Actually Needs

```
dp[i][j] depends on:
    dp[i-1][j-1]   ← diagonal, previous row
    dp[i-1][j]     ← same column, previous row
    dp[i][j-1]     ← same row, already computed this pass
```

Row `i` only ever needs row `i-1` (plus values already filled in the current row). This is the exact same shape as LC 1143's recurrence — so the same rolling-array trick applies: keep two 1D arrays, `prev` (row `i-1`) and `curr` (row `i`).

```java
class Solution {
    public int minDistance(String word1, String word2) {
        int n = word1.length();
        int m = word2.length();

        // prev[j] = dp value for row i-1
        // Base case: row 0 means word1 is empty → prev[j] = j
        int[] prev = new int[m + 1];
        for (int j = 0; j <= m; j++) {
            prev[j] = j;
        }

        for (int i = 1; i <= n; i++) {
            int[] curr = new int[m + 1];

            // Base case for this row: word2 empty (j=0) → curr[0] = i
            curr[0] = i;

            for (int j = 1; j <= m; j++) {
                if (word1.charAt(i - 1) == word2.charAt(j - 1)) {
                    // WHY prev[j-1]: this is dp[i-1][j-1], the diagonal
                    curr[j] = prev[j - 1];
                } else {
                    int insertOp  = curr[j - 1];   // dp[i][j-1] — same row, already filled
                    int deleteOp  = prev[j];        // dp[i-1][j] — row above
                    int replaceOp = prev[j - 1];    // dp[i-1][j-1] — diagonal

                    curr[j] = 1 + Math.min(insertOp, Math.min(deleteOp, replaceOp));
                }
            }

            // Slide forward: current row becomes next iteration's "previous"
            prev = curr;
        }

        // prev now holds the last computed row (row n)
        return prev[m];
    }
}
```

**Time Complexity — O(n × m):**
Same two nested loops, same iteration count as tabulation. No change.

**Space Complexity — O(m):**
No `(n+1) × (m+1)` array. Just two arrays of size `m+1` — `prev` and `curr`.

### Why We Cannot Go Below O(m) — Unlike Some Earlier Problems

A few problems back in this pattern (Longest Common Substring), the recurrence only ever touched the *diagonal* neighbor, which meant even the row-above dependency was minimal. Here, the recurrence genuinely needs **three different neighbors simultaneously** — diagonal (`prev[j-1]`), directly above (`prev[j]`), and same-row-to-the-left (`curr[j-1]`). Because we need both the **previous row's** value at `j` (for delete) and the **current row's** value at `j-1` (for insert) *at the same time*, we cannot collapse this down to a single rolling array the way some 0/1 Knapsack problems could — we genuinely need two full rows in flight simultaneously. **Two 1D arrays of size `m+1` is the practical floor for this problem.**

---

## Final Comparison

| Approach | Time | Space | Notes |
| --- | --- | --- | --- |
| Pure Recursion | O(3^(n+m)) | O(n+m) stack | Exponential — never use |
| Memoization | O(n × m) | O(n×m) + O(n+m) stack | Good interview starting point |
| Tabulation | O(n × m) | O(n × m) | Better — eliminates stack |
| Space Optimization | O(n × m) | **O(m)** | Best — submit this |

---

## How LC 72 Differs From Every Other Problem in Pattern 7

| Property | LC 1143 (LCS) | LC 583 (Delete Only) | LC 72 (Edit Distance) |
| --- | --- | --- | --- |
| Operations allowed | — (just comparing) | Delete only | **Insert, Delete, Replace** |
| Mismatch options | 2 (drop from s1 or s2) | 2 (same as LCS, then formula) | **3 (insert, delete, replace)** |
| Solvable via a formula on LCS? | — | Yes — `n + m - 2·LCS` | **No — needs its own recurrence** |
| Match case cost | — | 0 | **0** |
| Mismatch case cost | — | (handled by formula) | **1 + min of 3 sub-results** |
| Base case values | 0 / 0 | — | **`dp[0][j] = j`, `dp[i][0] = i`** |

---

## The Key Takeaway

```
┌────────────────────────────────────────────────────────────────────┐
│  LC 72 is the problem that breaks the "everything in Pattern 7     │
│  reduces to an LCS formula" pattern we'd gotten used to from       │
│  LC 516, LC 1312, and LC 583. The lesson: recognizing a            │
│  reduction is powerful, but recognizing when a NEW operation       │
│  breaks a previous reduction is just as important.                 │
│                                                                    │
│  The recurrence, in full:                                          │
│                                                                    │
│  if word1[i] == word2[j]:                                          │
│      f(i,j) = f(i-1, j-1)                     ← free               │
│  else:                                                             │
│      f(i,j) = 1 + min( f(i, j-1),    // insert                     │
│                         f(i-1, j),    // delete                    │
│                         f(i-1, j-1) ) // replace                   │
│                                                                    │
│  Base cases (after the one-based shift):                           │
│      dp[0][j] = j    (word1 empty → insert everything)             │
│      dp[i][0] = i    (word2 empty → delete everything)             │
│                                                                    │
│  Why space optimization stops at O(m), not O(1):                   │
│  The recurrence needs THREE simultaneous neighbors — diagonal,     │
│  directly above, AND same-row-to-the-left. Two full rolling rows   │
│  (prev + curr) is the practical floor here, unlike simpler         │
│  1D DP recurrences that collapse to a handful of variables.        │
│                                                                    │
│  General pattern-recognition signal going forward: whenever a      │
│  string-transformation problem allows REPLACE alongside            │
│  insert/delete, expect the "3-way minimum recurrence" template     │
│  from this problem — it's the standard shape for edit-distance     │
│  style problems, and it will NOT reduce to a clean LCS formula     │
│  the way pure-deletion or pure-insertion problems do.              │
└────────────────────────────────────────────────────────────────────┘
```
