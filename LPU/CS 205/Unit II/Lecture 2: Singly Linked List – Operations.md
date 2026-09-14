# Lecture 2: Singly Linked List – Operations

Date: September 7, 2026
Status: Done

## Quick Recap Before We Begin

In Class 1, we built the basic `Node` structure, learned about `head`, and wrote code to **traverse** and **count** a linked list. Today we go a big step further: actually **inserting** and **deleting** nodes — at the head, at the tail, and at any given position. This is the heart of linked list operations, and where most exam/interview questions come from.

We'll keep using the same `Node` structure from Class 1:

```cpp
struct Node {
    int data;
    Node* next;
};
```

---

## 1. Insertion at the Head (Beginning)

This is the **easiest and fastest** insertion — no traversal needed at all.

**The logic:**

1. Create a new node.
2. Make the new node's `next` point to the current `head`.
3. Make `head` point to the new node.

```cpp
Node* insertAtHead(Node* head, int value) {
    Node* newNode = new Node();
    newNode->data = value;
    newNode->next = head;   // new node points to old first node
    head = newNode;          // head now points to the new node
    return head;
}
```

**Real-life analogy:** Imagine a **queue of people waiting outside a shop**. Inserting at the head is like a new person walking straight to the **front** of the queue and simply saying "everyone who was first is now behind me" — they don't need to talk to anyone else in the queue, they just need to know who was previously first.

```
**Before**:
head
 │
 ▼
┌────┬───┐     ┌────┬──────┐
│ 10 │ ●─┼────►│ 20 │ NULL │
└────┴───┘     └────┴──────┘

**Inserting 5 at head:**

**Step 1**: newNode->next = head
                 head
                  │
                  ▼
┌───┬───┐      ┌────┬───┐     ┌────┬──────┐
│ 5 │ ●─┼─────►│ 10 │ ●─┼────►│ 20 │ NULL │
└───┴───┘      └────┴───┘     └────┴──────┘
 newNode

**Step 2**: head = newNode
head
 │
 ▼
┌───┬───┐      ┌────┬───┐     ┌────┬──────┐
│ 5 │ ●─┼─────►│ 10 │ ●─┼────►│ 20 │ NULL │
└───┴───┘      └────┴───┘     └────┴──────┘
```

⚠️ **Order matters!** You must set `newNode->next = head` **before** changing `head` itself — otherwise you'd lose the address of the rest of the list forever (this is a classic beginner bug).

**Time complexity:** O(1) — constant time, since there's no traversal involved.

---

## 2. Insertion at the Tail (End)

This one is slower, because we don't have direct access to the last node — we must **traverse** the entire list to find it first.

**The logic:**

1. Create a new node, with `next = nullptr` (since it will become the new last node).
2. If the list is empty, the new node simply becomes `head`.
3. Otherwise, traverse until you find the current last node (the one whose `next` is `nullptr`).
4. Set that last node's `next` to the new node.

```cpp
Node* insertAtTail(Node* head, int value) {
    Node* newNode = new Node();
    newNode->data = value;
    newNode->next = nullptr;

    if (head == nullptr) {        // empty list — new node becomes head
        return newNode;
    }

    Node* temp = head;
    while (temp->next != nullptr) {  // walk until the last node
        temp = temp->next;
    }
    temp->next = newNode;             // link last node to new node

    return head;
}
```

**Real-life analogy:** This is like a new person joining the **back of the queue**. But since there's no direct way to know who's currently last, you have to walk all the way from the front of the queue, tapping each person's shoulder and asking "are you the last one?" — until you finally reach the person at the very back, and only they get told about the new person joining behind them.

```
**Before:**
head
 │
 ▼
┌────┬───┐     ┌────┬──────┐
│ 10 │ ●─┼────►│ 20 │ NULL │
└────┴───┘     └────┴──────┘

**Inserting 30 at tail** — traverse until temp->next == nullptr:

temp starts at head (10) → temp->next is NOT null → move to 20
temp is now at 20 → temp->next IS null → this is the last node!

Link it:
┌────┬───┐     ┌────┬───┐     ┌────┬──────┐
│ 10 │ ●─┼────►│ 20 │ ●─┼────►│ 30 │ NULL │
└────┴───┘     └────┴───┘     └────┴──────┘
                                ↑ newly linked
```

