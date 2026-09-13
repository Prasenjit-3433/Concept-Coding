# Lecture 4: Class Objects & Access Specifiers

Status: Pending

# 🎯Part 1 — The Object-Oriented Programming Overview

---

## 1. The Four Pillars of OOP

Before touching any code, it helps to see the entire roadmap of what "Object-Oriented Programming" (OOP) actually means. OOP is built on **four pillars**:

```
┌─────────────────────────────────────────────────────────────────┐
│                   **OBJECT-ORIENTED PROGRAMMING**               │
├───────────────┬───────────────┬───────────────┬─────────────────┤
│ Encapsulation │  Abstraction  │  Inheritance  │ Polymorphism    │
└───────────────┴───────────────┴───────────────┴─────────────────┘
```

This lecture focuses on **Encapsulation** (and lays the groundwork with Classes and Objects, which everything else in OOP builds on). Abstraction, Inheritance, and Polymorphism each get their own dedicated lectures later.

---

## 2. Why Do We Need Object-Oriented Programming?

> With the help of Object-Oriented Programming, code becomes **better organized, reusable, and secure** — because OOP is built around the concept of **Classes and Objects**.
> 

### The Problem — Storing Data the "Old" Way

Suppose you're asked to store the data of two students:

```cpp
string student1Name = "Alice";
int student1Age = 14;
float student1Marks = 89.5;

string student2Name = "Bob";
int student2Age = 15;
float student2Marks = 76;
```

This *works* — but think about what happens if you need to store data for **100 students**. You'd end up writing roughly **300 separate lines**, and accessing any one student (say, student #39) means digging through variable names like `student39Name`.

> This is a bad way to write code. The data becomes cluttered, hard to access, and doesn't scale.
> 

### The OOP Solution — Group Data Into Objects

Instead of scattering each student's data across many separate variables, OOP groups all the data belonging to **one entity** into a **single packet** — this packet is called an **Object**.

```
┌─────────────────────────┐     ┌─────────────────────────┐
│      OBJECT: Student1   │     │      OBJECT: Student2   │
│  name:  Alice           │     │  name:  Bob             │
│  age:   14              │     │  age:   15              │
│  marks: 89.5            │     │  marks: 76              │
└─────────────────────────┘     └─────────────────────────┘
```

> By creating Objects, data is kept in a far more **organized** format — each real-world entity (each student) is represented as a single, self-contained unit, instead of being scattered across many disconnected variables.
> 

### Old Way vs. OOP Way

| Feature | Scattered Variables (Old Way) | Objects (OOP Way) |
| --- | --- | --- |
| Structure | None — just loose variables | Proper, defined structure |
| Maintainability | Hard to maintain | Easy to maintain |
| Real-world mapping | None | Matches real life — one entity, one Object |

> A student in the real world is a single entity. OOP mirrors this directly by enclosing all of a student's data inside a single Object.
> 

---

## 3. What Is a Class? — The "Blueprint" Analogy

To understand *how* Objects get created, we need to understand **Classes** first. The instructor's analogy for this is genuinely worth internalizing:

> Imagine God wants to create humans, but can't create every human individually by hand. So He prepares a **paper** — a blueprint — describing what every human will look like: it will have a name, an age, a gender, and it will be able to talk and run.
> 

This blueprint is called a **Class**. Using it, factory workers can create as many actual humans (**Objects**) as needed — each one following the same blueprint, but holding its own individual data.

```
┌─────────────────────────────────────────────────┐
│              CLASS: Human (blueprint)           │
│  Attributes: name, age, gender                  │
│  Functions:  talk(), run()                      │
└─────────────────────────────────────────────────┘
                    │
      ┌─────────────┴─────────────┐
      ▼                           ▼
┌────────────────┐         ┌───────────────┐
│ OBJECT: H1     │         │ OBJECT: H2    │
│ name: Sachin   │         │ name: Faraz   │
│ age: 20        │         │ age: 26       │
│ gender: Male   │         │ gender: Male  │
└────────────────┘         └───────────────┘
```

