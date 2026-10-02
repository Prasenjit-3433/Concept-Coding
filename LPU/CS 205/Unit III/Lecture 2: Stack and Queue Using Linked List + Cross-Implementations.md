# Lecture 2: Stack and Queue Using Linked List + Cross-Implementations

[L1. Introduction to Stack and Queue | Implementation using Data Structures](https://www.youtube.com/watch?v=tqQ5fTamIN4&list=PLgUwDviBIf0pOd5zvVVSzgpo6BaCpHT9c)

## Quick Recap Before We Begin

In Class 1, we built both a stack and a queue on top of a **fixed-size array**, and saw the trade-off clearly: O(1) operations, but wasted, pre-reserved memory. Today we rebuild both on top of a **linked list** — removing the fixed-size limitation entirely — and then tackle a different kind of question altogether: *what if you're only given one of these structures, and asked to make it behave like the other?*

---

## 7. Stack Using Linked List

This removes the fixed-size limitation entirely — the structure grows and shrinks exactly as needed. We reuse the singly linked list `Node` from Unit II:

```cpp
struct Node {
    int data;
    Node* next;
};

class StackLL {
public:
    Node* top;
    int stackSize;

    StackLL() {
        top = nullptr;
        stackSize = 0;
    }

    void push(int x) {
        Node* temp = new Node();
        temp->data = x;
        temp->next = top;    // new node points to old top
        top = temp;           // top now points to the new node
        stackSize++;
    }

    int pop() {
        int val = top->data;
        Node* temp = top;
        top = top->next;
        delete temp;
        stackSize--;
        return val;
    }

    int peekTop() {
        return top->data;
    }

    int size() {
        return stackSize;
    }
};
```

**Real-life analogy:** This is exactly `insertAtHead` and `deleteAtHead` from the linked list unit, wearing a new name. Every push is a head-insertion; every pop is a head-deletion. That's precisely *why* both are O(1) — a stack's "top" and a linked list's "head" are the same idea, just relabeled.

**Walkthrough — `push(4), push(2), push(3), push(1)`, then `pop()`:**

```
push(4):
top
 │
 ▼
┌───┬──────┐
│ 4 │ NULL │
└───┴──────┘

push(2):
top
 │
 ▼
┌───┬───┐     ┌───┬──────┐
│ 2 │ ●─┼────►│ 4 │ NULL │
└───┴───┘     └───┴──────┘

push(3):
top
 │
 ▼
┌───┬───┐     ┌───┬───┐     ┌───┬──────┐
│ 3 │ ●─┼────►│ 2 │ ●─┼────►│ 4 │ NULL │
└───┴───┘     └───┴───┘     └───┴──────┘

push(1):
top
 │
 ▼
┌───┬───┐     ┌───┬───┐     ┌───┬───┐     ┌───┬──────┐
│ 1 │ ●─┼────►│ 3 │ ●─┼────►│ 2 │ ●─┼────►│ 4 │ NULL │
└───┴───┘     └───┴───┘     └───┴───┘     └───┴──────┘

top() → returns 1

pop():
Step 1: temp = top          (temp points to node 1)
Step 2: top = top->next     (top now points to node 3)
Step 3: delete temp          (node 1 freed)

top
 │
 ▼
┌───┬───┐     ┌───┬───┐     ┌───┬──────┐
│ 3 │ ●─┼────►│ 2 │ ●─┼────►│ 4 │ NULL │
└───┴───┘     └───┴───┘     └───┴──────┘
```

**Time complexity:** all operations **O(1)** — no traversal anywhere.

**Space complexity:** **O(n)**, where `n` is the actual number of elements stored — no wasted, pre-reserved slots like the array version.

---

## 8. Queue Using Linked List

Just like the array version needed both `start` and `end`, the linked-list version needs both a `start` and an `end` pointer — because pushing happens at one end (`end`) while popping happens at the other (`start`).

```cpp
struct Node {
    int data;
    Node* next;
};

class QueueLL {
public:
    Node* start;
    Node* end;
    int queueSize;

    QueueLL() {
        start = nullptr;
        end = nullptr;
        queueSize = 0;
    }

    void push(int x) {
        Node* temp = new Node();
        temp->data = x;
        temp->next = nullptr;

        if (start == nullptr) {        // first element ever
            start = temp;
            end = temp;
        } else {
            end->next = temp;           // old last node points to new node
            end = temp;                  // end moves to the new node
        }
        queueSize++;
    }

    int pop() {
        int val = start->data;
        Node* temp = start;
        start = start->next;
        if (start == nullptr) {        // queue just became empty
            end = nullptr;
        }
        delete temp;
        queueSize--;
        return val;
    }

    int peekFront() {
        return start->data;
    }

    int size() {
        return queueSize;
    }
};
```

**Real-life analogy:** `push` behaves exactly like `insertAtTail` from the linked list unit — except this time we're **handed the `end` pointer directly**, as a separately-maintained variable (exactly the caveat flagged in Class 3's doubly-linked-list note), so there's **no traversal needed to find the last node**. That's what makes this push O(1), unlike the plain singly-linked-list tail insertion from Unit II. `pop` behaves exactly like `deleteAtHead`.

