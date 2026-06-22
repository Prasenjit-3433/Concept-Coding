I've gone through the transcript carefully, and I can also see all the existing solution files (Fibonacci, Climbing Stairs, Frog Jump, House Robber I & II) to make sure I match Striver's exact teaching style.

Let me write the complete solution note for Ninja's Training.

---

# Ninja's Training (Coding Ninjas)

---

# Stage 1: Identification

**Step 1 — Which topic?**

You are given a 2D array of size n × 3 representing merit points for three activities over n days. Ninja can perform any one activity per day but **cannot perform the same activity on two consecutive days**. You need to find the **maximum merit points** Ninja can earn.

The moment you see **"find the maximum among all possible ways"** — that is the signal. To find the best among all ways, you must **try all possible ways**. And whenever you try all possible ways, **Recursion** is your first thought. And when that recursion has overlapping subproblems — it becomes **Dynamic Programming**.

**Why doesn't Greedy work here?**

Your greedy instinct says: on day 0, pick the maximum — which is 50. Then on day 1, since 50 was task 2 (0-indexed), you cannot pick task 2 again, so pick max of the rest — which is 11. Total = 61.

But the actual answer is 110.

```
Day 0: pick task 0 → 10 points
Day 1: pick task 1 → 100 points
Total = 110
```

Greedy failed because picking 50 on day 0 looked great locally but blocked access to 100 on day 1. **A greedy choice now can close off a much better path later.** You must try all ways.

**Step 2 — Which pattern?**

The input is a 2D array. But notice — the recurrence here is not about rows and columns of a matrix in the grid-path sense. The key insight is that **there are two changing variables**: the day (index) and the last task performed. This is **2D DP** — two state variables define each subproblem.

That is **Pattern 3: 2D / Grid DP**.

**Step 3 — Which key concept?**

Apply Striver's **3-step shortcut**:

```
Step 1: Represent problem in terms of index
Step 2: Do all possible things on that index
Step 3: Question says maximum → take max of all results
```

The twist here: **in order to do all possible things on a given day, you need to know what was done on the previous day** (the consecutive constraint). This forces a **second parameter** — `last` — alongside the day index. This is what makes it 2D DP, not 1D.

---

# Stage 2: Intuition Building

### The Problem Setup

```
points[][] = {
    {10, 40, 70},   // Day 0: Task0=10, Task1=40, Task2=70
    {20, 50, 80},   // Day 1
    {30, 60, 90}    // Day 2
}
```

Three tasks (0, 1, 2). Three days (0, 1, 2). Cannot do the same task on two consecutive days.

### Step 1 — Represent the problem in terms of index

Each **day** is an index. Striver tends to go **top-down**: start from day n-1 and recurse downward to day 0.

Define:

```
f(day, last) = maximum merit points from day 0 to 'day',
               given that the task performed after 'day' was 'last'
               (so 'last' task cannot be repeated on 'day')
```

So the answer we want is `f(n-1, 3)` — start from the last day, and pass `last = 3` to mean **no task has been performed yet** (3 is a dummy value outside the valid range 0, 1, 2).

### Step 2 — Do all possible things on that day

On any given day, you have 3 tasks to choose from. **But you cannot choose the task that was done on the next day** (remember, we are going right to left, so `last` means what was done on the day after current).

So iterate all tasks 0, 1, 2. For each task `i`:
- If `i != last` → you can perform this task
- Points earned = `points[day][i]` + result of solving `f(day-1, i)`

### Step 3 — Take maximum

Since the question asks for **maximum**, take the max over all valid task choices.

### The Recurrence

```
f(day, last) = max over all task i (where i != last):
                   points[day][i] + f(day - 1, i)

Base case (day == 0):
    Return max of points[0][i] for all i where i != last
```

### Why do we need the second parameter `last`?

Without knowing what task was done **after** the current day, we have no way to enforce the constraint. The `last` parameter carries that information as we recurse downward.

```
f(2, 3)           ← day 2, no restriction
  ├── task 0: 30 + f(1, 0)
  │             ├── task 1: 50 + f(0, 1)
  │             │             ├── task 0: 10 ✓  
  │             │             └── task 2: 70 ✓ → max = 70
  │             │           = 50 + 70 = 120
  │             └── task 2: 80 + f(0, 2)
  │                           ├── task 0: 10 ✓
  │                           └── task 1: 40 ✓ → max = 40
  │                         = 80 + 40 = 120
  │           → max(120, 120) = 120
  │         30 + 120 = 150
  ├── task 1: 60 + f(1, 1)
  │             ├── task 0: 20 + f(0, 0)
  │             │             ├── task 1: 40 ✓
  │             │             └── task 2: 70 ✓ → max = 70
  │             │           = 20 + 70 = 90
  │             └── task 2: 80 + f(0, 2)
  │                           ├── task 0: 10 ✓
  │                           └── task 1: 40 ✓ → max = 40
  │                         = 80 + 40 = 120
  │           → max(90, 120) = 120
  │         60 + 120 = 180
  └── task 2: 90 + f(1, 2)
                ├── task 0: 20 + f(0, 0) = 20 + 70 = 90
                └── task 1: 50 + f(0, 1) = 50 + 40 = 90  ← wait, f(0,1) = max(10, 70) = 70
                  → 50 + 70 = 120
              → max(90, 120) = 120
            90 + 120 = 210

Answer = max(150, 180, 210) = 210
```

