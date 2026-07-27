# Minimum Spanning Tree (MST) — Introduction

---

## A Quick Flag Before We Start

This lecture is purely visual — Striver draws the graph and the spanning trees on his iPad live, and the transcript (audio-only) has a few numbers that got garbled in translation (e.g. *"6 + 2 is 8 + 3 is 11... sum is 16"* — the audio clearly drops a number mid-sentence). Rather than guess at exact figures that might be wrong, I've built a clean, equivalent example graph from scratch that preserves **every single teaching point** Striver makes — same node count, same edge count, same "multiple valid spanning trees, only one is minimum" structure. You can paste it into csacademy and follow along exactly as if it were his board.

---

## What Problem Are We Solving?

You're given an **undirected, weighted, connected graph** with `n` nodes and `m` edges. There's no fixed relationship between `n` and `m` — could be anything, as long as the graph is connected.

The question MST answers:

> *"What is the cheapest possible way to keep every node connected to every other node?"*

Before we can define "cheapest," we first need to define what "keep every node connected" even means structurally. That's where **spanning tree** comes in.

---

## Step 1 — What Is a Spanning Tree?

### The Example Graph

Paste into csacademy.com (undirected, weighted):

```
0 1 6
0 2 3
1 2 2
1 4 2
2 3 4
2 4 5
3 4 3
```

This graph has **5 nodes** (0 to 4) and **7 edges**.

### The Definition

> A **spanning tree** is a subgraph that has exactly **`n` nodes** and **`n − 1` edges**, and every node is reachable from every other node.

Three conditions, all mandatory:

```
┌────────────────────────────────────────────────────────────────┐
│  1. Contains ALL n nodes of the original graph                 │
│  2. Contains EXACTLY n − 1 edges (not more, not fewer)         │
│  3. Every node is reachable from every other node              │
│     (i.e., it's connected, and — since edges = n−1 —           │
│      it's automatically also acyclic. That's what makes        │
│      it a "tree" and not just "a connected subgraph.")         │
└────────────────────────────────────────────────────────────────┘
```

Think back to what a plain tree looks like — say a simple 5-node tree:

```
0 1
0 2
1 3
1 4
```

5 nodes, 4 edges (`n−1`), everyone reachable from everyone. That's the shape a spanning tree must take — except now it's carved *out of* a bigger, more richly-connected graph.

---

## Step 2 — A Graph Can Have MANY Spanning Trees

This is the part that surprises people the first time: **there is no limit on how many spanning trees a graph can have.** It depends entirely on the graph's structure — some graphs have exactly one, some have dozens.

Let's pull three different spanning trees out of our example graph above. Remember: each one must use all 5 nodes and exactly 4 edges, with everyone connected.

**Spanning Tree A** — pick edges `{0-1, 1-2, 1-4, 2-3}`:

```
0 1 6
1 2 2
1 4 2
2 3 4
```
5 nodes ✓, 4 edges ✓, all connected ✓ → valid spanning tree.
Sum of weights: `6 + 2 + 2 + 4 = 14`

**Spanning Tree B** — pick edges `{0-2, 1-2, 2-4, 2-3}`:

```
0 2 3
1 2 2
2 4 5
2 3 4
```
5 nodes ✓, 4 edges ✓, all connected ✓ → also valid.
Sum of weights: `3 + 2 + 5 + 4 = 14`

**Spanning Tree C** — pick edges `{0-1, 0-2, 2-3, 3-4}`:

```
0 1 6
0 2 3
2 3 4
3 4 3
```
5 nodes ✓, 4 edges ✓, all connected ✓ → also valid.
Sum of weights: `6 + 3 + 4 + 3 = 16`

All three are legitimate spanning trees of the same graph. Nothing stops us from finding even more by picking different subsets of edges — as long as the three conditions from Step 1 hold.

```
┌───────────────────────────────────────────────────────────────┐
│  KEY REALIZATION                                              │
│                                                               │
│  "Spanning tree" is not a unique object per graph.            │
│  A single graph can yield many different spanning trees.      │
│  Which one is "best" depends on what we do next.              │
└───────────────────────────────────────────────────────────────┘
```

