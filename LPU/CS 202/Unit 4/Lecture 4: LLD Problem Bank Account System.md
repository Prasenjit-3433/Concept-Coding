# LLD Problem: Bank Account System

Date: October 29, 2026
Status: Done

## How We'll Work Through It

Real software engineers don't start by typing code. They go through these steps (the same flow used in LLD, low-level design):

```
┌─────────────────────────────────────────────────────────────────────┐
│  **Step 1**  →  Problem statement      (what are we building?)      │
│  **Step 2**  →  Requirements gathering (what must it do?)           │
│  **Step 3**  →  Find the classes       (nouns → classes)            │
│  **Step 4**  →  Design relationships   (is-a or has-a?)             │
│  **Step 5**  →  Decide access levels   (private / protected)        │
│  **Step 6**  →  Write the code                                      │
│  **Step 7**  →  Test with sample runs                               │
└─────────────────────────────────────────────────────────────────────┘
```

Follow the same steps for every problem. The code is the *last* thing we write.

---

## Step 1: Problem Statement

> A small bank offers **two kinds of accounts**: **`Savings`** and **`Current`**.
> 
> 
> Every account has an account number, a holder name and a balance. Every account can **deposit** money, **withdraw** money and **display** its details.
> 
> A **Savings** account earns **interest**, and must always keep a **minimum balance of 500**.
> 
> A **Current** account has no interest, but allows an **overdraft**: the balance may go *below zero*, up to a fixed limit.
> 
> Build a program that creates one account of each kind and performs some transactions.
> 

---

## Step 2: Requirement Gathering

Break the statement into small, checkable requirements.

### Functional Requirements (what the program does)

| # | Requirement |
| --- | --- |
| R1 | Every account stores *account number*, *holder name* and *balance*. |
| R2 | Every account can **deposit** a positive amount. |
| R3 | Every account can **withdraw** an amount. |
| R4 | Every account can **display** its details. |
| R5 | **Savings**: **cannot withdraw** if the balance would drop below the minimum balance (500). |
| R6 | **Savings**: can **add interest** based on an interest rate. |
| R7 | **Current**: can withdraw even beyond the balance, but **not beyond the overdraft limit**. |

### Assumptions (decisions we make to keep it simple)

- Money is stored as `double`.
- No file handling, no passwords, no multiple customers.
- Interest is calculated once, when we ask for it: `interest = balance × rate / 100`.
- Invalid amounts (zero or negative) are rejected with a message.

> **Why write assumptions?** The problem statement is never complete. Writing assumptions down shows what you decided, so nobody is surprised later.
> 

---

## Step 3: Finding the Classes

A simple trick: read the problem statement and **underline the *nouns*** (candidate classes or data) and **verbs** (candidate functions).

```
**Nouns**:  account, savings account, current account,
        account number, holder name, balance,
        interest, minimum balance, overdraft limit

**Verbs**:  deposit, withdraw, display, add interest
```

Now sort them:

```
┌───────────────────────────────────────────────────────────────────┐
│ **CLASSES**:   Account, SavingsAccount, CurrentAccount            │
│ **DATA**:      accountNumber, holderName, balance                 │
│            interestRate, minBalance, overdraftLimit               │
│ **FUNCTIONS**: deposit, withdraw, display, addInterest            │
└───────────────────────────────────────────────────────────────────┘
```

---

## Step 4: Design the Relationships

Apply the **is-a / has-a test** from Lecture 2 and Lecture 3 Part 3:

| Sentence | Verdict |
| --- | --- |
| A Savings account **is an** Account | ✅ Inheritance |
| A Current account **is an** Account | ✅ Inheritance |
| An Account **has a** balance | Just a data member |

So this is **hierarchical inheritance** (Lecture 2, Part 4): one base class with two children.

### What Goes Where?

The rule from inheritance is: **put common things in the parent, special things in the child.**

