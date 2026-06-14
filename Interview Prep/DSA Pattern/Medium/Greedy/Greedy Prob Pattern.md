
# Stack DSA Patterns

*"Problems are infinite, but patterns are finite!"*

---

## The Core Mental Model — Before Any Pattern

A stack enforces **LIFO order** — the last thing pushed is the first thing processed. Every stack pattern is exploiting this in a specific way:

- **Bracket/nesting problems:** The stack tracks unmatched openers — when a closer arrives, it pairs with the most recent unmatched opener.
- **Simulation problems:** The stack holds an intermediate state that gets modified as new elements arrive — like a running computation or a history of events.
- **Monotonic stack:** The stack maintains a sequence that is strictly increasing or decreasing. When a new element breaks the monotone property, you pop — and that moment of popping is where the answer lives.

**The monotonic stack insight — say this out loud before every monotonic problem:**
> *"I pop when the invariant is violated. The element being popped now has the current element as its answer."*

Everything in Patterns 3 and 4 flows from this single sentence.

---

## Pattern Identification Quick Reference

| Pattern | Trigger |
|---|---|
| Parenthesis & Bracket Problems | "valid parentheses", "minimum removals", "longest valid", "balance brackets" |
| Stack Simulation & Design | "calculator", "evaluate expression", "decode", "min in O(1)", "most frequent", "simulate process step by step" |
| Monotonic Stack — NGE & Spans | "next greater", "next smaller", "previous greater", "how many days until warmer", "stock span", "visible people" |
| Monotonic Stack — Area, Sums & Greedy | "largest rectangle", "trapping rain water", "sum of subarray min/max", "lexicographically smallest", "remove k digits" |

---

## Pattern 1: Parenthesis & Bracket Problems

**Identify:** The problem involves bracket characters `()[]{}` and asks about validity, balance, minimum removals, or the longest valid subsequence. The stack models **unmatched openers** — push when you see an opener, pop when you see a matching closer.

**The key extension — tracking indices instead of characters:** Once you store the *index* of each unmatched bracket on the stack rather than the character itself, problems like Longest Valid Parentheses become straightforward — the length of a valid segment is computed from the gap between indices. This single insight unlocks the harder problems in this pattern.

**Valid Parenthesis String (LC 678):** The `*` wildcard introduces ambiguity. The clean solution maintains two stacks — one for `(` indices, one for `*` indices. After the main pass, greedily pair leftover `(` with `*` that appear after them. If any `(` has no available `*` to its right, return false.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 20. Valid Parentheses | Base template — push openers, pop and match on closers |
| 2 | LC 1614. Maximum Nesting Depth of Parentheses | Track current depth via push/pop counter — running max |
| 3 | LC 1021. Remove Outermost Parentheses | Depth tracking — strip the outermost layer |
| 4 | LC 678. Valid Parenthesis String | Two stacks — one for `(` indices, one for `*` indices; greedy matching |
| 5 | LC 32. Longest Valid Parentheses | Store indices on stack — segment length = gap between unmatched bracket indices |
| 6 | LC 1249. Minimum Remove to Make Valid Parentheses | Two-pass — collect unmatched indices, remove them |
| 7 | LC 921. Minimum Add to Make Parentheses Valid | Count unmatched open and close — no stack needed, pure counter |

---

## Pattern 2: Stack Simulation & Design

**Identify:** The problem requires simulating a step-by-step process where each new element either modifies recent history or builds on it. The stack holds intermediate computation state. Sub-categories: expression evaluation (calculator problems), nested structure decoding, augmented stacks (support extra queries in O(1)), and event-driven simulation.

**Expression Evaluation — the operator precedence trick:** For Basic Calculator II (+ − × ÷), maintain a stack of *signed terms*. When you see `+` or `−`, push the signed number. When you see `×` or `÷`, pop the top, apply the operation, push the result back. Final answer = sum of everything on the stack. This handles precedence without building an AST.

**Augmented Stacks — the pairing trick:** For Min Stack and Maximum Frequency Stack, the insight is the same: each stack entry stores not just the value but additional metadata — `(value, current_min)` for Min Stack, or the frequency/group structure for FreqStack. The extra metadata never requires recomputing from scratch.

