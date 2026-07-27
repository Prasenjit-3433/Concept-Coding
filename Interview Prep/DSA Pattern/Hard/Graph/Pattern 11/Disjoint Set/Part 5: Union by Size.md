# Disjoint Set (Union-Find) — Part 5: Union by Size

---

## Why Bother With a Second Implementation?

Striver raises this directly in the lecture: after Path Compression, `rank[]` stops meaning "exact height" — it becomes a rough, never-decreasing signal (as we established at the end of Part 3). So a fair question is:

> *"If rank is already an approximation and gets distorted by compression anyway, why not track something that stays meaningful and intuitive throughout — like the actual number of nodes in each component?"*

That's exactly what **Union by Size** does. It's functionally equivalent to Union by Rank — same asymptotic guarantees, same core idea of "attach smaller under larger" — but the quantity being compared is something concrete and easy to reason about: **how many nodes does this component actually contain right now?**

```
┌──────────────────────────────────────────────────────────────────┐
│  Union by Rank   → compares an abstract, rough "tree height"     │
│                     signal that gets distorted by compression    │
│                                                                  │
│  Union by Size   → compares the ACTUAL number of nodes in each   │
│                     component — always exact, always meaningful, │
│                     never distorted by anything                  │
│                                                                  │
│  Both give the same O(4α) guarantee. Size is just more           │
│  intuitive to reason about — which is why Striver personally     │
│  prefers it, as he says explicitly in the lecture.               │
└──────────────────────────────────────────────────────────────────┘
```

**One hard rule before we go further:** never mix the two. A single Disjoint Set instance uses *either* `rank[]` *or* `size[]` — never both together. They're two independent, complete strategies for the same decision ("who attaches under whom"), not complementary pieces.

---

## Initial Configuration

Same seven nodes, same starting point — but now every component starts with a size of exactly 1 (itself):

```
Node:    1   2   3   4   5   6   7
parent:  1   2   3   4   5   6   7      (everyone is their own parent, as always)
size:    1   1   1   1   1   1   1      (every component currently has exactly 1 node)
```

---

## The Union by Size — Pseudocode

```
unionBySize(u, v):
    pu = findUParent(u)
    pv = findUParent(v)

    if pu == pv:
        return                          ← already same component

    if size[pu] < size[pv]:
        parent[pu] = pv                  ← smaller component attaches under larger
        size[pv] += size[pu]             ← larger component absorbs the smaller one's count
    else:
        parent[pv] = pu                  ← covers BOTH "pv is smaller" AND "equal size" cases
        size[pu] += size[pv]
```

Notice the `else` branch quietly handles the equal-size case too — if neither is strictly smaller, attach `v`'s tree under `u`'s tree, exactly the same "doesn't matter which way when tied" reasoning from Union by Rank.

---

## Full Trace — Same Six Edges, In Order

### Edge 1 — unionBySize(1, 2)

```
pu = 1, pv = 2
size[1] = 1, size[2] = 1  → NOT (size[pu] < size[pv]) → else branch

parent[2] = 1
size[1] += size[2]  →  size[1] = 2
```

```
Node:    1   2   3   4   5   6   7
parent:  1   1   3   4   5   6   7
size:    2   1   1   1   1   1   1
```

---

### Edge 2 — unionBySize(2, 3)

```
pu = findUParent(2) = 1   (2's parent is 1, and 1 is its own parent)
pv = findUParent(3) = 3

size[1] = 2, size[3] = 1  → NOT (2 < 1) → else branch

parent[3] = 1
size[1] += size[3]  →  size[1] = 3
```

```
Node:    1   2   3   4   5   6   7
parent:  1   1   1   4   5   6   7
size:    3   1   1   1   1   1   1
```

---

### Edge 3 — unionBySize(4, 5)

```
pu = 4, pv = 5
size[4] = 1, size[5] = 1  → equal → else branch

parent[5] = 4
size[4] += size[5]  →  size[4] = 2
```

```
Node:    1   2   3   4   5   6   7
parent:  1   1   1   4   4   6   7
size:    3   1   1   2   1   1   1
```

---

### Edge 4 — unionBySize(6, 7)

```
pu = 6, pv = 7
size[6] = 1, size[7] = 1  → equal → else branch

parent[7] = 6
size[6] += size[7]  →  size[6] = 2
```

```
Node:    1   2   3   4   5   6   7
parent:  1   1   1   4   4   6   6
size:    3   1   1   2   1   2   1
```

**Checkpoint — same question as before:**

> *Does 1 and 4 belong to the same component?*

```
findUParent(1) = 1
findUParent(4) = 4

1 != 4 → NOT the same component. Matches every prior trace.
```

---

### Edge 5 — unionBySize(5, 6)

```
pu = findUParent(5)
   5 != parent[5] (which is 4) → recurse
   findUParent(4): 4 == parent[4] → BASE CASE → return 4
   compression: parent[5] = 4  (no change — already 4)
   pu = 4

pv = findUParent(6) = 6   (6 is its own parent)

size[4] = 2, size[6] = 2  → equal → else branch

parent[6] = 4
size[4] += size[6]  →  size[4] = 4
```