| Feature | Common to both? | Goes in |
| --- | --- | --- |
| accountNumber, holderName, balance | Yes | `Account` |
| deposit(), display() | Yes (same behavior) | `Account` |
| withdraw() | Both have it, but the **rules differ** | `Account` **and overridden** in each child |
| interestRate, addInterest() | Savings only | `SavingsAccount` |
| minBalance | Savings only | `SavingsAccount` |
| overdraftLimit | Current only | `CurrentAccount` |

### Quick recap of `UML`

![image.png](LLD%20Problem%20Bank%20Account%20System/image.png)

### Class Diagram

```
                ┌───────────────────────────────┐
                │           **Account**                │
                ├───────────────────────────────┤
                │ # accountNumber : int            │
                │ # holderName    : string         │
                │ # balance       : double         │
                ├───────────────────────────────┤
                │ + deposit(amount)                │
                │ + withdraw(amount)               │
                │ + display()                      │
                └───────────────────────────────┘
                  ▲                            ▲
                  │                            │
        ┌─────────┴───────────────┐      ┌─────┴────────────────────┐
        │   SavingsAccount        │      │      CurrentAccount      │
        ├─────────────────────────┤      ├──────────────────────────┤
        │ - interestRate          │      │ - overdraftLimit         │
        │ - minBalance            │      │                          │
        ├─────────────────────────┤      ├──────────────────────────┤
        │ + withdraw() (override) │      │ + withdraw() (override)  │
        │ + addInterest()         │      │ + display() (override)   │
        │ + display() (override)  │      │                          │
        └─────────────────────────┘      └──────────────────────────┘

   **Legend**:   #  protected      -  private      +  public
```

---

## Step 5: Decide Access Levels

This is where Lecture 2, Part 2 pays off.

`SavingsAccount::withdraw()` needs to **change `balance`**, which belongs to `Account`. So:

| Option | Result |
| --- | --- |
| `balance` is `private` | ❌ Child can't touch it. We'd need extra getter/setter functions. |
| `balance` is `public` | ❌ Anyone in `main()` could write `acc.balance = 1000000;`. Dangerous. |
| `balance` is **`protected`** | ✅ Children can use it, outside code cannot. |

```
   main() tries:    acc.balance = 1000000;   ❌ blocked (protected)
   SavingsAccount:  balance -= amount;       ✅ allowed (child)
```

So the common data goes under `protected`, and all the functions are `public`. The extra data of each child (`interestRate`, `minBalance`, `overdraftLimit`) isn't needed by anyone else, so it stays `private`.

> **Design principle:** give each piece of data the **strictest access** that still lets the program work. Money is sensitive, so outsiders must go through `deposit()` and `withdraw()`, where the rules are checked. (This is the data binding idea from Unit 2.)
> 

---

## Step 6: Write the Code

### The Base Class: `Account`

```cpp
#ifndef ACCOUNT_H
#define ACCOUNT_H

#include <iostream>
#include <string>
using namespace std;

class Account {
protected:
    int accountNumber;
    string holderName;
    double balance;

public:
    Account(int accNo, string name, double initialBalance)
        : accountNumber(accNo),
          holderName(name),
          balance(initialBalance) {
        cout << "Account created for " << holderName << endl;
    }

    void deposit(double amount) {
        if (amount <= 0) {
            cout << "Invalid deposit amount" << endl;
            return;
        }

        balance += amount;
        cout << "Deposited " << amount << endl;
    }

    virtual void withdraw(double amount) {
        if (amount <= 0 || amount > balance) {
            cout << "Withdrawal denied" << endl;
            return;
        }

        balance -= amount;
    }

    virtual void display() {
        cout << "Account No: " << accountNumber << endl;
        cout << "Holder: " << holderName << endl;
        cout << "Balance: " << balance << endl;
    }

    virtual ~Account() = default;
};

#endif
```

This is a general account: it denies a withdrawal if there isn't enough money. The children will **override** `withdraw()` with their own rules.

### Child 1: `SavingsAccount`

