# Lecture 3: Merge Sort

Status: Done

[Merge Sort | Practice | GeeksforGeeks](https://www.geeksforgeeks.org/problems/merge-sort/1)

## Quick Recap Before We Begin

In Unit I we built three sorting algorithms (Bubble, Selection, Insertion). All three cost **O(n²)** in the worst case, which is why we said they suit only small or nearly sorted data. In Recursion Level 1 we learned that a recursive function can call itself **twice**, and that the first call's whole branch finishes before the second call starts (Lecture 5, Fibonacci). In Level 2 we learned the "do work, recurse, undo" shape of backtracking.

Today those ideas pay off: **Merge Sort** is a recursive sorting algorithm that runs in **O(n log n)**, a huge improvement over O(n²). It is also the first time we see a recursion whose *useful work happens on the way back* (during unwinding).

---

## 1. The Core Idea: Divide and Merge

Merge sort is built from just two words:

| Step | What it does |
| --- | --- |
| **Divide** | split the array into two halves, again and again, until each piece has a single element |
| **Merge** | combine two already-sorted pieces into one bigger sorted piece, again and again, until the whole array is one sorted piece |

**Why does this work?** A single element is always sorted (nothing to be out of order with). Merging two sorted pieces is easy and cheap (Section 3). So if we keep dividing until everything is a single element, and then keep merging back up, every merge is combining two sorted pieces, and the final result is a fully sorted array.

**Real-life analogy:** Sorting a huge pile of exam papers by roll number. Instead of sorting all 900 papers yourself, you split the pile between two friends, who each split their half again, and so on, until everyone holds just **one paper** (trivially sorted). Then the merging starts: two people holding one paper each combine into a sorted pair, two sorted pairs combine into a sorted four, and so on back up to the full sorted pile. Nobody ever has to sort a big pile from scratch, they only ever *merge* two already-sorted piles.

---

## 2. The Divide Phase: Splitting an Odd-Sized Array

Take this array of 9 elements:

```
Index:     0     1     2     3     4     5     6     7     8
        ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐
arr  =  │  3  │  1  │  2  │  4  │  1  │  5  │  2  │  6  │  4  │
        └─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┘
```

Nine is odd, so it can't split into two equal halves. Two choices exist: `4 + 5` or `5 + 4`. Either is fine, **as long as you follow the same pattern every time**. We use the bigger half on the left (`5 + 4`).

```
                      [3 1 2 4 1 5 2 6 4]
                       /                \
              [3 1 2 4 1]              [5 2 6 4]
               /       \                /      \
          [3 1 2]     [4 1]         [5 2]      [6 4]
           /   \       /  \          /  \       /  \
        [3 1]  [2]   [4]  [1]      [5]  [2]   [6]  [4]
        /  \
      [3]  [1]
```

We stop dividing the moment a piece has **one element**, since a single element can't be divided any further.

⚠️ **"Hypothetically" divides.** We never actually build new smaller arrays. The pieces are just *ranges of indices* into the one original array. This is the key to writing the code (Section 4).

---

## 3. The Merge Phase: Combining Two Sorted Pieces

Merging is the heart of the algorithm. Suppose two neighbouring pieces are **each already sorted**:

```
left piece  = [1, 1, 2, 3, 4]      right piece = [2, 4, 5, 6]
```

We use **two pointers**, one at the front of each piece, plus an empty `temp` array that collects the result. At each step, **take the smaller of the two front elements** and advance only that pointer.

```
left  = [ 1 | 1 | 2 | 3 | 4 ]        right = [ 2 | 4 | 5 | 6 ]
          ↑                                    ↑
          left ptr                           right ptr

temp = [ ]
```

The picture after each comparison, with `^` marking the pointer positions:

```
compare 1 vs 2 → take 1 (left)     temp = [1]
compare 1 vs 2 → take 1 (left)     temp = [1, 1]
compare 2 vs 2 → take 2 (left)     temp = [1, 1, 2]      ← tie: take LEFT
compare 3 vs 2 → take 2 (right)    temp = [1, 1, 2, 2]
compare 3 vs 4 → take 3 (left)     temp = [1, 1, 2, 2, 3]
compare 4 vs 4 → take 4 (left)     temp = [1, 1, 2, 2, 3, 4]
left piece is now EXHAUSTED → copy everything left in the right piece:
                                   temp = [1, 1, 2, 2, 3, 4, 4, 5, 6]
```

Two rules fall out of this:

1. **Keep comparing while both pieces still have elements.** Take the smaller front element each time.
2. **When one piece runs out, the other piece's remaining elements are already sorted and larger than everything taken so far**, so just copy them over, no more comparisons needed.

This is exactly the two-pointer merge from Unit I, Lecture 2 (Section 9.2), now being used as a building block.

---

## 4. From the Idea to the Code

### Replacing "New Arrays" With `low`, `mid`, `high`

Since pieces are index ranges, every call to `mergeSort` receives:

| Variable | Meaning |
| --- | --- |
| `low` | first index of the current piece |
| `high` | last index of the current piece |
| `mid` | the split point: left piece is `low … mid`, right piece is `mid + 1 … high` |

```
mid = (low + high) / 2
```

For the whole 9-element array, `low = 0`, `high = 8`, so `mid = 4`: left piece = indices `0..4` (5 elements), right piece = `5..8` (4 elements). This matches the 5 + 4 split from Section 2.

### The Recursive Structure

Each call does three things, in this order:

1. **Sort the left piece** (`low … mid`), by a recursive call.
2. **Sort the right piece** (`mid + 1 … high`), by a recursive call.
3. **Merge** the two now-sorted pieces.

### The Base Case

A piece with a single element (`low == high`) is already sorted, so we stop. We write `low >= high` for safety, so a piece that is somehow empty also stops.

```cpp
if (low >= high) return;
```

### The Code

```cpp
void merge(vector<int> &arr, int low, int mid, int high) {
    vector<int> temp;                     // collects the merged result
    int left = low;                       // pointer into the left piece
    int right = mid + 1;                  // pointer into the right piece

    while (left <= mid && right <= high) {      // both pieces still have elements
        if (arr[left] <= arr[right]) {
            temp.push_back(arr[left]);
            left++;
        } else {
            temp.push_back(arr[right]);
            right++;
        }
    }

    while (left <= mid) {                 // left piece leftovers
        temp.push_back(arr[left]);
        left++;
    }
    while (right <= high) {               // right piece leftovers
        temp.push_back(arr[right]);
        right++;
    }

    for (int i = low; i <= high; i++) {   // copy back into the original array
        arr[i] = temp[i - low];
    }
}

void mergeSort(vector<int> &arr, int low, int high) {
    if (low >= high) return;              // base case: 0 or 1 element

    int mid = (low + high) / 2;
    mergeSort(arr, low, mid);             // sort the left piece
    mergeSort(arr, mid + 1, high);        // sort the right piece
    merge(arr, low, mid, high);           // merge the two sorted pieces
}

int main() {
    vector<int> arr = {3, 1, 2, 4, 1, 5, 2, 6, 4};
    int n = arr.size();
    mergeSort(arr, 0, n - 1);             // whole array: low = 0, high = n - 1
    for (int x : arr) cout << x << " ";   // 1 1 2 2 3 4 4 5 6
}
```

### Why the Copy-Back Uses `temp[i - low]`

`temp` always starts filling at its own index `0`, but the piece we merged lives at `low … high` in `arr`. So position `i` in `arr` corresponds to position `i - low` in `temp`:

```
arr  indices:   low   low+1   low+2  ...  high
temp indices:    0     1       2     ...  high - low
```

⚠️ **Forgetting `- low` is a classic bug.** For the left-most piece (`low = 0`) it happens to work, so the code looks correct on small tests but breaks for every piece that doesn't start at index 0.

---

## 5. Understanding the Recursion: The Call Tree and Merge Order

Use a smaller array of 5 elements, `[3, 1, 2, 4, 1]`, indices `0..4`. Each node is `mergeSort(low, high)`:

```
                         ms(0,4)
                        /        \
                 ms(0,2)          ms(3,4)
                 /     \          /     \
           ms(0,1)    ms(2,2)  ms(3,3)  ms(4,4)
           /     \
      ms(0,0)  ms(1,1)
```

Leaves are the base case (`low == high`): they return immediately and do nothing.

### The Order Things Actually Happen In

Because the **left call comes first in the code**, the entire left subtree finishes before the right subtree starts (the Level 1, Lecture 5 rule). And because `merge` is placed **after** both recursive calls, each merge runs during **unwinding**, only after both of its children are fully sorted.

| Order | Merge performed | What it combines |
| --- | --- | --- |
| 1st | `merge(0, 0, 1)` | `[3]` + `[1]` → `[1, 3]` |
| 2nd | `merge(0, 1, 2)` | `[1, 3]` + `[2]` → `[1, 2, 3]` |
| 3rd | `merge(3, 3, 4)` | `[4]` + `[1]` → `[1, 4]` |
| 4th | `merge(0, 2, 4)` | `[1, 2, 3]` + `[1, 4]` → `[1, 1, 2, 3, 4]` |

```
Divide goes DOWN the tree  ──►  merges happen on the way back UP

          merge #4   (0..4)   ◄── last, combines the two big halves
          /        \
   merge #2 (0..2)  merge #3 (3..4)
       /
   merge #1 (0..1)
```

**Real-life analogy:** The same exam-paper pile. The work of *splitting* is trivial (just pointing at ranges). All the real sorting effort happens as the sorted sub-piles are handed back up and merged.

⚠️ **This is the Level 1, Lecture 2 idea again.** Work placed *after* the recursive calls runs during unwinding. In Lecture 2 that work was `cout`, in subsequence problems it was `pop_back()`, and here it is `merge()`.

---

## 6. Complexity Analysis

### Time Complexity: O(n log n)

Two ingredients:

**1. How many levels of division?** The array is halved each time, so the number of levels is about **log₂ n** (same halving argument as binary search).

```
n = 16 :   16 → 8 → 4 → 2 → 1        4 halvings = log₂ 16
```

**2. How much work does each level do?** At any one level, the merges together touch every element about once. Each merge costs roughly the size of the piece it combines, and the pieces at one level add up to `n`.

```
level 0:        1 merge  of size n          → n work
level 1:        2 merges of size n/2        → n work
level 2:        4 merges of size n/4        → n work
   ...
level log n:    n merges of size 1          → (nothing to compare)
```

```
Total = (work per level) × (number of levels) = n × log n = O(n log n)
```

Merge sort does the same amount of work whatever the input looks like, so:

| Case | Time |
| --- | --- |
| **Best** | O(n log n) |
| **Average** | O(n log n) |
| **Worst** | O(n log n) |

Unlike Bubble and Insertion sort, there is no input that makes it faster, and none that makes it slower.

### Space Complexity: O(n)

Each `merge` creates a `temp` array of up to the size of the piece being merged. At the top level that is `n` elements, so the extra space is **O(n)**. (The recursion also uses O(log n) stack space, which is smaller and doesn't change the answer.)

⚠️ **Merge sort is not in-place.** That `temp` array is the price paid for the speed. This is the usual time-space trade-off from Unit I, Lecture 1: here we spend O(n) extra memory to get O(n log n) time instead of O(n²).

---

## 7. Properties and a Comparison With the Earlier Sorts

**Stability:** merge sort is **stable**, *provided* the merge uses `<=` (take from the left on a tie, as in our code). With `<`, equal elements from the right piece would be taken first and their original order would flip.

⚠️ **`<=`, not `<`.** A single character decides whether equal elements keep their original order.

| Property | Bubble | Selection | Insertion | **Merge** |
| --- | --- | --- | --- | --- |
| Best case | O(n)* | O(n²) | O(n) | **O(n log n)** |
| Worst case | O(n²) | O(n²) | O(n²) | **O(n log n)** |
| Extra space | O(1) | O(1) | O(1) | **O(n)** |
| Stable? | ✅ | ❌ | ✅ | ✅ |
| In-place? | ✅ | ✅ | ✅ | ❌ |
| Technique | adjacent swaps | select minimum | insert into sorted prefix | **divide and merge (recursion)** |
- with the early-exit flag.

**When to prefer which:** for small or nearly sorted arrays, Insertion sort is simple and fast. For large arrays, or whenever you need a *guaranteed* O(n log n), merge sort is the right tool.

---

## 8. Common Traps Checklist

| Pitfall | What happens | Fix |
| --- | --- | --- |
| Missing base case `low >= high` | infinite recursion, stack overflow | stop when the piece has 0 or 1 element |
| Calling `merge` before both recursive calls | merging unsorted pieces, wrong output | order is: left call → right call → merge |
| Right piece starting at `mid` instead of `mid + 1` | element `mid` is counted in both pieces; recursion may never shrink | left = `low…mid`, right = `mid + 1…high` |
| Forgetting the two leftover loops in `merge` | elements silently dropped | after the main loop, copy whatever remains of either piece |
| Copy-back with `temp[i]` instead of `temp[i - low]` | wrong values for pieces not starting at index 0 | use `temp[i - low]` |
| Using `<` instead of `<=` in the merge comparison | sort becomes unstable | take from the left on a tie |
| Passing the array by value | changes are made to a copy, original stays unsorted | pass `vector<int> &arr` |
| Initial call with `high = n` | out-of-bounds read | initial call is `mergeSort(arr, 0, n - 1)` |
| `(low + high) / 2` on huge index ranges | integer overflow | `low + (high - low) / 2` (Unit I, Lecture 4, Section 14) |

---

## 9. Summary: Lecture 3

| Concept | Meaning |
| --- | --- |
| Merge sort | recursive sorting algorithm: **divide** into halves, then **merge** sorted halves |
| Why it works | a single element is sorted; merging two sorted pieces is cheap |
| Dividing | done with index ranges `low`, `mid`, `high`, never by building new arrays |
| `mid` | `(low + high) / 2`; left = `low…mid`, right = `mid + 1…high` |
| Base case | `low >= high`: a piece of 0 or 1 element is already sorted |
| Recursive order | sort left → sort right → merge |
| Merge | two pointers, take the smaller front element, then copy leftovers of whichever piece remains |
| Merge happens | during **unwinding**, after both recursive calls return |
| Copy-back | `arr[i] = temp[i - low]` for `i` from `low` to `high` |
| Time complexity | **O(n log n)** in best, average, and worst case (`log n` levels × `n` work per level) |
| Space complexity | **O(n)** for the `temp` array (not in-place) |
| Stability | stable, if ties are taken from the left (`<=`) |

**The single biggest idea from this lecture:** instead of sorting a big array directly, keep splitting it until the pieces are trivially sorted, then merge sorted pieces back together. Splitting is free; merging two sorted pieces costs only O(size); and halving gives only `log n` levels. That combination turns the O(n²) of the simple sorts into **O(n log n)**, at the price of O(n) extra memory.

---