# Math & Geometry DSA Patterns

*"Problems are infinite, but patterns are finite!"*

---

## Step 1 — What's Actually New Here

Math & Geometry is the topic most likely to smuggle in problems that are secretly DP, Recursion, Heap, or Two Pointers wearing a numeric costume. Filter first.

**Already fully covered elsewhere — cross-reference, don't re-teach:**

| Problem | Primary Home |
|---|---|
| LC 202. Happy Number | Two Pointers sheet, Pattern 9 (cycle detection on an implicit sequence) |
| LC 287. Find the Duplicate Number | Two Pointers sheet, Pattern 9 (Floyd's on array-as-graph) |
| LC 50. Pow(x, n) | Recursion & Backtracking sheet, Pattern 1 (fast exponentiation) |
| LC 60. Permutation Sequence | Recursion & Backtracking sheet, Pattern 4 (factorial number system) |
| LC 62. Unique Paths | DP sheet, Pattern 3 (DP on Grid) — the combinatorial closed form is a valid alternative, noted there |
| LC 973. K Closest Points to Origin | Heap sheet, Pattern 2 (Top K) |
| LC 1230. Toss Strange Coins | DP sheet, Pattern 15 (Probability DP) |
| LC 611. Valid Triangle Number | Two Pointers sheet, Pattern 1 (counting variant) |
| LC 1569. Number of Ways to Reorder Array to Get Same BST | BST sheet, Pattern 7 (BST + DP Crossover) |
| LC 2779. Minimum Rectangles to Cover Points | Greedy sheet, Pattern 2 (interval/coverage greedy — sort + sweep) |

**On the raw Codeforces combinatorics problems** (1178C, 52B, 1312D, 300C, 895D): these have no LeetCode home and no single canonical write-up target — they're competitive-programming drills for the *general* combinatorics toolkit (stars-and-bars, inclusion-exclusion, counting via generating-function intuition), not individually distinct patterns. Rather than force each into a fake three-stage note, they're grouped as "practice pool" under Pattern 4 once the underlying identities are understood — padding this sheet with under-specified CF problems would violate the same "don't force coverage" principle you've applied everywhere else.

What's left is genuinely raw: number theory primitives, digit/base manipulation, combinatorial counting, and geometric primitives (lines, areas, circles) that don't reduce to any data structure you've already built a sheet around.

---

## The Core Mental Model — Before Any Pattern

Math & Geometry splits into two very different mental postures:

1. **Number theory / combinatorics** — the skill is recognizing a *closed-form or formulaic* answer exists at all, so you stop trying to simulate or brute-force something that has an O(1) or O(log n) identity behind it (GCD, modular exponentiation, counting formulas).
2. **Geometry** — the skill is almost always **translating a visual/spatial claim into an algebraic condition** (collinearity → cross product is zero; overlap → interval intersection on each axis separately; area → shoelace formula) so that a picture-based problem becomes ordinary arithmetic, with the usual traps being floating-point precision and edge-case degeneracies (vertical lines, zero-area shapes).

---

## Pattern Identification Quick Reference

| Pattern | Trigger |
|---|---|
| Number Theory Fundamentals | "GCD/LCM", "count primes", "sieve", divisor/multiple relationships |
| Digit & Base Manipulation | "reverse a number", "palindrome number", "column title", digit-by-digit carry/overflow |
| Big-Number String Arithmetic | "multiply two large numbers as strings", numbers exceed integer range |
| Combinatorics — Counting Without Enumeration | "number of ways", "distribute items", "count arrangements/anagrams" without needing to enumerate |
| Geometric Primitives — Lines & Collinearity | "straight line", "points on a line", "do two segments intersect" |
| Geometric Primitives — Area & Shape Validity | "valid square/triangle", "area of triangle/rectangle", "do rectangles overlap" |
| Circle Geometry & Candidate Enumeration | "points inside a circle", "circle overlaps rectangle", "smallest circle covering darts" |
| Path Simulation Geometry | "does the path cross itself", "light ray reflection", "cuts to divide a circle" |

---

## Pattern 1: Number Theory Fundamentals

**Identify:** The problem is really asking about divisibility, common factors, or primality — and a brute-force check (trial division per query, or checking every number up to n) is asymptotically wasteful once you recognize the underlying number-theoretic identity.

**GCD/LCM via the Euclidean Algorithm — the foundation:** `gcd(a, b) = gcd(b, a % b)`, terminating when `b = 0`. `lcm(a, b) = (a * b) / gcd(a, b)`. This single recurrence, taking `O(log(min(a,b)))` time, underlies every problem in this pattern and reappears constantly elsewhere in your reference (Bit Manipulation's LC 1998 GCD-Sort, Intervals' scheduling problems) — it's worth having so automatic that using it inside a harder problem never costs a second thought.

**Sieve of Eratosthenes — the batch version of primality:** Checking "is n prime" one number at a time is `O(√n)` per query. When you need primality for *every* number up to `N`, mark composites in a single `O(N log log N)` pass instead: start from 2, and for every unmarked number, mark all its multiples as composite. **LC 204 (Count Primes)** is the direct, unmodified application — the entire problem is implementing the sieve correctly (starting the inner marking loop at `i*i`, not `2*i`, is the standard micro-optimization worth knowing).

**LC 1922 (Count Good Numbers) — modular exponentiation as the payoff:** A "good number" has even-indexed positions filled from 5 choices (even digits) and odd-indexed positions from 4 choices (prime digits), so the total count is `5^(ceil(n/2)) * 4^(floor(n/2))`, computed mod `10^9+7`. Since `n` can be huge, this *requires* fast exponentiation (the same halving recursion as LC 50, cross-referenced above) combined with a combinatorial counting insight — the problem exists specifically to force those two ideas together.

**LC 258 (Add Digits) — the digital-root shortcut:** Repeatedly summing a number's digits until one digit remains looks like it needs simulation, but the result is always `1 + (n - 1) % 9` (with the special case `n = 0 → 0`) — a direct consequence of modular arithmetic base-9 periodicity. Worth knowing as the canonical "a loop can be replaced with one modular formula" example.

| # | Problem | Key Concept |
|---|---|---|
| 1 | Theory: GCD/LCM (Euclidean Algorithm) | `gcd(a,b) = gcd(b, a%b)` — O(log(min(a,b))), the foundation everything else reuses |
| 2 | Theory: Sieve of Eratosthenes | Mark composites in one O(N log log N) pass instead of checking primality per-query |
| 3 | LC 204. Count Primes | Direct sieve application |
| 4 | LC 1922. Count Good Numbers | Combinatorial counting formula + modular fast exponentiation |
| 5 | LC 258. Add Digits | Digital root via `1 + (n-1) % 9` — modular-arithmetic shortcut replaces simulation |
| — | LC 1979. Find Greatest Common Divisor of an Array *(not in original list, added — the canonical GCD warm-up)* | `gcd(max, min)` of the array — GCD is associative and monotonic under min/max here |

---

## Pattern 2: Digit & Base Manipulation

**Identify:** The problem operates on a number's individual digits (reversing them, checking symmetry, converting between number systems) with a fixed small radix. The technique is always some form of `while (n > 0) { digit = n % base; n /= base; ...}`, and the main interview signal is careful handling of overflow and edge cases (negative numbers, leading/trailing zeros), not algorithmic cleverness.

**LC 7 (Reverse Integer) — the anchor, overflow-checking as the actual skill:** Peel digits with `% 10` and `/ 10`, building the reversed number one digit at a time. The entire difficulty is checking for 32-bit signed overflow **before** it happens (comparing against `INT_MAX / 10` and the last digit, rather than letting the multiplication overflow and then detecting it after the fact) — same discipline as the Strings sheet's atoi problem, applied to a cleaner, more contained case.

**LC 9 (Palindrome Number) — the O(1)-extra-space refinement:** Converting to a string and checking palindrome-ness works, but the numeric refinement is to only reverse **half** the number: peel digits off the original from the back while building a reversed number, stopping once the reversed half is ≥ the remaining original half, then compare the two halves directly (adjusting for odd digit-counts by dividing the reversed half by 10). This avoids both string conversion and any overflow risk from reversing the *entire* number.

**LC 168 (Excel Sheet Column Title) — bijective base-26, the trap-worthy variant:** This looks like ordinary base conversion, but standard base-26 has a digit "0," while Excel's system does not — there's no letter for zero, so column 26 is "Z," not "A0." The fix: before each division, subtract 1 first (`n -= 1; letter = n % 26; n /= 26`), which shifts the range from `[0,25]` per digit to a bijective `[1,26]` system with no zero digit. Recognizing "this is base conversion but without a zero digit" is the entire problem.

**LC 66 (Plus One) — carry propagation on a digit array:** Walk from the last digit backward, incrementing and propagating a carry; if the carry survives past the first digit, prepend a new leading 1. This is the array-based sibling of the Linked List sheet's "Add 1 to a Number Represented by a Linked List" — same carry logic, different underlying structure (direct array indexing here means no need to reverse-traverse via recursion/stack).

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 7. Reverse Integer | Digit-by-digit reversal with overflow checked *before* it happens |
| 2 | LC 9. Palindrome Number | Reverse only half the digits — avoids full reversal and overflow entirely |
| 3 | LC 168. Excel Sheet Column Title | Bijective base-26 — subtract 1 before each division since there's no zero digit |
| 4 | LC 66. Plus One | Carry propagation on a digit array, from the last index backward |
| 5 | LC 13. Roman to Integer | Left-to-right scan, subtract when a smaller-value symbol precedes a larger one |

---

## Pattern 3: Big-Number String Arithmetic

**Identify:** The numbers involved exceed what a standard integer type can hold, so arithmetic must be simulated digit-by-digit on their string representations — the same carry-propagation instinct as Pattern 2, but now with two full numbers instead of a single increment.

**LC 43 (Multiply Strings) — the anchor:** Multiplying two numbers of length `m` and `n` produces a result of at most `m+n` digits. The trick that avoids repeated string concatenation: allocate a result array of size `m+n` up front, and for every digit pair `(i, j)`, add their product directly into `result[i+j+1]` (with the carry rippling into `result[i+j]`) — this exploits the fact that multiplying the digit at position `i` (from the end) by the digit at position `j` always contributes to exactly positions `i+j` and `i+j+1` in the result, regardless of what other digit pairs have already been processed, so every contribution can be accumulated independently before a single final carry-cleanup pass.

**LC 1071 (Greatest Common Divisor of Strings) — GCD's logic ported to string concatenation:** Two strings have a valid "common divisor string" only if `str1 + str2 == str2 + str1` (if one exists at all, this equality is both necessary and sufficient). Once confirmed, the length of the answer is `gcd(len(str1), len(str2))`, and the answer itself is simply `str1`'s prefix of that length — a direct reuse of Pattern 1's GCD, applied to string lengths instead of numeric values, once the concatenation-equality check establishes that a common divisor exists at all.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 43. Multiply Strings | Digit-pair contributions land at fixed `result[i+j]`/`result[i+j+1]` positions — accumulate, then carry-cleanup once |
| 2 | LC 1071. Greatest Common Divisor of Strings | `str1+str2 == str2+str1` check, then answer length = `gcd(len1, len2)` |

---

## Pattern 4: Combinatorics — Counting Without Enumeration

**Identify:** The problem asks "how many ways," "how many arrangements," or "how many valid sequences" — with a search space far too large to enumerate. The skill is recognizing which combinatorial identity (permutations with repetition, stars-and-bars, inclusion-exclusion) directly computes the count in closed form or via a manageable recurrence, without ever generating a single actual arrangement.

**Theory — nCr mod a large prime, the building block:** Computing `C(n, r) mod p` for large `n` requires modular inverse (via Fermat's Little Theorem, since `p` is prime: `a^(p-1) ≡ 1 (mod p)`, so `a^(-1) ≡ a^(p-2) (mod p)`), combined with precomputed factorials and their modular inverses up to `n`. This single piece of machinery — precomputed factorial + modular-inverse-factorial arrays — is the entry point for every "count mod 1e9+7" combinatorics problem you'll encounter.

**LC 2927 (Distribute Candies Among Children II) — stars-and-bars plus inclusion-exclusion:** Without an upper limit per child, distributing `n` candies among 3 children is a pure stars-and-bars count: `C(n+2, 2)`. The upper limit per child (`limit`) breaks this directly, so inclusion-exclusion subtracts off the "at least one child exceeds `limit`" cases: for each of the (up to 3) children that could individually exceed the limit, subtract the count where that child has more than `limit`, then add back the doubly-over-counted cases where two children simultaneously exceed it, following the standard inclusion-exclusion sign-alternation pattern.

**LC 2850 (Minimum Moves to Spread Stones Over Grid) — deceptive naming, actually assignment, not combinatorics; flagged and excluded** — worth a one-line note *why* it's not here: it reduces to a min-cost bipartite matching / brute-force permutation of at most 9 cells, which is a search/assignment problem, not a counting formula. Included here only to explicitly show the filtering discipline at work — not every "distribute stones/candies" problem belongs to the same family.

**LC 2850 (Count Anagrams) — actually, the correctly-cited combinatorics problem — multinomial coefficients:** For each word, the number of distinct arrangements of its letters is `(length)! / (freq[c1]! * freq[c2]! * ... )` — the standard multinomial-coefficient formula for permutations with repeated elements. Summed (as a product, since the question asks about a sentence) across every word in a sentence, with all factorials precomputed mod `10^9+7` using Pattern 4's theory building block.

**LC 1359 (Count All Valid Pickup and Delivery Options) — a counting recurrence built from a placement argument:** With `n` pickup/delivery pairs already validly placed, adding pair `n+1` can insert its pickup into any of `2n+1` gaps, and its delivery into any of the `2n+2` gaps *after* the pickup's position — giving the recurrence `ways(n) = ways(n-1) * (2n-1) * (2n)`. The insight worth naming: rather than trying to count all valid final sequences directly, count how many new valid insertion choices are added at each step, exactly the same "build up one element at a time, count local choices" instinct used in many combinatorics derivations.

**Advanced / optional practice pool** — the Codeforces problems and the harder LeetCode combinatorics problems (LC 2338 Count the Number of Ideal Arrays, LC 2954 Count the Number of Infection Sequences) all extend this same toolkit (stars-and-bars, multinomial counting, inclusion-exclusion) to progressively less obvious setups. They're intentionally not given individual three-stage treatment here — once the nCr-mod-p building block and the stars-and-bars/inclusion-exclusion instinct from LC 2927 are solid, these become "harder applications of the same toolkit" to practice independently, not new concepts to learn.

| # | Problem | Key Concept |
|---|---|---|
| 1 | Theory: nCr mod p via Modular Inverse | Precomputed factorials + Fermat's Little Theorem — the building block for all mod-counting problems |
| 2 | LC 2927. Distribute Candies Among Children II | Stars-and-bars base count, corrected via inclusion-exclusion for the per-child upper limit |
| 3 | LC 2850. Count Anagrams | Multinomial coefficient per word (`n! / product of freq!`), combined across a sentence |
| 4 | LC 1359. Count All Valid Pickup and Delivery Options | Recurrence from counting insertion choices at each step, not direct enumeration |
| — | *Advanced practice pool: LC 2338, LC 2954, CF 1178C/52B/1312D/300C/895D* | Same toolkit (stars-and-bars, inclusion-exclusion, multinomial counting) at increasing difficulty — practice once the above are solid, not separate concepts |

---

## Pattern 5: Geometric Primitives — Lines & Collinearity

**Identify:** The problem asks whether points are collinear, what line(s) they form, or whether two line segments intersect. The core translation: **slope comparison should almost never be done with division** (floating point precision breaks exact equality checks, and vertical lines cause division by zero) — instead, use the **cross-product / cross-multiplication form** of the slope condition, which stays in exact integer arithmetic.

**The cross-product collinearity test — the foundation:** Three points `(x1,y1)`, `(x2,y2)`, `(x3,y3)` are collinear exactly when `(y2-y1)*(x3-x2) == (y3-y2)*(x2-x1)` — the cross-multiplied form of "slope between points 1,2 equals slope between points 2,3," with no division anywhere. This single formula is the entire content of **LC 1232 (Check if It Is a Straight Line)**: verify every point against the first two using this test.

**LC 149 (Max Points on a Line) — the anchor for the harder version:** For every point, compute its slope (as a normalized, reduced fraction using `gcd` — reusing Pattern 1's GCD directly — to avoid floating-point slope comparison entirely) to every other point, and hash-count how many points share each slope relative to the current point. The maximum count across all points and all slopes (plus 1, for the anchor point itself) is the answer. Handling the vertical-line case (`dx = 0`) and duplicate points explicitly are the two edge cases that break naive implementations.

**LC 2280 (Minimum Lines to Represent a Line Chart) — reduced-fraction slopes again, applied to grouping consecutive segments:** Two consecutive segments belong to the same line iff their reduced-fraction slopes are identical — same GCD-normalization trick as LC 149, just compared consecutively rather than all-pairs.

**GFG: Two Line Segments Intersect — the orientation test:** Two segments `AB` and `CD` intersect if and only if the orientations of `(A,B,C)` and `(A,B,D)` differ, AND the orientations of `(C,D,A)` and `(C,D,B)` differ (the "general case"), where orientation is computed via the same cross-product sign used in the collinearity test above — plus special-case handling when any three points are exactly collinear (checking if one point lies within the bounding box of the segment). This orientation-sign test is the standalone geometric primitive that both this problem and LC 335 (below) build on.

**LC 335 (Self Crossing) — the orientation test applied to a moving path, case-split by pattern:** As a path is drawn segment by segment, it self-intersects only in a small number of *relative* geometric configurations (current segment crosses the segment 3 steps back, 4 steps back, or 5 steps back, each with a specific inequality condition derivable from the orientation/overlap logic above). Rather than checking every past segment against the current one (`O(n²)`), the key realization is that a crossing can only occur in these few fixed relative positions, reducing the check to O(1) per step.

**LC 3102 (Minimize Manhattan Distances) — the rotation trick, not a primitive of its own but a transformation worth naming:** Manhattan distance `|x1-x2| + |y1-y2|` becomes Chebyshev distance `max(|u1-u2|, |v1-v2|)` under the transform `u = x+y, v = x-y`. This turns an axis-aligned Manhattan-distance optimization into a Chebyshev one, which is often easier to reason about (Chebyshev distance is just the max coordinate difference along two independent axes) — a genuinely reusable coordinate-transform idea whenever Manhattan distance is the metric in play.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 1232. Check if It Is a Straight Line | Cross-multiplied collinearity test — no division, no floating point |
| 2 | LC 149. Max Points on a Line | Reduced-fraction slope (via GCD) as a hashmap key, per-anchor-point counting |
| 3 | LC 2280. Minimum Lines to Represent a Line Chart | Same reduced-fraction slope comparison, applied consecutively |
| 4 | GFG: Check if Two Line Segments Intersect | Orientation-sign test (cross product) — the general primitive, with collinear-overlap special case |
| 5 | LC 335. Self Crossing | Orientation logic reduced to a handful of fixed relative-position cases — O(1) per step, not O(n²) |
| 6 | LC 3102. Minimize Manhattan Distances | Rotate coordinates `(x+y, x-y)` — Manhattan becomes Chebyshev distance |

---

## Pattern 6: Geometric Primitives — Area & Shape Validity

**Identify:** The problem asks you to compute an area, or verify that a set of points forms a specific valid shape (square, valid rectangle configuration). The core tools: the **shoelace formula** for polygon area from vertex coordinates, and **squared-distance comparisons** (never take an actual square root, to stay in exact integer arithmetic) for shape validation.

**LC 812 (Largest Triangle Area) — the shoelace formula, the anchor:** For three points, area `= 0.5 * |x1(y2-y3) + x2(y3-y1) + x3(y1-y2)|` — the 2D cross-product-based area formula, generalizable to any polygon. With only a handful of points, checking every triplet directly (`O(n³)`) is acceptable given the problem's small constraints; the formula itself, not the enumeration, is the transferable content.

**LC 593 (Valid Square) — squared distances, never actual distances:** Compute all 6 pairwise squared distances among the 4 points. A valid square has exactly two distinct values among these six: four equal "side" distances and two equal (larger) "diagonal" distances, with the diagonal value equal to exactly twice the side value (Pythagorean relationship for a square's diagonal) — and critically, none of the distances can be zero (which would mean two points coincide). Using squared distances throughout avoids any floating-point square root entirely.

**LC 836 (Rectangle Overlap) — decompose to 1D interval overlap on each axis independently:** Two axis-aligned rectangles overlap if and only if their x-projections overlap **and** their y-projections overlap — reducing a 2D overlap question to two independent 1D interval-overlap checks (the exact same "does `[a,b]` overlap `[c,d]`" check from the Intervals sheet, applied twice). Recognizing that 2D axis-aligned overlap decomposes into independent per-axis 1D checks is the transferable insight, and it recurs in every axis-aligned rectangle problem in this pattern.

**LC 223 (Rectangle Area) — the same overlap decomposition, feeding an inclusion-exclusion area formula:** Total covered area = area of rectangle A + area of rectangle B − area of their overlap (computed via LC 836's per-axis decomposition, clamped to zero if there's no overlap). Direct inclusion-exclusion once the overlap region itself is known.

**LC 939 / LC 963 (Minimum Area Rectangle I / II) — points-as-a-hashset, diagonal-driven search:** For axis-aligned rectangles (LC 939), put all points in a hashset; for every pair of points that could be a rectangle's diagonal (sharing neither x nor y coordinate), check whether the other two implied corners also exist in the hashset — an `O(n²)` pairwise check with `O(1)` lookups. For arbitrary-rotation rectangles (LC 963), the diagonal test changes: two pairs of points form a valid rectangle's diagonals iff they share the same midpoint **and** the same diagonal length (a rectangle's diagonals always bisect each other and are equal in length) — group all point-pairs by `(midpoint, diagonal-length-squared)` and look for groups with 2+ pairs.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 812. Largest Triangle Area | Shoelace formula — signed area from vertex coordinates |
| 2 | LC 593. Valid Square | Squared pairwise distances — exactly 2 distinct values, diagonal² = 2×side², none zero |
| 3 | LC 836. Rectangle Overlap | 2D overlap decomposes into two independent 1D interval-overlap checks |
| 4 | LC 223. Rectangle Area | Inclusion-exclusion: areaA + areaB − overlap (from LC 836's decomposition) |
| 5 | LC 939. Minimum Area Rectangle | Hashset of points + diagonal-pair check for axis-aligned rectangles |
| 6 | LC 963. Minimum Area Rectangle II | Group diagonal pairs by (midpoint, length²) — handles arbitrary rotation |

---

## Pattern 7: Circle Geometry & Candidate Enumeration

**Identify:** Problems involving circles almost always reduce to a **squared-distance-to-center comparison against squared-radius** (again, avoiding square roots), or — for the harder "find the best circle" problems — a **candidate-generation** trick where the optimal circle's center is provably constrained to a small, enumerable set of positions rather than a continuous search space.

**LC 1828 (Queries on Number of Points Inside a Circle) — the direct application:** For each query circle, count points where `(px-cx)² + (py-cy)² <= r²` — squared-distance comparison, no square root needed, checked directly per query since constraints are small.

**LC 2249 (Count Lattice Points Inside a Circle) — the same check, but avoiding double-counting across overlapping circles:** Rather than checking every integer point against every circle (which risks counting a point multiple times if circles overlap), use a hashset of `(x,y)` pairs found to be inside *any* circle, scanning each circle's bounding box and adding qualifying points to the set — the hashset naturally deduplicates points covered by multiple circles.

**LC 1330 (Circle and Rectangle Overlapping) — clamp-then-check, the key trick:** To check if a circle overlaps an axis-aligned rectangle, find the point on the rectangle **closest to the circle's center** by clamping the center's coordinates independently to the rectangle's x-range and y-range, then check if that closest point is within the circle's radius. This clamping trick — "find the nearest point on a bounded region by clamping each coordinate independently" — is a genuinely reusable geometric primitive beyond just this one problem.

**LC 1453 (Maximum Number of Darts Inside of a Circular Dartboard) — the anchor, candidate-generation as the actual insight:** A continuous search over all possible circle-center positions is infeasible, but the *optimal* circle (the one containing the most darts) can always be shifted, without losing any contained darts, until at least two darts lie exactly on its boundary — so the optimal center must be one of the (at most two) centers equidistant from some pair of darts at exactly radius `r`. This collapses an infinite search space to `O(n²)` candidate centers (one or two per pair of darts), each checked in `O(n)` against every dart — the technique of "the optimal continuous parameter can be proven to coincide with a boundary/tangency condition, collapsing the search to a finite candidate set" is the single most valuable idea in this entire pattern, and it recurs constantly in harder geometry/optimization problems beyond this specific one.

**LC 478 (Generate Random Point in a Circle) — rejection sampling vs. the correct direct method:** Naively picking a random angle and a random radius in `[0, R]` produces a *biased* distribution (points cluster near the center, since area grows with `r²` but this sampling is uniform in `r`, not `r²`). The correct fix: sample the radius as `R * sqrt(random())`, not `R * random()` — the square root compensates for the area-vs-radius relationship, making point density uniform per unit *area* rather than per unit *radius*. This is worth knowing as the canonical "uniform-looking sampling that's secretly biased" trap.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 1828. Queries on Number of Points Inside a Circle | Direct squared-distance-vs-squared-radius check per query |
| 2 | LC 2249. Count Lattice Points Inside a Circle | Same check + hashset to dedupe points covered by overlapping circles |
| 3 | LC 1330. Circle and Rectangle Overlapping | Clamp circle's center to the rectangle's bounds — check the clamped point's distance |
| 4 | LC 1453. Maximum Number of Darts Inside of a Circular Dartboard | Candidate-generation — optimal circle's center must pass through some pair of darts, collapsing infinite search to O(n²) candidates |
| 5 | LC 478. Generate Random Point in a Circle | Sample radius as `R*sqrt(random())`, not `R*random()` — corrects for area-vs-radius bias |

---

## Pattern 8: Path Simulation Geometry

**Identify:** A grab-bag of problems where a geometric rule (light reflecting, a path potentially crossing itself, a circle being sliced) must be simulated or reasoned about directly — no single reusable formula ties them together, but each teaches a standalone geometric-simulation trick worth having.

**LC 2481 (Minimum Cuts to Divide a Circle) — a pure parity/counting insight, no simulation needed:** `n = 1` needs 0 cuts (already one piece); otherwise, an odd `n` needs `n` cuts (each cut must pass through the center to divide evenly, and with odd count, cuts can't be paired as diameters), while an even `n` only needs `n/2` cuts (each cut is a full diameter, dividing into 2 pieces per cut). The entire "geometry" here is a two-case arithmetic check, worth including specifically as a contrast to LC 1453 — not every circle problem needs candidate enumeration or distance formulas.

**LC 858 (Mirror Reflection) — LCM-based reflection unfolding:** Rather than simulating bounces one reflection at a time (which could run indefinitely for irrational-looking paths), "unfold" the room by reflecting it repeatedly, turning the zigzag path into a straight line through a grid of mirrored rooms. The ray reaches a corner exactly at `lcm(p, q)`, and which corner depends on the parity of `lcm(p,q)/p` and `lcm(p,q)/q` — a closed-form answer via LCM (Pattern 1's building block again) rather than any actual simulation.

**LC 335 (Self Crossing)** — already covered in Pattern 5, listed here only as a cross-reference since it's simultaneously a path-simulation problem and a collinearity/orientation problem; its primary written home is Pattern 5.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 2481. Minimum Cuts to Divide a Circle | Pure parity case-split — no candidate search or distance formula needed |
| 2 | LC 858. Mirror Reflection | Unfold reflections into a straight line via LCM — closed-form corner + parity, no bounce-by-bounce simulation |
| — | LC 335. Self Crossing *(cross-ref: Pattern 5)* | Primary treatment lives with the orientation-test primitive |

---

## Final Summary

| Pattern | Problems (new) | Core Mechanism |
|---|---|---|
| Number Theory Fundamentals | 5 (incl. 2 theory) | GCD/LCM, sieve, modular exponentiation — replace brute-force checks with identities |
| Digit & Base Manipulation | 5 | Digit-by-digit peel with careful overflow/base-zero edge cases |
| Big-Number String Arithmetic | 2 | Simulate arithmetic on digit strings — position-indexed contribution or GCD-of-lengths |
| Combinatorics — Counting Without Enumeration | 4 (+1 theory, +practice pool) | Stars-and-bars, inclusion-exclusion, multinomial counting — count without generating |
| Geometric Primitives — Lines & Collinearity | 6 | Cross-product replaces slope/division; GCD-reduced fractions for exact slope comparison |
| Geometric Primitives — Area & Shape Validity | 6 | Shoelace formula, squared distances, per-axis interval decomposition |
| Circle Geometry & Candidate Enumeration | 5 | Squared-distance checks; candidate-generation collapses continuous search to finite set |
| Path Simulation Geometry | 2 (+1 cross-ref) | Parity arithmetic or unfolding-via-LCM replaces direct simulation |
| **Total** | **35 new + 10 cross-referenced + 1 theory-only practice pool** | |

---

## How to Use This Sheet

**Pattern 1 is the quiet foundation for nearly everything else in this sheet.** GCD reappears directly inside Pattern 5 (slope reduction) and Pattern 8 (mirror unfolding), and modular exponentiation reappears inside Pattern 4's nCr-mod-p machinery — get the Euclidean algorithm and fast exponentiation completely automatic before treating this as "done," since half the later patterns silently assume them.

**The "no floating point, no square roots" discipline threading through Patterns 5–7 is the single most important interview signal in this entire sheet.** Every geometry problem here has a floating-point-naive version (compute actual slopes, actual distances, actual angles) and an exact-integer-arithmetic version (cross products, squared distances, reduced fractions). Interviewers specifically probe for the second version because the first one silently produces wrong answers on edge cases (vertical lines, very close points) that only show up on hidden test cases, not the example given.

**Pattern 7's LC 1453 deserves outsized attention relative to its single-problem footprint.** The "optimal continuous parameter must coincide with a boundary/tangency condition, collapsing to finite candidates" idea is a genuinely advanced optimization technique that generalizes far beyond circles — it's the same class of reasoning behind "the optimal line in a 2D optimization problem must pass through two of the given points," which shows up in competitive programming well beyond this specific problem.

**Pattern 4's practice pool is intentionally left unstructured.** Once the nCr-mod-p building block and the stars-and-bars/inclusion-exclusion instinct are solid from the four fully-treated problems, the harder LeetCode and Codeforces problems in the pool are exactly the kind of "go apply what you know to something unfamiliar" practice your Strategy notes ("How to Handle Problems You've Never Seen") already describe — no additional scaffolding would add value beyond what those two notes already provide.
