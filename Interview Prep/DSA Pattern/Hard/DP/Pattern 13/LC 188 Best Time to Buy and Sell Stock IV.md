# LC 188. Best Time to Buy and Sell Stock IV

Key Concept: At most k transactions — generalize III
Solution: https://www.youtube.com/watch?v=IV1dHbk5CDc&ab_channel=takeUforward
Status: Done

# Stage 1: Identification

## **Step 1 — Which topic?**

Same array of stock prices as every problem in this pattern. But now the hardcoded "at most 2 transactions" from LC 123 is replaced by an arbitrary integer **k** — you are allowed **at most k transactions**, where k is given as input rather than fixed.

The moment you see **"generalize a hardcoded constant into a variable parameter"** — this is the clearest possible signal that you already own the solution. LC 123 solved "at most 2." LC 188 asks "at most k." If your LC 123 solution genuinely understood *why* it worked (rather than just pattern-matching on the number 2), turning it into LC 188 should require **zero new thinking** — only replacing a constant with a variable everywhere it appears.

## **Step 2 — Which pattern?**

Still **Pattern 13: State Machine DP**. This is the direct capstone of the `canBuy` + `transLimit` skeleton built across LC 122 and LC 123 — the exact same state machine, just with the transaction budget generalized.

## **Step 3 — Which key concept?**

**Two equally valid state formulations exist, and this problem is the perfect place to see them side by side, since Striver actually codes both here (whereas in LC 123 the second one was left as homework):**

```
Formulation A — canBuy + transLimit (LC 123's exact skeleton, generalized)
    f(index, canBuy, transLimit)
    transLimit ranges 0 to k
    Buy: transLimit unchanged.  Sell: transLimit - 1.

Formulation B — index + transaction number (the "homework" from LC 123, now built out fully)
    f(index, transactionNo)
    transactionNo ranges 0 to 2k
    Even transactionNo → this step is a BUY.  Odd transactionNo → this step is a SELL.
    Acting (buy or sell) always increases transactionNo by exactly 1.
```

Both formulations answer the exact same question and give the exact same result — this problem is a good checkpoint for confirming you understand *why* two structurally different-looking states can encode identical information. Since the transcript walks through Formulation B in full code detail this time (recursion → memoization → tabulation → space optimization), that's the one we'll build out completely below, with Formulation A given as the quick "just generalize the constant" version first.

---

# Stage 2: Intuition Building

### The Problem Setup

```
prices = [3, 2, 6, 5, 0, 3]
k = 2
```

Exactly the same shape of problem as LC 123, except the cap on transactions is now handed to you as an input rather than baked into the code as the literal `2`.

### Formulation A — The "Just Generalize the Constant" Version

If you go back to LC 123's recursive code:

```
f(index, canBuy, transLimit)

Base cases: index == n → 0
            transLimit == 0 → 0

canBuy == 1:  buy = -prices[index] + f(index+1, 0, transLimit)
              notBuy = f(index+1, 1, transLimit)
              return max(buy, notBuy)

canBuy == 0:  sell = prices[index] + f(index+1, 1, transLimit-1)
              notSell = f(index+1, 0, transLimit)
              return max(sell, notSell)
```

**Every single line of this is already correct for arbitrary k.** The only place the number `2` ever appeared was in the *initial call* — `f(0, 1, 2)` — and in the *size of the DP array's third dimension*. Change the initial call to `f(0, 1, k)`, change the array size from `3` (values 0,1,2) to `k+1` (values 0 through k), and the solution is complete. No recurrence logic changes at all — this is the cleanest possible illustration of what it means to have *actually understood* a pattern rather than memorized a specific instance of it.

```
DP table size: (n+1) × 2 × (k+1)
```

We won't re-derive this in full below since it is a pure copy-paste-and-relabel of LC 123 — instead, we build out **Formulation B** completely, since that's the one genuinely new construction Striver walks through in this lecture.

### Formulation B — Collapsing `canBuy` and `transLimit` Into One Counter

Recall the idea flagged (but not built) at the end of LC 123: instead of carrying **two** separate pieces of state — "am I holding stock?" and "how much budget is left?" — collapse them into a **single transaction-number counter** that increases by exactly 1 every time you take an action (buy or sell), and whose **parity** tells you which action is legal.

