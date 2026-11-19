# Lecture 2: Inheritance - level 1

Date: October 23, 2026
Status: Done

# 🎯 Part 1: What Is Inheritance?

---

## 1. Where This Fits

In the Classes lecture (Unit 2), we saw that a class bundles data and functions together. Now suppose we need several classes that share a lot of the same data:

```cpp
class Human {
public:
    string name;
    int age;
};

class Employee {
public:
    string name;       // ← written again
    int age;           // ← written again
    int eid;
};

class Manager {
public:
    string name;       // ← written again
    int age;           // ← written again
    int cabinNumber;
};
```

`name` and `age` are copied into every class. For 20 classes, that's 20 copies of the same code. If we later want to add `height`, we'd have to edit all 20.

```
┌───────────────────────────────────────────────────────────────┐
│   Same code repeated in many classes                          │
│     → wasted effort                                           │
│     → easy to make mistakes                                   │
│     → hard to change later                                    │
└───────────────────────────────────────────────────────────────┘
```

---

## 2. The Real-Life Idea

Think about biology. A child receives certain characteristics (genes) from their father. The father doesn't give the child a separate copy of everything. The child simply *has* those features because of the family relationship.

> **Inheritance in programming works the same way.** The features of a **parent class** are passed on to a **child class**, so we don't have to write them again.
> 

```
        PARENT                       CHILD
   ┌───────────────┐           ┌────────────────┐
   │ features of   │  ───────▶ │ gets parent's  │
   │ the parent    │ inherited │ features too   │
   └───────────────┘           └────────────────┘
```

In a class, "features" means **data members** and **member functions** (Unit 2).

---

## 3. Formal Definition

> **Inheritance** is an OOP mechanism where a **new class (child)** acquires the **properties and behaviours**, meaning the data and functions, of an **existing class (parent)**.
> 

In the OOP overview (Unit 2), we named four pillars: Encapsulation, Abstraction, **Inheritance** and Polymorphism. This is the third one.

### Why Do We Need It?

| Benefit | Meaning |
| --- | --- |
| **Code reusability** | Write common features once, use them in many classes |
| **Avoids duplication** | No repeated `name` / `age` in every class |
| **Hierarchical classification** | Models real life: Human → Employee → Manager |
| **Foundation of polymorphism** | The next pillar builds directly on inheritance |

---

## 4. Terminology

Different books use different names for the same two roles. All of these are equivalent:

```
┌─────────────────────────┬────────────────────────────────┐
│   PARENT CLASS          │   CHILD CLASS                  │
│   Base class            │   Derived class                │
│   Super class           │   Sub class                    │
├─────────────────────────┼────────────────────────────────┤
│   The class whose       │   The class that               │
│   members are inherited │   inherits from the parent     │
└─────────────────────────┴────────────────────────────────┘
```

> Whenever someone says **base class**, think **parent**. Whenever someone says **derived class**, think **child**. The class is *derived from* the base.
> 

---

## 5. Syntax

```cpp
class Derived : access_specifier Base {
    // extra members of the derived class
};
```

```
   class   Employee   :   public   Human   { ... };
     │        │       │      │        │
     │        │       │      │        └── the BASE (parent) class
     │        │       │      └─────────── access specifier (inheritance mode)
     │        │       └────────────────── colon (:) means "inherits from"
     │        └────────────────────────── the DERIVED (child) class
     └─────────────────────────────────── class keyword
```

- The **colon (`:`)** is what declares the parent.
- The **access specifier** (`public`, `protected` or `private`) decides how the parent's members look inside the child. We'll study this fully in Part 2. For now, we use `public`.
- Inside `{ }`, we add the **extra** members that only the child needs.

---

## 6. Worked Example: `Human` and `Employee`

### The Plan

```
        ┌───────────────────────┐
        │  Human  (PARENT)      │
        │   name                │   ← features everyone has
        │   age                 │
        └──────────┬────────────┘
                   │ inherited by
                   ▼
        ┌───────────────────────┐
        │  Employee  (CHILD)    │
        │   department          │   ← only what is NEW
        └───────────────────────┘
```

Every person has a name and age, so they go into `Human`. An employee **is a** human, so `Employee` inherits from `Human` and only adds `department`.

### The Code

```cpp
#include <iostream>
#include <string>
using namespace std;

// ---------- PARENT CLASS ----------
class Human {
public:
    string name;
    int age;

    void setInfoHuman(string name, int age) {
        this->name = name;       // this-> because parameter and member share a name (Unit 3)
        this->age = age;
    }

    void getInfoHuman() {
        cout << name << endl;
        cout << age << endl;
    }
};

// ---------- CHILD CLASS ----------
class Employee : public Human {      // ← Human is the parent
public:
    string department;               // only NEW data goes here

    void setInfoEmployee(string department) {
        this->department = department;
    }

    void getInfoEmployee() {
        cout << department << endl;
    }
};

int main() {
    Employee e1;

    e1.setInfoHuman("Sachin", 30);   // function of Human, used on an Employee!
    e1.setInfoEmployee("Tech");      // function of Employee

    e1.getInfoHuman();
    e1.getInfoEmployee();

    return 0;
}
```

**Output:**

```
Sachin
30
Tech
```

*(This uses plain setter/getter functions instead of constructors, exactly as the instructor did. Constructors and inheritance come in Part 3.)*

### Tracing Through It

Look at `Employee`. Nowhere inside it did we write `name`, `age`, `setInfoHuman` or `getInfoHuman`. Yet `e1` can use all of them.

```
Employee e1;
   ↓
e1 is an object of Employee. What does it contain?

   ┌───────────────────────────────────────┐
   │   e1                                  │
   │  ┌─────────────────────────────────┐  │
   │  │ **INHERITED** from Human:       │  │
   │  │    name, age                    │  │
   │  │    setInfoHuman(), getInfoHuman │  │
   │  ├─────────────────────────────────┤  │
   │  │ Employee's OWN:                 │  │
   │  │    department                   │  │
   │  │    setInfoEmployee(),           │  │
   │  │    getInfoEmployee()            │  │
   │  └─────────────────────────────────┘  │
   └───────────────────────────────────────┘
```

