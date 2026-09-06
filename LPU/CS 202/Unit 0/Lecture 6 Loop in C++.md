# Lecture 6: Loop in C++

Status: Done

# 🎯Part 1: For Loop in C++

---

## 1. Introducing Loops — Why Do We Need Them?

Suppose you're asked to print your name five times. You *could* write:

```cpp
cout << "Sachin";
cout << "Sachin";
cout << "Sachin";
cout << "Sachin";
cout << "Sachin";
```

That works — for five times. But what if you were asked to print it **100 times**? Writing the same line 100 times is clearly not a smart way to code.

> A loop lets you execute a block of code repeatedly, without writing that code again and again. You simply tell the loop how many times to run, and it does the repeating for you.
> 

> A loop allows you to execute a block of statements repeatedly until a certain condition becomes false.
> 

### The Looping Statements We'll Study

```
┌─────────────────────────────────────────────┐
│           LOOPING STATEMENTS                │
├───────────┬─────────────┬───────────────────┤
│  For Loop │ While Loop  │  Do-While Loop    │
├───────────┴─────────────┴───────────────────┤
│          For-Each Loop (later, with arrays) │
└─────────────────────────────────────────────┘

┌─────────────────────────────────────────────┐
│          JUMPING STATEMENTS                 │
├───────────┬───────────┬─────────────────────┤
│   break   │ continue  │  return (later,     │
│           │           │  with functions)    │
└─────────────────────────────────────────────┘

Plus: Infinite Loops & Nested Loops (used heavily in pattern printing)
```

We're starting today with the **for loop**.

---

## 2. What Is a For Loop?

A **for loop** is a **count-controlled loop** — meaning you use it when you already know *how many times* you want something to repeat.

Say we want to print "Hello" 5 times. Think of it step by step:

```
Start from 1 → print
Go to 2      → print
Go to 3      → print
Go to 4      → print
Go to 5      → print
Stop — don't go to 6
```

We need a **counter** (also called an **iterator**) that tracks which repetition we're on. We conventionally name it `i` — short for "iterator," since typing "iterator" every time would be tedious.

---

## 3. The Three Parts of a For Loop

Every `for` loop is built from exactly three components:

```cpp
for (initialization; condition; update) {
    // body of the loop
}
```

| Part | Purpose | Example |
| --- | --- | --- |
| **Initialization** | Where does the loop start? | `int i = 1` |
| **Condition** | Loop keeps running *as long as* this is true | `i <= 5` |
| **Update** | What happens after each pass through the loop | `i++` |

Let's map this onto our "print 1 to 5" example:

- **Initialization** → `i = 1` (start counting from 1)
- **Condition** → `i <= 5` (keep going as long as `i` is 5 or less)
- **Update** → `i++` (after each round, increase `i` by 1)

So `i` moves through: `1 → 2 → 3 → 4 → 5 → 6`. The moment `i` becomes `6`, the condition `i <= 5` turns false, and the loop stops.

---

## 4. The Flow of a For Loop

```
┌──────────────┐
│ Initialize   │   (runs only ONCE, at the very start)
└──────┬───────┘
       ▼
┌───────────────┐
│Check Condition│◄────────────┐
└──────┬────────┘             │
       │ true                 │
        ▼                     │
┌──────────────┐              │
│ Execute Body │              │
└──────┬───────┘              │
       ▼                      │
┌──────────────┐              │
│    Update    │──────────────┘
└──────────────┘
       │ condition becomes false
       ▼
   Loop Ends
```

**Important order to notice:** the condition is checked **before** the body runs each time, and the update happens **after** the body runs — right before the condition gets checked again.

---

## 5. Worked Example: Print Numbers from 1 to 5

```cpp
for (int i = 1; i <= 5; i++) {
    cout << i << " ";
}
```

### Dry Run (tracing through it step by step)

A **dry run** means manually executing the code, one step at a time, exactly as the compiler would.

| Step | `i` value | Condition `i <= 5` | Action |
| --- | --- | --- | --- |
| 1 | 1 | true | print `1`, then `i++` → `i = 2` |
| 2 | 2 | true | print `2`, then `i++` → `i = 3` |
| 3 | 3 | true | print `3`, then `i++` → `i = 4` |
| 4 | 4 | true | print `4`, then `i++` → `i = 5` |
| 5 | 5 | true | print `5`, then `i++` → `i = 6` |
| 6 | 6 | **false** | loop terminates |

**Output:** `1 2 3 4 5`

### Making It Dynamic — Take the Limit from the User

Instead of hardcoding `5`, we can ask the user how far to count:

```cpp
#include <iostream>
using namespace std;

int main() {
    int number;
    cout << "Enter a number: ";
    cin >> number;

    for (int i = 1; i <= number; i++) {
        cout << i << " ";
    }

    return 0;
}
```

If the user enters `8`, the output becomes `1 2 3 4 5 6 7 8`.

---

## 6. Running a For Loop in Reverse — Print 5 to 1

To count *downward* instead of upward, we simply flip all three parts:

| Part | Forward (1→5) | Reverse (5→1) |
| --- | --- | --- |
| Initialization | `i = 1` | `i = 5` |
| Condition | `i <= 5` | `i >= 1` |
| Update | `i++` | `i--` |

```cpp
for (int i = 5; i >= 1; i--) {
    cout << i << " ";
}
```

### Dry Run

