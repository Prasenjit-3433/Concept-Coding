# Practice Set

Status: Pending

# 🎯Section A — Multiple Choice Questions

---

### **Q1.** Which of the following is **not** a valid property of a constructor?

A) Same name as the class

B) Can have a return type of `void`

C) Automatically called when an object is created

D) Can be overloaded

**Answer: B**

**Explanation:** A constructor has **no return type at all** — not `int`, not `void`, nothing. This is one of the three defining rules that separates it from an ordinary function (Part 1 of the main lecture note). Writing `void ClassName() { ... }` inside a class doesn't create a constructor — it creates an ordinary member function that happens to share the class's name, and it will **not** be auto-called when an object is created. A, C, and D are all genuine constructor properties.

---

### **Q2.** What happens if you write your own parameterized constructor in a class and **don't** write a default constructor?

A) C++ still generates a free default constructor automatically

B) `ClassName obj;` (no arguments) will fail to compile

C) The parameterized constructor becomes the default constructor

D) A compiler warning is shown but the code still runs

**Answer: B**

**Explanation:** C++ only generates the free empty default constructor when you write **no constructors at all**. The moment you write even one constructor yourself (parameterized or otherwise), that automatic generosity stops — the compiler assumes you're taking full responsibility for construction. So `ClassName obj;` now has no matching constructor (zero arguments doesn't match a constructor that expects, say, two), and it fails to compile. This is exactly the trap demonstrated in the Gap Fill note's Part 1, Section 1.

---

### **Q3.** Which statement about the `this` pointer is correct?

A) `this` must be declared manually inside every member function that needs it

B) `this` can be reassigned to point to a different object mid-function

C) `this` is only available inside constructors, not other member functions

D) `this` holds the address of the object the member function was called on

**Answer: D**

**Explanation:** `this` is automatically available inside **every non-static member function**, not just constructors (ruling out C) — you never declare it yourself (ruling out A). It also **cannot be reassigned** — it stays locked to the calling object for the entire duration of that function call (ruling out B). D is the core definition from Part 2 of the lecture note: `this` is a pointer holding the current object's address.

---

### **Q4.** A copy constructor's parameter is conventionally declared as:

A) `ClassName obj`

B) `ClassName *obj`

C) `const ClassName &obj`

D) `ClassName &&obj`

**Answer: C**

**Explanation:** `const ClassName &obj` does two jobs at once. The `&` (reference) means the object being copied from isn't itself copied again just to be passed in — no infinite recursion, no wasted copy (recall References from Pointers & References). The `const` guarantees the copy constructor can't accidentally modify the source object while reading from it. A (`ClassName obj`, pass by value) is actually self-contradictory here — passing by value into the copy constructor would itself require calling the copy constructor, causing infinite recursion. B is a pointer, not the reference syntax C++ copy constructors use. D is move-semantics syntax, a later topic, not what's covered here.

---

### **Q5.** Which of these situations does **not** trigger a call to the copy constructor?

A) `MyClass b = a;` where `a` is an existing object

B) Passing an object into a function by value

C) Returning an object by value from a function

D) `a.someFunction();` calling a member function on `a`

**Answer: D**

**Explanation:** Calling an ordinary member function on an existing object doesn't create any new object — nothing is being copied, so the copy constructor plays no role. A, B, and C are exactly the three trigger situations named in Part 3 of the lecture note: initializing one object from another, passing by value (a copy is made for the function's local parameter), and returning by value (a copy is made to hand back to the caller).

---

### **Q6.** In a shallow copy of an object containing a pointer member, what exactly gets copied?

A) The value sitting at the address the pointer points to

B) The address stored in the pointer, not the underlying data

C) Both the pointer's address and the underlying data are deep-copied

D) Nothing — shallow copy skips pointer members entirely

**Answer: B**

**Explanation:** This is the precise definition from Part 4: a shallow copy copies a pointer member's value **exactly as it's stored** — and what's stored in a pointer is an address, not the data. So both objects end up holding the *same* address, pointing at one shared block of memory. A describes what a **deep** copy does instead. C incorrectly claims shallow copy does deep-copy work. D is simply false — pointer members are copied too, just copied as raw addresses.

---

### **Q7.** Which of these is true about destructors?

A) A class can have multiple overloaded destructors

B) A destructor can take parameters to control what gets cleaned up

C) A destructor's name is the class name prefixed with `~`

