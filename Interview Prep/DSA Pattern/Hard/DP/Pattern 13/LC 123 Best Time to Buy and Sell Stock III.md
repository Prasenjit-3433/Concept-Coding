# LC 123. Best Time to Buy and Sell Stock III

Key Concept: Exactly 2 transactions — 4 states
Solution: https://www.youtube.com/watch?v=-uQGzhYj8BQ&ab_channel=takeUforward
Status: Done

# Stage 1: Identification

**Step 1 — Which topic?**

Same setup as LC 121 and LC 122 — an array of stock prices, one price per day. But now a new constraint sits on top of LC 122's "unlimited transactions" rule: you are allowed **at most two transactions**. Not zero, not one, not unlimited — **at most two**, and each transaction still means a full buy-then-sell pair, with the same rule as before that you cannot hold more than one share at a time.

The moment you see **"maximize profit with a limited number of transactions"** — this should immediately remind you of a **budget** or a **cap**, the same flavor of constraint you've seen in Knapsack problems ("you can only carry so much weight," "you can only use k coins"). Whenever a problem places a hard numeric limit on how many times you're allowed to repeat an action, that limit has to become **part of the state** — you cannot make a correct decision today without knowing how much of your budget is still left.

**Step 2 — Which pattern?**

Still **Pattern 13: State Machine DP**. LC 122 already built the two-state machine — `canBuy = 1` (free to buy) and `canBuy = 0` (must sell first). LC 123 doesn't throw that machine away — it **extends** it by adding a third piece of tracked information: **how many transactions do I still have permission to complete?**

**Step 3 — Which key concept?**

**Add a "transactions remaining" counter as a third state parameter — and decrement it exactly once per completed transaction, at the moment of selling.**

```
Step 1: Represent in terms of (index, canBuy, transLimit)
Step 2: Explore all possibilities at (index, canBuy, transLimit)
        → if canBuy: buy today, OR don't buy today
        → if not canBuy (holding stock): sell today, OR don't sell today
Step 3: Question says maximize profit → take max of both choices
```

The one genuinely new idea here, and it's worth being precise about it: **a transaction is not "used up" the moment you buy — it's used up the moment you sell.** Buying alone is an incomplete transaction; only the buy-sell *pair together* counts as one. So the `transLimit` counter only ever decreases inside the "sell" branch, never inside the "buy" branch. This single placement decision is the entire new mechanic in this problem — everything else is copy-pasted directly from LC 122's two-state machine.

---

# Stage 2: Intuition Building

### The Problem Setup

```
prices = [3, 3, 5, 0, 0, 3, 1, 4]
days   =  0  1  2  3  4  5  6  7
```

If transactions were **unlimited** (LC 122's rule), you'd grab every single upward zig-zag you could find — buy at every local minimum, sell at every local maximum before the next dip, and stack as many of these as the array allows. But here, you're capped at **two** transactions total, no more. So you have to be selective — you want the **two** buy-sell pairs that, combined, give the largest possible total profit.

```
Buy day 3 (price 0), sell day 5 (price 3)  → profit 3
Buy day 6 (price 1), sell day 7 (price 4)  → profit 3

Total = 3 + 3 = 6
```

Could you have done better with only one transaction? The best single trade here is buy day 3 or 4 (price 0), sell day 7 (price 4) → profit 4 — worse than 6. Could a *different* pairing of two transactions beat 6? Try buy day 0 (price 3), sell day 2 (price 5) → profit 2, then buy day 3 (price 0), sell day 7 (price 4) → profit 4, total = 6 as well. Either way, **6** is the ceiling — and notice you cannot just greedily grab *every* zig-zag the way LC 122 would, because LC 122's greedy path would want a *third* trade too (buy day 4, sell day 5 is already used up above; there's also a small dip around day 6) — but you're not allowed a third one. **Something has to be sacrificed**, and figuring out which zig-zags to keep and which to drop is exactly why this needs recursion, not greedy.

### Why LC 122's Greedy Instinct Breaks Down Here

LC 122 never needed to compare "should I take this trade or skip it in favor of a better one later" — every profitable zig-zag was worth taking, full stop, because there was no limit on how many you could take. The moment you introduce a cap, that stops being true: taking a *small* profitable trade now might cost you the "budget" you needed for a *much bigger* trade later. You genuinely cannot know in advance which trades to keep without trying out the possibilities — which is exactly the recursive "try both options, take the best" instinct from every DP pattern so far.

