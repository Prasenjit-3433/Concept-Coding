Me to you:
    Hi there, I'm working on creating "pattern" in DSA problem. This thing I realized late

    "Problems are infinite, but patterns are finite!", so if we can master the patterns and build ability to
    identify and apply those patterns, we would be to solve more more unseen dsa problems on our own.

    Even after revising Striver DSA sheet 2 times, still I don't feel confident on interview. Whenever new problem
    comes in interview, I totally get stuck. This is happening because I've no structure in learning!
    So this time, I won't repeat this mistake.

    Moreover, Striver SDE Sheet alone is not enough. Lots of pattern, concepts are still missing because many new 
    problems came, but striver did not update his sheet.
    So I collected problems & their patterns from two new great resources: Fraz DSA Sheet (aka Mohammad Fraz) & 
    Algomaster.io (aka. Ashish Pratap Singh), which has updated collect which aligns with current market standard.
    We've to be very careful here about merging these sheets as they might have different pattern naming convension,
    same problem might apear in different pattern category in those two different sheet. There would definitely some
    conflicts!
    So we have analyze the very carefully to come with an uniform pattern list.

    I'm gonna create pattern on every dsa topic, started with Graph.
    
    What would be your job:
    - first look at all the pattern they recognized, then check any pattern is missing
    - look at all the problems they listed, analyze the pattern, concept used in solving the problem, frequency of getting
       asked in interview (if possible)
    - also, look for any problem missing in their list, which is very important, problems based on that pattern often gets 
      asked interview.
    - Then final step is place them all in an organized way:
        - In each pattern, problems should be organized in such a way that it would help me to build mastery on that 
          pattern gradutually, Not like a difficult concept at starting when prerequisites are not covered yet. That's 
          order of problems in each pattern matters a lot on learning!
        - If redundant concept or pattern being in many problems, then only keep only one problem from that concept or 
          pattern. Because there's no point of doing the same pattern again & again, rather we should aim covering
          more new unique pattern or concepts, so that we can reduce the chance of getting "unseen hard problem" in
          in interview.

    Okay, then let's start working!

--------------------------------
You to me:
    Hey! I can see the documents you've shared — the Patterns.md file with your graph DSA pattern list, and the current conversation.md file explaining your goal.
    Let me do a thorough analysis of everything: missing patterns, missing problems, redundancies, and then give you a fully reorganized, interview-optimized version.

    I updated the content of "Graph Prob Patterns.md" with all improvements, have a look!

--------------------------------
Me to you:
    Great job you've done on Graph.
    Do the same quality of analysis work on DP now. Dp is the most difficult topic & most widely asked in FAANG level 
    companies. So we've to prepare it the best possible way!

