# Lecture 1: Operator Overloading and Type Conversion

Date: October 22, 2026
Status: Done

# 🎯 Part 1: Operator Overloading, Basics & Unary Operators

---

## 1. Where This Fits

In the Operators lecture (Unit 0), we used `+`, `-`, `++` and others on built-in types like `int` and `float`:

```cpp
int a = 10, b = 20;
int c = a + b;    // ✅ **works**: C++ knows how to add two ints
```

Now think about a class we made ourselves:

```cpp
class Complex {
public:
    int real, imag;
};

Complex c1, c2;
Complex c3 = c1 + c2;    // ❌ **ERROR**: C++ has no idea how to "add" two Complex objects
```

```
┌──────────────────────────────────────────────────────────┐
│  int + int        → C++ already knows what to do         │
│  Complex + Complex → C++ has *NO IDEA* what "+" means    │
│                      for objects YOU created             │
└──────────────────────────────────────────────────────────┘
```

C++ knows how to add two `int`s because the language defines it. For your own class, the compiler can't guess whether "adding" means adding the real parts, the imaginary parts, or both. **You** have to tell it. That is ***operator overloading***.

---

## 2. What Is Operator Overloading?

> **Operator overloading** means giving an operator (`+`, `-`, `++`, `==`, etc.) an **extra meaning** for objects of a class you created.
> 

You have already seen something similar in two places:

| Where | What it shows |
| --- | --- |
| **Strings lecture (Unit 0)** | `"Learn" + " & Code"` joins strings, but `10 + 20` adds numbers. Same `+`, different behavior depending on the data type. |
| **Functions lecture (Unit 0)** | *Function overloading*: same function name, different behavior depending on the parameters. |

```
                 SAME SYMBOL "+", DIFFERENT MEANING
┌─────────────────┬────────────────────────────────────┐
│  10 + 20        │ → 30           (numeric addition)  │
│  s1 + s2        │ → "Learn & Code"  (string joining) │
│  c1 + c2        │ → ???  ← you decide (Complex)      │
└─────────────────┴────────────────────────────────────┘
```

> **Important:** Operator overloading doesn't change how operators work on `int`, `float`, etc. It only adds new behavior for **your own classes**.
> 

### Why Bother?

Compare these two ways of adding two complex numbers:

```cpp
c3 = c1.add(c2);    // works, but reads awkwardly
c3 = c1 + c2;       // reads just like normal math
```

Operator overloading makes objects behave like built-in types, so the code is cleaner and easier to read.

---

## 3. Syntax of Operator Overloading

An operator is overloaded by writing a **special function** whose name is the keyword `operator` followed by the symbol.

```cpp
return_type operator symbol (parameters) {
    // what the operator should do
}
```

```
   Complex   operator+   ( Complex c )  { ... }
      │          │              │
      │          │              └── operand(s) it works on
      │          └── keyword "operator" + the symbol being overloaded
      └── what the operator gives back
```

It is just a normal member function (Class Objects lecture) with a special name.

### What the Compiler Does Behind the Scenes

When you write `c1 + c2`, the compiler quietly turns it into a function call:

```
c1 + c2     ⟹     c1.operator+(c2)
                    │           │
                    │           └── c2 is passed as the argument
                    └── c1 is the object the function is called ON
                        (so inside the function, *"this"* points to c1)
```

> This ties back to the **`this` pointer** (Unit 3): the object on the **left** of the operator becomes `this`, and the object on the **right** is the parameter.
> 

---

## 4. Rules of Operator Overloading

These rules are very common in exams.

| # | Rule |
| --- | --- |
| 1 | You **cannot create new operators**. Only existing C++ operators can be overloaded (no `**` for power). |
| 2 | You **cannot change the number of operands**. A binary operator stays binary, a unary operator stays unary. |
| 3 | You **cannot change precedence or associativity**. `*` still runs before `+` (Operators lecture, Unit 0). |
| 4 | At least one operand must be a ***user-defined type*** (an object). You can't redefine `int + int`. |
| 5 | The meaning for built-in types **never changes**. |

### Operators That CANNOT Be Overloaded

```
┌──────────────────────────────────────────────────────┐
│   **::**     scope resolution operator               │
│   **.**      dot (member access) operator            │
│   **.***     pointer-to-member operator              │
│   **?:**     ternary operator                        │
│   **sizeof** sizeof operator                         │
└──────────────────────────────────────────────────────┘
```

> The scope resolution operator (`::`) and the dot operator (`.`) are the ones we used in the Classes lecture. They stay fixed so the compiler can always tell which class or object you mean.
> 

Also, `=`, `[]`, `()` and `->` can only be overloaded as **member functions**. We won't go deeper into those here.

---

## 5. A Quick Recap: Unary vs Binary

Recall from the Operators lecture (Unit 0):

```
┌─────────────────────────────────────────────────────┐
│  **UNARY operato**r  → works on ONE operand         │
│        -a      ++a      a--      !a                 │
│                                                     │
│  **BINARY operator** → works on TWO operands        │
│        a + b     a - b     a == b                   │
└─────────────────────────────────────────────────────┘
```

This distinction matters because it decides **how many parameters** your overloading function needs. This part covers unary operators, and Part 2 covers binary operators.

---

## 6. Overloading a Unary Operator

### The Key Rule

> When a unary operator is overloaded as a **member function**, it takes **no parameters**. The single operand is the object itself, reached through `this`.
> 

```
   -p      ⟹      p.operator-()
                        │
                        └── no parameter needed: p IS the operand
                            
```

### `Example 1`: Unary Minus () on a `Point`

We want `-p` to flip the sign of both coordinates.

