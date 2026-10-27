# Parameterised and Functional Recursion

Status: Done

So far, every recursive function we've written has been **void** — it doesn't return anything, it just does work (printing) along the way. Now we look at two different *styles* of writing recursive functions that actually **compute and return a value**: the **parameterized way** and the **functional way**. We'll use the same problem — sum of the first `n` natural numbers — to compare both styles directly.

---

### The Problem: Sum of First N Numbers

If `n = 3`, the answer is `1 + 2 + 3 = 6`. Easy with a loop — but the goal is to build strong recursive thinking, since this same pattern reappears everywhere in DSA.

---

### `Style 1`: Parameterized Recursion

**Core idea:** carry the "running answer" as an extra parameter, and update it a little bit with each call — similar to how a loop uses an accumulator variable. The function itself doesn't return anything meaningful; the final answer just gets printed once we hit the base case.

```cpp
void findSum(int i, int sum) {
    if (i < 1) {              // base case: nothing left to add
        cout << sum;
        return;
    }
    
    findSum(i - 1, sum + i);    // add i into the running sum, move to i-1
}

int main() {
    int n = 3;
    findSum(n, 0);   // start at n, running sum starts at 0
}
```

**Tracing for `n = 3`:**

```
findSum(3, 0)  → i=3, not <1 → call findSum(2, 0+3) = findSum(2, 3)
findSum(2, 3)  → i=2, not <1 → call findSum(1, 3+2) = findSum(1, 5)
findSum(1, 5)  → i=1, not <1 → call findSum(0, 5+1) = findSum(0, 6)
findSum(0, 6)  → i=0, IS <1  → print sum → prints 6, then return
```

**Recursion tree:**

```
findSum(3, 0)
   └── findSum(2, 3)
          └── findSum(1, 5)
                 └── findSum(0, 6) → **base case** → print 6, return
```

Notice: the `sum` parameter itself is what carries the answer forward, growing a little bit with each call. By the time we hit the base case, `sum` already holds the final answer — we just print it. This is essentially "recursion imitating a loop," where the parameters play the role that loop variables normally would.

---

### `Style 2`: Functional Recursion

**Core idea:** instead of carrying the answer forward as a parameter, let the function **return** a value, and build the final answer by combining that returned value with the current step — the way you'd write a mathematical definition.

Think about it like this: *the sum of the first `n` numbers = `n` + (the sum of the first `n-1` numbers)*. That's genuinely how you can define it:

```
sum(3) = 3 + sum(2)
sum(2) = 2 + sum(1)
sum(1) = 1 + sum(0)
sum(0) = 0            ← known directly, no further breakdown needed
```

Translating this directly into code:

```cpp
int findSum(int n) {
    if (n == 0) {           // base case: sum of first 0 numbers is 0
        return 0;
    }
    return n + findSum(n - 1);   // combine current n with the rest
}

int main() {
    int n = 3;
    cout << findSum(n);   // prints 6
}
```

**Tracing for `n = 3` — this time paying attention to what each call *returns*, not just what it prints:**

```
findSum(3) needs: 3 + findSum(2)   → waits for findSum(2) to return
findSum(2) needs: 2 + findSum(1)   → waits for findSum(1) to return
findSum(1) needs: 1 + findSum(0)   → waits for findSum(0) to return
findSum(0) → base case → returns 0 directly
```

Now unwind, filling in each return value as it comes back:

```
findSum(0) returns 0
findSum(1) returns 1 + 0 = 1
findSum(2) returns 2 + 1 = 3
findSum(3) returns 3 + 3 = 6   ← final answer
```

**Recursion tree (showing return values):**

```
findSum(3) = 3 + findSum(2)
                  = 2 + findSum(1)
                           = 1 + findSum(0)
                                    = 0   ← base case
                           = 1 + 0 = 1
                  = 2 + 1 = 3
       = 3 + 3 = 6
```

**Why does this style matter?** Because a lot of the time — especially once you get to topics like Dynamic Programming later in the course — you need a function that *hands back* a value to whoever called it, rather than just printing something to the screen. Functional recursion is what makes that possible: each call trusts that the *smaller* version of the problem will correctly return its own answer, and just focuses on combining that with its own piece of the work.

