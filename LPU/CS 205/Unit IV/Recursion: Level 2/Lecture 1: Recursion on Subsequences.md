# Recursion on Subsequences

Status: Done

## Quick Recap Before We Begin

In Level 1, Lecture 5 we saw that a function can make **more than one** recursive call, and that those calls run strictly one after another: the first call's whole branch finishes before the second call even starts. In Level 1, Lecture 2 we saw that code placed **after** a recursive call runs during the unwinding phase, which is the seed of **backtracking**.

Today those two ideas combine into the most important code structure of Level 2: the **pick / not pick** pattern. Almost every problem in this level (Combination Sum, Subset Sum, Permutations, Palindrome Partitioning) is a variation of the structure we build here, so this lecture deserves slow, careful reading.

---

## 1. What Is a Subsequence?

A **subsequence** is a sequence formed from an array by picking some (or all, or none) of its elements such that the **original order is preserved**. The picked elements may be contiguous or non-contiguous.

|  | Contiguous? | Order preserved? |
| --- | --- | --- |
| **Subarray** | must be contiguous | yes |
| **Subsequence** | may be contiguous **or** non-contiguous | yes |

So every subarray is a subsequence, but not every subsequence is a subarray.

Take the array `[3, 1, 2]` (indices `0, 1, 2`):

```
Index:     0     1     2
        ┌─────┬─────┬─────┐
arr  =  │  3  │  1  │  2  │
        └─────┴─────┴─────┘
```

| Candidate | Valid subsequence? | Why |
| --- | --- | --- |
| `[3]`, `[1]`, `[2]` | ✅ | single elements |
| `[3, 1]`, `[1, 2]` | ✅ | contiguous, order kept |
| `[3, 2]` | ✅ | **non-contiguous** (skips `1`), but `3` is still before `2` |
| `[3, 1, 2]` | ✅ | the whole array |
| `[]` | ✅ | pick nothing: the **empty subsequence** |
| `[2, 3]`, `[2, 1]`, `[1, 3]` | ❌ | order broken: in the array `3` comes before `1`, `1` before `2` |

⚠️ **The order rule is the whole definition.** `[3, 2]` is valid because `3` appears before `2` in the original array. `[2, 3]` is invalid even though it uses the same two elements, because it reverses their order.

**Real-life analogy:** Think of the array as a row of shops on a street, visited left to right. A subsequence is any shopping trip where you decide shop by shop whether to go in, and you never walk back. You may skip shops (non-contiguous), skip all of them (empty trip), or enter all of them, but you can never visit a shop that is behind you.

### How Many Subsequences Are There?

For `[3, 1, 2]` we listed 8 of them:

```
[3, 1, 2]   [3, 1]   [3, 2]   [3]   [1, 2]   [1]   [2]   []
```

Each of the `n` elements is either in or out, so the count is:

```
2 × 2 × 2 × ... (n times)  =  2ⁿ

n = 3  →  2³ = 8 subsequences
```

⚠️ The empty set is counted. Forgetting it is the usual reason a hand-count comes out as `2ⁿ - 1`.

**Goal of today:** write a recursive function that prints **all** subsequences of an array.

(This can also be done with bit manipulation, the "power set" technique. Here we learn the recursive way, since recursion is the tool this unit is about.)

---

## 2. The Key Observation: Pick or Not Pick

Look at how one particular subsequence arises. Take `[3, 2]` from `[3, 1, 2]`:

```
Index:        0      1      2
Element:      3      1      2
Decision:    PICK   SKIP   PICK      →   [3, 2]
```

Every subsequence is just a different string of decisions, one per index:

| Subsequence | Decision at index 0 (`3`) | at index 1 (`1`) | at index 2 (`2`) |
| --- | --- | --- | --- |
| `[3, 2]` | pick | not pick | pick |
| `[1, 2]` | not pick | pick | pick |
| `[3, 1]` | pick | pick | not pick |
| `[3, 1, 2]` | pick | pick | pick |
| `[]` | not pick | not pick | not pick |

> **For every index, we have exactly two options: pick it or not pick it.** Making that decision at every index, in every possible combination, generates every subsequence.
> 

This is the "pattern" to keep in your head from now on. The rest of the lecture is just turning it into code.

---

## 3. Building the Code Piece by Piece

We always carry three things:

| Variable | Role |
| --- | --- |
| `index` | the position we are currently deciding about |
| `ds` | a data structure (vector in C++, list in Java) holding the subsequence **built so far** |
| `arr`, `n` | the input array and its size |

### Piece 1: The Base Case

We must decide for indices `0, 1, ..., n - 1`. When `index` reaches `n`, every index has been decided, so `ds` now holds one complete subsequence. We **print it and return**.

```cpp
if (index >= n) {
    // print ds
    return;
}
```

### Piece 2: The "Pick" Call

Add `arr[index]` to `ds`, then move on to the next index:

```cpp
ds.push_back(arr[index]);
printSubsequences(index + 1, ds, arr, n);
```

### Piece 3: Undo the Pick (Backtracking)

When that call returns, **remove** the element we added:

```cpp
ds.pop_back();
```

### Piece 4: The "Not Pick" Call

Now move on to the next index **without** adding anything:

```cpp
printSubsequences(index + 1, ds, arr, n);
```

### The Complete Function

```cpp
void printSubsequences(int index, vector<int> &ds, int arr[], int n) {
    if (index >= n) {                          // base case: every index decided
        for (auto it : ds) {
            cout << it << " ";
        }
        if (ds.size() == 0) {
            cout << "{}";                      // empty subsequence
        }
        cout << endl;
        return;
    }

    // PICK
    ds.push_back(arr[index]);
    printSubsequences(index + 1, ds, arr, n);

    // undo the pick
    ds.pop_back();

    // NOT PICK
    printSubsequences(index + 1, ds, arr, n);
}

int main() {
    int arr[] = {3, 1, 2};
    int n = 3;
    vector<int> ds;                            // starts empty
    printSubsequences(0, ds, arr, n);
}
```

**Output:**

```
3 1 2
3 1
3 2
3
1 2
1
2
{}
```

All 8 subsequences appear, with the empty one printed last (because "not pick" is the last branch explored).

⚠️ **`ds` must be passed by reference (`vector<int> &ds`).** Every call has to see and modify the **same** vector, so that `push_back` in one call and `pop_back` after it refer to the same list. Passing by value would give each call its own copy, and the whole add-then-remove idea falls apart.

⚠️ **Why `index >= n` and not `index == n`.** Here `index` only ever reaches exactly `n`, so both work. `>=` is the safer habit: it still stops the recursion if some variation of the problem ever steps past `n`.

---

## 4. Why `pop_back()`? The Backtracking Step

This is the line students find hardest, so let's understand it properly.

**Real-life analogy: the trial room.** You are in a shopping-mall trial room with a few clothes to try. To try the next piece, you must **first take off** what you are currently wearing. If you skip that step and just put the new clothes on top, you end up wearing everything at once.

`ds` is what you are currently wearing. After the "pick" branch is fully explored, `ds` still contains the element we added. Before we explore the "not pick" branch, we must take it off, so that `ds` returns to **exactly the state it had when this call began**.

### The `ds` State Over Time

Follow `ds` along the very first path of the recursion (the leftmost, all-picks path), and then back out of it. Each line is one event:

```
event                              ds after the event

start  f(0)                        [ ]
pick 3                             [3]
  f(1): pick 1                     [3, 1]
    f(2): pick 2                   [3, 1, 2]
      f(3): base case → PRINT "3 1 2", return
    pop_back  (removes 2)          [3, 1]          ← back to the state f(2) started with
    f(2): not pick 2
      f(3): base case → PRINT "3 1", return
  pop_back  (removes 1)            [3]             ← back to the state f(1) started with
  f(1): not pick 1
    f(2): pick 2                   [3, 2]
      ...
```

Notice that after each `pop_back`, `ds` equals what the current call received when it started. That is the invariant:

> **Every call must leave `ds` exactly as it found it.** Whatever you add before the recursive call, you remove right after it returns.
> 

### What Goes Wrong Without `pop_back`

Remove the `pop_back()` line and trace the first two prints:

```
f(2) picks 2        →  ds = [3, 1, 2]     →  f(3) prints "3 1 2"
back in f(2), no pop_back, so ds is STILL [3, 1, 2]
f(2) "not pick" call →  f(3) sees ds = [3, 1, 2]  →  prints "3 1 2" AGAIN   ❌
```

The "not pick" branch is supposed to print `3 1`, but because `2` was never taken off, it prints `3 1 2` a second time. Every later subsequence is polluted the same way.

⚠️ **This connects directly to Level 1, Lecture 2.** There, work placed **after** the recursive call ran during unwinding. `pop_back()` is exactly that: undo work that runs on the way back. Pick (do) → recurse → undo is the core shape of **backtracking**.

---

## 5. The Recursion Tree

Each node is a call `f(index, ds)`. Left branch = **pick**, right branch = **not pick**. Leaves have `index = 3` (the base case) and are where printing happens.

```
f(0, [])
├── PICK 3 ──► f(1, [3])
│     ├── PICK 1 ──► f(2, [3,1])
│     │     ├── PICK 2 ──► f(3, [3,1,2])   ✅ prints  3 1 2
│     │     └── SKIP 2 ──► f(3, [3,1])     ✅ prints  3 1
│     └── SKIP 1 ──► f(2, [3])
│           ├── PICK 2 ──► f(3, [3,2])     ✅ prints  3 2
│           └── SKIP 2 ──► f(3, [3])       ✅ prints  3
│
└── SKIP 3 ──► f(1, [])
      ├── PICK 1 ──► f(2, [1])
      │     ├── PICK 2 ──► f(3, [1,2])     ✅ prints  1 2
      │     └── SKIP 2 ──► f(3, [1])       ✅ prints  1
      └── SKIP 1 ──► f(2, [])
            ├── PICK 2 ──► f(3, [2])       ✅ prints  2
            └── SKIP 2 ──► f(3, [])        ✅ prints  { }
```

