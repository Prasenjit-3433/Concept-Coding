# Lecture 4: Searching (Linear Search & Binary Search)

Date: August 24, 2026
Status: Done

# 🎯Part 1: What is Searching, Linear Search & the Idea Behind Binary Search

---

## Quick Recap Before We Begin

In Lecture 3 we built three O(n²) sorting algorithms. In Lecture 2 we previewed the payoff of sorting: **binary search**, which finds a value in O(log n) instead of O(n), but only on sorted data. Today we cash that in.

---

## 1. What is Searching?

**Searching** means finding whether a given value (the **target**) exists in a collection, and if so, **where**.

For arrays, "where" means the **index**. Both algorithms in this lecture follow the same contract:

| Situation | What the function returns |
| --- | --- |
| Target is present | the **index** where it sits |
| Target is absent | **`-1`** |

**Why `-1` for "not found"?** Valid array indices run from `0` to `n - 1`, so `-1` can never be a real position. It is a safe way to say "no such index" without any ambiguity.

⚠️ **Throughout this lecture we assume the array has unique elements** (no duplicates). That way, if the target exists, it exists at exactly one index, and "the index" is a well-defined answer.

---

## 2. Linear Search

### The Idea

Start at the first element and check each one in order, until you either find the target or run out of elements.

**Real-life analogy:** You are looking for a particular name in a stack of unsorted admission forms. There is no order to exploit, so you pick up the first form, check it, then the second, then the third, and so on. You stop the moment you find it, or when the stack is finished and you can say "it isn't here."

### Walking Through the Algorithm

```
Index:     0     1     2     3     4     5
        ┌─────┬─────┬─────┬─────┬─────┬─────┐
arr  =  │  8  │  3  │ 11  │  5  │ 14  │  2  │       n = 6
        └─────┴─────┴─────┴─────┴─────┴─────┘
```

**Case 1: target = 5 (present).**

```
i=0:  arr[0] = 8   equal to 5?  No  → move on
i=1:  arr[1] = 3   equal to 5?  No  → move on
i=2:  arr[2] = 11  equal to 5?  No  → move on
i=3:  arr[3] = 5   equal to 5?  YES → return 3

4 comparisons made. Elements at index 4 and 5 were never looked at.
```

**Case 2: target = 20 (absent).**

```
i=0:  8   ≠ 20
i=1:  3   ≠ 20
i=2:  11  ≠ 20
i=3:  5   ≠ 20
i=4:  14  ≠ 20
i=5:  2   ≠ 20
Loop finished without finding it → return -1

6 comparisons made (every element checked).
```

The second case shows the cost of an unsuccessful search: **to be sure something is absent, you have to look at everything.**

### The Code

```cpp
int linearSearch(int arr[], int n, int target) {
    for (int i = 0; i < n; i++) {
        if (arr[i] == target) {
            return i;            // found: return immediately
        }
    }
    return -1;                   // loop ended: not present
}
```

The caller uses the returned index directly:

```cpp
int index = linearSearch(arr, n, 5);    // 3
int missing = linearSearch(arr, n, 20); // -1
```

⚠️ **`return -1` goes AFTER the loop, never inside it.** A common bug is writing `else return -1;` inside the loop body. That would give up after checking only `arr[0]`, so a target at index 3 would wrongly come back as "not found." Only after *every* element has been checked can we say the target is absent.

⚠️ **Return the moment you find it.** Continuing the loop after a match wastes time, and the loop would never exit with the correct index. Since we assume unique elements, the first match is the only match.

### Complexity of Linear Search

The cost depends on **where the target sits**, so this is a case where best, average, and worst differ (recall Lecture 1, Section 5):

| Case | Where the target is | Comparisons | Time |
| --- | --- | --- | --- |
| **Best** | at index 0 | 1 | **O(1)** |
| **Average** | equally likely anywhere | about `n/2` | **O(n)** |
| **Worst** | at the last index, or absent | `n` | **O(n)** |

The average is `n/2` comparisons, and dropping the constant `1/2` leaves O(n).

**Space complexity:** **O(1)**. Only the loop variable `i` is used.

### Strengths and Limits

|  | Linear search |
| --- | --- |
| Needs sorted data? | **No**, it works on any array |
| Works on linked lists? | Yes (walk node by node) |
| Cost for large `n` | grows in direct proportion to `n` |

Linear search's strength is that it demands **nothing** from the data. Its weakness is that it **uses nothing** about the data either: even if the array happens to be sorted, it still checks element after element. That is the waste binary search removes.

---

## 3. The Problem with Linear Search on Sorted Data

Suppose someone hands you a **dictionary** and asks you to find the word "Raj".

The linear way is to open page 1 and ask "is Raj here?", then page 2, then page 3, and so on until you find it. This is exactly linear search, and for a 1000-page dictionary it could take up to 1000 page checks.

But nobody searches a dictionary this way. Why not? Because a dictionary has a special property:

> **The words are in sorted order.**
> 

First all the words starting with A, then B, then C, and so on up to Z. That ordering carries **information**, and a smart searcher uses it to skip huge parts of the book.

---

## 4. Binary Search: The Idea Through a Real-Life Example

### What is Binary Search?

**Binary search** is a searching algorithm for a **limited, sorted search space**. It uses the sorted order to throw away **half** of the remaining possibilities with a single comparison.

> **Remember this condition: binary search applies whenever the search space is sorted.**
> 

### The Dictionary Example, Step by Step

