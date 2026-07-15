# Iterative Postorder Traversal using 2 Stack & 1 Stack

Solution: https://www.youtube.com/watch?v=2YBhNLodD8Q&list=PLgUwDviBIf0q8Hkd7bK2Bpryj2xVJk8Vk&index=12&ab_channel=takeUforward
Status: Done

# Iterative Postorder Traversal using 2 Stacks

---

# Stage 1: Identification

**Step 1 — Which topic?**

You are given the root of a binary tree. You need to return the **postorder traversal** — left, right, root — without recursion, using stacks instead.

**Step 2 — Which pattern?**

Still **Pattern 1: Tree Traversals (Core Templates)** — same order as LC 145, iterative mechanism using **two stacks** this time.

**Step 3 — Which key concept?**

**Deliberately build the REVERSE of postorder using one stack, then flip it using a second stack.**

Postorder is left → right → root. That's awkward to produce directly with a simple push/pop loop, because the root must come out *last*, after both children — the opposite of how preorder naturally falls out of a stack. So instead of fighting that, the trick is: produce a sequence that's easy to generate with a stack, and that sequence just happens to be the **exact reverse** of postorder. Once you have that reversed sequence, pop it into a second stack to flip it back into the correct order.

---

# Stage 2: Intuition Building

### What Sequence Is Easy to Produce, and Why It's the Reverse of Postorder

Postorder is: **left, right, root**.

The reverse of that order is: **root, right, left**.

Notice something: "root, right, left" looks almost exactly like preorder ("root, left, right") — except left and right are swapped. And preorder is easy to do iteratively with a single stack (pop, visit, push children). So if you take that exact same iterative preorder mechanism, but push **left before right** instead of right before left, you get root-right-left out directly — which is precisely postorder, reversed.

### Building the First Stack (Stack One → Stack Two)

Start with the root pushed into stack one. Then repeat:

```
Pop the top of stack one.
Push that popped node into stack two (this is where the answer gets collected, in reverse order).
Then push its LEFT child onto stack one (if it exists).
Then push its RIGHT child onto stack one (if it exists).
```

Because left is pushed after right here, left sits higher up in stack one and gets popped sooner on a later iteration — so the popping order out of stack one comes out as root, right, left, exactly the reverse of what postorder wants.

### Why a Second Stack Fixes the Order

Every node, as it's popped off stack one, gets pushed onto stack two rather than being printed immediately. Stack two is *also* a Last-In-First-Out structure — so whatever went in last (which was in root-right-left order) comes out **reversed**, giving left-right-root: exactly postorder.

Think of stack two purely as a "reversing tool" — pushing an entire sequence into a stack and then popping it all back out flips that sequence end to end. Since the sequence going in was the reverse of postorder, the sequence coming out is postorder itself.

### Walking Through the Shape (Not a Dry Run — Just the Idea)

```
Stack one starts with just the root.

Repeat until stack one is empty:
    Pop from stack one → call it "current"
    Push "current" onto stack two
    If current has a left child  → push it onto stack one
    If current has a right child → push it onto stack one

Once stack one is empty, stack two holds every node,
but in "root, right, left" order from top to bottom.

Now pop everything from stack two, one at a time,
and collect it — this reversal step naturally produces
"left, right, root" order: the actual postorder traversal.
```

### Why This Works for Every Subtree, Not Just the Top Level

The same push order (left after right, so left is popped from stack one first on the next pass) is applied uniformly at every node — not just the root. So this "root, right, left" ordering property holds recursively for every subtree as well, which is exactly what guarantees the final reversal gives correct postorder for the whole tree, not just the top few nodes.

### Why This Terminates

