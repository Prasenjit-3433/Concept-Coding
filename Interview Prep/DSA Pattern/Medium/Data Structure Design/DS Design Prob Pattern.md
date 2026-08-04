# Data Structure Design Patterns

*"Problems are infinite, but patterns are finite!"*

---

## The Core Mental Model — Before Any Pattern

Design problems are different from algorithmic problems in one key way: **you are not asked to compute an answer, you are asked to build a machine that answers repeated queries efficiently.** The question is never "what is the answer" but "what combination of data structures gives O(1) or O(log n) for every required operation."

**The recurring meta-skill across this entire sheet:**
> List every operation the problem requires. For each operation, ask "what is the fastest data structure that supports this alone?" Then ask "which combination of those structures, kept in sync, supports all operations simultaneously?"

This is why almost every hard design problem is "HashMap + X" — the hashmap gives O(1) lookup, and X (a doubly linked list, a heap, a trie, a TreeMap) gives the ordering or priority the hashmap alone cannot.

**Two different things both get called "design" in interviews:**
- **DSA-flavored design** (this entire sheet): pick the right combination of data structures for O(1)/O(log n) operations, judged by a compiler.
- **True LLD** (Parking Lot, Elevator, Vending Machine): class diagrams, SOLID principles, extensibility — judged on paper, no compiler. Pattern 8 below sits at the seam between the two — LeetCode-judged, but rewards OOP-style state modeling, which is exactly what carries over into mixed LLD+DSA rounds.

---

## Pattern Identification Quick Reference

| Pattern | Trigger |
|---|---|
| Hashing-Based Design | "O(1) insert/delete/lookup", "encode/decode" |
| Randomized Design | "random pick", "equal probability", "blacklist" |
| Trie-Based Design | "prefix", "autocomplete", "wildcard", "file path", "stream matching" |
| Sliding Window / Streaming Counters | "count in last N seconds/calls", "first unique in stream" |
| Cache Design | "LRU", "LFU", "evict", "O(1) get and put" |
| Heap-Based Design | "top k", "highest rated", "merge feeds", "leaderboard", "assign seat" |
| Ordered Structure / Range Design | "range query", "nearest/farthest", "merge ranges dynamically" |
| Iterator & OOP Simulation Design | "peek", "wrap an iterator", "simulate a game/system with rules and state" |

---

## Pattern 1: Hashing-Based Design Fundamentals

**Identify:** The entire problem is solvable with O(1) hashmap/hashset operations, possibly paired with a plain array for O(1) positional access. There is no ordering requirement and no priority requirement — just fast membership, fast lookup, or a fast reversible encoding. If you find yourself reaching for anything beyond "map a key to a value" or "map a value to its position," you've likely wandered into a later pattern.

**LC 380's swap-and-pop trick (the idea every later "array + hashmap" pattern reuses):** Arrays give O(1) random access but O(n) arbitrary removal, because removing from the middle requires shifting everything after it. Fix: swap the element to remove with the array's last element, then pop the last element — O(1). The hashmap's job is to instantly locate *where* in the array a value currently lives, since swapping moves things around.

**LC 271's length-prefix trick:** Encoding strings by joining them with a delimiter breaks the moment a string contains that delimiter itself. Prefixing each string with its length (`5#hello`) removes the ambiguity entirely — the decoder always knows exactly how many characters to consume next, regardless of what's inside them.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 705. Design HashSet | Array of buckets + chaining — build the primitive before treating `HashMap`/`HashSet` as a black box |
| 2 | LC 706. Design HashMap *(cross-ref: LL sheet, Pattern 7)* | Separate chaining |
| 3 | LC 359. Logger Rate Limiter | HashMap of message → last-seen timestamp; allow if gap ≥ threshold |
| 4 | LC 271. Encode and Decode Strings | Length-prefix encoding — avoids delimiter collision ambiguity |
| 5 | LC 535. Encode and Decode TinyURL | HashMap + counter/hash as key — simplest possible encode/decode pair |
| 6 | LC 1656. Design an Ordered Stream | Array + pointer — advance pointer while consecutive values are already filled |