You want to find "Raj". Instead of starting at page 1, you **open the dictionary in the middle**.

```
        ┌──────────────────────┬──────────────────────┐
        │   A  B  C  D ... ?   │   ? ... S  T  U  V   │
        │     left half        │      right half      │ 
        └──────────────────────┴──────────────────────┘
                               ↑
                     you open it here (the middle)
```

Say the page you land on has words starting with **S**. Now reason about it:

- The words are sorted, so the **right half** contains words that start with **S or later letters** (S, T, U, ... Z).
- "Raj" starts with **R**, and R comes **before** S.
- Therefore "Raj" **cannot** be in the right half, and you can **ignore the entire right half**.

```
Before:   [ A ........ R ........ S ........ Z ]
                                  ↑ opened here (S)

R comes before S, so Raj is NOT on the right.

After:    [ A ........ R ........ S ]       ← right half discarded
```

Your search space has just been **cut in half** with one look. Now repeat the same thinking on what remains: open the left half in the middle, compare, discard half again, and so on.

```
Search space shrinks like this:

   whole dictionary   →   left half   →   a quarter   →   an eighth   →  ...  →  "Raj"
        1000 pages         500 pages       250 pages        125 pages           found
```

**Real-life analogy (the same idea, in numbers):** The "guess my number" game. Someone thinks of a number from 1 to 100. Asking "is it 1?", "is it 2?", ... is linear search. The smart way is to ask "is it higher than 50?" Every answer eliminates half of the candidates. In about **7 questions** you can pin down any number from 1 to 100.

### The Key Observations

| Observation | Why it matters |
| --- | --- |
| Each step looks at **one** element (the middle) | very little work per step |
| That one comparison tells you **which half** can't contain the target | possible **only** because the data is sorted |
| The search space **halves** every step | the shrinking is what makes it fast |
| You repeat the **same steps** on a smaller space | this repetition is why both a loop and a recursive version exist (Parts 2 and 3) |

⚠️ **Binary search is not limited to arrays.** The dictionary has no array at all. The principle is: *any* search space that is **sorted** (or more generally, where one check lets you rule out half) can use binary search. For now our search space is an array, but you will meet problems later where the "search space" is a range of possible answers instead.

### Why It Needs Sorted Data

Suppose the dictionary were shuffled, and you opened the middle and saw the word "Sam". Would that tell you anything about whether "Raj" is to the left or right? **No.** In a shuffled book, "Raj" could be on any page. The reasoning "R comes before S, so it isn't on the right" **only works because of the sorting**.

```
Sorted:     [ 3 | 4 | 6 | 7 | 9 | 12 | 16 | 17 ]
             looking for 6, middle = 7  →  6 < 7  →  everything right of 7
             is bigger than 7, so 6 can't be there. ✅ safe to discard.

Unsorted:   [ 9 | 3 | 17 | 7 | 6 | 16 | 4 | 12 ]
             looking for 6, middle = 7  →  tells us NOTHING about where 6 is.
             ❌ can't safely discard anything.
```

Recall from Lecture 2 that arrays offer **O(1) random access**: we can jump straight to the middle index by arithmetic. That is the other ingredient. Binary search needs both **sorted order** (to know which half to drop) and **instant access to the middle** (to look at it cheaply). A linked list has no instant access to its middle, which is why binary search is an array-world tool.

---

## 5. Linear Search vs Binary Search: A First Comparison

How many checks, in the worst case, for different sizes?

| Elements (`n`) | Linear search | Binary search |
| --- | --- | --- |
| 8 | 8 | about 4 |
| 1,000 | 1,000 | about 10 |
| 1,000,000 | 1,000,000 | about 20 |
| 1,000,000,000 | 1,000,000,000 | about 30 |

A billion-element sorted array needs only about **30** binary search steps, while linear search could need a billion. That is the O(n) versus O(log n) gap from Lecture 1's growth-rate table, in action. In Part 3 we will prove where this log comes from.

---

## 6. Part 1 Recap

| Concept | Meaning | Key point |
| --- | --- | --- |
| Searching | find whether a target exists, and where | return the index, or `-1` if absent |
| Why `-1` | valid indices are `0 … n-1` | `-1` can never be a real position |
| Linear search | check each element in order | works on **any** array, sorted or not |
| Linear: `return -1` placement | **after** the loop | inside the loop it quits after one element |
| Linear: best case | target at index 0 | O(1) |
| Linear: average / worst | target in the middle / at the end / absent | O(n) |
| Linear: space | only `i` | O(1) |
| Binary search | search a **sorted** space by discarding half each step | the dictionary method |
| Core reasoning | compare with the middle, so one half can't contain the target | only valid because data is sorted |
| Two requirements | sorted order, plus O(1) access to the middle | arrays satisfy both, linked lists don't |
| Search space | the part of the data where the target could still be | shrinks by half every step |

**The single biggest idea from this part:** linear search ignores the order of the data and pays for it with O(n). Binary search **uses** the sorted order: one look at the middle rules out half the remaining possibilities, so the search space halves every step.

# 🎯Part 2: Binary Search on an Array & the Iterative Code

---

## Quick Recap Before We Continue

In Part 1 we saw that linear search ignores the order of the data and costs O(n). Binary search uses the **sorted order**: one look at the middle rules out half of the remaining possibilities, so the search space halves every step. We saw this with a dictionary.

Today we turn that idea into working code on an actual array. We introduce the three pointers (`low`, `high`, `mid`), trace a target that is **found** and a target that is **absent**, see exactly how the search knows when to stop, and write the **iterative version**.

