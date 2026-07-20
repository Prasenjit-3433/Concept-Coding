# LC 450. Delete Node in a BST

Key Concept: Three cases — leaf, one child, two children with inorder successor replacement
Solution: https://www.youtube.com/watch?v=kouxiP_H5WE&list=PLgUwDviBIf0q8Hkd7bK2Bpryj2xVJk8Vk&index=45&ab_channel=takeUforward
Status: Done

# Stage 1: Identification

## **Step 1 — Which topic?**

You are given the root of a **Binary Search Tree** and a `key`. You need to delete the node holding that key (if it exists) and return the root of the modified tree, such that the BST property still holds everywhere afterward.

## **Step 2 — Which pattern?**

Same family as search and insert — the trigger is direct:

> "delete" a node from a BST → **Pattern 1: Basic BST Operations**
> 

This is the hardest of the three basic operations, and the reason is structural: search and insert only ever needed to find or create **one** node. Deletion has to correctly **stitch the tree back together** after removing a node that might have two children hanging off it — and that stitching is where all the real difficulty lives.

## **Step 3 — Which key concept?**

**Structural Splice — attach the deleted node's right subtree onto the rightmost node of its left subtree, then promote the left subtree upward. No value copying anywhere.**

This deserves a sharper name than the sheet's original "inorder successor replacement," because Striver's actual code here does **not** use the classic successor-value-copy technique most resources teach. That classic technique copies the *value* of the inorder successor into the node being deleted, then recursively deletes the successor from the right subtree. Striver's approach is purely **structural** — it never copies a value into any node. It **splices** two existing subtrees together directly and promotes one of them to take the deleted node's place. Every node keeps its own identity; only pointers move.

---

# Stage 2: Intuition Building

## The Three Cases, Stated Plainly

Before any clever trick, ask the basic question: when you remove a node, what has to happen to whatever was hanging beneath it?

```
Case 1 — Node has NO children (a leaf):
    → Just remove it. Nothing needs to be reattached.

Case 2 — Node has EXACTLY ONE child:
    → That child simply takes the deleted node's place.
      Nothing about ordering breaks — the single child's
      entire subtree was already correctly positioned
      relative to everything else.

Case 3 — Node has TWO children:
    → This is the only case that's genuinely hard.
      Both a left subtree and a right subtree need to
      survive, but only ONE thing can sit in the deleted
      node's old spot.
```

Cases 1 and 2 collapse into a single observation: if at most one child exists, whichever child there is (possibly none) simply **replaces** the deleted node directly.

## The Hard Case — Two Children

This is where the real thinking happens. You have a node with a left subtree (everything smaller) and a right subtree (everything larger). The node itself is being removed. Something has to become the new "root" of this combined region, and it has to sit correctly between everything in the left subtree and everything in the right subtree.

The classic textbook approach: find the node's **inorder successor** (the smallest value in the right subtree — reached by going right once, then left as far as possible), **copy that value** into the node being deleted, and then recursively delete the successor from the right subtree (which is now guaranteed to be an easy case, since the successor itself can have at most a right child).

Striver's code takes a different, purely structural route. Ask a different question instead:

> *"Rather than copying a value, can I just directly glue the left subtree and the right subtree together into one valid BST — and let one of them simply become the new subtree root?"*
> 

## Why "Rightmost of the Left Subtree" Is the Splice Point

