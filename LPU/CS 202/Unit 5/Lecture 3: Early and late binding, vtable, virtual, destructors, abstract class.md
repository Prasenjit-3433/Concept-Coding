# Lecture 3: Early/late binding, vtable, virtual, destructors, abstract class

# 🎯 Part 1: Early Binding and Late Binding

## 1. What Does "Binding" Mean?

When you write a function call like `a1->sound();`, the compiler must connect that call to the actual function code that will run. This connecting step is called **binding**.

```
   a1->sound();          ← a CALL
        │
        │   binding = "which exact function body runs for this call?"
        ▼
   void sound() { ... }   ← the FUNCTION CODE
```

The only question is **when** that connection is made.

```
┌─────────────────────────────┬──────────────────────────────────┐
│  EARLY BINDING              │  LATE BINDING                    │
│  (static binding)           │  (dynamic binding)               │
├─────────────────────────────┼──────────────────────────────────┤
│  Connected at COMPILE time  │  Connected at RUN time           │
│  Compiler decides.          │  Program decides while running.  │
│  Faster                     │  Slightly slower                 │
│  = compile-time             │  = run-time polymorphism         │
│    polymorphism             │                                  │
└─────────────────────────────┴──────────────────────────────────┘
```

> The compiler decided "too early" and chose `Animal::sound()` because the pointer's type was `Animal*`. That was **early binding**. The `virtual` keyword switches it to **late binding**.
> 

## 2. Which Calls Use Which Binding?

| Situation | Binding |
| --- | --- |
| Normal function call | Early |
| Function overloading | Early |
| Constructor overloading | Early |
| Operator overloading | Early |
| Non-virtual function called through a base pointer | Early (base version runs) |
| **Virtual** function called through a base pointer or reference | **Late** |
| Virtual function called directly on an object (`d.sound()`) | Early (the compiler already knows the exact type) |

The last row surprises many students. If you write `Dog d; d.sound();`, the compiler can see that `d` is a `Dog`, so there is nothing to postpone. Late binding only matters when the compiler **cannot know** the real type, which happens with pointers and references.

## 3. One Program Showing Both

```cpp
#include <iostream>
using namespace std;

class Animal {
public:
    void eat()           { cout << "Animal eats" << endl; }      // normal
    virtual void sound() { cout << "Animal sound" << endl; }     // virtual
};

class Dog : public Animal {
public:
    void eat()   { cout << "Dog eats" << endl; }
    void sound() { cout << "Bhow Bhow" << endl; }
};

int main() {
    Animal* a1 = new Dog();

    a1->eat();       // **EARLY binding** → Animal::eat()
    a1->sound();     // **LATE binding**  → Dog::sound()

    delete a1;
    return 0;
}
```

**Output:**

```
Animal eats
Bhow Bhow
```

```
a1->eat();
   compiler sees: a1 is Animal*, eat() is NOT virtual
   → fixes the call to Animal::eat() right now (compile time)

a1->sound();
   compiler sees: a1 is Animal*, sound() IS virtual
   → leaves the choice for run time
   → at run time the object is a Dog → Dog::sound()
```

> Both calls use the same pointer on the same object. The difference comes only from the word `virtual`.
> 

(This program has a hidden problem on the `delete a1;` line. Section 6 explains it.)

---

# 🎯 Part 2: How `virtual` Works Inside

You don't need this to write programs, but it explains *why* virtual calls are a little slower and why objects get a little bigger. Exams often ask about it.

> **Note:** the C++ standard does not force a particular method. The description below is the one almost every compiler uses (the **vtable** method).
> 

## 4. The vtable and the vptr

When a class has at least one virtual function, the compiler does two things:

1. It builds a **table of function addresses** for that class, called the **vtable** (virtual table). There is **one vtable per class**.
2. It adds a hidden pointer inside **every object** of that class, called the **vptr** (virtual pointer). The vptr points to that class's vtable.

```
   class Animal { virtual void sound(); };
   class Dog : public Animal { void sound(); };

   VTABLES (one per class, made at compile time)
   ┌──────────────────────┐        ┌──────────────────────┐
   │ Animal vtable        │        │ Dog vtable           │
   ├──────────────────────┤        ├──────────────────────┤
   │ sound → Animal::sound│        │ sound → Dog::sound   │
   └──────────────────────┘        └──────────────────────┘
              ▲                                ▲
              │                                │
   OBJECTS    │                                │
   ┌──────────────────┐             ┌──────────────────┐
   │ an Animal object │             │ a Dog object     │
   │  vptr ───────────┘             │  vptr ───────────┘
   │  (other data)    │             │  (other data)    │
   └──────────────────┘             └──────────────────┘
```

