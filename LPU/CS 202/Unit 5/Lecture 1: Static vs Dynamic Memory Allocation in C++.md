# Static vs Dynamic Memory Allocation in C++

Status: Pending

## 1. Where This Fits — The Journey From Code to Output

![image.png](Static%20vs%20Dynamic%20Memory%20Allocation%20in%20C++/image.png)

Before jumping into static vs. dynamic memory, it helps to see the **complete path** your code travels before it actually runs:

```
┌────────────────┐   compiled by    ┌────────────────┐    executed by       ┌───────────────┐
│  **.cpp** file │ ───────────────▶ │ **.obj** file  │ ──────────────────▶  │ **.exe** file │
│ (your code)    │   the compiler   │(machine        │   your computer      │ (output) ──── │
└────────────────┘                  │  code)         │                      └───────────────┘
                                    └────────────────┘
```

> Your C++ code lives in a `.cpp` file. The **compiler** converts it into machine code, stored in a `.obj` file. That machine code is then **executed** by your computer, producing an output — which lives in the `.exe` file.
> 

### Two Time Periods That Matter

- **Compile time** → the entire duration from writing your `.cpp` code to it being compiled into machine code.
- **Runtime** → the time taken *after* that, while the program is actually running and producing output.

```
┌────────────────────────────────┬────────────────────────────────┐
│         COMPILE TIME           │           RUNTIME              │
│  (.cpp → compiled → .obj)      │   (.obj is executed → .exe)    │
└────────────────────────────────┴────────────────────────────────┘
```

> This distinction is the entire foundation of today's topic: **memory allocated at compile time is called static memory allocation. Memory allocated at runtime is called dynamic memory allocation.**
> 

### A First Example

```cpp
int x = 10;
```

When does `x` get its memory? At **compile time** — so this is an example of **static memory allocation**.

> Even a **function** (like `main()` itself) needs memory, and that memory is also allocated at compile time. So a function is also considered static memory allocation.
> 

---

## 2. The Four Logical Sections of RAM

Your RAM (memory) is logically divided into four sections — not physically separated, just organized this way conceptually:

```
┌─────────────────────────────────────┐
│           Stack Memory              │
├─────────────────────────────────────┤
│           Heap Memory               │
├─────────────────────────────────────┤
│        Global / Static Memory       │
├─────────────────────────────────────┤
│           Code Segment              │
└─────────────────────────────────────┘
```

| Segment | What It Stores | Key Traits |
| --- | --- | --- |
| **Code Segment** | The complete compiled program instructions | Read-only, fixed size |
| **Global / Static Memory** | Global and static variables | Allocated once, exists for the entire program's lifetime |
| **Stack Memory** | Local variables, function calls | Automatic allocation & deallocation, follows LIFO |
| **Heap Memory** | Dynamically allocated memory | Managed manually by the programmer, slower than stack, larger size |

```cpp
int g = 10;        // global → lives in Global/Static memory
static int x = 5;   // static → lives in Global/Static memory too
```

> Today's lecture is mainly about **stack and heap**, since those are the two sections directly tied to static vs. dynamic memory allocation.
> 

---

## 2.1 Stack Memory — Simplified View

![image.png](Static%20vs%20Dynamic%20Memory%20Allocation%20in%20C++/image%201.png)

For this lecture, think of RAM as broadly split into just two working areas:

```
┌─────────────────────────┐
│       STACK             │   ← smaller
├─────────────────────────┤
│                         │
│       HEAP              │   ← larger
│                         │
└─────────────────────────┘
```

> Heap memory is deliberately kept **larger** than stack memory, since dynamic allocations (which can be big — arrays, large data structures) live there.
> 

> Whenever you perform **static memory allocation**, that memory is allocated inside the **stack**. Whenever you perform **dynamic memory allocation**, that memory is allocated inside the **heap**.
> 

### Walking Through `main()`

```cpp
int main() {
    int a = 10;
    return 0;
}
```

- `main()` itself gets memory allocated at compile time → lives in the **stack**.
- `a = 10` is also allocated at compile time → also lives in the **stack**.
- If there were a function like `sum()` called from `main()`, it too would get its memory allocated statically, inside the stack.

> **Rule to lock in:** Whether it's the `main` function, any variable, or any normal function — all of these get their memory allocated through **static memory allocation**, at **compile time**, inside the **stack**.
> 

### Why "Stack"?

> Think of a **pile of books** — one placed on top of another, on top of another. That's a stack. In exactly the same way, memory here keeps getting stacked one on top of the other — which is why it's called **stack memory**.
> 

