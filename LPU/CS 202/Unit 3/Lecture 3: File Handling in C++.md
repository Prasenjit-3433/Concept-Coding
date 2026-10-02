# Lecture 3: File Handling in C++

## Where This Fits

Every variable we've used so far — from a simple `int x` to an entire array of class Objects — lives in **RAM**, which is **temporary** (Static vs. Dynamic Memory lecture: even heap memory, once the program ends, is released back to the OS). The moment your program closes, everything it was holding in memory is gone.

```
┌──────────────────────────────────────────────────┐
│   Program runs → data lives in RAM (temporary)   │
│   Program ends → RAM is released → data is GONE  │
└──────────────────────────────────────────────────┘
```

**File handling** solves exactly this: it lets your program write data into a file on **secondary storage** (your hard disk / SSD) — storage that persists even after the program, and even the computer, is turned off.

> File handling means storing data **permanently** in a file, and reading it back later — unlike variables, which only exist temporarily while the program runs.
> 

---

## 1. The Three File-Handling Classes

Recall from Lecture 1 (Preprocessors, Header Files, Namespaces): `cin` and `cout` aren't functions — they're **objects**, made available through the `iostream` header, which internally is built from an `istream` class (input) and `ostream` class (output). Reading and writing to a file works on the exact same principle — just with a different header, and different classes.

```cpp
#include <fstream>
```

| Class | Purpose |
| --- | --- |
| `ofstream` | **O**utput to a **f**ile — used to **write** data into a file |
| `ifstream` | **I**nput from a **f**ile — used to **read** data from a file |
| `fstream` | Combination of both — can **read and write** |

> Just as `iostream` is the combination of `istream` and `ostream`, `fstream` is the combination of `ifstream` and `ofstream`. If you include `fstream`, you get both reading and writing ability in one class.
> 

---

## 2. Writing to a File — The Three-Step Process

Writing to a file always follows the same three steps: **open**, **write**, **close**.

```
┌────────────────────────────────────────┐
│   STEP 1: Open the file                │
│   STEP 2: Write data into it           │
│   STEP 3: Close the file               │
└────────────────────────────────────────┘
```

> Closing is necessary so that whatever computer resources the file was using get released back to the system — leaving a file open unnecessarily keeps those resources tied up.
> 

### Worked Example

```cpp
#include <iostream>
#include <fstream>
using namespace std;

int main() {
    ofstream fout;              // Step 0: create an ofstream object
    fout.open("data.txt");        // Step 1: open (creates the file if it doesn't exist)

    fout << "Welcome to File Handling in C++\n";   // Step 2: write
    fout << "This is a text file example";

    fout.close();                  // Step 3: close

    return 0;
}
```

### Tracing Through It

```
fout.open("data.txt");
   ↓
Does data.txt already exist in this folder?
   ├── Yes → that file gets opened
   └── No  → a brand-new data.txt gets CREATED automatically
   ↓
fout << "...";
   ↓
Text gets written into data.txt, using the SAME << operator you
already know from cout — because ofstream objects support it too
   ↓
fout.close();
   ↓
File is saved and resources are released
```

> Notice the writing syntax: `fout << "some text";` — this is the **exact same** `<<` operator used with `cout` (Lecture 1). `ofstream` objects support it too, since both `cout` and file-output objects are built from the same underlying output-stream design. There's nothing new to learn here syntactically — only the destination (a file, instead of the console) has changed.
> 

---

## 3. Reading From a File — Two Approaches

Just like writing follows open → write → close, reading follows **open → read → close**. But reading has two distinct styles, depending on what you need.

### Approach 1 — Character by Character

```cpp
#include <iostream>
#include <fstream>
using namespace std;

int main() {
    ifstream fin("data.txt");   // open directly, by passing the filename to the constructor
    char ch;

    while (fin.get(ch)) {         // reads ONE character into ch, returns false at end of file
        cout << ch;
    }

    fin.close();
    return 0;
}
```

> Notice `ifstream fin("data.txt");` — the filename is passed **directly into the constructor**, opening the file in one step, instead of a separate `fin.open(...)` call. Both styles work; this is simply shorter.
> 

