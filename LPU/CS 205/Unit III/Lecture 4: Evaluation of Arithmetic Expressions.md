# Lecture 4: Evaluation of Arithmetic Expressions

## Quick Recap Before We Begin

Lecture 3 was entirely about **transformation** — converting an expression from one notation to another (infix, prefix, postfix), without ever computing an actual number. Today we do the opposite: given an expression, **compute its numeric value**. This closes out the *"Arithmetic expressions"* line item in the syllabus (Polish notation, evaluation *and* transformation — transformation is done, evaluation is today).

We'll cover, in order: **evaluating postfix** (the easiest and most common), **evaluating prefix** (its mirror image), and **evaluating infix directly** (the hardest — this is where priority order and brackets both have to be handled live, during evaluation itself, not as a separate conversion step first).

---

## 1. Why Postfix/Prefix Are "Calculator-Friendly" — The Core Idea

Before any code: the entire reason postfix and prefix are used in real calculators (Section 4 of Class 3) is that **evaluating them never requires knowing operator priority or brackets at evaluation time** — by the time an expression is in postfix or prefix form, all the priority/bracket decisions have *already* been baked into the token order. You just need a stack and one rule: "when you see an operator, it always applies to the two operands that were most recently seen."

This is fundamentally different from infix evaluation, where the *meaning* of what you've seen so far can change depending on what comes later (e.g., seeing `2 + 3` doesn't tell you whether to add yet — a `*` might show up next and change everything). That's precisely why infix evaluation (Section 4) needs a more elaborate algorithm.

---

## 2. Evaluating a Postfix Expression

**The setup:**

- `i` — index into the postfix string/token list, starts at `0`
- `st` — an empty stack, this time storing **numbers** (not characters/strings)

**The core rule:**

1. **Operand** → push its numeric value onto the stack.
2. **Operator** → pop the top two numbers. The **first** pop is `t1`, the **second** pop is `t2`. Compute `t2 <operator> t1` (note the order — `t2` first, matching the order operands originally appeared in), and push the result back.
3. **End of string** → the single value remaining on the stack is the answer.

⚠️ **Operand order is not symmetric for `-` and `/`.** This is the single most common mistake in postfix evaluation. `t1` is the operand that appeared **later** in the string (popped first, since the stack is LIFO), so it must go on the **right** of the operator: `t2 - t1`, `t2 / t1` — never `t1 - t2`.

Let's evaluate `5 1 2 + 4 * + 3 -` (a classic worked example — this should equal `5 + ((1+2)*4) - 3 = 5 + 12 - 3 = 14`):

```
Tokens: 5  1  2  +  4  *  +  3  -
```

```
i: '5' operand → push                   stack: [ 5 ]
i: '1' operand → push                   stack: [ 5, 1 ]
i: '2' operand → push                   stack: [ 5, 1, 2 ]
i: '+' operator → pop t1=2, pop t2=1
                  compute t2+t1 = 1+2 = 3
                                          stack: [ 5, 3 ]
i: '4' operand → push                   stack: [ 5, 3, 4 ]
i: '*' operator → pop t1=4, pop t2=3
                  compute t2*t1 = 3*4 = 12
                                          stack: [ 5, 12 ]
i: '+' operator → pop t1=12, pop t2=5
                  compute t2+t1 = 5+12 = 17
                                          stack: [ 17 ]
i: '3' operand → push                   stack: [ 17, 3 ]
i: '-' operator → pop t1=3, pop t2=17
                  compute t2-t1 = 17-3 = 14
                                          stack: [ 14 ]

Final result: 14  ✅ matches hand-computed 5 + (1+2)*4 - 3 = 14
```

**Full code:**

```cpp
int evaluatePostfix(string s) {
    stack<int> st;
    int i = 0;

    while (i < s.size()) {
        if (isdigit(s[i])) {
            // handle multi-digit numbers (see Section 3 note below)
            int num = 0;
            while (i < s.size() && isdigit(s[i])) {
                num = num * 10 + (s[i] - '0');
                i++;
            }
            st.push(num);
            continue;   // skip the i++ at the bottom, already advanced
        }
        else {   // operator
            int t1 = st.top(); st.pop();
            int t2 = st.top(); st.pop();
            int result;
            if (s[i] == '+') result = t2 + t1;
            else if (s[i] == '-') result = t2 - t1;
            else if (s[i] == '*') result = t2 * t1;
            else result = t2 / t1;   // '/'
            st.push(result);
        }
        i++;
    }

    return st.top();
}
```

