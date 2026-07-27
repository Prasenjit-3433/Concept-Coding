# Disjoint Set (Union-Find) — Part 3: Path Compression

---

## Picking Up Exactly Where Part 2 Left Off

The last trace exposed the issue: computing `findParent(7)` required climbing `7 → 6 → 4` — two hops. Union by Rank keeps this bounded at `O(log N)` in the worst case, which is already excellent, but we were promised something even stronger: **effectively constant time**. Path Compression is the optimization that gets us there.

---

## Building the Intuition — An Imaginary Deep Chain

Before touching our real graph again, let's build the intuition with a scenario Striver deliberately exaggerates to make the point obvious. Imagine — purely hypothetically, this is **not** part of our running example — that some sequence of unions produced this long, unbalanced chain:

```
1 2
2 3
3 4
4 5
```

Visualized:
```
1
└── 2
    └── 3
        └── 4
            └── 5
```

Someone asks: **`findParent(5)`**.

Following the plain recursive definition from Part 2, you'd climb:
```
5 → 4 → 3 → 2 → 1
```
Five nodes touched, just to discover that `1` is the boss. Every single one of those nodes — 2, 3, 4, 5 — will *always* have the same answer every time anyone asks for their ultimate parent: it's always going to be `1`, no matter how many times you ask. So why keep re-climbing this same chain over and over?

### The Core Idea

> *"While I'm already climbing up to find the boss, why not permanently rewire every node I pass through to point directly at the boss — so nobody ever has to make this climb again?"*

That's path compression, in one sentence. It doesn't change *what* the answer is — `1` was always going to be the ultimate parent of `5`. It changes *how expensive it is to ask next time*.

### What Happens Structurally

`findParent(5)` climbs `5 → 4 → 3 → 2 → 1`, hits `1` (which is its own parent — the base case), and returns `1`. Now, as the recursion **unwinds back down** that same path, every node it touched gets its `parent[]` entry rewritten directly to `1`:

```
findParent(5) returns 1
    → parent[4] = 1   (4 no longer points to 3 — straight to the boss)
findParent(4) returns 1
    → parent[3] = 1
findParent(3) returns 1
    → parent[2] = 1
findParent(2) returns 1
    → (2's own compression happens one level up in the original chain, 
       but here 2 already pointed at 1, so nothing changes)
```

The chain, after this single `findParent(5)` call, transforms from:
```
1 ← 2 ← 3 ← 4 ← 5     (a long chain)
```
into:
```
1 ← 2
1 ← 3
1 ← 4
1 ← 5     (everyone points DIRECTLY at 1)
```

Every node that was on the path is now a **direct child of the root**. The next time anyone calls `findParent` on 2, 3, 4, or 5, it resolves in a **single step** — no climbing required at all.

```
┌──────────────────────────────────────────────────────────────────┐
│  PATH COMPRESSION — the one-line idea                            │
│                                                                  │
│  Every node visited while searching for the ultimate parent      │
│  gets its parent pointer permanently rewritten to point          │
│  directly at that ultimate parent — flattening the tree as       │
│  a side effect of simply answering the query.                    │
└──────────────────────────────────────────────────────────────────┘
```

---

## The Recursive Implementation

This is the exact mechanism that makes the rewiring happen automatically, just by how the recursion is written:

```
findParent(node):
    if node == parent[node]:
        return node                              ← base case: found the boss

    parent[node] = findParent(parent[node])       ← THE KEY LINE
    return parent[node]
```

Walk through *why* this single line does all the work. Call `findParent(5)` on our imaginary chain:

```
findParent(5) calls findParent(parent[5]) = findParent(4)
  findParent(4) calls findParent(parent[4]) = findParent(3)
    findParent(3) calls findParent(parent[3]) = findParent(2)
      findParent(2) calls findParent(parent[2]) = findParent(1)
        findParent(1): 1 == parent[1] → BASE CASE → return 1

      ← back in findParent(2): parent[2] = 1 (the returned value) → return 1
    ← back in findParent(3): parent[3] = 1 → return 1
  ← back in findParent(4): parent[4] = 1 → return 1
← back in findParent(5): parent[5] = 1 → return 1
```

