# Lecture 3: Inheritance - level 2

Date: October 27, 2026
Status: Done

# 🎯 Part 1: Overriding Member Functions & Resolving Ambiguities

---

## 1. Overriding Member Functions

### The Problem

Take an `Animal` class with a `sound()` function. Every animal makes a sound, but a `Dog` barks and a `Cat` meows. The inherited `sound()` is too generic for them.

```cpp
class Animal {
public:
    void sound() {
        cout << "Some generic animal sound" << endl;
    }
};

class Dog : public Animal { };

int main() {
    Dog d;
    d.sound();      // prints the **generic sound**, not what we want for a Dog
}
```

### The Solution

> **Function overriding** means the derived class writes its own version of a function with the **same name, same parameters and same return type** as the base class function. The derived version replaces the inherited one for objects of the derived class.
> 

```cpp
#include <iostream>
using namespace std;

class Animal {
public:
    void sound() {
        cout << "Some generic animal sound" << endl;
    }
};

class Dog : public Animal {
public:
    void sound() {                       // same name and signature as Animal::sound()
        cout << "Dog barks" << endl;
    }
};

class Cat : public Animal {
public:
    void sound() {
        cout << "Cat meows" << endl;
    }
};

int main() {
    Animal a;
    Dog d;
    Cat c;

    a.sound();
    d.sound();
    c.sound();
    return 0;
}
```

**Output:**

```
Some generic animal sound
Dog barks
Cat meows
```

### How the Compiler Chooses

```
d.sound();
   ↓
The compiler looks for sound() in Dog FIRST.
   ├── Found in Dog?  → use it, and STOP looking.
   └── Not found?     → go up to the parent (Animal) and look there.
```

> The search always starts at the **object's own class** and moves **upward**. The nearest version wins.
> 

### 🧠`Overriding` vs `Overloading`

Two ideas that sound alike and are often confused (Functions lecture, Unit 0):

| Feature | Overloading | Overriding |
| --- | --- | --- |
| Where | **Same class** | **Base and derived** classes |
| Function name | Same | Same |
| Parameters | **Different** | **Same** |
| Needs inheritance? | No | **Yes** |
| Example | `sum(int, int)` and `sum(double, double)` | `Animal::sound()` and `Dog::sound()` |

### Calling the Base Version Using `::`

After overriding, the base version is hidden but not gone. Use the **scope resolution operator** (`::`, from Unit 2) to reach it:

```cpp
class Dog : public Animal {
public:
    void sound() {
        Animal::sound();                 // call the **parent's version** first
        cout << "Dog barks" << endl;     // then add the Dog-specific part
    }
};
```

**Output of `d.sound()`:**

```
Some generic animal sound
Dog barks
```

You can also call it from outside, on an object:

```cpp
Dog d;
d.Animal::sound();       // forces the base version, even on a Dog
```

```
   d.sound()          → Dog's own version
   d.Animal::sound()  → Animal's version, chosen explicitly
```

> **Why is this useful?** Often the derived version wants to do what the base does, and then a bit more. Writing `Base::function()` reuses the base logic instead of copying it.
> 

### A Gotcha: Name Hiding

If the derived class defines **any** function with the same name, **all** base functions with that name are hidden, even if their parameters differ.

```cpp
class Base {
public:
    void show()      { cout << "Base show()" << endl; }
    void show(int x) { cout << "Base show(int)" << endl; }
};

class Derived : public Base {
public:
    void show() { cout << "Derived show()" << endl; }
};

int main() {
    Derived d;
    d.show();          // ✅ **Derived's** version
    // d.show(5);      // ❌ **ERROR**: Base::show(int) is hidden by Derived::show()
    d.Base::show(5);   // ✅ **works**: reach it with ::
}
```

### A Note on What Comes Next

