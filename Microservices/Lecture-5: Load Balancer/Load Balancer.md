# Step 1 — What is a Load Balancer & the Two Types

---

## What Problem Does a Load Balancer Solve?

Imagine your Spring Boot service is getting thousands of requests per second. If all of them hit a **single server instance**, that server will eventually get overwhelmed and crash. This is the core problem.

A **Load Balancer** solves this by:
- Distributing incoming traffic across **multiple instances** of the same service
- Making sure **no single server gets overloaded**

---

## The Two Types of Load Balancers

This is where things get interesting. There are **two fundamentally different approaches**:

---

### 1. Server-Side Load Balancer

```
                    ┌─────────────────────────────────────┐
                    │       CENTRALIZED LOAD BALANCER      │
                    │   (Nginx / AWS ELB / separate server)│
                    └──────────────────┬──────────────────┘
                                       │
          ┌────────────────────────────┼────────────────────────────┐
          ▼                            ▼                            ▼
  ┌───────────────┐           ┌───────────────┐           ┌───────────────┐
  │  Service-A    │           │  Service-A    │           │  Service-A    │
  │  Instance 1   │           │  Instance 2   │           │  Instance 3   │
  └───────────────┘           └───────────────┘           └───────────────┘

YOUR CLIENT ──────► LOAD BALANCER ──────► picks one instance ──────► done
(has ZERO load
balancing logic)
```

**How it works:**
- Your client (Spring Boot app) simply sends the request to the **load balancer server**
- The load balancer server decides which instance to forward it to
- Your client has **zero knowledge** of how many instances exist or which one gets picked

**Examples:** Nginx, AWS ELB (Elastic Load Balancer)

> The instructor mentions: *"Server-side load balancer is like a separate microservice sitting in between. We'll cover this when we get to API Gateway and Nginx."*

---

### 2. Client-Side Load Balancer

```
                        ┌──────────────────────┐
                        │   SERVICE DISCOVERY  │
                        │   (Eureka Server)    │
                        └──────────┬───────────┘
                                   │
              1. "Give me all      │  2. Returns list:
                 instances of      │     Instance1: 192.168.1.1:8081
                 Service-A"        │     Instance2: 192.168.1.2:8081
                                   │     Instance3: 192.168.1.3:8081
                                   │
                        ┌──────────▼────────────┐
                        │       CLIENT          │
                        │  (Your Spring Boot)   │
                        │                       │
                        │  ┌─────────────────┐  │
                        │  │ LOAD BALANCING  │  │
                        │  │ LOGIC IS HERE   │  │  3. Client picks
                        │  │ (library/custom)│  │     one instance
                        │  └─────────────────┘  │     itself
                        └──────────┬────────────┘
                                   │
                    4. Sends request directly to
                       the chosen instance
                                   │
          ┌────────────────────────┼────────────────────────────┐
          ▼                        ▼                            ▼
  ┌───────────────┐       ┌───────────────┐           ┌───────────────┐
  │  Service-A    │       │  Service-A    │           │  Service-A    │
  │  Instance 1   │       │  Instance 2   │           │  Instance 3   │
  └───────────────┘       └───────────────┘           └───────────────┘
```

**How it works:**
- Your client **itself** contains the load balancing logic (either via a library or custom code)
- It first asks Service Discovery: *"Hey, give me all instances of Service-A"*
- Then it **picks one instance on its own** using a load balancing algorithm
- Then it sends the request **directly** to that instance — no middleman

**Examples:** Spring Cloud Load Balancer, Netflix Ribbon (now deprecated), Istio with Sidecar

---

## Side-by-Side Comparison

```
┌─────────────────────┬──────────────────────────┬──────────────────────────┐
│                     │   SERVER-SIDE LB         │   CLIENT-SIDE LB         │
├─────────────────────┼──────────────────────────┼──────────────────────────┤
│ Where is the logic? │ Separate dedicated server│ Inside the client itself │
│ Client awareness    │ Zero — just calls LB     │ Full — client decides    │
│ Service Discovery   │ Not needed by client     │ Required (prerequisite!) │
│ Examples            │ Nginx, AWS ELB           │ Spring Cloud LB, Istio   │
│ When covered        │ API Gateway topic        │ THIS lecture             │
└─────────────────────┴──────────────────────────┴──────────────────────────┘
```

---

## Important Note on Prerequisites

> The instructor is very clear here: **Service Discovery is a hard prerequisite** for Client-Side Load Balancing.

Why? Because in client-side load balancing, the **client needs to know all available instances** of a service before it can pick one. The only way to get that list is from a **Service Discovery server** (like Eureka).

Without Service Discovery → you don't have the list → you can't do client-side load balancing.

---
# Step 2 — The Problem Without a Proper Load Balancer

---

## What Were We Doing Before?

In the Service Discovery lecture (previous video), after setting up Eureka, the instructor showed how to call another service using `RestTemplate`. Let's look at that code carefully — because **this is the "bad" approach** that we are going to fix today.

---

## The Old Code (Manual Approach)

**The Controller:**
```java
@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    DiscoveryClient discoveryClient;   // used to fetch instances from Eureka

    @Autowired
    RestTemplate restTemplate;

    @GetMapping("/{id}")
    public void callProductAPI(@PathVariable String id) {

        // Step 1: Ask Eureka — "give me all instances of product-service"
        List<ServiceInstance> instances =
                discoveryClient.getInstances("product-service");

        // Step 2: Pick one instance — always picking index 0 (first one!)
        URI uri = instances.get(0).getUri();

        // Step 3: Manually build the URL and call it
        String response = restTemplate.getForObject(
                uri + "/products/" + id,
                String.class
        );

        System.out.println("Response from Product api call is: " + response);
    }
}
```

**The Config:**
```java
@Configuration
public class Config {

    @Bean
    public RestTemplate restTemplateObj() {
        return new RestTemplate();   // plain RestTemplate, no load balancing
    }
}
```

---

## Let's Understand What's Happening Here — Step by Step

```
┌──────────────────────────────────────────────────────────────────┐
│                     ORDER SERVICE (Client)                       │
│                                                                  │
│   1. DiscoveryClient.getInstances("product-service")             │
│          │                                                       │
│          ▼                                                       │
│   ┌─────────────────────┐                                        │
│   │   EUREKA SERVER     │  ◄──── returns list of all instances   │
│   └─────────────────────┘                                        │
│          │                                                       │
│          ▼                                                       │
│   instances = [                                                  │
│     Instance 0 → 192.168.1.1:8081   ◄──── always picks this one  │
│     Instance 1 → 192.168.1.2:8081   ✗ never picked               │
│     Instance 2 → 192.168.1.3:8081   ✗ never picked               │
│   ]                                                              │
│                                                                  │
│   2. instances.get(0)  ◄──── THIS is the problem                 │
│                                                                  │
│   3. restTemplate.getForObject(uri + "/products/" + id)          │
└──────────────────────────────────────────────────────────────────┘
```

---

## What Are the Problems With This Approach?

### Problem 1: Fake Load Balancing — Always Picks Instance 0
```
┌──────────────────────────────────────────────────────┐
│  instances.get(0)  ←── hardcoded index               │
│                                                      │
│  No matter how many instances are running...         │
│  Instance 0 → gets 100% of the traffic ← OVERLOADED  │
│  Instance 1 → gets 0% of the traffic   ← IDLE        │
│  Instance 2 → gets 0% of the traffic   ← IDLE        │
│                                                      │
│  This completely defeats the purpose of having       │
│  multiple instances!                                 │
└──────────────────────────────────────────────────────┘
```

### Problem 2: You're Writing Load Balancing Logic Yourself — In the Wrong Place
The controller is not the place for this kind of infrastructure logic. You are mixing **business logic** with **infrastructure concern** (which instance to call). This makes the code messy and hard to maintain.

### Problem 3: Every Developer Has to Repeat This
Every service that calls `product-service` has to write this same boilerplate — fetch instances, pick one, build URL manually. That's a lot of repeated, fragile code.

### Problem 4: No Real Algorithm
`get(0)` is not an algorithm. In a real system you want Round Robin, Random, Least Connection, etc. You'd have to write all of that yourself.

---

## What We Actually Need

```
┌─────────────────────────────────────────────────────────┐
│  What we WANT:                                          │
│                                                         │
│  restTemplate.getForObject(                             │
│      "http://product-service/products/" + id,           │
│       String.class                                      │
│  );                                                     │
│                                                         │
│  → No DiscoveryClient in controller                     │
│  → No manual instance picking                           │
│  → No hardcoded host:port                               │
│  → Just use the SERVICE NAME                            │
│  → Framework handles everything else                    │
└─────────────────────────────────────────────────────────┘
```

This is exactly what **Spring Cloud Load Balancer** gives us — and that's what we cover from Step 3 onwards.

---

## Quick Summary of the Gap

