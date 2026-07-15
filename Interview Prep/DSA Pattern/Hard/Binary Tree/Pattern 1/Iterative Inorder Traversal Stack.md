# Iterative Inorder Traversal | Stack

Solution: https://www.youtube.com/watch?v=lxTGsVXjwvM&list=PLgUwDviBIf0q8Hkd7bK2Bpryj2xVJk8Vk&index=11&ab_channel=takeUforward
Status: Done

# Iterative Inorder Traversal | Stack

---

# Stage 1: Identification

**Step 1 — Which topic?**

You are given the root of a binary tree. You need to return the **inorder traversal** — left, root, right — but without using recursion, since an interviewer may explicitly ask for a non-recursive solution.

**Step 2 — Which pattern?**

Still **Pattern 1: Tree Traversals (Core Templates)** — same order as LC 94, different mechanism.

**Step 3 — Which key concept?**

**Reuse the exact same stack the recursion was already building — just build it yourself.**

The core realization: in the recursive version, when you kept calling `inorder(node.left)`, the nodes you passed through on the way down (root, then its left child, then that child's left child, and so on) were sitting on the **recursion's own call stack** the entire time, waiting for their turn to be printed once the leftmost node was reached. An iterative solution doesn't get that call stack for free — so you create a `Stack` yourself and push nodes onto it in exactly the same way, as you walk as far left as possible before doing anything else.

---

# Stage 2: Intuition Building

### What the Recursive Version Was Actually Doing

In the recursive version, the flow at any point was: keep going left, left, left — and every node you pass through along the way effectively "waits" until you hit a null. Only then does the most recently visited node get printed, and the process backtracks one step to check its right subtree. Those waiting nodes — the ones stored implicitly by nested function calls — are exactly what a stack captures explicitly.

### The Core Loop

Maintain two things: a `Stack<TreeNode>`, and a variable `node` that always points to "where I currently am," starting at the root.

```
Repeat:
    While node is not null:
        Push node onto the stack
        Move node to node.left
        (this mirrors "keep going left" from the recursive version)

    Once node becomes null:
        There is nothing further left to explore from here —
        pop the top of the stack. This popped node is the next
        one to print — the leftmost unvisited node so far.
        Print it.
        Then move node to (popped node).right —
        because after visiting a node, inorder says go right next.

Stop when the stack is empty AND node is null — nothing left to explore.
```

### Why "Go Left, Push Along the Way" Mirrors Recursion Exactly

Every time the recursive call went `inorder(node.left)`, the current node was effectively "on hold" until that entire left subtree finished. Pushing the node onto a stack before moving left captures precisely that same "on hold" state — the node sits there, waiting, until we come back for it. When we finally pop it, that's the equivalent of the recursive call returning back up to that node and executing its print statement.

### Why We Move Right After Popping and Printing

Inorder is left → root → right. Once a node has been popped and printed (its "root" step just happened), the very next thing to do — exactly as the recursive version's third line does — is explore its right subtree. So `node` is reassigned to the popped node's right child, and the outer loop's "push while going left" step takes over again from there — because the right child itself might have its own left subtree to burrow into first.

### Why the Stack Naturally Recreates the Correct Order

Because we always push while going as far left as possible, the node sitting on top of the stack is always the **deepest, leftmost node not yet printed**. Popping it gives exactly the next value that inorder demands. After printing it, moving to its right child and repeating "push while going left" correctly finds the next leftmost candidate in the remaining unvisited part of the tree — which is exactly what happens naturally in the recursive call stack, just made explicit here.

### Why This Terminates

Every node gets pushed onto the stack exactly once (the moment it's first reached while moving left) and popped exactly once (when it's finally its turn to be printed). Once every node has gone through this push-then-pop cycle, both the stack is empty and `node` has become null with nowhere left to go — so the loop stops there.

---

# Stage 3: Coding

## Approach — Iterative with a Single Stack

```java
class Solution {
    public List<Integer> inorderTraversal(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        Stack<TreeNode> stack = new Stack<>();
        TreeNode node = root;

        // Keep going until there's nothing left to explore
        // (no more nodes waiting on the stack, and nowhere left to move)
        while (true) {

            // Push while moving left — exactly mirrors the recursive
            // call chain going deeper and deeper into node.left
            if (node != null) {
                stack.push(node);
                node = node.left;
            } else {
                // node is null — nothing further left from here.
                // If the stack is also empty, we're completely done.
                if (stack.isEmpty()) break;

                // Pop the node that's been "waiting" — this is the
                // deepest leftmost node not yet printed
                node = stack.pop();

                // ROOT step: print it now
                result.add(node.val);

                // RIGHT step: move to its right child,
                // then the loop's "push while going left" handles
                // whatever lies within that right subtree
                node = node.right;
            }
        }

        return result;
    }
}
```

---

## Complexity Analysis

**Time Complexity — O(n):**

Every node is pushed onto the stack exactly once and popped exactly once, and each push/pop does O(1) work. With `n` nodes total, the total work is **O(n)** — every node is traversed exactly once.

**Space Complexity — O(n) in the worst case:**

The auxiliary stack space depends on how deep the "push while going left" chain can get before anything is popped. Consider a tree that is completely skewed to the left — every node only has a left child. In that case, every single node gets pushed onto the stack before the first pop ever happens, so the stack holds all `n` nodes at once. More generally, this bounds to the **height of the tree**: **O(height)** — which becomes `O(n)` in the worst case (a skewed tree) and `O(log n)` for a balanced tree.

---

## Comparing Recursive vs Iterative Inorder

| Property | Recursive (LC 94) | Iterative (this note) |
| --- | --- | --- |
| Mechanism | Language's implicit call stack | Explicit `Stack<TreeNode>` you manage yourself |
| What gets "stored" while going left | Call frames | The nodes themselves, pushed directly |
| When a node is printed | When its call frame resumes after the left call returns | When it's popped off the stack |
| Space | O(height) | O(height) — same bound, since the stack recreates the exact same call chain |

---

## The Key Takeaway

```
┌─────────────────────────────────────────────────────────────┐
│  Iterative Inorder = recreate the recursion's own call stack     │
│  yourself, explicitly, using a Stack<TreeNode>.                  │
│                                                                  │
│  Loop:                                                           │
│      while node != null: push node, move to node.left            │
│      when node == null: pop → print → move to (popped).right     │
│      stop when stack is empty AND node is null                   │
│                                                                  │
│  WHY this mirrors recursion exactly:                             │
│  Going left repeatedly and pushing along the way is identical    │
│  to the recursive calls "waiting" on the call stack. Popping     │
│  and printing is identical to a call frame resuming after its    │
│  left recursive call returns.                                    │
│                                                                  │
│  Time  — O(n): every node pushed and popped exactly once.        │
│  Space — O(height): worst case O(n) for a skewed tree,           │
│           O(log n) for a balanced tree.                          │
└─────────────────────────────────────────────────────────────┘
```

Ready for **Iterative Postorder Traversal** whenever you have that transcript — it's flagged as the hardest of the three, so it'll be good to see Striver's exact approach (whether he does the two-stack trick or the one-stack-with-tracking trick).