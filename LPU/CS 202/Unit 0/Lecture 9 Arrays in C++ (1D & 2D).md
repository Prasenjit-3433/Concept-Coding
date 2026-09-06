# Lecture 9: Arrays in C++ (1D & 2D)

Status: Done

# 🎯Part 1 — One-Dimensional Arrays

---

## 1. Why Do We Need Arrays?

### The Problem: Storing Many Related Values

Suppose you want to store your 12th-grade math marks. You'd simply write:

```cpp
int Sachin = 35;
```

But a class doesn't have just one student — it has many. If you had to store marks for 5 students, you might try:

```cpp
int Sachin = 35;
int Ashu = 19;
int Sahil = 20;
// ...and so on for every student
```

> Is this the right approach? No. If a class has 100 students, you'd need to create 100 separate variables — that's not good programming.
> 

This is exactly the same problem loops solved earlier: instead of writing one line 100 times, we used a loop. Here, instead of creating 100 *separate variables*, we want **one variable split into multiple divisions/blocks**.

> This new data structure — where you can store multiple values inside a single variable — is called an **array**.
> 

```
Instead of:                          Use an array:
int Sachin = 35;                     int Class12[5] = {35, 19, 20, 22, 95};
int Ashu = 19;                             ↑    ↑   ↑   ↑   ↑
int Sahil = 20;                          idx0 idx1 idx2 idx3 idx4
int Ravi = 22;
int Topper = 95;
```

### Where Arrays Fit in the Data Types Picture

Recall from Lecture 2: data types are divided into **built-in**, **derived**, and **user-defined** types.

> An array is a **derived/user-defined data type** — we take a built-in data type (like `int`), combine multiple values of it together, and create this new structure.
> 

---

## 2. What Is an Array, Technically?

> An array is a **collection of elements of the same data type**, stored in **contiguous memory locations**, and accessed using an **index** (starting from 0).
> 

### Key Properties

1. **Fixed size** — you must specify how many elements it will hold, at creation time.
2. **Same data type** — every element inside a given array must share the same data type.
3. **Index-based access** — each element has a position number (its index), starting from `0`.
4. **Contiguous memory allocation** — all elements sit in memory addresses right next to each other (covered in detail in Section 5).

---

## 3. Declaring an Array

### Syntax

```cpp
data_type array_name[size];
```

### Example

```cpp
int arr[5];   // creates an array named "arr" that can hold 5 integers
```

This creates 5 "blocks" in memory, all sharing the name `arr`, with indices `0, 1, 2, 3, 4`.

> If an array's size is stored in a variable called `size`, the **first index is always 0**, and the **last index is always `size - 1`**. Here, `size = 5`, so the last valid index is `5 - 1 = 4`.
> 

### More Examples

```cpp
float prices[10];       // 10 floats
char vowels[5];          // 5 characters
```

---

## 4. Initializing an Array — Three Ways

### a) Complete Initialization (at Declaration)

Specify every value up front, inside curly braces, separated by commas:

```cpp
int arr[5] = {10, 20, 30, 40, 50};
```

```cpp
char vowels[5] = {'A', 'E', 'I', 'O', 'U'};
```

> Note: characters must be written in **single quotes**, exactly as covered in Lecture 2.
> 

### b) Partial Initialization

If you provide fewer values than the declared size, the **remaining elements automatically become `0`**:

```cpp
int arr[5] = {10, 20};
```

**Memory content:** `10 20 0 0 0`

> This is very different from a plain uninitialized array — where, as with regular variables, leftover **garbage values** would sit in the unused slots. Partial initialization specifically guarantees the *unfilled* slots become `0`, not garbage.
> 

### c) Automatic Size Declaration

If you leave the square brackets empty, the compiler counts how many values you provided and sets the size automatically:

```cpp
int arr[] = {1, 2, 3, 4};   // compiler infers size = 4
```

### d) Initialization Using a Loop

You can also declare an array first (empty), and fill it in later via user input, using a `for` loop:

```cpp
int arr[5];
for (int i = 0; i < 5; i++) {
    cin >> arr[i];
}
```

---

## 5. Accessing and Updating Array Elements

### Accessing by Index

