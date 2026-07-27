# Disjoint Set (Union-Find) — Part 4: Full Java Implementation (Union by Rank + Path Compression)

---

## Building the Reusable Class

Following Striver's exact approach: package everything into a single `DisjointSet` class, so it can be dropped into any future problem (Kruskal's, and the whole run of DSU problems coming up next) without rewriting the mechanics each time.

### The Constructor — Initial Configuration

```java
class DisjointSet {

    // rank[i]   → a rough, never-decreasing upper bound on the height
    //             of the tree rooted at i's component (used ONLY to
    //             decide which tree attaches under which during union)
    // parent[i] → the node directly above i in its component's tree
    List<Integer> rank = new ArrayList<>();
    List<Integer> parent = new ArrayList<>();

    // Constructor: n = number of nodes.
    // We size everything to n+1 so this class works cleanly for BOTH
    // 1-indexed graphs (nodes 1..n, index 0 just sits unused) AND
    // 0-indexed graphs (nodes 0..n-1, we simply never touch index n).
    public DisjointSet(int n) {
        for (int i = 0; i <= n; i++) {
            rank.add(0);      // no node has anyone "beneath" it yet
            parent.add(i);    // every node starts as its own parent
        }
    }

    // ... findUParent and unionByRank go here — built out below
}
```

```
┌──────────────────────────────────────────────────────────────────┐
│  WHY n+1, not n?                                                 │
│                                                                  │
│  If the graph is 1-indexed (nodes 1 to n), you need valid        │
│  array slots at index 1 through n — so size n+1 (indices         │
│  0 to n) covers it, with index 0 simply unused.                  │
│                                                                  │
│  If the graph is 0-indexed (nodes 0 to n-1), size n+1 still      │
│  works fine — you just never touch the last index.               │
│                                                                  │
│  One constructor, works for both conventions. No special-casing  │
│  needed anywhere else in the class.                              │
└──────────────────────────────────────────────────────────────────┘
```

---

## findUParent — Recursive, With Path Compression Built In

This is the exact recursive shape derived in Part 3 — the compression happens as a natural side effect of the recursion unwinding, not as a separate step.

```java
    // Returns the ultimate parent ("boss") of `node`, and permanently
    // rewires every node along the path to point directly at that boss —
    // this is the path compression optimization from Part 3.
    public int findUParent(int node) {

        // BASE CASE: a node whose parent is itself IS the ultimate parent.
        // This is exactly how we recognize "we've reached the boss."
        if (node == parent.get(node)) {
            return node;
        }

        // RECURSIVE CASE: climb one level up, but don't just return
        // the answer — ASSIGN it back into parent.get(node) first.
        //
        // This single line is the entire path compression mechanism:
        // by the time this call returns, `node` points DIRECTLY at
        // the ultimate parent, no matter how deep the original chain was.
        int ultimateParent = findUParent(parent.get(node));
        parent.set(node, ultimateParent);
        return parent.get(node);
    }
```

**Why write it as a separate `ultimateParent` variable rather than one dense line?** Purely for readability — it makes explicit that we first *discover* the answer via recursion, and only *then* perform the compression assignment. Functionally identical to writing `parent.set(node, findUParent(parent.get(node))); return parent.get(node);` in one line, but this version is easier to trace when reading it back after a long gap.

---

## unionByRank — The Merge Logic

```java
    // Merges the components containing u and v.
    // Called once for every edge added to the dynamic graph.
    public void unionByRank(int u, int v) {

        int ultimateParentU = findUParent(u);
        int ultimateParentV = findUParent(v);

        // Already in the same component — nothing to merge.
        if (ultimateParentU == ultimateParentV) {
            return;
        }

        if (rank.get(ultimateParentU) < rank.get(ultimateParentV)) {
            // u's tree is shorter → attach it under v's tree
            parent.set(ultimateParentU, ultimateParentV);

        } else if (rank.get(ultimateParentV) < rank.get(ultimateParentU)) {
            // v's tree is shorter → attach it under u's tree
            parent.set(ultimateParentV, ultimateParentU);

        } else {
            // EQUAL rank — attach either way (arbitrary choice, we
            // follow Striver's convention: attach V under U)
            parent.set(ultimateParentV, ultimateParentU);

            // Rank ONLY increases here — this is the ONE situation
            // where the resulting tree's height genuinely grows,
            // because two equal-height trees just merged.
            rank.set(ultimateParentU, rank.get(ultimateParentU) + 1);
        }
    }
```

---

## The Complete Class, Assembled

```java
import java.util.*;

class DisjointSet {

    List<Integer> rank = new ArrayList<>();
    List<Integer> parent = new ArrayList<>();

    public DisjointSet(int n) {
        for (int i = 0; i <= n; i++) {
            rank.add(0);
            parent.add(i);
        }
    }

    public int findUParent(int node) {
        if (node == parent.get(node)) {
            return node;
        }
        int ultimateParent = findUParent(parent.get(node));
        parent.set(node, ultimateParent);
        return parent.get(node);
    }

    public void unionByRank(int u, int v) {
        int ultimateParentU = findUParent(u);
        int ultimateParentV = findUParent(v);

        if (ultimateParentU == ultimateParentV) {
            return;
        }

        if (rank.get(ultimateParentU) < rank.get(ultimateParentV)) {
            parent.set(ultimateParentU, ultimateParentV);
        } else if (rank.get(ultimateParentV) < rank.get(ultimateParentU)) {
            parent.set(ultimateParentV, ultimateParentU);
        } else {
            parent.set(ultimateParentV, ultimateParentU);
            rank.set(ultimateParentU, rank.get(ultimateParentU) + 1);
        }
    }
}
```

---

## Wiring It Up — Reproducing Our Exact Running Example

