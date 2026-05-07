# Step 1 — What is an Interceptor & Why Do We Need It?

---

## What is an Interceptor?

In simple words:

> **An interceptor is a mediator — a piece of code that gets invoked either before or after your actual code runs.**

That's it. Nothing fancy. Think of it like a security checkpoint at an airport — before you board the plane (your actual destination / controller), you go through security (the interceptor). The interceptor can inspect, allow, block, or log things — without the actual flight (controller) knowing about it.

---

## Why Do We Need It?

Imagine you're building a Spring Boot application with 50 REST APIs. Now your manager says:

- *"Log every incoming request"*
- *"Check if the user is authenticated before hitting any API"*
- *"Add caching to specific methods"*

Without interceptors, you'd have to write that logic **inside every single controller method** — which is a nightmare to maintain.

With an interceptor, you write that logic **once**, and it automatically applies wherever you configure it.

Shreyansh specifically mentions that in future videos, custom interceptors will be needed for:
- ✅ Caching
- ✅ Logging
- ✅ Authentication & Authorization

---

## Where Does the Interceptor Sit? (The Big Picture)

This is the most important thing to understand before writing any code. Let's look at what happens when an HTTP request hits your Spring Boot app:

```
                        YOUR SPRING BOOT APPLICATION
                       ┌──────────────────────────────────────────────────────┐
                       │                                                      │
  HTTP Request         │   ┌─────────────┐      ┌─────────────────────────┐   │
─────────────────────► │   │   Servlet   │─────►│   DispatcherServlet     │   │
                       │   │  Container  │      │                         │   │
                       │   │  (Tomcat)   │      │  1. Choose Controller   │   │
                       │   └─────────────┘      │  2. Create Instance     │   │
                       │                        │  3. Invoke method       │   │
  HTTP Response        │                        │  4. Send Response       │   │
◄───────────────────── │                        └──────────┬──────────────┘   │
                       │                                   │                  │
                       │                    ┌──────────────▼──────────────┐   │
                       │                    │      Your Controller        │   │
                       │                    │   (e.g. UserController)     │   │
                       │                    └─────────────────────────────┘   │
                       └──────────────────────────────────────────────────────┘
```

Now — **where do interceptors plug in?**

There are **two places** Shreyansh teaches in this lecture:

```
  HTTP Request
      │
      ▼
 [Servlet Container / Tomcat]
      │
      ▼
 [DispatcherServlet]
      │
      ▼
 ┌────────────────────────────┐
 │  🔴 INTERCEPTOR TYPE 1     │  ← Before request even reaches the Controller
 │  (HandlerInterceptor)      │     e.g. Auth check, logging incoming request
 └────────────┬───────────────┘
              │
              ▼
 ┌────────────────────────────┐
 │      Your Controller       │
 │   (e.g. UserController)    │
 │            │               │
 │            ▼               │
 │  ┌──────────────────────┐  │
 │  │ 🔵 INTERCEPTOR TYPE 2│  │  ← After Controller is invoked, intercepts
 │  │  (AOP / @Aspect)     │  │     specific method calls inside the app
 │  └──────────────────────┘  │
 │            │               │
 │            ▼               │
 │     Service / Other        │
 │       class method         │
 └────────────────────────────┘
              │
              ▼
         HTTP Response
```

---

## The Two Types of Interceptors in This Lecture

| | Type 1 | Type 2 |
|---|---|---|
| **When** | Before request hits controller | After controller, on specific methods |
| **How** | `HandlerInterceptor` | AOP (`@Aspect` + custom annotation) |
| **Use case** | Auth, request logging | Caching, method-level logging |

---

## Prerequisites Shreyansh Recommends

Before this topic makes full sense, you should know:
1. **Java Annotations in depth** — from the Java playlist
2. **AOP (Aspect Oriented Programming)** in Spring Boot — specifically pointcut, advice, and proxy concepts

> 💡 Shreyansh says: *"If you get a doubt about what pointcut or advice is — stop here, check the AOP video, and then come back. Otherwise it will create confusion."*

---

# Step 2 — Interceptor Before the Controller (HandlerInterceptor)

---

## The Goal

