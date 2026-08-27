# LC 102. Binary Tree Level Order Traversal

Key Concept: Pure BFS template — process nodes level by level
Problem: https://leetcode.com/problems/binary-tree-level-order-traversal/description/
Solution: https://www.youtube.com/watch?v=EoAsWbO7sqg&list=PLgUwDviBIf0q8Hkd7bK2Bpryj2xVJk8Vk&index=9&ab_channel=takeUforward
Status: Done

# Theory: Level Order Traversal (BFS on a Binary Tree)

---

# Stage 1: Identification

**Step 1 — Which topic?**

You are given the root of a binary tree. You need to return its values grouped **level by level** — all nodes at depth 0 together, then all nodes at depth 1 together, and so on.

**Step 2 — Which pattern?**

This is the founding problem of **Pattern 3: Level Order Traversal**. Every traversal so far (Pattern 1) went **deep** first — root, then fully finish left, then fully finish right. This problem needs the opposite discipline: go **wide** first — finish an entire level before touching the next one. The trigger:

> "level by level", "level order", "print level wise" → **BFS on a tree**
> 

**Step 3 — Which key concept?**

**Queue + "snapshot the level size before draining it."**

A plain queue naturally processes nodes in the order they were discovered — first in, first out. If you seed it with the root and then repeatedly "pop a node, push its children," the queue itself ends up holding **exactly one level's worth of nodes at a time**, in order. The one trick that turns "just a queue" into "level order" is: before you start popping for a level, **record how many nodes are currently in the queue** — that number is exactly how many nodes belong to the current level, no more, no less.

---

# Stage 2: Intuition Building

### Why a Queue, and Not a Stack

Think about what you need: process the root, then process *everything at depth 1* before *anything at depth 2*. A stack (LIFO) would let you dive deep immediately, which is exactly the traversal shape Pattern 1 already covers — that's wrong here. A queue (FIFO) preserves discovery order: whatever gets added first, comes out first. If you always add a node's children right after popping it, children discovered earlier (i.e., belonging to an earlier level) always get processed before children discovered later (a later level). That ordering guarantee is the entire reason BFS works.

### The Tree We're Working With

```
1 2
1 3
2 4
2 5
3 6
3 7
```

```
              1
           /     \
          2       3
         / \     / \
        4   5   6   7
```

### Walking Through the Idea (Not a Dry Run — Just the Shape)

Start by pushing the root into the queue:

```
Queue: [1]
```

Now here's the key move: **before touching the queue this round, note its current size.** Right now that's `1` — meaning exactly one node belongs to this level. Pop exactly that many nodes, and for each one popped, push its children (if they exist) onto the back of the queue, and collect its value into "this level's list."

```
Pop 1 → children 2, 3 get pushed → level list so far: [1]
Queue now: [2, 3]
```

Level 1 is done — collected `[1]`. Now look at the queue again: its size is `2`. That's exactly how many nodes belong to the next level — no more, no less, because everything pushed onto the queue during this round belongs to the *next* level, and nothing from that next level has been popped yet.

```
Pop 2 → children 4, 5 pushed → level list: [2]
Pop 3 → children 6, 7 pushed → level list: [2, 3]
Queue now: [4, 5, 6, 7]
```

Level 2 done — collected `[2, 3]`. Queue size is now `4` — snapshot that, drain exactly that many:

```
Pop 4, 5, 6, 7 → none have children → level list: [4, 5, 6, 7]
Queue now: []
```

Level 3 done — collected `[4, 5, 6, 7]`. Queue is empty — traversal complete.

**Final answer: `[[1], [2, 3], [4, 5, 6, 7]]`**

### Why "Snapshot the Size First" Is Non-Negotiable

This is the one detail that makes or breaks the whole algorithm. While you're processing the current level, you are simultaneously **pushing next level's nodes into the very same queue**. If you check `queue.size()` fresh on every loop iteration instead of snapshotting it once at the start, the size keeps growing as you push children — you'd never know where "this level" actually ends and the next one begins. Capturing `levelSize = queue.size()` **once, before the inner loop starts**, freezes the boundary — you process exactly that many pops, and nothing pushed during this round contaminates the current level's count.

```
┌────────────────────────────────────────────────────────────────────────┐
│  levelSize = queue.size()   ← frozen BEFORE any pop/push this round    │
│                                                                        │
│  for i in 0 .. levelSize-1:                                            │
│      pop a node                                                        │
│      push its children (these belong to the NEXT level)                │
│      collect its value                                                 │
│                                                                        │
│  Children pushed during this loop are invisible to THIS loop's         │
│  bound, since levelSize was already fixed before they arrived.         │
└────────────────────────────────────────────────────────────────────────┘
```

### Why This Terminates

Every node is pushed onto the queue exactly once (either as the root, or as someone's child) and popped exactly once. Once every node has been popped, the queue is empty, the outer `while` loop's condition fails, and the traversal stops — having visited every node exactly once, level by level.

---

# Stage 3: Coding

## Approach — BFS with a Queue

```java
class Solution {
    public List<List<Integer>> levelOrder(TreeNode root) {
        List<List<Integer>> result = new ArrayList<>();

        // Nothing to traverse
        if (root == null) return result;

        Queue<TreeNode> queue = new LinkedList<>();
        queue.add(root);

        while (!queue.isEmpty()) {

            // Freeze the current level's size BEFORE popping/pushing
            // anything this round — this is what separates "this
            // level" from whatever gets pushed as we go
            int levelSize = queue.size();
            List<Integer> level = new ArrayList<>();

            for (int i = 0; i < levelSize; i++) {
                TreeNode curr = queue.poll();

                // Children belong to the NEXT level — push them,
                // but they don't affect THIS round's loop bound
                if (curr.left != null) queue.add(curr.left);
                if (curr.right != null) queue.add(curr.right);

                level.add(curr.val);
            }

            result.add(level);
        }

        return result;
    }
}
```

---

## Complexity Analysis

**Time Complexity — O(n):**

Every node is pushed onto the queue exactly once and popped exactly once. Each of those operations does O(1) work (a couple of null checks, one list insertion). With `n` total nodes, each handled in O(1) time, total time is **O(n)**.

**Space Complexity — O(n):**

Two things to account for separately. The recursion-free BFS itself uses only a queue — but that queue can, in the worst case (a tree whose widest level is close to `n/2`, e.g. a complete binary tree's last level), hold up to **O(n)** nodes at once. The `result` list holding every level's values is the required output, so it's not usually counted as "auxiliary" space — but even if you did count it, it also totals **O(n)** across all levels, since every node appears in exactly one level's list.

So: **O(n) time, O(n) space** — both driven by the same worst case, a tree that's very wide at its bottom level.

---

## The Key Takeaway

```
┌───────────────────────────────────────────────────────────────────────┐
│  Level order = BFS = Queue.                                           │
│                                                                       │
│  THE ONE TRICK: snapshot levelSize = queue.size() BEFORE              │
│  popping anything this round. That number is exactly how many         │
│  nodes belong to the current level — children pushed during           │
│  this round belong to the NEXT level and don't blur the line.         │
│                                                                       │
│  Loop: pop → push children → collect value, repeated levelSize        │
│  times per level, until the queue is empty.                           │
│                                                                       │
│  Time  — O(n): every node pushed and popped exactly once.             │
│  Space — O(n): queue can hold up to ~n/2 nodes at the widest          │
│           level (e.g. a complete binary tree's last level).           │
└───────────────────────────────────────────────────────────────────────┘
```

---