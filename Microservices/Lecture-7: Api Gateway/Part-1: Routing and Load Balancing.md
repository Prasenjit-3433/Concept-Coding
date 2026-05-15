# Step 1 — What is API Gateway & Why Do We Need It?

---

## The Problem First (Why does API Gateway even exist?)

Imagine you're working at a company that has built its backend using microservices. You have multiple services running — Invoice Service, Order Service, Sales Service, Product Service, and so on. And you also have multiple clients hitting these services — web apps, mobile apps, third-party integrations, maybe hundreds of them.

Now, **without an API Gateway**, the setup looks like this:

```
Client 1  ──────────────────────►  Invoice Service  (localhost:8081)
Client 2  ──────────────────────►  Order Service    (localhost:8082)
Client 3  ──────────────────────►  Sales Service    (localhost:8083)
Client n  ──────────────────────►  ...more services
```

Every client needs to **know the host and port of every microservice** it wants to talk to. That means:

- Client 1 maintains: "Invoice is at port 8081, Order is at 8082, Sales is at 8083..."
- Client 2 maintains the same list.
- Client 3 maintains the same list.
- ...and so on for all 100 clients.

### Now what happens when things change?

In the microservices world, services keep evolving:
- One big service might **split into two smaller services**.
- Two services might **merge into one**.
- A service might **move to a different host/port**.

This happens all the time. And every time it does — **all 100 clients need to update their routing logic**. That's a massive, painful change.

> For example: Order Service and Sales Service get merged into one "OrderSales Service". Now all 100 clients who were sending order traffic to port 8082 need to update their code to point to the new merged service. The impact is huge.

---

## The Solution — API Gateway

An API Gateway sits **between your clients and your microservices**. It acts as a **single entry point** for all client requests.

```
                        ┌─────────────────────────────────────────┐
                        │            API GATEWAY                  │
                        │           (Port: 8083)                  │
                        │                                         │
Client 1 ──────────────►│  /api/invoice  ──► Invoice Microservice │
Client 2 ──────────────►│  /api/order    ──► Order Microservice   │
Client 3 ──────────────►│  /api/sales    ──► Sales Microservice   │
Client n ──────────────►│                                         │
                        └─────────────────────────────────────────┘
```

Clients **never talk directly to microservices**. They only talk to the API Gateway. The gateway internally figures out where to forward the request.

Now if Order and Sales merge? **Only the API Gateway's routing config needs to change** — not a single client is affected. That's the power.

---

## What Benefits Does This Single Entry Point Give Us?

Because every request passes through the gateway, you can plug in a lot of cross-cutting features right there — without touching individual microservices. The instructor lists these:

```
┌──────────────────────────────────────────────────────┐
│                    API GATEWAY                       │
│                                                      │
│  1.  Routing                → forward to right svc   │
│  2.  Load Balancing         → spread traffic evenly  │
│  3.  Authentication (JWT)   → verify token once      │
│  4.  Rate Limiting          → stop traffic flooding  │
│  5.  Circuit Breaker/Retry  → resilience             │
│  6.  Req/Res Transformation → modify payloads        │
│  7.  Monitoring & Logging   → centralized logs       │
└──────────────────────────────────────────────────────┘
```

Let's understand each one briefly before we go into implementation:

**1. Routing** — The gateway knows which path maps to which microservice. Client just hits `/products/1`, gateway decides "this goes to Product Service."

**2. Load Balancing** — If Product Service has 3 instances running, the gateway distributes traffic across all 3 using a load balancing algorithm. No single instance gets overwhelmed.

**3. Authentication** — Instead of writing JWT verification logic in every single microservice (which is code duplication), you write it once at the gateway. If the token is invalid, the request is rejected right there — it never even reaches the microservice.

**4. Rate Limiting** — You can say "only allow 100 requests per second per client." The gateway enforces this at the front door, so your microservices are never flooded with traffic.

