Ready! Let's go step by step — I'll start with **Part 1: The Problem & What RestClient Is**.

---

# RestClient — Synchronous Communication Between Microservices

---

## Part 1 — Why RestClient? (The Problem First)

### What came before: RestTemplate

Before RestClient existed, Spring developers used **RestTemplate** to make HTTP calls between microservices. But RestTemplate had serious problems:

**Problems with RestTemplate:**

```
┌─────────────────────────────────────────────────────────────┐
│                   RestTemplate Problems                     │
├─────────────────────────────────────────────────────────────┤
│ 1. Too many overloaded methods                              │
│    getForObject(), getForEntity(), postForObject(),         │
│    postForEntity(), exchange(), execute()...                │
│    Hard to remember which one to use and when.              │
│                                                             │
│ 2. Not built for modern concepts                            │
│    Retry, Circuit Breaker, etc. were invented AFTER         │
│    RestTemplate. Adding support = more overloaded methods   │
│    = even messier API.                                      │
│                                                             │
│ 3. In Maintenance Mode                                      │
│    No new features. Only bug fixes.                         │
│    Spring team has moved on.                                │
└─────────────────────────────────────────────────────────────┘
```

---

### The Alternatives to RestTemplate

```
┌───────────────────────────────────────────────────────────────────┐
│               Alternatives to RestTemplate                        │
├───────────────────────┬───────────────────────────────────────────┤
│     WebClient         │           RestClient                      │
├───────────────────────┼───────────────────────────────────────────┤
│ Asynchronous,         │ Synchronous, blocking                     │
│ Non-blocking          │                                           │
│                       │ Client WAITS for server's response        │
│ Client does NOT wait  │ before continuing — just like             │
│ for response before   │ RestTemplate, but with a much             │
│ continuing            │ cleaner API                               │
│                       │                                           │
│ Used in Reactive      │ Used in regular Spring MVC apps           │
│ Programming           │ (most common use case)                    │
│ (Spring WebFlux)      │                                           │
│                       │ Introduced in:                            │
│ Will be covered       │ Spring Framework 6.0+                     │
│ separately            │ Spring Boot 3.0+                          │
└───────────────────────┴───────────────────────────────────────────┘
```

So for everyday synchronous microservice communication — RestClient is your go-to tool now. It's the modern replacement for RestTemplate.

---

### The Real-World Setup This Lecture Uses

```
┌─────────────────────┐          HTTP Call          ┌──────────────────────┐
│    Order Service    │ ─────────────────────────►  │   Product Service    │
│   localhost:8081    │                             │   localhost:8082     │
└─────────────────────┘                             └──────────────────────┘

  "When a user places an order, OrderService needs
   to fetch product details from ProductService."
```

This is the simplest form of inter-service communication — one service calling another over HTTP. RestClient is what makes this clean and easy.

---

That's Part 1 done — the problem, the context, and what RestClient is.

# 🎯Part 2 — Fluent API & How to Use RestClient Daily

---

### What is Fluent API? (Quick Recap)

```
┌─────────────────────────────────────────────────────────────────┐
│                    Fluent API = Method Chaining                       │
│                                                                       │
│  restClient                                                           │
│      .get()          ← returns object A                               │
│      .uri(...)       ← called on A, returns object B                  │
│      .retrieve()     ← called on B, returns object C                  │
│      .body(...)      ← called on C, fires the HTTP call               │
│                                                                       │
│  Each method returns the next object in the chain.                    │
│  Each object exposes only the methods relevant at that step.          │
│  Wrong order = compiler stops you. Chain breaks.                      │
└─────────────────────────────────────────────────────────────────┘
```

---

### All Available Methods & Their Purpose

