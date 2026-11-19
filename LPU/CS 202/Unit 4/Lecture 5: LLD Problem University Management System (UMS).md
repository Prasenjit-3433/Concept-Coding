# LLD Problem: University Management System (UMS)

Date: October 29, 2026
Status: Done

# 🎯 Part 5: Practice Problem 2, University Management System

*(A quick correction first: at the end of Part 4 I said `Person → Student` and `Person → Faculty` would be "multilevel". That shape is actually **hierarchical** (one parent, two children). This design adds a real multilevel chain, `Person → Student → PGStudent`, so it uses all three ideas.)*

We follow the same LLD flow as Problem 1.

---

## Step 1: Problem Statement

> A university keeps records of **students** and **faculty**. Every person has a name and an age.
> 
> 
> A **student** also has a roll number and a **home address** (city and pincode). A **PG student** is a student who also has a **thesis title**.
> 
> A **faculty** member has a department.
> 
> A **course** has a title, is taught by **one faculty member**, and has up to **3 enrolled students**. Faculty and students exist in the university on their own. They don't disappear when a course is closed.
> 
> Build a program that creates people, creates a course, enrolls students, and displays the course.
> 

---

## Step 2: Requirements

| # | Requirement |
| --- | --- |
| R1 | Every person stores name and age. |
| R2 | A student adds roll number and address. |
| R3 | A PG student adds a thesis title. |
| R4 | A faculty member adds a department. |
| R5 | A course stores its title, one instructor, and up to 3 students. |
| R6 | A course can enroll a student (rejecting the 4th). |
| R7 | Everything can display its details. |

**Assumptions:** no file handling, no grades, and a fixed array of 3 student slots.

---

## Step 3: Find the Classes

```
Nouns:  person, student, PG student, faculty, course, address,
        name, age, roll number, thesis title, department, city, pincode
Verbs:  enroll, display
```

```
CLASSES: Person, Student, PGStudent, Faculty, Course, Address
```

---

## Step 4: Design the Relationships

Test each pair with the **is-a / has-a** idea:

| Sentence | Relationship |
| --- | --- |
| A Student **is a** Person | Inheritance |
| A Faculty **is a** Person | Inheritance |
| A PGStudent **is a** Student | Inheritance |
| A Student **has an** Address (belongs only to that student) | **Composition** |
| A Course **has a** Faculty (exists independently) | **Aggregation** |
| A Course **has** Students (exist independently) | **Aggregation** |

The quick test from Part 3 settles the last four. Delete the student, and the address has no meaning, so that is composition. Delete the course, and the faculty and students still exist, so that is aggregation.

```
                    ┌──────────┐
                    │  Person  │
                    └──────────┘
                     ▲        ▲
                     │        │                 hierarchical
              ┌─────────┐  ┌─────────┐
              │ Student │  │ Faculty │
              └─────────┘  └─────────┘
                   ▲   ◆─────▶ Address           composition
             ┌───────────┐
             │ PGStudent │                       multilevel
             └───────────┘

      ┌────────┐ ◇─────▶ Faculty                 aggregation
      │ Course │ ◇─────▶ Student (up to 3)
      └────────┘
```

---

## Step 5: Access Levels

| Data | Access | Reason |
| --- | --- | --- |
| `name`, `age` in `Person` | `protected` | Children's `display()` may need them, `main()` should not change them freely |
| Other class data | `private` | Nobody else needs it |
| Functions | `public` | They are the way in |

`Person` offers a public `getName()`, so `Course` can print the instructor's name without touching the data.

---

## Step 6: The Code

