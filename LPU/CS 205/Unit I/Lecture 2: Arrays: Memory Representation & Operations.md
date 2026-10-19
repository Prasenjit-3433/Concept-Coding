# Lecture 2: Arrays: Memory Representation & Operations

Date: August 12, 2026
Status: Done

## Quick Recap Before We Begin

In Lecture 1 we built the yardstick: time complexity, space complexity, and the Big O / Omega / Theta notations, plus the idea that every data structure trades something (memory, flexibility, bookkeeping) for speed on the operations we care about. Today we put that yardstick to work for the first time, on the simplest and most widely used structure: the **array**.

We'll cover, in order: what a linear array is, how it sits in **memory** (and why that makes access O(1)), then the core operations: **traversal, insertion, deletion, searching, sorting, and merging**, with a complexity analysis of each. Searching and sorting get a complexity preview here; the algorithms themselves are Lectures 3 and 4.

---

## 1. What is a Linear Array?

A **linear array** is a collection of elements of the **same data type**, stored in **consecutive memory locations**, where each element is identified by an **index** (also called a subscript).

**Real-life analogy:** A **row of numbered parking slots** (Lecture 1's analogy). Every slot is the same size, the slots sit side by side, and each has a number painted on it. To find the car in slot 4, you walk straight to slot 4 without checking slots 0 to 3 first.

```
Index:     0     1     2     3     4
        ┌─────┬─────┬─────┬─────┬─────┐
arr  =  │ 10  │ 20  │ 30  │ 40  │ 50  │
        └─────┴─────┴─────┴─────┴─────┘
```

### Terminology

| Term | Meaning | Example (above) |
| --- | --- | --- |
| **Element** | one stored value | `30` |
| **Index / Subscript** | position of an element | `30` is at index `2` |
| **Lower bound (LB)** | smallest valid index | `0` in C++ |
| **Upper bound (UB)** | largest valid index | `4` |
| **Length / Size** | number of elements | `UB - LB + 1 = 5` |
| **Capacity** | slots reserved, which may exceed the elements actually in use | e.g. `10` |

⚠️ **Size vs capacity is the distinction this whole lecture leans on.** An array declared as `int arr[10]` has capacity 10 even if only 4 slots hold meaningful data. Insertion and deletion change the *number of elements in use* (`n`), never the capacity.

**C++ uses 0-based indexing:** `LB = 0`, so the length is `UB + 1`. Some languages (and many textbook formulas) allow other lower bounds, which is why the general formula below includes `LB`.

---

## 2. Memory Representation of a Linear Array

The word **consecutive** is the whole story. When you declare an array, the system reserves **one unbroken block** of memory and places the elements back to back.

If each element needs `w` bytes (for `int`, typically `w = 4`), then:

```
Address of element i  =  Base + i × w
```

where `Base` is the address of the first element (`arr[0]`).

```
Base address = 1000,   w = 4 bytes (int)

 Address:   1000    1004    1008    1012    1016
          ┌───────┬───────┬───────┬───────┬───────┐
          │  10   │  20   │  30   │  40   │  50   │
          └───────┴───────┴───────┴───────┴───────┘
 Index:      0       1       2       3       4

 arr[3] lives at  1000 + 3 × 4 = 1012  ✅
```

### The General Formula (any lower bound)

For an array `A[LB ... UB]`:

```
LOC(A[k])  =  Base + w × (k − LB)
```

**Worked example:** `A[5 ... 14]`, base address `200`, each element `2` bytes. Find the address of `A[9]`, and the length.

```
Length     = UB − LB + 1 = 14 − 5 + 1 = 10

LOC(A[9])  = 200 + 2 × (9 − 5)
           = 200 + 2 × 4
           = 208
```

With `LB = 0` (C++), `k − LB` collapses to `k`, which gives the simple `Base + i × w`.

### Why This Makes Access O(1)

Look at the formula again: **one multiplication and one addition**, regardless of whether the array has 10 or 10 million elements. The computer never walks past other elements. It computes the address and jumps there.

**Real-life analogy:** House number 4 on a numbered street. You don't knock on doors 1, 2, and 3 to find it. You do the arithmetic and walk straight there. This is called **random access** (or direct access), and it's the array's defining strength.

⚠️ **The same layout that gives O(1) access causes every weakness of arrays.** Because elements must stay consecutive, you can't squeeze a new element into the middle without moving others, and you can't grow past your reserved block without finding a bigger one. Keep this in mind through the insertion and deletion sections: they are costly *because of* the contiguity.

### Array Names Are Addresses (Preview of Pointers)

In C++, the array name is essentially the address of its first element:

```
arr  →  address of arr[0]
arr[i]  is equivalent to  *(arr + i)
```

`arr + i` automatically scales by `sizeof(int)`, so it's exactly the `Base + i × w` formula, done by the compiler. This is also why an array passed to a function arrives as a plain pointer, with no size attached (Section 3).

---

## 3. Declaring and Using Arrays in C++

```cpp
#include <iostream>
using namespace std;

int main() {
    int a[5];                          // 5 slots, values uninitialized (garbage)
    int b[5] = {10, 20, 30, 40, 50};   // fully initialized
    int c[5] = {10, 20};               // rest become 0 → {10, 20, 0, 0, 0}
    int d[5] = {};                     // all zeros
    int e[] = {7, 8, 9};               // size deduced as 3

    cout << b[2] << endl;              // 30
    b[2] = 99;                         // update: O(1)

    return 0;
}
```

### Finding the Length

```cpp
int b[5] = {10, 20, 30, 40, 50};
int length = sizeof(b) / sizeof(b[0]);   // total bytes / bytes per element = 5
```

⚠️ **This `sizeof` trick only works where the array was declared.** Once the array is passed to a function, it decays to a pointer, and `sizeof(arr)` inside that function returns the size of a *pointer* (typically 8 bytes), not the array. That is why every function in this lecture takes the size `n` as a **separate parameter**:

```cpp
void printArray(int arr[], int n) {   // arr is really an int*
    // sizeof(arr) here is the size of a pointer. Do NOT use it for length.
}
```

⚠️ **C++ does not check array bounds.** Writing `arr[10]` on a size-5 array compiles fine and corrupts neighboring memory, or crashes. Valid indices are `0` to `n - 1`, and staying inside that range is entirely your responsibility.

### Static vs Dynamic Sizing

| Kind | Declaration | Size decided | Resizable? |
| --- | --- | --- | --- |
| **Static array** | `int arr[100];` | at compile time | no |
| **Dynamic array** | `int* arr = new int[n];` (remember `delete[] arr;`) | at runtime | not in place; must allocate a new, bigger block and copy |
| **`vector<int>`** | `vector<int> v;` | at runtime | yes, automatically (it does the allocate-and-copy internally) |

In this lecture we use **static arrays with a manually tracked count `n`**, because that exposes exactly what insertion and deletion really cost.

---

## 4. Traversal

**Traversal** means visiting every element exactly once, in index order. It's the base operation behind printing, summing, counting, finding the maximum, and so on.

```cpp
void traverse(int arr[], int n) {
    for (int i = 0; i < n; i++) {
        cout << arr[i] << " ";
    }
    cout << endl;
}
```

A typical use is aggregating while traversing:

```cpp
int findMax(int arr[], int n) {
    int maxVal = arr[0];
    for (int i = 1; i < n; i++) {
        if (arr[i] > maxVal) maxVal = arr[i];
    }
    return maxVal;
}
```

**Real-life analogy:** Walking down the row of parking slots and noting each car as you pass: you must pass every slot once, so the work grows with the length of the row.

**Time complexity:** **O(n)**, since each of the `n` elements is touched once.

**Space complexity:** **O(1)**, with only the loop variable (and `maxVal`) as extra storage.

---

## 5. Insertion

Insertion adds a new element at a chosen position. Because elements must stay **consecutive**, making room means **shifting** the later elements one slot to the right.

For these examples, the array has capacity `6` and currently holds `n = 4` elements:

```
Index:     0     1     2     3     4     5
        ┌─────┬─────┬─────┬─────┬─────┬─────┐
arr  =  │ 10  │ 20  │ 30  │ 40  │  _  │  _  │      n = 4, capacity = 6
        └─────┴─────┴─────┴─────┴─────┴─────┘
```

### 5.1 Insertion at the End

The easy case: if there's spare capacity, write into slot `n` and increase the count. No shifting.

```cpp
int insertAtEnd(int arr[], int n, int capacity, int value) {
    if (n == capacity) {            // overflow: no room left
        cout << "Array is full" << endl;
        return n;
    }
    arr[n] = value;
    return n + 1;                    // return the new element count
}
```

```
insertAtEnd(arr, 4, 6, 50):

Before:  [ 10 | 20 | 30 | 40 |  _  |  _ ]
                               ↑
                         arr[n] = arr[4] gets 50

After:   [ 10 | 20 | 30 | 40 | 50 |  _ ]      n = 5
```

**Time complexity:** **O(1)**.

### 5.2 Insertion at a Given Position

To insert `value` at index `pos`, every element from `pos` onward must slide one slot right to open a gap.

**The logic:**

1. If the array is full (`n == capacity`), stop (overflow).
2. If `pos` is outside `0 ... n`, stop (invalid).
3. Shift elements `arr[pos] ... arr[n-1]` one slot to the **right**, starting from the **last** element.
4. Place `value` at `arr[pos]`.
5. Increase the element count.

```cpp
int insertAtPosition(int arr[], int n, int capacity, int pos, int value) {
    if (n == capacity) {
        cout << "Overflow: array is full" << endl;
        return n;
    }
    if (pos < 0 || pos > n) {
        cout << "Invalid position" << endl;
        return n;
    }

    for (int i = n - 1; i >= pos; i--) {   // shift right, starting from the END
        arr[i + 1] = arr[i];
    }

    arr[pos] = value;
    return n + 1;
}
```

The caller reassigns the count, mirroring the "return the updated value" pattern from the linked list notes:

```cpp
n = insertAtPosition(arr, n, 6, 2, 25);
```

**Real-life analogy:** People sitting in a row of chairs, with empty chairs at the far right end. To seat a new person in chair 2, everyone from chair 2 onward must shift one seat right, and **the person at the far end moves first**, otherwise they'd sit on top of someone.

```
Insert 25 at position 2   (n = 4, capacity = 6)

Start:     [ 10 | 20 | 30 | 40 |  _  |  _ ]
                       ↑    ↑
                      pos  n-1

Step 1:  arr[4] = arr[3]     (40 moves right)
           [ 10 | 20 | 30 | 40 | 40 |  _ ]

Step 2:  arr[3] = arr[2]     (30 moves right)
           [ 10 | 20 | 30 | 30 | 40 |  _ ]
                       ↑
                 now a "hole" at pos (30 is duplicated; safe to overwrite)

Step 3:  arr[2] = 25         (new value placed)
           [ 10 | 20 | 25 | 30 | 40 |  _ ]      n = 5
```

⚠️ **Direction matters: insertion shifts from the RIGHT end toward `pos`.** If you shift left-to-right instead (`for i = pos ... n-1`), the first assignment `arr[pos+1] = arr[pos]` overwrites `arr[pos+1]` before it has been moved, and every element after it becomes a copy of `arr[pos]`. The loop must run backward.

⚠️ **Two checks, not one.** `n == capacity` (full) and `pos` out of range are *different* failures. Skipping the first writes past the end of the array. Skipping the second leaves a gap of garbage between elements.

### 5.3 Insertion at the Beginning

This is just `insertAtPosition` with `pos = 0`, and it is the **worst case**, since *every* element must shift.

```
Insert 5 at position 0:

Start:   [ 10 | 20 | 30 | 40 |  _ |  _ ]
Result:  [  5 | 10 | 20 | 30 | 40 |  _ ]      all 4 elements shifted
```

### Complexity of Insertion

The cost is driven by the **number of shifts** (`n - pos`):

| Case | Where | Shifts | Complexity |
| --- | --- | --- | --- |
| **Best** | at the end (`pos = n`) | 0 | **O(1)** |
| **Worst** | at the beginning (`pos = 0`) | `n` | **O(n)** |
| **Average** | any position, equally likely | about `n/2` | **O(n)** |

The average works out because the positions `0 ... n` are equally likely, so the shift counts `n, n-1, ..., 0` average to `n/2`, and dropping the constant `1/2` leaves **O(n)**.

**Space complexity:** **O(1)** auxiliary, since the shifting happens in place.

**Compare with Unit II:** a linked list inserts at the head in O(1) with no shifting, by re-linking pointers. This is exactly the array-vs-list trade-off from Lecture 1: the array pays for O(1) access with O(n) insertion.

---

## 6. Deletion

Deletion removes an element and **closes the gap** by shifting later elements one slot to the **left**.

### 6.1 Deletion at the End

No shifting needed: just decrease the count. The old value stays physically in memory but is no longer counted, so it will never be read and will be overwritten by a later insertion.

```cpp
int deleteAtEnd(int arr[], int n) {
    if (n == 0) {
        cout << "Underflow: array is empty" << endl;
        return n;
    }
    return n - 1;
}
```

**Time complexity:** **O(1)**.

### 6.2 Deletion at a Given Position

**The logic:**

1. If the array is empty, stop (underflow).
2. If `pos` is outside `0 ... n-1`, stop (invalid).
3. Shift elements `arr[pos+1] ... arr[n-1]` one slot to the **left**, starting from the **front**.
4. Decrease the element count.

```cpp
int deleteAtPosition(int arr[], int n, int pos) {
    if (n == 0) {
        cout << "Underflow: array is empty" << endl;
        return n;
    }
    if (pos < 0 || pos >= n) {
        cout << "Invalid position" << endl;
        return n;
    }

    for (int i = pos; i < n - 1; i++) {    // shift left, starting from pos
        arr[i] = arr[i + 1];
    }

    return n - 1;
}
```

```
Delete the element at position 1   (n = 5)

Start:     [ 10 | 20 | 30 | 40 | 50 ]
                  ↑
                 pos  (20 will be overwritten)

Step 1:  arr[1] = arr[2]     (30 moves left)
           [ 10 | 30 | 30 | 40 | 50 ]

Step 2:  arr[2] = arr[3]     (40 moves left)
           [ 10 | 30 | 40 | 40 | 50 ]

Step 3:  arr[3] = arr[4]     (50 moves left)
           [ 10 | 30 | 40 | 50 | 50 ]

Count becomes n - 1 = 4, so the trailing duplicate 50 at index 4 is
now outside the "in use" region and is ignored.

Result:  [ 10 | 30 | 40 | 50 ]      n = 4
```

**Real-life analogy:** The person in chair 1 leaves, so everyone to their right slides one seat left to close the gap, **starting with the person immediately next to the gap**, so each one moves into a seat that has just been vacated.

⚠️ **Direction matters here too, and it's the opposite of insertion.** Insertion shifts **right-to-left in iteration order** (last element first). Deletion shifts **left-to-right in iteration order** (nearest-the-gap first). Running either loop in the wrong direction overwrites values before they've been moved.

⚠️ **`i < n - 1`, not `i < n`.** The loop reads `arr[i + 1]`. Letting `i` reach `n - 1` reads `arr[n]`, one past the last valid element: a classic off-by-one.

### Complexity of Deletion

| Case | Where | Shifts | Complexity |
| --- | --- | --- | --- |
| **Best** | last element | 0 | **O(1)** |
| **Worst** | first element (`pos = 0`) | `n - 1` | **O(n)** |
| **Average** | any position, equally likely | about `n/2` | **O(n)** |

**Space complexity:** **O(1)**.

### Deleting by Value

If you're asked to delete a *value* instead of a *position*, you first have to **find** it (a search, O(n)), then delete by position (O(n) shifting). Total: **O(n) + O(n) = O(n)**.

---

## 7. Searching

**Searching** finds the position of a given value in the array. This lecture covers the *complexity picture*; full code and walkthroughs for each algorithm come in **Lecture 4**.

| Algorithm | Requirement | How it works | Best | Worst |
| --- | --- | --- | --- | --- |
| **Linear search** | none | check elements one by one | O(1) | **O(n)** |
| **Binary search** | array must be **sorted** | compare with the middle, discard half each step | O(1) | **O(log n)** |

**Real-life analogy:** Finding a name in a phone book. **Linear search** is reading from page one. **Binary search** is opening the middle and deciding which half to keep, which only works because the book is *sorted*.

```
Binary search for 40 in a sorted array of 8:

[ 5 | 12 | 18 | 25 | 40 | 52 | 67 | 80 ]
                    ↑ mid: 25 < 40 → discard left half

                  [ 40 | 52 | 67 | 80 ]
                       ↑ mid: continue in the remaining half

8 elements → 4 → 2 → 1 : about log₂(8) = 3 steps
```

⚠️ **Binary search is only valid on sorted data**, and the O(1) random access from Section 2 is what makes jumping to the middle possible. A linked list can't do this efficiently, because reaching the middle takes O(n) there. This is one concrete payoff of the array's contiguous layout.

---

## 8. Sorting

**Sorting** rearranges the elements into a defined order (ascending or descending). Again, a preview for complexity, with the algorithms in **Lecture 3**.

| Algorithm | Idea | Best | Worst | Extra space |
| --- | --- | --- | --- | --- |
| **Bubble sort** | repeatedly swap adjacent out-of-order pairs | O(n) (with early exit) | O(n²) | O(1) |
| **Selection sort** | repeatedly pick the minimum and place it | O(n²) | O(n²) | O(1) |
| **Insertion sort** | grow a sorted prefix, inserting each new element into place | O(n) | O(n²) | O(1) |

**Real-life analogy:** Arranging a hand of playing cards. Picking up cards one by one and sliding each into its correct place in the cards you already hold is exactly **insertion sort**, which also uses the same "shift right to make room" move you saw in Section 5.2.

⚠️ **Sorting is not free, but it can pay for itself.** An O(n²) sort followed by many O(log n) binary searches beats repeated O(n) linear searches *if you search enough times*. Whether to sort first is a trade-off decision, the same kind we discussed in Lecture 1.

---

## 9. Merging

**Merging** combines two arrays into one. There are two situations, with very different costs.

### 9.1 Merging Two Unsorted Arrays (Concatenation)

Just copy one after the other. The result isn't sorted, and it doesn't need to be.

```cpp
void concatenate(int a[], int n, int b[], int m, int c[]) {
    for (int i = 0; i < n; i++) c[i] = a[i];
    for (int j = 0; j < m; j++) c[n + j] = b[j];
}
```

**Time:** O(n + m). **Space:** O(n + m) for the result array `c`.

### 9.2 Merging Two Sorted Arrays (the `Two-Pointer` Merge)

When both inputs are **already sorted**, we can produce a **sorted** result in a single pass, without sorting from scratch. We keep one pointer into each array and always take the **smaller** of the two current elements.

**Real-life analogy:** Two **already-sorted queues** of people, merging into one line ordered by height. At every step, compare the two people at the *front* of each queue, and let the shorter one go next. You never need to look past the fronts.

**The setup:**

- `i`: current position in `a` (length `n`)
- `j`: current position in `b` (length `m`)
- `k`: next free position in the result `c`

**The logic:**

1. While **both** arrays have elements left: copy the smaller of `a[i]` and `b[j]` into `c[k]`, and advance that array's pointer and `k`.
2. When one array runs out, copy **all remaining** elements of the other.

```cpp
void mergeSorted(int a[], int n, int b[], int m, int c[]) {
    int i = 0, j = 0, k = 0;

    while (i < n && j < m) {
        if (a[i] <= b[j]) {
            c[k++] = a[i++];
        } else {
            c[k++] = b[j++];
        }
    }

    while (i < n) {          // leftovers from a (if b ran out first)
        c[k++] = a[i++];
    }
    while (j < m) {          // leftovers from b (if a ran out first)
        c[k++] = b[j++];
    }
}
```

```
a = [ 2 | 5 | 9 ]      b = [ 1 | 5 | 7 | 10 ]       c = [ _ _ _ _ _ _ _ ]
      i                      j                              k

Compare a[i]=2 vs b[j]=1 → take 1 (from b):   c = [ 1 | _ | _ | _ | _ | _ | _ ]
Compare a[i]=2 vs b[j]=5 → take 2 (from a):   c = [ 1 | 2 | _ | _ | _ | _ | _ ]
Compare a[i]=5 vs b[j]=5 → tie, take a's 5:   c = [ 1 | 2 | 5 | _ | _ | _ | _ ]
Compare a[i]=9 vs b[j]=5 → take 5 (from b):   c = [ 1 | 2 | 5 | 5 | _ | _ | _ ]
Compare a[i]=9 vs b[j]=7 → take 7 (from b):   c = [ 1 | 2 | 5 | 5 | 7 | _ | _ ]
Compare a[i]=9 vs b[j]=10 → take 9 (from a):  c = [ 1 | 2 | 5 | 5 | 7 | 9 | _ ]
a is exhausted → copy leftovers of b (just 10): c = [ 1 | 2 | 5 | 5 | 7 | 9 | 10 ]
```

⚠️ **Why `<=` and not `<` in the comparison:** on a tie, taking from `a` first keeps equal elements in their original relative order (a *stable* merge). With `<`, ties would come from `b` first and flip that order. It never changes sortedness, but it matters whenever elements carry extra data.

⚠️ **The two cleanup loops aren't optional.** The main loop stops the moment *either* array runs out. Whatever remains in the other array is already sorted and larger than everything copied so far, so it just needs copying. Forgetting these loops silently drops elements.

**Time complexity:** **O(n + m)**. Each element of `a` and `b` is copied exactly once, and `i`, `j` never move backward.

**Space complexity:** **O(n + m)** for the result array `c`.

### Why Not Just Concatenate and Sort?

| Approach | Time |
| --- | --- |
| Concatenate, then sort the whole thing (with an O(N log N) sort, where N = n + m) | O((n + m) log(n + m)) |
| Two-pointer merge | **O(n + m)** |

The merge exploits the fact that both inputs are *already* sorted, which saves a full `log` factor. The same two-pointer idea is the core step of **merge sort**, an efficient sorting algorithm you'll meet in later courses.

---

## 10. Complexity Summary: Arrays at a Glance

| Operation | Best | Average | Worst | Why |
| --- | --- | --- | --- | --- |
| Access `arr[i]` | O(1) | O(1) | O(1) | address arithmetic: `Base + i × w` |
| Update `arr[i] = x` | O(1) | O(1) | O(1) | same direct access |
| Traversal | O(n) | O(n) | O(n) | touch every element once |
| Insert at end | O(1) | O(1) | O(1) | no shifting (needs spare capacity) |
| Insert at position | O(1) | O(n) | O(n) | shift up to `n` elements right |
| Delete at end | O(1) | O(1) | O(1) | just decrease the count |
| Delete at position | O(1) | O(n) | O(n) | shift up to `n - 1` elements left |
| Linear search | O(1) | O(n) | O(n) | may check every element |
| Binary search | O(1) | O(log n) | O(log n) | halves the range; needs sorted data |
| Sorting (bubble/selection/insertion) | O(n)* | O(n²) | O(n²) | *only bubble/insertion with early exit |
| Merge two sorted arrays | O(n + m) | O(n + m) | O(n + m) | one pass over both |

**Important teaching point:** an array is **superb at reading and terrible at structural change**. Everything that needs to *find a location* by index is O(1), while everything that changes *where elements sit* is O(n). That single sentence predicts almost every row above.

---

## 11. Limitations of Arrays: The Motivation for Unit II

| Limitation | Consequence |
| --- | --- |
| **Fixed size** (for static arrays) | you must guess the capacity in advance. Too small and you overflow, too large and memory is wasted |
| **Contiguous block required** | even if enough total memory is free, you may fail to find one unbroken chunk big enough |
| **Costly insertion/deletion in the middle** | O(n) shifting, as shown in Sections 5 and 6 |
| **Wasted capacity** | reserved-but-unused slots sit idle for the lifetime of the array |

These are exactly the pain points **linked lists** (Unit II) were designed to fix: nodes scattered anywhere in memory, no shifting, size that grows one node at a time, at the price of losing O(1) indexed access. As Lecture 1 said, no structure wins everywhere.

---

## 12. Full Working Example

```cpp
#include <iostream>
using namespace std;

void traverse(int arr[], int n) {
    cout << "[ ";
    for (int i = 0; i < n; i++) cout << arr[i] << " ";
    cout << "]" << endl;
}

int insertAtPosition(int arr[], int n, int capacity, int pos, int value) {
    if (n == capacity) {
        cout << "Overflow: array is full" << endl;
        return n;
    }
    if (pos < 0 || pos > n) {
        cout << "Invalid position" << endl;
        return n;
    }
    for (int i = n - 1; i >= pos; i--) {
        arr[i + 1] = arr[i];
    }
    arr[pos] = value;
    return n + 1;
}

int deleteAtPosition(int arr[], int n, int pos) {
    if (n == 0) {
        cout << "Underflow: array is empty" << endl;
        return n;
    }
    if (pos < 0 || pos >= n) {
        cout << "Invalid position" << endl;
        return n;
    }
    for (int i = pos; i < n - 1; i++) {
        arr[i] = arr[i + 1];
    }
    return n - 1;
}

int linearSearch(int arr[], int n, int key) {
    for (int i = 0; i < n; i++) {
        if (arr[i] == key) return i;
    }
    return -1;
}

void mergeSorted(int a[], int n, int b[], int m, int c[]) {
    int i = 0, j = 0, k = 0;
    while (i < n && j < m) {
        if (a[i] <= b[j]) c[k++] = a[i++];
        else              c[k++] = b[j++];
    }
    while (i < n) c[k++] = a[i++];
    while (j < m) c[k++] = b[j++];
}

int main() {
    const int CAPACITY = 8;
    int arr[CAPACITY] = {10, 20, 30, 40};
    int n = 4;

    traverse(arr, n);                          // [ 10 20 30 40 ]

    n = insertAtPosition(arr, n, CAPACITY, 2, 25);
    traverse(arr, n);                          // [ 10 20 25 30 40 ]

    n = insertAtPosition(arr, n, CAPACITY, 0, 5);
    traverse(arr, n);                          // [ 5 10 20 25 30 40 ]

    n = deleteAtPosition(arr, n, 1);
    traverse(arr, n);                          // [ 5 20 25 30 40 ]

    cout << "Index of 30: " << linearSearch(arr, n, 30) << endl;   // 3
    cout << "Index of 99: " << linearSearch(arr, n, 99) << endl;   // -1

    int a[] = {2, 5, 9};
    int b[] = {1, 5, 7, 10};
    int c[7];
    mergeSorted(a, 3, b, 4, c);
    traverse(c, 7);                            // [ 1 2 5 5 7 9 10 ]

    return 0;
}
```

---

## 13. Quick Recap Table

| Concept | Meaning | Key point |
| --- | --- | --- |
| Linear array | same-type elements in **consecutive** memory, accessed by index | contiguity is the root of every strength and weakness |
| Length | `UB − LB + 1` | `n` in C++ (0-based) |
| Size vs capacity | elements in use vs slots reserved | insertion/deletion change `n`, never capacity |
| Address formula | `LOC(A[k]) = Base + w × (k − LB)` | one multiply + one add, so O(1) access |
| `sizeof` trick | `sizeof(arr)/sizeof(arr[0])` | works only where declared, not inside a function |
| No bounds checking | C++ won't stop `arr[n]` | valid indices are `0 … n−1` |
| Traversal | visit each element once | O(n) time, O(1) space |
| Insertion | shift right, **last element first**, then place | O(1) best (end), O(n) worst (front) |
| Deletion | shift left, **nearest-the-gap first**, decrease count | O(1) best (end), O(n) worst (front) |
| Opposite loop directions | insertion backward, deletion forward | wrong direction overwrites unmoved values |
| Linear search | check one by one | O(n) |
| Binary search | halve the range, **sorted data only** | O(log n), enabled by O(1) random access |
| Sorting (preview) | bubble / selection / insertion | O(n²), details in Lecture 3 |
| Merge (sorted) | two pointers, take the smaller, then flush leftovers | O(n + m) time and space |
| Array's weakness | fixed size, contiguous block, O(n) mid-insert/delete | motivates linked lists in Unit II |

**The single biggest idea to carry out of this lecture:** an array's defining feature, **elements sitting side by side in memory**, makes reading by index O(1) and makes every structural change (insert, delete) O(n), because staying contiguous forces elements to shift. Every complexity in the table above follows from that one fact.

---

## What's Next

**Lecture 3 – Sorting:** Bubble sort, Selection sort, and Insertion sort, each with full step-by-step diagrams, the loop-direction and boundary traps, and a best/average/worst complexity analysis.