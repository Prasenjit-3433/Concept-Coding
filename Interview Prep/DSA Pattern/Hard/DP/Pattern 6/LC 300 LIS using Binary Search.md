# LC 300. LIS using Binary Search

Key Concept: Core template — Binary Search gives O(n log n) 
Solution: https://www.youtube.com/watch?v=on2hvxBXJH4&ab_channel=takeUforward
Status: Done

# Part 1: Intuition & Why Binary Search

---

## Why O(n²) Fails Here

The problem constraint is **n = 10⁵**.

The best DP approach (tabulation/memoization) gives us:

- Time: **O(n²)**
- Space: **O(n)**

But n² at n = 10⁵ means **10¹⁰ operations** — that's a hard TLE. We need something better. This is where binary search enters the picture.

---

## Building Intuition From Scratch

### The Problem

Given an array, find the **length** of the longest strictly increasing subsequence.

A subsequence preserves order but doesn't need to be contiguous.

```
Array:  [1, 7, 8, 4, 5, 6, -1, 9]

Valid subsequence:     1, 7, 8       (increasing)
Not a subsequence:    1, 8, 7       (order violated — 7 appears before 8 in original)
Longest:              1, 4, 5, 6, 9  (length 5)
```

---

### The First Mental Model — *"Attach or Start Fresh?"*

The instructor's starting point: *walk across every element and decide —* 

> *Can I attach this to an existing subsequence, or do I start a new one?*
> 

Let's trace through `[1, 7, 8, 4, 5, 6, -1, 9]`:

```
Element: 1
  → Nothing exists yet. Start fresh.
  Subsequences: [1]

Element: 7
  → 7 > 1, attach it.
  Subsequences: [1, 7]

Element: 8
  → 8 > 7, attach it.
  Subsequences: [1, 7, 8]

Element: 4
  → 4 < 8, can't attach to [1,7,8]
  → But 4 > 1, so start [1, 4] as a new branch
  Subsequences: [1, 7, 8]
                [1, 4]

Element: 5
  → 5 < 8, can't attach to [1,7,8]
  → 5 > 4, attach to [1,4]
  Subsequences: [1, 7, 8]
                [1, 4, 5]

Element: 6
  → 6 < 8, can't attach to [1,7,8]
  → 6 > 5, attach to [1,4,5]
  Subsequences: [1, 7, 8]
                [1, 4, 5, 6]

Element: -1
  → Can't attach anywhere. -1 is the smallest.
  → Start fresh.
  Subsequences: [1, 7, 8]
                [1, 4, 5, 6]
                [-1]

Element: 9
  → 9 > everything. Attach to all three.
  Subsequences: [1, 7, 8, 9]      → length 4
                [1, 4, 5, 6, 9]   → **length 5**  ✓
                [-1, 9]            → length 2
```

**Answer = 5** ✓

But notice the problem — we're creating **multiple separate subsequences**, branching at every conflict. This eats up a lot of *space* and time. We need a smarter way.

---

## 💡The Key Insight — *Overwrite* Instead of Creating New Branch

The instructor asks a critical question here:

> *"We only need the **LENGTH**, not the actual subsequence. Can we exploit that?"*
> 

Yes. Here's the idea:

When `4` came in and conflicted with `[1, 7, 8]`, instead of spawning a new branch `[1, 4]`, what if we just **replaced 7 with 4 in the same array?**

```
Before 4 arrives:   [1, 7, 8]
4 conflicts with 8, but fits after 1.
Replace 7 with 4:   [1, 4, 8]

so we're kind of embedding the new branch into the old one
[1, 4] embedded into [1, 7, 8], becomes [**1, 4**, 8]
```

**Wait — is `[1, 4, 8]` a valid subsequence of the original array?**
No, it's not. In the original array, 4 appears *after* 8. So `[1, 4, 8]` is not even a real subsequence.

**But the instructor says — that's okay. We're not tracking the actual subsequence. We're only tracking the LENGTH.**

The reason this works:

- We know 8 is in the temp array because something smaller (7) existed before it.
- Even if we overwrite 7 with 4, we still know there's something before 8.
- The **length** of the array doesn't change from this overwrite.
- What we gain: a smaller "tail" at position 1, which gives future elements a better chance to extend the sequence.

Let's retrace using this overwrite strategy:

```
Temp array after each element:

Start:          []
Element 1:      [1]
Element 7:      [1, 7]          → 7 > last(1), just append
Element 8:      [1, 7, 8]       → 8 > last(7), just append
Element 4:      [1, 4, 8]       → 4 < last(8), replace 7 with 4
Element 5:      [1, 4, 5]       → 5 < last(8), replace 8 with 5
Element 6:      [1, 4, 5, 6]    → 6 > last(5), just append
Element -1:     [-1, 4, 5, 6]   → -1 < last(6), replace 1 with -1
Element 9:      [-1, 4, 5, 6, 9]→ 9 > last(6), just append

Final temp array: [-1, 4, 5, 6, 9]
Length = 5  ✓
```