```cpp
#include <iostream>
using namespace std;

class Point {
    int x, y;

public:
    Point(int a = 0, int b = 0) {
		    x = a;
		    y = b;
		}

    // **Overloading unary minus**
    Point operator-() {
        return Point(-x, -y);       // build and return a NEW Point
    }

    void display() {
        cout << "(" << x << ", " << y << ")" << endl;
    }
};

int main() {
    Point p(3, 4);
    Point q = -p;       // same as: p.operator-()

    p.display();
    q.display();
    return 0;
}
```

**Output:**

```
(3, 4)
(-3, -4)
```

### Tracing Through It

```
Point q = -p;
   ↓
Compiler translates to:  p.operator-()
   ↓
Inside the function:  x = 3, y = 4  (p's own data, via this)
   ↓
Creates a NEW Point(-3, -4) and returns it
   ↓
q receives that new Point
   ↓
p is UNCHANGED: still (3, 4)
```

```
        p                           q
   ┌─────────┐   operator-()   ┌──────────┐
   │ (3, 4)  │ ──────────────▶ │ (-3, -4) │
   └─────────┘                 └──────────┘
   stays the same               brand-new object
```

> This matches how `-` works on normal numbers: `int b = -a;` doesn't change `a`, it produces a new value. That's why the function **returns a new object** instead of changing `x` and `y`.
> 

(The constructor uses an **initializer list**, `: x(a), y(b)`, from Unit 3, and **default arguments**, so `Point p;` also works.)

---

## 7. Overloading Increment `++` and Decrement `-`

These are the *trickiest* unary operators, because each one comes in **two forms**. From the Operators lecture:

```
++a   → PREFIX  (pre-increment):  increase FIRST, then use the value
a++   → POSTFIX (post-increment): use the CURRENT value, then increase
```

C++ must tell the two apart, but both are written `operator++`. The trick:

```
┌──────────────────────────────────────────────────────────┐
│  **PREFIX**   ++a  →  operator++()       (no parameter)  │
│  **POSTFIX**  a++  →  operator++(int)    (a dummy int)   │
│                                                          │
│  The *"int"* in the postfix version is a **DUMMY**.      │
│  It is never used; its only job is to tell the           │
│  compiler "this is the postfix version".                 │
└──────────────────────────────────────────────────────────┘
```

### `Example 2`: A `Counter` Class

```cpp
#include <iostream>
using namespace std;

class Counter {
    int count;

public:
    Counter(int c = 0) {
	    count = c;
    }

    // **PREFIX**  ++c
    Counter& operator++() {
        count = count + 1;          // increase FIRST
        return *this;               // then hand back the **updated** object
    }

    // **POSTFIX**  c++
    Counter operator++(int) {
        Counter temp = *this;       // save the OLD state
        count = count + 1;          // increase
        return temp;                // hand back the **OLD** state
    }

    void display() {
        cout << "Count = " << count << endl;
    }
};

int main() {
    Counter c(5);

    ++c;                    // prefix
    c.display();            // Count = 6

    Counter d = c++;        // postfix
    d.display();            // Count = 6   (the **OLD** value)
    c.display();            // Count = 7   (c was **increased** afterwards)

    return 0;
}
```

**Output:**

```
Count = 6
Count = 6
Count = 7
```

### Tracing the Prefix Version (`++c`)

```
c.count = 5
   ↓
++c  →  c.operator++()
   ↓
count = 5 + 1 = 6
   ↓
return *this   → hands back c itself (now holding 6)
```

### Tracing the Postfix Version (`Counter d = c++;`)

```
c.count = 6
   ↓
c++  →  c.operator++(0)     (compiler passes a dummy 0 automatically)
   ↓
Step 1: temp = *this          → temp is a COPY of c   (temp.count = 6)
Step 2: count = 6 + 1 = 7     → c itself becomes 7
Step 3: return temp           → hands back the OLD copy (6)
   ↓
d = that old copy  →  d.count = 6
```

```
        BEFORE         c++ runs         AFTER
   c  [ 6 ]  ──────────────────────▶  c  [ 7 ]
                 │
                 └─ a copy [ 6 ] is returned ──▶ d [ 6 ]
```

This is exactly the **pre vs post increment** idea from the Operators lecture (`int a = i++;` gives the old value, `int b = ++i;` gives the new one). We are just building the same behavior ourselves.

### Why Prefix Returns `Counter&` and Postfix Returns `Counter`

| Version | Return type | Reason |
| --- | --- | --- |
| **Prefix** `++c` | `Counter&` (reference) | It returns the ***same object***, already updated. A reference avoids making a needless copy (Pointers & References lecture). |
| **Postfix** `c++` | `Counter` (by value) | It must return the **old** state, which is a separate temporary copy. A reference to a local variable like `temp` would be dangerous, since `temp` disappears when the function ends. |

> Notice that `Counter temp = *this;` calls the **copy constructor** (Unit 3). `*this` is the current object itself (dereferencing the `this` pointer).
> 

> **Why this matters:** postfix creates an extra copy while prefix doesn't, so for objects, `++c` is slightly more efficient than `c++`. This is a good habit to remember.
> 

### Decrement Works the Same Way

```cpp
Counter& operator--() {         // prefix  --c
    count = count - 1;
    return *this;
}

Counter operator--(int) {       // postfix c--
    Counter temp = *this;
    count = count - 1;
    return temp;
}
```

Only the `+` changes to `-`.

---

## 8. Overloading the Logical `NOT` Operator (`!`)

Not every unary operator has to return an object. `!` is usually used to answer a yes/no question, so it returns `bool`.

```cpp
class Counter {
    int count;

public:
    Counter(int c = 0) { 
	    count = c;
    }

    bool operator!() {
        return count == 0;      // **true** if the counter is zero
    }
};

int main() {
    Counter a(0), b(5);

    if (!a) cout << "a is empty" << endl;       // runs: !a is true
    if (!b) cout << "b is empty" << endl;       // skipped: !b is false

    return 0;
}
```

