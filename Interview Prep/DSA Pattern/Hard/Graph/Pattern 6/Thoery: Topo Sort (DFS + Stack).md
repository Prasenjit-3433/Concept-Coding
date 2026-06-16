# Step 1 — What is Topological Sort? (And Why DAG Only?)

---

## What is Topological Sort?

Topological Sort is a **linear ordering of all vertices** of a directed graph such that:

> For every directed edge **u → v**, vertex **u always appears before vertex v** in the ordering.

That's the entire definition. Simple. But let's make it concrete.

---

## Let's Build the Exact Graph Striver Uses

```
    5 ──→ 0
    │
    ↓
    2 ──→ 3 ──→ 1
              ↑
    4 ──→ 0   │
    └─────────┘
```

Let me draw this more precisely with all edges listed:

```
Edges:
  5 → 0
  5 → 2
  4 → 0
  4 → 1
  2 → 3
  3 → 1

Graph (Adjacency List):
  0 → []
  1 → []
  2 → [3]
  3 → [1]
  4 → [0, 1]
  5 → [0, 2]
```

Now, one valid topological ordering Striver finds is:

```
  5 → 4 → 2 → 3 → 1 → 0
```

Let's verify every edge satisfies "u appears before v":

```
  Edge 5→0 : 5 comes before 0 ✔
  Edge 5→2 : 5 comes before 2 ✔
  Edge 4→0 : 4 comes before 0 ✔
  Edge 4→1 : 4 comes before 1 ✔
  Edge 2→3 : 2 comes before 3 ✔
  Edge 3→1 : 3 comes before 1 ✔
```

All edges satisfied. This is a valid topological sort.

> **Note:** There can be multiple valid orderings. You just need to produce any one of them.

---

## Why ONLY on a Directed Acyclic Graph (DAG)?

Striver explains two separate reasons — one for "directed", one for "acyclic." Let's derive both from scratch.

---

### Reason 1: Why NOT on an Undirected Graph?

Take a simple undirected edge between node **1** and node **2**:

```
  1 ── 2      (undirected)
```

In an undirected graph, an edge between 1 and 2 actually means **both directions exist simultaneously**:

```
  1 → 2   (1 must come before 2)
  2 → 1   (2 must come before 1)
```

Now apply the topological sort definition:
- Edge 1→2 says: **1 must appear before 2**
- Edge 2→1 says: **2 must appear before 1**

These two constraints directly **contradict each other**. You cannot satisfy both at once in any linear ordering.

```
  Can we write:  1, 2  ?
    → 1 before 2 ✔  but 2 before 1 ✘

  Can we write:  2, 1  ?
    → 2 before 1 ✔  but 1 before 2 ✘
```

**Conclusion:** Topological sort is undefined on undirected graphs.

---

### Reason 2: Why NOT on a Graph with a Cycle?

Take this directed graph with a cycle:

```
  1 → 2 → 3
  ↑         │
  └─────────┘
```

Edges:
```
  1 → 2   (1 must come before 2)
  2 → 3   (2 must come before 3)
  3 → 1   (3 must come before 1)
```

Now try to find a linear ordering:

```
  From 1→2 :  order must have  1 ... 2
  From 2→3 :  order must have  2 ... 3
  From 3→1 :  order must have  3 ... 1

  Combining: 1 must be before 2
             2 must be before 3
             3 must be before 1
  
  So:   1 < 2 < 3 < 1   → IMPOSSIBLE
```

This is a **cyclic dependency** — node 1 indirectly depends on itself. There is no starting point. No matter which node you place first, the cycle will always create a contradiction.

```
  Intuition:
  ┌─────────────────────────────────────────────┐
  │  A cycle means: "A needs B, B needs C,      │
  │  and C needs A." There's no valid order.    │
  │  It's like a deadlock.                      │
  └─────────────────────────────────────────────┘
```

**Conclusion:** Topological sort requires the graph to be acyclic.

---

## The Full Picture — Why DAG?

```
  Directed   →  edges have a clear direction, so "before/after" is meaningful
  Acyclic    →  no circular dependencies, so a valid ordering can always exist
  
  DAG = the only type of graph where topological sort makes sense
```

---

## Real World Analogy (to lock in the intuition)

Think of university course prerequisites:

```
  Math 101 → Math 201 → Math 301
                ↓
             Physics 101 → Physics 201
```

You must take Math 101 before Math 201. Math 201 before Math 301. There's no cycle (you don't need Physics 201 to take Math 101). A topological sort gives you a valid study sequence.

If there were a cycle — "you need Course A to take Course B, and Course B to take Course A" — **no valid enrollment order exists**. That's exactly why cycles break topological sort.

---

## Quick Summary Before We Move On

```
┌──────────────────────────────────────────────────────────┐
│  Topological Sort                                        │
│                                                          │
│  • Linear ordering of vertices                           │
│  • For every edge u→v, u appears before v                │
│  • Multiple valid orderings may exist                    │
│  • Only defined on DAGs (Directed Acyclic Graphs)        │
│                                                          │
│  Why Directed?  → "before/after" needs a direction       │
│  Why Acyclic?   → cycles create contradictions           │
└──────────────────────────────────────────────────────────┘
```

---

# Step 2 — The DFS + Stack Algorithm (Rewritten)

---

## The Graph We're Working With

