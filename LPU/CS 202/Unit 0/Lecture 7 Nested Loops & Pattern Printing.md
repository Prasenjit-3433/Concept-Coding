# Lecture 7: Nested Loops & Pattern Printing

Status: Done

# 🎯Nested Loops in C++

---

## 1. What Is a Nested Loop?

We've already seen **nested if** — an `if` written inside another `if`. The same idea applies to loops:

> When one loop is written inside another loop, it is called a nested loop.
> 

```cpp
for (initialization; condition; update) {
    for (initialization; condition; update) {
        // inner loop body
    }
}
```

- The loop on the **outside** is called the **Outer Loop**.
- The loop written **inside** it is called the **Inner Loop**.

> Nesting isn't limited to `for` inside `for`. You can put a `while` loop inside a `for` loop, a `while` inside a `while`, or even nest at multiple levels — like two loops inside one outer loop, or a loop inside a loop inside another loop. As long as one loop sits inside another, it's a nested loop.
> 

---

## 2. A Real-Life Example: How a Clock Works

Think about how a clock's hour hand moves. For the hour hand to go from `1:00` to `2:00`, the **minute hand** has to complete a full cycle — all 60 minutes: `1:01, 1:02, 1:03, ... 1:59`, and only then does it become `2:00`.

```
Outer Loop (Hours):    runs from 1 to 24  → 24 iterations
Inner Loop (Minutes):  runs 60 times for EACH hour → 60 iterations per outer pass
```

For **one** run of the outer loop (one hour passing), the inner loop must run **completely** (all 60 minutes). This is exactly the nested loop pattern.

**Total iterations = outer iterations × inner iterations = 24 × 60**

> Nested loops are used for performing repeated operations in a **2D manner** — this becomes especially useful later when working with matrices and 2D arrays (rows and columns).
> 

---

## 3. The Flow of a Nested Loop

```
┌─────────────────────────────────────────────┐
│  Outer loop runs FIRST                      │
│     ↓                                       │
│  For EACH single iteration of the           │
│  outer loop → the inner loop runs           │
│  COMPLETELY (start to finish)               │
│     ↓                                       │
│  Total iterations = outer × inner           │
└─────────────────────────────────────────────┘
```

### Simple Example — Print Row-Column Pairs

```cpp
for (int i = 1; i <= 3; i++) {
    for (int j = 1; j <= 4; j++) {
        cout << i << "," << j << " ";
    }
    cout << endl;
}
```

Here, the outer loop (`i`) runs 3 times, and for **each** of those times, the inner loop (`j`) runs completely — all 4 times. That gives us `3 × 4 = 12` total iterations.

---

## 4. The Golden Rule for Solving Any Pattern Problem

> No matter how complex a pattern looks, you only ever need to figure out **three things**:
> 
> 1. Where does the **outer loop** run — from where to where?
> 2. Where does the **inner loop** run — from where to where?
> 3. What exactly do we need to **print**?

The **outer loop** always controls the number of **rows** (how many lines get printed). The **inner loop** always controls the number of **columns** (how many elements appear *per line*).

```
        COLUMN 1  COLUMN 2  COLUMN 3  COLUMN 4  COLUMN 5
ROW 1      *         *         *         *         *
ROW 2      *         *         *         *         *
ROW 3      *         *         *         *         *
ROW 4      *         *         *         *         *

Outer loop → runs 4 times (rows)
Inner loop → runs 5 times (columns), for EACH row
```

---

## 5. Building Up to a Pattern — Step by Step

### Step 1: Print 5 Stars in One Line

```cpp
for (int i = 1; i <= 5; i++) {
    cout << "*";
}
```

**Output:** `*****`

### Step 2: Repeat That Same Line 4 Times (Nesting It)

The instructor's insight here is simple but powerful: if you already know how to print **one line** of 5 stars, and you want that *same line* printed 4 times, just wrap the existing loop inside another loop that runs 4 times.

