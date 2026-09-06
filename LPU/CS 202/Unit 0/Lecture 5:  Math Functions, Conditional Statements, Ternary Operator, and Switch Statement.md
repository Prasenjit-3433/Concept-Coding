# Lecture 5:  Math Functions, Conditional Statements, Ternary Operator, and Switch Statement

Status: Done

# 🎯Math Functions

### 1. Introduction

C++ gives you a toolbox of ready-made mathematical functions so you don't have to write the logic for power, square root, rounding, etc. yourself. These live inside the **C++ Standard Template Library (STL)** — the same library that makes C++ fast and convenient to write.

Think of it like a **calculator built into the language**. Instead of manually calculating `2 × 2 × 2` to get 2 cubed, you just say "give me 2 to the power 3" and C++ hands you the answer.

---

### 2. Number Types Recap (Quick Refresher)

Before using math functions, remember the two number types you're working with:

| Type | Meaning | Example |
| --- | --- | --- |
| **Integer** | Whole numbers, no decimal point. Range: −∞ to +∞ | `-2, -1, 0, 1, 2` |
| **Floating-point** | Numbers with a decimal point | `10.2, 11.33` |

Floating-point numbers come in two flavors:

- **float** → stores fewer digits after the decimal point
- **double** → stores more digits after the decimal point (more precision)

You already know arithmetic operators (`+`, `-`, `*`, `/`, `%`) can be applied to these numbers. Math functions take this further with ready-made operations.

---

### 3. Required Header Files

Just like `cout` needs `iostream`, math functions need their own header file.

```cpp
#include <iostream>   // needed for cout, cin
#include <cmath>      // needed for math functions (preferred)
// OR
#include <algorithm>  // min() and max() are also available here
```

> 💡 **Tip:** For `min()` and `max()`, either `<algorithm>` or `<cmath>` works, but `<cmath>` (from the C Math Library) is generally preferred.
> 

---

### 4. `min()` and `max()` Functions

These simply tell you which of two numbers is smaller or bigger.

```cpp
#include <iostream>
#include <cmath>
using namespace std;

int main() {
    int a = 10, b = 20;

    cout << min(a, b);   // Output: 10 (smallest of the two)
    cout << max(a, b);   // Output: 20 (largest of the two)

    return 0;
}
```

**Analogy:** Imagine two friends comparing their heights. `min()` points to the shorter one, `max()` points to the taller one — no manual comparison needed.

---

### 5. The C Math Library (`cmath` / `math.h`)

`cmath` (also called `math.h` in C) is a built-in library containing many ready-to-use mathematical functions.

```cpp
#include <cmath>
```

Let's go through the most commonly used ones.

#### 5.1 `pow(x, y)` — Power Function

Returns **x raised to the power y**.

```cpp
cout << pow(2, 3);   // Output: 8  (2 × 2 × 2)
```

#### 5.2 `sqrt(x)` — Square Root Function

Returns the square root of a number.

```cpp
cout << sqrt(25);    // Output: 5
```

#### 5.3 `floor(x)` and `ceil(x)` — Floor and Ceiling Functions

This is where beginners often get confused, so let's break it down carefully with a number line.

Take the number **1.5**:

```
        floor(1.5) = 1        ceil(1.5) = 2
             |                     |
   ----------●---------------------●----------
             1                    1.5   2
                    (below)             (above)
```

- **Floor** = the nearest whole number **below** (or equal to) the value
- **Ceiling** = the nearest whole number **above** (or equal to) the value

**Analogy:** Think of a building. The **floor** is the ground level below you, and the **ceiling** is the roof above you. `floor()` always rounds *down*, `ceil()` always rounds *up*.

```cpp
cout << floor(1.5);   // Output: 1
cout << ceil(1.5);    // Output: 2

cout << floor(5.8);   // Output: 5  (5 is below 5.8)
cout << ceil(5.1);    // Output: 6  (6 is above 5.1)
```

> ⚠️ **Note:** Even if the decimal part is `.9` (very close to the next number), `floor()` still rounds down and `ceil()` still rounds up — they don't "round to nearest."
> 

#### 5.4 `abs(x)` — Absolute Value Function

Converts a **negative number into its positive value**. Positive numbers stay unchanged.

```cpp
cout << abs(-10);   // Output: 10
cout << abs(-9);    // Output: 9
```

