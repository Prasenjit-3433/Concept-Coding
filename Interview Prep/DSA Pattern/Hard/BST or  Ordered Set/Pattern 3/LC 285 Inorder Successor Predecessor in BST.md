# LC 285. Inorder Successor/Predecessor in BST

Key Concept: Successor = leftmost in right subtree, or lowest ancestor where node is in left subtree
Solution: https://www.youtube.com/watch?v=SXKAD2svfmI&list=PLgUwDviBIf0q8Hkd7bK2Bpryj2xVJk8Vk&index=50&ab_channel=takeUforward
Status: Done

# LC 285 / GFG: Predecessor and Successor in a BST

🪔*(A **scoping** note before we start: LC 285 itself is locked behind LeetCode Premium, and — as you correctly flagged — Striver's transcript actually teaches a narrower problem than what GFG's "Predecessor and Successor" asks for. I'll build this note around the GFG version (find both predecessor and successor, given a `key` that may or may not exist as an actual node), use Striver's taught technique as the foundation, and dedicate a full section to exactly why his single-successor method can't be blindly doubled up — this is genuinely the most important thing to internalize in this note, not a footnote.)*

---

# Stage 1: Identification

## **Step 1 — Which topic?**

You're given the root of a **Binary Search Tree** and a `key` value. You need to find that key's **inorder predecessor** (the largest value strictly smaller than `key`) and **inorder successor** (the smallest value strictly greater than `key`) — and critically, the `key` is **not guaranteed to exist** as an actual node in the tree.

## **Step 2 — Which pattern?**

Still **Pattern 3: BST Navigation**. The trigger:

> "inorder successor", "inorder predecessor", in a BST → **BST Navigation**
> 

## **Step 3 — Which key concept?**

**`BST Compass with Ancestor Fallback` — when the key matches an actual node, predecessor/successor are found via direct subtree lookup (rightmost of left subtree / leftmost of right subtree); when the key isn't present, they're simply the last ancestors recorded while descending toward where the key *would* be.**

The sharper name matters here because Striver's own lecture teaches a version of this idea that only ever needs *one* of these two mechanisms (ancestor tracking) to solve successor-only, for a node *guaranteed* to already exist in the tree. The GFG problem needs **both** mechanisms working together correctly in a single pass — which is exactly where a naive "just do the mirror-image of Striver's rule twice" attempt breaks.

---

# Stage 2: Intuition Building

## The Tree We're Working With

```
              8
            /   \
           3     10
          / \       \
         1   6       14
            / \      /
           4   7    13
```

Inorder: `1, 3, 4, 6, 7, 8, 10, 13, 14`

## Part A — Striver's Taught Technique (Successor Only, Node Guaranteed Present)

Striver's problem gives you a **node** `p` that is already sitting inside the tree, and asks only for its successor. His rule:

```
curr = root
successor = null

while curr != null:
    if curr.val <= p.val:
        → curr is not big enough (or is exactly p itself) → move right
    else:
        → curr is a CANDIDATE successor — bigger than p
        → but there might be something even smaller-yet-still-bigger
          further down the left side, so record it and keep looking left
        successor = curr
        curr = curr.left
```

The reason `<=` (not just `<`) is safe here: the moment `curr` equals `p` itself, treating it as "not big enough yet" and moving right is *exactly* correct — you don't want the node itself reported as its own successor, and moving right is precisely where its successor (if any) would live, either directly (leftmost of its right subtree) or via later backtracking through stored ancestors.

**Trace: successor of node `6`.**

```
curr=8: 8 > 6 → candidate! successor=8, move left → curr=3
curr=3: 3 <= 6 → not big enough → move right → curr=6
curr=6: 6 <= 6 → not big enough (this IS the node itself) → move right → curr=7
curr=7: 7 > 6 → candidate! successor=7, move left → curr=null
stop.
```

**Successor of 6 = 7.** Correct — matches the inorder sequence `..., 6, 7, ...` directly.

This works beautifully *because it only ever has to track one thing*. Every step either says "not there yet, go right" or "found a better candidate, go left." There's no competing second piece of state to keep consistent.

---

## Part B — Why You Cannot Just Mirror This Rule to Also Get the Predecessor

The tempting move: run the *exact same kind of walk*, but symmetric — track predecessor with `>=` / move left, track successor with `<` / move right, **in the same single pass**:

```
if curr.data <= key:  predecessor = curr;  curr = curr.right
else:                 successor = curr;    curr = curr.left
```

Let's actually run this on `key = 6` (a node that **exists** in the tree) and see what happens:

```
curr=8: 8 > 6 → successor = 8, move left → curr=3
curr=3: 3 <= 6 → predecessor = 3, move right → curr=6
curr=6: 6 <= 6 → predecessor = 6, move right → curr=7
curr=7: 7 > 6 → successor = 7, move left → curr=null
stop.

Result: predecessor = 6, successor = 7
```

**Predecessor = 6 is wrong.** The predecessor of `6` cannot be `6` itself — it must be strictly smaller. The correct predecessor of `6` is `4` (inorder: `..., 4, 6, 7, ...`).

```
┌──────────────────────────────────────────────────────────────────────┐
│  THE **CAVEAT**, PRECISELY STATED:                                   │
│                                                                      │
│  Striver's <=-based rule is safe for successor-only because          │
│  the equality case (curr == key) is swallowed into "move             │
│  right, keep looking" — which is the CORRECT direction to            │
│  find the true successor.                                            │
│                                                                      │
│  But that same equality case, if it's ALSO used to assign            │
│  predecessor = curr, incorrectly reports the key node ITSELF         │
│  as its own predecessor. The two rules are not safely                │
│  symmetric to run together in one pass, because "move right          │
│  on equality" is the right call for successor, but "assign           │
│  on equality, then move right" is the WRONG call for                 │
│  predecessor — assigning must never happen at equality.              │
└──────────────────────────────────────────────────────────────────────┘
```

So the moment the key is **actually present as a node**, you cannot rely on directional ancestor-tracking alone for both quantities at once. You need to handle that case *specially* the instant you land on it.

---

## The Fix — What to Do the Moment You Land Exactly on the Key

When you finally reach a node where `curr.data == key`, you are standing **exactly** at the value in question. Ask directly:

> *"Given that I'm standing on the exact key node, where do its inorder neighbors live?"*
> 
- The **predecessor** — the largest value smaller than `key` — is the **rightmost node of the key's own left subtree** (if it has one). Walking as far right as possible inside a left subtree always lands on the largest value in that subtree, and everything in that subtree is already guaranteed smaller than `key`.
- The **successor** — the smallest value bigger than `key` — is the **leftmost node of the key's own right subtree** (if it has one), by the mirrored argument.

```
┌──────────────────────────────────────────────────────────────────────┐
│  When curr.data == key:                                              │
│                                                                      │
│  predecessor = rightmost node of curr's LEFT subtree                 │
│                (if a left subtree exists)                            │
│                                                                      │
│  successor   = leftmost node of curr's RIGHT subtree                 │
│                (if a right subtree exists)                           │
└──────────────────────────────────────────────────────────────────────┘
```

**What if the key node has no left subtree at all?** Then there's nothing to look up locally — but this is exactly where the **ancestor tracking you were already doing on the way down** saves you. Every time you moved *right* while descending (because the current node was smaller than `key`), you recorded that node as a predecessor candidate. If the key node itself has no left subtree, whatever predecessor value you last recorded from an ancestor is *already* the correct answer — you simply don't overwrite it. Same logic, mirrored, for successor when there's no right subtree.

### Trace: `key = 8` (the root itself, both subtrees present)

```
curr = 8: 8 == key → landed immediately, no ancestor tracking happened yet

  Left subtree of 8 exists (rooted at 3):
    rightmost walk: 3 → 6 → 7 (7.right is null, stop)
    predecessor = 7

  Right subtree of 8 exists (rooted at 10):
    leftmost walk: 10 (10.left is null immediately, stop)
    successor = 10
```

**Predecessor = 7, Successor = 10.** Check against inorder: `..., 7, 8, 10, ...` ✓. Notice `10` has no left child at all, so the "leftmost" walk terminates immediately at `10` itself — this correctly handles the case where the right subtree's own leftmost path is trivially short.

### Trace: `key = 6`, Done Correctly This Time

```
curr=8: 8 > 6 → successor = 8, move left → curr=3
curr=3: 3 < 6 → predecessor = 3, move right → curr=6
curr=6: 6 == key → landed on it

  Left subtree of 6 exists (rooted at 4, since 6's left child is 4):
    rightmost walk: 4 (4.right is null, stop)
    predecessor = 4   ← overwrites the ancestor-tracked 3

  Right subtree of 6 exists (rooted at 7):
    leftmost walk: 7 (7.left is null, stop)
    successor = 7   ← overwrites the ancestor-tracked 8

stop (break immediately once the key is found)
```

**Predecessor = 4, Successor = 7.** Correct, matching inorder `..., 4, 6, 7, ...` exactly. This also shows *why* the ancestor-tracked values (`predecessor=3`, `successor=8`) are treated purely as **fallbacks** — the moment a real subtree-based value is available, it always takes priority, because it's strictly closer to `key` than anything an ancestor could offer.

### Why the Ancestor-Tracked Values Are Always Safe as a Fallback

This deserves to be stated explicitly, because it's the piece that makes the whole approach correct without extra bookkeeping. While walking down *before* reaching the key (or the point where the key would be, if absent), every time you move **right** because the current node is smaller than `key`, that node genuinely is a value smaller than `key` and lying on the path toward it — a legitimate predecessor candidate, and the *most recent* one is always the tightest (largest) one recorded so far. The symmetric argument holds for successor candidates recorded while moving left. Nothing about this reasoning depends on whether the key itself turns out to exist as a node — it holds regardless, which is exactly why it doubles as the correct answer outright when the key is **absent**, and as a safe fallback when the key **is present** but one side's subtree happens to be missing.

---

### What Happens When the Key Doesn't Exist At All

If `key = 5` (not a node in this tree):

```
curr=8: 8 > 5 → successor = 8, move left → curr=3
curr=3: 3 < 5 → predecessor = 3, move right → curr=6
curr=6: 6 > 5 → successor = 6, move left → curr=4
curr=4: 4 < 5 → predecessor = 4, move right → curr=null
stop (curr became null — walk ended without ever matching)
```

**Predecessor = 4, Successor = 6.** No special-case subtree lookup ever triggers — the walk simply runs out of tree, and whatever was last recorded on each side is the answer. This is the exact same logic that powers LC 700's search: if the key were present, the walk would have landed on it; running off the end into `null` is proof it isn't there, and the accumulated ancestor values are already correct.

---

# Stage 3: Coding

## Approach 1 — Brute Force: Inorder Collect + Linear/Binary Scan

**The honest thinking:**

> "Collect the inorder traversal into a sorted list. Find where `key` would sit, and read off its neighbors."
> 

```java
class Solution {
    public ArrayList<Node> findPreSuc(Node root, int key) {
        List<Integer> values = new ArrayList<>();
        inorder(root, values);

        Node predecessor = null, successor = null;
        for (int v : values) {
            if (v < key) predecessor = new Node(v);   // simplified for illustration
            if (v > key && successor == null) successor = new Node(v);
        }
        return new ArrayList<>(Arrays.asList(predecessor, successor));
    }

    private void inorder(Node node, List<Integer> values) {
        if (node == null) return;
        inorder(node.left, values);
        values.add(node.data);
        inorder(node.right, values);
    }
}
```

Correct, but pays `O(n)` time and `O(n)` space to store the entire tree, when in the best case the answer sits just a few steps down a single path. Establishes correctness only.

---

## Approach 2 — Your Iterative Inorder With Early Break

**The thinking:**

> "Simulate the standard iterative inorder traversal. Every value smaller than `key` becomes the running predecessor candidate. The instant a value bigger than `key` shows up, that's the successor — stop immediately, no need to see the rest of the tree."
> 

```java
class Solution {
    public ArrayList<Node> findPreSuc(Node root, int key) {
        Node predecessor = null, successor = null;

        Stack<Node> st = new Stack<>();
        Node curr = root;

        while (true) {
            if (curr != null) {
                // Standard iterative inorder — push while going left
                st.push(curr);
                curr = curr.left;
            } else {
                if (st.isEmpty()) break;

                curr = st.pop();

                // This node is being "visited" in ascending order.
                // Anything smaller than key keeps refining the
                // predecessor — later (larger) values overwrite
                // earlier ones, since we want the LARGEST value
                // still smaller than key.
                if (curr.data < key) predecessor = curr;

                // The FIRST value strictly greater than key, in
                // ascending order, IS the successor — nothing
                // further in the traversal can beat it, so stop.
                if (curr.data > key) {
                    successor = curr;
                    break;
                }

                curr = curr.right;
            }
        }

        return new ArrayList<>(Arrays.asList(predecessor, successor));
    }
}
```

This is fully correct for every case — key present, key absent, key at an extreme — because it's directly simulating the sorted sequence itself and picking out neighbors, with no special-casing needed anywhere. If `curr.data == key` exactly, neither `if` fires, so the key node is correctly excluded from being reported as its own predecessor or successor, and the walk simply continues rightward from it to find the true successor.

**Why this isn't the fully optimal approach, though:** its cost is bounded by *how far into the inorder sequence you have to walk before hitting the first value greater than `key`* — not by the tree's height. If `key` is larger than every value in the tree, `successor` never gets set and the traversal runs through the **entire** tree before the stack empties. Worst case: `O(n)`.

---

## Approach 3 — Optimal: BST Compass With Ancestor Fallback

**Mental workflow before writing a single line:**

```
1. Walk from the root using the BST compass:
   → curr.data > key: curr is a successor CANDIDATE (ancestor
     fallback) → record it, move LEFT (look for something
     smaller-but-still-bigger)
   → curr.data < key: curr is a predecessor CANDIDATE (ancestor
     fallback) → record it, move RIGHT
   → curr.data == key: landed exactly on the key node —
       → if it has a left subtree: predecessor = rightmost
         node of that subtree (overrides the ancestor fallback)
       → if it has a right subtree: successor = leftmost
         node of that subtree (overrides the ancestor fallback)
       → stop, this is final

2. If the walk runs off into null without ever matching,
   whatever was last recorded on each side is already correct
   (key doesn't exist — same guarantee as LC 700's search)
```

```java
class Solution {
    public ArrayList<Node> findPreSuc(Node root, int key) {
        Node predecessor = null, successor = null;

        Node curr = root;

        while (curr != null) {
            if (curr.data > key) {
                // curr is bigger than key — a CANDIDATE successor.
                // There might be something even closer (smaller,
                // but still > key) further down the left side.
                successor = curr;
                curr = curr.left;
            } else if (curr.data < key) {
                // curr is smaller than key — a CANDIDATE predecessor.
                // Something closer (bigger, but still < key) might
                // be further down the right side.
                predecessor = curr;
                curr = curr.right;
            } else {
                // curr.data == key — landed exactly on the node.
                // This is the ONE case that cannot be resolved by
                // simple directional movement — must look directly
                // into each subtree.

                // Predecessor = largest value smaller than key
                //             = rightmost node of the LEFT subtree
                if (curr.left != null) {
                    Node rightMost = curr.left;
                    while (rightMost.right != null) {
                        rightMost = rightMost.right;
                    }
                    predecessor = rightMost;
                    // if curr.left == null, predecessor keeps
                    // whatever ancestor value was already recorded
                }

                // Successor = smallest value bigger than key
                //           = leftmost node of the RIGHT subtree
                if (curr.right != null) {
                    Node leftMost = curr.right;
                    while (leftMost.left != null) {
                        leftMost = leftMost.left;
                    }
                    successor = leftMost;
                    // if curr.right == null, successor keeps
                    // whatever ancestor value was already recorded
                }

                break;
            }
        }

        return new ArrayList<>(Arrays.asList(predecessor, successor));
    }
}
```

---

## Comparing the Two Correct Approaches

| Property | Stack-Based Inorder (Approach 2) | BST Compass + Fallback (Approach 3) |
| --- | --- | --- |
| Uses BST ordering to prune? | Only partially — still walks the full ascending sequence up to the answer | Fully — one comparison eliminates a whole subtree at every step |
| Worst-case time | O(n) — if successor is never found, traverses everything | O(h) — always bounded by tree height, no exceptions |
| Handles "key present" specially? | No — falls out naturally from the traversal order | Yes — explicit subtree lookups the instant of exact match |
| Extra space | O(h) — explicit stack | O(1) — just pointer reassignment |

---

## Workflow Trace Summary (BST Compass version, `key = 6`)

```
curr=8: 8>6 → successor=8 (fallback), move left → curr=3
curr=3: 3<6 → predecessor=3 (fallback), move right → curr=6
curr=6: 6==6 → EXACT MATCH
    left subtree exists (rooted at 4) → rightmost walk: 4 → predecessor=4
    right subtree exists (rooted at 7) → leftmost walk: 7 → successor=7
    stop

Final: predecessor=4, successor=7
```

---

## Complexity Analysis

**Time Complexity — O(h), where h is the height of the tree:**

The walk moves to exactly one child per step — never both — right up until it either finds the exact key (then does at most one bounded rightmost/leftmost walk into a *single* subtree, itself bounded by that subtree's height, so still `O(h)` overall) or runs out of tree entirely. No node outside this single root-to-key path (plus at most one extra descent into one subtree) is ever touched.

- **Balanced BST:** `h = O(log n)`.
- **Worst case (skewed BST):** `h = O(n)`.

**Space Complexity — O(1):**

The iterative compass walk uses only a handful of pointer variables — no stack, no recursion, no list.

---

## The Key Takeaway

```
┌───────────────────────────────────────────────────────────────────────┐
│  STRIVER'S TAUGHT RULE (successor only, node guaranteed               │
│  present) is safe with a single <=/move-right rule, because           │
│  equality is swallowed into "keep looking right" — the                │
│  correct direction for successor.                                     │
│                                                                       │
│  THE CAVEAT: mirroring that same rule to ALSO get predecessor         │
│  in one pass breaks the instant the key IS a real node — it           │ 
│  wrongly reports the key node as its own predecessor, because         │
│  the equality case gets misused to ASSIGN rather than just            │ 
│  redirect.                                                            │
│                                                                       │
│  THE FIX: use STRICT comparisons for ancestor tracking on the         │
│  way down (both sides), and handle the exact-match case               │
│  SEPARATELY and EXPLICITLY:                                           │
│      predecessor = rightmost of LEFT subtree (if it exists)           │
│      successor   = leftmost  of RIGHT subtree (if it exists)          │
│  If a needed subtree doesn't exist, the ancestor value already        │
│  recorded during descent is automatically the correct fallback        │
│  — no extra logic needed for that case.                               │
│                                                                       │
│  Time  — O(h): bounded by tree height throughout, no exceptions.      │
│  Space — O(1): pure iterative pointer walk.                           │
└───────────────────────────────────────────────────────────────────────┘
```