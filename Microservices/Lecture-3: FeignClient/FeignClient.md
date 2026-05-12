# Step 1 — What is FeignClient? Why does it exist?

---

## The Problem First

When building microservices, services need to talk to each other. You've already seen two ways to do this:

- **RestTemplate** — older approach, verbose, you manually build URLs, handle responses, etc.
- **RestClient** — newer, fluent API, cleaner than RestTemplate, but you still have to *write the logic* of how to make the HTTP call.

Both of these require you to think about **how** to make the HTTP call — building the URL, setting headers, handling the response stream, serialization, etc.

**FeignClient solves this** by letting you only say ***what* you want to call, not *how* to call it.**

This idea is called **Declarative HTTP Communication.**

---

## What Exactly is FeignClient?

> "Feign is a **Declarative HTTP Client** developed by **Netflix**."

**Declarative** means:

| You say... | Framework handles... |
|---|---|
| "Call `/products/{id}` on product-service" | How to build the HTTP request |
| "This is a GET call" | How to serialize/deserialize |
| "Base URL is `localhost:8082`" | How to retry on failure |
| Nothing else | How to handle errors |

You write an **interface**. No implementation. The framework figures out the rest.

---

## Where Does FeignClient Live in Spring?

FeignClient was originally built by Netflix. In Spring Boot, this capability is available through the **Spring Cloud OpenFeign** library.

```
Netflix Feign  →  adopted by  →  Spring Cloud OpenFeign
```

And Spring Cloud is not just one thing. It's a **collection of tools and libraries** that help you build distributed microservices:

```
┌─────────────────────────────────────────────────────┐
│                   SPRING CLOUD                      │
│                                                     │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────┐   │
│  │   OpenFeign │  │   Eureka     │  │  Gateway  │   │
│  │(HTTP Client)│  │  (Service    │  │   (API    │   │
│  │             │  │  Discovery)  │  │  Gateway) │   │
│  └─────────────┘  └──────────────┘  └───────────┘   │
│                                                     │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────┐   │
│  │ Resilience4j│  │   Sleuth /   │  │  Config   │   │
│  │  (Circuit   │  │    Zipkin    │  │  Server   │   │
│  │  Breaker)   │  │  (Tracing)   │  │           │   │
│  └─────────────┘  └──────────────┘  └───────────┘   │
└─────────────────────────────────────────────────────┘
```

All these libraries are **designed to work seamlessly with each other.** This is a key reason why the instructor says:

> *"In future, if we have to do microservice communication, most probably we are going to use FeignClient only."*

Because once you're using Spring Cloud, FeignClient integrates **naturally** with service discovery (Eureka), load balancing, circuit breakers, etc. — without any extra wiring effort on your part.

---

## Real World Picture — What We're Building

The example throughout this lecture is simple and concrete:

```
┌─────────────────────────┐         HTTP Call          ┌──────────────────────────┐
│      ORDER SERVICE      │  ─────────────────────►    │     PRODUCT SERVICE      │
│      port: 8081         │                            │      port: 8082          │
│                         │   GET /products/{id}       │                          │
│  Wants product details  │ ◄───────────────────────   │  Returns product details │
└─────────────────────────┘                            └──────────────────────────┘
```

**Order Service** needs to call **Product Service** to fetch product details. This inter-service communication is what FeignClient will handle — cleanly and declaratively.

---

## Quick Comparison — So You Appreciate FeignClient

```
┌─────────────────┬──────────────────────────────────┬──────────────────────────┐
│                 │     RestTemplate / RestClient    │       FeignClient        │
├─────────────────┼──────────────────────────────────┼──────────────────────────┤
│ Style           │ Imperative (you write HOW)       │ Declarative (say WHAT)   │
│ Implementation  │ You write the HTTP logic         │ Framework handles it     │
│ Code amount     │ More                             │ Much less                │
│ Spring Cloud    │ Manual integration               │ Seamless integration     │
│ integration     │                                  │                          │
│ Load balancing  │ Manual setup needed              │ Built-in with Ribbon/LB  │
│ Service         │ You hardcode URLs                │ Works with Eureka        │
│ Discovery       │                                  │ auto-discovery           │
└─────────────────┴──────────────────────────────────┴──────────────────────────┘
```

---

## Key Takeaway from Step 1

- FeignClient = **Declarative HTTP Client** from Netflix, available in Spring via **Spring Cloud OpenFeign**
- You only tell **what** to call — the framework handles **how**
- It's part of Spring Cloud, which means it plays well with **all other microservice tools** (discovery, load balancing, circuit breaker, etc.)
- This is why it's the **preferred choice** for microservice communication going forward

---
# Step 2 — Setting Up FeignClient (pom.xml + Dependency Management)

---

## What Do You Need to Add?

To use FeignClient in your Spring Boot project, you need to add the **Spring Cloud OpenFeign** dependency in your `pom.xml`.

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

Notice something? **No version is specified here.** This is intentional. Let's understand why.

---

## Why No Version? — The Dependency Management Problem

Imagine your Order Service needs multiple Spring Cloud libraries in the future:

```
┌─────────────────────────────────────────────────────────────┐
│              ORDER SERVICE - pom.xml (future)               │
│                                                             │
│   spring-cloud-starter-openfeign      → version ?          │
│   spring-cloud-starter-netflix-eureka → version ?          │
│   spring-cloud-starter-loadbalancer   → version ?          │
│   spring-cloud-starter-gateway        → version ?          │
└─────────────────────────────────────────────────────────────┘
```

All of these are **Spring Cloud libraries**. And here's the catch:

> All Spring Cloud libraries must have **compatible versions** with each other. If they don't match, your application will break in unexpected ways — and these bugs are very hard to trace.

If you manually specify versions like this:

```xml
<!-- DON'T do this manually -->
<dependency>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
    <version>4.1.0</version>   <!-- is this compatible with eureka below? -->
</dependency>

<dependency>
    <artifactId>spring-cloud-starter-netflix-eureka</artifactId>
    <version>4.0.3</version>   <!-- you have to verify this yourself -->
</dependency>
```

You're now **manually responsible** for making sure every Spring Cloud library version is compatible with every other one. This is a maintenance headache — especially as you keep adding more libraries.

---

## The Solution — Spring Cloud Dependency Management (BOM)

Instead, you add a **BOM (Bill of Materials)** inside `<dependencyManagement>`:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2023.0.1</version> <!-- one single version to manage all -->
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Now whenever you add **any** Spring Cloud library, you just skip the version:

```xml
<!-- No version needed — BOM handles it -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

---

## How This Works — The Full Picture

```
┌──────────────────────────────────────────────────────────────────┐
│                     pom.xml                                      │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  <dependencyManagement>                                    │  │
│  │      spring-cloud-dependencies  version: 2023.0.1  ◄───┐   │  │
│  │  </dependencyManagement>                               │   │  │
│  └────────────────────────────────────────────────────────│───┘  │
│                                                           │      │
│  ┌──────────────────────────────────────────────────────┐ │      │
│  │  <dependencies>                                      │ │      │
│  │                                                      │ │      │
│  │   openfeign        (no version) ──────────picks──────┘ │      │
│  │   eureka-client    (no version) ──────────picks───────►│      │
│  │   loadbalancer     (no version) ──────────picks────────│      │
│  │                                                      │ │      │
│  │   Maven auto-resolves compatible versions for all   ◄──┘      │
│  └──────────────────────────────────────────────────────┘        │
└──────────────────────────────────────────────────────────────────┘
```

Maven reads the BOM, finds the pre-tested compatible versions for each Spring Cloud library, and pulls them in automatically.

> **Instructor's words:** *"If you don't want the headache of managing versions manually, leave it up to Spring Boot itself. Maven will take care that all dependencies it brings in are compatible."*

---

## Complete pom.xml Setup (What You Actually Write)

```xml
<!-- STEP 1: Add the BOM in dependencyManagement -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2023.0.1</version> <!-- Use latest compatible version -->
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- STEP 2: Add OpenFeign dependency — NO version needed -->
<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-openfeign</artifactId>
    </dependency>
</dependencies>
```

---

## Key Takeaways from Step 2

```
┌────────────────────────────────────────────────────────┐
│                   REMEMBER THIS                        │
│                                                        │
│  1. Add spring-cloud-starter-openfeign dependency      │
│                                                        │
│  2. Do NOT specify version for it                      │
│                                                        │
│  3. Instead, add spring-cloud-dependencies BOM         │
│     inside <dependencyManagement>                      │
│                                                        │
│  4. BOM ensures ALL Spring Cloud libs are              │
│     version-compatible automatically                   │
│                                                        │
│  5. In future, when you add Eureka, LoadBalancer etc.  │
│     → still no version needed → BOM handles it         │
└────────────────────────────────────────────────────────┘
```

---

# Step 3 — Core Implementation: The 4 Things You Need to Make FeignClient Work

---

The instructor is very clear about this. To make FeignClient work, you need exactly **4 things**. Let's go through each one carefully.

---

## The Big Picture First

```
┌─────────────────────────────────────────────────────────────────────┐
│                    ORDER SERVICE (port: 8081)                       │
│                                                                     │
│  ┌─────────────────┐      ┌──────────────────┐                      │
│  │  OrderController│─────►│  ProductClient   │                      │
│  │                 │      │  (interface only) │                     │
│  │  @Autowired     │      │  @FeignClient     │                     │
│  │  ProductClient  │      │                  │                      │
│  └─────────────────┘      └────────┬─────────┘                      │
│                                    │ base URL from                  │
│  ┌─────────────────┐               │ application.properties         │
│  │@SpringBootApp   │               │                                │
│  │@EnableFeign     │               ▼                                │
│  │Clients          │    http://localhost:8082                       │
│  └─────────────────┘                                                │
└──────────────────────────────────────┬──────────────────────────────┘
                                       │  HTTP GET /products/{id}
                                       ▼
┌─────────────────────────────────────────────────────────────────────┐
│                   PRODUCT SERVICE (port: 8082)                      │
│                                                                     │
│   GET /products/{id}  →  returns product details                    │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Thing 1 — Create the FeignClient Interface

