# BST / Ordered Set DSA Patterns

*"Problems are infinite, but patterns are finite!"*

---

## BST Core Property — The Foundation of Everything

Before any pattern: **In a BST, inorder traversal gives sorted order.** Every BST pattern is either exploiting this directly or maintaining it through modifications.

| Property | Consequence |
|---|---|
| Left subtree values < root | Search/insert/delete in O(h) |
| Right subtree values > root | Inorder = sorted sequence |
| Inorder = sorted | Kth smallest, validation, successor all become O(h) |
| Structure encodes order | Preorder alone is enough to reconstruct the BST — unlike general binary tree which needs preorder + inorder |

---

## Pattern Identification Quick Reference

| Pattern | Trigger |
|---|---|
| Basic BST Operations | "search", "insert", "delete" in BST |
| BST Properties & Validation | "validate BST", "kth smallest", "range sum", "mode", "min distance", "count smaller after self" |
| BST Navigation | "inorder successor", "closest value", "LCA in BST", "floor/ceiling" |
| BST Construction & Transformation | "build BST from", "convert to", "serialize BST", "split BST", "trim BST", "verify preorder sequence" |
| BST Iterator | "iterator", "merge two BSTs", "next element lazily" |
| BST Repair & Structural Reasoning | "recover BST", "two swapped nodes", "largest valid BST subtree" |
| BST + DP | "count unique BSTs", "number of ways", "how many structures" |
| Ordered Set Applications | "no overlap", "booking", "max concurrent bookings", "merge intervals dynamically", "add/remove range", "dynamic min/max", "floor/ceiling queries on stream" |

---

## Pattern 1: Basic BST Operations

**Identify:** The three fundamental operations. Every other BST problem builds on these. Master both recursive and iterative versions.

**Key insight for Delete:** Three cases — node is a leaf (just remove), node has one child (replace with child), node has two children (replace value with inorder successor, then delete inorder successor).

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 700. Search in a Binary Search Tree | Base template — go left if target < root, right if target > root |
| 2 | LC 701. Insert into a Binary Search Tree | Same navigation as search — insert at the correct null position |
| 3 | LC 450. Delete Node in a BST | Three cases — leaf, one child, two children with inorder successor replacement |

---

## Pattern 2: BST Properties & Validation

**Identify:** Problems that verify the BST property or exploit the sorted-inorder property to answer queries. Recognize that **inorder traversal of a BST is a sorted sequence** — most BST property problems reduce to problems on a sorted array.

**On Validate BST:** Two valid approaches exist. Preferred: pass valid (min, max) bounds downward — more robust and directly transferable to other BST validation problems. Alternative: verify inorder traversal is strictly increasing — correct but easier to implement incorrectly if you only compare adjacent nodes without tracking the running maximum.

**On order-statistics augmentation:** LC 230's follow-up (frequent kth-smallest queries under modification) hints at augmenting a BST with subtree sizes. LC 315 is where that idea becomes the *primary* technique rather than a footnote — worth doing right after LC 230 while that intuition is fresh. This same augmented-BST rank-query idea is also the conceptual bridge to Binary Indexed Trees (Fenwick) and Segment Trees for the online-update variant of this problem — but those data structures belong in a separate advanced/competitive-programming sheet, not here, since they're rarely the expected first solution in a standard interview.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 98. Validate Binary Search Tree | Preferred: min/max bounds downward. Alternative: inorder strictly increasing |
| 2 | LC 501. Find Mode in Binary Search Tree | Inorder traversal — track current streak; no extra space needed |
| 3 | LC 530. Minimum Absolute Difference in BST | Inorder — minimum difference always between adjacent elements in sorted order |
| 4 | LC 783. Minimum Distance Between BST Nodes | Same as LC 530 — different problem number, identical solution; do only one |
| 5 | LC 938. Range Sum of BST | Prune left if root.val ≤ low, prune right if root.val ≥ high |
| 6 | LC 230. Kth Smallest Element in a BST | Inorder — kth element in sorted order; follow-up: augment BST with subtree sizes for O(h) |
| 7 | LC 315. Count of Smaller Numbers After Self | Order-statistics BST — insert values right-to-left into a BST augmented with subtree/left-count sizes; each insertion's left-count-passed gives the answer for that element in O(h) |

