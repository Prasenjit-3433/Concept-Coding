# Placement Problems

Status: Pending

## 🎯1. Swap Two Numbers Using Call by Reference (Pointers)

### The Core Swap Logic (Recap)

Before bringing pointers in, remember the classic three-step swap using a temporary variable:

```cpp
int temp = a;   // save a's original value
a = b;            // a takes b's value
b = temp;         // b takes a's original value (saved in temp)
```

If `a = 10` and `b = 20`:

```
temp = 10   (a's original value saved)
a = 20      (a takes b's value)
b = 10      (b takes temp, i.e., a's original value)
```

### Applying This With Pointers

The exact same three-step logic applies — the only difference is that instead of working with the variables directly, we work with **their addresses**, passed in as pointers.

```cpp
void swapNums(int *a, int *b) {
    int temp = *a;    // dereference to get the value at a's address
    *a = *b;            // store b's value at a's address
    *b = temp;           // store temp's value at b's address
}

int main() {
    int x = 10, y = 20;
    swapNums(&x, &y);   // pass the ADDRESSES of x and y
    cout << x << " " << y;
    return 0;
}
```

### Why This Actually Changes `x` and `y`

> When we pass `&x` and `&y`, the pointers `a` and `b` inside `swapNums` store the addresses of `x` and `y`. Every change made through `*a` and `*b` (using the dereference operator) directly modifies the memory at those addresses.
> 

This connects directly back to **call by reference** from the Pointers & References lecture: passing an address means the function isn't working on a copy — it's working on the original variable's exact memory location, just accessed through a pointer instead of a reference.

### Dry Run

| Step | Action | `x` (via `*a`) | `y` (via `*b`) |
| --- | --- | --- | --- |
| Start | — | 10 | 20 |
| `temp = *a` | `temp` saves `x`'s value | 10 | 20 |
| `*a = *b` | `x` takes `y`'s value | 20 | 20 |
| `*b = temp` | `y` takes `temp` | 20 | 10 |

**Output:** `20 10`

> Before the swap: `x = 10, y = 20`. After the swap: `x = 20, y = 10`.
> 

---

## 🎯2. Copy One Array Into Another Using Pointers

### The Setup

Suppose we have a `source` array and want to copy every element into a `destination` array of the same size.

> Recall from the Pointers lecture: an array name is really just a pointer to its first element. So `source` behaves like a pointer to `source[0]`.
> 

```cpp
int src[] = {10, 20, 30, 40};
int dest[4];

int *P1 = src;    // P1 points to source[0]
int *P2 = dest;   // P2 points to destination[0]
```

### The Copying Loop

```cpp
for (int i = 0; i < 4; i++) {
    *(P2 + i) = *(P1 + i);
}
```

