# Strings DSA Patterns

*"Problems are infinite, but patterns are finite!"*

---

## Step 1 — What's Actually New Here

Same filtering discipline as Arrays: Algomaster's Strings list is the only source (Fraz has no dedicated Strings sheet), and a large fraction of it is already the primary content of sheets you've built. Sort first, invent patterns second.

**Already fully covered elsewhere — cross-reference, don't re-teach:**

| Problem | Primary Home |
|---|---|
| LC 344. Reverse String | Two Pointers sheet, Pattern 5 |
| LC 125. Valid Palindrome | Two Pointers sheet, Pattern 5 |
| LC 680. Valid Palindrome II | Two Pointers sheet, Pattern 5 |
| LC 151. Reverse Words in a String | Two Pointers sheet, Pattern 5 |
| LC 392. Is Subsequence | Two Pointers sheet, Pattern 7 |
| LC 443. String Compression *(not in raw list, flagged for completeness)* | Two Pointers sheet, Pattern 7 |
| LC 3. Longest Substring Without Repeating Characters *(not in raw list, flagged)* | Sliding Window sheet, Pattern 2 |
| LC 5 / LC 647. Longest Palindromic Substring / Palindromic Substrings | Two Pointers sheet, Pattern 6 (Expand Around Center) |
| LC 72. Edit Distance *(full DP version)* | DP sheet, Pattern 7 (LCS) |
| LC 44 / LC 10. Wildcard / Regex Matching | DP sheet, Pattern 8 (String DP) |
| LC 271. Encode and Decode Strings | Data Structure Design sheet, Pattern 1 |
| LC 187. Repeated DNA Sequences | Bit Manipulation sheet, Pattern 4 |
| LC 394. Decode String | Stack sheet, Pattern 2 (cross-ref) / Recursion & Backtracking sheet |
| LC 616. Add Bold Tag in String | Intervals sheet, Pattern 1 (merge overlapping ranges on string indices) |

**Deliberately deferred, not dropped — future sheet owns the mechanism:**

| Problem | Correct Home |
|---|---|
| LC 49. Group Anagrams | Hashing sheet (next up) — frequency/sorted-string as hashmap key |
| LC 242. Valid Anagram | Could live in Hashing too, but the raw technique (fixed-size frequency array) is foundational enough to string problems that it earns a place here first; Hashing sheet will cross-reference back |

That leaves a focused, genuinely string-specific set of techniques — several of which fill real gaps flagged as "out of scope" in earlier sheets (the Binary Search sheet explicitly deferred Rabin-Karp/string hashing to "a String Hashing sheet [that] doesn't exist yet" — this is that sheet).

---

## The Core Mental Model — Before Any Pattern

Once palindrome-checking, sliding-window substrings, and DP-flavored matching are stripped out, "Strings" as its own topic covers four genuinely distinct raw ideas:

1. **A string's character composition is a fingerprint** — comparable via frequency counts, independent of order.
2. **A string concatenated with itself contains all its rotations** — a single non-obvious trick that unlocks several rotation/repetition problems at once.
3. **Careful simulation** — some problems have no clever trick at all; the skill is disciplined state-tracking through edge cases (this is a legitimate pattern, not an admission of no pattern).
4. **Finding a pattern inside a text efficiently** — the one area of CS where naive O(nm) scanning is a known bottleneck, solved by precomputing structure *within* the pattern itself (KMP, Z-function) rather than comparing from scratch at every position.

---

## Pattern Identification Quick Reference

| Pattern | Trigger |
|---|---|
| Frequency Fingerprinting & Equality | "valid anagram", "same character frequencies", "close strings" |
| Palindrome Constructability | "can rearrange into a palindrome", "longest palindrome you can build", parity of character counts |
| Longest Common Prefix | "common prefix", compare across many strings vertically/horizontally |
| Self-Concatenation / Doubling Trick | "is a rotation of", "repeated substring pattern" |
| Near-Match / Single-Edit Comparison | "at most one edit away", O(n) single pass, contrast with full edit distance |
| Simulation & Careful Parsing | "count and say", "zigzag", "justify text", "convert string to integer" — no algorithmic trick, disciplined state machine |
| String Matching (KMP / Z-Function) | "find all occurrences of pattern in text", "shortest palindrome via prefix", "longest happy prefix" |

