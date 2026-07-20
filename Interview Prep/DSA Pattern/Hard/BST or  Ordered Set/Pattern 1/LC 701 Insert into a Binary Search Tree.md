# LC 701. Insert into a Binary Search Tree

Key Concept: Same navigation as search — insert at the correct null position
Solution: https://www.youtube.com/watch?v=FiFiNvM29ps&list=PLgUwDviBIf0q8Hkd7bK2Bpryj2xVJk8Vk&index=44&ab_channel=takeUforward
Status: Done

# Stage 1: Identification

## **Step 1 — Which topic?**

You are given the root of a **Binary Search Tree** and a value that is guaranteed **not** to already exist in the tree. You need to insert it somewhere in the tree such that the BST property — left subtree smaller, right subtree greater, recursively at every node — still holds afterward, and return the root of the modified tree.

## **Step 2 — Which pattern?**

Same family as the previous problem — the trigger is direct:

> "insert" into a BST → **Pattern 1: Basic BST Operations**
> 

## **Step 3 — Which key concept?**

**`Follow the BST Compass until you hit null — that null IS the insertion point.`**

This problem reuses the exact same navigation idea from LC 700 almost unchanged. In search, you followed the compass (left if smaller, right if greater) until you either found a match or ran into `null` — and `null` meant "the value doesn't exist." Here, the value is *guaranteed* not to exist, so you know in advance the search will always end at `null`. The entire trick of this problem is realizing that **the exact `null` position the search would have failed at is precisely where the new value belongs.** Insertion isn't a separate operation from search — it's search, continued one tiny step further: instead of returning `null`, you replace that `null` with a brand-new node.

---

# Stage 2: Intuition Building

## Multiple Correct Answers — And Why That's Not a Problem

Before touching the algorithm, Striver makes an important point worth internalizing first: **there is no single correct output tree.** A value like `5` could legally be inserted at several different positions in a BST and the tree would still be perfectly valid at every one of them — the shape changes, but the BST property (left smaller, right greater, everywhere) is preserved either way. The problem only asks for *any one* valid result, not a specific one. This immediately tells you something important: **you don't need to search for the "best" or "most balanced" place to put the new value — you just need any position that keeps the invariant intact, and the simplest such position is a leaf.**

## The Core Question — Where Would This Value "Have Been"?

Take a value you need to insert — say `5` — and ask the same question you'd ask if you were merely *searching* for `5` in the tree, even though you already know it isn't there:

> *"If `5` DID exist somewhere in this tree, where would the search for it have ended up?"*
> 

Walk through the tree exactly like a search: at every node, compare the target against the current node's value, and let the BST compass tell you which side to go — smaller goes left, greater goes right — exactly the same directional logic as LC 700. Eventually, this walk runs out of tree and lands on `null`. That `null` position is not a dead end here — it is **exactly** the correct leaf slot for the new value. Nothing else needs to be figured out.

```
┌───────────────────────────────────────────────────────────────────────┐
│  Insertion = Search, one step further.                                │
│                                                                       │
│  A search for a value NOT in the tree always terminates at a          │
│  null. That specific null is the ONLY place a new node could go       │
│  while keeping every ancestor's ordering guarantee intact —           │
│  because every step that led to that null was a correct,              │
│  BST-consistent directional decision.                                 │
│                                                                       │
│  So: walk the compass like a search. The moment you'd normally        │
│  return null, create a new node there instead.                        │
└───────────────────────────────────────────────────────────────────────┘
```

## Why the Insertion Point Must Be a Leaf Position

This deserves a moment of justification, not just acceptance. Every step of the walk moves strictly downward — from a node to one of its children. The walk only stops when it reaches a position with **no child at all** in the required direction (a `null` left or right pointer). That "no child" slot, by definition, is a spot that would become a **new leaf** if filled — you are never inserting into the *middle* of the tree, splitting an existing edge; you're always attaching a brand-new node at the boundary where the tree currently stops growing in that direction.

### A Worked Example

```
4 2
4 7
2 1
2 3
7 6
7 8
```

Picture this as:

```
              4
            /   \
           2     7
          / \   / \
         1   3 6   8
```

Insert `5`:

```
At node 4:  5 > 4  → BST compass says: go RIGHT, discard the entire
                      left subtree (rooted at 2) — everything there
                      is < 4, so it's definitely < 5 too, irrelevant
            → move to node 7

At node 7:  5 < 7  → go LEFT, discard the entire right subtree
                      (rooted at 8) — everything there is > 7,
                      so it's definitely > 5, irrelevant
            → move to node 6

At node 6:  5 < 6  → go LEFT
            → node 6 has no left child at all (null)
            → THIS is the insertion point.
              Create a new node holding 5, attach it as
              node 6's left child.
```

