# Lecture 2: Memory Representation of Binary Trees

Status: Done

# 🎯Part 1: Linked Representation

---

## Quick Recap Before We Begin

In Lecture 1 we met the binary tree: a root with at most two subtrees, a **left** one and a **right** one. We named its shapes (full, complete, perfect, balanced, degenerate) and its extended form, where every empty child slot becomes an external node.

Everything so far was drawn on paper. Today we answer the question every data structure eventually faces: **how does this actually sit in memory?** The syllabus names two ways:

- **Linked representation** (Part 1): nodes connected by pointers, like the linked lists of Unit II.
- **Sequential representation** (Part 2): the tree stored in a plain array, using index arithmetic.

---

## 1. The Problem: A Hierarchy in a Straight Line of Memory

Computer memory is **linear**: a long row of numbered addresses. A tree is **hierarchical**: nodes at different levels, with branching. To store a tree, we have to **encode the hierarchy using only addresses and indices**.

There are two natural ways to do that:

|  | Linked representation | Sequential representation |
| --- | --- | --- |
| Idea | each node stores the **addresses** of its children | each node's **position (index)** tells you where its children are |
| Built on | pointers and dynamic memory (Unit II) | arrays (Unit I) |
| Layout in memory | nodes scattered anywhere | one contiguous block |

This is the same split we saw between linked lists and arrays, and the same trade-offs return in Section 14.

---

## 2. The Tree Node

In a singly linked list, a node held `data` and one `next` pointer. A binary tree node has **two** outgoing connections, so it holds `data` and **two** pointers: `left` and `right`.

```
      ┌──────────┬──────────┬──────────┐
      │   left   │   data   │  right   │
      └──────────┴──────────┴──────────┘
         │                        │
         ▼                        ▼
   address of the           address of the
   left child               right child
```

**Real-life analogy:** Recall the treasure hunt from Unit II. Each clue told you where the *next* clue was. In a tree, each clue carries **two slips of paper**: one says "left child is hidden at this address" and the other says "right child is hidden at this address". A slip that says "nothing here" means that child does not exist.

⚠️ **This looks like a doubly linked list node, but the meaning is different.** A doubly linked list node has `prev` and `next`, which point to neighbours in a **line**. A tree node has `left` and `right`, which point to **children one level down**. Neither pointer ever points back up.

### The Code

![image.png](Lecture%202%20Memory%20Representation%20of%20Binary%20Trees/image.png)

```cpp
struct TreeNode {
    int data;
    TreeNode* left;
    TreeNode* right;

    TreeNode(int value) {          // constructor: runs when a node is created
        data = value;
        left = nullptr;            // a new node has no children yet
        right = nullptr;
    }
};
```

Like the `Node` in Unit II, `TreeNode` contains pointers to its own type, so it is a **self-referential structure**.

The **constructor** `TreeNode(int value)` is the new piece. In Unit II we wrote `new Node()` and then filled the fields in by hand. Here, writing `new TreeNode(5)` allocates the node **and** initializes it in one step:

```
new TreeNode(5):
   data  = 5
   left  = nullptr
   right = nullptr
```

⚠️ **Why `left` and `right` start as `nullptr`.** A brand-new node is not connected to anything. Leaving the pointers uninitialized would give them garbage addresses, and the first time code tried to follow one, the program would crash or misbehave. `nullptr` is the "nothing here" slip from the analogy, and it is exactly the **empty subtree** from Lecture 1's recursive definition.

---

## 3. A Tree in Memory

Take this binary tree:

```
          5
        /   \
       6     7
      / \
     8   9
        /
       1
```

Each node is created separately, and the operating system places it **wherever memory is free**. The nodes are not stored near each other. Only the pointers tie them together:

```
 Address    left      data    right
 ───────────────────────────────────
 0x1000     0x2000      5     0x1800        ← root (5)
 0x2000     0x3100      6     0x2600        ← 6
 0x1800     nullptr     7     nullptr       ← 7   (leaf)
 0x3100     nullptr     8     nullptr       ← 8   (leaf)
 0x2600     0x4200      9     nullptr       ← 9   (left child only)
 0x4200     nullptr     1     nullptr       ← 1   (leaf)
```