**Time complexity:** O(n) — you must walk through every node to reach the end.

---

## 3. Insertion at a Given Position

A more general operation: insert a new node at position `pos` (let's say 0-indexed, so position 0 = head).

**The logic:**

1. If `pos == 0`, this is really just insertion at head — handle it directly.
2. Otherwise, traverse to the node **just before** the target position.
3. Re-link pointers so the new node fits in between.

```cpp
Node* insertAtPosition(Node* head, int value, int pos) {
    if (pos == 0) {
        return insertAtHead(head, value);
    }

    Node* newNode = new Node();
    newNode->data = value;

    Node* temp = head;
    for (int i = 0; i < pos - 1; i++) {   // stop at the node BEFORE pos
        temp = temp->next;
    }

    newNode->next = temp->next;   // new node points to what comes after it
    temp->next = newNode;          // previous node now points to new node

    return head;
}
```

**Real-life analogy:** Think of people standing in a queue holding hands in a chain — each person holding the hand of the person behind them. To insert someone at position 3, you walk up to the person currently at position 2, ask them to **let go** of the hand of the person behind them, hold the new person's hand instead, and have the new person hold the hand of whoever was originally next. Nobody else in the queue needs to move at all — only one connection point changes.

```
**Before** (positions: 0,1,2):
head
 │
 ▼
┌────┬───┐     ┌────┬───┐     ┌────┬──────┐
│ 10 │ ●─┼────►│ 20 │ ●─┼────►│ 30 │ NULL │
└────┴───┘     └────┴───┘     └────┴──────┘
  pos 0          pos 1          pos 2

**Inserting 15 at position 1**:

temp walks to position (pos - 1) = 0, i.e. the node with 10

Step 1: newNode->next = temp->next   (newNode's next = node with 20)
Step 2: temp->next = newNode          (node with 10's next = newNode)

After:
┌────┬───┐     ┌────┬───┐     ┌────┬───┐     ┌────┬──────┐
│ 10 │ ●─┼────►│ 15 │ ●─┼────►│ 20 │ ●─┼────►│ 30 │ NULL │
└────┴───┘     └────┴───┘     └────┴───┘     └────┴──────┘
  pos 0          pos 1(new)     pos 2          pos 3
```

⚠️ Just like insertion at head, **order matters**: always set `newNode->next` **first**, before changing `temp->next` — otherwise you'd disconnect the rest of the list before the new node has a chance to "grab hold" of it.

**Time complexity:** O(n) in the worst case (inserting near the end), since we may need to traverse most of the list to reach position `pos - 1`.

---

## 4. Deletion from the Head

The simplest deletion — just move `head` forward by one node, and free the old first node.

**The logic:**

1. Keep a temporary pointer to the current `head` (so we don't lose it before freeing memory).
2. Move `head` to `head->next`.
3. Delete the old first node.

```cpp
Node* deleteAtHead(Node* head) {
    if (head == nullptr) {   // empty list, nothing to delete
        return head;
    }

    Node* temp = head;        // remember the node we're about to remove
    head = head->next;        // head moves to the second node
    delete temp;               // free the old first node's memory

    return head;
}
```

**Real-life analogy:** This is like the **first person in the queue simply walking away**. The second person automatically becomes the new "first person" — you just update who you consider to be at the front, and the person who left is gone for good.

```
**Before:**
head
 │
 ▼
┌────┬───┐     ┌────┬───┐     ┌────┬──────┐
│ 10 │ ●─┼────►│ 20 │ ●─┼────►│ 30 │ NULL │
└────┴───┘     └────┴───┘     └────┴──────┘

**Step 1**: temp = head (temp now also points to node with 10)
**Step 2**: head = head->next

                  head
                   │
                   ▼
┌────┬───┐     ┌────┬───┐     ┌────┬──────┐
│ 10 │ ●─┼────►│ 20 │ ●─┼────►│ 30 │ NULL │
└────┴───┘     └────┴───┘     └────┴──────┘
 (temp still
  points here,
  about to be
  deleted)

**Step 3**: delete temp

                 head
                  │
                  ▼
               ┌────┬───┐     ┌────┬──────┐
               │ 20 │ ●─┼────►│ 30 │ NULL │
               └────┴───┘     └────┴──────┘
```

⚠️ **Why we need `temp` at all:** If we just wrote `head = head->next;` and then tried `delete head;`, we'd be deleting the **new** head (the second node), not the old first node — we'd lose the old node's address entirely and never free it (a **memory leak**). `temp` preserves that address just long enough to delete it safely.

**Time complexity:** O(1).

---

## 5. Deletion from the Tail

Deleting the last node requires finding the **second-to-last** node, since that node's `next` needs to be set to `nullptr` after the deletion.

**The logic:**

1. If the list is empty, or has only one node, handle those as special cases.
2. Otherwise, traverse until you find the second-to-last node (the one whose `next->next` is `nullptr`).
3. Delete the last node, and set the second-to-last node's `next` to `nullptr`.

```cpp
Node* deleteAtTail(Node* head) {
    if (head == nullptr) {          // empty list
        return head;
    }

    if (head->next == nullptr) {    // only one node in the list
        delete head;
        return nullptr;
    }

    Node* temp = head;
    while (temp->next->next != nullptr) {   // stop at **SECOND-to-last** node
        temp = temp->next;
    }

    delete temp->next;      // delete the actual last node
    temp->next = nullptr;    // second-to-last node is now the new last node

    return head;
}
```

**Real-life analogy:** This is like removing the **very last person from the queue**. Since people in a queue only know who's *behind* them (not who's in front), you have to walk all the way from the front, asking "is the person behind you the last one?" until you find the person whose neighbor is indeed last. That person then says goodbye to their neighbor and becomes the new end of the queue themselves.

```
**Before**:
head
 │
 ▼
┌────┬───┐     ┌────┬───┐     ┌────┬──────┐
│ 10 │ ●─┼────►│ 20 │ ●─┼────►│ 30 │ NULL │
└────┴───┘     └────┴───┘     └────┴──────┘

temp starts at 10 → temp->next->next is node 30 (not null) → move on
temp is now at 20 → temp->next->next IS null → 20 is second-to-last!

delete temp->next   (deletes node 30)
temp->next = nullptr

**After**:
head
 │
 ▼
┌────┬───┐     ┌────┬──────┐
│ 10 │ ●─┼────►│ 20 │ NULL │
└────┴───┘     └────┴──────┘
```

**Time complexity:** O(n) — same reasoning as tail insertion, we must walk almost the entire list.

---

## 6. Deletion at a Given Position

Similar to insertion at a position — find the node **just before** the target, and re-link around the node being removed.

```cpp
Node* deleteAtPosition(Node* head, int pos) {
    if (head == nullptr) {
        return head;
    }

    if (pos == 0) {
        return deleteAtHead(head);
    }

    Node* temp = head;
    for (int i = 0; i < pos - 1; i++) {   // stop at the node BEFORE pos
        temp = temp->next;
    }

    Node* nodeToDelete = temp->next;
    temp->next = nodeToDelete->next;   // skip over the node being deleted
    delete nodeToDelete;

    return head;
}
```

**Real-life analogy:** Back to the hand-holding queue. To remove the person at position 2, you go to the person at position 1, have them **let go** of the hand of the person at position 2, and instead hold hands directly with whoever was at position 3. The person at position 2 is now completely disconnected from the chain and can step away.

```
Before (positions 0,1,2,3):
┌────┬───┐   ┌────┬───┐   ┌────┬───┐   ┌────┬──────┐
│ 10 │ ●─┼──►│ 20 │ ●─┼──►│ 30 │ ●─┼──►│ 40 │ NULL │
└────┴───┘   └────┴───┘   └────┴───┘   └────┴──────┘
  pos 0        pos 1        pos 2        pos 3

Deleting position 2 (node with 30):

temp walks to position (pos - 1) = 1, i.e. node with 20
nodeToDelete = temp->next  (points to node with 30)

Step 1: temp->next = nodeToDelete->next   (20's next now points to 40, skipping 30)
Step 2: delete nodeToDelete                 (node with 30 is freed)

After:
┌────┬───┐   ┌────┬───┐   ┌────┬──────┐
│ 10 │ ●─┼──►│ 20 │ ●─┼──►│ 40 │ NULL │
└────┴───┘   └────┴───┘   └────┴──────┘
```

**Time complexity:** O(n) in the worst case.

---

## 7. Searching for a Value

A straightforward traversal that stops early if the value is found.

```cpp
bool search(Node* head, int key) {
    Node* temp = head;
    while (temp != nullptr) {
        if (temp->data == key) {
            return true;      // found it!
        }
        temp = temp->next;
    }
    return false;   // walked through the whole list, not found
}
```

**Real-life analogy:** Exactly like the treasure hunt walk from Class 1, except this time you're checking at every clue: "does this clue say the magic word?" — and you stop the moment you find it, instead of always walking to the very end.

**Time complexity:** O(n) worst case (value is at the end, or not present at all).

---

## 8. Summary Table — Time Complexity of All Operations

| Operation | Time Complexity | Why |
| --- | --- | --- |
| Insert at head | O(1) | direct access via `head`, no traversal |
| Insert at tail | O(n) | must traverse to find the last node |
| Insert at position | O(n) | must traverse to reach position - 1 |
| Delete at head | O(1) | direct access via `head` |
| Delete at tail | O(n) | must traverse to find second-to-last node |
| Delete at position | O(n) | must traverse to reach position - 1 |
| Search | O(n) | may need to check every node |

**Important teaching point:** Notice that head operations are the *only* ones that are O(1) — everything else requires traversal because a singly linked list only lets you move **forward**, never backward. This limitation is exactly what motivates **doubly linked lists**, which we'll cover in Class 3.

---

## 9. Full Working Example (All Operations Together)

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
    newNode->next = head;
    head = newNode;
    return head;
}