## 5. What Happens During `a1->sound();`

```
Animal* a1 = new Dog();
a1->sound();
   ↓
Step 1: go to the object a1 points to          (a Dog object)
Step 2: read its hidden vptr                   (points to Dog's vtable)
Step 3: look up "sound" in that vtable         (→ Dog::sound)
Step 4: jump to that function and run it
```

This lookup happens at **run time**, which is why it is called late binding. Compare with a normal call, where the compiler already wrote the function's address into the program.

### Proof: Objects Get Bigger

```cpp
#include <iostream>
using namespace std;

class A {                    // no virtual function
public:
    void f() { }
};

class B {                    // has a virtual function
public:
    virtual void f() { }
};

int main() {
    cout << sizeof(A) << endl;
    cout << sizeof(B) << endl;
    return 0;
}
```

**Typical output (on a 64-bit system):**

```
1
8
```

`A` is an empty class, so C++ gives it 1 byte (an object must have an address). `B` holds a hidden vptr, and a pointer takes 8 bytes on a 64-bit system. The exact numbers depend on your compiler, but `B` is always bigger.

| Cost of virtual | Why |
| --- | --- |
| Every object carries a **vptr** | Slightly more memory |
| Each virtual call needs a **table lookup** | Slightly slower |

This is why C++ does **not** make every function virtual automatically. You pay only for what you use.

### Two Rules That Follow From This

| Rule | Reason |
| --- | --- |
| A **constructor cannot be virtual** | The vptr is set up *by* the constructor, so it cannot already be used to pick the constructor. |
| `virtual` works only on **member functions** | Free functions have no object, so no vptr. |

# 🎯 Part 3: Virtual Destructors

---

## 6. The Hidden Problem From Section 3

Look again at this pattern, which appeared in Lecture 2's `Animal* a1 = new Dog();`:

```cpp
Animal* a1 = new Dog();
...
delete a1;         // ← what happens here?
```

Recall the destructor rule from **Unit 4, Lecture 2**: when an object is destroyed, the ***derived destructor* runs first, then the *base destructor***. But that is the rule when C++ *knows* the object is a Dog. Here the pointer says `Animal*`.

### Without a Virtual Destructor

```cpp
#include <iostream>
using namespace std;

class Animal {
public:
    Animal()  { cout << "Animal constructor" << endl; }
    ~Animal() { cout << "Animal destructor" << endl; }      // NOT virtual
};

class Dog : public Animal {
    int* data;
public:
    Dog() {
        data = new int[5];                                  // Dog owns **heap memory**
        cout << "Dog constructor" << endl;
    }
    ~Dog() {
        delete[] data;                                      // Dog cleans it up
        cout << "Dog destructor" << endl;
    }
};

int main() {
    Animal* a1 = new Dog();
    delete a1;
    return 0;
}
```

**Typical output:**

```
Animal constructor
Dog constructor
Animal destructor
```

`Dog destructor` **never ran**.

```
delete a1;
   ↓
Compiler sees: a1 is Animal*, ~Animal() is NOT virtual
   ↓
**EARLY binding** → only ~Animal() is called
   ↓
~Dog() is skipped, so  delete[] data;  never runs
   ↓
The int[5] block on the heap is never freed  →  **MEMORY LEAK**
```

```
   HEAP after delete a1;
   ┌───────────────────────┐
   │ Dog object  (freed)   │
   ├───────────────────────┤
   │ int[5] block          │  ← STILL THERE, nobody can reach it → leaked
   └───────────────────────┘
```

> **Important:** the C++ standard says that deleting a derived object through a base pointer, when the base destructor is not virtual, is **undefined behavior**. The leak above is the usual result, but anything could happen. Treat it as a bug.
> 

## 7. The Fix: Make the Base Destructor Virtual

```cpp
class Animal {
public:
    Animal()          { cout << "Animal constructor" << endl; }
    virtual ~Animal() { cout << "Animal destructor" << endl; }   // ← virtual
};
```

**Output now:**

```
Animal constructor
Dog constructor
Dog destructor
Animal destructor
```

```
delete a1;
   ↓
~Animal() is virtual → LATE binding
   ↓
Run time: the object is really a Dog
   ↓
Step 1: ~Dog() runs      → delete[] data; frees the heap block ✔
Step 2: ~Animal() runs   → base part is cleaned up
```