```cpp
int arr[3] = {5, 10, 15};
cout << arr[0];   // 5
cout << arr[2];   // 15
```

> Just like a class assigns roll numbers to students so it doesn't need to track them all individually by name, arrays use **indices** the same way — a numbering system to individually reach any stored value, all under one variable name.
> 

### Updating an Element

```cpp
arr[1] = 20;   // overwrites whatever was previously at index 1
```

### Worked Example

```cpp
int marks[4] = {35, 94, 75, 67};
cout << marks[2];   // 75

marks[2] = 95;         // update: index 2 now holds 95
```

---

## 6. Contiguous Memory Allocation — What It Actually Looks Like

This is the part most explanations skip, so let's walk through it properly (building on the memory concepts from Lecture 2).

### Setup

```cpp
int marks[4] = {10, 20, 30, 40};
```

We know from Lecture 2 that an `int` takes **4 bytes**. With 4 integers stored, the array needs:

```
4 integers × 4 bytes each = 16 bytes total
```

### Visualizing It in Memory

Imagine memory as a long strip of individually addressed bytes:

```
Address:  100  101  102  103 | 104  105  106  107 | 108 ... | 112 ...
Bytes:    [ index 0: 10    ] [ index 1: 20      ] [idx 2:30] [idx 3:40]
```

> Since one `int` needs 4 bytes, addresses `100–103` together store `marks[0]`. The **next** index doesn't jump to some random address — it starts immediately after, at `104`. Then `108`, then `112`.
> 

> **Contiguous memory allocation**: every element of an array sits at a memory address immediately following the previous one — never scattered randomly. If index 0 is at address 100, index 1 will always be at 104 (never something like 1000).
> 

---

## 7. Looping Through an Array

### Why Loop Instead of Writing `cout` Repeatedly?

If an array has 5 elements, writing `cout << arr[0]; cout << arr[1]; ...` five separate times is exactly the kind of repetition loops exist to eliminate.

### Using a Standard `for` Loop

```cpp
int marks[4] = {35, 94, 75, 67};

for (int i = 0; i < 4; i++) {
    cout << marks[i] << " ";
}
```

> Notice the condition is `i < 4`, not `i <= 4` — since valid indices only go up to `size - 1` (here, `3`), using `<=` would try to access `marks[4]`, which doesn't exist.
> 

### Using a Range-Based `for` Loop (For-Each Loop)

> Introduced in **C++11**. Used when you just need each *value* in the array, and don't actually need to know the index.
> 

```cpp
int arr[] = {1, 2, 3, 4};

for (int x : arr) {
    cout << x << " ";
}
```

**How to read this out loud:** *"for each value in `arr`, call it `x`, and run this code."*

```
Iteration 1: x = 1 → print 1
Iteration 2: x = 2 → print 2
Iteration 3: x = 3 → print 3
Iteration 4: x = 4 → print 4
```

> This is much shorter than the standard `for` loop when you don't care about the index — just the values themselves.
> 

---

## 8. Finding the Size of an Array — the `sizeof` Trick

### The Problem

If an array's size wasn't explicitly hardcoded (e.g., it was auto-detected), how do you know how many elements it has — especially since you need that number to correctly bound a `for` loop?

### The Logic

> `sizeof` is a built-in operator that tells you the **total size of the array in bytes**. Dividing that by the size of a *single* element gives you the **number of elements**.
> 

```cpp
int arr[5] = {1, 2, 3, 4, 5};

int size = sizeof(arr) / sizeof(arr[0]);
cout << "Array size = " << size;
```

### Worked Trace

```
sizeof(arr)     → 5 integers × 4 bytes each = 20 bytes (total array size)
sizeof(arr[0])  → size of ONE element = 4 bytes (since arr[0] is an int)

size = 20 / 4 = 5   ← the number of elements in the array
```

> This trick is especially useful when the array's size wasn't hardcoded by you (e.g., it came from user input, or automatic size detection) — you can still recover the element count reliably using `sizeof`.
> 

---

## Key Points to Remember

