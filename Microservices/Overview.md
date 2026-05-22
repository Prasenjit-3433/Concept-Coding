Looking at your microservices notes, I'll create a comprehensive overview following the same style as your Spring Boot overview.

---

# Spring Boot Microservices — Complete Course Overview

---

## Lecture 1 — RestTemplate (Inter-Service Communication)

**Communication types:** Synchronous (blocking, caller waits — RestTemplate, RestClient, FeignClient) vs Asynchronous (non-blocking, message brokers like Kafka/RabbitMQ). **HTTP internals covered:** GET/POST request structure (method, URI, host, headers, Content-Type, Content-Length, body), HTTP response structure, HTTP 1.0 (connection closes after every response) vs HTTP 1.1 (Keep-Alive default — reuses TCP connection based on `timeout` and `max` config), WebSocket (persistent + bidirectional).

**Plain Java approach first (`HttpURLConnection`):** Create envelope (URL, method, headers, timeouts) → `getInputStream()` triggers TCP (checks KeepAlive Cache: key=host:port, value=HttpClient object) → read stream manually → `disconnect()` (if stream fully read → return to cache; else close TCP). Problems: 40+ lines of boilerplate, manual stream parsing, no automatic JSON→object conversion.

**RestTemplate:** Abstracts `HttpURLConnection` entirely. Internally: `createRequest()` → `SimpleClientHttpRequestFactory` creates `HttpURLConnection` → `execute()` → checks KeepAlive Cache → `getResponseCode()` sends request → `SimpleClientHttpResponse` wraps response → auto-converts via Jackson → closes stream (NOT TCP, returns to cache). Currently in **maintenance mode** (no new features).

**Setup:** `@Bean RestTemplate` with optional `SimpleClientHttpRequestFactory` for custom connect/read timeouts. Methods: `getForObject` (body only), `getForEntity` (body + status + headers), `postForObject`, `postForEntity`, `put`, `delete`, `exchange` (custom headers/method, Spring handles response conversion), `execute` (full manual control — all other methods call this internally). **Limitations:** explosion of overloaded methods, not built for modern patterns (Retry, Circuit Breaker), maintenance mode → replaced by RestClient.

---

## Lecture 2 — RestClient (Modern Synchronous Communication)

**Why:** RestTemplate maintenance mode, too many overloaded methods. RestClient introduced in Spring Framework 6.0+ / Spring Boot 3.0+. **WebClient** (async/reactive, Spring WebFlux) is the other alternative but covered separately.

**Fluent/Builder API — 4 groups with strict ordering:**
- Group 1 (HTTP method): `.get()/.post()/.put()/.delete()` — always first
- Group 2 (request building): `.uri()` must be first, then `.accept()/.header()` in any order, `.body(object)` last (POST/PUT only)
- Group 3 (trigger): `.retrieve()` (standard) or `.exchange(lambda)` (full manual control, skips Group 4)
- Group 4 (response): `.onStatus(lambda)` before `.body(Class)/.toEntity(Class)/.toBodilessEntity()`

**Setup:** `@Bean RestClient restClientInstance(RestClient.Builder builder)` — must use `Builder` (not `RestClient.create()`) for interceptors to work. **Exception handling:** `.onStatus()` after `.retrieve()` (standard) or `.exchange()` (full control). **Interceptors:** implement `ClientHttpRequestInterceptor`, register via `.builder().requestInterceptor(...)` — fires at Step 2 (execute), before actual HTTP call. **Internals:** `exchangeInternal()` → Step 1 `JdkClientHttpRequestFactory` creates `HttpClient` (java.net.http, supports HTTP/1.1 + HTTP/2) → Step 2 `execute()` (interceptor fires here, then `HttpClient.sendAsync()`, blocking wait) → Step 3 exchange function (response mapping + exception handling). Uses `java.net.http.HttpClient` (Java 11+) unlike RestTemplate, giving HTTP/2 support.

---

## Lecture 3 — FeignClient (Declarative HTTP, Spring Cloud OpenFeign)

**Concept:** Declarative HTTP — you declare *what* to call (interface), framework handles *how*. Part of Spring Cloud OpenFeign (originally Netflix). Integrates seamlessly with Eureka, Load Balancer, Circuit Breaker.

**Setup (4 things):** (1) `@FeignClient(name="product-service", url="${...}")` on interface — name must match `spring.application.name` of target. (2) `application.properties` with base URL. (3) `@Autowired` the interface in controller/service. (4) `@EnableFeignClients` on main class — mandatory, without it nothing scans. **Dependency management:** `spring-cloud-starter-openfeign` + `spring-cloud-dependencies` BOM in `<dependencyManagement>` (no versions on individual dependencies, BOM ensures compatibility).

**Annotations on interface methods:** Same as controller annotations — `@GetMapping/@PutMapping`, `@PathVariable`, `@RequestParam`, `@RequestHeader`, `@RequestBody`. Parameter order doesn't matter (matched by annotation, not position). `consumes="application/json"` sets Content-Type header.

**Internals (3 steps):** `@EnableFeignClients` triggers `ReflectiveFeign.newInstance()` → Step 1: per method creates `MethodHandler` (holds targetURL, httpMethod, headers, encoder, decoder, errorDecoder, retryer, logger, httpClient) → Step 2: `InvocationHandler` stores Map{method→MethodHandler} → Step 3: Dynamic Proxy creates runtime implementation of interface, registered as Spring Bean, injected by `@Autowired`. At runtime: proxy → `InvocationHandler.invoke()` → `MethodHandler.invoke()` → `executeAndDecode()` (encode request → HTTP call → decode response or errorDecoder).

**Per-client configuration via `@FeignClient(configuration=MyConfig.class)`:**
- **Encoder** (Java→JSON, default SpringEncoder/Jackson): implement `Encoder` interface, `@Bean` in config class
- **Decoder** (JSON→Java, default SpringDecoder/Jackson): implement `Decoder` interface, `@Bean` in config class
- **ErrorDecoder** (non-2xx responses): implement `ErrorDecoder`, check `response.status()`, throw custom exceptions, delegate unhandled to `new Default()`. 4xx/5xx → directly to ErrorDecoder; network errors → Retryer first
- **Retryer** (connection timeout/IOException only): `Retryer.NEVER_RETRY` (disable), extend `Retryer.Default` (change numbers only), implement `Retryer` (full control — override `continueOrPropagate()` and `clone()`). Must override `clone()` — FeignClient clones per request for fresh attempt counter
- **Timeouts** (in `application.properties`): per-client `feign.client.config.{name}.connectTimeout/readTimeout` or global `feign.client.config.default.*`. Per-client takes priority. `name` in `@FeignClient` matches the config key.

---

## Lecture 4 — Service Discovery (Netflix Eureka)

**Problem with hardcoded URLs:** Single point of failure, no load balancing, tight coupling (IP:port baked in), difficulty testing across environments. **Solution:** Eureka Server (central registry/phone book) + Eureka Clients (all microservices).

