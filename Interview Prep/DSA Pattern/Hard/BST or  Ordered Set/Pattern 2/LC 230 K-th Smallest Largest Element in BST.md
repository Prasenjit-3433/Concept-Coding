# LC 230. K-th Smallest/Largest Element in BST

Key Concept: Inorder traversal of any BST is always sorted
Solution: https://www.youtube.com/watch?v=9TJYWh0adfk&list=PLgUwDviBIf0q8Hkd7bK2Bpryj2xVJk8Vk&index=46&ab_channel=takeUforward
Status: Done

# Stage 1: Identification

## **Step 1 — Which topic?**

You're given the root of a **Binary Search Tree** and an integer `k`. You need to find the `k`th smallest value in it.

## **Step 2 — Which pattern?**

Still **BST Pattern 2: BST Properties & Validation**. The trigger:

> "kth smallest" in a BST → exploit **inorder traversal = sorted sequence**
> 

## **Step 3 — Which key concept?**

**Counted Inorder Traversal — increment a counter at the exact moment a node is "visited" in inorder order, and stop the instant that counter hits `k`.**

The BST core property does the heavy lifting here: inorder traversal of a BST always produces values in sorted order. So "find the kth smallest value" is really just "find the kth value that inorder traversal visits." You don't need to collect the sorted sequence anywhere — you just need to count visits and grab the value the moment the count matches `k`.

---

# Stage 2: Intuition Building

### The Tree We're Working With

![image.png](LC%20230%20K-th%20Smallest%20Largest%20Element%20in%20BST/image.png)

```
              5
           /     \
          3       7
         / \     / \
        1   4   6   8
         \
          2
```

Inorder traversal of this tree — left, root, right, applied recursively — gives: `1, 2, 3, 4, 5, 6, 7, 8`. Every value in sorted order, for free, purely because of how a BST is shaped: at any node, everything smaller sits in the left subtree, everything bigger sits in the right subtree, and inorder always finishes the smaller side before touching the node itself.

### The Question to Ask

If inorder already visits nodes in sorted order, then the **3rd smallest** value is simply **the 3rd node that inorder visits**. Nothing more sophisticated needs to happen — you just need a way to know "which visit am I on right now" as the traversal runs.

### Why Sorting Is Wasted Work

The most obvious first idea: collect every node's value into a list (any traversal works for this — preorder, postorder, doesn't matter), sort that list, then pick index `k-1`.

But think about what you're actually doing there: you're paying to **re-derive** an order that the tree's structure already hands you for free, the moment you choose inorder instead of any other traversal. Sorting is solving a problem you never needed to have.

### From "Collect Everything" to "Count as You Go"

The next improvement: do an **inorder** traversal specifically (not preorder/postorder), collect into a list — now the list comes out **pre-sorted**, so no sorting step is needed. Just return index `k-1`.

<aside>
💬

**`Property`**: The inorder traversal of any BST is always sorted.

</aside>

This is better, but there's still a wasted cost: you're storing **every single value** in the tree, even though you only ever care about one of them. If the tree has a million nodes and `k = 3`, you'd still allocate a million-element list just to read its 3rd entry.

### The Real Insight — You Don't Need the List, Just a Counter

Ask yourself: what does storing a value into a list actually give you that you need? Only its **position** in the sorted order. You don't need the whole list sitting around afterward — you only need to know, at each point during the traversal, "how many values have I visited so far?"

So replace "append to a list" with "increment a counter." The counter's value at any moment tells you exactly which smallest-value-rank you're currently standing on. The moment that counter equals `k`, whatever node you're standing at **is** the answer — you can stop right there, without visiting anything else.

```
┌──────────────────────────────────────────────────────────────────────┐
│  Inorder traversal visits nodes in ascending order.                  │
│  A running counter, incremented at each visit, tells you             │
│  the RANK of the node currently being visited.                       │
│                                                                      │
│  counter == k  →  this node IS the kth smallest. Stop here.          │
└──────────────────────────────────────────────────────────────────────┘
```

### Where Exactly Does the Counter Increment?

Inorder is **left → root → right**. The "visit" step — the one that corresponds to "this value now appears in the sorted sequence" — is the **root** step, sitting in the middle. So the counter increments neither before recursing left nor after recursing right — it increments exactly at the point where you'd normally print/collect the node's value.

### Walking Through the Idea on the Example Tree, `k = 3`