```
e1.setInfoHuman("Sachin", 30);
   ↓
Employee doesn't define setInfoHuman itself, but it inherited it
   ↓
name = "Sachin", age = 30   (stored inside e1)

e1.setInfoEmployee("Tech");
   ↓
department = "Tech"
```

> **The key idea:** an `Employee` object contains a complete `Human` part inside it, plus its own extra part. So an Employee can do everything a Human can do, and more.
> 

### Relating to the Instructor's Argument

The instructor pointed out that this stops us from repeating code. We put the general attributes everyone shares (name, age, and in real programs height, address, phone number) into `Human`. Then `Manager`, `Employee`, or a tech-department employee class can all inherit from `Human`. We never rewrite `name` and `age`.

```
                 ┌──────────┐
                 │  Human   │   ← written ONCE
                 └────┬─────┘
         ┌────────────┼────────────┐
         ▼            ▼            ▼
     ┌─────────┐ ┌─────────┐ ┌───────────┐
     │Employee │ │ Manager │ │ TechStaff │   ← all get name, age
     └─────────┘ └─────────┘ └───────────┘      for free
```

---

## 7. The "IS-A" Test

A simple way to check whether inheritance fits:

> If you can say **"Child *is a* Parent"** and it sounds natural, inheritance is a good fit.
> 

| Sentence | Sounds right? | Use inheritance? |
| --- | --- | --- |
| An Employee **is a** Human | ✅ | Yes |
| A Dog **is an** Animal | ✅ | Yes |
| A Circle **is a** Shape | ✅ | Yes |
| A Car **is an** Engine | ❌ | No (a car *has an* engine; Lecture 3 covers this) |

---

## 8. Common Mistakes to Avoid

| Mistake | What goes wrong |
| --- | --- |
| Rewriting `name` and `age` inside `Employee` | Defeats the purpose of inheritance and wastes memory. |
| Forgetting the colon: `class Employee public Human` | Syntax error. The `:` is required. |
| Forgetting the semicolon after the class `}` | Same rule as every class (Unit 2). |
| Forgetting the access specifier | The default may not be what you expect (see Part 2). |
| Using inheritance when "is-a" doesn't hold | Confusing, badly designed code. |

---

## Key Points to Remember (Part 1)

- **Inheritance** lets a child class acquire the **data and functions** of a parent class, so common code is written **once**.
- Parent = **base** = **super** class. Child = **derived** = **sub** class.
- Syntax: `class Derived : access_specifier Base { ... };`
- An object of the derived class contains the **base part** *and* its **own part**, so it can use both.
- Main benefits: **code reusability**, **no duplication**, **hierarchical classification**, and it is the **foundation of polymorphism**.
- Use the **"is-a"** test to decide whether inheritance fits.

# 🎯 Part 2: Access Specifiers and Inheritance Modes

---

![image.png](Lecture%202%20Inheritance%20-%20level%201/image.png)

## 1. Two Different Questions

In Part 1, we wrote `class Employee : public Human`. The instructor said he would explain that `public` properly, and this is where it happens.

There are **two separate places** where access matters:

```
┌──────────────────────────────────────────────────────────────────┐
│  **QUESTION 1**: Inside the BASE class                           │
│     class Human {                                                │
│     public:    ...    ← who can use these members?               │
│     protected: ...                                               │
│     private:   ...                                               │
│     };                                                           │
│                                                                  │
│  **QUESTION 2**: When INHERITING                                 │
│     class Employee : *public* Human                              │
│                       ▲                                          │
│                       └── how should the inherited members       │
│                           be treated inside Employee?            │
└──────────────────────────────────────────────────────────────────┘
```

We'll take them one at a time.

---

## 2. Question 1: Access Specifiers Inside the Base Class

From the Classes lecture (Unit 2), we know `private` and `public`. Inheritance adds a third useful one: **`protected`**.

| Specifier | Who can access it |
| --- | --- |
| `private` | **Inside the base class only** |
| `protected` | The base class **and its derived (child) classes** |
| `public` | **Everywhere** |

> In Unit 2 we said "`protected` will make sense once we study Inheritance." Now it does. **Protected means: like private, but children are allowed to use it too.**
> 

### The "Family House" Picture

```
                     OUTSIDE WORLD (main, other code)
   ┌─────────────────────────────────────────────────────────┐
   │                                                         │
   │    public   → anyone in the world can see this          │
   │                                                         │
   │   ┌─────────────────────────────────────────────────┐   │
   │   │   CHILD classes (derived)                       │   │
   │   │   protected → family members can use this       │   │
   │   │                                                 │   │
   │   │   ┌─────────────────────────────────────────┐   │   │
   │   │   │  BASE class itself                      │   │   │
   │   │   │  private → only the base class itself   │   │   │
   │   │   └─────────────────────────────────────────┘   │   │
   │   └─────────────────────────────────────────────────┘   │
   └─────────────────────────────────────────────────────────┘
```

Each layer can see everything on its own layer and the layers outside it. The base class sees all three. A child sees `protected` and `public`. The outside world sees only `public`.

### Example: Seeing It in Code

```cpp
class Base {
public:
    int a;          // anyone can use
protected:
    int b;          // base + derived only
private:
    int c;          // base only
};

class Derived : public Base {
public:
    void show() {
        a = 10;     // ✅ OK   (public)
        b = 20;     // ✅ OK   (protected: child can use it)
        // c = 30;  // ❌ ERROR (private: not even the child)
    }
};

int main() {
    Derived d;
    d.a = 5;        // ✅ OK   (public: anyone)
    // d.b = 5;     // ❌ ERROR (protected: outside world blocked)
    // d.c = 5;     // ❌ ERROR (private)
    return 0;
}
```

