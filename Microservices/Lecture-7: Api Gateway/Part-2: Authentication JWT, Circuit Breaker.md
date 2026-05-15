# Part 1 — Filters in API Gateway: The Big Picture

---

## Why Do We Need Filters?

In the previous lecture (Part 1 of API Gateway), Shreyansh covered **routing** and **load balancing**. But an API Gateway does much more than just forwarding requests. It is the **single entry point** for all your microservices, which means it is the perfect place to handle cross-cutting concerns like:

- Authentication (JWT)
- Rate Limiting
- Circuit Breaker / Retry
- Request/Response Transformation
- Monitoring and Logging

But how does the API Gateway actually intercept a request and apply all these things **before** forwarding it to the right microservice? The answer is **Filters**.

---

## What is a Filter?

A filter can **intercept and modify** the HTTP request and response as they pass through the gateway.

Think of it like a security checkpoint at an airport. Before you board the plane (reach the microservice), you pass through multiple checkpoints (filters) — ID check, baggage scan, boarding pass scan. Each checkpoint can either let you through or stop you. And when you return (response), you again pass through some checkpoints.

```
                         FILTER CHAIN
                         
CLIENT  -->  [Filter 1]  -->  [Filter 2]  -->  [Filter 3]  -->  MICROSERVICE
        <--  [Filter 1]  <--  [Filter 2]  <--  [Filter 3]  <--
        
        Each filter has:
        - PRE logic  : runs before passing request forward
        - POST logic : runs after getting response back
```

---

## Two Types of Filters

```
                            FILTERS
                               |
               +---------------+---------------+
               |                               |
        GLOBAL FILTERS                 ROUTE-SPECIFIC FILTERS
        (GlobalFilter)                 (GatewayFilters)
               |                               |
  Applied to EVERY request          Applied only to requests
  that passes through gateway       matching a PARTICULAR route
               |                               |
  Good for:                         Good for:
  - Authentication (JWT)            - Rate Limiting
  - Logging                         - Circuit Breaker
                                    - Retry
                                    - Header manipulation
```

The key difference:

| | Global Filter | Route-Specific Filter |
|---|---|---|
| Scope | All requests | Only matching route |
| Interface | `GlobalFilter` | `AbstractGatewayFilterFactory` |
| Use case | Auth, Logging | Retry, Circuit Breaker, Rate Limiting |
| Configured in | Java class | `application.properties` |

---

## The Complete Request Lifecycle in API Gateway

This is the most important diagram of this lecture. Read it carefully.

```
CLIENT
  |
  | GET localhost:8083/products/1
  v
+------------------+
| DispatcherHandler|   <-- Entry point of Spring Cloud Gateway
+------------------+
  |
  v
+----------------------+
| RouteMappingHandler  |   <-- Looks at the path (/products/**)
|                      |       Finds matching route (route[0])
|                      |       Creates a Route object
+----------------------+
  |
  v
+=============================================+
|           GLOBAL FILTER (Pre Logic)         |
|  e.g. JwtAuthGlobalFilter (order = -1)      |
|  - Runs FIRST because lowest order value    |
|  - Checks JWT token                         |
|  - If valid: calls next filter in chain     |
+=============================================+
  |
  v
+=============================================+
|       ROUTE-SPECIFIC FILTER (Pre Logic)     |
|  e.g. Retry, CircuitBreaker, AddHeader      |
|  - Applied only to /products/** route       |
|  - Runs its pre-filter logic                |
|  - Calls next filter in chain               |
+=============================================+
  |
  v
+=============================================+
|   GLOBAL FILTER: RouteToRequestUrlFilter    |
|   order = 10,000  (runs late intentionally) |
|   - Converts lb://product-service           |
|     --> calls Eureka --> gets actual host   |
|     --> e.g. http://localhost:8082          |
+=============================================+
  |
  v
+=============================================+
|   GLOBAL FILTER: NettyRoutingFilter         |
|   order = LOWEST_PRECEDENCE (runs last)     |
|   - Actually INVOKES the microservice       |
|   - Makes the real HTTP call                |
+=============================================+
  |
  | (Microservice processes and returns response)
  v
+=============================================+
|       ROUTE-SPECIFIC FILTER (Post Logic)    |
|   - Response comes back                     |
|   - Post logic of route-specific filter     |
|     runs here (reverse direction)           |
+=============================================+
  |
  v
+=============================================+
|        GLOBAL FILTER (Post Logic)           |
|   - Post logic of your global filter runs   |
+=============================================+
  |
  v
+=============================================+
|  GLOBAL FILTER: NettyWriteResponseFilter    |
|  order = -1  (post logic runs at the END)   |
|  - Sends the final response back to client  |
+=============================================+
  |
  v
CLIENT receives response
```

---

## Pre Logic and Post Logic — How It Works in Code

Every filter has a **pre** part and a **post** part. Here is the mental model:

```
filter(exchange, chain) {

    // ---- PRE LOGIC ----
    // Everything written BEFORE calling chain.filter()
    // e.g. validate JWT, log request, add header
    
    return chain.filter(exchange)   // <-- pass to next filter
    
           .then(Mono.fromRunnable(() -> {
               // ---- POST LOGIC ----
               // Runs AFTER the microservice has responded
               // e.g. log response, modify response header
           }));
}
```

The flow goes **forward** through pre-logic, hits the microservice, then unwinds **backward** through post-logic:

```
Request direction  -->   Pre1 --> Pre2 --> Pre3 --> [Microservice]
Response direction <--   Post1<-- Post2<-- Post3<--
```

---

## Why Does Spring Cloud Gateway Use `Mono` / Reactive Style?

You'll notice `chain.filter(exchange)` returns a `Mono`, and post-logic is written inside `Mono.fromRunnable(...)`. This is because Spring Cloud Gateway is built on **Spring WebFlux** (reactive programming). It does NOT block the thread while waiting for the microservice to respond. Instead:

- It passes the request **asynchronously**
- When the response arrives, the `.then(...)` block (post logic) gets triggered automatically

Don't worry about reactive programming deeply right now — Shreyansh will cover Spring WebFlux separately. For now just remember: **write post-filter logic inside `Mono.fromRunnable(() -> { ... })`**.

---

## Quick Summary of Part 1

- Filters intercept requests/responses passing through the gateway
- Two types: **Global** (all requests) and **Route-Specific** (per route)
- Every filter has **pre logic** (before forwarding) and **post logic** (after response)
- The gateway has several **built-in global filters** already doing important jobs: `RouteToRequestUrlFilter`, `NettyRoutingFilter`, `NettyWriteResponseFilter`
- The full lifecycle goes: DispatcherHandler → RouteMappingHandler → Global Filters → Route-Specific Filters → RouteToRequestUrlFilter → NettyRoutingFilter → Microservice → (reverse) post logic → NettyWriteResponseFilter → Client

---
# Part 2 — Ordering: Why Some Global Filters Appear After Route-Specific Filters

---

## The Confusion

Looking at the request lifecycle diagram from Part 1, you might have noticed something odd:

```
Global Filter (JwtAuth)          <-- Global Filter
    |
Route-Specific Filters           <-- Route-Specific Filter
    |
RouteToRequestUrlFilter          <-- Global Filter AGAIN?
    |
NettyRoutingFilter               <-- Global Filter AGAIN?
    |
[Microservice]
    |
Route-Specific Filters (post)    <-- Route-Specific Filter
    |
Global Filter (post)             <-- Global Filter
    |
NettyWriteResponseFilter (post)  <-- Global Filter AGAIN at the END?
```

The natural question is: **why is there no clean separation?** Why aren't all global filters together, and all route-specific filters together?

The answer is: **ORDERING**.

---

## How Ordering Works

In Spring Cloud Gateway, **every filter** — whether global or route-specific — is assigned an **order value** (a number). This number decides when the filter runs relative to all other filters.

The rule is simple:

```
LOWER order value  =  HIGHER priority  =  Runs EARLIER
HIGHER order value =  LOWER priority   =  Runs LATER
```

Example:

```
Filter A  (order = -1)   --> runs FIRST
Filter B  (order =  0)   --> runs SECOND  
Filter C  (order =  1)   --> runs THIRD
Filter D  (order = 10000)--> runs VERY LATE
```

So the "type" of filter (global vs route-specific) does NOT decide the sequence. The **order value** decides it. Global and route-specific filters all sit in **one single chain**, sorted by their order values.

---

## Default Order Values

```
+---------------------------+----------------------------------------+
|      Filter Type          |         Default Order Value            |
+---------------------------+----------------------------------------+
| Global Filter             | Ordered.LOWEST_PRECEDENCE              |
| (if you don't set one)    | (runs last by default)                 |
+---------------------------+----------------------------------------+
| Route-Specific Filters    | Index in the filter list               |
| (if you define 3 filters) | filter[0] → order 0                   |
|                           | filter[1] → order 1                   |
|                           | filter[2] → order 2                   |
+---------------------------+----------------------------------------+
```

---

## The Built-in Global Filters and Their Order Values

This is where it gets interesting. Spring Cloud Gateway ships with several built-in global filters. Most global filters are intentionally given **low order values** (so they run early). But three specific ones are given **very high order values** on purpose:

```
+-------------------------------+----------------+------------------+
| Built-in Global Filter        | Order Value    | Why              |
+-------------------------------+----------------+------------------+
| NettyWriteResponseFilter      | -1             | Needs to run     |
|                               |                | EARLY in chain   |
|                               |                | so its POST logic|
|                               |                | runs LAST        |
+-------------------------------+----------------+------------------+
| Your Custom GlobalFilter      | whatever you   | You control this |
| e.g. JwtAuthGlobalFilter      | set, e.g. -1   |                  |
+-------------------------------+----------------+------------------+
| RouteToRequestUrlFilter       | 10,000         | Must run AFTER   |
|                               |                | route-specific   |
|                               |                | filters to build |
|                               |                | the correct URL  |
+-------------------------------+----------------+------------------+
| NettyRoutingFilter            | LOWEST         | Must run LAST    |
|                               | PRECEDENCE     | to actually call |
|                               | (Integer.MAX)  | the microservice |
+-------------------------------+----------------+------------------+
```

---

## Why NettyWriteResponseFilter Has Order = -1 (The Tricky One)

This one needs careful explanation because it's counterintuitive.

`NettyWriteResponseFilter` has order = -1, meaning it runs **very early** in the forward direction. But its **pre-logic is completely empty** — it does nothing on the way in.

```
NettyWriteResponseFilter (order = -1):

    PRE logic  --> EMPTY, does nothing, just calls next filter
    
    chain.filter(exchange)  --> passes to next filter
    
    POST logic --> THIS is where all the work is:
                   sends the final response back to the client
```

Because filters unwind in **reverse order** during the response phase, a filter that runs **first** on the way in will run **last** on the way back out.

```
Forward direction (request):
NettyWriteResponse(-1) --> JwtAuth(-1*) --> RouteSpecific(0,1,2) 
--> RouteToRequestUrl(10000) --> NettyRouting(MAX)
                                                |
                                         [Microservice]
                                                |
Reverse direction (response):
NettyWriteResponse   <-- JwtAuth POST  <-- RouteSpecific POST
<-- RouteToRequestUrl <-- NettyRouting POST
         ^
         |
   This POST logic runs LAST
   = sends response to client  ✓
```

So the trick is: **to make your POST logic run last, give your filter the lowest order value (earliest in chain)**. The post logic of the earliest filter in the chain runs last during response unwinding.

---

## The Full Picture — All Filters in Order

```
ORDER VALUE         FILTER                          DIRECTION
-----------         ------                          ---------

   -1         NettyWriteResponseFilter         PRE: empty (pass through)
                                               POST: send response to client (runs LAST)

   -1         JwtAuthGlobalFilter (custom)     PRE: validate JWT token
                                               POST: your custom post logic

   0,1,2...   Route-Specific Filters           PRE: add/remove headers, 
              (Retry, CircuitBreaker,                circuit breaker check...
               AddHeader, etc.)                POST: response transformations...

  10,000      RouteToRequestUrlFilter          PRE: resolve lb://product-service
                                                    --> call Eureka
                                                    --> get real host:port

  MAX         NettyRoutingFilter               PRE: actually call the microservice
                                               POST: receive the response
```

---

## Code Proof — The Order Values in Spring Source

From the instructor's PDF, here is what the Spring source code actually shows:

```java
// RouteToRequestUrlFilter
public static final int ROUTE_TO_URL_FILTER_ORDER = 10000;

// NettyRoutingFilter
public static final int ORDER = Ordered.LOWEST_PRECEDENCE;
// Ordered.LOWEST_PRECEDENCE = Integer.MAX_VALUE
```

And for `NettyWriteResponseFilter`, its order is `-1`, which is why it enters the chain first but sends the response last.

---

## Summary Table — The "Why" Behind Each Filter's Order

```
+----------------------------+----------+---------------------------+
| Filter                     | Order    | Reason for this order     |
+----------------------------+----------+---------------------------+
| NettyWriteResponseFilter   | -1       | POST logic must run last  |
|                            |          | (sends response to client)|
+----------------------------+----------+---------------------------+
| JwtAuthGlobalFilter        | -1       | Auth must happen before   |
| (your custom global filter)| (custom) | everything else           |
+----------------------------+----------+---------------------------+
| Route-Specific Filters     | 0, 1, 2  | Run after auth, before    |
|                            |          | URL resolution            |
+----------------------------+----------+---------------------------+
| RouteToRequestUrlFilter    | 10,000   | URL must be resolved after|
|                            |          | all filters have run      |
+----------------------------+----------+---------------------------+
| NettyRoutingFilter         | MAX      | Actual microservice call  |
|                            |          | must be the very last step|
+----------------------------+----------+---------------------------+
```

---

## Quick Summary of Part 2

- All filters (global + route-specific) sit in **one unified chain**, sorted by order value
- Lower order value = higher priority = runs earlier in the chain
- Most global filters run early (low order values), but `RouteToRequestUrlFilter` and `NettyRoutingFilter` are intentionally given very high values so they run late
- `NettyWriteResponseFilter` has order `-1` — its pre-logic is empty, but its post-logic (sending response to client) runs last because of how the chain unwinds in reverse
- If you don't set an order on your custom global filter, it defaults to `Ordered.LOWEST_PRECEDENCE` (runs last)
- Route-specific filters default to index order: 0, 1, 2...