------------------------------------
You to Me:

    I've reviewed all the files carefully. Here's my understanding of the context:
    From current conversation.md: We already did this exact exercise for Graphs — I analyzed both the Fraz DSA Sheet and Algomaster.io sheet, resolved naming conflicts, filled missing problems, removed redundancies, and produced the final Graph Prob Patterns.md. Now you want the same treatment for DP.
    What I have to work with:
    
    DP problem aggregation.md — raw problems from both Fraz and Algomaster sheets
    DP Prob Pattern.md — currently empty (the file exists but has no content)
    
    So my job is to build the DP Prob Pattern.md from scratch, the same way I built the Graph one — with the same rigor and quality.

    Let me now do the full analysis before writing the final sheet.

    Step 1: Pattern Inventory from Both Sheets
    Fraz's patterns: Linear DP, 2D DP, DP on Grid, Knapsack DP, LIS, LCS, DP on String, Cumulative Sum, Matrix Chain Multiplication, Kadane's Algo, DP with Bitmask, DP on Trees
    Algomaster's patterns: 1-D DP, 0/1 Knapsack, Unbounded Knapsack, LIS, 2D Grid DP, String DP, Tree/Graph DP, Bitmask DP, Digit DP, Probability DP, State Machine DP

    Step 2: Conflict Resolution & Naming
    ConflictResolution"Linear DP" (Fraz) vs "1-D DP" (Algomaster)→ 1-D DP (clearer)"2D DP" (Fraz) vs "2D Grid DP" (Algomaster)→ 2D / Grid DP"Knapsack DP" (Fraz, lumped) vs "0/1 Knapsack" + "Unbounded Knapsack" (Algomaster, split)→ Split them — these are fundamentally different recurrences"DP on String" (Fraz) vs "String DP" (Algomaster)→ String DP"DP on Trees" (Fraz) vs "Tree/Graph DP" (Algomaster)→ Tree / Graph DP"Cumulative Sum" (Fraz, inside DP)→ Belongs inside 2D/Grid DP as a sub-concept, not standalone"Matrix Chain Multiplication" (Fraz)→ Rename to Interval DP — MCM is one instance of a broader pattern (burst balloons, palindrome partition, etc.)"Kadane's Algo" (Fraz)→ Keep as Kadane's / Subarray DP — it IS a distinct pattern frequently testedDigit DP, Probability DP, State Machine DP (Algomaster only)→ Add all three — these are real patterns Fraz missed, frequently asked at FAANG

    Step 3: Missing Patterns (Neither Sheet Has)
    
    DP on Subsequences with Constraint — problems like LC 1048 Longest String Chain, LC 2369 Check if There is a Valid Partition — distinct enough in structure from pure LIS/LCS
    This is actually absorbed into LIS/LCS with state expansion — no separate pattern needed, but flag it in those sections


    Step 4: Final Pattern List (Ordered by Learning Dependency)
    Phase 1 — Foundation
    1. 1-D DP (Linear)
    2. Kadane's / Subarray DP
    3. 2D / Grid DP
    
    Phase 2 — Classical DP
    4. 0/1 Knapsack
    5. Unbounded Knapsack
    6. Longest Increasing Subsequence (LIS)
    7. Longest Common Subsequence (LCS)
    8. String DP
    
    Phase 3 — Advanced Patterns
    9. Interval DP (Matrix Chain Multiplication generalized)
    10. Bitmask DP
    11. Tree / Graph DP
    
    Phase 4 — Specialized Patterns
    12. State Machine DP
    13. Digit DP
    14. Probability DP

    Now here is the complete final sheet and it's saved inside "DP Prob Pattern.md" as expected.

    A few key structural decisions I want to flag for you:
    Why Interval DP, not "Matrix Chain Multiplication"? MCM is just one problem. The real pattern is 
    "optimize over range splits" — burst balloons and strange printer both use the exact same recurrence shape. Naming 
    it by one problem was limiting your pattern recognition.

    Why split Knapsack into 0/1 and Unbounded? The recurrence direction is fundamentally different — 0/1 iterates weight
    backward (to avoid reuse), unbounded iterates forward. Conflating them is one of the most common sources of bugs in 
    interviews.

    Why State Machine DP is its own pattern? All 6 stock problems share the same mental model (finite states + 
    transitions), and that model transfers to non-stock problems like LC 1955. Learning it as a pattern — not as 
    "stock problems" — makes it generalize.

    Digit DP and Probability DP were completely absent from Fraz's sheet. These come up at Google, Meta, and Jane Street
    level interviews. Adding them is not padding — it's filling a real gap.

----------------------------------------------
Me to You:
    Again, Great job you've done on DP.
    This is our work is on Binary Tree & Binary Search Tree / ordered set. Both 2 sheet categorized these 2 topics 
    separately as individual topics.
    As a result, it's likely possible that one guy placed the same prob in "Binary Tree", whereas the other guy might
    have put it in "Binary Search Tree / ordered set".
    
    And as earlier said, we've to be very careful here about merging these sheets as they might have different pattern 
    naming convension, same problem might apear in different pattern category in those two different sheet. There would 
    definitely some conflicts!
    Therefore, we have analyze the very carefully to come with an uniform pattern list.
    So my suggestion would be to go through all patterns, problems of Binary Tree in "Binary Tree problem aggregation.md" 
    file & Binary Search Tree / ordered set in "BST problem aggregation.md" file, do the same quality of analysis work
    as you did previously on graph, DP, then provide final list for both of those 2 topic one by one.

    Don't try to do it in one single response - one response for complete Binary Tree and let me check, then when I
    would ask you to go for Binary Search Tree / ordered set, then another response Binary Search Tree / ordered set.

    So that you don't make any mistake while segregation & categorization!.. don't hurry, keep calm, don't make any
    mistake!

