# Lecture 1: Introduction to Linked Lists & Singly Linked List Basics

Date: September 2, 2026
Status: Done

## 1. Before We Start: Why Do We Need Linked Lists When Arrays Already Exist?

You already know arrays store a bunch of elements together. So why do we need another data structure to do something similar?

The answer lies in a big **limitation of arrays**: arrays need a **fixed, contiguous** block of memory decided in advance.

**Real-life analogy:** Imagine a **row of parking slots** painted on the ground, side by side, all fixed in place. That's an array. If you need to fit one more car and the row is already full, you can't just "add a slot" — there's no room left. You'd have to find an entirely new, bigger row of slots elsewhere and move every car into it.

```
Array (fixed row of slots):

┌────┬────┬────┬────┬────┐
│ 10 │ 20 │ 30 │ 40 │ 50 │   ← full! no room to add a 6th
└────┴────┴────┴────┴────┘
```

A **linked list** solves this differently — instead of demanding one continuous block of memory, it stores elements **scattered anywhere in memory**, and connects them using **pointers**.

**Real-life analogy:** Think of a **treasure hunt** instead of a parking lot. Each clue (element) can be hidden **anywhere** — under a rock, in a tree, behind a door — it doesn't matter where, because each clue also tells you **where to find the next one**. You don't need all the clues to be in one neat row; you just need each one to correctly point to the next.

```
Linked List (scattered, connected by pointers):

┌────┬───┐        ┌────┬───┐        ┌────┬───┐         ┌────┬──────┐
│ 10 │ ●─┼───────►│ 20 │ ●─┼───────►│ 30 │ ●─┼───────► │ 40 │ NULL │
└────┴───┘        └────┴───┘        └────┴───┘         └────┴──────┘
 (memory            (memory            (memory            (memory
  location            location           location           location
  0x1000)             0x2500)            0x1900)            0x3100)
```

Notice: the nodes are **not next to each other in memory** (0x1000, 0x2500, 0x1900, 0x3100 — all over the place), but they're still perfectly connected, because each one carries the address of the next.

---

## 2. Array vs Linked List — Core Trade-off

| Feature | Array | Linked List |
| --- | --- | --- |
| Memory | contiguous (one connected block) | scattered (nodes anywhere in memory) |
| Size | fixed at creation (in most cases) | grows/shrinks easily at runtime |
| Access an element | instant, by index (`arr[3]`) | must walk from the start, node by node |
| Insert/delete in the middle | costly — have to shift elements | cheap — just re-link a couple of pointers |
| Extra memory used | none — just the data | extra memory per element, to store the pointer |

**Real-life analogy:** Finding the 4th house on a **numbered street** (array) is instant — you just walk straight to house #4. But finding the 4th clue in a **treasure hunt** (linked list) means you must go clue by clue from the start — clue 1 leads to clue 2, clue 2 leads to clue 3, and so on. You can't "jump" directly to clue 4 without following the chain.

This trade-off — **fast access vs flexible size/insertion** — is the single most important thing to remember about linked lists, and it's why we choose one structure over the other depending on the situation.

---

## 3. What Exactly Is a "Node"?

A linked list isn't one single block — it's built from many small units called **nodes**, chained together. Each node has exactly two parts:

1. **Data** — the actual value being stored.
2. **Next** — a pointer holding the address of the *next* node in the chain.

```
      ┌─────────────┬─────────────┐
      │    data     │    next     │
      └─────────────┴─────────────┘
             one "node"
```

**Real-life analogy:** A node is like a single **train compartment**. Each compartment carries passengers (data), and has a **coupling hook** (next pointer) connecting it to the compartment behind it. The whole train (list) is just compartments hooked together one after another.

```cpp
struct Node {
    int data;       // the value stored in this node
    Node* next;     // pointer to the next node in the list
};
```

⚠️ Notice `Node* next;` is a pointer to the **same struct type** (`Node`) — this is called a **self-referential structure**, and it's exactly what lets nodes chain together. This is the direct payoff of everything we studied about pointers in Unit II — a linked list literally could not exist without pointers.

---

## 4. The Special Role of `head`

Since nodes are scattered in memory, how do we even find the *first* one? We keep a single pointer called `head`, which always points to the first node of the list. If you lose `head`, you lose access to the **entire list** — there's no other way to reach it.

```
head
 │
 ▼
┌────┬───┐        ┌────┬───┐        ┌────┬──────┐
│ 10 │ ●─┼───────►│ 20 │ ●─┼───────►│ 30 │ NULL │
└────┴───┘        └────┴───┘        └────┴──────┘
```

**Real-life analogy:** `head` is like the **very first clue's location** in the treasure hunt, written on a card you keep in your pocket. Lose that card, and it doesn't matter how well-connected the rest of the clues are — you have no way to even begin.

The **last node's** `next` pointer is set to `NULL` (or `nullptr` in modern C++) — this is how we know the list has ended, just like the last train compartment has no hook trailing behind it.

---

## 5. Creating Nodes and Linking Them Manually

Let's build the 3-node list shown above, step by step, using raw pointers first (before writing reusable functions) — so you see exactly what's happening in memory.

```cpp
#include <iostream>
using namespace std;

struct Node {
    int data;
    Node* next;
};

int main() {
    // Step 1: create three separate nodes using dynamic memory allocation
    Node* first  = new Node();
    Node* second = new Node();
    Node* third  = new Node();

    // Step 2: fill in the data for each node
    first->data  = 10;
    second->data = 20;
    third->data  = 30;

    // Step 3: link them together
    first->next  = second;   // first's next points to second
    second->next = third;    // second's next points to third
    third->next  = nullptr;  // third is the last node, so it points to nothing

    // Step 4: keep track of the starting point
    Node* head = first;

    return 0;
}
```