```
┌────────────────────┬─────────────────────────┬──────────────────────────┐
│                    │  OLD APPROACH           │  WHAT WE NEED            │
├────────────────────┼─────────────────────────┼──────────────────────────┤
│ Instance fetching  │ Manual (DiscoveryClient)│ Automatic (interceptor)  │
│ Instance picking   │ Hardcoded get(0)        │ Algorithm (Round Robin)  │
│ URL building       │ Manual (host + port)    │ Just use service name    │
│ Where logic lives  │ Inside controller       │ Inside framework         │
│ Reusability        │ Copy-paste everywhere   │ One annotation does it   │
└────────────────────┴─────────────────────────┴──────────────────────────┘
```

---
# Step 3 — Spring Cloud Load Balancer: How it Works Internally

---

## The Fix — What Changes in the Code

Before jumping into internals, let's first see what actually changes in the code when we bring in Spring Cloud Load Balancer.

### Step 1: Add the Dependency
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
```

### Step 2: Annotate the RestTemplate Bean with `@LoadBalanced`
```java
@Configuration
public class Config {

    @Bean
    @LoadBalanced   // ← this single annotation does ALL the magic
    public RestTemplate restTemplateObj() {
        return new RestTemplate();
    }
}
```

### Step 3: Use Service Name in URL Instead of Host:Port
```java
// application.properties
server.port=8081
spring.application.name=order-service
eureka.client.service-url.defaultZone=http://localhost:8761/eureka

product.service.baseurl=http://product-service   // ← just the service name!
```

```java
@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    RestTemplate restTemplate;          // ← no more DiscoveryClient here!

    @Value("${product.service.baseurl}")
    String productBaseURL;

    @GetMapping("/{id}")
    public void callProductAPI(@PathVariable String id) {

        String response = restTemplate.getForObject(
                productBaseURL + "/products/" + id,   // http://product-service/products/123
                String.class
        );

        System.out.println("Response from Product api call is: " + response);
    }
}
```

---

## Look at What Disappeared

```
┌─────────────────────────────────────────────────────────────────┐
│  BEFORE                          │  AFTER                        │
│                                  │                               │
│  @Autowired                      │  (gone!)                      │
│  DiscoveryClient discoveryClient │                               │
│                                  │                               │
│  List<ServiceInstance> instances │  (gone!)                      │
│  = discoveryClient               │                               │
│    .getInstances("product-svc")  │                               │
│                                  │                               │
│  URI uri = instances.get(0)      │  (gone!)                      │
│              .getUri()           │                               │
│                                  │                               │
│  uri + "/products/" + id         │  "http://product-service"     │
│  (dynamic host:port)             │   + "/products/" + id         │
│                                  │  (just the name!)             │
└─────────────────────────────────────────────────────────────────┘
```

Three lines of messy infrastructure code → completely gone. Just one annotation `@LoadBalanced` on the `RestTemplate` bean. But how? Let's go deep.

---

## What Does `@LoadBalanced` Actually Do Internally?

This annotation tells the Spring framework:

> *"Hey, whenever this RestTemplate makes a request — intercept it. Don't send it directly. First call Service Discovery, get instances, apply a load balancing algorithm, pick one instance, then send the request."*

The class that does this interception is called **`LoadBalancerInterceptor`**.

---

## The Full Internal Flow

```
┌───────────────────────────────────────────────────────────────────────────┐
│                                                                           │
│   restTemplate.getForObject("http://product-service/products/123")        │
│                    │                                                      │
│                    ▼                                                      │
│         RestTemplate.execute()  ← this is where interception happens      │
│                    │                                                      │
│                    ▼                                                      │
│  ┌─────────────────────────────────────────────┐                          │
│  │         LoadBalancerInterceptor              │  ← gets invoked FIRST   │
│  │                                              │    before actual call   │
│  │  Step 1:                                     │                         │
│  │  loadBalancerClientFactory                   │                         │
│  │    .getInstance(serviceId)                   │                         │
│  │         │                                    │                         │
│  │         ▼                                    │                         │
│  │   Picks the Load Balancing                   │                         │
│  │   Algorithm for this serviceId               │                         │
│  │   (default = RoundRobin)                     │                         │
│  │                                              │                         │
│  │  Step 2:                                     │                         │
│  │  loadBalancer.choose(request)                │                         │
│  │         │                                    │                         │
│  │         ▼                                    │                         │
│  │   Internally calls Service Discovery         │                         │
│  │   Gets all instances of "product-service"    │                         │
│  │   Applies the algorithm → picks ONE instance │                         │
│  └──────────────────┬──────────────────────────┘                          │
│                     │                                                     │
│                     ▼                                                     │
│         Replaces "product-service" in URL                                 │
│         with actual host:port                                             │
│         e.g. http://192.168.1.2:8081/products/123                         │
│                     │                                                     │
│                     ▼                                                     │
│         Actual HTTP call goes out to that instance                        │
│                                                                           │
└───────────────────────────────────────────────────────────────────────────┘
```

---

## The Most Important Concept — ServiceId

> The instructor says: *"I'm pretty much sure you will face a lot of challenge because of this ServiceId. A lot of engineers get stuck here. Understand this properly."*

### What is a ServiceId?
A **ServiceId** is simply the **name of the service** you are trying to call — the same name registered in Eureka.

```
# In product-service's application.properties:
spring.application.name=product-service   ← this is the ServiceId

# In order-service's application.properties:
product.service.baseurl=http://product-service   ← same name used here
```

### Why Does ServiceId Matter So Much?

Here is the key rule:

> **Every load balancing algorithm is attached to a specific ServiceId.**

This means it's never just "I'm using Round Robin". It's always:

> *"I'm using Round Robin **for product-service**"*
> *"I'm using Random **for order-service**"*

```
┌───────────────────────────────────────────────────────────────┐
│              YOUR ORDER SERVICE APPLICATION                   │
│                                                               │
│   Calls multiple downstream services:                         │
│                                                               │
│   "product-service"  →  attached to  →  RoundRobin algo       │
│   "sales-service"    →  attached to  →  Random algo           │
│   "xyz-service"      →  attached to  →  Custom algo           │
│                                                               │
│   Each service gets its OWN algorithm.                        │
│   It is NOT one algorithm for all services.                   │
└───────────────────────────────────────────────────────────────┘
```

This concept becomes critical when you want to **override the default algorithm** — which we'll cover in Step 4.

---

## What Algorithms Does Spring Cloud Load Balancer Support?

```
┌───────────────────────────────────────────────────────────────┐
│           SPRING CLOUD LOAD BALANCER CLASS HIERARCHY          │
│                                                               │
│              <<interface>>                                    │
│           ReactiveLoadBalancer                                │
│                    │                                          │
│                    ▼                                          │
│              <<interface>>                                    │
│           ReactorLoadBalancer                                 │
│                    │                                          │
│                    ▼                                          │
│              <<interface>>                                    │
│      ReactorServiceInstanceLoadBalancer                       │
│                /          \                                   │
│               /            \                                  │
│  ┌─────────────────┐   ┌──────────────────┐                   │
│  │ RoundRobin      │   │ Random           │                   │
│  │ LoadBalancer    │   │ LoadBalancer     │                   │
│  │  choose()       │   │  choose()        │                   │
│  └─────────────────┘   └──────────────────┘                   │
│                                                               │
│  Only 2 built-in algorithms!                                  │
│                                                               │
│  Default = RoundRobinLoadBalancer                             │
│                                                               │
│  For Weighted, Least Connection etc →                         │
│  write a Custom implementation (Step 6)                       │
│  OR use Istio with Sidecar                                    │
└───────────────────────────────────────────────────────────────┘
```

> The instructor notes: *"Netflix Ribbon also used to be a client-side load balancer, but it is now deprecated. Spring Cloud Load Balancer is the recommended one."*

---

## Putting It All Together — The Complete Picture

```
┌──────────────────────────────────────────────────────────────────────┐
│                        ORDER SERVICE                                 │
│                                                                      │
│  restTemplate.getForObject("http://product-service/products/123")    │
│                    │                                                 │
│                    ▼                                                 │
│         ┌──────────────────────┐                                     │
│         │  @LoadBalanced       │  ← tells framework to attach        │
│         │  RestTemplate        │    LoadBalancerInterceptor          │
│         └──────────┬───────────┘                                     │
│                    │                                                 │
│                    ▼                                                 │
│         ┌──────────────────────┐                                     │
│         │ LoadBalancer         │  1. picks algo for "product-service"│
│         │ Interceptor          │  2. calls Eureka                    │
│         │                      │  3. gets instances                  │
│         │                      │  4. applies algo → picks one        │
│         └──────────┬───────────┘                                     │
│                    │                                                 │
│                    ▼                                                 │
│         ┌──────────────────────┐                                     │
│         │   EUREKA SERVER      │  returns list of product-service    │
│         │  (Service Discovery) │  instances                          │
│         └──────────┬───────────┘                                     │
│                    │                                                 │
│                    ▼                                                 │
│    Resolves "product-service" → 192.168.1.2:8081                     │
│                    │                                                 │
└────────────────────┼─────────────────────────────────────────────────┘
                     │
                     ▼
        ┌────────────────────┐
        │  product-service   │
        │  Instance 2        │  ← actual request lands here
        │  192.168.1.2:8081  │
        └────────────────────┘
