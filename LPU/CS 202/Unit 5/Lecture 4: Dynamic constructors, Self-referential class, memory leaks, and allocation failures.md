# Lecture 4: Dynamic constructors, Self-referential class, memory leaks, and allocation failures


## Where This Fits

```
┌────────────────────────────────────────────────────────────┐
│  Lecture 3:   Early/late binding, vtable, virtual          │
│                  destructors, concrete vs abstract class   │
├────────────────────────────────────────────────────────────┤
│  Lecture 4: (this note)                                    │
│    5. Dynamic constructors                                 │
│    6. Self-referential class                               │
│    7. Memory leak and allocation failures                  │
└────────────────────────────────────────────────────────────┘
```

All three topics build on **Lecture 1 (Static vs Dynamic Memory)** and on the **destructor and copy constructor** work from Unit 3. Quickly recall the key facts:

```
   new      → asks the HEAP for memory      (at run time)
   delete   → gives it back                 (YOU must do it)
   Heap memory has NO name → reachable only through a pointer
```

---

# 🎯 Part 5: Dynamic Constructors

## 1. The Problem

Suppose a class stores a name, but the length of the name is not known until the program runs:

```cpp
class Student {
    char name[20];      // fixed: wastes space for "Ravi", too small for a long name
};
```

A fixed array wastes memory or runs out of room. We want each object to take **exactly** the memory it needs.

## 2. What Is a Dynamic Constructor?

> A **dynamic constructor** is a constructor that allocates memory **at run time using `new`** for the object's data members.
> 

It is not a new kind of constructor. It is an ordinary constructor (usually a parameterized one) whose body contains `new`.

```
   **Normal constructor**:      stores values in members that already have space
   
   **Dynamic constructor**:     first ASKS THE **HEAP** for space with  **new**,
                            then stores values in it
```

## 3. Example: A Dynamic Array Inside a Class

```cpp
#include <iostream>
using namespace std;

class Marks {
    int* data;               // pointer member (the pointer is on the stack)
    int size;

public:
    Marks(int n) {                      // **DYNAMIC constructor**
        size = n;
        data = new int[size];           // memory taken from the heap at run time
        for (int i = 0; i < size; i++) {
            data[i] = 0;
        }
        cout << "Allocated " << size << " ints" << endl;
    }

    void set(int index, int value) { data[index] = value; }

    void display() {
        for (int i = 0; i < size; i++) {
            cout << data[i] << " ";
        }
        cout << endl;
    }

    ~Marks() {                          // MUST free what the constructor took
        delete[] data;
        cout << "Freed" << endl;
    }
};

int main() {
    int n;
    cout << "How many subjects? ";
    cin >> n;                           // size is known only at run time

    Marks m(n);
    m.set(0, 90);
    m.set(1, 85);
    m.display();
    return 0;
}
```

**Sample run (n = 3):**

```
How many subjects? 3
Allocated 3 ints
90 85 0
Freed
```

```
  STACK                         HEAP
 ┌───────────────┐          ┌─────────────────┐
 │ m             │          │  90 | 85 | 0    │  ← size decided at run time
 │  data ────────┼────────▶ └─────────────────┘
 │  size = 3     │
 └───────────────┘
```

### Tracing Through It

```
Marks m(n);                  n is read from the user, e.g. 3
   ↓
Constructor runs:
   data = new int[3]         heap block created (size chosen at run time)
   ↓
...program uses m...
   ↓
main ends → ~Marks() runs automatically
   delete[] data;            heap block returned
```

## 4. Dynamic Constructor for a String

The same idea works for text. This is the common exam example:

```cpp
#include <iostream>
#include <cstring>
using namespace std;

class MyString {
    char* str;

public:
    MyString(const char* s) {
        str = new char[strlen(s) + 1];      // +1 for the '\0' (Unit 0, Lecture 10)
        strcpy(str, s);
    }

    void display() { cout << str << endl; }

    ~MyString() { delete[] str; }
};

int main() {
    MyString a("Sachin");
    MyString b("LearnYard Programming");
    a.display();        // Sachin
    b.display();        // LearnYard Programming
    return 0;
}
```