**Output:**

```
a is empty
```

> From the Relational Operators lecture: a comparison like `count == 0` already gives `true` or `false`, so we can return it directly. This is the same shortcut used in `isPerfect()` in the Functions lecture.
> 

---

## 9. Calling an Overloaded Operator Explicitly

Since an operator function is a normal function with a special name, you can call it the long way. This shows what the compiler is really doing:

```cpp
Point p(3, 4);

Point q1 = -p;                  // normal, friendly way
Point q2 = p.operator-();       // same thing, written out fully
```

Both lines do exactly the same work. You will rarely write the long form, but it helps you understand that `-p` is just a function call in disguise.

---

## 10. Overloading a Unary Operator Using a Friend Function

Operators can also be overloaded with a **friend function** (Unit 3, Friend Function). The difference is in the parameter count:

```
┌──────────────────────────────────────────────────────────┐
│  MEMBER function  → the object is "this"                 │
│                     unary operator takes 0 parameters    │
│                                                          │
│  FRIEND function  → NOT a member, so there is no "this"  │
│                     the object must be passed in         │
│                     unary operator takes 1 parameter     │
└──────────────────────────────────────────────────────────┘
```

```cpp
class Point {
    int x, y;

public:
    Point(int a = 0, int b = 0){ 
	    x = a;
	    y = b;
    }

    friend Point operator-(Point p);     // declared inside as friend

    void display() {
        cout << "(" << x << ", " << y << ")" << endl;
    }
};

// defined **OUTSIDE**: no "Point::", because it is not a member
Point operator-(Point p) {
    return Point(-p.x, -p.y);            // allowed: friend can touch private x, y
}

int main() {
    Point p(3, 4);
    Point q = -p;       // now translates to:  operator-(p)
    q.display();        // (-3, -4)
    return 0;
}
```

> Without the `friend` keyword, `p.x` would fail because `x` is private. Remember that a friend function is called like a normal function, not with the dot operator.
> 

For unary operators, the member version is the more natural choice. The friend version becomes important for **binary** operators, which we'll see in Part 2.

---

## 11. Common Mistakes to Avoid

| Mistake | What goes wrong |
| --- | --- |
| Giving a member unary operator a parameter, e.g. `Point operator-(Point p)` | It becomes a **binary** minus (`a - b`). The compiler then expects two operands. |
| Forgetting the dummy `int` in postfix | Both versions look identical to the compiler, which causes an error. |
| Returning a **reference to the local `temp`** in postfix | `temp` is destroyed when the function ends, so you'd return a **dangling reference** (Dynamic Memory lecture). |
| Trying to overload `::`, `.`, `?:`, `sizeof` | Not allowed. |
| Changing the original object inside `operator-()` | `-p` should **not** change `p`. Return a new object instead. |

---

## Key Points to Remember (Part 1)

- **Operator overloading** gives an operator extra meaning for **user-defined types** (classes). It doesn't change how operators work on built-in types.
- Syntax: `return_type operator symbol (parameters) { ... }`. It is a normal function with a special name.
- `c1 + c2` is secretly `c1.operator+(c2)`: the left object becomes **`this`**.
- You **cannot** create new operators, change the number of operands, or change precedence/associativity.
- `::`, `.`, `.*`, `?:` and `sizeof` **cannot** be overloaded.
- A **unary** operator as a **member** function takes **no parameters**. As a **friend** function it takes **one**.
- **Prefix** `++c` → `operator++()` and **postfix** `c++` → `operator++(int)`. The `int` is a dummy marker.
- Prefix returns the **updated object (by reference)**. Postfix saves the **old state in a temporary** and returns it **by value**.
- `p` should return a **new** object and leave `p` unchanged. `!` usually returns `bool`.

# 🎯 Part 2: Binary Operators, Friend Functions & `<<` / `>>`

---

## 1. Quick Recap from Part 1

```
┌──────────────────────────────────────────────────────────┐
│  UNARY  as MEMBER  → 0 parameters   (object is "this")   │
│  UNARY  as FRIEND  → 1 parameter                         │
│                                                          │
│  BINARY as MEMBER  → 1 parameter    (left = this)        │
│  BINARY as FRIEND  → 2 parameters                        │
└──────────────────────────────────────────────────────────┘
```

A binary operator has **two operands**, like `a + b`. The compiler translates the expression like this:

```
   c1 + c2     ⟹     c1.operator+(c2)
                        │          │
                        │          └── RIGHT operand → the parameter
                        └── LEFT operand → the object calling (this)
```

> **Rule to lock in:** as a member function, a binary operator takes **only one parameter**, the right-hand operand. The left-hand operand is already there as `this`.
> 

---

## 2. Overloading `+` for a `Complex` Class

A complex number has a real part and an imaginary part. For example, `3 + 4i` has real = 3 and imag = 4. Adding two complex numbers means adding the real parts together and the imaginary parts together:

```
   (3 + 4i) + (1 + 2i)  =  (3+1) + (4+2)i  =  4 + 6i
```

```cpp
#include <iostream>
using namespace std;

class Complex {
    int real, imag;

public:
    Complex(int r = 0, int i = 0) { 
	    real = r;
	    imag = i;
    }

    // Binary + as a **MEMBER** function
    Complex operator+(Complex c) {
        Complex result;
        result.real = real + c.real;      // this->real + c.real
        result.imag = imag + c.imag;
        return result;
    }

    void display() {
        cout << real << " + " << imag << "i" << endl;
    }
};

int main() {
    Complex c1(3, 4), c2(1, 2);
    Complex c3 = c1 + c2;       // same as: c1.operator+(c2)

    c3.display();
    return 0;
}
```

