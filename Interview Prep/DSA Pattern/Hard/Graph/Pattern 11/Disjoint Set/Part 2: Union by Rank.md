# Disjoint Set (Union-Find) — Part 2: Union by Rank

---

## What We Need Before Writing Any Code

Two arrays, sized for the number of nodes:

```
parent[] — parent[i] tells you the node directly above i in its component's tree
rank[]   — rank[i] is a rough measure of "how tall the tree rooted at i has grown"
```

### Initial Configuration

Before any unions happen, every node is its own component — so every node is its own parent, and every rank starts at 0:

```
Node:    1   2   3   4   5   6   7
parent:  1   2   3   4   5   6   7      (everyone is their own parent)
rank:    0   0   0   0   0   0   0      (no one has anyone "beneath" them yet)
```

```
┌─────────────────────────────────────────────────────────────────┐
│  What does rank 0 mean right now?                               │
│  "There are zero nodes beneath this node."                      │
│                                                                 │
│  This meaning will shift slightly as we go — rank isn't         │
│  literally "count of nodes beneath," it's closer to "height     │
│  of the tree below this node." We'll see exactly why that       │
│  distinction matters once Path Compression enters the picture   │
│  in Part 3.                                                     │
└─────────────────────────────────────────────────────────────────┘
```

---

## What Is the "Ultimate Parent"?

Before touching the union logic, nail down this one idea, because everything else depends on it:

> Given a node, its **ultimate parent** is whatever you get by repeatedly asking *"who's your parent?"* until you reach a node that is **its own parent**.

That node — the one sitting at the very top, whose `parent[node] == node` — is the ultimate parent (also called the "boss," or the representative of the whole component).

```
findParent(node):
    if node == parent[node]:
        return node              ← you've hit the boss, stop here
    return findParent(parent[node])   ← otherwise, keep climbing up
```

Right now, since every node is its own parent, every node is currently its own ultimate parent too. That will change as unions happen.

---

## The Union by Rank — Pseudocode

```
union(u, v):
    pu = findParent(u)     ← ultimate parent of u
    pv = findParent(v)     ← ultimate parent of v

    if pu == pv:
        return              ← already in the same component, nothing to do

    if rank[pu] < rank[pv]:
        parent[pu] = pv      ← smaller tree attaches under the bigger one
    else if rank[pv] < rank[pu]:
        parent[pv] = pu      ← same idea, other direction
    else:
        parent[pv] = pu      ← EQUAL ranks: attach either way, doesn't matter
        rank[pu] += 1         ← rank only grows when two EQUAL-rank trees merge
```

The one rule to lock in before anything else:

```
┌─────────────────────────────────────────────────────────────────┐
│  ALWAYS attach the SMALLER-rank tree underneath the LARGER-     │
│  rank tree.                                                     │
│                                                                 │
│  Rank only increases when two trees of EQUAL rank merge —       │
│  because that's the only situation where the resulting tree's   │
│  height genuinely grows by one. Attaching a smaller tree to a   │
│  bigger one never increases the bigger tree's height, so its    │
│  rank stays exactly the same.                                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## Full Trace — All Six Edges, In Order

We'll process the exact same sequence from Part 1: `(1,2)`, `(2,3)`, `(4,5)`, `(6,7)`, `(5,6)`, `(3,7)`. This trace does **not** yet include Path Compression — we're tracking `findParent` exactly as the plain recursive definition above describes, so you can see, honestly, what happens without the optimization. Part 3 picks up from exactly where this leaves off.

### Edge 1 — union(1, 2)

```
pu = findParent(1) = 1     (1 is its own parent)
pv = findParent(2) = 2     (2 is its own parent)

rank[1] = 0, rank[2] = 0  → EQUAL

→ attach 2 under 1 (arbitrary choice when equal — Striver picks this direction)
   parent[2] = 1
   rank[1] += 1  →  rank[1] = 1
```

```
Node:    1   2   3   4   5   6   7
parent:  1   1   3   4   5   6   7
rank:    1   0   0   0   0   0   0
```

Tree so far:
```
1 2
```
(paste-able: node 1 is the root, node 2 hangs beneath it)

---

### Edge 2 — union(2, 3)

```
pu = findParent(2)
   2 != parent[2] (which is 1) → recurse
   findParent(1): 1 == parent[1] → return 1
   so pu = 1

pv = findParent(3) = 3     (3 is its own parent)

rank[1] = 1, rank[3] = 0  → 3's tree is SMALLER

→ attach 3 under 1 (the ultimate parent of 2, not under 2 itself!)
   parent[3] = 1
   rank UNCHANGED — attaching smaller to larger never grows the larger tree's height
```

```
Node:    1   2   3   4   5   6   7
parent:  1   1   1   4   5   6   7
rank:    1   0   0   0   0   0   0
```

```
┌──────────────────────────────────────────────────────────────────┐
│  IMPORTANT DETAIL: node 3 attaches directly to 1 — the           │
│  ULTIMATE parent of 2 — not to node 2 itself. This is exactly    │
│  why we compute findParent(2) first, rather than just looking    │
│  at parent[2] directly. The union always operates on ultimate    │
│  parents, never on the raw nodes passed in.                      │
└──────────────────────────────────────────────────────────────────┘
```

---

### Edge 3 — union(4, 5)

```
pu = findParent(4) = 4
pv = findParent(5) = 5

rank[4] = 0, rank[5] = 0  → EQUAL

→ attach 5 under 4
   parent[5] = 4
   rank[4] += 1  →  rank[4] = 1