### The Two Things Every Class Contains

> A Class's blueprint contains exactly two kinds of things:
> 
> 1. **Normal variables** — these become the Object's **attributes** (e.g., name, age, gender)
> 2. **Functions** — these become the Object's **methods** (e.g., talk(), run())

### Key Ideas to Lock In

- A **Class is imaginary** — it's just a description written down; it doesn't exist as a real thing.
- An **Object is real** — it's an actual entity built according to the Class's blueprint.
- A **Class does not occupy memory**. Only **Objects occupy memory**, because Objects hold actual data.
- **Variables inside a Class → attributes.** **Functions inside a Class → methods** (methods and functions mean the same thing — a common interview point).

> Whatever attributes and methods exist inside a Class, every Object created from that Class automatically gets them too.
> 

---

## 4. A Real-World Example: Amazon Products

To make this concrete, think about Amazon's product listing page. Every product shown (a PC, a pen, a phone) is an **Object**.

```
Object 1: PC          Object 2: PC          Object 3: Pen
```

Behind the scenes, Amazon's developers would have created a **Class** named `Product`, describing what every product needs:

**Attributes (data members):**

- Image
- Name
- Rating
- Price

**Functions (methods):**

- Add to Cart
- Buy Now

```
┌──────────────────────────────────────┐
│         CLASS: Product               │
│  Attributes: image, name,            │
│              rating, price           │
│  Functions:  addToCart(),            │
│              buyNow()                │
└──────────────────────────────────────┘
              │
   ┌──────────┼──────────┐
   ▼          ▼          ▼
Product 1  Product 2  Product 3
 (Object)   (Object)   (Object)
```

> Every product listed on the page is a separate **Object**, all built from the same `Product` **Class** — this is exactly how Object-Oriented Programming gets used in the real world.
> 

---

## 5. Class and Object — Formal Definitions

> **Class**: acts as a blueprint for creating Objects. It contains **data members** (variables) and **member functions** (methods). A Class does **not** occupy memory until Objects are actually created from it.
> 

> **Object**: an **instance** of a Class. Just as you are an instance of your parents, an Object is an instance of a Class — it's created *from* the Class, and it's what actually occupies memory.
> 

---

## 6. Syntax for Creating a Class and an Object

```cpp
class ClassName {
    // data members (attributes)
    // member functions (methods)
};
```

> Don't forget the semicolon after the closing curly brace of a class — the same rule we saw with `struct` in the previous lecture.
> 

The Class itself is written **outside** `main()` (it's a blueprint, so it needs to exist before it's used), while the **Object** is created **inside** `main()`.

### Worked Example — The `Product` Class

```cpp
#include <iostream>
#include <string>
using namespace std;

class Product {
public:
    string name;
    float price;
    float rating;

    void showProduct() {
        cout << "Name: " << name << endl;
        cout << "Price: " << price << endl;
        cout << "Rating: " << rating << endl;
    }
};

int main() {
    Product P;              // creating an Object of the Product class
    P.name = "iPhone";
    P.price = 150000;
    P.rating = 4.5;

    P.showProduct();        // calling the function through the Object

    return 0;
}
```

> Just like with a `struct` (from the previous lecture), we use the **dot operator (`.`)** to set or access a Class Object's data members and to call its functions.
> 

**Output:**

```
Name: iPhone
Price: 150000
Rating: 4.5
```

---

## 7. The "Private by Default" Surprise

If you tried the code above **without** writing `public:` at the top, you'd get a wall of compiler errors like:

```
'name' is private within this context
'price' is private within this context
'rating' is private within this context
```

> **Key rule:** Inside a `class`, if you don't explicitly write `public` or `private`, everything is **private by default**. This is exactly opposite to a `struct`, where everything is **public by default** (as covered in the previous lecture on Structures).
> 

This "private-by-default" behavior is *why* we need to properly understand **Access Specifiers** — which is exactly what fixes the error above.

---

## 8. Access Specifiers

> An **Access Specifier** controls **which parts of the program are allowed to access** a Class's members (its attributes and functions).
> 

