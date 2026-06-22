I've gone through the transcript carefully, and I've also reviewed all the existing solution files — especially the Unbounded Knapsack classic, Coin Change, and Coin Change II solutions — to match Striver's style precisely.

---

# GFG: Rod Cutting

---

# Stage 1: Identification

**Step 1 — Which topic?**

You are given a rod of length `n` and a price array where `price[i]` represents the price of a rod of length `i+1`. You need to cut the rod into pieces and sell them in the market to **maximize the total price** obtained.

The moment you see **"maximize the total price by trying all possible cuts"** — that is the signal. To find the maximum, you must **try all possible ways** to cut the rod. And whenever you try all possible ways, **Recursion** is your first thought. And when that recursion has overlapping subproblems — it becomes **Dynamic Programming**.

**Why doesn't Greedy work here?**

Consider:

```
n = 4,  price = [2, 5, 7, 8]
```

Greedy picks the highest price-per-unit denomination. Price per unit: length 1 → 2, length 2 → 2.5, length 3 → 2.33, length 4 → 2. Greedy picks two pieces of length 2 → total = 10.

But the optimal is: one piece of length 3 + one piece of length 1 = 7 + 2 = 9... actually two pieces of length 2 is 10 which beats this. Let's try a clearer failure case:

```
n = 4,  price = [1, 5, 8, 9]
```

Greedy picks length 2 (price 5, rate 2.5 per unit). Two pieces of length 2 = 10. But optimal is length 2 + length 2 = 10... or length 1 + length 3 = 1 + 8 = 9. Actually two pieces of length 2 wins here too. Let's find where greedy truly fails:

```
n = 4,  price = [2, 5, 7, 8]
Greedy: length 2 (rate 2.5) twice → 5 + 5 = 10
Optimal: length 2 + length 2 → 10, or length 1 + length 3 → 2 + 7 = 9
```

Greedy gets the right answer by accident here. The key point Striver makes is: **there is no uniformity in price per unit**. For any given input, you cannot guarantee that the locally best piece now leads to the globally best combination. You must try all combinations.

**Step 2 — Which pattern?**

You can cut the rod into pieces of length 1, 2, 3, ..., n — and **each length can be used any number of times** (you can cut multiple pieces of the same length). There is a target constraint (total length must equal n). You want to **maximize** the total price.

That is **Pattern 5: Unbounded Knapsack**.

The key structural signal:
- Each piece length can be used unlimited times → Unbounded
- Total length must equal n → target constraint (Knapsack)
- Goal is maximize price → variation of the classic template

**Step 3 — Which key concept?**

**Reframe the problem first — this is the core insight.**

Instead of thinking "cut a rod of length n into pieces," think the opposite:

> **Collect rod lengths from available pieces to form the total length n, while maximizing price.**

This reframing converts the problem into something identical to Unbounded Knapsack:
- **Items** = rod pieces of length 1, 2, 3, ..., n
- **Item weight** = piece length = `index + 1`
- **Item value** = `price[index]`
- **Capacity** = n (target rod length to form)
- **Infinite supply** → same thumb rule applies

Apply Striver's **3-step shortcut**:

```
Step 1: Represent in terms of (index, n)
Step 2: Explore all possibilities — Take or Not Take
Step 3: Question says maximize → take max of all choices
```

**The Unbounded thumb rule stays exactly the same:**

> When you TAKE a piece, **stay at the same index** — don't move to index-1.
> The remaining length shrinks. The index does not.
> This allows the same piece length to be cut again.

---

# Stage 2: Intuition Building

### The Reframing

```
n = 5,  price = [2, 5, 7, 8, 10]
Indices:          0  1  2  3   4
```

Index 0 → piece of length 1, price 2
Index 1 → piece of length 2, price 5
Index 2 → piece of length 3, price 7
Index 3 → piece of length 4, price 8
Index 4 → piece of length 5, price 10

**Rod length at index i = i + 1**

Now we ask: collect rod lengths from these pieces to form exactly length 5, maximizing total price.

