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
| BST Properties & Validation | "validate BST", "kth smallest", "range sum", "mode", "min distance" |
| BST Navigation | "inorder successor", "closest value", "LCA in BST", "floor/ceiling" |
| BST Construction & Transformation | "build BST from", "convert to", "serialize BST", "split BST" |
| BST Iterator | "iterator", "merge two BSTs", "next element lazily" |
| BST Repair & Structural Reasoning | "recover BST", "two swapped nodes", "largest valid BST subtree" |
| BST + DP | "count unique BSTs", "number of ways", "how many structures" |
| Ordered Set Applications | "no overlap", "booking", "dynamic min/max", "floor/ceiling queries on stream" |

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

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 98. Validate Binary Search Tree | Preferred: min/max bounds downward. Alternative: inorder strictly increasing |
| 2 | LC 501. Find Mode in Binary Search Tree | Inorder traversal — track current streak; no extra space needed |
| 3 | LC 530. Minimum Absolute Difference in BST | Inorder — minimum difference always between adjacent elements in sorted order |
| 4 | LC 783. Minimum Distance Between BST Nodes | Same as LC 530 — different problem number, identical solution; do only one |
| 5 | LC 938. Range Sum of BST | Prune left if root.val ≤ low, prune right if root.val ≥ high |
| 6 | LC 230. Kth Smallest Element in a BST | Inorder — kth element in sorted order; follow-up: augment BST with subtree sizes for O(h) |

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

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 108. Convert Sorted Array to BST | Mid of array is root — recursively build halves; guarantees height-balanced |
| 2 | LC 109. Convert Sorted List to BST | Same idea — find mid of linked list using slow/fast pointers |
| 3 | LC 1008. Construct BST from Preorder Traversal | Value bounds — each value must fall within valid (min, max) range |
| 4 | LC 449. Serialize and Deserialize BST | Preorder serialization — BST property reconstructs tree without needing inorder |
| 5 | GFG: Construct BST from Postorder | Last element is root — same bounds logic as preorder construction |
| 6 | LC 538. Convert BST to Greater Tree | Reverse inorder (right → root → left) — running suffix sum added to each node |
| 7 | LC 426. Convert BST to Sorted Doubly Linked List | Inorder — link prev and curr as you go; connect tail back to head at end |
| 8 | LC 1382. Balance a Binary Search Tree | Inorder to sorted array → apply LC 108 to rebuild balanced |
| 9 | LC 776. Split BST | Recursion — if root.val ≤ target, root and left stay, split right recursively; else root and right move, split left recursively |

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

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 729. My Calendar I | TreeMap — for each interval use floorKey/ceilingKey to check overlap |
| 2 | LC 731. My Calendar II | Two TreeMaps — single bookings and double bookings; reject on triple overlap |
| 3 | LC 2034. Stock Price Fluctuation | Two ordered sets — one for timestamps, one for prices (enables O(log n) min/max) |
| 4 | LC 220. Contains Duplicate III | Sliding window + ordered set — find if any value in window falls within [val-t, val+t] |
| 5 | LC 480. Sliding Window Median | Two ordered multisets — balance sizes; median lives at the boundary between them |

---

## Final Summary

| Pattern | Problems | Core Insight |
|---|---|---|
| Basic BST Operations | 3 | Three-case delete is the hard one |
| BST Properties & Validation | 6 | Inorder = sorted array — most properties reduce to this |
| BST Navigation | 6 | BST ordering enables O(h) directed navigation |
| BST Construction & Transformation | 9 | Preorder alone reconstructs BST |
| BST Iterator & Traversal Tricks | 4 | Explicit stack simulates recursive inorder lazily |
| BST Repair & Structural Reasoning | 2 | Detect violations, repair or measure validity |
| BST + DP Crossover | 3 | BST as model for counting DP |
| Ordered Set Applications | 5 | BST as tool for dynamic sorted queries |
| **Total** | **~38 problems** | |