---

## 7. Setting Up: The Array and the Search Space

We'll use this sorted array of 8 elements, with unique values:

```
Index:     0     1     2     3     4     5     6     7
        ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐
arr  =  │  3  │  4  │  6  │  7  │  9  │ 12  │ 16  │ 17  │      n = 8
        └─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┘
```

Just like linear search, the function returns the **index** of the target, or **`-1`** if it is absent.

### How Do We Represent "The Search Space"?

In the dictionary example, the search space was the part of the book where the word could still be. In an array, we mark that part with **two pointers**:

| Pointer | Meaning | Starts at |
| --- | --- | --- |
| `low` | the first index of the search space | `0` |
| `high` | the last index of the search space | `n - 1` |

> **Everything between `low` and `high` (inclusive) is the search space.** Everything outside it has been ruled out.
> 

```
Initially, the whole array is the search space:

 Index:    0     1     2     3     4     5     6     7
        ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐
        │  3  │  4  │  6  │  7  │  9  │ 12  │ 16  │ 17  │
        └─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┘
           ↑                                         ↑
          low                                       high
```

### Splitting the Space: the `mid` Pointer

The dictionary trick was to open the book **in the middle**. For the array we need a third variable, `mid`, which is the middle index of the current search space:

```
mid = (low + high) / 2
```

Because array indices are integers, this is **integer division**, which drops any fractional part:

```
low = 0, high = 7  →  (0 + 7) / 2 = 3.5  →  mid = 3
```

⚠️ **`mid` is an index, not a value.** The middle *element* is `arr[mid]`. Mixing the two up is a very common slip. `mid = 3` means "look at position 3", and `arr[3] = 7` is what sits there.

---

## 8. Walking Through a Search That Succeeds: target = 6

The target is `6`. Every step has the same three moves: compute `mid`, compare `arr[mid]` with the target, and shrink the search space.

**Round 1.**

```
 Index:    0     1     2     3     4     5     6     7
        ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐
        │  3  │  4  │  6  │  7  │  9  │ 12  │ 16  │ 17  │
        └─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┘
           ↑                  ↑                         ↑
          low                mid                       high

mid = (0 + 7) / 2 = 3         arr[3] = 7
Is 7 equal to 6?  No.
Is the target smaller than 7?  Yes (6 < 7).
```

Think about what this tells us. The array is sorted, so everything to the **right** of `7` is **bigger** than 7 and therefore can never be `6`. The whole right part, including `mid` itself, can be thrown away. We do this by moving `high` to **one place before `mid`**:

```
high = mid - 1 = 2

 Index:    0     1     2     3     4     5     6     7
        ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐
        │  3  │  4  │  6  │  ░  │  ░  │  ░  │  ░  │  ░  │     ░ = ruled out
        └─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┘
           ↑           ↑
          low         high
```

We can't literally delete part of the array, but we don't need to. By the rule above ("the search space is everything between `low` and `high`"), moving `high` is enough to shrink it.

**Round 2.**

```
mid = (0 + 2) / 2 = 1         arr[1] = 4
Is 4 equal to 6?  No.
Is the target smaller than 4?  No, the target is bigger (6 > 4).
```

Everything to the **left** of `4` is smaller than 4, so none of it can be `6`. Throw away the left part, including `mid`, by moving `low` to **one place after `mid`**:

```
low = mid + 1 = 2

 Index:    0     1     2     3     4     5     6     7
        ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐
        │  ░  │  ░  │  6  │  ░  │  ░  │  ░  │  ░  │  ░  │
        └─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┘
                       ↑
                   low, high   (the search space is a single element)
```

**Round 3.**

```
mid = (2 + 2) / 2 = 2         arr[2] = 6
Is 6 equal to 6?  YES  →  found at index 2.  Return 2.
```

Three rounds for 8 elements. Linear search would also have needed three checks for this target, since 6 sits at index 2. The real saving appears for targets near the far end, as we'll see in a moment.

⚠️ **Notice Round 3 carefully.** At that point `low == high == 2`: the search space has exactly **one** element, and that single element turned out to be the answer. If our loop had stopped as soon as `low` *equaled* `high`, we would have **skipped checking this last element** and wrongly reported "not found". This detail decides the loop condition, which we come back to in Section 10.

### What if the Target Were 7?

On Round 1, `arr[mid] = arr[3] = 7`, so we would be done immediately. Whenever the middle element is the target, stop right there. The array has unique elements, so there is nothing more to look for.

---

## 9. Walking Through a Search That Fails: target = 13

`13` is **not** in the array. This case shows how binary search decides "it isn't here."

**Round 1.**

```
 Index:    0     1     2     3     4     5     6     7
        ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐
        │  3  │  4  │  6  │  7  │  9  │ 12  │ 16  │ 17  │
        └─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┘
           ↑                 ↑                         ↑
          low               mid                       high

mid = (0 + 7) / 2 = 3         arr[3] = 7
7 ≠ 13, and 13 > 7  →  the target would be on the RIGHT.
Left side (including mid) is ruled out:   low = mid + 1 = 4
```

**Round 2.**

```
 Index:    0     1     2     3     4     5     6     7
        ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐
        │  ░  │  ░  │  ░  │  ░  │  9  │ 12  │ 16  │ 17  │
        └─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┘
                                  ↑     ↑           ↑
                                 low   mid         high

mid = (4 + 7) / 2 = 5         arr[5] = 12
12 ≠ 13, and 13 > 12  →  target would be on the RIGHT again.
low = mid + 1 = 6
```

