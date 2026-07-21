# Bellman-Ford Algorithm

Solution: https://www.youtube.com/watch?v=0vVofAhAYjc&ab_channel=takeUforward
Status: Done

# What is Bellman-Ford?

This is again an algorithm that helps you find the **shortest path** — same goal as Dijkstra. So the natural question is: *we already have Dijkstra, why do we need another algorithm for the exact same thing?*

The answer lies in where Dijkstra breaks down.

**Dijkstra fails when the graph has negative edges.** And if the graph has a **negative cycle**, Dijkstra doesn't just give a wrong answer — it goes into a TLE. It keeps running forever, endlessly minimizing the distance, because it has no way of knowing it's stuck in a loop that keeps producing "better" answers.

Bellman-Ford is built to handle exactly this gap:

```
┌────────────────────────────────────────────────────────────────────┐
│  Bellman-Ford finds shortest paths — same as Dijkstra —            │
│  but it ALSO:                                                      │
│                                                                    │
│  1. Works correctly with **negative edge** weights                 │
│  2. DETECTS **negative cycles** (Dijkstra cannot do this at all)   │
│                                                                    │
│  That detection ability is the specialty of Bellman-Ford.          │
└────────────────────────────────────────────────────────────────────┘
```

---

### The One Hard Requirement — Directed Graphs Only

Bellman-Ford is applicable **only on *directed graphs* (DG)**.

This is how a directed edge looks:

![image.png](Bellman-Ford%20Algorithm/image.png)

There is an edge from 1 to 2, and it costs 5.

Now, what if you're given an **undirected** graph — say, an edge between 1 and 2 with weight 5? 

![image.png](Bellman-Ford%20Algorithm/image%201.png)

To run Bellman-Ford on it, you convert that single undirected edge into **two directed edges**, both with the same weight:

![image.png](Bellman-Ford%20Algorithm/image%202.png)

![image.png](Bellman-Ford%20Algorithm/image%203.png)

```
┌────────────────────────────────────────────────────────────────────┐
│  Undirected edge (u, v, wt)                                        │
│         ↓ convert to                                               │
│  Directed edge u → v, weight wt                                    │
│  Directed edge v → u, weight wt                                    │
│                                                                    │
│  This is how you always adapt Bellman-Ford for undirected          │
│  graphs — represent every undirected edge as two directed          │
│  edges going opposite ways with identical weight.                  │
└────────────────────────────────────────────────────────────────────┘
```

---

### What Exactly Is a Negative Cycle?

![image.png](Bellman-Ford%20Algorithm/image%204.png)

Walk the cycle once and add up the weights:

```
-2 + (-1) + 2 = -1
```

The total path weight for going around this cycle is **negative**. That is the definition:

```
┌────────────────────────────────────────────────────────────────────┐
│  If a graph has any cycle whose total path weight is less          │
│  than 0 → that graph has a NEGATIVE CYCLE.                         │
└────────────────────────────────────────────────────────────────────┘
```

If you tried running Dijkstra on this, it would keep circling around this cycle forever — every loop reduces the path weight further (`-1`, then `-2`, then `-3`, ...), so it never converges to a final answer. This is exactly the infinite-loop failure mode mentioned above.

---

## Setting Up the Algorithm

### The Example Graph

Let's use the same directed graph Striver builds this up with. Source = **0**. Edges are given as `(u, v, weight)`:

![image.png](Bellman-Ford%20Algorithm/image%205.png)

**Very important point:** edges can be given in **any order** — it does not matter whether `3 2 6` appears first or last in the list. What matters is that *all* the edges of the graph are present. There is no requirement to process them in any particular sequence.

---

### The Core Rule — What Bellman-Ford States

![image.png](Bellman-Ford%20Algorithm/image%206.png)

> **Relax all the edges, n − 1 times, sequentially.**
> 

Two pieces here that need to be fully unpacked: what does "relax" mean, and why exactly `n - 1`?

---

### What Does "Relax an Edge" Mean?

You have a distance array. For an edge `u → v` with weight `wt`:

```
if (dist[u] + wt) < dist[v]:
    dist[v] = dist[u] + wt
```

In words: *"If the distance I've taken to reach `u`, plus this edge's weight, is smaller than what I currently know for `v` — then I've found a better way to reach `v`. Update it."*

This is the exact same relaxation idea as Dijkstra. Nothing new here conceptually — the difference is entirely in **how many times** and **in what order** you apply it.

---

### Initial Setup