| Where we try to use it | `a` (public) | `b` (protected) | `c` (private) |
| --- | --- | --- | --- |
| Inside `Base` | ✅ | ✅ | ✅ |
| Inside `Derived` | ✅ | ✅ | ❌ |
| In `main()` (outside) | ✅ | ❌ | ❌ |

> **Important:** a private member of the base class **is still inside the derived object** (memory is still reserved for it). The derived class just has **no permission** to touch it directly. If the child needs to work with it, the base class can offer a public or protected function that uses it (like a getter, Unit 2).
> 

---

## 3. Question 2: The Inheritance Mode

Now look at the access specifier written **before the base class name**:

```cpp
class Employee : public Human
class Employee : protected Human
class Employee : private Human
```

This is called the **inheritance mode**. As the instructor explained:

> Name and age are `public` in `Human`. But after they come into `Employee`, will they stay `public`, or become `protected`, or `private`? The access specifier we write **before the base class name** decides that.
> 

The mode works like a **filter**. It can only keep the access level the same or make it **stricter**. It can **never** make a member more open than it was.

```
   MOST OPEN  ──────────────────────────────▶  MOST RESTRICTED
     public        protected        private

   Inheritance mode can only move members to the RIGHT (or leave them),
   never to the left.
```

---

## 4. Public Inheritance

```cpp
class B : public A { ... };
```

> Members keep the **same access level** they had in the base class.
> 

| Member in Base | Becomes in Derived |
| --- | --- |
| public | **public** |
| protected | **protected** |
| private | not accessible |

```
        BASE (A)                       DERIVED (B : public A)
   ┌──────────────────┐            ┌──────────────────────────┐
   │ public    x      │  ───────▶  │ public     x   (same)    │
   │ protected y      │  ───────▶  │ protected  y   (same)    │
   │ private   z      │  ──╳       │ (z is there but can't    │
   └──────────────────┘            │  be touched directly)    │
                                   └──────────────────────────┘
```

This is the most common mode, and it's the one used in all of Part 1. It matches the "is-a" idea: an Employee is a Human, so an Employee should be able to do everything a Human does, publicly.

### Example

```cpp
class Base {
public:    int a;
protected: int b;
private:   int c;
};

class Derived : public Base {
public:
    void show() {
        a = 10;       // ✅ OK
        b = 20;       // ✅ OK
        // c = 30;    // ❌ not accessible
    }
};

int main() {
    Derived d;
    d.a = 100;        // ✅ a is still PUBLIC in Derived, so main() can use it
    return 0;
}
```

---

## 5. Protected Inheritance

```cpp
class B : protected A { ... };
```

> Everything that was `public` or `protected` in the base becomes **`protected`** in the derived class.
> 

| Member in Base | Becomes in Derived |
| --- | --- |
| public | **protected** |
| protected | **protected** |
| private | not accessible |

```
        BASE (A)                       DERIVED (B : protected A)
   ┌──────────────────┐             ┌────────────────────────────┐
   │ public    x      │  ───────▶   │ protected  x   ← tightened │
   │ protected y      │  ───────▶   │ protected  y               │
   │ private   z      │  ──╳        │ (not accessible)           │
   └──────────────────┘             └────────────────────────────┘
```

```cpp
class A { public: int x; };
class B : protected A { };

int main() {
    B obj;
    // obj.x = 5;   // ❌ ERROR: x is now PROTECTED inside B,
                    //    so main() (outside) can't use it
    return 0;
}
```

Inside `B`, or inside classes that inherit from `B`, `x` is still usable. Only the **outside world** is blocked.

---

## 6. Private Inheritance

```cpp
class B : private A { ... };
```

> Everything that was `public` or `protected` in the base becomes **`private`** in the derived class.
> 

| Member in Base | Becomes in Derived |
| --- | --- |
| public | **private** |
| protected | **private** |
| private | not accessible |

```
        BASE (A)                       DERIVED (B : private A)
   ┌──────────────────┐             ┌────────────────────────────┐
   │ public    x      │  ───────▶   │ private    x   ← tightened │
   │ protected y      │  ───────▶   │ private    y   ← tightened │
   │ private   z      │  ──╳        │ (not accessible)           │
   └──────────────────┘             └────────────────────────────┘
```

Inside `B`, the members can still be used by `B`'s own functions. But the world outside **and any class that inherits from `B`** cannot use them. They are "sealed" inside `B`.

```cpp
class A { public: int x; };
class B : private A {
public:
    void set() { x = 5; }     // ✅ B's own function can use x
};
class C : public B {
public:
    void test() {
        // x = 10;            // ❌ ERROR: x is private inside B,
                              //    so C (a child of B) can't use it either
    }
};
```

This matches what the instructor said in the lecture. With private inheritance, "you can use all these functions only inside Employee, and you cannot use them outside Employee."

---

## 7. All Three Modes Side by Side

This table is very commonly asked in exams. Read **across a row**: base access → access inside the derived class.

| Base class member | `public` inheritance | `protected` inheritance | `private` inheritance |
| --- | --- | --- | --- |
| **public** | public | protected | private |
| **protected** | protected | protected | private |
| **private** | ❌ not accessible | ❌ not accessible | ❌ not accessible |

```
┌───────────────────────────────────────────────────────────────────┐
│  TWO RULES THAT EXPLAIN THE WHOLE TABLE                           │
│                                                                   │
│  Rule 1:  private members of the base are NEVER                   │
│           accessible in the derived class (any mode).             │
│                                                                   │
│  Rule 2:  the mode can only keep or TIGHTEN access:               │
│           the result is whichever is MORE restricted,             │
│           the member's original level or the mode.                │
│                                                                   │
│     public    member + protected mode → protected                 │
│     protected member + private   mode → private                   │
│     protected member + public    mode → protected (unchanged)     │
└───────────────────────────────────────────────────────────────────┘
```

