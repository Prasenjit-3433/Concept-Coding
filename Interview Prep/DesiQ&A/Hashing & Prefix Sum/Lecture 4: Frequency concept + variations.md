# Frequency concept + variations

## 1. Problem 1: Max and Min Frequency

Given an array, print the **maximum frequency** and the **minimum frequency** of any element.
Example: `[3,2,3,2,4,3]` → freq(3)=3, freq(2)=2, freq(4)=1 → **max = 3, min = 1**.

### 1.1 Brute force — O(n²) time, O(1) space

For each `a[i]`, run an inner loop counting how many `a[j] == a[i]`; take min/max of those counts.

```java
int ans1 = Integer.MAX_VALUE;  // min freq: must start at +infinity so any real count replaces it
int ans2 = 0;                  // max freq: must start at 0 (or -infinity) so any real count replaces it
for (int i = 0; i < n; i++) {
    int count = 0;
    for (int j = 0; j < n; j++)
        if (a[j] == a[i]) count++;
    ans1 = Math.min(ans1, count);
    ans2 = Math.max(ans2, count);
}
```

```cpp
int ans1 = 1e9, ans2 = 0;
for (int i = 0; i < n; i++) {
    int count = 0;
    for (int j = 0; j < n; j++)
        if (a[j] == a[i]) count++;
    ans1 = min(ans1, count);
    ans2 = max(ans2, count);
}
```

- **Drawback he points out:** the same value is re-counted every time it reappears (no memory of the past). A hash map fixes this.
- His "homework": why `ans1 = INF` and `ans2 = 0` (answer above).

### 1.2 Variation: also print the element that has min/max frequency

Same loop, but update the element along with the count. Any element tied for min/max is acceptable.

```java
int minFreq = Integer.MAX_VALUE, minEle = -1, maxFreq = 0, maxEle = -1;
// ... after computing count for a[i]:
if (count < minFreq) { minFreq = count; minEle = a[i]; }
if (count > maxFreq) { maxFreq = count; maxEle = a[i]; }
```

Strict `<` / `>` keeps the first element encountered on ties.

---

## 2. Hash Map Solution — O(n) average

### 2.1 Mental model

> A hash map is "an efficient array that only stores the indices actually needed."
> 
- **Key** = distinct element of the array. **Value** = its frequency.
- `freq[x]` answers "how many times does `x` occur in the original array."
- For `[1,2,1,3,3,4]`: `{1:2, 2:1, 3:2, 4:1}`.

### 2.2 Two steps

1. **Build** the map: one pass, for each element insert (new) or update (existing).
2. **Traverse** the map: compute min/max frequency and the corresponding elements.

### 2.3 Complexity (how he explains it)

- Insert, update, lookup: **O(1) average** each → build = O(n).
- Traversal: map has at most n keys → O(n). Total ≈ O(2n) → **O(n)** (constants dropped).
- **Space: O(n)** — always quote **worst case** for space (n distinct elements). For time, the average case is what's normally quoted.
- Worst-case per operation is O(n) (all keys collide into one bucket) but he says this is practically never seen.

### 2.4 Java — `HashMap`

```java
Map<Integer, Integer> freq = new HashMap<>();
for (int i = 0; i < n; i++) {
    int y = sc.nextInt();
    freq.put(y, freq.getOrDefault(y, 0) + 1);   // absent -> treat current freq as 0
    // equivalent: freq.merge(y, 1, Integer::sum);
}

int minFreq = Integer.MAX_VALUE, minEle = -1, maxFreq = 0, maxEle = -1;
for (Map.Entry<Integer, Integer> e : freq.entrySet()) {
    int num = e.getKey(), count = e.getValue();
    if (count < minFreq) { minFreq = count; minEle = num; }
    if (count > maxFreq) { maxFreq = count; maxEle = num; }
}
```

- No need to even store the array: build the map while reading input.

### 2.5 C++ — `unordered_map`

```cpp
unordered_map<int,int> freq;
for (int i = 0; i < n; i++) {
    int y; cin >> y;
    freq[y]++;            // operator[] default-inserts 0 for a new key, then ++
}

int minFreq = 1e9, minEle = -1, maxFreq = 0, maxEle = -1;
for (auto it = freq.begin(); it != freq.end(); ++it) {
    int num = it->first, count = it->second;   // first = key, second = value
    if (count < minFreq) { minFreq = count; minEle = num; }
    if (count > maxFreq) { maxFreq = count; maxEle = num; }
}
// C++17: for (auto& [num, count] : freq) { ... }
```

### 2.6 Names per language (interview-relevant)

| Concept | C++ | Java |
| --- | --- | --- |
| Hash map | `unordered_map` | `HashMap` |
| Sorted map | `map` | `TreeMap` |

(He also notes Python `dict` has no built-in sorted equivalent — skipping per your request.)

---

## 3. Hash Map Internals & Complexity (he says this gets asked in interviews, e.g. media.net)

