# Lecture 2: Constructor with Default Arguments & Initializer Lists

Status: Pending

## Where This Fits

Recall the four constructor types LearnYard's lecture covered:

```
Default          → Student() { roll = 0; }
Parameterized    → Student(int r) { roll = r; }
Overloaded       → multiple constructors, different parameter counts
Copy             → Student(const Student &s) { ... }
```

**Constructor with default arguments** is really a small variation on the *parameterized* constructor — nothing new mechanically, just the default-parameter idea from the Functions lecture, applied specifically to a constructor. **Initializer lists** are a genuinely different way of *writing* any constructor's initialization step — not a new "type" of constructor at all.

# 🎯Part 1: Constructor with Default Arguments

---

## 1. The Problem It Solves

A plain parameterized constructor **forces** you to supply every argument, every time:

```cpp
class Student {
    int roll;
    float marks;

public:
    Student(int r, float m) {
        roll = r;
        marks = m;
    }
};

int main() {
    Student s1(101, 89.5);   // ✅ fine
    Student s2;                // ❌ ERROR — no matching constructor takes zero arguments
    return 0;
}
```

> Since `Student` no longer has a default (no-parameter) constructor once you write `Student(int r, float m)` yourself, C++ **stops generating the free empty one** it used to give you automatically. `Student s2;` now has nothing to match.
> 

Sometimes you want the best of both: let the caller supply values if they have them, but fall back to sensible defaults if they don't.

## 2. The Fix — Default Arguments on a Constructor

This is exactly the **default parameters** concept from the Functions lecture (`void greet(string name = "Guest")`), applied to a constructor's own parameter list.

```cpp
class Student {
    int roll;
    float marks;

public:
    Student(int r = 0, float m = 0.0) {
        roll = r;
        marks = m;
    }

    void display() {
        cout << "Roll: " << roll << ", Marks: " << marks << endl;
    }
};
```

### Three Ways to Call It

```cpp
int main() {
    Student s1;                // uses BOTH defaults → roll=0, marks=0.0
    Student s2(101);            // supplies roll, marks falls back to default → roll=101, marks=0.0
    Student s3(102, 91.5);      // supplies both → roll=102, marks=91.5

    s1.display();
    s2.display();
    s3.display();
    return 0;
}
```

**Output:**

```
Roll: 0, Marks: 0
Roll: 101, Marks: 0
Roll: 102, Marks: 91.5
```

### Tracing Through It

```
Student s2(101);
   ↓
Compiler sees: 1 argument supplied, constructor expects up to 2
   ↓
r = 101 (from the argument you gave)
m = 0.0 (falls back to its default, since you gave nothing for it)
```

> **Critical rule, carried over from the Functions lecture:** default arguments must come **last** in the parameter list. `Student(int r = 0, float m)` is illegal — once one parameter has a default, every parameter after it must also have one, since C++ fills arguments left to right and can't leave a "gap" in the middle.
> 

## 3. This Single Constructor Now Replaces Several Overloads

Notice this one constructor covers exactly the same ground as writing **three separate overloaded constructors** (Response 1, Part 5 of the main lecture note) would have:

```cpp
// Without default arguments — needs 3 separate overloads:
Student() { roll = 0; marks = 0.0; }
Student(int r) { roll = r; marks = 0.0; }
Student(int r, float m) { roll = r; marks = m; }

// With default arguments — just ONE constructor does all three jobs:
Student(int r = 0, float m = 0.0) { roll = r; marks = m; }
```

> This doesn't replace overloading entirely — you'd still overload when the parameter *types* genuinely differ (like the `int` vs. `double` `sum()` example from the Functions lecture). But when the difference is purely "fewer arguments supplied," default arguments are the cleaner tool.
> 

---

# Part 2: Initializer Lists

## 1. What We've Been Doing So Far — Assignment Inside the Body

Every constructor so far has initialized members like this:

```cpp
Student(int r, float m) {
    roll = r;     // this is ASSIGNMENT — happens inside the body
    marks = m;
}
```

Technically, this isn't quite "initializing" `roll` and `marks` — by the time you reach the `{` of the constructor body, `roll` and `marks` **already exist** in memory (holding garbage values, exactly as with any uninitialized variable — Variables lecture). The lines inside the body then **assign** new values over that garbage. Two separate steps: create-with-garbage, then overwrite.

## 2. The Alternative — Initializing Directly

An **initializer list** lets you set a member's value at the exact moment it's created — skipping the "garbage first, then overwrite" step entirely.

### Syntax

```cpp
ClassName(parameters) : member1(value1), member2(value2) {
    // body — often empty, or just extra logic
}
```

### Worked Example — Rewriting `Student`

```cpp
class Student {
    int roll;
    float marks;

public:
    Student(int r, float m) : roll(r), marks(m) {
        // body can stay empty — the work is already done above
    }

    void display() {
        cout << "Roll: " << roll << ", Marks: " << marks << endl;
    }
};
```

