# LC 938. Range Sum of BST

Key Concept: Prune left if root.val ≤ low, prune right if root.val ≥ high
Problem: https://leetcode.com/problems/range-sum-of-bst/description/
Solution: https://www.youtube.com/watch?v=qIFhQ2m6i64
Status: Pending

# Stage 1: Identification

### **Step 1 — Which topic?**

You're given the root of a **Binary Search Tree**, and two integers `low` and `high`. You need to return the **sum of values** of all nodes whose value lies within the inclusive range `[low, high]`.

### **Step 2 — Which pattern?**

Still **BST Pattern 2: BST Properties & Validation**. The trigger:

> "range sum", "sum of nodes within [low, high]", in a BST → exploit the ordering property to **skip** entire subtrees, not just traverse everything
> 

### **Step 3 — Which key concept?**

**Prune left if `root.val ≤ low`, prune right if `root.val ≥ high`.**

This is a slightly different flavor of BST exploitation than LC 530/230 — those problems used inorder's *sorted* property. This one uses the more basic **directional pruning** idea from LC 700 (Search) and LC 235 (LCA): at any node, the BST ordering tells you, with certainty, whether an entire subtree can contain **zero** values in range — and if so, you skip it completely, without visiting a single node inside it.

---

# Stage 2: Intuition Building

### The Tree We're Working With

```
              10
            /    \
           5      15
          / \        \
         3   7        18

range = [7, 15]
```

### The Naive First Instinct — Visit Every Node, Check the Range

The most direct reading: traverse the whole tree with any traversal, and at every node, check `low <= node.val <= high` — if true, add it to the sum.

```
Visit 3:  3 in [7,15]?  No
Visit 5:  5 in [7,15]?  No
Visit 7:  7 in [7,15]?  Yes → sum += 7
Visit 10: 10 in [7,15]? Yes → sum += 10
Visit 15: 15 in [7,15]? Yes → sum += 15
Visit 18: 18 in [7,15]? No

Total = 7 + 10 + 15 = 32
```

This is correct — but it visits **every single node**, even ones that the BST ordering could have told you, in advance, are hopeless. Node `3`, for instance, sits in a region where you can already prove nothing useful can exist once you know a bit more about its neighborhood.

### The Core Question — What Does a Node's Value Tell You About Its *Subtrees*, Not Just Itself?

Stand at node `5` in the trace above, with range `[7, 15]`. Ask:

> *"Given that `5 < low (7)`, do I already know something about `5`'s entire left subtree, without looking at it?"*
> 

Yes. The BST invariant says everything in `5`'s left subtree is **smaller than 5**. Since `5` itself is already less than `low`, everything in that left subtree — being even smaller than `5` — is *also* less than `low`. Not one node in there could possibly fall inside `[low, high]`. The entire left subtree is **provably irrelevant** — you don't need to check it; you already know the answer.

```
┌───────────────────────────────────────────────────────────────────────┐
│  If root.val <= low:                                                  │
│      → root ITSELF might still be too small to count (check           │
│        it normally), but its LEFT subtree is guaranteed to be         │
│        entirely SMALLER than root.val, hence entirely < low           │
│      → PRUNE the left subtree — never recurse into it                 │
│                                                                       │
│  If root.val >= high:                                                 │
│      → root's RIGHT subtree is guaranteed to be entirely              │
│        BIGGER than root.val, hence entirely > high                    │
│      → PRUNE the right subtree — never recurse into it                │
└───────────────────────────────────────────────────────────────────────┘
```

This is exactly the same "one comparison eliminates an entire subtree" idea from LC 700's search — just applied to a *range* instead of a single target value, and instead of stopping the walk entirely, you only prune the *one* side that's provably useless while still exploring the other side (and possibly both, if the current node is safely inside the range).

### Tracing rangeSumBST on the Example, With Pruning

```
              10
            /    \
           5      15
          / \        \
         3   7        18

range = [7, 15]
```

