# Spring Boot — Course Overview

A complete walkthrough of modern backend development with Spring Boot, from fundamentals through production-grade patterns.

---

## Lecture 1 — Introduction & Why Spring Boot Exists

Traces the full evolutionary arc from raw Servlets to Spring Boot. Covers what a **Servlet** is, how **Tomcat** (Servlet Container) manages them, and why `web.xml` became an unmanageable bottleneck in large applications. Then explains how **Spring MVC** solved those problems — introducing **DispatcherServlet** as the front controller, **annotation-based URL mapping** (`@Controller`, `@RequestMapping`, `@GetMapping`), and **Dependency Injection / IoC** (`@Component`, `@Autowired`) to eliminate tight coupling and enable proper unit testing. Finally explains the remaining pain of Spring MVC — manually managed dependency versions, boilerplate `AppConfig` classes, and manual DispatcherServlet initialization — establishing exactly why Spring Boot was created.

---

## Lecture 2 — Setup & Architecture
**Core Concepts:** Project bootstrapping, JAR vs WAR, Layered Architecture

- **Project Setup** — Spring Initializr (`start.spring.io`); Maven build tool; `pom.xml` for dependency management; Spring Web dependency brings embedded Tomcat
- **JAR vs WAR** — JAR is self-contained with embedded Tomcat (modern microservices standard); WAR requires external server deployment (legacy monolithic apps); always use JAR for REST APIs
- **Layered Architecture** — 3 core layers:
    - **Controller** (`@RestController`) — entry point; maps DTOs only, zero business logic
    - **Service** (`@Service`) — all business logic; maps Entity → Response DTO
    - **Repository** (`@Repository`) — only layer touching DB; returns Entity objects
- **Supporting packages** — `dto/` (Request DTO + Response DTO to decouple client fields from internals), `entity/` (`@Entity` classes mirroring DB tables), `utility/` (shared helper methods), `configuration/` (`application.properties` driven via `@Value`, no hardcoding)

---

## Lecture 3 — Beans, Lifecycle & IOC
**Core Concepts:** Bean, IOC Container, `@Component` vs `@Bean`, Lifecycle hooks, Eager vs Lazy

- **Bean** — any Java object whose lifecycle is managed by Spring's IOC Container (ApplicationContext)
- **IOC** — Inversion of Control; Spring owns object creation/destruction instead of the developer
- **Creating Beans:**
    - `@Component` — convention-based; Spring calls the default no-arg constructor automatically; fails if no default constructor exists
    - `@Bean` inside `@Configuration` — explicit control; you write the creation logic; handles classes with custom constructors; higher priority over `@Component`
    - `@Controller`, `@Service`, `@Repository` are all specialized `@Component` variants
- **Bean Discovery** — `@ComponentScan` (default inside `@SpringBootApplication`) scans root package and all sub-packages; `@Configuration` is also a `@Component` so gets picked up automatically
- **Initialization:**
    - **Eager** — Singleton beans created at startup (default)
    - **Lazy** — `@Lazy` annotation; created only when first needed; if a Singleton depends on a `@Lazy` bean, the lazy bean is created at that point
- **Lifecycle Hooks:**
    - `@PostConstruct` — runs after bean constructed AND dependencies injected, before bean is used
    - `@PreDestroy` — runs just before bean destruction; used for cleanup (closing connections, releasing resources)

---

## Lecture 4 — Controller Layer
**Core Concepts:** Request routing, parameter binding, response control

