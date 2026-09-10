# Excercise: Placement Problems

Status: Done

# 🎯Practice Questions on Conditionals  (Part 1)

## Why This Lecture Matters

This lecture doesn't introduce new syntax — it takes everything you already know (`if-else`, logical operators, `switch`) and applies it to **placement-style and coding-round problems**. The goal isn't just to see the "right" code, but to understand the **thought process** behind building the condition.

> ✌️**`Tip from the instructor`**: Pause before seeing the solution and try solving each problem yourself first.
> 

---

## `Problem 1`: Find the Maximum of Two Numbers

### The Logic

We're given two numbers, `A` and `B`, and we need to find out which one is bigger. This is a straightforward `if-else` job.

```
Is A > B?
   ├── Yes → A is the maximum
   └── No  → Is B > A?
                ├── Yes → B is the maximum
                └── No  → Both are equal
```

### The Code

```cpp
#include <iostream>
using namespace std;

int main() {
    int A, B;
    cout << "Enter first number: ";
    cin >> A;
    cout << "Enter second number: ";
    cin >> B;

    if (A > B) {
        cout << A << " is greater than " << B;
    }
    else if (B > A) {
        cout << B << " is greater than " << A;
    }
    else {
        cout << "Both are equal";
    }

    return 0;
}
```

### Sample Runs

| A | B | Output |
| --- | --- | --- |
| 10 | 20 | 20 is greater than 10 |
| 20 | 10 | 20 is greater than 10 |
| 10 | 10 | Both are equal |

**Key takeaway:** If the first `if` is true, only that block runs. If it's false, the compiler checks the `else if`. If that's also false, it falls into `else`. Only one of the three blocks ever executes.

---

## `Problem 2`: Find the Maximum of Three Numbers

### The Logic — Where Logical Operators Come In

Now we have three numbers: `A`, `B`, and `C`. To say "A is the largest," it's not enough to compare A with just one other number — **A must be greater than or equal to both B and C at the same time**.

This is exactly why we need the **logical AND (`&&`)** operator here — both conditions need to be true simultaneously.

```
Is (A >= B) AND (A >= C)?
   ├── Yes → A is the largest
   └── No  → Is (B >= A) AND (B >= C)?
                ├── Yes → B is the largest
                └── No  → C must be the largest
```

> Why does "C must be the largest" work without even checking C? Because if A isn't the largest, and B isn't the largest either, the only number left standing is C — by elimination.
> 

### The Code

```cpp
#include <iostream>
using namespace std;

int main() {
    int A, B, C;
    cout << "Enter first number: ";
    cin >> A;
    cout << "Enter second number: ";
    cin >> B;
    cout << "Enter third number: ";
    cin >> C;

    if (A >= B && A >= C) {
        cout << "Maximum number is A";
    }
    else if (B >= A && B >= C) {
        cout << "Maximum number is B";
    }
    else {
        cout << "Maximum number is C";
    }

    return 0;
}
```

### Sample Run

Input: `A = 10`, `B = 20`, `C = 5` → Output: **Maximum number is B** (since `20 >= 10` and `20 >= 5`, both true)

---

## `Problem 3`: Check Vowel or Consonant

### The Logic

We take a single character from the user and check: is it one of `a, e, i, o, u` (in either uppercase or lowercase)? If yes → vowel. If no → consonant.

Since there are **10 possible matches** (5 vowels × 2 cases each), this is where the **logical OR (`||`)** operator becomes essential — if *any one* of these ten comparisons is true, the character is a vowel.

```
Is ch == 'a' OR ch == 'A' OR ch == 'e' OR ch == 'E' OR ... OR ch == 'U'?
   ├── Yes → it's a vowel
   └── No  → it's a consonant
```

### The Code

```cpp
#include <iostream>
using namespace std;

int main() {
    char ch;
    cout << "Enter the character: ";
    cin >> ch;

    if (ch == 'a' || ch == 'A' ||
        ch == 'e' || ch == 'E' ||
        ch == 'i' || ch == 'I' ||
        ch == 'o' || ch == 'O' ||
        ch == 'u' || ch == 'U') {
        cout << ch << " is a vowel";
    }
    else {
        cout << ch << " is a consonant";
    }

    return 0;
}
```

### Sample Runs

| Input | Output |
| --- | --- |
| `A` | A is a vowel |
| `Z` | Z is a consonant |

