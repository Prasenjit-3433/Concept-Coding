# Linked List DSA Patterns

*"Problems are infinite, but patterns are finite!"*

---

## The Core Mental Model — Before Any Pattern

Every linked list problem reduces to **pointer manipulation**. Before identifying a pattern, internalize these three mechanics cold:

**Traversal:** `curr = curr.next` until `curr is None` or `curr.next is None`. Know which stopping condition you need — they produce different final positions.

**The dummy node trick:** When the head itself might be deleted or changed (remove Nth, partition, merge), create a `dummy` node before head. Return `dummy.next` at the end. This eliminates all head-special-case bugs.

**Drawing the pointer state:** Before coding any LL problem, draw the before/after pointer state on paper. One missed `.next` reassignment corrupts the entire list silently.

---

## Pattern Identification Quick Reference

| Pattern | Trigger |
|---|---|
| Foundations (Raw Mechanics) | build a list from an array, traverse, find length, search, insert/delete at a position |
| Core Pointer Mechanics | basic traversal, insert, delete, design a linked list, "kth from start and end", removing duplicates |
| Fast & Slow Pointers | cycle detection (and removal), middle, Kth from end, distance, loop length, converging search on sorted DLL |
| In-place Reversal | "reverse", "palindrome check", "reverse a sublist", "K-group reversal", "twin sum" |
| Merge & Sort | "merge two sorted", "merge K sorted", "sort a linked list", "add two numbers", "flatten a multilevel list" |
| Multi-List & Restructuring | "reorder", "partition", "rotate", "interleave", "flatten", "copy with random pointer" |
| Stack-Assisted Traversal | "add numbers in forward order", "palindrome" without modifying list, reverse-order processing |
| Design Problems | LRU cache, LFU cache, browser history, O(1) get/put operations |

---

## Pattern 0: Foundations — Raw Mechanics (Do This Before Everything Else)

**Singly Linked List:**

| # | Problem | Key Concept |
|---|---|---|
| 1 | Convert Array to Linked List | Build a list node-by-node from an array |
| 2 | Traversal in a Linked List | `curr = curr.next` until `None` — print or collect every value |
| 3 | Find the Length of a Linked List | Traverse with a counter — building block for every "Kth node" problem in Pattern 2 |
| 4 | Search an Element in a Linked List | Traverse comparing each value — O(n), no shortcut without extra structure |
| 5 | Insertion in a Linked List | Insert at head, tail, and given position — three distinct cases |
| 6 | Deletion in a Linked List | Delete at head, tail, given position — needs a reference to the node *before* the target |

**Doubly Linked List:**

| # | Problem | Key Concept |
|---|---|---|
| 7 | Convert Array to DLL & Traversal | Same build, but every node wires up a `prev` pointer too |
| 8 | Insertion in a DLL | Every insertion touches four pointers instead of two |
| 9 | Deletion in a DLL | `prev` lets you delete a node in O(1) without an external predecessor reference — the exact property Pattern 7's cache designs depend on |

---

## Pattern 1: Core Pointer Mechanics

**How to identify:** Remove, insert, or rearrange individual nodes — no speed differential, no reversal, no merging. "I just need to walk the list and fix some pointers" = this pattern.

**LC 707 is Pattern 0, composed.** If it feels harder than Pattern 0's individual pieces, re-drill Pattern 0.

**Sorted vs unsorted duplicate removal:** LC 83/82 work by comparing *adjacent* nodes only because sortedness guarantees duplicates sit next to each other. Once unsorted, that guarantee is gone — the pointer-only trick breaks and a hashset takes over. Same lesson as the Two Pointers sheet's LC 1 vs LC 167.