### Are there overlapping subproblems?

Notice `f(1, 0)`, `f(0, 1)`, `f(0, 2)` all get called multiple times across different branches. As n grows, the explosion is massive.

**Overlapping subproblems confirmed** → DP is needed.

### The DP Table Size

Two parameters:
- `day`: ranges from 0 to n-1 → **n values**
- `last`: can be 0, 1, 2, or 3 → **4 values**

So we need an **n × 4** dp table.

---

# Stage 3: Coding

## Approach 1 — Pure Recursion (Brute Force)

```java
import java.util.*;

public class Solution {
    public static int ninjaTraining(int n, int points[][]) {
        return solve(n - 1, 3, points);
    }

    private static int solve(int day, int last, int[][] points) {
        // Base case: we've reached day 0
        // Pick the best task that wasn't done on the day after
        if (day == 0) {
            int maxi = 0;
            for (int task = 0; task < 3; task++) {
                if (task != last) {
                    maxi = Math.max(maxi, points[0][task]);
                }
            }
            return maxi;
        }

        // Try all tasks on the current day
        int maxi = 0;
        for (int task = 0; task < 3; task++) {
            if (task != last) {
                // Points from this task + best we can do from day 0 to day-1
                // Tell the next call that 'task' was the last performed
                int point = points[day][task] + solve(day - 1, task, points);
                maxi = Math.max(maxi, point);
            }
        }
        return maxi;
    }
}
```

**Time Complexity — O(3^n):**
At every day, the function tries up to 3 tasks (actually 2 since one is blocked, but at the starting call all 3 are available). The recursion tree branches into 2 or 3 calls at every level. The tree has n levels. In the worst case this grows exponentially — roughly O(3^n). For n = 100, this is completely impractical.

**Space Complexity — O(n):**
No dp array is allocated. But the **recursion call stack** holds frames — the deepest chain goes from day n-1 all the way down to day 0. That is n frames simultaneously on the stack. So call stack uses O(n) space.

---

## Approach 2 — Memoization (Top-Down DP)

The same subproblems are solved repeatedly. For example `f(1, 0)` might be computed multiple times from different branches. Store each result the first time it is computed.

**3 steps to convert recursion to memoization:**
```
Step 0: Declare dp array of size n × 4, initialize all to -1
Step 1: Before computing, check — if dp[day][last] != -1, return it   (lookup)
Step 2: After computing, store — dp[day][last] = maxi                  (store)
```

```java
import java.util.*;

public class Solution {
    public static int ninjaTraining(int n, int points[][]) {
        // Step 0: dp[day][last] = -1 means not yet computed
        // last can be 0, 1, 2, 3 → size 4 in second dimension
        int[][] dp = new int[n][4];
        for (int[] row : dp) {
            Arrays.fill(row, -1);
        }
        return solve(n - 1, 3, points, dp);
    }

    private static int solve(int day, int last, int[][] points, int[][] dp) {
        // Base case: day 0
        if (day == 0) {
            int maxi = 0;
            for (int task = 0; task < 3; task++) {
                if (task != last) {
                    maxi = Math.max(maxi, points[0][task]);
                }
            }
            return maxi;
        }

        // Step 1: Already computed? Return stored value instantly
        // WHY: f(1, 0) = "best points from day 0 to 1 given task 0 was done after"
        //      This never changes no matter when you call it.
        if (dp[day][last] != -1) return dp[day][last];

        // Try all tasks on this day
        int maxi = 0;
        for (int task = 0; task < 3; task++) {
            if (task != last) {
                int point = points[day][task] + solve(day - 1, task, points, dp);
                maxi = Math.max(maxi, point);
            }
        }

        // Step 2: Store before returning
        // WHY: Next time solve(day, last) is called, answer is ready in O(1)
        dp[day][last] = maxi;
        return dp[day][last];
    }
}
```