```
i = 5 → 5 >= 1 true → print 5 → i-- → i = 4
i = 4 → 4 >= 1 true → print 4 → i-- → i = 3
i = 3 → 3 >= 1 true → print 3 → i-- → i = 2
i = 2 → 2 >= 1 true → print 2 → i-- → i = 1
i = 1 → 1 >= 1 true → print 1 → i-- → i = 0
i = 0 → 0 >= 1 false → loop terminates
```

**Output:** `5 4 3 2 1`

> A common beginner slip here: forgetting to change the update from `i++` to `i--`. If you keep `i++` while counting down from a starting value with a `>=` condition, the loop never becomes false — this creates an **infinite loop**, since `i` keeps growing further past the limit instead of shrinking toward it.
> 

### Making the Reverse Loop Dynamic

```cpp
#include <iostream>
using namespace std;

int main() {
    int n;
    cout << "Enter number: ";
    cin >> n;

    for (int i = n; i >= 1; i--) {
        cout << i << " ";
    }

    return 0;
}
```

If the user enters `7`, the output is `7 6 5 4 3 2 1`.

---

## 7. Printing Even Numbers from 1 to 20 (Using an `if` Inside a Loop)

### The Logic

We know a number is **even** if it leaves **no remainder** when divided by 2 — in other words, `number % 2 == 0`.

```
6 % 2 = 0   →  6 is even
5 % 2 = 1   →  5 is NOT even
```

### Method 1: Loop Through Every Number, Filter with `if`

```cpp
for (int i = 1; i <= 20; i++) {
    if (i % 2 == 0) {
        cout << i << " ";
    }
}
```

We can even skip one wasted check by starting from `2` (since `1` is never even anyway):

```cpp
for (int i = 2; i <= 20; i++) {
    if (i % 2 == 0) {
        cout << i << " ";
    }
}
```

### Method 2: Skip Straight to Even Numbers (No `if` Needed)

Since we already know even numbers are `2, 4, 6, 8...`, we can just **increment by 2 each time**, instead of checking every number:

```cpp
for (int i = 2; i <= 20; i += 2) {
    cout << i << " ";
}
```

Both methods give the exact same output: `2 4 6 8 10 12 14 16 18 20`

> Key insight: The **update** part of a `for` loop doesn't have to be `i++`. It can be `i += 2`, `i += 5`, `i += 10` — whatever step size fits your problem.
> 

---

## 8. Printing Numbers from 1 to 100 Divisible by 15

Same core idea as the even-numbers problem — just replace `2` with `15`.

```cpp
for (int i = 15; i <= 100; i += 15) {
    cout << i << " ";
}
```

**Why start at 15 instead of 1?** Because no number smaller than 15 (between 1–100) is divisible by 15 — so starting from 1 would just waste iterations checking numbers we already know will fail.

**Output:** `15 30 45 60 75 90`

