# Binary Tree DSA Patterns

*"Problems are infinite, but patterns are finite!"*

---

## The Core Mental Model — Before Any Pattern

Every binary tree problem is a choice between three things: **which traversal order**, **what direction information flows** (top-down via parameters, bottom-up via return values, or both at once), and **what a node returns to its parent** (nothing, a single value, or a tuple of states).

**The traversal-order decision, stated plainly:**
- **Preorder (root, left, right):** use when the parent must hand something *down* to its children before they act — a running sum, a depth, a bound.
- **Postorder (left, right, root):** use whenever a node's answer depends on its children's answers first. This is the default for almost every "compute a property" problem in this sheet — height, balance, diameter, any DP-on-tree state.
- **Level order (BFS):** use whenever the question is inherently about "which level" or "what's visible from the side" — position, not structure, is what matters.

**The single most common bug in this entire topic:** conflating "what this node returns to its parent" with "what the final answer is." Diameter and Path Sum III both need a **global variable** for the answer, while the **return value** carries only the one piece of information the parent can actually use (a height, a boolean, a single-direction extension). The moment you're tempted to return "the best of everything below me" instead of "the one number my parent needs," stop — that's the Path pattern's central trap, and it resurfaces in Tree DP under a different name.

---

## Pattern Identification Quick Reference

| Pattern | Trigger |
|---|---|
| Tree Traversals | "preorder/inorder/postorder", "iterative traversal using a stack" |
| Tree Properties | "height", "balanced", "diameter", "symmetric", "count nodes/subtrees satisfying X" |
| Level Order Traversal | "level by level", "zigzag", "side view", "vertical order", "connect next pointers", "burn/spread from a node" |
| Tree Construction | "build a tree from preorder/inorder/postorder arrays" |
| Binary Tree Paths | "root-to-leaf", "any node to any node", "path sum", "lowest common ancestor" |
| Tree DP | node's return value is a **tuple of states**, not one number — "rob or don't rob", "camera covered/uncovered", "largest valid BST subtree" |
| Tree Transformation | "invert", "flatten", "delete nodes", "restructure in place" |
| Tree Hashing | "find duplicate subtrees", "is this a subtree of that tree" — canonical serialization as a fingerprint |
| Two-Tree Problems | two trees given simultaneously — "same tree", "merge", "flip equivalent" |
| N-ary Tree | generalization of binary tree patterns to a `children[]` list instead of `left`/`right` |

---

## Pattern 1: Tree Traversals (Core Templates)

**Identify:** Pure traversal mechanics — no property to compute, no path to track. Everything else in this sheet builds on these. Know both recursive **and** iterative for all three DFS orders; interviewers frequently ask for the iterative version specifically to test stack understanding, since the recursive version hides the stack inside the call frame.

**Why postorder's iterative version is the hard one:** Preorder and inorder each visit a node exactly once as they descend or between children — a single stack with a straightforward push order handles both. Postorder visits a node only *after* both children, which means the naive stack walk needs to know whether it's returning from the left child or the right child before it can safely process the current node. The two-stack trick sidesteps this by computing a *reversed preorder* (root, right, left) and reversing the output — the one-stack version instead tracks the last node visited to decide whether it's safe to process the current top of stack.

**Morris Traversal — know it exists, learn it after the stack-based versions are automatic:** O(1) space traversal using threaded pointers — temporarily rewires a node's inorder predecessor's right pointer to point back to the current node, then removes the thread once it's been used to return. Rarely expected at junior level; appears at senior/FAANG level as a "can you do this without a stack" follow-up.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 144. Binary Tree Preorder Traversal | Root, then left, then right — base recursive template |
| 2 | LC 94. Binary Tree Inorder Traversal | Left, then root, then right |
| 3 | LC 145. Binary Tree Postorder Traversal | Left, then right, then root |
| 4 | Iterative Preorder Traversal | Single stack — push right child then left child, process on pop |
| 5 | Iterative Inorder Traversal | Single stack — push the entire left chain, process, then move right |
| 6 | Iterative Postorder (Two Stacks, and One Stack) | Two-stack: compute reversed preorder (root, right, left), reverse the output. One-stack: track the last-visited node to know when it's safe to process the current top |
| 7 | GFG: All Three Traversals Using a Single Stack | One stack of `(node, state)` pairs — a single pass produces preorder, inorder, and postorder simultaneously |