----------------------------------------------------
You to me:

    Binary tree's all prob patterns are ready and saved as "Binary Tree Prob Pattern.md"
    now let's start working on BST or ordered set.
    
------------------------------------------------------------
You to me:

    Binary Search tree or ordered set's all prob patterns are ready and saved as "BST Prob Pattern.md"
    Now have a look at both of them carefully.

------------------------------------------------------------
Me to you:
    Okay, we finished Graph, DP, Binary Tree, BST or Ordered Set.
    Now it's the time to start working on Recursion & Backtracking problem patterns. As usual, I collected all problems
    from FRAZ & Algomaster.io, which is available on "Recursion & Backtracking prob aggregation.md" file.

    So go ahead and start working as did during Graph, DP, Binary Tree, BST or Ordered Set.

------------------------------------------------------------
You to me:

    Recursion & Backtracking's all prob patterns are ready and saved as "Recursion & Backtracking Prob Pattern.md"
    Now have a look at both of them carefully.

------------------------------------------------------------
Me to you:
    Okay, you did great job around Recursion & Backtracking pattern.
    Now it's the time to start working on Binary Search problem patterns. As usual, I collected all problems
    from FRAZ & Algomaster.io, which is available on "Binary Search prob aggregation.md" file.

    So go ahead and start working as did during Graph, DP, Binary Tree, BST or Ordered Set, Recursion & Backtracking.

------------------------------------------------------------
You to me:

    Binary Search's all prob patterns are ready and saved as "Binary Search Prob Pattern.md"
    Now have a look at both of them carefully.

------------------------------------------------------------
Me to you:
    Okay, you did great job around Binary Search as well.
    Now it's the time to start working on Linked List problem patterns. As usual, I collected all problems
    from FRAZ & Algomaster.io, which is available on "LinkedList prob aggregation.md" file.

    So go ahead and start working as did deep analysis during Graph, DP, Binary Tree, BST or Ordered Set, Recursion & Backtracking, 
    Binary search.

------------------------------------------------------------
You to me:

    Linked List's all prob patterns are ready and saved as "LinkedList Prob Pattern.md"
    Now have a look at both of them carefully.

------------------------------------------------------------
Me to you:
    Okay, you did great job around Binary Search as well.
    Now it's the time to start working on Stack problem patterns. As usual, I collected all problems
    from FRAZ & Algomaster.io, which is available on "Stack prob aggregation.md" file.

    So go ahead and start working as did deep analysis during Graph, DP, Binary Tree, BST or Ordered Set, Recursion & Backtracking, 
    Binary search, Linked List

