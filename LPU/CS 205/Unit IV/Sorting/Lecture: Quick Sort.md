# Lecture 4: Quick Sort

Status: Not Started

## Quick Recap Before We Begin

In Lecture 3 we met Merge Sort: **divide** the array into halves (by index ranges `low`/`mid`/`high`), recurse, and then do the real work, the **merge**, on the way back up. It is O(n log n) in every case, but it needs an O(n) `temp` array.

Today's algorithm, **Quick Sort**, is also recursive and also divide and conquer, but it flips where the work happens. Merge sort divides cheaply and merges expensively. Quick sort does the **expensive work first** (the partition step) and then simply recurses on the two sides, with nothing to combine afterwards. The payoff: **no temporary array**.

---

## 1. The Core Idea: Pick a Pivot, Place It, Split Around It

Quick sort repeats two steps:

| Step | What it does |
| --- | --- |
| **1. Pick a pivot** | choose any element of the current piece to be the pivot |
| **2. Place the pivot at its *correct sorted position***, with **smaller elements on its left** and **larger elements on its right** | this is called **partitioning** |

Then do the same thing recursively on the left part and on the right part.

**Why does this sort the array?** Once the pivot sits at its correct sorted position, it never needs to move again. If we keep choosing pivots and fixing each one in place, eventually *every* element has been fixed in its correct place, so the whole array is sorted. It doesn't matter which element you pick as the pivot, only that each pick gets placed correctly.

**Real-life analogy:** A teacher arranging students by height. She picks one student (the pivot), asks everyone shorter to stand on that student's left and everyone taller to stand on the right. That student is now in the final position. The teacher then repeats the same exercise separately for the left group and for the right group.

### The Pivot Can Be Anything

First element, last element, middle, median, or a random element: all are valid. In this lecture we use the **first element of the current piece**.

### Why "Smaller Left, Larger Right" Is Logical

In the final sorted array, everything to the left of the pivot is smaller and everything to the right is larger. So we only need to *push* elements to the correct *side* of the pivot. The two sides are not yet sorted internally, which is exactly what the recursive calls will fix.

---

## 2. Seeing It on an Array

Take this array (indices `0..7`):

```
Index:     0     1     2     3     4     5     6     7
        ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐
arr  =  │  4  │  6  │  2  │  5  │  7  │  9  │  1  │  3  │
        └─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┘
```

The sorted array would be `1 2 3 4 5 6 7 9`, so `4` belongs at **index 3**. After one partition with pivot `4`:

```
Before:   [ 4 | 6 | 2 | 5 | 7 | 9 | 1 | 3 ]
                  pivot = 4

After:    [ 1 | 3 | 2 ]  4  [ 7 | 9 | 5 | 6 ]
           smaller         ▲    larger
           (unsorted)   partition   (unsorted)
                         index = 3
```

`4` is now at its final position. The left part `[1, 3, 2]` and right part `[7, 9, 5, 6]` are still unsorted, so we run quick sort again on each. The index where the pivot lands is called the **partition index**, because it splits the array into two independent problems.

---

## 3. How to Do the Partition in Code

The question is how to get the pivot to its correct place **without building new arrays** and without cleverly peeking at the sorted answer. We use two pointers.

### The Two Pointers

| Pointer | Starts at | Job |
| --- | --- | --- |
| `i` | `low` | moves **right**, looking for the first element **greater than the pivot** |
| `j` | `high` | moves **left**, looking for the first element **smaller than (or not greater than) the pivot** |

### The Loop

Repeat while `i < j`:

1. Move `i` right until it stops on an element **greater than** the pivot.
2. Move `j` left until it stops on an element **less than or equal to** the pivot.
3. If `i < j`, **swap** `arr[i]` and `arr[j]`. (A big element on the left and a small element on the right are both in the wrong zone, so swapping fixes both.)

When the pointers **cross** (`j < i`), stop. At that moment:

- everything at or before `j` is `<=` pivot (left zone),
- everything after `j` is `>` pivot (right zone).

`j` is therefore pointing at the **last element of the left zone**, which is exactly where the pivot belongs. So the final step is:

1. **Swap the pivot** (still at `arr[low]`) with `arr[j]`, and **return `j`** as the partition index.

### The Partition, Snapshot by Snapshot (pivot = 4)

`^i` and `^j` mark the pointer positions after each pointer search finishes:

