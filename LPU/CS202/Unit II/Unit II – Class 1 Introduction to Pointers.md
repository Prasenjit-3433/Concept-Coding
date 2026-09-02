# Unit II – Class 1: Introduction to Pointers

Status: Pending

*(Pointers, Reference Variables, Arrays and String Concepts — CS202)*

---

## Before We Start: Why Do Pointers Even Exist?

Every variable you create lives somewhere in your computer's memory (RAM). That memory location has an **address** — just like every house on a street has an address.

**Real-life analogy:** Think of your computer's memory as a huge street with millions of houses, each with a unique house number (address). A variable is like a **person living in one of those houses**, and the value stored in the variable is like **what that person owns**.

Now — what if you wanted to write down someone's **house number** on a piece of paper, instead of writing down what they own? That piece of paper, which stores an *address*, is exactly what a **pointer** is.

```
House (Memory Location)          Piece of paper with house number
┌───────────────────┐              ┌───────────────────┐
│   House #24       │              │  Contains: 24     │
│   Owner: Aman     │              │  (this is the     │
│   (value: "Aman") │              │  address, not     │
│                   │              │  the person)      │
└───────────────────┘              └───────────────────┘
      variable                          pointer
```

---

## 1. What is a Pointer?

A **pointer** is a variable that does not store an ordinary value (like a number or character) — instead, it stores the **memory address** of another variable.

```cpp
int age = 20;      // a normal variable storing the value 20
int* p = &age;      // a pointer storing the ADDRESS of 'age'
```

```
Memory:
 ┌──────────────┐          ┌────────────────┐
 │   age        │          │      p         │
 │   value: 20  │◄─────────┤ value: address │
 │   address:   │          │ of 'age'       │
 │   0x1000     │          │ (i.e. 0x1000)  │
 └──────────────┘          └────────────────┘
```

**Real-life analogy:** Think of a delivery app. Your friend's **house** is the variable `age`, holding the actual person (value = 20). The **delivery address you type into the app** is the pointer `p` — it doesn't contain your friend, it just tells the delivery driver *where* to find them.

---

## 2. The Two Key Operators: `&` and

### `&` — Address-of Operator

Placed before a variable name, `&` gives you that variable's **memory address**.

```cpp
int age = 20;
cout << &age;   // prints something like 0x7ffee4a1c
```

**Real-life analogy:** If `age` is your friend living in a house, `&age` is like asking "What's the house number where this person lives?" — it doesn't give you the person, just their address.

### — Dereference Operator

Placed before a pointer, `*` means "go to the address stored in this pointer, and give me the value sitting there."

```cpp
int age = 20;
int* p = &age;

cout << p;    // prints the ADDRESS of age (e.g. 0x7ffee4a1c)
cout << *p;   // prints 20 — the VALUE at that address
```

**Real-life analogy:** If someone hands you a **slip of paper with an address written on it** (`p`), then:

- Reading the slip itself (`p`) tells you the *address*.
- Actually **going to that address and looking inside the house** (`p`) tells you *who/what is there* — in this case, 20.

⚠️ **Important — `*` has two completely different meanings depending on context, and this trips up almost every beginner:**

```cpp
int* p = &age;   // HERE, * means "p is a pointer" (declaration)
cout << *p;        // HERE, * means "go to that address" (dereferencing)
```

```
Context 1 — Declaration:           Context 2 — Usage:
int* p = &age;                     cout << *p;
   ↑                                    ↑
"p is a pointer to an int"        "give me the value AT the
                                    address stored in p"
```

---

## 3. Declaring and Initializing Pointers

**Syntax:**

```cpp
dataType* pointerName;
```

```cpp
int* p1;        // pointer to an int
float* p2;      // pointer to a float
char* p3;       // pointer to a char
```

A pointer **must** point to the same data type as the variable it stores the address of. This is because the compiler needs to know **how many bytes to read** starting from that address.

```cpp
int age = 20;
int* p = &age;    // ✅ correct — int* points to an int

float height = 5.9;
int* q = &height;  // ❌ WRONG — type mismatch, compiler error
```

