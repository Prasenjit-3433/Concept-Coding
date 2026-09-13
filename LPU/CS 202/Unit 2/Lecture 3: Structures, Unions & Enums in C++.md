# Structures, Unions & Enums in C++

Status: Pending

# 🎯 Part 1: Structures (`struct`)

---

## 1. The Problem Structures Solve

Suppose you want to store a student's data — say, their name and age:

```cpp
string studentName = "Sachin";
int studentAge = 20;
```

Now suppose you have a **second** student. You'd need a whole new set of variables:

```cpp
string student2Name = "Rahul";
int student2Age = 22;
```

> If you had to store data for 100 students this way, you'd need to create a huge number of separate variables. It becomes cluttered and messy — you won't even be able to easily tell which age belongs to which student.
> 

> Isn't there some data type that can store a student's data individually, as a **`single packet`**? Yes — this is exactly what a **structure** is for.
> 

---

## 2. What Is a Structure?

[class.avif](Structures,%20Unions%20&%20Enums%20in%20C++/class.avif)

> Just like `int` and `char` are data types built into C++, you can create your **own** data type. A structure lets you group multiple different data types together, into a single new data type of your own design.
> 

```
┌─────────────────────────────────────────────────┐
│              STRUCTURE = BLUEPRINT              │
│                                                 │
│   struct Student {                              │
│       name                                      │
│       roll number                               │
│       marks                                     │  
│   }                                             │
│                                                 │
│   → This defines WHAT a student looks like      │
│     — it doesn't create an actual student       │
│     yet, just the blueprint for one.            │
└─────────────────────────────────────────────────┘
```

> A structure is a **user-defined data type** that allows us to group different data types together, under one name.
> 

---

## 3. Declaring a Structure — Syntax

```cpp
struct structure_name {
    **data_type** member1;
    **data_type** member2;
};
```

> Don't forget the semicolon `;` after the closing curly brace — this is a common beginner slip, since normal blocks (like `if` or a function body) don't need one.
> 

### Example — The `Student` Structure

```cpp
struct Student {
    int roll;
    float marks;
};
```

> This creates a **blueprint** called `Student`. It doesn't create any actual student yet — it just says "whenever you make a `Student`, it will have a roll number and marks."
> 

---

## 4. Creating a Structure Variable

Once the blueprint exists, you can create variables of this new type — exactly like creating an `int` variable:

```cpp
int marks = 10;        // a normal variable of a built-in type
Student s1;              // a variable of our NEW, user-defined type
```

> `s1` is like an **object created from the blueprint** — an actual student, built according to the `Student` structure's design.
> 

### Setting Values — the Dot Operator (`.`)

To access or set any of `s1`'s properties, you use the **dot operator**:

```cpp
s1.roll = 101;
s1.marks = 89.5;
```

> Whenever you want to read or write an attribute inside a structure, you use the dot operator (`.`) between the variable name and the attribute name.
> 

### Full Example — With a Name Field Too

```cpp
#include <iostream>
#include <string>
using namespace std;

struct Student {
    string name;
    int roll;
    float marks;
};

int main() {
    Student s1;
    s1.name = "Sachin";
    s1.roll = 1001;
    s1.marks = 92.5;

    cout << s1.name << endl;
    cout << s1.roll << endl;
    cout << s1.marks << endl;

    return 0;
}
```

**Output:**

```
Sachin
1001
92.5
```

> Notice: because we used `string`, we need `#include <string>` at the top, alongside the usual `#include <iostream>`. The structure itself (`struct Student {...}`) is written **outside** `main()` — it's a blueprint, so it needs to exist before `main()` uses it.
> 

---

## 5. Memory Allocation in a Structure

This is one of the most important ideas in this entire lecture, because it's what makes structures and unions genuinely different from each other later.

> **Total memory used by a structure = the sum of the memory taken by each of its individual members** (plus any padding the compiler might add — but for our purposes, think of it as "add them all up").
> 

### Worked Example — `s1` with name = "Sachin"