### Step 1 — Represent in Terms of (index, canBuy, transLimit)

Define:

```
f(index, canBuy, transLimit) = maximum profit achievable from day 'index' onward,
                                given that 'canBuy' tells you whether you're
                                currently free to buy (1) or must sell first (0),
                                and 'transLimit' tells you how many MORE complete
                                transactions you're still allowed to make
```

The answer we want is `f(0, 1, 2)` — start at day 0, free to buy, with a budget of 2 transactions remaining.

**Why does `transLimit` need to be a parameter, and not just something checked once at the very end?** Because the decision to buy or sell *today* has to account for how much budget is left — if you're already down to 0 remaining transactions, buying today would be pointless (you'd never be allowed to complete the sell that follows it), and the recursion needs to know this *before* it explores that branch, not after.

### Step 2 — Explore All Possibilities at (index, canBuy, transLimit)

This part is **identical to LC 122** — the only new territory is where `transLimit` gets touched.

**Case 1 — `canBuy == 1` (not holding anything):**

```
Buy today:
    → pay prices[index], flip canBuy to 0 (now holding)
    → transLimit UNCHANGED — buying alone does not complete a transaction
    → buy = -prices[index] + f(index + 1, 0, transLimit)

Don't buy today:
    → notBuy = 0 + f(index + 1, 1, transLimit)
```

**Case 2 — `canBuy == 0` (currently holding a stock):**

```
Sell today:
    → receive prices[index], flip canBuy back to 1 (free again)
    → transLimit DECREASES BY 1 — the buy-sell pair is now complete
    → sell = prices[index] + f(index + 1, 1, transLimit - 1)

Don't sell today:
    → notSell = 0 + f(index + 1, 0, transLimit)
```

### Step 3 — Take Maximum

```
f(index, canBuy, transLimit) = max(buy, notBuy)     when canBuy == 1
f(index, canBuy, transLimit) = max(sell, notSell)   when canBuy == 0
```

### Why the Decrement Belongs on Sell, Not Buy

This is worth dwelling on, because it's the one place a wrong instinct could sneak in. A "transaction," by the problem's own definition, is a *complete* buy-then-sell pair. If you decremented `transLimit` at the moment of buying, you would be spending your budget before you've actually finished anything — and worse, you could end up in a state where `transLimit` hits 0 while you're still *holding* a stock with nowhere left to sell it, which makes no sense against the problem's own accounting. Decrementing on the **sell** instead guarantees that `transLimit` only ever drops at the exact moment a transaction is *actually completed* — which is the only point where "one transaction has been used" is factually true.

### Base Cases

**When `index == n` (all days exhausted):**

```
if index == n → return 0
```

No more days left to trade on — no matter what `canBuy` or `transLimit` are, no further profit is possible. Exactly the same reasoning as LC 122.

**When `transLimit == 0` (budget exhausted):**

```
if transLimit == 0 → return 0
```

This is the genuinely new base case. If you have zero transactions left to complete, it doesn't matter how many days remain or whether you're currently holding stock — you are not permitted to sell anything more (selling would be the 3rd transaction), so no more profit is achievable from here. Notice this mirrors exactly how LC 121's single-transaction cap worked, just generalized to a counter instead of a hardcoded "1."

### Visualizing the Extra Dimension

```
f(0, 1, 2)   ← day 0, free to buy, 2 transactions left
   │
   ├── buy: -prices[0] + f(1, 0, 2)      ← transLimit UNCHANGED after buying
   │                    │
   │                    ├── sell: prices[1] + f(2, 1, 1)   ← transLimit DROPS to 1
   │                    └── wait: 0 + f(2, 0, 2)      ← still holding, budget unchanged
   │
   └── skip: 0 + f(1, 1, 2)               ← still free to buy, budget unchanged
```

Every buy leaves `transLimit` untouched. Every sell knocks it down by exactly one. The recursion naturally stops exploring any deeper once `transLimit` reaches 0 — that branch is capped off by the base case, exactly like running out of days does.

### Are There Overlapping Subproblems?

The same `(index, canBuy, transLimit)` triple gets reached through many different sequences of earlier buy/sell/skip decisions — for instance, `f(4, 1, 1)` could be arrived at by completing one full transaction early and skipping the rest, or by several different combinations of skips. **Overlapping subproblems confirmed** → DP applies.