```
solve(10): 10 is in [7,15] → contributes 10 to the sum
           10 > low(7) → left subtree NOT provably useless → recurse left
           10 < high(15) → right subtree NOT provably useless → recurse right

  solve(5): 5 is NOT in [7,15] (5 < 7) → contributes 0
            5 <= low(7) → left subtree of 5 is entirely < 5 <= 7
                        → PRUNE left, don't even look at node 3
            5 < high(15) → right subtree NOT provably useless → recurse right

    solve(7): 7 is in [7,15] → contributes 7
              7 <= low(7) → left subtree of 7 (there is none anyway) → prune
              7 < high(15) → recurse right → solve(null) → returns 0

  solve(15): 15 is in [7,15] → contributes 15
             15 > low(7) → recurse left → solve(null) → returns 0
             15 >= high(15) → right subtree of 15 is entirely > 15 >= 15
                             → PRUNE right, don't even look at node 18

Total = 10 + 7 + 15 = 32
```

**Same answer as brute force — 32 — but node `3` and node `18` were never even visited.** For a larger, deeper tree, this pruning compounds heavily: entire branches with thousands of nodes can be skipped in one comparison, the same way LC 700's search discards half the tree at every step.

### Why This Still Naturally Falls Out of a Simple Recursive Shape

Nothing exotic is happening mechanically. It's the same base-case-then-combine recursion used throughout this pattern — base case `node == null → contributes 0`, and at every node you decide, via two simple comparisons, which of the (at most two) recursive calls are even worth making. The only new idea compared to a plain traversal is that a call can be skipped entirely — not just have its result discarded, but never made at all — whenever the BST ordering already proves it would find nothing.

```
┌───────────────────────────────────────────────────────────────────────┐
│  solve(node):                                                         │
│      if node is null → return 0                                       │
│                                                                       │
│      sum = 0                                                          │
│      if low <= node.val <= high: sum += node.val                      │
│                                                                       │
│      if node.val > low:  sum += solve(node.left)   ← only if          │
│                                                        NOT proven     │
│                                                        useless        │
│      if node.val < high: sum += solve(node.right)  ← same idea        │
│                                                                       │
│      return sum                                                       │
└───────────────────────────────────────────────────────────────────────┘
```

### Why This Terminates and Is Never Worse Than O(n)

Every recursive call still either hits `null` or moves to a child, so the recursion terminates for the same reason every tree recursion does — the tree is finite with no cycles. In the worst case (e.g., `[low, high]` covers the entire range of values in the tree), no pruning happens at all, and every node gets visited — so this approach is never worse than the brute force's `O(n)`. But whenever the range is narrower than the tree's full value spread, pruning kicks in and skips real work, something the naive traversal could never do.

---

# Stage 3: Coding

## Approach 1 — Brute Force: Visit Every Node, Filter By Range

```java
class Solution {
    public int rangeSumBST(TreeNode root, int low, int high) {
        if (root == null) return 0;

        int sum = 0;
        if (root.val >= low && root.val <= high) {
            sum += root.val;
        }

        // Recurses into BOTH children unconditionally — ignores
        // the BST property entirely, just like LC 700's brute force
        sum += rangeSumBST(root.left, low, high);
        sum += rangeSumBST(root.right, low, high);

        return sum;
    }
}
```

This is correct — it works on *any* binary tree, BST or not — but that's exactly the tell that it's not exploiting the BST guarantee. Worst case, and in fact *every* case, it visits all `n` nodes regardless of how narrow the range is.

---

## Approach 2 — Optimal: Prune Using the BST Ordering

**Mental workflow before writing a single line:**

```
1. Base case: root == null → nothing here → return 0

2. sum = 0
   if root.val is within [low, high] → sum += root.val

3. if root.val > low:
       → left subtree is NOT provably all-too-small → recurse left,
         add its result to sum
   (if root.val <= low, left subtree is guaranteed entirely < low
    → skip it, contributes nothing)

4. if root.val < high:
       → right subtree is NOT provably all-too-big → recurse right,
         add its result to sum
   (if root.val >= high, right subtree is guaranteed entirely > high
    → skip it, contributes nothing)

5. return sum
```