```
Start:      [ 4 | 6 | 2 | 5 | 7 | 9 | 1 | 3 ]
                   ^i                      ^j       i stops at 6 (>4), j stops at 3 (<=4)
swap(6,3):  [ 4 | 3 | 2 | 5 | 7 | 9 | 1 | 6 ]

                           ^i          ^j           i stops at 5 (>4), j stops at 1 (<=4)
swap(5,1):  [ 4 | 3 | 2 | 1 | 7 | 9 | 5 | 6 ]

                       ^j  ^i                       i stops at 7 (>4), j walks left to 1 (<=4)
                                                    j (index 3) has CROSSED i (index 4): STOP, no swap

swap pivot with arr[j]:
            [ 1 | 3 | 2 | 4 | 7 | 9 | 5 | 6 ]
                          ▲
                 partition index = j = 3
```

Left zone `[1, 3, 2]` is all smaller than 4, right zone `[7, 9, 5, 6]` is all larger.

---

## 4. The Code

```cpp
int partition(vector<int> &arr, int low, int high) {
    int pivot = arr[low];              // first element is the pivot
    int i = low;
    int j = high;

    while (i < j) {
        while (arr[i] <= pivot && i <= high - 1) {   // find first element > pivot
            i++;
        }
        while (arr[j] > pivot && j >= low + 1) {     // find first element <= pivot
            j--;
        }
        if (i < j) {
            swap(arr[i], arr[j]);                    // both are in the wrong zone
        }
    }

    swap(arr[low], arr[j]);            // place the pivot at its correct position
    return j;                          // partition index
}

void quickSort(vector<int> &arr, int low, int high) {
    if (low < high) {                  // piece has more than one element
        int pIndex = partition(arr, low, high);
        quickSort(arr, low, pIndex - 1);       // sort the left part
        quickSort(arr, pIndex + 1, high);      // sort the right part
    }
}

int main() {
    vector<int> arr = {4, 6, 2, 5, 7, 9, 1, 3};
    int n = arr.size();
    quickSort(arr, 0, n - 1);
    for (int x : arr) cout << x << " ";        // 1 2 3 4 5 6 7 9
}
```

### Reading `quickSort`

- **Condition `low < high`:** a piece with one element (`low == high`) or none (`low > high`) is already sorted, so we do nothing. This is the base case, written as "recurse only if there is something to sort".
- **Order of the two recursive calls** doesn't matter, since the two sides are independent. Either order sorts correctly.
- **No merge step.** Once both sides are sorted, the array is already sorted: left zone, then pivot, then right zone.

---

## 5. Details That Decide Correctness

### Why `<=` for `i` and `>` for `j`?

Suppose the array holds another `4`. We must decide which side equal elements go to. Our rule: **equal elements go on the left**.

| Pointer | Condition to keep moving | Stops at |
| --- | --- | --- |
| `i` | `arr[i] <= pivot` | first element **strictly greater** than pivot |
| `j` | `arr[j] > pivot` | first element **less than or equal** to pivot |

⚠️ **Whatever side you choose for equal elements, the two conditions must be consistent with each other.** If both pointers treat "equal to pivot" the same way, they can fail to stop, so the loop never terminates or elements end up in the wrong zone. If you prefer equals on the right, mirror both conditions together.

### Why the Extra Boundary Guards `i <= high - 1` and `j >= low + 1`?

Each pointer's `while` loop reads `arr[i]` or `arr[j]` and then moves. Without a guard, a pointer can run off the piece:

- **`i`**: if every element from `i` onward is `<= pivot`, `i++` would eventually step past `high` (past the end of the piece), reading memory outside it. Stopping at `i <= high - 1` means the last move lands on `high` and no further.
- **`j`**: if every element is `> pivot`, `j--` would walk below `low`. Stopping at `j >= low + 1` means the last move lands on `low` (where the pivot itself sits) and no further.

⚠️ **C++ does no bounds checking** (Unit I, Lecture 2). An unguarded pointer silently reads garbage or crashes. The guards are not decoration.

### Why Swap the Pivot With `arr[j]` and Not `arr[i]`?

When the loop ends, `i` has crossed over into the right zone (it is sitting on an element `> pivot`), so swapping the pivot with `arr[i]` would put a larger element at `low`, on the left. `j` is the last element of the **left** zone (`<= pivot`), so that is the one that can safely trade places with the pivot.