The overriding here is decided at **compile time**. If a base-class pointer points to a derived object, C++ would call the **base** version. Making C++ pick the derived version at **run time** needs `virtual` functions. That is **runtime polymorphism**, the next topic in the course (Polymorphism). For now, remember that overriding works when you call the function **through an object of the derived class**.

---

## 2. Resolving Ambiguities in Inheritance

### What Is an Ambiguity?

> An **ambiguity** happens when the compiler finds **more than one possible match** for a name and cannot decide which one you mean.
> 

It mostly appears in **multiple inheritance** (Lecture 2, Part 4).

### `Ambiguity Case 1`: Two Parents, Same Function Name

```cpp
#include <iostream>
using namespace std;

class A {
public:
    void show() { cout << "Class A" << endl; }
};

class B {
public:
    void show() { cout << "Class B" << endl; }
};

class C : public A, public B { };

int main() {
    C obj;
    obj.show();      // ❌ ERROR: ambiguous. A::show() or B::show()?
    return 0;
}
```

```
                 ┌───────────┐   ┌───────────┐
                 │ A: show() │   │ B: show() │
                 └───────────┘   └───────────┘
                        ▲           ▲
                         \         /
                       ┌─────────────┐
      obj.show() ───▶  │      C      │  ← which show() ??? the compiler refuses
                       └─────────────┘     to guess
```

C++ refuses to guess because a wrong guess would silently call the wrong code.

### `Fix 1`: Use the Scope Resolution Operator

Say exactly which parent you mean:

```cpp
int main() {
    C obj;
    obj.A::show();      // Class A
    obj.B::show();      // Class B
    return 0;
}
```

### `Fix 2`: Override the Function in the Child

Define `show()` inside `C`. The compiler finds `C::show()` first (the "nearest wins" rule from Section 1), so there is no ambiguity. You can call both parents inside it:

```cpp
class C : public A, public B {
public:
    void show() {
        A::show();
        B::show();
    }
};

int main() {
    C obj;
    obj.show();       // ✅ no ambiguity now
}
```

**Output:**

```
Class A
Class B
```

> **Which fix to use?** Use Fix 1 when the caller should choose. Use Fix 2 when the class should decide the behavior itself. Fix 2 is usually the cleaner design.
> 

### `Ambiguity Case 2`: Two Parents, Same Data Member

The same problem occurs with data:

```cpp
class A { public: int x; };
class B { public: int x; };
class C : public A, public B { };

int main() {
    C obj;
    // obj.x = 10;     // ❌ **ERROR**: ambiguous
    obj.A::x = 10;     // ✅ A's x
    obj.B::x = 20;     // ✅ B's x
}
```

```
   C object in memory:
   ┌─────────────────────┐
   │ A part:  x = 10     │
   │ B part:  x = 20     │   ← TWO different x variables inside one object
   └─────────────────────┘
```

### 💎`Ambiguity Case 3`: The Diamond

When a class inherits the ***same grandparent twice*** through two parents, even a single `name` becomes ambiguous. We saw this as a teaser in Lecture 2, Part 4. Part 2 below solves it with `virtual`.

### When Is There *No* Ambiguity?

| Situation | Ambiguous? |
| --- | --- |
| Parent and child both have `show()` | ❌ No. The child's version wins (overriding). |
| Two parents have `show()`, child defines its own `show()` | ❌ No. The child's version wins. |
| Two parents have `show()`, child does not | ✅ **Yes.** |
| Two parents have **different** names (`showA`, `showB`) | ❌ No. |

---

## Common Mistakes to Avoid (Part 1)

| Mistake | What goes wrong |
| --- | --- |
| Changing the parameters and expecting overriding | That is **overloading**, and the base version gets hidden. |
| Forgetting `Base::` and calling `sound()` inside `Dog::sound()` | Infinite recursion. The function calls itself forever. |
| Expecting a base-class pointer to call the derived version | Needs `virtual` (next topic). |
| Calling an ambiguous function without `::` | Compiler error. |
| Fixing ambiguity by deleting a function from one parent | Better to use `::` or override in the child. |

