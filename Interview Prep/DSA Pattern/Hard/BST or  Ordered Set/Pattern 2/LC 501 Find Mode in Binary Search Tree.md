# LC 501. Find Mode in Binary Search Tree

Key Concept: Inorder traversal — track current streak; no extra space needed
Problem: https://leetcode.com/problems/find-mode-in-binary-search-tree/description/
Solution: https://www.youtube.com/watch?v=tYoq7CMJP4A&t=26s
Status: Done

# Stage 1: Identification

### **Step 1 — Which topic?**

You're given the root of a **Binary Search Tree** that may contain **duplicate values**. You need to find the **mode(s)** — the value(s) that appear with the **highest frequency**. If there are multiple values tied for the highest frequency, return all of them (in any order).

### **Step 2 — Which pattern?**

Still **BST Pattern 2: BST Properties & Validation**. The trigger:

> "find mode", "most frequently occurring value(s)" in a BST → exploit **inorder traversal = sorted sequence** to count streaks, with **no extra space**
> 

### **Step 3 — Which key concept?**

**Inorder traversal — track the current streak of equal values; no extra space needed.**

The sharp part of this problem, and the reason it's a distinct entry in this pattern rather than a repeat of LC 530: because duplicates in a BST always sit **adjacent to each other** in sorted order (all equal values must occupy a contiguous run in a sorted sequence), you never need a hash map to count frequencies. A single running streak counter, maintained while walking inorder, is enough to find the mode(s) in **O(1) extra space** — not counting the recursion stack.

---

# Stage 2: Intuition Building

### The Tree We're Working With

```
        2
       / \
      1   2
```

Inorder traversal: `1, 2, 2` — sorted, as always. Notice `2` appears twice, and **both occurrences sit right next to each other** in this sorted sequence — never separated by some other value.

### Why Duplicates in a BST Are Always Adjacent in Sorted Order

This is worth nailing down explicitly, because it's the entire foundation the "no extra space" trick rests on.

