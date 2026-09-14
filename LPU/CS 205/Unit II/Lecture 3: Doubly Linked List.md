# Lecture 3: Doubly Linked List

Date: September 8, 2026
Status: In progress

## Quick Recap Before We Begin

In Class 2, we ran into a recurring pain point: several operations (like inserting/deleting at the tail) needed O(n) time because a singly linked list only lets you move **forward** — to find the second-to-last node, we had no choice but to walk from `head` all the way to the end. Today we fix part of this limitation by giving every node a second pointer that lets us move **backward** too — and we'll be honest about exactly which operations this actually speeds up, and which ones it doesn't.

---

## 1. What is a Doubly Linked List?

A **doubly linked list (DLL)** is a linked list where each node has **two** pointers instead of one:

1. **prev** — points to the previous node.
2. **next** — points to the next node.

```
     ┌──────┬──────┬──────┐
     │ prev │ data │ next │
     └──────┴──────┴──────┘
              one "node"
```

**Real-life analogy:** Recall the treasure hunt clues from Class 1 — each clue told you where to find the *next* one, but never where you came *from*. A doubly linked list is like a treasure hunt where **every clue also has a note on the back** saying "you came from here" — so you can walk the hunt forwards *or* retrace your steps backwards, at any point.

```cpp
struct Node {
    int data;
    Node* prev;
    Node* next;
};
```

---

## 2. Visualizing a Doubly Linked List

```
NULL              ┌────┬────┬───┐        ┌────┬────┬───┐        ┌────┬────┬──────┐
 ▲                │prev│ 10 │ ●─┼───────►│prev│ 20 │ ●─┼───────►│prev│ 30 │ NULL │
 └────────────────┼─●  │    │   │◄───────┼─●  │    │   │◄───────┼─●  │    │      │
                  └────┴────┴───┘        └────┴────┴───┘        └────┴────┴──────┘
                        head
```

Two important details, compared to a singly linked list:

- The **first node's** `prev` points to `NULL` (there's nothing before it).
- The **last node's** `next` still points to `NULL`, just like before.
- We are given only `head` — exactly like every function you'll see on LeetCode or in exams. There is **no** wrapper class holding both `head` and `tail` as members; if a problem needs a `tail`, it hands you that as a **separate pointer argument**, not bundled inside a struct. We'll see exactly what this means for time complexity as we go.

**Real-life analogy:** Think of people standing in a line, but this time **holding hands with both neighbors** — the person in front, and the person behind. The very first person's front hand is empty (nothing there), and the very last person's back hand is empty. Anyone in the line can tell you who's next to them in *either* direction — but if you just walked up to this line knowing only where the *first* person is standing, you'd still have to walk down the whole line yourself to find the last person; knowing that everyone holds both hands doesn't teleport you there.

---

## 3. Insertion at the Head

**The logic:**

1. Create a new node, with `prev = nullptr` (it will be the new first node).
2. Point the new node's `next` to the current `head`.
3. If the list wasn't empty, update the old head's `prev` to point back to the new node.
4. Return the new node as the new head.

```cpp
Node* insertAtHead(Node* head, int value) {
    Node* newNode = new Node();
    newNode->data = value;
    newNode->prev = nullptr;
    newNode->next = head;

    if (head != nullptr) {
        head->prev = newNode;   // old head now points BACK to new node
    }

    return newNode;   // new node is the new head
}
```

Just like the singly linked list version from Class 2, we **return** the new head and the caller reassigns it:

```cpp
head = insertAtHead(head, 5);
```

**Real-life analogy:** A new person steps in **front** of the current first person in the line. They now hold the old first person's front hand with their own back hand — and the old first person, in turn, holds the new person's back hand with their front hand. It's a two-way handshake, unlike the one-way "hook" in a singly linked list.

```
Before:
head
 │
 ▼
NULL◄──┬────┬───┐        ┌────┬──────┐
       │ 10 │  ●─┼───────►│ 20 │ NULL │
       └────┴───┘◄───────┼─●         │
                           └──────────┘

Inserting 5 at head:

Step 1 & 2: newNode->prev = nullptr, newNode->next = head
Step 3: head->prev = newNode   (10's prev now points to new node 5)
Step 4: return newNode as the new head

head
 │
 ▼
NULL◄──┬───┬───┐        ┌────┬───┐        ┌────┬──────┐
       │ 5 │ ●─┼───────►│ 10 │ ●─┼───────►│ 20 │ NULL │
       └───┴───┘◄───────┼─●  │   │◄───────┼─●         │
                        └────┴───┘        └───────────┘
```

