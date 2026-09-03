# Unit II – Class 3: Pointers and Arrays

Status: Pending

*(Pointers, Reference Variables, Arrays and String Concepts — CS202)*

---

## Quick Recap Before We Begin

In Class 2, we saw that pointer arithmetic moves in "jumps" the size of the data type, and that `*(p + 3)` lets us reach the 4th element from wherever `p` points. Today, we connect this directly to **arrays** — and you'll discover that arrays and pointers are far more related than they first appear.

---

## 1. The Big Reveal: An Array Name IS (Almost) a Pointer

When you declare an array, its name actually represents the **address of its first element**.

```cpp
int arr[5] = {10, 20, 30, 40, 50};

cout << arr;       // prints address of arr[0], e.g. 0x1000
cout << &arr[0];   // prints the SAME address, 0x1000
```

**Real-life analogy:** Think of an array as a **row of identical lockers in a school hallway**, all numbered in sequence and right next to each other. The array's name (`arr`) is like the **hallway's entrance sign** — it doesn't refer to *all* the lockers at once, it simply tells you **where the first locker is**. Once you know where locker #1 is, and you know every locker is the same size, you can find any locker just by counting forward.

```
Array in memory:

Address:   0x1000    0x1004    0x1008    0x100C    0x1010
          ┌────────┬────────┬────────┬────────┬────────┐
Value:    │   10   │   20   │   30   │   40   │   50   │
          └────────┴────────┴────────┴────────┴────────┘
 arr →       arr[0]   arr[1]   arr[2]   arr[3]   arr[4]
(= address of arr[0])
```

So this is completely valid:

```cpp
int* p = arr;    // no '&' needed! arr already IS an address
```

⚠️ **Important distinction:** Even though `arr` behaves like a pointer in most situations, it is **not exactly the same** as a pointer variable — `arr` is a fixed address that can't be reassigned (`arr = arr + 1;` is illegal), whereas a pointer variable like `p` can be changed to point anywhere. Think of `arr` as a signboard **permanently bolted** to the entrance, while `p` is a signboard you're **holding in your hand** and can carry anywhere.

---

## 2. Accessing Array Elements via Pointers

Since `arr` is really the address of `arr[0]`, and pointer arithmetic moves element-by-element, we can access every array element in **two equivalent ways**:

```cpp
int arr[5] = {10, 20, 30, 40, 50};

cout << arr[2];       // Method 1: normal indexing → 30
cout << *(arr + 2);   // Method 2: pointer arithmetic → 30
```

**In fact, `arr[i]` is just syntactic sugar (a shortcut) for `*(arr + i)` — the compiler literally converts one into the other internally.**

```
arr[2]     is compiled as     *(arr + 2)
     ↑                              ↑
"give me element              "move 2 slots from arr's
 at index 2"                   start, then look inside"
```

**Real-life analogy:** Asking for `arr[2]` is like telling a hallway assistant "give me what's in locker #2 (counting from 0)." Asking for `*(arr + 2)` is like saying "walk 2 lockers down from the entrance, then open whichever locker you're standing at." Both instructions land you at the **exact same locker** — just phrased differently.

This also means indexing works **both ways** with pointers:

```cpp
int arr[5] = {10, 20, 30, 40, 50};
int* p = arr;

cout << p[2];        // 30 — yes, pointers can use [] too!
cout << *(p + 2);    // 30 — same thing
```

---

## 3. Traversing an Array Using a Pointer

Instead of using an index variable `i`, you can walk through an array by **moving the pointer itself**.

**Traditional way (using index):**

```cpp
int arr[5] = {10, 20, 30, 40, 50};

for (int i = 0; i < 5; i++) {
    cout << arr[i] << " ";
}
```

**Pointer way (moving the pointer forward):**

```cpp
int arr[5] = {10, 20, 30, 40, 50};
int* p = arr;

for (int i = 0; i < 5; i++) {
    cout << *p << " ";   // print value at current position
    p++;                    // move pointer to next element
}
```

**Real-life analogy:** The index method is like standing still at the hallway entrance and **shouting out a locker number** each time ("Give me locker 0! Now locker 1! Now locker 2!"). The pointer method is like **physically walking down the hallway yourself**, one locker at a time, opening whichever locker you're currently standing in front of. Both approaches visit the same lockers in the same order — just different styles of movement.

```
Step 0: p → arr[0]=10   print 10, then p++
Step 1: p → arr[1]=20   print 20, then p++
Step 2: p → arr[2]=30   print 30, then p++
Step 3: p → arr[3]=40   print 40, then p++
Step 4: p → arr[4]=50   print 50, then p++
```

⚠️ Since `arr` itself can't be moved, always copy it into a separate pointer variable (`p`) before incrementing — that's exactly why we wrote `int* p = arr;` first.