```cpp
#ifndef SAVINGS_ACCOUNT_H
#define SAVINGS_ACCOUNT_H

#include "Account.h"

class SavingsAccount : public Account {
private:
    double interestRate;
    double minBalance;

public:
    SavingsAccount(int accNo, string name,
                   double initialBalance, double rate)
        : Account(accNo, name, initialBalance),
          interestRate(rate), minBalance(500) {}

    void withdraw(double amount) override {
        if (amount <= 0 || balance - amount < minBalance) {
            cout << "Withdrawal denied" << endl;
            return;
        }

        balance -= amount;
    }

    void addInterest() {
        double interest = balance * interestRate / 100;
        balance += interest;
    }

    void display() override {
        Account::display();
        cout << "Type: Savings" << endl;
    }
};

#endif
```

### Child 2: `CurrentAccount`

```cpp
#ifndef CURRENT_ACCOUNT_H
#define CURRENT_ACCOUNT_H

#include "Account.h"

class CurrentAccount : public Account {
private:
    double overdraftLimit;

public:
    CurrentAccount(int accNo, string name,
                   double initialBalance, double limit)
        : Account(accNo, name, initialBalance),
          overdraftLimit(limit) {}

    void withdraw(double amount) override {
        if (amount <= 0 || balance - amount < -overdraftLimit) {
            cout << "Withdrawal denied" << endl;
            return;
        }

        balance -= amount;
    }

    void display() override {
        Account::display();
        cout << "Type: Current" << endl;
    }
};

#endif
```

### The `main()` Function

```cpp
#include <iostream>
#include "Account.h"
#include "SavingsAccount.h"
#include "CurrentAccount.h"

using namespace std;

int main() {

    SavingsAccount s(101, "Riya", 5000, 4);

    s.deposit(1000);
    s.withdraw(2000);
    s.addInterest();
    s.display();

    cout << "\n------------------\n";

    CurrentAccount c(201, "Aman", 2000, 1000);

    c.withdraw(2500);
    c.display();

    return 0;
}
```

*(Place the three classes above `main()` in one file. `#include` lines go at the very top.)*

