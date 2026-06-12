# Linked List DSA Patterns

*"Problems are infinite, but patterns are finite!"*

---

## The Core Mental Model — Before Any Pattern

Every linked list problem reduces to **pointer manipulation**. Before identifying a pattern, internalize these three mechanics cold:

**Traversal:** `curr = curr.next` until `curr is None` or `curr.next is None`. Know which stopping condition you need — they produce different final positions.

**The dummy node trick:** When the head itself might be deleted or changed (remove Nth, partition, merge), create a `dummy` node before head. Return `dummy.next` at the end. This eliminates all head-special-case bugs.

**Drawing the pointer state:** Before coding any LL problem, draw the before/after pointer state on paper. One missed `.next` reassignment corrupts the entire list silently. The drawing takes 30 seconds and saves 10 minutes of debugging.

---

## Pattern Identification Quick Reference

| Pattern | Trigger |
|---|---|
| Core Pointer Mechanics | basic traversal, insert, delete, design a linked list |
| Fast & Slow Pointers | cycle detection, middle, Kth from end, distance between pointers |
| In-place Reversal | "reverse", "palindrome check", "reverse a sublist", "K-group reversal" |
| Merge & Sort | "merge two sorted", "merge K sorted", "sort a linked list" |
| Multi-List & Restructuring | "reorder", "partition", "rotate", "interleave", "flatten", "copy with random pointer" |
| Stack-Assisted Traversal | "add numbers in forward order", "palindrome" without modifying list, reverse-order processing |
| Design Problems | LRU cache, LFU cache, O(1) get/put operations |

---

## Pattern 1: Core Pointer Mechanics

**How to identify in an unseen problem:**
The problem gives you a linked list and asks you to remove, insert, or rearrange individual nodes without any special technique — no two-pointer speed difference, no reversal, no merging. If you find yourself thinking "I just need to walk the list and fix some pointers," this is Pattern 1. The dummy node is almost always the right first move.

**Key insight:** The dummy node pattern is introduced here because it applies across patterns 3, 4, and 5. Learn it once, use it everywhere. Implementing LC 707 from scratch forces you to handle every pointer edge case that all subsequent problems will throw at you.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 707. Design Linked List | Implement a doubly linked list from scratch — handles all pointer edge cases |
| 2 | LC 203. Remove Linked List Elements | Dummy node pattern — head deletion no longer a special case |
| 3 | LC 83. Remove Duplicates from Sorted List | Basic traversal — skip nodes where `curr.val == curr.next.val` |
| 4 | LC 82. Remove Duplicates from Sorted List II | Remove ALL occurrences — outer while loop skips the entire duplicate run |
| 5 | LC 19. Remove Nth Node from End of List | Dummy node + two-pointer gap — advance one pointer N steps first |
| 6 | LC 24. Swap Nodes in Pairs | Pointer reassignment on groups of two — base case for K-group reversal |
| 7 | LC 237. Delete Node in a Linked List | Copy next node's value, skip next — unusual problem, tests pointer thinking |

---

## Pattern 2: Fast & Slow Pointers

**How to identify in an unseen problem:**
The problem involves a distance relationship between two positions in the list — finding the middle, detecting a cycle, or finding the Nth node from the end. The signal is that you need to know the position of two nodes simultaneously without knowing the list length upfront. If you would need two passes to do it naively, fast/slow does it in one. Trigger words: "middle", "cycle", "Kth from end", "does the list loop".

**Key insight — the cycle entry point derivation (LC 142):** When slow and fast meet inside the cycle, reset one pointer to head. Advance both one step at a time. They meet at the cycle entry. You should be able to explain *why* this works mathematically, not just *that* it works — interviewers ask.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 876. Middle of the Linked List | Slow/fast — when fast reaches end, slow is at middle |
| 2 | LC 141. Linked List Cycle | Slow/fast meet → cycle exists |
| 3 | LC 142. Linked List Cycle II | Meet inside cycle → reset one to head → advance both → meet at entry point |
| 4 | LC 160. Intersection of Two Linked Lists | Two pointers — switch to other list's head when reaching end; path lengths equalize |

---

## Pattern 3: In-place Reversal