**The temp array at the end is NOT the actual LIS.**
The actual LIS is `[1, 4, 5, 6, 9]` but our temp array shows `[-1, 4, 5, 6, 9]`.
That's fine — the **length is correct**, and that's all we need.

---

## Why Is the Temp Array Always *Sorted*?

This is the property that makes binary search possible, and it comes directly from how we build the array:

- We only **append** when the new element is **greater than the last element** → preserves sorted order.
- We only **replace** an element with something **smaller** (never larger) → still preserves sorted order.

So at every point in time, the temp array is **strictly sorted in increasing order**.

---

## Why Binary Search? — First Principles

Now that we know the temp array is always sorted, the question becomes:

> *"When a new element comes in, where exactly do we place it?"*
> 

We want:

- If the element **already exists** in temp → replace it in place (no change to length).
- If it **doesn't exist** → find the ***first element in temp that is greater*** than the incoming element, and replace it.

Both of these reduce to one operation:

> **Find the first index in temp where `temp[index] >= incoming element`.**
> 

This is exactly the definition of **lower_bound** in binary search, which we learned before.

Since temp is always sorted, this search takes **O(log n)** instead of O(n).

This brings total complexity to **O(n log n)** — perfectly fine for n = 10⁵.

```
n = 10⁵
n log n = 10⁵ × 17 ≈ 1.7 × 10⁶ operations  ✓
```

---

## The Property That Ties It All Together

```
┌────────────────────────────────────────────────────┐
│  Temp array is always sorted                            │
│       ↓                                                 │
│  Binary search is valid on it                           │
│       ↓                                                 │
│  Each element → O(log n) placement                      │
│       ↓                                                 │
│  n elements → O(n log n) total                          │
│       ↓                                                 │
│  Length of temp array at end = Length of LIS            │
└────────────────────────────────────────────────────┘
```

---

# Part 2: Algorithm + Code + Interview Tips

---

## The Algorithm Workflow

The entire algorithm revolves around maintaining one temp array that is **always sorted**. Here's the complete decision logic:

```
For each element x in the input array:

    Case 1: x > last element of temp
    → Simply append x to temp.
    → Length increases by 1.

    Case 2: x <= some element in temp
    → Binary search for the first index where temp[index] >= x
    → Replace temp[index] with x
    → Length stays the same.

After all elements are processed:
    → return temp.size() — that is your LIS length.
```

**The binary search we need:**

We need the index of the **first element in temp that is >= x**.
This is the classic **lower bound** binary search.

```
lower_bound(temp, x):
    lo = 0, hi = temp.size() - 1
    result = temp.size()   ← default: x is larger than everything

    while lo <= hi:
        mid = (lo + hi) / 2
        if temp[mid] >= x:
            result = mid   ← mid is a candidate, but try to go left
            hi = mid - 1
        else:
            lo = mid + 1

    return result
```

---

## The Overwrite Table (Diagram)

This is the most important visual. Read each row as the **state of temp after processing that element**.

```
Array: [1, 7, 8, 4, 5, 6, -1, 9]

Element  │ Action                             │ Temp Array
────────┼─────────────────────────────────┼──────────────────────
  1      │ temp is empty → append             │ [1]
  7      │ 7 > last(1)  → append              │ [1, 7]
  8      │ 8 > last(7)  → append              │ [1, 7, 8]
  4      │ lower_bound = index 1 (7≥4)        │ [1, 4, 8]
         │ replace 7 with 4                   │
  5      │ lower_bound = index 2 (8≥5)        │ [1, 4, 5]
         │ replace 8 with 5                   │
  6      │ 6 > last(5)  → append              │ [1, 4, 5, 6]
  -1     │ lower_bond = index 0 (1≥-1)       │ [-1, 4, 5, 6]
         │ replace 1 with -1                  │
  9      │ 9 > last(6)  → append              │ [-1, 4, 5, 6, 9]
────────┴─────────────────────────────────┴──────────────────────

Final temp: [-1, 4, 5, 6, 9]
Length = 5  ✓

Actual LIS: [1, 4, 5, 6, 9]   ← NOT the same as temp, but same length
```

**Why the lengths match even though the arrays differ:**
Every time we overwrite, we are not losing a position — we are just making the tail of the sequence smaller (more favorable for future elements). The number of positions in temp never decreases incorrectly. Each append is a genuine length increase, and each overwrite is a "better candidate at the same length" — so the count is always honest.

---

## Handling Duplicates (Bonus Example from Instructor)

