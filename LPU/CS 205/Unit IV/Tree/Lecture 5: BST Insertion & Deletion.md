# Lecture 5: BST: Insertion & Deletion

Status: Done

# 🎯Part 1: Insertion

---

## Quick Recap Before We Begin

In Lecture 4 we defined the BST (**left subtree < node < right subtree, at every node**) and used it for searching in O(h). We drew BSTs of different shapes from the same values, and we noted that insertion order decides the shape. We never wrote the code that builds a BST. We also met the in-order successor, the minimum of the right subtree, and promised to use it for deletion.

Today we make the BST **change**. The syllabus asks for both operations, and they are very different in difficulty:

- **Part 1** (this note): **insertion**. It is search plus one pointer assignment.
- **Part 2**: **deletion**. It has three cases, and the third one is the hardest idea in the unit.

We keep using the same reference BST from Lecture 4:

```
              8
            /   \
           3     10
          / \      \
         1   6      14
            / \     /
           4   7   13
```

---

## 1. The Key Observation: A New Node Is Always a Leaf

Where should a new value go? Search for it. Follow the same left/right decisions as `searchBST`. If the value is not in the tree, the search ends by falling off the tree at a `nullptr`. That empty slot is exactly where the value belongs.

> **Insertion into a BST: search for the value, and attach the new node at the `nullptr` where the search ends.**
> 

The new node therefore always becomes a **leaf**. Nothing already in the tree moves.

**Real-life analogy:** Using the signposts from Lecture 4 to find where a **new house** belongs on a street. You follow the signs exactly as if you were looking for the house number, and you build at the spot where the signs run out. No existing house has to move.

### Why It Must Be a Leaf

Any non-empty position in the tree is already occupied. The BST property says each value has a unique allowed range (Lecture 4, Section 3). The search path narrows that range one comparison at a time, and when we reach a `nullptr`, the range at that spot is exactly the range our value falls in. Placing the node anywhere else would break the property for some ancestor.

### Walking Through: Insert 5

```
Start at 8:   5 < 8   → go LEFT
At 3:         5 > 3   → go RIGHT
At 6:         5 < 6   → go LEFT
At 4:         5 > 4   → go RIGHT
Right child of 4 is **nullptr**  → attach the new node here
```

```
Before:                               After:

              8                                     8
            /   \                                 /   \
           3     10                              3     10
          / \      \                            / \      \
         1   6      14                         1   6      14
            / \     /                             / \     /
           4   7   13                            4   7   13
                                                  \
                                                   5
```

Check that `5` respects every ancestor: it is less than `8`, greater than `3`, less than `6`, and greater than `4`. The in-order traversal is now `1 3 4 5 6 7 8 10 13 14`, still sorted.

---

## . The Recursive Code

```cpp
TreeNode* insertBST(TreeNode* root, int value) {
		// Base case
    if (root == nullptr) {
        return new TreeNode(value);                    // empty spot found: the new node goes here
    }
    
    if (value < root->data) {
        root->left = insertBST(root->left, value);     // insert into the left subtree
    } else if (value > root->data) {
        root->right = insertBST(root->right, value);   // insert into the right subtree
    }
    
    return root;                                       // this subtree's root is unchanged
}
```

Like `insertAtHead` in Unit II, it **returns the updated root** for the caller to reassign, and it takes only a `TreeNode* root` and a value:

```cpp
root = insertBST(root, 5);
```

### How the Line `root->left = insertBST(root->left, value)` Works

This line looks odd the first time: why assign the result back to `root->left`? Because the recursive call **returns the root of the subtree it was given**, which is either the same subtree or a new one:

| What `root->left` currently is | What the recursive call returns | Effect of the assignment |
| --- | --- | --- |
| a real node | that same node (the last line, `return root`) | nothing changes: the pointer is overwritten with its own value |
| `nullptr` | a **brand new node** (the base case) | the new node is **attached** as the left child |

So the same assignment does both jobs: it keeps existing links intact, and it hooks the new leaf in at the one empty slot. This is the same "return the updated pointer and reassign it" pattern used for the linked list functions in Unit II.

⚠️ **The assignment is what attaches the node.** If you write `insertBST(root->left, value);` without `root->left =`, the new node is created and then lost, a memory leak with no change to the tree.

