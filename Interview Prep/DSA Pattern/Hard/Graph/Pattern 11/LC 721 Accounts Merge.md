# LC 721. Accounts Merge

Key Concept: DSU on string keys
Problem: https://leetcode.com/problems/accounts-merge/description/
Solution: https://www.youtube.com/watch?v=FMwpt_aQOGw&list=PLgUwDviBIf0oE3gA41TKO2H5bHpPd7fzn&index=50&ab_channel=takeUforward
Status: Done

# Stage 1: Identification

---

### **Step 1 — Which topic?**

You're given a list of accounts, each starting with a name followed by a set of emails. Some accounts secretly belong to the same person — you know this because they share at least one email. The moment you see *"merge these things together because they share a common element"*, you should think of components forming dynamically as you scan through data → **Graph**.

### **Step 2 — Which pattern?**

Look at what's actually being asked: two accounts are the "same person" if they overlap on even one email, and this overlap can chain — account A merges with B, B merges with C, so A, B, and C all merge together even if A and C share nothing directly. This is **dynamic connectivity** — exactly the trigger for:

> "merge groups", "check if same component", "dynamic connectivity" → **Disjoint Set Union (Pattern 11)**
> 

### **Step 3 — Which key concept?**

**`DSU on String Keys — Emails as the Union Target, Not Account Indices`**

This is the twist that makes this problem a distinct entry in Pattern 11, rather than a repeat of LC 323. Every DSU problem you've seen so far unions **integer node indices** directly — `union(u, v)` where `u` and `v` are already numbers. Here, the actual "nodes" you need to merge are **email strings**, not integers. DSU fundamentally only understands integers (that's what `parent[]` and `rank[]`/`size[]` arrays are indexed by). So the real work in this problem isn't the DSU mechanics themselves — those are the exact same `findUParent` + `union` you already know cold. The real work is **building a bridge**: assigning every account a numeric index, and then using that index as the thing you actually union, while a hash map tracks which email belongs to which index.

# Stage 2: Intuition Building

---

### The Core Question

Before touching any data structure, ask yourself:

> *"When are two accounts actually the same person?"*
> 

Answer: **the moment they share even a single email.** Not "if all emails match" — just one shared email is proof positive they're the same person, because emails are assumed unique to an individual.

Now the harder question:

> *"If account 0 and account 3 both own the email `J1@com`, what should I actually merge — the emails, or the accounts?"*
> 

This is where the problem's real structure reveals itself. You don't ultimately care about "email J1 is connected to email J2" as an end goal — you care about **which accounts belong to the same person**, so you can group all of that person's emails into one final answer. So the natural thing to union is the **account indices** (0, 1, 2, 3, 4, 5 — one per row in the input), and emails are just the *evidence* you use to decide which indices should merge.

---

### Walking Through Striver's Example By Hand

![image.png](LC%20721%20Accounts%20Merge/image.png)

```
Index 0: John  → J1@com, J2@com, J3@com
Index 1: John  → J4@com
Index 2: raj   → r1@com, r2@com
Index 3: John  → J1@com, J5@com
Index 4: raj   → r2@com, r3@com
Index 5: Mary  → M1@com
```

**Initial DSU state** — every account index is its own parent, exactly like every DSU problem so far:

```
parent = [0, 1, 2, 3, 4, 5]
```

### Phase 1 — Build a Map From Email → Owning Index, Union Whenever You See a Repeat

![image.png](LC%20721%20Accounts%20Merge/image%201.png)

Walk through every account, every email inside it, one at a time:

```
Index 0: J1@com → not seen before → map[J1@com] = 0
Index 0: J2@com → not seen before → map[J2@com] = 0
Index 0: J3@com → not seen before → map[J3@com] = 0

Index 1: J4@com → not seen before → map[J4@com] = 1

Index 2: r1@com → not seen before → map[r1@com] = 2
Index 2: r2@com → not seen before → map[r2@com] = 2
```

![image.png](LC%20721%20Accounts%20Merge/image%202.png)

```
Index 3: J1@com → **ALREADY SEEN**, owned by index 0!
          → this means index 3 and index 0 are the SAME PERSON
          → union(3, 0)
```

![image.png](LC%20721%20Accounts%20Merge/image%203.png)

```
Index 3: J5@com → not seen before → map[J5@com] = 3
          (doesn't matter that 3 is now merged into 0 — mapping
           J5@com to index 3 is still fine, since findUParent(3)
           will correctly resolve to 0 whenever it's needed later)
```