---

# 🤔Clearing the Confusion — Auth in Microservices vs Monolith

Before we go back to Part 4 of the API Gateway notes, let's fully resolve this confusion first. Because if this isn't clear, nothing else will make sense.

---

## First — Let's Revisit What You Already Know (Monolith)

In a monolith, everything lived in one application:

```
ONE SPRING BOOT APP (Monolith)
┌──────────────────────────────────────────────────────────────┐
│                                                              │
│  SecurityFilterChain (your SecurityConfig)                   │
│  ├── JWTAuthenticationFilter   → /generate-token             │
│  ├── JwtValidationFilter       → all requests                │
│  └── JWTRefreshFilter          → /refresh-token              │
│                                                              │
│  JWTAuthenticationProvider                                   │
│  └── validates token                                         │
│  └── loads user from THIS app's DB                           │
│  └── puts Authentication object in SecurityContext           │
│                                                              │
│  Controllers                                                 │
│  ├── /generate-token    → handled inside filter              │
│  ├── /api/orders        → @PreAuthorize("hasRole...")        │
│  └── /api/products      → @PreAuthorize("hasRole...")        │
│                                                              │
│  ONE DB                                                      │
│  └── users table (username, password, role)                  │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

Everything was in the same process. The `SecurityContext` was **thread-local** — it lived for the duration of that one request, inside that one app. `@PreAuthorize` worked because Spring Security's interceptor could reach into `SecurityContextHolder` and find the `Authentication` object right there.

---

## Now — What Changes in Microservices?

In microservices, you have **separate processes** running independently. There is no shared memory, no shared `SecurityContext`, no shared DB between services.

```
MICROSERVICES WORLD
┌──────────────┐    ┌──────────────┐    ┌───────────────┐
│ Auth Service │    │ Order Service│    │Product Service│
│  (port 8081) │    │  (port 8082) │    │  (port 8083)  │
│              │    │              │    │               │
│  Has its-    │    │  Has its-    │    │  Has its-     │
│  own process │    │  own process │    │  own process  │
│  own memory  │    │  own memory  │    │  own memory   │
│  own DB      │    │  own DB      │    │  own DB       │
└──────────────┘    └──────────────┘    └───────────────┘
        ↑
        |
**No SecurityContext**
can travel between
these processes!
```

This means:

- The `Authentication` object you stored in `SecurityContextHolder` inside Order Service **does not exist** in Product Service
- You **cannot** just share the filter chain across services
- Each service is a completely separate Spring Boot app

So the question becomes: **how does auth work across all these separate processes?**

---

## The Answer — Split Responsibilities Clearly

Here is the clean mental model. Read this carefully:

```
┌──────────────────────────────────────────────────────────────────────┐
│              WHO IS RESPONSIBLE FOR WHAT                             │
├───────────────────────┬──────────────────────────────────────────────┤
│  **Auth Service**     │  User registration (signup)                  │
│                       │  Token generation (login → JWT)              │
│                       │  Token refresh                               │
│                       │  User DB (username, password, role)          │
├───────────────────────┼──────────────────────────────────────────────┤
│  **API Gateway**      │  JWT verification (for ALL requests)         │
│  (Global Filter)      │  If invalid → reject immediately (401)       │
│                       │  If valid → extract role, pass forward       │
├───────────────────────┼──────────────────────────────────────────────┤
│  **Individual**       │  Business logic only                         │
│  **Microservices**    │  Trust that gateway already verified token   │
│  (Order, Product etc) │  Use @PreAuthorize for role-based access     │
│                       │  Read role from request header               │
└───────────────────────┴──────────────────────────────────────────────┘
```

Now let's build the full picture step by step.

---

## Step 1 — User Registration & Login (Auth Service)

The Auth Service is just a regular Spring Boot app. It has everything you already know from your monolith notes.

```
AUTH SERVICE (port 8081)
┌────────────────────────────────────────────────────────────┐
│                                                            │
│  DB: users table                                           │
│  ┌────┬──────────┬──────────────┬───────────┐              │
│  │ id │ username │ password     │ role      │              │
│  ├────┼──────────┼──────────────┼───────────┤              │
│  │ 1  │ john     │ $2a$10$...   │ ROLE_USER │              │
│  │ 2  │ admin    │ $2a$10$...   │ ROLE_ADMIN│              │
│  └────┴──────────┴──────────────┴───────────┘              │
│                                                            │
│  POST /auth/register  → save user to DB                    │
│  POST /auth/login     → validate credentials               │
│                         generate JWT token                 │
│                         return token to client             │
│                                                            │
│  SecurityConfig → /auth/register and /auth/login           │
│                   are permitAll() (no token needed)        │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

The JWT token generated here contains the user's **role** inside the payload (claims):

```
JWT Payload (what goes inside the token):
{
  "sub": "john",           ← username
  "role": "ROLE_USER",     ← role  ← THIS IS **CRITICAL**
  "iat": 1716000000,       ← issued at
  "exp": 1716000900        ← expires at (15 min later)
}
```

This is the key insight: ***the role must be baked into the token itself*** at the time of generation. Why? Because when the API Gateway later verifies the token, it needs to extract the role from there — it has no access to the Auth Service's DB.

---

## Step 2 — The API Gateway's Job (JWT Verification in Global Filter)

Now the client has a token. They call `GET /api/orders`. This request hits the API Gateway first.

The `JwtAuthGlobalFilter` (**Global Filter**) does the following:

```
REQUEST: GET localhost:8083/api/orders
         **Authorization: Bearer eyJhbGci...**
                │
                ▼
┌───────────────────────────────────────────────────────────┐
│             JwtAuthGlobalFilter (API Gateway)             │
│                                                           │
│  1. Is this /auth/**?                                     │
│     YES → skip, pass through to Auth Service              │
│     NO  → continue verification                           │
│                                                           │
│  2. Is Authorization header present?                      │
│     NO  → return 401 Unauthorized immediately             │
│     YES → extract the token                               │
│                                                           │
│  3. Verify the token:                                     │
│     - Check signature (was it signed with our secret key?)│
│     - Check expiry (is it still valid?)                   │
│     INVALID → return 401 Unauthorized immediately         │
│     VALID   → extract claims from payload                 │
│                                                           │
│  4. Extract from token payload:                           │
│     - username = "john"                                   │
│     - role     = "ROLE_USER"                              │
│                                                           │
│  5. Add **role** to the **REQUEST HEADER**                │
│     → X-User-Role: ROLE_USER                              │
│     → X-Username: john                                    │
│                                                           │
│  6. Forward the modified request to Order Service         │
│                                                           │
└───────────────────────────────────────────────────────────┘
                │
                ▼
        Order Service receives request
        WITH the added headers:
        X-User-Role: ROLE_USER
        X-Username: john
```

**Step 5 is the missing piece you were confused about.** The Gateway doesn't just verify — it also **extracts the role and passes it forward as a request header**. This is how the role travels from the token into the microservice.

---

## The JwtAuthGlobalFilter — Full Code

This is what the actual implementation looks like, using your JWT knowledge from the monolith:

```java
@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    // Same JWTUtil you already know from your monolith notes
    // It has: validateAndExtractClaims(token)
    @Autowired
    private JWTUtil jwtUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();

        // Step 1: Skip validation for auth endpoints
        // These are handled by Auth Service — no token exists yet
        if (path.startsWith("/auth")) {
            return chain.filter(exchange);
        }

        // Step 2: Check Authorization header
        String authHeader = exchange.getRequest()
                                    .getHeaders()
                                    .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // Step 3: Extract and verify token
        String token = authHeader.substring(7);

        try {
            // validateAndExtractClaims() - same logic from your monolith JWTUtil
            // parses the token, checks signature, checks expiry
            // returns the Claims (payload) if valid
            Claims claims = jwtUtil.validateAndExtractClaims(token);

            // Step 4: Extract role and username from token payload
            String username = claims.getSubject();
            String role = claims.get("role", String.class);

            // Step 5: Add role info to the request header
            // So downstream microservices can read it
            ServerHttpRequest modifiedRequest = exchange.getRequest()
                .mutate()
                .header("X-Username", username)
                .header("X-User-Role", role)
                .build();

            // Step 6: Forward modified request (with headers) to microservice
            return chain.filter(exchange.mutate()
                                        .request(modifiedRequest)
                                        .build());

        } catch (Exception e) {
            // Token invalid or expired
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -1; // Run first, before everything else
    }
}
```

---

## Step 3 — Inside the Microservice (Order Service)

Now Order Service receives the request. It has the headers `X-Username: john` and `X-User-Role: ROLE_USER` added by the gateway.

But here's your exact question: **how does `@PreAuthorize` work here?**