- **`@Controller` vs `@RestController`** — `@Controller` treats return as view name (MVC); `@RestController` = `@Controller` + `@ResponseBody`; every method return written directly to HTTP response body
- **`@RequestMapping`** — maps URL + HTTP method to a Java method; can be placed at class level (common path prefix) and method level; `path`/`value` are aliases
- **Shortcut annotations** — `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, `@PatchMapping`; just `@RequestMapping` with method pre-filled
- **`@RequestParam`** — binds query string params (`?key=value`); `required=true` by default (missing param = error); `required=false` = optional (null if absent); Spring auto-converts types (String → int, etc.)
- **`@InitBinder`** — pre-processes request params before method runs; registers custom `PropertyEditorSupport` for custom type conversion/formatting
- **`@PathVariable`** — extracts values from URL path (`/users/{id}`); curly brace placeholders; always part of URL structure; not optional by default
- **`@RequestBody`** — reads HTTP request body (JSON); Jackson deserializes JSON → Java object via getters/setters; `@JsonProperty("json_key")` handles field name mismatches (snake_case vs camelCase)
- **`ResponseEntity<T>`** — full control over HTTP response (status code + headers + body); `ResponseEntity.status(HttpStatus.OK).header(...).body(data)`; Spring internally wraps plain returns in ResponseEntity anyway
- **Validation** — `spring-boot-starter-validation` (Hibernate Validator); annotations on Request DTOs (`@NotBlank`, `@Email`, `@Min`, `@Max`, `@Pattern`, `@Size`, etc.); `@Valid` triggers validation on `@RequestBody`; `@Validated` adds group support (OnCreate/OnUpdate); `@RestControllerAdvice` + `@ExceptionHandler(MethodArgumentNotValidException.class)` for clean error responses

---

## Lecture 5 — Dependency Injection
**Core Concepts:** DI types, advantages, circular and unsatisfied dependency

- **Problem without DI** — tight coupling; class creates its own dependencies with `new`; violates Dependency Inversion Principle (SOLID D); breaks when dependency structure changes
- **DI in Spring** — `@Component` marks class for Spring management; `@Autowired` signals Spring to inject; Spring uses IOC container as the external source
- **Three types of injection:**
    - **Field Injection** — `@Autowired` on field; simplest to read; Spring uses Reflection; cannot use `final` (Reflection bypasses it); NPE risk if object created manually; hard to unit test (need `@InjectMocks`)
    - **Setter Injection** — `@Autowired` on setter method; dependency changeable after creation; easier to test than field; still can't use `final`; harder to read (must hunt for `@Autowired` setter)
    - **Constructor Injection** — `@Autowired` on constructor (optional if only one constructor, from Spring 4.3); object fully initialized at creation; supports `final` fields; fail-fast (missing bean = startup failure); cleanest unit testing (pass mock directly); large constructor = code smell signal (violates SRP)
- **Circular Dependency** — A depends on B, B depends on A → startup failure; solutions: (1) **Refactor** — extract shared logic to third class (preferred), (2) `@Lazy` on `@Autowired` — Spring injects proxy, real bean created on first use, (3) `@PostConstruct` — manually set dependency (hacky, avoid)
- **Unsatisfied Dependency** — multiple beans of same type, Spring can't choose; solutions: (1) `@Primary` — global default preference, (2) `@Qualifier("name")` — per-injection-point specific selection

---

## Lecture 6 — Bean Scopes
**Core Concepts:** 5 scope types, initialization timing, proxy mode

- **Singleton** — one object per IOC container; eagerly initialized at startup; same object shared everywhere; default (no annotation needed)
- **Prototype** — new object every time the bean is needed; lazily initialized; if a Singleton depends on it, Prototype is created when Singleton is built
- **Request** — one object per HTTP request; shared within that request (same object reused if needed in multiple places within one request); lazily initialized; `@RequestScope` or `@Scope("request")`
- **Session** — one object per HTTP session; shared across multiple requests within the same session; destroyed when session expires/invalidated; `@SessionScope` or `@Scope("session")`
- **Application** — one object across multiple IOC containers; rarely used in practice
- **Proxy Mode problem** — Singleton (eager) depending on Request/Session (lazy) fails at startup because no HTTP context exists yet; fix: `@Scope(value="request", proxyMode=ScopedProxyMode.TARGET_CLASS)` — Spring injects a proxy placeholder at startup; real bean created when actual HTTP request arrives

---

## Lecture 7 — Dynamically Initialized Beans
**Core Concepts:** `@Qualifier` smart usage, `@Bean` + `@Value` config-driven selection, `@Value` sources

- **Problem** — interface with multiple implementations; Spring can't choose which to inject → `UnsatisfiedDependencyException`
- **Solution 1 (Industry Standard): Smart `@Qualifier`** — inject ALL implementations into the class using `@Qualifier` on each `@Autowired` field; use runtime `if-else` logic based on client request parameter to decide which to call; both Singleton beans pre-created at startup; selection happens per API call
- **Solution 2: `@Bean` + `@Value`** — remove `@Component` from implementations; create `@Configuration` class with `@Bean` method; use `@Value("${propertyKey}")` to read from `application.properties`; `if-else` inside `@Bean` method decides which implementation to instantiate; only ONE bean created at startup; switch by changing config and restarting
- **`@Value` sources:**
    - `@Value("${key}")` — reads from `application.properties` or environment variables (same syntax)
    - `@Value("literalValue")` — hardcoded inline literal
    - Can be placed on fields, method parameters, constructor parameters

---

## Lecture 8 — `@ConditionalOnProperty`
**Core Concepts:** Conditional bean creation, feature toggling, `@Autowired(required=false)`

- **Purpose** — controls whether a bean is created based on a property value; de-clutters ApplicationContext; saves memory; faster startup
- **Parameters** — `prefix` + `value` forms the full key (`prefix.value`); `havingValue` = expected string value (just string comparison, not limited to true/false); `matchIfMissing` = behavior when key absent (false = don't create)
- **Two-part solution** — (1) `@ConditionalOnProperty` on the bean class, (2) `@Autowired(required=false)` at injection point (default `required=true` would crash if bean not created)
- **Use cases** — DB migration toggle (MySQL ↔ NoSQL via config flip); shared codebase between multiple apps (each app's `application.properties` controls which beans exist)
- **Disadvantages** — misconfiguration = silent runtime failures; complexity increases when overused; same property key for multiple beans causes confusion; requires careful property file management

---

## Lecture 9 — Spring Profiles
**Core Concepts:** Environment separation, profile activation, `@Profile` annotation

- **Problem** — same codebase, different configs per environment (dev/qa/prod): DB credentials, URLs, timeouts, retry values
- **Solution** — multiple `application-{profileName}.properties` files; active profile's file loaded on top of default `application.properties` (parent-child: child overrides parent for same key; child-only keys added; parent-only keys kept)
- **Activation methods:**
    1. `spring.profiles.active=qa` inside `application.properties` (static, not ideal)
    2. `mvn spring-boot:run -Dspring-boot.run.profiles=prod` (dynamic)
    3. `mvn spring-boot:run -Pproduction` via Maven `<profiles>` in `pom.xml` (dynamic, cleanest — instructor preferred)
- **Priority** — runtime flag > `application.properties` default
- **`@Profile` annotation** — placed on `@Component` class; bean created only if named profile is active; multiple active profiles: bean created if its profile is in the active list; last profile in list wins for properties file selection
- **`@Profile` vs `@ConditionalOnProperty`** — `@Profile` = environment separation (dev/qa/prod); `@ConditionalOnProperty` = conditional bean creation based on config; using `@Profile("app1")` for app-specific beans is wrong use — it creates confusion since profiles are expected to be environment names

---


## Lecture 10 — Maven & Its Build Lifecycle

Covers Maven as a **project management tool** (not just a build tool). Explains the difference between Ant's "tell it what AND how" vs Maven's "tell it only what." Walks through the **standard project structure** (`src/main/java`, `src/test/java`) and why Maven assumes it. Deep dives into every element of `pom.xml` — the `<parent>` block and the **Super POM hierarchy**, `<groupId>/<artifactId>/<version>` as project coordinates, `<properties>` for reusable key-value config, `<repositories>` for remote dependency sources, `<dependencies>`, and `<build>`. Covers the **7-phase build lifecycle** (validate → compile → test → package → verify → install → deploy) and how running any phase runs all prior phases. Explains **local repository** (`~/.m2/`) as a cache, **remote repositories** (Maven Central, company Nexus/Artifactory), and `settings.xml` for storing credentials separately from `pom.xml`.

---

## Lecture 11 — AOP (Aspect Oriented Programming)

Solves the problem of repetitive cross-cutting concerns (logging, transactions, security) scattered across hundreds of methods. Introduces the **4 core AOP vocabulary terms** — **Aspect** (the class holding boilerplate), **Pointcut** (the expression targeting which methods), **Advice** (the code + timing of execution), and **JoinPoint** (the moment the real method fires). Covers all **7 Pointcut expression types**: `execution`, `within`, `@within`, `@annotation`, `args`, `@args`, `target` — with wildcard rules (`*` = one, `..` = zero or more). Covers all **3 Advice types**: `@Before`, `@After`, and `@Around` (with `ProceedingJoinPoint` and `joinPoint.proceed()`). Explains **how AOP works internally** via Spring proxies — at startup, `PointcutParser` pre-parses expressions, `AbstractAutoProxyCreator` checks each bean for interception eligibility, and either a **JDK Dynamic Proxy** (for interface-implementing classes) or a **CGLIB subclass proxy** (for others) is created. At runtime, the proxy runs an **advice chain** via `ReflectiveMethodInvocation` — no runtime matching overhead.

---

## Lecture 12 — Transaction Management (`@Transactional`)

**Part 1** establishes the problem: **Critical Sections** (code that reads and modifies shared resources) cause **data inconsistency** under parallel requests (demonstrated with a cab booking example). Introduces **Transactions** as the solution and covers all **4 ACID properties** (Atomicity, Consistency, Isolation, Durability) with a money transfer example. Shows the old manual way (`BEGIN_TRANSACTION / COMMIT / ROLLBACK`) is pure boilerplate, then introduces `@Transactional` — which uses **AOP internally** (specifically `@within` pointcut + **Around Advice** running inside `TransactionalInterceptor.invokeWithinTransaction()`). Covers setup (`spring-boot-starter-data-jpa`, DB driver, `application.properties`), class-level vs method-level annotation placement, and `@EnableTransactionManagement`.

**Part 2** covers the **Transaction Manager hierarchy** (`TransactionManager` → `PlatformTransactionManager` → `AbstractPlatformTransactionManager` → concrete managers: `DataSourceTransactionManager`, `JpaTransactionManager`, `HibernateTransactionManager`, `JtaTransactionManager`). Explains **Declarative vs Programmatic** transaction management — with the specific use case where programmatic is necessary (mixed DB + external API calls holding DB connections open). Shows two programmatic approaches: **Approach 1** using `PlatformTransactionManager.getTransaction() / commit() / rollback()` directly, and **Approach 2** using `TransactionTemplate.execute()` (cleaner, wraps plumbing). Covers all **6 Propagation types** (`REQUIRED`, `REQUIRED_NEW`, `SUPPORTS`, `NOT_SUPPORTED`, `MANDATORY`, `NEVER`) with suspend/resume behavior.

**Part 3** covers **Isolation Levels** — the 3 concurrency problems (**Dirty Read**, **Non-Repeatable Read**, **Phantom Read**), **Shared Lock vs Exclusive Lock** mechanics, and all **4 isolation levels** (`READ_UNCOMMITTED`, `READ_COMMITTED`, `REPEATABLE_READ`, `SERIALIZABLE`) mapped to which problems they solve, which locking strategy they use, and when to choose each. Database-specific defaults noted (MySQL = `REPEATABLE_READ`, PostgreSQL = `READ_COMMITTED`).

---

## Lecture 13 — `@Async` Annotation

**Part 1** starts with **Thread Pool** fundamentals — why pools exist, how `ThreadPoolExecutor` works with `minPoolSize`, `maxPoolSize`, and `queueSize`, and the exact decision flow for task assignment (free thread → queue → new thread → reject). Then covers `@Async` — what it does, `@EnableAsync` requirement, and the **3 use cases** for which executor gets selected: Use Case 1 (nothing defined → Spring's default `ThreadPoolTaskExecutor` with dangerous defaults — `coreSize=8`, `maxSize=Integer.MAX_VALUE`, `queue=Integer.MAX_VALUE`), Use Case 2 (Spring `ThreadPoolTaskExecutor` bean → picked automatically), Use Case 3 (plain Java `ThreadPoolExecutor` bean → falls back to `SimpleAsyncTaskExecutor` unless `@Async("beanName")` is explicit). Covers the **industry-standard approach**: implementing `AsyncConfigurer` and overriding `getAsyncExecutor()` — with singleton handling via `synchronized` + null check since the method isn't a `@Bean`.

**Part 2** covers the **2 conditions** for `@Async` to work (public method + different class, both due to AOP proxy mechanics). Covers `@Async` + `@Transactional` interaction — 3 use cases (nesting async inside transactional = no transaction for async thread ❌; same method with both = transaction works but propagation breaks ⚠️; async calls separate transactional method = industry standard ✅). Covers the **3 return types** (`void`, `Future<T>` — deprecated, `CompletableFuture<T>` — modern standard). Covers **exception handling** for each: with return type exceptions surface at `.get()`, for `void` methods uses a custom `AsyncUncaughtExceptionHandler` registered via `AsyncConfigurer.getAsyncUncaughtExceptionHandler()`.

---

## Lecture 14 — Interceptors & Filters

**Custom Interceptors** covers two types. **Type 1 (HandlerInterceptor)** — implements `HandlerInterceptor` with `preHandle()` (returns boolean, runs before controller), `postHandle()` (runs after controller, success only), and `afterCompletion()` (always runs, like `finally`). Registered via `WebMvcConfigurer.addInterceptors()` with `addPathPatterns()` and `excludePathPatterns()`. Internal mechanics traced through `DispatcherServlet.doDispatch()`. **Type 2 (AOP @Aspect with custom annotation)** — creating a `@Target(METHOD) @Retention(RUNTIME)` custom annotation with fields, then an `@Aspect` class using `@Around("@annotation(...)")` with `ProceedingJoinPoint` to read annotation values via Reflection and call `joinPoint.proceed()`. Covers all **3 `@Retention` policies** (SOURCE, CLASS, RUNTIME) and why RUNTIME is mandatory for interceptors.

**Filters vs Interceptors** establishes the full architecture: Filters live at the **Servlet Container level** (before any servlet is chosen, applies to ALL servlets, Jakarta EE), Interceptors live at the **DispatcherServlet level** (Spring-specific, after servlet chosen, before controller). Covers registering multiple Filters with `FilterRegistrationBean` and `setOrder()`, multiple Interceptors via registration sequence, the **stack pattern** for execution order (request = forward, response = reverse), and a combined flow showing both layers running together. Decision rule: generic logic → Filter, Spring-specific application logic → Interceptor.

---

## Lecture 16 — ResponseEntity & HTTP Response Codes

Establishes that every HTTP response has **3 parts**: status code, headers, body. Covers `ResponseEntity<T>` — the **Builder pattern** (`.status() → .headers() → .body()`, body always last), `.build()` for no-body responses, and when `ResponseEntity` is optional vs necessary. Explains `@ResponseBody` and why `@RestController` = `@Controller + @ResponseBody`. Then provides a complete reference for all HTTP status code families — **1xx** (100 Continue — pre-flight for large payloads), **2xx** (200 OK, 201 Created, 202 Accepted, 204 No Content, 206 Partial Content with when to use each), **3xx** (301 Moved Permanently, 308 Permanent Redirect, 304 Not Modified — GET+caching only, common misuse with PATCH explained), **4xx** (400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found, 405 Method Not Allowed, 422 Unprocessable Entity — business logic failure vs technical failure, 429 Too Many Requests — rate limiting, 409 Conflict — locking mechanism with Redis TTL), **5xx** (500 Internal Server Error, 501 Not Implemented, 502 Bad Gateway — Nginx proxy scenario).

---

## Lecture 17 — Exception Handling

Covers the **5 key internal classes**: `HandlerExceptionResolverComposite` (orchestrator, calls resolvers in sequence), `ExceptionHandlerExceptionResolver` (handles `@ExceptionHandler` and `@ControllerAdvice`), `ResponseStatusExceptionResolver` (handles **uncaught** exceptions with `@ResponseStatus` — "uncaught" is critical), `DefaultHandlerExceptionResolver` (handles Spring's own internal exceptions like 404, 405), and `DefaultErrorAttributes` (always runs last, builds the actual `ResponseEntity` with timestamp/status/error/message/path — resolvers only set status, they don't build the response). Traces why custom exceptions with a `BAD_REQUEST` field still return 500 (JPA doesn't read your fields — none of the 3 resolvers recognized the exception).

Covers **`@ExceptionHandler`** — controller-level (multiple handlers, single vs multi-exception handlers, allowed method parameters: `Exception`, `HttpServletRequest`, `HttpServletResponse`), and **`@ControllerAdvice`** — global-level, with the priority order (controller exact match → controller parent match → global exact match → global parent match). Deep dives into **`ResponseStatusExceptionResolver`** behavior — `@ResponseStatus` on exception class (straightforward), `@ResponseStatus` on `@ExceptionHandler` method (Spring's request mechanism applies override, not the resolver), and the dangerous combination of `@ResponseStatus` + `response.sendError()` (causes 500 due to committed response). The two approaches: **manual try-catch returning `ResponseEntity`** (bypasses all resolvers entirely — still used in many companies) vs **resolver framework** (`@ExceptionHandler`/`@ControllerAdvice`).

---

## Lecture 18 — Spring Data JPA (10 Parts)

**Part 1 — Introduction & JDBC**: Traces from raw JDBC (manual `Class.forName()`, `DriverManager.getConnection()`, `PreparedStatement`, `ResultSet`, manual connection closing) through its 5 problems (driver loading, connection management, vague `SQLException`, resource cleanup, no connection pool). Shows `JdbcTemplate` solving all 5 — `HikariCP` as the default connection pool, granular Spring DAO exceptions, and covers all `JdbcTemplate` methods (`execute()`, `update()` with args and `PreparedStatementSetter`, `query()` with `RowMapper`, `queryForList()`, `queryForObject()`).

**Part 2 — ORM, JPA Setup & Architecture**: Defines ORM and the JPA (`interface`) → Hibernate (`implementation`) relationship. The **4 setup requirements**: `pom.xml` dependency, `application.properties` config, `@Entity` class, `@Repository` interface extending `JpaRepository`. The full JPA architecture: **Persistence Unit** (application.properties as config, `persistence.xml` for manual/multi-DB), **EntityManagerFactory** (one per DB, created at startup, heavy), **Transaction Manager** (`RESOURCE_LOCAL` vs `JTA`), **EntityManager** (one per operation, lightweight, with `PersistenceContext`), **Dialect** (JPQL → database-specific SQL translation).

**Part 3 — L1 Cache (First-Level Cache)**: How `PersistenceContext` is a `HashMap` (`StatefulPersistenceContext`) keyed by `EntityKey`. Lookup order — check map first, DB only on miss. `DispatcherServlet` creates one `EntityManager` per HTTP request (`OpenEntityManagerInViewInterceptor`), so one request = one PC = one L1 cache. Proven with manual `EntityManagerFactory.createEntityManager()` demos showing complete isolation between two sessions. INSERT does not populate L1, the first GET does. Why `save()` + `findById()` in same request = no SELECT query.

**Part 4 — L2 Cache (Second-Level Cache)**: Why L1 is insufficient across requests. L2 as a shared cache sitting between PersistenceContext and DB. Lookup order: L1 → L2 → DB. INSERT bypasses L2 entirely. Setup: 3 dependencies (`ehcache`, `cache-api` for JCache interfaces/loose coupling, `hibernate-jcache` as bridge/orchestrator), 3 `application.properties` keys, `@Cache` annotation on entity. **Cache Regions** — named buckets with independent TTL, max size, and eviction strategy (FIFO/LIFO/LRU) configured in `ehcache.xml`. All **4 Concurrency Strategies**: `READ_ONLY` (no updates, fastest), `READ_WRITE` (shared lock on read, exclusive lock on update, cache updated after commit, self-heals after rollback), `NONSTRICT_READ_WRITE` (no read lock, cache only invalidated on commit, small stale window possible), `TRANSACTIONAL` (strictest, reads during lock bypass cache to DB, writes queue).

**Part 5 — Mapping DTOs to Tables**: `spring.jpa.hibernate.ddl-auto` — all 5 values (none for production, update for dev, validate, create, create-drop). DB vs Schema distinction (schema = logical grouping for multi-team shared DBs, Hibernate does NOT auto-create schemas). `@Table` annotation — `name`, `schema`, `uniqueConstraints` (single and composite `@UniqueConstraint`), `indexes` (`@Index` with comma-separated `columnList`). `@Column` — `name`, `unique`, `nullable`, `length`. `@GeneratedValue` — `IDENTITY` (auto-increment, table-specific), `SEQUENCE` with `@SequenceGenerator` (`name`, `sequenceName`, `initialValue`, `allocationSize` for batch ID caching), `TABLE` (why it's almost never used — lock contention on shared table).

**Part 6 — One-to-One Relationships**: Unidirectional `@OneToOne` — FK in parent table, `@JoinColumn(name, referencedColumnName)`, `@JoinColumns` for composite keys. All **6 CascadeType values** explained with scenarios (PERSIST, MERGE, REMOVE, REFRESH, DETACH, ALL). **Eager vs Lazy loading** defaults (`@OneToOne`/`@ManyToOne` = EAGER, `@OneToMany`/`@ManyToMany` = LAZY) — why Lazy + Jackson serialization fails during GET (empty proxy, no transaction), two fixes (`@JsonIgnore` = blunt, DTO = recommended — DTO constructor explicitly calls getter which triggers lazy load). Bidirectional — **Owner side** (holds FK, uses `@JoinColumn`) vs **Inverse side** (uses `mappedBy`), DB structure unchanged. Infinite recursion problem and 3 solutions: `@JsonManagedReference` + `@JsonBackReference` (blocks backward), `@JsonIgnore`, `@JsonIdentityInfo` (serializes object once then uses ID as reference — most powerful, shows data from both sides).

**Part 7 — One-to-Many, Many-to-One, Many-to-Many**: One-to-Many — JPA default creates join table, `@JoinColumn` overrides to store FK in child table (recommended). Lazy loading demo with DTO showing exactly when second SELECT fires. **Orphan Removal** — what it is (child with null FK sitting in DB), `orphanRemoval = true` (two-step: set FK to null then DELETE), distinction from Cascade Delete (cascade = parent deleted, orphan = child removed from collection without deleting parent). Bidirectional — child (Order) is the **Owning Side** in One-to-Many (holds FK), parent (User) is Inverse Side with `mappedBy`, custom setter on inverse side to manually set back-references, `@JsonIdentityInfo` for recursion prevention. Many-to-One = same relationship from child's perspective. Many-to-Many — no parent-child, always requires a join table, `@JoinTable(name, joinColumns, inverseJoinColumns)`, choosing owning side matters (only owning side drives the join table), `@JsonIgnore` on inverse side.

**Part 8 — JPQL, Derived Queries, N+1, Pagination**: **Derived Query** — naming convention (prefix + optional desc + "By" + field + keyword), all comparison keywords, DELETE with `@Transactional`, `deleteByX` internally does SELECT then N DELETEs. **Pagination** — `Pageable` parameter + `PageRequest.of(page, size)`, `Page<T>` vs `List<T>` return types. **Sorting** — `Sort.by("field").ascending()`, multi-field sort with `Sort.Order.asc/desc`, combined pagination+sorting with `PageRequest.of(page, size, Sort)`. **JPQL** — `@Query` with entity names (not table names), `@Param` for named parameters, JOIN via relationship field (no ON needed), `Object[]` vs custom DTO with `new DTO(...)` constructor syntax inside query, `@NamedQuery` on entity for reusability. **N+1 Problem** — occurs when fetching multiple parents with children, why EAGER doesn't fix it for multi-parent queries, 3 solutions: `JOIN FETCH` in JPQL (1 query), `@BatchSize` (batches child fetches), `@EntityGraph` (works on derived methods). **`@Modifying`** — required for DELETE/UPDATE/INSERT in `@Query`, always with `@Transactional`, `flushAutomatically` and `clearAutomatically` to sync persistence context.

**Part 9 — Native Query & Criteria API**: **Native Query** — when to use (JSONB, non-entity results, unrelated joins, bulk performance), `nativeQuery = true` parameter. Uses DB table/column names (not entity names). `SELECT *` → JPA auto-maps via `@Column`. Partial field mapping via `@SqlResultSetMapping` + `@NamedNativeQuery` (annotation-driven DTO construction) or manual `Object[]` mapping. **Dynamic Native Query** — `EntityManager.createNativeQuery()` with `StringBuilder`, `WHERE 1=1` pattern, `List<Object>` parameter tracking, 1-indexed `setParameter()`, manual LIMIT/OFFSET for pagination. Trade-offs: powerful but DB-dependent and not type-safe. **Criteria API** — solves dynamic + DB-independent + type-safe gap. Three-object hierarchy: `CriteriaBuilder` (factory), `CriteriaQuery` (structure — `from()`, `select()`, `multiselect()`, `where()`, `orderBy()`), `TypedQuery` (execution — `getResultList()`, `setFirstResult()`, `setMaxResults()`). `Root` as primary table. `createQuery(Entity.class)` for SELECT *, `createQuery(Object[].class)` for partial. All Predicate types (`equal`, `notEqual`, `gt/ge/lt/le`, `like`, `in`, `and/or/not`). Sorting on `CriteriaQuery`, pagination on `TypedQuery`. Dynamic conditions with `List<Predicate>` + `cb.and(predicates.toArray(...))`.

**Part 10 — Specification API**: Solves two Criteria API problems — **code duplicity** (same predicates copy-pasted across methods) and **code boilerplate** (manually creating CriteriaBuilder/CriteriaQuery/Root every time). `Specification<T>` as a **functional interface** with one abstract method `toPredicate(root, query, cb)` — implemented via lambda. `UserSpecification` class pattern — each static method = one predicate (one condition), join workaround by doing `root.join()` and returning `null`. Repository extends `JpaSpecificationExecutor<T>` (body stays empty, provides `findAll(spec)`, `findOne(spec)`, `exists(spec)`, paginated `findAll(spec, pageable)`). Service uses `Specification.where().and().and()` chaining. Internally `JpaSpecificationExecutor` calls `applySpecificationToCriteria()` which creates all Criteria objects and calls `toPredicate()` — you write zero boilerplate. Limitation: designed for predicates, joins are a workaround.

---

## Lecture 19 Part 1 — Security: Attacks (same as Lecture 1 above)
---
**Core Concepts:** CSRF, XSS, CORS, SQL Injection

- **CSRF** — session cookie auto-sent by browser; protected via CSRF tokens (server-generated, embedded in forms, validated on each state-changing request)
- **XSS** — malicious scripts injected into user-generated content; protected via input escaping (`th:text` vs `th:utext` in Thymeleaf) and input validation
- **CORS** — not an attack; browser-enforced origin policy (Protocol + Domain + Port); configured in Spring via `CorsConfiguration` inside `SecurityConfig`, whitelisting allowed origins/methods/headers
- **SQL Injection** — user input concatenated directly into SQL; protected via parameterized queries using `:name` placeholders with `setParameter()`

---

## Lecture 19 Part 2 — Security Architecture
**Core Concepts:** Security filter chain placement, authentication/authorization flow, core components

- **Where Spring Security fits** — inserts a `SecurityFilterChain` as one filter inside the existing Servlet Filter Chain; internally contains multiple security-specific filters; not all run per request — depends on configured authentication method
- **Full internal flow:**
    1. Security Filter (method-dependent) creates partial `Authentication` object (`isAuthenticated=false`)
    2. Passes to `AuthenticationManager` (interface; default impl: `ProviderManager`)
    3. `ProviderManager` delegates to matching `AuthenticationProvider` via `supports()` check
    4. Provider does: hash incoming password (`PasswordEncoder`) + fetch stored user (`UserDetailsService`) + compare hashes
    5. Returns fully populated `Authentication` object (`isAuthenticated=true`, roles populated)
    6. Stored in `SecurityContextHolder` → available to entire request lifecycle
- **Key components:**
    - `ProviderManager` — iterates provider list, calls `support()`, delegates to correct provider
    - `DaoAuthenticationProvider` — handles `UsernamePasswordAuthenticationToken`; uses `UserDetailsService` + `PasswordEncoder`
    - `UserDetailsService` — interface; two built-in impls: `InMemoryUserDetailsManager` (HashMap in memory) and `JdbcUserDetailsManager` (DB)
    - `PasswordEncoder` — `BCryptPasswordEncoder` (recommended); `DelegatingPasswordEncoder` (default, reads `{id}` prefix to delegate)
    - `SecurityContextHolder` — thread-local storage; holds `SecurityContext` → `Authentication` for request lifetime

---

## Lecture 19 Part 3 — User Creation & Password Storage
**Core Concepts:** 3 user creation approaches, password encoding, `{noop}` prefix

- **Approach 1: `application.properties`** — `spring.security.user.name/password/roles`; Spring uses Reflection to override `SecurityProperties.User` defaults; only one user possible; dev/testing only
- **Approach 2: Custom `InMemoryUserDetailsManager` bean** — define `@Bean UserDetailsService` returning `new InMemoryUserDetailsManager(user1, user2, ...)`; `User.withUsername().password().roles().build()`; multiple users; still in-memory (lost on restart)
- **Password format** — `{id}encodedPassword`; `DelegatingPasswordEncoder` reads `{id}` prefix to select encoder; `{noop}` = plain text (no encoding); `{bcrypt}` = BCrypt hashed; if you define a `PasswordEncoder` bean explicitly, `{id}` prefix not required (Spring skips `DelegatingPasswordEncoder`)
- **Approach 3: DB-backed (production)** — `UserAuthEntity implements UserDetails`; `UserAuthEntityRepository extends JpaRepository`; `UserAuthEntityService implements UserDetailsService` (overrides `loadUserByUsername()`); registration endpoint hashes password with `passwordEncoder.encode()` before saving

---

## Lecture 19 Part 4 — Form-Based Authentication
**Core Concepts:** Stateful auth, session lifecycle, full filter flow, SecurityConfig customization

- **What it is** — stateful; server maintains `HttpSession`; user proves identity once; subsequent requests use `JSESSIONID` cookie
- **Login flow filters:** `UsernamePasswordAuthenticationFilter` → `AuthenticationManager` → `DaoAuthenticationProvider` (hash + load + compare) → `SecurityContextHolderFilter` → `HttpSessionSecurityContextRepository` (creates HttpSession, stores SecurityContext) → JSESSIONID sent via `Set-Cookie`
- **Subsequent request flow:** `SecurityContextHolderFilter` → `HttpSessionSecurityContextRepository` (looks up session by JSESSIONID) → SecurityContext loaded into `SecurityContextHolder` → `AuthorizationFilter` checks roles → Controller
- **Session storage** — default: in-memory (Tomcat RAM); distributed systems need DB: `spring-session-jdbc` dependency + `spring.session.store-type=jdbc` + `spring.session.jdbc.initialize-schema=always`; creates `SPRING_SESSION` and `SPRING_SESSION_ATTRIBUTES` tables
- **Session expiry** — `server.servlet.session.timeout=5m`; `expiry = last_access_time + max_inactive_interval` (resets on each request, not fixed from creation)
- **SecurityConfig customization:**
    - `permitAll()` for public endpoints; `hasRole()` / `hasAnyRole()` for role restrictions; `authenticated()` for any-user-authenticated
    - `hasRole("USER")` auto-prepends `ROLE_` → checks for `ROLE_USER`; `hasAuthority("ROLE_USER")` no auto-prepend
    - Session controls: `maximumSessions(1)` + `maxSessionsPreventsLogin(true/false)`
    - Session creation policies: `IF_REQUIRED` (default), `ALWAYS`, `NEVER`, `STATELESS`
- **CSRF** — enabled by default for form-based; should NOT be disabled; CSRF protection requires `spring-boot-starter-security`
- **Disadvantages** — CSRF/session hijacking vulnerabilities; session management overhead; distributed system scalability (DB needed); DB load per request

---

## Lecture 19 Part 5 — Basic Authentication
**Core Concepts:** Stateless auth, Authorization header, Base64 encoding

- **What it is** — stateless; no session; credentials sent with EVERY request in `Authorization: Basic base64(username:password)` header
- **Base64 encoded, NOT encrypted** — trivially reversible; only safe over HTTPS
- **Why Authorization header** — RFC 7617 standard; headers not logged by servers (unlike body/query params); works for all HTTP methods including GET (no body)
- **Filter flow** — `BasicAuthenticationFilter` decodes header → `UsernamePasswordAuthenticationToken` → `AuthenticationManager` → `DaoAuthenticationProvider` → `SecurityContextHolder`; NO session created (`SessionCreationPolicy.STATELESS`); flow repeats every request
- **Config differences from Form-Based** — `.httpBasic(Customizer.withDefaults())` instead of `.formLogin()`; `SessionCreationPolicy.STATELESS`; `.csrf(csrf -> csrf.disable())` (stateless = no CSRF risk); no `spring-session-jdbc` needed
- **Disadvantages** — credentials travel every request (only Base64); DB lookup + BCrypt hash every request = performance overhead at scale; bad UX (browser native popup, no custom login, no proper logout); no revocation mechanism

---

## Lecture 19 Part 6 — JWT Structure & Theory
**Core Concepts:** JWT anatomy, JWS, JWE, signing algorithms, challenges

- **JWT structure** — `Header.Payload.Signature` (Base64 encoded, dot-separated)
    - **Header** — `typ` (JWT), `alg` (RSA/HMAC), `kid` (Key ID for public key lookup)
    - **Payload** — Registered claims (`iss`, `sub`, `aud`, `exp`, `nbf`, `iat`, `jti`) + Public claims (email, name) + Private claims (internal fields)
    - **Signature** — `sign(base64(Header).base64(Payload), key)` → tamper-proof
- **Signing algorithms** — HMAC (HS256): symmetric, same secret key for sign + verify; RSA (RS256): asymmetric, private key signs, public key verifies
- **JWT vs JWS vs JWE** — JWT = base concept (unsecured with `alg:none`); JWS = JWT + Signature (what everyone calls "JWT" in practice); JWE = JWT + Encrypted Payload (confidential data)
- **Payload is encoded, NOT encrypted** — anyone can decode it; never put passwords/secrets in payload; use JWE for confidentiality
- **Challenges:**
    1. Token invalidation — no built-in mechanism; solutions: blacklist (`jti`), key rotation (affects all users), short-lived tokens (most popular), single-use tokens
    2. `alg:none` (unsecured JWT) — always reject
    3. JWK exploit — attacker embeds own public key in `jwk` header; fix: use `kid` to look up key from auth server's own `jwks.json`, never from token's `jwk` field
- **Token sent as** — `Authorization: Bearer <token>`; Basic = username:password; Bearer = token

---

## Lecture 19 Part 7 — JWT Implementation (Custom)
**Core Concepts:** Custom filter chain, custom provider, custom authentication token — NOT using Keycloak/Spring OAuth Resource Server

**Approach used:** Fully custom implementation using raw Spring Security extension points.

- **Why no default** — JWT implementation varies (payload contents, algorithm choice, refresh strategy); Spring provides no out-of-box JWT support
- **Dependencies** — `jjwt-api` + `jjwt-impl` (runtime) + `jjwt-jackson` (JSON parsing of payload)

**Step 1 — Token Generation (`/generate-token`):**
- `LoginRequest` POJO captures username/password from request body
- `JWTUtil` — `generateToken(username, expiryMinutes)` using `Jwts.builder()` with `setSubject`, `setIssuedAt`, `setExpiration`, `signWith(key, HS256)`, `.compact()`; HMAC-SHA256 with hardcoded secret key (env var in production)
- `JWTAuthenticationFilter extends OncePerRequestFilter` — checks path == `/generate-token`; reads `LoginRequest` from body via `ObjectMapper`; creates `UsernamePasswordAuthenticationToken` (reuses `DaoAuthenticationProvider`'s existing logic for DB lookup + password compare); calls `authenticationManager.authenticate()`; on success: calls `jwtUtil.generateToken()` + sets `Authorization: Bearer <token>` header; does NOT call `filterChain.doFilter()` (no controller needed)
- `SecurityConfig` — manually creates `DaoAuthenticationProvider` bean (sets `UserDetailsService` + `PasswordEncoder`); manually creates `AuthenticationManager` bean as `new ProviderManager(Arrays.asList(daoProvider))`; adds filter via `.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)`

**Step 2 — Token Validation (all subsequent requests):**
- `JwtAuthenticationToken extends AbstractAuthenticationToken` — custom Authentication object; holds raw JWT string; `isAuthenticated=false` initially; `getCredentials()` returns token; `getCredentials()` returns null for principal (unknown until validated); TYPE determines which provider handles it
- `JWTUtil.validateAndExtractUsername(token)` — `Jwts.parser().setSigningKey(key).build().parseClaimsJws(token)` auto-validates signature + expiry; extracts `sub` claim (username); returns null on `JwtException`
- `JWTAuthenticationProvider implements AuthenticationProvider` — `supports()` returns true for `JwtAuthenticationToken.class`; `authenticate()`: extracts token → validates → loads user via `UserDetailsService` → returns `new UsernamePasswordAuthenticationToken(userDetails, null, authorities)` (3-arg constructor sets `isAuthenticated=true`)
- `JwtValidationFilter extends OncePerRequestFilter` — runs for ALL requests; extracts Bearer token from Authorization header; creates `JwtAuthenticationToken`; calls `authenticationManager.authenticate()`; stores result in `SecurityContextHolder`; DOES call `filterChain.doFilter()` (request proceeds to controller)
- `SecurityConfig` updated — adds `JWTAuthenticationProvider` to `ProviderManager` list; adds `JwtValidationFilter` via `.addFilterAfter(jwtValidationFilter, JWTAuthenticationFilter.class)`

**Step 3 — Refresh Token:**
- During login: generates short-lived access token (15 min) in `Authorization` header + long-lived refresh token (7 days) as `HttpOnly` cookie (`setHttpOnly(true)`, `setSecure(true)`, `setPath("/refresh-token")`, `setMaxAge(7*24*60*60)`)
- `JWTRefreshFilter extends OncePerRequestFilter` — checks path == `/refresh-token`; reads token from cookie (not header); creates `JwtAuthenticationToken`; passes to `AuthenticationManager` → same `JWTAuthenticationProvider` validates it; generates new access token; does NOT call `filterChain.doFilter()`
- Reuses `JwtAuthenticationToken` and `JWTAuthenticationProvider` for both access + refresh token validation

**Step 4 — Authorization:**
- `.requestMatchers("/api/users").hasRole("USER")` in `SecurityFilterChain` — single line addition; `AuthorizationFilter` reads from `SecurityContextHolder` (populated by `JwtValidationFilter`); 403 if role mismatch

**Final filter chain order:** `JWTAuthenticationFilter` → `JwtValidationFilter` → `JWTRefreshFilter` → `UsernamePasswordAuthenticationFilter` → `AuthorizationFilter` → Controller

---

## Lecture 19 Part 8 — OAuth 2.0 Theory
**Core Concepts:** 4 actors, Authorization Code Grant flow, grant types, `state` parameter

- **4 actors** — Resource Owner (user/you), Client (third-party app e.g. Instagram), Authorization Server (issues tokens e.g. Gmail Auth), Resource Server (holds protected data e.g. Gmail data)
- **Registration** — Client registers with Auth Server once; provides `redirect_uris` (up to 3); receives `client_id` (public) + `client_secret` (confidential, never expose)
- **Authorization Code Grant flow** — User clicks login → Client redirects to `/authorize` with `response_type=code`, `client_id`, `scope`, `state` → User authenticates + consents → Auth Server redirects to callback with `code` + `state` → Client validates state → Client POSTs to `/token` with `code` + `client_secret` (server-to-server) → Receives `access_token` + `refresh_token` → Uses access token as `Bearer` header to call Resource Server → Resource Server validates token with Auth Server → Returns user data
- **`state` parameter** — random unique value per request; echoed back in response; Client validates match; prevents CSRF attack (attacker can't know/guess the state value)
- **Token types** — Access Token (short-lived, calls Resource Server); Refresh Token (long-lived, gets new access token silently without user re-login)
- **Other grant types:**
    - **Implicit** — `response_type=token`; token in URL fragment directly; no server-to-server exchange; no refresh token; discouraged (token exposed in browser)
    - **Resource Owner Password Credentials** — client receives user's password directly; POSTs to `/token` with `grant_type=password`; only for highly trusted first-party apps
    - **Client Credentials** — no user; machine-to-machine; `grant_type=client_credentials`; client IS the resource owner; no refresh token needed (just re-request with `client_secret`)

---

## Lecture 19 Part 9 — OAuth2 Implementation (Spring Boot)
**Core Concepts:** Spring OAuth2 Client, OIDC, custom success handler, stateless token validation

**OAuth2 vs OIDC distinction:**
- OAuth2 = Authorization (access to data); token = opaque Access Token; scope = `read/write`
- OIDC = Authentication layer on top of OAuth2; token = ID Token (JWT with user identity); scope must include `openid`; ID Token only for the client app, never sent to Resource Server

**Approach used:** `spring-boot-starter-oauth2-client` + custom `AuthenticationSuccessHandler` + custom validation filter. NOT using Spring Security OAuth Resource Server or Keycloak.

**3 config changes:**
1. `pom.xml` — `spring-boot-starter-oauth2-client`
2. `application.properties` — per provider: `client-id`, `client-secret`, `scope`, `authorization-grant-type=authorization_code`, `redirect-uri`, `authorization-uri`, `token-uri`, `issuer-uri`, `jwk-set-uri` (for two providers: `gitlab` + `auth0` registration IDs)
3. `SecurityConfig` — `.oauth2Login(Customizer.withDefaults())` (default stateful version)

**Internal flow (auto-handled by framework):**
- `DefaultLoginPageGeneratingFilter` — generates login page from registered providers
- `OAuth2AuthorizationRequestRedirectFilter` — catches `/oauth2/authorization/{regId}`; builds auth request; redirects user to provider
- `OAuth2LoginAuthenticationFilter` — catches `/login/oauth2/code/{regId}`; extracts auth code; creates `OAuth2LoginAuthenticationToken`; passes to `OidcAuthorizationCodeAuthenticationProvider`; provider calls `/token` endpoint server-to-server; receives Access Token + ID Token (JWT); stores in `InMemoryOAuth2AuthorizedClientService`; creates HTTP Session; stores in `SecurityContextHolder`

**Making it stateless (custom additions):**
- `CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler` — overrides `onAuthenticationSuccess()`; casts Authentication to `OAuth2AuthenticationToken`; loads `OAuth2AuthorizedClient` via `OAuth2AuthorizedClientService`; extracts ID Token as JWT string via `((OidcUser) authToken.getPrincipal()).getIdToken().getTokenValue()`; writes `{"id_token": "..."}` to response body; NO session created
- `OAuthTokenValidatorUtil` — `validateAndExtractUsername(token)`: manually decodes JWT payload (Base64) to extract `iss` (issuer) claim; `JwtDecoders.fromIssuerLocation(issuer)` fetches correct `jwk-set-uri` for that issuer; `decoder.decode(token)` validates signature + expiry; returns `sub` claim; works for multiple providers automatically
- `OAuthValidationFilter extends OncePerRequestFilter` — extracts Bearer token from Authorization header; calls `tokenValidatorUtil.isTokenValid()`; on valid: creates `UsernamePasswordAuthenticationToken` + stores in `SecurityContextHolder`; always calls `filterChain.doFilter()`
- `SecurityConfig` updated — `SessionCreationPolicy.STATELESS`; `.oauth2Login(oauth -> oauth.successHandler(successHandler))`; `.addFilterBefore(OAuthValidationFilter, UsernamePasswordAuthenticationFilter.class)`

---

## Lecture 19 Part 10 — Role-Based Access Control (RBAC)
**Core Concepts:** `@PreAuthorize`, `@PostAuthorize`, SpEL, method-level security, interceptors

- **Problem with filter-layer-only authorization** — 100s of endpoints → giant `SecurityConfig`; scattered ownership (API in controller, rule in config); no per-method granularity
- **`@EnableMethodSecurity(prePostEnabled=true)`** — mandatory; without it all `@PreAuthorize`/`@PostAuthorize` annotations silently ignored
- **`@PreAuthorize`** — check before method runs; intercepted by `AuthorizationManagerBeforeMethodInterceptor`; 403 if fails, method never executes; most efficient
- **`@PostAuthorize`** — check after method runs, before response sent; intercepted by `AuthorizationManagerAfterMethodInterceptor`; has access to `returnObject`; business logic + DB queries already executed even if check fails; use only when authorization depends on returned data
- **`hasRole()` vs `hasAuthority()`** — both call `hasAnyAuthorityName()` in `SecurityExpressionRoot`; `hasRole("USER")` auto-prepends `ROLE_` → checks `ROLE_USER`; `hasAuthority("ORDER_READ")` checks exactly as written; convention: `hasRole` for high-level (USER/ADMIN), `hasAuthority` for granular permissions (ORDER_READ)
- **User model** — two levels: Role (high level: `ROLE_USER`) + Permissions (granular: `ORDER_READ`, `SALES_CREATE`); `UserLoginEntity implements UserDetails`; `getAuthorities()` adds both role and all permissions as `SimpleGrantedAuthority` into flat `Set`; `@OneToMany(fetch=FetchType.EAGER)` on permissions (must be EAGER — needed during authentication)
- **SpEL expressions** — string parsed by `SpelExpressionParser` into AST; resolved recursively; logical operators: `and`, `or`, `not`, `!`; relational: `==`, `!=`, `<`, `>`, `<=`, `>=`; method param access: `#paramName`; example: `@PreAuthorize("#id == authentication.principal.id")`
- **Two security layers working together** — Layer 1: `SecurityFilterChain` (is user authenticated? → 401); Layer 2: `@PreAuthorize` (does authenticated user have right role/permission? → 403)