We want to intercept an incoming HTTP request **before it even reaches our controller**. So somewhere between the DispatcherServlet choosing the controller and actually invoking it — our custom code should run.

---

## Step-by-Step: How to Build This

There are **two things** you need to do:
1. **Create** the custom interceptor class
2. **Register** it — tell Spring for which URLs it should apply

Let's go through both.

---

## Part 1 — Creating the Interceptor Class

You create a normal Java class and implement the `HandlerInterceptor` interface.

```java
@Component
public class MyCustomInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        System.out.println("inside pre handle method");
        return true; // ⚠️ important — explained below
    }

    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler,
                           @Nullable ModelAndView modelAndView) throws Exception {
        System.out.println("inside post handle method");
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                @Nullable Exception ex) throws Exception {
        System.out.println("inside after completion method");
    }
}
```

When you implement `HandlerInterceptor`, you **must** provide implementations for these three methods. Let's understand each one.

---

## The Three Methods — What Do They Mean?

```
  HTTP Request
      │
      ▼
 DispatcherServlet
      │
      ▼
 ┌─────────────────────────────────┐
 │  1. preHandle()                 │  ← Runs BEFORE your controller method
 │     return true → continue      │
 │     return false → stop here    │
 └──────────────┬──────────────────┘
                │
                ▼
 ┌─────────────────────────────────┐
 │  Your Controller Method runs    │  ← Your actual business logic
 │  (e.g. getUser())               │
 └──────────────┬──────────────────┘
                │
       ┌────────┴────────┐
       │                 │
   ✅ Success        ❌ Exception
       │                 │
       ▼                 │
 ┌──────────────┐        │
 │ 2.postHandle │        │  ← Runs ONLY on success, skipped if exception
 └──────┬───────┘        │
        │                │
        ▼                ▼
 ┌─────────────────────────────────┐
 │  3. afterCompletion()           │  ← ALWAYS runs — success OR exception
 │  (like finally in Java)         │
 └─────────────────────────────────┘
```

---

## The Key Difference: postHandle vs afterCompletion

This is something Shreyansh emphasizes clearly, and it's an **interview-worthy point**:

| | `postHandle` | `afterCompletion` |
|---|---|---|
| **When it runs** | After controller, only on success | After controller, always |
| **If exception occurs** | ❌ Skipped | ✅ Still runs |
| **Java equivalent** | like code after a method call | like `finally` block |

> 💡 **Interview Tip:** If asked *"what's the difference between postHandle and afterCompletion?"* — say: *"postHandle runs only when the controller executes successfully. afterCompletion runs no matter what — even if there's an exception — similar to how a finally block works in Java."*

---

## One Important Thing About preHandle's Return Value

Notice `preHandle` returns a **boolean** — the other two methods return void. This matters:

- `return true` → Spring continues, your controller gets invoked ✅
- `return false` → Spring stops right there, controller is never called ❌

This is exactly how **authentication interceptors** work — if the token is invalid, return false and the request never reaches your controller.

---

## Part 2 — Registering the Interceptor

Just creating the interceptor class isn't enough. You need to tell Spring:
- *"Hey, use this interceptor"*
- *"Apply it to these URL patterns"*
- *"But skip these specific URLs"*

You do this in a **configuration class** that implements `WebMvcConfigurer`:

```java
@Configuration
public class AppConfig implements WebMvcConfigurer {

    @Autowired
    MyCustomInterceptor myCustomInterceptor; // ⬅️ auto-wired because @Component is on the interceptor

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(myCustomInterceptor)
                .addPathPatterns("/api/*")                          // ✅ apply to these URLs
                .excludePathPatterns("/api/updateUser",
                                     "/api/deleteUser");           // ❌ skip these URLs
    }
}
```

---

## Understanding the URL Pattern

```
  /api/*   ← the * is a wildcard, means "anything after /api/"

  So:
  /api/getUser      ✅ interceptor runs
  /api/createUser   ✅ interceptor runs
  /api/updateUser   ❌ excluded explicitly
  /api/deleteUser   ❌ excluded explicitly
```

---

## Why @Autowired Works Here (Small but Important Point)