**Eureka Server setup (3 things):** `spring-cloud-starter-netflix-eureka-server` + `spring-cloud-dependencies` BOM → `@EnableEurekaServer` on main class → `application.properties`: `server.port=8761`, `register-with-eureka=false`, `fetch-registry=false`.

**Eureka Client setup:** `spring-cloud-starter-netflix-eureka-client` → `application.properties`: `spring.application.name=product-service` (becomes the registry key), `eureka.client.service-url.defaultZone=http://localhost:8761/eureka`, `register-with-eureka=true` (default), `fetch-registry=true` (default). No special annotation needed — auto-configured.

**Full flow:** Client fetches from Config Server (only at startup, cached locally) → requests use local cache → cache refreshes every 30s (`eureka.client.registry-fetch-interval-seconds=30`). **Heartbeat:** client sends every 30s (`eureka.instance.lease-renewal-interval-in-seconds`); server waits `eureka.instance.lease-expiration-duration-in-seconds` before removing. **Self-preservation mode** (`eureka.server.enable-self-preservation=false` for testing) — prevents mass removal on network partition. **Eviction interval** (`eureka.server.eviction-interval-timer-in-ms`) — how often server checks for dead instances.

**Data storage:** In-memory only (`Map<String, Lease<InstanceInfo>>`, key=`appName/instanceId`). No DB persistence — if Eureka crashes, all data lost. **HA:** 3-node cluster — each server is also a client to the other two (`register-with-eureka=true`, `fetch-registry=true`, `defaultZone` pointing to other two servers). **Consistency model:** Eventual consistency (data eventually replicates, no guarantee all servers identical at same moment).

**Calling services after discovery:** `DiscoveryClient.getInstances("product-service")` returns list of `ServiceInstance` → manual `get(0)` to pick (no load balancing). Or use FeignClient `@FeignClient(name="product-service")` (no URL, resolves via Eureka automatically).

---

## Lecture 5 — Load Balancer (Spring Cloud Load Balancer)

**Two types:** Server-side LB (centralized — Nginx, AWS ELB, no client awareness) vs Client-side LB (logic inside client — Spring Cloud LB, uses Service Discovery as prerequisite).

**Problem with manual approach:** `DiscoveryClient.getInstances().get(0)` always picks first instance — no real load balancing, messy infrastructure code in controller.

**Spring Cloud Load Balancer with RestTemplate:** Add `spring-cloud-starter-loadbalancer` dependency → `@Bean @LoadBalanced RestTemplate` — this attaches `LoadBalancerInterceptor`. Use service name in URL: `http://product-service/products/` (not hardcoded host:port). **Internally:** `LoadBalancerInterceptor` → `loadBalancerClientFactory.getInstance(serviceId)` picks algorithm for that serviceId → `loadBalancer.choose(request)` calls Eureka → gets instances → applies algorithm → resolves URL. **ServiceId** = `spring.application.name` of target service — every algorithm is tied to a specific serviceId, not global.

**Built-in algorithms:** Only 2 — `RoundRobinLoadBalancer` (default) and `RandomLoadBalancer`. Both implement `ReactorServiceInstanceLoadBalancer` interface. Netflix Ribbon is deprecated, Spring Cloud LB is the replacement.

**Overriding algorithm per-service:** `@LoadBalancerClient(name="product-service", configuration=LoadBalancerProductClientConfig.class)` on main class. Config class: `@Configuration` + `@Bean ReactorLoadBalancer<ServiceInstance>` returning `new RandomLoadBalancer(factory.getLazyProvider("product-service", ServiceInstanceListSupplier.class), "product-service")`. **Key detail:** LoadBalancer config is LAZY (runs at runtime on first call, not at startup — serviceId would be null at startup).

**Global + per-service config:** `@LoadBalancerClients(defaultConfiguration=LoadBalancerGlobalConfig.class, value={@LoadBalancerClient(...)})`. Global config needs `@ConditionalOnMissingBean(ReactorLoadBalancer.class)` — prevents duplicate bean crash when both specific and global configs fire for same service. Global config uses `environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME)` for dynamic serviceId (resolved at runtime). Loading order: specific config first → global config second.

**Custom load balancer:** Implement `ReactorServiceInstanceLoadBalancer` → override `choose(Request)` → get instances via `serviceInstanceSuppliers.getIfAvailable().get().next()` → apply custom algorithm → return `new DefaultResponse(instance)` or `new EmptyResponse()`. Register as `@Bean` in config class with same constructor pattern as built-in algorithms.

**FeignClient + Load Balancer:** FeignClient internally already uses `LoadBalancerInterceptor` — automatic, no `@LoadBalanced` annotation needed. Still requires `spring-cloud-starter-loadbalancer` dependency. Override algorithm via same `@LoadBalancerClient` approach — identical to RestTemplate.

---

## Lecture 6 — Resilience4j (Fault Tolerance)

**Why:** Cascading failures — one slow downstream blocks all threads, exhausts thread pool, brings down entire service. **Recommended ordering:** Rate Limiter → Bulkhead → Time Limiter → Circuit Breaker → Retry. **Single dependency:** `resilience4j-spring-boot3` version `2.1.0` covers all 5 mechanisms. **All mechanisms use Spring AOP internally** — must be public method on Spring-managed bean.

### Rate Limiter
**5 algorithms:** Fixed Window Counter (simple, edge spike problem), Sliding Log (exact timestamps, high memory), Sliding Window Counter Sub-Window (count per sub-window), Sliding Window Counter Weighted (approximation, assumes uniform distribution), Token Bucket (Resilience4j default — bucket refills at interval, burst risk if capacity too high), Leaky Bucket (fixed output rate, latency/queue bottleneck).

**Implementation:** `@RateLimiter(name="productRateLimiter", fallbackMethod="rateLimitedFallback")` on method. Fallback: same return type + same params + extra `Throwable` param. **Config (`application.properties`):** `resilience4j.ratelimiter.instances.productRateLimiter.limitForPeriod=2` (tokens/bucket), `.limitRefreshPeriod=10s` (refill interval), `.timeoutDuration=1s` (wait before reject). **Custom via AOP:** `@Target(METHOD) @Retention(RUNTIME)` custom annotation + `@Aspect @Component` class with `@Around("@annotation(...)")` + `ProceedingJoinPoint.proceed()`.

### Bulkhead
**Two types:** Semaphore (controls max concurrent calls via counter/permit — downstream has hard concurrency limit) vs Thread Pool (dedicated thread pool per downstream — prevents noisy neighbor from starving others, solves Thundering Herd scoped to threads).

**Semaphore:** `@Bulkhead(name="productService", type=Bulkhead.Type.SEMAPHORE, fallbackMethod="...")`. Normal return type. Config: `resilience4j.bulkhead.instances.productService.maxConcurrentCalls=2`, `.maxWaitDuration=0` (0=reject immediately, or `300ms`/`2s`/`1m`).