```java
class Solution {
    public int rangeSumBST(TreeNode root, int low, int high) {
        // Base case: nothing here to sum
        if (root == null) return 0;

        int sum = 0;

        // This node itself might fall inside the range
        if (root.val >= low && root.val <= high) {
            sum += root.val;
        }

        // **PRUNE LEFT**: if root.val <= low, everything in the left
        // subtree is guaranteed smaller than root.val, hence
        // smaller than low — provably contributes nothing.
        // Only recurse left when that guarantee does NOT hold.
        if (root.val > low) {
            sum += rangeSumBST(root.left, low, high);
        }

        // **PRUNE RIGHT**: if root.val >= high, everything in the right
        // subtree is guaranteed bigger than root.val, hence bigger
        // than high — provably contributes nothing.
        // Only recurse right when that guarantee does NOT hold.
        if (root.val < high) {
            sum += rangeSumBST(root.right, low, high);
        }

        return sum;
    }
}
```

---

## Complexity Analysis

**Time Complexity — O(h + k) in the best/typical case, O(n) worst case:**

This deserves an honest, non-hand-wavy statement, the way Striver would frame it. Let `k` be the number of nodes whose values actually fall within `[low, high]`.

- Every node that's **visited but pruned away** (its subtree skipped) costs O(1) — that's the entire benefit of pruning.
- Every node that genuinely lies within the range, plus the handful of nodes on the boundary paths leading to those in-range nodes, gets visited and contributes O(1) work each.
- In the **best case** — a narrow range sitting deep in a *balanced* tree — you only ever walk down two root-to-boundary paths (O(h) each) plus visit the `k` in-range nodes themselves, giving roughly **O(h + k)**.
- In the **worst case** — the range covers the entire tree's value spread (e.g., `[Integer.MIN_VALUE, Integer.MAX_VALUE]`) — no pruning ever triggers, every node is visited, and this degrades to exactly the brute force's **O(n)**.

So the honest statement is: **this approach is never worse than O(n)**, and is substantially better than that whenever the range is narrow relative to the tree's full value spread — which the brute force can never achieve regardless of how narrow the range is.

**Space Complexity — O(h), where h is the height of the tree:**

The only auxiliary space is the recursion call stack — one frame per node on whatever path is currently being explored.

- **Balanced BST:** `h = O(log n)`.
- **Worst case (skewed BST):** `h = O(n)`.

---

## Comparing Brute Force vs Optimal

| Property | Brute Force | Optimal (Pruned) |
| --- | --- | --- |
| Uses BST ordering? | No — treats it like a generic binary tree | Yes — prunes an entire subtree per failed bound check |
| Always visits all n nodes? | Yes, always | Only when the range covers the tree's whole value spread |
| Time | O(n), always | O(h + k) typical, O(n) worst case — never worse than brute force |
| Space | O(h) | O(h) |

---

## The Key Takeaway

```
┌───────────────────────────────────────────────────────────────────────┐
│  At any node, the BST invariant lets you PROVE an entire              │
│  subtree contains nothing useful, without visiting it:                │
│                                                                       │
│      root.val <= low   →  left subtree is entirely < low              │
│                            → PRUNE it, skip recursing left            │
│                                                                       │
│      root.val >= high  →  right subtree is entirely > high            │
│                            → PRUNE it, skip recursing right           │
│                                                                       │
│  This is the SAME one-comparison-eliminates-a-subtree idea            │
│  from LC 700 (Search) and LC 235 (LCA in BST) — just applied          │
│  to a RANGE instead of a single target value.                         │
│                                                                       │
│  Unlike LC 530/230, this problem doesn't need inorder's SORTED        │
│  guarantee — it only needs the more basic "left < root < right"       │
│  ordering fact to justify skipping subtrees.                          │
│                                                                       │
│  Time  — never worse than O(n); O(h+k) typical for a narrow           │
│           range on a balanced tree.                                   │
│  Space — O(h): O(log n) balanced, O(n) worst case (skewed).           │
└───────────────────────────────────────────────────────────────────────┘
```