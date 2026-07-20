# LC 122. Best Time to Buy and Sell Stock II

Key Concept: Unlimited transactions — greedy OR DP
Solution: https://www.youtube.com/watch?v=nGJmxkUJQGs&ab_channel=takeUforward
Status: Done

# Stage 1: Identification

## **Step 1 — Which topic?**

Same setup as LC 121 — an array of stock prices, one price per day. But now the constraint that made LC 121 simple is gone: instead of a single buy-sell pair, you can **buy and sell as many times as you want**. The only rule is you must **sell your current holding before you can buy again** — no stacking multiple buys on top of each other.

The moment you see **"maximize profit with unlimited transactions, but each buy must be followed by a sell before the next buy"** — that is a very different shape from LC 121. You can no longer just track a running minimum, because the decision to buy or sell today depends on **what you already did** — specifically, whether you're currently holding a stock or not. Whenever the optimal choice today depends on a decision made earlier, and there are many ways to combine those decisions, that's the signal for **recursion — try every possibility, and take the best.**

## **Step 2 — Which pattern?**

This is **Pattern 13: State Machine DP**, and it's the problem where the "state machine" part of the name actually becomes visible in the code. At every day, you are in exactly one of **two states**:

```
State "canBuy = 1"  →  you are NOT holding any stock right now,
                        so your only meaningful choice is: buy today, or wait
State "canBuy = 0"  →  you ARE holding a stock right now,
                        so your only meaningful choice is: sell today, or wait
```

LC 121 never needed to track this explicitly, because only one buy-sell pair was ever allowed. Here, since transactions can repeat, the recursion needs to know **which of the two states it's currently in** before it can decide what moves are even legal.

## **Step 3 — Which key concept?**

**Two states — index and "can I buy right now" — with the state flipping every time a buy or sell happens.**

```
Step 1: Represent in terms of (index, canBuy)
Step 2: Explore all possibilities at (index, canBuy)
        → if canBuy: buy today, OR don't buy today
        → if not canBuy (i.e., holding stock): sell today, OR don't sell today
Step 3: Question says maximize profit → take max of both choices
```

The one new idea Striver introduces here, worth internalizing as the seed for every stock problem that follows: **whenever you buy, subtract the price and flip the state to "cannot buy." Whenever you sell, add the price and flip the state back to "can buy."** This flip is what makes it a genuine state *machine* — two states, and the transitions between them are exactly buy and sell.

---

# Stage 2: Intuition Building

### The Problem Setup

```
prices = [7, 1, 5, 3, 6, 4]
days   =  0  1  2  3  4  5
```

Unlike LC 121, you're not limited to one trade. You could:

```
Buy day 1 (price 1), sell day 2 (price 5) → profit 4
Buy day 3 (price 3), sell day 4 (price 6) → profit 3
Total = 4 + 3 = 7
```

Or any other combination — as long as every buy is followed by a sell before the next buy. The goal: find the combination that maximizes total profit.

### Why "Try Everything" Is the Right First Instinct

At any given day, standing at some index, you genuinely don't know in advance whether buying today, or waiting, leads to a better outcome — it depends on what happens on all the days after it. The only way to be sure is to **try both options and see which one gives more profit**. This is exactly the "pick or not pick" instinct from every earlier DP pattern, applied here to buy/sell decisions instead of array elements.

### Step 1 — Represent in Terms of (index, canBuy)

Define:

```
f(index, canBuy) = maximum profit achievable from day 'index' onward,
                    given that 'canBuy' tells you whether you are
                    currently free to buy (1) or must sell first (0)
```

The answer we want is `f(0, 1)` — starting at day 0, with full freedom to buy (nothing purchased yet).