There are **three types**:

| Access Specifier | Accessibility |
| --- | --- |
| `private` | Accessible **only inside the Class** |
| `public` | Accessible **from anywhere** |
| `protected` | Accessible inside the Class **and its derived (child) classes** |

> `protected` will make full sense once we study **Inheritance** — it exists specifically for situations involving a parent Class and Subclasses. For now, focus on `public` and `private`.
> 

### Default Access — Class vs. Struct

| Type | Default Access |
| --- | --- |
| `class` | `private` |
| `struct` | `public` |

> This is one of the few actual technical differences between a `class` and a `struct` in C++ — otherwise, they behave almost identically.
> 

### How to Use Access Specifiers — The `Employee` Example

```cpp
class Employee {
private:
    int salary;         // hidden — only accessible inside the class

public:
    string name;         // accessible from anywhere
    int age;
    string department;
};
```

> Once you write `private:`, **everything below it remains private** until you write `public:`. From that point onward, everything below `public:` becomes accessible from anywhere — until (and unless) you switch back to `private:` again.
> 

```
┌───────────────────────────────────────────────┐
│              CLASS: Employee                  │
├───────────────────────────────────────────────┤
│  private:                                     │
│     int salary        ← hidden                │
├───────────────────────────────────────────────┤
│  public:                                      │
│     string name        ← open                 │
│     int age             ← open                │
│     string department   ← open                │
└───────────────────────────────────────────────┘
```

### Fixing the `Product` Class With Access Specifiers

```cpp
class Product {
public:
    string name;
    float price;
    float rating;

    void showProduct() {
        cout << "Name: " << name << endl;
        cout << "Price: " << price << endl;
        cout << "Rating: " << rating << endl;
    }
};
```

By explicitly marking everything `public`, the earlier compiler errors disappear, and the code runs exactly as expected.

---

## Key Points to Remember (Part 1)

- OOP rests on **four pillars**: Encapsulation, Abstraction, Inheritance, Polymorphism. This lecture builds the Class/Object foundation and covers Encapsulation.
- OOP solves the problem of **cluttered, unscalable code** by grouping each real-world entity's data into a single **Object**, instead of scattering it across many separate variables.
- A **Class** is a **blueprint** — it's imaginary, contains **data members** (attributes) and **member functions** (methods), and does **not** occupy memory by itself.
- An **Object** is a **real, actual entity** created from a Class — it's an **instance** of that Class, and it's what actually occupies memory.
- Class syntax: `class ClassName { ... };` — declared **outside** `main()`; Objects are created **inside** `main()`, and accessed via the **dot operator (`.`)**.
- **Critical default-access rule:** a `class`'s members are **private by default**; a `struct`'s members are **public by default** — the reverse of each other.
- **Access Specifiers** (`private`, `public`, `protected`) control where a Class's members can be accessed from: `private` → inside the Class only; `public` → anywhere; `protected` → inside the Class and its derived classes (covered properly with Inheritance).
- Once you write `private:` or `public:` inside a Class, that access level applies to **everything below it**, until the next Access Specifier keyword appears.

# 🎯Part 2 — `Encapsulation`, Data Binding, Getters/Setters & Member Functions Outside the Class

---

## 1. Setters and Getters — Accessing Private Data Safely

We now know `private` members can only be touched from inside the Class. But if `salary` is private, how does the rest of the program ever set or read it? The answer: through the Class's own **public functions**.

### The `Employee` Example — Building It Up

```cpp
#include <iostream>
#include <string>
using namespace std;

class Employee {
private:
    string name;
    int id;
    float salary;

public:
    void setEmployee(string n, int id, float s) {
        name = n;
        this->id = id;    // (see note below on why 'n' and 's' are used)
        salary = s;
    }

    void getEmployee() {
        cout << "Name: " << name << endl;
        cout << "ID: " << id << endl;
        cout << "Salary: " << salary << endl;
    }
};

int main() {
    Employee e;
    e.setEmployee("Sachin", 101, 55000);
    e.getEmployee();

    return 0;
}
```

