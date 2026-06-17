# How to Handle Problems You've Never Seen

The previous chapter focused on problems where at least one signal points to a known pattern. But some problems do not give you that clue. The input type, constraints, and wording may not clearly map to anything familiar.

This chapter covers what to do in that situation. The goal is to keep making progress when the starting point is unclear, without depending on recognizing the pattern upfront.

**Why Unfamiliar Problems Are Often Familiar Underneath**

A problem can feel new while its underlying structure is one a candidate has already practiced. Different wording, an unfamiliar setting, and an unusual phrasing of the constraints can hide a structure that maps cleanly to a known pattern.

Consider this problem:

<aside>
💬

You are a gardener with a row of flower pots. Each pot can hold one flower. You want to plant flowers such that no two flowers are adjacent.

Given an array where `1` means a flower is already planted and `0` means the pot is empty, and an integer `n`, determine if you can plant `n` new flowers without violating the rule.

</aside>

At first, this reads like a domain-specific scenario.

If you strip away the story:

- You have an array of 0s and 1s
- There is a constraint on adjacent elements
- You need to make placement decisions
- The goal is to check feasibility

What remains is a greedy array problem with adjacency constraints.

# **What Changes and What Stays the Same**

Problem statements often change:

- The **story** (gardener, robots, meetings, tasks)
- The **terminology** (pots, nodes, intervals, items)
- The **constraints phrased in words**

But what usually stays the same:

- The **data structure** (array, graph, tree)
- The **type of operation** (search, traversal, optimization)
- The **core constraint** (adjacency, ordering, capacity)
- The **goal** (count, optimize, validate)

**Technique 1: Work From Examples**

When the algorithm is not obvious, start by solving a concrete example by hand without trying to invent a general algorithm first. Walking through the example step by step exposes the implicit decisions, and those decisions are usually what the algorithm needs to encode.

# **How to Do It**

- Start with the smallest example.
- Solve it manually without thinking about code.
- Write down each step and why you chose it.
- Then look for patterns in those decisions.

# **Example: Trapping Rain Water**

<aside>
💬

Given an array of non-negative integers representing the height of bars, compute how much water can be trapped between them after rain.

</aside>

```
heights = [0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1]
```

Set the algorithm aside and reason about each position individually: how much water can sit at this index, given the heights around it?

```
Position 0 (height 0): No bar to the left. No water.
Position 1 (height 1): Tallest left = 0, tallest right = 3. Water = min(0,3) - 1 = no water (left bound is 0).
Position 2 (height 0): Tallest left = 1, tallest right = 3. Water = min(1,3) - 0 = 1.
Position 3 (height 2): Tallest left = 1, tallest right = 3. Water = min(1,3) - 2 = no water (bar is taller than left bound).
Position 4 (height 1): Tallest left = 2, tallest right = 3. Water = min(2,3) - 1 = 1.
Position 5 (height 0): Tallest left = 2, tallest right = 3. Water = min(2,3) - 0 = 2.
Position 6 (height 1): Tallest left = 2, tallest right = 3. Water = min(2,3) - 1 = 1.
Position 7 (height 3): Tallest left = 2, tallest right = 2. Bar is taller than both bounds, no water.
Position 8 (height 2): Tallest left = 3, tallest right = 2. Water = min(3,2) - 2 = no water.
Position 9 (height 1): Tallest left = 3, tallest right = 2. Water = min(3,2) - 1 = 1.
Position 10 (height 2): Tallest left = 3, tallest right = 2. Water = min(3,2) - 2 = no water.
Position 11 (height 1): No bar to the right. No water.

Total water trapped: 1 + 1 + 2 + 1 + 1 = 6.
```

A rule emerges from working the positions one by one: the water at each index equals the shorter of the tallest bars on its left and right, minus the bar's own height, clamped at zero. Recognizing the problem as a known "trapping rain water" question is not required for the rule to surface; it falls out of the manual trace itself.