### DP Table Size

Three parameters:

- `index`: 0 to n → **n+1 values**
- `canBuy`: 0 or 1 → **2 values**
- `transLimit`: 0, 1, or 2 → **3 values**

dp table: **(n+1) × 2 × 3**

This is a genuine 3D DP — the same style of extension you've already seen once before, when Ninja's Training added a `last` parameter on top of `day`, and again when LC 122 added `canBuy` on top of `index`. The recipe never changes: whenever a new constraint depends on "something I need to remember from earlier," it becomes a new axis of the state.

---

# Stage 3: Coding

---

## Approach 1 — Pure Recursion (Brute Force)

Direct translation of the recurrence built in Stage 2 — copy LC 122's two-state machine exactly, and add `transAllowed` as a third parameter, decremented only inside the sell branch.

```java
class Solution {
    private int func(int idx, int canBuy, int transAllowed, int[] prices) {
        // Base case 1: no more days left — no more profit possible
        if (idx == prices.length) return 0;

        // Base case 2: budget of transactions exhausted — cannot sell
        // anything more, no matter how many days remain
        // WHY this is new compared to LC 122: LC 122 never had a budget,
        // so this check simply didn't exist there
        if (transAllowed == 0) return 0;

        if (canBuy == 1) {
            // Buy today: pay the price, flip state to "cannot buy"
            // WHY transAllowed UNCHANGED here: buying alone does not
            // complete a transaction — only the matching sell does
            int buy = -prices[idx] + func(idx + 1, 1 - canBuy, transAllowed, prices);

            // Don't buy: freedom to buy carries forward, budget unchanged
            int notBuy = 0 + func(idx + 1, canBuy, transAllowed, prices);

            return Math.max(buy, notBuy);
        } else {
            // Sell today: receive the price, flip state to "can buy"
            // WHY transAllowed - 1 here: this sell COMPLETES a
            // buy-sell pair — exactly one transaction has now been used
            int sell = prices[idx] + func(idx + 1, 1 - canBuy, transAllowed - 1, prices);

            // Don't sell: still holding, budget unchanged
            int notSell = 0 + func(idx + 1, canBuy, transAllowed, prices);

            return Math.max(sell, notSell);
        }
    }

    public int maxProfit(int[] prices) {
        // Start at day 0, free to buy, budget of 2 complete transactions
        return func(0, 1, 2, prices);
    }
}
```

