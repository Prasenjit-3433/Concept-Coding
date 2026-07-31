# LC 1008. Construct BST from Preorder Traversal

Key Concept: Value bounds — each value must fall within valid (min, max) range
Solution: https://www.youtube.com/watch?v=UmJT3j26t1I&list=PLgUwDviBIf0q8Hkd7bK2Bpryj2xVJk8Vk&index=49&ab_channel=takeUforward
Status: Pending

# Stage 1: Identification

## **Step 1 — Which topic?**

You are given an array representing the **preorder traversal** of a Binary Search Tree, with the guarantee that all values are distinct. You need to reconstruct the actual BST and return its root.

## **Step 2 — Which pattern?**

You're building a tree structure from traversal data, and specifically exploiting the fact that this traversal came from a *BST*, not a generic binary tree. The trigger:

> "construct BST from", given only **one** traversal array → **Pattern 4: BST Construction & Transformation**
> 

## **Step 3 — Which key concept?**

**Upper-Bound-Only Range Propagation — a node only ever needs an upper limit, never a lower one, because "cannot fit under the current ceiling" already implies "must be too big for here," which is exactly what a violated lower bound would have told you anyway.**

This is the single sharpest idea in the whole problem, and it's worth naming precisely because most people's first instinct is to carry *two* bounds (a `low` and a `high`) the way LC 98 (Validate BST) does. The transcript's entire "why efficient method works" section is built around proving you can drop the lower bound completely and the algorithm still produces the correct tree — for reasons specific to *preorder* traversal order, not true of every BST algorithm.

---

# Stage 2: Intuition Building

## Why Preorder Alone Is Enough — And Why That's Special

For a *generic* binary tree, one traversal is never enough to reconstruct the tree ***uniquely*** — you always need a second one (preorder + inorder, or postorder + inorder) to pin down the shape. But a BST carries an extra piece of information that a generic tree doesn't: **ordering**. At every node, you already know which values must be smaller (left) and which must be bigger (right) — you don't need inorder to tell you that split; the values themselves tell you, the moment you compare them against the current node.

So the entire problem becomes: **can preorder's specific visiting order (root, then left, then right) be walked in a way that reconstructs the tree using only comparisons, no second array needed?**

## The Brute Force First — Sort to Get Inorder, Then Use the Classic Two-Traversal Build

![image.png](LC%201008%20Construct%20BST%20from%20Preorder%20Traversal/image.png)

![image.png](LC%201008%20Construct%20BST%20from%20Preorder%20Traversal/image%201.png)

The first idea the transcript walks through: if a BST's inorder traversal is always sorted, and preorder already contains every value the tree has, then **sorting the preorder array produces the tree's inorder traversal** for free. Once you have both preorder and inorder, the classic "build tree from two traversals" technique (root = preorder's first unused element, split inorder at that value's position) reconstructs the tree exactly.

This works — but it's clearly wasteful. You're manufacturing a second traversal that didn't need to exist, purely by sorting, and then running the full two-traversal reconstruction machinery on top of it. That's the seed for the optimal idea: **if sorting basically hands you the "right subtree cutoff" for free, is there a way to get that same cutoff information without ever building the second array at all?**

## The Core Question — What Does Comparing a Value Against a Ceiling Actually Tell You?

![image.png](LC%201008%20Construct%20BST%20from%20Preorder%20Traversal/image%202.png)

Take the example: `8, 5, 1, 7, 10, 12`.

`8` is obviously the root — first element of any preorder. Now the walk proceeds *exactly like a normal preorder recursion* (root, then left, then right), but at every step, before placing a value into the tree, you ask one question:

> *"Is this value still small enough to fit under the ceiling I'm currently allowed?"*
> 

Start with the ceiling at `+infinity` — nothing is off-limits yet. Place `8` as the root. Now, following preorder's own natural order, the *next* element (`5`) is checked against a ceiling of `8` (because you're implicitly trying to place it as `8`'s left child first — preorder always tries left before right). `5 < 8` → fits → `5` becomes the left child, and the ceiling *tightens* to `5` for anything going further left from here.

Continue: `1` is checked against ceiling `5`. `1 < 5` → fits → becomes `1`'s spot in the tree, ceiling now tightens to `1`.