```
{1, 2, 2} → length = 5, price = 2 + 5 + 5 = 12
{1, 1, 3} → length = 5, price = 2 + 2 + 7 = 11
{2, 3}    → length = 5, price = 5 + 7 = 12
{5}       → length = 5, price = 10
{1, 4}    → length = 5, price = 2 + 8 = 10
```

Maximum = **12** ✓

### Step 1 — Represent in terms of (index, n)

Define:

```
f(index, n) = maximum price obtainable by collecting rod pieces
              from pieces[0...index] (lengths 1 to index+1),
              to form exactly total length n,
              where each piece has infinite supply
```

The answer we want is `f(n-1, n)`.

### Step 2 — Do all possible things at (index, n)

At any index, exactly two choices:

**Choice 1 — Not Take:**
Skip the piece at this index. Move to the previous index. Target length unchanged.

```
notTake = 0 + f(index - 1, n)
```

**Choice 2 — Take:**
Use the piece at this index. Only valid if `rodLength <= n`, where `rodLength = index + 1`.
Add `price[index]`. Target shrinks by `rodLength`.
**Stay at the same index** — infinite supply, can cut this length again.

```
take = Integer.MIN_VALUE   (initially invalid)
rodLength = index + 1
if rodLength <= n:
    take = price[index] + f(index, n - rodLength)
                               ↑
                    SAME index — not index-1
```

### Step 3 — Take maximum

```
f(index, n) = max(notTake, take)
```

### Base Case — When index == 0

Only piece of length 1 is available with infinite supply. The remaining target `n` can be anything from 0 to the original n.

```
If we use pieces of length 1 to form remaining n:
    We need exactly n pieces of length 1.
    Price = n × price[0]
    Return n × price[0]
```

**Why not a divisibility check like Coin Change?** Because piece length at index 0 is always 1 — it divides every integer target. So the answer is always `n × price[0]`.

### Visualizing the Recursion Tree

```
n = 3,  price = [2, 5, 7]
Rod lengths:  1  2  3

f(2, 3):
├── NOT TAKE length 3  → f(1, 3)
│   ├── NOT TAKE length 2 → f(0, 3)
│   │       3 × price[0] = 3 × 2 = 6
│   └── TAKE length 2   → 5 + f(1, 1)   ← stays at index 1
│           ├── NOT TAKE → f(0, 1) = 1 × 2 = 2
│           └── TAKE: length 2 > 1, CANNOT TAKE
│           f(1,1) = max(2, INVALID) = 2
│           → 5 + 2 = 7
│   f(1,3) = max(6, 7) = 7
│
└── TAKE length 3       → 7 + f(2, 0)   ← stays at index 2
        n=0: 0 × price[0] = 0
        → 7 + 0 = 7

f(2, 3) = max(7, 7) = 7 ✓
```

### Overlapping Subproblems

States like `f(0, 1)` and `f(1, 1)` appear in multiple branches. With larger n, the explosion is massive.

**Overlapping subproblems confirmed** → DP is needed.

### DP Table Size

Two parameters:
- `index`: 0 to n-1 → **n values**
- `remaining length`: 0 to n → **n+1 values**

dp table: **n × (n+1)**

---

# Stage 3: Coding

## Approach 1 — Pure Recursion (Brute Force)

```java
class Solution {
    public int cutRod(int price[], int n) {
        return solve(n - 1, n, price);
    }

    private int solve(int index, int rodLen, int[] price) {
        // Base case: only piece of length 1 available, infinite supply
        // Use it rodLen times to fill the remaining length
        // WHY rodLen × price[0]: piece at index 0 always has length 1
        //     so it always divides any rodLen, no divisibility check needed
        if (index == 0) {
            return rodLen * price[0];
        }

        // Choice 1: Not Take — skip this piece length
        // Add 0 to price since we're not using this piece
        // Move to previous index, target length unchanged
        int notTake = solve(index - 1, rodLen, price);

        // Choice 2: Take — use this piece length (only if it fits)
        // WHY index + 1: rod length at this index is index + 1
        // WHY stay at same index: infinite supply — can cut this length again
        // WHY rodLen - (index + 1): remaining rod length after using this piece
        int take = Integer.MIN_VALUE;
        int pieceLength = index + 1;
        if (pieceLength <= rodLen) {
            take = price[index] + solve(index, rodLen - pieceLength, price);
        }

        // Return maximum — we want maximum price
        return Math.max(notTake, take);
    }
}
```