```
transaction number 0  →  even → BUY   (this is the 1st transaction's buy half)
transaction number 1  →  odd  → SELL  (this is the 1st transaction's sell half)
transaction number 2  →  even → BUY   (2nd transaction's buy half)
transaction number 3  →  odd  → SELL  (2nd transaction's sell half)
...
transaction number 2k-2 → even → BUY   (kth transaction's buy half)
transaction number 2k-1 → odd  → SELL  (kth transaction's sell half)
transaction number 2k     →  ALL TRANSACTIONS COMPLETE
```

Since k transactions means k buys and k sells — **2k actions total** — this counter needs to range from `0` up to `2k`, and once it *reaches* `2k`, there is nothing left to do.

### Why "Even Means Buy" Actually Works

Think about what it means to be at an **even** transaction number. It means every transaction *before* this one has been fully completed — paired buys and sells, in full. The very next logical action, therefore, has to be a **fresh buy** — you cannot sell something you haven't bought yet, and you cannot skip straight to a second buy either (that would mean holding two stocks at once, which isn't allowed). So being at an even count *forces* the next meaningful action to be a buy.

Symmetrically, being at an **odd** transaction number means you've just completed a buy but not yet its matching sell — you are, right now, holding one unsold stock. The only meaningful next action is to sell it.

This single **parity check** is doing exactly the same job that the separate `canBuy` flag did in Formulation A — it's just derived arithmetically from the counter instead of being tracked as its own independent variable.

### Step 1 — Represent in Terms of (index, transactionNo)

Define:

```
f(index, transactionNo) = maximum profit achievable from day 'index' onward,
                           given that 'transactionNo' actions (buys and sells,
                           combined) have already taken place
```

The answer we want is `f(0, 0)` — start at day 0, with zero actions taken so far.

### Step 2 — Explore All Possibilities at (index, transactionNo)

**Case 1 — `transactionNo` is even (a buy is due):**

```
Buy today:
    → pay prices[index], this action is now complete
    → transactionNo increases by 1
    → buy = -prices[index] + f(index + 1, transactionNo + 1)

Don't buy today:
    → nothing happens, transactionNo stays exactly the same
    → notBuy = 0 + f(index + 1, transactionNo)
```

**Case 2 — `transactionNo` is odd (a sell is due):**

```
Sell today:
    → receive prices[index], this action is now complete
    → transactionNo increases by 1
    → sell = prices[index] + f(index + 1, transactionNo + 1)

Don't sell today:
    → nothing happens, transactionNo stays exactly the same
    → notSell = 0 + f(index + 1, transactionNo)
```

Notice the pleasant symmetry here compared to Formulation A: **every action, whether buy or sell, increases the same single counter by exactly 1.** There's no separate "decrement on sell only" rule to remember — the bookkeeping is uniform across both branches, because both a buy and a sell are equally "one action closer to being done."

### Step 3 — Take Maximum

```
f(index, transactionNo) = max(buy, notBuy)     when transactionNo is even
f(index, transactionNo) = max(sell, notSell)   when transactionNo is odd
```

### Base Cases

```
if index == n → return 0            (no more days — no more profit possible)
if transactionNo == 2k → return 0   (all k transactions fully completed)
```

Exactly the same shape as every base case in this pattern — running out of days, or running out of budget, both cap the recursion off at zero further profit. The only change from Formulation A's `transLimit == 0` check is that the "budget exhausted" signal is now `transactionNo == 2k` instead of `transLimit == 0` — same meaning, different counting direction (Formulation A counts *down* from k, Formulation B counts *up* to 2k).

### Why the Array Needs to Be Sized `2k`, Not `k`

A subtle but important detail flagged directly in the lecture: it's tempting to size the transaction-number dimension as `k`, mirroring Formulation A's `transLimit` (which only ever needed `k+1` values, `0` through `k`). But Formulation B's counter isn't counting *transactions* — it's counting *individual actions*, and there are **two actions per transaction** (one buy, one sell). So the counter needs to range over `2k + 1` values (`0` through `2k`), and the very first fix needed when coding this up is exactly this: replace a naive size of `k` with **`2 * k`**.

### Visualizing the Counter

```
f(0, 0)   ← day 0, transactionNo = 0 (even → buy is due)
   │
   ├── buy: -prices[0] + f(1, 1)      ← transactionNo now 1 (odd → sell due)
   │                    │
   │                    ├── sell: prices[1] + f(2, 2)   ← back to even → buy due again
   │                    └── wait: 0 + f(2, 1)             ← still odd, still holding
   │
   └── skip: 0 + f(1, 0)               ← still even, still nothing bought
```

