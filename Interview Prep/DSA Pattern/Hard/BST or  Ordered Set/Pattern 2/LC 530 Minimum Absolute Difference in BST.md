# LC 530. Minimum Absolute Difference in BST

Key Concept: Inorder — minimum difference always between adjacent elements in sorted order
Problem: https://leetcode.com/problems/minimum-absolute-difference-in-bst/description/
Solution: https://www.youtube.com/watch?v=yNvt9ejf9Ec
Status: Done

# Stage 1: Identification

### **Step 1 — Which topic?**

You're given the root of a **Binary Search Tree**. You need to find the **minimum absolute difference** between the values of any two **different** nodes in the tree.

### **Step 2 — Which pattern?**

Still **BST Pattern 2: BST Properties & Validation**. The trigger:

> "minimum absolute difference" / "minimum distance" between values in a BST → exploit **inorder traversal = sorted sequence**
> 

### **Step 3 — Which key concept?**

**Inorder — the minimum difference is always found between two *adjacent* elements in sorted order; you never need to compare non-adjacent pairs.**

The BST core property does essentially all the work here, once you see it: inorder traversal of a BST hands you every value in strictly sorted order, for free. And once you have a sorted sequence, there's a classical fact about minimum absolute difference that makes the rest of the problem almost trivial — you never need to check every pair.

---

# Stage 2: Intuition Building

### The Tree We're Working With

```
              4
           /     \
          2       6
         / \
        1   3
```

Inorder traversal: `1, 2, 3, 4, 6` — sorted, as always for a BST.

### The Naive First Instinct — Compare Every Pair

The most direct reading of the problem: *"minimum absolute difference between any two nodes"* sounds like it's asking you to check every possible pair of values and take the smallest `|a - b|`.

```
Collect all values: [1, 2, 3, 4, 6]
Check every pair:
  |1-2|=1, |1-3|=2, |1-4|=3, |1-6|=5,
  |2-3|=1, |2-4|=2, |2-6|=4,
  |3-4|=1, |3-6|=3,
  |4-6|=2
Minimum = 1
```

This works, but comparing every pair is `O(n²)` — and the moment you notice the values came from a BST, you should immediately ask: *is there a cheaper way, exploiting the ordering?*

### The Core Insight — Why Only Adjacent Pairs (in Sorted Order) Matter

Here's the fact that collapses this problem: **once you have values in sorted order, the minimum absolute difference between *any* two values is always achieved by some pair of *adjacent* values in that sorted sequence.**

Why is this true? Think about any two values `a` and `b` from the sorted sequence that are **not** adjacent — meaning there's at least one value `c` sitting strictly between them in sorted order:

```
a < c < b
```

Then:

```
b - a = (b - c) + (c - a)
```

Both `(b - c)` and `(c - a)` are positive (since `a < c < b`). So `b - a` is the **sum of two positive quantities** — which means `b - a` is strictly **greater than each individual piece**:

```
b - a > b - c
b - a > c - a
```

In other words, whatever gap `a` and `b` have between them, you can always find a *smaller* gap by looking at `a` and `c`, or `c` and `b` — some adjacent pair "in between" them. So a non-adjacent pair can **never** be the unique minimum — there's always an adjacent pair that's at least as good, usually strictly better.

```
┌───────────────────────────────────────────────────────────────────────┐
│  In a SORTED sequence:                                                │
│                                                                       │
│      minimum absolute difference over ALL pairs                       │
│           =                                                           │
│      minimum absolute difference over only ADJACENT pairs             │
│                                                                       │
│  Any gap between two non-adjacent values can be "broken down"         │
│  into two smaller gaps using whatever lies between them —             │
│  so checking non-adjacent pairs is provably wasted work.              │
└───────────────────────────────────────────────────────────────────────┘
```

Verify this on the example: sorted values `1, 2, 3, 4, 6`. Adjacent gaps only: `|2-1|=1, |3-2|=1, |4-3|=1, |6-4|=2`. Minimum among these = **1** — exactly matching the full `O(n²)` brute-force answer above. You genuinely never needed to check `|1-6|`, `|1-4|`, `|2-6|`, or any of the other non-adjacent pairs.

