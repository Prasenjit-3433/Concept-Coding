# Lecture 2: DataTypes in C++

Status: Done

# Lecture 2: Data Types in C++

---

## 1. What is a Data Type?

Think of it like food. "Food" is a broad category, but inside it there are types — fruits, vegetables, snacks. Each type behaves differently and is used differently.

In the same way, **data type** tells the compiler what *kind* of data a variable is going to store, and consequently:

- How much **memory** it will take
- What **range of values** it can hold
- What **operations** are allowed on it

---

## 2. The Three Categories of Data Types

C++ data types are divided into **3 categories**:

![image.png](Lecture%202%20DataTypes%20in%20C++/image.png)

**1. Built-in (Fundamental) data types** — these come pre-built into the computer/compiler. They are the **smallest**, most basic data types: `int`, `float`, `double`, `char`, `bool`, `void`.

**2. Derived data types** — formed by combining built-in data types in large quantities. For example, a single number (say, 7) is an `int`. But if you need to store *many* integers together, you use an **array** — a derived data type built from the fundamental one. Arrays, pointers, references, and functions all fall here, and each gets its own dedicated module later in the course.

**3. User-defined data types** — unlike the above two (which are handed to you by the language), these are types **you create yourself**: `class`, `structure`, `union`, and `typedef`. Out of these, `typedef` is what we cover today; class, structure, and union get their own dedicated videos.

---

## 3. Built-in Data Types — In Detail

There are 5 built-in data types we care about right now (the 6th, `void`, will make sense once we study functions):

| Data Type | Keyword |
| --- | --- |
| Integer | `int` |
| Floating-point | `float` |
| Character | `char` |
| Double | `double` |
| Boolean | `bool` |

### Integer (`int`)

Integers are **whole numbers** — every number from negative infinity through zero to positive infinity, with **no decimal point**.

```cpp
77   // integer
89   // integer
92   // integer
```

### Float vs. Double

Both `float` and `double` store **floating-point numbers** — numbers with a decimal point.

```cpp
3.14   // this is a floating-point number (pi)
3      // this alone would just be an integer
```

So what's the actual difference between them? **Precision.**

- `float` → lower precision, stores **fewer digits** after the decimal point
- `double` → higher precision, stores **more digits** after the decimal point

This distinction is small but important — it trips up a lot of beginners, so make sure it's clear.

### Character (`char`)

A character stores a **single symbol** — one letter, one digit, one symbol.

Take the name "Sachin": each letter — S, A, C, H, I, N — is an individual **character**.

### Boolean (`bool`)

Boolean stores only **true or false** (yes/no, on/off).

Think of a Netflix pop-up: "Do you want to exit the app?" — Yes or No. That answer gets stored as a Boolean value in their database:

```
Yes  →  stored as 1  (true)
No   →  stored as 0  (false)
```

So internally, Boolean values are represented using just **1 and 0**.

---

## 4. How Much Space Does Each Data Type Take?

| Data Type | Memory (bytes) |
| --- | --- |
| `int` | 4 bytes |
| `float` | 4 bytes |
| `double` | 8 bytes |
| `char` | 1 byte |
| `bool` | 1 byte |

---

## 5. How Is Data Actually Stored in Memory?

This is the part most teachers skip — let's actually understand *where* those zeros and ones live.

### From hard drive to bit

![image.png](Lecture%202%20DataTypes%20in%20C++/image%201.png)

A hard drive, internally, looks like a **cylindrical stack of circular platters** (imagine several DVDs stacked on top of each other, spinning around a central rod):

```
        ┌───────────┐
        │ Platter 5 │
        ├───────────┤
        │ Platter 4 │
        ├───────────┤
        │ Platter 3 │  ← stacked platters form
        ├───────────┤     a ***cylindrical*** shape
        │ Platter 2 │
        ├───────────┤
        │ Platter 1 │
        └───────────┘
             ▲
        (Read/Write Head
         reads & writes data)
```

A **read/write head** sits beside this stack and is responsible for reading data from and writing data to the platters.

### Zooming into one platter

Looked at from the top, a single platter is made up of many concentric **rings**:

![image.png](Lecture%202%20DataTypes%20in%20C++/image%202.png)

Each ring is further divided into tiny **sections** (blocks). If you "unroll" one ring and stretch it out into a straight line, it looks like a long strip made of tiny blocks, one after another:

![image.png](Lecture%202%20DataTypes%20in%20C++/image%203.png)

*(This exact strip-of-blocks representation is what we'll reuse throughout the course whenever we draw memory — for arrays, linked lists, stacks, and more.)*

### What lives inside one block?

![image.png](Lecture%202%20DataTypes%20in%20C++/image%204.png)

Each tiny block stores exactly **one bit** — a single binary value: either a **0** or a **1**.

![image.png](Lecture%202%20DataTypes%20in%20C++/image%205.png)

> **Bit** = the smallest unit of memory. It can only be 0 or 1.
> 

Now, if we take only one round strip & cut it at any place on it and then flatten it, then it will look like an ***array***.

![image.png](Lecture%202%20DataTypes%20in%20C++/image%206.png)

### Bit vs. Byte

**8 bits together = 1 byte.**

A **byte** is what actually has a **memory address** — an individual bit does not.

```
[0][1][1][0][0][1][0][1]  ← 8 bits = 1 byte  (has an address, e.g. 100)
```

### Example: storing the integer 4

`4` in binary is `100`.

Since an `int` takes **4 bytes = 32 bits**, the number is padded with zeros to fill all 32 bits:

```
00000000 00000000 00000000 00000**100**
└──────────── 29 zeros ──────┘└100┘
```

Even though `4` only "needs" 3 bits to represent, C++ still reserves the full 32 bits for every `int` — this way, large numbers don't run out of space and cause data loss.

![image.png](Lecture%202%20DataTypes%20in%20C++/image%207.png)

In memory, 1 byte (i.e., 8 bits) is represented by one memory address. 

### How are characters stored?

Characters are stored using their **ASCII value** — a numeric code officially assigned to every character.

| Character | ASCII Value |
| --- | --- |
| `A` | 65 |
| `B` | 66 |
| `C` | 67 |

So when you store `'A'`, the compiler actually converts `65` into binary and stores *that* — but displays it back to you as `A`.

```cpp
char A = 65;
cout << A;   // Output: A   (because 65 is A's ASCII value)
```

---

## 6. Precision Recap

| Type | Decimal Precision |
| --- | --- |
| `float` | up to 6–7 digits after the decimal |
| `double` | up to 15 digits after the decimal |

---

## 7. Variables — A Quick Preview

*(Full topic in the next video, but you need a taste of it now to write data-type code.)*

Suppose you want to store your Science marks (74) in memory. You need to specify **three things**:

```
   data type      variable name      value
     ▼                 ▼               ▼
   int          scienceMarks    =      74;
```

This is exactly like labeling containers in a kitchen — a jar of sugar gets a "sugar" label so nobody confuses it with salt. That label is the **variable name**; it lets you access, modify, and update the data stored in that memory block later.

```cpp
int scienceMarks = 74;     // creates a 4-byte block named "scienceMarks", stores 74
float bankBalance = 992.6; // creates a 4-byte block named "bankBalance"
char initials = 'S';       // creates a 1-byte block named "initials"
bool isCodingFun = true;   // creates a 1-byte block, stores 1
```

---

## 8. Type Modifiers — More Flavors of `int`

Sometimes plain `int` isn't precise enough about *how small* or *how large* a number is. That's where **modifiers** come in:

| Modifier | Meaning |
| --- | --- |
| `short` | for small integer values |
| `long` | for very large integer values |
| `signed` | can store negative and positive values |
| `unsigned` | stores only non-negative (0 and above) values |

```cpp
short int smallNumber = 8;
long int bigNumber = 238623826387943287;
unsigned int positiveOnly = 40000;
```

---

## 9. The `auto` Keyword

Instead of manually specifying the data type every time, `auto` tells the compiler: **"figure out the type yourself, based on the value I'm assigning."**

```cpp
auto marks = 5;      // compiler infers this is an int
auto pi = 3.14;       // compiler infers this is a double
auto initial = 'S';   // compiler infers this is a char
```

**Important:** once `auto` locks in a type, you **cannot** later assign a value of a different type to that same variable:

```cpp
auto x = 5;      // x becomes int
x = 10;          // ✅ fine, still an int
x = 9.99;        // ❌ error — x was locked in as int
```

---

## 10. The `sizeof` Operator

`sizeof` tells you **how much memory (in bytes)** a variable or data type is occupying.

```cpp
int x = 10;
cout << sizeof(x);      // Output: 4
cout << sizeof(int);    // Output: 4
```

**Rule to remember:**

- When using it on a **variable**, no parentheses needed: `sizeof x`
- When using it on a **data type**, parentheses are required: `sizeof(int)`

---

## 11. `typedef` — Creating Your Own Data Type Name

`typedef` lets you create an **alias** — a new name — for an existing data type.

**Why would you want this?** Say you're storing measurements in feet. Instead of writing `int` everywhere, you can create a more meaningful name:

```cpp
typedef int feet;     // "feet" is now another name for "int"

feet distance = 10;   // same as writing: int distance = 10;
```

Breaking this down:

- `feet` → your new data type name
- `distance` → variable name
- `10` → the value stored

This new type (`feet`) is still built **on top of** an existing type (`int`) — you're just giving it a more meaningful label for your use case.

---

## 12. Type Casting (Type Conversion)

**Type casting** = converting a value from one data type into another.

There are **two kinds**: Implicit and Explicit.

### Implicit Type Casting (Automatic)

This happens **automatically**, without you asking for it — usually when you mix different data types in one expression.

```cpp
int x = 10;
double y = 6.6;
int z = x + y;     // first 10 becomes 10.0, then x + y = 16.6, but z is an int...
cout << z;          // Output: 16  (the .6 gets dropped *automatically*)
```

Here, the compiler automatically converted the `double` result into an `int` because `z` was declared as an `int`. This silent, automatic conversion is called **implicit type casting**.

Similarly, if we go the other way:

```cpp
int a = 5;
double b = 2.5;
double result = a + b;   // a is automatically promoted to 5.0 (double)
cout << result;            // Output: 7.5
```

Here, the **smaller type (`int`) got promoted to the larger type (`double`)** to avoid losing data — this is the general rule implicit casting follows.

### Explicit Type Casting (Manual)

Here, **you** tell the compiler exactly what conversion to perform. There are two ways to write it:

**a) C-style casting (older method):**

