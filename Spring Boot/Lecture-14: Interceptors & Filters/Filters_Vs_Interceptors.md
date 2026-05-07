# Step 1 — The Core Concept & One-Liner Definitions

---

## Why are we even learning this?

If you've worked with Spring Boot, you've likely written controllers, services, and repositories. But sometimes you need logic that runs **before** your actual business code — things like:

- Checking if the user is authenticated
- Logging every incoming request
- Modifying headers
- Validating tokens

Now the question is — **where exactly do you put this logic?** That's where Filters and Interceptors come in. Both let you intercept HTTP requests and responses, but they sit at **different points** in the request lifecycle. Knowing which one to use, and when, is what this lecture is all about.

---

## The Two One-Liner Definitions

The instructor starts with these two definitions right away. Even if they don't fully make sense yet, just read them — they'll become crystal clear as we go further.

---

### 🔹 Filter
> **Intercepts the HTTP request and response *before* they reach the Servlet.**

---

### 🔹 Interceptor
> **Specific to the Spring Framework. Intercepts the HTTP request and response *after the Servlet* but *before the Controller*.**

---

So in simple terms:

```
Request comes in
     ↓
  [ Filter ]         ← before the Servlet
     ↓
  [ Servlet ]
     ↓
  [ Interceptor ]    ← after Servlet, before Controller
     ↓
  [ Controller ]
```

---

That's it for Step 1 — clean and simple. The real depth comes in Step 2, where we look at the **full architecture diagram** and understand exactly where each one fits in.

# Step 2 — Understanding the Architecture (with Diagram)

---

## The Big Picture — What happens when a request comes in?

The instructor builds this understanding layer by layer. Let's do the same.

---

## Layer 1 — The Basic Flow (from the first Spring Boot video)

When a request hits your Spring Boot application, here's what happens at a high level:

```
Client sends HTTP Request
          ↓
   [ Servlet Container ]   ← e.g. Tomcat
          ↓
   [ Servlet ]             ← processes the request
          ↓
   [ Controller ]          ← your business logic
          ↓
   Response goes back to Client
```

In Spring Boot specifically, the servlet used is called **DispatcherServlet**. It's the one that decides:
1. Which controller should handle this request?
2. Creates an instance of that controller
3. Invokes the appropriate method in that controller
4. Sends back the response

---

## Layer 2 — Now Where Do Filters & Interceptors Fit In?

The instructor now **drills deeper** into this diagram to show exactly where Filters and Interceptors live.

```
Client sends HTTP Request
          ↓
  [ Servlet Container / Tomcat ]
          ↓
  ┌─────────────────────────┐
  │      FILTER CHAIN       │  ← Filters live HERE
  │  Filter 1               │     (before Servlet is chosen)
  │      ↓                  │
  │  Filter 2               │
  │      ↓                  │
  │  Filter n               │
  └─────────────────────────┘
          ↓
  ┌─────────────────────────────────────────┐
  │            SERVLETS                     │
  │                                         │
  │  [ Dispatcher Servlet ] ← Spring Boot   │
  │            ↓                            │
  │     [ INTERCEPTORS ]  ← live HERE       │
  │            ↓                            │
  │      [ Controller ]                     │
  │                                         │
  │  [ Servlet 2 ]                          │
  │  [ Servlet n ]                          │
  └─────────────────────────────────────────┘
          ↓
  Response goes back to Client
```

---

## What this diagram tells us

### Filters:
- They run **inside the Servlet Container** (Tomcat)
- They execute **before** the container even decides which Servlet to hand the request to
- They are **not specific** to any one Servlet — they run for all of them
- This is why Spring Security uses Filters — because security checks should apply **regardless** of which Servlet handles the request

### Interceptors:
- They run **after** the Servlet Container has already chosen a Servlet
- In Spring Boot's case — after **DispatcherServlet** is chosen
- They execute **before** the request reaches your Controller
- They are **specific** to that one Servlet (DispatcherServlet in Spring Boot)
- This is why Interceptors are considered a **Spring-specific concept**

---

## The Key Takeaway from this Diagram

| | Filter | Interceptor |
|---|---|---|
| Lives at | Servlet Container level | Servlet level (DispatcherServlet) |
| Runs | Before Servlet is chosen | After Servlet, before Controller |
| Specific to | No specific Servlet | Spring's DispatcherServlet |
| Framework | Plain Java / Jakarta EE | Spring Framework |

---

This diagram is the **heart of this entire lecture**. Everything else — the code, the ordering, the use cases — all builds on top of this mental model.