| Member | Type | Memory |
| --- | --- | --- |
| `name` ("Sachin") | string (6 characters) | 6 × 1 byte = 6 bytes |
| `roll` | int | 4 bytes |
| `marks` | float | 4 bytes |

```
Total memory for s1 = 6 + 4 + 4 = 14 bytes
```

```
┌────────────────────────────────────────────────────┐
│           **STRUCTURE MEMORY LAYOUT**              │
│                                                    │
│  name (6 bytes) │ roll (4 bytes) │ marks (4 bytes) │
│  ┌─────────────┐ ┌───────────┐    ┌──────────┐     │
│  │ S a c h i n │ │  1001     │    │  92.5    │     │
│  └─────────────┘ └───────────┘    └──────────┘     │
│                                                    │
│  Every member gets its OWN separate space          │
└────────────────────────────────────────────────────┘
```

> Keep this "sum of all members" idea in your back pocket — when we get to unions shortly, memory allocation works completely differently, and comparing the two side by side is what really makes unions click.
> 

---

## 6. Structure Variation 1 — Direct Initialization

Instead of setting each field one at a time with the dot operator, you can initialize all the values directly, in order, using curly braces:

```cpp
Student s2 = {"Rahul", 1002, 78.0};
```

> This creates `s2` and fills in `name`, `roll`, and `marks` all at once, in the exact order they were declared inside the structure.
> 

---

## 7. Structure Variation 2 — Arrays of Structures

Just like you can have an array of integers, you can have an **array of structures** — useful when you need to store many students (or any structured records) under a single variable name.

```cpp
Student list[2];

list[0].name = "Sachin";
list[0].roll = 1001;
list[0].marks = 92.5;
```

> This creates an array called `list`, of size `2`, where each **slot** in the array holds a full `Student` — not just a single number, but an entire packet of name, roll, and marks. This is exactly the same array indexing concept from Lecture 9 — just applied to a user-defined type instead of a built-in one.
> 

### A Cleaner Way — Combined Declaration + Initialization

```cpp
Student school[2] = {
    {"Sachin", 1001, 89.5},
    {"Aman", 1002, 44.5}
};
```

> Each individual student's data sits inside its own set of curly braces — and all the students together sit inside one larger set of curly braces, exactly the same nested-braces pattern used for 2D array initialization back in Lecture 9.
> 

### Accessing an Element From the Array

```cpp
cout << school[0].name;    // "Sachin"
cout << school[0].roll;    // 1001
cout << school[0].marks;   // 89.5

cout << school[1].name;    // "Aman"
```

```
┌──────────────────────────────────────────────────┐
│                 **ARRAY OF STRUCTURES**          │
│                                                  │
│      school[0]               school[1]           │
│  ┌───────────────┐      ┌───────────────┐        │
│  │ name: Sachin  │      │ name: Aman    │        │
│  │ roll: 1001    │      │ roll: 1002    │        │
│  │ marks: 89.5   │      │ marks: 44.5   │        │
│  └───────────────┘      └───────────────┘        │
└──────────────────────────────────────────────────┘
```

> To get any *other* student's data, you don't need any special syntax — just change the index. `school[0]` gives you the first student's full record, `school[1]` gives you the second, and so on.
> 

### A Full Worked Example — A Class of 50 Students

The same idea scales up naturally:

```cpp
Student classOf10[50];   // an array holding 50 full student records
```

> This is the practical, real-world version of the idea: instead of 50 sets of separate name/roll/marks variables, you get one clean array of 50 structured records.
> 

---

## 8. Structure Variation 3 — `Structure Pointers`

Just like you can create a pointer to an `int` or `char`, you can create a pointer to a **structure**.

### Declaring a Structure Pointer

```cpp
Student s1;
s1.name = "Sachin";

Student* studentPtr = &s1;
```

Break this declaration into its two pieces:

```
Student*        studentPtr
   ↑                ↑
 the TYPE       the VARIABLE
(pointer to a    (just a name
   Student)       you chose)
```

- `Student*` is the **type** — "a pointer to a `Student`."
- `studentPtr` is just a regular **variable name** — you could call it `p`, `ptr`, `addr`, anything you like. Naming it `studentPtr` is only for readability; the name itself doesn't do anything special.

