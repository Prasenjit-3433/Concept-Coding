# Pointers & Reference in C++

Status: Pending

This is one of the most important topics in your entire C++ journey — pointers especially become unavoidable once you reach Data Structures & Algorithms (linked lists, trees, graphs simply cannot be built without them). So let's slow down and build this concept brick by brick.

# 🎯Part 1: Memory Addressing & References

---

## 1. Why We Need to Understand Memory Addressing First

Before touching pointers or references, you need one foundational idea: **how does a computer actually write down a memory address?**

> Internally, memory addresses are stored in **binary**. But binary addresses can get extremely long and unreadable — memory is made up of countless individual bits, so a raw binary address could look like `01010` or `101011`, and for larger memory, these binary strings become huge.
> 

Because binary addresses are so unwieldy, C++ (and computers in general) display memory addresses in a more compact, human-friendly format:

> Memory addresses are represented in **hexadecimal format** — not binary — purely because hexadecimal is far more compact and readable.
> 

### Example

```cpp
int x = 10;
```

This creates a variable `x` somewhere in memory, holding the value `10`. If you ask "what is the address of `x`?", you won't get a binary answer — you'll get something like:

```
0xAC7E
```

- The `0x` prefix simply tells you: "what follows is a hexadecimal address."
- A special case: `0x0` represents a **null address** — meaning "this points to nothing."

```
┌─────────────────────────────────────────────┐
│   MEMORY ADDRESS REPRESENTATION             │
│                                             │
│   Internally: binary (long, unreadable)     │
│   Displayed as: hexadecimal (0xAC7E)        │
│   Null/empty address: 0x0                   │
└─────────────────────────────────────────────┘
```

> Why does this matter right now? Because **everything** about references and pointers is fundamentally a conversation about **addresses**. Once addressing makes sense, the rest falls into place.
> 

---

## 2. The Address-Of Operator (`&`)

Suppose:

```cpp
int a = 10;
```

`a` is a variable holding `10`, sitting at some memory address. How do we actually *see* that address?

> C++ gives us the **ampersand (`&`)** symbol — also called the **address-of operator** — specifically to retrieve the memory address of a variable.
> 

```cpp
cout << &a;
```

This doesn't print `10`. It prints the **hexadecimal memory address** where `a` lives.

```
&a   →   "the address of a"
```

---

## 3. What Is a Reference?

Now for the main event. Suppose:

```cpp
int a = 10;
int &ref = a;
```

What did we just create? We created a **reference** to `a`.

> **A reference is another name for an existing variable — an alias.**
> 

This is the single most important sentence to internalize in this entire topic. `ref` isn't a brand-new box in memory holding a fresh copy of `10`. It's simply **another label** pointing at the *exact same* memory location that `a` already occupies.

```
              MEMORY
        ┌───────────────┐
   a ──▶│      10       │◀── ref
        └───────────────┘
      (same address, two names)
```

So when you write:

```cpp
cout << ref;   // prints 10
```

You get `10` — not because `ref` has its own copy, but because `ref` is pointing at the same location `a` already occupies. **The same memory location now has two names: `a` and `ref`.**

### Reference = Alias

> **Alias** simply means "another name for the same thing." A reference is another name for an existing variable, at the exact same memory location.
> 

The name `ref` isn't special — you could just as easily write:

```cpp
int &x = a;
int &y = a;
int &z = a;
```

These are all just variable names *you* choose. What matters is that they all point back to `a`'s exact memory location.

---

## 4. Modifying Through a Reference

Here's where the "alias" idea really proves itself.

```cpp
int a = 10;
int &ref = a;

a = 20;        // change through the original name
cout << a;      // 20
```

That's expected. But now watch this:

```cpp
ref = 20;      // change through the REFERENCE instead
cout << a;      // 20 !!
```

> Why does changing `ref` also change `a`? Because there is **no separate copy**. `a` and `ref` are two names pointing at **one single memory location**. Changing the data through either name changes the *same* underlying data.
> 

```
Before:  a = 10 ,  ref = 10   (same memory: 10)
Action:  ref = 20;
After:   a = 20 ,  ref = 20   (same memory: 20 — because it's literally one location)
```