---

### Parameterized vs. Functional — Side by Side

|  | Parameterized Recursion | Functional Recursion |
| --- | --- | --- |
| Return type | usually `void` | returns a real value (e.g. `int`) |
| How answer is built | carried forward through an extra parameter, updated each call | built backward as each call returns and combines with its own step |
| Feels like | a loop translated into recursive form | a mathematical definition translated directly into code |
| When it's printed/used | printed once, right at the base case | returned all the way up to `main()`, then printed/used there |

Both styles solve the exact same problem — the difference is purely in *how* the answer travels: forward through parameters, or backward through return values.

---

### A Common Trap: Factorial and the Wrong Base Case

Let's apply functional recursion to a very similar-looking problem: **factorial of n** (`n! = 1 × 2 × 3 × ... × n`). For `n = 3`, that's `6`; for `n = 4`, that's `24`.

Since it's "the same pattern" as sum, it's tempting to just swap `+` for `*` and reuse the same base case:

```cpp
/**/ ⚠️ THIS HAS A BUG**
int factorial(int n) {
    if (n == 0) {
        return 0;     // copied from the sum example — looks reasonable, but isn't!
    }
    return n * factorial(n - 1);
}
```

**Let's trace this for `n = 3` and see what goes wrong:**

```
factorial(3) = 3 * factorial(2)
factorial(2) = 2 * factorial(1)
factorial(1) = 1 * factorial(0)
factorial(0) = 0    ← base case returns 0
```

Unwinding:

```
factorial(0) returns 0
factorial(1) returns 1 * 0 = 0
factorial(2) returns 2 * 0 = 0
factorial(3) returns 3 * 0 = 0
```

**Final answer: `0`. Completely wrong** — it should be `6`.

**Why did this happen?** In the sum problem, adding `0` at the base case is harmless — it doesn't change anything (`x + 0 = x`). But in factorial, we're *multiplying*, and multiplying anything by `0` wipes it out to `0` — and that `0` then contaminates every single multiplication above it in the chain.

**The fix:** the base case for factorial must return `1` (the "do-nothing" value for multiplication), not `0`:

```cpp
int factorial(int n) {
    if (n == 0) {
        return 1;    // correct base case for multiplication
    }
    return n * factorial(n - 1);
}
```

**Re-tracing for `n = 3`:**

```
factorial(0) returns 1
factorial(1) returns 1 * 1 = 1
factorial(2) returns 2 * 1 = 2
factorial(3) returns 3 * 2 = 6   ✅ correct!
```

**The lesson this teaches:** the correct base case value depends on the *operation* you're combining results with — `0` is "neutral" for addition, `1` is "neutral" for multiplication. Copying a base case from a similar-looking problem without thinking about *why* it worked there is a common source of subtle, silent bugs (notice this bug didn't crash — it just quietly gave a wrong answer, which is often harder to catch than a crash).

---

### Time and Space Complexity (Both Problems)

For both `findSum` and `factorial`:

- **Time complexity: O(n).** Each call does a small constant amount of work (one comparison, one multiplication/addition), and there are exactly `n` calls total.
- **Space complexity: O(n).** At the deepest point of recursion, all `n` calls are sitting in the stack simultaneously, each waiting for the call below it to return before it can complete. This stack usage is often called **auxiliary space**, since it's memory the algorithm uses internally (via the call stack) rather than in an explicit array or data structure you created yourself.

---

## Summary: Lecture 3

| Concept | Meaning |
| --- | --- |
| Parameterized recursion | answer is built forward, carried through an extra parameter, printed at the base case |
| Functional recursion | answer is built backward, through return values, combined as calls return |
| Neutral base case value | the "do-nothing" value for whatever operation you're combining with (`0` for `+`, `1` for `×`) |
| Silent wrong-answer bug | picking the wrong base case value doesn't crash — it just gives incorrect output, which is harder to debug than a crash |

---