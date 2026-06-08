# Graph DSA Patterns

"Problems are infinite, but patterns are finite!"

---

## Pattern Identification Quick Reference

| Pattern | Trigger |
|---|---|
| Graph Traversal | explicit graph, just visit nodes, no shortest path needed |
| Grid DFS / Flood Fill | 2D matrix, connected cells, count/mark regions |
| BFS Single-source | shortest path, minimum steps, unweighted graph |
| Multi-source BFS | minimum distance from ANY of multiple sources |
| 0/1 BFS | move costs are strictly 0 or 1, use Deque |
| Cycle Detection | "can you finish?", "is it possible?" on directed graph |
| Topological Sort | prerequisites, ordering, dependencies, scheduling |
| Dijkstra | weighted graph, non-negative edges, minimum cost/effort |
| Bellman-Ford | K-step constraint, or negative weights possible |
| Floyd-Warshall | all pairs shortest path, N ≤ 200 |
| DSU | merge groups, check if same component |
| MST | connect all nodes with minimum total cost |
| Bridges & APs | which single edge/node removal disconnects graph |
| SCC | circular dependency, directed graph reachability groups |
| Eulerian Path | use every edge exactly once |

---

## PHASE 1 — Foundation

---

### Pattern 1: Graph Traversal (DFS/BFS on Explicit Graphs)

Identify: Graph given as adjacency list/matrix, no grid, no shortest path — just traverse and collect info.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 547. Number of Provinces | Basic DFS on adjacency matrix |
| 2 | LC 841. Keys and Rooms | DFS reachability check |
| 3 | LC 133. Clone Graph | DFS + HashMap for node cloning |
| 4 | LC 690. Employee Importance | DFS/BFS on implicit tree graph |
| 5 | LC 1376. Time Needed to Inform All Employees | DFS on tree, propagate timing downward |
| 6 | LC 1466. Reorder Routes to Make All Paths Lead to City Zero | DFS with edge direction awareness |
| 7 | LC 863. All Nodes Distance K in Binary Tree | DFS to build undirected graph + BFS for distance |

---

### Pattern 2: Grid DFS / Flood Fill

Identify: 2D matrix problem, explore connected cells, count or mark regions.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 733. Flood Fill | Basic grid DFS template |
| 2 | LC 200. Number of Islands | Connected components on grid |
| 3 | LC 130. Surrounded Regions | Boundary DFS + inversion trick |
| 4 | LC 1020. Number of Enclaves | Boundary DFS, multi-source flavor |
| 5 | LC 417. Pacific Atlantic Water Flow | Reverse multi-source DFS from both boundaries |
| 6 | LC 79. Word Search | DFS + backtracking on grid |
| 7 | GFG: Number of Distinct Islands | DFS + shape encoding/hashing |
| 8 | LC 827. Making A Large Island | DFS component ID + DSU merge |
| 9 | LC 2246. Longest Path With Different Adjacent Characters | DFS on tree with neighbor value constraint |

---

### Pattern 3: BFS — Single Source

Identify: Unweighted graph or grid, find shortest path or minimum steps from one source.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 1091. Shortest Path in Binary Matrix | Basic BFS shortest path on grid |
| 2 | LC 1197. Minimum Knight Moves | BFS on implicit position graph |
| 3 | LC 752. Open the Lock | BFS on string state space |
| 4 | LC 433. Minimum Genetic Mutation | BFS with word transformation |
| 5 | LC 127. Word Ladder | BFS + implicit graph built from dictionary |
| 6 | LC 909. Snakes and Ladders | BFS with position remapping |
| 7 | LC 815. Bus Routes | BFS on hypergraph, route treated as node |
| 8 | LC 126. Word Ladder II | BFS layer-build + DFS backtrack to find all paths |

---

### Pattern 4: Multi-Source BFS

Identify: Multiple starting points — seed ALL sources into queue at level 0 simultaneously.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 994. Rotting Oranges | Multi-source BFS with time tracking |
| 2 | LC 542. 01 Matrix | Multi-source BFS from all 0s outward |
| 3 | LC 286. Walls and Gates | Multi-source BFS from all gates |
| 4 | LC 1020. Number of Enclaves | Multi-source BFS from boundary cells |

---

## PHASE 2 — Core Algorithms

---

### Pattern 5: Cycle Detection

Identify: Check if a cycle exists. Directed graph — 3-color DFS or Kahn's BFS. Undirected — parent tracking or DSU.