```cpp
for (int i = 1; i <= 4; i++) {       // outer loop → controls rows
    for (int j = 1; j <= 5; j++) {   // inner loop → controls columns
        cout << "*";
    }
    cout << endl;   // move to the next line after each row finishes
}
```

**Output:**

```
*****
*****
*****
*****
```

> Critical detail: `cout << endl;` sits **outside** the inner loop but **inside** the outer loop. This means: after the inner loop finishes printing one complete row of stars, move to a new line — then the outer loop starts its next row.
> 

---

## 6. `Pattern 1`: Square Pattern (Stars)

```
*****
*****
*****
```

- **Outer loop** → 3 rows → runs from `1` to `3`
- **Inner loop** → 5 columns → runs from `1` to `5`
- **What to print** → simply , every time

```cpp
for (int i = 1; i <= 3; i++) {
    for (int j = 1; j <= 5; j++) {
        cout << "*";
    }
    cout << endl;
}
```

---

## 7. `Pattern 2`: A Number Grid — Same Number Repeated

```
1 1 1 1
1 1 1 1
1 1 1 1
```

Same logic as the star square — just print `1` instead of `*`.

```cpp
for (int row = 1; row <= 4; row++) {
    for (int col = 1; col <= 5; col++) {
        cout << 1 << " ";
    }
    cout << endl;
}
```

> Naming tip: instead of generic `i` and `j`, the instructor renames them `row` and `col` here — this makes the code much easier to read, since it directly tells you what each loop controls.
> 

### Making It Dynamic — Ask the User for Rows & Columns

Instead of hardcoding `4` and `5`, take them as input:

```cpp
#include <iostream>
using namespace std;

int main() {
    int n, m;
    cout << "Enter the number of rows: ";
    cin >> n;
    cout << "Enter the number of columns: ";
    cin >> m;

    for (int row = 1; row <= n; row++) {
        for (int col = 1; col <= m; col++) {
            cout << 1 << " ";
        }
        cout << endl;
    }

    return 0;
}
```

If the user enters `n = 7`, `m = 8`, this prints a `7×8` grid of `1`s.

---

## 8. `Pattern 3`: Printing the Row Number

```
1 1 1 1
2 2 2 2
3 3 3 3
4 4 4 4
```

Now the "what to print" step changes: instead of always printing `1`, we print the **current row number** — meaning row 1 prints `1`s, row 2 prints `2`s, and so on.

```cpp
for (int row = 1; row <= n; row++) {
    for (int col = 1; col <= m; col++) {
        cout << row << " ";
    }
    cout << endl;
}
```

The **outer loop** value (`row`) is what gets printed on every pass of the inner loop — that's the only change from Pattern 2.

---

## 9. `Pattern 4`: Printing the Column Number

```
1 2 3 4
1 2 3 4
1 2 3 4
1 2 3 4
```

This time, flip it — print the **column number** (`col`) instead of the row number. Every row looks identical, because the columns count `1, 2, 3, 4` regardless of which row we're on.

```cpp
for (int row = 1; row <= n; row++) {
    for (int col = 1; col <= m; col++) {
        cout << col << " ";
    }
    cout << endl;
}
```

> The takeaway from Patterns 3 and 4: the loop structure (outer/inner, rows/columns) stays exactly the same — the **only** thing that changes is *what value* you choose to `cout` inside the inner loop.
> 

---

## 10. `Pattern 5`: Character Patterns Using ASCII Values

### The Problem

We want to print letters like `A`, `B`, `C` instead of numbers. But you can't just "set" a character to increase — characters work differently. This is where **ASCII values** come back into play (from Lecture 2).

> Every character has a numeric ASCII value behind it: `'A'` is `65`, `'B'` is `66`, `'C'` is `67`, and so on. If you take the character `'A'` and increment it (`ch++`), you're really just incrementing `65` to `66` — and `66` happens to be `'B'`.
> 