**Full working example:**

```cpp
#include <iostream>
using namespace std;

int main() {
    int age = 20;
    int* p = &age;

    cout << "Value of age: " << age << endl;      // 20
    cout << "Address of age: " << &age << endl;    // e.g. 0x7ffee...
    cout << "Value stored in p: " << p << endl;     // same address
    cout << "Value pointed to by p: " << *p << endl; // 20

    return 0;
}
```

**Real-life analogy for the whole flow:** Imagine a **hotel front desk**.

- `age` = the guest sitting in room 0x7ffee... (their actual room)
- `&age` = asking the front desk "which room is this guest in?"
- `p` = a notecard where you wrote down that room number
- `p` = walking to that room number and checking who's actually inside

---

## 4. Changing a Value *Through* a Pointer

This is where pointers show their real power — you can modify the original variable *using* the pointer.

```cpp
int age = 20;
int* p = &age;

*p = 25;    // go to the address p points to, and set it to 25

cout << age;   // prints 25 — the ORIGINAL variable changed!
```

```
Before:  age = 20  ←── p points here
After:   age = 25  ←── p still points here, but value changed
```

**Real-life analogy:** If `p` is the room number written on your notecard, then `*p = 25` is like walking into that exact room and **replacing whoever is inside** with a new person named "25." Since `age` and `*p` refer to the **same room**, checking on `age` afterward shows the new occupant.

This is the foundation for something you'll see constantly in DSA — passing pointers to functions so the function can modify the original data, not just a copy (we'll cover this properly in Class 5).

---

## 5. Pointer Declaration — Placement of  (a Common Confusion)

All three of these are valid and mean the same thing, though the middle style is most common:

```cpp
int *p;
int* p;
int * p;
```

⚠️ **Trap to watch out for:** the position of `*` matters when declaring **multiple pointers on one line**.

```cpp
int* p1, p2;   // ❌ p1 is a pointer, but p2 is just a NORMAL int! (common mistake)
int *p1, *p2;   // ✅ both p1 and p2 are pointers
```

This happens because `*` binds to the variable name, not the data type, in C++'s grammar — even though we visually read `int*` as "one unit."

---

## 6. Size of a Pointer

No matter what data type a pointer points to, the pointer itself always takes the **same amount of memory** on a given system (typically 8 bytes on a modern 64-bit system) — because it's just storing an address, not the actual data.

```cpp
int* p1;      // 8 bytes (on 64-bit system)
double* p2;   // 8 bytes
char* p3;     // 8 bytes
```

**Real-life analogy:** Whether the address you write down is for a tiny studio apartment or a huge mansion, the **slip of paper with the address** is always the same size — it's just storing a location, not the size of what's there.

```cpp
cout << sizeof(p1);   // prints 8 (on most modern systems)
cout << sizeof(age);  // prints 4 (int is 4 bytes) — different from pointer size!
```

---

## 7. Quick Recap Table

| Concept | Symbol | Meaning | Real-life equivalent |
| --- | --- | --- | --- |
| Address-of | `&var` | gives address of `var` | asking "what's your house number?" |
| Dereference | `*ptr` | gives value stored at that address | actually visiting that house |
| Pointer declaration | `int* p;` | `p` will store an address of an int | a blank notecard reserved for writing an address |
| Pointer assignment | `p = &var;` | store `var`'s address in `p` | writing the house number on the notecard |
| Modify via pointer | `*p = val;` | change the value at that address | walking to the house and changing what's inside |

---

## 8. Practice Questions for Students (recommended to give in class)

1. Predict the output:

```cpp
int x = 10;
int* p = &x;
*p = *p + 5;
cout << x;
```

1. What's wrong with this code?

```cpp
int a = 5;
int* p;
p = a;      // will this compile?
```

1. Declare two pointers `p1` and `p2` to `float` variables in a single line correctly.

---

That covers **Class 1: Introduction to Pointers** in full depth. Ready for **Class 2 – Pointer Arithmetic & Types** whenever you'd like to continue.