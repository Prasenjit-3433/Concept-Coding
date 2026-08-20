# LC 272. Closest Binary Search Tree Value II

Key Concept: Inorder + sliding window of k closest values (need 2 BST Iterator’s)
Solution: https://www.youtube.com/watch?v=bnBHf7vVpzM
Status: Pending

# Stage 1: Identification

**Step 1 — Which topic?**

You're given the root of a **Binary Search Tree**, a floating-point `target`, and an integer `k`. You need to return the **`k` values** in the BST that are closest to `target`, as a list (order doesn't matter, per the problem statement, though we'll get them out in sorted order for free).

**Step 2 — Which pattern?**

Still **Pattern 3: BST Navigation**. The trigger:

> "closest binary search tree value **II**", "k closest values" → **BST Navigation** — direct generalization of LC 270
> 

**Step 3 — Which key concept?**

**`Inorder + Sliding Window of k Closest Values.`**

LC 270 asked for *the single* closest value, and a directional compass walk was enough. The moment you need the **k closest** values, a single directional walk can no longer work — the answer isn't confined to one root-to-leaf path anymore; it can be spread across a contiguous *block* of values straddling `target` on both sides. The key realization: since **inorder traversal of a BST produces a sorted sequence**, the `k` closest values to `target` must form some **contiguous window** within that sorted sequence — so this reduces to sliding a window of size `k` across a sorted array to find the best-positioned window.

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

`target = 3.714286`, `k = 2`

Inorder traversal: `1, 2, 3, 4, 5` — **sorted**, as always for a BST.

### The Core Question — Where Do the `k` Closest Values Live in a Sorted Sequence?

Forget the tree for a moment. If you had the plain sorted array `[1, 2, 3, 4, 5]` and needed the 2 values closest to `3.71`, how would you find them?

Look at the array:

- `3` is `0.71` away
- `4` is `0.29` away
- `2` is `1.71` away
- `5` is `1.29` away
- `1` is `2.71` away

The two smallest differences belong to `4` and `3` — and notice something important: **these two values are *adjacent* in the sorted array.**

This isn't a coincidence. Ask yourself:

> *"Could the k closest values to a single target point ever be scattered, **non-contiguous**, in a **sorted** array?"*
> 

No. Here's why: suppose two of your `k` closest values are `a` and `b` (with `a` appearing before `b` in sorted order), but some third value `c` sits **between** them in the sorted array (`a < c < b`) and is *not* among your chosen `k`. Since `c` is between `a` and `b` in value, `c` is closer to `target` than at least one of `a` or `b` is — whichever one is on the far side of `c` relative to `target`. So `c` should have been chosen instead of that farther one. Contradiction. **The k closest values to any single target must always form a contiguous block in sorted order.**

```
┌──────────────────────────────────────────────────────────────────────┐
│  In a SORTED sequence, the k values closest to a single target       │
│  ALWAYS form a **CONTIGUOUS window** — never scattered.              │
│                                                                      │
│  This is exactly analogous to LC 530's "minimum difference is        │
│  always between adjacent elements" proof, extended from a            │
│  window of size 1 (just two adjacent values) to a window of          │
│  size k.                                                             │
└──────────────────────────────────────────────────────────────────────┘
```

### Turning This Into an Algorithm — Two-Pointer Sliding Window

Since the `k` closest values form a contiguous window in the sorted (inorder) sequence, the problem becomes: **find the position of that window.**

Build the inorder list first — `[1, 2, 3, 4, 5]`. Now think of a window of exactly `k` elements. Start it at the very left (`[1, 2]`), and ask:

> *"Would sliding this window one step to the right make it a better fit for `target`?"*
> 

The window `[1, 2]` has left edge `1` and (after sliding) a new right edge that would be added. The comparison that decides whether to slide: compare the value **about to leave** the window on the left (the current leftmost element) against the value **about to enter** on the right (the element just past the current rightmost). If the entering value is *closer* to `target` than the leaving value, sliding right makes the window strictly better — do it. Otherwise, stop; the current window is already optimal.

