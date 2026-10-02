# Practice Set for Lecture 3 & 4

# 🎯Questions 1–12

---

#### **Q1.** Which header file must be included to perform file handling in C++?

A) `<iostream>`

B) `<fstream>`

C) `<filestream>`

D) `<stdio.h>`

**Answer: B**

**Explanation:** `fstream` is the header that provides the three file-handling classes — `ofstream`, `ifstream`, and `fstream` itself. `<iostream>` only gives you console I/O (`cin`, `cout`); it has nothing to do with files. `<filestream>` doesn't exist in C++. `<stdio.h>` is the C-style file I/O header, not the C++ one covered in this course.

---

#### **Q2.** Which class should you use if your program only needs to **write** data into a file?

A) `ifstream`

B) `ofstream`

C) `fstream`

D) `iostream`

**Answer: B**

**Explanation:** `ofstream` (**o**utput file stream) is built specifically for writing data out to a file. `ifstream` is for reading only. `fstream` can do both, but it's unnecessary overhead if you genuinely only ever need to write. `iostream` is unrelated to files entirely.

---

#### **Q3.** What does the following line do if `data.txt` does **not** already exist in the folder?

```cpp
ofstream fout("data.txt");
```

A) Throws a compiler error

B) Throws a runtime error and terminates the program

C) Creates a new, empty `data.txt` file automatically

D) Silently does nothing

**Answer: C**

**Explanation:** Opening a file with `ofstream` automatically creates it if it doesn't already exist — this is one of the first things established in the File Handling lecture. There's no error here at all; this is completely normal, expected behavior. If the file *did* already exist, the default behavior would instead overwrite its existing content.

---

#### **Q4.** What is the correct C++ syntax to write the string `"Hello"` into a file object named `fout`?

A) `fout.write("Hello");`

B) `fout << "Hello";`

C) `fout >> "Hello";`

D) `write(fout, "Hello");`

**Answer: B**

**Explanation:** `ofstream` objects support the exact same `<<` operator used with `cout` — this was explicitly emphasized in the lecture as "nothing new to learn," since the syntax is identical to console output, just redirected to a file. A (`.write()`) is the binary-mode function, which takes a `char*` and a size, not a plain string like this. C uses the wrong operator direction (`>>` is for reading). D isn't valid C++ syntax for this.

---

#### **Q5.** Which function reads an entire file **character by character**, stopping automatically once there is nothing left to read?

A) `getline(fin, ch)`

B) `fin >> ch` inside a `for` loop

C) `while (fin.get(ch))`

D) `fin.readChar(ch)`

**Answer: C**

**Explanation:** `fin.get(ch)` reads exactly one character into `ch` and returns a value that evaluates to `false` once the end of the file is reached — which is exactly what makes `while (fin.get(ch))` work as a self-terminating loop. A mismatches `getline`'s purpose (it reads full lines into a `string`, not single characters into a `char`). B (`fin >> ch`) would actually skip whitespace and stop at the first space, unlike `.get()`, which reads every character including spaces. D isn't a real C++ function.

---

#### **Q6.** What is the key difference in behavior between `fin.get(ch)` and `fin >> ch` when reading a file containing `"C++ is fun"`?

A) There is no difference — both behave identically

B) `.get(ch)` reads every character including spaces; `fin >> ch` skips over whitespace

C) `fin >> ch` reads faster than `.get(ch)`

D) `.get(ch)` can only read the first character of the file

**Answer: B**

**Explanation:** This is directly from the lecture's comparison: `.get()` captures every character, spaces included, while plain `cin`/`fin >>` style extraction stops at (and skips) whitespace — exactly the same whitespace-termination behavior first seen with `cin >>` in the Strings lecture. This is precisely why `.get()` is the correct choice when spacing in the output matters.

---

#### **Q7.** Which function is used to read an entire **line** (including any spaces within it) from a file into a `string` variable?

A) `fin.readLine(line)`

B) `fin >> line`

C) `getline(fin, line)`

D) `fin.get(line)`

**Answer: C**

**Explanation:** `getline(fin, line)` is the exact same `getline()` function introduced in the Strings lecture for reading a full line from `cin` — here it's simply given a file stream (`fin`) instead of `cin` as its source. B (`fin >> line`) would stop at the first whitespace inside the line, not read the whole thing. A isn't a real function. D is a plausible-looking option, but `.get()` is designed for single characters, not whole lines, and doesn't accept a `string` argument this way.