```

---

## Quick Recap of Step 3

```
┌──────────────────────────────────────────────────────────────────┐
│  KEY TAKEAWAYS                                                   │
│                                                                  │
│  1. Add spring-cloud-starter-loadbalancer dependency             │
│                                                                  │
│  2. @LoadBalanced on RestTemplate bean →                         │
│     attaches LoadBalancerInterceptor to it                       │
│                                                                  │
│  3. Use service name in URL (http://product-service)             │
│     NOT hardcoded host:port                                      │
│                                                                  │
│  4. LoadBalancerInterceptor does 2 things:                       │
│     → picks load balancing algorithm for that ServiceId          │
│     → calls Service Discovery, gets instances, picks one         │
│                                                                  │
│  5. ServiceId = application name registered in Eureka            │
│                                                                  │
│  6. Each algorithm is tied to a specific ServiceId               │
│     NOT global across all services                               │
│                                                                  │
│  7. Default algorithm = RoundRobin                               │
│     Only 2 built-in: RoundRobin + Random                         │
└──────────────────────────────────────────────────────────────────┘
```

---
# Step 4 — Overriding the Default Algorithm (Per-Service Config)

---

## The Requirement

By default, Spring Cloud Load Balancer uses **RoundRobin** for every service. But what if you want to say:

> *"For `product-service` specifically, I want to use **Random** algorithm instead of RoundRobin."*

This is exactly what we solve in this step.

---

## What Changes in the Code — Overview

```
┌─────────────────────────────────────────────────────────────────┐
│  To override algorithm for a specific service, you need:        │
│                                                                 │
│  1. @LoadBalancerClient annotation on your main app class       │
│     → tells framework: "for THIS service, use THIS config"      │
│                                                                 │
│  2. A separate LoadBalancer config class                        │
│     → defines WHICH algorithm to use                            │
│     → this config behaves DIFFERENTLY from normal @Configuration│
│       (very important — explained below)                        │
└─────────────────────────────────────────────────────────────────┘
```

---

## Step 1 — Annotate Your Main Application Class

```java
@SpringBootApplication
@EnableFeignClients
@LoadBalancerClient(                                    // ← new annotation
    name = "product-service",                           // ← for THIS service
    configuration = LoadBalancerProductClientConfig.class  // ← use THIS config
)
public class OrderserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderserviceApplication.class, args);
    }
}
```

This annotation is saying:

> *"Whenever a request goes to `product-service`, don't use the default config. Use `LoadBalancerProductClientConfig` instead."*

---

## Step 2 — Write the LoadBalancer Config Class

```java
@Configuration
public class LoadBalancerProductClientConfig {

    @Bean
    public ReactorLoadBalancer<ServiceInstance> productClientLoadBalancer(
            LoadBalancerClientFactory factory) {

        return new RandomLoadBalancer(          // ← Random instead of RoundRobin
            factory.getLazyProvider(
                "product-service",              // ← ServiceInstanceListSupplier
                ServiceInstanceListSupplier.class  // for THIS service
            ),
            "product-service"                  // ← attach this algo to THIS serviceId
        );
    }
}
```

Everything else — the Controller, the `@LoadBalanced` RestTemplate, and `application.properties` — **stays exactly the same** as Step 3. No changes there.

---

## The Most Critical Thing — This Config is NOT a Normal Config Class

The instructor is very clear about this. Let's understand it properly.

```
┌─────────────────────────────────────────────────────────────────────┐
│                    TWO TYPES OF CONFIG CLASSES                      │
│                                                                     │
│  ┌───────────────────────────┐   ┌────────────────────────────────┐ │
│  │   NORMAL @Configuration   │   │  LOADBALANCER @Configuration   │ │
│  │   (e.g. Config.java where │   │  (e.g. LoadBalancerProduct     │ │
│  │   RestTemplate is defined)│   │   ClientConfig.java)           │ │
│  ├───────────────────────────┤   ├────────────────────────────────┤ │
│  │ When does it run?         │   │ When does it run?              │ │
│  │                           │   │                                │ │
│  │ At APPLICATION STARTUP    │   │ At RUNTIME — when you actually │ │
│  │ only. All beans created   │   │ invoke "product-service" for   │ │
│  │ once when app starts.     │   │ the FIRST time.                │ │
│  ├───────────────────────────┤   ├────────────────────────────────┤ │
│  │ Example:                  │   │ Example:                       │ │
│  │ RestTemplate bean is      │   │ When restTemplate hits         │ │
│  │ created at startup        │   │ "http://product-service/..."   │ │
│  │                           │   │ → config invokes               │ │
│  │                           │   │ → algo bean created            │ │
│  │                           │   │ → tied to "product-service"    │ │
│  └───────────────────────────┘   └────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
```

> The instructor says: *"This is very important. The LoadBalancer config is lazy — it does not run at application startup. It runs at runtime when you actually invoke that service."*

Why does this matter? We'll see the full reason in Step 5 (Global Config). But keep this in the back of your mind right now.

---

## Understanding the Two Parameters of RandomLoadBalancer Constructor

```java
return new RandomLoadBalancer(
    factory.getLazyProvider(
        "product-service",
        ServiceInstanceListSupplier.class    // ← Parameter 1
    ),
    "product-service"                        // ← Parameter 2
);
```

Let's understand both:

```
┌─────────────────────────────────────────────────────────────────────┐
│                                                                     │
│  PARAMETER 1: ServiceInstanceListSupplier                           │
│  ─────────────────────────────────────────────────────────────────  │
│  This is the object that internally calls Service Discovery.        │
│                                                                     │
│  factory.getLazyProvider(...) gives a LAZY object —                 │
│  meaning it does NOT call Eureka right now.                         │
│  It will call Eureka only when an actual request comes in           │
│  and an instance needs to be picked.                                │
│                                                                     │
│  For which service does it ask Eureka? → "product-service"          │
│                                                                     │
│                                                                     │
│  PARAMETER 2: serviceId = "product-service"                         │
│  ─────────────────────────────────────────────────────────────────  │
│  This ties the RandomLoadBalancer algorithm specifically            │
│  to "product-service".                                              │
│                                                                     │
│  So Random algo is used ONLY when "product-service" is called.      │
│  It will NOT affect any other service.                              │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## The Full Picture — How It All Connects

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          ORDER SERVICE                                   │
│                                                                          │
│  restTemplate.getForObject("http://product-service/products/123")        │
│                    │                                                     │
│                    ▼                                                     │
│         ┌──────────────────────┐                                         │
│         │ LoadBalancer         │                                         │
│         │ Interceptor          │                                         │
│         └──────────┬───────────┘                                         │
│                    │                                                     │
│                    ▼                                                     │
│    loadBalancerClientFactory.getInstance("product-service")              │
│                    │                                                     │
│                    ▼                                                     │
│    ┌───────────────────────────────────────┐                             │
│    │  Checks: is there a specific config   │                             │
│    │  for "product-service"?               │                             │
│    │                                       │                             │
│    │  YES → @LoadBalancerClient found!     │                             │
│    │  → run LoadBalancerProductClientConfig│  ← runs at runtime (lazy)   │
│    │  → get RandomLoadBalancer bean        │                             │
│    │  → tied to "product-service"          │                             │
│    └───────────────────────────────────────┘                             │
│                    │                                                     │
│                    ▼                                                     │
│    loadBalancer.choose(request)                                          │
│                    │                                                     │
│                    ▼                                                     │
│    ┌───────────────────────────────────────┐                             │
│    │  ServiceInstanceListSupplier          │                             │
│    │  → calls Eureka for "product-service" │                             │
│    │  → gets [Instance1, Instance2, ...]   │                             │
│    │  → RandomLoadBalancer picks one       │                             │
│    │    randomly                           │                             │
│    └───────────────────────────────────────┘                             │
│                    │                                                     │
│                    ▼                                                     │
│    URL resolved → 192.168.1.3:8081/products/123                          │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
                     │
                     ▼
        ┌────────────────────┐
        │  product-service   │
        │  Instance 3        │  ← randomly picked instance
        └────────────────────┘
```

---

## Full Code Summary for Step 4

```
┌──────────────────────────────────────────────────────────────────┐
│                     FILES THAT CHANGE                            │
│                                                                  │
│  1. OrderserviceApplication.java  → add @LoadBalancerClient      │
│  2. LoadBalancerProductClientConfig.java  → NEW file (config)    │
│                                                                  │
│                     FILES THAT STAY THE SAME                     │
│                                                                  │
│  3. OrderController.java  → no change                            │
│  4. Config.java (@LoadBalanced RestTemplate)  → no change        │
│  5. application.properties  → no change                          │
└──────────────────────────────────────────────────────────────────┘
```

```java
// 1. OrderserviceApplication.java
@SpringBootApplication
@EnableFeignClients
@LoadBalancerClient(
    name = "product-service",
    configuration = LoadBalancerProductClientConfig.class
)
public class OrderserviceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderserviceApplication.class, args);
    }
}
```

```java
// 2. LoadBalancerProductClientConfig.java  (NEW FILE)
@Configuration
public class LoadBalancerProductClientConfig {