⚠️ **`root == nullptr` is both the base case and the insertion point.** The first insertion into an empty tree reaches it immediately, which is why the caller writes `root = insertBST(root, 8)` and `root` becomes the first node.

⚠️ **Equal values do nothing here.** With distinct values assumed (Lecture 4, Section 9), neither `if` is true for a duplicate, so the tree is returned unchanged. If your problem allows duplicates, you must choose a convention first.

---

## 3. The Iterative Code

The recursion does no work after the recursive call except pass the pointer back up, so a loop can replace it. The trouble is that to attach a node, we must stand on the **parent**, not on the empty slot. We solve it by **looking one step ahead**: before moving, check whether the child we would move to is empty.

```cpp
TreeNode* insertBST(TreeNode* root, int value) {
    TreeNode* newNode = new TreeNode(value);
    if (root == nullptr) return newNode;               // empty tree: the new node is the root

    TreeNode* curr = root;                             // walking pointer
    while (true) {
        if (value < curr->data) {
            if (curr->left == nullptr) {
                curr->left = newNode;                  // empty slot: attach here
                break;
            }
            curr = curr->left;
        } else if (value > curr->data) {
            if (curr->right == nullptr) {
                curr->right = newNode;
                break;
            }
            curr = curr->right;
        } else {
            delete newNode;                            // ***duplicate***: discard the new node
            break;
        }
    }
    
    return root;
}
```

|  | Recursive | Iterative |
| --- | --- | --- |
| Code | short and mirrors the definition | longer, needs the look-ahead |
| Time | O(h) | O(h) |
| Extra space | O(h) call stack | **O(1)** |

⚠️ **Never write `while (curr != nullptr) { ... curr = curr->left; }` and then attach.** When the loop ends, `curr` is `nullptr`, and we no longer know which node was its parent. That is why the look-ahead check (`curr->left == nullptr`) happens **before** moving.

⚠️ **The duplicate branch frees the node.** We allocated `newNode` before searching, so if it ends up unused it must be deleted, or the memory leaks.

---

## 4. Building a BST From a Sequence

To build a BST, insert the values one at a time into an initially empty tree (`root = nullptr`):

```cpp
TreeNode* root = nullptr;
int values[] = {8, 3, 10, 1, 6, 14, 4, 7, 13};
for (int v : values) {
    root = insertBST(root, v);
}
```

This sequence builds exactly the reference tree: `8` becomes the root, `3` goes left, `10` goes right, `1` goes left of `3`, and so on.

### The Tree Growing

The keys `4, 2, 6, 1, 3` inserted in that order:

```
insert 4:      insert 2:      insert 6:       insert 1:        insert 3:

    4              4              4               4                4
                  /              / \             / \              / \
                 2              2   6           2   6            2   6
                                               /               / \
                                              1               1   3
```

Each value follows the signposts from the root and stops at the first empty slot.

### The Shape Follows the Order

This is the point made in Lecture 4, Section 5, now with code behind it:

```
Order  4, 2, 6, 1, 3, 5, 7:        Order  1, 2, 3, 4, 5, 6, 7:

            4                       1
          /   \                      \
         2     6                      2
        / \   / \                      \
       1   3 5   7                      3
                                         \
   perfect, height 2                      4 ... → a chain, height 6
```

**Real-life analogy:** Seating guests as they arrive at a party where everyone is placed relative to the first guest. Guests arriving in a mixed order form a wide cluster. Guests arriving already sorted by seniority form a single queue.

### Tree Sort

Insert `n` values into a BST, then read the tree with an **in-order traversal**: the output is sorted (Lecture 4, Section 4). That is a complete sorting algorithm, called **tree sort**:

| Case | Reason | Time |
| --- | --- | --- |
| Typical (random order) | each insertion costs about `log n` | **O(n log n)** |
| Worst (already sorted input) | the tree is a chain, each insertion costs up to `n` | **O(n²)** |

This is the same behaviour as Quick Sort with a first-element pivot: a good split gives `n log n`, and sorted input gives `n²`. Both fail on the input you would least like them to fail on.

---

## 5. Complexity of Insertion

### Time Complexity: O(h)

Insertion follows one root-to-leaf path and then does a constant amount of work (one allocation, one pointer assignment). So it costs **O(h)**:

| Tree shape | Height `h` | Insertion cost |
| --- | --- | --- |
| Balanced | about `log₂ n` | **O(log n)** |
| Degenerate | `n − 1` | **O(n)** |

Unlike insertion into a sorted array (Unit I, Lecture 2), **no element ever shifts**. The only cost is finding the spot.

### Space Complexity

- Iterative: **O(1)** extra space (apart from the new node itself).
- Recursive: **O(h)** for the call stack.

---

## 6. Common Traps (Insertion)

| Pitfall | What happens | Fix |
| --- | --- | --- |
| Forgetting to assign: `insertBST(root->left, value);` | the new node is created and lost (memory leak), and the tree is unchanged | write `root->left = insertBST(root->left, value);` |
| Forgetting `return root;` at the end | the function returns garbage, and the parent link gets corrupted | every non-base path returns `root` |
| Not reassigning `root` in the caller | inserting into an empty tree leaves `root` as `nullptr` | `root = insertBST(root, value);` |
| Looping until `curr == nullptr` in the iterative version | the parent of the empty slot is lost | look ahead: check `curr->left` / `curr->right` before moving |
| Using `<=` for the left branch while also testing for equality | duplicates are inserted or ignored inconsistently | decide the duplicate convention first, then keep it everywhere |
| Not freeing the unused node on a duplicate (iterative) | memory leak | `delete newNode;` when the value already exists |
| Assuming insertion is always O(log n) | sorted input creates a chain with O(n) insertion | the cost is O(h) |
| Inserting a new node anywhere other than the empty slot at the end of the search path | the BST property breaks | always follow the search path |

---

## 7. Part 1 Recap

| Concept | Meaning | Key point |
| --- | --- | --- |
| Insertion idea | search for the value, attach the new node at the `nullptr` where the search ends | the new node is always a leaf |
| Why a leaf | the empty slot's allowed range is exactly the new value's range | nothing existing moves |
| Recursive insertion | `root->left = insertBST(root->left, value)` | the return value re-links, or attaches the new node |
| Return pattern | returns the updated root, caller reassigns | same pattern as `insertAtHead` in Unit II |
| Iterative insertion | look one step ahead before moving | needs a parent, which a `nullptr` loop loses |
| Duplicates | ignored (and freed, if allocated) | convention must be consistent |
| Building a BST | insert values one by one into an empty tree | the shape follows the order |
| Tree sort | build a BST, then read in-order | O(n log n) typical, O(n²) on sorted input |
| Time | **O(h)** | O(log n) balanced, O(n) degenerate |
| Space | O(1) iterative, O(h) recursive |  |

**The single biggest idea from this part:** insertion is search with one extra step. The tree already knows where the value belongs, because the BST property leaves exactly one empty slot that fits. We only need to attach a new leaf there, and nothing else in the tree has to move.

# 🎯Part 2: Deletion

---

## Quick Recap Before We Continue

In Part 1, insertion was easy because the new node always lands at an empty slot at the bottom of the tree. Deletion removes a node that may be **anywhere**, possibly in the middle with whole subtrees hanging below it. When a node disappears, two things must still be true afterwards:

1. **The tree stays connected**: no subtree is orphaned.
2. **The BST property still holds** everywhere.

The right action depends on **how many children the node has**: 0, 1, or 2. Those are the three cases.

We use the same reference tree and the same two-step beginning as every BST operation:

```
              8
            /   \
           3     10
          / \      \
         1   6      14
            / \     /
           4   7   13
```

**Step 0 is always the same: find the node** by searching, exactly as in Lecture 4. Only then do we decide what to do with it.

---

## 8. `Case 1`: The Node Is a Leaf (0 Children)

The simplest case. A leaf has nothing hanging below it, so we just **remove it and clear its parent's pointer**.

### Example: Delete 4

```
Before:                               After:

              8                                     8
            /   \                                 /   \
           3     10                              3     10
          / \      \                            / \      \
         1   6      14                         1   6      14
            / \     /                                \     /
           4   7   13                                 7   13
           ↑
       **delete** this

Parent 6's left pointer is set to nullptr, and node 4 is freed.
```

The property holds trivially: removing a value never makes any remaining comparison false.