# Step 3 — What is a Servlet? (Deep Dive)

---

## Why is the instructor explaining Servlets here?

After seeing the diagram in Step 2, a natural question pops up in your head:

> *"What exactly IS a Servlet? And can there really be more than one?"*

The instructor anticipates this and addresses it right here. Understanding Servlets is **crucial** to understanding why Filters and Interceptors exist where they do.

---

## So, What is a Servlet?

> **A Servlet is simply a Java class that accepts an incoming HTTP request, processes it, and returns a response.**

That's it. Nothing fancy. It's just Java code whose job is — take request, do something, give back response.

---

## Can we have Multiple Servlets?

**Yes!** And this is where it gets interesting.

Imagine you're building a **monolithic application** (one big application that does everything). You might have:

```
Your Application
      │
      ├── REST APIs        → Servlet 1 handles this  (configured for /api/*)
      │
      ├── SOAP APIs        → Servlet 2 handles this
      │
      ├── File Uploads     → Servlet 3 handles this
      │
      └── Static Content   → Servlet 4 handles this  (images, HTML, etc.)
```

Each Servlet tells the Servlet Container:
> *"Hey, I can handle requests that match THIS pattern."*

The Servlet Container looks at the incoming request URL and decides:
> *"Okay, this request goes to Servlet 1. That one goes to Servlet 3."*

---

## So what is DispatcherServlet then?

In **Spring Boot**, especially when building **microservices**, you rarely need multiple Servlets. Your app usually does one focused thing — handle REST APIs.

So Spring Boot gives you one Servlet that handles everything — the **DispatcherServlet**.

```
DispatcherServlet
      │
      │  default configuration → handles "/*"
      │  (meaning ANY request that comes in)
      │
      ├── /api/user     → UserController
      ├── /api/order    → OrderController
      └── /api/product  → ProductController
```

The DispatcherServlet is configured by default to handle **all requests** (`/*`). It then uses Spring's internal logic (HandlerMapping) to figure out which **Controller** should handle which request.

---

## Old Way vs New Way — How things changed

The instructor makes a really important historical point here:

### Old Way (2016-2017 era, monolithic):
```
Servlet (e.g. REST Servlet)
      │
      ├── if /user   → run user logic here
      ├── if /order  → run order logic here  
      └── if /product→ run product logic here
      
  (All logic lived INSIDE the Servlet itself — like a big if-else factory)
```

### New Way (Spring Boot, microservices):
```
DispatcherServlet
      │
      ├── /user    → UserController    (separate class, clean)
      ├── /order   → OrderController   (separate class, clean)
      └── /product → ProductController (separate class, clean)

  (Logic is split into clean Controllers — Servlet just routes)
```

So Controllers are basically Spring Boot's modern, clean way of handling what old Servlets used to do with messy if-else blocks.

---

## Putting it all together — The Complete Mental Model

```
HTTP Request comes in
        ↓
[ Tomcat / Servlet Container ]
        ↓
[ Filter Chain ]          ← runs BEFORE servlet is chosen
  Filter 1
  Filter 2
  Filter n
        ↓
[ Servlet Container decides which Servlet to use ]
        ↓
[ DispatcherServlet ]     ← Spring Boot's Servlet (handles /*)
        ↓
[ Interceptors ]          ← runs AFTER servlet chosen, BEFORE controller
  Interceptor 1
  Interceptor 2
        ↓
[ Controller ]            ← your actual business logic
        ↓
Response travels back in REVERSE order
```

---

## 🔑 Key Point the Instructor Emphasizes

> Interceptors **cannot** intercept requests going to Servlet 2 or Servlet 3. They only intercept requests going to the **specific Servlet they are attached to** — in Spring Boot's case, the DispatcherServlet.

This is exactly why:
- **Filters** = generic, servlet-agnostic logic
- **Interceptors** = specific to Spring Boot's DispatcherServlet

---

Now that we fully understand the architecture and what Servlets are, Step 4 is where the instructor gives you the **practical decision guide** — when to actually pick Filter vs Interceptor in real projects.

# Step 4 — When to Use Filter vs Interceptor (Decision Guide)

---

## The Core Question

Both Filters and Interceptors have access to the HTTP request and response. Both can:
- Read/modify request headers
- Read/modify response headers
- Block a request from going further

So the instructor is very clear here:

> *"I don't consider Filter or Interceptor as better or worse than each other. Both are totally different, used at totally different places."*

The decision comes down to **one simple question:**