Shreyansh points this out specifically:

Since `MyCustomInterceptor` is annotated with `@Component`, Spring **already creates its bean**. So in `AppConfig`, you can simply `@Autowired` it.

If you had NOT put `@Component` on the interceptor, you'd have to manually create it like:
```java
// if no @Component on the interceptor
registry.addInterceptor(new MyCustomInterceptor());
```

But with `@Component`, Spring manages the bean and you just wire it in. This keeps things clean.

---

## How It All Looks Internally (Inside DispatcherServlet)

Shreyansh actually opens the `DispatcherServlet.class` source code to show this. Here's what happens inside its `doDispatch()` method in simple terms:

```
doDispatch() inside DispatcherServlet:

  1. applyPreHandle()      ← your preHandle() runs here
       │
       ▼
  2. handle()              ← your actual Controller method runs here
       │
       ▼
  3. applyPostHandle()     ← your postHandle() runs here
       │
       ▼
  4. afterCompletion()     ← always runs, even on exception
```

This confirms that the interceptor is not something you bolt on from outside — it's **baked into how DispatcherServlet works**.

---

## The Output When You Hit `/api/getUser`

```
inside pre handle Method       ← preHandle ran first
hitting db to get the userdata ← controller ran
inside post handle method      ← postHandle ran after
inside after completion method ← afterCompletion ran last
```

---

## Quick Summary of Step 2

```
To intercept BEFORE the controller:

  1. Create a class → implement HandlerInterceptor
       - preHandle()       → before controller  (return true to proceed)
       - postHandle()      → after controller, only on success
       - afterCompletion() → after controller, always (like finally)

  2. Register it in a @Configuration class → implement WebMvcConfigurer
       - override addInterceptors()
       - addPathPatterns()     → which URLs to intercept
       - excludePathPatterns() → which URLs to skip
```

---

# Step 3 — Custom Annotations (Foundation for the Second Interceptor)

---

## Why Are We Learning This Here?

Before building the second type of interceptor (the AOP-based one), you need to know how to create your own annotation — because that annotation is what the interceptor will **listen to**.

Think of it like this:

> You put a label (`@MyCustomAnnotation`) on a method → the interceptor sees that label → it runs its logic.

So annotations are the **trigger mechanism** for the second interceptor. That's why Shreyansh teaches this first.

---

## How to Create a Custom Annotation

In Java, you create an annotation using the `@interface` keyword:

```java
public @interface MyCustomAnnotation {
}
```

That's it — you've created an annotation. You can now use it like this:

```java
public class User {
    @MyCustomAnnotation
    public void updateUser() {
        // some business logic
    }
}
```

But right now it does **nothing**. To make it meaningful, you need two important things called **Meta Annotations**.

---

## What is a Meta Annotation?

> A meta annotation is simply **an annotation that is applied ON TOP of another annotation**.

There are two meta annotations you absolutely must know:

---

## Meta Annotation 1 — `@Target`

**What it does:** Tells Java *where* this annotation is allowed to be used — on a method? On a class? On a field?

```java
// Only allowed on methods
@Target(ElementType.METHOD)
public @interface MyCustomAnnotation {
}
```

```java
// Allowed on multiple places
@Target({ElementType.CONSTRUCTOR,
         ElementType.METHOD,
         ElementType.PARAMETER,
         ElementType.FIELD})
public @interface MyCustomAnnotation {
}
```

Common `ElementType` values:

| ElementType | Where it applies |
|---|---|
| `METHOD` | On a method |
| `FIELD` | On a class field / variable |
| `CONSTRUCTOR` | On a constructor |
| `PARAMETER` | On a method parameter |
| `TYPE` | On a class or interface |

---

## Meta Annotation 2 — `@Retention`

**What it does:** Tells Java *how long* this annotation should be kept — should it survive compilation? Should it be available when the code is actually running?

There are **three retention policies**:

---

### Policy 1 — `RetentionPolicy.SOURCE`

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface MyCustomAnnotation {
}
```

```
  Your .java file          Compiler runs         .class file (bytecode)
  ─────────────            ────────────          ──────────────────────
  @MyCustomAnnotation  ──────────────────►       (annotation is GONE)
  public void updateUser()                        public void updateUser()
