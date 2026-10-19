# Lecture 3: Sorting (Bubble, Selection & Insertion Sort)

Date: August 17, 2026
Status: Done

[Sorting - Part 1 | Selection Sort, Bubble Sort, Insertion Sort | Strivers A2Z DSA Course](https://www.youtube.com/watch?v=HGk_ypEuS24)

# 🎯Part 1: Sorting Basics & Selection Sort

## Quick Recap Before We Begin

In Lecture 2 we saw that arrays are superb at reading by index (O(1)) but costly at structural change (O(n)). We also previewed sorting as an array operation and noted that three simple sorts (bubble, selection, insertion) all land at O(n²) in the worst case. Today we open those three up and build each one from scratch.

---

## 1. What is Sorting?

**Sorting** means rearranging the elements of an array into a defined order, usually **ascending** (smallest to largest) or **descending**.

Why bother? Recall from Lecture 2: binary search needs sorted data to run in O(log n) instead of O(n). Sorted data also makes it easy to find duplicates, minimums, maximums, and medians. Sorting also comes up often in placement tests and interviews, where "write the code for insertion sort" is a common question.

All three algorithms in this lecture follow one core principle: **repeatedly make the array a little more sorted until it is fully sorted**. What differs is *how* each one makes progress:

| Algorithm | The core move |
| --- | --- |
| **Selection Sort** | **select** the minimum and place it at the front |
| **Bubble Sort** | push the **maximum** to the back using **adjacent swaps** |
| **Insertion Sort** | take one element and **insert** it into its correct position |

**Real-life analogy:** Arranging a class by height for a photo. A teacher could (a) repeatedly look at the *whole* line, pick the shortest student, and send them to the front (selection), (b) walk down the line swapping any neighboring pair that is in the wrong order, until the tallest ends up at the back (bubble), or (c) let students join one at a time and have each new student slide into the right spot among those already arranged (insertion). The same goal can be reached in all three ways, just at different costs.

### Vocabulary for Comparing Sorting Algorithms

We'll use these terms to compare the three algorithms in Part 3. Here is what each one means:

| Term | Meaning |
| --- | --- |
| **In-place** | sorts using only O(1) extra memory, by rearranging the array itself |
| **Stable** | equal elements keep their original relative order after sorting |
| **Adaptive** | runs faster when the input is already (nearly) sorted |
| **Comparison count vs swap count** | two separate costs, and an algorithm can be cheap in one and expensive in the other |

All three algorithms in this lecture are **in-place**. Whether each is stable or adaptive is different, and we'll see why as we go.

---

## 2. Selection Sort

### The Idea

The name tells you the algorithm: **select the minimum**. Look at the entire unsorted part of the array, find the smallest element, and **swap** it into the first unsorted position. Repeat on what remains.

**Real-life analogy:** The teacher scans the *entire* line, picks the shortest student, and sends them to the front. The student who was standing at the front takes the shortest student's old spot (a swap, not a long shuffle). Now the front student is permanently in place, so the teacher repeats the scan on the rest of the line only.

### Walking Through the Algorithm

We'll sort this array of 6 elements:

```
Index:     0     1     2     3     4     5
        ┌─────┬─────┬─────┬─────┬─────┬─────┐
arr  =  │ 13  │ 46  │ 24  │ 52  │ 20  │  9  │
        └─────┴─────┴─────┴─────┴─────┴─────┘
```

In each step, the part to the left of the `|` marker is sorted and final. The part to the right is still unsorted.

**Step 1: find the minimum in the whole array (index 0 to 5).**

```
        [ 13 | 46 | 24 | 52 | 20 |  9 ]
                                    ↑ minimum is 9, at index 5

Swap arr[0] and arr[5]  (13 goes to where 9 was):

        [  9 | 46 | 24 | 52 | 20 | 13 ]
         ✅ sorted
```

**Step 2: find the minimum in the unsorted part (index 1 to 5).**

```
        [  9 | 46 | 24 | 52 | 20 | 13 ]
                                    ↑ minimum is 13, at index 5

Swap arr[1] and arr[5]  (46 goes to where 13 was):

        [  9 | 13 | 24 | 52 | 20 | 46 ]
         ✅ sorted (2 elements)
```

**Step 3: find the minimum in the unsorted part (index 2 to 5).**

```
        [  9 | 13 | 24 | 52 | 20 | 46 ]
                              ↑ minimum is 20, at index 4

Swap arr[2] and arr[4]  (24 goes to where 20 was):

        [  9 | 13 | 20 | 52 | 24 | 46 ]
         ✅ sorted (3 elements)
```

**Step 4: find the minimum in the unsorted part (index 3 to 5).**

```
        [  9 | 13 | 20 | 52 | 24 | 46 ]
                              ↑ minimum is 24, at index 4

Swap arr[3] and arr[4]  (52 goes to where 24 was):

        [  9 | 13 | 20 | 24 | 52 | 46 ]
         ✅ sorted (4 elements)
```

**Step 5: find the minimum in the unsorted part (index 4 to 5).**

```
        [  9 | 13 | 20 | 24 | 52 | 46 ]
                                    ↑ minimum is 46, at index 5

Swap arr[4] and arr[5]  (52 goes to where 46 was):

        [  9 | 13 | 20 | 24 | 46 | 52 ]
         ✅ fully sorted
```

**Why only 5 steps for 6 elements?** After the first five elements are in their correct places, the last remaining element has nowhere else to be. A single element is always sorted. So for `n` elements, we need only **`n − 1` steps**.

### Turning the Idea into Code: Observing the Pattern

To convert the idea into code, we need to spot the pattern in *where the swap happens* and *where we search for the minimum*:

```
Step 1:  swap position = 0,   search range = index 0 to n-1
Step 2:  swap position = 1,   search range = index 1 to n-1
Step 3:  swap position = 2,   search range = index 2 to n-1
Step 4:  swap position = 3,   search range = index 3 to n-1
   ...
Last:    swap position = n-2, search range = index n-2 to n-1
```

Two observations fall out of this table:

1. The **swap position** goes `0, 1, 2, ..., n-2`. This is our **outer loop**, with `i` running from `0` to `n - 2`. It stops at `n - 2` because the final element needs no step of its own.
2. In each step, the **search range starts at `i`** and always ends at `n - 1`. Searching a range means an **inner loop**, with `j` running from `i` to `n - 1`.

### Finding the Minimum Inside the Inner Loop

How do we find the minimum of a range? Start with an assumption, then challenge it as you scan:

1. Assume the **first element of the range** (index `i`) is the minimum. Remember it by **index**, in a variable `minIndex = i`.
2. Scan the rest of the range. Whenever you meet an element **smaller than `arr[minIndex]`**, update `minIndex` to that element's index.
3. When the scan finishes, `minIndex` holds the position of the true minimum.

```
Scanning for the minimum in the full array (Step 1):

arr = [ 13 | 46 | 24 | 52 | 20 |  9 ]       minIndex starts at 0 (value 13)

j=1: is 46 < arr[minIndex]=13?  No.    minIndex stays 0
j=2: is 24 < 13?                No.    minIndex stays 0
j=3: is 52 < 13?                No.    minIndex stays 0
j=4: is 20 < 13?                No.    minIndex stays 0
j=5: is  9 < 13?                YES.   minIndex becomes 5

Scan finished → minimum is at index 5 → swap arr[0] with arr[5]
```

We track the **index**, not the value, because the swap needs to know *where* the minimum lives.

### How to Swap Two Values

To swap `arr[i]` and `arr[minIndex]`, use a **temporary variable**, a third cup to hold one value while the other moves:

```
Goal: swap  arr[i] = 15   and   arr[minIndex] = 12

   temp = arr[minIndex]      →  temp = 12          (save the one about to be overwritten)
   arr[minIndex] = arr[i]    →  arr[minIndex] = 15 (overwrite it with the other value)
   arr[i] = temp             →  arr[i] = 12        (put the saved value into its new home)

Result:  arr[i] = 12,  arr[minIndex] = 15  ✅
```

**Real-life analogy:** Two people want to swap seats, but they can't both stand up in the same instant. One steps aside into the aisle (temp), the other takes the empty seat, and the first person then sits in the seat just vacated.

⚠️ **Without `temp`, a swap fails.** Writing `arr[i] = arr[minIndex]; arr[minIndex] = arr[i];` makes **both** positions hold the same value, because the first line overwrites `arr[i]` before its old value was saved anywhere.

### The Code

```cpp
void selectionSort(int arr[], int n) {
    for (int i = 0; i <= n - 2; i++) {          // outer loop: swap position
        int minIndex = i;                        // assume arr[i] is the minimum

        for (int j = i + 1; j <= n - 1; j++) {   // inner loop: scan the rest
            if (arr[j] < arr[minIndex]) {
                minIndex = j;                    // found a smaller one: remember where
            }
        }

        int temp = arr[minIndex];                // swap the minimum into position i
        arr[minIndex] = arr[i];
        arr[i] = temp;
    }
}
```

The caller just passes the array and its size, and the array is sorted **in place**, so nothing needs to be returned:

```cpp
selectionSort(arr, n);
```

⚠️ **Outer loop bound: `i <= n - 2`, not `n - 1`.** Running it to `n - 1` isn't wrong in result, but it does a pointless extra pass where the "range" is a single element that is trivially the minimum. Stopping at `n - 2` matches the `n - 1` steps we derived above.

⚠️ **Inner loop starting point: `i + 1` is enough.** `minIndex` already starts at `i`, so comparing `arr[i]` against itself on the first inner iteration can never change anything. Starting the inner loop at `j = i` is harmless but wastes one comparison per pass.

⚠️ **Track the index, not the value.** If you store `minValue = arr[j]` instead of `minIndex = j`, you'll know *what* the smallest value is but not *where* it sits, so you can't swap it into place.

### Full Working Example

```cpp
#include <iostream>
using namespace std;

void selectionSort(int arr[], int n) {
    for (int i = 0; i <= n - 2; i++) {
        int minIndex = i;
        for (int j = i + 1; j <= n - 1; j++) {
            if (arr[j] < arr[minIndex]) {
                minIndex = j;
            }
        }
        int temp = arr[minIndex];
        arr[minIndex] = arr[i];
        arr[i] = temp;
    }
}

void printArray(int arr[], int n) {
    for (int i = 0; i < n; i++) {
        cout << arr[i] << " ";
    }
    cout << endl;
}

int main() {
    int arr[] = {13, 46, 24, 52, 20, 9};
    int n = sizeof(arr) / sizeof(arr[0]);

    selectionSort(arr, n);
    printArray(arr, n);      // 9 13 20 24 46 52

    return 0;
}
```

### Time Complexity of Selection Sort

Count how many times the inner loop's comparison runs in each pass:

```
Pass for i = 0   →  compares against the remaining n-1 elements
Pass for i = 1   →  n-2 comparisons
Pass for i = 2   →  n-3 comparisons
   ...
Pass for i = n-2 →  1 comparison

Total comparisons = (n-1) + (n-2) + ... + 2 + 1
                  = n(n-1)/2
                  = (n² − n) / 2
```

This is the **sum of the first (n−1) natural numbers**, the same series pattern we met in Lecture 1 (Pattern 4, the dependent inner loop). Apply the simplification rules from Lecture 1: drop the constant `1/2` and the lower-order term `n`, leaving **O(n²)**.

(Counting the inner loop as running over `i … n−1` instead gives `n(n+1)/2`, which is a slightly different count but the same O(n²) after simplification.)

**The important detail:** Selection sort's inner loop **always scans the entire remaining range**, no matter what the data looks like. Finding the minimum requires looking at *every* remaining element, since any of them could be the smallest. There's no way to stop early. So:

| Case | Time Complexity | Why |
| --- | --- | --- |
| **Best** | O(n²) | even a sorted array still needs every scan to confirm the minimum |
| **Average** | O(n²) | same scans |
| **Worst** | O(n²) | same scans |

**Space complexity:** **O(1)**. Only `i`, `j`, `minIndex`, and `temp` are used, and the array is rearranged in place.

### Swaps vs Comparisons: A Selection Sort Specialty

Selection sort does **O(n²) comparisons** but at most **`n − 1` swaps**, since each outer-loop pass performs exactly one swap. Most other simple sorts (including bubble sort, coming in Part 2) can perform up to O(n²) swaps.

**Real-life analogy:** If moving each item is expensive (say, the "elements" are heavy boxes) while just *looking* at them is cheap, selection sort's habit of doing a lot of looking and very little moving is a genuine advantage.

⚠️ **Selection sort is not stable.** Because it swaps the minimum across a long distance, it can jump an element past an equal one. For example, sorting `[3a, 3b, 1]`: the first pass swaps `1` with `3a`, giving `[1, 3b, 3a]`, so the two equal `3`s have changed their relative order. This doesn't matter for plain integers but matters when elements carry extra data (like student records sorted by marks).

---

## 3. Part 1 Recap

| Concept | Meaning | Key point |
| --- | --- | --- |
| Sorting | rearranging elements into a defined order | enables binary search and more |
| In-place | O(1) extra memory | all three sorts today are in-place |
| Stable | equal elements keep their original relative order | selection sort is **not** stable |
| Selection sort idea | **select** the minimum of the unsorted part and swap it to the front | one swap per pass |
| Outer loop | `i` from `0` to `n − 2` | `n − 1` passes, since one element is always sorted |
| Inner loop | `j` from `i + 1` to `n − 1` | always scans the whole remaining range |
| `minIndex` | index of the smallest element seen so far in this pass | track the **index**, not the value |
| Swap | three assignments through a `temp` variable | without `temp`, both slots get the same value |
| Time complexity | **O(n²)** best, average, and worst | no early exit is possible |
| Space complexity | **O(1)** | in-place |
| Swap count | at most `n − 1` | selection sort's special strength |

**The single biggest idea from this part:** selection sort **repeatedly finds the minimum and places it**. That means it always does the same amount of scanning regardless of the input, which makes it O(n²) in every case but economical in swaps.

# 🎯Part 2: Bubble Sort

---

## Quick Recap Before We Continue

In Part 1 we covered the vocabulary of sorting (in-place, stable, adaptive) and built **Selection Sort**: scan the whole unsorted range, **select** the minimum, swap it to the front. It was O(n²) in every case, because the scan can never stop early, but it needed at most `n − 1` swaps.

Today's algorithm flips the direction. Instead of pulling the **minimum to the front**, Bubble Sort pushes the **maximum to the back**, and it does so using only **adjacent swaps**.

---

## 3. Bubble Sort

### The Idea

Walk through the array comparing each **pair of neighbors**. If a pair is in the wrong order (the left one is bigger), swap them. By the time you reach the end of one full walk, the **largest element has "bubbled up" to the last position**, and it is now in its final place. Repeat the walk on everything before it.

**Real-life analogy:** Students standing in a line, and the teacher walks along it once, looking at each neighboring pair. Whenever a taller student stands *before* a shorter one, the teacher makes them swap places. After one trip down the line, the tallest student has been carried all the way to the end, like a bubble rising to the surface. The teacher then repeats the walk, but can stop one student earlier each time, because the back of the line is already settled.

### Walking Through the Algorithm

We'll use the same array as Part 1, so you can compare the two algorithms directly:

```
Index:     0     1     2     3     4     5
        ┌─────┬─────┬─────┬─────┬─────┬─────┐
arr  =  │ 13  │ 46  │ 24  │ 52  │ 20  │  9  │
        └─────┴─────┴─────┴─────┴─────┴─────┘
```

In each pass, `[ ]` marks the pair being compared. Everything to the right of `||` is already in its final position.

**Pass 1: compare neighbors across the entire array (index 0 to 5).**

```
[13 | 46]| 24 | 52 | 20 |  9      13 < 46 → in order, no swap
 13 |[46 | 24]| 52 | 20 |  9      46 > 24 → SWAP
 13 | 24 |[46 | 52]| 20 |  9      46 < 52 → in order, no swap
 13 | 24 | 46 |[52 | 20]|  9      52 > 20 → SWAP
 13 | 24 | 46 | 20 |[52 |  9]     52 > 9  → SWAP

Result:  13 | 24 | 46 | 20 |  9 || 52
                                   ✅ the maximum (52) has bubbled to the end
```

**Pass 2: the last element is settled, so compare only index 0 to 4.**

```
[13 | 24]| 46 | 20 |  9 || 52      13 < 24 → no swap
 13 |[24 | 46]| 20 |  9 || 52      24 < 46 → no swap
 13 | 24 |[46 | 20]|  9 || 52      46 > 20 → SWAP
 13 | 24 | 20 |[46 |  9]|| 52      46 > 9  → SWAP

Result:  13 | 24 | 20 |  9 || 46 | 52
                              ✅ 46 is the maximum of this range
```

**Pass 3: compare only index 0 to 3.**

```
[13 | 24]| 20 |  9 || 46 | 52      13 < 24 → no swap
 13 |[24 | 20]|  9 || 46 | 52      24 > 20 → SWAP
 13 | 20 |[24 |  9]|| 46 | 52      24 > 9  → SWAP

Result:  13 | 20 |  9 || 24 | 46 | 52
```

**Pass 4: compare only index 0 to 2.**

```
[13 | 20]|  9 || 24 | 46 | 52      13 < 20 → no swap
 13 |[20 |  9]|| 24 | 46 | 52      20 > 9  → SWAP

Result:  13 |  9 || 20 | 24 | 46 | 52
```

**Pass 5: compare only index 0 to 1.**

```
[13 |  9]|| 20 | 24 | 46 | 52      13 > 9  → SWAP

Result:   9 || 13 | 20 | 24 | 46 | 52
          ✅ fully sorted
```

**Why only 5 passes for 6 elements?** Just like selection sort: once the last five elements are in place, the one remaining element at the front has nowhere else to be. So `n` elements need **`n − 1` passes**.

Notice that each pass **shrinks the range by one**, because each pass permanently settles one more element at the back. That shrinking range is exactly what we need to capture in the loops.

### Turning the Idea into Code: Observing the Pattern

Write down the range of indices each pass covers:

```
Pass 1:  compare within index 0 to n-1
Pass 2:  compare within index 0 to n-2
Pass 3:  compare within index 0 to n-3
Pass 4:  compare within index 0 to n-4
   ...
Last:    compare within index 0 to 1
```

The **start** of the range is always `0`. The **end** of the range goes `n-1, n-2, n-3, ..., 1`, shrinking by one each pass. That gives us:

1. **Outer loop:** a variable `i` that marks the **last index of the current range**, starting at `n - 1` and counting **down** to `1`. (It stops at `1`, not `0`, because a range of a single element needs no comparison.)
2. **Inner loop:** a variable `j` that walks from `0` up through the range, comparing the pair `(arr[j], arr[j+1])`.

Compare this to Part 1: selection sort's outer loop counted **up** (`0 … n-2`) with a range that shrank from the **left**. Bubble sort's outer loop counts **down** with a range that shrinks from the **right**.

### The Boundary of the Inner Loop: The Critical Detail

Each comparison looks at `arr[j]` **and** `arr[j + 1]`. So the last valid value of `j` is **one before the range's last index**:

```
Range is index 0 to i. Pairs available:
   (0,1), (1,2), (2,3), ..., (i-1, i)
                               ↑
                    the last pair starts at j = i - 1

So the inner loop runs:   j = 0  up to  j = i - 1     (i.e., j < i)
```

If the inner loop instead ran all the way to `j = i`, then `arr[j + 1]` would be `arr[i + 1]`, which is the element that's *already settled* (or, in pass 1, one slot **past the end of the array**).

⚠️ **This is the most important trap in bubble sort.** In pass 1, `i = n - 1`. If `j` were allowed to reach `n - 1`, the code would read `arr[n]`: an index that does not exist. As we saw in Lecture 2, C++ does no bounds checking, so this silently reads garbage memory (or crashes at runtime). The rule: **the inner loop stops at `j < i`, so that `j + 1` never exceeds `i`.**

### The Comparison and Swap

Inside the inner loop, for each pair:

- If `arr[j] > arr[j + 1]`, the pair is out of order, so **swap** them using the same `temp` technique from Part 1.
- Otherwise, do nothing.

The swap uses a temporary variable, with the same three-step pattern as Part 1:

```
   temp = arr[j + 1]
   arr[j + 1] = arr[j]
   arr[j] = temp
```

### The Basic Code

```cpp
void bubbleSort(int arr[], int n) {
    for (int i = n - 1; i >= 1; i--) {        // i = last index of the current range
        for (int j = 0; j <= i - 1; j++) {     // compare each adjacent pair in the range
            if (arr[j] > arr[j + 1]) {
                int temp = arr[j + 1];          // swap the out-of-order pair
                arr[j + 1] = arr[j];
                arr[j] = temp;
            }
        }
    }
}
```

The array is sorted **in place**, so the caller only passes the array and its size:

```cpp
bubbleSort(arr, n);
```

⚠️ **Use strict `>`, not `>=`.** With `>=`, two equal neighbors would be swapped for no reason. That wastes work and, worse, destroys **stability** (equal elements would trade places on every pass). With strict `>`, equal elements never cross each other, which makes bubble sort a **stable** sort, unlike selection sort from Part 1.

⚠️ **Compare *adjacent* elements only.** Bubble sort never compares or swaps elements that are far apart. That is exactly what makes it stable, and also what makes it slow: an element that is far from its final position can only move **one slot per swap**.

### Time Complexity: The Basic Version

Count the comparisons made in each pass:

```
Pass 1:  n-1 comparisons    (j runs 0 … n-2)
Pass 2:  n-2 comparisons
Pass 3:  n-3 comparisons
   ...
Pass n-1:  1 comparison

Total = (n-1) + (n-2) + ... + 2 + 1
      = n(n-1)/2
      = (n² − n) / 2
```

The same summation of natural numbers as selection sort, so after dropping the constant `1/2` and the lower-order `n`, we get **O(n²)**.

Notice the problem with the basic version: the loops run **exactly the same number of times regardless of the data**. Even if you hand it an array that is *already sorted*, it performs all `n(n−1)/2` comparisons, finds nothing to swap, and uses the full O(n²) time anyway. That feels wasteful. This is where the optimization comes in.

---

## 4. The Optimization: Stop Early If Nothing Moved

### The Observation

Think about what a pass *means*. During a pass, every adjacent pair is checked. If **no swap happened in the entire pass**, then every neighboring pair was already in the correct order. And if every neighboring pair is in order, **the whole array is sorted**. There is no reason to run any further passes.

**Real-life analogy:** The teacher walks down the line checking each pair of neighbors, and finds that nobody needs to swap. The line is already in height order, so the teacher stops and goes home, instead of dutifully walking the line `n − 1` times anyway.

```
Array that is already sorted:

        [ 2 | 3 | 5 | 15 | 20 ]

Pass 1:  2 < 3 ✓   3 < 5 ✓   5 < 15 ✓   15 < 20 ✓
         → no swaps happened in this entire pass
         → the array must be sorted → STOP (skip all remaining passes)
```

### Implementing It with a Flag

We add a flag variable, `didSwap`, and follow three rules:

1. **At the start of every pass**, set `didSwap = 0` (assume nothing will move).
2. **Whenever a swap happens**, set `didSwap = 1`.
3. **After the inner loop finishes**, check the flag. If `didSwap` is still `0`, no swap occurred in this pass, so **`break`** out of the outer loop.

```cpp
void bubbleSort(int arr[], int n) {
    for (int i = n - 1; i >= 1; i--) {
        int didSwap = 0;                        // reset at the START of every pass

        for (int j = 0; j <= i - 1; j++) {
            if (arr[j] > arr[j + 1]) {
                int temp = arr[j + 1];
                arr[j + 1] = arr[j];
                arr[j] = temp;
                didSwap = 1;                    // a swap happened in this pass
            }
        }

        if (didSwap == 0) {                     // a full pass with zero swaps
            break;                              // → already sorted → stop
        }
    }
}
```

⚠️ **The flag must be reset inside the outer loop, not before it.** If `didSwap = 0` is declared once above the outer loop, then a swap in pass 1 sets it to `1` and it **stays** `1` forever, so the `break` never fires, and the optimization silently does nothing. Each pass needs to ask its own question: "did *this* pass swap anything?"

### What the Optimization Does to the Complexity

Compare the same function on two different inputs of size 6:

```
Reverse-sorted input:   [ 6 | 5 | 4 | 3 | 2 | 1 ]

   Every pass finds out-of-order pairs, so didSwap is set to 1 in every
   pass, and the outer loop runs all n - 1 = 5 passes.
   → full O(n²) work.

Already-sorted input:   [ 1 | 2 | 3 | 4 | 5 | 6 ]

   Pass 1: no pair is out of order → didSwap stays 0 → break immediately.
   → only ONE pass of n - 1 comparisons is ever made.
   → O(n) work.
```

| Case | Input | What happens | Time Complexity |
| --- | --- | --- | --- |
| **Best** | already sorted | one pass, zero swaps, `break` | **O(n)** |
| **Average** | random order | many passes, many swaps | **O(n²)** |
| **Worst** | reverse sorted | every pass swaps, no early exit | **O(n²)** |

The best case is **O(n)** because the single pass still has to *look at* all `n − 1` adjacent pairs to confirm they are in order. It can't be faster than reading the input once.

⚠️ **This is a favorite interview question.** If asked for bubble sort's best-case time complexity, the answer is **O(n)**, but *only with the `didSwap` optimization*. Without it, bubble sort is O(n²) even on sorted input. A complete answer states both the use case ("the array is already sorted") and the mechanism ("a pass with no swaps lets us break out after the first pass").

**Space complexity:** **O(1)**. Only `i`, `j`, `temp`, and `didSwap` are used.

### Swaps vs Comparisons for Bubble Sort

The reverse-sorted array also shows bubble sort's weakness from Part 1's perspective. Every comparison on a reversed array triggers a swap, giving up to **n(n−1)/2 swaps**. Recall that selection sort made at most `n − 1` swaps. So if swaps are the expensive part, bubble sort is the **worse** choice of the two, even though both are O(n²) in comparisons.

---

## 5. Full Working Example

```cpp
#include <iostream>
using namespace std;

void bubbleSort(int arr[], int n) {
    for (int i = n - 1; i >= 1; i--) {
        int didSwap = 0;

        for (int j = 0; j <= i - 1; j++) {
            if (arr[j] > arr[j + 1]) {
                int temp = arr[j + 1];
                arr[j + 1] = arr[j];
                arr[j] = temp;
                didSwap = 1;
            }
        }

        if (didSwap == 0) {
            break;
        }
    }
}

void printArray(int arr[], int n) {
    for (int i = 0; i < n; i++) {
        cout << arr[i] << " ";
    }
    cout << endl;
}

int main() {
    int arr[] = {13, 46, 24, 52, 20, 9};
    int n = sizeof(arr) / sizeof(arr[0]);

    bubbleSort(arr, n);
    printArray(arr, n);      // 9 13 20 24 46 52

    int sortedArr[] = {1, 2, 3, 4, 5, 6};
    bubbleSort(sortedArr, 6);    // exits after a single pass
    printArray(sortedArr, 6);    // 1 2 3 4 5 6

    return 0;
}
```

---

## 6. Selection Sort vs Bubble Sort: A Direct Comparison

| Feature | Selection Sort | Bubble Sort |
| --- | --- | --- |
| Core move | **select** the minimum, swap it to the front | **adjacent swaps** push the maximum to the back |
| Sorted part grows from | the **front** | the **back** |
| Outer loop | `i` counts **up**, `0 … n-2` | `i` counts **down**, `n-1 … 1` |
| Inner loop | `j` from `i+1` to `n-1` | `j` from `0` to `i-1` |
| Compares | elements anywhere in the range | **only adjacent** elements |
| Best case | O(n²): can never stop early | **O(n)** with the `didSwap` flag |
| Worst case | O(n²) | O(n²) |
| Number of swaps | at most `n − 1` | up to `n(n−1)/2` |
| Stable? | ❌ no | ✅ yes (with strict `>`) |
| Adaptive? | ❌ no | ✅ yes (only with the `didSwap` flag) |
| Space | O(1) | O(1) |

**The takeaway:** both are O(n²) in the worst case, so neither is used for large data. But they differ in *character*. Selection sort is predictable and swap-frugal, while bubble sort is stable and, with one flag, fast on data that is already (nearly) sorted.

---

## 7. Part 2 Recap

| Concept | Meaning | Key point |
| --- | --- | --- |
| Bubble sort idea | swap adjacent out-of-order pairs, so the **maximum bubbles to the end** | one element settles per pass |
| Passes needed | `n − 1` | the last remaining element is trivially sorted |
| Outer loop | `i` from `n − 1` **down to** `1` | `i` = last index of the current range |
| Inner loop | `j` from `0` to `i − 1` (`j < i`) | stops early so `j + 1` stays inside the range |
| Boundary trap | letting `j` reach `i` reads `arr[i + 1]` | in pass 1 that is `arr[n]`, out of bounds |
| Comparison | `arr[j] > arr[j + 1]` (strict) | `>=` would break stability and waste swaps |
| `didSwap` flag | reset to `0` at the **start of each pass**, set to `1` on a swap | `0` after a pass means sorted → `break` |
| Flag placement | inside the outer loop | declared outside, it never resets and the `break` never fires |
| Best case | **O(n)** (sorted input, with the flag) | one pass confirms the order |
| Average / Worst | **O(n²)** | summation `n(n−1)/2` |
| Space | **O(1)** | in-place |
| Swaps | up to `n(n−1)/2` | far more than selection sort's `n − 1` |

**The single biggest idea from this part:** bubble sort pushes the **maximum to the back** through adjacent swaps, and a pass with **no swaps proves the array is sorted**. That one observation is what turns its best case from O(n²) into O(n).

# 🎯Part 3: Insertion Sort & Full Comparison

---

## Quick Recap Before We Continue

In Part 1 we built **Selection Sort**, which selects the minimum from the unsorted part and swaps it to the front. It is O(n²) in every case. In Part 2 we built **Bubble Sort**, which uses adjacent swaps to push the maximum to the back. It is O(n²) in general, but O(n) on sorted input thanks to the `didSwap` flag.

Today's algorithm works from a different angle. Selection sort asks "which element belongs at this position?" Insertion sort asks the opposite question: **"where does this element belong?"** After the algorithm, we compare all three side by side.

---

## 8. Insertion Sort

### The Idea

Insertion sort takes **one element at a time** and **places it in its correct position** among the elements to its left. Those left-hand elements are already sorted, so the new element only needs to slide left until it reaches its correct spot.

**Real-life analogy:** Sorting a hand of playing cards. You pick up cards one at a time. Each new card is compared with the cards already in your hand (which are kept in order) and slid left past every larger card until it sits in the right place. You never re-sort the whole hand, only the newcomer finds its spot.

### Walking Through the Algorithm

We'll sort this array of 7 elements:

```
Index:     0     1     2     3     4     5     6
        ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┐
arr  =  │ 14  │  9  │ 15  │ 12  │  6  │  8  │ 13  │
        └─────┴─────┴─────┴─────┴─────┴─────┴─────┘
```

The part to the left of `||` is the **sorted region**. The element just right of `||` is the one being inserted next.

**Start: the first element alone is already sorted.** A region of size one has nothing to be out of order with.

```
[ 14 ] || 9 | 15 | 12 | 6 | 8 | 13
```

**Take 9 (index 1).** Compare with its left neighbor: 14 > 9, so swap. Nothing is left of it, so it stops.

```
[ 14 |  9 ] → swap → [  9 | 14 ] || 15 | 12 | 6 | 8 | 13
```

**Take 15 (index 2).** Compare with the left neighbor: 14 > 15? No. It is already in the right place, so stop.

```
[  9 | 14 | 15 ] || 12 | 6 | 8 | 13        (no movement)
```

**Take 12 (index 3).** It slides left past 15 and 14, and stops when it meets 9.

```
 9 | 14 | 15 | 12      15 > 12 → swap
 9 | 14 | 12 | 15      14 > 12 → swap
 9 | 12 | 14 | 15       9 > 12? No → stop

[  9 | 12 | 14 | 15 ] || 6 | 8 | 13
```

**Take 6 (index 4).** It is smaller than everything, so it slides all the way to the front.

```
 9 | 12 | 14 | 15 |  6      15 > 6 → swap
 9 | 12 | 14 |  6 | 15      14 > 6 → swap
 9 | 12 |  6 | 14 | 15      12 > 6 → swap
 9 |  6 | 12 | 14 | 15       9 > 6 → swap
 6 |  9 | 12 | 14 | 15      nothing left of index 0 → stop

[  6 |  9 | 12 | 14 | 15 ] || 8 | 13
```

**Take 8 (index 5).** It slides left past 15, 14, 12, and 9, and stops when it meets 6.

```
 6 |  9 | 12 | 14 | 15 |  8
 6 |  9 | 12 | 14 |  8 | 15      swap
 6 |  9 | 12 |  8 | 14 | 15      swap
 6 |  9 |  8 | 12 | 14 | 15      swap
 6 |  8 |  9 | 12 | 14 | 15      swap
 6 > 8? No → stop

[  6 |  8 |  9 | 12 | 14 | 15 ] || 13
```

**Take 13 (index 6).** It slides left past 15 and 14, and stops when it meets 12.

```
 6 |  8 |  9 | 12 | 14 | 15 | 13
 6 |  8 |  9 | 12 | 14 | 13 | 15      swap
 6 |  8 |  9 | 12 | 13 | 14 | 15      swap
12 > 13? No → stop

[  6 |  8 |  9 | 12 | 13 | 14 | 15 ]
 ✅ fully sorted
```

Every element was picked up once, and each was slid left into place within the sorted region on its left.

### Turning the Idea into Code: Observing the Pattern

Two patterns come out of the walkthrough:

1. **Outer loop:** every element gets picked up once, left to right. So `i` runs across the array. (For `i = 0` there is nothing on the left, so nothing happens, and starting from `i = 1` would be equivalent.)
2. **Inner movement:** for each picked element at index `i`, we keep moving it left, one swap at a time, **as long as it can and should move**.

We capture the inner movement with a variable `j`, which starts at `i` (the element's current position) and moves left by one after each swap (`j--`). The movement continues only while **both** of these hold:

| Condition | Why it must hold |
| --- | --- |
| `j > 0` | there must be something to the left to compare with |
| `arr[j - 1] > arr[j]` | the left neighbor is larger, so the order is wrong and a swap is needed |

The moment either condition fails, the element is in its correct position, so stop and move on to the next `i`.

### The Boundary Trap: `j > 0`, Not `j >= 0`

The comparison reads `arr[j - 1]`, so `j - 1` must be a valid index:

```
If j = 1:  arr[j - 1] = arr[0]    → valid, the last comparison we can make
If j = 0:  arr[j - 1] = arr[-1]   → INVALID, an index before the array starts
```

So the loop must stop **when `j` reaches `0`**: `j > 0`. In the walkthrough above, when 6 slid to the very front, the last real comparison was between 6 and 9 (`j = 1`). After that swap, `j` became `0`, and the loop ended without trying to read anything left of index 0.

⚠️ **Using `j >= 0` reads `arr[-1]`.** As in Lecture 2 and Part 2, C++ does not check bounds, so this silently reads garbage memory or crashes.

⚠️ **Condition order matters: `j > 0` must come first.** In `while (j > 0 && arr[j - 1] > arr[j])`, C++ evaluates left to right and **stops evaluating as soon as the result is known**. When `j > 0` is false, `arr[j - 1]` is never touched. Written the other way (`arr[j - 1] > arr[j] && j > 0`), the array access happens first and reads `arr[-1]` before the guard can protect it.

### The Code

```cpp
void insertionSort(int arr[], int n) {
    for (int i = 0; i <= n - 1; i++) {          // pick up each element, left to right
        int j = i;

        while (j > 0 && arr[j - 1] > arr[j]) {   // can move left AND should move left
            int temp = arr[j - 1];               // swap with the left neighbor
            arr[j - 1] = arr[j];
            arr[j] = temp;

            j--;                                  // follow the element one step left
        }
    }
}
```

The array is sorted **in place**, so the caller just passes the array and its size:

```cpp
insertionSort(arr, n);
```

⚠️ **Don't forget `j--`.** The swap moves the element one slot left, so `j` must follow it. Without `j--`, the loop compares the same two positions forever (an infinite loop), because the condition is re-checked on indices that never change.

⚠️ **Strict `>`, not `>=`.** With `>=`, an element would slide past an *equal* neighbor, which pointlessly reorders equal values and destroys stability. With strict `>`, the element stops as soon as it meets something equal or smaller. This is what makes insertion sort **stable**.

### Full Working Example

```cpp
#include <iostream>
using namespace std;

void insertionSort(int arr[], int n) {
    for (int i = 0; i <= n - 1; i++) {
        int j = i;
        while (j > 0 && arr[j - 1] > arr[j]) {
            int temp = arr[j - 1];
            arr[j - 1] = arr[j];
            arr[j] = temp;
            j--;
        }
    }
}

void printArray(int arr[], int n) {
    for (int i = 0; i < n; i++) {
        cout << arr[i] << " ";
    }
    cout << endl;
}

int main() {
    int arr[] = {14, 9, 15, 12, 6, 8, 13};
    int n = sizeof(arr) / sizeof(arr[0]);

    insertionSort(arr, n);
    printArray(arr, n);      // 6 8 9 12 13 14 15

    return 0;
}
```

### Time Complexity of Insertion Sort

Unlike selection sort, the cost here **depends on the input**. The `while` loop runs only as many times as the element needs to travel left.

**Worst case: reverse-sorted input.** Every element must travel all the way to the front.

```
arr = [ 5 | 4 | 3 | 2 | 1 ]

Pick 5 → nothing to its left          →  0 swaps
Pick 4 → slides past 5                 →  1 swap
Pick 3 → slides past 5, 4              →  2 swaps
Pick 2 → slides past 5, 4, 3           →  3 swaps
Pick 1 → slides past 5, 4, 3, 2        →  4 swaps

Total swaps = 0 + 1 + 2 + 3 + 4 = n(n−1)/2   →   O(n²)
```

This is the same summation of natural numbers we saw in selection sort and bubble sort.

**Best case: already-sorted input.** No element has to move.

```
arr = [ 1 | 2 | 3 | 4 | 5 ]

Pick 1 → nothing to its left                  → while loop does not run
Pick 2 → is 1 > 2? No → stop immediately      → while loop does not run
Pick 3 → is 2 > 3? No → stop immediately      → while loop does not run
Pick 4 → is 3 > 4? No → stop immediately      → while loop does not run
Pick 5 → is 4 > 5? No → stop immediately      → while loop does not run

The outer loop still runs n times, and each iteration does one comparison.
→ O(n)
```

| Case | Input | What happens | Time Complexity |
| --- | --- | --- | --- |
| **Best** | already sorted | every `while` check fails immediately | **O(n)** |
| **Average** | random order | each element travels about halfway | **O(n²)** |
| **Worst** | reverse sorted | each element travels all the way to the front | **O(n²)** |

**Space complexity:** **O(1)**. Only `i`, `j`, and `temp` are used.

⚠️ **Unlike bubble sort, insertion sort needs no extra flag for its O(n) best case.** The `while` condition `arr[j - 1] > arr[j]` fails at once on sorted data, so the optimization is built into the algorithm's structure. This also makes it **adaptive**: the closer the input is to sorted, the less work it does.

⚠️ **A swap-by-swap insertion is not the cheapest form.** Here each step is a full three-assignment swap. A common refinement (the same "shift right to make room" move from Lecture 2, Section 5.2) saves the picked element once, shifts larger elements right with single assignments, and writes the element once at the end. The complexity class is the same, but the number of writes drops. The swap form above is the clearest for learning the idea.

---

## 9. Comparing All Three Sorts

### The Core Idea of Each

|  | Selection Sort | Bubble Sort | Insertion Sort |
| --- | --- | --- | --- |
| Question it asks | "which element belongs at this position?" | "is this adjacent pair in order?" | "where does this element belong?" |
| Core move | select the minimum, swap to the front | swap adjacent pairs, maximum bubbles to the back | slide one element left into the sorted region |
| Sorted region grows | at the **front** | at the **back** | at the **front** (but not yet final) |
| Analogy | picking the shortest student from the whole line | the teacher swapping neighbors down the line | arranging a hand of cards |

⚠️ **A subtle but important difference:** after each pass of selection sort and bubble sort, the settled elements are in their **final** positions. In insertion sort, the sorted region on the left is sorted **only relative to itself**. A later, smaller element (like 6 in our walkthrough) can still push its way to the very front and shift everything.

### Complexity and Properties

| Property | Selection Sort | Bubble Sort | Insertion Sort |
| --- | --- | --- | --- |
| **Best case** | O(n²) | **O(n)** (with `didSwap`) | **O(n)** |
| **Average case** | O(n²) | O(n²) | O(n²) |
| **Worst case** | O(n²) | O(n²) | O(n²) |
| **Space** | O(1) | O(1) | O(1) |
| **In-place?** | ✅ | ✅ | ✅ |
| **Stable?** | ❌ | ✅ | ✅ |
| **Adaptive?** | ❌ | ✅ (only with the flag) | ✅ (naturally) |
| **Max swaps** | `n − 1` | `n(n−1)/2` | `n(n−1)/2` |
| **Loop shape** | outer up, inner scans the whole rest | outer **down**, inner shrinks from the right | outer up, inner is a `while` that moves left |

### Which One to Use?

| Situation | Best choice | Reason |
| --- | --- | --- |
| Swaps are expensive, comparisons are cheap | **Selection sort** | at most `n − 1` swaps |
| Data is already (or nearly) sorted | **Insertion sort** | close to O(n) naturally |
| Need to keep equal elements in their original order | **Bubble or insertion sort** | both are stable |
| Small arrays | **Insertion sort** | low overhead and simple code |
| Large arrays | **None of these** | O(n²) grows too fast; later courses cover O(n log n) sorts such as merge sort |

**The interview angle:** a likely question is "give the best-case complexity of each". The answer is selection: O(n²), bubble: O(n) only with the early-exit flag, and insertion: O(n). A complete answer states the *input that causes it* (already sorted) and the *mechanism* (no swaps in a pass for bubble, the `while` loop never running for insertion).

---

## 10. Part 3 Recap

| Concept | Meaning | Key point |
| --- | --- | --- |
| Insertion sort idea | take one element and **insert it into its correct place** among the elements on its left | like sorting playing cards in your hand |
| Starting point | the first element alone is a sorted region of size one | nothing on its left to compare with |
| Outer loop | `i` across the array, left to right | each element is picked up once |
| Inner loop | `while (j > 0 && arr[j - 1] > arr[j])` | keep moving left while it can and should |
| Boundary | `j > 0`, not `j >= 0` | `j >= 0` would read `arr[-1]` |
| Condition order | `j > 0` **first** | short-circuiting protects `arr[j - 1]` |
| Movement | swap with the left neighbor, then `j--` | forgetting `j--` loops forever |
| Comparison | strict `>` | keeps equal elements in order, so the sort is stable |
| Best case | **O(n)** | sorted input, so the `while` never runs |
| Average / Worst | **O(n²)** | worst case is `n(n−1)/2` swaps on reversed input |
| Space | **O(1)** | in-place |
| Sorted region | sorted only relative to itself, not final | later elements can still shift it |

**The single biggest idea from this lecture:** three sorting algorithms, one goal, three ways of making progress. **Selection** picks the right element for a position, **bubble** fixes neighboring pairs until the maximum settles, and **insertion** finds the right position for an element. All three are O(n²) in general, which is why they suit small or nearly sorted data, while their best cases (O(n) for bubble with a flag and for insertion) show how much a well-placed early exit can save.

---

## What's Next

**Lecture 4 – Searching:** Linear Search and Binary Search, each with a full step-by-step walkthrough, the loop-boundary traps (`low <= high`, the midpoint calculation), and complexity analysis. This is where the sorting we just built pays off, since binary search only works on sorted data.