# Lecture 4: Circular Linked List + Wrap-up

Status: Pending

## Quick Recap Before We Begin

Across Classes 1–3, every list we built eventually **ended** — the last node's `next` (and in DLLs, the first node's `prev`) pointed to `NULL`, clearly marking a beginning and an end. Today we look at what happens when a list has **no end at all** — it loops back on itself, forming a circle. Then we'll wrap up the unit with real-world applications and a full comparison of everything we've covered.

---

## 1. What is a Circular Linked List?

A **circular linked list** is a linked list where the **last node points back to the first node**, instead of pointing to `NULL`. This means there's no "end" — if you keep traversing, you'll loop around forever.

**Real-life analogy:** Recall the treasure hunt from Class 1, where the last clue said "the hunt ends here." A circular linked list is like a treasure hunt where the **last clue secretly points back to the very first clue** — there's no true ending, just an endless loop, like people seated around a **round dinner table** instead of a straight line. There's no "first" or "last" seat in any absolute sense — you can start counting from anywhere and keep going around indefinitely.

```
Circular Singly Linked List:

        ┌──────────────────────────────────────────┐
        │                                          │
        ▼                                          │
┌────┬───┐        ┌────┬───┐        ┌────┬───┐     │
│ 10 │ ●─┼───────►│ 20 │ ●─┼───────►│ 30 │ ●─┼─────┘
└────┴───┘        └────┴───┘        └────┴───┘
   ▲
  head
```

Notice: node `30`'s `next` doesn't point to `NULL` anymore — it points **back to node 10**.

---

## 2. Circular Singly Linked List — Key Difference in Code

The `Node` structure is exactly the same as a normal singly linked list — the only thing that changes is **where the last node's `next` points**.

```cpp
struct Node {
    int data;
    Node* next;
};
```

**Creating a circular list of 3 nodes:**

```cpp
Node* first  = new Node();
Node* second = new Node();
Node* third  = new Node();

first->data  = 10;
second->data = 20;
third->data  = 30;

first->next  = second;
second->next = third;
third->next  = first;    // ⚠️ points back to first, NOT nullptr!

Node* head = first;
```

⚠️ **This one line — `third->next = first;` instead of `third->next = nullptr;`** — is the *entire* difference between a normal singly linked list and a circular one. Everything else about node creation stays identical to Class 1.

---

## 3. Traversal — Why `while (temp != nullptr)` No Longer Works

In every previous class, our traversal loop relied on eventually hitting `nullptr` to know when to stop. In a circular list, that **never happens** — the loop would run forever, since `temp` keeps looping back to `head` endlessly.

```cpp
// ❌ DANGEROUS on a circular list — infinite loop!
void printListWrong(Node* head) {
    Node* temp = head;
    while (temp != nullptr) {
        cout << temp->data << " -> ";
        temp = temp->next;   // never becomes nullptr!
    }
}
```

**Real-life analogy:** Imagine walking around that round dinner table, waiting for someone to say "there's no one after me" — but nobody ever will, because it's a circle. You'd keep walking around and around forever unless you specifically remember **where you started** and stop once you get back there.

**The fix — stop when we return to `head`, not when we hit `nullptr`:**

```cpp
void printList(Node* head) {
    if (head == nullptr) return;   // empty list check

    Node* temp = head;
    do {
        cout << temp->data << " -> ";
        temp = temp->next;
    } while (temp != head);   // stop once we're back at the start

    cout << "(back to head)" << endl;
}
```

**Output:** `10 -> 20 -> 30 -> (back to head)`

⚠️ **Why `do-while` instead of `while`?** With a regular `while (temp != head)`, the loop would never even execute its first iteration — since `temp` **starts** at `head`, the condition `temp != head` is false immediately! A `do-while` guarantees the body runs at least once *before* checking the condition, which is exactly the "check after" behavior we need here (this is the same `do-while` you learned in Unit I — a nice callback to that).

---

## 4. Insertion at the Head (Circular Singly Linked List)

**The logic:**