```

- The annotation exists only in your source code
- The compiler **throws it away** — it doesn't even appear in the `.class` file
- **Real world example:** `@Override` — it's just there so engineers can read the code clearly. It adds zero value at runtime.

---

### Policy 2 — `RetentionPolicy.CLASS`

```
  Your .java file          Compiler runs         .class file          JVM at Runtime
  ─────────────            ────────────          ───────────          ──────────────
  @MyCustomAnnotation  ──────────────────►   @MyCustomAnnotation  ──►  (annotation IGNORED)
  public void updateUser()                   public void updateUser()   public void updateUser()
```

- The annotation **makes it into the `.class` file**
- But the JVM **ignores it completely** at runtime
- So it's stored in bytecode but has no effect when the program runs
- This is actually the **default** if you don't specify `@Retention` at all

---

### Policy 3 — `RetentionPolicy.RUNTIME` ⭐ (Most Important for Interceptors)

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyCustomAnnotation {
}
```

```
  Your .java file          Compiler runs         .class file          JVM at Runtime
  ─────────────            ────────────          ───────────          ──────────────
  @MyCustomAnnotation  ──────────────────►   @MyCustomAnnotation  ──►  @MyCustomAnnotation
  public void updateUser()                   public void updateUser()   public void updateUser()
                                                                              │
                                                                              ▼
                                                                    You can READ this annotation
                                                                    using Reflection and run
                                                                    custom logic based on it ✅
```

- The annotation survives all the way to **runtime**
- You can use **Java Reflection** to detect it and do something based on it
- This is the one we'll use for our interceptor

---

## Visual Comparison of All Three Policies

```
                    SOURCE          CLASS           RUNTIME
                    ──────          ─────           ───────
In .java file?       ✅              ✅               ✅
In .class file?      ❌              ✅               ✅
Available at
runtime (JVM)?       ❌              ❌               ✅
Can write logic
based on it?         ❌              ❌               ✅
```

> 💡 **Interview Tip:** If asked *"what is @Retention and what are its types?"* — explain all three and make sure to say: *"For custom interceptors and AOP-based logic, we always use RetentionPolicy.RUNTIME because we need to read the annotation while the program is actually running."*

---

## Adding Fields to Your Custom Annotation

An annotation can also **carry data** — like passing arguments. This is very useful for interceptors because you might want to pass extra information (like a cache key name, a log level, etc.).

You define these as methods inside `@interface` — but Shreyansh calls them **fields** because that's how they behave:

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyCustomAnnotation {

    String key() default "defaultKeyName"; // a field with a default value
}
```

And you use it like this:

```java
public class User {
    @MyCustomAnnotation(key = "userKey")
    public void updateUser() {
        // business logic
    }
}
```

Your interceptor can then **read** the value `"userKey"` at runtime and act on it.

---

## Rules for Annotation Fields

Shreyansh explains that return types for these fields are **very restricted** — and this is intentional, to keep annotations lightweight:

| Allowed Return Types | Example |
|---|---|
| Primitive types | `int`, `boolean`, `double`, etc. |
| `String` | `String name()` |
| `Enum` | `MyEnum value()` |
| `Class<?>` | `Class<?> type() default String.class` |
| Another Annotation | `SomeAnnotation inner()` |
| Array of any above | `String[] tags()`, `int[] ids()` |

---

## A Full Example With Multiple Fields

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyCustomAnnotation {

    int intKey() default 0;
    String stringKey() default "defaultString";
    Class<?> classTypeKey() default String.class;
    MyCustomEnum enumKey() default MyCustomEnum.ENUM_VAL1;
    String[] stringArrayKey() default {"default1", "default2"};
    int[] intArrayKey() default {1, 2};
}
```

Using it:

```java
@MyCustomAnnotation(
    intKey = 10,
    stringKey = "user",
    classTypeKey = User.class,
    enumKey = MyCustomEnum.ENUM_VAL2
)
public void updateUser() {
    // business logic
}
```

---

## Quick Summary of Step 3