**How to identify in an unseen problem:**
The problem asks you to reverse all or part of the list, or the solution requires comparing the list against its own reversed form. Trigger words: "reverse", "palindrome", "K-group". Key distinguisher from Stack-Assisted (Pattern 6): if the problem requires O(1) space OR allows modifying the list, use in-place reversal. If the problem requires the original order to be preserved after processing, use a stack instead.

**The three-pointer template — must know cold:**
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

**K-Group Reversal (LC 25):** Check if K nodes remain → reverse exactly K nodes → stitch reversed group back → recurse on remainder. Drawing the before/after pointer state is non-negotiable here.

**Palindrome check (LC 234):** Find middle (Pattern 2) → reverse second half (Pattern 3) → compare both halves → restore list. This problem tests two patterns together and is a frequent warm-up question.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 206. Reverse Linked List | Core three-pointer template — implement both iterative and recursive |
| 2 | LC 92. Reverse Linked List II | Reverse a sublist from position left to right — dummy node + isolated reversal |
| 3 | LC 234. Palindrome Linked List | Fast/slow mid → reverse second half → compare — synthesis of Pattern 2 + Pattern 3 |
| 4 | LC 25. Reverse Nodes in K-Group | Check K nodes available → reverse group → stitch back → recurse |

---

## Pattern 4: Merge & Sort on Linked List

**How to identify in an unseen problem:**
The problem gives you sorted lists and asks to combine them, or gives an unsorted list and asks to sort it. Trigger words: "merge two sorted lists", "merge K sorted lists", "sort a linked list". The structural constraint that makes this its own pattern: there is no random access in a linked list, so merge sort is the only efficient sorting algorithm here — not quicksort, not heapsort.

**Merge Sort on a Linked List (LC 148):** Find mid using slow/fast pointers → split → recursively sort both halves → merge. This is a direct synthesis of Pattern 2 (find mid) + the merge base case (LC 21).

**Merge K Sorted Lists (LC 23):** One of the most frequently asked FAANG linked list problems. Two approaches — min-heap O(N log K) or divide-and-conquer using LC 21 pairwise. The divide-and-conquer approach demonstrates structural thinking and is the interview-preferred answer.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 21. Merge Two Sorted Lists | Base merge template — dummy node + compare heads iteratively |
| 2 | LC 148. Sort List | Merge sort on LL — find mid (Pattern 2) + merge (LC 21) |
| 3 | LC 23. Merge K Sorted Lists | Min-heap O(N log K) OR divide-and-conquer pairwise merge |

---

## Pattern 5: Multi-List & Restructuring Operations

**How to identify in an unseen problem:**
The problem asks you to restructure the list's node ordering, interleave nodes from different positions, rotate the list, flatten a multilevel structure, or deep-copy a list with non-trivial pointer connections. These problems do not fit neatly into reversal or merge — the challenge is identifying the correct sequence of pointer reconnections. Trigger words: "reorder", "rotate", "flatten", "copy with random pointer", "partition into two groups".

---

> ### Must-Master Special Problem: LC 138. Copy List with Random Pointer
>
> This problem has a unique technique that appears in no other linked list problem — the **three-pass interleaving approach**:
> - Pass 1: Weave clone nodes between originals — `original → clone → original.next → ...`
> - Pass 2: Set `clone.random = original.random.next` (the clone of the random target is always one step ahead)
> - Pass 3: Separate the two interleaved lists
>
> This achieves O(1) space without a hashmap. Interviewers love it because it requires genuinely creative pointer thinking. Do not skip it.

---

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 138. Copy List with Random Pointer | Three-pass interleaving — weave clones, set randoms, separate lists |
| 2 | LC 328. Odd Even Linked List | Two pointer chains — collect odd-indexed, collect even-indexed, connect |
| 3 | LC 86. Partition List | Two dummy-headed chains — less-than and greater-equal — connect at end |
| 4 | LC 61. Rotate List | Find length → form cycle → break at new tail position (`length - k % length`) |
| 5 | LC 143. Reorder List | Find mid + reverse second half + merge-interleave — three-step synthesis of Patterns 2, 3, and 5 |
| 6 | LC 430. Flatten a Multilevel Doubly Linked List | DFS splice — insert child list between current and current.next; track child tail |
| 7 | LC 1669. Merge In Between Linked Lists | Walk to position a-1, walk to position b, splice in second list |