```
Array: [1, 4, 4, 2, 8]

Element  │ Action                             │ Temp Array
────────┼─────────────────────────────────┼──────────────────────
  1      │ append                             │ [1]
  4      │ 4 > last(1) → append               │ [1, 4]
  4      │ lower_bound = index 1 (4≥4)        │ [1, 4]
         │ replace 4 with 4 (no change)       │
  2      │ lower_bound = index 1 (4≥2)        │ [1, 2]
         │ replace 4 with 2                   │
  8      │ 8 > last(2) → append               │ [1, 2, 8]
────────┴─────────────────────────────────┴──────────────────────

Length = 3  ✓  (e.g., [1, 4, 8] or [1, 2, 8])
```

Notice: when a duplicate arrives (second `4`), lower_bound finds the existing `4` and replaces it with itself — temp doesn't change. Duplicates are handled naturally, no special case needed.

---

## Java Implementation

```java
import java.util.ArrayList;

public class LIS {

    // Manual lower bound — finds the first index in temp where temp[index] >= target
    // This is what C++ std::lower_bound does internally
    private static int lowerBound(ArrayList<Integer> temp, int target) {
        int lo = 0, hi = temp.size() - 1;
        int result = temp.size(); // default: target is greater than all elements → append position

        while (lo <= hi) {
            int mid = (lo + hi) / 2;
            if (temp.get(mid) >= target) {
                result = mid;   // mid is a valid position, but try further left
                hi = mid - 1;
            } else {
                lo = mid + 1;
            }
        }
        return result;
    }

    public static int lengthOfLIS(int[] arr) {
        ArrayList<Integer> temp = new ArrayList<>();

        for (int x : arr) {
            if (temp.isEmpty() || x > temp.get(temp.size() - 1)) {
                // Case 1: x is greater than everything in temp → extend the sequence
                temp.add(x);
            } else {
                // Case 2: find where x belongs and overwrite
                int idx = lowerBound(temp, x);
                temp.set(idx, x);
            }
        }

        // temp is NOT the actual LIS, but its length equals the LIS length
        return temp.size();
    }

    public static void main(String[] args) {
        int[] arr = {1, 7, 8, 4, 5, 6, -1, 9};
        System.out.println(lengthOfLIS(arr)); // Output: 5
    }
}
```

**Annotated decisions in the code:**

```
temp.isEmpty() || x > temp.get(temp.size() - 1)
→ This is the "just append" check.
→ We look only at the LAST element because temp is always sorted —
  if x > last, it's definitely > everything before last too.

lowerBound(temp, x)
→ Finds the first position where temp[pos] >= x.
→ If x exists in temp, it finds that exact position.
→ If x doesn't exist, it finds the next larger element's position.
→ Either way, we overwrite that position with x.

return temp.size()
→ NOT the LIS itself.
→ Just the count of positions — which equals the LIS length.
```

---

## Complexity

```
┌──────────────────┬────────────────────────────────────────┐
│                    │                                           │
│  Time              │  O(n log n)                               │
│                    │  n elements × log n per binary search     │
│                    │                                           │
│  Space             │  O(n)                                     │
│                    │  temp array, at most n elements           │
│                    │                                           │
└──────────────────┴────────────────────────────────────────┘

Previous best (DP):   O(n²) time, O(n) space
Binary search:        O(n log n) time, O(n) space  ✓
```

---

## Interview Tips (Directly from the Instructor)

**Tip 1 — Don't modify the original array**

> The instructor explicitly says: in interviews, it is **not preferred** to reuse the input array for overwrites. Always create a separate `temp` array. It shows clean coding practice.
> 

**Tip 2 — The temp array is NOT the LIS**

> This is the most common confusion. The instructor warns about this explicitly. If an interviewer asks you to **print the actual LIS**, this approach alone won't work — you'd need to store additional parent pointers. This approach only gives you the **length**.
> 

**Tip 3 — Know your lower_bound**

> In C++ you get `std::lower_bound` for free. In Java, you implement it manually. The instructor links a dedicated lower_bound video. The key definition to remember: *lower_bound returns the first index where `arr[index] >= target`.*
> 

**Tip 4 — Frame the "why" in interviews**

> The instructor's entire point about building intuition from first principles is itself an interview tip. When asked about this problem, don't just say "I used binary search." Walk the interviewer through:
> 
> 1. Why O(n²) DP fails for large n
> 2. Why we only need length, not the actual subsequence
> 3. Why the temp array is always sorted (which justifies binary search)
> 4. What lower_bound does and why it handles both "replace exact" and "replace next greater" in one shot

---

## One-Line Mental Model to Remember This Forever

```
"Walk through the array. If the current element extends the sequence, append it.
 If not, find where it fits using binary search and replace — because a smaller
 tail always gives future elements a better chance to extend the sequence.
 The length of the temp array at the end is your answer."
```

---

That's the complete note for this lecture. Let me know if you want any section expanded or if you're ready to move to the next lecture!