**Analogy:** Think of `abs()` as removing the "debt sign" from a number — whether you owe ₹10 or have ₹10, the *amount* is the same: 10.

#### 5.5 `round(x)` — Rounding Function

Rounds a decimal number to the **nearest** whole number (standard rounding rules).

```cpp
cout << round(3.6);   // Output: 4
```

---

### 6. Comparison Table: Common `cmath` Functions

| Function | Purpose | Example | Output |
| --- | --- | --- | --- |
| `pow(x, y)` | x raised to power y | `pow(2, 3)` | `8` |
| `sqrt(x)` | Square root of x | `sqrt(25)` | `5` |
| `floor(x)` | Round down to nearest integer | `floor(1.5)` | `1` |
| `ceil(x)` | Round up to nearest integer | `ceil(1.5)` | `2` |
| `abs(x)` | Absolute (positive) value | `abs(-10)` | `10` |
| `round(x)` | Round to nearest integer | `round(3.6)` | `4` |
| `min(a, b)` | Smaller of two values | `min(10, 20)` | `10` |
| `max(a, b)` | Larger of two values | `max(10, 20)` | `20` |

---

### 7. Generating Random Numbers (`rand()`)

Random numbers are used everywhere in real life — like generating an **OTP** (One-Time Password), which is just a set of random digits.

Unlike the other functions, `rand()` does **not** come from `cmath`. It comes from a different header file:

```cpp
#include <cstdlib>
```

```cpp
#include <iostream>
#include <cstdlib>
using namespace std;

int main() {
    cout << rand();          // generates a large random number
    cout << rand() % 10;     // restricts the random number to range 0–9
    return 0;
}
```

**Analogy:** `rand()` is like shaking a jar full of numbered balls (0 to a very large number) and picking one blindly. If you only want numbers between 0–9 (like a single OTP digit), you take the result **modulo 10**, which restricts the range.

---

### 8. Quick Recap Diagram

```
                    C++ MATH FUNCTIONS
                            |
        ---------------------------------------------
        |                   |                        |
   min() / max()      cmath functions            rand()
   (algorithm/cmath)         |                  (cstdlib)
                    ----------------------
                    |    |    |    |    |
                  pow  sqrt floor ceil abs/round

  pow(2,3)  = 8        floor(1.5) = 1        abs(-10) = 10
  sqrt(25)  = 5         ceil(1.5) = 2        round(3.6) = 4
```

**Key takeaway:** Math functions save you from writing manual logic — just remember which header file each function needs (`cmath` for most, `cstdlib` for `rand()`).

# 🎯Conditional Statements in C++ | IF, IF-ELSE, NESTED IF

---

## 1. Introduction

Until now, every program you've written executes **line by line, top to bottom** — this is called a **sequence statement**. But real-world decisions aren't always sequential. Sometimes you need your code to say: *"Do this ONLY IF a certain condition is true."*

That's exactly what **conditional statements** (also called **selection statements**) let you do.

**Analogy:** Imagine your dad says, *"If you score above 90 marks, I'll buy you a bike. Otherwise, you get my old shoes."* Your reward depends entirely on a **condition** (your marks). This is the core logic behind every conditional statement in C++.

---

## 2. The Three Types of Statements in C++

Before diving into conditions, it's worth knowing where they fit in the bigger picture:

| Type | What it does | Example |
| --- | --- | --- |
| **Sequence statements** | Execute line by line, in order | `a = 5; b = 10; sum = a+b;` |
| **Selection (conditional) statements** | Execute based on a condition | `if`, `if-else`, `switch` |
| **Iteration (looping) statements** | Repeat a task multiple times | `for`, `while`, `do-while` |

This lecture (and the next few) focuses on **selection statements**.

---

## 3. Types of Conditional Statements

There are **5 main types**, and we'll cover the first three today:

1. ✅ **if statement**
2. ✅ **if-else statement**
3. ✅ **nested if statement**
4. ⏭️ switch statement (covered later in this note)
5. ⏭️ ternary operator (covered later in this note)

---

## 4. The `if` Statement

### 4.1 Core Logic

> **If the condition is true → execute the statement.If the condition is false → skip the statement entirely (nothing happens).**
> 