```
┌──────────────────────────────────────────────────────────────────────┐
│  Window slides right when:                                           │
│      |**entering value** - target| < |**leaving value** - target|    │
│                                                                      │
│  This greedy slide is safe because of the SAME contiguity            │
│  argument above — once sliding stops improving things, it            │
│  never starts improving again (the array is sorted, so               │
│  distances to target decrease then increase monotonically            │
│  as you move away from target's true position).                      │
└──────────────────────────────────────────────────────────────────────┘
```

### Walking Through the Example

```
inorder = [1, 2, 3, 4, 5], target = 3.714286, k = 2
```

Start with a window of size `k=2` at the very left: `left = 0, right = 1` → window = `[1, 2]`.

```
Check: should we slide right?
  Leaving value (arr[left] = arr[0] = 1):    |1 - 3.71| = 2.71
  Entering value (arr[right+1] = arr[2] = 3): |3 - 3.71| = 0.71

  0.71 < 2.71 → entering is CLOSER → SLIDE. left=1, right=2. Window = [2, 3]
```

```
Check again:
  Leaving value (arr[left]=arr[1]=2):        |2 - 3.71| = 1.71
  Entering value (arr[right+1]=arr[3]=4):     |4 - 3.71| = 0.29

  0.29 < 1.71 → entering is CLOSER → SLIDE. left=2, right=3. Window = [3, 4]
```

```
Check again:
  Leaving value (arr[left]=arr[2]=3):        |3 - 3.71| = 0.71
  Entering value (arr[right+1]=arr[4]=5):     |5 - 3.71| = 1.29

  1.29 < 0.71? NO → entering is FARTHER → STOP sliding
```

**Final window: `[3, 4]`** — matches the manual distance check from Stage 2's opening exactly (`3` at `0.71`, `4` at `0.29`, both beating `2` and `5`).

### Why You Don't Need to Track Absolute Distances for the Whole Window — Just the Two Boundary Comparisons

A subtlety worth calling out: you never need to compute "the sum of distances for this whole window" or anything like that. Because the window always has exactly `k` elements and slides one step at a time, the *only* thing that changes between one window and the next is: *one element leaves from the left*, *one element enters from the right*. So the *entire* decision of "is the new window better" collapses to comparing just those two single values — the one leaving versus the one entering. Everything else in the window is shared between both windows and cancels out of the comparison entirely.

### Why This Terminates and Is Correct

The window starts at the leftmost `k` elements and can only ever move rightward, one step at a time, stopping the moment sliding would make things worse. Because of the contiguous-window proof above, once sliding stops improving the window, continuing to slide could never improve it again — so a single left-to-right sweep, stopping at the first non-improving slide, is guaranteed to land on the globally best window. No backtracking needed.

---

# Stage 3: Coding

### Brute Force — Collect All, Sort by Distance, Take Top K

**The honest thinking:**

> "Traverse the tree, collect every value. Sort all values by their absolute difference from target. Take the first k."
> 

```java
class Solution {
    public List<Integer> closestKValues(TreeNode root, double target, int k) {
        List<Integer> values = new ArrayList<>();
        collect(root, values);

        // Sort by distance to target
        values.sort((a, b) -> Double.compare(Math.abs(a - target), Math.abs(b - target)));

        return values.subList(0, k);
    }

    private void collect(TreeNode node, List<Integer> values) {
        if (node == null) return;
        values.add(node.val);
        collect(node.left, values);
        collect(node.right, values);
    }
}
```

Correct, but pays `O(n log n)` for a sort that completely ignores the BST's built-in sorted structure — and doesn't even need inorder specifically, since it re-sorts by distance anyway. Establishes correctness only.

---

### Better — Inorder Collect (Free Sort), Then Two-Pointer Sliding Window

**Mental workflow before writing a single line:**