**Output:**

```
4 + 6i
```

### Tracing Through It

```
Complex c3 = c1 + c2;
   ↓
Compiler translates to:  c1.operator+(c2)
   ↓
Inside the function:
     this → c1    (real = 3, imag = 4)
     c    → c2    (real = 1, imag = 2)
   ↓
result.real = 3 + 1 = 4
result.imag = 4 + 2 = 6
   ↓
return result   → a NEW Complex(4, 6)
   ↓
c3 receives it.  c1 and c2 are UNCHANGED.
```

```
     c1 [3,4]  ─┐
                ├──  operator+  ──▶  c3 [4,6]   (new object)
     c2 [1,2]  ─┘
   (both stay the same)
```

> Just like `int c = a + b;` leaves `a` and `b` untouched, our `+` returns a **new object** and doesn't modify either operand.
> 

### A Cleaner Version

`operator+(Complex c)` makes a full copy of `c` every time (the copy constructor runs, Unit 3). We can avoid that by passing a **`const` reference**, a combination you've already seen in the copy constructor:

```cpp
Complex operator+(const Complex &c) {
    return Complex(real + c.real, imag + c.imag);
}
```

```
   const Complex &c
      │       │   │
      │       │   └── a reference: no copy is made   (**Pointers** & **References**)
      │       └────── "Complex" is the type
      └────────────── const: the function promises not to modify c
```

This also shows a shortcut: building and returning an object in one line, `return Complex(...)`, using the constructor.

---

## 3. Other Arithmetic Operators Follow the Same Pattern

Only the maths changes:

```cpp
// Subtraction:  (3+4i) - (1+2i) = 2 + 2i
Complex operator-(const Complex &c) {
    return Complex(real - c.real, imag - c.imag);
}
```

Each arithmetic operator (`-`, `*`, `/`, `%`) is written the same way: take one parameter, build a new object, return it.

---

## 4. Overloading Comparison Operators (`==`, `>`, `<`)

From the Relational Operators lecture (Unit 0): a comparison gives back `1` (true) or `0` (false). So comparison operators return **`bool`**, not an object.

```cpp
class Complex {
    int real, imag;

public:
    Complex(int r = 0, int i = 0) { 
	    real = r;
	    imag = i;
    }

    bool operator==(const Complex &c) {
        return (real == c.real && imag == c.imag);
    }

    bool operator>(const Complex &c) {
        return real > c.real;       // our own rule: compare real parts
    }
};

int main() {
    Complex a(3, 4), b(3, 4), c(1, 2);

    cout << (a == b) << endl;       // 1  (both parts equal)
    cout << (a == c) << endl;       // 0
    cout << (a > c) << endl;        // 1  (3 > 1)

    return 0;
}
```

**Output:**

```
1
0
1
```

> `&&` here is the **logical AND** from Unit 0: the numbers are equal only if the real parts match **and** the imaginary parts match.
> 

> **Notice:** *you* decide what "greater" means for your class. C++ doesn't know whether `3+4i` is greater than `1+9i`. That is the power (and the responsibility) of operator overloading.
> 

---

## 5. Overloading Compound Assignment (`+=`)

Recall from the Assignment Operators lecture: `x += 3` means `x = x + 3`. It **modifies the left operand itself**, so it returns a reference to the updated object, much like prefix `++`.

```cpp
Complex& operator+=(const Complex &c) {
    real = real + c.real;       // change THIS object
    imag = imag + c.imag;
    return *this;               // hand back the updated object
}
```

```cpp
Complex a(3, 4), b(1, 2);
a += b;             // a becomes 4 + 6i
a.display();
```

```
   Compare:
   c3 = c1 + c2   →  c1 and c2 UNCHANGED, new object returned
   a += b         →  a ITSELF CHANGES, a is returned
```

---

## 6. The Limit of Member Functions

Member functions work well when the **left operand is an object**. But what about this?

```cpp
Complex c(3, 4);
Complex r = c + 5;      // object + int  → left is object
Complex s = 5 + c;      // int + object  → left is an INT
```

Suppose we write `Complex operator+(int n)` as a member:

```
   c + 5   ⟹  c.operator+(5)     ✅ works: left is an object
   5 + c   ⟹  5.operator+(c)     ❌ IMPOSSIBLE: 5 is an int,
                                    and int has no member functions
```

```
┌─────────────────────────────────────────────────────────────┐
│   A MEMBER function needs the LEFT operand to be            │
│   an object of that class.                                  │
│                                                             │
│   If the left operand is something else (int, cout),        │
│   a member function can't be used.                          │
│   → use a FRIEND (non-member) function instead.             │
└─────────────────────────────────────────────────────────────┘
```

---

## 7. Binary Operators Using a `Friend Function`

A friend function (Unit 3) is **not** a member, so there is no `this`. Both operands must be passed in as parameters:

```
   BINARY operator as FRIEND  →  TWO parameters
   a + b   ⟹   operator+(a, b)       (a plain function call)
```

### Example: `int + Complex`

```cpp
class Complex {
    int real, imag;

public:
    Complex(int r = 0, int i = 0) { 
	    real = r;
	    imag = i;
    }

    // **friend**: left operand is an int, right operand is a Complex
    friend Complex operator+(int n, const Complex &c);

    void display() {
        cout << real << " + " << imag << "i" << endl;
    }
};

// defined **OUTSIDE** the class: no "Complex::" prefix
Complex operator+(int n, const Complex &c) {
    return Complex(n + c.real, c.imag);     // friend can ***touch private*** members
}

int main() {
    Complex c(3, 4);
    Complex s = 5 + c;      // translates to: operator+(5, c)
    s.display();            // 8 + 4i
    return 0;
}
```

