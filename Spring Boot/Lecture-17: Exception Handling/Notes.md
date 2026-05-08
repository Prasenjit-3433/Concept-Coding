# Part 1: The Big Picture — 5 Key Classes

---

When an exception occurs in a Spring Boot application, it doesn't just crash randomly. Spring Boot has a **well-defined set of classes** that work together to catch the exception, figure out how to handle it, and send a proper response back to the client.

There are **5 key classes** involved. The instructor groups them into two categories:

```
3 are RESOLVERS → they actually handle the exception
2 are HELPERS  → they orchestrate and finalize the response
```

---

## The Class Hierarchy

```
                    «interface»
              HandlerExceptionResolver
                        |
          ┌─────────────┴──────────────┐
          |                            |
«Helper»                          «Abstract»
HandlerExceptionResolver     AbstractHandlerExceptionResolver
     Composite                         |
                         ┌─────────────┴──────────────┐
                         |                            |
                   «Abstract»               DefaultHandler
          AbstractHandlerMethod              Exception
            ExceptionResolver               Resolver ✅
                    |
                    |
          ┌─────────┴──────────────────┐
          |                            |
ExceptionHandler              ResponseStatus
ExceptionResolver ✅          ExceptionResolver ✅


«Helper»
DefaultErrorAttributes
```

---

## The 5 Classes — What Each One Does

---

### 🔵 The 3 Resolvers (the actual exception handlers)

These three are invoked **in sequence**, one after another. Each one specializes in handling a specific *type* of exception situation.

| # | Class | What it handles |
|---|-------|----------------|
| 1st | `ExceptionHandlerExceptionResolver` | `@ExceptionHandler` and `@ControllerAdvice` annotations |
| 2nd | `ResponseStatusExceptionResolver` | Uncaught exceptions annotated with `@ResponseStatus` |
| 3rd | `DefaultHandlerExceptionResolver` | Spring framework's own internal exceptions (e.g. resource not found, method not supported) |

---

### 🟡 The 2 Helpers (orchestration + response building)

**`HandlerExceptionResolverComposite`**
Think of this as the **orchestrator**. It doesn't handle the exception itself. Its job is to call each of the 3 resolvers above, one by one, and check after each one — *"Was the exception handled?"* If yes, it stops. If no, it moves to the next resolver.

**`DefaultErrorAttributes`**
Think of this as the **response builder**. No matter what happens — whether a resolver handled the exception or not — control always comes here at the end. It takes whatever status and message were set, builds the final response object (with `timestamp`, `status`, `error`, `message`, `path`), and sends it back to the client.

---

## One Line Summary of Each

```
HandlerExceptionResolverComposite  →  "I'll decide who handles this"
ExceptionHandlerExceptionResolver  →  "I handle @ExceptionHandler & @ControllerAdvice"
ResponseStatusExceptionResolver    →  "I handle uncaught exceptions with @ResponseStatus"
DefaultHandlerExceptionResolver    →  "I handle Spring's own internal exceptions"
DefaultErrorAttributes             →  "I build and send the final response"
```

---

> 💡 **Interview Tip:** If asked *"What classes are involved in exception handling in Spring Boot?"* — don't just say `@ControllerAdvice`. Walk through all 5 classes, their roles, and the sequence. That's what separates a good answer from a great one.

---

# Part 2: The Full Flow — From Exception to Response

---

The instructor walks through this flow very carefully because **understanding this sequence is the foundation of everything else** in exception handling. Let's build it step by step.

---

## The Starting Point — DispatcherServlet

In Spring Boot, every single HTTP request first goes through **DispatcherServlet**. It is the front controller — it receives the request and routes it to the right controller method.

Now two things can go wrong here:
- Your **controller method throws an exception** during business logic
- The DispatcherServlet **can't even find/invoke** your controller (wrong URL, wrong method type, etc.)

In **either case**, the exception handling flow below kicks in.

---

## The Full Flow — Step by Step

```
                        HTTP Request comes in
                               │
                               ▼
                      ┌─────────────────┐
                      │DispatcherServlet│
                      └────────┬────────┘
                               │
                    Any Exception occurs?
                               │
                              YES
                               │
                               ▼
              ┌────────────────────────────────┐
              │  HandlerExceptionResolver      │
              │       Composite                │
              │   (The Orchestrator)           │
              │                                │
              │   Calls resolvers one by one   │
              │   from LEFT ──────────► RIGHT  │
              └──┬─────────────────────────────┘
                 │
                 ▼
    ┌────────────────────────┐
    │  Resolver 1            │
    │  ExceptionHandler      │
    │  ExceptionResolver     │
    └────────────┬───────────┘
                 │
                 ▼
        Was exception handled?
          │              │
         YES             NO
          │              │
          │              ▼
          │   ┌────────────────────────┐
          │   │  Resolver 2            │
          │   │  ResponseStatus        │
          │   │  ExceptionResolver     │
          │   └────────────┬───────────┘
          │                │
          │                ▼
          │       Was exception handled?
          │         │              │
          │        YES             NO
          │         │              │
          │         │              ▼
          │         │   ┌────────────────────────┐
          │         │   │  Resolver 3            │
          │         │   │  DefaultHandler        │
          │         │   │  ExceptionResolver     │
          │         │   └────────────┬───────────┘
          │         │                │
          └────┬────┘                │
               │    ◄────────────────┘
               │   (all paths lead here)
               ▼
    ┌────────────────────────┐
    │   DefaultError         │
    │   Attributes           │
    │  (Response Builder)    │
    └────────────┬───────────┘
                 │
                 ▼
        Final Response sent to Client
        {
          timestamp: ...,
          status: ...,
          error: ...,
          message: ...,
          path: ...
        }
```

---

## What Each Step Actually Does

---

### Step 1 — HandlerExceptionResolverComposite receives the exception

