# Step 1: Prerequisites & Why Scopes Matter

---

## What Do You Need to Know Before This?

The instructor is very clear about this — **you must understand the Bean Lifecycle before studying scopes.**

Why? Because scopes are all about **when an object gets created and how long it lives**. If you don't know the lifecycle, you can't visualize what's happening under the hood.

Here's the bean lifecycle flow the instructor refers to:

```
┌─────────────────────────────────────────────────────────────┐
│                     BEAN LIFECYCLE                          │
│                                                             │
│  1. IOC Container Starts                                    │
│          │                                                  │
│          ▼                                                  │
│  2. Bean is Instantiated (Constructor is called)            │
│          │                                                  │
│          ▼                                                  │
│  3. Dependencies are Injected (@Autowired)                  │
│          │                                                  │
│          ▼                                                  │
│  4. @PostConstruct is called (bean is fully ready)          │
│          │                                                  │
│          ▼                                                  │
│  5. Bean is used by the application                         │
│          │                                                  │
│          ▼                                                  │
│  6. Bean is destroyed when IOC shuts down                   │
└─────────────────────────────────────────────────────────────┘
```

---

## So What Exactly is a Scope?

Think of it this way —

When Spring manages a class as a bean, it has to decide: **how many objects of this class should exist, and when should they be created?**

That decision is controlled by **scope**.

---

## The 5 Types of Scopes in Spring Boot

```
┌──────────────────────────────────────────────────────────────────┐
│                        SPRING BEAN SCOPES                        │
├────────────────┬─────────────────────────────────────────────────┤
│   Scope        │   Meaning (in plain English)                    │
├────────────────┼─────────────────────────────────────────────────┤
│  Singleton     │  One object per IOC container                   │
│  Prototype     │  New object every single time it is needed      │
│  Request       │  New object for each HTTP request               │
│  Session       │  New object for each HTTP session               │
│  Application   │  One object shared across multiple IOC          │
│                │  containers (very rarely used)                  │
└────────────────┴─────────────────────────────────────────────────┘
```

---

## Eager vs Lazy Initialization — The Key Idea

This concept runs through ALL the scopes, so understand it here once clearly.

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│   EAGER INITIALIZATION                                          │
│   → Object is created at application startup itself             │
│   → Spring doesn't wait for someone to "use" the bean           │
│   → Singleton is eagerly initialized                            │
│                                                                 │
│   LAZY INITIALIZATION                                           │
│   → Object is created only when it is actually needed           │
│   → Spring waits until someone requests/uses that bean          │
│   → Prototype, Request, Session are lazily initialized          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

That's the foundation. Now we're ready to go scope by scope.

# Step 2: Singleton Scope

---

## What is Singleton?

Singleton is the **default scope** in Spring Boot. If you don't write any scope on your class, Spring automatically treats it as Singleton.

**One rule: Only ONE object of this class is created per IOC container.**

You can think of it as — no matter how many places in your application need this class, they all share the **same single object**.

---

## How to Define Singleton

The instructor shows three ways — all mean exactly the same thing:

```java
// Way 1 — Don't write anything at all (default is singleton)
@RestController
public class TestController1 {
}

// Way 2 — Using the enum
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
@RestController
public class TestController2 {
}

// Way 3 — Using a plain string
@Scope("singleton")
@Component
public class User {
}
```

All three are identical in behavior. The instructor says use whichever you're comfortable with.

---

## The Full Walkthrough — What Happens When You Start the App?

The instructor walks through this with three classes:
- `TestController1` → Singleton, depends on `User`
- `TestController2` → Singleton, depends on `User`
- `User` → Singleton, no dependencies

```
┌──────────────────────────────────────────────────────────────────────┐
│                     APPLICATION STARTUP                              │
│                  (IOC Container Starts)                              │
│                                                                      │
│  STEP 1: IOC scans and finds TestController1                         │
│          → It's Singleton → Eagerly initialize it                    │
│          → Calls constructor                                         │
│          → Prints: "TestController1 instance initialization"         │
│          → But wait! It has a dependency → User                      │
│                                                                      │
│  STEP 2: IOC now looks at User                                       │
│          → It's Singleton → Create its object                        │
│          → Calls constructor                                         │
│          → Prints: "User initialization"                             │
│          → User has NO dependency                                    │
│          → @PostConstruct runs                                       │
│          → Prints: "User object hashCode: 1140202235"                │
│                                                                      │
│  STEP 3: User object (1140202235) is injected into TestController1   │
│          → No more dependencies                                      │
│          → @PostConstruct of TestController1 runs                    │
│          → Prints: "TestController1 hashCode: 1046302571"            │
│          → Prints: "User object hashCode: 1140202235"  ← same user   │
│                                                                      │
│  STEP 4: IOC now finds TestController2                               │
│          → It's Singleton → Eagerly initialize it                    │
│          → Calls constructor                                         │
│          → Prints: "TestController2 instance initialization"         │
│          → It also depends on User                                   │
│          → User is Singleton → already created → REUSE same object   │
│          → Same User object (1140202235) injected here too           │
│          → @PostConstruct runs                                       │
│          → Prints: "TestController2 hashCode: 1525241687"            │
│          → Prints: "User object hashCode: 1140202235"  ← same user   │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## The Key Observation from the Logs

```
TestController1 instance initialization
User initialization
User object hashCode:      1140202235   ← created once
TestController1 hashCode:  1046302571
  └─ User object hashCode: 1140202235   ← same object

