# Spring Boot Annotations — Controller Layer

---

## Part 1 — The Big Picture: How Does Spring Boot Route a Request?

### The Problem We're Solving

Imagine a user types a URL into their browser — say `GET /fetchUser`. Your Spring Boot app receives this. But your app might have *hundreds* of classes and *hundreds* of methods. So the very first question is:

> **"Which method should run when this URL is hit?"**

This is the core problem all the annotations in this lecture are solving.

---

### How Spring Boot Handles It Internally

Spring Boot uses a component called the **Dispatcher Servlet**. Think of it as the **traffic cop** of your application.

```
User hits an API
      │
      ▼
┌─────────────────────┐
│   Dispatcher        │
│   Servlet           │  ◄── The front door of every request
│  (Traffic Cop)      │
└─────────┬───────────┘
          │
          │ "Who can handle /fetchUser ?"
          ▼
┌─────────────────────┐
│   Handler Mapping   │  ◄── Looks at all registered controllers
│                     │       and finds the right one
└─────────┬───────────┘
          │
          │ "Found it! → SampleController.getUserDetails()"
          ▼
┌─────────────────────┐
│   Your Controller   │  ◄── Your actual code runs here
│   Method            │
└─────────────────────┘
          │
          ▼
     Response sent back to user
```

---

### What is Handler Mapping?

Handler Mapping is the internal Spring mechanism that **maintains a map** of:

```
URL + HTTP Method  →  Controller Class + Method
```

For example:
```
GET  /api/fetchUser  →  SampleController.getUserDetails()
POST /api/saveUser   →  SampleController.saveUser()
```

But for Handler Mapping to even *consider* a class, that class needs to be **marked** in a way that tells Spring Boot:

> *"Hey! I'm eligible to handle HTTP requests."*

That's exactly what the annotations in this lecture do — and **that's why they exist.**

---

### The Flow in One Line

> User hits URL → Dispatcher Servlet → Handler Mapping (finds the right controller + method using annotations) → Your method runs → Response goes back.

---
## Part 2 — @Controller vs @RestController (+ @ResponseBody)

---

### First, Why Do These Annotations Exist?

Picking up from Part 1 — Handler Mapping needs to know **which classes are eligible** to handle HTTP requests. Out of potentially hundreds of classes in your project, Spring Boot needs a way to say:

> *"Only these classes are controllers. Only look here when routing requests."*

That's the first job of `@Controller` and `@RestController` — they **register the class** as a candidate for handling HTTP requests.

---

### @Controller

When you put `@Controller` on a class, you're telling Spring Boot:

> *"This class can handle incoming HTTP requests."*

Internally, if you look at how `@Controller` is defined:

```
@Controller is just a specialized @Component
```

```
@Target({ElementType.TYPE})         // Can only be applied on a CLASS
@Retention(RetentionPolicy.RUNTIME) // Available at runtime (so Spring can read it)
@Documented
@Component                          // Registers this class as a Spring Bean
public @interface Controller {
    @AliasFor(annotation = Component.class)
    String value() default "";
}
```

So `@Controller` does two things:
1. Registers the class as a **Spring Bean** (via `@Component`)
2. Marks it as a **request handler** — so Handler Mapping considers it

---

### The Problem with @Controller Alone

Here's where it gets interesting. Let's say you write this:

```java
@Controller
public class SampleController {

    @GetMapping("/fetchUser")
    public String getUserDetails() {
        return "hello";
    }
}
```

You hit `/fetchUser` and expect to see `hello` on screen.

But instead — **you get an error.**

Why? Because `@Controller` by default assumes you are building a **traditional MVC web app** where methods return the *name of a view* (like a JSP or Thymeleaf page) — not raw data.

So when you return `"hello"`, Spring Boot thinks:

```
"hello" returned
     │
     ▼
Spring Boot looks for a file called hello.jsp (or hello.html)
     │
     ▼
Can't find it → Error!
```

This is called **view resolution.** Spring tries to *render* the return value as a UI page.

---

### @ResponseBody — The Fix

To tell Spring Boot:

> *"Stop treating the return value as a view name. Treat it as the actual HTTP response body — send it as-is."*

You use `@ResponseBody`:

```java
@Controller
public class SampleController {

    @GetMapping("/fetchUser")
    @ResponseBody               // ← Now Spring sends "hello" directly as response
    public String getUserDetails() {
        return "hello";
    }
}
```

Now the flow looks like this:

```
"hello" returned
     │
     ▼
@ResponseBody is present?  YES
     │
     ▼
Send "hello" directly as HTTP Response Body ✅
```

---

### @RestController — The Cleaner Way

Now imagine you have 20 methods in your controller. Writing `@ResponseBody` on every single method is painful and easy to forget.

`@RestController` solves this. It is simply:

```
@RestController  =  @Controller  +  @ResponseBody
```

When you annotate the class with `@RestController`, **every method** in that class automatically behaves as if it has `@ResponseBody`. You don't write it on each method anymore.

```java
@RestController                     // @ResponseBody applies to ALL methods
public class SampleController {

    @GetMapping("/fetchUser")
    public String getUserDetails() {  // No need for @ResponseBody here
        return "hello";
    }

    @PostMapping("/saveUser")
    public String saveUser() {        // No need for @ResponseBody here either
        return "saved";
    }
}
```

---

### The Full Picture Side by Side

```
┌─────────────────────────────────────────────────────────────┐
│                    @Controller                              │
│                                                             │
│  Return value → treated as VIEW NAME by default             │
│  Need @ResponseBody on each method                          │
│  to send data directly as HTTP response                     │
│                                                             │
│  Used when: Building traditional MVC apps with              │
│  JSP / Thymeleaf views                                      │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                  @RestController                            │
│                                                             │
│  = @Controller + @ResponseBody                              │
│                                                             │
│  Return value → treated as HTTP response body always        │
│  No need to write @ResponseBody on each method              │
│                                                             │
│  Used when: Building REST APIs (which is almost always      │
│  the case in modern apps)                                   │
└─────────────────────────────────────────────────────────────┘
```

---

### 💡 Interview Tip

A very common interview question:

> **"What is the difference between @Controller and @RestController?"**

The answer Shreyansh recommends:

> `@RestController` is a convenience annotation that combines `@Controller` and `@ResponseBody`. In `@Controller`, if you don't add `@ResponseBody` on a method, Spring treats the return value as a view name and tries to resolve it. With `@RestController`, every method's return value is automatically written directly to the HTTP response body — no view resolution happens.

---
## Part 3 — @RequestMapping, @GetMapping, and Friends

---

### The Problem We're Solving Now

In Part 2, we solved the first question:
> *"Which **classes** are eligible to handle HTTP requests?"*

Now we need to solve the second question:
> *"Okay, but within that class — **which method** handles **which specific URL**?"*

That's exactly what `@RequestMapping` does — it **maps a URL + HTTP method → to a specific Java method.**

---

### @RequestMapping

Think of it as drawing a direct line:

```
HTTP Request (URL + Method)  ──────►  Java Method
GET /api/fetchUser           ──────►  getUserDetails()
POST /api/saveUser           ──────►  saveUser()
```

Here's how you write it:

```java
@Controller
public class SampleController {

    @RequestMapping(path = "/fetchUser", method = RequestMethod.GET)
    public String getUserDetails() {
        // your logic here
        return "hello";
    }
}
```