```
'A' (65)  +1→  'B' (66)  +1→  'C' (67)  +1→  'D' (68)
```

### Printing Row Letters (A, B, C, D — One Per Row)

```
A
B
C
D
```

Here, we start counting from **0** instead of **1** — this becomes especially useful once you study arrays later, since array indexing also starts at 0.

- **Outer loop** → `row` goes from `0` to `n - 1` (so `row < n`, not `row <= n`)
- **What to print** → `char(ch + row)`, where `ch` is the character `'A'`

```cpp
char ch = 'A';
int n = 4;

for (int row = 0; row < n; row++) {
    cout << char(ch + row) << endl;
}
```

### Tracing Through It

```
row = 0 → ch + 0 = 'A' + 0 = 'A' (65 + 0 = 65) → print 'A'
row = 1 → ch + 1 = 'A' + 1 = 'B' (65 + 1 = 66) → print 'B'
row = 2 → ch + 2 = 'A' + 2 = 'C' (65 + 2 = 67) → print 'C'
row = 3 → ch + 3 = 'A' + 3 = 'D' (65 + 3 = 68) → print 'D'
```

> Note the explicit type cast: `char(ch + row)`. Since `ch + row` mixes a `char` with an `int`, C++'s implicit type casting (from Lecture 2) would normally treat the result as an `int` — so we cast it back to `char` explicitly to make sure it prints as a letter, not a number.
> 

### Printing Column Letters (Same Letters Repeating Per Row)

```
ABCD
ABCD
ABCD
```

Same idea, but now we add the **column** number to `ch` instead of the row:

```cpp
for (int row = 0; row < n; row++) {
    for (int col = 0; col < m; col++) {
        cout << char(ch + col);
    }
    cout << endl;
}
```

```
col = 0 → ch + 0 = 'A'
col = 1 → ch + 1 = 'B'
col = 2 → ch + 2 = 'C'
col = 3 → ch + 3 = 'D'
```

---

## 11. `Pattern 6`: A Continuously Increasing Number (Not Tied to Row or Column)

```
1
2 3
4 5 6
7 8 9 10
```

This is different from earlier patterns — the number doesn't reset each row; it just **keeps climbing** across the entire pattern.

### The Key Trick: Declare the Counter Outside the Outer Loop

```cpp
int num = 1;   // declared OUTSIDE both loops

for (int row = 1; row <= 4; row++) {
    for (int col = 1; col <= row; col++) {
        cout << num << " ";
        num++;
    }
    cout << endl;
}
```

> Why declare `num` **outside** the outer loop? Because of **scope** (Lecture 3) — if `num` were declared *inside* the inner loop, it would reset back to its starting value every single time the inner loop restarted for a new row. By placing it outside both loops, its value persists and keeps growing across the *entire* pattern, not just within one row.
> 

### Tracing Through It

```
row=1: col runs 1 time  → print num(1), num becomes 2
row=2: col runs 2 times → print num(2), num becomes 3
                           print num(3), num becomes 4
row=3: col runs 3 times → print num(4,5,6), num becomes 7
row=4: col runs 4 times → print num(7,8,9,10), num becomes 11
```

**Output:**

```
1
2 3
4 5 6
7 8 9 10
```

> Also notice: the inner loop's condition here is `col <= row` — not a fixed number. This is what makes each row have a *different* number of columns (row 1 has 1 element, row 2 has 2, row 3 has 3, row 4 has 4) — this exact structure is the foundation for triangle-shaped patterns, which we'll build on in Part 2.
> 

---

## Key Points to Remember

