# Lecture 10: Characters & Strings in C++

Status: Done

# Part 1 — Characters & C-Style Strings

---

## 1. What Is a Character?

> Characters are single entities enclosed inside single quotes (single inverted commas). Whatever you put inside single quotes — alphabets, digits, or special symbols — becomes a character.
> 

```cpp
'A'   // a character
'5'   // also a character (even though it looks like a digit)
'#'   // also a character
```

> A character is a single symbol, letter, or digit enclosed in single quotes. A character variable stores **one byte** and internally holds an **ASCII value**.
> 

This matches exactly what we saw back in Lecture 2: `char` takes **1 byte** of memory, compared to `int`'s 4 bytes.

> If you combine multiple characters together — like `'S', 'a', 'c', 'h', 'i', 'n'` — you get a **word**. Each individual letter is a character; putting them together makes a string (which we'll get to shortly).
> 

---

## 2. How Characters Are Stored — ASCII Values Revisited

Every character has a corresponding **numeric ASCII value** — this is exactly the concept we first saw in Lecture 2 (and used again for character-based patterns in Lecture 9).

| Character | ASCII Value |
| --- | --- |
| `'A'` | 65 |
| `'B'` | 66 |
| `'a'` | 97 *(different from 'A' — case-sensitive!)* |
| `'0'` | 48 |
| `' '` (space) | 32 |

> Why keep a numeric value behind every character? Because storing a *number* in memory (as binary) is straightforward — you can't directly "store a letter," but you can store the binary representation of its ASCII number.
> 

### Declaring a Character & Finding Its ASCII Value

```cpp
char ch = 'A';
cout << "Character is " << ch << endl;        // A
cout << "ASCII value is " << (int)ch << endl;  // 65
```

> To get the ASCII value, you **typecast** the character into an `int` — exactly the explicit type casting concept from Lecture 2. You don't need to memorize ASCII values; typecasting gets you the number directly.
> 

---

## 3. Built-In Character Functions (from `<cctype>`)

> These come from the `<cctype>` header and are used to **test or modify** individual characters.
> 

| Function | What It Checks/Does |
| --- | --- |
| `isalpha(ch)` | Is it a letter? (returns `1`/true or `0`/false) |
| `isdigit(ch)` | Is it a digit? |
| `isalnum(ch)` | Is it a letter **or** a digit? |
| `islower(ch)` | Is it lowercase? |
| `isupper(ch)` | Is it uppercase? |
| `isspace(ch)` | Is it a whitespace character? |
| `tolower(ch)` | Converts to lowercase |
| `toupper(ch)` | Converts to uppercase |

> All the `is...` functions return **Boolean-style values** (`1` for true, `0` for false) — the exact same `1`/`0` Boolean representation from relational operators back in Lecture 4.
> 

### Worked Example

```cpp
#include <cctype>

char ch = 'A';
cout << isalpha(ch);   // 1 (true — it IS a letter)
cout << tolower(ch);   // a
```

---

## 4. What Is a String?

> A string is a **set of characters**. When you combine multiple characters together, they form a string.
> 

```
'S' + 'a' + 'c' + 'h' + 'i' + 'n'  →  "Sachin"
```

> Just as characters are wrapped in **single quotes**, strings are wrapped in **double quotes**.
> 

```cpp
"Sachin"              // a string
"Sachin Bhardwaj"     // also a string — spaces count as characters too!
"LearnYard is best"   // even a full sentence is a string
```

> Important detail: **whitespace counts as a character** inside a string. `"Sachin Bhardwaj"` has 16 characters total — 15 letters plus 1 space.
> 

---

## 5. Two Ways to Represent Strings in C++

> There are **two types** of strings in C++: **C-style strings** (character strings) and the **string class** (also called C++ style strings, or STL strings).
> 

```
┌─────────────────────────────────────────────────────┐
│                    STRINGS                          │
├───────────────────────┬─────────────────────────────┤
│  C-Style Strings      │   String Class (C++)        │
│  (character arrays)   │   (from STL, next lecture)  │
└───────────────────────┴─────────────────────────────┘
```

Today's lecture focuses entirely on **C-style strings**. The **string class** gets its own full lecture next (Part 2 of these notes).

---

## 6. C-Style Strings — Character Arrays

### The Core Idea

We already know from Lecture 11: if you need to store many integers, you use an **array**. The same logic applies here — if you need to store many *characters* (i.e., a string), you use a **character array**.

> A C-style string is a **one-dimensional array of characters**, terminated by a **null character**.
> 

### Declaration

```cpp
char name[10];   // a character array that can hold a string
```

### The Null Character — What Makes String Arrays Different from Normal Arrays

This is the **key difference** between a character array and a regular numeric array:

> Every character array has an **extra final block** containing the **null character** (`'\0'`), which marks exactly where the string terminates.
> 

```cpp
char name[] = "Sachin";
```

```
Index:     0    1    2    3    4    5    6
Value:    'S'  'a'  'c'  'h'  'i'  'n'  '\0'
                                          ↑
                                   null character
                              (marks the END of the string)
```

> If you find the **length** of this string, it's **6**, not 7 — length calculations **exclude** the null character. It's there purely as an internal marker, not counted as part of the actual content.
> 

---

## 7. Two Ways to Declare & Initialize a C-Style String

### a) Using a String Literal (Compiler Adds `\0` Automatically)

```cpp
char name[] = "Sachin";
```

> When you write the string directly inside double quotes like this, the compiler **automatically appends** the null character at the end — you don't have to do it yourself.
> 

### b) Character-Wise Initialization (You Must Add `\0` Manually)

```cpp
char name[] = {'S', 'a', 'c', 'h', 'i', 'n', '\0'};
```

> Here, you're building the array character-by-character (just like a normal array), so **you** must explicitly include `'\0'` as the last element — the compiler won't add it for you in this style.
> 

> If you forget the null character in this manual style, you'll often see **garbage values** printed after your actual string — because there's no marker telling the program where the string content actually ends.
> 

---

## 8. Accessing and Modifying C-Style Strings

Since a C-style string is fundamentally just a character array, it supports **index-based access**, exactly like any other array (Lecture 11).

```cpp
char str[] = "LearnYard";
cout << str[2];   // 'a' — index 2 in "LearnYard"
```

### Modifying a Character

```cpp
char str[] = "Hello";
str[0] = 'M';
cout << str;   // "Mello"
```

---

## 9. Taking Input Into a C-Style String

### The Problem With Plain `cin`

```cpp
char str[100];
cout << "Enter your name: ";
cin >> str;
```

If the user types `"Sachin Bhardwaj"`, only `"Sachin"` gets stored!

> `cin`, when used normally, **terminates at the first whitespace**. It reads character-by-character, and as soon as it hits a space, it stops — the rest of the input (`"Bhardwaj"`) is never stored.
> 

### The Fix: `cin.getline()`

```cpp
char str[100];
cin.getline(str, 100);
```

> `cin.getline()` takes **two arguments**: the character array to store into, and the maximum size to read. Unlike plain `cin`, it reads the **entire line, including whitespace**, and stops only at the newline.
> 

### Side-by-Side Comparison

|  | `cin >> str` | `cin.getline(str, size)` |
| --- | --- | --- |
| Stops at | First whitespace | End of the line (newline) |
| Captures multi-word input? | No | Yes |

---

## 10. Common C-String Functions (from `<cstring>`)

> Just like we needed `<iostream>` for `cin`/`cout`, working with C-style string functions requires including the **`<cstring>`** header.
> 

| Function | What It Does |
| --- | --- |
| `strlen(str)` | Returns the string's length (**excluding** the null character) |
| `strcpy(dest, src)` | Copies `src` into `dest` |
| `strcat(dest, src)` | Concatenates (merges) `src` onto the end of `dest` |
| `strcmp(str1, str2)` | Compares two strings character by character |
| `strrev(str)` | Reverses a string *(works in C, often unreliable in C++ compilers)* |

### Worked Examples

**`strlen` — Finding Length**

```cpp
char s[] = "Hello";
cout << strlen(s);   // 5
```

**`strcpy` — Copying One String Into Another**

```cpp
char s1[20], s2[] = "C++";
strcpy(s1, s2);   // s1 now contains "C++"
```

> Syntax order matters: **destination first, source second** — `strcpy(where_to_copy_TO, where_to_copy_FROM)`.
> 

**`strcat` — Concatenation**

```cpp
char s1[20] = "Hello ";
char s2[] = "World";
strcat(s1, s2);   // s1 becomes "Hello World"
```

> Notice `s2` gets merged **into** `s1` — `s1` is both the destination and grows to include `s2`'s content.
> 

**`strcmp` — Comparing Two Strings**

```cpp
char a[] = "abc";
char b[] = "abc";
cout << strcmp(a, b);   // 0
```

> `strcmp` compares strings **character by character**: if the two strings are equal, it returns `0`. If the first string is "smaller" (comes earlier alphabetically, based on ASCII values), it returns a **negative** number. If the second string is smaller, it returns a **positive** number.
> 

---

## Key Points to Remember

- A **character** is a single symbol in single quotes, occupying **1 byte**, and internally stored via its **ASCII value** (typecast to `int` to retrieve it, e.g., `(int)ch`).
- Built-in `<cctype>` functions like `isalpha`, `isdigit`, `islower`, `isupper`, `tolower`, and `toupper` let you test and convert individual characters.
- A **string** is a sequence of characters wrapped in **double quotes** — and whitespace **counts** as a character within a string.
- C++ has two string representations: **C-style strings** (character arrays, this lecture) and the **string class** (STL, next lecture).
- A **C-style string** is a character array terminated by a **null character (`'\0'`)** — this is what distinguishes it structurally from a normal numeric array.
- String literals (`char name[] = "Sachin";`) get the null character added **automatically**; character-wise initialization (`{'S','a',...,'\0'}`) requires you to add it **manually**.
- Plain `cin >> str` stops reading at the **first whitespace**; use **`cin.getline(str, size)`** to capture a full line including spaces.
- C-string functions (`strlen`, `strcpy`, `strcat`, `strcmp`, `strrev`) live in the **`<cstring>`** header and let you measure, copy, merge, and compare character arrays.

# 🎯Part 2 — The C++ String Class

---

## 1. What Is the String Class?

> If asked "What are C++ style strings?" or "What is the string class?" or "What are STL strings?" — all three questions mean the same thing.
> 

We just saw that C-style strings are really character arrays with a null character stuck on the end. C++ gives us a much cleaner alternative:

> Just as `int` stores integers and `char` stores single characters, the C++ STL (Standard Template Library) provides a built-in data type called **`string`**.
> 

```cpp
#include <string>
using namespace std;

string str;   // creates a string variable, just like "int x;"
```

### Is `string` a Data Type, a Function, or Something Else?

> `string` is neither a plain data type nor a function — it is implemented as a **class** inside the STL.
> 

> We'll properly understand what a "class" means once we reach Object-Oriented Programming. For now, just think of it this way: just as `int` is defined internally to allocate 4 bytes and handle numeric values, `string` is a class defined to handle textual data — and **internally, it still stores things using arrays**, but you never have to manage that yourself.
> 

> A string is a sequence of characters represented using the `std::string` class, from the Standard Template Library. It provides **dynamic sizing**, built-in functions for manipulation, and easy concatenation and comparison.
> 

### Why `std::string`?

> Because `string` is a **standard class** living inside the `std` namespace (remember Lecture 1!) — you either write `std::string`, or you write `using namespace std;` once at the top, exactly as we've been doing all along.
> 

---

## 2. The Big Advantage: Dynamic Sizing

This is the string class's headline improvement over C-style strings:

> A C-style character array has a **fixed size** — if you declare `char str[5]`, you can never fit a longer string into it. The C++ `string` class removes this limitation entirely.
> 

```cpp
string name = "Sachin";
name = "LearnYard";   // perfectly fine — the string resizes itself automatically
```

> This is called **dynamic sizing** — the string automatically adjusts its size as needed, unlike a fixed-size character array.
> 

---

## 3. Declaring and Initializing a String

```cpp
string s;                  // declaration only, empty for now
string s1 = "Hello";       // declare + initialize
string s2("World");         // alternate initialization syntax
string s3(5, 'A');          // repeats 'A' five times → "AAAAA"
```

---

## 4. Taking Input Into a String

### The Same Whitespace Problem as C-Style Strings

```cpp
string name;
cin >> name;
```

If the user enters `"Sachin Dwivedi"`, only `"Sachin"` gets stored.

> Exactly the same issue as with character arrays: the normal `cin` function **terminates at whitespace**. It doesn't matter that we're now using the string class instead of a character array — the underlying behavior of `cin` itself hasn't changed.
> 

### The Fix: `getline()`

```cpp
string name;
getline(cin, name);
```

> Notice the syntax difference from character arrays. There, we wrote `cin.getline(str, size)` — a *member function* of `cin`, needing a size limit. Here, we simply write `getline(cin, name)` — a standalone function, with **no size argument needed**, since `string` handles its own sizing dynamically.
> 

### A Common Gotcha: `cin` Followed by `getline`

> If you use `cin >>` to read something, and then immediately try to `getline()` afterward, the leftover newline character from the first input can interfere. The fix is to call `cin.ignore();` right before the `getline()` call, to clear that leftover character from the input buffer.
> 

```cpp
cin.ignore();          // clears the buffer
getline(cin, name);    // now reads properly
```

---

## 5. Accessing and Modifying Characters in a String

### Accessing

```cpp
string s = "C++";
cout << s[0];   // 'C'
```

> Internally, `s` is still stored like an array: `s[0] = 'C'`, `s[1] = '+'`, `s[2] = '+'`. But there's a huge convenience bonus here.
> 

**The Big Plus Point:**

```cpp
cout << s;   // prints "C++" directly!
```

> With a normal numeric array, `cout << array_name` would just print a memory address — not the actual contents. But with the `string` class, `cout` directly prints the string's actual content. You don't need to loop through it just to display it.
> 

### Using `.at()` as an Alternative to `[]`

```cpp
cout << s.at(1);   // '+'
```

### Modifying a Character

```cpp
string s = "Ball";
s[0] = 'C';
cout << s;   // "Call"
```

---

## 6. Iterating Over a String

### Using a Standard `for` Loop

```cpp
string text = "Hello Coders";

for (int i = 0; i < text.length(); i++) {
    cout << text[i] << endl;
}
```

> `.length()` is a built-in function that returns the total number of characters in the string — this replaces the role that `sizeof` played for regular arrays back in Lecture 11.
> 

### Using a Range-Based `for` Loop

```cpp
for (char c : text) {
    cout << c << " ";
}
```

> Exactly the same range-based loop concept from Lecture 11's arrays — here, the iterator `c` directly represents each character's *value*, without needing an index counter at all.
> 

---

## 7. Concatenation — Joining Strings Together

C-style strings needed the clunky `strcat()` function. The string class makes this dramatically simpler.

### Using the `+` Operator

```cpp
string first = "Learn";
string second = "Yard";
string result = first + second;   // "LearnYard"
```

> You can literally **add** two strings together with `+`, just like you'd add two numbers.
> 

### Using `.append()`

```cpp
string a = "Hello";
a.append(" World");
cout << a;   // "Hello World"
```

---

## 8. The Tricky Relationship Between Numbers and Strings

This is an important conceptual gotcha:

```cpp
int x = 10, y = 20;
int z = x + y;      // z = 30  (mathematical addition)

string x2 = "10", y2 = "20";
string z2 = x2 + y2;   // z2 = "1020" (string CONCATENATION, not math!)
```

> When `x` and `y` are strings, `"10"` and `"20"` are treated as a **sequence of characters**, not numeric values. So `x2 + y2` doesn't add them mathematically — it just **joins the two character sequences together**, giving `"1020"` instead of `30`.
> 

### Converting a String Back to a Number: `stoi()`

If you actually need the string's numeric value, use `stoi` ("string to integer"):

```cpp
string str1 = "123";
int x = stoi(str1);
cout << x + 10;   // 133 (now it's real math, since x is an int)
```

> Without `stoi`, doing `str1 + "10"` would give you the concatenated string `"12310"`, not `133`. `stoi` is what bridges the gap between string data and actual numeric computation.
> 

---

## 9. Common Built-In String Functions

> You don't need to memorize all of these — when in doubt, you can always look them up. Still, it's worth noting them down.
> 

| Function | What It Does | Example |
| --- | --- | --- |
| `length()` / `size()` | Returns the number of characters | `s.length()` |
| `resize(n)` | Shrinks/changes the string to `n` characters | `s.resize(3)` |
| `swap(other)` | Swaps the contents of two strings | `s1.swap(s2)` |
| `find(substr)` | Returns the **starting index** of a substring | `s.find("Yard")` |
| `substr(pos, len)` | Extracts `len` characters starting at `pos` | `s.substr(2, 4)` |
| `push_back(ch)` | Adds a single character to the end | `s.push_back('d')` |
| `pop_back()` | Removes the last character | `s.pop_back()` |
| `clear()` | Empties the string's contents (doesn't delete the variable) | `s.clear()` |
| `replace(pos, len, newstr)` | Replaces part of the string with another | `s.replace(0, 5, "Code")` |
| `compare(other)` | Compares two strings (like `strcmp`) | `s1.compare(s2)` |
| `erase(pos, len)` | Removes part of the string | `s.erase(4, 4)` |
| `empty()` | Checks whether the string has zero characters | `s.empty()` |