*(Not 105 — that's outside our 1–100 range.)*

---

## 9. Printing Numbers from 10 to 100, Separated by 10

Same pattern again — just a different step size:

```cpp
for (int i = 10; i <= 100; i += 10) {
    cout << i << " ";
}
```

**Output:** `10 20 30 40 50 60 70 80 90 100`

---

## 10. Printing a Multiplication Table

### The Core Idea — Using Two Variables

To print the table of `5` (i.e., `5×1`, `5×2`, ... `5×5`), we need **two values**: one that stays **fixed** (the number whose table we want) and one that **changes** (the multiplier).

```cpp
int num = 5;

for (int i = 1; i <= 5; i++) {
    cout << num << " x " << i << " = " << num * i << endl;
}
```

Here, `num` stays constant at `5` throughout, while `i` climbs from `1` to `5`.

```
num=5, i=1 → 5 x 1 = 5
num=5, i=2 → 5 x 2 = 10
num=5, i=3 → 5 x 3 = 15
num=5, i=4 → 5 x 4 = 20
num=5, i=5 → 5 x 5 = 25
```

> It doesn't matter which variable you call "fixed" and which you call the "iterator" — the naming is your choice. What matters is that **one value stays constant** while the **other one changes**.
> 

### Full Table (1 to 10) with User Input

```cpp
#include <iostream>
using namespace std;

int main() {
    int num;
    cout << "Enter a number: ";
    cin >> num;

    for (int i = 1; i <= 10; i++) {
        cout << num << " x " << i << " = " << num * i << endl;
    }

    return 0;
}
```

If the user enters `10`, it prints the complete multiplication table of `10`, from `10 x 1 = 10` all the way to `10 x 10 = 100`.

---

## 11. Sum of the First 50 Natural Numbers

### The Logic

We want: `1 + 2 + 3 + 4 + ... + 50`

The idea: keep a **running total** (a `sum` variable), and on every pass of the loop, add the current number to it.

```
sum = 0
sum = sum + 1 = 1
sum = sum + 2 = 3
sum = sum + 3 = 6
sum = sum + 4 = 10
sum = sum + 5 = 15
...and so on, up to 50
```

> Why start `sum` at `0` and not something else? Because we haven't added anything yet — `0` is the "empty" starting point for addition. (Contrast this with multiplication problems like factorial, where the starting point needs to be `1` instead — more on that in the next lecture.)
> 

### The Code

```cpp
#include <iostream>
using namespace std;

int main() {
    int sum = 0;

    for (int i = 1; i <= 50; i++) {
        sum = sum + i;
    }

    cout << "Sum of first 50 natural numbers is: " << sum;
    return 0;
}
```

**Output:** `Sum of first 50 natural numbers is: 1275`

### Making the Limit Dynamic

```cpp
#include <iostream>
using namespace std;

int main() {
    int num, sum = 0;
    cout << "Enter a number: ";
    cin >> num;

    for (int i = 1; i <= num; i++) {
        sum = sum + i;
    }

    cout << "Sum of first " << num << " natural numbers is: " << sum;
    return 0;
}
```

If the user enters `100`, the program calculates the sum of the first 100 natural numbers instead.

---

## Key Points to Remember

- A **loop** repeats a block of code without you having to write it multiple times.
- A `for` loop has **three components**: **Initialization** (where to start), **Condition** (when to keep going), and **Update** (how to move forward each time).
- The condition is checked **before** every execution of the loop body; if it's false even the very first time, the body never runs at all.
- To reverse a loop's direction, flip all three parts: start from the high end, use `>=`, and decrement (`i--`) instead of incrementing.
- The **update** doesn't have to be `i++` — it can be `i--`, `i += 2`, `i += 10`, or anything the problem calls for.
- Combining an `if` condition inside a `for` loop lets you selectively act on only *some* of the values the loop passes through (e.g., picking out even numbers).
- Two-variable loops (one constant, one changing) are the pattern behind multiplication tables.
- A **dry run** — manually tracing through code step by step — is the best way to understand *and debug* loop behavior.

# 🎯Part 2 — Factorial & Fibonacci

---

## 1. Factorial Using a For Loop

### What Is a Factorial?

The **factorial** of a number is what you get when you multiply every whole number from that number all the way down to `1`.

> Factorial of a number n is: n! = n × (n-1) × (n-2) × ... × 1
> 

For example:

```
Factorial of 5 = 5 × 4 × 3 × 2 × 1 = 120
Factorial of 3 = 3 × 2 × 1 = 6
```

| n | n! |
| --- | --- |
| 5 | 120 |
| 4 | 24 |
| 0 | 1 *(special rule)* |

> Important rule: 0! = 1 — this is a mathematical convention worth memorizing, since it comes up in edge-case questions.
> 

### The Logic — Similar to the "Sum" Problem, But With Multiplication

In the previous lecture, we calculated the **sum** of numbers by looping and *adding* each number to a running total. Factorial uses the **exact same loop structure** — except instead of adding, we **multiply**.

```
Sum logic:       sum = sum + i   (running total via addition)
Factorial logic: fact = fact * i   (running total via multiplication)
```

### Why Start `fact` at `1`, Not `0`?

This is a subtle but important detail:

> If you add something to zero, it stays meaningful (0 + 5 = 5). But if you multiply something by zero, the entire answer becomes zero (5 × 0 = 0) — no matter what else you multiply afterward.
> 

So we must **initialize `fact = 1`**, not `0`, otherwise every multiplication would collapse to zero immediately.

### Why `long long` Instead of `int`?

Factorials grow **very fast** — even `20!` is a massive number that overflows a normal `int`. That's why we declare the factorial variable as `long long`:

> Just like `double` stores very large floating-point values, `long long` stores very large integer values.
> 

### Dry Run — Factorial of 5

| Step | `i` | Condition `i <= 5` | `fact = fact * i` | New `fact` |
| --- | --- | --- | --- | --- |
| Start | — | — | — | `fact = 1` |
| 1 | 1 | true | `1 * 1` | `1` |
| 2 | 2 | true | `1 * 2` | `2` |
| 3 | 3 | true | `2 * 3` | `6` |
| 4 | 4 | true | `6 * 4` | `24` |
| 5 | 5 | true | `24 * 5` | `120` |
| 6 | 6 | **false** | — | loop ends |

**Final Output:** `Factorial = 120`

### The Full Code (with Negative-Number Validation)

Since factorial isn't defined for negative numbers, it's good practice to check for that first using an `if-else`:

```cpp
#include <iostream>
using namespace std;

int main() {
    int n;
    long long fact = 1;

    cout << "Enter the number: ";
    cin >> n;

    if (n < 0) {
        cout << "Factorial can't be found for negative numbers" << endl;
    }
    else {
        for (int i = 1; i <= n; i++) {
            fact = fact * i;
        }
        cout << "Factorial for given number is " << fact;
    }

    return 0;
}
```

### The Simplified Version (Matching the Instructor's Notes)

```cpp
#include <iostream>
using namespace std;

int main() {
    int n;
    long long fact = 1;

    cout << "Enter a number: ";
    cin >> n;

    for (int i = 1; i <= n; i++) {
        fact *= i;
    }

    cout << "Factorial = " << fact;
    return 0;
}
```

*(Notice `fact *= i;` here — this is the compound multiplication-assignment operator from Lecture 4, meaning exactly `fact = fact * i;`.)*

**Explanation:**

- Start with `fact = 1`
- Multiply `fact` by each number from `1` to `n`
- The loop repeats exactly `n` times

---

## 2. Fibonacci Series Using a For Loop

### What Is the Fibonacci Series?

> The Fibonacci series starts with 0 and 1. Every number after that is the sum of the previous two numbers.
> 

```
0, 1, 1, 2, 3, 5, 8, 13, 21, 34, ...
```

Let's see exactly how each term is built:

```
0 + 1 = 1     → 3rd term
1 + 1 = 2     → 4th term
1 + 2 = 3     → 5th term
2 + 3 = 5     → 6th term
3 + 5 = 8     → 7th term
5 + 8 = 13    → 8th term
8 + 13 = 21   → 9th term
13 + 21 = 34  → 10th term
```

### What Does "n Terms" Mean?

If someone asks for the Fibonacci series **up to 5 terms**, they mean: give me the 1st, 2nd, 3rd, 4th, and 5th numbers in the series — **not** "count up to the number 5."

```
5 terms → 0, 1, 1, 2, 3        (five numbers total)
8 terms → 0, 1, 1, 2, 3, 5, 8, 13   (eight numbers total)
```

### The Core Logic

We need **three variables**:

| Variable | Purpose |
| --- | --- |
| `a` | holds the "first" of the two previous terms |
| `b` | holds the "second" of the two previous terms |
| `next` (or `c`) | holds the newly calculated term |

The pattern, repeated every time a new term is needed:

```
Step 1: next = a + b        (calculate the new term)
Step 2: print next
Step 3: a = b                (shift a forward)
Step 4: b = next             (shift b forward)
```

### Why Does the Loop Start From `i = 3`?

Because the **first two terms** (`0` and `1`) are already known and printed *before* the loop even begins — we don't need to calculate them, only the terms *after* them.

```
Term 1 → 0   (printed directly, before loop)
Term 2 → 1   (printed directly, before loop)
Term 3 → calculated inside loop  ← loop starts here
Term 4 → calculated inside loop
...
Term n → calculated inside loop
```

So if the user asks for `n` terms total, and 2 are already handled, the loop only needs to run from `3` up to `n`.

### Dry Run — Fibonacci up to 6 Terms

| Term | `a` (before) | `b` (before) | `next = a + b` | Printed | `a` (after) | `b` (after) |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | — | — | — | `0` | — | — |
| 2 | — | — | — | `1` | — | — |
| 3 | 0 | 1 | 1 | `1` | 1 | 1 |
| 4 | 1 | 1 | 2 | `2` | 1 | 2 |
| 5 | 1 | 2 | 3 | `3` | 2 | 3 |
| 6 | 2 | 3 | 5 | `5` | 3 | 5 |

**Output:** `0 1 1 2 3 5`

### The Full Code

```cpp
#include <iostream>
using namespace std;

int main() {
    int n;
    cout << "Enter number of terms: ";
    cin >> n;

    int a = 0, b = 1;
    cout << a << " " << b << " ";

    for (int i = 3; i <= n; i++) {
        int c = a + b;
        cout << c << " ";
        a = b;
        b = c;
    }

    return 0;
}
```

**Sample run:** If the user enters `10`, the loop runs from `i = 3` to `i = 10` (8 iterations), producing:

```
0 1 1 2 3 5 8 13 21 34
```

### Explanation Recap

- Start with `a = 0`, `b = 1` — the first two terms — and print both immediately.
- Each new term is computed as `c = a + b`.
- After printing `c`, "shift" the window forward: `a` takes `b`'s old value, and `b` takes the newly calculated `c`.
- The loop runs from `3` to `n`, since the first two terms were already handled outside the loop.

---

## Key Points to Remember

- **Factorial** uses the same loop structure as a running sum, but with **multiplication** instead of addition — so the accumulator must start at `1`, not `0` (multiplying by zero would zero out everything).
- Factorials grow extremely fast, so use `long long` instead of `int` to avoid overflow.
- Always consider validating input — factorial isn't defined for negative numbers.
- The **Fibonacci series** starts with two fixed terms, `0` and `1`; every term after that is the **sum of the previous two**.
- "n terms" means the count of numbers in the sequence, not counting up to the number `n` itself.
- Fibonacci needs **three variables**: two to hold the previous terms (`a`, `b`) and one to hold the newly computed term (`next`/`c`) — and after each calculation, you must "shift" `a` and `b` forward.
- The Fibonacci loop starts from `i = 3` because the first two terms are already printed before the loop begins.
- Both factorial and Fibonacci reinforce the same core skill from Lecture 6 Part 1: tracking a running value across iterations using a `for` loop — the difference is only in *what* operation updates that value each time.

# 🎯While Loop in C++

---

## 1. What Is a While Loop?

> A while loop is very similar to a for loop. In a for loop, the initialization, condition, and update all sit together in the loop's header. In a while loop, that's not the case — you only put the **condition**.
> 

> The while loop repeats a block of code as long as a specified condition is true. As long as this condition remains true, the code inside keeps executing, again and again, until the condition finally becomes false.
> 

### Syntax

```cpp
while (condition) {
    // statements
}
```

> If it's a single statement, curly braces aren't strictly required. But for multiple statements, you use curly braces to form a **block statement** — same rule we learned back in Lecture 1.
> 

> Condition is checked **before** executing the loop body — this makes it an **entry-controlled loop**. (Contrast this with the do-while loop from an earlier lecture, which is exit-controlled — it checks the condition *after* running the body at least once.)
> 

---

## 2. For Loop vs. While Loop — Same Three Ingredients, Different Layout

Even though the `while` loop's syntax only shows a condition, it still relies on the same **three components** as a `for` loop: **initialization**, **condition**, and **update** — they're just placed in different spots.

```cpp
// For loop: all three parts together in the header
for (int i = 1; i <= 5; i++) {
    cout << i << endl;
}

// While loop: same three parts, but spread out
int i = 1;              // initialization — happens BEFORE the loop starts
while (i <= 5) {         // condition — sits inside the while()
    cout << i << endl;
    i++;                   // update — written inside the loop body
}
```

> Notice: `i = 1` must be initialized **before** the while loop begins — the while loop syntax has no place for initialization. And the update (`i++`) must be written explicitly inside the loop body — if you forget it, `i` stays stuck at its starting value forever, causing an **infinite loop**.
> 

---

## 3. The Flow of a While Loop

```
┌──────────────┐
│    Start      │
└──────┬───────┘
       ▼
┌──────────────┐
│Check Condition│◄─────────────┐
└──────┬───────┘               │
       │ true                   │
       ▼                        │
┌──────────────┐               │
│ Execute Code  │──────────────┘
└──────┬───────┘
       │ condition becomes false
       ▼
     Exit
```

> Come to the condition, check if it's true — if true, execute the code inside and check the condition again. Repeat this until the condition becomes false, at which point the loop breaks and execution moves past it.
> 

---

## 4. `Example 1` — Print Numbers 1 to 5 (For Loop vs. While Loop)

### The For Loop Version (Recap)

```cpp
for (int i = 1; i <= 5; i++) {
    cout << i << endl;
}
```

### The While Loop Version

```cpp
int i = 1;              // initialize BEFORE the loop

while (i <= 5) {          // condition only
    cout << i << endl;
    i++;                    // update, written inside the body
}
```

### Tracing Through It

```
i = 1 → is 1 <= 5? Yes → print 1 → i++  → i = 2
i = 2 → is 2 <= 5? Yes → print 2 → i++  → i = 3
i = 3 → is 3 <= 5? Yes → print 3 → i++  → i = 4
i = 4 → is 4 <= 5? Yes → print 4 → i++  → i = 5
i = 5 → is 5 <= 5? Yes → print 5 → i++  → i = 6
i = 6 → is 6 <= 5? No  → loop terminates
```

**Output:** `1 2 3 4 5`

### The Full Code

```cpp
#include <iostream>
using namespace std;

int main() {
    int i = 1;
    while (i <= 5) {
        cout << i << " ";
        i++;
    }
    return 0;
}
```

### Making It Dynamic

```cpp
int num;
cout << "Enter the number: ";
cin >> num;

int i = 1;
while (i <= num) {
    cout << i << " ";
    i++;
}
```

If the user enters `10`, this prints `1 2 3 4 5 6 7 8 9 10`.

---

## 5. `Example 2` — Print Even Numbers Between 1 and 10

Same core logic as the `for`-loop version from Lecture 6 — just restructured with a `while` loop.

```cpp
int i = 1;

while (i <= 10) {
    if (i % 2 == 0) {
        cout << i << " ";
    }
    i++;
}
```

### Tracing Through It

```
i=1 → 1<=10 true → 1%2==0? No  → don't print → i++ → i=2
i=2 → 2<=10 true → 2%2==0? Yes → print 2       → i++ → i=3
i=3 → 3<=10 true → 3%2==0? No  → don't print → i++ → i=4
i=4 → 4<=10 true → 4%2==0? Yes → print 4       → i++ → i=5
... and so on
```

**Output:** `2 4 6 8 10`

> Notice the `if` check sits **inside** the while loop's body — this is exactly the same "if inside a loop" pattern used for filtering values that we first saw with `for` loops.
> 

---

## 6. Input Validation — A Signature Use Case for `while`

> This is one of the most common real-world use cases of the while loop: repeatedly asking the user for input **until** they provide something valid.
> 

### `Example 1` — Validating a Number Under 100

```cpp
int num;
cout << "Enter a number less than 100: ";
cin >> num;

while (num >= 100) {
    cout << "Invalid, enter a number less than 100: ";
    cin >> num;
}

cout << "You entered: " << num;
```

> The logic: **keep looping while the condition indicating "invalid input" is true**. Here, the loop only runs when `num >= 100` — meaning as soon as the user finally enters something below 100, the condition turns false and the loop stops.
> 

You could also flip this using logical NOT (from Lecture 4):

```cpp
while (!(num < 100)) {
    // same effect: keep looping as long as num is NOT less than 100
}
```

### `Example 2` — Validating a Range (Using Logical OR)

```cpp
int num;
cout << "Enter an integer between 1 and 5: ";
cin >> num;

while (num < 1 || num > 5) {
    cout << "Invalid, enter an integer between 1 and 5: ";
    cin >> num;
}

cout << "Thanks";
```

> Here, **logical OR** (`||`) is doing the real work — from Lecture 4: OR is true if *either* condition holds. So the loop keeps running if the number is **too low** (`< 1`) **OR too high** (`> 5`). The moment the number falls anywhere in the valid range (`1` to `5`), both conditions become false, OR becomes false, and the loop exits.
> 

### `Example 3` — Validating Marks Between 0 and 100

```cpp
int marks;
cout << "Enter marks (0-100): ";
cin >> marks;

while (marks < 0 || marks > 100) {
    cout << "Invalid marks! Enter again: ";
    cin >> marks;
}

cout << "Final Marks: " << marks;
```

> This mirrors a real constraint: marks are only meaningful between 0 and 100 (you can't score negative marks, and you can't exceed the total). The while loop simply won't let the program move forward until the input actually makes sense.
> 

---

## 7. `Example 3` — Countdown (Reverse Counting)

> A countdown is nothing but printing numbers in **reverse order** — for example, `10, 9, 8, ... 1`.
> 

```cpp
int num;
cout << "Enter countdown number: ";
cin >> num;

while (num >= 0) {
    cout << num << " ";
    num--;
}
```

### Tracing Through It (starting at 10)

```
num=10 → 10>=0 true → print 10 → num-- → num=9
num=9  → 9>=0  true → print 9  → num-- → num=8
...
num=0  → 0>=0  true → print 0  → num-- → num=-1
num=-1 → -1>=0 false → loop terminates
```

**Output:** `10 9 8 7 6 5 4 3 2 1 0`

> Notice the countdown **includes 0** — since the condition is `num >= 0`, zero itself still satisfies it and gets printed before the loop stops.
> 

---

## 8. `Example 4` — Print the Multiplication Table (While Loop Version)

```cpp
int n;
cin >> n;

int i = 1;
while (i <= 10) {
    cout << n << " x " << i << " = " << n * i << endl;
    i++;
}
```

This is exactly the multiplication table logic from Lecture 6 — same "one variable fixed, one variable changing" pattern — just written with a `while` loop instead of a `for` loop.

---

## 9. `Example 5` — Sum of the First N Natural Numbers

```cpp
int n;
cin >> n;

int sum = 0;
int i = 1;

while (i <= n) {
    sum = sum + i;
    i++;
}

cout << "Sum = " << sum;
```

### Tracing Through It (n = 5)

| `i` | Condition `i <= n` | `sum = sum + i` | New `sum` |
| --- | --- | --- | --- |
| 1 | true (1≤5) | 0 + 1 | 1 |
| 2 | true (2≤5) | 1 + 2 | 3 |
| 3 | true (3≤5) | 3 + 3 | 6 |
| 4 | true (4≤5) | 6 + 4 | 10 |
| 5 | true (5≤5) | 10 + 5 | 15 |
| 6 | **false** (6≤5) | — | loop ends |

**Output:** `Sum = 15`

> This is precisely the same "running total" logic from Lecture 6's for-loop sum problem — the instructor deliberately revisits it here to show that the **underlying logic doesn't change** between a `for` loop and a `while` loop; only the syntax layout does.
> 

---

## 10. The Infinite Loop Trap (Specific to `while`)

> A while loop becomes infinite when its condition **never becomes false** — most commonly because the **update step was forgotten**.
> 

```cpp
int i = 1;
while (i <= 5) {
    cout << i;
    // missing i++  ← BUG: i never changes, condition stays true forever
}
```

> Since `i` is never incremented here, `i <= 5` remains true on every single check — the loop runs forever, endlessly printing `1`. This is exactly the kind of bug the instructor warned about earlier: **always make sure your update step is actually inside the loop body**, since a `while` loop (unlike a `for` loop) doesn't force you to write the update anywhere specific — it's easy to forget.
> 

---

## Key Points to Remember

- A **while loop** repeats code **as long as a condition is true** — but unlike the `for` loop, only the **condition** sits in its syntax; **initialization** happens *before* the loop, and **update** must be written explicitly *inside* the loop body.
- It's an **entry-controlled loop**: the condition is checked *before* the body ever runs, so if the condition is false from the very start, the loop body never executes at all — not even once (contrast with `do-while`).
- Forgetting the **update step** inside a `while` loop is the most common cause of an accidental **infinite loop**, since nothing in the syntax forces you to remember it.
- **Input validation** is a classic, powerful use case: keep looping (re-prompting the user) while their input fails a condition, and only proceed once it passes.
- Logical **OR** (`||`) is especially useful for range validation — checking "too low OR too high" in a single condition — while logical **AND** or **NOT** (`!`) can express the same idea in different equivalent forms.
- A **countdown** is just a reverse-counting while loop: start high, decrement (`num--`), and continue `while (num >= 0)`.
- Every problem we already solved with `for` loops in Lecture 6 (printing ranges, even numbers, multiplication tables, sum of natural numbers) can be rewritten with a `while` loop — the **core logic is identical**; only the placement of initialization/condition/update changes.

# 🎯 Part 3: Jumping Statements — `break` and `continue`

---

## 1. What Are Jumping Statements?

A **jumping statement** does exactly what it sounds like — it makes the program "jump" out of, or skip over, part of its normal flow. There are three jumping statements in C++:

```
┌───────────────────────────────────────────────┐
│              JUMPING STATEMENTS                   │
├─────────────┬─────────────┬───────────────────┤
│    break     │  continue    │      return         │
├─────────────┼─────────────┼───────────────────┤
│ Exits the    │ Skips the     │ Exits a function   │
│ loop         │ current       │ (covered later,    │
│ immediately  │ iteration     │ with functions)    │
└─────────────┴─────────────┴───────────────────┘
```

We'll only cover `break` and `continue` today, since both are used **inside loops**. `return` belongs to functions, which we haven't studied yet — so we'll pick that up when we get there.

---

## 2. The `break` Statement

### What Does `break` Do?

> The break statement terminates the loop it's inside, immediately — right at the point where it's written.
> 

Think of it like this: suppose you're running a loop from `1` to `5`, but you want the loop to stop completely the moment `i` becomes `3` — meaning `4` and `5` should never even be checked or printed.

```cpp
for (int i = 1; i <= 5; i++) {
    if (i == 3) {
        break;
    }
    cout << i << " ";
}
```

### Tracing Through It

```
i = 1 → is i == 3? No  → print 1
i = 2 → is i == 3? No  → print 2
i = 3 → is i == 3? Yes → break! → loop exits immediately
```

Notice: when `i` hits `3`, the `break` fires **before** the `cout` line even gets a chance to run. So `3` itself never gets printed — the loop just stops dead at that point.

**Output:** `1 2`

```
┌───────────────────────────────────────┐
│  for (i = 1 to 5)                         │
│     if (i == 3) → break ──────┐          │
│     cout << i                  │          │
│                                ▼          │
│                          LOOP ENDS        │
│                          (4, 5 never run) │
└───────────────────────────────────────┘
```

### Where Is `break` Actually Used?

The instructor points out two very common real-world uses:

1. **Stopping infinite loops** — since an infinite loop has no natural end, `break` is often the *only* way to exit it (we'll see this properly in the next lecture).
2. **Inside `switch` statements** — as covered back in Lecture 5, `break` is what stops a `switch` from "falling through" into the next case.

---

## 3. The `continue` Statement

### What Does `continue` Do?

> Unlike break, continue does NOT stop the loop. It only skips the current iteration and moves on to the next one.
> 

Suppose you want to print all numbers from `1` to `5`, **except** `4`. You still want `1, 2, 3, 5` to print — the loop shouldn't stop, it should just skip over `4` specifically.

```cpp
for (int i = 1; i <= 5; i++) {
    if (i == 4) {
        continue;
    }
    cout << i << " ";
}
```

### Tracing Through It

```
i = 1 → is i == 4? No  → print 1
i = 2 → is i == 4? No  → print 2
i = 3 → is i == 4? No  → print 3
i = 4 → is i == 4? Yes → continue → skip the cout, jump to next iteration
i = 5 → is i == 4? No  → print 5
```

**Output:** `1 2 3 5`

```
┌───────────────────────────────────────────────┐
│  for (i = 1 to 5)                             │
│     if (i == 4) → continue ────┐              │
│     cout << i                  │              │
│           ▲                    │              │
│           └────────────────────┘              │
│      (skips ONLY this cout for i=4,           │
│       then moves to i++ and next check)       │
└───────────────────────────────────────────────┘
```

> The key difference to lock in: `break` says "stop the whole loop, right now." `continue` says "skip just this one round, but keep the loop going."
> 

---

## 4. `break` vs `continue` — Side by Side

| Feature | `break` | `continue` |
| --- | --- | --- |
| Effect | Terminates the **entire loop** immediately | Skips **only the current iteration** |
| Remaining iterations | Never run | Still run normally |
| Common use case | Stopping infinite loops, exiting `switch` cases | Skipping specific values while keeping the loop alive |

### A Combined Example to See the Contrast

```cpp
// Using break
for (int i = 1; i <= 5; i++) {
    if (i == 3) break;
    cout << i << " ";
}
// Output: 1 2        (loop stops completely at i=3)

// Using continue
for (int i = 1; i <= 5; i++) {
    if (i == 3) continue;
    cout << i << " ";
}
// Output: 1 2 4 5    (only 3 is skipped, loop keeps running)
```

---

## Key Points to Remember

- **Jumping statements** alter the normal flow of a program: `break`, `continue`, and `return` (return is for functions, covered later).
- **`break`** immediately terminates the loop it's written inside — no further iterations happen at all, even if the loop's condition would otherwise still be true.
- **`continue`** skips only the *current* iteration's remaining code and jumps straight to the next iteration — the loop itself keeps running.
- `break` is commonly used to escape infinite loops and to stop `switch` statements from falling through (as seen in Lecture 5).
- Always place the `if` condition that triggers `break`/`continue` **before** the code you want to skip — since C++ executes top to bottom, anything written after the `break`/`continue` in that same iteration simply won't run.

# 🎯Do-While Loop, Infinite Loops

---

## 1. While Loop vs. Do-While Loop — The Core Difference

Before understanding `do-while`, it helps to first understand what a plain **while loop** means:

> A while loop means: while this condition is true, do this following code. "While" comes first, "do" comes after.
> 

Now, a **do-while loop** simply **reverses this order**:

> A do-while loop means: "do" comes first, and the "while" condition comes after.
> 

This small reordering creates one very important behavioral difference:

| Loop Type | Behavior |
| --- | --- |
| **while** | Checks the condition **first**. If it's false right away, the code inside **never runs**, not even once. |
| **do-while** | Runs the code **first**, and *then* checks the condition. So the code always runs **at least once**, no matter what. |

> The key difference: in a do-while loop, the code executes at least once, whether the condition turns out to be true or false — because the compiler runs the code line by line, sees "do" first, executes it, and only afterward checks the "while" condition.
> 

### Syntax

```cpp
do {
    // code to execute
} while (condition);
```

**Important:** notice the semicolon (`;`) after `while (condition)` — this is required and easy to forget.

---

## 2. Worked Example: A Basic Do-While

```cpp
int i = 10;

do {
    cout << "i is " << i << endl;
    i++;
} while (i < 5);
```

### Tracing Through It

```
i = 10
→ enter the "do" block directly (no condition check yet)
→ print "i is 10"
→ i++  → i becomes 11
→ NOW check the while condition: is i < 5?  → 11 < 5 → FALSE
→ loop does not run again
```

**Output:** `i is 10`

> Notice: even though the condition `i < 5` was false from the very start (since `i` was 10), the code inside the `do` block still ran **once** — that one-time guaranteed execution is the entire point of a do-while loop.
> 

---

## 3. A Second Worked Example — Counting Up

```cpp
int i = 1;

do {
    cout << "i is " << i << endl;
    i++;
} while (i < 5);
```

### Tracing Through It

```
i = 1 → print "i is 1" → i++ → i = 2 → check: 2 < 5? true
i = 2 → print "i is 2" → i++ → i = 3 → check: 3 < 5? true
i = 3 → print "i is 3" → i++ → i = 4 → check: 4 < 5? true
i = 4 → print "i is 4" → i++ → i = 5 → check: 5 < 5? FALSE
→ loop terminates
```

**Output:**

```
i is 1
i is 2
i is 3
i is 4
```

> Notice it stops at 4, not 5 — because the condition `i < 5` becomes false exactly when `i` reaches `5`. If you wanted it to also print 5, you'd need the condition to be `i <= 5` instead.
> 

### What If the Condition Is Wrong From the Start?

```cpp
int i = 1;

do {
    cout << "i is " << i << endl;
    i++;
} while (i > 5);
```

Here, `i > 5` is false the very first time it's checked (since `i` becomes `2`, and `2 > 5` is false). But because it's a **do-while**, the body still runs **once** before that check happens.

**Output:** `i is 1`

*(then the loop stops, since the condition fails immediately after)*

---

## 4. Practical Use Case: Input Validation

This is the classic real-world scenario for `do-while`: you want to **ask the user for input at least once**, and if their input is invalid, **keep asking again**.

### The Logic

```
Ask "Enter a positive number"
Take input
Is the number less than 0?
   ├── Yes → ask again (loop runs again)
   └── No  → done, move to the next line
```

We *must* ask at least once — there's no way to validate input without first getting some input — which is exactly why `do-while` fits this problem better than a plain `while` loop.

### The Code

```cpp
#include <iostream>
using namespace std;

int main() {
    int num;

    do {
        cout << "Enter a positive number: ";
        cin >> num;
    } while (num < 0);

    cout << "The positive number is " << num;
    return 0;
}
```

### Tracing Through a Sample Run

```
Enter a positive number: -10   → -10 < 0 → true  → ask again
Enter a positive number: -5    → -5 < 0  → true  → ask again
Enter a positive number: -3    → -3 < 0  → true  → ask again
Enter a positive number: 2     → 2 < 0   → false → loop ends
```

**Output:** `The positive number is 2`

> This pattern — do the thing, then check if it needs repeating — is the signature use case for `do-while`: whenever you need something to happen **at least once**, before you even know whether it should repeat.
> 

---

## 5. The Infinite Loop

### What Is an Infinite Loop?

> If a loop's condition remains true forever, the loop runs infinitely — it never naturally stops.
> 

The clearest way to create one deliberately:

```cpp
while (true) {
    cout << "Server is running" << endl;
}
```

Since the literal value `true` can never become `false`, this loop has no natural exit point — it will run forever unless something *inside* it forces a stop.

### How Do You Stop an Infinite Loop?

> To prevent infinite loops from running forever, we use the break statement.
> 

This is exactly the `break` we studied in the previous lecture — it's often the *only* way to escape a loop that has no built-in ending condition.

### Infinite Loops with a `for` Loop

You don't need a `while (true)` to create an infinite loop — simply leaving out the exit condition in a `for` loop achieves the same thing:

```cpp
for (;;) {
    cout << "Running";
}
```

Here, all three parts (initialization, condition, update) are left empty — with no condition to ever check, the loop just runs forever.

```
┌───────────────────────────────────────────┐
│         INFINITE LOOP PATTERNS            │
├───────────────────────┬───────────────────┤
│    while (true) { }   │   for (;;) { }    │
├───────────────────────┴───────────────────┤
│   Both loop forever unless a break        │
│   statement is placed inside to escape.   │
└───────────────────────────────────────────┘
```

---

## 6. Combining Infinite Loops with `switch` — A Calculator Program

### The Idea

A really practical use of `while(true)` + `break` is building a **menu-driven program** — like a simple calculator that keeps offering the user options ("Add, Subtract, Multiply, Divide") **repeatedly**, until the user chooses to stop.

The general shape of this pattern:

```
LOOP FOREVER:
    show menu (add / subtract / multiply / divide / exit)
    take user's choice
    switch on that choice:
        case 'add'      → perform addition
        case 'subtract' → perform subtraction
        case 'multiply' → perform multiplication
        case 'divide'   → perform division
        case 'exit'     → break out of the infinite loop entirely
```

This is exactly why `break` is so important inside a `switch` that's sitting inside an infinite loop: without it, you'd have no way to ever stop the program once it starts.

> This "ask again and again until the user says stop" pattern is the same underlying idea as the do-while input-validation example — just extended with a `switch` to offer multiple operations instead of validating a single number.
> 

---

## Key Points to Remember

- A **while loop** checks its condition *before* running the code — the body may never run at all if the condition starts out false.
- A **do-while loop** runs its code *first*, and checks the condition *afterward* — guaranteeing the body executes **at least once**, no matter what.
- Syntax: `do { ... } while (condition);` — don't forget the semicolon after the `while`.
- The classic use case for `do-while` is **input validation**: you must take input at least once before you can even check whether it's valid.
- An **infinite loop** happens when a loop's condition can never become false — commonly written as `while (true)` or `for (;;)`.
- The `break` statement is typically what's used to escape an infinite loop from the inside — otherwise, it truly never stops.
- Combining an infinite loop with a `switch` statement is the pattern behind simple menu-driven programs (like a calculator) — the loop keeps showing the menu, and `break` (tied to an "exit" choice) is what finally lets the user leave.