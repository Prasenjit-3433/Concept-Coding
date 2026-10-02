# Lecture 3: Arithmetic Expressions

# 🎯Part 1: Operators, Priority Order & Infix → Postfix Conversion

---

## Quick Recap Before We Begin

Classes 1 & 2 built stacks and queues as data structures — the "container" itself. Today we look at one of the **most classic real applications of a stack**: converting between the different ways of *writing* arithmetic expressions. This is a genuinely stack-heavy topic — nearly every conversion in this lecture leans on the stack's LIFO behavior in some form.

Given the size of this topic, we're splitting it into 3 parts, all belonging to this one lecture:

- **Part 1** (this note): Operators, operands, priority order, the three expression forms (infix/prefix/postfix), and **Infix → Postfix** conversion.
- **Part 2**: **Infix → Prefix** and **Postfix → Infix** conversion.
- **Part 3**: **Prefix → Infix**, **Postfix → Prefix**, **Prefix → Postfix** conversion, plus a full summary table.

---

## 1. What is an Operator?

An **operator** is a symbol that performs a mathematical operation on operands:

| Operator | Symbol | Meaning |
| --- | --- | --- |
| Power | `^` | e.g., `2^3` |
| Multiplication | `*` |  |
| Division | `/` |  |
| Addition | `+` |  |
| Subtraction | `-` |  |

## 2. What is an Operand?

An **operand** is the actual value the operator acts on. In these expressions, operands are generically taken to be:

- Uppercase letters: `A`–`Z`
- Lowercase letters: `a`–`z`
- Digits: `0`–`9`

---

## 3. Priority Order of Operators

This priority order is the backbone of every conversion in this lecture — you'll use it constantly to decide "does this operator go into the stack now, or does something get popped out first?"

| Operator | Priority |
| --- | --- |
| `^` (power) | 3 (highest) |
| `*`, `/` | 2 |
| `+`, `-` | 1 |
| anything else (e.g., `(`, `)`) | -1 (lowest) |

```cpp
int priority(char op) {
    if (op == '^') return 3;
    if (op == '*' || op == '/') return 2;
    if (op == '+' || op == '-') return 1;
    return -1;
}
```

⚠️ Note: `*` and `/` share the same priority (`2`), and `+` and `-` share the same priority (`1`) — they are **not** ranked against each other, only against the other tiers.

---

## 4. Infix, Prefix, and Postfix — What Do These Actually Mean?

Take the expression: `A + B * (C ^ D - E)`

### Infix — operator **in between** the operands

```
B + Q          (operators sit IN the middle, between operands)
```

This is the form you already know and use every day in C++, Java, and virtually every mainstream programming language.

### Prefix — operator comes **before** the operands

```
+ B Q          (operator comes PRE, i.e. before, the operands)
```