**Time Complexity — O(n × 4 × 3):**
Without memoization, the same subproblem like `f(1, 0)` gets recomputed many times. With memoization, each unique (day, last) pair is computed **exactly once**. There are n × 4 unique subproblems. For each subproblem, we run a loop of size 3. Total: O(n × 4 × 3) = O(12n) = O(n).

**Space Complexity — O(n × 4) + O(n):**
Two sources of space. First, the dp array of size n × 4 — that is O(4n). Second, the **recursion call stack** — the deepest chain goes n levels deep before hitting day 0. So stack uses another O(n). Total space: O(4n) + O(n) = O(n).

---

## Approach 3 — Tabulation (Bottom-Up DP)

Flip the direction. Instead of starting from day n-1 and going down, start from day 0 (base case) and build upward to day n-1. No recursion stack at all.

**The recipe:**
```
Step 1: Declare dp array of size n × 4
Step 2: Fill base cases (day == 0) directly
Step 3: Look at recurrence — figure out starting index
        (uses day-1, so valid from day=1 onwards)
Step 4: Fill from day=1 to n-1 using the recurrence
```

**Base case analysis:**
When `day == 0`, last can be 0, 1, 2, or 3. Fill all four:
- `dp[0][0]` = max of task 1, task 2 (task 0 is blocked)
- `dp[0][1]` = max of task 0, task 2 (task 1 is blocked)
- `dp[0][2]` = max of task 0, task 1 (task 2 is blocked)
- `dp[0][3]` = max of task 0, task 1, task 2 (nothing blocked)

```java
import java.util.*;

public class Solution {
    public static int ninjaTraining(int n, int points[][]) {
        int[][] dp = new int[n][4];

        // Step 2: Fill base cases for day = 0
        // dp[0][last] = max points on day 0 excluding the 'last' task
        dp[0][0] = Math.max(points[0][1], points[0][2]);
        dp[0][1] = Math.max(points[0][0], points[0][2]);
        dp[0][2] = Math.max(points[0][0], points[0][1]);
        dp[0][3] = Math.max(points[0][0], Math.max(points[0][1], points[0][2]));

        // Step 3 & 4: Fill from day=1 to n-1
        for (int day = 1; day < n; day++) {
            // last can be 0, 1, 2, 3
            for (int last = 0; last < 4; last++) {
                dp[day][last] = 0;
                // Try all 3 tasks on this day
                for (int task = 0; task < 3; task++) {
                    if (task != last) {
                        // Points from this task + best from previous days
                        // Previous day's answer: dp[day-1][task]
                        // WHY dp[day-1][task]: after doing 'task' today,
                        // yesterday cannot have done 'task'
                        int point = points[day][task] + dp[day - 1][task];
                        dp[day][last] = Math.max(dp[day][last], point);
                    }
                }
            }
        }

        // Answer: last day, no restriction (last = 3)
        return dp[n - 1][3];
    }
}
```

**Time Complexity — O(n × 4 × 3):**
Three nested loops — outer loop runs n-1 times (day 1 to n-1), middle loop runs 4 times (all last values), inner loop runs 3 times (all tasks). Total: O(12n) = O(n). Same as memoization but no function call overhead and no recursion stack.

**Space Complexity — O(n × 4):**
Only the dp array of size n × 4. No recursion stack. Already an improvement over memoization.

---

## Approach 4 — Space Optimization (The Final Form)

Look at the tabulation code carefully:

```
dp[day][last] = points[day][task] + dp[day - 1][task]
```

To compute `dp[day][...]`, you only need `dp[day-1][...]`. You do not need anything before that. So why keep the entire n × 4 array?

**The key observation:**
When computing row `day`, you only ever look at row `day-1`. After computing row `day`, row `day-1` is never needed again.

So instead of an n × 4 array, keep just **one array of size 4** representing the previous day's answers. Compute the current day into a temporary array of size 4. Then slide forward.

```
Before loop:
    prev[0] = dp[0][0] = max(points[0][1], points[0][2])
    prev[1] = dp[0][1] = max(points[0][0], points[0][2])
    prev[2] = dp[0][2] = max(points[0][0], points[0][1])
    prev[3] = dp[0][3] = max(points[0][0], points[0][1], points[0][2])

day = 1:
    curr[last] = max over valid tasks of: points[1][task] + prev[task]
    prev = curr   ← slide forward

day = 2:
    curr[last] = max over valid tasks of: points[2][task] + prev[task]
    prev = curr

... and so on

Answer = prev[3]   ← last = 3 means no restriction, full answer
```