---

## The One Question to Ask Yourself

> **"Is this logic generic for ALL servlets, or is it specific to ONE particular servlet?"**

```
Is your logic generic
for ALL servlets?
        │
        ├── YES → Use FILTER
        │
        └── NO, it's specific
            to one Servlet
            (DispatcherServlet) → Use INTERCEPTOR
```

---

## Real World Use Cases

### Use a Filter when:

These are things that should apply **regardless** of which Servlet handles the request. They are cross-cutting, application-wide concerns.

```
┌─────────────────────────────────────────────┐
│              USE FILTER FOR:                │
│                                             │
│  ✅ Security checks (Spring Security)       │
│     → Must apply to ALL requests            │
│     → Not specific to any one Servlet       │
│                                             │
│  ✅ Logging every HTTP request/response     │
│     → You want ALL requests logged          │
│                                             │
│  ✅ Compression (GZIP)                      │
│     → Apply to all responses                │
│                                             │
│  ✅ CORS headers                            │
│     → Must be handled before Servlet        │
│                                             │
│  ✅ Request/Response encoding               │
│     → Generic, applies everywhere           │
└─────────────────────────────────────────────┘
```

**The instructor's best example:**
> Spring Security uses Filters — because authentication and authorization are concerns that apply to **all HTTP requests**, irrespective of which Servlet handles them. It makes no sense to tie security to one specific Servlet.

---

### Use an Interceptor when:

These are things specific to your **Spring Boot application's** DispatcherServlet — things tied to your Controllers and business logic.

```
┌─────────────────────────────────────────────┐
│           USE INTERCEPTOR FOR:              │
│                                             │
│  ✅ Logging specific to your REST APIs      │
│     → Only for DispatcherServlet requests   │
│                                             │
│  ✅ Checking roles/permissions              │
│     → After authentication (post-Filter)    │
│     → Specific to your app's controllers    │
│                                             │
│  ✅ Tracking API execution time             │
│     → preHandle starts timer                │
│     → postHandle/afterCompletion stops it   │
│                                             │
│  ✅ Injecting common data into Model        │
│     → Specific to Spring MVC controllers    │
└─────────────────────────────────────────────┘
```

---

## Side-by-Side Comparison

| Question | Filter | Interceptor |
|---|---|---|
| Where does it sit? | Before Servlet is chosen | After Servlet, before Controller |
| Is it Spring specific? | ❌ No — plain Java/Jakarta | ✅ Yes — Spring Framework only |
| Which Servlet does it apply to? | ALL Servlets | ONE specific Servlet |
| Can you have multiple? | ✅ Yes, with ordering | ✅ Yes, with ordering |
| Can it block the request? | ✅ Yes | ✅ Yes (via preHandle returning false) |
| Used by Spring Security? | ✅ Yes | ❌ No |
| Access to Spring context? | ❌ Limited | ✅ Full access |

---

## The Instructor's Simple Rule — Memorize This

```
GENERIC logic (doesn't care about Servlet)
        → FILTER

SPECIFIC logic (tied to DispatcherServlet / Spring Boot)
        → INTERCEPTOR
```

---

## 🔑 One More Important Point

The instructor points out that **Interceptor has three hooks** you can use — which gives you more fine-grained control than a Filter:

```
Request comes in
      ↓
[ preHandle ]       → runs BEFORE controller
                      returns true  → continue
                      returns false → STOP, nothing else runs
      ↓
[ Controller runs ]
      ↓
[ postHandle ]      → runs AFTER controller, before response is sent
      ↓
[ afterCompletion ] → runs AFTER response is sent (cleanup work)
```

A Filter's `doFilter()` is simpler — it's just one method where you write logic before and after calling `filterChain.doFilter()`.

---

Now that you know the **what, why and when** — Step 5 is where we get into the **code**. The instructor shows how to wire up multiple Interceptors and control their ordering.

# Step 5 — Code: Multiple Interceptors & Their Ordering

---

## Quick Recap from the Previous Video

The instructor mentions that in his previous video, he already showed how to create **one interceptor** and how `preHandle`, `postHandle`, and `afterCompletion` work. So here he builds on top of that and shows:

1. How to add **more than one** Interceptor
2. How **ordering** works between them
3. What the actual **execution flow** looks like

---

## How to Register Multiple Interceptors

You register Interceptors inside a `@Configuration` class that implements `WebMvcConfigurer`. You override the `addInterceptors` method and simply **add them one by one** — the order you add them is the order they execute.

