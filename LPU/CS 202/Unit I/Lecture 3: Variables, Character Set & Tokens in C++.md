# Lecture 3: Variables, Character Set & Tokens in C++

Status: Done

## 1. What is a Variable?

A **variable** is simply a *name* — a name that lets you access data stored somewhere in memory.

> A variable is a named storage location in memory that can hold a value of a specified type, and whose value may change over time.
> 

Think of it like this: if you want to store `10` in memory, C++ creates a labeled box for it in memory. That label — the name you give the box — is the variable. You could name that box `value`, `sachin`, `learnyard`, anything you like. Whatever name you choose, that box now holds `10`.

The key part of the definition is *"may change over time"* — once you set a variable to `10`, you're free to update it to something else later.

---

## 2. Declaring vs. Initializing a Variable

To create a variable, you write two things: its **data type**, and its **name**.

```cpp
int marks;
```

This alone creates the variable — a block of memory is reserved and labeled `marks`. This step is called **declaration**.

Now, if you put an actual value inside it:

```cpp
marks = 75;
```

...this step is called **initialization**.

Here's an analogy: opening a bank account is like *declaring* it — the account exists. Depositing money into it for the first time is like *initializing* it — the account becomes active.

### Garbage values

If you only declare a variable and never initialize it, does that mean it holds *nothing*? Not quite.

```cpp
int marks;   // no value assigned
```

C++ will place some **garbage value** inside it — leftover data that happened to be sitting in that memory location before. It's unpredictable, so never assume an uninitialized variable is empty or zero.

### The three ways to write it

```cpp
int marks;        // 1. Declaration only
marks = 75;        // 2. Initialization (on a separate line)

int y = 20;         // 3. Declaration + Initialization together
```

A few more examples across data types:

```cpp
int age = 20;
float pi = 3.14;
double balance = 992.6;
char initial = 'S';
```

---

## 3. Accessing and Updating Variables

### Accessing

To read a variable's current value, simply `cout` it:

```cpp
int marks = 74;
cout << marks;     // Output: 74
```

### Updating

To change a variable's value, just assign it a new one:

```cpp
int marks = 93;
marks = 45;          // marks is now 45
```

You can also update a variable **based on its own current value**:

```cpp
int y = 10;
y = y + 2;    // take the current value of y (10), add 2, store back into y
              // y is now 12
```

### Walking through execution order — step by step

```cpp
int marks;             // Step 1: block "marks" created in memory
marks = 88;              // Step 2: 88 stored inside marks
cout << marks << endl;   // Step 3: prints 88
marks = 70;               // Step 4: marks updated to 70
cout << marks << endl;    // Step 5: prints 70
```

**Output:**

```
88
70
```

This matters because your compiler runs code **line by line, in order**. The `cout` on line 3 executes *before* the update on line 4 — so it still prints the old value, `88`. This is a common source of confusion for beginners, so trace through the lines carefully whenever the output surprises you.

---

## 4. Declaring Multiple Variables at Once

If you need several variables of the same type, you can declare them all in a single line, separated by commas.

**Same value for all:**

```cpp
int x, y, z;
x = y = z = 50;      // all three now hold 50
```

**Different values for each:**

```cpp
int x = 10, y = 20, z = 30;
```

**Mixing declared-only and initialized:**

```cpp
int num1 = 10, num2;
// num1 is declared AND initialized
// num2 is only declared — it holds a garbage value for now
```

---

## 5. Adding Variables Together

You can use variables in expressions just like you'd expect:

```cpp
int x = 5;
int y = 6;
int sum = x + y;      // sum = 11
cout << "Sum = " << sum;
```

---

## 6. The C++ Character Set

Every C++ program is built out of a fixed set of characters. This is called the **character set**, and it's made up of 5 categories:

```
┌─────────────────────────────────────────────────┐
│               C++ CHARACTER SET                 │
├────────────┬────────────┬────────────┬──────────┤
│  Letters   │  Digits    │  Special   │ White-   │
│  (A-Z,a-z) │  (0-9)     │  Characters│ spaces   │
└────────────┴────────────┴────────────┴──────────┘
                    +
            Escape Sequences
```

1. **Letters** — e.g. `i`, `n`, `t` in `int main`
2. **Digits** — any numeral, `0` through `9`
3. **Special characters** — curly braces `{}` (used for functions), square brackets `[]` (used for arrays), semicolons `;`, commas `,`, the hashtag `#`, and more
4. **Whitespace** — the empty gaps between tokens (like the space between `int` and `a`)
5. **Escape sequences** — special symbols that represent whitespace or special characters inside strings