Result:

```
              4
            /   \
           2     7
          / \   / \
         1   3 6   8
              /
             5
```

Verify the invariant still holds at every affected ancestor: `5 < 6` ✓ (it's in `6`'s left subtree, correctly smaller). `5 < 7` ✓ (still correctly in `7`'s left subtree — the entire left subtree of 7, including the newly added 5, remains smaller than 7). `5 > 4` ✓ (still correctly in `4`'s right subtree). The insertion is valid at every level, precisely because each directional step taken to reach it was itself a valid BST comparison.

## Why "Any Valid Position" Doesn't Mean "Any Random Position"

It's worth being precise about what "multiple correct answers exist" actually means. The problem doesn't mean you can drop the new node *anywhere* — it means that **different search-paths through different-shaped (but still valid) BSTs would produce different, individually correct, leaf positions.** Given *this specific* tree's actual shape, there is exactly one leaf position the compass-following walk will lead you to, and that's the one you must use. The "multiple possible answers" freedom exists across different valid tree *shapes*, not as multiple choices available to you *within* one fixed input tree.

## Why the Walk Always Terminates at a Valid Insertion Point

Every step moves one level deeper — to a child — and the tree is finite, so eventually a `null` child pointer is reached. Because the value is guaranteed not to already exist in the tree, the walk can never terminate via an exact-match stop (unlike LC 700, where a match was a valid stopping condition) — it will always run out of tree and hit `null`. That guarantee is exactly why this problem never has to handle a "value already exists" case at all.

## Reusing the Compass, With One Structural Difference From Search

In LC 700, the moment you hit `null`, you simply *returned* it — the search had failed, nothing further to do. Here, hitting that same `null` is the trigger to **do work**: create a new node and attach it as the child of whatever node you were just standing at. This means the walk needs to keep a reference to the **previous** node (the parent of wherever `null` was found), not just chase pointers forward and lose track of where you came from — because the new node has to be wired into the tree at that exact parent's left or right slot.

```
┌───────────────────────────────────────────────────────────────────────┐
│  Search (LC 700):    walk compass → hit null → return null            │
│  Insert (LC 701):    walk compass → hit null → CREATE a node          │
│                       HERE, wire it to the parent you were            │
│                       just standing at, done                          │
│                                                                       │
│  Same compass. Same directional logic. One extra action               │
│  at the exact moment the search would have failed.                    │
└───────────────────────────────────────────────────────────────────────┘
```

---

# Stage 3: Coding

## Brute Force — There Isn't a Meaningfully Different One Here

Unlike some problems where a brute force exists as a slower but conceptually simpler baseline, insertion into a BST doesn't really have a separate "naive" approach worth coding — the compass-following walk itself is already the simplest possible correct approach; there's no wasteful alternative to contrast it against (you can't, say, "check every node" the way LC 700's brute force ignored the BST property, because insertion isn't a lookup — it's a placement decision that only makes sense in terms of the ordering invariant to begin with). So we go directly to the intended approach.

## Optimal — Walk the Compass, Insert at the First Null

**Mental workflow before writing a single line:**

```
1. Edge case: if root is null → the tree is empty, the new node
   itself becomes the entire tree → return new TreeNode(val)

2. Otherwise, walk from the root using the BST compass:
   → at the current node, compare val against curr.val
   → if val is greater: look right
       → if a right child exists, move curr to it, continue walking
       → if it doesn't exist (null), THIS is the spot —
         attach a new node here, stop
   → if val is smaller: look left, same logic mirrored

3. Return the (possibly untouched) root — the tree was modified
   in place via the parent's left/right pointer, so the root
   reference itself never needs to change
```

---

#### Striver's Version — Iterative

```java
class Solution {
    public TreeNode insertIntoBST(TreeNode root, int val) {
        if (root == null) return new TreeNode(val);

        TreeNode curr = root;

        while (true) {
            if (curr.val < val) {
                if (curr.right != null) {
                    curr = curr.right;
                } else {
                    curr.right = new TreeNode(val);
                    break;
                }
            } else {
                if (curr.left != null) {
                    curr = curr.left;
                } else {
                    curr.left = new TreeNode(val);
                    break;
                }
            }
        }

        return root;
    }
}
```

