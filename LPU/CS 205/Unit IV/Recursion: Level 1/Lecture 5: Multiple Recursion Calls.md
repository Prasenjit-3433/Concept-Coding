# Multiple Recursion Calls

Status: In progress

Every example so far has had **one** recursive call inside the function. Now we look at functions that make **more than one** recursive call — a pattern that shows up constantly in harder problems later in the course (like backtracking problems with several branching choices).

### The Problem: Nth Fibonacci Number

**What is the Fibonacci sequence?** Each number is the sum of the two numbers before it, starting with `0` and `1`:

```
Position:  0   1   2   3   4   5   6   7   8
Value:     0   1   1   2   3   5   8   13  21
```

So `fib(5) = 5`, `fib(4) = 3`, and in general: `fib(n) = fib(n-1) + fib(n-2)`.

**Base case:** if `n` is `0` or `1`, the answer is just `n` itself (fib(0) = 0, fib(1) = 1) — no further breakdown needed.

```cpp
int fib(int n) {
    if (n <= 1) {              // base case
        return n;
    }
    int last = fib(n - 1);       // first recursive call
    int secondLast = fib(n - 2); // second recursive call
    return last + secondLast;
}

int main() {
    cout << fib(4);   // prints 3
}
```

### How Do Two Recursive Calls Inside One Function Actually Run?

This is the key question this lecture answers: **they don't run at the same time.** The first recursive call (`fib(n-1)`) runs completely — goes all the way down to its base cases and fully returns — *before* the second call (`fib(n-2)`) even starts. Recursion always executes one call at a time, in the order they appear in the code.

**Tracing `fib(4)` step by step:**

`fib(4)` needs `fib(3)` first. So execution dives into `fib(3)` completely before touching `fib(2)`.

```
fib(4)
 └── fib(3)                     ← this whole branch finishes FIRST
       └── fib(2)
             └── fib(1) → base case → returns 1
             └── fib(0) → base case → returns 0
             → fib(2) returns 1 + 0 = 1
       └── fib(1) → base case → returns 1
       → fib(3) returns 1 + 1 = 2
 └── fib(2)                     ← only NOW does this branch begin
       └── fib(1) → base case → returns 1
       └── fib(0) → base case → returns 0
       → fib(2) returns 1 + 0 = 1
 → fib(4) returns 2 + 1 = 3
```

**Recursion tree (the standard way this is drawn):**

```
                    fib(4)
                   /      \
              fib(3)        fib(2)
             /     \        /     \
        fib(2)   fib(1)  fib(1)  fib(0)
        /    \      |       |       |
    fib(1) fib(0)   1       1       0
      |      |
      1      0
```

Reading the tree from the bottom up: `fib(2) = fib(1) + fib(0) = 1 + 0 = 1`, then `fib(3) = fib(2) + fib(1) = 1 + 1 = 2`, then `fib(4) = fib(3) + fib(2) = 2 + 1 = 3`.

**Important thing to notice:** `fib(2)` gets computed **twice** in this tree (once inside the `fib(3)` branch, once as a direct child of `fib(4)`), and `fib(1)` gets computed even more times than that. This repeated, wasted work is a big deal — and it's the reason naive recursive Fibonacci becomes painfully slow for larger `n`. This exact inefficiency is what **Dynamic Programming** (a later topic) is designed to fix, by remembering results instead of recomputing them.

### Relating the Code to the Tree

The way to read *any* recursion tree with multiple calls: look at your function line by line, in order. Whichever recursive call appears **first** in the code is the one whose entire branch finishes first — completely, all the way down and back up — before the **second** call even begins. This holds no matter how many recursive calls a function makes (two, three, or more) — they always execute strictly one after another, never simultaneously.

### Time Complexity: Why It's Exponential

Count the actual number of calls made for `fib(4)`: it comes out to 9 total calls — clearly far more than just 4.

**Why?** Because at (almost) every level, one call spawns **two** more calls, each of which spawns two more, and so on — a branching pattern rather than a straight line. This gives roughly `2 × 2 × 2 × ...`  (n times), i.e., **O(2ⁿ)** — exponential time.

It's not *exactly* `2ⁿ` (for `fib(4)` that would suggest 16 calls, but we only counted 9) — that's because one branch (`n-1`) is always slightly "deeper" than the other (`n-2`), so the tree isn't perfectly symmetric. But the growth rate is still classified as exponential, and it gets dramatically slower as `n` grows.

**Space complexity:** still O(n) — even though there are exponentially many *calls*, at any given moment the stack only holds calls along the *current* path from root to the deepest active call, and that path's length is bounded by `n`.

---

## Summary: Lectures 4 & 5

| Concept | Meaning |
| --- | --- |
| Reverse array (two-pointer recursion) | swap outer pair, recurse inward until pointers meet/cross |
| Reverse array (single-pointer recursion) | derive the "partner" index mathematically (`n - i - 1`) instead of tracking two pointers |
| Palindrome check | functional recursion that compares instead of swaps; early `false` short-circuits the rest |
| Multiple recursion calls | a function calling itself more than once; calls execute strictly one after another, never simultaneously |
| Fibonacci recursion tree | branches into two calls per level, causing repeated recomputation of the same subproblems |
| Exponential time complexity O(2ⁿ) | arises when each call spawns roughly two more calls, creating a branching (tree-shaped) call pattern |

---