# Concept: Djisktra's Algorithm

Solution: https://www.youtube.com/watch?v=V6H1qAeB-l4
Status: Done

[Dijkstra Algorithm | Practice | GeeksforGeeks](https://www.geeksforgeeks.org/problems/implementing-dijkstra-set-1-adjacency-matrix/1)

# 🚄**Dijkstra's Algorithm using Priority Queue**

---

[G-32. Dijkstra's Algorithm - Using Priority Queue - C++ and Java - Part 1](https://www.youtube.com/watch?v=V6H1qAeB-l4)

### What is Dijkstra's Algorithm?

You are given a **weighted directed or undirected graph** and a **source node**. The task is simple:

> Find the shortest distance from the source node to **every other node** in the graph.
> 

Not just one destination — every node. And "shortest" means the path whose **total edge weight sum is minimum**.

---

### The Example Graph

![image.png](Concept%20Djisktra's%20Algorithm/image.png)

Source node = **0**

---

### Why Can't We Reuse What We Already Know?

We already have two algorithms for shortest paths:

**`Plain BFS`** — works perfectly, but only for ***unit weight* graphs** (all edges cost exactly 1). The reason BFS works there is that the queue stays naturally *sorted* by distance. Every neighbour is always exactly +1 away.

The moment edge weights differ — say one edge costs 1 and another costs 10 — BFS breaks. A node reached in 2 hops via cheap edges might be closer than a node reached in 1 hop via an expensive edge. BFS has no way to account for this.

**`DAG Shortest Path` (Topo Sort + Edge Relaxation)** — works for weighted graphs, but **only on DAGs** (Directed Acyclic Graphs). Real graphs often have cycles.

So we need something that handles:

- **Variable edge weights** (not unit)
- **Graphs with cycles**
- **Non-negative weights** (we will come back to this constraint)

That is exactly what Dijkstra solves.

---

### The Core Idea — Greedy, Not Exhaustive

Before touching any data structure, ask yourself:

> *"If I'm standing at the source and I want to find the shortest distance to every node — what is the safest first move?"*
> 

The answer: **go to the node you can reach with the least cost right now.**

Why is this safe? Because all edge weights are non-negative. Once you've confirmed the shortest distance to a node, no future path can make it shorter — any additional edges can only add more weight, never subtract.

This greedy principle is the entire soul of Dijkstra's algorithm:

```
┌──────────────────────────────────────────────────────────────┐
│  Always process the node with the smallest known distance    │
│  first. Once processed, that distance is FINAL.              │
│                                                              │
│  This works ONLY because all edge weights are non-negative.  │
│  A negative edge could always create a surprise shorter path │
│  discovered later — breaking the guarantee.                  │
└──────────────────────────────────────────────────────────────┘
```

---

## The Initial Setup + Data Structures

---

### What Data Structures Do We Need and Why?

Before writing a single line of code, let's derive each data structure from first principles. Nothing should be memorized — everything should make sense.

---

### Data Structure 1 — The Distance Array

We need to track the **shortest known distance** from the source to every node.

Start by assuming the worst — you know nothing. So initialize every node's distance to **infinity** (meaning "not yet reached").

```
dist[] = [∞, ∞, ∞, ∞, ∞, ∞]   for nodes 0 to 5
```

Then set the source node's distance to 0 — you're already there, it costs nothing.

```
dist[] = [0, ∞, ∞, ∞, ∞, ∞]   source = 0
```

**Why `(int) 1e9` instead of `Integer.MAX_VALUE`?**

When you do `dist[curr] + adjWt`, if `dist[curr]` is `Integer.MAX_VALUE`, adding any positive number causes **integer overflow** and wraps around to a negative value. The comparison `dist[curr] + adjWt < dist[adjNode]` then gives a wrong result.

`(int) 1e9` is large enough to represent "unreachable" for any real graph, and safe from overflow when you add a reasonable edge weight on top.

```
┌──────────────────────────────────────────────────────────────┐
│  dist[] serves two purposes:                                 │
│  1. Tracks shortest known distance to each node              │
│  2. Acts as a **"visited"** check — dist[v] != 1e9 means     │
│     someone already reached v                                │
└──────────────────────────────────────────────────────────────┘
```

---

### Data Structure 2 — The Min-Heap (Priority Queue)

Remember the core greedy principle from Step 1:

> Always process the node with the **smallest known distance** first.
> 

So you need a data structure that always gives you the minimum distance node at the top. That is a **min-heap** — also called a **priority queue** with minimum ordering.

You store pairs of `(distance, node)` in it — not just the node alone. Why?

Because when you pull something out of the heap, you need to know **both** which node you're at AND how much distance you took to get there. The distance in the heap might be *stale* (a better path was found later), so you need it to validate before processing.

**Why (distance, node) and not (node, distance)?**

The heap orders by the **first field**. You want ordering by distance, so distance must come first.

---

### The Custom Pair Class

Rather than using `int[]` arrays inside the heap (which require a `Comparator` and are harder to read), we define a clean `Pair` class that knows how to compare itself:

```java
class Pair implements Comparable<Pair> {
    int dist;
    int node;

    Pair(int dist, int node) {
        this.dist = dist;
        this.node = node;
    }

    public int compareTo(Pair other) {
        if (this.dist != other.dist) {
            return Integer.compare(this.dist, other.dist);
        } else {
            return Integer.compare(this.node, other.node);
        }
    }
}
```

Two things this does:

- **Primary sort by distance** — smaller distance comes out first (min-heap behavior)
- **Tie-break by node index** — when two pairs have equal distance, the one with the smaller node index comes out first. This keeps behavior deterministic and matches what Striver expects.

When you declare `PriorityQueue<Pair> minHeap = new PriorityQueue<>()`, Java uses this `compareTo` automatically. No `Comparator` lambda needed.

---

### Data Structure 3 — The Adjacency List

Since edges are weighted, storing just the neighbour node is not enough. For every neighbour, you also need the edge weight. So each entry in the adjacency list is a pair of `(neighbourNode, edgeWeight)`.

For the GFG format, this looks like:

```
adj.get(u) → [[v1, w1], [v2, w2], ...]
```

Each inner list has exactly two elements: index 0 is the neighbour node, index 1 is the edge weight.

---

### Initial Configuration — The Starting State

```
Source = 0

dist[]  = [0, ∞, ∞, ∞, ∞, ∞]
           ↑
           dist[0] = 0, everything else = (int)1e9

minHeap = [(0, 0)]
           ↑
           (distance=0, node=0) — the source pair
```

This is always the same regardless of the graph. Two lines before BFS begins:

```java
dist[src] = 0;
minHeap.add(new Pair(0, src));
```

```
┌──────────────────────────────────────────────────────────────┐
│  INITIAL CONFIGURATION SUMMARY                               │
│                                                              │
│  dist[]   → all (int)1e9, except dist[src] = 0               │
│  minHeap  → contains exactly one entry: Pair(0, src)         │
│                                                              │
│  This is identical for every Dijkstra problem.               │
│  The only thing that changes is the adjacency list.          │
└──────────────────────────────────────────────────────────────┘
```

---

## The Algorithm Walkthrough + Edge Relaxation Logic

---

### The Edge Relaxation Condition — The Heart of Dijkstra

Before walking through the algorithm, nail down this one condition. Everything else is just bookkeeping around it.

You are standing at node `curr`, reached at distance `dist[curr]`. You are looking at a neighbour `adjNode` connected by an edge of weight `adjWt`. You ask yourself:

> *"Is going through `curr` a shorter way to reach `adjNode` than what I currently know?"*
> 

```
if (dist[curr] + adjWt < dist[adjNode]):
    → YES — found a shorter path
    → Update dist[adjNode] = dist[curr] + adjWt
    → Push Pair(dist[adjNode], adjNode) into minHeap

else:
    → NO — adjNode already reachable via a shorter or equal path
    → Discard. Do nothing.
```

This condition is called **edge relaxation**. Every time you relax an edge successfully, you are saying: *"I found a better way to reach this node — let me record it and explore further from there."*

```
┌──────────────────────────────────────────────────────────────┐
│  RELAXATION = "Can I reach adjNode cheaper via curr?"        │
│                                                              │
│  dist[curr] + adjWt < dist[adjNode]                          │
│       → update dist[adjNode]                                 │
│       → push into minHeap for future exploration             │
│                                                              │
│  The minHeap ensures you always explore the cheapest         │
│  known path first — greedy, not exhaustive.                  │
└──────────────────────────────────────────────────────────────┘
```

---

### The Full Algorithm Workflow

![image.png](Concept%20Djisktra's%20Algorithm/image.png)

```
INITIAL STATE
─────────────────────────────────────────────────────
dist[]  = [0, ∞, ∞, ∞, ∞, ∞]    (source = node 0)
minHeap = [(0, 0)]

STEP 1: Pop (0, node=0) from minHeap
currDist = 0, currNode = 0

Neighbours of 0:
  → node 1, adjWt = 4 : 0+4=4  < ∞  → dist[1]=4,  push (4,1)
  → node 2, adjWt = 4 : 0+4=4  < ∞  → dist[2]=4,  push (4,2)

dist[]  = [0, 4, 4, ∞, ∞, ∞]
minHeap = [(4,1), (4,2)]
─────────────────────────────────────────────────────
STEP 2: Pop (4, node=1) from minHeap
  (4 < 4 tie broken by node index, so node 1 comes first)
currDist = 4, currNode = 1

Neighbours of 1:
  → node 0, adjWt=4 : 4+4=8  > dist[0]=0  → discard
  → node 2, adjWt=2 : 4+2=6  > dist[2]=4  → discard
  → node 3, adjWt=4 : 4+4=8  < ∞          → dist[3]=8, push (8,3)

dist[]  = [0, 4, 4, 8, ∞, ∞]
minHeap = [(4,2), (8,3)]
─────────────────────────────────────────────────────
STEP 3: Pop (4, node=2) from minHeap
currDist = 4, currNode = 2

Neighbours of 2:
  → node 0, adjWt=4 : 4+4=8  > dist[0]=0  → discard
  → node 3, adjWt=3 : 4+3=7  < dist[3]=8  → dist[3]=7, push (7,3)
  → node 4, adjWt=1 : 4+1=5  < ∞          → dist[4]=5, push (5,4)
  → node 5, adjWt=6 : 4+6=10 < ∞          → dist[5]=10, push (10,5)

dist[]  = [0, 4, 4, 7, 5, 10]
minHeap = [(5,4), (7,3), (8,3), (10,5)]
─────────────────────────────────────────────────────
STEP 4: Pop (5, node=4) from minHeap
currDist = 5, currNode = 4

Neighbours of 4:
  → node 3, adjWt=2 : 5+2=7  = dist[3]=7  → NOT less than → discard
  → node 5, adjWt=3 : 5+3=8  < dist[5]=10 → dist[5]=8, push (8,5)

dist[]  = [0, 4, 4, 7, 5, 8]
minHeap = [(7,3), (8,3), (8,5), (10,5)]
─────────────────────────────────────────────────────
STEP 5: Pop (7, node=3) from minHeap
currDist = 7, currNode = 3

Neighbours of 3:
  → node 4, adjWt=2 : 7+2=9  > dist[4]=5  → discard
  → node 5, adjWt=1 : 7+1=8  = dist[5]=8  → NOT less than → discard

dist[]  = [0, 4, 4, 7, 5, 8]   ← no change
minHeap = [(8,3), (8,5), (10,5)]
─────────────────────────────────────────────────────
STEP 6: Pop (8, node=3) from minHeap
  → This is a STALE entry. dist[3] is already 7, not 8.
  → 8 > dist[3]=7 → skip all neighbours, nothing will improve

STEP 7: Pop (8, node=5) from minHeap
currDist = 8, currNode = 5
  → node 2, adjWt=6: 8+6=14 > dist[2]=4 → discard
  → node 3, adjWt=2: 8+2=10 > dist[3]=7 → discard
  → node 4, adjWt=3: 8+3=11 > dist[4]=5 → discard

STEP 8: Pop (10, node=5) from minHeap
  → STALE. dist[5] is already 8, not 10. Skip.

minHeap empty → DONE
─────────────────────────────────────────────────────
FINAL ANSWER
dist[] = [0, 4, 4, 7, 5, 8]
```

---

### The Stale Entry Problem — and Why It's Harmless

Notice in Step 6 and Step 8 above — the heap contained `(8,3)` and `(10,5)` even after better distances were already found for those nodes. These are **stale entries**.

Why do they exist? Because the Priority Queue has no `update` operation. When you find a better distance for node `v`, you cannot modify the old `(oldDist, v)` entry already sitting inside the heap. So you just push a new `(newDist, v)` entry, and the old one becomes stale.

Why are they harmless? When a stale entry `(staleDist, v)` is eventually popped:

```
staleDist > dist[v]   (because dist[v] was already updated to something better)
```

So for every neighbour `adjNode`:

```
staleDist + adjWt > dist[v] + adjWt
```

The relaxation condition `dist[curr] + adjWt < dist[adjNode]` will never be satisfied — every attempt to relax will fail and get discarded. The stale entry does zero damage and costs very little time.

```
┌──────────────────────────────────────────────────────────────┐
│  ⚡**STALE ENTRY DETECTION**                                  │
│                                                              │
│  When you pop (currDist, currNode):                          │
│  → Some implementations explicitly check:                    │
│    **if (currDist > dist[currNode]) continue;**              │
│  → Even without this check, stale entries are harmless       │
│    because their relaxation attempts always fail             │
│                                                              │
│  The Set-based implementation avoids stale entries           │
│  entirely by erasing old entries before inserting new ones.  │
│  (Covered in the Set section below)                          │
└──────────────────────────────────────────────────────────────┘
```

### Complete Implementation Using Priority Queue

```java
class Pair implements Comparable<Pair> {
    int dist;
    int node;

    Pair(int dist, int node) {
        this.dist = dist;
        this.node = node;
    }

    public int compareTo(Pair other) {
        if (this.dist != other.dist) {
            return Integer.compare(this.dist, other.dist);
        } else {
            return Integer.compare(this.node, other.node);
        }
    }
}

class Solution {
    static int[] dijkstra(int V,
                          ArrayList<ArrayList<ArrayList<Integer>>> adj,
                          int S) {

        // Step 1: Initialize distance array
        int[] dist = new int[V];
        Arrays.fill(dist, (int) 1e9);
        dist[S] = 0;

        // Step 2: Min-heap — smallest (dist, node) at top
        PriorityQueue<Pair> minHeap = new PriorityQueue<>();
        minHeap.add(new Pair(0, S));

        // Step 3: Process until heap is empty
        while (!minHeap.isEmpty()) {
            Pair curr = minHeap.poll();
            int currNode = curr.node;
            int currDist = curr.dist;

            for (ArrayList<Integer> adjacent : adj.get(currNode)) {
                int adjNode = adjacent.get(0);
                int adjWt   = adjacent.get(1);

                // Relaxation condition - dist via curr node
                if (currDist + adjWt < dist[adjNode]) {
                    dist[adjNode] = currDist + adjWt;
                    minHeap.add(new Pair(dist[adjNode], adjNode));
                }
            }
        }

        return dist;
    }
}
```

---

## 🚨Why Negative Weights Break Everything

Striver demonstrates this with a clean two-node example. Let's understand it from first principles.

Suppose you have:

![image.png](Concept%20Djisktra's%20Algorithm/image%201.png)

Two nodes, each connected to the other with an edge weight **-2**.

![image.png](Concept%20Djisktra's%20Algorithm/image%202.png)

What does "shortest path from 0 to 1" mean here?

- Go directly: cost = -2
- Go 0→1→0→1: cost = -2 + (-2) + (-2) = -6
- Go around the cycle 10 times: cost = -20

The distance keeps ***decreasing forever***. There is no minimum. The algorithm loops infinitely, chasing a smaller and smaller distance that never bottoms out.

This is not a flaw in Dijkstra — it's a flaw in the problem itself. The concept of "shortest path" becomes undefined when you can cycle through negative edges and reduce cost indefinitely.

```
┌─────────────────────────────────────────────────────────────────┐
│  Dijkstra requires non-negative edge weights.                   │
│                                                                 │
│  ❌ **Negative weights** → possible infinite loop             │
│  ❌ **Negative weight cycles** → "shortest path" is undefined │
│                                                                 │
│  For graphs with negative weights: use **Bellman-Ford** instead │
└─────────────────────────────────────────────────────────────────┘
```

---

# 🏖️**Dijkstra's Algorithm** Using Set

---

[G-33. Dijkstra's Algorithm - Using Set - Part 2](https://www.youtube.com/watch?v=PATgNiuTP20)

### The Problem With Priority Queue — Stale Entries

In Step 3, we saw that when a better distance is found for a node, the old `(staleDist, node)` entry sits inside the heap doing nothing useful — it just waits until it gets popped, fails all relaxation checks, and gets discarded.

For most graphs this is fine. But in a dense graph with many updates, the heap can accumulate a large number of stale entries. They don't cause wrong answers, but they cost time.

The question Striver asks is:

> *"What if instead of letting stale entries pile up, we actively removed them the moment we found a better path?"*
> 

That is exactly what the **Set-based implementation** does.

---

### Why Set?

A `TreeSet` in Java (backed by a Red-Black tree) has two properties that make it useful here:

- It stores entries in ***ascending sorted order*** — so the smallest `(distance, node)` is always at the front, just like a min-heap
- It supports ***O(log n) removal* of any specific entry** — you can find and erase an old stale entry before inserting the updated one

The Priority Queue only supports the removal of the **top** element efficiently. Removing an arbitrary element from inside a PQ is **`O(n)`**. Whereas TreeSet gives you **`O(log n)`** removal of any element.

```
┌──────────────────────────────────────────────────────────────┐
│  **Priority Queue**:                                         │
│  → Stale entries accumulate inside                           │
│  → Processed eventually, all relaxations fail, discarded     │
│  → Cannot remove arbitrary entries efficiently               │
│                                                              │
│  **TreeSet**:                                                │
│  → When a better distance found for node v:                  │
│    1. Erase old (staleDist, v) from set → O(log n)           │
│    2. Insert new (newDist, v) into set  → O(log n)           │
│  → Set never contains stale entries                          │
│  → Fewer iterations in the main loop                         │
└──────────────────────────────────────────────────────────────┘
```

---

### The Key Difference in the Relaxation Step

In the Priority Queue version, when you find a better distance:

```java
// **PQ version**
dist[adjNode] = dist[currNode] + adjWt;
minHeap.add(new Pair(dist[adjNode], adjNode));
// old (staleDist, adjNode) still sitting in heap — can't remove it
```

In the Set version, you do one extra step before inserting:

```java
// **Set version**
if (dist[adjNode] != (int) 1e9) {
    // Someone already reached adjNode before — there's a stale entry in the set
    // Remove it before inserting the better one
    set.remove(new Pair(dist[adjNode], adjNode));
}
dist[adjNode] = dist[currNode] + adjWt;
set.add(new Pair(dist[adjNode], adjNode));
```

The condition `dist[adjNode] != (int) 1e9` is important. If `dist[adjNode]` is still infinity, nobody has reached `adjNode` yet — there is no stale entry in the set to erase. You only erase when a previous entry exists.

---

### Set-Based Implementation — Full Java Code

```java
class Pair implements Comparable<Pair> {
    int dist;
    int node;
    
    Pair(int dist, int node) {
        this.dist = dist;
        this.node = node;
    }
    
    @Override
    public int compareTo(Pair other) {
        if(this.dist != other.dist) {
            return Integer.compare(this.dist, other.dist);
        } else {
            return Integer.compare(this.node, other.node);
        }
    }
    
}

class Solution
{
    //Function to find the shortest distance of all the vertices
    //from the source vertex S.
    static int[] dijkstra(int V, ArrayList<ArrayList<ArrayList<Integer>>> adj, int S)
    {
        TreeSet<Pair> st = new TreeSet<>();
        
        int[] dist = new int[V];
        for(int i=0; i<V; i++) {
            dist[i] = (int) 1e9;
        }
        
        
        dist[S] = 0;
        st.add(new Pair(0, S));
        
        while(st.size() > 0) {
            Pair curr = st.pollFirst();
            int currNode = curr.node;
            int currDist = curr.dist;
            
            for(List<Integer> adjacent : adj.get(currNode)) {
                int adjNode = adjacent.get(0);
                int adjDist = adjacent.get(1);
                
                if(dist[currNode] + adjDist < dist[adjNode]) {
                    // if it's other than infinity:
                    if (dist[adjNode] != (int) 1e9 && st.contains(new Pair(dist[adjNode], adjNode))) {
                        st.remove(new Pair(dist[adjNode], adjNode));
                    }
                    
                    dist[adjNode] = dist[currNode] + adjDist;
                    st.add(new Pair(dist[adjNode], adjNode));
                }
            }
        }
        
        return dist;
    }
}
```

---

### Priority Queue vs Set — Which is Better?

Striver is very clear on this — **you cannot give a definitive answer**. It depends on the graph.

```
┌──────────────────────────────────────────────────────────────┐
│  Priority Queue                                              │
│  → Stale entries accumulate                                  │
│  → Each push/pop → O(log n) where n = heap size              │
│  → Heap size can grow up to O(E) in worst case               │
│  → More iterations but simpler logic                         │
│                                                              │
│  TreeSet                                                     │
│  → No stale entries — actively erased                        │
│  → Each remove + insert → O(log n) where n = set size        │
│  → Set size stays bounded at O(V) — at most one entry        │
│    per node at any point                                     │
│  → Fewer iterations but erase cost on every update           │
│                                                              │
│  PQ saves on erase cost but pays for stale iterations.       │
│  Set saves on stale iterations but pays erase cost.          │
│  The tradeoff depends on graph density.                      │
│                                                              │
│  In interviews: mention both, explain the tradeoff,          │
│  and default to Priority Queue (simpler to implement).       │
└──────────────────────────────────────────────────────────────┘
```

---

# 🤯**Why PQ and not Q, Intuition, Time Complexity Derivation**

[G-34. Dijkstra's Algorithm - Why PQ and not Q, Intuition, Time Complexity Derivation - Part 3](https://www.youtube.com/watch?v=3dINsjyfooY&t=1s)

## 🚨Why `PriorityQueue` over `Queue`

Striver demonstrates this with a dedicated example. The core insight is this:

If you replace the min-heap with a plain FIFO Queue, the algorithm **still gives correct answers** — but it processes far more entries unnecessarily.

With a plain Queue, you process nodes in the order they were inserted, not in the order of shortest distance. This means:

- You might process a node reached via a long expensive path **before** you've discovered the short cheap path to it
- When the short cheap path is found later, you push the node again
- Now the node gets processed **twice** — once with the wrong (*longer*) distance, once with the correct (*shorter*) one
- All the relaxations from the first (wrong) processing are wasted work

With a min-heap, you always process the node with the ***currently known minimum distance*** first. By the time you process node `v`, all shorter paths to `v` have already been discovered and recorded. The ***greedy*** guarantee holds — you never waste time processing a node with a stale or suboptimal distance.

```
┌──────────────────────────────────────────────────────────────────┐
│  **Plain Queue**  → correct answers, but explores too many paths │
│                 unnecessary iterations, wasted relaxations       │
│                                                                  │
│  **Min-Heap**     → always processes minimum distance first      │
│                 greedy guarantee → each node processed           │
│                 with its FINAL shortest distance                 │
│                 no redundant exploration                         │
└──────────────────────────────────────────────────────────────────┘
```

---

### Time Complexity Derivation — Why `O(E log V)`?

This is the part that most resources just state without deriving. Striver derives it carefully — let's do the same. (just **`watch the video`** lecture’s portion)

![image.png](Concept%20Djisktra's%20Algorithm/image%203.png)

The pseudocode structure of Dijkstra with a Priority Queue looks like this:

```
while minHeap not empty:                      ← runs at most heap_size times
    pop top of minHeap (currDist, currNode)   ← O(log heap_size)
    for each neighbour of currNode:           ← runs degree(currNode) times
        if relaxation condition met:
            push (newDist, adjNode)           ← O(log heap_size)
```

There are two things to figure out: **how many times does each part run**, and **what is the heap size**.

---

**How many times does the outer loop run?**

In the worst case, every edge causes a push into the heap. How many edges are there? `E`. So the heap can accumulate up to `E` entries — one push per successful relaxation.

But across all iterations of the outer loop, the total work done in the inner `for` loop is bounded by the total number of edges, which is `E`. Each edge is examined exactly once from each direction.

So the total number of push operations across the entire algorithm is at most `E`.

---

**What is the heap size?**

In the worst case (dense graph where every relaxation succeeds), every edge causes a push. There are `E` edges, so the heap size can grow to at most `E`.

Therefore `log(heap_size) = log(E)`.

---

**Putting it together:**

![Screenshot from 2026-06-29 12-38-23.png](Concept%20Djisktra's%20Algorithm/Screenshot_from_2026-06-29_12-38-23.png)

```
Total work = E pushes × O(log E) per push
           = O(E log E)
```

Now, in a simple graph, `E ≤ V²` (at most V² edges). So:

```
log(E) ≤ log(V²) = 2 log(V)
```

Therefore:

```
O(E log E) = O(E × 2 log V) = O(E log V)
```

```
┌──────────────────────────────────────────────────────────────┐
│  TIME COMPLEXITY: O(E log V)                                 │
│                                                              │
│  E = total edges (each examined once across all iterations)  │
│  log V = cost per heap operation (push or pop)               │
│                                                              │
│  Derivation: at most E pushes into the heap,                 │
│  each push/pop costs O(log E) = O(log V²) = O(2 log V)       │
│  = O(log V)                                                  │
│                                                              │
│  SPACE COMPLEXITY: O(V + E)                                  │
│  adjacency list → O(V + E)                                   │
│  dist[] array   → O(V)                                       │
│  heap           → O(E) worst case                            │
└──────────────────────────────────────────────────────────────┘
```

---

### Quick Revision Card

```
┌─────────────────────────────────────────────────────────┐
│  DIJKSTRA'S ALGORITHM                                   │
├─────────────────────────────────────────────────────────┤
│  Use when : weighted graph, non-negative edges,         │
│             single source shortest path                 │
│  Forbidden: negative edge weights                       │
│                                                         │
│  Setup    : dist[] = 1e9, dist[src] = 0                 │
│             minHeap.add(Pair(0, src))                   │
│                                                         │
│  Relax    : if dist[curr] + adjWt < dist[adjNode]       │
│               → update dist[adjNode]                    │
│               → push Pair(dist[adjNode], adjNode)       │
│                                                         │
│  Stale    : harmless in PQ, actively erased in Set      │
│                                                         │
│  Time     : O(E log V)                                  │
│  Space    : O(V + E)                                    │
└─────────────────────────────────────────────────────────┘
```

---