⚠️ **The pivot is at `arr[low]` the entire time.** It never moves during the loop. Its swap with `arr[j]` is the very last step, and that is what makes it land at its final index.

---

## 6. The Recursion Tree

For `[4, 6, 2, 5, 7, 9, 1, 3]`, the partition index and the pieces each call works on:

```
qs(0,7)  pivot 4 → lands at index 3
├── qs(0,2)   [1,3,2]      pivot 1 → lands at index 0
│   ├── qs(0,-1)  (empty, nothing to do)
│   └── qs(1,2)   [3,2]    pivot 3 → lands at index 2
│       ├── qs(1,1)  (one element)
│       └── qs(3,2)  (empty)
│
└── qs(4,7)   [7,9,5,6]    pivot 7 → lands at index 6
    ├── qs(4,5)   [5,6]    pivot 5 → lands at index 4
    │   ├── qs(4,3)  (empty)
    │   └── qs(5,5)  (one element)
    └── qs(7,7)   [9]      (one element)
```

Compare this with Lecture 3's tree:

|  | Merge sort tree | Quick sort tree |
| --- | --- | --- |
| How the split is chosen | always exactly the middle | wherever the pivot lands (depends on the data) |
| Is the tree balanced? | always | **not necessarily** |
| Where the real work happens | merging on the way **up** | partitioning on the way **down** |
| Work after the recursive calls | `merge` | nothing |

This is the key conceptual contrast: **merge sort's shape is fixed, quick sort's shape depends on the input.** That one fact explains the complexity analysis below.

---

## 7. Complexity Analysis

### Time Complexity

**One partition call** moves `i` rightward and `j` leftward across the piece. Together they touch each element of the piece about once, so partitioning a piece of size `m` costs **O(m)**.

**Best / average case: the pivot lands near the middle.** Then each call splits the piece into two roughly equal halves, giving about **log₂ n** levels. Every level partitions pieces that together cover about `n` elements:

```
Total = (work per level) × (number of levels) = n × log n = O(n log n)
```

This is the same halving argument as merge sort.

**Worst case: the pivot lands at one extreme.** If the pivot is always the smallest or largest element of its piece, one side is empty and the other has `n - 1` elements:

```
n  +  (n-1)  +  (n-2)  + ... + 1  =  n(n+1)/2   →   O(n²)
```

This happens with our "first element as pivot" choice when the **array is already sorted** (or reverse sorted): every pivot is the smallest element of its piece, so the tree degenerates into a straight line, with depth `n` instead of `log n`.

```
Balanced (typical):               Degenerate (sorted input, first-element pivot):

      qs(n)                        qs(n)
     /     \                          \
  qs(n/2) qs(n/2)                    qs(n-1)
   ...     ...                           \
  depth ≈ log n                        qs(n-2)
                                          ...
                                      depth ≈ n
```

| Case | Pivot behaviour | Time |
| --- | --- | --- |
| **Best** | lands near the middle every time | O(n log n) |
| **Average** | lands reasonably far from the extremes | O(n log n) |
| **Worst** | lands at an extreme every time | **O(n²)** |

⚠️ **Quick sort's worst case is O(n²), unlike merge sort's guaranteed O(n log n).** On a sorted array, quick sort with a first-element pivot is as slow as Bubble sort. In practice, the standard defences are choosing a **random** pivot or the **median of three** (first, middle, last), which make the worst case extremely unlikely.

### Space Complexity

- **Auxiliary arrays:** none. All swapping happens inside the given array, so quick sort is **in-place**.
- **Recursion stack:** the depth of the tree, which is **O(log n)** in the balanced case and **O(n)** in the degenerate case.

So the extra space is **O(1)** if you ignore the recursion stack, and O(log n) to O(n) if you count it. This is the advantage over merge sort's O(n) `temp` array.

---

## 8. Merge Sort vs Quick Sort

| Feature | Merge Sort | Quick Sort |
| --- | --- | --- |
| Technique | divide and conquer | divide and conquer |
| Real work happens | in `merge`, after recursion (unwinding) | in `partition`, before recursion (winding) |
| Split point | always the middle | wherever the pivot lands |
| Best / Average time | O(n log n) | O(n log n) |
| **Worst time** | **O(n log n)** | **O(n²)** |
| Extra space | **O(n)** `temp` array | **O(1)** array space (plus recursion stack, O(log n) typical) |
| In-place? | ❌ | ✅ |
| Stable? | ✅ (with `<=`) | ❌ |