---

## Pattern 1: Frequency Fingerprinting & Equality

**Identify:** Two strings (or one string against itself) need to be compared purely on **which characters occur and how many times** — order is irrelevant. A fixed-size array (26 for lowercase letters) or a hashmap captures this fingerprint in O(n), and equality of fingerprints is the entire check.

**LC 242 (Valid Anagram) — the anchor:** Build a frequency array for each string (or increment for one, decrement for the other, using a single array), and confirm every count returns to zero. This is the simplest possible fingerprint comparison and the base case every other problem in this pattern extends.

**LC 1657 (Determine if Two Strings Are Close) — fingerprint-of-the-fingerprint:** Two strings are "close" if you can freely swap any two characters and freely rename one character's identity to another's, as long as it's done consistently. This means two conditions on the frequency arrays, not one: (1) the **set** of characters used must be identical between the two strings (character identity can only be swapped, not invented), and (2) the **multiset of frequency values themselves**, sorted, must match (a character appearing 3 times in one string must be matched by *some* character appearing 3 times in the other). This is the genuinely new idea here — comparing the sorted list of frequencies, not the frequencies per-character.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 242. Valid Anagram | Frequency array increment/decrement — all counts return to zero |
| 2 | LC 1657. Determine if Two Strings Are Close | Same character set + same sorted multiset of frequencies — two separate fingerprint checks |

---

## Pattern 2: Palindrome Constructability (Parity Counting)

**Identify:** The question isn't "is this string a palindrome" (Two Pointers territory) or "find the longest palindromic substring within it" (Expand Around Center territory) — it's "can the characters of this string be **rearranged** into a palindrome, and if so, how long can that rearrangement be." This is a pure counting argument, no scanning of order at all.

**LC 409 (Longest Palindrome) — the anchor:** A palindrome can have at most **one** character with an odd count (sitting alone in the middle); every other character must contribute an even number of positions (one copy on each side of center). So: for every character, take the largest even number ≤ its frequency (`freq - (freq % 2)`) and sum these; then add 1 if *any* character had an odd frequency at all (to seat one odd-count character in the middle). This parity-counting insight — not any traversal of the string — is the entire solution.

**Why this deserves separation from Pattern 1:** Pattern 1 compares two fingerprints for *equality*. This pattern reasons about what a *single* fingerprint's parity structure permits you to *build*. The mechanism (frequency counting) looks similar, but the question being asked — equality vs. constructibility — is different enough that conflating them causes people to miss the parity insight and instead try to brute-force character rearrangement.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 409. Longest Palindrome | Parity counting — sum largest even count per character, +1 if any odd count exists |

---

## Pattern 3: Longest Common Prefix

**Identify:** Given multiple strings, find the longest prefix shared by all of them. There's no clever trick here beyond choosing a sensible scan order — worth naming as its own small pattern because the two scan directions (vertical vs. horizontal) are both valid and interviewers sometimes ask for the alternative as a follow-up.

**LC 14 — both variants, know both:**
- **Horizontal scanning:** treat the first string as the current answer, and repeatedly shrink it by comparing against each subsequent string's prefix, until it fits every string or becomes empty.
- **Vertical scanning:** for each character position `i`, check whether every string has the same character at position `i`; stop at the first mismatch or the first string that's too short.

**Why the vertical variant matters as a follow-up:** it terminates early — the moment a mismatch is found at position `i`, you're done, without needing to fully scan any string past that point. Horizontal scanning can end up re-scanning a long shared prefix repeatedly across many strings before catching a mismatch that vertical scanning would have caught immediately.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 14. Longest Common Prefix | Horizontal scan (shrink a running answer) or vertical scan (column-by-column, early exit) — know both |

---

## Pattern 4: Self-Concatenation / Doubling Trick

**Identify:** The problem asks whether one string is a **rotation** of another, or whether a string is built from repeating a smaller substring some number of times. Both questions have the exact same one-line trick: concatenating a string with itself contains every possible rotation of that string as a contiguous substring.

