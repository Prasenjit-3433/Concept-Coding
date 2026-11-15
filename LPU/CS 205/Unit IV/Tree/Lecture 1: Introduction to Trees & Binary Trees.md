# Lecture 1: Introduction to Trees & Binary Trees

Status: Done

# 🎯Part 1: Hierarchical Data, Tree Terminology & Binary Trees

---

## Quick Recap Before We Begin

Across Units I to III, every structure we built was **linear**: arrays, linked lists, stacks, queues, and deques all arrange elements in a single sequence, one after another. In Unit IV we met recursion, which is the natural tool for the structure we start today. Recursion repeats a task on a smaller version of the same problem, and that is how a tree is defined.

This lecture is split into two parts:

- **Part 1** (this note): why we need trees, the full vocabulary of trees, and what makes a tree *binary*.
- **Part 2**: the types of binary trees (full, complete, perfect, balanced, degenerate and extended), the counting formulas, and a full summary.

---

## 1. From Linear to Hierarchical

Every structure so far puts data in a line. Each element has at most one "next" element:

```
Linear (array / linked list / stack / queue):

┌────┐   ┌────┐   ┌────┐    ┌────┐
│ 10 │──►│ 20 │──►│ 30 │──► │ 40 │
└────┘   └────┘   └────┘    └────┘
```

But a lot of real data is not a line. It has **levels**, where one item *contains* or *leads to* several others.

**Real-life analogy:** Think of the **folders on your computer**. A folder `Documents` holds the folders `College` and `Photos`. `College` holds `CS205` and `CS202`, and each of those holds files. Nobody stores this as one long row, because the natural picture is a branching hierarchy.

```
Documents/
├── College/
│   ├── CS205/
│   │   ├── Unit1.pdf
│   │   └── Unit2.pdf
│   └── CS202/
└── Photos/
    └── Trip.jpg
```

Turn the same picture on its side, and you get a **tree**:

```
                 Documents
                /         \
          College          Photos
          /     \             \
      CS205     CS202        Trip.jpg
      /    \
 Unit1.pdf  Unit2.pdf
```

A **tree** is a **non-linear, hierarchical data structure**. Data sits at different levels, and each item can lead to several items below it.

|  | Linear structures | Tree |
| --- | --- | --- |
| Shape | one straight sequence | branching hierarchy |
| Next element(s) | at most **one** | **zero, one or many** |
| Examples | array, linked list, stack, queue | folders, org chart, family tree |

### Where Trees Show Up

| Real-world use | Why it is a tree |
| --- | --- |
| **File system** (folders and files) | a folder contains sub-folders and files |
| **Company org chart** | a manager has several people reporting to them |
| **Family tree** | a person has children, who have children |
| **Knockout tournament bracket** | two teams feed into one match, and winners feed into the next round |
| **HTML page structure** (the DOM) | a `<div>` contains other tags, which contain more tags |

**Real-life analogy:** A knockout tournament bracket is a tree drawn upside down. The final is the single starting point, and every match splits into the two earlier matches that fed it.

---

## 2. What Exactly Is a Tree?

A **tree** is a collection of **nodes** connected by **edges**, with these rules:

1. There is exactly **one special node at the top**, called the **root**.
2. Every other node is connected to **exactly one parent** (the node directly above it).
3. There are **no cycles**: you can never start at a node, follow edges, and return to it.

```
        A              ← root
       / \
      B   C            ← A's children
     / \   \
    D   E   F
```

Rules 2 and 3 together give a useful fact:

> **A tree with `n` nodes always has exactly `n − 1` edges.**
> 

Every node except the root has exactly one edge going up to its parent, so that is `n − 1` edges in total.

⚠️ **Edges point downward, from parent to child.** The picture is drawn upside down compared to a real tree, with the root at the top and the leaves at the bottom. It's a convention: reading top to bottom matches how we think about a hierarchy.

### The Recursive Nature of a Tree

Look at the tree above. Cover everything except node `B` and its descendants:

```
      B
     / \
    D   E
```