```
1. Do an inorder traversal → values come out ALREADY sorted, no
   sort step needed at all (unlike brute force)

2. Initialize a window: left = 0, right = k - 1
   (the first k elements, at the very start of the sorted array)

3. While right + 1 < values.size():
   → compare |values[left] - target| (leaving candidate)
     against |values[right + 1] - target| (entering candidate)
   → if entering is closer (or equal): slide the window right
     (left++, right++)
   → else: stop, current window is optimal

4. Return values[left..right] (inclusive) as the answer
```

```java
class Solution {
    public List<Integer> closestKValues(TreeNode root, double target, int k) {
        List<Integer> values = new ArrayList<>();
        inorder(root, values);
        // values is now SORTED — free, thanks to the BST property

        int left = 0;
        int right = k - 1;

        // Slide the window right as long as doing so strictly
        // improves the fit — same greedy shift as a classic
        // "smallest window" two-pointer sweep
        while (right + 1 < values.size()) {
            double leavingDiff = Math.abs(values.get(left) - target);
            double enteringDiff = Math.abs(values.get(right + 1) - target);

            if (enteringDiff < leavingDiff) {
                left++;
                right++;
            } else {
                break; // sliding further can only **get worse** from here
            }
        }

        return values.subList(left, right + 1);
    }

    private void inorder(TreeNode node, List<Integer> values) {
        if (node == null) return;
        inorder(node.left, values);
        values.add(node.val);
        inorder(node.right, values);
    }
}
```

This drops the sort entirely — `O(n)` instead of `O(n log n)` — but still costs `O(n)` **space** to materialize the full inorder list, even when `k` is tiny compared to `n`.

---

### Optimal — Two-Stack Predecessor/Successor Merge (No Full List Needed)

<aside>
💬

`LC 173. **Binary Search Tree Iterator (BST Iterator) (Pattern 5)`** is a must prerequisite for this approach

</aside>

**The thinking:**

> "I don't actually need every value in the tree — I only need to walk **outward** from target's position, one step closer on each side (left + right) at a time, merging like the merge step of merge sort, and stop after k values."
> 

This reuses the exact iterator machinery from **Pattern 5 (BST Iterator)**: one stack simulates an inorder walk moving **forward** from just below `target` (predecessors), and a second stack simulates an inorder walk moving **backward** from just above `target` (successors). At each step, compare the next candidate from each side and take whichever is closer — exactly a two-pointer merge, just with the "arrays" being lazily generated by stacks instead of materialized upfront.

```java
class Solution {
    public List<Integer> closestKValues(TreeNode root, double target, int k) {
        Deque<TreeNode> predStack = new ArrayDeque<>(); // values <= target, descending
        Deque<TreeNode> succStack = new ArrayDeque<>(); // values > target, ascending

        // Build predecessor stack: walk toward target, pushing
        // nodes whose value could still be a predecessor
        TreeNode curr = root;
        while (curr != null) {
            if (curr.val <= target) {
                predStack.push(curr);
                curr = curr.right;
            } else {
                curr = curr.left;
            }
        }

        // Build successor stack: mirror image
        curr = root;
        while (curr != null) {
            if (curr.val > target) {
                succStack.push(curr);
                curr = curr.left;
            } else {
                curr = curr.right;
            }
        }

        List<Integer> result = new ArrayList<>();

        for (int i = 0; i < k; i++) {
            if (succStack.isEmpty() ||
                (!predStack.isEmpty() &&
                 target - predStack.peek().val < succStack.peek().val - target)) {
                // predecessor side is closer (or successor side exhausted)
                result.add(predStack.peek().val);
                advancePredecessor(predStack);
            } else {
                result.add(succStack.peek().val);
                advanceSuccessor(succStack);
            }
        }

        return result;
    }

    // Advances the predecessor stack to the NEXT smaller value
    // (mirrors the classic BST iterator "advance" step)
    private void advancePredecessor(Deque<TreeNode> stack) {
        TreeNode node = stack.pop();
        node = node.left;
        while (node != null) {
            stack.push(node);
            node = node.right;
        }
    }

    // Advances the successor stack to the NEXT bigger value
    private void advanceSuccessor(Deque<TreeNode> stack) {
        TreeNode node = stack.pop();
        node = node.right;
        while (node != null) {
            stack.push(node);
            node = node.left;
        }
    }
}
```

