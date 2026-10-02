# Lecture 4: Random Accessing Files & Structs

## Where This Fits

Every read/write operation in the File Handling lecture followed the file **from start to finish, in order** — write record 1, then 2, then 3; read record 1, then 2, then 3. This is called **sequential access**.

> **Sequential access**: reading or writing a file's data strictly in order, one piece after another, from the beginning.
> 

But sometimes you don't want to read the *whole* file just to reach one specific record — imagine a file with 10,000 student records, and you only need to update record #4,521. Reading through the first 4,520 records just to get there would be enormously wasteful.

> **Random access**: jumping directly to any specific position (byte offset) inside a file, without reading through everything before it.
> 

```
┌───────────────────────────────────────────────────────────┐
│         **SEQUENTIAL** vs. **RANDOM ACCESS**              │
│                                                           │
│  **SEQUENTIAL**:  [1][2][3] [4][5][6][7][8][9][10]        │
│                └──┴──┴──┴──┴──┴──┴──┴──┴──┴──┘            │
│                must pass through every record             │
│                in order to reach the end                  │
│                                                           │
│  **RANDOM**:      [1][2][3][4][5][6]**[7]**[8][9][10]     │
│                                  ▲                        │
│                    jump DIRECTLY here                     │
│                    — records 1-6 are never touched        │
└───────────────────────────────────────────────────────────┘
```

---

# 🎯Part 1: Random Access File Processing

## 1. The File Pointer — What's Actually Being Moved

Every open file stream secretly keeps track of **where** the next read or write will happen — this position is called the **file pointer** (a different concept from the memory pointers in Pointers & References, though the name is deliberately similar: both "point at" a specific location).

> Every `ifstream`/`ofstream`/`fstream` object maintains an internal position — the file pointer — marking exactly where the next byte will be read from or written to.
> 

Since reading and writing are tracked **separately**, C++ actually gives you two distinct pointers:

| Pointer | Stands for | Controls |
| --- | --- | --- |
| **get pointer** | "get" (reading) | Where the next `.read()` will pull data from |
| **put pointer** | "put" (writing) | Where the next `.write()` will place data |

## 2. The Four Functions

| Function | Purpose |
| --- | --- |
| `seekg(offset)` | Move the **get** (read) pointer to a specific byte position |
| `seekp(offset)` | Move the **put** (write) pointer to a specific byte position |
| `tellg()` | Return the **current** position of the get pointer |
| `tellp()` | Return the **current** position of the put pointer |

> Naming pattern worth locking in: **g** = get (reading), **p** = put (writing). `seek` = "move to," `tell` = "tell me where you currently are."
> 

### Basic Syntax

```cpp
fin.seekg(10);       // move the read pointer to byte 10 (counting from the start)
fout.seekp(0);        // move the write pointer to byte 0 — the very beginning
```

By default, `seekg`/`seekp` count from the **beginning** of the file. You can also specify a different reference point as a second argument:

```cpp
fin.seekg(0, ios::beg);   // 0 bytes from the BEGINNING (the default)
fin.seekg(0, ios::end);   // 0 bytes from the END — i.e., jump straight to the end
fin.seekg(-4, ios::end);  // 4 bytes BACK from the end
fin.seekg(4, ios::cur);   // 4 bytes FORWARD from wherever the pointer currently is
```

| Reference Point | Meaning |
| --- | --- |
| `ios::beg` | Measure from the **beginning** of the file (the default if omitted) |
| `ios::cur` | Measure from the pointer's **current** position |
| `ios::end` | Measure from the **end** of the file |

---

## 3. Why This Matters With Binary Files Specifically

Recall from the File Handling lecture: a binary record written with `.write((char*)&s, sizeof(s))` always takes up **exactly the same number of bytes** every time, for a given class/struct (`sizeof(s)` never changes for the same type). This fixed-size property is exactly what makes random access *possible* — if you know each record is, say, 28 bytes wide, then record number `n` (counting from 0) always starts at byte `n × 28`, with zero ambiguity.

```
Record 0: bytes   0–27
Record 1: bytes  28–55
Record 2: bytes  56–83
Record 3: bytes  84–111
              ↑
   record n starts at byte:  n × sizeof(record)
```