This piece is **itself a tree**, with `B` as its root. Every node in a tree is the root of a smaller tree hanging below it.

> **A tree is a root, plus zero or more smaller trees attached beneath it.**
> 

This is the most important idea in the whole unit. It is the reason that **recursion is the natural way to work with trees**. Whatever we do to the whole tree, we do to the root, and then ask the smaller trees below to do the same thing.

---

## 3. Tree Terminology

Here is one reference tree. All the terms below are explained using it.

```
Level 0:                 A
                       /   \
Level 1:              B     C
                     / \     \
Level 2:            D   E     F
                       / \
Level 3:              G   H
```

### The Terms

| Term | Meaning | In the tree above |
| --- | --- | --- |
| **Node** | one element of the tree (holds data) | `A`, `B`, ..., `H` |
| **Edge** | the connection between a parent and a child | `A–B`, `B–E`, `E–G`, ... |
| **Root** | the topmost node, with no parent | `A` |
| **Parent** | the node directly above a given node | parent of `E` is `B` |
| **Child** | a node directly below a given node | children of `B` are `D` and `E` |
| **Siblings** | children of the same parent | `B` and `C`; `D` and `E`; `G` and `H` |
| **Leaf (external node)** | a node with **no children** | `D`, `F`, `G`, `H` |
| **Internal node** | a node with **at least one child** | `A`, `B`, `C`, `E` |
| **Ancestors** | all nodes on the path from a node up to the root (excluding itself) | ancestors of `G`: `E`, `B`, `A` |
| **Descendants** | all nodes below a given node, at any depth | descendants of `B`: `D`, `E`, `G`, `H` |
| **Subtree** | a node together with all its descendants | subtree of `E`: `E`, `G`, `H` |

**Real-life analogy:** Your own family. Your parents are your **parents**, and their parents and so on up the line are your **ancestors**. Your brother or sister is your **sibling**. Your children, grandchildren and so on are your **descendants**. A person with no children is a **leaf** of the family tree.

### Ancestors, Visually

```
Ancestors of G:

        A  ◄── ancestor (the root is always an ancestor of every other node)
       / \
      B   C
     / \
    D   E  ◄── ancestor
       / \
      G   H
      ▲
   this node
```

Only the nodes on the **single path from `G` up to the root** are ancestors. `D`, `H`, `C` and `F` are not, even though they sit in the same tree.

### Subtrees, Visually

A subtree is "this node, plus everything beneath it". Standing at different nodes gives different subtrees:

```
Subtree at B:      Subtree at E:     Subtree at C:     Subtree at D:
      B                  E               C                  D
     / \                / \               \              (a single node,
    D   E              G   H               F              also a tree)
       / \
      G   H
```

⚠️ **A leaf's subtree is just the leaf itself.** A single node is a valid tree. This will matter when we write recursive code, because it is where the recursion stops.

### Levels, Depth, Height and Degree

These four terms describe **position and size**. They are often confused, so read them carefully.

| Term | Meaning | Example |
| --- | --- | --- |
| **Level** of a node | how far the node is from the root. Root is at level 0, its children at level 1, and so on | `G` is at level 3 |
| **Depth** of a node | the number of **edges** from the root down to that node (same number as its level) | depth of `E` = 2 |
| **Height** of a node | the number of **edges** on the longest path from that node **down to a leaf** | height of `B` = 2 (`B→E→G`) |
| **Height of the tree** | the height of the root = the largest level of any node | height of the tree = 3 |
| **Degree** of a node | the number of **children** the node has | degree of `B` = 2, of `C` = 1, of `D` = 0 |

```
Depth (counted from the TOP)         Height (counted from the BOTTOM)

 A   depth 0                          A   height 3
 B   depth 1                          B   height 2
 E   depth 2                          E   height 1
 G   depth 3                          G   height 0  (a leaf)
```

**Real-life analogy:** Standing in a building. **Depth** is how many floors you are *below the roof* (count from the top). **Height** is how many floors there are *below you* down to the ground floor (count from the bottom).