Think of RestClient methods in **4 groups** — based on what stage of the request they belong to:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     Group 1 — HTTP Method Selection                            │
│              (Always the FIRST call on restClient object)                      │
├────────────────┬────────────────────────────────────────────────────────┤
│ .get()          │ Start a GET request — for fetching data                     │
│ .post()         │ Start a POST request — for creating data                    │
│ .put()          │ Start a PUT request — for updating data                     │
│ .delete()       │ Start a DELETE request — for deleting data                  │
└────────────────┴────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                     Group 2 — Request Building                                 │
│         (Called AFTER http method, BEFORE retrieve/exchange)                   │
├────────────────┬────────────────────────────────────────────────────────┤
│ .uri(String)    │ Set the target URL                                          │
│                 │ Example: .uri("http://localhost:8082/products/" + id)       │
│                 │ MUST be called first within this group.                     │
│                 │ Without URL, request can never be sent.                     │
├────────────────┼────────────────────────────────────────────────────────┤
│ .accept(...)    │ Tell the server what response format you expect             │
│                 │ Example: .accept(MediaType.APPLICATION_JSON)                │
│                 │ Sets the "Accept" header internally                         │
├────────────────┼────────────────────────────────────────────────────────┤
│ .header(k, v)   │ Add any custom header to the request                        │
│                 │ Example: .header("Authorization", "Bearer token123")        │
│                 │ Example: .header("X-Request-ID", "abc-xyz")                 │
├────────────────┼────────────────────────────────────────────────────────┤
│ .body(Object)   │ Attach a request payload — POST and PUT only                │
│                 │ Example: .body(new ProductEntity("Ice-cream"))              │
│                 │ NOT available for GET and DELETE                            │
└────────────────┴────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                     Group 3 — Trigger Point                                   │
│              (Marks the boundary — request building is done)                  │
├────────────────┬────────────────────────────────────────────────────────┤
│ .retrieve()     │ Signals: "I am done building the request.                   │
│                 │ Prepare to handle the response."                            │
│                 │ No HTTP call yet. Just moves to response handling.          │
│                 │ Returns a ResponseSpec object.                              │
├────────────────┼────────────────────────────────────────────────────────┤
│ .exchange(...)  │ Skip retrieve() entirely.                                   │
│                 │ You handle BOTH response mapping and exceptions             │
│                 │ yourself inside a lambda.                                   │
│                 │ Use when you need full manual control.                      │
└────────────────┴────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                     Group 4 — Response Handling                               │
│           (Called AFTER retrieve() — THIS fires the HTTP call)                │
├──────────────────────┬──────────────────────────────────────────────────┤
│ .body(Class)           │ Fire the call. Map response body to given type.      │
│                        │ Example: .body(String.class)                         │
│                        │ Example: .body(ProductEntity.class)                  │
│                        │ Returns: just the body object directly               │
├──────────────────────┼──────────────────────────────────────────────────┤
│ .toEntity(Class)       │ Fire the call. Map full response.                    │
│                        │ Returns: ResponseEntity<T>                           │
│                        │ Gives you body + status code + headers               │
│                        │ Example: .toEntity(ProductEntity.class)              │
├──────────────────────┼──────────────────────────────────────────────────┤
│ .toBodilessEntity()    │ Fire the call. Expect NO response body.              │
│                        │ Returns: ResponseEntity<Void>                        │
│                        │ Used for DELETE — you only care about status         │
├──────────────────────┼──────────────────────────────────────────────────┤
│ .onStatus(lambda)      │ Define exception handling before .body()             │
│                        │ Called after retrieve(), before body/toEntity        │
│                        │ Lambda receives response, you check status code      │
│                        │ throw your custom exception or return false          │
└──────────────────────┴──────────────────────────────────────────────────┘
```

---

## 🧠**In each group, can we call them in any order, or does it have to maintain some order?**

---

Great question. Let’s break this down group by group clearly.

---

### Order Rules — Group by Group

---

### Group 1 — HTTP Method Selection

```
┌─────────────────────────────────────────────────────────────────┐
│                        Group 1 Rule                                   │
│                                                                       │
│  ALWAYS first. No flexibility here.                                   │
│                                                                       │
│  restClient.get()  / .post() / .put() / .delete()                     │
│                                                                       │
│  Why: restClient object itself only exposes these 4 methods.          │
│  Nothing else exists on it. You have no choice but to start           │
│  here. The moment you pick one, it opens up Group 2 methods.          │
└─────────────────────────────────────────────────────────────────┘
```

---

### Group 2 — Request Building

This is where order actually gets interesting. The methods here are:
`.uri()` → `.accept()` → `.header()` → `.body()` (POST/PUT only)

```
┌─────────────────────────────────────────────────────────────────┐
│                        Group 2 Rule                                   │
│                                                                       │
│  .uri()    → MUST come first within this group. STRICT.               │
│                                                                       │
│  .accept() → can come in any order AFTER .uri()                       │
│  .header() → can come in any order AFTER .uri()                       │
│                                                                       │
│  .body()   → MUST come last within this group. STRICT.                │
│  (POST/PUT only — after all headers are set)                          │
└─────────────────────────────────────────────────────────────────┘
```

Let's understand why each rule exists:

**Why `.uri()` must be first in Group 2:**

```
After .get(), you land on an object that has BOTH:
  - uri()    ← from UriSpec
  - accept() ← from RequestHeaderSpec
  - header() ← from RequestHeaderSpec

Tempting to call .accept() first right? But if you do:

  restClient
      .get()
      .accept(MediaType.APPLICATION_JSON)  ← called first
      .uri(...)                            ← ❌ GONE. Not available anymore.

Why? Because .accept() returns the same RequestHeaderSpec object
which only has accept(), header(), retrieve().
uri() is on a DIFFERENT parent — UriSpec.
Once you go into header territory, you can never come back to set URI.
Request has no URL. It can never be sent.
```

**Why `.accept()` and `.header()` can be in any order between themselves:**

```
  restClient
      .get()
      .uri(...)
      .accept(MediaType.APPLICATION_JSON)   ✅ this order works
      .header("X-Custom-Header", "xyz")

  restClient
      .get()
      .uri(...)
      .header("X-Custom-Header", "xyz")     ✅ this order also works
      .accept(MediaType.APPLICATION_JSON)

Why? Both .accept() and .header() return the same type of object
back — RequestHeaderSpec. So after calling either one, you still
have access to the other. The chain doesn't break either way.
They're both just adding metadata to the same request object.
No dependency between them.
```

**Why `.body()` must come last in Group 2 (POST/PUT):**

```
  restClient
      .post()
      .uri(...)
      .body(new ProductEntity("Ice-cream"))  ← set body first
      .header("Content-Type", "application/json")  ← ❌ GONE.

Why? Once you call .body(), it returns a different object
that only has .retrieve() on it.
Header methods are no longer available.
So always set all your headers BEFORE setting the body.

Correct order:
  .uri(...)
  .accept(...)       ← headers first
  .header(...)       ← headers first
  .body(object)      ← payload last, just before retrieve()