---

## 3. Static Memory Allocation — Formal Definition

> **Static memory allocation**: memory is allocated automatically, at **compile time**, when the program starts. The **size, type, and lifetime** of the variable are all fixed.
> 

### Characteristics

- **Faster access**
- **Automatic memory management** (you don't have to do anything — it's handled for you)
- **Limited size** (fixed, decided in advance)
- **Memory wasted if not fully used** (e.g., declaring an array of 100 but only using 10)

### Examples

**Local variable:**

```cpp
int a = 10;
```

**Fixed-size array:**

```cpp
int arr[5];
```

Both of these get memory allocated **automatically**, at **compile time**, inside the **stack**.

### Limitations

- The size must be **known in advance** — you can't decide it while the program is running.
- Risk of **stack overflow** if you try to allocate too much.
- Not suitable for handling **large or unpredictable amounts of data**.

---

## 4. Automatic Deallocation — Why the Stack "Cleans Up After Itself"

This is one of the most important practical points from the lecture.

Suppose you have a function:

```cpp
void sum() {
    int a = 5;
    // ... some logic
}
```

> When `sum()` runs, memory for `sum()` itself, and for its local variable `a`, gets allocated inside the stack. But once `sum()` finishes running — once it **goes out of scope** — that memory is **automatically deleted**.
> 

```
┌──────────────────────────────┐
│  STACK (while sum() runs)    │
│   sum()                      │
│     └── a = 5                │
└──────────────────────────────┘
        │
        │  sum() finishes → goes out of scope
        ▼
┌──────────────────────────────┐
│  STACK (after sum() ends)    │
│   (memory automatically      │
│    freed — a no longer       │
│    exists)                   │
└──────────────────────────────┘
```

> This connects directly back to **variable scope** (Lecture 3): once a function's scope ends, its local variables cease to exist — and here we see *why* that's genuinely useful. If memory were never automatically freed, running the same program repeatedly would eventually **fill up your memory completely**.
> 

> **Key takeaway:** Statically allocated memory (stack memory) is deallocated **automatically** the moment its scope ends. You don't have to do anything yourself.
> 

---

## 5. Dynamic Memory Allocation — Formal Definition

> **Dynamic memory allocation**: memory allocated **at runtime**, on the **heap**, using the **`new`** keyword.
> 

### Why Do We Need This At All?

- Sometimes the **size is unknown at compile time** — e.g., you don't know how many elements a user will need until the program is actually running and asks them.
- It allows more **efficient memory usage** — you only take exactly what you need, exactly when you need it.
- It's essential for handling **large amounts of data** that wouldn't fit comfortably (or predictably) inside fixed-size stack allocations.

### An Important Clarification About Pointers

You might have heard somewhere that "pointers use dynamic memory allocation" — this is only half-true, and the instructor is very explicit about correcting this:

```cpp
int a = 10;
int *ptr = &a;
```

> The **pointer variable itself** (`ptr`) — the box that stores an address — is allocated **statically**, inside the **stack**, at compile time. Just like any other variable, it has a name (`ptr`) and a fixed size.
> 

> **Only** the memory created using the **`new`** keyword is what actually counts as dynamic memory allocation, and *that* memory lives inside the **heap**, allocated at **runtime**.
> 

```
┌────────────────────────────────────────────────────┐
│  int *ptr = new int;                               │
│                                                    │
│      STACK:                      HEAP:             │
│  ┌──────────────┐           ┌──────────────┐       │
│  │ ptr (stores  │ ───────▶  │  (no name!)  │       │
│  │  an address) │           │  4 bytes,    │       │
│  │  — STATIC    │           │  int-sized   │       │
│  └──────────────┘           │  — DYNAMIC   │       │
│                             └──────────────┘       │
└────────────────────────────────────────────────────┘
```

### The Defining Feature: No Name

> Statically allocated memory always has a **name** — `a` is a variable's name, `main` is a function's name, `ptr` is a pointer's name. All of these also have **fixed sizes**.
> 

> But memory allocated dynamically has **no name at all**. The only way to reach it is **through a pointer**.
> 

---

## 6. The `new` Keyword — Allocating Memory Dynamically

### Allocating a Single Variable

```cpp
int *ptr = new int;   // allocates 4 bytes on the heap — no data yet
*ptr = 100;             // now store 100 at that heap address
```

> `new int` creates a fresh block of integer-sized memory inside the **heap**. It doesn't come with a value already inside it — you dereference the pointer (`*ptr = 100`) to actually place data there.
> 

