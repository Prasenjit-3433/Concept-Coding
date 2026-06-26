# LC 673. Number of LIS

Key Concept: Count — maintain both length and count arrays
Solution: https://www.youtube.com/watch?v=cKVl1TFdNXg&ab_channel=takeUforward
Status: Done

# Part 1: Intuition & The Two Key Cases

---

## What's the Problem Asking?

In the previous problem, we found the **length** of the LIS. Here, we already know how to find the length — the new question is:

> **How many subsequences exist that match the maximum LIS length?**
> 

```
Array: [1, 3, 5, 4, 7]

LIS length = 4

Subsequences of length 4:
  → [1, 3, 5, 7]
  → [1, 3, 4, 7]

Answer = 2
```

We're not printing them — just **counting** them.

---

## The Foundation — Revisiting the LIS DP

Recall from DP-42, the classic LIS uses a `dp[]` array where:

```
dp[i] = length of the longest increasing subsequence ending at index i
```

Every element starts with `dp[i] = 1` because every element is at minimum an LIS of length 1 (just itself).

The recurrence:

```
for each j < i:
    if arr[j] < arr[i]:
        dp[i] = max(dp[i], dp[j] + 1)
```

Now we introduce one more array alongside it:

```
count[i] = number of LIS's of length dp[i] that end at index i
```

Every element also starts with `count[i] = 1` — because there is always exactly one way to form a subsequence of just itself.

---

## The Two Key Cases — Heart of This Problem

This is where the new logic lives. When we find that `arr[j] < arr[i]` (meaning j can attach to i), two things can happen:

---

### Case 1: We found a LONGER subsequence

```
condition: dp[j] + 1 > dp[i]
```

This means attaching `arr[j]` before `arr[i]` gives a strictly longer subsequence than what we had before for `i`.

**What to do:**

- Update `dp[i] = dp[j] + 1` — new best length
- **Inherit** `count[i] = count[j]` — the number of ways to reach `i` is now exactly the number of ways to reach `j`, because all of those can now extend to `i`

**Why inherit and not add?**
Because the previous `count[i]` was tracking a shorter length — that's now outdated. We completely replace it.

```
Example:
  dp[j] = 2, count[j] = 3
  dp[i] was 1, count[i] was 1

  After: dp[i] = 3, count[i] = 3  ← inherited from j
```

---

### Case 2: We found an EQUAL LENGTH subsequence

```
condition: dp[j] + 1 == dp[i]
```

This means attaching `arr[j]` before `arr[i]` gives the **same length** as what we already have for `i` — just via a different path.

**What to do:**

- `dp[i]` stays the same — length doesn't change
- **Add** `count[i] += count[j]` — we found more ways to reach the same length

**Why add?**
Because we're discovering additional subsequences of the same length ending at `i`. Each way to reach `j` gives a new valid subsequence ending at `i`.

```
Example:
  dp[j] = 2, count[j] = 2
  dp[i] is already 3, count[i] is 1

  After: dp[i] = 3, count[i] = 3  ← accumulated
```

---

### The Two Cases Side by Side

```
┌─────────────────────────────┬──────────────────────────────────────┐
│  Condition                     │  Action                                 │
├─────────────────────────────┼──────────────────────────────────────┤
│  dp[j] + 1 > dp[i]             │  dp[i]    = dp[j] + 1                   │
│  (found longer path)           │  count[i] = count[j]   ← INHERIT        │
├─────────────────────────────┼──────────────────────────────────────┤
│  dp[j] + 1 == dp[i]            │  dp[i]    unchanged                     │
│  (found equal length path)     │  count[i] += count[j]  ← ACCUMULATE     │
└─────────────────────────────┴──────────────────────────────────────┘
```

---

## The Diagram — Full Table Walkthrough

### Example 1: `[1, 3, 5, 4, 7]`

Every element starts with `dp[i] = 1`, `count[i] = 1`.

```
Array:  1    3    5    4    7
Index:  0    1    2    3    4

Initial state:
  dp:     [1,   1,   1,   1,   1]
  count:  [1,   1,   1,   1,   1]
```

---

**i = 1 (arr[i] = 3)**

```
j = 0: arr[0]=1 < arr[1]=3 ✓
  dp[0]+1 = 2 > dp[1]=1  → INHERIT
  dp[1] = 2, count[1] = count[0] = 1

dp:    [1,  2,  1,  1,  1]
count: [1,  1,  1,  1,  1]
```

---

**i = 2 (arr[i] = 5)**