---

#### **Q8.** What does `ios::app` mode do when opening a file for writing?

A) Deletes all existing content before writing

B) Adds new content to the end of the file, without touching existing content

C) Opens the file in read-only mode

D) Automatically closes the file after one write

**Answer: B**

**Explanation:** `app` stands for **append** — new writes are added onto the end of whatever content is already there, preserving it entirely. This is explicitly contrasted in the lecture against the *default* `ofstream` behavior (which overwrites existing content) and against `ios::trunc` (which explicitly wipes existing content before writing).

---

#### **Q9.** Which mode, if used instead of `ios::app`, would **delete** all existing content in a file before new data is written?

A) `ios::ate`

B) `ios::binary`

C) `ios::trunc`

D) `ios::in`

**Answer: C**

**Explanation:** `ios::trunc` ("truncate") explicitly clears out existing content before writing fresh data — the direct opposite of `ios::app`'s preserve-and-add behavior. `ios::ate` just moves the pointer to the end without deleting anything. `ios::binary` controls byte-vs-text mode, unrelated to content deletion. `ios::in` is the read mode.

---

#### **Q10.** How do you open a file for both reading **and** writing at the same time?

A) You cannot — a file can only be opened in one mode at a time

B) Use `fstream` with `ios::in | ios::out`

C) Open the file twice, once with `ifstream` and once with `ofstream`, in the same statement

D) Use `ios::both`

**Answer: B**

**Explanation:** `fstream`, combined with the bitwise OR (`|`) of `ios::in` and `ios::out`, is exactly how the lecture demonstrated combined read/write access through one single object. `ios::both` isn't a real mode flag. C would technically work as two separate objects but isn't how the lecture teaches combining modes, and isn't what the question is asking for. A is false — multiple mode flags can absolutely be combined via `|`.

---

#### **Q11.** What is stored in a **binary** file, as opposed to a text file?

A) Human-readable ASCII characters only

B) The raw bytes of memory, exactly as they exist in RAM

C) Compressed text using a special encoding

D) Only numeric data — binary files cannot store characters or strings

**Answer: B**

**Explanation:** This is the lecture's formal definition: a binary file stores the raw bytes of memory directly, with no text conversion — contrasted against a text file, which stores human-readable characters. D is incorrect — binary files can hold the raw bytes of *any* data type, including character arrays and strings, exactly as in the `Student` class example (`char name[20]` was written to the binary file along with the `int` and `float`).

---

#### **Q12.** In the function call `fout.write((char*)&s, sizeof(s));`, what is the purpose of the `(char*)` cast?

A) It converts `s`'s data into readable text before saving

B) It reinterprets `s`'s address as a plain pointer to raw bytes, which is what `.write()` requires

C) It permanently changes the data type of `s` to `char`

D) It has no real effect — it's optional syntax some programmers add out of habit

**Answer: B**

**Explanation:** `.write()`'s function signature expects a `char*` — a pointer to raw bytes — regardless of what type of object you're actually saving. The cast doesn't convert or alter `s`'s actual data at all (ruling out A and C); it just tells the compiler "treat this address as a byte pointer for the purposes of this function call," conceptually the same explicit-cast need seen with void pointers in the Advanced Pointers lecture. D is false — omitting the cast for a non-`char*` type causes a compiler error, since `.write()`'s signature doesn't accept just any pointer type without it.

# 🎯Questions 13–24

---

#### **Q13.** Why does `.write()` need the `sizeof(s)` argument?

A) To tell the compiler what data type `s` is

B) To specify exactly how many bytes should be copied out of memory into the file

C) To reserve extra space in the file for future writes

D) `sizeof(s)` is not actually required — it's optional

**Answer: B**

**Explanation:** `.write()` doesn't inherently know how "big" the object at the given address is — since the address was just cast to a generic `char*`, all type information about `s` is gone from `.write()`'s point of view. `sizeof(s)` explicitly tells it exactly how many bytes to copy, starting from that address. Without it, `.write()` would have no way to know where the object's data ends.

---

#### **Q14.** For the class below, what is `sizeof(s)` (assuming no compiler padding)?

```cpp
class Student {
public:
    int roll;
    char name[20];
    float marks;
};
```

A) 3 bytes (one per member)

B) 24 bytes

C) 28 bytes