---

## Key Points to Remember (Part 1)

- **Overriding** = derived class redefines a base function with the **same name and parameters**.
- The compiler searches from the **object's own class upward**, and the nearest version wins.
- Reach the base version with **`Base::function()`**, inside the derived class or from outside on an object.
- **Overloading** is within one class with different parameters. **Overriding** is across base and derived classes with the same parameters.
- A same-named function in the child **hides all** base functions of that name.
- An **ambiguity** occurs when two parents both provide the same name. Fix it with `obj.A::name`, or by overriding in the child.

# 🎯 Part 2: Virtual Base Class (The `Diamond Problem`)

---

## 1. Recreating the Problem

From Lecture 2, Part 4 (hybrid inheritance), we built this shape:

```
              ┌──────────┐
              │  Human   │
              └──────────┘
               ▲        ▲
              /          \
      ┌──────────┐    ┌──────────┐
      │ Employee │    │ Student  │
      └──────────┘    └──────────┘
               ▲        ▲
                \      /
          ┌────────────────────┐
          │ TeachingAssistant  │     (a student who also works as a TA)
          └────────────────────┘
```

A teaching assistant is **both** an Employee and a Student, and both of those are Humans.

```cpp
#include <iostream>
#include <string>
using namespace std;

class Human {
public:
    string name;
    Human() { cout << "Human constructor" << endl; }
};

class Employee : public Human {
public:
    Employee() { cout << "Employee constructor" << endl; }
};

class Student : public Human {
public:
    Student() { cout << "Student constructor" << endl; }
};

class TeachingAssistant : public Employee, public Student {
public:
    TeachingAssistant() { cout << "TeachingAssistant constructor" << endl; }
};

int main() {
    TeachingAssistant ta;
    // ta.name = "Riya";      // ❌ **ERROR**: ambiguous
    return 0;
}
```

**Output:**

```
Human constructor
Employee constructor
Human constructor
Student constructor
TeachingAssistant constructor
```

### What Went Wrong?

`Human` was built **twice**, once inside `Employee` and once inside `Student`:

```
   TeachingAssistant object in memory:

   ┌──────────────────────────────────────┐
   │ **Employee** part                    │
   │   ┌──────────────────────────────┐   │
   │   │ Human part:  name  (copy 1)  │   │
   │   └──────────────────────────────┘   │
   ├──────────────────────────────────────┤
   │ **Student** part                     │
   │   ┌─────────────────────────────┐    │
   │   │ Human part:  name  (copy 2) │    │
   │   └─────────────────────────────┘    │
   └──────────────────────────────────────┘
```

Two problems follow:

| Problem | Why it matters |
| --- | --- |
| **Ambiguity** | `ta.name` could mean copy 1 or copy 2, so the compiler gives an error. |
| **Wasted memory and inconsistent data** | A person should have **one** name, but the object holds two. One could say "Riya" and the other stay empty. |

This is called the **`diamond problem`**, named after the diamond shape of the diagram.

You could patch it with `ta.Employee::name = "Riya"`, but that sets only one copy, and the other copy is still there. We want **one** shared `Human` part.

---

## 2. The Solution: `Virtual Base Class`

> A **virtual base class** makes sure that, no matter how many paths lead to it, the derived object contains **only one copy** of that base class.
> 

### Syntax

Add the keyword **`virtual`** when the middle classes inherit from the top class:

```cpp
class Employee : virtual public Human { ... };
class Student  : virtual public Human { ... };
```

```
   class Employee : **virtual** public Human
                      │       │
                      │       └── the usual access mode
                      └────────── "**share** this base with others"
```

(`virtual public` and `public virtual` both work.)

### The Fixed Program

