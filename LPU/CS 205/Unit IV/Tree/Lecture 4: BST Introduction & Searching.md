# Lecture 4: BST: Introduction & Searching

Status: Done

# 🎯Part 1: The BST Property and Why It Matters

---

## Quick Recap Before We Begin

In Unit I, we met **binary search**: on a sorted array, one look at the middle rules out half the data, so searching costs O(log n). We also saw its price. The array must stay sorted, so inserting or deleting costs O(n) because elements have to shift. A linked list gives the opposite trade: cheap insertion, but searching walks node by node in O(n).

In Lecture 3 we saw that the in-order traversal of a tree visits `L N R`, and we promised that for one special kind of tree this produces **sorted output**. Today we meet that tree. A **Binary Search Tree** adds one rule to an ordinary binary tree, and that rule lets us **discard a whole subtree with a single comparison**, the same trick as binary search, on a linked structure that can grow cheaply.

This lecture has two parts:

- **Part 1** (this note): the BST property, how to recognize a valid BST, and how its shape depends on insertion order.
- **Part 2**: searching (recursive and iterative), minimum and maximum, validating a BST, complexity, and a comparison with arrays and linked lists.

---

## 1. The Problem a BST Solves

Look at what each structure we know offers for a **dynamic collection** that must support search, insert, and delete:

| Structure | Search | Insert | Delete |
| --- | --- | --- | --- |
| Unsorted array | O(n) | O(1) at the end | O(n) |
| Sorted array | **O(log n)** (binary search) | O(n) (shifting) | O(n) (shifting) |
| Linked list | O(n) | O(1) at the head | O(n) (to find the node) |

No row is fast across all three operations. We want a structure that keeps **search at O(log n)** like the sorted array while making **insertion and deletion cheap** like a linked list, by changing a few pointers instead of shifting elements.

> A **Binary Search Tree (BST)** achieves O(log n) for search, insert and delete, **when the tree is balanced**.
> 

The condition at the end matters, and we will see exactly why in Section 5.

---

## . The BST Property

> **A binary search tree is a binary tree in which, for every node:**
> 
> 1. **every value in its left subtree is *smaller* than the node's value,**
> 2. **every value in its right subtree is *larger* than the node's value, and**
> 3. **its left and right subtrees are themselves binary search trees.**

In this course, we assume **distinct values** (no duplicates), as we did for searching in Unit I. Section 9 of Part 2 says what changes if duplicates are allowed.

### An Example

```
              8
            /   \
           3     10
          / \      \
         1   6      14
            / \     /
           4   7   13
```

Check the property node by node:

| Node | Left subtree | Right subtree | Property holds? |
| --- | --- | --- | --- |
| `8` | `3, 1, 6, 4, 7` (all < 8) | `10, 14, 13` (all > 8) | ✅ |
| `3` | `1` (< 3) | `6, 4, 7` (all > 3) | ✅ |
| `6` | `4` (< 6) | `7` (> 6) | ✅ |
| `10` | none | `14, 13` (all > 10) | ✅ |
| `14` | `13` (< 14) | none | ✅ |
| leaves `1, 4, 7, 13` | none | none | ✅ (nothing to violate) |

**Real-life analogy:** A **signpost at every junction** of a road network. At the junction marked `8`, the sign says *"house numbers below 8 turn left, house numbers above 8 turn right"*. At every later junction a new sign narrows the choice further. Whatever number you are hunting for, the signs lead you towards it, and you never need to explore the other branch.

### Why This Rule Makes Searching Fast

Suppose you are looking for `7` and you stand at the root `8`. Since `7 < 8`, and **everything** in the right subtree is larger than 8, the target **cannot** be in the right subtree. One comparison removes `10, 14, 13` from consideration, along with the entire branch beneath them. This is the halving idea from binary search (Unit I, Lecture 4, Part 1), applied to a tree instead of an array.

---

## 3. The Property Is About the *Whole Subtree*, Not Just the Children

This is the most common mistake when recognizing a BST. Checking only that each node is greater than its left child and smaller than its right child is ***not enough***.

```
          5
         / \
        3   8
       / \
      1   6
```

Look at each parent and its children:

| Node | Left child | Right child | Local check |
| --- | --- | --- | --- |
| `5` | `3` (< 5) | `8` (> 5) | ✅ |
| `3` | `1` (< 3) | `6` (> 3) | ✅ |

Every parent-child pair looks fine. But this is **not a BST**: the node `6` sits in the **left subtree of `5`**, and the rule says everything there must be smaller than `5`. Yet `6 > 5`.

Here is the consequence. Search for `6` in this tree: at the root `5`, we see `6 > 5` and go **right**. We reach `8`, then fall off the tree, and report "not found" even though `6` is in the tree. The signpost at `5` lied, because the tree broke its own promise.

### Seeing Why: Each Node Has an Allowed Range

Each node has a range of values it is allowed to hold, which gets narrower as we go down:

```
Going left from a node with value v:   the upper limit becomes v
Going right from a node with value v:  the lower limit becomes v

          5                     range for 5:   (−∞, +∞)
         / \
        3   8                   range for 3:   (−∞, 5)       range for 8: (5, +∞)
       / \
      1   6                     range for 1:   (−∞, 3)       range for 6: (3, 5)
```

Node `6` must lie between `3` and `5` (it is in the right subtree of `3`, but still within the left subtree of `5`). The value `6` is outside `(3, 5)`, so the tree is invalid.

⚠️ **A BST is defined by ranges that accumulate down the whole path, not by a single comparison with the parent.** We use exactly this idea to write a validity check in Part 2.

---

## 4. In-order Traversal of a BST Is `Sorted`

Now we can cash in the promise from Lecture 3. In-order visits the **left subtree, then node, then right subtree**. The BST property says the left subtree holds smaller values and the right subtree holds larger values. So at every node, in-order produces *"everything smaller, then me, then everything larger"*, and that holds at every level of the tree.

For our example tree:

```
              8
            /   \
           3     10
          / \      \
         1   6      14
            / \     /
           4   7   13

In-order:   1  3  4  6  7  8  10  13  14        ← **sorted**, ascending
```

Compare with the invalid tree from Section 3:

```
In-order of the invalid tree:   1  3  6  5  8        ← not sorted (6 comes before 5)
```

This gives an exact test:

> **A binary tree (with distinct values) is a BST if and only if its in-order traversal is strictly increasing.**
> 

### Why This Matters

- **Sorting for free.** Insert values one by one into a BST, then read them in-order, and the output is sorted. (This is a real sorting method, called tree sort.)
- **A validity check.** Run the in-order traversal and verify each value is greater than the previous one.
- **Connection to Unit I.** The in-order traversal turns the tree back into the sorted array that binary search needed, so a BST can be viewed as a sorted array that has been folded into a tree.

⚠️ **The converse needs care with the word "strictly".** With distinct values, in-order must be strictly increasing. A non-strict check (allowing equal neighbours) would accept some invalid trees, and only makes sense if duplicates are deliberately allowed.

---

## 5. The Same Values, Very Different Trees

One set of values can form many different BSTs, depending on **the order in which the values are inserted**. (We write the insertion code in Lecture 5. For now, just read the shapes.)

The rule used: the first value becomes the root. Each later value starts at the root and goes left if smaller, right if larger, until it finds an empty spot.

Take the keys `1, 2, 3, 4, 5, 6, 7`:

```
Inserted in the order  4, 2, 6, 1, 3, 5, 7:

            4
          /   \
         2     6
        / \   / \
       1   3 5   7

   a perfect tree: height 2
```

```
Inserted in the order  1, 2, 3, 4, 5, 6, 7  (already sorted):

   1
    \
     2
      \
       3
        \
         4
          \
           5
            \
             6
              \
               7

   a right-skewed tree: height 6
```

Both are valid BSTs holding exactly the same seven values. Their in-order traversals are identical (`1 2 3 4 5 6 7`), but their **shapes** are completely different.

This ties directly back to the shapes from Lecture 1:

| Insertion order | Resulting shape | Height | Searching costs |
| --- | --- | --- | --- |
| Balanced mix of small and large values | balanced / perfect | about `log₂ n` | about `log₂ n` steps |
| Already sorted (ascending or descending) | **degenerate** (a chain) | `n − 1` | up to `n` steps |