**Time Complexity — Exponential:**
At every index the function makes 2 recursive calls. But the take case stays at the same index, only reducing the remaining rod length. For piece length 1 and rod length n, the recursion can go n levels deep just from the take branch. This is not 2^n — it grows based on both n and the remaining length. The time complexity is **exponential**.

**Space Complexity — O(n) for the recursion stack:**
The deepest call chain occurs when repeatedly taking piece of length 1 from a rod of length n. That is n frames deep. So auxiliary stack space is **O(n)** in the worst case.

---

## Approach 2 — Memoization (Top-Down DP)

The same state `f(index, rodLen)` is reached from multiple branches. Store each result the first time.

```java
class Solution {
    public int cutRod(int price[], int n) {
        int[][] dp = new int[n][n + 1];
        for (int[] row : dp) {
            Arrays.fill(row, -1);
        }
        return solve(n - 1, n, price, dp);
    }

    private int solve(int index, int rodLen, int[] price, int[][] dp) {
        // Base case: only piece of length 1 available
        if (index == 0) {
            return rodLen * price[0];
        }

        // Step 1: Already computed? Return stored value instantly
        // WHY: f(1, 3) = max price forming length 3 using pieces[0..1]
        //      This never changes regardless of which path reached this state
        if (dp[index][rodLen] != -1) return dp[index][rodLen];

        // Choice 1: Not Take
        int notTake = solve(index - 1, rodLen, price, dp);

        // Choice 2: Take (only if piece fits)
        // WHY stay at same index: infinite supply of each piece length
        int take = Integer.MIN_VALUE;
        int pieceLength = index + 1;
        if (pieceLength <= rodLen) {
            take = price[index] + solve(index, rodLen - pieceLength, price, dp);
        }

        // Step 2: Store before returning
        // WHY: next time solve(index, rodLen) is called,
        //      answer is ready in O(1) — no recomputation
        dp[index][rodLen] = Math.max(notTake, take);
        return dp[index][rodLen];
    }
}
```

**Time Complexity — O(n²):**
Each unique (index, rodLen) pair is computed exactly once. There are n × (n+1) unique states. For each state, O(1) work is done. Total: **O(n²)**.

**Space Complexity — O(n²) + O(n):**
Two sources of space. First, the dp array of size n × (n+1) — that is O(n²). Second, the recursion call stack — the deepest chain can go n levels deep. Total: **O(n²) + O(n)**.

---

## Approach 3 — Tabulation (Bottom-Up DP)

**Direction:** Recursion goes from index n-1 **downward** to index 0. Tabulation goes the **opposite direction** — from index 0 **upward** to index n-1.

**Base case in tabulation:**

When `index == 0`, for every remaining rod length `len` from 0 to n:
- `dp[0][len] = len × price[0]`

**The key direction rule — same as all Unbounded Knapsack:**

The take case uses `dp[i][len - pieceLength]` — the **same row**, to the left (since `pieceLength > 0` means `len - pieceLength < len`).

Fill left to right so that `dp[i][len - pieceLength]` is already updated when we read it — correctly allowing the same piece length to be used again.

