# Step 1 — The Problem: Why Annotation-Based Authorization?

---

## What We Already Know

Before this lecture, we had already learned how to do authorization at the **Security Filter Layer**. It looked something like this:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/orders").hasRole("USER")
                .requestMatchers("/sales").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(Customizer.withDefaults());

        return http.build();
    }
}
```

Simple enough. You list your APIs, assign roles. Done.

---

## So What's the Problem?

Imagine you're working at a company with **hundreds of APIs**. *(Yes, this is very real — large orgs easily have 100s of endpoints.)*

Now picture your `SecurityConfig` file looking like this:

```
/orders              → ROLE_USER
/orders/delete       → ROLE_ADMIN
/sales               → ROLE_MANAGER
/sales/create        → ROLE_ADMIN
/inventory           → ROLE_USER
/inventory/update    → ROLE_ADMIN
/payments            → ROLE_USER
/payments/refund     → ROLE_ADMIN
/reports             → ROLE_MANAGER
... (100 more)
```

This creates **real problems:**

```
┌─────────────────────────────────────────────────────────────┐
│              PROBLEMS WITH SECURITY FILTER APPROACH         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. ONE GIANT FILE                                          │
│     All 100+ API rules live in SecurityConfig.java          │
│     → Hard to read, hard to manage                          │
│                                                             │
│  2. SCATTERED OWNERSHIP                                     │
│     The API is defined in OrderController.java              │
│     But its security rule is in SecurityConfig.java         │
│     → You have to look in 2 places to understand 1 API      │
│                                                             │
│  3. SCALABILITY ISSUE                                       │
│     Every new API = update SecurityConfig                   │
│     → One wrong change can break everything                 │
│                                                             │
│  4. NO GRANULAR CONTROL                                     │
│     hasRole("USER") allows ALL users                        │
│     Can't say: "USER can read orders but not delete them"   │
│     without adding even more rules to SecurityConfig        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## The Better Solution

The instructor makes a very clean point here:

> *"The place where we have defined our endpoint — generally that's the controller — at that point itself, we tell what authorization is required for it."*

So instead of managing security in one central config file, **move the authorization rule right next to the API it protects** — inside the controller itself.

```
┌─────────────────────────────────────────────────────────────────┐
│                    THE BETTER APPROACH                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  BEFORE (Security Filter Layer):                                │
│                                                                 │
│  SecurityConfig.java                                            │
│  ├── /orders        → ROLE_USER                                 │
│  ├── /sales         → ROLE_ADMIN                                │
│  ├── /payments      → ROLE_USER                                 │
│  └── ... 100 more rules here                                    │
│                                                                 │
│  ─────────────────────────────────────────────────────────────  │
│                                                                 │
│  AFTER (Annotation-based, Controller Layer):                    │
│                                                                 │
│  OrderController.java                                           │
│  └── @PreAuthorize("hasRole('USER')")   ← rule lives HERE       │
│      GET /orders → fetch all orders                             │
│                                                                 │
│  SalesController.java                                           │
│  └── @PreAuthorize("hasRole('ADMIN')")  ← rule lives HERE       │
│      GET /sales → fetch all sales                               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

The security rule and the API it protects are **right next to each other**. Clean, readable, maintainable.

---

## Key Takeaway from Step 1

The Security Filter Layer approach works fine for small apps. But as your app grows, annotation-based authorization at the controller level is:

- **Easier to read** — rule is right above the method it protects
- **Easier to maintain** — each team owns their controller + its security
- **More scalable** — adding a new API doesn't touch a central config file
- **More granular** — you can control per-method, not just per-URL

---
# Step 2 — The Big Picture: `@PreAuthorize` vs `@PostAuthorize`

---

## Two Annotations, Two Different Timings

The instructor introduces two primary annotations for method-level authorization. The only difference between them is **when** the authorization check happens.

```
┌──────────────────────────────────────────────────────────────────────┐
│           ANNOTATIONS FOR ROLE-BASED AUTHORIZATION                   │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│                    @PreAuthorize                                     │
│                         │                                            │
│              Does authorization CHECK                                │
│              BEFORE the API logic runs                               │
│                                                                      │
│                    @PostAuthorize                                    │
│                         │                                            │
│              Does authorization CHECK                                │
│              AFTER the API logic runs                                │
│              but BEFORE response is sent to user                     │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## The Full Request Flow

Let's see where exactly these annotations fit in the bigger picture of a Spring Boot request:

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                        FULL REQUEST FLOW                                     │
└──────────────────────────────────────────────────────────────────────────────┘

   HTTP Request
       │
       ▼
┌─────────────────┐
│  Security       │   ← Authentication happens here
│  Filter Chain   │     (Basic Auth / JWT / OAuth)
│                 │     Authentication Object is created
│                 │     and stored in SecurityContextHolder
└────────┬────────┘
         │
         │  Authentication Object contains:
         │  ┌──────────────────────────────┐
         │  │  username: "john"            │
         │  │  granted authorities:        │
         │  │    - ROLE_USER               │
         │  │    - ORDER_READ              │
         │  │    - SALES_CREATE            │
         │  └──────────────────────────────┘
         │
         ▼
┌─────────────────┐
│  Dispatcher     │
│  Servlet        │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────┐
│                    INTERCEPTOR LAYER                    │
│                                                         │
│   @PreAuthorize CHECK happens HERE                      │
│   ┌──────────────────────────────────────────────────┐  │
│   │  AuthorizationManagerBeforeMethodInterceptor     │  │
│   │  Reads the annotation expression                 │  │
│   │  Checks against Authentication Object            │  │
│   │  ✅ Pass → continue to Controller                 │  │
│   │  ❌ Fail → throw 403 Forbidden immediately        │  │
│   └──────────────────────────────────────────────────┘  │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│                    CONTROLLER                           │
│                                                         │
│   @GetMapping("/orders")                                │
│   public OrderDTO readOrders() {                        │
│       // business logic runs here                       │
│       // returns OrderDTO object                        │
│   }                                                     │
└────────────────────────┬────────────────────────────────┘
                         │
                         │  Returns: OrderDTO
                         ▼
┌─────────────────────────────────────────────────────────┐
│                    INTERCEPTOR LAYER                    │
│                                                         │
│   @PostAuthorize CHECK happens HERE                     │
│   ┌──────────────────────────────────────────────────┐  │
│   │  AuthorizationManagerAfterMethodInterceptor      │  │
│   │  Has access to the returned object               │  │
│   │  Checks returnObject fields vs Authentication    │  │
│   │  ✅ Pass → send response to user                  │  │
│   │  ❌ Fail → throw 403 Forbidden, block response    │  │
│   └──────────────────────────────────────────────────┘  │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
                   HTTP Response
                   sent to user