Paste this into [csacademy.com](https://csacademy.com/) to visualize:

```
5 0
5 2
4 0
4 1
2 3
3 1
```

Adjacency list (for reference):
```
0 → []
1 → []
2 → [3]
3 → [1]
4 → [0, 1]
5 → [0, 2]
```

---

## Why a Stack? (Derived from First Principles)

Before anything else, let's ask the honest question:

> DFS already visits nodes in some order. Why do we need a stack on top of that? And why specifically a stack?

Take a simple chain to think it through:

```
1 2
2 3
3 4
```

Valid topological order: `1 → 2 → 3 → 4`

Now watch what DFS does naturally:

```
DFS starts at 1 → goes to 2 → goes to 3 → goes to 4
  4 has no neighbours → 4 FINISHES first
  back to 3, done    → 3 FINISHES second
  back to 2, done    → 2 FINISHES third
  back to 1, done    → 1 FINISHES last

DFS finish order :  4, 3, 2, 1
Valid topo order :  1, 2, 3, 4
```

The finish order is **exactly the reverse** of the valid topological order. This is not a coincidence — it's the core insight:

```
┌──────────────────────────────────────────────────────────────┐
│  DFS finishes a node ONLY AFTER every node reachable from    │
│  it has already finished.                                    │
│                                                              │
│  So nodes that should come LAST in topological order         │
│  finish FIRST in DFS.                                        │
│                                                              │
│  Reversing the finish order gives topological order.         │
│  What reverses order naturally? → A STACK (LIFO)             │
└──────────────────────────────────────────────────────────────┘
```

A stack is not an arbitrary choice — it is the **direct expression** of this insight. Push nodes as they finish, pop them out to get topological order. No manual reversal needed.

---

## The Workflow

This is the mental model to carry. Read this once and the whole algorithm re-anchors itself:

```
1. Maintain a visited[] array and a stack.

2. Iterate over all nodes 0 to V-1.
   For each unvisited node, call DFS.
   (This handles disconnected graphs too —
    every component gets covered.)

3. Inside DFS:
   a. Mark current node as visited.
   b. Recurse into every unvisited neighbour first.
   c. ONLY AFTER all neighbours are fully done,
      push the current node onto the stack.

4. Once all DFS calls are complete,
   pop the stack one by one — that's your
   topological order.
```

The one rule that drives everything:

```
┌──────────────────────────────────────────────────────────────┐
│  A node enters the stack only when its entire                │
│  reachable subgraph is already in the stack below it.        │
│  So when you pop, dependents always come out after           │
│  the nodes they depend on.                                   │
└──────────────────────────────────────────────────────────────┘
```

---

## Java Implementation

```java
import java.util.*;

class TopologicalSort {

    private static void dfs(int node, int[] visited,
                            Stack<Integer> stack,
                            List<List<Integer>> adj) {

        // Mark current node visited before going deeper
        // (prevents revisiting in case of multiple paths to same node)
        visited[node] = 1;

        // Go deeper first — exhaust everything reachable
        // from this node before coming back
        for (int neighbour : adj.get(node)) {
            if (visited[neighbour] == 0) {
                dfs(neighbour, visited, stack, adj);
            }
        }

        // All neighbours are done — this node's DFS is complete
        // Everything it points to is already deeper in the stack
        // So this node belongs BEFORE all of them → push it now
        stack.push(node);
    }

    public static List<Integer> topoSort(int V,
                                         List<List<Integer>> adj) {

        int[] visited = new int[V];
        Stack<Integer> stack = new Stack<>();

        // Iterate all nodes — ensures disconnected
        // components are not missed
        for (int i = 0; i < V; i++) {
            if (visited[i] == 0) {
                dfs(i, visited, stack, adj);
            }
        }

        // Pop stack → topological order
        List<Integer> result = new ArrayList<>();
        while (!stack.isEmpty()) {
            result.add(stack.pop());
        }

        return result;
    }

    public static void main(String[] args) {
        int V = 6;
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < V; i++) adj.add(new ArrayList<>());

        // Edges: 5→0, 5→2, 4→0, 4→1, 2→3, 3→1
        adj.get(5).add(0);
        adj.get(5).add(2);
        adj.get(4).add(0);
        adj.get(4).add(1);
        adj.get(2).add(3);
        adj.get(3).add(1);

        List<Integer> result = topoSort(V, adj);
        System.out.println(result); // [5, 4, 2, 3, 1, 0]
    }
}
```

---

## Complexity

```
┌──────────────────────────────────────────────┐
│  Time  → O(V + E)                            │
│          Every node visited once  → O(V)     │
│          Every edge traversed once → O(E)    │
│                                              │
│  Space → O(V)                                │
│          visited[]      → O(V)               │
│          stack          → O(V)               │
│          DFS call stack → O(V) worst case    │
└──────────────────────────────────────────────┘
```

---

## Quick Refresh Card
*(the only thing you need to re-read after a long gap)*

```
┌──────────────────────────────────────────────────────────────┐
│  TOPOLOGICAL SORT — DFS + STACK                              │
│                                                              │
│  Core idea:  DFS finish order is reverse topo order.         │
│              Stack captures that reversal naturally.         │
│                                                              │
│  Key rule:   Push a node ONLY after all its neighbours       │
│              are fully done.                                 │
│                                                              │
│  Outer loop: Iterate all nodes — handles disconnected        │
│              components.                                     │
│                                                              │
│  Valid only on DAGs.                                         │
│  Time: O(V+E)   Space: O(V)                                  │
└──────────────────────────────────────────────────────────────┘
```

---

Ready for **Step 3 — Interview Tips + How to Identify Topological Sort Problems** even when the problem doesn't explicitly mention it?