```

---

### Group 3 — Trigger Point

```
┌─────────────────────────────────────────────────────────────────┐
│                        Group 3 Rule                                   │
│                                                                       │
│  Only one method here — .retrieve() or .exchange()                    │
│  No ordering question. Pick one, move to Group 4.                     │
│                                                                       │
│  .retrieve() → standard path, use Group 4 methods after               │
│  .exchange() → manual path, you handle everything in lambda           │
│                Group 4 methods NOT available after exchange()         │
└─────────────────────────────────────────────────────────────────┘
```

---

### Group 4 — Response Handling

```
┌─────────────────────────────────────────────────────────────────┐
│                        Group 4 Rule                                   │
│                                                                       │
│  .onStatus()  → if used, MUST come before .body()/.toEntity()         │
│                                                                       │
│  .body()             ┐                                                │
│  .toEntity()         ├─ pick exactly ONE of these. LAST call.         │
│  .toBodilessEntity() ┘                                                │
└─────────────────────────────────────────────────────────────────┘
```

**Why `.onStatus()` must come before `.body()` / `.toEntity()`:**

```
  restClient
      .get()
      .uri(...)
      .retrieve()
      .onStatus(response -> { ... })   ← registers the error handler
      .body(String.class)              ← fires the call

  .onStatus() just REGISTERS what to do if an error comes.
  It doesn't fire the call itself.
  It returns the same ResponseSpec object back.
  So you still have .body() / .toEntity() available after it.

  But if you call .body() first:
  restClient
      .get()
      .uri(...)
      .retrieve()
      .body(String.class)              ← fires the call immediately
      .onStatus(...)                   ← ❌ too late. call already done.
                                          String has no onStatus() on it.
```

---

### The Complete Order Rules — One View

```
┌─────────────────────────────────────────────────────────────────────┐
│                   Complete Ordering Rules                                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  Group 1   .get() / .post() / .put() / .delete()                          │
│            │                                                              │
│            │  STRICT — always first, no flexibility                       │
│            ▼                                                              │
│  Group 2   .uri(...)              ← STRICT — always first here            │
│                │                                                          │
│                ├── .accept()  ┐  ← FLEXIBLE — any order                   │
│                └── .header()  ┘    between these two                      │
│                │                                                          │
│                └── .body(obj)     ← STRICT — always last here             │
│                    (POST/PUT only)                                        │
│            │                                                              │
│            │  STRICT — all headers before body                            │
│            ▼                                                              │
│  Group 3   .retrieve()            ← only one choice here                  │
│         OR .exchange(lambda)      ← skip Group 4 entirely                 │
│            │                                                              │
│            ▼                                                              │
│  Group 4   .onStatus(lambda)      ← STRICT — must come first              │
│                │                    if you want exception handling        │
│                ▼                                                          │
│            .body(Class)           ┐                                       │
│         OR .toEntity(Class)       ├─ pick ONE — always last               │
│         OR .toBodilessEntity()    ┘                                       │
│                                                                           │
└─────────────────────────────────────────────────────────────────────┘
```

---

### Summary — Strict vs Flexible

```
┌──────────────────────────────┬──────────────────────────────────────┐
│         STRICT order            │         FLEXIBLE order                  │
├──────────────────────────────┼──────────────────────────────────────┤
│ Group 1 before everything       │ .accept() and .header() between         │
│                                 │ themselves — any order fine             │
│ .uri() before accept/header     │                                         │
│                                 │                                         │
│ .body(payload) after all        │                                         │
│ headers in POST/PUT             │                                         │
│                                 │                                         │
│ .retrieve() before Group 4      │                                         │
│                                 │                                         │
│ .onStatus() before              │                                         │
│ .body()/.toEntity()             │                                         │
│                                 │                                         │
│ .body()/.toEntity() always      │                                         │
│ last — fires the HTTP call      │                                         │
└──────────────────────────────┴──────────────────────────────────────┘
```

The flexibility only exists between `.accept()` and `.header()` — everything else has a strict order enforced by the chain itself. Now you know not just WHAT to call, but WHY the order is what it is.

## The Chain for Each HTTP Operation

---

### GET — Fetching Data

```
restClient
    .get()                              ← Group 1: what type of request?
    .uri("http://.../products/" + id)   ← Group 2: where to send it?
    .accept(MediaType.APPLICATION_JSON) ← Group 2: what format back?
    .header("X-Custom-Header", "xyz")   ← Group 2: any extra headers?
    .retrieve()                         ← Group 3: done building, handle response
    .body(String.class);                ← Group 4: fire call, give me body as String
```

**Why this order?**

```
┌─────────────────────────────────────────────────────────────────┐
│  Step          Why it must come here                                  │
├─────────────────────────────────────────────────────────────────┤
│  .get()        First always. Tells RestClient this is a GET.          │
│                Without this, nothing else is available.               │
├─────────────────────────────────────────────────────────────────┤
│  .uri()        Must come before accept/header.                        │
│                Once you call accept() first, you lose access          │
│                to uri() — chain breaks, URL never gets set,           │
│                request can never be sent.                             │
├─────────────────────────────────────────────────────────────────┤
│  .accept()     Optional. Can come in any order AFTER uri().           │
│  .header()     Both just add metadata to the request object.          │
│                Order between these two doesn't matter.                │
├─────────────────────────────────────────────────────────────────┤
│  .retrieve()   Marks end of request building phase.                   │
│                Must come before any response handling method.         │
│                You cannot call .body() without retrieve() first.      │
├─────────────────────────────────────────────────────────────────┤
│  .body()       LAST. This actually fires the HTTP call.               │
│                Cannot come before retrieve().                         │
│                retrieve() gives you the object that has body().       │
└─────────────────────────────────────────────────────────────────┘
```

**What if you need the full response (status + headers + body)?**

```java
// Instead of .body(String.class) at the end, use .toEntity()
ResponseEntity<String> response = restClient
        .get()
        .uri("http://localhost:8082/products/" + id)
        .retrieve()
        .toEntity(String.class);