The moment DispatcherServlet catches an exception, it passes it to `HandlerExceptionResolverComposite`. This class doesn't handle anything itself. It just holds references to all 3 resolvers and calls them in order.

---

### Step 2 — Resolver 1 gets the first chance

`ExceptionHandlerExceptionResolver` looks at the exception and asks:

> *"Is there any method annotated with @ExceptionHandler that can handle this exception?"*

- If **yes** → it handles it, marks it as resolved, and the composite **stops** — it does NOT call resolver 2 or 3.
- If **no** → composite moves on to resolver 2.

---

### Step 3 — Resolver 2 gets the next chance

`ResponseStatusExceptionResolver` looks at the exception and asks:

> *"Does this exception class have a @ResponseStatus annotation on it?"*

- If **yes** → it handles it, marks it as resolved, composite **stops**.
- If **no** → composite moves on to resolver 3.

---

### Step 4 — Resolver 3 gets the last chance

`DefaultHandlerExceptionResolver` looks at the exception and asks:

> *"Is this one of the standard Spring framework exceptions I know about?"*
> *(things like NoResourceFoundException, MethodNotAllowedException, etc.)*

- If **yes** → it handles it.
- If **no** → it can't do anything either.

---

### Step 5 — DefaultErrorAttributes always runs at the end

No matter which path was taken above, **control always reaches `DefaultErrorAttributes`**.

Here is what it does:
- Reads whatever `status` and `message` were set in the HTTP response by the resolvers
- Builds the response body — fills in `timestamp`, `status`, `error`, `message`, `path`
- Creates the `ResponseEntity` object
- Sends it back to the client

**Key point the instructor makes:**
The resolvers themselves **do NOT create the ResponseEntity**. They only **set the status and message** into the HTTP response object. It is always `DefaultErrorAttributes` that actually builds and returns the final `ResponseEntity`.

---

## What Happens When NO Resolver Can Handle the Exception?

This is the important part. If you throw a plain `NullPointerException` or your own `CustomException` and none of the 3 resolvers understand it:

- No status is set by any resolver
- `DefaultErrorAttributes` reaches the end with nothing set
- It falls back to a **default status of 500 Internal Server Error**
- That's why you always see 500 when your exception isn't handled by any resolver

```
Your CustomException thrown
        │
        ▼
Resolver 1 → ❌ can't handle
Resolver 2 → ❌ can't handle  
Resolver 3 → ❌ can't handle
        │
        ▼
DefaultErrorAttributes
→ nobody set a status
→ default = 500 Internal Server Error
→ response sent
```

---

> 💡 **Interview Tip:** A very common question is *"Why does Spring Boot return 500 even when I throw a custom exception with BAD_REQUEST status?"* The answer is — because you're not returning a `ResponseEntity` yourself, you're relying on the resolver framework. And if none of the 3 resolvers recognize your exception, `DefaultErrorAttributes` fills in 500 as the default. The instructor says this is one of the most misunderstood things in Spring Boot exception handling.

---

# Part 3: The "Aha" Moment — Why You See 500 When You Expect 400

---

This is where the instructor takes everything from Part 1 and Part 2 and makes it **click** with real code. He shows two examples, both giving 500, and then explains exactly why — and what to do about it.

---

## Example 1 — Throwing a NullPointerException

This is the simplest case. A basic controller, throwing a `NullPointerException`:

```java
@RestController
@RequestMapping(value = "/api/")
public class UserController {

    @GetMapping(path = "/get-user")
    public String getUser() {
        throw new NullPointerException("throwing null pointer exception for testing");
    }
}
```

You hit this API. What do you get?

```json
{
  "timestamp": "2024-10-22T16:36:34.7%+00:00",
  "status": 500,
  "error": "Internal Server Error",
  "path": "/api/get-user"
}
```

**The question the instructor asks:**
> *"I never wrote anywhere that this is a 500 error. Who set all of this? Who created this response?"*

The answer — `DefaultErrorAttributes`. You never built a `ResponseEntity`. So the framework stepped in, went through all 3 resolvers, none of them understood `NullPointerException` as a custom case, and `DefaultErrorAttributes` filled in 500 as the default and built the whole response.

---

## Example 2 — Throwing a CustomException with BAD_REQUEST

Now the instructor makes it more interesting. He creates a `CustomException` that **explicitly carries a 400 BAD_REQUEST status and a message**:

```java
// The Custom Exception class
public class CustomException extends RuntimeException {

    HttpStatus status;
    String message;

    CustomException(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
```

And the controller throws it like this:

```java
@RestController
@RequestMapping(value = "/api/")
public class UserController {

    @GetMapping(path = "/get-user")
    public String getUser() {
        throw new CustomException(HttpStatus.BAD_REQUEST,
                                  "request is not correct, UserID is missing");
    }
}
```