Drawn as boxes and arrows:

![image.png](Lecture%202%20Memory%20Representation%20of%20Binary%20Trees/image%201.png)

### The `root` Pointer Is the Only Entry Point

Just as `head` was the only way into a linked list, a single pointer `root` is the only way into a tree. From it we can reach every node by following `left` and `right` pointers. If you lose `root`, you lose the entire tree.

**An empty tree is simply `root == nullptr`.** This is the "empty" case from Lecture 1's recursive definition, written in code.

---

## 4. Building the Tree by Hand

We build the tree from Section 3 one node at a time, using plain pointers, just as we built linked lists by hand in Unit II.

```cpp
TreeNode* root = new TreeNode(5);               // step 1: create the root

root->left  = new TreeNode(6);                  // step 2: 5's left child
root->right = new TreeNode(7);                  // step 3: 5's right child

root->left->left  = new TreeNode(8);            // step 4: 6's left child
root->left->right = new TreeNode(9);            // step 5: 6's right child

root->left->right->left = new TreeNode(1);      // step 6: 9's left child
```

### Reading `root->left->right->left`

Read the chain **from left to right**, one hop at a time. Each `->` means "follow this pointer to the node it points to":

```
root                        → the node 5
root->left                  → follow 5's left pointer   → the node 6
root->left->right           → follow 6's right pointer  → the node 9
root->left->right->left     → follow 9's left pointer   → the node 1's slot
```

The expression on the left side of `=` identifies **which pointer to fill in**, and the right side supplies the new node's address.

### The Tree Growing

```
After step 1:        After steps 2–3:        After steps 4–5:        After step 6:

     5                    5                       5                       5
                         / \                     / \                     / \
                        6   7                   6   7                   6   7
                                               / \                     / \
                                              8   9                   8   9
                                                                          /
                                                                         1
```

⚠️ **Create the parent before the child.** The line `root->left->left = ...` follows `root->left`, which must already point to a real node. Running step 4 before step 2 would try to follow `nullptr->left`, a crash.

⚠️ **Each `new` creates an independent node.** Assigning `root->left = new TreeNode(6)` stores the new node's **address** in the pointer. It does not copy a node into the parent. This is why the nodes can live anywhere in memory.

### A Common Shorthand

For small trees, we often build them in nested form, with the variable names carrying the structure:

```cpp
TreeNode* root  = new TreeNode(5);
TreeNode* six   = new TreeNode(6);
TreeNode* seven = new TreeNode(7);

root->left  = six;
root->right = seven;
```

The tree is identical. Only the way we hold the addresses differs.

---

## 5. Counting the Pointers: A Link to Extended Trees

Every node has **2 pointers**, so a tree with `n` nodes stores `2n` pointers in total. How many of them point to real nodes?

```
Every node except the root has exactly one parent pointing at it
→ n − 1 pointers are in use (point to real nodes)

Pointers that are nullptr  =  2n − (n − 1)  =  n + 1
```

For our 6-node tree: `2 × 6 = 12` pointers, `5` in use, `7` are `nullptr`. You can verify this in the table in Section 3.

This `n + 1` is exactly the count of **external nodes** from the extended binary tree in Lecture 1. That is not a coincidence:

> **Every `nullptr` in the linked representation is an external node of the extended tree.**
> 

The extended tree simply draws the "nothing here" slips as visible squares.

⚠️ **This is a lot of unused pointers.** More than half of all pointers in a binary tree are `nullptr`. We pay that price in memory for the flexibility of pointers, which matters in the comparison in Section 14.

---

## 6. Our First Recursive Function on a Tree: Counting Nodes

Lecture 1 defined a binary tree as *empty, or a root plus a left subtree and a right subtree*. A function that follows that definition almost writes itself. Here is the smallest useful example, counting the nodes:

```cpp
int countNodes(TreeNode* root) {
    if (root == nullptr) {                  // empty tree: no nodes
        return 0;
    }
    return 1 + countNodes(root->left)       // this node + nodes in the left subtree
             + countNodes(root->right);     // + nodes in the right subtree
}
```

The function takes only a `TreeNode* root`, with no wrapper class, like every function in this course.

```cpp
cout << countNodes(root);    // 6, for the tree we built
```

