# LC 121. Best Time to Buy and Sell Stock

Key Concept: Trivial — one transaction, baseline
Solution: https://www.youtube.com/watch?v=excAOvwF_Wk&ab_channel=takeUforward
Status: Done

# LC 121. Best Time to Buy and Sell Stock

---

# Stage 1: Identification

**Step 1 — Which topic?**

You are given an array where each index represents a day, and the value at that index is the stock price on that day. You must choose **one day to buy** and a **later day to sell**, and maximize the profit. You are only allowed **one transaction** — one buy, one sell, in that order.

The moment you see **"maximize profit from a single buy-sell pair, sell must come after buy"** — this looks like it wants you to check every possible pair of days. Checking every pair is the brute force instinct. But watch closely — the moment you fix a **selling day**, the best possible buying day is completely determined: it's whichever day *before* it had the **lowest price**. That single realization — *"the optimal decision today depends on something you can carry forward from the past"* — is what pulls this into **Dynamic Programming**, even though the final code won't look like a classic DP table.

**Step 2 — Which pattern?**

This is the entry point into **Pattern 13: State Machine DP**. At every day, you are conceptually in one of two states — *you're holding a stock* (because you bought it earlier) or *you're not holding any stock*. The whole DP-on-Stocks pattern is about tracking these states as you scan through the days. LC 121 is the simplest possible version of this: only one buy and one sell are allowed, ever, so the "state machine" barely needs to branch — but the seed of the idea (**carry forward information about the best decision so far**) is exactly what every harder stock problem in this pattern builds on.

**Step 3 — Which key concept?**

**Track the minimum price seen so far — remembering the past is the DP.**

```
Step 1: Fix the day you plan to SELL on.
Step 2: The best BUY day for that sell day is whichever earlier day
        had the minimum price.
Step 3: Question says maximize profit → keep a running maximum
        of (today's price − minimum price seen before today).
```

Striver's own framing is worth holding onto directly: *"Dynamic programming is nothing but remembering the past."* Here, what you remember is a single number — the minimum price encountered so far — and that one number is enough to make the optimal decision at every single day without ever looking backward again.

---

# Stage 2: Intuition Building

### The Problem Setup

```
prices = [7, 1, 5, 3, 6, 4]
days   =  0  1  2  3  4  5
```

You need to pick a buy day and a later sell day such that `price[sell] - price[buy]` is as large as possible.

### Why Checking Every Pair Feels Natural First

The most direct way to think about this: for every possible **selling day**, ask *"what is the best day before it that I could have bought on?"* Since you want to maximize `sell price − buy price`, and the sell price is fixed once you pick that day, you want the buy price to be as **small as possible** among all days that come before it.

```
If selling on day 5 (price 4): best buy day is whichever of days 0–4
                                had the lowest price → day 1 (price 1)
                                profit = 4 - 1 = 3

If selling on day 4 (price 6): best buy day among days 0–3 → day 1 (price 1)
                                profit = 6 - 1 = 5   ← this turns out best

If selling on day 2 (price 5): best buy day among days 0–1 → day 1 (price 1)
                                profit = 5 - 1 = 4
```

Notice something important here: buying on day 0 (price 7) is **never** the right choice for any selling day — it's the most expensive day to buy on. Buying on day 1 (price 1) is always at least as good as buying on day 0, no matter which day you sell on.

### The Realization That Kills the Need to Check Every Pair

If, for every day, you already know **the smallest price seen on any day before it**, you don't need to look backward at all — you can just compute `today's price − minimum price so far` in a single forward pass, and keep the best one you've seen.

This is exactly the same shift in thinking as every other DP problem you've solved: instead of asking *"let me search everything before this point every single time,"* you ask *"what is the one small piece of information from the past that I need to carry forward, so I never have to search again?"* Here that one small piece of information is a single running minimum.

### Walking Through It, Day by Day

```
prices = [7, 1, 5, 3, 6, 4]

Start:
    minSoFar = prices[0] = 7      (nothing to sell against yet, profit = 0)
    maxProfit = 0

Day 1 (price 1):
    possible profit = 1 - minSoFar(7) = -6   → negative, don't take it
    maxProfit stays 0
    update minSoFar = min(7, 1) = 1           ← new lowest seen

Day 2 (price 5):
    possible profit = 5 - minSoFar(1) = 4
    maxProfit = max(0, 4) = 4
    update minSoFar = min(1, 5) = 1           ← unchanged, 1 is still lower

Day 3 (price 3):
    possible profit = 3 - minSoFar(1) = 2
    maxProfit = max(4, 2) = 4                 ← no improvement
    update minSoFar = min(1, 3) = 1

Day 4 (price 6):
    possible profit = 6 - minSoFar(1) = 5
    maxProfit = max(4, 5) = 5                 ← new best!
    update minSoFar = min(1, 6) = 1

Day 5 (price 4):
    possible profit = 4 - minSoFar(1) = 3
    maxProfit = max(5, 3) = 5                 ← no improvement
    update minSoFar = min(1, 4) = 1