⚠️ **Quick sort is not stable.** The long-distance swaps in `partition` (and the final pivot swap) can jump an element past an equal one, reordering equal values, the same reason Selection sort isn't stable (Unit I, Lecture 3).

**Which one to use?**

| Situation | Choice | Reason |
| --- | --- | --- |
| Need a guaranteed O(n log n) | **Merge sort** | worst case never degrades |
| Memory is tight, average speed matters | **Quick sort** | no temp array, good constant factors |
| Need stability (equal elements keep their order) | **Merge sort** | quick sort is not stable |
| Input might be already sorted and pivot is the first element | **Merge sort** (or random pivot) | avoids the O(n²) trap |

Both are worth knowing: some problems are naturally solved by the **merge** step (counting inversions, for instance), which is why merge sort is still taught even though quick sort is often faster in practice.

---

## 9. Sorting in Descending Order

The only things that change are the two comparisons inside `partition`. Flip them, and the larger elements go on the left instead:

```
i keeps moving while  arr[i] >= pivot      (stops at first element < pivot)
j keeps moving while  arr[j] <  pivot      (stops at first element >= pivot)
```

Everything else, including the recursion, stays exactly the same.

---

## 10. Common Traps Checklist

| Pitfall | What happens | Fix |
| --- | --- | --- |
| Using `i < high` / `j > low` guards wrongly (no guards at all) | pointer runs off the piece, reads out of bounds | keep `i <= high - 1` and `j >= low + 1` |
| Inconsistent equality handling (`<=` on both pointers, or `>` on both) | infinite loop or elements in the wrong zone | `i` uses `<=`, `j` uses `>` (or mirror both) |
| Swapping the pivot with `arr[i]` at the end | a larger element lands at the front of the left zone | swap with `arr[j]` |
| Swapping when `i >= j` inside the loop | undoes work that was already in the correct zones | only `if (i < j) swap(...)` |
| Recursing on `low…pIndex` or `pIndex…high` | the pivot is included again, so the call may never shrink (infinite recursion) | recurse on `low…pIndex-1` and `pIndex+1…high` |
| Missing the `low < high` check | recursion never stops | guard the whole body with `low < high` |
| Passing the array by value | the original stays unsorted | pass `vector<int> &arr` |
| Assuming O(n log n) always | an already-sorted input with a first-element pivot is O(n²) | use a random or median-of-three pivot |
| Expecting stability | equal elements may be reordered | use merge sort when order of equals matters |

---

## 11. Summary: Lecture 4

| Concept | Meaning |
| --- | --- |
| Quick sort | pick a pivot, place it at its correct sorted position (smaller left, larger right), recurse on both sides |
| Why it sorts | every pivot placed is final; repeating covers every element |
| Pivot | any element works; here, the first element `arr[low]` |
| Partition | two pointers: `i` finds an element `> pivot`, `j` finds an element `<= pivot`; swap them; stop when they cross |
| Final step | swap the pivot with `arr[j]`; return `j` as the partition index |
| Boundary guards | `i <= high - 1`, `j >= low + 1` stop the pointers leaving the piece |
| Equal elements | go on the left here (`<=` for `i`, `>` for `j`); keep the two conditions consistent |
| Recursion | `quickSort(low, p - 1)` and `quickSort(p + 1, high)` only if `low < high`; no merge step |
| Work happens | during winding (partition), the opposite of merge sort |
| Time complexity | best/average **O(n log n)**, worst **O(n²)** (sorted input with a first-element pivot) |
| Space complexity | **O(1)** array space (in-place); recursion stack O(log n) typical, O(n) worst |
| Stability | not stable |
| Descending order | flip the two comparisons in `partition` |

**The single biggest idea from this lecture:** quick sort sorts by repeatedly fixing one element, the pivot, into its final position and splitting the rest around it. It needs no extra array, because the work is done by swapping inside the array, but because the split point depends on the data, a bad pivot choice can degrade it from O(n log n) to O(n²). Merge sort buys a guaranteed O(n log n) with O(n) extra memory, and quick sort buys in-place speed with a risk.

---