    @Bean
    public ReactorLoadBalancer<ServiceInstance> productClientLoadBalancer(
            LoadBalancerClientFactory factory) {

        return new RandomLoadBalancer(
            factory.getLazyProvider(
                "product-service",
                ServiceInstanceListSupplier.class
            ),
            "product-service"
        );
    }
}
```

```java
// 3. Config.java — NO CHANGE
@Configuration
public class Config {

    @Bean
    @LoadBalanced
    public RestTemplate restTemplateObj() {
        return new RestTemplate();
    }
}
```

```java
// 4. OrderController.java — NO CHANGE
@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    RestTemplate restTemplate;

    @Value("${product.service.baseurl}")
    String productBaseURL;

    @GetMapping("/{id}")
    public void callProductAPI(@PathVariable String id) {
        String response = restTemplate.getForObject(
                productBaseURL + "/products/" + id,
                String.class
        );
        System.out.println("Response from Product api call is: " + response);
    }
}
```

```properties
# 5. application.properties — NO CHANGE
server.port=8081
spring.application.name=order-service
eureka.client.service-url.defaultZone=http://localhost:8761/eureka
product.service.baseurl=http://product-service
```

---

## Quick Recap of Step 4

```
┌──────────────────────────────────────────────────────────────────┐
│  KEY TAKEAWAYS                                                   │
│                                                                  │
│  1. @LoadBalancerClient(name, configuration)                     │
│     → ties a specific service to a specific config class         │
│                                                                  │
│  2. Inside the config class → return the algorithm bean          │
│     → new RandomLoadBalancer(supplier, serviceId)                │
│                                                                  │
│  3. ServiceInstanceListSupplier                                  │
│     → the object that calls Eureka (lazily)                      │
│     → fetches instances of the given service                     │
│                                                                  │
│  4. serviceId in constructor                                     │
│     → ties this algorithm to ONLY that service                   │
│                                                                  │
│  5. LoadBalancer config is LAZY                                  │
│     → does NOT run at startup                                    │
│     → runs at runtime when service is first invoked              │
│                                                                  │
│  6. Controller, RestTemplate bean, application.properties        │
│     → ZERO changes needed                                        │
└──────────────────────────────────────────────────────────────────┘
```

---
# Step 5 — Global + Per-Service Config Together

---

## The New Requirement

In Step 4 we handled one specific case:

> *"For `product-service` → use Random algorithm"*

But now the requirement is more real-world:

> *"For `product-service` → use **Random***
> *For `sales-service` → use **some other algorithm***
> *For ALL other services → use one **common default** algorithm"*

This is where `@LoadBalancerClients` (plural) comes in — and this is also where **a very common bug** can happen if you don't understand the internals properly.

---

## `@LoadBalancerClient` vs `@LoadBalancerClients`

```
┌──────────────────────────────────────────────────────────────────────┐
│                                                                      │
│  @LoadBalancerClient   (singular — Step 4)                           │
│  ─────────────────────────────────────────                           │
│  Used when you want to override algorithm                            │
│  for ONE specific service only.                                      │
│                                                                      │
│  @LoadBalancerClient(                                                │
│      name = "product-service",                                       │
│      configuration = LoadBalancerProductClientConfig.class           │
│  )                                                                   │
│                                                                      │
│  ─────────────────────────────────────────────────────────────────   │
│                                                                      │
│  @LoadBalancerClients  (plural — THIS step)                          │
│  ─────────────────────────────────────────                           │
│  Used when you want to:                                              │
│  → override algorithm for SOME specific services                     │
│  → AND also define a DEFAULT algorithm for ALL other services        │
│                                                                      │
│  @LoadBalancerClients(                                               │
│      defaultConfiguration = LoadBalancerGlobalConfig.class,          │
│      value = {                                                       │
│          @LoadBalancerClient(                                        │
│              name = "product-service",                               │
│              configuration = LoadBalancerProductClientConfig.class   │
│          )                                                           │
│          // more @LoadBalancerClient entries can go here             │
│      }                                                               │
│  )                                                                   │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Step 1 — Update the Main Application Class

```java
@SpringBootApplication
@EnableFeignClients

/*
 * First, child (specific) client configs will be loaded.
 * Then the default (global) config will be loaded.
 * ORDER MATTERS — specific always before global.
 */
@LoadBalancerClients(
    defaultConfiguration = LoadBalancerGlobalConfig.class,   // ← for ALL services
    value = {
        @LoadBalancerClient(
            name = "product-service",
            configuration = LoadBalancerProductClientConfig.class  // ← specific
        )
        // you can add more @LoadBalancerClient entries here
        // for other services with their own configs
    }
)
public class OrderserviceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderserviceApplication.class, args);
    }
}
```

---

## Step 2 — The Per-Service Config (Same as Step 4)

```java
// LoadBalancerProductClientConfig.java — no change from Step 4
@Configuration
public class LoadBalancerProductClientConfig {

    @Bean
    public ReactorLoadBalancer<ServiceInstance> productClientLoadBalancer(
            LoadBalancerClientFactory factory) {

        return new RandomLoadBalancer(       // ← Random for product-service
            factory.getLazyProvider(
                "product-service",
                ServiceInstanceListSupplier.class
            ),
            "product-service"
        );
    }
}
```

---

## Step 3 — The Global Config (New — This is the Tricky Part)

```java
@Configuration
@ConditionalOnMissingBean(ReactorLoadBalancer.class)   // ← CRITICAL — explained below
public class LoadBalancerGlobalConfig {

    @Bean
    public ReactorLoadBalancer<ServiceInstance> randomLoadBalancer(
            LoadBalancerClientFactory factory,
            Environment environment) {           // ← Environment injected here

        // serviceId is resolved DYNAMICALLY at runtime
        // NOT hardcoded like "product-service"
        String serviceId =
            environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);

        return new RoundRobinLoadBalancer(      // ← RoundRobin for all others
            factory.getLazyProvider(
                serviceId,                      // ← dynamic, not hardcoded
                ServiceInstanceListSupplier.class
            ),
            serviceId                           // ← dynamic serviceId
        );
    }
}
```

---

## The Two Big Questions About Global Config

### Question 1 — Why is `serviceId` dynamic here? Why can't we hardcode it?

In `LoadBalancerProductClientConfig` (per-service), we hardcoded `"product-service"` because that config is **only ever used for product-service**. Simple.

But `LoadBalancerGlobalConfig` is used for **ALL other services** — `sales-service`, `xyz-service`, and any future service you add. We have no idea at the time of writing the code which services will be called. So we can't hardcode it.

```
┌──────────────────────────────────────────────────────────────────────┐
│                                                                      │
│  REMEMBER: LoadBalancer config runs at RUNTIME (lazy)                │
│                                                                      │
│  So when your app calls "sales-service" at runtime:                  │
│                                                                      │
│  → Global config gets invoked                                        │
│  → environment.getProperty(                                          │
│         LoadBalancerClientFactory.PROPERTY_NAME                      │
│    )                                                                 │
│  → this gives us "sales-service" dynamically                         │
│  → RoundRobin algo gets tied to "sales-service"                      │
│                                                                      │
│  When your app calls "xyz-service" at runtime:                       │
│  → same global config invoked again                                  │
│  → environment gives us "xyz-service" dynamically                    │
│  → RoundRobin algo gets tied to "xyz-service"                        │
│                                                                      │
│  During application STARTUP → serviceId would be NULL                │
│  (nothing is being called yet)                                       │
│  That's WHY it MUST be runtime — not startup                         │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

### Question 2 — Why is `@ConditionalOnMissingBean` absolutely required?

This is the **most important interview point** in this entire topic. The instructor warns about this explicitly.

Let's understand the bug that happens WITHOUT it:

```
┌──────────────────────────────────────────────────────────────────────┐
│                      THE DUPLICATE BEAN BUG                          │
│                                                                      │
│  At runtime, your app calls "http://product-service/..."             │
│                                                                      │
│  Spring sees @LoadBalancerClients and loads configs in this order:   │
│                                                                      │
│  STEP 1: Load specific config first                                  │
│  → LoadBalancerProductClientConfig runs                              │
│  → Creates RandomLoadBalancer bean                                   │
│  → Ties it to "product-service" ✓                                    │
│                                                                      │
│  STEP 2: Load global config next                                     │
│  → LoadBalancerGlobalConfig runs                                     │
│  → environment gives "product-service" (because that's what          │
│    is being invoked right now)                                       │
│  → Tries to create RoundRobinLoadBalancer bean                       │
│  → Tries to tie it to "product-service" ✗                            │
│                                                                      │
│  RESULT: Two ReactorLoadBalancer beans for "product-service"!        │
│  → Random (from specific config)                                     │
│  → RoundRobin (from global config)                                   │
│  → RUNTIME EXCEPTION — Spring doesn't know which one to use!         │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