**5. Circuit Breaker / Retry** — If a microservice is down or slow, the gateway can retry the request or "break the circuit" and return a fallback response, instead of letting failures cascade.

**6. Request/Response Transformation** — The gateway can modify the incoming request (add headers, change body) before forwarding, and also transform the response before sending it back to the client.

**7. Monitoring & Logging** — Since every request passes through here, you get a centralized place to log all traffic. One log system for all microservices.

---

## Quick Summary Before Moving to Implementation

```
WITHOUT API GATEWAY:
  - Clients know all microservice addresses
  - Any service change = update ALL clients
  - Auth, logging, rate limiting duplicated everywhere

WITH API GATEWAY:
  - Clients know only ONE address (the gateway)
  - Any service change = update only the gateway
  - Auth, logging, rate limiting handled in ONE place
```

---

That's the full picture of **what API Gateway is and why it exists**. The instructor builds up from the problem to the solution very naturally — first showing you the pain, then introducing the gateway as the fix, then listing all the bonuses you get for free.

# Step 2 — Routing: How It Works + Full Implementation

---

## What is Routing in API Gateway?

Routing simply means — **when a request comes in, forward it to the correct microservice.**

The gateway looks at the incoming request (mainly the URL path) and decides:
- "This request starts with `/products` → send it to Product Service"
- "This request starts with `/orders` → send it to Order Service"

The client doesn't need to know anything about where these services live. It just hits the gateway.

---

## What We Are Building

The instructor sets up **3 Spring Boot applications**:

```
┌─────────┐        ┌─────────────────────┐        ┌──────────────────────┐
│         │        │                     │ /products/**                   │
│  Client │───────►│    API GATEWAY      │───────► Product Service :8082  │
│(Postman)│        │    (Port: 8083)     │        └──────────────────────┘
│         │        │                     │        
└─────────┘        │                     │        ┌──────────────────────┐
                   │                     │ /orders/**                     │
                   │                     │───────► Order Service  :8081   │
                   └─────────────────────┘        └──────────────────────┘
```

- **Product Service** → runs on port `8082`, exposes `/products/{id}`
- **Order Service** → runs on port `8081`, exposes `/orders/{id}`
- **API Gateway** → runs on port `8083`, routes traffic to both

The client (Postman) **only talks to port 8083**. It never directly calls 8081 or 8082.

---

## Step-by-Step Implementation

---

### Service 1 — Product Microservice