You hit this API. What do you expect? You'd naturally think:
- Status → 400 BAD_REQUEST ✅ (because that's what we passed)
- Message → "request is not correct, UserID is missing" ✅ (because that's what we passed)

What do you actually get?

```json
{
  "timestamp": "2024-10-22T16:41:41.887+00:00",
  "status": 500,
  "error": "Internal Server Error",
  "path": "/api/get-user"
}
```

**Still 500. Still Internal Server Error.**

---

## Why? — The Root Cause

The instructor explains this very clearly.

The `CustomException` has `BAD_REQUEST` and the message inside it — but **nobody is reading those fields**. Here is what's actually happening:

```
CustomException is thrown
(it has status=400 and message="UserID is missing" inside it,
but those are just fields sitting in the object)
        │
        ▼
Goes to Resolver 1 (ExceptionHandlerExceptionResolver)
→ "Is there a @ExceptionHandler for CustomException?" 
→ NO ❌
        │
        ▼
Goes to Resolver 2 (ResponseStatusExceptionResolver)
→ "Does CustomException have @ResponseStatus annotation?"
→ NO ❌
        │
        ▼
Goes to Resolver 3 (DefaultHandlerExceptionResolver)
→ "Is this a Spring framework internal exception?"
→ NO ❌
        │
        ▼
DefaultErrorAttributes
→ Nobody set any status in the HTTP response
→ Default = 500
→ Builds response and sends it
```

The key point here is:

> The `status` and `message` fields inside your `CustomException` object are **just Java fields**. They mean nothing to the Spring resolver framework. The framework doesn't go looking inside your exception object for a status code. It only cares about what's been **set into the HTTP response** by one of the 3 resolvers.

---

## The Fix — Two Approaches

The instructor explains there are two ways to handle this properly.

---

### Approach 1 — Take Full Control Yourself (Don't Rely on Resolvers)

Wrap your logic in a try-catch inside the controller method, catch the exception yourself, and **manually build and return a `ResponseEntity`**:

```java
@RestController
@RequestMapping(value = "/api/")
public class UserController {

    @GetMapping(path = "/get-user")
    public ResponseEntity<?> getUser() {
        try {
            // your business logic...
            throw new CustomException(HttpStatus.BAD_REQUEST, "UserID is missing");
        }
        catch (CustomException e) {
            ErrorResponse errorResponse = new ErrorResponse(
                new Date(),
                e.getMessage(),
                e.getStatus().value()
            );
            return new ResponseEntity<>(errorResponse, e.getStatus());
        }
        catch (Exception e) {
            ErrorResponse errorResponse = new ErrorResponse(
                new Date(),
                e.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
```

The `ErrorResponse` is just a simple POJO with three fields:

```java
public class ErrorResponse {
    private Date timestamp;
    private String msg;
    private int status;

    public ErrorResponse(Date timestamp, String msg, int status) {
        this.msg = msg;
        this.status = status;
        this.timestamp = timestamp;
    }
    // getters...
}
```

Now when you hit the API you get:

```json
{
  "timestamp": "2024-10-25T12:13:02.318+00:00",
  "status": 400,
  "message": "UserID is missing"
}
```

**Why does this work?**
Because you're not relying on any resolver at all. You caught the exception yourself, you read the status and message from it yourself, you built the `ResponseEntity` yourself, and you returned it. The resolver flow is completely bypassed.

---

### Approach 2 — Use the Resolver Framework Properly

Instead of try-catch everywhere, let the framework handle it — but **tell the framework how to handle it** using `@ExceptionHandler` or `@ControllerAdvice`. The instructor says this is the cleaner, industry-preferred approach — and this is exactly what Parts 4, 5, and 6 will cover in detail.

---

## The Core Rule to Remember

The instructor repeats this multiple times and it's worth remembering word for word:

```
If you want FULL CONTROL over the response
→ always return a ResponseEntity yourself
→ resolvers are not involved at all

If you don't return a ResponseEntity
→ your exception goes through all 3 resolvers
→ if none handles it → DefaultErrorAttributes sets 500
→ if one handles it → DefaultErrorAttributes uses that status/message
```

---

> 💡 **Interview Tip:** The instructor specifically says *"many companies still follow the approach of returning ResponseEntity directly from try-catch"* — they don't rely on the resolver framework at all. So if an interviewer asks *"how do you handle exceptions in Spring Boot"* — knowing BOTH approaches (manual ResponseEntity vs. resolver framework) and when to use which, puts you way ahead.

---

# Part 4: ExceptionHandlerExceptionResolver — The First & Most Important Resolver

---

This is the resolver you'll use the most in real projects. The instructor spends the most time here and walks through multiple use cases one by one. Let's go through each exactly as he teaches it.

---

## What Does This Resolver Handle?

This resolver is responsible for handling exceptions through two annotations:

```
ExceptionHandlerExceptionResolver handles:

1. @ExceptionHandler   → method level annotation
                         written inside a controller class
                         (controller-level handling)

2. @ControllerAdvice   → class level annotation
                         written on a separate class
                         (global-level handling)
```

---

## Part A — Controller Level Exception Handling

---

### The Basic Idea

Instead of writing try-catch in every method, you write **one special method inside your controller** annotated with `@ExceptionHandler`. Whenever any method in that controller throws the specified exception, Spring automatically routes it to this handler method.

```
Controller Class
│
├── getUser()          → throws CustomException ──┐
├── getUserHistory()   → throws CustomException ──┤
├── createUser()       → throws CustomException ──┤
│                                                  │
└── @ExceptionHandler(CustomException.class)  ◄───┘
    handleCustomException()
    → ALL of the above get caught here automatically
```

No try-catch needed in any of the business methods.

---

### Use Case 1 — Basic @ExceptionHandler returning a String message

```java
@RestController
@RequestMapping(value = "/api/")
public class UserController {

    @GetMapping(path = "/get-user")
    public ResponseEntity<?> getUser() {
        // your business logic...
        throw new CustomException(HttpStatus.BAD_REQUEST, "UserID is missing");
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<String> handleCustomException(CustomException ex) {
        return new ResponseEntity<>(ex.getMessage(), ex.getStatus());
    }
}
```

Output:
```
Status  → 400 Bad Request
Body    → UserID is missing        (plain string, no JSON)
```

The instructor points out — notice the response body is just a plain string, not a JSON object. That's because we returned `ResponseEntity<String>`. If you want a proper JSON response, you need to return an object instead. That's the next use case.

---

### Use Case 2 — Returning a proper ErrorResponse object (JSON response)

```java
@RestController
@RequestMapping(value = "/api/")
public class UserController {

    @GetMapping(path = "/get-user")
    public ResponseEntity<?> getUser() {
        throw new CustomException(HttpStatus.BAD_REQUEST, "UserID is missing");
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Object> handleCustomException(CustomException ex) {

        ErrorResponse errorResponse = new ErrorResponse(
            new Date(),
            ex.getMessage(),
            ex.getStatus().value()
        );

        return new ResponseEntity<>(errorResponse, ex.getStatus());
    }
}
```

Output:
```json
{
  "timestamp": "2024-10-24T15:41:24.294+00:00",
  "status": 400,
  "message": "UserID is missing"
}
```

Now you get a proper JSON object back. This is the standard way most companies return error responses.

---

### Use Case 3 — Multiple @ExceptionHandlers in one Controller

You can have as many `@ExceptionHandler` methods as you need in a single controller — one for each exception type:

```java
@RestController
@RequestMapping(value = "/api/")
public class UserController {

    @GetMapping(path = "/get-user")
    public ResponseEntity<?> getUser() {
        throw new CustomException(HttpStatus.BAD_REQUEST, "UserID is missing");
    }

    @GetMapping(path = "/get-user-history")
    public ResponseEntity<?> getUserHistory() {
        throw new IllegalArgumentException("inappropriate arguments passed");
    }

    // handles CustomException from any method in this controller
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<String> handleCustomException(CustomException ex) {
        return new ResponseEntity<>(ex.getMessage(), ex.getStatus());
    }

    // handles IllegalArgumentException from any method in this controller
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgException(IllegalArgumentException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }
}
```

Each handler takes care of its own exception type. Clean and organized.

---

### Use Case 4 — One @ExceptionHandler handling Multiple Exceptions

If two exceptions need to be handled the same way, you don't have to write two separate methods. You can group them:

```java
// Instead of this (duplicate logic):
@ExceptionHandler(CustomException.class)
public ResponseEntity<String> handleCustomException(CustomException ex) {
    return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
}

@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<String> handleIllegalArgException(IllegalArgumentException ex) {
    return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
}

// You can do this (cleaner):
@ExceptionHandler({CustomException.class, IllegalArgumentException.class})
public ResponseEntity<String> handleMultipleExceptions(Exception ex) {
    return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
}
```

**Important note from the instructor:**
When your `@ExceptionHandler` handles only ONE exception type, the method parameter can be that exact exception type. But when it handles **multiple exception types**, you must use the **parent class** (`Exception`) as the method parameter — because the method needs to accept any of the listed exceptions.

---

### What Parameters Can an @ExceptionHandler Method Accept?

The instructor specifically calls this out because it's a common point of confusion. An `@ExceptionHandler` method can accept these parameters **in any order**:

```
1. The Exception object      → CustomException ex  (or Exception ex for multiple)
2. HttpServletRequest        → HttpServletRequest request
3. HttpServletResponse       → HttpServletResponse response
```

```java
// All of these are valid:
public ResponseEntity<?> handleException(CustomException ex) { }
public ResponseEntity<?> handleException(HttpServletRequest req, CustomException ex) { }
public ResponseEntity<?> handleException(CustomException ex, HttpServletResponse res) { }
public ResponseEntity<?> handleException(HttpServletRequest req, CustomException ex, HttpServletResponse res) { }
```

Spring internally knows how to fill in each of these parameters. You can use any one, two, or all three, in any order.

**What you CANNOT do:**
```java
// This will FAIL — Spring doesn't know how to fill a String parameter
public ResponseEntity<?> handleException(CustomException ex, String someRandomParam) { }
```

---

### Use Case 5 — @ExceptionHandler NOT returning ResponseEntity (letting DefaultErrorAttributes build the response)

This is a slightly advanced use case. Instead of returning a `ResponseEntity` from your handler, you just set the status and message into the `HttpServletResponse` and let `DefaultErrorAttributes` build the response for you:

```java
@ExceptionHandler(CustomException.class)
public void handleCustomException(HttpServletResponse response,
                                  CustomException ex) throws IOException {
    response.sendError(HttpStatus.BAD_REQUEST.value(), ex.message);
}
```

**What happens here:**
```
CustomException thrown
        │
        ▼
ExceptionHandlerExceptionResolver
→ finds our @ExceptionHandler ✅
→ invokes handleCustomException()
→ sets status = 400 and message in HttpServletResponse
→ does NOT return ResponseEntity
        │
        ▼
DefaultErrorAttributes
→ reads status = 400 from response ✅
→ reads message from response ✅
→ builds full response object
→ sends it back to client
```

Output:
```json
{
  "timestamp": "2024-10-24T15:55:07.807+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "UserID is missing",
  "path": "/api/get-user"
}
```

**Note:** To see the `message` field in the response, you need to add this to your `application.properties`:
```properties
server.error.include-message=always
```
Without this, `DefaultErrorAttributes` filters out the message field by default.

---

## Part B — Global Exception Handling with @ControllerAdvice

---

### The Problem with Controller-Level Handling

The instructor raises a very practical point here. Imagine you have 100 controllers in a large application. Every single controller might throw `CustomException`. If you're doing controller-level handling, you'd have to write the same `@ExceptionHandler` method in **every single controller**. That's pure code duplication.

```
UserController        → @ExceptionHandler(CustomException.class) { same code }
OrderController       → @ExceptionHandler(CustomException.class) { same code }
InvoiceController     → @ExceptionHandler(CustomException.class) { same code }
PaymentController     → @ExceptionHandler(CustomException.class) { same code }
...100 controllers... → same code everywhere ❌
```

This is exactly the problem `@ControllerAdvice` solves.

---

### The Solution — @ControllerAdvice

You create **one separate class**, annotate it with `@ControllerAdvice`, and write your exception handlers there. This class now acts as a **global exception handler** for your entire application — all controllers, all methods.

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<String> handleCustomException(CustomException ex) {
        return new ResponseEntity<>(ex.message, ex.getStatus());
    }
}
```

Now no matter which controller throws `CustomException`, this single method handles it.

```
UserController    → throws CustomException ──┐
OrderController   → throws CustomException ──┤
InvoiceController → throws CustomException ──┤──► GlobalExceptionHandler
PaymentController → throws CustomException ──┘    handles all of them ✅
```

---

### Priority — Controller Level vs Global Level

The instructor demonstrates this with a specific example where **both** a controller-level and a global-level handler exist for the same exception:

```java
// Global handler
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<String> handleCustomException(CustomException ex) {
        return new ResponseEntity<>(ex.message + ": from Global ExceptionHandler",
                                    ex.getStatus());
    }
}