TestController2 instance initialization
TestController2 hashCode:  1525241687
  └─ User object hashCode: 1140202235   ← same object again!
```

Both `TestController1` and `TestController2` are sharing the **exact same User object**. That's Singleton in action.

---

## What Happens When You Hit an API?

Say you call `GET /fetchUser` after the app has started.

```
┌────────────────────────────────────────────────┐
│         API CALL → GET /fetchUser              │
│                                                │
│  NO new object is created.                     │
│  All objects were already created at startup.  │
│  Spring simply invokes the API method.         │
│  That's it.                                    │
└────────────────────────────────────────────────┘
```

This is the benefit of eager initialization — by the time a request comes in, everything is already ready.

---

## Summary of Singleton

```
┌──────────────────────────────────────────────────┐
│                   SINGLETON                      │
├──────────────────────────────────────────────────┤
│  Default scope — no annotation needed            │
│  One object per IOC container                    │
│  Eagerly initialized at application startup      │
│  Same object shared across the entire app        │
│  No new object created on API calls              │
└──────────────────────────────────────────────────┘
```

---

# Step 3: Prototype Scope

---

## What is Prototype?

Simple rule: **Every single time a bean is needed, a brand new object is created.**

It doesn't matter if an object of that class already exists somewhere. Prototype always creates a fresh one.

And unlike Singleton, **Prototype is lazily initialized** — meaning Spring does NOT create its object at application startup. It waits until the object is actually needed.

---

## How to Define Prototype

Again, the instructor shows two ways:

```java
// Way 1 — Using the enum
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class User {
}

// Way 2 — Using a plain string
@Scope("prototype")
@Component
public class User {
}
```

Both are identical. Use whichever feels natural.

---

## The Setup — Three Classes

The instructor uses these three classes to walk through the example:

```java
// TestController1 — Prototype, depends on User and Student
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RestController
public class TestController1 {
    @Autowired
    User user;

    @Autowired
    Student student;
}

// Student — Singleton, depends on User
@Component
public class Student {
    @Autowired
    User user;
}

// User — Prototype, no dependencies
@Scope("prototype")
@Component
public class User {
}
```

So the dependency map looks like this:

```
┌─────────────────────────────────────────┐
│         DEPENDENCY MAP                  │
│                                         │
│  TestController1 (Prototype)            │
│       ├──→ User     (Prototype)         │
│       └──→ Student  (Singleton)         │
│                 └──→ User  (Prototype)  │
│                                         │
└─────────────────────────────────────────┘
```

---

## What Happens at Application Startup?

```
┌──────────────────────────────────────────────────────────────────────┐
│                      APPLICATION STARTUP                             │
│                                                                      │
│  IOC scans and finds TestController1                                 │
│  → Scope is Prototype → Lazily initialized → SKIP, do nothing        │
│                                                                      │
│  IOC finds User                                                      │
│  → Scope is Prototype → Lazily initialized → SKIP, do nothing        │
│                                                                      │
│  IOC finds Student                                                   │
│  → Scope is Singleton → Eagerly initialized → CREATE its object      │
│  → Calls constructor                                                 │
│  → Prints: "Student instance initialization"                         │
│  → Student depends on User                                           │
│  → User is Prototype but NOW it is actually needed                   │
│  → So User object gets created here                                  │
│  → Prints: "User initialization"                                     │
│  → User has no dependency → @PostConstruct runs                      │
│  → Prints: "User object hashCode: 1510009630"                        │
│  → This User object is injected into Student                         │
│  → Student's @PostConstruct runs                                     │
│  → Prints: "Student hashCode: 2092450685"                            │
│  → Prints: "User object hashCode: 1510009630"                        │
│                                                                      │
│  App starts. No object created for TestController1 or standalone User│
└──────────────────────────────────────────────────────────────────────┘
```

---

## What Happens When You Hit the API — First Call?

Now you call `GET /fetchUser` for the first time:

```
┌──────────────────────────────────────────────────────────────────────┐
│                   API CALL 1 → GET /fetchUser                        │
│                                                                      │
│  TestController1 is Prototype                                        │
│  → New object must be created                                        │
│  → Prints: "TestController1 instance initialization"                 │
│                                                                      │
│  Resolving dependency → User                                         │
│  → User is Prototype → New object must ALWAYS be created             │
│  → Even though a User object exists inside Student already           │
│  → Prototype doesn't care. Fresh object is created.                  │
│  → Prints: "User initialization"                                     │
│  → Prints: "User object hashCode: 1984730322"  ← DIFFERENT from     │
│                                          Student's user (1510009630) │
│                                                                      │
│  Resolving dependency → Student                                      │
│  → Student is Singleton → Only one object exists → REUSE it         │
│  → Same Student object injected (hashCode: 2092450685)               │
│  → Student internally still holds its original User (1510009630)    │
│                                                                      │
│  TestController1's @PostConstruct runs                               │
│  → Prints: "TestController1 hashCode: 1786739287"                   │
│  → Prints: "User object hashCode: 1984730322"   ← new user          │
│  → Prints: "Student object hashCode: 2092450685" ← same singleton   │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## What Happens on the Second API Call?