**Key takeaway:** AND (`&&`) demands *all* conditions be true (used in Problem 2, where A had to beat both B and C). OR (`||`) only needs *one* condition to be true (used here, where matching any single vowel character is enough).

---

## `Problem 4`: Number of Days in a Month (using `switch`)

### The Logic

Given a month number (1–12), print how many days that month has. Since several months **share** the same day count, we can group multiple `case` labels together and let them run the same block of code — this avoids repeating the same `cout` line over and over.

```
31 days → months 1, 3, 5, 7, 8, 10, 12
30 days → months 4, 6, 9, 11
28/29 days → month 2 (we're skipping leap-year logic for now)
anything else → Invalid month number
```

### How "Grouping Cases" Works

```cpp
switch (month) {
    case 1:
    case 3:
    case 5:
    case 7:
    case 8:
    case 10:
    case 12:
        cout << "Number of days = 31";
        break;
    ...
}
```

Notice: `case 1:` has **no code directly under it** — it just falls straight through to `case 3:`, then `case 5:`, and so on, until it hits `case 12:`, where the actual `cout` statement finally sits. Since none of these cases have a `break` in between, they all share that one block of code.

### The Full Code

```cpp
#include <iostream>
using namespace std;

int main() {
    int month;
    cout << "Enter month number: ";
    cin >> month;

    switch (month) {
        case 1:
        case 3:
        case 5:
        case 7:
        case 8:
        case 10:
        case 12:
            cout << "Number of days = 31";
            break;

        case 4:
        case 6:
        case 9:
        case 11:
            cout << "Number of days = 30";
            break;

        case 2:
            cout << "Number of days = 28 or 29";
            break;

        default:
            cout << "Invalid month number";
            break;
    }

    return 0;
}
```

### Sample Runs

| Input | Output |
| --- | --- |
| 5 | Number of days = 31 |
| 13 | Invalid month number |

# 🎯Practice Questions on Conditionals (Part 2)

---

## Why Output-Based Questions Are Different

In the programs we just covered, *we* wrote the logic from scratch. In **output-based questions**, you're handed a finished piece of code and asked: "What will this print?" These are extremely common in placement rounds and online assessments (OAs), and they usually hide a small trap — a missing `break`, a swapped operator, or a precedence rule you forgot.

> Golden rule: Always trace through the code line by line, the exact same way the compiler executes it. Don't just skim — even if "compiler error" isn't listed as an option, actively check the syntax anyway.
> 

---

## `Question 1`: Switch Fall-Through Behavior

### The Code

```cpp
int x = 2;

switch (x) {
    case 1:
        cout << 1;
        break;
    case 2:
        cout << 2;
    case 3:
        cout << 3;
        break;
    case 4:
        cout << 4;
        break;
}
```

### Tracing Through It

- `x = 2`, so the switch jumps straight to `case 2`.
- Inside `case 2`, it prints `2`.
- **Now look for a `break` right after — there isn't one.** This means **fall-through** happens: execution doesn't stop, it just continues into the *next* case below, regardless of whether that case's value matches `x`.
- So it drops into `case 3`, prints `3`.
- `case 3` **does** have a `break`, so the switch now exits.
- `case 4` never gets a chance to run.

```
x = 2
   ↓
case 2 → print 2 → (no break) → falls through
   ↓
case 3 → print 3 → break → exit switch
```

**Output: `2 3`**

> Fall-through behavior: If a case block has no `break`, execution "falls through" into the next case's code, even if that case's label doesn't match the switch value. This continues until a `break` is hit (or the switch ends).
> 

---

## `Question 2`: Which Statement About Nested `if` Is True?

You're given four statements and asked to pick the correct one.

### Statement A: "Every `else` matches the nearest unmatched `if`."

Picture nested `if` blocks, one inside another:

```cpp
if (condition1) {
    if (condition2) {
        // ...
    }
    else {
        // which if does THIS else belong to?
    }
}
```

That `else` belongs to `condition2`'s `if` — the **closest one above it that doesn't already have its own `else`**. This is exactly what "nearest unmatched if" means.

✅ **This statement is correct.**

### Statement B: "You can use multiple `else` blocks after one `if`."

You *can* chain multiple `else if` blocks after an `if`. But you can only ever have **one final plain `else`** — you cannot write two bare `else` blocks after the same `if`.

❌ **This statement is false.**