**Round 3.**

```
 Index:    0     1     2     3     4     5     6     7
        ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐
        │  ░  │  ░  │  ░  │  ░  │  ░  │  ░  │ 16  │ 17  │
        └─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┘
                                              ↑     ↑
                                         low,mid   high

mid = (6 + 7) / 2 = 6         arr[6] = 16
16 ≠ 13, and 13 < 16  →  the target would be on the LEFT.
Right side (including mid) is ruled out:   high = mid - 1 = 5
```

**The search space has vanished.**

```
 Index:    0     1     2     3     4     5     6     7
        ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐
        │  ░  │  ░  │  ░  │  ░  │  ░  │  ░  │  ░  │  ░  │     everything ruled out
        └─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┘
                                        ↑     ↑
                                       high  low        high (5) < low (6)
```

Now `low = 6` and `high = 5`, so **`high` has crossed `low`**. Remember that the search space is "everything between `low` and `high`". If `high` is smaller than `low`, there is **no index between them**: the search space is empty. Every position has been ruled out, so the target cannot be present. We report **`-1`**.

**Real-life analogy:** Playing "guess my number" and narrowing it down: "higher than 12?" yes, "lower than 16?" yes... and now the only candidates left would have to be bigger than 12 *and* smaller than 16 *and* present in the list. There are none, so the number you're hunting for isn't there.

---

## 10. From the Walkthroughs to the Algorithm

Both walkthroughs repeated the same steps. Pull out the pattern:

**What the loop does, round after round:**

1. Compute `mid`.
2. Compare `arr[mid]` with the target. Exactly **one** of three things is true:

| Comparison | Meaning | Action |
| --- | --- | --- |
| `arr[mid] == target` | found it | **return `mid`** |
| `arr[mid] < target` | the target is on the **right** | rule out the left side: **`low = mid + 1`** |
| `arr[mid] > target` | the target is on the **left** | rule out the right side: **`high = mid - 1`** |
1. Go round again with the smaller search space.

**When does the loop stop?** Two possible ways:

- We **found** the target (we returned from inside the loop).
- The search space became **empty** (`low > high`), so we fall out of the loop and return `1`.

### The Loop Condition: `while (low <= high)`

The loop should continue **as long as the search space is non-empty**, which means `low <= high`.

Compare this with the two situations we saw:

| Situation | `low` vs `high` | Search space | Should we keep looping? |
| --- | --- | --- | --- |
| Round 3 of the target-6 search | `low == high == 2` | **one** element (still unchecked) | **Yes** |
| End of the target-13 search | `low = 6 > high = 5` | empty | No |

So the loop must keep going when `low == high`, because a single remaining element still needs to be checked. That is exactly why the condition is **`<=`** and not `<`.

⚠️ **`low < high` would be a bug.** With `<`, the loop would stop the instant `low` equals `high`, never checking the last remaining element. In our target-6 search, that is *precisely* the element holding the answer. The code would return `-1` for a target that is present.

### Why Is It `mid + 1` and `mid - 1`, Not Just `mid`?

We have already checked `arr[mid]` and found it is **not** the target, so `mid` itself is ruled out too. The new boundary goes **one past** it:

```
target is on the right:   low  = mid + 1      (not  low  = mid)
target is on the left:    high = mid - 1      (not  high = mid)
```

⚠️ **Setting `low = mid` or `high = mid` can cause an infinite loop.** Suppose `low = 2`, `high = 3`, so `mid = (2 + 3) / 2 = 2`. If the target is larger than `arr[2]` and you write `low = mid`, then `low` stays `2`, `mid` stays `2`, and the loop repeats forever. Moving to `mid + 1` guarantees the search space shrinks at least a little every round.

### The Iterative Code

"Iterative" means the repeated steps are done with a **loop**:

```cpp
int binarySearch(int arr[], int n, int target) {
    int low = 0;
    int high = n - 1;

    while (low <= high) {                 // search space is non-empty
        int mid = (low + high) / 2;

        if (arr[mid] == target) {
            return mid;                   // found it
        }
        else if (arr[mid] < target) {
            low = mid + 1;                // target is on the right
        }
        else {
            high = mid - 1;               // target is on the left
        }
    }

    return -1;                            // search space exhausted: not present
}
```

The caller uses it just like linear search:

```cpp
int found   = binarySearch(arr, 8, 6);    // 2
int missing = binarySearch(arr, 8, 13);   // -1
```

⚠️ **Why the last branch is a plain `else`.** After ruling out "equal" and "smaller", the only remaining possibility is "larger", so there is no need to write a third condition. The three cases cover everything: equal, `arr[mid]` too small, `arr[mid]` too big.

⚠️ **`return -1` goes after the loop, just like in linear search.** Reaching that line means the `while` condition became false, so `low > high`, so the search space is empty. Putting `return -1` inside the loop would make the search give up after one round.

⚠️ **The array must be sorted in ascending order.** The code reads "`arr[mid] < target` means go right" as a fact about ascending data. On an unsorted array, the function runs without any error and returns confidently wrong answers, which makes this bug hard to notice.

### Full Working Example