### Escape sequences in detail

| Sequence | Meaning |
| --- | --- |
| `\n` | New line |
| `\t` | Tab |
| `\\` | Backslash |
| `\'` | Single quote |
| `\"` | Double quote |

**Example — `\t` (tab):**

```cpp
cout << "Welcome to \t LearnYard \t";
```

Wherever `\t` appears, a **tab space** is inserted into the output.

**Example — `\n` (newline):**

```cpp
cout << "Welcome to LearnYard \n for learning C++";
```

Even though the code is written across two lines in your source file, that alone does **not** make the output print on two lines. Only `\n` forces the next part of the text onto a new line.

**Example — inserting double quotes inside a string:**

If you want the word `LearnYard` to actually appear surrounded by quotation marks in the output, you can't just type `"` directly inside your string — you need to escape it with a backslash:

```cpp
cout << "Welcome to \"LearnYard\"";
```

**Output:**

```
Welcome to "LearnYard"
```

---

## 7. Tokens in C++ — The Full Picture

Earlier, we learned that a **token** is the smallest building block of a C++ program. Now let's look at **all 7 types** of tokens:

```
┌──────────────────────────────────────────────────┐
│                    TOKENS                        │
├──────────┬────────────┬─────────────┬────────────┤
│ Keywords │ Identifiers│ Constants/  │  Strings   │
│          │            │ Literals    │            │
├──────────┴────────────┴─────────────┴────────────┤
│  Operators  │  Special Symbols  │ Punctuators/   │
│             │                   │ Delimiters     │
└──────────────────────────────────────────────────┘
```

### a) Keywords

Keywords are **reserved words** — words the C++ language has already assigned a special meaning to. You cannot use them as variable names.

```cpp
int int = 10;      // ❌ error — "int" is a keyword, can't be a variable name
int float;          // ❌ error — "float" is a keyword too
```

Some common keywords: `int`, `double`, `if`, `while`, `return`, `void`, `class`, `namespace`, `static`, `inline`, `auto`, `for`, `switch`, `case`, `template`, `virtual`, `public`, `private`, `protected`.

*(You'll meet most of these gradually as the course progresses — don't worry if some look unfamiliar right now.)*

### b) Identifiers

An **identifier** is just another name for a variable (or function, or anything you name yourself).

> C++ variables must be identified with unique names — these unique names are called identifiers.
> 

Write **meaningful** identifiers, especially on large codebases — a name like `marks` instantly tells a reader what's being stored, unlike a vague name like `x1`.

A function's name is also an identifier — for example, a function called `add()` clearly signals that it performs addition.

### Rules for naming identifiers

| Rule | Example |
| --- | --- |
| Must begin with a letter or underscore | `sachin`, `_data` ✅    `9sachin` ❌ |
| Can contain letters, digits, underscore *(after the first character)* | `sachin9` ✅ |
| Case-sensitive | `marks` and `Marks` are two *different* variables |
| No whitespace or special characters allowed | `sa chin` ❌, `sachin#1` ❌ |
| Cannot be a reserved keyword | `int`, `float`, etc. ❌ |

**Valid identifiers:** `sum`, `_data`, `totalMarks`**Invalid identifiers:** `2sum` (starts with digit), `float` (keyword), `sum#` (special character)

### c) Constants / Literals

Fixed values that don't change: `10`, `3.14`, `'A'`, `"Hello"` — we'll cover these in detail shortly.

### d) Strings

A collection of characters wrapped in double quotes:

```cpp
"Sachin Dwivedi"
```

### e) Operators

Symbols used to perform operations, e.g., in `a + b`, the `+` is an **operator token**. *(Operators get their own dedicated lecture later in the course.)*

### f) Special Symbols

Symbols like `{}`, `[]`, `()`, `,`, `#`, etc. — used for the structure and syntax of your program.

### g) Punctuators / Delimiters

Symbols that separate parts of your code — most notably the **semicolon** (`;`), which marks the end of a statement, along with `{}` for blocks and `,` as a separator.

---

## 8. Scope of a Variable

**Scope** defines *where in your program* a variable can be accessed.

> Scope is the region of the source code in which a variable or function is accessible. Outside that region, the name has no validity.
> 

A variable can have one of two scopes:

### Local Scope

A variable declared **inside a function** (inside its curly braces `{ }`) only exists within that function. Outside it, the variable simply doesn't exist.

### Global Scope

A variable declared **outside any function** exists throughout the entire program — it can be accessed from anywhere, inside or outside functions.

