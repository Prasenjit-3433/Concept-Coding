# Lecture 4: Operators in C++

Status: Done

## The Full Operators Roadmap

Before diving into today's topic, it helps to know where this fits in the bigger picture. Operators in C++ are covered across several dedicated videos:

```
┌──────────────────────────────────────────────────────┐
│                OPERATORS IN C++                      │
├───────────┬───────────┬───────────┬──────────────────┤
│ Arithmetic│ Relational│ Logical   │  Bitwise         │
│           │           │           │ (a bit tricky)   │
├───────────┴───────────┴───────────┴──────────────────┤
│  Assignment  │  Ternary Conditional  │ Miscellaneous │
├──────────────────────────────────────────────────────┤
│         Precedence & Associativity                   │
└──────────────────────────────────────────────────────┘
```

---

# 🎯Part 1: Arithmetic Operators

---

### 1. A Quick Refresher: Input & Output

Before touching operators, let's re-solidify `cin`/`cout`, since we'll be using them constantly today.

```cpp
int data;         // memory block named "data" is created
cin >> data;        // user enters a value, say 30 — it gets stored in "data"
cout << data;        // prints 30 to the screen
```

Both `cin` and `cout` come from the `iostream` header file, as we covered earlier.

---

### 2. Operands and Operators

Take this expression:

```cpp
5 + 2
```

- The **entities** on which an operation is performed (`5` and `2`) are called **operands**.
- The **symbol** that performs the operation (`+`) is called an **operator**.

```
     5    +    2
     │    │    │
  operand │  operand
        operator
```

> Operands = the data being operated on
Operator = the symbol responsible for performing the operation
> 

---

### 3. Types of Arithmetic Operators

There are **7 arithmetic operators** in total. Let's set up two variables to see them all in action:

```cpp
int a = 10;
int b = 20;
```

| Operator | Meaning | Example | Result |
| --- | --- | --- | --- |
| `+` | Addition | `a + b` | `30` |
| `-` | Subtraction | `a - b` | `-10` |
| `*` | Multiplication | `a * b` | `200` |
| `/` | Division | `b / a` | `2` |
| `%` | Modulus (remainder) | `b % a` | `0` |
| `++` | Increment | `a++` | increases `a` by 1 |
| `--` | Decrement | `a--` | decreases `a` by 1 |

```cpp
cout << a + b;   // 30
cout << a - b;   // -10
cout << a * b;   // 200
cout << b / a;   // 2
cout << b % a;   // 0
```