**Create a new Spring Boot project** from [start.spring.io](https://start.spring.io) with just the `Spring Web` dependency.

**ProductController.java**
```java
@RestController
@RequestMapping("/products")
public class ProductController {

    @GetMapping("/{id}")
    public ResponseEntity<String> getProduct(@PathVariable String id) {
        return ResponseEntity.ok().body("fetch the product details with id: " + id);
    }
}
```

**application.properties**
```properties
server.port=8082
spring.application.name=product-service
```

> Simple — one endpoint, runs on 8082. Nothing special here.

---

### Service 2 — Order Microservice

**Create another Spring Boot project** with just the `Spring Web` dependency.

**OrderController.java**
```java
@RestController
@RequestMapping("/orders")
public class OrderController {

    @GetMapping("/{id}")
    public ResponseEntity<String> getOrder(@PathVariable String id) {
        return ResponseEntity.ok().body("fetch the Order details with id: " + id);
    }
}
```

**application.properties**
```properties
server.port=8081
spring.application.name=order-service
```

> Same idea — one endpoint, runs on 8081.

---

### Service 3 — API Gateway (The Main Part)

**Create a new Spring Boot project** from [start.spring.io](https://start.spring.io) but this time choose the **`Reactive Gateway`** dependency (NOT Spring Web).

This adds the following to your `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
```

> This is the core dependency that turns your Spring Boot app into an API Gateway.

No controller class needed here. **All the magic happens in `application.properties`.**

---

**application.properties (API Gateway)**
```properties
spring.application.name=apigateway
server.port=8083

# ─────────────────────────────────────────
# Route 0: Forward /products/** → Product Service
# ─────────────────────────────────────────

# Unique name for this route
spring.cloud.gateway.routes[0].id=product-service

# Where to forward: host + port of Product Service
spring.cloud.gateway.routes[0].uri=http://localhost:8082

# Predicate: match any request whose path starts with /products/
spring.cloud.gateway.routes[0].predicates[0]=Path=/products/**

# ─────────────────────────────────────────
# Route 1: Forward /orders/** → Order Service
# ─────────────────────────────────────────

spring.cloud.gateway.routes[1].id=order-service
spring.cloud.gateway.routes[1].uri=http://localhost:8081
spring.cloud.gateway.routes[1].predicates[0]=Path=/orders/**
```

---

## Breaking Down the Config — Very Important

Think of `routes` as an **array of routing rules**. Each route has 3 key parts:

```
┌─────────────────────────────────────────────────────────┐
│  routes[0]   ← Route for Product Service                │
│                                                         │
│  .id          = "product-service"  ← any unique name    │
│  .uri         = http://localhost:8082  ← where to send  │
│  .predicates  = Path=/products/**  ← when to trigger    │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  routes[1]   ← Route for Order Service                  │
│                                                         │
│  .id          = "order-service"                         │
│  .uri         = http://localhost:8081                   │
│  .predicates  = Path=/orders/**                         │
└─────────────────────────────────────────────────────────┘
```

If you had 10 microservices, you'd have `routes[0]` through `routes[9]` — one per service.

---

## What are Predicates?

A **predicate** is a condition/filter that must match for the route to activate. Predicates are also an array — you can have multiple conditions for one route.

```properties
# Predicate 0 — match by path
spring.cloud.gateway.routes[0].predicates[0]=Path=/products/**

# Predicate 1 — match by HTTP method (optional)
spring.cloud.gateway.routes[0].predicates[1]=Method=GET,POST
```

With **both predicates**, the route only triggers when:
- The path starts with `/products/` **AND**
- The method is GET or POST

With **only the path predicate** (as in our example), the route triggers for `/products/**` regardless of whether it's GET, POST, PUT, or DELETE.

> The instructor says: **Path is by far the most commonly used predicate.** Method is used sometimes, but path alone usually covers most real-world cases.

```
┌──────────────────────────────────────────────┐
│            PREDICATES (array)                │
│                                              │
│  predicates[0] = Path=/products/**  (common) │
│  predicates[1] = Method=GET,POST    (optional│
│  predicates[2] = Header=...         (rare)   │
│                                              │
│  ALL predicates must match for route to fire │
└──────────────────────────────────────────────┘
```

---

## Testing It — How the Routing Works in Action

Start all 3 servers. Now from Postman:

```
# Hit the gateway (8083), NOT the individual services

GET http://localhost:8083/products/1
→ Gateway sees: path starts with /products
→ Forwards to: http://localhost:8082/products/1
→ Response: "fetch the product details with id: 1"

GET http://localhost:8083/orders/1
→ Gateway sees: path starts with /orders
→ Forwards to: http://localhost:8081/orders/1
→ Response: "fetch the Order details with id: 1"
```

```
┌──────────┐   /products/1    ┌─────────────┐   /products/1   ┌─────────────────┐
│  Postman │ ───────────────► │ API GATEWAY │ ──────────────► │ Product Service │
│          │                  │   :8083     │                 │     :8082       │
└──────────┘                  └─────────────┘                 └─────────────────┘
                                     │
                /orders/1            │             /orders/1   ┌─────────────────┐
                                     └──────────────────────►  │  Order Service  │
                                                               │     :8081       │
                                                               └─────────────────┘
```

The client is always talking to **8083 only**. The gateway handles the rest silently.

---

## The Routing Benefit — Revisited With Clarity

```
BEFORE (no gateway):
  Order Service merges with Sales Service
  → All 100 clients must update their code to point to new service
  → Huge coordination effort, risky deployments

AFTER (with gateway):
  Order Service merges with Sales Service
  → Only update routes[1].uri in gateway's application.properties
  → Zero changes needed in any client
  → Clients don't even know anything changed
```

---

## Quick Recap of Step 2

| Component | Port | Role |
|---|---|---|
| Product Service | 8082 | Exposes `/products/{id}` |
| Order Service | 8081 | Exposes `/orders/{id}` |
| API Gateway | 8083 | Routes traffic using path predicates |

The routing config is **purely in `application.properties`** — no Java code needed in the gateway for basic routing. Just define the `id`, `uri`, and `predicates` for each route.

---
# Step 3 — Load Balancing with API Gateway + Eureka Service Discovery

---

## The Problem With What We Built So Far

In Step 2, the gateway config had this:

```properties
spring.cloud.gateway.routes[0].uri=http://localhost:8082
spring.cloud.gateway.routes[1].uri=http://localhost:8081
```

These are **hardcoded URIs** — meaning the gateway always forwards to one specific host and port. But in production, you never run just one instance of a service. You run **multiple instances** to handle load and ensure availability.

```
                         ┌─────────────────────────────┐
                         │  Product Service Instance 1 │  :8082
                         └─────────────────────────────┘
Client ──► API Gateway ──►  ??? which one to call ???
                         ┌─────────────────────────────┐
                         │  Product Service Instance 2 │  :8085
                         └─────────────────────────────┘
                         ┌─────────────────────────────┐
                         │  Product Service Instance   │  :8086
                         └─────────────────────────────┘
```

With a hardcoded URI, the gateway can only call Instance 1 — the other two instances just sit idle. That defeats the whole purpose of scaling.

So we need two things:
1. **Service Discovery** — something that keeps track of all running instances and their host/port
2. **Load Balancer** — something that picks one instance smartly from that list

---

## The Full Picture — How It All Fits Together

```
┌──────────┐       ┌─────────────────────────────────────────────┐
│          │       │              API GATEWAY  :8083             │
│  Client  │──────►│         (Client-Side Load Balancing)        │
│(Postman) │       │                    │                        │
└──────────┘       └────────────────────┼────────────────────────┘
                                        │
                          1. "Give me all instances
                              of product-service"
                                        │
                                        ▼
                        ┌───────────────────────────┐
                        │     EUREKA SERVER         │
                        │   (Service Discovery)     │
                        │        :8761              │
                        │                           │
                        │  product-service →        │
                        │    instance1: :8082       │
                        │    instance2: :8085       │
                        │    instance3: :8086       │
                        │                           │
                        │  order-service →          │
                        │    instance1: :8081       │
                        │    instance2: :8087       │
                        └───────────────────────────┘
                                        │
                          2. Returns list of instances
                                        │
                                        ▼
                        ┌───────────────────────────┐
                        │   SPRING CLOUD            │
                        │   LOAD BALANCER           │
                        │                           │
                        │  Picks one instance using │
                        │  load balancing algorithm │
                        └───────────────────────────┘
                                        │
                          3. Forwards to picked instance
                          ┌─────────────┴──────────────┐
                          ▼                            ▼
             ┌─────────────────────┐    ┌─────────────────────┐
             │  Product Service    │    │   Order Service     │
             │  (one of instances) │    │  (one of instances) │
             └─────────────────────┘    └─────────────────────┘
```

> The instructor makes it clear: **API Gateway itself acts as the client-side load balancer** — it uses the Spring Cloud Load Balancer framework internally. It first asks Eureka "who's available?", gets the list, picks one, and forwards the request.

---

## Step-by-Step Implementation

We now update all 4 services — Product, Order, Eureka Server (new), and API Gateway.

---

### Service 1 — Product Service (Updated)

Add the **Eureka Client** dependency to `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

**application.properties**
```properties
server.port=8082
spring.application.name=product-service

# Tell this service where the Eureka Server is running
eureka.client.service-url.defaultZone=http://localhost:8761/eureka
```

**Controller stays exactly the same** — no changes needed here.

```java
@RestController
@RequestMapping("/products")
public class ProductController {

    @GetMapping("/{id}")
    public ResponseEntity<String> getProduct(@PathVariable String id) {
        return ResponseEntity.ok().body("fetch the product details with id: " + id);
    }
}
```

> What happens now: every time a Product Service instance starts up, it **registers itself** with Eureka at `localhost:8761`. Eureka now knows "hey, product-service is available at this host and port."

---

### Service 2 — Order Service (Updated)

Same change — add Eureka Client dependency:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

**application.properties**
```properties
server.port=8081
spring.application.name=order-service

# Tell this service where the Eureka Server is running
eureka.client.service-url.defaultZone=http://localhost:8761/eureka
```

**Controller stays the same:**
```java
@RestController
@RequestMapping("/orders")
public class OrderController {

    @GetMapping("/{id}")
    public ResponseEntity<String> getOrder(@PathVariable String id) {
        return ResponseEntity.ok().body("fetch the Order details with id: " + id);
    }
}
```

---

### Service 3 — Eureka Server (Brand New Service)

Create a new Spring Boot project with the **`Eureka Server`** dependency.

**pom.xml dependency:**
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
```

**application.properties**
```properties
spring.application.name=eureka-server
server.port=8761

# This is a SERVER — it should NOT register itself
# as a client, and should NOT try to fetch registry
# from another Eureka server
eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false
```

**Main Application class** — you must add `@EnableEurekaServer`:

```java
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

> The two `false` flags are important. Since this is the server itself, it doesn't need to register with anyone or fetch a registry — it IS the registry.

---

### Service 4 — API Gateway (Updated — The Key Changes)

Add **both** dependencies — Gateway + Eureka Client:

```xml
<!-- Gateway dependency (already had this) -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>

<!-- Now also register gateway as Eureka client -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

**application.properties — The Critical Change:**

```properties
spring.application.name=apigateway
server.port=8083

# Route 0: Forward /products/** → Product Service
spring.cloud.gateway.routes[0].id=product-service

# ✅ CHANGED: Instead of hardcoded http://localhost:8082
# Now using: lb://product-service
# lb = load balancer — triggers Spring Cloud Load Balancer
# product-service = the name registered in Eureka
spring.cloud.gateway.routes[0].uri=lb://product-service
spring.cloud.gateway.routes[0].predicates[0]=Path=/products/**

# Route 1: Forward /orders/** → Order Service
spring.cloud.gateway.routes[1].id=order-service

# ✅ CHANGED: lb://order-service
spring.cloud.gateway.routes[1].uri=lb://order-service
spring.cloud.gateway.routes[1].predicates[0]=Path=/orders/**

# Tell the gateway where Eureka Server is running
# so it can fetch instance details
eureka.client.service-url.defaultZone=http://localhost:8761/eureka
```

---

## The Most Important Part — Understanding `lb://`

This is the single biggest change in the whole step. Let's break it down:

```
BEFORE:
  uri = http://localhost:8082
  → hardcoded, always goes to one instance
  → no load balancing possible

AFTER:
  uri = lb://product-service
  → lb = load balancer prefix
  → product-service = service name registered in Eureka
  → Gateway now asks Eureka: "give me all instances of product-service"
  → Spring Cloud Load Balancer picks one using Round Robin (default)
  → Gateway forwards to that picked instance
```

```
┌─────────────────────────────────────────────────────────┐
│  lb://product-service  — what happens internally:       │
│                                                         │
│  Step 1: Gateway intercepts request to /products/1      │
│  Step 2: Sees uri = lb://product-service                │
│  Step 3: Calls Eureka → "list all product-service       │
│           instances"                                    │
│  Step 4: Eureka returns:                                │
│           - instance1 → localhost:8082                  │
│           - instance2 → localhost:8085                  │
│           - instance3 → localhost:8086                  │
│  Step 5: Spring Cloud Load Balancer picks one           │
│           (Round Robin by default)                      │
│  Step 6: Gateway forwards request to picked instance    │
└─────────────────────────────────────────────────────────┘
```

> The `spring.application.name` in each service's `application.properties` is what becomes the service name in Eureka. So `product-service` in `lb://product-service` must exactly match `spring.application.name=product-service` in the Product Service config.

---

## All 4 Services — Port Summary

| Service | Port | Role |
|---|---|---|
| Order Service | 8081 | Business logic — orders |
| Product Service | 8082 | Business logic — products |
| API Gateway | 8083 | Single entry point, routes + load balances |
| Eureka Server | 8761 | Service registry — tracks all instances |

---

## How Registration & Discovery Works at Runtime

```
STARTUP SEQUENCE:

1. Eureka Server starts at :8761
   → Registry is empty

2. Product Service starts at :8082
   → Registers itself: "I am product-service at localhost:8082"
   → Eureka registry: { product-service: [localhost:8082] }

3. Another Product Service instance starts at :8085
   → Registers itself: "I am product-service at localhost:8085"
   → Eureka registry: { product-service: [localhost:8082, localhost:8085] }

4. Order Service starts at :8081
   → Registers itself: "I am order-service at localhost:8081"
   → Eureka registry: { product-service: [...], order-service: [localhost:8081] }

5. API Gateway starts at :8083
   → Also registers with Eureka as a client
   → Now ready to query Eureka for instance lists

6. Request comes in: GET localhost:8083/products/1
   → Gateway queries Eureka for product-service instances
   → Gets [localhost:8082, localhost:8085]
   → Load balancer picks localhost:8082 (Round Robin)
   → Forwards request → response back to client
```

---

## Complete Flow Diagram — Everything Together

```
  Postman
    │
    │  GET localhost:8083/products/1
    ▼
┌─────────────────────────────────────┐
│          API GATEWAY :8083          │
│                                     │
│  1. Path matches /products/**  ✅    │
│  2. uri = lb://product-service      │
│  3. Query Eureka for instances ────────────────────────────┐
│  4. Load balancer picks one    ◄───────────────────────────┘
│  5. Forward request            │
└────────────────────────────────┼────┘
          ┌─────────────────┐    │    ┌──────────────────────────────┐
          │ Product Service │◄───┘    │       EUREKA SERVER :8761    │
          │   :8082         │         │                              │
          │  (picked by LB) │         │  product-service:            │
          └─────────────────┘         │    → localhost:8082          │
                                      │    → localhost:8085          │
          ┌─────────────────┐         │  order-service:              │
          │ Product Service │         │    → localhost:8081          │
          │   :8085         │         └──────────────────────────────┘
          │  (idle / next)  │
          └─────────────────┘
```

---

## Quick Recap — What Changed From Step 2 to Step 3

```
STEP 2 (basic routing only):
  gateway uri = http://localhost:8082   ← hardcoded, 1 instance only
  no Eureka, no load balancing

STEP 3 (routing + load balancing):
  gateway uri = lb://product-service   ← dynamic, N instances
  Eureka Server tracks all instances
  Spring Cloud Load Balancer picks one per request
  API Gateway itself acts as the load balancer client
```

---

That covers the **complete Part 1** of the API Gateway series — Routing and Load Balancing, exactly as the instructor taught it, with all the code from the PDF notes.

**Part 2** (which the instructor mentions at the end) will cover Authentication (JWT), Rate Limiting, Circuit Breaker, Retry, and more — all handled at the gateway level. Whenever you're ready to continue with that or have any questions about what we covered, just let me know!