```java
@Configuration
public class AppConfig implements WebMvcConfigurer {

    @Autowired
    MyCustomInterceptor1 myCustomInterceptor1;

    @Autowired
    MyCustomInterceptor2 myCustomInterceptor2;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // Interceptor 1 registered first → runs first
        registry.addInterceptor(myCustomInterceptor1)
                .addPathPatterns("/api/*")              // apply to these URLs
                .excludePathPatterns("/api/updateUser",
                                     "/api/deleteUser"); // skip these URLs

        // Interceptor 2 registered second → runs second
        registry.addInterceptor(myCustomInterceptor2)
                .addPathPatterns("/api/*")
                .excludePathPatterns("/api/updateUser");
    }
}
```

### Key things to notice:
- The **sequence you register** them in `addInterceptors()` is the sequence they run
- You can control **which URLs** each interceptor applies to using `addPathPatterns()`
- You can **exclude specific URLs** using `excludePathPatterns()`
- Each interceptor can have its **own set of URL rules** — they don't have to be the same

---

## How the Execution Flow Works

This is the most important part. The instructor explains the request and response flow very carefully.

```
HTTP Request comes in
        ↓
[ preHandle → Interceptor 1 ]    ← request going IN, Interceptor 1 first
        ↓
[ preHandle → Interceptor 2 ]    ← request going IN, Interceptor 2 second
        ↓
[ Controller runs ]               ← your actual business logic
        ↓
[ postHandle → Interceptor 2 ]   ← response going OUT, Interceptor 2 first
        ↓
[ postHandle → Interceptor 1 ]   ← response going OUT, Interceptor 1 second
        ↓
[ afterCompletion → Interceptor 2 ]
        ↓
[ afterCompletion → Interceptor 1 ]
        ↓
Response sent back to Client
```

### The Pattern to Remember:
```
Request  flows → IN ORDER         (1 → 2 → Controller)
Response flows → REVERSE ORDER    (2 → 1)
```

Think of it like a **stack** — what goes in first, comes out last.

---

## Actual Output the Instructor Shows

When both interceptors are running, this is what you see in the logs:

```
inside pre handle Method - MyCustomInterceptor1
inside pre handle Method - MyCustomInterceptor2
hitting db to get the userdata        ← Controller ran
inside post handle method - MyCustomInterceptor2
inside post handle method - MyCustomInterceptor1
inside after completion method - MyCustomInterceptor2
inside after completion method - MyCustomInterceptor1
```

This perfectly matches the flow diagram above — confirms that **request goes 1→2, response comes back 2→1**.

---

## What Happens if preHandle Returns False?

The instructor specifically calls this out as an important behavior:

```java
@Override
public boolean preHandle(HttpServletRequest request,
                          HttpServletResponse response,
                          Object handler) {

    // if you return false here...
    return false;
}
```

```
[ preHandle → Interceptor 1 ] returns FALSE
        ↓
  ┌─────────────────────────────────────┐
  │  EVERYTHING STOPS HERE              │
  │  - Interceptor 2 will NOT run       │
  │  - Controller will NOT run          │
  │  - postHandle will NOT run          │
  └─────────────────────────────────────┘
```

> This is actually how you can **block a request** at the Interceptor level — for example, if a token is invalid or a user doesn't have permission, just return `false` from `preHandle` and nothing downstream will execute.

---

## 🔑 Key Takeaways from This Step

```
1. Register interceptors in addInterceptors() 
   → order of registration = order of execution

2. Request flows FORWARD  (Interceptor 1 → 2 → Controller)
   Response flows BACKWARD (Controller → 2 → 1)

3. preHandle  → return true  = continue
               return false = STOP everything

4. You can fine-tune WHICH URLs each 
   interceptor applies to or skips
```

---

Step 6 is where the instructor shows the **Filter code** — how to create filters, the three methods inside a Filter, and how to control ordering using `FilterRegistrationBean`.

# Step 6 — Code: How to Add Filters

---

## How a Filter is Different to Write vs Interceptor

With Interceptors, Spring manages everything — you just register them in a `@Configuration` class and Spring wires it all up.

Filters are slightly different. They come from **Jakarta EE (plain Java)**, not Spring. So you implement the `Filter` interface directly, and register them using a `FilterRegistrationBean` in your config.

---

## Step 1 — Create Your Filter Class

A Filter has **three methods** you need to implement:

```java
import jakarta.servlet.*;
import java.io.IOException;

public class MyFilter1 implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Called ONCE when the Filter object is first created
        // Use this to do any one-time setup/initialization
        // In practice, the instructor says he rarely uses this
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain filterChain)
                         throws IOException, ServletException {

        // YOUR LOGIC BEFORE passing request forward
        System.out.println("MyFilter1 inside");

        // This line hands off to the NEXT thing in the chain
        // (next Filter, or actual code logic if no more Filters)
        filterChain.doFilter(servletRequest, servletResponse);

        // YOUR LOGIC AFTER the request has been processed
        System.out.println("MyFilter1 completed");
    }

    @Override
    public void destroy() {
        // Called when the Filter is being taken down/destroyed
        // Cleanup work goes here
        Filter.super.destroy();
    }
}
```

---

## Understanding `doFilter()` — The Heart of a Filter

The instructor explains this very carefully. Think of `filterChain.doFilter()` as:

> *"I'm done with my part. Now process everything that comes after me."*

```
MyFilter1.doFilter() is called
        │
        ├── Code ABOVE filterChain.doFilter()
        │   → runs on the way IN (request coming in)
        │
        ├── filterChain.doFilter()
        │   → hands off to whatever is next:
        │       - If Filter2 exists → goes to Filter2
        │       - If no more Filters → goes to actual business logic
        │
        └── Code BELOW filterChain.doFilter()
            → runs on the way OUT (response going out)
```

So the structure of `doFilter()` is essentially:

```java
// PRE-processing (request going IN)
System.out.println("MyFilter1 inside");

filterChain.doFilter(request, response);  // ← the pivot point

// POST-processing (response going OUT)
System.out.println("MyFilter1 completed");
```

---

## Step 2 — Create Your Second Filter

Same structure, just different logic:

```java
import jakarta.servlet.*;
import java.io.IOException;

public class MyFilter2 implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain filterChain)
                         throws IOException, ServletException {

        System.out.println("MyFilter2 inside");
        filterChain.doFilter(servletRequest, servletResponse);
        System.out.println("MyFilter2 completed");
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
```

---

## Step 3 — Register Your Filters with Ordering

This is where Filters differ from Interceptors. You use `FilterRegistrationBean` to register them and **explicitly set an order number**.

```java
@Configuration
public class AppConfig {

    @Bean
    public FilterRegistrationBean<MyFilter1> myFirstFilter() {
        FilterRegistrationBean<MyFilter1> filterRegistrationBean
                                = new FilterRegistrationBean<>();

        filterRegistrationBean.setFilter(new MyFilter1()); // which filter
        filterRegistrationBean.addUrlPatterns("/*");        // which URLs
        filterRegistrationBean.setOrder(1);                 // order number

        return filterRegistrationBean;
    }

    @Bean
    public FilterRegistrationBean<MyFilter2> mySecondFilter() {
        FilterRegistrationBean<MyFilter2> filterRegistrationBean
                                = new FilterRegistrationBean<>();

        filterRegistrationBean.setFilter(new MyFilter2()); // which filter
        filterRegistrationBean.addUrlPatterns("/*");        // which URLs
        filterRegistrationBean.setOrder(2);                 // order number

        return filterRegistrationBean;
    }
}
```

### Important thing to notice:
- **Lower order number = runs first**
- So `MyFilter1` with order `1` runs **before** `MyFilter2` with order `2`
- The instructor deliberately swapped orders earlier in the video (Filter1=2, Filter2=1) just to **prove** that it's the order number that controls execution — not the class name

---

## The Execution Flow for Filters

```
HTTP Request comes in
        ↓
[ MyFilter1 - "inside" ]      ← order 1, runs first on way IN
        ↓
[ MyFilter2 - "inside" ]      ← order 2, runs second on way IN
        ↓
[ Business Logic / Controller ]
        ↓
[ MyFilter2 - "completed" ]   ← reverse order on way OUT
        ↓
[ MyFilter1 - "completed" ]   ← reverse order on way OUT
        ↓
Response sent to Client
```

Same **stack pattern** as Interceptors — what goes in first, comes out last.

---

## Actual Output the Instructor Shows

```
MyFilter1 inside
MyFilter2 inside
hitting db to get the userdata    ← business logic ran
MyFilter2 completed
MyFilter1 completed
```

---

## Three Methods — Quick Summary

