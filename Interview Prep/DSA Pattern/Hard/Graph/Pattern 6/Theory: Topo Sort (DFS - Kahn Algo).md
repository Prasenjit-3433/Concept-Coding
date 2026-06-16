# Topological Sort — Kahn's Algorithm (BFS-based)
### Part 1: Conceptual Foundation

---

## What is Topological Sort?

Topological sort is a **linear ordering of vertices** of a directed graph such that for every directed edge **u → v**, vertex **u appears before v** in the ordering.

In plain terms: if task A must happen before task B, then A comes before B in your final order.

---

## The Example Graph

Copy-paste this into **https://csacademy.com/** (directed graph):

```
5 0
5 2
4 0
4 1
2 3
3 1
```

---

## Valid Topological Orderings

For the graph above, two valid orderings are:

```
5 → 4 → 0 → 2 → 3 → 1
4 → 5 → 2 → 3 → 1 → 0
```

**How to verify?** For every edge u → v in your graph, check that u appears before v in your ordering. If all edges satisfy this — it's valid.

> There is no single correct answer. Multiple valid orderings exist. Your job is to find **any one** of them.

---

## Why Only DAGs?

Topological sort is **only valid on a DAG — Directed Acyclic Graph.**

Two conditions must hold. Here's *why* each one is non-negotiable:

---

### Condition 1: Must be Directed

Suppose you have an **undirected** edge between node 1 and node 2.

```
1 2
2 1
```

An undirected edge is essentially **two directed edges** — 1→2 and 2→1 simultaneously.

This means your ordering must satisfy:
- 1 appears before 2 (because of edge 1→2)
- 2 appears before 1 (because of edge 2→1)

Both can never be true at the same time. **Contradiction.** So the graph must be directed.

---

### Condition 2: Must be Acyclic

Suppose you have a cycle:

```
1 2
2 3
3 1
```

Your ordering must satisfy:
- 1 before 2 (edge 1→2)
- 2 before 3 (edge 2→3)
- 3 before 1 (edge 3→1)

So 1 must come before itself. **Impossible.**

A cycle creates a circular dependency — no node can go first without violating some edge. So the graph must be acyclic.

> **Key takeaway:** The moment a graph has a cycle, topological sort breaks down. In fact, Kahn's algorithm has a beautiful side effect — it **detects cycles** automatically. (More on this later.)

---

# Topological Sort — Kahn's Algorithm (BFS-based)
### Part 2: The Key Insight — Why In-degree? Why BFS?

---

## Starting From First Principles

Forget the algorithm for a moment. Ask yourself a simple question:

> **"In a topological ordering, which node can I place first — with absolute confidence?"**

Think about it from the definition: for every edge u → v, u must come before v.

So a node can be placed first **only if nobody needs to come before it** — meaning, **no incoming edges.**

That's it. That's the entire insight.

---

## What is In-degree?

**In-degree of a node = number of incoming edges to that node.**

For the example graph:
```
5 0
5 2
4 0
4 1
2 3
3 1
```

| Node | Incoming Edges | In-degree |
|------|----------------|-----------|
| 0 | from 5, from 4 | 2 |
| 1 | from 4, from 3 | 2 |
| 2 | from 5 | 1 |
| 3 | from 2 | 1 |
| 4 | none | 0 |
| 5 | none | 0 |

Nodes **4 and 5** have in-degree 0 — they have no dependencies. They can go first.

> **Guaranteed:** In any valid DAG, there will always be at least one node with in-degree 0. If there isn't — there's a cycle.

---

## How In-degree Leads Naturally to BFS

Here's the thinking that leads you to BFS on your own, without memorizing anything:

**Step 1:** Start with all nodes whose in-degree is 0 — they're ready to be placed. Put them in a queue.

**Step 2:** Take a node out of the queue. Place it in your ordering. Now, this node's outgoing edges are no longer relevant — this node is already placed before its neighbors. So "remove" its contribution: reduce the in-degree of all its neighbors by 1.

**Step 3:** After reducing, if any neighbor's in-degree drops to 0 — it means all nodes that needed to come before it are already placed. So it's now safe to place. Push it into the queue.

**Step 4:** Repeat until the queue is empty.

This is **exactly BFS** — process level by level, driven by a queue, expanding neighbors. The only twist is: instead of a visited array controlling who enters the queue, it's the **in-degree reaching 0** that controls it.

> **Why not DFS?** DFS works too (previous video), but it requires a stack and a visited array and works backwards. BFS via in-degree is more *intuitive* — you're always picking the next "ready" node, which mirrors how you'd naturally schedule tasks in real life.

---

## The Cycle Detection Bonus

Here's something elegant about Kahn's algorithm:

If the graph has a cycle, the nodes in that cycle will **never reach in-degree 0** — because they're all waiting on each other. So they'll never enter the queue. They'll never be processed.