```
Custom Annotation:
  - Created using  →  public @interface MyCustomAnnotation { }

Two must-know Meta Annotations:
  - @Target     →  WHERE can this annotation be used (method, field, class...)
  - @Retention  →  HOW LONG does this annotation live

Three Retention Policies:
  - SOURCE   →  discarded by compiler, not even in .class file
  - CLASS    →  in .class file, but JVM ignores it at runtime
  - RUNTIME  →  survives to runtime, readable via Reflection ✅ (use this for interceptors)

Annotation Fields:
  - Defined as methods inside @interface
  - No parameters, no body
  - Return type is restricted (primitives, String, Enum, Class, Array of these)
  - Can have default values
  - Used to pass information to the interceptor at runtime
```

---

# Step 4 — Interceptor After the Controller (AOP-based, using Custom Annotation)

---

## The Goal

Now we want to intercept **after the controller has been invoked** — specifically, we want to intercept a **particular method call** inside our application (like a service method), based on a custom annotation we put on it.

This is where **Step 3 (custom annotations) + AOP** come together.

---

## The Full Picture Before We Write Code

Here's the scenario Shreyansh sets up:

```
  HTTP Request
      │
      ▼
  UserController.getUser()        ← controller gets invoked
      │
      ▼
  User.getUser()                  ← controller calls this method
      │                              this method has @MyCustomAnnotation on it
      ▼
  🔵 AOP Interceptor kicks in     ← sees the annotation, runs custom logic
      │
      ├── do something BEFORE the method runs
      │
      ▼
  actual User.getUser() runs      ← real business logic executes
      │
      ▼
  do something AFTER the method   ← interceptor continues after method
      │
      ▼
  Response goes back
```

---

## The Three Classes You Need

Shreyansh builds this with three pieces:

```
  1. MyCustomAnnotation     ← the label/trigger
  2. User                   ← the class whose method gets intercepted
  3. MyCustomInterceptor    ← the AOP aspect that does the interception
```

Let's go through each one.

---

## Piece 1 — The Custom Annotation

```java
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)   // ⬅️ RUNTIME because we need to read it while app is running
public @interface MyCustomAnnotation {

    String name() default "";         // a field to carry extra info
}
```

Simple — target is METHOD, retention is RUNTIME, and it has one field called `name`.

---

## Piece 2 — The Class Whose Method Gets Intercepted

```java
@Component
public class User {

    @MyCustomAnnotation(name = "user")   // ⬅️ putting our annotation here
    public void getUser() {
        System.out.println("get the user details");
    }
}
```

And the controller that calls it:

```java
@RestController
@RequestMapping(value = "/api/")
public class UserController {

    @Autowired
    User user;

    @GetMapping(path = "/getUser")
    public String getUser() {
        user.getUser();          // ⬅️ this triggers the interception
        return "success";
    }
}
```

So the flow is: request hits `UserController.getUser()` → which calls `User.getUser()` → which has `@MyCustomAnnotation` on it → which triggers our interceptor.

---

## Piece 3 — The AOP Interceptor (The Star of the Show)

```java
@Component
@Aspect     // ⬅️ tells Spring: this class contains AOP logic
public class MyCustomInterceptor {

    @Around("@annotation(com.conceptandcoding.learningspringboot.CustomInterceptor.MyCustomAnnotation)")
    public void invoke(ProceedingJoinPoint joinPoint) throws Throwable {

        System.out.println("do something before actual method");

        // getting the method that was intercepted
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();

        // checking if our annotation is present on that method
        if (method.isAnnotationPresent(MyCustomAnnotation.class)) {

            // reading the annotation and its field value
            MyCustomAnnotation annotation = method.getAnnotation(MyCustomAnnotation.class);
            System.out.println("name from annotation: " + annotation.name());
        }

        joinPoint.proceed();    // ⬅️ this is where the ACTUAL method runs

        System.out.println("do something after actual method");
    }
}
```

Let's break every part of this down carefully.

---

## Breaking Down the AOP Interceptor

### `@Aspect`
Tells Spring that this class is not a regular class — it contains **pointcuts and advices**. Spring will treat it specially.

---

### `@Around` — The Advice Type