**Note on LC 394 Decode String:** This problem appeared in the Recursion & Backtracking sheet as a Pure Recursion problem. It fits here equally well as a stack simulation (maintain a stack of `(current_string, repeat_count)` pairs). If you solved it recursively there, understand the iterative stack solution here — interviewers sometimes ask for both.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 155. Min Stack | Store `(val, current_min)` pairs — O(1) minimum without scanning |
| 2 | LC 716. Max Stack | Same principle as Min Stack — O(1) maximum |
| 3 | LC 150. Evaluate Reverse Polish Notation | Postfix evaluation — operands push, operators pop two and push result |
| 4 | LC 227. Basic Calculator II | Signed-term stack — handle `×` and `÷` by popping and repushing; sum at end |
| 5 | LC 224. Basic Calculator | Full expression with parentheses — push sign and running result at `(`, pop and combine at `)` |
| 6 | LC 394. Decode String | Stack of `(built_string, repeat_count)` — push on `[`, decode and combine on `]` |
| 7 | LC 636. Exclusive Time of Functions | Stack of active function IDs — compute intervals on push/pop events |
| 8 | LC 735. Asteroid Collision | Stack of surviving asteroids — pop on collision, push if no collision |
| 9 | LC 71. Simplify Path | Split on `/`, stack of directory names — handle `.` and `..` |
| 10 | LC 895. Maximum Frequency Stack | FreqStack design — `freq` map + `group` map (frequency → stack of elements at that freq) |
| 11 | LC 682. Baseball Game | Running score simulation — stack of valid scores, handle `C`, `D`, `+` |
| 12 | LC 1472. Design Browser History | Stack-based forward/back navigation — or doubly linked list; stack insight is the lesson |

---

## Pattern 3: Monotonic Stack — Next Greater / Smaller & Spans

**Identify:** Problems asking for the **next or previous element** that is greater or smaller than the current one, or asking how far back/forward until a dominant element appears. The stack maintains a **monotone sequence** — strictly decreasing for Next Greater, strictly increasing for Next Smaller. The answer for a popped element is always the element that caused the pop.

**The universal template — memorize this:**
```
stack = []   # stores indices
for i in range(len(arr)):
    while stack and arr[stack[-1]] < arr[i]:   # condition depends on problem
        idx = stack.pop()
        answer[idx] = i   # or arr[i], or i - idx, depending on what's asked
    stack.append(i)
# anything remaining in stack has no next greater element
```

**Circular array (LC 503):** Run the same loop twice over the array using `i % n`. Do not physically duplicate the array — just let the index wrap. The stack still stores original indices (0 to n-1).

**Online Stock Span (LC 901):** The same NGE logic applied online (one element at a time). The span of the current price = how many consecutive previous days had price ≤ today. The monotonic stack maintains previous prices that haven't been "dominated" yet.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 496. Next Greater Element I | Base NGE template — stack of unresolved elements, answer on pop |
| 2 | LC 739. Daily Temperatures | Same NGE template — answer is `i - popped_index` (days to wait) |
| 3 | LC 503. Next Greater Element II | Circular array — run loop twice, index with `i % n` |
| 4 | LC 901. Online Stock Span | NGE in reverse (previous greater) — online version; span = distance to previous greater |
| 5 | LC 1019. Next Greater Node in Linked List | NGE applied to linked list — convert to array or traverse with index tracking |
| 6 | LC 1944. Number of Visible People in a Queue | Monotonic decreasing stack — count pops + final taller person |
| 7 | LC 42. Trapping Rain Water | Two approaches: left/right max arrays OR monotonic stack — learn both; stack approach: water trapped when popping = `(min(left_bar, right_bar) - bottom) * width` |
| 8 | LC 2866. Beautiful Towers II | Previous greater and next greater both needed — contribution from each tower as the peak |

---

## Pattern 4: Monotonic Stack — Area, Sums & Lexicographic Greedy

**Identify:** These are harder applications of the monotonic stack where the answer is not just "index of next greater" but requires combining multiple values at the moment of pop — computing areas, accumulating contribution sums, or making greedy character removal decisions. The stack invariant is the same; the computation at the pop step is more involved.

**Largest Rectangle in Histogram (LC 84) — the anchor problem:** Every element on the stack is a "potential left boundary." When you pop because a shorter bar arrives, the popped bar extends rightward to the current index and leftward to the new stack top. Area = `height[popped] × (right - left - 1)`. This exact logic powers LC 85 (Maximal Rectangle) row by row — and that relationship makes LC 84 non-optional.