### Tracing Through It

```
5 + c
   ↓
Compiler translates to:  operator+(5, c)     (NOT 5.operator+(c))
   ↓
n = 5,  c.real = 3,  c.imag = 4
   ↓
return Complex(5 + 3, 4)  →  Complex(8, 4)
```

---

## 8. Overloading `<<` (the `cout` Operator)

Right now we print with a `display()` function. We'd prefer to write:

```cpp
cout << c1;       // just like printing an int
```

You already know `<<` from `cout << x;` (Unit 0). The Operators lecture also mentioned that `<<` is the bitwise shift operator, and that using it with `cout` is itself an example of operator overloading. Now we do the same for our own class.

### Why It MUST Be a Friend

```
   cout << c1;
     │      │
     │      └── right operand: our Complex object
     └── LEFT operand: cout, which is an object of the
         ostream class (not our class!)
```

If `<<` were a member of `Complex`, the **left** operand would have to be a `Complex`. But the left operand is `cout`. We also can't add a member function to `ostream`, because that class belongs to C++'s standard library. So the only option is a **friend (non-member) function**.

### The Code

```cpp
#include <iostream>
using namespace std;

class Complex {
    int real, imag;

public:
    Complex(int r = 0, int i = 0) : real(r), imag(i) { }

    friend ostream& operator<<(ostream &out, const Complex &c);
};

ostream& operator<<(ostream &out, const Complex &c) {
    out << c.real << " + " << c.imag << "i";
    return out;                     // return cout itself
}

int main() {
    Complex c1(3, 4), c2(1, 2);

    cout << c1 << endl;             // 3 + 4i
    cout << c1 << " and " << c2 << endl;

    return 0;
}
```

**Output:**

```
3 + 4i
3 + 4i and 1 + 2i
```

### Breaking Down the Function

```
   ostream&   operator<<  ( ostream &out ,  const Complex &c )
      │                          │               │
      │                          │               └── the object to print
      │                          └── cout (passed by REFERENCE)
      └── returns the stream itself, by reference
```

| Piece | Why |
| --- | --- |
| `ostream &out` | A reference to `cout`. A stream **cannot be copied**, so it must be passed by reference. |
| `out << ...` | Inside, we just print each part using the normal `<<` we already know. |
| `return out;` | Returns the same stream so another `<<` can follow. |

### Why Return `out`? The Cascading Effect

Recall the **cascading effect** from Lecture 1 (Unit 0): `cout << "A" << endl << "B"`. Cascading works because each `<<` returns `cout` for the next one to use:

```
   cout << c1 << " and " << c2;

   Step 1:  (cout << c1)       → prints c1, returns cout
   Step 2:  (cout << " and ")  → prints text, returns cout
   Step 3:  (cout << c2)       → prints c2, returns cout
```

> If we returned `void`, the second `<<` would have nothing to work with, and the chain would break.
> 

---

## 9. Overloading `>>` (the `cin` Operator)

Input works the same way, but with `istream`, and the object is **not** `const` because we are changing it.

```cpp
class Complex {
    int real, imag;

public:
    Complex(int r = 0, int i = 0) : real(r), imag(i) { }

    friend istream& operator>>(istream &in, Complex &c);
    friend ostream& operator<<(ostream &out, const Complex &c);
};

istream& operator>>(istream &in, Complex &c) {
    cout << "Enter real part: ";
    in >> c.real;
    cout << "Enter imaginary part: ";
    in >> c.imag;
    return in;
}

ostream& operator<<(ostream &out, const Complex &c) {
    out << c.real << " + " << c.imag << "i";
    return out;
}

int main() {
    Complex c;
    cin >> c;                   // reads both parts
    cout << "You entered: " << c << endl;
    return 0;
}
```

**Sample run:**

```
Enter real part: 5
Enter imaginary part: 7
You entered: 5 + 7i
```

```
   << (output)  →  ostream &out,  const Complex &c   (const: we only READ c)
   >> (input)   →  istream &in,         Complex &c   (NOT const: we CHANGE c)
```

> `Complex &c` is a **reference**, so the function fills the **original** object, not a copy. This is the same reason `swapNums(int &a, int &b)` could swap the caller's variables (Functions lecture).
> 

---

## 10. Member vs Friend: Side by Side

| Feature | Member function | Friend function |
| --- | --- | --- |
| Unary operator parameters | **0** | **1** |
| Binary operator parameters | **1** | **2** |
| Left operand | Must be an object of the class (`this`) | Can be **any type** (`int`, `cout`, ...) |
| `c + 5` | ✅ | ✅ |
| `5 + c` | ❌ | ✅ |
| `cout << c` / `cin >> c` | ❌ | ✅ (the only choice) |
| Access to private data | Directly | Through the object passed in (allowed by `friend`) |
| Defined | Inside or outside with `Class::` | Outside, **no** `Class::` prefix |
| Called as | `a.operator+(b)` | `operator+(a, b)` |

```
┌─────────────────────────────────────────────────────────────┐
│   WHICH ONE SHOULD I USE?                                   │
│                                                             │
│   • Left operand is YOUR object  → member is fine           │
│   • Left operand is NOT your object                         │
│       (5 + c,  cout << c,  cin >> c) → friend               │
└─────────────────────────────────────────────────────────────┘
```

---

## 11. Putting It All Together: A Complete Program