### Worked Examples

**`length()`**

```cpp
string s = "Hello";
cout << s.length();   // 5
```

**`substr(pos, len)` — Extracting a Substring**

```cpp
string s = "Programming";
cout << s.substr(0, 7);   // "Program"
```

> Reading it out: *"starting at index 0, give me the next 7 characters."*
> 

**`find(substr)` — Locating a Substring**

```cpp
string s = "C++ Programming";
cout << s.find("Pro");   // returns the starting index where "Pro" begins
```

**`erase(pos, len)` — Removing Part of a String**

```cpp
string s = "Hello World";
s.erase(5, 6);   // removes 6 characters starting at index 5
cout << s;        // "Hello"
```

**`replace(pos, len, newstr)` — Swapping Out Part of a String**

```cpp
string str = "LearnYard";
str.replace(0, 5, "Code");   // replace 5 chars starting at index 0
cout << str;                   // "CodeYard"
```

**`compare()` — Comparing Two Strings**

```cpp
string a = "abc";
string b = "abc";
cout << a.compare(b);   // 0 (equal)
```

> Just like `strcmp`: `0` means equal, negative means the calling string is "smaller," positive means it's "larger" — compared character by character based on ASCII order.
> 

**`swap()` — Exchanging Two Strings' Contents**

```cpp
string s1 = "Hello", s2 = "Namaste";
s1.swap(s2);
// now s1 = "Namaste", s2 = "Hello"
```