![image.png](Bellman-Ford%20Algorithm/image%207.png)

Since you're computing shortest paths, you need a distance array initialized to infinity everywhere, except the source:

```
dist[] = [0, ∞, ∞, ∞, ∞, ∞]     (source = node 0)
```

Number of nodes `n = 6` (nodes 0 through 5, zero-indexed). So the number of relaxation passes required is `n - 1 = 5`.

```
┌────────────────────────────────────────────────────────────────────┐
│  "First iteration" means: go across EVERY edge in the list,        │
│  one by one, attempting relaxation on each. Once you've gone       │
│  through all of them once, that is ONE complete iteration.         │
│                                                                    │
│  You repeat this entire pass — across all edges again — for        │
│  a total of n − 1 times.                                           │
└────────────────────────────────────────────────────────────────────┘
```

---

### Walking Through Iteration 1

![image.png](Bellman-Ford%20Algorithm/image%208.png)

Going edge by edge, in the given order:

```
Edge (3,2,6):  dist[3] = ∞ → haven't reached 3 yet → nothing to relax, skip
Edge (5,3,1):  dist[5] = ∞ → haven't reached 5 yet → skip
Edge (0,1,5):  dist[0] = 0, so 0+5=5 < dist[1]=∞ → update dist[1] = 5
Edge (1,5,-3): dist[1] = 5, so 5+(-3)=2 < dist[5]=∞ → update dist[5] = 2
Edge (1,2,-2): dist[1] = 5, so 5+(-2)=3 < dist[2]=∞ → update dist[2] = 3
Edge (3,4,-2): dist[3] = ∞ → skip
Edge (2,4,3):  dist[2] = 3, so 3+3=6 < dist[4]=∞ → update dist[4] = 6
```

After iteration 1:

```
dist[] = [0, 5, 3, ∞, 6, 2]
```

Notice something important: within this **single pass**, node 1's freshly-updated distance (5) was immediately used later in the *same* pass to update nodes 5 and 2. Edges don't wait for the next iteration to see updates made earlier in the current one — they use whatever is currently in `dist[]` at the moment they're processed.

---

### Walking Through Iteration 2

![image.png](Bellman-Ford%20Algorithm/image%209.png)

```
Edge (3,2,6):  dist[3] = ∞ → still not reached → skip
Edge (5,3,1):  dist[5] = 2, so 2+1=3 < dist[3]=∞ → update dist[3] = 3
Edge (0,1,5):  0+5=5, not < dist[1]=5 → skip
Edge (1,5,-3): 5+(-3)=2, not < dist[5]=2 → skip
Edge (1,2,-2): 5+(-2)=3, not < dist[2]=3 → skip
Edge (3,4,-2): dist[3] = 3 (just updated above!), so 3+(-2)=1 < dist[4]=6 → update dist[4] = 1
Edge (2,4,3):  3+3=6, not < dist[4]=1 → skip
```

After iteration 2:

```
dist[] = [0, 5, 3, 3, 1, 2]
```

You'd continue this for iterations 3, 4, and 5 — but in this graph, nothing changes further. The array has already converged. After all `n - 1 = 5` iterations, `dist[]` holds the correct shortest distance from the source to every node.

![image.png](Bellman-Ford%20Algorithm/image%2010.png)

---

## The Two Big Questions

### `Question 1` — Why Exactly `n - 1` Iterations?

This is the question most explanations skip over. Let's derive it properly with a clean *chain* example.

![image.png](Bellman-Ford%20Algorithm/image%2011.png)

Take a directed graph with 5 nodes where the edges are deliberately listed in the **worst possible order** — completely backwards from how you'd want to process them:

```
edges given in this order:
3 4 1
2 3 1
1 2 1
0 1 1
```

Source = 0. `n = 5` nodes, so we expect `n - 1 = 4` iterations.

```
dist[] = [0, ∞, ∞, ∞, ∞]
```

**Iteration 1** (go through edges top to bottom, in the given order):

![image.png](Bellman-Ford%20Algorithm/image%2012.png)

```
Edge (3,4,1): dist[3] = ∞ → skip
Edge (2,3,1): dist[2] = ∞ → skip
Edge (1,2,1): dist[1] = ∞ → skip
Edge (0,1,1): dist[0] = 0, 0+1=1 < ∞ → **update dist[1] = 1**
```

Only node 1 got updated. Result: `dist[] = [0, 1, ∞, ∞, ∞]`

**Iteration 2:**

![image.png](Bellman-Ford%20Algorithm/image%2013.png)