```

---

## When to Use Which?

This is where a lot of beginners get confused. The instructor explains this very clearly:

```
┌──────────────────────────────────────────────────────────────────────┐
│                    @PreAuthorize                                     │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Use when: You can make the authorization decision                   │
│            BEFORE running the business logic                         │
│                                                                      │
│  Example:                                                            │
│  "Only ROLE_USER with ORDER_READ permission can call this API"       │
│                                                                      │
│  You already know the user's role & permissions                      │
│  from the Authentication Object → no need to run logic first         │
│                                                                      │
│  ✅ Most common scenario                                              │
│  ✅ More efficient (saves unnecessary DB calls if unauthorized)       │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│                    @PostAuthorize                                    │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Use when: You need to look at the RETURNED DATA                     │
│            to make the authorization decision                        │
│                                                                      │
│  Example:                                                            │
│  "User can call this API, but the order returned                     │
│   must belong to THIS user — not someone else's order"               │
│                                                                      │
│  You can't know this without running the business logic first        │
│  because the data (order's userID) only exists in the response       │
│                                                                      │
│  ⚠️  Business logic RUNS even if authorization fails                 │
│      (DB is queried, but response is blocked)                        │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## The Authentication Object — The Bridge

The instructor makes a very important point here. The **same Authentication Object** that was created during authentication (in the filter layer) travels all the way up to the controller level. Both `@PreAuthorize` and `@PostAuthorize` have access to this object.

```
┌───────────────────────────────────────────────────────┐
│              Authentication Object                    │
├───────────────────────────────────────────────────────┤
│                                                       │
│  principal:                                           │
│    id          → 1                                    │
│    username    → "john"                               │
│    password    → (encoded)                            │
│    role        → "ROLE_USER"                          │
│                                                       │
│  grantedAuthorities:                                  │
│    → ROLE_USER                                        │
│    → ORDER_READ                                       │
│    → SALES_CREATE                                     │
│                                                       │
│  isAuthenticated → true                               │
│                                                       │
└───────────────────────────────────────────────────────┘

This object is available inside your annotation expressions:

@PreAuthorize("authentication.principal.id == #id")
@PostAuthorize("returnObject.userID == authentication.principal.id")
```

---

## Key Takeaways from Step 2

- `@PreAuthorize` → check happens **before** method runs → handled by `AuthorizationManagerBeforeMethodInterceptor`
- `@PostAuthorize` → check happens **after** method runs, before response is sent → handled by `AuthorizationManagerAfterMethodInterceptor`
- Both annotations have full access to the **Authentication Object**
- `@PostAuthorize` additionally has access to **`returnObject`** (the value your method returned)
- Use `@PreAuthorize` when you can decide access from user's roles/permissions alone
- Use `@PostAuthorize` when the decision depends on the **data returned** by the method

---
# Step 3 — Setting Up the User: Entity, Permissions & `getAuthorities()`

---

## Why This Step is the Foundation

Before we even touch `@PreAuthorize` or `@PostAuthorize`, we need to understand **how Spring Security knows what roles and permissions a user has.**

The answer lies in one method: `getAuthorities()`

Everything — every role check, every permission check — ultimately comes from what this method returns. So if this is set up wrong, nothing else will work.

---

## The Data Model

The instructor designs the user system with **two levels of access control:**

```
┌──────────────────────────────────────────────────────────────────────┐
│                     TWO LEVELS OF ACCESS CONTROL                     │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  LEVEL 1 — ROLE (High Level)                                         │
│  ─────────────────────────────                                       │
│  A broad category the user belongs to                                │
│  Examples: ROLE_USER, ROLE_ADMIN, ROLE_MANAGER                       │
│                                                                      │
│  LEVEL 2 — PERMISSION (Granular Level)                               │
│  ──────────────────────────────────────                              │
│  Fine-grained actions the user can perform                           │
│  Examples: ORDER_READ, ORDER_DELETE, SALES_CREATE, SALES_READ        │
│                                                                      │
│  Real world example:                                                 │
│  ┌────────────┬────────────┬───────────────────────────────────────┐ │
│  │  Username  │   Role     │         Permissions                   │ │
│  ├────────────┼────────────┼───────────────────────────────────────┤ │
│  │  Shreyansh │ ROLE_USER  │ ORDER_READ, ORDER_DELETE, SALES_CREATE│ │
│  │  John      │ ROLE_ADMIN │ ORDER_READ, SALES_READ, SALES_DELETE  │ │
│  └────────────┴────────────┴───────────────────────────────────────┘ │
│                                                                      │
│  → Even though both are ROLE_USER, they can have                     │
│    completely different permissions                                  │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## The Database Design

The instructor creates two tables with a **One-to-Many** relationship:

```
┌─────────────────────────────────────────────────────────────────────┐
│                        DATABASE DESIGN                              │
└─────────────────────────────────────────────────────────────────────┘

  TABLE: user_login                    TABLE: user_permission
  ┌────┬──────────┬──────────┬───────┐  ┌────┬───────────────┐
  │ id │ username │ password │ role  │  │ id │ name          │
  ├────┼──────────┼──────────┼───────┤  ├────┼───────────────┤
  │ 1  │ john     │ $2a$...  │ROLE_  │  │ 1  │ ORDER_READ    │
  │    │          │          │USER   │  │ 2  │ ORDER_DELETE  │
  │ 2  │ admin    │ $2a$...  │ROLE_  │  │ 3  │ SALES_CREATE  │
  │    │          │          │ADMIN  │  │ 4  │ SALES_READ    │
  └────┴──────────┴──────────┴───────┘  └────┴───────────────┘

  One user_login → Many user_permissions

  user_login (id=1, john, ROLE_USER)
       │
       ├──→ user_permission (ORDER_READ)
       └──→ user_permission (ORDER_DELETE)
```

---

## The Code — 4 Files to Set Up

### File 1: `UserPermissionEntity.java`
The entity representing a single permission:

```java
@Entity
@Table(name = "user_permission")
public class UserPermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // e.g., ORDER_READ, SALES_READ, SALES_CREATE
    private String name;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
```

---

### File 2: `UserLoginEntity.java` — THE MOST IMPORTANT FILE
This is where `getAuthorities()` lives. This is the method Spring Security calls to know what the user is allowed to do:

```java
@Entity
@Table(name = "user_login")
public class UserLoginEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String role; // e.g., "ROLE_USER" or "ROLE_ADMIN"

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<UserPermissionEntity> permissions = new ArrayList<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // Add the ROLE (high level)
        authorities.add(new SimpleGrantedAuthority(role));

        // Add all PERMISSIONS (granular level)
        permissions.forEach(permission ->
            authorities.add(new SimpleGrantedAuthority(permission.getName()))
        );

        return authorities;
    }

    @Override
    public String getPassword() { return password; }

    @Override
    public String getUsername() { return username; }
}
```

---

### File 3: `UserLoginEntityService.java`
This is the service Spring Security uses to **load user by username** during authentication:

```java
@Service
public class UserLoginEntityService implements UserDetailsService {

    @Autowired
    private UserLoginEntityRepository userLoginEntityRepository;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        return userLoginEntityRepository.findByUsername(username)
                .orElseThrow(() ->
                    new UsernameNotFoundException("user not found"));
    }

    public UserDetails save(UserLoginEntity userLoginEntity) {
        return userLoginEntityRepository.save(userLoginEntity);
    }
}
```

---

### File 4: `UserLoginEntityRepository.java`

```java
@Repository
public interface UserLoginEntityRepository
        extends JpaRepository<UserLoginEntity, Long> {

    Optional<UserLoginEntity> findByUsername(String username);
}
```

---

### File 5: `UserLoginController.java`
The endpoint to **create/register a new user:**

```java
@RestController
public class UserLoginController {

    @Autowired
    UserLoginEntityService userLoginEntityService;

    @Autowired
    PasswordEncoder passwordEncoder;

    @PostMapping("/user-login")
    public ResponseEntity<String> login(
            @RequestBody UserLoginEntity userLoginEntity) {

        // Hash the password before saving
        userLoginEntity.setPassword(
            passwordEncoder.encode(userLoginEntity.getPassword())
        );

        userLoginEntityService.save(userLoginEntity);

        return ResponseEntity.ok("User registered successfully!");
    }
}
```

Request body to create a user:
```json
{
    "username": "john",
    "password": "123",
    "role": "ROLE_USER",
    "permissions": [
        { "name": "ORDER_READ" }
    ]
}
```

---

## The Critical Part — How `getAuthorities()` Feeds into Everything

This is the most important thing to understand from this whole step. The instructor explains it very clearly:

```
┌──────────────────────────────────────────────────────────────────────┐
│              HOW getAuthorities() FEEDS INTO SPRING SECURITY         │
└──────────────────────────────────────────────────────────────────────┘

  UserLoginEntity.getAuthorities() returns:
  ┌─────────────────────────────┐
  │  Set<GrantedAuthority>      │
  │  ─────────────────────────  │
  │  → ROLE_USER                │  ← from role field
  │  → ORDER_READ               │  ← from permissions list
  │  → SALES_CREATE             │  ← from permissions list
  └─────────────────────────────┘
           │
           │  Spring Security takes this list and
           │  puts it inside the Authentication Object
           ▼
  ┌──────────────────────────────────────────┐
  │         Authentication Object            │
  │  ──────────────────────────────────────  │
  │  principal        → UserLoginEntity      │
  │  grantedAuthorities:                     │
  │    → ROLE_USER                           │
  │    → ORDER_READ                          │
  │    → SALES_CREATE                        │
  │  isAuthenticated  → true                 │
  └──────────────────────────────────────────┘
           │
           │  This object is what
           │  @PreAuthorize and @PostAuthorize
           │  check against!
           ▼
  @PreAuthorize("hasRole('USER') and hasAuthority('ORDER_READ')")
  → checks: does grantedAuthorities contain ROLE_USER? ✅
  → checks: does grantedAuthorities contain ORDER_READ? ✅
  → Allow access ✅
