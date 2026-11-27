# Lecture 2: Polymorphism in C++



# 🎯 Part 1: What Is Polymorphism? (Compile Time)

---

## 1. The Meaning

The word breaks into two parts:

```
   POLY      +      MORPHISM
    │                  │
   many              forms

   Polymorphism = "a thing that can take many forms"
```

You have already met this idea in the **Functions lecture (Unit 0)**:

```cpp
#include <iostream>
using namespace std;

void sum(int x, int y) {
   cout << x + y;
}

void sum(double x, double y) {
   cout << x + y;
}

void sum(int x, int y, int z) {
   cout << x + y + z;
}

int main() {
    
    sum(10, 15);
    sum(11.5, 26.5);
    sum(10, 20, 30);
    
    return 0;
}
```

```
   sum(int, int)          → adds 2 integers
   sum(double, double)    → adds 2 decimals
   sum(int, int, int)     → adds 3 integers

   ONE name, MANY forms.
```

The name is the same, but each version does its own job. That is **polymorphism**.

## 2. The Two Types

```
┌──────────────────────────────────────────────────────────────┐
│                      POLYMORPHISM                            │
├───────────────────────────────┬──────────────────────────────┤
│  **COMPILE TIME**             │  **RUN TIME**                │
│  (static polymorphism)        │  (dynamic polymorphism)      │
│                               │                              │
│  Decided while the program    │  Decided while the program   │
│  is being COMPILED            │  is RUNNING                  │
│                               │                              │
│  • Function overloading       │  • Function overriding       │
│  • Constructor overloading    │    + virtual functions       │
│  • Operator overloading       │                              │
└───────────────────────────────┴──────────────────────────────┘
```

> **Instructor's idea:** in compile time polymorphism, the compiler itself decides which form to run. In run time polymorphism, that decision is postponed until the program is actually running.
> 

---

## 3. Compile Time: `Function Overloading`

This is the example the instructor typed. A class has three `sum` functions that differ in their inputs:

```cpp
#include <iostream>
using namespace std;

class Calculator {
public:
    void sum(int a, int b) {
        cout << a + b << endl;
    }

    void sum(double a, double b) {
        cout << a + b << endl;
    }

    void sum(int a, int b, int c) {
        cout << a + b + c << endl;
    }
};

int main() {
    Calculator c1;
    c1.sum(3, 4, 5);        // 12
    c1.sum(2, 3);            // 5
    c1.sum(2.5, 3.5);        // 6
    return 0;
}
```

**Output:**

```
12
5
6
```

### How the Compiler Chooses

```
c1.sum(3, 4, 5);
   ↓
The compiler reads the call while compiling:
   "3 arguments, all int"
   ↓
Looks at the three sum() versions:
   sum(int, int)          ✗  (2 parameters)
   sum(double, double)    ✗  (2 parameters)
   sum(int, int, int)     ✔  matches!
   ↓
The decision is made BEFORE the program runs.
```

> Since the choice is made during compilation, this is **compile time polymorphism**.
> 

**Rule (same class):** several functions with the **same name inside the same class**, but **different parameters**, is **function overloading**.

## 4. Compile Time: `Constructor Overloading`

A constructor is also a function, so it can be overloaded too. We did this in **Unit 3** (`Sum(int,int)`, `Sum(int,int,int)`). Which constructor runs is again decided at compile time, by the arguments you pass. Nothing new here.

## 5. Compile Time: `Operator Overloading`

Recall from **Unit 4, Lecture 1** that the same `+` symbol behaves differently for different types:

```
   5 + 10                 → 15              (adds two ints)
   "Learn" + " & Fun"       → "Learn & Fun"     (joins two strings)
   c1 + c2                → ???             (we can teach it!)
```

The instructor used **complex numbers** to show this. A complex number has a **real part** and an **imaginary part**:

```
   3 + 4i    → real = 3, imaginary = 4
   4 + 5i    → real = 4, imaginary = 5

   Adding:  (3 + 4i) + (4 + 5i)  =  (3+4) + (4+5)i  =  7 + 9i
```