### Connecting This Back to the BST

This is where the BST property becomes the whole trick: **inorder traversal already produces the values in sorted order.** So "adjacent pairs in sorted order" is exactly the same thing as "**consecutively visited nodes during an inorder traversal**." You don't need to sort anything — the tree's own shape already sorts it for you, the moment you traverse it the right way.

So the entire problem reduces to: *do an inorder traversal, and at every step, compare the current node's value against the value of the node visited immediately before it — keep the smallest such difference.*

### Walking Through the Idea on the Example Tree

```
              4
           /     \
          2       6
         / \
        1   3
```

Inorder visits nodes in this order: `1, 2, 3, 4, 6`.

```
Visit 1 → nothing visited before it yet → no comparison possible, just remember it
Visit 2 → compare against previous (1)  → |2-1| = 1 → candidate minimum = 1
Visit 3 → compare against previous (2)  → |3-2| = 1 → still 1
Visit 4 → compare against previous (3)  → |4-3| = 1 → still 1
Visit 6 → compare against previous (4)  → |6-4| = 2 → not smaller, stays 1
```

**Minimum absolute difference = 1.**

Notice: only **4 comparisons** were made (one per consecutive pair), instead of the 10 comparisons brute force needed — and this gap only grows as the tree gets bigger. For `n` nodes, brute force does `O(n²)` comparisons; this approach does exactly `n - 1`.

### Where Exactly Does the Comparison Happen?