---

## Pattern 3: BST Navigation

**Identify:** Navigate the BST using ordering property to find specific nodes in O(h) rather than O(n). The BST property lets you make directional decisions at each node — go left, go right, or current node is the answer.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 270. Closest Binary Search Tree Value | Navigate — track closest seen; go left if target < root, right otherwise |
| 2 | LC 272. Closest Binary Search Tree Value II | Inorder + sliding window of k closest values |
| 3 | LC 285. Inorder Successor in BST | Successor = leftmost in right subtree, or lowest ancestor where node is in left subtree |
| 4 | LC 510. Inorder Successor in BST II | Same logic but with parent pointer — no need to traverse from root |
| 5 | LC 235. Lowest Common Ancestor of a BST | Both < root → go left; both > root → go right; else root is LCA |
| 6 | LC 2476. Closest Nodes Queries in a Binary Search Tree | For each query find floor and ceiling — inorder array + binary search |

---

## Pattern 4: BST Construction & Transformation

**Identify:** Build a BST from given data or transform a BST into another structure. Key insight: **BST can be reconstructed from preorder alone** — the ordering property encodes what inorder would give you, making BST serialization more compact than general tree serialization.

**On structural pruning vs. value pruning:** LC 938 (Pattern 2) *skips over* subtrees during a read-only sum — it never modifies the tree. LC 669 below is a different technique entirely: it *restructures* the tree, splicing valid subtrees upward and discarding invalid ones. Don't conflate the two just because both involve a `[low, high]`-style bound.

**On preorder + BST validity:** LC 1008 builds a tree from a preorder sequence using ceiling propagation. LC 255 below asks the inverse question — *given* a sequence, could it have come from *some* valid BST's preorder? — solved with the same ceiling idea, but via a monotonic stack instead of recursion.

**Variant worth knowing (no dedicated note needed):** *Convert a Binary Tree to a BST* (GFG/Amazon) — given a general binary tree's *shape*, overwrite node values so the tree becomes a valid BST, without changing structure. The technique is a direct combination of things you already know: inorder-traverse to extract all values (BT Pattern 1), sort them (trivial), then inorder-traverse again to overwrite values in the same positions. It's a good "wait, can you also do it this way" follow-up to have in your back pocket, but it doesn't introduce new BST-specific reasoning beyond "inorder position = sorted rank" — not worth a full three-stage note on its own.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 108. Convert Sorted Array to BST | Mid of array is root — recursively build halves; guarantees height-balanced |
| 2 | LC 109. Convert Sorted List to BST | Same idea — find mid of linked list using slow/fast pointers |
| 3 | LC 1008. Construct BST from Preorder Traversal | Value bounds — each value must fall within valid (min, max) range |
| 4 | LC 255. Verify Preorder Sequence of a Binary Search Tree | Monotonic stack + running lower bound — pop while descending, last popped value becomes the floor; a value below the floor means no valid BST could produce this sequence |
| 5 | LC 449. Serialize and Deserialize BST | Preorder serialization — BST property reconstructs tree without needing inorder |
| 6 | LC 538. Convert BST to Greater Tree | Reverse inorder (right → root → left) — running suffix sum added to each node |
| 7 | LC 426. Convert BST to Sorted Doubly Linked List | Inorder — link prev and curr as you go; connect tail back to head at end |
| 8 | LC 1382. Balance a Binary Search Tree | Inorder to sorted array → apply LC 108 to rebuild balanced |
| 9 | LC 776. Split BST | Recursion — if root.val ≤ target, root and left stay, split right recursively; else root and right move, split left recursively |
| 10 | LC 669. Trim a Binary Search Tree | Structural splice, not just skip — when a node falls outside [low, high], discard it and recursively promote whichever trimmed child subtree survives in its place |
| 11 | GFG: Construct BST from Postorder | Last element is root — same bounds logic as preorder construction |