```

---

## Key Takeaways from Step 3

- A user has **one role** (high level) and **many permissions** (granular level)
- Both role and permissions are loaded into a **single flat list** inside `getAuthorities()`
- This list becomes the `grantedAuthorities` inside the **Authentication Object**
- `@PreAuthorize` and `@PostAuthorize` check against this exact list
- `FetchType.EAGER` on permissions is important — permissions must be loaded immediately with the user, not lazily, because Spring Security needs them during authentication
- Password is always **encoded** before saving using `BCryptPasswordEncoder`

---
# Step 4 — Enabling Method Security + Security Config

---

## The Critical Switch

Here's something very important the instructor highlights:

> *"Without enabling this, `@PreAuthorize` and `@PostAuthorize` annotations will be ignored."*

By default, Spring Security does **not** process these annotations. You have to explicitly turn them on. Just one annotation on your config class does it:

```
@EnableMethodSecurity(prePostEnabled = true)
```

Without this, you could put `@PreAuthorize` on every single method in your app — and Spring Security would **silently ignore all of them.** No error, no warning. Just no security. This is a very common gotcha for beginners.

---

## The Full Security Config

```java
@Configuration
@EnableWebSecurity
/*
 * Without enabling this, @PreAuthorize and
 * @PostAuthorize annotations will be ignored.
 */
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http.authorizeHttpRequests(auth -> auth
                // Allow user registration without authentication
                .requestMatchers("/user-login").permitAll()
                // Everything else needs authentication
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .csrf(csrf -> csrf.disable())
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
```

---

## Breaking Down Each Decision in This Config

The instructor is using **Basic Authentication** here just for simplicity of testing. The whole point of this lecture is authorization, not authentication. The annotation-based authorization works exactly the same whether you use Basic Auth, JWT, or OAuth.

```
┌───────────────────────────────────────────────────────────────────────┐
│                    SECURITY CONFIG DECISIONS                          │
├───────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  @EnableMethodSecurity(prePostEnabled = true)                         │
│  → Activates @PreAuthorize and @PostAuthorize                         │
│  → Without this, annotations are completely ignored                   │
│                                                                       │
│  .requestMatchers("/user-login").permitAll()                          │
│  → User registration endpoint must be open                            │
│  → Otherwise no one can create a user to test with!                   │
│                                                                       │
│  .anyRequest().authenticated()                                        │
│  → Every other endpoint needs authentication at minimum               │
│  → The GRANULAR authorization (role/permission check)                 │
│    is handled by @PreAuthorize on each controller method              │
│                                                                       │
│  SessionCreationPolicy.STATELESS                                      │
│  → No session is created or stored on server                          │
│  → Every request must carry credentials (suits REST APIs)             │
│                                                                       │
│  .csrf(csrf -> csrf.disable())                                        │
│  → CSRF protection disabled for stateless REST APIs                   │
│  → CSRF is only needed for browser-based form submissions             │
│                                                                       │
│  .httpBasic(Customizer.withDefaults())                                │
│  → Using Basic Auth for simplicity of testing                         │
│  → Can be swapped with JWT / OAuth — annotations still work           │
│                                                                       │
└───────────────────────────────────────────────────────────────────────┘
```

---

## How the Two Layers of Security Now Work Together

This is a really important mental model to have. After this config, your app has **two layers** of security:

```
┌──────────────────────────────────────────────────────────────────────┐
│                    TWO LAYERS OF SECURITY                            │
└──────────────────────────────────────────────────────────────────────┘

  LAYER 1 — Security Filter Chain (Global Level)
  ───────────────────────────────────────────────
  Defined in SecurityConfig
  Answers the question: "Is the user authenticated at all?"

  Rule:
  → /user-login       → anyone can access (permitAll)
  → everything else   → must be authenticated

  ┌─────────────────────────────────────────────┐
  │  Not authenticated?                         │
  │  → 401 Unauthorized ← stopped HERE          │
  └─────────────────────────────────────────────┘


  LAYER 2 — Method Security (Controller Level)
  ─────────────────────────────────────────────
  Defined via @PreAuthorize / @PostAuthorize on controller methods
  Answers the question: "Does this authenticated user have
                         the RIGHT role/permission for THIS API?"

  Rule:
  → @PreAuthorize("hasRole('USER') and hasAuthority('ORDER_READ')")

  ┌─────────────────────────────────────────────┐
  │  Authenticated but wrong role/permission?   │
  │  → 403 Forbidden ← stopped HERE             │
  └─────────────────────────────────────────────┘


  FULL FLOW:

  Request
    │
    ▼
  ┌─────────────────────┐
  │  Filter Chain       │  ← Layer 1: Are you authenticated?
  │  (SecurityConfig)   │
  └────────┬────────────┘
           │ ✅ Yes, authenticated
           ▼
  ┌─────────────────────┐
  │  @PreAuthorize      │  ← Layer 2: Do you have the right
  │  on Controller      │             role & permission?
  │  Method             │
  └────────┬────────────┘
           │ ✅ Yes, authorized
           ▼
  ┌─────────────────────┐
  │  Controller Method  │  ← Business logic runs
  │  Executes           │
  └─────────────────────┘
```

---

## Key Takeaways from Step 4

- `@EnableMethodSecurity(prePostEnabled = true)` is **mandatory** — without it, all your `@PreAuthorize` and `@PostAuthorize` annotations are silently ignored
- The Security Filter Chain handles **authentication** (Layer 1 — are you logged in?)
- The method-level annotations handle **authorization** (Layer 2 — do you have the right permissions?)
- The authentication type (Basic Auth / JWT / OAuth) doesn't matter for annotation-based authorization — it works the same way with all of them
- `/user-login` must be `permitAll()` so users can register without being authenticated first

---
# Step 5 — `@PreAuthorize` in Action + `hasRole` vs `hasAuthority` Deep Dive

---

## The Controllers

The instructor creates two controllers for testing — one for Orders, one for Sales:

### `OrderController.java`
```java
@RestController
@RequestMapping("/api")
public class OrderController {

    @GetMapping("/orders")
    @PreAuthorize("hasRole('USER') and hasAuthority('ORDER_READ')")
    public ResponseEntity<String> readOrders() {
        return ResponseEntity.ok("ALL orders has been fetched successfully");
    }
}
```

### `SalesController.java`
```java
@RestController
@RequestMapping("/api")
public class SalesController {

    @GetMapping("/sales")
    @PreAuthorize("hasAuthority('SALES_READ')")
    public ResponseEntity<String> readSalesDetails() {
        return ResponseEntity.ok("ALL Sales details has been fetched successfully");
    }
}
```

---

## The Test User We Created (from Step 3)

```json
{
    "username": "john",
    "password": "123",
    "role": "ROLE_USER",
    "permissions": [
        { "name": "ORDER_READ" }
    ]
}
```

So this user has:
- `ROLE_USER` → the role
- `ORDER_READ` → the only permission

---

## What Happens When This User Hits Each API

```
┌──────────────────────────────────────────────────────────────────────┐
│                         TEST RESULTS                                 │
└──────────────────────────────────────────────────────────────────────┘

  USER: john | PASSWORD: 123
  grantedAuthorities: [ROLE_USER, ORDER_READ]

  ─────────────────────────────────────────────────────────────────────

  API 1: GET /api/orders
  @PreAuthorize("hasRole('USER') and hasAuthority('ORDER_READ')")

  Check 1: hasRole('USER')
  → internally looks for ROLE_USER in grantedAuthorities
  → ROLE_USER is present ✅

  Check 2: hasAuthority('ORDER_READ')
  → looks for ORDER_READ in grantedAuthorities
  → ORDER_READ is present ✅

  Both checks pass → ✅ 200 OK
  Response: "ALL orders has been fetched successfully"

  ─────────────────────────────────────────────────────────────────────

  API 2: GET /api/sales
  @PreAuthorize("hasAuthority('SALES_READ')")

  Check: hasAuthority('SALES_READ')
  → looks for SALES_READ in grantedAuthorities
  → SALES_READ is NOT present ❌

  Check fails → ❌ 403 Forbidden
  Response: { "error": "Forbidden" }

  ─────────────────────────────────────────────────────────────────────
```

---

## `hasRole` vs `hasAuthority` — The Big Question

At this point the instructor says this question will come to your mind:

> *"What is the difference between `hasRole` and `hasAuthority`?"*

The short answer: **at the code level, there is no real difference.** The only difference is a prefix.

Let's go through it exactly as the instructor explains:

```
┌───────────────────────────────────────────────────────────────────────┐
│               hasRole vs hasAuthority — THE ONLY DIFFERENCE           │
├───────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  hasRole('USER')                                                      │
│  → Spring automatically prepends "ROLE_"                              │
│  → So it actually looks for "ROLE_USER" in grantedAuthorities         │
│  → You don't have to write ROLE_ yourself                             │
│                                                                       │
│  hasAuthority('ORDER_READ')                                           │
│  → No prefix added                                                    │
│  → Looks for exactly "ORDER_READ" in grantedAuthorities               │
│  → What you write is what gets checked                                │
│                                                                       │
│  That's it. No other difference.                                      │
│                                                                       │
│  Convention (not a rule):                                             │
│  → Use hasRole()      for HIGH LEVEL distinction  (USER, ADMIN)       │
│  → Use hasAuthority() for GRANULAR level          (ORDER_READ)        │
│                                                                       │
└───────────────────────────────────────────────────────────────────────┘
```

---

## Proof — Inside Spring Security Source Code

The instructor opens `SecurityExpressionRoot.java` to prove this. Let's walk through it exactly:

```java
// SecurityExpressionRoot.java (Spring Security source code)

// Step 1: hasAuthority() calls hasAnyAuthority()
@Override
public final boolean hasAuthority(String authority) {
    return hasAnyAuthority(authority);
}

// Step 2: hasAnyAuthority() calls hasAnyAuthorityName() with NULL prefix
@Override
public final boolean hasAnyAuthority(String... authorities) {
    return hasAnyAuthorityName(null, authorities);
    //                          ↑
    //                    prefix is NULL
    //                    (nothing gets added)
}

// Step 3: hasRole() calls hasAnyRole()
@Override
public final boolean hasRole(String role) {
    return hasAnyRole(role);
}

// Step 4: hasAnyRole() calls hasAnyAuthorityName() with DEFAULT PREFIX
@Override
public final boolean hasAnyRole(String... roles) {
    return hasAnyAuthorityName(this.defaultRolePrefix, roles);
    //                          ↑
    //                    prefix is "ROLE_"
    //                    (gets prepended automatically)
}

// Step 5: BOTH end up at the EXACT SAME METHOD
private boolean hasAnyAuthorityName(String prefix, String... roles) {
    Set<String> roleSet = getAuthoritySet(); // gets grantedAuthorities list

    for (String role : roles) {
        // if prefix is "ROLE_" → "ROLE_USER"
        // if prefix is null   → "ORDER_READ" (unchanged)
        String defaultedRole = getRoleWithDefaultPrefix(prefix, role);

        if (roleSet.contains(defaultedRole)) {
            return true;
        }
    }
    return false;
}
```

---

## The Full Picture — How Both Methods Work

```
┌──────────────────────────────────────────────────────────────────────┐
│           hasRole vs hasAuthority INTERNAL FLOW                      │
└──────────────────────────────────────────────────────────────────────┘

  hasRole('USER')                    hasAuthority('ORDER_READ')
       │                                      │
       ▼                                      ▼
  hasAnyRole('USER')            hasAnyAuthority('ORDER_READ')
       │                                      │
       │  prefix = "ROLE_"        prefix = null
       ▼                                      ▼
       └──────────────┬───────────────────────┘
                      │
                      ▼
          hasAnyAuthorityName(prefix, value)
                      │
                      ▼
          getAuthoritySet()
          → gets the full grantedAuthorities list:
            [ROLE_USER, ORDER_READ, SALES_CREATE]
                      │
                      ▼
          getRoleWithDefaultPrefix(prefix, value)
          → hasRole:      "ROLE_" + "USER"       = "ROLE_USER"
          → hasAuthority: null   + "ORDER_READ"  = "ORDER_READ"
                      │
                      ▼
          roleSet.contains(defaultedRole)
          → "ROLE_USER"    in list? ✅ return true
          → "ORDER_READ"   in list? ✅ return true
          → "SALES_READ"   in list? ❌ return false
```

---

## How `getAuthorities()` Fills This List

The instructor connects this back to the `getAuthorities()` method we wrote in Step 3:

```
┌──────────────────────────────────────────────────────────────────────┐
│         WHERE THE grantedAuthorities LIST COMES FROM                 │
└──────────────────────────────────────────────────────────────────────┘

  UserLoginEntity.getAuthorities():
  ┌────────────────────────────────────────────────────┐
  │  authorities.add(new SimpleGrantedAuthority(role)) │
  │  → adds "ROLE_USER"                                │
  │                                                    │
  │  permissions.forEach(p ->                          │
  │    authorities.add(                                │
  │      new SimpleGrantedAuthority(p.getName())       │
  │    )                                               │
  │  )                                                 │
  │  → adds "ORDER_READ"                               │
  │  → adds "SALES_CREATE"                             │
  └────────────────────────────────────────────────────┘
                      │
                      ▼
  Final grantedAuthorities list in Authentication Object:
  ┌─────────────────────┐
  │  → ROLE_USER        │  ← role (high level)
  │  → ORDER_READ       │  ← permission (granular)
  │  → SALES_CREATE     │  ← permission (granular)
  └─────────────────────┘
                      │
                      ▼
  hasRole('USER')      → looks for ROLE_USER   ✅
  hasAuthority('ORDER_READ') → looks for ORDER_READ ✅
  hasAuthority('SALES_READ') → looks for SALES_READ ❌
```

---

## Interview Tip 🎯

The instructor specifically explains this at source code level for a reason — this is a **very commonly asked interview question:**

> *"What is the difference between `hasRole()` and `hasAuthority()` in Spring Security?"*

**Perfect answer:**

Both `hasRole()` and `hasAuthority()` internally call the exact same method — `hasAnyAuthorityName()` in `SecurityExpressionRoot.java`. The only difference is that `hasRole()` automatically prepends `"ROLE_"` to whatever string you pass, while `hasAuthority()` uses the string exactly as you provide it. By convention, `hasRole()` is used for high-level distinctions like USER or ADMIN, and `hasAuthority()` is used for granular permissions like ORDER_READ or SALES_DELETE. But at the code level, they are identical in behavior.

---

## Key Takeaways from Step 5

- `@PreAuthorize` takes a **SpEL expression** as a string — we'll go deep on SpEL in the next step
- `hasRole('USER')` automatically prepends `ROLE_` → looks for `ROLE_USER`
- `hasAuthority('ORDER_READ')` looks for exactly `ORDER_READ` — no prefix added
- Both ultimately call the **same internal method** in `SecurityExpressionRoot.java`
- Everything is checked against a **single flat list** — the `grantedAuthorities` set
- You can combine checks using `and`, `or`, `not` inside the annotation expression

---
# Step 6 — How It Works Under the Hood: Interceptors + SpEL

---

## The Question

After seeing `@PreAuthorize` work, a natural question comes up:

> *"How does this authorization method get invoked BEFORE the controller method runs? Who is taking care of it?"*

The answer is: **Interceptors.**

---

## Quick Recap — Filters vs Interceptors

The instructor references his previous videos on this. Here's the mental model you need:

```
┌──────────────────────────────────────────────────────────────────────┐
│                  FILTERS vs INTERCEPTORS                             │
└──────────────────────────────────────────────────────────────────────┘

  HTTP Request
      │
      ▼
  ┌──────────────────────────────────────────────────────────────┐
  │  SERVLET CONTAINER (Tomcat)                                  │
  │                                                              │
  │  ┌──────────────┐                                            │
  │  │ Filter Chain │  ← Filters live HERE                       │
  │  │  Filter 1    │    Outside Spring context                  │
  │  │  Filter 2    │    Work on raw HttpRequest/HttpResponse    │
  │  │  Filter n    │    e.g. Security Filter Chain lives here   │
  │  └──────┬───────┘                                            │
  │         │                                                    │
  │         ▼                                                    │
  │  ┌──────────────────┐                                        │
  │  │ Dispatcher       │  ← Entry point into Spring context     │
  │  │ Servlet          │                                        │
  │  └──────┬───────────┘                                        │
  │         │                                                    │
  │         ▼                                                    │
  │  ┌──────────────────┐                                        │
  │  │  Interceptors    │  ← Interceptors live HERE              │
  │  │                  │    Inside Spring context               │
  │  │  @PreAuthorize   │    Work on Spring's HandlerMethod      │
  │  │  is handled here │    Have access to Spring beans         │
  │  └──────┬───────────┘                                        │
  │         │                                                    │
  │         ▼                                                    │
  │  ┌──────────────────┐                                        │
  │  │   Controller     │  ← Your method runs here               │
  │  └──────────────────┘                                        │
  │                                                              │
  └──────────────────────────────────────────────────────────────┘
```

---

## The Specific Interceptor — `AuthorizationManagerBeforeMethodInterceptor`

Spring Security ships with a built-in interceptor that specifically watches for `@PreAuthorize`:

```
┌───────────────────────────────────────────────────────────────────────┐
│         AuthorizationManagerBeforeMethodInterceptor                   │
├───────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  This interceptor:                                                    │
│  → Watches every method call in your Spring app                       │
│  → Checks if the method has @PreAuthorize annotation                  │
│  → If yes → reads the expression string from it                       │
│  → Evaluates the expression against Authentication Object             │
│  → ✅ passes → lets the method execute                                 │
│  → ❌ fails  → throws AccessDeniedException (403 Forbidden)            │
│                                                                       │
│  Similarly for @PostAuthorize:                                        │
│  AuthorizationManagerAfterMethodInterceptor                           │
│  → Intercepts AFTER the method returns                                │
│  → Has access to the returned object (returnObject)                   │
│                                                                       │
└───────────────────────────────────────────────────────────────────────┘
```

---

## What is SpEL?

The string you write inside `@PreAuthorize(...)` is not just a plain string. It is a **SpEL expression** — Spring Expression Language.

```java
@PreAuthorize("hasRole('USER') and hasAuthority('ORDER_READ')")
//             ↑
//    This entire string is a SpEL expression
```

SpEL is a powerful expression language built into Spring that lets you write logic — method calls, comparisons, logical operators — as a string, which Spring then evaluates at runtime.

---

## How SpEL Processes the Expression — Step by Step

The instructor walks through the full internal flow. Here it is as a detailed diagram:

```
┌──────────────────────────────────────────────────────────────────────┐
│    FULL INTERNAL FLOW OF @PreAuthorize PROCESSING                    │
└──────────────────────────────────────────────────────────────────────┘

  @PreAuthorize("hasRole('USER') and hasAuthority('ORDER_READ')")
                │
                │
  STEP 1: Interceptor reads the string
  ──────────────────────────────────────
                │
                ▼
  "hasRole('USER') and hasAuthority('ORDER_READ')"
  → Currently just a plain String


  STEP 2: SpelExpressionParser converts String → AST
  ────────────────────────────────────────────────────
                │
                ▼
  SpelExpressionParser parses the string into an
  Abstract Syntax Tree (AST):

                    [AND]  ← root operation
                   /     \
     [hasRole('USER')]   [hasAuthority('ORDER_READ')]
      left operand            right operand

  The tree represents the logical structure of your expression.
  Complex expressions create deeper trees:

  "hasRole('ADMIN') or (hasRole('USER') and hasAuthority('READ'))"

                         [OR]
                        /    \
            [hasRole('ADMIN')] [AND]
                              /     \
                   [hasRole('USER')] [hasAuthority('READ')]


  STEP 3: Interceptor resolves the AST recursively
  ──────────────────────────────────────────────────
                │
                ▼
  Spring walks the tree recursively:

  → Hits [AND] node
    → Resolve LEFT child: hasRole('USER')
      → calls SecurityExpressionRoot.hasRole('USER')
      → checks grantedAuthorities list
      → returns true ✅
    → Resolve RIGHT child: hasAuthority('ORDER_READ')
      → calls SecurityExpressionRoot.hasAuthority('ORDER_READ')
      → checks grantedAuthorities list
      → returns true ✅
    → AND(true, true) = true ✅

  You never write this recursive logic yourself.
  Spring handles all of it internally.


  STEP 4: Interceptor validates the final result
  ────────────────────────────────────────────────
                │
                ▼
  Final result = true  → ✅ Let the controller method execute
  Final result = false → ❌ Throw 403 Forbidden, block execution
```

---

## How the Interceptor Gets the Authentication Object

The instructor explains this too. The interceptor doesn't magically know who the user is — it gets it from the `SecurityContextHolder`:

```java
// Inside AuthorizationManagerBeforeMethodInterceptor (simplified)

Authentication authentication =
    SecurityContextHolder.getContext().getAuthentication();

// This is the same Authentication Object that was created
// during the filter chain (Basic Auth / JWT / OAuth)
// and stored in SecurityContextHolder

// Now it uses this authentication to evaluate your SpEL expression:
// hasRole('USER') → checks authentication.getAuthorities()
// authentication.principal.id → checks authentication.getPrincipal().getId()
```

```
┌──────────────────────────────────────────────────────────────────────┐
│              WHERE DOES THE INTERCEPTOR GET THE USER INFO?           │
└──────────────────────────────────────────────────────────────────────┘

  During Filter Chain (earlier):
  ┌────────────────────────────────────────┐
  │  BasicAuthFilter / JWTFilter           │
  │  → authenticates the user              │
  │  → creates Authentication Object       │
  │  → stores it in SecurityContextHolder  │
  └────────────────────────────────────────┘
                    │
                    │  stored here ↓
                    ▼
  ┌────────────────────────────────────────┐
  │  SecurityContextHolder                 │
  │  └── SecurityContext                   │
  │       └── Authentication Object        │
  │            ├── principal (user info)   │
  │            ├── grantedAuthorities      │
  │            └── isAuthenticated: true   │
  └────────────────────────────────────────┘
                    │
                    │  interceptor reads from here ↓
                    ▼
  ┌────────────────────────────────────────┐
  │  AuthorizationManagerBefore            │
  │  MethodInterceptor                     │
  │  → getAuthentication()                 │
  │  → evaluates SpEL expression           │
  │  → allow or deny                       │
  └────────────────────────────────────────┘
```

---

## SpEL Operators Available in `@PreAuthorize`

Since the expression is SpEL, you get a full set of operators to build complex authorization logic:

### Logical Operators
```
┌───────────┬──────────────┬──────────────────────────────────────────┐
│ Operator  │ Description  │ Example                                  │
├───────────┼──────────────┼──────────────────────────────────────────┤
│ and       │ Logical AND  │ hasRole('ADMIN') and hasAuthority('READ')│
│ or        │ Logical OR   │ hasRole('ADMIN') or hasRole('USER')      │
│ not       │ Logical NOT  │ not hasRole('ADMIN')                     │
│ !         │ Logical NOT  │ !hasAuthority('DELETE')                  │
└───────────┴──────────────┴──────────────────────────────────────────┘
```

### Relational Operators
```
┌───────────┬──────────────────────┬───────────────────────────────────┐
│ Operator  │ Description          │ Example                           │
├───────────┼──────────────────────┼───────────────────────────────────┤
│ ==        │ Equal                │ #id == authentication.principal.id│
│ !=        │ Not equal            │ #value != 15                      │
│ <         │ Less than            │ #value < 100                      │
│ >         │ Greater than         │ #value > 100                      │
│ <=        │ Less than or equal   │ #value <= 15                      │
│ >=        │ Greater than or equal│ #value >= 90                      │
└───────────┴──────────────────────┴───────────────────────────────────┘
```

### Accessing Method Parameters in SpEL
You can even use the actual method parameter values inside your expression using `#paramName`:

```java
// Example: ensure user can only fetch their own details
@PreAuthorize("#id == authentication.principal.id")
@GetMapping("/users/{id}")
public User fetchUserDetails(@PathVariable Long id) {
    return userServiceObject.fetchUserDetails(id);
}
```

```
How this works:

  Request: GET /users/3
  Authenticated user's principal.id = 2

  SpEL evaluates: #id == authentication.principal.id
                   3  ==  2
                   → false ❌ → 403 Forbidden

  Request: GET /users/2
  Authenticated user's principal.id = 2

  SpEL evaluates: #id == authentication.principal.id
                   2  ==  2
                   → true ✅ → method executes
```

---

## The Full Picture Together

```
┌──────────────────────────────────────────────────────────────────────┐
│              COMPLETE FLOW — FROM ANNOTATION TO DECISION             │
└──────────────────────────────────────────────────────────────────────┘

  @PreAuthorize("hasRole('USER') and hasAuthority('ORDER_READ')")
        │
        │ detected by
        ▼
  AuthorizationManagerBeforeMethodInterceptor
        │
        ├── reads Authentication from SecurityContextHolder
        │
        ├── reads SpEL string from annotation
        │
        ├── passes string to SpelExpressionParser
        │   → String converted to AST
        │
        ├── resolves AST recursively
        │   → calls hasRole(), hasAuthority() etc.
        │   → these check grantedAuthorities list
        │   → logical operators combine results
        │
        └── final result
              │
              ├── true  → ✅ controller method executes
              └── false → ❌ 403 Forbidden thrown
```

---

## Key Takeaways from Step 6

- `@PreAuthorize` is intercepted by `AuthorizationManagerBeforeMethodInterceptor` — a built-in Spring Security interceptor
- `@PostAuthorize` is intercepted by `AuthorizationManagerAfterMethodInterceptor`
- The expression inside `@PreAuthorize(...)` is a **SpEL (Spring Expression Language)** string
- Spring parses this string into an **Abstract Syntax Tree (AST)** and resolves it recursively
- The interceptor gets the user's authentication from **`SecurityContextHolder`** — the same object created during filter-level authentication
- You can use `#paramName` in SpEL to reference actual method parameter values for very fine-grained control
- You don't write any of this parsing/recursive logic yourself — Spring handles it all internally

---
# Step 7 — `@PostAuthorize` in Action

---

## The Concept — Why Do We Need `@PostAuthorize`?

The instructor sets up the problem very clearly:

> *"Sometimes you need to execute the code logic first. And whatever the response you are getting inside that — it might have certain data which we have to use. And based on that we have to do the authorization logic and then decide — do we have to allow the user to access this API or not?"*

Real world example:
- Two users: `a_user` (id=1) and `b_user` (id=2)
- Both have `ROLE_USER` and `ORDER_READ` permission
- Both are allowed to call `GET /api/orders`
- BUT — each user should only get **their own orders**, not someone else's

`@PreAuthorize` can't solve this. Why? Because at the time of the pre-check, the order data hasn't been fetched yet. You don't know **whose order** the DB will return until the method actually runs.

That's exactly where `@PostAuthorize` comes in.

---

## The Setup — Two Users in the Database

```
┌──────────────────────────────────────────────────────────────────────┐
│                         DATABASE STATE                                │
└──────────────────────────────────────────────────────────────────────┘

  TABLE: user_login
  ┌────┬──────────┬──────────────┬───────────┐
  │ id │ username │ password     │ role      │
  ├────┼──────────┼──────────────┼───────────┤
  │ 1  │ a_user   │ $2a$10$...   │ ROLE_USER │
  │ 2  │ b_user   │ $2a$10$...   │ ROLE_USER │
  └────┴──────────┴──────────────┴───────────┘

  Both users have permission: ORDER_READ

  So both users pass @PreAuthorize check:
  @PreAuthorize("hasRole('USER') and hasAuthority('ORDER_READ')")
  → a_user: ✅ passes
  → b_user: ✅ passes

  But the ORDER in the system belongs to a_user (userID = 1)
  → a_user should get the data   ✅
  → b_user should be blocked     ❌
```

---

## The `OrderDTO`

The instructor creates a simple DTO to represent an order:

```java
public class OrderDTO {
    public Long userID;   // which user this order belongs to
    public Long orderID;  // the order identifier
}
```

---

## The Controller with Both Annotations

```java
@RestController
@RequestMapping("/api")
public class OrderController {

    @GetMapping("/orders")
    @PreAuthorize("hasRole('USER') and hasAuthority('ORDER_READ')")
    @PostAuthorize("returnObject.userID == authentication.principal.id")
    public OrderDTO readOrders() {

        OrderDTO orderDTO = new OrderDTO();

        /*
         * For testing purpose, hardcoding the userID of a_user
         * In real app, this would be fetched from DB
         */
        orderDTO.userID = 1L;   // this order belongs to a_user (id=1)
        orderDTO.orderID = 100001L;

        return orderDTO;
    }
}
```

---

## The Key — `returnObject`

Inside `@PostAuthorize`, Spring gives you a special variable called `returnObject`. This is automatically set to whatever your method returned — in this case, the `OrderDTO` object.

```
┌──────────────────────────────────────────────────────────────────────┐
│                     returnObject in @PostAuthorize                    │
├──────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  Your method returns: OrderDTO { userID=1, orderID=100001 }          │
│                                                                       │
│  Spring automatically sets:                                           │
│  returnObject = OrderDTO { userID=1, orderID=100001 }                │
│                                                                       │
│  You can access any field:                                            │
│  returnObject.userID   → 1                                            │
│  returnObject.orderID  → 100001                                       │
│                                                                       │
│  No casting needed — Spring handles type casting internally           │
│                                                                       │
└──────────────────────────────────────────────────────────────────────┘
```

---

## The Full Flow — Step by Step

```
┌──────────────────────────────────────────────────────────────────────┐
│              FULL @PostAuthorize FLOW                                 │
└──────────────────────────────────────────────────────────────────────┘

  @PreAuthorize("hasRole('USER') and hasAuthority('ORDER_READ')")
  @PostAuthorize("returnObject.userID == authentication.principal.id")
  public OrderDTO readOrders() { ... }


  ─────────────────────────────────────────────────────────────────────
  SCENARIO 1: b_user (id=2) calls GET /api/orders
  ─────────────────────────────────────────────────────────────────────

  STEP 1: @PreAuthorize check
  ┌──────────────────────────────────────────────┐
  │  hasRole('USER')       → ROLE_USER ✅        │
  │  hasAuthority('ORDER_READ') → ORDER_READ ✅  │
  │  Result: PASS ✅                             │
  └──────────────────────────────────────────────┘
                    │
                    ▼ (method is allowed to run)

  STEP 2: Controller method executes
  ┌──────────────────────────────────────────────┐
  │  OrderDTO orderDTO = new OrderDTO()          │
  │  orderDTO.userID  = 1L  ← hardcoded         │
  │  orderDTO.orderID = 100001L                  │
  │  return orderDTO                             │
  └──────────────────────────────────────────────┘
                    │
                    ▼ (method returned OrderDTO)

  STEP 3: @PostAuthorize check
  ┌──────────────────────────────────────────────┐
  │  returnObject.userID           = 1           │
  │  authentication.principal.id   = 2 (b_user)  │
  │                                              │
  │  1 == 2 → false ❌                           │
  │  Result: FAIL ❌                             │
  └──────────────────────────────────────────────┘
                    │
                    ▼
  403 Forbidden — response is BLOCKED
  b_user does NOT get the order data


  ─────────────────────────────────────────────────────────────────────
  SCENARIO 2: a_user (id=1) calls GET /api/orders
  ─────────────────────────────────────────────────────────────────────

  STEP 1: @PreAuthorize check
  ┌──────────────────────────────────────────────┐
  │  hasRole('USER')       → ROLE_USER ✅        │
  │  hasAuthority('ORDER_READ') → ORDER_READ ✅  │
  │  Result: PASS ✅                             │
  └──────────────────────────────────────────────┘
                    │
                    ▼ (method is allowed to run)

  STEP 2: Controller method executes
  ┌──────────────────────────────────────────────┐
  │  OrderDTO orderDTO = new OrderDTO()          │
  │  orderDTO.userID  = 1L  ← hardcoded         │
  │  orderDTO.orderID = 100001L                  │
  │  return orderDTO                             │
  └──────────────────────────────────────────────┘
                    │
                    ▼ (method returned OrderDTO)

  STEP 3: @PostAuthorize check
  ┌──────────────────────────────────────────────┐
  │  returnObject.userID           = 1           │
  │  authentication.principal.id   = 1 (a_user)  │
  │                                              │
  │  1 == 1 → true ✅                            │
  │  Result: PASS ✅                             │
  └──────────────────────────────────────────────┘
                    │
                    ▼
  200 OK — response is SENT
  a_user gets: { userID: 1, orderID: 100001 }
```

---

## Important Warning About `@PostAuthorize`

The instructor points this out clearly and it is something you must always keep in mind:

```
┌──────────────────────────────────────────────────────────────────────┐
│                  ⚠️  CRITICAL DIFFERENCE                              │
├──────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  @PreAuthorize                                                        │
│  → Authorization fails BEFORE method runs                            │
│  → No DB call, no business logic executed                            │
│  → More efficient for simple role/permission checks                  │
│                                                                       │
│  @PostAuthorize                                                       │
│  → Authorization fails AFTER method runs                             │
│  → DB was already queried, business logic already executed           │
│  → Response is blocked but the work was already done                 │
│                                                                       │
│  So use @PostAuthorize ONLY when you genuinely need                   │
│  to inspect the returned data to make the auth decision              │
│  Don't use it as a replacement for @PreAuthorize                     │
│                                                                       │
└──────────────────────────────────────────────────────────────────────┘
```

---

## `@PreAuthorize` vs `@PostAuthorize` — Side by Side

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    COMPLETE COMPARISON                                   │
├────────────────────────┬────────────────────────────────────────────────┤
│    @PreAuthorize       │    @PostAuthorize                               │
├────────────────────────┼────────────────────────────────────────────────┤
│ Check happens BEFORE   │ Check happens AFTER                             │
│ method runs            │ method runs                                     │
├────────────────────────┼────────────────────────────────────────────────┤
│ Method does NOT run    │ Method RUNS regardless,                         │
│ if check fails         │ response blocked if check fails                 │
├────────────────────────┼────────────────────────────────────────────────┤
│ Use when decision can  │ Use when decision depends                       │
│ be made from user's    │ on data returned by the method                  │
│ role/permission alone  │                                                 │
├────────────────────────┼────────────────────────────────────────────────┤
│ No access to returned  │ Has access to returnObject                      │
│ data                   │ (the method's return value)                     │
├────────────────────────┼────────────────────────────────────────────────┤
│ Intercepted by:        │ Intercepted by:                                 │
│ Authorization          │ Authorization                                   │
│ ManagerBefore          │ ManagerAfter                                    │
│ MethodInterceptor      │ MethodInterceptor                               │
├────────────────────────┼────────────────────────────────────────────────┤
│ More efficient         │ Less efficient                                  │
│ (saves DB calls)       │ (DB already queried on failure)                 │
├────────────────────────┼────────────────────────────────────────────────┤
│ Most commonly used     │ Used for data-level ownership checks            │
└────────────────────────┴────────────────────────────────────────────────┘
```

---

## Interview Tip 🎯

> *"When would you use `@PostAuthorize` over `@PreAuthorize`?"*

**Perfect answer:**

`@PostAuthorize` is used when the authorization decision depends on the **data returned by the method**, not just the user's role or permission. For example, if a user is allowed to call a `GET /orders` API but should only receive orders that belong to them — you can't make that check before running the method because the order data isn't available yet. `@PostAuthorize` lets the method run, then checks the returned object using `returnObject` before sending the response. However, be aware that unlike `@PreAuthorize`, the business logic and DB queries have already executed by the time `@PostAuthorize` blocks the response — so use it only when truly necessary.

---

## Key Takeaways from Step 7

- `@PostAuthorize` runs authorization **after** the method executes but **before** the response reaches the user
- `returnObject` is a special SpEL variable that holds the method's return value — no manual casting needed
- `authentication.principal.id` gives you the currently logged-in user's ID inside the SpEL expression
- Both `@PreAuthorize` and `@PostAuthorize` can be used **together** on the same method
- `@PostAuthorize` is best for **data ownership checks** — ensuring users only see data that belongs to them
- Business logic still runs even if `@PostAuthorize` fails — this is a key difference from `@PreAuthorize`

---
# Step 8 — Full Summary & Quick Reference

---

## The Complete Picture — Everything in One Diagram

```
┌──────────────────────────────────────────────────────────────────────────────┐
│         SPRING BOOT METHOD SECURITY — COMPLETE FLOW                          │
└──────────────────────────────────────────────────────────────────────────────┘

  HTTP Request (with credentials)
         │
         ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        FILTER CHAIN                                      │
│                                                                          │
│  BasicAuthFilter / JWTFilter / OAuthFilter                               │
│  → Decodes credentials                                                   │
│  → Calls UserDetailsService.loadUserByUsername()                         │
│  → UserLoginEntity.getAuthorities() is called                            │
│  → Authentication Object is built:                                       │
│                                                                          │
│    Authentication {                                                      │
│      principal: UserLoginEntity {                                        │
│        id: 1                                                             │
│        username: "john"                                                  │
│      }                                                                   │
│      grantedAuthorities: [ROLE_USER, ORDER_READ, SALES_CREATE]           │
│      isAuthenticated: true                                               │
│    }                                                                     │
│                                                                          │
│  → Stored in SecurityContextHolder                                       │
└────────────────────────────┬────────────────────────────────────────────┘
                             │
                             │ ❌ Not authenticated → 401 Unauthorized
                             │ ✅ Authenticated → continue
                             ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      DISPATCHER SERVLET                                  │
└────────────────────────────┬────────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────────┐
│              AuthorizationManagerBeforeMethodInterceptor                 │
│                                                                          │
│  Triggered by @PreAuthorize annotation                                   │
│                                                                          │
│  1. Reads SpEL string from @PreAuthorize                                 │
│  2. Gets Authentication from SecurityContextHolder                       │
│  3. Parses SpEL string → AST via SpelExpressionParser                   │
│  4. Resolves AST recursively                                             │
│     → calls hasRole(), hasAuthority() etc.                               │
│     → checks against grantedAuthorities list                             │
│  5. Validates final result                                               │
└────────────────────────────┬────────────────────────────────────────────┘
                             │
                             │ ❌ Check fails → 403 Forbidden
                             │ ✅ Check passes → continue
                             ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        CONTROLLER METHOD                                 │
│                                                                          │
│  @GetMapping("/orders")                                                  │
│  @PreAuthorize("hasRole('USER') and hasAuthority('ORDER_READ')")         │
│  @PostAuthorize("returnObject.userID == authentication.principal.id")    │
│  public OrderDTO readOrders() {                                          │
│      // business logic runs                                              │
│      // DB is queried                                                    │
│      return orderDTO;   ← returns data                                   │
│  }                                                                       │
└────────────────────────────┬────────────────────────────────────────────┘
                             │
                             │ method returns OrderDTO
                             ▼
┌─────────────────────────────────────────────────────────────────────────┐
│               AuthorizationManagerAfterMethodInterceptor                 │
│                                                                          │
│  Triggered by @PostAuthorize annotation                                  │
│                                                                          │
│  1. Reads SpEL string from @PostAuthorize                                │
│  2. Gets Authentication from SecurityContextHolder                       │
│  3. Sets returnObject = the returned OrderDTO                            │
│  4. Evaluates: returnObject.userID == authentication.principal.id        │
│  5. Validates final result                                               │
└────────────────────────────┬────────────────────────────────────────────┘
                             │
                             │ ❌ Check fails → 403 Forbidden
                             │ ✅ Check passes → continue
                             ▼
                      HTTP Response
                      sent to user
```

---

## All The Code — In One Place

### 1. `UserPermissionEntity.java`
```java
@Entity
@Table(name = "user_permission")
public class UserPermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // e.g., ORDER_READ, SALES_READ, SALES_CREATE
    private String name;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
```

---

### 2. `UserLoginEntity.java`
```java
@Entity
@Table(name = "user_login")
public class UserLoginEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String role; // e.g., "ROLE_USER" or "ROLE_ADMIN"

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<UserPermissionEntity> permissions = new ArrayList<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // Add role (high level)
        authorities.add(new SimpleGrantedAuthority(role));

        // Add all permissions (granular level)
        permissions.forEach(permission ->
            authorities.add(new SimpleGrantedAuthority(permission.getName()))
        );

        return authorities;
    }

    @Override
    public String getPassword() { return password; }

    @Override
    public String getUsername() { return username; }
}
```

---

### 3. `UserLoginEntityService.java`
```java
@Service
public class UserLoginEntityService implements UserDetailsService {

