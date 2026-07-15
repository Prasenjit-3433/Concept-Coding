# LC 94. Binary Tree Inorder Traversal

Key Concept: Recursive + iterative stack + Morris (O(1) space variant)
Solution: https://www.youtube.com/watch?v=Z_NEgBgbRVI&list=PLgUwDviBIf0q8Hkd7bK2Bpryj2xVJk8Vk&index=7&ab_channel=takeUforward
Status: Done

# LC 94. Binary Tree Inorder Traversal

---

# Stage 1: Identification

**Step 1 — Which topic?**

You are given the root of a binary tree. You need to return the **inorder traversal** of its node values — visit the left subtree first, then the root, then the right subtree.

**Step 2 — Which pattern?**

This is **Pattern 1: Tree Traversals (Core Templates)** — same pattern as preorder, just a different visiting order.

**Step 3 — Which key concept?**

**Left → Root → Right — same recursive shape as preorder, only the position of the print step changes.**

At any node, do three things in this exact order:

1. Recurse into the left subtree
2. Process (print/collect) the current node
3. Recurse into the right subtree

---

# Stage 2: Intuition Building

### Why This Is Still "Don't Complicate It"

Binary trees are naturally recursive — to traverse a whole tree you go into a subtree, do the required task, and come back, which is exactly what a recursive call does: go, come back, go, come back. Whatever the traversal order says on paper, you write down in that exact order in the code. There is nothing more to invent here than what preorder already taught — only the **position** of the print statement relative to the two recursive calls changes.

### The Rule, Stated Plainly

At any node:

```
1. Apply this exact same rule to the left   →  LEFT
2. Visit (print) the node itself            →  ROOT
3. Apply this exact same rule to the right  →  RIGHT
```

"Inorder" means the root is printed **in between** exploring its left and right subtrees — after everything on the left is fully done, and before anything on the right begins.

### The Base Case

Same as preorder:

```
If the current node is null → there is nothing to visit and nothing to recurse into.
Just return.
```

Every branch of recursion eventually walks off a leaf into a null child, hits this base case, and returns — that's what unwinds the recursion back up to the caller.

### Walking Through the Shape (Not a Dry Run — Just the Idea)

Picture the same single traveler standing at the root, following instructions that are identical at every node it visits:

```
Stand at a node
   → Walk into left child, follow the SAME instructions there
   → Come back, THEN print the current node
   → Walk into right child, follow the SAME instructions there
   → Once both left and right are fully explored, walk back to whoever sent you here
```

Because the traveler always finishes the **entire left subtree** before printing the current node, and always finishes printing the current node before starting the **right subtree**, the values naturally come out in left, root, right order — consistently, at every level of the tree, not just at the top.

### Why the Recursion Eventually Terminates

Exactly the same reasoning as preorder: every recursive call moves strictly downward to a child, the tree has finitely many nodes and no cycles, so every path eventually reaches null. The base case catches that and returns, which is what allows the print statements above it in the call chain to finally execute.

---

# Stage 3: Coding

## Approach — Recursive (The Only Natural Approach Here)

```java
class Solution {
    public List<Integer> inorderTraversal(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        solve(root, result);
        return result;
    }

    private void solve(TreeNode node, List<Integer> result) {
        // Base case: nothing here — stop and return
        // WHY: a null node means we've gone past a leaf; there is
        //      nothing to print and nothing further to recurse into
        if (node == null) return;

        // Step 1: LEFT — fully explore the left subtree first
        solve(node.left, result);

        // Step 2: ROOT — process the current node only after
        // the entire left subtree is done
        result.add(node.val);

        // Step 3: RIGHT — explore the right subtree last,
        // only after the current node has been printed
        solve(node.right, result);
    }
}
```

The only difference from preorder's code: the line that adds `node.val` to the result has **moved** — from being the first statement to being the middle statement, sitting between the two recursive calls instead of before both of them.

---

## Complexity Analysis

**Time Complexity — O(n):**

Every node is visited exactly once — the recursion calls `solve` once per node, does a constant amount of work there (add it to the list), and moves on. With `n` total nodes, each processed in O(1) time, the total time is **O(n)**.

**Space Complexity — O(h), where h is the height of the tree:**

The only space that matters here is the **recursion call stack** — at any point during execution, it holds one frame for every node on the path from the root down to the current position. That depth equals the height of the tree, `h`.

- **Best case (balanced tree):** height is `O(log n)`, so auxiliary space is `O(log n)`.
- **Worst case (skewed tree — every node has only one child):** height becomes `O(n)`, since the tree degenerates into a straight line, and the recursion stack holds all `n` nodes at once.

So: **O(h) auxiliary space — O(log n) for a balanced tree, O(n) in the worst case (skewed tree)**.

---

## The Key Takeaway

```
┌─────────────────────────────────────────────────────────────┐
│  Inorder = Left → Root → Right.                                  │
│                                                                  │
│  Same recursive shape as preorder — don't complicate it.         │
│  Only the POSITION of the print step changes:                    │
│      Preorder:  print, then left,  then right                    │
│      Inorder:   left,  then print, then right                    │
│                                                                  │
│  Base case: node == null → return immediately.                   │
│                                                                  │
│  Time  — O(n): every node is visited exactly once.               │
│  Space — O(h): recursion stack depth = height of the tree.       │
│           O(log n) balanced, O(n) worst case (skewed tree).      │
└────────────────────────────────────────────────────────────┘
```

Ready for **LC 145: Binary Tree Postorder Traversal** whenever you are — and let me know if that transcript covers the iterative version too, since the pattern sheet flags iterative postorder as "the hardest" among the three DFS orders.