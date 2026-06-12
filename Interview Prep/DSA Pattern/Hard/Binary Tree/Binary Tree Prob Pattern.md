Here is the final clean version:

---

# Binary Tree DSA Patterns

*"Problems are infinite, but patterns are finite!"*

---

## Traversal Recognition Guide

Before identifying the pattern, identify the traversal direction first:

| Traversal | When to use |
|---|---|
| Preorder | Need parent info available before processing children |
| Inorder | BST-related problems — gives sorted order |
| Postorder | Need children's answers before computing current node's answer |
| Level Order | Need level relationships, coordinates, or shortest distance |

---

## Pattern Identification Quick Reference

| Pattern | Trigger |
|---|---|
| Tree Traversals | "traverse", "collect nodes", need DFS/BFS base; O(1) space → Morris |
| Tree Properties | "height", "diameter", "balanced", "complete", "symmetric", "count nodes" |
| Level Order | "level by level", "right side", "zigzag", "vertical", "next pointer", "boundary" |
| Tree Construction | "build tree from traversal arrays", "serialize", "deserialize" |
| Binary Tree Paths | "path sum", "root to leaf", "any path", "LCA" |
| Tree DP | "rob", "cameras", "coins", "maximize/minimize over subtree states", multi-state return |
| Tree Transformation | "flatten", "invert", "delete nodes", "restructure" |
| Tree Hashing | "duplicate subtrees", "identical subtrees", "count distinct shapes" |
| Two-Tree Problems | "same tree", "subtree check", "merge two trees", "flip equivalent" |
| N-ary Tree | "N children", "N-ary generalization" |

---

## Pattern 1: Tree Traversals (Core Templates)

**Identify:** Pure traversal mechanics. Everything else builds on these. Must know both recursive AND iterative for all three DFS orders — interviewers frequently ask for the iterative version specifically to test stack understanding.

**Morris Traversal Note:** O(1) space traversal using threaded pointers — temporarily modifies the tree structure then restores it. Not asked at junior level but appears at senior/FAANG level. Learn only after fully mastering the stack-based iterative approach.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 144. Binary Tree Preorder Traversal | Recursive + iterative stack: root → left → right |
| 2 | LC 94. Binary Tree Inorder Traversal | Recursive + iterative stack + Morris O(1) space variant |
| 3 | LC 145. Binary Tree Postorder Traversal | Iterative is hardest — two-stack trick or reverse-preorder approach |
| 4 | GFG: All Three Traversals Using Single Stack | Advanced iterative — one unified stack handles all three orders |

---

## Pattern 2: Tree Properties

**Identify:** Problem asks you to compute or verify a structural property of the tree. Answer propagates bottom-up from children to parent — almost always postorder. Key discipline: know what value to **return** vs what to **update globally**.

**Note on DFS State Propagation:** Several problems here pass constraints downward and return summaries upward simultaneously (LC 1026, LC 1448). This is not a separate pattern — it is just how DFS on trees works. The problem is still a property computation problem; the traversal direction is just the tool.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 104. Maximum Depth of Binary Tree | Base template — height via postorder recursion or BFS level count |
| 2 | LC 110. Balanced Binary Tree | Return -1 as sentinel for unbalanced subtree — prune early |
| 3 | LC 543. Diameter of Binary Tree | Global max updated at each node; diameter ≠ depth of tree |
| 4 | LC 101. Symmetric Tree | Mirror check — compare left and right subtrees simultaneously |
| 5 | LC 250. Count Univalue Subtrees | Postorder — univalue if both children are univalue and match current node value |
| 6 | LC 222. Count Complete Tree Nodes | O(log²n) — exploit complete tree height property, not plain O(n) recursion |
| 7 | LC 958. Check Completeness of a Binary Tree | BFS — null node must never be followed by non-null node |
| 8 | LC 662. Maximum Width of Binary Tree | BFS + heap-style index; width = rightmost index - leftmost index + 1 |
| 9 | LC 1026. Maximum Difference Between Node and Ancestor | Preorder — pass running min and max downward; answer = max abs diff at leaves |
| 10 | LC 1448. Count Good Nodes in Binary Tree | Preorder — pass running max downward; good node = value ≥ running max |

---

## Pattern 3: Level Order Traversal