```
        START
          |
          v
    ┌─────────────┐
    │  condition? │
    └──────┬──────┘
           |
     ┌─────┴─────┐
   TRUE         FALSE
     |             |
     v             v
 execute code   do nothing
     |             |
     └─────┬───────┘
           v
          END
```

### 4.2 Syntax

```cpp
if (condition) {
    // statement(s) to execute if condition is true
}
```

> 💡 **Rule:** If you're executing only **one statement**, curly braces `{ }` are optional. But if you want to execute **more than one statement**, curly braces are compulsory. This group of statements inside `{ }` is called a **block statement** — the same concept as everything you write inside `int main() { }`.
> 

### 4.3 Example

```cpp
#include <iostream>
using namespace std;

int main() {
    int marks;
    cout << "Enter your marks: ";
    cin >> marks;

    if (marks >= 33) {
        cout << "Pass" << endl;
        cout << "You will get a bike" << endl;   // block statement (2 lines → needs { })
    }

    return 0;
}
```

**Test runs:**

- Marks = 55 → Output: `Pass` and `You will get a bike`
- Marks = 20 → Output: *(nothing — condition was false)*

### 4.4 Combining Conditions with Logical Operators

Recall your logical operators (`&&` = AND, `||` = OR, `!` = NOT) — you can plug multiple conditions directly into an `if`.

**Using AND (`&&`)** — both conditions must be true:

```cpp
int marks = 75;
if (marks >= 60 && marks <= 100) {
    cout << "First Division" << endl;
}
```

Here, marks must be **between 60 and 100 (inclusive)**. If even one condition is false (e.g., marks = 110), the whole block is skipped.

**Using OR (`||`)** — at least one condition must be true:

```cpp
if (marks > 80 || grade == 'A') {
    cout << "You will get a bike" << endl;
}
```

Here, if **either** condition is true (high marks OR an A grade), the code runs. Only if **both** are false does it skip.

| Operator | Meaning | Executes when... |
| --- | --- | --- |
| `&&` (AND) | Both conditions must hold | Condition1 **AND** Condition2 are true |
| `||` (OR) | At least one condition must hold | Condition1 **OR** Condition2 is true |

---

## 5. The `if-else` Statement

### 5.1 Core Logic

The plain `if` statement has a gap: if the condition is false, *nothing* happens. But often, you want something to happen in **both** cases. That's what `if-else` solves.

> **If condition is true → execute statement 1 (the `if` block).If condition is false → execute statement 2 (the `else` block).**
> 

```
        START
          |
          v
    ┌─────────────┐
    │  condition? │
    └──────┬──────┘
           |
     ┌─────┴─────┐
   TRUE         FALSE
     |             |
     v             v
 if-block      else-block
     |             |
     └─────┬───────┘
           v
          END
```

### 5.2 Syntax

```cpp
if (condition) {
    // statement 1 - runs if condition is true
} else {
    // statement 2 - runs if condition is false
}
```

### 5.3 Example — Pass/Fail Check

```cpp
#include <iostream>
using namespace std;

int main() {
    int marks;
    cout << "Enter your marks: ";
    cin >> marks;

    if (marks >= 33) {
        cout << "Pass" << endl;
    } else {
        cout << "Fail" << endl;
    }

    return 0;
}
```

### 5.4 Example — Even or Odd

This example uses the **modulo operator (`%`)** you already know from arithmetic operators.

```cpp
int number = 5;

if (number % 2 == 0) {
    cout << "Even" << endl;
} else {
    cout << "Odd" << endl;
}
// number = 5 → 5 % 2 = 1 (not 0) → condition false → prints "Odd"
```

**Why this works:** `number % 2` gives the remainder when divided by 2. If the remainder is `0`, the number divides evenly → it's **even**. Any other remainder → it's **odd**.

---

## 6. The `if-else-if` Ladder (Construct)

### 6.1 When Do You Need This?

`if-else` handles **one** condition with two outcomes. But what if you have **multiple conditions to check one after another**? That's where the **if-else-if ladder** (also called **if-else-if construct**) comes in.

**Analogy:** Think of it like a grading system with multiple checkpoints — first check if marks are above 90 (Grade A), if not, check if above 75 (Grade B), if not, check above 50 (Grade C), and if none apply, fail them.

### 6.2 Syntax