> **Important clarification:** in C, you had to write `struct Student *ptr` — the `struct` keyword was required every time you used the type. In **C++, this is optional** — `Student* ptr` works perfectly fine on its own. You may still see the older `struct Student *ptr` style in some code, but there's no need to write it that way in C++.
> 

`&s1` gives us the address of `s1` (the address-of operator, exactly as in the Pointers & References lecture), and we store that address inside `studentPtr`.

```
┌─────────────────────────────────────────────────┐
│  Student* studentPtr = &s1;                     │
│                                                 │
│   studentPtr ────────► s1                       │
│   (stores s1's address)   (name: Sachin)        │
└─────────────────────────────────────────────────┘
```

---

### Accessing Members Through a Structure Pointer — The Arrow Operator (`>`)

Since `studentPtr` holds an *address*, not the structure itself, you can't use the dot operator directly on it. Instead, C++ gives you the **arrow operator** (`->`):

```cpp
cout << studentPtr->name;   // prints "Sachin"
cout << studentPtr->roll;
```

> The arrow operator (`->`) lets you reach into whatever the pointer is pointing to, and access one of its members directly — without ever needing to write `s1` again. Read `studentPtr->name` as: "go to the address this pointer holds, and get its `name` attribute."
> 

### Full Worked Example

```cpp
#include <iostream>
#include <string>
using namespace std;

struct Student {
    string name;
    int roll;
    float marks;
};

int main() {
    Student s1;
    s1.name = "Sachin";

    Student* studentPtr = &s1;

    cout << studentPtr->name;   // Output: Sachin

    return 0;
}
```

**Output:** `Sachin`

---

### A Common Point of Confusion — Pointer Variable vs. Pointer Type

It's easy to look at a name like `studentPtr` and assume it's some special new *type* you've created — like `Student` itself was a new type. **It isn't.** It's an ordinary variable, whose type happens to be `Student*`.

If you actually *want* to create a reusable name for the type "pointer to a `Student`" — so you can declare several such pointers without repeating `Student*` every time — C++ gives you the `using` keyword:

```cpp
using StudentPtr = Student*;   // StudentPtr is now a TYPE, not a variable

StudentPtr p1 = &s1;   // same as writing: Student* p1 = &s1;
StudentPtr p2 = &s2;
```

```
Student* studentPtr;      →  Student*  is the type,  studentPtr  is the variable

using StudentPtr = Student*;   →  StudentPtr  itself becomes a new TYPE NAME
```

You may also see the older, C-style way of doing the same thing, using `typedef`:

```cpp
typedef Student* StudentPtr;   // older syntax, same effect as "using"
```

Both achieve the same result — `using` is simply the modern, preferred style in C++.

> **For now:** stick with the plain form, `Student* ptr;`, for declaring structure pointers — that's what you'll use almost all the time. Just be aware that `using` (or `typedef`) exists for when you want to give a pointer type its own name, later on.
> 

---

## Key Points to Remember (Structures) — Updated

- A **structure** is a user-defined data type that groups **different data types** together under one name — solving the "too many separate variables" problem for related data.
- Declare with `struct structure_name { ... };` — **don't forget the trailing semicolon**.
- Create a variable of that type just like any built-in type: `Student s1;` — then use the **dot operator** (`.`) to set or read its members: `s1.roll = 101;`.
- **Memory allocation**: a structure's total size is the **sum of the memory of all its members**.
- You can initialize a structure **directly** with `{value1, value2, value3}`, in the same order the members were declared.
- An **array of structures** (`Student list[50];`) lets you store many full records under one variable name, indexed exactly like a normal array.
- A **structure pointer** is declared as `Student* ptr = &s1;` — `Student*` is the type, `ptr` is just an ordinary variable name. The `struct` keyword before the type name (`struct Student*`) is a C-style holdover and is **not required** in C++.
- Use the **arrow operator** (`>`) to access members through a pointer: `ptr->name`, instead of the dot operator.
- If you want to give the pointer type itself a reusable name, use `using AliasName = Student*;` (modern) or `typedef Student* AliasName;` (older) — but for everyday use, plain `Student* ptr` is what you'll reach for most.