How the code mirrors the definition:

| Part of the definition | Part of the code |
| --- | --- |
| "the tree is empty" | `root == nullptr` → return `0` (the **base case**) |
| "a root" | the `1` |
| "a left subtree and a right subtree, each a binary tree" | the two recursive calls on `root->left` and `root->right` |

**Time complexity:** **O(n)**, because every node is visited exactly once.

**Space complexity:** **O(h)**, where `h` is the height of the tree. The recursion goes as deep as the longest path, so at most `h + 1` calls wait on the stack at once. That is `O(log n)` for a balanced tree and `O(n)` for a degenerate one, a first practical reason the shapes from Lecture 1 matter.

⚠️ **The `nullptr` check must come first.** Without it, the call on a leaf's missing child would try to read `nullptr->left`, which crashes. Every recursive tree function in this unit begins with this check.

⚠️ **Freeing the tree.** Each `new TreeNode` must eventually be freed with `delete`, or the memory leaks, just as with linked lists. Freeing a tree has to remove the children **before** the parent (otherwise we lose the addresses of the children). We'll write that function in Lecture 3, once we have the traversal order that makes it natural.

---

## 7. Part 1 Recap

| Concept | Meaning | Key point |
| --- | --- | --- |
| Memory vs tree | memory is a line, a tree is a hierarchy | the hierarchy must be encoded |
| Linked representation | each node stores the addresses of its children | built on pointers, as in Unit II |
| `TreeNode` | `data`, `left`, `right` | self-referential struct |
| Constructor | `TreeNode(int value)` allocates and initializes in one step | sets `left` and `right` to `nullptr` |
| `nullptr` child | "no child here" | the empty subtree from Lecture 1 |
| `root` pointer | the only entry point into the tree | lose it and the tree is lost; `nullptr` means an empty tree |
| Building a tree | `root->left = new TreeNode(...)` | create parents before children |
| Pointer chain | `root->left->right` | follow one pointer per `->`, left to right |
| Pointer count | `2n` pointers, `n − 1` in use, `n + 1` are `nullptr` | the `nullptr`s are the external nodes of the extended tree |
| `countNodes` | `1 + left count + right count`, base case `nullptr` | O(n) time, O(h) stack space |

**The single biggest idea from this part:** in the linked representation, a tree is just nodes holding two pointers each, with `nullptr` meaning "empty subtree". The code `if (root == nullptr)` is the same "empty tree" case as in our recursive definition, so every recursive tree function begins with it.

# 🎯Part 2: Sequential (Array) Representation & Comparison

---

## Quick Recap Before We Continue

In Part 1 we stored a tree with **pointers**. The nodes could sit anywhere in memory, and each node carried two addresses. The cost was `2n` pointers for `n` nodes, with `n + 1` of them being `nullptr`.

Now we store the tree **without any pointers at all**. We place the nodes in an **array** and let each node's **index** encode where its relatives are.

---

## 8. The Idea: Number the Nodes Level by Level

Number the nodes **level by level, from top to bottom, and left to right within each level**, starting at `0`. Then store the node numbered `i` in `arr[i]`.

**Real-life analogy:** Seat numbers in a theatre, numbered row by row from the front. Once you know the numbering rule, you don't need a diagram to find "the two seats in the row behind me that belong to my group". You can calculate their numbers.

Take this complete binary tree of 6 nodes:

```
Level 0:             A          index 0
                   /   \
Level 1:          B     C       index 1, 2
                 / \   /
Level 2:        D   E F         index 3, 4, 5
```

The array:

```
Index:     0     1     2     3     4     5
        ┌─────┬─────┬─────┬─────┬─────┬─────┐
arr  =  │  A  │  B  │  C  │  D  │  E  │  F  │
        └─────┴─────┴─────┴─────┴─────┴─────┘
```

The array is simply the tree **read level by level**.

---

## 9. The Index Formulas

For the node stored at index `i` (with 0-based indexing):

| Relative | Formula | Condition |
| --- | --- | --- |
| **Left child** | `2i + 1` | exists only if `2i + 1 < n` |
| **Right child** | `2i + 2` | exists only if `2i + 2 < n` |
| **Parent** | `(i − 1) / 2` (integer division) | exists only if `i > 0` |

