# LC 98. Validate Binary Search Tree

Key Concept: Preferred: min/max bounds downward. Alternative: inorder strictly increasing
Solution: https://www.youtube.com/watch?v=f-sj7I5oXEI&list=PLgUwDviBIf0q8Hkd7bK2Bpryj2xVJk8Vk&index=47&ab_channel=takeUforward
Status: Done

# Stage 1: Identification

## **Step 1 — Which topic?**

You're given the root of a binary tree. You need to determine whether it satisfies the **Binary Search Tree property** — for every node, all values in its left subtree are strictly smaller, and all values in its right subtree are strictly greater.

## **Step 2 — Which pattern?**

This opens **BST Pattern 2: BST Properties & Validation**. The trigger:

> "validate BST" → check whether the ordering invariant holds everywhere → **BST Properties & Validation**
> 

## **Step 3 — Which key concept?**

**Valid Range Propagation — pass down a `(min, max)` window at every node, instead of only comparing a node against its immediate children.**

The key concept, and the mistake this problem is designed to expose: checking `node.left.val < node.val < node.right.val` **locally** at every node is *not enough* to confirm a valid BST. A node can look perfectly fine compared to its immediate children and still violate the property with respect to an ancestor further up. The fix is to make every node aware of the **valid range** it's allowed to fall into — a range that gets narrowed as you descend — rather than only checking against its direct neighbors.

---

# Stage 2: Intuition Building

### The Trap — Why "Check Against Immediate Children" Fails

Take this tree:

![image.png](LC%2098%20Validate%20Binary%20Search%20Tree/image.png)

```
        5
       / \
      1   4
         / \
        3   6
```

Look at node `4` in isolation: its left child is `3` (smaller ✓) and its right child is `6` (bigger ✓). By a purely local check, node `4` looks completely fine.

But node `4` sits in the **right subtree of node 5**. And the BST property doesn't just say "compare a node to its direct children" — it says **everything** in a node's right subtree must be bigger than that node. Node `4`'s right subtree contains `6`, which is fine relative to `5`. But node `4`'s **left subtree** contains `3` — and `3` is **smaller than 5**, yet it's sitting in `5`'s right subtree, where everything is supposed to be bigger than `5`. This tree is **not** a valid BST, even though every node individually looks fine next to its immediate children.

```
┌──────────────────────────────────────────────────────────────────────┐
│  A node's value must be consistent not just with its direct          │
│  children, but with EVERY ancestor whose subtree it sits in.         │
│                                                                      │
│  Checking only immediate neighbors misses violations that            │
│  come from further up the tree — this is the exact trap              │
│  this problem is built to catch.                                     │
└──────────────────────────────────────────────────────────────────────┘
```

### The Core Question — What Range Is Each Node Actually Allowed To Be In?

Instead of asking "is this node bigger than its left child and smaller than its right child," ask a sharper question at every single node:

> *"Given where I am in the tree, what is the full range of values I'm allowed to hold?"*
> 

At the root, there's no restriction yet — any value is fine, so the allowed range is `(-infinity, +infinity)`.

The moment you move to a **left** child, you've just said "this new node must be smaller than its parent." So the **upper bound** of the range tightens to the parent's value — the lower bound stays whatever it already was.

The moment you move to a **right** child, you've just said "this new node must be bigger than its parent." So the **lower bound** tightens to the parent's value — the upper bound stays whatever it already was.

```
┌──────────────────────────────────────────────────────────────────────┐
│  Moving LEFT   → new upper bound = parent's value                    │
│                  (lower bound inherited unchanged)                   │
│                                                                      │
│  Moving RIGHT  → new lower bound = parent's value                    │
│                  (upper bound inherited unchanged)                   │
│                                                                      │ 
│  At every node: check that its OWN value falls strictly              │
│  inside the range handed down to it. If not — invalid BST.           │
└──────────────────────────────────────────────────────────────────────┘
```