---

## 10. Side-by-Side: C-Style Strings vs. the String Class

| Feature | C-Style Strings | String Class |
| --- | --- | --- |
| **Underlying storage** | Character array | Array (handled internally) |
| **Size** | Fixed at declaration | **Dynamic** — grows/shrinks automatically |
| **Termination marker** | Requires `'\0'` (null character) | No null character concept — handled internally |
| **Printing full contents** | `cout << arr` works (special case for char arrays) | `cout << str` works directly |
| **Multi-word input** | `cin.getline(arr, size)` | `getline(cin, str)` — no size needed |
| **Concatenation** | `strcat(dest, src)` | `str1 + str2` or `.append()` |
| **Comparison** | `strcmp(a, b)` | `a.compare(b)` |
| **Length** | `strlen(arr)` (excludes `'\0'`) | `.length()` or `.size()` |
| **Header needed** | `<cstring>` | `<string>` |

> The instructor's key takeaway: understanding C-style strings first matters because, at the **core memory level**, this is genuinely what's happening under the hood — the string class simply hides this complexity from you and adds a lot of convenience on top.
> 

---

## Key Points to Remember

- The **string class** (`std::string`) is a built-in STL class — not a plain data type or function — that internally stores characters like an array but hides that complexity from you.
- Its biggest advantage over C-style strings is **dynamic sizing**: no fixed size limit, no manual null-character management.
- Taking input: plain `cin >>` still stops at whitespace; use **`getline(cin, str)`** for full-line input (no size argument needed, unlike `cin.getline` for character arrays). Use `cin.ignore()` if `getline` follows a prior `cin >>`.
- You can access individual characters with `[]` or `.at()`, and — unlike numeric arrays — `cout << str` prints the string's actual content directly.
- **Concatenation** is dramatically simpler than C-style strings: just use `+` or `.append()`.
- Strings containing digit-looking characters (like `"10"`) are **not numbers** — adding two such strings **concatenates** them (`"1020"`), it does **not** perform math. Use **`stoi()`** to convert a string to an actual integer for real arithmetic.
- The string class comes packed with convenient built-in functions: `length()`, `substr()`, `find()`, `erase()`, `replace()`, `compare()`, `swap()`, `push_back()`, `pop_back()`, `clear()`, and more — most of which have direct, clunkier C-style equivalents (`strlen`, `strcat`, `strcmp`, etc.).
- You don't need to memorize every function — knowing that they exist and roughly what they do is enough; the exact syntax can always be looked up when needed.

---