```cpp
#include <iostream>
using namespace std;

int binarySearch(int arr[], int n, int target) {
    int low = 0;
    int high = n - 1;

    while (low <= high) {
        int mid = (low + high) / 2;

        if (arr[mid] == target) {
            return mid;
        }
        else if (arr[mid] < target) {
            low = mid + 1;
        }
        else {
            high = mid - 1;
        }
    }

    return -1;
}

int main() {
    int arr[] = {3, 4, 6, 7, 9, 12, 16, 17};
    int n = sizeof(arr) / sizeof(arr[0]);

    cout << binarySearch(arr, n, 6)  << endl;    // 2
    cout << binarySearch(arr, n, 7)  << endl;    // 3
    cout << binarySearch(arr, n, 17) << endl;    // 7
    cout << binarySearch(arr, n, 13) << endl;    // -1
    cout << binarySearch(arr, n, 1)  << endl;    // -1  (smaller than everything)
    cout << binarySearch(arr, n, 99) << endl;    // -1  (bigger than everything)

    return 0;
}
```

### Where the Saving Really Shows: target = 17

Linear search on this array for `17` needs all 8 comparisons, since it sits at the last index. Binary search:

```
Round 1:  low=0, high=7,  mid=3,  arr[3]=7   → 17 > 7   → low = 4
Round 2:  low=4, high=7,  mid=5,  arr[5]=12  → 17 > 12  → low = 6
Round 3:  low=6, high=7,  mid=6,  arr[6]=16  → 17 > 16  → low = 7
Round 4:  low=7, high=7,  mid=7,  arr[7]=17  → FOUND, return 7

4 rounds instead of 8, and the gap widens enormously as n grows.
```

---

## 11. Part 2 Recap

| Concept | Meaning | Key point |
| --- | --- | --- |
| Search space | the part of the array where the target could still be | everything between `low` and `high` |
| `low`, `high` | first and last index of the search space | start at `0` and `n - 1` |
| `mid` | middle index: `(low + high) / 2` | integer division; `mid` is an **index**, `arr[mid]` is the value |
| Three outcomes per round | equal / smaller / larger | the `else` covers "larger" |
| `arr[mid] < target` | target is on the right | `low = mid + 1` |
| `arr[mid] > target` | target is on the left | `high = mid - 1` |
| Why `mid ± 1` | `mid` has already been checked and ruled out | `low = mid` can loop forever |
| Loop condition | `while (low <= high)` | `<=` so the last single element still gets checked |
| `low < high` | a bug | skips the final remaining element |
| Not found | `high` crosses `low` (`low > high`) | the search space is empty |
| `return -1` | after the loop | reaching it means the space was exhausted |
| Precondition | the array must be sorted ascending | otherwise answers are silently wrong |

**The single biggest idea from this part:** the whole algorithm is one repeated move: *check the middle, throw away the half that can't contain the target, and narrow `low` or `high` accordingly*. The search ends either when the middle is the target, or when `high` crosses `low` and no search space remains.

# 🎯Part 3: Recursive Binary Search, Complexity & the Overflow Trap

---

## Quick Recap Before We Continue

In Part 2 we turned the dictionary idea into the **iterative** algorithm. The search space is everything between `low` and `high`, `mid` is the middle index, and each round either finds the target, moves `low = mid + 1`, or moves `high = mid - 1`. The loop runs `while (low <= high)`, and falling out of it means the search space is empty, so we return `-1`.

Today we finish the topic. We rewrite the same algorithm **recursively**, **prove why it is O(log n)**, meet a subtle bug called **integer overflow** that matters in interviews, and compare linear and binary search side by side.

---

## 12. Recursive Binary Search

### Why Recursion Fits Here

Recall the pattern from Part 2: every round does the **same three steps** (compute `mid`, compare, shrink the search space). The only thing that changes between rounds is the search space, meaning the pair `(low, high)`.

Whenever the same task repeats on a smaller version of the same problem, we can express it as a function that **calls itself** with the smaller problem. The loop's job is handed over to the call stack.

```
Iterative:                          Recursive:

while (low <= high) {               search(arr, low, high, target) {
    compute mid                         if space is empty → return -1
    compare                             compute mid
    shrink the space                    compare
}                                       shrink the space  →  call search(...) again
                                    }
```

**Real-life analogy:** A manager says, "Check the middle file. If it's not the one, hand the remaining half of the drawer to an assistant and ask them to do exactly the same job." The assistant does the same thing, passing a smaller piece along, until someone finds the file or has no files left.

### Building the Recursive Function

Every recursive function needs a **base case** (when to stop) and a **recursive case** (the smaller call). We write it piece by piece.

**The function's inputs.** Besides the array and target, each call needs to know its own search space, so `low` and `high` become parameters:

```cpp
int search(int arr[], int low, int high, int target)
```

**Piece 1: the base case.** When does the recursion stop without an answer? When the search space is empty, which is exactly `low > high` (the same condition that ended the loop in Part 2):

```cpp
if (low > high) return -1;        // search space exhausted: not present
```

**Piece 2: find the middle.**

```cpp
int mid = (low + high) / 2;
```

**Piece 3: the three outcomes.**

| Comparison | Action |
| --- | --- |
| `arr[mid] == target` | found it: **`return mid`** |
| `arr[mid] < target` | the target is on the right: search the right half, with `low = mid + 1` |
| `arr[mid] > target` | the target is on the left: search the left half, with `high = mid - 1` |

In the iterative version we *updated* `low` or `high` and let the loop go around. Here, we instead **call the function again** with the new boundaries, and **return** whatever that call returns.

### The Recursive Code