This range doesn't just capture "compare against my direct parent" — because the bound is **inherited** and **narrowed** as you go, by the time you're several levels deep, the range already encodes every ancestor's constraint simultaneously, not just the most recent one.

### Walking Through the Invalid Example With Ranges

```
        5
       / \
      1   4
         / \
        3   6
```

```
Start at 5: range = (-infinity, +infinity)
  5 is inside this range ✓

Move to 4 (right child of 5): range tightens —
  lower bound becomes 5 (inherited upper stays +infinity)
  range = (5, +infinity)
  Is 4 inside (5, +infinity)?  NO — 4 is not greater than 5.

INVALID — caught immediately at node 4.
```

Notice this catches the problem **without ever needing to look at node 3**. The violation is detected the moment node `4` itself is checked against the range handed down from the root — because that range already encodes "you're in 5's right subtree, so you must be bigger than 5."

### Walking Through a Valid Example

![image.png](LC%2098%20Validate%20Binary%20Search%20Tree/image%201.png)

```
              5
           /     \
          3       8
         / \     / \
        1   4   7   9
```

```
Start at 5: range = (-inf, +inf) → 5 is inside ✓

Move to 3 (left of 5): range = (-inf, 5)
  Is 3 inside (-inf, 5)? YES ✓

  Move to 1 (left of 3): range = (-inf, 3)
    Is 1 inside (-inf, 3)? YES ✓

  Move to 4 (right of 3): range = (3, 5)
    Is 4 inside (3, 5)? YES ✓

Move to 8 (right of 5): range = (5, +inf)
  Is 8 inside (5, +inf)? YES ✓

  Move to 7 (left of 8): range = (5, 8)
    Is 7 inside (5, 8)? YES ✓

  Move to 9 (right of 8): range = (8, +inf)
    Is 9 inside (8, +inf)? YES ✓
```

Every node passes its own inherited range check — the tree is a valid BST.

### Why This Is Still a Simple Preorder-Style Recursion

Nothing exotic is happening mechanically — you're doing a standard top-down traversal (root first, then children), and the only addition is that two extra pieces of information — the current lower and upper bound — are threaded down as parameters, narrowing at each step depending on which direction you moved. The recursion still terminates the same way every tree recursion does: base case `null → valid` (an empty subtree can't violate anything), and the overall answer is `true` only if **both** the left and right recursive calls return `true`.

```
┌─────────────────────────────────────────────────────────────────┐
│  solve(node, lowerBound, upperBound):                           │
│      if node is null → true (nothing here to violate anything)  │
│      if node.val is NOT strictly between lowerBound and         │
│          upperBound → false, stop immediately                   │
│      return solve(node.left,  lowerBound,   node.val)           │
│         AND solve(node.right, node.val,     upperBound)         │
└─────────────────────────────────────────────────────────────────┘
```

### The Alternative Approach — Inorder Must Be Strictly Increasing

The pattern sheet flags a second valid technique, worth understanding even though bounds is preferred: since inorder traversal of a valid BST always produces a **strictly increasing** sequence, you can instead do an inorder traversal and check that each value is strictly greater than the value visited immediately before it.

```
┌─────────────────────────────────────────────────────────────────┐
│  Track "the previously visited value" during inorder            │
│  traversal (start it at -infinity, or null/None as a sentinel   │
│  meaning "nothing visited yet").                                │
│                                                                 │
│  At each visit: if current node's value <= previous value       │
│      → sequence isn't strictly increasing → invalid             │
│  Otherwise: update "previous" to the current value, continue    │
└─────────────────────────────────────────────────────────────────┘
```

This works correctly, but it's **easier to get subtly wrong**: if you only compare a node against its immediate predecessor in the sequence (rather than tracking a genuinely running "previous" variable across the entire traversal, correctly updated at every step including across subtree boundaries), you can end up with bugs that don't generalize well. The bounds approach is preferred here specifically because it transfers more directly to later problems in this pattern — for instance, LC 99 (Recover BST) is built on reasoning about exactly this kind of inorder-violation, and the discipline of "track valid ranges explicitly" tends to generalize better than "remember the last value I saw."