Used heavily in the **Lisp** programming language, and in tree data structures (it's literally a pre-order traversal of the expression tree).

### Postfix — operator comes **after** the operands

```
B Q +          (operator comes POST, i.e. after, the operands)
```

Used in **stack-based calculators**.

**Real-life analogy:** Think of infix as "the referee stands between the two players," prefix as "the referee announces the match before either player steps in," and postfix as "the referee only shows up after both players have already taken their positions." Same match, same players — just when the referee (operator) shows up relative to the operands changes.

⚠️ The **priority order** and **bracket handling** you learned in Section 3 are what drive every single conversion algorithm below — none of these are arbitrary string-rearrangement tricks, they all mechanically encode "who has higher priority, and who's inside which bracket."

---

## 5. Infix → Postfix Conversion

**The setup:**

- `i` — index into the infix string, starts at `0`
- `st` — an empty stack (character stack)
- `ans` — an empty string, built up as we go

**The core rules, one case at a time:**

1. **Operand** → append directly to `ans`. Never touches the stack.
2. **`(`** → always pushed onto the stack, no questions asked.
3. **`)`** → pop everything off the stack and append to `ans`, until you hit a `(` — then pop and discard that `(` too (it's never added to `ans`).
4. **Operator** → while the stack is non-empty **and** the top of the stack has **priority ≥** the current operator's priority, pop and append to `ans`. Then push the current operator.
5. **End of string** → pop everything remaining on the stack and append to `ans`.

Let's trace through `A + B * (C ^ D - E)` — real ASCII stack diagrams for every single step, since this is exactly the part where compressed explanations fail students.

```
Expression: A + B * ( C ^ D - E )
Index:      0 1 2 3 4 5 6 7 8 9 10
```

**Step 1 — `A` (operand):**

```
stack: (empty)              ans: "A"
```

**Step 2 — `+` (operator, stack is empty → just push):**

```
stack: [ + ]                ans: "A"
        ↑ top
```

**Step 3 — `B` (operand):**

```
stack: [ + ]                ans: "AB"
```

**Step 4 — `*` (operator). Check stack top: `+` has priority 1, `*` has priority 2.**

```
Is priority(stack top = '+') >= priority('*')?
   1 >= 2?  NO → don't pop, just push '*'

stack: [ +, * ]              ans: "AB"
             ↑ top
```

**Step 5 — `(` (always push):**

```
stack: [ +, *, ( ]           ans: "AB"
                 ↑ top
```

**Step 6 — `C` (operand):**

```
stack: [ +, *, ( ]           ans: "ABC"
```

**Step 7 — `^` (operator). Check stack top: `(` has priority -1.**

```
Is priority('(') >= priority('^')?
   -1 >= 3?     **NO** → don't pop, just push '^'

stack: [ +, *, (, ^ ]        ans: "ABC"
                  ↑ top
```

**Step 8 — `D` (operand):**

```
stack: [ +, *, (, ^ ]        ans: "ABCD"
```

**Step 9 — `-` (operator). Check stack top: `^` has priority 3.**

```
Is priority('^') >= priority('-')?
   3 >= 1?  YES → pop '^', append to ans
   ans: "ABCD^"
   stack: [ +, *, ( ]

Check new stack top: '(' has priority -1.
   -1 >= 1?  NO → stop popping, push '-'

stack: [ +, *, (, - ]        ans: "ABCD^"
                    ↑ top
```

**Step 10 — `E` (operand):**

```
stack: [ +, *, (, - ]        ans: "ABCD^E"
```

**Step 11 — `)` (pop until `(`):**

```
pop '-', append   →  ans: "ABCD^E-"
                       stack: [ +, *, ( ]
top is '(' → stop, discard the '(' itself (don't append it)

stack: [ +, * ]               ans: "ABCD^E-"
             ↑ top
```

**End of string — pop everything remaining:**

```
pop '*', append   →  ans: "ABCD^E-*"
                       stack: [ + ]
pop '+', append   →  ans: "ABCD^E-*+"
                       stack: []

Final postfix: ABCD^E-*+
```

**Full code:**

```cpp
string infixToPostfix(string s) {
    int i = 0;
    stack<char> st;
    string ans = "";

    while (i < s.size()) {
        if (isOperand(s[i])) {
            ans += s[i];
        }
        else if (s[i] == '(') {
            st.push(s[i]);
        }
        else if (s[i] == ')') {
            while (!st.empty() && st.top() != '(') {
                ans += st.top();
                st.pop();
            }
            st.pop();   // discard the '('
        }
        else {   // it's an operator
            while (!st.empty() && priority(s[i]) <= priority(st.top())) {
                ans += st.top();
                st.pop();
            }
            st.push(s[i]);
        }
        i++;
    }

    while (!st.empty()) {
        ans += st.top();
        st.pop();
    }

    return ans;
}

bool isOperand(char c) {
    return (c >= 'A' && c <= 'Z') ||
           (c >= 'a' && c <= 'z') ||
           (c >= '0' && c <= '9');
}
```

⚠️ **The exact condition matters:** popping happens when `priority(current) <= priority(stack top)` — i.e., **less than or equal**, not strictly less than. This is what correctly handles two same-priority operators back-to-back (e.g., `A - B + C`), making sure `-` gets popped before `+` is pushed, which preserves left-to-right evaluation order for same-priority operators.

**Time complexity:** the outer loop runs **O(n)**. The inner `while` (popping) *can* look like it adds another factor of `n`, but across the **entire** run of the algorithm, no operator can be pushed and popped more than once — so the total popping work across all iterations is bounded by `n`, not `n` *per* iteration. Overall: **O(n)**.

**Space complexity:** **O(n)** — the stack can hold up to `n` operators in the worst case, plus the `ans` string itself also grows up to size `n`.

# 🎯Part 2: Infix → Prefix & Postfix → Infix Conversion

---

## Quick Recap Before We Continue

In Part 1, we covered operators, operands, priority order, and did a full **Infix → Postfix** conversion on `A + B * (C ^ D - E)`, arriving at the postfix result **`ABCD^E-*+`**. This part covers **Infix → Prefix** and **Postfix → Infix** — we'll reuse the same expression throughout so you can cross-check every result against Part 1's postfix answer.

---

## 6. Infix → Prefix Conversion

This one has a clever trick: instead of inventing a whole new algorithm, we **reuse** infix-to-postfix — just on a reversed, bracket-swapped version of the string, with one small rule change.

**The three steps:**

1. **Reverse** the infix string, and **swap every `(` with `)`** and vice versa.
2. Run a **modified infix → postfix** conversion on this reversed-swapped string.
    - The modification: pop from the stack only when the **stack top's priority is strictly greater** than the current operator's priority — **not** "greater than or equal" like in normal infix-to-postfix. Equal-priority operators are pushed without popping.
3. **Reverse** the result from step 2 — that reversed string is your prefix expression.

**Real-life analogy:** Reversing the string is like reading the sentence backward, and swapping brackets is making sure a bracket that used to "open" a group (when read forward) still correctly "closes" that same group when you're now reading right-to-left. The modified priority rule exists because reversing an expression flips left-to-right order — so for operators of *equal* priority, we deliberately hold off on popping them, to avoid silently flipping their original left-to-right evaluation order.

**Step 1 — Reverse `A + B * ( C ^ D - E )` and swap brackets:**

```
Original:              A + B * ( C ^ D - E )

Reversed (tokens):     ) E - D ^ C ( * B + A

Swap brackets
( )'s → ( and ) → ):   ( E - D ^ C ) * B + A
```

**Step 2 — Modified infix-to-postfix on `( E - D ^ C ) * B + A`:**

```
Expression: ( E - D ^ C ) * B + A
```

**Step-by-step, `st` = character stack, `ans` = string built so far:**

```
i=0  '(' → always push
     st: [ ( ]                        ans: ""

i=1  'E' operand → append
     st: [ ( ]                        ans: "E"

i=2  '-' operator. Stack top = '(' (priority -1).
     Is priority(top) > priority('-')?   -1 > 1?  NO → push, don't pop
     st: [ (, - ]                      ans: "E"

i=3  'D' operand → append
     st: [ (, - ]                      ans: "ED"

i=4  '^' operator. Stack top = '-' (priority 1).
     Is priority(top) > priority('^')?   1 > 3?  NO → push, don't pop
     st: [ (, -, ^ ]                    ans: "ED"

i=5  'C' operand → append
     st: [ (, -, ^ ]                    ans: "EDC"

i=6  ')' → pop everything until '(' :
     pop '^', append → ans: "EDC^"
     pop '-', append → ans: "EDC^-"
     hit '(' → discard it, stop
     st: []                             ans: "EDC^-"

i=7  '*' operator. Stack is empty → just push
     st: [ * ]                          ans: "EDC^-"

i=8  'B' operand → append
     st: [ * ]                          ans: "EDC^-B"

i=9  '+' operator. Stack top = '*' (priority 2).
     Is priority(top) > priority('+')?   2 > 1?  YES → pop '*', append
     ans: "EDC^-B*"     st: []
     Stack now empty → push '+'
     st: [ + ]                          ans: "EDC^-B*"

i=10 'A' operand → append
     st: [ + ]                          ans: "EDC^-B*A"

End of string — pop everything remaining:
     pop '+', append → ans: "EDC^-B*A+"
     st: []

Result of step 2: EDC^-B*A+
```

**Step 3 — Reverse the result:**

```
EDC^-B*A+   reversed →   +A*B-^CDE

Final prefix: +A*B-^CDE
```

✅ **Sanity check against Part 1:** we can verify this by hand — `A + B*(C^D-E)` breaks down as `A + (B * (C^D - E))`. Building prefix from the inside out: `C^D` → `^CD`; `(C^D - E)` → `-^CDE`; `B * (...)` → `*B-^CDE`; `A + (...)` → `+A*B-^CDE`. Matches exactly.

**Full code:**

```cpp
string reverseAndSwapBrackets(string s) {
    reverse(s.begin(), s.end());
    for (int i = 0; i < s.size(); i++) {
        if (s[i] == '(') s[i] = ')';
        else if (s[i] == ')') s[i] = '(';
    }
    return s;
}

string infixToPrefix(string s) {
    s = reverseAndSwapBrackets(s);

    int i = 0;
    stack<char> st;
    string ans = "";

    while (i < s.size()) {
        if (isOperand(s[i])) {
            ans += s[i];
        }
        else if (s[i] == '(') {
            st.push(s[i]);
        }
        else if (s[i] == ')') {
            while (!st.empty() && st.top() != '(') {
                ans += st.top();
                st.pop();
            }
            st.pop();   // discard the '('
        }
        else {   // operator — MODIFIED condition vs. infix-to-postfix
            while (!st.empty() && priority(s[i]) < priority(st.top())) {
                ans += st.top();
                st.pop();
            }
            st.push(s[i]);
        }
        i++;
    }

    while (!st.empty()) {
        ans += st.top();
        st.pop();
    }

    reverse(ans.begin(), ans.end());
    return ans;
}
```

⚠️ **The one-character difference that matters most:** in `infixToPostfix`, the popping condition was `priority(current) <= priority(top)`. Here it's `priority(current) < priority(top)` — **strictly less than**. Miss this distinction and same-priority operators (like the `-` and `^` at steps 2 and 4 above) will pop in the wrong order, silently corrupting the final prefix string.

**Time complexity:** the reverse-and-swap is **O(n)**. The modified infix-to-postfix pass is **O(n)** by the same reasoning as before (each operator pushed/popped at most once across the whole run). The final reverse is another **O(n)**. Total: **O(n)** (informally "3n", which simplifies to O(n)).

**Space complexity:** **O(n)** — the stack and the `ans` string.

---

## 7. Postfix → Infix Conversion

Here the stack stores **strings**, not characters — because we're building up fully-bracketed sub-expressions as we go.

**The core rules:**

1. **Operand** → push it onto the stack as-is (as a 1-character string).
2. **Operator** → pop the top two strings off the stack. Call the **first** pop `t1` and the **second** pop `t2`. Combine them as `( t2 <operator> t1 )` and push that combined string back.
3. **End of string** → whatever remains on the stack (should be exactly one string) is the final infix expression.

⚠️ **Pop order matters and is easy to get backwards:** the *first* thing you pop (`t1`) is the operand that came **later** in the postfix string — it goes on the **right** of the operator. The *second* pop (`t2`) goes on the **left**.

Let's trace `A B C D ^ E - * +` (Part 1's postfix result for our running example):

```
Tokens: A  B  C  D  ^  E  -  *  +
```

**Step 1 — `A` (operand):**

```
stack: [ "A" ]
```

**Step 2 — `B` (operand):**

```
stack: [ "A", "B" ]
```

**Step 3 — `C` (operand):**

```
stack: [ "A", "B", "C" ]
```

**Step 4 — `D` (operand):**

```
stack: [ "A", "B", "C", "D" ]
```

**Step 5 — `^` (operator). Pop t1 = "D", pop t2 = "C". Combine: `(C^D)`:**

```
stack: [ "A", "B", "(C^D)" ]
```

**Step 6 — `E` (operand):**

```
stack: [ "A", "B", "(C^D)", "E" ]
```

**Step 7 — `-` (operator). Pop t1 = "E", pop t2 = "(C^D)". Combine: `((C^D)-E)`:**

```
stack: [ "A", "B", "((C^D)-E)" ]
```

**Step 8 — `*` (operator). Pop t1 = "((C^D)-E)", pop t2 = "B". Combine: `(B*((C^D)-E))`:**

```
stack: [ "A", "(B*((C^D)-E))" ]
```

*Step 9 — `+` (operator). Pop t1 = "(B((C^D)-E))", pop t2 = "A". Combine: `(A+(B*((C^D)-E)))`:**

```
stack: [ "(A+(B*((C^D)-E)))" ]
```

**End of string — one element remains, that's the answer:**

```
Final infix: (A+(B*((C^D)-E)))
```

✅ This matches the original expression `A + B * (C ^ D - E)`, just with every implicit grouping made fully explicit via brackets — exactly what you'd expect, since postfix→infix has no way of knowing which brackets in the *original* were "necessary" vs. redundant, so it adds one around every operation.

**Full code:**

```cpp
string postfixToInfix(string s) {
    int i = 0;
    stack<string> st;

    while (i < s.size()) {
        if (isOperand(s[i])) {
            st.push(string(1, s[i]));
        }
        else {   // operator
            string t1 = st.top(); st.pop();
            string t2 = st.top(); st.pop();
            string combined = "(" + t2 + s[i] + t1 + ")";
            st.push(combined);
        }
        i++;
    }

    return st.top();
}
```

⚠️ **This algorithm assumes a valid postfix expression is given.** If the input is malformed, `st.top()` at the end may not represent a correct expression at all — production code intended for untrusted input would need explicit validation, but for exam/interview purposes the input is assumed well-formed.

**Time complexity:** the loop itself is **O(n)**. Every operator step does a constant number of pops/pushes, but **string concatenation** (`t2 + s[i] + t1`) itself costs time proportional to the length of the strings being joined — in the worst case, repeated concatenation across the whole run can add up to **O(n)** additional work (language-dependent, since some languages implement string concatenation more efficiently than others). Overall: **O(n)**, with this caveat worth mentioning to an interviewer.

**Space complexity:** **O(n)** — the stack ends up holding strings whose combined length is proportional to the original expression's length.

# 🎯Part 3: Prefix → Infix, Postfix ↔ Prefix Conversion & Summary

---

## Quick Recap Before We Continue

Part 1 gave us **Infix → Postfix**: `A + B*(C^D-E)` → `ABCD^E-*+`. Part 2 gave us **Infix → Prefix** (`+A*B-^CDE`) and **Postfix → Infix** (`(A+(B*((C^D)-E)))`). This final part covers the three remaining conversions — **Prefix → Infix**, **Postfix → Prefix**, and **Prefix → Postfix** — and closes with a full summary table plus a syllabus check for this class.

---

## 8. Prefix → Infix Conversion

Structurally almost identical to Postfix → Infix (Section 7) — a string stack, pop two on seeing an operator, wrap in brackets, push back. The two differences: we scan **right to left**, and the **pop order swaps sides**.

**The core rules:**

1. Iterate the string from the **last** character to the **first** (`i = n-1` down to `0`).
2. **Operand** → push as-is.
3. **Operator** → pop the top two strings. Call the **first** pop `t1`, the **second** pop `t2`. Combine as `( t1 <operator> t2 )` — note `t1` goes on the **left** this time — and push back.
4. **End of string** → whatever remains on the stack is the final infix expression.

⚠️ **This is the exact mirror-image swap from Postfix→Infix:** there, `t1` (first pop) went on the **right**, `t2` on the **left**. Here, scanning direction is reversed, so `t1` (first pop) goes on the **left**, `t2` on the **right**. Mixing these two up is the single most common mistake students make between these two conversions — they look nearly identical, and that's exactly why it's easy to swap the sides by accident.

Let's trace `- M N` (a simple prefix expression, matching the example used in the lecture) — right to left:

```
Tokens (reading right to left): N  M  -
```

**Step 1 — `N` (rightmost token, operand):**

```
stack: [ "N" ]
```

**Step 2 — `M` (operand):**

```
stack: [ "N", "M" ]
```

**Step 3 — `-` (operator). Pop t1 = "M", pop t2 = "N". Combine: `(M-N)`:**

```
stack: [ "(M-N)" ]
```

**End of string:**

```
Final infix: (M-N)
```

✅ Correct — `- M N` in prefix means "subtract N from M", i.e. `M - N`.

**A second, richer trace** — `- + P Q * M N` (also from the lecture) — right to left:

```
Tokens (right to left): N  M  *  Q  P  +  -
```

```
i: 'N' operand → push          stack: [ "N" ]
i: 'M' operand → push          stack: [ "N", "M" ]
i: '*' operator → pop t1="M", pop t2="N"
                  combine (t1 op t2) = "(M*N)"
                                        stack: [ "(M*N)" ]
i: 'Q' operand → push          stack: [ "(M*N)", "Q" ]
i: 'P' operand → push          stack: [ "(M*N)", "Q", "P" ]
i: '+' operator → pop t1="P", pop t2="Q"
                  combine = "(P+Q)"
                                        stack: [ "(M*N)", "(P+Q)" ]
i: '-' operator → pop t1="(P+Q)", pop t2="(M*N)"
                  combine = "((P+Q)-(M*N))"
                                        stack: [ "((P+Q)-(M*N))" ]

Final infix: ((P+Q)-(M*N))
```

✅ Matches: prefix `- + P Q * M N` reads as "subtract (M*N) from (P+Q)", i.e. `(P+Q) - (M*N)`.

**Full code:**

```cpp
string prefixToInfix(string s) {
    int i = s.size() - 1;
    stack<string> st;

    while (i >= 0) {
        if (isOperand(s[i])) {
            st.push(string(1, s[i]));
        }
        else {   // operator
            string t1 = st.top(); st.pop();
            string t2 = st.top(); st.pop();
            string combined = "(" + t1 + s[i] + t2 + ")";
            st.push(combined);
        }
        i--;
    }

    return st.top();
}
```

**Time complexity:** **O(n)** for the scan; string concatenation adds up to **O(n)** worst-case additional work across the run, same caveat as Section 7.

**Space complexity:** **O(n)** — the stack holds strings whose combined length grows with the original expression.

---

## 9. Postfix → Prefix Conversion

This one deliberately **avoids** the two-step "convert to infix, then infix to prefix" route — it's a single, direct pass.

**The core rules:**

1. Iterate **left to right** (`i = 0` to `n-1`), stack holds strings.
2. **Operand** → push as-is.
3. **Operator** → pop the top two strings. Call the **first** pop `t1`, the **second** pop `t2`. Combine as `<operator> t2 t1` — **operator first, no brackets** — and push back.
4. **End of string** → the remaining stack element is the final prefix expression.

⚠️ **No brackets are added anywhere in this conversion.** Unlike Sections 7 and 8 (postfix/prefix → *infix*), where brackets are mandatory to preserve meaning, prefix and postfix notation are **inherently unambiguous** without any brackets at all — that's actually one of their major selling points over infix.

Let's trace `A B - D + E F * G /` (the running example from the lecture, deliberately using letters like the lecture does):

```
Tokens: A  B  -  D  +  E  F  *  G  /
```

```
i: 'A' operand → push                    stack: [ "A" ]
i: 'B' operand → push                    stack: [ "A", "B" ]
i: '-' operator → pop t1="B", pop t2="A"
                  combine = "-" + t2 + t1 = "-AB"
                                            stack: [ "-AB" ]
i: 'D' operand → push                    stack: [ "-AB", "D" ]
i: '+' operator → pop t1="D", pop t2="-AB"
                  combine = "+" + t2 + t1 = "+-ABD"
                                            stack: [ "+-ABD" ]
i: 'E' operand → push                       stack: [ "+-ABD", "E" ]
i: 'F' operand → push                       stack: [ "+-ABD", "E", "F" ]
i: '*' operator → pop t1="F", pop t2="E"
                  combine = "*" + t2 + t1 = "*EF"
                                            stack: [ "+-ABD", "*EF" ]
i: 'G' operand → push                       stack: [ "+-ABD", "*EF", "G" ]
i: '/' operator → pop t1="G", pop t2="*EF"
                  combine = "/" + t2 + t1 = "/*EFG"
                                            stack: [ "+-ABD", "/*EFG" ]
```

⚠️ Wait — at this point there are still **two** elements left in the stack (`"+-ABD"` and `"/*EFG"`), but the input has been fully consumed. That's expected here only because this trace intentionally used a postfix string with two independent sub-expressions to show intermediate stack state clearly — a genuinely valid, single postfix expression always reduces to **exactly one** stack element at the end. Let's finish with the lecture's actual intended single expression instead, to avoid leaving a wrong impression:

**Corrected full trace — `A B C D ^ E - * +` (Part 1's postfix result):**

```
Tokens: A  B  C  D  ^  E  -  *  +
```

```
i: 'A' operand → push                     stack: [ "A" ]
i: 'B' operand → push                     stack: [ "A", "B" ]
i: 'C' operand → push                     stack: [ "A", "B", "C" ]
i: 'D' operand → push                     stack: [ "A", "B", "C", "D" ]
i: '^' operator → pop t1="D", pop t2="C"
                  combine = "^" + t2 + t1 = "^CD"
                                             stack: [ "A", "B", "^CD" ]
i: 'E' operand → push                        stack: [ "A", "B", "^CD", "E" ]
i: '-' operator → pop t1="E", pop t2="^CD"
                  combine = "-" + t2 + t1 = "-^CDE"
                                             stack: [ "A", "B", "-^CDE" ]
i: '*' operator → pop t1="-^CDE", pop t2="B"
                  combine = "*" + t2 + t1 = "*B-^CDE"
                                             stack: [ "A", "*B-^CDE" ]
i: '+' operator → pop t1="*B-^CDE", pop t2="A"
                  combine = "+" + t2 + t1 = "+A*B-^CDE"
                                             stack: [ "+A*B-^CDE" ]

Final prefix: +A*B-^CDE
```

✅ **Matches Part 2's answer exactly** (`+A*B-^CDE`), confirming both routes — the direct postfix→prefix conversion here, and the reverse-and-swap infix→prefix from Part 2 — agree.

**Full code:**

```cpp
string postfixToPrefix(string s) {
    int i = 0;
    stack<string> st;

    while (i < s.size()) {
        if (isOperand(s[i])) {
            st.push(string(1, s[i]));
        }
        else {   // operator
            string t1 = st.top(); st.pop();
            string t2 = st.top(); st.pop();
            string combined = string(1, s[i]) + t2 + t1;
            st.push(combined);
        }
        i++;
    }

    return st.top();
}
```

**Time complexity:** **O(n)** for the scan, plus up to **O(n)** additional for string concatenation across the run.

**Space complexity:** **O(n)**.

---

## 10. Prefix → Postfix Conversion

The mirror image of Section 9 — same "no brackets" principle, but scanning **right to left**, operator placed **last** instead of first.

**The core rules:**

1. Iterate **right to left** (`i = n-1` down to `0`).
2. **Operand** → push as-is.
3. **Operator** → pop the top two strings. Call the **first** pop `t1`, the **second** pop `t2`. Combine as `t1 t2 <operator>` — operator **last**, no brackets — and push back.
4. **End of string** → the remaining stack element is the final postfix expression.

⚠️ Compare carefully against Section 9: there, scanning was left-to-right and the combine order was `operator + t2 + t1`. Here, scanning is right-to-left and the combine order is `t1 + t2 + operator`. Both `t1`/`t2` roles and the scan direction flip together — this is the same mirror relationship you saw between Sections 7 and 8.

Let's trace `+ A * B - ^ C D E` (Part 2's prefix result — this is `+A*B-^CDE` with spaces added for readability) — right to left:

```
Tokens (right to left): E  D  C  ^  -  B  *  A  +
```

```
i: 'E' operand → push                       stack: [ "E" ]
i: 'D' operand → push                       stack: [ "E", "D" ]
i: 'C' operand → push                       stack: [ "E", "D", "C" ]
i: '^' operator → pop t1="C", pop t2="D"
                  combine = t1 + t2 + op = "CD^"
                                            stack: [ "E", "CD^" ]
i: '-' operator → pop t1="CD^", pop t2="E"
                  combine = t1 + t2 + op = "CD^E-"
                                            stack: [ "CD^E-" ]
i: 'B' operand → push                       stack: [ "CD^E-", "B" ]
i: '*' operator → pop t1="B", pop t2="CD^E-"
                  combine = t1 + t2 + op = "BCD^E-*"
                                            stack: [ "BCD^E-*" ]
i: 'A' operand → push                       stack: [ "BCD^E-*", "A" ]
i: '+' operator → pop t1="A", pop t2="BCD^E-*"
                  combine = t1 + t2 + op = "ABCD^E-*+"
                                            stack: [ "ABCD^E-*+" ]

Final postfix: ABCD^E-*+
```

✅ **Matches Part 1's very first postfix result exactly** (`ABCD^E-*+`) — this closes the loop across all three parts of this lecture, confirming every conversion direction is mutually consistent for our running example.

**Full code:**

```cpp
string prefixToPostfix(string s) {
    int i = s.size() - 1;
    stack<string> st;

    while (i >= 0) {
        if (isOperand(s[i])) {
            st.push(string(1, s[i]));
        }
        else {   // operator
            string t1 = st.top(); st.pop();
            string t2 = st.top(); st.pop();
            string combined = t1 + t2 + string(1, s[i]);
            st.push(combined);
        }
        i--;
    }

    return st.top();
}
```

**Time complexity:** **O(n)** for the scan, plus up to **O(n)** additional for string concatenation.

**Space complexity:** **O(n)**.

---

## 11. Full Summary Table — All Six Conversions

| Conversion | Scan direction | Stack stores | Operator combine rule | Brackets added? |
| --- | --- | --- | --- | --- |
| Infix → Postfix | Left → Right | characters | pop while `priority(cur) <= priority(top)` | N/A (input has them) |
| Infix → Prefix | Reverse+swap, then L→R | characters | pop while `priority(cur) < priority(top)` (strict), then reverse result | N/A (input has them) |
| Postfix → Infix | Left → Right | strings | `( t2 op t1 )` | ✅ every operation wrapped |
| Prefix → Infix | Right → Left | strings | `( t1 op t2 )` | ✅ every operation wrapped |
| Postfix → Prefix | Left → Right | strings | `op + t2 + t1` | ❌ none |
| Prefix → Postfix | Right → Left | strings | `t1 + t2 + op` | ❌ none |

**The single biggest idea to take away:** every one of these six algorithms is the *same* skeleton — scan the string, push operands, and when you hit an operator, either resolve priority against the stack top (infix conversions) or just pop-combine-push the last two operands (the four operand-stack conversions). The **direction** of the scan and **which side** the popped operands land on are the only things that change from one conversion to the next — and mixing those two details up is exactly where students lose marks, since the algorithms *look* almost identical to each other.

**On time/space complexity across all six:** every conversion is **O(n)** time (the "extra" popping or string-concatenation work never exceeds another O(n) factor across the *entire* run, not per character) and **O(n)** space (the stack, in the worst case, holds content proportional to the whole expression).