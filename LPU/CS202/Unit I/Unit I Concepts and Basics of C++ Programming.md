# Unit I:
Concepts and Basics of C++ Programming

*Notes for 2nd Year BTech Students (CS202 – DSA)*

---

## 1. Programming Paradigms & Why C++

**What is a programming paradigm?**
It's just a *style* or *approach* to writing code — a way of organizing your instructions.

**Procedural Programming (old style)**

- Code is a sequence of steps/instructions.
- Program is broken into **functions**.
- Data and functions are **separate**.
- Example languages: C

**Object-Oriented Programming (OOP)**

- Code is organized around **objects** (real-world entities).
- Data and the functions that work on that data are **bundled together** inside a class.
- Focus is on "what things are" (objects) rather than "what steps to do."

```
Procedural Style                    OOP Style
─────────────────                   ─────────
 Data  (separate)                    ┌──────────────┐
   |                                 │   Object     │
 Function1 → Function2               │  ┌────────┐  │
   |            |                    │  │ Data   │  │
 acts on data  acts on data          │  ├────────┤  │
                                     │  │Function│  │
                                     │  └────────┘  │
                                     └──────────────┘
```

**Why C++?**

- C++ = C + OOP features (classes, objects, inheritance, etc.)
- Gives you low-level control (like C) *and* high-level structure (like OOP).
- Faster execution, used in system software, game engines, competitive programming, and — most importantly for you — it's great for implementing **data structures** efficiently because of manual memory control (pointers!).

---

## 2. Structure of a C++ Program, Tokens, Keywords, Identifiers

**Basic Structure of a C++ Program**

```cpp
#include <iostream>      // Header file section
using namespace std;     // Namespace declaration

int main() {              // main() function - execution starts here
    cout << "Hello!";     // Statement
    return 0;              // Return statement
}
```

| Part | Meaning |
| --- | --- |
| `#include <iostream>` | Tells compiler to include input-output library |
| `using namespace std;` | Lets us use `cout`, `cin` without writing `std::cout` |
| `int main()` | Every C++ program **must** have this — execution starts here |
| `{ }` | Curly braces mark the start and end of a block of code |
| `return 0;` | Tells the OS the program ended successfully |

**Tokens**
The smallest individual units in a program. Every line of code is made of tokens.

```
int age = 20;

Tokens: int | age | = | 20 | ;
```

Types of tokens:

- **Keywords** – reserved words (`int`, `if`, `for`, `return`, `class`...)
- **Identifiers** – names given by programmer (variable names, function names)
- **Constants** – fixed values (`10`, `"hello"`, `3.14`)
- **Operators** – symbols like `+`, , `=`, `==`
- **Punctuators** – `;`, `{ }`, `( )`, `,`

**Keywords**
Reserved words that already have a fixed meaning in C++. You **cannot** use them as variable names.
Examples: `int`, `float`, `char`, `if`, `else`, `while`, `for`, `return`, `class`, `void`, `new`, `delete`

**Identifiers**
Names that *you* create for variables, functions, classes, etc.

Rules for valid identifiers:

- Can contain letters, digits, underscore (`_`)
- Must **not** start with a digit
- Cannot be a keyword
- Case-sensitive (`Age` and `age` are different)

```
Valid:    age, _count, student1, totalMarks
Invalid:  1student, float, total-marks
```

---

## 3. Data Types, Variables, Type Modifiers, Constants

**Data Types** — tell the compiler what *kind* of data a variable will hold, and how much memory to reserve.

| Data Type | Size (typical) | Example |
| --- | --- | --- |
| `int` | 4 bytes | 10, -5, 2000 |
| `float` | 4 bytes | 3.14, -0.5 |
| `double` | 8 bytes | 3.14159265 |
| `char` | 1 byte | 'A', 'x' |
| `bool` | 1 byte | true, false |
| `void` | — | represents "no value" |

**Variables**
A named location in memory used to store a value that can change.

```cpp
int age = 20;
```

```
Memory:
 ┌────────────┐
 │    20      │  ← value
 └────────────┘
   address: 0x7ffd...
   name: age (this is just a label we use to refer to that address)
```

This "name refers to a memory address" idea is **very important** — it's the foundation you'll need when we study pointers in Unit II.

