# LC 235. LCA in a BST

Key Concept: Both < root → go left; both > root → go right; else root is LCA
Solution: https://www.youtube.com/watch?v=cX_kPV_foZc&list=PLgUwDviBIf0q8Hkd7bK2Bpryj2xVJk8Vk&index=48&ab_channel=takeUforward
Status: Done

# Stage 1: Identification

### **Step 1 — Which topic?**

You're given the root of a **Binary Search Tree** and two nodes `p` and `q` (guaranteed to exist in the tree). You need to find their **Lowest Common Ancestor** — the deepest node that has both `p` and `q` as descendants (a node is allowed to be its own descendant).

### **Step 2 — Which pattern?**

This opens **Pattern 3: BST Navigation**. Unlike LC 700/701/450 (Pattern 1), you're not searching for or modifying a single value — you're navigating toward a specific **relationship** between two values, using the BST ordering property to decide direction at every step. The trigger:

> "LCA", "lowest common ancestor", in a BST specifically → **BST Navigation**
> 

### **Step 3 — Which key concept?**

**BST Compass, applied to two values simultaneously — the point where the paths to `p` and `q` diverge IS the LCA.**

You already know from LC 700 that a BST lets you eliminate one entire subtree at every node using a single comparison. This problem reuses that exact compass, but now you're tracking **two** target values at once instead of one, and watching for the specific moment their directions stop agreeing.

---

# Stage 2: Intuition Building

### What Does "LCA" Actually Mean, Concretely?

If you draw the path from the root down to `p`, and separately draw the path from the root down to `q`, both paths start at the same place (the root) and, at some point, one of two things happens: either they eventually split apart into different directions, or one path is entirely contained inside the other (because one of the two nodes *is* an ancestor of the other). The **LCA** is the last node the two paths still have in common — the exact point right before they'd go their separate ways.

### The Tree We're Working With

```
              10
            /    \
           4      20
          / \
         2   8
        / \  / \
       1  3 5   9
```

### The Core Question — What Does a Node Being "Between" `p` and `q` Actually Tell You?

Stand at any node in the tree and ask:

> *"Relative to THIS node's value, where do `p` and `q` sit — both smaller, both bigger, or split across both sides?"*
> 

Because this is a BST, that single comparison at the current node tells you, with certainty, whether the LCA could possibly be somewhere further down, or whether it must be **right here**.

```
┌────────────────────────────────────────────────────────────────┐
│  Both p and q < current node's value                                 │
│      → LCA must be somewhere in the LEFT subtree                     │
│      → move left, this node cannot be the answer                     │
│                                                                      │
│  Both p and q > current node's value                                 │
│      → LCA must be somewhere in the RIGHT subtree                    │
│      → move right, this node cannot be the answer                    │
│                                                                      │
│  Otherwise (p and q sit on OPPOSITE sides, or the current node       │
│  itself equals p or q)                                               │
│      → this is exactly the point where the two paths diverge —       │
│        THIS node is the LCA. Stop here.                              │
└────────────────────────────────────────────────────────────────┘
```

Why does "both smaller" guarantee the LCA is entirely in the left subtree? Because the BST invariant says everything larger than the current node lives in the right subtree — so if both `p` and `q` are smaller, neither of them, nor any node connecting them, can possibly live to the right. The entire relationship between `p` and `q` — and therefore their LCA — must be findable somewhere within the left subtree alone. Same reasoning, mirrored, for "both greater."

### Tracing LCA(5, 9) on the Example Tree

```
At node 10:  5 < 10 and 9 < 10 → both smaller
             → LCA is somewhere in the left subtree → move to 4

At node 4:   5 > 4 and 9 > 4 → both bigger
             → LCA is somewhere in the right subtree → move to 8

At node 8:   5 < 8 and 9 > 8 → SPLIT — one smaller, one bigger
             → this is the exact point the paths to 5 and to 9
               diverge → node 8 IS the LCA. Stop.
```

**LCA(5, 9) = 8.**

Notice the path to `5` (`10 → 4 → 8 → 5`) and the path to `9` (`10 → 4 → 8 → 9`) share the prefix `10 → 4 → 8` and then split — exactly matching node `8` as the last shared node.