---

## Pattern 2: Tree Properties

**Identify:** Compute or verify a structural property of the tree. Almost always postorder — children must report their answer up before the current node can compute its own. The key skill is distinguishing what value gets **returned** to the parent from what gets **updated globally** (a running maximum, a counter) — the same distinction the Core Mental Model flags, showing up here for the first time.

**Note on DFS state propagation (LC 1026, LC 1448):** Several problems here pass a constraint *downward* through parameters (the max/min seen so far on the root-to-node path) while also computing something to return upward. This isn't a separate pattern — it's still a property computation, just one where the "property" depends on ancestors, not only descendants. The traversal direction downward is a tool, not a new mental model.

**LC 222 — exploiting completeness, not brute-forcing it:** A naive node count is O(n). A complete tree lets you compare the height of the leftmost and rightmost paths from any node: if they're equal, that subtree is a perfect tree and its size is computable by formula in O(1); if not, recurse into both children. This collapses to O(log² n) — the completeness guarantee is what licenses skipping full traversal.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 104. Maximum Depth of Binary Tree | Postorder — `1 + max(leftDepth, rightDepth)` |
| 2 | LC 110. Balanced Binary Tree | Postorder returning height; return a sentinel (e.g. -1) on imbalance to short-circuit and prune early |
| 3 | LC 543. Diameter of Binary Tree | Postorder returning height; global max updated with `leftHeight + rightHeight` at every node |
| 4 | LC 101. Symmetric Tree | Mirrored recursion — compare `left.left` vs `right.right` and `left.right` vs `right.left` simultaneously |
| 5 | LC 250. Count Univalue Subtrees | Postorder — a subtree is univalue only if both children are univalue AND match the root's value |
| 6 | LC 222. Count Complete Tree Nodes | Compare left-spine height vs right-spine height — equal means a perfect subtree, computable by formula; O(log² n) |
| 7 | LC 958. Check Completeness of a Binary Tree | BFS with explicit null markers — completeness fails if any non-null node appears after the first null |
| 8 | LC 662. Maximum Width of Binary Tree | BFS with positional indexing (`2*i`, `2*i+1` per level) — width = last index minus first index, plus one |
| 9 | LC 1026. Maximum Difference Between Node and Ancestor | Pass the running min and max seen on the path down; update the answer at every node |
| 10 | LC 1448. Count Good Nodes in Binary Tree | Pass the running max seen on the path down; count a node as "good" when it's ≥ that max |

---

## Pattern 3: Level Order Traversal

**Identify:** The question needs the tree processed level by level, or needs positional/coordinate information (column, row, side visibility) that only makes sense in a BFS framing. Core tool: a queue, with **level separation** via a size snapshot (`levelSize = queue.size()` before the inner loop) — this single line is what turns a plain BFS into a level-aware one.

**Boundary Traversal — a level-aware DFS/BFS combination, not a new mechanic:** Walk the left boundary top-to-bottom (excluding leaves), then all leaves left-to-right (a plain DFS), then the right boundary bottom-to-top (excluding leaves) — three separate, ordinary walks stitched together, with care taken not to double-count a node that is both a boundary node and a leaf.