// Controller-level handler (inside UserController)
@ExceptionHandler(CustomException.class)
public ResponseEntity<String> handleCustomException(CustomException ex) {
    return new ResponseEntity<>(ex.message + ": from Controller ExceptionHandler",
                                ex.getStatus());
}
```

**Result:**
```
Output → "UserID is missing: from Controller ExceptionHandler"
```

**Rule:**
```
Controller-level @ExceptionHandler  →  HIGHER priority  (checked first)
Global @ControllerAdvice            →  LOWER priority   (fallback)
```

The resolver first checks the controller where the exception originated. Only if no handler is found there does it go to the global `@ControllerAdvice`.

---

### Priority — When Two Handlers Can Handle the Same Exception

What if inside your `@ControllerAdvice` you have two handlers — one for `CustomException` and one for `RuntimeException` (which is the parent of `CustomException`)?

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<String> handleCustomException(CustomException ex) {
        return new ResponseEntity<>(ex.message, ex.getStatus());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
```

Which one handles a `CustomException`?

**Rule — it always follows the class hierarchy, bottom to top:**
```
Step 1 → Look for EXACT match first
         CustomException handler exists? → YES → use it ✅

Step 2 → If no exact match, go UP the hierarchy
         RuntimeException handler exists? → use it as fallback
```