1. Create the new node.
2. If the list is empty, the new node points to *itself* (it's the only node — the circle starts and ends with itself).
3. Otherwise, traverse to the **last** node (the one whose `next` is `head`), so we can update its `next` to the new node.
4. Link the new node in front, and update `head`.

```cpp
Node* insertAtHead(Node* head, int value) {
    Node* newNode = new Node();
    newNode->data = value;

    if (head == nullptr) {
        newNode->next = newNode;   // points to itself!
        return newNode;
    }

    Node* temp = head;
    while (temp->next != head) {   // find the last node
        temp = temp->next;
    }

    newNode->next = head;    // new node points to old head
    temp->next = newNode;     // last node now points to new node
    head = newNode;

    return head;
}
```

**Real-life analogy:** At the round dinner table, seating a new person "first" still means someone has to update the seat of the person currently sitting "last" (right before the old first seat) so they now point to the new person instead. Even though there's no true "start" at a round table, we still need to walk all the way around once to find whoever currently considers themselves last.

⚠️ Notice this makes insertion at head **O(n)** here, unlike a normal singly linked list where it was O(1) — because we must find the last node to fix its `next` pointer. (This overhead disappears if you maintain a `tail` pointer alongside `head`, similar to what we did for doubly linked lists in Class 3.)

---

## 5. Circular Doubly Linked List (Brief Overview)

Just as we combined "doubly" (two pointers) in Class 3, we can combine "circular" with "doubly" too — giving the most flexible (and most memory-heavy) structure of the four:

- `head->prev` points to the **last** node (instead of `nullptr`).
- `tail->next` points to the **first** node (instead of `nullptr`).
- You can walk forward *or* backward, endlessly, in either direction.

```
        ┌────────────────────────────────────────────────────────────┐
        │                                                            │
        ▼                                                            │
      ┌────┬────┬───┐        ┌────┬────┬───┐        ┌────┬────┬───┐  │
      │prev│ 10 │ ●─┼───────►│prev│ 20 │ ●─┼───────►│prev│ 30 │ ●─┼──┘
──────┼─●  │    │   │◄───────┼─●  │    │   │◄───────┼─●  │    │   │
│     └────┴────┴───┘        └────┴────┴───┘        └────┴────┴───┘
│        ▲
└────────┘
        head
```

**Real-life analogy:** This is the round dinner table again, but now everyone holds hands with **both** neighbors, just like the doubly linked list from Class 3 — except the line has been curved into a full circle, so there's no empty hand anywhere at all.

We won't write full code for this variant — the pattern is a direct combination of "circular" (Sections 1–4) and "doubly" (Class 3), and working through it yourself is a great practice exercise (see Section 9, Question 4).

---

## 6. Real-World Applications of Linked Lists

It's worth connecting these structures to where they're actually used, so the concepts don't feel purely academic:

| Application | Which type of list, and why |
| --- | --- |
| **Music/video "playlist" with repeat/loop** | Circular linked list — after the last song, it loops back to the first automatically |
| **Browser back/forward navigation** | Doubly linked list — you move forward to new pages, and backward to previous ones |
| **Undo/redo in text editors** | Doubly linked list — each action links to the previous and next state |
| **Multiplayer board game with turns (e.g. passing a turn around a table)** | Circular linked list — after the last player's turn, control loops back to the first player |
| **Implementing other data structures (Stacks, Queues)** | Singly linked list — simple, memory-efficient chains, which we'll cover in the next unit |
| **Memory management / free-block lists in operating systems** | Singly or doubly linked list, depending on whether blocks need to be merged with neighbors on both sides |

**Real-life analogy for the whole idea:** Just like a treasure hunt (singly), a two-way handshake line (doubly), and a round dinner table (circular) are all different *shapes* for organizing people, real software picks whichever "shape" naturally matches the problem it's solving — a playlist that repeats naturally *is* a circle, so we use a circular list, rather than forcing it into a straight line and adding extra code to fake the loop.

---

## 7. Array vs Singly vs Doubly vs Circular — Full Comparison

| Feature | Array | Singly Linked List | Doubly Linked List | Circular Linked List |
| --- | --- | --- | --- | --- |
| Memory layout | contiguous | scattered | scattered | scattered |
| Direct index access (`arr[i]`) | O(1) | O(n) | O(n) | O(n) |
| Insert/delete at head | O(n) (shifting) | O(1) | O(1) | O(n)* |
| Insert/delete at tail | O(1) (if space available) | O(n) (O(1) with tail ptr) | O(1) (with tail ptr) | O(n)* |
| Traverse backward | ✅ (via index) | ❌ | ✅ | only if also doubly |
| Has a clear "end" (`NULL`) | ✅ | ✅ | ✅ | ❌ — loops forever |
| Extra memory per element | none | 1 pointer | 2 pointers | 1 or 2 pointers |
| Natural fit for | fixed-size, fast lookup data | simple dynamic chains, stacks | undo/redo, back/forward nav | round-robin/looping data (playlists, turn-based systems) |
- *O(n) for a circular singly linked list without a `tail` pointer, as shown in Section 4 — becomes O(1) if a `tail` pointer is maintained, same trick we used for doubly linked lists in Class 3.*

**The single biggest idea to leave students with:** every one of these structures makes the **same fundamental trade-off** — how much are you willing to spend (extra memory, extra pointer-updates) to gain speed on the operations you actually care about? Arrays optimize for fast *access*. Linked lists optimize for fast *insertion/deletion*. Doubly adds backward movement at the cost of memory. Circular removes the concept of "ending" entirely, for problems that are naturally cyclical. There's no universally "best" structure — only the best *fit* for the problem in front of you.

---

## 8. Full Working Example (Circular Singly Linked List)

```cpp
#include <iostream>
using namespace std;

struct Node {
    int data;
    Node* next;
};

Node* insertAtHead(Node* head, int value) {
    Node* newNode = new Node();
    newNode->data = value;

    if (head == nullptr) {
        newNode->next = newNode;
        return newNode;
    }

    Node* temp = head;
    while (temp->next != head) {
        temp = temp->next;
    }

    newNode->next = head;
    temp->next = newNode;
    head = newNode;

    return head;
}

void printList(Node* head) {
    if (head == nullptr) return;

    Node* temp = head;
    do {
        cout << temp->data << " -> ";
        temp = temp->next;
    } while (temp != head);

    cout << "(back to head)" << endl;
}

int main() {
    Node* head = nullptr;

    head = insertAtHead(head, 30);   // list: 30 (points to itself)
    head = insertAtHead(head, 20);   // list: 20 -> 30
    head = insertAtHead(head, 10);   // list: 10 -> 20 -> 30

    printList(head);   // 10 -> 20 -> 30 -> (back to head)

    return 0;
}
```

---

## 9. Practice Questions for Students

1. Trace `insertAtHead` step by step for a circular list `20 -> 30 -> (back to 20)`, inserting `10` at the head. Draw the before/after diagram.
2. Write a function `countNodes(Node* head)` for a **circular** singly linked list. Why can't you reuse the exact `countNodes` function from Class 1 without modification?
3. Write a function `deleteAtHead` for a circular singly linked list. Think carefully about what should happen if the list has only **one** node before deletion.
4. As a challenge: sketch (in comments or pseudocode) how `insertAtTail` would work for a **circular doubly linked list**, using the ideas from Section 5. How does having a `tail` pointer change the time complexity compared to Section 4's circular singly linked list?
5. A music app's "shuffle + repeat" playlist and a browser's back/forward history are both mentioned in Section 6. Explain in your own words why one naturally fits a **circular** list and the other fits a **doubly** (non-circular) list.

---

That completes all **4 classes’ theory** of the Linked Lists unit for CS205:

- **Class 1** – Introduction to Linked Lists & Singly Linked List Basics
- **Class 2** – Singly Linked List – Operations
- **Class 3** – Doubly Linked List
- **Class 4** – Circular Linked List + Wrap-up