> **Memory trick:** *"the mode is a ceiling."* In `protected` inheritance, nothing can be more open than `protected`. In `private` inheritance, nothing can be more open than `private`.
> 

### Quick Reference: What Can `main()` Do With a Derived Object?

Suppose `Base` has `public int a`, and we create `Derived d;`:

| Inheritance mode | `d.a` from `main()` |
| --- | --- |
| `public` | ✅ allowed |
| `protected` | ❌ blocked |
| `private` | ❌ blocked |

---

## 8. A Clarification About the Lecture

In the video, the instructor described a few of these rules in words while thinking aloud. The tables above are the **exact, correct rules** (they match the instructor's PDF note exactly). The key ideas from the video all hold:

- Private members of the base can't be accessed in the derived class, **no matter which mode you use**.
- Protected members are usable inside the child, but not from outside.
- A protected member can **never become public** through inheritance. It stays protected or becomes more restricted.
- A public member can be inherited using **all three modes**.

---

## 9. What If You Forget to Write the Mode?

```cpp
class Employee : Human { ... };      // no mode written
```

C++ uses a **default**, which depends on the keyword used. This echoes the `class` vs `struct` rule from Unit 2:

| Declared with | Default inheritance mode |
| --- | --- |
| `class` | **private** |
| `struct` | **public** |

> So if you write `class Employee : Human` and expect `e1.name` to work from `main()`, you'll get an error, because it silently became **private** inheritance. **Always write the mode explicitly.** In practice, `public` is the one you'll use most.
> 

---

## 10. Common Mistakes to Avoid

| Mistake | What goes wrong |
| --- | --- |
| Expecting a child to use the base's `private` data | Not possible in any mode. Use `protected`, or a public/protected function. |
| Writing `class B : A` and expecting public access | `class` defaults to **private** inheritance. |
| Thinking `protected` is accessible from `main()` | No. Only the class itself and its children can use it. |
| Thinking `protected` inheritance can make members `public` | The mode can only **tighten**, never loosen. |
| Assuming private base members are "deleted" in the child | They still exist in memory. They are just inaccessible directly. |
| Using `private` inheritance when "is-a" is meant | Then `main()` can't use the base functions on the child. Use `public`. |

---

## Key Points to Remember (Part 2)

- Inside a class there are three levels: **private** (base only), **protected** (base + derived), **public** (everywhere).
- The **inheritance mode** (written before the base class name) decides how inherited members look **inside the derived class**.
- The mode can only **keep or tighten** access. It can **never loosen** it.
- **Public inheritance**: public → public, protected → protected.
- **Protected inheritance**: public → protected, protected → protected.
- **Private inheritance**: public → private, protected → private.
- **Private members of the base are never accessible in the derived class**, in any mode, though they still exist inside the object.
- `class` defaults to **private** inheritance, `struct` defaults to **public**. Always write the mode.
- `public` inheritance is the most common, and it matches the "is-a" relationship.

# 🎯 Part 3: Constructors and Destructors in Inheritance

---

## 1. Where This Fits

From the Constructors lecture (Unit 3), we know:

- A **constructor** runs automatically when an object is created.
- A **destructor** runs automatically when an object goes out of scope.

Now a new question arises. When we create an `Employee` object, it contains a `Human` part and an Employee part. **Which constructor runs first?** And what if the `Human` part needs values, like a name and age?

```
Employee e1;
   │
   ├── a Human part must be built   → Human's constructor
   └── an Employee part must be built → Employee's constructor

   In what ORDER? And who gives Human its values?
```

This part answers both questions.

---

## 2. Order of Constructor Calls

> **The base class constructor runs first. Then the derived class constructor.**
> 

The reason is simple. The child is built **on top of** the parent. You can't build the second floor before the first floor exists. The `Human` part must be ready before the Employee part, since the Employee part may depend on it.

```
   Creating a Derived object:

   Step 1:  Base constructor     ──▶  builds the BASE part
   Step 2:  Derived constructor  ──▶  builds the DERIVED part

        ┌────────────────────┐
        │ Derived part  ②   │   ← built second
        ├────────────────────┤
        │ Base part     ①   │   ← built first
        └────────────────────┘
```

### Example: Seeing the Order

This is the `Parent` / `Child` example from the lecture. We only print messages so we can watch the order.

```cpp
#include <iostream>
using namespace std;

class Parent {
public:
    Parent() {
        cout << "This is a Parent constructor" << endl;
    }
    ~Parent() {
        cout << "Parent destructor" << endl;
    }
};

class Child : public Parent {
public:
    Child() {
        cout << "I am a Child constructor" << endl;
    }
    ~Child() {
        cout << "Child destructor" << endl;
    }
};

int main() {
    Child c1;       // only ONE object created: a Child
    return 0;
}
```

**Output:**

```
This is a Parent constructor
I am a Child constructor
Child destructor
Parent destructor
```

### Tracing Through It

```
Child c1;
   ↓
We only asked for a Child, but a Child CONTAINS a Parent part.
   ↓
Step 1: Parent() runs   → prints "This is a Parent constructor"
Step 2: Child() runs    → prints "I am a Child constructor"
   ↓
main() reaches return 0; and c1 goes out of scope
   ↓
Step 3: ~Child() runs   → prints "Child destructor"
Step 4: ~Parent() runs  → prints "Parent destructor"
```

Notice that we never wrote `Parent p;` anywhere. Creating a `Child` automatically triggers the `Parent` constructor too.

---

## 3. Order of Destructor Calls

> **Destructors run in the opposite order: the derived destructor first, then the base destructor.**
> 

```
   Constructors:   Base  ──▶  Derived        (build bottom-up)
   Destructors:    Derived ──▶ Base          (tear down top-down)
```

The reason is the same building analogy. When demolishing a house, you remove the second floor **before** the first floor. The base part must still exist while the derived part is being cleaned up, because the derived part may still be using it.

```
   CONSTRUCTION (going up)         DESTRUCTION (coming down)

      ┌─────────────┐                 ┌─────────────┐
      │  Derived ②  │                │  Derived ①  │
      ├─────────────┤                 ├─────────────┤
      │  Base    ①  │                │  Base    ②  │
      └─────────────┘                 └─────────────┘
```

This is a **last in, first out** pattern, like a stack of books (Functions lecture, call stack). The last thing built is the first thing removed.

### Summary Table

| Event | Order |
| --- | --- |
| **Constructors** | Base → Derived |
| **Destructors** | Derived → Base |

> This matches the instructor's PDF note: "Base class constructor is called first, then derived class constructor," and "Derived destructor first, then Base destructor."
> 

---

## 4. When the Base Class Needs Values

So far the constructors had no parameters. Now the real situation: we want to create an Employee and give its name, age and department **all at once**, instead of calling `setInfoHuman()` and `setInfoEmployee()` separately.

```cpp
class Human {
public:
    string name;
    int age;

    Human(string name, int age) {          // Human NEEDS two values
        this->name = name;
        this->age = age;
    }
};
```

Since `Human` now has a parameterized constructor, building a `Human` part **requires** a name and an age. So here is the problem:

```
Employee e1("Sachin", 30, "Tech");
                 │       │     │
                 │       │     └── Employee's own
                 └───────┴──────── these two belong to Human!

The Employee constructor receives all three.
But who passes "Sachin" and 30 on to the Human constructor?
```

> C++ will **not** guess. When you create an Employee, the Employee constructor runs. It does not automatically hand your values to the Human constructor. **You** must do that explicitly.
> 

---

## 5. The Solution: Calling the Base Constructor With a Colon

The instructor's method: accept all three values in the Employee constructor, then put a **colon** after the parameter list and call the Human constructor, passing the values it needs.

### Syntax

```cpp
DerivedClass(all parameters) : BaseClass(values for the base) {
    // set the derived class's own members here
}
```

### The Code

```cpp
#include <iostream>
#include <string>
using namespace std;

class Human {
public:
    string name;
    int age;

    Human(string name, int age) {
        this->name = name;
        this->age = age;
    }
};

class Employee : public Human {
public:
    string department;

    Employee(string name, int age, string department) : Human(name, age) {
        this->department = department;       // Employee sets only its OWN member
    }

    void getInfo() {
        cout << name << endl;
        cout << age << endl;
        cout << department << endl;
    }
};

int main() {
    Employee e1("Sachin", 30, "Tech");
    e1.getInfo();
    return 0;
}
```

**Output:**

```
Sachin
30
Tech
```

### Breaking Down the Constructor Line

```
Employee( string name, int age, string department )  :  Human( name, age )  { ... }
          └──────────── all 3 values ────────────┘      │     └─ passes these
                                                        │        two UP to Human
                                                        └── colon: "before the body
                                                            runs, call this base
                                                            constructor"
```

### Tracing Through It

```
Employee e1("Sachin", 30, "Tech");
   ↓
Employee constructor receives: name="Sachin", age=30, department="Tech"
   ↓
BEFORE its body runs, the part after the colon executes:
Human("Sachin", 30) is called
   ↓
Human's constructor sets:  name = "Sachin",  age = 30
   ↓
NOW Employee's body runs:  department = "Tech"
   ↓
e1 is fully built
```

```
        Employee e1("Sachin", 30, "Tech")
                    │
                    ▼
        ┌──── Employee constructor ─────┐
        │  gets all 3 values            │
        │                               │
        │   "Sachin", 30  ───────────▶  Human constructor
        │                               │   sets name, age
        │   "Tech" stays here           │
        │   sets department             │
        └───────────────────────────────┘
```

> **Think of it as a relay.** The Employee constructor is the front desk. It receives everything, keeps what belongs to Employee, and forwards what belongs to Human.
> 

### The Colon Is the Initializer List

This colon is the **same initializer list syntax** from Unit 3 (`Student(int r) : roll(r) { }`). The only difference is that here we initialize a **base class part** instead of a member variable.

```
   Student(int r) : roll(r) { }        ← initializes a MEMBER
   Employee(...)  : Human(name, age) { } ← initializes the BASE CLASS part
```

And the same rule applies: the initializer list runs **before** the constructor body. That is why the Human part is already ready by the time Employee's body starts.

---

## 6. What If You Don't Call the Base Constructor?

If you leave out the `: Human(...)` call, C++ tries to call Human's **default constructor** (the one with no parameters) automatically.

| Situation | Result |
| --- | --- |
| Base has a **default constructor** (no parameters) | ✅ Works. It's called automatically. |
| Base has **only** a parameterized constructor | ❌ **Compiler error**, because C++ doesn't know what values to give. |

```cpp
class Human {
public:
    Human(string name, int age) { ... }     // only this one exists
};

class Employee : public Human {
public:
    Employee() {                            // ❌ ERROR!
        // C++ tries Human() with no arguments, but none exists
    }
};
```

This connects to Unit 3: once you write **any** constructor yourself, C++ stops providing the free default one. The fix is either to call the base constructor with the colon, or to add a default constructor to the base class.

---

## 7. Instructor's PDF Example: `Base` and `Derived`

The PDF shows the same idea with a simpler pair, using `protected` data (Part 2):

```cpp
class Base {
protected:
    int x;
public:
    Base(int a) {
        x = a;
    }
};

class Derived : public Base {
    int y;
public:
    Derived(int a, int b) : Base(a) {      // a goes UP to Base
        y = b;                              // b stays in Derived
    }

    void show() {
        cout << x << " " << y;
    }
};

int main() {
    Derived d(10, 20);
    d.show();
    return 0;
}
```

**Output:**

```
10 20
```

```
Derived d(10, 20);
   ↓
Derived( a=10, b=20 ) : Base(a)
   ↓
Base(10) runs first  →  x = 10
   ↓
Derived's body runs  →  y = 20
   ↓
show() prints:  10 20
```

> Notice `show()` is able to use `x` directly. That works because `x` is `protected` in Base, so the derived class is allowed to use it (Part 2). If `x` were `private`, `show()` would fail.
> 

---

## 8. Full Order With Parameters: Constructor Calls vs Body

It's easy to mix up *which line runs when*. Let's trace a program with messages.

```cpp
#include <iostream>
using namespace std;

class Base {
public:
    Base(int a) {
        cout << "Base constructor, a = " << a << endl;
    }
    ~Base() {
        cout << "Base destructor" << endl;
    }
};

class Derived : public Base {
public:
    Derived(int a, int b) : Base(a) {
        cout << "Derived constructor, b = " << b << endl;
    }
    ~Derived() {
        cout << "Derived destructor" << endl;
    }
};

int main() {
    Derived d(10, 20);
    cout << "Inside main" << endl;
    return 0;
}
```

**Output:**

```
Base constructor, a = 10
Derived constructor, b = 20
Inside main
Derived destructor
Base destructor
```

```
Timeline:

  Derived d(10, 20);
      │
      ├─▶ ① Base(10) runs            (from the colon part)
      ├─▶ ② Derived's body runs
      │
  cout << "Inside main"
      │
  } ← d goes out of scope
      │
      ├─▶ ③ ~Derived() runs
      └─▶ ④ ~Base() runs
```

---

## 9. Constructors in More Than Two Levels

The instructor's `Manager` example extends this one level further. Understanding it here helps in Part 4 (multilevel inheritance).

```
        Human          ← at the bottom (built first)
          ▲
        Employee
          ▲
        Manager        ← at the top (built last)
```

Each constructor passes **only to its direct parent**, and that parent passes to *its* parent:

```cpp
class Manager : public Employee {
public:
    int cabinNumber;

    Manager(string name, int age, string dept, int cabin)
        : Employee(name, age, dept) {        // Manager → Employee
        this->cabinNumber = cabin;           // Manager keeps cabin
    }
};
```

```
Manager m1("Sachin", 30, "Tech", 101);
   │
   ▼  Manager receives all 4 values
   │
   ├── passes "Sachin", 30, "Tech" ──▶ Employee constructor
   │                                       │
   │                                       ├── passes "Sachin", 30 ──▶ Human constructor
   │                                       │                           sets name, age
   │                                       └── keeps "Tech"  → sets department
   │
   └── keeps 101 → sets cabinNumber
```

**Order of construction:** Human → Employee → Manager.

**Order of destruction:** Manager → Employee → Human.

> Manager does **not** call `Human(...)` directly. It only talks to its direct parent `Employee`, and Employee talks to `Human`. Each class takes care of its own immediate parent. This is how the instructor explained it, and the full multilevel example comes in Part 4.
> 

---

## 10. Common Mistakes to Avoid

| Mistake | What goes wrong |
| --- | --- |
| Assuming the derived constructor runs first | Base always runs **first**. Derived runs second. |
| Assuming destructors run in the same order as constructors | They run in **reverse**: derived first, then base. |
| Not passing values to a parameterized base constructor | Compiler error: no matching default constructor in the base. |
| Trying to set `name` and `age` in the Employee **body** only | Human's constructor already ran (or failed to compile). Use the colon call. |
| Putting the base call inside the `{ }` body | Wrong. It must go **after the colon**, before the body. |
| Writing the colon part but forgetting the base needs matching argument types | Argument count and types must match a Base constructor. |
| Forgetting a destructor in a class that uses `new` | Memory leak (Unit 5). Destructors still run in derived → base order. |

---

## Key Points to Remember (Part 3)

- Creating a derived object builds the **base part first**, then the derived part.
- **Constructors:** Base → Derived. **Destructors:** Derived → Base. Destruction is the reverse of construction.
- If the base has a **parameterized constructor**, the derived constructor must call it explicitly after a **colon**: `Derived(...) : Base(values) { ... }`.
- This colon is the **initializer list** from Unit 3, and it runs **before** the derived constructor's body.
- The derived constructor accepts **all** the values and forwards the base's share upward, keeping the rest for itself.
- If you don't call a base constructor, C++ uses the base's **default constructor**. If none exists, you get a **compiler error**.
- In multilevel chains, each constructor passes values to its **direct parent only**.

# 🎯 Part 4: Types of Inheritance

---

## 1. Where This Fits

So far, every example had **one parent and one child**. That is just one way to arrange classes. Depending on how many parents and children are involved, inheritance comes in five types:

```
┌──────────────────────────────────────────────────────────────┐
│                  **TYPES OF INHERITANCE**                    │
├──────────────┬───────────────┬──────────────┬────────────────┤
│  Single      │  Multilevel   │  Multiple    │ Hierarchical   │
├──────────────┴───────────────┴──────────────┴────────────────┤
│                     Hybrid (a mixture)                       │
└──────────────────────────────────────────────────────────────┘
```

In the diagrams below, an **arrow points from the child up to the parent**, the usual way inheritance is drawn.

---

## 2. Single Inheritance

> **One base class → one derived class.**
> 

```
      ┌────────┐
      │   A    │   ← base (parent)
      └────────┘
          ▲
          │
      ┌────────┐
      │   B    │   ← derived (child)
      └────────┘
```

This is what we have used throughout Parts 1 to 3 (`Human → Employee`). The instructor's PDF gives a second example:

```cpp
#include <iostream>
using namespace std;

class Animal {
public:
    void eat() {
        cout << "Eating" << endl;
    }
};

class Dog : public Animal {
public:
    void bark() {
        cout << "Barking" << endl;
    }
};

int main() {
    Dog d;
    d.eat();       // inherited from Animal
    d.bark();      // Dog's own
    return 0;
}
```

**Output:**

```
Eating
Barking
```

---

## 3. Multilevel Inheritance

> **A class is derived from another derived class**, forming a chain: grandparent → parent → child.
> 

```
      ┌──────────┐
      │  Human   │   ← base class (grandparent)
      └──────────┘
           ▲
      ┌──────────┐
      │ Employee │   ← child of Human, parent of Manager
      └──────────┘
           ▲
      ┌──────────┐
      │ Manager  │   ← grandchild
      └──────────┘
```

As the instructor explained, the features of the parent pass into the child, and the child's features (which already include the parent's) pass into the grandchild. So a `Manager` has the features of **both** `Employee` and `Human`.