```
┌─────────────────────────────────────────────────────┐
│                  FILTER METHODS                     │
│                                                     │
│  init()      → runs ONCE when filter is created     │
│                one-time setup, rarely used          │
│                                                     │
│  doFilter()  → runs on EVERY request                │
│                this is where your logic goes        │
│                call filterChain.doFilter() to       │
│                pass control to next filter/logic    │
│                                                     │
│  destroy()   → runs ONCE when filter is destroyed   │
│                cleanup work                         │
└─────────────────────────────────────────────────────┘
```

---

## Filter vs Interceptor — Code Structure Comparison

```
FILTER                          INTERCEPTOR
──────────────────────          ──────────────────────
implements Filter               implements HandlerInterceptor

init()      → one time setup    no equivalent
doFilter()  → main logic        preHandle()     → before controller
                                postHandle()    → after controller
destroy()   → cleanup           afterCompletion → after response

Registered via                  Registered via
FilterRegistrationBean          addInterceptors() in WebMvcConfigurer

Order set via                   Order set via
setOrder(number)                sequence in addInterceptors()
```

---

We're almost at the finish line! Step 7 brings it all together — the instructor shows what happens when **both Filters and Interceptors run together**, and the complete end-to-end flow.

# Step 7 — Combined Flow: Filters + Interceptors Together

---

## The Complete Picture

This is where the instructor brings **everything together**. When your Spring Boot app has both Filters and Interceptors configured, here is the exact sequence of what runs and when.

---

## The Full End-to-End Flow

```
HTTP Request comes in
        ↓
[ Servlet Container / Tomcat ]
        │
        ▼
┌───────────────────────┐
│      FILTER CHAIN     │
│                       │
│  MyFilter1 - inside   │  ← order 1, runs first
│        ↓              │
│  MyFilter2 - inside   │  ← order 2, runs second
└───────────────────────┘
        │
        ▼
[ Servlet Container hands request to DispatcherServlet ]
        │
        ▼
┌─────────────────────────────────────┐
│         DISPATCHER SERVLET          │
│                                     │
│  preHandle → Interceptor 1          │  ← Interceptor 1 first
│        ↓                            │
│  preHandle → Interceptor 2          │  ← Interceptor 2 second
│        ↓                            │
│      [ Controller runs ]            │  ← your business logic
│        ↓                            │
│  postHandle → Interceptor 2         │  ← reverse order
│        ↓                            │
│  postHandle → Interceptor 1         │  ← reverse order
│        ↓                            │
│  afterCompletion → Interceptor 2    │  ← reverse order
│        ↓                            │
│  afterCompletion → Interceptor 1    │  ← reverse order
└─────────────────────────────────────┘
        │
        ▼
┌───────────────────────┐
│      FILTER CHAIN     │
│  (response going out) │
│                       │
│  MyFilter2 - completed│  ← reverse order on way out
│        ↓              │
│  MyFilter1 - completed│  ← reverse order on way out
└───────────────────────┘
        │
        ▼
Response sent back to Client
```

---

## Actual Output the Instructor Shows

This is the exact console output when both Filters and Interceptors are active:

```
MyFilter1 inside
MyFilter2 inside
inside pre handle Method - MyCustomInterceptor1
inside pre handle Method - MyCustomInterceptor2
hitting db to get the userdata            ← Controller ran
inside post handle method - MyCustomInterceptor2
inside post handle method - MyCustomInterceptor1
inside after completion method - MyCustomInterceptor2
inside after completion method - MyCustomInterceptor1
MyFilter2 completed
MyFilter1 completed
```

---

## Breaking Down the Output Line by Line

```
MyFilter1 inside                ← Filter layer, request going IN
MyFilter2 inside                ← Filter layer, request going IN
                                      ↓ handed to DispatcherServlet
pre handle - Interceptor1       ← Interceptor layer, request going IN
pre handle - Interceptor2       ← Interceptor layer, request going IN
                                      ↓ handed to Controller
hitting db to get the userdata  ← Controller/business logic ran
                                      ↓ response coming OUT
post handle - Interceptor2      ← Interceptor layer, REVERSE order
post handle - Interceptor1      ← Interceptor layer, REVERSE order
afterCompletion - Interceptor2  ← Interceptor layer, REVERSE order
afterCompletion - Interceptor1  ← Interceptor layer, REVERSE order
                                      ↓ back to Filter layer
MyFilter2 completed             ← Filter layer, REVERSE order
MyFilter1 completed             ← Filter layer, REVERSE order
```

---

## The One Universal Pattern

No matter how many Filters or Interceptors you add, this pattern **never changes:**