```cpp
#include <iostream>
#include <string>
using namespace std;

// ---------- COMPOSITION PART ----------
class Address {
    string city;
    int pincode;
public:
    Address(string c, int p) : city(c), pincode(p) { }
    void display() {
        cout << "Address: " << city << " - " << pincode << endl;
    }
};

// ---------- BASE CLASS ----------
class Person {
protected:
    string name;
    int age;
public:
    Person(string n, int a) : name(n), age(a) { }
    string getName() { return name; }
    void display() {
        cout << "Name: " << name << endl;
        cout << "Age: " << age << endl;
    }
};

// ---------- LEVEL 2: Student (has-an Address) ----------
class Student : public Person {
    int rollNo;
    Address address;                      // composition: member object
public:
    Student(string n, int a, int roll, string city, int pin)
        : Person(n, a), rollNo(roll), address(city, pin) { }

    void display() {
        Person::display();                // reuse parent's version
        cout << "Roll No: " << rollNo << endl;
        address.display();
    }
};

// ---------- LEVEL 3: PGStudent (multilevel) ----------
class PGStudent : public Student {
    string thesisTitle;
public:
    PGStudent(string n, int a, int roll, string city, int pin, string thesis)
        : Student(n, a, roll, city, pin), thesisTitle(thesis) { }

    void display() {
        Student::display();
        cout << "Thesis: " << thesisTitle << endl;
    }
};

// ---------- Faculty (hierarchical sibling of Student) ----------
class Faculty : public Person {
    string department;
public:
    Faculty(string n, int a, string dept) : Person(n, a), department(dept) { }

    void display() {
        Person::display();
        cout << "Department: " << department << endl;
    }
};

// ---------- AGGREGATION: Course uses Faculty and Students ----------
class Course {
    string title;
    Faculty* instructor;                  // pointers: Course does NOT own these
    Student* students[3];
    int count;
public:
    Course(string t, Faculty* f) : title(t), instructor(f), count(0) { }

    ~Course() {
        cout << "Course destroyed" << endl;   // no delete: we don't own them
    }

    void enroll(Student* s) {
        if (count >= 3) {
            cout << "Course full: cannot enroll " << s->getName() << endl;
            return;
        }
        students[count++] = s;
    }

    void display() {
        cout << "Course: " << title << endl;
        cout << "Instructor: " << instructor->getName() << endl;
        cout << "Enrolled students: " << count << endl;
        for (int i = 0; i < count; i++) {
            cout << "---" << endl;
            students[i]->display();
        }
        cout << "---" << endl;
    }
};

int main() {
    Faculty f("Dr. Mehta", 45, "CSE");
    Student s1("Riya", 19, 101, "Jalandhar", 144001);
    PGStudent p1("Aman", 24, 201, "Phagwara", 144411, "Smart Traffic Systems");

    {
        Course c("C++ Programming", &f);
        c.enroll(&s1);
        c.enroll(&p1);
        c.display();
    }   // course destroyed here

    cout << "\nCourse closed, but people still exist:" << endl;
    p1.display();
    return 0;
}
```

### Output

```
Course: C++ Programming
Instructor: Dr. Mehta
Enrolled students: 2
---
Name: Riya
Age: 19
Roll No: 101
Address: Jalandhar - 144001
---
Name: Aman
Age: 24
Roll No: 201
Address: Phagwara - 144411
---
Course destroyed

Course closed, but people still exist:
Name: Aman
Age: 24
Roll No: 201
Address: Phagwara - 144411
Thesis: Smart Traffic Systems
```

---

## Step 7: Tracing and Testing

### Construction of `p1` (the multilevel chain)

```
PGStudent p1("Aman", 24, 201, "Phagwara", 144411, "Smart Traffic Systems");
   │
   ├─▶ passes 5 values ─▶ Student constructor
   │                         ├─▶ passes "Aman", 24 ─▶ Person constructor
   │                         ├─▶ rollNo = 201
   │                         └─▶ address("Phagwara", 144411)   ← composition,
   │                                                              via initializer list
   └─▶ keeps thesisTitle
```

Order: `Person` → `Student`'s members (`address`) → `Student` body → `PGStudent`. Each constructor talks only to its **direct parent**, as in Lecture 2, Part 3.

### Aggregation in Action

```
Course c(..., &f);       → c stores f's ADDRESS only
c.enroll(&s1);           → c stores s1's ADDRESS only
} closes                 → ~Course() runs, deletes nothing
p1.display();            → ✅ still works: p1 outlived the course
```

