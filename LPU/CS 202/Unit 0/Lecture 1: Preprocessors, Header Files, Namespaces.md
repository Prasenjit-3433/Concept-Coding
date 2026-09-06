# Lecture 1: Preprocessors, Header Files, Namespaces & Tokens

Status: Done

## 1. The Basic Structure of a C++ Program

Every C++ program you write will start with the same three lines. Let's look at the simplest possible program:

```cpp
#include <iostream>     // Preprocessor directive
using namespace std;    // Namespace declaration

int main() {
    cout << "Welcome to LearnYard";
    return 0;
}
```

This program has **5 building blocks**, and we're going to understand each one, piece by piece:

1. Preprocessor directive
2. Header file inclusion
3. Namespace declaration
4. The `main()` function
5. Statements and blocks

---

## 2. Preprocessors

### What is a preprocessor?

The word itself tells you the meaning — **pre + processor** = something that runs *before* your program is compiled.

> A preprocessor directive is an instruction that gets executed **before compilation** begins — before your actual C++ code is turned into a running program.
> 

Any line starting with `#` is a preprocessor directive.

```cpp
#include <iostream>   // this whole line is a preprocessor directive
#define PI 3.14       // this is also a preprocessor directive
```

### The two preprocessors you must know

**a) `#include`**
Used to bring in a header file into your program.

**b) `#define`**
Used to create a constant value that cannot be changed later.

Example — say you're building a calculator and want the value of π fixed throughout your program:

```cpp
#define PI 3.14
```

Now, wherever `PI` is used in your code, the preprocessor replaces it with `3.14` *before* compilation happens. And since it was defined this way, its value **cannot be changed** later in the program.

---

## 3. Header Files

### Why do we need header files?

Imagine every time you wanted to print something on the screen, you had to write the entire logic for "how to display text" from scratch. That would be exhausting.

So developers have already written that logic for you and stored it inside a file — this file is called a **header file**.

> A header file is a file containing pre-written code (mostly function declarations) so that you don't have to write the same logic again and again — you just include the file and use the functions directly.
> 

When you write:

```cpp
#include <iostream>
```

You're telling the compiler: "Bring in the code from the `iostream` file, because I want to use the functions written inside it (like `cin` and `cout`)."

### Where does the name `iostream` come from?

`iostream` = **i**nput **stream** + **o**utput **stream**

Think of a "stream" like a flow of water — water flows from one point to another, carrying something along with it.

| Stream | Direction | Example |
| --- | --- | --- |
| **istream** (input stream) | Data flows *into* your program | You type your marks on the keyboard → they travel into memory |
| **ostream** (output stream) | Data flows *out of* your program | Your program shows a result → it travels to your screen |

So:

- Whenever you need to **take input** from the user → you need `istream` (used through the `cin` function)
- Whenever you need to **produce output** → you need `ostream` (used through the `cout` function)

Both of these live inside the `iostream` header file — which is why we include it.

### Commonly used header files

| Header File | Used For |
| --- | --- |
| `<iostream>` | Input/output — gives you `cin`, `cout`, `cerr` |
| `<cmath>` | Mathematical functions, e.g. `sqrt(9)` |
| `<string>` | Working with strings, e.g. `"I am Sachin"` |
| `<algorithm>` | Built-in algorithm functions |

**User-defined header files:** You can also write your own header file, put your own functions inside it, and include it in your program:

```cpp
#include "myheader.h"
```

### Two rules to remember

1. Header file names are always written inside **angular brackets**: `<iostream>`
2. `cout` and `COUT` are **not** the same — C++ is a **case-sensitive** language. Be careful with capitalization.

---

## 4. Namespaces

### The problem namespaces solve

Here's an everyday example to understand this: imagine twin siblings in a house, and their mother buys them identical clothes. Without labels, mixing up whose shirt is whose is inevitable — "You wore my shirt!" To avoid this, the mother creates two separate boxes, one labeled "Rahul" and one labeled "Sachin," and keeps each person's clothes in their own box.

**Namespaces work exactly like these boxes.**

### How this applies to C++

Imagine two different companies — Company X and Company Y — both create their own header files, and by coincidence, **both** have a function called `cout`. But:

- Company X's `cout` prints output.
- Company Y's `cout` performs addition.

If you include both header files in your program:

```cpp
#include "X"
#include "Y"
```

...and you write `cout`, the compiler gets confused. Which `cout` do you mean? This confusion is called a **naming conflict**.