Next: `7`. Checked against ceiling `1`. `7` is **not** less than `1` → doesn't fit here. This is the exact moment the left-recursion for `1` gives up and returns — `1` gets no left child.

## Why "Doesn't Fit Under the Ceiling" Automatically Means "Go Right Instead" — No Lower Bound Needed

This is the crux the transcript spends the most time justifying, and it deserves to be stated precisely:

> *If a value fails the ceiling check while you're trying to place it as someone's LEFT child, that failure by itself already proves the value is too big for the left side — which is exactly the same information a lower-bound check would have told you. You don't need to separately verify "is this bigger than some floor" — "doesn't fit under this ceiling, while attempting a left placement" already means exactly that.*
> 

Concretely: `7` failed the ceiling check of `1` while `1` was trying to grow a left child. The *only* reason a value fails a left-child ceiling check is that it's too big — and "too big for the left" is precisely the condition under which it belongs somewhere to the *right* instead. There's no scenario where a value could fail a left-ceiling check for being "too small" — smallness never causes a ceiling violation, only bigness does. So the ceiling alone carries all the information a two-sided range would have carried, **specifically because of the direction you're currently recursing in**.

```
┌──────────────────────────────────────────────────────────────────────┐
│  A LOWER bound exists only to catch "this value is too small         │
│  to belong here." But by construction, values are only ever          │
│  tested against a ceiling while attempting a LEFT placement —        │
│  and failing that test can only ever mean "too big," never           │
│  "too small." So the lower bound would NEVER fire on its own —       │
│  it's redundant information, made redundant by preorder's            │
│  specific traversal direction.                                       │
└──────────────────────────────────────────────────────────────────────┘
```

## Tracing the Rest of the Example, Tracking the Ceiling Explicitly

Continue from where `7` failed to fit under `1`'s ceiling:

```
1's left child attempt failed (7 doesn't fit under ceiling 1) → 1 gets no left child
1's right child attempt: ceiling is INHERITED unchanged from 1's own incoming ceiling,
    which was 5 (the ceiling 1 itself was placed under)
    → 7 < 5 → FITS → 7 becomes 1's right child
```

This is the second half of the key concept: **moving right does NOT tighten the ceiling** — it inherits whatever ceiling was already in force. Moving right only ever means "I'm still inside the same left-subtree region my parent claimed, just exploring the upper portion of it" — the outer boundary (the ceiling that got you into this region in the first place) hasn't changed.

```
┌──────────────────────────────────────────────────────────────────────┐
│  Moving LEFT   → ceiling TIGHTENS to the current node's value        │
│                  (you're claiming "everything here must be           │
│                  smaller than me")                                   │
│                                                                      │
│  Moving RIGHT  → ceiling STAYS THE SAME as what was inherited        │
│                  (you're still within the region your parent         │
│                  claimed — just the upper half of it)                │
└──────────────────────────────────────────────────────────────────────┘
```

Continuing the trace: after `7` is placed as `1`'s right child, `7` tries a left child next — ceiling would tighten to `7`, but the next preorder element is `10`, which doesn't fit under `7`'s own inherited ceiling of `5` in the first place (checked before even reaching `7`'s children)... — walking this precisely the way the transcript does: `10` fails to fit anywhere under `5`'s region entirely (`10` isn't less than `5`), so the entire left subtree of `8` is now closed off, and the recursion unwinds all the way back to `8`, which now attempts its **right** child with its own inherited ceiling (`+infinity`, unchanged from what `8` itself was placed under). `10 < +infinity` → fits → `10` becomes `8`'s right child. Then `10` tries left (`12` fails — `12` isn't less than `10`), so `10` gets no left child, and `10` tries right with the *inherited* ceiling `+infinity` (unchanged, since moving right never tightens) → `12` fits → becomes `10`'s right child.

Final tree, matching the transcript's own worked diagram:

```
              8
           /     \
          5       10
         /          \
        1            12
         \
          7
```

## Why a Shared, Advancing Pointer Is Needed — Not Index-Based Recursion

