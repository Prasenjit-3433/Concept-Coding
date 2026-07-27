# Disjoint Set (Union-Find) — Part 1: Why Do We Need This At All?

---

## A Quick Flag Before We Start

This is the single densest lecture in the entire Graph series — Striver himself calls it one of the most important (and trickiest) topics. Given how much ground it covers (motivation → find-parent → Union by Rank → Path Compression → Union by Size → full Java), we're splitting it into **multiple parts**, exactly as requested, so each piece gets the depth it deserves. This is **Part 1: the motivation** — why does this data structure need to exist at all, when we already know DFS/BFS?

---

## The Setup — Two Separate Components

Imagine you're given these nodes, forming two completely separate components:

```
Component A: 1 2 3 4
Component B: 5 6 7
```

Someone walks up and asks:

> *"Do node 1 and node 5 belong to the same component?"*

### The Brute Force Answer

The obvious approach: run a DFS starting from node 1. Traverse `1 → 2 → 3 → 4`. Once the traversal is complete, check — did you ever visit node 5? No. So the answer is **no**, they don't belong to the same component.

```
┌─────────────────────────────────────────────────────────────────┐
│  Brute force: run DFS/BFS from u, check if v was visited        │
│                                                                 │
│  Time complexity: O(N + E)                                      │
│  (standard traversal cost — nothing new here)                   │
└─────────────────────────────────────────────────────────────────┘
```

This works. It's correct. But it's **linear time**, every single time you ask the question. If you need to ask this question repeatedly — and especially if the graph keeps changing — this becomes expensive fast.

### What Disjoint Set Promises Instead

> *"I'm going to answer 'does u belong to the same component as v?' in constant time — O(1) — no traversal needed."*

That's the entire value proposition. Not a different way of building the graph — a fundamentally faster way of answering **connectivity queries** on it.

---

## The Second Half of the Motivation — Dynamic Graphs

This is the part that's easy to gloss over but is actually the *whole reason* this data structure exists. Disjoint Set isn't just "DFS but faster" — it's built specifically for graphs that **change over time**, where edges get added one at a time, and you might need to answer a connectivity query **at any point in between** those additions — not just once, at the very end, on the finished graph.

### The Exact Graph We'll Use Throughout This Entire Theory Note

Striver builds one single running example and reuses it for every concept in this lecture — motivation, Union by Rank, Path Compression, and Union by Size. We'll do the same. Here are the nodes and the edges, added **one at a time, in this exact order**:

```
Nodes: 1 2 3 4 5 6 7   (all start out completely disconnected — every node is its own component)

Edges added in sequence:
1 2
2 3
4 5
6 7
5 6
3 7
```

### Before Any Edges — The Starting Point

Before a single edge is added, every node is alone — its own isolated component:

```
{1}  {2}  {3}  {4}  {5}  {6}  {7}
```

This is the natural starting state for Disjoint Set: **every node begins as its own component**, and every `union` operation merges two components together.

### Building the Graph, One Edge at a Time

```
Add edge (1, 2):
   Components: {1, 2}  {3}  {4}  {5}  {6}  {7}

Add edge (2, 3):
   Components: {1, 2, 3}  {4}  {5}  {6}  {7}

Add edge (4, 5):
   Components: {1, 2, 3}  {4, 5}  {6}  {7}

Add edge (6, 7):
   Components: {1, 2, 3}  {4, 5}  {6, 7}
```

**Stop right here.** This is the exact moment Striver poses the first question:

> *"Does 1 and 4 belong to the same component?"*

Look at the components as they stand right now: `{1, 2, 3}` and `{4, 5}` are **separate**. Node 1 lives in the first group, node 4 lives in the second. They are **not** connected.

```
┌─────────────────────────────────────────────────────────────────┐
│  Answer at this point: NO — 1 and 4 do NOT belong to the        │
│  same component.                                                │
│                                                                 │
│  This is the key behavior Disjoint Set must support: you        │
│  can pause the graph's construction at ANY point and ask        │
│  this question, and it must reflect the graph's state           │
│  EXACTLY as it is at that instant — not the final graph.        │
└─────────────────────────────────────────────────────────────────┘
```

### Continuing the Construction