---
Got it. Concise, structured, detail-rich but not verbose. Here's the rewrite:

---

## Lecture 20 — Spring Boot Actuator + Micrometer
**Core Concepts:** Production monitoring endpoints, health indicators, metrics, custom endpoints, Micrometer registry

- **Problem** — no standard way to answer: is app up? memory usage? deadlocked threads? DB pool exhausted? HTTP stats? Without Actuator: manual log digging, SSH, custom code per team
- **Solution** — one dependency (`spring-boot-starter-actuator`), Spring auto-configures standard HTTP endpoints; business logic and monitoring layer stay completely separate
- **Setup knobs in `application.properties`:**
    - `management.endpoints.web.base-path=/manage` — overrides default `/actuator`
    - `management.endpoints.web.exposure.include=*` — default exposes only `/health` and `/info`; `*` exposes all
- **`/health`** — aggregates all sub-component statuses; if ANY is DOWN, overall is DOWN; default response is just `{ "status": "UP" }`; `show-details=always` reveals per-component breakdown
    - **Custom health checks** — implement `HealthIndicator` interface + `@Component`; override `health()`; return `Health.up()` or `Health.down()` with `.withDetail()`; Spring auto-discovers all such beans; two written: `DatabaseHealthIndicator` (returns true) and `CacheHealthIndicator` (returns false → overall DOWN)