Start the traversal. It dives all the way left first (that's what inorder does before it visits anything): `5 → 3 → 1 → null`. At `null`, there's nothing to visit, so it backtracks to `1`.

```
Visit 1  → counter becomes 1   (1 ≠ 3, keep going)
Visit 2  → counter becomes 2   (2 ≠ 3, keep going)
Visit 3  → counter becomes 3   (3 == 3 → STOP, answer is node with value 3)
```

Everything to the right of node `3` — nodes `4, 5, 6, 7, 8` — never needs to be touched at all. The moment the counter hits `k`, the search is done.

### Why "Stopping Early" Actually Works With Recursion

A subtlety worth calling out: recursion doesn't have a built-in "stop everything" button the way a loop has `break`. The way you achieve early termination in a recursive traversal is by **threading the found result back up through return values**. Once a node deep in the recursion finds the answer and returns it (non-null), every parent call up the chain, upon seeing a non-null result come back from the left recursive call, immediately returns that same result upward too — without ever bothering to increment its own counter or recurse into its right subtree. This is exactly why the recursive shape in your code checks `if (left != null) return left;` right after the left recursive call — that's the early-exit propagation mechanism.

```
┌──────────────────────────────────────────────────────────────────────┐
│  Recursion doesn't have a "break" — but a found answer can be        │
│  passed back up through return values. Every ancestor, upon          │
│  seeing a non-null answer bubble up from a child call, simply        │
│  forwards it further up immediately, skipping its own                │
│  remaining work (counter update, right subtree recursion).           │
└──────────────────────────────────────────────────────────────────────┘
```

---

# Stage 3: Coding

## Approach 1 — Brute Force: Traverse + Sort

**The honest thinking:**

> "Traverse the whole tree with any DFS order, collect every value into a list, sort it, return index k-1."
> 

```java
class Solution {
    public int kthSmallest(TreeNode root, int k) {
        List<Integer> values = new ArrayList<>();
        collect(root, values);
        Collections.sort(values);
        return values.get(k - 1);
    }

    private void collect(TreeNode node, List<Integer> values) {
        if (node == null) return;
        values.add(node.val);
        collect(node.left, values);
        collect(node.right, values);
    }
}
```

Why this is wasteful: it completely ignores the fact that this is a **BST**, not a generic binary tree — the sorted order is available for free via inorder, but this approach pays an extra `O(n log n)` to re-derive it anyway, plus `O(n)` space to store everything.

---

## Approach 2 — Better: Inorder Collect (No Sorting Needed)

**The thinking:**

> "If I traverse in INORDER specifically, the list comes out pre-sorted. No sort step needed."
> 

```java
class Solution {
    public int kthSmallest(TreeNode root, int k) {
        List<Integer> values = new ArrayList<>();
        inorder(root, values);
        return values.get(k - 1);
    }

    private void inorder(TreeNode node, List<Integer> values) {
        if (node == null) return;
        inorder(node.left, values);
        values.add(node.val);
        inorder(node.right, values);
    }
}
```

This removes the `O(n log n)` sort entirely — time drops to `O(n)`. But it still visits and stores **every** node, even when `k` is small and the answer is found early. Space is still `O(n)`.

---

## Approach 3 — Optimal: Counted Inorder With Early Exit

**Mental workflow:**

```
1. Maintain a counter, starting at 0.

2. Recursively:
   → recurse left first; if it already found the answer
     (non-null), propagate that answer straight up, do nothing else
   → increment the counter — THIS is the "visit" step
   → if counter == k → this node IS the answer, return it
   → otherwise, recurse right; propagate whatever it finds
     (found node, or null if not found on this side)

3. Base case: node == null → nothing here, return null
```

This is exactly the shape of your code — here it is again with the reasoning attached inline:

```java
class Solution {
    private TreeNode func(TreeNode root, int[] counter, int k) {
        // Base case: nothing here to visit or count
        if (root == null) return null;

        // Step 1: go left first — inorder must exhaust the
        // smaller values before this node counts as "visited"
        TreeNode left = func(root.left, counter, k);

        // EARLY EXIT: if the answer was already found somewhere
        // in the left subtree, don't do ANY more work at this
        // level — just forward it straight up
        if (left != null) return left;

        // Step 2: THIS is the "visit" moment of inorder —
        // increment the counter now, exactly where you'd
        // normally collect/print the value
        counter[0]++;

        // If this visit is exactly the kth one, this node
        // IS the answer
        if (counter[0] == k) return root;

        // Step 3: not yet at k — continue into the right subtree
        TreeNode right = func(root.right, counter, k);
        if (right != null) return right;

        // Neither this node nor anything below it (on this path)
        // was the answer
        return null;
    }

    public int kthSmallest(TreeNode root, int k) {
        if (root == null) return -1;

        int[] counter = new int[1];
        // int[] instead of a plain int — Java has no pass-by-
        // reference for primitives, so a 1-element array plays
        // that role, letting every recursive call share and
        // mutate the SAME counter

        TreeNode ans = func(root, counter, k);
        return ans.val;
    }
}
```

This is correct exactly as you wrote it. One thing worth being explicit about: even though the recursion technically still has calls sitting on the stack for nodes further to the right (they just haven't executed yet when the answer is found), the **moment** the answer is found, no further counter increments or comparisons happen anywhere — the `if (left != null) return left;` guard is what prevents any wasted work from that point onward.

### 🧠An Equivalent Iterative Version (Worth Knowing)

The same idea, without recursion — useful because it makes the "stop as soon as you have the answer" behavior a literal `break` instead of return-value threading, and generalizes cleanly to the "modified frequently" follow-up discussion later:

```java
class Solution {
    public int kthSmallest(TreeNode root, int k) {
        Stack<TreeNode> stack = new Stack<>();
        TreeNode node = root;
        int count = 0;

        while (true) {
            // Push while going left — same mechanism as the
            // standard iterative inorder traversal
            while (node != null) {
                stack.push(node);
                node = node.left;
            }

            // Pop = the next node in sorted order
            node = stack.pop();
            count++;

            if (count == k) return node.val;

            // Move right, then continue the "push while going left"
            // loop from there
            node = node.right;
        }
    }
}
```

This has the same time/space bounds as the recursive version, but the early stop is a direct `return` the moment `count == k` — no propagation needed since there's no call stack to unwind.

---

## Complexity Analysis

**Time Complexity — O(H + k):**

The traversal first dives straight down to the leftmost node — that costs `O(H)`, where `H` is the tree's height. From there, it keeps visiting nodes in ascending order and stops the moment it has visited exactly `k` of them. So the total number of nodes actually touched is bounded by the initial descent plus the `k` visits needed — `O(H + k)`.

- In the **worst case**, `k = n` (asking for the largest value via smallest-side counting) — then this becomes `O(n)`, same as full traversal.
- For **small `k`**, this is dramatically cheaper than visiting the whole tree, unlike Approaches 1 and 2, which always pay `O(n)` regardless of `k`.

**Space Complexity — O(H):**

Only the recursion call stack (or the explicit stack, in the iterative version) is used — no list of collected values. Depth is bounded by the tree's height: `O(log n)` for a balanced BST, `O(n)` worst case for a skewed one.

---

# Kth Largest Element — The Companion Problem

Striver poses this as a natural follow-up: same problem, but the largest instead of the smallest.

### Approach A — Striver's `n - k + 1` Trick

The reasoning: if you know the total number of nodes `n`, then the `k`th largest is exactly the `(n - k + 1)`th smallest — counting from the small end instead of the large end lands on the same value.

```
kth largest  =  (n - k + 1)th smallest
```

This needs one pass to count `n` (if not already known), then the exact same counted-inorder technique from above, just searching for rank `n - k + 1` instead of `k`.

### Approach B — Reverse Inorder (Cleaner, No Need for `n`)

A more direct idea: if **inorder** (left, root, right) visits nodes smallest-first, then **reverse inorder** (right, root, left) visits nodes **largest-first**. Apply the exact same counter-and-early-exit trick, just walking right before left:

```java
class Solution {
    private TreeNode func(TreeNode root, int[] counter, int k) {
        if (root == null) return null;

        // Go RIGHT first this time — reverse inorder visits
        // the LARGEST values first
        TreeNode right = func(root.right, counter, k);
        if (right != null) return right;

        counter[0]++;
        if (counter[0] == k) return root;

        TreeNode left = func(root.left, counter, k);
        if (left != null) return left;

        return null;
    }

    public int kthLargest(TreeNode root, int k) {
        if (root == null) return -1;
        int[] counter = new int[1];
        TreeNode ans = func(root, counter, k);
        return ans.val;
    }
}
```

This avoids needing `n` at all, and keeps the same `O(H + k)` time — it's the more natural mirror of the smallest-element technique, rather than a two-pass workaround.

---

# ❓The `Follow-Up` — Frequent Inserts/Deletes, Frequent Kth-Smallest Queries

This is the part of the problem that turns it from *"one clever traversal trick"* into a real system-design-flavored question, and it's worth understanding properly.

### Why the Current Approach Breaks Down Under Repetition

The counted-inorder technique is `O(H + k)` — perfectly fine for a **single** query. But if the tree is being modified constantly (insertions, deletions) and you need to answer "what's the kth smallest?" **repeatedly**, paying `O(H + k)` — potentially `O(n)` — on every single query becomes expensive if this happens many times.

### The Core Idea — Augment Each Node With a Subtree Size

The fix doesn't change the tree's shape — it adds **one extra piece of bookkeeping** to every node: a `count` field, storing **how many nodes exist in the subtree rooted at this node** (including itself).

```
count(node) = 1 + count(node.left) + count(node.right)
```

Why does this unlock fast kth-smallest queries? Because at any node, `count(node.left)` tells you **exactly how many values are smaller than this node** — no traversal needed, just a field lookup.

### Answering a Query Using the Augmented Counts

Standing at any node, let `L = count(node.left)`. Compare `k` against `L`:

```
if k == L + 1:
    → exactly L values are smaller than this node, and this
      node itself is the (L+1)th smallest → THIS node is the **answer**

if k <= L:
    → the answer lies entirely within the left subtree
      (which alone already has at least k smaller values)
    → recurse left with the SAME k

if k > L + 1:
    → the answer lies in the right subtree — but L+1 values
      (everything in the left subtree, plus this node itself)
      have already been "used up"
    → recurse right with k adjusted to (k - L - 1)
```

This walks exactly one root-to-node path — no different from a BST search — so each query costs **O(H)**, without needing to touch `k` nodes one at a time the way the plain inorder counter does. For a balanced tree this is `O(log n)` per query, regardless of how large `k` is.

### Keeping the Counts Correct Under Insert/Delete

The `count` field isn't free — it has to be maintained. But the good news is it only needs updating **along the single root-to-node path** that insertion or deletion already walks:

```
Insert:  as you walk down to find the insertion point
         (same navigation as LC 701), increment count[]
         by 1 at every node you pass through along the way

Delete:  as you walk down to find the node to delete
         (same navigation as LC 450), decrement count[]
         by 1 at every node you pass through along the way
```

Since both insert and delete already cost `O(H)` on their own (as established in the Basic BST Operations pattern), adding this bookkeeping doesn't change their asymptotic cost at all — it just adds a constant amount of extra work per node on a path you were already walking.

```
┌──────────────────────────────────────────────────────────────────────┐
│  This augmented structure is sometimes called an                     │
│  **"Order-Statistics BST"** — a BST where every node additionally    │
│  knows the size of its own subtree.                                  │
│                                                                      │
│  Insert / Delete   → O(H), same as plain BST, plus O(H) count        │
│                       updates along the same path (no extra          │
│                       asymptotic cost)                               │
│  Kth-smallest query → O(H), using subtree counts to decide           │
│                        direction — no per-node counter needed        │ 
│                                                                      │
│  This is exactly what the pattern sheet's follow-up note for         │
│  LC 230 means by "augment BST with subtree sizes for O(h)."          │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Comparing All Approaches

| Approach | Time (per query) | Space | Handles frequent modification well? |
| --- | --- | --- | --- |
| Traverse + Sort | O(n log n) | O(n) | No |
| Inorder Collect | O(n) | O(n) | No |
| Counted Inorder (early exit) | O(H + k) | O(H) | No — still O(H+k) every single query |
| Augmented BST (subtree sizes) | O(H) | O(H) extra per node | Yes — O(H) per query AND per update |

---

## The Key Takeaway

```
┌───────────────────────────────────────────────────────────────────────┐
│  Inorder traversal of a BST = values in sorted order.                 │
│  kth smallest = the kth node inorder visits.                          │
│                                                                       │
│  DON'T collect a list and sort it — the tree already IS sorted        │
│  once you traverse it the right way.                                  │
│                                                                       │
│  DON'T even collect a list at all — just count visits with a          │
│  running counter, and stop the moment the count hits k.               │
│  Recursion has no "break," so early exit is achieved by               │
│  threading the found node back up through return values.              │ 
│                                                                       │
│  Kth LARGEST = same idea, reversed traversal order                    │
│  (right → root → left), OR (n - k + 1)th smallest if n is known.      │
│                                                                       │
│  FOR REPEATED QUERIES under frequent insert/delete: augment           │
│  every node with its subtree's size. Then kth-smallest                │
│  becomes a single O(H) directional walk — no counting needed —        │
│  and insert/delete absorb the bookkeeping for free, since they        │
│  already walk the same path.                                          │
│                                                                       │
│  Time  — O(H + k) for plain counted inorder;                          │
│           O(H) for the augmented order-statistics BST.                │
│  Space — O(H): recursion/explicit stack depth only.                   │
└───────────────────────────────────────────────────────────────────────┘
```