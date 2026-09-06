# Lecture 8: Functions in C++

Status: Done

# 🎯Part 1 — Fundamentals

## 1. What Is a Function? — The Machine Analogy

> A function is a machine: you give it some raw material as input, and it gives you processed output.
> 

Picture an actual machine: you feed potatoes in on one side, and gold comes out the other side.

```
   INPUT              MACHINE (function)              OUTPUT
┌──────────┐      ┌──────────────────────┐       ┌───────────┐
│ Potatoes │ ───▶ │                      │ ───▶  │   Gold    │
└──────────┘      └──────────────────────┘       └───────────┘
```

A function works exactly the same way — it takes some input, does some processing internally, and hands you back a result.

### Worked Example — A `sum` Function

Suppose we create a function called `sum` that takes two numbers, `a` and `b` (say `a = 10`, `b = 20`), and internally creates a variable `z`, calculates `z = a + b`, and gives `z` back as the output (`30`).

> A function is a block of code that performs a specific task, helps in code reuse, modularity, and readability.
> 

---

## 2. The Technical Vocabulary

| Term | Meaning |
| --- | --- |
| **Function name** | The identifier for the function (e.g., `sum`) |
| **Parameters** | The inputs the function accepts |
| **Function body** | Where the function's logic/work is written |
| **Return statement** | The output the function sends back |

```cpp
int sum(int a, int b) {   // ← name, parameters, and return type all live here
    int c = a + b;         // ← function body
    return c;               // ← return statement
}
```

---

## 3. The Three Things Every Function Declaration Needs

Just like declaring a variable requires a **data type** and a **name** (`int a = 10;`), declaring a function requires **three** things:

```
return_type   function_name   (parameters)
    ▼               ▼                ▼
   int              sum         (int a, int b)
```

1. **Return type** — what *kind* of value the function will hand back (`int`, `float`, `char`, `bool`, or `void` if nothing is returned)
2. **Function name** — what you'll call it (`sum`)
3. **Parameters** — what inputs it accepts, and their data types

### Why Does `c`'s Type Decide the Return Type?

Since `a` and `b` are both `int`, adding them (`a + b`) also produces an `int` — so `c` is `int`, and that's exactly what gets written as the function's return type.

> Whatever the function is returning, you need to mention its data type — that's exactly what "return type" means.
> 

---

## 4. The Complete Syntax

```cpp
return_type function_name(parameters) {
    // function body
}
```

### Real Example

```cpp
int add(int a, int b) {
    return a + b;
}
```

---

## 5. Function Declaration vs. Definition vs. Call

These three terms often get mixed up — here's the exact distinction:

| Term | What It Means | Example |
| --- | --- | --- |
| **Declaration (Prototype)** | Tells the compiler the function's name, return type, and parameters — without the actual body | `int add(int, int);` |
| **Definition** | The *actual code* — the real implementation of the function | `int add(int a, int b) { return a + b; }` |
| **Call** | Actually *running* the function by writing its name with arguments | `int result = add(5, 3);` |

### Why Does the Function Need to Be *Called*?

> Just like a machine in a factory doesn't run on its own — you have to press the button and feed in the raw material — a function doesn't run automatically either. You have to call it, and you have to give it input.
> 

A function call **always happens inside `main`** (or inside some other function that itself eventually gets called from `main`) — because the compiler only executes what's inside `main`.

### Tracing a Function Call — `sum(a, b)`

```cpp
int sum(int a, int b) {
    int c = a + b;
    return c;
}

int main() {
    int a = 3, b = 6;
    cout << sum(a, b);   // function call
    return 0;
}
```

```
main() has a = 3, b = 6
   ↓
sum(a, b) is called → 3 and 6 are passed in
   ↓
inside sum: c = a + b = 3 + 6 = 9
   ↓
return c  → hands back 9
   ↓
cout prints 9
```

> Interesting side note: `main` itself is technically a function too! It has a return type (`int`), a name (`main`), no parameters in the simplest form, a body, and a `return 0;` statement.
> 

---

## 6. Parameters vs. Arguments — A Common Interview Question

This distinction comes up constantly in interviews, so it's worth locking in precisely:

> Parameters are the values a function accepts (defined in the function's own declaration). Arguments are the values you actually pass in at the time you call the function.
> 

```cpp
int sum(int a, int b) {   // 'a' and 'b' here are PARAMETERS
    return a + b;
}

int main() {
    cout << sum(3, 6);     // '3' and '6' here are ARGUMENTS
    return 0;
}
```

---

## 7. Why Use Functions At All?

> Avoids rewriting the same code, makes debugging easier, breaks complex problems into smaller parts, and improves code organization.
> 
- **Code reusability** — write the logic once, call it as many times as you need, instead of retyping `c = a + b` everywhere.
- **Reduces code length** — `main` doesn't get bloated with repeated logic.
- **Easier debugging** — if something's wrong with addition, you know exactly where to look: inside the `sum` function.
- **Modular structure** — the code is organized into clean, self-contained blocks instead of one giant mess.

---

## 8. Two Types of Functions

```
┌───────────────────────────────────────────────────┐
│                  FUNCTIONS                        │
├─────────────────────┬─────────────────────────────┤
│  Built-in Functions │   User-Defined Functions    │
│  (already written   │   (created by YOU)          │
│   for you)          │                             │
├─────────────────────┼─────────────────────────────┤
│  sqrt(), pow(),     │  sum(), multiply(),         │
│  cin, cout, etc.    │  greet(), etc.              │
└─────────────────────┴─────────────────────────────┘
```

---

## 9. The Four Categories of User-Defined Functions

Based on whether a function takes parameters and whether it returns a value, every function falls into one of these four combinations:

### a) No Return Type, No Parameters

```cpp
void greet() {
    cout << "Hello!";
}
```

Here, the function doesn't need any input, and it doesn't hand anything back — it just performs an action directly (printing something).

> Since this function isn't returning any value, we write its return type as `void` — meaning "nothing," "empty," "hollow."
> 

> Important distinction: the `cout` inside a function is **not** the same as a `return` statement. `cout` just prints something to the screen — the function might still return `void` even though it visibly produces output.
> 

### b) No Return Type, With Parameters

```cpp
void printSum(int a, int b) {
    cout << a + b;
}
```

This one *does* take input (`a` and `b`), but still doesn't return a value — it directly prints the result and finishes.

### c) Return Type, No Parameters

```cpp
int getNumber() {
    return 10;
}
```

This function needs no input at all, but it does hand back a value when called.

### d) Return Type, With Parameters

```cpp
int multiply(int a, int b) {
    return a * b;
}
```

The most common shape — takes input, processes it, and returns a result. This was exactly our `sum` example.

---

## 10. Default Parameters

Sometimes you want a function to work sensibly even if the caller doesn't provide a value.

### The Problem `greet` Solves

```cpp
void greet(string name = "Guest") {
    cout << "Hello " << name;
}
```

Here, `name` is given a **default value** of `"Guest"`. This means:

- If you call `greet("Aditya")` → the argument `"Aditya"` **overrides** the default → prints `Hello Aditya`
- If you call `greet()` with nothing → C++ falls back to the default → prints `Hello Guest`

```
greet("Aditya")  →  name = "Aditya"  →  "Hello Aditya"
greet()          →  name = "Guest" (default)  →  "Hello Guest"
```

> This matches the instructor's PDF notes exactly: "Assign default values to parameters," e.g. `int add(int a, int b = 5) { return a + b; }` — if the second argument isn't provided, `b` automatically becomes `5`.
> 

---

## 11. Multiple Parameters

We've already seen this pattern with `sum` and `multiply` — functions can accept more than one input at once.

```cpp
int multiply(int a, int b) {
    return a * b;
}

int main() {
    cout << "Product = " << multiply(3, 4);
    return 0;
}
```

**Output:** `Product = 12`

---

## 12. Function Overloading

> When we create multiple functions with the same name, it's called function overloading.
> 

The trick: the **data type of the arguments** you pass determines which version of the function actually runs.

```cpp
int sum(int a, int b) {
    return a + b;
}

double sum(double a, double b) {
    return a + b;
}

int main() {
    cout << "Sum of int is = " << sum(3, 5) << endl;      // calls the int version → 8
    cout << "Sum of double is = " << sum(3.5, 4.5) << endl; // calls the double version → 8
    return 0;
}
```

> The compiler automatically detects the data type of what you passed in, and picks the matching version of the function to call. You don't have to tell it explicitly which one to use.
> 

### A Real Gotcha: `float` vs. `double`

If you declare an overload using `float`, but then pass plain decimal values like `6.5`, you might hit an **ambiguous call** compiler error. Why?

> By default, C++ treats decimal literals (like `6.5`) as `double`, not `float`. So if you only wrote a `float` version of the function, the compiler gets confused about which one you meant.
> 

**Two ways to fix it:**

1. Change your overload to accept `double` instead of `float`, matching what the compiler assumes by default.
2. Explicitly suffix your literals with `f` (e.g., `6.5f`), which forces C++ to treat them as `float`.

> This is a great example of the "Float vs. Double" precision distinction from Lecture 2 coming back around in a very practical, debugging context.
> 

---

## 13. Pass by Value vs. Pass by Reference — Setting Up the Puzzle

This is where things get subtle, and the instructor deliberately leaves you with a "wait, what?" moment to build intuition.

### The Setup

```cpp
int modify(int num) {
    num = num + 10;
    return num;
}

int main() {
    int x = 10;
    cout << modify(x);   // prints 20
    cout << x;             // prints... 10, NOT 20!
    return 0;
}
```

### Why Doesn't `x` Become `20`?

Even though we "modified" the parameter inside `modify()`, the original `x` back in `main()` stays untouched at `10`. This happens because of **pass by value**:

> A copy of the variable is passed into the function. Changes made inside the function do not affect the original variable.
> 

```
main():  x = 10
             │
             │  (a COPY of x's value, 10, is handed to modify)
             ▼
modify(num):  num = 10 → num = num + 10 = 20 → return 20

Back in main(): x is STILL 10 — only the COPY (num) was changed
```

### The Fix — Pass by Reference

If we instead want the function to actually change the original variable, we use an `&` (reference) in the parameter:

```cpp
void change(int &x) {
    x = 20;
}
```

> Pass by reference passes a direct reference to the variable itself — so changes made inside the function *do* affect the original variable, unlike pass by value.
> 

We'll pick this up properly with full worked examples in Part 2, since the instructor's transcript cuts off right at this cliffhanger.

---

## Key Points to Remember

- A function is like a machine: it takes **input** (parameters), does some processing (**function body**), and hands back **output** (return statement).
- Every function declaration needs three things: **return type**, **function name**, and **parameters**.
- **Declaration** = telling the compiler the function's signature. **Definition** = the actual code. **Call** = running it.
- **Parameters** are what the function *accepts*; **arguments** are what you *pass in* at the call site — a classic interview distinction.
- Functions exist to improve **code reusability**, reduce repetition, ease **debugging**, and keep code **modular**.
- There are 4 combinations of user-defined functions: with/without a return type, crossed with with/without parameters.
- **`void`** is used as the return type whenever a function doesn't hand back any value.
- **Default parameters** let a function fall back to a preset value if the caller doesn't supply an argument.
- **Function overloading** lets multiple functions share the same name, distinguished only by the **data types of their parameters** — the compiler picks the right one automatically based on what you pass in.
- **Pass by value** (the default in C++) only works on a *copy* of a variable — changes inside the function don't affect the original. **Pass by reference** (using `&`) lets the function modify the original variable directly.

---

# 🎯Part 2 — Pass by Reference, Nested Functions & Call Stack

## 1. Finishing Pass by Value vs. Pass by Reference

### Quick Recap from Part 1

We saw that **pass by value** only hands a function a *copy* of a variable — so any changes made inside the function never reach the original variable back in `main`.

```cpp
int modify(int num) {
    num = num + 10;
    return num;
}

int main() {
    int x = 10;
    cout << modify(x);   // 20
    cout << x;             // still 10
    return 0;
}
```

### Pass by Reference — Fixing the Problem

If we actually want a function to change the original variable, we pass it **by reference**, using the `&` symbol in the parameter list:

```cpp
void change(int &x) {
    x = 20;
}

int main() {
    int num = 10;
    change(num);
    cout << num;   // prints 20 — the ORIGINAL variable changed!
    return 0;
}
```

> Pass by reference passes a direct reference to the variable itself, not a copy. This means changes made inside the function directly affect the original variable back in `main`.
> 

### Side-by-Side Comparison

|  | Pass by Value | Pass by Reference |
| --- | --- | --- |
| **What's passed** | A copy of the variable | A direct reference to the variable |
| **Changes inside function** | Do NOT affect the original | DO affect the original |
| **Syntax** | `void change(int x)` | `void change(int &x)` |
| **Default in C++?** | Yes | No — must explicitly add `&` |

```
PASS BY VALUE:
┌─────────────┐        copy of value          ┌──────────────┐
│  main: x=10 │ ──────────────────────────▶   │ func: num=10 │
└─────────────┘                               └──────────────┘
      │                                             │
      │ x stays 10                    num becomes 20 (but it's a
      │ (unaffected)                   separate copy — doesn't matter)
      ▼
   x = 10  (unchanged)

PASS BY REFERENCE:
┌─────────────┐     direct link to x          ┌──────────────┐
│  main: x=10 │ ◀═══════════════════════════  │ func: &x     │
└─────────────┘                               └──────────────┘
      │
      │ x = 20 written directly through the reference
      ▼
   x = 20  (changed!)
```

---

## 2. Two More Function Features (From the Instructor's Notes)

### Inline Functions

> An inline function is a hint to the compiler to insert the function's code directly at the call site, instead of doing a full function call. This improves speed, particularly for small functions.
> 

```cpp
inline int cube(int x) {
    return x * x * x;
}
```

Think of it this way: normally, calling a function involves some overhead (jumping to that function's code, then jumping back). For very small, simple functions, `inline` tells the compiler: "just paste this function's code directly wherever it's called," skipping that jump-and-return overhead entirely.

### Library Functions

These are **built-in functions** that come from C++'s standard header files — you don't write them yourself, just `#include` the right header and use them:

| Header | Functions |
| --- | --- |
| `<cmath>` | `sqrt()`, `pow()`, `ceil()` |
| `<cstring>` | `strlen()`, `strcpy()` |
| `<algorithm>` | `max()`, `min()`, `sort()` |

---

## 3. Nested Functions — What They Are (and Why C++ Doesn't Really Support Them)

### The Concept, Borrowed From Nested `if` and Nested Loops

We've already seen: an `if` inside an `if` is a **nested if**. A loop inside a loop is a **nested loop**. So naturally, a **function inside another function** would be a **nested function**.

> But — and this is important — exact nesting of functions (defining one function's complete body *inside* another function's body) is **not allowed in C++**. You can do this in Python, but not in C++.
> 

### The Workaround

> The solution: define the "inner" function separately, in the global scope, and simply *call* it from inside the "outer" function. The effect looks the same as true nesting, even though technically the functions are defined side-by-side, not literally inside one another.
> 

```
┌─────────────────────────────────────────────────┐
│  TRUE NESTING (**NOT allowed in C++**)          │
│  function outer() {                             │
│      function inner() {  ← defined INSIDE       │
│          ...                                    │
│      }                                          │
│  }                                              │
├─────────────────────────────────────────────────┤
│  C++'s WORKAROUND (**this is what we actually   │
│  do — simulated nesting**)                      │
│                                                 │
│  function inner() { ... }   ← defined           │
│                                separately,      │
│  function outer() {           at global scope   │
│      inner();   ← just CALLED from here         │
│  }                                              │
└─────────────────────────────────────────────────┘
```

> A function can call another function, or even call itself (recursion). This helps in modular programming and breaking problems into smaller tasks.
> 

---

## 4. Worked Example 1 — A Simple Nested Call

```cpp
#include <iostream>
using namespace std;

void greet() {
    cout << "Hello! ";
}

void welcome() {
    greet();   // "nested" call — greet() is called from inside welcome()
    cout << "Welcome to C++ Programming";
}

int main() {
    welcome();
    return 0;
}
```

**Output:** `Hello! Welcome to C++ Programming`

**Explanation:**

- `main()` calls `welcome()`
- `welcome()` calls `greet()` → which prints `"Hello!"`
- Then `welcome()` continues and prints `"Welcome to C++ Programming"`

---

## 5. Worked Example 2 — A Full Chain: Square of the Sum

Let's build something meatier: **calculate the square of the sum of two numbers**, using multiple nested function calls chained together.

### The Logic Chain

```
main()
  → calls calculateSquareOfSum(a, b)
       → calls add(a, b)  →  computes sum = a + b
            → calls square(sum)
                 → calls printSquare(sum)  →  prints sum * sum
```

### The Code

```cpp
#include <iostream>
using namespace std;

void printSquare(int num) {
    cout << num * num;
}

void square(int num) {
    printSquare(num);
}

void add(int a, int b) {
    int sum = a + b;
    square(sum);
}

void calculateSquareOfSum(int a, int b) {
    add(a, b);
}

int main() {
    int num1 = 3, num2 = 7;
    calculateSquareOfSum(num1, num2);
    return 0;
}
```

### Tracing Through It

```
num1 = 3, num2 = 7
   ↓
calculateSquareOfSum(3, 7) called
   ↓
add(3, 7) called → sum = 3 + 7 = 10
   ↓
square(10) called
   ↓
printSquare(10) called → prints 10 * 10 = 100
```

**Output:** `100`

> Notice the whole chain is built from small, single-purpose functions — `add` only adds, `square` only forwards the sum along, and `printSquare` only prints the final squared value. This is exactly the "breaking complex problems into smaller parts" benefit of functions from Part 1.
> 

---

## 6. The Modular Version (Matching the Instructor's PDF Notes)

The PDF frames this same idea slightly differently — using **return values** instead of `void` functions that just call the next one:

```cpp
int add(int a, int b) {
    return a + b;
}

int multiply(int a, int b) {
    return a * b;
}

int calculate(int x, int y) {
    int sum = add(x, y);
    int prod = multiply(x, y);
    return sum + prod;
}

int main() {
    cout << calculate(2, 3);   // Output: 11
    return 0;
}
```

**Explanation:**

- `calculate()` calls both `add()` and `multiply()` internally
- `sum = add(2, 3) = 5`, `prod = multiply(2, 3) = 6`
- `calculate` returns `sum + prod = 5 + 6 = 11`

This demonstrates **nested function calls used for modular design** — `calculate` doesn't know *how* addition or multiplication work internally; it just trusts `add` and `multiply` to do their jobs and combines the results.

---

## 7. Nested Recursion — A Function Calling a Helper That Calls Itself

```cpp
int factorialHelper(int n) {
    if (n == 0) return 1;
    return n * factorialHelper(n - 1);
}

int factorial(int n) {
    return factorialHelper(n);   // "nested" call
}

int main() {
    cout << factorial(5);   // Output: 120
    return 0;
}
```

Here, `factorial()` doesn't do the actual work itself — it delegates to `factorialHelper()`, which calls **itself** repeatedly (this is called **recursion** — a function calling itself, which we'll study properly in a dedicated lecture).

> A function can call another function that calls the first function (or itself) — this is the essence of nested recursion.
> 

---

## 8. The Function Call Stack

### The Book-Stacking Analogy

> Have you ever heard of a stack? When you keep placing things one on top of another in a pile, that's a stack. Like books — I place one book, then another on top, then another on top of that.
> 

When functions call other functions (like our `calculateSquareOfSum` → `add` → `square` → `printSquare` chain), C++ builds up a **call stack** in memory — tracking exactly which function is "on top" (currently running) and which ones are waiting underneath for their turn to finish.

### Visualizing the Call Stack for Our Square-of-Sum Example

```
┌────────────────────────────────┐  ← called LAST, finishes FIRST
│   printSquare(sum)             │
├────────────────────────────────┤
│   square(sum)                  │
├────────────────────────────────┤
│   add(a, b)                    │
├────────────────────────────────┤
│   calculateSquareOfSum(a, b)   │
├────────────────────────────────┤
│   main()                       │  ← called FIRST, finishes LAST
└────────────────────────────────┘
```

### How It Unwinds

```
main() calls calculateSquareOfSum
   → calculateSquareOfSum calls add
        → add calls square
             → square calls printSquare
                  → printSquare prints the result and finishes
                       ← control returns back to square
                  ← square finishes, returns back to add
             ← add finishes, returns back to calculateSquareOfSum
        ← calculateSquareOfSum finishes, returns back to main
   ← main returns 0
```

> Each function call gets stacked on top of the previous one. Once a function finishes and returns its result, it gets removed ("popped") from the stack, and control goes back to whichever function called it. This continues until everything unwinds back down to `main`, which finally returns `0`.
> 

---

## 9. Practice Program: Checking for a Perfect Number

### What Is a Perfect Number?

> A perfect number is a number that is equal to the sum of its proper divisors (excluding the number itself).
> 

### Worked Example — Is 6 a Perfect Number?

The divisors of `6` are `1, 2, 3,` and `6` itself. **Excluding 6 itself**, we add up the remaining divisors:

```
1 + 2 + 3 = 6
```

Since this sum **equals** the original number (`6`), **6 is a perfect number**.

### The Logic — Reusing the Prime-Number-Style Loop

This uses the exact same divisor-finding approach you'd use to check for prime numbers: loop from `1` up to `n/2`, and check which numbers divide `n` evenly (remainder `0`).

```
For each i from 1 to n/2:
    if n % i == 0:
        add i to a running sum
After the loop:
    if sum == n → it's a perfect number
    else → it's not
```

### The Code

```cpp
#include <iostream>
using namespace std;

bool isPerfect(int n) {
    int sum = 0;
    for (int i = 1; i <= n / 2; i++) {
        if (n % i == 0) {
            sum += i;
        }
    }
    return sum == n;
}

int main() {
    int num = 28;

    if (isPerfect(num)) {
        cout << "The number is a perfect number";
    } else {
        cout << "The number is not a perfect number";
    }

    return 0;
}
```

### Tracing Through It — `n = 28`

Divisors of 28 (excluding itself): `1, 2, 4, 7, 14`

```
1 + 2 + 4 + 7 + 14 = 28
```

Since `sum == n` (`28 == 28`), `isPerfect(28)` returns `true`.

**Output:** `The number is a perfect number`

### Sample Run — `n = 56`

Divisors of 56 (excluding itself): `1, 2, 4, 7, 8, 14, 28`

```
1 + 2 + 4 + 7 + 8 + 14 + 28 = 64
```

Since `64 ≠ 56`, `isPerfect(56)` returns `false`.

**Output:** `The number is not a perfect number`

> Notice the function's return type here is `bool` — since `isPerfect` only ever needs to answer a yes/no question, returning `sum == n` directly (which is itself a `true`/`false` expression, exactly like we learned with relational operators back in Lecture 4) is cleaner than writing a full `if-else` inside the function.
> 

---

## Key Points to Remember

- **Pass by reference** (`int &x`) lets a function modify the caller's original variable directly — unlike pass by value, which only ever works on a copy.
- **Inline functions** (`inline`) hint to the compiler to paste small functions' code directly at the call site, avoiding call overhead — useful for tiny, frequently-used functions.
- **Library functions** come from standard headers like `<cmath>`, `<cstring>`, and `<algorithm>` — no need to write them yourself.
- C++ does **not** support true nested function definitions (unlike Python) — but you can simulate nesting by defining functions separately and simply *calling* one from inside another.
- Nested function calls are the backbone of **modular design**: breaking one big task (like "square of a sum") into small, single-purpose functions that call each other in sequence.
- **Recursion** — a function calling itself — is a special, important case of a function "nesting" a call to itself; we'll cover this properly in its own dedicated lecture.
- The **call stack** tracks function calls like a stack of books: each new call sits "on top," and functions are removed from the stack (in reverse order) as they finish and return control back to their caller.
- The **Perfect Number** problem reuses the same divisor-finding loop structure from prime number checking — proving that a lot of DSA logic is about recognizing and reusing familiar patterns, not memorizing brand-new tricks for every problem.

---