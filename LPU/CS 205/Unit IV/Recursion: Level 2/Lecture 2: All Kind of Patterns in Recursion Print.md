# Lecture 2: All Kind of Patterns in Recursion | Print All | Print one | Count

Status: In progress

## Quick Recap Before We Begin

In Lecture 1 we learned to generate every subsequence using the **pick / not pick** structure: push, recurse, pop, recurse. All of that printed subsequences, one leaf at a time.

Today we keep the same skeleton and ask three different questions about it. All three use one concrete problem: **subsequences of an array whose sum equals `k`**.

| Question | What the problem wants |
| --- | --- |
| **Print all** | every subsequence whose sum is `k` |
| **Print one** | any single subsequence whose sum is `k` |
| **Count** | how many subsequences have sum `k` |

These are not three separate topics. They are three **patterns**, and each one is decided by two things: the **return type** of the function and what the **base case** does. Dynamic Programming later in the course relies on exactly these three patterns, so it is worth getting them into muscle memory now.

---

## 1. The Problem: Subsequences With Sum `k`

Take `arr = [1, 2, 1]` and `k = 2`:

```
Index:     0     1     2
        ┌─────┬─────┬─────┐
arr  =  │  1  │  2  │  1  │
        └─────┴─────┴─────┘
```

The subsequences whose elements add up to `2`:

```
[1, 1]    →  1 + 1 = 2   ✅   (picks index 0 and index 2)
[2]       →  2         = 2   ✅   (picks index 1)
```

Every other subsequence has a different sum. So the answers are **two subsequences**: `[1, 1]` and `[2]`.

---

## 2. The New Idea: Carry a Running Sum

In Lecture 1 the function carried `index` and `ds` (the subsequence built so far). To check a sum at the end, we add one more parameter:

| Parameter | Meaning |
| --- | --- |
| `index` | the position being decided |
| `ds` | the subsequence built so far |
| `s` | the **sum of the elements currently in `ds`** |

Whenever we **pick** an element, we add it to `ds` **and** to `s`. When we **undo** the pick, we remove it from both. At the base case (`index == n`), if `s == k`, then `ds` is a valid answer.

**Real-life analogy:** Shopping with a fixed budget. You walk through the shops (indices), putting items in your basket (`ds`) while a running bill (`s`) keeps the total. At the exit, you only care about the baskets whose bill is exactly your target. Putting an item back removes it from the basket **and** from the bill.

### The Recursion Tree

Each node is `f(index, ds, s)`. Left branch = pick, right branch = not pick. Leaves (`index = 3`) are marked ✅ when `s == 2`:

```
f(0, [], 0)
├── PICK 1 ──► f(1, [1], 1)
│     ├── PICK 2 ──► f(2, [1,2], 3)
│     │     ├── PICK 1 ──► f(3, [1,2,1], 4)   ❌ 4 ≠ 2
│     │     └── SKIP 1 ──► f(3, [1,2], 3)     ❌ 3 ≠ 2
│     └── SKIP 2 ──► f(2, [1], 1)
│           ├── PICK 1 ──► f(3, [1,1], 2)     ✅ 2 == 2   → answer [1, 1]
│           └── SKIP 1 ──► f(3, [1], 1)       ❌ 1 ≠ 2
│
└── SKIP 1 ──► f(1, [], 0)
      ├── PICK 2 ──► f(2, [2], 2)
      │     ├── PICK 1 ──► f(3, [2,1], 3)     ❌ 3 ≠ 2
      │     └── SKIP 1 ──► f(3, [2], 2)       ✅ 2 == 2   → answer [2]
      └── SKIP 2 ──► f(2, [], 0)
            ├── PICK 1 ──► f(3, [1], 1)       ❌ 1 ≠ 2
            └── SKIP 1 ──► f(3, [], 0)        ❌ 0 ≠ 2
```

Eight leaves (`2³`), exactly as in Lecture 1. The only new thing is the check at each leaf: two of them have `s == 2`, so there are two answers. We will reuse this same tree for all three patterns.

---

## 3. `Pattern 1`: Print All

### The Code

```cpp
void printAll(int index, vector<int> &ds, int s, int k, int arr[], int n) {
    if (index == n) {                           // base case: all indices decided
        if (s == k) {                           // sum matches the target
            for (auto it : ds) cout << it << " ";
            cout << endl;
        }
        return;
    }

    // PICK
    ds.push_back(arr[index]);
    s += arr[index];
    printAll(index + 1, ds, s, k, arr, n);

    // undo the pick: remove from BOTH ds and s
    s -= arr[index];
    ds.pop_back();

    // NOT PICK
    printAll(index + 1, ds, s, k, arr, n);
}

int main() {
    int arr[] = {1, 2, 1};
    int n = 3, k = 2;
    vector<int> ds;
    printAll(0, ds, 0, k, arr, n);
}
```