```
Node:    1   2   3   4   5   6   7
parent:  1   1   1   4   4   4   7
size:    3   1   1   4   1   2   1
```

```
┌──────────────────────────────────────────────────────────────────┐
│  Notice: node 7 comes along for free again — same as the         │
│  rank version. It didn't move directly, but its ultimate         │
│  parent is now 4 (via 6), because the whole {6,7} component      │
│  fused with the whole {4,5} component in one union call.         │
│                                                                  │
│  Also notice: size[6] is now STALE — it still reads 2, but       │
│  node 6 is no longer a root, so this value is simply never       │
│  looked at again. This is expected and harmless — exactly the    │
│  same situation as a non-root's rank going unused in Part 2.     │
└──────────────────────────────────────────────────────────────────┘
```

---

### Edge 6 — unionBySize(3, 7)

```
pu = findUParent(3)
   3 != parent[3] (which is 1) → recurse
   findUParent(1): 1 == parent[1] → BASE CASE → return 1
   compression: parent[3] = 1  (no change — already 1)
   pu = 1

pv = findUParent(7)
   7 != parent[7] (which is 6) → recurse
      findUParent(6):
         6 != parent[6] (which is 4) → recurse
            findUParent(4): 4 == parent[4] → BASE CASE → return 4
         compression: parent[6] = 4  (no change — already 4)
         findUParent(6) returns 4
   compression: parent[7] = 4   ← REAL CHANGE! 7 used to point to 6, now points directly to 4
   pv = 4

size[1] = 3, size[4] = 4  → size[pu]=3 < size[pv]=4 → TRUE, first branch

parent[1] = 4
size[4] += size[1]  →  size[4] = 7
```

```
Node:    1   2   3   4   5   6   7
parent:  4   1   1   4   4   4   4
size:    3   1   1   7   1   2   1
```

**Final checkpoint:**

```
findUParent(1):
   1 != parent[1] (which is 4) → recurse
   findUParent(4): 4 == parent[4] → return 4
   findUParent(1) returns 4

findUParent(4) = 4

4 == 4 → YES, same component. Matches every prior trace.
```

---

## Comparing the Final State — Rank vs Size

This is worth pausing on, because it's a genuinely interesting confirmation:

```
Union by Rank (Part 3, final):        Union by Size (this part, final):

Node:    1  2  3  4  5  6  7          Node:    1  2  3  4  5  6  7
parent:  4  1  1  4  4  4  4          parent:  4  1  1  4  4  4  4
```

**The final `parent[]` array is byte-for-byte identical.** Both approaches converge to the exact same tree shape on this particular graph — node 4 as the single root, with 1, 5, 6, 7 as direct children and 2, 3 as grandchildren through 1.

This isn't a coincidence specific to this graph, but it's also not a general guarantee — both strategies make the *same kind* of "attach smaller under larger" decision at every step, and on a graph where rank and size happen to track each other consistently (which is common but not universal), you'll often see them agree exactly like this. The takeaway isn't "they always produce identical trees" — it's that **both are equally valid, correctness-preserving strategies for the same underlying goal.**

---

## Full Java Implementation — Union by Size

Same structural pattern as Part 4's `DisjointSet` class — only the merge criterion changes.

```java
import java.util.*;

class DisjointSetBySize {

    // size[i] → the ACTUAL current number of nodes in the component
    //           rooted at i. Always exact, unlike rank — even though
    //           entries for non-root nodes go stale and unused, the
    //           value at any current ROOT is always trustworthy.
    // parent[i] → the node directly above i in its component's tree
    List<Integer> size = new ArrayList<>();
    List<Integer> parent = new ArrayList<>();

    public DisjointSetBySize(int n) {
        for (int i = 0; i <= n; i++) {
            size.add(1);       // every node starts as its own component of size 1
            parent.add(i);     // every node starts as its own parent
        }
    }

    // Identical to the rank version — path compression doesn't care
    // whether the union strategy is rank-based or size-based, it
    // operates purely on the parent[] chain.
    public int findUParent(int node) {
        if (node == parent.get(node)) {
            return node;
        }
        int ultimateParent = findUParent(parent.get(node));
        parent.set(node, ultimateParent);
        return parent.get(node);
    }

    public void unionBySize(int u, int v) {
        int ultimateParentU = findUParent(u);
        int ultimateParentV = findUParent(v);

        if (ultimateParentU == ultimateParentV) {
            return;
        }

        if (size.get(ultimateParentU) < size.get(ultimateParentV)) {
            // u's component is smaller → attach it under v's component
            parent.set(ultimateParentU, ultimateParentV);
            size.set(ultimateParentV,
                     size.get(ultimateParentV) + size.get(ultimateParentU));

        } else {
            // Covers BOTH "v's component is smaller" AND "equal size" —
            // attach v's component under u's component either way
            parent.set(ultimateParentV, ultimateParentU);
            size.set(ultimateParentU,
                     size.get(ultimateParentU) + size.get(ultimateParentV));
        }
    }
}
```