```java
@Around("@annotation(com.conceptandcoding.learningspringboot.CustomInterceptor.MyCustomAnnotation)")
```

Two things here:

**1. `@Around`** means:
- Run my code **before** the actual method
- Then let the actual method run (`joinPoint.proceed()`)
- Then run my code **after** the actual method

It wraps around the actual method — like a sandwich:

```
  Your interceptor code (before)
          │
          ▼
  joinPoint.proceed()  →  actual method runs
          │
          ▼
  Your interceptor code (after)
```

**2. `@annotation(...)`** is the **pointcut expression**:
- It tells Spring: *"watch for this specific annotation"*
- The full path is the package path to your custom annotation class
- Whenever Spring sees a method call where that annotation is present → trigger this advice

> 💡 **Interview Tip:** `@Around` is the most powerful advice type because it controls both before AND after, and it even controls WHETHER the actual method runs at all (you could skip `joinPoint.proceed()` to block execution — useful for auth checks).

---

### `ProceedingJoinPoint`

```java
public void invoke(ProceedingJoinPoint joinPoint) throws Throwable {
```

`ProceedingJoinPoint` is an object that gives you **all the metadata about the method being intercepted**:
- What is the method name?
- What are its parameters?
- What annotations does it have?
- What is its return type?

You can think of it as a **snapshot of the method call** that your interceptor caught.

---

### Reading the Method & Its Annotation

```java
// Step 1: get the actual Method object from the joinPoint
Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();

// Step 2: check if our annotation is present
if (method.isAnnotationPresent(MyCustomAnnotation.class)) {

    // Step 3: get the annotation instance
    MyCustomAnnotation annotation = method.getAnnotation(MyCustomAnnotation.class);

    // Step 4: read its field value
    System.out.println("name from annotation: " + annotation.name()); // prints "user"
}
```

This is **Java Reflection** in action — and this is exactly why we needed `RetentionPolicy.RUNTIME`. Without it, the annotation wouldn't exist at runtime, and `isAnnotationPresent()` would always return false.

---

### `joinPoint.proceed()`

```java
joinPoint.proceed();
```

This is the line that actually **invokes the real method** (`User.getUser()`). Everything before this line runs first. Everything after this line runs once the real method is done.

If you remove this line — the actual method **never runs**. That's powerful but dangerous, so use it carefully.

---

## The Complete Flow Visualized

```
  HTTP Request → /api/getUser
        │
        ▼
  UserController.getUser()  runs
        │
        ▼
  calls User.getUser()
        │
        ▼  Spring sees @MyCustomAnnotation on User.getUser()
        │
        ▼
  AOP Proxy intercepts the call
        │
        ▼
  ┌─────────────────────────────────────────────┐
  │  MyCustomInterceptor.invoke() starts         │
  │                                             │
  │  "do something before actual method"  ✅    │
  │                                             │
  │  reads annotation → name = "user"     ✅    │
  │                                             │
  │  joinPoint.proceed() ──────────────────────►│──► User.getUser() runs
  │                                             │         │
  │  ◄───────────────────────────────────────── │◄── returns
  │                                             │
  │  "do something after actual method"   ✅    │
  │                                             │
  └─────────────────────────────────────────────┘
        │
        ▼
  "success" returned to client
```

---

## The Output

```
do something before actual method    ← interceptor ran first
name from annotation: user           ← annotation value was read
get the user details                 ← actual method ran
do something after actual method     ← interceptor ran after
```

---

## Quick Summary of Step 4

```
To intercept AFTER the controller, on specific methods:

  1. Create a @interface annotation
       - @Target(ElementType.METHOD)
       - @Retention(RetentionPolicy.RUNTIME)   ← must be RUNTIME
       - add fields if needed

  2. Put that annotation on the method you want to intercept

  3. Create an @Aspect class
       - @Around("@annotation(full.path.to.YourAnnotation)")
       - use ProceedingJoinPoint to access method metadata
       - use Reflection to read annotation values
       - call joinPoint.proceed() to invoke the real method
```

---

## Type 1 vs Type 2 — Side by Side