- A **nested loop** is a loop written inside another loop; the outer one is the **Outer Loop**, the one inside is the **Inner Loop**.
- For every **single** pass of the outer loop, the inner loop runs to **completion**.
- **Total iterations = outer iterations × inner iterations.**
- The **three questions** to ask for any pattern: (1) Where does the outer loop run? (2) Where does the inner loop run? (3) What do we print?
- The **outer loop controls rows**; the **inner loop controls columns**.
- `cout << endl;` goes **inside the outer loop, but outside the inner loop** — it moves to a new line after each complete row.
- Changing *what* you print (row number, column number, a fixed character, a running counter) — while keeping the same loop structure — is what creates entirely different-looking patterns.
- For character patterns, use **ASCII arithmetic**: `char(ch + offset)` — and remember to explicitly cast back to `char`, since adding an `int` to a `char` promotes the result to `int`.
- To make a number **keep growing across the whole pattern** (not reset every row), declare its variable **outside both loops**.
- Making the inner loop's limit depend on the outer loop's current value (like `col <= row`) is the key trick behind triangle-shaped patterns — covered further in Part 2.

---

# 🎯Part 2 — Triangle, Pyramid & Advanced Patterns

## Recap: The Three Questions

Before diving in, remember the golden rule from Part 1 — for every pattern, ask:

1. Where does the **outer loop** run (rows)?
2. Where does the **inner loop** run (columns)?
3. What exactly do we **print**?

---

## Basic Patterns

### 1. Number Triangle (1 to n in each row)

```
1
12
123
1234
```

- **Outer loop** → rows `1` to `4`
- **Inner loop** → this time the limit **depends on the current row**: it runs from `1` up to `i` (not a fixed number)
- **What to print** → the inner loop's own counter, `j`

```cpp
for (int i = 1; i <= 4; i++) {
    for (int j = 1; j <= i; j++) {
        cout << j;
    }
    cout << endl;
}
```

### Tracing Through It

```
i=1 → j runs 1 to 1 → prints: 1
i=2 → j runs 1 to 2 → prints: 1 2 (no space, so "12")
i=3 → j runs 1 to 3 → prints: 123
i=4 → j runs 1 to 4 → prints: 1234
```

> This is the key structural idea behind every triangle pattern: making the **inner loop's limit depend on the outer loop's current value** (`j <= i`) is what causes each row to grow longer than the last.
> 

---

### 2. Star Triangle

```
*
**
***
****
```

Exact same structure as the Number Triangle — just print `"*"` instead of `j`.

```cpp
for (int i = 1; i <= 4; i++) {
    for (int j = 1; j <= i; j++) {
        cout << "*";
    }
    cout << endl;
}
```

> Notice: row `i` always ends up printing exactly `i` stars — row 1 prints 1 star, row 2 prints 2, and so on — because the inner loop runs `i` times.
> 

---

## Intermediate Patterns

### 3. Inverted Triangle

```
****
***
**
*
```

This is the star triangle, flipped upside down. To do that, we simply **run the outer loop backward** — starting from the largest row count and counting down to `1`.

```cpp
for (int i = 4; i >= 1; i--) {
    for (int j = 1; j <= i; j++) {
        cout << "*";
    }
    cout << endl;
}
```

### Tracing Through It

```
i=4 → j runs 1 to 4 → prints: **** (4 stars)
i=3 → j runs 1 to 3 → prints: ***  (3 stars)
i=2 → j runs 1 to 2 → prints: **   (2 stars)
i=1 → j runs 1 to 1 → prints: *    (1 star)
```

> The **inner loop logic is identical** to the normal star triangle (`j <= i`) — the only change is the **outer loop's direction**. This is a good example of how small tweaks to just one loop can flip an entire pattern.
> 

---

### 4. Floyd's Triangle

```
1
2 3
4 5 6
7 8 9 10
```

This is exactly **Pattern 6 from Part 1** — the continuously-increasing number pattern. Let's revisit it here since it fits naturally alongside the other triangles.

- **Outer loop** → rows `1` to `4`
- **Inner loop** → `j` runs from `1` up to `i` (same triangle-growing structure as the Number Triangle)
- **What to print** → a separate counter `num`, declared **outside both loops**, that keeps climbing across the entire pattern