> Notice: instead of directly writing `e.name = "Sachin"` (which would fail — `name` is private), we call a **function** — `setEmployee()` — that lives *inside* the Class and is allowed to touch `name`, `id`, and `salary` directly.
> 

### Why Parameter Names Like `n`, `id`, `s`?

> The instructor deliberately used short parameter names like `n` and `s` instead of writing `name` and `salary` again — this avoids a naming clash between the parameter and the Class's own data member. (Full clarity on exactly *why* this matters comes in the upcoming **Constructors** lecture — for now, just know that short, distinct parameter names sidestep the ambiguity.)
> 

### Naming Convention — Setters and Getters

> A function that **sets** a private value is called a **Setter**. A function that **reads/returns** a private value is called a **Getter**.
> 

| Function Type | Purpose | Example |
| --- | --- | --- |
| **Setter** | Writes/modifies a private value | `setEmployee(...)`, `setBalance(...)` |
| **Getter** | Reads/returns a private value | `getEmployee()`, `getBalance()` |

### A Second Worked Example — Bank Balance

```cpp
class Account {
private:
    float balance;

public:
    void setBalance(float amount) {
        balance = amount;
    }

    float getBalance() {
        return balance;
    }
};
```

> `setBalance()` is the **Setter** — it writes into the private `balance`. `getBalance()` is the **Getter** — it reads and returns `balance`'s current value. This is the standard, universally-used pattern for working with private data in any OOP language, not just C++.
> 

---

## 2. Encapsulation — Finally Defining It Properly

The instructor makes a great point here: **you've already been *doing* Encapsulation this whole lecture** — you just didn't have the formal name for it yet.

> **Encapsulation**: wrapping **data** (attributes) and **functions** (methods) together inside a single Class.
> 

That's it. Every time you put variables and functions together inside one `class { ... }` block, you're using Encapsulation. The private/public system is what makes this wrapping **useful** — it lets you hide sensitive data while still exposing controlled ways to interact with it.

```
┌─────────────────────────────────────────────────┐
│           ENCAPSULATION                         │
│                                                 │
│   class Account {                               │
│       private:  float balance;    ← DATA        │
│       public:   setBalance()       ← FUNC       │
│                 getBalance()       ← FUNC       │
│   };                                            │
│                                                 │
│   Data + Functions, wrapped together            │
│   inside ONE Class = Encapsulation              │
└─────────────────────────────────────────────────┘
```

---

## 3. Data Binding

Closely related to Encapsulation is another term that sounds intimidating but is simple in practice:

> **Data Binding**: private data can only be accessed **through functions** defined inside the Class — never accessed directly from outside.
> 

> Because of Data Binding, the data becomes "bound" inside the Class — protected from being casually read or overwritten from anywhere in the program. This is exactly why security improves: outside code has no direct line to `balance` or `salary` — it can only go through the Setter/Getter "front door."
> 

### Encapsulation vs. Data Binding — Side by Side

| Concept | Definition |
| --- | --- |
| **Encapsulation** | Wrapping data + functions together inside a Class |
| **Data Binding** | Data is accessed only *through* functions — never directly |

> These are genuinely common interview questions — being able to state both definitions cleanly (and explain how they connect to `private`/`public`) is worth memorizing precisely.
> 

---

## 4. Defining Member Functions — Inside vs. Outside the Class

So far, every function we've written has lived **inside** the Class's curly braces. C++ also lets you **declare** a function inside the Class, but write its actual **definition** (the body/logic) **outside** the Class.

### Defining Inside the Class (What We've Done So Far)

```cpp
class Welcome {
public:
    void greet() {
        cout << "Hello Master Ji";
    }
};

int main() {
    Welcome w;
    w.greet();   // Output: Hello Master Ji
    return 0;
}
```

### Defining Outside the Class — The Scope Resolution Operator (`::`)

To define a function outside the Class, you:

1. **Declare** the function inside the Class (just its signature — return type, name, parameters — no body).
2. **Define** the actual function body outside, using the **Scope Resolution Operator (`::`)** to tell the compiler which Class this function belongs to.