### The Key Insight From the Video

The instructor reached operator overloading through something you already know. Look at this line:

```cpp
c1.fn(c2);      // object c1 calls a function and passes c2 into it
```

This is perfectly normal (we did the same with the copy constructor). Now replace the function name with an operator:

```
   c1.fn(c2)       can be seen as       c1 + c2
       │                                   │
       └─ function name                    └─ the operator IS the function name
```

So `c1 + c2` means: **`c1` calls the `+` function, and `c2` is passed in as the parameter.**

### The Code

```cpp
#include <iostream>
using namespace std;

class Complex {
public:
    int real, imag;

    Complex(int real = 0, int imag = 0) {      // constructor with default arguments
        this->real = real;
        this->imag = imag;
    }

    // "+" written as a FUNCTION. Return type is Complex.
    Complex operator+(Complex c2) {
        Complex answer;
        answer.real = real + c2.real;          // real       = c1's real
        answer.imag = imag + c2.imag;          // c2.real    = c2's real
        return answer;
    }

    void getInfo() {
        cout << real << " + " << imag << "i" << endl;
    }
};

int main() {
    Complex c1(3, 4);
    Complex c2(4, 5);

    c1.getInfo();            // 3 + 4i
    c2.getInfo();            // 4 + 5i

    Complex c3 = c1 + c2;    // c1.operator+(c2)
    c3.getInfo();            // 7 + 9i
    return 0;
}
```

**Output:**

```
3 + 4i
4 + 5i
7 + 9i
```

### Tracing Through It

```
Complex c3 = c1 + c2;
   ↓
Compiler turns it into:   c1.operator+(c2)
   ↓
Inside the function:
      "this" → c1           (real = 3, imag = 4)
      c2     → c2           (real = 4, imag = 5)
   ↓
answer.real = 3 + 4 = 7
answer.imag = 4 + 5 = 9
   ↓
return answer   → c3 receives it
```

### Why the Instructor Used `this->real` vs `c2.real`

> The function was called by `c1`, so plain `real` (or `this->real`) means **c1's** real part. To reach **c2's** real part, you must write `c2.real` clearly.
> 

### The Compile Error the Instructor Hit

When he first wrote `Complex answer;`, the compiler complained, because he had only written a constructor that takes **two values**. `Complex answer;` supplies **no** values, and no matching constructor existed. This is the same rule from **Unit 3**: once you write any constructor yourself, C++ stops giving you the free empty one.

```
   Fix: give the constructor DEFAULT ARGUMENTS
        Complex(int real = 0, int imag = 0)
        → now it works with 0, 1 or 2 arguments
```