---

## Pattern 2: Randomized Design

**Identify:** The operation must return a **uniformly random** element in O(1), often under an added constraint — duplicates allowed, or certain values excluded. The default building block is "array for O(1) random-index access + hashmap for O(1) lookup, kept in sync via swap-and-pop" — the same trick from Pattern 1, now serving randomness instead of plain removal. The two hard problems in this cluster (LC 398, LC 710) each break that default in a specific way and require a genuinely different idea.

**LC 398 — reservoir sampling:** When you're asked for a random *index matching a target value*, and you don't want to pre-store every matching index (O(1) extra space constraint), you can't just jump to a random one. Instead, walk the array once: every time you see the target for the kth time, overwrite your current answer with this new index *with probability 1/k*. By the end, every occurrence has been included with equal probability — this is the general reservoir sampling technique, not just a trick for this one problem.

**LC 710 — remap the blacklist:** The valid answer range is `[0, n - blacklist.size())` — call this the "front" region. Some blacklisted numbers fall inside the front region and some don't. For every blacklisted number that *does* fall inside the front region, remap it (via a hashmap) to some whitelisted number that lives *outside* the front region. Now picking a uniformly random index in the front region and redirecting through the map when it hits a blacklisted entry gives correct uniform randomness in O(1).

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 380. Insert Delete GetRandom O(1) | Array + HashMap(value→index) — swap-and-pop removal |
| 2 | LC 381. Insert Delete GetRandom O(1) - Duplicates allowed | Same idea, but HashMap(value→Set of indices) since a value can live at multiple positions |
| 3 | LC 398. Random Pick Index | Reservoir sampling — uniform random choice in one pass, O(1) space |
| 4 | LC 710. Random Pick with Blacklist | Remap blacklisted values inside the valid range to whitelisted values outside it |
| — | LC 528. Random Pick with Weight *(cross-ref)* | Binary Search sheet, Pattern 8 — prefix sum + lower bound, a different randomness technique entirely |

---

## Pattern 3: Trie-Based Design

**Identify:** The problem revolves around **prefixes** — of words, of file paths, of a live stream of characters. A hashmap alone can tell you if an exact string exists, but it cannot efficiently answer "does *any* string start with this prefix" without scanning every entry. A trie answers that in O(length of prefix), because each node *is* a prefix, and walking down the tree one character at a time is the query.

**Why this pattern is worth building deliberately:** Autocomplete, spell-checkers, and in-memory file systems all reduce to "a tree where each node represents a prefix or path segment, and children are keyed by the next character or segment." Once that reframing clicks, LC 588 (file system) and LC 642 (autocomplete) stop looking like unrelated problems — they're the same trie with a different alphabet.

**LC 211's wildcard extension:** A `.` in the search pattern means "try every child at this node, not just one." This turns a simple O(m) trie walk into a DFS/backtracking search *over* the trie — the trie becomes the search space, not just a lookup table.

**LC 1032 — the trie built backward:** Most trie problems insert words forward and query prefixes forward. Stream of Characters flips this: you insert each *reversed* word into the trie, then on each new streamed character, walk the trie using the *most recent characters read backward*. The reversal is the entire insight — miss it and the problem looks unsolvable with a trie at all.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 208. Implement Trie (Prefix Tree) | Core template — node with children map + isEndOfWord flag |
| 2 | LC 211. Design Add and Search Words Data Structure | Trie + DFS backtracking — `.` wildcard explores all children |
| 3 | LC 1166. Design File System | HashMap of full-path→value — simpler prerequisite before LC 588's tree structure |
| 4 | LC 1268. Search Suggestions System | Trie + DFS per prefix, or sort + binary search — compare both approaches |
| 5 | LC 642. Design Search Autocomplete System | Trie/hashmap of prefixes + heap — rank completions by frequency then lexicographic order |
| 6 | LC 588. Design In-Memory File System | Trie-shaped directory tree — each node is a HashMap of name→child node |
| 7 | LC 1032. Stream of Characters | Trie built on **reversed** words — query walks backward through recently streamed characters |

