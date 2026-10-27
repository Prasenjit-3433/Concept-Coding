# Introduction to Recursion | Recursion Tree | Stack Space

Status: Done

## 1. What Exactly Is Recursion?

**Recursion = a function that calls itself**, breaking a problem into a smaller version of the same problem, until the problem becomes so small it can be answered directly.

That's the core idea. But *how* does *"a function calling itself"* actually behave when the program runs? Let's build this up slowly with the simplest possible example: a function that just prints `1`, and then calls itself again.

```cpp
void f() {
    cout << 1;
    f();   // calling itself
}

int main() {
    f();
}
```

**What actually happens when this runs?**

1. `main()` calls `f()`.
2. `f()` runs its first line → prints `1`.
3. `f()` runs its second line → calls `f()` again.
4. This *new* call to `f()` also prints `1`, then calls `f()` again.
5. This keeps happening — forever.

**Why forever?** Because there is nothing in this function telling it *when to stop*. It has no exit condition, so it just keeps calling itself again and again.

```
f() → prints 1 → calls f()
        f() → prints 1 → calls f()
                f() → prints 1 → calls f()
                        f() → prints 1 → calls f() ...  (**never stops**)
```

This is called **infinite recursion**, and if you actually run code like this, it won't run forever in practice — it will crash. Which brings us to the next concept: *why* does it crash?

---

## 2. Why Infinite Recursion Crashes: Stack Space & Stack Overflow

Here's the important part most beginners miss: when `f()` calls `f()` again, the **first call doesn't finish** — it's sitting there, paused, *waiting* for the inner call to complete before it can finish itself.

**Real-life analogy:** Imagine you ask a friend a question, and to answer it, your friend has to ask *their* friend the same question, who has to ask *their* friend, and so on. Nobody can give you an answer until someone, somewhere down the chain, actually answers directly. Everyone up the chain is standing there, **waiting**, holding their question open, unable to leave.

That *"waiting" needs to be tracked somewhere in memory*. The place where the program keeps track of all these paused, not-yet-completed function calls is called the **`call stack`** (or just "the stack").

```
Call Stack (each box = one paused, incomplete call to f):

┌─────────────┐
│ f()  ← waiting for the call below to finish
├─────────────┤
│ f()  ← waiting for the call below to finish
├─────────────┤
│ f()  ← waiting for the call below to finish
├─────────────┤
│ f()  ← waiting...
├─────────────┤
│    ...      │
└─────────────┘
```

A call is only considered "done" once its **last line has fully executed**. But in our infinite example, the last line is *another call to f()* — which never finishes — so **none** of these calls ever get to complete. They just keep piling up in the stack.

The stack has a **limited amount of memory**. It cannot hold an infinite number of waiting calls. Once it runs out of space, the program crashes with an error commonly called a **stack overflow** (you may also see it called a "segmentation fault").

**So: infinite recursion = infinite waiting calls = stack fills up = crash.**

This is exactly *why* every recursive function needs a way to stop.

---

## 3. The Base Case: How Recursion Actually Stops

The condition that tells a recursive function "stop calling yourself now, just return" is called the **base case**.

Let's modify our earlier example so it only prints `0`, `1`, `2` and then stops — using a counter.

```cpp
int count = 0;   // global variable, starts at 0

void f() {
    if (count == 3) {      // base case: stop condition
        return;
    }
    cout << count << " ";  // do the actual work
    count++;
    f();                     // recursive call
}

int main() {
    f();
}
// Output: 0 1 2
```

**Let's trace through this very carefully, step by step:**

| Call | Is `count == 3`? | Action |
| --- | --- | --- |
| 1st call to `f()` | count = 0 → No | print `0`, count becomes 1, call `f()` again |
| 2nd call to `f()` | count = 1 → No | print `1`, count becomes 2, call `f()` again |
| 3rd call to `f()` | count = 2 → No | print `2`, count becomes 3, call `f()` again |
| 4th call to `f()` | count = 3 → **Yes** | `return` immediately — nothing else in this call runs |

The moment the 4th call hits `return`, that call is instantly finished. Since it's finished, the 3rd call (which was waiting on it) can now also finish. Since the 3rd call is done, the 2nd call can finish. And so on, all the way back up to `main()`.

```
GOING IN (winding)              COMING BACK (unwinding)
f() call 1 — prints 0 ─┐                               ┌─ done, return
f() call 2 — prints 1 ─┤   waiting...                  ├─ done, return
f() call 3 — prints 2 ─┤                               ├─ done, return
f() call 4 — count==3 ─┘ ← base case hit, returns ─────┘
```

This is the key insight: **going "in" (calling deeper) is called winding, and coming back "out" (returning) is called unwinding.** Every recursive function has both phases.

⚠️ **Important:** There can be a single base case, or sometimes multiple base cases (different stop conditions for different situations) — we'll see examples of that as we go.

---

## 4. The Recursion Tree

Tracing through every single function call by literally re-writing the whole function body each time (like the table above) gets tedious fast. So instead, we use a shorthand diagram called a **recursion tree** — it just shows which call led to which call, and where things returned.

For the example above, the recursion tree looks like this:

```
f() [count=0]
  └── f() [count=1]
        └── f() [count=2]
              └── f() [count=3] → base case hit → return
```

Each node is one function call. An arrow going down means "this call made this next call." Once you hit the base case, that branch stops and starts returning back up.

You'll see recursion trees constantly going forward in DSA — they're the standard way to visualize and reason about how a recursive function behaves, especially once functions start making *multiple* recursive calls (not just one).

---

## 5. Summary So Far

| Concept | Meaning |
| --- | --- |
| Recursion | a function calling itself to solve a smaller version of the same problem |
| Infinite recursion | recursion with no stop condition — keeps calling forever |
| Stack / stack space | where the program keeps track of function calls that have started but not yet finished |
| Stack overflow | crash caused by too many incomplete calls piling up in the stack (usually from missing/broken base case) |
| Base case | the condition that stops the recursion and lets calls start returning |
| Winding | the phase of going deeper into recursive calls |
| Unwinding | the phase of returning back out once the base case is hit |
| Recursion tree | a diagram showing which call led to which, used to trace recursion visually |

---