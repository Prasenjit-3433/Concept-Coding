# Unit II – Class 2: Pointer Arithmetic & Types

Status: Pending

*(Pointers, Reference Variables, Arrays and String Concepts — CS202)*

---

## Quick Recap Before We Begin

In Class 1, we learned a pointer stores an **address**, and `*` lets us access the value at that address. Today we go one step further: what happens when you do *math* on a pointer, and what special "types" of pointers exist?

---

## 1. Pointer Arithmetic — Why It's Different from Normal Math

When you add `1` to a normal number, it just increases by 1.
But when you add `1` to a **pointer**, it doesn't move forward by 1 byte — it moves forward by **the size of the data type it points to**.

```cpp
int arr[5] = {10, 20, 30, 40, 50};
int* p = arr;      // p points to arr[0]

cout << p;         // e.g. 0x1000
cout << p + 1;      // e.g. 0x1004  (jumped by 4 bytes, because int = 4 bytes)
```

**Real-life analogy:** Imagine a **parking lot** where every car takes up exactly 4 parking slots (because it's a big car). If you're standing at car #1 and someone says "move to the next car," you don't move 1 slot — you move 4 slots, because that's the width of one car. A pointer works the same way: `p + 1` doesn't mean "add 1 byte," it means "move to the next *element* of that type."

```
Array in memory (int = 4 bytes each):

Address:   0x1000    0x1004    0x1008    0x100C    0x1010
          ┌────────┬────────┬────────┬────────┬────────┐
Value:    │   10   │   20   │   30   │   40   │   50   │
          └────────┴────────┴────────┴────────┴────────┘
             p        p+1       p+2      p+3      p+4
```

So if `int` is 4 bytes: `p + 1` moves the address forward by 4 bytes.
If it were `char* p` (1 byte each): `p + 1` would move forward by only 1 byte.
If it were `double* p` (8 bytes each): `p + 1` would move forward by 8 bytes.

---

## 2. Increment (`++`) and Decrement (`-`) on Pointers

```cpp
int arr[5] = {10, 20, 30, 40, 50};
int* p = arr;

cout << *p;    // 10  (value at arr[0])
p++;             // move to next int-sized slot
cout << *p;    // 20  (value at arr[1])
p++;
cout << *p;    // 30  (value at arr[2])
```

**Real-life analogy:** Think of a **train with numbered compartments**, each compartment being the same size. `p++` is like walking from your current compartment into the *next* compartment — you don't end up halfway inside a wall, you land exactly at the start of the next compartment. That's because the compiler already knows the "compartment size" (data type size) and adjusts automatically.

```cpp
p--;    // move back one int-slot (from arr[2] back to arr[1])
```

---

## 3. Adding/Subtracting a Number from a Pointer

```cpp
int arr[5] = {10, 20, 30, 40, 50};
int* p = arr;

cout << *(p + 3);   // 40  (jumps 3 elements ahead, then dereferences)
```

**Real-life analogy:** If `p` points to house #1 on a street of identical houses, `p + 3` is like saying "walk 3 houses down the street" — and `*(p + 3)` is "walk 3 houses down, then knock on the door and see who's there."

```
p       →  house 1 (10)
p + 1   →  house 2 (20)
p + 2   →  house 3 (30)
p + 3   →  house 4 (40)   ← *(p+3) = 40
```

This is actually the exact mechanism behind **array indexing** — `arr[i]` is really just shorthand for `*(arr + i)` internally. We'll explore this fully in Class 3.

---

## 4. Subtracting Two Pointers

If two pointers point into the *same array*, subtracting them tells you **how many elements apart** they are (not how many bytes).

```cpp
int arr[5] = {10, 20, 30, 40, 50};
int* p1 = &arr[1];   // points to 20
int* p2 = &arr[4];   // points to 50

cout << (p2 - p1);   // prints 3 (they are 3 elements apart)
```

**Real-life analogy:** If you're at house #2 and your friend is at house #5 on the same street, and someone asks "how many houses apart are you?", you'd say "3 houses" — not "the difference in house numbers in feet." Pointer subtraction gives you the "number of houses," i.e., the number of elements, automatically accounting for the type size.

⚠️ **Important rule:** You can only meaningfully subtract two pointers if they point into the **same array**. Subtracting pointers into unrelated memory gives a meaningless result.

---

## 5. Comparing Pointers

Pointers can be compared using `==`, `!=`, `<`, `>`, just like numbers — this tells you their relative position in memory.

```cpp
if (p1 < p2) {
    cout << "p1 comes before p2 in memory";
}
```

This is very useful in loops that walk through arrays using pointers (which we'll do in Class 3), e.g., `while (p != endOfArray)`.

---

## 6. Types of Pointers

### a) Pointer to Different Data Types

We already saw `int*`, but pointers can point to any data type:

```cpp
int* pi;      // points to int
float* pf;    // points to float
char* pc;     // points to char
double* pd;   // points to double
```

Each behaves the same conceptually (stores an address), but the compiler treats the "jump size" differently for pointer arithmetic, and `*` on each gives back the correct type of value.

---

### b) Pointer to Pointer (Double Pointer)

A pointer doesn't just have to point to a normal variable — it can point to **another pointer**.

```cpp
int age = 20;
int* p = &age;      // p points to age
int** pp = &p;        // pp points to p (a pointer to a pointer!)

cout << age;    // 20
cout << *p;     // 20  (value age holds)
cout << **pp;   // 20  (go to p, then go to age)
```

**Real-life analogy:** Imagine a **treasure hunt with two clues**.

- Clue 2 (`pp`) doesn't point directly to the treasure. It points to **Clue 1's location**.
- Clue 1 (`p`) points to the actual treasure (`age`).
- To get the treasure, you first go to where Clue 2 points (find Clue 1), then follow Clue 1 to actually find the treasure.

```
pp ──────► p ──────► age
(points    (points    (value: 20)
 to p)      to age)

**pp  =  *p  =  age  =  20
```

```
Memory Layout:

 age                p                   pp
┌──────┐      ┌──────────┐      ┌────────────┐
│  20  │◄─────┤ addr of  │◄─────┤  addr of p │
└──────┘      │  age     │      └────────────┘
              └──────────┘
```

Double pointers become especially useful later when working with dynamic 2D arrays and when a function needs to modify what a pointer itself points to (not just the value it points to) — we'll see this in later classes.

---

### c) Null Pointer

A pointer that is intentionally set to point to **nothing** — it doesn't hold the address of any valid variable.

```cpp
int* p = nullptr;   // modern C++ way (preferred)
int* p = NULL;        // older C-style way
int* p = 0;             // also technically valid, but less clear
```

**Real-life analogy:** A null pointer is like a **delivery address slip that's completely blank**. If a delivery driver tries to deliver to a blank address, there's nowhere to go — it's a clear, intentional signal of "no destination," rather than a random/garbage address.

⚠️ **Why this matters:** If you try to dereference (`*p`) a null pointer, your program **crashes** (this is called a "null pointer dereference" — one of the most common runtime errors in C++).

```cpp
int* p = nullptr;
cout << *p;   // ❌ CRASH — trying to access "nothing"
```

Good practice: always check before dereferencing.

```cpp
if (p != nullptr) {
    cout << *p;
}
```

---

### d) Void Pointer (Generic Pointer)

A pointer that can point to **any data type**, but doesn't know what type it's pointing to until you tell it.

```cpp
void* vp;

int x = 10;
vp = &x;     // vp can point to an int...

float y = 3.14;
vp = &y;     // ...or now point to a float — totally valid!
```

**Real-life analogy:** A void pointer is like a **universal parking spot with no label** — it can hold a car, bike, or truck (any type), but since there's no label, you first need to tell the system what kind of vehicle is actually parked there before it can figure out how much space it occupies.

⚠️ **Important restriction:** You **cannot** directly dereference a void pointer without first casting it back to a specific type, because the compiler doesn't know the "size" to read.

```cpp
void* vp = &x;
cout << *vp;              // ❌ ERROR — compiler doesn't know the type
cout << *(int*)vp;        // ✅ correct — explicitly cast back to int*
```

Void pointers are mostly used in low-level/generic programming (e.g., malloc() in C returns a void pointer) — not something you'll use often as a beginner, but important to recognize.

---

### e) Dangling Pointer

A pointer that still holds an address, but that address is **no longer valid** — because the variable it once pointed to has gone out of scope, been deleted, or otherwise stopped existing.

```cpp
int* getDanglingPointer() {
    int x = 5;
    return &x;     // ❌ returning address of a LOCAL variable
}   // x is destroyed here, once function ends

int main() {
    int* p = getDanglingPointer();
    cout << *p;    // ❌ undefined behavior — x doesn't exist anymore!
}
```

**Real-life analogy:** Imagine you wrote down your friend's hotel room number (`p = &x`). Your friend then **checks out of the hotel** (the variable `x` goes out of scope once the function ends), and the hotel gives that room to someone else entirely. Your notecard still says "Room 204," but if you show up expecting your friend, you'll find a **stranger** — or an empty room. Using the pointer at that point gives unpredictable/garbage results.

```
Before:  p ──────► Room 204 (contains x = 5)
After:   p ──────► Room 204 (x is GONE — room reassigned or empty)
                     ⚠️ p still holds this address, but it's invalid now
```

This also happens with `delete`:

```cpp
int* p = new int(10);
delete p;        // memory freed
cout << *p;    // ❌ p is now dangling — points to freed memory
```

Good practice: set a pointer to `nullptr` right after deleting it, so it's not left "dangling."

```cpp
delete p;
p = nullptr;
```

---

## 7. Recap Table — Types of Pointers

| Type | Meaning | Real-life analogy |
| --- | --- | --- |
| Typed pointer (`int*`, `float*`...) | points to a specific data type | address slip for a specific house type |
| Pointer to pointer (`int**`) | points to another pointer | a clue that points to another clue, which points to treasure |
| Null pointer | points to nothing (intentionally) | a blank address slip |
| Void pointer | can point to any type (generic) | unlabeled universal parking spot |
| Dangling pointer | points to invalid/freed memory | an old address slip for a hotel room your friend already checked out of |

---

## 8. Practice Questions for Students

1. Given `int arr[4] = {5, 10, 15, 20}; int* p = arr;` — what is the value of `(p + 2)`?
2. What will happen if you run this code?

```cpp
int* p;
cout << *p;
```

(Hint: is `p` initialized to anything?)

1. Explain in your own words the difference between a **null pointer** and a **dangling pointer**.
2. Write code to declare a double pointer `pp` that eventually lets you print the value `50` stored in an `int x = 50;`, going through two levels of pointers.

---

That wraps up **Class 2: Pointer Arithmetic & Types**. This naturally sets up **Class 3 – Pointers and Arrays**, where we'll connect everything here directly to array indexing and traversal. Let me know when you're ready to continue.