**Output:**

```
1 1
2
```

### What Changed From Lecture 1

| Lecture 1 (print every subsequence) | Today (print those with sum `k`) |
| --- | --- |
| parameters: `index`, `ds` | parameters: `index`, `ds`, **`s`**, `k` |
| base case prints every leaf | base case prints only if `s == k` |
| undo: `ds.pop_back()` | undo: `ds.pop_back()` **and** `s -= arr[index]` |

The skeleton (base case, pick, undo, not pick) is **identical**. Only the extra parameter and the check at the leaf are new.

⚠️ **Undo both `ds` and `s`.** Forgetting `s -= arr[index]` leaves the sum inflated for the not-pick branch, so correct subsequences get rejected and wrong ones can get accepted. Whatever the pick step changes, the undo step must change back.

⚠️ **Why `s` needs undoing even though it is an `int` passed by value.** `s += arr[index]` changes this call's own local copy. That local `s` is then reused for the not-pick call below it, so it must be restored before that call. The callee's copy never affects the caller, but the caller's own edits do.

⚠️ **Parameter order convention.** The first few parameters (`index`, `ds`, `s`) are the recursion's **state**, which changes every call. The remaining ones (`k`, `arr`, `n`) are fixed inputs, passed along unchanged.

**Time complexity:** `O(2ⁿ × n)` (all `2ⁿ` subsequences are explored; printing a match costs up to `n`). **Space complexity:** `O(n)` auxiliary stack space.

---

## 4. `Pattern 2`: Print One Answer

Now suppose the problem says: print **any one** subsequence with sum `k`. For `[1, 2, 1]` and `k = 2`, printing either `[1, 1]` or `[2]` is acceptable, but not both.

### Attempt 1: A Boolean Flag (works, but not preferred)

A tempting fix is a global flag `found`, set to `true` after the first print, with an extra check `if (s == k && found == false)`. This does print only one answer, but:

- it uses a **global variable**, which makes the function harder to reason about;
- worse, **the recursion keeps running** after the answer is found. It still explores every remaining branch, wasting time, even though nothing more will be printed.

### The Real Technique: Return `true` / `false`

The better idea: let the recursive call **tell its caller** whether it found an answer. If it did, the caller stops immediately and passes the good news upward.

> **The "print one" pattern**
> 
> 1. Make the function return `bool`.
> 2. **Base case:** condition satisfied → `return true`. Condition not satisfied → `return false`.
> 3. At every recursive call, wrap it in an `if`: if the call returned `true`, **immediately `return true`**.
> 4. If none of the calls returned `true`, `return false` at the end.

### The Code

```cpp
bool printOne(int index, vector<int> &ds, int s, int k, int arr[], int n) {
    if (index == n) {                           // base case
        if (s == k) {
            for (auto it : ds) cout << it << " ";
            cout << endl;
            return true;                        // condition satisfied
        }
        return false;                           // condition not satisfied
    }

    // PICK
    ds.push_back(arr[index]);
    s += arr[index];
    if (printOne(index + 1, ds, s, k, arr, n) == true) {
        return true;                            // answer found: stop right here
    }

    s -= arr[index];                            // undo the pick
    ds.pop_back();

    // NOT PICK
    if (printOne(index + 1, ds, s, k, arr, n) == true) {
        return true;                            // answer found: stop right here
    }

    return false;                               // neither branch found an answer
}
```

**Output** (for `[1, 2, 1]`, `k = 2`):

```
1 1
```

### How the `true` Travels Up

Same tree as before, now annotated with **what each call returns**. Evaluation order is top to bottom (pick before not pick):

```
f(0, [], 0)                                  → true   (its PICK branch returned true)
├── PICK 1 ──► f(1, [1], 1)                  → true
│     ├── PICK 2 ──► f(2, [1,2], 3)          → false  (both children returned false)
│     │     ├── PICK 1 ──► f(3,[1,2,1],4)    → false  (4 ≠ 2)
│     │     └── SKIP 1 ──► f(3,[1,2],3)      → false  (3 ≠ 2)
│     └── SKIP 2 ──► f(2, [1], 1)            → true
│           ├── PICK 1 ──► f(3,[1,1],2)      → true   ✅ prints "1 1"
│           └── SKIP 1                       (never executed: PICK already returned true)
│
└── SKIP 1                                   (never executed: PICK already returned true)
```

