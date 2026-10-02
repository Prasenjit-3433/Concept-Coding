# Lecture 6: Deque

# 🎯Part 1: Introduction & **array-based** implementation

---

## 1. What Is a Deque?

A **deque** is a linear structure that allows **insertion and deletion at both the front and the back** — nothing is restricted to a single end, unlike a stack (one end only) or a plain queue (insert at one end, delete at the other, but never insert at the delete-end or vice versa).

**Real-life analogy:** Recall Class 1's **ticket-counter line** analogy for a plain queue — people could only join at the back and leave from the front. A deque is like that same line, except now people are also allowed to **join at the front** (cutting in, by the line's own rules) and **leave from the back** (someone at the back can decide to step out) — both ends are fully active, both for joining and leaving.

```
**Plain Queue (Class 1)**:                  **Deque**:
  insert ONLY here                        insert OR delete here
                │                                       │
                ▼                                       ▼
 ┌───┬───┬───┬───┐                       ┌───┬───┬───┬───┐
 │   │   │   │   │                       │   │   │   │   │
 └───┴───┴───┴───┘                       └───┴───┴───┴───┘
   ▲                                       ▲
   │                                       │
  delete ONLY here                        insert OR delete here
```

### Deque's Core Operations

A deque generalizes the plain queue's four operations into **seven**:

| Operation | What it does |
| --- | --- |
| `pushFront(x)` | insert `x` at the front |
| `pushBack(x)` | insert `x` at the back |
| `popFront()` | remove the element at the front |
| `popBack()` | remove the element at the back |
| `peekFront()` | return the front element, without removing it |
| `peekBack()` | return the back element, without removing it |
| `size()` | return the current number of elements |

⚠️ **A deque is a strict generalization, not a different structure:** a plain queue is "a deque where you only ever call `pushBack` and `popFront`." A stack is "a deque where you only ever call `pushFront` and `popFront`" (or, equally validly, only `pushBack`/`popBack` — both ends behave identically for a stack-like usage). Every structure from Classes 1–2 is really just a *restricted usage pattern* of a deque — the underlying data structure doesn't change, only which operations you choose to call.

---

## 2. Deque Using Array — Extending the Circular Queue

Class 1's circular array queue (Lecture 1, Section 5) already solved the hardest part of this: wrapping `start`/`end` around using modulo arithmetic so that freed-up slots at either end get reused instead of wasted. A deque-via-array reuses **exactly that same circular buffer trick** — the only new work is handling insertion/deletion at **both** ends instead of just one each.

**The state we track:** identical to the circular queue —

- `arr[SIZE]`
- `start`, `end` — both initialized to `1`
- `currentSize` — tracks how many elements currently exist

**The one new idea — moving `start` backward:** `pushBack` and `popFront` work exactly like the circular queue already did. But `pushFront` and `popBack` need to move `start` or `end` **backward**, which means wrapping in the *opposite* direction when you hit index `0`. The formula for "one step backward, with wraparound" is:

```
**(index - 1 + SIZE) % SIZE**
```

⚠️ **Why the `+ SIZE` is necessary:** in C++, the `%` operator on a negative number doesn't behave like mathematical modulo — `-1 % 4` evaluates to `-1`, not `3`. Adding `SIZE` before taking `% SIZE` guarantees the value being modulo'd is never negative, so the wraparound lands on a valid index (`3`, in this example) instead of an invalid negative one.

```cpp
class DequeArray {
public:
    int arr[5];     // fixed capacity, e.g. 5
    int start, end, currentSize;
    int capacity;

    DequeArray() {
        start = -1;
        end = -1;
        currentSize = 0;
        capacity = 5;
    }

    void pushBack(int x) {
        if (currentSize == capacity) return;   // overflow
        if (start == -1) {                      // first element ever
            start = 0;
            end = 0;
        } else {
            end = (end + 1) % capacity;
        }
        arr[end] = x;
        currentSize++;
    }

    void pushFront(int x) {
        if (currentSize == capacity) return;   // overflow
        if (start == -1) {                      // first element ever
            start = 0;
            end = 0;
        } else {
            start = (start - 1 + capacity) % capacity;   // step backward, with wraparound
        }
        arr[start] = x;
        currentSize++;
    }

    int popFront() {
        if (currentSize == 0) return -1;   // underflow
        int val = arr[start];
        if (currentSize == 1) {
            start = -1;
            end = -1;
        } else {
            start = (start + 1) % capacity;
        }
        currentSize--;
        return val;
    }

    int popBack() {
        if (currentSize == 0) return -1;   // underflow
        int val = arr[end];
        if (currentSize == 1) {
            start = -1;
            end = -1;
        } else {
            end = (end - 1 + capacity) % capacity;   // step backward, with wraparound
        }
        currentSize--;
        return val;
    }

    int peekFront() {
        return arr[start];
    }

    int peekBack() {
        return arr[end];
    }

    int size() {
        return currentSize;
    }
};
```