```

```
Node:    1   2   3   4   5   6   7
parent:  1   1   1   4   4   6   7
rank:    1   0   0   1   0   0   0
```

---

### Edge 4 — union(6, 7)

```
pu = findParent(6) = 6
pv = findParent(7) = 7

rank[6] = 0, rank[7] = 0  → EQUAL

→ attach 7 under 6
   parent[7] = 6
   rank[6] += 1  →  rank[6] = 1
```

```
Node:    1   2   3   4   5   6   7
parent:  1   1   1   4   4   6   6
rank:    1   0   0   1   0   1   0
```

**Checkpoint — same as Part 1's question:**

> *Does 1 and 4 belong to the same component?*

```
findParent(1) = 1
findParent(4) = 4

1 != 4 → NOT the same component. Matches Part 1's answer exactly.
```

---

### Edge 5 — union(5, 6)

```
pu = findParent(5)
   5 != parent[5] (which is 4) → recurse
   findParent(4): 4 == parent[4] → return 4
   so pu = 4

pv = findParent(6) = 6     (6 is its own parent... wait, check: parent[6] = 6? 
   Yes — node 6 never got attached to anyone; node 7 attached to node 6.
   So parent[6] = 6, meaning 6 IS its own ultimate parent.)
   pv = 6

rank[4] = 1, rank[6] = 1  → EQUAL

→ attach 6 under 4 (arbitrary direction, since equal — Striver's choice)
   parent[6] = 4
   rank[4] += 1  →  rank[4] = 2
```

```
Node:    1   2   3   4   5   6   7
parent:  1   1   1   4   4   4   6
rank:    1   0   0   2   0   1   0
```

```
┌──────────────────────────────────────────────────────────────────┐
│  Notice what just happened: attaching 6 under 4 didn't just      │
│  merge two single nodes — node 7 came along for free, because    │
│  it was already hanging off node 6. This is exactly the          │
│  "whole component fuses with whole component" behavior           │
│  flagged back in Part 1.                                         │
└──────────────────────────────────────────────────────────────────┘
```

---

### Edge 6 — union(3, 7)

This is the interesting one — it involves finding the ultimate parent of a node that's now **two levels deep**.

```
pu = findParent(3)
   3 != parent[3] (which is 1) → recurse
   findParent(1): 1 == parent[1] → return 1
   so pu = 1

pv = findParent(7)
   7 != parent[7] (which is 6) → recurse
   findParent(6): 6 != parent[6] (which is 4) → recurse
   findParent(4): 4 == parent[4] → return 4
   ...unwinding back up: findParent(6) returns 4, findParent(7) returns 4
   so pv = 4

rank[1] = 1, rank[4] = 2  → node 1's tree is SMALLER

→ attach 1 under 4 (the smaller tree goes under the larger one)
   parent[1] = 4
   rank UNCHANGED (attaching smaller to larger doesn't grow the larger tree)
```

```
Node:    1   2   3   4   5   6   7
parent:  4   1   1   4   4   4   6
rank:    1   0   0   2   0   1   0
```

**Final checkpoint — same question, asked after the last edge:**

> *Does 1 and 4 belong to the same component?*

```
findParent(1):
   1 != parent[1] (which is 4) → recurse
   findParent(4): 4 == parent[4] → return 4
   so findParent(1) = 4

findParent(4) = 4

4 == 4 → YES, same component. Matches Part 1's flipped answer exactly.
```

---

## The Final Tree Structure — Visualized

```
                4
        ┌───────┼───────┐
        1       5       6
      ┌─┴─┐             │
      2   3             7
```

Paste-able edge list (root-to-child direction, for csacademy):
```
4 1
4 5
4 6
1 2
1 3
6 7
```

All 7 nodes now share the single ultimate parent **4** — confirming the entire graph has become one connected component, exactly as concluded in Part 1.

---

## The Problem This Trace Exposes

Look closely at what happened when we computed `findParent(7)` during the last union: it had to climb `7 → 6 → 4` — **two hops** — before finding the boss. That's not expensive yet with only 7 nodes, but think about what happens as the graph grows:

```
┌──────────────────────────────────────────────────────────────────┐
│  Even with Union by Rank keeping trees as flat as possible,      │
│  the tree's height can still grow — Striver proves it            │
│  theoretically caps out at O(log N) in the worst case.           │
│                                                                  │
│  So a plain findParent call costs O(log N), not O(1).            │
│  That's already a huge win over DFS's O(N + E) — but we were     │
│  promised CONSTANT time. O(log N) isn't quite there yet.         │
└──────────────────────────────────────────────────────────────────┘
```

The fix is **Path Compression** — and the moment where we computed `findParent(7)` climbing `7 → 6 → 4` in this very trace is the exact scenario that motivates it. If we're already paying the cost to climb that chain once, why not permanently shortcut it so nobody ever has to climb it again?

---

## Quick Checkpoint Before Moving On

- [ ] Why does `union(2, 3)` attach node 3 to node **1**, not to node 2 — even though the operation was called with `(2, 3)`?
- [ ] Walk through why `rank[4]` becomes exactly `2` by the end, and identify precisely which two union operations caused it to increase.
- [ ] In the final structure, what is the ultimate parent of every single node? Verify each one by hand.
- [ ] Why does attaching a smaller-rank tree under a larger-rank tree never require incrementing the larger tree's rank?
- [ ] What specific chain of parent-pointers in this trace foreshadows the need for Path Compression?

---

Ready for **Part 3 — Path Compression** whenever you are — we'll pick up right where this leaves off, using Striver's deep-chain example to show exactly how compression collapses long chains permanently.