```
Edge (3,4,1): dist[3] = ∞ → skip
Edge (2,3,1): dist[2] = ∞ → skip
Edge (1,2,1): dist[1] = 1, 1+1=2 < ∞ → **update dist[2] = 2**
Edge (0,1,1): 0+1=1, not < dist[1]=1 → skip
```

Result: `dist[] = [0, 1, 2, ∞, ∞]`

**Iteration 3:**

![image.png](Bellman-Ford%20Algorithm/image%2014.png)

```
Edge (3,4,1): dist[3] = ∞ → skip
Edge (2,3,1): dist[2] = 2, 2+1=3 < ∞ → **update dist[3] = 3**
```

Result: `dist[] = [0, 1, 2, 3, ∞]`

**Iteration 4:**

![image.png](Bellman-Ford%20Algorithm/image%2015.png)

```
Edge (3,4,1): dist[3] = 3, 3+1=4 < ∞ → **update dist[4] = 4**
```

Result: `dist[] = [0, 1, 2, 3, 4]` — fully converged.

![image.png](Bellman-Ford%20Algorithm/image%2016.png)

```
┌────────────────────────────────────────────────────────────────────┐
│  What just happened, iteration by iteration:                       │
│                                                                    │
│  Iteration 1 → node 0 updated node 1                               │
│  Iteration 2 → node 1 updated node 2                               │
│  Iteration 3 → node 2 updated node 3                               │
│  Iteration 4 → node 3 updated node 4                               │
│                                                                    │
│  Because the edges were given in the WORST possible order,         │
│  each iteration could only propagate the "wave" of correct         │
│  distance ONE hop further along the chain.                         │
│                                                                    │
│  A shortest path in a graph with n nodes can have AT MOST          │
│  n - 1 edges (a simple path visits each node once, so the          │
│  longest possible shortest-path chain has n-1 edges).              │
│                                                                    │
│  In the worst case — edges given in exactly the wrong order —      │
│  it takes exactly one full pass to propagate the correct           │
│  distance across ONE edge of that chain. So to guarantee the       │
│  distance has propagated across a chain of n-1 edges, you          │
│  need n-1 full passes.                                             │
└────────────────────────────────────────────────────────────────────┘
```

This is the entire justification: `n - 1` is not an arbitrary number — it is the worst-case number of hops any shortest path can have, and each full relaxation pass guarantees propagating correct information at least one hop further, no matter how adversarially the edges are ordered.

---

### `Question 2` — How Do You Detect a *Negative Cycle*?

![image.png](Bellman-Ford%20Algorithm/image%2017.png)

Think about what a negative cycle actually does to your distance values: every time you go around it, the path weight keeps decreasing. One loop gives you `-1`, another loop gives you another `-1` on top of that, and so on — it never bottoms out.

Now connect this to what you just proved: after `n - 1` iterations, **every legitimate shortest path (which has at most n-1 edges) is guaranteed to have already converged.** There should be nothing left to relax.

So the detection trick is beautifully simple:

```
┌────────────────────────────────────────────────────────────────────┐
│  Perform ***ONE MORE relaxation*** pass — the **n-th** iteration.  │
│                                                                    │
│  If ANY edge still successfully relaxes during this extra          │
│  pass (i.e., dist[v] can still be reduced) — that is               │
│  IMPOSSIBLE for a graph without negative cycles, because           │
│  we already proved n-1 iterations are always sufficient.           │
│                                                                    │
│  So if a reduction happens on the n-th pass, it can only mean      │
│  the graph contains a NEGATIVE CYCLE, and you're still going       │
│  around it, endlessly finding "smaller" distances.                 │
│                                                                    │
│  → Negative cycle exists → return an array of -1s (or              │
│    whatever the problem specifies)                                 │
└────────────────────────────────────────────────────────────────────┘
```

---

## Complete Java Implementation