### The solution: Namespace

To avoid this conflict, companies wrap their functions inside a **namespace** — basically their own labeled "box." So now, to specify which `cout` you want, you write the namespace name first:

```cpp
X::cout << "Sachin";   // use X's cout
Y::cout << "Sachin";   // use Y's cout
```

### Where does `std` come in?

The company that builds C++'s most commonly used functions (`cin`, `cout`, `cerr`, `endl`, etc.) is called the **Standard Template Library (STL)**. STL created its own namespace and named it `standard`, which is shortened to **`std`**.

So technically, every time you use `cout`, you should write:

```cpp
std::cout << "Hello";
```

This tells the compiler: "I want the `cout` function from the `std` namespace."

### Why we write `using namespace std;`

Writing `std::` before every single `cout` and `cin` gets repetitive. So instead, we declare it **once**, at the top of the program:

```cpp
using namespace std;
```

Now you can simply write:

```cpp
cout << "Hello";
```

...instead of:

```cpp
std::cout << "Hello";
```

Both do the exact same thing — one is just shorter to write.

**Functions that live inside the `std` namespace:** `cout`, `cin`, `endl`, and more — all part of the Standard Template Library (STL), which you'll study in full detail later in this course.

---

## 5. The `main()` Function

### Why is `main()` special?

Everything written between the curly braces `{ }` of `main()` is the code that actually gets **executed** when your program runs.

> The compiler looks for the `main()` function inside your program and runs whatever code is written inside it. That's why it's called the entry point / core of your C++ program.
> 

### Functions are like machines

Think of a function as a machine: you feed it an **input**, it processes it, and gives you an **output**.

```
        INPUT              MACHINE (function)             OUTPUT
    ┌───────────┐      ┌────────────────────┐      ┌───────────┐
    │  2, 3     │ ───▶ │   add(a, b)        │ ───▶ │    5      │
    └───────────┘      └────────────────────┘      └───────────┘
```

Just like the `add()` function returns a value (5), the `main()` function also returns a value — and that value is `0`.

### Why does `main()` return 0?

`return 0;` is how your program tells the operating system: **"I ran successfully, with no errors."**

| Return Value | Meaning |
| --- | --- |
| `0` | Program executed successfully |
| Any non-zero value (commonly `-1`) | Program did **not** execute successfully |

**Good to know:** Modern C++ compilers are smart enough to automatically add `return 0;` at the end even if you forget it. But it's still considered **good practice** to write it explicitly.

---

## 6. Comments in C++

### Why do we need comments?

Imagine you're working on a huge codebase with thousands of lines, and 10 different engineers are editing it. If someone changes one line of code, others need a way to know **what changed and why** — without that explanation actually being treated as executable code.

> A comment is a part of your program that the compiler completely ignores. It exists only to help programmers understand the code — it has zero effect on how the program runs.
> 

### Two types of comments

**a) Single-line comment** — for short, one-line notes:

```cpp
// This is a single-line comment
cout << "Sachin";
```

Everything after `//` on that line is treated as a comment. Note: this only works for a **single line** — if you write a comment and then continue code on the next line without `//`, that next line will be treated as actual code (and may throw an error).

**b) Multi-line comment** — for longer explanations spanning several lines:

```cpp
/* This is a multi-line comment.
   You can write across
   as many lines as you like. */
```

Everything between `/*` and `*/` is treated as a comment, no matter how many lines it spans.

---

## 7. C++ Input/Output Streams

Inside the `iostream` header file, there are 3 (mainly used) functions:

| Function | Purpose |
| --- | --- |
| `cin` | Takes **input** from the user |
| `cout` | Displays **output** to the screen |
| `cerr` | Displays **error messages** |

*(There's a 4th one, `clog`, but it's rarely used.)*

```cpp
int x = 10;
std::cout << x;      // outputs the value of x
std::cin >> y;        // takes input and stores it in y
std::cerr << "Error!"; // outputs an error message
```

> Notice: since `using namespace std;` wasn't written above, we had to specify `std::` before each function — that's the trade-off of skipping the namespace declaration.
> 

---

## 8. Printing on the Same Line vs. Next Line

By default, C++ does **not** automatically move to a new line between two `cout` statements.

```cpp
cout << "Hello World";
cout << "Programming is fun";
```

**Output:**

```
Hello WorldProgramming is fun
```