```cpp
if (condition1) {
    // runs if condition1 is true
} else if (condition2) {
    // runs if condition1 is false AND condition2 is true
} else if (condition3) {
    // runs if condition1 & condition2 are false AND condition3 is true
} else {
    // runs if ALL conditions above are false
}
```

**How it flows:** C++ checks each condition **top to bottom**. The moment one condition is true, that block runs and the **rest of the ladder is skipped entirely** — even if later conditions would also be true.

### 6.3 Example — Grading System

```cpp
#include <iostream>
using namespace std;

int main() {
    int marks;
    cout << "Enter your marks: ";
    cin >> marks;

    if (marks >= 90) {
        cout << "Grade A" << endl;
    } else if (marks >= 80) {
        cout << "Grade B" << endl;
    } else if (marks >= 70) {
        cout << "Grade C" << endl;
    } else if (marks < 33) {
        cout << "Fail" << endl;
    } else {
        cout << "Grade D" << endl;
    }

    return 0;
}
```

**Test runs:**

- Marks = 55 → checks 90? No → 80? No → 70? No → <33? No → falls to `else` → **Grade D**
- Marks = 95 → **Grade A**
- Marks = 78 → **Grade C**
- Marks = 22 → **Fail**

> ⚠️ **Important:** Order matters! Since the ladder stops at the **first true condition**, always arrange checks from most restrictive to least restrictive (or in a logical sequence) to avoid wrong results.
> 

---

## 7. Nested `if` Statement

### 7.1 Core Logic

A **nested if** is simply **an `if` statement written inside another `if` statement**. You use this when a decision depends on **multiple related conditions**, checked one after another, where the second condition only matters if the first one is true.

**Analogy:** Dad says: *"IF you score 90 in Maths, THEN I'll check — did you also score 80 in Science? If yes, you get a bike. If no, you get some other reward. But if you didn't even get 90 in Maths, you get Dad's old shoes — no need to check Science at all."*

Notice: the Science condition is only ever checked **if** the Maths condition passed first.

### 7.2 Syntax

```cpp
if (condition1) {
    // condition1 is true, now check condition2
    if (condition2) {
        // both condition1 AND condition2 are true
    } else {
        // condition1 true, but condition2 false
    }
} else {
    // condition1 itself is false — condition2 is never even checked
}
```

### 7.3 Example — College Admission Check

```cpp
#include <iostream>
using namespace std;

int main() {
    int age, marks;

    cout << "Enter your age: ";
    cin >> age;
    cout << "Enter 12th marks: ";
    cin >> marks;

    if (age >= 18) {
        if (marks >= 60) {
            cout << "Admission successful" << endl;
        } else {
            cout << "Marks are insufficient for admission" << endl;
        }
    } else {
        cout << "You are underage" << endl;
    }

    return 0;
}
```

**Test runs:**

- Age = 19, Marks = 80 → **Admission successful**
- Age = 20, Marks = 55 → **Marks are insufficient for admission**
- Age = 17 → **You are underage** *(marks are never even evaluated for admission logic)*

### 7.4 Nested-if Flow Diagram

```
                    START
                      |
                      v
              ┌───────────────┐
              │  age >= 18 ?   │
              └───────┬───────┘
                       |
          ┌───────────┴───────────┐
        TRUE                      FALSE
          |                         |
          v                         v
  ┌───────────────┐       "You are underage"
  │ marks >= 60 ?  │
  └───────┬───────┘
          |
    ┌─────┴─────┐
  TRUE         FALSE
    |             |
    v             v
"Admission      "Marks are
 successful"     insufficient"
```

---

## 8. Comparison Table: `if` vs `if-else` vs `if-else-if` vs Nested `if`

| Construct | Use case | Number of outcomes |
| --- | --- | --- |
| `if` | Execute code only when one condition is true; do nothing otherwise | 1 |
| `if-else` | Execute one of two blocks depending on a single condition | 2 |
| `if-else-if` | Check multiple *independent, sequential* conditions | Many |
| Nested `if` | Check a condition **only after** another condition is already true (dependent conditions) | Depends on nesting depth |

---

## 9. Quick Recap Diagram