```cpp
#include <iostream>
#include <string>
using namespace std;

class Human {
public:
    string name;
    Human() { cout << "Human constructor" << endl; }
};

class Employee : **virtual** public Human {         // ← **virtual**
public:
    Employee() { cout << "Employee constructor" << endl; }
};

class Student : **virtual** public Human {          // ← **virtual**
public:
    Student() { cout << "Student constructor" << endl; }
};

class TeachingAssistant : public Employee, public Student {
public:
    TeachingAssistant() { cout << "TeachingAssistant constructor" << endl; }
};

int main() {
    TeachingAssistant ta;
    ta.name = "Riya";                    // ✅ **works**: only one name exists
    cout << ta.name << endl;
    return 0;
}
```

**Output:**

```
Human constructor
Employee constructor
Student constructor
TeachingAssistant constructor
Riya
```

```
   **TeachingAssistant** object in memory (with virtual):

   ┌───────────────────────────────────┐
   │ **Employee** part                 │──┐
   ├───────────────────────────────────┤  │  both share
   │ **Student** part                  │──┤  the SAME
   ├───────────────────────────────────┤  │  **Human** part
   │ **Human** part (ONE copy): name   │◀─┘
   └───────────────────────────────────┘
```

`Human` constructor ran **once**, and `ta.name` is unambiguous.

### Before and After

|  | Without `virtual` | With `virtual` |
| --- | --- | --- |
| Copies of `Human` in the object | **2** | **1** |
| `ta.name` | ❌ Ambiguous | ✅ Works |
| `Human` constructor runs | Twice | **Once** |
| Constructor output | `Human, Employee, Human, Student, TA` | `Human, Employee, Student, TA` |

---

## 3. Constructor Order With a Virtual Base

Two rules to remember:

> **`Rule 1`:** **Virtual base classes are constructed first**, before any non-virtual base.
> 
> 
> **`Rule 2`:** The **most derived class** (here `TeachingAssistant`) is responsible for constructing the virtual base.
> 

After the virtual base, the remaining bases are built in the order they appear in the inheritance list, then the derived class itself:

```
TeachingAssistant ta;
   ① Human()                 ← virtual base, built first
   ② Employee()              ← listed first
   ③ Student()               ← listed second
   ④ TeachingAssistant()     ← the class itself
```

Destructors run in exactly the **reverse** order.

### When Human Needs Values

With a parameterized `Human(string name)`, the **most derived class** must call it directly:

```cpp
class Human {
public:
    string name;
    Human(string name) { this->name = name; }
};

class Employee : **virtual** public Human {
public:
    Employee(string n) : Human(n) { }
};

class Student : **virtual** public Human {
public:
    Student(string n) : Human(n) { }
};

class TeachingAssistant : public Employee, public Student {
public:
    TeachingAssistant(string n)
        : Human(n), Employee(n), Student(n) { }     // ← **Human** called DIRECTLY
};
```

```
   **Normal inheritance**:  each class only calls its DIRECT parent.
   **Virtual base**:        the **MOST DERIVED** class calls the virtual base itself.
```

> **Why?** `Employee` and `Student` would otherwise both try to set up `Human`, and there is only one `Human`. C++ settles it by letting only the **most derived class** do it. The `Human(n)` calls inside `Employee` and `Student` are **ignored** when constructing a `TeachingAssistant`. They only apply when someone creates a plain `Employee` or `Student`.
> 

This is the one exception to the "only call your direct parent" rule from Lecture 2, Part 3.

---

## 4. When to Use Virtual Inheritance

| Situation | Use `virtual`? |
| --- | --- |
| Simple single, multilevel or hierarchical inheritance | ❌ No |
| Multiple inheritance with **different** parents | ❌ No |
| Diamond: two parents share the **same** grandparent | ✅ **Yes** |

It is a small but real cost, so add it only when you have a diamond.

---

## 5. Common Mistakes to Avoid

