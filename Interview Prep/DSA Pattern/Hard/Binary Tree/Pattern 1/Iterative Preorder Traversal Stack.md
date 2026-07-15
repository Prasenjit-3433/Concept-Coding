# Iterative Preorder Traversal | Stack

Solution: https://www.youtube.com/watch?v=Bfqd8BsPVuw&list=PLgUwDviBIf0q8Hkd7bK2Bpryj2xVJk8Vk&index=10&ab_channel=takeUforward
Status: Done

# Iterative Preorder Traversal | Stack

---

# Stage 1: Identification

**Step 1 — Which topic?**

You are given the root of a binary tree. You need to return the **preorder traversal** — root, then left, then right — but this time using an **explicit stack** instead of recursion.

**Step 2 — Which pattern?**

This is still **Pattern 1: Tree Traversals (Core Templates)** — same traversal order as LC 144, just a different mechanism for achieving it.

**Step 3 — Which key concept?**

**Simulate the call stack manually — push right before left.**

Recursion works because the language maintains a hidden call stack for you, remembering "what to do next" at every level. To do preorder without recursion, you maintain that stack **yourself**, explicitly, using a `Stack` data structure. The one detail that makes this work correctly: whenever a node has both children, you must **push the right child before the left child** — so that when you pop from the stack, the left child comes out first.

*(Note: this note is written from the standard, well-known iterative-stack technique for preorder traversal — not reconstructed from a specific transcript, since the transcript provided for this lecture wasn't usable.)*

---

# Stage 2: Intuition Building

### Why Recursion Needs a Stack, and Why We Can Replace It With One

In the recursive version, every call to `solve(node)` sits on the call stack until both its left and right recursive calls finish. That stack is what lets the program "remember" where to come back to. An iterative version has no such automatic memory — so we bring our own `Stack<TreeNode>` to play the exact same role: it holds the nodes we still owe a visit to, in the order we should visit them.

### The Core Idea

For preorder, the moment you arrive at a node, you must print it **immediately** — there's no waiting involved, unlike inorder or postorder. So the loop is simple:

```
1. Pop a node from the stack.
2. Print it — it's preorder, so this always happens right away.
3. Push its RIGHT child (if it exists).
4. Push its LEFT child (if it exists).
5. Repeat until the stack is empty.
```

### Why Right Gets Pushed Before Left

A stack is Last-In-First-Out — whatever goes in last comes out first. Since we want to process **left before right** (that's the definition of preorder), the left child needs to come out of the stack **first**. That means left has to go in **last** — i.e., right must be pushed before left.

If you get this backwards and push left before right, you'd pop right first on the next iteration, and the whole traversal order would be reversed for every node with two children.

### Walking Through the Shape (Not a Dry Run — Just the Idea)

Picture the stack as a "to-do pile" of nodes waiting to be visited:

```
Start: push the root. Stack = [root]

Loop:
   Pop the top of the stack → this is the next node to print
   Print it
   Push its right child (goes in first, sits deeper)
   Push its left child (goes in last, sits on top)
      → left will be popped next, exactly as preorder requires

Continue until the stack is empty.
```

Every time you pop a node, you are effectively doing the same "visit, then queue up children" step that the recursive version's call stack did automatically for you — except now the pushing order (right before left) is what enforces "left gets visited before right."

### Why This Terminates

Every node is pushed onto the stack exactly once (either as the initial root, or as somebody's left/right child), and every iteration of the loop pops exactly one node off. So the loop runs exactly `n` times for a tree with `n` nodes, and then the stack is empty and the loop ends.

---

# Stage 3: Coding

## Approach — Iterative with a Single Stack

```java
class Solution {
    public List<Integer> preorderTraversal(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        if (root == null) return result;

        Stack<TreeNode> stack = new Stack<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            // Pop the next node to visit
            TreeNode node = stack.pop();

            // Preorder: visit immediately, no waiting
            result.add(node.val);

            // Push RIGHT first, then LEFT
            // WHY: stack is LIFO — whatever is pushed last comes out first.
            //      We want LEFT to be processed before RIGHT, so LEFT
            //      must be pushed AFTER right, so it sits on top.
            if (node.right != null) {
                stack.push(node.right);
            }
            if (node.left != null) {
                stack.push(node.left);
            }
        }

        return result;
    }
}
```

---

## Complexity Analysis

**Time Complexity — O(n):**

Every node is pushed onto the stack exactly once and popped exactly once. Each push/pop is O(1), and there are `n` nodes total, so the total work is **O(n)**.

**Space Complexity — O(n) in the worst case:**

Unlike the recursive version — where auxiliary space was bounded by the **height** of the tree — here the stack can hold up to roughly one full level's worth of nodes at a time, and in the worst case (a tree shaped so that many right children pile up before being popped, such as a completely skewed tree), the stack can grow to hold up to `O(n)` nodes. So the auxiliary space is **O(n)** in the worst case, which is actually looser than the recursive version's `O(h)` — the explicit stack here isn't guaranteed to only ever hold one root-to-node path's worth of nodes the way the call stack does, since both children get pushed before either is fully explored.

---

## Comparing Recursive vs Iterative Preorder

| Property | Recursive (LC 144) | Iterative (this note) |
| --- | --- | --- |
| Mechanism | Language's implicit call stack | Explicit `Stack<TreeNode>` you manage yourself |
| Order enforced by | Order of statements in code | Order of pushes (right before left) |
| Space | O(h) — height of tree | O(n) — worst case can exceed height |
| Code length | 3 lines | ~10 lines, but no recursion |

---

## The Key Takeaway

```
┌─────────────────────────────────────────────────────────────┐
│  Iterative Preorder = simulate the call stack yourself.          │
│                                                                  │
│  Loop:                                                           │
│      pop → visit immediately → push right → push left            │
│                                                                  │
│  WHY right before left:                                          │
│  Stack is LIFO. We want left processed first, so left must       │
│  go in LAST — meaning right is pushed first, left pushed after.  │
│                                                                  │
│  Time  — O(n): every node pushed and popped exactly once.        │
│  Space — O(n) worst case: the explicit stack can hold more       │
│           nodes at once than the recursive call stack would.     │
└─────────────────────────────────────────────────────────────┘
```

Ready for **Iterative Inorder Traversal** or **Iterative Postorder Traversal** whenever you have a transcript — those two are trickier than preorder (inorder needs a "go left as far as possible" loop, and postorder is flagged in the pattern sheet as the hardest of the three), so a clean transcript would help me stay faithful to Striver's specific approach there, especially for postorder where there's more than one common technique (two-stack vs. one-stack-with-a-visited-marker).