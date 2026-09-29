LCA / Binary Lifting DSA Patterns

"Problems are infinite, but patterns are finite!"

---

NOTE ON SOURCE

Unlike your other sheets, this one has no Fraz/Algomaster raw aggregation to filter against — you haven't collected one for this topic yet. This is built directly from standard competitive programming and interview coverage of the technique. Flag anything here that doesn't match what you've seen elsewhere so it can be corrected.

---

THE CORE MENTAL MODEL — BEFORE ANY PATTERN

LCA (Lowest Common Ancestor) of two nodes u and v in a rooted tree is the deepest node that is an ancestor of both. Binary Lifting is the technique that makes LCA queries, and a family of related "jump up the tree" questions, fast after preprocessing.

Why naive LCA is too slow, and what binary lifting replaces:
Walking one node up to the root one parent-pointer-at-a-time, per query, is O(n) per query. If you have q queries on a tree of n nodes, that's O(n times q) total — too slow once both are large (n, q around 10^5). Binary lifting preprocesses in O(n log n) and answers each query in O(log n).

The core idea — jump in powers of two:
Instead of storing only "who is my parent" (a jump of 2^0 = 1), precompute "who is my ancestor 2^k steps up" for every node and every power of 2 up to log(n). Any distance d can be written as a sum of powers of two (its binary representation), so you can reach any ancestor d steps up in O(log n) jumps instead of d single-parent hops. This is the same "decompose into powers of two" idea behind fast exponentiation and segment tree range queries, just applied to tree ancestors instead of numbers or ranges.

The up[node][k] table — the one structure this entire topic is built on:

up[node][0] = parent(node)
up[node][k] = up( up[node][k-1], k-1 )   — jump 2^k equals jump 2^(k-1), twice

Precompute this once via a single DFS/BFS from the root (O(n log n) total, since each node fills O(log n) entries). Every pattern below is a different query built on top of this same table — the preprocessing step never changes.

The two-step LCA algorithm, stated once so every problem below can refer back to it:

Step 1, Equalize depth. If depth(u) is not equal to depth(v), lift the deeper node up by exactly depth(u) minus depth(v), using the binary-lifting table (decompose the depth difference into powers of two, same as any binary-lifting jump).

Step 2, Binary search for the LCA together. With both nodes now at the same depth, jump both u and v up simultaneously by decreasing powers of two, but only take a jump if it does NOT make them equal — that is, only jump while up[u][k] is not equal to up[v][k]. This deliberately stops one step below the actual LCA. The final answer is parent(u), equivalently up[u][0], taken once the loop ends — not u itself.

Why "stop one step below, then take one more step" is not an off-by-one bug to just memorize around: if you jumped until u equals v, you might overshoot past the true LCA into a common ancestor that isn't the LOWEST one — you have no way to tell from u equals v alone whether you landed exactly on the LCA or jumped past it. Stopping just before equality and taking the final single parent step is the only way to guarantee landing exactly on the lowest one.

---

PATTERN IDENTIFICATION QUICK REFERENCE

Binary Lifting Fundamentals
Trigger: "kth ancestor of a node", precompute ancestor jumps

LCA via Binary Lifting
Trigger: "lowest common ancestor", tree with many repeated LCA queries

Distance and Path Queries on Trees
Trigger: "distance between two nodes", "does a node lie on the path between two others", "kth node on the path"

Weighted / Aggregate Path Queries
Trigger: "minimum/maximum edge weight on path between two nodes", "XOR/GCD on path"