> **The simplest way to remember this:** A reference is just another name for an existing variable.
> 

---

## 5. Key Properties of a Reference (from the instructor's notes)

These four rules matter a lot, and they're exactly what separates a reference from a pointer (which we'll get to in Part 2):

| Property | Meaning |
| --- | --- |
| **Must be initialized at declaration** | You cannot write `int &ref;` and assign it something later — it must be tied to a variable the moment it's created |
| **No separate memory** | A reference doesn't occupy its own independent memory block — it's just another name for existing memory |
| **Cannot be null** | A reference must always refer to a real, existing variable — there's no such thing as a reference to "nothing" |
| **Cannot be reassigned** | Once `ref` is bound to `a`, you can't later make `ref` refer to some *different* variable `b` — it's locked in forever |

```cpp
int a = 10;
int &ref = a;   // ✅ correctly initialized at declaration

int &ref2;       // ❌ ERROR — a reference must be initialized immediately
```

> Contrast this against pointers, which — as we'll see in Part 2 — *can* be reassigned, *can* be null, and *do* occupy their own memory. This is the core distinction between the two concepts.
> 

---

## 6. Where References Actually Matter: Call by Value vs. Call by Reference

This is the "aha" moment of the whole references topic — the instructor deliberately builds up a puzzle here, so let's walk through it exactly as he did.

### The Setup — A `modify` Function

```cpp
int modify(int a) {
    a = a + 10;
    return a;
}

int main() {
    int x = 10;
    cout << modify(x);   // what do you expect?
    cout << x;             // what do you expect?
    return 0;
}
```

**What you'd expect:** Since we "modified" the value inside `modify()`, you might expect both lines to print `20`.

**What actually happens:**

```
modify(x)  →  20   ✅ (as expected)
x          →  10   ❌ (wait, what?!)
```

### Why Doesn't `x` Change?

> This happens because of **pass by value** — when you pass a variable into a function *normally* (without `&`), a **copy** of that variable is created inside the function. Any changes made inside the function happen to that *copy*, not to the original.
> 

```
main():  x = 10
              │
              │  (a COPY of x's value — 10 — is handed over)
              ▼
modify(a):  a = 10  →  a = a + 10 = 20  →  return 20

Back in main(): x is STILL 10 — only the copy (a) was ever touched
```

This is exactly the same idea as **local scope** from Lecture 3: once `modify()`'s scope ends, the copy `a` disappears entirely — it never had any real connection to `x` in the first place.

### The Fix — Call by Reference

If we actually want the function to change the *original* variable, we pass it **by reference**, using `&` in the parameter list:

```cpp
int modify(int &a) {
    a = a + 10;
    return a;
}

int main() {
    int x = 10;
    cout << modify(x);   // 20
    cout << x;             // 20  ✅ now it actually changed!
    return 0;
}
```

> What changed? By writing `int &a` in the parameter, `a` is no longer a separate copy — it becomes **another name for `x` itself**. So when the function does `a = a + 10`, it's directly modifying the same memory location that `x` occupies.
> 

```
PASS BY VALUE:                          PASS BY REFERENCE:
┌─────────────┐   copy of value        ┌─────────────┐   direct link
│ main: x=10   │ ──────────────▶       │ main: x=10   │ ◀═══════════
└─────────────┘                        └─────────────┘
      │                 ┌────────────┐        │            ┌────────────┐
      │ x unaffected    │ func: a=10 │        │ x changes  │ func: &a   │
      ▼                 └────────────┘        ▼            └────────────┘
   x = 10 (still 10)                        x = 20 (changed!)
```

### Side-by-Side Comparison

|  | Pass by Value | Pass by Reference |
| --- | --- | --- |
| **What's passed** | A copy of the variable | A direct alias to the original variable |
| **Changes inside function** | Do **not** affect the original | **Do** affect the original |
| **Syntax** | `int modify(int a)` | `int modify(int &a)` |
| **Default in C++?** | Yes | No — you must explicitly write `&` |

The official notes phrase this example slightly differently but identically in spirit:

```cpp
void update(int &x) {
    x = x + 10;
}
```

Calling `update(someVariable)` will permanently change `someVariable` in the caller, because `x` here is just another name for it.