> This is precisely why random access is demonstrated with **binary** files, not text files — a text file's records can vary in length line to line (different names, different digit counts), so there's no fixed formula for "where does record #4,521 start." Binary's fixed-size records are what make jump-directly-there access possible at all.
> 

---

## 4. Worked Example — Updating One Record Without Rewriting the Whole File

Building directly on the `Student` class from the File Handling lecture:

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
    // Step 1: write 3 records sequentially, exactly as before
    Student list[3] = {
        {101, "Amit", 85.5},
        {102, "Sonal", 76.0},
        {103, "Rahul", 91.2}
    };

    ofstream fout("students.dat", ios::binary);
    for (int i = 0; i < 3; i++) {
        fout.write((char*)&list[i], sizeof(list[i]));
    }
    fout.close();

    // Step 2: jump DIRECTLY to record #1 (Sonal) and update her marks only
    fstream file("students.dat", ios::in | ios::out | ios::binary);

    Student updated = {102, "Sonal", 99.9};   // her new record
    file.seekp(1 * sizeof(Student));            // jump to where record #1 starts
    file.write((char*)&updated, sizeof(updated));

    file.close();

    // Step 3: read all 3 records back to confirm only Sonal changed
    ifstream fin("students.dat", ios::binary);
    Student s;
    while (fin.read((char*)&s, sizeof(s))) {
        cout << s.roll << " " << s.name << " " << s.marks << endl;
    }
    fin.close();

    return 0;
}
```

**Output:**

```
101 Amit 85.5
102 Sonal 99.9
103 Rahul 91.2
```

### Tracing Through the Key Line

```
file.seekp(1 * sizeof(Student));
              │
              └── sizeof(Student) = 28 bytes (from the File Handling lecture's
                  calculation: 4-byte int + 20-byte char array + 4-byte float)

1 * 28 = byte 28  →  exactly where record #1 (Sonal's record) begins

file.write(...)  →  overwrites JUST those 28 bytes — records #0 and #2
                     are never touched, never even read
```

> Notice what did **not** happen here: we never read through Amit's record, never rewrote the whole file, never even opened it with `ios::trunc` (which would have wiped everything). `seekp` let us reach into the *middle* of an existing file and overwrite exactly one record — this is the entire value proposition of random access over sequential access.
> 

---

## 5. Reading One Specific Record — `seekg` in Action

The read side works identically, using `seekg` instead of `seekp`:

```cpp
ifstream fin("students.dat", ios::binary);
Student s;

fin.seekg(2 * sizeof(Student));      // jump straight to record #2 (Rahul)
fin.read((char*)&s, sizeof(s));

cout << s.roll << " " << s.name << " " << s.marks;
fin.close();
```

**Output:** `103 Rahul 91.2`

> This reads **only** Rahul's 28 bytes — records #0 and #1 are skipped entirely, never loaded into memory at all. For a file with thousands of records, this is the difference between an instant lookup and reading megabytes of data you don't need.
> 

---

## 6. `tellg()` / `tellp()` — Checking Where You Are

```cpp
ifstream fin("students.dat", ios::binary);
Student s;

fin.read((char*)&s, sizeof(s));
cout << "After reading record 0, position is: " << fin.tellg() << endl;

fin.read((char*)&s, sizeof(s));
cout << "After reading record 1, position is: " << fin.tellg() << endl;