**Thread Pool:** `@Bulkhead(name="productService", type=Bulkhead.Type.THREADPOOL, fallbackMethod="...")`. Return type **must be `CompletableFuture<T>`**. Inside method use `CompletableFuture.completedFuture(result)` — NOT `supplyAsync()` (that would use default pool, bypassing bulkhead pool). AOP internally does `CompletableFuture.supplyAsync(() -> yourMethod(), bulkheadThreadPoolExecutor)`. Config prefix: `resilience4j.thread-poolbulkhead.instances.productService.coreThreadPoolSize/maxThreadPoolSize/queueCapacity`. Thread names show `bulkhead-productService-N` confirming dedicated pool.

### Circuit Breaker
**3 states:** CLOSED (calls flow, CB monitors) → OPEN (all calls blocked instantly, fail-fast, fallback runs) → HALF-OPEN (limited test calls) → back to CLOSED (100% test calls pass) or OPEN (any test call fails). CLOSED = calls flow (opposite of intuition). Fallback runs on every failure — CLOSED or OPEN.

**Implementation:** `@CircuitBreaker(name="productService", fallbackMethod="fallback")` on method. No separate dependency — included in `resilience4j-spring-boot3`. **Config:** `sliding-window-type=COUNT_BASED`, `sliding-window-size=10`, `minimum-number-of-calls=5` (prevents false alarms on low traffic), `failure-rate-threshold=50`, `wait-duration-in-open-state=10s`, `permitted-number-of-calls-in-half-open-state=3`, `automatic-transition-from-open-to-half-open-enabled=true`.

**Internals:** AOP → `CircuitBreakerStateMachine` (checks thresholds after every call, `isClosed.compareAndSet(true, false)` for thread-safe transition, `transitionToOpenState()`). OPEN→HALF-OPEN timer: `OpenState` constructor → `ScheduledThreadPoolExecutor.schedule(toHalfOpenState, waitDurationMs, MILLISECONDS)` → `DelayedQueue` (priority queue sorted by min remaining delay) → worker thread waits → OS wakes → executes transition. Works only if `automatic-transition-from-open-to-half-open-enabled=true`.

### Retry
**When to retry:** Never on 4xx (permanent failures), never on non-idempotent operations (POST creates duplicates), yes on 5xx/network errors (idempotent only), yes on 429 (with delay). **Why internal retry:** avoids redoing 90% of work already done upstream.

**4 strategies:** Fixed Interval (simple, Thundering Herd risk), Exponential Backoff (`enableExponentialBackoff=true`, `exponentialBackoffMultiplier=2`, formula: `base × factor^failed_attempts`, capped at `maxBackoffInterval`), Exponential Backoff + Jitter (`enableRandomizedWait=true`, formula: `random(0, min(maxDelay, base × 2^failed))`, desynchronizes clients, solves Thundering Herd — industry standard), Custom (`IntervalFunction` lambda, `RetryConfig.custom().intervalFunction(...).build()`, `Retry.of("name", config)`, `customRetry.executeSupplier(() -> {...})`).

**Implementation (strategies 1-3):** `@Retry(name="productService", fallbackMethod="productFallback")`. Config: `resilience4j.retry.instances.productService.maxAttempts=3`, `.waitDuration=2s`. **Custom (strategy 4):** Remove `@Retry` — manually build `IntervalFunction` + `RetryConfig` + `Retry.of()` as `@Bean`, inject and call `customRetry.executeSupplier(() -> {...})` with try-catch as fallback. **Internals (AOP for 1-3):** reads config → picks `IntervalFunction.of()/ofExponentialBackoff()/ofExponentialRandomBackoff()` → builds `RetryConfig` → `Retry.of()` → `executeSupplier()` wraps method.

**Time Limiter:** For async/reactive calls (Mono/Flux in WebFlux). Blocking calls (RestTemplate/FeignClient) use `connectTimeout`/`readTimeout` in client config instead — covered with WebFlux separately.

---

## Lecture 7 — API Gateway (Spring Cloud Gateway)

### Part 1 — Routing & Load Balancing
**Why:** Without gateway, clients know all microservice addresses — any service change requires updating all clients. Gateway = single entry point, decouples clients from services. Additional benefits: routing, load balancing, auth, rate limiting, circuit breaker, request/response transformation, centralized logging.

**Setup:** New Spring Boot project with `spring-cloud-starter-gateway` (Reactive Gateway, NOT Spring Web). No controller needed — all config in `application.properties`. **Route config:** `spring.cloud.gateway.routes[N].id`, `.uri` (target), `.predicates[N]` (conditions). **Predicates:** `Path=/products/**` (most common), `Method=GET,POST`, `Header=...` — ALL predicates must match. `**` = wildcard for any path segment.

**Load balancing integration:** Add `spring-cloud-starter-netflix-eureka-client` to gateway. Change `.uri` from `http://localhost:8082` (hardcoded) to `lb://product-service` (load balanced). `lb://` prefix triggers Spring Cloud Load Balancer — asks Eureka for instances, picks one via Round Robin, resolves URL. `product-service` must match `spring.application.name` of target exactly.

### Part 2 — Filters (Authentication, Circuit Breaker, etc.)
**Two filter types:** Global (`GlobalFilter` — all requests, good for auth/logging) vs Route-specific (`GatewayFilterFactory` — per route, good for retry/circuit breaker/header manipulation).

**Filter ordering:** All filters (global + route-specific) in ONE chain sorted by order value. Lower = earlier. Built-in: `NettyWriteResponseFilter` (order=-1, pre=empty, post=sends response — runs last on response because entered first), custom `GlobalFilter` (you set order, e.g. -1), route-specific (index 0,1,2...), `RouteToRequestUrlFilter` (order=10000 — resolves `lb://` to real URL), `NettyRoutingFilter` (order=MAX — actually calls microservice). Pre logic runs forward, post logic unwinds backward.

**Custom Global Filter (JWT Auth):** `implements GlobalFilter, Ordered`. `filter(ServerWebExchange exchange, GatewayFilterChain chain)` → pre logic before `chain.filter(exchange)`, post logic inside `.then(Mono.fromRunnable(() -> {...}))`. Uses Spring WebFlux (reactive) — non-blocking. `getOrder()` returns -1 to run first. Skips `/auth/**` paths, reads `Authorization: Bearer <token>`, verifies via `JWTUtil.validateAndExtractClaims()`, extracts username+role from claims, adds `X-Username` and `X-User-Role` headers to modified request, forwards. Invalid token → `exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED)` + `setComplete()`.

**Auth architecture in microservices:** Auth Service handles user registration/login/token generation (has UserDetailsService, BCrypt, JWTUtil, user DB — same as monolith). API Gateway verifies JWT once centrally (secret key only in gateway). Individual microservices read `X-User-Role` header, build `Authentication` object via `GatewayAuthFilter extends OncePerRequestFilter`, store in `SecurityContextHolder` → `@PreAuthorize` works normally. No re-verification in microservices — secret key stays in one place.

