# LC 145. Binary Tree Postorder Traversal

Key Concept: Iterative is hardest of three — two-stack or reverse-preorder trick
Solution: https://www.youtube.com/watch?v=COQOU6klsBg&list=PLgUwDviBIf0q8Hkd7bK2Bpryj2xVJk8Vk&index=8&ab_channel=takeUforward
Status: Done

# LC 145. Binary Tree Postorder Traversal

---

# Stage 1: Identification

**Step 1 — Which topic?**

You are given the root of a binary tree. You need to return the **postorder traversal** of its node values — visit the left subtree first, then the right subtree, then the root.

**Step 2 — Which pattern?**

This is **Pattern 1: Tree Traversals (Core Templates)** — the third and final DFS order, same pattern as preorder and inorder.

**Step 3 — Which key concept?**

**Left → Right → Root — same recursive shape as the other two, root just moves to the very end.**

At any node, do three things in this exact order:

1. Recurse into the left subtree
2. Recurse into the right subtree
3. Process (print/collect) the current node

---

# Stage 2: Intuition Building

### Why This Is Still "Don't Complicate It"

Nothing new needs to be invented here. The same recursive shape from preorder and inorder applies — go to a subtree, do the required task there, come back. Whatever the traversal name says on paper — left, right, root — you write down in that exact order in the code. The only thing that has moved is the position of the print step: now it sits **after** both recursive calls instead of before them or in between.

### The Rule, Stated Plainly

At any node:

```
1. Apply this exact same rule to the left   →  LEFT
2. Apply this exact same rule to the right  →  RIGHT
3. Visit (print) the node itself            →  ROOT
```

"Postorder" means the root is printed **last** — only after everything in both its left and right subtrees has been fully explored and printed.

### The Base Case

Same as preorder and inorder:

```
If the current node is null → there is nothing to visit and nothing to recurse into.
Just return.
```

### Walking Through the Shape (Not a Dry Run — Just the Idea)

Picture the same single traveler standing at the root, following identical instructions at every node it visits:

```
Stand at a node
   → Walk into left child, follow the SAME instructions there
   → Come back, walk into right child, follow the SAME instructions there
   → Come back — NOW print the current node
   → Walk back to whoever sent you here
```

Because the traveler always finishes the **entire left subtree**, then the **entire right subtree**, and only prints the current node once both of those are completely done, the values naturally come out in left, right, root order — at every level of the tree, not just the top.

### Why the Recursion Eventually Terminates

Same reasoning as before: every recursive call moves strictly downward to a child, the tree is finite with no cycles, so every path eventually reaches null. The base case catches that immediately and returns — which is what lets each node's print statement finally execute once its left and right calls have both unwound.

---

# Stage 3: Coding

## Approach — Recursive (The Only Natural Approach Here)

```java
class Solution {
    public List<Integer> postorderTraversal(TreeNode root) {
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

        // Step 2: RIGHT — fully explore the right subtree next
        solve(node.right, result);

        // Step 3: ROOT — process the current node only after
        // BOTH left and right subtrees are completely done
        result.add(node.val);
    }
}
```

The only difference from preorder/inorder's code: the line that adds `node.val` to the result has moved all the way to the **end**, after both recursive calls instead of before them or between them.

---

## Complexity Analysis

**Time Complexity — O(n):**

Every node is visited exactly once — the recursion calls `solve` once per node, does a constant amount of work there (add it to the list), and moves on. With `n` total nodes, each processed in O(1) time, the total time is **O(n)**.

**Space Complexity — O(h), where h is the height of the tree:**

The only space that matters is the **recursion call stack** — at any point during execution, it holds one frame for every node on the path from the root down to the current position. That depth equals the height of the tree, `h`.

- **Best case (balanced tree):** height is `O(log n)`, so auxiliary space is `O(log n)`.
- **Worst case (skewed tree — every node has only one child):** height becomes `O(n)`, since the tree degenerates into a straight line, and the recursion stack holds all `n` nodes at once.

So: **O(h) auxiliary space — O(log n) for a balanced tree, O(n) in the worst case (skewed tree)**.

---

## The Key Takeaway

```
┌─────────────────────────────────────────────────────────────┐
│  Postorder = Left → Right → Root.                                │
│                                                                  │
│  Same recursive shape as preorder and inorder — don't            │
│  complicate it. Only the POSITION of the print step changes:     │
│      Preorder:   print, then left,  then right                   │
│      Inorder:    left,  then print, then right                   │
│      Postorder:  left,  then right, then print                   │
│                                                                  │
│  Base case: node == null → return immediately.                   │
│                                                                  │
│  Time  — O(n): every node is visited exactly once.               │
│  Space — O(h): recursion stack depth = height of the tree.       │
│           O(log n) balanced, O(n) worst case (skewed tree).      │
└─────────────────────────────────────────────────────────────┘
```

This transcript only covered the recursive version. Ready for the iterative traversals (or the next problem in the pattern sheet, **GFG: All Three Traversals Using Single Stack**) whenever you send the transcript.