```
                     CONDITIONAL STATEMENTS
                              |
        --------------------------------------------------
        |              |              |                  |
       if           if-else      if-else-if          nested if
   (1 outcome)    (2 outcomes)  (multiple,          (dependent
                                sequential          conditions)
                                 checks)

  if (m>=33)      if(m>=33)      if(m>=90) A         if(age>=18){
  cout<<"Pass";   cout<<"Pass";  else if(m>=80) B       if(marks>=60)
                  else            else if(m>=70) C        "Admission"
                  cout<<"Fail";   else D                else
                                                          "Insufficient"
                                                       } else "Underage"

  Logical Operators inside if:
  &&  → BOTH conditions must be true
  ||  → AT LEAST ONE condition must be true
```

**Key takeaway:** Use `if` for single yes/no checks, `if-else` when you need a fallback, `if-else-if` for a sequence of independent conditions, and **nested if** when one condition only makes sense to check after another has passed.

# 🎯Ternary Operator (Shorthand If)

---

## 1. Introduction

You've just learned `if-else` — but what if your `if-else` only needs to execute **one simple statement** on each side? Writing 4-5 lines for something this small feels like overkill. That's exactly the problem the **ternary operator** solves.

The ternary operator is a **shorthand (short form) way to write `if-else`**. It's also called the **conditional operator**, and its symbol is `? :` (question mark and colon).

**Why "ternary"?** Because it works on **three operands**: the condition, the true-case expression, and the false-case expression.

---

## 2. Syntax

```cpp
condition ? expression1 : expression2;
```

**How to read this:**

> "If `condition` is true, execute/return `expression1`. If `condition` is false, execute/return `expression2`."
> 

```
condition ? expression1 : expression2;
    |            |             |
    |            |             └── runs if condition is FALSE
    |            └──────────────── runs if condition is TRUE
    └───────────────────────────── the thing being checked
```

---

## 3. Ternary vs if-else — Side by Side

**Using if-else:**

```cpp
if (age >= 18) {
    status = "Adult";
} else {
    status = "Minor";
}
```

**Using ternary (same logic, one line):**

```cpp
status = (age >= 18) ? "Adult" : "Minor";
```

Both produce the **exact same output** — the ternary version is just more compact.

> ⚠️ **Important limitation:** The ternary operator is meant for a **single statement** on each side — you cannot use it to execute multiple statements or a block of code like you can with `if-else`. It's "perfect for simple conditions" where only one action needs to happen per branch.
> 

---

## 4. Basic Example

```cpp
#include <iostream>
using namespace std;

int main() {
    int marks = 10;
    cout << (marks > 30 ? "Pass" : "Fail");   // Output: Fail
    return 0;
}
```

Here, since `marks (10) > 30` is false, the expression after the colon (`"Fail"`) is printed.

---

## 5. Ternary Operator with Assignment

A very common use is directly **assigning a value** to a variable based on a condition.

```cpp
int a = 20, b = 30;
int max = (a > b) ? a : b;
cout << max;   // Output: 30
```

**Walkthrough:** Since `a (20) > b (30)` is false, the value after the colon — `b`, which is `30` — gets stored in `max`.

---

## 6. Ternary Operator with `cout` Directly

You can even skip the variable entirely and use the ternary operator straight inside `cout`.

```cpp
int x = 7;
(x % 2 == 0) ? cout << "Even" : cout << "Odd";   // Output: Odd
```

**Walkthrough:** `7 % 2` gives remainder `1`, so the condition `(x % 2 == 0)` is false → the statement after the colon runs → prints `"Odd"`.

---

## 7. Nested Ternary Operator

Just like nested `if`, you can **nest ternary operators** to check multiple conditions in sequence — useful for building compact grading logic.

```cpp
int marks = 85;
string grade = (marks >= 90) ? "A" :
               (marks >= 75) ? "B" :
               (marks >= 50) ? "C" : "Fail";

cout << grade;   // Output: B
```

**How it evaluates step by step:**

1. Is `marks >= 90`? → `85 >= 90` → **False** → move to next check
2. Is `marks >= 75`? → `85 >= 75` → **True** → return `"B"`, stop here

This is essentially the same logic as an `if-else-if` ladder, just written in a single compact line.

```
marks >= 90 ?  "A"  :
marks >= 75 ?  "B"  :
marks >= 50 ?  "C"  :
               "Fail"
```