### Tracing LCA(1, 2) — The Case Where One Node Is the Other's Ancestor

```
At node 10:  1 < 10 and 2 < 10 → both smaller → move to 4

At node 4:   1 < 4 and 2 < 4 → both smaller → move to 2

At node 2:   Is 1 < 2 and 2 < 2? No — 2 is NOT less than 2 itself.
             Is 1 > 2 and 2 > 2? No either.
             You cannot say "both smaller" or "both bigger" —
             the current node's own value IS one of the targets.
             → this node itself is the LCA. Stop.
```

**LCA(1, 2) = 2.**

This case falls out of the exact same rule without needing a separate check: the moment you can no longer cleanly say "both smaller" or "both bigger," you've found the LCA — whether that's because the paths genuinely diverge in two different directions, or because you've walked directly onto one of the two target nodes itself (which happens precisely when that node is an ancestor of the other).

### Why You Never Need to Check Both Subtrees

At every step, exactly one of three things is true: both targets are smaller (go left), both are bigger (go right), or neither (stop, you're at the LCA). There's no scenario requiring you to explore both children — the BST ordering already tells you, before you move anywhere, which single direction could possibly contain the answer. This is exactly the same one-comparison-eliminates-a-subtree idea from LC 700's search, just applied to two values simultaneously instead of one.

### Why This Terminates and Is O(height)

Every step moves to exactly one child — never both, never back up. Since `p` and `q` are guaranteed to exist in the tree, the walk is guaranteed to reach a genuine split point (or hit one of the targets directly) before running out of tree — you never walk all the way to `null`. The number of steps taken is bounded by the tree's height, `h`, since you only ever follow one root-to-node path.

```
┌────────────────────────────────────────────────────────────────┐
│  Plain binary tree LCA (no ordering guarantee): O(n)                 │
│      → must potentially search both subtrees at every node           │
│                                                                      │
│  BST LCA (ordering guarantee at every node): O(h)                    │
│      → one comparison of p and q against the current node            │
│        tells you the single direction to go, or that you've          │
│        already arrived                                               │
└────────────────────────────────────────────────────────────────┘
```

---

# Stage 3: Coding

## Approach — BST Compass Applied to Two Values

**Mental workflow before writing a single line:**

```
1. Start at the root.

2. At the current node:
   → if BOTH p.val and q.val are less than current node's value:
       move to the left child, continue
   → if BOTH p.val and q.val are greater than current node's value:
       move to the right child, continue
   → otherwise (values split across both sides, OR the current
     node's value equals p.val or q.val):
       this node IS the LCA — stop and return it
```

Both recursive and iterative versions implement this identical logic — they only differ in *how* "move to the child and repeat" is expressed.

---

### Recursive Version

```java
class Solution {
    public TreeNode lowestCommonAncestor(TreeNode root, TreeNode p, TreeNode q) {
        // Base case: ran out of tree (shouldn't happen given the
        // problem's guarantee that p and q exist, but safe to keep)
        if (root == null) return null;

        // Both targets are smaller than the current node —
        // the LCA can only live in the left subtree
        if (p.val < root.val && q.val < root.val) {
            return lowestCommonAncestor(root.left, p, q);
        }

        // Both targets are bigger than the current node —
        // the LCA can only live in the right subtree
        if (p.val > root.val && q.val > root.val) {
            return lowestCommonAncestor(root.right, p, q);
        }

        // Neither "both smaller" nor "both bigger" holds — either
        // the targets split across both sides, or the current node
        // itself IS one of the targets. Either way, this is the
        // exact point where the two paths diverge — this is the LCA.
        return root;
    }
}
```

---

### Iterative Version

```java
class Solution {
    public TreeNode lowestCommonAncestor(TreeNode root, TreeNode p, TreeNode q) {
        TreeNode curr = root;

        while (curr != null) {
            if (p.val < curr.val && q.val < curr.val) {
                // Both smaller — move left, nothing to track,
                // just reassign the pointer and keep going
                curr = curr.left;
            } else if (p.val > curr.val && q.val > curr.val) {
                // Both bigger — move right
                curr = curr.right;
            } else {
                // Split point (or curr itself is p or q) — this
                // is the LCA. Stop immediately.
                return curr;
            }
        }

        return null; // unreachable given the problem's guarantee
    }
}
```

Read the loop exactly as the recursive version's logic, just without call-stack frames: keep reassigning `curr` while the compass says "both smaller" or "both bigger," and the moment it says neither, `curr` itself is the answer — return it directly, no need to keep walking.

---

## Workflow Trace on the Example Tree — LCA(5, 9)

```
              10
            /    \
           4      20
          / \
         2   8
        / \  / \
       1  3 5   9
```

```
curr = 10:  p=5, q=9. Is 5<10 and 9<10? Yes → move left → curr = 4

curr = 4:   Is 5<4 and 9<4? No.
            Is 5>4 and 9>4? Yes → move right → curr = 8

curr = 8:   Is 5<8 and 9<8? No (9 is not < 8).
            Is 5>8 and 9>8? No (5 is not > 8).
            Neither holds → curr (8) IS the LCA → return 8
```

**Answer: node 8.** Matches Stage 2's traced result exactly.

---

## Comparing Recursive vs Iterative

```
┌────────────────────────────────────────────────────────────────┐
│  Both versions:                                                      │
│    → visit exactly ONE node per level — never explore both           │
│      children                                                        │
│    → same O(h) TIME complexity                                       │
│                                                                      │
│  Recursive:                                                          │
│    → each call adds a stack frame, unwound only once the             │
│      base case (split point / null) is hit                           │
│    → SPACE: O(h) — call stack proportional to tree height            │
│                                                                      │
│  Iterative:                                                          │
│    → no call stack — just a pointer reassigned in a loop             │
│    → SPACE: O(1)                                                     │
└────────────────────────────────────────────────────────────────┘
```

---

## Complexity Analysis

**Time Complexity — O(h), where h is the height of the tree:**

At every node visited, the work is O(1) — two comparisons — and the walk moves to exactly one child, never both, until a split point (or one of the targets) is reached. The total nodes touched equals the length of a single root-to-LCA path, bounded by the tree's height.

- **Balanced BST:** `h = O(log n)`.
- **Worst case (skewed BST):** `h = O(n)`.

**Space Complexity:**

- **Iterative — O(1):** only a single pointer being reassigned, no extra structure.
- **Recursive — O(h):** one stack frame per level of the single path walked, unwound as soon as the base case fires.

---

## Comparing This to LC 700 (Search)

| Property | LC 700 (Search) | LC 235 (LCA in BST) |
| --- | --- | --- |
| Values tracked | One target | Two targets simultaneously |
| Stopping condition | Exact match, or `null` | Values split across both sides, or current node equals one target |
| What triggers "found" | `root.val == val` | "Both smaller" and "both bigger" both fail to hold |
| Core mechanism | BST Compass | Same BST Compass, applied to two values at once |

---

## The Key Takeaway

```
┌─────────────────────────────────────────────────────────────────┐
│  At any node, compare p and q against the current node's value:       │
│                                                                       │
│      both p, q < current  →  LCA is in the LEFT subtree,              │
│                               move left, this node is NOT it          │
│                                                                       │
│      both p, q > current  →  LCA is in the RIGHT subtree,             │
│                               move right, this node is NOT it         │
│                                                                       │
│      otherwise             →  paths diverge HERE (or current          │
│                               node IS p or q) → THIS is the LCA       │
│                                                                       │ 
│  "Otherwise" silently covers BOTH real cases — a genuine split        │
│  across left/right, AND one target being an ancestor of the           │
│  other — no separate check needed for either.                         │
│                                                                       │
│  Only ONE direction is ever explored per step — never both —          │
│  because the BST ordering already tells you where the answer          │
│  must be before you move.                                             │
│                                                                       │
│  Time  — O(h): O(log n) balanced, O(n) worst case (skewed).           │
│  Space — O(1) iterative, O(h) recursive (call stack).                 │
└─────────────────────────────────────────────────────────────────┘
```