```java
public class Main {
    public static void main(String[] args) {

        // 7 nodes, 1-indexed — matches our running example exactly
        DisjointSet ds = new DisjointSet(7);

        // The exact edge sequence from Parts 1-3:
        ds.unionByRank(1, 2);
        ds.unionByRank(2, 3);
        ds.unionByRank(4, 5);
        ds.unionByRank(6, 7);

        // Checkpoint from Part 1/2 — asked right after edge (6,7):
        // "Does 1 and 4 belong to the same component?"
        if (ds.findUParent(1) == ds.findUParent(4)) {
            System.out.println("Same component");
        } else {
            System.out.println("Not same component");
        }
        // Expected: "Not same component" — matches our manual trace

        ds.unionByRank(5, 6);
        ds.unionByRank(3, 7);

        // Final checkpoint — asked after the last edge:
        if (ds.findUParent(1) == ds.findUParent(4)) {
            System.out.println("Same component");
        } else {
            System.out.println("Not same component");
        }
        // Expected: "Same component" — matches our manual trace
    }
}
```

**Expected output:**
```
Not same component
Same component
```

This matches every checkpoint we hand-traced across Parts 1, 2, and 3, exactly.

---

## Complexity Analysis

### Time Complexity — O(4α) per operation, effectively O(1)

This is genuinely one of the more mathematically deep results in this entire pattern sheet, so let's build the reasoning carefully rather than just quoting the number.

**Without any optimization**, a plain tree-based union-find (no rank, no compression) can degenerate into a long chain — imagine always attaching the new node under the previous one, blindly. Then `findParent` on the deepest node costs `O(N)` — no better than a linear scan.

**Union by Rank alone** (no compression) fixes this partially. Because you always attach the shorter tree under the taller one, the tree's height is mathematically bounded: it can be proven the height never exceeds `O(log N)`. So `findParent` costs `O(log N)` in the worst case — a solid improvement, but not yet constant.

**Path Compression alone** (no rank — imagine attaching arbitrarily, but flattening on every find) also helps a lot in practice, through a different mechanism: every query actively shortens future queries along the same path. Analyzed on its own, this gives an *amortized* `O(log N)` as well.

**Combined — Union by Rank + Path Compression together** — this is where the result becomes remarkable. The two optimizations interact in a way that pushes the amortized cost down to `O(α(N))`, where `α` is the **inverse Ackermann function**.

```
┌──────────────────────────────────────────────────────────────────┐
│  What is the inverse Ackermann function, intuitively?            │
│                                                                  │
│  The Ackermann function A(m, n) grows FASTER than any            │
│  exponential, tower-of-exponentials, or virtually any function   │
│  you could name — it's one of the canonical examples of an       │
│  extremely fast-growing function in computability theory.        │
│                                                                  │
│  α(N) is its INVERSE — so it grows unbelievably SLOWLY.          │
│  For all practical values of N — even N far larger than the      │
│  number of atoms in the observable universe — α(N) ≤ 4.          │
│                                                                  │
│  This is why the combined complexity is written as O(4α),        │
│  which for every real input size that could ever exist is,       │
│  literally, at most a small constant — genuinely O(1) in         │
│  practice, not just "close to it."                               │
└──────────────────────────────────────────────────────────────────┘
```

The full formal derivation of this bound involves a fairly intricate amortized analysis (potential functions, block decomposition of the Ackermann hierarchy) that is well beyond interview scope — and Striver explicitly says so in the lecture. **What matters for an interview**: know that combining rank/size with path compression achieves `O(4α)` per operation, know that `α` is the inverse Ackermann function, and know that this is effectively constant time for any N you'll ever encounter. Being able to *state* this — rather than reciting a memorized `O(1)` with no justification — is what signals real understanding.

```
┌──────────────────────────────────────────────────────────────────┐
│  Summary table:                                                  │
│                                                                  │
│  No optimization                    → O(N) worst case            │
│  Union by Rank/Size only            → O(log N)                   │
│  Path Compression only              → O(log N) amortized         │
│  BOTH combined                      → O(4α) ≈ O(1) amortized     │
│                                                                  │
│  findUParent(node)  → O(4α)                                      │
│  unionByRank(u, v)  → O(4α)  (two findUParent calls + O(1) work) │
└──────────────────────────────────────────────────────────────────┘
```

### Space Complexity — O(N)

```
parent[] array   → O(N)
rank[] array     → O(N)
─────────────────────────
Total: O(N)
```

No adjacency list is stored at all — Disjoint Set doesn't need one. It operates purely on the `parent[]`/`rank[]` relationship, completely independent of how the original graph's edges are represented.

---

## Quick Checkpoint Before Moving On

- [ ] In `findUParent`, which exact line performs the path compression — and why does writing the recursive call's result into `parent.set(...)` before returning it accomplish this?
- [ ] Why does the constructor size both arrays to `n+1` rather than `n`?
- [ ] Trace by hand: after `unionByRank(1,2)`, `unionByRank(2,3)`, `unionByRank(4,5)`, `unionByRank(6,7)` — what is `findUParent(4)` and what is `findUParent(1)`? Why are they different?
- [ ] What is the difference between the *provable* bound from Union by Rank alone (`O(log N)`) and the *combined* bound with Path Compression (`O(4α)`)?
- [ ] In plain words (not the formal derivation), why does `α(N) ≤ 4` for essentially any N you could realistically encounter?
- [ ] Why does `unionByRank` need to call `findUParent` on `u` and `v` first, rather than operating on `parent.get(u)` and `parent.get(v)` directly?

---

Ready for **Part 5 — Union by Size** whenever you are — we'll trace the same six-edge graph from scratch with the size-based variant, build its Java implementation, and close with the size-vs-rank comparison Striver gives at the end of the lecture.