### Wiring It Up — Reproducing the Same Checkpoints

```java
public class Main {
    public static void main(String[] args) {

        DisjointSetBySize ds = new DisjointSetBySize(7);

        ds.unionBySize(1, 2);
        ds.unionBySize(2, 3);
        ds.unionBySize(4, 5);
        ds.unionBySize(6, 7);

        // Checkpoint after edge (6,7):
        System.out.println(
            ds.findUParent(1) == ds.findUParent(4)
                ? "Same component" : "Not same component"
        );
        // Expected: "Not same component"

        ds.unionBySize(5, 6);
        ds.unionBySize(3, 7);

        // Final checkpoint:
        System.out.println(
            ds.findUParent(1) == ds.findUParent(4)
                ? "Same component" : "Not same component"
        );
        // Expected: "Same component"
    }
}
```

**Expected output** (identical to the rank version's output in Part 4):
```
Not same component
Same component
```

---

## Complexity — Identical to Union by Rank

```
┌──────────────────────────────────────────────────────────────────┐
│  Union by Size + Path Compression achieves the EXACT same        │
│  O(4α) amortized bound as Union by Rank + Path Compression.      │
│                                                                  │
│  This makes sense structurally: both are "attach smaller         │
│  under larger" strategies. Rank and size are just two            │
│  different ways of measuring "which one is smaller" —            │
│  the underlying tree-flattening mechanics (and therefore the     │
│  complexity proof) are the same either way.                      │
│                                                                  │
│  findUParent(node)  → O(4α)                                      │
│  unionBySize(u, v)  → O(4α)                                      │
│  Space              → O(N)  (parent[] + size[] arrays)           │
└──────────────────────────────────────────────────────────────────┘
```

---

## Rank vs Size — The Practical Comparison

```
┌───────────────────────┬──────────────────────────┬──────────────────────────┐
│ Aspect                │ Union by Rank            │ Union by Size            │
├───────────────────────┼──────────────────────────┼──────────────────────────┤
│ What's compared       │ Approximate tree height  │ Exact node count         │
│ Meaning after         │ Distorted — no longer    │ Still exact and          │
│ compression           │ literally "height"       │ meaningful               │
│ Interviewer intuition │ Slightly more abstract   │ Generally easier to      │
│                       │                          │ explain out loud         │
│ Time complexity       │ O(4α)                    │ O(4α) — identical        │
│ Space complexity      │ O(N)                     │ O(N) — identical         │
│ Can mix the two?      │ NO — pick one per instance, never combine           │
└───────────────────────┴──────────────────────────┴──────────────────────────┘
```

Striver's personal take, stated directly in the lecture: he finds size **more intuitive** than rank, specifically because rank stops corresponding to anything concrete once path compression starts firing, while size always means exactly what it says. Either one is completely correct and commonly seen in solutions — this is genuinely a matter of preference, not one being "more correct" than the other.

---

## The Complete DSU Theory — Tied Together

```
┌──────────────────────────────────────────────────────────────────┐
│  DISJOINT SET (UNION-FIND) — FULL PICTURE                        │
│                                                                  │
│  Problem solved: answer "same component?" queries in ~O(1),      │
│  even as the graph is built up dynamically, edge by edge.        │
│                                                                  │
│  Two operations:                                                 │
│    findUParent(node) → the root/representative of node's tree    │
│    union(u, v)        → merges the two components                │
│                                                                  │
│  Two equally valid union strategies:                             │
│    Union by Rank → compare approximate tree height               │
│    Union by Size → compare exact node count                      │
│    (pick ONE per instance — never mix)                           │
│                                                                  │
│  One mandatory optimization:                                     │
│    Path Compression → flatten every visited path to point        │
│    directly at the root, as a side effect of every findUParent   │
│    call                                                          │
│                                                                  │
│  Combined complexity: O(4α) per operation — effectively O(1)     │
│  for any N you will ever encounter in practice.                  │
│                                                                  │
│  What's next: Kruskal's Algorithm — the second MST algorithm,    │
│  which is literally impossible to implement without this exact   │
│  data structure.                                                 │
└──────────────────────────────────────────────────────────────────┘
```

---

## Quick Checkpoint Before Moving On

- [ ] Why does the final `parent[]` array come out identical between the rank and size implementations on this particular graph?
- [ ] Why is `size[6] = 2` after Edge 5 considered "stale" — and why is that completely harmless?
- [ ] Trace by hand: what would `size[4]` be immediately after Edge 6, and why does node 1's component (size 3) attach *under* node 4's component (size 4) rather than the other way around?
- [ ] Why must you never mix `rank[]`-based and `size[]`-based logic within the same Disjoint Set instance?
- [ ] In your own words, why does Striver find Union by Size more intuitive than Union by Rank?

---

That closes out the full DSU theory — motivation, Union by Rank, Path Compression, full Java, and Union by Size, all traced consistently through the same seven-node graph. Whenever you're ready, send over the next piece — whether that's **Kruskal's Algorithm** or the first DSU problem from Pattern 11 (LC 323, LC 261, etc.).