```cpp
int num = 1;
for (int i = 1; i <= 4; i++) {
    for (int j = 1; j <= i; j++) {
        cout << num << " ";
        num++;
    }
    cout << endl;
}
```

> Why does `num` need to live outside both loops? Because of **scope** — if it were declared inside the inner loop, it would reset to `1` at the start of every new row instead of continuing to grow.
> 

---

### 5. Alphabet Pattern

```
A
AB
ABC
```

Same triangle structure as the Number Triangle — but now looping directly over `char` values instead of `int`, using the same ASCII-arithmetic idea from Part 1.

```cpp
for (char i = 'A'; i <= 'C'; i++) {
    for (char j = 'A'; j <= i; j++) {
        cout << j;
    }
    cout << endl;
}
```

### Tracing Through It

```
i='A' → j runs 'A' to 'A' → prints: A
i='B' → j runs 'A' to 'B' → prints: AB
i='C' → j runs 'A' to 'C' → prints: ABC
```

> Since characters have ordered ASCII values behind them, you can use `char` variables directly as loop counters — `i++` on a `char` moves it to the next letter, exactly like incrementing an `int` moves to the next number.
> 

---

## Advanced / High-Level Patterns

These patterns combine **more than two loops** — typically one for leading spaces, and one or two for the actual symbols.

### 6. Pyramid Pattern

```
   *
  ***
 *****
*******
```

This needs **three** loops working together per row:

1. A loop for **leading spaces** (to center the pyramid)
2. A loop for the **stars** themselves
3. The **outer loop** controlling which row we're on

```cpp
int n = 4;
for (int i = 1; i <= n; i++) {
    for (int s = 1; s <= n - i; s++) cout << " ";       // leading spaces
    for (int j = 1; j <= (2 * i - 1); j++) cout << "*"; // stars
    cout << endl;
}
```

### Why `n - i` Spaces and `2*i - 1` Stars?

| Row (`i`) | Spaces (`n - i`) | Stars (`2i - 1`) |
| --- | --- | --- |
| 1 | 4 - 1 = 3 | 2(1) - 1 = 1 |
| 2 | 4 - 2 = 2 | 2(2) - 1 = 3 |
| 3 | 4 - 3 = 1 | 2(3) - 1 = 5 |
| 4 | 4 - 4 = 0 | 2(4) - 1 = 7 |

```
Row 1: 3 spaces + 1 star   →    *
Row 2: 2 spaces + 3 stars  →   ***
Row 3: 1 space  + 5 stars  →  *****
Row 4: 0 spaces + 7 stars  → *******
```

> The **spaces shrink** as we go down (`n - i` gets smaller), while the **stars grow** in an odd-number sequence (`1, 3, 5, 7...`, given by `2i - 1`) — together, they create the triangular pyramid shape with straight edges.
> 

---

### 7. Inverted Pyramid

```
*******
 *****
  ***
   *
```

Simply the pyramid, upside down — achieved the same way as the Inverted Triangle: **run the outer loop backward**.

```cpp
int n = 4;
for (int i = n; i >= 1; i--) {
    for (int s = 1; s <= n - i; s++) cout << " ";
    for (int j = 1; j <= (2 * i - 1); j++) cout << "*";
    cout << endl;
}
```

The space and star formulas (`n - i` and `2i - 1`) stay **exactly the same** — only the direction the outer loop counts is reversed, from `n` down to `1` instead of `1` up to `n`.

---

### 8. Hollow Rectangle

```
*****
*   *
*****
```

Here, we print a `*` **only on the border** (first row, last row, first column, or last column) — and a blank space everywhere else inside.

```cpp
int rows = 3, cols = 5;
for (int i = 1; i <= rows; i++) {
    for (int j = 1; j <= cols; j++) {
        if (i == 1 || i == rows || j == 1 || j == cols)
            cout << "*";
        else
            cout << " ";
    }
    cout << endl;
}
```