Now with `@ConditionalOnMissingBean`:

```
┌──────────────────────────────────────────────────────────────────────┐
│               WITH @ConditionalOnMissingBean — FIXED                 │
│                                                                      │
│  At runtime, your app calls "http://product-service/..."             │
│                                                                      │
│  STEP 1: Load specific config first                                  │
│  → LoadBalancerProductClientConfig runs                              │
│  → Creates RandomLoadBalancer bean                                   │
│  → Ties it to "product-service" ✓                                    │
│                                                                      │
│  STEP 2: Load global config next                                     │
│  → LoadBalancerGlobalConfig runs                                     │
│  → @ConditionalOnMissingBean checks:                                 │
│    "Is there already a ReactorLoadBalancer bean                      │
│     for product-service?"                                            │
│  → YES there is (Random from Step 1)                                 │
│  → SKIP — don't create another one ✓                                 │
│                                                                      │
│  RESULT: Only ONE bean for "product-service" → Random ✓              │
│                                                                      │
│  At runtime, your app calls "http://sales-service/..."               │
│                                                                      │
│  STEP 1: No specific config for sales-service → skip                 │
│                                                                      │
│  STEP 2: Load global config                                          │
│  → @ConditionalOnMissingBean checks:                                 │
│    "Is there already a ReactorLoadBalancer bean                      │
│     for sales-service?"                                              │
│  → NO there isn't                                                    │
│  → CREATE RoundRobinLoadBalancer bean for "sales-service" ✓          │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## The Loading Order — Very Important

```
┌──────────────────────────────────────────────────────────────────────┐
│                    CONFIG LOADING ORDER                              │
│                                                                      │
│  When a service is invoked at runtime:                               │
│                                                                      │
│  1st → Specific child config                                         │
│         (@LoadBalancerClient with name=...)                          │
│         If found → creates algo bean for that service                │
│                                                                      │
│  2nd → Default global config                                         │
│         (defaultConfiguration=...)                                   │
│         @ConditionalOnMissingBean → only creates bean                │
│         if specific config didn't already create one                 │
│                                                                      │
│  This order is fixed. You cannot change it.                          │
│  Specific always wins over global.                                   │
└──────────────────────────────────────────────────────────────────────┘
```

---

## The Full Picture — All Services Together

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         ORDER SERVICE                                    │
│                                                                          │
│   Calls these downstream services:                                       │
│                                                                          │
│   "product-service"                                                      │
│         │                                                                │
│         ▼                                                                │
│   Specific config found?  YES                                            │
│   → LoadBalancerProductClientConfig                                      │
│   → RandomLoadBalancer tied to "product-service" ✓                       │
│                                                                          │
│   "sales-service"                                                        │
│         │                                                                │
│         ▼                                                                │
│   Specific config found?  NO                                             │
│   → Falls through to LoadBalancerGlobalConfig                            │
│   → @ConditionalOnMissingBean → no bean exists yet                       │
│   → RoundRobinLoadBalancer tied to "sales-service" ✓                     │
│                                                                          │
│   "xyz-service"                                                          │
│         │                                                                │
│         ▼                                                                │
│   Specific config found?  NO                                             │
│   → Falls through to LoadBalancerGlobalConfig                            │
│   → @ConditionalOnMissingBean → no bean exists yet                       │
│   → RoundRobinLoadBalancer tied to "xyz-service" ✓                       │
│                                                                          │
│   RESULT:                                                                │
│   product-service  →  Random        (from specific config)               │
│   sales-service    →  RoundRobin    (from global config)                 │
│   xyz-service      →  RoundRobin    (from global config)                 │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## Full Code Summary for Step 5

```java
// 1. OrderserviceApplication.java
@SpringBootApplication
@EnableFeignClients
@LoadBalancerClients(
    defaultConfiguration = LoadBalancerGlobalConfig.class,
    value = {
        @LoadBalancerClient(
            name = "product-service",
            configuration = LoadBalancerProductClientConfig.class
        )
    }
)
public class OrderserviceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderserviceApplication.class, args);
    }
}
```

```java
// 2. LoadBalancerProductClientConfig.java — specific for product-service
@Configuration
public class LoadBalancerProductClientConfig {

    @Bean
    public ReactorLoadBalancer<ServiceInstance> productClientLoadBalancer(
            LoadBalancerClientFactory factory) {

        return new RandomLoadBalancer(
            factory.getLazyProvider(
                "product-service",
                ServiceInstanceListSupplier.class
            ),
            "product-service"
        );
    }
}
```

```java
// 3. LoadBalancerGlobalConfig.java — default for ALL other services
@Configuration
@ConditionalOnMissingBean(ReactorLoadBalancer.class)   // ← NEVER forget this
public class LoadBalancerGlobalConfig {

    @Bean
    public ReactorLoadBalancer<ServiceInstance> globalLoadBalancer(
            LoadBalancerClientFactory factory,
            Environment environment) {

        // resolved dynamically at runtime — NOT at startup
        String serviceId =
            environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);

        return new RoundRobinLoadBalancer(
            factory.getLazyProvider(
                serviceId,
                ServiceInstanceListSupplier.class
            ),
            serviceId
        );
    }
}
```

---

## Interview Tips From The Instructor

```
┌──────────────────────────────────────────────────────────────────────┐
│                        INTERVIEW TIPS                                │
│                                                                      │
│  Q: What is the difference between @LoadBalancerClient               │
│     and @LoadBalancerClients?                                        │
│  A: Client (singular) = override for one specific service.           │
│     Clients (plural) = override for specific services + set          │
│     a default for all remaining services.                            │
│                                                                      │
│  Q: Why is @ConditionalOnMissingBean needed in global config?        │
│  A: To prevent duplicate ReactorLoadBalancer beans for the same      │
│     serviceId when both a specific config and global config          │
│     are present. Without it → runtime exception.                     │
│                                                                      │
│  Q: Why is serviceId resolved from Environment in global config?     │
│  A: Because global config serves ALL services dynamically.           │
│     We can't hardcode the service name. The LoadBalancer config      │
│     runs at runtime (lazy) — so environment gives us the current     │
│     serviceId being invoked at that moment.                          │
│                                                                      │
│  Q: What is the loading order of configs?                            │
│  A: Specific child configs first → global default config second.     │
│     Specific always wins.                                            │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Quick Recap of Step 5

```
┌──────────────────────────────────────────────────────────────────────┐
│  KEY TAKEAWAYS                                                      │
│                                                                     │
│  1. @LoadBalancerClients = specific configs + one global default    │
│                                                                     │
│  2. Specific config → hardcoded serviceId → specific algo           │
│                                                                     │
│  3. Global config → dynamic serviceId from Environment              │
│     → resolved at runtime when service is actually called           │
│                                                                     │
│  4. Loading order: specific first → global second                   │
│                                                                     │
│  5. @ConditionalOnMissingBean on global config                      │
│     → prevents duplicate bean creation for services                 │
│       that already have a specific config                           │
│     → without this → runtime exception                              │
│                                                                     │
│  6. Global config serviceId is NULL at startup                      │
│     → that is WHY loadbalancer configs are lazy/runtime             │
└──────────────────────────────────────────────────────────────────────┘
```

---
# Step 6 — Writing a Custom Load Balancer From Scratch

---

## The Requirement

Spring Cloud Load Balancer only gives you **two built-in algorithms** — RoundRobin and Random. But what if your use case needs something different like:

- Weighted load balancing
- Least connection
- Sticky session (always same instance for same user)
- Any custom business logic

For all of these → you need to **write your own custom load balancer**.

---

## Where Does Custom Load Balancer Fit in the Class Hierarchy?