---

# Stage 3: Coding

## Approach 1 — Brute Force: Local Child Comparison (Incorrect!)

**The honest, tempting-but-wrong thinking:**

> "At every node, just check that it's bigger than its left child and smaller than its right child, then recurse."
> 

```java
class Solution {
    public boolean isValidBST(TreeNode root) {
        if (root == null) return true;

        if (root.left != null && root.left.val >= root.val) return false;
        if (root.right != null && root.right.val <= root.val) return false;

        return isValidBST(root.left) && isValidBST(root.right);
    }
}
```

**Why this is wrong, not just suboptimal:** as shown in Stage 2's trap example, this fails to catch violations that come from an ancestor further up than the immediate parent. This isn't included as a "slower but correct" baseline — it's included specifically because it's the natural first instinct, and recognizing exactly why it's wrong is the whole point of this problem.

---

## Approach 2 — Alternative: Inorder Strictly Increasing

**Mental workflow:**

```
1. Do an inorder traversal (left, root, right)
2. Track "previous value visited" — start as null (nothing visited yet)
3. At each visit:
   → if previous is not null AND current value <= previous → return false
   → update previous = current value
4. If the traversal completes without any violation → valid BST
```

```java
class Solution {
    private Long prev = null; // Long (boxed) so it can start as null

    public boolean isValidBST(TreeNode root) {
        if (root == null) return true;

        // Step 1: fully process the left subtree first
        if (!isValidBST(root.left)) return false;

        // Step 2: this is the "visit" moment — compare against
        // whatever was visited immediately before this, in
        // inorder sequence
        if (prev != null && root.val <= prev) return false;
        prev = (long) root.val;

        // Step 3: process the right subtree
        return isValidBST(root.right);
    }
}
```

Using `Long` (not `long`) lets `prev` start as `null`, cleanly representing "nothing visited yet" without needing a sentinel numeric value that might collide with a legitimate node value.

---

## Approach 3 — Optimal: Valid Range Propagation

**Mental workflow before writing a single line:**

```
1. Write solve(node, lowerBound, upperBound):
   → base case: node == null → true (nothing to violate)
   → if node.val is NOT strictly between lowerBound and
     upperBound → return false immediately
   → recurse left, narrowing the UPPER bound to node.val
     (lower bound stays the same, inherited)
   → recurse right, narrowing the LOWER bound to node.val
     (upper bound stays the same, inherited)
   → return true only if BOTH recursive calls return true

2. Start the call with the widest possible range:
   solve(root, Long.MIN_VALUE, Long.MAX_VALUE)
```

```java
class Solution {
    public boolean isValidBST(TreeNode root) {
        // Start with no restriction at all — any value is
        // legal at the root. Using Long (not int) bounds
        // because node values can legitimately be
        // Integer.MIN_VALUE or Integer.MAX_VALUE themselves,
        // and the bound needs room to sit strictly outside
        // that without overflowing.
        return solve(root, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    private boolean solve(TreeNode node, long lower, long upper) {
        // Base case: an empty subtree can't violate anything
        if (node == null) return true;

        // THIS node must fall strictly inside the range handed
        // down from its ancestors. This single check encodes
        // every ancestor's constraint simultaneously, not just
        // the immediate parent's.
        if (node.val <= lower || node.val >= upper) {
            return false;
        }

        // Moving LEFT: this subtree's values must all be
        // smaller than the current node — so the UPPER bound
        // tightens to node.val. Lower bound is inherited as-is.
        boolean leftValid = solve(node.left, lower, node.val);

        // Moving RIGHT: this subtree's values must all be
        // bigger than the current node — so the LOWER bound
        // tightens to node.val. Upper bound is inherited as-is.
        boolean rightValid = solve(node.right, node.val, upper);

        return leftValid && rightValid;
    }
}
```

