# Part 1 — What is a Bean & the IOC Container?

---

## The Starting Point: What Problem Are We Solving?

In any real application, you have dozens (sometimes hundreds) of Java objects that need to be **created**, **connected to each other**, and **cleaned up** when no longer needed. If you do all of this manually, your code becomes a mess — you're responsible for tracking every object's lifecycle yourself.

Spring solves this by saying: *"Give me the responsibility. Tell me which objects matter, and I'll handle the rest."*

That's the entire idea behind **Beans** and the **IOC Container**.

---

## What is a Bean?

> *"In simple terms, a bean is a Java object which is managed by the Spring container — that is, the IOC (Inversion of Control) container."*

So a bean is **not** some special or exotic thing. It's just a plain Java object. The only thing that makes it a "bean" is that **Spring is the one creating it and managing it** — not you.

```
Normal Java Object        vs        Spring Bean
──────────────────────────────────────────────
You create it manually              Spring creates it
  (new User())
You manage its lifecycle            Spring manages its lifecycle
You wire dependencies               Spring wires dependencies
  yourself                            automatically
```

---

## What is the IOC Container?

IOC stands for **Inversion of Control**.

Normally, *you* are in control — you write `new User()`, you decide when to create or destroy objects. IOC **flips that control** — now Spring is in control. You just declare what you need, and Spring takes care of it.

The IOC Container is the place where all this happens. Think of it as a **big registry/manager** that:

- Holds all the beans
- Creates them
- Initializes them
- Manages their entire lifecycle from birth to death

```
┌─────────────────────────────────────────┐
│            IOC Container                │
│                                         │
│   ┌──────────┐  ┌──────────┐            │
│   │ UserBean │  │OrderBean │  ...       │
│   └──────────┘  └──────────┘            │
│                                         │
│   → Creates them                        │
│   → Connects them to each other         │
│   → Destroys them when done             │
└─────────────────────────────────────────┘
```

In Spring Boot, the IOC Container is implemented through something called **ApplicationContext**. Whenever you start a Spring Boot app, ApplicationContext starts up and takes over the management of all your beans. You'll actually see it in the startup logs:

```
Initializing Spring embedded WebApplicationContext
Root WebApplicationContext: initialization completed
```

That log line? That's the IOC container waking up.

---

## One Line Summary

| Term | What it means |
|---|---|
| **Bean** | A Java object whose lifecycle is managed by Spring |
| **IOC** | Spring takes control of object creation & management (instead of you) |
| **IOC Container** | The place that holds and manages all beans (implemented as ApplicationContext) |

---

### 💡 Interview Tip
If asked *"What is a bean?"* — don't just say "an object managed by Spring." Add: *"It's no different from a regular Java object, except that its creation, dependency injection, and destruction are all handled by the IOC container (ApplicationContext), not the developer."* That one extra line shows depth.

---
# Part 2 — How to Create a Bean (@Component vs @Bean)

---

## There Are Two Ways to Create a Bean

```
          How to Create a Bean?
                   │
        ┌──────────┴──────────┐
        │                     │
  @Component             @Bean
  Annotation             Annotation
```

Both create beans. But they serve **different purposes**. Let's go through each one.

---

## Way 1 — @Component Annotation

### The Core Idea
@Component follows a **"convention over configuration"** approach.

What that means in plain English: You don't tell Spring *how* to create the object. Spring just figures it out on its own using some built-in defaults (auto-configuration). The main default it uses is — **call the default (no-arg) constructor**.

So when you put @Component on a class, you're basically saying to Spring:
> *"Hey Spring, you have to create an object of this class and manage its lifecycle."*

And Spring responds:
> *"Okay, I'll use the default constructor to create it. I don't need any instructions from you."*

### Simple Example — This Works Fine ✅

```java
@Component
public class User {

    String username;
    String email;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
```

Here, no constructor is explicitly defined. So Java automatically provides a **default no-arg constructor**. Spring calls it, creates the object — no problem.

### The Hidden Rule About Constructors
> *"When we provide our own constructor, Java will NOT add the default constructor."*

This is standard Java behavior, but it's super important in the context of Spring. Let's see what breaks.

### When @Component FAILS ❌

