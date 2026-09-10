# Exercise 3: Placement Problems on functions

Date of Class: September 10, 2026
Status: Done

# 🎯 Problem 1: Cube of a Number Using a Function

---

## 1. The Core Logic

Before touching code, understand the plain logic: if you're given a number `n`, its cube is simply `n * n * n`. The only twist here is wrapping that logic inside a **function**, instead of computing it directly in `main()`.

## 2. Building It Step by Step

```cpp
int cube(int n) {
    return n * n * n;
}
```

- **Function name** → `cube`
- **Parameter** → `n` (the number whose cube we want)
- **Return statement** → `n * n * n`

## 3. Full Code

```cpp
#include <iostream>
using namespace std;

int cube(int n) {
    return n * n * n;
}

int main() {
    int num;
    cout << "Enter a number to find cube: ";
    cin >> num;

    cout << "Cube of " << num << " = " << cube(num);
    return 0;
}
```

### Dry Run (`num = 5`)

```
cube(5) called → n = 5
return 5 * 5 * 5 = 125
```

**Output:** `Cube of 5 = 125`

> This is a direct application of the **"Return Type, With Parameters"** category from Lecture 8 — the function takes input, processes it, and hands back a result.
> 

# 🎯 Problem 2: Swap Two Numbers (Using Call by Reference)

---

## 1. The Core Logic — The Temp Variable Trick

Suppose `A = 5` and `B = 3`. "Swapping" means: after the operation, `A` should hold `3` and `B` should hold `5`.

The classic technique uses a **temporary variable** to avoid losing a value mid-swap:

```
Step 1: temp = A       → temp holds A's original value (5)
Step 2: A = B           → A now holds B's value (3)
Step 3: B = temp        → B now holds temp's saved value (5)
```

### Tracing Through It

| Step | `A` | `B` | `temp` |
| --- | --- | --- | --- |
| Start | 5 | 3 | — |
| `temp = A` | 5 | 3 | 5 |
| `A = B` | 3 | 3 | 5 |
| `B = temp` | 3 | 5 | 5 |

**Result:** `A = 3`, `B = 5` — successfully swapped.

## 2. Why This Needs Call by Reference

If we passed `A` and `B` to a `swap` function normally (**call by value**), the function would only receive **copies** — any swapping inside the function would never affect the original `A` and `B` back in `main()`. This is exactly the pass-by-value pitfall from Lecture 8.

So instead, we pass both variables **by reference**, using `&`:

```cpp
void swapNums(int &a, int &b) {
    int temp = a;
    a = b;
    b = temp;
}
```

> Since `a` and `b` here are references (aliases) to the original `A` and `B`, any change made inside `swapNums` directly modifies the original variables in `main()` — exactly the call-by-reference mechanism from Lecture 8.
> 

Notice also that `swapNums` doesn't need a `return` statement at all — it's a `void` function that works purely through its side effects on the referenced variables.

## 3. Full Code

```cpp
#include <iostream>
using namespace std;

void swapNums(int &a, int &b) {
    int temp = a;
    a = b;
    b = temp;
}

int main() {
    int A, B;
    cout << "Enter the numbers to be swapped: ";
    cin >> A >> B;

    cout << "Before Swap: A = " << A << ", B = " << B << endl;

    swapNums(A, B);

    cout << "After Swap: A = " << A << ", B = " << B << endl;
    return 0;
}
```

### Sample Run

```
Enter the numbers to be swapped: 10 20
Before Swap: A = 10, B = 20
After Swap: A = 20, B = 10
```

# 🎯 Problem 3: Reverse a Number (Using Recursion)

---

## 1. Revisiting the Core Logic

This problem was originally solved with loops — now we solve it with **recursion** instead.

The idea: repeatedly pull off the **last digit** of the number and build up a reversed number.

```
Step 1: last digit = n % 10          (modulo extracts the last digit)
Step 2: rev = rev * 10 + last digit   (shift rev left, insert new digit)
Step 3: n = n / 10                    (chop off the last digit)
Repeat until n becomes 0
```