You hit `GET /fetchUser` again:

```
┌──────────────────────────────────────────────────────────────────────┐
│                   API CALL 2 → GET /fetchUser                        │
│                                                                      │
│  TestController1 is Prototype → NEW object again                     │
│  → Prints: "TestController1 instance initialization"  (again!)       │
│                                                                      │
│  User is Prototype → ANOTHER new object                              │
│  → Prints: "User object hashCode: 1227388929"  ← different again!    │
│                                                                      │
│  Student is Singleton → Same old object reused                       │
│  → hashCode: 2092450685  ← exactly the same as before                │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## The Full Picture — Visualized

```
┌────────────────────────────────────────────────────────────────────┐
│                    PROTOTYPE vs SINGLETON                          │
│                    (what gets shared, what doesn't)                │
│                                                                    │
│  AT STARTUP:                                                       │
│  Student ──────────────→ User(1510009630)   [created for Student]  │
│                                                                    │
│  API CALL 1:                                                       │
│  TestController1(new) ─→ User(1984730322)   [brand new]            │
│                    └───→ Student(2092450685) [same singleton]      │
│                               └──→ User(1510009630) [unchanged]    │
│                                                                    │
│  API CALL 2:                                                       │
│  TestController1(new) ─→ User(1227388929)   [brand new again]      │
│                    └───→ Student(2092450685) [still same]          │
│                               └──→ User(1510009630) [unchanged]    │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

---

## Summary of Prototype

```
┌──────────────────────────────────────────────────────────┐
│                      PROTOTYPE                           │
├──────────────────────────────────────────────────────────┤
│  New object created every single time it is needed       │
│  Lazily initialized — not created at startup             │
│  If another Singleton depends on it → Prototype object   │
│  gets created at that point (when Singleton is built)    │
│  Every API call → fresh object for Prototype beans       │
│  Singleton beans stay the same throughout                │
└──────────────────────────────────────────────────────────┘
```

---

# Step 4: Request Scope

---

## What is Request Scope?

Simple rule: **A new object is created for each HTTP request.**

But here's the important addition the instructor makes — if within **one single HTTP request**, the same bean is needed in multiple places, Spring will **NOT** create multiple objects. It creates **one object per request** and reuses it within that request.

And just like Prototype, **Request scope is lazily initialized.**

---

## How to Define Request Scope

```java
@Scope("request")
@Component
public class User {
}

// OR using the annotation directly
@RequestScope
@Component
public class User {
}
```

---

## The Setup — Three Classes

```java
// TestController1 — Request scope, depends on User and Student
@Scope("request")
@RestController
public class TestController1 {
    @Autowired
    User user;

    @Autowired
    Student student;
}

// Student — Prototype, depends on User
@Scope("prototype")
@Component
public class Student {
    @Autowired
    User user;
}

// User — Request scope, no dependencies
@Scope("request")
@Component
public class User {
}
```

Dependency map:

```
┌──────────────────────────────────────────────┐
│              DEPENDENCY MAP                  │
│                                              │
│  TestController1  (Request)                  │
│       ├──→ User     (Request)                │
│       └──→ Student  (Prototype)              │
│                 └──→ User  (Request)         │
│                                              │
└──────────────────────────────────────────────┘
```

---

## What Happens at Application Startup?

```
┌──────────────────────────────────────────────────────────────────┐
│                    APPLICATION STARTUP                           │
│                                                                  │
│  IOC finds TestController1 → Scope is Request → Lazy → SKIP      │
│  IOC finds Student → Scope is Prototype → Lazy → SKIP            │
│  IOC finds User → Scope is Request → Lazy → SKIP                 │
│                                                                  │
│  Result: NO objects created at startup.                          │
│  App starts clean.                                               │
└──────────────────────────────────────────────────────────────────┘
```

---

## What Happens on the First API Call?

You hit `GET /fetchUser` for the first time — this is **HTTP Request 1**:

```
┌──────────────────────────────────────────────────────────────────────┐
│                  HTTP REQUEST 1 → GET /fetchUser                     │
│                                                                      │
│  TestController1 is Request scope                                    │
│  → New HTTP request came in → Create new object                      │
│  → Prints: "TestController1 instance initialization"                 │
│                                                                      │
│  Resolving dependency → User                                         │
│  → User is Request scope                                             │
│  → Is there a User object already for THIS request? No.              │
│  → Create new User object                                            │
│  → Prints: "User initialization"                                     │
│  → Prints: "User object hashCode: 39793904"                          │
│  → This User is now bound to HTTP Request 1                          │
│                                                                      │
│  Resolving dependency → Student                                      │
│  → Student is Prototype → Always create new object                   │
│  → Prints: "Student instance initialization"                         │
│  → Student depends on User                                           │
│  → User is Request scope → One object per request                    │
│  → A User object ALREADY exists for this request (39793904)          │
│  → REUSE the same User object — no new object created                │
│  → Prints: "Student hashCode: 275139209"                             │
│  → Prints: "User object hashCode: 39793904" ← SAME user              │
│                                                                      │
│  TestController1's @PostConstruct runs                               │
│  → Prints: "TestController1 hashCode: 898967761"                     │
│  → Prints: "User object hashCode: 39793904"   ← same user            │
│  → Prints: "Student object hashCode: 275139209"                      │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## What Happens on the Second API Call?

You hit `GET /fetchUser` again — this is **HTTP Request 2**:

```
┌──────────────────────────────────────────────────────────────────────┐
│                  HTTP REQUEST 2 → GET /fetchUser                     │
│                                                                      │
│  TestController1 is Request scope                                    │
│  → New HTTP request → New object created                             │
│  → Prints: "TestController1 instance initialization"  (again)        │
│                                                                      │
│  User is Request scope → New request → New User object               │
│  → Prints: "User object hashCode: 1227388929" ← DIFFERENT from R1    │
│                                                                      │
│  Student is Prototype → New object created anyway                    │
│  → But Student's User dependency → same request's User (1227388929)  │
│  → Reuses the new request's User object                              │
│                                                                      │
│  Within Request 2, both TestController1 and Student                  │
│  share the SAME User object → one object per request                 │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## The Full Picture — Visualized

```
┌──────────────────────────────────────────────────────────────────┐
│                 REQUEST SCOPE — THE BIG PICTURE                  │
│                                                                  │
│  HTTP REQUEST 1:                                                 │
│  TestController1 (new) ──→ User(39793904)    [created fresh]     │
│                      └───→ Student(275139209) [prototype,new]    │
│                                  └──→ User(39793904) [REUSED]    │
│                                                                  │
│  HTTP REQUEST 2:                                                 │
│  TestController1 (new) ──→ User(1227388929)   [new for R2]       │
│                      └───→ Student(new)        [prototype,new]   │
│                                  └──→ User(1227388929) [REUSED]  │
│                                                                  │
│  KEY POINT:                                                      │
│  Within one request → same User object shared everywhere         │
│  Across requests → brand new User object each time               │
└──────────────────────────────────────────────────────────────────┘
```

---

## ⚠️ The Big Problem — Singleton Depending on Request Scope

Now the instructor raises a very important scenario. This is where most engineers get confused.

**What if a Singleton class depends on a Request-scoped class?**

```java
@Scope("singleton")
@RestController
public class TestController1 {
    @Autowired
    User user;   // User is Request scoped!
}

@Scope("request")
@Component
public class User {
}
```

### What goes wrong?

```
┌──────────────────────────────────────────────────────────────────────┐
│                      APPLICATION STARTUP                             │
│                                                                      │
│  IOC finds TestController1 → Singleton → Eagerly initialize          │
│  → Calls constructor                                                 │
│  → Prints: "TestController1 instance initialization"                 │
│  → Now tries to resolve dependency → User                            │
│                                                                      │
│  User is Request scope.                                              │
│  Request scope means → object must be tied to an HTTP request        │
│  But at startup, is there any active HTTP request? ← NO!             │
│                                                                      │
│  Spring cannot create a Request-scoped bean without an               │
│  active HTTP request present.                                        │
│                                                                      │
│  RESULT: ❌ APPLICATION FAILS TO START                                │
│  Error: "Error creating bean — Unsatisfied dependency"               │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## ✅ The Solution — Proxy Mode

The instructor explains that the fix is to tell Spring:

> *"Hey, I know there's no HTTP request right now. Just create a dummy placeholder object for now. When a real request comes in, swap it out with the real object."*

That's exactly what **Proxy Mode** does.

```java
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Component
public class User {
    public User() {
        System.out.println("User initialization");
    }

    @PostConstruct
    public void init() {
        System.out.println("User object hashCode: " + this.hashCode());
    }

    public void dummyMethod() {
    }
}
```

---

## How Proxy Mode Works — Step by Step

```
┌──────────────────────────────────────────────────────────────────────┐
│               WITH PROXY MODE — APPLICATION STARTUP                  │
│                                                                      │
│  IOC finds TestController1 → Singleton → Eagerly initialize          │
│  → Tries to resolve User dependency                                  │
│  → User is Request scope, but has proxyMode = TARGET_CLASS           │
│  → Spring says: "OK, I'll create a PROXY (dummy) User object"        │
│  → No "User initialization" printed yet ← real object not created    │
│  → Proxy object injected into TestController1                        │
│  → TestController1's @PostConstruct runs                             │
│  → Prints: "TestController1 hashCode: 1356419559"                    │
│  → Prints: "User object hashCode: 1159352444" ← this is proxy hash   │
│                                                                      │
│  ✅ Application starts successfully!                                  │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

Now when you hit the API:

```
┌──────────────────────────────────────────────────────────────────────┐
│               API CALL → GET /fetchUser  (with Proxy)                │
│                                                                      │
│  HTTP request comes in                                               │
│  → Now there IS an active HTTP request                               │
│  → Proxy steps aside, real User object is created                    │
│  → Prints: "User initialization"   ← NOW it prints                   │
│  → Prints: "User object hashCode: 1078757370" ← real object          │
│  → This real object is now tied to this HTTP request                 │
│  → fetchUser api invoked                                             │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Singleton + Request Scope — The Full Visual

```
┌──────────────────────────────────────────────────────────────────┐
│            SINGLETON + REQUEST SCOPE WITH PROXY                  │
│                                                                  │
│  AT STARTUP:                                                     │
│  TestController1 (Singleton) ──→ User [PROXY object] 🔲          │
│  No real User created yet. App starts fine.                      │
│                                                                  │
│  ON API CALL:                                                    │
│  HTTP Request arrives                                            │
│  Proxy swaps out → Real User object created 🟢                   │
│  TestController1 now uses the real User tied to this request     │
│                                                                  │
│  NEXT API CALL:                                                  │
│  New HTTP Request arrives                                        │
│  Proxy swaps out again → New real User object created 🟢         │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## Summary of Request Scope

```
┌────────────────────────────────────────────────────────────────┐
│                       REQUEST SCOPE                            │
├────────────────────────────────────────────────────────────────┤
│  New object created for each HTTP request                      │
│  Within one request → same object reused everywhere            │
│  Lazily initialized — nothing created at startup               │
│  If a Singleton depends on it → app will FAIL at startup       │
│  Fix → add proxyMode = ScopedProxyMode.TARGET_CLASS            │
│  Proxy acts as placeholder at startup                          │
│  Real object created when actual HTTP request arrives          │
└────────────────────────────────────────────────────────────────┘
```

---

## 🎯 Interview Tip (from the instructor)

The Singleton + Request scope problem and its Proxy Mode solution is a **very common interview question**. Interviewers love asking:

> *"What happens if a Singleton bean depends on a Request-scoped bean? How do you fix it?"*

Your answer:
- At startup, Singleton is eagerly initialized
- Request scope needs an active HTTP request to create its object
- At startup there is no HTTP request → bean creation fails
- Fix: add `proxyMode = ScopedProxyMode.TARGET_CLASS` to the Request-scoped bean
- Spring injects a proxy (dummy) object at startup
- When a real HTTP request comes in, the proxy is replaced by the actual object

---

# Step 5: Session Scope

---

## What is Session Scope?

The rule here is: **A new object is created for each HTTP session.**

Now the instructor is very clear about the difference between a **request** and a **session**:

```
┌──────────────────────────────────────────────────────────────────┐
│           HTTP REQUEST  vs  HTTP SESSION                         │
│                                                                  │
│  HTTP REQUEST                                                    │
│  → Every single API call is one request                          │
│  → Call 1 = Request 1                                            │
│  → Call 2 = Request 2                                            │
│  → Each one is independent                                       │
│                                                                  │
│  HTTP SESSION                                                    │
│  → A session is created when a user first accesses an endpoint   │
│  → Multiple API calls can happen within ONE session              │
│  → Session stays alive until it expires or is invalidated        │
│  → Call 1, Call 2, Call 3 → all inside the same session          │
│  → Only ONE object created for all of them                       │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

Think of it like this — a session is like a **user's visit to a website**. They can click many links (many requests), but it's all one visit (one session). The session-scoped object lives through all of those clicks.

And just like Request, **Session scope is also lazily initialized.**

---

## How to Define Session Scope

```java
@Scope("session")
@RestController
public class TestController1 {
}

// OR
@SessionScope
@RestController
public class TestController1 {
}
```

---

## Session Lifecycle

```
┌──────────────────────────────────────────────────────────────────┐
│                     SESSION LIFECYCLE                            │
│                                                                  │
│  User hits any endpoint for the first time                       │
│          │                                                       │
│          ▼                                                       │
│  HTTP Session is CREATED                                         │
│  → Session-scoped bean object is created                         │
│          │                                                       │
│          ▼                                                       │
│  User makes more API calls (Request 2, 3, 4...)                  │
│  → Same session → Same object reused each time                   │
│          │                                                       │
│          ▼                                                       │
│  Session EXPIRES or is INVALIDATED (logout)                      │
│  → Session-scoped bean is destroyed                              │
│  → Next request creates a brand new session + new object         │
│                                                                  │
│  NOTE: You can configure session expiry in                       │
│  application.properties using servlet session timeout property   │
└──────────────────────────────────────────────────────────────────┘
```

---

## The Setup — Two Classes

```java
// TestController1 — Session scope, depends on User
@Scope("session")
@RestController
@RequestMapping(value = "/api/")
public class TestController1 {
    @Autowired
    User user;

    public TestController1() {
        System.out.println("TestController1 instance initialization");
    }

    @PostConstruct
    public void init() {
        System.out.println("TestController1 object hashCode: " + this.hashCode()
                + " User object hashCode: " + user.hashCode());
    }

    @GetMapping(path = "/fetchUser")
    public ResponseEntity<String> getUserDetails() {
        System.out.println("fetchUser api invoked");
        return ResponseEntity.status(HttpStatus.OK).body("");
    }

    @GetMapping(path = "/logout")
    public ResponseEntity<String> getUserDetails(HttpServletRequest request) {
        System.out.println("end the session");
        HttpSession session = request.getSession();
        session.invalidate();
        return ResponseEntity.status(HttpStatus.OK).body("");
    }
}

// User — Singleton, no dependencies
@Component
public class User {
    public User() {
        System.out.println("User initialization");
    }

    @PostConstruct
    public void init() {
        System.out.println("User object hashCode: " + this.hashCode());
    }
}
```

Dependency map:

```
┌──────────────────────────────────────────┐
│            DEPENDENCY MAP                │
│                                          │
│  TestController1  (Session)              │
│       └──→ User   (Singleton)            │
│                                          │
└──────────────────────────────────────────┘
```

---

## Full Walkthrough — All 4 Scenarios

The instructor walks through four specific scenarios. Let's go through each one carefully.

---

### Scenario 1 — Application Startup

```
┌──────────────────────────────────────────────────────────────────────┐
│                      APPLICATION STARTUP                             │
│                                                                      │
│  IOC finds TestController1 → Scope is Session → Lazy → SKIP          │
│                                                                      │
│  IOC finds User → Scope is Singleton → Eagerly initialize            │
│  → Calls constructor                                                 │
│  → Prints: "User initialization"                                     │
│  → No dependency                                                     │
│  → @PostConstruct runs                                               │
│  → Prints: "User object hashCode: 254812619"                         │
│                                                                      │
│  App starts. Only User object created. No TestController1 yet.       │
└──────────────────────────────────────────────────────────────────────┘
```

---

### Scenario 2 — First API Call (Session Gets Created)

You hit `GET /fetchUser` for the first time:

```
┌──────────────────────────────────────────────────────────────────────┐
│              API CALL 1 → GET /fetchUser                             │
│              (First time → HTTP Session gets CREATED)                │
│                                                                      │
│  New session is created                                              │
│  TestController1 is Session scope → New session → Create new object  │
│  → Prints: "TestController1 instance initialization"                 │
│                                                                      │
│  Resolving dependency → User                                         │
│  → User is Singleton → Only one object exists → REUSE it             │
│  → Same User object (254812619) gets injected                        │
│                                                                      │
│  @PostConstruct runs                                                 │
│  → Prints: "TestController1 hashCode: 1474370807"                    │
│  → Prints: "User object hashCode: 254812619"  ← singleton user       │
│  → Prints: "fetchUser api invoked"                                   │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

### Scenario 3 — Second API Call (Same Session Still Running)

You hit `GET /fetchUser` again **without logging out**:

```
┌──────────────────────────────────────────────────────────────────────┐
│              API CALL 2 → GET /fetchUser                             │
│              (Same session is still active)                          │
│                                                                      │
│  HTTP session is still valid — not ended yet                         │
│  TestController1 is Session scope                                    │
│  → This is NOT a new session, just a new HTTP request                │
│  → So NO new object is created for TestController1                   │
│  → Same object from Scenario 2 is reused                             │
│                                                                      │
│  Spring simply invokes the API method directly                       │
│  → Prints: "fetchUser api invoked"                                   │
│                                                                      │
│  No constructor, no @PostConstruct, nothing.                         │
│  Just the API call.                                                  │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

### Scenario 4 — Session Ends and New API Call Comes In

You hit `GET /logout` which invalidates the session, then hit `GET /fetchUser` again:

```
┌──────────────────────────────────────────────────────────────────────┐
│         LOGOUT → GET /logout  (Session is INVALIDATED)               │
│                                                                      │
│  Prints: "end the session"                                           │
│  session.invalidate() is called                                      │
│  → Old session is destroyed                                          │
│  → TestController1 object from old session is gone                   │
│                                                                      │
├──────────────────────────────────────────────────────────────────────┤
│         API CALL 3 → GET /fetchUser  (After logout)                  │
│                                                                      │
│  No active session exists → New session is created                   │
│  TestController1 is Session scope → New session → New object         │
│  → Prints: "TestController1 instance initialization"  (again!)       │
│                                                                      │
│  Resolving dependency → User                                         │
│  → User is Singleton → Same old object → REUSE                       │
│  → Prints: "User object hashCode: 254812619"  ← still the same!      │
│                                                                      │
│  @PostConstruct runs                                                 │
│  → Prints: "TestController1 hashCode: 75484954"  ← NEW object!       │
│  → Prints: "User object hashCode: 254812619"  ← same singleton       │
│  → Prints: "fetchUser api invoked"                                   │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## The Full Picture — All 4 Scenarios Together

```
┌──────────────────────────────────────────────────────────────────────┐
│                  SESSION SCOPE — COMPLETE PICTURE                    │
│                                                                      │
│  STARTUP:                                                            │
│  User(254812619) created ← Singleton, eager                          │
│  TestController1 → NOT created yet ← Session, lazy                   │
│                                                                      │
│  SESSION 1 STARTS (first API call):                                  │
│  TestController1(1474370807) created                                 │
│         └──→ User(254812619) ← same singleton                        │
│                                                                      │
│  SAME SESSION, API CALL 2:                                           │
│  TestController1(1474370807) ← SAME object, no new creation          │
│         └──→ User(254812619) ← same singleton                        │
│                                                                      │
│  SESSION 1 ENDS (logout called):                                     │
│  TestController1(1474370807) ← destroyed with session                │
│                                                                      │
│  SESSION 2 STARTS (new API call after logout):                       │
│  TestController1(75484954) ← BRAND NEW object                        │
│         └──→ User(254812619) ← still the same singleton!             │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Request vs Session — Side by Side

```
┌────────────────────────────────────────────────────────────────────┐
│               REQUEST SCOPE  vs  SESSION SCOPE                     │
├───────────────────────────┬────────────────────────────────────────┤
│       REQUEST             │         SESSION                        │
├───────────────────────────┼────────────────────────────────────────┤
│ New object per API call   │ New object per user session            │
│ Call 1 → new object       │ Call 1 → new object (session starts)   │
│ Call 2 → new object       │ Call 2 → SAME object (same session)    │
│ Call 3 → new object       │ Call 3 → SAME object (same session)    │
│                           │ Logout → session ends                  │
│                           │ Call 4 → new object (new session)      │
├───────────────────────────┼────────────────────────────────────────┤
│ Lazily initialized        │ Lazily initialized                     │
│ Tied to HTTP request      │ Tied to HTTP session                   │
└───────────────────────────┴────────────────────────────────────────┘
```

---

## Summary of Session Scope

```
┌────────────────────────────────────────────────────────────────┐
│                       SESSION SCOPE                            │
├────────────────────────────────────────────────────────────────┤
│  New object created for each HTTP session                      │
│  Multiple API calls within same session → same object reused   │
│  Session ends on expiry or manual invalidation                 │
│  New session after that → new object created                   │
│  Lazily initialized — nothing created at startup               │
│  Session expiry can be configured in application.properties    │
└────────────────────────────────────────────────────────────────┘
```

---

# Step 6: Application Scope + Complete Revision Summary

---

## Application Scope

The instructor keeps this one brief — and intentionally so. Here's why.

### What is Application Scope?

```
┌──────────────────────────────────────────────────────────────────┐
│                     APPLICATION SCOPE                            │
│                                                                  │
│  One single object shared across MULTIPLE IOC containers         │
│                                                                  │
│  Singleton  →  one object per IOC container                      │
│  Application →  one object across ALL IOC containers             │
│                                                                  │
│   IOC 1 ──┐                                                      │
│   IOC 2 ──┼──→  Single shared Application-scoped bean            │
│   IOC 3 ──┘                                                      │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### How to Define It

```java
@Scope("application")
@Component
public class User {
}

// OR
@ApplicationScope
@Component
public class User {
}
```

### The Instructor's Honest Opinion

> *"In my nine years of experience, I haven't found a use case where multiple IOC containers are running at the same time. Typically one application runs one IOC. So Application scope is rarely used in practice. It's good to know it exists, but don't worry too much about it."*

---

## Complete Revision — All 5 Scopes Together

Now let's bring everything together in one clean consolidated view.

---

### The Master Comparison Table

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                     ALL SPRING BEAN SCOPES — MASTER TABLE                    │
├─────────────────┬──────────────────────────┬───────────────┬─────────────────┤
│   SCOPE         │   OBJECT CREATED WHEN?   │ INIT TYPE     │  TYPICAL USE    │
├─────────────────┼──────────────────────────┼───────────────┼─────────────────┤
│  Singleton      │  Once per IOC container  │ Eager         │  Services,      │
│  (default)      │  at application startup  │               │  Repositories,  │
│                 │                          │               │  Controllers    │
├─────────────────┼──────────────────────────┼───────────────┼─────────────────┤
│  Prototype      │  Every single time the   │ Lazy          │  Stateful beans │
│                 │  bean is needed          │               │  that shouldn't │
│                 │                          │               │  be shared      │
├─────────────────┼──────────────────────────┼───────────────┼─────────────────┤
│  Request        │  Once per HTTP request   │ Lazy          │  Storing        │
│                 │                          │               │  request-level  │
│                 │                          │               │  data           │
├─────────────────┼──────────────────────────┼───────────────┼─────────────────┤
│  Session        │  Once per HTTP session   │ Lazy          │  Storing user   │
│                 │                          │               │  session data   │
│                 │                          │               │  like login     │
├─────────────────┼──────────────────────────┼───────────────┼─────────────────┤
│  Application    │  Once across all IOC     │ Eager         │  Rarely used    │
│                 │  containers              │               │  in practice    │
└─────────────────┴──────────────────────────┴───────────────┴─────────────────┘
```

---

### The Complete Visual — All Scopes Behavior

```
┌──────────────────────────────────────────────────────────────────────┐
│                     SINGLETON                                        │
│                                                                      │
│  App Start ──→ Object Created ONCE                                   │
│  API Call 1 ──→ Same object                                          │
│  API Call 2 ──→ Same object                                          │
│  API Call 3 ──→ Same object                                          │
│  [one object for entire lifetime of application]                     │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│                     PROTOTYPE                                        │
│                                                                      │
│  App Start ──→ No object (lazy)                                      │
│  First needed ──→ Object 1 created                                   │
│  Needed again ──→ Object 2 created  (always fresh)                   │
│  Needed again ──→ Object 3 created  (always fresh)                   │
│  [new object every single time it is needed]                         │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│                     REQUEST                                          │
│                                                                      │
│  App Start ──→ No object (lazy)                                      │
│  HTTP Request 1 ──→ Object 1 created, shared within Request 1        │
│  HTTP Request 2 ──→ Object 2 created, shared within Request 2        │
│  HTTP Request 3 ──→ Object 3 created, shared within Request 3        │
│  [new object per request, shared within that request]                │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│                     SESSION                                          │
│                                                                      │
│  App Start ──→ No object (lazy)                                      │
│  Session 1 starts ──→ Object 1 created                               │
│    Request 1 (Session 1) ──→ Same Object 1                           │
│    Request 2 (Session 1) ──→ Same Object 1                           │
│    Request 3 (Session 1) ──→ Same Object 1                           │
│  Session 1 ends (logout/expiry) ──→ Object 1 destroyed               │
│  Session 2 starts ──→ Object 2 created (fresh)                       │
│    Request 4 (Session 2) ──→ Same Object 2                           │
│  [new object per session, shared within that session]                │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│                     APPLICATION                                      │
│                                                                      │
│  IOC 1 ──┐                                                           │
│  IOC 2 ──┼──→ One single shared object across all IOCs               │
│  IOC 3 ──┘                                                           │
│  [like singleton but across multiple IOC containers]                 │
└──────────────────────────────────────────────────────────────────────┘
```

---

### The Proxy Mode Problem — Quick Revision

```
┌──────────────────────────────────────────────────────────────────────┐
│              WHEN DO YOU NEED PROXY MODE?                            │
│                                                                      │
│  PROBLEM:                                                            │
│  Singleton (eager) ──depends on──→ Request/Session (lazy)            │
│                                                                      │
│  WHY IT FAILS:                                                       │
│  Singleton is created at startup                                     │
│  Request/Session bean needs active HTTP request/session              │
│  At startup → no HTTP request present → FAILS                        │
│                                                                      │
│  FIX:                                                                │
│  Add proxyMode = ScopedProxyMode.TARGET_CLASS                        │
│  to the Request/Session scoped bean                                  │
│                                                                      │
│  HOW IT WORKS:                                                       │
│  Startup → Proxy (dummy) object injected into Singleton ✅            │
│  API Call → Real object created and swapped in ✅                     │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 🎯 Interview Tips — Everything the Instructor Wants You to Remember

### Question 1
**"What is the default scope in Spring Boot?"**
→ Singleton. You don't need to write anything. If no scope is defined, it's singleton.

---

### Question 2
**"What is the difference between Singleton and Prototype?"**
→ Singleton creates one object per IOC, eagerly at startup, shared everywhere.
Prototype creates a new object every single time it is needed, lazily.

---

### Question 3
**"What is the difference between Request and Session scope?"**
→ Request creates one object per HTTP request. Session creates one object per HTTP session. A session can contain multiple requests. Within a session, the same object is reused across all those requests.

---

### Question 4 ← Most Important Interview Question
**"What happens when a Singleton bean depends on a Request-scoped bean? How do you fix it?"**

Answer in steps:
- Singleton is eagerly initialized at startup
- Request-scoped bean needs an active HTTP request to be created
- At startup there is no HTTP request
- So Spring cannot create the Request-scoped bean → application fails to start
- Fix: Add `proxyMode = ScopedProxyMode.TARGET_CLASS` to the Request-scoped bean
- Spring creates a proxy (dummy) object at startup and injects it into the Singleton
- When a real HTTP request comes in, the proxy is replaced by the actual object

---

### Question 5
**"Which scopes are eagerly initialized and which are lazily initialized?"**

```
Eagerly initialized  →  Singleton, Application
Lazily initialized   →  Prototype, Request, Session
```

---

### Question 6
**"What is the difference between Singleton and Application scope?"**
→ Singleton ensures one object per IOC container.
Application ensures one object across multiple IOC containers.
In practice, Application scope is rarely used because most applications run only one IOC container.

---

## One Final Cheat Sheet

```
┌──────────────────────────────────────────────────────────────────┐
│                    SCOPE CHEAT SHEET                             │
│                                                                  │
│  No annotation      →  Singleton (default)                       │
│  @Scope("singleton")                                             │
│  @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)                 │
│                                                                  │
│  @Scope("prototype")                                             │
│  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)                 │
│                                                                  │
│  @Scope("request")                                               │
│  @RequestScope                                                   │
│                                                                  │
│  @Scope("session")                                               │
│  @SessionScope                                                   │
│                                                                  │
│  @Scope("application")                                           │
│  @ApplicationScope                                               │
│                                                                  │
│  Proxy fix:                                                      │
│  @Scope(value = "request",                                       │
│         proxyMode = ScopedProxyMode.TARGET_CLASS)                │
└──────────────────────────────────────────────────────────────────┘
```

---

That's the complete lecture on **Spring Boot Bean Scopes** — covered exactly the way the instructor taught it, step by step, with every example, every scenario, and every interview tip included.

Hope these notes serve you well! 🎯