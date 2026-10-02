# Lecture 1: Introduction to Stack and Queue + Array Implementations

[L1. Introduction to Stack and Queue | Implementation using Data Structures](https://www.youtube.com/watch?v=tqQ5fTamIN4&list=PLgUwDviBIf0pOd5zvVVSzgpo6BaCpHT9c)

## Quick Recap Before We Begin

We just finished Unit II's Linked List topic — and this new topic leans directly on it. Today we start a brand-new unit: **Stack and Queue**. Both are simpler than linked lists conceptually (no traversal puzzles, no pointer gymnastics for most operations) — but they introduce a new idea: **restricted access**. Unlike an array or a linked list, where you can touch *any* element you want, a stack and a queue only let you touch **one specific end** at a time. That single restriction is what defines both structures, and everything else follows from it.

This class covers the two structures conceptually, and builds both from scratch using **arrays**. Class 2 will rebuild both using **linked lists**, and then tackle the "trick" problems — simulating one structure using the other.

---

## 1. What is a Stack?

A **stack** is a data structure that holds a collection of elements of one type (int, string, custom objects, etc.) and follows the **LIFO** rule:

> **LIFO = Last In, First Out** — whichever element was added *most recently* is the one that comes out *first*.
> 

**Real-life analogy:** Think of a **stack of plates** in a cafeteria. You can only add a plate to the **top** of the stack, and you can only remove a plate from the **top** as well. The plate at the very bottom was placed first, but it's the *last* one anyone will ever take off — it's buried under everything added after it.

```
        ┌───┐
        │ 1 │  ← top (last one pushed — this is what
        ├───┤     push/pop/top all interact with)
        │ 4 │
        ├───┤
        │ 3 │
        ├───┤
        │ 2 │  ← bottom (first one pushed — buried, untouchable
        └───┘     until everything above it is gone)
```

### Stack's Four Core Operations

| Operation | What it does | Real-life analogy |
| --- | --- | --- |
| `push(x)` | adds `x` to the top | placing a new plate on top |
| `pop()` | removes the topmost element | lifting the top plate off |
| `top()` | returns the topmost element **without removing it** | peeking at the top plate |
| `size()` | returns how many elements are currently stored | counting the plates |

⚠️ **`top()` never deletes.** It only *looks*. A very common beginner confusion is expecting `top()` to also remove the element — it doesn't. `pop()` is the only operation that removes something.

**Walking through an example**, one operation at a time — `push(2), push(3), push(4), push(1), pop(), top(), top(), pop(), pop(), size()`:

```
push(2):
┌───┐
│ 2 │ ← top
└───┘

push(3):
┌───┐
│ 3 │ ← top
├───┤
│ 2 │
└───┘

push(4):
┌───┐
│ 4 │ ← top
├───┤
│ 3 │
├───┤
│ 2 │
└───┘

push(1):
┌───┐
│ 1 │ ← top
├───┤
│ 4 │
├───┤
│ 3 │
├───┤
│ 2 │
└───┘

pop():  removes 1 →
┌───┐
│ 4 │ ← top
├───┤
│ 3 │
├───┤
│ 2 │
└───┘

top():  returns 4, stack unchanged
top():  returns 4 again, stack still unchanged

pop():  removes 4 →
┌───┐
│ 3 │ ← top
├───┤
│ 2 │
└───┘

pop():  removes 3 →
┌───┐
│ 2 │ ← top
└───┘

size(): returns 1
```

---

## 2. What is a Queue?

A **queue** is a data structure that also holds a collection of elements, but follows the opposite rule — **FIFO**:

> **FIFO = First In, First Out** — whichever element was added *first* is the one that comes out *first*.
> 

**Real-life analogy:** Think of people standing in a **line at a ticket counter**. New people join at the **back** of the line, but the person served next is always the one standing at the **front** — the one who arrived earliest, not the one who just joined.

```
          front         back
            │            │
            ▼            ▼
          ┌───┬───┬───┬───┐
          │ 2 │ 1  │ 3 │ 4 │
          └───┴───┴───┴───┘
            ↑             ↑
      served next     most recently joined
 (pop/top act here)    (push acts here)
```

### Queue's Four Core Operations

| Operation | What it does | Real-life analogy |
| --- | --- | --- |
| `push(x)` | adds `x` to the back of the line | a new person joins the back |
| `pop()` | removes the element at the front | the front person is served and leaves |
| `top()` (often called `front()`) | returns the front element **without removing it** | seeing who's at the front, without serving them |
| `size()` | returns how many elements are currently stored | counting people in the line |

⚠️ Note the terminology overlap: both stack and queue expose `push`, `pop`, `top`, `size` — but **which end each one actually touches is completely different**. Stack: both ends of activity happen at the *same* end (top). Queue: push happens at one end (back), pop/top happen at the *other* end (front).

**Walking through an example** — `push(2), push(1), push(3), push(4), pop(), top(), pop(), top(), push(7), top(), size()`:

```
push(2):           front→[2]←back

push(1):           front→[2, 1]←back

push(3):           front→[2, 1, 3]←back

push(4):           front→[2, 1, 3, 4]←back

pop():   removes 2 (entered first)
                    front→[1, 3, 4]←back

top():   returns 1, nothing removed

pop():   removes 1
                    front→[3, 4]←back

top():   returns 3

push(7):            front→[3, 4, 7]←back

top():   returns 3   (still the earliest-entered survivor)

size():  returns 3
```

There's no mention of a **deque (double-ended queue)** yet — that's a separate, more flexible structure (insertion/removal at *both* ends) that we'll cover later in this unit, per the syllabus.

---

## 3. You Won't Implement These Yourself in Practice — But You Should Know How They Work

In real problem-solving, you almost never hand-roll a stack or queue — every language ships one:

| Language | Stack | Queue |
| --- | --- | --- |
| C++ | `stack<int> st;` (STL) | `queue<int> q;` (STL) |
| Java |  `Stack` (Collections) | `LinkedList` (Collections) |

You'd simply call `st.push(6)`, `st.pop()`, `st.top()`, `st.size()` and the internals are handled for you.

⚠️ **But interviewers love asking:** *"Fine, you know how to use the library — now show me how it works underneath."* That's exactly what the rest of this lecture (and Class 2) cover: building both structures from scratch, first using **arrays** (this class), then using **linked lists** (next class).

---

## 4. Stack Using Array

**The constraint:** an array needs a fixed, known size decided upfront — so this implementation is **not dynamic**. You must know the maximum capacity in advance.

**The state we track:**

- `arr[SIZE]` — the underlying fixed array
- `top` — an integer index, **initialized to `1`**, meaning "empty"

**The core idea:** `top` isn't just a variable — it *is* the entire stack. It tells you both *where* the last element sits and *how many* elements exist. Popping doesn't erase the value in the array; it just moves `top` backward, and the old value becomes irrelevant — it's still physically sitting in memory, but nothing will ever read it again until it's overwritten.

```cpp
class StackImpl {
public:
    int arr[10];
    int top;

    StackImpl() {
        top = -1;
    }

    void push(int x) {
        // interviewer follow-up: what if top reaches SIZE - 1? add an overflow check here
        top = top + 1;
        arr[top] = x;
    }

    int pop() {
        int val = arr[top];
        top = top - 1;
        return val;
    }

    int peekTop() {
        return arr[top];   // does NOT touch `top`
    }

    int size() {
        return top + 1;
    }
};
```

**Walkthrough — `push(4), pop(), push(5), push(6), push(10)`:**

```
Start:  top = -1   (empty)
        arr: [_, _, _, _, _, _, _, _, _, _]

push(4):  top → 0,  arr[0] = 4
        arr: [4, _, _, _, ...]
                ↑
              top=0

pop():   val = arr[0] = 4,  top → -1
        arr: [4, _, _, _, ...]     ← 4 is still physically there,
                                       but top says "nothing exists"
              top=-1

push(5):  top → 0,  arr[0] = 5
        arr: [5, _, _, _, ...]
                ↑
              top=0

push(6):  top → 1,  arr[1] = 6
        arr: [5, 6, _, _, ...]
                   ↑
                 top=1

push(10): top → 2,  arr[2] = 10
        arr: [5, 6, 10, _, ...]
                       ↑
                     top=2

size()    = top + 1 = 3
peekTop() = arr[2]  = 10
```

**Time complexity:** every single operation — `push`, `pop`, `peekTop`, `size` — is **O(1)**, since there's no traversal, just direct index math.

**Space complexity:** **O(SIZE)** regardless of how many elements you actually use — if you declared a size-10 array but only ever store 3 elements, the other 7 slots sit reserved and wasted. This fixed-capacity waste is the core trade-off of an array-backed stack.

---

## 5. Queue Using Array — Why It's Trickier Than Stack

A naive array-backed queue (just tracking `end`, no `start`) would work fine for pushing, but every `pop` leaves the freed front slot **permanently unusable** — it just becomes dead space that's never reused, and eventually the whole array fills up with "holes" even though the queue logically has room. The fix: treat the array as **circular** — once `end` (or `start`) reaches the last index, wrap back around to index `0` using the modulo operator.

**The state we track:**

- `arr[SIZE]`
- `start` and `end` — both initialized to `1`
- `currentSize` — tracks how many elements currently exist

```cpp
class QueueImpl {
public:
    int arr[4];        // fixed capacity, e.g. 4
    int start, end, currentSize;

    QueueImpl() {
        start = -1;
        end = -1;
        currentSize = 0;
    }

    void push(int x) {
        if (currentSize == 4) {
            // overflow — queue is full
            return;
        }
        if (start == -1) {          // first element ever pushed
            start = 0;
            end = 0;
        } else {
            end = (end + 1) % 4;    // wrap around if needed
        }
        arr[end] = x;
        currentSize++;
    }

    int pop() {
        if (currentSize == 0) {
            // underflow — nothing to pop
            return -1;
        }
        int val = arr[start];
        if (currentSize == 1) {     // last element being removed
            start = -1;
            end = -1;
        } else {
            start = (start + 1) % 4;
        }
        currentSize--;
        return val;
    }

    int peekFront() {
        return arr[start];
    }

    int size() {
        return currentSize;
    }
};
```

**Walkthrough — capacity 4, `push(3), push(2), push(4)`:**

```
Start:  start=-1, end=-1, currentSize=0
        arr: [_, _, _, _]

push(3): start==-1 → start=0, end=0
        arr[0]=3,  currentSize=1
        arr: [3, _, _, _]
               ▲
          start,end

push(2): end = (0+1)%4 = 1
        arr[1]=2,  currentSize=2
        arr: [3, 2, _, _]
               ▲  ▲
            start end

push(4): end = (1+1)%4 = 2
        arr[2]=4,  currentSize=3
        arr: [3, 2, 4, _]
               ▲     ▲
            start   end
```

**Now `pop()` twice:**

```
pop(): val=arr[0]=3, currentSize=3≠1 → start=(0+1)%4=1, currentSize=2
        arr: [3, 2, 4, _]
                  ▲  ▲
               start end

pop(): val=arr[1]=2, currentSize=2≠1 → start=(1+1)%4=2, currentSize=1
        arr: [3, 2, 4, _]
                     ▲
                start,end  (only "4" remains)
```

**Now `push(2), push(3)` — this is the wraparound case:**

```
push(2): end=(2+1)%4=3
        arr[3]=2, currentSize=2
        arr: [3, 2, 4, 2]
                     ▲     ▲
                  start   end

push(3): end=(3+1)%4=0  ← wraps back to index 0!
        arr[0]=3 (overwrites the old, already-popped '3'), currentSize=3
        arr: [3, 2, 4, 2]
               ▲  ↑     ▲
              end │   start
                   └── index 0 reused after wrapping around
```

This is the entire trick of a circular array queue: **`(index + 1) % SIZE`** lets `end` (and `start`) loop back to `0` instead of running off the array and leaving the freed-up front slots permanently unusable.

⚠️ **Destroying the queue on the last pop:** when `currentSize` drops to exactly `0` after a pop, both `start` and `end` are reset to `-1` — this re-establishes the "empty" state cleanly, so the *next* `push` correctly re-detects "this is the first element" and re-initializes both pointers together (instead of, say, `end` silently wrapping forward from a stale index while the queue is actually empty).

**Time complexity:** `push`, `pop`, `peekFront`, `size` — all **O(1)**.

**Space complexity:** **O(SIZE)** — same fixed-capacity trade-off as the array-based stack.

---

## 6. Class 1 Recap

| Structure | Rule | push acts on | pop/top act on |
| --- | --- | --- | --- |
| Stack | LIFO | top | top |
| Queue | FIFO | back (`end`) | front (`start`) |

| Array Implementation | push | pop | top | size | Space |
| --- | --- | --- | --- | --- | --- |
| Stack | O(1) | O(1) | O(1) | O(1) | O(fixed capacity) |
| Queue (circular) | O(1) | O(1) | O(1) | O(1) | O(fixed capacity) |

**Important teaching point:** every array-based operation here is O(1) — but that speed is paid for with a **fixed, wasted capacity** decided in advance. If you declare room for 10 but only ever need 3, those other 7 slots sit reserved and unused for the entire lifetime of the structure. That's the exact motivation for Class 2, where we rebuild both structures on top of a **linked list** instead — trading that fixed waste for exactly the memory actually needed, at no extra time cost.

---