⚠️ **Order matters** here too — you must set `newNode->next = head` and update `head->prev` **before** the function returns, using the *original* `head` value. Since `head` here is a plain `Node*` parameter (just a local copy), the caller's variable only changes when you assign the returned value back to it.

**Time complexity:** O(1).

---

## 4. Insertion at the Tail

We are only given `head` — so, exactly like Class 2's singly linked list, we must **traverse to find the last node** ourselves.

```cpp
Node* insertAtTail(Node* head, int value) {
    Node* newNode = new Node();
    newNode->data = value;
    newNode->next = nullptr;

    if (head == nullptr) {          // empty list — new node becomes head
        newNode->prev = nullptr;
        return newNode;
    }

    Node* temp = head;
    while (temp->next != nullptr) {   // walk until the last node
        temp = temp->next;
    }

    temp->next = newNode;    // old last node points forward to new node
    newNode->prev = temp;    // new node points back to old last node

    return head;   // head itself doesn't change
}
```

**Real-life analogy:** Even though everyone in this line holds hands with both neighbors, **you** — someone just walking in from outside, knowing only where the line *starts* — still don't know who's currently last until you walk the whole line and check. Knowing that people hold both hands doesn't help an outsider skip that walk; only someone who was *already standing at the back* could skip it, and nobody handed us that shortcut here.

```
**Before**:
head
 │
 ▼
┌────┬───┐        ┌────┬──────┐
│ 10 │ ●─┼───────►│ 20 │ NULL │
└────┴───┘◄───────┼─●         │
                   └──────────┘

**Inserting 30 at tail** — must traverse to find it:

temp starts at 10 → temp->next is NOT null → move to 20
temp is now at 20 → temp->next IS null → 20 is the last node!

temp->next = newNode   (20's next points to 30)
newNode->prev = temp    (30's prev points to 20)

**After**:
head
 │
 ▼
┌────┬───┐        ┌────┬───┐        ┌────┬──────┐
│ 10 │ ●─┼───────►│ 20 │ ●─┼───────►│ 30 │ NULL │
└────┴───┘◄───────┼─●  │   │◄───────┼─●         │
                  └────┴───┘        └───────────┘
```

⚠️ **Important, honest note:** This is **O(n)**, not O(1). A common misconception is that "doubly linked list means fast tail insertion" — that's only true if a `tail` pointer is *separately maintained and kept up to date* somewhere (for example, as an extra variable a class keeps around, like in an LRU Cache implementation). Given only `head`, as in nearly all standard exam/LeetCode problems, tail insertion costs exactly what it did for a singly linked list.

**Time complexity:** O(n).

---

## 5. Deletion from the Tail

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
    while (temp->next != nullptr) {   // walk until the last node
        temp = temp->next;
    }

    temp->prev->next = nullptr;   // second-to-last node is now the new last node
    delete temp;

    return head;
}
```

**Real-life analogy:** We still have to walk the full line to physically *reach* the last person — there's no shortcut for an outsider who only knows where the line starts. But notice what happens once we're there: the last person's neighbor (`temp->prev`) is reached in a **single step**, instead of Class 2's singly linked list version, where we had to carefully stop **one node early** during the walk itself, because there was no way to look backward once we arrived.

```
**Before**:
head
 │
 ▼
┌────┬───┐        ┌────┬───┐        ┌────┬──────┐
│ 10 │ ●─┼───────►│ 20 │ ●─┼───────►│ 30 │ NULL │
└────┴───┘◄───────┼─●  │   │◄───────┼─●         │
                  └────┴───┘        └───────────┘

temp walks all the way to 30 (the last node)
temp->prev is node 20