String body       = response.getBody();
HttpStatusCode status  = response.getStatusCode();
HttpHeaders headers    = response.getHeaders();
```

---

### POST — Sending Data to Create Something

```
restClient
    .post()                                  ← Group 1: POST request
    .uri("http://.../products/create")       ← Group 2: target URL
    .accept(MediaType.APPLICATION_JSON)      ← Group 2: expect JSON back
    .header("Content-Type","application/json") ← Group 2: sending JSON
    .body(new ProductEntity("Ice-cream"))    ← Group 2: request payload
    .retrieve()                              ← Group 3: done building
    .toEntity(ProductEntity.class);          ← Group 4: fire, get full response
```

**Why this order?**

```
┌─────────────────────────────────────────────────────────────────┐
│  Step              Why it must come here                              │
├─────────────────────────────────────────────────────────────────┤
│  .post()           First always. Unlocks .body(Object) for            │
│                    setting request payload — not available            │
│                    in GET or DELETE chains.                           │
├─────────────────────────────────────────────────────────────────┤
│  .uri()            Same rule as GET — must come right after           │
│                    the HTTP method. Set URL before anything else.     │
├─────────────────────────────────────────────────────────────────┤
│  .accept()         Optional. Set expected response format.            │
│  .header()         Optional. Set Content-Type or custom headers.      │
│                    These can be in any order between themselves.      │
├─────────────────────────────────────────────────────────────────┤
│  .body(Object)     The REQUEST payload. Must come BEFORE              │
│                    retrieve(). This is the data you're sending        │
│                    TO the server (not the response).                  │
│                    Confusion alert: .body() appears in Group 2        │
│                    (request payload) AND Group 4 (response body)      │
│                    Context tells you which is which.                  │
├─────────────────────────────────────────────────────────────────┤
│  .retrieve()       Marks end of request building.                     │
├─────────────────────────────────────────────────────────────────┤
│  .toEntity()       Fires the call. Returns full ResponseEntity        │
│                    with body + status code + headers.                 │
└─────────────────────────────────────────────────────────────────┘
```

---

### DELETE — Removing Data

```
restClient
    .delete()                              ← Group 1: DELETE request
    .uri("http://.../products/" + id)      ← Group 2: what to delete
    .retrieve()                            ← Group 3: done building
    .toBodilessEntity();                   ← Group 4: fire, no body expected
```

**Why this order?**

```
┌─────────────────────────────────────────────────────────────────┐
│  Step                Why it must come here                            │
├─────────────────────────────────────────────────────────────────┤
│  .delete()           First always.                                    │
├─────────────────────────────────────────────────────────────────┤
│  .uri()              Must come right after. Same rule always.         │
├─────────────────────────────────────────────────────────────────┤
│  .retrieve()         Comes directly after uri() in DELETE.            │
│                      No body to set, no special headers needed        │
│                      usually. Straight to retrieve.                   │
├─────────────────────────────────────────────────────────────────┤
│  .toBodilessEntity() DELETE responses carry no body.                  │
│                      .body() or .toEntity() would fail here           │
│                      because there's nothing to map.                  │
│                      Use this to just get the status code back.       │
└─────────────────────────────────────────────────────────────────┘

// After the call, check if deletion succeeded:
HttpStatusCode status = response.getStatusCode();
```

---

### The Cheatsheet — Daily Reference

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      RestClient Daily Cheatsheet                              │
├──────────┬──────────────────────────────────────────────────────────────┤
│           │                                                                   │
│  GET      │  restClient                                                       │
│           │      .get()                                                       │
│           │      .uri("http://host/path/" + id)                               │
│           │      .accept(MediaType.APPLICATION_JSON)    // optional           │
│           │      .header("key", "value")               // optional            │
│           │      .retrieve()                                                  │
│           │      .body(YourClass.class)                // body only           │
│           │   OR .toEntity(YourClass.class)            // full response       │
│           │                                                                   │
├──────────┼──────────────────────────────────────────────────────────────┤
│           │                                                                   │
│  POST     │  restClient                                                       │
│           │      .post()                                                      │
│           │      .uri("http://host/path")                                     │
│           │      .accept(MediaType.APPLICATION_JSON)    // optional           │
│           │      .header("Content-Type","application/json") // optional       │
│           │      .body(new YourRequestObject(...))     // payload             │
│           │      .retrieve()                                                  │
│           │      .toEntity(YourResponseClass.class)                           │
│           │                                                                   │
├──────────┼──────────────────────────────────────────────────────────────┤
│           │                                                                   │
│  DELETE   │  restClient                                                       │
│           │      .delete()                                                    │
│           │      .uri("http://host/path/" + id)                               │
│           │      .retrieve()                                                  │
│           │      .toBodilessEntity()                                          │
│           │                                                                   │
├──────────┼──────────────────────────────────────────────────────────────┤
│           │                                                                   │
│Exception  │  ...after .retrieve()...                                          │
│Handling   │      .onStatus(response -> {                                      │
│           │          if (response.getStatusCode().is4xxClientError())         │
│           │              throw new MyException("bad request");                │
│           │          if (response.getStatusCode().is5xxServerError())         │
│           │              throw new MyException("server error");               │
│           │          return false;                                            │
│           │      })                                                           │
│           │      .body(YourClass.class)                                       │
│           │                                                                   │
├──────────┼──────────────────────────────────────────────────────────────┤
│           │                                                                   │
│ .body()   │  BEFORE retrieve() = request payload  (POST/PUT)                  │
│ appears   │  AFTER  retrieve() = response mapping (GET/POST/DELETE)           │
│  twice!   │  Context tells you which is which.                                │
│           │                                                                   │
└──────────┴──────────────────────────────────────────────────────────────┘
```