---

### What's Inside @RequestMapping?

If you look at the annotation definition internally, it has several parameters:

```
@RequestMapping has:
├── path (or value)  → the URL path e.g. "/fetchUser"
├── method           → GET, POST, PUT, PATCH, DELETE
├── consumes         → what format does this API accept? e.g. "application/json"
└── produces         → what format does this API return? e.g. "application/json"
```

> **path vs value** — These are aliases for each other. `path = "/fetchUser"` and `value = "/fetchUser"` do exactly the same thing. Shreyansh personally prefers `path` because it reads more clearly — *this is the URL path.*

A full example:
```java
@RequestMapping(
    path = "/fetchUser",
    method = RequestMethod.GET,
    consumes = "application/json",
    produces = "application/json"
)
```

---

### How Does the Mapping Actually Happen Internally?

Inside `@RequestMapping`, there are two important meta-annotations doing the heavy lifting:

```
@Mapping
@Reflective(ControllerMappingReflectiveProcessor.class)
```

The `@Reflective` part is what actually performs the mapping logic at runtime — it uses **Java Reflection** to read your annotations and build the internal map of:

```
URL + HTTP Method  →  Controller + Method
```

> This is why Shreyansh mentions that Java Reflection is a prerequisite concept — all Spring annotations are resolved through reflection under the hood. You don't need to know the internals for daily work, but it's good to know *why* it works.

---

### @RequestMapping at Class Level — A Very Useful Pattern

Here's something practical. Say all your APIs share a common base path like `/api/`:

```
GET  /api/fetchUser
POST /api/saveUser
PUT  /api/updateUser
```

Without class-level mapping, you'd repeat `/api` everywhere:

```java
// Repetitive ❌
@RequestMapping(path = "/api/fetchUser", method = RequestMethod.GET)
@RequestMapping(path = "/api/saveUser", method = RequestMethod.POST)
@RequestMapping(path = "/api/updateUser", method = RequestMethod.PUT)
```

Instead, put the **common part at the class level** and only the **unique part at the method level:**

```java
@RestController
@RequestMapping("/api")          // ← common base path for ALL methods
public class SampleController {

    @RequestMapping(path = "/fetchUser", method = RequestMethod.GET)
    public String getUserDetails() { ... }

    @RequestMapping(path = "/saveUser", method = RequestMethod.POST)
    public String saveUser() { ... }
}
```

Spring Boot **appends** them automatically:

```
Class level:   /api
Method level:  /fetchUser
               ──────────
Final URL:     /api/fetchUser  ✅
```

---

### @GetMapping, @PostMapping — Shortcut Annotations

Writing `method = RequestMethod.GET` every time is a bit verbose. So Spring Boot gives you **shortcut annotations:**

```
@GetMapping("/fetchUser")   =   @RequestMapping(path="/fetchUser", method=GET)
@PostMapping("/saveUser")   =   @RequestMapping(path="/saveUser",  method=POST)
@PutMapping("/updateUser")  =   @RequestMapping(path="/updateUser",method=PUT)
@DeleteMapping("/deleteUser")=  @RequestMapping(path="/deleteUser",method=DELETE)
@PatchMapping("/patchUser") =   @RequestMapping(path="/patchUser", method=PATCH)
```

They are all just `@RequestMapping` with the `method` pre-filled. Everything else (path, consumes, produces) works exactly the same.

So in practice, your controller looks like this:

```java
@RestController
@RequestMapping("/api")
public class SampleController {

    @GetMapping("/fetchUser")       // cleaner ✅
    public String getUserDetails() { ... }

    @PostMapping("/saveUser")       // cleaner ✅
    public String saveUser() { ... }
}
```

---

### The Full Flow So Far

Putting Parts 1, 2, and 3 together:

```
User hits: GET /api/fetchUser
                │
                ▼
       Dispatcher Servlet
                │
                ▼
       Handler Mapping scans classes
       marked with @Controller
       or @RestController
                │
                ▼
       Finds SampleController
       (has @RequestMapping("/api") at class level)
                │
                ▼
       Looks for method mapped to
       GET + /fetchUser
                │
                ▼
       Finds getUserDetails()
       (has @GetMapping("/fetchUser"))
                │
                ▼
       Runs getUserDetails()
                │
                ▼
       Return value sent as
       HTTP Response (because @RestController)
```

---

### 💡 Interview Tip

> **"What is the difference between @RequestMapping and @GetMapping?"**

> `@GetMapping` is a composed annotation — it's a shortcut for `@RequestMapping` with `method = RequestMethod.GET` already set. They work identically. `@GetMapping` is preferred in practice because it's more readable and less verbose. Similarly, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, and `@PatchMapping` exist for other HTTP methods.

---
## Part 4 — @RequestParam (+ Type Conversion & @InitBinder)

---

### The Problem We're Solving Now

So far we know how to route a request to the right method. But now a new question comes up:

> *"The user is sending **extra data** along with the URL — how do we grab that data inside our method?"*

There are actually **three different ways** users send data in a URL. Let's understand them first before jumping into the annotation.

---

### Three Ways Data Can Travel in a URL

```
https://localhost:8080/api/fetchUser?firstName=Shreyansh&lastName=Jain&age=32
│                      │             │
│                      │             └── Query String (Request Parameters)
│                      └── Path
└── Host
```

```
┌─────────────────────────────────────────────────────────────────┐
│  TYPE 1: Request Parameters (Query String)                      │
│                                                                 │
│  Comes AFTER the ? in the URL                                   │
│  Separated by &                                                 │
│  Format: key=value                                              │
│                                                                 │
│  Example: ?firstName=Shreyansh&lastName=Jain&age=32             │
│                                                                 │
│  Handled by: @RequestParam  ← THIS PART                         │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  TYPE 2: Path Variable                                          │
│                                                                 │
│  Embedded WITHIN the URL path itself                            │
│                                                                 │
│  Example: /api/fetchUser/Shreyansh                              │
│                                                                 │
│  Handled by: @PathVariable  ← covered in Part 5                 │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  TYPE 3: Request Body                                           │
│                                                                 │
│  Sent in the BODY of the HTTP request (not in the URL)          │
│  Usually JSON format                                            │
│                                                                 │
│  Example: { "username": "Shreyansh", "email": "s@gmail.com" }   │
│                                                                 │
│  Handled by: @RequestBody  ← covered in Part 6                  │
└─────────────────────────────────────────────────────────────────┘
```

---

### @RequestParam

`@RequestParam` **binds a query parameter from the URL to a method parameter** in your Java method.

Here's the URL:
```
/api/fetchUser?firstName=Shreyansh&lastName=Jain&age=32
```

And here's how you catch all three parameters:

```java
@RestController
@RequestMapping(value = "/api/")
public class SampleController {

    @GetMapping(path = "/fetchUser")
    public String getUserDetails(
        @RequestParam(name = "firstName") String firstName,
        @RequestParam(name = "lastName", required = false) String lastName,
        @RequestParam(name = "age") int age
    ) {
        return "fetching and returning user details based on" +
               " first name = " + firstName +
               ", lastName = " + lastName +
               " and age is = " + age;
    }
}
```

---

### The `required` attribute — Very Important