If a BST is allowed to have duplicate values, some convention must exist for where a duplicate goes relative to an existing equal node (commonly: equal values go to the right, though the exact convention doesn't matter for this argument). What matters is this: **the BST ordering invariant (left ≤ node ≤ right, or similar) guarantees that every occurrence of a given value must be more extreme in one direction than any smaller value and less extreme than any larger value.** Concretely — if value `v` appears multiple times anywhere in the tree, there is no way for some *other* value strictly between two occurrences of `v` to exist, because "strictly between two `v`'s" would mean a value that's simultaneously `≥` one occurrence of `v` and `≤` another occurrence of `v` — which forces it to equal `v` itself, a contradiction.

```
┌───────────────────────────────────────────────────────────────────────┐
│  In a SORTED sequence, all occurrences of the same value              │
│  MUST form one single contiguous block — nothing else can             │
│  wedge itself between two equal values without itself being           │
│  equal to them.                                                       │
│                                                                       │
│  Since BST inorder traversal produces a sorted sequence,              │
│  every duplicate value in the tree shows up as one unbroken           │
│  "streak" during inorder — never scattered.                           │
└───────────────────────────────────────────────────────────────────────┘
```

### The Naive First Instinct — Hash Map of Value → Count

The obvious approach: traverse the tree (any order), tally every value's frequency in a `HashMap<Integer, Integer>`, then scan the map for whichever value(s) have the maximum count.

```
Traverse: 1, 2, 2  (any order works for counting)
map = {1: 1, 2: 2}
max frequency = 2 → mode = [2]
```

This works, and is a perfectly reasonable **first pass**. But it costs `O(n)` **extra space** for the map — and the moment you notice duplicates in a BST are always adjacent in sorted order, you should ask: *can I count a streak without ever needing to look values up by key?*

### The Core Insight — A Running Streak, Not a Frequency Table

Because inorder guarantees "all occurrences of the same value arrive back-to-back," you don't need to remember *every* value's count simultaneously — you only ever need to know **the count of whatever streak you're currently inside**, plus **the best streak count seen so far**.

Walk inorder, and at each visited node:

```
if current value == previous value:
    → we're still inside the same streak → currentStreak += 1
else:
    → a NEW streak has just started → currentStreak = 1

Compare currentStreak against the best streak seen so far (maxStreak):
    if currentStreak > maxStreak:
        → this is a NEW strict best → maxStreak = currentStreak
        → CLEAR the result list (everything found before is now
          beaten) → add current value as the sole mode so far
    else if currentStreak == maxStreak:
        → this value TIES the current best → add it to the result
          list too (don't clear anything)
    else (currentStreak < maxStreak):
        → this value's streak isn't good enough → do nothing
```

```
┌───────────────────────────────────────────────────────────────────────┐
│  ONE running streak counter + ONE best-streak counter is ALL          │
│  that's needed — no hash map, no per-value storage.                   │
│                                                                       │
│  Because equal values are ALWAYS adjacent in inorder, "did the        │
│  streak just get beaten, tied, or fall short" is a decision you       │
│  can make immediately, using only the PREVIOUS value and the          │
│  running counts — nothing further back needs to be remembered.        │
└───────────────────────────────────────────────────────────────────────┘
```

### Walking Through a Richer Example

```
1 1
1 2
2 2
2 2
```

```
              1
             / \
           null  2
                / \
               2   2
                  /
                 2
```

Inorder traversal: `1, 2, 2, 2, 2`.

```
Visit 1: no previous value → new streak. currentStreak = 1.
         maxStreak was 0 → 1 > 0 → NEW BEST. maxStreak = 1.
         result = [1]
         prev = 1

Visit 2: 2 != prev(1) → new streak. currentStreak = 1.
         1 == maxStreak(1) → TIE. result = [1, 2]
         prev = 2

Visit 2: 2 == prev(2) → same streak. currentStreak = 2.
         2 > maxStreak(1) → NEW BEST. maxStreak = 2.
         result = [2]   ← the old "1" gets CLEARED, it's beaten
         prev = 2

Visit 2: 2 == prev(2) → same streak. currentStreak = 3.
         3 > maxStreak(2) → NEW BEST. maxStreak = 3.
         result = [2]   ← still just [2], the streak owner
         prev = 2

Visit 2: 2 == prev(2) → same streak. currentStreak = 4.
         4 > maxStreak(3) → NEW BEST. maxStreak = 4.
         result = [2]
         prev = 2

Final: result = [2], maxStreak = 4
```

**Mode = [2]**, appearing 4 times — correct, and the result list was cleared and rebuilt entirely through the streak logic, never touching a hash map.

### Why "Clear and Restart" Is Safe When a Strictly Better Streak Appears

This deserves a moment of justification: the instant `currentStreak > maxStreak`, every value **previously** added to the result list is now provably **not** a mode — because a genuinely higher frequency has just been found, and mode requires the *maximum* frequency. So clearing the list and starting fresh with just the current value is always correct, never a case of prematurely discarding a value that might turn out to matter later — once beaten, a frequency can never "come back" to tie or beat a later, higher streak, since frequencies only accumulate monotonically as a streak continues and reset to 1 the moment a new value starts.

### Where Exactly Does the Streak Logic Run?

Same discipline as LC 530 and LC 98's alternative: the comparison against `prev` happens at the **root step** of inorder — left, then this check, then right — because that's the exact moment values arrive in sorted order, one at a time.

### Why the Traversal Terminates and Touches Every Node Exactly Once

Identical reasoning to every inorder-based problem in this pattern: base case `node == null → return`, every recursive call moves strictly to a child, the tree is finite with no cycles, so the traversal completes after visiting each of the `n` nodes exactly once.

---

# Stage 3: Coding

## Approach 1 — Brute Force: Hash Map Frequency Count

**The honest thinking:**

> "Traverse the tree with any order, tally every value's frequency in a map. Then scan the map to find whichever value(s) share the maximum frequency."
> 

```java
class Solution {
    public int[] findMode(TreeNode root) {
        Map<Integer, Integer> freq = new HashMap<>();
        collect(root, freq);

        int maxFreq = 0;
        for (int count : freq.values()) {
            maxFreq = Math.max(maxFreq, count);
        }

        List<Integer> modes = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : freq.entrySet()) {
            if (entry.getValue() == maxFreq) {
                modes.add(entry.getKey());
            }
        }

        int[] result = new int[modes.size()];
        for (int i = 0; i < modes.size(); i++) result[i] = modes.get(i);
        return result;
    }

    private void collect(TreeNode node, Map<Integer, Integer> freq) {
        if (node == null) return;
        freq.put(node.val, freq.getOrDefault(node.val, 0) + 1);
        collect(node.left, freq);
        collect(node.right, freq);
    }
}
```

**Why this doesn't use the BST property:** it works identically on any binary tree, BST or not — it never exploits the fact that duplicates must be adjacent in sorted order. It costs `O(n)` extra space for the map, plus a second pass over the map to find the max. Establishes correctness only.

---

## Approach 2 — Optimal: Inorder Streak Tracking, No Extra Space

**Mental workflow before writing a single line:**

```
1. Maintain three pieces of running state:
   → prev: value of the last node visited (Long, boxed, null initially)
   → currentStreak: length of the streak of equal values we're
     currently inside
   → maxStreak: the best streak length seen so far
   → result: a growable list of the current mode(s)

2. Recursively, in INORDER (left, root, right):
   → recurse left first
   → at the "visit" step:
       if prev != null and current.val == prev:
           currentStreak += 1        (still inside the same streak)
       else:
           currentStreak = 1          (a brand-new streak just started)

       if currentStreak > maxStreak:
           maxStreak = currentStreak
           result.clear()             (everything before is now beaten)
           result.add(current.val)
       else if currentStreak == maxStreak:
           result.add(current.val)    (ties the current best)

       prev = current.val
   → recurse right

3. Base case: node == null → return

4. Convert result to an int[] and return it
```

```java
class Solution {
    private Long prev = null;
    private int currentStreak = 0;
    private int maxStreak = 0;
    private List<Integer> result = new ArrayList<>();

    public int[] findMode(TreeNode root) {
        inorder(root);

        int[] modes = new int[result.size()];
        for (int i = 0; i < result.size(); i++) {
            modes[i] = result.get(i);
        }
        return modes;
    }

    private void inorder(TreeNode node) {
        // Base case: nothing here to visit
        if (node == null) return;

        // Step 1: LEFT — fully explore the smaller side first
        inorder(node.left);

        // Step 2: ROOT — the "visit" moment. Duplicates in a BST
        // are ALWAYS adjacent in sorted order, so comparing only
        // against the immediately previous value is enough to
        // track the current streak correctly — no hash map needed.
        if (prev != null && node.val == prev) {
            currentStreak++;             // still inside the same streak
        } else {
            currentStreak = 1;           // a new streak just started
        }

        if (currentStreak > maxStreak) {
            // A strictly better streak was just found — everything
            // in `result` so far is now provably not the mode.
            maxStreak = currentStreak;
            result.clear();
            result.add(node.val);
        } else if (currentStreak == maxStreak) {
            // Ties the current best frequency — also a mode.
            result.add(node.val);
        }

        prev = (long) node.val;

        // Step 3: RIGHT — continue into the larger side
        inorder(node.right);
    }
}
```

---

## Workflow Trace on the Richer Example

```
              1
             / \
           null  2
                / \
               2   2
                  /
                 2

inorder(1):
  inorder(null) → return
  VISIT 1: prev=null → currentStreak=1. 1 > maxStreak(0) → NEW BEST.
           maxStreak=1, result=[1]. prev=1.
  inorder(2):
    inorder(2):
      inorder(2):
        inorder(null) → return
        VISIT 2: prev=1, 2!=1 → currentStreak=1. 1==maxStreak(1) → TIE.
                 result=[1, 2]. prev=2.
        inorder(null) → return
      VISIT 2: prev=2, 2==2 → currentStreak=2. 2 > maxStreak(1) → NEW BEST.
               maxStreak=2, result=[2]. prev=2.
      inorder(null) → return
    VISIT 2: prev=2, 2==2 → currentStreak=3. 3 > maxStreak(2) → NEW BEST.
             maxStreak=3, result=[2]. prev=2.
    inorder(null) → return

Final: result = [2], maxStreak = 3
```

(Matches the earlier hand-trace's logic exactly — tree shape here has one fewer `2` than the Stage 2 walk-through, giving `maxStreak = 3` instead of `4`, but the mechanism is identical.)

---

## Complexity Analysis

**Time Complexity — O(n):**

Every node is visited exactly once by the inorder recursion. At each visit, the work done is O(1) — a couple of comparisons, at most a list clear-and-insert (amortized O(1) for a single element, since clearing and re-adding one element is bounded work relative to the traversal, not proportional to the list's prior size in any way that changes the overall asymptotic bound). With `n` total nodes, total time is **O(n)**.

**Space Complexity — O(h) auxiliary, excluding the output:**

The only auxiliary space is the recursion call stack, exactly like every other inorder-based problem in this pattern — one frame per node on the current root-to-node path.

- **Best case (balanced BST):** `h = O(log n)`.
- **Worst case (skewed BST):** `h = O(n)`.

The `result` list itself is **not** counted as auxiliary space — it's the required output, holding at most all `n` values in the pathological case where every value is unique (every value ties at frequency 1). This mirrors the same "output list doesn't count as extra space" convention used throughout this pattern sheet (e.g., LC 144's iterative preorder). The genuine improvement over Approach 1 is that **no hash map is needed at all** — the streak logic entirely replaces the need for per-value frequency storage.

---

## Comparing Brute Force vs Optimal

| Property | Brute Force (Hash Map) | Optimal (Inorder Streak) |
| --- | --- | --- |
| Uses BST property? | No — works on any binary tree | Yes — exploits "duplicates are adjacent in sorted order" |
| Extra space (beyond call stack / output) | O(n) — the frequency map | O(1) — just a few running variables |
| Passes needed | Two — one to build the map, one to scan for the max | One — streak tracking happens inline during the single traversal |
| Core trick | None — direct frequency counting | Adjacent-duplicates guarantee replaces the need for a map entirely |

---

## The Key Takeaway

```
┌───────────────────────────────────────────────────────────────────────┐
│  In a BST, duplicate values are ALWAYS adjacent to each other         │
│  in inorder (sorted) order — nothing else can wedge between           │
│  two occurrences of the same value without itself being equal         │
│  to it.                                                               │
│                                                                       │
│  THE TRICK: because of this, you never need a hash map to count       │
│  frequencies. A single running STREAK counter — reset to 1            │
│  whenever the value changes, incremented when it repeats — is         │
│  enough to track every value's count, one streak at a time.           │
│                                                                       │
│  Compare each streak against the best seen so far:                    │
│      strictly better → CLEAR the result list, start fresh             │
│      exactly tied     → ADD to the result list                        │
│      falls short      → do nothing                                    │
│                                                                       │
│  Same skeleton as LC 530/LC 98-alternative: inorder + one small       │
│  piece of running state, letting the sorted guarantee do the          │
│  rest — here that state is a streak counter instead of a              │
│  running difference or strictly-increasing check.                     │
│                                                                       │
│  Time  — O(n): every node visited exactly once.                       │
│  Space — O(h) auxiliary (call stack only); O(1) extra beyond          │
│           that — no hash map needed, unlike the brute force.          │
└───────────────────────────────────────────────────────────────────────┘
```