- **`/metrics`** — two-level: hit `/metrics` for full name list, then `/metrics/{name}` for data; categories: JVM memory (`jvm.memory.used/max`), GC (`jvm.gc.pause` — COUNT/TOTAL_TIME/MAX), threads (`jvm.threads.live/peak`), CPU (`system.cpu.usage`, 0.0–1.0), HTTP (`http.server.requests` — filterable by method/status via `availableTags`), JDBC pool (`jdbc.connections.active/idle/max` — pool exhaustion diagnosis), async executor (`executor.active/queued/completed`)
- **`/threaddump`** — full per-thread snapshot: `threadState` (RUNNABLE/WAITING/BLOCKED/TIMED_WAITING), `stackTrace`, `blockedCount`/`waitedCount`; use cases: deadlock detection (mutually BLOCKED threads), hung threads (WAITING + known stackTrace line), thread leak (count grows across repeated dumps)
- **Critical endpoints — `/shutdown` & `/heapdump`** — RESTRICTED by default even with `exposure.include=*`; require an explicit second property each: `management.endpoint.shutdown.access=unrestricted` and `management.endpoint.heapdump.access=unrestricted`; both properties AND exposure include needed together
    - `/shutdown` — single POST kills the entire app; prefer process managers (systemd, Kubernetes, Docker) in prod instead
    - `/heapdump` — downloads full `.hprof` heap file; readable via VisualVM/Eclipse MAT; can expose passwords, JWT tokens, API keys, user PII in plain text