    @Autowired
    private UserLoginEntityRepository userLoginEntityRepository;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        return userLoginEntityRepository.findByUsername(username)
                .orElseThrow(() ->
                    new UsernameNotFoundException("user not found"));
    }

    public UserDetails save(UserLoginEntity userLoginEntity) {
        return userLoginEntityRepository.save(userLoginEntity);
    }
}
```

---

### 4. `UserLoginEntityRepository.java`
```java
@Repository
public interface UserLoginEntityRepository
        extends JpaRepository<UserLoginEntity, Long> {

    Optional<UserLoginEntity> findByUsername(String username);
}
```

---

### 5. `UserLoginController.java`
```java
@RestController
public class UserLoginController {

    @Autowired
    UserLoginEntityService userLoginEntityService;

    @Autowired
    PasswordEncoder passwordEncoder;

    @PostMapping("/user-login")
    public ResponseEntity<String> login(
            @RequestBody UserLoginEntity userLoginEntity) {

        userLoginEntity.setPassword(
            passwordEncoder.encode(userLoginEntity.getPassword())
        );

        userLoginEntityService.save(userLoginEntity);
        return ResponseEntity.ok("User registered successfully!");
    }
}
```

---

### 6. `SecurityConfig.java`
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // ← CRITICAL
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/user-login").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .csrf(csrf -> csrf.disable())
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
```