fin.close();
```

**Output:**

```
After reading record 0, position is: 28
After reading record 1, position is: 56
```

> Each `.read()` call automatically **advances** the get pointer forward by however many bytes it just consumed — `tellg()` simply reports wherever that pointer currently sits. This confirms the "record n starts at byte n × size" formula from Section 3 directly: after reading one 28-byte record, the pointer sits at byte 28, ready for record #1 to begin exactly there.
> 

---

## Recap — Random Access

```
┌───────────────────────────────────────────────────────────────┐
│               RANDOM ACCESS FILE PROCESSING                   │
├───────────────────────────────────────────────────────────────┤
│  Sequential access  → read/write strictly start to end        │
│  Random access      → jump directly to any byte position      │
│                                                               │
│  seekg(offset)  → move the READ pointer                       │
│  seekp(offset)  → move the WRITE pointer                      │
│  tellg()        → report current READ position                │
│  tellp()        → report current WRITE position               │
│                                                               │
│  Optional 2nd argument (reference point):                     │
│     ios::beg (default) │ ios::cur │ ios::end                  │
│                                                               │
│  Works cleanly with BINARY files because every record         │
│  is a FIXED size (sizeof(RecordType)) — so record n           │
│  always starts at byte  n × sizeof(RecordType)                │
│                                                               │
│  Lets you update/read ONE record without touching the         │
│  rest of the file — the core advantage over sequential        │
│  access                                                       │
└───────────────────────────────────────────────────────────────┘
```

---

# 🎯Part 2: Structures and File Operations

## 1. Nothing New Mechanically — Just a Different Blueprint

Recall from Structures, Unions & Enums, and again from Class Objects & Access Specifiers: a `struct` and a `class` are structurally almost identical in C++ — the **only** real difference is default access (`struct` members are `public` by default; `class` members are `private` by default). Everything else — memory layout, `sizeof` calculation, how `.write()`/`.read()` treat the raw bytes — is completely identical between the two.

> Since `.write()` and `.read()` only care about **raw bytes at an address**, and don't care at all whether that address belongs to a `class` or a `struct`, the exact same binary file techniques from the File Handling lecture apply to a `struct` with **zero changes**.
> 

## 2. Worked Example — Rewriting `Student` as a `struct`

```cpp
#include <iostream>
#include <fstream>
using namespace std;

struct Student {
    int roll;
    char name[20];
    float marks;
};   // note the required trailing semicolon — Structures lecture

int main() {
    Student s = {101, "Amit", 85.5};

    // Writing — IDENTICAL to the class version
    ofstream fout("student.dat", ios::binary);
    fout.write((char*)&s, sizeof(s));
    fout.close();

    // Reading — IDENTICAL to the class version
    Student s2;
    ifstream fin("student.dat", ios::binary);
    fin.read((char*)&s2, sizeof(s2));
    fin.close();

    cout << "Roll: " << s2.roll << endl;
    cout << "Name: " << s2.name << endl;
    cout << "Marks: " << s2.marks << endl;

    return 0;
}
```

**Output:**

```
Roll: 101
Name: Amit
Marks: 85.5
```

> Compare this line-for-line against the `class Student` version from the File Handling lecture — the `.write()` and `.read()` calls are byte-for-byte the same code. The only change is the keyword `struct` instead of `class`, and (since `struct` defaults to public) we didn't even need to write `public:` explicitly, unlike the `class` version.
> 

## 3. Why the Syllabus Names Both Separately Anyway

Even though the *mechanics* are identical, it's worth understanding **why** a real course would still call out `struct` and `class` as separate syllabus bullet points:

- **Historical/practical convention**: in real-world C++ codebases, plain data records meant purely for storage (like a `Student` record with no behavior attached) are very often written as `struct`s — reserving `class` for objects that also carry meaningful behavior (methods), private state, and encapsulation (Class Objects & Access Specifiers lecture).
- **Exam-style questions** sometimes test whether a student *assumes* file operations only work with `class`, and deliberately ask the same question phrased around a `struct` instead — this is a recognition check, not a new mechanic to learn.

## 4. A `struct` Works with Random Access Too

Since Part 1's entire random-access technique depends only on `sizeof(RecordType)` being fixed — and a `struct`'s size calculation works exactly like a `class`'s (Structures lecture) — every random-access example above works identically if `Student` were declared as a `struct` instead. Nothing needs to change beyond the keyword itself.

---

## Recap — Structures and File Operations

```
┌───────────────────────────────────────────────────────────────┐
│              STRUCTURES AND FILE OPERATIONS                   │
├───────────────────────────────────────────────────────────────┤
│  A struct's binary file operations are MECHANICALLY           │
│  IDENTICAL to a class's — .write()/.read() only care          │
│  about raw bytes at an address, not which keyword             │
│  declared the type                                            │
│                                                               │
│  Only real difference: struct members are public by           │
│  default (Class Objects & Access Specifiers lecture) —        │
│  so no explicit 'public:' section is needed for a plain       │
│  data-record struct                                           │
│                                                               │
│  Convention: plain storage-only records → often struct        │
│              records needing behavior/encapsulation → class   │
│                                                               │
│  Random access (seekg/seekp) works identically for            │
│  structs too, since it only depends on sizeof(RecordType)     │
│  being fixed                                                  │
└───────────────────────────────────────────────────────────────┘
```