### Worked Table for the Reference Tree

| Node | Level / Depth | Height | Degree | Leaf? |
| --- | --- | --- | --- | --- |
| `A` | 0 | 3 | 2 | no |
| `B` | 1 | 2 | 2 | no |
| `C` | 1 | 1 | 1 | no |
| `D` | 2 | 0 | 0 | yes |
| `E` | 2 | 1 | 2 | no |
| `F` | 2 | 0 | 0 | yes |
| `G` | 3 | 0 | 0 | yes |
| `H` | 3 | 0 | 0 | yes |

Check the fact from Section 2: there are 8 nodes, so there are `8 − 1 = 7` edges (`A–B, A–C, B–D, B–E, C–F, E–G, E–H`). ✅

⚠️ **Counting convention.** Textbooks differ. Some start levels at **1** for the root, and some measure height in **nodes** instead of edges (so a single node has height 1). In this course we use the **0-based, edge-counting** convention above, and every formula in Part 2 uses it. When you read another book, check which convention it follows before comparing answers.

⚠️ **The leaves all have height 0, but they can have different depths.** In the tree above, `F` has depth 2 and `G` has depth 3, yet both have height 0. Depth is measured from the root and height from the node itself, so they are different measurements.

---

## 4. What Makes a Tree "Binary"?

A general tree has no limit on how many children a node can have. Fixing that limit gives us a much easier structure to work with.

> **A binary tree is a tree in which every node has *at most two* children.**
> 

"Binary" simply means **two**. The two children are named the **left child** and the **right child**.

```
General tree (not binary)           Binary tree

        A                                A
      / | \                            /   \
     B  C  D                          B     C
   (3 children)                    (at most 2 children
                                    per node)
```

So in a binary tree every node has **degree 0, 1 or 2**, and never more.

```
Degree 2:     X          Degree 1:    X         Degree 1:    X       Degree 0:    X
             / \                     /                          \              (leaf)
            L   R                   L                            R
```

**Real-life analogy:** A knockout tournament. Every match has exactly two teams feeding into it, never three. Or a person's family tree traced *upward*: everyone has at most two biological parents.

### Left and Right Are Different

In a binary tree, **which side a child is on matters**. A lone child on the left is a different tree from a lone child on the right:

```
   10              10
  /                  \
 20                   20

Two DIFFERENT binary trees
(the second has no left child, only a right child)
```

⚠️ **A binary tree is not just "a tree where each node has two or fewer children".** The left and right positions are part of the structure. Two trees that hold the same values can still be different binary trees if the values sit on different sides. This is why, later in the unit, "left subtree" and "right subtree" are always named separately.

### The Recursive Definition of a Binary Tree

Putting Section 2's idea together with the two-child limit:

> A **binary tree** is either
> 
> 1. **empty** (no nodes at all), or
> 2. a **root node** together with a **left subtree** and a **right subtree**, each of which is itself a binary tree.

```
          root
         /    \
   [ left     [ right
    subtree ]  subtree ]       ← each is a binary tree (possibly empty)
```

The "empty tree" case is not a technicality. It is what makes a missing child fit into the definition: **a node with no left child simply has an *empty* left subtree.** In code this becomes a `NULL` pointer, and in recursion it becomes the base case.

⚠️ **Why this definition is the one that matters.** Every recursive tree algorithm you will write (traversals, search, insertion, height) follows this exact shape: *"if the tree is empty, stop. Otherwise, handle the root, then handle the left subtree and the right subtree the same way."* The structure of the data and the structure of the code are identical.

### Why So Much Attention on Binary Trees?

- **Simple to represent.** Two named child positions (`left`, `right`) are enough. We'll see exactly how in Lecture 2.
- **Easy to process recursively.** Exactly two subproblems per node, which matches the two-call recursion we practiced with Fibonacci and Merge Sort.
- **The foundation of other structures.** Binary Search Trees (later in this unit) are binary trees with an ordering rule that makes searching fast, and heaps (which sit behind the priority queue from Unit III) are binary trees with a different rule.
- **Very common in exams and interviews.** A large share of tree questions are about binary trees.