> **The takeaway:** Whenever you want a function to actually modify the caller's original variable — instead of quietly working on a throwaway copy — pass that variable **by reference**.
> 

---

## 7. Reference as a Return Type

References aren't just useful as function *parameters* — a function can also **return** a reference.

### The Motivating Example

```cpp
int numbers[3] = {10, 20, 30};
```

Suppose we want a function that lets us reach *into* this array and modify a specific index directly.

```cpp
int& getValue(int arr[], int index) {
    return arr[index];
}
```

Notice the `&` sitting right after `int` in the return type — this tells C++: **"this function isn't returning a copy of the value — it's returning a reference to the actual element itself."**

### Using It

```cpp
getValue(numbers, 1) = 100;
```

This looks unusual at first — assigning a value directly to a function call! But because `getValue()` returns a **reference** to `numbers[1]` (not a copy of its value), this line is really saying: *"whatever `numbers[1]` refers to, set it to 100."*

**Result:**

```
Before: numbers = {10, 20, 30}
After:  numbers = {10, 100, 30}
```

The official notes give a nearly identical example, finding the larger of two values and returning a reference to it:

```cpp
int &max(int &a, int &b) {
    if (a > b)
        return a;
    else
        return b;
}
```

```cpp
max(x, y) = 100;
```

> Whichever of `x` or `y` is larger, this line directly overwrites that original variable with `100` — because `max()` handed back a reference to the real variable, not a disposable copy of its value.
> 

> **The general rule:** If a function's return type is written with an `&` (like `int&`), it's returning a **reference**, meaning the caller can use that returned value as if it *were* the original variable — including modifying it directly.
> 

---

## Key Points to Remember (Part 1)

- Memory addresses are stored in binary internally, but displayed in **hexadecimal** (`0xAC7E`) because it's far more compact and readable. A null/empty address is shown as `0x0`.
- The **address-of operator (`&`)** retrieves a variable's memory address: `&a` means "the address of `a`."
- A **reference** is an **alias** — another name for an existing variable, pointing at the *exact same* memory location. It is **not** a separate copy.
- Changing a variable through its reference changes the original too, since there's genuinely only one piece of data, just accessible under two names.
- References have four strict properties: must be **initialized at declaration**, have **no separate memory** of their own, **cannot be null**, and **cannot be reassigned** to refer to a different variable later.
- **Pass by value** (default in C++) hands a function a **copy** — changes inside the function never reach the original variable.
- **Pass by reference** (`int &a` in the parameter) hands the function a direct alias to the original — changes made inside the function directly affect the caller's variable.
- A function can also **return a reference** (`int &functionName(...)`), which lets the caller modify the original variable through the function's return value — as seen in both the `getValue()` and `max()` examples.

# 🎯Part 2: Pointers in C++

---

## 1. What Is a Pointer?

If references made sense, pointers won't feel like a big leap — the core idea (dealing with addresses) is identical. The difference is *how* that address gets used.

> Pointers become unavoidable once you reach **linked lists, trees, and graphs** in DSA — you simply cannot build these structures without understanding pointers properly.
> 

### Building the Idea

```cpp
int a = 10;
```

`a` is a normal variable. It holds `10`, and it lives at some memory address — say, for the sake of example:

```
a = 10
address of a = 100
```

Now let's create a *new* kind of variable — one that doesn't store data like `10` directly, but instead stores **an address**.

> **A pointer is a variable that stores the memory address of another variable.**
> 

So if `a` lives at address `100`, a pointer pointing to `a` would simply hold the value `100` inside itself.

```
        ptr
         │
         │ (stores: 100)
         ▼
┌─────────────────┐
│   a = 10        │  ← address 100
└─────────────────┘
```

> Why do we call it a "pointer"? Because it's literally **pointing toward** the address of another variable — it doesn't hold the data itself, just directions to where the data lives.
> 

---

## 2. Creating a Pointer

To create a pointer that stores `a`'s address, C++ uses this syntax:

```cpp
int a = 10;
int *ptr = &a;
```

Let's break this down piece by piece:

```
   int    *    ptr    =    &a
    │     │     │          │
 data    "this  name     address
 type    is a             of a
        pointer"
```