Inside your **Order Service**, create an interface and annotate it with `@FeignClient`.

```java
@FeignClient(
    name = "product-service",               // arbitrary name — you can give anything
    url = "${feign.client.product-service.url}"  // base URL — read from application.properties
)
public interface ProductClient {

    @GetMapping("/products/{id}")           // relative path of the endpoint you want to call
    String getProductById(@PathVariable("id") String id);
}
```

Let's break this down carefully:

```
┌───────────────────────────────────────────────────────────────────┐
│                   @FeignClient Annotation                         │
│                                                                   │
│   name = "product-service"                                        │
│         → Just an arbitrary label for this FeignClient            │
│         → Used later in application.properties for config         │
│                                                                   │
│   url = "${feign.client.product-service.url}"                     │
│         → This is the BASE URL  →  http://localhost:8082          │
│         → Can hardcode, but better to read from properties        │
│                                                                   │
│   @GetMapping("/products/{id}")                                   │
│         → This is the RELATIVE PATH of the endpoint to call       │
│         → Final URL = base URL + relative path                    │
│         → http://localhost:8082  +  /products/{id}                │
│                                                                   │
│   @PathVariable("id") String id                                   │
│         → The {id} in the path gets replaced by this value        │
└───────────────────────────────────────────────────────────────────┘
```

> **Very important point from instructor:** This interface has **NO implementation**. You're just declaring *what* endpoint you want to call and *what* HTTP method it is — exactly like how you define endpoints in a Controller. The framework provides the implementation internally.

---

**Direct mapping between ProductClient interface and ProductController:**

```
ORDER SERVICE                              PRODUCT SERVICE
─────────────────────────────────────────────────────────────────
ProductClient (interface)                  ProductController

@FeignClient(url="localhost:8082")
public interface ProductClient {           @RestController
                                           @RequestMapping("/products")
  @GetMapping("/products/{id}")     ←───► public class ProductController {
  String getProductById(                     @GetMapping("/{id}")
    @PathVariable("id") String id);          public String getProduct(
}                                              @PathVariable String id) {
                                               return "Product fetched: " + id;
                                             }
                                           }
```

The method in the interface **mirrors** the endpoint in the controller of the other service.

---

## Thing 2 — application.properties (Base URL Configuration)

```properties
server.port=8081

# Base URL for Product Service
feign.client.product-service.url=http://localhost:8082
```

You could also hardcode the URL directly in the `@FeignClient` annotation:

```java
// Option A — hardcoded (not recommended)
@FeignClient(name = "product-service", url = "http://localhost:8082")

// Option B — read from properties (recommended)
@FeignClient(name = "product-service", url = "${feign.client.product-service.url}")
```

> **Why properties file is better:** If your Product Service URL changes (different environment, different port), you only change it in one place — the properties file. No code change needed.

---

## Thing 3 — Autowire and Use the Client in Controller

Now in your **OrderController**, you simply autowire the `ProductClient` interface and call its method — just like calling any regular Java method.

```java
@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    ProductClient productClient;      // autowiring the interface — no impl written by us

    @GetMapping("/{id}")
    public ResponseEntity<String> getOrder(@PathVariable String id) {

        // Just call the method — FeignClient handles the HTTP call internally
        String responseFromProductAPI = productClient.getProductById(id);

        System.out.println(
            "Response from Product api call is: " + responseFromProductAPI
        );

        return ResponseEntity.ok("order call successful");
    }
}
```

> **Question that comes to mind:** *"We never wrote any implementation for ProductClient interface — so how is @Autowired working? Where is the object coming from?"*
>
> This is exactly what the instructor addresses in Step 4 (Internal Working). For now just know — the **FeignClient framework creates the implementation for you at runtime.**

---

## Thing 4 — Enable FeignClients in Main Application Class

This is small but **very important.** Without this, Spring Boot will never scan for `@FeignClient` interfaces.

```java
@SpringBootApplication
@EnableFeignClients                    // ← THIS IS MANDATORY
public class OrderserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderserviceApplication.class, args);
    }
}
```

> **What `@EnableFeignClients` does:** It tells Spring Boot — *"Hey, go scan for all interfaces annotated with `@FeignClient` and create their implementations."* Without this annotation, Spring Boot simply ignores all your `@FeignClient` interfaces completely.

---

## All 4 Things Together — The Complete Flow

```
┌──────────────────────────────────────────────────────────────────────┐
│                    THE 4 THINGS — SUMMARY                            │
│                                                                      │
│  ┌─────┐                                                             │
│  │  1  │  Create interface annotated with @FeignClient               │
│  │     │  → Define which endpoints to call (like a controller)       │
│  │     │  → No implementation needed                                 │
│  └─────┘                                                             │
│                                                                      │
│  ┌─────┐                                                             │
│  │  2  │  application.properties                                     │
│  │     │  → Define base URL of the target service                    │
│  │     │  → Better than hardcoding                                   │
│  └─────┘                                                             │
│                                                                      │
│  ┌─────┐                                                             │
│  │  3  │  Autowire ProductClient in Controller/Service               │
│  │     │  → Call the interface method directly                       │
│  │     │  → Response comes back automatically                        │
│  └─────┘                                                             │
│                                                                      │
│  ┌─────┐                                                             │
│  │  4  │  Add @EnableFeignClients on main application class          │
│  │     │  → Tells Spring Boot to scan for @FeignClient interfaces    │
│  │     │  → Without this — nothing works                             │
│  └─────┘                                                             │
└──────────────────────────────────────────────────────────────────────┘
```

---

## What Happens When You Run and Test

When you start both services and hit the Order endpoint:

```
GET http://localhost:8081/orders/1
```

Internally this happens:

```
1. Request hits OrderController.getOrder("1")
        │
        ▼
2. productClient.getProductById("1") is called
        │
        ▼
3. FeignClient framework builds HTTP request:
   GET http://localhost:8082/products/1
        │
        ▼
4. Product Service receives request
   Returns: "fetch the product details with id: 1"
        │
        ▼
5. Response comes back to OrderController
   Prints: "Response from Product api call is: fetch the product details with id: 1"
        │
        ▼
6. OrderController returns: "order call successful"
```

---

## Key Takeaways from Step 3