---

## 5. Part 1 Recap

| Concept | Meaning | Key point |
| --- | --- | --- |
| Linear vs hierarchical | a line of elements vs a branching structure with levels | trees model things that *contain* other things |
| Tree | nodes connected by edges, one root, no cycles | `n` nodes → exactly `n − 1` edges |
| Recursive nature | every node is the root of a smaller tree | the reason recursion fits trees |
| Root / parent / child / sibling | topmost node / node above / node below / same parent | edges point from parent to child |
| Leaf vs internal node | no children vs at least one child | a leaf's subtree is just itself |
| Ancestors / descendants | everything above on the path to the root / everything below | do not include the node itself |
| Subtree | a node plus all its descendants | itself a tree |
| Level / depth | distance from the root, in edges | root is at level 0 |
| Height | longest downward path to a leaf, in edges | leaves have height 0 |
| Degree | number of children of a node | binary tree: 0, 1 or 2 |
| Counting convention | 0-based levels, edge-counting heights | check the convention in other books |
| Binary tree | every node has **at most two** children, named left and right | left and right are not interchangeable |
| Recursive definition | empty, or root + left subtree + right subtree | the empty case is the recursion's base case |

**The single biggest idea from this part:** a tree is a **root with smaller trees hanging beneath it**, and a binary tree is the case where the root has **at most two** such trees, a left one and a right one. The data and the recursion have the same shape, which is why recursion will be our main tool for the rest of the unit.

# 🎯Part 2: Types of Binary Trees, Extended Binary Trees & Formulas

---

## Quick Recap Before We Continue

In Part 1 we saw that a tree is a **root with smaller trees hanging beneath it**, and that a **binary tree** limits every node to **at most two children**, a left one and a right one. We also fixed our counting convention: **levels start at 0, and heights are measured in edges**.

Not all binary trees look alike. Some are packed tight, some are lopsided, and some are stretched into a line. Today we name the shapes, because the shape decides how a tree behaves: how fast it can be searched, how it can be stored in memory (Lecture 2), and how much work a traversal does. We then add the **extended binary tree** from the syllabus, and finish with the formulas that connect height, levels and node counts.

---

## 6. Full Binary Tree

> **A binary tree is full when every node has either 0 or 2 children, and never exactly 1.**
> 

```
Full:                     Full:                     NOT full:

      A                        A                          A
     / \                      / \                        / \
    B   C                    B   C                      B   C
                                / \                     /
                               D   E                   D
(A: 2, B: 0, C: 0)        (A: 2, B: 0, C: 2,        (B has exactly one
                           D: 0, E: 0)               child → not full)
```

**Real-life analogy:** A knockout tournament in which every match has exactly two teams, with no match ever left with a single team and no opponent. Every internal node is a match, so it always needs both slots filled.

⚠️ **The check is per node.** A single node with one child anywhere in the tree breaks the "full" property, however neat the rest of the tree looks.

---

## 7. Complete Binary Tree

> **A binary tree is complete when every level is completely filled, except possibly the last level, and the last level is filled from left to right with no gaps.**
> 

Two rules, both mandatory:

1. All levels except the last are **fully packed**.
2. In the last level, the nodes sit **as far left as possible**. There is no empty slot to the left of any node.

```
Complete:                    Complete:                    NOT complete:

        A                          A                           A
      /   \                      /   \                       /   \
     B     C                    B     C                     B     C
    / \   /                    / \                         / \     \
   D   E F                    D   E                       D   E     F
                                                                     ↑
  last level filled          last level: D, E              F sits on the RIGHT of C,
  left to right              still left to right           but C's left slot is empty
                                                           → a gap → not complete
```

**Real-life analogy:** Seats in a cinema, filled row by row from the front. Every row except the last must be completely full, and in the last row people sit from the left end with no empty seat between them. Someone sitting alone at the far right of a row with empty seats before them breaks the rule.

### Why Completeness Matters