**Real-life analogy:** Taking down the last house at the end of a street. No other house depends on it, so you remove it and cross it off the map.

---

## 9. `Case 2`: The Node Has One Child

The node's single child (together with everything below it) must take its place. We **bypass** the node: the parent skips over it and points straight to the grandchild.

### Example: Delete 10 (It Has Only a Right Child, 14)

```
Before:                               After:

              8                                     8
            /   \                                 /   \
           3     10  ← delete                    3     14
          / \      \                            / \    /
         1   6      14                         1   6  13
            / \     /                             / \
           4   7   13                            4   7

8's right pointer is changed from 10 to 14, and node 10 is freed.
```

Why is the property still true? Everything in the subtree that moved up (`14, 13`) was already greater than `10` and, because `10` was in `8`'s right subtree, greater than `8`. Removing `10` does not change either fact. The subtree simply moves up one level.

The mirror image works the same way. If the node has only a **left** child, the parent points to that left child instead (for example, deleting `14` from the original tree makes `10`'s right pointer point to `13`).

**Real-life analogy:** This is exactly `deleteAtPosition` from Unit II. In that case you let the previous person let go of the removed person and hold the hand of the person behind them. Here the parent "holds the hand" of the single child directly.

---

## 10. `Case 3`: The Node Has Two Children

This is the hard case. Suppose we delete `3`, which has two children, `1` and `6`, each with their own subtrees. We can't simply bypass it, because the parent (`8`) has only **one** pointer available but there are **two** subtrees to reattach.

```
              8
            /   \
           3  ← delete         The parent has ONE pointer,
          / \                  but node 3 leaves behind TWO subtrees.
         1   6
            / \
           4   7
```

### The Idea: Replace the Value With a Neighbour

Instead of removing the **node** (the box), we replace the **value** inside it with another value from the tree. The replacement must satisfy:

- greater than everything in the left subtree, and
- smaller than everything in the right subtree.

Only two values in the tree can do this: the value just **before** `3` in sorted order, and the value just **after** it.

```
In-order of the tree:   1   3   4   6   7   8   10   13   14
                            ↑
                        deleting this
                      predecessor ← 1   4 → successor
```

| Candidate | What it is | Where to find it |
| --- | --- | --- |
| **In-order successor** | the next larger value | the **minimum of the right subtree** (leftmost node of the right subtree) |
| **In-order predecessor** | the next smaller value | the **maximum of the left subtree** (rightmost node of the left subtree) |

Both work. We use the **successor**, which is the `findMin` function from Lecture 4 applied to `node->right`.

### The Procedure

1. Find the node to delete (it has two children).
2. Find its **in-order successor**: `succ = findMin(node->right)`.
3. **Copy** the successor's value into the node.
4. **Delete the successor** from the right subtree.

### Why Step 4 Is Easy

The successor is the **leftmost** node of the right subtree, so it has **no left child**. That means it has at most one child (a right child), so deleting it is **Case 1 or Case 2**, which we already know. The hard case reduces to an easy one.

### Example: Delete 3

```
**Step 1**:  find node 3.
**Step 2**:  successor = leftmost node of 3's right subtree.
         Right subtree of 3 starts at 6 → go left → 4 → no left child → successor = 4.
**Step 3**:  copy 4 into the node that held 3.
**Step 4**:  delete the original 4 from the right subtree (it is a leaf → Case 1).
```

```
Before:                               After step 3 (value copied):

              8                                     8
            /   \                                 /   \
           3     10                              4     10      ← this node now holds 4
          / \      \                            / \      \
         1   6      14                         1   6      14
            / \     /                             / \     /
           4   7   13                            4   7   13
           ↑                                     ↑
       successor                          the original 4 is still here (duplicate for now)
```

```
After step 4 (original 4 deleted):

              8
            /   \
           4     10
          / \      \
         1   6      14
              \     /
               7   13

In-order:  1  4  6  7  8  10  13  14     ← sorted, and 3 is gone ✅
```

### Example: Delete the Root, 8

The root has two children. The right subtree of `8` starts at `10`. Going left from `10`, there is no left child, so the successor is `10` itself. This time the successor has a right child (`14`), so removing it is **Case 2**:

```
Step 2:  successor = 10  (leftmost node of 8's right subtree)
Step 3:  copy 10 into the root.
Step 4:  delete the original 10 from the right subtree → it has one child (14) → Case 2

Before:                               After:

              8                                    10
            /   \                                 /   \
           3     10                              3     14
          / \      \                            / \    /
         1   6      14                         1   6  13
            / \     /                             / \
           4   7   13                            4   7

In-order:  1  3  4  6  7  10  13  14     ✅ sorted, 8 is gone
```

**Real-life analogy:** An elected position becomes vacant, and the replacement must be the **next person in line by seniority**, since anyone else would jump the queue. Whoever takes the seat leaves their own old seat empty, but that old seat was at the end of a row, so it is easy to tidy up.

⚠️ **Why not just any value from the right subtree?** Taking, for example, `14` as the replacement would put `14` at the top of the right subtree, and the nodes `10` and `13`, which are smaller than `14`, would sit in its right subtree. Only the **minimum** of the right subtree is smaller than everything else there.

---

## 11. The Code

```cpp
TreeNode* findMin(TreeNode* root) {                 // from Lecture 4
    TreeNode* curr = root;
    
    while (curr->left != nullptr) {
        curr = curr->left;
    }
    
    return curr;
}

TreeNode* deleteNode(TreeNode* root, int key) {
		// Base case
    if (root == nullptr) {
        return nullptr;                             // key not found: nothing to delete
    }

    if (key < root->data) {
        root->left = deleteNode(root->left, key);   // keep searching on the left
    } else if (key > root->data) {
        root->right = deleteNode(root->right, key); // keep searching on the right
    } else {
        // found the node to delete
        if (root->left == nullptr) {                // Case 1 (leaf) or Case 2 (only a right child)
            TreeNode* child = root->right;
            delete root;
            return child;                           // the parent will point to this
        }
        if (root->right == nullptr) {               // Case 2 (only a left child)
            TreeNode* child = root->left;
            delete root;
            return child;
        }
        // Case 3: two children
        TreeNode* successor = findMin(root->right);
        root->data = successor->data;               // copy the successor's value here
        root->right = deleteNode(root->right, successor->data);   // remove the original successor
    }
    return root;
}
```

The function takes only `TreeNode* root` and a key, and it returns the updated root, the same pattern as every function in this unit and in Unit II:

```cpp
root = deleteNode(root, 3);
```

### How the Code Maps to the Cases

| Case | What the code does |
| --- | --- |
| **Not found** | recursion reaches `nullptr` and returns `nullptr`; the tree is unchanged |
| **Case 1: leaf** | `root->left == nullptr`, so `child = root->right`, which is `nullptr`; the node is freed and `nullptr` is returned, so the parent's pointer becomes `nullptr` |
| **Case 2: one child** | the missing side's check fires, and the existing child is returned, so the parent now points to the grandchild |
| **Case 3: two children** | neither side is empty, so we copy the successor's value into this node and recursively delete the successor from the right subtree |

Notice that **Case 1 needs no code of its own**: a leaf has no left child, so it is handled by the same lines as "only a right child", and the "child" that gets returned happens to be `nullptr`.

### Why the Line `root->left = deleteNode(root->left, key)` Works

It is the same re-linking pattern as insertion. The recursive call returns the **new root of that subtree**: the same node if nothing changed there, a different node if the old root was deleted (the child that replaced it), or `nullptr` if it was a leaf. Assigning it back updates the parent's pointer with whichever it is.

⚠️ **Save the child pointer before `delete root`.** After `delete root;`, the node's memory is freed, and reading `root->right` afterwards is undefined behaviour. That is why the code stores `child` first and deletes second.

⚠️ **Case 3 copies a value, not a node.** This is simpler than relinking nodes, and it is fine when nothing else holds a pointer to the node being "deleted". If other parts of a program keep pointers to nodes, a different approach (physically swapping the nodes) is needed.

⚠️ **In Case 3, delete the successor by its value from `root->right`.** Because the successor is the minimum of that subtree, the search finds it by going left repeatedly, and it falls into Case 1 or Case 2 there. Starting the second deletion from the original root instead of `root->right` can find a different node when copied values coincide.

⚠️ **Deleting the root of a one-node tree returns `nullptr`.** The caller must write `root = deleteNode(root, key);`. Without reassignment, `root` would still point to freed memory.

---

## 12. The Predecessor Alternative

Using the **predecessor** instead is symmetrical: copy the **maximum of the left subtree** into the node, then delete that maximum from the left subtree. The maximum of a subtree has no right child, so it is again Case 1 or Case 2.

```cpp
// Case 3, predecessor version
TreeNode* predecessor = findMax(root->left);
root->data = predecessor->data;
root->left = deleteNode(root->left, predecessor->data);
```

Both versions are correct, and the results differ only in which neighbouring value ends up in the node. If a question does not say which to use, state your choice. Repeatedly using the same side can unbalance the tree over many deletions, but that is beyond this syllabus.

---

## 13. Complexity of Deletion

### Time Complexity: O(h)

Deletion does at most three things, each following **one path** down the tree:

1. Search for the node: at most `h` steps.
2. Find the successor (Case 3 only): `findMin` goes further down, at most `h` more steps in total.
3. Delete the successor from the right subtree (Case 3 only): another downward path.

In Case 3, steps 1 to 3 together follow, in total, at most about `2h` nodes (the search path plus the successor path), so the cost is still **O(h)**.

| Tree shape | Height `h` | Deletion cost |
| --- | --- | --- |
| Balanced | about `log₂ n` | **O(log n)** |
| Degenerate | `n − 1` | **O(n)** |

Compare with a sorted array (Unit I, Lecture 2), where deleting from the front shifts `n − 1` elements. Here the cost depends only on the height.

### Space Complexity

**O(h)** for the recursion stack. The pointer changes themselves take O(1).

---

## 14. Full Working Example

```cpp
#include <iostream>
#include <vector>
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

TreeNode* insertBST(TreeNode* root, int value) {
    if (root == nullptr) return new TreeNode(value);
    if (value < root->data) {
        root->left = insertBST(root->left, value);
    } else if (value > root->data) {
        root->right = insertBST(root->right, value);
    }
    return root;
}

TreeNode* findMin(TreeNode* root) {
    TreeNode* curr = root;
    while (curr->left != nullptr) curr = curr->left;
    return curr;
}

TreeNode* deleteNode(TreeNode* root, int key) {
    if (root == nullptr) return nullptr;

    if (key < root->data) {
        root->left = deleteNode(root->left, key);
    } else if (key > root->data) {
        root->right = deleteNode(root->right, key);
    } else {
        if (root->left == nullptr) {
            TreeNode* child = root->right;
            delete root;
            return child;
        }
        if (root->right == nullptr) {
            TreeNode* child = root->left;
            delete root;
            return child;
        }
        TreeNode* successor = findMin(root->right);
        root->data = successor->data;
        root->right = deleteNode(root->right, successor->data);
    }
    return root;
}

void inorder(TreeNode* root, vector<int> &ans) {
    if (root == nullptr) return;
    inorder(root->left, ans);
    ans.push_back(root->data);
    inorder(root->right, ans);
}

void printInorder(TreeNode* root) {
    vector<int> v;
    inorder(root, v);
    for (int x : v) cout << x << " ";
    cout << endl;
}

int main() {
    TreeNode* root = nullptr;
    int values[] = {8, 3, 10, 1, 6, 14, 4, 7, 13};
    for (int v : values) {
        root = insertBST(root, v);
    }
    printInorder(root);          // 1 3 4 6 7 8 10 13 14

    root = deleteNode(root, 4);  // Case 1: leaf
    printInorder(root);          // 1 3 6 7 8 10 13 14

    root = deleteNode(root, 10); // Case 2: one child
    printInorder(root);          // 1 3 6 7 8 13 14

    root = deleteNode(root, 3);  // Case 3: two children
    printInorder(root);          // 1 6 7 8 13 14

    root = deleteNode(root, 8);  // Case 3 on the root
    printInorder(root);          // 1 6 7 13 14

    root = deleteNode(root, 99); // not present: no change
    printInorder(root);          // 1 6 7 13 14

    return 0;
}
```

Every printed line is in sorted order, which confirms that the BST property survived each deletion.

---

## 15. Common Traps (Deletion)

| Pitfall | What happens | Fix |
| --- | --- | --- |
| Reading `root->right` after `delete root` | undefined behaviour (reading freed memory) | store the child pointer first, delete second |
| Not assigning the recursive result: `deleteNode(root->left, key);` | the parent still points to the freed node, a dangling pointer | `root->left = deleteNode(root->left, key);` |
| Forgetting `return root;` at the end | the parent link is overwritten with garbage | every non-deleting path returns `root` |
| Not reassigning `root` in the caller | deleting the actual root leaves `root` dangling | `root = deleteNode(root, key);` |
| Using any value from the right subtree as the replacement | the BST property breaks | use the **minimum** of the right subtree (or the maximum of the left) |
| Using the maximum of the right subtree, or the minimum of the left | wrong neighbour, so the property breaks | successor = min of right; predecessor = max of left |
| Treating the two-child case like the one-child case | one subtree is lost | copy the successor's value, then delete the successor |
| Deleting the successor from the whole tree rather than `root->right` | can remove the wrong node if values coincide after copying | recurse into `root->right` |
| Forgetting that the successor itself may have a right child | the child is lost | deleting the successor goes through the same function, so Cases 1 and 2 handle it |
| Assuming deletion always costs O(log n) | a chain costs O(n) | the cost is O(h) |
| Calling `findMin` on `nullptr` | crash | in Case 3 the right child is never `nullptr`, but check before reusing `findMin` elsewhere |

---

## 16. Part 2 Recap

| Concept | Meaning | Key point |
| --- | --- | --- |
| Deletion goal | remove a node, stay connected, keep the BST property | find the node first, then act by case |
| Case 1: leaf | clear the parent's pointer, free the node | no other change |
| Case 2: one child | the parent bypasses the node and points to its child | the subtree moves up one level; like `deleteAtPosition` in Unit II |
| Case 3: two children | copy the in-order successor's value in, then delete the successor | the successor is the minimum of the right subtree |
| Why the successor works | greater than all of the left subtree, smaller than the rest of the right | the only neighbours in sorted order are the predecessor and the successor |
| Why Step 4 is easy | the successor has no left child | deleting it is Case 1 or Case 2 |
| Predecessor alternative | maximum of the left subtree | symmetrical and equally valid |
| Code pattern | returns the updated root; the leaf case falls into the "no left child" branch | `root = deleteNode(root, key);` |
| Dangling pointer trap | save `child` before `delete root` | reading freed memory is undefined |
| Time | **O(h)** | at most about `2h` nodes are followed |
| Space | O(h) recursion stack |  |

**The single biggest idea from this part:** deletion looks like three different problems but is really one, plus a reduction. A leaf and a one-child node are handled by letting the parent bypass the node. A two-child node is turned into one of those two easy cases by **borrowing the in-order neighbour's value**, which can be taken without breaking the sorted order.

---

## 17. Full Lecture Summary

| Concept | Meaning | Key point |
| --- | --- | --- |
| Insertion | search for the value, attach a new leaf at the `nullptr` where the search ends | nothing existing moves |
| Insertion code | `root->left = insertBST(root->left, value)`, returns the updated root | iterative needs a look-ahead |
| Shape and order | the insertion order decides the shape | sorted input gives a chain |
| Tree sort | insert all values, then read in-order | O(n log n) typical, O(n²) worst |
| Deletion cases | leaf, one child, two children | find the node first |
| Leaf / one child | bypass: the parent points past the node | parent pointer becomes `nullptr` or the child |
| Two children | copy the in-order successor (min of the right subtree) in, then delete it | reduces to an easy case |
| Return-and-reassign | every function returns the updated root | mirrors `insertAtHead` and `deleteAtHead` from Unit II |
| Complexity | **O(h)** for search, insertion and deletion | O(log n) balanced, O(n) degenerate |
| Property preserved | every operation keeps the in-order traversal sorted | the quickest way to check an answer |

**The single biggest idea from this lecture:** every BST operation, whether search, insert or delete, is the **same walk down one root-to-leaf path**, guided by the BST property. Insertion adds a leaf at the end of that walk, and deletion removes a node found along it, patching the hole by letting a neighbour in sorted order fill the gap. Because all three follow a single path, all three cost O(h), and the tree's shape decides whether that is fast or slow.