Offline LCA (Tarjan's)
Trigger: LCA queries known in advance, all offline, no updates needed between queries

LCA in a General DAG / Multiple Parents
Trigger: node can have more than one parent — binary lifting doesn't directly apply (flagged out of scope)

---

PATTERN 1: BINARY LIFTING FUNDAMENTALS

Identify: The problem asks directly for "the kth ancestor of a node," or requires jumping a fixed distance up a tree, repeated across many queries. This is the foundational pattern — every later pattern in this sheet reuses the up[node][k] table built here without re-deriving it.

LC 1483 is the anchor, and the only problem that tests the raw table in isolation. Build up[node][k] via one DFS from the root. Answering getKthAncestor(node, k) decomposes k into its binary representation and jumps through the corresponding powers of two, checking at each step whether the current node has gone above the root, returning -1 if so.

Why this problem is worth doing standalone before touching LCA: LCA's two-step algorithm (equalize depth, then binary-search together) is really "call the kth-ancestor jump twice, with a different stopping condition the second time." If the raw jump mechanic from LC 1483 isn't automatic, the LCA algorithm's second step will look like new machinery instead of a variation of something already known.

Problems:

1. Theory: Binary Lifting Table Construction
   Key Concept: up[node][k] = up[up[node][k-1]][k-1] — one DFS builds the whole table in O(n log n)

2. LC 1483. Kth Ancestor of a Tree Node
   Key Concept: Direct application — decompose k into powers of two, jump through the table

---

PATTERN 2: LCA VIA BINARY LIFTING

Identify: Many queries asking for the lowest common ancestor of two nodes in a FIXED, unchanging tree. The precompute-once-query-many shape is the signal — if there's only ever going to be one or two LCA queries total, this machinery is overkill, and a simple O(n) ancestor-path-and-compare approach is fine instead (that simpler approach already lives in your BST sheet's Pattern 3, LC 235, for the sorted special case).

The full algorithm, applying the Core Mental Model's two steps directly: precompute depth[] and up[][] via one DFS. For each query (u, v): equalize depth by lifting the deeper node, then binary-search both nodes up together, and return parent(u), equivalently up[u][0], once they'd become equal on the next jump.

