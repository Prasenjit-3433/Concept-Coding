# Lecture 3: Tree Traversals

Status: Done

# 🎯Part 1: Depth-First Traversals (Pre-order, In-order, Post-order)

---

## Quick Recap Before We Begin

In Lecture 2 we stored a binary tree using `TreeNode` (`data`, `left`, `right`) and the single entry pointer `root`. We wrote our first recursive tree function, `countNodes`, which visited every node. But counting doesn't care about **order**. Today we ask: *in what order should we visit the nodes, and why does the order matter?*

This lecture is split into three parts:

- **Part 1** (this note): the three **depth-first** traversals, **pre-order, in-order and post-order**.
- **Part 2**: **level-order** traversal, which needs a queue instead of recursion.
- **Part 3**: the two harder traversals, **boundary traversal** and **vertical order traversal**.

---

## 1. What Is a Traversal?

**Traversal** means visiting **every node of the tree exactly once**, in some systematic order, doing some work at each node (printing it, collecting it, adding it up).

For a linear structure, there is only one sensible order: start at one end and walk to the other. A tree **branches**, so at every node we must choose which way to go first, and that choice creates many possible orders.

### Two Families of Traversal

| Family | Idea | Traversals |
| --- | --- | --- |
| **Depth-first (DFS)** | go *as **deep** as possible* along one path before backing up and trying another | pre-order, in-order, post-order |
| **Breadth-first (BFS)** | visit *all nodes at one **level*** before moving to the next | level-order |

**Real-life analogy:** Exploring a building. **Depth-first** is taking one corridor all the way to its dead end, backing up, and trying the next corridor. **Breadth-first** is checking every room on the ground floor first, then every room on the first floor, and so on.

Part 1 covers the depth-first family.

---

## 2. The Reference Tree

We use this one tree for the whole lecture, so every traversal can be compared on the same data:

```
              1
            /   \
           2     3
          / \     \
         4   5     6
            / \
           7   8
```

| Fact | Value |
| --- | --- |
| Nodes | 8 |
| Leaves | 4, 7, 8, 6 |
| Height | 3 |
| Nodes at each level | 1, 2, 3, 2 |

It is deliberately lopsided: node `3` has only a right child, and node `5` has two children. A symmetric tree would hide the differences between the traversals.

---

## 3. One Skeleton, Three Orders

Recall the recursive definition from Lecture 1: a binary tree is **empty**, or a **root** with a **left subtree** and a **right subtree**. So at any node, only three jobs exist:

| Symbol | Job |
| --- | --- |
| **N** | do the work **at the Node** (visit it) |
| **L** | traverse the **Left** subtree (a recursive call) |
| **R** | traverse the **Right** subtree (a recursive call) |

We always do `L` before `R` (left subtree before right). The only choice is **where `N` goes**:

| Traversal | Order | The node is visited... |
| --- | --- | --- |
| **Pre-order** | **N** L R | **before** its subtrees |
| **In-order** | L **N** R | **between** its subtrees |
| **Post-order** | L R **N** | **after** its subtrees |

The prefix names the position of the node: **pre** = before, **in** = in between, **post** = after.

### The Code

All three functions are identical except for the position of one line. Each collects the visited values in a vector passed by reference, so every call adds to the same list:

```cpp
void preorder(TreeNode* root, vector<int> &ans) {
		// base case
    if (root == nullptr) return;          // empty tree: nothing to visit
    
    ans.push_back(root->data);            // N  (before the recursive calls)
    
    preorder(root->left, ans);            // L
    preorder(root->right, ans);           // R
}

void inorder(TreeNode* root, vector<int> &ans) {
		// base case
    if (root == nullptr) return;
    
    inorder(root->left, ans);             // L
    ans.push_back(root->data);            // N  (between the recursive calls)
    inorder(root->right, ans);            // R
}

void postorder(TreeNode* root, vector<int> &ans) {
		// base case
    if (root == nullptr) return;
    
    postorder(root->left, ans);           // L
    postorder(root->right, ans);          // R
    ans.push_back(root->data);            // N  (after the recursive calls)
}
```

⚠️ **This is the winding and unwinding idea from Recursion Level 1, Lecture 2.** Work placed **before** a recursive call runs on the way **in** (winding). Work placed **after** it runs on the way **out** (unwinding). Pre-order does its work while winding, post-order while unwinding, and in-order does it in the middle, after the left call has returned and before the right call starts.

⚠️ **The `nullptr` check comes first**, as in `countNodes`. A leaf's missing child is a `nullptr`, and reading `nullptr->data` would crash. This check is also the **base case**: an empty tree has nothing to visit.

---

## 4. Pre-order Traversal (N L R)

**Rule:** visit the node first, then the entire left subtree, then the entire right subtree.

**Real-life analogy:** Reading a **book's table of contents**. You list the chapter title first, then its sections, then move to the next chapter. A parent always appears **before** the things it contains.

### The Visit Order on the Reference Tree

The number in brackets is the order in which each node is visited:

```
                 1 [1]
               /       \
         2 [2]           3 [7]
        /     \               \
   4 [3]     5 [4]            6 [8]
             /    \
        7 [5]    8 [6]
```

**Output:** `1 2 4 5 7 8 3 6`

The root is always the **first** element. After it comes the *entire* left subtree (`2 4 5 7 8`), and then the entire right subtree (`3 6`). Whole subtrees appear as **consecutive blocks**:

```
preorder = [ 1 ] [ 2 4 5 7 8 ] [ 3 6 ]
             root   left subtree  right subtree
```

### Where Pre-order Is Used

- **Copying a tree.** The parent must exist before its children can be attached, and pre-order creates parents first.
- **Prefix expressions.** Pre-order on an expression tree gives the prefix form from Unit III (see Section 8).
- **Saving a tree to a file** in a way that can be rebuilt (Section 9).

---

## 5. In-order Traversal (L N R)

**Rule:** traverse the entire left subtree, then visit the node, then traverse the entire right subtree.

### The Visit Order on the Reference Tree

```
                 1 [6]
               /       \
         2 [2]           3 [7]
        /     \               \
   4 [1]     5 [4]            6 [8]
             /    \
        7 [3]    8 [5]
```

**Output:** `4 2 7 5 8 1 3 6`

The root appears **in the middle**, with the whole left subtree before it and the whole right subtree after it:

```
inorder = [ 4 2 7 5 8 ] [ 1 ] [ 3 6 ]
            left subtree  root  right subtree
```

