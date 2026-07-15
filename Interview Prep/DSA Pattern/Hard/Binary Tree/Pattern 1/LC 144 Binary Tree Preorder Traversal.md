# LC 144. Binary Tree Preorder Traversal

Key Concept: Recursive + iterative stack: root → left → right
Solution: https://www.youtube.com/watch?v=RlUu72JrOCQ&list=PLgUwDviBIf0q8Hkd7bK2Bpryj2xVJk8Vk&index=6&ab_channel=takeUforward
Status: Done

# LC 144. Binary Tree Preorder Traversal

---

# Stage 1: Identification

**Step 1 — Which topic?**

You are given the root of a binary tree. You need to return the **preorder traversal** of its node values — visit the root first, then the left subtree, then the right subtree.

**Step 2 — Which pattern?**

This is the most fundamental tree problem there is — no property to compute, no path to track, just a traversal order to produce. This is **Pattern 1: Tree Traversals (Core Templates)**.

**Step 3 — Which key concept?**

**Root → Left → Right — and don't overcomplicate it.**

The entire idea: at any node, do three things in this exact order —

1. Process (print/collect) the current node
2. Recurse into the left subtree
3. Recurse into the right subtree

That's the whole algorithm. The recursion handles the rest.

---

# Stage 2: Intuition Building

### Why Recursion Fits Naturally Here

A binary tree is a **recursive structure** by definition — every node is itself the root of a smaller tree (its left subtree) and another smaller tree (its right subtree). So a traversal rule that works for the whole tree must also work identically for every subtree inside it. This is exactly what recursion captures: solve the problem for the current node using the same exact rule applied to its children.

### The Rule, Stated Plainly

At any node:

```
1. Visit (print) the node itself           →  ROOT
2. Apply this exact same rule to the left  →  LEFT
3. Apply this exact same rule to the right →  RIGHT
```

The word "preorder" literally tells you the order: the root comes **before** ("pre") its subtrees are explored.

### The Base Case

What happens when there is no node to process — i.e., you've gone past a leaf into a null child?

```
If the current node is null → there is nothing to visit, nothing to recurse into.
Just return.
```

This base case is what stops the recursion from going on forever. Every recursive call on a real node eventually hits null on both its left and right children, and at that point the recursion for that branch is complete.

### Walking Through the Idea (Not a Dry Run — Just the Shape)

Think of the recursion as a single traveler standing at the root. The traveler's instructions are always the same three steps, repeated at every node it visits:

```
Stand at a node
   → Print it
   → Walk into left child, follow the SAME instructions there
   → Come back, walk into right child, follow the SAME instructions there
   → Once both left and right are fully explored, walk back to whoever sent you here
```

Because the traveler always fully finishes exploring **left** before even starting on **right**, and always prints the current node before doing either — the output naturally comes out in root, left, right order, at every level of the tree, not just the top level.

### Why the Recursion Eventually Terminates

Every recursive call moves to a **child** of the current node — never back up, never sideways. Since the tree has a finite number of nodes and no cycles, every path from the root downward eventually ends at a null pointer (past a leaf). The base case catches that null and returns immediately, which is what unwinds the recursion back up.

---

# Stage 3: Coding

## Approach — Recursive (The Only Natural Approach Here)

```java
class Solution {
    public List<Integer> preorderTraversal(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        solve(root, result);
        return result;
    }

    private void solve(TreeNode node, List<Integer> result) {
        // Base case: nothing here — stop and return
        // WHY: a null node means we've gone past a leaf; there is
        //      nothing to print and nothing further to recurse into
        if (node == null) return;

        // Step 1: ROOT — process the current node first
        result.add(node.val);

        // Step 2: LEFT — fully explore the left subtree
        // using the exact same root-left-right rule
        solve(node.left, result);

        // Step 3: RIGHT — fully explore the right subtree
        // only after the entire left subtree is done
        solve(node.right, result);
    }
}
```

That's the entire solution. No special cases, no extra bookkeeping — just three lines in the right order, matching the definition of preorder directly.

---

## Complexity Analysis

**Time Complexity — O(n):**

Every node in the tree is visited **exactly once** — the recursion calls `solve` once per node, does a constant amount of work at that node (add it to the list), and moves on to its children. Since there are `n` nodes total, and each one is processed in O(1) time, the total time is **O(n)**.

**Space Complexity — O(h), where h is the height of the tree:**

No extra data structure is allocated beyond the output list (which doesn't count as "auxiliary" space since it's the required output). The space that matters here is the **recursion call stack** — at any point during execution, the stack holds one frame for every node currently on the path from the root down to wherever the recursion currently is. That depth is exactly the height of the tree, `h`.

- **Best case (balanced tree):** height is `O(log n)`, so auxiliary space is `O(log n)`.
- **Worst case (skewed tree — every node has only one child, forming a straight line):** height becomes `O(n)`, since the tree degenerates into something resembling a linked list. In this case, the recursion stack holds all `n` nodes at once.

So the honest way to state it: **O(h) auxiliary space, which is O(log n) for a balanced tree and O(n) in the worst case (skewed tree)**.

---

## The Key Takeaway

```
┌─────────────────────────────────────────────────────────────┐
│  Preorder = Root → Left → Right.                                 │
│                                                                  │
│  Don't overcomplicate binary tree recursion:                     │
│  Write down exactly what you want to do at a node, in order.     │
│  Let recursion apply that same rule to every subtree.            │
│                                                                  │
│  Base case: node == null → return immediately.                   │
│  This is what stops the recursion and unwinds the call stack.    │
│                                                                  │
│  Time  — O(n): every node is visited exactly once.               │
│  Space — O(h): recursion stack depth = height of the tree.       │
│           O(log n) balanced, O(n) worst case (skewed tree).      │
└─────────────────────────────────────────────────────────────┘
```