```
j = 0: arr[0]=1 < arr[2]=5 ✓
  dp[0]+1 = 2 > dp[2]=1  → INHERIT
  dp[2] = 2, count[2] = 1

j = 1: arr[1]=3 < arr[2]=5 ✓
  dp[1]+1 = 3 > dp[2]=2  → INHERIT
  dp[2] = 3, count[2] = count[1] = 1

dp:    [1,  2,  3,  1,  1]
count: [1,  1,  1,  1,  1]
```

---

**i = 3 (arr[i] = 4)**

```
j = 0: arr[0]=1 < arr[3]=4 ✓
  dp[0]+1 = 2 > dp[3]=1  → INHERIT
  dp[3] = 2, count[3] = 1

j = 1: arr[1]=3 < arr[3]=4 ✓
  dp[1]+1 = 3 > dp[3]=2  → INHERIT
  dp[3] = 3, count[3] = count[1] = 1

j = 2: arr[2]=5 < arr[3]=4? NO ✗ (5 > 4, skip)

dp:    [1,  2,  3,  3,  1]
count: [1,  1,  1,  1,  1]
```

---

**i = 4 (arr[i] = 7)**

```
j = 0: arr[0]=1 < arr[4]=7 ✓
  dp[0]+1 = 2 > dp[4]=1  → INHERIT
  dp[4] = 2, count[4] = 1

j = 1: arr[1]=3 < arr[4]=7 ✓
  dp[1]+1 = 3 > dp[4]=2  → INHERIT
  dp[4] = 3, count[4] = count[1] = 1

j = 2: arr[2]=5 < arr[4]=7 ✓
  dp[2]+1 = 4 > dp[4]=3  → INHERIT
  dp[4] = 4, count[4] = count[2] = 1

j = 3: arr[3]=4 < arr[4]=7 ✓
  dp[3]+1 = 4 == dp[4]=4  → ACCUMULATE
  count[4] += count[3] → count[4] = 1 + 1 = 2

dp:    [1,  2,  3,  3,  4]
count: [1,  1,  1,  1,  2]
```

---

**Extracting the Answer**

```
Max LIS length = 4

Which indices have dp[i] = 4?  → index 4

Answer = count[4] = 2  ✓

The two subsequences:
  [1, 3, 5, 7]  → path through index 2
  [1, 3, 4, 7]  → path through index 3
```

---

# Part 2: Workflow + Code + Interview Tips

---

## The Algorithm Workflow

```
Initialize:
  dp[i]    = 1 for all i   (every element is an LIS of length 1 by itself)
  count[i] = 1 for all i   (exactly one way to form that length-1 subsequence)

For each i from 0 to n-1:
  For each j from 0 to i-1:
    If arr[j] < arr[i]:                    ← j can attach to i

      Case 1: dp[j] + 1 > dp[i]
        → Found a strictly longer path through j
        → dp[i]    = dp[j] + 1
        → count[i] = count[j]              ← inherit, discard old count

      Case 2: dp[j] + 1 == dp[i]
        → Found another path of the same length
        → dp[i] unchanged
        → count[i] += count[j]             ← accumulate

  After j loop i.e. dp[i] computation finish:
    maxLen = max value in dp[]

ans = sum of count[i] for all i where dp[i] == maxLen

Return ans
```

---

## Java Implementation

```java
public class NumberOfLIS {

    public static int findNumberOfLIS(int[] arr) {
        int n = arr.length;

        // dp[i]    → length of LIS ending at index i
        // count[i] → number of LIS of that length ending at index i
        int[] dp    = new int[n];
        int[] count = new int[n];

        // Every element is an LIS of length 1 by itself
        // and there is exactly 1 way to form it
        for (int i = 0; i < n; i++) {
            dp[i]    = 1;
            count[i] = 1;
        }

        int maxLen = 1; // track the global LIS length

        for (int i = 1; i < n; i++) {
            for (int j = 0; j < i; j++) {

                if (arr[j] < arr[i]) { // j can attach to i

                    if (dp[j] + 1 > dp[i]) {
                        // Case 1: found a strictly longer path through j
                        // inherit count from j — old count is now irrelevant
                        dp[i]    = dp[j] + 1;
                        count[i] = count[j];

                    } else if (dp[j] + 1 == dp[i]) {
                        // Case 2: found another path of the same length
                        // accumulate — more ways to reach the same length
                        count[i] += count[j];
                    }
                }
            }
            maxLen = Math.max(maxLen, dp[i]);
        }

        // Sum up counts for all indices that achieve the maximum LIS length
        int numberOfLIS = 0;
        for (int i = 0; i < n; i++) {
            if (dp[i] == maxLen) {
                numberOfLIS += count[i];
            }
        }

        return numberOfLIS;
    }

    public static void main(String[] args) {
        int[] arr1 = {1, 3, 5, 4, 7};
        System.out.println(findNumberOfLIS(arr1)); // Output: 2

        int[] arr2 = {2, 2, 2, 2};
        System.out.println(findNumberOfLIS(arr2)); // Output: 4
    }
}
```