Read it bottom-up:

- `f(3, [1,1], 2)` hits the base case with `s == k`: it prints and returns `true`.
- Its parent `f(2, [1], 1)` sees `true` from the pick call, so it returns `true` at once and **skips its not-pick call**.
- `f(1, [1], 1)` sees `true` from its not-pick call and returns `true`. (Its other child had returned `false`, so it moved on to this one.)
- `f(0, [], 0)` sees `true` from its pick call and returns `true`, so the whole **"SKIP 1" half of the tree is never explored**.

This is the point of the technique: once an answer exists, the remaining recursion simply does not run.

⚠️ **The `true` must be returned at every level.** Writing just `printOne(index + 1, ...);` without checking the result, or checking it without returning, throws the answer away. Each caller has to pass `true` upward by itself, or the levels above will carry on exploring.

⚠️ **Don't forget the final `return false`.** If both branches fail, the function must report failure. Without it, control falls off the end of a `bool` function (undefined behavior), and the caller may read a garbage "true".

⚠️ **`ds` and `s` are not restored on the `true` path.** That is fine: once an answer is found we never look at `ds` or `s` again, so we return immediately without undoing anything.

**Which answer is printed:** the first one the recursion reaches, which here is `[1, 1]` because pick is explored before not pick. You cannot ask for a *specific* answer with this technique; you only avoid doing further work once *some* answer is found.

---

## 5. `Pattern 3`: Count the Subsequences

Now the problem asks only for the **number** of subsequences with sum `k`. We don't need to print anything or even remember which elements were picked.

### The Pattern

> **The "count" pattern**
> 
> 1. Make the function return `int`.
> 2. **Base case:** condition satisfied → `return 1`. Condition not satisfied → `return 0`.
> 3. Call every branch, store each result: `l = f(...)`, `r = f(...)`.
> 4. `return l + r`.

**Why it works:** every call ultimately reaches leaves. Each leaf honestly reports "I am 1 valid subsequence" or "I am 0." Every call above simply adds up what its children reported, so the root receives the total.

### The Code

Since we only count, we **no longer need `ds`** at all. We still need `s`, because the base case compares it with `k`.

```cpp
int countSubsequences(int index, int s, int k, int arr[], int n) {
    if (index == n) {                           // base case
        if (s == k) return 1;                   // one valid subsequence
        return 0;                               // not valid
    }

    // PICK: add arr[index] into the sum
    s += arr[index];
    int l = countSubsequences(index + 1, s, k, arr, n);

    // undo the pick
    s -= arr[index];

    // NOT PICK
    int r = countSubsequences(index + 1, s, k, arr, n);

    return l + r;
}

int main() {
    int arr[] = {1, 2, 1};
    int n = 3, k = 2;
    cout << countSubsequences(0, 0, k, arr, n);   // 2
}
```

### The Recursion Tree With Return Values

Every node shows `f(index, s) → value`:

```
f(0, 0) → 2                                   (1 + 1)
├── PICK 1 ──► f(1, 1) → 1                    (0 + 1)
│     ├── PICK 2 ──► f(2, 3) → 0              (0 + 0)
│     │     ├── PICK 1 ──► f(3, 4) → 0        s=4 ≠ 2
│     │     └── SKIP 1 ──► f(3, 3) → 0        s=3 ≠ 2
│     └── SKIP 2 ──► f(2, 1) → 1              (1 + 0)
│           ├── PICK 1 ──► f(3, 2) → 1        s=2 == 2 ✅
│           └── SKIP 1 ──► f(3, 1) → 0        s=1 ≠ 2
│
└── SKIP 1 ──► f(1, 0) → 1                    (1 + 0)
      ├── PICK 2 ──► f(2, 2) → 1              (0 + 1)
      │     ├── PICK 1 ──► f(3, 3) → 0        s=3 ≠ 2
      │     └── SKIP 1 ──► f(3, 2) → 1        s=2 == 2 ✅
      └── SKIP 2 ──► f(2, 0) → 0              (0 + 0)
            ├── PICK 1 ──► f(3, 1) → 0        s=1 ≠ 2
            └── SKIP 1 ──► f(3, 0) → 0        s=0 ≠ 2
```

The two ✅ leaves each contribute `1`; every internal node adds its children; the root returns **2**. Same two subsequences as before, `[1, 1]` and `[2]`, but now we only count them.

⚠️ **A function that "counts" must `return` each branch's value and add them.** A common slip is to keep a global counter, or to forget `return l + r`. The functional style (each call returns what its subtree found) is exactly what Dynamic Programming builds on.