> 💡 **Readability tip:** While nested ternaries are compact, they can get hard to read if overused. For 2-3 conditions, they're fine; for anything more complex, an `if-else-if` ladder is usually clearer.
> 

---

## 8. Comparison Table: Ternary vs if-else

| Feature | `if-else` | Ternary Operator |
| --- | --- | --- |
| Number of statements per branch | Multiple (needs `{ }`) | Only **one** expression |
| Readability for complex logic | Better | Can get messy if nested too much |
| Best used for | Multi-line logic, blocks | Simple, single-value decisions |
| Symbol | `if`, `else` keywords | `?` and `:` |

---

## 9. Quick Recap Diagram

```
                    TERNARY OPERATOR (? :)
                     Shorthand for if-else
                              |
        --------------------------------------------------
        |                    |                            |
   Basic form           With Assignment            Nested Ternary
        |                    |                            |
 cond ? "Pass"        int max = (a>b)          (m>=90)?"A":
       : "Fail"              ? a : b                  (m>=75)?"B":
                                                        (m>=50)?"C":"Fail"

  Rule: condition ? expr1(if true) : expr2(if false)
  Limitation: only ONE statement per branch — no blocks allowed
```

**Key takeaway:** Reach for the ternary operator when your `if-else` collapses down to a single value or single statement per branch — it keeps code shorter without losing clarity.

# 🎯Switch Statement | Conditional Statement

---

## 1. Introduction

The **switch statement** is often described as the **"big brother" of the if-else-if ladder**. When you have **one variable** that needs to be checked against **many possible fixed values**, `if-else-if` can get long and repetitive. `switch` gives you a cleaner, more organized way to write the same logic.

**Analogy:** Think of a switch statement like a **directory/menu board** at a building entrance — you check ONE thing (say, floor number) and directly get sent to the right floor, instead of checking "is it floor 1? is it floor 2? is it floor 3?" one by one.

---

## 2. Motivating Example

Suppose you ask the user to enter a day number (1–7) and want to print the day's name:

```cpp
int day;
cout << "Enter a day number from 1 to 7: ";
cin >> day;

switch (day) {
    case 1: cout << "Today is Monday"; break;
    case 2: cout << "Today is Tuesday"; break;
    case 3: cout << "Today is Wednesday"; break;
    default: cout << "Invalid Day"; break;
}
```

Instead of writing `if (day==1)`, `else if (day==2)`, `else if (day==3)`... you simply check the variable **once** and branch into the matching **case**.

---

## 3. Syntax

```cpp
switch (expression) {
    case value1:
        // statements
        break;

    case value2:
        // statements
        break;

    // ... more cases

    default:
        // statements (runs if no case matches)
}
```

**How it works, step by step:**

1. `expression` is evaluated **once**.
2. Its value is compared against each `case` value, top to bottom.
3. The **matching case executes**.
4. `break` stops execution and exits the switch.
5. If **no case matches**, the `default` block runs (this is optional but recommended, and behaves like the `else` in an if-else).

---

## 4. The `break` Keyword — Why It's Critical

`break` is a keyword used to **stop the program flow** and exit the switch block immediately. Once a matching case finishes and hits `break`, control jumps outside the switch entirely — the remaining cases are **not checked or executed**.

> ⚠️ This is why you should put a `break` at the end of **every case** (except when you deliberately want fall-through — covered in Section 6).
> 

---

## 5. Data Types Supported by `switch`

A switch statement can only work with specific data types:

| Supported ✅ | Not Supported ❌ |
| --- | --- |
| `int` | `float` |
| `char` | `double` |
| `enum` | `string` |
| `short` |  |
| `long` (implementation-dependent) |  |
| `bool` |  |

**Why no floats/strings?** Switch relies on exact matching of discrete, countable values (integral types) — it isn't built to compare continuous decimal values or sequences of characters.

---

## 6. Full Working Example

```cpp
#include <iostream>
using namespace std;

int main() {
    int day;
    cout << "Enter a day number from 1 to 7: ";
    cin >> day;

    switch (day) {
        case 1: cout << "Today is Monday";    break;
        case 2: cout << "Today is Tuesday";   break;
        case 3: cout << "Today is Wednesday"; break;
        case 4: cout << "Today is Thursday";  break;
        case 5: cout << "Today is Friday";    break;
        case 6: cout << "Today is Saturday";  break;
        case 7: cout << "Today is Sunday";    break;
        default: cout << "Invalid Day";       break;
    }

    return 0;
}
```