```cpp
#include <iostream>
using namespace std;

class Complex {
    int real, imag;

public:
    Complex(int r = 0, int i = 0) : real(r), imag(i) { }

    Complex operator+(const Complex &c) {                // member
        return Complex(real + c.real, imag + c.imag);
    }

    bool operator==(const Complex &c) {                  // member
        return (real == c.real && imag == c.imag);
    }

    friend Complex operator+(int n, const Complex &c);   // friend
    friend ostream& operator<<(ostream &out, const Complex &c);  // friend
};

Complex operator+(int n, const Complex &c) {
    return Complex(n + c.real, c.imag);
}

ostream& operator<<(ostream &out, const Complex &c) {
    out << c.real << " + " << c.imag << "i";
    return out;
}

int main() {
    Complex a(3, 4), b(1, 2);

    cout << "a     = " << a << endl;
    cout << "b     = " << b << endl;
    cout << "a + b = " << a + b << endl;
    cout << "5 + a = " << 5 + a << endl;
    cout << "a == b? " << (a == b) << endl;

    return 0;
}
```

**Output:**

```
a     = 3 + 4i
b     = 1 + 2i
a + b = 4 + 6i
5 + a = 8 + 4i
a == b? 0
```

> Notice `(a == b)` is in brackets. This comes from **precedence** (Operators lecture): `<<` ranks higher than `==`, so without brackets, C++ would try `(cout << "a == b? " << a) == b`. Brackets always win.
> 

---

## 12. Common Mistakes to Avoid

| Mistake | What goes wrong |
| --- | --- |
| Giving a **member** binary operator **two** parameters | Compiler error: a member binary operator takes exactly one. |
| Writing `<<` or `>>` as a **member** | The left operand is `cout`/`cin`, not your class. It can't work. |
| Passing `ostream` **by value** (`ostream out`) | Streams can't be copied. Always use `ostream &out`. |
| Forgetting `return out;` in `<<` | Cascading (`cout << a << b`) breaks. |
| Writing `Complex::` before a friend function's definition | A friend is not a member, so no `Complex::` is needed. |
| Returning a reference to a local variable in `+` | The local object is destroyed, leaving a dangling reference. Return **by value**. |
| Modifying `this` inside `operator+` | `a + b` must not change `a`. Build a new object. |

---

## Key Points to Remember (Part 2)

- A **binary** operator as a **member** takes **one** parameter (right operand). The left operand is `this`: `c1 + c2` ⟹ `c1.operator+(c2)`.
- Arithmetic operators (`+`, , ) return a **new object** and leave both operands unchanged.
- Comparison operators (`==`, `>`, `<`) return **`bool`**, and **you** decide what "equal" or "greater" means for your class.
- `+=` changes the **left object itself** and returns `this` by **reference**.
- Use `const ClassName &` for parameters you only read: it avoids a copy and protects the value.
- A **member** function needs the **left operand** to be an object of the class. For `5 + c`, `cout << c` and `cin >> c`, use a **friend** function.
- As a **friend**, a binary operator takes **two** parameters: `a + b` ⟹ `operator+(a, b)`.
- `<<` and `>>` are always **friend** functions. They take `ostream&` / `istream&` as the first parameter and **return that same stream by reference**, which keeps cascading working.
- `<<` takes `const Complex &c` (read only), and `>>` takes `Complex &c` (it must modify the object).

# 🎯 Part 3: Type Conversion (Basic ↔ Class)

---

## 1. Quick Recap: Type Casting for Built-in Types

In the Data Types lecture (Unit 0), we saw two kinds of type casting:

```cpp
int x = 10;
double y = 6.6;
int z = x + y;              // **IMPLICIT**: compiler converts automatically (z = 16)
double b = (double)x / 3;   // **EXPLICIT**: programmer asks for the conversion
```

C++ knows how to convert between `int`, `float`, `double`, etc. because the language defines those rules. For **our own classes**, C++ has no rules, so we must write them:

```
┌───────────────────────────────────────────────────────────────────┐
│  int  → double     C++ already knows how                          │
│  int  → Distance   C++ has **NO IDEA** (our own class)            │
│  Distance → int    C++ has **NO IDEA** either                     │
└───────────────────────────────────────────────────────────────────┘
```

---

## 2. The Three Possible Conversions

```
┌────────────────────────────────────────────────────────────────────┐
│  1. BASIC  → CLASS    int  → Distance   (conversion constructor)   │
│  2. CLASS  → BASIC    Distance → int    (conversion operator)      │
│  3. CLASS  → CLASS    A → B             (either one, see §8)       │
└────────────────────────────────────────────────────────────────────┘
```

The syllabus asks for the first two, so we cover those in detail. We use a `Distance` class that stores **meters** and **centimeters**, because converting between a single number and a two-part object is easy to picture.

```
   275 cm   ⟺   2 meters + 75 cm
```

---

## 3. Basic Type → Class Type

### The Idea

We want to write:

```cpp
Distance d = 275;     // an int on the right, a Distance on the left
```

The compiler needs instructions for turning `275` into a `Distance` object, which we give it with a ***conversion constructor***.

> A **`conversion constructor`** is a constructor that takes **one argument** of the basic type. The compiler uses it automatically to convert that basic type into an object of the class.
> 

### Example

```cpp
#include <iostream>
using namespace std;

class Distance {
    int meters, cm;

public:

		// normal constructor
    Distance(int m, int c) : { 
	    meters = m'
	    cm = c;
    }     

    // **CONVERSION CONSTRUCTOR**: int (total cm) → Distance
    Distance(int totalCm) {
        meters = totalCm / 100;       // whole meters
        cm = totalCm % 100;           // leftover cm
    }

    void display() {
        cout << meters << " m " << cm << " cm" << endl;
    }
};

int main() {
    Distance d1 = 275;      // basic → class
    d1.display();
    return 0;
}
```

**Output:**

```
2 m 75 cm
```

### Tracing Through It