D) You must manually call the destructor before an object goes out of scope

**Answer: C**

**Explanation:** C is the syntax rule from Part 5. A and B are both false for the same underlying reason: a destructor **never takes parameters** — since it's always called automatically with zero arguments (when the object goes out of scope), there's no way to overload it by parameter count the way you overload constructors. A class can therefore only ever have **exactly one** destructor. D is false — that's the entire point of a destructor: it's called **automatically**, exactly like a constructor is, without you writing an explicit call.

---

### **Q8.** A friend function is best described as:

A) A private member function that only the class itself can call

B) A non-member function granted access to a class's private members

C) A constructor that initializes friend classes

D) A function that can only be called using the dot operator on an object

**Answer: B**

**Explanation:** B is the exact definition from Part 6. A friend function is explicitly **not** a member of the class (ruling out A) — it's an ordinary standalone function that the class has chosen to trust with access to its private data. C describes nothing that exists in this lecture. D is backwards: a friend function is called like any normal function (`showLength(b)`), **not** with the dot operator (`b.showLength()`), precisely because it was never a member in the first place.

---

### **Q9.** Which of the following **must** use an initializer list rather than assignment inside the constructor body?

A) An `int` data member

B) A `string` data member

C) A `const` data member

D) A `float` data member with a default value of `0.0`

**Answer: C**

**Explanation:** From Part 2, Section 3 of the Gap Fill note: a `const` member has no "garbage value, then overwrite" phase — it must receive its one and only value at the exact moment it's created. Assigning to it inside the constructor body fails to compile, since that's an assignment to something already `const` by the time the body runs. A, B, and D are all ordinary (non-const, non-reference) members — assignment inside the body works perfectly fine for them; an initializer list is optional style there, not a requirement.

---

### **Q10.** In `Student(int r, float m) : marks(m), roll(r) { }`, if `roll` is declared **before** `marks` in the class body, in what order are they actually initialized?

A) `marks`, then `roll` — following the initializer list's written order

B) `roll`, then `marks` — following the class's declaration order

C) Order is undefined / compiler-dependent

D) Both are initialized simultaneously

**Answer: B**

**Explanation:** This is the gotcha from Part 2, Section 5 of the Gap Fill note: members always initialize in the order they're **declared inside the class**, completely independent of the order written in the initializer list. Here, even though `marks(m)` is written first, `roll` still initializes first, because `roll` was declared first in the class body. This isn't compiler-dependent (ruling out C) — it's a fixed C++ rule, and mismatched ordering is exactly the kind of thing that produces silent, hard-to-spot bugs (or compiler warnings) when one member's initialization would depend on another's.

# 🎯Section B — Output-Based Questions

---

### **Q11.**

```cpp
class Box {
public:
    Box() {
        cout << "Box created" << endl;
    }
};

int main() {
    Box b1;
    Box b2;
    return 0;
}
```

**Answer:**

```
Box created
Box created
```

**Explanation:** Two separate objects, `b1` and `b2`, are created — one on each line. Since the constructor is auto-called every single time an object is created (Part 1), it runs once for `b1` and once, independently, for `b2`. Two objects means two constructor calls, means the line prints twice.

---

### **Q12.**

```cpp
class Counter {
    int count;
public:
    Counter(int c = 5) {
        count = c;
    }
    void show() { cout << count << endl; }
};

int main() {
    Counter c1;
    Counter c2(10);
    c1.show();
    c2.show();
    return 0;
}
```

**Answer:**

```
5
10
```

**Explanation:** `Counter(int c = 5)` is a constructor with a default argument (Gap Fill, Part 1). `Counter c1;` supplies **zero** arguments, so `c` falls back to its default, `5` — this is legal precisely because a default-argument constructor can be called with no arguments too, filling that role of a default constructor. `Counter c2(10);` explicitly supplies `10`, overriding the default. `count` is set accordingly in each object, and `show()` prints each one's own independent value.

---

### **Q13.**

```cpp
class Test {
public:
    int x;
    Test(int val) {
        x = val;
    }
};

int main() {
    Test t1(5);
    Test t2 = t1;
    t2.x = 20;
    cout << t1.x << " " << t2.x;
    return 0;
}
```

**Answer:**

```
5 20
```