A complete binary tree has **no holes**, so its nodes can be numbered `0, 1, 2, ...` level by level, left to right, with no gaps in the numbering. That makes it perfect for storing in a plain **array** (Lecture 2), and it is the shape that heaps use, the structure behind the priority queue from Unit III.

⚠️ **"Complete" is about the last level.** The tree does not have to be full. In the first example above, `C` has only a left child, and the tree is still complete. What it cannot have is a gap before a node.

---

## 8. Perfect Binary Tree

> **A binary tree is perfect when every internal node has exactly 2 children and all leaves are at the same level.**
> 

Equivalently: **every level is completely filled**, including the last.

```
Perfect (height 2):                    NOT perfect:

          A                                   A
        /   \                               /   \
       B     C                             B     C
      / \   / \                           / \     \
     D   E F   G                         D   E     F

 all leaves D, E, F, G are            leaves D, E are at level 2,
 at level 2                           but F's sibling slot is missing
```

A perfect tree is the "ideal" shape. Every level holds the maximum possible number of nodes, which is why a perfect tree of height `h` has exactly `2^(h+1) − 1` nodes (proved in Section 12).

**Real-life analogy:** A knockout tournament with 8 teams. Every team plays in round one, every match in every round has two teams, and all teams are the same number of rounds away from the final.

---

## 9. Balanced Binary Tree

> **A binary tree is balanced when its height stays small, of the order of `log n` for `n` nodes, so no part of the tree is much deeper than any other.**
> 

The usual precise rule, called **height-balanced**, is:

> **For every node, the heights of its left and right subtrees differ by at most 1.**
> 

```
Balanced:                            NOT balanced:

        A                                  A
       / \                                / \
      B   C                              B   F
     /                                  /
    D                                  C
                                      /
  at A: left height 1, right 0        D
  at B: left height 0, right (empty)
  every node differs by ≤ 1  ✅       at A: left height 2, right height 0
                                      → differ by 2  ❌
```

(Here we treat an **empty subtree as height −1**, so a node with one leaf child and no other child still differs by only 1.)

### Why Balance Matters

Searching a tree takes time proportional to its **height**. A balanced tree keeps the height near `log n`, so searching, inserting and deleting are about **O(log n)**. A lopsided tree can push the height up to `n`, which is as bad as a linked list.

```
n = 7 nodes:

Balanced: height 2                 Lopsided: height 6

        4                          1
      /   \                         \
     2     6                         2
    / \   / \                         \
   1   3 5   7                         3 ...   (all the way to 7)

 about log₂ 7 steps to reach           up to 7 steps to reach
 any node                              the last node
```

We study this properly in the **Binary Search Tree** lectures, where the height decides the cost of every operation.

⚠️ **Balanced does not mean perfect or complete.** The balanced tree above is missing nodes, and yet it is balanced. Balance only limits how *uneven* the heights are.

---

## 10. Degenerate (Skewed) Binary Tree

> **A binary tree is degenerate when every internal node has exactly one child.**
> 

The tree collapses into a single path. If all the children are on the left, it is **left-skewed**. If all are on the right, it is **right-skewed**. A path that zigzags between left and right is also degenerate.

```
Left-skewed:        Right-skewed:         Zigzag:

      A                  A                    A
     /                    \                  /
    B                      B                B
   /                        \                \
  C                          C                C
 /                            \              /
D                              D            D
```

It is a **linked list wearing a tree's clothes**. Each node has one "next", the height is `n − 1`, and operations cost O(n).

| Property | Degenerate tree with `n` nodes |
| --- | --- |
| Height | `n − 1` (the maximum possible) |
| Nodes per level | exactly 1 |
| Search cost | O(n), no better than a linked list |

⚠️ **Degenerate trees are the reason balance matters.** Keep this shape in mind. It is the worst case we compare everything against, and it appears quite naturally (for example, when sorted data is inserted into a Binary Search Tree one value at a time).

---

## 11. Extended Binary Tree (2-Tree)

This type is named in our syllabus, so it deserves careful treatment.

### The Idea