---

## Step 3 — What Makes One Spanning Tree "Minimum"?

Now sum up the edge weights of each spanning tree you found — exactly as we did above:

```
Spanning Tree A → sum = 14
Spanning Tree B → sum = 14
Spanning Tree C → sum = 16
```

The **Minimum Spanning Tree (MST)** is simply:

> Among *all* possible spanning trees of a graph, the one (or ones) whose **total edge weight is the smallest**.

Here, Trees A and B are tied at 14 — both qualify as an MST for this graph (a graph can have more than one *minimum* spanning tree too, if there's a tie). Tree C, at 16, is *a* spanning tree, but it is **not** *the* minimum spanning tree.

```
┌────────────────────────────────────────────────────────────────┐
│  Spanning Tree           →  n nodes, n−1 edges, connected      │
│  Minimum Spanning Tree   →  a spanning tree with the LEAST     │
│                              possible sum of edge weights      │
│                                                                │
│  Every MST is a spanning tree.                                 │
│  NOT every spanning tree is an MST.                            │
└────────────────────────────────────────────────────────────────┘
```

This distinction is worth saying explicitly if it ever comes up in an interview — Striver is emphatic about this exact wording: *"you cannot call [a higher-sum tree] a minimum spanning tree — it's a spanning tree, but not the minimum one."*

---

## Step 4 — Try It Yourself (Practice Example)

Here's a slightly bigger graph — 6 nodes, weighted, undirected. Paste into csacademy:

```
0 1 4
0 3 6
1 2 2
1 3 1
1 4 3
2 4 4
3 4 7
4 5 5
```

**Your task:** find the MST — the spanning tree (5 edges, since `n=6`) with the smallest possible total weight. Pause here and work it out by hand before reading on.

<br>

One valid MST for this graph: pick edges `{1-3, 1-2, 1-4, 0-1, 4-5}`:

```
1 3 1
1 2 2
0 1 4
1 4 3
4 5 5
```
6 nodes ✓, 5 edges ✓, all reachable ✓.
Sum: `1 + 2 + 4 + 3 + 5 = 15`

The intuition for *why* this works — greedily grabbing cheap edges while never creating a cycle and never leaving a node stranded — is exactly what next video's algorithm (**Prim's**) formalizes into a repeatable procedure. Right now, the goal is just to feel comfortable eyeballing "is this a valid spanning tree?" and "can I find a cheaper one?" by hand.

---

## Where This Is Headed

```
┌────────────────────────────────────────────────────────────────┐
│  You now know: what a spanning tree is, and what makes one     │
│  "minimum."                                                    │
│                                                                │
│  What's still missing: an actual ALGORITHM to find the MST     │
│  without brute-force generating every possible spanning tree   │
│  (which is exponential — completely impractical for anything   │
│  but toy graphs).                                              │
│                                                                │
│  Two classic algorithms solve this:                            │
│    → Prim's Algorithm    (next video)                          │
│    → Kruskal's Algorithm (needs Disjoint Set / Union-Find      │
│                            as a prerequisite — covered right   │
│                            after Prim's)                       │
└────────────────────────────────────────────────────────────────┘
```

This is exactly why DSU sits in **Pattern 11** right before MST (Pattern 12) in our sheet — Kruskal's algorithm cannot be implemented without it. Striver's sequence is: MST intro (this video) → Prim's → Disjoint Set theory → Kruskal's → DSU problems.

---

## Quick Revision Checklist

- [ ] What are the three mandatory conditions for a subgraph to be called a "spanning tree"?
- [ ] Why does "n nodes, n−1 edges, connected" automatically imply "acyclic" as well?
- [ ] Can a single graph have more than one spanning tree? Can it have more than one *minimum* spanning tree?
- [ ] What is the precise distinction between "a spanning tree" and "the minimum spanning tree"?
- [ ] Why do we need Disjoint Set / Union-Find before we can learn Kruskal's algorithm?

---

Whenever you're ready, send over the **Prim's Algorithm** transcript and we'll continue in the same sequence Striver follows.