**Why does `canBuy` need to be a parameter, and not just inferred from the index?** Because the same index can be reached in two completely different situations — once where you're holding a stock, and once where you're not — and those two situations lead to entirely different sets of legal next moves. The index alone cannot tell you which situation you're in; you have to carry that information forward explicitly, exactly the way Ninja's Training needed to carry `last` (the previous day's task) alongside the day index.

### Step 2 — Explore All Possibilities at (index, canBuy)

**Case 1 — `canBuy == 1` (you are not holding anything):**

Your only two meaningful actions are **buy today** or **skip today and keep the freedom to buy on a later day**.

```
Buy today:
    → pay prices[index]     (money leaves your pocket, hence subtract)
    → you are now holding a stock, so canBuy flips to 0
    → buy = -prices[index] + f(index + 1, 0)

Don't buy today:
    → nothing happens, freedom to buy carries forward unchanged
    → notBuy = 0 + f(index + 1, 1)
```

**Case 2 — `canBuy == 0` (you are currently holding a stock):**

Your only two meaningful actions are **sell today** or **skip today and keep holding**.

```
Sell today:
    → receive prices[index]     (money enters your pocket, hence add)
    → you are no longer holding anything, canBuy flips back to 1
    → sell = prices[index] + f(index + 1, 1)

Don't sell today:
    → nothing happens, you're still holding, canBuy stays 0
    → notSell = 0 + f(index + 1, 0)
```

### Step 3 — Take Maximum

Since the goal is to maximize profit, take the better of the two options in whichever case you're in:

```
f(index, canBuy) = max(buy, notBuy)     when canBuy == 1
f(index, canBuy) = max(sell, notSell)   when canBuy == 0
```

### Base Case

**When `index == n` (all days exhausted):**

The array is over — there are no more days to trade on. No matter what `canBuy` is, no more profit can be made from this point forward.

```
if index == n → return 0
```

Note something Striver flags explicitly: if you happen to still be holding a stock when the days run out (`canBuy == 0` at the end), that's fine — it simply means whatever money you spent buying it has already been accounted for as a negative value earlier in the recursion. You don't need a separate penalty here; the cost was already subtracted at the moment of purchase.

### Visualizing the State Flip

```
f(0, 1)   ← day 0, free to buy
   │
   ├── buy: -prices[0] + f(1, 0)   ← now holding, must sell next
   │                    │
   │                    ├── sell: prices[1] + f(2, 1)  ← sold, free again
   │                    └── wait: 0 + f(2, 0)           ← still holding
   │
   └── skip: 0 + f(1, 1)            ← still free to buy
```

Every time a buy happens, the very next call flips to `canBuy = 0`. Every time a sell happens, the very next call flips back to `canBuy = 1`. This alternation is the entire "state machine" — two states, two possible transitions between them.

### Are There Overlapping Subproblems?

The same `(index, canBuy)` pair gets reached through many different sequences of past buy/sell decisions — for instance, `f(3, 1)` could be arrived at by buying-then-selling early, or by skipping every day until index 3. **Overlapping subproblems confirmed** → DP applies.

### DP Table Size

Two parameters:

- `index`: 0 to n → **n+1 values** (including the base case at n)
- `canBuy`: 0 or 1 → **2 values**

dp table: **(n+1) × 2**

---

# Stage 3: Coding

## Approach 1 — Pure Recursion (Brute Force)

```java
class Solution {
    private int func(int idx, int canBuy, int[] prices) {
        // Base case: no more days left — no more profit possible
        if (idx == prices.length) return 0;

        if (canBuy == 1) {
            // Buy today: pay the price now, flip state to "cannot buy"
            int buy = -prices[idx] + func(idx + 1, 1 - canBuy, prices);

            // Don't buy: freedom to buy carries forward unchanged
            int notBuy = 0 + func(idx + 1, canBuy, prices);

            return Math.max(buy, notBuy);
        } else {
            // Sell today: receive the price now, flip state to "can buy"
            int sell = prices[idx] + func(idx + 1, 1 - canBuy, prices);

            // Don't sell: still holding, state stays "cannot buy"
            int notSell = 0 + func(idx + 1, canBuy, prices);

            return Math.max(sell, notSell);
        }
    }

    public int maxProfit(int[] prices) {
        // Start at day 0, with full freedom to buy (canBuy = 1)
        return func(0, 1, prices);
    }
}
```

**Time Complexity — O(2^n):**
At every day, the function makes 2 recursive calls — one for the "act" branch (buy or sell) and one for the "skip" branch. The recursion tree is a binary tree of depth n. Total nodes grow as 2^n. For n in the thousands (a realistic constraint), this is completely impractical.

**Space Complexity — O(n):**
No dp array. The recursion call stack holds one frame per day — the deepest chain goes from day 0 all the way to day n, which is n frames deep. Stack uses O(n) space.

---

## Approach 2 — Memoization (Top-Down DP)

The same state `f(idx, canBuy)` gets reached from many different combinations of earlier buy/sell decisions. Store each result the first time it's computed.

```java
class Solution {
    private int func(int idx, int canBuy, int[] prices, int[][] dp) {
        // Base case: no more days left
        if (idx == prices.length) return 0;

        // Step 1: Already computed? Return stored value instantly
        // WHY: f(idx, canBuy) = best profit from day idx onward with this
        //      state — this never changes no matter which sequence of
        //      earlier decisions led here
        if (dp[idx][canBuy] != -1) return dp[idx][canBuy];

        if (canBuy == 1) {
            int buy = -prices[idx] + func(idx + 1, 1 - canBuy, prices, dp);
            int notBuy = 0 + func(idx + 1, canBuy, prices, dp);

            dp[idx][canBuy] = Math.max(buy, notBuy);
        } else {
            int sell = prices[idx] + func(idx + 1, 1 - canBuy, prices, dp);
            int notSell = 0 + func(idx + 1, canBuy, prices, dp);

            dp[idx][canBuy] = Math.max(sell, notSell);
        }

        // Step 2: Store before returning
        return dp[idx][canBuy];
    }

    public int maxProfit(int[] prices) {
        int n = prices.length;

        // dp[idx][canBuy] = -1 means not yet computed
        int[][] dp = new int[n + 1][2];
        for (int[] row : dp) {
            Arrays.fill(row, -1);
        }

        return func(0, 1, prices, dp);
    }
}
```

**Time Complexity — O(n × 2):**
Each unique `(idx, canBuy)` pair is computed exactly once. There are `(n+1) × 2` unique states. For each state, O(1) work is done — two recursive lookups and a max. Total: **O(n)**.

**Space Complexity — O(n × 2) + O(n):**
Two sources of space. First, the dp array of size `(n+1) × 2`. Second, the recursion call stack, which goes n levels deep. Total: **O(n) + O(n) = O(n)**.

---

## Approach 3 — Tabulation (Bottom-Up DP)

**Direction:** Recursion goes from index 0 **forward** toward index n. So tabulation goes the **opposite direction** — from index n **backward** toward index 0. This is the same "tabulation is always opposite to recursion" rule seen throughout every earlier pattern.

**Base case in tabulation:** `dp[n][0] = 0` and `dp[n][1] = 0` — no matter the state, once the days run out, no more profit is possible.

```java
class Solution {
    public int maxProfit(int[] prices) {
        int n = prices.length;

        int[][] dp = new int[n + 1][2];

        // Base case: no more days left, regardless of state
        dp[n][0] = 0;
        dp[n][1] = 0;

        // Fill from idx = n-1 down to 0 — opposite of recursion's direction
        for (int idx = n - 1; idx >= 0; idx--) {
            for (int canBuy = 1; canBuy >= 0; canBuy--) {
                if (canBuy == 1) {
                    int buy = -prices[idx] + dp[idx + 1][1 - canBuy];
                    int notBuy = 0 + dp[idx + 1][canBuy];

                    dp[idx][canBuy] = Math.max(buy, notBuy);
                } else {
                    int sell = prices[idx] + dp[idx + 1][1 - canBuy];
                    int notSell = 0 + dp[idx + 1][canBuy];

                    dp[idx][canBuy] = Math.max(sell, notSell);
                }
            }
        }

        // Answer: start at day 0, free to buy
        return dp[0][1];
    }
}
```

### Full Cell-by-Cell DP Table Trace

`prices = [7, 1, 5, 3, 6, 4]` (n = 6)

```
                canBuy=0   canBuy=1
idx=6 (base)   [   0    ,     0    ]

idx=5 (price 4):
  canBuy=1: buy = -4 + dp[6][0] = -4,  notBuy = 0 + dp[6][1] = 0  → max = 0
  canBuy=0: sell = 4 + dp[6][1] = 4,   notSell = 0 + dp[6][0] = 0 → max = 4
                [   4    ,     0    ]

idx=4 (price 6):
  canBuy=1: buy = -6 + dp[5][0] = -6+4 = -2,  notBuy = 0 + dp[5][1] = 0 → max = 0
  canBuy=0: sell = 6 + dp[5][1] = 6+0 = 6,    notSell = 0 + dp[5][0] = 4 → max = 6
                [   6    ,     0    ]

idx=3 (price 3):
  canBuy=1: buy = -3 + dp[4][0] = -3+6 = 3,   notBuy = 0 + dp[4][1] = 0 → max = 3
  canBuy=0: sell = 3 + dp[4][1] = 3+0 = 3,    notSell = 0 + dp[4][0] = 6 → max = 6
                [   6    ,     3    ]

idx=2 (price 5):
  canBuy=1: buy = -5 + dp[3][0] = -5+6 = 1,   notBuy = 0 + dp[3][1] = 3 → max = 3
  canBuy=0: sell = 5 + dp[3][1] = 5+3 = 8,    notSell = 0 + dp[3][0] = 6 → max = 8
                [   8    ,     3    ]

idx=1 (price 1):
  canBuy=1: buy = -1 + dp[2][0] = -1+8 = 7,   notBuy = 0 + dp[2][1] = 3 → max = 7
  canBuy=0: sell = 1 + dp[2][1] = 1+3 = 4,    notSell = 0 + dp[2][0] = 8 → max = 8
                [   8    ,     7    ]

idx=0 (price 7):
  canBuy=1: buy = -7 + dp[1][0] = -7+8 = 1,   notBuy = 0 + dp[1][1] = 7 → max = 7
  canBuy=0: sell = 7 + dp[1][1] = 7+7 = 14,   notSell = 0 + dp[1][0] = 8 → max = 14
                [   14   ,     7    ]

Answer = dp[0][1] = 7 ✓
```

Matches the expected answer exactly — buy at day 1 (price 1), sell at day 2 (price 5), buy at day 3 (price 3), sell at day 4 (price 6): `(5-1) + (6-3) = 4 + 3 = 7`.

**Time Complexity — O(n × 2):**
Two nested loops — outer runs n times, inner runs 2 times. Each cell does O(1) work. Total: **O(n)**.

**Space Complexity — O(n × 2):**
Only the dp array of size `(n+1) × 2`. No recursion stack at all.

---

## Approach 4 — Space Optimization (The Final Form)

Look at the tabulation recurrence:

```
dp[idx][...] depends only on dp[idx + 1][...]
```

Row `idx` only ever looks at row `idx + 1`. So instead of the full `(n+1) × 2` array, keep just **two arrays of size 2** — `lastRow` (representing `idx + 1`, already computed) and `currRow` (representing `idx`, being computed).

```java
class Solution {
    public int maxProfit(int[] prices) {
        int n = prices.length;

        // lastRow[canBuy] = best profit from day idx+1 onward, given this state
        int[] lastRow = new int[2];

        // Base case: no more days left, regardless of state
        lastRow[0] = 0;
        lastRow[1] = 0;

        // Fill from idx = n-1 down to 0
        for (int idx = n - 1; idx >= 0; idx--) {
            int[] currRow = new int[2];

            for (int canBuy = 1; canBuy >= 0; canBuy--) {
                if (canBuy == 1) {
                    // WHY lastRow[1 - canBuy]: dp[idx+1][0] — the row below,
                    //     state flipped to "cannot buy" after buying
                    int buy = -prices[idx] + lastRow[1 - canBuy];

                    // WHY lastRow[canBuy]: dp[idx+1][1] — state unchanged
                    int notBuy = 0 + lastRow[canBuy];

                    currRow[canBuy] = Math.max(buy, notBuy);
                } else {
                    int sell = prices[idx] + lastRow[1 - canBuy];
                    int notSell = 0 + lastRow[canBuy];

                    currRow[canBuy] = Math.max(sell, notSell);
                }
            }

            // Slide forward: current row becomes next iteration's "lastRow"
            // WHY: when idx decreases by 1, what was currRow becomes
            //      the row below for the next iteration
            lastRow = currRow;
        }

        // lastRow now holds the last computed row (idx = 0)
        return lastRow[1];
    }
}
```

**Time Complexity — O(n × 2):**
Same two nested loops, same iteration count as tabulation. No change in time complexity — **O(n)**.

**Space Complexity — O(1) (technically O(2)):**
No `(n+1) × 2` array. No recursion stack. Just two arrays of size 2 — `lastRow` and `currRow` — regardless of how large n is. Whether n is 10 or 10 million, memory stays constant.

---

## A Note on the "Four Variable" Version Seen in Discussion Forums

Striver explicitly calls out a very common alternative you'll see in LeetCode discussion forums: instead of using two arrays of size 2, people flatten this into **four separate variables** — something like `aheadNotBuy`, `aheadBuy`, `curNotBuy`, `curBuy`. This is **not a further optimization** — it uses exactly the same amount of memory (four numbers total, same as `2 + 2`). It's purely a stylistic choice: unrolling the `canBuy` loop (which only ever runs for `canBuy = 0` and `canBuy = 1`) into two explicit branches, rather than looping over it. Understanding that these four-variable solutions are doing *the same computation* as the two-array version — just with the inner loop manually unrolled — is what lets you read and recognize them instantly instead of being confused by unfamiliar variable names.

```java
class Solution {
    public int maxProfit(int[] prices) {
        int n = prices.length;

        long aheadNotBuy = 0, aheadBuy = 0;

        for (int idx = n - 1; idx >= 0; idx--) {
            long curNotBuy, curBuy;

            // canBuy == 0 branch (holding stock, must sell or wait)
            curNotBuy = Math.max(prices[idx] + aheadBuy, aheadNotBuy);

            // canBuy == 1 branch (free to buy or wait)
            curBuy = Math.max(-prices[idx] + aheadNotBuy, aheadBuy);

            aheadNotBuy = curNotBuy;
            aheadBuy = curBuy;
        }

        return (int) aheadBuy;
    }
}
```

---

## Final Comparison

| Approach | Time | Space | Notes |
| --- | --- | --- | --- |
| Pure Recursion | O(2^n) | O(n) stack | Exponential — never use |
| Memoization | O(n) | O(n) + O(n) stack | Good interview starting point |
| Tabulation | O(n) | O(n) | Better — eliminates stack |
| Space Optimization | O(n) | **O(1)** | Best — submit this |

---

## How This Differs From LC 121

| Property | LC 121 (One Transaction) | LC 122 (Unlimited Transactions) |
| --- | --- | --- |
| Number of buy-sell pairs allowed | Exactly 1 | **Unlimited** |
| State needed | None — single running minimum | **`canBuy` — are you currently holding stock?** |
| Approach | Single forward pass, O(1) extra space directly | **Recursion → Memo → Tabulation → Space Opt** |
| Why LC 121's trick doesn't extend | Only works because there's just one trade to optimize | **Multiple trades interact — a greedy running-min can't account for the state flip between buy and sell** |
| DP table shape | None needed | **(n+1) × 2** |

---

## The Key Takeaway

```
┌────────────────────────────────────────────────────────────────────┐
│  LC 122 introduces the true "state machine" of DP on Stocks:       │
│                                                                    │
│  Two states at every day:                                          │
│      canBuy = 1  →  free to buy (not holding anything)             │
│      canBuy = 0  →  must sell first (currently holding a stock)    │
│                                                                    │
│  Buying: -prices[idx] + f(idx+1, 0)   ← state flips to "cannot"    │
│  Selling: +prices[idx] + f(idx+1, 1)  ← state flips to "can"       │
│  Waiting: state stays exactly the same, move to idx+1              │
│                                                                    │
│  This buy/sell state flip — subtract and flip to 0 on buy,         │
│  add and flip to 1 on sell — is the CORE mechanism that every      │
│  remaining problem in Pattern 13 builds on. Cooldown adds a        │
│  "wait one extra day after selling" rule to this same flip.        │
│  Transaction fee subtracts a constant at the moment of selling.    │
│  III and IV add a THIRD dimension — how many transactions are      │
│  left — on top of this exact two-state skeleton.                   │
│                                                                    │
│  Base case: index == n → return 0, regardless of state.            │
│  If still holding a stock at the end, its cost was already         │
│  paid for (as a negative) at the moment of purchase — no extra     │
│  penalty needed.                                                   │
│                                                                    │
│  Space optimization: dp[idx] only depends on dp[idx+1], so         │
│  collapse the (n+1)×2 table to two arrays of size 2. The           │
│  "four variable" forum solutions are the same O(1) space,          │
│  just with the size-2 loop manually unrolled into two branches.    │
└────────────────────────────────────────────────────────────────────┘
```