**LC 863 / Burning Tree — BFS on a tree read as an undirected graph:** A binary tree's DFS-only pointers (`left`, `right`) don't let you walk *upward*. Build a `child → parent` map with one DFS pass first, then run ordinary multi-level BFS from the target/start node treating the tree as an undirected graph. This is the same "no explicit reverse edge, so build one first" instinct that recurs in the Graph sheet.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 102. Binary Tree Level Order Traversal | Base BFS template — queue + level-size snapshot |
| 2 | LC 107. Binary Tree Level Order Traversal II | Same BFS, reverse the collected result |
| 3 | LC 637. Average of Levels in Binary Tree | BFS + running sum/count per level |
| 4 | LC 103. Binary Tree Zigzag Level Order Traversal | BFS + alternate append direction (or reverse) every other level |
| 5 | LC 116. Populating Next Right Pointers in Each Node | Perfect tree — use already-established `next` pointers from the level above to link the level below in O(1) space |
| 6 | LC 117. Populating Next Right Pointers in Each Node II | Arbitrary tree — same idea, but a dummy head node per level is needed since children aren't guaranteed to exist |
| 7 | GFG: Boundary Traversal of Binary Tree | Left boundary (top-down) + leaves (left-to-right DFS) + right boundary (bottom-up) — stitched, not one mechanism |
| 8 | LC 987. Vertical Order Traversal of a Binary Tree | DFS/BFS assigning `(column, row)` coordinates; sort by column, then row, then value |
| 9 | GFG: Top View of Binary Tree | BFS with column tracking — keep only the *first* node seen at each column |
| 10 | GFG: Bottom View of Binary Tree | Same column tracking — keep the *last* node seen at each column |
| 11 | LC 199. Binary Tree Right Side View | BFS take the last node per level, or DFS right-first while tracking depth |
| 12 | LC 297. Serialize and Deserialize Binary Tree | BFS-based encoding with explicit null markers — reconstructs unambiguously without needing a second traversal order |
| 13 | LC 863. All Nodes Distance K in Binary Tree | Build parent pointers via one DFS pass, then BFS from the target node as an undirected graph |
| 14 | LC 2385 / GFG: Burning Tree | Same parent-map + BFS as LC 863 — BFS level count from the start node is the answer |

---

## Pattern 4: Tree Construction

**Identify:** Build a tree from given traversal arrays, or from a value sequence with an implicit ordering rule (BST). Core insight: **preorder or postorder tells you the root; inorder tells you where the left/right split happens.** Always use a hashmap for O(1) index lookup into the inorder array — a linear scan per call degrades the whole algorithm to O(n²).

**Theory — what's actually needed to reconstruct a unique tree:** Inorder plus one of {preorder, postorder} always uniquely determines a binary tree, because inorder gives the left/right split and the other array gives the root at every level. Preorder + postorder *alone*, without inorder, is only sufficient when the tree is guaranteed **full** (every node has 0 or 2 children, never exactly 1) — this is exactly why LC 889 needs a different boundary-finding trick than LC 105/106.

**LC 889 — the one case without inorder:** The root is `preorder[0]`. The *next* value in preorder is the root of the left subtree — find that value's position in postorder, and everything up to and including it (in postorder) is the left subtree; everything after is the right subtree. This only works because fullness guarantees no ambiguity about which side a lone child belongs to.

**LC 1008 — BST property replaces the need for inorder entirely:** Because a BST's inorder is *always* its sorted order, you never need to pass a separate inorder array — a simple `(min, max)` bound recursion on the preorder array alone reconstructs the tree.

| # | Problem | Key Concept |
|---|---|---|
| 1 | Theory: Requirements to Construct a Unique Binary Tree | Inorder + (preorder or postorder) always suffices; preorder + postorder alone only works if the tree is full |
| 2 | LC 105. Construct Binary Tree from Preorder and Inorder | Root from preorder[0], hashmap lookup in inorder for the split |
| 3 | LC 106. Construct Binary Tree from Postorder and Inorder | Root from the end of postorder, same inorder-split logic |
| 4 | LC 889. Construct Binary Tree from Preorder and Postorder | Full-tree-only technique — next preorder value located in postorder marks the left/right boundary |
| 5 | LC 1008. Construct BST from Preorder Traversal | Value-bound recursion — BST property makes inorder unnecessary |

---

## Pattern 5: Binary Tree Paths

**Identify:** Problems involving paths — root-to-leaf, root-to-node, or any-node-to-any-node — plus lowest common ancestor, which is really "the meeting point of two paths." This is the hardest cluster in the sheet, precisely because of the return-value-vs-global-answer trap named in the Core Mental Model.

**The two families, and why confusing them is the single most common mistake here:**
- **Root-anchored paths** (must start at the root): pass a running value **down** through parameters — a remaining target sum, a running number being built digit by digit.
- **Any-path problems** (can start and end anywhere): postorder, update a **global** answer at every node, and return to the parent only the best *single-direction* extension — never both directions combined. The parent can only continue the path in one direction (through this node into just one child); the "both directions meet here" value is only valid as *this node's* contribution to the global answer, and must never be returned upward, or the parent will silently double-count a bent path as if it were straight.