Every stack frame, on its way back down, doesn't just *return* the answer — it **assigns** `parent[node] = <the answer>` before returning it. That assignment is the compression. It happens for free, as a side effect of the recursion unwinding — no separate pass needed.

---

## Now — Back to Our Real Graph

Let's return to the actual running example from Parts 1 and 2, and redo the **final edge** — `union(3, 7)` — but this time with path compression switched on, so we can see exactly how it changes the outcome compared to Part 2.

### State Just Before the Last Edge (identical to Part 2, after edge 5)

```
Node:    1   2   3   4   5   6   7
parent:  1   1   1   4   4   4   6
rank:    1   0   0   2   0   1   0
```

Tree shape:
```
            4
        ┌───┼───┐
        1   5   6
      ┌─┴─┐     │
      2   3     7
```

### union(3, 7) — With Path Compression

**Step 1 — findParent(3), with compression:**

```
3 != parent[3] (which is 1) → recurse
findParent(1): 1 == parent[1] → BASE CASE → return 1

unwinding: parent[3] = 1   (no actual change — it already pointed to 1)
findParent(3) returns 1

pu = 1
```

No visible change here — node 3 was already a direct child of the root, so "compression" is a no-op in this case. This matters: **path compression only does real work when a chain is 2+ hops deep.**

**Step 2 — findParent(7), with compression:**

```
7 != parent[7] (which is 6) → recurse
   findParent(6):
      6 != parent[6] (which is 4) → recurse
         findParent(4): 4 == parent[4] → BASE CASE → return 4

      unwinding: parent[6] = 4   (no change — 6 already pointed to 4)
      findParent(6) returns 4

unwinding: parent[7] = 4    ← REAL CHANGE! 7 used to point to 6, now points DIRECTLY to 4
findParent(7) returns 4

pv = 4
```

**This is the compression actually doing something.** Before this call, `parent[7] = 6`. After this call, `parent[7] = 4` — the intermediate hop through node 6 has been permanently erased for node 7.

```
Node:    1   2   3   4   5   6   7
parent:  1   1   1   4   4   4   4     ← 7 now points DIRECTLY to 4 (was 6)
rank:    1   0   0   2   0   1   0
```

**Step 3 — the union itself:**

```
pu = 1, pv = 4
rank[1] = 1, rank[4] = 2  → node 1's tree is SMALLER

→ attach 1 under 4
   parent[1] = 4
   rank UNCHANGED
```

```
Node:    1   2   3   4   5   6   7
parent:  4   1   1   4   4   4   4
rank:    1   0   0   2   0   1   0
```

### Final Tree Shape — Compare to Part 2

```
Part 2 (no compression):          Part 3 (with compression):

            4                                  4
        ┌───┼───┐                    ┌───┬───┬─┼───┬───┐
        1   5   6                    1   5   6  7   
      ┌─┴─┐     │                  ┌─┴─┐
      2   3     7                  2   3
```

The final `parent[]` array is **identical either way** in this particular case (both end with `parent[7] = 4`, since the union in step 3 also would have collapsed things similarly) — but notice the *path taken to get there* was cheaper, and more importantly, **node 6's subtree got flattened as a side effect**, even though node 6 wasn't the direct subject of this union. Any future `findParent(7)` call now costs exactly one step, not two.

---

## The Crucial Detail — Why Rank Is *Not* Decreased

This is a subtlety worth sitting with, because it's a common point of confusion.

After the compression above, node 4's rank is still `2` — but if you look at the *actual* tree, node 4 directly has `1, 5, 6, 7` all as children, plus `2` and `3` two levels down through `1`. The *true* height of this tree, if you measured it freshly, might no longer match what rank `2` originally represented.

> *So why don't we recalculate rank downward after compression?*

Because it's **not safe to do so in general.** Consider a bigger hypothetical tree where node 4 has multiple large subtrees hanging off it — compressing *one* path (say, just the path to node 7) doesn't mean *every* other branch got shorter too. Some other branch might still be just as deep as before. If you decremented rank based on only the one path you happened to compress, you'd be lying about the tree's true remaining height everywhere else.