```
Distance d1 = 275;
   ↓
Left side is a Distance, right side is an int.
Compiler ***looks for*** a constructor that accepts **ONE** int
   ↓
Finds: Distance(int totalCm)   ← the conversion constructor
   ↓
meters = 275 / 100 = 2       (integer division, Operators lecture)
cm     = 275 % 100 = 75      (modulus gives the remainder)
   ↓
d1 holds 2 m 75 cm
```

```
        int                        Distance
     ┌───────┐   conversion   ┌───────────────┐
     │  275  │ ─────────────▶ │ 2 m  |  75 cm │
     └───────┘  constructor   └───────────────┘
```

> It uses the `/` and `%` from the Arithmetic Operators lecture: `/` gives the quotient and `%` gives the remainder.
> 

### It Also Works in Assignment and Function Calls

```cpp
Distance d2(1, 20);
d2 = 150;                // int converted to a temporary Distance, then assigned
d2.display();            // 1 m 50 cm
```

```
d2 = 150;
   ↓
Step 1: 150 → temporary Distance(1 m 50 cm)   (conversion constructor)
Step 2: that temporary is assigned into d2
```

The same happens when you pass an `int` to a function that expects a `Distance`.

### Important: Any One-Argument Constructor Counts

You don't need a special keyword. **Any constructor that can be called with a single argument** acts as a conversion constructor. That includes constructors with **default arguments** (Unit 3):

```cpp
Complex(int r = 0, int i = 0)   // can be called as Complex(5)
```

```cpp
Complex c = 5;     // legal! converts 5 → Complex(5, 0)
```

This is convenient but can cause surprises, as we'll see in the `explicit` section below.

---

## 4. Class Type → Basic Type

### The Idea

Now the reverse:

```cpp
Distance d(3, 50);
int total = d;       // we want 350 (total cm)
```

A constructor can't help here, because constructors create **objects**. We need the object to turn **itself** into a basic value. For that, C++ has the ***conversion operator function***.

> A **`conversion operator function`** is a member function that tells the compiler how to convert an object of the class into a basic type.
> 

### Syntax

```cpp
operator target_type() {
    // return a value of target_type
}
```

```
   operator   int   ( )   { return ...; }
      │        │     │
      │        │     └── always EMPTY parentheses (no parameters)
      │        └──────── the type we are converting TO
      └───────────────── keyword "operator"

   NO return type is written before "operator"!
```

### Example

```cpp
class Distance {
    int meters, cm;

public:
    Distance(int m, int c) : meters(m), cm(c) { }

    // **CONVERSION OPERATOR**: Distance → int (total cm)
    operator int() {
        return meters * 100 + cm;
    }
};

int main() {
    Distance d(3, 50);

    int total = d;               // class → basic (implicit)
    cout << total << endl;       // 350

    int total2 = int(d);         // explicit form, also works
    cout << total2 << endl;      // 350

    return 0;
}
```

**Output:**

```
350
350
```

### Tracing Through It

```
int total = d;
   ↓
Left side is an int, right side is a Distance.
Compiler looks inside Distance for a way to produce an int
   ↓
Finds: operator int()
   ↓
Runs it on d:  3 * 100 + 50 = 350
   ↓
total = 350
```

```
       Distance                           int
  ┌───────────────┐    operator int()  ┌───────┐
  │ 3 m  |  50 cm │ ─────────────────▶ │  350  │
  └───────────────┘                    └───────┘
```

### Rules for a Conversion Operator Function

| # | Rule |
| --- | --- |
| 1 | It must be a **member** function. |
| 2 | It has **no return type** written, because the type after `operator` already says what it returns. |
| 3 | It takes **no parameters** (empty `()`). |
| 4 | It must actually **return** a value of the target type. |
| 5 | It should usually be **`const`**, since converting shouldn't modify the object. |

> Compare with a constructor, which also has no return type. Rule 2 is the same shape, but the reasons differ. A constructor has no return type because it builds an object. A conversion operator has none because its target type is part of its name.
> 

---

## 5. Using Both Conversions Together

```cpp
#include <iostream>
using namespace std;

class Distance {
    int meters, cm;

public:
    Distance(int m, int c) : meters(m), cm(c) { }

    Distance(int totalCm) {                  // basic → class
        meters = totalCm / 100;
        cm = totalCm % 100;
    }

    operator int() const {                   // class → basic
        return meters * 100 + cm;
    }

    void display() const {
        cout << meters << " m " << cm << " cm" << endl;
    }
};

int main() {
    Distance d1 = 275;          // int → Distance
    d1.display();               // 2 m 75 cm

    Distance d2(3, 50);
    int total = d2;             // Distance → int
    cout << "Total cm = " << total << endl;     // 350

    return 0;
}
```

**Output:**

```
2 m 75 cm
Total cm = 350
```

---

## 6. Side by Side: The Two Conversions