So `CustomException` → handled by `handleCustomException()` ✅
If you remove that, then → handled by `handleRuntimeException()` as fallback

---

## The Complete Priority Order — Summary

```
When an exception occurs:

1st → Check controller where exception came from
      → any @ExceptionHandler for this exact exception? → use it

2nd → Check controller where exception came from
      → any @ExceptionHandler for a parent of this exception? → use it

3rd → Check @ControllerAdvice global class
      → any @ExceptionHandler for this exact exception? → use it

4th → Check @ControllerAdvice global class
      → any @ExceptionHandler for a parent of this exception? → use it

5th → No handler found anywhere
      → move to next resolver (ResponseStatusExceptionResolver)
```

---

> 💡 **Interview Tips:**
> - *"What is the difference between @ExceptionHandler and @ControllerAdvice?"* → `@ExceptionHandler` is method-level, scoped to one controller. `@ControllerAdvice` is class-level, scoped globally to all controllers.
> - *"Which has higher priority — controller-level or global-level handler?"* → Controller-level always takes priority over global.
> - *"How does Spring decide which handler to use when multiple handlers match?"* → It follows the class hierarchy — exact match first, then parent class, going bottom to top.
> - *"What parameters can an @ExceptionHandler method take?"* → `Exception`, `HttpServletRequest`, `HttpServletResponse`, in any order.

---

# Part 5: ResponseStatusExceptionResolver — The Tricky One

---

The instructor calls this the **most confusing** of the three resolvers. A lot of engineers get the definition wrong and misunderstand how it behaves when combined with `@ExceptionHandler`. Let's break it down carefully.

---

## What Does This Resolver Handle?

The correct definition — and the instructor is very specific about this:

```
ResponseStatusExceptionResolver handles:

UNCAUGHT exceptions that are annotated with @ResponseStatus
```

Two conditions BOTH must be true:
```
Condition 1 → The exception must be UNCAUGHT
              (no @ExceptionHandler handled it before this resolver ran)

Condition 2 → The exception class must have
              @ResponseStatus annotation on it
```

The word **"uncaught"** is what trips most people up. The instructor explains this in detail through the use cases below.

---

## Part A — Use Case 1: @ResponseStatus on an Exception Class

This is the straightforward case. You put `@ResponseStatus` directly on your custom exception class:

```java
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CustomException extends RuntimeException {

    CustomException(String message) {
        super(message);
    }
}
```

And your controller just throws it — no `@ExceptionHandler` anywhere:

```java
@RestController
@RequestMapping(value = "/api/")
public class UserController {

    @GetMapping(path = "/get-user")
    public ResponseEntity<?> getUser() {
        throw new CustomException("UserID is missing");
    }
}
```

**What happens:**

```
CustomException thrown (no @ExceptionHandler exists for it)
        │
        ▼
Resolver 1 (ExceptionHandlerExceptionResolver)
→ any @ExceptionHandler for CustomException? NO ❌
        │
        ▼
Resolver 2 (ResponseStatusExceptionResolver)
→ does CustomException have @ResponseStatus? YES ✅
→ reads status = BAD_REQUEST from the annotation
→ sets status = 400 into HttpServletResponse
→ marks exception as handled
        │
        ▼
DefaultErrorAttributes
→ reads status = 400 ✅
→ builds response and sends it
```

Output:
```json
{
  "timestamp": "2024-10-25T12:50:42.453+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "UserID is missing",
  "path": "/api/get-user"
}
```

---

## @ResponseStatus with a `reason` — Two Options

The `@ResponseStatus` annotation has two attributes you can set:

```
value  → the HTTP status code
reason → the message to show in the response
```

```java
// Option 1 — status only
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CustomException extends RuntimeException { ... }

// Option 2 — status + reason (message)
@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Invalid Request Passed")
public class CustomException extends RuntimeException { ... }
```

**What changes when you add `reason`:**

```
Without reason:
→ message in response = whatever was passed to super(message)
→ "UserID is missing"

With reason:
→ message in response = the reason from @ResponseStatus annotation
→ "Invalid Request Passed"   ← this OVERRIDES your exception message
```

The instructor makes this very clear — **`reason` takes higher priority** over the message you pass into the exception constructor. If you define a `reason`, that's what shows up in the response, not your exception's message.