**Contribution counting (LC 907, LC 2104):** Instead of "what is the NGE of this element," ask "for how many subarrays is this element the minimum?" The answer is `left_count × right_count` where left and right counts come from the previous smaller and next smaller element positions. This "contribution" framing recurs in many hard problems.

**Lexicographic greedy (LC 402, LC 316, LC 321):** Maintain a monotonically increasing stack of characters/digits to build the lexicographically smallest result. When the current character is smaller than the stack top AND you still have budget to remove, pop. The stack invariant here is about character order, not numerical order — same mechanism, different domain.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 84. Largest Rectangle in Histogram | Pop when shorter bar arrives — area = `height × (right - left - 1)` |
| 2 | LC 85. Maximal Rectangle | Apply LC 84 logic row by row — build histogram height array per row |
| 3 | LC 907. Sum of Subarray Minimums | Contribution counting — `left_count × right_count × value` for each element as minimum |
| 4 | LC 2104. Sum of Subarray Ranges | Apply LC 907 twice — once for minimums, once for maximums; answer = sum_max − sum_min |
| 5 | LC 402. Remove K Digits | Maintain increasing stack — pop larger digit when smaller arrives and k > 0 |
| 6 | LC 316. Remove Duplicate Letters | Same greedy — maintain increasing stack, skip already-included characters, only pop if character appears later |
| 7 | LC 456. 132 Pattern | Decreasing stack tracking the "3" — maintain running minimum as "1"; pop when "2" candidate found |
| 8 | LC 1793. Maximum Score of a Good Subarray | Monotonic stack or two-pointer — find the largest rectangle containing index k |
| 9 | LC 321. Create Maximum Number | Lexicographic greedy on two arrays — hardest in this tier; do only after LC 402 and LC 316 are solid |

---

## Appendix: Problems Removed / Reassigned

| Problem | Reason |
|---|---|
| LC 57. Insert Interval | Not a stack problem — pure interval merging; belongs in Array/Intervals pattern |
| LC 394. Decode String | Kept in Pattern 2 (iterative stack) but primary home is Recursion & Backtracking sheet |
| LC 174. Dungeon Game | Appeared in some aggregations — belongs in DP on Grid sheet |

---

## Final Summary

| Pattern | Problems | Core Mechanism |
|---|---|---|
| Parenthesis & Bracket Problems | 7 | Stack of unmatched openers; index-storing for length problems |
| Stack Simulation & Design | 12 | Intermediate state on stack; augmented metadata; expression evaluation |
| Monotonic Stack — NGE & Spans | 8 | Pop when invariant violated; answer at pop is the causing element |
| Monotonic Stack — Area, Sums & Greedy | 9 | Pop step computes area/contribution/lexicographic decision |
| **Total** | **~36 problems** | |

---

## How to Use This Sheet

**Pattern 1 before Pattern 2.** The bracket problems establish the mental model of "stack tracks unresolved state" in its cleanest form. That intuition transfers directly into expression evaluation.

**Pattern 3 is the gateway to Pattern 4.** Do not attempt LC 84, LC 907, or LC 402 without NGE completely automatic. The mechanism is identical — the pop-step computation is just more complex. If the pop step confuses you, you have not internalized Pattern 3 yet.

**LC 84 is the single most important problem in Pattern 4.** It appears standalone and as a subroutine inside LC 85. Master it first. Every other Pattern 4 problem is either a variation of histogram area or a variation of contribution counting.

**Lexicographic greedy (LC 402 → LC 316 → LC 321) must be done in that order.** LC 321 is genuinely hard and the solution is non-obvious without the prior two building the intuition.

---

This is the complete Stack pattern sheet. The key structural decisions to flag:

**Why four patterns instead of two?** Fraz and Algomaster both used two buckets (Standard + Monotonic), but that hides a critical learning dependency. The NGE template and the area/contribution/greedy applications require different mental models at the pop step — conflating them causes learners to memorize LC 84 without understanding why it works. Separating them makes the progression explicit.

**Why split Monotonic Stack at all if the mechanism is the same?** Because the trigger recognition differs. "Next greater element" is immediately obvious as a monotonic stack problem. "Sum of subarray minimums" is not — you need the contribution counting insight first. Grouping them by *difficulty of recognition*, not just mechanism, is what makes this sheet useful for unseen problems.

**LC 57 removal from Fraz's stack section.** This was a clear misclassification. Keeping it here would pollute the pattern with noise and weaken your ability to identify what actually belongs here.