*(Note: `b / a` is used instead of `a / b` here — dividing 10 by 20 would give a decimal, which isn't the point of this example.)*

#### Understanding Modulus (`%`)

The **modulus operator** gives you the **remainder** left over after division — not the quotient.

Think of long division:

```
3 % 2  →  3 ÷ 2 = 1 remainder 1  →  answer: 1
20 % 10 → 20 ÷ 10 = 2 remainder 0  →  answer: 0
```

> Whenever you want to find the *remainder* of a division, use modulus (`%`).
> 

---

### 4. Binary vs. Unary Operators

Arithmetic operators are further split into two groups, based on **how many operands** they act on:

### Binary Operators — need TWO operands

`+`, `-`, `*`, `/`, `%` all require two values to work. You can't "add" just one number — addition inherently needs two operands.

```cpp
a + b     // needs both a AND b
```

### Unary Operators — work on ONE operand

`++` and `--` can act on a **single** value.

```cpp
a++;      // works on just "a" alone
20++;     // also technically valid — a single operand
```

```
┌────────────────────────────────────────┐
│           ARITHMETIC OPERATORS         │
├───────────────────┬────────────────────┤
│   Binary (need 2) │   Unary (need 1)   │
│  +  -  *  /  %    │      ++   --       │
└───────────────────┴────────────────────┘
```

---

### 5. Increment (`++`) and Decrement (`-`)

**Increment** means: take the operand's current value and **add 1** to it.
**Decrement** means: take the operand's current value and **subtract 1** from it.

```cpp
int a = 10;
a++;   // a becomes 11
a--;   // a becomes 9
```

Think of it like shifting gears — `++` shifts you up a gear (4th → 5th), `--` shifts you down (4th → 3rd).

---

### 6. Full Code Example

```cpp
#include <iostream>
using namespace std;

int main() {
    int a, b;
    cin >> a;
    cin >> b;

    cout << "Arithmetic Operators" << endl;
    cout << a + b << endl;   // addition
    cout << a - b << endl;   // subtraction
    cout << a * b << endl;   // multiplication
    cout << b / a << endl;   // division
    cout << b % a << endl;   // modulus

    return 0;
}
```

**If `a = 10` and `b = 20`:**

```
Arithmetic Operators
30
-10
200
2
0
```

---

### 7. Pre-Increment vs. Post-Increment — The Tricky Part

This is the concept that trips up most beginners, so let's slow down here.

### Prefix vs. Postfix — what's the difference?

- **Prefix** = the `++` is placed **before** the operand → `++a` → called **pre-increment**
- **Postfix** = the `++` is placed **after** the operand → `a++` → called **post-increment**

```cpp
int a = 10;
cout << ++a;    // pre-increment
cout << a++;    // post-increment
```

**You'd expect both to print `11`. But they don't.**

```
++a  →  prints 11
a++  →  prints 10
```

#### Why does this happen?

The compiler reads your code **line by line, and within a line, left to right**.

**For `cout << ++a;`:**

1. Compiler sees `cout` → "okay, something needs to be printed"
2. Sees `++a` → "increment `a` FIRST, *then* give me the result"
3. `a` (10) becomes 11 → **then** it's printed

```
Step 1: increment a → a becomes 11
Step 2: print a      → prints 11
```

**For `cout << a++;`:**

1. Compiler sees `cout` → "okay, something needs to be printed"
2. Sees `a` → immediately prints the **current** value of `a` (which is still 10)
3. **Then** it processes the `++` → `a` becomes 11 in memory, but this happens *after* the print

```
Step 1: print a (current value) → prints 10
Step 2: increment a               → a becomes 11 (in memory, silently)
```

> **Pre-increment (`++x`):** increments `x` first, *then* evaluates/uses the new value.
**Post-increment (`x++`):** evaluates/uses the *current* value first, *then* increments `x`.
> 

#### Worked example

```cpp
int i = 1;
int a = i++;   // a gets the ORIGINAL value of i (1), THEN i becomes 2
// a = 1, i = 2

int i = 1;
int b = ++i;   // i becomes 2 FIRST, THEN b gets the NEW value
// b = 2, i = 2
```

| Expression | What happens | Result |
| --- | --- | --- |
| `a = i++;` (i=1) | i used first (1), then incremented | `a = 1`, `i = 2` |
| `b = ++i;` (i=1) | i incremented first, then used | `b = 2`, `i = 2` |

---

### 8. Pre-Decrement vs. Post-Decrement

Exact same logic as increment — just with `-` instead of `+`.

```cpp
int i = 3;
int a = --i;   // decrement FIRST, then use → a = 2, i = 2
```

```cpp
int i = 3;
int b = i--;   // use current value FIRST (3), then decrement → b = 3, i = 2
```

| Expression | What happens | Result |
| --- | --- | --- |
| `a = --i;` (i=3) | i decremented first, then used | `a = 2`, `i = 2` |
| `b = i--;` (i=3) | i used first (3), then decremented | `b = 3`, `i = 2` |

---

### 9. Practice Questions (Placement/OA-Style MCQs)

### Question 1

```cpp
int a = 3.5;   // wait — actually let's use the real example:
int a = 7 / 2;
```

**What gets stored in `a`?**

`7 / 2` mathematically equals `3.5`. But since `a` is declared as `int`, **implicit type casting** kicks in before the value is stored — the `float` result gets converted down to an `int`.

**Answer: `3`** (not `3.5`)

> This question isn't really testing division — it's testing whether you understand **implicit type casting**, which we covered in the data types lecture.
> 

#### Question 2

**Which statement is true about the modulus operator?**

- ❌ "It works for both integer and floating-point types" — **false**
- ❌ "It always gives non-negative results" — **false** (common sense: think about what a remainder even means)
- ❌ "It performs bitwise remainder" — **false**, that's not a real concept
- ✅ **"It works only for integers"** — **correct**

#### Question 3

```cpp
char c = 100;
auto result = c + 50;
```

**What is the data type of `result`?**

Here, a `char` (1 byte) is being added to an `int` (4 bytes). In implicit type casting, C++ always promotes the **smaller** data type up to the **larger** one, to avoid data loss.

So `c` (character, 1 byte) gets promoted to fit alongside the `int` (4 bytes), and the final result:

```
100 + 50 = 150   →   stored as an int
```

**Answer: `result` is of type `int`, holding `150`.**

---

#### Quick Recap Diagram

```
ARITHMETIC OPERATORS
─────────────────────
+   Addition        a + b
-   Subtraction     a - b
*   Multiplication  a * b
/   Division        a / b   (integer ÷ integer = integer result)
%   Modulus         a % b   (remainder only — works ONLY on integers)
++  Increment       increases value by 1
--  Decrement       decreases value by 1

BINARY vs UNARY
────────────────
Binary (needs 2 operands): +  -  *  /  %
Unary  (needs 1 operand):  ++  --

PRE vs POST (++/--)
─────────────────────
++x  (pre)   → increment FIRST, then use the new value
x++  (post)  → use the CURRENT value first, then increment

Example:
  int a = 10;
  cout << ++a;   // 11  (incremented, then printed)
  cout << a++;   // 10  (printed, then incremented)

TYPE CASTING REMINDER
──────────────────────
int a = 7 / 2;        →  a = 3   (float 3.5 truncated to int)
char + int             →  result is promoted to int
```

---

# 🎯Part 2: Relational Operators

---

### What Are Relational Operators?

Whenever you need to **compare two values** inside your C++ program, you reach for a relational operator.

Here's the example the instructor built this around: imagine you're sorting people by whether they're allowed to vote.

> "Pick out the people whose age is less than 18."
> 

So you'd say: **if** someone's age is **less than 18**, then — **you cannot vote**.

```cpp
if (age < 18) {
    cout << "You cannot vote";
}
```

Technically, what's happening here? `age` is a value — let's say the user was asked to input it via `cin >> age`, and they entered `14`. Now the program is **comparing** `14` and `18`, and producing an output based on that comparison. That's a **condition**. And "if," in plain English, is exactly what the `if` keyword means — you'll study `if-else` statements properly in this same batch, covering how conditions are built and used.

The key takeaway: to compare these two values, a **less-than operator** (`<`) was used. And this `<` is what we call a **relational operator**.

> A relational operator is what lets you practically compare two values.
> 

### The 6 Relational Operators

| Operator | Meaning |
| --- | --- |
| `==` | Equal to — checks whether two values are equal |
| `!=` | Not equal to — checks whether two values are *not* equal |
| `>` | Greater than |
| `<` | Less than |
| `>=` | Greater than or equal to |
| `<=` | Less than or equal to |

Let's go through **each one with its own example**, exactly the way the instructor walked through them.

#### Equal To (`==`)

**The example:** "Dad says — if you score 80 marks, I'll get you a bike."

```cpp
if (marks == 80) {
    cout << "You get a bike!";
}
```

Here, `marks` is something we'd take as input from the user (`cin >> marks`), and then compare it against `80`. If it's **exactly** 80 — not 81, not 79, *exactly* 80 — then this condition becomes true, and the bike is awarded.

#### Not Equal To (`!=`)

**The example (the second half of the same scenario):** "But if your marks are **not** 80... you get scolded instead."

```cpp
if (marks != 80) {
    cout << "You get scolded instead!";
}
```

So here we've now seen both the **equal** case and the **not-equal** case — using the exact same `marks` variable, just flipping the condition.

#### Less Than (`<`) and Less Than or Equal To (`<=`)

**The example:** "If your attendance is less than 75%, you won't be allowed to sit for the mid-semester exams."

```cpp
if (attendance < 75) {
    cout << "No mid-semester exams for you";
}
```

Now here's a subtlety worth sitting with: what if the rule were **stricter** — attendance has to be *strictly above* 75%, meaning exactly 75% still disqualifies you? Then you wouldn't use `<`, you'd use `<=`:

```cpp
if (attendance <= 75) {
    cout << "No mid-semester exams for you";
}
```

The difference is subtle but important: `< 75` excludes only values below 75, while `<= 75` also excludes 75 itself. Attendance would need to be *strictly greater than* 75 to pass.

#### Greater Than (`>`) and Greater Than or Equal To (`>=`)

**The example:** "If your age is greater than 18, you can drive."

```cpp
if (age > 18) {
    cout << "You can drive";
}
```

Notice: `> 18` means 18 itself is **excluded**. You'd need to be 19 or above.

But now suppose the rule changes: "Even if you're *exactly* 18, you're allowed to drive." Now you need `>=`:

```cpp
if (age >= 18) {
    cout << "You can drive";
}
```

This is the exact same distinction as `<` vs `<=`, just mirrored for the "greater than" direction — and it's precisely the kind of boundary-condition detail that trips people up if they don't slow down and think about whether the edge value should be *included* or *excluded*.

### Full Code Example

Here's the instructor's actual coded walkthrough, put together as one program:

```cpp
#include <iostream>
using namespace std;

int main() {
    int marks;
    cout << "Enter your marks: ";
    cin >> marks;

    if (marks == 80) {
        cout << "You get a bike!" << endl;
    }

    if (marks != 80) {
        cout << "You get scolded!" << endl;
    }

    int attendance;
    cout << "Enter your attendance: ";
    cin >> attendance;

    if (attendance < 75) {
        cout << "No mid-sem" << endl;
    }

    if (attendance >= 75) {
        cout << "Eligible" << endl;
    }

    return 0;
}
```

**Sample run 1:** Marks entered → `80` → Output: `You get a bike!`**Sample run 2:** Marks entered → `79` → Output: `You get scolded!`**Sample run 3:** Attendance entered → `85` → Output: `Eligible`**Sample run 4:** Attendance entered → `74` → Output: `No mid-sem`

Notice something important in that last pair of examples: the code checks `attendance < 75`, and separately checks `attendance >= 75`. This is because — as the instructor pointed out — every relational operator, when evaluated, doesn't just silently decide whether to run some code. It actually **produces a value**: either `1` (true) or `0` (false).

### Relational Operators Return Boolean Values — Not Just "Yes/No" Behavior

This is worth slowing down on, because it's a subtlety many beginners miss.

```cpp
int age = 20;
cout << (age >= 18);
```

**What gets printed here?** Not "true," not "yes" — you get **`1`**.

Why? Because a relational operator **returns a Boolean value** — meaning it evaluates to `1` (true) or `0` (false), and that's what gets printed if you directly `cout` the expression.

```cpp
int age = 17;
cout << (age >= 18);   // prints 0, because the condition is false
```

So any time you write a condition like `a == b`, `x > y`, etc., you're not writing some abstract "yes/no" logic — you're writing an **expression that literally evaluates to the number 1 or 0**. This becomes especially important once you start combining multiple conditions together (which is exactly what logical operators — the next topic — are built for).

## A Compact Practice Example

The instructor also walked through this small standalone example, comparing two fixed values `a` and `b` across every relational operator, which is a great one to run yourself and predict the output before checking:

```cpp
int a = 6, b = 4;

cout << (a == b) << endl;    // Is 6 equal to 4?         → 0
cout << (a > b)  << endl;    // Is 6 greater than 4?      → 1
cout << (a >= b) << endl;    // Is 6 >= 4? (either works)  → 1
cout << (a < b)  << endl;    // Is 6 less than 4?          → 0
cout << (a <= b) << endl;    // Is 6 <= 4?                 → 0
cout << (a != b) << endl;    // Is 6 not equal to 4?       → 1
```

Walking through this mentally *before* running it is genuinely good practice — this is precisely the kind of question that shows up in placement rounds and online assessments.

## Key Points to Remember

- Relational operators are used to **compare two values**.
- They always return a **Boolean result**: `1` (true) or `0` (false) — never a string like "true" or "yes."
- `==` checks **equality**; it is completely different from `=`, which is the **assignment** operator (covered later — assigning a value *into* a variable, versus *comparing* two values).
- Pay close attention to whether a boundary value should be *included* (`>=`, `<=`) or *excluded* (`>`, `<`) — this single distinction is where most mistakes happen.
- Relational operators are almost always used inside `if` conditions, which you'll formally study alongside `if-else` in this same batch.

# 🎯Part 3: Logical Operators

---

### Setting the Stage: Why Do We Need Logical Operators?

In the previous lecture, we covered conditions, relations, and relational operators. Today's topic builds directly on that — because logical operators are specifically about handling **multiple conditions at once**.

Here's the formal definition the instructor put up first — and, in his own words, it's a definition you'll read once and understand nothing from:

> "Logical operators are used to combine two or more conditions and constraints, and to complement the evaluation of the original condition under consideration."
> 

As the instructor himself joked — quite a mouthful, and it probably didn't land at all on first read. So let's break it down properly.

> In plain terms: logical operators are used when you need to evaluate **two or more conditions together, at the same time**.
> 

There are **three types** of logical operators, and we'll go through each one exactly the way they were introduced.

### Setting Up the Comparison

Recall a normal, single condition:

```cpp
if (x <= 5) {
    // do something
}
```

This is **one condition**. But what if you want to apply **multiple conditions simultaneously**? For example:

```cpp
if (x < 5 && y < 4) {
    // do something
}
```

Here, we've applied **two conditions together**: only if *both* are satisfied — only if *both* are true — should the code inside execute.

That `&&` sitting in the middle — the double ampersand — is our **first logical operator**: the **logical AND operator**.

### 1. Logical AND (`&&`)

> The logical AND operator checks both conditions. Only if **both conditions are true** will the following statement execute.
> 

As we already know from relational operators: every condition returns either `true` or `false`. So the logic here is — if condition 1 is true, **and** condition 2 is also true, only then does the code that follows get executed.

#### Worked Example

```cpp
int x = 4, y = 6;

if (x > 3 && y > 2) {
    cout << "Both are greater than three";
}
```

Let's trace through exactly how the compiler evaluates this:

1. **First condition checked:** Is `x > 3`? → `4 > 3` → Yes, this is true → gives `1`.
2. **Second condition checked:** Is `y > 2`? → `6 > 2` → Yes, also true → gives `1`.
3. Since **both** conditions are true, the entire `if` block becomes true (`1`), and the code inside executes.

**Output:** `Both are greater than three`

Now here's the important part to really understand — what happens if even **one** of the two conditions turns out to be false?

Suppose instead of `y = 6`, we had `y = 1`. Then `y > 2` becomes false. And because logical AND requires **both** to be true, the moment even one condition fails, the entire expression becomes false — and the code inside the `if` block simply does **not** run.

### The AND Truth Table

Let's suppose `A` and `B` are two separate conditions — each of which independently evaluates to either `true` or `false` (represented as `1` or `0`).

If we perform `A && B`:

> **The rule:** If even one condition is false, the output is false. Both conditions must be true for the result to be true.
> 

| A | B | A && B |
| --- | --- | --- |
| false | false | **false** |
| false | true | **false** |
| true | false | **false** |
| true | true | **true** |

Notice: out of these four possibilities, only **one** combination — both true — produces a true result. Every other combination, where even a single condition fails, gives you false.

---

### 2. Logical OR (`||`)

Now let's move to the **OR operator** — and this one works on a completely opposite philosophy.

> **The rule:** If **any one** of the two conditions is true, the output will be true.
> 

As the name itself suggests — OR means "either this, or this." If **either one** of the two conditions turns out to be true, the code inside executes.

#### The OR Truth Table

| A | B | A || B |
| --- | --- | --- |
| false | false | **false** |
| false | true | **true** |
| true | false | **true** |
| true | true | **true** |

Notice how this is almost the mirror image of AND: here, only **one** combination — both false — produces a false result. Every other combination gives you true, because OR only needs *one* of the two conditions to hold.

#### Worked Example

```cpp
int x = 3, y = 4;

if (x > 2 || y > 5) {
    cout << "OR operation successful";
}
```

Let's trace through this one too:

1. **First condition:** Is `x > 2`? → `3 > 2` → true → `1`.
2. **Second condition:** Is `y > 5`? → `4 > 5` → false → `0`.
3. Now, because we're using **OR**, and at least *one* of the two conditions (the first one) came out true, the overall result becomes true (`1`).

**Output:** `OR operation successful`

This is the core distinguishing feature between AND and OR: AND is strict — it demands unanimous agreement — while OR is lenient — a single "yes" is enough to satisfy it.

---

### 3. Logical NOT (`!`)

The third logical operator is the **complement operator**, or **logical NOT**.

> Logical NOT flips a value to its opposite. If a condition is true, NOT makes it false — and vice versa.
> 

Here's the simplest possible illustration:

```cpp
int a = 0;
```

If we put NOT in front of `a`, it effectively **flips** it — `0` becomes `1`.

#### A Practical Example

Let's build on the earlier marks example. Suppose:

```cpp
int x = 100;

if (x != 100) {
    cout << "x is not 100";
}
```

But let's actually understand NOT specifically using the `==` version instead, since that's cleaner to reason through:

> Suppose there's a condition: "marks equal to 80." First, we check whether marks are equal to 80. Now, if we apply NOT to this condition, we're essentially asking: **"is it the case that marks are NOT equal to 80?"**
> 

So NOT takes whatever a condition evaluates to, and **inverts** it. If the underlying condition (`marks == 80`) is true, then `!(marks == 80)` becomes false. If the underlying condition is false (marks are *not* 80), then applying NOT flips it to true.

This might not feel entirely intuitive on the first pass — and that's fine. The instructor himself acknowledged this is one of those concepts that clicks properly only once you've seen it used in context. And there is a very concrete place where it shows up constantly:

> **Where NOT gets used heavily:** Traversing linked lists with pointers. You'll encounter conditions like `while (node != NULL)` extensively once you reach the DSA portion of this course — that's genuinely where the logical NOT operator becomes second nature.
> 

#### The NOT Rule, Formally

> If the value of a condition is `0` (false), applying NOT makes it `1` (true).
If the value of a condition is `1` (true), applying NOT makes it `0` (false).
> 

It simply **complements** — takes the exact opposite of — whatever it's applied to.

| Expression | Result |
| --- | --- |
| `!true` | `false` |
| `!false` | `true` |

---

### Precedence of Logical Operators

This is one of the most important things to lock in, because it directly determines how complex expressions get evaluated.

> **Precedence order (highest to lowest):**
> 
> 1. `!` (NOT) — evaluated first
> 2. `&&` (AND) — evaluated next
> 3. `||` (OR) — evaluated last

So whenever a NOT, an AND, and an OR all show up in the same expression, NOT gets resolved first, then AND, and only then OR.

### Associativity — What It Means and Why It Matters

Alongside precedence, there's another concept: **associativity**.

> Associativity tells you: if multiple operators **of the same type** appear together in one expression, in which direction do you evaluate them — left to right, or right to left?
> 

For the logical operators (and most operators in general), the associativity is **left to right** — meaning when you have several ANDs (or several ORs) chained together, you resolve them starting from the leftmost one and move rightward.

### A Full Worked Example — Precedence AND Associativity Together

Let's take the exact expression walked through in the lecture, using `1` and `0` to represent `true` and `false`:

```
1 && 0 || 0 || 0 && !1 || 1
```

**Step 1 — NOT has the highest precedence, so resolve it first:**

`!1` becomes `0`.

Expression is now: `1 && 0 || 0 || 0 && 0 || 1`

**Step 2 — Now we have multiple ANDs and ORs. AND has higher precedence than OR, so we look for AND operations first.**

There are two `&&` operations in this expression. Since associativity for AND is left-to-right, we resolve the **leftmost** AND first:

`1 && 0 = 0`

Expression is now: `0 || 0 || 0 && 0 || 1`

**Step 3 — There's still one more AND remaining (`0 && 0`) — and AND still takes precedence over OR, so we resolve it next:**

`0 && 0 = 0`

Expression is now: `0 || 0 || 0 || 1`

**Step 4 — Now only ORs remain. Associativity is left-to-right, so we go one by one:**

- `0 || 0 = 0`
- `0 || 0 = 0`
- `0 || 1 = 1`

**Final Answer: `1`**

The broader lesson here: no matter how many conditions you chain together — six, ten, however many — as long as you know **precedence** (which operator type resolves first) and **associativity** (which direction to move when there are ties), you can always break the expression down step by step and arrive at the correct answer.

> This is genuinely useful to internalize, because expressions exactly like this show up in technical interview rounds and OA (online assessment) rounds.
> 

### Full Example Program

```cpp
#include <iostream>
using namespace std;

int main() {
    int age = 20;
    int marks = 85;

    if (age > 18 && marks > 50)
        cout << "Eligible";
    else
        cout << "Not Eligible";

    return 0;
}
```

**Output:** `Eligible` — because both `age > 18` (true) and `marks > 50` (true) are satisfied, and AND requires both.

### Key Points to Remember

- Logical operators combine **multiple conditions** into a single overall result.
- `&&` (AND): true **only if both** conditions are true — strict, unanimous agreement required.
- `||` (OR): true if **at least one** condition is true — lenient, just needs one "yes."
- `!` (NOT): flips/complements a single condition — true becomes false, false becomes true.
- Precedence order: `!` → `&&` → `||` (NOT is resolved first, OR is resolved last).
- Associativity for all three is **left to right**.
- Logical NOT becomes especially important later, in topics like linked-list traversal in DSA.

# 🎯Part 4: Bitwise Operators

---

### Bitwise vs. Logical — The Critical Distinction First

Before jumping into bitwise operators themselves, the instructor made a point of clearing up one confusion immediately, because these two families of operators look deceptively similar on the page.

Consider: if you write a **double ampersand**, `&&`, that's your **logical AND** — the one we just covered.

But if you write a **single ampersand**, `&`, that becomes something entirely different: **bitwise AND**.

So what's the actual difference between these two?

> **Logical AND** works on **Boolean values** — meaning it operates on `0` and `1` as whole "true/false" units, the same way we saw in the previous lecture on logical operators.
> 
> 
> **Bitwise AND**, on the other hand, performs its operation **bit by bit**. It works at the **binary bit level**.
> 

This is the core idea to hold onto throughout this entire topic: bitwise operators don't look at a number as one single value — they break it down into its individual binary digits and operate on each digit separately.

### There Are 6 Types of Bitwise Operators

1. **Bitwise AND**
2. **Bitwise OR** — just as logical AND had a double ampersand, logical OR would have double vertical bars (`||`); bitwise OR uses a **single** vertical bar (`|`)
3. **Bitwise XOR** (Exclusive OR)
4. **Bitwise NOT** (Complement) — just as there was a logical NOT, there's a bitwise NOT too
5. **Left Shift**
6. **Right Shift**

Let's look at each one, starting with how they behave at the individual-bit level before applying them to full numbers.

---

### How Each Operator Behaves on a Single Bit

Before working with full binary numbers, it helps to see the raw truth table for a single bit — exactly how the instructor built it up.

#### Bitwise AND — bit by bit

```
0 AND 0 = 0
0 AND 1 = 0
1 AND 1 = 1
1 AND 0 = 0
```

This is *exactly* the same logic as logical AND: if you AND two zeros, you get zero. **Only when both values are 1** will the answer be 1 — in every other case, AND gives you zero.

#### Bitwise XOR — bit by bit

XOR (**Exclusive OR**) works differently from both AND and OR:

> XOR returns **0** if the two bits are the **same**, and returns **1** if the two bits are **different**.
> 

```
0 XOR 0 = 0   (same bits → 0)
0 XOR 1 = 1   (different bits → 1)
1 XOR 1 = 0   (same bits → 0)
1 XOR 0 = 1   (different bits → 1)
```

This table is genuinely important to memorize, since XOR shows up constantly later on (especially in bit-manipulation problems in DSA).

---

### Now Let's Apply These to Real Numbers

Set up two variables:

```cpp
int x = 5;
int y = 9;
```

Since these operations happen at the **binary bit level**, we need their binary representations first:

```
5  in binary → 0101
9  in binary → 1001
```

(Both are padded to 4 bits so they line up for comparison.)

#### 1. Bitwise AND (`&`)

> Bitwise AND gives **1** only if **both** values are `1` at that position. Otherwise, it gives `0`.
> 

```cpp
cout << (x & y);
```

Let's line up the bits and AND them position by position:

```
  0101   (5)
& 1001   (9)
--------
  0001
```

Going digit by digit: `0&1=0`, `1&0=0`, `0&0=0`, `1&1=1` → giving us `0001`, which is `1` in decimal.

**Output:** `1`

#### 2. Bitwise OR (`|`)

> Bitwise OR says: if **even one** of the two bits is `1`, the answer is `1`.
> 

```cpp
cout << (x | y);
```

```
  0101   (5)
| 1001   (9)
--------
  1101
```

Going digit by digit: `0|1=1`, `1|0=1`, `0|0=0`, `1|1=1` → giving `1101`, which is `13` in decimal.

**Output:** `13`

#### 3. Bitwise XOR (`^`)

> XOR says: same bits give `0`, different bits give `1`.
> 

```cpp
cout << (x ^ y);
```

```
  0101   (5)
^ 1001   (9)
--------
  1100
```

Going digit by digit: `0^1=1` (different), `1^0=1` (different), `0^0=0` (same), `1^1=0` (same) → giving `1100`, which is `12` in decimal.

**Output:** `12`

#### 4. Bitwise NOT (`~`) — The Complement Operator

> Bitwise NOT takes the **complement** of every single bit: every `1` becomes `0`, and every `0` becomes `1`.
> 

```cpp
cout << (~x);
```

Taking the complement of `0101`:

```
Original:    0101
Complement:  1010
```

`1010` in decimal is `10`.

**Output:** `10`

*(Note: in actual C++ execution on a real integer — which is 32 bits wide, not just 4 — the true output of `~5` is `-6`, because of how negative numbers are represented using 2's complement across the full width of an `int`. The 4-bit demonstration above is just for understanding the complementing mechanism itself — flip every bit.)*

---

### Left Shift and Right Shift

These two work differently from the first four — instead of comparing two numbers bit-by-bit, they take **one number** and physically **slide its bits** left or right.

> If you left-shift a value by one position, it effectively gets **multiplied by two**. If you right-shift it by one position, it effectively gets **divided by two**.
> 

Let's see exactly *why* this happens, not just take it as a rule to memorize.

#### Left Shift (`<<`)

```cpp
cout << (x << 1);
```

*(A quick side-note the instructor flagged: this is a case of **operator overloading** — the same `<<` symbol is being used both as the "output" operator with `cout`, and here as the actual **bitwise left-shift operator**. You'll study operator overloading properly in the OOPs section later — for now, just notice that context tells C++ which meaning applies.)*

Take `x = 5`, whose binary is `0101`. There are 4 positions here:

```
Position:   1    2    3    4
Bit:        0    1    0    1
```

To **left-shift by one**, every bit moves one position to the left, and a fresh `0` gets added on the right to fill the gap left behind:

```
Before:  0  1  0  1
After:   1  0  1  0
```

`1010` in decimal is `10`. And notice: `5` shifted left by 1 gives `10` — which is exactly `5 × 2`. That's the multiply-by-two effect in action.

**Output:** `10`

**What about shifting by two positions?** The same idea, just repeated — every bit slides two spots to the left, and **two** fresh zeros get added at the end to fill the vacated space:

```cpp
x << 2
```

```
Original:        0  1  0  1
Shift left by 2: 0  1  0  1  0  0
                        (first 2 digits drop off the left,
                         two zeros added on the right)
```

Result: `010100` — shifted twice to the left.

> **Practical use:** Left-shift and right-shift are especially useful later for calculating **powers of two** efficiently — a technique you'll see reused when this topic connects with DSA.
> 

#### Right Shift (`>>`)

Let's use a fresh variable for this one:

```cpp
int z = 10;
cout << (z >> 1);
```

`10` in binary is `1010`. Let's mark the positions:

```
Position (power of 2):   2³   2²   2¹   2⁰
Bit:                      1    0    1    0
```

Here's an important structural point the instructor emphasized: notice that each position in a binary number corresponds to a specific **power of 2** — the rightmost bit is always `2⁰`, the next one is `2¹`, and so on. This means:

> You **can** freely add extra zeros on the **left** side of a binary number (padding it doesn't change its value). But you **cannot** add anything on the right side, because that's where the binary representation's lowest power of 2 (`2⁰`) already sits — that's the defined "end" of the number.
> 

So when we right-shift `1010` by one position, every bit slides one spot to the right. Whatever bit was sitting in the rightmost position (`2⁰`) simply **falls off and is discarded** — it goes out of range, permanently lost:

```
Before:  1  0  1  0
After:   ?  1  0  1     (the trailing 0 fell off)
```

To keep the same bit-width, we pad a `0` back in on the left side (since padding the left doesn't change the value):

```
Result:  0  1  0  1
```

`0101` in decimal is `5`. And sure enough: `10` right-shifted by 1 gives `5` — exactly `10 ÷ 2`. That's the divide-by-two effect.

**Output:** `5`

*(You can similarly shift by two positions, following the same left-to-right sliding logic, discarding two bits off the right edge instead of one.)*

### Full Code Example

```cpp
#include <iostream>
using namespace std;

int main() {
    int x = 5;
    int y = 9;

    cout << (x & y) << endl;    // Bitwise AND → 1
    cout << (x | y) << endl;    // Bitwise OR  → 13
    cout << (x ^ y) << endl;    // Bitwise XOR → 12
    cout << (~x)    << endl;    // Bitwise NOT → -6 (full 32-bit int)

    cout << (x << 1) << endl;   // Left shift  → 10

    int z = 10;
    cout << (z >> 1) << endl;   // Right shift → 5

    return 0;
}
```

### Homework Questions (from the lecture)

The instructor left these as practice — worth working through by hand before checking, since bit-manipulation fluency comes from repetition:

1. Calculate `5 AND 4`
2. Calculate `5 OR 8`
3. Calculate `5 XOR 8`
4. Find the complement (`NOT`) of `9`
5. Calculate `4 << 2` (left shift by 2 positions)
6. Calculate `8 << 1` (left shift by 1 position)
7. Calculate `10 >> 1` (right shift by 1 position)

*(Tip for solving these: convert each number to binary first, line up the bit-widths, then apply the bit-by-bit rule for that operator.)*

### Key Points to Remember

- **Bitwise operators work on individual bits**, not on the number as a whole — this is the fundamental difference from logical operators (`&&`, `||`, `!`), which work on Boolean true/false values.
- `&` (AND): `1` only if **both** bits are `1`.
- `|` (OR): `1` if **either** bit is `1`.
- `^` (XOR): `1` if the bits are **different**; `0` if they're the **same**.
- `~` (NOT): flips **every** bit — `1`s become `0`s, `0`s become `1`s.
- `<<` (Left shift): slides bits left, filling with zeros on the right → equivalent to **multiplying by 2** per shift.
- `>>` (Right shift): slides bits right, discarding whatever falls off the right edge → equivalent to **dividing by 2** per shift.
- You can always pad zeros on the **left** of a binary number without changing its value — but the **right** side is fixed, since that's where `2⁰` lives.

# 🎯Part 5: Assignment & Compound Assignment Operators

---

### A Quick Heads-Up From the Instructor

This topic was introduced as a genuinely short one — but with one important exception tucked inside it: **compound assignment operators**, which the instructor flagged as "a little important" and worth paying real attention to, even though the overall lecture itself is brief.

### The Simple Assignment Operator (`=`)

Let's start with something you've technically been using since Lecture 1, but never formally named.

```cpp
int x = 10;
```

That `=` sign right there — that is your **assignment operator**.

### Why This Often Confuses Beginners

Here's the exact confusion the instructor called out directly: when we studied relational operators, we saw the "equal to" comparison written as `==` — a **double** equal sign, used specifically because we're **comparing** two values.

But for **assignment**, the equal sign you use is just a **single** `=`.

> Here's the real meaning of the assignment operator: you take the value sitting on the **right-hand side**, and you go and **store it** inside the variable sitting on the **left-hand side**.
> 

So `x = 10` literally means: take `10`, and go store it inside the memory block named `x`.

> Assignment operators work such that there's a **variable on the left side**, and an **expression on the right side** — and the value from the right gets assigned into the variable on the left (LHS).
> 

#### A Worked Example

```cpp
int a = 10;      // a variable is created, 10 is assigned into it
int b = a + 5;   // take a + 5 (that is, 10 + 5 = 15), and store it inside b
```

So what happens here? `b` becomes `15`. And `a`? It stays exactly as it was — `10`.

This might feel almost too obvious to state — but it's genuinely worth locking in as a mental model, because it's exactly what sets up the next, more interesting part of this lecture: **compound assignment operators**.

---

### Compound Assignment Operators

> A compound assignment operator is where you've **joined two operators together** — in this case, joining an arithmetic (or bitwise) operator with the assignment operator.
> 

The instructor's own framing: what we just covered (`=` alone) is your **simple assignment operator**. What we're about to cover is your **compound assignment operator** — because you're literally combining two symbols, like `+` and `=`, into one joined operator: `+=`.

#### The Motivating Example

Suppose you write:

```cpp
int x = 30;
```

Now suppose you want to add `3` to `x`'s current value:

```cpp
x = x + 3;
```

What does `x` become? `33`. In plain words: **take the current value of `x`, add `3` to it, and store the result back into `x`**.

Now here's the shorthand the instructor introduced:

> `x = x + 3` can also be written, more compactly, as: `x += 3`
> 

This `+=` is your **compound assignment operator** — specifically, the "plus-equals" operator. It's simply a shorter way of writing the exact same instruction.

```cpp
x = x + 3;   // long form
x += 3;      // compound form — identical result
```

#### The Full Family of Compound Assignment Operators

The instructor walked through each variant, building the pattern one at a time:

- If you want `x = x - 3`, you can shorten it to: `x -= 3`
- If you want `x = x * 3`, you can shorten it to: `x *= 3`
- Similarly, for division: `x = x / 3` becomes `x /= 3`
- And the same for modulo: `x = x % 3` becomes `x %= 3`

And this pattern doesn't stop at arithmetic — it extends to **bitwise operators** too, since we just covered those:

- Bitwise AND: `x = x & y` becomes `x &= y`
- Bitwise OR: `x = x | y` becomes `x |= y`
- Bitwise XOR: `x = x ^ y` becomes `x ^= y`
- Left shift: `x = x << y` becomes `x <<= y`
- Right shift: `x = x >> y` becomes `x >>= y`

> As the instructor put it: every single operator you've learned so far — arithmetic and bitwise both — has this compound-assignment shorthand available. The pattern is always the same: `variable OPERATOR= value` is shorthand for `variable = variable OPERATOR value`.
> 

#### The Complete Reference Table

| Operator | Meaning | Equivalent To | Example |
| --- | --- | --- | --- |
| `+=` | Add & assign | `a = a + b` | `a += 3;` |
| `-=` | Subtract & assign | `a = a - b` | `a -= 2;` |
| `*=` | Multiply & assign | `a = a * b` | `a *= 4;` |
| `/=` | Divide & assign | `a = a / b` | `a /= 5;` |
| `%=` | Modulus & assign | `a = a % b` | `a %= 2;` |
| `&=` | Bitwise AND & assign | `a = a & b` | `a &= b;` |
| `^=` | Bitwise XOR & assign | `a = a ^ b` | `a ^= b;` |
| `<<=` | Left shift & assign | `a = a << b` | `a <<= 1;` |
| `>>=` | Right shift & assign | `a = a >> b` | `a >>= 2;` |

> The instructor's own advice here is worth repeating: build this table into your notes for **every** operator category, because when it's time to revise, having everything laid out compactly like this makes the whole process genuinely enjoyable rather than a chore.
> 

#### Worked Examples of Each

**Addition Assignment (`+=`)**

```cpp
int a = 10;
a += 5;   // a becomes 15
```

**Subtraction Assignment (`-=`)**

```cpp
int x = 20;
x -= 4;   // x becomes 16
```

**Multiplication Assignment (`*=`)**

```cpp
int n = 6;
n *= 3;   // n becomes 18
```

**Division Assignment (`/=`)**

```cpp
int p = 40;
p /= 5;   // p becomes 8
```

**Modulus Assignment (`%=`)**

```cpp
int r = 17;
r %= 3;   // r becomes 2
```

**Bitwise Compound Operators**

```cpp
int a = 10;    // binary: 1010
a &= 6;        // 1010 & 0110 = 0010 → a becomes 2

int b = 5;     // binary: 0101
b <<= 1;       // shifted left → 1010 → b becomes 10
```

---

### A Special Case: Three Ways to Write the Same Increment

Here's a neat detail the instructor pointed out explicitly, tying this topic back to something you already know from the arithmetic operators lecture.

Suppose `x = 30`, and you want to increase it by `1`. There are actually **three completely equivalent ways** to write this:

```cpp
x = x + 1;   // Method 1 — the long, explicit way
x += 1;      // Method 2 — compound assignment
x++;         // Method 3 — the increment operator
```

All three produce the exact same outcome: `x` becomes `31`. As the instructor put it, "there's no issue" with any of these — they're just different levels of shorthand for expressing the identical instruction. `x++` is the shortest and most idiomatic; `x += 1` makes the "add one" explicit; and `x = x + 1` is the most beginner-transparent version.

### Full Code Example

```cpp
#include <iostream>
using namespace std;

int main() {
    int a = 10;
    a += 3;
    cout << a;   // Output: 13

    return 0;
}
```

**A chained example, applying multiple compound operators one after another:**

```cpp
int x = 15;
x -= 5;    // x becomes 10
x *= 2;    // x becomes 20
cout << x; // Output: 20
```

---

### Homework Questions (from the lecture)

The instructor set up a specific practice exercise here — worth working through by hand:

```cpp
int x = 10;   // starting value
```

Now, perform and evaluate each of these operations one at a time (each one modifying `x`, then showing the result):

1. `x = x + 3`
2. `x = x - 2`
3. `x = x & 2` *(using the bitwise AND operator between `x` and `2`)*
4. `x = x | 2` *(bitwise OR)*
5. `x = x << 1` *(left shift `x` by one position)*

> As the instructor put it: this much of a hint is enough — the rest is for you to work through yourself, since by this point in the course, all these individual operators (arithmetic and bitwise both) have already been taught. Practicing them all in the compound-assignment form is the real point of this exercise.
> 

### Key Points to Remember

- `=` is the **simple assignment operator** — it takes the value from the RHS and stores it into the variable on the LHS.
- Never confuse `=` (assignment) with `==` (comparison, from relational operators) — they do fundamentally different jobs.
- A **compound assignment operator** joins a regular operator with `=`, giving you a shorthand: `x += 3` is identical in effect to `x = x + 3`.
- This shorthand pattern applies to **every** arithmetic operator (`+`, , , `/`, `%`) and **every** bitwise operator (`&`, `|`, `^`, `<<`, `>>`).
- `x = x + 1`, `x += 1`, and `x++` are all **three interchangeable ways** of writing the exact same increment operation.

# 🎯Part 6: Operator Precedence & Associativity

---

### Setting Up the Big Picture

Today's topic is about **how precedence is decided**, and specifically **where operator precedence actually gets used** in practice — because up until now, we've studied each operator family in isolation. This lecture is about what happens when they all show up **together** in the same expression.

### The Precedence Table

The instructor built this up as one master table, and it's genuinely worth having in front of you as the anchor for everything else in this section:

> Whenever you have an expression, evaluation happens based on **precedence** — and as you move down the table, precedence keeps **decreasing**.
> 

```
┌───────────────────────────────────────────────────┐
│  1. ()                          → Brackets (highest)   │
│  2. ++  --  !                   → Unary operators      │
│  3. *  /  %                     → Mult./Div./Modulus   │
│  4. +  -                        → Addition/Subtraction │
│  5. <  <=  >  >=                → Relational (compare) │
│  6. ==  !=                      → Relational (equality)│
│  7. &  ^  |                     → Bitwise operators    │
│  8. &&                          → Logical AND          │
│  9. ||                          → Logical OR           │
│ 10. =  +=  -=  etc.             → Assignment (lowest)  │
└───────────────────────────────────────────────────┘
```

Walking through this exactly the way it was laid out:

- **Highest precedence goes to brackets.** Whatever is written inside brackets gets evaluated first — always.
- **After brackets come unary operators** — `++`, `-`, and the `!` (NOT) operator. These sit at position two.
- **Next come multiplication, division, and modulus** — all three share the same precedence level, at position three.
- **Then come plus and minus** — position four.
- **After that, your relational operators** — but notice something subtle here: within the relational family, the *comparison* operators (`<`, `<=`, `>`, `>=`) sit slightly *higher* than the *equality* operators (`==`, `!=`), which come just below them.
- **Somewhere after the relational operators come your bitwise operators.**
- **And finally, your logical AND, logical OR, and the assignment operator (`=`)** sit at the bottom — with logical NOT (`!`) technically already accounted for up at the unary-operator level, since NOT is a unary operator.

#### Where Exactly Do Bitwise Operators Sit?

The instructor was explicit that bitwise operators slot in **between** the relational operators and the logical operators — after `==`/`!=`, but before `&&` and `||`. And **within** the bitwise family itself, there's a further internal ordering:

```
&   (AND)   — highest among bitwise
^   (XOR)
|   (OR)    — lowest among bitwise
```

### The Two Things You Actually Need to Check

> When dealing with precedence, you only ever need to look at **two things**:
> 
> 1. What is the **order of precedence** — which operator type resolves first?
> 2. What is the **associativity** — left to right, or right to left?

And here's a useful shortcut for remembering associativity across the board:

> **Almost everything is left to right.** The only exceptions are your **unary operators** and the **assignment operator** — those are **right to left**.
> 

### Practice Round 1 — Basic Precedence

#### Example: `(8 - 3) * 4 / 2`

Since brackets have the highest precedence, we evaluate what's inside them first:

```
8 - 3 = 5
```

So the expression becomes:

```
5 * 4 / 2
```

Now — between multiplication and division, which one has higher precedence? If you check your notes: **multiplication and division share the exact same precedence level.** So when there's a tie, we fall back on **associativity**, which for these is **left to right**.

That means we evaluate strictly left to right:

```
5 * 4 = 20
20 / 2 = 10
```

**Final Answer: `10`**

> This is the general lesson here: whenever precedence is *tied* between two operators, associativity is what breaks the tie — and for the vast majority of operators, that means working left to right.
> 

#### Example: `3 + 5 * 6`

We know multiplication and division always outrank plus and minus in precedence. So:

```
5 * 6 = 30
```

Then:

```
3 + 30 = 33
```

**Final Answer: `33`**

### Practice Round 2 — Bringing In Bitwise Operators

#### Example: `4 & 6 / 2`

This one's a bit more complex, but nothing to worry about — as the instructor put it, you just need to know how to deal with the added complexity systematically.

We know division, multiplication, and modulus all have **higher** precedence than bitwise operators. So the division resolves first:

```
6 / 2 = 3
```

The expression becomes:

```
4 & 3
```

Now let's actually calculate this using binary. The binary value of `4` is `100`, and the binary value of `3` is `011`.

Now, AND these two together bit by bit:

```
1 AND 0 = 0
0 AND 1 = 0
0 AND 0 = 0
```

**The answer is `0`.**

#### Example: `2 * (4 - 2)`

The `4 - 2` sits inside brackets, so it resolves first:

```
4 - 2 = 2
```

Then:

```
2 * 2 = 4
```

**Final Answer: `4`**

### Practice Round 3 — A Longer, Trickier Expression

Let's recap the rule first, since it's about to get used repeatedly: **the precedence of multiplication, division, and modulus is higher than plus and minus, and their mutual associativity is left to right.**

Now, solve this expression carefully — find the value of `x`:

```cpp
int x = (1 - 5) + 3 + 4 * 2 / -4 % 2;
```

**Step 1 — Brackets have the highest precedence, so resolve them first:**

```
1 - 5 = -4
```

Expression becomes:

```
-4 + 3 + 4 * 2 / -4 % 2
```

**Step 2 — Multiplication, division, and modulus all share the same precedence, so we resolve them left to right. First up: multiplication.**

```
4 * 2 = 8
```

Expression becomes:

```
-4 + 3 + 8 / -4 % 2
```

**Step 3 — Continuing left to right among the same-precedence group: division comes next.**

```
8 / -4 = -2
```

Expression becomes:

```
-4 + 3 + -2 % 2
```

**Step 4 — Now modulus resolves.**

```
-2 % 2 = 0
```

Expression becomes:

```
-4 + 3 + 0
```

**Step 5 — Now only plus and minus remain. Associativity here is also left to right:**

```
-4 + 3 = -1
-1 + 0 = -1
```

Wait — let's double check against the actual arithmetic the instructor walked through, since the exact numbers matter here. Following the lecture's own step-by-step:

```
3 - 2 % 2
```

Here, **modulus has higher precedence than minus**, so it resolves first:

```
2 % 2 = 0     (2 divided by 2 leaves no remainder)
```

Then:

```
3 - 0 = 3
```

**Final Answer: `3`**

> The instructor's own takeaway here: these are exactly the kinds of general questions that come up in practice. You just need to remember precedence and associativity, and you can work through questions like this efficiently in an online assessment round. It's not "rocket science" — it's mechanical, step-by-step application of the same two rules, every time.
> 

### Practice Round 4 — A Full Bitwise Expression

This is the toughest one from the lecture, combining everything — bitwise AND, XOR, and OR — in one expression:

```cpp
5 & 3 ^ 8 ^ 2
```

*(Following the lecture's actual working — note the expression uses XOR twice, applied to `8` and `2`.)*

First, recall the internal bitwise precedence order:

```
&  (highest among bitwise)
^
|  (lowest among bitwise)
```

**Step 1 — AND resolves first: `5 & 3`**

```
5 → 0101
3 → 0011
```

AND bit by bit:

```
1 AND 1 = 1
0 AND 1 = 0
1 AND 0 = 0
0 AND 0 = 0
```

Result: `0001` → decimal `1`

**Step 2 — Between OR and XOR, XOR has higher precedence, so we resolve `8 XOR 2` next.**

```
8 → 1000
2 → 0010
```

XOR bit by bit (same bits → 0, different bits → 1):

```
1 XOR 0 = 1
0 XOR 0 = 0
0 XOR 1 = 1
0 XOR 0 = 0
```

Result: `1010` → decimal `10`

**Step 3 — Now we're left with: `1 OR 10`**

```
1  → 0001
10 → 1010
```

OR bit by bit (1 if either bit is 1):

```
0 OR 1 = 1
0 OR 0 = 0
0 OR 1 = 1
1 OR 0 = 1
```

Result: `1011` → decimal **`11`**

**Final Answer: `11`**

> As the instructor summarized: if you have this precedence table in front of you, and you know precedence and associativity, you can attempt many such questions — this exact style of layered bitwise expression is common in placement-prep material.
> 

### Quick Reference: The Complete Precedence Table

| Level | Operators | Associativity |
| --- | --- | --- |
| 1 (highest) | `()` | — |
| 2 | `++` `--` `!` (unary) | Right → Left |
| 3 | `*` `/` `%` | Left → Right |
| 4 | `+` `-` | Left → Right |
| 5 | `<` `<=` `>` `>=` | Left → Right |
| 6 | `==` `!=` | Left → Right |
| 7 | `&` `^` `|` (bitwise) | Left → Right |
| 8 | `&&` | Left → Right |
| 9 | `||` | Left → Right |
| 10 (lowest) | `=` `+=` `-=` etc. (assignment) | Right → Left |

### Key Points to Remember

- Whenever an expression has multiple operator types, two things decide the evaluation order: **precedence** (which type resolves first) and **associativity** (which direction to move when there's a tie).
- Brackets always win — evaluate what's inside them first, no exceptions.
- Multiplication, division, and modulus share precedence and are evaluated **left to right** when they appear together.
- Bitwise operators sit **between** relational and logical operators in the overall precedence chain, with their own internal order: `&` > `^` > `|`.
- Almost every operator associates **left to right** — the standout exceptions are the **unary operators** and the **assignment operator**, which associate **right to left**.
- These precedence/associativity questions show up frequently in **technical interview rounds** and **online assessment (OA) rounds** — the skill here is purely mechanical repetition until it becomes automatic.