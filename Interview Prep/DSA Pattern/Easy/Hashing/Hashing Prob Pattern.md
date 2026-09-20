# Hashing DSA Patterns

*"Problems are infinite, but patterns are finite!"*

---

## Step 1 — What's Actually New Here

Same discipline as Arrays and Strings: sort every problem into "already owned elsewhere" before inventing anything new. Hashing is the most over-subscribed topic in both source sheets — almost every topic uses a hashmap *somewhere*, so the real job is isolating what makes a problem's core insight specifically about **hashing as the technique**, not just an implementation detail borrowed for a problem that really belongs to another pattern.

**Already fully covered elsewhere — cross-reference, don't re-teach:**

| Problem | Primary Home |
|---|---|
| LC 169 / LC 229. Majority Element (I/II) | Arrays sheet, Pattern 2 (Boyer-Moore Voting) |
| LC 448. Find All Numbers Disappeared in an Array | Arrays sheet, Pattern 3 (Cyclic Sort — negation marking) |
| LC 560. Subarray Sum Equals K | Prefix Sum sheet, Pattern 2 |
| LC 974. Subarray Sums Divisible by K | Prefix Sum sheet, Pattern 2 |
| LC 1248. Count Number of Nice Subarrays | Prefix Sum sheet, Pattern 2 |
| LC 1074. Number of Submatrices That Sum to Target | Prefix Sum sheet, Pattern 4 |
| LC 705 / LC 706. Design HashSet / Design HashMap | Data Structure Design sheet, Pattern 1 |
| LC 535. Encode and Decode TinyURL | Data Structure Design sheet, Pattern 1 |
| LC 767. Reorganize String | Heap sheet, Pattern 1 |
| LC 49. Group Anagrams *(appears in both Fraz's "Implementation" and Algomaster's "Pattern Matching" buckets — kept here, see Pattern 2)* | — |

The "Hashing with prefix sum" bucket from Fraz's sheet is entirely a duplicate of work already done in the Prefix Sum sheet's Pattern 2 — those problems belong to prefix sum's monotonicity-breaking insight (negative numbers / exact-match need a hashmap lookup), not to hashing's own raw techniques. Re-listing them here would just be relabeling the same four problems under a different topic header, which is exactly the redundancy you asked me to cut.

What's left is a focused set of problems where the hashmap/hashset isn't a supporting actor for some other algorithm — it **is** the algorithm.

---

## The Core Mental Model — Before Any Pattern

Strip away every problem that's really "prefix sum + hashmap" or "cyclic sort" or "Boyer-Moore," and what remains as genuinely hashing-specific splits into four raw ideas:

1. **A hashmap/hashset turns "have I seen this before" into O(1)** — the simplest and most overused justification, worth naming precisely so it isn't confused with the more interesting ideas below.
2. **A carefully chosen transformation of a string/array produces a canonical key** — two different-looking inputs that are "the same" under some rule (anagram, shifted pattern) map to the *identical* key, so grouping becomes a single pass.
3. **A hashmap can enforce a bijection** — not just "does this map to that," but "does this map to that, *and* does the reverse mapping also hold consistently."
4. **A hashset's O(1) membership check turns a global structural question (is this the start of a run? how many distinct elements exist to my left?) into a per-element decision**, avoiding a sort or a second data structure entirely.

---

## Pattern Identification Quick Reference

| Pattern | Trigger |
|---|---|
| Frequency Counting Fundamentals | "contains duplicate", "ransom note", "good pairs", "first unique character" — direct seen-before or count check |
| String/Array Signature Grouping | "group anagrams", "group shifted strings" — canonical key groups equivalent items |
| Bijective Character/Word Mapping | "isomorphic", "word pattern" — mapping must hold consistently in both directions |
| Hashset for Sequence & Range Detection | "longest consecutive sequence", "number of distinct characters on each side" — O(1) membership replaces sorting |
| Greedy Frequency-Driven Construction | "split into consecutive subsequences", "minimum deletions for unique frequencies" — frequency map drives a greedy decision |
| Hashmap as Matching Automaton | "number of matching subsequences" — bucket items by "what they're waiting for next" |

---

## Pattern 1: Frequency Counting Fundamentals

**Identify:** The question reduces to "has this value been seen," "how many times has it been seen," or "does the multiset of one thing cover the multiset of another." No transformation of the key is needed — the raw value (or a raw character) is the key itself. This is the baseline every other pattern in this sheet builds on; if a problem needs nothing more than this, don't overthink it.