**Route-specific filters — naming pattern:** `{FilterName}GatewayFilterFactory` in framework, you use just `{FilterName}` in properties with `.args.field=value`. Config: `spring.cloud.gateway.routes[N].filters[N].name=FilterName`.
- `AddRequestHeader` / `AddResponseHeader`: `args.name`, `args.value`
- `RemoveRequestHeader`: `args.name`
- `Retry`: `args.retries=4`, `args.methods=GET,DELETE` (idempotent only — POST not retried, creates duplicates)
- `CircuitBreaker`: needs `spring-cloud-starter-circuitbreaker-reactor-resilience4j`, `args.name=myCircuitBreakerName`, `args.fallbackUri=forward:/fallback` (internal call to `FallbackController` inside gateway itself). Circuit breaker config: `resilience4j.circuitbreaker.instances.myCircuitBreakerName.*` — same properties as Lecture 6.

**Custom route-specific filter:** Implement `AbstractGatewayFilterFactory<ConfigClass>` → `@Override apply(Config config)` returns `GatewayFilter` lambda → access `args.field` via inner `Config` class. Register as `@Component`.

---

## Lecture 8 — Central Configuration (Spring Cloud Config)

### Part 1 — Spring Cloud Config Server & Client
**4 problems with local `application.properties`:** Rebuild/redeploy for every change, inconsistent config across services (same property in multiple services, easy to miss updates), no runtime update (loaded once at startup), time-consuming rollback (manual fix + rebuild + redeploy per service).

**3-layer architecture:** Git Repo (plain repo, `.properties` files only) → Config Server (Spring Boot app, fetches from Git, caches, serves via REST) → Microservices (fetch from Config Server at startup).

**File naming convention in Git repo:** `application.properties` (all services, all profiles), `application-{profile}.properties` (all services, specific profile), `{service-name}.properties` (specific service, all profiles), `{service-name}-{profile}.properties` (specific service + profile). Service name must exactly match `spring.application.name`.

**Precedence (highest→lowest):** `{service}-{profile}.properties` (Git) → `application-{profile}.properties` (Git) → `{service}.properties` (Git) → `application.properties` (Git) → `application-{profile}.properties` (local) → `application.properties` (local). Central Git config always overrides local.

**Config Server setup:** `spring-cloud-config-server` dependency + `@EnableConfigServer` on main class. `application.properties`: `server.port=8888`, `spring.cloud.config.server.git.uri`, `.username=${GIT_USERNAME}`, `.password=${GIT_ACCESS_TOKEN}` (never hardcode — env vars), `.search-paths=global,orderservice` (if organized in folders), `.clone-on-start=true` (download at startup, not on first request), `.default-label=main`. Test endpoint: `GET http://localhost:8888/{application}/{profile}/{label}` returns JSON array of property sources in precedence order.

**Config Client setup:** `spring-cloud-starter-config` dependency. No annotation needed — auto-configured. `application.properties`: `spring.application.name=order-service`, `spring.config.import=optional:configserver:http://localhost:8888` (`optional:` prefix = app starts even if Config Server down, uses local fallback; without `optional:` = app refuses to start if Config Server down), `spring.profiles.active=dev`. `@Value` works identically regardless of property source.

### Part 2 — Spring Boot Actuator
**Purpose:** Production-ready monitoring endpoints out of the box. **Dependency:** `spring-boot-starter-actuator`. **Config:** `management.endpoints.web.base-path=/manage` (default `/actuator`), `management.endpoints.web.exposure.include=*` (default: only `health` and `info`).

**Key endpoints:** `/health` (aggregated UP/DOWN, `show-details=always` for component breakdown — custom `HealthIndicator`: `@Component implements HealthIndicator`, `health()` returns `Health.up()/.down().withDetail(...).build()`, any sub-component DOWN = overall DOWN), `/metrics` (two-level: list names first, then `/metrics/{name}` for data — JVM memory, GC pause COUNT/TOTAL_TIME/MAX, threads live/peak, CPU 0.0-1.0, HTTP server requests, JDBC connections active/idle/max, executor pool/active/queued), `/threaddump` (per-thread: threadName, threadState RUNNABLE/WAITING/BLOCKED/TIMED_WAITING, stackTrace, blockedCount/waitedCount — use for deadlock/hung thread/leak detection).

**Critical endpoints (restricted by default even with `*`):** `/shutdown` (POST kills app — prefer process managers systemd/Kubernetes/Docker) and `/heapdump` (downloads `.hprof` — exposes passwords/tokens/PII in plaintext). Enable via `management.endpoint.shutdown.access=unrestricted` AND `management.endpoint.heapdump.access=unrestricted` (both properties + exposure include required).