---

## 3. Time & Space Complexity — Array Implementation

| Operation | Time Complexity | Why |
| --- | --- | --- |
| `pushFront` | O(1) | direct index write + backward-wrap arithmetic |
| `pushBack` | O(1) | direct index write + forward-wrap arithmetic |
| `popFront` | O(1) | direct index read + pointer move |
| `popBack` | O(1) | direct index read + pointer move |
| `peekFront` / `peekBack` | O(1) | direct index read, no pointer movement |
| `size` | O(1) | just returns a stored counter |

**Every single operation is O(1)** — there's no traversal anywhere, exactly like the circular queue from Class 1. This makes sense: a deque-via-array is literally the same circular buffer, just with both "ends" now fully reachable in O(1) via direct indexing in either direction.

**Space complexity:** **O(capacity)** — same fixed, pre-reserved trade-off as every array-based structure in this unit (Class 1's stack/queue). Whatever fixed size you declare, that's what you pay for, regardless of how many elements you actually store at any given moment.

# Part 2: Deque Using Doubly Linked List

---

## 4. Deque Using Doubly Linked List — Why DLL, Not Singly Linked

A plain singly linked list (Unit II, Classes 1–2) only lets you move **forward** and only gives O(1) access at the **head**. A deque needs O(1) access and O(1) insertion/deletion at **both** ends — and Unit II's Class 3 told us exactly which structure guarantees that: a **doubly linked list**, where every node holds both a `prev` and a `next` pointer.

This directly reuses the `QueueLL` pattern from Class 2 of this unit (Lecture 1) — which already needed a `start` and `end` pointer for its singly-linked-list queue. The difference here is that **every node itself is doubly linked**, so that removing from *either* end is a genuine O(1) operation without needing to "look one node ahead" the way a singly linked list would require (exactly the limitation flagged back in Unit II, Class 3, Section 5 — deletion from the tail of a singly linked list needs the second-to-last node, which isn't directly reachable without a `prev` pointer).

**The state we track:**

- `Node* start` — points to the front of the deque
- `Node* end` — points to the back of the deque
- `dequeSize` — tracks the current number of elements

```cpp
struct Node {
    int data;
    Node* prev;
    Node* next;
};

class DequeLL {
public:
    Node* start;
    Node* end;
    int dequeSize;

    DequeLL() {
        start = nullptr;
        end = nullptr;
        dequeSize = 0;
    }

    void pushBack(int x) {
        Node* temp = new Node();
        temp->data = x;
        temp->next = nullptr;

        if (start == nullptr) {        // first element ever
            temp->prev = nullptr;
            start = temp;
            end = temp;
        } else {
            temp->prev = end;           // new node points back to old last node
            end->next = temp;            // old last node points forward to new node
            end = temp;                   // end moves to the new node
        }
        dequeSize++;
    }

    void pushFront(int x) {
        Node* temp = new Node();
        temp->data = x;
        temp->prev = nullptr;

        if (start == nullptr) {        // first element ever
            temp->next = nullptr;
            start = temp;
            end = temp;
        } else {
            temp->next = start;         // new node points forward to old first node
            start->prev = temp;          // old first node points back to new node
            start = temp;                 // start moves to the new node
        }
        dequeSize++;
    }

    int popFront() {
        if (start == nullptr) return -1;   // underflow

        int val = start->data;
        Node* temp = start;

        if (start == end) {                 // only one node in the deque
            start = nullptr;
            end = nullptr;
        } else {
            start = start->next;
            start->prev = nullptr;           // new front has nothing before it
        }
        delete temp;
        dequeSize--;
        return val;
    }

    int popBack() {
        if (end == nullptr) return -1;     // underflow

        int val = end->data;
        Node* temp = end;

        if (start == end) {                 // only one node in the deque
            start = nullptr;
            end = nullptr;
        } else {
            end = end->prev;
            end->next = nullptr;              // new back has nothing after it
        }
        delete temp;
        dequeSize--;
        return val;
    }

    int peekFront() {
        return start->data;
    }

    int peekBack() {
        return end->data;
    }

    int size() {
        return dequeSize;
    }
};
```

⚠️ **The `start == end` check matters in all four mutating operations.** When the deque holds exactly one node, both `start` and `end` point to that *same* node. Removing it from either end must reset **both** pointers to `nullptr` — forgetting this (e.g., only updating `start` on a `popFront` of the last node) would leave `end` dangling, pointing at freed memory, exactly the dangling-pointer bug flagged for the plain queue in Lecture 1, Section 7.

---

## 5. Time & Space Complexity — Linked List Implementation

| Operation | Time Complexity | Why |
| --- | --- | --- |
| `pushFront` | O(1) | direct access via `start`, no traversal |
| `pushBack` | O(1) | direct access via `end`, no traversal |
| `popFront` | O(1) | direct access via `start`, `prev` pointer available for relinking |
| `popBack` | O(1) | direct access via `end`, `prev` pointer available for relinking |
| `peekFront` / `peekBack` | O(1) | direct pointer dereference |
| `size` | O(1) | returns a stored counter |

**Every operation is O(1)** — identical time complexity to the array version. The payoff for using a DLL is purely in **space**: **O(n)**, where `n` is the actual number of elements stored, with no fixed, wasted capacity the way the array version requires.

---

## 6. Array vs Linked List Deque — Side-by-Side

| Feature | Array-based Deque | Linked-List-based Deque |
| --- | --- | --- |
| All 7 operations | O(1) | O(1) |
| Space | O(fixed capacity) — wasted if under-filled | O(n) — exactly what's needed |
| Needs capacity decided upfront? | Yes | No — grows/shrinks freely at runtime |
| Extra memory per element | None beyond the array itself | 2 pointers (`prev`, `next`) per node |
| Index arithmetic needed? | Yes — circular wraparound via `% capacity` | No — just pointer relinking |

**The teaching point, same as every other structure in this unit:** both implementations hit O(1) on every operation — the real trade-off, once again, is the classic **space-vs-flexibility** one that's run through this entire course since Unit II: fixed-but-wasted memory (array) vs. exactly-sized-but-pointer-heavier memory (linked list).

# Part 3: STL Usage & Hands-on Examples

---

## 7. Deque in C++ STL

C++'s STL gives you `std::deque`, which already implements everything built from scratch in Parts 1–2.

```cpp
#include <deque>
using namespace std;

deque<int> dq;
```

### Core Operations

| Operation | What it does | Maps to |
| --- | --- | --- |
| `dq.push_back(x)` | insert `x` at the back | `pushBack` (Parts 1–2) |
| `dq.push_front(x)` | insert `x` at the front | `pushFront` |
| `dq.pop_back()` | remove the back element | `popBack` |
| `dq.pop_front()` | remove the front element | `popFront` |
| `dq.back()` | return the back element, without removing | `peekBack` |
| `dq.front()` | return the front element, without removing | `peekFront` |
| `dq.size()` | return the number of elements | `size` |
| `dq.empty()` | returns `true` if the deque has no elements | — (new, but same idea as `priority_queue.empty()`, Lecture 5) |
| `dq[i]` | **random access** at index `i` | — genuinely new capability |

⚠️ **`dq[i]` is the one capability neither Part 1 nor Part 2's hand-built version exposed cleanly.** STL's `deque` is actually implemented internally as a sequence of fixed-size blocks (not a single contiguous array, and not a plain linked list either) — specifically so it can support O(1) random access *in addition to* O(1) push/pop at both ends. This hybrid internal structure is genuinely more advanced than either implementation in Parts 1–2, and is why real-world code uses `std::deque` rather than hand-rolling one — you get indexed access for free, which neither of our from-scratch versions provide without added work.

**Time complexity, STL `deque`:**

| Operation | Time Complexity |
| --- | --- |
| `push_back` / `push_front` | O(1) amortized |
| `pop_back` / `pop_front` | O(1) |
| `front` / `back` | O(1) |
| `size` / `empty` | O(1) |
| `dq[i]` (random access) | O(1) |

**Space complexity:** O(n) — dynamic, grows/shrinks as needed, same as our Part 2 linked-list version.

---

## `Exercise 1` — Basic Operations

**Task:** Perform a sequence of mixed front/back operations and print the deque's contents after each step, to build comfort with the API before moving to patterns.

```cpp
#include <iostream>
#include <deque>
using namespace std;

void printDeque(const deque<int>& dq) {
    cout << "[ ";
    for (int x : dq) cout << x << " ";
    cout << "]" << endl;
}

int main() {
    deque<int> dq;

    dq.push_back(10);   printDeque(dq);   // [ 10 ]
    dq.push_front(20);  printDeque(dq);   // [ 20 10 ]
    dq.push_back(30);   printDeque(dq);   // [ 20 10 30 ]
    dq.push_front(40);  printDeque(dq);   // [ 40 20 10 30 ]

    cout << "front(): " << dq.front() << ", back(): " << dq.back() << endl;
    // front(): 40, back(): 30

    dq.pop_back();       printDeque(dq);   // [ 40 20 10 ]
    dq.pop_front();      printDeque(dq);   // [ 20 10 ]

    cout << "size(): " << dq.size() << endl;   // size(): 2

    return 0;
}
```

**What to check before running:** this is the *exact same* operation sequence used in Part 1's array walkthrough and Part 2's linked-list walkthrough. If you've hand-traced both of those already, you should be able to predict every line of output here before compiling — that's the real test of whether Parts 1–2 actually landed.

---

## `Exercise 2` — Check if a Sequence Is a Palindrome, Using a Deque

**Task:** Given a sequence of characters, use a deque's dual-ended access to check if it reads the same forwards and backwards — without using any extra array indexing.

```cpp
#include <iostream>
#include <deque>
#include <string>
using namespace std;

bool isPalindrome(string s) {
    deque<char> dq;
    for (char c : s) {
        dq.push_back(c);
    }

    while (dq.size() > 1) {
        char front = dq.front();
        char back = dq.back();

        if (front != back) {
            return false;
        }

        dq.pop_front();
        dq.pop_back();
    }

    return true;   // 0 or 1 remaining characters are trivially palindromic
}

int main() {
    cout << boolalpha;
    cout << "\"racecar\": " << isPalindrome("racecar") << endl;   // true
    cout << "\"hello\": "   << isPalindrome("hello") << endl;     // false
    cout << "\"level\": "   << isPalindrome("level") << endl;     // true

    return 0;
}
```

**Trace for `"racecar"`:**

```
dq = [r, a, c, e, c, a, r]

front='r', back='r' → match → pop both → dq = [a, c, e, c, a]
front='a', back='a' → match → pop both → dq = [c, e, c]
front='c', back='c' → match → pop both → dq = [e]
size == 1 → loop stops → return true
```

**What this exercise checks:** `peekFront`/`peekBack` and `popFront`/`popBack` working **in the same loop iteration**, closing from both ends simultaneously — a usage pattern that's only natural with a deque, and awkward with any single-ended structure from earlier in this unit.

**Time complexity:** **O(n)** — each character is visited exactly once across the whole run.

**Space complexity:** **O(n)** — the deque initially holds all `n` characters.

---

## Full Recap (Parts 1–3)

| Topic | Key Takeaway |
| --- | --- |
| What a Deque is | Generalizes stack & queue — insertion/deletion allowed at **both** front and back |
| Array implementation | Direct extension of Class 1's circular queue; new idea is backward wraparound: `(index - 1 + capacity) % capacity` |
| Linked list implementation | Needs a **doubly** linked list (Unit II, Class 3) — `prev` pointers are what make O(1) deletion from the back possible |
| `start == end` edge case | Must reset **both** pointers to `nullptr` when removing the last remaining node — same dangling-pointer risk flagged for the plain queue in Lecture 1 |
| Time complexity (both implementations) | All 7 core operations: **O(1)** |
| Space complexity | Array: O(fixed capacity), wasted if under-filled. Linked list: O(n), exact fit |
| STL `deque` | `push_back/front`, `pop_back/front`, `back()`, `front()`, `size()`, `empty()` — plus **O(1) random access** via `dq[i]`, which neither hand-built version provides |
| Sliding Window Maximum pattern | The signature deque interview pattern — evict expired indices from the front, evict dominated values from the back, in the same pass |
| Palindrome-check pattern | Simultaneous front/back access and removal — a usage shape unique to deques among this unit's structures |