**Real-life analogy:** Filing letters into an alphabetical cabinet. If letters arrive in a random order, the cabinet fills up evenly. If they arrive already sorted, every new letter goes after the previous one, and you end up with one long chain, and finding anything means walking down the whole chain.

⚠️ **A BST does not balance itself.** Its efficiency depends on the shape, and the shape depends on the data it receives. Sorted input is the worst case, and ironically, it is the most common kind of input in real programs (IDs, timestamps, alphabetical lists). Trees that rebalance themselves, such as AVL trees and red-black trees, exist for this reason. They are beyond this syllabus, and we only note that the problem is real.

---

## 6. Part 1 Recap

| Concept | Meaning | Key point |
| --- | --- | --- |
| The gap BST fills | sorted arrays search fast but insert slowly; linked lists insert fast but search slowly | BST: O(log n) for all three, when balanced |
| BST property | left subtree < node < right subtree, at every node, recursively | distinct values assumed |
| Signpost analogy | each node tells you which way to go | one comparison discards a whole subtree |
| The whole-subtree trap | checking only parent and children is not enough | each node has an allowed range that accumulates down the path |
| Allowed ranges | going left caps the upper limit, going right raises the lower limit | root range is `(−∞, +∞)` |
| In-order of a BST | strictly increasing (sorted) | a binary tree is a BST iff its in-order is strictly increasing |
| Shape depends on insertion order | the same values can give a perfect tree or a chain | sorted input gives a degenerate tree |
| No self-balancing | a plain BST can degrade to O(n) | balanced variants exist beyond this syllabus |

**The single biggest idea from this part:** the BST property is a promise about **every value in an entire subtree**, and that promise is exactly what lets one comparison discard a whole branch. The same promise makes the in-order traversal sorted, and it also means the **shape** of the tree, which depends on the order of insertion, decides how fast everything else is.

# 🎯Part 2: Searching, Minimum & Maximum, Validation and Complexity

---

## Quick Recap Before We Continue

In Part 1 we defined the BST: for every node, **everything in the left subtree is smaller, and everything in the right subtree is larger**. We saw that checking only the immediate children is not enough, since the rule applies to whole subtrees. We also saw that the in-order traversal is sorted, and that the shape of the tree depends on the insertion order.

Now we use the property for its main purpose: **searching**.

---

## 7. Searching in a BST

### The Idea

Start at the root and compare the **target** with the current node's value:

| Comparison | What it tells us | Action |
| --- | --- | --- |
| `target == node's value` | found it | **stop and return** this node |
| `target < node's value` | the target can only be in the **left** subtree | go **left** |
| `target > node's value` | the target can only be in the **right** subtree | go **right** |
| we reach `nullptr` | there is nowhere left to look | the target is **not present** |

Each step discards **an entire subtree** and moves exactly one level down.

**Real-life analogy:** Using the signposts from Section 2. At every junction, the sign sends you one way and rules out the other. You never backtrack, and you never visit both directions at one junction.

### Walking Through a Successful Search: target = 7

```
              8      ← 7 < 8, go LEFT   (the whole right subtree is ruled out)
            /
           3         ← 7 > 3, go RIGHT  (node 1 is ruled out)
            \
             6       ← 7 > 6, go RIGHT  (node 4 is ruled out)
              \
               7     ← 7 == 7  FOUND
```

Four nodes were looked at (`8, 3, 6, 7`) out of nine. The nodes `1, 4, 10, 14, 13` were never visited.

### Walking Through an Unsuccessful Search: target = 5

```
              8      ← 5 < 8, go LEFT
            /
           3         ← 5 > 3, go RIGHT
            \
             6       ← 5 < 6, go LEFT
            /
           4         ← 5 > 4, go RIGHT
            \
             ∅       ← the right child of 4 is nullptr  →  NOT FOUND
```

This mirrors the "`high` crossed `low`" ending of binary search in Unit I. The search space has become empty. In a tree, an empty search space is simply a `nullptr`.

### The Recursive Code

```cpp
TreeNode* searchBST(TreeNode* root, int target) {
    if (root == nullptr || root->data == target) {
        return root;                                  // not found (nullptr) or found
    }
    if (target < root->data) {
        return searchBST(root->left, target);         // go left
    }
    return searchBST(root->right, target);            // go right
}
```