```
Step 1-2: Three separate, unconnected nodes just created

┌────┬──────┐   ┌────┬──────┐   ┌────┬──────┐
│ 10 │  ?   │   │ 20 │  ?   │   │ 30 │  ?   │
└────┴──────┘   └────┴──────┘   └────┴──────┘
  first           second          third

Step 3: After linking

┌────┬───┐      ┌────┬───┐     ┌────┬──────┐
│ 10 │  ●─┼────►│ 20 │ ●─┼────►│ 30 │ NULL │
└────┴───┘      └────┴───┘     └────┴──────┘
  first          second          third

Step 4: head just points to the same place 'first' does

head, first
      │
      ▼
┌────┬───┐     ┌────┬───┐      ┌────┬──────┐
│ 10 │ ●─┼────►│ 20 │ ●─┼────► │ 30 │ NULL │
└────┴───┘     └────┴───┘      └────┴──────┘
```

⚠️ **Important — `->` operator:** Since `first`, `second`, `third` are all **pointers to** `Node` (not the node itself), we use `->` (the arrow operator) to access their members, instead of `.`. `first->data` is shorthand for `(*first).data` — "go to the address `first` points to, then access `data` there." You'll use `->` constantly when working with linked lists.

---

## 6. Traversing the List (Visiting Every Node)

"Traversal" means walking through the list from `head` to the end, visiting each node exactly once — the linked-list equivalent of a `for` loop over an array.

```cpp
void printList(Node* head) {
    Node* temp = head;          // start at the beginning
    while (temp != nullptr) {   // keep going until we fall off the end
        cout << temp->data << " -> ";
        temp = temp->next;      // move to the next node
    }
    cout << "NULL" << endl;
}
```

**Output for our 3-node list:** `10 -> 20 -> 30 -> NULL`

**Real-life analogy:** This is exactly the treasure hunt again — `temp` is **you**, standing at a clue. You read the data at your current clue, then follow the "next" pointer to physically walk to the next clue's location. You keep doing this until a clue says "there's nothing next" (`NULL`) — that's your signal the hunt is over.

```
Walkthrough:

temp = head (points to node with 10)
  → print 10, then temp = temp->next  (temp now points to node with 20)
  → print 20, then temp = temp->next  (temp now points to node with 30)
  → print 30, then temp = temp->next  (temp now points to NULL)
  → temp == nullptr → loop stops
```

⚠️ **Why we use a separate `temp` pointer instead of moving `head` itself:** If we moved `head` forward during traversal, we'd permanently lose the beginning of the list once we're done printing — exactly like losing your only card with the first clue's address. `temp` is a disposable "walking" pointer; `head` must always stay parked at the first node.

---

## 7. Counting the Number of Nodes

A simple, very common operation — walk the list and count how many nodes you pass.

```cpp
int countNodes(Node* head) {
    int count = 0;
    Node* temp = head;
    while (temp != nullptr) {
        count++;
        temp = temp->next;
    }
    return count;
}
```

**Real-life analogy:** Same treasure hunt walk as before, except this time you're just keeping a tally on your fingers of how many clues you visited, not reading what's written on them.

---

## 8. Putting It All Together — Full Working Example

```cpp
#include <iostream>
using namespace std;

struct Node {
    int data;
    Node* next;
};

void printList(Node* head) {
    Node* temp = head;
    while (temp != nullptr) {
        cout << temp->data << " -> ";
        temp = temp->next;
    }
    cout << "NULL" << endl;
}

int countNodes(Node* head) {
    int count = 0;
    Node* temp = head;
    while (temp != nullptr) {
        count++;
        temp = temp->next;
    }
    return count;
}

int main() {
    Node* first  = new Node();
    Node* second = new Node();
    Node* third  = new Node();

    first->data  = 10;
    second->data = 20;
    third->data  = 30;

    first->next  = second;
    second->next = third;
    third->next  = nullptr;

    Node* head = first;

    printList(head);                          // 10 -> 20 -> 30 -> NULL
    cout << "Total nodes: " << countNodes(head); // Total nodes: 3

    return 0;
}
```

---

## 9. Quick Recap Table

| Concept | Meaning | Real-life analogy |
| --- | --- | --- |
| Node | a unit holding data + a pointer to the next node | one train compartment with a coupling hook |
| `head` | pointer to the first node — the only entry point into the list | the card in your pocket with the first clue's location |
| `next` pointer | connects one node to the next | the coupling hook / the "go here next" instruction on a clue |
| `NULL` at the end | signals "no more nodes" | the last clue says "the hunt ends here" |
| Traversal | walking node-by-node from `head` to the end using a temporary pointer | following the treasure hunt clue by clue |
| Array vs Linked List | contiguous & fast-access vs scattered & flexible-size | numbered street vs treasure hunt |

---

## 10. Practice Questions for Students

1. Create a linked list of 4 nodes storing the values `5, 15, 25, 35` manually (like in Section 5), then write code to print it.
2. What would happen if you forgot to set the last node's `next` to `nullptr`? Why is this dangerous during traversal?
3. Modify `printList` to print the sum of all node values instead of the values themselves.
4. Given a linked list, write a function `getNthNode(Node* head, int n)` that returns the data at the *n*th position (0-indexed) by traversal. What happens if `n` is larger than the list's length?