```cpp
int search(int arr[], int low, int high, int target) {
    if (low > high) {
        return -1;                                    // base case: empty space
    }

    int mid = (low + high) / 2;

    if (arr[mid] == target) {
        return mid;                                   // found it
    }
    else if (arr[mid] < target) {
        return search(arr, mid + 1, high, target);    // go right
    }
    else {
        return search(arr, low, mid - 1, target);     // go left
    }
}

int binarySearch(int arr[], int n, int target) {      // same signature as the iterative one
    return search(arr, 0, n - 1, target);             // start with the whole array
}
```

The wrapper `binarySearch` keeps the **same signature** as the iterative version from Part 2, so callers don't care which one is underneath. It supplies the initial search space (`0` to `n - 1`).

⚠️ **`return` before each recursive call is essential.** Writing `search(arr, mid + 1, high, target);` *without* `return` runs the recursive call, then **throws its answer away** and falls off the end of the function with no return value. The correct answer would be computed and then lost. Each recursive call's result must travel back up the chain, so each one needs `return`.

⚠️ **The base case must come first.** If you compute `arr[mid]` before checking `low > high`, then on an empty search space `mid` could point outside the array. The empty-space check comes before any array access.

⚠️ **`mid + 1` and `mid - 1` matter here for the same reason as before.** Passing `mid` instead would give the next call the *same* search space again, so the recursion would never shrink and would never reach the base case (infinite recursion, ending in a crash from stack overflow).

### Tracing the Recursion: target = 13 (absent)

Using the same array as Part 2:

```
Index:     0     1     2     3     4     5     6     7
        ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐
arr  =  │  3  │  4  │  6  │  7  │  9  │ 12  │ 16  │ 17  │
        └─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┘
```

Each call waits for the call below it to finish. Here is the chain of calls going **down**:

```
search(low=0, high=7)
   mid = 3, arr[3] = 7.   7 < 13  → go right
   └─► search(low=4, high=7)
          mid = 5, arr[5] = 12.   12 < 13  → go right
          └─► search(low=6, high=7)
                 mid = 6, arr[6] = 16.   16 > 13  → go left
                 └─► search(low=6, high=5)
                        low > high  →  BASE CASE  →  return -1
```

Now the answer travels back **up**, one call at a time:

```
search(low=6, high=5)   returns -1
        │
        ▼
search(low=6, high=7)   gets -1 from the call below → returns -1
        │
        ▼
search(low=4, high=7)   gets -1 from the call below → returns -1
        │
        ▼
search(low=0, high=7)   gets -1 from the call below → returns -1
        │
        ▼
binarySearch(...)       returns -1   ✅ target is not present
```

This chain is why `return` in front of every recursive call is essential: each level hands the value it received to the level above it.

The call chain visits the **same sequence of search spaces** as the iterative version did in Part 2, Section 9: `(0,7)` then `(4,7)`, then `(6,7)`, then the empty space `(6,5)`. Only the bookkeeping differs: a loop reuses the same variables, while recursion creates a fresh set of `low`/`high` for each call.

### Iterative vs Recursive: The Space Difference

Recall from Lecture 1, Section 6, that **recursion costs space**, because each call sits on the call stack until it returns.

```
Deepest point of the trace above:

  search(0,7)       ← frame 1, waiting
  search(4,7)       ← frame 2, waiting
  search(6,7)       ← frame 3, waiting
  search(6,5)       ← frame 4, running (base case)
```

| Version | Time | Extra space |
| --- | --- | --- |
| Iterative | O(log n) | **O(1)** (just `low`, `high`, `mid`) |
| Recursive | O(log n) | **O(log n)** (one stack frame per halving) |

Both do the same comparisons. The recursive version is a little more elegant, while the iterative version is leaner in memory. Because the depth is only about log n (roughly 30 even for a billion elements), the extra space is tiny in practice, but it is still not O(1).

---

## 13. Time Complexity of Binary Search: The Proof

We claimed O(log n) in Part 1. Now let's see exactly where it comes from.

### Step 1: What Does One Round Cost?

In each round we compute `mid`, make a comparison, and move one pointer. These are a **fixed number of simple operations, with no inner loop**. So **one round costs O(1)**.

The total time is therefore:

```
Total time  =  (number of rounds)  ×  O(1)
```

So everything depends on **how many rounds** the algorithm can take.

### Step 2: How Many Times Can the Search Space Be Halved?

Each round throws away about half of the search space. Start with a search space of **32** elements and look at the worst case, where we keep going until a single element is left:

```
Round 1:   32 elements
              │  halve
Round 2:   16 elements
              │  halve
Round 3:    8 elements
              │  halve
Round 4:    4 elements
              │  halve
Round 5:    2 elements
              │  halve
Round 6:    1 element        → last check, then the space is empty
```

The search space went `32 → 16 → 8 → 4 → 2 → 1`. That is **5 halvings**, plus one final check on the last remaining element.

⚠️ **"Half" is an approximation.** With an even-sized space, `mid` can leave one side slightly bigger than the other. For example, 8 elements split into 3 on one side (plus `mid`) and 4 on the other. The larger leftover side is **at most** half, rounded down, plus a bit. This detail never changes the order of growth. The space still shrinks by roughly half each round.

### Step 3: Connect Halvings to a Logarithm

Ask: how many times can you halve a number `n` before reaching 1? Work backward from 32:

```
32 = 2 × 2 × 2 × 2 × 2 = 2⁵      →   5 halvings
64 = 2⁶                          →   6 halvings
128 = 2⁷                         →   7 halvings
```

