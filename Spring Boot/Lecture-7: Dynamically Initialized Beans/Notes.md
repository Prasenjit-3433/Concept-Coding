# Step 1 — The Problem: Unsatisfied Dependency Exception

---

## The Setup

The instructor starts with a very simple, relatable scenario. There is an **interface** called `Order`, and it has **two implementing classes**:

```
Order (interface)
├── OnlineOrder  (implements Order) → @Component
└── OfflineOrder (implements Order) → @Component
```

Both `OnlineOrder` and `OfflineOrder` are marked with `@Component`, which means **Spring manages them** — they are Spring beans.

Each class is very simple. They just have:
- A constructor that prints something like *"Online Order Initialized"*
- A `createOrder()` method that prints *"Created Online Order"* or *"Created Offline Order"*

Nothing fancy. Just bare minimum classes to demonstrate the concept.

---

## The User Class (Where the Problem Lives)

Now there is a `User` class, which is a `@RestController`. It has a **dependency on the `Order` interface**, injected via `@Autowired`:

```java
@RestController
@RequestMapping(value = "/api")
public class User {

    @Autowired
    Order order;  // ← problem is here

    @PostMapping("/createOrder")
    public ResponseEntity<String> createOrder() {
        order.createOrder();
        return ResponseEntity.ok(body: "");
    }
}
```

When someone hits the `/createOrder` API, it calls `order.createOrder()`. Simple enough, right?

---

## So What Goes Wrong?

Here's the thing — `Order` is just an **interface**. It has **two implementations**: `OnlineOrder` and `OfflineOrder`. Both are `@Component`, so both are registered as beans in Spring's container.