By default, every `@RequestParam` is **mandatory.** If the user doesn't send it, Spring Boot will **not invoke the method** and will throw an error.

```
┌──────────────────────────────────────────────────────────┐
│  required = true  (DEFAULT)                              │
│                                                          │
│  The parameter MUST be present in the URL                │
│  If missing → Spring throws an error                     │
│  Method does NOT get invoked                             │
│                                                          │
│  Example: @RequestParam(name = "firstName")              │
│           → firstName MUST be in the URL                 │
└──────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────┐
│  required = false                                        │
│                                                          │
│  The parameter is OPTIONAL                               │
│  If missing → value is set to null                       │
│  Method still gets invoked                               │
│                                                          │
│  Example: @RequestParam(name = "lastName",               │
│                         required = false)                │
│           → lastName can be absent, will be null         │
└──────────────────────────────────────────────────────────┘
```

Shreyansh demonstrated this live:

```
Hit: /api/fetchUser?firstName=Shreyansh
→ Works fine. lastName is null, firstName is "Shreyansh" ✅

Hit: /api/fetchUser?lastName=Jain
→ Error! firstName is mandatory but missing ❌
```

---

### Type Conversion — Spring Does It Automatically

Here's something subtle but important. Everything that comes in a URL is **a String by default.** The URL is plain text.

But in your method, you wrote `int age` — not `String age`.

So how does `"32"` (a String from the URL) become `32` (an int in Java)?

**Spring Boot automatically performs type conversion for you.**

It supports:

```
┌─────────────────────────────────────────────────────────┐
│           Automatic Type Conversion Support             │
├─────────────────────────────────────────────────────────┤
│  1. Primitive types   →  int, long, float, double,      │
│                           boolean, etc.                 │
│                                                         │
│  2. Wrapper classes   →  Integer, Long, Float,          │
│                           Double, Boolean, etc.         │
│                                                         │
│  3. String            →  stays as String                │
│                                                         │
│  4. Enums             →  binds to matching enum value   │
│                                                         │
│  5. Custom objects    →  via PropertyEditor             │
│                          (you write the logic)          │
└─────────────────────────────────────────────────────────┘
```

For primitives and wrappers, Spring handles it with zero effort from you. But what about complex types — like a `Date` object?

```
URL sends:  "23/04/1999"  (a String)
You want:   Date object in your method
Problem:    Spring doesn't know how to convert this automatically
Solution:   Write your own custom PropertyEditor
```

---

### Custom Type Conversion — @InitBinder & PropertyEditor

This is where `@InitBinder` comes in. It lets you register **custom conversion logic** for specific parameters before any method in the controller is invoked.

Here's the scenario Shreyansh explains:

> The user sends `firstName` — but they might send it as `SHREYANSH`, `shreyansh`, `Shreyansh`, or any mix of cases. Before this value reaches your method, you want to **trim it and convert it to lowercase.**

**Step 1 — Write your custom PropertyEditor:**

```java
public class FirstNamePropertyEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        // text = whatever string came from the URL
        // do your custom logic here
        setValue(text.trim().toLowerCase());  // trim spaces + make lowercase
    }
}
```

**Step 2 — Register it in your controller using @InitBinder:**

```java
@RestController
@RequestMapping(value = "/api/")
public class SampleController {

    @InitBinder
    protected void initBinder(DataBinder binder) {
        binder.registerCustomEditor(
            String.class,                    // target type
            "firstName",                     // which parameter to apply this to
            new FirstNamePropertyEditor()    // your custom editor
        );
    }

    @GetMapping(path = "/fetchUser")
    public String getUserDetails(
        @RequestParam(name = "firstName") String firstName,
        @RequestParam(name = "lastName", required = false) String lastName,
        @RequestParam(name = "age") int age
    ) {
        return "fetching user: " + firstName;
    }
}
```

---

### How @InitBinder Works — The Flow

```
User hits: /api/fetchUser?firstName=SHREYANSH&lastName=Jain&age=32
                │
                ▼
       Spring finds the right method
                │
                ▼
       @InitBinder runs FIRST (before the method)
                │
                ▼
       Checks: any parameter needs pre-processing?
                │
                ├── firstName → YES, FirstNamePropertyEditor registered
                │              runs setAsText("SHREYANSH")
                │              setValue("shreyansh")  ← trimmed + lowercased
                │
                └── lastName, age → no custom editor, convert normally
                │
                ▼
       getUserDetails("shreyansh", "Jain", 32) runs
                │
                ▼
       Response sent
```

> **Key point:** `@InitBinder` runs **before every method invocation** in that controller. It's a pre-processing hook.

---

### 💡 Interview Tips

> **"What does required = false do in @RequestParam?"**

> By default `@RequestParam` marks a parameter as mandatory. If `required = false` is set, the parameter becomes optional — if absent from the URL, it's injected as `null` into the method. The method still gets called.

> **"How does Spring handle type conversion for request parameters?"**

> Spring automatically converts the String representation of a query parameter into the target Java type — supporting primitives, wrappers, Strings, and Enums out of the box. For custom types, you can register a `PropertyEditor` via `@InitBinder`.

> **"What is @InitBinder used for?"**

> `@InitBinder` is used to register custom editors or formatters that pre-process request parameters before they are bound to method parameters. It runs before every controller method invocation in that class.

---

## Part 5 — @PathVariable

---

### The Problem We're Solving Now

In Part 4, we saw how to grab data that comes **after the `?`** in a URL (query parameters). But there's another very common pattern where the data is **embedded directly inside the URL path itself** — no `?`, no `key=value`, just the value sitting right there in the path.

```
Query Parameter style:    /api/fetchUser?firstName=Shreyansh
Path Variable style:      /api/fetchUser/Shreyansh
```

Both carry the same data — but in different ways. `@PathVariable` handles the second style.

---

### What Does @PathVariable Do?

> It **extracts a value from within the URL path** and binds it to a method parameter.

---

### How to Define a Path Variable in the URL

The key is the **curly braces `{}`** in your mapping. They act as a **placeholder** — telling Spring:

> *"Something dynamic will come here. Capture it."*

```
/api/fetchUser/{firstName}
                │
                └── This is a placeholder.
                    Whatever the user puts here,
                    capture it and give it a name: "firstName"
```

So the URL can be:
```
/api/fetchUser/Shreyansh   → firstName = "Shreyansh"
/api/fetchUser/John        → firstName = "John"
/api/fetchUser/12345       → firstName = "12345"
```

---

### Writing It in Code

```java
@RestController
@RequestMapping(value = "/api/")
public class SampleController {

    @GetMapping(path = "/fetchUser/{firstName}")
    public String getUserDetails(
        @PathVariable(value = "firstName") String firstName
    ) {
        return "fetching and returning user details based on first name = "
               + firstName;
    }
}
```

The `value = "firstName"` inside `@PathVariable` must **exactly match** the placeholder name inside `{}` in the path.

---

### Multiple Path Variables

You can have more than one path variable in the same URL:

```java
@GetMapping(path = "/fetchUser/{firstName}/{age}")
public String getUserDetails(
    @PathVariable(value = "firstName") String firstName,
    @PathVariable(value = "age") int age
) {
    return "User: " + firstName + ", Age: " + age;
}
```

URL:
```
/api/fetchUser/Shreyansh/32
→ firstName = "Shreyansh"
→ age = 32
```