Think about what the **left subtree** already guarantees: every value in it is smaller than the node being deleted. Within that left subtree, its **rightmost node** (walk right, right, right, until there's no right child left) is the **largest value inside the entire left subtree**.

Now think about the **right subtree**: every value in it is larger than the node being deleted — which also means every value in the right subtree is larger than *every* value in the left subtree (since the left subtree's largest value is still smaller than the deleted node, and the right subtree's smallest value is still larger than the deleted node).

```
┌─────────────────────────────────────────────────────────────────┐
│  left subtree's maximum  <  deleted node  <  right subtree's          │
│                                              minimum                  │
│                                                                       │
│  So: left subtree's maximum < EVERY value in the right subtree        │
└─────────────────────────────────────────────────────────────────┘
```

That single fact is the whole trick. If you take the **rightmost node of the left subtree** (its maximum) and attach the **entire right subtree** as that node's **right child**, you haven't broken anything — that rightmost node has no right child of its own yet (by definition, it's the rightmost node), and everything you're attaching to it is guaranteed to be larger than it, and larger than everything else already in the left subtree.

## Why This Keeps the Tree Sorted (The Real Proof)

A BST is valid precisely when its **inorder traversal produces a sorted sequence**. Before deletion, inorder of the whole affected region looked like:

```
[ left subtree, sorted ] → [ deleted node ] → [ right subtree, sorted ]
```

After the splice — attaching the right subtree onto the left subtree's rightmost node, then promoting the left subtree to replace the deleted node entirely — the new inorder sequence becomes:

```
[ left subtree, sorted ] → [ right subtree, sorted ]
```

The deleted node's value is simply gone from the middle, and everything else remains exactly in sorted relative order — because concatenating two already-sorted sequences, where every element of the first is smaller than every element of the second, produces another sorted sequence. No value needed to move or be copied for this to hold.

```
┌───────────────────────────────────────────────────────────────────────┐
│  mergeChilds(node):                                                   │
│                                                                       │
│  if node has no left child  → right child replaces it directly        │
│  if node has no right child → left child replaces it directly         │
│  if node has BOTH:                                                    │
│      1. find the rightmost node of the LEFT subtree                   │
│      2. attach the ENTIRE right subtree as that node's                │
│         right child                                                   │
│      3. the LEFT subtree (now carrying the right subtree too)         │
│         becomes the replacement for the deleted node                  │
│                                                                       │
│  WHY VALID: left-subtree-max < everything in right subtree,           │
│  so gluing them this way keeps inorder traversal sorted —             │
│  which is the exact definition of a valid BST.                        │
└───────────────────────────────────────────────────────────────────────┘
```

---

## My Own Example — Chosen to Cover Every Case

```
6 3
6 9
3 2
3 5
9 8
9 11
2 1
2 null
5 4
5 null
11 10
11 12
```

Reconstructed:

```
                    6
                 /     \
                3        9
               / \      / \
              2   5    8   11
             /   /        /  \
            1   4        10   12
```

This single tree is deliberately built so that deleting different nodes exercises every distinct case:

- `1` — a **leaf** (no children)
- `4` — a **leaf** hanging off `5` (tests the "left child has no right subtree" shape)
- `5` — **one child** (`4` on the left, nothing on the right)
- `9` — **two children**, non-root, where the left subtree's root (`8`) already has no right child (splice point found immediately)
- `6` — **two children**, at the **root itself**

---

### Trace 1 — Deleting `1` (Leaf, No Children)

```
curr = 6:  6 < 1?  No → left branch
           curr.left(3).val == 1?  No → curr = 3

curr = 3:  3 < 1?  No → left branch
           curr.left(2).val == 1?  No → curr = 2

curr = 2:  2 < 1?  No → left branch
           curr.left(1).val == 1?  YES → curr.left = mergeChilds(1)
           break
```

**Inside `mergeChilds(1)`:** node `1` has `left == null` → return `root.right`, which is also `null`. So `2`'s left pointer becomes `null`. The leaf is simply gone — nothing needed reattaching. This confirms Case 1 falls straight out of the same `mergeChilds` function without any special-casing at the call site.

---

### Trace 2 — Deleting `5` (Exactly One Child)

Working from the **original** tree again (each trace independent):

```
curr = 6:  6 < 5?  No → left branch
           curr.left(3).val == 5?  No → curr = 3

curr = 3:  3 < 5?  Yes → right branch
           curr.right(5).val == 5?  YES → curr.right = mergeChilds(5)
           break
```

**Inside `mergeChilds(5)`:** node `5` has `left = 4`, `right = null`. Since `root.right == null`, return `root.left` (node `4`) directly. Node `3`'s right pointer now points straight to `4`.

Result:

```
                    6
                 /     \
                3        9
               / \      / \
              2   4    8   11
             /             /  \
            1             10   12
```

Node `4` — previously `5`'s left child — has taken `5`'s exact former position. Nothing about ordering broke: `4` is still correctly greater than `3` and still correctly less than `6`.

---

### Trace 3 — Deleting `9` (Two Children, Splice Point Found Immediately)

Working from the original tree:

```
curr = 6:  6 < 9?  Yes → right branch
           curr.right(9).val == 9?  YES → curr.right = mergeChilds(9)
           break
```

**Inside `mergeChilds(9)`:** node `9` has both `left = 8` and `right = 11`.

```
findRightMost(root.left = 8):
    start at 8 → 8.right = null → stop immediately
    returns node 8 itself

Attach: node 8's right child = node 9's right subtree (rooted at 11)
Return: node 8 — this replaces node 9
```

Result:

```
                    6
                 /     \
                3        8
               / \        \
              2   5        11
             /   /        /  \
            1   4       10    12
```

This is the edge case flagged in Stage 2: the left subtree's root (`8`) had **no right child to begin with**, so `findRightMost` terminates on its very first check and `8` absorbs the entire right subtree directly onto its own right pointer. No walking down multiple levels was needed — but the mechanism is identical to a deeper case; it just happens to bottom out in one step.

Verify inorder around this region: `..., 8, 10, 11, 12, ...` — strictly increasing, `9` cleanly gone.

---

### Trace 4 — Deleting `6` (Two Children, at the Root)

```
deleteNode(root, 6):
    root.val == 6 == key → directly return mergeChilds(root)
```

**Inside `mergeChilds(6)`:** left = `3`'s subtree, right = `9`'s subtree.

```
findRightMost(root.left = 3):
    start at 3 → 3.right = 5 → move to 5
    5.right = null → stop
    returns node 5

Attach: node 5's right child = node 6's right subtree (rooted at 9)
Return: node 3 — this becomes the new root
```

Result:

```
                    3
                 /     \
                2        5
               /           \
              1             9
                            / \
                           8   11
                              /  \
                            10    12
```

Node `3` — formerly the root's left child — absorbs the entire right subtree onto `5` (its own rightmost descendant) and gets promoted to the very top. This is the exact same mechanism as Trace 3, just triggered from the root-handling branch instead of the walk loop, and this time `findRightMost` had to walk one extra step (`3 → 5`) before finding the true rightmost node.

Verify inorder: `1, 2, 3, 5, 8, 9, 10, 11, 12` — fully sorted, `6` is gone, nothing else moved out of relative order.

---

## Why the Search-and-Splice Never Needs a Separate "Parent" Variable

Look closely at the shape of the main `deleteNode` loop — it never actually **moves onto** the node being deleted. At every step, `curr` checks **its own child** against the key *before* deciding to move there:

```
if (curr.right != null && curr.right.val == key) {
    curr.right = mergeChilds(curr.right);   // rewire directly, no parent needed
    break;
} else {
    curr = curr.right;                      // only move if the child ISN'T the target
}
```

This "look before you leap" navigation means `curr` is *always* already sitting at the exact parent of the node about to be spliced out, the moment splicing needs to happen. There's no need to separately track "the previous node visited" the way some parent-tracking implementations require — the very structure of checking-before-moving guarantees `curr` never overshoots onto the target itself.

```
┌───────────────────────────────────────────────────────────────────────┐
│  Standard BST compass walk: move onto a node, THEN inspect it.        │
│  This walk: inspect the CHILD first, only move if it's not            │
│  the target.                                                          │
│                                                                       │
│  Result: curr is always the parent of whatever gets deleted,          │
│  with zero extra bookkeeping.                                         │
└───────────────────────────────────────────────────────────────────────┘
```

The root itself is the one case this walk can't handle (a node has no "parent" pointer sitting above the root) — which is exactly why `if (root.val == key) return mergeChilds(root);` is checked separately, before the loop even starts.

---

# Stage 3: Coding

## Brute Force — Rebuild From Scratch

**The honest (wasteful) thinking:**

> "Collect every value in the tree except the one to delete via an inorder traversal (giving a sorted list for free), then rebuild a fresh BST from that sorted list."
> 
- Inorder traversal → O(n) to collect all values, skipping the key
- Rebuild a height-balanced BST from a sorted array (same technique as LC 108) → O(n)
- Total: O(n), but throws away the entire original tree structure and reconstructs everything, even the vast majority of nodes that were never anywhere near the deleted one

Completely impractical compared to a targeted fix — this establishes a correctness baseline only, no code needed.

---

## Better — Classic Recursive Approach (Inorder Successor Value-Copy)

**The thinking most resources teach first:**

> "Recurse down to find the node. If it has 0 or 1 children, replace it directly. If it has 2 children, find its inorder successor (leftmost of the right subtree), copy that successor's VALUE into the current node, then recursively delete the successor from the right subtree."
> 

```java
class Solution {
    public TreeNode deleteNode(TreeNode root, int key) {
        if (root == null) return null;

        if (key < root.val) {
            root.left = deleteNode(root.left, key);
        } else if (key > root.val) {
            root.right = deleteNode(root.right, key);
        } else {
            // found the node to delete
            if (root.left == null) return root.right;
            if (root.right == null) return root.left;

            // two children: find inorder SUCCESSOR
            // (smallest value in the right subtree)
            TreeNode successor = root.right;
            while (successor.left != null) {
                successor = successor.left;
            }

            // copy the successor's VALUE into this node
            root.val = successor.val;

            // recursively delete the successor from the
            // right subtree (it now has at most a right child,
            // so this recursive call is guaranteed easy)
            root.right = deleteNode(root.right, successor.val);
        }

        return root;
    }
}
```

**Why this works but is worth contrasting:** it's correct and also O(h), but it **mutates the value of the node being "kept"** — the node object that physically sits where `6` used to be still exists, but its `.val` field is silently changed to the successor's value. If any external code held a reference to that specific node object expecting it to always represent `6`, this approach would surprise it. It also does a second, nested recursive descent to actually remove the successor. Correct, but not the technique Striver's code uses.

---

## Optimal — Striver's Iterative Structural Splice

**Mental workflow before writing a single line:**

```
1. Edge case: root == null → nothing to delete → return null

2. If root itself is the target → return mergeChilds(root) directly
   (no parent exists above the root, so this case is handled
   separately, outside the main walk)

3. Otherwise, walk the tree using the BST compass, but ALWAYS
   check the CHILD before moving onto it:
   → if curr.val < key: target must be on the right
       → if curr.right IS the target: splice it out, break
       → else: move curr to curr.right, continue
   → if curr.val >= key: target must be on the left
       → if curr.left IS the target: splice it out, break
       → else: move curr to curr.left, continue

4. mergeChilds(node) — the actual splice:
   → no left child  → return right child (right takes its place)
   → no right child → return left child (left takes its place)
   → both exist:
       → find the rightmost node of the left subtree
       → attach the right subtree as that node's new right child
       → return the left subtree — it becomes the replacement

5. Return root (unchanged reference, unless root itself was
   the target handled in step 2)
```

```java
class Solution {

    // Walks straight down the right-child chain until there's
    // no further right child — this is the LARGEST value in
    // the given subtree (the "rightmost" node).
    private TreeNode findRightMost(TreeNode root) {
        while (root != null && root.right != null) {
            root = root.right;
        }
        return root;
    }

    // The core splice operation. Given a node that is ABOUT TO
    // BE DELETED, returns whatever should take its place —
    // purely by rewiring existing subtrees, never by copying
    // any node's value.
    private TreeNode mergeChilds(TreeNode root) {
        // Case: no left child → right child simply replaces it
        if (root.left == null) {
            return root.right;
        }
        // Case: no right child → left child simply replaces it
        else if (root.right == null) {
            return root.left;
        }

        // Case: BOTH children exist — the real splice.
        // The rightmost node of the LEFT subtree holds the
        // largest value smaller than `root`. Since every value
        // in root's RIGHT subtree is larger than root itself,
        // it's safe to attach the entire right subtree there.
        TreeNode rightMostLeftSubtree = findRightMost(root.left);
        rightMostLeftSubtree.right = root.right;

        // The left subtree — now carrying the right subtree
        // grafted onto its rightmost node — becomes the
        // replacement for the deleted node.
        return root.left;
    }

    public TreeNode deleteNode(TreeNode root, int key) {
        if (root == null) return null;

        // The root itself is the target — no parent exists
        // above it, so this must be handled before the walk.
        if (root.val == key) return mergeChilds(root);

        TreeNode curr = root;

        while (curr != null) {
            if (curr.val < key) {
                // Target must live in the right subtree.
                // Check the CHILD directly, before moving onto it —
                // this is what guarantees `curr` is always the
                // parent of whatever gets deleted.
                if (curr.right != null && curr.right.val == key) {
                    curr.right = mergeChilds(curr.right);
                    break;
                } else {
                    curr = curr.right;
                }
            } else {
                // Target must live in the left subtree
                // (this branch also covers curr.val == key,
                // but that case is only reachable via a CHILD
                // check below, never by curr itself equaling key —
                // curr never lands ON the target node)
                if (curr.left != null && curr.left.val == key) {
                    curr.left = mergeChilds(curr.left);
                    break;
                } else {
                    curr = curr.left;
                }
            }
        }

        return root;
    }
}
```

---

## Complexity Analysis

### **Time Complexity — O(h), where h is the height of the tree:**

The main walk moves one level deeper per iteration, exactly like search and insert — O(h) in the worst case to *locate* the parent of the target. Once located, `mergeChilds` does at most one additional walk — `findRightMost` on the left subtree — which is itself bounded by the height of that subtree, so also O(h) in the worst case.

```
Locating the target's parent  → O(h)
Splicing (findRightMost call) → O(h)
─────────────────────────────────────
Total                         → O(h)
```

- **Balanced BST:** `h = O(log n)`.
- **Worst case (skewed BST):** `h = O(n)`.

### **Space Complexity — O(1):**

The entire algorithm — the main walk and both helper functions (`findRightMost`, `mergeChilds`) — is written iteratively, using only a constant number of pointer variables. No recursion, no auxiliary data structure whose size scales with the tree. This is a genuine improvement over the classic recursive successor-copy approach, which costs O(h) in call-stack space.

---

## Comparing the Two Approaches for the Two-Children Case

| Property | Classic (Inorder Successor Value-Copy) | Striver's (Structural Splice) |
| --- | --- | --- |
| Modifies existing node values? | Yes — copies successor's value into the deleted node's slot | Never — every surviving node keeps its original value |
| Extra recursive descent needed? | Yes — a second delete call to remove the successor | No — one `findRightMost` walk, done |
| Splice point | Successor = leftmost of RIGHT subtree | Predecessor = rightmost of LEFT subtree |
| Space | O(h) — recursive call stack | O(1) — fully iterative |
| Core mechanism | Copy value, then recursively delete elsewhere | Directly rewire two existing subtrees together |

---

## The Key Takeaway

```
┌──────────────────────────────────────────────────────────────────┐
│  Three cases when deleting a node:                               │
│      no children     → remove it, nothing to reattach            │
│      one child       → that child directly replaces it           │
│      two children    → SPLICE: attach the right subtree onto     │
│                         the rightmost node of the left subtree,  │
│                         then promote the left subtree upward     │
│                                                                  │
│  WHY THE SPLICE IS VALID: left subtree's maximum value is        │
│  always smaller than EVERY value in the right subtree (both      │
│  are bounded by the deleted node itself). Gluing the right       │
│  subtree onto the left subtree's rightmost (largest) node        │
│  keeps the inorder traversal sorted — which is the exact         │
│  definition of a valid BST. No value ever needs to be copied.    │
│                                                                  │
│  NAVIGATION TRICK: check a CHILD against the key before moving   │
│  onto it, never land directly on the target. This guarantees     │
│  the current pointer is always the parent of whatever needs      │
│  to be spliced, with zero extra bookkeeping.                     │
│                                                                  │
│  Time  — O(h): O(log n) balanced, O(n) worst case (skewed).      │
│  Space — O(1): fully iterative, no recursion anywhere.           │
└──────────────────────────────────────────────────────────────────┘
```