**Real-life analogy:** Reading a row of people **from left to right** as they appear on a page. Everything to the left of a person is read before them, and everything to the right after them.

### Where In-order Is Used

- **Binary Search Trees.** A BST is arranged so that everything in the left subtree is smaller than the node and everything in the right subtree is larger. Reading `L N R` therefore visits the values in **sorted order**. This is the main reason in-order matters, and we will use it in Lectures 4 and 5.
- **Infix expressions.** In-order on an expression tree gives the infix form (Section 8).

⚠️ **In-order gives sorted output only for a Binary Search Tree.** For an ordinary binary tree like our reference tree, `4 2 7 5 8 1 3 6` is not sorted, and nothing says it should be.

---

## 6. Post-order Traversal (L R N)

**Rule:** traverse the entire left subtree, then the entire right subtree, then visit the node.

### The Visit Order on the Reference Tree

```
                 1 [8]
               /       \
         2 [5]           3 [7]
        /     \               \
   4 [1]     5 [4]            6 [6]
             /    \
        7 [2]    8 [3]
```

**Output:** `4 7 8 5 2 6 3 1`

The root is always the **last** element, and each node appears only after both of its subtrees:

```
postorder = [ 4 7 8 5 2 ] [ 6 3 ] [ 1 ]
             left subtree  right    root
                           subtree
```

**Real-life analogy:** **Tidying a house before guests come**. You can only call a room finished once everything inside it is finished. Or **dismantling a stack of boxes**: you must unpack the contents of each box before you can throw the box away.

### Where Post-order Is Used

- **Deleting a tree.** A node must be freed only **after** its children, otherwise we lose the addresses of the children. This is the function promised in Lecture 2.
- **Computing a value from the children upward**, such as the height of a tree or the size of each subtree.
- **Evaluating expression trees**, and the postfix form (Section 8).

---

## 7. Why Three Orders? Every Node Is Touched Three Times

Here is a view that makes all three traversals feel like **one** traversal. Walk around the outside of the tree, hugging its edges, starting at the root and keeping the tree on your left. You pass each node **three times**:

```
      A
     / \
    B   C

The walk:  A  →  B  →  B  →  B  →  A  →  C  →  C  →  C  →  A
           ①    ①    ②    ③    ②    ①    ②    ③    ③
```

| Touch | When it happens | Which traversal acts here |
| --- | --- | --- |
| **1st** | arriving at the node from above, before going into its *left subtree* | **pre-order** |
| **2nd** | coming back up from the left subtree, before going into the right | **in-order** |
| **3rd** | coming back up from the *right subtree*, finished with the node | **post-order** |

Reading off the first touches gives `A B C` (pre-order). The second touches give `B A C` (in-order). The third touches give `B C A` (post-order).

A **leaf** has no subtrees, so all three touches happen back to back. This is why leaves appear in the **same left-to-right order** in all three traversals. In our reference tree the leaves always appear as `4, 7, 8, 6`: `4 … 7 8 … 6` in pre-order, `4 … 7 … 8 … 6` in in-order and `4 7 8 … 6` in post-order.

⚠️ **This is a useful check.** If you work out the three traversals of a tree and the leaves are not in the same relative order in each, there is a mistake somewhere.

---

## 8. Expression Trees: Connecting to Unit III

In Unit III we converted expressions between infix, prefix and postfix using stacks. Trees explain *why* those three forms exist. Draw `a * (b + c)` as a tree, with operators as internal nodes and operands as leaves (this is the extended binary tree from Lecture 1, Section 11):

```
        *
       / \
      a   +
         / \
        b   c
```

| Traversal | Output | Which form? |
| --- | --- | --- |
| Pre-order (N L R) | `* a + b c` | **prefix** |
| In-order (L N R) | `a * b + c` | **infix** (but without brackets) |
| Post-order (L R N) | `a b c + *` | **postfix** |

The three forms are simply the **three DFS orders** on the same tree.

⚠️ **In-order loses the brackets.** The output `a * b + c` would normally mean `(a * b) + c`, a different expression. The tree itself has no ambiguity, because the structure shows that `+` is a child of `*`. When printing infix from a tree, an extra `(` and `)` are added around each internal node's subtree. Prefix and postfix need no brackets at all, which is exactly the advantage we noted in Unit III.

---

## 9. Which Traversals Identify a Tree?

If you are given only the output of one traversal, can you rebuild the tree? Consider two different trees with the same node values `A` and `B`:

```
   A           A
  /             \
 B               B

pre-order:   A B         A B     ← identical
in-order:    B A         A B     ← different
post-order:  B A         B A     ← identical
```

A single traversal is not enough: pre-order and post-order each fail to tell these trees apart. Combining two traversals gives these rules:

| Given | Can the tree be rebuilt uniquely? |
| --- | --- |
| **Pre-order + In-order** | ✅ yes |
| **Post-order + In-order** | ✅ yes |
| **Pre-order + Post-order** | ❌ no, in general (the two trees above show why) |
| Any single traversal | ❌ no |

### Why In-order Is the Key

Pre-order tells you **who the root is** (the first element). Post-order tells you the root too (the last element). In-order tells you **how to split the rest**: everything to the left of the root in the in-order list is the left subtree, and everything to the right is the right subtree.

Example: pre-order `A B D E C`, in-order `D B E A C`.

```
Step 1:  pre-order starts with A → root is A.
         In the in-order list, split at A:   [ D B E ]  A  [ C ]
                                              left          right

Step 2:  the left part has 3 nodes, so the next 3 entries of pre-order
         (B D E) are the left subtree's pre-order. Its root is B.
         In its in-order list [ D B E ], split at B:  [ D ]  B  [ E ]

Result:          A
               /   \
              B     C
             / \
            D   E
```

The same splitting idea works recursively down to single nodes. This is a common exam question.

⚠️ **The reason in-order is needed:** it is the only traversal that tells you where the left subtree *ends*. This also assumes all values are **distinct**, since with duplicate values the root cannot be found uniquely in the in-order list.

---

## 10. Putting the Traversals to Work: Delete and Height

### Deleting a Whole Tree (Post-order)

A node can only be freed **after** both of its children have been freed. Otherwise, the pointers to the children disappear with the parent, and the children leak. That order is post-order:

```cpp
TreeNode* deleteTree(TreeNode* root) {
		// base case
    if (root == nullptr) return nullptr;
    
    deleteTree(root->left);          // L: free the left subtree first
    deleteTree(root->right);         // R: free the right subtree
    
    delete root;                     // N: only now free this node
    return nullptr;                  // the tree is now empty
}
```

Like the linked list functions in Unit II, it **returns the updated pointer** for the caller to reassign:

```cpp
root = deleteTree(root);             // root becomes nullptr, no dangling pointer
```

⚠️ **Writing `delete root;` before the recursive calls is a bug.** After the node is freed, `root->left` and `root->right` read **freed memory**. Delete is the one operation where the node's work **must** come last.

### Height of a Tree (Post-order)

The height of a node depends on the heights of its children, so the children must be computed first. In our convention (edge-counting, leaves have height 0), an **empty tree has height −1**:

```cpp
int height(TreeNode* root) {
		// base case
    if (root == nullptr) return -1;              // empty tree
    
    int leftHeight  = height(root->left);        // L
    int rightHeight = height(root->right);       // R
    
    return 1 + max(leftHeight, rightHeight);     // N: combine the children's answers
}
```

For the reference tree this returns `3`, matching Section 2.

⚠️ **Conventions again.** Many problem statements count height in **nodes**, so a single node has height 1 and an empty tree has height 0. The only difference in code is the base case: `return 0;` instead of `return -1;`. Check the definition in each problem.

**The pattern to notice:** both functions follow *"get the answer from the children first, then deal with this node"*. Whenever a node's result depends on its children, use post-order.

---

## 11. Complexity of the DFS Traversals

### Time Complexity: O(n)

Each node is reached once, and the work done at each node (one `push_back`, two calls) is constant. The `nullptr` calls add at most `n + 1` more constant-time calls (recall `n + 1` null pointers from Lecture 2, Section 5). So the total is **O(n)**.

This is the same for all three traversals and for every tree shape.

### Space Complexity: O(h)

Not counting the output vector, the only extra memory is the **recursion stack**. At any moment, the stack holds the calls along **one root-to-node path**, at most `h + 1` calls where `h` is the tree's height:

```
Balanced tree (n = 7):            Skewed tree (n = 7):

       4                          1
     /   \                         \
    2     6                         2
   / \   / \                         \
  1   3 5   7                         3 ... (down to 7)

 deepest stack: 3 calls            deepest stack: 7 calls
 → O(log n)                        → O(n)
```

| Tree shape | Height `h` | Stack space |
| --- | --- | --- |
| Balanced / complete | about `log₂ n` | **O(log n)** |
| Degenerate (skewed) | `n − 1` | **O(n)** |

This is the practical payoff of the shapes from Lecture 1. A balanced tree traverses cheaply, and a skewed tree behaves like a linked list.

⚠️ **Very deep trees can overflow the call stack.** A skewed tree of a few hundred thousand nodes may crash a recursive traversal, exactly like the infinite-recursion crash from Recursion Level 1, Lecture 1. The fix is an **iterative** traversal with an explicit stack (the stack from Unit III). We keep the recursive versions here since they match the syllabus and the structure of the data.

---

## 12. Full Working Example

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

void preorder(TreeNode* root, vector<int> &ans) {
    if (root == nullptr) return;
    ans.push_back(root->data);
    preorder(root->left, ans);
    preorder(root->right, ans);
}

void inorder(TreeNode* root, vector<int> &ans) {
    if (root == nullptr) return;
    inorder(root->left, ans);
    ans.push_back(root->data);
    inorder(root->right, ans);
}

void postorder(TreeNode* root, vector<int> &ans) {
    if (root == nullptr) return;
    postorder(root->left, ans);
    postorder(root->right, ans);
    ans.push_back(root->data);
}

TreeNode* deleteTree(TreeNode* root) {
    if (root == nullptr) return nullptr;
    deleteTree(root->left);
    deleteTree(root->right);
    delete root;
    return nullptr;
}

void printVector(vector<int> &v) {
    for (int x : v) cout << x << " ";
    cout << endl;
}

int main() {
    TreeNode* root = new TreeNode(1);
    root->left = new TreeNode(2);
    root->right = new TreeNode(3);
    root->left->left = new TreeNode(4);
    root->left->right = new TreeNode(5);
    root->right->right = new TreeNode(6);
    root->left->right->left = new TreeNode(7);
    root->left->right->right = new TreeNode(8);

    vector<int> pre, in, post;
    preorder(root, pre);
    inorder(root, in);
    postorder(root, post);

    printVector(pre);     // 1 2 4 5 7 8 3 6
    printVector(in);      // 4 2 7 5 8 1 3 6
    printVector(post);    // 4 7 8 5 2 6 3 1

    root = deleteTree(root);
    return 0;
}
```

---

## 13. Common Traps (Part 1)

| Pitfall | What happens | Fix |
| --- | --- | --- |
| Missing the `root == nullptr` check | reading `nullptr->left` crashes on every leaf | the check is the first line of every tree function |
| Putting the visit line in the wrong position | the traversal silently turns into a different one | pre = before the calls, in = between, post = after |
| Visiting the right subtree before the left | output is the mirror image of the expected one | the convention is always `L` before `R` |
| Passing `ans` by value | each call fills its own copy, and the result stays empty | pass `vector<int> &ans` |
| Expecting sorted output from in-order | wrong for any non-BST tree | sorted output is a property of **Binary Search Trees** |
| `delete root;` before deleting the children | the children's addresses are lost or read from freed memory | delete in post-order, with the node last |
| Not reassigning `root` after `deleteTree` | `root` still holds the address of freed memory | `root = deleteTree(root);` |
| Mixing the height base cases | off by one on the height | edges: empty returns `-1`; nodes: empty returns `0` |
| Assuming pre-order + post-order identifies a tree | two different trees can share both | use in-order together with one of the other two |

---

## 14. Part 1 Recap

| Concept | Meaning | Key point |
| --- | --- | --- |
| Traversal | visit every node exactly once | order matters |
| DFS vs BFS | go deep first vs go level by level | Part 1 covers DFS |
| `N`, `L`, `R` | visit the node / traverse the left subtree / traverse the right subtree | `L` always comes before `R` |
| Pre-order | N L R | root first; whole subtrees appear as blocks; copies a tree, gives prefix |
| In-order | L N R | root in the middle; **sorted for a BST**; gives infix |
| Post-order | L R N | root last; children before parent; used for delete, height, postfix |
| Winding / unwinding | pre does its work while winding, post while unwinding | same idea as Recursion Level 1 |
| Three touches | every node is passed three times; each traversal acts at one of them | leaves appear in the same order in all three |
| Expression trees | pre → prefix, in → infix (needs brackets), post → postfix | the Unit III forms are tree traversals |
| Unique identification | pre + in, or post + in, rebuilds a tree; pre + post does not | in-order locates the split |
| `deleteTree` | post-order, node freed last, returns `nullptr` | `root = deleteTree(root);` |
| `height` | `1 + max(left, right)`, empty returns `-1` | answer from the children first |
| Time | **O(n)** | each node visited once |
| Space | **O(h)** stack | O(log n) balanced, O(n) skewed |

**The single biggest idea from this part:** the three depth-first traversals are **one recursive function with one line moved**. Whether you act on a node before, between or after its subtrees decides the order, and that choice decides what the traversal is good for: copying (pre), sorted output (in), or building an answer from the children upward (post).

# 🎯Part 2: Level-order Traversal (Breadth-First)

---

## Quick Recap Before We Continue

In Part 1 we visited nodes by going **deep first**: all of one subtree before the other. On our reference tree, node `1` is followed by `2`, then `4`, then `5`, so the traversal dives to the bottom before it ever reaches node `3`. Now we want the opposite: visit **every node on a level before moving to the next level**.

```
              1               ← level 0
            /   \
           2     3            ← level 1
          / \     \
         4   5     6          ← level 2
            / \
           7   8              ← level 3