---

### 7. `OrderController.java`
```java
@RestController
@RequestMapping("/api")
public class OrderController {

    // Using both @PreAuthorize and @PostAuthorize together
    @GetMapping("/orders")
    @PreAuthorize("hasRole('USER') and hasAuthority('ORDER_READ')")
    @PostAuthorize("returnObject.userID == authentication.principal.id")
    public OrderDTO readOrders() {
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.userID = 1L;       // in real app: fetch from DB
        orderDTO.orderID = 100001L;
        return orderDTO;
    }
}
```

---

### 8. `SalesController.java`
```java
@RestController
@RequestMapping("/api")
public class SalesController {

    @GetMapping("/sales")
    @PreAuthorize("hasAuthority('SALES_READ')")
    public ResponseEntity<String> readSalesDetails() {
        return ResponseEntity.ok("ALL Sales details has been fetched successfully");
    }
}
```

---

### 9. `OrderDTO.java`
```java
public class OrderDTO {
    public Long userID;
    public Long orderID;
}
```

---

## Quick Reference — Everything at a Glance

### Annotations
```
┌─────────────────────┬───────────────────────────────────────────────────┐
│ Annotation          │ When authorization check happens                  │
├─────────────────────┼───────────────────────────────────────────────────┤
│ @PreAuthorize       │ BEFORE method runs                                │
│ @PostAuthorize      │ AFTER method runs, before response sent           │
└─────────────────────┴───────────────────────────────────────────────────┘
```