**LC 217 vs LC 219 — the window constraint is the only new idea:** Contains Duplicate is a plain hashset membership check. Contains Duplicate II adds "within distance k" — which means storing the **last seen index** per value (not just presence) and checking `i - lastIndex[val] <= k` before updating. The moment "does X exist" becomes "does X exist *nearby*," the hashmap's value has to carry position information, not just act as a set.

**LC 350 (Intersection of Two Arrays II) — frequency map as a shared budget:** Build a frequency map of one array, then walk the second array and, for each element present with a remaining positive count, include it once and decrement. This is a "spend from a budget" mental model — every hashmap-based multiset-intersection problem reduces to this.

**LC 383 (Ransom Note) — the same budget idea, phrased as a feasibility check:** Build a frequency map of `magazine`, then decrement for each character needed in `ransomNote`; if any count goes negative, the note is infeasible. Structurally identical to LC 350, just returning a boolean instead of a collected list.

**LC 1189 (Max Number of Balloons) — budget check with an uneven consumption rate:** The word "balloon" needs two `l`s and two `o`s per copy — so the answer is `min(count[c] // requiredCount[c])` across only the letters that actually appear in "balloon." The insight worth naming: when the "resource" required per unit isn't 1-for-1, divide each available count by its *required* count before taking the minimum, rather than just taking the raw minimum count.

**LC 1512 (Number of Good Pairs) — frequency count feeding a combinatorial formula, not a lookup:** Rather than checking pairs directly, count occurrences of each value; a value seen `k` times contributes `k*(k-1)/2` pairs. This is the same triangular-number instinct as the Arrays sheet's run-length pattern, applied to value-frequency instead of run-length.

**LC 387 (First Unique Character in a String) — frequency map, then a second pass for order:** A single frequency pass tells you *which* characters are unique, but the answer needs the *first index* — so a second pass over the original string (not the frequency map, which has no order) finds the first character whose count is exactly 1. The lesson: a hashmap destroys order, so if the question needs positional information, you need a second, order-preserving pass.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 217. Contains Duplicate | Plain hashset membership — the baseline |
| 2 | LC 219. Contains Duplicate II | Hashmap of value → last seen index; check distance before updating |
| 3 | LC 350. Intersection of Two Arrays II | Frequency map as a spendable budget — decrement on match |
| 4 | LC 383. Ransom Note | Same budget idea as feasibility check — any count going negative fails |
| 5 | LC 1189. Maximum Number of Balloons | Budget check with uneven per-unit consumption — divide before taking the min |
| 6 | LC 1512. Number of Good Pairs | Frequency count feeds a triangular-number combinatorial formula |
| 7 | LC 387. First Unique Character in a String | Frequency map identifies candidates; a second ordered pass finds the first one |

---

## Pattern 2: String/Array Signature Grouping

**Identify:** Multiple strings (or arrays) need to be grouped by some notion of "these are structurally equivalent," where equivalence isn't literal string equality. The technique: define a **canonical transformation** such that all equivalent inputs map to the exact same key, then group by that key in a single hashmap pass.

**LC 49 (Group Anagrams) — the anchor:** Two strings are anagrams iff their sorted character sequences are identical. Sort each string to produce its key (`O(k log k)` per string of length `k`), or alternatively use a fixed-size character-count tuple as the key (`O(k)` per string, better for long strings with a small alphabet) — both are valid canonical-key choices, and knowing the count-array alternative is a legitimate "can you do better" follow-up.

**LC 249 (Group Shifted Strings) — same instinct, a different equivalence rule:** Two strings belong together if every corresponding pair of characters has the same **circular difference** (e.g., `"abc"` and `"bcd"` both have consecutive `+1` shifts between each character). The canonical key here is the sequence of differences between adjacent characters (mod 26, to handle wraparound like `z → a`), not the sorted string — recognizing that the *equivalence rule itself* determines what the canonical key must encode is the actual transferable skill, not memorizing "sort for anagrams."

**Why these two belong in one pattern despite different keys:** The mechanism — "find a transformation where equivalent inputs collide, use it as a hashmap key, group in one pass" — is identical. What changes is *what makes two things equivalent*, which is a property of the problem statement, not of hashing itself. Seeing LC 49 and LC 249 back to back is what turns "sort the string" from a memorized trick into "find the canonical form implied by the equivalence relation."

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 49. Group Anagrams | Canonical key = sorted string (or character-count tuple) — group by identical key |
| 2 | LC 249. Group Shifted Strings | Canonical key = sequence of adjacent circular differences — different equivalence rule, same grouping mechanism |

---

## Pattern 3: Bijective Character/Word Mapping