[C++ Header Files, Header Guards & Multiple File Compilation](https://app.notion.com/p/C-Header-Files-Header-Guards-Multiple-File-Compilation-409cf4b5ee378063a743e17290e89577?pvs=21)

### Output

```
Account created for Riya
Savings account opened
Deposited 1000. New balance: 6000
Withdrawal denied: minimum balance of 500 must remain
Withdrawn 2000. New balance: 4000
Interest added: 160
Account No: 101
Holder: Riya
Balance: 4160
Type: Savings (interest rate 4%)
Account created for Aman
Current account opened
Withdrawn 2500. New balance: -500
Withdrawal denied: overdraft limit of 1000 exceeded
Account No: 201
Holder: Aman
Balance: -500
Type: Current (overdraft limit 1000)
```

---

## Step 7: Testing, Tracing the Code

### Savings Account (`balance` starts at 5000, minimum 500, rate 4%)

| Call | Calculation | Result |
| --- | --- | --- |
| `deposit(1000)` | 5000 + 1000 | balance = **6000** |
| `withdraw(5800)` | 6000 − 5800 = 200, and 200 < 500 | ❌ **denied** |
| `withdraw(2000)` | 6000 − 2000 = 4000, and 4000 ≥ 500 | balance = **4000** |
| `addInterest()` | 4000 × 4 / 100 = 160 | balance = **4160** |

### Current Account (`balance` starts at 2000, limit 1000)

| Call | Calculation | Result |
| --- | --- | --- |
| `withdraw(2500)` | 2000 − 2500 = −500, and −500 ≥ −1000 | balance = **−500** |
| `withdraw(700)` | −500 − 700 = −1200, and −1200 < −1000 | ❌ **denied** |

### How Construction Worked (Lecture 2, Part 3)

```
SavingsAccount s(101, "Riya", 5000, 4);
   ↓
SavingsAccount constructor receives 4 values
   ↓
: Account(accNo, name, initialBalance)    ← forwards 3 values UP
   ↓
① Account's constructor runs      → prints "Account created for Riya"
② SavingsAccount's body runs      → prints "Savings account opened"
```

### How the Compiler Picked the Right `withdraw()` (Lecture 3, Part 1)

```
s.withdraw(5800);
   ↓
Look in SavingsAccount first → found → use it   (minimum-balance rule)

c.withdraw(2500);
   ↓
Look in CurrentAccount first → found → use it   (overdraft rule)
```

`Account::withdraw()` exists, but each child's own version is found first, so the parent's version is never used for these objects.

### Test Cases a Good Programmer Would Add

Edge cases catch the most bugs. Try these yourself:

| Test | Expected |
| --- | --- |
| `deposit(0)` or `deposit(-50)` | "Invalid deposit amount" |
| Savings: withdraw exactly down to 500 (e.g. balance 1500, withdraw 1000) | ✅ allowed (since 500 is not less than 500) |
| Current: withdraw exactly to −1000 | ✅ allowed |
| Savings: `addInterest()` when balance is 0 | interest 0, no crash |

---

## Reflection: What Did We Practice?

| Concept | Where it appeared |
| --- | --- |
| Hierarchical inheritance | `Account` → `SavingsAccount` and `CurrentAccount` |
| `protected` members | `balance` shared with children but hidden from `main()` |
| Constructor chaining | `: Account(accNo, name, initialBalance)` |
| Overriding | `withdraw()` and `display()` in both children |
| Calling the base version | `Account::display()` |
| Encapsulation | Outsiders can only use `deposit()` / `withdraw()` |

---

## Extension Exercises (Try on Your Own)

Practice by changing the design and the code together:

1. **Easy:** Add a `getBalance()` function to `Account` and print it from `main()`.
2. **Easy:** Add a `FixedDepositAccount` child that **doesn't allow any withdrawal** at all. Override `withdraw()` to print "Not allowed before maturity."
3. **Medium:** Make the minimum balance a **constructor parameter** instead of fixing it at 500.
4. **Medium:** Add a transaction counter in `Account` that counts how many deposits and withdrawals succeeded, and show it in `display()`.
5. **Thinking question:** What if a bug in `Account::deposit()` is fixed once? Do both children get the fix? (Yes. That's the code reusability benefit from Lecture 2.)

---

## Common Mistakes to Avoid

| Mistake | What goes wrong |
| --- | --- |
| Making `balance` `public` "to keep it easy" | Any code can corrupt the money. Use `protected`. |
| Making `balance` `private` in `Account` | The child's `withdraw()` can't change it. Compile error. |
| Copying `accountNumber` and `holderName` into each child | Defeats the purpose of inheritance. |
| Forgetting `: Account(...)` in a child constructor | Compile error, since `Account` has no default constructor. |
| Calling `display()` inside `display()` without `Account::` | Infinite recursion. |
| Checking `balance - amount <= minBalance` | Off by one: a balance of exactly 500 should be allowed. |
| Writing all the logic first and designing later | Hard to fix. Design the classes first. |

---

## Key Points to Remember

- Follow the LLD flow: **`problem` → `requirements` → `find classes` → `relationships` → `access levels` → `code` → `test`**.
- Underline **nouns** (classes and data) and **verbs** (functions) in the problem statement to find your classes.
- Use the **is-a** test to confirm inheritance. Here, Savings and Current are both Accounts: **hierarchical inheritance**.
- Common data and behavior go in the **parent**. Special data and behavior go in the **children**.
- Give data the **strictest access** that still works: `protected` for what children need, `private` for the rest.
- When children follow different rules for the same action, **override** that function (`withdraw()`).
- Reuse parent logic with `Parent::function()`, and pass shared values up with the constructor's colon.
- Always test **edge cases**, not just the normal case.

---