- An **array** solves the problem of needing many related variables — instead of creating dozens of individual variables, you create **one array with multiple indexed slots**.
- An array is a **collection of elements of the same data type**, stored in **contiguous memory**, and accessed via **zero-based indices**.
- Declaration syntax: `data_type array_name[size];` — and the valid indices always run from `0` to `size - 1`.
- There are **three ways to initialize** an array: complete (`{10,20,30,40,50}`), partial (remaining elements auto-fill with `0`), and automatic size detection (`arr[] = {...}`).
- Array elements are accessed and updated the same way: `array_name[index]`.
- **Contiguous memory allocation** means each element sits at consecutive memory addresses — never scattered — which is what makes index-based access so fast and predictable.
- Loop through arrays with a normal `for` loop (when you need the index) or a **range-based for loop** (when you only need the values).
- `sizeof(arr) / sizeof(arr[0])` is the standard trick for recovering an array's element count when the size wasn't hardcoded.

# 🎯Part 2 — Multidimensional / 2D Arrays

---

## 1. From 1D to 2D — Understanding Dimensions

Before diving into 2D arrays, it helps to understand what "dimension" actually means:

```
1D:  a straight line of blocks, growing in ONE direction
     [ ][ ][ ][ ][ ]

2D:  a plane with length AND breadth (rows and columns)
     [ ][ ][ ][ ]
     [ ][ ][ ][ ]
     [ ][ ][ ][ ]

3D:  a box shape — length, breadth, AND height
```

The arrays we studied in Part 1 were all **one-dimensional** — they only grew in a single direction. Today's topic, **2D arrays**, adds a second direction, letting us store data in a **rows-and-columns** (matrix) format.

---

## 2. Why Do We Need 2D Arrays?

### The Problem: Multiple Classes, Each with Multiple Students

We already know that if a single class has many students, we use a **1D array** to store all their marks in one variable:

```cpp
int Class1[5] = {31, 32, 40, 50, 90};
```

But a school doesn't have just **one** class — it has many. So naturally, we might try creating a separate array *per class*:

```cpp
int Class1[5] = {31, 32, 40, 50, 90};
int Class2[4] = {92, 91, 82, 85};
int Class3[4] = {81, 82, 83, 84};
```

> But if a school has 12 classes — or more, once you factor in sections — would you create 12 separate array variables? No, that defeats the purpose of arrays in the first place.
> 

### The Solution: Club the Arrays Together

> Just as a 1D array is a **collection of elements**, a **2D array is a collection of arrays**.
> 

```
Individual variables  →  clubbed together  →  1D array
Individual 1D arrays  →  clubbed together  →  2D array
```

There's one catch: to club several 1D arrays together into a 2D array, **every individual array must be the same size** — you can't mix a size-5 array with a size-4 array in the same 2D structure.

```
Class1: [31][32][40][50]
Class2: [92][91][82][85]
Class3: [81][82][83][84]
        ────────────────
             ↓ clubbed together ↓

         School (2D array)
   ┌────┬────┬────┬────┐
   │ 31 │ 32 │ 40 │ 50 │  ← row 0 (Class 1)
   ├────┼────┼────┼────┤
   │ 92 │ 91 │ 82 │ 85 │  ← row 1 (Class 2)
   ├────┼────┼────┼────┤
   │ 81 │ 82 │ 83 │ 84 │  ← row 2 (Class 3)
   └────┴────┴────┴────┘
```

This is a **3×4 matrix** — 3 rows, 4 columns. (Convention: **rows first, then columns**.)

> A multidimensional array is an **array of arrays**, used to store data in **row and column (matrix) form**. It's the most commonly used dimension beyond 1D, which is exactly why we focus on 2D arrays specifically.
> 

---

## 3. Row and Column Indexing

In a 1D array, there's only one index per element. In a **2D array**, every element has **two** indices — a **row index** and a **column index**:

```
          col 0  col 1  col 2  col 3
row 0    [ 31  ][  32 ][  40 ][  50 ]
row 1    [ 92  ][  91 ][  82 ][  85 ]
row 2    [ 81  ][  82 ][  83 ][  84 ]
```

> This is exactly the same row/column indexing concept from the **pattern-printing lecture** (Lecture 9) — a 2D array is essentially a matrix, the same shape we were building with nested loops earlier.
> 