**LC 437 — a path-sum problem that is secretly Prefix Sum + HashMap on a tree:** "Any downward, contiguous path summing to target" is not the any-direction-any-path case above — it's directional (parent to descendant) but not root-anchored. The fix: maintain a running prefix sum down each root-to-node path in a hashmap (exactly LC 560's `prefix[r] - k` lookup from the Prefix Sum sheet), backtracking the hashmap entry on the way back up out of a subtree.

**LC 2096 — LCA reasoning without ever computing the LCA explicitly:** Find the root-to-node path (as a direction string) to each of the two target nodes separately, strip their common prefix (that shared prefix *is* implicitly the path to the LCA), then the answer is "U" repeated for whatever remains of the start node's path, followed by the remaining target path unchanged.

| # | Problem | Key Concept |
|---|---|---|
| 1 | Print Root to Node Path in Binary Tree | DFS + backtracking — build the path list on the way down, undo on the way back up if the target isn't found |
| 2 | LC 112. Path Sum | Root-anchored — pass the remaining target sum down; true only at a matching leaf |
| 3 | LC 113. Path Sum II | Same as LC 112, plus backtracking to collect every valid path, not just detect one |
| 4 | LC 129. Sum Root to Leaf Numbers | Root-anchored — pass `runningNumber * 10 + digit` down; sum at every leaf |
| 5 | LC 257. Binary Tree Paths | DFS + backtracking, building string paths root-to-leaf |
| 6 | LC 437. Path Sum III | Directional-but-not-root-anchored — prefix sum + hashmap on the tree, same lookup as Prefix Sum sheet's LC 560 |
| 7 | LC 124. Binary Tree Maximum Path Sum | Any-path — postorder returns the single best one-direction extension; global max uses both directions combined |
| 8 | LC 687. Longest Univalue Path | Postorder — extend a child's contribution only if its value matches the current node; global max combines both directions when values agree |
| 9 | LC 236. Lowest Common Ancestor of a Binary Tree | Postorder — a node is the LCA exactly when `p` and `q` are found in different subtrees (or the node itself is one of them) |
| 10 | LC 1123. Lowest Common Ancestor of Deepest Leaves | Postorder returning `(depth, LCA candidate)` — compare children's depths to decide which side (or both) determines the LCA |
| 11 | LC 1373. Maximum Sum BST in Binary Tree | Postorder returning `(isValidBST, min, max, sum)` — track the max sum among all valid BST subtrees |
| 12 | LC 2096. Step-By-Step Directions From a Binary Tree Node to Another | Two root-to-node paths, strip common prefix, convert the start path's remainder to `U`s, keep the target path's remainder as-is |

---

## Pattern 6: Tree DP

**Identify:** Each node must return **multiple state values** to its parent, not just one — the parent combines children's states according to some rule. This is postorder, but structurally distinct from Pattern 2's single-value property computation and Pattern 5's single-best-extension: here the return value is a **tuple**.

**Key distinction from the Paths pattern, stated precisely:** Path problems return one number upward (the best single-direction extension). Tree DP returns a small fixed set of states — `(robbed, notRobbed)`, `(hasCamera, coveredNoCamera, notCovered)`, `(isValidBST, min, max, size)` — because the parent's own optimal choice genuinely depends on knowing more than one scenario about each child, not just the single best one.

**LC 968 — the greedy-with-tree-DP hybrid:** Postorder returns one of three states per node: has a camera, is covered by a child's camera but has none itself, or is not covered at all. The greedy insight sits *inside* the DP: whenever a child reports "not covered," the **parent** must place a camera immediately — waiting to decide later is never better, since a camera never becomes less useful by being placed as low as possible.

**LC 2246 — the general-tree exception, worth flagging explicitly:** This operates on an N-ary rooted tree, not strictly binary, but the technique — track the top-two longest matching child chains at each node — transfers directly. Included here rather than in Pattern 10 because the *lesson* (multi-state postorder combination) is what's being taught, not the N-ary mechanics themselves.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 337. House Robber III | Postorder returns `(robbedHere, notRobbedHere)` — parent picks the better combination per child |
| 2 | LC 968. Binary Tree Cameras | Postorder returns a 3-state enum; greedily place a camera at the *parent* the instant a child reports uncovered |
| 3 | LC 333. Largest BST Subtree | Postorder returns `(isValidBST, min, max, size)` — track the max size among all valid BST subtrees |
| 4 | LC 979. Distribute Coins in Binary Tree | Postorder returns each subtree's coin surplus/deficit; accumulate `abs(childBalance)` as moves at every edge |
| 5 | LC 1530. Number of Good Leaf Nodes Pairs | Postorder returns leaf depths within range; combine pairs formed across a node's two children |
| 6 | LC 1372. Longest ZigZag Path in Binary Tree | Postorder/DFS returns `(leftZigLength, rightZigLength)` — direction-alternating chain length per node |
| 7 | LC 2246. Longest Path With Different Adjacent Characters | N-ary tree — postorder returns the top-two longest valid child chains; same multi-state combination instinct, generalized |