**Type Modifiers**
Keywords that adjust the size/range/behavior of a basic data type.

| Modifier | Example | Effect |
| --- | --- | --- |
| `signed` | `signed int` | can store negative & positive (default) |
| `unsigned` | `unsigned int` | only positive values, larger positive range |
| `short` | `short int` | smaller memory, smaller range |
| `long` | `long int` | larger memory, larger range |

**Constants**
Values that **cannot** change once assigned.

```cpp
const float PI = 3.14;   // using 'const' keyword
#define MAX 100            // using preprocessor directive
```

---

## 4. Operators & Expressions

An **operator** performs an operation on one or more values (called **operands**). An **expression** is a combination of operators and operands that evaluates to a value.

**Arithmetic Operators**

| Operator | Meaning | Example |
| --- | --- | --- |
| `+` | Addition | `5 + 3 = 8` |
| `-` | Subtraction | `5 - 3 = 2` |
| `*` | Multiplication | `5 * 3 = 15` |
| `/` | Division | `5 / 2 = 2` (integer division) |
| `%` | Modulus (remainder) | `5 % 2 = 1` |

**Relational Operators** (compare two values → result is `true`/`false`)

| Operator | Meaning |
| --- | --- |
| `==` | equal to |
| `!=` | not equal to |
| `>` | greater than |
| `<` | less than |
| `>=` | greater than or equal |
| `<=` | less than or equal |

**Logical Operators** (combine boolean conditions)

| Operator | Meaning | Example |
| --- | --- | --- |
| `&&` | AND (both true) | `(a>5 && b<10)` |
| `||` | OR (either true) | `(a>5 || b<10)` |
| `!` | NOT (reverses value) | `!(a>5)` |

**Bitwise Operators** (work on individual bits — used a lot in DSA optimization)

| Operator | Meaning |
| --- | --- |
| `&` | AND |
| `|` | OR |
| `^` | XOR |
| `~` | NOT (complement) |
| `<<` | left shift |
| `>>` | right shift |

**Assignment Operators**

```cpp
a = 5;      // simple assignment
a += 5;     // a = a + 5
a -= 5;     // a = a - 5
a *= 5;     // a = a * 5
```

**Ternary Operator** (short-hand for if-else)

```cpp
int max = (a > b) ? a : b;
// meaning: if(a > b) max = a; else max = b;
```

---

## 5. Input/Output using cin/cout, Manipulators

**cout** – used to display output (Console OUTput)
**cin** – used to take input (Console INput)

```cpp
int age;
cout << "Enter your age: ";   // display message
cin >> age;                    // take input, store in 'age'
cout << "Your age is: " << age;
```

```
cin  >>  →  arrow points TOWARD the variable (data goes IN)
cout <<  →  arrow points AWAY from cout (data goes OUT)
```

**Manipulators** – special functions used to format output.

| Manipulator | Use |
| --- | --- |
| `endl` | inserts a new line |
| `setw(n)` | sets field width to `n` |
| `setprecision(n)` | sets decimal precision |
| `fixed` | forces fixed-point notation |

```cpp
#include <iomanip>
cout << fixed << setprecision(2) << 3.14159; // Output: 3.14
```

---

## 6. Control Structures – Decision Making

Used when the program needs to make a **choice**.

**if statement**

```cpp
if (age >= 18) {
    cout << "Eligible to vote";
}
```

**if-else statement**

```cpp
if (age >= 18) {
    cout << "Eligible";
} else {
    cout << "Not eligible";
}
```

**if-else if ladder**

```cpp
if (marks >= 90) grade = 'A';
else if (marks >= 75) grade = 'B';
else if (marks >= 50) grade = 'C';
else grade = 'F';
```

**switch statement** — used when comparing one variable against many fixed values.

```cpp
switch (day) {
    case 1: cout << "Monday"; break;
    case 2: cout << "Tuesday"; break;
    default: cout << "Invalid day";
}
```

```
Flow of switch:
   day
    │
    ▼
 ┌───────┐   yes     ┌─────────────┐
 │day==1? ├─────────►│print Monday │──► break (exit switch)
 └───┬───┘           └─────────────┘
     │ no
     ▼
 ┌───────┐   yes     ┌─────────────┐
 │day==2?├─────────► │print Tuesday│──► break
 └───┬───┘           └─────────────┘
     │ no
     ▼
  default case runs
```