**Identify:** Process tree level by level, or need positional/coordinate information. Core tool: BFS queue with level separation. Boundary traversal lives here — it is a combination of level-aware DFS and BFS.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 102. Binary Tree Level Order Traversal | Pure BFS template — size-based level separation |
| 2 | LC 637. Average of Levels in Binary Tree | BFS — aggregate sum per level, divide by count |
| 3 | LC 199. Binary Tree Right Side View | BFS — last node at each level |
| 4 | LC 103. Binary Tree Zigzag Level Order Traversal | BFS + direction flip per level |
| 5 | LC 116. Populating Next Right Pointers (Perfect Tree) | O(1) space — use existing next pointers to thread the level |
| 6 | LC 117. Populating Next Right Pointers II (Any Tree) | Same concept but harder edge cases on arbitrary tree |
| 7 | GFG: Top View of Binary Tree | BFS + column coordinate tracking — first node seen per column |
| 8 | LC 987. Vertical Order Traversal of a Binary Tree | BFS + (row, col) coordinates; sort by col → row → val |
| 9 | GFG: Boundary Traversal of Binary Tree | Left boundary top-down + all leaves left-to-right + right boundary bottom-up; skip leaves in boundary portions |
| 10 | LC 863. All Nodes Distance K in Binary Tree | DFS to add parent pointers → BFS from target node |
| 11 | GFG: Burning Tree | Same parent-pointer + BFS pattern as Distance K; fire spreads one level per unit time |

---

## Pattern 4: Tree Construction

**Identify:** Build a tree from given data — traversal arrays, sorted input. Core insight: **preorder or postorder gives you the root; inorder gives you the left/right boundary split.** Always use a hashmap for O(1) index lookup in the inorder array.

**Note on Serialization:** LC 297 Serialize and Deserialize Binary Tree is intentionally kept in Level Order (Pattern 3) because the canonical interview solution is BFS-based. Understanding level order encoding unlocks the deserialize logic naturally. Construction here focuses on building from traversal arrays.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 105. Construct from Preorder + Inorder | Root = preorder[0]; split inorder at root index; recurse with bounds |
| 2 | LC 106. Construct from Inorder + Postorder | Root = postorder[-1]; same inorder split logic |
| 3 | LC 889. Construct from Preorder + Postorder | Root + left-root from preorder; find left-root in postorder to determine split size |
| 4 | LC 1008. Construct BST from Preorder Traversal | Placed here as pure construction — use value bounds or monotonic stack approach |

---

## Pattern 5: Binary Tree Paths

**Identify:** Problems involving paths through the tree. This is the hardest pattern cluster conceptually.

**The critical distinction — get this right and most path problems become straightforward:**

- **Root-anchored paths** (must start at root, end anywhere) → pass running sum/state **downward** via parameters
- **Any-path problems** (can start and end at any node) → postorder, update **global answer** at each node, return only the best **single-direction** extension upward to the parent

Returning the full path value upward in any-path problems is the single most common mistake. The parent can only use one direction — left or right — not both. The "both directions combined" answer is only valid at the current node and goes into the global variable.

| # | Problem | Path Type | Key Concept |
|---|---|---|---|
| 1 | LC 112. Path Sum | Root-to-leaf | Base template — reduce target as you go down, check at leaf |
| 2 | LC 113. Path Sum II | Root-to-leaf | Backtracking — add to path going down, remove on return |
| 3 | LC 129. Sum Root to Leaf Numbers | Root-to-leaf | Accumulate number digit by digit going down |
| 4 | LC 257. Binary Tree Paths | Root-to-leaf | Collect all path strings — string backtracking template |
| 5 | LC 437. Path Sum III | Any-path | Prefix sum + hashmap along DFS path — O(n) not O(n²) |
| 6 | LC 124. Binary Tree Maximum Path Sum | Any-path | Global max at each node; return only best single extension upward |
| 7 | LC 687. Longest Univalue Path | Any-path | Same structure as diameter — global max, single-direction return |
| 8 | LC 236. Lowest Common Ancestor | LCA | Postorder — if both left and right non-null, current node is LCA |
| 9 | LC 1123. LCA of Deepest Leaves | LCA variant | Return (node, depth) pair — LCA when both sides have equal max depth |
| 10 | LC 1373. Maximum Sum BST in Binary Tree | Any-path + validation | Postorder returning (min, max, sum, isValid) — combine to find max valid BST sum |
| 11 | LC 2096. Step-By-Step Directions Node to Node | LCA + path strings | Find LCA, build path strings from LCA to both nodes, combine directions |

---

## Pattern 6: Tree DP

**Identify:** Each node must return **multiple state values** as a tuple to its parent, not just a single value. The parent combines children states according to a DP rule. This is what separates Tree DP from normal postorder property computation.

**Key distinction from Paths pattern:** Path problems return one best scalar value upward. Tree DP returns a struct/tuple of states — e.g., (robbed, not_robbed) or (covered, has_camera, uncovered). The number of states is the signal.

