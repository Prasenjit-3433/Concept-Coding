# LC 270. Closest Binary Search Tree Value

Key Concept: Navigate — track closest seen; go left if target < root, right otherwise
Status: Pending

# Stage 1: Identification

### **Step 1 — Which topic?**

You're given the root of a **Binary Search Tree** and a floating-point `target` value. You need to return the value in the BST that is **closest** to `target` (closest in terms of absolute difference).

### **Step 2 — Which pattern?**

Still **Pattern 3: BST Navigation**. The trigger:

> "closest value", "closest binary search tree value" → **BST Navigation**
> 

### **Step 3 — Which key concept?**

**BST Compass with a Running "Closest Seen" Tracker — navigate using the ordering property, updating a candidate at every node, never needing to check both children.**

This reuses the exact same directional-decision machinery as LC 700 (Search) — at every node, one comparison against `target` tells you which single direction could possibly hold something closer. The only new ingredient is that instead of stopping at an exact match (there may be no exact match at all, since `target` is a float), you keep a **running best candidate**, updated at every node you pass through, and let the walk naturally terminate at `null`.

---

# Stage 2: Intuition Building

### The Tree We're Working With

```
              4
            /   \
           2     5
          / \
         1   3
```

`target = 3.714286`

### The Core Question — Why Can You Ignore an Entire Subtree?

Stand at node `4`. Compare `target (3.71...)` against `4`. Since `target < 4`, ask the same question LC 700 already trained you to ask:

> *"Does this comparison tell me anything about an entire subtree, without looking inside it?"*
> 

Yes. Everything in `4`'s right subtree is `> 4`, and `target` is `< 4`. Could something in the right subtree still be *closer* to `target` than `4` itself is? No — every value there is **at least as far** from `target` as `4` is (since they're all bigger than `4`, and `4` is already bigger than `target`). So the right subtree is provably useless — not just "unlikely to help," but *mathematically incapable* of producing a better answer than what you already have at this node. Move left.

```
┌──────────────────────────────────────────────────────────────────────┐
│  target < node.val  →  node itself is a candidate. Anything          │
│                         in the RIGHT subtree is even FARTHER         │
│                         from target than node.val is (since          │
│                         it's all > node.val > target).               │
│                         → go LEFT, right subtree is dead weight      │
│                                                                      │
│  target > node.val  →  mirror image → go RIGHT, left subtree         │
│                         is dead weight                               │
│                                                                      │
│  target == node.val →  exact match, this IS the closest value        │
│                         (difference of 0, nothing can beat it)       │
└──────────────────────────────────────────────────────────────────────┘
```

This is the exact same one-comparison-eliminates-a-subtree logic from LC 700's search — just applied to "closeness" instead of "equality."

### Why You Must Track a Candidate at *Every* Node, Not Just the Last One

Unlike LC 700, you can't just walk until you hit `null` and report whatever you last stood on — the node you happen to stop at (right before hitting `null`) is not guaranteed to be the closest one you passed through. You need to compare **every node you visit** against your best-so-far candidate, because "closest" isn't a property that only shows up at the end of the walk — any node along the path could be the winner.

### Walking Through the Example

```
              4
            /   \
           2     5
          / \
         1   3

target = 3.714286
```

```
Start: closest = 4 (root, by default — nothing better seen yet)

At node 4: |4 - 3.71| = 0.286
           closest = 4 (best so far, difference 0.286)
           target(3.71) < 4 → go LEFT (right subtree of 4, i.e. node 5,
           is provably farther — 5 > 4 > target, so |5-target| > |4-target|)

At node 2: |2 - 3.71| = 1.71
           1.71 > 0.286 → NOT better → closest stays 4
           target(3.71) > 2 → go RIGHT (left subtree of 2 is provably
           farther — everything there is < 2 < target)

At node 3: |3 - 3.71| = 0.71... wait, |3 - 3.714286| = 0.714286
           0.714286 > 0.286 → NOT better → closest stays 4
           target(3.71) > 3 → go RIGHT → node 3 has no right child → null

Walk ends (hit null). Final answer: closest = 4
```

**Closest value = 4.**

Notice node `1` and node `5` were **never visited** — pruned away exactly like LC 700's search, purely from directional comparisons. Only three nodes were ever touched, out of five.

### Why "First Node Encountered" Is a Safe Initial Candidate

You don't need any special sentinel value (like `+infinity`) to start — the very first node you visit (the root) is always a perfectly valid initial candidate. Every subsequent node either beats it or doesn't; the comparison logic handles this uniformly from the first step, no special-casing needed.

### Why the Recursion/Loop Terminates and Is O(height)

Exactly the same argument as LC 700: every step moves to exactly **one** child — never both — so the walk follows a single root-to-leaf(ish) path, bounded by the tree's height `h`. The walk stops the moment it runs off into `null`, at which point the best candidate seen along that single path is guaranteed to be the answer, precisely because every node *not* visited was provably farther away than some node that *was* visited.

```
┌──────────────────────────────────────────────────────────────────────┐
│  Same O(h) shape as LC 700's search — the only difference is         │
│  WHAT triggers "keep going": instead of stopping on an exact         │
│  match, you keep a running best candidate and update it at           │
│  every node, letting the walk run all the way to null.               │
└──────────────────────────────────────────────────────────────────────┘
```

---