**Walkthrough — `push(7), push(2), push(3), push(5)`, then `pop()`:**

```
push(7):
start,end
    │
    ▼
┌───┬──────┐
│ 7 │ NULL  │
└───┴──────┘

push(2):
start                    end
  │                       │
  ▼                       ▼
┌───┬───┐             ┌───┬──────┐
│ 7 │ ●─┼───────────►│ 2  │ NULL │
└───┴───┘             └───┴──────┘

push(3):
start                                  end
  │                                     │
  ▼                                     ▼
┌───┬───┐        ┌───┬───┐         ┌───┬──────┐
│ 7 │ ●─┼───────►│ 2 │ ●─┼───────►│ 3  │ NULL │
└───┴───┘        └───┴───┘         └───┴──────┘

push(5):
start                                              end
  │                                                 │
  ▼                                                 ▼
┌───┬───┐    ┌───┬───┐    ┌───┬───┐    ┌───┬──────┐
│ 7 │ ●─┼───►│ 2 │ ●─┼───►│ 3 │ ●─┼───►│ 5 │ NULL │
└───┴───┘    └───┴───┘    └───┴───┘    └───┴──────┘

pop():
Step 1: temp = start           (temp points to node 7)
Step 2: start = start->next    (start now points to node 2)
Step 3: delete temp             (node 7 freed)

start                                  end
  │                                     │
  ▼                                     ▼
┌───┬───┐        ┌───┬───┐         ┌───┬──────┐
│ 2 │ ●─┼───────►│ 3 │ ●─┼───────►│ 5  │ NULL │
└───┴───┘        └───┴───┘         └───┴──────┘

top() → returns 2
```