And by the definition of a logarithm, the power you raise 2 to in order to get `n` is exactly `log₂ n`:

```
n = 32 :   log₂ 32 = log₂ (2⁵) = 5
n = 64 :   log₂ 64 = log₂ (2⁶) = 6
```

So the number of halvings is **`log₂ n`**, and with the one extra final check, the worst case is about **`log₂ n + 1`** rounds.

### Step 4: Put It Together

```
Total time  =  (about log₂ n + 1 rounds)  ×  O(1)  =  O(log n)
```

(In Big O we drop the constant `+1` and the base of the logarithm, using the simplification rules from Lecture 1.)

| `n` | Halvings (`log₂ n`) | Worst-case rounds (about) |
| --- | --- | --- |
| 8 | 3 | 4 |
| 32 | 5 | 6 |
| 1,024 | 10 | 11 |
| 1,000,000 | about 20 | about 21 |
| 1,000,000,000 | about 30 | about 31 |

**Real-life analogy:** Folding a sheet of paper in half repeatedly. A stack of 1,000 pages shrinks to a single page in only about 10 folds. Binary search "folds" the search space in half each round, so even a huge space collapses quickly. Doubling `n` adds only **one** more round.

### The Three Cases

| Case | When it happens | Time |
| --- | --- | --- |
| **Best** | the target is exactly the first `mid` | **O(1)** |
| **Average** | the target is found after a few halvings | **O(log n)** |
| **Worst** | the target is found in the very last round, or is absent | **O(log n)** |

**Space complexity:** **O(1)** for the iterative version, **O(log n)** for the recursive version (call stack).

⚠️ **This complexity does not include sorting.** If the array starts out unsorted, you must sort it first (at least O(n²) with the sorts from Lecture 3), and that cost may outweigh the savings for a one-time search. Sorting first only pays off when you search the **same data many times**, since the sorting cost is paid once but the O(log n) saving is repeated on every search.

---

## 14. The Integer Overflow Trap

### The Problem with `(low + high) / 2`

So far we computed the middle as:

```cpp
int mid = (low + high) / 2;
```

For arrays this is safe, since array sizes are limited to values like 10⁵ or 10⁷. But recall from Part 1 that binary search is **not limited to arrays**. The "search space" can be a huge range of numbers, for example, searching for an answer somewhere between `0` and the **largest value an `int` can hold**.

A 32-bit `int` holds values up to **2,147,483,647** (about 2.1 billion), written `INT_MAX`. Suppose the search space has narrowed down to a spot near the top of that range:

```
low  = 2,000,000,000
high = 2,000,000,000          (both are valid ints)

low + high = 4,000,000,000    ← bigger than INT_MAX (2,147,483,647)
```

The sum **does not fit** in an `int`. It overflows, wrapping around to a garbage (often negative) value. In C++, signed integer overflow is actually **undefined behavior**. `mid` becomes nonsense, and the search either crashes or returns wrong answers, even though the algorithm is logically correct.

```
Intended:    mid = 4,000,000,000 / 2 = 2,000,000,000
Actually:    low + high overflows first → garbage → garbage / 2 → invalid mid ❌
```

**Real-life analogy:** An odometer with only 6 digits. When it reaches 999,999 and you add one more kilometer, it rolls back to 000,000. The car didn't un-drive those kilometers, but the display can no longer represent the true total. An `int` that is too small for a sum behaves the same way.

### Fix 1: Use a Bigger Type

Store `low`, `high`, and `mid` as `long long`, which is 64 bits wide and holds values up to about 9 × 10¹⁸:

```cpp
long long low = 0, high = n - 1;
long long mid = (low + high) / 2;
```

### Fix 2: Rearrange the Formula

If you are sure the search space lies within `0` to `INT_MAX`, keep `int` and compute the middle this way instead:

```cpp
int mid = low + (high - low) / 2;
```

**Why is this the same value?** Simple algebra:

```
low + (high − low) / 2
   =  (2·low + high − low) / 2         (put everything over a common denominator)
   =  (low + high) / 2                 ✅ identical
```

**Why is it safe?** The new expression never adds two big numbers:

- `high - low` is a **difference**, which is never bigger than `high` itself (for non-negative values), so it can't overflow.
- Dividing by 2 makes it even smaller.
- Adding that small amount to `low` lands at a value between `low` and `high`, which always fits.

```
Same example:   low = high = 2,000,000,000

high − low       = 0
(high − low) / 2 = 0
low + 0          = 2,000,000,000     ✅ correct, no overflow anywhere
```

⚠️ **Fix 2 only works if the space stays between `0` and `INT_MAX`.** If your values could go beyond that range (for example, negative numbers close to `INT_MIN`), `high - low` itself could overflow, and you would need `long long` instead.

⚠️ **For ordinary array problems, both forms behave identically.** But `low + (high - low) / 2` is a very common interview expectation. Interviewers often ask, "what could go wrong with `(low + high) / 2`?", and the overflow answer shows you understand it. In the final, safe version of the code, use the rearranged formula by default.

### The Safe Version of the Code

```cpp
int binarySearch(int arr[], int n, int target) {
    int low = 0;
    int high = n - 1;

    while (low <= high) {
        int mid = low + (high - low) / 2;     // overflow-safe middle

        if (arr[mid] == target) {
            return mid;
        }
        else if (arr[mid] < target) {
            low = mid + 1;
        }
        else {
            high = mid - 1;
        }
    }

    return -1;
}
```