- **Security** — dependency: `spring-boot-starter-security`; approach used: manual `SecurityFilterChain` bean with `HttpSecurity` DSL (`authorizeHttpRequests`, `requestMatchers`, `httpBasic`) — NOT Keycloak, NOT OAuth2 Resource Server, NOT custom JWT filter (those in separate 9-part Spring Security series); `/health` and `/info` kept public (`permitAll()`) so load balancers and monitoring tools work without auth; everything else requires `ADMIN` role; CSRF disabled (`.csrf(csrf -> csrf.disable())`) because actuator POST/DELETE clients don't send CSRF tokens; demo credentials hardcoded in properties (`spring.security.user.name/password/roles`) — explicitly called out as test-only
- **Custom endpoints** — `@Endpoint(id="x")` on class + `@Component`; `id` becomes URL path under base path; `@ReadOperation`→GET, `@WriteOperation`→POST, `@DeleteOperation`→DELETE; `@Selector` on params = path variable equivalent, positional; Spring matches by HTTP method + selector count; write/delete must always be secured; same mechanism used internally by Spring Cloud Config (`/refresh`), Resilience4j (circuit breaker endpoints) — standard extension point for the whole Spring ecosystem
- **Micrometer + Datadog** — Actuator alone is pull-based and manual (no history, no alerts); Micrometer is a vendor-neutral metrics facade (like SLF4J for metrics); swap registry dependency to change platform without touching app code; Datadog: `micrometer-registry-datadog` + `api-key` + `enabled=true` + `step=5s` → **push-based** (app pushes at each interval); Prometheus: `micrometer-registry-prometheus` → **pull-based** (Prometheus scrapes `/prometheus` endpoint on its own schedule); API key: use `${DATADOG_API_KEY}` env variable placeholder in prod, never hardcode

---