---

## 4. Passing Arrays to Functions (A Direct Consequence of This Relationship)

Because an array's name is really just an address, when you pass an array to a function, you're actually passing a **pointer** — not a copy of the whole array.

```cpp
void printArray(int* arr, int size) {
    for (int i = 0; i < size; i++) {
        cout << arr[i] << " ";
    }
}

int main() {
    int nums[5] = {10, 20, 30, 40, 50};
    printArray(nums, 5);   // 'nums' decays into a pointer automatically
}
```

This can also be written with array-style syntax in the parameter — both mean exactly the same thing to the compiler:

```cpp
void printArray(int arr[], int size) {   // identical to int* arr
    ...
}
```

**Real-life analogy:** When you pass an array to a function, it's like giving someone **directions to the hallway entrance**, instead of physically moving all the lockers into a new building. The function doesn't get its own separate copy of the lockers — it gets the same hallway you already had, and can look at (or even rearrange) the very same lockers you had.

⚠️ **Big consequence:** Because the function receives the *same* memory (not a copy), any changes made to array elements **inside the function will affect the original array**.

```cpp
void doubleValues(int arr[], int size) {
    for (int i = 0; i < size; i++) {
        arr[i] = arr[i] * 2;   // modifies the ORIGINAL array
    }
}

int main() {
    int nums[3] = {1, 2, 3};
    doubleValues(nums, 3);
    cout << nums[0];   // prints 2 — original array changed!
}
```

This is different from what we saw in Unit I, where passing a normal `int` by value gave the function only a **copy**. Arrays behave differently — they "decay" into pointers, so the function always works with the original data.

⚠️ **Another consequence:** Because only the address is passed (not the actual array), `sizeof()` **won't work correctly** inside the function to find array length — this is exactly why we manually pass `size` as a separate parameter.

```cpp
void printArray(int arr[], int size) {
    cout << sizeof(arr);   // prints 8 (just the pointer's size!) — NOT the array's size
}
```

---

## 5. Array of Pointers

Just as you can have an array of `int`s or `char`s, you can have an array where **each slot itself is a pointer**.

```cpp
int a = 10, b = 20, c = 30;
int* arr[3] = {&a, &b, &c};   // array of 3 int-pointers

cout << *arr[0];   // 10  (dereference the pointer stored at index 0)
cout << *arr[1];   // 20
cout << *arr[2];   // 30
```

**Real-life analogy:** Imagine a **key rack on a wall with 3 hooks**. Each hook doesn't hold an actual room — it holds a **key** that opens a specific room somewhere else in the building. `arr[0]` is the key on hook 0; `*arr[0]` means "use that key to actually open the room and see what's inside."

```
arr (array of pointers)          Actual variables elsewhere in memory
┌────────────┐
│  arr[0]    │──────────► a = 10
├────────────┤
│  arr[1]    │──────────► b = 20
├────────────┤
│  arr[2]    │──────────► c = 30
└────────────┘
```

This is especially useful for arrays of strings, since (as we'll see in Class 7) a C-style string is itself represented via a `char*` — so an "array of strings" is really an **array of char pointers**:

```cpp
const char* names[3] = {"Aman", "Riya", "Karan"};
cout << names[1];   // prints "Riya"
```

---

## 6. Recap Table

| Concept | Meaning | Real-life analogy |
| --- | --- | --- |
| `arr` (array name) | address of the first element | hallway entrance sign, permanently fixed |
| `arr[i]` | element at index `i` | asking for locker `i` by number |
| `*(arr + i)` | same as `arr[i]` | walking `i` lockers down, then opening it |
| passing array to function | passes a pointer, not a copy | giving directions to the same hallway, not building a new one |
| array of pointers | each slot holds an address, not a direct value | a key rack, where each hook holds a key to a different room |

---

## 7. Practice Questions for Students

1. Given `int arr[4] = {2, 4, 6, 8};`, what does `(arr + 3)` print? What about `arr[3]`?
2. Why does this code print the **modified** array instead of the original?

```cpp
void setZero(int arr[], int size) {
    arr[0] = 0;
}
int main() {
    int nums[3] = {5, 5, 5};
    setZero(nums, 3);
    cout << nums[0];   // what prints, and why?
}
```

1. Explain why `sizeof(arr)` gives a different answer inside a function compared to inside `main()`, for the same array.
2. Create an array of 3 `int*` pointers, each pointing to a separate `int` variable, and print all 3 values using dereferencing.

---

That completes all **3 classes** you asked for (Class 1: Introduction to Pointers, Class 2: Pointer Arithmetic & Types, Class 3: Pointers and Arrays). Since your original plan had Class 3 as "Pointers and Arrays" and you're teaching live from here — want me to keep going with Class 4 (Reference Variables) next, or pause here for now?