D) It cannot be determined without running the program

**Answer: C**

**Explanation:** Recall from Structures, Unions & Enums: a structure/class's total size is the sum of its members' sizes. `int roll` = 4 bytes, `char name[20]` = 20 bytes (1 byte × 20 characters), `float marks` = 4 bytes. Total: 4 + 20 + 4 = **28 bytes**. D is incorrect — `sizeof` is resolved at compile time for a fixed-size type like this, not something that varies at runtime.

---

#### **Q15.** What does the following loop actually do?

```cpp
ifstream fin("student.dat", ios::binary);
Student s;

while (fin.read((char*)&s, sizeof(s))) {
    cout << s.roll << endl;
}
```

A) It reads only the first record, then exits

B) It reads and prints every record in the file, one at a time, until no records remain

C) It causes an infinite loop, since `while` conditions must be boolean

D) It throws a compiler error because `.read()` cannot be used as a loop condition

**Answer: B**

**Explanation:** `.read()` returns a value that's `true` when a read succeeds and `false` once the file has no more data — so the `while` loop naturally keeps reading and printing one record after another, and stops cleanly the moment the file is exhausted. This is exactly the pattern from the File Handling lecture for reading multiple binary records. C and D are both wrong — `.read()`'s return type is specifically designed to work as a boolean-style loop condition.

---

#### **Q16.** What is the danger of using `while (!fin.eof())` as a file-reading loop condition, as opposed to checking a read's own success?

A) `eof()` doesn't exist in standard C++

B) The loop body may execute one extra time with stale/garbage data, since `eof()` only becomes true *after* a failed read attempt

C) `eof()` can only be used with text files, never binary files

D) There is no danger — this is the universally recommended approach

**Answer: B**

**Explanation:** This is the specific trap flagged in the lecture's own instructor note ("not recommended alone"). `eof()` only flips to `true` **after** an attempted read has already gone past the end and failed — meaning the loop body can run one extra time processing leftover/garbage data from that failed read, before the loop notices and stops. The safer pattern is checking the read operation's own success directly (as in Q15), which catches the failure at the moment it happens rather than one iteration late.

---

#### **Q17.** Which function checks whether a file was successfully opened, immediately after attempting to open it?

A) `fail()`

B) `eof()`

C) `is_open()`

D) `good()`

**Answer: C**

**Explanation:** `is_open()` is specifically for checking the open step — `if (!fin.is_open())` is the standard pattern for catching a missing or inaccessible file before attempting any reads or writes. `fail()` is more general-purpose (checks the last operation broadly, not specifically the open step). `eof()` checks for end-of-file, unrelated to whether opening succeeded. `good()` wasn't covered in this lecture.

---

#### **Q18.** What does `seekg()` do?

A) Moves the write pointer to a specific position

B) Moves the read pointer to a specific position

C) Deletes data starting at a specific position

D) Returns the total size of the file in bytes

**Answer: B**

**Explanation:** The naming convention from the Gap Fill note: **g** = get (reading), **p** = put (writing). `seekg()` moves the **g**et pointer — the one controlling where the next `.read()` happens. `seekp()` (not this question's answer) is the write-side equivalent. Neither function deletes data or reports file size.

---

#### **Q19.** By default, what reference point does `seekg(10)` measure its offset from?

A) The current position of the pointer

B) The end of the file

C) The beginning of the file

D) It's undefined — a reference point must always be specified explicitly

**Answer: C**

**Explanation:** `ios::beg` (beginning of file) is the default reference point if no second argument is given — so `seekg(10)` means "move to byte 10, counting from the very start of the file." To measure from the current position or the end instead, you'd explicitly pass `ios::cur` or `ios::end` as a second argument.

---

#### **Q20.** In a binary file storing fixed-size `Student` records (each 28 bytes), which line correctly jumps directly to the **3rd** record (index 2, i.e., the third one written)?

A) `fin.seekg(2);`

B) `fin.seekg(3 * sizeof(Student));`

C) `fin.seekg(2 * sizeof(Student));`

D) `fin.seekg(28);`

**Answer: C**

**Explanation:** Records are indexed from 0: record #0 occupies bytes `0`–`27`, record #1 occupies bytes `28`–`55`, record #2 (the third record) occupies bytes `56`–`83`. The formula is `record_index × sizeof(RecordType)` — so the third record (index 2) starts at `2 × 28 = 56`. A treats the index as a raw byte count, ignoring record size entirely. B uses the wrong index (would land on the *4th* record's start). D happens to be the correct byte offset for record #1, not record #2 — a common off-by-one trap.