⚠️ **The one edge case that trips people up:** when the *last remaining* node is popped, `start` becomes `nullptr` — and if you forget to also reset `end` to `nullptr` at that moment, `end` would be left as a **dangling pointer** to freed memory, silently corrupting the very next `push` (which checks `start == nullptr` to decide whether it's the first element, but would then try to write through a stale `end->next`).

**Time complexity:** all operations **O(1)**.

**Space complexity:** **O(n)** — dynamic, no wasted pre-allocated space.

---

## 9. Implementing a Stack Using a Queue

A different flavor of question: *given only a queue, make it behave like a stack.* The queue is FIFO by nature — pushing directly would put the newest element at the *back*, but a stack needs it accessible at the *front*. The fix: after every push, **rotate the queue** so the newest element ends up at the front.

**The logic for `push(x)`:**

1. Note the current size of the queue *before* pushing.
2. Push `x` normally (it lands at the back).
3. Pop and re-push every *older* element, one at a time — this cycles them from front to back, which pushes `x` all the way up to the front.

```cpp
class StackUsingQueue {
public:
    queue<int> q;

    void push(int x) {
        int sizeBefore = q.size();
        q.push(x);

        for (int i = 0; i < sizeBefore; i++) {
            q.push(q.front());
            q.pop();
        }
    }

    int pop() {
        int val = q.front();
        q.pop();
        return val;
    }

    int top() {
        return q.front();
    }

    int size() {
        return q.size();
    }
};
```

**Walkthrough — `push(4), push(9), push(2)`:**

```
push(4):  sizeBefore = 0, nothing to rotate
front→[4]←back

push(9):  sizeBefore = 1
  q.push(9):        front→[4, 9]←back
  rotate 1 old element:
    pop 4, push 4:  front→[9, 4]←back
                     ↑
              front now holds the most-recently-pushed 9 — correct!

push(2):  sizeBefore = 2
  q.push(2):        front→[9, 4, 2]←back
  rotate 2 old elements:
    pop 9, push 9:  front→[4, 2, 9]←back
    pop 4, push 4:  front→[2, 9, 4]←back
                     ↑
              front now holds the most-recently-pushed 2 — correct!
```

After every push, the **most recently pushed element sits at the front** — exactly a stack's `top`. `pop()` and `top()` need no extra work at all, since the queue is already kept in the right order.

**Time complexity:** `push` is **O(n)** (the rotation), while `pop`, `top`, `size` are all **O(1)**.

**Space complexity:** O(n) — a single queue, no extra structure.

---

## 10. Implementing a Queue Using Two Stacks

The reverse problem: *given only stacks, make them behave like a queue.* Since a single stack can only ever expose its most-recent element, one stack alone cannot give FIFO behavior — we need **two** stacks working together, `s1` and `s2`.

There are two possible strategies, depending on which operation you expect to be called more often.

### Approach 1 — Cheap-to-read, expensive push (O(n) push)

**The logic for `push(x)`:** always keep `s1` empty before inserting, so the queue's front-to-back order is preserved top-to-bottom in `s1` at all other times.

1. Move everything from `s1` into `s2`.
2. Push `x` onto (now-empty) `s1`.
3. Move everything back from `s2` into `s1`.

```cpp
class QueueUsingStacks_V1 {
public:
    stack<int> s1, s2;

    void push(int x) {
        while (!s1.empty()) {
            s2.push(s1.top());
            s1.pop();
        }
        s1.push(x);
        while (!s2.empty()) {
            s1.push(s2.top());
            s2.pop();
        }
    }

    int pop()  { int v = s1.top(); s1.pop(); return v; }
    int top()  { return s1.top(); }
    int size() { return s1.size(); }
};
```

**Walkthrough — `push(4), push(2)`:**

```
push(4): s1 empty already → s1.push(4)
  s1 (top→bottom): [4]
  s2:               []

push(2):
  Step 1 — move s1 into s2:
    s1: []     s2 (top→bottom): [4]

  Step 2 — push 2 onto empty s1:
    s1 (top→bottom): [2]     s2 (top→bottom): [4]

  Step 3 — move s2 back into s1:
    s1 (top→bottom): [2, 4]     s2: []
              ↑
     wait — check this: popping 4 off s2 and pushing onto s1
     puts 4 ON TOP of 2, since s1 already had 2 on top.

  s1 (top→bottom): [4, 2]

top() = 4  ← correct! 4 was pushed first, so it should come out first
```

**Time complexity:** `push` is **O(n)** every single time (two full transfers), while `pop`, `top`, `size` are **O(1)**.

### Approach 2 — Cheap push, occasionally-expensive read (amortized O(1) everything)

**The insight:** don't rearrange on every push — instead, always push new elements onto `s1` freely, and only shuffle things into `s2` **lazily, when `s2` is empty and someone actually asks for `pop()`/`top()`**.

```cpp
class QueueUsingStacks_V2 {
public:
    stack<int> s1, s2;

    void push(int x) {
        s1.push(x);   // always cheap
    }

    int pop() {
        if (s2.empty()) {
            while (!s1.empty()) {
                s2.push(s1.top());
                s1.pop();
            }
        }
        int val = s2.top();
        s2.pop();
        return val;
    }

    int top() {
        if (s2.empty()) {
            while (!s1.empty()) {
                s2.push(s1.top());
                s1.pop();
            }
        }
        return s2.top();
    }
};
```

**Walkthrough — `push(2), push(3), push(4), push(5)`, then `top()`:**

```
push(2),(3),(4),(5): all go straight onto s1, no transfer
  s1 (top→bottom): [5, 4, 3, 2]     s2: []

top():  s2 is empty → transfer ALL of s1 into s2
  pop 5, push onto s2 →  s1:[4,3,2]   s2:[5]
  pop 4, push onto s2 →  s1:[3,2]     s2:[4,5]
  pop 3, push onto s2 →  s1:[2]       s2:[3,4,5]
  pop 2, push onto s2 →  s1:[]        s2:[2,3,4,5]

  s2 (top→bottom): [2, 3, 4, 5]   ← 2 is now on top, the earliest pushed!
  return s2.top() = 2   ← correct!
```

**Now `pop()`, then `push(1)`, then `pop()` three times:**

```
pop(): s2 not empty → just s2.pop()
  returns 2,   s2 (top→bottom): [3, 4, 5]

push(1): s1.push(1)
  s1: [1]     s2: [3, 4, 5]   ← s2 untouched, its order is still valid

pop(): s2 not empty → s2.pop() → returns 3,  s2: [4, 5]
pop(): s2 not empty → s2.pop() → returns 4,  s2: [5]
pop(): s2 not empty → s2.pop() → returns 5,  s2: []

top(): s2 empty now → transfer s1 (just [1]) into s2
  s1: []   s2: [1]
  return 1
```

⚠️ **Why this is still correct even though `s1` and `s2` can both hold elements at once:** as long as `s2` isn't empty, its order is already guaranteed correct (oldest on top) from a previous transfer, so newly-pushed elements in `s1` are safely left untouched until `s2` genuinely runs dry.

**Time complexity:** `push` is **always O(1)**. `pop`/`top` are **O(1)** on average — the expensive O(n) transfer only happens *occasionally* (whenever `s2` is empty), not on every call. This is called **amortized O(1)**.

**Space complexity (both approaches):** O(n) — two stacks, dynamic sizing.

---

## 11. Summary — Time Complexity Across All Implementations

| Implementation | push | pop | top | size | Space |
| --- | --- | --- | --- | --- | --- |
| Stack — Array (Class 1) | O(1) | O(1) | O(1) | O(1) | O(fixed capacity) |
| Queue — Array, circular (Class 1) | O(1) | O(1) | O(1) | O(1) | O(fixed capacity) |
| Stack — Linked List | O(1) | O(1) | O(1) | O(1) | O(n) |
| Queue — Linked List | O(1) | O(1) | O(1) | O(1) | O(n) |
| Stack using Queue | O(n) | O(1) | O(1) | O(1) | O(n) |
| Queue using 2 Stacks (V1) | O(n) | O(1) | O(1) | O(1) | O(n) |
| Queue using 2 Stacks (V2) | O(1) | O(1)* amortized | O(1)* amortized | — | O(n) |

**Important teaching point:** array-based implementations are O(1) across the board but pay for it with a **fixed, wasted capacity**. Linked-list versions trade that fixed waste for **exactly the memory needed**, at no extra time cost — the same array-vs-linked-list trade-off from Unit II, just showing up again in a new context. The stack↔queue cross-implementations exist purely to test whether you *understand* the LIFO/FIFO distinction deeply enough to fake one using the other.