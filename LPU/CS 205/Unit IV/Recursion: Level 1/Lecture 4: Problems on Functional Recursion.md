# Problems on Functional Recursion

Status: Done

### `Problem 1`: Reverse an Array Using Recursion

**The goal:** given an array like `[1, 2, 3, 4, 5]`, produce `[5, 4, 3, 2, 1]` — but using recursion instead of a loop.

**First, recall how you'd do this iteratively:** keep two pointers, one at the start (`l`) and one at the end (`r`), swap the elements they point to, then move `l` forward and `r` backward, repeating until they meet or cross.

```
[1, 2, 3, 4, 5]
 l           r     → swap → [5, 2, 3, 4, 1]
    l     r          → swap → [5, 4, 3, 2, 1]
       l,r            → pointers meet, stop
```

The recursive version does exactly this — each call handles one swap, then calls itself for the next pair.

#### Approach A: Two-Pointer Recursion

```cpp
void reverseArray(int arr[], int l, int r) {
    if (l >= r) {              // base case: pointers met or crossed
        return;
    }
    swap(arr[l], arr[r]);
    reverseArray(arr, l + 1, r - 1);   // move inward
}

int main() {
    int arr[] = {1, 3, 2, 3, 5, 4};
    int n = 6;
    reverseArray(arr, 0, n - 1);
}
```

**Tracing through a smaller example, `[1, 3, 5, 4]` (n = 4):**

```
reverseArray(arr, 0, 3) → swap arr[0], arr[3] → [4,3,5,1] → call reverseArray(arr, 1, 2)
reverseArray(arr, 1, 2) → swap arr[1], arr[2] → [4,5,3,1] → call reverseArray(arr, 2, 1)
reverseArray(arr, 2, 1) → 2 >= 1 → base case → return
```

Each call does one swap and then hands off to the next pair, moving `l` and `r` toward each other until they meet or cross.

#### Approach B: Single-Pointer Recursion

Instead of tracking two separate pointers, you can compute the "partner" index from a single pointer `i`. For an array of size `n`, the element that pairs with index `i` is always at index `n - i - 1`.

```
Array:  [1, 3, 5, 4, 0]   (n = 5)
Index:   0  1  2  3  4

i = 0  →  partner = n - 0 - 1 = 4
i = 1  →  partner = n - 1 - 1 = 3
i = 2  →  partner = n - 2 - 1 = 2   (this is the middle — no swap needed)
```

You keep swapping as long as `i` hasn't crossed the halfway point (`n / 2`):

```cpp
void reverseArray(int arr[], int i, int n) {
    if (i >= n / 2) {                    // base case: crossed the midpoint
        return;
    }
    swap(arr[i], arr[n - i - 1]);
    reverseArray(arr, i + 1, n);           // move to the next pair
}

int main() {
    int arr[] = {1, 2, 3, 4, 5};
    int n = 5;
    reverseArray(arr, 0, n);

    for (int i = 0; i < n; i++) {
        cout << arr[i] << " ";
    }
    // Output: 5 4 3 2 1
}
```

**Note on passing arrays:** as we covered in Unit II, arrays passed to functions decay into pointers — so `reverseArray` is working on and modifying the *original* array directly, not a copy. That's exactly why the changes made inside the function are visible in `main()` afterward.

**Key skill this teaches:** identifying *exactly* what task each recursive call is responsible for. Here, that task is "swap this one pair, then hand off the next pair to another call of yourself."

---

### `Problem 2`: Check If a String Is a Palindrome (Using Recursion)

**Definition:** a string is a **palindrome** if it reads the same forwards and backwards. Examples: `"madam"`, `"11211"`.

**The connection to Problem 1:** reversing a string works by swapping the left half with the right half. So a string is a palindrome exactly when, if you were to "reverse" it, every left-right pair you'd swap turns out to already be *equal* to each other. Instead of swapping, we just **compare**.

```
"madam"
 m a d a m
 ↑       ↑   compare s[0] and s[4] → 'm' == 'm' ✓
   ↑   ↑     compare s[1] and s[3] → 'a' == 'a' ✓
     ↑       middle character — no partner needed, automatically fine
```

Since this needs to report `true`/`false` back to whoever called it, this has to be **functional recursion** (returns a value), not void recursion.

```cpp
bool isPalindrome(string &s, int i) {
    int n = s.size();

    if (i >= n / 2) {                    // base case: checked all pairs
        return true;
    }
    if (s[i] != s[n - i - 1]) {          // mismatch found
        return false;
    }
    return isPalindrome(s, i + 1);        // check the next pair
}

int main() {
    string s = "madam";
    cout << isPalindrome(s, 0);   // prints 1 (true)
}
```

**Tracing for `"madam"` (n = 5):**

```
isPalindrome(s, 0) → 0 >= 2? No → s[0]='m', s[4]='m' → equal → check isPalindrome(s, 1)
isPalindrome(s, 1) → 1 >= 2? No → s[1]='a', s[3]='a' → equal → check isPalindrome(s, 2)
isPalindrome(s, 2) → 2 >= 2? Yes → base case → return true
```

That `true` now travels back up through each waiting call:

```
isPalindrome(s, 1) returns true (whatever isPalindrome(s,2) returned)
isPalindrome(s, 0) returns true
```

Final result: `true`.

**Now trace a non-palindrome, `"madxm"` → actually let's use `"maam"` broken deliberately, e.g. `"mads"` vs `"madam"` — take `"madem"`:**

Actually, a cleaner mismatching example: `"madan"` (n = 6, but let's keep it simple with `"mad"` vs itself) — using `"maed"`-style, the key point is:

```
isPalindrome(s, 0) → s[0] != s[n-1] → mismatch found → return false immediately
```

The moment any pair doesn't match, that call returns `false` right away — it does **not** bother checking the remaining pairs. That `false` then propagates straight back up through every waiting call, so the final answer is `false` without ever reaching the base case.

**This is an important recursion pattern:** a `false` (or any "stop early" signal) returned partway through doesn't need to keep recursing — it just needs to bubble back up, call by call, all the way to the top.

**Time and space complexity:** since we only need to check up to the halfway point of the string, we make roughly `n/2` recursive calls — so **time complexity is O(n/2)**, and since each of those calls sits in the stack waiting for the one below it, **space complexity is also O(n/2)** (auxiliary stack space, no extra array used).

---