Both get printed **on the same line**, right next to each other — because you never *told* the compiler to move to the next line.

### How to move to a new line

There are two ways:

**Option 1: `endl`** (short for "end line")

```cpp
cout << "Hello World" << endl;
cout << "Programming is fun";
```

**Option 2: `\n`** (newline character, written inside the string itself)

```cpp
cout << "Hello World\n";
cout << "Programming is fun";
```

**Output (either way):**

```
Hello World
Programming is fun
```

### `\n` vs `endl` — what's the difference?

| Feature | `\n` | `endl` |
| --- | --- | --- |
| Type | Escape sequence | Manipulator |
| Action | Inserts a newline | Inserts a newline **and** flushes the output buffer |
| Speed | Faster | Slightly slower (because of the flush) |

---

## 9. The Cascading Effect

You don't need a separate `cout` statement for every single thing you want to print. You can chain multiple outputs together in a **single** `cout` statement using the `<<` operator repeatedly:

```cpp
cout << "Hello World" << endl << "Programming is fun" << endl;
```

**Output:**

```
Hello World
Programming is fun
```

This chaining of multiple outputs within one `cout` statement is called the **cascading effect**.

The same applies to `cin` — you can take multiple inputs in a single line:

```cpp
cin >> a >> b;
```

---

## 10. Tokens, Whitespace, Semicolons & Blocks

### Tokens

Take this statement:

```cpp
cout << "LearnYard";
```

Break it down — `cout`, the `<<` operator, and `"LearnYard"` are all individual, meaningful pieces.

> A **token** is the smallest individual unit of a C++ program. Everything you write — keywords, identifiers, operators, punctuation, strings — is made up of tokens.
> 

**Types of tokens:**

| Type | Examples |
| --- | --- |
| Keywords | `int`, `return`, `if` |
| Identifiers (variable names) | `x`, `sum` |
| Constants | `5`, `'A'`, `3.14` |
| Operators | `+`, `-`, `*`, `/` |
| Punctuators/Symbols | `{ }`, `;`, `( )`, `,` |
| Strings | `"Hello"` |

Example:

```cpp
int x = a + 5 * (b - 3);
```

Here, `int`, `x`, `=`, `a`, `+`, `5`, `*`, `(`, `b`, `-`, `3`, `)` — each of these is a **separate token**.

### Whitespace

Any empty space between tokens (used purely for readability) is called **whitespace**. The compiler ignores it — it's there for humans, not machines.

### Semicolon (`;`)

Every statement in C++ must end with a semicolon — this tells the compiler: **"this statement has ended here."**

Think of it like a full stop at the end of a sentence. Miss it, and your compiler will throw an error.

```cpp
cout << "Sachin";   // ✅ correct — ends with a semicolon
cout << "Sachin"    // ❌ error — missing semicolon
```

### Blocks

When you need to group **multiple statements** together (like inside `main()`), you wrap them inside curly braces `{ }`. This group is called a **block**.

```cpp
int main() {
    int a = 10;         ┐
    cout << a;           │  all inside one block
    return 0;            ┘
}
```

**Important rule:** Anything created *inside* a block only exists *inside* that block. If you try to use it outside, the compiler won't recognize it.

```cpp
{
    int a = 10;
}
cout << a;   // ❌ error — 'a' doesn't exist outside its block
```

- A **single statement** (one line) doesn't need curly braces.
- **Multiple statements** grouped together *must* be wrapped inside `{ }` — this is called a **compound statement**, or simply, a block.

---

## Quick Recap Diagram

```
┌─────────────────────────────────────────────┐
│  #include <iostream>   ← Preprocessor       │
│  using namespace std;  ← Namespace          │
│                                             │
│  int main() {          ← Main function      │
│      cout << "Hi";     ← Statement          │
│      return 0;         ← Return value       │
│  }                      ← Block ends        │
└─────────────────────────────────────────────┘
```

- **Preprocessor** → runs *before* compilation (`#include`, `#define`)
- **Header file** → pre-written code you can reuse (`iostream`, `cmath`, `string`)
- **Namespace** → avoids naming conflicts (`std` = Standard Template Library's box)
- **`main()`** → the entry point; everything inside `{ }` gets executed
- **`return 0`** → tells the OS the program ran successfully
- **Comments** → ignored by the compiler, help programmers understand code
- **Tokens** → smallest building blocks of any C++ program
- **Semicolon** → marks the end of every statement
- **Block** → group of statements inside `{ }`