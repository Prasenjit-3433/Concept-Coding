# Lecture 1: Constructors, Destructors `this` Pointer & Friend Function

Status: In Progress

## Where This Fits

Until now, every class we've built (Lecture on Class Objects & Access Specifiers) needed its data filled in manually, one line at a time:

```cpp
Product P;
P.name = "iPhone";
P.price = 150000;
```

This works, but it means an Object can exist in an **incomplete, half-set-up state** right after it's created — nothing stops you from forgetting to set `price`. A **constructor** fixes exactly this: it lets you guarantee that the moment an Object is created, it's already filled in correctly.

```
┌─────────────────────────────────────────────────┐
│   **WITHOUT a constructor**                     │
│   Product P;        ← P exists, but empty       │
│   P.name = "...";   ← you must remember*        │
│   P.price = ...;      to fill every field       │
├─────────────────────────────────────────────────┤
│   **WITH a constructor**                        │
│   Product P("iPhone", 150000);                  │
│   ← P is created AND fully set up,              │
│     in one guaranteed step                      │
└─────────────────────────────────────────────────┘
```

# 🎯Part 1: The Constructor

---

## 1. Constructors Are Functions — With Three Special Rules

Recall a normal function needs a **return type**, a **name**, **parameters**, and a **body** (Functions lecture). A constructor is still a function — but it bends three of those rules:

| Normal Function | Constructor |
| --- | --- |
| Can have any name | Name **must** match the class name exactly |
| Needs a return type (`int`, `void`, ...) | Has **no return type at all** — not even `void` |
| You must call it yourself | Called **automatically**, the moment an Object is created |

> A constructor is a special member function of a class, automatically called when an Object of that class is created, used to initialize the Object's data members.
> 

### The Three Rules, Visually

```
┌─────────────────────────────────────────────────────┐
│              **CONSTRUCTOR RULES**                  │
│                                                     │
│  1. Only exists inside a class                      │
│  2. Name == Class name (exactly)                    │
│  3. No return type — not even void                  │
│  4. Never called manually — auto-runs on            │
│     object creation                                 │
└─────────────────────────────────────────────────────┘
```

### Syntax

```cpp
class ClassName {
public:
    ClassName() {
        // initialization code
    }
};
```

---

## 2. A First Worked Example — The `Product` Class

Picture an iPhone factory: every `Product` made there should automatically belong to `"Apple"`, without anyone setting it by hand.

```cpp
#include <iostream>
#include <string>
using namespace std;

class Product {
public:
    string company;

    Product() {                  // constructor — same name as the class
        company = "Apple";        // runs automatically for every object made
    }
};

int main() {
    Product iPhone;                // the moment this line runs, the constructor fires
    cout << iPhone.company;         // Output: Apple

    return 0;
}
```

### Tracing Through It

```
Product iPhone;
   ↓
Object "iPhone" is being created
   ↓
Because it's being created, the constructor Product() runs AUTOMATICALLY
   ↓
Inside the constructor: company = "Apple"
   ↓
iPhone now exists, already holding company = "Apple"
   ↓
cout << iPhone.company;   →   Apple
```

> Worth sitting with this: you never wrote `iPhone.Product()` anywhere. The constructor call is invisible — it's triggered purely by the act of creating the Object, `Product iPhone;`.
> 

### A Hidden Detail Worth Knowing

Every class actually comes with a constructor whether you write one or not — if you don't define any constructor, C++ silently generates an empty one for you. This is exactly *why* `Product P;` was even legal before we ever wrote a constructor at all — some constructor was always running behind the scenes; we just hadn't customized it yet.

---

## 3. The Default Constructor

> A **default constructor** is a constructor that takes **no parameters**.
> 

The `Product()` constructor above is a default constructor — it doesn't accept any input; it just runs the same setup steps every single time.

### Worked Example — `Student`

```cpp
class Student {
    int roll;

public:
    Student() {
        roll = 0;      // every Student starts with roll = 0
    }

    void display() {
        cout << roll << endl;
    }
};
```

```cpp
int main() {
    Student s;
    s.display();   // Output: 0
    return 0;
}
```

> Notice `roll` is declared **before** `public:` — meaning it's private (Class Objects & Access Specifiers lecture: a class's members are private by default). The constructor, being a `public` member function, is still allowed to touch it directly, since a class's own functions always have access to its own private data.
> 