**Identify:** You need to verify a **one-to-one, consistent** correspondence between two sequences — not just "does A map to B" but "does A map to B, and does nothing else also map to B, and does B map back only to A." A single hashmap checking one direction is a common bug here; genuine bijection requires checking both directions simultaneously (or maintaining two hashmaps).

**LC 205 (Isomorphic Strings) — the anchor, and where the bug lives:** `s = "ab"`, `t = "aa"` should return false — `a → a` and `b → a` would pass a single one-directional hashmap check (`map[s[i]] == t[i]` is never contradicted on first sight if you only check forward), but this isn't a valid bijection because two different characters in `s` both map to the same character in `t`. The fix: maintain two hashmaps, `s→t` and `t→s`, and verify both directions agree at every position — or equivalently, check that `map.get(s[i]) == t[i]` AND `reverseMap.get(t[i]) == s[i]` before establishing a new mapping.

**LC 290 (Word Pattern) — the identical bijection check, one level up:** Instead of character-to-character, this is pattern-character-to-word. Split the string into words, then run the exact same two-hashmap bijection check as LC 205, substituting "word" for "character." Doing this immediately after LC 205 is what makes the pattern generalize — the unit being mapped (character vs. word) is incidental; the two-way consistency check is the actual content of the pattern.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 205. Isomorphic Strings | Two-way hashmap check — forward AND reverse mapping must both hold, or a false positive slips through |
| 2 | LC 290. Word Pattern | Identical bijection check, applied to pattern-character ↔ word instead of character ↔ character |

---

## Pattern 4: Hashset for Sequence & Range Detection

**Identify:** A hashset's O(1) membership check replaces what would otherwise require sorting or a second pass, specifically for questions about **runs, ranges, or distinct-count boundaries**. The recognition signal: the question involves "consecutive" or "distinct up to this point" — concepts that sound like they need order, but a hashset resolves them without one.

**LC 128 (Longest Consecutive Sequence) — the anchor, and the trick that makes it O(n):** Put every number in a hashset, then for each number, only **start counting a sequence if `num - 1` is not in the set** (i.e., this number is the start of a run). From a genuine start, walk forward (`num+1`, `num+2`, ...) checking set membership until the run breaks, tracking the longest. The `num - 1` check is what keeps this O(n) overall — without it, every number would redundantly re-walk sequences that a smaller starting number already covered, degrading to O(n²). This single guard condition is the entire difference between a correct O(n) solution and an accidentally-quadratic one that looks correct.

**LC 1750 (Number of Good Ways to Split a String) — hashset size as a distinct-count proxy:** For every split point `i`, you need "number of distinct characters in `s[0..i]`" and "number of distinct characters in `s[i+1..n]`" to be equal. Precompute, in one left-to-right pass, a running hashset (or frequency array counting nonzero entries) giving the distinct-count-so-far at every prefix position, and symmetrically for suffixes. Then a single pass comparing prefix-distinct-count against suffix-distinct-count at each split point answers the whole problem in O(n) total — the hashset's job here is quietly converting "distinct count" into an O(1)-amortized running value instead of recomputing it per split point.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 128. Longest Consecutive Sequence | Hashset membership + "only start counting at true run starts" — the guard that makes it O(n) |
| 2 | LC 1750. Number of Good Ways to Split a String | Running distinct-count via hashset/frequency array, precomputed prefix and suffix, compared per split point |

---

## Pattern 5: Greedy Frequency-Driven Construction

**Identify:** A frequency map doesn't just answer a query — it **drives a greedy construction or repair decision**, where at each step you consult current counts to decide what to do next, and the counts themselves change as a result of your decisions. This is a step up from Pattern 1's static budget-checking: here the map is actively mutated as part of the algorithm's logic, not just decremented until zero.

**LC 1647 (Minimum Deletions to Make Character Frequencies Unique) — the anchor:** Build a frequency map, then process frequencies from **highest to lowest** (sort them descending). For each frequency, if it's already lower than the previous kept frequency (or is 0), keep it as-is; otherwise, reduce it (deleting characters) until it's strictly lower than the last kept frequency, accumulating the deletions. The greedy insight: always shrink the *current* frequency down to fit under what's already been fixed, rather than trying to reason about all frequencies globally at once — each decision only depends on the immediately preceding kept value.