**LC 796 (Rotate String) — the anchor:** `s` is a rotation of `goal` if and only if `goal` is a substring of `s + s` (with the length check `len(s) == len(goal)` first, to avoid false positives). Every rotation of `s` — shifting by 0, 1, 2, ... positions — appears somewhere inside `s + s` as a contiguous window; this is what makes the trick correct, not a coincidence.

**LC 459 (Repeated Substring Pattern) — the same trick applied one level removed:** A string `s` is built by repeating some substring `k ≥ 2` times if and only if `s` is a substring of `(s + s)` **with the first and last characters of the doubled string removed**. Intuition: if `s` truly repeats, shifting it by the length of one repeated unit reproduces `s` again inside the doubled string, strictly before reaching the very end — trimming the two boundary characters is what forces the match to come from an internal shift rather than the trivial full-length match. *(This problem also has a KMP failure-function solution — see Pattern 7's cross-reference — worth knowing both because the doubling trick is O(n) but relies on a clever construction, while KMP's version generalizes better to "what is the repeating unit" rather than just yes/no.)*

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 796. Rotate String | `goal` is a rotation of `s` iff `goal` is a substring of `s+s` |
| 2 | LC 459. Repeated Substring Pattern | `s` is a repetition iff `s` is a substring of `(s+s)` with first/last chars trimmed — same doubling instinct, one twist |

---

## Pattern 5: Near-Match / Single-Edit Comparison

**Identify:** You need to check whether two strings differ by **at most one** insertion, deletion, or substitution — explicitly *not* the general "minimum edit distance" question (which is a DP problem with an unbounded number of edits). Because the edit budget is fixed at exactly one, this collapses to a single linear scan with no table needed at all.

**LC 161 (One Edit Distance) — the anchor:** Walk both strings simultaneously. The moment a mismatch is found: if the strings are the same length, it must be a substitution — skip both pointers and confirm the rest matches exactly. If the lengths differ by one, it must be an insertion/deletion — skip only the pointer of the longer string and confirm the rest matches exactly. If the lengths differ by more than one, or a second mismatch is found after the "used" edit, the answer is immediately false.

**Why this earns its own pattern rather than being "Edit Distance, easy mode":** The DP sheet's LCS Pattern 7 (Edit Distance) exists because the number of allowed edits is unbounded, which is exactly what forces a table of subproblems. Here, the edit budget is fixed at exactly one — no subproblem overlap exists at all, and reaching for DP machinery on this problem is a sign of not recognizing that the fixed budget removes the need for it entirely. Recognizing "small fixed budget → linear scan, don't reach for DP" is the transferable lesson, and it recurs (e.g., some Sliding Window "at most k replacements" problems flagged in that sheet's Pattern 2 use the same instinct at a slightly larger fixed budget).

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 161. One Edit Distance | Single linear scan, branch on length difference at the first mismatch — no DP table needed for a fixed budget of 1 |

---

## Pattern 6: Simulation & Careful Parsing

**Identify:** There is no clever algorithmic trick to discover — the problem describes a precise, multi-rule process (build a string, parse a string, lay out a string) and the entire difficulty is tracking state correctly through every edge case without an algorithmic shortcut. This is a legitimate, nameable pattern: recognizing "this is simulation, stop looking for a trick" is itself the skill, and it saves significant time versus hunting for a nonexistent clever insight.

**LC 38 (Count and Say) — the anchor:** Generate the next term by run-length-encoding the previous term (count consecutive identical digits, emit count-then-digit). Pure iterative construction, `n` times.

**LC 6 (Zigzag Conversion) — row simulation:** Rather than actually drawing a zigzag, maintain a `currentRow` index and a direction flag; append each character to a list-of-strings-per-row, flipping direction whenever you hit row 0 or the last row. The "trick," such as it is, is recognizing you don't need to compute final positions mathematically — simulating the back-and-forth row pointer is simpler and just as correct.

