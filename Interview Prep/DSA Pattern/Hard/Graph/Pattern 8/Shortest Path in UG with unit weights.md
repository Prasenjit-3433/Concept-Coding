# Shortest Path in Undirected Graph with Unit Weights
## Part 1 — Problem Understanding + Graph Setup + The "Why BFS?" Intuition

---

### What is the Problem Asking?

You are given:
- An **undirected graph** where every edge has **weight = 1** (unit weight)
- A **source node**
- You must find the **shortest distance from the source to every other node**
- If a node is **unreachable**, return **-1** for it

---

### The Example Graph

Paste this into [csacademy.com](https://csacademy.com/) to visualize:

```
0 1
0 3
1 2
1 3
2 6
3 4
4 5
5 6
6 7
6 8
7 8
```

Source node = **0**

Expected shortest distances from node 0:

| Node | Shortest Distance |
|------|-------------------|
| 0    | 0                 |
| 1    | 1                 |
| 2    | 2                 |
| 3    | 1                 |
| 4    | 2                 |
| 5    | 3                 |
| 6    | 3                 |
| 7    | 4                 |
| 8    | 4                 |

---

### First Principles — Why Does BFS Work Here? (The Core Intuition)

This is the most important question. Don't just memorize "use BFS for unit weight graphs." Understand *why.*

**Think about what BFS naturally does:**

BFS explores a graph **level by level**. It first visits all nodes at distance 1 from the source, then all nodes at distance 2, then distance 3, and so on.

Now think about what "unit weight" means:

> Every single edge costs exactly **+1**.

So when you're at a node that you reached at distance `d`, every neighbour of that node is reachable at exactly distance `d + 1`. Not `d + 2`, not `d + 0.5`. Always `d + 1`.

This means the queue in BFS naturally looks like this at any point in time:

```
Queue state (conceptually):

[ nodes at distance 1 | nodes at distance 2 | nodes at distance 3 | ... ]

The queue is ALWAYS sorted by distance — automatically.
```

This is the key insight the instructor emphasizes:

> **Because edge weight is always +1, the queue is always in sorted order of distance. You never need to manually sort. A plain queue is enough.**

---

### Why Can't You Use a Plain Queue for Weighted Graphs?

This is the flip side — understanding the contrast builds the intuition permanently.

Suppose edges had **different weights**, like 1, 5, 10. Then when you process a node at distance `d`, its neighbours could be at `d+1`, `d+5`, or `d+10`. The queue would mix up nodes at very different distances, and processing order would be wrong — you might process a node at distance 10 before a shorter path of distance 3 is even discovered.

That's exactly why weighted graphs need **Dijkstra's algorithm**, which uses a **priority queue (min-heap)** to always process the closest node next, keeping things sorted.

But here? All edges are +1. The queue sorts itself. Dijkstra is overkill. A plain `Queue` is all you need.

---

### The Data Structures You Need and Why

**1. Queue**

- Stores nodes to process in BFS order
- Since all edges are unit weight, the queue stays naturally sorted by distance
- No priority queue needed

**2. Distance Array** (size = number of nodes)

- Stores the **shortest distance found so far** to each node
- Initialized to **infinity** (a very large number, e.g., `Integer.MAX_VALUE` or `1e9`)
- The source node's distance is set to **0** before BFS begins
- This replaces the usual `visited[]` boolean array — because here, knowing the *shortest distance* is more useful than just knowing *whether visited*

**Why distance array instead of visited array?**

In a standard BFS (say, finding connected components), you just need to know: *have I been here before?* A boolean is enough.

But here, you need to know: *what is the shortest distance to get here?* And you only update a node's distance if the new path is shorter. The distance array serves both purposes — it tells you if a node was visited (distance ≠ infinity) AND what the shortest distance is.

---

### Key Condition — When Do You Update?

When you're at node `u` (reached at distance `d`) and you look at neighbour `v`:

```
If (distance[u] + 1) < distance[v]:
    → Update distance[v] = distance[u] + 1
    → Push v into the queue

If (distance[u] + 1) >= distance[v]:
    → v was already reached via a shorter or equal path
    → Discard. Do nothing.
```

This greedy condition ensures you never overwrite a shorter distance with a longer one.

---
## Part 2 — Algorithm Workflow (Clean Memory-Refresh Version)

---

### The Overall Workflow at a Glance

```
Build adjacency list
        ↓
Initialize distance[] = ∞ for all nodes
Set distance[source] = 0
        ↓
Push source into Queue
        ↓
While Queue is not empty:
    Poll node u from Queue
    For each neighbour v of u:
        If distance[u] + 1 < distance[v]:
            Update distance[v] = distance[u] + 1
            Push v into Queue
        Else:
            Discard v (already reached via shorter/equal path)
        ↓
Queue empty → done
        ↓
Build answer[]:
    If distance[i] == ∞ → answer[i] = -1
    Else               → answer[i] = distance[i]
```

---

### How the Distance Array Evolves (Snapshot Style)

Rather than a step-by-step dry run, here's the mental model of how the distance array fills up in waves — matching the BFS level structure:

```
INITIAL STATE
─────────────────────────────────────────────────
Node:      0    1    2    3    4    5    6    7    8
Distance:  0    ∞    ∞    ∞    ∞    ∞    ∞    ∞    ∞
Queue:    [0]

WAVE 1 — Processing node 0 (distance = 0)
Neighbours of 0: {1, 3}
Both at ∞ → update both to 0+1 = 1
─────────────────────────────────────────────────
Node:      0    1    2    3    4    5    6    7    8
Distance:  0    1    ∞    1    ∞    ∞    ∞    ∞    ∞
Queue:    [1, 3]

WAVE 2 — Processing node 1 (distance = 1)
Neighbours of 1: {0, 2, 3}
  0 → distance 2 > already 0 → discard
  2 → distance 2 < ∞         → update
  3 → distance 2 > already 1 → discard
─────────────────────────────────────────────────
Node:      0    1    2    3    4    5    6    7    8
Distance:  0    1    2    1    ∞    ∞    ∞    ∞    ∞
Queue:    [3, 2]

WAVE 2 continued — Processing node 3 (distance = 1)
Neighbours of 3: {0, 1, 4}
  0 → distance 2 > already 0 → discard
  1 → distance 2 > already 1 → discard
  4 → distance 2 < ∞         → update
─────────────────────────────────────────────────
Node:      0    1    2    3    4    5    6    7    8
Distance:  0    1    2    1    2    ∞    ∞    ∞    ∞
Queue:    [2, 4]

WAVE 3 — Processing node 2 (distance = 2)
Neighbours of 2: {1, 6}
  1 → distance 3 > already 1 → discard
  6 → distance 3 < ∞         → update
─────────────────────────────────────────────────
Node:      0    1    2    3    4    5    6    7    8
Distance:  0    1    2    1    2    ∞    3    ∞    ∞
Queue:    [4, 6]

WAVE 3 continued — Processing node 4 (distance = 2)
Neighbours of 4: {3, 5}
  3 → distance 3 > already 1 → discard
  5 → distance 3 < ∞         → update
─────────────────────────────────────────────────
Node:      0    1    2    3    4    5    6    7    8
Distance:  0    1    2    1    2    3    3    ∞    ∞
Queue:    [6, 5]

WAVE 4 — Processing node 6 (distance = 3)
Neighbours of 6: {2, 5, 7, 8}
  2 → distance 4 > already 2 → discard
  5 → distance 4 > already 3 → discard
  7 → distance 4 < ∞         → update
  8 → distance 4 < ∞         → update
─────────────────────────────────────────────────
Node:      0    1    2    3    4    5    6    7    8
Distance:  0    1    2    1    2    3    3    4    4
Queue:    [5, 7, 8]

WAVE 4 continued — Processing nodes 5, 7, 8
All their neighbours are already reached at shorter distances
→ All discarded
─────────────────────────────────────────────────
Queue becomes EMPTY → BFS terminates

FINAL DISTANCE ARRAY
─────────────────────────────────────────────────
Node:      0    1    2    3    4    5    6    7    8
Distance:  0    1    2    1    2    3    3    4    4
```

---

### The Three Situations You'll Always Encounter

As BFS runs, every neighbour `v` of the current node `u` will fall into exactly one of these three buckets:

```
┌─────────────────────────────────────────────────────────────────┐
│ SITUATION 1: distance[v] == ∞                                   │
│ → First time reaching v                                         │
│ → Update distance[v] = distance[u] + 1                          │
│ → Push v into queue                                             │
│ → This is the shortest path to v (BFS guarantees it)            │
├─────────────────────────────────────────────────────────────────┤
│ SITUATION 2: distance[u] + 1 < distance[v]  (not ∞, but worse)  │
│ → v was reached before, but via a longer path                   │
│ → Update distance[v] = distance[u] + 1                          │
│ → Push v into queue again with better distance                  │
│ → NOTE: In unit weight graphs, this situation almost never      │
│         happens — BFS processes nodes in order, so the first    │
│         time you reach v IS already the shortest.               │
│         But the condition handles it correctly if it does.      │
├─────────────────────────────────────────────────────────────────┤
│ SITUATION 3: distance[u] + 1 >= distance[v]                     │
│ → v was already reached via an equal or shorter path            │
│ → Discard. Do nothing.                                          │
└─────────────────────────────────────────────────────────────────┘
```

---

### The BFS "Wave" Mental Model — Lock This In

The cleanest way to think about this algorithm:

```
SOURCE (distance 0)
    │
    ├──── all direct neighbours (distance 1)
    │         │
    │         ├──── their unvisited neighbours (distance 2)
    │         │             │
    │         │             ├──── their unvisited neighbours (distance 3)
    │         │             │              ...and so on
```

Each wave expands exactly one step outward. Because every step costs exactly +1, the FIRST time BFS reaches any node is always via the shortest path. This is the guarantee that makes the plain queue correct.

---

### Interview Tip the Instructor Gives — Word for Word

> *"Since edge weight increases by exactly +1 at every step, the queue is already in sorted order — source at distance 0, then distance 1, then distance 2, then distance 3. You don't need a sorted data structure. That's why the plain queue works, and you don't need Dijkstra here."*

A clean one-liner to say in interviews:

> **"This is BFS with a greedy compression condition on the distance array — plain queue works because unit weights keep the queue naturally sorted by distance."**

---
## Part 3 — Java Implementation + Complexity Analysis

---

### Full Java Implementation

```java
import java.util.*;

class Solution {
    public int[] shortestPath(int V, int[][] edges, int src) {

        // ─────────────────────────────────────────
        // STEP 1: Build the Adjacency List
        // ─────────────────────────────────────────
        // Why ArrayList<List<Integer>>?
        // Each node can have a variable number of neighbours.
        // An adjacency matrix would waste space for sparse graphs.

        List<List<Integer>> adjList = new ArrayList<>();
        for (int node = 0; node < V; node++) {
            adjList.add(new ArrayList<>());
        }

        // Since the graph is UNDIRECTED, every edge (u, v)
        // must be added in BOTH directions
        for (int[] edge : edges) {
            int u = edge[0];
            int v = edge[1];
            adjList.get(u).add(v);
            adjList.get(v).add(u);   // ← don't forget this for undirected graphs
        }

        // ─────────────────────────────────────────
        // STEP 2: Initialize Distance Array
        // ─────────────────────────────────────────
        // (int) 1e9 is used as ∞ — large enough to never be
        // a real answer, and safe from overflow when you do dist[u] + 1
        // (unlike Integer.MAX_VALUE which overflows on + 1)

        int[] dist = new int[V];
        Arrays.fill(dist, (int) 1e9);

        dist[src] = 0;  // distance from source to itself is always 0

        // ─────────────────────────────────────────
        // STEP 3: Initialize Queue with Source
        // ─────────────────────────────────────────
        // We only push the NODE into the queue (not a pair)
        // because dist[] already tracks the distance separately.
        // No need to store (node, distance) pairs like Dijkstra.

        Queue<Integer> queue = new LinkedList<>();
        queue.add(src);

        // ─────────────────────────────────────────
        // STEP 4: BFS
        // ─────────────────────────────────────────
        while (!queue.isEmpty()) {

            int curr = queue.poll();  // get the front node

            // Visit all neighbours of curr
            for (int neighbour : adjList.get(curr)) {

                // Core condition:
                // Only update if we found a shorter path to neighbour
                if (dist[curr] + 1 < dist[neighbour]) {
                    dist[neighbour] = dist[curr] + 1;
                    queue.add(neighbour);  // push to explore its neighbours later
                }
                // Otherwise discard — neighbour already reached via shorter/equal path
            }
        }

        // ─────────────────────────────────────────
        // STEP 5: Replace ∞ with -1 (in-place)
        // ─────────────────────────────────────────
        // Nodes still at (int) 1e9 were never reached → -1
        // No separate answer[] array needed — reuse dist[] directly

        for (int node = 0; node < V; node++) {
            if (dist[node] == (int) 1e9) {
                dist[node] = -1;
            }
        }

        return dist;
    }

    // ─────────────────────────────────────────
    // Driver — test with the example from the lecture
    // ─────────────────────────────────────────
    public static void main(String[] args) {
        Solution sol = new Solution();

        int[][] edges = {
            {0, 1}, {0, 3}, {1, 2}, {1, 3},
            {2, 6}, {3, 4}, {4, 5}, {5, 6},
            {6, 7}, {6, 8}, {7, 8}
        };

        int V = 9;    // nodes: 0 to 8
        int src = 0;

        int[] result = sol.shortestPath(V, edges, src);

        System.out.println("Shortest distances from node " + src + ":");
        for (int i = 0; i < V; i++) {
            System.out.println("Node " + i + " → " + result[i]);
        }
    }
}
```

---

### Expected Output

```
Shortest distances from node 0:
Node 0 → 0
Node 1 → 1
Node 2 → 2
Node 3 → 1
Node 4 → 2
Node 5 → 3
Node 6 → 3
Node 7 → 4
Node 8 → 4
```

---

### Why No (node, distance) Pair in the Queue?

You might have seen Dijkstra store pairs like `(distance, node)` in a priority queue. This BFS only pushes the **node** alone. Here's why that's safe:

```
In Dijkstra (weighted graph):
  → Queue can have nodes at wildly different distances mixed together
  → You NEED to store distance alongside node
  → So you know which instance of the node you're processing

In BFS (unit weight graph):
  → Queue is naturally sorted by distance (as established in Part 1)
  → When you poll node curr, dist[curr] already holds the correct
    shortest distance
  → No need to carry distance in the queue — dist[] tells you everything
```

---

### Complexity Analysis

```
┌──────────────────────────────────────────────────────────┐
│ TIME COMPLEXITY                                          │
├──────────────────────────────────────────────────────────┤
│ Building adjacency list  →  O(E)  [E = number of edges]  │
│ BFS traversal            →  O(V + 2E)                    │
│   Each node polled once  →  O(V)                         │
│   Each edge visited      →  O(2E) [both directions]      │
│                                                          │
│ Overall: O(V + E)                                        │
└──────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────┐
│ SPACE COMPLEXITY                                         │
├──────────────────────────────────────────────────────────┤
│ Adjacency list  →  O(V + 2E)                             │
│ Distance array  →  O(V)                                  │
│ Queue           →  O(V)  [at most V nodes in queue]      │
│                                                          │
│ Overall: O(V + E)                                        │
└──────────────────────────────────────────────────────────┘
```

---

### The Complete Mental Map — Tie Everything Together

```
PROBLEM TYPE DETECTOR (for unseen problems)
───────────────────────────────────────────────────────────
Is it a graph problem asking for shortest path?
            │
            ▼
Are all edge weights EQUAL (unit or any same value)?
     │                        │
    YES                       NO
     │                        │
     ▼                        ▼
  Use BFS               Are weights non-negative?
(plain Queue)                 │
                             YES
                              │
                              ▼
                         Use Dijkstra
                       (Priority Queue)
───────────────────────────────────────────────────────────

WHY BFS WORKS HERE (one-line anchor):
  Unit weights → queue stays sorted → first reach = shortest reach
  → No priority queue needed → plain BFS is optimal

WHAT MAKES THIS BFS DIFFERENT FROM STANDARD BFS:
  Standard BFS  → visited[] boolean array (just tracks yes/no)
  This BFS      → dist[]   integer array  (tracks shortest distance)
                            ↑
                 also acts as visited check: dist[v] != (int)1e9 means visited
```

---

### Quick Revision Card

```
┌─────────────────────────────────────────────────────────┐
│  SHORTEST PATH — UNIT WEIGHT UNDIRECTED GRAPH           │
├─────────────────────────────────────────────────────────┤
│  Algorithm   : BFS                                      │
│  Queue type  : Plain Queue (not priority queue)         │
│  Visited     : dist[] array initialized to (int) 1e9    │
│  Update cond : dist[curr] + 1 < dist[neighbour]         │
│                → update + push                          │
│  Unreachable : dist[i] still (int) 1e9 → return -1      │
│  Time        : O(V + E)                                 │
│  Space       : O(V + E)                                 │
│                                                         │
│  KEY INSIGHT : unit weight → queue auto-sorted          │
│                → first time BFS reaches v               │
│                  = shortest path to v. Guaranteed.      │
└─────────────────────────────────────────────────────────┘
```

---

That's Part 3 cleaned up. Ready for the next lecture whenever you are!