---

## Pattern 4: Sliding Window / Streaming Counters

**Identify:** The problem asks for a running aggregate — a count, sum, product, or "first unique so far" — over the most recent window of *time* or the most recent *k* calls. This is a lighter-weight cousin of the Queue sheet's monotonic-deque pattern: there is no value-comparison invariant to maintain, just a window boundary defined by a timestamp or a call count, and a queue or running variable is enough to track it.

**LC 1352's zero-reset trick:** A running product breaks the instant a 0 is appended, because you can never divide a zero back out later. The fix: keep a list of *prefix products since the last zero*. Appending a zero clears this list and starts fresh. A query for "product of the last k" is a suffix product from this list — or 0 outright, if fewer numbers have been appended since the last reset than k.

**LC 1429's lazy discard:** "First unique number" in a stream means the front of a queue is only trustworthy if it hasn't since become a duplicate. Rather than removing duplicates the moment they occur, mark them in a hashset and let the queue's front lazily skip over anything no longer unique when `showFirstUnique()` is actually called.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 362. Design Hit Counter | Queue of timestamps — evict entries older than 300s on each query |
| 2 | LC 1352. Product of the Last K Numbers | Prefix-product array, reset on zero — query = suffix product via division |
| 3 | LC 1429. First Unique Number | Queue + HashSet — lazily discard duplicates from the queue's front |

---

## Pattern 5: Cache Design (HashMap + Doubly Linked List)

**Identify:** The problem needs O(1) get, O(1) put, and O(1) eviction under some ordering policy — least recently used, least frequently used, or least-recently-used-within-least-frequent. A plain hashmap gives O(1) lookup but has no concept of order; a plain doubly linked list gives O(1) reordering but has no concept of "find this key instantly." Every problem in this pattern is the same architectural answer: combine both, and keep them in sync on every operation.

**Why doubly linked, never singly:** Deleting a node in O(1) requires knowing its predecessor so you can stitch the list back together. A singly linked list only knows `next`, forcing an O(n) search for that predecessor. A doubly linked list hands you `prev` directly — this is the entire reason the design uses two links instead of one.

**LC 432 as a direct extension, not a new idea:** All O`one needs O(1) insert, increment, decrement, `getMaxKey`, and `getMinKey`. The mechanism is LFU's frequency-bucket idea taken one step further: a HashMap(key→node) plus a doubly linked list of **frequency buckets**, where each bucket itself holds the set of keys currently at that frequency. Max and min key are simply the first and last bucket in the list — no scanning required.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 146. LRU Cache *(cross-ref: LL sheet, Pattern 7)* | HashMap + Doubly LL — move accessed node to front, evict from back |
| 2 | LC 460. LFU Cache *(cross-ref: LL sheet, Pattern 7)* | HashMap + HashMap(freq→DLL) + min_freq tracking |
| 3 | LC 432. All O`one Data Structure | HashMap(key→node) + DLL of frequency buckets — generalizes LFU's bucket idea |

---

## Pattern 6: Heap-Based Design

**Identify:** The system must repeatedly answer "what is currently the best/highest/soonest-available item" while items are continuously added, updated, or consumed. This is Heap sheet territory (Patterns 1, 3, and 5) showing up as *one component* inside a larger stateful system, rather than as the entire algorithm — the design problem is figuring out what else needs to sit alongside the heap to make the whole system correct.

**LC 355 as K-Way Merge in disguise:** Each followed user's tweets already arrive in time order. Building a home timeline is exactly the K-Way Merge pattern — take the k most recent tweets across all followed users' individually sorted tweet lists, using a max-heap keyed on timestamp.