```java
import java.util.*;

public class Solution {
    public static int ninjaTraining(int n, int points[][]) {

        // prev[last] = best points from day 0,
        // given that 'last' task cannot be repeated
        int[] prev = new int[4];

        // Base case: day 0
        prev[0] = Math.max(points[0][1], points[0][2]);
        prev[1] = Math.max(points[0][0], points[0][2]);
        prev[2] = Math.max(points[0][0], points[0][1]);
        prev[3] = Math.max(points[0][0], Math.max(points[0][1], points[0][2]));

        // Fill from day 1 to n-1
        for (int day = 1; day < n; day++) {
            int[] curr = new int[4];

            for (int last = 0; last < 4; last++) {
                curr[last] = 0;
                for (int task = 0; task < 3; task++) {
                    if (task != last) {
                        // Points today + best from previous days
                        // prev[task] = best points achievable from day 0 to day-1
                        //              given that 'task' was done on day
                        //              (so day-1 cannot repeat 'task')
                        int point = points[day][task] + prev[task];
                        curr[last] = Math.max(curr[last], point);
                    }
                }
            }

            // Slide the window forward
            // WHY: what was 'day' becomes 'day-1' in the next iteration
            prev = curr;
        }

        // prev[3] = answer with no restriction = full answer
        return prev[3];
    }
}
```

**Time Complexity — O(n × 4 × 3):**
Still three nested loops with the same iteration count. No change in time complexity — O(12n) = O(n).

**Space Complexity — O(1) — well, O(4) = constant:**
No n × 4 array. No recursion stack. Just two arrays of size 4 — `prev` and `curr` — regardless of how large n is. Whether n is 10 or 10 million, the memory used stays constant. That is O(1).

---

## Dry Run — Space Optimized on Example

```
n = 3
points = {
    {10, 40, 70},
    {20, 50, 80},
    {30, 60, 90}
}
```

**Base case (day = 0):**
```
prev[0] = max(40, 70) = 70     (task 0 blocked, pick best of task 1, task 2)
prev[1] = max(10, 70) = 70     (task 1 blocked, pick best of task 0, task 2)
prev[2] = max(10, 40) = 40     (task 2 blocked, pick best of task 0, task 1)
prev[3] = max(10, 40, 70) = 70 (nothing blocked)
```

**day = 1:**
```
last=0: task1→ 50+prev[1]=50+70=120, task2→ 80+prev[2]=80+40=120 → curr[0]=120
last=1: task0→ 20+prev[0]=20+70=90,  task2→ 80+prev[2]=80+40=120 → curr[1]=120
last=2: task0→ 20+prev[0]=20+70=90,  task1→ 50+prev[1]=50+70=120 → curr[2]=120
last=3: task0→ 20+70=90, task1→ 50+70=120, task2→ 80+40=120      → curr[3]=120

prev = [120, 120, 120, 120]
```

**day = 2:**
```
last=0: task1→ 60+prev[1]=60+120=180, task2→ 90+prev[2]=90+120=210 → curr[0]=210
last=1: task0→ 30+prev[0]=30+120=150, task2→ 90+prev[2]=90+120=210 → curr[1]=210
last=2: task0→ 30+120=150, task1→ 60+120=180                        → curr[2]=180
last=3: task0→ 30+120=150, task1→ 60+120=180, task2→ 90+120=210    → curr[3]=210

prev = [210, 210, 180, 210]
```

**Answer = prev[3] = 210** ✓

---

## Final Comparison

| Approach | Time | Space | Notes |
|---|---|---|---|
| Pure Recursion | O(3^n) | O(n) stack | Exponential — never use |
| Memoization | O(n × 4 × 3) | O(n × 4) + O(n) stack | Good interview starting point |
| Tabulation | O(n × 4 × 3) | O(n × 4) | Better — eliminates stack |
| Space Optimization | O(n × 4 × 3) | **O(4) ≈ O(1)** | Best — submit this |

---

## The Key Takeaway

```
┌──────────────────────────────────────────────────────────────────┐
│  When you need to track a condition across adjacent indices,     │
│  introduce a second parameter alongside the index.               │
│                                                                  │
│  "What did I do on the previous day?" → add 'last' parameter     │
│  This is what makes this problem 2D DP, not 1D.                  │
│                                                                  │
│  The 'last = 3' trick:                                           │
│  Use a value outside the valid range (0,1,2) to represent        │
│  "no restriction" at the very first call.                        │
│                                                                  │
│  Space optimization for 2D DP:                                   │
│  If dp[day][...] only uses dp[day-1][...],                       │
│  replace the entire n×4 array with two arrays of size 4.         │
│  prev holds yesterday, curr holds today.                         │
│  After computing curr, make prev = curr and move forward.        │
│                                                                  │
│  This pattern of replacing a 2D table with two 1D arrays         │
│  will repeat across many 2D DP problems.                         │
└──────────────────────────────────────────────────────────────────┘
```