The same change applies inside the recursive version: replace its `mid` line with `int mid = low + (high - low) / 2;`.

---

## 15. Linear Search vs Binary Search: Full Comparison

| Feature | Linear Search | Binary Search |
| --- | --- | --- |
| Idea | check every element in order | check the middle, discard half |
| Data must be sorted? | **No** | **Yes** |
| Needs O(1) access to the middle? | No (works on linked lists too) | **Yes** (array-style access) |
| Best case | O(1) | O(1) |
| Average case | O(n) | O(log n) |
| Worst case | O(n) | **O(log n)** |
| Space (iterative) | O(1) | O(1) |
| Space (recursive) | n/a | O(log n) |
| Works on a linked list? | ✅ | ❌ not efficiently |
| Code complexity | very simple | simple, but easy to get boundaries wrong |
| Checks for 1,000,000 elements (worst) | 1,000,000 | about 20 |

### Which One to Use?

| Situation | Best choice | Reason |
| --- | --- | --- |
| Data is unsorted, one-time search | **Linear search** | sorting first would cost more than the search saves |
| Data is sorted | **Binary search** | O(log n) vs O(n) |
| Data is unsorted but searched many times | **Sort once, then binary search** | the sorting cost is paid once, the savings repeat |
| Data lives in a linked list | **Linear search** | no O(1) access to the middle |
| Very small array (a handful of elements) | either | the difference is negligible |

---

## 16. Binary Search: Common Traps (Checklist)

| Pitfall | Where it bites | The fix |
| --- | --- | --- |
| Using `while (low < high)` | iterative loop | use `low <= high`, so the last single element is checked |
| Setting `low = mid` or `high = mid` | pointer updates | use `mid + 1` / `mid - 1`, or the loop/recursion may never end |
| Running it on unsorted data | any input | sort first, or use linear search |
| Forgetting `return` before the recursive call | recursive version | every recursive call needs `return`, or its answer is lost |
| Base case after the array access | recursive version | check `low > high` first |
| `(low + high) / 2` on huge ranges | mid calculation | use `low + (high - low) / 2` (or `long long`) |
| Putting `return -1` inside the loop | iterative version | it goes after the loop |
| Confusing `mid` with `arr[mid]` | comparisons | `mid` is the index, `arr[mid]` is the value |

---

## 17. Full Working Example (Both Versions)

```cpp
#include <iostream>
using namespace std;

int linearSearch(int arr[], int n, int target) {
    for (int i = 0; i < n; i++) {
        if (arr[i] == target) return i;
    }
    return -1;
}

int binarySearchIterative(int arr[], int n, int target) {
    int low = 0, high = n - 1;
    while (low <= high) {
        int mid = low + (high - low) / 2;
        if (arr[mid] == target)      return mid;
        else if (arr[mid] < target)  low = mid + 1;
        else                         high = mid - 1;
    }
    return -1;
}

int search(int arr[], int low, int high, int target) {
    if (low > high) return -1;
    int mid = low + (high - low) / 2;
    if (arr[mid] == target)      return mid;
    else if (arr[mid] < target)  return search(arr, mid + 1, high, target);
    else                         return search(arr, low, mid - 1, target);
}

int binarySearchRecursive(int arr[], int n, int target) {
    return search(arr, 0, n - 1, target);
}

int main() {
    int arr[] = {3, 4, 6, 7, 9, 12, 16, 17};
    int n = sizeof(arr) / sizeof(arr[0]);

    cout << linearSearch(arr, n, 16) << endl;             // 6
    cout << binarySearchIterative(arr, n, 16) << endl;    // 6
    cout << binarySearchRecursive(arr, n, 16) << endl;    // 6

    cout << binarySearchIterative(arr, n, 13) << endl;    // -1
    cout << binarySearchRecursive(arr, n, 13) << endl;    // -1

    return 0;
}
```

---

## 18. Part 3 Recap

| Concept | Meaning | Key point |
| --- | --- | --- |
| Why recursion fits | each round repeats the same steps on a smaller space | `(low, high)` become the function's parameters |
| Base case | `low > high` | empty search space → `return -1` |
| Recursive calls | `search(mid + 1, high)` for right, `search(low, mid - 1)` for left | always `return` the call's result |
| Call trace | the chain visits the same search spaces as the loop | answer travels back up through every `return` |
| Space | iterative O(1), recursive **O(log n)** | recursion stacks one frame per halving |
| One round costs | O(1) | no inner loop |
| Rounds needed | about `log₂ n + 1` | halving `n` repeatedly reaches 1 in `log₂ n` steps |
| Time complexity | best O(1), average/worst **O(log n)** | doubling `n` adds just one round |
| Sorting cost | not included | worth it only if you search the same data many times |
| Overflow | `low + high` can exceed `INT_MAX` | `int` wraps to garbage (undefined behavior in C++) |
| Fix 1 | use `long long` | wider type |
| Fix 2 | `mid = low + (high - low) / 2` | algebraically identical, never adds two large values |
| Fix 2 limit | valid when the space stays within `0 … INT_MAX` | otherwise use `long long` |
| Linear vs binary | O(n) on any data vs O(log n) on sorted data | choose by whether data is sorted and how often you search |

**The single biggest idea from this lecture:** linear search takes the data as it comes, while binary search **exploits sorted order** to halve the search space every round. That one idea turns O(n) into O(log n), the difference between a million checks and about twenty. It comes with two conditions (sorted data, instant access to the middle) and one hidden trap (overflow in the midpoint formula).

---