Level-order:  1  2 3  4 5 6  7 8
```

**Real-life analogy:** A **ripple in a pond**. The ripple reaches everything at distance 1 from the stone, then everything at distance 2, and so on. It never reaches a far point before every nearer point has been reached. Or reading a **family tree generation by generation**: grandparents, then parents, then children.

---

## 15. Why Recursion Alone Doesn't Give Level Order

Look at how a depth-first traversal moves: from `1` it goes to `2`, and from `2` straight down to `4`. Level-order must instead go from `2` **sideways** to `3` before going down anywhere. But `2` and `3` are siblings, and `2` does not hold any pointer to `3`. In the linked representation, a node only knows its **children**, not its siblings or cousins.

So we need a way to **remember the nodes we have discovered but not yet visited**, and to visit them in the order we discovered them: first discovered, first visited.

> That is exactly a **queue**: **First In, First Out** (Unit III, Lecture 1).
> 

DFS used the **call stack** (last in, first out) to remember where to come back to. BFS uses a **queue**.

---

## 16. The Algorithm

1. If the tree is empty, return an empty result.
2. Put the `root` in the queue.
3. While the queue is not empty:
    1. **Take** the node at the front of the queue and **visit** it.
    2. **Add its left child** to the back of the queue (if it exists).
    3. **Add its right child** to the back of the queue (if it exists).

Each node, when visited, **adds its own children to the back of the line**. Since children are added after everything already waiting (the rest of their level), a whole level is always processed before the next one starts.

**Real-life analogy:** People joining a **ticket-counter line**. When someone is served, their friends join the *back* of the line. Everyone ahead of those friends, who were already waiting, is served first.

### The Code

```cpp
vector<int> levelOrder(TreeNode* root) {
    vector<int> ans;
    if (root == nullptr) return ans;       // empty tree: empty result

    queue<TreeNode*> q;                    // a queue of node ADDRESSES
    q.push(root);

    while (!q.empty()) {
        TreeNode* node = q.front();        // the next node in line
        q.pop();
        ans.push_back(node->data);         // visit it

        if (node->left != nullptr) {       // enqueue its children, left first
            q.push(node->left);
        }
        if (node->right != nullptr) {
            q.push(node->right);
        }
    }
    return ans;
}
```

The queue stores **`TreeNode*`** (addresses), not values. We need each node's `left` and `right` pointers when its turn comes, and a plain `int` could not provide them.

### The Queue, Step by Step

For the reference tree, the queue is shown with the **front on the left**. Each row is the queue after processing one node:

```
Start:                     [ 1 ]

visit 1, add 2 and 3:      [ 2  3 ]
visit 2, add 4 and 5:      [ 3  4  5 ]
visit 3, add 6:            [ 4  5  6 ]            (3 has no left child)
visit 4, add nothing:      [ 5  6 ]               (4 is a leaf)
visit 5, add 7 and 8:      [ 6  7  8 ]
visit 6, add nothing:      [ 7  8 ]
visit 7, add nothing:      [ 8 ]
visit 8, add nothing:      [ ]                    → queue empty, stop

Visited in order:  1  2  3  4  5  6  7  8
```

The queue acts as a **frontier**: at any moment it holds the nodes that have been discovered but not yet visited, and it is always ordered level by level.

⚠️ **Only push non-null children.** Pushing a `nullptr` and later reading `node->left` on it crashes the program. The two `if` checks are essential.

⚠️ **Left before right, always.** Pushing the right child first would visit every level from right to left. The traversal is still breadth-first, but the order within each level is mirrored.

⚠️ **A stack here would not give level order.** With a stack (last in, first out), the node most recently discovered is visited first, which dives deep along the newest path. That is depth-first behaviour. The FIFO property is what makes the order level by level.

---

## 17. Separating the Levels

Often the problem asks for the nodes **grouped by level**: `[[1], [2, 3], [4, 5, 6], [7, 8]]`. The flat version above has no idea where one level ends and the next begins, since the queue mixes both. The fix is a **size snapshot**.

At the **start** of each level's processing, the queue contains **exactly the nodes of that level** and nothing else. So if we record `size = q.size()` at that moment, we know exactly how many nodes belong to this level, and we process exactly that many.

```
Start of level 2:   queue = [ 4  5  6 ]    size = 3
                              └─ these 3 nodes are level 2, and only them ─┘

While processing them, we add their children (7, 8) to the back:

After 4:   [ 5  6 ]
After 5:   [ 6  7  8 ]       ← level 3's nodes join behind level 2's
After 6:   [ 7  8 ]          ← exactly 3 nodes processed → level 2 is done