You can also combine allocation and initialization in one line:

```cpp
int *ptr = new int(50);   // allocates memory AND immediately stores 50 inside it
```

### Accessing the Value

Since this memory has no name, the **only** way to reach it is through the pointer, using the dereference operator:

```cpp
cout << *ptr;   // prints 100 (or whatever value was stored)
```

```
┌────────────────────────────────────────────┐
│  int *ptr = new int;                       │
│  *ptr = 100;                               │
│                                            │
│  STACK: ptr ───────▶ HEAP: [ 100 ]         │
│         (has a name)      (no name —       │
│                             reached only   │
│                             via ptr)       │
└────────────────────────────────────────────┘
```

---

## 7. The `delete` Keyword — Freeing Dynamic Memory

Here's the critical difference from stack memory: **dynamic memory is NOT automatically deleted.**

> Unlike the stack, memory allocated dynamically using `new` does **not** get freed automatically once a function or scope ends. **You** must free it yourself, using the **`delete`** keyword.
> 

```cpp
int *ptr = new int(100);
cout << *ptr;    // 100
delete ptr;        // manually free this memory
```

> As soon as you run `delete ptr;`, that heap memory block becomes **free** — available to be reused. If you forget to do this, that memory remains "stuck," wasted, and unusable for the rest of the program's run. This is called a **memory leak** (more on this in Section 9).
> 

---

## 8. Dynamically Allocating an Array

This is where dynamic memory allocation becomes genuinely powerful — arrays whose size isn't known until the program is actually running.

### Declaring & Filling a Dynamic Array

```cpp
int n;
cin >> n;                  // ask the user for the size, AT RUNTIME

int *arr = new int[n];     // allocate an array of size n, on the heap

for (int i = 0; i < n; i++) {
    cin >> arr[i];           // fill it with user input
}
```

> Notice the **square brackets** `new int[n]` — this is the syntax specifically for allocating an **array** dynamically. For a single variable, you'd just use `new int` (no brackets). For an array, you always use `new int[size]`.
> 

### Where Does Everything Live?

```
    STACK:                                HEAP:
┌──────────────┐                 ┌───┬───┬───┬───┬───┐
│ arr (stores  │ ─────────────▶  │ 0 │ 1 │ 2 │...│n-1│  ← array of size n
│ the address  │                 └───┴───┴───┴───┴───┘
│ of index 0)  │                  (no name — only
│ — STATIC     │                  reachable via arr)
└──────────────┘
```

> Just as we learned in the **Pointers lecture**: `arr` itself is really just a **pointer** — it stores the address of index `0`. The pointer `arr` lives in the stack; the actual array data lives in the heap.
> 

### Deleting a Dynamic Array

```cpp
delete[] arr;
```

> **Important distinction:** when deleting a single dynamically-allocated variable, you write `delete ptr;`. But when deleting a dynamically-allocated **array**, you must use `delete[] arr;` — with the square brackets. Using the wrong form (`delete arr;` instead of `delete[] arr;`) is a common mistake, and we'll see exactly why it matters in the errors section below.
> 

---

## 9. Static vs. Dynamic — Side-by-Side Comparison Table

This table is genuinely useful for revision and frequently shows up in placement-style MCQs:

| Feature | Static Memory Allocation | Dynamic Memory Allocation |
| --- | --- | --- |
| **When allocated** | Compile time | Runtime |
| **Where it lives** | Stack (or global/static segment) | Heap |
| **Size** | Fixed | Can change (dynamic size) |
| **Who allocates it** | The compiler | The programmer (manually, using `new`) |
| **Example** | `int a = 10;` | `int *arr = new int[5];` |
| **Lifetime** | Until scope ends | Until you manually `delete` it |
| **Deallocation** | Automatic | Manual (using `delete` / `delete[]`) |
| **Speed** | Fast | Slightly slower |

---

## 10. Errors Associated With Dynamic Memory

Since dynamic memory management is entirely **your** responsibility, several classic bugs can creep in if you're not careful. These are also common **interview and OA topics**.

### 10.1 Memory Leak

> A **memory leak** occurs when memory is allocated (using `new`) but never freed (never `delete`d).
> 

```cpp
int *p = new int(10);
// missing delete — this memory is now permanently wasted
```

> That block of heap memory remains reserved for the rest of the program's execution, doing nothing useful — a genuine waste of resources.
> 

### 10.2 Dangling Pointer

This is a subtler, more dangerous bug.