```
                  HandlerInterceptor        AOP @Aspect
                  ─────────────────         ───────────
  Where?          Before controller         After controller, on specific methods
  Interface?      HandlerInterceptor        No interface, just @Aspect + @Around
  Registered?     In WebMvcConfigurer       Auto-detected via @Component + @Aspect
  Trigger?        URL pattern               Custom annotation on method
  Use case?       Auth, request logging     Caching, method-level logging
  Controls flow?  Yes (return true/false)   Yes (skip joinPoint.proceed())
```

---

# Step 5 — Full Picture, Revision & Interview Tips

---

## The Complete Mental Model

Before anything else, let's put the **entire lecture into one single diagram** so your brain has one clear picture to hold onto:

```
  HTTP Request
      │
      ▼
┌─────────────────┐
│  Servlet        │
│  Container      │
│  (Tomcat)       │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Dispatcher      │
│ Servlet         │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────────────────┐
│  🔴 TYPE 1 INTERCEPTOR (HandlerInterceptor) │
│                                             │
│  preHandle()                                │
│  → return false = STOP, controller skipped  │
│  → return true  = continue ✅               │
└────────┬────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────┐
│           UserController.getUser()          │
│                                             │
│   calls → User.getUser()                   │
│                  │                          │
│    ┌─────────────▼──────────────────────┐   │
│    │ 🔵 TYPE 2 INTERCEPTOR (AOP @Aspect)│   │
│    │                                    │   │
│    │  before code runs                  │   │
│    │  → reads @MyCustomAnnotation       │   │
│    │  → joinPoint.proceed()             │   │
│    │     → actual User.getUser() runs   │   │
│    │  → after code runs                 │   │
│    └────────────────────────────────────┘   │
└─────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────┐
│  🔴 TYPE 1 continues                        │
│                                             │
│  postHandle()      → only on success        │
│  afterCompletion() → always (like finally)  │
└─────────────────────────────────────────────┘
         │
         ▼
    HTTP Response
```

---

## Full Revision — Everything in One Place

### What is an Interceptor?
A mediator that runs **before or after your actual code** — without modifying the actual code itself.

---

### Type 1 — HandlerInterceptor (Before Controller)

```
STEP 1: Create the interceptor
─────────────────────────────
@Component
public class MyCustomInterceptor implements HandlerInterceptor {
    preHandle()       → before controller  | return true/false
    postHandle()      → after controller   | only on success
    afterCompletion() → after controller   | always, like finally
}

STEP 2: Register it
───────────────────
@Configuration
public class AppConfig implements WebMvcConfigurer {
    override addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(myCustomInterceptor)
                .addPathPatterns("/api/*")
                .excludePathPatterns("/api/updateUser", "/api/deleteUser")
    }
}
```

---

### Type 2 — AOP @Aspect (After Controller, on Specific Methods)

```
STEP 1: Create the custom annotation
─────────────────────────────────────
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyCustomAnnotation {
    String name() default "";
}

STEP 2: Put annotation on the method you want to intercept
──────────────────────────────────────────────────────────
@MyCustomAnnotation(name = "user")
public void getUser() { ... }

STEP 3: Create the AOP interceptor
────────────────────────────────────
@Component
@Aspect
public class MyCustomInterceptor {
    @Around("@annotation(full.path.MyCustomAnnotation)")
    public void invoke(ProceedingJoinPoint joinPoint) {
        // before
        joinPoint.proceed()   // actual method runs
        // after
    }
}
```

---

### @Retention — The Three Policies

```
SOURCE   → annotation gone after compilation
           example: @Override

CLASS    → annotation in .class file, JVM ignores it at runtime
           (this is the default if you don't specify @Retention)

RUNTIME  → annotation survives to runtime, readable via Reflection
           use this for interceptors ✅
```

---

### Annotation Fields — Rules

```
✅ Allowed return types:
   primitive (int, boolean, double...)
   String
   Enum
   Class<?>
   Annotation
   Array of any of the above

❌ NOT allowed:
   Objects (like List, Map, custom classes)
   → keeps annotations lightweight
```

---

## When to Use Which Interceptor?

This is something Shreyansh implies throughout the lecture — here's it stated clearly:

| Situation | Use |
|---|---|
| Check auth/token before any API is hit | Type 1 (HandlerInterceptor) |
| Log every incoming HTTP request | Type 1 (HandlerInterceptor) |
| Apply caching to a specific method | Type 2 (AOP @Aspect) |
| Log what a specific method does | Type 2 (AOP @Aspect) |
| Block a request entirely before controller | Type 1 (return false in preHandle) |
| Read extra info passed via annotation | Type 2 (read annotation fields via Reflection) |

---

## Interview Tips — All in One Place

Here are all the interview-worthy points from this lecture:

---

> 💡 **Q: What is an interceptor in Spring Boot?**

A mediator that runs before or after your actual code. Spring Boot provides two ways — `HandlerInterceptor` for intercepting before the controller, and AOP (`@Aspect`) for intercepting specific method calls after the controller.

---

> 💡 **Q: What is the difference between preHandle, postHandle and afterCompletion?**

`preHandle` runs before the controller. It returns a boolean — false stops the request entirely. `postHandle` runs after the controller but only if there's no exception. `afterCompletion` always runs regardless of success or exception — similar to a `finally` block in Java.

---

> 💡 **Q: What are the retention policies in Java annotations?**

Three types — SOURCE (discarded by compiler, not in .class file), CLASS (in .class file but JVM ignores it at runtime — this is the default), and RUNTIME (survives to runtime and is readable via Reflection — used for custom interceptors).

---

> 💡 **Q: What is the difference between @Before and @Around in AOP?**

`@Before` only runs before the actual method. `@Around` wraps the entire method — it runs before, then calls `joinPoint.proceed()` to trigger the actual method, then runs after. `@Around` also gives you control to skip the actual method entirely by not calling `proceed()`.

---

> 💡 **Q: Why do we need RetentionPolicy.RUNTIME for custom interceptors?**

Because the interceptor reads the annotation **while the application is running** using Java Reflection (`isAnnotationPresent()`, `getAnnotation()`). If the retention is SOURCE or CLASS, the annotation won't exist at runtime and Reflection won't find it.

---

> 💡 **Q: What is a ProceedingJoinPoint?**

It's an object available in `@Around` advice that represents the intercepted method call. It gives you metadata about the method (name, parameters, annotations, return type) and lets you invoke the actual method via `joinPoint.proceed()`.

---

> 💡 **Q: Why are annotation field return types restricted?**

Annotations are designed to be very lightweight metadata. Allowing complex objects like Lists or Maps would make them heavy and misuse their purpose. So Java restricts return types to primitives, String, Enum, Class, other annotations, and arrays of these.

---

## One Last Thing Shreyansh Says

> *"Practice this out a lot — because in future videos we might have to write a few interceptors. It's very much required for logging, caching, authentication, authorization — everything."*

So this lecture is not just a standalone topic. It's the **foundation** for almost every advanced Spring Boot concept coming next. The clearer this is, the easier those topics will be.

---

## Your Complete Notes at a Glance

```
Custom Interceptors in Spring Boot
───────────────────────────────────

Definition:
  A mediator invoked before or after your actual code.

Type 1 — HandlerInterceptor:
  → implements HandlerInterceptor
  → 3 methods: preHandle, postHandle, afterCompletion
  → registered in WebMvcConfigurer via addInterceptors()
  → triggered by URL pattern

Type 2 — AOP @Aspect:
  → @interface for custom annotation (@Target + @Retention RUNTIME)
  → @Aspect + @Around + pointcut via @annotation()
  → triggered by annotation on a method
  → reads annotation values via Reflection at runtime

Key Differences:
  postHandle    → after controller, success only
  afterCompletion → after controller, always (like finally)

Retention Policies:
  SOURCE  → gone after compile
  CLASS   → in bytecode, ignored at runtime (default)
  RUNTIME → survives to runtime ✅ use for interceptors

Use cases:
  Auth, logging → Type 1
  Caching, method-level logic → Type 2
```

---

And that's the **complete lecture** on Custom Interceptors in Spring Boot! 🎉

These notes cover everything Shreyansh taught — the concept, the code, the internal flow, and the interview angles. Whenever you're ready for the next lecture, just let me know! 🚀