Start of level 3:   queue = [ 7  8 ]    size = 2
```

### The Code

```cpp
vector<vector<int>> levelOrderByLevel(TreeNode* root) {
    vector<vector<int>> result;
    if (root == nullptr) return result;

    queue<TreeNode*> q;
    q.push(root);

    while (!q.empty()) {
        int size = q.size();               // snapshot: number of nodes in this level
        vector<int> level;

        for (int i = 0; i < size; i++) {   // process exactly this level's nodes
            TreeNode* node = q.front();
            q.pop();
            level.push_back(node->data);

            if (node->left != nullptr)  q.push(node->left);
            if (node->right != nullptr) q.push(node->right);
        }
        result.push_back(level);           // one finished level
    }
    return result;
}
```

For the reference tree: `[[1], [2, 3], [4, 5, 6], [7, 8]]`.

⚠️ **The snapshot must be taken *before* the inner loop.** Writing `for (int i = 0; i < q.size(); i++)` re-reads the size on every iteration, but the queue keeps growing as children are added, so the loop would run past the end of the current level and merge levels together.

⚠️ **The number of levels is the number of outer-loop iterations**, which is `result.size()`. A tree with `L` levels has height `L − 1` in our edge-counting convention. This gives a second way to compute the height, without recursion.

---

## 18. Complexity of Level-order

### Time Complexity: O(n)

Every node is added to the queue once and removed once, with constant work for each. Total: **O(n)**.

### Space Complexity: O(w)

The extra memory is the **queue**. Its size at any moment is at most the number of nodes on one level, called the **width** `w` of the tree. For our reference tree the levels hold 1, 2, 3 and 2 nodes, so the width is 3, and the queue never holds more than that (plus the next level's nodes being added, which is still bounded by about `2w`).

| Tree shape | Width `w` | Queue space |
| --- | --- | --- |
| Skewed (degenerate) | 1 | **O(1)** |
| Perfect, `n` nodes | about `n / 2` (the last level) | **O(n)** |

Notice the **opposite behaviour** compared to DFS from Part 1:

|  | DFS (pre/in/post) | BFS (level-order) |
| --- | --- | --- |
| Structure that remembers the frontier | recursion call stack | queue |
| Extra space | **O(h)** | **O(w)** |
| Cheap on | wide, shallow trees | narrow, deep trees |
| Expensive on | deep, skewed trees (O(n)) | wide, bushy trees (O(n)) |
| Order of visiting | one whole path at a time | one whole level at a time |

**Real-life analogy:** DFS keeps a record of the *one corridor you are in*, which is short in a wide building. BFS keeps a record of *every corridor entrance on the current floor*, which is short in a tall, thin building.

---

## 19. Connection to the Array Representation

In Lecture 2 we stored a **complete** binary tree in an array by numbering nodes level by level, left to right. That array is **exactly the level-order traversal** of the tree:

```
        A
      /   \
     B     C
    / \   /
   D   E F

Level-order:  A B C D E F
Array:        A B C D E F      ← the same sequence
```

This also explains why the index formulas `2i + 1` and `2i + 2` work: the level-order position of a node's children is fixed by how many nodes come before them in the traversal.

---

## 20. Where Level-order Is Used

| Use | Why level-order fits |
| --- | --- |
| Printing a tree level by level | the grouped version above |
| Finding the height or the number of levels | count the outer-loop iterations |
| Finding the **closest** node that satisfies a condition | the first match found is on the shallowest level |
| Per-level calculations (largest per level, right side view, width) | the snapshot gives each level as a group |
| Building or checking **complete** trees | a tree is complete exactly when level-order never meets a missing child before a real one |
| Serializing a tree to an array | level order is the array form |

⚠️ **The same queue-and-snapshot skeleton solves a whole family of problems.** Only the bookkeeping inside the inner loop changes: keep the maximum of the level, keep only the last node of the level, or keep a running sum. The loops themselves remain exactly as written above.

---

## 21. Common Traps (Part 2)

| Pitfall | What happens | Fix |
| --- | --- | --- |
| Pushing `nullptr` children | crash when `node->left` is read on a null node | push only when the child is not `nullptr` |
| No check for an empty tree | `q.push(nullptr)` and a crash on the first pop | `if (root == nullptr) return ...;` first |
| Using a stack instead of a queue | a depth-first order, not level order | the structure must be FIFO |
| Pushing right before left | every level comes out mirrored | push `left`, then `right` |
| `i < q.size()` as the inner loop condition | the loop runs past the level boundary as the queue grows | take `int size = q.size();` **before** the loop |
| Storing values instead of node addresses in the queue | the children cannot be reached | `queue<TreeNode*>` |
| Forgetting to `pop` | the same front node is processed forever | `q.pop()` right after reading `q.front()` |
| Assuming O(1) extra space | the queue can hold half the tree | space is O(w), up to O(n) |

---

## 22. Part 2 Recap

| Concept | Meaning | Key point |
| --- | --- | --- |
| Level-order | visit all nodes of one level before the next | breadth-first |
| Why not recursion | a node does not know its siblings or cousins | the discovered-but-unvisited nodes must be remembered |
| The queue | first discovered, first visited (FIFO) | the frontier is ordered level by level |
| Algorithm | pop the front, visit it, push its non-null children left then right | the queue stores `TreeNode*` |
| Grouping by level | take `size = q.size()` before the inner loop | the queue holds exactly one level at that moment |
| Number of levels | the number of outer-loop iterations | height = levels − 1 |
| Time | **O(n)** | each node is pushed and popped once |
| Space | **O(w)** | up to `n / 2` for a perfect tree |
| DFS vs BFS | stack of one path vs queue of one level | opposite best and worst cases |
| Array link | the array of a complete tree is its level-order traversal | explains the `2i + 1` formulas |

**The single biggest idea from this part:** depth-first traversal remembers *where to return to*, using the recursion stack, and breadth-first traversal remembers *what is waiting to be visited*, using a queue. Replace the stack with a queue and the traversal changes from path by path to level by level.

# 🎯Part 3: Boundary Traversal & Vertical Order Traversal

---

## Quick Recap Before We Continue

Parts 1 and 2 gave us two families of traversal. The DFS orders (pre, in, post) differ only by where the node is visited. Level-order differs in its data structure: a queue instead of the call stack. Both visit **every** node, in an order decided by one simple rule.

Today's two traversals are different: their order is decided by **geometry**, meaning where a node sits in the **picture** of the tree.

- **Boundary traversal** reads only the **outline** of the tree.
- **Vertical order traversal** reads the tree **column by column**.

Neither can be written as one line moved around in a three-line skeleton. Each needs a small plan of its own, which is why they come last.

---

## 23. Boundary Traversal

### The Idea

Imagine **walking along the outer fence of the tree**, anticlockwise, starting from the root. You touch the root, then travel down the left edge, then along the bottom (every leaf), and then back up the right edge. The nodes you pass form the boundary.

**Real-life analogy:** Walking around the **border of a field** instead of crossing it. Anything in the middle of the field is not visited, however many things grow there.

### The Reference Tree, Marked

```
              1               ← root
            /   \
           2     3            ← 2: left boundary    3: right boundary
          / \     \
         4   5     6          ← 4, 6: leaves     5: interior
            / \
           7   8              ← 7, 8: leaves