`a` takes 7 bytes and `b` takes 22 bytes. Each object gets **exactly** what it needs, which a fixed `char name[20]` can never do.

> `strlen` does not count the null character, so we add `1`, otherwise the `'\0'` would have no room.
> 

## 5. The Three Companions

A dynamic constructor is never alone. The moment a class uses `new` inside a constructor, two more things are needed:

```
┌───────────────────────────────────────────────────────────────────────┐
│  **CONSTRUCTOR**      → takes memory with new                         │
│  **DESTRUCTOR**       → gives it back with delete / delete[]          │
│  **COPY CONSTRUCTOR** → makes a **DEEP** copy (new memory + copy)     │
└───────────────────────────────────────────────────────────────────────┘
```

This is the same warning you saw in **Unit 2 (shallow copy)** and **Unit 3 (deep copy)**. Look at what goes wrong with `MyString` if we skip the copy constructor:

```cpp
MyString a("Sachin");
MyString b = a;             // default copy = SHALLOW: copies the pointer only
```

```
   a.str ──────┐
               ├──▶ [ S a c h i n \0 ]    ONE block, two owners
   b.str ──────┘

   End of main: ~b runs → delete[] str   (block freed)
                ~a runs → delete[] str   (SAME block freed again!) ❌ double deletion
```

The fix is the deep copy constructor from Unit 3:

```cpp
MyString(const MyString& other) {
    str = new char[strlen(other.str) + 1];     // NEW memory for the copy
    strcpy(str, other.str);                     // copy the characters
}
```

> **Rule of Three:** if a class needs **one** of (destructor, copy constructor, copy assignment operator) written by you, it almost certainly needs **all three**. A dynamic constructor is the usual trigger. The copy assignment operator (`b = a;`) follows the same deep-copy idea, and you'll meet it when you study operator overloading of `=`.
> 

## 6. Dynamic Objects vs Dynamic Constructors

Students often mix up two different things:

|  | Dynamic **constructor** | Dynamic **object** |
| --- | --- | --- |
| What uses `new`? | A **member** inside the constructor | The **whole object** |
| Code | `data = new int[n];` (inside constructor) | `Marks* p = new Marks(3);` |
| Who frees it? | The class's **destructor** | You, with `delete p;` |

You can have both at once. `new Marks(3)` creates the object on the heap, and its constructor also allocates `data` on the heap. Then `delete p;` runs `~Marks()` (freeing `data`) and then frees the object itself.

---

# 🎯 Part 6: Self-Referential Class

## 7. The Idea

> A **self-referential class** is a class that contains a **pointer to an object of its own type** as a data member.
> 

```cpp
class Node {
public:
    int data;
    Node* next;          // pointer to another Node
};
```

```
   ┌───────────────┐
   │ Node          │
   │  data : 10    │
   │  next ────────┼──▶ points to another Node (the same class!)
   └───────────────┘
```

### Why a Pointer and Not an Object?

```cpp
class Bad {
    int data;
    Bad inner;           // ❌ ERROR: a Bad that contains a Bad that contains a Bad...
};
```

An object that contains a full copy of itself would need **infinite memory**, so the compiler rejects it. A **pointer** is always the same small size (just an address), so `Node* next;` is perfectly fine.

```
   Bad inside Bad inside Bad ...   → size never ends ❌
   Node* next                      → size = one address   ✅
```

Remember this from the **Pointers lecture (Unit 2)**: the last node's `next` is set to `nullptr`, meaning "nothing comes after me."

## 8. Building a Linked List

Self-referential classes are the building block of **linked lists, trees and graphs** in DSA. The instructor said in the Pointers lecture that you cannot build these structures without pointers. Here is the smallest working example, using dynamic memory from Lecture 1:

```cpp
#include <iostream>
using namespace std;

class Node {
public:
    int data;
    Node* next;

    Node(int value) {
        data = value;
        next = nullptr;          // new node points to nothing
    }
};

int main() {
    // create three nodes on the heap
    Node* head = new Node(10);
    head->next = new Node(20);
    head->next->next = new Node(30);

    // walk through the list
    Node* current = head;
    while (current != nullptr) {
        cout << current->data << " ";
        current = current->next;
    }
    cout << endl;

    // free every node
    current = head;
    while (current != nullptr) {
        Node* temp = current->next;      // save the next address FIRST
        delete current;
        current = temp;
    }
    return 0;
}
```

**Output:**

```
10 20 30
```

### The Picture

```
   head
    │
    ▼
 ┌────┬───┐     ┌────┬───┐     ┌────┬─────────┐
 │ 10 │ ──┼───▶ │ 20 │ ──┼───▶ │ 30 │ nullptr │
 └────┴───┘     └────┴───┘     └────┴─────────┘
  Node 1          Node 2          Node 3
  (heap, no name — reachable only through head and next)
```

### Tracing the Traversal

```
current = head                  → Node 1
current != nullptr ?  yes → print 10 → current = current->next   (Node 2)
current != nullptr ?  yes → print 20 → current = current->next   (Node 3)
current != nullptr ?  yes → print 30 → current = current->next   (nullptr)
current != nullptr ?  NO  → stop
```

This is the `while (node != NULL)` condition the instructor mentioned in the **Logical Operators lecture (Unit 0)** when he said the NOT operator becomes second nature in DSA.

### Why Save `temp` Before `delete`?

```
delete current;
current = current->next;     ❌ current was just freed; reading it is undefined behavior

Node* temp = current->next;  ✅ save the address first
delete current;
current = temp;
```

## 9. Where Else You'll See It

```
   Linked list:   node → node → node → nullptr       (one next pointer)
   Binary tree:   node has left* and right*          (two pointers)
   Graph:         node holds a list of neighbors
```

All of them are self-referential classes (or structs). A `struct` can be self-referential in exactly the same way:

```cpp
struct Node {
    int data;
    Node* next;
};
```

# 🎯 Part 7: Memory Leak and Allocation Failures

---

## 10. Memory Leak (Deeper Look)

In **Lecture 1** you defined a memory leak: memory taken with `new` and never freed. Here are the common ways it actually happens, since the exam and real code both ask for them.

### Cause 1: Forgetting `delete`

```cpp
void f() {
    int* p = new int(10);
    cout << *p;
}                              // p (stack) disappears, the heap int stays → LEAK
```

### Cause 2: Losing the Only Pointer

```cpp
int* p = new int(10);
p = new int(20);               // the first block now has NO pointer → LEAK
```

```
BEFORE p = new int(20);          AFTER
  p ──▶ [10]                      p ──▶ [20]
                                        [10]   ← still allocated, unreachable
```

Remember: heap memory has **no name**. If you lose the pointer, you lose the only way to free it.

### Cause 3: Early `return` Before `delete`

```cpp
void f(bool bad) {
    int* p = new int[100];
    if (bad) {
        return;                // leaves without delete[] → LEAK
    }
    delete[] p;
}
```

### Cause 4: Missing or Non-Virtual Destructor

```
   class with new inside but no destructor            → LEAK   (Part 5)
   delete basePtr with a non-virtual base destructor  → LEAK   (Part 1)
```

### Cause 5: Mismatched `new[]` / `delete`

```cpp
int* arr = new int[5];
delete arr;                    // ❌ must be delete[] arr  → undefined behavior
```

### Summary Table

| Cause | Fix |
| --- | --- |
| Forgot `delete` | Pair every `new` with a `delete` |
| Overwrote the only pointer | Free first, then reassign |
| Early return | Free before every `return` (or use smart pointers, below) |
| No destructor in a class that uses `new` | Write the destructor |
| Non-virtual base destructor | Make it `virtual` |
| `delete` instead of `delete[]` | Match `new[]` with `delete[]` |