```
┌──────────────────────────────────────────────────────────────────┐
│  This is EXACTLY why the array is called "rank" and not          │
│  "height."                                                       │
│                                                                  │
│  Height would need to be exact and would need constant           │
│  updating as the tree changes shape.                             │
│                                                                  │
│  Rank is only a rough, NEVER-DECREASING upper-bound signal:      │
│  "this tree is at least this deep, or was at some point."        │
│  It's good enough to make the "attach smaller to larger"         │
│  decision correctly — that's its only job. It was never          │
│  meant to be a live, precise height tracker.                     │
└──────────────────────────────────────────────────────────────────┘
```

---

## One More Query — Does 3 and 6 Belong to the Same Component?

Let's use this to see compression firing again, on a query rather than a union:

```
findParent(3):
   3 != parent[3] (which is 1) → recurse
   findParent(1): 1 != parent[1] (which is 4) → recurse
      findParent(4): 4 == parent[4] → BASE CASE → return 4
   unwinding: parent[1] = 4  (no change — already 4)
   findParent(1) returns 4

   unwinding: parent[3] = 4   ← COMPRESSED! 3 used to point to 1, now points DIRECTLY to 4
   findParent(3) returns 4

findParent(6):
   6 != parent[6] (which is 4) → recurse
   findParent(4): 4 == parent[4] → BASE CASE → return 4
   unwinding: parent[6] = 4  (no change — already 4)
   findParent(6) returns 4

Compare: findParent(3) = 4,  findParent(6) = 4
4 == 4 → YES, same component.
```

```
Node:    1   2   3   4   5   6   7
parent:  4   1   4   4   4   4   4     ← 3 now points DIRECTLY to 4 (was 1)
rank:    1   0   0   2   0   1   0
```

Notice the tree keeps flattening every time it's touched. Node 2 is the only node left that requires more than one hop (`2 → 1 → 4`) — and it will get compressed the very next time anyone asks for its parent.

---

## Why This Gets You (Effectively) Constant Time

```
┌──────────────────────────────────────────────────────────────────┐
│  Union by Rank alone caps tree height at O(log N).               │
│                                                                  │
│  Path Compression, layered on top, means that EVERY findParent   │
│  call doesn't just answer the current query — it actively        │
│  shortens the tree for every future query on that same path.     │
│                                                                  │
│  The combined, formally proven time complexity per operation     │
│  is O(4α) — where α is the inverse Ackermann function.           │
│  α grows so unbelievably slowly that for any N you could ever    │
│  actually construct (way, way beyond the number of atoms in      │
│  the observable universe), α(N) ≤ 4.                             │
│                                                                  │
│  So O(4α) is, for every practical purpose, O(1) — genuinely      │
│  constant time. This is not an approximation or hand-wave —      │
│  it's a real, if famously intricate, mathematical result.        │
│  You don't need to derive it for interviews — knowing it exists  │
│  and can be quoted as "near-constant, ~O(4α)" is sufficient.     │
└──────────────────────────────────────────────────────────────────┘
```

---

## Quick Checkpoint Before Moving On

- [ ] In the imaginary deep-chain example, why does compressing the path for `findParent(5)` also fix the answer for nodes 2, 3, and 4 — not just node 5?
- [ ] Walk through exactly which line in the recursive `findParent` performs the actual rewiring, and explain why it happens automatically as the recursion unwinds.
- [ ] In our real graph, why did compressing `findParent(7)` change `parent[7]` but compressing `findParent(3)` (the first time, during the union) change nothing?
- [ ] Why is it unsafe to decrement `rank[]` after a path compression shortens part of a tree?
- [ ] After all the operations traced in this note, which single node in the final structure still requires more than one hop to find its ultimate parent — and why hasn't it been compressed yet?

---

Ready for **Part 4 — Full Java Implementation (Union by Rank + Path Compression combined) and the complete complexity derivation** whenever you are.