One more structural detail worth calling out explicitly before coding: this recursion doesn't slice the array into left/right halves the way the classic two-traversal build does (there's no "inorder split index" to compute here). Instead, there is **one single moving pointer** through the preorder array, and every recursive call — whether building a left subtree or a right subtree — reads from wherever that shared pointer currently sits, and advances it. This is why the pointer must be passed **by reference** conceptually (in Java, via a one-element array, exactly as seen in LC 543's `maxDiameter[0]` trick) — every recursive call, no matter how deep, needs to see and mutate the *same* underlying position, not a local copy of it.

```
┌──────────────────────────────────────────────────────────────────────┐
│  Two-traversal build (LC 105/106): recursion is driven by            │
│  INDEX RANGES sliced from the inorder array at each call.            │
│                                                                      │
│  This problem (single traversal + BST property): recursion is        │
│  driven by a SHARED POINTER that advances through the SAME           │
│  preorder array on every call — no slicing, no second array,         │
│  just "have I already consumed this value, or is it still            │
│  ahead of me?"                                                       │
└──────────────────────────────────────────────────────────────────────┘
```

## Why the Recursion Terminates

Every recursive call either successfully places a value (which permanently advances the shared pointer forward by one — this can happen at most `n` times total, since the array has `n` elements) or fails the ceiling check and returns `null` immediately without advancing the pointer. Since the pointer only ever moves forward and the array is finite, the total number of successful placements is bounded by `n`, and the recursion cannot loop indefinitely — every path either plants a node and moves on, or hits a ceiling failure and returns.

---

# Stage 3: Coding

## Approach 1 — Brute Force: Sort to Get Inorder, Then Classic Two-Traversal Build

**The honest thinking:**

> "A BST's inorder traversal is always sorted, and preorder already contains every value in the tree. So if I sort a copy of preorder, I get inorder for free. Then I can use the standard preorder+inorder tree-construction technique."
> 

```java
class Solution {
    public TreeNode bstFromPreorder(int[] preorder) {
        int n = preorder.length;

        // Manufacture the inorder traversal by sorting a copy —
        // valid ONLY because this preorder is guaranteed to be
        // a BST's preorder (values are exactly the tree's values)
        int[] inorder = preorder.clone();
        Arrays.sort(inorder);

        // Map each value to its index in inorder, for O(1) split lookup
        Map<Integer, Integer> inorderIndex = new HashMap<>();
        for (int i = 0; i < n; i++) {
            inorderIndex.put(inorder[i], i);
        }

        int[] preIndex = {0}; // shared pointer into preorder
        return build(preorder, inorderIndex, preIndex, 0, n - 1);
    }

    private TreeNode build(int[] preorder, Map<Integer, Integer> inorderIndex,
                            int[] preIndex, int inStart, int inEnd) {
        if (inStart > inEnd) return null;

        int rootVal = preorder[preIndex[0]++];
        TreeNode root = new TreeNode(rootVal);

        int mid = inorderIndex.get(rootVal);

        // Left subtree comes from whatever inorder positions sit
        // BEFORE mid; right subtree from positions AFTER mid
        root.left = build(preorder, inorderIndex, preIndex, inStart, mid - 1);
        root.right = build(preorder, inorderIndex, preIndex, mid + 1, inEnd);

        return root;
    }
}
```

**Why this is wasteful:** it pays `O(n log n)` just to sort a second array that isn't structurally necessary, plus `O(n)` extra space to store both `inorder` and the `inorderIndex` map — all to re-derive information the BST property already hands you for free through simple comparisons. Establishes correctness only.

---

## Approach 2 — Optimal: Upper-Bound-Only Recursion With a Shared Pointer

**Mental workflow before writing a single line:**

```
1. Maintain a single shared pointer i into the preorder array,
   starting at 0. In Java: a one-element array, since primitives
   can't be passed by reference.

2. Write build(preorder, i, ceiling):
   → if i has run past the end of the array → return null
      (nothing left to place)
   → if preorder[i[0]] does NOT fit under ceiling
     (i.e., preorder[i[0]] >= ceiling) → return null
     (this value belongs elsewhere — don't consume it, don't advance i)
   → otherwise:
       → take preorder[i[0]] as this node's value, ADVANCE i
       → root.left  = build(preorder, i, ceiling = root.val)
              ← tighten the ceiling when going left
       → root.right = build(preorder, i, ceiling = SAME ceiling
                              inherited by this call)
              ← do NOT tighten when going right
       → return root

3. Call build(preorder, i, +infinity) to start
```

```java
class Solution {
    public TreeNode bstFromPreorder(int[] preorder) {
        // Shared pointer through the preorder array — every
        // recursive call, no matter how deep, reads from and
        // advances this SAME position. A one-element array
        // simulates pass-by-reference for this int in Java.
        int[] i = {0};

        // Start with no restriction at all — the root can be
        // any value, so the ceiling starts at +infinity
        return build(preorder, i, Integer.MAX_VALUE);
    }

    private TreeNode build(int[] preorder, int[] i, long ceiling) {
        // Base case 1: no elements left to place at all
        if (i[0] == preorder.length) {
            return null;
        }

        // Base case 2: the next unplaced value doesn't fit under
        // the ceiling currently in force. Because this check only
        // ever happens while attempting a LEFT placement (right
        // placements always inherit their parent's ceiling
        // unchanged), failing here can ONLY mean "too big" —
        // never "too small" — so no separate lower bound is needed.
        if (preorder[i[0]] >= ceiling) {
            return null;
        }

        // This value fits — consume it, ADVANCE the shared pointer
        TreeNode root = new TreeNode(preorder[i[0]]);
        i[0]++;

        // Going LEFT: tighten the ceiling to this node's own value —
        // everything in the left subtree must be smaller than root
        root.left = build(preorder, i, root.val);

        // Going RIGHT: inherit the SAME ceiling this node itself
        // was placed under. Moving right never tightens the ceiling —
        // you're still within the region your parent originally
        // claimed, just exploring its upper half.
        root.right = build(preorder, i, ceiling);

        return root;
    }
}
```

**Why `long ceiling` instead of `int`?** `Integer.MAX_VALUE` is used as the initial "no restriction" sentinel. If a node's actual value could legitimately equal `Integer.MAX_VALUE`, comparing `preorder[i[0]] >= ceiling` with both sides as `int` risks ambiguity at that exact boundary. Using `long` for the ceiling parameter keeps `Integer.MAX_VALUE` as a genuinely unreachable ceiling rather than a value collision risk — the same defensive reasoning used for `Long.MIN_VALUE`/`Long.MAX_VALUE` bounds in LC 98.

---

## Workflow Trace on Striver's Example

```
preorder = [8, 5, 1, 7, 10, 12]
i = [0]

build(ceiling = +inf):
  preorder[0] = 8, 8 < +inf → fits. Consume 8, i becomes [1].
  root = 8

  root.left = build(ceiling = 8):
    preorder[1] = 5, 5 < 8 → fits. Consume 5, i becomes [2].
    root = 5

    root.left = build(ceiling = 5):
      preorder[2] = 1, 1 < 5 → fits. Consume 1, i becomes [3].
      root = 1

      root.left = build(ceiling = 1):
        preorder[3] = 7, 7 < 1? NO → doesn't fit → return null
        (i stays at [3] — 7 was never consumed)
      → node 1's left = null

      root.right = build(ceiling = 5):     ← INHERITED, not tightened to 1
        preorder[3] = 7, 7 < 5? NO → doesn't fit → return null
        (i stays at [3])
      → node 1's right = null

      return node 1 (no children)

    root.right = build(ceiling = 8):        ← INHERITED, not tightened to 5
      preorder[3] = 7, 7 < 8 → fits. Consume 7, i becomes [4].
      root = 7

      root.left = build(ceiling = 7):
        preorder[4] = 10, 10 < 7? NO → return null
      → node 7's left = null

      root.right = build(ceiling = 8):      ← INHERITED, not tightened to 7
        preorder[4] = 10, 10 < 8? NO → return null
      → node 7's right = null

      return node 7 (no children)

    return node 5 (left = 1, right = 7)

  root.right = build(ceiling = +inf):        ← INHERITED, not tightened to 8
    preorder[4] = 10, 10 < +inf → fits. Consume 10, i becomes [5].
    root = 10

    root.left = build(ceiling = 10):
      preorder[5] = 12, 12 < 10? NO → return null
    → node 10's left = null

    root.right = build(ceiling = +inf):      ← INHERITED
      preorder[5] = 12, 12 < +inf → fits. Consume 12, i becomes [6].
      root = 12

      root.left = build(ceiling = 12):
        i[0] == preorder.length (6) → return null
      root.right = build(ceiling = +inf):
        i[0] == preorder.length (6) → return null

      return node 12 (no children)

    return node 10 (left = null, right = 12)

  return node 8 (left = 5, right = 10)
```

**Final tree:**

```
              8
           /     \
          5       10
         /          \
        1            12
         \
          7
```

Matches Stage 2's manually-traced tree and the transcript's own worked diagram exactly. Every one of the 6 array elements was consumed exactly once, and the shared pointer only ever moved forward.

---

## Complexity Analysis

**Time Complexity — O(n):**

This deserves the same honest derivation the transcript itself walks through, rather than a one-line assertion. At first glance it might look like some nodes get "visited more than once" — after all, node `5` is reached once when attempting a left placement, and its ceiling-check machinery runs again on the way back for the right-child attempt. But notice: **the pointer `i` only ever advances when a value is actually consumed**, and every value in the array is consumed **exactly once**, at the exact moment it successfully fits under some ceiling. The extra `build()` calls that return `null` immediately (failed ceiling checks) do **not** consume anything and do **not** recurse further — they're O(1) dead ends, not further branching.

So while it's true that a single node like `5` participates in up to two or three `build()` invocations at its position in the call tree (one that plants it, one for its left attempt, one for its right attempt), the actual **work** done — successful placements — is bounded by `n`, and every failed/null-returning call is a constant-time leaf that terminates immediately. Total calls across the whole recursion is bounded by a small constant multiple of `n` (at most `2n+1`, since every real node triggers at most two child-attempt calls), which is still **O(n)**.

**Space Complexity — O(h), where h is the height of the tree:**

The only auxiliary space is the recursion call stack — one frame per level of the tree currently being constructed, exactly like every other recursive tree-building/traversal problem in this pattern.

- **Best case (balanced BST):** `h = O(log n)`.
- **Worst case (skewed BST — e.g., preorder already sorted ascending, like `1, 2, 3, 4, 5`):** `h = O(n)`, since every node would only ever have a right child, degenerating into a straight line.

So: **O(h) auxiliary space — O(log n) balanced, O(n) worst case (skewed)**. Note this is a dramatic improvement over the brute force's `O(n)` *extra* space (the sorted array plus the hashmap), independent of the tree's shape.

---

## Comparing Brute Force vs Optimal

| Property | Brute Force (Sort + Two-Traversal Build) | Optimal (Ceiling-Only Recursion) |
| --- | --- | --- |
| Needs a second traversal array? | Yes — manufactured via sorting | No — single array, single pass |
| Extra space beyond the tree | O(n) — sorted array + hashmap | O(h) — recursion stack only |
| Time | O(n log n) | O(n) |
| Core trick | Treat it like a generic two-traversal reconstruction | Exploit BST ordering directly via a shrinking/inherited ceiling |
| Lower bound ever needed? | Implicitly, via the inorder split | Never — proven redundant by preorder's fixed left-then-right direction |

---

## The Key Takeaway

```
┌───────────────────────────────────────────────────────────────────────┐
│  A BST's preorder traversal ALONE is enough to reconstruct            │
│  the tree — no second traversal needed — because the ordering         │
│  property lets comparisons stand in for what inorder would            │
│  otherwise have to encode.                                            │
│                                                                       │
│  THE TRICK: track only an UPPER BOUND (ceiling), never a lower        │
│  one.                                                                 │
│      Moving LEFT  → ceiling TIGHTENS to the current node's            │
│                      own value                                        │
│      Moving RIGHT → ceiling is INHERITED unchanged                    │
│                                                                       │
│  WHY NO LOWER BOUND IS NEEDED: a value only ever gets ceiling-        │
│  checked while attempting a LEFT placement. Failing that check        │
│  can only ever mean "too big" — never "too small" — so the            │
│  ceiling alone already carries everything a two-sided range           │
│  would have told you, specifically because of preorder's fixed        │
│  root-left-right direction.                                           │
│                                                                       │
│  A single SHARED POINTER (not index slicing) advances through         │
│  preorder as values get consumed — every recursive call reads         │
│  from and mutates the same position.                                  │
│                                                                       │
│  Time  — O(n): every array value consumed exactly once;               │
│           failed ceiling checks are O(1) dead ends.                   │
│  Space — O(h): O(log n) balanced, O(n) worst case (skewed).           │
└───────────────────────────────────────────────────────────────────────┘
```