### The Full Program (from the video)

```cpp
#include <iostream>
#include <string>
using namespace std;

class Human {
public:
    string name;
    int age;

    Human(string name, int age) {
        this->name = name;
        this->age = age;
    }
};

class Employee : public Human {
public:
    string department;

    Employee(string name, int age, string department) : Human(name, age) {
        this->department = department;
    }

    void getInfo() {
        cout << "Name: " << name << endl;
        cout << "Age: " << age << endl;
        cout << "Department: " << department << endl;
    }
};

class Manager : public Employee {
public:
    int cabinNumber;

    Manager(string name, int age, string department, int cabinNumber)
        : Employee(name, age, department) {
        this->cabinNumber = cabinNumber;
    }

    void getManagerInfo() {
        getInfo();                                      // inherited from Employee
        cout << "Cabin Number: " << cabinNumber << endl;
    }
};

int main() {
    Manager m1("Sachin", 30, "Tech", 101);
    m1.getManagerInfo();
    return 0;
}
```

**Output:**

```
Name: Sachin
Age: 30
Department: Tech
Cabin Number: 101
```

### Tracing the Values (Relay from Part 3)

```
Manager m1("Sachin", 30, "Tech", 101);
   │
   ▼
Manager constructor gets all 4 values
   ├── forwards "Sachin", 30, "Tech" ─▶ Employee constructor
   │                                       ├── forwards "Sachin", 30 ─▶ Human constructor
   │                                       │                            sets name, age
   │                                       └── keeps "Tech" → department
   └── keeps 101 → cabinNumber
```