**Circular Linked List note:** LC 708 (Insert into Sorted Circular List) belongs conceptually here. The key difference from standard traversal: the stopping condition is `while curr.next != head` instead of `while curr.next != None`. If a problem gives you a circular list, that stopping condition change is the entire adaptation.

---

## Pattern 6: Stack-Assisted Traversal

**How to identify in an unseen problem:**
The problem requires processing nodes in reverse order — but you either cannot or should not modify the original list. The stack simulates reverse traversal without physically reversing the list. Distinguishing signal from Pattern 3 (In-place Reversal): ask yourself — does the problem require the original forward order to be intact after processing, OR does it involve values being combined from two lists in reverse simultaneously? If yes to either, reach for a stack. Trigger: "numbers stored in forward order", "process from tail to head", "palindrome check without modifying the list".

**The core decision rule — Stack vs In-place Reversal:**

| Situation | Use |
|---|---|
| O(1) space required, list modification allowed | In-place Reversal (Pattern 3) |
| Original list must be preserved after processing | Stack (Pattern 6) |
| Two lists need reverse-order simultaneous processing | Stack (Pattern 6) |

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 445. Add Two Numbers II | Push both lists to stacks → pop and add from LSB → build result forward |
| 2 | LC 234. Palindrome Linked List *(stack variant)* | Push first half to stack → compare with second half — O(n) space, original preserved |

*Note: LC 234 intentionally appears in both Pattern 3 and Pattern 6. It is the canonical problem that teaches the decision between the two approaches. Solve it both ways and be able to justify the choice.*

---

## Pattern 7: Design Problems

**How to identify in an unseen problem:**
The problem asks you to implement a cache or data structure with O(1) get and put — and some eviction or ordering policy. The signal is the O(1) constraint on operations that naively would require O(n) search. This combination — O(1) lookup AND O(1) reorder/evict — is only achievable by pairing a hashmap with a doubly linked list.

**Why doubly linked list and not singly?** Deleting a node in O(1) requires knowing its predecessor. A singly linked list requires O(n) to find it. A doubly linked list gives you `prev` directly. This is the architectural insight — understand it, don't just memorize the code.

**LFU (LC 460) is significantly harder than LRU (LC 146).** LFU requires a second hashmap from frequency to a doubly linked list of all nodes at that frequency, plus tracking the current minimum frequency at all times. Do not attempt LFU until LRU is completely clean.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 706. Design HashMap | Separate chaining — array of linked list buckets; builds intuition for LRU internals |
| 2 | LC 146. LRU Cache | HashMap + Doubly LL — move accessed node to front; evict from back |
| 3 | LC 460. LFU Cache | HashMap + HashMap(freq → DLL) + min_freq tracking — O(1) all operations |

---

## Final Summary

| Pattern | Problems | Core Mechanism |
|---|---|---|
| Core Pointer Mechanics | 7 | Dummy node + pointer reassignment fundamentals |
| Fast & Slow Pointers | 4 | Speed differential → cycle, middle, distance |
| In-place Reversal | 4 | Three-pointer template — prev/curr/next |
| Merge & Sort | 3 | Sequential merge; merge sort uses fast/slow for mid |
| Multi-List & Restructuring | 7 | Pointer reconnection — reorder, rotate, flatten, deep copy |
| Stack-Assisted Traversal | 2 | Stack reverses traversal order without modifying list |
| Design Problems | 3 | HashMap + Doubly LL = O(1) get/put/evict |
| **Total** | **~30 problems** | |

---

## How to Use This Sheet

**Pattern 1 is non-negotiable first.** If `dummy.next` and pointer reassignment are not automatic, every other pattern produces subtle bugs.

**Patterns 2 and 3 run in parallel** once Pattern 1 is solid — they do not depend on each other.

**Pattern 4 synthesizes Patterns 2 and 3.** LC 148 (Sort List) is the clearest synthesis — do it only after both are clean.

**LC 143 (Reorder List) in Pattern 5 is the capstone problem** for the first four patterns. It combines find-mid, reverse, and merge-interleave. If you can solve it cleanly without hints, Patterns 1–4 are solid.

**Pattern 6 is small but the decision rule matters.** The two problems are not the point — knowing *when* to reach for a stack over in-place reversal is what Pattern 6 teaches.

**Design Problems last.** LRU and LFU require deep understanding of O(1) doubly linked list deletion — which only comes after Pattern 3 is fully internalized.