# 🎯 Part 2: Unions (`union`)

---

## 1. The Problem Unions Solve

Let's build up to this with the instructor's own example: an MNC company with employees from many regions, whose salaries could be paid in different currencies.

Suppose we want to store an employee's salary — but the salary might be in **INR**, **USD**, or **pounds**, depending on the employee. You'd never need all three *at the same time* for one employee — only **one** of them applies.

> A structure works great when you need to store and use **multiple attributes simultaneously** — like a student's name, roll number, and marks, all needed at once. But when you have multiple *possible* attributes and only **one** of them will ever be used at a time, a structure is the wrong tool — you'd be wasting memory holding space for options you'll never use together.
> 

This is exactly the gap a **union** fills.

> A union is a user-defined data type used when you have **multiple options**, but at any given moment, you only need to store **one** of them.
> 

---

## 2. What Is a Union?

Think of a real-world analogy the instructor uses: a currency toggle in a banking app.

```
┌─────────────────────────────────────────────────┐
│         A CURRENCY TOGGLE (Analogy)             │
│                                                 │
│     [ INR ]     [ USD ]     [ Pounds ]          │
│                                                 │
│   Only ONE of these is ever "active" at         │
│   a time. Switching to USD doesn't keep         │
│   the INR value around alongside it — it        │
│   REPLACES it.                                  │
└─────────────────────────────────────────────────┘
```

> Just like a language toggle (Hindi, English, Marathi...) — the moment you switch to English, Hindi isn't "also" being displayed. Only one option is active at any given time. A union works exactly the same way: all its members share **one single block of memory**, and setting one member overwrites whatever was stored there before.
> 

---

## 3. Declaring a Union — Syntax

The syntax looks nearly identical to a structure — this is deliberate, since the *declaration* style is the same, but the *behavior* is completely different:

```cpp
union union_name {
    data_type member1;
    data_type member2;
};
```

### Example — A `Data` Union

```cpp
union Data {
    int i;
    float f;
};
```

---

## 4. Using a Union — Watch the Overwrite Happen

```cpp
Data d;
d.i = 10;
cout << d.i;   // Output: 10

d.f = 5.5;
cout << d.f;   // Output: 5.5   ← but now d.i is gone!
```

> As soon as you store a value into `d.f`, the value previously held in `d.i` is **destroyed** — because `i` and `f` were never in separate memory locations to begin with. They were always sharing the **exact same block of memory**.
> 

### Tracing It With the Salary/Wallet Example

```cpp
union Money {
    float INR;
    float USD;
    float pounds;
};

Money wallet;
wallet.INR = 1500.50;
cout << wallet.INR;   // 1500.50

wallet.USD = 20.75;
cout << wallet.USD;   // 20.75

cout << wallet.INR;   // NOT 1500.50 anymore — it now reflects whatever
                        // bits are sitting in that shared memory, since
                        // USD overwrote it
```

> Notice the wallet only ever has **one active value** at a time. The moment `wallet.USD` is set, the `INR` value stored previously is gone — overwritten in the same shared memory block. If you then print `wallet.INR`, you won't get back `1500.50` — you'll get whatever the USD value looks like when misread as INR's type.
> 

![image.png](Structures,%20Unions%20&%20Enums%20in%20C++/image.png)

```
┌──────────────────────────────────────────────────────┐
│              **UNION MEMORY: SHARED BLOCK**          │
│                                                      │
│   wallet.INR = 1500.50                               │
│   ┌──────────────────┐                               │
│   │   1500.50        │  ← single shared block        │
│   └──────────────────┘                               │
│                                                      │
│   wallet.USD = 20.75                                 │
│   ┌──────────────────┐                               │
│   │   20.75          │  ← INR's value is GONE,       │
│   └──────────────────┘     overwritten by USD        │
└──────────────────────────────────────────────────────┘
```

---