### SpEL Methods
```
┌──────────────────────────────┬──────────────────────────────────────────┐
│ SpEL Expression              │ What it does                             │
├──────────────────────────────┼──────────────────────────────────────────┤
│ hasRole('USER')              │ checks for ROLE_USER in authorities      │
│ hasAuthority('ORDER_READ')   │ checks for ORDER_READ in authorities     │
│ returnObject.fieldName       │ accesses returned object (PostAuthorize) │
│ authentication.principal.id  │ currently logged-in user's id            │
│ #paramName                   │ method parameter value                   │
└──────────────────────────────┴──────────────────────────────────────────┘
```

### Logical Operators
```
┌───────────┬────────────────────────────────────────────────────────────┐
│ Operator  │ Example                                                    │
├───────────┼────────────────────────────────────────────────────────────┤
│ and       │ hasRole('USER') and hasAuthority('ORDER_READ')             │
│ or        │ hasRole('ADMIN') or hasRole('USER')                        │
│ not / !   │ not hasRole('ADMIN') / !hasAuthority('DELETE')             │
└───────────┴────────────────────────────────────────────────────────────┘
```

### Relational Operators
```
┌───────────┬────────────────────────────────────────────────────────────┐
│ Operator  │ Example                                                    │
├───────────┼────────────────────────────────────────────────────────────┤
│ ==        │ #id == authentication.principal.id                         │
│ !=        │ #value != 15                                               │
│ <  >      │ #value < 100  /  #value > 100                              │
│ <= >=     │ #value <= 15  /  #value >= 90                              │
└───────────┴────────────────────────────────────────────────────────────┘
```