- `&a` → the address-of operator, giving us `a`'s address (exactly as in Part 1).
- `ptr` → the  here tells the compiler: **"`ptr` is a pointer, not a regular variable."**

> Just like a normal variable needs a data type, a pointer needs one too — and the pointer's data type should **match** the data type of the variable whose address it's storing. Since `a` is an `int`, `ptr` is declared as `int *ptr`.
> 

### Quick Check

```cpp
int a = 10;
int *ptr = &a;

cout << a;       // 10          → the value stored in a
cout << &a;      // 0x...       → the address of a
cout << ptr;     // 0x...       → same address as &a!
```

> Since we stored `&a` inside `ptr`, printing `ptr` directly gives you the **same address** as printing `&a`. That's the whole mechanism — `ptr` is simply *holding* that address as its value.
> 

```
┌──────────────────────────────────────────────┐
│         CREATING A POINTER                   │
│                                              │
│  int a = 10;      → normal variable          │
│  int *ptr = &a;   → pointer to a             │
│                                              │
│  Syntax:  data_type *pointer_name = &var;    │
└──────────────────────────────────────────────┘
```

---

## 3. Pointer-to-Pointer

Since a pointer is itself just another variable, it has its own memory address too — meaning we can create a pointer that points to *that* pointer.

### Building the Picture

```cpp
int a = 10;         // address of a, say, = 100
int *ptr = &a;      // ptr stores 100. But ptr itself lives at, say, address 200.
```

Now let's create a second pointer that stores `ptr`'s address (200):

```cpp
int **ptr2 = &ptr;
```

```
ptr2 ────────► ptr ────────► a
  (stores 200)   (stores 100)   (= 10)
```

> This is called a **pointer to a pointer**. Just like a pointer can store the address of a normal variable, it can equally store the address of *another pointer* — there's nothing special stopping it.
> 

### Why Two Stars?

| Declaration | Meaning |
| --- | --- |
| `int *ptr` | Pointer to an `int` |
| `int **ptr2` | Pointer to a pointer to an `int` |

> Each extra `*` represents one more "layer" of pointing. `int *` points directly at an integer's address. `int **` points at the address of *another pointer*, which itself points at the integer.
> 

### Dereferencing a Pointer-to-Pointer

```cpp
int a = 5;
int *ptr = &a;
int **ptr2 = &ptr;

cout << *ptr;    // 5   → the value of a
cout << *ptr2;   // the value stored in ptr, i.e., the address of a
cout << **ptr2;  // 5   → drilling all the way down to a's actual value
```

> `*ptr2` gives you what's *inside* `ptr2` one level down — which happens to be `ptr`'s value (an address). `**ptr2` dereferences **twice**, drilling all the way down to `a`'s actual value.
> 