# Stage 3: Coding

### Brute Force — Ignore the BST Property, Check Every Node

**The honest (wasteful) thinking:**

> "Traverse the whole tree, any order, compare every single node's value against target, keep the closest one."
> 

```java
class Solution {
    private double closestDiff = Double.MAX_VALUE;
    private int closestVal = 0;

    public int closestValue(TreeNode root, double target) {
        traverse(root, target);
        return closestVal;
    }

    private void traverse(TreeNode node, double target) {
        if (node == null) return;

        double diff = Math.abs(node.val - target);
        if (diff < closestDiff) {
            closestDiff = diff;
            closestVal = node.val;
        }

        // explores BOTH children unconditionally — ignores the
        // BST ordering property entirely
        traverse(node.left, target);
        traverse(node.right, target);
    }
}
```

This works on *any* binary tree, BST or not — the tell that it isn't using the ordering guarantee. Worst case, it visits every node: **O(n)**.

---

### Optimal — BST Compass with Running Closest Candidate

**Mental workflow before writing a single line:**

```
1. closest = root.val (always a valid starting candidate)

2. curr = root
3. While curr is not null:
   → if |curr.val - target| < |closest - target|: closest = curr.val
   → if target < curr.val: move to curr.left
     (right subtree of curr is provably farther — skip it)
   → else: move to curr.right
     (left subtree of curr is provably farther — skip it)

4. Return closest
```

#### Iterative Version

```java
class Solution {
    public int closestValue(TreeNode root, double target) {
        int closest = root.val;
        TreeNode curr = root;

        while (curr != null) {
            // Update the running best candidate — every node
            // visited is a genuine candidate, not just the last one
            if (Math.abs(curr.val - target) < Math.abs(closest - target)) {
                closest = curr.val;
            }

            // BST compass: one comparison eliminates an entire
            // subtree, exactly like LC 700's search
            if (target < curr.val) {
                curr = curr.left;
            } else {
                curr = curr.right;
            }
        }

        return closest;
    }
}
```

#### Recursive Version (equivalent logic)

```java
class Solution {
    public int closestValue(TreeNode root, double target) {
        return solve(root, target, root.val);
    }

    private int solve(TreeNode node, double target, int closest) {
        if (node == null) return closest;

        if (Math.abs(node.val - target) < Math.abs(closest - target)) {
            closest = node.val;
        }

        // Only ONE recursive call is ever made per level —
        // never both — same pruning as the iterative version
        if (target < node.val) {
            return solve(node.left, target, closest);
        } else {
            return solve(node.right, target, closest);
        }
    }
}
```

---

## Workflow Trace on the Example

```
              4
            /   \
           2     5
          / \
         1   3

target = 3.714286
```

```
closest = 4 (initial)

curr = 4: |4-3.71|=0.286 < |4-3.71|=0.286? No (equal, no update)
          target(3.71) < 4 → curr = 2

curr = 2: |2-3.71|=1.71 < 0.286? No → closest stays 4
          target(3.71) > 2 → curr = 3

curr = 3: |3-3.71|=0.71 < 0.286? No → closest stays 4
          target(3.71) > 3 → curr = null (3 has no right child)

Loop ends. Return closest = 4.
```

Matches Stage 2's manual trace exactly.

---

## Complexity Analysis

**Time Complexity — O(h), where h is the height of the tree:**

At every node visited, the work is O(1) — one absolute-difference comparison, one directional comparison — and the walk moves to exactly one child, never both. Total nodes touched equals the length of a single root-to-`null` path.

- **Balanced BST:** `h = O(log n)`.
- **Worst case (skewed BST):** `h = O(n)`.

**Space Complexity:**

- **Iterative — O(1):** just a couple of pointer/primitive variables reassigned each step.
- **Recursive — O(h):** one stack frame per level of the single path walked.

---

## Comparing Brute Force vs Optimal

| Property | Brute Force | Optimal (BST Compass) |
| --- | --- | --- |
| Uses BST ordering? | No | Yes — prunes an entire subtree at every step |
| Children explored per node | Both | Exactly one |
| Time | O(n) | O(h) → O(log n) typical, O(n) worst case (skewed) |
| Space | O(h) recursion stack | O(1) iterative, O(h) recursive |

---

## The Key Takeaway

```
┌───────────────────────────────────────────────────────────────────────┐
│  At any node, comparing target against node.val tells you             │
│  which ENTIRE subtree can be safely discarded:                        │
│                                                                       │
│      target < node.val  →  everything in the RIGHT subtree is         │
│                             even farther from target than             │
│                             node.val is → go LEFT                     │
│                                                                       │
│      target > node.val  →  mirror image → go RIGHT                    │
│                                                                       │
│  Unlike LC 700, there's no "exact match" stopping condition —         │
│  target may not exist in the tree at all (it's a float). So a         │
│  RUNNING closest candidate must be updated at EVERY node              │
│  visited, not just checked at the end.                                │
│                                                                       │
│  Same O(h) shape, same one-direction-per-step pruning as every        │
│  other BST Navigation problem — only what you TRACK differs.          │
│                                                                       │
│  Time  — O(h): O(log n) typical, O(n) worst case (skewed BST).        │
│  Space — O(1) iterative, O(h) recursive (call stack).                 │
└───────────────────────────────────────────────────────────────────────┘
```