Check these on the 6-node tree:

| `i` | Node | Parent index | Left child | Right child |
| --- | --- | --- | --- | --- |
| 0 | A | none (root) | 1 → B | 2 → C |
| 1 | B | 0 → A | 3 → D | 4 → E |
| 2 | C | 0 → A | 5 → F | 6 ≥ 6, none |
| 3 | D | 1 → B | 7 ≥ 6, none | 8 ≥ 6, none |
| 4 | E | (4−1)/2 = 1 → B | none | none |
| 5 | F | (5−1)/2 = 2 → C | none | none |

```
Following the formulas on the array:

Index:     0     1     2     3     4     5
        ┌─────┬─────┬─────┬─────┬─────┬─────┐
        │  A  │  B  │  C  │  D  │  E  │  F  │
        └──┬──┴──┬──┴──┬──┴─────┴─────┴─────┘
           │     │     │
           │     │     └──► children of C: index 5, 6  → F (6 is outside the array)
           │     └────────► children of B: index 3, 4  → D, E
           └──────────────► children of A: index 1, 2  → B, C
```

Both children of a node at index `i` sit **side by side**, at `2i + 1` and `2i + 2`.

### Why the Formulas Work

Look at where each level begins in the array:

```
Level 0 starts at index 0       (1 node)
Level 1 starts at index 1       (2 nodes)   ← 1
Level 2 starts at index 3       (4 nodes)   ← 1 + 2
Level 3 starts at index 7       (8 nodes)   ← 1 + 2 + 4
Level k starts at index 2^k − 1
```

Suppose node `i` is at level `k`, in position `p` within that level (counting from 0), so `i = (2^k − 1) + p`. Its children are in the **next level**, at positions `2p` and `2p + 1` (each earlier node in the level has already used up two slots below). So the left child is at index:

```
(2^(k+1) − 1) + 2p
 = 2·(2^k − 1) + 1 + 2p
 = 2·((2^k − 1) + p) + 1
 = 2i + 1     ✅
```

and the right child is the very next slot, `2i + 2`. The parent formula is the same relationship run backwards: `(2i + 1 − 1)/2 = i` and `(2i + 2 − 1)/2 = i` (integer division drops the half).

⚠️ **Many textbooks number from 1 instead of 0.** With 1-based indexing the formulas are **left = `2i`, right = `2i + 1`, parent = `i / 2`**. They describe the same tree, shifted by one. C++ arrays start at 0, so this course uses the 0-based formulas. If an exam question says "index starts at 1", switch formulas.

⚠️ **Always check the array bounds.** C++ does not stop you from reading `arr[8]` on a 6-element array (Unit I, Lecture 2). Before using `2i + 1` or `2i + 2`, check that it is less than `n`.

### The Formulas in Code

```cpp
int leftChild(int i)  { return 2 * i + 1; }
int rightChild(int i) { return 2 * i + 2; }
int parent(int i)     { return (i - 1) / 2; }

void printFamily(char arr[], int n, int i) {
    cout << "Node " << arr[i];

    if (i > 0) {
        cout << ", parent " << arr[parent(i)];
    }
    if (leftChild(i) < n) {
        cout << ", left " << arr[leftChild(i)];
    }
    if (rightChild(i) < n) {
        cout << ", right " << arr[rightChild(i)];
    }
    cout << endl;
}
```

Each lookup is a single arithmetic operation, so **finding a parent or child is O(1)**. In the linked version there is no pointer to the parent at all.

---

## 10. When the Tree Is *Not Complete*: Gaps and Waste

The formulas assume that **every index has a place in the tree**. If a node is missing in the middle, its slot in the array is still reserved, because the formulas would break if we squeezed it out. The missing positions are left ***empty***.

Here is the tree from Part 1 again:

```
          5                    index 0
        /   \
       6     7                 index 1, 2
      / \
     8   9                     index 3, 4
        /
       1                       index 9   (left child of 9 at index 4 → 2·4 + 1)
```

7 has no children, so indices `5` and `6` are reserved but empty. 8 has no children, so indices `7` and `8` are empty too. 1 sits at index `9`, so the array must be at least **10** long:

```
Index:     0    1     2    3     4    5    6     7    8    9
        ┌────┬────┬────┬────┬────┬────┬────┬────┬────┬────┐
arr  =  │ 5  │ 6  │ 7  │ 8  │ 9  │ ── │ ── │ ── │ ── │ 1  │
        └────┴────┴────┴────┴────┴────┴────┴────┴────┴────┘
                                  └──── **4 empty slots** ───┘

6 nodes stored, 10 slots reserved
```

### How Do We Mark an Empty Slot?

The array needs some way to say "no node here". The common choices:

| Method | How | Limitation |
| --- | --- | --- |
| **Sentinel value** | a value that can never be real data, such as `-1` or `INT_MIN` | only works when the data has a spare value |
| **Parallel boolean array** | a second array `exists[i]` that records whether slot `i` is occupied | uses extra memory |

### The Worst Case: Skewed Trees

The waste grows **exponentially** with the height. Take a right-skewed tree of just 4 nodes:

```
A                     index 0
 \
  B                   index 2   (right child of 0 → 2·0 + 2)
   \
    C                 index 6   (right child of 2 → 2·2 + 2)
     \
      D               index 14  (right child of 6 → 2·6 + 2)
```

```
Index:    0    1     2    3     4    5     6    ...   13   14
       ┌────┬────┬────┬────┬────┬────┬────┬─────┬────┬────┐
 arr = │ A  │ ── │ B  │ ── │ ── │ ── │ C  │ ... │ ── │ D  │
       └────┴────┴────┴────┴────┴────┴────┴─────┴────┴────┘

4 nodes stored, 15 slots reserved, 11 wasted
```

Recall Formula 2 from Lecture 1: a tree of height `h` can span up to `2^(h+1) − 1` positions. Reaching the deepest node forces the array to be that long, regardless of how few nodes there actually are.

| Tree shape | Nodes `n` | Array size needed | Wasted slots |
| --- | --- | --- | --- |
| Complete tree | `n` | `n` | **0** |
| Our 6-node tree from Part 1 | 6 | 10 | 4 |
| Degenerate (skewed) tree | `n` | `2^n − 1` | `2^n − 1 − n` |

For a skewed tree of only *20 nodes*, the array would need over a ***million*** slots.

⚠️ **This is why the complete tree matters.** A complete tree has no gaps (Lecture 1, Section 7), so the array is packed with zero waste and every formula works without sentinels. That is exactly why heaps are stored in arrays, and why the priority queue from Unit III is built on this representation.

⚠️ **The array has to be sized for the *deepest position*, not the node count.** Allocating `n` slots for an arbitrary tree with `n` nodes can place nodes beyond the end of the array.

---

## 11. Why `Insertion` and `Deletion` Are Awkward in an Array

In a **complete** tree, adding a node means placing it at the next free index `n`, which is O(1). Removing the last node is also O(1).

For an **arbitrary** tree, deleting a node in the middle is a problem. Its slot becomes empty, but its descendants still sit at the indices implied by the old position. Moving a whole subtree means recalculating every index inside it, which is O(size of the subtree).

In the linked representation, the same change is just reassigning one or two pointers, with no other node moving at all.

---

## 12. Linked vs Sequential: Side-by-Side Comparison

| Feature | Linked representation | Sequential (array) representation |
| --- | --- | --- |
| Built on | pointers and dynamic memory | a contiguous array |
| Memory per node | `data` + 2 pointers | `data` only |
| Wasted memory | `n + 1` null pointers (a fixed overhead) | **empty slots, which can be exponential** for skewed trees |
| Wasted memory (complete tree) | `n + 1` null pointers | **none** |
| Find a child | follow a pointer | compute `2i + 1` / `2i + 2` |
| Find the parent | **not directly possible** (no upward pointer) | compute `(i − 1) / 2`, O(1) |
| Insert / delete in the middle | change a few pointers | may need to move whole subtrees |
| Size | grows one node at a time | fixed (or must be reallocated) |
| Node addresses | scattered in memory | contiguous, cache-friendly |
| Best suited to | **any** binary tree, especially irregular or changing ones | **complete** binary trees (heaps) |