This means at the end of the algorithm:

> **If the number of nodes in your topological ordering < total number of nodes in the graph → a cycle exists.**

You get cycle detection for free. No extra work needed.

---

# Topological Sort — Kahn's Algorithm (BFS-based)
### Part 3: Workflow + Java Implementation

---

## Workflow (The Mental Model)

When you come back to this after a gap, this is what you need to re-read:

```
1. Build the adjacency list + compute in-degree for every node
   → traverse adjacency list: for every neighbor of node i, do inDegree[neighbor]++

2. Push all nodes with inDegree == 0 into the queue
   → these are your "no dependency" nodes — safe to place first

3. While queue is not empty:
   a. Pop a node from the front → add it to your result (topo order)
   b. For every neighbor of this node:
      → reduce neighbor's inDegree by 1
      → if neighbor's inDegree becomes 0 → push it into the queue
         (reason: all nodes that needed to come before this neighbor are now placed)

4. Return result
   → if result.size() < V → cycle exists (some nodes never reached inDegree 0)
```

---

## Java Implementation

```java
import java.util.*;

class KahnsAlgorithm {

    // Returns topological ordering of V nodes
    // adj is the adjacency list (directed graph)
    static int[] topoSort(int V, ArrayList<ArrayList<Integer>> adj) {

        // Step 1: Compute in-degree for every node
        int[] inDegree = new int[V];
        for (int i = 0; i < V; i++) {
            for (int neighbor : adj.get(i)) {
                inDegree[neighbor]++;
            }
        }
        // WHY: in-degree tells us how many nodes must be placed
        // before a given node. Only when all of them are placed
        // (inDegree drops to 0) is it safe to place this node.

        // Step 2: Push all nodes with inDegree 0 into the queue
        Queue<Integer> queue = new LinkedList<>();
        for (int i = 0; i < V; i++) {
            if (inDegree[i] == 0) {
                queue.add(i);
            }
        }
        // WHY: these nodes have no dependencies — no one needs
        // to come before them. They are safe to place first.

        // Step 3: BFS — process nodes level by level
        int[] topo = new int[V];
        int idx = 0;

        while (!queue.isEmpty()) {
            int node = queue.poll();
            topo[idx++] = node;
            // This node is now placed in the ordering.
            // Its outgoing edges are no longer relevant.

            for (int neighbor : adj.get(node)) {
                inDegree[neighbor]--;
                // WHY: we're "removing" the edge from node → neighbor
                // because node is already placed before neighbor.

                if (inDegree[neighbor] == 0) {
                    queue.add(neighbor);
                }
                // WHY: inDegree == 0 means every node that needed
                // to come before this neighbor is now placed.
                // So this neighbor is ready to be placed too.
            }
        }

        // Step 4: Cycle detection (bonus)
        // If not all nodes were processed → cycle exists
        if (idx != V) {
            System.out.println("Cycle detected! Topological sort not possible.");
        }

        return topo;
    }

    // Driver code to test with the example graph
    public static void main(String[] args) {

        // Graph:
        // 5 0
        // 5 2
        // 4 0
        // 4 1
        // 2 3
        // 3 1
        // (paste above into csacademy.com to visualize)

        int V = 6;
        ArrayList<ArrayList<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < V; i++) adj.add(new ArrayList<>());

        adj.get(5).add(0);
        adj.get(5).add(2);
        adj.get(4).add(0);
        adj.get(4).add(1);
        adj.get(2).add(3);
        adj.get(3).add(1);

        int[] result = topoSort(V, adj);

        System.out.print("Topological Order: ");
        for (int node : result) {
            System.out.print(node + " ");
        }
        // One valid output: 4 5 0 2 3 1
        // Other valid outputs exist — all are acceptable
    }
}
```

---

## Complexity Analysis

| | Complexity | Reason |
|--|------------|--------|
| **Time** | O(V + E) | Every node is enqueued/dequeued once (V). Every edge is visited once when reducing in-degree (E). |
| **Space** | O(V + E) | Adjacency list is O(V+E). In-degree array is O(V). Queue holds at most O(V) nodes. Result array is O(V). |

---

## Quick Revision Checklist

Before any interview, make sure you can answer these cold:

- [ ] What is in-degree and why does it matter here?
- [ ] Why will a DAG always have at least one node with in-degree 0?
- [ ] What happens to cycle nodes — why do they never get processed?
- [ ] How do you detect a cycle using Kahn's algorithm?
- [ ] Why is the time complexity O(V+E) and not O(V²)?

---

That wraps up the full notes for **Kahn's Algorithm**. Want me to also write up the **DFS + Stack based Topological Sort** (which was the previous video in the series) so you have both approaches side by side for comparison?