This is the first thing to understand before writing any code.

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    CLASS HIERARCHY                                       │
│                                                                          │
│                      <<interface>>                                       │
│                   ReactiveLoadBalancer                                   │
│                          │                                               │
│                          ▼                                               │
│                      <<interface>>                                       │
│                   ReactorLoadBalancer                                    │
│                          │                                               │
│                          ▼                                               │
│                      <<interface>>                                       │
│            ReactorServiceInstanceLoadBalancer                            │
│               /              |               \                           │
│              /               |                \                          │
│  ┌──────────────────┐  ┌──────────────┐  ┌───────────────────┐           │
│  │ RoundRobin       │  │ Random       │  │ MyCustom          │           │
│  │ LoadBalancer     │  │ LoadBalancer │  │ LoadBalancer      │ ← YOU     │
│  │  choose()        │  │  choose()    │  │  choose()         │   WRITE   │
│  └──────────────────┘  └──────────────┘  └───────────────────┘   THIS    │
│                                                                          │
│  Your custom class implements                                            │
│  ReactorServiceInstanceLoadBalancer                                      │
│  and provides its own choose() logic                                     │
└──────────────────────────────────────────────────────────────────────────┘
```

The only thing you need to implement is the **`choose()` method** — this is where your load balancing logic lives.

---

## What Does `choose()` Actually Do?

Before writing code, let's understand what `choose()` is supposed to do:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     THE choose() METHOD                                  │
│                                                                          │
│  Input:  a Request object (the incoming HTTP request)                    │
│  Output: a Mono<Response<ServiceInstance>>                               │
│          → basically: "here is the ONE instance I picked"                │
│                                                                          │
│  What it must do internally:                                             │
│                                                                          │
│  STEP 1: Call ServiceInstanceListSupplier                                │
│          → this internally calls Eureka                                  │
│          → gets back a list of all available instances                   │
│                                                                          │
│  STEP 2: Check if list is empty or null                                  │
│          → if empty → return EmptyResponse()                             │
│                                                                          │
│  STEP 3: Apply YOUR custom algorithm                                     │
│          → pick ONE instance from the list                               │
│          → return DefaultResponse(chosenInstance)                        │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

Let's look at how RoundRobin and Random do it internally to understand the pattern:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                   HOW BUILT-IN ALGORITHMS WORK                           │
│                                                                          │
│  RoundRobinLoadBalancer.choose():                                        │
│  → calls ServiceInstanceListSupplier → gets instances                    │
│  → uses an atomic counter to go through instances one by one             │
│  → picks next one in sequence                                            │
│  → wraps around when it reaches the end                                  │
│                                                                          │
│  RandomLoadBalancer.choose():                                            │
│  → calls ServiceInstanceListSupplier → gets instances                    │
│  → calls Math.random() to pick a random index                            │
│  → returns that instance                                                 │
│                                                                          │
│  YOUR CustomLoadBalancer.choose():                                       │
│  → calls ServiceInstanceListSupplier → gets instances                    │
│  → applies YOUR logic to pick one                                        │
│  → returns that instance                                                 │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## Writing the Custom Load Balancer

```java
// MyCustomLoadBalancer.java
public class MyCustomLoadBalancer implements ReactorServiceInstanceLoadBalancer {

    // This is what calls Eureka internally to get instances
    private final ObjectProvider<ServiceInstanceListSupplier> serviceInstanceSuppliers;

    // The service this load balancer is tied to
    private final String serviceId;

    // Constructor — same pattern as RoundRobin and Random
    public MyCustomLoadBalancer(
            ObjectProvider<ServiceInstanceListSupplier> serviceInstanceSuppliers,
            String serviceId) {

        this.serviceInstanceSuppliers = serviceInstanceSuppliers;
        this.serviceId = serviceId;
    }

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {

        return serviceInstanceSuppliers
            .getIfAvailable()       // get the supplier (calls Eureka lazily)
            .get()                  // get the flux of instance lists
            .next()                 // take the first emission
            .map(instances -> {

                // STEP 1: handle empty instance list
                if (instances == null || instances.isEmpty()) {
                    return new EmptyResponse();
                }

                // STEP 2: YOUR custom algorithm goes here
                // Currently: always picks first instance (demo only)
                // Replace this with your real logic:
                //   → weighted → check instance metadata for weights
                //   → least connection → track active connections
                //   → sticky session → check request headers/cookies
                return new DefaultResponse(instances.get(0));
            });
    }
}
```

---

## Understanding the `choose()` Flow in Detail

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    INSIDE choose() — STEP BY STEP                        │
│                                                                          │
│  serviceInstanceSuppliers                                                │
│       │                                                                  │
│       ▼                                                                  │
│  .getIfAvailable()                                                       │
│  → gets the ServiceInstanceListSupplier object                           │
│  → if not available yet → returns null safely                            │
│       │                                                                  │
│       ▼                                                                  │
│  .get()                                                                  │
│  → calls Eureka for "product-service" instances                          │
│  → returns a Flux (stream) of instance lists                             │
│       │                                                                  │
│       ▼                                                                  │
│  .next()                                                                 │
│  → takes just the first emission from the Flux                           │
│  → gives us a Mono<List<ServiceInstance>>                                │
│       │                                                                  │
│       ▼                                                                  │
│  .map(instances -> { ... })                                              │
│  → instances = [Instance1, Instance2, Instance3, ...]                    │
│       │                                                                  │
│       ├── if empty → return new EmptyResponse()                          │
│       │                                                                  │
│       └── else → apply your algorithm                                    │
│                → return new DefaultResponse(pickedInstance)              │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## Plugging the Custom Load Balancer Into the Config

Now that we have written `MyCustomLoadBalancer`, we plug it into the config class — exactly like we did for Random in Step 4. The only change is `new MyCustomLoadBalancer(...)` instead of `new RandomLoadBalancer(...)`.

```java
// LoadBalancerProductClientConfig.java — updated to use custom LB
@Configuration
public class LoadBalancerProductClientConfig {

    @Bean
    public ReactorLoadBalancer<ServiceInstance> productClientLoadBalancer(
            LoadBalancerClientFactory factory) {

        return new MyCustomLoadBalancer(    // ← swap this line
            factory.getLazyProvider(
                "product-service",
                ServiceInstanceListSupplier.class
            ),
            "product-service"
        );
    }
}
```

Everything else — `@LoadBalancerClient` on main class, `@LoadBalanced` RestTemplate, controller, `application.properties` — **stays exactly the same**.

---

## Side by Side — Built-in vs Custom

```
┌─────────────────────────────────────────────────────────────────────────┐
│                                                                         │
│  // RoundRobinLoadBalancer (built-in)                                   │
│  return new RoundRobinLoadBalancer(                                     │
│      factory.getLazyProvider(                                           │
│          "product-service",                                             │
│          ServiceInstanceListSupplier.class                              │
│      ),                                                                 │
│      "product-service"                                                  │
│  );                                                                     │
│                                                                         │
│  // RandomLoadBalancer (built-in)                                       │
│  return new RandomLoadBalancer(                                         │
│      factory.getLazyProvider(                                           │
│          "product-service",                                             │
│          ServiceInstanceListSupplier.class                              │
│      ),                                                                 │
│      "product-service"                                                  │
│  );                                                                     │
│                                                                         │
│  // MyCustomLoadBalancer (your own)                                     │
│  return new MyCustomLoadBalancer(                                       │
│      factory.getLazyProvider(                                           │
│          "product-service",                                             │
│          ServiceInstanceListSupplier.class                              │
│      ),                                                                 │
│      "product-service"                                                  │
│  );                                                                     │
│                                                                         │
│  The constructor signature is IDENTICAL for all three.                  │
│  The ONLY difference is the class name and the choose() logic.          │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## The Complete Flow With Custom Load Balancer

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         ORDER SERVICE                                   │
│                                                                         │
│  restTemplate.getForObject("http://product-service/products/123")       │
│                    │                                                    │
│                    ▼                                                    │
│         LoadBalancerInterceptor                                         │
│                    │                                                    │
│                    ▼                                                    │
│    loadBalancerClientFactory.getInstance("product-service")             │
│                    │                                                    │
│                    ▼                                                    │
│    Finds @LoadBalancerClient for "product-service"                      │
│    → runs LoadBalancerProductClientConfig                               │
│    → gets MyCustomLoadBalancer bean                                     │
│                    │                                                    │
│                    ▼                                                    │
│    MyCustomLoadBalancer.choose(request)                                 │
│                    │                                                    │
│                    ▼                                                    │
│    serviceInstanceSuppliers.getIfAvailable()                            │
│    → calls Eureka → gets instances                                      │
│    [Instance1: 192.168.1.1:8081]                                        │
│    [Instance2: 192.168.1.2:8081]                                        │
│    [Instance3: 192.168.1.3:8081]                                        │
│                    │                                                    │
│                    ▼                                                    │
│    YOUR algorithm runs on this list                                     │
│    → picks Instance1 (or whichever your logic picks)                    │
│    → returns DefaultResponse(Instance1)                                 │
│                    │                                                    │
│                    ▼                                                    │
│    URL resolved → 192.168.1.1:8081/products/123                         │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
                     │
                     ▼
        ┌────────────────────┐
        │  product-service   │
        │  Instance 1        │  ← your custom logic decided this
        └────────────────────┘
```

---

## Full Code Summary for Step 6

```java
// 1. MyCustomLoadBalancer.java — NEW FILE
public class MyCustomLoadBalancer implements ReactorServiceInstanceLoadBalancer {

    private final ObjectProvider<ServiceInstanceListSupplier> serviceInstanceSuppliers;
    private final String serviceId;

    public MyCustomLoadBalancer(
            ObjectProvider<ServiceInstanceListSupplier> serviceInstanceSuppliers,
            String serviceId) {
        this.serviceInstanceSuppliers = serviceInstanceSuppliers;
        this.serviceId = serviceId;
    }

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        return serviceInstanceSuppliers
            .getIfAvailable()
            .get()
            .next()
            .map(instances -> {
                if (instances == null || instances.isEmpty()) {
                    return new EmptyResponse();
                }
                // Replace with your real algorithm
                return new DefaultResponse(instances.get(0));
            });
    }
}
```

```java
// 2. LoadBalancerProductClientConfig.java — updated
@Configuration
public class LoadBalancerProductClientConfig {