⚠️ **Important:** Don't forget `break;` — without it, execution "falls through" into the next case.

---

## 7. Control Structures – Loops

Used to repeat a block of code multiple times.

**for loop** — best when you know how many times to repeat.

```cpp
for (int i = 1; i <= 5; i++) {
    cout << i << " ";
}
// Output: 1 2 3 4 5
```

```
   ┌─────────────┐
   │ i = 1       │ (initialization - runs once)
   └──────┬──────┘
          ▼
   ┌─────────────┐   false
   │  i <= 5 ?   ├────────► exit loop
   └──────┬──────┘
           │ true
           ▼
   ┌─────────────┐
   │ print i     │
   └──────┬──────┘
          ▼
   ┌─────────────┐
   │   i++       │
   └──────┬──────┘
          └────────► back to condition check
```

**while loop** — best when repetition depends on a condition, and number of repeats isn't fixed in advance. Condition checked **before** the loop body runs.

```cpp
int i = 1;
while (i <= 5) {
    cout << i << " ";
    i++;
}
```

**do-while loop** — same as while, but condition checked **after** the loop body. Guarantees the body runs **at least once**.

```cpp
int i = 1;
do {
    cout << i << " ";
    i++;
} while (i <= 5);
```

| Loop | Checks condition | Minimum runs |
| --- | --- | --- |
| `for` | before | 0 |
| `while` | before | 0 |
| `do-while` | after | 1 |

**break** – immediately exits the loop.
**continue** – skips the current iteration and moves to the next one.

```cpp
for (int i = 1; i <= 5; i++) {
    if (i == 3) continue;   // skips printing 3
    if (i == 5) break;       // stops loop entirely at 5
    cout << i << " ";
}
// Output: 1 2 4
```

---

## 8. Functions

A function is a named block of code that performs a specific task, which you can call (use) whenever needed — avoiding repetition of code.

**Declaration (Prototype), Definition, and Call**

```cpp
// Declaration (tells compiler function exists)
int add(int a, int b);

int main() {
    int result = add(5, 3);   // Function Call
    cout << result;
}

// Definition (actual code of the function)
int add(int a, int b) {
    return a + b;
}
```

```
                   ┌────────────────────┐
   main() calls    │      add(5, 3)     │
  ───────────────► │ a=5, b=3           │
                   │ return a+b = 8     │
   result = 8     ◄┤                    │
                   └────────────────────┘
```

**Parameter Passing (by value)**
When you pass a variable to a function by value, a **copy** of it is made. Changes inside the function do **not** affect the original variable.

```cpp
void change(int x) {
    x = 100;   // only changes the local copy
}

int main() {
    int num = 5;
    change(num);
    cout << num;  // still prints 5
}
```