---

### @PathVariable vs @RequestParam — Side by Side

This is a very common confusion point. Here's a clear comparison:

```
┌────────────────────────────────────────────────────────────────────┐
│                    @RequestParam                                   │
│                                                                    │
│  Data location:  After ? in the URL                                │
│  Format:         key=value pairs                                   │
│  Separator:      & between multiple params                         │
│  Example URL:    /api/fetchUser?firstName=Shreyansh&age=32         │
│  Optional?:      Yes, via required = false                         │
│  Best for:       Filters, search, optional inputs                  │
└────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────┐
│                    @PathVariable                                   │
│                                                                    │
│  Data location:  Embedded inside the URL path itself               │
│  Format:         Part of the URL structure                         │
│  Separator:      / between segments                                │
│  Example URL:    /api/fetchUser/Shreyansh                          │
│  Optional?:      No, it's always part of the URL structure         │
│  Best for:       Identifying a specific resource (ID, name)        │
└────────────────────────────────────────────────────────────────────┘
```

---

### When to Use Which? — Real World Intuition

This is something Shreyansh doesn't explicitly cover but is very important in practice:

```
┌──────────────────────────────────────────────────────────────┐
│  Use @PathVariable when...                                   │
│                                                              │
│  You are identifying a SPECIFIC resource                     │
│                                                              │
│  GET /api/users/42          → get user with ID 42            │
│  GET /api/orders/ORD-001    → get order ORD-001              │
│  DELETE /api/products/99    → delete product 99              │
│                                                              │
│  Think of it as: "Which one?"                                │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  Use @RequestParam when...                                   │
│                                                              │
│  You are FILTERING, SEARCHING, or passing OPTIONAL data      │
│                                                              │
│  GET /api/users?city=Kolkata&age=25    → filter users        │
│  GET /api/products?sort=price&order=asc → sort products      │
│  GET /api/search?query=shoes           → search              │
│                                                              │
│  Think of it as: "Show me results based on these options"    │
└──────────────────────────────────────────────────────────────┘
```

---

### The Full Flow

```
User hits: GET /api/fetchUser/Shreyansh
                │
                ▼
       Dispatcher Servlet
                │
                ▼
       Handler Mapping looks for a method
       mapped to GET + /api/fetchUser/{something}
                │
                ▼
       Finds getUserDetails() with
       @GetMapping("/fetchUser/{firstName}")
                │
                ▼
       Extracts "Shreyansh" from the path
       using @PathVariable
                │
                ▼
       getUserDetails("Shreyansh") runs
                │
                ▼
       Response sent back
```

---

### 💡 Interview Tips

> **"What is the difference between @PathVariable and @RequestParam?"**

> `@PathVariable` extracts data embedded directly within the URL path, identified using `{}` placeholders in the mapping. `@RequestParam` extracts data from the query string — the part after `?` in the URL. Path variables are typically used to identify a specific resource, while request parameters are used for filtering, sorting, or optional inputs.

> **"Can @PathVariable be optional?"**

> By default, `@PathVariable` is not optional — it is always part of the URL structure. However, from Spring 4.3.3 onwards, you can set `required = false` on it, but this requires careful API design because it changes the URL structure itself. In practice, optional values are almost always handled with `@RequestParam` instead.

---
## Part 6 — @RequestBody (+ @JsonProperty)

---

### The Problem We're Solving Now

So far, we've seen two ways data travels to the server:
- **Query string** → `@RequestParam`
- **URL path** → `@PathVariable`

But both of these have a limitation — they work well for **small, simple data.** What if the user needs to send a **complex object** — like a full user profile with 10 fields, or an order with nested items?

Putting all that in the URL would be:
- Messy and unreadable
- Insecure (URLs are logged and visible in browser history)
- Limited in size

This is where the **request body** comes in. Data is sent **inside the HTTP request body** — completely separate from the URL — usually as **JSON.**

---

### What Does @RequestBody Do?

> It tells Spring Boot: *"Look inside the body of the incoming HTTP request, take the JSON data you find there, and convert it into a Java object — then give it to me as a method parameter."*

---

### A Simple Example First

Here's the HTTP request a user sends:

```
POST /api/saveUser
Content-Type: application/json

{
    "username": "Shreyansh",
    "email": "s@gmail.com"
}
```

And here's how you catch it in your controller:

```java
@RestController
@RequestMapping(value = "/api/")
public class SampleController {

    @PostMapping(path = "/saveUser")
    public String saveUser(@RequestBody User user) {
        return "User created: " + user.getUsername()
               + " : " + user.getEmail();
    }
}
```

And your `User` class:

```java
public class User {
    String username;
    String email;

    // getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
```

---

### How Does JSON → Java Object Conversion Happen?

Spring Boot doesn't do this conversion itself. It delegates to a library. By default, Spring Boot uses **Jackson** (sometimes Gson) to:

```
┌─────────────────────────────────────────────────────────────┐
│              JSON → Java Object (Deserialization)           │
│                                                             │
│  JSON comes in:                                             │
│  {                                                          │
│      "username": "Shreyansh",                               │
│      "email": "s@gmail.com"                                 │
│  }                                                          │
│                    │                                        │
│                    ▼                                        │
│            Jackson Library                                  │
│                    │                                        │
│                    ▼                                        │
│  Java object created:                                       │
│  User {                                                     │
│      username = "Shreyansh"                                 │
│      email    = "s@gmail.com"                               │
│  }                                                          │
└─────────────────────────────────────────────────────────────┘
```

Jackson does this by **matching JSON keys to Java field names.** It looks at the key `"username"` in JSON and finds the field `username` in your Java class — names match, so it maps the value across.

But what if the names **don't match?**

---

### The Problem — Mismatched Field Names

This is very common in real projects. APIs often use **snake_case** (`user_name`) while Java conventionally uses **camelCase** (`username`).

```
JSON coming in:          Java class field:
"user_name": "Shreyansh"   String username;
     │                          │
     └──── names don't match ───┘
           Jackson can't map this automatically ❌
```

---

### @JsonProperty — The Fix

You use `@JsonProperty` on your Java field to tell Jackson:

> *"In the JSON, this field will come with the key `user_name` — map it to this Java field."*

```java
public class User {

    @JsonProperty("user_name")     // ← JSON key name
    String username;               // ← Java field name (can be different)

    String email;                  // ← same name in JSON and Java, no annotation needed

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
```

Now Jackson knows:

```
JSON key "user_name"  →  maps to Java field "username" ✅
JSON key "email"      →  maps to Java field "email"    ✅
```

---

### How Jackson Uses Getters & Setters

An important detail Shreyansh points out — Jackson doesn't just directly set field values. It **invokes your setters** to set values and **invokes your getters** to read them. This is why getters and setters matter in your model classes.

```
Jackson reads JSON key "user_name"
        │
        ▼
Finds @JsonProperty("user_name") on field "username"
        │
        ▼
Calls setUsername("Shreyansh")
        │
        ▼
username = "Shreyansh" ✅
```

---

### The Full Flow