Every node is pushed into stack one exactly once (either as the initial root or as someone's left/right child) and popped from it exactly once. Each of those pops also causes exactly one push into stack two. So stack one empties after exactly `n` pops, and stack two ends up holding exactly `n` nodes, which are then popped out one by one to build the final answer.

---

# Stage 3: Coding

## Approach — Iterative with Two Stacks

```java
class Solution {
    public List<Integer> postorderTraversal(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        if (root == null) return result;

        Stack<TreeNode> stack1 = new Stack<>();
        Stack<TreeNode> stack2 = new Stack<>();

        stack1.push(root);

        // Phase 1: build "root, right, left" order into stack2
        while (!stack1.isEmpty()) {
            TreeNode node = stack1.pop();

            // Move this node into stack2 — this is where the
            // answer accumulates, but in REVERSED postorder
            stack2.push(node);

            // Push LEFT before RIGHT this time — the opposite of
            // standard iterative preorder — so that when stack1
            // is popped again, LEFT comes out before RIGHT,
            // which lands LEFT deeper in stack2 than RIGHT
            if (node.left != null) {
                stack1.push(node.left);
            }
            if (node.right != null) {
                stack1.push(node.right);
            }
        }

        // Phase 2: pop everything out of stack2 — this reverses
        // "root, right, left" into "left, right, root" — postorder
        while (!stack2.isEmpty()) {
            result.add(stack2.pop().val);
        }

        return result;
    }
}
```

---

## Complexity Analysis

**Time Complexity — O(n):**

Every node is pushed into stack one exactly once, popped from it exactly once, and consequently pushed into stack two exactly once. Then every node is popped from stack two exactly once. Each of these operations is O(1), and there are `n` nodes total, so total work is **O(n)** — every node is handled a constant number of times.

**Space Complexity — O(n):**

Two stacks are used, and between them, every one of the `n` nodes is held at some point — in the worst case, both stacks together can hold close to `n` nodes simultaneously (for example, right before stack one finishes emptying and stack two has already collected most of the nodes). So the auxiliary space is **O(n)**, using two stacks instead of one. As pointed out directly: the output list itself is generally not counted toward auxiliary space, but the real distinguishing cost of this approach compared to other traversals is that it needs **two** stack structures instead of one.

---

## Comparing the Three Iterative Traversals So Far

| Property | Preorder | Inorder | Postorder (2-stack) |
| --- | --- | --- | --- |
| Order produced | root, left, right | left, root, right | left, right, root |
| Stacks needed | 1 | 1 | **2** |
| Core trick | pop → visit → push right, push left | push while going left, pop → visit → go right | pop → collect (stack2) → push left, push right → then reverse stack2 |
| Direct or needs reversal? | Direct | Direct | **Needs a reversal step** |

---

## The Key Takeaway

```
┌─────────────────────────────────────────────────────────────┐
│  Postorder (left, right, root) is awkward to build directly      │
│  with a simple stack loop — root must come out LAST.             │
│                                                                  │
│  The trick: build the REVERSE of postorder instead, which is     │
│  "root, right, left" — and that is just preorder with left       │
│  and right swapped, easy to produce with one stack.              │
│                                                                  │
│  Then dump that entire reversed sequence into a SECOND stack.    │
│  Popping a stack always reverses whatever was pushed into it —   │
│  so popping stack two turns "root, right, left" back into        │
│  "left, right, root" — the actual postorder traversal.           │
│                                                                  │
│  Stack1: pop → push to stack2 → push LEFT, then push RIGHT       │
│  Stack2: pop everything out at the end → this is the answer      │
│                                                                  │
│  Time  — O(n): every node handled a constant number of times.    │
│  Space — O(n): TWO stacks used, unlike the single stack that     │
│           sufficed for preorder and inorder.                     │
└─────────────────────────────────────────────────────────────┘
```

A note on the title: the transcript for this lecture only walked through the **two-stack** method in full detail — it mentions the single-stack version is coming in a separate, later video, but doesn't actually explain it here. So I've only written up the two-stack approach faithfully to what was taught. Send that follow-up transcript whenever you have it and I'll add the single-stack version as its own note.