**LC 659 (Split Array into Consecutive Subsequences) — the hardest problem in this pattern, two maps working together:** Maintain a frequency map of remaining counts and a second map tracking how many subsequences currently end at each value, looking for an extension point. For each number encountered: first try to **append it to an existing subsequence** that ends at `num - 1` (greedy — extending existing runs is always at least as good as starting new ones, since it never creates a shorter run than necessary); if none exists, try to **start a new subsequence** of length 3 using `num, num+1, num+2` if all are available; if neither works, the split is impossible. The two hashmaps working in tandem — one for "what's left to use," one for "what's actively extendable" — is the genuinely new mechanical idea this problem teaches.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 1647. Minimum Deletions to Make Character Frequencies Unique | Sort frequencies descending, greedily shrink each to fit strictly under the last kept value |
| 2 | LC 659. Split Array into Consecutive Subsequences | Two hashmaps — remaining counts + active-extension tracking; prefer extending over starting fresh |

---

## Pattern 6: Hashmap as a Matching Automaton

**Identify:** You need to check many strings against a single reference string for the "is a subsequence" relationship, and doing it one-by-one (each an O(n) scan of the reference) is too slow when there are many query strings. The fix: bucket each query string by **the single character it's currently waiting for next**, then make just one pass over the reference string, advancing whichever queries are waiting for the character currently being scanned.

**LC 792 (Number of Matching Subsequences) — the anchor, and the only problem needing this idea:** Maintain a hashmap from `character → list of (word, positionInWord)` pairs currently waiting for that character. Scan the reference string `s` once, character by character; at each character `c`, take every word currently waiting for `c`, advance its position pointer by one, and re-bucket it under whatever character it's waiting for next (or mark it complete if it's exhausted). This turns what looks like `O(words × |s|)` naive checking into a single `O(|s| + total word length)` pass — the hashmap's role is fundamentally different from every other pattern in this sheet: it's not storing a fingerprint or a count, it's storing **which pieces of unfinished work are waiting for the current input symbol**, which is a genuinely different use of a hashmap worth recognizing on its own.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 792. Number of Matching Subsequences | Bucket words by "next character needed"; one pass over the reference string advances all waiting words simultaneously |

---

## Final Summary

| Pattern | Problems (new) | Core Mechanism |
|---|---|---|
| Frequency Counting Fundamentals | 7 | Seen-before checks, budget-style decrementing, combinatorial counting from frequencies |
| String/Array Signature Grouping | 2 | Canonical key derived from the equivalence rule, group by identical key |
| Bijective Character/Word Mapping | 2 | Two-way hashmap check — forward and reverse must both hold |
| Hashset for Sequence & Range Detection | 2 | O(1) membership replaces sorting for run-starts and distinct-count boundaries |
| Greedy Frequency-Driven Construction | 2 | Frequency map mutates as the algorithm's greedy decisions are made |
| Hashmap as Matching Automaton | 1 | Bucket by "what's needed next," one pass advances all waiting items |
| **Total** | **16 new** | |

---

## How to Use This Sheet

**Pattern 1 is the floor, not the ceiling — resist the temptation to pad it further.** Every problem in it is genuinely a variation of "map value to count or last-seen-index," and the risk in Hashing specifically (more than any other topic) is treating every hashmap usage as worth its own bullet point. If a problem's *only* insight is "use a hashmap instead of nested loops," it belongs in Pattern 1 or it doesn't belong in this sheet at all (it belongs to whatever topic the rest of the problem is actually testing — as the cross-reference table at the top shows).

**Pattern 2 and Pattern 3 are easy to conflate — don't.** Signature grouping (Pattern 2) asks "which things are equivalent to each other," a many-to-many-within-groups relationship. Bijective mapping (Pattern 3) asks "does this one specific correspondence hold consistently," a strict one-to-one relationship. LC 49's anagram groups can have any number of members; LC 205's isomorphism fails the instant a second character tries to reuse a mapping. Say which question you're actually answering before choosing which check to write.

**Pattern 4's LC 128 is the single most important problem in this sheet.** The "only start counting from a true run start" guard is a genuinely subtle correctness-vs-complexity insight that's easy to get *accidentally correct but slow* on — walk through why the naive version (checking every number as a potential start) is still correct but O(n²) before appreciating why the guard condition matters.

**Pattern 5 should be done after Pattern 1 is completely automatic.** Both problems here start from a plain frequency map but immediately do something more demanding with it (sorting and greedy-shrinking in LC 1647, dual-map coordination in LC 659) — if basic frequency counting isn't reflexive yet, the added greedy layer will obscure which part is "hashing" and which part is "greedy reasoning."

**Pattern 6 is a single problem, and that's intentional, not a gap.** LC 792's "bucket by what's needed next" idea is genuinely distinct from everything else in your entire reference — it doesn't generalize into a family of LeetCode problems the way the other patterns do, but it's a technique worth having filed away on its own, because when a variant of it *does* show up, nothing else in this sheet will trigger the recognition.