```java
@Component
public class User {

    String username;
    String email;

    // Custom constructor — default constructor is now GONE
    public User(String username, String email) {
        this.username = username;
        this.email = email;
    }

    // getters & setters...
}
```

Now what happens when Spring tries to create this bean?

Spring's auto-configuration says: *"I'll call the default constructor."*
But there is no default constructor anymore.
Spring also doesn't know what values to pass for `username` and `email`.

**Result:**
```
***************************
APPLICATION FAILED TO START
***************************
```

Spring can't create the bean because it doesn't know *how* to create it. Nobody told it what to pass.

This is **exactly the problem @Bean solves.**

---

## Way 2 — @Bean Annotation

### The Core Idea
@Bean is where **you provide the configuration**. You explicitly tell Spring:
> *"Here's exactly how you should create this object — use these values, call this constructor."*

This is called **external configuration** — and Spring always gives it **first priority** over auto-configuration.

### How to Use @Bean — Step by Step

**Step 1:** Remove @Component from the class (you don't need it anymore)

```java
public class User {

    String username;
    String email;

    public User(String username, String email) {
        this.username = username;
        this.email = email;
    }

    // getters & setters...
}
```

**Step 2:** Create a separate Configuration class

```java
@Configuration
public class AppConfig {

    @Bean
    public User createUserBean() {
        return new User("defaultUsername", "defaultEmail");
    }
}
```

That's it. Now Spring knows exactly how to create the `User` object.

### Breaking This Down

```
@Configuration  →  Tells Spring: "Look inside this class, 
                   you'll find bean creation methods here."

@Bean           →  Tells Spring: "This method creates a bean.
                   Call this method to get the object."

Method name     →  Can be anything (createUserBean, makeUser, etc.)
                   It's just a label.

Return type     →  This matters. Spring sees "User" as the 
                   return type and registers a User bean.
```

---

## What If Both @Component AND @Bean Exist for the Same Class?

Good question — the instructor addresses this directly.

Say you have @Component on the class (with a default constructor present), AND you have a @Bean method in a config class for the same type. Which one does Spring use?

> *"First priority goes to external configuration — the @Bean method. Not auto-configuration."*

Whatever you explicitly configure always wins over what Spring auto-figures-out.

---

## What If You Have Multiple @Bean Methods for the Same Type?

```java
@Configuration
public class AppConfig {

    @Bean
    public User createUserBean() {
        return new User("defaultUsername", "defaultEmail");
    }

    @Bean
    public User createAnotherUserBean() {
        return new User("anotherUsername", "anotherEmail");
    }
}
```

Spring doesn't complain. It creates **both** beans and manages both inside the IOC container. So now there are two `User` beans floating around.

But then the question becomes — when you need a `User` bean somewhere in your code, which one does Spring give you? That's handled by **@Qualifier** (and bean naming) — which the instructor will cover in a later part.

---

## @Controller, @Service, @Repository — Are They Also Beans?

Yes! The instructor makes this very clear.

```
@Controller    ──┐
@Service       ──┤──→ All internally are @Component only
@Repository    ──┘

They do a specific job (label the layer) +
they also tell Spring to create & manage a bean for that class.
```

So whenever you put @Service or @Controller on a class, Spring is already going to treat it as a bean — you don't need @Component separately.

---

## Side-by-Side Comparison

```
┌─────────────────────────────────────────────────────────────┐
│              @Component          │          @Bean            │
├─────────────────────────────────────────────────────────────┤
│ Put on the class itself          │ Put on a method inside    │
│                                  │ a @Configuration class    │
├─────────────────────────────────────────────────────────────┤
│ Spring auto-figures out how      │ You explicitly tell Spring │
│ to create it (default            │ how to create it          │
│ constructor)                     │                           │
├─────────────────────────────────────────────────────────────┤
│ Works when default constructor   │ Works even when there's   │
│ is present                       │ no default constructor    │
├─────────────────────────────────────────────────────────────┤
│ Convention over configuration    │ Explicit configuration    │
└─────────────────────────────────────────────────────────────┘
```

---

## When Should You Use @Bean Over @Component?

The clearest use case the instructor gives:

> *"When the default constructor is not present — like when you have a custom constructor with parameters — you HAVE to use @Bean. @Component simply won't work there."*

More generally, use @Bean when you need to **control exactly how the object gets created** — what values it's initialized with, what logic runs during creation, etc.

---

### 💡 Interview Tips

- If asked *"Difference between @Component and @Bean"* — the key answer is: @Component is convention-based (Spring auto-creates using default constructor), while @Bean gives you full control over object creation through explicit configuration. @Bean always gets higher priority.

- If asked *"What happens if there's no default constructor and you use @Component?"* — the application fails to start because Spring doesn't know what values to pass to the constructor.

- If asked *"Can you have multiple @Bean methods of the same return type?"* — yes, Spring creates a separate bean for each. You use @Qualifier or bean names to choose which one to inject where.

---

# Part 3 — How Spring Boot Finds These Beans

---

## The Problem First

Imagine you have a huge application — thousands of files. You've put @Component on 50-60 classes. You've also got @Configuration classes with @Bean methods scattered around.

The natural question is:
> *"How does Spring Boot actually find all these classes? It can't just magically know where they are."*

There are **two ways** Spring Boot finds beans. Let's go through both.

---

## Way 1 — @ComponentScan

### What it does
@ComponentScan tells Spring Boot:
> *"Start from this package, go through it and all its sub-packages, and look for any class annotated with @Component, @Service, @Repository, @Controller, etc. Those are your beans."*

### How it looks in code

```java
@SpringBootApplication
@ComponentScan(basePackages = "com.conceptandcoding.learningspringboot")
public class SpringbootApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringbootApplication.class, args);
    }
}
```

Spring starts from `com.conceptandcoding.learningspringboot` and scans everything inside it — including all nested sub-packages.

---

### The Important Detail — You Don't Always Need to Write It

Here's something the instructor specifically points out:

> *"Even if you don't write @ComponentScan yourself, @SpringBootApplication already has it internally."*

So @SpringBootApplication is actually a combination of multiple annotations under the hood. One of them is @ComponentScan. And its default behavior is:

> *"Start scanning from the package where the main class (the class with @SpringBootApplication) lives — and go into all sub-packages from there."*

```
com.myapp
│
├── SpringbootApplication.java  ← @SpringBootApplication lives here
│                                  Spring scans from HERE downward
│
├── user/
│   └── User.java               ← ✅ Found (sub-package)
│
├── order/
│   └── Order.java              ← ✅ Found (sub-package)
│
└── payment/
    └── Payment.java            ← ✅ Found (sub-package)
```

But if you put a class **outside** this root package — Spring won't find it. That's a common mistake beginners make.

```
com.myapp
└── SpringbootApplication.java  ← scans com.myapp and below

com.other                       ← ❌ Spring won't scan here
└── SomeClass.java                 unless you explicitly add it
                                   to @ComponentScan
```

---

## Way 2 — @Configuration

The second way Spring finds beans is by looking for **@Configuration classes**.

When Spring Boot scans the packages, it also specifically looks for classes marked with @Configuration. Once it finds one, it reads all the @Bean methods inside it and registers those as beans too.

```java
@Configuration          // ← Spring finds this class during scan
public class AppConfig {

    @Bean               // ← Spring reads this method
    public User createUserBean() {
        return new User("defaultUsername", "defaultEmail");
    }
}
```

---

### The Interesting Connection — @Configuration is also @Component

The instructor points out something subtle but important here:

> *"@Configuration is also internally a @Component."*

So when @ComponentScan is running and looking for @Component annotated classes — it also picks up @Configuration classes automatically, because @Configuration is built on top of @Component.

This means both mechanisms are actually connected — @ComponentScan finds @Configuration classes too, not just plain @Component ones.

```
@ComponentScan runs and finds:
│
├── @Component classes     → registers as beans directly
├── @Service classes       → registers as beans (also @Component internally)
├── @Repository classes    → registers as beans (also @Component internally)
├── @Controller classes    → registers as beans (also @Component internally)
└── @Configuration classes → registers @Bean methods as beans
                             (also @Component internally)
```

---

## The Full Picture — How Spring Finds Everything

```
Application Starts
        │
        ▼
@SpringBootApplication triggers
        │
        ▼
@ComponentScan activates
(starts from root package, goes into all sub-packages)
        │
        ├──────────────────────────────────────┐
        ▼                                      ▼
Finds @Component,                    Finds @Configuration
@Service, @Repository,               classes
@Controller classes                            │
        │                                      ▼
        ▼                             Reads all @Bean
Registers them                        methods inside
as beans in IOC                                │
                                              ▼
                                    Registers those
                                    as beans in IOC
                                              │
                              ┌───────────────┘
                              ▼
                    All beans now collected
                    inside IOC Container
```

---

## Quick Recap — Two Ways Spring Finds Beans

```
┌──────────────────────────────────────────────────────────────┐
│                  How Spring Finds Beans                      │
├───────────────────────────┬──────────────────────────────────┤
│     @ComponentScan        │       @Configuration             │
├───────────────────────────┼──────────────────────────────────┤
│ Scans specified package   │ Spring looks for @Configuration  │
│ + all sub-packages        │ classes during scan              │
├───────────────────────────┼──────────────────────────────────┤
│ Finds classes marked      │ Reads @Bean methods inside       │
│ with @Component,          │ those classes and registers      │
│ @Service, @Repository,    │ them as beans                    │
│ @Controller etc.          │                                  │
├───────────────────────────┼──────────────────────────────────┤
│ @SpringBootApplication    │ @Configuration is also           │
│ already includes this     │ @Component internally —          │
│ by default                │ so @ComponentScan finds it       │
│                           │ automatically                    │
└───────────────────────────┴──────────────────────────────────┘
```

---

### 💡 Interview Tips

- If asked *"How does Spring Boot find beans automatically?"* — mention both @ComponentScan (scans packages for annotated classes) and @Configuration (explicitly defined beans via @Bean methods). Also mention that @SpringBootApplication already includes @ComponentScan by default, scanning from the root package downward.

- If asked *"What happens if I put a class outside the root package?"* — Spring won't find it unless you explicitly specify that package in @ComponentScan's `basePackages` attribute.

- If asked *"Is @Configuration a @Component?"* — Yes. @Configuration is internally built on @Component, which is why @ComponentScan picks it up automatically during the scan.

---
# Part 4 — When Are Beans Created? (Eager vs Lazy Initialization)

---

## The Question

Now we know what beans are and how Spring finds them. But there's one more important question before we get to the full lifecycle:

> *"At what time are these beans actually created?"*

Is it when you write the code? When Java compiles? When the app starts? Or later?

The answer is — **it depends**. There are two modes:

```
        When are Beans Created?
                  │
       ┌──────────┴──────────┐
       │                     │
   Eagerness               Lazy
(At app startup)      (When actually needed)
```

---

## Mode 1 — Eager Initialization

### What it means
The bean is created **as soon as the application starts** — even before anyone has asked for it or used it.

### Who gets eagerly initialized?
> *"Beans with Singleton scope are eagerly initialized by default."*

Don't worry too much about "Singleton scope" yet — the instructor says he'll cover scope in depth in the next part. For now, just understand this:

- If you put @Component on a class and don't specify any scope — **Spring treats it as Singleton by default**
- Singleton = one single shared instance across the whole application
- Singleton beans = created eagerly at startup

### Example

```java
@Component          // no @Scope mentioned = Singleton by default
public class User {

    public User() {
        System.out.println("Initializing User");
    }
}
```

When you start the app — even before any request comes in, even before anything calls `User` — you'll see in the logs:

```
Initializing Spring embedded WebApplicationContext
Initializing User                        ← bean created at startup itself
Root WebApplicationContext: initialization completed
Started SpringbootApplication
```

Spring found the class, saw it's a singleton, and created it right away during startup. That's eager initialization.

---

## Mode 2 — Lazy Initialization

### What it means
The bean is **NOT created at startup**. It just sits as a definition. Spring only creates it when something actually needs it — when it's first requested or depended upon.

### Who gets lazily initialized?
Two cases:

**Case 1 — Beans with Prototype scope** (and certain other scopes)
These are lazily initialized by nature. Again, scope will be covered in depth next part — just note this for now.

**Case 2 — Any bean explicitly marked with @Lazy**
Even a Singleton bean can be made lazy if you specifically tell Spring with the @Lazy annotation.

---

### @Lazy in Action

```java
@Lazy               // ← "Don't create this at startup.
@Component          //    Create it only when needed."
public class Order {

    public Order() {
        System.out.println("Initializing Order");
    }
}
```

Now when you start the app — `Order` bean will NOT be created. No "Initializing Order" in the logs at startup. Spring skips it.

It only gets created when something actually needs it. Let's see exactly when that happens.

---

## The Interesting Case — Lazy Bean Needed by an Eager Bean

This is the example the instructor walks through in detail, and it beautifully shows how both modes interact.

### Setup

```java
@Component              // Singleton → Eager
public class User {

    @Autowired
    Order order;        // User depends on Order

    public User() {
        System.out.println("Initializing User");
    }
}
```

```java
@Lazy                   // explicitly Lazy
@Component
public class Order {

    public Order() {
        System.out.println("Lazy: Initializing Order");
    }
}
```

### What Happens Step by Step at Startup?

```
App starts
    │
    ▼
IOC Container initializes
    │
    ▼
Spring scans → finds User class (Singleton, no @Lazy)
→ Eligible for eager initialization
→ Creates User object → prints "Initializing User"
    │
    ▼
Now Spring checks — does User have any dependencies?
→ Yes! @Autowired Order order
    │
    ▼
Spring checks IOC container — is Order bean already present?
→ No (it was marked @Lazy, so not created at startup)
    │
    ▼
But Order is NOW needed (User depends on it)
→ Spring creates Order anyway → prints "Lazy: Initializing Order"
→ Injects it into User
    │
    ▼
User bean is now fully ready
```

**Console output at startup:**
```
Initializing User
Lazy: Initializing Order
```

### The Key Insight Here
> *"@Lazy means don't create at startup — but if something needs it, Spring will create it at that point. It doesn't mean it will NEVER be created."*

Now what if `User` had no dependency on `Order`?

```java
@Component
public class User {
    // no Order dependency here

    public User() {
        System.out.println("Initializing User");
    }
}
```

**Console output at startup:**
```
Initializing User
← no "Initializing Order" here because nothing needed it
```

`Order` simply wouldn't be created at startup at all. It stays dormant until something somewhere actually asks for it.

---

## Side by Side Comparison

```
┌─────────────────────────────────────────────────────────────┐
│            Eager                  │          Lazy            │
├─────────────────────────────────────────────────────────────┤
│ Created at app startup            │ Created when first       │
│                                   │ needed/requested         │
├─────────────────────────────────────────────────────────────┤
│ Default for Singleton beans       │ Default for Prototype    │
│                                   │ beans, or when @Lazy     │
│                                   │ is explicitly used       │
├─────────────────────────────────────────────────────────────┤
│ No annotation needed              │ Add @Lazy annotation     │
│ (it's the default)                │ to opt into this         │
├─────────────────────────────────────────────────────────────┤
│ Startup is slightly slower        │ Startup is faster        │
│ (all beans created upfront)       │ (beans created on demand)│
├─────────────────────────────────────────────────────────────┤
│ Any issues (like missing          │ Issues only surface when │
│ dependencies) show up at          │ that bean is first used  │
│ startup immediately               │ — harder to catch early  │
└─────────────────────────────────────────────────────────────┘
```

---

## Quick Visual Summary

```
WITHOUT @Lazy (Eager — default Singleton):

App Starts ──→ IOC Container ──→ Creates ALL Singleton Beans immediately
                                  │
                                  ├── User bean ✅ created
                                  ├── Order bean ✅ created
                                  └── Payment bean ✅ created


WITH @Lazy on Order:

App Starts ──→ IOC Container ──→ Creates Singleton Beans (skips @Lazy ones)
                                  │
                                  ├── User bean ✅ created
                                  ├── Order bean ⏸ skipped (lazy)
                                  └── Payment bean ✅ created

                                  Later, when Order is needed...
                                  └── Order bean ✅ created now
```

---

### 💡 Interview Tips

- If asked *"What is eager vs lazy initialization in Spring?"* — Eager means beans are created at application startup (default for Singleton beans). Lazy means beans are created only when first requested, either because of their scope (like Prototype) or because @Lazy is explicitly used.

- If asked *"What is the advantage of @Lazy?"* — Faster startup time, since not all beans are created upfront. But the trade-off is that errors in those beans only surface when they're first used, not at startup.

- If asked *"If a @Lazy bean is needed by an eager bean — when does it get created?"* — At the time the eager bean is being initialized and Spring tries to inject the dependency. So it gets created then, not at general startup.

- Singleton scope and Prototype scope will be covered in the next part in depth — if asked about scope in an interview, mention that singleton beans are eagerly initialized and prototype beans are lazily initialized by default.

---
# Part 5 — The Full Lifecycle of a Bean

---

## The Big Picture First

This is the complete journey of a bean — from the moment your app starts to the moment the bean gets destroyed. Everything we've covered in Parts 1-4 comes together here.

```
┌─────────────────────────────────────────────────────────────────┐
│                    LIFECYCLE OF A BEAN                          │
│                                                                 │
│  App        IOC          Construct      Inject                  │
│  Start ──→  Container ──→  Bean    ──→  Dependency              │
│             Started        ↑            into Bean               │
│                            │                 │                  │
│                     Configuration            ▼                  │
│                      Loaded            @PostConstruct           │
│                                              │                  │
│                                             ▼                   │
│                                        Use the Bean             │
│                                              │                  │
│                                             ▼                   │
│                                        @PreDestroy              │
│                                              │                  │
│                                             ▼                   │
│                                       Bean Destroyed            │
└─────────────────────────────────────────────────────────────────┘
```

Now let's go through each step one by one, exactly as the instructor explains — with the demo examples.

---

## Step 1 — Application Starts → IOC Container Invoked

When you start a Spring Boot application, the very first thing that happens internally is Spring Boot **invokes the IOC container** (ApplicationContext).

You can actually see this in your startup logs:

```
Initializing Spring embedded WebApplicationContext
Root WebApplicationContext: initialization completed
```

That right there — that's the IOC container waking up.

Once the IOC container is up, its job is to:
- Use @ComponentScan to find all annotated classes
- Use @Configuration classes to find all @Bean methods
- Collect everything that needs to become a bean

```
App Starts
    │
    ▼
Spring Boot triggers IOC Container (ApplicationContext)
    │
    ▼
IOC uses @ComponentScan + @Configuration
to find all classes/methods that need beans
    │
    ▼
List of "beans to create" is ready
→ Now move to Step 2
```

---

## Step 2 — Construct the Bean

Once IOC knows what beans to create, it starts **constructing them** — meaning it actually creates the objects.

- For **Singleton** beans (default) → constructed eagerly, right now at startup
- For **Lazy** beans → constructed only when needed

### Demo Example

```java
@Component          // Singleton by default → Eager
public class User {

    public User() {
        System.out.println("Initializing User");
    }
}
```

The moment the app starts and IOC initializes — it finds this class, sees it's a Singleton (eligible for eager initialization), and calls the constructor.

**Console output during startup:**
```
Initializing Spring embedded WebApplicationContext
Initializing User                    ← constructor called here
Root WebApplicationContext: initialization completed
```

The bean is now constructed — the object exists in memory inside the IOC container.

---

## Step 3 — Inject Dependencies into the Constructed Bean

After a bean is constructed, Spring checks: **does this bean depend on any other bean?**

This is done through `@Autowired`. When Spring sees @Autowired on a field, it says:
> *"This bean needs another bean. Let me check if that bean already exists in the IOC container."*

Two scenarios:

```
@Autowired found on a field
        │
        ▼
Is the required bean already
in the IOC container?
        │
   ┌────┴────┐
  YES        NO
   │          │
   ▼          ▼
Inject it   Create it first,
directly    then inject it
```

### Demo Example

```java
@Component
public class User {

    @Autowired
    Order order;        // User depends on Order

    public User() {
        System.out.println("Initializing User");
    }
}
```

```java
@Lazy
@Component
public class Order {

    public Order() {
        System.out.println("Lazy: Initializing Order");
    }
}
```

**What happens:**
1. User bean is constructed → *"Initializing User"* printed
2. Spring sees @Autowired Order — checks if Order bean is in IOC container
3. Order is @Lazy — so it wasn't created at startup — not in container yet
4. But it's needed NOW — so Spring creates Order → *"Lazy: Initializing Order"* printed
5. Order bean injected into User

**Console output:**
```
Initializing User
Lazy: Initializing Order
```

### Note on Types of Injection
The instructor briefly mentions there are **three ways** Spring can inject dependencies. These will be covered in depth in the next part:

```
Three Types of Dependency Injection:
├── Constructor Injection   ← Most recommended (used heavily in industry)
├── Setter Injection
└── Field Injection
```

For now, just know that @Autowired is the signal to Spring to perform injection — and Spring first looks for an existing bean, only creating one if it doesn't exist yet.

---

## Step 4 — @PostConstruct (Do Something Before the Bean is Used)

Now the bean is fully constructed AND all dependencies are injected. The bean is technically ready. But before it actually goes into action — Spring gives you a hook:

> *"If you want to do anything right after the bean is constructed and dependencies are injected — but before the bean is actually used — put it in a method marked @PostConstruct."*

### When would you use this?
- Logging that setup is complete
- Initializing some data (like pre-filling a HashMap)
- Any one-time setup logic that needs the dependencies to already be injected

### Demo Example

```java
@Component
public class User {

    @Autowired
    Order order;

    @PostConstruct
    public void initialize() {
        System.out.println("Bean has been constructed and dependencies have been injected");
    }

    public User() {
        System.out.println("Initializing User");
    }
}
```

**Console output at startup:**
```
Initializing User                                               ← Step 2: constructed
Lazy: Initializing Order                                        ← Step 3: dependency injected
Bean has been constructed and dependencies have been injected   ← Step 4: @PostConstruct
```

The sequence is guaranteed — @PostConstruct always runs **after** construction and **after** dependency injection. Never before.

---

## Step 5 — Use the Bean

This is straightforward. After @PostConstruct, the bean is fully ready and in the IOC container. Your application can now:
- Call its methods
- Use it to perform business logic
- Pass it around wherever needed

```java
// Somewhere in your application
user.someMethod();      // you're using the bean now
```

This is the "active life" of the bean — it's doing its job in your application.

---

## Step 6 — @PreDestroy (Do Something Before the Bean Dies)

Every bean eventually gets destroyed — usually when the application shuts down (IOC container closes). Before that happens, Spring gives you one more hook:

> *"If you want to do anything right before the bean gets destroyed — like releasing resources, closing connections — put it in a method marked @PreDestroy."*

### When would you use this?
- Closing a database connection
- Releasing file handles
- Clearing out resources
- Any cleanup logic

### Demo Example

The instructor specifically closes the ApplicationContext manually in the main class — just to demonstrate the destruction phase. In a real app you don't usually do this manually — it happens automatically when the app shuts down.

```java
@SpringBootApplication
public class SpringbootApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context =
            SpringApplication.run(SpringbootApplication.class, args);

        context.close();    // manually closing IOC → triggers bean destruction
    }
}
```

```java
@Component
public class User {

    @PostConstruct
    public void initialize() {
        System.out.println("Post Construct initiated");
    }

    @PreDestroy
    public void preDestroy() {
        System.out.println("Bean is about to destroy, in PreDestroyMethod");
    }

    public User() {
        System.out.println("Initializing User");
    }
}
```

**Complete console output — full lifecycle visible:**
```
Initializing Spring embedded WebApplicationContext
Initializing User                                    ← Step 2: constructed
Root WebApplicationContext: initialization completed
Post Construct initiated                             ← Step 4: @PostConstruct
Started SpringbootApplication
Bean is about to destroy, in PreDestroyMethod        ← Step 6: @PreDestroy
```

---

## The Complete Lifecycle — Everything Together

```
┌──────────────────────────────────────────────────────────────────────┐
│                    COMPLETE BEAN LIFECYCLE                           │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  STEP 1: App Starts                                                  │
│  └─→ IOC Container (ApplicationContext) invoked                      │
│      └─→ Scans via @ComponentScan + @Configuration                  │
│          └─→ Finds all beans that need to be created                 │
│                                                                      │
│  STEP 2: Construct the Bean                                          │
│  └─→ For Singleton (default): created eagerly at startup            │
│  └─→ For @Lazy or Prototype: created when first needed              │
│      └─→ Constructor is called → object exists in IOC               │
│                                                                      │
│  STEP 3: Inject Dependencies                                         │
│  └─→ Spring checks for @Autowired fields                            │
│  └─→ If required bean exists in IOC → inject directly               │
│  └─→ If not → create it first, then inject                          │
│                                                                      │
│  STEP 4: @PostConstruct                                              │
│  └─→ Bean is fully built + dependencies injected                    │
│  └─→ @PostConstruct method runs (your pre-use logic)                │
│                                                                      │
│  STEP 5: Bean is in Action                                           │
│  └─→ Application uses the bean                                       │
│  └─→ Methods called, business logic runs                            │
│                                                                      │
│  STEP 6: @PreDestroy                                                 │
│  └─→ App is shutting down / IOC container closing                   │
│  └─→ @PreDestroy method runs (your cleanup logic)                   │
│                                                                      │
│  STEP 7: Bean Destroyed                                              │
│  └─→ Bean removed from IOC container                                 │
│  └─→ IOC container closes                                            │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## The Hooks Spring Gives You — Quick Reference

```
┌─────────────────────────────────────────────────────────────┐
│  Annotation       │  When it runs          │  Use it for    │
├─────────────────────────────────────────────────────────────┤
│  @PostConstruct   │  After bean is built   │  Setup, data   │
│                   │  + deps injected,      │  init, logging │
│                   │  before bean is used   │                │
├─────────────────────────────────────────────────────────────┤
│  @PreDestroy      │  Just before bean      │  Cleanup,      │
│                   │  is destroyed          │  closing DB    │
│                   │                        │  connections,  │
│                   │                        │  releasing     │
│                   │                        │  resources     │
└─────────────────────────────────────────────────────────────┘
```

---

### 💡 Interview Tips

- *"Explain the lifecycle of a Spring Bean"* — This is one of the most common Spring interview questions. Walk through all steps: IOC starts → bean constructed → dependencies injected → @PostConstruct → bean used → @PreDestroy → bean destroyed. Knowing the exact sequence shows depth.

- *"What is @PostConstruct used for?"* — It runs after the bean is constructed AND after all dependencies are injected. Use it for any initialization logic that depends on those injected dependencies being available.

- *"What is @PreDestroy used for?"* — It runs just before the bean is destroyed. Use it for cleanup — releasing DB connections, clearing resources, etc.

- *"What is ApplicationContext?"* — It is the implementation of the IOC container in Spring. It manages the complete lifecycle of beans — creation, dependency injection, and destruction.

- *"What happens when you close the ApplicationContext?"* — All beans inside it get destroyed. Spring calls the @PreDestroy method on each bean before removing them.

---

## What's Coming Next (as the instructor mentions)

The instructor says the next parts will cover:

```
Coming up:
├── Dependency Injection in depth
│   ├── Constructor Injection  ← most important, used heavily in industry
│   ├── Setter Injection
│   └── Field Injection
│
├── Bean Scope in depth
│   ├── Singleton
│   └── Prototype (and others)
│
└── @Qualifier + Bean Naming
    └── When multiple beans of same type exist —
        how to tell Spring which one to use
```

---

## Full Lecture Summary — All 5 Parts Together

```
PART 1 → Bean = Java object managed by Spring's IOC container
          IOC = Spring takes control of object creation & lifecycle

PART 2 → Two ways to create beans:
          @Component = convention based, uses default constructor
          @Bean = explicit config, you control how object is created
          Use @Bean when default constructor isn't present

PART 3 → Spring finds beans via:
          @ComponentScan = scans packages for annotated classes
          @Configuration = Spring reads @Bean methods inside it
          @SpringBootApplication already includes @ComponentScan

PART 4 → Beans are created either:
          Eagerly = at startup (default for Singleton beans)
          Lazily = when first needed (@Lazy or Prototype scope)

PART 5 → Full Bean Lifecycle:
          Start → IOC invoked → Construct → Inject Dependencies
          → @PostConstruct → Use Bean → @PreDestroy → Destroyed
```

---

That wraps up the complete lecture on **Spring Boot Beans & their Lifecycle**! The instructor's advice at the end is worth noting:

> *"Try it out with your own hands. Only then will you get more questions, and then we can discuss further."*

Hands-on practice is the best way to solidify this — try creating beans both ways, observe the console logs, and trace through the lifecycle yourself! 🚀