------------------------------------------------------------
You to me:
    I've gone through all the files carefully, including `current conversation.md` which gives me the full picture of our work so far.
    
    Here's my understanding of where we are:
    
    **Completed topics:**
      - Graph
      - DP
      - Binary Tree
      - BST / Ordered Set
      - Recursion & Backtracking
      - Binary Search
      - Linked List
    
    **Current task:** Stack — the `Stack prob aggregation.md` has problems collected from both Fraz and Algomaster, and `Stack Prob Pattern.md` is currently **empty**. My job is to build it from scratch with the same rigor.
    
    Let me do the full analysis now.
    
    ---
    
    ## Step 1: Pattern Inventory from Both Sheets
    
    **Fraz's patterns:**
      - Parenthesis / Advance Stack Problems
      - Monotonic Stack
    
    **Algomaster's patterns:**
      - Standard Stack Problems
      - Monotonic Stack
    
    ---
    
    ## Step 2: Problem Analysis & Conflict Resolution

    Both sheets agree on the two broad categories. But "Parenthesis / Advance Stack Problems" and "Standard Stack Problems" are essentially the same bucket with different names — Algomaster's version is more comprehensive. The real question is whether **two patterns are enough**, or whether the problem set reveals more distinct sub-patterns hiding inside "Standard Stack."
    
    Let me analyze the full problem list:
    
    **Standard/Advanced Stack problems across both sheets:**
      - Valid Parentheses, Longest Valid Parentheses, Minimum Remove to Make Valid, Valid Parenthesis String → all parenthesis matching
      - Min Stack, Maximum Frequency Stack → augmented/design stack
      - Basic Calculator I & II → expression evaluation
      - Evaluate Reverse Polish Notation → postfix evaluation
      - Decode String → nested bracket recursion via stack
      - Asteroid Collision, Remove All Adjacent Duplicates, Remove Duplicate Letters, Remove K Digits, Removing Stars → "simulation/reduction" problems — process element, decide push or pop/modify
      - Simplify Path → stack on string tokens
      - Baseball Game, Exclusive Time of Functions → simulation
      - Car Fleet, Validate Stack Sequences → structural reasoning
      - Insert Interval → this is **not a stack problem** — it's an interval problem; Fraz placed it incorrectly
    
    **Monotonic Stack problems:**
      - Next Greater Element I & II, Daily Temperatures, Online Stock Span, Buildings with Ocean View → classic NGE
      - Sum of Subarray Minimums, Sum of Subarray Ranges → contribution counting
      - Trapping Rain Water, Largest Rectangle in Histogram → histogram/area problems
      - 132 Pattern, Maximum Width Ramp, Shortest Unsorted Continuous Subarray → order-violation problems
      - Remove K Digits, Create Maximum Number → greedy lexicographic
      - Beautiful Towers II → prefix/suffix max via monotonic stack
      - Max Chunks to Make Sorted, Number of Visible People → structural reasoning
    
    ---
    
    ## Step 3: Missing Patterns & Problems
    
    **Missing problems worth adding:**
      - LC 84 Largest Rectangle in Histogram is listed — but LC 85 Maximal Rectangle (which directly builds on it using stack row by row) is missing. Essential pair.
      - LC 907 Sum of Subarray Minimums is listed — LC 2104 Sum of Subarray Ranges is also listed in Algomaster — good, both are there.
      - LC 496 Next Greater Element I is listed — LC 503 Next Greater Element II is also listed — good.
      - LC 1944 Number of Visible People in a Queue — listed in Algomaster, worth keeping; frequently asked at Google.
      - LC 735 Asteroid Collision — listed in both, keep.
      - **Missing:** LC 316 Remove Duplicate Letters is listed — but the pattern note connecting it to LC 402 Remove K Digits (both are "build lexicographically smallest/largest result via monotonic stack greedy") needs to be explicit. Both are there but their connection should be flagged.
      - **Missing:** LC 901 Online Stock Span — listed in Algomaster, not in Fraz. Keep it.
    
    **Insert Interval (LC 57)** — this is in Fraz's "Parenthesis / Advance Stack" section but has zero relation to stacks. It's a pure interval problem. **Drop it entirely from this sheet.**
    
    ---
    
    ## Step 4: Final Pattern Structure
    
    After analysis, the problems naturally cluster into **4 distinct patterns**, not 2:
    
       1. **Parenthesis & Bracket Problems** — matching, validity, minimum removals; the stack models nesting depth
       2. **Stack Simulation & Design** — expression evaluation, calculators, augmented stacks (Min Stack, Freq Stack); the stack models a process or state machine
       3. **Monotonic Stack — Next Greater/Smaller** — NGE, temperatures, stock span, buildings; the stack maintains a monotone invariant
       4. **Monotonic Stack — Area, Sum & Advanced** — histograms, subarray min/max sums, contribution counting, lexicographic greedy; harder applications of the same invariant
    
    Splitting monotonic stack into two tiers matters for learning order — NGE problems are the entry point and must be solid before attempting histogram area or contribution counting problems.
    
    ---

    Now here is the complete final sheet and saved as "Stack Prob Pattern.md" inside stack folder.

    This is the complete Stack pattern sheet. The key structural decisions to flag:

    Why four patterns instead of two? Fraz and Algomaster both used two buckets (Standard + Monotonic), but that hides a 
    critical learning dependency. The NGE template and the area/contribution/greedy applications require different mental
    models at the pop step — conflating them causes learners to memorize LC 84 without understanding why it works. 
    Separating them makes the progression explicit.

    Why split Monotonic Stack at all if the mechanism is the same? Because the trigger recognition differs. "Next greater
    element" is immediately obvious as a monotonic stack problem. "Sum of subarray minimums" is not — you need the 
    contribution counting insight first. Grouping them by difficulty of recognition, not just mechanism, is what makes 
    this sheet useful for unseen problems.

    LC 57 removal from Fraz's stack section. This was a clear misclassification. Keeping it here would pollute the pattern
    with noise and weaken your ability to identify what actually belongs here.

