# Problems on Recursion

Status: Done

![image.png](Problems%20on%20Recursion/image.png)

### Problem 1: Print Your Name N Times (Using Recursion, Not a Loop)

The obvious way to print something `n` times is a `for` loop. The challenge here is to do it *without* a loop — purely with recursion.

**Key new idea: passing parameters instead of using a global variable.**

In Lecture 1's example, we used a `global count` variable to track progress. That works, but it's not great practice — a cleaner approach is to pass the tracking variable *as a parameter* to the function itself. This way, each function call carries its own state, instead of everyone sharing one global variable.

```cpp
void printName(int i, int n) {
    if (i > n) {          // base case: stop once we've printed n times
        return;
    }
    cout << "Raj" << endl;
    printName(i + 1, n);   // move one step closer to the base case
}

int main() {
    int n;
    cin >> n;
    printName(1, n);       // start counting from 1
}
```

**Tracing this for `n = 3`:**

```
printName(1, 3)  →  1 > 3? No  → print "Raj" → call printName(2, 3)
printName(2, 3)  →  2 > 3? No  → print "Raj" → call printName(3, 3)
printName(3, 3)  →  3 > 3? No  → print "Raj" → call printName(4, 3)
printName(4, 3)  →  4 > 3? Yes → return (base case hit)
```

**Recursion tree:**

```
printName(1,3)
   └── printName(2,3)
          └── printName(3,3)
                 └── printName(4,3) → base case → return
```

Notice the pattern: each call does its job (`print`), then hands off to the next call with an updated parameter (`i + 1`) that's *closer* to the base case. This "moving closer to the base case with each call" is essential — without it, you'd get infinite recursion again.

**Time and space complexity:**

- We make exactly `n` function calls (one per print), and each call does a constant amount of work — so **time complexity is O(n)**.
- At the deepest point, there are `n` calls sitting in the stack, all waiting to complete — so **space complexity (stack space) is also O(n)**.

---

### Problem 2: Print Numbers 1 to N

Same structure as above, just print `i` instead of a name:

```cpp
void printAscending(int i, int n) {
    if (i > n) {
        return;
    }
    cout << i << " ";
    printAscending(i + 1, n);
}

int main() {
    int n;
    cin >> n;
    printAscending(1, n);   // Output for n=4: 1 2 3 4
}
```

Nothing conceptually new here — it's the exact same pattern as Problem 1, just printing the counter itself.

---

### Problem 3: Print Numbers N to 1

Now reverse it: given `n = 4`, print `4 3 2 1`.

The trick: instead of starting at `1` and going up, start at `n` and count *down* to `1`.

```cpp
void printDescending(int i, int n) {
    if (i < 1) {           // base case: stop once we go below 1
        return;
    }
    cout << i << " ";
    printDescending(i - 1, n);   // move down instead of up
}

int main() {
    int n;
    cin >> n;
    printDescending(n, n);   // start from n itself
}
```

**Tracing for `n = 3`:**

```
printDescending(3,3) → print 3 → call printDescending(2,3)
printDescending(2,3) → print 2 → call printDescending(1,3)
printDescending(1,3) → print 1 → call printDescending(0,3)
printDescending(0,3) → 0 < 1 → return
```

Output: `3 2 1`. The only real change from Problem 2 is: start from `n` instead of `1`, decrement instead of increment, and flip the base case check accordingly.

---

### Problem 4: Print 1 to N — Without Ever Incrementing (Introducing Backtracking)

This is the interesting one. The challenge: print `1` to `n` in ascending order, **but you're only allowed to use `i - 1`** (decrementing) in your recursive call — no `i + 1` allowed.

At first this seems contradictory — how do you print in *increasing* order while only ever *decreasing* your parameter? The trick lies in **where you place the print statement relative to the recursive call.**

So far, every example printed *before* making the recursive call — meaning the printing happened during the "winding" (going-in) phase. This time, we flip it: **print *after* the recursive call**, so the printing happens during the "unwinding" (coming-back) phase instead.

```cpp
void printAsc_backtrack(int i, int n) {
    if (i < 1) {
        return;
    }
    printAsc_backtrack(i - 1, n);   // recursive call FIRST
    cout << i << " ";                 // print AFTER — on the way back
}

int main() {
    int n;
    cin >> n;
    printAsc_backtrack(n, n);   // start from n
}
```

**Tracing for `n = 3`:**

Going in (winding) — no printing happens yet, we just keep calling deeper:

```
printAsc_backtrack(3,3) → i=3, not <1 → call printAsc_backtrack(2,3) first...
printAsc_backtrack(2,3) → i=2, not <1 → call printAsc_backtrack(1,3) first...
printAsc_backtrack(1,3) → i=1, not <1 → call printAsc_backtrack(0,3) first...
printAsc_backtrack(0,3) → i=0, IS <1 → return (base case hit, nothing printed)
```

Now, coming back out (unwinding) — *this* is when the print statements actually execute, in reverse order of how the calls were made:

```
printAsc_backtrack(1,3) resumes → prints 1
printAsc_backtrack(2,3) resumes → prints 2
printAsc_backtrack(3,3) resumes → prints 3
```

Final output: `1 2 3` — ascending order, even though we only ever decremented `i`.

**Why this works:** The call that goes "deepest" (smallest `i`) is the first one to hit the base case and start returning — so it's also the first one to reach its print statement. Since we started at `n` and went down to `1`, the deepest call corresponds to `i = 1`, so it prints first, then `2`, then `3` — giving us ascending order purely from the *order in which calls complete*, not from the order they were made.

**Recursion tree (with print position marked):**

```
printAsc_backtrack(3,3)
   └── printAsc_backtrack(2,3)
          └── printAsc_backtrack(1,3)
                 └── printAsc_backtrack(0,3) → base case, return
                 ← prints 1 (on the way back)
          ← prints 2 (on the way back)
   ← prints 3 (on the way back)
```

This technique — deliberately placing work *after* the recursive call so it executes during the unwind phase — is the foundation of **backtracking**, a pattern you'll see a lot more of later in the course.

---

### Try It Yourself: Print N to 1, Using Only `i + 1`

As a follow-up exercise for students: print `n` down to `1`, but this time you're only allowed to use `i + 1` in the recursive call (the mirror image of Problem 4).

*(Hint: start `i` from `1`, and think about where the print statement needs to go — before or after the recursive call — to get descending output from an incrementing parameter.)*

---

## Summary: Lecture 2

| Problem | Direction | Print position | Key idea |
| --- | --- | --- | --- |
| Print name n times | — | before recursive call | basic parameterized recursion |
| Print 1 to n | ascending, `i+1` | before recursive call | print during winding |
| Print n to 1 | descending, `i-1` | before recursive call | flip start point + base case |
| Print 1 to n (no `i+1` allowed) | uses `i-1` only | **after** recursive call | print during unwinding → backtracking |

The single biggest takeaway from this lecture: **the order code executes in recursion depends on whether you print/act before or after the recursive call** — before means "on the way in," after means "on the way out." This one idea unlocks a huge number of recursion patterns later on.

---