**Time Complexity — O(2^n):**
At every day, the function makes 2 recursive calls — one for the "act" branch (buy or sell) and one for the "skip" branch. The recursion tree is a binary tree of depth n (the `transAllowed` cap only ever prunes branches early, it doesn't change the worst-case branching factor). Total nodes grow as 2^n. Completely impractical for realistic n.

**Space Complexity — O(n):**
No dp array. The recursion call stack holds one frame per day — the deepest chain goes from day 0 to day n, which is n frames deep. Stack uses O(n) space.

---

## Approach 2 — Memoization (Top-Down DP)

The same state `f(idx, canBuy, transAllowed)` gets reached from many different combinations of earlier buy/sell/skip decisions. Store each result the first time it's computed.

```java
class Solution {
    private int func(int idx, int canBuy, int transLimit, int[] prices, int[][][] dp) {
        // Base cases — identical to pure recursion
        if (idx == prices.length) return 0;
        if (transLimit == 0) return 0;

        // Step 1: Already computed? Return stored value instantly
        // WHY: f(idx, canBuy, transLimit) = best profit from day idx
        //      onward with this exact state — never changes no matter
        //      which sequence of earlier decisions led here
        if (dp[idx][canBuy][transLimit] != -1) return dp[idx][canBuy][transLimit];

        if (canBuy == 1) {
            int buy = -prices[idx] + func(idx + 1, 1 - canBuy, transLimit, prices, dp);
            int notBuy = 0 + func(idx + 1, canBuy, transLimit, prices, dp);

            dp[idx][canBuy][transLimit] = Math.max(buy, notBuy);
        } else {
            int sell = prices[idx] + func(idx + 1, 1 - canBuy, transLimit - 1, prices, dp);
            int notSell = 0 + func(idx + 1, canBuy, transLimit, prices, dp);

            dp[idx][canBuy][transLimit] = Math.max(sell, notSell);
        }

        // Step 2: Store before returning
        return dp[idx][canBuy][transLimit];
    }

    public int maxProfit(int[] prices) {
        int n = prices.length;

        // dp[idx][canBuy][transLimit] = -1 means not yet computed
        // WHY size 3 for the last dimension: transLimit can be 0, 1, or 2
        int[][][] dp = new int[n + 1][2][3];
        for (int[][] mat : dp) {
            for (int[] row : mat) {
                Arrays.fill(row, -1);
            }
        }

        return func(0, 1, 2, prices, dp);
    }
}
```

**Time Complexity — O(n × 2 × 3):**
Each unique `(idx, canBuy, transLimit)` triple is computed exactly once. There are `(n+1) × 2 × 3` unique states. For each state, O(1) work is done — two recursive lookups and a max. Total: **O(n)** (since 2×3 is a constant factor).

**Space Complexity — O(n × 2 × 3) + O(n):**
Two sources of space. First, the dp array of size `(n+1) × 2 × 3`. Second, the recursion call stack, which goes n levels deep. Total: **O(n) + O(n) = O(n)**.

---

## Approach 3 — Tabulation (Bottom-Up DP)

**Direction:** Recursion goes from index 0 **forward** toward index n. So tabulation goes the **opposite direction** — from index n **backward** toward index 0. Same rule as LC 122, just with a third loop for `transLimit` nested in.

**Base cases in tabulation:**

```
idx == n           →  dp[n][canBuy][transLimit] = 0  for every canBuy, transLimit
transLimit == 0    →  dp[idx][canBuy][0] = 0          for every idx, canBuy
```

```java
class Solution {
    public int maxProfit(int[] prices) {
        int n = prices.length;

        int[][][] dp = new int[n + 1][2][3];

        // Base case 1: no more days left — regardless of state
        for (int canBuy = 0; canBuy < 2; canBuy++) {
            for (int transLimit = 0; transLimit < 3; transLimit++) {
                dp[n][canBuy][transLimit] = 0;
            }
        }

        // Base case 2: budget exhausted — regardless of day or canBuy
        for (int idx = n; idx >= 0; idx--) {
            for (int canBuy = 0; canBuy < 2; canBuy++) {
                dp[idx][canBuy][0] = 0;
            }
        }

        // Fill from idx = n-1 down to 0 — opposite of recursion's direction
        for (int idx = n - 1; idx >= 0; idx--) {
            for (int canBuy = 0; canBuy < 2; canBuy++) {
                // WHY transLimit starts at 1: transLimit = 0 is already
                // fully handled by base case 2 above
                for (int transLimit = 1; transLimit < 3; transLimit++) {
                    if (canBuy == 1) {
                        int buy = -prices[idx] + dp[idx + 1][1 - canBuy][transLimit];
                        int notBuy = 0 + dp[idx + 1][canBuy][transLimit];

                        dp[idx][canBuy][transLimit] = Math.max(buy, notBuy);
                    } else {
                        int sell = prices[idx] + dp[idx + 1][1 - canBuy][transLimit - 1];
                        int notSell = 0 + dp[idx + 1][canBuy][transLimit];

                        dp[idx][canBuy][transLimit] = Math.max(sell, notSell);
                    }
                }
            }
        }

        // Answer: start at day 0, free to buy, full budget of 2 transactions
        return dp[0][1][2];
    }
}
```

### Full Cell-by-Cell DP Table Trace

`prices = [7, 1, 5, 3, 6, 4]` (n = 6) — same array used for LC 121 and LC 122, so you can directly compare how the answer grows as the transaction budget increases: LC 121 (1 transaction) gave **5**, LC 122 (unlimited) gave **7**, and here (2 transactions) we should also land on **7** — confirming that 2 transactions is already enough to capture the full unlimited-transaction profit on this particular array.

```
Base case — idx = 6 (all states):
[canBuy=0][t=0]=0  [canBuy=0][t=1]=0  [canBuy=0][t=2]=0
[canBuy=1][t=0]=0  [canBuy=1][t=1]=0  [canBuy=1][t=2]=0
```

**idx = 5 (price 4):**

```
t=1: canBuy=1 → buy=-4+dp[6][0][1]=-4,  notBuy=dp[6][1][1]=0  → max=0
     canBuy=0 → sell=4+dp[6][1][0]=4,   notSell=dp[6][0][1]=0 → max=4
t=2: canBuy=1 → buy=-4+dp[6][0][2]=-4,  notBuy=dp[6][1][2]=0  → max=0
     canBuy=0 → sell=4+dp[6][1][1]=4,   notSell=dp[6][0][2]=0 → max=4

dp[5]:  canBuy=0 → [t1=4, t2=4]     canBuy=1 → [t1=0, t2=0]
```

**idx = 4 (price 6):**

```
t=1: canBuy=1 → buy=-6+dp[5][0][1]=-6+4=-2, notBuy=dp[5][1][1]=0 → max=0
     canBuy=0 → sell=6+dp[5][1][0]=6+0=6,   notSell=dp[5][0][1]=4 → max=6
t=2: canBuy=1 → buy=-6+dp[5][0][2]=-6+4=-2, notBuy=dp[5][1][2]=0 → max=0
     canBuy=0 → sell=6+dp[5][1][1]=6+0=6,   notSell=dp[5][0][2]=4 → max=6

dp[4]:  canBuy=0 → [t1=6, t2=6]     canBuy=1 → [t1=0, t2=0]
```

**idx = 3 (price 3):**

```
t=1: canBuy=1 → buy=-3+dp[4][0][1]=-3+6=3,  notBuy=dp[4][1][1]=0 → max=3
     canBuy=0 → sell=3+dp[4][1][0]=3+0=3,   notSell=dp[4][0][1]=6 → max=6
t=2: canBuy=1 → buy=-3+dp[4][0][2]=-3+6=3,  notBuy=dp[4][1][2]=0 → max=3
     canBuy=0 → sell=3+dp[4][1][1]=3+0=3,   notSell=dp[4][0][2]=6 → max=6

dp[3]:  canBuy=0 → [t1=6, t2=6]     canBuy=1 → [t1=3, t2=3]
```

**idx = 2 (price 5):**

```
t=1: canBuy=1 → buy=-5+dp[3][0][1]=-5+6=1,  notBuy=dp[3][1][1]=3 → max=3
     canBuy=0 → sell=5+dp[3][1][0]=5+0=5,   notSell=dp[3][0][1]=6 → max=6
t=2: canBuy=1 → buy=-5+dp[3][0][2]=-5+6=1,  notBuy=dp[3][1][2]=3 → max=3
     canBuy=0 → sell=5+dp[3][1][1]=5+3=8,   notSell=dp[3][0][2]=6 → max=8
                                              ↑ this is where the SECOND
                                                transaction's profit starts
                                                accumulating on top of the first
dp[2]:  canBuy=0 → [t1=6, t2=8]     canBuy=1 → [t1=3, t2=3]
```

**idx = 1 (price 1):**

```
t=1: canBuy=1 → buy=-1+dp[2][0][1]=-1+6=5,  notBuy=dp[2][1][1]=3 → max=5
     canBuy=0 → sell=1+dp[2][1][0]=1+0=1,   notSell=dp[2][0][1]=6 → max=6
t=2: canBuy=1 → buy=-1+dp[2][0][2]=-1+8=7,  notBuy=dp[2][1][2]=3 → max=7
     canBuy=0 → sell=1+dp[2][1][1]=1+3=4,   notSell=dp[2][0][2]=8 → max=8

dp[1]:  canBuy=0 → [t1=6, t2=8]     canBuy=1 → [t1=5, t2=7]
```

**idx = 0 (price 7):**

```
t=1: canBuy=1 → buy=-7+dp[1][0][1]=-7+6=-1, notBuy=dp[1][1][1]=5 → max=5
     canBuy=0 → sell=7+dp[1][1][0]=7+0=7,   notSell=dp[1][0][1]=6 → max=7
t=2: canBuy=1 → buy=-7+dp[1][0][2]=-7+8=1,  notBuy=dp[1][1][2]=7 → max=7
     canBuy=0 → sell=7+dp[1][1][1]=7+5=12,  notSell=dp[1][0][2]=8 → max=12

dp[0]:  canBuy=0 → [t1=7, t2=12]    canBuy=1 → [t1=5, t2=7]
```

**Answer = `dp[0][1][2] = 7`** ✓ — matches the expected profit exactly: buy day 1 (price 1), sell day 2 (price 5) for a profit of 4, then buy day 3 (price 3), sell day 4 (price 6) for a profit of 3, giving `4 + 3 = 7`.

**Time Complexity — O(n × 2 × 3):**
Three nested loops — outer runs n times, middle runs 2 times, inner runs 3 times. Each cell does O(1) work. Total: **O(n)**, since 2 × 3 is a constant factor.

**Space Complexity — O(n × 2 × 3):**
Only the dp array of size `(n+1) × 2 × 3`. No recursion stack at all.

---

## Approach 4 — Space Optimization (The Final Form)

Look at the tabulation recurrence:

```
dp[idx][...][...] depends only on dp[idx + 1][...][...]
```

Row `idx` only ever looks at row `idx + 1`. So instead of the full `(n+1) × 2 × 3` array, keep just **two 2D arrays of size 2 × 3** — `lastRow` (representing `idx + 1`, already computed) and `currRow` (representing `idx`, being computed). This is exactly the same collapse as LC 122's `(n+1) × 2` array shrinking to two arrays of size 2 — here the "row" is simply a 2×3 block instead of a 2-element block.

```java
class Solution {
    public int maxProfit(int[] prices) {
        int n = prices.length;

        // lastRow[canBuy][transLimit] = best profit from day idx+1 onward,
        // given this state
        int[][] lastRow = new int[2][3];

        // Base case: no more days left, regardless of state
        // (Java already zero-initializes, written here for clarity)
        for (int canBuy = 0; canBuy < 2; canBuy++) {
            for (int transLimit = 0; transLimit < 3; transLimit++) {
                lastRow[canBuy][transLimit] = 0;
            }
        }

        for (int idx = n - 1; idx >= 0; idx--) {
            int[][] currRow = new int[2][3];

            for (int canBuy = 0; canBuy < 2; canBuy++) {
                // transLimit = 0 stays 0 by default (Java's zero-init),
                // exactly matching base case 2 from tabulation
                for (int transLimit = 1; transLimit < 3; transLimit++) {
                    if (canBuy == 1) {
                        // WHY lastRow[1-canBuy][transLimit]: dp[idx+1][0][transLimit]
                        //     — the row below, state flipped after buying,
                        //     budget unchanged (buy doesn't complete a transaction)
                        int buy = -prices[idx] + lastRow[1 - canBuy][transLimit];

                        int notBuy = 0 + lastRow[canBuy][transLimit];

                        currRow[canBuy][transLimit] = Math.max(buy, notBuy);
                    } else {
                        // WHY lastRow[1-canBuy][transLimit-1]: this sell
                        //     completes a transaction — budget drops by 1
                        //     in the state we read from
                        int sell = prices[idx] + lastRow[1 - canBuy][transLimit - 1];

                        int notSell = 0 + lastRow[canBuy][transLimit];

                        currRow[canBuy][transLimit] = Math.max(sell, notSell);
                    }
                }
            }

            // Slide forward: current row becomes next iteration's "lastRow"
            lastRow = currRow;
        }

        // lastRow now holds the last computed row (idx = 0)
        return lastRow[1][2];
    }
}
```

**Time Complexity — O(n × 2 × 3):**
Same three nested loops, same iteration count as tabulation. No change — **O(n)**.

**Space Complexity — O(1) (technically O(6)):**
No `(n+1) × 2 × 3` array. No recursion stack. Just two 2×3 arrays — `lastRow` and `currRow` — regardless of how large n is. Whether n is 10 or 10 million, memory stays constant.

---

## The Homework Alternative — "Transaction Number" Instead of "canBuy + transLimit" (n × 4)

![image.png](LC%20123%20Best%20Time%20to%20Buy%20and%20Sell%20Stock%20III/image.png)

Striver explicitly flags an alternative formulation used in many discussion-forum solutions, and is honest that he finds it **less intuitive** and doesn't personally teach it as the primary approach — but it's worth understanding so you can recognize it instantly rather than being confused by it.

**The idea:** instead of carrying two separate pieces of state (`canBuy` and `transLimit`), collapse them into a **single transaction-number counter** that ticks up by exactly 1 every time you act (buy or sell):

```
transaction number 0  →  first buy   (**even** → buy)
transaction number 1  →  first sell  (odd → sell)
transaction number 2  →  second buy  (**even** → buy)
transaction number 3  →  second sell (odd → sell)
```

Since at most 2 transactions means 4 total actions (buy, sell, buy, sell), this transaction number ranges from `0` to `4` — and once it reaches `4`, all transactions are complete, giving a natural base case identical in spirit to `transLimit == 0`.

```
f(idx, transactionNo):
    if idx == n OR transactionNo == 4:  return 0

    if transactionNo is even:   // this is a "buy" step
        buy   = -prices[idx] + f(idx+1, transactionNo+1)
        notBuy = 0 + f(idx+1, transactionNo)
        return max(buy, notBuy)
    else:                        // this is a "sell" step
        sell    = prices[idx] + f(idx+1, transactionNo+1)
        notSell = 0 + f(idx+1, transactionNo)
        return max(sell, notSell)
```

This state space is `n × 4` — a flat single counter instead of the `2 × 3` combination — and it computes the exact same answer. The reason Striver doesn't lead with this: it hides *why* the recurrence works behind a slightly arbitrary "even means buy, odd means sell" encoding, whereas `canBuy` + `transLimit` names the two pieces of information (*am I holding stock?* and *how much budget is left?*) directly and transparently. Both are valid and both would be accepted in an interview — but the `canBuy`/`transLimit` version is the one that generalizes cleanly to LC 188 (at most **k** transactions) without needing any reinterpretation, since `transLimit` already *is* the general k, whereas the transaction-number trick would need to be re-derived for an arbitrary k.

*(As a self-check exercise: try writing the memoization → tabulation → space optimization for this n×4 version yourself — the structure is a direct mechanical translation of everything already built above, just with one fewer dimension.)*

---

## Final Comparison

| Approach | Time | Space | Notes |
| --- | --- | --- | --- |
| Pure Recursion | O(2^n) | O(n) stack | Exponential — never use |
| Memoization | O(n) | O(n) + O(n) stack | Good interview starting point |
| Tabulation | O(n) | O(n) | Better — eliminates stack |
| Space Optimization | O(n) | **O(1)** | Best — submit this |

*(All "O(n)" entries here hide a constant factor of 2×3=6 from the `canBuy`/`transLimit` dimensions — negligible next to n.)*

---

## How This Differs From LC 121 and LC 122

| Property | LC 121 (1 transaction) | LC 122 (unlimited) | LC 123 (at most 2) |
| --- | --- | --- | --- |
| State needed | None — running minimum | `canBuy` only | **`canBuy` AND `transLimit`** |
| Extra base case | — | — | **`transLimit == 0` → return 0** |
| When does budget decrease | — | — | **Only on the sell branch, never on buy** |
| DP table shape | None | `(n+1) × 2` | **`(n+1) × 2 × 3`** |
| Generalizes to "at most k"? | No | No (k is fixed at ∞) | **Yes — directly becomes LC 188 by replacing `2` with `k`** |

---

## The Key Takeaway

```
┌──────────────────────────────────────────────────────────────────────────┐
│  LC 123 = LC 122's exact two-state machine (canBuy) + ONE new            │
│  dimension: a transactions-remaining counter.                            │
│                                                                          │
│  The single rule that makes this work correctly:                         │
│      Buying:  transLimit stays UNCHANGED                                 │
│      Selling: transLimit decreases by exactly 1                          │
│                                                                          │
│  WHY the decrement lives on sell, not buy: a "transaction" is            │
│  defined as a COMPLETE buy-sell pair. Buying alone is only half          │
│  of one — charging the budget at buy-time would let you run out          │
│  of budget while still holding stock with nowhere to sell it,            │
│  which contradicts the problem's own accounting.                         │
│                                                                          │
│  New base case: transLimit == 0 → return 0, regardless of day            │
│  or holding state. This generalizes LC 121's implicit "you get           │
│  exactly 1 transaction" into an explicit, checkable counter.             │
│                                                                          │
│  Everything else — the buy/sell transition logic, the direction          │
│  of tabulation (opposite of recursion), the two-rolling-array            │
│  space optimization — is copy-pasted directly from LC 122, just          │
│  with one extra loop dimension threaded through consistently.            │
│                                                                          │
│  Why this matters going forward: this exact `canBuy` + `transLimit`      │
│  skeleton is what LC 188 (Best Time to Buy and Sell Stock IV)            │
│  reuses completely unchanged — the only difference there is that         │
│  the hardcoded "2" becomes a variable "k" passed into the function.      │
│  Master this state shape once, and LC 188 requires zero new              │
│  thinking, only a generalized cap.                                       │
└──────────────────────────────────────────────────────────────────────────┘
```