### Example — watch what happens

```cpp
#include <iostream>
using namespace std;

void func() {
    int a = 7;              // local variable — only exists inside func()
    cout << a << endl;       // ✅ works fine — prints 7
}

int b = 20;                   // global variable

int main() {
    func();                    // must call func() for its code to run
    cout << b << endl;          // ✅ works — b is global, accessible anywhere
    cout << a << endl;          // ❌ ERROR — "a was not declared in this scope"
    return 0;
}
```

```
┌─────────────────────────────────────────┐
│         GLOBAL SCOPE                    │
│   int b = 20;   ← accessible EVERYWHERE │
│                                         │
│  ┌───────────────────────────────┐      │
│  │   func() { }  ← LOCAL SCOPE   │      │
│  │   int a = 7;                  │      │
│  │   ← only accessible in here   │      │
│  └───────────────────────────────┘      │
│                                         │
│  ┌─────────────────────────────┐        │
│  │   main() { }                │        │
│  │   can access: b ✅, a ❌      │        │
│  └─────────────────────────────┘        │
└─────────────────────────────────────────┘
```

`a` was declared inside the curly braces (the "block") of `func()`, so its **local scope** is limited to that block. `b` was never placed inside any block, so it lives in the **global scope** and is visible everywhere — inside `func()`, inside `main()`, anywhere in the program.

---

## 9. Variable Shadowing

What happens if a local variable and a global variable have the **exact same name**?

```cpp
int globalVariable = 10;     // global

void printFunction() {
    int globalVariable = 20;    // local — same name!
    cout << globalVariable;      // which one gets printed?
}
```

**Output inside `printFunction()`:** `20`

Even though there's a global `globalVariable` holding `10`, the **local** version — because it's inside the function's own scope — takes priority. The local variable **shadows** (hides) the global one, but *only within that function's scope*.

> Variable shadowing: a local variable hides a global variable of the same name within that specific local scope.
> 

Outside `printFunction()` (say, inside `main()`), the local variable doesn't exist, so referencing `globalVariable` there refers back to the **global** one (`10`).

### Explicitly accessing the global variable

If you're inside a scope where shadowing is happening, but you specifically want the **global** version, use the **scope resolution operator** `::`:

```cpp
int number = 100;             // global

void someFunction() {
    int number = 5;             // local, shadows the global one
    cout << number;              // prints 5 (local)
    cout << ::number;            // prints 100 (explicitly global, via ::)
}
```

---

## 10. Constants and Literals

A **variable** can change value over time — but sometimes you want a value that should **never** change once set. That's a **constant**.

> Constants (also called literals) are fixed values that cannot be altered during program execution.
> 

There are **two ways** to create a constant in C++:

### a) Using the `const` keyword

```cpp
const int marks = 66;
marks = 70;    // ❌ error — cannot modify a const variable
```

Once declared `const`, the variable becomes **read-only** — you can view/access it, but never modify it.

### b) Using `#define`

```cpp
#define PI 3.14
#define MINUTES_PER_HOUR 60
```

This is written at the very top of your program (right where you'd write `#include`). Anywhere in your program you use `PI`, the preprocessor substitutes `3.14` before compilation — and its value can never be changed afterward.

---

## Quick Recap Diagram

```
VARIABLES
─────────
datatype variable_name = value;

Declaration only:            int x;
Initialization:               x = 10;
Declare + Initialize:        int y = 20;
Access:                       cout << x;
Update:                       x = 15;
Multiple at once:            int a = 1, b = 2, c = 3;

CHARACTER SET
─────────────
Letters + Digits + Special Characters + Whitespace + Escape Sequences
Escape sequences: \n (newline)  \t (tab)  \\ (backslash)  \' \" (quotes)

TOKENS (7 types)
────────────────
1. Keywords     → reserved words (int, if, return...) — can't be identifiers
2. Identifiers  → names you give to variables/functions (must follow naming rules)
3. Constants    → fixed values (10, 3.14, 'A', "Hello")
4. Strings      → text in double quotes
5. Operators    → +, -, *, /, =, ==...
6. Special Symbols → {}, [], (), #
7. Punctuators  → ; (end statement), {} (block), , (separator)

SCOPE
─────
Local  → declared inside a function/block → accessible ONLY there
Global → declared outside all functions   → accessible EVERYWHERE

Variable Shadowing → local variable of same name hides the global one
                      (use :: to force access to the global one)

CONSTANTS
─────────
const int x = 10;         // method 1
#define PI 3.14            // method 2 (preprocessor)
→ value can never be changed once set
```