| Mistake | What goes wrong |
| --- | --- |
| Adding `virtual` only to **one** of the middle classes | The diamond is **not** fixed. Both `Employee` and `Student` need `virtual`. |
| Putting `virtual` on `TeachingAssistant : virtual public Employee, ...` | Wrong place. It goes where the **shared** base is inherited (the middle classes). |
| Not calling the virtual base's parameterized constructor from the **most derived** class | Compiler error. |
| Expecting `Employee`'s `Human(n)` call to run when building a `TA` | It is ignored. The `TA` constructor decides. |
| Using `virtual` everywhere "just in case" | Unneeded complexity. |
| Forgetting that without `virtual`, `Human` is built twice | Two `name` copies and ambiguity. |

---

## Key Points to Remember (Part 2)

- The **diamond problem**: when a class inherits the same base through two paths, it gets **two copies** of it, which causes **ambiguity** and **wasted, inconsistent data**.
- The fix is **`virtual` inheritance**: `class Employee : virtual public Human`, written in **both** middle classes.
- A **virtual base class** is shared, so the final object holds **exactly one** copy.
- **Virtual bases are constructed first**, then the other bases in their listed order, then the class itself. Destructors run in reverse.
- The **most derived class** calls the virtual base's constructor directly. The middle classes' calls to it are ignored.
- Use `virtual` **only** for the diamond.

# 🎯 Part 3: Aggregation and Composition (The **`"Has-A"`** Relationship)

---

## 1. Where This Fits

In Lecture 2, Part 1, we introduced the ***"is-a"* test** to decide whether inheritance fits:

| Sentence | Use inheritance? |
| --- | --- |
| An Employee **is a** Human | ✅ Yes |
| A Car **is an** Engine | ❌ No. A car **has an** engine. |

Many beginners overuse inheritance. But there is a second way to build a class out of other classes: **instead of inheriting from a class, keep an object of that class inside your class as a data member.**

```
┌──────────────────────────────────────────────────────────────┐
│   **IS-A**   →  INHERITANCE        Dog is an Animal          │
│   **HAS-A**  →  AGGREGATION        Department has Teachers   │
│             COMPOSITION        Car has an Engine             │
└──────────────────────────────────────────────────────────────┘
```

> The two "has-a" ideas, aggregation and composition, are collectively called **object composition** or **containership** in many books. The syllabus lists both, so we separate them carefully.
> 

---

## 2. The Basic Idea: An Object Inside a Class

You have already done this. A class can have a `string` or an `int` as a member (Unit 2). A class can equally have an object of **another class** as a member:

```cpp
class Engine {
public:
    void start() { cout << "Engine started" << endl; }
};

class Car {
public:
    Engine engine;          // ← an Engine OBJECT inside Car
    void drive() {
        engine.start();     // Car uses its Engine
        cout << "Car is moving" << endl;
    }
};
```

```
        ┌──────────────── Car ──────────────────┐
        │   ┌──────────┐                        │
        │   │  Engine  │  ← a part of the car   │
        │   └──────────┘                        │
        └───────────────────────────────────────┘
```

Notice that `Car` does **not** inherit from `Engine`. It **contains** one and calls its functions. This is the "has-a" relationship.

Whether it is **composition** or **aggregation** depends on one question: **who owns the part, and does the part live independently?**

---

## 3. Composition: A Strong `"Has-A"`

> **Composition** means the whole **owns** the part. The part is created **with** the whole and destroyed **with** the whole. The part cannot exist on its own.
> 

Think of a **Human and a Heart**. A heart is created as part of a person, and when the person ceases to exist, that heart ceases to exist with them. It doesn't live on separately in your program.

```
   **COMPOSITION**  (*filled* **diamond ◆** in UML)

   ┌─────────┐             ┌─────────┐
   │   Car   │ ◆─────────  │ Engine  │
   └─────────┘             └─────────┘

   Car dies  →  Engine dies with it.
```

### How It Looks in Code