```
User sends POST /api/saveUser
with body: { "user_name": "Shreyansh", "email": "s@gmail.com" }
                │
                ▼
       Dispatcher Servlet
                │
                ▼
       Routes to saveUser() method
       (because @PostMapping("/saveUser"))
                │
                ▼
       Sees @RequestBody on method parameter
                │
                ▼
       Hands JSON body to Jackson library
                │
                ▼
       Jackson reads each JSON key:
       ├── "user_name" → @JsonProperty found → calls setUsername()
       └── "email"     → name matches directly → calls setEmail()
                │
                ▼
       User object fully populated:
       User { username="Shreyansh", email="s@gmail.com" }
                │
                ▼
       saveUser(user) runs with populated object
                │
                ▼
       Response sent back
```

---

### @RequestParam vs @PathVariable vs @RequestBody — Complete Picture

```
┌──────────────────────────────────────────────────────────────────┐
│  @RequestParam                                                   │
│  Data in:   URL query string (?key=value)                        │
│  Format:    Simple key-value pairs                               │
│  Used for:  Simple, optional, filterable data                    │
│  Example:   /api/users?city=Kolkata&age=25                       │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│  @PathVariable                                                   │
│  Data in:   URL path itself (/resource/{id})                     │
│  Format:    Part of URL structure                                │
│  Used for:  Identifying a specific resource                      │
│  Example:   /api/users/42                                        │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│  @RequestBody                                                    │
│  Data in:   HTTP request body (not in URL at all)                │
│  Format:    JSON / XML                                           │
│  Used for:  Complex objects, sensitive data, large payloads      │
│  Example:   POST with { "username": "x", "email": "y" }          │
└──────────────────────────────────────────────────────────────────┘
```

---

### 💡 Interview Tips

> **"What does @RequestBody do?"**

> `@RequestBody` tells Spring Boot to read the body of the incoming HTTP request and deserialize it into a Java object. Spring Boot uses Jackson by default to convert JSON to a Java object by matching JSON keys to Java field names via getters and setters.

> **"What is @JsonProperty used for?"**

> `@JsonProperty` is a Jackson annotation used when the JSON key name doesn't match the Java field name. It tells Jackson which JSON key to map to which Java field — very commonly used when APIs use snake_case and Java uses camelCase.

> **"Why do we need getters and setters in model classes used with @RequestBody?"**

> Jackson uses getters and setters to read and write field values during serialization and deserialization. Without them, Jackson cannot populate your Java object from JSON.

---
## Part 7 — ResponseEntity

---

### The Problem We're Solving Now

So far, when we return something from a `@RestController` method, we just return a plain value — a `String`, an object, etc. Spring Boot sends it as the response body. Simple.

But think about this — an HTTP response is **not just a body.** It has multiple parts:

```
┌─────────────────────────────────────────────────────────────┐
│                    HTTP Response                            │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  Status Code                                        │    │
│  │  e.g. 200 OK, 404 Not Found, 400 Bad Request        │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  Headers                                            │    │
│  │  e.g. Content-Type, Authorization, Custom headers   │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  Body                                               │    │
│  │  e.g. { "username": "Shreyansh" }                   │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

When you simply return a `String` from a `@RestController`, you only control **the body.** You have no say over the status code or headers — Spring picks defaults for you.

But in real applications, you need full control:
- Return `200 OK` when everything works
- Return `400 Bad Request` when input is wrong
- Return `404 Not Found` when a resource doesn't exist
- Return custom headers when needed

This is exactly what `ResponseEntity` gives you.

---

### What is ResponseEntity?

> `ResponseEntity` represents the **entire HTTP response** — status code, headers, and body — all in one object. It gives you complete control over what goes back to the client.

Shreyansh puts it simply:

```
Returning just a String from @RestController
→ You control: Body only
→ Spring decides: Status code (always 200), Headers (defaults)

Returning ResponseEntity
→ You control: Body + Status Code + Headers
→ Spring decides: Nothing — you're in full control
```

---

### What @RestController Does Internally

This is a subtle but important point Shreyansh makes. When you return a plain `String` from a `@RestController`:

```java
@RestController
public class SampleController {

    @GetMapping("/fetchUser")
    public String getUserDetails() {
        return "hello";       // just a plain string
    }
}
```

Spring Boot **internally wraps this into a ResponseEntity anyway:**

```
return "hello"
      │
      ▼
Spring Boot internally creates:
ResponseEntity.status(HttpStatus.OK).body("hello")
      │
      ▼
Sends full HTTP response with 200 OK + body "hello"
```

So `ResponseEntity` is always there under the hood. When you return a plain value, Spring creates it for you with defaults. When you return `ResponseEntity` explicitly, you're creating it yourself with your own settings.

---

### But What About @Controller?

Here's the contrast Shreyansh demonstrates:

```java
@Controller                    // NOT @RestController
public class SampleController {

    @GetMapping("/fetchUser")
    public String getUserDetails() {
        return "hello";
    }
}
```

Hit this API → **Error!**

Why? Because without `@ResponseBody`, Spring treats `"hello"` as a **view name** — tries to find `hello.jsp` or `hello.html` — can't find it — throws an error.

Spring does **NOT** internally create a `ResponseEntity` here. That only happens when `@ResponseBody` is present (either directly or via `@RestController`).

```
┌──────────────────────────────────────────────────────────┐
│  @RestController + return "hello"                        │
│  → Spring wraps into ResponseEntity internally  ✅        │
│  → Sends as HTTP response body                           │
└──────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────┐
│  @Controller + return "hello" (no @ResponseBody)         │
│  → Spring treats "hello" as a VIEW NAME         ❌        │
│  → Tries to render hello.jsp → Error                     │
└──────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────┐
│  @Controller + @ResponseBody + return "hello"            │
│  → Spring wraps into ResponseEntity internally  ✅        │
│  → Sends as HTTP response body                           │
└──────────────────────────────────────────────────────────┘
```

---

### Using ResponseEntity Explicitly

Here's how you use it in code:

```java
@RestController
@RequestMapping(value = "/api/")
public class SampleController {

    @GetMapping(path = "/fetchUser")
    public ResponseEntity<String> getUserDetails(
        @RequestParam(value = "firstName") String firstName
    ) {
        String output = "Fetched User details of " + firstName;

        return ResponseEntity
                .status(HttpStatus.OK)      // 200 OK
                .body(output);              // response body
    }
}
```

---

### Setting Status Code, Body, and Headers

```java
// Just status + body
return ResponseEntity
        .status(HttpStatus.OK)
        .body(output);

// Bad request with body
return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body("Invalid input provided");

// Not found
return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body("User not found");

// With custom headers
return ResponseEntity
        .status(HttpStatus.OK)
        .header("Custom-Header", "some-value")
        .body(output);

// Created (for POST requests)
return ResponseEntity
        .status(HttpStatus.CREATED)        // 201 Created
        .body(savedUser);