### Statement C: "Nested `if` cannot contain a loop."

There's no such restriction — you can absolutely place a `for` loop, `while` loop, or anything else inside a nested `if` block.

❌ **This statement is false.**

### Statement D: "All of the above."

Since B and C are false, this can't be correct either.

❌ **False.**

**Correct answer: Statement A** — "Every else matches the nearest unmatched if."

---

## `Question 3`: The Classic `=` vs `==` Trap

### The Code

```cpp
int x = 0;

if (x = 1) {
    cout << "true";
}
else {
    cout << "false";
}
```

### What's Actually Wrong Here

At first glance, this looks like a normal comparison. But look closely at the condition inside the `if`:

```cpp
if (x = 1)
```

This is **not** the comparison operator `==` — it's the **assignment operator** `=`. Instead of *checking* whether `x` equals `1`, this line **assigns** `1` into `x`.

> Remember from Lecture 4: `=` stores a value into a variable (assignment). `==` compares two values (relational operator). They look almost identical but do completely different jobs.
> 

Using `=` where a comparison was intended is a classic beginner mistake — and one interviewers deliberately test for.

**Answer: Compiler error** (from using assignment instead of comparison inside the condition).

> Takeaway: Whenever you see "compiler error" as an option in an output-based question, that's your cue to scan every single line extra carefully for exactly this kind of operator mix-up.
> 

---

## `Question 4`: Nested `switch`

### The Code

```cpp
int x = 1, y = 2;

switch (x) {
    case 1:
        switch (y) {
            case 1:
                cout << "Inside x";
                break;
            case 2:
                cout << "Inside y";
                break;
        }
        break;
    case 2:
        cout << "Outer 2";
        break;
}
```

### Tracing Through It

Just like a `for` loop can sit inside another `for` loop (a **nested loop**), a `switch` can sit inside another `switch` — this is called a **nested switch**.

```
switch(x) where x = 1
   ↓
matches case 1
   ↓
enters the INNER switch(y) where y = 2
   ↓
matches inner case 2 → prints "Inside y" → break (exits INNER switch only)
   ↓
reaches outer break → exits OUTER switch
```

**Output: `Inside y`**

> Common confusion to avoid: Beginners often think a `break` exits *everything* at once. It doesn't — a `break` only exits the **one switch (or loop) it's physically sitting inside**. Here, the first `break` only closes the inner switch; the outer switch needed its own separate `break` to close too.
> 

---

## `Question 5`: Operator Precedence — `&&` vs `||`

### The Code

```cpp
int a = 3, b = 5, c = 10;

if (a > b || b < c && c > a) {
    cout << "YES";
}
else {
    cout << "NO";
}
```

### Tracing Through It

From Lecture 4, we already know: **`&&` has higher precedence than `||`**. So even though `||` appears first when reading left to right, the `&&` portion gets evaluated *first*.

**Step 1 — Resolve the `&&` part first:**

```
b < c  →  5 < 10  →  true
c > a  →  10 > 3  →  true
```

Both are true, so `(b < c && c > a)` → **true**

**Step 2 — Now resolve the `||`:**

```
(a > b) || (true)
```

We don't even need to check `a > b` at this point — with OR, **if even one side is true, the whole expression is true**, regardless of the other side.

```
a > b || (b < c && c > a)
   3 > 5   ||        true
  false    ||        true
            ↓
           true
```

**Output: `YES`**

> Takeaway: Precedence isn't about reading order — it's about which operator *type* gets evaluated first, regardless of where it's positioned in the line. `&&` always resolves before `||`.
> 

---

## Key Points to Remember (Both Parts)

- **AND (`&&`)** requires *all* conditions to be true; **OR (`||`)** requires just *one*.
- **Switch fall-through**: without a `break`, execution slides into the next case automatically.
- **Multiple `case` labels** can share one block of code by stacking them with no code in between.
- **Nested `if`**: an `else` always binds to the *nearest* `if` above it that doesn't already have one.
- Only **one** plain `else` is allowed per `if` chain — but you can have multiple `else if`s.
- **`=` (assignment)** vs **`==` (comparison)** — mixing these up is one of the most common (and most tested) beginner mistakes.
- **Nested `switch`**: a `break` only exits the switch it's directly inside, not any outer one.
- Precedence rule recap: `&&` is evaluated before `||`, no matter the left-to-right order they appear in.

---