### How Do You Find Leaks?

You do not have to hunt by eye. Tools can report them:

| Tool | Use |
| --- | --- |
| **Valgrind** (`valgrind --leak-check=full ./a.out`) | Linux |
| **AddressSanitizer** (compile with `-fsanitize=address`) | GCC / Clang |

### Smart Pointers (Preview)

C++ also offers **smart pointers** (`unique_ptr`, `shared_ptr`) that call `delete` for you automatically when they go out of scope. They remove most of the causes above. They are outside the Unit V syllabus, so we only mention them here.

## 11. Allocation Failures

## 12. What If the Heap Is Full?

`new` asks the operating system for memory. The system can say **no**, for example when you ask for far more than exists.

```
   new int[huge]    →    heap cannot supply that much   →   FAILURE
```

C++ gives two ways to handle this.

### Way 1: The Default, `new` Throws `bad_alloc`

By default, a failed `new` **throws an exception** of type `std::bad_alloc` (from the `<new>` header). If nobody catches it, the program **terminates**.

You will study exceptions in full in **Unit VI**. For now, just read this `try` / `catch` as: *"try this risky code; if `bad_alloc` happens, run the catch block instead of crashing."*

```cpp
#include <iostream>
#include <new>                 // for bad_alloc
using namespace std;

int main() {
    try {
        int* p = new int[1000000000000];       // far too big
        cout << "Memory allocated" << endl;
        delete[] p;
    }
    catch (const bad_alloc& e) {
        cout << "Memory allocation failed" << endl;
    }
    return 0;
}
```

**Output:**

```
Memory allocation failed
```

```
new int[1000000000000]
   ↓
Heap cannot give that much
   ↓
new THROWS bad_alloc
   ↓
Control jumps to the catch block, so the program does not crash
```

> On some systems the number is so huge that the compiler throws a closely related error (`bad_array_new_length`), which is a kind of `bad_alloc`, so the same `catch` works.
> 

### Way 2: `nothrow`, `new` Returns `nullptr`

If you prefer a plain `if` check instead of exceptions, use **`nothrow`**:

```cpp
#include <iostream>
#include <new>
using namespace std;

int main() {
    int* p = new (nothrow) int[1000000000000];

    if (p == nullptr) {                          // check the result
        cout << "Memory allocation failed" << endl;
        return 1;
    }

    cout << "Memory allocated" << endl;
    delete[] p;
    return 0;
}
```

**Output:**

```
Memory allocation failed
```

This is where the **null pointer** from the Pointers lecture helps: `nullptr` means "points to nothing," so checking `p == nullptr` tells you the request failed.

### Comparing the Two Ways

|  | Normal `new` | `new (nothrow)` |
| --- | --- | --- |
| On failure | Throws `bad_alloc` | Returns `nullptr` |
| How you handle it | `try` / `catch` | `if (p == nullptr)` |
| If you forget to handle it | Program terminates | Crash when you use the null pointer |
| Header needed | `<new>` | `<new>` |

### The C Way: `malloc`

C's `malloc()` never throws. On failure it returns `NULL`, so you must always check:

```cpp
int* p = (int*)malloc(100 * sizeof(int));
if (p == NULL) { /* failed */ }
```

(We prefer `new`/`delete` in C++. Never mix them: memory from `malloc` is freed with `free`, memory from `new` with `delete`.)

## 13. Allocation Failure Inside a Constructor

What if a **dynamic constructor** fails halfway?

```cpp
class Marks {
    int* data;
public:
    Marks(int n) {
        data = new int[n];        // throws bad_alloc if it fails
    }
    ~Marks() { delete[] data; }
};
```

```
Marks m(1000000000000);
   ↓
new throws bad_alloc inside the constructor
   ↓
The constructor did NOT finish → the object is NOT considered created
   ↓
~Marks() is NOT called   (so there is nothing to free, which is correct here,
                          because the allocation never succeeded)
   ↓
The exception travels out to a catch block
```