Walk through the shape of this loop carefully — it's doing exactly the two-part decision described in Stage 2, just written as nested conditionals instead of a single ternary (unlike LC 700's tighter version), because here each branch needs to do one of *two* different things depending on whether a child exists: either **move deeper** (if a child is already there) or **insert and stop** (if it's `null`). That's precisely why the structure needs the inner `if (curr.right != null) { move } else { insert; break }` shape — a plain `curr = curr.right` reassignment, like in the search problem, isn't enough here, because you need to detect the exact moment you're about to step onto a `null` and intervene *before* that happens, not after.

**Why an infinite `while (true)` loop, exited via `break`?** Because the natural exit condition — "I found the null slot and inserted into it" — isn't a simple pointer-based condition to check at the top of a loop the way LC 700's `while (root != null && root.val != val)` was. The loop needs to run until an insertion actually happens, and `break` is the cleanest way to say "insertion complete, stop now," right at the exact place the insertion occurs.

---

### Dry Run — Matching Striver's Traced Example

```
        4
       / \
      2   7
     / \ / \
    1  3 6  8

Insert val = 5
```

```
curr = root (4)

Iteration 1: curr.val (4) < val (5) → look right
  curr.right (7) is not null → curr = 7

Iteration 2: curr.val (7) < val (5)?  No, 7 is NOT less than 5
  → falls into the else branch → look left
  curr.left (6) is not null → curr = 6

Iteration 3: curr.val (6) < val (5)?  No, 6 is NOT less than 5
  → else branch → look left
  curr.left is null → THIS is the spot
  curr.left = new TreeNode(5)
  break

Return root (still node 4 — untouched reference,
             the tree was modified via node 6's left pointer)
```

Final tree:

```
        4
       / \
      2   7
     / \ / \
    1  3 6  8
          /
         5
```

Matches exactly what Stage 2's manual walk-through predicted.

---

## Why `root` Itself Is Always Safe to Return Unchanged

A subtlety worth calling out explicitly: except for the special empty-tree case, the `root` reference **never changes** throughout this function — insertion always happens by modifying some *descendant* node's `left` or `right` pointer, never the root pointer itself (unless the tree started empty, which is the one case handled separately at the very top). This is different from operations like deletion, where the root itself can sometimes need to be replaced. Here, the new node is always attached several levels below the root (or exactly at the root's own child slot, in a small tree) — the root object in memory is the same object before and after the function runs; only something reachable *from* it has changed.

---

## Complexity Analysis

### **Time Complexity — O(h), where h is the height of the tree:**

The exact same reasoning as LC 700 applies without modification. At every node visited, the work is O(1) — one comparison, one pointer check — and the walk moves to exactly one child per step, never both, until a `null` slot is found and filled. The number of steps taken equals the length of one root-to-insertion-point path.

- **Balanced BST:** `h = O(log n)`.
- **Worst case (skewed BST):** `h = O(n)`.

Striver explicitly notes this: **assuming the BST is height-balanced**, the time complexity is `O(log₂n)` — the same caveat as search: this is the expected-case bound, not a hard guarantee the structure itself enforces.

### **Space Complexity — O(1):**

The iterative version uses only a single pointer (`curr`) that gets reassigned as it walks — no recursion, no extra data structure whose size depends on the tree. This is a genuine improvement in space over a recursive equivalent, which would cost `O(h)` for the call stack, exactly as it did in LC 700's recursive version.

---

## Comparing This to LC 700 (Search)

| Property | LC 700 (Search) | LC 701 (Insert) |
| --- | --- | --- |
| What triggers stopping | Exact match found, OR null reached | Only null reached (value never pre-exists) |
| What happens at null | Return null — search failed | Create a new node there — this IS success |
| Needs to track parent? | No — just chase the pointer forward | Yes — must wire the new node into the parent's left/right slot |
| Return value | The found node (or null) | The (usually unchanged) root reference |
| Core mechanism | BST Compass | Same BST Compass, one step further |

---

## The Key Takeaway

```
┌───────────────────────────────────────────────────────────────────┐
│  Insertion is NOT a new algorithm — it's search, taken one        │
│  step past where search would normally give up.                   │
│                                                                   │
│  A value guaranteed not to exist will ALWAYS cause a search       │
│  walk to terminate at a null pointer. That exact null position    │
│  is the only leaf slot where a new node can be attached           │
│  without breaking any ancestor's BST ordering guarantee —         │
│  because every directional step taken to reach it was already     │
│  a valid, invariant-preserving comparison.                        │
│                                                                   │
│  So: walk the BST compass exactly like LC 700. The moment you     │
│  would return null, instead attach val = new TreeNode(val)        │
│  to whichever node you were just standing at.                     │
│                                                                   │
│  Multiple valid output trees can exist for the SAME value —       │
│  but only ONE is reachable from any ONE fixed input tree's        │
│  shape, and that's the one the compass walk finds.                │
│                                                                   │
│  Time  — O(h): O(log n) for a balanced BST, O(n) worst case.      │
│  Space — O(1) iterative (just a moving pointer), O(h) if written  │
│           recursively (call stack).                               │
└───────────────────────────────────────────────────────────────────┘
```