### Dry Run — Reversing 521

| `n` | `n % 10` | `rev = rev*10 + (n%10)` | `n / 10` |
| --- | --- | --- | --- |
| 521 | 1 | `0*10 + 1 = 1` | 52 |
| 52 | 2 | `1*10 + 2 = 12` | 5 |
| 5 | 5 | `12*10 + 5 = 125` | 0 |

Once `n` becomes `0`, we stop — **this is the base condition** for the recursion.

**Result:** `521` reversed is `125`.

## 2. Writing It Recursively

```cpp
int reverseNum(int n, int rev) {
    if (n == 0) {
        return rev;
    }
    return reverseNum(n / 10, rev * 10 + n % 10);
}
```

> Notice the recursive call: `reverseNum` calls **itself**, each time with a smaller `n` (chopped by one digit) and an updated `rev`. The moment `n` reaches `0`, the base condition fires and the accumulated `rev` is returned back up the chain.
> 

## 3. Full Code

```cpp
#include <iostream>
using namespace std;

int reverseNum(int n, int rev) {
    if (n == 0) {
        return rev;
    }
    return reverseNum(n / 10, rev * 10 + n % 10);
}

int main() {
    int num;
    cout << "Enter the number to be reversed: ";
    cin >> num;

    cout << "Reverse is " << reverseNum(num, 0);
    return 0;
}
```

**Sample run:** Input `521` → Output `Reverse is 125`

> Notice `rev` starts at `0` when the function is first called (`reverseNum(num, 0)`) — same "start from zero" idea as a running sum, from Lecture 6.
> 

# 🎯 Problem 4: Check Whether a Number Is Prime

---

## 1. What Is a Prime Number?

> A prime number is only divisible by `1` and by itself — no other number can divide it evenly.
> 

Examples: `7` is only divisible by `1` and `7`. `17` is only divisible by `1` and `17`.

## 2. The Core Logic

We loop through possible divisors and check: does **any** number (other than `1` and the number itself) divide it evenly? If yes → not prime. If no such number exists → prime.

### Why Check Only Up to `n / 2`?

> There's no need to check divisors larger than `n / 2` — no number greater than half of `n` can ever divide `n` evenly (other than `n` itself).
> 

Example: to check if `16` is prime, checking divisors from `2` up to `16 / 2 = 8` is enough — `8` itself already divides `16` evenly, so we already have the answer by then. Nothing between `9` and `15` needs to be checked.

### Special Case

> Numbers less than or equal to `1` (negative numbers, `0`, and `1` itself) are **never** prime — this is checked first, immediately.
> 

## 3. Writing the `isPrime` Function

```cpp
bool isPrime(int n) {
    if (n <= 1) {
        return false;
    }
    for (int i = 2; i <= n / 2; i++) {
        if (n % i == 0) {
            return false;
        }
    }
    return true;
}
```

> Notice the return type is `bool` — `isPrime` only ever needs to answer a yes/no question, so returning `true`/`false` directly (rather than printing something) is the clean approach, exactly like the `isPerfect` function from Lecture 8.
> 

## 4. Full Code

```cpp
#include <iostream>
using namespace std;

bool isPrime(int n) {
    if (n <= 1) {
        return false;
    }
    for (int i = 2; i <= n / 2; i++) {
        if (n % i == 0) {
            return false;
        }
    }
    return true;
}

int main() {
    int num;
    cout << "Enter the number: ";
    cin >> num;

    if (isPrime(num)) {
        cout << "Number is prime";
    } else {
        cout << "Number is not a prime number";
    }

    return 0;
}
```

### Sample Runs

```
Enter the number: 52   → Output: Number is not a prime number
Enter the number: 7    → Output: Number is prime
```

# 🎯 Problem 5: Print All Prime Numbers in a Range

---

## 1. The Core Logic — Reusing `isPrime`

This problem builds directly on Problem 4: instead of checking just **one** number, we loop through an entire **range** and check each number using the same `isPrime` function.