If the constructor had made **two** allocations and the **second** failed, the first would leak, because the destructor will not run. Managing that safely needs smart pointers or exception handling techniques. That belongs to **Unit VI**. The takeaway for now: *a failing `new` inside a constructor leaves the object uncreated, so allocate carefully and clean up what you already took.*

## 14. A Practical Habit

```cpp
int* p = new int[n];
// ... use p ...
delete[] p;
p = nullptr;          // from Lecture 1: avoids a dangling pointer or double deletion
```

---

## Common Mistakes to Avoid (Part 2)

| Mistake | What goes wrong |
| --- | --- |
| Dynamic constructor with no destructor | Memory leak every time an object dies. |
| Dynamic constructor with default (shallow) copying | Two objects share one block, then double deletion. |
| Forgetting `+1` for the `'\0'` in a char buffer | Writes past the end of the block. |
| Giving a class an object of its own type as a member | Infinite size, compile error. Use a pointer. |
| `delete current; current = current->next;` | Reads freed memory. Save `next` first. |
| Forgetting `nullptr` in the last node | Traversal runs off the end. |
| Overwriting the only pointer to heap memory | Leak. |
| Using `delete` for `new[]` | Undefined behavior. Use `delete[]`. |
| Ignoring a possible `new` failure on large requests | Crash, or use of a null pointer. |
| Mixing `malloc`/`free` with `new`/`delete` | Undefined behavior. |

---

## Key Points to Remember (Part 2)

- A **dynamic constructor** is a constructor that allocates memory with **`new`** at run time. Each object takes exactly the memory it needs.
- It must be accompanied by a **destructor** (`delete` / `delete[]`) and a **deep-copy constructor**. Together with the copy assignment operator, this is the **Rule of Three**.
- A **self-referential class** holds a **pointer to its own type** (`Node* next;`). It uses a **pointer** and never a full object, because an object inside itself would be infinitely large.
- Self-referential classes are the base of **linked lists, trees and graphs**. The last link is **`nullptr`**.
- A **memory leak** happens when heap memory becomes unreachable or is never freed: forgotten `delete`, overwritten pointer, early return, missing destructor, non-virtual base destructor.
- Find leaks with **Valgrind** or **AddressSanitizer**. Smart pointers prevent most of them.
- A failed `new` **throws `std::bad_alloc`**, which you can catch. **`new (nothrow)`** returns **`nullptr`** instead, which you check with `if`.
- If `new` fails inside a **constructor**, the object is not created and its destructor does not run.

---

# 📋 Recap: Lecture 3 & Lecture 4

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                                 FULL MAP                                        │
├─────────────────────────────────────────────────────────────────────────────────┤
│  **BINDING**                                                                    │
│    Early (compile time): normal calls, overloading                              │
│    Late  (run time):     virtual call via base pointer/reference                │
│    Late binding uses vtable + hidden vptr in each object                        │
│                                                                                 │
│  **VIRTUAL DESTRUCTOR**                                                         │
│    Base with virtual functions → virtual ~Base()                                │
│    else delete basePtr skips ~Derived() → leak / undefined behavior             │
│                                                                                 │
│  **CONCRETE** vs **ABSTRACT**                                                   │
│    Abstract: has a pure virtual function, no objects                            │
│    Concrete: all functions have bodies, objects allowed                         │
│                                                                                 │
│  **DYNAMIC CONSTRUCTOR**                                                        │
│    new inside constructor → needs destructor + deep copy (Rule of Three)        │
│                                                                                 │
│  **SELF-REFERENTIAL CLASS**                                                     │
│    class Node { int data; Node* next; };  → linked list, ends in nullptr        │
│                                                                                 │
│  **LEAKS & FAILURES**                                                           │
│    Leak causes: no delete, lost pointer, early return, no/virtual-less          │
│                 destructor, delete vs delete[]                                  │
│    new fails → throws bad_alloc  |  new (nothrow) → returns nullptr             │
└─────────────────────────────────────────────────────────────────────────────────┘
```