| Structure | Average (insert/update/lookup) | Worst case | Notes |
| --- | --- | --- | --- |
| C++ `unordered_map` | O(1) | O(n) | Separate chaining. Anti-hash tests exist (Codeforces hacks); fix with a **custom hash**. Can be slower than `map` under heavy collisions. |
| Java `HashMap` | O(1) | O(log n) from Java 8 | Array of buckets + linked list; **since Java 8, a bucket that grows past a threshold is converted to a red-black tree**. |
| C++ `map` / Java `TreeMap` | O(log n) | O(log n) | Red-black tree, always balanced. Keys kept **sorted**. |
- **Collision** = many keys land in the same bucket; with a bad hash function the bucket degenerates into a list → O(n).
- **Trade-off he highlights for `map`/`TreeMap`:** average gets worse (O(log n) vs O(1)) but the worst case is bounded at O(log n). "Balanced risk."
- Advice: re-check current-version complexities yourself; they change across language versions.

**Caveats to my notes (not from the lecture):**

- Java's treeification only helps fully if keys are `Comparable`; "can never get TLE in Java" is an overstatement.
- Insert/update on a `map`/`TreeMap` is O(log n) per op, so the whole frequency build is **O(n log n)**.

### Sorted map vs normal map

For `[1,3,2,2,4,4]`: a hash map has no guaranteed key order; a sorted map (`map`/`TreeMap`) always iterates keys ascending: `1,2,3,4`. Use it when you need ordered keys or range-style queries.

Switching from the hash version is a one-word change:

```java
Map<Integer,Integer> freq = new TreeMap<>();   // was HashMap
```

```cpp
map<int,int> freq;                              // was unordered_map
```

Everything else in the code stays the same.

---

## 4. The Key Trick: Array as a (Sorted) Hash Map

**Use when the value range is small and known** (e.g. all elements in `0..500`).

```java
int[] b = new int[501];          // all zeros initially
for (int i = 0; i < n; i++) b[a[i]]++;
```

```cpp
int b[501] = {0};                // global/static or zero-initialized
for (int i = 0; i < n; i++) b[a[i]]++;
```

- **Index = key, `b[i]` = value (frequency of `i`).**
- Example `[1,2,4,3,2]` → `b = [0,1,2,1,1,0,...]`.
- **Time:** O(1) per op in the worst case too (no hashing, no collisions) → O(n) total.
- Real constant factor is much lower than any map → preferred in **Codeforces and online assessments** where time/memory is tight.

### It is also a *sorted* map for free

Scanning `i = 0..500` visits keys in ascending order — the same ordering `map`/`TreeMap` gives, in O(1)-per-step with no tree overhead.

```java
int minFreq = Integer.MAX_VALUE, minEle = -1, maxFreq = 0, maxEle = -1;
for (int i = 0; i <= 500; i++) {
    if (b[i] == 0) continue;            // IMPORTANT: skip keys that never appeared
    if (b[i] < minFreq) { minFreq = b[i]; minEle = i; }
    if (b[i] > maxFreq) { maxFreq = b[i]; maxEle = i; }
}
```

> The `b[i] == 0` skip is not spelled out in the lecture but is required for **min** frequency — otherwise every absent value gives min = 0.
> 

### Limitations (his interview discussion points)

1. **Space:** you allocate the whole range even if only a few keys occur. Hash map for `[1,2,3,4]` stores 4 entries; the array stores 501. Hash map is more space-efficient when the data is sparse.
2. **Range too big:** values up to 10^9 or 10^10 → cannot allocate. Rule of thumb he gives: **~10^7 elements is the practical max array size**; beyond that, runtime error. (Caveat: in C++ a *local* array overflows the stack much earlier, ~10^5–10^6; declare large arrays globally or use `vector`.)
3. **Negative values:** an index cannot be negative. Options: a second array for negatives (`c[-x]`), or **shift all values by an offset** (`b[x + OFFSET]`).
4. "O(1) space" because the size is a constant is technically true, but 10^5 slots is not small in practice.

### Decision guide

| Situation | Choose |
| --- | --- |
| Small known range of non-negative ints | Array |
| Large / unknown / sparse range, no ordering needed | `HashMap` / `unordered_map` |
| Need keys sorted, or want a bounded worst case | `TreeMap` / `map` |
| C++ with adversarial tests (Codeforces) | `unordered_map` + custom hash, or `map`/array |

---

## 5. Where This Fits in Your Pattern Sheets

- Frequency map as the core idea → **Hashing Pattern 1 (Frequency Counting Fundamentals)**.
- Array-indexed-by-value counting → **Sorting Pattern 1 (Counting Sort & Rank Mapping)**; same mechanism, here used for counting rather than sorting.
- `TreeMap` / `map` as ordered key store → **BST/Ordered Set Pattern 8** and **DS Design Pattern 7**.
- Map-vs-array and bounded worst case → relevant to the Strings sheet's 26-size frequency arrays (`int[26]`).

## 6. Quick Recap

- Frequency problem = build `value → count`, then scan the counts.
- Brute force O(n²) re-counts duplicates; hash map gives O(n) average.
- `HashMap`/`unordered_map`: O(1) avg; `TreeMap`/`map`: O(log n) always, keys sorted.
- Small known non-negative range → plain array as hash map (fastest; scan = sorted order). Remember: skip zero-count slots for min.
- Space complexity: always state worst case.