```cpp
int a = 10;
double b = (double)a / 3;   // forcibly convert a to double before dividing
cout << b;                    // Output: 3.33333
```

**b) C++-style casting using `static_cast` (newer method):**

```cpp
double pi = 3.14;
int x = static_cast<int>(pi);   // explicitly convert pi to int
cout << x;                        // Output: 3
```

Both approaches achieve the same result — you're just being explicit about the conversion instead of letting the compiler decide silently.

### Quick Comparison

|  | Implicit | Explicit |
| --- | --- | --- |
| Who triggers it? | Compiler (automatic) | Programmer (manual) |
| Example | `int z = x + y;` | `static_cast<int>(pi)` |

---

## Quick Recap Diagram

```
┌──────────────────────────────────────────────┐
│              DATA TYPES                      │
│  ┌──────────┐ ┌──────────┐ ┌─────────────┐   │
│  │ Built-in │ │ Derived  │ │ User-Defined│   │
│  └──────────┘ └──────────┘ └─────────────┘   │
└──────────────────────────────────────────────┘

int    → 4 bytes  → whole numbers
float  → 4 bytes  → decimals (low precision)
double → 8 bytes  → decimals (high precision)
char   → 1 byte   → single character (via ASCII)
bool   → 1 byte   → true(1) / false(0)

Memory:  1 byte = 8 bits    Bit = smallest unit (0 or 1)

auto     → compiler infers the type automatically
sizeof() → tells you how many bytes something occupies
typedef  → create your own alias for an existing type

Type Casting:
  Implicit → done automatically by the compiler
  Explicit → done manually: (type)value  OR  static_cast<type>(value)
```