**LC 68 (Text Justification) — greedy line-packing plus careful spacing arithmetic:** Greedily pack as many words as fit within `maxWidth` per line (this part is an easy greedy). The actual difficulty is the spacing rules once a line is decided: for a full line with multiple words, distribute extra spaces as evenly as possible, with any remainder going to the leftmost gaps first; a line with only one word, or the final line, is left-justified with single spaces and padded on the right. Worth doing specifically because it's a high-frequency "no shortcuts, get every edge case right" interview problem.

**LC 8 (String to Integer / atoi) — the parsing-discipline anchor:** No single trick; the entire problem is a checklist executed in order — skip leading whitespace, consume an optional sign, consume digits while accumulating a running value, stop at the first non-digit, and clamp the result to the 32-bit signed integer range at every step (checking for overflow *before* it happens, not after, since the accumulated value can silently overflow a standard int if unchecked). This is the canonical "how carefully do you handle edge cases" string interview question.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 38. Count and Say | Run-length encode the previous term to build the next, iteratively |
| 2 | LC 6. Zigzag Conversion | Simulate row-by-row placement with a direction-flipping pointer |
| 3 | LC 68. Text Justification | Greedy line packing + precise space-distribution arithmetic, including single-word/last-line edge cases |
| 4 | LC 8. String to Integer (atoi) | Whitespace → sign → digits → overflow-clamp, in strict order — the canonical parsing-discipline problem |

---

## Pattern 7: String Matching Algorithms (KMP / Z-Function)

**Identify:** You need to find where a pattern occurs inside a text — possibly all occurrences, possibly just existence — and naive character-by-character comparison at every starting position is O(n·m) in the worst case. The fix in both classical algorithms below is the same core insight: **precompute structural information about the pattern itself** so that after a mismatch, you know how far you can safely skip without re-comparing characters you've already matched.

**Why this pattern was entirely absent from your reference until now:** The Binary Search sheet explicitly flagged Rabin-Karp/string hashing as "out of scope... revisit once a String Hashing sheet exists." This is that sheet. String matching deserves to live here rather than in Binary Search or Hashing, because the core technique (prefix-function precomputation) is a string-structural idea, not a search-space or hashing idea, even though Rabin-Karp itself does use hashing underneath.

**KMP's failure function (LCS prefix-suffix array) — the anchor concept:** For a pattern of length `m`, build `lps[i]` = the length of the longest proper prefix of `pattern[0..i]` that is *also* a suffix of `pattern[0..i]`. When matching against the text and a mismatch occurs at pattern position `j`, you don't restart from `j = 0` — you jump to `j = lps[j-1]`, because you already know that many characters immediately before the mismatch are guaranteed to match a prefix of the pattern, so re-checking them is provably wasted work. This is what gets the whole algorithm to O(n + m).

**LC 28 (Implement strStr / Find the Index of the First Occurrence) — the direct application:** Build the pattern's `lps` array once, then scan the text with the pattern pointer bouncing back via `lps` on mismatch instead of resetting to zero.

**LC 1392 (Longest Happy Prefix) — the `lps` array *is* the entire answer:** A "happy prefix" is exactly "a proper prefix that's also a proper suffix" — which is the literal definition of the last value in a string's own `lps` array (computed on the string against itself). No separate algorithm needed once KMP's failure function is understood — this problem exists specifically to test whether you understand what `lps` *means*, not just how to use it inside a search.

**LC 214 (Shortest Palindrome) — `lps` applied to a constructed string:** You need the longest prefix of `s` that is *also* a palindrome, so you can prepend the reverse of everything after it to make the whole string a palindrome. Construct a new string `s + '#' + reverse(s)` (the `#` separator prevents the two halves from bleeding into each other) and compute its `lps` array — the last value tells you exactly how long the palindromic prefix of the original `s` is. This is the hardest problem in this pattern precisely because recognizing *which* string to run KMP's preprocessing on is the actual insight, not the KMP mechanism itself.