---

#### **Q21.** Why does random access file processing require **fixed-size** records (as achieved through binary mode), rather than working reliably with variable-length text lines?

A) Text files cannot be opened with `seekg`/`seekp` at all

B) Without a fixed record size, there's no reliable formula for where record `n` begins, since each line could differ in length

C) `seekg`/`seekp` only work on `.dat` files, never `.txt` files

D) Random access is actually equally easy with text files — this is a common misconception

**Answer: B**

**Explanation:** This is the core reasoning from the Gap Fill note: the formula `record_index × sizeof(RecordType)` only works because every binary record of the same type takes up exactly the same number of bytes. A text file's lines can vary — different names, different digit counts — so there's no consistent way to calculate "where line 4,521 starts" without actually scanning through everything before it. C is a naming-convention myth — `.dat` vs `.txt` is just a convention, not something that changes what `seekg`/`seekp` can operate on.

---

#### **Q22.** What does `tellg()` return?

A) The total number of bytes in the entire file

B) The current position of the read (get) pointer

C) The current position of the write (put) pointer

D) A boolean indicating whether the last read succeeded

**Answer: B**

**Explanation:** `tellg()` reports **where the get pointer currently sits** — useful for confirming your position after a read, exactly as demonstrated in the Gap Fill note's worked example (`tellg()` reporting byte 28 after reading one 28-byte record). `tellp()` (not this question) would report the write pointer's position instead. It does not report total file size or read success/failure.

---

#### **Q23.** After the following code runs, what does `fin.tellg()` report?

```cpp
ifstream fin("student.dat", ios::binary);   // Student is 28 bytes
Student s;

fin.read((char*)&s, sizeof(s));
cout << fin.tellg();
```

A) `0`

B) `1`

C) `28`

D) It cannot be determined — `tellg()` only works after `seekg()` is called first

**Answer: C**

**Explanation:** Each successful `.read()` call **advances** the get pointer forward by exactly the number of bytes it just consumed. Since one `Student` record is 28 bytes, after reading the first record the pointer now sits at byte 28 — ready for the *next* record to begin reading from exactly there. `tellg()` doesn't require a prior `seekg()` call (ruling out D) — it simply reports wherever the pointer happens to be at that moment, including its starting position before any seek.

---

#### **Q24.** A `struct` and a `class` holding the exact same members (same types, same order) are used for binary file writing with `.write()`. Which statement is correct?

A) `.write()` will fail to compile for a `struct`, since binary operations only work with `class`

B) `.write()` behaves identically for both — it only cares about raw bytes at an address, not which keyword declared the type

C) A `struct`'s version will take up less file space, since structs don't have access specifiers

D) You must convert the `struct` into a `class` before it can be written to a binary file

**Answer: B**

**Explanation:** This is the core point of the Gap Fill note's structures section: `.write()`/`.read()` operate purely on raw bytes at a given address and size — they have no awareness of, or preference for, whether that data originated from a `class` or a `struct`. C is a myth — access specifiers (`public`/`private`) are a compile-time-only concept and have zero effect on runtime memory layout or file size. A and D are both false; no such restriction or conversion exists.

# 🎯Questions 25–33

---

#### **Q25.** What does the following code print, given `data.txt` does not yet exist?

```cpp
ofstream fout("data.txt");
if (fout.is_open()) {
    cout << "Opened successfully";
} else {
    cout << "Failed to open";
}
```

A) `Failed to open` — since the file doesn't exist yet

B) `Opened successfully` — since `ofstream` creates the file automatically

C) Compiler error — `is_open()` cannot be used with `ofstream`

D) Nothing prints — the program crashes

**Answer: B**

**Explanation:** As established in Part 1's Q3, `ofstream` automatically **creates** a file that doesn't already exist rather than failing. So the open succeeds, `is_open()` correctly returns `true`, and `"Opened successfully"` prints. `is_open()` works identically across `ifstream`, `ofstream`, and `fstream` — it isn't restricted to any one class.

---

#### **Q26.** Which of these correctly opens a file for writing in **binary append** mode — i.e., adding new binary records onto the end of an existing binary file, without erasing what's already there?

A) `ofstream fout("data.dat", ios::binary);`