---

# 🎯Part 3 — GET, POST, DELETE Implementations + Exception Handling

---

### GET Request

The simplest operation — fetch data from another service.

```
┌─────────────────────────────────────────────────────────────────┐
│                     GET Flow Diagram                                  │
│                                                                       │
│  OrderService                          ProductService                 │
│  (port 8081)                           (port 8082)                    │
│                                                                       │
│  GET /orders/{id}                                                     │
│       │                                                               │
│       ▼                                                               │
│  restClient                                                           │
│    .get()          ──── sets HTTP method = GET ────                  │
│    .uri(...)       ──── sets target URL ───────────────────►        │
│    .accept(...)    ──── sets Accept header ────                      │
│    .header(...)    ──── sets custom header ────                      │
│    .retrieve()     ──── prepares response handler ─                  │
│    .body(String)   ──── fires HTTP call, maps response ◄────         │
│                                                                       │
│  returns: String response                                             │
└─────────────────────────────────────────────────────────────────┘
```

**Step 1 — Register RestClient as a Bean (AppConfig):**

```java
@Configuration
public class AppConfig {

    @Bean
    public RestClient restClientInstance() {
        return RestClient.create();
        // internally calls RestClient.builder().build()
        // you can also write that explicitly if needed
    }
}
```

**Step 2 — ProductService (the service being called):**

```java
@RestController
@RequestMapping("/products")
public class ProductController {

    @GetMapping("/{id}")
    public String getProduct(@PathVariable String id) {
        return "Product fetched with id: " + id;
    }
}
```

**Step 3 — OrderService calls ProductService using RestClient:**

```java
@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    RestClient restClient;

    @GetMapping("/{id}")
    public ResponseEntity<String> getOrder(@PathVariable String id) {

        String response = restClient
                .get()
                .uri("http://localhost:8082/products/" + id)
                .accept(MediaType.APPLICATION_JSON)
                .header("X-Custom-Header", "xyz")
                .retrieve()
                .body(String.class);

        System.out.println(
            "Response from Product API called from order service: "
            + response
        );

        return ResponseEntity.ok("order call successful");
    }
}
```

---

### POST Request

POST is used when you need to **send a body** to create something on the other service. The key difference from GET is that after `.uri()`, you now land on `RequestBodySpec` — which exposes the `.body()` method to attach a request payload.

```
┌─────────────────────────────────────────────────────────────┐
│                      POST Flow Diagram                           │
│                                                                  │
│  restClient                                                      │
│    .post()           sets HTTP method = POST                     │
│    .uri(...)         sets target URL                             │
│    .accept(...)      sets Accept header        ← optional        │
│    .header(...)      sets Content-Type header  ← optional        │
│    .body(object)     attaches request payload  ← NEW in POST     │
│    .retrieve()       prepares response handler                   │
│    .toEntity(Class)  fires call, maps full ResponseEntity        │
│                                                                  │
│  returns: ResponseEntity<ProductEntity>                          │
└─────────────────────────────────────────────────────────────┘
```

```
┌──────────────────────────────────────────────────────────────────┐
│              GET vs POST — Key Difference                              │
├────────────────────────┬─────────────────────────────────────────┤
│         GET              │              POST                           │
├────────────────────────┼─────────────────────────────────────────┤
│ No request body          │ Has a request body (.body(...))             │
│ .body(String.class)      │ .body(new ProductEntity(...))               │
│ at end = response type   │ at middle = request payload                 │
│                          │ .toEntity(Class) at end = response          │
├────────────────────────┼─────────────────────────────────────────┤
│ .retrieve()              │ .retrieve()                                 │
│ .body(String.class)      │ .toEntity(ProductEntity.class)              │
│ → returns String         │ → returns ResponseEntity<ProductEntity>     │
└────────────────────────┴─────────────────────────────────────────┘
```

**POST Implementation:**

```java
RestClient restClient = RestClient.create();

ResponseEntity<ProductEntity> response = restClient
        .post()
        .uri("http://localhost:8082/products/create")
        .accept(MediaType.APPLICATION_JSON)
        .header("Content-Type", "application/json")
        .body(new ProductEntity("Ice-cream")) // request payload
        .retrieve()
        .toEntity(ProductEntity.class);       // maps full response

ProductEntity responseBody = response.getBody();
```

---

### DELETE Request

DELETE is simpler — no request body to send, and usually no response body comes back either. So instead of `.body()` or `.toEntity()`, you use `.toBodilessEntity()`.

```
┌─────────────────────────────────────────────────────────────┐
│                    DELETE Flow Diagram                           │
│                                                                  │
│  restClient                                                      │
│    .delete()              sets HTTP method = DELETE              │
│    .uri(...)              sets target URL                        │
│    .retrieve()            prepares response handler              │
│    .toBodilessEntity()    fires call, only gets status code      │
│                                                                  │
│  returns: ResponseEntity<Void>                                   │
│  (you can then call .getStatusCode() to check result)            │
└─────────────────────────────────────────────────────────────┘
```

```java
RestClient restClient = RestClient.create();

ResponseEntity<Void> response = restClient
        .delete()
        .uri("http://localhost:8082/products/" + id)
        .retrieve()
        .toBodilessEntity();

HttpStatusCode deletionStatus = response.getStatusCode();
```

---

### Quick Comparison — GET vs POST vs DELETE