```
┌─────────────────────────────────────────┐
│                                         │
│   REQUEST  → flows in FORWARD order     │
│   RESPONSE → flows in REVERSE order     │
│                                         │
│   Think of it as a STACK:               │
│   What goes IN first, comes OUT last    │
│                                         │
└─────────────────────────────────────────┘
```

---

## Layered Mental Model — All Concepts Together

```
┌─────────────────────────────────────────────────────────┐
│                   YOUR SPRING BOOT APP                  │
│                                                         │
│  ┌───────────────────────────────────────────────────┐  │
│  │              SERVLET CONTAINER (Tomcat)           │  │
│  │                                                   │  │
│  │  ┌─────────────────┐                              │  │
│  │  │   FILTER CHAIN  │  ← Jakarta EE / Plain Java   │  │
│  │  │   Filter 1      │     Generic, all servlets    │  │
│  │  │   Filter 2      │     Spring Security lives    │  │
│  │  │   Filter n      │     here                     │  │
│  │  └────────┬────────┘                              │  │
│  │           │                                       │  │
│  │  ┌────────▼──────────────────────────────────┐    │  │
│  │  │         DISPATCHER SERVLET                │    │  │
│  │  │   ┌─────────────────────────────────┐     │    │  │
│  │  │   │        INTERCEPTORS             │     │    │  │
│  │  │   │  Spring specific, this servlet  │     │    │  │
│  │  │   │  Interceptor 1                  │     │    │  │
│  │  │   │  Interceptor 2                  │     │    │  │
│  │  │   └──────────────┬──────────────────┘     │    │  │
│  │  │                  │                        │    │  │
│  │  │         [ CONTROLLER ]                    │    │  │
│  │  │           Your business logic             │    │  │
│  │  └───────────────────────────────────────────┘    │  │
│  │                                                   │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

---

## Quick Decision Reminder

```
Need logic that applies to ALL servlets?
→ FILTER

Need logic specific to DispatcherServlet / Spring Boot controllers?
→ INTERCEPTOR

Not sure?
→ Ask: "Does this care about which Servlet handles it?"
    NO  → Filter
    YES → Interceptor
```

---

We're at the final step! Step 8 is the **Interview Tips & Quick Revision** — the instructor wraps up with key points that are commonly asked in interviews, plus a clean one-page summary of everything.

# Step 8 — Interview Tips & Quick Revision

---

## What Interviewers Typically Ask

These are the most common ways this topic shows up in interviews. The instructor indirectly covers all of these through the lecture.

---

### Question 1: "What is the difference between a Filter and an Interceptor?"

**The answer they're looking for:**

```
FILTER
→ Lives at the Servlet Container level
→ Runs BEFORE a Servlet is chosen
→ Applies to ALL Servlets
→ Part of Jakarta EE / plain Java
→ Used for generic, cross-cutting concerns

INTERCEPTOR
→ Lives inside DispatcherServlet
→ Runs AFTER Servlet is chosen, BEFORE Controller
→ Specific to ONE Servlet (DispatcherServlet in Spring Boot)
→ Part of Spring Framework
→ Used for Spring-specific, application-level concerns
```

---

### Question 2: "Where does Spring Security sit — Filter or Interceptor? Why?"

**Answer:**
> Spring Security uses **Filters** — because security is a generic concern that must apply to **all incoming requests**, regardless of which Servlet handles them. It makes no sense to tie security checks to one specific Servlet. By living at the Filter level, Spring Security can intercept and block requests **before** they even reach any Servlet.

---

### Question 3: "Can you have multiple Filters and Interceptors? How do you control their order?"

**Answer:**
```
FILTERS:
→ Yes, multiple Filters are possible
→ Order controlled via setOrder(number) in FilterRegistrationBean
→ Lower number = runs first

INTERCEPTORS:
→ Yes, multiple Interceptors are possible
→ Order controlled by sequence of registration
  in addInterceptors() method
→ First registered = runs first
```

---

### Question 4: "What happens if preHandle() returns false?"

**Answer:**
```
If preHandle() returns false:
→ The current Interceptor stops
→ Next Interceptor will NOT run
→ Controller will NOT run
→ postHandle() will NOT run
→ afterCompletion() will NOT run

Use case: Token validation failed,
          user not authorized →
          return false to block the request
```

---

### Question 5: "What is the execution order when both Filters and Interceptors are present?"

**Answer — just write this flow:**
```
Request IN:
Filter1 → Filter2 → Interceptor1(preHandle)
→ Interceptor2(preHandle) → Controller