This is the genuinely optimal approach — it never touches nodes far from `target`, and never materializes the full inorder sequence. It builds directly on the BST Iterator pattern (Pattern 5) — worth recognizing that connection, since it's exactly the "two iterators, one forward, one backward" idea used in LC 653 (Two Sum IV).

---

## Workflow Trace on the Example (Better Approach — Sliding Window)

```
values = [1, 2, 3, 4, 5], target = 3.714286, k = 2

Initial window: left=0, right=1 → [1, 2]

Check: leaving=|1-3.71|=2.71, entering=|values[2]-3.71|=|3-3.71|=0.71
  0.71 < 2.71 → SLIDE. left=1, right=2 → [2, 3]

Check: leaving=|2-3.71|=1.71, entering=|values[3]-3.71|=|4-3.71|=0.29
  0.29 < 1.71 → SLIDE. left=2, right=3 → [3, 4]

Check: leaving=|3-3.71|=0.71, entering=|values[4]-3.71|=|5-3.71|=1.29
  1.29 < 0.71? NO → STOP

Final window: values[2..3] = [3, 4]
```

**Answer: `[3, 4]`** — matches Stage 2's trace exactly.

---

## Complexity Analysis

**Approach — Inorder + Sliding Window (Better):**

- **Time — O(n):** the inorder traversal visits every node once (`O(n)`), and the sliding window scan is a single linear pass over the resulting list (`O(n)` worst case). Total: **O(n)**.
- **Space — O(n):** the materialized inorder list holds all `n` values, regardless of how small `k` is.

**Approach — Two-Stack Predecessor/Successor Merge (Optimal):**

- **Time — O(h + k):** building both initial stacks costs `O(h)` each (walking down the tree once), and each of the `k` output values costs an amortized `O(1)` (or `O(h)` worst case per single step, same amortized argument as the BST Iterator's `next()` calls across a full traversal) to advance. Total: **O(h + k)**.
- **Space — O(h):** each stack holds at most `O(h)` nodes at any point — no full list of tree values is ever materialized.

---

## Comparing All Three Approaches

| Approach | Time | Space | Uses BST property? |
| --- | --- | --- | --- |
| Brute Force (collect + sort by distance) | O(n log n) | O(n) | No |
| Better (inorder + sliding window) | O(n) | O(n) | Partially — sorted order, not locality |
| Optimal (two-stack merge) | O(h + k) | O(h) | Fully — never visits nodes far from target |

---

## The Key Takeaway

```
┌───────────────────────────────────────────────────────────────────────┐
│  The k values closest to a single target ALWAYS form a                │
│  CONTIGUOUS window in sorted order — never scattered. This is         │
│  the direct k-sized generalization of LC 530's "closest pair          │
│  is always adjacent" proof.                                           │
│                                                                       │
│  BETTER: inorder traversal gives the sorted sequence for free.        │
│  Slide a size-k window right, one step at a time, comparing           │
│  only the single leaving value against the single entering            │
│  value — everything else in the window cancels out of the             │
│  comparison. Stop the instant sliding stops improving.                │
│                                                                       │
│  OPTIMAL: don't materialize the whole sorted list at all — walk       │
│  outward from target's position using two BST-iterator-style          │
│  stacks (Pattern 5), one for predecessors, one for successors,        │
│  merging like the merge step of merge sort. Never touches a           │
│  node far from target.                                                │
│                                                                       │
│  Time  — O(n) for the sliding-window version;                         │
│           O(h + k) for the two-stack optimal version.                 │
│  Space — O(n) vs O(h) respectively.                                   │
└───────────────────────────────────────────────────────────────────────┘
```