### ⚠️ A Surprise Worth Noticing

Look at Aman in the **course listing**. The `Thesis:` line is **missing**, yet it appears when we call `p1.display()` directly.

```
students[i]->display();     // students[i] is a  Student*  pointing to a PGStudent
```

The pointer's type is `Student*`, so C++ calls `Student::display()` and never looks at `PGStudent::display()`. This is the compile-time overriding limit from Lecture 3, Part 1. Making C++ pick the derived version at run time needs **`virtual` functions**, the next topic (Polymorphism). Nothing is wrong with your understanding. This is the exact problem polymorphism solves.

### Tests to Try

| Test | Expected |
| --- | --- |
| Enroll 4 students | 4th prints "Course full: cannot enroll …" |
| Display a course with 0 students | "Enrolled students: 0" |
| Change `Course` destructor to `delete instructor;` | ❌ Bug: it would destroy `f`, which `main()` still owns |

---

## Reflection: What Did We Practice?

| Concept | Where it appeared |
| --- | --- |
| Hierarchical inheritance | `Student` and `Faculty` both from `Person` |
| Multilevel inheritance | `Person → Student → PGStudent` |
| Constructor chaining | `: Person(n, a)`, `: Student(...)` |
| Composition | `Address address;` inside `Student` |
| Aggregation | `Faculty*` and `Student*` inside `Course` |
| Overriding and `Parent::display()` | `display()` in each class |
| Static binding limit | The missing thesis line |

---

## Extension Exercises

1. **Easy:** Add `getRollNo()` to `Student` and print it from `main()`.
2. **Easy:** Add a `Teaching Assistant`style check: what stops `PGStudent` from being built without a thesis title?
3. **Medium:** Add a `removeStudent(int rollNo)` to `Course`. (Hint: you will need `Student` to expose its roll number.)
4. **Medium:** Let a `Faculty` keep a list of up to 2 `Course` pointers it teaches (aggregation in the other direction).
5. **Thinking:** Why is `Address` a member object, while `Faculty` in `Course` is a pointer? (Answer with the ownership test.)

---

## Common Mistakes to Avoid

| Mistake | What goes wrong |
| --- | --- |
| Making `Address` a base class of `Student` | A student is not an address. Use composition. |
| Using `delete` on pointers inside `~Course()` | Destroys objects the course doesn't own. |
| Creating `Student` objects inside `Course` | That would make the student die with the course, which is composition, not what we want. |
| Forgetting `address(city, pin)` in the initializer list | Compile error, because `Address` has no default constructor. |
| Expecting `students[i]->display()` to show the thesis | Needs `virtual` (next topic). |

---

## Key Points to Remember

- **Is-a** → inheritance. **Owns** → composition (member object). **Uses an independent object** → aggregation (pointer).
- Hierarchical and multilevel inheritance combine naturally in one design.
- Member objects like `address` are initialized with the **initializer list**.
- An aggregating class must **not delete** what it doesn't own.
- Through a base-class pointer, the **base** version of an overridden function runs unless the function is `virtual`.

---

# 📋 Recap

```
┌──────────────────────────────────────────────────────────────────────────┐
│          INHERITANCE GAP FILL + PRACTICE PROBLEMS                        │
├──────────────────────────────────────────────────────────────────────────┤
│  **Overriding**: same name + parameters in derived; nearest wins;        │
│              reach base with Base::fn()                                  │
│  **Ambiguity**:  two parents, same name → obj.A::fn() or override        │
│  **Virtual base**: class Employee : virtual public Human                 │
│              → one shared copy; most derived class builds it             │
│  **Composition**: whole owns part, member object, dies together          │
│  **Aggregation**: whole uses part, pointer, part outlives whole          │
│  **LLD flow**: problem → requirements → classes → relationships          │
│            → access levels → code → test                                 │
└──────────────────────────────────────────────────────────────────────────┘
```