**Explanation:** `x` here is a plain `int`, **not** a pointer — so even though `Test t2 = t1;` triggers the default (shallow) copy constructor, that's completely safe for plain data members (Part 4 explicitly notes shallow copy is only dangerous for pointer members). `t2` gets its **own independent copy** of the value `5`. Changing `t2.x = 20;` only touches `t2`'s own copy — `t1.x` stays untouched at `5`. This question is a deliberate contrast to Q15 below, to test whether students understand shallow copy is fine for non-pointer members.

---

### **Q14.**

```cpp
class Demo {
public:
    Demo() { cout << "A"; }
    ~Demo() { cout << "B"; }
};

int main() {
    Demo d;
    cout << "C";
    return 0;
}
```

**Answer:**

```
ACB
```

**Explanation:** Trace strictly in execution order (Part 5). `Demo d;` — constructor runs immediately → prints `A`. Next line, `cout << "C";` → prints `C`. Then `main()` reaches `return 0;` and ends — at this point `d` goes out of scope, so its destructor fires automatically → prints `B`. The destructor always runs **last**, after every other line in the scope has finished, not at the point where `d` was declared.

---

### **Q15.**

```cpp
class Wallet {
    int *balance;
public:
    Wallet(int b) {
        balance = new int(b);
    }
    void setBalance(int b) {
        *balance = b;
    }
    void show() {
        cout << *balance << endl;
    }
};

int main() {
    Wallet w1(100);
    Wallet w2 = w1;   // default (shallow) copy constructor
    w2.setBalance(500);
    w1.show();
    return 0;
}
```

**Answer:**

```
500
```

**Explanation:** This is the shallow-copy danger from Part 4, deliberately paired against Q13's safe case. `balance` is a **pointer** to dynamically allocated memory. `Wallet w2 = w1;` uses the **default** copy constructor (no custom one was written), which copies `balance`'s address directly — so `w1.balance` and `w2.balance` now point at the **same** heap block holding `100`. `w2.setBalance(500);` dereferences `w2.balance` and overwrites that shared memory to `500`. Since `w1.balance` points at that identical address, `w1.show()` also reads `500` — even though `w1` was never directly touched. This is exactly the bug a custom deep-copy constructor would prevent.

---

### **Q16.**

```cpp
class Point {
public:
    int x;
    Point(int x) {
        x = x;   // note: no 'this->'
    }
};

int main() {
    Point p(10);
    cout << p.x;
    return 0;
}
```

**Answer:** Garbage value (unpredictable — **not** `10`)