```java
class Solution {
    public int cutRod(int price[], int n) {
        // dp[i][len] = max price forming rod of length 'len'
        //              using piece lengths 1 to i+1 (from pieces[0..i])
        int[][] dp = new int[n][n + 1];

        // Step 2: Fill base case — index = 0, only piece of length 1
        // For every remaining rod length from 0 to n:
        // need 'len' pieces of length 1, each costing price[0]
        for (int len = 0; len <= n; len++) {
            dp[0][len] = len * price[0];
        }

        // Step 3: Fill from index=1 to n-1
        for (int i = 1; i < n; i++) {
            int pieceLength = i + 1;   // rod length at this index

            // Fill LEFT TO RIGHT
            // WHY: take case uses dp[i][len - pieceLength] — same row, to the left
            //      Already updated in this pass → correctly allows reuse of same piece
            //      Filling right to left would use old row i-1 value → 0/1 behavior
            for (int len = 0; len <= n; len++) {

                // Choice 1: Not Take this piece length
                // WHY dp[i-1][len]: skip this piece, use result from previous row
                int notTake = dp[i - 1][len];

                // Choice 2: Take this piece — only if it fits in remaining length
                // WHY dp[i][len - pieceLength] not dp[i-1][...]: same row (Unbounded rule)
                //     Piece lengths are reusable — stay on the same row
                int take = Integer.MIN_VALUE;
                if (pieceLength <= len) {
                    take = price[i] + dp[i][len - pieceLength];
                }

                dp[i][len] = Math.max(notTake, take);
            }
        }

        // Answer: max price for full rod of length n using all piece lengths
        return dp[n - 1][n];
    }
}
```

**Dry Run:**

```
n = 4,  price = [2, 5, 7, 8]
Piece lengths:   1  2  3  4

Step 1 — Fill index=0 (pieceLength=1, price=2):
dp[0][len] = len × 2
dp[0] = [0, 2, 4, 6, 8]
          0  1  2  3  4

Step 2 — Fill i=1 (pieceLength=2, price=5):
len=0: notTake=0,    take=N/A(2>0)       → 0
len=1: notTake=2,    take=N/A(2>1)       → 2
len=2: notTake=4,    take=5+dp[1][0]=5   → max(4,5)=5
len=3: notTake=6,    take=5+dp[1][1]=7   → max(6,7)=7
len=4: notTake=8,    take=5+dp[1][2]=10  → max(8,10)=10

dp[1] = [0, 2, 5, 7, 10]

Step 3 — Fill i=2 (pieceLength=3, price=7):
len=0: notTake=0,    take=N/A(3>0)       → 0
len=1: notTake=2,    take=N/A(3>1)       → 2
len=2: notTake=5,    take=N/A(3>2)       → 5
len=3: notTake=7,    take=7+dp[2][0]=7   → max(7,7)=7
len=4: notTake=10,   take=7+dp[2][1]=9   → max(10,9)=10

dp[2] = [0, 2, 5, 7, 10]

Step 4 — Fill i=3 (pieceLength=4, price=8):
len=0: notTake=0,    take=N/A(4>0)       → 0
len=1: notTake=2,    take=N/A(4>1)       → 2
len=2: notTake=5,    take=N/A(4>2)       → 5
len=3: notTake=7,    take=N/A(4>3)       → 7
len=4: notTake=10,   take=8+dp[3][0]=8   → max(10,8)=10

dp[3] = [0, 2, 5, 7, 10]

Answer = dp[3][4] = 10 ✓
```

**Time Complexity — O(n²):**
Two nested loops — outer runs n-1 times, inner runs n+1 times. Each cell does O(1) work. Total: **O(n²)**. No function call overhead, no recursion stack.

**Space Complexity — O(n²):**
Only the dp array of size n × (n+1). No recursion stack.

---

## Approach 4 — Space Optimization (The Final Form)

Look at the recurrence:

```
dp[i][len] = max(dp[i-1][len],  price[i] + dp[i][len - pieceLength])
                  ↑                              ↑
             previous row                same row, to the left
```

Since we fill left to right, both values are correctly accessible from a single array updated in place:
- `prev[len]` before updating still holds the row i-1 value → correct for notTake.
- `prev[len - pieceLength]` (to the left) already holds the row i value (updated this pass) → correct for take.

So we update `prev` in place, left to right — one array, no extra curr needed.