    @Bean
    public ReactorLoadBalancer<ServiceInstance> productClientLoadBalancer(
            LoadBalancerClientFactory factory) {

        return new MyCustomLoadBalancer(
            factory.getLazyProvider(
                "product-service",
                ServiceInstanceListSupplier.class
            ),
            "product-service"
        );
    }
}
```

```java
// 3. OrderserviceApplication.java — no change from Step 4/5
@SpringBootApplication
@EnableFeignClients
@LoadBalancerClient(
    name = "product-service",
    configuration = LoadBalancerProductClientConfig.class
)
public class OrderserviceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderserviceApplication.class, args);
    }
}
```

---

## Interview Tips From The Instructor

```
┌──────────────────────────────────────────────────────────────────────┐
│                        INTERVIEW TIPS                                │
│                                                                      │
│  Q: How do you write a custom load balancer in                       │
│     Spring Cloud Load Balancer?                                      │
│  A: Implement ReactorServiceInstanceLoadBalancer interface,          │
│     override the choose() method, inject                             │
│     ObjectProvider<ServiceInstanceListSupplier> and serviceId        │
│     via constructor, then plug it into a @Configuration class        │
│     and wire it via @LoadBalancerClient annotation.                  │
│                                                                      │
│  Q: What does ServiceInstanceListSupplier do?                        │
│  A: It internally calls Service Discovery (Eureka) to fetch          │
│     all available instances of a given service.                      │
│     It is lazy — it doesn't call Eureka until actually needed.       │
│                                                                      │
│  Q: When would you use a custom load balancer?                       │
│  A: When built-in RoundRobin and Random are not enough.              │
│     Examples: weighted routing, least connections,                   │
│     sticky sessions, canary deployments, or any                      │
│     business-specific routing logic.                                 │
│                                                                      │
│  Q: What do you return from choose() if no instances are found?      │
│  A: new EmptyResponse() — never return null.                         │
│     If an instance is found → new DefaultResponse(instance).         │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Quick Recap of Step 6

```
┌──────────────────────────────────────────────────────────────────────┐
│  KEY TAKEAWAYS                                                      │
│                                                                     │
│  1. Implement ReactorServiceInstanceLoadBalancer                    │
│     → this is the interface to implement for custom LB              │
│                                                                     │
│  2. Constructor takes two things:                                   │
│     → ObjectProvider<ServiceInstanceListSupplier> (calls Eureka)    │
│     → String serviceId (ties algo to this service)                  │
│                                                                     │
│  3. Override choose() method:                                       │
│     → get instances via supplier                                    │
│     → if empty → EmptyResponse()                                    │
│     → else → apply your algo → DefaultResponse(instance)            │
│                                                                     │
│  4. Plug into config class:                                         │
│     → return new MyCustomLoadBalancer(supplier, serviceId)          │
│     → same pattern as RoundRobin and Random                         │
│                                                                     │
│  5. Wire via @LoadBalancerClient — same as Step 4                   │
│     → no changes to controller, RestTemplate, or properties         │
│                                                                     │
│  6. Built-in algorithms (RoundRobin, Random) follow the             │
│     exact same constructor pattern — study them as reference        │
└──────────────────────────────────────────────────────────────────────┘
```

---
# Step 7 — FeignClient + Load Balancer

---

## Quick Context — What is FeignClient?

Before diving in, a quick reminder of what FeignClient is (from the Service Discovery lecture):

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         FEIGN CLIENT                                     │
│                                                                          │
│  RestTemplate way:                                                       │
│  → You manually build the URL                                            │
│  → You manually call restTemplate.getForObject(...)                      │
│  → You handle response mapping yourself                                  │
│                                                                          │
│  FeignClient way:                                                        │
│  → You just declare an interface                                         │
│  → Annotate it with @FeignClient                                         │
│  → Framework generates the implementation for you                        │
│  → You just call it like a normal Java method                            │
│                                                                          │
│  @FeignClient(name = "product-service")                                  │
│  public interface ProductClient {                                        │
│      @GetMapping(value = "/products/{id}")                               │
│      String getProductById(@PathVariable("id") String id);               │
│  }                                                                       │
│                                                                          │
│  → No URL building                                                       │
│  → No restTemplate calls                                                 │
│  → No response mapping                                                   │
│  → Framework handles everything                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## The Key Point — FeignClient Handles Load Balancing Automatically

> The instructor says: *"With FeignClient, load balancing is handled automatically and by the framework. Everything is abstracted from us."*

This means:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                                                                          │
│  With RestTemplate:                                                      │
│  → You add @LoadBalanced on RestTemplate bean                            │
│  → Framework attaches LoadBalancerInterceptor                            │
│  → Interceptor calls Eureka, picks instance, resolves URL                │
│                                                                          │
│  With FeignClient:                                                       │
│  → You don't need @LoadBalanced at all                                   │
│  → FeignClient internally ALREADY uses LoadBalancerInterceptor           │
│  → Same interceptor, same flow — just hidden from you                    │
│                                                                          │
│  The load balancing mechanism is IDENTICAL.                              │
│  FeignClient just abstracts it one level further.                        │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## What You Need — Without Service Discovery vs With Service Discovery

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    FEIGN CLIENT SETUP                                    │
│                                                                          │
│  WITHOUT Service Discovery:                                              │
│  ─────────────────────────                                               │
│  @FeignClient(                                                           │
│      name = "product-service",                                           │
│      url = "${feign.client.product-service.url}"  ← URL required         │
│  )                                                                       │
│  public interface ProductClient {                                        │
│      @GetMapping(value = "/products/{id}")                               │
│      String getProductById(@PathVariable("id") String id);               │
│  }                                                                       │
│                                                                          │
│  → You must provide URL                                                  │
│  → No load balancing possible                                            │
│                                                                          │
│  ─────────────────────────────────────────────────────────────────       │
│                                                                          │
│  WITH Service Discovery + Load Balancer:                                 │
│  ───────────────────────────────────────                                 │
│  @FeignClient(name = "product-service")  ← just the name, no URL         │
│  public interface ProductClient {                                        │
│      @GetMapping(value = "/products/{id}")                               │
│      String getProductById(@PathVariable("id") String id);               │
│  }                                                                       │
│                                                                          │
│  → No URL needed                                                         │
│  → Framework resolves service name via Eureka                            │
│  → Load balancing handled automatically                                  │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## Dependencies Needed for FeignClient + Load Balancer

```xml
<!-- 1. Feign Client dependency -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>

<!-- 2. Load Balancer dependency — needed separately! -->
<!-- FeignClient uses it internally but you still need to add it -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
```

> The instructor specifically points this out: *"We need to provide the Load Balancer dependency too, apart from `spring-cloud-starter-netflix-eureka-client`. FeignClient uses it internally — but you still have to declare it."*

---

## The Full Setup With FeignClient

```java
// 1. ProductClient.java — the FeignClient interface
@FeignClient(name = "product-service")   // ← just service name, no URL
public interface ProductClient {

    @GetMapping(value = "/products/{id}")
    String getProductById(@PathVariable("id") String id);
}
```

```java
// 2. OrderController.java — uses ProductClient directly
@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    ProductClient productClient;    // ← injected directly, no RestTemplate

    @GetMapping("/{id}")
    public void callProductAPI(@PathVariable String id) {

        String response = productClient.getProductById(id);
        System.out.println("Response from Product api call is: " + response);
    }
}
```

```java
// 3. OrderserviceApplication.java
@SpringBootApplication
@EnableFeignClients    // ← enables FeignClient scanning
public class OrderserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderserviceApplication.class, args);
    }
}
```

```properties
# 4. application.properties — same as before
server.port=8081
spring.application.name=order-service
eureka.client.service-url.defaultZone=http://localhost:8761/eureka
```

Notice — **no `@LoadBalanced` annotation anywhere**, **no `RestTemplate` bean**, **no URL in properties**. FeignClient handles all of it.

---

## Internal Flow — FeignClient With Load Balancer

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         ORDER SERVICE                                   │
│                                                                         │
│  productClient.getProductById("123")                                    │
│                    │                                                    │
│                    ▼                                                    │
│  ┌──────────────────────────────────────┐                               │
│  │   FeignClient Framework              │                               │
│  │   (generated implementation)         │                               │
│  │                                      │                               │
│  │   Internally builds:                 │                               │
│  │   "http://product-service/           │                               │
│  │    products/123"                     │                               │
│  └──────────────────┬───────────────────┘                               │
│                     │                                                   │
│                     ▼                                                   │
│  ┌──────────────────────────────────────┐                               │
│  │   LoadBalancerInterceptor            │  ← SAME interceptor as        │
│  │   (used internally by Feign)         │    RestTemplate               │
│  │                                      │                               │
│  │   1. picks algo for "product-service"│                               │
│  │   2. calls Eureka → gets instances  │                                │
│  │   3. applies algo → picks one        │                               │
│  └──────────────────┬───────────────────┘                               │
│                     │                                                   │
│                     ▼                                                   │
│  Resolves "product-service" → 192.168.1.2:8081                          │
│                     │                                                   │
└─────────────────────────────────────────────────────────────────────────┘
                      │
                      ▼
         ┌────────────────────┐
         │  product-service   │
         │  Instance 2        │  ← load balancer picked this
         └────────────────────┘