```
┌──────────────┬──────────────┬──────────────────┬──────────────────────┐
│               │     GET       │      POST          │       DELETE           │
├──────────────┼──────────────┼──────────────────┼──────────────────────┤
│ HTTP Method   │ .get()        │ .post()            │ .delete()              │
│ URI           │ .uri(...)     │ .uri(...)          │ .uri(...)              │
│ Headers       │ optional      │ optional           │ not needed usually     │
│ Request Body  │ ✗ none        │ ✓ .body(object)    │ ✗ none                 │
│ Retrieve      │ .retrieve()   │ .retrieve()        │ .retrieve()            │
│ Response      │ .body(Class)  │ .toEntity(Class)   │ .toBodilessEntity()    │
│ Return Type   │ String /      │ ResponseEntity     │ ResponseEntity<Void>   │
│               │ Object        │ <YourEntity>       │                        │
└──────────────┴──────────────┴──────────────────┴──────────────────────┘
```

---

### Exception Handling

Now, what happens when the server returns a 4xx or 5xx? By default, RestClient will throw a generic exception. You want to handle this properly — map it to your own custom exceptions.

There are **two ways** to do exception handling in RestClient:

---

### Way 1 — Using `.onStatus()` after `.retrieve()`

This is the standard approach. After calling `.retrieve()` you get a `ResponseSpec` object back. On that object, you can call `.onStatus()` to define what to do for different HTTP status codes — before you call `.body()`.

```
┌────────────────────────────────────────────────────────────┐
│              Exception Handling via .onStatus()                  │
│                                                                  │
│  .retrieve()                                                     │
│       │                                                          │
│       ▼  returns ResponseSpec                                    │
│  .onStatus(response -> {                                         │
│       if (4xx) → throw MyCustomException                         │
│       if (5xx) → throw NullPointerException                      │
│       else     → return false  (no error, proceed)               │
│  })                                                              │
│       │                                                          │
│       ▼  returns same ResponseSpec                               │
│  .body(String.class)   ← mapping happens here                    │
│                                                                  │
│  NOTE: .onStatus() sets up the handler — the actual              │
│  check runs only when the HTTP response arrives                  │
└─────────────────────────────────────────────────────────────┘
```

```java
@GetMapping("/{id}")
public ResponseEntity<String> getOrder(@PathVariable String id) {

    RestClient restClient = RestClient.create();

    String responseObj = restClient
            .get()
            .uri("http://localhost:8082/products/" + id)
            .retrieve()
            .onStatus(response -> {

                if (response.getStatusCode().is4xxClientError()) {
                    throw new MyCustomException("Invalid request passed");

                } else if (response.getStatusCode().is5xxServerError()) {
                    throw new NullPointerException("Something wrong at server");
                }

                return false; // no error, continue normally
            })
            .body(String.class);

    System.out.println(
        "Response from Product API called from order service: "
        + responseObj
    );

    return ResponseEntity.ok("order call successful");
}
```

---

### Way 2 — Using `.exchange()` directly (Full Control)

If you want complete control over both response mapping AND exception handling in one place — skip `.retrieve()` entirely and call `.exchange()` directly after `.uri()`.

```
┌─────────────────────────────────────────────────────────────┐
│           Exception Handling via .exchange()                     │
│                                                                  │
│  .get()                                                          │
│  .uri(...)                                                       │
│  .exchange((request, response) -> {                              │
│                                                                  │
│       if (4xx) → throw MyCustomException                         │
│       if (5xx) → throw InternalError                             │
│       else     → read body manually & return mapped object       │
│                                                                  │
│  })                                                              │
│                                                                  │
│  No .retrieve() needed.                                          │
│  No .onStatus() needed.                                          │
│  You handle EVERYTHING inside the lambda.                        │
└─────────────────────────────────────────────────────────────┘
```

```java
@GetMapping("/{id}")
public ResponseEntity<String> getOrder(@PathVariable String id) {

    RestClient restClient = RestClient.create();

    String responseObj = restClient
            .get()
            .uri("http://localhost:8082/products/" + id)
            .exchange((request, response) -> {

                if (response.getStatusCode().is4xxClientError()) {
                    throw new MyCustomException("Invalid request passed");

                } else if (response.getStatusCode().is5xxServerError()) {
                    throw new InternalError("Something wrong at server");

                } else {
                    // manually map the response body to String
                    return StreamUtils.copyToString(
                        response.getBody(),
                        StandardCharsets.UTF_8
                    );
                }
            });

    System.out.println(
        "Response from Product API called from order service: "
        + responseObj
    );

    return ResponseEntity.ok("order call successful");
}
```

---

### .onStatus() vs .exchange() — When to Use Which?

```
┌───────────────────────┬──────────────────────────────────────────┐
│     .onStatus()         │           .exchange()                        │
├───────────────────────┼──────────────────────────────────────────┤
│ Cleaner, less code      │ More verbose, more control                   │
│                         │                                              │
│ RestClient handles      │ YOU handle response mapping                  │
│ response mapping        │ manually (StreamUtils etc.)                  │
│                         │                                              │
│ Use when: standard      │ Use when: you need full control              │
│ response mapping is     │ over both mapping AND errors                 │
│ enough                  │ in one single place                          │
│                         │                                              │
│ Uses ResponseSpec       │ Skips ResponseSpec entirely                  │
│ internally              │                                              │
└───────────────────────┴──────────────────────────────────────────┘
```

---

That's Part 3 done — full GET, POST, DELETE code + both styles of exception handling.

# 🎯Part 4 — How the HTTP Call Actually Happens Internally + Interceptors

---

### What Actually Happens When You Call `.body()` or `.toEntity()`?