In an ordinary binary tree, many nodes have **empty child slots**: a leaf has two, and a node with one child has one. An **extended binary tree** (also called a **2-tree**) fills **every empty slot with a special new node**. The result is a tree in which **every node has either 0 or 2 children**.

| Kind of node | What it is | Drawn as |
| --- | --- | --- |
| **Internal node** | one of the **original** nodes | circle `( )` |
| **External node** | one of the **new nodes**, added in place of an empty slot | square `[ ]` |

### Building One, Step by Step

Start with this ordinary binary tree of 4 nodes:

```
Original tree:

        A
       / \
      B   C
     /
    D
```

List the empty child slots:

```
A: no empty slot (has B and C)
B: right slot is empty
C: left and right slots are empty
D: left and right slots are empty

Total empty slots = 1 + 2 + 2 = 5
```

Put an external node `[ ]` in each of those 5 slots:

```
Extended tree:

           (A)
          /   \
       (B)     (C)
       / \     / \
    (D)  [ ] [ ] [ ]
    / \
  [ ] [ ]

Internal nodes (original):  A, B, C, D      → 4
External nodes (added):     5 squares       → 5
```

Now every circle has **exactly two children**, and every square has none, so the extended tree is a **full binary tree** by Section 6's definition.

### The Key Fact

> **An extended binary tree built from a binary tree with `n` nodes has `n` internal nodes and exactly `n + 1` external nodes (`2n + 1` nodes in total).**
> 

**Why?** Each of the `n` original nodes has 2 child slots, giving `2n` slots in total. Only `n − 1` of those slots are used, because a tree with `n` nodes has `n − 1` edges. So:

```
Empty slots = 2n − (n − 1) = n + 1
```

and each empty slot receives exactly one external node. Check with our example: `n = 4`, external nodes = `5` ✅.

### Where Extended Trees Are Used

**Expression trees.** An arithmetic expression such as `a * (b + c)` has a natural tree form. Operators are the internal nodes, since each needs exactly two operands, and operands are the external nodes:

```
Expression:  a * (b + c)

        (*)
        / \
      [a] (+)
          / \
        [b] [c]

 internal nodes (operators):  *, +     → 2
 external nodes (operands):   a, b, c  → 3 = 2 + 1  ✅
```

This connects to Unit III. Reading this tree in different orders produces the **infix, prefix and postfix** forms we converted between with stacks. We'll see exactly how when we study traversals in Lecture 3.

⚠️ **"External node" means something different here.** In Section 3, an external node was another name for a leaf of the original tree. In an extended tree, the external nodes are the **newly added placeholders**, and the original leaves become internal nodes (each with two new external children). Always ask which tree the term is being applied to.

⚠️ **The "extra" nodes are not data.** They mark *"nothing here"*. This is the same idea as the `NULL` pointers we'll draw in Lecture 2: an external node is a `NULL` pointer promoted to a visible node.

---

## 12. Counting Formulas for Binary Trees

These formulas use our convention: **root at level 0, height `h` measured in edges**.

### `Formula 1`: Maximum Nodes at Level `i`

> **A binary tree has at most `2^i` nodes at level `i`.**
> 

Level 0 has 1 node (the root), and each node can have at most 2 children, so each level can at most **double** the previous one:

```
Level 0:  1  node   =  2⁰
Level 1:  2  nodes  =  2¹
Level 2:  4  nodes  =  2²
Level 3:  8  nodes  =  2³
Level i:  2^i nodes
```

### `Formula 2`: Maximum Nodes in a Tree of Height `h`

> **A binary tree of height `h` has at most `2^(h+1) − 1` nodes.**
> 

Add up the maximum for every level from 0 to `h`:

```
1 + 2 + 4 + 8 + ... + 2^h  =  2^(h+1) − 1
```

| Height `h` | Levels | Maximum nodes |
| --- | --- | --- |
| 0 | 0 | 1 |
| 1 | 0–1 | 3 |
| 2 | 0–2 | 7 |
| 3 | 0–3 | 15 |
| 4 | 0–4 | 31 |