---

## Part B — Use Case 2: @ResponseStatus on an @ExceptionHandler Method

This is where it gets confusing. The instructor says this is the scenario that **makes many engineers scratch their heads**.

What if you put `@ResponseStatus` not on the exception class, but on an `@ExceptionHandler` method?

```java
@RestController
@RequestMapping(value = "/api/")
public class UserController {

    @GetMapping(path = "/get-user")
    public ResponseEntity<?> getUser() {
        throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "UserID is missing");
    }

    @ExceptionHandler(CustomException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Invalid Request Sent")
    public ResponseEntity<Object> handleCustomException(CustomException e) {
        return new ResponseEntity<>("you are not authorized", HttpStatus.FORBIDDEN);
    }
}
```

Look at this carefully. There are **two conflicting things** happening:

```
Inside the method body:
→ returning ResponseEntity with status = FORBIDDEN (403)
→ body = "you are not authorized"

On the @ResponseStatus annotation:
→ status = BAD_REQUEST (400)
→ reason = "Invalid Request Sent"
```

**What does the engineer expect (wrongly)?**

Many engineers think the flow goes like this:
```
❌ WRONG mental model:

Exception thrown
→ Resolver 1 handles it → sets FORBIDDEN + "you are not authorized"
→ Resolver 2 then overrides it → sets BAD_REQUEST + "Invalid Request Sent"
```

**What actually happens:**

```
✅ CORRECT flow:

Exception thrown
        │
        ▼
Resolver 1 (ExceptionHandlerExceptionResolver)
→ finds @ExceptionHandler for CustomException ✅
→ invokes handleCustomException()
→ method returns ResponseEntity (FORBIDDEN, "you are not authorized")
→ exception is marked as HANDLED
→ Resolver 2 is NEVER called
        │
        ▼
BUT — Spring framework itself (not any resolver)
→ reads the @ResponseStatus on the handler method
→ OVERRIDES the status with BAD_REQUEST
→ OVERRIDES the message with "Invalid Request Sent"
        │
        ▼
DefaultErrorAttributes
→ reads BAD_REQUEST ✅
→ reads "Invalid Request Sent" ✅
→ builds and sends response
```

Output:
```json
{
  "timestamp": "2024-10-25T13:05:53.089+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid Request Sent",
  "path": "/api/get-user"
}
```

**Key insight the instructor gives:**

> The `@ResponseStatus` override here is done by **Spring's request handling mechanism** (`ServletInvocableHandlerMethod`) — NOT by `ResponseStatusExceptionResolver`. The resolver never even gets involved because Resolver 1 already handled the exception. Spring framework itself processes the `@ResponseStatus` annotation after the handler method runs and overrides whatever was set.

---

## Part C — The Dangerous Combination: @ResponseStatus + response.sendError()

This is the case the instructor warns about most strongly. What if your `@ExceptionHandler` method uses `response.sendError()` instead of returning a `ResponseEntity`, AND also has `@ResponseStatus`?

```java
@ExceptionHandler(CustomException.class)
@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Invalid Request Sent")
public void handleCustomException(CustomException e,
                                  HttpServletResponse response) throws IOException {
    response.sendError(HttpStatus.FORBIDDEN.value(), "you are not authorized");
}
```

**What happens here:**

```
@ExceptionHandler invoked
        │
        ▼
response.sendError(FORBIDDEN, "you are not authorized")
→ sets status = 403 in HttpServletResponse
→ sets message = "you are not authorized"
→ COMMITS the response ⚠️
        │
        ▼
Spring framework tries to process @ResponseStatus
→ tries to call response.sendError(BAD_REQUEST, "Invalid Request Sent")
→ BUT response is already committed ❌
→ you CANNOT write to a committed response
→ this throws an Exception INSIDE ExceptionHandlerExceptionResolver itself
        │
        ▼
DefaultErrorAttributes
→ something went wrong inside the resolver itself
→ default = 500 Internal Server Error 💥
```

Output:
```
500 Internal Server Error
```

The instructor explains why:

> When you call `response.sendError()`, it sets the status AND **commits** the response — meaning it's finalized and sent. After that, nobody can change it. When Spring then tries to apply `@ResponseStatus` and calls `sendError()` again on an already-committed response, it throws an exception inside the resolver itself, which ultimately results in 500.

---

## The Golden Rule — Never Mix These Two Together

The instructor is very direct:

```
❌ DON'T do this:

@ExceptionHandler(CustomException.class)
@ResponseStatus(...)
public ResponseEntity<?> handleException(...) {
    return new ResponseEntity<>(...);   // two things fighting each other
}

❌ DON'T do this either:

@ExceptionHandler(CustomException.class)
@ResponseStatus(...)
public void handleException(..., HttpServletResponse response) {
    response.sendError(...);   // will cause 500
}
```

If you absolutely MUST use `@ExceptionHandler` and `@ResponseStatus` together, give responsibility to only ONE of them:

```java
// ✅ Option 1 — Let @ResponseStatus do the work, method does nothing
@ExceptionHandler(CustomException.class)
@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Invalid Request Sent")
public void handleCustomException(CustomException e) {
    // do nothing here — let @ResponseStatus set the status and message
}
```

Output:
```json
{
  "timestamp": "2024-10-25T14:52.309+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid Request Sent",
  "path": "/api/get-user"
}
```

```java
// ✅ Option 2 — Let the method do the work, don't use @ResponseStatus
@ExceptionHandler(CustomException.class)
public ResponseEntity<Object> handleCustomException(CustomException e) {
    return new ResponseEntity<>("your message here", HttpStatus.BAD_REQUEST);
}
```

---

## Clean Summary of ResponseStatusExceptionResolver