temp->prev->next = nullptr   (20's next becomes NULL)
delete temp                    (node 30 freed)

**After**:
head
 │
 ▼
┌────┬───┐        ┌────┬──────┐
│ 10 │ ●─┼───────►│ 20 │ NULL │
└────┴───┘◄───────┼─●         │
                  └───────────┘
```

**Time complexity:** O(n) — the traversal to reach the end still costs the same as in a singly linked list, but the code is simpler and safer, since we don't need to "look one node ahead" during the walk — we walk straight to the end and step back with `->prev`.

---

## 6. Deletion at the Head

```cpp
Node* deleteAtHead(Node* head) {
    if (head == nullptr) {   // empty list
        return head;
    }

    Node* temp = head;
    head = head->next;

    if (head != nullptr) {
        head->prev = nullptr;   // new head has nothing before it
    }

    delete temp;
    return head;
}
```

**Real-life analogy:** The first person in line steps away. The second person becomes the new first person — and since there's now nobody in front of them, their "front hand" (`prev`) is empty.

**Time complexity:** O(1).

---

## 7. Deletion at a Given Node — Where a DLL Truly Shines

This is the operation where doubly linked lists give a **genuine, guaranteed** advantage — and it's the version most commonly tested (similar to LeetCode's "Delete Node in a Doubly Linked List"-style problems), because here you're **handed a direct pointer to the node to delete**, not just `head`.

```cpp
Node* deleteNode(Node* head, Node* nodeToDelete) {
    if (nodeToDelete == nullptr) return head;

    if (nodeToDelete->prev != nullptr) {
        nodeToDelete->prev->next = nodeToDelete->next;   // skip over this node, forward link
    } else {
        head = nodeToDelete->next;   // we were deleting the head
    }

    if (nodeToDelete->next != nullptr) {
        nodeToDelete->next->prev = nodeToDelete->prev;   // skip over this node, backward link
    }

    delete nodeToDelete;
    return head;
}
```

**Real-life analogy:** Think back to the hand-holding queue from Class 2's positional deletion. There, only the person **behind** the one being removed knew to reconnect — hands were held in only one direction, so someone had to walk from the front just to find "who comes before this node." Here — **only because we were directly handed the node to remove, without needing to search for it** — both of that node's neighbors already know each other exist, and can shake hands with each other immediately.

```
Given a direct pointer to the middle node (20):

┌────┬───┐        ┌────┬───┐        ┌────┬──────┐
│ 10 │ ●─┼───────►│ 20 │ ●─┼───────►│ 30 │ NULL │
└────┴───┘◄───────┼─●  │   │◄───────┼─●         │
                  └────┴───┘        └───────────┘
                 nodeToDelete

nodeToDelete->prev->next = nodeToDelete->next   (10's next now points to 30)
nodeToDelete->next->prev = nodeToDelete->prev   (30's prev now points to 10)

After:
┌────┬───┐                          ┌────┬──────┐
│ 10 │ ●─┼─────────────────────────►│ 30 │ NULL │
└────┴───┘◄─────────────────────────┼─●         │
                                    └───────────┘
```

⚠️ **Important, honest note:** The O(1) here applies **only to the re-linking step**, and only *because a direct pointer to the node was already given*. If you instead had to find that node by value or position starting from `head`, that search itself would still cost O(n) — the saving is specifically in not needing to separately locate "the node before it," which a singly linked list *would* require (since it has no `prev`).

**Time complexity:** O(1), given a direct pointer to the node.

---

## 8. Traversing Backward

Since we're only given `head`, we first traverse forward to reach the last node, and only then can we walk backward from there.

```cpp
void printReverse(Node* head) {
    if (head == nullptr) return;

    Node* temp = head;
    while (temp->next != nullptr) {   // walk forward to reach the last node
        temp = temp->next;
    }

    while (temp != nullptr) {          // now walk backward from there
        cout << temp->data << " -> ";
        temp = temp->prev;
    }
    cout << "NULL" << endl;
}
```

**Real-life analogy:** The last person in the hand-holding line can walk backward through everyone, one handshake at a time, all the way to the front — something completely impossible in a one-directional treasure-hunt-style singly linked list, where you can only ever move toward the next clue, never back to a previous one. But since we only started with `head`, we still have to walk to the back of the line first, before we can turn around.

---

## 9. Doubly Linked List vs Singly Linked List — Honest Comparison

| Feature | Singly Linked List | Doubly Linked List |
| --- | --- | --- |
| Pointers per node | 1 (`next`) | 2 (`prev`, `next`) |
| Memory per node | less | more (extra pointer) |
| Traverse forward | ✅ | ✅ |
| Traverse backward | ❌ | ✅ (but must reach the end first, if only `head` is given) |
| Insert/delete at head | O(1) | O(1) |
| Insert at tail (given only `head`) | O(n) | O(n) — **same cost**, no free improvement |
| Delete at tail (given only `head`) | O(n) | O(n) — traversal still required, though the actual unlinking step is simpler |
| Delete a given node (direct pointer in hand) | O(n) — must still search for the node *before* it | **O(1)** — `prev` is already known |

**Important teaching point:** The doubly linked list is **not** "faster at everything" — its real, guaranteed advantage over a singly linked list is **O(1) deletion once you already hold a pointer to the node**, and the ability to traverse backward at all. It is *not* automatically faster at tail insertion/deletion unless a `tail` pointer is separately tracked and maintained by whoever is using the list — that's a design choice on top of the structure, not a free property of it. Being upfront about this distinction avoids a very common misconception (and a frequent interview trick question).

The trade-off, in plain terms: doubly linked lists spend **extra memory per node** (for the `prev` pointer) to gain **backward movement and O(1) node deletion when a pointer is already known** — a classic space-vs-time trade-off you'll see repeatedly throughout DSA.

---

## 10. Full Working Example

```cpp
#include <iostream>
using namespace std;

struct Node {
    int data;
    Node* prev;
    Node* next;
};

Node* insertAtHead(Node* head, int value) {
    Node* newNode = new Node();
    newNode->data = value;
    newNode->prev = nullptr;
    newNode->next = head;

    if (head != nullptr) {
        head->prev = newNode;
    }

    return newNode;
}

Node* insertAtTail(Node* head, int value) {
    Node* newNode = new Node();
    newNode->data = value;
    newNode->next = nullptr;

    if (head == nullptr) {
        newNode->prev = nullptr;
        return newNode;
    }

    Node* temp = head;
    while (temp->next != nullptr) {
        temp = temp->next;
    }
    temp->next = newNode;
    newNode->prev = temp;

    return head;
}

Node* deleteAtHead(Node* head) {
    if (head == nullptr) return head;

    Node* temp = head;
    head = head->next;
    if (head != nullptr) {
        head->prev = nullptr;
    }
    delete temp;
    return head;
}

void printForward(Node* head) {
    Node* temp = head;
    while (temp != nullptr) {
        cout << temp->data << " -> ";
        temp = temp->next;
    }
    cout << "NULL" << endl;
}

void printReverse(Node* head) {
    if (head == nullptr) return;
    Node* temp = head;
    while (temp->next != nullptr) {
        temp = temp->next;
    }
    while (temp != nullptr) {
        cout << temp->data << " -> ";
        temp = temp->prev;
    }
    cout << "NULL" << endl;
}

int main() {
    Node* head = nullptr;

    head = insertAtTail(head, 10);
    head = insertAtTail(head, 20);
    head = insertAtTail(head, 30);
    printForward(head);   // 10 -> 20 -> 30 -> NULL
    printReverse(head);   // 30 -> 20 -> 10 -> NULL

    head = insertAtHead(head, 5);
    printForward(head);   // 5 -> 10 -> 20 -> 30 -> NULL

    head = deleteAtHead(head);
    printForward(head);   // 10 -> 20 -> 30 -> NULL

    return 0;
}
```

---

## 11. Quick Recap Table

| Operation | Time Complexity | Why |
| --- | --- | --- |
| Insert at head | O(1) | direct access via `head` |
| Insert at tail | O(n) | must traverse to find the last node — no shortcut without a separately stored `tail` |
| Delete at head | O(1) | direct access via `head` |
| Delete at tail | O(n) | must traverse to reach the last node; unlinking itself is simple via `prev` |
| Delete a given node (pointer already in hand) | **O(1)** | the real, honest advantage of a DLL — no search needed to find "the node before it" |
| Traverse backward | O(n) to reach the end, then O(n) back | only possible at all because of `prev` — a singly linked list cannot do this in any complexity |

---

## 12. Practice Questions for Students

1. Trace through `insertAtHead` step by step for a doubly linked list `20 <-> 30 <-> NULL`, inserting `10` at the head. Draw the before/after diagram, showing both `prev` and `next` pointers.
2. Why does `deleteNode` in Section 7 check `nodeToDelete->prev != nullptr` and `nodeToDelete->next != nullptr` separately, instead of always doing `nodeToDelete->prev->next = ...`? What breaks if you skip those checks?
3. Write a function `Node* insertBefore(Node* head, Node* targetNode, int value)` that inserts a new node directly before a given node, using `prev`/`next` re-linking — no traversal from `head` needed, since `targetNode` is handed to you directly. What is its time complexity, and why?
4. A singly linked list uses less memory per node. Give one real-world scenario where you'd deliberately choose a singly linked list over a doubly linked list, despite losing backward traversal.
5. True or False, with justification: "A doubly linked list always inserts at the tail in O(1) time." Explain what would need to be true for this statement to actually hold.

---