**Test runs:**

- Day = 3 → **Today is Wednesday**
- Day = 7 → **Today is Sunday**
- Day = 20 → **Invalid Day** *(no case matches → default runs)*

---

## 7. Switch Flow Diagram

```
                    START
                      |
                      v
              ┌───────────────┐
              │  expression     │
              └───────┬───────┘
                       |
       ┌──────────────┼──────────────┬───────────────┐
       v               v                v                v
   case 1?         case 2?          case 3?         no match?
       |               |               |                 |
     match           match           match            default
       |               |               |                 |
       v               v               v                 v
   execute code    execute code    execute code     execute code
       |               |               |                 |
     break           break           break             (ends)
       |               |               |                 |
       └──────────────┴──────┬──────┴───────────────┘
                                v
                               END
```

---

## 8. Fall-Through Behaviour

### 8.1 What Is Fall-Through?

**Fall-through** happens when you **forget to put `break`** after a case. When this happens, control does **not** stop there — it automatically continues into the **next case**, executing it too, even though that case's value doesn't match the input. This continues until a `break` is finally hit, or the switch ends.

> **Definition:** The absence of a `break` statement causes the code to continue executing until it hits a `break`.
> 

### 8.2 Example

```cpp
int x = 2;

switch (x) {
    case 1:
        cout << "One";
    case 2:
        cout << "Two";
    case 3:
        cout << "Three";
}
```

**Output:** `TwoThree`

**Walkthrough:**

1. `x = 2` → matches `case 2` → prints `"Two"`
2. No `break` after `case 2` → execution **falls through** into `case 3`
3. `case 3` prints `"Three"` (even though `x` was never `3`!)
4. No more cases and no `break` → switch simply ends

```
x = 2
   |
   v
case 2: print "Two"  ──(no break)──> case 3: print "Three" ──(end)
```

> ⚠️ If there had been a `default` case after `case 3` with no `break` in `case 3` either, the `default` would have executed too! Fall-through keeps cascading until a `break` stops it.
> 

### 8.3 When Is Fall-Through Actually Useful?

While fall-through is usually an accidental bug, it becomes a **deliberate, useful technique** when you want **multiple case values to trigger the same result** — like grouping vowels together:

```cpp
char ch = 'e';

switch (ch) {
    case 'a':
    case 'e':
    case 'i':
    case 'o':
    case 'u':
        cout << "Vowel";
        break;
    default:
        cout << "Consonant";
}
```

**Why this works:** Cases `'a'`, `'e'`, `'i'`, `'o'`, `'u'` have **no code and no break** between them — they all "fall through" into the single `cout << "Vowel"` statement. Only after that does `break` finally stop execution. This avoids repeating `cout << "Vowel";` five times.

---

## 9. Comparison Table: `if-else-if` vs `switch`

| Feature | if-else-if ladder | switch statement |
| --- | --- | --- |
| Best for | Range checks (`marks >= 90`), complex/multiple variable conditions | Checking ONE variable against many fixed, exact values |
| Data types | Any (int, float, string, bool expressions...) | Only `int`, `char`, `enum`, `short`, `long`, `bool` |
| Readability with many conditions | Gets messy | Cleaner and more organized |
| Needs explicit stop keyword? | No | Yes — `break` (or it falls through) |
| Default/fallback case | `else` | `default` |

---

## 10. Quick Recap Diagram

```
                        SWITCH STATEMENT
                              |
        ---------------------------------------------------
        |                     |                            |
     Syntax               break keyword              Fall-Through
        |                     |                            |
  switch(expr){        Stops execution,          No break → moves to
    case val: ...;      exits the switch          next case automatically
    break;                                        (accidental bug OR
    default: ...;                                  intentional grouping)
  }

  Supported types: int, char, enum, short, long, bool
  NOT supported:   float, double, string

  Grouping trick:
  case 'a': case 'e': case 'i': case 'o': case 'u':
      cout << "Vowel"; break;
```

**Key takeaway:** Use `switch` when one variable needs to be matched against several exact values — it's cleaner than a long if-else-if chain. Always remember `break`, unless you deliberately want cases to fall through (like grouping vowels).

---