| Feature | Basic → Class | Class → Basic |
| --- | --- | --- |
| **Tool used** | Conversion **constructor** | Conversion **operator function** |
| **Where written** | Inside the **destination** class | Inside the **source** class |
| **Syntax** | `ClassName(basic_type x)` | `operator basic_type()` |
| **Parameters** | Takes **one** argument | Takes **none** |
| **Return type** | None (it's a constructor) | None written (the type is in the name) |
| **Example** | `Distance d = 275;` | `int t = d;` |

```
┌───────────────────────────────────────────────────────────────┐
│  BASIC → CLASS : the CLASS learns how to BUILD itself         │
│                  from a basic value  (constructor)            │
│                                                               │
│  CLASS → BASIC : the CLASS learns how to TURN ITSELF          │
│                  INTO a basic value  (operator function)      │
└───────────────────────────────────────────────────────────────┘
```

---

## 7. The `explicit` Keyword: Stopping Unwanted Conversions

Implicit conversion is convenient but can hide mistakes. Suppose a function expects a `Distance` and someone accidentally passes an `int`:

```cpp
void show(Distance d) { d.display(); }

show(275);       // compiles silently, because 275 → Distance automatically
```

If you don't want that, put **`explicit`** before the constructor:

```cpp
class Distance {
    int meters, cm;

public:
    explicit Distance(int totalCm) {      // explicit!
        meters = totalCm / 100;
        cm = totalCm % 100;
    }
};
```

```cpp
Distance d1 = 275;            // ❌ ERROR: implicit conversion blocked
Distance d2(275);             // ✅ OK: you asked for it directly
Distance d3 = Distance(275);  // ✅ OK: explicit conversion
```

> `explicit` means "only convert when the programmer clearly asks." This ties back to **explicit type casting** from the Data Types lecture, where you request the conversion yourself.
> 

The same keyword works on a conversion operator (`explicit operator int()`), which then allows only `int(d)` or `static_cast<int>(d)`. This is introduced in C++11.

---

## 8. Class → Class Conversion (Brief Mention)

Converting one class to another follows the same ideas. It isn't in your syllabus list, but it completes the picture. For two classes, `A` and `B`, you can convert `A` to `B` in **either** place:

```
   Option 1: in B, a constructor that takes an A     B(A a) { ... }
   Option 2: in A, a conversion operator to B        operator B() { ... }
```

Use **only one** of the two for a given conversion. Having both makes the compiler unable to decide, and it reports an **ambiguity** error.

---

## 9. Limits and Gotchas

### Only One User-Defined Conversion Per Step

The compiler will do **one** user-defined conversion automatically, but not a chain of them. Suppose `A` converts to `B`, and `B` converts to `C`. Then `C c = a;` fails, because it would need two hops (`A → B → C`). You must write the first hop yourself: `C c = B(a);`.

### Conversions Can Create Ambiguity in Operators

Remember from Part 2 that `5 + c` needed a friend function. A conversion constructor can change what works:

```cpp
Complex c(3, 4);
Complex r = c + 5;      // 5 is converted to Complex(5, 0), then operator+ runs
```

This works through the conversion constructor, and the member `operator+(const Complex&)` receives the converted `5`. But if a class has **both** a conversion constructor and a conversion operator, the compiler may find more than one way to evaluate `a + b`, and report an ambiguity. Keep conversions simple, and don't give a class conversions in both directions unless you need both.

### Data Can Be Lost

Class → basic often **throws information away**: `Distance(3 m, 50 cm)` becomes just `350`, and the "meters" and "cm" split is gone. This is similar to the truncation in `int x = 5.7;` from the Data Types lecture (x becomes `5`).

---

## 10. Common Mistakes to Avoid

| Mistake | What goes wrong |
| --- | --- |
| Writing a return type: `int operator int()` | Compiler error. A conversion operator has **no** written return type. |
| Giving the conversion operator parameters | Not allowed. It always has empty `()`. |
| Trying to write the conversion operator as a **friend** or non-member | It must be a **member** function. |
| Forgetting that `Distance(int, int = 0)`-style constructors also convert | Surprising implicit conversions appear. Use `explicit` if unwanted. |
| Returning the wrong type from `operator int()` | It must return exactly what the name promises. |
| Providing both a conversion constructor and a conversion operator between the same two types | **Ambiguity** error. |
| Expecting a **chain** of conversions to happen automatically | Only one user-defined conversion is applied per step. |

---

## Key Points to Remember (Part 3)

- **Type conversion for classes** must be written by us, because C++ only knows the rules for built-in types.
- **Basic → Class** uses a **conversion constructor**: a constructor that takes **one** argument. `Distance d = 275;` calls `Distance(int)`.
- **Class → Basic** uses a **conversion operator function**: `operator type() { ... }`. It has **no return type**, **no parameters**, and is always a **member**.
- The conversion constructor lives in the **destination** class. The conversion operator lives in the **source** class.
- Any constructor callable with **one argument** (including ones with default arguments) can act as a conversion constructor.
- `explicit` stops **implicit** conversions, so the programmer must request them clearly.
- Class → class conversion can use either a constructor in the destination or a conversion operator in the source, but **never both**.
- Class → basic conversions may **lose information**.

---

# 📋 Full Recap: Operator Overloading & Type Conversion

```
┌────────────────────────────────────────────────────────────────────────┐
│               OPERATOR OVERLOADING & TYPE CONVERSION                   │
├────────────────────────────────────────────────────────────────────────┤
│  **OPERATOR OVERLOADING**                                              │
│    • Gives operators extra meaning for YOUR classes                    │
│    • Syntax: return_type operator<symbol>(params)                      │
│    • a + b  ⟹  a.operator+(b)   (left = this)                          │
│    • Cannot overload:  ::  .  .*  ?:  sizeof                           │
│    • Cannot invent operators or change precedence/operand count        │
│                                                                        │
│  **UNARY**    member: 0 params  │ friend: 1 param                      │
│    • ++a → operator++()     │ a++ → operator++(int)  (dummy int)       │
│    • Prefix returns by reference, postfix returns old copy             │
│                                                                        │
│  **BINARY**   member: 1 param   │ friend: 2 params                     │
│    • + - * return a NEW object; == > < return bool                     │
│    • += changes left object, returns *this                             │
│    • 5 + c, cout << c, cin >> c  →  need FRIEND                        │
│    • << returns ostream&, >> returns istream& (cascading)              │
│                                                                        │
│  **TYPE CONVERSION**                                                   │
│    • Basic → Class : conversion CONSTRUCTOR  (one argument)            │
│    • Class → Basic : conversion OPERATOR     operator type()           │
│    • explicit blocks implicit conversion                               │
└────────────────────────────────────────────────────────────────────────┘
```