**LC 2353's recurring lazy-deletion need:** Ratings change over time, so a `(rating, food)` entry sitting inside the heap can go stale. The fix is the same one used for Skyline and Sliding Window Median in the Heap sheet: when you pop from the heap, check whether it still matches the current true rating in a hashmap. If it doesn't, discard it silently and pop again.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 1845. Seat Reservation Manager | Min-heap of available seat numbers — always reserve the smallest |
| 2 | LC 355. Design Twitter | HashMap(user→tweets) + K-Way Merge via max-heap on timestamp *(cross-ref: Heap sheet, Pattern 3)* |
| 3 | LC 2353. Design a Food Rating System | HashMap(food→rating) + max-heap per cuisine, with lazy deletion on stale ratings |
| 4 | LC 1244. Design A Leaderboard | HashMap(player→score) + sort-on-query, or a size-k heap for `top(k)` — compare both |
| — | LC 895, LC 716 *(cross-ref)* | Stack sheet, Pattern 2 — augmented stack, not a heap, despite the "max/frequency" framing |

---

## Pattern 7: Ordered Structure / Range Design

**Identify:** The problem needs dynamic range queries or "nearest/latest valid value" lookups — the same ordered-set skill as the BST sheet's Pattern 8, now applied as a standalone design problem rather than inside a traversal. Java's `TreeMap`/`TreeSet` is the tool of choice, and `floorKey`/`ceilingKey`/`floor`/`ceiling` are the operations that do essentially all the work.

**LC 855 — placement via nearest neighbors:** Exam Room asks you to seat someone as far as possible from the nearest occupied seat. A TreeSet of currently occupied seats lets you find the seat immediately before and after any candidate gap in O(log n), which is all you need to evaluate every gap between consecutive occupied seats and pick the largest.

**LC 715 — the hardest problem in this sheet:** Range Module must support adding, removing, and querying arbitrary real-number ranges, with overlapping or adjacent ranges merged automatically. Store the current state as a `TreeMap<start, end>` of disjoint ranges. Every add/remove must first locate all existing ranges that overlap the new one using `floorKey`/`ceilingKey`, then merge or split them — the bookkeeping, not the data structure choice, is what makes this hard.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 855. Exam Room | TreeSet of occupied seats — floor/ceiling to find the largest gap to place next |
| 2 | LC 715. Range Module | `TreeMap<start,end>` of disjoint ranges — merge/split via floor/ceiling on every update |
| 3 | LC 2276. Count Integers in Intervals | Harder Range Module variant — track total covered count alongside the ranges themselves |
| — | LC 981, LC 1146 *(cross-ref)* | Binary Search sheet, Pattern 8 — same ordered-retrieval skill, framed as timestamp lookup |

---

## Pattern 8: Iterator & OOP Simulation Design

**Identify:** Two flavors live here, and they share one instinct even though the mechanisms differ — **model or cache just enough state to answer the next call without recomputing from scratch.**
- **Iterator wrappers** control access to an existing sequence — peeking ahead, or flattening a nested structure lazily.
- **OOP simulation** models an entity whose internal state evolves across calls according to fixed rules — this is the flavor that bridges directly into LLD-style thinking, because the core skill is "what state does this object need to hold, and what does each operation mutate."

**LC 284's caching trick:** `peek()` must return the next element without consuming it from the underlying iterator. Solution: eagerly pull one element ahead of where the caller thinks you are, and cache it. `peek()` just reads the cache; `next()` reads the cache *and* refills it from the underlying iterator.

**LC 341 — recursion turned into an explicit stack:** A nested list is a tree wearing a different name. Flattening it lazily (rather than eagerly flattening the whole thing upfront) means maintaining a stack of "iterators into a list." `hasNext()` must dig into whatever nested list sits at the top of the stack, repeatedly, until it either finds an actual integer or the stack empties.

**LC 348 / LC 353 — state as counters and deques, not a scanned grid:** Tic-Tac-Toe never needs to scan the board to check for a win — maintain running counters per row, per column, and per diagonal, updated in O(1) on every move; a win is just a counter hitting ±n. Snake Game maintains the snake's body as a deque (push new head to the front, pop the tail from the back on each move) plus a hashset for O(1) self-collision checks — the grid itself is never stored.