Response OUT:
Interceptor2(postHandle) → Interceptor1(postHandle)
→ Interceptor2(afterCompletion)
→ Interceptor1(afterCompletion)
→ Filter2 → Filter1
```

---

### Question 6: "What are the three methods in a Filter and what do they do?"

```
init()      → Called ONCE when Filter is created
              One-time initialization/setup

doFilter()  → Called on EVERY request
              Write your logic here
              Call filterChain.doFilter() to pass
              control to the next Filter or business logic

destroy()   → Called ONCE when Filter is destroyed
              Cleanup work
```

---

### Question 7: "What are the three methods in an Interceptor and what do they do?"

```
preHandle()      → Runs BEFORE Controller
                   Returns boolean
                   true  = proceed
                   false = stop everything

postHandle()     → Runs AFTER Controller
                   BEFORE response is sent
                   Returns void

afterCompletion()→ Runs AFTER response is sent
                   Good for cleanup/logging
                   Returns void
```

---

## 🔑 The Instructor's Golden Rules — Never Forget These

```
┌─────────────────────────────────────────────────────┐
│                                                     │
│  1. Filter = before Servlet is chosen               │
│     Interceptor = after Servlet, before Controller  │
│                                                     │
│  2. Filter = generic (all Servlets)                 │
│     Interceptor = specific (DispatcherServlet)      │
│                                                     │
│  3. Filter = Jakarta EE / plain Java                │
│     Interceptor = Spring Framework specific         │
│                                                     │
│  4. Both support multiple instances with ordering   │
│                                                     │
│  5. Request flows FORWARD                           │
│     Response flows REVERSE (stack pattern)          │
│                                                     │
│  6. preHandle returning FALSE = hard stop,          │
│     nothing downstream runs                         │
│                                                     │
│  7. Spring Security = Filters                       │
│     (generic, must apply everywhere)                │
│                                                     │
└─────────────────────────────────────────────────────┘
```

---

## One Page Revision — The Complete Summary

```
                    HTTP Request
                         │
                         ▼
              [ Servlet Container ]
                         │
                         ▼
              ┌──────────────────┐
              │   FILTER CHAIN   │  → Jakarta EE
              │   (generic)      │  → All Servlets
              │   Filter 1       │  → setOrder() controls
              │   Filter 2       │    sequence
              └────────┬─────────┘
                       │
                       ▼
          [ DispatcherServlet chosen ]
                       │
                       ▼
              ┌──────────────────────┐
              │    INTERCEPTORS      │  → Spring only
              │    (app specific)    │  → DispatcherServlet
              │    preHandle()       │  → registration order
              │    → Controller      │    controls sequence
              │    postHandle()      │
              │    afterCompletion() │
              └──────────┬───────────┘
                         │
                         ▼
                   [ Controller ]
                  Business Logic
                         │
                         ▼
              (response travels back
               in REVERSE order
               through all layers)
                         │
                         ▼
                    HTTP Response
```

---

## When to Use What — Final Decision Card

```
┌─────────────────────────────────────────────────────┐
│                                                     │
│   Authentication / Security   →   FILTER            │
│   CORS handling               →   FILTER            │
│   Request/Response logging    →   FILTER            │
│   GZIP Compression            →   FILTER            │
│   Encoding                    →   FILTER            │
│                                                     │
│   Role/Permission checks      →   INTERCEPTOR       │
│   API execution time tracking →   INTERCEPTOR       │
│   Spring MVC model injection  →   INTERCEPTOR       │
│   App-specific request audit  →   INTERCEPTOR       │
│                                                     │
│   NOT SURE?                                         │
│   Ask: Does it need to apply to ALL Servlets?       │
│   YES → Filter    NO → Interceptor                  │
│                                                     │
└─────────────────────────────────────────────────────┘
```

---

## 🎯 That's the Complete Lecture!

Here's a recap of all 8 steps we covered:

1. **Core Concept & Definitions** — what Filter and Interceptor are in one line
2. **Architecture Diagram** — where each sits in the request lifecycle
3. **What is a Servlet** — deep dive into Servlet Container, DispatcherServlet, old vs new way
4. **Decision Guide** — when to use Filter vs Interceptor with real world use cases
5. **Multiple Interceptors & Ordering** — code + execution flow
6. **How to Add Filters** — code + three methods explained
7. **Combined Flow** — Filters + Interceptors running together, full output
8. **Interview Tips & Revision** — all common questions + golden rules

---

These notes cover everything the instructor taught — concept, architecture, code, and interview prep. You're well prepared on this topic now! 🚀