**Real-life analogy:** The linked version is like a company where each manager **keeps the phone numbers** of their two direct reports. It works for any shape of organization, but nobody can call their boss unless that number is also written down. The array version is like **numbered desks in a fixed floor plan**. If everybody sits in order, you can calculate where anyone's boss or reports sit. An irregular organization, though, leaves rows of empty desks.

### Which One Will This Course Use?

| Representation | Where it appears |
| --- | --- |
| **Linked** | traversals (Lecture 3), Binary Search Trees (Lectures 4–5): the arbitrary-shape trees we build and modify |
| **Sequential** | complete trees only, as in the heap behind the priority queue |

From now on, "a binary tree" in code means the **linked representation** with `TreeNode* root`, unless we say otherwise.

⚠️ **This is the same trade-off, one more time.** Arrays give O(1) positional access but a fixed layout, and linked nodes give flexibility at the cost of extra pointers. It is the time-space trade-off from Unit I, Lecture 1, showing up in a new setting, and the right choice again depends on the shape of the data and the operations you care about.

---

## 13. Common Traps Checklist

| Pitfall | Where it bites | The fix |
| --- | --- | --- |
| Mixing the 0-based and 1-based formulas | array representation | **0-based**: `2i+1`, `2i+2`, `(i−1)/2`. **1-based**: `2i`, `2i+1`, `i/2` |
| Using `i / 2` for the parent in 0-based indexing | parent lookup | the 0-based parent is `(i − 1) / 2`; `i / 2` is the 1-based formula |
| Reading `arr[2i + 1]` without checking `< n` | child lookup | check the bounds before every child access |
| Calling `parent(0)` | the root | the root has no parent; guard with `i > 0` |
| Sizing the array by node count for a non-complete tree | array representation | size it for the largest index reached, up to `2^(h+1) − 1` |
| Using a real data value as the "empty" marker | gap marking | use a value that can never be real data, or a separate `exists` array |
| Following `root->left` when `root` is `nullptr` | linked representation | always check `root == nullptr` first |
| Building a child before its parent exists | `root->left->left = ...` | create nodes top-down so each pointer in the chain is valid |
| Forgetting `left`/`right` start as `nullptr` | constructors | initialize both pointers, or they hold garbage addresses |
| Expecting to find a node's parent in the linked version | linked representation | there is no upward pointer; either add one or pass the parent down during recursion |
| Forgetting to `delete` nodes | linked representation | every `new` needs a matching `delete` (written in Lecture 3) |

---

## 14. Full Lecture Summary

| Concept | Meaning | Key point |
| --- | --- | --- |
| Representation problem | memory is linear, a tree is hierarchical | the hierarchy must be encoded |
| Linked representation | nodes with `data`, `left`, `right`; entry point is `root` | works for any shape; `nullptr` = empty subtree |
| Constructor | allocates and initializes a node in one step | `new TreeNode(5)` |
| Pointer count | `2n` pointers, `n + 1` of them `nullptr` | equals the external nodes of the extended tree |
| `countNodes` | `1 + left + right`, base case `nullptr` | O(n) time, O(h) stack space |
| Sequential representation | store node `i` in `arr[i]`, level by level, left to right | no pointers needed |
| Index formulas (0-based) | left `2i+1`, right `2i+2`, parent `(i−1)/2` | check `< n` and `i > 0` |
| 1-based formulas | left `2i`, right `2i+1`, parent `i/2` | same tree, shifted by one |
| Why they work | level `k` starts at index `2^k − 1` | children land at `2i+1`, `2i+2` |
| Gaps | missing nodes leave reserved, empty slots | mark with a sentinel or `exists[]` |
| Complete tree in an array | zero waste | why heaps use arrays |
| Skewed tree in an array | `n` nodes need `2^n − 1` slots | exponential waste |
| Linked vs sequential | flexible and pointer-heavy vs compact but rigid | the time-space trade-off again |
| Default for this unit | the linked `TreeNode* root` | used for traversals and BSTs |

**The single biggest idea from this lecture:** a tree can be stored by **remembering addresses** (linked) or by **computing positions** (array). Pointers handle any shape at the cost of two pointers per node, and index arithmetic costs nothing but only works without waste when the tree is **complete**. We will use the linked form for everything that follows.

---