### The Logic Behind the Condition

```
if (i == 1 || i == rows || j == 1 || j == cols)
```

This is a direct application of **logical OR** (Lecture 4) — if **any one** of these four conditions is true, we're sitting on the border:

| Condition | Meaning |
| --- | --- |
| `i == 1` | We're on the **first row** (top edge) |
| `i == rows` | We're on the **last row** (bottom edge) |
| `j == 1` | We're on the **first column** (left edge) |
| `j == cols` | We're on the **last column** (right edge) |

If none of these are true, we're somewhere in the *middle* of the rectangle — so we print a blank space instead of a star, which is what creates the "hollow" look.

---

### 9. Butterfly Pattern

```
*    *
**  **
*** ***
********
*** ***
**  **
*    *
```

This is the most complex pattern here — it's essentially **two pyramids mirrored side-by-side** (upper half growing, lower half shrinking), each row built from **three inner loops**.

### Upper Half

```cpp
int n = 4;
for (int i = 1; i <= n; i++) {
    for (int j = 1; j <= i; j++) cout << "*";        // left stars
    for (int s = 1; s <= 2*(n-i); s++) cout << " ";  // middle gap
    for (int j = 1; j <= i; j++) cout << "*";        // right stars
    cout << endl;
}
```

### Lower Half (Mirror of the Upper Half)

```cpp
for (int i = n; i >= 1; i--) {
    for (int j = 1; j <= i; j++) cout << "*";
    for (int s = 1; s <= 2*(n-i); s++) cout << " ";
    for (int j = 1; j <= i; j++) cout << "*";
    cout << endl;
}
```

### Breaking Down Each Row (Upper Half, `n = 4`)

| Row (`i`) | Left stars (`i`) | Middle gap (`2(n-i)`) | Right stars (`i`) |
| --- | --- | --- | --- |
| 1 | 1 | 2(4-1) = 6 | 1 |
| 2 | 2 | 2(4-2) = 4 | 2 |
| 3 | 3 | 2(4-3) = 2 | 3 |
| 4 | 4 | 2(4-4) = 0 | 4 |

> Notice the pattern: as the row number increases, both **wings** (left and right stars) grow together, while the **gap between them shrinks** — until row 4, where the gap disappears completely and the two wings merge into one solid line (`********`). The lower half then does the exact same thing in reverse, using a backward-counting outer loop just like the Inverted Pyramid.
> 

---

## Key Points to Remember

- Making the **inner loop's limit depend on the outer loop's value** (`j <= i`) is the foundational trick behind every triangle pattern.
- To **flip a pattern vertically** (triangle → inverted triangle, pyramid → inverted pyramid), simply reverse the **outer loop's direction** — the inner loop logic stays untouched.
- Floyd's Triangle reuses the "counter declared outside both loops" trick from Part 1 to keep a number climbing continuously across an otherwise normal triangle shape.
- Character-based patterns can loop directly over `char` variables (`char i = 'A'; i <= 'C'; i++`) since characters have an inherent ASCII order.
- Complex shapes like the **Pyramid** need **multiple loops per row** — typically one for spaces, one for symbols — where the space count and symbol count follow their own separate formulas (`n - i` spaces, `2i - 1` stars).
- The **Hollow Rectangle** uses a logical OR condition (`i==1 || i==rows || j==1 || j==cols`) to detect border cells — printing a symbol only on the edges, and blank space everywhere inside.
- The **Butterfly Pattern** combines two mirrored pyramid-style halves, each built from **three inner loops per row**: left stars, a shrinking middle gap, and right stars — with the lower half simply running the outer loop backward.
- The universal strategy for *any* nested-loop pattern, however complicated it looks: identify the outer loop (rows), the inner loop(s) (columns/spaces/symbols), and exactly what to print at each position — then work out the formula connecting the row number to how much of each element that row needs.

---