```

---

### Common HTTP Status Codes You'll Use

```
┌────────────────────────────────────────────────────────────┐
│  2xx — Success                                             │
│  200 OK          → general success                         │
│  201 CREATED     → resource created (POST)                 │
│  204 NO_CONTENT  → success but nothing to return           │
├────────────────────────────────────────────────────────────┤
│  4xx — Client Errors                                       │
│  400 BAD_REQUEST    → invalid input from client            │
│  401 UNAUTHORIZED   → not logged in                        │
│  403 FORBIDDEN      → logged in but no permission          │
│  404 NOT_FOUND      → resource doesn't exist               │
├────────────────────────────────────────────────────────────┤
│  5xx — Server Errors                                       │
│  500 INTERNAL_SERVER_ERROR → something broke on server     │
└────────────────────────────────────────────────────────────┘
```

---

### The Full Flow

```
User hits: GET /api/fetchUser?firstName=Shreyansh
                │
                ▼
       Dispatcher Servlet → routes to getUserDetails()
                │
                ▼
       Method runs, builds output string
                │
                ▼
       Returns ResponseEntity
       with status = 200 OK
       and body = "Fetched User details of Shreyansh"
                │
                ▼
       Spring sends full HTTP response:
       ┌─────────────────────────────────┐
       │  Status:  200 OK                │
       │  Headers: Content-Type: text    │
       │  Body:    Fetched User          │
       │           details of Shreyansh  │
       └─────────────────────────────────┘
```

---

### 💡 Interview Tips

> **"What is ResponseEntity in Spring Boot?"**

> `ResponseEntity` represents the complete HTTP response including the status code, headers, and body. It gives the developer full control over what is sent back to the client, rather than letting Spring decide defaults. It's a generic class — `ResponseEntity<String>`, `ResponseEntity<User>` etc. — where the type parameter is the body type.

> **"When would you use ResponseEntity over just returning a plain object?"**

> When you need to control the HTTP status code (e.g., return 201 for creation, 404 for not found), add custom headers, or conditionally return different responses based on business logic. For simple cases where 200 OK is always correct and no custom headers are needed, returning a plain object is fine — Spring wraps it internally anyway.

> **"What does Spring Boot do internally when you return a String from a @RestController?"**

> It wraps it into a `ResponseEntity` with a default status of `200 OK` and sends it as the HTTP response body. This only happens because `@RestController` includes `@ResponseBody` — without it, Spring would treat the return value as a view name instead.

---

That wraps up everything from Shreyansh's lecture! Now we move into the **"Beyond the Lecture"** territory.

## Part 8 — DTOs: What, Why & Best Practices

### ⚠️ Beyond the Lecture — Industry Essential

---

### First, What Problem Does a DTO Solve?

Let's say you have a `User` entity in your application — this is the class that maps directly to your database table:

```java
public class User {
    private Long id;
    private String username;
    private String email;
    private String password;        // sensitive!
    private String internalNotes;   // internal only!
    private String createdBy;       // internal only!
    private Date createdAt;
    private boolean isDeleted;      // internal flag!
}
```

Now a client hits your API asking for user details. Do you send back the entire `User` object?

**Absolutely not.** Because:
- `password` would be exposed 🔴
- `internalNotes` is not meant for outside world 🔴
- `isDeleted` is an internal flag, client doesn't need it 🔴
- `createdBy` is internal audit info 🔴

This is the core problem. Your **database entity** and what you **expose to the outside world** are two different things. DTOs solve this.

---

### What is a DTO?

> **DTO = Data Transfer Object**

It is a simple plain Java class whose only job is to **carry data between layers** — specifically between your API (controller layer) and the outside world (clients).

```
┌─────────────────────────────────────────────────────────────────┐
│                    Without DTO                                  │
│                                                                 │
│  Client ◄──────────────────────────────────► Database Entity    │
│              (exposes everything ❌)                             │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                     With DTO                                    │
│                                                                 │
│  Client ◄────────────────► DTO ◄────────────► Database Entity   │
│           (only what       (filter,            (full data,      │
│            client needs ✅) transform)          stays internal)  │
└─────────────────────────────────────────────────────────────────┘
```

---

### A Concrete Example

Your full `User` entity:
```java
public class User {
    private Long id;
    private String username;
    private String email;
    private String password;       // never expose this
    private String internalNotes;  // never expose this
    private boolean isDeleted;     // never expose this
    private Date createdAt;
}
```

Your `UserResponseDTO` — what you send to the client:
```java
public class UserResponseDTO {
    private Long id;
    private String username;
    private String email;
    // password → NOT here ✅
    // internalNotes → NOT here ✅
    // isDeleted → NOT here ✅
}
```

Your `UserRequestDTO` — what the client sends when creating a user:
```java
public class UserRequestDTO {
    private String username;
    private String email;
    private String password;
    // id → NOT here (server generates it) ✅
    // createdAt → NOT here (server sets it) ✅
    // isDeleted → NOT here (internal flag) ✅
}
```

---

### Request DTO vs Response DTO

This is a very important distinction that most beginners miss:

```
┌─────────────────────────────────────────────────────────────────┐
│  Request DTO                                                    │
│                                                                 │
│  Direction:  Client → Server                                    │
│  Purpose:    Carries data the client SENDS to your API          │
│  Used with:  @RequestBody                                       │
│  Contains:   Only fields the client is allowed to provide       │
│                                                                 │
│  Example:    CreateUserRequestDTO, UpdateUserRequestDTO         │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  Response DTO                                                   │
│                                                                 │
│  Direction:  Server → Client                                    │
│  Purpose:    Carries data your API SENDS back to client         │
│  Used with:  Return type of controller method                   │
│  Contains:   Only fields the client is allowed to see           │
│                                                                 │
│  Example:    UserResponseDTO, OrderSummaryDTO                   │
└─────────────────────────────────────────────────────────────────┘
```

---

### How DTOs Fit in the Controller Layer

```
                        Controller Layer
                              │
          ┌───────────────────┼───────────────────┐
          │                   │                   │
          ▼                   ▼                   ▼
   Client sends         Controller          Controller
   Request DTO    ────► receives it   ────► returns
   via @RequestBody      validates it       Response DTO
                         passes to
                         Service layer
                              │
                              ▼
                        Service Layer
                        (business logic)
                              │
                              ▼
                        Repository Layer
                        (database entity)
```

Your controller method looks like this in practice:

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(
        @RequestBody CreateUserRequestDTO requestDTO  // client sends this
    ) {
        // pass to service layer
        UserResponseDTO responseDTO = userService.createUser(requestDTO);

        // send back only what client needs
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(responseDTO);
    }
}
```

---

### How to Convert Entity ↔ DTO — Mapping

Your service layer converts between DTOs and entities. There are two common ways:

**Option 1 — Manual Mapping (simple, full control):**
```java
// Entity → Response DTO
public UserResponseDTO toResponseDTO(User user) {
    UserResponseDTO dto = new UserResponseDTO();
    dto.setId(user.getId());
    dto.setUsername(user.getUsername());
    dto.setEmail(user.getEmail());
    // password, internalNotes etc. intentionally left out
    return dto;
}

// Request DTO → Entity
public User toEntity(CreateUserRequestDTO dto) {
    User user = new User();
    user.setUsername(dto.getUsername());
    user.setEmail(dto.getEmail());
    user.setPassword(hashPassword(dto.getPassword())); // hash before storing
    return user;
}
```

**Option 2 — MapStruct (industry standard for large projects):**
```java
@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponseDTO toResponseDTO(User user);
    User toEntity(CreateUserRequestDTO dto);
}
```
MapStruct generates the mapping code automatically at compile time — no runtime overhead, no manual field-by-field assignment.

---

### How to Structure DTOs in a Real Project

Here's the folder structure most teams follow:

```
src/
└── main/
    └── java/
        └── com/yourcompany/yourapp/
            ├── controller/
            │   └── UserController.java
            │
            ├── dto/
            │   ├── request/
            │   │   ├── CreateUserRequestDTO.java
            │   │   └── UpdateUserRequestDTO.java
            │   └── response/
            │       ├── UserResponseDTO.java
            │       └── UserSummaryDTO.java
            │
            ├── entity/
            │   └── User.java
            │
            ├── service/
            │   └── UserService.java
            │
            └── repository/
                └── UserRepository.java
```

---

### DTO Best Practices

```
┌─────────────────────────────────────────────────────────────────┐
│  ✅ DO                                                           │
│                                                                 │
│  • Separate Request and Response DTOs                           │
│  • Keep DTOs in their own package (dto/request, dto/response)   │
│  • Name them clearly: CreateUserRequestDTO, UserResponseDTO     │
│  • Put validation annotations on Request DTOs (Part 9)          │
│  • Use MapStruct for mapping in large projects                  │
│  • Keep DTOs simple — just fields, getters, setters             │
│  • Never expose sensitive fields in Response DTOs               │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  ❌ DON'T                                                        │
│                                                                 │
│  • Don't use your database Entity as a DTO                      │
│  • Don't put business logic inside DTOs                         │
│  • Don't have one DTO that does everything                      │
│    (one for create, one for update, one for response)           │
│  • Don't expose internal flags or audit fields to clients       │
│  • Don't skip DTOs thinking "it's a small project"              │
│    (projects always grow)                                       │
└─────────────────────────────────────────────────────────────────┘
```

---

### 💡 Interview Tips

> **"What is a DTO and why do we use it?"**

> A DTO (Data Transfer Object) is a simple class used to carry data between layers — specifically between the controller and the client. We use it to control exactly what data goes in and out of our API, keeping database entities internal and preventing sensitive fields from being accidentally exposed.

> **"What is the difference between a DTO and an Entity?"**

> An Entity maps directly to a database table and represents your data model. A DTO is what you expose through your API — it carries only the fields relevant to the client. They are kept separate so that database structure changes don't directly affect your API contract, and vice versa.

> **"Should you use the same DTO for request and response?"**

> No. Request and response DTOs serve different purposes. A request DTO carries what the client sends — it needs validation annotations. A response DTO carries what you send back — it controls what the client sees. Mixing them leads to confusion and security risks.

---

## Part 9 — Validation in the Controller Layer

### ⚠️ Beyond the Lecture — Industry Essential

---

### First, Why Validation at the Controller Layer?

Think about what happens without validation:

```
Client sends:
{
    "username": "",          ← empty string
    "email": "notanemail",   ← invalid format
    "age": -5                ← negative age??
}
        │
        ▼
Controller receives it
        │
        ▼
Passes to Service layer
        │
        ▼
Service tries to process garbage data
        │
        ▼
Either crashes, or worse —
saves bad data to your database ❌
```

Validation at the controller layer acts as a **gatekeeper:**

```
Client sends bad data
        │
        ▼
Controller layer checks it ← validation happens HERE
        │
   ┌────┴────┐
   │         │
Invalid    Valid
   │         │
   ▼         ▼
400 Bad   Passes to
Request   Service layer ✅
sent back
immediately
```

> **Rule of thumb:** Never trust data coming from outside. Always validate at the entry point — the controller layer.

---

### Step 1 — Add the Dependency

Spring Boot doesn't include validation out of the box. You need to add the starter dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

This pulls in **Hibernate Validator** — which is the reference implementation of **Jakarta Bean Validation** (formerly javax validation). These are the actual annotations like `@NotNull`, `@Email` etc.

```
┌─────────────────────────────────────────────────────────────┐
│              Validation Stack                               │
│                                                             │
│  Jakarta Bean Validation  ← defines the standard/spec       │
│           │                                                 │
│           ▼                                                 │
│  Hibernate Validator      ← implements the spec             │
│           │                                                 │
│           ▼                                                 │
│  spring-boot-starter-validation ← bundles it for            │
│                                   Spring Boot               │
└─────────────────────────────────────────────────────────────┘
```

---

### Step 2 — Applying Constraints on Your Request DTO

Remember from Part 8 — validation annotations go on **Request DTOs**, not entities, not response DTOs.

Here's a `CreateUserRequestDTO` with validation constraints:

```java
public class CreateUserRequestDTO {

    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotNull(message = "Age cannot be null")
    @Min(value = 18, message = "Age must be at least 18")
    @Max(value = 120, message = "Age must be less than 120")
    private Integer age;

    // getters and setters
}
```

---

### Common Validation Annotations — Cheat Sheet

```
┌─────────────────────────────────────────────────────────────────┐
│  Null / Empty Checks                                            │
├──────────────────────┬──────────────────────────────────────────┤
│  @NotNull            │  Field cannot be null                    │
│  @NotEmpty           │  Cannot be null or empty ("")            │
│  @NotBlank           │  Cannot be null, empty, or just spaces   │
│                      │  (most strict — use this for Strings)    │
└──────────────────────┴──────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  String Checks                                                  │
├──────────────────────┬──────────────────────────────────────────┤
│  @Size(min, max)     │  String length must be within range      │
│  @Email              │  Must be valid email format              │
│  @Pattern(regexp)    │  Must match a regex pattern              │
└──────────────────────┴──────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  Number Checks                                                  │
├──────────────────────┬──────────────────────────────────────────┤
│  @Min(value)         │  Number must be >= value                 │
│  @Max(value)         │  Number must be <= value                 │
│  @Positive           │  Must be > 0                             │
│  @PositiveOrZero     │  Must be >= 0                            │
│  @Negative           │  Must be < 0                             │
└──────────────────────┴──────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  Date Checks                                                    │
├──────────────────────┬──────────────────────────────────────────┤
│  @Past               │  Date must be in the past                │
│  @PastOrPresent      │  Date must be past or today              │
│  @Future             │  Date must be in the future              │
│  @FutureOrPresent    │  Date must be future or today            │
└──────────────────────┴──────────────────────────────────────────┘
```

---

### Step 3 — Triggering Validation — @Valid vs @Validated

Just putting annotations on your DTO fields is not enough. You need to **trigger** the validation. This is done in the controller method:

#### @Valid

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(
        @Valid @RequestBody CreateUserRequestDTO requestDTO  // ← @Valid triggers validation
    ) {
        UserResponseDTO responseDTO = userService.createUser(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }
}
```

`@Valid` tells Spring:
> *"Before passing this object to the method, run all the validation annotations on it. If anything fails, don't even call the method."*

---

#### @Valid vs @Validated — What's the Difference?

```
┌─────────────────────────────────────────────────────────────────┐
│  @Valid                                                         │
│                                                                 │
│  From: Jakarta Bean Validation (standard)                       │
│  Use:  General purpose validation                               │
│        Also triggers validation on nested objects               │
│        (when used on a field inside a DTO)                      │
│                                                                 │
│  Most common — use this by default                              │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  @Validated                                                     │
│                                                                 │
│  From: Spring Framework (Spring specific)                       │
│  Use:  Everything @Valid does PLUS                              │
│        supports Validation Groups                               │
│        (validate different fields for                           │
│         different scenarios)                                    │
│                                                                 │
│  Use when you need groups — otherwise @Valid is fine            │
└─────────────────────────────────────────────────────────────────┘
```

---

### Validation Groups — When @Validated Shines

Here's a real scenario: When **creating** a user, `id` should not be provided (server generates it). When **updating** a user, `id` is mandatory.

```java
// Define group interfaces
public interface OnCreate {}
public interface OnUpdate {}