![image.png](How%20to%20Handle%20Problems%20You've%20Never%20Seen/image.png)

---

# **Technique 2: Simplify the Problem**

A problem that looks complex at full size often becomes tractable when reduced to its smallest non-trivial cases. Working those cases out by hand surfaces the structure of the answer, which then generalizes back to the original input.

# **The Progression**

Work through increasing input sizes and observe what changes:

- **n = 0 (empty input):** What should the answer be? This gives you a base case.
- **n = 1:** What happens with the smallest valid input?
- **n = 2:** Do elements start interacting?
- **n = 3:** Does a pattern begin to appear?
- **General n:** Can the answer be expressed using smaller instances of the same problem?

# **Example: Decode Ways**

<aside>
💬

A message containing letters A-Z is encoded: 'A' = 1, 'B' = 2, ..., 'Z' = 26. Given a string of digits, return the number of ways to decode it.

</aside>

The recurrence becomes visible once the smallest cases are written out:

```
s = ""       → 1 way (empty string decodes one way: the empty decoding)
s = "1"      → 1 way: "A"
s = "12"     → 2 ways: "AB" (1,2) or "L" (12)
s = "123"    → 3 ways: "ABC" (1,2,3), "LC" (12,3), "AW" (1,23)
s = "1234"   → 3 ways: ways("234") + ways("34") = 2 + 1 = 3
```

Working through "1234" gives the rule: at each position, the next character can be taken alone (decode the rest) or combined with the following character (if the pair is between 10 and 26, decode what follows). The number of ways for "1234" is the number of ways for "234" plus the number of ways for "34".

That is a recurrence: `ways(i) = ways(i+1) + ways(i+2)` whenever the two-digit number is valid. Walking through n = 0 through n = 4 by hand turns an opaque counting problem into a familiar dynamic programming structure analogous to climbing stairs.

# **Why Simplification Works**

Reducing input size strips away the bookkeeping that hides the underlying rule. With the bookkeeping gone, the recurrence or invariant becomes easier to spot, and the general solution then reads as an extension of the smaller cases rather than a fresh insight.

---

# **Technique 3: Draw It Out**

Problems involving position, ordering, movement, overlap, or connections often have a structure that is much easier to see in a picture than in prose. A quick sketch tends to converge on the algorithm faster than further reading of the statement.

# **What to Draw**

| **Problem Type** | **What to Visualize** |
| --- | --- |
| Array problems | Draw the array with indices, annotate with arrows showing pointer movement |
| Tree problems | Draw the actual tree, label nodes, trace the traversal path |
| Graph problems | Draw nodes and edges, mark visited nodes, show traversal order |
| Interval problems | Draw intervals on a number line, show overlaps |
| Matrix problems | Draw the grid, shade cells, show movement directions |
| Linked list problems | Draw nodes with arrows, show how pointers change |

## **Example: Merge Intervals**

<aside>
💬

Given a collection of intervals, merge all overlapping intervals.

</aside>

```
intervals = [[1,3], [2,6], [8,10], [15,18]]
```

Draw these on a number line:

```
1---3
  2------6
              8--10
                       15--18
```

The overlap reads off the picture directly. `[1,3]` and `[2,6]` share a region, so they merge into `[1,6]`. The other two intervals sit on their own, giving the final result `[[1,6], [8,10], [15,18]]`.

Once the picture is clear, the algorithm follows:

- Sort intervals by start time
- Scan from left to right
- If the current interval overlaps the previous one, merge them
- Otherwise, start a new interval group

The same technique helps with debugging. Sketching the state at each step and comparing it against what the code actually produces tends to surface off-by-one and pointer mistakes faster than re-reading the code.

---

# **Technique 4: Start With Brute Force**

This idea comes up often, but it is especially useful when nothing else is working. A brute force approach may not be efficient, but it gives you a clear and correct starting point.

# **The Brute Force Mindset**

A simple way to begin is to ask: "If I had unlimited time and computing power, how would I solve this?"

- For a search problem: check every possible option.
- For an optimization problem: try every combination and pick the best.
- For a counting problem: enumerate every possibility and count.

This gives you a baseline solution you can build on.

# **Example: Longest Increasing Subsequence**

<aside>
💬

Given an integer array, find the length of the longest strictly increasing subsequence.

</aside>

**Brute force approach:**

- Generate all possible subsequences
- Check which ones are increasing
- Track the maximum length

There are **2ⁿ subsequences**, so the brute force runs in **O(2ⁿ)** time. It is not practical for realistic input sizes, but it is correct and easy to reason about.

# **Improving the Solution**

While working through brute force, a pattern starts to appear.

You keep asking:

- What is the longest increasing subsequence ending at index `i`?

And you notice that this question depends on answers to the same question at earlier indices.

This is a sign of **overlapping subproblems**, which leads to dynamic programming.

# **Dynamic Programming Approach**

Define:

- `dp[i] = length of the longest increasing subsequence ending at index i`

For each `i`, check all `j < i`:

- If `nums[j] < nums[i]`, then `dp[i] = max(dp[i], dp[j] + 1)`

This reduces the complexity to **O(n²)**.

# **Further Optimization**

The complexity drops further to **O(n log n)** using a patience-sorting style approach: maintain an auxiliary array where the position `k` stores the smallest tail value seen so far for any increasing subsequence of length `k+1`. For each new number, binary-search for the leftmost position in that array whose value is greater than or equal to the new number and replace it (or append if the number is larger than all values seen). The length of the auxiliary array at the end is the answer.

![image.png](How%20to%20Handle%20Problems%20You've%20Never%20Seen/image%201.png)

A brute-force-first progression is useful even when the optimal solution does not surface in time. It produces a correct baseline, makes the bottleneck explicit, and turns optimization into a series of incremental changes rather than a single leap from blank page to optimal solution.

---

# **Technique 5: Break Into Subproblems**

Many large problems decompose cleanly into smaller, individually-solvable pieces. Solving the pieces and composing them is usually easier than solving the whole problem at once.

# **How to Decompose**

When you feel stuck, ask:

- Can this be split into smaller, independent parts?
- Can I solve a simpler version first and then extend it?
- Is there a preprocessing step that simplifies the main task?
- Can I transform this into something I already know how to solve?

# **Example: Find All Anagrams in a String**

<aside>
💬

Given a string s and a string p, find all start indices of p's anagrams in s.

</aside>

Decomposing this problem produces three familiar pieces:

1. **What is an anagram?** Two strings are anagrams when they have the same character frequencies.
2. **What needs to be found?** Substrings of `s` with the same length as `p` and the same character frequencies.
3. **How is a fixed-length substring checked across `s`?** A sliding window of size `len(p)`.

Combined, the algorithm slides a window of size `len(p)` over `s`, maintains a frequency count of the window, and compares it against the frequency count of `p`. Each piece (frequency counting, sliding window, comparison) is a routine subproblem.

# **Example: Serialize and Deserialize a Binary Tree**

The problem splits cleanly into two routine tree operations:

1. **Serialize:** Convert the tree to a string via preorder traversal, recording each node's value and using a marker (such as `#`) for null children.
2. **Deserialize:** Reconstruct the tree from the preorder sequence by recursively consuming the next token and attaching subtrees in the same order they were written.

Both halves are standard tree traversals. The full problem is the composition of the two.

# **Technique 6: Recognize Disguised Problems**

Plenty of interview problems are familiar patterns wrapped in unfamiliar wording. The underlying data structure, operation, and constraint usually map to something well-known once the scenario is stripped away.

# **Common Disguises**

### **Graph problems disguised as grids**

Any time you have a 2D grid where you move between adjacent cells, it is essentially a graph problem. Each cell is a node, and each valid move is an edge. "Number of islands" is just counting connected components. "Shortest path in a maze" is BFS on a graph.

### **DP problems disguised as counting questions**

When the problem asks for the "number of ways" or "number of distinct results," it often points to dynamic programming.

These usually involve breaking the problem into smaller subproblems and combining results.

### **Tree problems disguised as recursion**

Nested structures often behave like trees. For example, evaluating expressions with parentheses can be seen as traversing an implicit tree.

### **Stack problems disguised as string processing**

Whenever you see nested structures, matching pairs, or processing from inside out, a stack is often involved.

# **Concrete Example: Meeting Rooms II**

<aside>
💬

Given a list of meeting time intervals, find the minimum number of conference rooms required.

</aside>

The problem reads like scheduling, but reframing it as "find the maximum number of meetings overlapping at any point in time" turns it into the classic maximum-overlap problem solvable by a sweep line. Sort start times and end times into separate arrays, sweep through both in order, increment a counter on each start and decrement on each end, and track the maximum value the counter reaches. That maximum is the number of rooms required. The conference-room framing is the disguise; the underlying pattern is sweep line over events.

# **Concrete Example: Rotting Oranges**

<aside>
💬

In a grid, a fresh orange becomes rotten if it is adjacent to a rotten orange (each minute, rotting spreads). Return the minimum number of minutes until no fresh orange remains, or -1 if impossible.

</aside>

The simulation framing hides a shortest-distance question. Each rotten orange spreads exactly one step per minute, which is the definition of multi-source BFS: start the queue seeded with every initially rotten orange, expand in lockstep, and the number of BFS levels until the queue empties is the answer. The orange framing is the disguise; the pattern is multi-source BFS for shortest distance.

---

# **Putting It All Together: A Decision Process for When You Are Stuck**

When pattern matching does not produce a candidate approach, the techniques in this chapter can be applied in order. Each one approaches the problem from a different angle, and going through them sequentially usually surfaces enough structure to start coding.

![image.png](How%20to%20Handle%20Problems%20You've%20Never%20Seen/image%202.png)

The underlying principle is forward progress. Working an example, simplifying the input, drawing the structure, or writing a brute-force solution each yields concrete state that the next step can build on. The optimal solution does not need to appear on the first attempt; moving from the blank page to something tractable is what matters.