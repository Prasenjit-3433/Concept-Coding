# LC 107. Binary Tree Level Order Traversal II

Key Concept: Same BFS, reverse result at end
Problem: https://leetcode.com/problems/binary-tree-level-order-traversal-ii/description/
Solution: https://www.youtube.com/watch?v=by24w0MXDhQ
Status: Done

# Stage 1: Identification

### **Step 1 — Which topic?**

You are given the root of a binary tree. You need to return its level order traversal, but **bottom-up** — the deepest level comes first in the answer, and the root's level comes last.

### **Step 2 — Which pattern?**

Still **Pattern 3: Level Order Traversal**. The trigger:

> "level order traversal", but "bottom-up" / "from leaf level to root level" → **BFS, with the output order flipped**
> 

### **Step 3 — Which key concept?**

**Same BFS as LC 102 — reverse only the *order of levels* at the very end, never the order of values *within* a level.**

Nothing about *how* you discover the levels changes. BFS still has to start at the root and work its way down — there's no way to "start from the bottom" directly, since you don't even know how many levels exist, or which nodes belong to the deepest level, until you've walked down to them. So the algorithm is identical to LC 102 in every respect except one: once you have the list of levels in top-to-bottom order, you flip that list before returning it.

---

# Stage 2: Intuition Building

### Why You Can't Just "Start From the Bottom"

It's tempting to think this problem needs some fundamentally different traversal — "BFS but upside down." But think about what information you'd need to start from the bottom: you'd need to already know the tree's height, and which specific nodes sit at the deepest level. The only way to discover that is to actually walk down from the root, level by level, exactly as before. There is no shortcut that skips this — the tree's shape simply isn't known in advance.

### The Tree We're Working With

```
1 2
1 3
2 4
2 5
3 null
3 null
```

```
              1
           /     \
          2       3
         / \
        4   5
```

### The Core Question — What Actually Needs to Flip?

Running plain BFS (exactly like LC 102) on this tree gives:

```
Level 0: [1]
Level 1: [2, 3]
Level 2: [4, 5]

top-to-bottom result: [[1], [2, 3], [4, 5]]
```

The problem wants:

```
[[4, 5], [2, 3], [1]]
```

Compare the two closely: **within** each level, the order of values is completely untouched — `[2, 3]` stays `[2, 3]`, not `[3, 2]`. The only thing that changed is the **sequence of levels** — level 2 now comes first, level 0 comes last. So "bottom-up" doesn't mean "reverse everything" — it means **reverse the list of levels**, treating each level's inner list as an atomic, unchanged unit.

```
┌───────────────────────────────────────────────────────────────────────┐
│  Do NOT reverse values inside a level.                                │
│  DO reverse the ORDER in which levels appear in the final list.       │
│                                                                       │
│  [[1], [2,3], [4,5]]  →  reverse the OUTER list only  →               │
│  [[4,5], [2,3], [1]]                                                  │
└───────────────────────────────────────────────────────────────────────┘
```

### Two Equally Valid Ways to Achieve the Flip

There are two mechanically different ways to produce this, and both are worth knowing:

**Option A — build top-to-bottom, then reverse the whole outer list at the very end.** Run BFS exactly as in LC 102, collecting each level's list into `result` in the normal order. Once BFS finishes, call `Collections.reverse(result)` once, on the outer list only.

**Option B — insert each new level at the front instead of the back, as you go.** Every time a new level's list is finished, instead of `result.add(level)` (which appends to the end), insert it at index `0` (`result.add(0, level)`). This means the *first* level discovered (the root) ends up pushed to the back over time, and the *last* level discovered (the deepest) ends up sitting at the front — achieving the same final order without a separate reverse step at the end.

```
┌───────────────────────────────────────────────────────────────────────┐
│  Option A: collect normally, reverse ONCE at the end.                 │
│      → simple, one extra O(number of levels) pass                     │
│                                                                       │
│  Option B: insert each level at index 0 as you go.                    │
│      → no separate reverse step, but repeated "insert at front"       │
│        on an ArrayList costs O(k) each time (shifting elements)       │
│        — a LinkedList avoids that cost, since addFirst is O(1)        │
└───────────────────────────────────────────────────────────────────────┘
```

Both are correct; Option A is simpler to reason about and doesn't depend on choosing the right underlying list type, so that's the one we'll code as the primary approach.