The function returns a **pointer to the node** holding the target, or **`nullptr`** if the target is absent. This plays the role of "the index, or `-1`" from Unit I's searching contract. A pointer to the node is more useful than a bool, because the caller can immediately read or change it.

Notice how the first line packs two cases together:

| Condition | What is returned | Meaning |
| --- | --- | --- |
| `root == nullptr` | `nullptr` | the target is absent |
| `root->data == target` | `root` | the target was found |

⚠️ **The `root == nullptr` test must come first in the `||`.** C++ stops evaluating an `||` as soon as the left side is true (short-circuiting, as in Unit I's insertion sort condition). If the order were reversed, `root->data` would be read on a `nullptr` and the program would crash.

⚠️ **Each branch needs `return`.** As in recursive binary search (Unit I, Lecture 4, Part 3), writing `searchBST(root->left, target);` without `return` computes the answer and then throws it away.

### The Iterative Code

The recursion above never does any work after the recursive call returns (it just passes the answer up). That means a loop can replace it, with a walking pointer, just like the `temp` pointer in the linked list traversal:

```cpp
TreeNode* searchBST(TreeNode* root, int target) {
    TreeNode* curr = root;                            // walking pointer; root stays untouched
    
    while (curr != nullptr && curr->data != target) {
        if (target < curr->data) {
            curr = curr->left;
        } else {
            curr = curr->right;
        }
    }
    
    return curr;                                      // found node, or nullptr
}
```

The loop ends in one of two ways, and both are handled by the single `return curr;`:

- `curr->data == target`: `curr` points at the found node.
- `curr == nullptr`: we fell off the tree, and `nullptr` means not found.

### Recursive vs Iterative

|  | Recursive | Iterative |
| --- | --- | --- |
| Code | shorter, mirrors the definition | a simple loop |
| Time | O(h) | O(h) |
| Extra space | **O(h)** (call stack) | **O(1)** |
| Risk | stack overflow on a very deep tree | none |

For searching, the iterative version is usually preferred, because the extra stack space buys nothing.

### The Link to Binary Search on an Array

The two searches are the same algorithm in different clothes:

|  | Binary search (Unit I) | BST search |
| --- | --- | --- |
| The search space | `low … high` | the current subtree |
| What we compare against | `arr[mid]` | `root->data` |
| Target is larger | `low = mid + 1` | go to `root->right` |
| Target is smaller | `high = mid - 1` | go to `root->left` |
| Space is empty | `low > high` | `root == nullptr` |
| Halves the space each step? | always | **only if the tree is balanced** |
| Needs a `mid` computation | yes (and its overflow trap) | no, the tree has already chosen the "middle" |
| Cheap to insert and delete | no, O(n) shifting | yes, a few pointer changes (Lecture 5) |

---

## 8. Minimum and Maximum

The BST property gives these almost for free.

- **The minimum** is the **leftmost** node: smaller values are always to the left, so keep going left until you can't.
- **The maximum** is the **rightmost** node.

```
              8
            /   \
           3     10
          / \      \
         1   6      14
            / \     /
           4   7   13

Minimum: 8 → 3 → 1          (stop: 1 has no left child)   → 1
Maximum: 8 → 10 → 14        (stop: 14 has no right child) → 14
```

```cpp
TreeNode* findMin(TreeNode* root) {
    if (root == nullptr) return nullptr;
    
    TreeNode* curr = root;
    while (curr->left != nullptr) {
        curr = curr->left;
    }
    
    return curr;
}

TreeNode* findMax(TreeNode* root) {
    if (root == nullptr) return nullptr;
    
    TreeNode* curr = root;
    while (curr->right != nullptr) {
        curr = curr->right;
    }
    
    return curr;
}
```

**Time complexity:** O(h). **Space complexity:** O(1).

⚠️ **The minimum is not necessarily a leaf.** If the leftmost node has a right child, it is still the minimum, and it is not a leaf. For example, a node `2` with a right child `3` and no left child is the smallest value in its subtree, even though it has a child.

⚠️ **The maximum of the left subtree and the minimum of the right subtree are special.** They are the **in-order predecessor** and **in-order successor** of a node: the values just before and just after it in sorted order. Lecture 5 relies on exactly this when we delete a node that has two children.

---

## 9. Duplicates

Everything above assumed **distinct values**. If duplicates are allowed, the "smaller on the left, larger on the right" rule says nothing about an equal value, so you must choose a convention and keep to it:

| Option | How | Consequence |
| --- | --- | --- |
| Reject duplicates | ignore a value that already exists | the BST is a **set** |
| Store a count | each node keeps a `count` field, incremented on a repeat | one node per distinct value |
| Fixed side | put equal values always in the left subtree (or always in the right) | in-order is **non-decreasing**, not strictly increasing |

⚠️ **Whichever side you choose, use it everywhere.** A search or insertion that sends equal values left must be paired with a validity check that allows equal values on the left, or valid trees will be rejected.

---

## 10. Validating a BST

We found two ways to recognize a BST: the in-order traversal is strictly increasing (Section 4), and every node lies within an accumulated range (Section 3). The range method is the standard recursive check.

### The Idea

Pass down a **lower limit** and an **upper limit** for the current node:

- At the root, the limits are `−∞` and `+∞`.
- Moving to the **left child**, the upper limit becomes the current node's value.
- Moving to the **right child**, the lower limit becomes the current node's value.
- A node is invalid if its value is not **strictly between** the limits.

For the invalid tree from Section 3:

```
          5                       limits (−∞, +∞)  → 5 is fine
         / \
        3   8                     3: (−∞, 5) fine      8: (5, +∞) fine
       / \
      1   6                       1: (−∞, 3) fine
                                  6: (3, 5)  → 6 is NOT less than 5  ❌ **invalid**
```

### The Code

```cpp
bool isValid(TreeNode* root, long long low, long long high) {
    if (root == nullptr) return true;                       // an empty tree is a valid BST

    if (root->data <= low || root->data >= high) {
        return false;                                       // outside the allowed range
    }
    return isValid(root->left, low, root->data)             // left: upper limit shrinks
        && isValid(root->right, root->data, high);          // right: lower limit rises
}

bool isValidBST(TreeNode* root) {
    return isValid(root, LLONG_MIN, LLONG_MAX);             // needs <climits>
}
```

**Why `long long` for the limits?** Node values are `int`, and a node could legitimately hold `INT_MIN` or `INT_MAX`. If the initial limits were `INT_MIN` and `INT_MAX`, those legitimate values would be rejected as "not strictly inside". A wider type lets the starting limits sit safely outside every possible `int`. It is the same idea as the overflow trap in Unit I, Lecture 4: choose a type with enough room.

**Time complexity:** O(n), since every node is checked once. **Space complexity:** O(h) for the recursion stack.

⚠️ **The base case returns `true` for an empty tree.** An empty subtree can never violate the property, so reaching `nullptr` means the path so far was valid.

⚠️ **Comparing a node only with its parent's value is the Section 3 trap again.** The limits are what carry information from ancestors far above, so they must be passed down, not recomputed locally.

⚠️ **The alternative is the in-order test.** Run an in-order traversal, remembering the previously visited value, and fail the moment a value is not greater than the previous one. It is equally correct and O(n), and it is a good way to double-check the range method.

---

## 11. Complexity of BST Search

### Time Complexity: O(h)

Every step of the search moves **one level down**, and each step does a constant amount of work (one comparison). The search can therefore never take more steps than the **height** of the tree, plus one for the final `nullptr`. So the cost is **O(h)**, and what `h` is depends on the shape:

| Tree shape | Height `h` | Search cost |
| --- | --- | --- |
| Balanced / perfect | about `log₂ n` | **O(log n)** |
| Degenerate (chain) | `n − 1` | **O(n)** |

| Case | When it happens | Time |
| --- | --- | --- |
| **Best** | the target is the root | O(1) |
| **Worst** | the target is at the deepest level, or absent | O(h) |

### Why the Shape Decides Everything

Take `n = 1,000,000` values:

| Shape | Worst-case steps |
| --- | --- |
| Balanced BST | about **20** (`log₂ 1,000,000`) |
| Degenerate BST | up to **1,000,000** (as bad as linear search) |

The same pairing as binary search against linear search, from Unit I, Lecture 4. The difference is that in an array, sorted order guarantees the halving. In a BST, only a **balanced shape** does.

### Space Complexity

- **Iterative search:** O(1), with just the walking pointer.
- **Recursive search:** O(h), one stack frame per level visited.

The same applies to `findMin`, `findMax` (iterative, O(1) space) and `isValidBST` (recursive, O(h) space).

---

## 12. BST vs Sorted Array vs Linked List

| Operation | Sorted array | Sorted linked list | BST (balanced) | BST (degenerate) |
| --- | --- | --- | --- | --- |
| Search | O(log n) | O(n) | **O(log n)** | O(n) |
| Insert | O(n) (shifting) | O(n) (find the spot, then O(1) to link) | **O(log n)** | O(n) |
| Delete | O(n) (shifting) | O(n) (find the node) | **O(log n)** | O(n) |
| Minimum / maximum | O(1) | O(1) / O(n) | O(log n) | O(n) |
| Sorted output | already sorted | already sorted | O(n) in-order traversal | O(n) in-order traversal |
| Extra memory per element | none | 1 pointer | 2 pointers | 2 pointers |
| Random access by index | O(1) | O(n) | not supported | not supported |

⚠️ **A balanced BST is best when the data changes and must also be searched.** For data that is built once and only searched, a sorted array with binary search is simpler and uses less memory. For data that is mostly inserted and rarely searched, a plain linked list may be enough. As always in this course, the right structure depends on which operations you need to be cheap.

---

## 13. Full Working Example

```cpp
#include <iostream>
#include <vector>
#include <climits>
using namespace std;

struct TreeNode {
    int data;
    TreeNode* left;
    TreeNode* right;

    TreeNode(int value) {
        data = value;
        left = nullptr;
        right = nullptr;
    }
};

TreeNode* searchBST(TreeNode* root, int target) {
    TreeNode* curr = root;
    while (curr != nullptr && curr->data != target) {
        if (target < curr->data) {
            curr = curr->left;
        } else {
            curr = curr->right;
        }
    }
    return curr;
}

TreeNode* findMin(TreeNode* root) {
    if (root == nullptr) return nullptr;
    TreeNode* curr = root;
    while (curr->left != nullptr) curr = curr->left;
    return curr;
}

TreeNode* findMax(TreeNode* root) {
    if (root == nullptr) return nullptr;
    TreeNode* curr = root;
    while (curr->right != nullptr) curr = curr->right;
    return curr;
}

void inorder(TreeNode* root, vector<int> &ans) {
    if (root == nullptr) return;
    inorder(root->left, ans);
    ans.push_back(root->data);
    inorder(root->right, ans);
}

bool isValid(TreeNode* root, long long low, long long high) {
    if (root == nullptr) return true;
    if (root->data <= low || root->data >= high) return false;
    return isValid(root->left, low, root->data)
        && isValid(root->right, root->data, high);
}

bool isValidBST(TreeNode* root) {
    return isValid(root, LLONG_MIN, LLONG_MAX);
}

int main() {
    // the BST from Section 2
    TreeNode* root = new TreeNode(8);
    root->left = new TreeNode(3);
    root->right = new TreeNode(10);
    root->left->left = new TreeNode(1);
    root->left->right = new TreeNode(6);
    root->left->right->left = new TreeNode(4);
    root->left->right->right = new TreeNode(7);
    root->right->right = new TreeNode(14);
    root->right->right->left = new TreeNode(13);

    vector<int> sorted;
    inorder(root, sorted);
    for (int x : sorted) cout << x << " ";                  // 1 3 4 6 7 8 10 13 14
    cout << endl;

    cout << (searchBST(root, 7) != nullptr) << endl;        // 1  (found)
    cout << (searchBST(root, 5) != nullptr) << endl;        // 0  (not found)
    cout << findMin(root)->data << endl;                    // 1
    cout << findMax(root)->data << endl;                    // 14
    cout << isValidBST(root) << endl;                       // 1

    // the invalid tree from Section 3
    TreeNode* bad = new TreeNode(5);
    bad->left = new TreeNode(3);
    bad->right = new TreeNode(8);
    bad->left->left = new TreeNode(1);
    bad->left->right = new TreeNode(6);                     // 6 is in the left subtree of 5

    cout << isValidBST(bad) << endl;                        // 0

    return 0;
}
```

---

## 14. Common Traps Checklist

| Pitfall | What happens | Fix |
| --- | --- | --- |
| Validating by comparing each node only with its parent or children | invalid trees such as the Section 3 example are accepted | pass down the accumulated lower and upper limits |
| Using `INT_MIN` / `INT_MAX` as the starting limits | a node that holds one of those values is wrongly rejected | use `long long` limits (`LLONG_MIN`, `LLONG_MAX`) |
| `root->data == target` tested before `root == nullptr` | reading a field of `nullptr` crashes | check `nullptr` first |
| Missing `return` before the recursive search call | the answer is computed and lost | `return searchBST(...)` |
| Going the wrong way (smaller goes right) | the search misses values that are present | smaller goes left, larger goes right |
| Moving `root` itself in the iterative version | the original pointer is lost | walk with a separate `curr` pointer |
| Assuming a BST is always O(log n) | sorted input produces a chain with O(n) search | the cost is O(h), and `h` depends on the shape |
| Assuming the minimum is a leaf | a node with only a right child can be the minimum | follow `left` pointers until none remain |
| Expecting sorted in-order from any binary tree | wrong for non-BSTs | sorted in-order is a property of BSTs only |
| Mixing duplicate conventions | valid trees rejected, or searches fail | choose one rule and apply it everywhere |
| Using `<` instead of `<=` in the validity check on a duplicates-allowed tree | rejects legitimate equal values | match the check to the duplicate convention |

---

## 15. Part 2 Recap

| Concept | Meaning | Key point |
| --- | --- | --- |
| BST search | compare, then go left, go right, or stop | each step discards a whole subtree |
| Result | pointer to the node, or `nullptr` | the counterpart of "index or `-1`" |
| Recursive search | base case `nullptr` or match, then recurse on one side | O(h) stack space |
| Iterative search | a walking `curr` pointer in a loop | O(1) space; preferred |
| Binary search link | `low/high/mid` becomes "the current subtree" | halving only when the tree is balanced |
| Minimum / maximum | the leftmost / rightmost node | O(h); the minimum need not be a leaf |
| Predecessor / successor | max of the left subtree / min of the right subtree | used for deletion in Lecture 5 |
| Duplicates | reject, count, or fix a side | choose one rule and keep it consistent |
| Validation | pass `(low, high)` down; the root starts at `(−∞, +∞)` | `long long` limits; O(n) time |
| Search time | **O(h)** | O(log n) balanced, O(n) degenerate |
| Search space | O(1) iterative, O(h) recursive |  |
| Comparison | balanced BST is O(log n) for search, insert and delete | sorted array: fast search, slow insert; linked list: the reverse |

**The single biggest idea from this part:** searching a BST is binary search on a tree, and **the cost is the height of the tree, not the number of nodes**. A balanced BST makes the height `log n`, and a chain makes it `n`, so the shape from Lecture 1 decides whether the structure is a fast one or merely a slow linked list.

---

## 16. Full Lecture Summary

| Concept | Meaning | Key point |
| --- | --- | --- |
| Why BST | search, insert and delete all fast when balanced | arrays and linked lists each fail one of these |
| BST property | left subtree < node < right subtree, at every node | applies to whole subtrees, not just children |
| Allowed range | going left caps the upper limit, going right raises the lower limit | explains why local checks fail |
| In-order of a BST | strictly increasing | iff test for a BST (distinct values) |
| Shape and insertion order | the same values give different trees | sorted input gives a chain |
| Search | compare, then left, right or stop | pointer to the node, or `nullptr` |
| Recursive vs iterative | O(h) space vs O(1) space | prefer iterative |
| Min / max | leftmost / rightmost node | not necessarily leaves |
| Validation | accumulated `(low, high)` limits, `long long` bounds | O(n) time, O(h) space |
| Duplicates | convention required | course assumes distinct values |
| Complexity | **O(h)**: O(log n) balanced, O(n) degenerate | the height decides everything |

**The single biggest idea from this lecture:** one rule, *smaller on the left, larger on the right, at every node*, turns a plain binary tree into a structure where a single comparison discards an entire subtree. That gives binary-search speed on a structure that, unlike a sorted array, can grow by changing pointers. The speed depends entirely on staying balanced, which a plain BST does not guarantee.