---

## Pattern 7: Tree Transformation

**Identify:** Modify the tree's structure in place — restructure, delete nodes, relink pointers. Almost always postorder: children must be fixed before the current node can be correctly reattached to them.

**LC 1110 — the one problem here needing parent-awareness during deletion:** A deleted node's children, if not themselves deleted, become new roots of the returned forest. Postorder DFS carries down "is my parent still alive" (or checks it via a passed-in flag) so that exactly the right nodes get added to the forest — a node becomes a new root only if it survives while its parent doesn't.

**LC 366 — postorder height doubling as a bucketing key:** Every leaf removed in one pass is height 0 relative to its own subtree; the next pass's leaves are height 1, and so on. A single postorder pass computing each node's height and bucketing it into `result[height]` produces every "removal layer" in one traversal, instead of literally simulating repeated leaf-stripping.

| # | Problem | Key Concept |
|---|---|---|
| 1 | Children Sum Property in Binary Tree | Check/enforce that a node's value equals the sum of its children's values; fix by propagating a difference downward |
| 2 | LC 226. Invert Binary Tree | Swap `left`/`right` recursively — pre- or postorder both work |
| 3 | Morris Traversal — Preorder / Inorder | Thread the inorder predecessor's right pointer to the current node, traverse, then remove the thread — O(1) space |
| 4 | LC 114. Flatten Binary Tree to Linked List | Postorder in reverse (right, left, root) order rewiring, or a Morris-style threading trick |
| 5 | LC 1325. Delete Leaves With a Given Value | Postorder — return null for a leaf matching the target value; deletions cascade upward as new leaves are exposed |
| 6 | LC 1110. Delete Nodes and Return Forest | Postorder with parent-liveness awareness — a surviving child of a deleted node becomes a new forest root |
| 7 | LC 366. Find Leaves of Binary Tree | Postorder returning height; bucket each node into `result[height]` — one pass replaces repeated leaf-stripping |

---

## Pattern 8: Tree Hashing (Subtree Fingerprinting)

**Identify:** Detecting identical or duplicate subtree structures. Core technique: **serialize each subtree into a canonical string during postorder DFS**, and use that string as a hashmap key — the serialized string is a fingerprint of the subtree's exact shape and values combined.

**Why this is its own pattern, not folded into Properties or Two-Tree Problems:** The insight — collapse an entire structure into one comparable key — doesn't come from path thinking or DP thinking, and it isn't a pairwise comparison between two given trees either. It's a distinct technique, and it transfers directly to non-tree problems (grid shape hashing) once recognized.

**LC 652 — the anchor:** Serialize every subtree postorder (e.g. `val,leftSerial,rightSerial`), count occurrences in a hashmap, and collect any subtree whose serialization appears more than once.

**GFG: Number of Distinct Islands — the exact same fingerprinting instinct, applied to a grid:** Encode a connected island's shape as a sequence of relative moves from its starting cell during DFS, and hash that move-sequence. Structurally identical to LC 652's canonical serialization — the "shape" being fingerprinted is a grid region instead of a subtree, but the underlying idea (serialize a structure, compare via hashmap) doesn't change.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 652. Find Duplicate Subtrees | Postorder serialization + hashmap count — collect subtrees appearing 2+ times |
| 2 | LC 572. Subtree of Another Tree | Serialize-and-substring-check, or direct `isSameTree` comparison rooted at every node |
| 3 | GFG: Number of Distinct Islands | Same fingerprinting instinct on a grid — encode relative-move shape, hash to detect duplicates |

---

## Pattern 9: Two-Tree Problems