The parity flips every single time an action is taken, and stays put every time you skip — a clean, uniform mechanism replacing the two separate flags of Formulation A.

### Are There Overlapping Subproblems?

Just as with every prior stock problem, the same `(index, transactionNo)` pair is reachable through many different sequences of earlier skip/act decisions. **Overlapping subproblems confirmed** → DP applies.

### DP Table Size

Two parameters this time, not three — this is genuinely a smaller state space than Formulation A:

```
index:         0 to n     →  n+1 values
transactionNo: 0 to 2k    →  2k+1 values
```

dp table: **(n+1) × (2k+1)**

Compare this to Formulation A's `(n+1) × 2 × (k+1)` — both are the same order of magnitude (`O(n·k)`), but Formulation B needs only a single 1D array per row instead of a 2×(k+1) block, which is a genuinely cleaner shape to reason about once you're comfortable with what the counter represents.

---

# Stage 3: Coding

## Approach 1 — Pure Recursion (Brute Force)

Direct translation of the recurrence built in Stage 2 — a single `transactionNo` counter, incremented by exactly 1 on every action (buy or sell), with parity deciding which action is legal.

```java
class Solution {
    private int func(int idx, int transNo, int[] prices, int n, int k) {
        // Base case 1: no more days left — no more profit possible
        if (idx == n) return 0;

        // Base case 2: all k transactions (2k actions) completed
        // WHY 2*k and not k: each transaction is ONE buy + ONE sell —
        // two actions — so the counter must run all the way to 2k
        if (transNo == 2 * k) return 0;

        if (transNo % 2 == 0) {
            // Even transactionNo → a BUY is due
            // WHY: every transaction before this one is fully paired up,
            // so the only legal next action is starting a fresh buy
            int buy = -prices[idx] + func(idx + 1, transNo + 1, prices, n, k);
            int notBuy = 0 + func(idx + 1, transNo, prices, n, k);

            return Math.max(buy, notBuy);
        } else {
            // Odd transactionNo → a SELL is due
            // WHY: we're mid-transaction, holding exactly one unsold stock
            int sell = prices[idx] + func(idx + 1, transNo + 1, prices, n, k);
            int notSell = 0 + func(idx + 1, transNo, prices, n, k);

            return Math.max(sell, notSell);
        }
    }

    public int maxProfit(int k, int[] prices) {
        int n = prices.length;

        // Start at day 0, with 0 actions taken so far (transactionNo = 0)
        return func(0, 0, prices, n, k);
    }
}
```

**Time Complexity — O(2^n):**
At every day, the function branches into 2 recursive calls — act or skip. The recursion tree is a binary tree of depth n. Total nodes grow as 2^n. Completely impractical for realistic n — the `2k` cap only prunes branches early once transactions run out, it doesn't change the worst-case exponential shape.

**Space Complexity — O(n):**
No dp array. The recursion call stack holds one frame per day — the deepest chain goes from day 0 to day n. Stack uses O(n) space.

---

## Approach 2 — Memoization (Top-Down DP)

The same state `f(idx, transNo)` gets reached from many different combinations of earlier skip/act decisions. Store each result the first time it's computed.

```java
class Solution {
    private int func(int idx, int transNo, int[] prices, int n, int k, int[][] dp) {
        // Base cases — identical to pure recursion
        if (idx == n) return 0;
        if (transNo == 2 * k) return 0;

        // Step 1: Already computed? Return stored value instantly
        // WHY: f(idx, transNo) never changes no matter which sequence
        //      of earlier decisions led to this exact state
        if (dp[idx][transNo] != -1) return dp[idx][transNo];

        int result;

        if (transNo % 2 == 0) {
            int buy = -prices[idx] + func(idx + 1, transNo + 1, prices, n, k, dp);
            int notBuy = 0 + func(idx + 1, transNo, prices, n, k, dp);
            result = Math.max(buy, notBuy);
        } else {
            int sell = prices[idx] + func(idx + 1, transNo + 1, prices, n, k, dp);
            int notSell = 0 + func(idx + 1, transNo, prices, n, k, dp);
            result = Math.max(sell, notSell);
        }

        // Step 2: Store before returning
        dp[idx][transNo] = result;
        return result;
    }

    public int maxProfit(int k, int[] prices) {
        int n = prices.length;

        // dp[idx][transNo] = -1 means not yet computed
        // WHY 2*k for the second dimension: transactionNo ranges 0 to 2k,
        // which is 2k+1 distinct values — allocate one extra slot for safety
        int[][] dp = new int[n][2 * k + 1];
        for (int[] row : dp) {
            Arrays.fill(row, -1);
        }

        return func(0, 0, prices, n, k, dp);
    }
}
```