*(We'll see how "call by reference" and pointers can change this behavior in Unit II.)*

**Default Arguments**
A function can have default values for parameters, used if the caller doesn't provide them.

```cpp
void greet(string name = "Student") {
    cout << "Hello, " << name;
}

greet();          // Output: Hello, Student
greet("Aman");    // Output: Hello, Aman
```

**Function Overloading**
Multiple functions with the **same name** but **different parameters** (number or type).

```cpp
int add(int a, int b) { return a + b; }
double add(double a, double b) { return a + b; }
int add(int a, int b, int c) { return a + b + c; }
```

The compiler decides which version to call based on the arguments you pass.

**Inline Functions**
A request to the compiler to insert the function's code directly at the point of call, instead of doing a regular function call — used for very small functions to save the overhead of calling.

```cpp
inline int square(int x) {
    return x * x;
}
```

---

## 9. Storage Classes

Storage classes tell us about the **scope**, **lifetime**, and **default value** of a variable.

| Storage Class | Scope | Lifetime | Default Value | Notes |
| --- | --- | --- | --- | --- |
| `auto` | local | within block | garbage | default for local variables (rarely typed explicitly) |
| `static` | local/global | entire program | 0 | retains value between function calls |
| `extern` | global | entire program | 0 | declares a variable defined in another file |
| `register` | local | within block | garbage | suggests storing variable in CPU register for fast access (mostly ignored by modern compilers) |

**Example — static variable retaining value:**

```cpp
void counter() {
    static int count = 0;   // initialized only ONCE
    count++;
    cout << count << " ";
}

int main() {
    counter(); // prints 1
    counter(); // prints 2
    counter(); // prints 3
}
```

```
Normal local variable:            Static local variable:
each call → fresh copy            each call → SAME copy is reused
call 1: count=0→1                 call 1: count=0→1
call 2: count=0→1 (resets!)       call 2: count=1→2 (remembers!)
```

---

## Unit I (Expanded) – Recursion

### What is Recursion?

A recursive function is a function that **calls itself** inside its own definition, to break a big problem down into a smaller version of the *same* problem — until the problem becomes small enough to solve directly.

**Real-life analogy:** Imagine you're in a movie theatre and want to know which row you're sitting in, but the seats aren't numbered. You ask the person in front: "What row are you in?" That person doesn't know either, so they ask the person in front of them — and so on, until it reaches the very first row (Row 1). That person answers "1", and the answer travels back, each person adding 1 as it passes back to you.

This is recursion: **pass the problem forward until it's trivial, then build the answer on the way back.**

---

### The Two Non-Negotiable Parts

Every recursive function **must** have:

1. **Base Case** — the simplest version of the problem, solved directly (no further recursive call). This is what *stops* the recursion.
2. **Recursive Case** — calls the function again with an input that is *closer* to the base case.

```
Recursive Function
      │
      ├── Base Case         → stop, return a direct answer
      │
      └── Recursive Case    → call self with smaller input
```

If you forget the base case, or the recursive case never actually approaches it, the function calls itself forever → **stack overflow** (program crashes).

---

### Example 1: Sum of First N Natural Numbers (simplest possible example)

**Problem:** sum(4) = 4 + 3 + 2 + 1

```cpp
int sum(int n) {
    if (n == 0)              // Base case
        return 0;
    return n + sum(n - 1);   // Recursive case
}
```

**Step-by-step expansion:**

```
sum(4)
 = 4 + sum(3)
         = 3 + sum(2)
                 = 2 + sum(1)
                         = 1 + sum(0)
                                 = 0      ← base case hit
                         = 1 + 0 = 1
                 = 2 + 1 = 3
         = 3 + 3 = 6
 = 4 + 6 = 10
```

Notice the pattern: the function keeps calling itself **going down** (this is called the "winding" phase), until it hits the base case. Then it starts **coming back up** with actual values (this is called the "unwinding" phase).

```
WINDING (going in)         UNWINDING (coming out)
sum(4) ──┐                          ┌── 10 (4+6)
sum(3) ──┤                          ├── 6  (3+3)
sum(2) ──┤   waiting...             ├── 3  (2+1)
sum(1) ──┤                          ├── 1  (1+0)
sum(0) ──┘  ← base case, returns 0 ─┘
```

---

### Example 2: Factorial (multiplication instead of addition)

```cpp
int factorial(int n) {
    if (n == 0)                     // Base case
        return 1;
    return n * factorial(n - 1);    // Recursive case
}
```

```
factorial(4)
 = 4 * factorial(3)
         = 3 * factorial(2)
                 = 2 * factorial(1)
                         = 1 * factorial(0)
                                 = 1   ← base case
                         = 1 * 1 = 1
                 = 2 * 1 = 2
         = 3 * 2 = 6
 = 4 * 6 = 24
```

**Compare with Example 1** — structurally identical, just `+` swapped for `*`, and base case returns `1` instead of `0` (because multiplying by 0 would wipe out the answer). Pointing this out to students helps them see the *pattern*, not just memorize each example separately.

---

### Example 3: Print Numbers from N to 1 (recursion with no return value)

```cpp
void printDesc(int n) {
    if (n == 0)          // Base case
        return;            // just stop, nothing to return
    cout << n << " ";
    printDesc(n - 1);      // Recursive case
}
```

Call: `printDesc(5)` → Output: `5 4 3 2 1`

Here, the printing happens **before** the recursive call — so numbers print on the way **down** (winding phase).

---

### Example 4: Print Numbers from 1 to N (same problem, reversed order!)

```cpp
void printAsc(int n) {
    if (n == 0)             // Base case
        return;
    printAsc(n - 1);        // Recursive case FIRST
    cout << n << " ";       // print AFTER the call
}
```

Call: `printAsc(5)` → Output: `1 2 3 4 5`

**This is the most important comparison for students to see.** The *only* difference between Example 3 and Example 4 is the **order** of the print statement relative to the recursive call:

```
printDesc(3)                      printAsc(3)
─────────────                     ────────────
print 3                           printAsc(2)
  printDesc(2)                       printAsc(1)
    print 2                             printAsc(0) → base case, return
      printDesc(1)                    print 1
        print 1                    print 2
          printDesc(0) → return  print 3
```

```
printDesc: print happens on the way DOWN  → 3 2 1
printAsc:  print happens on the way UP    → 1 2 3
```

This single example usually makes the "winding vs unwinding" idea click for students better than any explanation alone.

---

### Example 5: Fibonacci (recursion that branches into two calls)

```cpp
int fib(int n) {
    if (n == 0 || n == 1)          // Base case
        return n;
    return fib(n - 1) + fib(n - 2);  // TWO recursive calls
}
```

Unlike previous examples (one call per step), Fibonacci makes **two** recursive calls — this creates a tree, not a straight line:

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

Reading the tree bottom-up: `fib(2) = fib(1)+fib(0) = 1+0 = 1`, `fib(3) = fib(2)+fib(1) = 1+1 = 2`, `fib(4) = fib(3)+fib(2) = 2+1 = 3`.

**Important observation for students:** `fib(2)` is computed **three separate times** in this tree, and `fib(1)` even more. This repeated work is why naive recursive Fibonacci becomes very slow for large `n` — a nice preview for when we study Dynamic Programming later in the course.

---

### The Call Stack — What's Actually Happening in Memory

Every time a function calls itself, the computer **pauses** the current call and pushes a new "frame" onto the call stack, keeping track of where to resume once the inner call returns.

```
Call Stack while computing factorial(4):

Step 1         Step 2      Step 3       Step 4        Step 5 (base case)
┌─────────┐                                             ┌──────────┐
│fact(4)  │   ┌───────┐                                 │fact(4)   │
└─────────┘   │fact(3)│  ┌────────┐                     │fact(3)   │
              │fact(4)│  │fact(2) │   ┌─────────┐       │fact(2)   │
              └───────┘  │fact(3) │   │fact(1)  │       │fact(1)   │
                         │fact(4) │   │fact(2)  │       │fact(0)   │ ← base, returns 1
                         └────────┘   │fact(3)  │       │fact(1)   │
                                      │fact(4)  │       │fact(2)   │
                                      └─────────┘       │fact(3)   │
                                                        │fact(4)   │
                                                        └──────────┘
```

Once `factorial(0)` returns `1`, the stack starts **popping** frames off one by one, each multiplying its `n` with the value returned from below — until `factorial(4)` finally returns `24`.

This is exactly why an infinite recursive call (missing/wrong base case) causes a **stack overflow** — the stack keeps growing with new frames and never gets to pop any off.

---

### Recursion vs Iteration — When to Use Which

|  | Recursion | Iteration (loops) |
| --- | --- | --- |
| Mechanism | function calls itself | `for` / `while` |
| Memory | uses call stack — more memory | uses less memory |
| Speed | slightly slower (function call overhead) | usually faster |
| Best for | problems that are naturally recursive: trees, graphs, divide & conquer, backtracking | simple repetition, counting, summing |
| Risk | stack overflow if base case is wrong | no such risk |

Every recursive solution *can* be rewritten as an iterative one (and vice versa) — but some problems (like tree traversal, which we'll hit later in the course) are so naturally recursive that writing them iteratively becomes awkward and harder to read.

---

### Common Beginner Mistakes (worth emphasizing to students)

1. **Missing base case entirely** → infinite recursion.
    
    ```cpp
    int badSum(int n) {
        return n + badSum(n - 1);  // never stops!
    }
    ```
    
2. **Base case present, but recursive call doesn't move toward it.**
    
    ```cpp
    int badFactorial(int n) {
        if (n == 0) return 1;
        return n * badFactorial(n);   // should be (n - 1), not (n)!
    }
    ```
    
3. **Wrong base case value** (e.g., returning 0 instead of 1 in factorial) — silently gives wrong answers instead of crashing, which is often harder to debug.

---