---

## Annotated Decision Points in the Code

```
if (arr[j] < arr[i])
→ Strictly increasing — equal elements are NOT allowed in LIS.
→ This is the gate. Only if j can attach to i do we proceed.

if (dp[j] + 1 > dp[i])
→ We found a better (longer) path to reach i.
→ count[i] = count[j]  ← NOT +=, it's =
→ Why? The old count[i] was counting paths of a shorter length.
   That shorter length is now outdated. We throw it away and
   start fresh with exactly the ways we can reach j.

else if (dp[j] + 1 == dp[i])
→ We found another path of equal length to reach i.
→ count[i] += count[j]  ← accumulate
→ Why? Both the old paths AND the new paths through j are valid.
   We're not replacing — we're adding more valid subsequences.

maxLen = Math.max(maxLen, dp[i])
→ Track the global maximum LIS length as we go.
→ We need this at the end to know which count[] values to sum.

Final loop: if (dp[i] == maxLen) numberOfLIS += count[i]
→ Multiple indices can independently be the "end" of an LIS
   of maximum length.
→ We sum all their counts to get the total number of LIS.
```

---

## The Final Answer Extraction — Why We Sum

This is easy to miss. Consider:

```
Array: [1, 3, 5, 4, 7]

dp:    [1,  2,  3,  3,  4]
count: [1,  1,  1,  1,  2]

maxLen = 4

Only dp[4] == 4, so answer = count[4] = 2
```

But consider an array where two different indices both reach the max length:

```
Hypothetical:
dp:    [1,  2,  3,  3]
count: [1,  1,  2,  1]
maxLen = 3

Indices with dp[i] == 3: index 2 and index 3
Answer = count[2] + count[3] = 2 + 1 = 3
```

Both index 2 and index 3 are valid "endings" of independent LIS chains — so their counts are additive.

---

## Complexity

```
┌──────────────────┬────────────────────────────────────────────────┐
│  Time            │  O(n²)                                         │
│                  │  Two nested loops, same as classic LIS DP      │
│                  │                                                │
│  Space           │  O(n)                                          │
│                  │  Two arrays: dp[] and count[], each of size n  │
└──────────────────┴────────────────────────────────────────────────┘
```

---

## The Complete Picture — dp[] and count[] Together

```
Array:  [1,  3,  5,  4,  7]

         i=0  i=1  i=2  i=3  i=4
──────────────────────────────────────────
arr:    [ 1,   3,   5,   4,   7 ]
dp:     [ 1,   2,   3,   3,   4 ]
count:  [ 1,   1,   1,   1,   2 ]
──────────────────────────────────────────

Reading count[4] = 2:
  "There are 2 ways to form an LIS of length 4 ending at index 4 (value 7)"
  → [1, 3, 5, 7]   (came through index 2)
  → [1, 3, 4, 7]   (came through index 3)

maxLen = 4
Sum of count[i] where dp[i] == 4 → count[4] = 2

Answer = 2  ✓
```

---

## Interview Tips (From the Instructor)

**Tip 1 — This is a small but precise extension of classic LIS DP**

> The instructor builds this entirely on top of the DP-42 LIS code. In an interview, frame it as: *"I'll reuse the LIS DP foundation and add one parallel array to track counts."* This shows you understand the problem at its core rather than memorizing a new algorithm.
> 

**Tip 2 — The two cases must be crisp in your head**

> The instructor is very deliberate about this. The single most common mistake is doing `count[i] += count[j]` in both cases. Remember:
> 
> - Longer path found → **inherit** (`count[i] = count[j]`) — replace, don't add
> - Equal length path found → **accumulate** (`count[i] += count[j]`) — add, don't replace

**Tip 3 — Initialization is not optional**

> Both `dp[i] = 1` and `count[i] = 1` must be set for every element. The instructor explicitly says *"every element is itself an LIS of length 1, and there is exactly one way to form it."* If you forget `count[i] = 1`, the accumulation logic breaks silently.
> 

**Tip 4 — The final answer needs a second pass**

> Don't return `count[n-1]`. The LIS doesn't have to end at the last index. You need to find `maxLen` first, then sum all `count[i]` where `dp[i] == maxLen`. This is a detail interviewers specifically watch for.
> 

---

## One-Line Mental Model to Remember This Forever

```
"Run the classic LIS DP. Alongside dp[i], maintain count[i].
 When you find a longer path → inherit the count.
 When you find an equal path → accumulate the count.
 At the end, sum counts at all indices that hit the max LIS length."
```

---

That wraps up the complete note for this lecture. Let me know when you're ready for the next one!