**Identify:** Two trees are given simultaneously, and the question is about their relationship — identical structure, a combined merge, or equivalence under some allowed transformation (a flip). The mechanic is always a **simultaneous DFS** walking both trees in lockstep.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 100. Same Tree | Simultaneous DFS — compare structure and value at every corresponding node |
| 2 | LC 617. Merge Two Binary Trees | Simultaneous DFS — sum values where both nodes exist, take whichever side exists where only one does |
| 3 | LC 951. Flip Equivalent Binary Trees | Simultaneous DFS trying both orientations (same-side or swapped) at every node pair |

---

## Pattern 10: N-ary Tree

**Identify:** A direct generalization of binary tree patterns to a `children[]` list instead of fixed `left`/`right` pointers. No new mental model — every earlier pattern (traversal, properties, level order) transfers by iterating over `children` instead of two fixed fields.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 590. N-ary Tree Postorder Traversal | Visit all children in order, then the node itself |
| 2 | LC 559. Maximum Depth of N-ary Tree | Postorder — `1 + max` over all children's depths, not just two |
| 3 | LC 428. Serialize and Deserialize N-ary Tree | Encode each node's child count (or a terminator marker) so the arbitrary branching factor is recoverable on decode |

---

## Final Summary

| Pattern | Problems | Core Mechanism |
|---|---|---|
| Tree Traversals | 7 | Recursive and iterative DFS orders — the stack mechanics everything else assumes |
| Tree Properties | 10 | Postorder property computation; parent/ancestor-aware variants pass state downward too |
| Level Order Traversal | 14 | BFS with level-size snapshotting; column/side tracking for positional questions |
| Tree Construction | 5 | Root from preorder/postorder, split from inorder (or from BST/fullness structure alone) |
| Binary Tree Paths | 12 | Root-anchored (pass down) vs any-path (global answer + single-direction return) |
| Tree DP | 7 | Postorder returns a tuple of states, not one value — parent combines states, not numbers |
| Tree Transformation | 7 | Postorder restructuring; parent-liveness awareness for deletion problems |
| Tree Hashing | 3 | Canonical postorder serialization as a hashmap fingerprint |
| Two-Tree Problems | 3 | Simultaneous DFS across two trees in lockstep |
| N-ary Tree | 3 | Same patterns, `children[]` list instead of `left`/`right` |
| **Total** | **~71 problems** | |

---

## How to Use This Sheet

**Pattern 1 is non-negotiable first, and the iterative versions matter as much as the recursive ones.** Every later pattern silently assumes you can walk a tree with an explicit stack, not just a call stack — the postorder iterative version in particular (Pattern 1) is what makes Pattern 7's deletion problems and Pattern 8's serialization feel mechanical instead of novel.

**Pattern 2 before Pattern 5 and Pattern 6.** The postorder "children report up, parent combines" reflex from Pattern 2's simple properties (height, diameter) is the exact same reflex Pattern 5's any-path problems and Pattern 6's multi-state DP need — just with a richer return value. If LC 543 (Diameter) isn't automatic, LC 124 (Max Path Sum) will feel like a new idea instead of "diameter, but summing values instead of counting edges, with negative numbers to worry about."

**Pattern 5 is where the return-value-vs-global-answer distinction from the Core Mental Model gets tested hardest.** Do LC 124 and LC 687 back to back specifically to drill "the parent gets one direction, the global answer gets both" until it's reflexive — every any-path problem you'll ever see reduces to this one rule.

**Pattern 6 requires Pattern 2 and Pattern 5 both solid.** LC 968 (Cameras) in particular combines a greedy decision with the tree-DP state-return mechanic — attempting it before multi-state postorder is comfortable will make the greedy insight much harder to isolate from the traversal mechanics.

**Pattern 4's LC 889 is the one place "no inorder array" changes the algorithm, not just the input.** Do LC 105/106 first so the hashmap-split technique is automatic, then treat LC 889 as "the one case where that technique doesn't apply and something else is needed" rather than a direct variation.

**Patterns 8, 9, and 10 are each small and mostly independent — safe to do in any order once Patterns 1–2 are solid.** Pattern 8's fingerprinting idea is worth connecting explicitly to the Graph sheet's "Number of Distinct Islands" the moment it comes up, since it's the clearest example in your whole reference of the same raw technique resurfacing across topic boundaries.