**Z-function — the alternative worth knowing, briefly:** The Z-function computes, for every position `i`, the length of the longest substring starting at `i` that matches a prefix of the whole string. It solves the same class of pattern-matching problems as KMP (concatenate `pattern + '#' + text` and scan the Z-array for values equal to the pattern's length) and is arguably more intuitive to derive from scratch than KMP's failure function, though KMP is more commonly expected by name in interviews. Know that it exists and solves the identical problem class — treat it as a alternative lens on Pattern 7, not a separate pattern.

**Rabin-Karp — the hashing alternative, flagged not taught in depth:** Rolling hash of a fixed-length window lets you compare a pattern against every window of the text in O(1) amortized per shift, with O(n+m) total time and a small false-positive risk (mitigated by double-hashing or full string comparison on hash match). Useful to know as the "how would you do this differently" follow-up to LC 28, but the full treatment (modular arithmetic, collision handling) is a separate, deeper topic — flagged here rather than fully expanded, consistent with how the BST and Binary Search sheets flag Fenwick/Segment Trees as reference points rather than full write-ups.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 28. Find the Index of the First Occurrence in a String | KMP — build pattern's `lps` array, scan text with fallback-on-mismatch via `lps` |
| 2 | LC 1392. Longest Happy Prefix | The `lps` array's final value *is* the answer — tests understanding of what `lps` means, not just how to use it |
| 3 | LC 214. Shortest Palindrome | KMP on a constructed `s + '#' + reverse(s)` string — the hard part is choosing the right string to preprocess |
| — | LC 459. Repeated Substring Pattern *(cross-ref: Pattern 4)* | Alternate KMP solution via `lps`: string repeats iff `n % (n - lps[n-1]) == 0` |
| — | Z-Function (concept, no dedicated LeetCode anchor) | Alternative to KMP for the same matching-problem class — worth knowing exists |
| — | Rabin-Karp (concept, flagged not deep-dived) | Rolling-hash alternative — full treatment deferred, same as Fenwick/Segment Tree flags elsewhere |

---

## Final Summary

| Pattern | Problems (new) | Core Mechanism |
|---|---|---|
| Frequency Fingerprinting & Equality | 2 | Fixed-array/hashmap character counts, compared for equality |
| Palindrome Constructability | 1 | Parity counting — at most one odd-frequency character allowed |
| Longest Common Prefix | 1 | Horizontal shrink vs. vertical column scan — know both |
| Self-Concatenation / Doubling Trick | 2 | `s+s` contains every rotation — one trick, two applications |
| Near-Match / Single-Edit Comparison | 1 | Fixed edit budget of 1 → linear scan, no DP needed |
| Simulation & Careful Parsing | 4 | No trick — disciplined state tracking through edge cases |
| String Matching (KMP / Z-Function) | 3 (+2 cross-ref/concept) | Precompute pattern structure (`lps`) to skip redundant comparisons |
| **Total** | **14 new + 3 cross-referenced + 2 concept-only** | |

---

## How to Use This Sheet

**This sheet fills a structural gap your reference has been carrying since the Binary Search sheet was built.** String matching (KMP/Z-function/Rabin-Karp) was explicitly deferred there with a note to revisit "once a String Hashing sheet exists" — Pattern 7 here is that sheet. Treat it as required, not optional, precisely because it was flagged as a known gap rather than an oversight.

**Pattern 6 (Simulation) deserves a mindset shift, not a technique.** The moment you catch yourself hunting for a clever trick on LC 68 or LC 8 and not finding one, that absence *is* the signal — stop searching for an algorithm and start being rigorous about state and edge cases instead. This is a real interview skill: knowing when a problem has no shortcut is as valuable as knowing the shortcut when one exists.

**Pattern 7 is the hardest material in this sheet and should be done last, in the order shown.** LC 28 to build the mechanical `lps`-array skill, then LC 1392 to confirm you understand what `lps` *represents* (not just how to apply it), then LC 214 as the synthesis — applying the same tool to a cleverly constructed string rather than the raw input. Don't attempt LC 214 before LC 1392 clicks; the "which string do I run this on" insight is much harder to see cold.

**Pattern 4's two problems should be done in order (LC 796 → LC 459)** — the rotation check is the cleaner first exposure to the doubling trick before the repeated-substring variant adds its one extra wrinkle (trimming boundary characters).