Till now we've been writing the chain and getting a response — but what actually fires the HTTP call? The instructor goes into the internals here. This is important because it also explains WHERE interceptors plug in.

Everything internally goes through a method called **`exchangeInternal()`**. Whether you use `.body()`, `.toEntity()`, `.toBodilessEntity()`, or directly call `.exchange()` — they all eventually land here.

---

### The 3-Step Internal Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│              exchangeInternal() — 3 Steps                                 │
│                                                                           │
│                                                                           │
│  STEP 1: createRequest()                                                  │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │  ClientHttpRequestFactory                                           │  │
│  │           │                                                         │  │
│  │           ▼  (default implementation)                               │  │
│  │  JdkClientHttpRequestFactory                                        │  │
│  │           │                                                         │  │
│  │           ▼                                                         │  │
│  │  Creates HttpClient object  (java.net.http.HttpClient)              │  │
│  │           │                                                         │  │
│  │           ▼                                                         │  │
│  │  Creates JdkClientHttpRequest                                       │  │
│  │  (sets requestMethod, connectTimeout, ReadTimeout etc.)             │  │
│  │  HttpClient object is stored inside this request object             │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                           │                                               │
│                           ▼                                               │
│  STEP 2: clientRequest.execute()                                          │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │  JdkClientHttpRequest.execute()                                     │  │
│  │           │                                                         │  │
│  │           ▼                                                         │  │
│  │  Calls HttpClient.sendAsync()                                       │  │
│  │           │                                                         │  │
│  │           ▼                                                         │  │
│  │  HttpClient internally handles:                                     │  │
│  │    - Creating TCP connection                                        │  │
│  │    - Connection pooling                                             │  │
│  │    - Sending HTTP request                                           │  │
│  │    - Supports both HTTP/1.1 and HTTP/2   ← important!               │  │
│  │    - Getting response                                               │  │
│  │           │                                                         │  │
│  │           ▼                                                         │  │
│  │  Even though sendAsync() is used internally,                        │  │
│  │  RestClient WAITS for the response (blocking)                       │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                           │                                               │
│                           ▼                                               │
│  STEP 3: exchangeFunction.exchange()                                      │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │  Once response arrives, the exchange function runs                  │  │
│  │                                                                     │  │
│  │  This is the lambda YOU provided (or ResponseSpec provides)         │  │
│  │  It handles:                                                        │  │
│  │    - Status code checking (4xx / 5xx)                               │  │
│  │    - Exception throwing if needed                                   │  │
│  │    - Response body mapping to your target class                     │  │
│  └───────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

---

### Important: RestClient's HttpClient vs RestTemplate's HttpClient

```
┌────────────────────────────────┬────────────────────────────────────┐
│        RestTemplate               │           RestClient                  │
├────────────────────────────────┼────────────────────────────────────┤
│ Uses older HTTP connection        │ Uses java.net.http.HttpClient         │
│ handling                          │ (introduced in Java 11+)              │
│                                   │                                       │
│ Supports HTTP/1.0 and HTTP/1.1    │ Supports HTTP/1.1 AND HTTP/2          │
│ only                              │                                       │
│                                   │ HTTP/2 benefit:                       │
│ Request → wait → Response         │ Multiple concurrent requests and      │
│ Request → wait → Response         │ responses over same connection        │
│ (strictly sequential)             │ (much more efficient)                 │
└────────────────────────────────┴────────────────────────────────────┘
```

So RestClient is not just a cleaner API — it also uses a more powerful HTTP engine underneath.

---

### Adding Interceptors

An interceptor lets you **intercept every outgoing request** and modify it before it gets sent — for example, adding an auth token, a tracing header, or logging every request.

The instructor shows this kicks in at **Step 2** of the internal flow — just before `execute()` fires.

```
┌─────────────────────────────────────────────────────────────────────┐
│                  Where Interceptor Plugs In                               │
│                                                                           │
│  STEP 1: createRequest()         ← interceptor NOT here yet               │
│                │                                                          │
│                ▼                                                          │
│  STEP 2: clientRequest.execute() ← interceptor fires HERE                 │
│                │                                                          │
│          ┌─────┴──────────────────────────────────┐                   │
│          │  InterceptingClientHttpRequest wraps       │                   │
│          │  the execute call.                         │                   │
│          │                                            │                   │
│          │  Your interceptor runs first:              │                   │
│          │   → inspect/modify request headers         │                   │
│          │   → add auth tokens, tracing IDs etc.      │                   │
│          │   → call execution.execute() to proceed    │                   │
│          └─────────────────────────────────────────┘                  │
│                │                                                          │
│                ▼                                                          │
│  STEP 3: exchangeFunction.exchange()  ← response handling                 │
└─────────────────────────────────────────────────────────────────────┘
```

---

### Interceptor Implementation — 3 Parts

**Part 1 — Create your custom interceptor class:**

```java
public class MyCustomRequestInterceptor
        implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {

        // intercept the request and add a custom header
        request.getHeaders().add("X-Custom-Header", "myvalue");

        // MUST call this to let the request proceed
        return execution.execute(request, body);
    }
}
```

```
┌─────────────────────────────────────────────────────────────────┐
│             What Each Parameter Means                                 │
├──────────────────────┬──────────────────────────────────────────┤
│ HttpRequest request    │ The outgoing HTTP request object             │
│                        │ You can read/modify headers, URI etc.        │
├──────────────────────┼──────────────────────────────────────────┤
│ byte[] body            │ The request body (for POST/PUT)              │
├──────────────────────┼──────────────────────────────────────────┤
│ ClientHttpRequest      │ Call execution.execute() to actually         │
│ Execution execution    │ proceed with the request after your          │
│                        │ interception logic is done                   │
└──────────────────────┴──────────────────────────────────────────┘
```