### More Than Two Branches

When a problem has `m` recursive options instead of two (for example, in N-Queens, coming up later in this level), the same idea extends: run a loop, add up every call's result, and return the total.

```cpp
int total = 0;
for (/* each of the m options */) {
    total += f(/* ... */);
}
return total;
```

With two branches this loop is just `l + r`.

---

## 6. An Optional Optimization: Early Exit When `s > k`

If the array contains **only non-negative numbers**, the running sum can never decrease as we pick more elements. So the moment `s` exceeds `k`, no continuation of that branch can ever work, and we can stop immediately:

```cpp
if (s > k) return 0;        // only valid when the array has no negative numbers
```

This is an extra base case, placed at the top of the function. Having more than one base case is perfectly normal.

⚠️ **This is only safe for non-negative arrays.** With negative elements, `s` can exceed `k` now and drop back to `k` later, so cutting the branch would miss valid subsequences.

⚠️ **It doesn't change the worst-case complexity.** If `k` is large (say, larger than the sum of the whole array), no branch is ever cut, and we still visit all `2ⁿ` paths. It is a practical speed-up, not a better bound.

⚠️ **Zero-sum edge case.** If `k = 0`, the empty subsequence has sum `0`, so it counts as a valid answer in the Count and Print-All patterns (it reaches the base case with `s == k`). Decide whether your problem wants it counted.

---

## 7. Complexity Summary

| Pattern | Time | Space (auxiliary stack) |
| --- | --- | --- |
| Print all | `O(2ⁿ × n)` (`× n` to print each match) | `O(n)` |
| Print one | `O(2ⁿ)` worst case, often far less (stops at the first answer) | `O(n)` |
| Count | `O(2ⁿ)` (no printing) | `O(n)` |

For every pattern, the recursion depth is `n` (one level per index), so the stack never holds more than about `n` calls at once, regardless of the `2ⁿ` total calls.

---

## 8. The Three Patterns Side by Side

|  | Print all | Print one | Count |
| --- | --- | --- | --- |
| Return type | `void` | `bool` | `int` |
| Base case: satisfied | print, `return` | print, **`return true`** | **`return 1`** |
| Base case: not satisfied | `return` | **`return false`** | **`return 0`** |
| Each recursive call | just call it | `if (call == true) return true;` | `l = call`, `r = call` |
| End of function | nothing | `return false` | `return l + r` |
| Needs `ds`? | yes (to print) | yes (to print) | **no** |
| Explores every branch? | yes | no, stops at first answer | yes |

> **Mnemonic:** *Print all* just walks. *Print one* **asks "did you find it?"** and stops when told yes. *Count* **asks "how many?"** and adds the answers.
> 

The structure underneath never changes: pick, recurse, undo, not pick, recurse. Only the base case and the way we use each call's result change.

---

## 9. Common Traps Checklist

| Pitfall | What happens | Fix |
| --- | --- | --- |
| Forgetting `s -= arr[index]` | sum stays inflated in the not-pick branch | undo both `ds` and `s` |
| Calling the recursive function in "print one" without returning the `true` | recursion continues, or the answer is lost | `if (f(...) == true) return true;` at every call |
| Missing final `return false` in "print one" | undefined behavior, garbage result | always end with `return false` |
| Using a global flag for "print one" | works, but keeps exploring and adds hidden state | use the `bool` return technique |
| Forgetting to `return l + r` in count | wrong or undefined result | combine all branch results and return them |
| Keeping `ds` in the count version | wasted work and memory | drop `ds`; only `s` is needed |
| Pruning with `s > k` on arrays containing negatives | misses valid subsequences | prune only for non-negative arrays |
| Ignoring the empty subsequence when `k = 0` | count is off by one | decide explicitly whether it counts |

---

## 10. Summary:

| Concept | Meaning |
| --- | --- |
| Running sum `s` | extra parameter holding the sum of `ds`; add on pick, subtract on undo |
| Print all | `void` function; base case prints when `s == k`; explores every branch |
| Print one | `bool` function; base case returns `true`/`false`; each call is wrapped in `if (...) return true;` so the first answer stops all further recursion |
| Count | `int` function; base case returns `1`/`0`; `return l + r` over all branches; no `ds` needed |
| Multiple branches | loop over the options and add up the results |
| Early exit `s > k` | safe only for non-negative arrays; doesn't improve worst-case time |
| Time / space | `O(2ⁿ)` (`× n` when printing) / `O(n)` stack |

---