------------------------------------------------------------
Me to you:
    Okay, you did great job around Binary Search as well.
    Now it's the time to start working on Queue problem patterns. As usual, I collected all problems
    from FRAZ & Algomaster.io, which is available on "Queue prob aggregation.md" file.

    So go ahead and start working as did deep analysis during Graph, DP, Binary Tree, BST or Ordered Set, Recursion & Backtracking, 
    Binary search, Linked List, Stack.
------------------------------------------------------------
You to me:
    I've gone through all the files carefully, including `current conversation.md` which gives me the full picture of our work so far.
    
    **Completed topics:**
      - Graph
      - DP
      - Binary Tree
      - BST / Ordered Set
      - Recursion & Backtracking
      - Binary Search
      - Linked List
      - Stack
    
    **Current task:** Queue — the `Queue prob aggregation.md` has problems collected from both Fraz and Algomaster, and `Queue Prob Pattern.md` is currently **empty**. My job is to build it from scratch with the same rigor.
    
    Let me do the full analysis now.
    
    ---
    
    ## Step 1: Pattern Inventory from Both Sheets
    
    **Fraz's patterns:**
      - Implementation Problems
      - Singly-Ended Queue
      - Doubly-Ended Queue
    
    **Algomaster's patterns:**
      - Standard Queue Problems
      - Monotonic Queue
    
    ---
    
    ## Step 2: Problem Analysis & Conflict Resolution
    
    Fraz's "Singly-Ended Queue" and Algomaster's "Standard Queue Problems" are essentially the same bucket. Fraz's "Doubly-Ended Queue" and Algomaster's "Monotonic Queue" overlap but are not identical — a deque is the data structure, a monotonic queue is the *technique*. These need to be cleanly separated.
    
    Let me analyze every problem:
    
    **Implementation / Design problems (Fraz):**
      - Implement Stack Using Queues, Implement Queue Using Stacks, Queue Using Two Stacks → classic CS fundamentals, interview warm-ups
      - Design Circular Queue, N-Queue Using Array → implementation-level design
      - Design Circular Deque → deque design
    
    **Standard Queue problems (Fraz + Algomaster):**
      - Find the Winner of the Circular Game (LC 1823) → simulate Josephus problem with queue
      - Reveal Cards in Increasing Order (LC 950) → simulate queue-based card dealing
      - Number of Recent Calls (LC 933) → sliding window via queue
      - Time Needed to Buy Tickets (LC 2073) → circular queue simulation
      - Moving Average from Data Stream (LC 346) → fixed-size window sum via queue
      - First Unique Number (LC 3656) → queue + set for ordering
      - Number of People Aware of a Secret (LC 2327) → BFS-like spreading via queue
      - Stamping the Sequence (LC 936) → hard simulation problem, reverse stamping via queue
    
    **Monotonic Queue / Deque problems:**
      - Sliding Window Maximum (LC 239) → the canonical monotonic deque template
      - Continuous Subarrays (LC 2762) → sliding window with monotonic deque for range tracking
      - Jump Game VI (LC 1696) → sliding window DP with deque (already in DP sheet)
      - Find the Most Competitive Subsequence (LC 1673) → monotonic deque greedy
      - Partition Array Such That Maximum Difference is K (LC 2294) → sliding window
      - Longest Continuous Subarray with Abs Diff ≤ Limit (LC 1438) → two deques (max + min)
      - Max Value of Equation (LC 1499) → deque on linear recurrence
      - Constrained Subsequence Sum (LC 1425) → sliding window DP with deque (already in DP sheet)
      - Shortest Subarray with Sum at Least K (LC 862) → prefix sum + monotonic deque
    
    ---
    
    ## Step 3: Conflict Resolution & Classification
    
    **Key conflict:** Fraz puts "Sliding Window Maximum" under Doubly-Ended Queue (data structure focus). Algomaster puts it under Monotonic Queue (technique focus). The technique label is more useful for pattern recognition — **Monotonic Queue wins**.
    
    **Cross-sheet duplicates with DP sheet:**
      - LC 1696 Jump Game VI — already in DP sheet (Sliding Window DP). Keep a reference here but don't list as primary.
      - LC 1425 Constrained Subsequence Sum — same situation.
    
    These two problems sit at the intersection of DP and Queue patterns. Primary home is DP sheet. A cross-reference note in the Queue sheet is the right call — same as how LC 394 Decode String appears in both Stack and Recursion sheets with a note.
    
    **Stamping the Sequence (LC 936):** This is genuinely hard and somewhat isolated in technique — reverse simulation with a queue. It's rare in interviews and the technique doesn't generalize broadly. Flag it as optional/hard at the end of the Standard Queue section.
    
    ---
    
    ## Step 4: Missing Problems Worth Adding
    
      - **LC 862. Shortest Subarray with Sum at Least K** — listed in Algomaster's monotonic queue section. This is one of the most important deque problems — prefix sum + monotonic deque to find shortest subarray. Must keep.
      - **LC 239. Sliding Window Maximum** — listed in both sheets. This is the canonical template for all monotonic deque problems. Non-negotiable anchor problem.
      - **LC 1438. Longest Continuous Subarray with Abs Diff ≤ Limit** — two deques simultaneously (one for max, one for min). Strong interview problem, keep.
      - **Missing from both sheets:** LC 59 Spiral Matrix II / LC 54 Spiral Matrix — sometimes appears in queue-based BFS simulations, but these belong in Array, not Queue. Don't add.
      - **Missing:** The Josephus problem (LC 1823 Find Winner of Circular Game) appears in Fraz. Keep — it's a clean queue simulation problem asked at Amazon/Google.
        
      ---
    
      ## Step 5: Final Pattern Structure
    
      After analysis, four patterns emerge naturally:
    
         1. **Queue Implementation & Design** — the data structure mechanics; needed before anything else
         2. **Standard Queue Simulation** — problems where a queue models a process: circular games, card dealing, ticket buying, secret spreading
         3. **Monotonic Deque — Sliding Window** — the canonical technique: maintain a deque of useful candidates across a window
         4. **Monotonic Deque — Advanced Applications** — harder problems: prefix sum + deque, DP + deque, two simultaneous deques

     ---

     Here is the complete final sheet and saved as Queue Prob Pattern.md inside Queue folder.

    ---

    The Queue sheet is now complete. A few key structural decisions to flag:

    **Why split Monotonic Queue into two tiers?** Fraz and Algomaster both treated it as one bucket. But LC 239 and LC 862
    require fundamentally different entry points — one is pure sliding window, the other requires you to first recognize 
    a prefix sum or DP structure. Conflating them causes learners to memorize LC 862 without understanding why the deque is there.

    **Why keep LC 1696 and LC 1425 as cross-references rather than full entries?** Because their primary lesson is the DP 
    recurrence, not the deque. Adding them as full entries here would duplicate work from the DP sheet and dilute the Queue
    pattern's identity. The cross-reference preserves the connection without redundancy.

    **Stamping the Sequence (LC 936) flagged as optional.** The reverse simulation technique is clever but does not generalize
    to a broader family of problems. Including it as a full entry would give it misleading weight. Flagging it as hard/optional 
    is honest about its interview frequency and pattern transferability.