---

## 4. The Parameterized Constructor

A default constructor always does the *exact same* setup. But usually, you want each Object to start with **different** values — that's what a **parameterized constructor** is for.

> A parameterized constructor is a constructor that accepts one or more parameters, letting you initialize an Object with values supplied at the moment it's created.
> 

### Worked Example — `Student`, Taking a Roll Number

```cpp
class Student {
    int roll;

public:
    Student(int r) {
        roll = r;
    }

    void display() {
        cout << roll << endl;
    }
};

int main() {
    Student s(101);     // 101 is passed straight into the constructor
    s.display();          // Output: 101
    return 0;
}
```

### Tracing Through It

```
Student s(101);
   ↓
Object "s" is being created, with the argument 101
   ↓
Constructor Student(int r) runs automatically, r = 101
   ↓
Inside constructor: roll = r → roll = 101
   ↓
s now exists, already holding roll = 101
```

### A Second Worked Example — `Employee` (Two Parameters)

```cpp
class Employee {
public:
    string name;
    int age;

    Employee(string n, int a) {
        name = n;
        age = a;
    }

    void getInfo() {
        cout << "Name: " << name << endl;
        cout << "Age: " << age << endl;
    }
};

int main() {
    Employee e1("Sachin", 33);
    e1.getInfo();
    return 0;
}
```

**Output:**

```
Name: Sachin
Age: 33
```

> Notice the parameters are named `n` and `a` — **not** `name` and `age`. This is deliberate: if the parameter and the data member shared the exact same name, writing `name = name;` inside the constructor would just assign the parameter to itself, and the actual data member would never get touched. We'll fix this naming headache properly with the **`this` pointer** in Part 2 — for now, just use distinct short names for parameters, exactly as done here.
> 

---

## 5. Constructor Overloading

Recall **function overloading** (Functions lecture): multiple functions sharing one name, distinguished by their parameter lists. Since a constructor is just a function, **you can overload constructors too** — the compiler picks the right one based on how many (and what type of) arguments you pass in.

> Constructor overloading: writing multiple constructors in the same class, each with a different parameter list, so the compiler automatically calls the correct one based on the number/type of arguments supplied when the Object is created.
> 

### Worked Example — `Sum`

```cpp
class Sum {
public:
    Sum(int a, int b) {
        cout << a + b;
    }

    Sum(int a, int b, int c) {
        cout << a + b + c;
    }

    Sum(int a, int b, int c, int d) {
        cout << a + b + c + d;
    }
};

int main() {
    Sum s1(2, 3, 6);   // three arguments → matches the 3-parameter constructor
    return 0;
}
```

**Output:** `11`

### How the Compiler Chooses

```
Sum s1(2, 3, 6);
        │
        ▼
  How many arguments were passed? → 3
        │
        ▼
  Matches: Sum(int a, int b, int c)   ← this one runs
  Skips:   Sum(int a, int b)
  Skips:   Sum(int a, int b, int c, int d)
```

> Exactly the same matching rule as ordinary function overloading (Functions lecture): the compiler looks at what you actually passed in — how many arguments, and of what type — and picks the single constructor whose parameter list matches.
> 

---

## 6. Defining a Constructor Outside the Class

Just like ordinary member functions can be **declared** inside a class but **defined** outside it using the **Scope Resolution Operator (`::`)** (Class Objects & Access Specifiers lecture), the same applies to constructors.

### Syntax

```cpp
ClassName::ClassName(parameters) {
    // constructor body
}
```

### Worked Example — `Car`

```cpp
#include <iostream>
#include <string>
using namespace std;

class Car {
public:
    string brand;
    int price;

    Car(string b, int p);   // declaration only — no body here
};

Car::Car(string b, int p) {   // definition, outside the class
    brand = b;
    price = p;
}

int main() {
    Car c1("Toyota", 2500000);
    cout << c1.brand << " - " << c1.price;
    return 0;
}
```

**Output:** `Toyota - 2500000`