It is the usual order (derived first, base second) from Unit 4. The only change is that now the right destructor is found even through a base pointer.

> Notice `~Dog()` does not need the word `virtual`. Once the base destructor is virtual, every derived destructor is automatically virtual.
> 

## 8. The Golden Rule

```
┌──────────────────────────────────────────────────────────────┐
│  If a class has ANY virtual function (so it is meant to be   │
│  used through base pointers), give it a VIRTUAL DESTRUCTOR.  │
└──────────────────────────────────────────────────────────────┘
```

| Class situation | Destructor |
| --- | --- |
| Not meant to be a base class, no virtual functions | Normal destructor is fine |
| Has virtual functions / used through base pointers | **`virtual ~ClassName()`** |

## 9. Pure Virtual Destructor (Short Mention)

A destructor can also be pure virtual, which makes a class abstract even when it has no other virtual function:

```cpp
class Animal {
public:
    virtual ~Animal() = 0;           // pure virtual destructor
};

Animal::~Animal() { }                // MUST still have a body, defined outside
```

A pure virtual **destructor** needs a body, unlike an ordinary pure virtual function. The reason is that when a Dog is destroyed, the chain `~Dog()` → `~Animal()` still runs `~Animal()`, so the code must exist.

---

# 🎯 Part 4: Concrete Class vs Abstract Class

The syllabus names both. You already know the abstract class, so the concrete class is its opposite.

|  | Abstract class | Concrete class |
| --- | --- | --- |
| Pure virtual function? | **At least one** | **None** (or all of them have been overridden) |
| Can create objects? | ❌ No | ✅ Yes |
| Purpose | A **blueprint** for child classes | A **complete, usable** class |
| Example | `Animal` with `virtual void sound() = 0;` | `Dog`, `Cat` |

```
        ┌────────────────────────┐
        │  Animal (ABSTRACT)     │   sound() = 0
        └────────────────────────┘
              ▲              ▲
   ┌────────────────┐  ┌────────────────┐
   │ Dog (CONCRETE) │  │ Cat (CONCRETE) │   both give sound() a body
   └────────────────┘  └────────────────┘
```

```cpp
Animal a;       // ❌ Animal is abstract
Dog d;          // ✅ Dog is concrete
Cat c;          // ✅ Cat is concrete
```

> A class becomes concrete the moment **every** inherited pure virtual function has a body. A normal class you've written all course (like `Student` or `Employee`) is a concrete class too. It just never needed the name.
> 

### Why Use an Abstract Class At All?

```
   Shape (abstract)       area() = 0
      ▲        ▲
   Circle    Rectangle     each writes its own area()

   Shape* list[2] = { new Circle(...), new Rectangle(...) };
   for each shape:  list[i]->area();      ← the right area() runs each time
```

You can treat different objects the same way (call `area()` on all of them) while each one behaves correctly. That is the real power of runtime polymorphism.

---

## Common Mistakes to Avoid (Part 1)

| Mistake | What goes wrong |
| --- | --- |
| Forgetting a virtual destructor in a base class | `delete basePtr` skips the derived destructor, causing a leak (undefined behavior). |
| Writing `virtual` on the constructor | Not allowed. |
| Giving a pure virtual destructor no body | Linker error. |
| Thinking `virtual` makes `d.sound()` on a plain object late-bound | The compiler already knows the type there. |
| Thinking every class needs a virtual destructor | Only classes used as polymorphic bases. |
| Saying "the compiler uses a vtable" as if the standard demands it | It is the common implementation, not a rule. |

---

## Key Points to Remember

- **Binding** = connecting a function call to the function's code.
- **Early (static) binding** happens at **compile time**. **Late (dynamic) binding** happens at **run time**.
- Normal calls, overloading and operator overloading use **early binding**. Virtual calls through a **base pointer or reference** use **late binding**.
- Compilers usually implement late binding with a **vtable** (one per class) and a hidden **vptr** (one per object). A virtual call looks up the function in the table at run time.
- Virtual functions cost a little **memory** (the vptr) and a little **speed** (the lookup).
- **Constructors cannot be virtual.**
- If you `delete` a derived object through a **base pointer**, the base destructor must be **virtual**, or the derived destructor is skipped (undefined behavior, usually a leak).
- **Rule:** a class with virtual functions should have a **virtual destructor**.
- A **concrete class** can create objects. An **abstract class** (with a pure virtual function) cannot.