```
@ResponseStatus on Exception CLASS + no @ExceptionHandler
→ handled by ResponseStatusExceptionResolver ✅
→ reads status (and reason if present) from annotation
→ sets into HttpServletResponse
→ DefaultErrorAttributes builds the final response

@ResponseStatus on @ExceptionHandler METHOD
→ ResponseStatusExceptionResolver is NEVER involved
→ ExceptionHandlerExceptionResolver handles it (Resolver 1)
→ Spring framework itself applies @ResponseStatus override afterward
→ @ResponseStatus wins over whatever the method returned

@ResponseStatus + response.sendError() together
→ response gets committed after sendError()
→ Spring can't apply @ResponseStatus afterward
→ exception thrown inside resolver
→ results in 500 💥
→ AVOID this combination
```

---

> 💡 **Interview Tips:**
> - *"What does ResponseStatusExceptionResolver handle?"* → Uncaught exceptions annotated with `@ResponseStatus`. Stress the word **uncaught** — that's the key word most people miss.
> - *"What is the difference between `value` and `reason` in @ResponseStatus?"* → `value` sets the HTTP status code, `reason` sets the message. If `reason` is provided, it overrides the exception's own message.
> - *"What happens if you use @ResponseStatus and @ExceptionHandler together?"* → `ResponseStatusExceptionResolver` is not involved at all. Spring's own request handling mechanism applies the `@ResponseStatus` override after `ExceptionHandlerExceptionResolver` runs. Never use `response.sendError()` alongside `@ResponseStatus` — it causes 500.

---

# Part 6: DefaultHandlerExceptionResolver + Complete Summary & Interview Tips

---

## DefaultHandlerExceptionResolver — The Third and Last Resolver

---

### What Does This Resolver Handle?

This one is the simplest of the three. The instructor keeps the explanation short and sweet because there isn't much complexity here.

```
DefaultHandlerExceptionResolver handles:

Spring framework's OWN internal exceptions ONLY

Examples:
→ NoResourceFoundException     (wrong URL / endpoint doesn't exist)
→ MethodNotAllowedException    (wrong HTTP method — POST instead of GET)
→ MissingServletRequestParameterException  (required param missing)
→ HttpMediaTypeNotSupportedException       (wrong content type)
```

It has a **predefined, fixed list** of Spring framework exceptions it knows about. It does NOT understand your custom exceptions. It does NOT understand `NullPointerException`. It only handles exceptions that Spring itself generates internally.

---

### When Does Control Reach This Resolver?

Remember the sequence — this resolver only gets called if BOTH Resolver 1 and Resolver 2 couldn't handle the exception:

```
Request comes in
      │
      ▼
DispatcherServlet can't even find your controller
(because the URL doesn't exist)
      │
      ▼
Exception generated by Spring itself
(NoResourceFoundException)
      │
      ▼
Resolver 1 → ❌ no @ExceptionHandler for this
Resolver 2 → ❌ no @ResponseStatus on it
Resolver 3 → ✅ "I know this one — it's NoResourceFoundException"
      │
      ▼
Sets status = 404 into HttpServletResponse
      │
      ▼
DefaultErrorAttributes builds and sends the response
```

---

### Code Example

The instructor shows this with a very simple case — hitting an API endpoint that doesn't exist:

```
GET /api/get-user-3223   ← this endpoint doesn't exist
```

```json
{
  "timestamp": "2024-10-25T14:52:309+00:00",
  "status": 404,
  "error": "No Static Resource get-user-3223",
  "path": "/api/get-user-3223"
}
```

Spring internally generates `NoResourceFoundException`. This flows through Resolver 1 and 2 without being handled, reaches Resolver 3, which recognizes it immediately and sets the correct 404 status and message.

---

### What This Resolver Does NOT Handle

```
Your CustomException           → ❌ not a Spring internal exception
NullPointerException           → ❌ not a Spring internal exception
IllegalArgumentException       → ❌ not a Spring internal exception
Any exception you created      → ❌ not a Spring internal exception
```

The instructor's point is simple — **don't expect this resolver to save you from your own exceptions**. It's purely a safety net for Spring's own framework-level errors.

---

## Complete Flow Summary — Everything Together

Now let's put everything from all 6 parts into one single picture. This is the complete mental model:

```
HTTP Request
      │
      ▼
DispatcherServlet
      │
      ├── invokes your Controller method
      │         │
      │         └── Exception thrown
      │
      └── OR can't find Controller at all
                │
                └── Spring generates its own Exception
      │
      ▼
HandlerExceptionResolverComposite (Orchestrator)
      │
      │ calls resolvers LEFT → RIGHT
      │
      ▼
┌─────────────────────────────────────────────┐
│  RESOLVER 1                                 │
│  ExceptionHandlerExceptionResolver          │
│                                             │
│  Looks for:                                 │
│  → @ExceptionHandler in same controller     │
│    (exact match first, then parent class)   │
│  → @ExceptionHandler in @ControllerAdvice   │
│    (exact match first, then parent class)   │
│                                             │
│  Priority order:                            │
│  1. Controller-level exact match            │
│  2. Controller-level parent match           │
│  3. Global-level exact match                │
│  4. Global-level parent match               │
└──────────────────┬──────────────────────────┘
                   │
            Handled? YES ──────────────────────┐
                   │ NO                        │
                   ▼                           │
┌─────────────────────────────────────────────┐│
│  RESOLVER 2                                 ││
│  ResponseStatusExceptionResolver            ││
│                                             ││
│  Looks for:                                 ││
│  → @ResponseStatus on the Exception CLASS   ││
│  → reads value (status) and reason (msg)    ││
│                                             ││
│  Does NOT handle:                           ││
│  → exceptions already caught by Resolver 1  ││
│  → exceptions without @ResponseStatus       ││
└──────────────────┬──────────────────────────┘│
                   │                           │
            Handled? YES ──────────────────────┤
                   │ NO                        │
                   ▼                           │
┌─────────────────────────────────────────────┐│
│  RESOLVER 3                                 ││
│  DefaultHandlerExceptionResolver            ││
│                                             ││
│  Looks for:                                 ││
│  → Spring framework internal exceptions     ││
│    (404, 405, 415, missing params etc.)     ││
│                                             ││
│  Does NOT handle:                           ││
│  → any custom exceptions                    ││
│  → any Java built-in exceptions             ││
└──────────────────┬──────────────────────────┘│
                   │                           │
                   └──────────────┬────────────┘
                                  │
                    (all paths lead here)
                                  ▼
                   ┌───────────────────────────┐
                   │   DefaultErrorAttributes  │
                   │                           │
                   │  → reads status from      │
                   │    HttpServletResponse    │
                   │  → if nothing set → 500   │
                   │  → builds response body:  │
                   │    {                      │
                   │      timestamp,           │
                   │      status,              │
                   │      error,               │
                   │      message,             │
                   │      path                 │
                   │    }                      │
                   │  → creates ResponseEntity │
                   │  → sends to client        │
                   └───────────────────────────┘
```