---

## 4. Declaring a 2D Array

### Syntax

```cpp
data_type array_name[rows][columns];
```

### Example

```cpp
int mat[3][4];   // 3 rows, 4 columns — no data yet, just the structure
```

> Important convention to lock in: the **first** bracket is always **rows**, the **second** is always **columns**.
> 

---

## 5. Initializing a 2D Array — Two Ways

### a) Complete (Grouped) Initialization

Each row is written as its own set of curly braces, separated by commas — exactly the same idea as "clubbing" smaller arrays together into a bigger one:

```cpp
int mat[2][3] = {
    {1, 2, 3},
    {4, 5, 6}
};
```

- The first `{1, 2, 3}` becomes **row 0**
- The second `{4, 5, 6}` becomes **row 1**

### b) Row-Wise (Inline) Initialization

You can skip the inner braces entirely and just list all the values — C++ fills them in **row by row**, wrapping to the next row once the current one is full:

```cpp
int mat[2][3] = {1, 2, 3, 4, 5, 6};
```

> Both versions produce the **exact same array** in memory — `1, 2, 3` fills row 0, and since there's no more room in that row, `4, 5, 6` automatically flows into row 1.
> 

### c) Partial Initialization

Just like 1D arrays, if you don't provide enough values, the **remaining elements default to `0`**:

```cpp
int mat[3][3] = {
    {1, 2},
    {3}
};
```

This creates a 3×3 grid where the unspecified slots are filled with `0`.

### A Character 2D Array Example

```cpp
char letters[2][4] = {
    {'A', 'B', 'C', 'D'},
    {'E', 'F', 'G', 'H'}
};
```

This creates a 2-row, 4-column character matrix — row 0 holds `A B C D`, row 1 holds `E F G H`.

---

## 6. Accessing Elements of a 2D Array

To access a single element, you now provide **two indices** — row first, then column:

```cpp
cout << mat[0][1];   // element at row 0, column 1
```

### Worked Example

```cpp
int mat[2][3] = {
    {1, 2, 3},
    {4, 5, 6}
};
```

```
                col 0  col 1  col 2
row 0    [        1  ][   2 ][   3 ]
row 1    [        4  ][   5 ][   6 ]
```

- Want `6`? → row 1, column 2 → `mat[1][2]`
- Want `3`? → row 0, column 2 → `mat[0][2]`
- Want `5`? → row 1, column 1 → `mat[1][1]`

### Updating an Element

Just assign directly to the two-index position:

```cpp
mat[0][1] = 10;   // overwrites whatever value was previously at row 0, col 1
```

You can also take input directly into a specific position:

```cpp
cin >> mat[0][1];
```

---

## 7. Input and Output Using Nested Loops

Since a 2D array has two dimensions, we need **two nested loops** to fully traverse it — exactly the nested-loop pattern from Lecture 9.

### Taking Input

```cpp
int mat[2][2];
for (int i = 0; i < 2; i++) {
    for (int j = 0; j < 2; j++) {
        cin >> mat[i][j];
    }
}
```

### Printing the Full Matrix

```cpp
for (int i = 0; i < 2; i++) {
    for (int j = 0; j < 2; j++) {
        cout << mat[i][j] << " ";
    }
    cout << endl;
}
```

### Tracing Through It

For `mat = {{1, 2, 3}, {4, 5, 6}}` (a 2×3 matrix):

```
Outer loop i = 0 (row 0):
    Inner loop j = 0 → print mat[0][0] → 1
    Inner loop j = 1 → print mat[0][1] → 2
    Inner loop j = 2 → print mat[0][2] → 3
    → cout << endl (move to next line)

Outer loop i = 1 (row 1):
    Inner loop j = 0 → print mat[1][0] → 4
    Inner loop j = 1 → print mat[1][1] → 5
    Inner loop j = 2 → print mat[1][2] → 6
    → cout << endl
```

**Output:**

```
1 2 3
4 5 6
```

> The key insight tying this back to Lecture 9: **the outer loop controls the row**, and **the inner loop controls the column** — for every single pass of the outer loop, the inner loop runs completely before moving to the next row. This is the exact same rows/columns nested-loop logic we used for pattern printing.
> 