---

## Pattern 5: BST Iterator & Traversal Tricks

**Identify:** Problems requiring lazy inorder traversal — get the next element on demand without pre-computing the full sequence. Core trick: simulate the call stack of recursive inorder using an explicit stack with a `pushLeft()` helper.

**Why this is its own pattern:** The iterator is a reusable component. Once built, Two Sum BSTs and All Elements in Two BSTs both become merge problems powered by two iterator instances. Seeing the iterator as a component — not just a standalone problem — is the key insight.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 173. Binary Search Tree Iterator | Explicit stack + pushLeft() helper simulates recursive inorder lazily |
| 2 | LC 653. Two Sum IV — Input is a BST | Two iterators — one forward, one backward — classic two-pointer on BST |
| 3 | LC 1214. Two Sum BSTs | Two iterators across two separate BSTs — forward on one, backward on other |
| 4 | LC 1305. All Elements in Two Binary Search Trees | Two iterator stacks — merge two inorder sequences like merge sort |

---

## Pattern 6: BST Repair & Structural Reasoning

**Identify:** Tree structure is broken or you need to reason about BST validity across the whole tree. Recognition signal — "two nodes are swapped", "find the largest valid BST", "is this subtree a valid BST". These combine inorder violation detection with structural repair or measurement.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 99. Recover Binary Search Tree | Inorder — find two nodes out of order (swapped); track first and second violation, swap values back |
| 2 | LC 1373. Maximum Sum BST in Binary Tree | Postorder returning (min, max, sum, isValid) — find max sum among all valid BST subtrees |

---

## Pattern 7: BST + DP Crossover

**Identify:** BST structure drives a DP recurrence. You are not traversing a given BST — you are counting or optimizing over the space of possible BST structures. Catalan numbers underlie problems 1 and 2.

**Prerequisite:** These require solid DP foundation (1D DP and combinatorics). Do these after completing the DP sheet if your DP is not solid yet.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 96. Unique Binary Search Trees | dp[n] = sum of dp[i-1] * dp[n-i] for i in 1..n — Catalan number recurrence |
| 2 | LC 95. Unique Binary Search Trees II | Recursion + memoization — generate all structurally unique BSTs for range [lo, hi] |
| 3 | LC 1569. Number of Ways to Reorder Array to Get Same BST | Combinatorics — count interleavings of left and right subtree elements preserving relative order |

---

## Pattern 8: Ordered Set Applications

**Identify:** Problems where you need to dynamically maintain a sorted collection and answer floor/ceiling/min/max queries efficiently. You are using BST *as a tool* — not traversing a given BST.

**Language mapping:** Java → `TreeMap`/`TreeSet`. C++ → `std::map`/`std::set`. Python → `SortedList` from `sortedcontainers`.

**Why this is its own pattern:** These problems have nothing to do with tree traversal. The BST is the underlying engine powering O(log n) sorted insertion and range queries. Recognizing *when* to reach for an ordered set is a completely distinct skill from BST traversal.

**On the Calendar trilogy:** LC 729 rejects any overlap, LC 731 rejects only triple-overlaps — but neither tells you *how much* overlap exists at any moment. LC 732 completes the trilogy by asking for the maximum simultaneous booking count, via a sweep-line built on a TreeMap. Interviewers who ask I or II frequently follow up with III — don't stop short.