```

The boundary traversal is:

```
Root:               1
Left boundary:      2          (top to bottom, leaves excluded)
Leaves:             4  7  8  6 (left to right, every leaf of the tree)
Right boundary:     3          (bottom to top, leaves excluded)

Output:  1  2  4  7  8  6  3
```

Notice that node `5` is **not** in the output. It is not on the left edge, not on the right edge, and not a leaf. It is an interior node, buried in the middle of the tree, and the outline never touches it.

### Breaking the Problem Into Three Easy Pieces

The boundary is built from three parts, each simple on its own:

| Part | Which nodes | Order | How to collect |
| --- | --- | --- | --- |
| **Left boundary** | the path down the left edge, **excluding leaves** | top to bottom | walk down from `root->left` |
| **Leaves** | **every** leaf in the whole tree, including interior ones like `7` and `8` | left to right | a DFS, collecting leaves |
| **Right boundary** | the path down the right edge, **excluding leaves** | **bottom to top** | walk down from `root->right`, then reverse |

The root goes first, once.

### How to Walk the Left Boundary

Start at `root->left`. At every node:

1. If it is **not a leaf**, add it to the answer.
2. Move to its **left child**. If it has **no left child**, move to its **right child** instead.

The second rule matters. If a node on the left edge has no left child, the edge continues through its right child. Here is a tree where that rule is needed:

```
        1
       / \
      2   7
       \   \
        3   8
       / \
      4   5
```

```
Left boundary:   2 has NO left child → go right to 3
                 3 has a left child  → go left to 4
                 4 is a leaf         → stop (it will be added as a leaf)
Collected:       2, 3

Leaves:          4  5  8
Right boundary:  7 (8 is a leaf, so it is excluded)

Output:  1  2  3  4  5  8  7
```

### How to Walk the Right Boundary

The mirror image: start at `root->right`, add non-leaf nodes, and move to the **right child**, or to the **left child** if there is no right child.

The nodes are found **top to bottom**, but the traversal needs them **bottom to top** (we are walking back up the right side). So we collect them in a temporary list and add them in **reverse**.

### Why Leaves Are Collected Separately

The left and right edges stop **before** the leaves, and the leaves are collected in one pass over the entire tree. Two reasons:

- A leaf at the bottom of the left edge would otherwise be counted twice (once as part of the edge, once as a leaf).
- Leaves in the **interior** of the tree, like `7` and `8`, belong to the boundary but are on neither edge. Only a full DFS over the tree finds them.

### The Code

```cpp
bool isLeaf(TreeNode* node) {
    return node->left == nullptr && node->right == nullptr;
}

void addLeftBoundary(TreeNode* root, vector<int> &ans) {
    TreeNode* curr = root->left;
    while (curr != nullptr) {
        if (!isLeaf(curr)) {
            ans.push_back(curr->data);        // leaves are added later, not here
        }
        if (curr->left != nullptr) {
            curr = curr->left;                // prefer the left child
        } else {
            curr = curr->right;               // no left child: the edge continues right
        }
    }
}

void addLeaves(TreeNode* root, vector<int> &ans) {
    if (root == nullptr) return;
    if (isLeaf(root)) {
        ans.push_back(root->data);            // every leaf, left to right
        return;
    }
    addLeaves(root->left, ans);               // left subtree's leaves come first
    addLeaves(root->right, ans);
}

void addRightBoundary(TreeNode* root, vector<int> &ans) {
    vector<int> temp;                         // collected top to bottom
    TreeNode* curr = root->right;
    while (curr != nullptr) {
        if (!isLeaf(curr)) {
            temp.push_back(curr->data);
        }
        if (curr->right != nullptr) {
            curr = curr->right;               // prefer the right child
        } else {
            curr = curr->left;                // no right child: the edge continues left
        }
    }
    for (int i = temp.size() - 1; i >= 0; i--) {
        ans.push_back(temp[i]);               // added bottom to top
    }
}

vector<int> boundaryTraversal(TreeNode* root) {
    vector<int> ans;
    if (root == nullptr) return ans;

    if (!isLeaf(root)) {
        ans.push_back(root->data);            // the root, unless it is the only node
    }
    addLeftBoundary(root, ans);
    addLeaves(root, ans);
    addRightBoundary(root, ans);
    return ans;
}
```

### Why the Root Check `if (!isLeaf(root))`

If the tree is a **single node**, that node is a leaf, and `addLeaves` already adds it. Adding it as the "root" as well would list it twice. So the root is pushed up front **only when it is not a leaf**. For a single-node tree, the result is just that one node, exactly once.

⚠️ **The left and right edges start at `root->left` and `root->right`, not at `root`.** The root is handled separately, so starting the edge walks at the root would add it again.

### Complexity

|  | Cost | Why |
| --- | --- | --- |
| **Time** | **O(n)** | each edge walk follows at most `h` nodes, and the leaves pass visits each node once |
| **Space** | **O(h)** | the recursion stack of `addLeaves`, and the temporary list for the right edge, are both bounded by the height |

(The output itself is not counted as extra space.)

### Common Traps (Boundary)

| Pitfall | What happens | Fix |
| --- | --- | --- |
| Including leaves in the edge walks | leaf nodes at the bottom of an edge appear twice | add a node on an edge only if `!isLeaf(curr)` |
| Adding the root again when it is a leaf | a single-node tree gives `[x, x]` | push the root first only if it is not a leaf |
| Stopping the left edge when `curr->left` is missing | edge nodes reached through a right child are skipped | fall back to `curr->right` |
| Forgetting to reverse the right boundary | the right side reads top to bottom, breaking the anticlockwise order | collect in `temp`, then add in reverse |
| Collecting only the leaves on the two edges | interior leaves (`7`, `8`) are lost | collect leaves with a DFS over the **whole** tree |
| Starting the edge walks at `root` | the root is added again | start at `root->left` and `root->right` |
| Adding every node that is "not a leaf and not on an edge" | interior nodes (like `5`) leak into the answer | only the edges and leaves belong to the boundary |

---

## 24. Vertical Order Traversal

### The Idea

Draw **vertical lines** through the tree, one for each horizontal position. Every node lies on exactly one line. Vertical order traversal reads the tree **line by line, from the leftmost line to the rightmost**, and **top to bottom** along each line.

**Real-life analogy:** **Shining a flashlight from above** and reading off what lies along each vertical line of light, left to right. Or sorting **bricks in a wall of columns**: each column is a stack you read from the top downward.

### Giving Every Node Coordinates

To say which vertical line a node sits on, we give each node two numbers:

| Number | Meaning | Root | Going to the left child | Going to the right child |
| --- | --- | --- | --- | --- |
| **column** | horizontal position | `0` | `column − 1` | `column + 1` |
| **row** | depth (the same as the level) | `0` | `row + 1` | `row + 1` |

Each step to the **left** moves one column to the left, each step to the **right** moves one column to the right, and **every step downward** adds one row. Each node's coordinates come directly from its parent's, so they can be computed during a traversal.

For the reference tree:

```
Node   column   row
 1        0       0
 2       −1       1
 3        1       1
 4       −2       2
 5        0       2
 6        2       2
 7       −1       3
 8        1       3