In composition, the part is a **normal member object** (not a pointer). It is built automatically when the whole is built.

```cpp
#include <iostream>
using namespace std;

class Engine {
public:
    Engine()  { cout << "Engine created" << endl; }
    ~Engine() { cout << "Engine destroyed" << endl; }
};

class Car {
    Engine engine;                  // member object → **composition**
public:
    Car()  { cout << "Car created" << endl; }
    ~Car() { cout << "Car destroyed" << endl; }
};

int main() {
    Car c;
    return 0;
}
```

**Output:**

```
Engine created
Car created
Car destroyed
Engine destroyed
```

### Tracing the Order

```
Car c;
   ↓
Member objects are built **BEFORE** the constructor body runs:
   ① Engine()        ← the part is built first
   ② Car()           ← then the whole's body
   ↓
c goes out of scope:
   ③ ~Car()          ← the whole's body first
   ④ ~Engine()       ← then the part is destroyed
```

This mirrors the inheritance order from Lecture 2, Part 3: **parts are built first and destroyed last**. We never wrote `delete` for `engine`. Because it lives inside `Car`, it dies with `Car` automatically.

### Passing Values to the Part

When the part's constructor needs values, use the **initializer list** (Unit 3), just as we did for base classes:

```cpp
class Engine {
    int horsepower;
public:
    Engine(int hp) { horsepower = hp; }
};

class Car {
    Engine engine;
public:
    Car(int hp) : engine(hp) { }      // initialize the member object
};
```

```
   Inheritance:   Derived(...) : Base(values)       ← names the CLASS
   Composition:   Whole(...)   : memberName(values) ← names the MEMBER
```

---

## 4. Aggregation: A Weak `"Has-A"`

> **Aggregation** means the whole **uses** the part, but does **not own** it. The part is created **independently**, can be shared, and **keeps living** after the whole is gone.
> 

Think of a **Department and its Teachers**. A teacher exists on their own. If the department is closed, the teacher doesn't vanish. They can join another department.

```
   AGGREGATION  (*hollow* **diamond ◇** in UML)

   ┌────────────┐             ┌──────────┐
   │ Department │ ◇─────────  │ Teacher  │
   └────────────┘             └──────────┘

   Department closes  →  Teacher still exists.
```

### How It Looks in Code

In aggregation, the whole keeps a **pointer or reference** to an object that was created **elsewhere** (Unit 2, Pointers). The whole doesn't build or delete it.

```cpp
#include <iostream>
#include <string>
using namespace std;

class Teacher {
public:
    string name;
    Teacher(string n) : name(n) {
        cout << "Teacher " << name << " created" << endl;
    }
    ~Teacher() {
        cout << "Teacher " << name << " destroyed" << endl;
    }
};

class Department {
    Teacher* teacher;               // pointer → just points, doesn't own
public:
    Department(Teacher* t) : teacher(t) {
        cout << "Department created" << endl;
    }
    ~Department() {
        cout << "Department destroyed" << endl;
        // NO delete teacher; : this class doesn't own it
    }
    void show() {
        cout << "Teacher: " << teacher->name << endl;
    }
};

int main() {
    Teacher t("Aman");              // created independently, in main

    {
        Department d(&t);           // d points to t
        d.show();
    }                               // d is destroyed here

    cout << "Teacher still alive: " << t.name << endl;
    return 0;
}
```

**Output:**

```
Teacher Aman created
Department created
Teacher: Aman
Department destroyed
Teacher still alive: Aman
Teacher Aman destroyed
```

### Tracing It

```
Teacher t("Aman");       → teacher exists in main, on its own
   ↓
Department d(&t);        → d stores t's ADDRESS (a pointer, not a copy)
   ↓
inner } closes           → ~Department() runs
                           Teacher is NOT touched
   ↓
cout << t.name           → ✅ still works: the teacher outlived the department
   ↓
main ends                → ~Teacher() runs (main's own object)
```