### Why the Underlying BFS Mechanics Don't Change at All

Everything from the LC 102 theory note carries over unchanged: seed the queue with the root, snapshot `levelSize = queue.size()` before draining a level, pop exactly that many nodes while pushing their children for the next round, collect values into a per-level list. The only new step, tacked on right before returning, is the single reversal of the outer list.

---

# Stage 3: Coding

## Approach — BFS (Identical to LC 102) + One Final Reversal

**Mental workflow before writing a single line:**

```
1. Edge case: root == null → return empty list

2. Run the EXACT same level-order BFS as LC 102:
   → queue seeded with root
   → snapshot levelSize before draining each level
   → pop levelSize nodes, push their children, collect values
   → append each completed level's list to result (top-to-bottom order)

3. Once BFS is fully done, reverse the OUTER list (result) once —
   do NOT touch the order of values inside any individual level

4. Return result
```

```java
class Solution {
    public List<List<Integer>> levelOrderBottom(TreeNode root) {
        List<List<Integer>> result = new ArrayList<>();

        // Nothing to traverse
        if (root == null) return result;

        Queue<TreeNode> queue = new LinkedList<>();
        queue.add(root);

        // ── Identical BFS to LC 102 ──
        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            List<Integer> level = new ArrayList<>();

            for (int i = 0; i < levelSize; i++) {
                TreeNode curr = queue.poll();

                if (curr.left != null) queue.add(curr.left);
                if (curr.right != null) queue.add(curr.right);

                level.add(curr.val);
            }

            result.add(level);
        }

        // ── The ONE new step: reverse the ORDER OF LEVELS only ──
        Collections.reverse(result);

        return result;
    }
}
```

---

## Workflow Trace on the Example

```
              1
           /     \
          2       3
         / \
        4   5
```

```
BFS produces (top-to-bottom, exactly like LC 102):
result = [[1], [2, 3], [4, 5]]

Collections.reverse(result) flips only the OUTER list:
result = [[4, 5], [2, 3], [1]]
```

Notice `[2, 3]` never became `[3, 2]` — the reversal never reaches inside an individual level's list.

---

## Complexity Analysis

**Time Complexity — O(n):**

The BFS portion is identical to LC 102 — every node pushed and popped exactly once, O(1) work each, giving **O(n)**. The added `Collections.reverse(result)` call operates on the *outer* list only, whose size is the number of levels (at most `h`, the tree's height) — reversing a list of size `h` costs **O(h)**, and since `h ≤ n`, this is absorbed into the overall **O(n)** bound without changing the asymptotic class.

**Space Complexity — O(n):**

Same reasoning as LC 102: the queue can hold up to O(n) nodes at the widest level in the worst case, and the `result` structure holds every node's value exactly once across all levels — also O(n). The reversal is done in-place on `result`, so it costs no additional space beyond what was already allocated.

---

## Comparing This to LC 102

| Property | LC 102 (Top-Down) | LC 107 (Bottom-Up) |
| --- | --- | --- |
| BFS mechanics | Standard queue + level-size snapshot | Identical, unchanged |
| Order within a level | Left to right | Left to right — **unchanged** |
| Order of levels in output | Root's level first | Root's level **last** — outer list reversed |
| Extra step | None | One `Collections.reverse()` on the outer list, after BFS completes |

---

## The Key Takeaway

```
┌───────────────────────────────────────────────────────────────────────┐
│  LC 107 is NOT a different traversal — it's LC 102's BFS,             │
│  completely unchanged, with ONE extra step tacked on at the end:      │
│  reverse the OUTER list of levels.                                    │
│                                                                       │
│  You cannot discover the tree "bottom-up" directly — the only         │
│  way to find the deepest level is to walk down to it first,           │
│  which means running ordinary top-down BFS regardless.                │
│                                                                       │
│  What flips: the SEQUENCE in which levels appear in the answer.       │
│  What never flips: the order of values WITHIN a single level.         │
│                                                                       │
│  Time  — O(n): identical to LC 102, plus O(h) for the reversal,       │
│           absorbed into O(n) overall.                                 │
│  Space — O(n): same worst case as LC 102 — a wide bottom level.       │
└───────────────────────────────────────────────────────────────────────┘
```