> Read `Car::Car(string b, int p)` as: *"the constructor belonging to the `Car` class, which takes a string and an int."* Notice the class name appears **twice** here — once before `::` (which class this belongs to), and once after (because the constructor's name must equal the class name). This double-appearance is unique to constructors; an ordinary member function like `area()` only needs `Rectangle::area()`, not `Rectangle::Rectangle()`.
> 

---

## Recap So Far

```
┌───────────────────────────────────────────────────────────────┐
│                    **CONSTRUCTOR BASICS**                     │
├───────────────────────────────────────────────────────────────┤
│  Same name as class │ No return type │ Auto-called            │
│                                                               │
│  Default constructor        → no parameters                   │
│  Parameterized constructor  → takes input, sets values        │
│  Constructor overloading    → multiple constructors,          │
│                                different parameter counts     │
│  Defined outside class      → ClassName::ClassName(...)       │
└───────────────────────────────────────────────────────────────┘
```

# 🎯Part 2: The `this` Pointer

---

## 1. The Problem It Solves — Revisiting the Naming Clash

In Response 1's `Employee` example, we deliberately used short parameter names (`n`, `a`) to avoid a clash with the data members (`name`, `age`). Let's see exactly what goes wrong if we *don't* do that.

```cpp
class Employee {
public:
    string name;

    Employee(string name) {   // parameter is ALSO called 'name'
        name = name;            // ❌ does NOT do what you'd expect
    }
};
```

> Inside the constructor, there are now **two different things** both called `name`: the class's data member, and the function's own parameter. C++ resolves this using **scope rules** (Variables, Character Set & Tokens lecture — this is exactly **variable shadowing**): the *closest* `name` wins. So `name = name;` just assigns the parameter to itself — the data member is never touched at all.
> 

```
┌───────────────────────────────────────────────────┐
│         WHY 'name = name;' FAILS                  │
│                                                   │
│  Employee(string name) {                          │
│      name = name;   ← both refer to the           │
│                        PARAMETER — the closest    │
│                        'name' in scope wins       │
│  }                                                │
│  The data member 'name' is never assigned!        │
└───────────────────────────────────────────────────┘
```

---

## 2. What Is the `this` Pointer?

> `this` is a special, automatically-available pointer that exists inside **every** non-static member function of a class. It always points to the **current Object** — the specific Object the function was called on.
> 

Think back to Pointers & References: a pointer stores an address. `this` is exactly that — a pointer, made available to you for free — holding the address of whichever Object is currently "running" the function.

```
┌──────────────────────────────────────────────────────────┐
│                THE **'this'** POINTER                    │
│                                                          │
│   e1.setName("Sachin");                                  │
│           │                                              │
│           ▼                                              │
│   Inside setName(), 'this' points to e1                  │
│                                                          │
│   e2.setName("Faraz");                                   │
│           │                                              │
│           ▼                                              │
│   Inside setName(), 'this' points to e2 instead          │
└──────────────────────────────────────────────────────────┘
```

> Every time you call a member function on a particular Object, C++ secretly passes that Object's address into the function — and `this` is what holds it, automatically, without you writing anything extra.
> 

---

## 3. Fixing the Naming Clash With `this`

Since `this` points at the current Object, `this->name` unambiguously means **"the data member `name` belonging to the current Object"** — completely separate from the plain `name`, which still refers to the parameter.

```cpp
class Employee {
public:
    string name;
    int age;

    Employee(string name, int age) {
        this->name = name;   // this->name = data member, name = parameter
        this->age = age;
    }

    void getInfo() {
        cout << "Name: " << name << endl;
        cout << "Age: " << age << endl;
    }
};

int main() {
    Employee e1("Sachin", 33);
    e1.getInfo();
    return 0;
}
```

**Output:**

```
Name: Sachin
Age: 33
```

> Read `this->name = name;` as: *"take the data member `name` belonging to whichever Object this constructor was called on, and set it equal to the parameter `name`."* The `->` here is the **exact same arrow operator** used for structure/class pointers (Advanced Pointers lecture) — because `this`, underneath, really is just a pointer to the current Object.
> 

Now that you have `this`, you can safely reuse the *same, readable* names for parameters and data members — no need for the awkward `n`, `a` shorthand from Response 1 anymore.

---

## 4. `this` Behaves Like Any Other Pointer

Since `this` genuinely is a pointer (of type `ClassName*`), everything from the Pointers lecture applies:

```cpp
cout << this;          // prints the ADDRESS of the current Object
cout << this->name;     // arrow operator, exactly like any Object pointer
```

The one restriction: `this` **cannot be reassigned**. Unlike a normal pointer, it always refers to whichever Object the currently-running member function was called on, for that entire call — you never point it somewhere else.

---

## Recap — `this` Pointer

```
┌───────────────────────────────────────────────────────────────┐
│                    THE **'this'** POINTER                     │
├───────────────────────────────────────────────────────────────┤
│  Automatically available inside every member function         │
│  Points to the current Object (the one the function           │
│  was called on)                                               │
│  Solves the parameter-vs-data-member naming clash:            │
│     this->name     → data member                              │
│     name           → parameter                                │
│  Cannot be reassigned                                         │
└───────────────────────────────────────────────────────────────┘
```

# 🎯Part 3: The Copy Constructor

---

## 1. The Problem — Copying One Object's Data Into Another

Suppose you've built an Object `C1`, filled it with data, and now you want a **second** Object, `C2`, that starts out with exactly the same data — without typing it all in again.

> A copy constructor is used to initialize a new Object using an already-existing Object of the same class.
> 

## 2. The Default Copy Constructor — You Get One for Free

Just like C++ silently generates a default constructor if you don't write one, it also silently generates a **default copy constructor**. It copies every data member's value, one by one, into the new Object.

### Worked Example

```cpp
#include <iostream>
using namespace std;

class MyCopy {
public:
    int a, b;

    MyCopy(int a, int b) {
        this->a = a;
        this->b = b;
    }

    void getInfo() {
        cout << "A = " << a << ", B = " << b << endl;
    }
};

int main() {
    MyCopy C1(10, 20);
    C1.getInfo();          // A = 10, B = 20

    MyCopy C2 = C1;         // default copy constructor runs here
    C2.getInfo();           // A = 10, B = 20 — copied!

    return 0;
}
```

### Tracing Through It

```
MyCopy C2 = C1;
   ↓
C2 is being created FROM an existing Object, C1
   ↓
The default copy constructor runs automatically
   ↓
Every data member of C1 gets copied into C2, one by one:
   C2.a = C1.a   (10)
   C2.b = C1.b   (20)
```

> This is a completely different situation from the constructors we saw in Part 1 — there, an Object was built from scratch using fresh values. Here, an Object is built **from another Object**, by copying its existing data.
> 

---

## 3. Writing Your Own Custom Copy Constructor

You're not stuck with the default behavior — you can write your own copy constructor explicitly, if you want to customize exactly what "copying" means.

### Syntax

```cpp
ClassName(const ClassName &obj);
```

> Break this down: `ClassName` — the function name matches the class, same rule as any constructor. `const ClassName &obj` — the single parameter is a **reference** (Pointers & References lecture) to another Object of the same class, marked `const` so the copy constructor can't accidentally modify the Object it's copying *from*.
> 

### Worked Example

```cpp
class MyCopy {
public:
    int a, b;

    MyCopy(int a, int b) {
        this->a = a;
        this->b = b;
    }

    // Custom copy constructor
    MyCopy(const MyCopy &C1) {
        this->a = C1.a;
        this->b = C1.b;
        cout << "Custom copy constructor called" << endl;
    }

    void getInfo() {
        cout << "A = " << a << ", B = " << b << endl;
    }
};

int main() {
    MyCopy C1(10, 20);
    MyCopy C2 = C1;      // custom copy constructor runs here

    C2.getInfo();
    return 0;
}
```

**Output:**

```
Custom copy constructor called
A = 10, B = 20
```

> `C1` here is just the **parameter's name** — you could call it anything. Read `this->a = C1.a;` as: *"set the current Object's (C2's) `a` equal to the Object being copied from's (C1's) `a`."*
> 

---

## 4. The Catch — This Works Fine for Plain Values, But Not for Pointers

Everything above works perfectly when a class only holds plain values (`int`, `float`, `string`...). The moment a class holds a **pointer** to dynamically allocated memory (Dynamic Memory Allocation lecture), copying gets dangerous — and this is exactly what the next section is about.

---

## Recap — Copy Constructor

```
┌───────────────────────────────────────────────────────────────┐
│                    COPY CONSTRUCTOR                           │
├───────────────────────────────────────────────────────────────┤
│  Initializes a NEW Object using an EXISTING one               │
│  You get a DEFAULT one for free — copies member-by-member     │
│  You can write a CUSTOM one:                                  │
│     ClassName(const ClassName &obj) { ... }                   │
│  Called when:                                                 │
│     - an Object is initialized from another Object            │
│       (MyCopy C2 = C1;)                                       │
│     - an Object is passed by value into a function            │
│     - an Object is returned by value from a function          │
│  Works safely for plain data — pointers are where it          │
│  gets dangerous (next: Shallow vs. Deep Copy)                 │
└───────────────────────────────────────────────────────────────┘
```

# 🎯Part 4: Shallow Copy vs. Deep Copy

---

## 1. Where the Problem Actually Comes From

Recall the default copy constructor copies each data member **one by one, exactly as it's stored**. For plain values (`int`, `float`), that's perfectly safe — each Object ends up with its own independent number.

But if a data member is a **pointer**, "copying it exactly as it's stored" means copying the **address** it holds — not the data sitting at that address.

> **Shallow copy**: copying a pointer member's value (its address) directly, rather than the data it points to. Both the original and the copy end up pointing at the **same** memory location.
> 

## 2. Watching the Problem Happen

```cpp
class Test {
    int *ptr;

public:
    Test(int x) {
        ptr = new int(x);   // dynamically allocated memory
    }

    void show() {
        cout << *ptr << endl;
    }

    void setValue(int x) {
        *ptr = x;
    }

    // no custom copy constructor written —
    // the default (shallow) one is what runs
};

int main() {
    Test t1(10);
    Test t2 = t1;        // shallow copy: t2.ptr and t1.ptr now hold the SAME address

    t2.setValue(99);      // change made through t2's pointer
    t1.show();              // Output: 99  ← t1 changed too, even though we never touched t1!

    return 0;
}
```

### Why This Happens — Visually

```
BEFORE the copy:
   t1.ptr ──────► [ 10 ]   (heap, address 5000)

AFTER  Test t2 = t1;   (shallow copy)
   t1.ptr ──────┐
                 ├────►   [ 10 ]   (still just ONE block, address 5000)
   t2.ptr ──────┘

Change through t2:  *t2.ptr = 99;
                        ▼
   t1.ptr ──────┐
                 ├────►   [ 99 ]   ← BOTH pointers see the change —
   t2.ptr ──────┘           they were always pointing at the same memory
```

> This is precisely why the topic matters: `t1` and `t2` **look** like two separate, independent Objects — but their pointer members are secretly pointing at one shared block of heap memory. Change one, and the other appears to change too.
> 

## 3. A Second, Worse Danger — Double Deletion

Recall from Dynamic Memory Allocation: deleting the same heap memory twice is a serious error, and a pointer left pointing at freed memory is a **dangling pointer**.

```cpp
Test t1(10);
Test t2 = t1;    // shallow copy — t1.ptr and t2.ptr share one address

// ... later, both t1 and t2 go out of scope ...
// t1's destructor runs: delete ptr;   → memory freed
// t2's destructor runs: delete ptr;   → ❌ deleting the SAME memory again!
```

> Since both `t1.ptr` and `t2.ptr` hold the identical address, when each Object's destructor eventually runs `delete ptr;`, the **same** block gets deleted twice — undefined behavior, and a classic interview-trap bug.
> 

---

## 4. The Fix — Deep Copy

> **Deep copy**: instead of copying a pointer's address, allocate a **brand-new** block of memory for the copy, and copy the actual **value** into it. The original and the copy now point at two completely separate memory locations.
> 

### The Fixed `Complex` Example (from the instructor's notes)

```cpp
#include <iostream>
using namespace std;

class Complex {
    int *real;
    int *imag;

public:
    // Parameterized Constructor
    Complex(int r, int i) {
        real = new int(r);
        imag = new int(i);
    }

    // Copy Constructor (Deep Copy)
    Complex(const Complex &c) {
        real = new int(*c.real);   // NEW memory, value copied in
        imag = new int(*c.imag);   // NEW memory, value copied in
    }

    void display() {
        cout << *real << " + " << *imag << "i" << endl;
    }

    ~Complex() {
        delete real;
        delete imag;
    }
};

int main() {
    Complex c1(3, 4);
    Complex c2 = c1;   // deep copy — c2 gets its OWN memory

    c1.display();   // 3 + 4i
    c2.display();   // 3 + 4i

    return 0;
}
```

### Why `real = new int(*c.real);` Is the Fix

```
real = new int(*c.real);
         │           │
         │           └── dereference c's real pointer →
         │                get the VALUE sitting there
         └── allocate a BRAND NEW block of memory,
             and put that value inside it
```

```
AFTER a DEEP copy:
   c1.real ──────►  [ 3 ]   (address 5000)
   c2.real ──────►  [ 3 ]   (address 7000 — a totally separate block)

Change through c2 now:  *c2.real = 99;
   c1.real ──────►  [ 3 ]    ← unaffected!
   c2.real ──────►  [ 99 ]   ← only c2 changes
```

> Each Object's `real` and `imag` now live at their own independent heap addresses. Modifying one Object's data — or deleting one Object — has zero effect on the other. This also fixes the double-deletion danger: each Object's destructor deletes its **own**, separately-allocated memory.
> 

---

## 5. Shallow vs. Deep Copy — Side by Side

| Feature | Shallow Copy | Deep Copy |
| --- | --- | --- |
| What gets copied | The pointer's **address** | The **value** at that address, into fresh memory |
| Result | Both Objects share **one** memory block | Each Object has its **own**, independent memory block |
| Changing one Object's data | Affects the other Object too | Only affects that one Object |
| Deleting one Object | Leaves the other with a **dangling pointer** | Safe — each Object deletes its own memory |
| Who provides it | C++'s **default** copy constructor | You must write a **custom** copy constructor |
| Safe for | Plain data members (`int`, `float`, `string`) | Classes containing **pointer** members |

> **The rule to lock in:** the moment a class holds a pointer to dynamically allocated memory, the compiler's free default copy constructor becomes dangerous. Write your own copy constructor, and make sure it allocates fresh memory rather than copying the address.
> 

---

# Part 5: The Destructor

## 1. What Is a Destructor?

We've seen the constructor build an Object up. The **destructor** is its mirror image — it tears the Object down.

> A destructor is a special member function, automatically called when an Object **goes out of scope**, used to release memory and clean up resources the Object was using.
> 

### Syntax

The destructor's name is the class name too — but prefixed with a **tilde (`~`)**:

```cpp
class ClassName {
public:
    ~ClassName() {
        // cleanup code
    }
};
```

```
┌──────────────────────────────────────────────────────┐
│          **CONSTRUCTOR vs. DESTRUCTOR**              │
│                                                      │
│   ClassName()    ← builds the Object up              │
│                     runs when Object is CREATED      │
│                                                      │
│   ~ClassName()   ← tears the Object down             │
│                     runs when Object goes            │
│                     OUT OF SCOPE                     │
└──────────────────────────────────────────────────────┘
```

## 2. Worked Example

```cpp
class Demo {
public:
    Demo() {
        cout << "Constructor\n";
    }

    ~Demo() {
        cout << "Destructor\n";
    }
};

int main() {
    Demo d;
    cout << "Inside main\n";
    return 0;
}
```

**Output:**

```
Constructor
Inside main
Destructor
```

> Notice: `d` was never explicitly deleted — the destructor still ran, **automatically**, the moment `main()` finished and `d` went out of scope. This is exactly the same automatic-stack-cleanup idea from the Static vs. Dynamic Memory lecture: statically allocated Objects clean themselves up when their scope ends.
> 

## 3. Why the Destructor Matters More for Pointer Members

Recall from Static vs. Dynamic Memory Allocation: plain stack variables are freed automatically, but memory allocated with `new` is **not** — you must `delete` it yourself, or it becomes a **memory leak**.

```cpp
class Test {
    int *ptr;

public:
    Test(int x) {
        ptr = new int(x);
    }

    ~Test() {
        delete ptr;   // manually free the dynamically allocated memory
    }
};
```

> When `Test`'s destructor runs, the Object's ordinary members (if any) get cleaned up automatically. But `ptr` itself — the pointer variable — being cleaned up does **not** free the heap memory it points to. You must explicitly `delete ptr;` inside the destructor, or that memory stays reserved and unusable for the rest of the program's run.
> 

## 4. Rules to Remember

- No parameters, no return type — same as a constructor in that respect, but a destructor additionally **cannot be overloaded** (a class can only ever have exactly one destructor, since it's always called with no arguments).
- Called automatically — you never write `d.~Demo();` yourself in normal code.
- Any class managing dynamically allocated memory (via `new`) **must** define a destructor that calls `delete`, or it will leak memory every time an Object of that class is created and destroyed.

---

# Part 6: The Friend Function

*(Note: this isn't a Unit III syllabus item — it's supplementary content LearnYard taught alongside constructors/destructors. Filing it here for completeness, since it directly follows in the same lecture.)*

## 1. The Problem It Solves

We know `private` members can normally only be touched from **inside** the class (Class Objects & Access Specifiers lecture) — usually via public getter/setter functions. A **friend function** is a deliberate, explicit exception to that rule.

> A friend function is a function that is **not** a member of the class, but is still granted permission to access the class's `private` (and `protected`) members directly.
> 

## 2. Syntax

Declare it **inside** the class, prefixed with the `friend` keyword — but define it **outside**, like any ordinary standalone function (no `ClassName::` needed, since it was never a member in the first place):

```cpp
class ClassName {
    friend void functionName(ClassName obj);
};
```

## 3. Worked Example — `Box`

```cpp
#include <iostream>
using namespace std;

class Box {
    int length;   // private by default

public:
    Box(int l) {
        length = l;
    }

    friend void showLength(Box b);   // declared inside, marked as a friend
};

// defined OUTSIDE — notice: no Box:: prefix, it was never a member
void showLength(Box b) {
    cout << b.length;   // accessing a PRIVATE member — allowed, because of 'friend'
}

int main() {
    Box b(10);
    showLength(b);   // Output: 10
    return 0;
}
```

> Without the `friend` declaration, `b.length` inside `showLength()` would fail to compile — `length` is private, and `showLength` is an ordinary outside function with no special access. The `friend` keyword is what grants the exception.
> 

## 4. Key Points

- A friend function is declared inside the class (so the class explicitly "trusts" it), but its **definition lives outside**, and it is **not** called using the dot operator — it's called like any normal function: `showLength(b)`, not `b.showLength()`.
- It's genuinely a niche feature — use it sparingly, since it breaks encapsulation's whole point (Class Objects & Access Specifiers lecture) on purpose, for specific cases where two things need tight cooperation.

---

## Final Recap — Full Lecture

```
┌────────────────────────────────────────────────────────────────────────────┐
│         CONSTRUCTOR, DESTRUCTOR, THIS, FRIEND — FULL MAP                   │
├────────────────────────────────────────────────────────────────────────────┤
│  **CONSTRUCTOR**                                                           │
│    • Same name as class, no return type, auto-called                       │
│    • Default (no params) / Parameterized / Overloaded                      │
│    • Can be defined outside class: ClassName::ClassName(...)               │
│                                                                            │
│  **THIS POINTER**                                                          │
│    • Auto-available inside every member function                           │
│    • Points to the current Object                                          │
│    • this->member = data member ; member = parameter                       │
│                                                                            │
│  **COPY CONSTRUCTOR**                                                      │
│    • ClassName(const ClassName &obj)                                       │
│    • Default: shallow copy (copies pointer addresses — unsafe              │
│      for pointer members)                                                  │
│    • Custom: write your own to do a DEEP copy (new memory +                │
│      copy the value) — required whenever a class holds pointers            │
│      to dynamically allocated data                                         │
│                                                                            │
│  **DESTRUCTOR**                                                            │
│    • ~ClassName(), no params, no return type, never overloaded             │
│    • Auto-called when Object goes out of scope                             │
│    • Must manually 'delete' any pointer members allocated                  │
│      with 'new', or you get a memory leak                                  │
│                                                                            │
│  **FRIEND FUNCTION** (supplementary — not a Unit III syllabus item)        │
│    • Non-member function granted access to private members                 │
│    • Declared inside class with 'friend', defined outside                  │
│      normally, called normally (no dot operator)                           │
└────────────────────────────────────────────────────────────────────────────┘
```