(We'll properly explain the dereference operator `*` in the very next section — for now, just notice the pattern: one star peels back one layer of pointing.)

---

## 4. The Dereference Operator ()

This is a small but crucial concept — don't let the same symbol (`*`) used in *declaring* a pointer confuse you with this different *usage* of `*`.

### The Setup

```cpp
int a = 10;
int *ptr = &a;
```

```
a = 10, address 100
ptr = 100   (stores a's address)
```

Now:

```cpp
cout << ptr;    // 100  → gives you the ADDRESS
```

But what if you want to know: **"what value is actually sitting at the address this pointer is pointing to?"** That's exactly what the dereference operator does.

```cpp
cout << *ptr;   // 10   → gives you the VALUE at that address
```

> The **dereference operator (`*`)**, when placed before an already-declared pointer, is used to access **the value stored at the address the pointer is pointing to.**
> 

```
┌───────────────────────────────────────────┐
│  ptr   →  gives you the ADDRESS           │
│  *ptr  →  gives you the VALUE at          │
│           that address                    │
└───────────────────────────────────────────┘
```

The beauty here: you extracted `a`'s value (`10`) *without ever directly referring to `a`* — purely through the pointer.

### Reference Table (from the instructor's notes)

| Operator | Meaning |
| --- | --- |
| `&` | Address of |
| `*` | Dereference (value at that address) |

```cpp
cout << p;    // address
cout << *p;   // value
```

---

## 5. The Relationship Between Arrays and Pointers

This connects directly back to Lecture 9 (Arrays) — and reveals something that was quietly true the whole time.

### Setting Up

```cpp
int arr[3] = {10, 20, 30};
```

You'd naturally assume `arr` is "the name of the array." Actually:

> When you write just `arr` (with no index), it does **not** print the whole array. Instead, `arr` behaves exactly like a pointer — specifically, it holds the **address of the first element**, `arr[0]`.
> 

```cpp
cout << arr;   // prints an address, not the array contents!
```

```
arr ───────► arr[0]  (10)   arr[1]  (20)   arr[2]  (30)
             address 100     address 104     address 108
```

> This means `arr` is essentially a pointer. We didn't discuss this back in Lecture 9 because your understanding of addressing wasn't built up yet — now that you understand pointers, this connection makes complete sense.
> 

### Accessing Elements Through Dereferencing

```cpp
cout << *arr;         // 10  → the value at arr[0]
cout << *(arr + 1);   // 20  → the value at arr[1]
cout << *(arr + 2);   // 30  → the value at arr[2]
```

This works because of **contiguous memory allocation** (recall Lecture 9): since array elements sit at consecutive addresses, moving *forward* through the array is just a matter of moving forward through memory addresses.

---

## 6. Pointer Arithmetic

Here's the part that trips up almost every beginner the first time: pointer arithmetic does **not** behave like normal number arithmetic.

> There are exactly **three** operations you can perform with pointer arithmetic: **increment, decrement, and comparison.**
> 

### The Critical Rule

> A pointer's increment/decrement does **not** simply add or subtract `1` from the raw address number. Instead, it moves by an amount equal to the **size of the pointer's data type.**
> 

### Worked Example — Increment

```cpp
int a = 10;
int *ptr = &a;   // suppose ptr = 100
```

You might expect:

```cpp
ptr + 1;   // "obviously" 101... right?
```

**Wrong.** Since `ptr` is an `int*`, and an `int` occupies **4 bytes** of memory (Lecture 2):

```
ptr + 1  =  100 + (1 × 4)  =  104
```

```
┌──────────────────────────────────────────────┐
│  ptr + 1  →  moves to the NEXT element       │
│  ptr - 1  →  moves to the PREVIOUS one       │
│                                              │
│  Movement size = size of the data type       │
│  (int → moves by 4 bytes at a time)          │
└──────────────────────────────────────────v───┘
```

### Worked Example — Decrement

```cpp
ptr--;
```

If `ptr = 100`, decrementing doesn't give `99` — it gives:

```
100 - 4 = 96
```

Exactly the same logic, just moving backward instead of forward.

### Worked Example — Comparison

```cpp
int arr[4] = {10, 20, 30, 40};
int *ptr1 = &arr[0];   // say, ptr1 = 100
int *ptr2 = &arr[1];   // say, ptr2 = 104

cout << (ptr1 < ptr2);   // true (1)
```

> Since `ptr1` (address 100) comes *before* `ptr2` (address 104) in memory, the comparison `ptr1 < ptr2` correctly evaluates to `true`. Pointers can be compared with `<`, `>`, `==`, and similar operators, exactly like regular relational comparisons from Lecture 4.
> 

### Iterating an Array Using a Pointer

This is a very natural, practical use of everything above — combining pointer arithmetic with a `for` loop to walk through an entire array:

```cpp
int arr[] = {10, 20, 30};
int *p = arr;

for (int i = 0; i < 3; i++) {
    cout << *(p + i) << " ";
}
```

**Output:** `10 20 30`

> Each pass through the loop, `p + i` shifts the pointer forward by `i` elements (not `i` raw bytes), and dereferencing (`*`) pulls out the actual value sitting there.
> 

### Modifying a Value Through a Pointer

You're not limited to just *reading* through a pointer — you can also *write* through one:

```cpp
*p = 50;
```

This directly overwrites whatever value the pointer is currently pointing at — in this case, setting `arr[0]` to `50`.

---

## 7. Reassigning a Pointer

Here's a major difference from references: **pointers can be pointed at a different variable after creation.** (Recall: references are locked in forever once initialized.)

```cpp
int a = 10;
int b = 20;

int *p = &a;
cout << *p;   // 10   (p points to a)

p = &b;        // reassign p to point somewhere else
cout << *p;   // 20   (p now points to b)
```

```
Before:  p ────► a (10)
After:   p ────► b (20)
```

> This flexibility — the ability to freely point a pointer at different variables over its lifetime — is exactly what references can never do, and it's one of the most important practical differences between the two concepts.
> 

---

## 8. The Null Pointer

The final piece for this lecture — and a genuinely important one, since it shows up constantly once you reach linked lists.

### The Problem

```cpp
int *ptr;      // declared, but never initialized
cout << *ptr;  // DANGEROUS — undefined behavior, program may crash
```

> An uninitialized pointer contains a **garbage address** — some leftover, unpredictable value. Dereferencing it (`*ptr`) can crash your program, because you're accessing memory you never actually meant to touch.
> 

### The Fix

> If you want a pointer to deliberately point at **nothing**, you initialize it as a **null pointer.**
> 

```cpp
int *ptr = nullptr;   // modern C++ style
```

*(You may also see the older style, `int *ptr = NULL;`, in older code or textbooks — both express the same idea.)*

```
┌─────────────────────────────────────────────┐
│  ptr = nullptr;                             │
│                                             │
│  ptr ──────► (nothing — points to no        │
│               valid memory location)        │
└─────────────────────────────────────────────┘
```

> **A null pointer is a pointer that is not currently pointing to any valid object.**
> 

### Why This Matters

Suppose a pointer is currently pointing somewhere useful, but you now want to detach it — without necessarily pointing it at something else immediately:

```cpp
int a = 10;
int *ptr = &a;   // ptr → a

ptr = nullptr;    // ptr → nothing, deliberately detached
```

> This is exactly the mechanism used in **linked lists**: the "next pointer" of the very last node is set to `nullptr`, signaling "there is nothing after this — this is the end of the list." You'll see this pattern constantly once you reach that topic in DSA.
> 

---

## Key Points to Remember (Part 2)

- A **pointer** is a variable that stores the **memory address** of another variable: `int *ptr = &a;`.
- The  in a declaration (`int *ptr`) marks the variable as a pointer; a pointer's data type should match the type of variable whose address it holds.
- A **pointer-to-pointer** (`int **ptr2 = &ptr;`) stores the address of *another pointer* — each extra  represents one more layer of indirection.
- The **dereference operator ()**, used on an already-declared pointer, retrieves the **value stored at the address** it points to — as opposed to the pointer itself, which gives you the **address**.
- An **array name** (`arr`) behaves like a pointer to its first element (`arr[0]`) — this is why `arr` gives you the first value, and `(arr + 1)` gives you the second.
- **Pointer arithmetic** (increment `++`, decrement `-`, comparison `<`/`>`/`==`) moves according to the **size of the pointer's data type** — not by raw address units. An `int*` moves by 4 bytes per step.
- Unlike references, **pointers can be reassigned** to point at a completely different variable at any time during the program.
- An uninitialized pointer holds a **garbage address** and is dangerous to dereference. A **null pointer** (`nullptr`, or the older `NULL`) deliberately points to nothing, and is the safe way to represent "this pointer isn't pointing anywhere valid right now" — a pattern used heavily in linked lists.

---

## Quick Recap Diagram — References vs. Pointers

```
┌───────────────────────────────────────────────────────────────┐
│                 REFERENCES vs. POINTERS                       │
├──────────────────────────┬────────────────────────────────────┤
│      REFERENCE           │           POINTER                  │
├──────────────────────────┼────────────────────────────────────┤
│ int &ref = a;            │ int *ptr = &a;                     │
│ Alias — same variable,   │ Separate variable that STORES      │
│ different name           │ an address                         │
│ Must init at declaration │ Can be declared, then assigned     │
│ No separate memory       │ Has its own memory & address       │
│ Cannot be null           │ Can be null (nullptr)              │
│ Cannot be reassigned     │ CAN be reassigned                  │
│ No dereference needed:   │ Needs dereference (*ptr) to        │
│ cout << ref; → value     │ get the value; cout << ptr;        │
│                          │ → gives the address                │
└──────────────────────────┴────────────────────────────────────┘
```