```
What does the m1 object contain?

   ┌───────────────────────────────┐
   │ Manager part:  cabinNumber    │
   │ ┌───────────────────────────┐ │
   │ │ Employee part: department │ │
   │ │ ┌───────────────────────┐ │ │
   │ │ │ Human part: name, age │ │ │
   │ │ └───────────────────────┘ │ │
   │ └───────────────────────────┘ │
   └───────────────────────────────┘
```

**Construction order:** Human → Employee → Manager. **Destruction order:** Manager → Employee → Human.

The PDF's version uses simple classes `A → B → C`:

```cpp
class A { public: void showA() { cout << "Class A\n"; } };
class B : public A { public: void showB() { cout << "Class B\n"; } };
class C : public B { public: void showC() { cout << "Class C\n"; } };
```

An object of `C` can call `showA()`, `showB()` and `showC()`, because C inherits B, and B already inherited A.

---

## 4. Multiple Inheritance

> **One derived class inherits from more than one base class.**
> 

```
   ┌───────────┐     ┌───────────┐
   │ ParentOne │     │ParentTwo  │    ← two (or more) base classes
   └───────────┘     └───────────┘
           ▲           ▲
            \         /
             \       /
            ┌─────────┐
            │  Child  │              ← ONE child gets features of both
            └─────────┘
```

