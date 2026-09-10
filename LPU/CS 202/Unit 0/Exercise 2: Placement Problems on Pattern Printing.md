# Exercise 2: Placement Problems on Pattern Printing

Date of Class: September 10, 2026
Status: Done

> Reminder of the golden rule from Lecture 7: for *any* pattern, always ask three questions —
> 
> 1. Where does the **outer loop** run (rows)?
> 2. Where does the **inner loop(s)** run (columns)?
> 3. What exactly do we **print**?

---

# 🎯 Pattern 1: Rhombus

## 1. What Are We Printing?

```
    *****
   *****
  *****
 *****
*****
```

At a glance: some leading white spaces, followed by stars — and every row has the **same number of stars**, but a **different number of spaces**.

## 2. Step 1 — The Outer Loop (Rows)

Counting the rows: there are **5** of them. So the outer loop runs from `1` to `5` (i.e., `1` to `n`, where `n = 5`).

```cpp
for (int row = 1; row <= n; row++) {
    ...
}
```

## 3. Step 2 — Figuring Out the Two Inner Loops

Since every row needs **two different things** printed (spaces, then stars), we need **two inner loops** per row — exactly like the Pyramid pattern from Lecture 7:

- **Inner Loop 1** → prints the leading spaces
- **Inner Loop 2** → prints the stars

### The Star Count (Easy Part)

Every row prints exactly **5 stars** — no variation. So Inner Loop 2 simply runs from `1` to `n`, always.

### The Space Count (The Part That Needs a Formula)

Spaces vary row by row:

| Row | Spaces |
| --- | --- |
| 1 | 4 |
| 2 | 3 |
| 3 | 2 |
| 4 | 1 |
| 5 | 0 |

Spotting the pattern: **spaces = n − row**.

Check it:

```
Row 1 → 5 - 1 = 4 ✅
Row 2 → 5 - 2 = 3 ✅
Row 5 → 5 - 5 = 0 ✅
```

So Inner Loop 1 runs from `1` to `n - row`.

## 4. Full Code

```cpp
#include <iostream>
using namespace std;

int main() {
    int n = 5;

    for (int row = 1; row <= n; row++) {
        // Inner Loop 1: spaces
        for (int space = 1; space <= n - row; space++) {
            cout << " ";
        }
        // Inner Loop 2: stars
        for (int star = 1; star <= n; star++) {
            cout << "*";
        }
        cout << endl;
    }

    return 0;
}
```

### Dry Run

```
row=1 → spaces: 1 to 4 (4 spaces) → stars: 1 to 5 (5 stars) →     *****
row=2 → spaces: 1 to 3 (3 spaces) → stars: 1 to 5 (5 stars) →    *****
row=3 → spaces: 1 to 2 (2 spaces) → stars: 1 to 5 (5 stars) →   *****
row=4 → spaces: 1 to 1 (1 space)  → stars: 1 to 5 (5 stars) →  *****
row=5 → spaces: 1 to 0 (0 spaces) → stars: 1 to 5 (5 stars) → *****
```

> Notice: when `space <= n - row` evaluates to `space <= 0`, the loop simply never runs (since `space` starts at `1`, and `1 <= 0` is false) — this is exactly how "zero spaces" gets handled without any special-case code.
> 

---

# 🎯 Pattern 2: Diamond

## 1. What Are We Printing?

```
   *
  ***
 *****
*******
 *****
  ***
   *
```

## 2. The Key Insight — Break It Into Two Halves

> A diamond is nothing but a **Pyramid** (upper half) stacked directly on top of an **Inverted Pyramid** (lower half) — both patterns we already know how to build from Lecture 7.
> 

So instead of trying to find one master formula for the whole diamond, we write **two separate nested loops**: one for the upper half, one for the lower half.

```
┌─────────────┐
│  UPPER HALF │  ← a normal pyramid
├─────────────┤
│  LOWER HALF │  ← an inverted pyramid
└─────────────┘
```

---

## 3. Upper Half — It's Just a Pyramid

Here `n = 4` (i.e., the diamond has 4 rows in its upper half).

### Outer Loop

Runs from `1` to `n` (i.e., `1` to `4`).

### Inner Loop 1 — Spaces

| Row | Spaces |
| --- | --- |
| 1 | 3 |
| 2 | 2 |
| 3 | 1 |
| 4 | 0 |

Same formula as before: **spaces = n − row**.

### Inner Loop 2 — Stars

| Row | Stars |
| --- | --- |
| 1 | 1 |
| 2 | 3 |
| 3 | 5 |
| 4 | 7 |

This is the same odd-number-sequence formula from Lecture 7's Pyramid pattern: **stars = 2 × row − 1**.

### Upper-Half Code

```cpp
for (int row = 1; row <= n; row++) {
    for (int space = 1; space <= n - row; space++) {
        cout << " ";
    }
    for (int star = 1; star <= (2 * row - 1); star++) {
        cout << "*";
    }
    cout << endl;
}
```

---

## 4. Lower Half — It's an Inverted Pyramid

Now we build the mirror image below. Counting from the bottom up, treat these as row `3`, row `2`, row `1` (i.e., the outer loop runs **backward**, from `n - 1` down to `1`):

| Row | Spaces | Stars |
| --- | --- | --- |
| 3 | 1 | 5 |
| 2 | 2 | 3 |
| 1 | 3 | 1 |

Both formulas turn out to be **exactly the same** as the upper half:

- **spaces = n − row**
- **stars = 2 × row − 1**

The only thing that changes is the **direction** the outer loop counts — exactly the same trick used for the Inverted Triangle/Inverted Pyramid in Lecture 7.

### Lower-Half Code