**The gap technique, twice:** LC 19 finds the Nth-from-end with a single gap between two pointers. LC 1721 needs Kth-from-start *and* Kth-from-end at once — same gap idea, applied from both directions.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 707. Design Linked List | Pattern 0's primitives combined into one design problem |
| 2 | LC 203. Remove Linked List Elements | Dummy node — head deletion no longer special-cased |
| 3 | LC 83. Remove Duplicates from Sorted List | Skip nodes where `curr.val == curr.next.val` |
| 4 | Remove Duplicates from Sorted Doubly Linked List | Same skip logic, plus re-linking `prev` on both sides |
| 5 | LC 82. Remove Duplicates from Sorted List II | Remove ALL occurrences — outer loop skips the entire run |
| 6 | Delete All Occurrences of a Key in a DLL | Generalizes LC 82 to an arbitrary key, anywhere in the list |
| 7 | LC 1836. Remove Duplicates From an Unsorted Linked List | Contrast case — hashset replaces the pointer trick once adjacency is gone |
| 8 | LC 19. Remove Nth Node from End of List | Dummy node + two-pointer gap |
| 9 | LC 1721. Swapping Nodes in a Linked List | Same gap technique, applied from both ends simultaneously |
| 10 | LC 24. Swap Nodes in Pairs | Pointer reassignment on groups of two — base case for K-group reversal |
| 11 | LC 237. Delete Node in a Linked List | Copy next node's value, skip next |

---

## Pattern 2: Fast & Slow Pointers

**How to identify:** A distance relationship between two positions — middle, cycle, Kth-from-end. Needs two passes naively; fast/slow does it in one.

**Detection isn't the end goal.** LC 142 finds *where* the cycle starts. The natural follow-up — and a common interviewer chain — is actually breaking it.