```cpp
void printPrimes(int start, int end) {
    cout << "Prime numbers in the range: ";
    for (int i = start; i <= end; i++) {
        if (isPrime(i)) {
            cout << i << " ";
        }
    }
}
```

> This is a great example of **function reuse** (Lecture 8's core benefit) — `printPrimes` doesn't need to know *how* primality is checked; it just trusts `isPrime` to do that job correctly, and focuses only on looping through the range.
> 

## 2. Full Code

```cpp
#include <iostream>
using namespace std;

bool isPrime(int n) {
    if (n <= 1) {
        return false;
    }
    for (int i = 2; i <= n / 2; i++) {
        if (n % i == 0) {
            return false;
        }
    }
    return true;
}

void printPrimes(int start, int end) {
    cout << "Prime numbers in the range: ";
    for (int i = start; i <= end; i++) {
        if (isPrime(i)) {
            cout << i << " ";
        }
    }
}

int main() {
    int start, end;
    cout << "Enter range: ";
    cin >> start >> end;

    printPrimes(start, end);
    return 0;
}
```

**Sample run:** Range `1` to `100` → prints every prime number in that range (`2 3 5 7 11 13 ... 97`).

# 🎯 MCQ-Style Placement Questions

---

### `Q1.` What is the correct syntax for declaring a function in C++?

**Correct pattern:** `return_type function_name(parameter_type parameter_name, ...)`

- ❌ Using a `function` keyword — C++ doesn't have one.
- ❌ Omitting data types for parameters (e.g., `int add(A, B)`) — every parameter needs an explicit type.
- ✅ `int add(int A, int B)` — data type, name, and typed parameters, exactly as covered in Lecture 8.

### `Q2.` Which function call type passes a **copy** of the argument?

- **Call by value** passes a copy — changes inside the function never touch the original.
- **Call by reference** (using `&`) passes a direct link to the original variable instead.

**Answer: Call by value.**

### `Q3.` Output-Based Question — Default Parameters

```cpp
int add(int a, int b = 5, int c = 10) {
    return a + b + c;
}

cout << add(5);
```

Only one argument (`5`) is passed, so:

- `a = 5` (from the explicit argument)
- `b = 5` (falls back to its **default parameter**, since no second argument was given)
- `c = 10` (falls back to its **default parameter**)

```
a + b + c = 5 + 5 + 10 = 20
```

**Answer: `20`**

> This directly reuses the **default parameters** concept from Lecture 8 — if the caller doesn't supply a value, the function falls back to its preset default.
> 

### `Q4.` Which statement about recursive functions is true?

- ❌ "Recursive functions always execute faster than iterative ones" — false, no such guarantee.
- ❌ "Recursive functions cannot call themselves" — false; calling itself is the entire definition of recursion.
- ❌ "Recursion does not consume stack memory" — false; every recursive call adds a new frame to the **call stack** (recall Lecture 8's call stack section).
- ✅ **"Every recursive function must have a base case"** — true; without one, the recursion never terminates.

**Answer: Every recursive function must have a base case.**

---

## Key Points to Remember

- **Cube via function**: a direct application of Lecture 8's "return type + parameters" function category — take a number in, return `n*n*n`.
- **Swap two numbers**: requires **call by reference** (`&`), since call by value would only swap local copies, leaving the original `A` and `B` in `main()` untouched. The classic three-step `temp` technique does the actual swapping.
- **Reverse a number recursively**: same digit-extraction logic as the loop-based version (`n % 10` for the last digit, `n / 10` to chop it off), but restructured so the function calls **itself** with a smaller `n` each time, stopping at the base case `n == 0`.
- **Prime checking**: loop only from `2` to `n / 2` (never higher — no divisor beyond half the number can evenly divide it), and immediately reject numbers `≤ 1`. Returns a clean `bool` rather than printing directly.
- **Printing primes in a range**: reuses the `isPrime` function inside a loop over the range — a clean demonstration of **function reuse** and modular design.
- Recurring exam/interview themes across all these problems: **call by value vs. call by reference**, **recursion needing a base case**, and **default parameters** — all direct extensions of Lecture 8's fundamentals.