---

## Common Mistakes to Avoid

```
┌──────────────────────────────────────────────────────────────────────┐
│                      COMMON MISTAKES                                 │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ❌ Forgetting @EnableMethodSecurity(prePostEnabled = true)           │
│     → Annotations silently ignored, no error thrown                  │
│                                                                      │
│  ❌ Using hasRole('ROLE_USER') instead of hasRole('USER')             │
│     → hasRole() auto-adds ROLE_ prefix                               │
│     → You'd end up checking for ROLE_ROLE_USER which doesn't exist   │
│                                                                      │
│  ❌ Using FetchType.LAZY on permissions                               │
│     → Spring Security loads user during filter chain                 │
│     → Session may be closed by the time permissions are needed       │
│     → Always use FetchType.EAGER for permissions                     │
│                                                                      │
│  ❌ Using @PostAuthorize when @PreAuthorize would work                │
│     → DB query runs unnecessarily even when access should be denied  │
│     → Use @PostAuthorize only when you need returnObject             │
│                                                                      │
│  ❌ Not storing ROLE_ prefix in DB for role field                     │
│     → In getAuthorities() you do:                                    │
│        new SimpleGrantedAuthority(role)                              │
│     → role in DB must be "ROLE_USER" not just "USER"                 │
│     → Otherwise hasRole('USER') check will fail                      │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Interview Tips 🎯 — All in One Place

**Q1: What is the difference between authorization at Security Filter Layer vs annotation level?**

Security Filter Layer puts all rules in one central `SecurityConfig` file — hard to manage at scale with 100s of APIs. Annotation-based authorization places the rule right on the controller method using `@PreAuthorize` or `@PostAuthorize` — more readable, maintainable, and scalable.

**Q2: What is the difference between `hasRole()` and `hasAuthority()`?**

Both call the same internal method `hasAnyAuthorityName()` in `SecurityExpressionRoot.java`. The only difference is `hasRole()` automatically prepends `ROLE_` to the value. By convention, `hasRole()` is for high-level distinctions (USER, ADMIN) and `hasAuthority()` is for granular permissions (ORDER_READ, SALES_DELETE).

**Q3: When would you use `@PostAuthorize` over `@PreAuthorize`?**

When the authorization decision depends on the data returned by the method — for example, ensuring a user only receives data that belongs to them. `@PostAuthorize` gives access to `returnObject` which holds the method's return value. However, the business logic still runs even if the check fails — so use it only when truly necessary.

**Q4: How does `@PreAuthorize` get triggered before the controller method runs?**

It is intercepted by `AuthorizationManagerBeforeMethodInterceptor` — a built-in Spring Security interceptor. It reads the SpEL expression from the annotation, parses it into an Abstract Syntax Tree using `SpelExpressionParser`, resolves it recursively against the `Authentication` object from `SecurityContextHolder`, and either allows or denies method execution based on the result.

**Q5: What happens if you forget `@EnableMethodSecurity(prePostEnabled = true)`?**

All `@PreAuthorize` and `@PostAuthorize` annotations are completely ignored — no error, no warning. Every user gets access to every endpoint regardless of their role or permissions.

---

## That's the Complete Lecture!

Here is everything this lecture covered, in order:

```
Step 1 → Problem with Security Filter Layer approach at scale
Step 2 → @PreAuthorize vs @PostAuthorize — concept & timing
Step 3 → User setup: Entity, Permissions, getAuthorities()
Step 4 → Enabling method security + SecurityConfig
Step 5 → @PreAuthorize in action + hasRole vs hasAuthority deep dive
Step 6 → Under the hood: Interceptors + SpEL parsing
Step 7 → @PostAuthorize in action + returnObject
Step 8 → Full summary + quick reference + interview tips
```