Things worth noticing in this tree:

- **8 leaves = 8 subsequences = 2³.** Each leaf is one complete string of pick / not-pick decisions.
- The **top half** (everything under "PICK 3") prints the 4 subsequences that **contain** `3`. The **bottom half** prints the 4 that **do not**.
- **Execution order** follows Level 1, Lecture 5: the entire "PICK 3" subtree finishes, with all its printing, before the "SKIP 3" subtree even starts. Within each node, the pick branch is fully explored before the not-pick branch, so the leaves print top to bottom exactly as drawn.
- The tree has **depth 3** (one level per index), then the leaves.

⚠️ **Draw this tree yourself, on paper, at least once** for a fresh array such as `[1, 2, 3]`. Every recursion problem in this level is easiest to understand through its tree.

---

## 6. Changing the Order: Not Pick First

Nothing forces "pick" to come first. If we swap the two branches:

```cpp
void printSubsequences(int index, vector<int> &ds, int arr[], int n) {
    if (index >= n) {
        for (auto it : ds) cout << it << " ";
        if (ds.size() == 0) cout << "{}";
        cout << endl;
        return;
    }

    // NOT PICK first
    printSubsequences(index + 1, ds, arr, n);

    // PICK second
    ds.push_back(arr[index]);
    printSubsequences(index + 1, ds, arr, n);
    ds.pop_back();
}
```

the same 8 subsequences are printed, but in a different order, and the empty one now comes **first** (all "not pick" decisions are explored first):

```
{}
2
1
1 2
3
3 2
3 1
3 1 2
```

⚠️ **The `pop_back` still pairs with the `push_back`.** In this version, the undo moves to after the pick call, because that is now the last thing the function does. The rule is not "put `pop_back` in the middle." The rule is "undo right after the recursive call that followed the `push_back`."

Both orders are correct. Which one to use is your choice, unless a problem asks for output in a particular order.

---

## 7. Complexity Analysis

### Time Complexity

For every index there are 2 options, and we explore both:

```
2 × 2 × 2 × ... (n times)  =  2ⁿ leaf calls
```

At each leaf we print the subsequence, which costs up to `n` steps (a loop over `ds`). So:

```
Time Complexity  =  O(2ⁿ × n)
```

The `2ⁿ` comes from the number of subsequences; the `× n` comes from the cost of printing each one.

### Space Complexity

We do **not** count the output itself. Look at the **auxiliary stack space**: the recursion goes from index `0` to `n`, so at any moment at most about `n` calls are waiting in the stack.

```
Space Complexity  =  O(n)       (recursion depth)
```

(The `ds` vector also grows to at most `n` elements, which is also O(n), so the total auxiliary space stays O(n).)

---

## 8. Common Traps Checklist

| Pitfall | What happens | Fix |
| --- | --- | --- |
| Forgetting `pop_back()` | "not pick" branches see leftover elements, giving duplicates and wrong subsequences | undo right after the recursive call that followed `push_back` |
| Passing `ds` by value | each call edits its own copy, so the add-then-remove pairing breaks | pass `vector<int> &ds` |
| Putting `pop_back()` before the recursive call | removes the element before it has been used | order is push → recurse → pop |
| Forgetting the empty subsequence | prints `2ⁿ - 1` results | the all-not-pick leaf prints an empty `ds`; handle it (`{}`) when printing |
| Base case checked after reading `arr[index]` | reads past the end of the array | base case comes first |
| Mixing up `index` and `arr[index]` | pushes an index instead of a value | `ds` stores values: `ds.push_back(arr[index])` |

---

## 9. Summary: Lecture 1

| Concept | Meaning |
| --- | --- |
| Subsequence | picked elements in original order; contiguous or non-contiguous; empty set included |
| Subarray vs subsequence | every subarray is a subsequence, not the reverse |
| Count | `2ⁿ` subsequences for an `n`-element array |
| Core pattern | at every index, **pick** or **not pick** |
| Code structure | base case (`index >= n`, print and return) → push → recurse → pop → recurse |
| `pop_back` | backtracking: restore `ds` to the state the call started with |
| `ds` by reference | all calls must share one vector |
| Recursion tree | depth `n`, two branches per node, `2ⁿ` leaves, leaves print |
| Time complexity | `O(2ⁿ × n)`: `2ⁿ` subsequences, `n` to print each |
| Space complexity | `O(n)` auxiliary stack space |

---