**Explanation:** This is the shadowing trap from Part 2, Section 1. The parameter is named `x`, identical to the data member `x`. Inside `x = x;`, both sides refer to the **parameter** — the closest `x` in scope wins (Variables lecture's shadowing rule). So this line just assigns the parameter to itself; the data member `x` is never touched, and it retains whatever garbage value was sitting in that memory when the object was created (Variables lecture — uninitialized memory). The fix would be `this->x = x;`. Since the actual printed value is unpredictable garbage, "10" is a common wrong answer students give here — the trap is deliberate.

---

### **Q17.**

```cpp
class Item {
public:
    Item(int a, int b) {
        cout << "Two-arg: " << a + b << endl;
    }
    Item(int a, int b, int c) {
        cout << "Three-arg: " << a + b + c << endl;
    }
};

int main() {
    Item i1(1, 2);
    Item i2(1, 2, 3);
    return 0;
}
```

**Answer:**

```
Two-arg: 3
Three-arg: 6
```

**Explanation:** Constructor overloading (Part 1, Section 5) — the compiler picks the constructor whose parameter count matches the number of arguments actually supplied. `Item i1(1, 2)` supplies 2 arguments → matches the two-parameter constructor → prints `Two-arg: 3`. `Item i2(1, 2, 3)` supplies 3 arguments → matches the three-parameter constructor → prints `Three-arg: 6`. No ambiguity here since the two constructors have genuinely different parameter counts.

---

### **Q18.**

```cpp
class Circle {
    const int radius;
public:
    Circle(int r) : radius(r) { }
    void show() { cout << radius; }
};

int main() {
    Circle c(7);
    c.show();
    return 0;
}
```

**Answer:**

```
7
```

**Explanation:** `radius` is `const`, so it **must** be set via an initializer list (Gap Fill, Part 2, Section 3) — assignment inside the body would fail to compile. Here `: radius(r)` correctly initializes it directly at creation, before the (empty) body runs. `c.show()` then simply prints the value it was given, `7`. This is a straightforward "does the syntax work" check, not a trap — it confirms students recognize valid const-initializer-list syntax versus the illegal in-body assignment.

---

### **Q19.**

```cpp
class Log {
public:
    Log() { cout << "Open "; }
    ~Log() { cout << "Close "; }
};

void run() {
    Log l;
    cout << "Working ";
}

int main() {
    run();
    cout << "Done";
    return 0;
}
```

**Answer:**

```
Open Working Close Done
```

**Explanation:** `l` is a **local** variable inside `run()` — its scope is limited to that function (Variables lecture, Local Scope). `run()` is called: `l`'s constructor fires immediately → `Open` . Then `Working`  prints. Then `run()` finishes — `l` goes out of scope **right there**, so its destructor fires immediately → `Close` , **before control even returns to `main()`**. Only after `run()` has fully exited (destructor included) does execution resume in `main()`, printing `Done`. A common wrong answer is `Open Working Done Close` — forgetting that the destructor fires the instant the function scope ends, not at the end of the whole program.

---

### **Q20.**

```cpp
class Account {
    int balance;
public:
    Account() {
        balance = 0;
    }
};

int main() {
    Account a1(500);
    return 0;
}
```

**Answer:** Compiler error

**Explanation:** `Account` has exactly **one** constructor defined — `Account()`, which takes zero parameters. Because a constructor was explicitly written, C++ does **not** generate any additional free constructors (Q2's rule, applied in reverse here: writing *any* constructor stops the auto-generated default from being the *only* one available, but more importantly, no constructor matching one `int` argument exists at all). `Account a1(500);` tries to pass one argument, but there is no constructor that accepts a single `int` — this fails to compile with an error along the lines of "no matching constructor for initialization." This tests whether students reflexively assume *any* argument list will "just work" as long as a constructor exists somewhere in the class.

# 🎯Section C — Coding Problems

---

### **Q21. Custom copy constructor with deep copy.**

*Problem:* Write a class `Buffer` that holds a dynamically allocated `int` array (`int *data`) and its size (`int size`). Write a parameterized constructor that allocates the array and fills it with `0, 1, 2, ..., size-1`; a custom copy constructor that performs a **deep copy**; a destructor that frees the array; and a `display()` function. In `main()`, create one `Buffer`, copy it, modify one element of the copy, and print both to prove they're independent.

### Full Solution

```cpp
#include <iostream>
using namespace std;

class Buffer {
    int *data;
    int size;

public:
    // Parameterized constructor
    Buffer(int s) {
        size = s;
        data = new int[size];        // dynamically allocate the array
        for (int i = 0; i < size; i++) {
            data[i] = i;               // fill with 0, 1, 2, ..., size-1
        }
    }

    // Custom copy constructor — DEEP COPY
    Buffer(const Buffer &other) {
        size = other.size;
        data = new int[size];         // allocate a FRESH, separate array
        for (int i = 0; i < size; i++) {
            data[i] = other.data[i];    // copy VALUES, not the pointer
        }
    }

    void setValue(int index, int value) {
        data[index] = value;
    }

    void display() {
        for (int i = 0; i < size; i++) {
            cout << data[i] << " ";
        }
        cout << endl;
    }

    // Destructor
    ~Buffer() {
        delete[] data;                 // array delete, since it was new int[size]
    }
};

int main() {
    Buffer b1(5);
    Buffer b2 = b1;         // deep copy constructor runs here

    b2.setValue(0, 999);     // modify ONLY b2's copy

    cout << "b1: ";
    b1.display();

    cout << "b2: ";
    b2.display();

    return 0;
}
```

**Output:**

```
b1: 0 1 2 3 4
b2: 999 1 2 3 4
```

### Why Each Piece Matters

- **`data = new int[size];` inside the copy constructor** — this is the entire point of the exercise. If this line were missing and we instead wrote `data = other.data;`, we'd be copying the *address*, giving a shallow copy (exactly Q15's bug) — modifying `b2` would then also corrupt `b1`.
- **`delete[] data;` in the destructor, not `delete data;`** — since `data` was allocated with `new int[size]` (array form), it must be freed with the array form `delete[]` (Static vs. Dynamic Memory lecture — mismatching these is undefined behavior).
- **The loop copying values one at a time** (`data[i] = other.data[i]`) is what makes this a *deep* copy rather than a shallow one — each byte of actual data is duplicated into the new block, not just the address referencing the old block.

### Common Student Mistakes to Watch For

1. Forgetting the `const &` on the copy constructor's parameter (leads to infinite recursion, as explained in Q4).
2. Writing `delete data;` instead of `delete[] data;`.
3. Not writing a custom copy constructor at all, and being surprised when `b1` and `b2` share memory.

---

### **Q22. Constructor with default arguments + initializer list, combined.**

*Problem:* Write a class `Rectangle` with private members `length` and `breadth`, where the constructor takes both as parameters, but `breadth` defaults to equal whatever `length` is passed — so `Rectangle(5)` creates a 5×5 square, and `Rectangle(5, 3)` creates a 5×3 rectangle.

### A Genuine Trap in This Problem, Worth Flagging First

The most natural-looking attempt is this:

```cpp
class Rectangle {
    int length, breadth;
public:
    Rectangle(int l, int b = l) : length(l), breadth(b) { }   // ❌ does NOT compile
};
```

This looks reasonable, but it's **illegal C++**: a default argument expression is not allowed to refer to **any other parameter** of the same function — even one that appears earlier in the list. The compiler rejects `b = l` outright. This is a genuinely common trap, and worth remembering precisely because it looks like it should work.

### Correct Solution — Option 1: Delegating Constructors (cleanest)

```cpp
#include <iostream>
using namespace std;

class Rectangle {
    int length, breadth;

public:
    // Two-argument constructor does the real work
    Rectangle(int l, int b) : length(l), breadth(b) { }

    // One-argument constructor DELEGATES to the two-argument one
    Rectangle(int l) : Rectangle(l, l) { }

    int area() {
        return length * breadth;
    }
};

int main() {
    Rectangle square(5);
    Rectangle rect(5, 3);

    cout << "Square area: " << square.area() << endl;
    cout << "Rectangle area: " << rect.area() << endl;

    return 0;
}
```

**Output:**

```
Square area: 25
Rectangle area: 15
```

**How delegation works:** `Rectangle(int l) : Rectangle(l, l) { }` doesn't use an initializer list to set `length`/`breadth` directly — instead, it calls the **other constructor** (`Rectangle(l, l)`) and lets *that* one do the actual initialization via its own initializer list. This is a C++11 feature called a **delegating constructor**: one constructor forwarding its work to another constructor of the same class, rather than duplicating initialization logic in two places.

### Correct Solution — Option 2: Sentinel Default Value (if delegating constructors aren't in your syllabus yet)

```cpp
class Rectangle {
    int length, breadth;

public:
    Rectangle(int l, int b = -1) {
        length = l;
        breadth = (b == -1) ? l : b;   // ternary operator, from Lecture 5
    }

    int area() {
        return length * breadth;
    }
};
```

> This uses a **sentinel value** (`-1`, a value that could never legitimately be a breadth) as the default, then checks inside the body whether the caller actually supplied a real value. This sidesteps the "default can't reference another parameter" restriction entirely, at the cost of needing an `if`/ternary check in the body instead of a clean initializer list.
> 

### Note for This Question

Both solutions are valid; which one to accept depends on whether delegating constructors have been taught in your course. If a student submits the illegal `Rectangle(int l, int b = l)` version, that's the exact misconception this question is designed to surface — worth discussing in class as a *"looks right, isn't"* example, the same category as the `=` vs `==` trap from the Unit 0 placement problems.

---

## Full Practice Set — Summary

```
┌────────────────────────────────────────────────────────────────────────┐
│              UNIT III PRACTICE SET — COVERAGE MAP                      │
├────────────────────────────────────────────────────────────────────────┤
│  **Section A (10 MCQs)**      → conceptual rules: constructor          │
│    properties, this pointer, copy constructor triggers,                │
│    shallow vs deep copy, destructor rules, friend functions,           │
│    const members, initializer list ordering                            │
│                                                                        │
│  **Section B (10 output Qs)** → tracing: default args, shallow         │
│    copy bugs (paired with a safe non-pointer case), shadowing          │
│    without 'this', constructor overloading resolution,                 │
│    destructor timing across function scope, missing-                   │
│    constructor compiler errors                                         │
│                                                                        │
│  **Section C (2 coding Qs)**  → building a deep-copy class from        │
│    scratch; combining default arguments with initializer               │
│    lists, including the "default can't reference another               │
│    parameter" restriction and its fix via delegating                   │
│    constructors                                                        │
└────────────────────────────────────────────────────────────────────────┘
```