```cpp
for (int row = n - 1; row >= 1; row--) {
    for (int space = 1; space <= n - row; space++) {
        cout << " ";
    }
    for (int star = 1; star <= (2 * row - 1); star++) {
        cout << "*";
    }
    cout << endl;
}
```

## 5. Full Diamond Code

```cpp
#include <iostream>
using namespace std;

int main() {
    int n = 4;

    // Upper half
    for (int row = 1; row <= n; row++) {
        for (int space = 1; space <= n - row; space++) cout << " ";
        for (int star = 1; star <= (2 * row - 1); star++) cout << "*";
        cout << endl;
    }

    // Lower half
    for (int row = n - 1; row >= 1; row--) {
        for (int space = 1; space <= n - row; space++) cout << " ";
        for (int star = 1; star <= (2 * row - 1); star++) cout << "*";
        cout << endl;
    }

    return 0;
}
```

### Dry Run (n = 4)

```
Upper half:
row=1 → 3 spaces, 1 star   →    *
row=2 → 2 spaces, 3 stars  →   ***
row=3 → 1 space,  5 stars  →  *****
row=4 → 0 spaces, 7 stars  → *******

Lower half:
row=3 → 1 space,  5 stars  →  *****
row=2 → 2 spaces, 3 stars  →   ***
row=1 → 3 spaces, 1 star   →    *
```

> The big takeaway: don't panic when a shape looks complex. Split it into pieces you already know how to build, and reuse the exact same formulas — only the loop's **direction** changes between the two halves.
> 

---

# 🎯 Pattern 3: Hollow Diamond

## 1. What Are We Printing?

```
   *
  * *
 *   *
*     *
 *   *
  * *
   *
```

Same diamond outline as before — but now the **inside is empty**. Only the border stars remain.

## 2. The Key Insight — Don't Touch the Space Loop

> The white-space loop (Inner Loop 1) stays **exactly the same** as the solid diamond. Nothing about spacing changes. The only thing that changes is the **star-printing loop** — instead of always printing a star, we now need to decide, star by star, whether it should actually be a `*` or a blank space.
> 

## 3. Figuring Out Which Stars Survive

Take a row that would normally print 5 stars (`*****`) in the solid diamond. In the hollow version, only the **first** and **last** star of that run should actually print — everything in between becomes a space.

```
Solid row:   * * * * *   (all 5 print)
Hollow row:  *       *   (only 1st and last print)
```

So inside the star-loop (which we know runs from `1` to `2 × row − 1`, same formula as before), we check:

- Is this the **first** star? → `star == 1`
- Is this the **last** star? → `star == (2 * row - 1)`

If **either** is true → print `*`. Otherwise → print a space.

```cpp
if (star == 1 || star == (2 * row - 1)) {
    cout << "*";
} else {
    cout << " ";
}
```

> This directly reuses **logical OR** from Lecture 4 — exactly the same technique used for the Hollow Rectangle in Lecture 7 (`i==1 || i==rows || j==1 || j==cols`), just adapted to a diagonal shape instead of a rectangular one.
> 

## 4. Full Hollow Diamond Code

```cpp
#include <iostream>
using namespace std;

int main() {
    int n = 4;

    // Upper half
    for (int row = 1; row <= n; row++) {
        for (int space = 1; space <= n - row; space++) cout << " ";
        for (int star = 1; star <= (2 * row - 1); star++) {
            if (star == 1 || star == (2 * row - 1))
                cout << "*";
            else
                cout << " ";
        }
        cout << endl;
    }

    // Lower half
    for (int row = n - 1; row >= 1; row--) {
        for (int space = 1; space <= n - row; space++) cout << " ";
        for (int star = 1; star <= (2 * row - 1); star++) {
            if (star == 1 || star == (2 * row - 1))
                cout << "*";
            else
                cout << " ";
        }
        cout << endl;
    }

    return 0;
}
```

### Dry Run — Row 3 of the Upper Half (`row = 3`, so stars run 1 to 5)

```
star=1 → star==1 → true  → print *
star=2 → neither  → false → print (space)
star=3 → neither  → false → print (space)
star=4 → neither  → false → print (space)
star=5 → star==5==(2*3-1) → true → print *
```

**Row output:** `*   *` (with the leading spaces from Inner Loop 1 in front)

> Notice: for `row = 1`, the star-loop only runs once (`star == 1` to `star == 1`), and that single star satisfies **both** conditions (`star == 1` and `star == 2*1-1 == 1`) — it still just prints one `*`, which is exactly right for the diamond's very tip.
> 

---

## Key Points to Remember

- The **golden rule from Lecture 7** still applies to every pattern here: figure out the outer loop (rows), the inner loop(s) (spaces/stars), and what exactly gets printed.
- **Rhombus**: outer loop for rows, one inner loop for spaces (`n - row`), one inner loop for a **fixed** number of stars (`n`) — the only pattern here where the star count doesn't change per row.
- **Diamond**: don't try to solve it as one shape — split it into an **upper pyramid** and a **lower inverted pyramid**, and reuse the exact same space (`n - row`) and star (`2×row - 1`) formulas from Lecture 7's Pyramid pattern for both halves; only the outer loop's direction flips.
- **Hollow Diamond**: keep the space-printing loop **completely untouched**. Only modify the star-printing loop, adding an `if (star == 1 || star == 2*row - 1)` check — print a  only at the first and last position of that row's star-run, and a blank space everywhere else in between.
- The overarching lesson across all three patterns: complex-looking shapes are almost always **combinations or small tweaks** of patterns you've already learned — spotting which known formula applies (and where to inject an `if` condition) is the real skill, not memorizing new logic from scratch.