```

---

## Overriding the Algorithm With FeignClient

Here is the important point:

> *"Even though FeignClient abstracts everything — if you want to change the load balancing algorithm, the approach is EXACTLY the same as RestTemplate."*

```
┌─────────────────────────────────────────────────────────────────────────┐
│                                                                          │
│  To override algorithm for FeignClient:                                  │
│                                                                          │
│  SAME @LoadBalancerClient annotation on main class                       │
│  SAME LoadBalancerProductClientConfig class                              │
│  SAME algorithm bean definition inside config                            │
│                                                                          │
│  ZERO difference from RestTemplate approach                              │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

```java
// OrderserviceApplication.java — add @LoadBalancerClient
@SpringBootApplication
@EnableFeignClients
@LoadBalancerClient(                                      // ← same as RestTemplate
    name = "product-service",
    configuration = LoadBalancerProductClientConfig.class
)
public class OrderserviceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderserviceApplication.class, args);
    }
}
```

```java
// LoadBalancerProductClientConfig.java — exactly same as RestTemplate
@Configuration
public class LoadBalancerProductClientConfig {

    @Bean
    public ReactorLoadBalancer<ServiceInstance> productClientLoadBalancer(
            LoadBalancerClientFactory factory) {

        return new RandomLoadBalancer(       // ← or MyCustomLoadBalancer
            factory.getLazyProvider(
                "product-service",
                ServiceInstanceListSupplier.class
            ),
            "product-service"
        );
    }
}
```

---

## RestTemplate vs FeignClient — Complete Comparison

```
┌──────────────────────────────┬──────────────────────────────────────────┐
│                              │                                          │
│        REST TEMPLATE         │           FEIGN CLIENT                   │
│                              │                                          │
├──────────────────────────────┼──────────────────────────────────────────┤
│ Need @LoadBalanced?          │ No — handled internally                  │
│ Yes — on RestTemplate bean   │                                          │
├──────────────────────────────┼──────────────────────────────────────────┤
│ Need RestTemplate bean?      │ No — just inject ProductClient           │
│ Yes — in @Configuration      │                                          │
├──────────────────────────────┼──────────────────────────────────────────┤
│ URL in properties?           │ No — just service name in @FeignClient   │
│ Yes — http://product-service │                                          │
├──────────────────────────────┼──────────────────────────────────────────┤
│ LoadBalancerInterceptor?     │ Same interceptor used internally         │
│ Explicitly attached via      │ FeignClient attaches it automatically    │
│ @LoadBalanced                │                                          │
├──────────────────────────────┼──────────────────────────────────────────┤
│ Override algorithm?          │ IDENTICAL approach                       │
│ @LoadBalancerClient +        │ @LoadBalancerClient +                    │
│ config class                 │ same config class                        │
├──────────────────────────────┼──────────────────────────────────────────┤
│ Custom load balancer?        │ IDENTICAL approach                       │
│ Implement                    │ Same — config class just returns         │
│ ReactorServiceInstance       │ MyCustomLoadBalancer                     │
│ LoadBalancer                 │                                          │
├──────────────────────────────┼──────────────────────────────────────────┤
│ Load balancer dependency?    │ Yes — still needed explicitly            │
│ Yes                          │ spring-cloud-starter-loadbalancer        │
└──────────────────────────────┴──────────────────────────────────────────┘
```

---

## The Big Picture — Everything Together

```
┌────────────────────────────────────────────────────────────────────────┐
│                 COMPLETE MENTAL MODEL                                  │
│                                                                        │
│  CLIENT SIDE LOAD BALANCING — TWO WAYS TO USE                          │
│                                                                        │
│  ┌────────────────────────────────────────────────────────────────┐    │
│  │  WAY 1: RestTemplate                                           │    │
│  │                                                                │    │
│  │  @LoadBalanced RestTemplate bean                               │    │
│  │       ↓                                                        │    │
│  │  LoadBalancerInterceptor (explicitly attached)                 │    │
│  │       ↓                                                        │    │
│  │  Eureka → instances → algo → pick one → resolve URL            │    │
│  └────────────────────────────────────────────────────────────────┘    │
│                                                                        │
│  ┌────────────────────────────────────────────────────────────────┐    │
│  │  WAY 2: FeignClient                                            │    │
│  │                                                                │    │
│  │  @FeignClient(name = "product-service")                        │    │
│  │       ↓                                                        │    │
│  │  LoadBalancerInterceptor (auto-attached by Feign)              │    │
│  │       ↓                                                        │    │
│  │  Eureka → instances → algo → pick one → resolve URL            │    │
│  └────────────────────────────────────────────────────────────────┘    │
│                                                                        │
│  CHANGING THE ALGORITHM — SAME FOR BOTH:                               │
│                                                                        │
│  @LoadBalancerClient / @LoadBalancerClients                            │
│       ↓                                                                │
│  LoadBalancerXxxConfig class                                           │
│       ↓                                                                │
│  return new RandomLoadBalancer / MyCustomLoadBalancer(...)             │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘
```

---

## Interview Tips From The Instructor

```
┌──────────────────────────────────────────────────────────────────────┐
│                        INTERVIEW TIPS                                │
│                                                                      │
│  Q: Does FeignClient support load balancing?                         │
│  A: Yes — automatically. It internally uses the same                 │
│     LoadBalancerInterceptor as RestTemplate. You don't need          │
│     @LoadBalanced annotation — it's already wired in.                │
│                                                                      │
│  Q: How do you change the load balancing algorithm with FeignClient? │
│  A: Exactly the same way as RestTemplate. Use @LoadBalancerClient    │
│     on the main class pointing to a config class that returns        │
│     the desired algorithm bean (Random, Custom etc.)                 │
│                                                                      │
│  Q: What dependencies are needed for FeignClient + Load Balancing?   │
│  A: Two — spring-cloud-starter-openfeign AND                         │
│     spring-cloud-starter-loadbalancer. Even though Feign uses        │
│     load balancer internally, you must declare both explicitly.      │
│                                                                      │
│  Q: What is the difference between RestTemplate and FeignClient      │
│     in terms of load balancing?                                      │
│  A: The mechanism is identical — same interceptor, same Eureka       │
│     call, same algorithm selection. The only difference is that      │
│     RestTemplate requires you to explicitly attach it via            │
│     @LoadBalanced, while FeignClient does it automatically.          │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Final Recap — The Entire Lecture in One Diagram

```
┌──────────────────────────────────────────────────────────────────────────┐
│              CLIENT SIDE LOAD BALANCER — FULL PICTURE                    │
│                                                                          │
│  CONCEPT                                                                 │
│  ────────                                                                │
│  Server-side LB → centralized (Nginx, ELB) — covered in API Gateway      │
│  Client-side LB → logic inside client — THIS lecture                     │
│  Prerequisite → Service Discovery (Eureka)                               │
│                                                                          │
│  SPRING CLOUD LOAD BALANCER                                              │
│  ──────────────────────────                                              │
│  Add dependency: spring-cloud-starter-loadbalancer                       │
│  Default algorithm: RoundRobin                                           │
│  Built-in algorithms: RoundRobin + Random only                           │
│  Key class: LoadBalancerInterceptor                                      │
│  Key concept: each algo is tied to a specific ServiceId                  │
│                                                                          │
│  WITH RESTTEMPLATE                                                       │
│  ─────────────────                                                       │
│  @LoadBalanced on RestTemplate bean → interceptor attached               │
│  Use service name in URL → http://product-service/...                    │
│                                                                          │
│  WITH FEIGN CLIENT                                                       │
│  ─────────────────                                                       │
│  @FeignClient(name="product-service") → interceptor auto-attached        │
│  No @LoadBalanced needed                                                 │
│                                                                          │
│  OVERRIDING ALGORITHM (SAME FOR BOTH)                                    │
│  ─────────────────────────────────────                                   │
│  Per-service → @LoadBalancerClient + config class                        │
│  Global+specific → @LoadBalancerClients + @ConditionalOnMissingBean      │
│  Custom algo → implement ReactorServiceInstanceLoadBalancer              │
│                override choose() method                                  │
│                                                                          │
│  CRITICAL RULES                                                          │
│  ──────────────                                                          │
│  1. LoadBalancer config is LAZY — runs at runtime not startup            │
│  2. ServiceId ties algo to specific service — not global                 │
│  3. Specific config loads before global config                           │
│  4. @ConditionalOnMissingBean prevents duplicate bean crash              │
│  5. Global config uses dynamic serviceId from Environment                │
└──────────────────────────────────────────────────────────────────────────┘
```

---

That wraps up the complete lecture on **Client Side Load Balancer** with Spring Cloud! You now have a full picture — from the concept, to internal mechanics, to per-service config, to global config, to custom algorithms, and finally to FeignClient. These notes cover everything the instructor taught, along with all the tips that matter for interviews and real-world implementation.