Node* insertAtTail(Node* head, int value) {
    Node* newNode = new Node();
    newNode->data = value;
    newNode->next = nullptr;

    if (head == nullptr) return newNode;

    Node* temp = head;
    while (temp->next != nullptr) temp = temp->next;
    temp->next = newNode;

    return head;
}

Node* deleteAtHead(Node* head) {
    if (head == nullptr) return head;
    Node* temp = head;
    head = head->next;
    delete temp;
    return head;
}

void printList(Node* head) {
    Node* temp = head;
    while (temp != nullptr) {
        cout << temp->data << " -> ";
        temp = temp->next;
    }
    cout << "NULL" << endl;
}

int main() {
    Node* head = nullptr;

    head = insertAtHead(head, 20);   // list: 20
    head = insertAtHead(head, 10);   // list: 10 -> 20
    head = insertAtTail(head, 30);   // list: 10 -> 20 -> 30
    printList(head);                   // 10 -> 20 -> 30 -> NULL

    head = deleteAtHead(head);        // list: 20 -> 30
    printList(head);                   // 20 -> 30 -> NULL

    return 0;
}
```

---

## 10. Practice Questions for Students

1. Trace through `insertAtPosition` step by step for a list `10 -> 20 -> 30 -> NULL`, inserting `99` at position 2. Draw the before/after diagram.
2. What happens if you call `deleteAtTail` on an **empty** list? What about a list with just **one** node? Verify the code handles both correctly.
3. Write a function `deleteByValue(Node* head, int value)` that deletes the *first* node containing a given value (not by position). What should happen if the value isn't found in the list?
4. Why can't we write a simple `deleteAtTail` the same O(1) way we wrote `deleteAtHead`? Explain in your own words, referencing the "forward-only" nature of singly linked lists.