```

Drawn on a grid, with each node in its column and row:

```
          col −2    col −1    col 0     col 1     col 2
                                1                           ← row 0
                      2                   3                 ← row 1
            4                   5                   6       ← row 2
                      7                   8                 ← row 3
```

Reading each column from top to bottom:

```
col −2:  4
col −1:  2  7           (2 is in row 1, 7 is in row 3)
col  0:  1  5           (1 is in row 0, 5 is in row 2)
col  1:  3  8
col  2:  6

Vertical order:  [[4], [2, 7], [1, 5], [3, 8], [6]]
```

⚠️ **Column 0 is not always the middle of the tree.** A node on column 0 is simply one whose number of left steps equals its number of right steps from the root. Node `5` is on column 0 (left once, right once), even though it is drawn below and to the left of the root's right child.

### Ties: Two Nodes in the Same Cell

Two different nodes can land on the **same column and the same row**. This happens when the paths to them zigzag differently but balance out:

```
          1
        /   \
       2     3
        \   /
         6 5          ← 6 is 2's right child, 5 is 3's left child
```

```
Node   column   row
 1        0       0
 2       −1       1
 3        1       1
 6        0       2      ← −1 + 1
 5        0       2      ← 1 − 1      ← same cell as 6

          col −1    col 0     col 1
                      1                    ← row 0
            2                   3          ← row 1
                     6, 5                  ← row 2: both land in (col 0, row 2)
```

When this happens we need a rule for the order within the cell. The usual rule is **smaller value first**, so column 0 reads `[1, 5, 6]`.

⚠️ **Check the problem statement for the tie rule.** Some problems instead keep the order in which the nodes appear in a level-order traversal (left to right), which would give `[1, 6, 5]`. The traversal logic is the same, but the structure that stores the cells must match the rule.

### Planning the Algorithm

We need to:

1. Compute each node's `(column, row)` while traversing.
2. **Group** the nodes by column.
3. Within a column, order them by **row**, and within a row, by **value**.
4. Read the columns from **leftmost to rightmost**.

Steps 2 to 4 are a sorting-and-grouping job. C++ provides the ideal tool: a **`map`**, which keeps its keys **automatically sorted**, and a **`multiset`**, which keeps its elements sorted and **allows duplicates**. Nest them:

```
map<int, map<int, multiset<int>>>
     │         │         │
     │         │         └── values in this cell, sorted (the tie rule, smaller first)
     │         └──────────── row   → values   (rows come out in increasing order)
     └────────────────────── column → rows     (columns come out left to right)