```java
class Solution {
    public int cutRod(int price[], int n) {
        // prev[len] = max price forming rod of length 'len'
        //             using piece lengths seen so far
        // Initialize with base case for index=0 (piece of length 1)
        int[] prev = new int[n + 1];

        for (int len = 0; len <= n; len++) {
            prev[len] = len * price[0];
        }

        // Fill from index=1 to n-1
        for (int i = 1; i < n; i++) {
            int pieceLength = i + 1;

            // Fill LEFT TO RIGHT — critical for correctness
            // WHY left to right: take case reads prev[len - pieceLength]
            //     which is to the left and already updated this pass
            //     → holds current row i value → same piece can be cut again
            //     Filling right to left would use old row i-1 value → 0/1 behavior
            for (int len = 0; len <= n; len++) {

                // Not Take: prev[len] before updating = row i-1 value
                // WHY: we haven't overwritten position len yet in this pass
                int notTake = prev[len];

                // Take: prev[len - pieceLength] is to the LEFT
                // WHY: already updated earlier in this same left-to-right pass
                //      correctly represents cutting the same piece again
                int take = Integer.MIN_VALUE;
                if (pieceLength <= len) {
                    take = price[i] + prev[len - pieceLength];
                }

                // Update in place — prev[len] now holds row i's answer
                prev[len] = Math.max(notTake, take);
            }
        }

        // prev[n] holds the maximum price for full rod of length n
        return prev[n];
    }
}
```

**Time Complexity — O(n²):**
Two nested loops — outer runs n-1 times, inner runs n+1 times left to right. Each iteration does O(1) work. Total: **O(n²)**. Same as tabulation.

**Space Complexity — O(n):**
No n × (n+1) array. No recursion stack. Just one array of size n+1. Memory reduced from O(n²) to O(n).

---

## Final Comparison

| Approach | Time | Space | Notes |
|---|---|---|---|
| Pure Recursion | Exponential | O(n) stack | Never use |
| Memoization | O(n²) | O(n²) + O(n) stack | Good interview starting point |
| Tabulation | O(n²) | O(n²) | Better — eliminates stack |
| Space Optimization | O(n²) | **O(n)** | Best — submit this |

---

## How This Problem Relates to the Full Unbounded Knapsack Progression

| Problem | Items | Weight | Value | Goal |
|---|---|---|---|---|
| Classic Unbounded Knapsack | items with wt and val | `wt[i]` | `val[i]` | maximize value |
| LC 322 Coin Change | coin denominations | `coins[i]` | `1` (per coin) | minimize count |
| LC 518 Coin Change II | coin denominations | `coins[i]` | `1` (per way) | count ways |
| **GFG Rod Cutting** | **piece lengths** | **`index + 1`** | **`price[index]`** | **maximize price** |

Rod Cutting is structurally identical to Classic Unbounded Knapsack. The only surface difference is that the "weight" of each item is derived from the index (`index + 1`) rather than given in a separate array.

---

## The Key Takeaway

```
┌──────────────────────────────────────────────────────────────────┐
│  Rod Cutting = Classic Unbounded Knapsack with one reframing:    │
│                                                                  │
│  Instead of:  "cut the rod of length n into pieces"              │
│  Think:       "collect rod pieces to form total length n"        │
│                                                                  │
│  The mapping:                                                    │
│  Item weight  = piece length = index + 1                         │
│  Item value   = price[index]                                     │
│  Capacity     = n (total length to form)                         │
│                                                                  │
│  The Unbounded rule stays exactly the same:                      │
│  When you TAKE, stay at the same index.                          │
│  The remaining length shrinks. The index does not.               │
│  This allows the same piece length to be cut again.              │
│                                                                  │
│  Base case at index 0:                                           │
│  Piece length is always 1 → divides every target                 │
│  No divisibility check needed (unlike Coin Change)               │
│  Return: rodLen × price[0]                                       │
│                                                                  │
│  Direction rule (same as all Unbounded Knapsack):                │
│  Fill the single prev[] array LEFT TO RIGHT.                     │
│  prev[len - pieceLength] already updated → same piece reused.    │
│  Filling right to left gives 0/1 behavior — wrong.               │
│                                                                  │
│  Complete Unbounded Knapsack summary:                            │
│  Maximize → max, sentinel = Integer.MIN_VALUE                    │
│  Minimize → min, sentinel = +1e9                                 │
│  Count    → sum, sentinel = 0                                    │
│  Structure identical across all three. Only Step 3 changes.      │
└──────────────────────────────────────────────────────────────────┘
```