---

**Part 2 — Register interceptor while building RestClient (AppConfig):**

```java
@Configuration
public class AppConfig {

    @Bean
    public RestClient restClientInstance(
            ClientHttpRequestInterceptor myCustomInterceptor
    ) {
        return RestClient.builder()
                .requestInterceptor(myCustomInterceptor) // plug in here
                .build();
    }

    @Bean
    public ClientHttpRequestInterceptor customRequestInterceptor() {
        return new MyCustomRequestInterceptor();
    }
}
```

Notice the difference from before:

```
┌──────────────────────────────────────────────────────────────┐
│   Without interceptor:   RestClient.create()                       │
│   With interceptor:      RestClient.builder()                      │
│                                    .requestInterceptor(...)        │
│                                    .build()                        │
│                                                                    │
│   .create() is just a shortcut for .builder().build()              │
│   When you need to configure anything (interceptors,               │
│   base URL, timeouts etc.) — use .builder() explicitly             │
└──────────────────────────────────────────────────────────────┘
```

---

**Part 3 — OrderController stays exactly the same:**

```java
@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    RestClient restClient; // already has interceptor baked in

    @GetMapping("/{id}")
    public ResponseEntity<String> getOrder(@PathVariable String id) {

        ResponseEntity<String> responseEntityObj = restClient
                .get()
                .uri("http://localhost:8082/products/" + id)
                .retrieve()
                .toEntity(String.class);

        System.out.println(
            "Response from Product API called from order service: "
            + responseEntityObj.getBody()
        );

        return ResponseEntity.ok("order call successful");
    }
}
```

The interceptor is invisible to the controller code — it automatically fires on every request made through this RestClient bean.

---

### Proof It Works — What ProductService Sees

When ProductService receives the request, its headers show the injected header:

```
┌──────────────────────────────────────────────────────────────┐
│         Headers received by ProductService                        │
├──────────────────────────────────────────────────────────────┤
│  connection       → Upgrade, HTTP2-Settings                       │
│  content-length   → 0                                             │
│  host             → localhost:8082                                │
│  http2-settings   → (negotiation value)                           │
│  upgrade          → h2c   ← HTTP/2 upgrade happening!             │
│  user-agent       → Java-http-client/17.0.12                      │
│  x-custom-header  → myvalue  ← our interceptor added this         │
└──────────────────────────────────────────────────────────────┘
```

Two things to notice here — the custom header is present, AND you can see `upgrade: h2c` which confirms RestClient is attempting HTTP/2 negotiation automatically.

---

### Full Picture — Everything Together

```
┌─────────────────────────────────────────────────────────────────────┐
│                    RestClient Complete Flow                               │
│                                                                           │
│  1. Create RestClient bean                                                │
│     RestClient.builder().requestInterceptor(...).build()                  │
│                    │                                                      │
│  2. Write the chain in your controller                                    │
│     restClient.get().uri(...).retrieve().toEntity(...)                    │
│                    │                                                      │
│  3. Chain builds up the request object                                    │
│     (no HTTP call yet — just filling in fields)                           │
│                    │                                                      │
│  4. .toEntity() / .body() triggers exchangeInternal()                     │
│                    │                                                      │
│            ┌───────┴────────┐                                            │
│            ▼                 ▼                                            │
│       STEP 1            STEP 2                      STEP 3                │
│   createRequest()    execute()               exchangeFunction             │
│   JdkClientHttp  →  Interceptor fires   →   Response mapping &            │
│   RequestFactory    THEN HttpClient         exception handling            │
│   builds request    sendAsync() fires                                     │
│                     TCP conn + HTTP call                                  │
│                     waits for response                                    │
└─────────────────────────────────────────────────────────────────────┘
```

---

That's Part 4 and the full lecture done!

### Quick Revision — Everything in One Table

```
┌──────────────────────┬────────────────────────────────────────────┐
│      Concept           │               Key Point                        │
├──────────────────────┼────────────────────────────────────────────┤
│ Why RestClient         │ RestTemplate has too many overloaded           │
│                        │ methods, in maintenance mode                   │
├──────────────────────┼────────────────────────────────────────────┤
│ Fluent API             │ Method chaining — each method returns          │
│                        │ object exposing next set of operations         │
├──────────────────────┼────────────────────────────────────────────┤
│ Sequence matters       │ Breaking the chain = compile error.            │
│                        │ uri() must come before accept()                │
├──────────────────────┼────────────────────────────────────────────┤
│ retrieve()             │ Returns ResponseSpec. No HTTP call yet.        │
├──────────────────────┼────────────────────────────────────────────┤
│ body() / toEntity()    │ THIS triggers the actual HTTP call             │
├──────────────────────┼────────────────────────────────────────────┤
│ Exception handling     │ .onStatus() for standard approach              │
│                        │ .exchange() for full manual control            │
├──────────────────────┼────────────────────────────────────────────┤
│ HTTP/2 support         │ RestClient uses java.net.http.HttpClient       │
│                        │ RestTemplate only supports HTTP/1.1            │
├──────────────────────┼────────────────────────────────────────────┤
│ Interceptors           │ Implement ClientHttpRequestInterceptor         │
│                        │ Register via .builder().requestInterceptor     │
│                        │ Fires at Step 2 (just before execute())        │
└──────────────────────┴────────────────────────────────────────────┘
```

That's the complete RestClient lecture — notes done! Let me know when you're ready for the next topic.