`@PreAuthorize` needs an `Authentication` object in the `SecurityContextHolder`. In the monolith, the `JwtValidationFilter` created this object. In a microservice, **we need to do the same thing** — but instead of reading from the JWT token directly (we don't even want to validate again), we read from the **request headers the gateway added**.

Here is the filter inside Order Service:

```java
// This filter runs inside EACH MICROSERVICE
// It reads the headers the gateway added
// and builds an Authentication object from them
// so that @PreAuthorize works normally

@Component
public class GatewayAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Read the headers that API Gateway added
        String username = request.getHeader("X-Username");
        String role     = request.getHeader("X-User-Role");

        // If headers are present (meaning gateway already verified the token)
        if (username != null && role != null) {

            // Build the GrantedAuthority list from role
            List<GrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority(role));

            // Build a fully authenticated **Authentication** object
            // 3-argument constructor = isAuthenticated() returns true
            UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                    username,    // principal
                    null,        // credentials (no password needed here)
                    authorities  // roles → ROLE_USER, ROLE_ADMIN etc.
                );

            // Store it in SecurityContextHolder
            // Now @PreAuthorize can read from here, just like in monolith!
            SecurityContextHolder.getContext()
                                 .setAuthentication(authToken);
        }

        // Continue the chain — reach the controller
        filterChain.doFilter(request, response);
    }
}
```

And the SecurityConfig inside Order Service:

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // enables @PreAuthorize
public class OrderServiceSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http.authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .csrf(csrf -> csrf.disable())
            // Add our custom filter that reads gateway headers
            .addFilterBefore(
                new GatewayAuthFilter(),
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}
```

Now `@PreAuthorize` works **exactly** as you know it from the monolith:

```java
@RestController
@RequestMapping("/orders")
public class OrderController {

    // Works exactly like monolith — reads from SecurityContextHolder
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}
```

---

## The Full End-to-End Picture — E-Commerce System

Let's now trace a complete request through the entire system:

```
E-COMMERCE MICROSERVICES SYSTEM
┌─────────────────────────────────────────────────────────────────────┐
│                                                                     │
│  Services:                                                          │
│  ├── API Gateway     (port 8080)                                    │
│  ├── Auth Service    (port 8081)  ← user DB, token generation       │
│  ├── Order Service   (port 8082)  ← orders DB                       │
│  └── Product Service (port 8083)  ← products DB                     │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

**Flow 1 — User Registration:**

```
Client: POST localhost:8080/auth/register
        Body: { username: "john", password: "123", role: "ROLE_USER" }
        │
        ▼
API Gateway
  JwtAuthGlobalFilter:
  → path starts with /auth → SKIP verification
  → forward to Auth Service
        │
        ▼
Auth Service
  → hash password with BCrypt
  → save to users table
  → return 200 OK
```

**Flow 2 — Login (Token Generation):**

```
Client: POST localhost:8080/auth/login
        Body: { username: "john", password: "123" }
        │
        ▼
API Gateway
  JwtAuthGlobalFilter:
  → path starts with /auth → SKIP verification
  → forward to Auth Service
        │
        ▼
Auth Service
  → load user from DB
  → verify password (BCrypt compare)
  → generate JWT:
    payload = { sub: "john", role: "ROLE_USER", exp: ... }
  → return token in Authorization header
        │
        ▼
Client receives:
  Authorization: Bearer eyJhbGci...
  (stores this token for future requests)
```

**Flow 3 — Accessing a Protected API (Order Service):**

```
Client: GET localhost:8080/orders
        Authorization: Bearer eyJhbGci...
        │
        ▼
API Gateway (port 8080)
  JwtAuthGlobalFilter:
  → path is /orders, NOT /auth → DO verify
  → extract token from header
  → verify signature ✅
  → verify not expired ✅
  → extract: username = "john", role = "ROLE_USER"
  → add to request:
      X-Username: john
      X-User-Role: ROLE_USER
  → forward to Order Service
        │
        ▼
Order Service (port 8082)
  GatewayAuthFilter:
  → reads X-Username = "john"
  → reads X-User-Role = "ROLE_USER"
  → builds Authentication object:
      principal    = "john"
      authorities  = [ROLE_USER]
      isAuthenticated = true
  → stores in SecurityContextHolder
  → continues filter chain
        │
        ▼
  AuthorizationFilter (Spring default):
  → checks .anyRequest().authenticated()
  → isAuthenticated() = true ✅ passes
        │
        ▼
  OrderController.getAllOrders()
  @PreAuthorize("hasRole('USER')")
  → checks SecurityContextHolder
  → ROLE_USER present ✅
  → method executes
  → returns list of orders
        │
        ▼
Client receives: [ { orderId: 1, ... }, { orderId: 2, ... } ]
```

**Flow 4 — Unauthorized Access (ROLE_USER trying ADMIN endpoint):**

```
Client: DELETE localhost:8080/orders/1
        Authorization: Bearer eyJhbGci...  (john's token, ROLE_USER)
        │
        ▼
API Gateway
  → token valid ✅
  → X-User-Role: ROLE_USER added
  → forwarded to Order Service
        │
        ▼
Order Service
  GatewayAuthFilter:
  → builds Authentication with ROLE_USER
        │
        ▼
  OrderController.deleteOrder()
  @PreAuthorize("hasRole('ADMIN')")
  → checks SecurityContextHolder
  → ROLE_ADMIN NOT present ❌
  → throws 403 Forbidden
  → controller never executes
```

---

## The "Why No Double Verification" Question

You might wonder: Order Service has Spring Security — why doesn't it verify the JWT itself again?

```
┌───────────────────────────────────────────────────────────────────┐
│              Why Microservices Don't Re-Verify JWT                │
├───────────────────────────────────────────────────────────────────┤
│                                                                   │
│  **Option A**: Each microservice verifies JWT itself              │
│                                                                   │
│  ✗ Every service needs the secret key                             │
│    → secret key must be shared everywhere                         │
│    → security risk                                                │
│  ✗ Every service needs **JWTUtil**, **jjwt** dependency           │
│    → repeated code in every service                               │
│  ✗ If you rotate the secret key                                   │
│    → must update every single service                             │
│  ✗ Defeats the purpose of centralized gateway                     │
│                                                                   │
│  **Option B**: Only API Gateway verifies (what we do)             │
│                                                                   │
│  ✓ Secret key lives in ONE place (the gateway)                    │
│  ✓ No JWT dependency needed in downstream services                │
│  ✓ Key rotation = update only one place                           │
│  ✓ Services just trust the headers gateway passes                 │
│  ✓ Clean separation of concerns                                   │
│                                                                   │
│  GOLDEN RULE:                                                     │
│  Verify once at the gate.                                         │
│  Trust what the gate passes through.                              │
│                                                                   │
└───────────────────────────────────────────────────────────────────┘
```

---

## Complete Architecture Diagram — The Big Picture

```
                        CLIENT
                           │
                           │ every request carries:
                           │ Authorization: Bearer <JWT>
                           │
                           ▼
              ┌────────────────────────┐
              │      API GATEWAY       │
              │       port 8080        │
              │                        │
              │  JwtAuthGlobalFilter   │
              │  ┌──────────────────┐  │
              │  │ /auth/**?        │  │
              │  │  YES → skip      │  │
              │  │  NO  → verify    │  │
              │  │                  │  │
              │  │ token invalid?   │  │
              │  │  → 401, STOP     │  │
              │  │                  │  │
              │  │ token valid?     │  │
              │  │  → extract       │  │
              │  │    username,role │  │
              │  │  → add headers   │  │
              │  │    X-Username    │  │
              │  │    X-User-Role   │  │
              │  └──────────────────┘  │
              │                        │
              │  Routes:               │
              │  /auth/**  → Auth Svc  │
              │  /orders/** → Order Svc│
              │  /products/**→ Prod Svc│
              └────────────────────────┘
                  │          │         │
           /auth/**    /orders/**   /products/**
                  │          │         │
                  ▼          ▼         ▼
        ┌──────────────┐ ┌──────────┐ ┌───────────────┐
        │ Auth Service │ │  Order   │ │    Product    │
        │  port 8081   │ │  Service │ │    Service    │
        │              │ │ port 8082│ │   port 8083   │
        │ /register    │ │          │ │               │
        │ /login       │ │ Gateway  │ │  Gateway      │
        │              │ │ AuthFltr │ │  AuthFilter   │
        │ UserDetails  │ │ reads    │ │  reads        │
        │ Service      │ │ X-headers│ │  X-headers    │
        │ (has user DB)│ │ builds   │ │  builds       │
        │              │ │ Auth obj │ │  Auth obj     │
        │ Generates    │ │ in       │ │  in           │
        │ JWT with     │ │ Security │ │  Security     │
        │ role in      │ │ Context  │ │  Context      │
        │ payload      │ │          │ │               │
        └──────────────┘ │ @PreAuth │ │  @PreAuth     │
                         │ works    │ │  works        │
                         │ normally │ │  normally     │
                         └──────────┘ └───────────────┘
```

---

## Summary — Answers to All Your Questions

```
┌──────────────────────────────────────────────────────────────┐
│                   YOUR QUESTIONS — ANSWERED                  │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  Q: Where does user creation and token generation go?        │
│  A: Auth Service. It has UserDetailsService, BCrypt,         │
│     JWTUtil — everything from your monolith notes.           │
│     Just in its own separate microservice.                   │
│                                                              │
│  Q: How does the global filter verify the token?             │
│  A: Using the same JWTUtil.validateAndExtractClaims()        │
│     you already know. It checks signature + expiry,          │
│     extracts username and role from payload.                 │
│                                                              │
│  Q: How does role get to the microservice?                   │
│  A: Gateway extracts role from token, adds it as             │
│     X-User-Role request header, forwards to service.         │
│                                                              │
│  Q: Does @PreAuthorize still work in microservices?          │
│  A: YES. Each microservice has a GatewayAuthFilter           │
│     that reads X-User-Role header, builds an                 │
│     Authentication object, puts it in                        │
│     SecurityContextHolder. @PreAuthorize reads               │
│     from SecurityContextHolder — same as monolith.           │
│                                                              │
│  Q: Does each microservice re-verify the JWT?                │
│  A: NO. Gateway verifies once. Services trust the            │
│     headers gateway passes. Secret key stays in              │
│     one place.                                               │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

---

All your existing concepts connect cleanly:

- **Auth Service** = everything from your JWT monolith notes (UserDetailsService, BCrypt, JWTUtil, token generation)
- **API Gateway Global Filter** = JwtValidationFilter logic from monolith, but runs centrally for all services
- **Each Microservice** = reads the headers gateway passes, builds Authentication object, `@PreAuthorize` works exactly as you know it

Ready to go back to **Part 4 — Route-Specific Filters**?

# Part 4 — Route-Specific Filters: Pre-existing Filters

---

## Quick Recap — Where We Are

So far we have covered:
- What filters are and the two types (Global vs Route-Specific)
- How ordering works across the entire filter chain
- How to write a custom Global Filter (JWT Auth)

Now we look at **Route-Specific Filters**. These are filters you attach to a **specific route** — not all traffic, only the traffic matching that particular route.

---

## Why Route-Specific Filters?

Different microservices have different needs. Your Product Service might need retry logic with 4 attempts. Your Order Service might need a circuit breaker with a 60% failure threshold. You don't want to apply these globally — each service needs its own configuration.

```
WITHOUT Route-Specific Filters:
─────────────────────────────────────────────────────────
  If you put retry logic in a Global Filter:
  → ALL routes get the same retry count
  → Product Service: retries 4 times
  → Order Service: also retries 4 times (not what you want)
  → Auth Service: also retries 4 times (makes no sense)

WITH Route-Specific Filters:
─────────────────────────────────────────────────────────
  Product Service route → Retry filter (4 attempts)
  Order Service route   → CircuitBreaker filter (60% threshold)
  Auth Service route    → No resilience filter needed
  
  Each route gets exactly what it needs. Nothing more.
```

---

## How Route-Specific Filters Are Configured

Before looking at individual filters, you need to understand the **naming pattern** — because every pre-existing filter follows the same convention.

Every route-specific filter in Spring Cloud Gateway has a class in the framework named like this:

```
<FilterName>GatewayFilterFactory.java
```

When you configure it in `application.properties`, you write only the `<FilterName>` part. Spring automatically appends `GatewayFilterFactory` when doing the mapping.

```
In application.properties:        Framework class it maps to:
──────────────────────────────    ──────────────────────────────────────
name=RemoveRequestHeader      →   RemoveRequestHeaderGatewayFilterFactory
name=AddRequestHeader         →   AddRequestHeaderGatewayFilterFactory
name=Retry                    →   RetryGatewayFilterFactory
name=CircuitBreaker           →   CircuitBreakerGatewayFilterFactory
name=CustomRoute              →   CustomRouteGatewayFilterFactory (yours)
```

Each factory class has a **Config inner class** that defines what dynamic values you can pass in. You supply those values via `.args.fieldName=value` in the properties file.

The general pattern in `application.properties`:

```properties
# Attach a filter to a specific route:
spring.cloud.gateway.routes[0].filters[0].name=<FilterName>
spring.cloud.gateway.routes[0].filters[0].args.<field>=<value>

# Multiple filters on the same route:
spring.cloud.gateway.routes[0].filters[0].name=<FilterName1>
spring.cloud.gateway.routes[0].filters[1].name=<FilterName2>
spring.cloud.gateway.routes[0].filters[2].name=<FilterName3>
```

---

## The Full Filter Chain Position

Route-specific filters sit **between** the global auth filter and the `RouteToRequestUrlFilter`:

```
Request
  │
  ▼
JwtAuthGlobalFilter (order=-1)       ← Global: auth check
  │
  ▼
Route-Specific Filters (order=0,1,2) ← These are what we cover NOW
  │  e.g. AddRequestHeader
  │  e.g. Retry
  │  e.g. CircuitBreaker
  ▼
RouteToRequestUrlFilter (order=10000) ← Global: resolve lb:// to real URL
  │
  ▼
NettyRoutingFilter (order=MAX)        ← Global: actually calls microservice
  │
  ▼
[Microservice responds]
  │
  ▼
Route-Specific Filters (post logic, reverse order)
  │
  ▼
JwtAuthGlobalFilter (post logic)
  │
  ▼
NettyWriteResponseFilter (post logic) ← sends response to client
```

---

## Pre-existing Route-Specific Filters — Overview

```
┌──────────────────────────┬───────────────────────────────────────────┐
│  Filter Name             │  What It Does                             │
├──────────────────────────┼───────────────────────────────────────────┤
│  AddRequestHeader        │  Adds a header to the outgoing request    │
│  AddResponseHeader       │  Adds a header to the response            │
│  RemoveRequestHeader     │  Removes a header from the request        │
│  RemoveResponseHeader    │  Removes a header from the response        │
│  Retry                   │  Retries the request on failure           │
│  RequestRateLimiter      │  Limits how many requests per second      │
│  CircuitBreaker          │  Stops calling a failing microservice     │
└──────────────────────────┴───────────────────────────────────────────┘
```

We will go through each one with its config, the framework class it maps to, and what the Config inner class looks like — so you can understand any filter by just opening its factory class.

---

## Filter 1 — RemoveRequestHeader

### What It Does
Removes a specific header from the incoming request **before** it reaches the microservice. Useful for stripping sensitive headers (like internal API keys) that clients shouldn't be able to see or manipulate.

```
CLIENT REQUEST                    MICROSERVICE RECEIVES
──────────────────────────────    ────────────────────────────────
Authorization: Bearer <token>     Authorization: Bearer <token>
Api-private-Key: secret123    →   (Api-private-Key is GONE)
Content-Type: application/json    Content-Type: application/json
```

### Framework Class

```java
// RemoveRequestHeaderGatewayFilterFactory.java (in Spring framework)

public class RemoveRequestHeaderGatewayFilterFactory
    extends AbstractGatewayFilterFactory<
            AbstractGatewayFilterFactory.NameConfig> {  // ← Config it uses

    @Override
    public GatewayFilter apply(NameConfig config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .headers(h -> h.remove(config.getName())) // removes the header
                .build();
            return chain.filter(exchange.mutate()
                                        .request(request).build());
        };
    }
}
```

The `NameConfig` has one field: `name`. That's the header name you want to remove.

### Configuration

```properties
spring.cloud.gateway.routes[0].id=product-service
spring.cloud.gateway.routes[0].uri=lb://product-service
spring.cloud.gateway.routes[0].predicates[0]=Path=/products/**

## FILTERS ##

# Remove the Api-private-Key header before forwarding to product service
spring.cloud.gateway.routes[0].filters[0].name=RemoveRequestHeader
spring.cloud.gateway.routes[0].filters[0].args.name=Api-private-Key
#                                                    ↑
#                               this maps to NameConfig.name field
```

---

## Filter 2 — AddRequestHeader and AddResponseHeader

### What They Do

`AddRequestHeader` adds a custom header to the request going **to** the microservice.
`AddResponseHeader` adds a custom header to the response going **back to** the client.

```
AddRequestHeader:

CLIENT REQUEST              MICROSERVICE RECEIVES
────────────────────        ──────────────────────────────────────
GET /products/1         →   GET /products/1
                            X-TestRequestHeader: ApiGatewayRequest
                            (header was added by gateway)


AddResponseHeader:

MICROSERVICE RESPONSE       CLIENT RECEIVES
────────────────────        ──────────────────────────────────────
200 OK                  →   200 OK
body: {...}                 X-TestResponseHeader: ApiGatewayResponse
                            body: {...}
                            (header was added by gateway)
```

### Framework Class

Both use `NameValueConfig` — a config with two fields: `name` (header name) and `value` (header value).

```java
// AddRequestHeaderGatewayFilterFactory.java (framework)
public static class NameValueConfig {
    protected String name;   // header name
    protected String value;  // header value
}
```

### Configuration — Multiple Filters Together

```properties
spring.cloud.gateway.routes[0].id=product-service
spring.cloud.gateway.routes[0].uri=lb://product-service
spring.cloud.gateway.routes[0].predicates[0]=Path=/products/**

## FILTERS ##

# Filter 0: Add a request header
spring.cloud.gateway.routes[0].filters[0].name=AddRequestHeader
spring.cloud.gateway.routes[0].filters[0].args.name=X-TestRequestHeader
spring.cloud.gateway.routes[0].filters[0].args.value=ApiGatewayRequest

# Filter 1: Add a response header
spring.cloud.gateway.routes[0].filters[1].name=AddResponseHeader
spring.cloud.gateway.routes[0].filters[1].args.name=X-TestResponseHeader
spring.cloud.gateway.routes[0].filters[1].args.value=ApiGatewayResponse

# Filter 2: Remove a request header
spring.cloud.gateway.routes[0].filters[2].name=RemoveRequestHeader
spring.cloud.gateway.routes[0].filters[2].args.name=Api-private-Key
```

This is how multiple filters stack on the same route. Filter index goes 0, 1, 2 and so on.

---

## Filter 3 — Retry

### The Problem It Solves

Microservices fail. Networks are unreliable. A request that fails once might succeed if you just try again. Instead of writing retry logic inside every microservice, the API Gateway handles it automatically.

```
WITHOUT Retry filter:
─────────────────────────────────────────────────────────
  Client → Gateway → Product Service (FAILS)
  Gateway returns error to client immediately.
  Client sees a failure even though a retry might succeed.

WITH Retry filter (retries=4, methods=GET):
─────────────────────────────────────────────────────────
  Attempt 1: Client → Gateway → Product Service (FAILS)
  Attempt 2: Gateway retries  → Product Service (FAILS)
  Attempt 3: Gateway retries  → Product Service (FAILS)
  Attempt 4: Gateway retries  → Product Service (SUCCESS ✅)
  Client gets the successful response.
  Client never even knew there were 3 failures.
```

### Framework Class and Its Config

```java
// RetryGatewayFilterFactory.java (framework)
public static class RetryConfig {
    int retries;               // total attempts (including first call)
    List<HttpStatus> statuses; // which HTTP status codes trigger a retry
    List<HttpMethod> methods;  // which HTTP methods to retry (GET, DELETE etc.)
    // ... more fields
}
```

### Configuration

```properties
spring.cloud.gateway.routes[0].id=product-service
spring.cloud.gateway.routes[0].uri=lb://product-service
spring.cloud.gateway.routes[0].predicates[0]=Path=/products/**

## FILTERS ##

# Retry on failure: max 4 total attempts, only for GET and DELETE
spring.cloud.gateway.routes[0].filters[0].name=Retry
spring.cloud.gateway.routes[0].filters[0].args.retries=4
spring.cloud.gateway.routes[0].filters[0].args.methods=GET,DELETE
```

### Important — What "retries=4" Actually Means

```
retries=4 means:
─────────────────────────────────────────────────────────
  Total attempts = 4

  Attempt 1 = original call
  Attempt 2 = retry 1
  Attempt 3 = retry 2
  Attempt 4 = retry 3  ← if this also fails → return error

  So you get 1 original + 3 retries = 4 total attempts
```

### Testing Code in Product Service Controller

Shreyansh writes this just to verify the retry filter is working. The endpoint fails for the first 3 calls and succeeds on the 4th:

```java
@RestController
@RequestMapping("/products")
public class ProductController {

    int counter = 0;

    @GetMapping("/{id}")
    public ResponseEntity<String> getProduct(@PathVariable String id) {

        counter++;

        // Fail for first 3 attempts
        if (counter <= 3) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Fail");
        }

        // Succeed on 4th attempt
        return ResponseEntity
            .ok()
            .body("fetch the product details on "
                  + counter + " attempt");
    }
}
```

When you hit `GET localhost:8083/products/1`, the response you get back is:

```
fetch the product details on 4 attempt
```

This proves the gateway retried 3 times and the 4th call succeeded — all transparent to the client.

### Why Only GET and DELETE?

```
┌──────────────────────────────────────────────────────────────┐
│           Why NOT retry POST or PUT?                         │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  GET, DELETE → IDEMPOTENT                                    │
│  Calling them multiple times has the same effect.            │
│  GET /products/1 twice → you just fetched twice, no harm     │
│  DELETE /products/1 twice → first deletes, second is no-op   │
│                                                              │
│  POST, PUT → NOT IDEMPOTENT                                  │
│  POST /orders twice → you created TWO orders (disaster!)     │
│  PUT /products/1 twice → might be okay but risky             │
│                                                              │
│  So by default, only retry safe/idempotent methods.          │
└──────────────────────────────────────────────────────────────┘
```

---

## Filter 4 — CircuitBreaker

### The Problem It Solves

Retry helps with occasional failures. But what if a microservice is completely down? Retrying a dead service is wasteful — it just delays the error response and wastes gateway resources.

The Circuit Breaker pattern solves this. It monitors failures and when failures cross a threshold, it **opens the circuit** — meaning it stops calling the failing service altogether and immediately returns a fallback response.

```
CIRCUIT BREAKER STATES:
─────────────────────────────────────────────────────────────────────

CLOSED STATE (normal operation):
  All calls allowed through to microservice.
  Failures are counted in a sliding window.
  
  Gateway → Product Service ✅ → response
  Gateway → Product Service ✅ → response
  Gateway → Product Service ❌ → failure (counted)
  Gateway → Product Service ❌ → failure (counted)
  Gateway → Product Service ❌ → failure threshold crossed!
  
  → Circuit OPENS


OPEN STATE (service is down, stop calling it):
  ALL calls are immediately redirected to fallback.
  Product Service is NOT called at all.
  Gateway waits for "waitDuration" (e.g. 10 seconds)
  
  Gateway → [FALLBACK] → "Service temporarily unavailable"
  Gateway → [FALLBACK] → "Service temporarily unavailable"
  (after 10 seconds...)
  
  → Circuit moves to HALF-OPEN


HALF-OPEN STATE (testing if service recovered):
  A limited number of test calls are allowed through.
  
  Gateway → Product Service ✅ → success!
  Gateway → Product Service ✅ → success!
  Gateway → Product Service ✅ → success!
  (if enough test calls succeed...)
  
  → Circuit CLOSES again (back to normal)
```

### Dependency Required

Unlike Retry (which is built in), CircuitBreaker needs Resilience4j:

```xml
<!-- pom.xml — add this to API Gateway -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-reactor-resilience4j</artifactId>
</dependency>
```

### The Fallback Controller (inside API Gateway)

When the circuit is open, the gateway calls an internal endpoint for the fallback response. This controller lives **inside the API Gateway itself** — not in any microservice:

```java
@Controller
public class FallbackController {

    @GetMapping("/fallback")
    public ResponseEntity<String> fallback() {
        return ResponseEntity.ok(
            "Service is temporarily unavailable, please try again later."
        );
    }
}
```

### Configuration

```properties
# API Gateway application.properties

spring.cloud.gateway.routes[0].id=product-service
spring.cloud.gateway.routes[0].uri=lb://product-service
spring.cloud.gateway.routes[0].predicates[0]=Path=/products/**

## FILTERS ##

# CircuitBreaker filter
spring.cloud.gateway.routes[0].filters[0].name=CircuitBreaker
spring.cloud.gateway.routes[0].filters[0].args.name=myCircuitBreakerName
#                                                    ↑
#              This name links the filter to the config block below

spring.cloud.gateway.routes[0].filters[0].args.fallbackUri=forward:/fallback
#                                                           ↑
#               "forward:" means internal call within API Gateway itself
#               "/fallback" is the endpoint in FallbackController above

## CircuitBreaker config (linked by the name above) ##

resilience4j.circuitbreaker.instances.myCircuitBreakerName.sliding-window-type=COUNT_BASED
resilience4j.circuitbreaker.instances.myCircuitBreakerName.sliding-window-size=10
#  ↑ look at last 10 calls to calculate failure rate

resilience4j.circuitbreaker.instances.myCircuitBreakerName.minimum-number-of-calls=5
#  ↑ need at least 5 calls before calculating failure rate
#    (avoids opening circuit on just 1-2 unlucky calls)

resilience4j.circuitbreaker.instances.myCircuitBreakerName.failure-rate-threshold=50
#  ↑ if 50% or more of calls fail → open the circuit

resilience4j.circuitbreaker.instances.myCircuitBreakerName.wait-duration-in-open-state=10s
#  ↑ stay in OPEN state for 10 seconds before moving to HALF-OPEN

resilience4j.circuitbreaker.instances.myCircuitBreakerName.permitted-number-of-calls-in-half-open-state=3
#  ↑ allow 3 test calls in HALF-OPEN before deciding to close or reopen

# See circuit breaker state transitions in logs
logging.level.io.github.resilience4j.circuitbreaker=DEBUG

# Eureka
eureka.client.service-url.defaultZone=http://localhost:8761/eureka
```

### How the Name Links Filter to Config

```
filters[0].args.name=myCircuitBreakerName
                     ↑
                     │ This name is used as a key
                     │ to find the right config block:
                     ↓
resilience4j.circuitbreaker.instances.myCircuitBreakerName.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.myCircuitBreakerName.sliding-window-size=10
...

You can have different circuit breakers for different routes
by giving them different names and different config blocks.
```

### Testing the Circuit Breaker

Shreyansh tests by **not starting the Product Service at all**, then calling the API more than 5 times (minimum-number-of-calls=5, threshold=50%):

```
Call 1: Gateway → Product Service → CONNECTION REFUSED (failure)
Call 2: Gateway → Product Service → CONNECTION REFUSED (failure)
Call 3: Gateway → Product Service → CONNECTION REFUSED (failure)
Call 4: Gateway → Product Service → CONNECTION REFUSED (failure)
Call 5: Gateway → Product Service → CONNECTION REFUSED (failure)
                                                            ↑
                              5 calls, all failed → 100% failure rate
                              100% > 50% threshold → CIRCUIT OPENS

Call 6 onwards:
Gateway → [Circuit is OPEN] → FallbackController
Response: "Service is temporarily unavailable, please try again later."

Log output:
CircuitBreaker 'myCircuitBreakerName' changed state from CLOSED to OPEN
```

After 10 seconds:
```
CircuitBreaker moves to HALF-OPEN
Allows 3 test calls through
If Product Service is still down → back to OPEN
If Product Service is up → CLOSES
```

---

## Understanding ANY Pre-existing Filter — The Pattern

Now that you've seen several filters, here is the pattern you can use to understand **any** pre-existing filter, even ones not covered in this lecture:

```
STEP 1: Find the filter class name
────────────────────────────────────────────────────────
  In application.properties you use: name=XYZ
  The class name is:                 XYZGatewayFilterFactory.java

STEP 2: Open the class in the framework
────────────────────────────────────────────────────────
  Look for the inner Config class:
  → What fields does it have?
  → Those fields become your args.fieldName=value entries

STEP 3: Configure it
────────────────────────────────────────────────────────
  spring.cloud.gateway.routes[0].filters[N].name=XYZ
  spring.cloud.gateway.routes[N].filters[N].args.field1=value1
  spring.cloud.gateway.routes[N].filters[N].args.field2=value2

Example — AddRequestHeader:
  Class:  AddRequestHeaderGatewayFilterFactory
  Config: NameValueConfig { String name; String value; }
  Props:  filters[0].name=AddRequestHeader
          filters[0].args.name=X-My-Header
          filters[0].args.value=HelloFromGateway
```

---

## The Full Configuration — All Filters Together

Here is what a fully configured route with multiple filters looks like:

```properties
spring.application.name=apigateway
server.port=8083

# Product Service Route
spring.cloud.gateway.routes[0].id=product-service
spring.cloud.gateway.routes[0].uri=lb://product-service
spring.cloud.gateway.routes[0].predicates[0]=Path=/products/**

## ROUTE-SPECIFIC FILTERS for product-service ##

# Filter 0: Add a custom header to every request going to product service
spring.cloud.gateway.routes[0].filters[0].name=AddRequestHeader
spring.cloud.gateway.routes[0].filters[0].args.name=X-TestRequestHeader
spring.cloud.gateway.routes[0].filters[0].args.value=ApiGatewayRequest

# Filter 1: Add a custom header to every response coming back
spring.cloud.gateway.routes[0].filters[1].name=AddResponseHeader
spring.cloud.gateway.routes[0].filters[1].args.name=X-TestResponseHeader
spring.cloud.gateway.routes[0].filters[1].args.value=ApiGatewayResponse

# Filter 2: Strip internal API key before forwarding
spring.cloud.gateway.routes[0].filters[2].name=RemoveRequestHeader
spring.cloud.gateway.routes[0].filters[2].args.name=Api-private-Key

# Filter 3: Retry on failure (GET and DELETE only, max 4 attempts)
spring.cloud.gateway.routes[0].filters[3].name=Retry
spring.cloud.gateway.routes[0].filters[3].args.retries=4
spring.cloud.gateway.routes[0].filters[3].args.methods=GET,DELETE

# Filter 4: Circuit breaker with fallback
spring.cloud.gateway.routes[0].filters[4].name=CircuitBreaker
spring.cloud.gateway.routes[0].filters[4].args.name=myCircuitBreakerName
spring.cloud.gateway.routes[0].filters[4].args.fallbackUri=forward:/fallback

## Circuit Breaker Config ##
resilience4j.circuitbreaker.instances.myCircuitBreakerName.sliding-window-type=COUNT_BASED
resilience4j.circuitbreaker.instances.myCircuitBreakerName.sliding-window-size=10
resilience4j.circuitbreaker.instances.myCircuitBreakerName.minimum-number-of-calls=5
resilience4j.circuitbreaker.instances.myCircuitBreakerName.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.myCircuitBreakerName.wait-duration-in-open-state=10s
resilience4j.circuitbreaker.instances.myCircuitBreakerName.permitted-number-of-calls-in-half-open-state=3

## Eureka ##
eureka.client.service-url.defaultZone=http://localhost:8761/eureka
```

---

## Interview Tips 🎯

> **Q: What is the difference between Retry and CircuitBreaker? Can they be used together?**
> Retry handles occasional, transient failures by attempting the request again. CircuitBreaker handles prolonged failures by stopping calls altogether and returning a fallback. They solve different problems and are often used together — Retry for short blips, CircuitBreaker for sustained outages.

> **Q: What does `forward:/fallback` mean in the CircuitBreaker config?**
> The `forward:` prefix tells the API Gateway this is an internal call — not to an external microservice. `/fallback` is an endpoint defined inside the API Gateway itself. When the circuit is open, the gateway calls its own fallback controller and returns that response to the client.

> **Q: Why do we configure circuit breaker with a name in the filter, then reference that name in the Resilience4j config?**
> The name acts as a key linking the filter (which route to protect) with the Resilience4j config block (how sensitive the circuit breaker should be). This means different routes can have different circuit breaker behaviors — different thresholds, different wait durations — by using different names.

> **Q: Why should POST requests not be retried?**
> POST is not idempotent — calling it multiple times creates multiple resources. Retrying a failed POST could result in duplicate data. Only idempotent operations like GET and DELETE are safe to retry.

---

## Quick Summary of Part 4

- Route-specific filters apply to only the routes you configure them on — not all traffic
- All pre-existing filters follow the same pattern: the name in properties maps to `<Name>GatewayFilterFactory` in the framework
- Each factory class has an inner Config class — its fields are what you set via `.args.fieldName=value`
- Multiple filters on one route are indexed as `filters[0]`, `filters[1]`, `filters[2]`...
- `AddRequestHeader` / `AddResponseHeader` / `RemoveRequestHeader` handle header manipulation
- `Retry` handles transient failures — use only for idempotent methods (GET, DELETE)
- `CircuitBreaker` handles sustained failures — needs Resilience4j dependency, uses a fallback controller inside the gateway itself

---

Ready for **Part 5 — Writing a Custom Route-Specific Filter**?