---

## 8. Passing a 2D Array to a Function

You can pass an entire 2D array into a function, but you must specify its **column count** explicitly in the parameter (the row count is more flexible):

```cpp
void printMatrix(int arr[2][3]) {
    for (int row = 0; row < 2; row++) {
        for (int col = 0; col < 3; col++) {
            cout << arr[row][col] << " ";
        }
        cout << endl;
    }
}

int main() {
    int matrix[2][3] = {{1, 2, 3}, {4, 5, 6}};
    printMatrix(matrix);
    return 0;
}
```

**Output:**

```
1 2 3
4 5 6
```

> This is a great example of combining two earlier concepts: **functions** (Lecture 10) and **nested loops** (Lecture 9) — the function's whole job is just to walk through the matrix using a nested loop and print each value.
> 

---

## 9. Traversing a 2D Array with a Range-Based `for` Loop

Just like we did with 1D arrays, C++ also supports range-based loops for 2D arrays — but since each "row" is itself an array, you need **two levels** of range-based loops:

```cpp
int mat[2][3] = {{1, 2, 3}, {4, 5, 6}};

for (auto &row : mat) {
    for (int element : row) {
        cout << element << " ";
    }
    cout << endl;
}
```

> Notice `auto &row : mat` — the **outer** loop iterates over each **row** (which is itself a small array), and the **inner** loop (`int element : row`) iterates over each **individual value** inside that row. The `&` (reference) is used here for efficiency, so each row isn't unnecessarily copied.
> 

---

## 10. Finding the Size of a 2D Array

The same `sizeof` trick from Part 1 extends naturally to 2D arrays — just applied at two different levels.

### Total Size (in Bytes)

```cpp
sizeof(mat)   // total bytes used by the ENTIRE 2D array
```

### Number of Rows

```cpp
int rows = sizeof(mat) / sizeof(mat[0]);
```

> **Why does this work?** `sizeof(mat)` gives the total bytes for the whole matrix. `sizeof(mat[0])` gives the bytes for just **one row**. Dividing total-size-by-one-row's-size tells you **how many rows** exist — the exact same logic as `sizeof(arr)/sizeof(arr[0])` for 1D arrays, just one level up.
> 

### Number of Columns

```cpp
int cols = sizeof(mat[0]) / sizeof(mat[0][0]);
```

> Here, `sizeof(mat[0])` is the size of one **entire row** (in bytes), and `sizeof(mat[0][0])` is the size of a **single element**. Dividing gives you how many elements fit in one row — i.e., the column count.
> 

### Practical Use — Summing All Elements

```cpp
int sum = 0;
for (int i = 0; i < rows; i++) {
    for (int j = 0; j < cols; j++) {
        sum += mat[i][j];
    }
}
```

This walks through every cell of the matrix using the row/column counts we just calculated, adding each value into a running total — the exact same "running total" pattern from the Lecture 6 sum-of-numbers problem, just extended to two dimensions.

---

## Key Points to Remember

- A **2D array** is an **array of arrays** — used to represent data in **row-and-column (matrix)** form.
- To combine multiple 1D arrays into a 2D array, **every individual array must be the same size**.
- Declaration syntax: `data_type array_name[rows][columns];` — **rows always come first**, columns second.
- 2D arrays can be initialized as **grouped** (`{{1,2,3},{4,5,6}}`), **inline/row-wise** (`{1,2,3,4,5,6}`), or **partially** (missing values default to `0`) — mirroring the three initialization styles from 1D arrays.
- Every element needs **two indices** to access: `array_name[row][column]`.
- Traversing a 2D array always needs **nested loops** — the outer loop walks the rows, the inner loop walks the columns — directly reusing the nested-loop pattern from Lecture 9.
- When passing a 2D array to a function, you must specify the **column count** in the parameter.
- A **range-based for loop** on a 2D array needs two levels: `for (auto &row : mat)` for each row, then `for (int element : row)` for each value inside that row.
- `sizeof` works at **two levels** for 2D arrays: `sizeof(mat)/sizeof(mat[0])` gives the row count, and `sizeof(mat[0])/sizeof(mat[0][0])` gives the column count.

---