B) `ofstream fout("data.dat", ios::binary | ios::app);`

C) `ofstream fout("data.dat", ios::trunc);`

D) `ofstream fout("data.dat", ios::binary | ios::trunc);`

**Answer: B**

**Explanation:** Combining `ios::binary` (byte-level mode) with `ios::app` (append — preserve existing content, add new content at the end) via the bitwise OR operator gives exactly the described behavior. A alone would default to overwriting existing content, since `app` wasn't specified. C and D both include `ios::trunc`, which explicitly **deletes** existing content first — the opposite of what's being asked for.

---

#### **Q27.** Which statement about `fail()` is most accurate?

A) `fail()` only ever returns `true` if the file doesn't exist

B) `fail()` can catch a failure in *any* prior stream operation — open, read, or write — not just the initial opening step

C) `fail()` and `is_open()` are exact synonyms and can always be used interchangeably

D) `fail()` automatically closes the file if it detects an error

**Answer: B**

**Explanation:** `fail()` is the more general-purpose error check — while `is_open()` specifically confirms whether the file opened, `fail()` reflects the state of the **most recent** stream operation broadly, whether that was opening, reading, or writing. This makes it useful even deep into a sequence of operations, not just immediately after opening. It doesn't close files automatically (ruling out D), and while the two checks often overlap in simple cases, they aren't strictly interchangeable (ruling out C) — `is_open()` is the more precise tool right after an open call.

---

#### **Q28.** A student writes this code intending to read a file line by line, but gets a compiler error. What's wrong?

```cpp
ifstream fin("data.txt");
char line;

while (getline(fin, line)) {
    cout << line << endl;
}
```

A) `getline()` cannot be used with file streams, only with `cin`

B) `line` is declared as `char`, but `getline()` requires a `string` variable

C) The `while` loop condition is invalid syntax

D) `ifstream` cannot be used to read text files, only binary files

**Answer: B**

**Explanation:** `getline()` reads a full line of text and needs somewhere to store potentially many characters — that requires a `string`, not a single `char`. Declaring `line` as `char` (which can only ever hold one character) causes a type mismatch compiler error. A is false — `getline(fin, line)` using a file stream instead of `cin` is exactly the correct, standard pattern from the lecture. D is also false — `ifstream` is precisely the class used for reading text files throughout the lecture.

---

#### **Q29.** Which of the following is true regarding a `class`'s default access level, if it matters for whether `.write()`/`.read()` can access its members directly?

A) `.write()`/`.read()` require every member to be explicitly `public`, or they will fail to compile

B) `.write()`/`.read()` don't access individual members at all — they copy the object's entire raw memory block as-is, so access specifiers are irrelevant to them

C) `private` members are silently skipped and left as garbage after a binary read

D) You must write a `friend` function to allow `.write()`/`.read()` to access private members

**Answer: B**

**Explanation:** This is a subtler point worth understanding: `.write()` and `.read()` operate at the level of raw bytes starting at an address — they never individually "access" `roll`, `name`, or `marks` by name the way a getter function would. Since access specifiers (`public`/`private`) are a compile-time restriction on *named member access*, not on raw memory layout, they have no bearing on whether `.write()`/`.read()` can copy those bytes. This is why the `Student` class example worked fine with all its members `public` — but it would have worked identically even if they were `private`, since no named access is happening here at all.

---

#### **Q30.** What is the output of the following, assuming `student.dat` already contains one `Student` record `{101, "Amit", 85.5}` (28 bytes), written exactly as shown in the lecture?

```cpp
ifstream fin("student.dat", ios::binary);
Student s;
fin.seekg(0, ios::end);
cout << fin.tellg();
```

A) `0`

B) `28`

C) `101`

D) Undefined — `seekg` cannot be combined with `ios::end`

**Answer: B**

**Explanation:** `fin.seekg(0, ios::end);` moves the get pointer to **0 bytes offset from the end** of the file — i.e., directly to the very end. Since the file contains exactly one 28-byte `Student` record and nothing else, the end of the file sits at byte 28. `tellg()` then reports that position: `28`. This is actually a common, genuinely useful technique for finding a binary file's total size (bytes) without needing a separate function for it. D is false — `ios::end` is one of the three standard, fully valid reference points for `seekg`/`seekp`.

---

**Q31.** Which statement correctly distinguishes **sequential access** from **random access**?