| # | Problem | Key Concept |
|---|---|---|
| 1 | Theory | Cycle Detection Undirected — DFS with parent |
| 2 | Theory | Cycle Detection Undirected — BFS / DSU |
| 3 | Theory | Cycle Detection Directed — DFS 3-color (WHITE/GRAY/BLACK) |
| 4 | Theory | Cycle Detection Directed — Kahn's BFS |
| 5 | LC 785. Is Graph Bipartite? | Odd-length cycle detection → not bipartite |
| 6 | LC 1559. Detect Cycles in 2D Grid | Cycle detection on grid graph |
| 7 | LC 2608. Shortest Cycle in a Graph | BFS-based shortest cycle length |

---

### Pattern 6: Topological Sort

Identify: DAG with dependencies — need ordering where all prerequisites come before dependents.

| # | Problem | Key Concept |
|---|---|---|
| 1 | Theory | Topological Sort — DFS + Stack |
| 2 | Theory | Kahn's Algorithm — BFS with in-degree |
| 3 | LC 207. Course Schedule | Cycle check via Kahn's — can we finish? |
| 4 | LC 210. Course Schedule II | Kahn's — return actual topo order |
| 5 | LC 802. Find Eventual Safe States | Reverse graph + Kahn's |
| 6 | LC 310. Minimum Height Trees | Leaf trimming = reverse topo sort |
| 7 | LC 1462. Course Schedule IV | Topo + transitive reachability |
| 8 | LC 329. Longest Increasing Path in a Matrix | Topo sort on implicit DAG in matrix |
| 9 | LC 444. Sequence Reconstruction | Topo sort uniqueness check |
| 10 | LC 2050. Parallel Courses III | Topo + DP on DAG for critical path |

---

### Pattern 7: 0/1 BFS

Identify: Shortest path where edge/move costs are strictly 0 or 1 — use Deque, not Queue. Faster than Dijkstra for this case.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 2290. Minimum Obstacle Removal to Reach Corner | 0/1 BFS on grid |
| 2 | LC 1368. Minimum Cost to Make at Least One Valid Path in Grid | 0/1 BFS with directional move cost |
| 3 | LC 1293. Shortest Path in Grid with Obstacles Elimination | BFS with state = (row, col, remaining k) |

---

## PHASE 3 — Weighted Shortest Path

---

### Pattern 8: Dijkstra's Algorithm

Identify: Weighted graph, non-negative edges, minimum cost/time/effort from a source.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 743. Network Delay Time | Pure Dijkstra template |
| 2 | LC 1631. Path With Minimum Effort | Custom weight = max edge on path |
| 3 | LC 1514. Path with Maximum Probability | Max-heap Dijkstra to maximize |
| 4 | LC 778. Swim in Rising Water | Dijkstra / Binary search + BFS |
| 5 | LC 787. Cheapest Flights Within K Stops | State = (node, stops used) |
| 6 | LC 1976. Number of Ways to Arrive at Destination | Dijkstra + count paths |
| 7 | LC 1102. Path With Maximum Minimum Value | Max-heap Dijkstra, maximize bottleneck |
| 8 | LC 505. The Maze II | Dijkstra on continuous rolling movement |
| 9 | LC 3341. Find Minimum Time to Reach Last Room I | Dijkstra with time constraint on grid |
| 10 | LC 2976. Minimum Cost to Convert String I | Dijkstra on character-level graph |
| 11 | LC 1334. Find City With Smallest Number of Neighbors | Dijkstra from every node |
| 12 | LC 2203. Minimum Weighted Subgraph With Required Paths | Multi-source Dijkstra merge |

---

### Pattern 9: Bellman-Ford Algorithm

Identify: K-step constraint, or negative weights possible. Solve LC 743 and LC 787 with both Dijkstra AND Bellman-Ford to understand the tradeoff.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 743. Network Delay Time | Bellman-Ford baseline template |
| 2 | LC 787. Cheapest Flights Within K Stops | K-relaxation Bellman-Ford |
| 3 | LC 1928. Minimum Cost to Reach Destination in Time | Bellman-Ford on time-state graph |

---

### Pattern 10: Floyd-Warshall Algorithm

Identify: Shortest path between ALL pairs, N is small (≤ 200), or transitive closure needed.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 1334. Find City With Smallest Number of Neighbors | All-pairs shortest path template |
| 2 | LC 1462. Course Schedule IV | Transitive closure via Floyd |
| 3 | LC 399. Evaluate Division | Floyd-Warshall on equation graph |
| 4 | LC 2642. Design Graph With Shortest Path Calculator | Dynamic Floyd-Warshall |