- The `@FeignClient` interface is **written exactly like a Controller** — same annotations (`@GetMapping`, `@PathVariable`, etc.) but on the *caller side*, not the *receiver side*
- The `name` in `@FeignClient` is just a label — but it becomes **useful later** for per-client configuration in properties (we'll see this in Step 9)
- **`@EnableFeignClients` is not optional** — without it, nothing works
- You write **zero HTTP logic** — no URL building, no connection handling, no response parsing

---
# Step 4 — How Does FeignClient Work Internally? (The Declarative Magic Explained)

---

This is the most important conceptual step. The instructor spends a lot of time on this because it answers the **one question everyone has:**

> *"We never wrote any implementation for the `ProductClient` interface. We never created any object. So how is `@Autowired` working? Who created the object? Who is making the actual HTTP call?"*

The answer lies in **3 steps** that happen internally when your application starts up.

---

## The Full Internal Flow — Overview First

```
APPLICATION STARTUP
        │
        ▼
┌───────────────────────────────────────────────────────────────────┐
│   @EnableFeignClients triggers a scan                             │
│   → Finds all interfaces annotated with @FeignClient              │
│   → For each interface, invokes ReflectiveFeign.newInstance()     │
└───────────────────────────────┬───────────────────────────────────┘
                                │
                ┌───────────────▼────────────────┐
                │                                │
         STEP 1 ▼                                │
  ┌──────────────────────┐                       │
  │  For each METHOD in  │                       │
  │  the interface,      │                       │
  │  create a            │                       │
  │  MethodHandler       │                       │
  │  object              │                       │
  └──────────┬───────────┘                       │
             │                                   │
         STEP 2 ▼                                │
  ┌──────────────────────┐                       │
  │  Create an           │                       │
  │  InvocationHandler   │                       │
  │  object — store the  │                       │
  │  map of              │                       │
  │  {method →           │                       │
  │   MethodHandler}     │                       │
  │  inside it           │                       │
  └──────────┬───────────┘                       │
             │                                   │
         STEP 3 ▼                                │
  ┌──────────────────────┐                       │
  │  Use Dynamic Proxy   │                       │
  │  to create a runtime │                       │
  │  implementation of   │◄──────────────────────┘
  │  your interface      │
  │  → This is what gets │
  │    injected by       │
  │    @Autowired        │
  └──────────────────────┘
```

Now let's go deep into each step.

---

## STEP 1 — MethodHandler: Parsing Every Method in Your Interface

When the application starts, for **each method** in your `@FeignClient` interface, the framework creates a **MethodHandler** object.

What is a MethodHandler? It holds **all the information needed to make an actual HTTP call** — derived by parsing the method's annotations.

```
Interface Method:
─────────────────────────────────────────────
@GetMapping("/products/{id}")
String getProductById(@PathVariable("id") String id);
─────────────────────────────────────────────
        │
        │  Framework parses this method
        │  and fills up a MethodHandler object
        ▼
┌─────────────────────────────────────────────────────┐
│                  MethodHandler                      │
│                                                     │
│  targetURL    = base URL + relative path            │
│               = "http://localhost:8082"             │
│                 + "/products/{id}"                  │
│               = "http://localhost:8082/products/1"  │
│                                                     │
│  httpMethod   = GET  ← derived from @GetMapping     │
│                                                     │
│  headerInfo   = (none here)  ← from @RequestHeader  │
│                                                     │
│  httpClient   = HttpURLConnection (default)         │
│               → can be changed to Apache HttpClient │
│                                                     │
│  encoder      = SpringEncoder (default, uses        │
│                 Jackson) — converts Java object     │
│                 to JSON for request body            │
│                                                     │
│  decoder      = SpringDecoder (default, uses        │
│                 Jackson) — converts JSON response   │
│                 back to Java object                 │
│                                                     │
│  errorDecoder = ErrorDecoder.Default — handles      │
│                 4xx and 5xx responses               │
│                                                     │
│  logger       = logs request and response info      │
│                                                     │
│  retryer      = Retryer.Default — retries on        │
│                 connection timeout / IOException    │
│                                                     │
│  invoke()     = the method that actually builds     │
│                 the HTTP request and executes it    │
└─────────────────────────────────────────────────────┘
```

> **Key insight from instructor:** *"MethodHandler is derived from the method — its annotations and signature. All these fields are very very important — they are everything needed to execute the actual HTTP request."*

If your interface has **2 methods**, a **map** is created:

```
Map<Method, MethodHandler>
─────────────────────────────────────────
getProductById    →   MethodHandler (GET /products/{id})
updateProduct     →   MethodHandler (PUT /products/update/{id})
```

---

## STEP 2 — InvocationHandler: The Bridge

Once the map of `{method → MethodHandler}` is ready, the framework creates an **InvocationHandler** object and stores that map inside it.

```
┌────────────────────────────────────────────────────┐
│               InvocationHandler                    │
│                                                    │
│  map = {                                           │
│    getProductById → MethodHandler,                 │
│    updateProduct  → MethodHandler                  │
│  }                                                 │
│                                                    │
│  invoke(Object proxy, Method method, Object[] args)│
│         └─────────────────────────────────────┐    │
│                                               │    │
│   1. Gets the method from the proxy call      │    │
│   2. Looks up its MethodHandler in the map    │    │
│   3. Calls methodHandler.invoke(args)         │    │
│      → This actually makes the HTTP call      │    │
└───────────────────────────────────────────────┴────┘
```

The `invoke()` method inside InvocationHandler acts as a **bridge** between the proxy (Step 3) and the MethodHandler.

---

## STEP 3 — Dynamic Proxy: The Runtime Implementation

This is where the magic happens. The framework uses **Java Dynamic Proxy** to create a **runtime implementation** of your interface.

> **Important note from instructor:** *"You will never find this class in the Spring Cloud OpenFeign library. This is just my understanding of what the dynamic proxy generates internally — just to help you visualize it."*

Here's what the dynamically generated class would conceptually look like:

```java
// This class is NEVER written by you
// This is what Dynamic Proxy generates at RUNTIME

public class FeignProductClientProxy implements ProductClient {

    private final InvocationHandler handler;

    public FeignProductClientProxy(InvocationHandler handler) {
        this.handler = handler;
    }

    @Override
    public String getProductById(String id) {

        // Step 1: Get the Method object for "getProductById"
        Method method = ProductClient.class
                            .getMethod("getProductById", String.class);

        // Step 2: Call InvocationHandler.invoke()
        //         → it looks up MethodHandler from the map
        //         → calls MethodHandler.invoke(args)
        //         → MethodHandler builds HTTP request and executes it
        return (String) handler.invoke(this, method, new Object[]{id});
    }
}
```

**This generated class is what gets injected when you write `@Autowired ProductClient`.**

---

## Putting It All Together — The Complete Runtime Flow

```
YOUR CODE (OrderController)
─────────────────────────────────────────────────────────────────────
productClient.getProductById("1");
        │
        │  productClient is actually FeignProductClientProxy
        ▼
DYNAMIC PROXY (generated at startup)
─────────────────────────────────────────────────────────────────────
FeignProductClientProxy.getProductById("1")
        │
        │  calls handler.invoke(this, method, ["1"])
        ▼
INVOCATION HANDLER
─────────────────────────────────────────────────────────────────────
invoke(proxy, getProductById method, ["1"])
        │
        │  looks up map → finds MethodHandler for getProductById
        │  calls methodHandler.invoke(["1"])
        ▼
METHOD HANDLER
─────────────────────────────────────────────────────────────────────
invoke(["1"])
        │
        │  builds HTTP request:
        │  → URL: http://localhost:8082/products/1
        │  → Method: GET
        │  → Headers, encoder, decoder all applied
        │
        │  executeAndDecode(request)
        │  → makes actual HTTP call to Product Service
        │  → gets response
        │  → decoder converts JSON → Java object
        ▼
RESPONSE
─────────────────────────────────────────────────────────────────────
"fetch the product details with id: 1"
        │
        ▼
back to OrderController
```

---

## The Complete Internal Picture — One Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        APPLICATION STARTUP                              │
│                                                                         │
│  @EnableFeignClients → scans → finds ProductClient (@FeignClient)       │
│                                                                         │
│  ReflectiveFeign.newInstance() is called                                │
│          │                                                              │
│          ├──── STEP 1 ────────────────────────────────────────────┐     │
│          │     For each method → create MethodHandler             │     │
│          │     MethodHandler holds:                               │     │
│          │     [targetURL, httpMethod, headers, encoder,          │     │
│          │      decoder, errorDecoder, retryer, logger,           │     │
│          │      httpClient, invoke()]                             │     │
│          │     Store in Map{method → MethodHandler}               │     │
│          │                                                        │     │
│          ├──── STEP 2 ─────────────────────────────────────────── │     │
│          │     Create InvocationHandler                           │     │
│          │     Store the Map inside it                            │     │
│          │     InvocationHandler.invoke() = bridge between        │     │
│          │     proxy and MethodHandler                            │     │
│          │                                                        │     │
│          └──── STEP 3 ─────────────────────────────────────────── ┘     │
│                Dynamic Proxy creates runtime implementation             │
│                of ProductClient interface                               │
│                This object is registered as a Spring Bean               │
│                → @Autowired injects THIS object                         │
└─────────────────────────────────────────────────────────────────────────┘

AT RUNTIME (when you call productClient.getProductById("1"))
┌─────────────────────────────────────────────────────────────────────────┐
│                                                                         │
│  OrderController                                                        │
│       │  productClient.getProductById("1")                              │
│       ▼                                                                 │
│  Dynamic Proxy (FeignProductClientProxy)                                │
│       │  handler.invoke(this, method, args)                             │
│       ▼                                                                 │
│  InvocationHandler                                                      │
│       │  map.get(method) → MethodHandler                                │
│       │  methodHandler.invoke(args)                                     │
│       ▼                                                                 │
│  MethodHandler                                                          │
│       │  builds request → executeAndDecode()                            │
│       │  → encoder encodes request body (if any)                        │
│       │  → HTTP call made to Product Service                            │
│       │  → decoder decodes response                                     │
│       ▼                                                                 │
│  Response returned back up the chain to OrderController                 │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Key Takeaways from Step 4

```
┌────────────────────────────────────────────────────────────┐
│                    REMEMBER THIS                           │
│                                                            │
│  Q: Who provides the implementation of ProductClient?      │
│  A: Dynamic Proxy — at runtime, during app startup         │
│                                                            │
│  Q: How does @Autowired work on an interface?              │
│  A: The proxy-generated class is registered as a           │
│     Spring Bean — that's what gets injected                │
│                                                            │
│  Q: Who actually makes the HTTP call?                      │
│  A: MethodHandler.invoke() → executeAndDecode()            │
│                                                            │
│  Q: What is InvocationHandler?                             │
│  A: The bridge between Dynamic Proxy and MethodHandler     │
│                                                            │
│  Q: What does MethodHandler hold?                          │
│  A: Everything needed for the HTTP call —                  │
│     URL, method, headers, encoder, decoder,                │
│     errorDecoder, retryer, logger                          │
│                                                            │
│  All these fields are CONFIGURABLE — you can provide       │
│  your own custom implementation for any of them            │
└────────────────────────────────────────────────────────────┘
```

---
# Step 5 — Advanced Annotation Usage in FeignClient Interface

---

## What This Step Is About

In Step 3, you saw a simple example — just a `@GetMapping` with a `@PathVariable`. But real-world APIs are more complex. They have:

- Path variables (`/products/{id}`)
- Query parameters (`?sendMail=true`)
- Custom headers (`X-ConceptCoding-ID: abc123`)
- Request bodies (JSON payload)

The instructor shows that FeignClient supports **all of these** — using the **exact same annotations** you already know from writing Controllers.

---

## The Real-World Scenario

```
┌──────────────────────────────────────────────────────────────────────┐
│                     ORDER SERVICE                                    │
│                                                                      │
│  Wants to UPDATE a product in Product Service                        │
│                                                                      │
│  → Path Variable  : product ID in the URL                            │
│  → Request Param  : sendMail=true (query param)                      │
│  → Request Header : X-ConceptCoding-ID (custom header)               │
│  → Request Body   : updated product details (JSON)                   │
└──────────────────────────────────────────────────────────────────────┘
                              │
                              │  PUT /products/update/{id}?sendMail=true
                              │  Header: X-ConceptCoding-ID: abc123
                              │  Body: { "name": "updated name" }
                              ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    PRODUCT SERVICE                                   │
│                                                                      │
│   PUT /products/update/{id}                                          │
│   → reads path variable, body, param, header                         │
│   → updates product → returns updated product                        │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Order Service — ProductClient Interface (Caller Side)

```java
@FeignClient(
    name = "product-service",
    url = "${feign.client.product-service.url}"
)
public interface ProductClient {

    // Simple GET — from previous step
    @GetMapping("/products/{id}")
    String getProductById(@PathVariable("id") String id);


    // Complex PUT — with all annotation types
    @PutMapping(
        value = "/products/update/{id}",
        consumes = "application/json"       // tells FeignClient: request body is JSON
    )
    Product updateProduct(
        /*
          Ordering is NOT mandatory.
          Spring Boot will assign values
          based on annotations, not position.
        */
        @PathVariable("id") String id,                          // → goes into URL path
        @RequestParam("sendMail") boolean sendMail,             // → goes as ?sendMail=true
        @RequestHeader("X-ConceptCoding-ID") String uniqueID,  // → goes as request header
        @RequestBody Product updatedProductDetails              // → goes as JSON body
    );
}
```

---

## Product Service — ProductController (Receiver Side)

```java
@RestController
@RequestMapping("/products")
public class ProductController {

    // Simple GET endpoint
    @GetMapping("/{id}")
    public ResponseEntity<String> getProduct(@PathVariable String id) {
        return ResponseEntity.ok("fetch the product details with id:" + id);
    }

    // Complex PUT endpoint
    @PutMapping("/update/{id}")
    public ResponseEntity<Product> createProduct(
        @PathVariable String id,
        @RequestBody Product product,
        @RequestParam("sendMail") boolean sendMail,
        @RequestHeader("X-ConceptCoding-ID") String uniqueID
    ) {
        Product dbProductObject = getProductFromDB(id);
        dbProductObject.setName(product.getName());

        return ResponseEntity.ok(dbProductObject);
    }
}
```

---

## Side-by-Side Mapping — Caller vs Receiver

```
ORDER SERVICE (Caller)                    PRODUCT SERVICE (Receiver)
ProductClient interface                   ProductController
──────────────────────────────────────────────────────────────────────

@PutMapping(                              @PutMapping("/update/{id}")
  value="/products/update/{id}",
  consumes="application/json"
)
Product updateProduct(                    ResponseEntity<Product> createProduct(

  @PathVariable("id")                       @PathVariable
  String id,                 ──────────►    String id,

  @RequestParam("sendMail")                 @RequestParam("sendMail")
  boolean sendMail,          ──────────►    boolean sendMail,

  @RequestHeader(                           @RequestHeader(
    "X-ConceptCoding-ID")                     "X-ConceptCoding-ID")
  String uniqueID,           ──────────►    String uniqueID,

  @RequestBody                              @RequestBody
  Product updatedProductDetails ───────►    Product product
);                                        )
```

---

## Very Important — Parameter Ordering is NOT Mandatory

This is a key point the instructor highlights.

Look at the parameter order in both sides:

```
CALLER (ProductClient interface)     RECEIVER (ProductController)
────────────────────────────────     ────────────────────────────
1. @PathVariable   id                1. @PathVariable   id
2. @RequestParam   sendMail          2. @RequestBody    product      ← different order!
3. @RequestHeader  uniqueID          3. @RequestParam   sendMail
4. @RequestBody    product           4. @RequestHeader  uniqueID
```

The order is **completely different** between caller and receiver — and it still works perfectly.

> **Why?** Because FeignClient doesn't match by position. It reads the **annotation** on each parameter and assigns the value accordingly. `@PathVariable` always goes into the URL, `@RequestParam` always becomes a query param, etc. — regardless of where in the parameter list it appears.

---

## How Each Annotation Maps to the HTTP Request

```
┌─────────────────────────────────────────────────────────────────────────┐
│              How FeignClient Builds the HTTP Request                    │
│                                                                         │
│  Method definition:                                                     │
│  updateProduct("123", true, "abc", productObj)                          │
│                                                                         │
│  ┌─────────────────┬──────────────────────────────────────────────┐     │
│  │   Annotation    │   Where it goes in HTTP Request              │     │
│  ├─────────────────┼──────────────────────────────────────────────┤     │
│  │ @PathVariable   │ Replaces {id} in URL                         │     │
│  │ id = "123"      │ → PUT http://localhost:8082/products/        │     │
│  │                 │         update/123                           │     │
│  ├─────────────────┼──────────────────────────────────────────────┤     │
│  │ @RequestParam   │ Appended as query parameter                  │     │
│  │ sendMail = true │ → ?sendMail=true                             │     │
│  │                 │ Full URL: /products/update/123?sendMail=true │     │
│  ├─────────────────┼──────────────────────────────────────────────┤     │
│  │ @RequestHeader  │ Added as HTTP header                         │     │
│  │ uniqueID="abc"  │ → Header: X-ConceptCoding-ID: abc            │     │
│  ├─────────────────┼──────────────────────────────────────────────┤     │
│  │ @RequestBody    │ Serialized to JSON, sent as request body     │     │
│  │ productObj      │ → Body: {"name":"updated name", ...}         │     │
│  └─────────────────┴──────────────────────────────────────────────┘     │
│                                                                         │
│  Final HTTP Request:                                                    │
│  ─────────────────────────────────────────────                          │
│  PUT http://localhost:8082/products/update/123?sendMail=true            │
│  Header: X-ConceptCoding-ID: abc                                        │
│  Content-Type: application/json                                         │
│  Body: {"name": "updated name", ...}                                    │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## The consumes = "application/json" — What Does It Do?

```java
@PutMapping(
    value = "/products/update/{id}",
    consumes = "application/json"       // ← this
)
```

This tells FeignClient:

```
┌────────────────────────────────────────────────────────┐
│  consumes = "application/json"                         │
│                                                        │
│  → Sets Content-Type: application/json                 │
│    in the outgoing HTTP request header                 │
│                                                        │
│  → Tells the Product Service:                          │
│    "I'm sending you a JSON body"                       │
│                                                        │
│  → The encoder will serialize the @RequestBody         │
│    Java object into JSON format                        │
└────────────────────────────────────────────────────────┘
```

---

## Key Takeaways from Step 5

```
┌────────────────────────────────────────────────────────────────┐
│                      REMEMBER THIS                             │
│                                                                │
│  FeignClient interface supports ALL the same annotations       │
│  you use in Controllers:                                       │
│                                                                │
│  @GetMapping / @PutMapping / @PostMapping / @DeleteMapping     │
│  @PathVariable   → replaces {placeholder} in URL               │
│  @RequestParam   → becomes ?key=value query param              │
│  @RequestHeader  → becomes HTTP header                         │
│  @RequestBody    → becomes JSON request body                   │
│                                                                │
│  Parameter ORDER does NOT matter                               │
│  → FeignClient maps by ANNOTATION, not by position             │
│                                                                │
│  The interface mirrors the Controller of target service        │
│  → Same endpoint path                                          │
│  → Same HTTP method                                            │
│  → Same parameters (but order can differ)                      │
└────────────────────────────────────────────────────────────────┘
```

---
# Step 6 — Encoder & Decoder in FeignClient

---

## What Problem Do Encoder & Decoder Solve?

When two microservices communicate over HTTP, they can't pass Java objects directly over the network. Everything has to be converted to a format that HTTP understands — like **JSON**.

```
┌─────────────────────────────────────────────────────────────────────┐
│                    THE CORE PROBLEM                                 │
│                                                                     │
│  Order Service has a Java object → Product(name="phone", price=999) │
│                                                                     │
│  Can't send Java object over HTTP directly ✗                        │
│                                                                     │
│  Must convert to JSON first:                                        │
│  {"name": "phone", "price": 999}  ✓                                 │
│                                                                     │
│  And when response comes back as JSON →                             │
│  Must convert back to Java object ✓                                 │
└─────────────────────────────────────────────────────────────────────┘
```

This is exactly what **Encoder** and **Decoder** handle in FeignClient.

---

## Encoder & Decoder — Simple Definition

```
┌─────────────────────────────────────────────────────────────────────┐
│                                                                     │
│   ENCODER                                                           │
│   → Converts Java object  ──────►  JSON (request body)              │
│   → Happens BEFORE sending the HTTP request                         │
│   → Same as "Serialization" in RestTemplate world                   │
│                                                                     │
│   DECODER                                                           │
│   → Converts JSON response  ──────►  Java object                    │
│   → Happens AFTER receiving the HTTP response                       │
│   → Same as "Deserialization" in RestTemplate world                 │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Where Exactly Do Encoder & Decoder Fit — In the Flow?

Remember the `MethodHandler.invoke()` from Step 4? Here's where encoder and decoder are called inside it:

```
MethodHandler.invoke(args)
        │
        ▼
┌────────────────────────────────────────────────────────────────────┐
│                    executeAndDecode()                              │
│                                                                    │
│   1. BUILD REQUEST                                                 │
│      buildTemplateFromArgs.create(args)                            │
│               │                                                    │
│               │  If there is a @RequestBody                        │
│               ▼                                                    │
│      encoder.encode(body, bodyType, requestTemplate)               │
│      → Java object gets converted to raw JSON                      │
│      → JSON is placed into the request body                        │
│               │                                                    │
│               ▼                                                    │
│   2. EXECUTE HTTP CALL                                             │
│      httpClient.execute(request)                                   │
│      → Actual network call happens here                            │
│               │                                                    │
│               ▼                                                    │
│   3. DECODE RESPONSE                                               │
│      decoder.decode(response, returnType)                          │
│      → JSON response converted to Java object                      │
│      → returnType = return type of your interface method           │
└────────────────────────────────────────────────────────────────────┘
```

---

## Default Behavior — What Happens If You Don't Configure Anything?

Both `Encoder` and `Decoder` are **interfaces** in FeignClient. Spring Cloud provides default implementations that use **Jackson** internally.

```
┌──────────────────────────────────────────────────────────────────┐
│                    DEFAULT IMPLEMENTATIONS                       │
│                                                                  │
│   Encoder interface                                              │
│        └── SpringEncoder (default)                               │
│              → uses Jackson                                      │
│              → converts Java object to JSON                      │
│              → puts JSON into RequestTemplate body               │
│                                                                  │
│   Decoder interface                                              │
│        └── SpringDecoder (default)                               │
│              → uses Jackson                                      │
│              → reads response byte stream                        │
│              → converts JSON to Java object                      │
│              → return type = method's return type                │
└──────────────────────────────────────────────────────────────────┘
```

> **Instructor's note:** *"The default implementation does the same thing we used to do manually in RestTemplate — reading the stream and converting to an object. But now it's all abstracted away from us."*

In most real projects, the **default encoder and decoder are perfectly fine.** You only need custom ones if you have special serialization logic.

---

## Custom Encoder & Decoder — When & How

The instructor shows a per-client configuration. Here's the full setup:

### Step A — The FeignClient Interface (with configuration attached)

```java
@FeignClient(
    name = "product-service",
    url = "${feign.client.product-service.url}",
    configuration = ProductClientConfig.class    // ← attach config class
)
public interface ProductClient {

    @PutMapping(
        value = "/products/update/{id}",
        consumes = "application/json"
    )
    Product updateProduct(
        @PathVariable("id") String id,
        @RequestParam("sendMail") boolean sendMail,
        @RequestHeader("X-ConceptCoding-ID") String uniqueID,
        @RequestBody Product updatedProductDetails
    );
}
```

---

### Step B — Custom Encoder Implementation

```java
// implements the Encoder interface from FeignClient
public class MyCustomProductClientEncoder implements Encoder {

    @Override
    public void encode(
        Object object,              // the Java object to encode (@RequestBody value)
        Type bodyType,              // the type of the object
        RequestTemplate template    // the HTTP request being built
    ) throws EncodeException {

        try {
            // Convert Java object → JSON string using Jackson
            String jsonString = new ObjectMapper().writeValueAsString(object);

            // Put the JSON into the request body
            template.body(jsonString);

        } catch (Exception e) {
            throw new EncodeException("Unable to encode object");
        }
    }
}
```

---

### Step C — Custom Decoder Implementation

```java
// implements the Decoder interface from FeignClient
public class MyCustomProductClientDecoder implements Decoder {

    @Override
    public Object decode(
        Response response,    // the HTTP response received
        Type type             // the expected return type of the interface method
    ) throws IOException, DecodeException, FeignException {

        // Read the response body as an InputStream
        InputStream responseBody = response.body().asInputStream();

        // Convert JSON stream → Java object using Jackson
        return new ObjectMapper().readValue(
            responseBody,
            new TypeReference<Object>() {
                @Override
                public Type getType() {
                    return type;    // uses the actual return type of the method
                }
            }
        );
    }
}
```

---

### Step D — Configuration Class (Register as Spring Beans)

```java
@Configuration
public class ProductClientConfig {

    @Bean
    public Encoder myCustomEncoder() {
        return new MyCustomProductClientEncoder();  // framework uses THIS encoder
    }

    @Bean
    public Decoder myCustomDecoder() {
        return new MyCustomProductClientDecoder();  // framework uses THIS decoder
    }
}
```

---

## Per-Client Configuration — The Big Advantage

This is a key advantage the instructor highlights. You can have **different configurations for different FeignClients**.

```
┌────────────────────────────────────────────────────────────────────┐
│                    ORDER SERVICE                                   │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  ProductClient                                               │  │
│  │  @FeignClient(configuration = ProductClientConfig.class)     │  │
│  │  → uses MyCustomProductEncoder                               │  │
│  │  → uses MyCustomProductDecoder                               │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  SalesClient                                                 │  │
│  │  @FeignClient(configuration = SalesClientConfig.class)       │  │
│  │  → uses different encoder/decoder                            │  │
│  │  → completely independent configuration                      │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  UserClient                                                  │  │
│  │  @FeignClient(configuration = UserClientConfig.class)        │  │
│  │  → uses its own encoder/decoder                              │  │
│  └──────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────┘
```

> **Instructor's words:** *"One of the advantages — it's so clean that we can manage the configuration between different clients easily. There is no impact between each other."*

---

## Complete Flow With Custom Encoder & Decoder

```
OrderController
      │
      │  productClient.updateProduct("123", true, "abc", productObj)
      ▼
FeignClient Proxy
      │
      ▼
InvocationHandler → MethodHandler
      │
      ▼
┌─────────────────────────────────────────────────────────────────┐
│                   executeAndDecode()                            │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  ENCODE (outgoing)                                       │   │
│  │  MyCustomProductClientEncoder.encode(productObj, ...)    │   │
│  │  productObj  ──Jackson──►  {"name":"phone","price":999}  │   │
│  │  → placed into request body                              │   │
│  └──────────────────────────────────────────────────────────┘   │
│                          │                                      │
│                          │  HTTP PUT to Product Service         │
│                          ▼                                      │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  DECODE (incoming)                                       │   │
│  │  MyCustomProductClientDecoder.decode(response, Product)  │   │
│  │  {"name":"phone","price":999}  ──Jackson──►  Product obj │   │
│  └──────────────────────────────────────────────────────────┘   │
│                          │                                      │
└──────────────────────────┼──────────────────────────────────────┘
                           │
                           ▼
                  Product object returned
                  back to OrderController
```

---

## Key Takeaways from Step 6

```
┌────────────────────────────────────────────────────────────────────┐
│                       REMEMBER THIS                                │
│                                                                    │
│  Encoder  → Java object to JSON (before sending request)           │
│  Decoder  → JSON to Java object (after receiving response)         │
│                                                                    │
│  Both are INTERFACES — default implementations use Jackson         │
│  Default is good enough for most cases                             │
│                                                                    │
│  Custom implementation:                                            │
│  → implement Encoder / Decoder interface                           │
│  → register as @Bean inside a @Configuration class                 │
│  → attach config class to @FeignClient via                         │
│    configuration = YourConfig.class                                │
│                                                                    │
│  Each FeignClient can have its OWN configuration                   │
│  → ProductClient → ProductClientConfig                             │
│  → SalesClient   → SalesClientConfig                               │
│  → UserClient    → UserClientConfig                                │
│  → No interference between them                                    │
└────────────────────────────────────────────────────────────────────┘
```

---
# Step 7 — ErrorDecoder in FeignClient

---

## What Problem Does ErrorDecoder Solve?

In real-world microservice communication, things go wrong. The service you're calling might return a `400 Bad Request`, `404 Not Found`, `500 Internal Server Error`, etc.

By default, when FeignClient gets a **non-2xx response**, it throws a generic `FeignException`. But in production systems, you want **meaningful, specific exceptions** — so you can handle them properly up the chain.

That's exactly what **ErrorDecoder** is for.

```
┌─────────────────────────────────────────────────────────────────────┐
│                    THE CORE PROBLEM                                 │
│                                                                     │
│  Product Service returns 404 Not Found                              │
│                    or 500 Internal Server Error                     │
│                                                                     │
│  Default FeignClient behavior:                                      │
│  → throws generic FeignException                                    │
│  → error message unclear                                            │
│  → hard to handle specifically in OrderController                   │
│                                                                     │
│  What we want:                                                      │
│  → 4xx → throw MyCustomBadRequestException("Client Error")          │
│  → 5xx → throw MyCustomServerException("Server Error")              │
│  → Clean, specific, meaningful exceptions                           │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Where ErrorDecoder Fits in the Flow

Remember `executeAndDecode()` from Step 4 and Step 6? ErrorDecoder is triggered right there — but only for non-2xx responses:

```
MethodHandler.invoke(args)
        │
        ▼
executeAndDecode(request, options)
        │
        ├──► encoder.encode()       → builds request body
        │
        ├──► httpClient.execute()   → makes HTTP call
        │
        ├──► response received
        │
        │    Is response 2xx?
        │    ┌─────YES──────┐         ┌──────NO──────┐
        │    ▼              │         ▼              │
        │  decoder          │    errorDecoder        │
        │  .decode()        │    .decode()           │
        │  → Java object    │    → throws Exception  │
        └───────────────────┘    └───────────────────┘
```

> **Important from instructor:** *"ErrorDecoder is ONLY for non-2xx status codes — 4xx and 5xx. For successful 2xx responses, the regular Decoder handles it."*

---

## Also Important — ErrorDecoder vs Retryer

```
┌─────────────────────────────────────────────────────────────────────┐
│            ErrorDecoder vs Retryer — Who Handles What?              │
│                                                                     │
│  Connection Timeout / IOException (network issues)                  │
│  → Retryer handles it first                                         │
│  → If all retry attempts exhausted → ErrorDecoder is invoked        │
│                                                                     │
│  4xx / 5xx HTTP responses                                           │
│  → Goes DIRECTLY to ErrorDecoder                                    │
│  → Retryer is NOT involved at all                                   │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Default ErrorDecoder Behavior

`ErrorDecoder` is an **interface**. It has a default implementation called `ErrorDecoder.Default`.

Here's what the default does:

```java
// This is what ErrorDecoder.Default does internally
// (simplified understanding)

public class Default implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {

        // Wraps the error into a FeignException
        // Includes: status code + response body + headers
        return FeignException.errorStatus(methodKey, response);
    }
}
```

So when Product Service returns a 400, by default you get something like:

```
FeignException$BadRequest:
  status: 400
  method: GET http://localhost:8082/products/999
  body: {"error": "product not found"}
  headers: {...}