A) Sequential access is only possible with text files; random access is only possible with binary files

B) Sequential access reads/writes data strictly in order from the start; random access can jump directly to any position without passing through what comes before it

C) Random access is simply a faster version of sequential access with no functional difference

D) Sequential access uses `seekg`/`seekp`; random access uses `.read()`/`.write()`

**Answer: B**

**Explanation:** This is the foundational distinction from the Gap Fill note. A is an oversimplification — sequential access with `.get()`/`getline()`/`.read()` (without seeking) works on both text and binary files; it's specifically *random access* (jumping to arbitrary positions) that requires binary's fixed-size records to be practical. D has the function associations completely backwards — `seekg`/`seekp` are exactly what *enable* random access; sequential access is what you get by simply not using them and letting the pointer advance naturally with each read/write.

---

#### **Q32.** In the following code, what happens on the second call to `fout.write(...)` inside the loop, in terms of *where* in the file it writes?

```cpp
ofstream fout("data.dat", ios::binary);
Student s;

for (int i = 0; i < 3; i++) {
    cin >> s.roll >> s.name >> s.marks;
    fout.write((char*)&s, sizeof(s));
}
```

A) It overwrites the same bytes as the first call, since `s` is reused

B) It writes to the position immediately following wherever the first call's bytes ended — the put pointer automatically advances after each write

C) It throws a runtime error, since `s` was already written once

D) It writes to a random position in the file

**Answer: B**

**Explanation:** Just as `.read()` automatically advances the get pointer forward after each successful read (Q23), `.write()` automatically advances the **put** pointer forward by however many bytes it just wrote. So even though the same variable `s` is reused and overwritten with new input each loop iteration, each `.write()` call lands at a fresh, sequentially-advancing position in the file — producing three back-to-back, non-overlapping records, exactly as demonstrated in the lecture's "writing multiple objects" example.

---

#### **Q33.** A file contains 5 fixed-size binary records of a `struct` where `sizeof(RecordType) == 40`. Which line correctly reads only the **last** record directly, without reading the first four?

A) `fin.seekg(4 * 40); fin.read((char*)&r, sizeof(r));`

B) `fin.seekg(5 * 40); fin.read((char*)&r, sizeof(r));`

C) `fin.seekg(-40, ios::end); fin.read((char*)&r, sizeof(r));`

D) Both A and C correctly read the last record

**Answer: D**

**Explanation:** The file has records at indices 0–4 (5 total), so the last record (index 4) starts at byte `4 × 40 = 160` — which is exactly what option A computes, measuring from the beginning (`ios::beg`, the default). Option C takes a different but equally valid route: starting from the **end** of the file and moving **back** by exactly one record's worth of bytes (`-40`) lands at that same byte 160, since the file's total size is `5 × 40 = 200` bytes, and `200 - 40 = 160`. Option B would overshoot — `5 × 40 = 200` is the position **just past** the last record (the end of file itself), not the start of the last record, and would cause the subsequent `.read()` to fail since there's nothing left to read from there.

---

## Full Practice Set — Final Recap

```
┌────────────────────────────────────────────────────────────────────┐
│           FILE HANDLING **PRACTICE SET** — COVERAGE MAP            │
├────────────────────────────────────────────────────────────────────┤
│  Q1–Q4    → headers, classes, file creation/writing basics         │
│  Q5–Q7    → text reading: character-by-character vs line-by-       │
│             line, whitespace behavior differences                  │
│  Q8–Q10   → file opening modes: app, trunc, combined read+write    │    
│  Q11–Q14  → binary files: raw bytes, .write() mechanics, cast,     │
│             sizeof calculation                                     │
│  Q15–Q17  → reading multiple binary records, the eof() trap,       │
│             is_open() vs other error checks                        │
│  Q18–Q23  → random access: seekg/seekp, reference points,          │
│             record-offset formula, tellg() pointer tracking        │
│  Q24, Q29 → struct vs class: identical binary mechanics,           │
│             access specifiers are irrelevant to raw byte copy      │
│  Q25–Q28  → combined mode flags, fail() vs is_open(), common       │
│             type-mismatch compiler error with getline()            │
│  Q30–Q33  → seekg with ios::end, sequential vs random access       │
│             distinction, put-pointer auto-advance, computing       │
│             an offset two different valid ways                     │
└────────────────────────────────────────────────────────────────────┘
```