**LC 1600 — a tree that mutates between queries:** Throne Inheritance models succession order as a DFS preorder traversal of a family tree. Births add children to the tree; deaths mark a node dead without removing it (a dead person can still have living descendants who inherit). The "system" being designed is really just this tree plus a preorder walk that skips dead nodes.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 284. Design Peeking Iterator | Cache one element ahead — `peek()` reads cache, `next()` reads and refills |
| 2 | LC 341. Flatten Nested List Iterator | Stack of iterators — lazy recursive flattening, dig in on `hasNext()` |
| 3 | LC 348. Design Tic-Tac-Toe | Row/column/diagonal running counters — O(1) win check per move, no grid scan |
| 4 | LC 353. Design Snake Game | Deque for the body (push front, pop back) + HashSet for O(1) self-collision check |
| 5 | LC 1600. Throne Inheritance | Tree + DFS preorder, mutated between queries — dead nodes skipped, not removed |
| 6 | LC 2296. Design a Text Editor | Two stacks (left of cursor / right of cursor, reversed) simulate cursor movement and edits |
| — | LC 1472, LC 173 *(cross-ref)* | LL sheet Pattern 5 (Browser History); BST sheet Pattern 5 (BST Iterator) — same "wrap and cache" instinct |

---

## Final Summary

| Pattern | Problems (new) | Core Mechanism |
|---|---|---|
| Hashing-Based Design | 6 (1 cross-ref) | HashMap/HashSet as the whole solution, or paired with an array |
| Randomized Design | 4 (1 cross-ref) | Array+HashMap random access; reservoir sampling; blacklist remapping |
| Trie-Based Design | 7 | Tree of prefixes/paths/streamed characters |
| Sliding Window / Streaming Counters | 3 | Queue or reset-based running aggregate |
| Cache Design | 3 (2 cross-ref) | HashMap + Doubly LL, generalized to frequency buckets |
| Heap-Based Design | 4 (2 cross-ref) | Heap as one component inside a larger stateful system |
| Ordered Structure / Range Design | 3 (2 cross-ref) | TreeMap/TreeSet for range/nearest-neighbor queries |
| Iterator & OOP Simulation Design | 6 (2 cross-ref) | Cache state, or model entity rules — the LLD-adjacent cluster |
| **Total** | **~36 new + 11 cross-referenced** | |

---

## How to Use This Sheet

**Pattern 1 first, always.** Every later pattern is "hashmap plus something else" — if raw hashmap/hashset design isn't automatic, the combinations in later patterns will feel arbitrary instead of obvious.

**Pattern 2 depends on Pattern 1's swap-and-pop trick.** Do LC 380 before LC 381, and don't attempt LC 398 or LC 710 until the basic array+hashmap sync is second nature — both hard problems bend that base idea in a specific direction.

**Pattern 3 has no dependency on 1 or 2** and can run in parallel, but treat it as genuinely new territory — LC 208 must be implementable cold before LC 211 or LC 1032, which both bend the base trie in a different direction (wildcard search, reversed insertion).

**Pattern 5 requires the Linked List sheet's Pattern 7 to already be solid.** LC 432 is a direct extension of LFU's bucket mechanism — attempting it before LRU/LFU are clean will feel like a brand-new problem instead of the natural next step it actually is.

**Pattern 6 requires Heap sheet Patterns 1 and 3.** The lazy-deletion trick in LC 2353 and the K-Way Merge in LC 355 are not re-taught here — if either feels unfamiliar, go back to the Heap sheet first.

**Pattern 7's LC 715 is the hardest problem in this entire sheet.** Attempt it only after BST sheet Pattern 8 (Ordered Set Applications) is fully solid — the floor/ceiling reasoning is identical, just applied to merging ranges instead of checking overlap. Do LC 855 first as a gentler on-ramp to TreeSet thinking.

**Pattern 8 last, and treat its two halves differently.** The iterator wrapper problems (284, 341) are mechanical once you've seen the trick. The OOP simulation problems (348, 353, 1600, 2296) are the ones worth over-preparing if you expect a mixed LLD+DSA round — the skill they test (what state to hold, what each operation mutates) is exactly what carries over when the interviewer swaps LeetCode's judge for a whiteboard.