**On dynamic interval merging vs. overlap checking:** The Calendar trilogy answers "does this new interval conflict with existing ones?" — none of them require *merging* adjacent/touching intervals into a single disjoint interval, or *splitting* an interval when part of it is later removed. LC 352 and LC 715 below test that distinct skill — using `floorKey`/`ceilingKey` to detect adjacency and fuse or split ranges, not just detect overlap. This is a commonly tested "next level" beyond the Calendar problems at Amazon/Bloomberg-style interviews.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 729. My Calendar I | TreeMap — for each interval use floorKey/ceilingKey to check overlap |
| 2 | LC 731. My Calendar II | Two TreeMaps — single bookings and double bookings; reject on triple overlap |
| 3 | LC 732. My Calendar III | Sweep line via TreeMap — +1 at every interval start, -1 at every end; running prefix sum across sorted keys gives max concurrent overlap |
| 4 | LC 352. Data Stream as Disjoint Intervals | TreeMap of disjoint intervals — on addNum, use floorKey/ceilingKey to check if the new number touches/merges neighboring intervals or forms a new standalone one |
| 5 | LC 715. Range Module | TreeMap of disjoint intervals — addRange merges overlapping/adjacent ranges; removeRange may split an existing interval into two; queryRange checks full coverage |
| 6 | LC 2034. Stock Price Fluctuation | Two ordered sets — one for timestamps, one for prices (enables O(log n) min/max) |
| 7 | LC 220. Contains Duplicate III | Sliding window + ordered set — find if any value in window falls within [val-t, val+t] |
| 8 | LC 480. Sliding Window Median | Two ordered multisets — balance sizes; median lives at the boundary between them |

---

## Final Summary

| Pattern | Problems | Core Insight |
|---|---|---|
| Basic BST Operations | 3 | Three-case delete is the hard one |
| BST Properties & Validation | 7 | Inorder = sorted array — most properties reduce to this; augmentation unlocks order statistics |
| BST Navigation | 6 | BST ordering enables O(h) directed navigation |
| BST Construction & Transformation | 11 | Preorder alone reconstructs BST — and lets you verify or trim just as cheaply |
| BST Iterator & Traversal Tricks | 4 | Explicit stack simulates recursive inorder lazily |
| BST Repair & Structural Reasoning | 2 | Detect violations, repair or measure validity |
| BST + DP Crossover | 3 | BST as model for counting DP |
| Ordered Set Applications | 8 | BST as tool for dynamic sorted queries — overlap checking, merging, splitting, sweep-line counting |
| **Total** | **~44 problems** | |

---

**Out of scope for this sheet (flagged, not forgotten):**
- **Tries (Prefix Trees)** — LC 208, 211, 212, etc. Technically trees, frequently asked alongside BSTs, but governed by prefix/character-array structure rather than the left-<-root-<-right ordering property. Deserves its own sheet.
- **Fenwick Tree / Segment Tree / Sparse Table** — the natural upgrade path from LC 315's augmented-BST technique for online-update variants, but rarely the expected first solution in a standard interview. Belongs in a separate advanced/competitive-programming sheet alongside Tries.

---

## How to Use This Sheet

**Order of attack:** Patterns 1 → 8 are sequenced by dependency, not difficulty alone. Don't skip ahead — later patterns silently assume you've internalized earlier ones:

- **Pattern 1 (Basic Operations)** is the foundation for everything. The directional "go left / go right" compass you learn here reappears in Patterns 2, 3, and 4 without being re-explained.
- **Pattern 2 (Properties & Validation)** and **Pattern 3 (Navigation)** both lean on "inorder = sorted" and "one comparison eliminates a subtree" — master these two ideas here, because Patterns 4-8 assume them as background knowledge, not something to re-derive.
- **Pattern 7 (BST + DP)** has a hard prerequisite: solid 1D DP and combinatorics. If your DP foundation isn't there yet, skip this pattern until after the DP sheet, then come back — don't force it early.
- **Pattern 8 (Ordered Set)** is the most different in *flavor* from the rest — you're using a BST as a black-box tool (`TreeMap`/`TreeSet`), not traversing a given tree. It's fine to jump here early if you're comfortable with the language's ordered-collection API, since it doesn't depend on Patterns 2-7.

---