**Security:** `spring-boot-starter-security` → `SecurityFilterChain` bean with `HttpSecurity` DSL — `permitAll()` for `/health`/`/info` (monitoring tools need these without auth), `hasRole("ADMIN")` for everything else, `.csrf(csrf -> csrf.disable())` (actuator clients don't send CSRF tokens), basic auth for demo (prod: JWT/OAuth2), credentials in `spring.security.user.name/password/roles` (test only).

**Custom endpoints:** `@Endpoint(id="x") @Component` → `id` becomes URL path. `@ReadOperation` (GET), `@WriteOperation` (POST — must secure), `@DeleteOperation` (DELETE — must secure). `@Selector` on params = path variable equivalent, positional. Spring matches by HTTP method + selector count. Same mechanism used by Spring Cloud Config `/refresh` and Resilience4j circuit breaker endpoints.

**Micrometer + Datadog:** Actuator alone is pull-based manual. Micrometer = vendor-neutral metrics facade (like SLF4J for metrics). Add `micrometer-registry-datadog` → `management.datadog.metrics.export.api-key=${DATADOG_API_KEY}` (never hardcode), `.enabled=true`, `.step=5s` (push interval). Prometheus alternative: `micrometer-registry-prometheus` (pull-based, scrapes `/prometheus`). Swap registry dependency to change platform — zero app code change.

### Part 3 — @ConfigurationProperties
**Problems with `@Value`:** 1-to-1 annotation per property (doesn't scale), no built-in validation. **`@ConfigurationProperties`:** maps group of related properties to one Java object. Advantages: structured, reusable (inject anywhere via `@Autowired`), validated.

**3-step internal sequence:** (1) Spring IoC creates empty bean via `@Component` + no-arg constructor, (2) Configuration Binder reads `application.properties` and calls **setter methods** to fill fields (why setters are mandatory), (3) `@Validated` triggers validation annotations after binding — failure = app refuses to start.

**Prefix matching:** `@ConfigurationProperties(prefix="user")` → maps `user.*` properties to fields by name. Relaxed binding: camelCase/underscore/lowercase all work, but instructor recommends exact match.

**Binding types:** Flat fields (String/int/boolean), Nested objects (must be `public static class` — non-static has no no-arg constructor, Reflection fails, binding fails silently), `List<String>` (`user.roles[0]=ADMIN`), `List<StaticClass>` (`user.course[0].name=Java`), `Map<String,String>` (`user.preferences.theme=dark`), `Map<String,StaticClass>` (`user.locations.home.city=Jaipur`).

**Validation:** Add `spring-boot-starter-validation` + `@Validated` on class + annotations on fields (`@NotBlank`, `@NotNull`, `@NotEmpty`, `@Min`, `@Max`, `@Positive`, `@PositiveOrZero`, `@Email`, `@Pattern`, `@AssertTrue/@AssertFalse`). Without `@Validated`, all annotations silently ignored.

**Immutability (Constructor Binding):** Problem: setter-based = mutable (values change between empty bean and after binding). Solution: remove `@Component`, make fields `final`, only parameterized constructor (no setters), add `@ConfigurationPropertiesScan` on main class — Configuration Binder reads properties first then calls constructor directly in one shot. Validation still works the same way.

### Part 4 — @RefreshScope
**Problem:** Config Server and microservices fetch properties only at startup — Git updates not reflected in running apps without restart. **Solution:** `@RefreshScope` marks bean as eligible for dynamic refresh (destroy old bean + create new with latest values) triggered by `POST /actuator/refresh`.

**Where to put it:** ONLY on stateless beans — best on `@ConfigurationProperties` POJOs (only holds config, no object state). **NEVER on Controller/Service** — `@RefreshScope` destroys and recreates the bean on refresh, losing all instance variables (e.g., counter resets), and can drop in-flight requests mid-execution.

**Trigger mechanism:** Spring Cloud Config provides `RefreshBusEndpoint` as custom actuator endpoint (`@Endpoint(id="refresh") @WriteOperation`). On Config Server: fetches latest from Git. On microservice: calls Config Server for latest → finds `@RefreshScope` beans → destroys + recreates. Returns list of refreshed property keys. Requires actuator dependency + `management.endpoints.web.exposure.include=refresh`.

**Limitation:** Must call `/actuator/refresh` on every service individually — 100 services = 100 manual calls.

### Part 5 — Spring Cloud Bus
**Problem solved:** Call refresh at one place (Config Server), all microservices automatically refresh. **Concept:** wraps a message broker (RabbitMQ or Kafka) so Spring's existing event mechanism works across microservices.

**Single-app events (foundation):** `publishEvent(new MyEvent extends ApplicationEvent)` → `SimpleApplicationEventMulticaster` → iterates registered `@EventListener` methods (Observer pattern, synchronous, same thread). At startup, Spring builds map of event-type→listeners.

**Spring Cloud Bus extends this:** Publisher calls `publishEvent(extends RemoteApplicationEvent)` → Bus intercepts (serializes to JSON) → pushes to message broker → deserializes at listener side → `SimpleApplicationEventMulticaster` → `@EventListener`. You still just write `publishEvent()` and `@EventListener` — Bus handles everything between.

**What Bus abstracts (and why you lose control):** Exchange/queue/topic creation, retry mechanism, message persistence, ACK logic, Kafka offset management. **Use only for:** low-volume non-critical messages (config refresh, cache clear). **Never for:** high-volume/critical business events (order placed, payment) — use Spring Cloud Stream or KafkaTemplate directly.

**Custom remote events:** `extends RemoteApplicationEvent` (not `ApplicationEvent`). Constructor: `(source, originService, destination, message)`. `originService` must match publishing app's **Bus ID** — by default `{app-name}:{port}:{random-UUID}` (unpredictable). Override: `spring.cloud.bus.id=${spring.application.name}-${server.port}`. `destination="*"` = broadcast all, `null` = same as `*`, specific name = targeted. `@RemoteApplicationEventScan(basePackages="com.concepts")` on listener's main class — needed for JSON deserialization (finding event class). Share event class via common module (prod) or duplicate (dev/testing).

**Dynamic config refresh via Bus:** Config Server + each microservice: add `spring-cloud-starter-bus-amqp` + RabbitMQ credentials. Expose `busrefresh` on Config Server. Call `POST /actuator/busrefresh` on Config Server ONCE → `RefreshBusEndpoint` (framework) publishes `RefreshRemoteApplicationEvent` with `destination=null` → RabbitMQ broadcasts to ALL apps → each app's `RefreshListener` (framework class, pre-provided) calls `contextRefresher.refresh()` → Config Server refreshes from Git, microservices refresh from Config Server → all `@RefreshScope` beans recreated. No custom publisher/listener code needed — just dependency + credentials. `/actuator/refresh` (per-service manual) vs `/actuator/busrefresh` (one call, all services).

---

## Lecture 9 — Distributed Tracing (Micrometer + OpenTelemetry + Jaeger)

**Logs vs Tracing:** Logs = detailed events inside one service (islands, no cross-service connection). Distributed Tracing = end-to-end request path across all services (which services, how long each, which failed). They complement — tracing finds *where*, logs explain *what happened inside*. Modern distributed logging stamps logs with Trace ID so tools like Kibana can filter all logs for one request across services.

**Core concepts:** **Trace ID** = one per API request, shared across ALL services (the thread connecting everything). **Span ID** = one per unit of work inside each service (timer with start/end). **Parent Span ID** = links spans into hierarchy/tree. Together enable reconstructing full call tree with timing. Auto-created spans: incoming HTTP request, thread handoff, outgoing HTTP call.

**Legacy flow (deprecated):** Spring Cloud Sleuth (hardcoded to Brave library) → Brave generates Zipkin-specific format → only works cleanly with Zipkin. Problem: Jaeger/Grafana use different formats, required hacks. Why deprecated: new backends emerged that Sleuth couldn't support cleanly.

**Modern flow:** Micrometer (interface/APIs only — comes via `spring-boot-starter-actuator`) + OpenTelemetry SDK (`micrometer-tracing-bridge-otel` — implementation of Micrometer APIs, does actual work) + OTLP Exporter (`opentelemetry-exporter-otlp` — reads completed spans from in-memory async queue, pushes to backend). **OTLP** = OpenTelemetry Protocol, universal format understood by Zipkin/Jaeger/Grafana — switching backends = just change the endpoint URL, zero code change.

**Setup (3 dependencies per service):**
```xml
spring-boot-starter-actuator          <!-- Micrometer -->
micrometer-tracing-bridge-otel        <!-- OTel SDK -->
opentelemetry-exporter-otlp           <!-- OTLP exporter -->
```
**`application.properties`:** `management.tracing.sampling.probability=1.0` (default ~10-30%, set 1.0 for dev/testing), `management.otlp.tracing.endpoint=http://localhost:4318/v1/traces`. **Jaeger via Docker:** `docker run -d -p 16686:16686 -p 4317:4317 -p 4318:4318 jaegertracing/all-in-one:latest` — UI at `localhost:16686`, OTLP HTTP at port 4318.

**Trace propagation internals:** Two automatic mechanisms. (1) **`ServerHttpObservationFilter`** (incoming side, auto-registered) — checks for `traceparent` header: not found = new Trace ID + Span ID (root), found = extract Trace ID, reuse it, new Span ID, set parent from header. (2) **Interceptor** (outgoing side) — auto-added when using `RestClient.Builder` — appends `traceparent: {traceID}-{spanID}` header to outgoing request.

**Critical HTTP client rules:**
- `RestClient` via `RestClient.Builder` (inject as parameter) → interceptor auto-added → `@Bean RestClient rc(RestClient.Builder b) { return b.build(); }` ✓
- `RestClient.create()` → no interceptor → App B creates new Trace ID → broken separate traces ✗
- `RestTemplate` → manual wiring: inject `ObservationRestTemplateCustomizer`, call `customizer.customize(restTemplate)` ✓
- `FeignClient` → auto-configured, works out of the box ✓

**Manual span creation (for DB calls, heavy ops):**
```java
@Autowired Tracer tracer;

Span parentSpan = tracer.currentSpan();                           // Step 1: get current (becomes parent)
Span childSpan = tracer.nextSpan(parentSpan).name("op-name");    // Step 2: create child (MUST pass parent)
childSpan.start();                                                // Step 3: start timer
Tracer.SpanInScope scope = tracer.withSpan(childSpan);           // Step 4: update current span pointer
try {
    // Step 5: your work here
} finally {
    scope.close();     // Step 6a: restore parent as current span
    childSpan.end();   // Step 6b: stop timer → span exported
}
```
`start()` ≠ `withSpan()` — start only starts timer, `withSpan()` updates current span pointer (needed so inner spans parent correctly). `nextSpan()` without parent = new Trace ID = orphan span (broken). Always use `finally` block.

---

# Lecture 10 — Distributed Logging (4 Parts)

**Core Concepts:** SLF4J, Logback, Log4j2, Appenders, Log Levels, Parent-Child Hierarchy, Propagation, Async Logging, Structured JSON Logging, MDC, Correlation ID, Distributed Tracing

---

## Part 1 — SLF4J, Logback, Log4j2, Levels, Parent-Child Loggers

**What logging is:** Recording runtime application events (requests, responses, errors, business milestones) to a destination so developers can monitor and debug what actually happened.

**The layered architecture:**
- **SLF4J** (`slf4j-api.jar`) — only an interface/facade, zero implementation. Your code calls `log.info()`, `log.error()` etc. against SLF4J. Never tied to a specific library.
- **Logback** — Spring Boot's default implementation of SLF4J. Pulled in automatically via `spring-boot-starter` → `spring-boot-starter-logging` → `slf4j-api` + `logback-classic` + `logback-core`. Zero configuration needed.
- **Log4j2** — alternative implementation. To use it: explicitly exclude `spring-boot-starter-logging` from `spring-boot-starter`, then add `spring-boot-starter-log4j2`. If both are on the classpath without exclusion, `LoggerFactory` does a `get(0)` on the provider list — unpredictable which one runs. Only one implementation allowed.
- **Appender** — component inside the implementation library that decides the destination (console, file, DB, Kafka). Without one, accepted logs go nowhere.

**Getting a Logger:**
```java
Logger log = LoggerFactory.getLogger(PaymentController.class);
```
`LoggerFactory` detects which implementation is on the classpath, delegates to its `LoggerContext`, which checks a `Map<String, Logger>` cache — one logger object per name, reused forever.

**Log levels (highest → lowest):** `ERROR → WARN → INFO (default) → DEBUG → TRACE`. A logger prints a statement only if the statement's level ≥ the logger's configured level. Changed via `application.properties`:
```properties
logging.level.com.concepts.PaymentController=DEBUG
```

**Parent-child logger hierarchy:** Calling `LoggerFactory.getLogger(PaymentController.class)` silently creates an entire chain — `ROOT → com → com.concepts → com.concepts.PaymentController` — each as a separate cached Logger object. Three advantages:
1. **Level inheritance** — set `logging.level.com=DEBUG` once; all children with `level=null` inherit it automatically.
2. **Per-child override** — set `logging.level.com.concepts.PaymentController=WARN` to override a specific class while the rest inherit the parent level.
3. **Log propagation** — accepted logs travel upward executing every appender found along the way.

**Propagation rule:** If a log statement is **rejected** (level too low) → stop, nothing propagates. If **accepted** → travel upward to ROOT, executing every appender encountered. Level is checked **only once** at the originating logger — parent logger levels are never re-checked during propagation.

**Why logs appear on console with zero config:** ROOT logger always exists, always has a Console Appender by default. Every accepted log propagates up to ROOT and gets printed there.

**`additivity=false`:** Stops propagation at that logger. Log is handled only by its own appenders. Common use case: audit logs or payment logs that must go only to a dedicated file, not mixed into general console output. Rule: if `additivity=false`, the logger must have at least one appender attached or logs vanish entirely.

---

## Part 2 — Appenders: Console, File, Rolling File, Custom

**`logback-spring.xml`** — required for full appender control. File must be at `src/main/resources/logback-spring.xml` exactly. Spring Boot auto-detects this filename. Three building blocks in order: **Appenders** (define destinations) → **Loggers** (map logger names to appenders, set level + additivity) → **Root** (fallback for everything unconfigured).

**Appender vs Encoder distinction:** Appender decides **where** the log goes. Encoder (inside the appender) decides **how** it looks (the format/pattern). Without an encoder, the appender won't work.

**Pattern placeholders** come from `PatternLayout.java` in Logback — `%d` (date), `%-5level` (level padded to 5 chars), `%logger` (full class name), `%msg` (message), `%n` (newline), `%thread` (thread name), `%X` (all MDC values), `%X{key}` (specific MDC value). Each maps to a converter class (e.g. `LevelConverter`, `DateConverter`).

**Level inheritance vs additivity — critical distinction:** `additivity` controls **appender propagation only**. Level is **always** inherited from parent regardless of `additivity` setting. If a logger has no level defined and `additivity=false`, it still looks upward to inherit level — just doesn't propagate logs to parent appenders.

**`application.properties` vs `logback-spring.xml` priority:** `application.properties` wins. If level is defined in both, the properties file overrides.

---

### Console Appender
```xml
<appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
        <pattern>%d %-5level %logger - %msg%n</pattern>
    </encoder>
</appender>
```
Writes to **STDOUT** (standard output stream). Java only knows STDOUT — who reads it depends on the platform: local terminal, Docker logging driver (`docker logs <container>`), Azure Monitor, etc. Fast, no persistence, no disk risk. Platform handles log collection automatically.

**Common production bug:** If both a specific logger and ROOT have Console Appender, and `additivity=true` (default), the log prints twice. Fix: `additivity="false"` on the specific logger.

---

### File Appender
```xml
<appender name="FILE" class="ch.qos.logback.core.FileAppender">
    <file>logs/app.log</file>
    <append>true</append>
    <encoder>...</encoder>
</appender>
```
Writes to a file on disk. `append=true` — new logs added after existing ones across restarts. `append=false` — file wiped clean on restart. **Not production-ready alone** — no size control, file grows forever, disk fills up and server crashes.

---

### Rolling File Appender — Time-Based Rolling Policy
```xml
<appender name="ROLLING" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/app.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <fileNamePattern>logs/app-%d{yyyy-MM-dd}.log</fileNamePattern>
        <maxHistory>30</maxHistory>
    </rollingPolicy>
    <encoder>...</encoder>
</appender>
```
Two-file concept: `app.log` = active current file. When time period ends (minute/hour/day depending on `%d{}` pattern), current file is archived with timestamp in name, fresh `app.log` created. `maxHistory=30` means keep last 30 units (days if daily rotation, minutes if minute-wise). Older archived files auto-deleted. **Still not fully production-ready** — controls when a new file is created, but not how big each file can grow.

---

### Rolling File Appender — Size + Time Based Rolling Policy ✅ Production-Ready
```xml
<rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
    <fileNamePattern>logs/app-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
    <maxFileSize>100MB</maxFileSize>
    <maxHistory>30</maxHistory>
    <totalSizeCap>2GB</totalSizeCap>
</rollingPolicy>
```
Adds two controls on top of time-based: `maxFileSize` — once a single file hits this limit (even mid-day), it's immediately archived and a fresh `app.log` created. `%i` in the filename pattern is an auto-incrementing index (0, 1, 2…) differentiating multiple files for the same day (`app-2025-12-19.0.log`, `app-2025-12-19.1.log`). `totalSizeCap` — hard limit on combined size of all log files; oldest file auto-deleted when cap is exceeded. Whichever limit fires first (maxHistory or totalSizeCap) triggers cleanup. Set values based on server disk capacity + traffic volume + how many days back you need queryable logs.

---

### Custom Appender (DB / Kafka / any destination)
```java
public class DbAppender extends AppenderBase<ILoggingEvent> {
    @Override
    protected void append(ILoggingEvent event) {
        String message = event.getFormattedMessage();
        String loggerName = event.getLoggerName();
        long timestamp = event.getTimeStamp();
        // insert into DB, publish to Kafka, etc.
    }
}
```
Used when no pre-built appender covers the destination. **Must extend `AppenderBase<ILoggingEvent>`** — this is how Logback identifies the class as a valid appender and calls `append()` automatically. `ILoggingEvent` is Logback's internal representation of one log statement — carries message, logger name, timestamp, level, thread name, MDC data. No encoder needed: you control format and destination entirely inside `append()`. Registered in `logback-spring.xml` by pointing `class` attribute to the full package path of your class.

---

## Part 3 — Async Appender + Structured Logging (JSON)

### Sync vs Async Logging

**The problem:** All logging by default is synchronous — the request thread does the log write, waits for it to finish (I/O), then continues. Under heavy traffic, every log write adds latency directly to response time.

**Two types of async logging — this distinction is a common interview question:**

| | AsyncAppender | AsyncLogger |
|---|---|---|
| Framework | Logback | Log4j2 only |
| What's async | Formatting + write only | Entire pipeline |
| Level check | Sync (request thread) | Async (background thread) |
| Traffic fit | Medium | High |

**Logback does NOT support AsyncLogger.** Only AsyncAppender.

---

### AsyncAppender (Logback)

AsyncAppender is a **wrapper** — it doesn't do any writing itself. It sits in front of a real appender (File, Rolling File, etc.) and provides an in-memory queue + a single worker thread.

```xml
<appender name="ASYNC_FILE" class="ch.qos.logback.classic.AsyncAppender">
    <queueSize>1000</queueSize>
    <discardingThreshold>10</discardingThreshold>
    <neverBlock>false</neverBlock>
    <appender-ref ref="FILE"/>
</appender>
```

- **`queueSize`** — in-memory queue capacity. Default 256.
- **`discardingThreshold`** — when queue is X% from full, drop DEBUG and TRACE to preserve space for higher-priority events. `discardingThreshold=10` means drop low-priority when last 10% of space remains. `discardingThreshold=0` = never discard.
- **`neverBlock=false` (default)** — if queue is full, request thread waits. `neverBlock=true` — if queue is full, drop the log event, thread moves on immediately (no blocking, but logs can be lost).
- One worker thread by default; no Logback config to increase this.

**App shutdown risk:** Queue is in-memory. On JVM shutdown, AsyncAppender stops accepting new events and attempts to flush remaining ones — but makes no guarantee. Anything still in queue when JVM exits is lost.

---

### Filter Strategy — Sync for ERROR, Async for the Rest

Because ERROR logs are critical and must not be lost on shutdown, the production pattern is: async for DEBUG/TRACE/INFO/WARN, sync for ERROR.

Two Logback filters used:
- **`LevelFilter`** — exact level match; configurable `onMatch` (ACCEPT/DENY) and `onMismatch` (ACCEPT/DENY/NEUTRAL). NEUTRAL = pass to next filter in chain.
- **`ThresholdFilter`** — level and above. Everything below is rejected.

```xml
<!-- AsyncAppender: deny ERROR before it enters queue -->
<appender name="ASYNC" class="ch.qos.logback.classic.AsyncAppender">
    <filter class="ch.qos.logback.classic.filter.LevelFilter">
        <level>ERROR</level>
        <onMatch>DENY</onMatch>
        <onMismatch>NEUTRAL</onMismatch>
    </filter>
    <appender-ref ref="FILE_NOT_CRITICAL"/>
</appender>

<!-- Sync FileAppender: only ERROR passes through -->
<appender name="FILE_CRITICAL" class="ch.qos.logback.core.FileAppender">
    <file>logs/critical_app.log</file>
    <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
        <level>ERROR</level>
    </filter>
    <encoder>...</encoder>
</appender>

<logger name="com.concepts.PaymentController" level="INFO" additivity="false">
    <appender-ref ref="ASYNC"/>
    <appender-ref ref="FILE_CRITICAL"/>
</logger>
```

`log.info()` → ASYNC appender (LevelFilter: not ERROR → NEUTRAL → queued) + FILE_CRITICAL (ThresholdFilter: not ERROR → rejected). `log.error()` → ASYNC appender (LevelFilter: ERROR → DENY → not queued) + FILE_CRITICAL (ThresholdFilter: ERROR → accepted → written synchronously).

---

### Placeholder Best Practices

**Never use string concatenation in log statements:**
```java
// ❌ String built in memory even if logger rejects this statement
log.info("User " + username + " created with id " + id);

// ✅ String only built if log level accepts the statement
log.info("User {} created with id {}", username, id);
```
Under high traffic with logger set to WARN, every `log.info()` with concatenation wastes CPU and memory building strings that are immediately discarded.

**Exception always last:**
```java
// ✅ Logback detects Throwable as last param and prints full stack trace
log.error("Payment failed for user {}", username, e);
```
If exception is not the last parameter, stack trace is silently lost.

---

### Structured Logging — JSON with LogstashEncoder

**Problem with plain text:** Log aggregation tools (ELK, Splunk, Datadog) must parse plain text with regex. Different developers use different patterns → regex breaks. Business data (`payment_id=123`) buried in a free-form string can't be indexed or queried efficiently. Faking JSON via pattern (`<pattern>{"level":"%level"...}</pattern>`) is fragile and unmanageable.

**Solution:** Replace the pattern encoder with `LogstashEncoder`.

**Dependency:**
```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

```xml
<appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
</appender>
```

Auto-includes: `@timestamp`, `message`, `logger_name`, `thread_name`, `level`, `level_value`, stack trace, caller info — all from the log event, no configuration needed. Also reads MDC automatically (`includeMdc=true` is the default).

**Adding dynamic business fields via MDC:**
```java
MDC.put("payment_id", "P123");  // MUST come before log.info()
log.info("payment is successful");
```
`MDC.put()` must precede the log statement — log event is created at the moment `log.info()` is called, reading MDC at that instant. MDC fields appear as top-level JSON properties: `"payment_id": "P123"`.

**Masking PII with `MaskingJsonGeneratorDecorator`:**
```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <jsonGeneratorDecorator
        class="net.logstash.logback.mask.MaskingJsonGeneratorDecorator">
        <defaultMask>*****</defaultMask>
        <path>password</path>
        <path>token</path>
        <path>creditCard</path>
    </jsonGeneratorDecorator>
</encoder>
```
Runs as a post-processing step after LogstashEncoder produces JSON, before the appender writes it. Scans the entire JSON tree including nested objects. Any field matching a configured path has its value replaced with the mask. Protects against developers accidentally logging sensitive data. Rule: if unsure whether data is PII — don't log it. Government bodies can issue heavy fines for PII in logs.

---

## Part 4 — MDC + Correlation ID + Distributed Tracing

### MDC (Mapped Diagnostic Context)

**What it is:** A per-thread key-value map inside the logging framework. Set it once at the start of a request; every log statement that thread generates automatically carries that data — no need to pass context into every `log.info()` call.

**How it works internally:** When `log.info()` is called, Logback creates an `ILoggingEvent` object and **copies the current thread's MDC map** into the event's `mdcPropertyMap` field. The appender reads this field during formatting. This is why MDC data appears in every log statement without any extra parameters.

**Using in pattern encoder:** `%X` prints all MDC key=value pairs. `%X{payment_id}` prints only the value for that key (no key name, just the value).

---

### Log Pollution — Why `MDC.clear()` Is Critical

Spring Boot uses `ThreadPoolExecutor` — threads are reused across requests. If Request-A sets `MDC.put("user_id", "user1")` and finishes without clearing, the next request that picks up that same thread inherits `user1`'s MDC data. Request-B's logs then show `user_id=user1` even though it's processing user2 — log pollution.

**Fix:** Always clear MDC in a `finally` block so it runs even if an exception is thrown:
```java
try {
    MDC.put("payment_id", "P123");
    // business logic
} finally {
    MDC.clear();
}
```

In production, this goes inside a Filter (single entry/exit point for every HTTP request), not in controller methods.

---

### MDC and @Async — The Thread Boundary Problem

MDC is per-thread. When `@Async` spawns a new child thread, that thread starts with a completely empty MDC — it has no knowledge of the parent thread's data. Parent logs carry `payment_id=P123`, child thread logs show nothing.

**Solution: TaskDecorator** — a Spring framework feature (nothing to do with Logback). It wraps the Runnable task before it's submitted to the executor queue.

```java
@Component
public class MdcTaskDecorator implements TaskDecorator {
    @Override
    public Runnable decorate(Runnable runnable) {
        // decorate() runs on PARENT thread — MDC is accessible here
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            if (contextMap != null) MDC.setContextMap(contextMap);
            try {
                runnable.run();
            } finally {
                MDC.clear(); // prevent pollution on child thread too
            }
        };
    }
}
```

Key insight: `decorate()` runs on the parent thread, so the parent MDC is readable at that point. The captured map is then injected into the child thread's MDC before the actual task runs.

**Registration:** If using Spring Boot's default executor, marking `MdcTaskDecorator` as `@Component` is enough — auto-picked up. For a custom `ThreadPoolTaskExecutor`, call `executor.setTaskDecorator(mdcTaskDecorator)` explicitly in `AsyncConfig`.

---

### Correlation ID — Filter Implementation

**Problem:** A production log file has thousands of lines from concurrent requests. When a customer reports a failure, you have no way to isolate which lines belong to their specific request.

**Solution:** Assign a unique ID to each incoming request. Store it in MDC. Every log statement for that request automatically carries it. Return it in the response header so the customer can share it with support.

**Implementation: `OncePerRequestFilter`** — best location because it's the first thing that runs when a request enters and the last when the response exits.

```java
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID = "correlationId";
    public static final String CORRELATION_HEADER = "X-CorrelationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId = request.getHeader(CORRELATION_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        try {
            MDC.put(CORRELATION_ID, correlationId);
            response.setHeader(CORRELATION_HEADER, correlationId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
```

Why check if the header already has a Correlation ID: in a microservices chain, Service-1 may call Service-2 passing its ID in the header. Service-2 should reuse that same ID so all logs across all services for one original request share a single correlation ID.

---

### Distributed Tracing + Full End-to-End Logging

**Correlation ID alone answers:** "Show me all logs for request X." It does not answer: "Which services did request X touch? Which service failed? How long did each take?"

**Distributed tracing answers the path question.** Tool stack used: Micrometer + OpenTelemetry + Jaeger.

**Dependencies:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

```properties
management.tracing.sampling.probability=1.0
management.otlp.tracing.endpoint=http://localhost:4318/v1/traces
```

**Critical insight:** Micrometer auto-registers `ServerHttpObservationFilter`. This filter generates a Trace ID for each incoming request, **puts it into MDC automatically**, and when Service-1 calls Service-2, passes the Trace ID in the request header. Service-2's filter detects the header, reuses the same Trace ID, and puts it in its own MDC. Both services' log statements carry the same Trace ID — without any manual header passing in application code.

**Trace ID = Correlation ID pattern:** Industry practice is to use the same ID for both. `CorrelationIdFilter` reads the Trace ID from Micrometer's `Tracer` and uses it as the Correlation ID — one ID serves both the tracing tool (Jaeger) and the log aggregation tool (Datadog).

```java
@Autowired
private Tracer tracer;

// inside doFilterInternal:
String uniqueId = tracer.currentSpan().context().traceId();
if (uniqueId == null) {
    uniqueId = UUID.randomUUID().toString(); // fallback if tracing disabled
}
MDC.put(CORRELATION_ID, uniqueId);
response.setHeader(CORRELATION_HEADER, uniqueId);
```

Why still write the custom filter even with tracing: (1) `ServerHttpObservationFilter` does not send the Trace ID back in the response header — clients need it to report issues, so the custom filter adds that. (2) The UUID fallback ensures correlation logging keeps working even if tracing is disabled.

**Final log output across all services:**
```json
{
  "@timestamp": "...",
  "message": "payment is successful",
  "level": "INFO",
  "logger_name": "com.concepts.PaymentController",
  "traceId": "9e7d21299f4ea8a1",
  "spanId": "2c3655fc800de28b",
  "correlationId": "9e7d21299f4ea8a1",
  "payment_id": "P123"
}
```

Same `traceId` and `correlationId` across every service in the chain. Log aggregation tool search on `correlationId` returns all detailed logs. Jaeger search on `traceId` shows the full request path with timing and status codes per service.