![image.png](LC%20721%20Accounts%20Merge/image%204.png)

```
Index 4: r2@com → **ALREADY SEEN**, owned by index 2!
          → union(4, 2)
Index 4: r3@com → not seen before → map[r3@com] = 4

Index 5: M1@com → not seen before → map[M1@com] = 5
```

**The moment you find an email that's already in the map, that's your signal to union the current account index with whatever index the map says already owns it.** You don't need to compare every account against every other account — the map gives you an O(1) lookup for "has someone already claimed this email?"

```
┌─────────────────────────────────────────────────────────────────────┐
│  THE CORE RULE                                                      │
│                                                                     │
│  For every email in every account:                                  │
│    → if this email has never been seen: record map[email] = i       │
│    → if this email WAS already seen at some index j:                │
│         union(i, j)   ← the two accounts are the same person        │
│                                                                     │
│  The map isn't the final answer — it's the EVIDENCE that            │
│  drives which indices get unioned.                                  │
└─────────────────────────────────────────────────────────────────────┘
```

After this phase, the DSU parent structure reflects:

```
Index 3's ultimate parent → 0   (John's two accounts merged)
Index 4's ultimate parent → 2   (raj's two accounts merged)
Index 0, 1, 2, 5 → still their own ultimate parent
```

---

### Phase 2 — Regroup Every Email Under Its Ultimate Parent

Now that DSU knows which account indices are truly the same person, walk the **email → index map** one more time — but this time, instead of using the raw index you stored, ask DSU for that index's **ultimate parent**, and file the email there.

![image.png](LC%20721%20Accounts%20Merge/image%205.png)

```
map[J1@com] = 0   → findUParent(0) = 0   → bucket[0] gets J1@com
map[J2@com] = 0   → findUParent(0) = 0   → bucket[0] gets J2@com
map[J3@com] = 0   → findUParent(0) = 0   → bucket[0] gets J3@com
map[J4@com] = 1   → findUParent(1) = 1   → bucket[1] gets J4@com
map[r1@com] = 2   → findUParent(2) = 2   → bucket[2] gets r1@com
map[r2@com] = 2   → findUParent(2) = 2   → bucket[2] gets r2@com
map[J5@com] = 3   → findUParent(3) = 0   → bucket[**0**] gets J5@com  ← **redirected!**
map[r3@com] = 4   → findUParent(4) = 2   → bucket[**2**] gets r3@com  ← **redirected!**
map[M1@com] = 5   → findUParent(5) = 5   → bucket[5] gets M1@com
```

This is the exact moment the "3 belongs to 0, so route J5 to bucket 0 instead" logic from the transcript happens. Notice bucket 3 and bucket 4 end up **completely empty** — every email that was ever mapped to those indices got redirected to their ultimate parent's bucket instead.

```
bucket[0] = {J1@com, J2@com, J3@com, J5@com}
bucket[1] = {J4@com}
bucket[2] = {r1@com, r2@com, r3@com}
bucket[3] = {}                          ← empty, absorbed into 0
bucket[4] = {}                          ← empty, absorbed into 2
bucket[5] = {M1@com}
```

---

### Phase 3 — Sort Each Bucket, Attach the Name, Build the Answer

Two final small steps, both mandatory per the problem statement:

1. **Sort every non-empty bucket's emails.** The problem explicitly requires emails within each merged account to be in sorted order — this is not optional formatting, it's part of the correctness contract.
2. **For every non-empty bucket, prepend the name** (`details[i][0]`, using that same index `i`) to form the final merged account.

```
bucket[0] → sorted: {J1@com, J2@com, J3@com, J5@com}
  → prepend name at index 0 → "John" → ["John", "J1@com", "J2@com", "J3@com", "J5@com"]

bucket[1] → sorted: {J4@com}
  → prepend name at index 1 → "John" → ["John", "J4@com"]

bucket[2] → sorted: {r1@com, r2@com, r3@com}
  → prepend name at index 2 → "raj" → ["raj", "r1@com", "r2@com", "r3@com"]

bucket[3] → EMPTY → skip entirely, nothing to output

bucket[4] → EMPTY → skip entirely, nothing to output

bucket[5] → sorted: {M1@com}
  → prepend name at index 5 → "Mary" → ["Mary", "M1@com"]
```