---

## PHASE 4 — Advanced Structures

---

### Pattern 11: Disjoint Set Union (DSU)

Identify: Dynamically merging components, checking if two nodes belong to the same component.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 323. Number of Connected Components | Basic DSU template |
| 2 | LC 261. Graph Valid Tree | DSU + cycle check |
| 3 | LC 684. Redundant Connection | DSU — first edge that forms a cycle |
| 4 | LC 1319. Number of Operations to Make Network Connected | DSU + component count |
| 5 | LC 721. Accounts Merge | DSU on string keys |
| 6 | LC 947. Most Stones Removed with Same Row or Column | DSU on row/col indices, not cells |
| 7 | LC 924. Minimize Malware Spread | DSU + component size analysis |
| 8 | LC 399. Evaluate Division | Weighted DSU |
| 9 | LC 827. Making A Large Island | DSU + Grid DFS merge |
| 10 | LC 1584. Min Cost to Connect All Points | DSU + MST bridge problem |

---

### Pattern 12: Minimum Spanning Tree (MST)

Identify: Connect all nodes with minimum total edge weight.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 1584. Min Cost to Connect All Points | Prim's / Kruskal's template |
| 2 | LC 1135. Connecting Cities With Minimum Cost | Kruskal's on explicit edge list |
| 3 | LC 1168. Optimize Water Distribution in a Village | Virtual node MST trick |
| 4 | LC 1489. Critical and Pseudo-Critical Edges in MST | MST edge classification |

---

### Pattern 13: Bridges & Articulation Points

Identify: Which single edge or node removal disconnects the graph. Uses Tarjan's DFS with disc[] and low[].

| # | Problem | Key Concept |
|---|---|---|
| 1 | Theory | Tarjan's Algorithm for Bridges |
| 2 | Theory | Tarjan's Algorithm for Articulation Points |
| 3 | LC 1192. Critical Connections in a Network | Bridges via Tarjan's DFS |
| 4 | GFG: Articulation Point in Graph | Articulation points via Tarjan's |
| 5 | LC 2685. Count the Number of Complete Components | DSU + component completeness check |

---

### Pattern 14: Strongly Connected Components (SCC)

Identify: Directed graph — find groups where every node can reach every other node within the group.

| # | Problem | Key Concept |
|---|---|---|
| 1 | Theory | Kosaraju's Algorithm — 2 DFS passes |
| 2 | Theory | Tarjan's Algorithm for SCC — 1 DFS pass |
| 3 | LC 802. Find Eventual Safe States | Nodes not part of any cycle |
| 4 | LC 2360. Longest Cycle in a Graph | DFS cycle detection in directed graph |

---

### Pattern 15: Eulerian Path / Circuit

Identify: Visit every edge exactly once.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 332. Reconstruct Itinerary | Hierholzer's algorithm on directed graph |
| 2 | LC 2097. Valid Arrangement of Pairs | Eulerian path on general directed graph |
| 3 | LC 753. Cracking the Safe | Eulerian circuit on de Bruijn graph |

---

## PHASE 5 — Hard Combinations

Do these only after both constituent patterns are fully solid.

| # | Problem | Pattern Combination |
|---|---|---|
| 1 | LC 847. Shortest Path Visiting All Nodes | BFS + Bitmask DP |
| 2 | LC 1349. Maximum Students Taking Exam | Bipartite Matching / Bitmask DP |
| 3 | LC 1042. Flower Planting With No Adjacent | Graph Coloring greedy |
| 4 | LC 1483. Kth Ancestor of a Tree Node | Binary Lifting / LCA |

---

## Summary

| Phase | Patterns | Problems |
|---|---|---|
| Phase 1 — Foundation | Graph Traversal, Grid DFS, BFS Single, Multi-source BFS | 28 |
| Phase 2 — Core Algorithms | Cycle Detection, Topo Sort, 0/1 BFS | 20 |
| Phase 3 — Weighted Shortest Path | Dijkstra, Bellman-Ford, Floyd-Warshall | 19 |
| Phase 4 — Advanced Structures | DSU, MST, Bridges, SCC, Eulerian | 23 |
| Phase 5 — Hard Combinations | Mixed | 4 |
| Total | 15 Patterns | ~94 problems |

---