> `fin.get(ch)` reads a single character from the file into `ch`, and the `while` loop condition checks whether that read actually succeeded — once the file has no more characters left, it returns `false` and the loop stops automatically. This naturally handles **whitespace correctly** too, since `.get()` reads every character, including spaces — unlike plain `cin >>`, which stops at whitespace (Strings lecture, cin's whitespace-termination behavior).
> 

### Approach 2 — Line by Line

```cpp
#include <iostream>
#include <fstream>
#include <string>
using namespace std;

int main() {
    ifstream fin("data.txt");
    string line;

    while (getline(fin, line)) {   // reads one FULL LINE into 'line' each time
        cout << line << endl;
    }

    fin.close();
    return 0;
}
```

> This is the **exact same** `getline()` function from the Strings lecture — there, we used `getline(cin, name)` to read a full line (including spaces) from the console. Here, we simply swap `cin` for `fin` (our file object), and it reads a full line **from the file** instead. The loop keeps calling `getline` and stops automatically once there are no more lines left to read.
> 

### Character-by-Character vs. Line-by-Line — When to Use Which

| Approach | Best for | Behavior |
| --- | --- | --- |
| `fin.get(ch)` | Processing one symbol at a time | Reads every character, including spaces |
| `getline(fin, line)` | Reading whole sentences/records | Reads up to (and stops at) each newline |

---

## 4. File Opening Modes

When you open a file, you can specify **how** you want to interact with it — this is called the file's **mode**.

### Syntax

```cpp
ofstream fout("filename.txt", ios::mode);
```

> `ios` is the base stream class all these modes live inside; `::` is the **Scope Resolution Operator** (Class Objects & Access Specifiers lecture) — read `ios::app` as "the `app` mode, defined inside the `ios` class."
> 

### The Modes

| Mode | Meaning |
| --- | --- |
| `ios::in` | **Read** mode — only takes input from the file |
| `ios::out` | **Write** mode — only outputs to the file |
| `ios::app` | **Append** — adds new content to the **end** of the file, without touching what's already there |
| `ios::trunc` | **Truncate** — deletes all existing content before writing fresh data |
| `ios::ate` | Opens the file and immediately jumps to its **end** ("at end") |
| `ios::binary` | Opens the file in **binary mode** — works with raw bytes instead of human-readable text (Response 2 covers this in depth) |

### Worked Example — Append Mode

```cpp
ofstream fout("data.txt", ios::app);
fout << "\nNew line added";
fout.close();
```

> Without `ios::app`, opening a file with `ofstream` normally **overwrites** any existing content. `ios::app` specifically preserves whatever's already there, and only adds your new text onto the end — genuinely useful for something like a running log file, where you want to keep adding entries without losing history.
> 

### Combining Multiple Modes

You can combine modes using the bitwise OR operator (`|`) — recall from the Operators lecture that `|` sets a bit if **either** side has it set, which is exactly the mechanism used here to combine multiple flags into one:

```cpp
fstream file("sample.txt", ios::in | ios::out | ios::app);
```

> This opens `sample.txt` for **reading, writing, and appending**, all at once — three separate mode flags, merged together with `|`.
> 

---

## Recap So Far

```
┌─────────────────────────────────────────────────────────────────┐
│                    FILE HANDLING BASICS                         │
├─────────────────────────────────────────────────────────────────┤
│  WHY: RAM is temporary; files on secondary storage persist      │
│                                                                 │
│  CLASSES: ofstream (write) │ ifstream (read) │ fstream (both)   │
│                                                                 │
│  WRITING: open → fout << data → close                           │
│  READING (char-by-char): while (fin.get(ch)) ...                │
│  READING (line-by-line):  while (getline(fin, line)) ...        │
│                                                                 │
│  MODES: ios::in, ios::out, ios::app, ios::trunc, ios::ate,      │
│         ios::binary — combine with | (bitwise OR)               │
└─────────────────────────────────────────────────────────────────┘
```

---

## 5. `fstream` — Reading and Writing Together

Recall from Response 1: `fstream` is the combined class, built from both `ifstream` and `ofstream`. Where `ofstream` alone can only write, and `ifstream` alone can only read, an `fstream` object can do **both** — provided you open it with the right combination of modes.

```cpp
#include <iostream>
#include <fstream>
using namespace std;

int main() {
    fstream file("sample.txt", ios::in | ios::out | ios::app);

    file << "Hello File\n";
    file.close();

    return 0;
}
```

> Here, `ios::in | ios::out | ios::app` (the bitwise-OR mode combination from Response 1) opens the file ready for reading, writing, and appending — all through the **one** `file` object. You don't need a separate `ifstream` and `ofstream` pair when a single `fstream` object can cover both roles.
> 

---

# Part B: Binary File Handling

## 1. Text Files vs. Binary Files

Everything in Response 1 dealt with **text files** — human-readable content, the kind you can open in a text editor and read directly.

> A **binary file** stores the raw bytes of memory, rather than human-readable text.
> 

```
┌───────────────────────────────────────────────────────────┐
│              TEXT FILE  vs.  BINARY FILE                  │
│                                                           │
│  Text:    "Amit,101,85.5"   ← readable characters         │
│  Binary:  01000001 01101101 01101001 01110100 ...         │
│           ← raw bytes, exactly as they sit in memory      │
└───────────────────────────────────────────────────────────┘
```

Binary files are commonly used for images, whole Objects, and database-style records — and they're generally **faster** to read/write, since there's no conversion between memory's raw bytes and readable text happening in either direction.

### Opening in Binary Mode

```cpp
ofstream fout("student.dat", ios::binary);
```

> Notice the **file extension convention**: text files typically use `.txt`; binary files typically use `.dat`. This is just a naming convention, not a hard rule the compiler enforces — but it's what you'll see everywhere in practice.
> 

---

## 2. Writing a Whole Object to a Binary File — `.write()`

Instead of writing individual pieces of data with `<<` (which converts everything to readable text), binary mode lets you write an entire Object's raw memory in one shot, using the **`.write()`** function.

### Worked Example — The `Student` Class

```cpp
#include <iostream>
#include <fstream>
using namespace std;

class Student {
public:
    int roll;
    char name[20];
    float marks;
};

int main() {
    Student s = {101, "Amit", 85.5};

    ofstream fout("student.dat", ios::binary);
    fout.write((char*)&s, sizeof(s));
    fout.close();

    return 0;
}
```

### Breaking Down `.write((char*)&s, sizeof(s));`

```
fout.write(  (char*)&s   ,   sizeof(s)  );
              │                  │
              │                  └── HOW MANY BYTES to write
              └── WHERE those bytes start — s's own address,
                  reinterpreted as a plain byte pointer
```

- **`&s`** — the address-of operator (Pointers & References lecture) gives us `s`'s memory address.
- **`(char*)`** — an explicit type cast (Data Types lecture — C-style casting). `.write()` doesn't know or care what type of Object you're saving; it just wants "a pointer to raw bytes," which is exactly what a `char*` represents here. This is conceptually the same idea as a **void pointer** (Advanced Pointers lecture) needing a cast before use — `.write()` insists on `char*` specifically, since that's the type its function signature expects.
- **`sizeof(s)`** — the `sizeof` operator (Data Types lecture) tells `.write()` exactly how many bytes to copy out of memory, starting from that address. Since `Student` holds an `int` (4 bytes) + a 20-character array (20 bytes) + a `float` (4 bytes), `sizeof(s)` here is `28` bytes total (Structures lecture — a structure/class's total size is the sum of its members, ignoring any compiler padding).

> The net effect: `.write()` copies `sizeof(s)` bytes, starting at `s`'s address, directly into the file — exactly as those bytes sit in memory right now. No text conversion, no formatting — just a raw byte-for-byte dump.
> 

---

## 3. Reading a Whole Object Back — `.read()`

The mirror-image function, `.read()`, pulls those raw bytes back out of the file and drops them straight into an Object's memory.

```cpp
#include <iostream>
#include <fstream>
using namespace std;

class Student {
public:
    int roll;
    char name[20];
    float marks;
};

int main() {
    Student s;

    ifstream fin("student.dat", ios::binary);
    fin.read((char*)&s, sizeof(s));

    cout << "Roll: " << s.roll << endl;
    cout << "Name: " << s.name << endl;
    cout << "Marks: " << s.marks << endl;

    fin.close();
    return 0;
}
```

**Output:**

```
Roll: 101
Name: Amit
Marks: 85.5
```

> Notice `Student s;` here is declared **empty** — no values given. `.read()` is what fills it, by copying `sizeof(s)` bytes straight from the file into `s`'s memory address, in exactly the same raw layout `.write()` originally saved it in. This only works correctly because both sides agree on the exact same `Student` structure — same members, same order, same sizes.
> 

---

## 4. Writing and Reading Multiple Objects — Looping `.write()` / `.read()`

A single `.write()` call handles one Object. To store several records — say, an entire class of students — you simply call `.write()` repeatedly, once per Object, inside a loop.

### Writing Multiple Records

```cpp
Student s;
ofstream fout("student.dat", ios::binary);

for (int i = 0; i < 3; i++) {
    cin >> s.roll >> s.name >> s.marks;
    fout.write((char*)&s, sizeof(s));
}

fout.close();
```

> Each pass through the loop takes fresh input into the **same** `s` variable, then writes it out — so the file ends up holding three separate, back-to-back 28-byte blocks, one per Student.
> 

### Reading Multiple Records — Using `.read()` as the Loop's Own Condition

```cpp
ifstream fin("student.dat", ios::binary);
Student s;

while (fin.read((char*)&s, sizeof(s))) {
    cout << s.roll << " " << s.name << " " << s.marks << endl;
}

fin.close();
```

> This is a compact, important pattern: `.read()` itself returns a value that's `true` if the read succeeded, and `false` once there's nothing left to read. So `while (fin.read(...))` doubles as both "read the next record" **and** "check whether we've reached the end" — the loop naturally stops the moment the file runs out of records, without needing a separate `eof()` check inside the loop body.
> 

---

## 5. Handling File Errors

Not every file operation succeeds — the file might not exist, might be locked, or you might make a mistake in file-handling logic. C++ gives you three checks for this.

### `is_open()` — Did the File Actually Open?

```cpp
ifstream fin("abc.txt");

if (!fin.is_open()) {
    cout << "File cannot be opened";
}
```

> `is_open()` returns `true` if the file successfully opened, `false` otherwise. Checking `!fin.is_open()` — read as "if it is **not** open" (logical NOT, Operators lecture) — is the standard way to catch a missing or inaccessible file **before** trying to read from or write to it.
> 

### `fail()` — Did the Last Operation Fail?

```cpp
ofstream fout("data.txt");

if (fout.fail()) {
    cout << "File opening failed";
}
```

> `fail()` checks whether the most recent stream operation encountered a problem — similar in spirit to `is_open()`, but more general: it can also catch failures during reading/writing, not just at the open step.
> 

### `eof()` — Have We Reached the End of the File?

```cpp
while (!fin.eof()) {
    // not recommended alone
}
```

> `eof()` (**e**nd **o**f **f**ile) returns `true` once the file pointer has passed the last piece of data. The instructor's own note flags this pattern (`while (!fin.eof())`) as **"not recommended alone"** — it's a genuinely common beginner trap. The problem: `eof()` only becomes `true` *after* a read has already failed trying to go past the end — meaning the loop body can still execute one extra, garbage time before the loop notices and stops. This is exactly why the `while (fin.read(...))` pattern from Section 4 above is the safer, preferred style: it checks success **at the moment of reading**, rather than checking end-of-file as an afterthought.
> 

### Quick Reference

| Function | Checks | Best used for |
| --- | --- | --- |
| `is_open()` | Did the file successfully open? | Right after `open()`, before any read/write |
| `fail()` | Did the last stream operation fail? | After any read/write, for general error-catching |
| `eof()` | Has the file pointer passed the last data? | Rarely alone — prefer checking a read's own return value instead |

---

## Recap — Full File Handling Lecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                  **FILE HANDLING** — FULL MAP                        │
├──────────────────────────────────────────────────────────────────────┤
│  WHY: RAM is temporary; secondary storage (files) persists           │
│                                                                      │
│  CLASSES                                                             │
│    ofstream → write only   ifstream → read only                      │
│    fstream  → both (needs ios::in | ios::out mode combo)             │
│                                                                      │
│  TEXT FILES                                                          │
│    Write: fout << data;                                              │
│    Read (char):  while (fin.get(ch))                                 │
│    Read (line):  while (getline(fin, line))                          │
│                                                                      │
│  MODES (combine with |, bitwise OR)                                  │
│    ios::in, ios::out, ios::app, ios::trunc, ios::ate,                │
│    ios::binary                                                       │
│                                                                      │
│  BINARY FILES                                                        │
│    Write: fout.write((char*)&obj, sizeof(obj));                      │
│    Read:  fin.read((char*)&obj, sizeof(obj));                        │
│    Multiple records: loop .write() to save,                          │
│                       while(fin.read(...)) to load until EOF         │
│                                                                      │
│  ERROR CHECKING                                                      │
│    is_open() → did the file open?                                    │
│    fail()    → did the last operation fail?                          │
│    eof()     → reached the end? (avoid using ALONE as a              │
│                 loop condition — prefer checking a read's own        │
│                 success instead)                                     │
└──────────────────────────────────────────────────────────────────────┘
```