**Final answer** (order among the four groups doesn't matter — only email order within each group matters):

```
[
  ["John", "J1@com", "J2@com", "J3@com", "J5@com"],
  ["John", "J4@com"],
  ["raj", "r1@com", "r2@com", "r3@com"],
  ["Mary", "M1@com"]
]
```

Matches the expected output exactly.

---

### Why Union on the *Owning Index*, Not on the Two Emails Directly?

A subtlety worth locking in explicitly, because it's easy to get backwards: when you discover `J1@com` is already owned by index 0, you call `union(currentIndex, 0)` — **not** some union between the email strings themselves. DSU here operates purely on **account indices** (integers 0 to n-1). Emails are never nodes in the DSU structure at all — they're just the lookup key in a separate hash map that tells you which two indices need merging. This is the entire meaning of "DSU on string keys" as a key concept: the strings (emails) are the *trigger*, the integers (account indices) are what actually gets unioned.

```
┌─────────────────────────────────────────────────────────────────────┐
│  WRONG mental model: "union the emails together"                    │
│  RIGHT mental model: "emails are evidence; union the ACCOUNT        │
│                        INDICES that share that evidence"            │
│                                                                     │
│  DSU's parent[]/size[] arrays are indexed 0..n-1 by ACCOUNT         │
│  INDEX — exactly like every other DSU problem. The only new         │
│  piece is the HashMap<String, Integer> that bridges from            │
│  "email I just read" to "which index owns it."                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

### The Key Insight to Carry

```
┌───────────────────────────────────────────────────────────────────────┐
│  ACCOUNTS MERGE — the pattern                                         │
│                                                                       │
│  Nodes for DSU     : account indices (0 to n-1), one per row          │
│  Union trigger      : two accounts share ANY email                    │
│  Bridge structure   : HashMap<String email, Integer ownerIndex>       │
│                                                                       │
│  Phase 1 — scan every email in every account:                         │
│    new email  → map it to current index                               │
│    seen email → union(currentIndex, map.get(email))                   │
│                                                                       │
│  Phase 2 — re-scan the map, file every email under                    │
│            findUParent(its stored index) — NOT the raw index          │
│                                                                       │
│  Phase 3 — sort each non-empty bucket, prepend the name using         │
│            that same index into the original input, skip empty        │
│            buckets entirely                                           │   
└───────────────────────────────────────────────────────────────────────┘
```

# Stage 3: Code

---

### The Complete Mental Workflow

```
1. n = number of accounts (rows in the input)
   Initialize DisjointSet(n) — reuse the exact class from Pattern 11
   (path compression + union by size)

2. Create HashMap<String, Integer> mailToIndex — maps every email
   seen so far to the account index that first claimed it

3. PHASE 1 — build the map, union whenever an email repeats:
   for each account i from 0 to n-1:
       for each email in accounts[i] (skip index 0, that's the name):
           if mailToIndex does NOT contain this email:
               mailToIndex.put(email, i)
           else:
               union(i, mailToIndex.get(email))
               // this account and the account that first owned
               // this email are the SAME person — merge them

4. PHASE 2 — regroup every email under its ULTIMATE parent:
   Create List<TreeSet<String>> (or List<List<String>>) mergedMails,
   size n, all initially empty
   for each (email, index) entry in mailToIndex:
       ultimateParent = findUParent(index)
       mergedMails.get(ultimateParent).add(email)

5. PHASE 3 — build the final answer:
   for each index i from 0 to n-1:
       if mergedMails.get(i) is empty → skip (this index was
          absorbed into someone else's bucket, nothing to output)
       else:
           sort mergedMails.get(i)
           result = [accounts.get(i).get(0)] + sorted emails
           add result to the answer list

6. return the answer list
```

---

### Full Java Implementation

```java
import java.util.*;

class Solution {

    // ─────────────────────────────────────────
    // Reused, unmodified from Pattern 11 theory notes.
    // Path compression + Union by Size.
    // ─────────────────────────────────────────
    static class DisjointSet {
        List<Integer> size = new ArrayList<>();
        List<Integer> parent = new ArrayList<>();

        public DisjointSet(int n) {
            for (int i = 0; i < n; i++) {
                size.add(1);
                parent.add(i);
            }
        }

        public int findUParent(int node) {
            if (node == parent.get(node)) {
                return node;
            }
            int ultimateParent = findUParent(parent.get(node));
            parent.set(node, ultimateParent);
            return parent.get(node);
        }

        public void unionBySize(int u, int v) {
            int ultimateParentU = findUParent(u);
            int ultimateParentV = findUParent(v);

            if (ultimateParentU == ultimateParentV) {
                return;
            }

            if (size.get(ultimateParentU) < size.get(ultimateParentV)) {
                parent.set(ultimateParentU, ultimateParentV);
                size.set(ultimateParentV,
                         size.get(ultimateParentV) + size.get(ultimateParentU));
            } else {
                parent.set(ultimateParentV, ultimateParentU);
                size.set(ultimateParentU,
                         size.get(ultimateParentU) + size.get(ultimateParentV));
            }
        }
    }

    public List<List<String>> accountsMerge(List<List<String>> accounts) {

        int n = accounts.size();
        DisjointSet ds = new DisjointSet(n);

        // ─────────────────────────────────────────
        // STEP 1: mailToIndex — the bridge structure.
        // Maps every distinct email to the FIRST account
        // index that claimed it. This is NOT the final
        // owner — just whoever we saw first.
        // ─────────────────────────────────────────
        Map<String, Integer> mailToIndex = new HashMap<>();

        // ─────────────────────────────────────────
        // PHASE 1: scan every email, union on repeats
        // ─────────────────────────────────────────
        for (int i = 0; i < n; i++) {
            List<String> account = accounts.get(i);

            // start from index 1 — index 0 is the NAME, not an email
            for (int j = 1; j < account.size(); j++) {
                String mail = account.get(j);

                if (!mailToIndex.containsKey(mail)) {
                    // first time seeing this email — claim it
                    // for the current account index
                    mailToIndex.put(mail, i);
                } else {
                    // this email was already claimed by some
                    // earlier account index — that account and
                    // THIS account are the same person → merge
                    int existingIndex = mailToIndex.get(mail);
                    ds.unionBySize(i, existingIndex);
                    // NOTE: we do NOT overwrite mailToIndex.put(mail, i)
                    // here. The original owner stays recorded — it
                    // doesn't matter WHICH index owns the mapping,
                    // since findUParent() in Phase 2 will correctly
                    // resolve either index to the same ultimate parent.
                }
            }
        }

        // ─────────────────────────────────────────
        // PHASE 2: regroup every email under its
        // ULTIMATE parent, not its raw stored index.
        //
        // WHY **TreeSet** instead of a plain List?
        // TreeSet keeps emails in sorted order automatically
        // as they're inserted — this satisfies the problem's
        // sorted-output requirement for free, without a
        // separate sort step per bucket at the end.
        // ─────────────────────────────────────────
        List<**TreeSet**<String>> mergedMails = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            mergedMails.add(new TreeSet<>());
        }

        for (Map.Entry<String, Integer> entry : mailToIndex.entrySet()) {
            String mail = entry.getKey();
            int index = entry.getValue();

            // ALWAYS route through findUParent — this is what
            // redirects J5@com (stored at index 3) to bucket 0,
            // exactly like the hand-trace in Part 1.
            int ultimateParent = ds.findUParent(index);
            mergedMails.get(ultimateParent).add(mail);
        }

        // ─────────────────────────────────────────
        // PHASE 3: build the final answer.
        // Skip any index whose bucket ended up empty —
        // that index was fully absorbed into someone
        // else's ultimate parent bucket.
        // ─────────────────────────────────────────
        List<List<String>> result = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            if (mergedMails.get(i).isEmpty()) {
                continue;
                // this account's emails were all redirected
                // to some OTHER index's bucket — nothing to
                // output here, avoid producing an empty/duplicate row
            }

            List<String> mergedAccount = new ArrayList<>();
            mergedAccount.add(accounts.get(i).get(0)); // the name
            mergedAccount.addAll(mergedMails.get(i));  // already sorted

            result.add(mergedAccount);
        }

        return result;
    }

}
```

---

### Why `TreeSet<String>` Instead of `List<String>` + Manual Sort?

This is a small implementation choice worth calling out explicitly, since it deviates slightly from "sort at the end" patterns seen in other problems:

```
┌─────────────────────────────────────────────────────────────────────┐
│  List<String> + Collections.sort() at the very end:                 │
│    → simple, but requires a separate O(k log k) sort call           │
│      per bucket, done AFTER all emails are collected                │
│                                                                     │
│  TreeSet<String> (self-sorting on insert):                          │
│    → every .add() call maintains sorted order automatically         │
│      (O(log k) per insertion, backed by a Red-Black tree)           │
│    → by the time Phase 2 finishes, every bucket is ALREADY          │
│      sorted — no separate sort pass needed in Phase 3               │
│    → also silently de-duplicates if the same email somehow          │
│      appeared twice within one account's own list (defensive,       │
│      though the problem doesn't guarantee this can happen)          │
│                                                                     │
│  Both are O(k log k) total per bucket either way — TreeSet          │
│  just distributes that cost across insertion instead of             │
│  paying it in one lump sort at the end. Either is acceptable;       │
│  TreeSet keeps Phase 3 simpler to read.                             │
└─────────────────────────────────────────────────────────────────────┘
```

---

### Dry Run Confirmation (Matches Part 1 Exactly)

```
n = 6
Accounts: 0=John(J1,J2,J3)  1=John(J4)  2=raj(r1,r2)
          3=John(J1,J5)     4=raj(r2,r3)  5=Mary(M1)

PHASE 1:
  i=0: J1→map{J1:0}, J2→map{J2:0}, J3→map{J3:0}
  i=1: J4→map{J4:1}
  i=2: r1→map{r1:2}, r2→map{r2:2}
  i=3: J1 already at 0 → union(3,0) ; J5→map{J5:3}
  i=4: r2 already at 2 → union(4,2) ; r3→map{r3:4}
  i=5: M1→map{M1:5}

After Phase 1, DSU:
  findUParent(3) = 0
  findUParent(4) = 2
  findUParent(0)=0, findUParent(1)=1, findUParent(2)=2, findUParent(5)=5

PHASE 2 (route every map entry through findUParent):
  J1(idx0)→0, J2(idx0)→0, J3(idx0)→0, J5(idx3)→0
    bucket[0] = {J1, J2, J3, J5}  (TreeSet auto-sorts)
  J4(idx1)→1
    bucket[1] = {J4}
  r1(idx2)→2, r2(idx2)→2, r3(idx4)→2
    bucket[2] = {r1, r2, r3}
  bucket[3] = {}   (empty — absorbed into 0)
  bucket[4] = {}   (empty — absorbed into 2)
  M1(idx5)→5
    bucket[5] = {M1}

PHASE 3:
  i=0: not empty → ["John", J1, J2, J3, J5]
  i=1: not empty → ["John", J4]
  i=2: not empty → ["raj", r1, r2, r3]
  i=3: EMPTY → skip
  i=4: EMPTY → skip
  i=5: not empty → ["Mary", M1]

FINAL: matches expected output exactly ✓
```

---

## Complexity Analysis

### Time Complexity — O(N · K · log(N·K))

Let `N` = number of accounts, `K` = maximum number of emails in any single account. Let's derive this carefully, the way the workflow structure demands.

**Phase 1 — scanning and unioning:**

```
Total emails across all accounts ≤ N × K
For each email:
  → one HashMap lookup/insert       → O(1) average
  → occasionally one unionBySize()  → O(4α) ≈ O(1) amortized

Phase 1 total: O(N × K)
```

**Phase 2 — regrouping via findUParent:**

```
Total entries in mailToIndex ≤ N × K (one per distinct email)
For each entry:
  → one findUParent() call          → O(4α) ≈ O(1) amortized
  → one TreeSet.add()               → O(log(bucket size)) ≤ O(log(N·K))

Phase 2 total: O(N × K × log(N·K))
```

**Phase 3 — building the answer:**

```
For each account index (N of them):
  → check emptiness                 → O(1)
  → copy the (already-sorted) bucket into the result → O(bucket size)

Sum of all bucket sizes across all indices ≤ N × K

Phase 3 total: O(N × K)
```

```
Overall Time: O(N·K) + O(N·K·log(N·K)) + O(N·K)
            = O(N·K·log(N·K))
```

The `TreeSet` insertions in Phase 2 are what dominate — this is the same cost you'd pay with a manual sort-per-bucket approach at the end, just distributed across insertion time instead of a single batch sort.

### Space Complexity — O(N · K)

```
DisjointSet (parent[] + size[])   → O(N)
mailToIndex HashMap                → O(N × K)  — one entry per distinct email
mergedMails (N TreeSets)           → O(N × K)  — every email lives in exactly
                                      one bucket by the end
result list                        → O(N × K)
─────────────────────────────────────
Total: O(N × K)
```