Same discipline as every inorder-based problem so far (LC 230, LC 98's alternative approach): the "visit" moment in inorder is **left → root → right**, so the comparison against the previous value happens exactly at the **root step**, sitting in the middle — not before recursing left, not after recursing right.

You need to track **one piece of running state**: "what was the last value visited?" Every time you visit a new node, if there *was* a previous value, compute the difference and update your running minimum; then update "previous" to the current node's value, regardless.

```
┌───────────────────────────────────────────────────────────────────────┐
│  Inorder traversal + one running variable ("previous value")          │
│  is ALL that's needed.                                                │
│                                                                       │
│  At each visit:                                                       │
│      if previous exists:                                              │
│          minDiff = min(minDiff, current.val - previous)               │
│      previous = current.val                                           │
│                                                                       │
│  Since inorder is sorted, current.val - previous is ALWAYS            │
│  non-negative — no need for Math.abs() if you trust the               │
│  traversal order (though it's harmless to include for safety).        │
└───────────────────────────────────────────────────────────────────────┘
```

### Why This Is Exactly the Same Shape as LC 230's Alternative Technique

If this feels familiar, it should — it's the identical mechanism as the "inorder strictly increasing" idea from LC 98's alternative approach, and structurally close to LC 230's counted inorder. All three problems share the same skeleton: **traverse inorder, track one small piece of running state (a counter, a "previous value", a comparison), and let the BST's sorted-inorder guarantee do the heavy lifting.** Only what you track and compare changes from problem to problem.

### Why the Traversal Terminates and Touches Every Node Exactly Once

Nothing new here — it's the same postorder/inorder recursive shape used throughout this pattern: base case `node == null → return`, and every recursive call moves strictly to a child. Since the tree is finite with no cycles, the traversal completes in a bounded number of steps, visiting each of the `n` nodes exactly once.

# Stage 3: Coding

---

## Approach 1 — Brute Force: Collect All Values, Compare Every Pair

**The honest thinking:**

> "Traverse the tree with any traversal, collect every value into a list. Then check every possible pair and take the minimum absolute difference."
> 

```java
class Solution {
    public int getMinimumDifference(TreeNode root) {
        List<Integer> values = new ArrayList<>();
        collect(root, values);

        int minDiff = Integer.MAX_VALUE;

        // Compare EVERY pair — completely ignores the fact
        // that these values came from a BST
        for (int i = 0; i < values.size(); i++) {
            for (int j = i + 1; j < values.size(); j++) {
                minDiff = Math.min(minDiff, Math.abs(values.get(i) - values.get(j)));
            }
        }

        return minDiff;
    }

    private void collect(TreeNode node, List<Integer> values) {
        if (node == null) return;
        values.add(node.val);
        collect(node.left, values);
        collect(node.right, values);
    }
}
```

**Why this is wasteful:** it pays `O(n²)` comparing pairs that Stage 2 already proved can never produce the true minimum unless they happen to be adjacent in sorted order anyway. It also doesn't even bother sorting — any traversal order works here since every pair gets checked regardless, but that's exactly the sign this approach isn't using the BST property at all. Establishes correctness only.

---

## Approach 2 — Better: Inorder Collect, Then Only Check Adjacent Pairs

**The thinking:**

> "If I traverse in INORDER specifically, the list comes out sorted for free. Then, per Stage 2's proof, I only need to check adjacent pairs in that list — not all pairs."
> 

```java
class Solution {
    public int getMinimumDifference(TreeNode root) {
        List<Integer> values = new ArrayList<>();
        inorder(root, values);

        int minDiff = Integer.MAX_VALUE;

        // Only ADJACENT pairs — proven sufficient in Stage 2
        for (int i = 1; i < values.size(); i++) {
            minDiff = Math.min(minDiff, values.get(i) - values.get(i - 1));
        }

        return minDiff;
    }

    private void inorder(TreeNode node, List<Integer> values) {
        if (node == null) return;
        inorder(node.left, values);
        values.add(node.val);
        inorder(node.right, values);
    }
}
```

This drops the time complexity from `O(n²)` to `O(n)` — a real improvement. But it still pays `O(n)` **space** to store every value in a list, even though the final answer only ever needs to compare a node against whatever came immediately before it. That list is fully materialized and then thrown away — nothing about it is needed afterward.

---

## Approach 3 — Optimal: Inorder With a Single Running "Previous" Variable

**Mental workflow before writing a single line:**

```
1. Maintain ONE running variable: prev, tracking the value of
   the last node visited in inorder order. Initialize to null
   (nothing visited yet).

2. Maintain ONE running variable: minDiff, initialized to
   Integer.MAX_VALUE.

3. Recursively, in INORDER (left, root, right):
   → recurse left first
   → at the "visit" step (the root step, in the middle):
       if prev is not null:
           minDiff = min(minDiff, current.val - prev)
       prev = current.val
   → recurse right

4. Base case: node == null → return (nothing to visit)

5. Return minDiff after the full traversal completes
```

```java
class Solution {
    // Tracks the value of the previously visited node in
    // inorder order. Long (boxed), not primitive long, so it
    // can start as null — cleanly representing "nothing visited
    // yet" without colliding with a legitimate node value.
    // (Same technique used in LC 98's inorder alternative.)
    private Long prev = null;

    private int minDiff = Integer.MAX_VALUE;

    public int getMinimumDifference(TreeNode root) {
        inorder(root);
        return minDiff;
    }

    private void inorder(TreeNode node) {
        // Base case: nothing here to visit or compare
        if (node == null) return;

        // Step 1: LEFT — fully explore the smaller side first,
        // exactly as inorder demands
        inorder(node.left);

        // Step 2: ROOT — this is the "visit" moment. Compare
        // against whatever was visited immediately before this,
        // in sorted order — that comparison alone is provably
        // enough (Stage 2's adjacent-pair argument).
        if (prev != null) {
            minDiff = Math.min(minDiff, (int) (node.val - prev));
        }
        prev = (long) node.val;

        // Step 3: RIGHT — continue into the larger side
        inorder(node.right);
    }
}
```

Notice this is structurally almost identical to LC 98's "inorder strictly increasing" alternative approach — same `prev` tracking mechanism, same traversal shape. The only difference is what gets *done* with the comparison: LC 98 checks `current <= prev` and bails out on violation; this problem computes `current - prev` and folds it into a running minimum.

---

## Workflow Trace on the Example Tree

```
              4
           /     \
          2       6
         / \
        1   3
```

```
inorder(4):
  inorder(2):
    inorder(1):
      inorder(null) → return
      VISIT 1: prev is null → no comparison. prev = 1.
      inorder(null) → return

    VISIT 2: prev = 1 → minDiff = min(∞, 2-1) = 1. prev = 2.

    inorder(3):
      inorder(null) → return
      VISIT 3: prev = 2 → minDiff = min(1, 3-2) = 1. prev = 3.
      inorder(null) → return

  VISIT 4: prev = 3 → minDiff = min(1, 4-3) = 1. prev = 4.

  inorder(6):
    inorder(null) → return
    VISIT 6: prev = 4 → minDiff = min(1, 6-4) = 2 → not smaller, stays 1. prev = 6.
    inorder(null) → return

Final: minDiff = 1
```

**Answer: 1** — matches both the brute force and Stage 2's manual trace exactly.

---

## Complexity Analysis

**Time Complexity — O(n):**

Every node is visited exactly once by the inorder recursion. At each visit, the work done is O(1) — one null check, one subtraction, one comparison, one assignment. With `n` total nodes, each processed in O(1) time, total time is **O(n)**. This is the best possible time complexity for this problem — you cannot determine the minimum difference without at least looking at every node once, since any unvisited node could secretly hold the value that produces the true minimum gap.

**Space Complexity — O(h), where h is the height of the tree:**

The only auxiliary space is the recursion call stack — one frame per node on the current root-to-node path. The `prev` and `minDiff` variables are single fields, not structures that grow with the size of the tree — a genuine improvement over Approach 2, which needed `O(n)` extra space to hold the full list of values.

- **Best case (balanced BST):** `h = O(log n)`.
- **Worst case (skewed BST):** `h = O(n)`.

So: **O(h) auxiliary space — O(log n) balanced, O(n) worst case (skewed tree)**.

---

## Comparing All Three Approaches

| Approach | Uses BST property? | Time | Extra Space |
| --- | --- | --- | --- |
| Brute Force (all pairs) | No — treats it like an unordered value set | O(n²) | O(n) — stores every value |
| Better (inorder collect + adjacent pairs) | Yes — sorted via inorder | O(n) | O(n) — stores every value |
| Optimal (inorder + running `prev`) | Yes — sorted via inorder | O(n) | O(h) — only the call stack |

---

## The Key Takeaway

```
┌───────────────────────────────────────────────────────────────────────┐
│  In ANY sorted sequence, the minimum absolute difference              │
│  between any two elements is always found between some pair           │
│  of ADJACENT elements — never a non-adjacent pair.                    │
│                                                                       │
│  WHY: if a < c < b, then (b-a) = (b-c) + (c-a), so the gap            │
│  between non-adjacent a and b is always strictly larger than          │
│  at least one of the smaller gaps hiding "inside" it.                 │
│                                                                       │
│  A BST's inorder traversal already visits values in sorted            │
│  order — so "adjacent in sorted order" = "consecutively               │
│  visited during inorder." No sorting, no pair-checking needed.        │
│                                                                       │
│  THE TRICK: track ONE running variable — the previous node's          │
│  value — and compare it against the current node's value at           │
│  the exact "visit" moment (the root step) of inorder. No list,        │
│  no extra storage beyond the call stack itself.                       │
│                                                                       │
│  Same skeleton as LC 98's inorder-alternative and LC 230's            │
│  counted inorder: traverse inorder, carry one small piece of          │
│  running state, let the sorted guarantee do the rest.                 │
│                                                                       │
│  Time  — O(n): every node visited exactly once.                       │
│  Space — O(h): O(log n) balanced, O(n) worst case (skewed).           │
└───────────────────────────────────────────────────────────────────────┘
```

---

One more thing worth flagging explicitly, since it's in the pattern sheet: **LC 783 (Minimum Distance Between BST Nodes)** is listed as "same as LC 530 — different problem number, identical solution; do only one." Given this note fully covers the technique, you're all set for LC 783 too without a separate write-up, unless you'd like one anyway for completeness.