(The instructor's PDF note uses exactly this default-argument version.)

> The full details of operator overloading (unary, binary, friend functions, `<<` and `>>`, type conversion) are in **Unit 4, Lecture 1**. Here we only need to see that it is another case of "one symbol, many forms, decided at compile time."
> 

# 🎯 Part 2: Run Time Polymorphism

---

## 6. The Idea

> **Run time polymorphism:** the program decides **while running** which version of a function to execute.
> 

The instructor builds this step by step with animals. We'll follow the same steps.

## 7. Step 1: Function Overriding

```cpp
#include <iostream>
using namespace std;

class Animal {
public:
    void sound() {
        cout << "Generic Animal sound" << endl;
    }
};

class Dog : public Animal {
public:
    void sound() {                    // same name as Animal::sound()
        cout << "Bhow Bhow" << endl;
    }
};

int main() {
    Dog d1;
    d1.sound();
    return 0;
}
```

**Output:**

```
Bhow Bhow
```

A `Dog` object now has **two** `sound()` functions: its own and the one inherited from `Animal`.

```
        ┌─────────── d1 (a Dog) ───────────┐
        │  Animal part:  sound() "Animal sound"
        │  Dog part:     sound() "Bhow Bhow"   ← this one is used
        └───────────────────────────────────┘
```

The instructor warned: don't apply the *constructor* thinking here (where both parent and child run). For a **normal function**, only one version runs. The child's version is chosen, and we say **`Dog::sound()` overrides `Animal::sound()`**.

### Overloading vs Overriding

|  | Function Overloading | Function Overriding |
| --- | --- | --- |
| Where | **Same class** | **Different classes** (parent and child) |
| Name | Same | Same |
| Parameters | **Different** | Same (same signature) |
| Type of polymorphism | Compile time | Run time |

> The instructor's definition: *when a derived class defines a function with the same name and signature as the base class, that is function overriding.* The word "signature" means the parameters must match too, as we saw in **Unit 4, Lecture 3**.
> 

## 8. Step 2: The Problem (Base Pointer)

So far everything worked. The trouble starts when we use a **pointer**.

You know from the Pointers and Dynamic Memory lectures that a pointer can hold an address. A class is also a data type, so you can make a pointer of the class type. And here is the new fact:

> **A base class pointer can hold the address of a derived class object.** A `Dog` *is an* `Animal`, so an `Animal*` is allowed to point to a `Dog`.
> 

```cpp
int main() {
    Animal* a1 = new Dog();      // Animal pointer, Dog object created on the heap
    a1->sound();
    return 0;
}
```

Now the question: this object is a Dog, so which `sound()` should run?

**Output:**

```
Animal sound
```

Even though the object is a Dog, the **Animal** version ran. That is not what we wanted.

```
        **STACK**                         **HEAP**
   ┌──────────────┐             ┌─────────────────┐
   │ a1 (Animal*) │ ──────────▶ │  a Dog object   │
   └──────────────┘             └─────────────────┘

   We expect:  "Bhow Bhow"   (the object is a Dog)
   We get:     "Animal sound"
```

### Why Did This Happen?

The compiler reads the code **line by line**:

```
Line 1:  Dog d1;           → compile time: a Dog is created
Line 2:  d1.sound();       → compile time: d1 is a Dog → Dog's sound()  ✔

Line 3:  Animal* a1 = new Dog();
              │        │
              │        └─ the memory is given at RUN time
              └─ the TYPE of the pointer (Animal*) is known at *COMPILE time*

Line 4:  a1->sound();
         Compiler thinks: "a1 is an Animal pointer, so
         I will call Animal's sound()."   ← decided at COMPILE time
```

```
   At COMPILE time the compiler only knows:    a1 is an Animal*
   At RUN time the memory holds:               a Dog

   The compiler decided too early, before it could know
   the object was really a Dog.
```

> **Remember:** the pointer's *type* is fixed at compile time. The *object it points to* is only known at run time. By default, C++ chooses the function using the pointer's type.
> 

This is a **wrong function being chosen**, not a crash. It is also exactly the problem the next keyword solves.

## 9. Step 3: The Fix, the `virtual` Keyword

Put **`virtual`** in front of the function in the **parent (base) class**:

```cpp
#include <iostream>
using namespace std;

class Animal {
public:
    **virtual** void sound() {                 // ← **virtual**
        cout << "Animal sound" << endl;
    }
};

class Dog : public Animal {
public:
    void sound() {
        cout << "Bhow Bhow" << endl;
    }
};

class Cat : public Animal {
public:
    void sound() {
        cout << "Meow" << endl;
    }
};

int main() {
    Animal* a1;

    Dog d;
    Cat c;

    a1 = &d;
    a1->sound();        // Bhow Bhow

    a1 = &c;
    a1->sound();        // Meow

    return 0;
}
```

**Output:**

```
Bhow Bhow
Meow
```

### What `virtual` Changes

```
**WITHOUT virtual**                      **WITH virtual**
─────────────────                    ─────────────────
a1->sound();                         a1->sound();
Decided at *COMPILE time*              Decided at *RUN time*
by the pointer's TYPE                by the real OBJECT
   → Animal::sound()                    → Dog::sound() or Cat::sound()
```

> **Instructor's explanation:** with `virtual`, at run time the program checks what is actually stored behind the pointer. If it is a Dog, Dog's function runs. If it is a Cat, Cat's function runs. This is **run time polymorphism**.
> 

Notice the same line `a1->sound();` produced **two different outputs**. One call, many forms. That is polymorphism.

### Rules to Remember

| Rule | Meaning |
| --- | --- |
| Write `virtual` in the **base class** | Instructor: the top-most parent's function gets the keyword. |
| Derived classes override with the **same signature** | Same name and same parameters. |
| It works through a **base class pointer (or reference)** | Calling directly on an object (`d.sound()`) never needed it. |
| Once `virtual` in the base, the derived version is virtual too | You don't have to repeat it, though many programmers do. |

> **Instructor's PDF definition:** *a member function declared `virtual` in the base class, so that when it is called through a base class pointer, the derived class's function executes.*
> 

### A Small Habit to Learn (C++11)

You can write `override` after the derived function:

```cpp
void sound() override {                    // compiler checks that a base virtual exists
    cout << "Bhow Bhow" << endl;
}
```

If you misspell the name or change the parameters by accident, the compiler gives an **error** instead of silently creating a separate function. We used this in the Bank Account problem (Unit 4, Lecture 4).

### Early Binding vs Late Binding (Introduction)

The syllabus uses two names for what we just saw:

```
   EARLY binding  =  function chosen at COMPILE time   (normal functions)
   LATE  binding  =  function chosen at RUN time       (virtual functions)
```

A fuller treatment, including how C++ actually does this behind the scenes, comes in **Lecture 3**.

### A Note About `new Dog()`

In Step 2 we used `new Dog()`, which allocates memory on the heap. From **Lecture 1** you know every `new` needs a `delete`. Deleting a derived object through a base pointer needs one more rule (a **virtual destructor**). That is in **Lecture 3**. For now, the examples use `a1 = &d`, which has no such issue.

# 🎯 Part 3: Pure Virtual Functions and Abstract Classes

---

## 10. Why Do We Need Them?

Think about `Animal`. Does it make sense to ask "what sound does *an animal* make?" Not really. A **dog** barks and a **cat** meows, but a general "animal" has no sound of its own.

Still, we want **every** animal class to have a `sound()` function. How do we force that?

```
   Animal   ──▶   "Every animal MUST have sound()"  (*a rule, not a real thing*)
     ▲
   ┌─┴──┐
  Dog   Cat       each one supplies its OWN sound()
```

## 11. Pure Virtual Function

> A **pure virtual function** is a virtual function with **no body** in the base class, written with **`= 0`**. It *forces* every derived class to write its own version.
> 

```cpp
virtual void sound() = 0;
```

```
   Normal virtual:     virtual void sound() { cout << "Animal sound"; }   ← has a body
   Pure virtual:       virtual void sound() = 0;                          ← no body, "= 0"
```

> here we have only *declared* that a `sound()` function exists. Dog and Cat will make their own. The `Animal` class works as a **blueprint** for its child classes.
> 

## 12. Abstract Class

> A class that contains **at least one pure virtual function** is an **abstract class**. **You *cannot* create an object of an abstract class.**
> 

```cpp
#include <iostream>
using namespace std;

class Animal {                           // **abstract class**
public:
    virtual void sound() = 0;            // pure virtual function
};

class Dog : public Animal {
public:
    void sound() {
        cout << "Bhow Bhow" << endl;
    }
};

class Cat : public Animal {
public:
    void sound() {
        cout << "Meow" << endl;
    }
};

int main() {
    // Animal an;               // ❌ **ERROR**: cannot create an object of an abstract class

    Animal* a1;

    Dog d;
    Cat c;

    a1 = &d;
    a1->sound();                // Bhow Bhow

    a1 = &c;
    a1->sound();                // Meow

    return 0;
}
```

**Output:**

```
Bhow Bhow
Meow
```

If you remove the comment on `Animal an;`, the compiler reports an error similar to: *cannot declare variable `an` to be of abstract type `Animal`*.

```
┌───────────────────────────────────────────────────────────┐
│   ABSTRACT CLASS  Animal                                  │
│   "I only describe WHAT animals must do (sound),          │
│    not HOW. I can never be created myself."               │
├───────────────────────────────────────────────────────────┤
│   Animal an;        ❌  not allowed                        │
│   Animal* a1;       ✅  allowed (a pointer to Animal)      │
│   Dog d;            ✅  Dog gives sound() a real body      │
└───────────────────────────────────────────────────────────┘
```

### Important Points

| Point | Explanation |
| --- | --- |
| Abstract class = blueprint | It acts as a **blueprint for child classes**. |
| No objects | Objects **cannot** be created from it. |
| Used only as a base class | Meant for **inheritance**. |
| How to make a class abstract in C++ | Add **at least one pure virtual function**. |
| No `abstract` keyword in C++ | Some YouTube teachers say you can write `abstract` before the class. That works in **Java**, not in **C++**. |

> A pointer **to** an abstract class is fine: `Animal* a1;`. Only creating the object itself is blocked. This is what makes the polymorphism example above work.
> 

### What If a Child Forgets to Override?

If `Cat` did **not** write its own `sound()`, `Cat` would still have the pure virtual function inherited from `Animal`. So `Cat` would also be abstract, and `Cat c;` would fail. A child class must implement **all** pure virtual functions to become creatable.

## 13. Virtual vs Pure Virtual (Side by Side)

| Feature | Virtual function | Pure virtual function |
| --- | --- | --- |
| Syntax | `virtual void f() { ... }` | `virtual void f() = 0;` |
| Body in base class | Yes | No |
| Must derived class override? | No (optional) | **Yes** |
| Makes the class abstract? | No | **Yes** |
| Can you create a base object? | Yes | **No** |

---

## All Polymorphism in One Picture

```
                         POLYMORPHISM
                              │
          ┌───────────────────┴─────────────────────────┐
     COMPILE TIME                                   RUN TIME
   (decided by compiler)                    (decided while running)
                │                                       │
   ┌────────────┼───────────┐                  Function overriding
   │            │           │                  + virtual function
 Function    Constructor   Operator              + base class pointer
overloading  overloading   overloading                  │
                                                 Pure virtual function
                                                        │
                                                 Abstract class
```

---

## Common Mistakes to Avoid

| Mistake | What goes wrong |
| --- | --- |
| Forgetting `virtual` in the base class | A base pointer calls the **base** function, even for a Dog object. |
| Putting `virtual` only in the derived class | Too late. The base class function must be the virtual one. |
| Changing the parameters in the derived function | That is **overloading** or hiding, not overriding. Use `override` to catch it. |
| Writing `Animal a;` when `Animal` is abstract | Compile error. |
| Confusing overloading and overriding | Same class → overloading. Parent and child → overriding. |
| Expecting `virtual` to work through a plain object (`d.sound()`) | That call is already direct. Virtual matters with **pointers and references**. |
| Writing `abstract class Animal` | Not C++. Use a pure virtual function. |
| Forgetting the semicolon after `= 0` | `virtual void sound() = 0;` needs it. |

---

## Key Points to Remember

- **Polymorphism** = "many forms": one name, different behavior.
- **Compile time (static):** function overloading, constructor overloading, operator overloading. The compiler decides.
- **Run time (dynamic):** function overriding with **virtual functions**. The decision is made while running.
- **Overloading** = same class, different parameters. **Overriding** = parent and child, same signature.
- A **base class pointer** can hold the address of a **derived class** object.
- Without `virtual`, the call is decided from the **pointer's type** (compile time), so the **base** function runs.
- With `virtual` in the base class, the call is decided from the **actual object** (run time), so the **derived** function runs.
- A **pure virtual function** has no body and ends with `= 0`.
- A class with **at least one pure virtual function** is an **abstract class**. You cannot create its objects, and it exists to be a **blueprint** for child classes.
- Children must override every pure virtual function, or they become abstract too.