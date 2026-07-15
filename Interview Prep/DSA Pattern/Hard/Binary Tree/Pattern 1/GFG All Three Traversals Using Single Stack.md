# GFG: All Three Traversals Using Single Stack

Key Concept: Advanced — one unified iterative approach for all three
Solution: https://www.youtube.com/watch?v=ySp2epYvgTE&list=PLgUwDviBIf0q8Hkd7bK2Bpryj2xVJk8Vk&index=14&ab_channel=takeUforward
Status: Done

# GFG: All Three Traversals Using Single Stack

---

# Stage 1: Identification

**Step 1 — Which topic?**

Given the root of a binary tree, produce **preorder, inorder, and postorder traversals all in a single pass**, using only **one stack** — no recursion, and no separate traversal for each order.

**Step 2 — Which pattern?**

Still **Pattern 1: Tree Traversals (Core Templates)** — the advanced, unified iterative technique that closes out this pattern.

**Step 3 — Which key concept?**

**Track how many times each node has been "visited" using a counter attached to the node on the stack.**

Every node is pushed and popped from the stack **up to three separate times**, and a small counter (1, 2, or 3) travels along with the node to record which visit this currently is:

```
1st time popped → this is the PREORDER moment (root comes first)
2nd time popped → this is the INORDER moment  (root comes in the middle)
3rd time popped → this is the POSTORDER moment (root comes last)
```

Instead of three different stack routines, one unified loop handles all three orders simultaneously by reading this counter.

*(Note: the transcript provided for this lecture was not usable — it came through as heavily garbled auto-generated Hindi captions that don't form coherent sentences. So this note is built directly from Striver's actual Java code you provided, reasoning out the logic behind it, rather than reconstructed from his spoken explanation.)*

---

# Stage 2: Intuition Building

### Why a Node Needs to Be Visited Three Times

Think back to the recursive version of all three traversals:

```
solve(node):
    print(node)         ← preorder moment: node is touched for the FIRST time
    solve(node.left)
    print(node)         ← inorder moment: node is touched again, AFTER left is done
    solve(node.right)
    print(node)         ← postorder moment: node is touched a THIRD time, AFTER right is done
```

A single node conceptually gets "control" back three separate times during its own recursive lifetime — once before exploring left, once between left and right, and once after both are fully explored. Each of preorder, inorder, and postorder is really just "print the node at one specific one of these three moments." So if a structure can track *which* of these three moments a node is currently at, it can simultaneously produce all three traversals in one sweep.

### The `Pair(node, num)` — Attaching a Visit-Counter to Each Node

Instead of pushing bare `TreeNode`s onto the stack, a small wrapper `Pair` is pushed instead — holding the node itself, and a number `num` that says: *"this is the `num`-th time this node has come up."*

```
num == 1  →  first visit  →  this is the moment for PREORDER
num == 2  →  second visit →  this is the moment for INORDER
num == 3  →  third visit  →  this is the moment for POSTORDER
```

Every node starts life on the stack with `num = 1`.

### The Core Loop — What Happens on Each Visit

```
Pop the top Pair (node, num) off the stack.

If num == 1:
    → PREORDER moment. Record node's value into the preorder list.
    → Increment num to 2, and push this SAME node back onto the stack
      (it still owes us its inorder and postorder moments later).
    → Then push the node's LEFT child (fresh, with num = 1) —
      because preorder must explore left immediately next.

Else if num == 2:
    → INORDER moment. Record node's value into the inorder list.
    → Increment num to 3, and push this SAME node back onto the stack
      (it still owes us its postorder moment).
    → Then push the node's RIGHT child (fresh, with num = 1) —
      because after the inorder moment, right comes next.

Else (num == 3):
    → POSTORDER moment. Record node's value into the postorder list.
    → This node has now given all three of its values. Do NOT push it
      back — its job is completely done.
```

### Why Pushing the Node Back Onto Itself Works

When a node is popped with `num = 1`, it isn't done with the stack yet — it will need to come back for its `num = 2` and `num = 3` moments later, but only **after** its subtrees have contributed what they need to first (left subtree entirely for `num=2`, then right subtree entirely for `num=3`). Pushing the node back onto the stack (with its counter incremented) before pushing its child, and letting the stack's Last-In-First-Out order naturally handle "explore the child fully, then come back to me," is what recreates the recursive call structure without actual recursion.

Because the child is pushed **after** the node is pushed back, the child sits higher on the stack — so it gets popped and fully processed (potentially generating its own chain of pushes and pops) before control ever returns to the node waiting underneath it. That's exactly the same relationship a recursive call has with its parent: the parent doesn't resume until the child call has completely finished.

### Why This Naturally Produces All Three Orders Correctly

- **Preorder** collects a node's value the instant it's first popped (`num == 1`) — before either subtree has been touched. That's root, then whatever comes from exploring left. Exactly root → left → right.
- **Inorder** collects a node's value on its second pop (`num == 2`) — which only happens after its entire left subtree has already been fully popped and processed (since left was pushed on top and had to be exhausted first). That's left → root → right.
- **Postorder** collects a node's value on its third and final pop (`num == 3`) — which only happens after both its left and right subtrees have been fully processed. That's left → right → root.

All three lists are being built up simultaneously, in one single pass over one single stack — each list simply picks out a node's value at a different one of its three visits.

### Why Each Node Is Pushed and Popped At Most 3 Times

Every node goes through this exact life cycle on the stack:

```
Pushed with num=1 → popped (num=1) → pushed back with num=2
                  → popped (num=2) → pushed back with num=3
                  → popped (num=3) → discarded (not pushed again)
```

That's a fixed, bounded number of pushes and pops per node — three of each, no more, regardless of the shape of the tree.

---

# Stage 3: Coding

## Approach — Iterative with a Single Stack, Using a Visit-Counter

```java
public class Solution {

    // A small wrapper that pairs a node with "how many times has this
    // node been visited so far" — this counter is what lets one stack
    // produce all three traversal orders at once
    static class Pair {
        TreeNode node;
        int num;

        public Pair(TreeNode node, int num) {
            this.node = node;
            this.num = num;
        }
    }

    public static List<List<Integer>> getTreeTraversal(TreeNode root) {
        List<Integer> preorder = new ArrayList<>();
        List<Integer> inorder = new ArrayList<>();
        List<Integer> postorder = new ArrayList<>();

        if (root == null) return Arrays.asList(preorder, inorder, postorder);

        Stack<Pair> st = new Stack<>();
        // Every node starts its life on the stack with num = 1 —
        // meaning "this is its first visit"
        st.push(new Pair(root, 1));

        while (!st.isEmpty()) {
            Pair el = st.pop();

            if (el.num == 1) {
                // FIRST visit — this is the PREORDER moment
                preorder.add(el.node.data);

                // This node isn't done yet — it owes an inorder
                // and postorder visit later. Push it back with
                // its counter bumped to 2.
                el.num++;
                st.push(el);

                // Preorder demands left comes next — push left
                // (fresh, starting its own life at num = 1)
                if (el.node.left != null) {
                    st.push(new Pair(el.node.left, 1));
                }

            } else if (el.num == 2) {
                // SECOND visit — this only happens after the ENTIRE
                // left subtree has been fully popped and processed,
                // since left was pushed on top and had to clear first.
                // This is the INORDER moment.
                inorder.add(el.node.data);

                // Still owes a postorder visit — push back with num = 3
                el.num++;
                st.push(el);

                // After the inorder moment, right comes next
                if (el.node.right != null) {
                    st.push(new Pair(el.node.right, 1));
                }

            } else {
                // THIRD visit (num == 3) — this only happens after
                // BOTH left and right subtrees are fully processed.
                // This is the POSTORDER moment — final visit, don't push back.
                postorder.add(el.node.data);
            }
        }

        return Arrays.asList(preorder, inorder, postorder);
    }
}
```

---

## Complexity Analysis

**Time Complexity — O(n):**

Each node is pushed onto the stack and popped from it **at most three times** — once for each of its three visit states. Since three is a constant (independent of `n`), the total number of push/pop operations across the whole tree is `3n` in the worst case, which is still **O(n)**.

**Space Complexity — O(n) in the worst case:**

The stack can, at various points, be holding a mix of nodes at different stages (some waiting at `num=2`, some fresh children at `num=1`, and so on) along a path down the tree. In the worst case this bounds similarly to the other iterative traversals — proportional to the number of nodes actively "in progress" at once, which can reach **O(n)** for certain tree shapes (this matches the same reasoning used for the other iterative stack-based traversals: the stack isn't strictly bounded to just the tree's height the way the pure recursive call stack is, since multiple partially-completed nodes can coexist on it).

---

## Comparing This to the Individual Iterative Traversals

| Property | Individual iterative traversals (3 separate solutions) | This unified single-stack approach |
| --- | --- | --- |
| Number of passes over the tree | One pass per traversal (3 total) | **One single pass produces all three** |
| Stacks needed | 1 (preorder/inorder) or 2 (postorder) | **1 stack for all three orders combined** |
| Mechanism | Push/pop order directly encodes one specific order | **A per-node visit-counter picks out the right moment for each order** |
| Pushes/pops per node | Exactly 1 push, 1 pop | **Up to 3 pushes, 3 pops** |

---

## The Key Takeaway

```
┌─────────────────────────────────────────────────────────────┐
│  A node's full recursive lifetime touches it exactly 3 times:    │
│      1st time  → before exploring left   → PREORDER moment       │
│      2nd time  → between left and right  → INORDER moment        │
│      3rd time  → after exploring right   → POSTORDER moment      │
│                                                                  │
│  Attach a counter (num = 1, 2, or 3) to each node on the stack   │
│  to track which of these three moments it's currently at.        │
│                                                                  │
│  num==1: record preorder, bump to 2, push self back, push left   │
│  num==2: record inorder,  bump to 3, push self back, push right  │
│  num==3: record postorder, done — do not push again              │
│                                                                  │
│  Pushing the node back onto the stack BEFORE pushing its child   │
│  is what recreates "the child fully finishes before control      │
│  returns to the parent" — the same relationship recursion gives  │
│  for free, but made explicit here with one shared stack.         │
│                                                                  │
│  Time  — O(n): each node pushed/popped at most 3 times, a        │
│           constant factor independent of n.                      │
│  Space — O(n) worst case for the stack.                          │
└─────────────────────────────────────────────────────────────┘
```

This completes **Pattern 1: Tree Traversals** — all three recursive versions, all three single-stack iterative versions (except postorder's single-stack variant, which the earlier transcript deferred to "a future video" — let me know if you find that one), and this unified all-three-at-once technique. Ready for **Pattern 2: Tree Properties**, starting with **LC 104: Maximum Depth of Binary Tree**, whenever you are.