> `P1 + i` moves the pointer forward by `i` positions (pointer arithmetic — moving according to the data type's size, exactly as covered in the Pointers lecture). Dereferencing it with `*` gives us the actual value sitting at that position. We take that value and store it at the corresponding position in `P2`.
> 

### Dry Run

| `i` | `*(P1 + i)` (source value) | Action | `dest[i]` becomes |
| --- | --- | --- | --- |
| 0 | 10 | copy | 10 |
| 1 | 20 | copy | 20 |
| 2 | 30 | copy | 30 |
| 3 | 40 | copy | 40 |

**Output (printing `dest`):** `10 20 30 40`

> This is the same pattern as accessing array elements through pointer arithmetic (`*(arr + i)`), just applied simultaneously to two arrays — one being read from, one being written to.
> 

---

## 🎯3. Reverse an Array In-Place Using Pointers

### The Problem

Reverse an array **without creating a second array** — the reversal has to happen inside the same array's memory.

```
Before:  10 20 30 40 50
After:   50 40 30 20 10
```

### The Two-Pointer Strategy

> Place one pointer at the **first** index (`start`) and another at the **last** index (`end`). Swap the values they point to, then move `start` forward and `end` backward. Keep repeating until they meet or cross.
> 

```cpp
void reverseArray(int arr[], int n) {
    int *start = arr;           // points to the first element
    int *end = arr + n - 1;     // points to the last element

    while (start < end) {
        int temp = *start;
        *start = *end;
        *end = temp;

        start++;   // move forward
        end--;     // move backward
    }
}
```

> `arr + n - 1` uses the exact same pointer arithmetic idea from before: since indices run from `0` to `n - 1`, adding `n - 1` to the base address lands exactly on the last element.
> 

### Dry Run — `{10, 20, 30, 40, 50}`

| Pass | `start` points to | `end` points to | Swap? | Array after swap |
| --- | --- | --- | --- | --- |
| 1 | index 0 (10) | index 4 (50) | Yes | `50 20 30 40 10` |
| 2 | index 1 (20) | index 3 (40) | Yes | `50 40 30 20 10` |
| 3 | index 2 (30) | index 2 (30) | `start < end`? No — loop stops | — |

> Notice the middle element (`30`, at index 2) never needs swapping — when `start` and `end` meet at the same index, the condition `start < end` becomes false and the loop stops. This is exactly why the loop condition is `<` rather than `<=`.
> 

**Final Output:** `50 40 30 20 10`

### Full Program

```cpp
#include <iostream>
using namespace std;

void reverseArray(int arr[], int n) {
    int *start = arr;
    int *end = arr + n - 1;

    while (start < end) {
        int temp = *start;
        *start = *end;
        *end = temp;
        start++;
        end--;
    }
}

int main() {
    int arr[] = {10, 20, 30, 40, 50};
    reverseArray(arr, 5);

    for (int i = 0; i < 5; i++) {
        cout << arr[i] << " ";
    }
    return 0;
}
```

---

## 🎯4. Finding the Length of a C-Style String Using Pointers

### The Setup

Recall from Lecture 10: a C-style string is a character array terminated by the **null character** (`'\0'`).

```
L E A R N Y A R D \0
```

### The Logic

> Instead of using `strlen()`, we can manually walk a pointer through the string, character by character, incrementing a counter each time — and stop the moment we hit `'\0'`.
> 

```cpp
int stringLength(char* str) {
    int length = 0;

    while (*str != '\0') {
        length++;
        str++;
    }

    return length;
}
```

> `*str` dereferences the pointer to check the **current character**. As long as it isn't the null character, we increment `length` and move the pointer one step forward (`str++`) to check the next character.
> 

### Dry Run — `"LearnYard"`

| `str` points to | `*str` | Is it `'\0'`? | Action |
| --- | --- | --- | --- |
| `L` | `'L'` | No | `length = 1`, move forward |
| `e` | `'e'` | No | `length = 2`, move forward |
| ... | ... | ... | ... continues through all 9 letters |
| `\0` | `'\0'` | **Yes** | loop stops |

**Output:** `9`

> Note the null character is written in **single quotes** (`'\0'`), since it's a single character — same rule as any other `char` literal from Lecture 10.
> 

### Full Program

```cpp
#include <iostream>
using namespace std;

int stringLength(char* str) {
    int length = 0;
    while (*str != '\0') {
        length++;
        str++;
    }
    return length;
}

int main() {
    char str[] = "LearnYard";
    cout << stringLength(str);   // 9
    return 0;
}
```

---

## 🎯5. Sorting an Array Using Pointers

### The Approach: Selection-Style Comparison Sort

> Compare each element against every element that comes after it. Whenever an earlier element is **greater** than a later one, swap them — this gradually pushes smaller values toward the front.
> 

```cpp
void sortArray(int arr[], int n) {
    for (int i = 0; i < n - 1; i++) {
        for (int j = i + 1; j < n; j++) {
            if (*(arr + i) > *(arr + j)) {
                int temp = *(arr + i);
                *(arr + i) = *(arr + j);
                *(arr + j) = temp;
            }
        }
    }
}
```

> The **outer loop** (`i`) fixes one position at a time. The **inner loop** (`j`) always starts one position ahead of `i`, scanning every remaining element for something smaller. `*(arr + i)` and `*(arr + j)` are just pointer-arithmetic ways of writing `arr[i]` and `arr[j]`.
> 

### Dry Run — `{40, 30, 10, 50, 70}`

```
i=0: compare arr[0]=40 against 30,10,50,70
     40>30 → swap → {30,40,10,50,70}
     40>10 → swap → {30,10,40,50,70}... (continues comparing against the current arr[0])
     ...after full pass, smallest value settles at index 0

i=1: repeat for index 1 onward, finding next-smallest
...and so on
```

**Final Output:** `10 30 40 50 70`

> This is the same nested-loop comparison structure used across many placement-round array problems — the pointer-arithmetic notation (`*(arr+i)`) is just an alternate way of writing standard index-based access (`arr[i]`), which is worth being comfortable reading either way.
> 

---

## 🎯6. Returning Multiple Values From a Function Using Pointers

### The Problem

A C++ function can normally `return` only **one** value directly. But sometimes you need a function to compute several results at once — say, a sum, a difference, and a product — and hand back all three.

### The Solution: Pass Output Variables by Pointer

> Give the function a `void` return type, and instead pass in **pointers** to the variables where the results should be stored. The function writes directly into those addresses using the dereference operator.
> 

```cpp
void calculate(int a, int b, int* sum, int* difference, int* product) {
    *sum = a + b;
    *difference = a - b;
    *product = a * b;
}

int main() {
    int a = 5, b = 3;
    int sum, difference, product;

    calculate(a, b, &sum, &difference, &product);

    cout << "Sum = " << sum << endl;
    cout << "Difference = " << difference << endl;
    cout << "Product = " << product << endl;
    return 0;
}
```

**Output:**

```
Sum = 8
Difference = 2
Product = 15
```

> Even though the function's return type is `void`, it effectively "returns" three separate values — because it's writing them directly into the caller's own variables via their addresses, exactly the same call-by-reference idea we've used throughout this lecture, just applied to multiple outputs at once.
> 

---

## 🎯7. Placement-Style MCQs — Dangling Pointers & Pointer Arithmetic

### MCQ 1 — Accessing Memory After `delete`

```cpp
int *a = new int;
*a = 25;
delete a;
cout << *a;
```

**What happens?**

> After `delete a;`, the heap memory that `a` was pointing to has been freed — `a` no longer points to a valid allocated object. Any attempt to dereference it (`*a`) afterward results in **undefined behavior**.
> 

This is exactly the **dangling pointer** problem: the pointer still holds the old address, but the memory at that address is no longer "owned" by the program.

**Correct answer:** Undefined behavior (*not* `25`, and not a guaranteed crash either — it's genuinely unpredictable).

### MCQ 2 — Pointer Arithmetic Recap

```cpp
int arr[] = {1, 2, 3};
int *ptr = arr;
cout << *(ptr + 2);
```

> `ptr` points to the first element. `ptr + 2` moves the pointer **two positions forward** — not two raw bytes — following the same data-type-based movement rule from the Pointers lecture (an `int` typically occupies 4 bytes, so `ptr + 2` actually moves 8 bytes in memory, landing exactly on the third element).
> 

**Output:** `3`

### MCQ 3 — Accessing a Deleted Dynamic Array

```cpp
int *arr = new int[3]{10, 20, 30};
delete[] arr;
cout << arr[0];   // ❌ undefined behavior
```

> Once `delete[] arr;` runs, the entire dynamically allocated array is freed. Accessing `arr[0]`, `arr[1]`, or `arr[2]` afterward is unsafe — the memory no longer belongs to your program, exactly the same category of bug as MCQ 1.
> 

### The General Safety Rule

![image.png](Placement%20Problems/image.png)

> After deleting dynamically allocated memory (`delete` or `delete[]`), it's good practice to immediately set the pointer to `nullptr`:
> 

```cpp
delete ptr;
ptr = nullptr;
```

> This prevents accidentally dereferencing a dangling pointer later in the program — a pointer set to `nullptr` will cause an obvious, catchable crash if misused, rather than silently corrupting memory with undefined behavior.
> 

---

## Key Points to Remember

- **Swapping via pointers** uses the exact same temp-variable logic as a normal swap — the only change is that every read/write happens through the dereference operator (`a`, `b`) on addresses passed into the function.
- **Copying an array** with pointers loops through both arrays in parallel, using `(P1 + i)` to read from the source and `(P2 + i)` to write to the destination.
- **Reversing an array in-place** uses a **two-pointer technique**: `start` and `end` pointers move toward each other, swapping values at each step, until `start < end` becomes false.
- **String length via pointers** manually walks through a C-style string one character at a time, stopping at the null character (`'\0'`) — this is essentially how `strlen()` works internally.
- **Sorting via pointers** uses the same nested-loop comparison structure as index-based sorting — `(arr + i)` and `arr[i]` are interchangeable ways of accessing the same element.
- A function can **"return" multiple values** by accepting pointers to output variables and writing results directly into them via dereferencing — even while its own return type stays `void`.
- Dereferencing a pointer **after** its memory has been `delete`d is **undefined behavior** — this is the classic **dangling pointer** trap, and it's a very common MCQ pattern in placement rounds.
- Always set a pointer to `nullptr` immediately after deleting it, to avoid accidentally reusing a dangling pointer later.
- Pointer arithmetic (`ptr + n`) always moves according to the **size of the pointed-to data type**, never by raw byte count — this rule underlies almost every pointer-based array problem.