// Use groups in DTO
public class UserRequestDTO {

    @Null(groups = OnCreate.class,
          message = "ID must not be provided on create")
    @NotNull(groups = OnUpdate.class,
             message = "ID is required for update")
    private Long id;

    @NotBlank(groups = {OnCreate.class, OnUpdate.class},
              message = "Username cannot be blank")
    private String username;
}

// Use @Validated with group in controller
@PostMapping
public ResponseEntity<UserResponseDTO> createUser(
    @Validated(OnCreate.class) @RequestBody UserRequestDTO dto
) { ... }

@PutMapping
public ResponseEntity<UserResponseDTO> updateUser(
    @Validated(OnUpdate.class) @RequestBody UserRequestDTO dto
) { ... }
```

---

### Nested Object Validation

What if your DTO has another object inside it? You need `@Valid` on that nested field too:

```java
public class CreateOrderRequestDTO {

    @NotBlank(message = "Order name cannot be blank")
    private String orderName;

    @NotNull(message = "Address cannot be null")
    @Valid                          // ← triggers validation on nested object
    private AddressDTO address;
}

public class AddressDTO {

    @NotBlank(message = "Street cannot be blank")
    private String street;

    @NotBlank(message = "City cannot be blank")
    private String city;

    @NotBlank(message = "Pincode cannot be blank")
    @Pattern(regexp = "\\d{6}",
             message = "Pincode must be 6 digits")
    private String pincode;
}
```

Without `@Valid` on the `address` field, Spring validates `CreateOrderRequestDTO` but **skips** validating the fields inside `AddressDTO`.

---

### Step 4 — Handling Validation Errors

When validation fails, Spring Boot automatically throws:

```
MethodArgumentNotValidException
```

By default, Spring returns a `400 Bad Request` with a large, messy error response that's not very client-friendly.

In real projects, you want to catch this and return a **clean, structured error response.**

---

### Writing a Global Exception Handler

```java
@RestControllerAdvice               // handles exceptions across ALL controllers
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(
        MethodArgumentNotValidException ex
    ) {
        Map<String, String> errors = new HashMap<>();

        // extract each field error and its message
        ex.getBindingResult()
          .getFieldErrors()
          .forEach(error ->
              errors.put(
                  error.getField(),           // which field failed
                  error.getDefaultMessage()   // your custom message
              )
          );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errors);
    }
}
```

Now when validation fails, instead of a messy Spring error, the client gets:

```json
{
    "username": "Username cannot be blank",
    "email": "Please provide a valid email address",
    "age": "Age must be at least 18"
}
```

Clean, readable, actionable. ✅

---

### The Complete Validation Flow

```
Client sends POST /api/users
with invalid body:
{
    "username": "",
    "email": "notanemail",
    "age": -5
}
        │
        ▼
Dispatcher Servlet routes to createUser()
        │
        ▼
@Valid triggers validation on CreateUserRequestDTO
        │
        ▼
Hibernate Validator checks all constraints:
├── username: @NotBlank FAILS ❌
├── email: @Email FAILS ❌
└── age: @Min(18) FAILS ❌
        │
        ▼
MethodArgumentNotValidException thrown
Method createUser() is NEVER called
        │
        ▼
GlobalExceptionHandler catches it
        │
        ▼
Returns 400 Bad Request:
{
    "username": "Username cannot be blank",
    "email": "Please provide a valid email address",
    "age": "Age must be at least 18"
}
```

---

### Where Does Validation Fit in the Big Picture?

```
┌─────────────────────────────────────────────────────────────────┐
│                     Controller Layer                            │
│                                                                 │
│  @RestController                                                │
│  @RequestMapping                                                │
│  @GetMapping / @PostMapping etc.                                │
│  @RequestParam / @PathVariable / @RequestBody                   │
│  @Valid / @Validated          ← validates incoming DTO          │
│  ResponseEntity               ← controls full response          │
│                                                                 │
│  GlobalExceptionHandler       ← catches validation errors       │
└───────────────────────┬─────────────────────────────────────────┘
                        │ clean, validated data passes through
                        ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Service Layer                               │
│               (business logic runs here)                        │
└───────────────────────┬─────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Repository Layer                              │
│                  (database operations)                          │
└─────────────────────────────────────────────────────────────────┘
```

---

### 💡 Interview Tips

> **"Where should validation logic live in a Spring Boot application?"**

> Validation of incoming data should happen at the controller layer — as early as possible. This prevents bad data from ever reaching your business logic or database. Spring Boot makes this easy with `@Valid` on `@RequestBody` parameters combined with constraint annotations on DTO fields.

> **"What is the difference between @Valid and @Validated?"**

> `@Valid` is from the Jakarta Bean Validation standard and handles general validation including nested objects. `@Validated` is Spring-specific and adds support for validation groups — useful when the same DTO is used for different operations like create and update, where different fields are required in each case.

> **"What exception does Spring throw when validation fails?"**

> `MethodArgumentNotValidException`. In production applications, you handle this in a `@RestControllerAdvice` class to return a clean, structured error response to the client instead of Spring's default error format.

> **"Why put validation on DTOs and not on entities?"**

> Entities represent your database model — their constraints are about data integrity at the database level. DTOs represent what the client sends — their constraints are about what your API accepts. These are different concerns. Also, the same entity might be used in different ways internally, while each DTO has a specific, well-defined API contract.

---

### Complete Lecture Summary

```
┌─────────────────────────────────────────────────────────────────┐
│           Spring Boot Controller Layer — Full Picture           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Part 1: Dispatcher Servlet + Handler Mapping                   │
│          → routes requests to the right method                  │
│                                                                 │
│  Part 2: @Controller vs @RestController                         │
│          → marks class as request handler                       │
│          → @RestController = @Controller + @ResponseBody        │
│                                                                 │
│  Part 3: @RequestMapping / @GetMapping etc.                     │
│          → maps URL + HTTP method to a specific method          │
│                                                                 │
│  Part 4: @RequestParam + @InitBinder                            │
│          → grabs data from query string (?key=value)            │
│                                                                 │
│  Part 5: @PathVariable                                          │
│          → grabs data from URL path (/resource/{id})            │
│                                                                 │
│  Part 6: @RequestBody + @JsonProperty                           │
│          → grabs complex data from request body (JSON)          │
│                                                                 │
│  Part 7: ResponseEntity                                         │
│          → full control over HTTP response                      │
│             (status + headers + body)                           │
│                                                                 │
│  Part 8: DTOs ⚠️ Beyond Lecture                                 │
│          → separates API contract from database entity          │
│          → Request DTO (in) + Response DTO (out)                │
│                                                                 │
│  Part 9: Validation ⚠️ Beyond Lecture                           │
│          → @Valid / @Validated triggers validation              │
│          → constraint annotations on Request DTOs               │
│          → GlobalExceptionHandler for clean error responses     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

That's the complete lecture notes! Everything from Shreyansh's lecture plus DTOs and Validation. Hope these notes serve you well! 🎉