**A note directly from the transcript on the first bug encountered:** the initial attempt sized the second dimension as just `k`, mirroring how LC 123's `transLimit` only needed values `0` through `k`. But `transactionNo` counts *actions*, not *transactions* — there are two actions per transaction — so this silently produces an array-index-out-of-bounds error until corrected to `2 * k` (plus one extra slot for the inclusive upper bound).

**Time Complexity — O(n × 2k):**
Each unique `(idx, transNo)` pair is computed exactly once. There are `n × (2k+1)` unique states. For each state, O(1) work is done. Total: **O(n × k)**.

**Space Complexity — O(n × 2k) + O(n):**
Two sources of space. First, the dp array of size `n × (2k+1)`. Second, the recursion call stack, which goes n levels deep. Total: **O(n × k) + O(n)**.

---

## Approach 3 — Tabulation (Bottom-Up DP)

**Direction:** Recursion goes from index 0 **forward** toward index n. So tabulation goes the **opposite direction** — from index n **backward** toward index 0. Same rule as every prior stock problem.

**Base cases in tabulation:** exactly as Striver notes — since every base case in the recursion returns `0`, and Java already zero-initializes integer arrays, **no explicit base-case-filling code is needed at all.** This is a nice simplification compared to LC 123, which needed two separate explicit base-case loops.

```java
class Solution {
    public int maxProfit(int k, int[] prices) {
        int n = prices.length;

        // dp[idx][transNo] — sized n+1 for the day dimension (base case at idx=n),
        // and 2k+1 for the transaction-number dimension (base case at transNo=2k)
        // WHY no explicit base-case fill: every base case returns 0, and Java
        // arrays are already zero-initialized — this saves the two setup loops
        // that LC 123's tabulation needed
        int[][] dp = new int[n + 1][2 * k + 1];

        // Fill from idx = n-1 down to 0 — opposite of recursion's direction
        for (int idx = n - 1; idx >= 0; idx--) {
            // transNo runs from 2k-1 down to 0 — opposite of recursion's direction
            // WHY start at 2k-1, not 2k: transNo = 2k is the base case,
            // already correctly 0 from Java's default initialization
            for (int transNo = 2 * k - 1; transNo >= 0; transNo--) {

                if (transNo % 2 == 0) {
                    int buy = -prices[idx] + dp[idx + 1][transNo + 1];
                    int notBuy = 0 + dp[idx + 1][transNo];

                    dp[idx][transNo] = Math.max(buy, notBuy);
                } else {
                    int sell = prices[idx] + dp[idx + 1][transNo + 1];
                    int notSell = 0 + dp[idx + 1][transNo];

                    dp[idx][transNo] = Math.max(sell, notSell);
                }
            }
        }

        // Answer: start at day 0, zero actions taken so far
        return dp[0][0];
    }
}
```

### Full Cell-by-Cell DP Table Trace

`prices = [3, 2, 6, 5, 0, 3]` (n = 6), `k = 2` → `transNo` ranges 0 to 4 (`2k = 4`)

```
Base case — idx = 6 (all transNo, from Java's zero-init):
transNo:  0  1  2  3  4
dp[6]:  [ 0, 0, 0, 0, 0 ]
```

**idx = 5 (price 3):**

```
transNo=3 (odd, sell due): sell=3+dp[6][4]=3, notSell=dp[6][3]=0 → max=3
transNo=2 (even, buy due): buy=-3+dp[6][3]=-3, notBuy=dp[6][2]=0 → max=0
transNo=1 (odd, sell due): sell=3+dp[6][2]=3, notSell=dp[6][1]=0 → max=3
transNo=0 (even, buy due): buy=-3+dp[6][1]=-3, notBuy=dp[6][0]=0 → max=0

dp[5]: [0, 3, 0, 3, 0]
```

**idx = 4 (price 0):**

```
transNo=3: sell=0+dp[5][4]=0, notSell=dp[5][3]=3 → max=3
transNo=2: buy=0+dp[5][3]=3,  notBuy=dp[5][2]=0  → max=3
transNo=1: sell=0+dp[5][2]=0, notSell=dp[5][1]=3 → max=3
transNo=0: buy=0+dp[5][1]=3,  notBuy=dp[5][0]=0  → max=3

dp[4]: [3, 3, 3, 3, 0]
```