GFG: LCA in a Binary Tree, the generic tree version — do this before jumping to weighted or distance problems. The plain O(V+E) DFS-based LCA (store each node's ancestor path, or return-up-from-postorder) is a DIFFERENT, simpler technique from binary lifting. Worth knowing both, and worth being explicit that binary lifting is the answer specifically when the NUMBER OF QUERIES is large, not because it's a strictly "better" algorithm in isolation. A single LCA query is faster to answer with plain O(V+E) DFS than with O(n log n) preprocessing.

LC 1650, LCA with parent pointers, no root given, no binary lifting needed at all — this is deliberately included as a CONTRAST problem. When each node already has a dot-parent pointer and you're solving one query at a time, the correct technique is two-pointer convergence: walk one path to the root, then walk the second path checking against a set, or use the classic "swap to the other list" trick from the Linked List sheet's LC 160, Intersection of Two Linked Lists. Not binary lifting. Recognizing when binary lifting is overkill is as much the point of this pattern as recognizing when it's needed.

Problems:

1. GFG: Lowest Common Ancestor in a Binary Tree, single query
   Key Concept: Plain O(V+E) DFS — contrast case, do this before binary lifting to see why preprocessing only helps when queries are many

2. LCA using Binary Lifting, Theory / GFG
   Key Concept: Full two-step algorithm — equalize depth, then binary-search up together

3. LC 1650. Lowest Common Ancestor of a Binary Tree III
   Key Concept: Parent pointers, single query — two-pointer convergence, same idea as Linked List sheet's LC 160, NOT binary lifting

4. Multiple LCA Queries on a Static Tree, CSES-style "Company Queries II"
   Key Concept: The canonical "many queries" trigger — binary lifting preprocessing pays off specifically here

---

PATTERN 3: DISTANCE AND PATH QUERIES ON TREES

Identify: The question is no longer "who is the LCA" directly, but something derived from it — the number of edges between two nodes, whether a node lies on the path between two others, or which node sits exactly k steps along that path. Every problem here computes the LCA first, using Pattern 2, and then does O(1) to O(log n) additional work on top.

Distance between two nodes — the formula that makes this pattern trivial once LCA is solid:

distance(u, v) = depth(u) + depth(v) - 2 times depth(LCA(u, v))

Both u's and v's paths to the root share the segment from the root down to the LCA exactly once each. Subtracting 2 times depth(LCA) removes the double-counted shared segment.

"Does node w lie on the path between u and v" is a direct consequence of the distance formula, not a new algorithm: w lies on the path if and only if distance(u, w) plus distance(w, v) equals distance(u, v). This reuses the distance formula three times — no new machinery, just the same tool applied three times and compared.

CSES: Company Queries II style, "kth node on the path from u to v," is the one genuinely new mechanic in this pattern. Find L, the LCA of u and v. If k is less than or equal to depth(u) minus depth(L), the answer is the kth ancestor of u, using Pattern 1's raw jump. Otherwise the answer lies on the v side: it's the (distance(u,v) minus k)th ancestor of v. This is the clearest example in this pattern of composing Pattern 1's kth-ancestor jump with Pattern 2's LCA into one query.

Problems:

1. CSES: Distance Between Nodes
   Key Concept: depth(u) + depth(v) - 2 times depth(LCA(u,v)) — direct formula application

2. Check if a Node Lies on the Path Between Two Other Nodes
   Key Concept: Same distance formula, applied three times and compared for equality

3. CSES: Company Queries II, kth node on path
   Key Concept: Composes Pattern 1's kth-ancestor jump with Pattern 2's LCA — branch on which side of the LCA the kth node falls

---

PATTERN 4: WEIGHTED / AGGREGATE PATH QUERIES

Identify: The tree has edge weights, and the query asks for an aggregate over the path between two nodes — minimum edge weight, maximum edge weight, XOR, GCD — not just the hop count. The binary lifting table itself needs to carry the aggregate alongside the ancestor jump, not just the jump.

The generalized table, the one change from Pattern 1's table:

up[node][k] stays the same, the ancestor 2^k steps up.
agg[node][k] is the aggregate of the edge weights on that 2^k-length jump.
agg[node][k] = combine( agg[node][k-1], agg[up[node][k-1]][k-1] )

Combine is min, max, gcd, or xor depending on what the problem asks. The table-building recurrence has the exact same shape as Pattern 1's, just replacing "jump" with "jump, and remember the aggregate along the way."

LC 1483's sibling for weighted trees, Minimum or Maximum Edge Weight on a Path: same two-step LCA algorithm from Pattern 2, but every lift, both the depth-equalizing step and the binary-search-up step, also combines agg[node][k] into a running answer as it jumps. By the time both pointers reach the LCA, the running answer already holds the aggregate over the entire path, with no separate second pass needed.

XOR-on-path problems, offline variant, cross-referenced: when the aggregate is XOR specifically, there's a prefix-XOR shortcut avoiding binary lifting's aggregate table entirely. xorOnPath(u, v) equals prefixXor(u) XOR prefixXor(v) XOR value(LCA(u,v)) — or without the LCA term if XORing edge weights rather than node values. This is the tree analogue of the Bit Manipulation sheet's Prefix XOR pattern, and worth recognizing as a lighter-weight alternative when the aggregate happens to be XOR, which is invertible, rather than min or max, which are not invertible and thus genuinely require the full aggregate table.

Problems:

1. Theory: Binary Lifting with Aggregate Table
   Key Concept: Extend up[node][k] with a parallel agg[node][k] combined the same way the ancestor jump is built

2. CSES-style "Company Queries III," min/max edge weight on path
   Key Concept: Same two-step LCA algorithm, combining the aggregate at every lift instead of just tracking the ancestor

3. XOR / GCD Queries on Tree Paths
   Key Concept: Same aggregate-table technique for XOR or GCD; XOR specifically has a prefix-XOR shortcut avoiding the aggregate table entirely — cross reference Bit Manipulation sheet, Pattern 3

---

PATTERN 5: OFFLINE LCA, TARJAN'S ALGORITHM

Identify: All LCA queries are known IN ADVANCE, given as a batch up front rather than arriving one at a time as the tree is explored, and there are no updates to the tree between queries. This is a fundamentally different technique from binary lifting — it processes all queries in a single DFS pass using a DSU, rather than precomputing a jump table.

Why this is a genuinely different tool, not a variant of Pattern 2: binary lifting answers each query independently in O(log n) after O(n log n) preprocessing — it works whether queries are known in advance or arrive interactively. Tarjan's offline algorithm instead achieves O(n plus q times inverse-Ackermann of n), often faster in practice, but REQUIRES knowing every query beforehand, because it answers queries opportunistically as the DFS happens to pass over the second node of a pair. If queries can arrive after the DFS has already run, Tarjan's doesn't apply at all, and binary lifting, or the simpler single-query DFS from Pattern 2, is the only option.

The mechanism: DFS from the root. When finishing, meaning postorder, a node u, union u's DSU set into its parent's. For every query (u, v) where v has ALREADY been fully visited, that is postorder-finished, by the time u finishes, the LCA of (u, v) is exactly find(v) in the DSU at that moment — the DSU's current root for v's component IS the lowest ancestor common to both, because everything below the true LCA has already been merged upward into it.

Problems:

1. Theory: Tarjan's Offline LCA
   Key Concept: DSU plus DFS postorder — answer a query for (u,v) the moment the second of the two nodes finishes its DFS

2. CSES-style "batch LCA queries known upfront"
   Key Concept: The canonical trigger for choosing Tarjan's over binary lifting — all queries given as one batch, no interactivity needed

---

PATTERN 6: LCA IN A GENERAL DAG / MULTIPLE PARENTS, OUT OF SCOPE

Flagged, not taught. Everything above assumes a TREE, where each node has exactly one parent. The moment a node can have multiple parents, a general DAG, "the" lowest common ancestor may not even be unique, and binary lifting's single up[node][k] table doesn't directly generalize. This shows up in some competitive programming problems, LCA in a DAG, multiple inheritance modeling, but is genuinely a different, harder topic. Consistent with how your other sheets flag Fenwick or Segment Trees or String Hashing as deferred rather than silently omitted, this is flagged the same way rather than force-fit into the tree-only machinery above.

---

FINAL SUMMARY

Binary Lifting Fundamentals — 2 problems. Core mechanism: up[node][k] table, jump 2^k steps in O(1) after O(n log n) preprocessing.

LCA via Binary Lifting — 4 problems. Core mechanism: equalize depth, then binary-search both nodes up together; know when NOT to use it too.

Distance and Path Queries — 3 problems. Core mechanism: depth(u) + depth(v) - 2 times depth(LCA), and direct compositions of it.

Weighted / Aggregate Path Queries — 3 problems. Core mechanism: parallel agg[node][k] table combined the same way the jump table is built.

Offline LCA, Tarjan's — 2 problems. Core mechanism: DSU plus DFS postorder — all queries answered in one pass, but only when known in advance.

LCA in a General DAG — flagged, out of scope. Multiple-parent case, binary lifting doesn't directly generalize.

Total: around 14 problems plus 2 theory-flagged items.

---

HOW TO USE THIS SHEET

Pattern 1 is mandatory first, with no exceptions. LC 1483 in isolation is what makes every later pattern's "jump" step feel like reuse instead of new machinery. Do not attempt Pattern 2's LCA algorithm until decomposing a number into powers of two and jumping through up[node][k] is completely automatic.

Pattern 2's contrast problems matter as much as the main algorithm. The single-query plain-DFS LCA and LC 1650's two-pointer-convergence-with-parent-pointers are deliberately placed alongside the binary lifting version specifically so the "many queries on a static tree" trigger becomes a recognition skill, not a reflex to reach for binary lifting every time "LCA" appears in a problem statement.

Pattern 3 is pure composition — treat it as confirmation, not new learning. If the distance formula and the kth-ancestor jump from Pattern 1 are both solid, every problem in Pattern 3 should feel like "I already know both halves of this."

Pattern 4 requires Pattern 2 completely solid first. The aggregate table's recurrence is structurally identical to the plain ancestor table, but building it while still shaky on the plain version compounds two new ideas at once instead of one.

Pattern 5 is the one place in this sheet where the answer to "which technique" is genuinely "it depends on whether queries are known upfront." Say that test out loud before choosing between Tarjan's and binary lifting, the same way the Binary Search sheet trains "accumulate versus place" before choosing between its own two lookalike patterns.

---

A couple of things worth flagging since this was built without a raw aggregation file: the CSES problem names in Patterns 3 through 5 are descriptive placeholders, since I don't have your CSES numbering in front of me — send the actual problem set entries if you have them and I'll slot in real names. Also, this sheet leans more competitive-programming-flavored than your other sheets, since LCA and binary lifting is genuinely more of a CP topic than a pure interview one — LC 1483 is close to the only LeetCode-native problem here. Worth knowing your interview ROI on this one is lower than DP or Graph — treat it as "good to know exists" rather than "grind until automatic."

Ready for Meet-in-the-Middle whenever you say go — same plain-text style.