### Syntax

```cpp
return_type ClassName::functionName(parameters) {
    // function body
}
```

### Worked Example — `Welcome` Class, Defined Outside

```cpp
#include <iostream>
using namespace std;

class Welcome {
public:
    void greet();   // declaration only — no body here
};

void Welcome::greet() {   // definition outside, using ::
    cout << "Hello Master Ji";
}

int main() {
    Welcome w;
    w.greet();

    return 0;
}
```

**Output:** `Hello Master Ji` — identical behavior to defining it inside; only the *location* of the function's body has changed.

> Read `void Welcome::greet()` as: **"the `greet` function, which belongs to the `Welcome` class, has a `void` return type."** The `::` is what links the loose function definition back to its Class.
> 

### A Second Worked Example — `Rectangle` (From the Instructor's PDF Notes)

**Defined inside the Class:**

```cpp
class Rectangle {
public:
    int length, breadth;

    int area() {
        return length * breadth;
    }
};
```

**Usage:**

```cpp
Rectangle r;
r.length = 10;
r.breadth = 5;
cout << r.area();   // Output: 50
```

**The same Class, with `area()` defined outside instead:**

```cpp
class Rectangle {
public:
    int length, breadth;
    int area();          // declaration only
};

int Rectangle::area() {   // definition, using Scope Resolution Operator
    return length * breadth;
}
```

> Notice that `main()`'s usage code (`r.length = 10; r.breadth = 5; cout << r.area();`) stays **exactly the same** either way — whether `area()` is defined inside or outside the Class makes no difference to how it's *called*. It only changes where the logic is physically written.
> 

---

## 5. Full Combined Program (From the Instructor's PDF Notes)

This example ties together **private data members**, **Setters/Getters**, and **defining functions outside the Class**, all in one clean program:

```cpp
#include <iostream>
using namespace std;

class Student {
private:
    int roll;
    float marks;

public:
    void setData(int r, float m);   // declared here
    void display();                   // declared here
};

void Student::setData(int r, float m) {   // defined outside
    roll = r;
    marks = m;
}

void Student::display() {                  // defined outside
    cout << "Roll: " << roll << endl;
    cout << "Marks: " << marks << endl;
}

int main() {
    Student s;
    s.setData(101, 89.5);
    s.display();
    return 0;
}
```

**Output:**

```
Roll: 101
Marks: 89.5
```

### Tracing Through It

```
main() creates Student object s
   ↓
s.setData(101, 89.5) called
   ↓
Student::setData(101, 89.5) runs → roll = 101, marks = 89.5
   ↓
s.display() called
   ↓
Student::display() runs → prints "Roll: 101" and "Marks: 89.5"
```

> This program is a complete, self-contained demonstration of **Encapsulation** (`roll` and `marks` are wrapped as private data, with public functions controlling access) and **Data Binding** (the only way to touch `roll`/`marks` is through `setData()` and `display()`) — plus the **inside vs. outside** function-definition style, all working together.
> 

---

## Key Points to Remember (Part 2)

- **Setters** write to private data; **Getters** read/return private data — this is the standard pattern for interacting with a Class's hidden attributes from outside.
- **Encapsulation** = wrapping data (attributes) and functions (methods) together inside one Class. You've been doing this the entire lecture — it's the formal name for the private + public + function structure.
- **Data Binding** = private data is accessible **only through functions**, never directly from outside the Class — this is what actually delivers the security benefit of Encapsulation.
- A member function can be **declared inside** the Class and **defined outside** it, using the **Scope Resolution Operator (`::`)**: `return_type ClassName::functionName() { ... }`.
- Whether a function is defined inside or outside the Class makes **zero difference** to how it's called from `main()` — the calling syntax (`objectName.functionName()`) stays identical either way.
- The full `Student` program (private `roll`/`marks`, public `setData()`/`display()`, both defined outside the Class) is a clean, complete template combining every concept from this lecture — worth keeping as a reference pattern for future Class-based problems.