**idx = 3 (price 5):**

```
transNo=3: sell=5+dp[4][4]=5, notSell=dp[4][3]=3 → max=5
transNo=2: buy=-5+dp[4][3]=-2, notBuy=dp[4][2]=3 → max=3
transNo=1: sell=5+dp[4][2]=8,  notSell=dp[4][1]=3 → max=8
transNo=0: buy=-5+dp[4][1]=-2, notBuy=dp[4][0]=3 → max=3

dp[3]: [3, 8, 3, 5, 0]
```

**idx = 2 (price 6):**

```
transNo=3: sell=6+dp[3][4]=6,  notSell=dp[3][3]=5 → max=6
transNo=2: buy=-6+dp[3][3]=-1, notBuy=dp[3][2]=3  → max=3
transNo=1: sell=6+dp[3][2]=9,  notSell=dp[3][1]=8 → max=9
transNo=0: buy=-6+dp[3][1]=2,  notBuy=dp[3][0]=3  → max=3

dp[2]: [3, 9, 3, 6, 0]
```

**idx = 1 (price 2):**

```
transNo=3: sell=2+dp[2][4]=2,  notSell=dp[2][3]=6 → max=6
transNo=2: buy=-2+dp[2][3]=4,  notBuy=dp[2][2]=3  → max=4
transNo=1: sell=2+dp[2][2]=5,  notSell=dp[2][1]=9 → max=9
transNo=0: buy=-2+dp[2][1]=7,  notBuy=dp[2][0]=3  → max=7

dp[1]: [7, 9, 4, 6, 0]
```

**idx = 0 (price 3):**

```
transNo=3: sell=3+dp[1][4]=3,  notSell=dp[1][3]=6 → max=6
transNo=2: buy=-3+dp[1][3]=3,  notBuy=dp[1][2]=4  → max=4
transNo=1: sell=3+dp[1][2]=7,  notSell=dp[1][1]=9 → max=9
transNo=0: buy=-3+dp[1][1]=6,  notBuy=dp[1][0]=7  → max=7

dp[0]: [7, 9, 4, 6, 0]
```

**Answer = `dp[0][0] = 7`** ✓ — matches the well-known expected result for this exact input: buy at price 2 (day 1), sell at price 6 (day 2) for profit 4, then buy at price 0 (day 4), sell at price 3 (day 5) for profit 3, giving `4 + 3 = 7`.

**Time Complexity — O(n × 2k):**
Two nested loops — outer runs n times, inner runs 2k times. Each cell does O(1) work. Total: **O(n × k)**.

**Space Complexity — O(n × 2k):**
Only the dp array of size `(n+1) × (2k+1)`. No recursion stack.

---

## Approach 4 — Space Optimization (The Final Form)

Look at the tabulation recurrence:

```
dp[idx][...] depends only on dp[idx + 1][...]
```

Row `idx` only ever looks at row `idx + 1`. So instead of the full `(n+1) × (2k+1)` array, keep just **two 1D arrays of size 2k+1** — `after` (representing `idx + 1`, already computed) and `curr` (representing `idx`, being computed).

```java
class Solution {
    public int maxProfit(int k, int[] prices) {
        int n = prices.length;

        // after[transNo] = best profit from day idx+1 onward, given this
        // many actions already taken. Java zero-initializes → base case free.
        int[] after = new int[2 * k + 1];

        for (int idx = n - 1; idx >= 0; idx--) {
            int[] curr = new int[2 * k + 1];

            for (int transNo = 2 * k - 1; transNo >= 0; transNo--) {
                if (transNo % 2 == 0) {
                    // WHY after[transNo+1]: dp[idx+1][transNo+1] — the row
                    // below, one action further along after buying
                    int buy = -prices[idx] + after[transNo + 1];
                    int notBuy = 0 + after[transNo];

                    curr[transNo] = Math.max(buy, notBuy);
                } else {
                    int sell = prices[idx] + after[transNo + 1];
                    int notSell = 0 + after[transNo];

                    curr[transNo] = Math.max(sell, notSell);
                }
            }

            // Slide forward: current row becomes next iteration's "after"
            after = curr;
        }

        // after now holds the last computed row (idx = 0)
        return after[0];
    }
}
```

**Time Complexity — O(n × 2k):**
Same two nested loops, same iteration count as tabulation. No change — **O(n × k)**.

**Space Complexity — O(k):**
No `(n+1) × (2k+1)` array. No recursion stack. Just two arrays of size `2k+1` — `after` and `curr` — completely independent of n.