```
Add edge (5, 6):
   Components: {1, 2, 3}  {4, 5, 6, 7}
   (this merge is the interesting one — adding 5-6 doesn't just
    join two single nodes, it fuses the ENTIRE {4,5} group with
    the ENTIRE {6,7} group into one bigger component)

Add edge (3, 7):
   Components: {1, 2, 3, 4, 5, 6, 7}
   (everything is now one single component)
```

Now, **after this final edge**, Striver asks the same question again:

> *"Does 1 and 4 belong to the same component?"*

This time, the answer has **flipped**:

```
┌─────────────────────────────────────────────────────────────────┐
│  Answer now: YES — 1 and 4 DO belong to the same component.     │
│                                                                 │
│  Nothing about node 1 or node 4 themselves changed. What        │
│  changed is the STRUCTURE connecting them — two edges added     │
│  after the first question (5-6 and 3-7) bridged the two         │
│  previously-separate groups into one.                           │
└─────────────────────────────────────────────────────────────────┘
```

---

## The Core Realization

```
┌─────────────────────────────────────────────────────────────────┐
│  The SAME query — "does u and v belong to the same              │
│  component?" — can have DIFFERENT answers depending on          │
│  WHEN in the graph's construction you ask it.                   │
│                                                                 │
│  This rules out any approach that only works on a               │
│  "finished, static" graph. The data structure has to be         │
│  something you can query and update incrementally, edge         │
│  by edge, and get a correct, instant answer at every step.      │
│                                                                 │
│  That is precisely what Disjoint Set is built for.              │
└─────────────────────────────────────────────────────────────────┘
```

---

## The Two Core Operations Disjoint Set Provides

Everything Disjoint Set does boils down to exactly two operations:

```
┌────────────────────────────────────────────────────────────────┐
│  1. findParent(node)                                             │
│     → Returns the "ultimate parent" (the representative/boss)   │
│       of whichever component `node` currently belongs to.       │
│                                                                  │
│     To check "do u and v belong to the same component?" —       │
│     just compute findParent(u) and findParent(v) and compare.   │
│     Same ultimate parent → same component. Different →          │
│     different components. That's the entire query.              │
│                                                                  │
│  2. union(u, v)                                                  │
│     → Merges the component containing u with the component      │
│       containing v, so they become one single component.        │
│                                                                  │
│     This is what you call every time a new edge gets added      │
│     to the dynamic graph.                                       │
└────────────────────────────────────────────────────────────────┘
```

`union` itself can be implemented two different ways — **Union by Rank** and **Union by Size** — both of which we'll build from scratch in the next parts. Both rely on `findParent` internally, and both get supercharged by an optimization called **Path Compression**, which is what ultimately gets the whole data structure down to *effectively* constant time per operation.

---

## What's Coming Next

```
┌──────────────────────────────────────────────────────────────────┐
│  Part 2 → Union by Rank: the rank[] and parent[] arrays,         │
│           full mechanics traced through our exact graph          │
│           (edges 1-2, 2-3, 4-5, 6-7, 5-6, 3-7 in order)          │
│                                                                  │
│  Part 3 → Path Compression: why plain Union by Rank alone is     │
│           only O(log N), and how path compression collapses      │
│           it further, traced with the deep 7-node chain example  │
│                                                                  │
│  Part 4 → Full Java implementation (Union by Rank + Path         │
│           Compression combined) + complexity derivation (4α)     │
│                                                                  │
│  Part 5 → Union by Size: the alternate (arguably more intuitive) │
│           implementation, traced through the same graph,         │
│           full Java code, and the size-vs-rank comparison        │
└──────────────────────────────────────────────────────────────────┘
```

---

## Quick Checkpoint Before Moving On

- [ ] Why is DFS/BFS-per-query not good enough here — what specifically does Disjoint Set improve on?
- [ ] What does "dynamic graph" mean in this context, and why does it matter that the graph is built edge-by-edge?
- [ ] At the moment right after edge `(6,7)` was added, why is the answer to "1 and 4 same component?" **no** — walk through which components exist at that exact instant.
- [ ] What are the exact two operations Disjoint Set provides, and what does each one return/do?
- [ ] What does it mean for a node to be its own "ultimate parent" before any unions have happened?

---

Ready for **Part 2 — Union by Rank** whenever you are. I'll pick up with the `rank[]`/`parent[]` initial configuration and trace every one of the six edge additions on this exact graph, exactly as Striver does on his board.