```
        main's scope
   ┌────────────────────────────────┐
   │  Teacher t  ◀────────┐         │
   │                      │ points  │
   │   ┌── Department d ──┘         │
   │   │   (shorter life)           │
   │   └──                          │
   └────────────────────────────────┘
   The Teacher outlives the Department.
```

> **Caution:** if the pointer's target were destroyed before the whole, the pointer would **dangle** (Unit 5). Aggregation requires that the part outlives its use.
> 

---

## 5. Composition vs Aggregation

| Feature | **Composition** | **Aggregation** |
| --- | --- | --- |
| Strength | Strong "has-a" (**owns**) | Weak "has-a" (**uses**) |
| Life of the part | Born and dies **with** the whole | **Independent** of the whole |
| Part created | Inside the whole | Outside, then handed in |
| Typically stored as | A **member object** | A **pointer or reference** |
| Can the part be shared? | No | **Yes** (many wholes, one part) |
| Real example | Car–Engine, Human–Heart, House–Room | Department–Teacher, Team–Player, Library–Book |
| UML symbol | Filled diamond ◆ | Hollow diamond ◇ |

```
┌───────────────────────────────────────────────────────────────────┐
│  QUICK TEST:  "If I delete the whole, should the part             │
│                still exist?"                                      │
│                                                                   │
│      NO   → COMPOSITION       (heart dies with the human)         │
│      YES  → AGGREGATION       (teacher outlives department)       │
└───────────────────────────────────────────────────────────────────┘
```

---

## 6. Inheritance vs Composition: Choosing the Right Tool

| Question | Answer | Use |
| --- | --- | --- |
| Is the child **a kind of** the parent? | Employee is a Human | **Inheritance** |
| Does the class **contain** another as a part? | Car has an Engine | **Composition** |
| Does the class **use** an independent object? | Department has Teachers | **Aggregation** |

> A well-known guideline: **prefer composition over inheritance** when both seem possible. Inheritance ties the child tightly to the parent, since any change in the parent affects every child. Composition keeps classes more independent. Use inheritance only when the "is-a" statement is truly natural.
> 

### Example: The Wrong Way and the Right Way

```cpp
class Engine { public: void start() { } };

class Car : public Engine { };    // ❌ WRONG: a Car is NOT an Engine
```

```cpp
class Car {
    Engine engine;                 // ✅ RIGHT: a Car HAS an Engine
public:
    void drive() { engine.start(); }
};
```

---

## 7. Common Mistakes to Avoid

| Mistake | What goes wrong |
| --- | --- |
| Using inheritance for "has-a" (`Car : public Engine`) | Wrong design. A Car would pretend to be an Engine. |
| Using `delete` on an aggregated pointer inside the whole's destructor | Destroys an object the whole doesn't own, so other users get a dangling pointer. |
| Using a pointer for composition and forgetting `delete` | Memory leak (Unit 5). Prefer a plain member object. |
| Passing values to a member object inside the constructor **body** | Too late. Use the initializer list: `Car(int hp) : engine(hp)`. |
| Expecting aggregation's part to be built by the whole | The part is created outside and passed in. |
| Confusing the two | Ask: "does the part outlive the whole?" |

---

## Key Points to Remember (Part 3)

- **`Is-a`** → inheritance. **`Has-a`** → aggregation or composition (an object inside another class).
- **Composition:** the whole **owns** the part. The part is a **member object**, created and destroyed **with** the whole.
- **Aggregation:** the whole **uses** a part that lives **independently**, usually through a **pointer or reference**. It never deletes the part.
- With composition, **members are built before** the whole's constructor body and **destroyed after** its destructor body.
- Pass values to a member object with the **initializer list**: `Car(int hp) : engine(hp)`.
- Quick test: delete the whole, should the part still exist? **No** → composition. **Yes** → aggregation.
- Prefer composition over inheritance when "is-a" isn't clearly true.