```java
class Solution {
    static int[] bellman_ford(int V, ArrayList<ArrayList<Integer>> edges, int S) {

        // **Step 1**: Initialize distance array
        // (int) 1e8 as infinity — Striver uses 1e8 here specifically
        // (matches GFG's convention for this problem; conceptually
        // identical to the (int)1e9 convention used in Dijkstra notes —
        // just needs to be large enough and safe from overflow)
        int[] dist = new int[V];
        Arrays.fill(dist, (int) 1e8);
        dist[S] = 0;

        // **Step 2**: Relax all edges, n - 1 times, sequentially
        for (int i = 0; i < V - 1; i++) {

            // One full pass over every edge
            for (ArrayList<Integer> edge : edges) {
                int u = edge.get(0);
                int v = edge.get(1);
                int wt = edge.get(2);

                // Only relax if u is actually reachable —
                // otherwise dist[u] + wt is meaningless (adding
                // weight on top of "unreached" tells you nothing)
                if (dist[u] != (int) 1e8 && dist[u] + wt < dist[v]) {
                    dist[v] = dist[u] + wt;
                }
            }
        }

        // **Step 3**: The N-th relaxation — negative cycle check
        // If ANY edge can still relax after n-1 guaranteed-sufficient
        // iterations, a negative cycle must be pulling distances down
        for (ArrayList<Integer> edge : edges) {
            int u = edge.get(0);
            int v = edge.get(1);
            int wt = edge.get(2);

            if (dist[u] != (int) 1e8 && dist[u] + wt < dist[v]) {
                return new int[]{-1};
            }
        }

        return dist;
    }
}
```

---

### Why the `dist[u] != (int)1e8` Guard Matters

This check is doing real work, not just being defensive. If `u` hasn't been reached yet, `dist[u]` is still infinity (`1e8`). Adding an edge weight on top of "unreached" (`1e8 + wt`) produces a number that is **still effectively meaningless** — it does not represent any real path, because there is no actual path to `u` yet for this edge to extend. Without this guard, you'd risk accidentally treating a non-existent path as if it were a real (if very long) one, and in graphs with negative weights this could cause incorrect relaxations.

---

## Complexity Analysis

### Time Complexity — O(V × E)

```
┌────────────────────────────────────────────────────────────────────┐
│  Outer loop (relaxation passes)  →  runs V - 1 times               │
│  Inner loop (over all edges)     →  runs E times per pass          │
│                                                                    │
│  Total: O(V - 1) × O(E) = O(V × E)                                 │
│                                                                    │
│  Plus one more full pass (O(E)) for the negative cycle check,      │
│  which doesn't change the asymptotic class.                        │
└────────────────────────────────────────────────────────────────────┘
```

Compare this to Dijkstra's `O(E log V)` — Bellman-Ford is meaningfully slower, effectively quadratic-ish behavior in dense graphs. But it buys you two things Dijkstra cannot offer: correctness with negative edges, and the ability to detect negative cycles at all.

### Space Complexity — O(V + E)

```
dist[] array     → O(V)
edges storage    → O(E)   (stored as given, no adjacency list needed)
─────────────────────────────
Total: O(V + E)
```

Note there's no adjacency list here at all — Bellman-Ford doesn't need one. It just needs the flat list of all edges, since every relaxation pass iterates over that list directly regardless of graph structure.

---

## Dijkstra vs Bellman-Ford — The Complete Comparison

```
┌──────────────────────────┬────────────────────────┬─────────────────────────────┐
│ Aspect                   │ Dijkstra               │ Bellman-Ford                │
├──────────────────────────┼────────────────────────┼─────────────────────────────┤
│ Negative edges           │ Fails                  │ Works correctly             │
│ Negative cycles          │ Infinite loop (**TLE**)│ Detects them explicitly     │
│ Graph type               │ Directed/Undirected    │ Directed only (convert      │
│                          │ both work directly     │ undirected → two edges)     │
│ Core data structure      │ Priority Queue         │ Flat edge list, no PQ       │ 
│ Processing order         │ Greedy (min-dist first)│ Brute-force, fixed order    │
│ Time Complexity          │ O(E log V)             │ O(V × E)                    │
│ When to use              │ Non-negative weights,  │ Negative weights present,   │
│                          │ want speed             │ or need cycle detection     │
└──────────────────────────┴────────────────────────┴─────────────────────────────┘
```

---

## Quick Revision Checklist

- [ ]  Why does Dijkstra fail on graphs with negative cycles specifically (not just negative edges)?
- [ ]  How do you adapt Bellman-Ford for an undirected graph?
- [ ]  What is the precise definition of "relaxing an edge"?
- [ ]  Why exactly `n - 1` iterations — what's the worst-case argument involving edge ordering?
- [ ]  Why does a successful relaxation on the n-th pass prove a negative cycle exists?
- [ ]  Why does the code check `dist[u] != (int)1e8` before attempting relaxation?
- [ ]  Why is there no adjacency list in this algorithm, unlike every Dijkstra implementation so far?
- [ ]  What is the time complexity, and why is it worse than Dijkstra's?

---