**A mechanic that looks like this pattern but isn't:** the sorted-DLL pair-sum problem below uses two pointers, but they move at the *same* speed from *opposite ends* — that's opposite-direction convergence (the Two Pointers sheet's mechanic), not fast/slow. Say out loud which one you're using before coding either.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 876. Middle of the Linked List | Slow/fast — when fast reaches end, slow is at middle |
| 2 | LC 2095. Delete the Middle Node of a Linked List | Find the middle, then unlink it — needs a `prev` tracked alongside slow |
| 3 | LC 141. Linked List Cycle | Slow/fast meet → cycle exists |
| 4 | Find the Length of the Loop in a Linked List | After meeting, keep one pointer moving and count steps until it laps back |
| 5 | LC 142. Linked List Cycle II | Meet inside cycle → reset one to head → advance both → meet at entry |
| 6 | Detect and Remove Loop in a Linked List | Walk to the node just before the entry point, set `.next = None` |
| 7 | LC 160. Intersection of Two Linked Lists | Switch to other list's head at the end — path lengths equalize |
| 8 | Find All Pairs with a Given Sum in a Sorted Doubly Linked List | **Different mechanic** — opposite-direction convergence from head and tail, same idea as the sorted-array two-pointer |

---

## Pattern 3: In-place Reversal

**How to identify:** Reverse all or part of the list, or compare/combine it against its own reversed form. Distinguisher from Pattern 6: if O(1) space is fine and modification is allowed, reverse in place; if the original must survive intact, use a stack instead.

**The three-pointer template:**
```
prev = None
curr = head
while curr:
    next_node = curr.next
    curr.next = prev
    prev = curr
    curr = next_node
return prev
```

**Twin Sum — same shape as LC 234, different final step:** find mid, reverse second half, then *sum* corresponding pairs instead of comparing them. Zero new pointer logic if LC 234 is solid.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 206. Reverse Linked List | Core three-pointer template — iterative and recursive |
| 2 | Reverse a Doubly Linked List | Swap `next`/`prev` at each node |
| 3 | LC 92. Reverse Linked List II | Reverse a sublist [left, right] — dummy node + isolated reversal |
| 4 | LC 234. Palindrome Linked List | Fast/slow mid → reverse second half → compare — synthesis of Patterns 2+3 |
| 5 | LC 25. Reverse Nodes in K-Group | Check K nodes available → reverse group → stitch back → recurse |
| 6 | LC 2130. Maximum Twin Sum of a Linked List | Same mid-find + reverse-half as LC 234, sum pairs instead of comparing |

*Optional, once LC 25 is automatic:* **Reverse Alternate K Nodes** (GFG) — reverse K, skip K, repeat. **LC 2181. Reverse Nodes in Even Length Groups** — group sizes grow (1,2,3,...) instead of staying fixed.

---

## Pattern 4: Merge & Sort on Linked List

**How to identify:** Combine sorted lists, sort an unsorted one, or walk two lists simultaneously combining values. No random access, so merge sort is the only efficient sort here.

**LC 21 → LC 2:** LC 21 walks two lists in lockstep, comparing heads and picking the smaller. LC 2 (Add Two Numbers) walks the same lockstep — but *combines with carry* instead of comparing. Same skeleton, different per-step operation. *(An entirely recursive version of LC 21 also exists — smaller head recurses on the rest — if that framing ever clicks better, that's fine; the iterative dummy-node version here is what the rest of this sheet builds on.)*

**Flattening a Linked List (GFG):** each node also has a `bottom` pointer to its own sorted sub-list. Recursively merge each `bottom` list into the accumulated result using LC 21's merge, one pair at a time — the bridge from "merge two" to "merge many."

**LC 148:** find mid (Pattern 2) → split → recursively sort halves → merge (LC 21). The recursive version costs O(log n) call-stack space; a bottom-up iterative version (merge sublists of size 1, then 2, then 4, doubling each pass) gets true O(1) space — a common hard follow-up.

**LC 23:** divide-and-conquer pairwise merge (generalizes Flattening's idea, no extra structure needed) is the primary technique here. Avoid the trap of "dump every value into an array, sort, rebuild" — it works but defeats the point of the exercise.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 21. Merge Two Sorted Lists | Base merge template — dummy node + compare heads |
| 2 | LC 2. Add Two Numbers | Same simultaneous-traversal shape as LC 21 — combine with carry |
| 3 | Flattening a Linked List | Repeated pairwise merge across a horizontal list of sorted sub-lists |
| 4 | LC 148. Sort List | Merge sort — find mid + merge; bottom-up variant for O(1) space |
| 5 | LC 23. Merge K Sorted Lists | Divide-and-conquer pairwise merge — generalizes the Flattening idea |

---

## Pattern 5: Multi-List & Restructuring Operations

**How to identify:** Restructure node ordering, interleave, rotate, flatten, or deep-copy with non-trivial connections.

---

> ### Must-Master Special Problem: LC 138. Copy List with Random Pointer
>
> **Three-pass interleaving:**
> - Pass 1: Weave clones between originals — `original → clone → original.next → ...`
> - Pass 2: `clone.random = original.random.next`
> - Pass 3: Separate the two interleaved lists
>
> O(1) space, no hashmap. **If this doesn't come immediately:** a plain HashMap of `original → clone`, built in one pass and used to wire `next`/`random` in a second pass, is a fully valid O(n)-space starting point — get that working first, then optimize.

---

**Sort a Linked List of 0's, 1's, and 2's** — three dummy-headed chains instead of two, the simplest form of the "multiple chains, stitch at the end" template, before LC 86 asks for the same with a comparison-based split.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 138. Copy List with Random Pointer | Three-pass interleaving (optimal) or HashMap (brute force) |
| 2 | LC 328. Odd Even Linked List | Two chains — odd-indexed, even-indexed — connect |
| 3 | Sort a Linked List of 0's, 1's and 2's | Three dummy-headed chains — warm-up before LC 86 |
| 4 | LC 86. Partition List | Two dummy-headed chains — less-than / greater-equal |
| 5 | LC 61. Rotate List | Find length → form cycle → break at new tail |
| 6 | LC 143. Reorder List | Mid + reverse second half + merge-interleave — synthesis of Patterns 2, 3, 5 |
| 7 | LC 430. Flatten a Multilevel Doubly Linked List | DFS splice — insert child list between current and next |
| 8 | LC 1669. Merge In Between Linked Lists | Walk to a-1, walk to b, splice in second list |
| 9 | LC 708. Insert into a Sorted Circular Linked List | Three cases — normal point, min/max boundary, all-equal edge case; `while curr.next != head` |

---

## Pattern 6: Stack-Assisted Traversal

**How to identify:** Process nodes in reverse order without modifying the original list. The stack simulates reverse traversal. Distinguisher from Pattern 3: does the original forward order need to survive, or do two lists need reverse-order *simultaneous* processing? Either → stack.

| Situation | Use |
|---|---|
| O(1) space required, modification allowed | In-place Reversal (Pattern 3) |
| Original list must be preserved | Stack (Pattern 6) |
| Two lists need reverse-order simultaneous processing | Stack (Pattern 6) |

**Add 1 to a Number Represented by a Linked List** — same "combine from the least-significant end, propagate a carry" idea as LC 445, but with one list. Recurse to the tail (or use a stack) so carry propagates backward — isolates that idea before adding a second list.

| # | Problem | Key Concept |
|---|---|---|
| 1 | Add 1 to a Number Represented by a Linked List | Recurse/stack to the tail, propagate carry backward |
| 2 | LC 445. Add Two Numbers II | Push both lists to stacks → pop and add from LSB → build result forward |
| 3 | LC 234. Palindrome Linked List *(stack variant)* | Push first half to stack, compare with second half — original preserved |

*LC 234 intentionally appears in both Pattern 3 and Pattern 6 — it's the canonical problem for deciding between the two. Solve it both ways.*

---

## Pattern 7: Design Problems

**How to identify:** Cache, history tracker, or structure needing O(1) get/put with an eviction, ordering, or navigation policy. Only achievable by pairing a hashmap with a doubly linked list.

**Why doubly linked, not singly?** Deleting a node in O(1) requires knowing its predecessor. Singly linked needs O(n) to find it; `prev` gives it directly — the exact property Pattern 0's DLL deletion drill builds toward.

**Worth naming before jumping to the optimal design:** a plain array/list with linear-scan eviction is correct but O(n) per operation — say that out loud, then explain *why* it fails the O(1) constraint, before reaching for HashMap+DLL.

**LFU is significantly harder than LRU.** Do not attempt it until LRU is completely clean.

**LC 1472 (Design Browser History)** doesn't need eviction or a hashmap — just a DLL where `back`/`forward` are literal `prev`/`next` walks, and visiting a new page cuts off everything ahead. It's the clean illustration of your original point: a stack-of-two solves this too, but the DLL solution teaches a distinct, useful technique (truncate-forward-on-new-visit) that's worth knowing on its own, not just as "another way to do it."

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 706. Design HashMap | Separate chaining — array of linked list buckets |
| 2 | LC 146. LRU Cache | HashMap + Doubly LL — move accessed node to front, evict from back |
| 3 | LC 460. LFU Cache | HashMap + HashMap(freq→DLL) + min_freq tracking |
| 4 | LC 432. All O`one Data Structure | HashMap(key→node) + DLL of frequency buckets — LFU's bucket idea taken one step further |
| 5 | LC 1472. Design Browser History | DLL traversal for back/forward — truncate-forward-on-new-visit is the transferable technique |

---

## Final Summary

| Pattern | Problems | Core Mechanism |
|---|---|---|
| Foundations (Raw Mechanics) | 9 | Build, traverse, insert, delete — singly and doubly |
| Core Pointer Mechanics | 11 | Dummy node; sorted-vs-unsorted dedup; gap technique, one and both ends |
| Fast & Slow Pointers | 8 | Cycle (detect + remove), middle, distance, loop length; convergence flagged separately |
| In-place Reversal | 6 (+2 optional) | Three-pointer template; twin sum as a same-shape variant |
| Merge & Sort | 5 | Compare, combine-with-carry, repeat, merge sort, scale up |
| Multi-List & Restructuring | 9 | Pointer reconnection — reorder, rotate, flatten, deep copy |
| Stack-Assisted Traversal | 3 | Stack reverses traversal order without modifying the list |
| Design Problems | 5 | HashMap + Doubly LL, generalized to frequency buckets |
| **Total** | **~56 problems** | |

---

## How to Use This Sheet

**Pattern 0 first, no exceptions.** Pattern 1 is next, with LC 707 as its capstone, not its start.

**Patterns 2 and 3 run in parallel.** Do the loop-removal follow-up right after LC 142 — same mental model, and interviewers chain the two constantly.

**Pattern 4 builds from "compare" → "combine" → "repeat":** LC 21 → LC 2 → Flattening → LC 148 → LC 23. Each step changes exactly one thing.

**LC 143 in Pattern 5 is the capstone** for Patterns 1–4 — solving it cleanly without hints means those four are solid.

**Pattern 6:** do the single-list carry problem before LC 445 to isolate that idea first.

**Design last.** LFU only after LRU is clean; LC 432 is a natural next step from LFU, not a new problem.