---

## The Edge Case Explicitly Addressed in the Transcript — Tiny Array, Huge k

A natural worry: what if `prices` has only 3 days, but `k` is passed in as `100`? Does the algorithm break, or waste enormous effort trying to force 100 transactions into 3 days?

**It handles this completely correctly, with no special-casing needed.** The `idx == n` base case fires the moment the array runs out — regardless of how many transactions the budget still theoretically allows. Whatever profit was achievable using however many transactions actually *fit* inside those 3 days gets carried forward correctly; the remaining unused budget simply never gets exercised, because there are no more days left to use it on. The base case at "ran out of days" always takes priority the instant it's reached, exactly the same way it did in every previous stock problem in this pattern.

---

## Final Comparison

| Approach | Time | Space | Notes |
| --- | --- | --- | --- |
| Pure Recursion | O(2^n) | O(n) stack | Exponential — never use |
| Memoization | O(n × k) | O(n×k) + O(n) stack | Good interview starting point |
| Tabulation | O(n × k) | O(n × k) | Better — eliminates stack, no explicit base cases needed |
| Space Optimization | O(n × k) | **O(k)** | Best — submit this |

---

## Formulation A vs. Formulation B — Side by Side

| Property | Formulation A (`canBuy` + `transLimit`) | Formulation B (`transactionNo`) |
| --- | --- | --- |
| State parameters | 3 — `index`, `canBuy`, `transLimit` | **2 — `index`, `transactionNo`** |
| What tells you "buy or sell"? | Explicit `canBuy` flag | **Parity of `transactionNo`** (even = buy, odd = sell) |
| Range of the transaction dimension | `0` to `k` → `k+1` values | **`0` to `2k` → `2k+1` values** |
| When does the counter change? | Only on sell (`transLimit - 1`) | **On EVERY action — buy or sell alike (`transactionNo + 1`)** |
| DP table shape | `(n+1) × 2 × (k+1)` | **`(n+1) × (2k+1)`** |
| Base case for "budget exhausted" | `transLimit == 0` | **`transactionNo == 2k`** |
| Space-optimized shape | Two 2×(k+1) blocks | **Two flat arrays of size 2k+1** |
| Directly reusable from which earlier problem? | LC 123, unchanged except constant→variable | **The "homework" hinted at in LC 123, now fully built** |

Both are asymptotically `O(n·k)` in time and space, and both are completely valid to present in an interview. Formulation A names its two pieces of information transparently (*am I holding? how much budget?*), which makes it the easier one to explain out loud from scratch. Formulation B is more compact — one fewer dimension — once you've internalized *why* parity alone is enough to replace an explicit flag.

---

## The Key Takeaway

```
┌────────────────────────────────────────────────────────────────────────────┐
│  LC 188 is the direct generalization of LC 123 — "at most 2"               │
│  becomes "at most k" — and it can be solved in two equally valid           │
│  ways:                                                                     │
│                                                                            │
│  Formulation A: keep LC 123's exact code, replace the literal 2            │
│  with k everywhere it appears (the initial call and the array size).       │
│  Zero new logic — proof that truly understanding a pattern means           │
│  a generalization costs nothing extra.                                     │
│                                                                            │
│  Formulation B: collapse "canBuy" and "transLimit" into a single           │
│  counter that ticks up by 1 on every action (buy OR sell), where           │
│  parity — even means buy is due, odd means sell is due — replaces          │
│  the explicit canBuy flag entirely.                                        │
│                                                                            │
│  The one bug this collapse invites, and the one thing to watch for:        │
│  the counter tracks ACTIONS, not TRANSACTIONS — there are two              │
│  actions per transaction (one buy, one sell) — so the array must           │
│  be sized 2k, not k. This off-by-a-factor-of-2 error is the exact          │
│  bug hit and fixed live in the lecture.                                    │
│                                                                            │ 
│  Tabulation needs NO explicit base-case-filling code here — every          │
│  base case returns 0, and Java's default zero-initialization               │
│  already provides that for free. This is a small but genuine               │
│  simplification over LC 123's tabulation, which needed two                 │
│  separate explicit base-case loops.                                        │
│                                                                            │
│  Edge case (tiny array, huge k): handled automatically — the               │
│  "ran out of days" base case always fires first, regardless of             │
│  how much unused transaction budget remains. No special-casing             │
│  is ever needed.                                                           │
└────────────────────────────────────────────────────────────────────────────┘
```