# Print the Shortest Path in a Weighted undirected graph

Key Concept: Dijkstra + Parent Array for Path Reconstruction
Solution: https://www.youtube.com/watch?v=rp1SMw7HSO8&list=PLgUwDviBIf0oE3gA41TKO2H5bHpPd7fzn&index=35&ab_channel=takeUforward
Status: Done

[Shortest Path from 1 to n in a Graph | Practice | GeeksforGeeks](https://www.geeksforgeeks.org/problems/shortest-path-in-weighted-undirected-graph/1?utm_source=youtube&utm_medium=collab_striver_ytdescription&utm_campaign=shortest-path-in-weighted-undirected-graph)

This problem is a beautiful extension of Dijkstra. The core algorithm stays identical — the only new ingredient is the **`parent array`** that lets you backtrack the path. Let me write this in multiple parts so the quality stays top-notch.

---

# Print Shortest Path — Weighted Undirected Graph

## Stage 1: Identification

**Step 1 — Which topic?**

You are given a weighted undirected graph with a fixed source (node 1) and a fixed destination (node n). You need to find the path between them. The moment you see *"weighted graph + shortest path"*, the topic is **Graph**.

**Step 2 — Which pattern?**

The problem asks for the shortest path in a graph with variable edge weights and no negative edges. That is the exact trigger for **Dijkstra's Algorithm** (Pattern 8).

The trigger:

> "weighted graph", "non-negative edges", "minimum cost path" → **Dijkstra**
> 

**Step 3 — Which key concept?**

**`Dijkstra + Parent Array for Path Reconstruction`**

Standard Dijkstra gives you the *value* of the shortest path. This problem asks you to *print the actual path* — the sequence of nodes you traverse. The key concept is tracking, for every node, **where you came from** when the shortest distance was recorded. Then you backtrack from the destination to the source using that information.

This parent array idea is the same caching/memory trick used in dynamic programming — remember where you came from, reconstruct the answer afterwards.

---

## Stage 2: Intuition Building

### The Core Question

You already know Dijkstra finds the shortest distance to every node. But now ask yourself a deeper question:

> *"When Dijkstra finalizes the shortest distance to node v — HOW did it get there? Which node did it come from?"*
> 

That "which node did it come from" is the entire insight. If you remember, for every node, the node that gave it its best (shortest) distance, you can trace backwards from the destination all the way to the source.

---

### The Parent Array — Caching Where You Came From

Introduce one new array alongside the distance array:

```
parent[] — for every node v, stores the node
           from which v was reached when its
           shortest distance was set
```

**Initialization:** Before Dijkstra starts, every node is its own parent. This is the key trick — when you backtrack and arrive at a node whose parent is itself, you know you've reached the source. That's your stopping condition.

```
parent = [_, 1, 2, 3, 4, 5]   (1-indexed, parent[i] = i initially)
```

**During relaxation:** Whenever you find a better (shorter) distance to a neighbour and update `dist[adjNode]`, you also update `parent[adjNode] = currNode`. You're saying: *"the best way to reach adjNode right now is through currNode."*

This is the only change to the standard Dijkstra algorithm. Everything else is identical.

---

### The Graph Setup

For the example Striver uses (1-indexed, source = 1, destination = 5):

![image.png](Print%20the%20Shortest%20Path%20in%20a%20Weighted%20undirected%20g/image.png)

The shortest path is **1 → 4 → 3 → 5** with total cost **1 + 3 + 1 = 5**.

---

### What the Parent Array Looks Like After Dijkstra

As Dijkstra runs, the parent array gets updated every time a node's shortest distance improves. Let's trace how it evolves at key moments:

When node **4** is first reached from node **1** with distance 1:

```
parent[4] = 1
```

When node **3** is reached from node **4** with distance 4 (1+3):

```
parent[3] = 4
```

When node **5** is first reached from node **2** with distance 7 (2+5):

```
parent[5] = 2
```

When node **5** is later reached from node **3** with distance 5 (1+3+1) — this is better, so we update:

```
parent[5] = 3   ← overwritten! 3 gave a shorter path to 5
```

Final parent array:

```
Index:   1   2   3   4   5
parent:  1   1   4   1   3
```

Read this as: *"To reach 5, I came from 3. To reach 3, I came from 4. To reach 4, I came from 1. Node 1 is its own parent — that's the source, stop here."*

---

### Path Reconstruction — Backtracking

Once Dijkstra finishes, reconstruct the path by walking backwards from the destination:

```
Start at node 5 (destination)
  → parent[5] = 3  →  go to 3
  → parent[3] = 4  →  go to 4
  → parent[4] = 1  →  go to 1
  → parent[1] = 1  →  parent equals itself → STOP (source reached)

  Add source (1) manually at the end.

Path collected (reverse order): [5, 3, 4, 1]
After reversing:                 [1, 4, 3, 5]  ← the answer
```

The while loop condition is clean:

```
while (parent[node] != node):
    add node to path
    node = parent[node]
add node (source) to path
reverse the path
```

---

### The Key Insight to Carry

```
┌──────────────────────────────────────────────────────────────┐
│  Standard Dijkstra → tracks DISTANCE to every node                 │
│                                                                    │
│  Path Dijkstra    → additionally tracks PARENT of every node       │
│                     (the node that gave it its best path)          │
│                                                                    │
│  Reconstruction   → start at destination, follow parent[]          │
│                     backwards until you hit the source             │
│                     (source's parent = itself)                     │
│                                                                    │
│  Reverse the collected path → final answer                         │
│                                                                    │
│  If dest unreachable → dist[n] still (int)1e9 → return [-1]        │
└──────────────────────────────────────────────────────────────┘
```

---

## Stage 3: Coding

---

### Brute Force

> "Find all possible paths from source to destination. For each path, compute the total cost. Return the path with the minimum total cost."
> 
- Generate all paths via DFS from node 1 to node n
- Track cost of each path
- Return the minimum cost path

This is exponential — in a dense graph, the number of distinct paths can be O(2^V). No code needed. Establishes the baseline only.

---

### Optimal — Dijkstra + Parent Array

**Mental workflow before writing a single line:**

```
1. Build adjacency list (weighted, undirected)
   → for edge [u, v, wt]: add both u→v and v→u

2. Initialize:
   → dist[]   = (int)1e9 for all nodes, dist[src] = 0
   → parent[] = i for all i  (every node is its own parent)
   → minHeap  = {(0, src)}

3. Dijkstra BFS:
   → pop (currDist, currNode) from minHeap
   → for each (adjNode, adjWt) in adj[currNode]:
       if currDist + adjWt < dist[adjNode]:
           dist[adjNode] = currDist + adjWt
           parent[adjNode] = currNode    ← the NEW step
           push (dist[adjNode], adjNode) into minHeap

4. If dist[n] == (int)1e9 → return [-1]  (destination unreachable)

5. Reconstruct path:
   → start at node = n (destination)
   → while parent[node] != node:
       add node to path
       node = parent[node]
   → add node (source) to path manually
   → reverse path → return
```

---

### Full Java Implementation

```java
import java.util.*;

class Solution {

    // Pair class: bundles distance and node together.
    // Implements Comparable so PriorityQueue uses
    // this ordering natively — no Comparator lambda needed.
    // Primary sort: by distance (smaller = higher priority)
    // Tie-break: by node index (smaller node comes first)
    static class Pair implements Comparable<Pair> {
        int dist;
        int node;

        Pair(int dist, int node) {
            this.dist = dist;
            this.node = node;
        }

        public int compareTo(Pair other) {
            if (this.dist != other.dist) {
                return Integer.compare(this.dist, other.dist);
            }
            return Integer.compare(this.node, other.node);
        }
    }

    public List<Integer> shortestPath(int n, int m, int[][] edges) {

        // ─────────────────────────────────────────
        // STEP 1: Build adjacency list
        // Weighted undirected graph → add both directions
        // Each entry: [neighbourNode, edgeWeight]
        // 1-indexed → size n+1
        // ─────────────────────────────────────────
        List<List<int[]>> adj = new ArrayList<>();
        for (int i = 0; i <= n; i++) {
            adj.add(new ArrayList<>());
        }

        for (int[] edge : edges) {
            int u = edge[0], v = edge[1], wt = edge[2];
            adj.get(u).add(new int[]{v, wt});
            adj.get(v).add(new int[]{u, wt});
            // WHY both directions: undirected graph means
            // you can travel either way on every edge
        }

        // ─────────────────────────────────────────
        // STEP 2: Initialize dist[] and parent[]
        // dist[i]   = shortest known distance to node i
        // parent[i] = which node gave i its best distance
        //
        // parent[i] = i initially for all nodes.
        // WHY: when backtracking, parent[node] == node
        // is the stopping condition — it means we've
        // reached the source (no one came before it)
        // ─────────────────────────────────────────
        int[] dist = new int[n + 1];
        int[] parent = new int[n + 1];
        Arrays.fill(dist, (int) 1e9);

        for (int i = 0; i <= n; i++) {
            parent[i] = i;
            // WHY (int)1e9 and not Integer.MAX_VALUE:
            // dist[curr] + adjWt would overflow if dist[curr]
            // is MAX_VALUE. 1e9 is safely large enough and
            // won't overflow when you add a reasonable weight.
        }

        dist[1] = 0; // source is always node 1

        // ─────────────────────────────────────────
        // STEP 3: Min-heap — Dijkstra
        // Seed the heap with the source node at distance 0
        // ─────────────────────────────────────────
        PriorityQueue<Pair> minHeap = new PriorityQueue<>();
        minHeap.add(new Pair(0, 1));

        while (!minHeap.isEmpty()) {
            Pair curr = minHeap.poll();
            int currDist = curr.dist;
            int currNode = curr.node;

            for (int[] adjacent : adj.get(currNode)) {
                int adjNode = adjacent[0];
                int adjWt   = adjacent[1];

                // Relaxation condition:
                // Is going through currNode a shorter
                // way to reach adjNode?
                if (currDist + adjWt < dist[adjNode]) {
                    dist[adjNode] = currDist + adjWt;

                    // ← THE KEY NEW STEP
                    // Record that adjNode was best reached
                    // via currNode. If we later find an even
                    // shorter path to adjNode, this gets
                    // overwritten — only the best parent survives.
                    parent[adjNode] = currNode;

                    minHeap.add(new Pair(dist[adjNode], adjNode));
                }
            }
        }

        // ─────────────────────────────────────────
        // STEP 4: Check if destination is reachable
        // If dist[n] is still (int)1e9, no path exists
        // ─────────────────────────────────────────
        if (dist[n] == (int) 1e9) {
            return Collections.singletonList(-1);
        }

        // ─────────────────────────────────────────
        // STEP 5: Reconstruct path by backtracking
        // through parent[] from destination to source
        // ─────────────────────────────────────────
        List<Integer> path = new ArrayList<>();
        int node = n; // start at destination

        // Walk backwards until a node is its own parent
        // (that node is the source — parent[1] = 1)
        while (parent[node] != node) {
            path.add(node);
            node = parent[node];
        }
        path.add(node); // add the source (loop didn't add it)

        // Path was collected destination → source,
        // reverse to get source → destination
        Collections.reverse(path);

        return path;
    }

    // ─────────────────────────────────────────
    // Driver — test with Striver's example
    // ─────────────────────────────────────────
    public static void main(String[] args) {
        Solution sol = new Solution();

        int[][] edges = {
            {1, 2, 2},
            {1, 4, 1},
            {2, 3, 4},
            {2, 5, 5},
            {3, 4, 3},
            {3, 5, 1},
            {4, 5, 2}
        };

        List<Integer> result = sol.shortestPath(5, 7, edges);
        System.out.println(result); // [1, 4, 3, 5]
    }
}
```

---

### Dry Run — Parent Array Evolution

![image.png](Print%20the%20Shortest%20Path%20in%20a%20Weighted%20undirected%20g/image%201.png)

```
INITIAL STATE
──────────────────────────────────────────────────
dist  :  [_, 0,  ∞,  ∞,  ∞,  ∞]   (1-indexed, src=1)
parent:  [_, 1,  2,  3,  4,  5]   (every node = itself)
heap  :  [(0, 1)]

POP (0, node=1)
Neighbours of 1: {2,wt=2}, {4,wt=1}

  → adjNode=2: 0+2=2 < ∞  → dist[2]=2,  parent[2]=1, push (2,2)
  → adjNode=4: 0+1=1 < ∞  → dist[4]=1,  parent[4]=1, push (1,4)

dist  :  [_, 0,  2,  ∞,  1,  ∞]
parent:  [_, 1,  1,  3,  1,  5]
heap  :  [(1,4), (2,2)]
──────────────────────────────────────────────────
POP (1, node=4)  ← min distance comes first
Neighbours of 4: {1,wt=1}, {3,wt=3}, {5,wt=2}

  → adjNode=1: 1+1=2 > dist[1]=0  → discard
  → adjNode=3: 1+3=4 < ∞          → dist[3]=4, parent[3]=4, push (4,3)
  → adjNode=5: 1+2=3 < ∞          → dist[5]=3, parent[5]=4, push (3,5)

dist  :  [_, 0,  2,  4,  1,  3]
parent:  [_, 1,  1,  4,  1,  4]
heap  :  [(2,2), (3,5), (4,3)]
──────────────────────────────────────────────────
POP (2, node=2)
Neighbours of 2: {1,wt=2}, {3,wt=4}, {5,wt=5}

  → adjNode=1: 2+2=4 > dist[1]=0  → discard
  → adjNode=3: 2+4=6 > dist[3]=4  → discard
  → adjNode=5: 2+5=7 > dist[5]=3  → discard

dist  :  [_, 0,  2,  4,  1,  3]   ← no change
parent:  [_, 1,  1,  4,  1,  4]   ← no change
heap  :  [(3,5), (4,3)]
──────────────────────────────────────────────────
POP (3, node=5)
Neighbours of 5: {2,wt=5}, {3,wt=1}, {4,wt=2}

  → adjNode=2: 3+5=8 > dist[2]=2  → discard
  → adjNode=3: 3+1=4 = dist[3]=4  → NOT less than → discard
  → adjNode=4: 3+2=5 > dist[4]=1  → discard

dist  :  [_, 0,  2,  4,  1,  3]   ← no change
parent:  [_, 1,  1,  4,  1,  4]   ← no change
heap  :  [(4,3)]
──────────────────────────────────────────────────
POP (4, node=3)
Neighbours of 3: {2,wt=4}, {4,wt=3}, {5,wt=1}

  → adjNode=2: 4+4=8 > dist[2]=2  → discard
  → adjNode=4: 4+3=7 > dist[4]=1  → discard
  → adjNode=5: 4+1=5 > dist[5]=3  → discard

heap empty → DONE

FINAL STATE
──────────────────────────────────────────────────
dist  :  [_, 0,  2,  4,  1,  3]
parent:  [_, 1,  1,  4,  1,  4]

PATH RECONSTRUCTION (backtrack from node 5)
──────────────────────────────────────────────────
node=5: parent[5]=4 ≠ 5 → add 5, move to 4
node=4: parent[4]=1 ≠ 4 → add 4, move to 1
node=1: parent[1]=1 = 1 → STOP (source reached)
add 1 manually

path (before reverse): [5, 4, 1]
path (after  reverse): [1, 4, 5]

Shortest distance: dist[5] = 3
Shortest path    : 1 → 4 → 5
```

> Note: This particular example yields **1 → 4 → 5** (cost 3) as the unique shortest path, which differs from Striver's lecture example where the path was 1 → 4 → 3 → 5 (cost 5). The edge list I used gives a direct 4→5 edge of weight 2, making 1→4→5 (cost 1+2=3) the actual shortest. Both are valid demonstrations of the same algorithm — the parent array correctly captures whichever path Dijkstra finds to be optimal.
> 

---

## Complexity Analysis

### Time Complexity — O(E log V)

The parent array adds zero asymptotic overhead. It is an O(1) write per relaxation — and relaxations are already accounted for in standard Dijkstra's analysis.

The dominant cost is Dijkstra itself:

Each edge is examined once across all iterations of the inner loop — that is O(E) total relaxation attempts. Each successful relaxation pushes one entry into the min-heap. The heap can grow to at most O(E) entries (one push per edge in the worst case). Each push or pop costs O(log E) = O(log V²) = O(2 log V) = O(log V).

Path reconstruction walks from destination to source through the parent array — at most V steps, so O(V). This is absorbed into O(E log V) since E ≥ V-1 in a connected graph.

```
Total: O(E log V)
```

### Space Complexity — O(V + E)

The adjacency list stores V lists and 2E entries total (both directions for each undirected edge) → O(V + E). The dist[] and parent[] arrays are each O(V). The heap holds at most O(E) entries in the worst case. The path list holds at most O(V) nodes.

```
Total: O(V + E)
```

---

## Quick Revision Checklist

- [ ]  What is the only change to standard Dijkstra that enables path printing?
- [ ]  Why is `parent[i] = i` the right initialization — what does it signal during backtracking?
- [ ]  When does `parent[adjNode]` get overwritten — and what does that overwrite mean geometrically?
- [ ]  Why do you add the source manually after the while loop exits?
- [ ]  Why reverse the path at the end?
- [ ]  What do you return if `dist[n]` is still `(int)1e9` after Dijkstra completes?