Final answer = 5
```

This matches Striver's own hand-traced answer exactly — buy on day 1 (price 1), sell on day 4 (price 6), profit = 5.

### Why We Never Have to Worry About "Selling Before Buying"

A subtle but important point: at every day, `minSoFar` only ever reflects prices from **strictly earlier days** (the update happens *after* computing that day's profit, not before). This automatically guarantees the buy day is always before the sell day — you never accidentally compute a profit using a future price as the buying price. The ordering constraint is enforced structurally by the loop itself, with no extra check needed.

### 🧠Why This Still Counts as Dynamic Programming

There is no explicit `dp[]` array here, and that can feel surprising after Patterns 1–8. But the defining property of DP — *reusing a small summary of previously solved subproblems instead of recomputing from scratch* — is exactly what `minSoFar` is doing. It is, in effect, a **space-optimized DP state** collapsed down to a single rolling variable from the very first line of code, the same way Pattern 1 problems eventually collapsed their `dp[]` array down to `prev1`/`prev2`. This problem simply starts at that final, most-optimized form directly, because the recurrence is simple enough that no intermediate table is ever needed.

---

# Stage 3: Coding

## Approach 1 — Brute Force (Check Every Pair)

**The thinking:** For every possible buy day, try every possible sell day that comes after it, and track the best profit across all such pairs.

```java
class Solution {
    public int maxProfit(int[] prices) {
        int n = prices.length;
        int maxProfit = 0;

        // Try every possible buy day
        for (int buy = 0; buy < n; buy++) {
            // Try every possible sell day AFTER the buy day
            // WHY sell starts at buy + 1: you must sell strictly after buying
            for (int sell = buy + 1; sell < n; sell++) {
                int profit = prices[sell] - prices[buy];
                maxProfit = Math.max(maxProfit, profit);
            }
        }

        return maxProfit;
    }
}
```

**Time Complexity — O(n²):**
Two nested loops. For each of the `n` possible buy days, we check up to `n` possible sell days after it. In the worst case this is roughly `n(n-1)/2` pairs checked — O(n²). For `n` in the tens of thousands (a realistic constraint), this is far too slow.

**Space Complexity — O(1):**
No extra array is used — just a running `maxProfit` variable.

---

## Approach 2 — Optimal (Track Minimum Price Seen So Far)

**The thinking:** This is the direct translation of the intuition built in Stage 2 — a single forward pass, carrying forward exactly one piece of information (the minimum price seen so far).

```java
class Solution {
    public int maxProfit(int[] prices) {
        int n = prices.length;

        // minSoFar = smallest price seen among all days BEFORE the current one
        // Initialize with day 0's price — nothing before it to compare against
        int minSoFar = prices[0];

        // maxProfit = best profit achievable using any valid buy-sell pair
        // seen so far. Starts at 0 because "do nothing" is always a valid
        // option (never forced to make a losing trade)
        int maxProfit = 0;

        // Start from day 1 — day 0 has nothing before it to sell against
        for (int i = 1; i < n; i++) {

            // If we sold TODAY, having bought at the cheapest price seen
            // before today, this is the profit we would make
            int profitIfSoldToday = prices[i] - minSoFar;

            // Keep the best profit seen across all days considered so far
            maxProfit = Math.max(maxProfit, profitIfSoldToday);

            // Update the running minimum AFTER computing today's profit
            // WHY after, not before: today's price cannot be used as
            // its own buying price — the buy must strictly precede the sell
            minSoFar = Math.min(minSoFar, prices[i]);
        }

        return maxProfit;
    }
}
```

**Time Complexity — O(n):**
A single pass through the array, starting from index 1. Each iteration does a constant amount of work — one subtraction, two comparisons. Total: **O(n)**. This is optimal — you cannot solve this problem without looking at every price at least once.

**Space Complexity — O(1):**
Only two variables are used — `minSoFar` and `maxProfit` — regardless of how large the input array is. No array, no recursion stack. This is the same "collapse to constant space" ending point every DP pattern eventually reaches, except here we arrive at it immediately because the recurrence never needed more than the immediately preceding state.

---

## Final Comparison

| Approach | Time | Space | Notes |
| --- | --- | --- | --- |
| Brute Force (check every pair) | O(n²) | O(1) | Correct but too slow for large inputs |
| Track Minimum So Far | O(n) | **O(1)** | Best — submit this |

---

## The Key Takeaway

```
┌───────────────────────────────────────────────────────────────────┐
│  LC 121 is DP in its most compressed possible form:               │
│                                                                   │
│  "Remembering the past" here means remembering just ONE number —  │
│  the minimum price seen among all earlier days.                   │
│                                                                   │
│  The 3-step shortcut applied here:                                │
│  Step 1: Fix the day you sell on                                  │
│  Step 2: Best buy day = day with minimum price before it          │
│  Step 3: Maximize → keep a running max of (today − minSoFar)      │
│                                                                   │
│  Why buy-before-sell is never violated:                           │
│  minSoFar is updated AFTER computing today's profit, never        │
│  before — so today's own price can never be used as its own       │
│  buying price. The ordering constraint is enforced by the         │
│  ORDER of operations inside the loop, with no explicit check.     │
│                                                                   │
│  This is the seed of the entire State Machine DP pattern that     │
│  follows: at every day, you are implicitly in one of two states   │
│  — "holding a stock bought at the best price so far" or "not      │
│  holding anything yet." Every later stock problem in this         │
│  pattern makes that state explicit and lets it branch further     │
│  (multiple transactions, cooldown periods, transaction fees).     │
│  This problem is the version where the state machine has          │
│  collapsed down to its simplest possible shape.                   │
└───────────────────────────────────────────────────────────────────┘
```

Ready for **LC 122: Best Time to Buy and Sell Stock II** (unlimited transactions) whenever you are.