Now when the app starts up, Spring tries to create the `User` bean (because it's a `@RestController`, which is singleton-scoped by default, so Spring initializes it **eagerly** at startup).

While creating the `User` bean, Spring sees:

> *"Hey, User needs an `Order` object. Let me inject one."*

But then Spring looks around and finds **two beans** of type `Order` — `OnlineOrder` and `OfflineOrder`. And Spring doesn't assume anything on its own. It doesn't guess. So it just **fails**.

---

## The Crash

```
***************************
APPLICATION FAILED TO START
***************************

UnsatisfiedDependencyException: 
Error creating bean with name 'user'
```

Spring is essentially saying:

> *"I was trying to create the 'user' bean. It needs an Order. But I found two beans that qualify — OnlineOrder and OfflineOrder. I don't know which one to pick. I give up."*

---

## Visual Diagram

```
SPRING CONTAINER AT STARTUP
─────────────────────────────────────────────────────

  Tries to create → [ User Bean ]
                          │
                          │ @Autowired
                          ▼
                    [ Order ] ← interface
                    /         \
          [OnlineOrder]    [OfflineOrder]
          @Component        @Component
               ↑                 ↑
               └────────┬────────┘
                        │
              Spring sees TWO beans
              of the same type here.
              It doesn't know which
              one to inject.
                        │
                        ▼
                   💥 APP CRASHES
         UnsatisfiedDependencyException
```

---

## Key Takeaway from Step 1

| Thing | Detail |
|---|---|
| Why does this happen? | Two beans of the same type exist in Spring's container |
| When does it crash? | At **application startup**, not when the API is called |
| Why doesn't Spring just pick one? | Spring never assumes. It fails fast rather than injecting the wrong thing silently |
| The class that fails to load | `User`, because its dependency `Order` can't be resolved |

---

This is the **Unsatisfied Dependency Problem**. The app won't even start.

In the next step, we'll see how `@Qualifier` was used to fix this crash — and why, even though it fixes the crash, it introduces a **different, subtler problem**.

# Step 2 — The "Quick Fix" That Creates a New Problem: @Qualifier

---

## How @Qualifier Fixes the Crash

To solve the crash, the instructor introduces `@Qualifier`. The idea is simple — since Spring is confused about *which* bean to inject, we just **tell it explicitly** which one to use.

Here's what changes:

### On the `OnlineOrder` class:
```java
@Qualifier("onlineOrderObject")
@Component
public class OnlineOrder implements Order {

    public OnlineOrder() {
        System.out.println("Online Order Initialized");
    }

    public void createOrder() {
        System.out.println("created Online Order");
    }
}
```

### On the `OfflineOrder` class:
```java
@Qualifier("offlineOrderObject")
@Component
public class OfflineOrder implements Order {

    public OfflineOrder() {
        System.out.println("Offline Order Initialized");
    }

    public void createOrder() {
        System.out.println("created Offline Order");
    }
}
```

### On the `User` class:
```java
@RestController
@RequestMapping(value = "/api")
public class User {

    @Qualifier("onlineOrderObject")  // ← tell Spring: use THIS one
    @Autowired
    Order order;

    @PostMapping("/createOrder")
    public ResponseEntity<String> createOrder() {
        order.createOrder();
        return ResponseEntity.ok(body: "");
    }
}
```

Now Spring knows exactly which bean to inject into `User`. The crash is gone. App starts fine. ✅

---

## What Happens at Runtime?

When you hit the `/createOrder` API, it always calls `order.createOrder()` — and since `order` is always the `OnlineOrder` bean (because of the hardcoded qualifier), you will **always** see:

```
created Online Order
```

Every. Single. Time.

---

## So What's the Problem?

Here is the exact concern the instructor says he received in **a lot of comments** from viewers:

> *"Hey, when we use @Qualifier, aren't we hardcoding the value? Doesn't that break Dependency Inversion?"*

And the instructor agrees — **yes, it does.**

Let's understand why.

---

## What is Dependency Inversion (in simple words)?

Dependency Inversion basically says:

> *"Your class should not be tightly tied to a specific implementation. It should depend on an abstraction (interface), so that you can swap out the actual implementation dynamically — without changing your class."*

That's exactly why `User` depends on the `Order` **interface** and not directly on `OnlineOrder` or `OfflineOrder`. The interface gives you flexibility.

But the moment you write this in `User`:

```java
@Qualifier("onlineOrderObject")
@Autowired
Order order;
```

...you have **hardcoded** the fact that `User` will *always* use `OnlineOrder`. The interface is still there, but the flexibility is gone. You can't switch to `OfflineOrder` without going into the code and changing the qualifier.

---

## Visual Diagram

```
WHAT DEPENDENCY INVERSION PROMISES:
─────────────────────────────────────────────────────

  [ User ]
     │
     │ depends on (interface)
     ▼
  [ Order ] ←── abstraction
  /         \
[OnlineOrder] [OfflineOrder]
     ↑
     └── can be swapped dynamically ✅


WHAT @QUALIFIER ACTUALLY DOES:
─────────────────────────────────────────────────────

  [ User ]
     │
     │ @Qualifier("onlineOrderObject") ← hardcoded!
     ▼
  [ OnlineOrder ] ← always this, no matter what ❌

  [ OfflineOrder ] ← never used, sitting idle
```

So even though `User` technically depends on the `Order` interface, the `@Qualifier` annotation **pins it down** to one specific implementation. The promise of the interface is broken in practice.

---

## Quick Summary of Step 2

| | Detail |
|---|---|
| What @Qualifier does | Tells Spring exactly which bean to inject, removing ambiguity |
| How it fixes the crash | Spring no longer has to guess — the qualifier name points to one specific bean |
| The new problem it creates | The injected bean is now **hardcoded** — no flexibility |
| What principle it breaks | **Dependency Inversion** — you lose the ability to swap implementations dynamically |
| What the instructor confirms | This concern from comments is **valid and correct** |

---

## The Real Question Now

If `@Qualifier` hardcodes things and breaks Dependency Inversion, how do we get the **best of both worlds** — no crash, *and* dynamic flexibility?

That's exactly what the next two steps solve. The instructor gives us **two solutions**:

- **Solution 1** — Still use `@Qualifier`, but in a smarter way, based on **client input** *(industry standard)*
- **Solution 2** — Use `@Bean` + `@Value` to decide which bean to create based on **configuration**

--- 
# Step 3 — Solution 1: Dynamic Bean Selection via @Qualifier (Industry Standard)

---

## The Core Idea

The instructor says this is the **industry standard way** of doing it — used in real production codebases today. The trick is simple but clever:

> *Instead of injecting ONE bean into User and hardcoding which one via @Qualifier... inject BOTH beans into User, and then decide which one to use at runtime, based on what the client/user asks for.*

---

## What Changes in the Code

### `OnlineOrder` and `OfflineOrder` stay the same:
Both still have their `@Qualifier` and `@Component` annotations — no changes there.

```java
@Qualifier("onlineOrderObject")
@Component
public class OnlineOrder implements Order { ... }

@Qualifier("offlineOrderObject")
@Component
public class OfflineOrder implements Order { ... }
```

---

### The `User` class is where the magic happens:

```java
@RestController
@RequestMapping(value = "/api")
public class User {

    @Qualifier("onlineOrderObject")
    @Autowired
    Order onlineOrder;       // ← holds the OnlineOrder bean

    @Qualifier("offlineOrderObject")
    @Autowired
    Order offlineOrder;      // ← holds the OfflineOrder bean

    @PostMapping("/createOrder")
    public ResponseEntity<String> createOrder(
                        @RequestParam boolean isOnlineOrder) {

        if (isOnlineOrder) {
            onlineOrder.createOrder();  // ← use this if client wants online
        } else {
            offlineOrder.createOrder(); // ← use this if client wants offline
        }

        return ResponseEntity.ok(body: "");
    }
}
```

---

## Let's Walk Through What Happens

### At Application Startup:
Both `OnlineOrder` and `OfflineOrder` are `@Component`, so Spring creates **both beans** — they are both singletons sitting in the Spring container.

```
Spring Container
─────────────────────────────
  Bean 1 → OnlineOrder  (singleton)
  Bean 2 → OfflineOrder (singleton)
```

When Spring creates the `User` bean, it sees **two `@Autowired` fields**, each with a different `@Qualifier`. So it injects:
- `OnlineOrder` bean → into `onlineOrder` field
- `OfflineOrder` bean → into `offlineOrder` field

Both beans are ready and sitting inside `User`. No hardcoding of *which one to use* — both are available.

---

### At Runtime (When API is Called):

The client hits the API and passes a parameter:

```
POST /api/createOrder?isOnlineOrder=true   → uses OnlineOrder
POST /api/createOrder?isOnlineOrder=false  → uses OfflineOrder
```

The `if-else` block inside `createOrder()` decides which one to call **based on what the client sent**. Spring is not involved in this decision at all — it's pure Java logic.

---

## Visual Diagram

```
APPLICATION STARTUP
──────────────────────────────────────────────────────────

  Spring Container creates:
  ┌──────────────────┐      ┌───────────────────┐
  │  OnlineOrder     │      │  OfflineOrder     │
  │  (singleton)     │      │  (singleton)      │
  └────────┬─────────┘      └─────────┬─────────┘
           │                          │
           │ injected via             │ injected via
           │ @Qualifier               │ @Qualifier
           │ "onlineOrderObject"      │ "offlineOrderObject"
           ▼                          ▼
  ┌─────────────────────────────────────────────┐
  │               User Bean                     │
  │                                             │
  │   Order onlineOrder  ← OnlineOrder bean     │
  │   Order offlineOrder ← OfflineOrder bean    │
  └─────────────────────────────────────────────┘


AT RUNTIME (API is called)
──────────────────────────────────────────────────────────

  Client Request
       │
       │  ?isOnlineOrder=true
       ▼
  ┌─────────────────────────────┐
  │  if (isOnlineOrder)         │
  │    onlineOrder.createOrder()│ → "created Online Order"
  │  else                       │
  │    offlineOrder.createOrder"│ → "created Offline Order"
  └─────────────────────────────┘
```

---

## Why This Doesn't Break Dependency Inversion

Remember the problem with the original @Qualifier approach — it hardcoded *which* implementation gets used, forever. Here, that's no longer the case:

| | Original @Qualifier Fix | Solution 1 |
|---|---|---|
| How many beans injected | 1 (hardcoded) | 2 (both available) |
| Who decides which to use | Spring (at startup, hardcoded) | Client (at runtime, dynamic) |
| Can you switch implementations? | ❌ No | ✅ Yes, every API call |
| Breaks Dependency Inversion? | ❌ Yes | ✅ No |

The `User` class still depends on the `Order` **interface** — not on a specific implementation. The `if-else` is just routing logic, not tight coupling.

---

## Important Note the Instructor Makes

> *Both `OnlineOrder` and `OfflineOrder` are still singletons. Both beans get created at startup. You're not creating new objects on every request — you're just choosing which already-existing singleton bean to call.*

This is important for performance. No wasteful object creation at runtime.

---

## 💡 Interview Tip

The instructor explicitly calls this the **industry standard approach**. If you're ever asked in an interview:

> *"How do you dynamically select between multiple implementations of an interface in Spring?"*

This is the answer — inject all the relevant beans using `@Qualifier`, and use conditional logic at the method level to decide which one to invoke based on the request. Clean, simple, and production-ready.

---

## Quick Summary of Step 3

| | Detail |
|---|---|
| Core trick | Inject ALL implementations into the class, not just one |
| How selection happens | `if-else` logic based on client request parameter |
| Both beans created? | ✅ Yes, both singletons created at startup |
| Dynamic? | ✅ Yes — every API call can go to a different implementation |
| Industry standard? | ✅ Yes, instructor confirms this explicitly |

---

This was Solution 1 — elegant, simple, and widely used. But there's a **second solution** that works differently. Instead of deciding at *runtime per request*, it decides **at application startup** based on your configuration file.

That's Solution 2 — `@Bean` + `@Value`. 

# Step 4 — Solution 2: Dynamic Bean Initialization via @Bean + @Value

---

## How This Solution Thinks Differently

Before jumping into code, understand the **mindset shift** between Solution 1 and Solution 2:

| | Solution 1 | Solution 2 |
|---|---|---|
| When is the decision made? | At **runtime** (every API call) | At **startup** (once, when app loads) |
| Who makes the decision? | The **client** (via request parameter) | The **configuration file** (`application.properties`) |
| How many beans are created? | Both `OnlineOrder` AND `OfflineOrder` | Only **one** — whichever the config says |
| Use case | User-driven, request-by-request switching | Environment-driven, deploy-time switching |

---

## What Changes in the Code

### `OnlineOrder` and `OfflineOrder` — Big Change Here:

```java
// NO @Component here anymore!
public class OnlineOrder implements Order {

    public OnlineOrder() {
        System.out.println("Online Order Initialized");
    }

    public void createOrder() {
        System.out.println("created Online Order");
    }
}
```

```java
// NO @Component here anymore!
public class OfflineOrder implements Order {

    public OfflineOrder() {
        System.out.println("Offline Order Initialized");
    }

    public void createOrder() {
        System.out.println("created Offline Order");
    }
}
```

The instructor makes a **very important point** here:

> *"Notice that there is no @Component on these classes anymore. That's because we are now manually providing the bean through a @Configuration class. We are taking control away from Spring's auto-detection and doing it ourselves."*

---

### The `User` class — Back to Simple:

```java
@RestController
@RequestMapping(value = "/api")
public class User {

    @Autowired
    Order order;   // ← just one, simple @Autowired, no @Qualifier

    @PostMapping("/createOrder")
    public ResponseEntity<String> createOrder() {
        order.createOrder();
        return ResponseEntity.ok(body: "");
    }
}
```

Clean and simple. `User` just asks for an `Order` — it doesn't know or care whether it's online or offline.

---

### The New Player — A @Configuration Class:

This is the heart of Solution 2. A brand new class is introduced:

```java
@Configuration
public class AppConfig {

    @Bean
    public Order createOrderBean(
            @Value("${isOnlineOrder}") boolean isOnlineOrder) {

        if (isOnlineOrder) {
            return new OnlineOrder();
        } else {
            return new OfflineOrder();
        }
    }
}
```

Let's break this down piece by piece.

---

## Breaking Down the @Configuration Class

### `@Configuration`
Tells Spring: *"This class contains bean definitions. Look inside it for methods annotated with @Bean."*

### `@Bean`
Tells Spring: *"This method's return value should be registered as a bean in the Spring container."* The return type is `Order`, so Spring will register whatever this method returns as an `Order` bean.

### `@Value("${isOnlineOrder}")`
This is the key annotation of this entire lecture. It tells Spring:

> *"Before calling this method, go find a property called `isOnlineOrder` from the configuration sources — like `application.properties` — and inject its value here as the `isOnlineOrder` parameter."*

### The `if-else` logic
Based on whatever value was injected:
- `true` → return a new `OnlineOrder` object
- `false` → return a new `OfflineOrder` object

Whichever object gets returned, Spring registers **that** as the `Order` bean.

---

### The `application.properties` file:

```properties
isOnlineOrder=false
```

This is where you **control** which implementation gets used. Change this one line, restart the app, and a completely different bean gets created.

---

## Walking Through What Happens Step by Step

```
STEP 1: App starts up
         │
         ▼
STEP 2: Spring sees User is a @RestController (singleton)
        → tries to create User bean
        → sees it needs an Order dependency
         │
         ▼
STEP 3: Spring looks for an Order bean
        → No @Component on OnlineOrder or OfflineOrder
        → Finds @Configuration class AppConfig
        → Sees @Bean method createOrderBean()
         │
         ▼
STEP 4: Spring tries to call createOrderBean()
        → Sees @Value("${isOnlineOrder}") parameter
        → Goes to application.properties
        → Finds: isOnlineOrder=false
        → Injects false into the method
         │
         ▼
STEP 5: Method runs with isOnlineOrder = false
        → goes to else branch
        → returns new OfflineOrder()
         │
         ▼
STEP 6: Spring registers OfflineOrder as the Order bean
        → injects it into User
         │
         ▼
STEP 7: App starts successfully
        Console: "Offline Order Initialized"
         │
         ▼
STEP 8: API is called → order.createOrder()
        Console: "created Offline Order"
```

---

## Visual Diagram

```
application.properties
┌─────────────────────────┐
│  isOnlineOrder=false    │
└────────────┬────────────┘
             │  @Value reads this
             ▼
┌─────────────────────────────────────┐
│  @Configuration AppConfig           │
│                                     │
│  @Bean                              │
│  Order createOrderBean(             │
│      boolean isOnlineOrder=false) { │
│                                     │
│    if(isOnlineOrder)  ← false       │
│      return new OnlineOrder();  ✗   │
│    else                             │
│      return new OfflineOrder(); ✓   │
│  }                                  │
└──────────────┬──────────────────────┘
               │
               │ registers this as Order bean
               ▼
    ┌─────────────────────┐
    │   OfflineOrder Bean  │
    │   (singleton)        │
    └──────────┬──────────┘
               │ @Autowired
               ▼
    ┌─────────────────────┐
    │     User Bean        │
    │   Order order ←──────┘
    └─────────────────────┘
               │
               │ API called
               ▼
      "created Offline Order"


── Change config to true & restart ──

application.properties
┌─────────────────────────┐
│  isOnlineOrder=true     │
└────────────┬────────────┘
             │
             ▼
    ┌─────────────────────┐
    │   OnlineOrder Bean   │  ← now THIS gets created
    └──────────┬──────────┘
               │
               ▼
      "created Online Order"
```

---

## Key Difference from Solution 1 — Only ONE Bean is Created

In Solution 1, **both** `OnlineOrder` and `OfflineOrder` beans were created at startup (both were singletons in the container). The selection happened at runtime.

In Solution 2, **only one** bean is created — whichever one the config file points to. The other class doesn't even get instantiated.

```
Solution 1 Spring Container:        Solution 2 Spring Container:
┌──────────────────────────┐        ┌──────────────────────────┐
│ OnlineOrder bean   ✅    │        │ OnlineOrder bean   ✅    │
│ OfflineOrder bean  ✅    │        │ OfflineOrder bean  ❌    │
│ User bean          ✅    │        │ User bean          ✅    │
└──────────────────────────┘        └──────────────────────────┘
  (isOnlineOrder=false config)
```

---

## When Would You Use Solution 2 in Real Life?

This approach is perfect when the decision is **environment-based**, not user-based. For example:

- In your **development** environment → use a mock payment gateway
- In your **production** environment → use the real payment gateway
- In **testing** → use an in-memory database
- In **production** → use a real database

You just change the `application.properties` (or environment variables on the server), restart, and the right bean gets wired up automatically. The rest of the code doesn't change at all.

---

## Quick Summary of Step 4

| | Detail |
|---|---|
| `@Component` removed? | ✅ Yes — beans are now manually defined |
| Who defines the bean? | `@Configuration` class with a `@Bean` method |
| How is the decision made? | `@Value` reads from `application.properties` |
| When is the decision made? | At **app startup**, once |
| How to switch implementations? | Change `application.properties` and restart |
| How many beans created? | Only **one** — the one the config points to |

---

We have one final step left — a proper **deep dive into the `@Value` annotation** itself, what it can do beyond this example, and how to use inline literals with it.

# Step 5 — Deep Dive into the @Value Annotation

---

## What Exactly is @Value?

The instructor gives a clean, one-line definition:

> *"@Value is used to inject values from various sources like property files, environment variables, or inline literals."*

That's it. Its job is simple — **pull a value from somewhere and inject it into your code**. But understanding *where* it can pull from, and *how* to write it correctly, is what makes it powerful.

---

## The Three Sources @Value Can Pull From

```
                        @Value
                           │
          ┌────────────────┼─────────────────┐
          ▼                ▼                 ▼
   Property File    Environment          Inline
  (application.     Variables            Literal
   properties)      (server/OS           (hardcoded
                     level)               value)
```

Let's look at each one.

---

### Source 1 — Property File (`application.properties`)

This is what the instructor demonstrated in Solution 2. You write a placeholder inside `${}` and Spring goes and fetches the matching key from `application.properties`.

```properties
# application.properties
isOnlineOrder=false
```

```java
@Bean
public Order createOrderBean(
        @Value("${isOnlineOrder}") boolean isOnlineOrder) {

    if (isOnlineOrder) {
        return new OnlineOrder();
    } else {
        return new OfflineOrder();
    }
}
```

Spring sees `${isOnlineOrder}`, searches `application.properties` for a key named `isOnlineOrder`, finds `false`, and injects it.

```
@Value("${isOnlineOrder}")
         │
         │  Spring reads this key
         ▼
application.properties
─────────────────────
isOnlineOrder=false    ← found it! inject false.
```

---

### Source 2 — Environment Variables

The same `${}` syntax works for **OS-level or server-level environment variables** too. This is heavily used in cloud deployments and CI/CD pipelines.

For example, on your server you might set:

```bash
export isOnlineOrder=true
```

And the exact same `@Value("${isOnlineOrder}")` in your code will pick it up — no code change needed. Spring checks both `application.properties` and environment variables.

This is why Solution 2 is so powerful for environment-based switching — you don't even need to touch the properties file. Just set an env variable on the server.

---

### Source 3 — Inline Literal

This is the simplest use. Instead of a placeholder `${}`, you directly **hardcode the value** inside the `@Value` annotation itself:

```java
@Bean
public Order createOrderBean(
        @Value("false") boolean isOnlineOrder) {

    if (isOnlineOrder) {
        return new OnlineOrder();
    } else {
        return new OfflineOrder();
    }
}
```

No property file needed. Spring just takes the string `"false"`, converts it to a boolean, and injects it directly.

The instructor notes this in the transcript:

> *"Inline literals means you can also provide the actual value directly — a string literal. You provide false or true directly, and that value gets set."*

---

## Syntax Comparison — All Three Sources

```
┌─────────────────────┬──────────────────────────────────────┐
│ Source              │ Syntax                               │
├─────────────────────┼──────────────────────────────────────┤
│ Property File /     │ @Value("${propertyKey}")             │
│ Environment Var     │                                      │
├─────────────────────┼──────────────────────────────────────┤
│ Inline Literal      │ @Value("actualValue")                │
└─────────────────────┴──────────────────────────────────────┘
```

The `${}` is called a **placeholder**. When Spring sees it, it knows to go look something up. Without `${}`, Spring treats the value as a direct literal.

---

## Where Can @Value Be Used?

The instructor uses it on a **method parameter** in the `@Bean` method, but `@Value` is flexible — it can be placed on:

```java
// 1. On a field directly
@Value("${isOnlineOrder}")
boolean isOnlineOrder;

// 2. On a method parameter (what instructor showed)
@Bean
public Order createOrderBean(@Value("${isOnlineOrder}") boolean isOnlineOrder) { ... }

// 3. On a constructor parameter
public AppConfig(@Value("${isOnlineOrder}") boolean isOnlineOrder) { ... }
```

---

## Full Picture — How @Value Fits Into Solution 2

```
SOURCES
─────────────────────────────────────────────────────────

  application.properties          Environment Variable
  ┌──────────────────────┐        ┌───────────────────┐
  │ isOnlineOrder=false  │   OR   │ isOnlineOrder=true │
  └──────────┬───────────┘        └────────┬──────────┘
             │                             │
             └──────────┬──────────────────┘
                        │
                        │  Spring resolves placeholder
                        ▼
              @Value("${isOnlineOrder}")
                        │
                        │  injects resolved value
                        ▼
         createOrderBean(boolean isOnlineOrder)
                        │
               ┌────────┴────────┐
           true│                 │false
               ▼                 ▼
        new OnlineOrder()   new OfflineOrder()
               │                 │
               └────────┬────────┘
                        │
                        ▼
                  Registered as
                  Order bean in
                  Spring Container
                        │
                        ▼
                   Injected into
                   User via @Autowired
```

---

## Common Mistake to Avoid

If Spring **cannot find** the property key you specified in `${}`, the app will crash at startup with something like:

```
Could not resolve placeholder 'isOnlineOrder' 
in value "${isOnlineOrder}"
```

So always make sure:
- The key in `application.properties` **exactly matches** what's inside `${}`
- The property file is in the right location (`src/main/resources/`)

---

## Quick Summary of @Value

| | Detail |
|---|---|
| What it does | Injects a value into your code from an external or inline source |
| Placeholder syntax | `@Value("${keyName}")` — Spring looks up `keyName` |
| Inline literal syntax | `@Value("someValue")` — Spring uses the value directly |
| Sources it supports | `application.properties`, environment variables, inline literals |
| Where it can be placed | Fields, method parameters, constructor parameters |
| What happens if key not found | App crashes at startup with placeholder resolution error |

---

## Putting It All Together — The Full Picture of This Lecture

```
PROBLEM
───────
Two beans (OnlineOrder, OfflineOrder) → Spring confused → App crashes
(UnsatisfiedDependencyException)

     │
     ▼

NAIVE FIX — @Qualifier (hardcoded)
───────────────────────────────────
Fixes crash ✅  BUT breaks Dependency Inversion ❌
One implementation always injected, no flexibility

     │
     ▼

SOLUTION 1 — @Qualifier (smart way) ← Industry Standard
─────────────────────────────────────────────────────────
Inject BOTH beans into User
Use if-else to pick one based on CLIENT INPUT at runtime
Dynamic per request ✅  Both beans exist in container ✅

     │
     ▼

SOLUTION 2 — @Bean + @Value
─────────────────────────────────────────────────────────
Remove @Component from implementations
Use @Configuration + @Bean to manually define the bean
Use @Value to read from application.properties
Only ONE bean created at startup
Switch by changing config file ✅  Perfect for environments ✅
```

---

That's the complete lecture! You now have a solid understanding of:
- What the Unsatisfied Dependency problem is and why it happens
- How `@Qualifier` solves it but breaks Dependency Inversion
- Two clean, real-world solutions to make bean selection dynamic
- What `@Value` is, where it pulls values from, and how to use it

Hope these notes serve you well! 🙌