The maximum is reached exactly by a **perfect** binary tree.

### `Formula 3`: Minimum Nodes in a Tree of Height `h`

> **A binary tree of height `h` has at least `h + 1` nodes.**
> 

You need at least one node on each of the `h + 1` levels (levels 0 to `h`). The minimum is reached by a **degenerate** tree.

### `Formula 4`: Height in Terms of `n`

For a binary tree with `n` nodes:

```
minimum height  =  ⌊log₂ n⌋        (the tightest possible packing)
maximum height  =  n − 1           (a degenerate tree)
```

For example, with `n = 8`, the minimum height is 3, and the maximum is 7. A ***complete*** binary tree always achieves the minimum, so its height is `⌊log₂ n⌋`.

### `Formula 5`: Leaves and Two-Child Nodes

Let `n₀` be the number of leaves (degree 0), `n₁` the number of nodes with one child, and `n₂` the number of nodes with two children.

> **In every binary tree, `n₀ = n₂ + 1`.**
> 

**Why?** Count nodes and edges two ways:

```
Total nodes  n  = n₀ + n₁ + n₂
Total edges     = n − 1                (every node except the root has one parent edge)
Total edges     = 1*n₁ + 2*n₂            (each node contributes as many edges as it has children)

So:   n₀ + n₁ + n₂ − 1  =  n₁ + 2·n₂
      n₀                 =  n₂ + 1     ✅
```

Check against the reference tree from Part 1: the leaves `D, F, G, H` give `n₀ = 4`, and the nodes with two children `A, B, E` give `n₂ = 3`, and `4 = 3 + 1` ✅.

### `Formula 6`: Full Binary Trees

For a **full** binary tree there are no one-child nodes (`n₁ = 0`), so Formula 5 gives:

```
Leaves = internal nodes + 1

With I internal nodes:   L = I + 1,   total nodes = 2I + 1
With L leaves:           total nodes = 2L − 1
```

This is the same `n + 1` pattern we found for extended trees in Section 11, and it is not a coincidence: an extended binary tree **is** a full binary tree.

⚠️ **These formulas give bounds and counts, not constructions.** `2^(h+1) − 1` is the *maximum*. A tree of height 3 can have anywhere from 4 to 15 nodes, and only a perfect tree has exactly 15.

---

## 13. How the Types Relate

The types overlap, and some imply others:

```
          Perfect  ───────►  is always  Full
             │
             └──────────────► is always  Complete

   Complete  does NOT imply  Full
   Full      does NOT imply  Complete
   Degenerate trees are usually neither Full nor Complete
   (the exception: a single node, or a tree with just 2 nodes, can be Complete)
```

Two small trees show that "complete" and "full" are independent:

```
Complete but NOT full:           Full but NOT complete:

        A                              A
       / \                            / \
      B   C                          B   C
     /                                  / \
    D                                  D   E

 B has one child → not full       level 2 has a gap on the left
 last level is left-aligned       (B has no children but C does)
 → complete                       → not complete
```

### Comparison Table

| Type | The defining rule | Checked per... | Height behaviour |
| --- | --- | --- | --- |
| **Full** | every node has 0 or 2 children | node | any |
| **Complete** | all levels full except the last, which is filled left to right | level | always `⌊log₂ n⌋` |
| **Perfect** | all levels completely full, so all leaves are at the same level | level | `log₂(n + 1) − 1` |
| **Balanced** | subtree heights differ by at most 1 at every node | node | `O(log n)` |
| **Degenerate** | every internal node has exactly one child | node | `n − 1` (worst) |
| **Extended (2-tree)** | an ordinary tree with every empty slot filled by an external node | whole tree | built from any tree |

### Classifying Shapes