## 5. Memory Allocation in a Union — The Key Contrast With Structures

This is where everything from Part 1's memory discussion pays off.

> **A union's total memory = the size of its LARGEST member only** — not the sum of all of them.
> 

### Side-by-Side Comparison

Suppose all three currency fields are `float` (4 bytes each):

| Data Type | Memory Calculation | Total |
| --- | --- | --- |
| **Structure** (all 3 fields) | 4 + 4 + 4 (each gets its own space) | **12 bytes** |
| **Union** (same 3 fields) | Only the largest one is allocated — all 4 bytes | **4 bytes** |

```
┌────────────────────────────────────────────────────────┐
│         **STRUCTURE vs UNION — MEMORY**                │
│                                                        │
│  STRUCT: [ INR: 4B ][ USD: 4B ][ pounds: 4B ]          │
│          Total = 12 bytes (all separate)               │
│                                                        │
│  UNION:  [ shared 4-byte block ]                       │
│          Total = 4 bytes (all three share this)        │
└────────────────────────────────────────────────────────┘
```

> Because a union only ever needs to hold **one** value at a time, there's no point reserving separate space for every possible option — it just reserves enough space for the **biggest** one, and every member takes turns using that same space.
> 

---

## 6. Structure vs. Union — Comparison Table

| Feature | Structure | Union |
| --- | --- | --- |
| **Memory** | Separate space for each member | Shared — all members use the same block |
| **Members active at once** | All | Only one |
| **Total size** | Sum of all members | Size of the **largest** member |
| **Best used for** | Complex, related data needed **together** (e.g., a student's full record) | Memory optimization when only **one** of several options is needed at a time |

> **When to use which:** if you need to store and use multiple attributes *simultaneously* → use a **structure**. If you have multiple *possible* attributes but only ever need *one* of them active at a time → use a **union**, since it saves memory.
> 

---

## Key Points to Remember (Unions)

- A **union** is a user-defined data type where **all members share a single block of memory** — only **one** member can hold a valid value at any given moment.
- Declaring a union looks just like declaring a structure (`union union_name { ... };`), but the memory behavior is completely different.
- Setting a new member's value **overwrites** whatever was previously stored in any other member — because they were never in separate memory to begin with.
- **Memory allocation**: a union's total size equals the size of its **largest member only** — not the sum of all members, unlike a structure.
- The classic use case: situations with multiple *possible* representations of the same underlying thing, where only one is ever needed at a time — like storing a salary in exactly one currency, chosen from several options.
- Structures and unions look nearly identical in syntax, but serve opposite purposes: structures are for holding everything **together**; unions are for holding **one thing at a time**, efficiently.

# 🎯 Part 3: Enumerations (`enum`)

---

## 1. The Problem Enums Solve

Suppose you create a plain integer variable to store marks:

```cpp
int marks;
```

An `int` can hold *anything* — `100`, `500`, `-7`, `1000000`. There's no restriction at all.

But now think of a different kind of variable — say, the day of the week. A week can only ever be one of **seven** specific values: Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday. There is no eighth option.

> Sometimes you want a variable to only be allowed to hold a **limited, fixed set of values** — nothing outside that set should even be possible. This is exactly what an **enum** (short for enumeration) is for.
> 

---

## 2. What Is an Enum?

> An enum lets you create your own data type that can only take on a small, fixed list of named values.
> 

Think of it like the `char` data type — a `char` can only ever be one of the letters/symbols in its character set, nothing else. An enum works the same way, except **you** decide exactly what the allowed values are.

### Example — Days of the Week

```cpp
enum Week {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
};
```

Now you can create a variable of this new type:

```cpp
Week today = MONDAY;
```

> `Week` is the data type here — just like `int` or `char` — and `today` is a variable of that type. But `today` can **only** ever be one of the seven values listed inside the enum. You cannot assign it anything else.
> 

### Another Example — Colors

```cpp
enum Color {
    RED, BLUE, GREEN
};

Color tableColor = RED;
```

> `Color` has exactly three allowed options. You could set `tableColor` to `RED`, `BLUE`, or `GREEN` — but **not** `YELLOW`, since `YELLOW` was never listed as one of the enum's values.
> 

---

## 3. Declaring an Enum — Syntax

```cpp
enum enum_name {
    value1,
    value2,
    value3
};
```

---

## 4. What Happens Internally — Enums Are Really Just Numbers

This is the part that surprises most beginners.

> Internally, an enum does **not** store the actual text ("Monday", "Tuesday", etc.). It stores a **number** — each value in the list is automatically assigned an integer, starting from `0`, in the order you wrote them.
> 

```cpp
enum Week {
    MONDAY,     // 0
    TUESDAY,    // 1
    WEDNESDAY,  // 2
    THURSDAY,   // 3
    FRIDAY,     // 4
    SATURDAY,   // 5
    SUNDAY      // 6
};
```

```
┌─────────────────────────────────────────────────────┐
│              **ENUM — INTERNAL VALUES**             │
│                                                     │
│  MONDAY=0  TUESDAY=1  WEDNESDAY=2  THURSDAY=3       │
│  FRIDAY=4  SATURDAY=5  SUNDAY=6                     │
│                                                     │
│  These numbers are assigned AUTOMATICALLY,          │
│  in the order the values were written.              │
└─────────────────────────────────────────────────────┘
```

### Proof — Printing an Enum Value

```cpp
enum Day { MON, TUE, WED, THU, FRI };

Day d = WED;
cout << d;   // Output: 2
```

> `WED` is the **third** value listed (counting from `0`), so it holds the number `2` internally. If you `cout` an enum variable directly, you get back this **number**, not the word "WED".
> 

> **Formal definition:** an enumeration is a user-defined data type that assigns names to integral constants — meaning it gives easy-to-read names to what are really just numbers under the hood.
> 

---

## 5. Assigning Custom Values

By default, enum values start at `0` and count upward by `1`. But you can override this and assign your **own** numbers instead:

```cpp
enum Level {
    LOW = 1,
    MEDIUM = 3,
    HIGH = 5
};
```

```cpp
Level l = HIGH;

if (l == HIGH) {
    cout << "High level selected";
}
```

> You can compare enum variables using the normal relational operators (`==`, from Lecture 4) — `l == HIGH` checks whether `l` currently holds the value `HIGH` (which, internally, means checking whether `l` equals `5`).
> 

---

## Key Points to Remember (Enums)

- An **enum** is a user-defined data type that restricts a variable to a **small, fixed set of named values** — nothing outside that list is allowed.
- Declare with `enum enum_name { value1, value2, ... };`
- Internally, each value is stored as an **integer**, starting from `0` and increasing by `1`, in the order they're written — the actual text names exist only for readability in your code, not in memory.
- Printing an enum variable directly (`cout << d;`) gives you its **numeric** value, not its name — this trips up a lot of beginners the first time.
- You can **override the default numbering** by assigning custom values (`LOW = 1, MEDIUM = 3, HIGH = 5`).
- Enum variables can be compared with `==`, exactly like any other value.
- Classic real-world use cases: days of the week, a fixed set of colors, employee roles, status flags — any situation where the *set* of valid values is fixed and known in advance.

# 🎯 Part 4: Bringing It All Together — Employee Management System

---

## 1. The Goal

Now let's combine everything from this lecture into one real example — an **Employee Management System** — using:

- A **struct** to hold an employee's overall record
- A **union** to store the employee's salary (since salary will be in *one* form only — either a fixed monthly salary or an hourly wage, never both)
- An **enum** to restrict the employee's type to a fixed set of options

> This example is deliberately built so that all three concepts work together in one place — seeing them combined like this is what really makes each piece click.
> 

---

## 2. Step 1 — The Enum for Employee Type

An employee in this company is either **Permanent** or on **Contract** — nothing else. This is exactly the kind of "fixed, limited set of options" enums are built for.

```cpp
enum EmployeeType {
    PERMANENT,
    CONTRACT
};
```

> Internally: `PERMANENT = 0`, `CONTRACT = 1` — following the same automatic-numbering rule from Part 3.
> 

---

## 3. Step 2 — The Union for Salary

A **permanent** employee is paid a fixed **monthly salary**. A **contract** employee is instead paid an **hourly wage**. An employee is only ever one or the other — never both at once. This is exactly the "only one option active at a time" situation a union is designed for.

```cpp
union Salary {
    float monthlySalary;   // used if the employee is Permanent
    float hourlyWage;      // used if the employee is Contract
};
```

> Since both members are `float` (4 bytes), and a union only reserves space for its **largest** member, this union takes up just 4 bytes total — not 8.
> 

---

## 4. Step 3 — The Structure for Employee

Now we bring everything together into one structure, representing a complete employee record:

```cpp
struct Employee {
    int id;
    char name[50];
    EmployeeType type;   // uses our enum as a data type
    Salary salary;         // uses our union as a data type
};
```

> Notice something important here: `EmployeeType` and `Salary` — types **we ourselves created** — are being used as data types for members inside `Employee`, exactly the same way `int` or `float` would be used. This is the real payoff of creating your own user-defined types: once defined, they behave just like any built-in type.
> 

```
┌────────────────────────────────────────────────────────┐
│                  **EMPLOYEE STRUCTURE**                │
│                                                        │
│   id           →  int                                  │
│   name         →  char array                           │
│   type         →  EmployeeType (enum: Permanent/       │
│                    Contract)                           │
│   salary       →  Salary (union: monthlySalary OR      │
│                    hourlyWage — never both)            │
└────────────────────────────────────────────────────────┘
```

---

## 5. A Snag — Why We Can't Just `cin >>` Into an Enum

Before writing the full program, there's one genuine trap worth walking through carefully, because it produces a wall of confusing compiler errors the first time you hit it.

You might reasonably try this, to ask the user for the employee type:

```cpp
cout << "Enter Employee Type (0-Permanent, 1-Contract): ";
cin >> e.type;   // ❌ this does NOT compile
```

This throws a compiler error along the lines of `no operator ">>" matches these operands`.

> **Why this fails:** `cin >>` only knows how to read directly into **built-in types** — `int`, `float`, `char`, `string`, and so on. `EmployeeType` is a type **we** created ourselves, and the compiler has no idea how to read keyboard input directly into it. There's no `operator>>` defined anywhere for our custom enum.
> 

**The fix** is simple, and it reuses something we already know: since an enum is really just a number underneath (Part 3), read the input into a plain `int` first, then convert that number into the enum type using an explicit cast:

```cpp
int choice;
cin >> choice;
e.type = EmployeeType(choice);   // convert the plain number into an EmployeeType
```

> This is exactly the same **explicit type casting** idea from Lecture 2 (`static_cast`, or the older C-style `(type)value`) — just applied here to a user-defined enum instead of a built-in type like `int` or `float`.
> 

---

## 6. Full Program (Corrected)

```cpp
#include <iostream>
using namespace std;

enum EmployeeType {
    PERMANENT,
    CONTRACT
};

union Salary {
    float monthlySalary;
    float hourlyWage;
};

struct Employee {
    int id;
    char name[50];
    EmployeeType type;
    Salary salary;
};

int main() {
    Employee e;

    cout << "Enter ID: ";
    cin >> e.id;

    cout << "Enter Name: ";
    cin.ignore();               // clears **leftover newline** from the previous cin
    cin.getline(e.name, 50);    // reads the full name, including spaces

    int choice;
    cout << "Enter Employee Type (0-Permanent, 1-Contract): ";
    cin >> choice;
    e.type = EmployeeType(choice);   // convert the **number** into an **EmployeeType**

    if (e.type == PERMANENT) {
        cout << "Enter Monthly Salary: ";
        cin >> e.salary.monthlySalary;
    } else {
        cout << "Enter Hourly Wage: ";
        cin >> e.salary.hourlyWage;
    }

    cout << "\n--- Employee Details ---\n";
    cout << "ID: " << e.id << endl;
    cout << "Name: " << e.name << endl;

    if (e.type == PERMANENT) {
        cout << "Type: Permanent\n";
        cout << "Salary: " << e.salary.monthlySalary << endl;
    } else {
        cout << "Type: Contract\n";
        cout << "Hourly Wage: " << e.salary.hourlyWage << endl;
    }

    return 0;
}
```

---

## 7. Why `cin.ignore()` Is Needed Here

This connects directly back to Lecture 10 (Characters & Strings):

> When you use `cin >>` to read `e.id`, it leaves a leftover newline character (`\n`) sitting in the input buffer — the **"Enter" key** press you made after typing the ID. If you then call `cin.getline()` right after, it immediately reads that leftover newline as if it were the entire name, and your name ends up empty. `cin.ignore()` clears that leftover character out of the buffer first, so `getline()` correctly waits for and reads your actual name.
> 

---

## 8. Why We Can't Just `cout << e.type` Directly Either

Reading isn't the only place this "enum is really just a number" fact shows up — printing has the same catch.

```cpp
cout << e.type;   // **prints 0 or 1** — not "Permanent" or "Contract"
```

Since `e.type` is an enum, printing it directly gives you the **number** stored internally, not the word `"Permanent"` or `"Contract"`. To show a readable label instead, we check the value with an `if-else` and print the matching text ourselves:

```cpp
if (e.type == PERMANENT) {
    cout << "Type: Permanent\n";
} else {
    cout << "Type: Contract\n";
}
```

> Unlike the `cin >>` case, `cout <<` an enum actually **does** compile fine — it happily prints the number. The catch here isn't a compiler error, it's just that the output isn't what you *want* to see. So translating that number back into something human-readable is still *your* job.
> 

---

## 9. Why We Access Salary Based on `type`

Since `salary` is a **union**, only *one* of `monthlySalary` or `hourlyWage` actually holds a meaningful value at any time — whichever one was set last. So before reading from the union, we must check `e.type` to know **which** member is actually valid right now:

```cpp
if (e.type == PERMANENT) {
    cout << "Salary: " << e.salary.monthlySalary << endl;
} else {
    cout << "Hourly Wage: " << e.salary.hourlyWage << endl;
}
```

> If you tried to read `e.salary.hourlyWage` for an employee whose `type` was actually `PERMANENT`, you'd get meaningless data — because only `monthlySalary` was ever written to that shared memory block. The `type` field (our enum) is what tells us **which interpretation of the union's shared memory is actually correct**.
> 

---

## 10. Sample Run

```
Enter ID: 1001
Enter Name: Sachin
Enter Employee Type (0-Permanent, 1-Contract): 0
Enter Monthly Salary: 45000

--- Employee Details ---
ID: 1001
Name: Sachin
Type: Permanent
Salary: 45000
```

---

## Key Points to Remember (Combined Example)

- A **struct** can contain members whose types are themselves **user-defined** — like an `enum` or a `union` — exactly the same way it contains built-in types like `int` or `float`.
- Use an **enum** to restrict a field (like employee type) to a small, fixed set of valid options.
- Use a **union** when a field (like salary) can be represented in more than one way, but only **one** representation is ever valid for a given record at a time.
- **`cin >>` cannot read directly into an enum variable** — the compiler doesn't know how to fill a user-defined type from input. The fix: read into a plain `int`, then convert with an explicit cast — `e.type = EmployeeType(choice);` — the same explicit-casting idea from Lecture 2.
- **`cout <<` an enum compiles fine, but prints the internal number**, not the readable name — translating that number back into text still requires your own `if-else` (or `switch`).
- Since a union only reserves **one shared block of memory**, always track — usually via a separate field, like our `type` enum — **which** member is currently valid, before reading from it.
- `cin.ignore()` before `cin.getline()` is required whenever a `cin >>` (which leaves a leftover newline) is immediately followed by a full-line read — this is a recurring gotcha from the Strings lecture that shows up constantly in real programs.
- This kind of combined example is exactly what shows up in real-world modeling: structs organize *related* data together, enums constrain *categorical* fields, and unions save memory when fields are *mutually exclusive*.