---

## Workflow Trace on the Invalid Example

```
        5
       / \
      1   4
         / \
        3   6
```

```
solve(5, -inf, +inf):
  5 is strictly inside (-inf, +inf) ✓
  → solve(1, -inf, 5)
  → solve(4, 5, +inf)

  solve(1, -inf, 5):
    1 is strictly inside (-inf, 5) ✓
    → solve(null, -inf, 1) → true
    → solve(null, 1, 5) → true
    → returns true

  solve(4, 5, +inf):
    Is 4 strictly inside (5, +inf)?
    4 <= 5 → VIOLATION → returns false immediately
    (node 4's own left/right children are never even visited —
     the violation is caught at node 4 itself, using the range
     inherited from the root, without needing to inspect node 3)

solve(5, ...) = leftValid(true) AND rightValid(false) = false
```

**Final answer: `false`** — correctly invalid, and caught at the exact node whose value first breaks an inherited ancestor constraint, not merely a local parent-child mismatch.

---

## Comparing the Two Correct Approaches

| Property | Valid Range Propagation | Inorder Strictly Increasing |
| --- | --- | --- |
| What's checked | Node's value against an explicit inherited `(lower, upper)` window | Current value against the single previous value in traversal order |
| Failure mode if implemented carelessly | Hard to get wrong — the range is explicit at every call | Easy to under-track "previous" across subtree boundaries if not handled as genuine shared state |
| Transfers to LC 99 (Recover BST)? | Less directly — that problem is inorder-violation based | Yes — directly reuses the "compare to previous in inorder" idea |
| Traversal shape | Preorder-style (check current node before recursing) | True inorder (left, then check, then right) |
| Early exit | Yes — stops the instant one node fails its range | Yes — stops the instant one comparison fails |

---

## Complexity Analysis

**Time Complexity — O(n):**

Both approaches visit every node at most once. In the range-propagation version, `solve()` is called once per node, does O(1) work (a comparison, then two recursive calls), so total work across `n` nodes is **O(n)**. Same reasoning applies to the inorder version — each node is visited exactly once during the traversal.

**Space Complexity — O(h), where h is the height of the tree:**

The only auxiliary space in either approach is the recursion call stack, one frame per node on the current root-to-node path.

- **Balanced BST:** `h = O(log n)`.
- **Worst case (skewed tree):** `h = O(n)`.

The range-propagation version's bounds (`lower`, `upper`) are passed as simple `long` parameters — no extra heap allocation per call. The inorder version's `prev` is a single shared field, not something that grows with tree size. So both approaches have identical **O(h)** space bounds.

---

## The Key Takeaway

```
┌──────────────────────────────────────────────────────────────────┐
│  THE TRAP: checking a node only against its immediate            │
│  children is NOT enough — a node can look locally fine and       │
│  still violate the BST property with respect to an ancestor      │
│  several levels up.                                              │
│                                                                  │
│  THE FIX: propagate a valid (lower, upper) RANGE downward.       │
│      Moving LEFT  → tighten the UPPER bound to the parent value  │
│      Moving RIGHT → tighten the LOWER bound to the parent value  │
│      At every node: check its value falls STRICTLY inside        │
│      the range it inherited.                                     │
│                                                                  │
│  This range accumulates every ancestor's constraint at once —    │
│  not just the most recent one — which is exactly why it          │
│  catches violations local-comparison misses.                     │
│                                                                  │
│  ALTERNATIVE: inorder traversal must be strictly increasing.     │
│  Also correct, but easier to implement subtly wrong if           │
│  "previous value" isn't tracked as genuine running state.        │
│                                                                  │
│  Time  — O(n): every node visited at most once, either way.      │
│  Space — O(h): recursion stack depth = height of the tree.       │
└──────────────────────────────────────────────────────────────────┘
```