```

Reading this structure in its natural order gives exactly the vertical order, with no extra sorting code.

### The Code

We traverse with a queue (as in level-order), and each queue entry carries the node **together with its coordinates**:

```cpp
vector<vector<int>> verticalOrder(TreeNode* root) {
    vector<vector<int>> result;
    if (root == nullptr) return result;

    map<int, map<int, multiset<int>>> nodes;        // column → row → values
    queue<pair<TreeNode*, pair<int, int>>> q;       // node, (column, row)
    q.push({root, {0, 0}});                          // the root is at (0, 0)

    while (!q.empty()) {
        auto front = q.front();
        q.pop();

        TreeNode* node = front.first;
        int col = front.second.first;
        int row = front.second.second;

        nodes[col][row].insert(node->data);          // record this node in its cell

        if (node->left != nullptr) {
            q.push({node->left, {col - 1, row + 1}});     // one column left, one row down
        }
        if (node->right != nullptr) {
            q.push({node->right, {col + 1, row + 1}});    // one column right, one row down
        }
    }

    for (auto &colEntry : nodes) {                   // columns in increasing order
        vector<int> column;
        for (auto &rowEntry : colEntry.second) {     // rows in increasing order
            for (int value : rowEntry.second) {      // values within the cell, sorted
                column.push_back(value);
            }
        }
        result.push_back(column);
    }
    return result;
}
```

### How the Structure Fills Up

For the reference tree, `nodes[col][row]` ends up holding:

```
nodes
├── −2 ── row 2: {4}
├── −1 ── row 1: {2}      row 3: {7}
├──  0 ── row 0: {1}      row 2: {5}
├──  1 ── row 1: {3}      row 3: {8}
└──  2 ── row 2: {6}
```

Walking the outer map in key order (−2, −1, 0, 1, 2) and each inner map in key order (rows increasing), and flattening each `multiset`, produces `[[4], [2, 7], [1, 5], [3, 8], [6]]`.

⚠️ **Why a `map` for the columns instead of an array.** Columns can be negative (`−2`), and we do not know in advance how far left or right the tree extends. A `map` accepts any integer key and keeps keys sorted. An array would first need the leftmost and rightmost columns.

⚠️ **The coordinates must travel with the node.** A node does not know its own column or row, and `TreeNode` has no field for them. We pair them with the node in the queue (as `pair<TreeNode*, pair<int,int>>`) so each child can compute its coordinates from its parent's.

⚠️ **Using `nodes[col][row]` creates the entry if it does not exist.** This is how `map`'s `operator[]` behaves, and it is what lets us call `.insert(...)` straight away without checking first.

### Does the Traversal Order Matter?

Not for correctness. Since each node carries its own `(column, row)` and the `map` and `multiset` do all the sorting, we could fill the structure with a pre-order, in-order or post-order traversal and get the same result. The `row` component of the key is what restores the top-to-bottom order. Level-order is used here because it visits nodes in a natural top-down order, which is easier to reason about.

### Complexity

|  | Cost | Why |
| --- | --- | --- |
| **Time** | **O(n log n)** | each of the `n` nodes is inserted into ordered containers, and each insertion costs `O(log n)` |
| **Space** | **O(n)** | every node is stored in the map, and the queue holds up to one level |

The `log n` factor is the price of the self-sorting containers. It is the reason this traversal is slower than the O(n) traversals earlier in the lecture.

### Common Variations: Top View and Bottom View

The same column machinery solves two famous related problems:

| Problem | What to keep from each column |
| --- | --- |
| **Top view** (what you see looking down from above) | the **first** (topmost) node of each column |
| **Bottom view** (looking up from below) | the **last** (bottommost) node of each column |

Only the final reading step changes. The coordinates and the grouping stay exactly as above.

### Common Traps (Vertical Order)

| Pitfall | What happens | Fix |
| --- | --- | --- |
| Moving the **row** by `−1` or `+0` for a child | rows no longer reflect depth, so the order inside a column is wrong | both children are always `row + 1` |
| Moving the column the wrong way | the output comes out mirrored | left child: `col − 1`, right child: `col + 1` |
| Using a `vector` indexed by column | negative columns are out of range | use a `map` keyed by column |
| Using a `set` instead of a `multiset` for the cell | two equal values in the same cell collapse into one | a cell can hold duplicates, so use `multiset` |
| Ignoring the tie rule | nodes in the same cell come out in the wrong order | decide the rule from the problem statement, and store accordingly |
| Not storing the row at all | nodes in one column mix up the top-to-bottom order | the key must include the row, or the BFS order must be relied on explicitly |
| Forgetting the empty-tree check | `q.push(root)` with a null `root` crashes when read | return an empty result first |

---

## 25. Choosing a Traversal

| Traversal | Order determined by | Data structure | Time | Extra space | Typical use |
| --- | --- | --- | --- | --- | --- |
| **Pre-order** | node before subtrees | recursion stack | O(n) | O(h) | copy a tree, prefix form |
| **In-order** | node between subtrees | recursion stack | O(n) | O(h) | sorted output of a BST, infix form |
| **Post-order** | node after subtrees | recursion stack | O(n) | O(h) | delete a tree, height, postfix form |
| **Level-order** | level number | queue | O(n) | O(w) | per-level work, shortest depth |
| **Boundary** | position on the outline | loops + one DFS | O(n) | O(h) | the perimeter of a tree |
| **Vertical order** | horizontal column, then row | queue + map of multisets | O(n log n) | O(n) | column-wise reading, top view, bottom view |

---

## 26. Part 3 Recap

| Concept | Meaning | Key point |
| --- | --- | --- |
| Boundary traversal | root, then left edge, then leaves, then right edge | anticlockwise; interior nodes are omitted |
| Left boundary | walk down from `root->left`, skipping leaves | prefer the left child, else the right child |
| Right boundary | walk down from `root->right`, skipping leaves | prefer the right child, else the left; **add in reverse** |
| Leaves | a DFS over the whole tree, collecting leaves left to right | catches interior leaves like `7` and `8` |
| Single-node root | the root is a leaf | push the root first only if it is not a leaf |
| Boundary cost | O(n) time, O(h) space | edge walks are at most `h` long |
| Vertical order | read column by column, top to bottom inside a column | each node has `(column, row)` |
| Coordinates | root `(0, 0)`; left child `(col − 1, row + 1)`; right child `(col + 1, row + 1)` | a child's coordinates come from its parent's |
| Ties | two nodes in the same `(col, row)` | usually smaller value first; check the problem |
| Data structure | `map<int, map<int, multiset<int>>>` | self-sorted by column, then row, then value |
| Vertical cost | O(n log n) time, O(n) space | the `log n` comes from the ordered containers |
| Top / bottom view | first / last node of each column | same machinery, different final reading |

**The single biggest idea from this part:** the earlier traversals visited nodes in an order built into the recursion. These two ask for an order defined by **position in the drawing**. The solution in both cases is the same: split the picture into simple pieces (edge, leaves, edge) or **attach coordinates to every node** and let sorted containers do the arranging.

---

## 27. Full Lecture Summary

| Concept | Meaning | Key point |
| --- | --- | --- |
| Traversal | visit every node exactly once | two families: depth-first and breadth-first |
| Skeleton | `N` (node), `L` (left subtree), `R` (right subtree), with `L` before `R` | only the position of `N` changes |
| Pre-order | N L R | root first; copy a tree; prefix |
| In-order | L N R | root in the middle; sorted for a BST; infix (needs brackets) |
| Post-order | L R N | root last; delete, height, postfix |
| Three touches | each node is passed three times; each DFS order acts at one touch | leaves appear in the same order in all three |
| Rebuilding a tree | pre + in, or post + in, is unique; pre + post is not | in-order locates the left/right split |
| `deleteTree`, `height` | answers built from the children, so post-order | `root = deleteTree(root);` |
| DFS cost | O(n) time, O(h) stack space | skewed trees cost O(n) space and can overflow the stack |
| Level-order | a queue visits one level at a time | push non-null children, left then right |
| Grouping by level | snapshot `q.size()` before the inner loop | the number of outer iterations is the number of levels |
| BFS cost | O(n) time, O(w) space | the opposite best and worst cases to DFS |
| Boundary traversal | root, left edge, leaves, reversed right edge | leaves excluded from the edges; interior nodes skipped |
| Vertical order | group by column, then row, then value | `map<int, map<int, multiset<int>>>`; O(n log n) |

**The single biggest idea from this lecture:** a traversal is just a decision about **order**, and different decisions answer different questions. Where the node is visited relative to its subtrees gives the three DFS orders. Using a queue instead of the stack gives levels. Using the **picture of the tree** gives the boundary and the columns. Every traversal visits the same nodes, and what changes is which question you want the order to answer.

---