### Syntax

Separate the base classes with **commas**. Each one gets its own access specifier:

```cpp
class Child : public ParentOne, public ParentTwo { ... };
```

### The Code (from the video)

```cpp
#include <iostream>
using namespace std;

class ParentOne {
public:
    ParentOne() {
        cout << "This is Parent One" << endl;
    }
};

class ParentTwo {
public:
    ParentTwo() {
        cout << "This is Parent Two" << endl;
    }
};

class Child : public ParentOne, public ParentTwo {
public:
    Child() {
        cout << "This is Child" << endl;
    }
};

int main() {
    Child c1;
    return 0;
}
```

**Output:**

```
This is Parent One
This is Parent Two
This is Child
```

### Order of Construction

```
Child c1;
   ↓
Base classes are built in the ORDER THEY ARE LISTED after the colon:
   ① ParentOne()
   ② ParentTwo()
   ③ Child()          ← derived class last
```

> The order depends on the **inheritance list** (`public ParentOne, public ParentTwo`), not on any other order you might write. Destructors run in the exact reverse: `~Child`, `~ParentTwo`, `~ParentOne`.
> 

The PDF's example shows the same idea with functions:

```cpp
class A { public: void showA() { cout << "A\n"; } };
class B { public: void showB() { cout << "B\n"; } };
class C : public A, public B {};

int main() {
    C obj;
    obj.showA();     // from A
    obj.showB();     // from B
    return 0;
}
```

### A Warning (Covered in Lecture 3)

What if **both** parents have a function with the same name, say `show()`? Then `obj.show()` is ambiguous, because the compiler doesn't know which one you mean. Solving this is the topic *Resolving Ambiguities in Inheritance* in the Unit IV syllabus, and we cover it in Lecture 3.

---

## 5. Hierarchical Inheritance

> **One base class → many derived classes.** It is the **reverse of multiple inheritance**.
> 

```
                 ┌──────────┐
                 │  Parent  │
                 └──────────┘
                ▲      ▲      ▲
               /       |       \
      ┌──────────┐ ┌──────────┐ ┌───────────┐
      │ ChildOne │ │ ChildTwo │ │ ChildThree│
      └──────────┘ └──────────┘ └───────────┘
```

```
   Multiple:       MANY parents  →  ONE child
   Hierarchical:   ONE parent    →  MANY children
```

### The Code

The PDF example uses shapes:

```cpp
#include <iostream>
using namespace std;

class Shape {
public:
    void draw() {
        cout << "Drawing Shape" << endl;
    }
};

class Circle : public Shape {};      // empty body: it still gets draw()
class Square : public Shape {};

int main() {
    Circle c;
    Square s;
    c.draw();
    s.draw();
    return 0;
}
```

**Output:**

```
Drawing Shape
Drawing Shape
```

Even though `Circle` and `Square` have empty bodies `{}`, both can call `draw()`, because they inherited it from `Shape`. In real programs, each child would add its own extras (radius for Circle, side for Square), while the shared part stays in the parent.

> The two children are **independent** of each other. `Circle` knows nothing about `Square`. They only share the same parent.
> 

---

## 6. Hybrid Inheritance

> **A mixture of two or more types** of inheritance in one program.
> 

As the instructor said, at one level multiple inheritance may be happening, at another level hierarchical, and at another multilevel. You have already seen all the building blocks, so no new syntax is needed.

### Example Shape: Multilevel + Multiple

```
            ┌──────────┐
            │  Human   │
            └──────────┘
             ▲        ▲
            /          \
    ┌──────────┐    ┌──────────┐
    │ Employee │    │ Student  │        ← hierarchical (Human has 2 children)
    └──────────┘    └──────────┘
             ▲        ▲
              \      /
        ┌────────────────┐
        │ TeachingAssist │               ← multiple (2 parents)
        └────────────────┘
```

```cpp
class Human { public: string name; };
class Employee : public Human { };
class Student  : public Human { };
class TeachingAssist : public Employee, public Student { };
```

### 💣The Catch: The `Diamond Problem`

This shape is nicknamed the **diamond**. Look closely: `TeachingAssist` gets `Human` **twice**, once through `Employee` and once through `Student`.

```
   TeachingAssist object contains:

   ┌──────────────────────────────────────────┐
   │ Employee part ── contains Human part     │  ← name (copy 1)
   │ Student part  ── contains Human part     │  ← name (copy 2)
   └──────────────────────────────────────────┘

   ta.name   → which copy? ❌ **AMBIGUOUS**
```

C++ gives an error on `ta.name`. The fix is the **virtual base class**, a Unit IV syllabus topic that we cover in Lecture 3.

---

## 7. All Types Side by Side

| Type | Structure | Parents | Children | Example |
| --- | --- | --- | --- | --- |
| **Single** | A ← B | 1 | 1 | `Animal ← Dog` |
| **Multilevel** | A ← B ← C | 1 per level | 1 per level | `Human ← Employee ← Manager` |
| **Multiple** | A, B ← C | **2 or more** | 1 | `ParentOne, ParentTwo ← Child` |
| **Hierarchical** | A ← B, C, D | 1 | **2 or more** | `Shape ← Circle, Square` |
| **Hybrid** | any mix | varies | varies | **`Diamond`**: `Human ← Employee, Student ← TA` |

```
   **Single**        **Multilevel**      **Multiple**       **Hierarchical**
     A               A           A    B            A
     ▲               ▲            \  /            /|\
     B               B             C             B C D
                     ▲
                     C
```

> **Memory trick:**
> 
> 
> *Multiple* → many **parents**, one child.
> 
> *Hierarchical* → one parent, many **children**.
> 
> *Multilevel* → a **chain** (grandparent → parent → child).
> 

---

## 8. Common Mistakes to Avoid

| Mistake | What goes wrong |
| --- | --- |
| Mixing up *multiple* and *hierarchical* | Remember: multiple = many parents, hierarchical = many children. |
| Writing one access specifier for several bases: `class C : public A, B` | Only `A` is public. `B` uses the default (private for `class`). Write `public` for each. |
| Calling `Human(...)` from `Manager` directly in multilevel | Each class only calls its **direct parent**. |
| Expecting base constructors in multiple inheritance to follow the order inside the `{ }` | The order follows the **inheritance list**. |
| Giving two parents the same function name and calling it on the child | **Ambiguity error** (see Lecture 3). |
| Building a diamond without a fix | Two copies of the top class, and ambiguous access. Use a virtual base class. |

---

## Key Points to Remember (Part 4)

- **Single:** one base → one derived.
- **Multilevel:** a chain. The grandchild gets features of both child and parent. Constructors run top to bottom (`Human → Employee → Manager`), and destructors run in reverse.
- **Multiple:** several bases → one derived, separated by commas. Base constructors run in the order they are **listed**.
- **Hierarchical:** one base → several derived. The children are independent of each other.
- **Hybrid:** a combination of the above. The diamond shape can give an object two copies of the same base, which causes ambiguity (fixed in Lecture 3).

---

# 📋 Full Recap:

```
┌──────────────────────────────────────────────────────────────────────────┐
│                           **INHERITANCE**                                │
├──────────────────────────────────────────────────────────────────────────┤
│  **WHAT**: child class acquires data + functions of parent class         │
│        class Derived : mode Base { ... };                                │
│  **WHY**:  code reusability, no duplication, foundation of polymorphism  │
│  **TERMS**: parent = base = super │ child = derived = sub                │
│                                                                          │
│  **ACCESS** (inside a class)                                             │
│    private → base only │ protected → base + derived │ public → all       │
│                                                                          │
│  **MODES** (can only keep or tighten access)                             │
│    public:    public→public,    protected→protected                      │
│    protected: public→protected, protected→protected                      │
│    private:   public→private,   protected→private                        │
│    private base members → never accessible in any mode                   │
│                                                                          │
│  **CONSTRUCTORS** / **DESTRUCTORS**                                      │
│    Constructors: Base → Derived │ Destructors: Derived → Base            │
│    Pass values up:  Derived(...) : Base(values) { ... }                  │
│                                                                          │
│  **TYPES**                                                               │
│    Single │ Multilevel │ Multiple │ Hierarchical │ Hybrid                │
└──────────────────────────────────────────────────────────────────────────┘
```