**Time complexity:** **O(n)** — single pass, constant work per token.

**Space complexity:** **O(n)** — the stack can hold up to roughly `n/2` operands in the worst case (e.g., an expression that's mostly operands with one final operator).

---

## 3. Handling *Multi-Digit Numbers* — A Detail That Trips Students in Exams

Every trace so far used single-digit operands (`A`, `5`, `2`) for clarity, but real inputs often have multi-digit numbers like `12`, `100`. If you check `isdigit(s[i])` one character at a time and push each digit separately, `"12"` would wrongly become two separate operands `1` and `2` instead of one operand `12`.

**The fix**, shown in the code above: when you detect a digit, run an **inner while loop** that keeps consuming digits and builds up the full number (`num = num * 10 + digit`) before pushing it as a single value. This is the exact same digit-accumulation pattern used anywhere you parse a number out of a string.

```
Example: postfix = "12 3 +" (space-separated for readability; tokens: "12", "3", "+")

i=0: sees '1', digit-loop consumes '1' then '2' → num = 1*10+2 = 12, push 12
     stack: [ 12 ]
i=2: (skip space) sees '3', digit-loop consumes just '3' → push 3
     stack: [ 12, 3 ]
i=4: sees '+' → pop t1=3, pop t2=12 → 12+3=15 → push 15
     stack: [ 15 ]

Result: 15
```

⚠️ **If tokens are space-separated** (common in exam problems written as `"12 3 +"` rather than `"123+"`), you'd typically split the string by spaces first (`stringstream` in C++, or `split()` conceptually) rather than scanning character-by-character — mentioning this distinction to an interviewer/examiner shows you understand *why* the digit-accumulation trick exists in the first place.

---

## 4. Evaluating a Prefix Expression

The mirror image of Section 2 — same idea, but scan **right to left**, and the **operand order flips** for the same reason it flipped in prefix-to-infix/postfix conversions (Class 3, Sections 8 & 10).

**The core rule:**

1. Iterate **right to left**.
2. **Operand** → push its numeric value.
3. **Operator** → pop the top two numbers. First pop is `t1`, second pop is `t2`. Compute `t1 <operator> t2` — **note**: `t1` now goes on the **left** — and push the result.
4. **End of string** → the remaining value is the answer.

Let's evaluate `- + 5 * 1 2 4` — wait, let's use a cleaner, standard example: `+ 5 * 4 - 2 3` (this should equal `5 + (4 * (2-3)) = 5 + 4*(-1) = 5 - 4 = 1`):

```
Tokens (right to left): 3  2  -  4  *  5  +
```

```
i: '3' operand → push                    stack: [ 3 ]
i: '2' operand → push                    stack: [ 3, 2 ]
i: '-' operator → pop t1=2, pop t2=3
                  compute t1-t2 = 2-3 = -1
                                           stack: [ -1 ]
i: '4' operand → push                    stack: [ -1, 4 ]
i: '*' operator → pop t1=4, pop t2=-1
                  compute t1*t2 = 4*(-1) = -4
                                           stack: [ -4 ]
i: '5' operand → push                    stack: [ -4, 5 ]
i: '+' operator → pop t1=5, pop t2=-4
                  compute t1+t2 = 5+(-4) = 1
                                           stack: [ 1 ]

Final result: 1  ✅ matches hand-computed 5 + 4*(2-3) = 1
```

**Full code:**

```cpp
int evaluatePrefix(string s) {
    stack<int> st;
    int i = s.size() - 1;

    while (i >= 0) {
        if (isdigit(s[i])) {
            // NOTE: multi-digit numbers when scanning right-to-left need
            // extra care — you'd typically pre-split into tokens rather
            // than accumulate digit-by-digit backward, since building a
            // number backward (units digit first) requires tracking place
            // value, unlike the simple left-to-right case in Section 3.
            int num = s[i] - '0';
            st.push(num);
        }
        else {   // operator
            int t1 = st.top(); st.pop();
            int t2 = st.top(); st.pop();
            int result;
            if (s[i] == '+') result = t1 + t2;
            else if (s[i] == '-') result = t1 - t2;
            else if (s[i] == '*') result = t1 * t2;
            else result = t1 / t2;   // '/'
            st.push(result);
        }
        i--;
    }

    return st.top();
}
```

⚠️ **Compare this directly against `evaluatePostfix`:** the only real differences are the scan direction and which popped value (`t1` vs `t2`) sits on which side of the operator. Every conversion and evaluation algorithm across Classes 3–4 shares this exact same skeleton — recognizing that pattern is what actually makes this topic fast to solve in an exam, rather than needing six memorized algorithms.

**Time complexity:** **O(n)**.

**Space complexity:** **O(n)**.

---

## 5. Evaluating an Infix Expression Directly — The Hard Case

This is the one genuinely new algorithm in this class. Unlike postfix/prefix, an infix expression's structure isn't "pre-resolved" — you have to track **both** the numbers seen so far **and** the operators seen so far, and decide *live* when it's safe to actually apply an operator (based on priority and brackets), without first converting to another notation.

**The setup — two stacks running in parallel:**

- `values` — a stack of numbers
- `ops` — a stack of operator characters (including brackets)

**The core rules, scanning left to right:**

1. **Operand** → push its numeric value onto `values` (handle multi-digit numbers as in Section 3).
2. **`(`** → push onto `ops`, no questions asked.
3. **`)`** → keep applying (see rule 5's "apply" step below) until you pop a matching `(` off `ops`.
4. **Operator** → while `ops` is non-empty, the top of `ops` isn't `(`, **and** the top of `ops` has priority **≥** the current operator's priority, **apply** the top operator first. Then push the current operator onto `ops`.
5. **"Apply"** (used by rules 3 and 4): pop the operator off `ops`; pop two values off `values` — first pop `v1`, second pop `v2`; compute `v2 <operator> v1` (same left/right convention as postfix evaluation — makes sense, since this is essentially running postfix evaluation *interleaved* with the scan); push the result onto `values`.
6. **End of string** → apply any operators still remaining on `ops`. The single value remaining on `values` is the answer.

**Real-life analogy:** Think of `values` as a running scratchpad of partial results, and `ops` as a to-do list of pending operations, ordered by "who has to happen first." Every time a new operator shows up, you first check: "does anything already on my to-do list have equal-or-higher priority and need to happen before this new one?" If yes, clear that off the to-do list (apply it) before adding the new one.

Let's evaluate `5 + 2 * 3 - 8 / 4` (should equal `5 + 6 - 2 = 9`):

```
Tokens: 5  +  2  *  3  -  8  /  4
```

```
i: '5' operand → push                     values: [ 5 ]              ops: [ ]

i: '+' operator. ops is empty → just push
                                            values: [ 5 ]              ops: [ + ]

i: '2' operand → push                     values: [ 5, 2 ]            ops: [ + ]

i: '*' operator. Top of ops = '+' (priority 1).
     priority('+') >= priority('*')?  1 >= 2?  NO → don't apply, push '*'
                                            values: [ 5, 2 ]            ops: [ +, * ]

i: '3' operand → push                     values: [ 5, 2, 3 ]          ops: [ +, * ]

i: '-' operator. Top of ops = '*' (priority 2).
     priority('*') >= priority('-')?  2 >= 1?  YES → APPLY '*':
        pop v1=3, pop v2=2 → 2*3=6 → push 6
                                            values: [ 5, 6 ]            ops: [ + ]
     Check new top: '+' (priority 1).
     priority('+') >= priority('-')?  1 >= 1?  YES → APPLY '+':
        pop v1=6, pop v2=5 → 5+6=11 → push 11
                                            values: [ 11 ]              ops: [ ]
     ops now empty → push '-'
                                            values: [ 11 ]              ops: [ - ]

i: '8' operand → push                     values: [ 11, 8 ]            ops: [ - ]

i: '/' operator. Top of ops = '-' (priority 1).
     priority('-') >= priority('/')?  1 >= 2?  NO → don't apply, push '/'
                                            values: [ 11, 8 ]            ops: [ -, / ]

i: '4' operand → push                     values: [ 11, 8, 4 ]         ops: [ -, / ]

End of string — apply everything remaining:
     APPLY '/': pop v1=4, pop v2=8 → 8/4=2 → push 2
                                            values: [ 11, 2 ]            ops: [ - ]
     APPLY '-': pop v1=2, pop v2=11 → 11-2=9 → push 9
                                            values: [ 9 ]                ops: [ ]

Final result: 9  ✅ matches hand-computed 5 + 2*3 - 8/4 = 5+6-2 = 9
```

**Now let's see brackets in action** — 

evaluate `(5 + 2) * (3 - 8 / 4)` (should equal `7 * (3-2) = 7`):

```
Tokens: (  5  +  2  )  *  (  3  -  8  /  4  )
```

```
i: '(' → push onto ops                    values: [ ]              ops: [ ( ]
i: '5' operand → push                     values: [ 5 ]            ops: [ ( ]
i: '+' operator. Top='(' (priority -1).
     -1 >= 1?  NO → push '+'
                                            values: [ 5 ]                ops: [ (, + ]
i: '2' operand → push                     values: [ 5, 2 ]              ops: [ (, + ]

i: ')' → keep applying until we pop '(':
     APPLY '+': pop v1=2, pop v2=5 → 5+2=7 → push 7
                                            values: [ 7 ]                ops: [ ( ]
     pop '(' → discard, stop
                                            values: [ 7 ]                ops: [ ]

i: '*' operator. ops empty → push '*'
                                            values: [ 7 ]                ops: [ * ]

i: '(' → push onto ops                    values: [ 7 ]                ops: [ *, ( ]
i: '3' operand → push                     values: [ 7, 3 ]              ops: [ *, ( ]
i: '-' operator. Top='(' (priority -1).
     -1 >= 1?  NO → push '-'
                                            values: [ 7, 3 ]            ops: [ *, (, - ]
i: '8' operand → push                     values: [ 7, 3, 8 ]           ops: [ *, (, - ]
i: '/' operator. Top='-' (priority 1).
     1 >= 2?  NO → push '/'
                                            values: [ 7, 3, 8 ]      ops: [ *, (, -, / ]
i: '4' operand → push                     values: [ 7, 3, 8, 4 ]     ops: [ *, (, -, / ]

i: ')' → keep applying until we pop '(':
     APPLY '/': pop v1=4, pop v2=8 → 8/4=2 → push 2
                                            values: [ 7, 3, 2 ]        ops: [ *, (, - ]
     APPLY '-': pop v1=2, pop v2=3 → 3-2=1 → push 1
                                            values: [ 7, 1 ]              ops: [ *, ( ]
     pop '(' → discard, stop
                                            values: [ 7, 1 ]              ops: [ * ]

End of string — apply everything remaining:
     APPLY '*': pop v1=1, pop v2=7 → 7*1=7 → push 7
                                            values: [ 7 ]                 ops: [ ]

Final result: 7  ✅ matches hand-computed (5+2) * (3-8/4) = 7*1 = 7
```

**Full code:**

```cpp
int applyOp(int v2, int v1, char op) {
    if (op == '+') return v2 + v1;
    if (op == '-') return v2 - v1;
    if (op == '*') return v2 * v1;
    return v2 / v1;   // '/'
}

int evaluateInfix(string s) {
    stack<int> values;
    stack<char> ops;
    int i = 0;

    while (i < s.size()) {
        if (s[i] == ' ') { i++; continue; }   // skip spaces if present

        if (isdigit(s[i])) {
            int num = 0;
            while (i < s.size() && isdigit(s[i])) {
                num = num * 10 + (s[i] - '0');
                i++;
            }
            values.push(num);
            continue;
        }
        else if (s[i] == '(') {
            ops.push(s[i]);
        }
        else if (s[i] == ')') {
            while (ops.top() != '(') {
                int v1 = values.top(); values.pop();
                int v2 = values.top(); values.pop();
                char op = ops.top(); ops.pop();
                values.push(applyOp(v2, v1, op));
            }
            ops.pop();   // discard the '('
        }
        else {   // operator
            while (!ops.empty() && ops.top() != '(' &&
                   priority(ops.top()) >= priority(s[i])) {
                int v1 = values.top(); values.pop();
                int v2 = values.top(); values.pop();
                char op = ops.top(); ops.pop();
                values.push(applyOp(v2, v1, op));
            }
            ops.push(s[i]);
        }
        i++;
    }

    while (!ops.empty()) {
        int v1 = values.top(); values.pop();
        int v2 = values.top(); values.pop();
        char op = ops.top(); ops.pop();
        values.push(applyOp(v2, v1, op));
    }

    return values.top();
}
```

⚠️ **This is structurally identical to Infix→Postfix conversion (Lecture 3, Section 5)** — same scanning logic, same priority-comparison condition (`>=`), same bracket handling. The *only* difference is that instead of appending the popped operator to an `ans` string, we immediately **apply** it to two popped numbers using a second stack. If you understand infix-to-postfix conversion cold, this algorithm is not new — it's that algorithm with one line changed.

**Time complexity:** **O(n)** — same reasoning as before: across the entire run, no operator is pushed/applied more than once, so total work stays linear.

**Space complexity:** **O(n)** — two stacks now instead of one, but both are still bounded by the expression length, so still O(n) overall (not O(2n) in any meaningful sense — constants don't matter in Big-O).

---

## 6. Common Exam Pitfalls — A Checklist

| Pitfall | Where it bites | The fix |
| --- | --- | --- |
| Swapping `t1`/`t2` order for `-` and `/` | Postfix eval (Section 2), Prefix eval (Section 4) | Postfix: `t2 - t1`. Prefix: `t1 - t2`. They're opposite — don't default to the same rule for both. |
| Forgetting multi-digit number handling | Any evaluation with numbers > 9 | Always accumulate digits in an inner loop before pushing (Section 3). |
| Using `>` instead of `>=` in infix priority checks | Infix evaluation (Section 5), Infix→Postfix (Class 3) | Same-priority operators (`5 - 3 + 2`) must resolve left-to-right — `>=` (not `>`) guarantees this. |
| Division by zero / integer division truncation | Any evaluation involving `/` | Always worth a one-line mention to the examiner/interviewer, even if the problem doesn't explicitly test it — shows completeness. |
| Not discarding `(` after popping it in the `)` case | Postfix conversion (Class 3) and infix evaluation (Section 5) | The `(` itself never gets applied or appended anywhere — it's purely a boundary marker. |
| Assuming a stack of exactly one operator/operand type when it should hold pairs (values vs. ops) | Infix evaluation (Section 5) | Infix evaluation is the *only* algorithm in this whole unit needing **two** stacks simultaneously — every other evaluation/conversion needs just one. |

---

## 7. Summary — Evaluation vs. Transformation (Lecture 3 & 4 Together)

|  | Class 3 (Transformation) | Class 4 (Evaluation) |
| --- | --- | --- |
| Goal | Rewrite expression in a different notation | Compute a single numeric answer |
| Stack stores | characters or strings | numbers |
| Infix handling | Needs one stack (operators only) | Needs **two** stacks (values + operators) |
| Postfix/Prefix handling | One stack, builds combined strings | One stack, computes actual results |
| Core skeleton | Same across all 6 conversions | Same across all 3 evaluations |

**The single biggest idea across both classes:** whether you're transforming or evaluating, postfix and prefix are always the "easy" cases (single stack, single linear pass, no live priority juggling), while infix is always the "hard" case (needs priority comparison live, because meaning isn't resolved until later tokens are seen). This is exactly *why* real stack-based calculators and compilers convert infix to postfix/prefix internally before evaluating, rather than evaluating infix directly, in most real systems — though, as shown in Section 5, direct infix evaluation is entirely possible and is a fair thing to be asked to implement in an exam.