**General Tree Extension Note:** LC 2246 operates on a general rooted tree with N children, not strictly a binary tree. It is included here because the technique is identical — the pattern transfers directly to any rooted tree beyond binary.

| # | Problem | States Returned | Key Concept |
|---|---|---|---|
| 1 | LC 337. House Robber III | (rob_current, skip_current) | Classic 2-state tree DP — combine children states at each node |
| 2 | LC 968. Binary Tree Cameras | (covered, has_camera, not_covered) | 3-state tree DP — greedy observation reduces cases |
| 3 | LC 333. Largest BST Subtree | (min, max, size, isBST) | 4-value return — prune subtrees that fail BST check |
| 4 | LC 979. Distribute Coins in Binary Tree | excess_coins | Moves = sum of abs(excess) at each node; excess flows upward |
| 5 | LC 1530. Number of Good Leaf Nodes Pairs | list of leaf distances | Postorder returns distances from current node to all leaves below; combine left + right |
| 6 | LC 1372. Longest ZigZag Path in Binary Tree | (left_len, right_len) | Return both direction lengths at each node; global max updated inline |
| 7 | LC 2246. Longest Path with Different Adjacent Characters *(General Tree)* | max_length | DFS returns best length; combine top-2 children only if different from parent value |

---

## Pattern 7: Tree Transformation

**Identify:** Modify the tree structure in-place or produce a restructured output. Almost always postorder — fix children before fixing the current node.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 226. Invert Binary Tree | Postorder — swap children after recursing into both |
| 2 | LC 114. Flatten Binary Tree to Linked List | Postorder — flatten right, flatten left, append original right to end of flattened left |
| 3 | LC 1325. Delete Leaves with a Given Value | Postorder — after deleting children, current node may itself become a new leaf |
| 4 | LC 1110. Delete Nodes and Return Forest | Postorder — process children first; detached nodes become new forest roots |
| 5 | LC 366. Find Leaves of Binary Tree | Postorder — height from leaf = 0; group nodes by this height level |

---

## Pattern 8: Tree Hashing (Subtree Fingerprinting)

**Identify:** Problems involving detection of identical or duplicate subtree structures. Core technique: serialize each subtree into a canonical string during postorder DFS and store in a hashmap. The serialized string is a unique fingerprint for that subtree's structure and values combined.

**Why this is its own pattern:** The key insight — serialize a subtree to detect structural equality — does not come from path thinking or DP thinking. It is a distinct technique that recurs across multiple problems and transfers to non-tree problems like island shape hashing.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 652. Find Duplicate Subtrees | Postorder serialize each subtree → hashmap → flag entries with count ≥ 2 |
| 2 | LC 572. Subtree of Another Tree | Serialize both trees → check if serialized(t) appears inside serialized(s); handle null encoding carefully |
| 3 | GFG: Number of Distinct Islands | DFS path encoding — serialize traversal path as string; set of distinct strings = answer |

---

## Pattern 9: Two-Tree Problems

**Identify:** Two trees given simultaneously. Traverse both in lockstep — process corresponding nodes from both trees at each recursive call.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 100. Same Tree | Simultaneous preorder DFS — null checks before value checks |
| 2 | LC 617. Merge Two Binary Trees | Preorder lockstep — merge nodes, handle null on either side |
| 3 | LC 951. Flip Equivalent Binary Trees | Preorder — at each node try both normal and flipped child pairing; recurse both cases |

---

## Pattern 10: N-ary Tree

**Identify:** Generalization of binary tree to N children. Every binary tree pattern applies — just iterate over the children list instead of accessing left/right directly.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 590. N-ary Tree Postorder Traversal | Postorder on N children — iterate children list, process current node last |
| 2 | LC 559. Maximum Depth of N-ary Tree | Max over all children depths + 1 — same logic as binary |
| 3 | LC 428. Serialize and Deserialize N-ary Tree | Encode child count per node during serialization so decoder knows when to stop reading children |

---

## Final Summary

| Pattern | Problems | Core Tool |
|---|---|---|
| Tree Traversals | 4 | Recursive + iterative + Morris |
| Tree Properties | 10 | Postorder return-up |
| Level Order | 11 | BFS + coordinates |
| Tree Construction | 4 | Root ID + inorder split |
| Binary Tree Paths | 11 | Root-anchored vs any-path distinction |
| Tree DP | 7 | Multi-state tuple return |
| Tree Transformation | 5 | Postorder modify-then-return |
| Tree Hashing | 3 | Postorder serialize + hashmap |
| Two-Tree Problems | 3 | Lockstep traversal |
| N-ary Tree | 3 | Iterate children list |
| **Total** | **~61 problems** | |

---