```

This works, but it's generic. In a real system, you want to be more specific.

---

## Custom ErrorDecoder — Full Implementation

### Step A — The FeignClient Interface (same as before, with config)

```java
@FeignClient(
    name = "product-service",
    url = "${feign.client.product-service.url}",
    configuration = ProductClientConfig.class    // ← config class attached
)
public interface ProductClient {

    @PutMapping(
        value = "/products/update/{id}",
        consumes = "application/json"
    )
    Product updateProduct(
        @PathVariable("id") String id,
        @RequestParam("sendMail") boolean sendMail,
        @RequestHeader("X-ConceptCoding-ID") String uniqueID,
        @RequestBody Product updatedProductDetails
    );
}
```

---

### Step B — Custom Exception Classes

```java
// For 4xx errors — client side problems
public class MyCustomBadRequestException extends RuntimeException {

    private final int statusCode;

    public MyCustomBadRequestException(String message) {
        super(message);
        this.statusCode = 400;
    }

    public int getStatusCode() {
        return statusCode;
    }
}


// For 5xx errors — server side problems
public class MyCustomServerException extends RuntimeException {

    private final int statusCode;

    public MyCustomServerException(String message) {
        super(message);
        this.statusCode = 500;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
```

---

### Step C — Custom ErrorDecoder Implementation

```java
// implements ErrorDecoder interface
public class MyCustomProductClientErrorDecoder implements ErrorDecoder {

    // Keep a reference to the default — for cases we don't handle ourselves
    private final ErrorDecoder defaultErrorDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {

        // Read the HTTP status code from the response
        HttpStatus statusCode = HttpStatus.valueOf(response.status());

        if (statusCode.is4xxClientError()) {
            // 400, 401, 403, 404 etc.
            return new MyCustomBadRequestException("Client Error");

        } else if (statusCode.is5xxServerError()) {
            // 500, 502, 503 etc.
            return new MyCustomServerException("Server Error");

        } else {
            // For anything else — let the default handle it
            return defaultErrorDecoder.decode(methodKey, response);
        }
    }
}
```

> **Key design decision from instructor:** *"If I am not able to handle it, ultimately I'll pass it to the default decode — 'Hey, you handle it.' This is a clean pattern."*

---

### Step D — Register in Configuration Class

```java
@Configuration
public class ProductClientConfig {

    // From Step 6 — encoder and decoder beans
    @Bean
    public Encoder myCustomEncoder() {
        return new MyCustomProductClientEncoder();
    }

    @Bean
    public Decoder myCustomDecoder() {
        return new MyCustomProductClientDecoder();
    }

    // New — ErrorDecoder bean
    @Bean
    public ErrorDecoder myCustomErrorDecoder() {
        return new MyCustomProductClientErrorDecoder();
    }
}
```

---

## Complete Flow With Custom ErrorDecoder

```
OrderController
      │
      │  productClient.updateProduct("999", ...)
      │  (product 999 doesn't exist)
      ▼
FeignClient Proxy → InvocationHandler → MethodHandler
      │
      ▼
executeAndDecode()
      │
      ├──► encoder.encode()           → builds JSON request body
      │
      ├──► HTTP PUT to Product Service
      │    → Product Service returns 404 Not Found
      │
      ├──► response.status() = 404
      │    Is 2xx? NO
      │
      ▼
MyCustomProductClientErrorDecoder.decode(methodKey, response)
      │
      │  statusCode = 404
      │  statusCode.is4xxClientError() = true
      │
      ▼
throws MyCustomBadRequestException("Client Error")
      │
      ▼
Exception propagates back to OrderController
      │
      ▼
OrderController can now catch specifically:
      try {
          productClient.updateProduct(...)
      } catch (MyCustomBadRequestException e) {
          // handle 4xx
      } catch (MyCustomServerException e) {
          // handle 5xx
      }
```

---

## Default vs Custom — Side by Side

```
┌────────────────────────────────────────────────────────────────────┐
│                  Default vs Custom ErrorDecoder                    │
│                                                                    │
│  Scenario: Product Service returns 404                             │
│                                                                    │
│  ┌────────────────────────┬───────────────────────────────────┐    │
│  │   Default Behavior     │    Custom Behavior                │    │
│  ├────────────────────────┼───────────────────────────────────┤    │
│  │ throws FeignException  │ throws                            │    │
│  │ {                      │ MyCustomBadRequestException       │    │
│  │   status: 404,         │ ("Client Error")                  │    │
│  │   method: GET /...,    │                                   │    │
│  │   body: {...},         │ Clean, specific, meaningful       │    │
│  │   headers: {...}       │ Easy to catch and handle          │    │
│  │ }                      │                                   │    │
│  │                        │                                   │    │
│  │ Generic, hard to       │                                   │    │
│  │ handle specifically    │                                   │    │
│  └────────────────────────┴───────────────────────────────────┘    │
└────────────────────────────────────────────────────────────────────┘
```

---

## The Full ProductClientConfig So Far

At this point your config class has grown to handle encoder, decoder, and errorDecoder — all specific to `ProductClient`:

```java
@Configuration
public class ProductClientConfig {

    // Custom Encoder — Java object to JSON
    @Bean
    public Encoder myCustomEncoder() {
        return new MyCustomProductClientEncoder();
    }

    // Custom Decoder — JSON to Java object
    @Bean
    public Decoder myCustomDecoder() {
        return new MyCustomProductClientDecoder();
    }

    // Custom ErrorDecoder — handles 4xx and 5xx
    @Bean
    public ErrorDecoder myCustomErrorDecoder() {
        return new MyCustomProductClientErrorDecoder();
    }
}
```

---

## Key Takeaways from Step 7

```
┌───────────────────────────────────────────────────────────────────┐
│                       REMEMBER THIS                               │
│                                                                   │
│  ErrorDecoder handles non-2xx HTTP responses (4xx and 5xx)        │
│                                                                   │
│  Default: throws generic FeignException                           │
│  Custom:  throw your own meaningful exceptions                    │
│                                                                   │
│  4xx / 5xx → goes DIRECTLY to ErrorDecoder                        │
│  Network issues → Retryer first → then ErrorDecoder               │
│                                                                   │
│  Custom implementation:                                           │
│  → implement ErrorDecoder interface                               │
│  → override decode(methodKey, response) method                    │
│  → check status code → throw appropriate exception                │
│  → for unhandled cases → delegate to defaultErrorDecoder          │
│  → register as @Bean in your config class                         │
│                                                                   │
│  Each FeignClient has its OWN ErrorDecoder                        │
│  → via configuration = YourConfig.class                           │
└───────────────────────────────────────────────────────────────────┘
```

---
# Step 8 — Retryer in FeignClient

---

## What Problem Does Retryer Solve?

In a distributed system, network calls can fail temporarily — not because the service is down permanently, but due to brief network hiccups, connection timeouts, etc. In these cases, simply retrying the call after a short wait often succeeds.

```
┌─────────────────────────────────────────────────────────────────────┐
│                    THE CORE PROBLEM                                 │
│                                                                     │
│  Order Service calls Product Service                                │
│  → Connection timeout happens (network blip)                        │
│  → Should we just fail immediately? ✗                               │
│  → Or should we wait a bit and try again? ✓                         │
│                                                                     │
│  Without retry:                                                     │
│  → One temporary network issue = request fails                      │
│  → Poor user experience                                             │
│                                                                     │
│  With retry:                                                        │
│  → Temporary issue → wait → try again → succeeds                    │
│  → Much more resilient system                                       │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Very Important — What Triggers a Retry?

The instructor is very specific about this. **Not every failure causes a retry.**

```
┌─────────────────────────────────────────────────────────────────────┐
│                  WHAT TRIGGERS RETRY?                               │
│                                                                     │
│  ✓ Connection Timeout                                               │
│  ✓ Network related exceptions (IOException)                         │
│    → These are "retriable" exceptions                               │
│                                                                     │
│  ✗ 4xx responses (Bad Request, Not Found, etc.)                     │
│  ✗ 5xx responses (Server Error, etc.)                               │
│    → These go DIRECTLY to ErrorDecoder                              │
│    → Retryer is NOT involved for HTTP error responses               │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Where Retryer Fits in the Flow

```
MethodHandler.invoke(args)
        │
        ▼
┌───────────────────────────────────────────────────────────────────┐
│  while(true) {                                                    │
│      try {                                                        │
│          return executeAndDecode(request)   ◄── actual HTTP call  │
│                                                                   │
│      } catch (RetryableException e) {       ◄── connection issue  │
│          try {                                                    │
│              retryer.continueOrPropagate(e) ◄── should we retry?  │
│                                                                   │
│          } catch (RetryableException th) {                        │
│              throw th;                      ◄── all retries done  │
│          }                                                        │
│          continue;                          ◄── try again         │
│      }                                                            │
│  }                                                                │
└───────────────────────────────────────────────────────────────────┘
        │
        │  if all retries exhausted
        ▼
  ErrorDecoder.decode()   ← handles the final exception
```

> **Key point from instructor:** *"continueOrPropagate — continue means retry attempt left, go hit it one more time. Propagate means all attempts finished, let ErrorDecoder handle it."*

---

## Default Retryer Behavior — Retryer.Default

`Retryer` is an **interface**. The default implementation is `Retryer.Default`.

Here's how the default behaves:

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Retryer.Default Configuration                    │
│                                                                     │
│  maxAttempts  = 5  (includes the 1st actual call)                   │
│  period       = 100ms  (initial wait time between retries)          │
│  maxPeriod    = 1 second  (maximum wait time cap)                   │
│                                                                     │
│  Wait time doubles with each retry (but capped at maxPeriod)        │
└─────────────────────────────────────────────────────────────────────┘
```

Let's visualize the retry timeline:

```
┌─────────────────────────────────────────────────────────────────────┐
│                    DEFAULT RETRY TIMELINE                           │
│                                                                     │
│  Try 1 ──► IMMEDIATE (actual call)                                  │
│              │                                                      │
│              │  Connection Timeout ✗                                │
│              ▼                                                      │
│  Wait 100ms                                                         │
│              │                                                      │
│  Try 2 ──► retry attempt 1                                          │
│              │                                                      │
│              │  Connection Timeout ✗                                │
│              ▼                                                      │
│  Wait 200ms  (doubled)                                              │
│              │                                                      │
│  Try 3 ──► retry attempt 2                                          │
│              │                                                      │
│              │  Connection Timeout ✗                                │
│              ▼                                                      │
│  Wait 400ms  (doubled)                                              │
│              │                                                      │
│  Try 4 ──► retry attempt 3                                          │
│              │                                                      │
│              │  Connection Timeout ✗                                │
│              ▼                                                      │
│  Wait 800ms  (doubled)                                              │
│              │                                                      │
│  Try 5 ──► retry attempt 4                                          │
│              │                                                      │
│              │  Connection Timeout ✗                                │
│              ▼                                                      │
│  All 5 attempts exhausted                                           │
│              │                                                      │
│              ▼                                                      │
│  ErrorDecoder.decode() is invoked                                   │
│                                                                     │
│  NOTE: Wait time would be 1600ms for Try 6 onwards                  │
│        but maxPeriod = 1 second caps it at 1000ms                   │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Option 1 — NEVER_RETRY (Disable Retrying Completely)

If you don't want any retries at all — not even the default 5 attempts — use `Retryer.NEVER_RETRY`. This is already built into the `Retryer` interface, no need to write your own class.

```java
@Configuration
public class ProductClientConfig {

    @Bean
    public Retryer myCustomRetryer() {
        return Retryer.NEVER_RETRY;    // only 1 attempt — the actual call
    }                                  // no retries at all
}
```

```
┌──────────────────────────────────────────────────────┐
│              NEVER_RETRY Timeline                    │
│                                                      │
│  Try 1 ──► IMMEDIATE (actual call)                   │
│              │                                       │
│              │  Connection Timeout ✗                 │
│              ▼                                       │
│  No more attempts                                    │
│  → ErrorDecoder.decode() invoked immediately         │
└──────────────────────────────────────────────────────┘
```

---

## Option 2 — Custom Retryer: Use Case 1 (Control Values Only)

> **Use Case:** *"I only want to control the attempt count, wait time, and max period. I'm happy with the rest of the default logic."*

You **extend** `Retryer.Default` and just pass different values to `super()`:

```java
public class MyCustomRetryer extends Retryer.Default {

    public MyCustomRetryer() {
        // super(period, maxPeriod, maxAttempts)
        super(200, 1000, 4);
        // period     = 200ms  (initial wait — was 100ms in default)
        // maxPeriod  = 1000ms (max wait cap — same as default)
        // maxAttempts = 4     (total attempts — was 5 in default)
    }

    // All the logic (doubling wait time, continueOrPropagate etc.)
    // is inherited from Retryer.Default — no need to rewrite
}
```

Register it in config:

```java
@Configuration
public class ProductClientConfig {

    @Bean
    public Retryer myCustomRetryer() {
        return new MyCustomRetryer();
    }
}
```

Timeline with these custom values:

```
┌─────────────────────────────────────────────────────────────────────┐
│           Custom Retryer (period=200, maxPeriod=1000, max=4)        │
│                                                                     │
│  Try 1 ──► IMMEDIATE                                                │
│              │  Timeout ✗                                           │
│  Wait 200ms                                                         │
│  Try 2 ──► retry 1                                                  │
│              │  Timeout ✗                                           │
│  Wait 400ms  (doubled)                                              │
│  Try 3 ──► retry 2                                                  │
│              │  Timeout ✗                                           │
│  Wait 800ms  (doubled)                                              │
│  Try 4 ──► retry 3  ← last attempt                                  │
│              │  Timeout ✗                                           │
│  All 4 attempts exhausted → ErrorDecoder invoked                    │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Option 3 — Custom Retryer: Use Case 2 (Full Control)

> **Use Case:** *"I want complete control over the retry logic — I'll implement everything myself."*

You **implement** the `Retryer` interface directly:

```java
public class MyCustomRetryer implements Retryer {

    private int attempt = 1;
    private final int maxAttempts = 5;

    @Override
    public void continueOrPropagate(RetryableException e) {

        // If we've used all attempts → stop retrying → throw exception
        if (attempt >= maxAttempts) {
            throw e;
        }

        // Otherwise → increment attempt count
        attempt++;

        // Wait before next retry (fixed 100ms here — you can make it dynamic)
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) {
            // ignore interruption
        }
    }

    @Override
    public Retryer clone() {
        // Important — FeignClient clones the Retryer for each request
        // so each request gets a fresh attempt counter
        return new MyCustomRetryer();
    }
}
```

Register it in config:

```java
@Configuration
public class ProductClientConfig {

    @Bean
    public Retryer myCustomRetryer() {
        return new MyCustomRetryer();
    }
}
```

---

## Extend vs Implement — When to Use Which?

```
┌─────────────────────────────────────────────────────────────────────┐
│              Extend vs Implement — Decision Guide                   │
│                                                                     │
│  extends Retryer.Default                                            │
│  → Use when you only want to change the numbers                     │
│    (attempt count, wait time, max period)                           │
│  → All the retry logic (doubling, capping) stays the same           │
│  → Less code to write                                               │
│                                                                     │
│  implements Retryer                                                 │
│  → Use when you want full control                                   │
│  → You write your own continueOrPropagate() logic                   │
│  → Fixed wait, custom backoff strategy, custom conditions, etc.     │
│  → More code but maximum flexibility                                │
└─────────────────────────────────────────────────────────────────────┘
```

---

## All 3 Retryer Options — Side by Side

```
┌────────────────────────────────────────────────────────────────────────┐
│                    ALL 3 RETRYER OPTIONS                               │
│                                                                        │
│  ┌──────────────────┬─────────────────────┬────────────────────────┐   │
│  │  Retryer.Default │  Retryer.NEVER_RETRY│  Custom Implementation │   │
│  ├──────────────────┼─────────────────────┼────────────────────────┤   │
│  │  5 attempts      │  1 attempt only     │  You decide            │   │
│  │  100ms initial   │  No waiting         │  You decide            │   │
│  │  doubles each    │                     │  You decide            │   │
│  │  1sec max cap    │                     │                        │   │
│  │                  │                     │  extend Default:       │   │
│  │  No config       │  return             │  change numbers only   │   │
│  │  needed          │  Retryer            │                        │   │
│  │                  │  .NEVER_RETRY       │  implement Retryer:    │   │
│  │                  │                     │  full custom logic     │   │
│  └──────────────────┴─────────────────────┴────────────────────────┘   │
└────────────────────────────────────────────────────────────────────────┘
```

---

## The Complete ProductClientConfig — Everything Together

At this point, your config class handles all customizations for `ProductClient`:

```java
@Configuration
public class ProductClientConfig {

    // Custom Encoder — Java object to JSON
    @Bean
    public Encoder myCustomEncoder() {
        return new MyCustomProductClientEncoder();
    }

    // Custom Decoder — JSON to Java object
    @Bean
    public Decoder myCustomDecoder() {
        return new MyCustomProductClientDecoder();
    }

    // Custom ErrorDecoder — handles 4xx and 5xx
    @Bean
    public ErrorDecoder myCustomErrorDecoder() {
        return new MyCustomProductClientErrorDecoder();
    }

    // Custom Retryer — handles connection timeouts / IOExceptions
    @Bean
    public Retryer myCustomRetryer() {
        return new MyCustomRetryer();
    }
}
```

---

## The Interaction Between Retryer and ErrorDecoder

This is important to understand clearly — they work together:

```
┌─────────────────────────────────────────────────────────────────────┐
│           How Retryer and ErrorDecoder Work Together                │
│                                                                     │
│  HTTP Call                                                          │
│      │                                                              │
│      ├──► 2xx Response                                              │
│      │         → Decoder handles it → return Java object            │
│      │                                                              │
│      ├──► 4xx / 5xx Response                                        │
│      │         → Skip Retryer completely                            │
│      │         → ErrorDecoder handles it immediately                │
│      │         → throws custom exception                            │
│      │                                                              │
│      └──► Connection Timeout / IOException                          │
│                │                                                    │
│                ▼                                                    │
│           Retryer.continueOrPropagate()                             │
│                │                                                    │
│                ├──► attempts left? → wait → retry the call          │
│                │                                                    │
│                └──► no attempts left?                               │
│                          │                                          │
│                          ▼                                          │
│                    ErrorDecoder.decode()                            │
│                    → throws exception                               │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Key Takeaways from Step 8

```
┌────────────────────────────────────────────────────────────────────┐
│                       REMEMBER THIS                                │
│                                                                    │
│  Retryer handles: Connection Timeout + IOException ONLY            │
│  NOT for 4xx/5xx — those go straight to ErrorDecoder               │
│                                                                    │
│  Default: 5 attempts, 100ms initial, doubles, 1sec cap             │
│                                                                    │
│  3 options:                                                        │
│  1. Retryer.Default     → use as-is, no config needed              │
│  2. Retryer.NEVER_RETRY → disable retrying completely              │
│  3. Custom:                                                        │
│     → extend Retryer.Default  (change numbers only)                │
│     → implement Retryer       (full control)                       │
│                                                                    │
│  Always override clone() in custom implementation                  │
│  → FeignClient clones Retryer per request                          │
│  → Ensures fresh attempt counter for each new request              │
│                                                                    │
│  After all retries exhausted → ErrorDecoder is invoked             │
│                                                                    │
│  Register custom Retryer as @Bean in your config class             │
└────────────────────────────────────────────────────────────────────┘
```

---
# Step 9 — Timeout Configuration in FeignClient

---

## What Problem Does Timeout Configuration Solve?

When your Order Service calls Product Service, two things can go wrong time-wise:

```
┌─────────────────────────────────────────────────────────────────────┐
│                    TWO TYPES OF TIMEOUTS                            │
│                                                                     │
│  1. CONNECTION TIMEOUT                                              │
│     → How long to WAIT while trying to establish                   │
│       a connection to the target service                            │
│     → If Product Service is unreachable/down                       │
│     → "I've been trying to connect for Xms — give up"             │
│                                                                     │
│  2. READ TIMEOUT                                                    │
│     → How long to WAIT for a response AFTER                        │
│       connection is established                                     │
│     → If Product Service is slow / stuck processing               │
│     → "I've been waiting for a response for Xms — give up"        │
└─────────────────────────────────────────────────────────────────────┘
```

Without proper timeouts, your Order Service could hang indefinitely waiting for a response — blocking threads, consuming resources, and causing cascading failures across your system.

---

## Where Does the `name` in @FeignClient Finally Become Useful?

Remember back in Step 3, the instructor said:

> *"The name in @FeignClient is just an arbitrary label."*

But now it becomes **very useful**. The name is how you target a **specific FeignClient** in your `application.properties` for configuration.

```java
@FeignClient(
    name = "product-service",    // ← THIS NAME is used in properties
    url = "${feign.client.product-service.url}"
)
public interface ProductClient {

    @GetMapping("/products/{id}")
    String getProductById(@PathVariable("id") String id);
}
```

---

## Two Ways to Configure Timeouts

### Way 1 — Per-Client Timeout (Only for a specific FeignClient)

Use the **name** of the FeignClient in the properties key:

```properties
# timeout applicable ONLY to product-service FeignClient
feign.client.config.product-service.connectTimeout=3000
feign.client.config.product-service.readTimeout=5000
```

```
┌──────────────────────────────────────────────────────────────────┐
│  feign.client.config.{name}.connectTimeout                       │
│                       ▲                                          │
│                       │                                          │
│           This matches the name="product-service"               │
│           in your @FeignClient annotation                        │
│                                                                  │
│  connectTimeout = 3000ms → 3 seconds to establish connection    │
│  readTimeout    = 5000ms → 5 seconds to wait for response       │
└──────────────────────────────────────────────────────────────────┘
```

---

### Way 2 — Global Timeout (Applies to ALL FeignClients)

Use the keyword `default` instead of the client name:

```properties
# timeout applicable to ALL FeignClients
feign.client.config.default.connectTimeout=3000
feign.client.config.default.readTimeout=5000
```

```
┌──────────────────────────────────────────────────────────────────┐
│  feign.client.config.default.connectTimeout                      │
│                       ▲                                          │
│                       │                                          │
│           "default" keyword = applies to every                   │
│            FeignClient in your application                       │
└──────────────────────────────────────────────────────────────────┘
```

---

## Real World Scenario — Why Per-Client Timeouts Matter

In a real microservice system, different services have different performance characteristics:

```
┌─────────────────────────────────────────────────────────────────────┐
│                      ORDER SERVICE                                  │
│                                                                     │
│  calls Product Service                                              │
│  → fast service, lightweight DB query                               │
│  → connectTimeout = 2000ms                                          │
│  → readTimeout    = 3000ms                                          │
│                                                                     │
│  calls Inventory Service                                            │
│  → moderate speed                                                   │
│  → connectTimeout = 2000ms                                          │
│  → readTimeout    = 5000ms                                          │
│                                                                     │
│  calls Report Service                                               │
│  → slow service, heavy data processing                              │
│  → connectTimeout = 2000ms                                          │
│  → readTimeout    = 30000ms  ← needs more time                      │
└─────────────────────────────────────────────────────────────────────┘
```

With per-client configuration, each FeignClient gets its own timeout — completely independent.

---

## Complete Configuration Example

### application.properties

```properties
server.port=8081

# ──────────────────────────────────────────────
# Base URLs for each service
# ──────────────────────────────────────────────
feign.client.product-service.url=http://localhost:8082
feign.client.inventory-service.url=http://localhost:8083
feign.client.report-service.url=http://localhost:8084

# ──────────────────────────────────────────────
# Per-client timeouts
# ──────────────────────────────────────────────

# Product Service — fast service
feign.client.config.product-service.connectTimeout=2000
feign.client.config.product-service.readTimeout=3000

# Inventory Service — moderate speed
feign.client.config.inventory-service.connectTimeout=2000
feign.client.config.inventory-service.readTimeout=5000

# Report Service — slow, heavy processing
feign.client.config.report-service.connectTimeout=2000
feign.client.config.report-service.readTimeout=30000
```

OR if all services share the same timeout:

```properties
# ──────────────────────────────────────────────
# Global timeout — applies to ALL FeignClients
# ──────────────────────────────────────────────
feign.client.config.default.connectTimeout=3000
feign.client.config.default.readTimeout=5000
```

---

## How Timeout Values Connect to MethodHandler Internally

Remember from Step 4 — `MethodHandler` holds all the info needed to make an HTTP call. Timeout values are part of that:

```
┌─────────────────────────────────────────────────────────────────────┐
│                       MethodHandler                                 │
│                                                                     │
│  targetURL      = http://localhost:8082/products/{id}               │
│  httpMethod     = GET                                               │
│  headerInfo     = ...                                               │
│  encoder        = SpringEncoder / custom                            │
│  decoder        = SpringDecoder / custom                            │
│  errorDecoder   = Default / custom                                  │
│  retryer        = Retryer.Default / custom                          │
│  logger         = ...                                               │
│                                                                     │
│  connectTimeout = 3000  ◄── read from application.properties        │
│  readTimeout    = 5000  ◄── read from application.properties        │
│                         via feign.client.config.{name}.*            │
└─────────────────────────────────────────────────────────────────────┘
```

> **Instructor's words:** *"All these configuration values — connection timeout, read timeout — ultimately all this somewhere will get set into MethodHandler. There might be some variables/fields which get filled up using these configurations."*

---

## Per-Client vs Global — Which Takes Priority?

```
┌─────────────────────────────────────────────────────────────────────┐
│              Per-Client vs Global — Priority Rule                   │
│                                                                     │
│  If BOTH are defined:                                               │
│                                                                     │
│  feign.client.config.default.connectTimeout=3000      ← global      │
│  feign.client.config.product-service.connectTimeout=2000 ← specific │
│                                                                     │
│  → Per-client (specific) takes PRIORITY over global (default)       │
│  → product-service FeignClient uses 2000ms                          │
│  → All other FeignClients use 3000ms from global                    │
└─────────────────────────────────────────────────────────────────────┘
```

---

## The name in @FeignClient — Full Picture of Its Uses

Now that we've reached the end, the `name` in `@FeignClient` has two important uses:

```
┌─────────────────────────────────────────────────────────────────────┐
│               What the 'name' in @FeignClient is used for           │
│                                                                     │
│  @FeignClient(name = "product-service", ...)                        │
│                                                                     │
│  USE 1 — application.properties timeout config                      │
│  feign.client.config.product-service.connectTimeout=3000            │
│  feign.client.config.product-service.readTimeout=5000               │
│                                                                     │
│  USE 2 — Service Discovery (Eureka) — coming in future              │
│  When you integrate Eureka, the name is used to look up             │
│  the service in the registry instead of hardcoding URL              │
│  → FeignClient will resolve "product-service" to actual             │
│    host:port via Eureka automatically                               │
│  → This is why URL becomes optional when using Eureka               │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Complete FeignClient Setup — Everything Together

Here is the **full picture** of everything covered across all 9 steps:

```
ORDER SERVICE
│
├── pom.xml
│   ├── spring-cloud-starter-openfeign (no version)
│   └── spring-cloud-dependencies BOM (version managed here)
│
├── OrderserviceApplication.java
│   └── @SpringBootApplication + @EnableFeignClients
│
├── ProductClient.java (interface)
│   └── @FeignClient(name="product-service",
│                    url="${...}",
│                    configuration=ProductClientConfig.class)
│       ├── @GetMapping  + @PathVariable
│       └── @PutMapping  + @PathVariable + @RequestParam
│                        + @RequestHeader + @RequestBody
│
├── ProductClientConfig.java (@Configuration)
│   ├── Encoder     → custom or default (Jackson)
│   ├── Decoder     → custom or default (Jackson)
│   ├── ErrorDecoder→ custom (4xx→BadRequestEx, 5xx→ServerEx)
│   └── Retryer     → custom / NEVER_RETRY / Default
│
└── application.properties
    ├── server.port=8081
    ├── feign.client.product-service.url=http://localhost:8082
    ├── feign.client.config.product-service.connectTimeout=3000
    └── feign.client.config.product-service.readTimeout=5000
```

---

## The Complete Internal Flow — Final End-to-End Diagram

```
APPLICATION STARTUP
──────────────────────────────────────────────────────────────────
@EnableFeignClients → scans → finds ProductClient
ReflectiveFeign.newInstance()
  → parses each method → creates MethodHandler
    (targetURL, httpMethod, headers, encoder, decoder,
     errorDecoder, retryer, connectTimeout, readTimeout)
  → creates InvocationHandler (stores method→MethodHandler map)
  → Dynamic Proxy creates runtime implementation of ProductClient
  → registered as Spring Bean → ready for @Autowired

AT RUNTIME
──────────────────────────────────────────────────────────────────
OrderController
  │  productClient.getProductById("1")
  ▼
Dynamic Proxy → InvocationHandler → MethodHandler
  │
  ▼
while(true) {
  try {
    encoder.encode(requestBody)       → Java obj to JSON
    httpClient.execute(request)       → HTTP call
    response received
      │
      ├── 2xx? → decoder.decode()    → JSON to Java obj → return
      │
      └── non-2xx? → errorDecoder.decode()
                      4xx → MyCustomBadRequestException
                      5xx → MyCustomServerException

  } catch (RetryableException) {
      retryer.continueOrPropagate()
        → attempts left? → wait → retry
        → no attempts?   → errorDecoder.decode()
  }
}
```

---

## Key Takeaways from Step 9

```
┌────────────────────────────────────────────────────────────────────┐
│                       REMEMBER THIS                                │
│                                                                    │
│  Two timeout types:                                                │
│  → connectTimeout: time to establish connection                    │
│  → readTimeout:    time to wait for response                       │
│                                                                    │
│  Two ways to configure:                                            │
│  → Per-client: feign.client.config.{name}.*                        │
│                uses the name from @FeignClient                     │
│  → Global:     feign.client.config.default.*                       │
│                applies to ALL FeignClients                         │
│                                                                    │
│  Per-client takes PRIORITY over global                             │
│                                                                    │
│  The name in @FeignClient:                                         │
│  → Used for per-client timeout config                              │
│  → Will be used for Eureka service discovery (future)              │
│                                                                    │
│  No code change needed for timeouts                                │
│  → Pure application.properties configuration                       │
└────────────────────────────────────────────────────────────────────┘
```

---

## That's a Wrap on FeignClient! 🎉

Here's everything covered across all 9 steps:

```
┌────────────────────────────────────────────────────────────────────┐
│                  COMPLETE FEIGN CLIENT NOTES                       │
│                                                                    │
│  Step 1 → What is FeignClient & why it exists                      │
│  Step 2 → pom.xml setup + BOM dependency management                │
│  Step 3 → 4 things to make it work                                 │
│  Step 4 → Internal working (MethodHandler → InvocationHandler      │
│           → Dynamic Proxy)                                         │
│  Step 5 → Advanced annotations (@PathVariable, @RequestParam,      │
│           @RequestHeader, @RequestBody)                            │
│  Step 6 → Encoder & Decoder (default + custom)                     │
│  Step 7 → ErrorDecoder (default + custom)                          │
│  Step 8 → Retryer (default, NEVER_RETRY, custom)                   │
│  Step 9 → Timeout configuration (per-client + global)              │
└────────────────────────────────────────────────────────────────────┘
```

Hope these notes serve you well — both for learning and for interviews! If you want to revisit any step or go deeper on anything, just ask!