```
Student(int r, float m) : roll(r), marks(m) { }
                  │            │        │
                  │            │        └── marks is initialized directly to m
                  │            └── roll is initialized directly to r
                  └── the colon (:) begins the initializer list
```

> Read the `:` as **"initialize the following members before the body even starts."** Both `roll(r)` and `marks(m)` run **before** the `{ }` body executes — so by the time you're inside the body, `roll` and `marks` are already correctly set.
> 

## 3. Why This Isn't Just a Style Preference — It's Sometimes Required

This is the part the syllabus specifically cares about: there are two situations where **assignment inside the body simply will not compile**, and an initializer list is the *only* way to do it.

### Case 1 — `const` Data Members

```cpp
class Circle {
    const float PI;   // must never change after being set

public:
    Circle() {
        PI = 3.14159;   // ❌ ERROR — cannot assign to a const member
    }
};
```

> Recall from Variables, Character Set & Tokens: `const` means "read-only, once set." A `const` member has no garbage-value phase you can overwrite later — it must receive its one-and-only value at the exact moment it's created.
> 

**The fix:**

```cpp
class Circle {
    const float PI;

public:
    Circle() : PI(3.14159) {   // ✅ set directly at creation — no assignment needed
    }

    void display() {
        cout << "PI = " << PI << endl;
    }
};
```

### Case 2 — Reference Data Members

```cpp
class Wrapper {
    int &ref;   // a reference member

public:
    Wrapper(int &x) {
        ref = x;   // ❌ ERROR — a reference cannot be assigned after creation
    }
};
```

> Recall from Pointers & References: a reference **must be initialized at declaration** and **can never be reassigned** to refer to something else later. Trying to assign to `ref` inside the body doesn't rebind it — it either fails to compile or (if `ref` were already bound) would just modify whatever `ref` already pointed to, not what you intended.
> 

**The fix:**

```cpp
class Wrapper {
    int &ref;

public:
    Wrapper(int &x) : ref(x) {   // ✅ bound directly at creation
    }
};
```

## 4. A Full Worked Example — Combining Both Cases

```cpp
#include <iostream>
using namespace std;

class Circle {
    const float PI;
    int radius;

public:
    Circle(int r) : PI(3.14159), radius(r) {
        // body empty — both members already initialized above
    }

    float area() {
        return PI * radius * radius;
    }
};

int main() {
    Circle c(5);
    cout << "Area = " << c.area();
    return 0;
}
```

**Output:** `Area = 78.5397`

### Tracing Through It

```
Circle c(5);
   ↓
Constructor called with r = 5
   ↓
Initializer list runs FIRST, before the body:
   PI = 3.14159   (directly initialized — legal, since it's not an assignment)
   radius = 5
   ↓
Body runs (empty here)
   ↓
c now exists, fully and correctly initialized
```

## 5. Initializer List Order — A Common Gotcha

> Members are initialized in the **order they're declared inside the class** — **not** the order they appear in the initializer list. If these two orders don't match, some compilers will warn you, because it can silently produce wrong results if one member's initialization depends on another's.
> 

```cpp
class Example {
    int a;
    int b;

public:
    // written as b, a here — but a is STILL initialized first,
    // because 'a' is declared before 'b' in the class body
    Example(int x) : b(x), a(b) {
        // DANGER: a(b) runs before b is actually set — a gets garbage!
    }
};
```

> **Rule to lock in:** always write your initializer list in the **same order** the members are declared in the class, to avoid this trap entirely.
> 

---

## Recap —

```
┌──────────────────────────────────────────────────────────────────────┐
│      CONSTRUCTOR WITH DEFAULT ARGUMENTS + INITIALIZER LISTS          │
├──────────────────────────────────────────────────────────────────────┤
│  DEFAULT ARGUMENTS ON A CONSTRUCTOR                                  │
│    • Same idea as default parameters (Functions lecture),            │
│      applied to a constructor's parameter list                       │
│    • Student(int r = 0, float m = 0.0)                               │
│    • Defaults must come LAST in the parameter list                   │
│    • One constructor can replace several overloads that only         │
│      differ by "how many arguments were supplied"                    │
│                                                                      │
│  INITIALIZER LISTS                                                   │
│    • ClassName(params) : member1(val1), member2(val2) { }            │
│    • Initializes members directly — skips the                        │
│      "garbage-then-assign" step that a plain constructor body        │
│      does                                                            │
│    • REQUIRED (not just style) for:                                  │
│        - const data members (can't be assigned after creation)       │
│        - reference data members (can't be reassigned, ever)          │
│    • Members initialize in DECLARATION order, regardless of          │
│      the order written in the list — mismatches are a classic        │
│      bug source                                                      │
└──────────────────────────────────────────────────────────────────────┘
```