```cpp
int *p = new int(5);
delete p;        // memory is now freed...
// ...but p still POINTS to that (now-invalid) address!
```

> After `delete p;`, the memory itself has been freed — but the pointer `p` is still holding onto that same old address. This is called a **dangling pointer**: a pointer that still points to memory that no longer belongs to it.
> 

**The Fix:**

```cpp
int *p = new int(5);
delete p;
p = nullptr;      // immediately point it to null, right after deleting
```

> Right after deleting memory, it's good practice to immediately set the pointer to `nullptr` (recall the **Pointers & References lecture**) — this way, the pointer clearly points to "nothing" instead of dangerously pointing to freed memory that could cause unpredictable behavior if accidentally used again.
> 

```
┌──────────────────────────────────────────────────┐
│  BEFORE delete:   p ──────▶ [ 5 ]  (valid)       │
│  AFTER delete:    p ──────▶ [ ??? ] (freed,      │
│                              dangling!)          │
│  AFTER p = nullptr:  p ──────▶ (nothing, safe)   │
└──────────────────────────────────────────────────┘
```

### 10.3 Double Deletion

```cpp
delete p;
delete p;   // ERROR — deleting the same memory twice
```

> Once memory has been freed, trying to `delete` it **again** is an error — the memory isn't there for you to delete a second time. This is exactly why setting the pointer to `nullptr` after the first `delete` also helps prevent this: deleting a `nullptr` is actually safe and does nothing, rather than crashing.
> 

### 10.4 Wild Pointer

```cpp
int *p;         // declared, but never initialized
*p = 10;         // DANGEROUS — dereferencing an uninitialized pointer
```

> A pointer that has never been initialized at all holds a **garbage address** — you have no idea what memory it's actually pointing to. Dereferencing it and writing data (`*p = 10`) can corrupt memory you never intended to touch, or crash your program outright.
> 

### 10.5 Using `delete` Instead of `delete[]`

```cpp
int *arr = new int[5];
delete arr;    // WRONG — should be delete[] arr;
```

> Since `arr` was allocated as an **array** (`new int[5]`), it must be deallocated using the array form, `delete[] arr;`. Using plain `delete arr;` on array memory is incorrect and can lead to undefined behavior — the two forms are not interchangeable.
> 

### Quick Reference — Errors Table

| Error | Cause | Fix |
| --- | --- | --- |
| **Memory Leak** | Allocated with `new`, never `delete`d | Always pair every `new` with a `delete` |
| **Dangling Pointer** | Pointer still points to freed memory | Set pointer to `nullptr` immediately after `delete` |
| **Double Deletion** | Calling `delete` twice on the same memory | Set pointer to `nullptr` after first `delete` |
| **Wild Pointer** | Using an uninitialized pointer | Always initialize pointers (even to `nullptr`) before use |
| **Wrong delete form** | Using `delete` on array memory instead of `delete[]` | Always match: single → `delete`, array → `delete[]` |

---

## Key Points to Remember

- The journey of a program is: `.cpp` file → compiled by the compiler → `.obj` machine code → executed → `.exe` output. **Compile time** covers writing-to-compiling; **runtime** covers actual execution.
- **Static memory allocation** happens at **compile time**, lives in the **stack**, has a fixed size, and is **deallocated automatically** once its scope ends.
- **Dynamic memory allocation** happens at **runtime**, lives in the **heap**, has a size that can be decided while the program runs, and must be **deallocated manually** using `delete`.
- RAM is logically divided into four sections: **Code Segment**, **Global/Static Memory**, **Stack**, and **Heap** — this lecture focuses on the latter two.
- A pointer **variable itself** is always statically allocated (it lives in the stack) — but memory created via the **`new`** keyword is what's genuinely dynamic, and lives in the heap.
- Dynamically allocated memory has **no name** — the only way to access it is **through a pointer**, using the dereference operator (`ptr`).
- Use `new` to allocate (`new int`, `new int(value)`, `new int[n]` for arrays) and `delete` / `delete[]` to deallocate — always match single-variable `delete` with single-variable `new`, and array `delete[]` with array `new[]`.
- Five classic dynamic-memory bugs to watch for: **memory leaks** (forgetting `delete`), **dangling pointers** (pointer surviving after its memory is freed), **double deletion**, **wild pointers** (uninitialized), and using **`delete` instead of `delete[]`** on arrays.
- The **static vs. dynamic comparison table** (timing, location, size, allocator, lifetime, deallocation, speed) is genuinely useful for placement-style MCQs — worth memorizing cleanly.

---