---

## When to Use What — Decision Guide

The instructor gives you enough to build this decision guide yourself:

```
SITUATION                              WHAT TO USE
─────────────────────────────────────────────────────────────
Want full manual control,              try-catch in each method
small application                      + return ResponseEntity yourself

Same exception in one controller,      @ExceptionHandler inside
handle it one place                    that controller

Same exception across many             @ControllerAdvice
controllers, avoid duplication         with @ExceptionHandler methods

Exception class itself should          @ResponseStatus on the
carry its own status                   Exception class
(no handler needed)

Spring framework errors                Nothing needed —
(404, 405, etc.)                       DefaultHandlerExceptionResolver
                                       handles automatically
```

---

## All Interview Tips — Collected in One Place

---

### On the 5 Key Classes

> *"What classes are involved in exception handling in Spring Boot?"*

Don't just say `@ControllerAdvice`. Walk through all 5:
- `HandlerExceptionResolverComposite` — orchestrates the 3 resolvers
- `ExceptionHandlerExceptionResolver` — handles `@ExceptionHandler` and `@ControllerAdvice`
- `ResponseStatusExceptionResolver` — handles uncaught exceptions with `@ResponseStatus`
- `DefaultHandlerExceptionResolver` — handles Spring's own internal exceptions
- `DefaultErrorAttributes` — builds and sends the final response

---

### On the 500 Default Status

> *"Why does Spring Boot return 500 even when I throw a BAD_REQUEST exception?"*

Because you're not returning a `ResponseEntity` yourself. The exception goes through all 3 resolvers. If none of them recognize it, `DefaultErrorAttributes` sets 500 as the default status. The status field inside your custom exception object means nothing to the resolver framework — it's just a Java field.

---

### On @ExceptionHandler vs @ControllerAdvice

> *"What is the difference between @ExceptionHandler and @ControllerAdvice?"*

- `@ExceptionHandler` is a method-level annotation written inside a controller — scoped to that one controller only
- `@ControllerAdvice` is a class-level annotation on a separate class — scoped globally to all controllers
- Controller-level always has higher priority than global-level

---

### On Handler Priority

> *"Which handler gets priority when multiple handlers can handle the same exception?"*

It follows the class hierarchy from bottom to top:
1. Controller-level exact match
2. Controller-level parent class match
3. Global-level exact match
4. Global-level parent class match

---

### On @ExceptionHandler Method Parameters

> *"What parameters can an @ExceptionHandler method accept?"*

Three supported parameters, in any order:
- The exception object (`CustomException ex` or `Exception ex` for multiple)
- `HttpServletRequest`
- `HttpServletResponse`

You cannot pass arbitrary parameters like `String` — Spring won't know how to fill them.

---

### On ResponseStatusExceptionResolver

> *"What does ResponseStatusExceptionResolver handle?"*

Stress the word **uncaught** — it handles uncaught exceptions annotated with `@ResponseStatus`. If the exception was already handled by `@ExceptionHandler` (Resolver 1), this resolver never gets involved.

---

### On @ResponseStatus + @ExceptionHandler Together

> *"What happens if you use @ResponseStatus and @ExceptionHandler together?"*

- `ResponseStatusExceptionResolver` is NOT involved at all
- `ExceptionHandlerExceptionResolver` handles the exception (Resolver 1)
- Spring's own request handling mechanism (`ServletInvocableHandlerMethod`) applies `@ResponseStatus` afterward and overrides whatever status the method returned
- Never use `response.sendError()` alongside `@ResponseStatus` — the response gets committed after `sendError()`, Spring can't override it, and you get 500

---

### On Two Approaches to Exception Handling

> *"How do you handle exceptions in Spring Boot?"*

Know BOTH approaches:
- **Manual approach** — try-catch in each method, build and return `ResponseEntity` yourself, resolvers not involved at all. Many companies still follow this.
- **Framework approach** — use `@ExceptionHandler` / `@ControllerAdvice`, let the resolver framework do the work. Cleaner for large applications with many controllers.

---

### On DefaultHandlerExceptionResolver

> *"What does DefaultHandlerExceptionResolver handle?"*

Only Spring framework's own internal exceptions — things like `NoResourceFoundException` (404), `MethodNotAllowedException` (405), `MissingServletRequestParameterException`. It does NOT handle your custom exceptions or standard Java exceptions.

---

### On DefaultErrorAttributes

> *"Who actually creates the response body in exception handling?"*

Always `DefaultErrorAttributes`. The resolvers only **set status and message** into the `HttpServletResponse`. They don't create the `ResponseEntity`. `DefaultErrorAttributes` always runs at the end, reads what was set, builds the full response body with `timestamp`, `status`, `error`, `message`, `path`, creates the `ResponseEntity`, and sends it to the client.

---

These notes now cover the complete lecture — every concept, every use case, every warning, and every interview tip the instructor shared. You should be well prepared both for interviews and for applying this in real projects!