| Shape | Full | Complete | Perfect | Balanced | Degenerate |
| --- | --- | --- | --- | --- | --- |
| A single node | ✅ | ✅ | ✅ | ✅ | ✅ (trivially) |
| Root with two leaf children | ✅ | ✅ | ✅ | ✅ | ❌ |
| Root with one left child only | ❌ | ✅ | ❌ | ✅ | ✅ |
| Root with one right child only | ❌ | ❌ | ❌ | ✅ | ✅ |
| Seven nodes, three full levels | ✅ | ✅ | ✅ | ✅ | ❌ |
| A chain of 5 nodes, all left children | ❌ | ❌ | ❌ | ❌ | ✅ |

⚠️ **The one-node cases are worth remembering.** A single node satisfies every definition, because no rule can be violated. A root with only a left child is complete, but a root with only a right child is not, since the gap on the left breaks the left-to-right rule. This is the same left-versus-right asymmetry we saw in Part 1.

---

## 14. Common Traps Checklist

| Pitfall | Where it bites | The fix |
| --- | --- | --- |
| Calling a tree "full" when it is only complete (or the reverse) | classification questions | full is about **0 or 2 children per node**, complete is about **level filling and left alignment** |
| Treating a gap in the last level as acceptable | complete trees | no empty slot may sit to the left of any node in the last level |
| Treating "balanced" as "perfect" | balanced trees | balanced only limits the height *difference*, not the node count per level |
| Mixing 0-based and 1-based levels | the height and node-count formulas | in this course, the root is at level 0 and a single node has height 0 |
| Using `2^h − 1` for height `h` | max-node formula | with the edge-counting height it is `2^(h+1) − 1`; `2^h − 1` is for `h` counted in levels |
| Confusing depth and height | terminology | depth counts down from the root, height counts down to the deepest leaf |
| Treating left and right children as interchangeable | binary vs general trees | a lone left child and a lone right child are different trees |
| Reading "external node" as "original leaf" in an extended tree | extended trees | in a 2-tree, external nodes are the **added** placeholders |
| Assuming other books use the same names | reading other sources | some books call "full" trees *strict* or *proper*, and some use "full" for what we call *perfect*; check each book's definition |

---

## 15. Full Lecture Summary

| Concept | Meaning | Key point |
| --- | --- | --- |
| Tree | hierarchical structure of nodes and edges with one root and no cycles | `n` nodes → `n − 1` edges |
| Recursive structure | a root plus smaller trees below it | the reason recursion suits trees |
| Terminology | root, parent, child, sibling, leaf, ancestor, descendant, subtree | defined using one reference tree |
| Level / depth / height / degree | position from the top / depth from the root / distance to the deepest leaf / number of children | root at level 0, edge-counting |
| Binary tree | at most two children per node, named left and right | left and right matter |
| Binary tree, recursively | empty, or root + left subtree + right subtree | the empty tree is the base case |
| Full | every node has 0 or 2 children | no one-child nodes |
| Complete | all levels full except the last, filled left to right | no gaps; suits arrays and heaps |
| Perfect | every level completely full | exactly `2^(h+1) − 1` nodes; both full and complete |
| Balanced | subtree heights differ by at most 1 at every node | height stays `O(log n)` |
| Degenerate | every internal node has exactly one child | height `n − 1`; behaves like a linked list |
| Extended binary tree (2-tree) | original tree with every empty slot filled by an external node | `n` internal nodes → `n + 1` external nodes |
| Max nodes at level `i` | `2^i` | each level at most doubles |
| Max nodes at height `h` | `2^(h+1) − 1` | reached by a perfect tree |
| Min nodes at height `h` | `h + 1` | reached by a degenerate tree |
| Height range | `⌊log₂ n⌋` to `n − 1` | complete trees reach the minimum |
| Leaf formula | `n₀ = n₂ + 1` | derived by counting edges two ways |
| Full tree formula | leaves = internal nodes + 1 | total nodes `2I + 1` |

**The single biggest idea from this lecture:** a tree is a root with smaller trees beneath it, and a binary tree limits that to **at most two**. The **shape** of a binary tree decides everything that follows: a **complete** tree packs neatly into an array, a **balanced** tree keeps operations near `O(log n)`, and a **degenerate** tree collapses into a linked list. The type names are the vocabulary for talking about that shape.