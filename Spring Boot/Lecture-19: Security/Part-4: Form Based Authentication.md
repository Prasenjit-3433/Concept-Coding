# Step 1 — What is Form-Based Authentication & Why Does It Exist?

---

## The Problem It Solves

Imagine you're logged into your bank's website. You click on "View Balance", then "Transfer Money", then "Transaction History" — three different API calls to the server.

Now think about this — **HTTP is a stateless protocol.** That means the server remembers *nothing* about you between requests. Every single request looks brand new to the server.

So the naive solution would be: **send your username & password with every single request.**

That's terrible. Why?
- It's a massive security risk — your credentials travel over the network repeatedly
- It's a horrible user experience — imagine typing your password every time you click something

**Form-Based Authentication solves exactly this problem.**

---

## The Core Idea — "I'll Remember You"

The server says: *"You don't need to prove who you are every single time. Just prove it once. I'll remember you using a Session."*

Here's the deal in plain English:

```
First Request  → You send username + password → Server trusts you → Server creates a Session
All Future Requests → You just show your Session ID → Server recognizes you → Done
```

This is what the instructor means when he says:

> *"Stateful authentication means your server is maintaining a state of a client — also known as Session."*

---

## What Exactly is "Stateful"?

The word **Stateful** simply means — **the server maintains some information (state) about you across multiple requests.**

That "state" here = your authentication state = whether you're logged in or not.

```
┌─────────────────────────────────────────────────────────────┐
│                    STATEFUL vs STATELESS                    │
├──────────────────────────┬──────────────────────────────────┤
│      STATEFUL            │         STATELESS                │
│  (Form-Based Auth)       │       (JWT - coming later)       │
├──────────────────────────┼──────────────────────────────────┤
│ Server remembers you     │ Server remembers nothing         │
│ via Session stored       │ You carry your own proof         │
│ on the server side       │ (token) with every request       │
│                          │                                  │
│ Client only sends        │ Client sends the full token      │
│ a small Session ID       │ which server just verifies       │
└──────────────────────────┴──────────────────────────────────┘
```

---

## Why Learn This Before JWT?

The instructor makes a very important point here —

> *"To understand JWT and OAuth, we need to first understand what was previously popular, what were the disadvantages, how it was working — only then will JWT give us a better sense of what advantage it is adding."*

So Form-Based Auth is your **foundation**. Every problem you'll see with Form-Based Auth directly explains *why JWT was invented.*

---

## Quick Summary — Key Facts to Remember

```
┌─────────────────────────────────────────────────────────────┐
│              FORM-BASED AUTHENTICATION — AT A GLANCE        │
├─────────────────────────────────────────────────────────────┤
│  Type          →  Stateful (server maintains session)       │
│  Who stores it →  Server (in-memory or DB)                  │
│  What client   →  Only Session ID (not password again)      │
│  sends         →                                            │
│  Default in    →  YES — it's Spring Boot Security's         │
│  Spring Boot?  →  default authentication method             │
│  Default URLs  →  Login  → /login                           │
│                →  Logout → /logout                          │
└─────────────────────────────────────────────────────────────┘
```

---

## 🎯 Interview Tip from the Instructor

> **Q: What is the default authentication method in Spring Boot Security — is it JWT?**
>
> **A: No.** Form-Based Authentication is the default. If you want JWT, you have to *explicitly* configure it. Spring Boot won't use JWT unless you tell it to.

This is a very commonly asked question in interviews and people often get it wrong assuming JWT is the default.

---
# Step 2 — How Sessions Work Internally

---

## What Exactly is an HTTP Session?

When you successfully log in, the server doesn't just say "okay, you're in" and forget about it. It creates a **concrete Java object** called `HttpSession` and stores it somewhere.

Think of it like a **locker system** at a gym:
- You show your ID once at the front desk (login)
- They give you a locker key (Session ID)
- Every time you come back, you just show the key
- The locker (Session) holds all your info on the server side

---

## What Does the HttpSession Object Contain?

```
┌─────────────────────────────────────────────────────────────┐
│                    HttpSession Object                       │
├─────────────────────────────────────────────────────────────┤
│  Session ID        →  Unique ID for this session            │
│  Creation Time     →  When was this session created         │
│  Last Access Time  →  When did the user last make a request │
│  Max Inactive      →  How long before session expires       │
│  Interval          →  (default: 30 minutes)                 │
│  Expiry Time       →  last_access_time + max_inactive       │
│  Principal Name    →  The username (who owns this session)  │
│  Security Context  →  Authentication data (roles, etc.)     │
└─────────────────────────────────────────────────────────────┘
```

The instructor showed this live from the database — this is exactly what Spring Boot stores in the `SPRING_SESSION` table when you configure DB storage.

---

## Where is the Session Stored?

By default, the session is stored **in-memory** — inside the servlet container (e.g., Tomcat's RAM).

But this creates a **big problem in distributed systems.** The instructor explains it very clearly:

```
┌─────────────────────────────────────────────────────────────────────┐
│              THE DISTRIBUTED SYSTEM PROBLEM                         │
│                                                                     │
│   ┌────────┐         ┌─────────────┐                                │
│   │        │ ──────► │  Server 1   │  ← Session stored HERE         │
│   │ Client │         └─────────────┘    (in memory)                 │
│   │        │                                                        │
│   │        │         ┌─────────────┐                                │
│   │        │ ──────► │  Server 2   │  ← Session NOT here!           │
│   └────────┘         └─────────────┘    "Please login again" 😤     │
│                                                                     │
│   Load balancer can send requests to ANY server.                    │
│   If session is only in Server 1's memory,                          │
│   Server 2 has no idea who you are → Bad user experience!           │
└─────────────────────────────────────────────────────────────────────┘


┌─────────────────────────────────────────────────────────────────────┐
│              THE SOLUTION — STORE SESSION IN DB                     │
│                                                                     │
│   ┌────────┐         ┌─────────────┐                                │
│   │        │ ──────► │  Server 1   │ ──┐                            │
│   │ Client │         └─────────────┘   │   ┌─────────────────┐      │
│   │        │                           ├──►│    Database      │     │
│   │        │         ┌─────────────┐   │   │  (SPRING_SESSION │     │
│   │        │ ──────► │  Server 2   │ ──┘   │     table)       │     │
│   └────────┘         └─────────────┘       └─────────────────┘      │
│                                                                     │
│   Now BOTH servers query the same DB → Session found → ✅            │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Session Timeout — How Does Expiry Work?

The instructor makes a very important distinction here that most people get confused about:

> *"It requires ONE MINUTE of TOTAL INACTIVITY. Not just one minute from creation."*

```
┌─────────────────────────────────────────────────────────────────────┐
│                    SESSION EXPIRY LOGIC                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  Default timeout = 30 minutes (Tomcat servlet container)            │
│                                                                     │
│  WRONG understanding:                                               │
│  Session created at 12:00 → expires at 12:30 no matter what ❌       │
│                                                                     │
│  CORRECT understanding:                                             │
│  Session created at 12:00                                           │
│  User makes request at 12:25 → expiry RESETS to 12:55               │
│  User makes request at 12:50 → expiry RESETS to 13:20               │
│  No request for 30 mins → THEN session expires ✅                    │
│                                                                     │
│  Formula:                                                           │
│  Expiry Time = Last Access Time + Max Inactive Interval             │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

Internally, every time a request comes in, Spring Boot updates the `LAST_ACCESS_TIME` column in the session table, and recalculates `EXPIRY_TIME`.

---

## How to Configure Session Timeout

You can change the default 30-minute timeout in `application.properties`:

```properties
# Change session timeout to 1 minute
server.servlet.session.timeout=1m

# Or 5 minutes
server.servlet.session.timeout=5m
```

---

## How to Store Session in Database

**Step 1 — Add the dependency in `pom.xml`:**

```xml
<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-jdbc</artifactId>
</dependency>
```

**Step 2 — Add config in `application.properties`:**

```properties
# Tell Spring to use JDBC (DB) for session storage
spring.session.store-type=jdbc

# Let Spring Boot auto-create the session tables for you
spring.session.jdbc.initialize-schema=always

# Set session timeout
server.servlet.session.timeout=5m
```

**That's it.** Spring Boot will automatically create and manage two tables for you:

```
┌─────────────────────────────────────────────────────────────────────┐
│               Tables Spring Boot Creates Automatically              │
├──────────────────────────┬──────────────────────────────────────────┤
│   SPRING_SESSION         │  Stores core session info                │
│                          │  (session_id, creation_time,             │
│                          │   last_access_time,                      │
│                          │   max_inactive_interval,                 │
│                          │   expiry_time, principal_name)           │
├──────────────────────────┼──────────────────────────────────────────┤
│   SPRING_SESSION         │  Stores Security Context data            │
│   _ATTRIBUTES            │  (the serialized authentication object   │
│                          │   — roles, username, etc.)               │
└──────────────────────────┴──────────────────────────────────────────┘
```

---

## Full Picture — Session Lifecycle

```
┌─────────────────────────────────────────────────────────────────────┐
│                     SESSION LIFECYCLE                               │
│                                                                     │
│  1. User logs in with username + password                           │
│            │                                                        │
│            ▼                                                        │
│  2. Server creates HttpSession object                               │
│     (Session ID generated, stored in DB or memory)                  │
│            │                                                        │
│            ▼                                                        │
│  3. Session ID sent back to client via Cookie                       │
│     Set-Cookie: JSESSIONID=abc123                                   │
│            │                                                        │
│            ▼                                                        │
│  4. Client sends JSESSIONID with every subsequent request           │
│     Cookie: JSESSIONID=abc123                                       │
│            │                                                        │
│            ▼                                                        │
│  5. Server looks up HttpSession using JSESSIONID                    │
│     ├── Found & Valid → Fulfill the request ✅                       │
│     └── Not Found / Expired → Redirect to /login ❌                  │
│            │                                                        │
│  6. On every request, LAST_ACCESS_TIME updates                      │
│     Expiry time recalculates automatically                          │
│            │                                                        │
│  7. After configured inactivity → Session expires                   │
│     User must login again                                           │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🎯 Interview Tip from the Instructor

> **Q: What is the default session timeout in Spring Boot?**
>
> **A:** 30 minutes — but it depends on the servlet container being used (e.g., Tomcat). It can be configured via `server.servlet.session.timeout` in `application.properties`.

> **Q: Why would you store sessions in a database instead of in-memory?**
>
> **A:** In a distributed system with multiple server instances behind a load balancer, storing sessions in memory means only the server that created the session knows about it. Other servers won't recognize the session, forcing the user to log in again. Storing in DB ensures all servers can look up the same session.

---
# Step 3 — End-to-End Flow: Login Request (First Time)

---

## Setting the Scene

Before any of this works, **a user must already exist** in the system. The instructor is very clear about this:

> *"Assume that we have already present user. We have username password already present. User exist."*

So the starting point is always — user is created, stored in DB (or in-memory), and now they're trying to log in **for the very first time.** No session exists yet.

The client hits the default login URL: **`/login`**

Spring Boot automatically provides this login page for form-based authentication. You don't write a single line of HTML for it.

---

## The Complete Internal Flow — Login Request

Let's walk through this **exactly** the way the instructor explained it, filter by filter:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    LOGIN REQUEST FLOW (/login)                              │
│                                                                             │
│  ┌──────────┐   username+password+CSRF token     ┌───────────────────────┐  │
│  │  Client  │ ─────────────────────────────────► │  Security Filter      │  │
│  │ (Browser)│                                    │  Chain                │  │
│  └──────────┘                                    └───────────────────────┘  │
│                                                            │                │
│                                                            ▼                │
│                                              ┌─────────────────────────┐    │
│                            FILTER 1          │  UsernamePassword       │    │
│                                              │  AuthenticationFilter   │    │
│                                              └─────────────────────────┘    │
│                                                            │                │
│                                              Creates Authentication Object  │
│                                              {username, password,           │
│                                               authenticated=FALSE}          │
│                                                            │                │
│                                                            ▼                │
│                                              ┌─────────────────────────┐    │
│                                              │  AuthenticationManager  │    │
│                                              │  (ProviderManager)      │    │
│                                              └─────────────────────────┘    │
│                                                            │                │
│                                              Delegates to...                │
│                                                            │                │
│                                                            ▼                │
│                                              ┌─────────────────────────┐    │
│                                              │  DaoAuthentication      │    │
│                                              │  Provider               │    │
│                                              └─────────────────────────┘    │
│                                                    │       │                │
│                                         Step A     │       │  Step B        │
│                                                    ▼       ▼                │
│                                         ┌──────────────┐  ┌──────────────┐  │
│                                         │ Password     │  │UserDetails   │  │
│                                         │ Encoder      │  │Service       │  │
│                                         │(hash the     │  │(fetch user   │  │
│                                         │ incoming     │  │ from DB /    │  │
│                                         │ password)    │  │ in-memory)   │  │
│                                         └──────────────┘  └──────────────┘  │
│                                                    │                        │
│                                         Step C: Compare hashed incoming     │
│                                         password WITH stored hashed password│
│                                                    │                        │
│                                         ┌──────────┴──────────┐             │
│                                         │  Match?             │             │
│                                    YES  │                     │  NO         │
│                                         ▼                     ▼             │
│                              Authentication Object      Authentication      │
│                              UPDATED:                   Fails ❌             │
│                              {username,                                     │
│                               authenticated=TRUE,                           │
│                               roles=[...]}                                  │
│                                         │                                   │
│                                         ▼                                   │
│                            FILTER 2  ┌─────────────────────────┐            │
│                                      │SecurityContextHolder    │            │
│                                      │Filter                   │            │
│                                      └─────────────────────────┘            │
│                                                  │                          │
│                              Creates SecurityContext object                 │
│                              Puts Authentication object inside it           │
│                                                  │                          │
│                                                  ▼                          │
│                                      ┌─────────────────────────┐            │
│                                      │HttpSessionSecurity      │            │
│                                      │ContextRepository        │            │
│                                      └─────────────────────────┘            │
│                                                  │                          │
│                              Creates NEW HttpSession object                 │
│                              Stores SecurityContext inside it               │
│                              Saves to Memory or DB                          │
│                                                  │                          │
│                                                  ▼                          │
│                              Response sent back to Client                   │
│                              Set-Cookie: JSESSIONID=abc123 🍪               │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Breaking Down Each Step in Detail

### 🔷 Step 1 — UsernamePasswordAuthenticationFilter

This is the **first filter** that gets invoked when `/login` is hit.

What it does:
- Reads the `username`, `password`, and `CSRF token` from the request
- Creates an **Authentication Object** — specifically a `UsernamePasswordAuthenticationToken`

The Authentication Object at this point looks like:

```
Authentication Object (BEFORE verification)
┌────────────────────────────────────────┐
│  principal    =  "username"            │
│  credentials  =  "raw_password"        │
│  authenticated = FALSE                 │
│  authorities  =  [] (empty)            │
└────────────────────────────────────────┘
```

It is **not yet authenticated** — this object is just a carrier of what the user *claimed* about themselves.

This object is then passed to the **AuthenticationManager.**

---

### 🔷 Step 2 — AuthenticationManager → DaoAuthenticationProvider

The `AuthenticationManager` (implemented by `ProviderManager`) **delegates** the authentication request to the right provider — in this case, **`DaoAuthenticationProvider`**.

Inside `DaoAuthenticationProvider`, three things happen:

**Step A — Hash the incoming password:**
```
Raw password from request → PasswordEncoder → Hashed password
```

**Step B — Fetch the stored user details:**
```
Username → UserDetailsService.loadUserByUsername() → 
Fetches from DB or in-memory → Returns {username, hashed_password, roles}
```

**Step C — Compare:**
```
Hashed incoming password  vs  Stored hashed password
        │                              │
        └──────────── Match? ──────────┘
              YES → proceed
              NO  → throw AuthenticationException ❌
```

If matched, the Authentication Object gets **fully updated:**

```
Authentication Object (AFTER verification)
┌────────────────────────────────────────┐
│  principal    =  "username"            │
│  credentials  =  protected (cleared)   │
│  authenticated = TRUE ✅                │
│  authorities  =  ["ROLE_USER"]         │
│  accountNonExpired     = true          │
│  accountNonLocked      = true          │
│  credentialsNonExpired = true          │
└────────────────────────────────────────┘
```

> **Important note from instructor:** *"Till now, no session is created. Only what happened is we have an authentication object which has all this information."*

The session hasn't been created yet at this point. That happens in the next filter.

---

### 🔷 Step 3 — SecurityContextHolderFilter

This filter:
1. Creates a **SecurityContext** object
2. Puts the fully authenticated **Authentication Object** inside it

```
┌─────────────────────────────────────┐
│          SecurityContext            │
│  ┌───────────────────────────────┐  │
│  │    Authentication Object      │  │
│  │  authenticated = TRUE         │  │
│  │  username = "user"            │  │
│  │  roles = ["ROLE_USER"]        │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
```

Then passes this `SecurityContext` to...

---

### 🔷 Step 4 — HttpSessionSecurityContextRepository

This is where the **Session finally gets created.**

It:
1. Creates a brand new `HttpSession` object
2. Stores the `SecurityContext` (with all authentication data) **inside** the `HttpSession`
3. Saves the `HttpSession` — either in-memory or in DB

```
┌──────────────────────────────────────────────────────┐
│                   HttpSession Object                 │
├──────────────────────────────────────────────────────┤
│  session_id          =  "abc123"                     │
│  creation_time       =  1234567890                   │
│  last_access_time    =  1234567890                   │
│  max_inactive        =  300 (5 mins)                 │
│  expiry_time         =  1234568190                   │
│  principal_name      =  "user"                       │
│                                                      │
│  ┌────────────────────────────────────────────────┐  │
│  │  SecurityContext (serialized & stored here)    │  │
│  │  → Authentication → roles, username, etc.      │  │
│  └────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────┘
         │
         ▼
  Saved in DB (SPRING_SESSION table)
  OR in-memory (Tomcat RAM)
```

---

### 🔷 Step 5 — Response Back to Client

The session ID is sent back to the client inside a **Cookie**:

```
HTTP Response Headers:
Set-Cookie: JSESSIONID=abc123; Path=/; HttpOnly; SameSite=Lax
```

The browser **automatically stores** this cookie and will send it with every future request. The user never has to manage this manually.

---

### 🔷 Step 6 — What Happens After Login?

The instructor makes an important distinction here:

```
┌──────────────────────────────────────────────────────────────┐
│            WHAT GETS INVOKED AFTER LOGIN?                    │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  Scenario 1: User directly hit /login                        │
│  → After success, Spring calls the DEFAULT endpoint "/"      │
│  → Whatever controller you mapped to "/" gets invoked        │
│                                                              │
│  Scenario 2: User tried to hit /users but wasn't logged in   │
│  → Spring redirected them to /login automatically            │
│  → After successful login, Spring redirects them back        │
│    to the ORIGINAL endpoint /users they wanted               │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

---

## What Code Do You Need to Write for This Entire Flow?

**Practically nothing for the login flow itself.**

The instructor is very clear:

> *"We don't have to write a single line of code. Everything is handled by the framework itself — this filter chain, fetching the HTTP session, authorization filters — everything is already there."*

All you need is:

**`pom.xml` — Dependencies:**
```xml
<!-- Core Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- Only if you want to store session in DB -->
<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-jdbc</artifactId>
</dependency>
```

**`application.properties` — Configuration:**
```properties
# Create a test user (only for dev/testing — NOT for production)
spring.security.user.name=user
spring.security.user.password=pass

# Store session in DB (remove these 2 lines if you want in-memory)
spring.session.store-type=jdbc
spring.session.jdbc.initialize-schema=always

# Session timeout
server.servlet.session.timeout=5m
```

Spring Boot's `SpringBootWebSecurityConfiguration` class automatically sets up form login — here's what it looks like internally (this is **framework code**, not yours):

```java
// This is Spring Boot's internal default — you don't write this
@Bean
@Order(SecurityProperties.BASIC_AUTH_ORDER)
SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http)
        throws Exception {
    http.authorizeHttpRequests((requests) ->
            requests.anyRequest().authenticated()
    );
    http.formLogin(withDefaults());   // ← form login set as default
    http.httpBasic(withDefaults());

    return http.build();
}
```

This is exactly why form login works out of the box — Spring already wired it for you.

---

## 🎯 Interview Tips from the Instructor

> **Q: When exactly is the HTTP Session created during form-based login?**
>
> **A:** NOT immediately when credentials are validated. First, the `UsernamePasswordAuthenticationFilter` creates the Authentication Object and passes it through `AuthenticationManager` → `DaoAuthenticationProvider` for validation. Only **after** successful validation, the `SecurityContextHolderFilter` creates the `SecurityContext`, and then `HttpSessionSecurityContextRepository` creates and stores the actual `HttpSession`. The Session ID is then returned to the client via a cookie.

> **Q: What does the HttpSession store internally?**
>
> **A:** It stores the `SecurityContext` object (serialized), which contains the `Authentication` object — holding the username, roles, and authentication status. It also stores metadata like session ID, creation time, last access time, max inactive interval, and expiry time.

> **Q: Where is the HttpSession stored by default?**
>
> **A:** In-memory inside the servlet container (e.g., Tomcat). For distributed systems with multiple server instances, it should be stored in a database using `spring-session-jdbc`.

---
# Step 4 — End-to-End Flow: Subsequent Requests (After Login)

---

## Setting the Scene

Login is done. Session is created. JSESSIONID cookie is sitting in the browser.

Now the user wants to actually **use the application** — they call `/users` or any other API endpoint. This time:
- They do **NOT** send username + password
- They **only** send the `JSESSIONID` cookie automatically
- A completely **different set of filters** gets invoked compared to the login flow

The instructor is very clear about this:

> *"Username Password Authentication Filter is now NOT involved. SecurityContextHolderFilter will get invoked."*

---

## The Complete Internal Flow — Subsequent Request

```
┌──────────────────────────────────────────────────────────────────────────────┐
│              SUBSEQUENT REQUEST FLOW (e.g. /users)                           │
│                                                                              │
│  ┌──────────┐   Cookie: JSESSIONID=abc123     ┌───────────────────────────┐  │
│  │  Client  │ ──────────────────────────────► │   Security Filter Chain   │  │
│  │ (Browser)│   (NO username/password!)       └───────────────────────────┘  │ 
│  └──────────┘                                             │                  │
│       ▲                                                   │                  │
│       │                                                   ▼                  │
│       │                                     ┌─────────────────────────┐      │
│       │                      FILTER 1       │ SecurityContextHolder   │      │
│       │                                     │ Filter                  │      │
│       │                                     └─────────────────────────┘      │
│       │                                                   │                  │
│       │                                    Passes request to...              │
│       │                                                   │                  │
│       │                                                   ▼                  │
│       │                                     ┌─────────────────────────┐      │
│       │                                     │ HttpSessionSecurity     │      │
│       │                                     │ ContextRepository       │      │
│       │                                     └─────────────────────────┘      │
│       │                                           │            │             │
│       │                                           │            │             │
│       │                              Looks up HttpSession      │             │
│       │                              using JSESSIONID          │             │
│       │                                           │            │             │
│       │                               ┌───────────┴──────────┐ │             │
│       │                               │                       ││             │
│       │                          NOT FOUND               FOUND││             │
│       │                          or EXPIRED               and ││             │
│       │                               │                  VALID││             │
│       │                               ▼                       ││             │
│       │◄──────────────── Redirect to /login ❌                 ││             │
│                                                               ▼▼             │
│                                                  Extracts SecurityContext    │
│                                                  from HttpSession            │
│                                                               │              │
│                                                               ▼              │
│                                                  Stores it in                │
│                                                  SecurityContextHolder       │
│                                                  (available throughout       │
│                                                   request lifecycle)         │
│                                                               │              │
│                                                               ▼              │
│                                              ┌──────────────────────────┐    │
│                             FILTER 2         │   Authorization Filter   │    │
│                                              └──────────────────────────┘    │
│                                                               │              │
│                                              Checks: Does this user have     │
│                                              permission to access /users?    │
│                                                               │              │
│                                               ┌──────────────┴───────────┐   │
│                                               │                          │   │
│                                          Role matches              Role missing│
│                                               │                          │   │
│                                               ▼                          ▼   │
│                                     ┌──────────────────┐    403 FORBIDDEN ❌  │
│                                     │ Dispatcher       │                     │
│                                     │ Servlet →        │                     │
│                                     │ Controller       │                     │
│                                     │ → Fulfill        │                     │
│                                     │   Request ✅      │                     │
│                                     └──────────────────┘                     │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## Breaking Down Each Step in Detail

### 🔷 Step 1 — SecurityContextHolderFilter

This is the **first filter** invoked for any subsequent request. Notice — `UsernamePasswordAuthenticationFilter` is completely skipped. That filter is only for the `/login` endpoint.

What `SecurityContextHolderFilter` does:
- Receives the incoming request which has `JSESSIONID` in the cookie
- Passes the request to `HttpSessionSecurityContextRepository`
- Gets back the `SecurityContext`
- Stores it in `SecurityContextHolder` — a placeholder that keeps authentication data available **throughout the entire lifecycle of this request**

The instructor explains why this matters:

> *"This SecurityContextHolder is available in the complete lifecycle of the request — till the point your controller gets invoked. This SecurityContext holder data is present."*

```
┌──────────────────────────────────────────────────────────────┐
│                   SecurityContextHolder                      │
│                                                              │
│   Think of it as a "request-scoped shelf" where              │
│   authentication data sits during the request                │
│                                                              │
│   ┌────────────────────────────────────────────────┐         │
│   │              SecurityContext                   │         │
│   │  ┌──────────────────────────────────────────┐  │         │
│   │  │         Authentication Object            │  │         │
│   │  │  username      = "user"                  │  │         │
│   │  │  authenticated = TRUE                    │  │         │
│   │  │  roles         = ["ROLE_USER"]           │  │         │
│   │  └──────────────────────────────────────────┘  │         │
│   └────────────────────────────────────────────────┘         │
│                                                              │
│   Available to: Filters → Dispatcher Servlet →               │
│                 Interceptors → Controller                    │
└──────────────────────────────────────────────────────────────┘
```

---

### 🔷 Step 2 — HttpSessionSecurityContextRepository

This is where the **actual session lookup** happens.

```
┌──────────────────────────────────────────────────────────────┐
│          HttpSessionSecurityContextRepository                │
│                    Session Lookup Logic                      │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  Incoming request has:                                       │
│  Cookie: JSESSIONID = abc123                                 │
│                    │                                         │
│                    ▼                                         │
│  Search for HttpSession with ID = abc123                     │
│  (in DB or in-memory, wherever it was stored)                │
│                    │                                         │
│         ┌──────────┴──────────┐                              │
│         │                     │                              │
│    FOUND & VALID         NOT FOUND / EXPIRED                 │
│         │                     │                              │
│         ▼                     ▼                              │ 
│  Extract SecurityContext  Redirect to /login                 │
│  from HttpSession         User must authenticate again       │
│         │                                                    │
│         ▼                                                    │
│  Return SecurityContext                                      │
│  back to                                                     │
│  SecurityContextHolderFilter                                 │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

---

### 🔷 Step 3 — Authorization Filter

Once the `SecurityContext` is loaded and placed into `SecurityContextHolder`, the request moves to the **Authorization Filter.**

This filter answers one question:

> *"Yes, this user is authenticated — but do they have **permission** to access THIS specific resource?"*

The instructor explains:

> *"Authorization filter got SecurityContext. This has authentication data where all user details are present — username, password, role. So it knows what role this user has."*

```
┌──────────────────────────────────────────────────────────────┐
│                    Authorization Filter                      │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  Question: Can user "user" access /users endpoint?           │
│                                                              │
│  Checks two things:                                          │
│                                                              │
│  1. What role does this endpoint REQUIRE?                    │
│     → Defined in SecurityFilterChain config                  │
│     → e.g. /users requires "ROLE_USER"                       │
│                                                              │
│  2. What role does this USER HAVE?                           │
│     → Fetched from SecurityContext                           │
│     → e.g. user has "ROLE_USER"                              │
│                                                              │
│  ┌─────────────────┬──────────────────────────────────┐      │
│  │ Required Role   │  ROLE_USER                       │      │
│  │ User's Role     │  ROLE_USER                       │      │
│  │ Match?          │  YES ✅ → proceed to controller   │      │
│  └─────────────────┴──────────────────────────────────┘      │
│                                                              │
│  ┌─────────────────┬──────────────────────────────────┐      │
│  │ Required Role   │  ROLE_USER                       │      │
│  │ User's Role     │  ROLE_ADMIN                      │      │
│  │ Match?          │  NO ❌ → 403 FORBIDDEN            │      │
│  └─────────────────┴──────────────────────────────────┘      │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

---

### 🔷 Step 4 — Dispatcher Servlet → Controller

Only if the Authorization Filter gives the green light does the request finally reach:

```
Authorization Filter ✅
        │
        ▼
Dispatcher Servlet
        │
        ▼
Interceptors (if any)
        │
        ▼
Controller → Business Logic → Response back to Client
```

---

## Side-by-Side Comparison: Login vs Subsequent Request

This is very important to understand — the **filters involved are completely different:**

```
┌──────────────────────────────────────────────────────────────────────┐
│          LOGIN REQUEST          │      SUBSEQUENT REQUEST            │
│          (/login)               │      (/users or any API)           │
├─────────────────────────────────┼────────────────────────────────────┤
│ Carries: username + password    │ Carries: JSESSIONID cookie only    │
├─────────────────────────────────┼────────────────────────────────────┤
│ Filter 1:                       │ Filter 1:                          │
│ UsernamePassword                │ SecurityContextHolder              │
│ AuthenticationFilter            │ Filter                             │
│ (creates Auth object)           │ (loads SecurityContext             │
│                                 │  from HttpSession)                 │
├─────────────────────────────────┼────────────────────────────────────┤
│ AuthenticationManager           │ HttpSessionSecurity                │
│ → DaoAuthenticationProvider     │ ContextRepository                  │
│ → PasswordEncoder               │ (looks up session by ID)           │
│ → UserDetailsService            │                                    │
│ (validates credentials)         │                                    │
├─────────────────────────────────┼────────────────────────────────────┤
│ SecurityContextHolderFilter     │ Authorization Filter               │
│ → Creates SecurityContext       │ (checks if user has                │
│ → HttpSessionSecurityContext    │  permission for this resource)     │
│   Repository                    │                                    │
│ → Creates HttpSession           │                                    │
│ → Stores SecurityContext in it  │                                    │
├─────────────────────────────────┼────────────────────────────────────┤
│ Result:                         │ Result:                            │
│ JSESSIONID cookie sent          │ Request fulfilled ✅                │
│ back to client 🍪               │ OR 403 Forbidden ❌                 │
│                                 │ OR redirect to /login ❌            │
└─────────────────────────────────┴────────────────────────────────────┘
```

---

## What Code Do You Need for Subsequent Request Validation?

Again — **nothing extra.** The framework handles all of this automatically.

But if you want to **customize** which endpoints need authentication and which don't, you write your own `SecurityFilterChain`:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Public API — no authentication needed
                .requestMatchers("/users").permitAll()
                // Everything else must be authenticated
                .anyRequest().authenticated()
            )
            .formLogin(Customizer.withDefaults()); // keep form login

        return http.build();
    }
}
```

The instructor explains why this bean matters:

> *"When you have to work with JWT, you will always see that we write this SecurityFilterChain because we don't have to use `.formLogin()` — we have to tell it something like JWT. Form login is the default one, otherwise we have to tell it."*

So understanding this config is foundational for **everything that comes after** — Basic Auth, JWT, OAuth.

---

## 🎯 Interview Tips from the Instructor

> **Q: Which filter handles session validation for subsequent requests in form-based authentication?**
>
> **A:** `SecurityContextHolderFilter` — it coordinates with `HttpSessionSecurityContextRepository` to look up the `HttpSession` using the `JSESSIONID` from the cookie. If found and valid, it extracts the `SecurityContext` and stores it in `SecurityContextHolder` for the duration of the request.

> **Q: What happens if the session has expired and the user makes a request?**
>
> **A:** `HttpSessionSecurityContextRepository` will not find a valid `HttpSession` for the given `JSESSIONID`. The user gets redirected to `/login` and must authenticate again.

> **Q: What is SecurityContextHolder and why is it important?**
>
> **A:** It's a thread-local storage that holds the `SecurityContext` (which contains the `Authentication` object) for the **entire lifecycle of a single request** — from filters all the way through to the controller. Any part of your application can access the current user's authentication details from it during request processing.

---
# Step 5 — Configuration & Customization

---

## Setting the Scene

So far, everything has been working **out of the box** — default login page, default URLs, all endpoints requiring authentication. But in real applications you always need to customize things.

The instructor lists the common customization needs:

> *"Let's say I want to change few things like — I don't want this default login page, I want to create mine. Or I want to relax authentication on few endpoints. By default, no matter any request, you have to be authenticated. But I have certain public APIs for which authentication is not required."*

All of this is done by writing **your own `SecurityFilterChain` bean** — which overrides Spring Boot's default one.

---

## The Master Configuration Class

Everything in Spring Security customization flows through one place — `SecurityConfig.java`. Let's build it up piece by piece exactly as the instructor taught.

```
┌──────────────────────────────────────────────────────────────────────┐
│                    SecurityConfig — The Control Center               │
│                                                                      │
│  This single class controls:                                         │
│                                                                      │
│  1. Which endpoints are PUBLIC (no auth needed)                      │
│  2. Which endpoints are PROTECTED (auth required)                    │
│  3. Which endpoints need specific ROLES                              │
│  4. What login page to use (default or custom)                       │
│  5. What logout page to use (default or custom)                      │
│  6. How passwords are encoded                                        │
│  7. Session management rules                                         │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Customization 1 — Relaxing Authentication on Public Endpoints

The industry standard is — your **registration endpoint must be public.** You can't ask someone to authenticate before they've even created an account.

```
┌──────────────────────────────────────────────────────────────────────┐
│               PUBLIC vs PROTECTED ENDPOINTS                          │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  PUBLIC (permitAll)          PROTECTED (authenticated)               │
│  ─────────────────           ───────────────────────                 │
│  /auth/register              /users                                  │
│  /auth/login                 /orders                                 │
│  /home                       /profile                                │
│  /about                      /dashboard                              │
│                              (anything sensitive)                    │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

**Code:**

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Anyone can hit this — no login needed
                .requestMatchers("/auth/register").permitAll()
                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .formLogin(Customizer.withDefaults()); // use default login page

        return http.build();
    }
}
```

> **Important:** Order of `requestMatchers` matters. Always put specific rules **before** `.anyRequest()`. Spring evaluates them top to bottom — first match wins.

---

## Customization 2 — Custom Login & Logout Page

By default Spring Boot gives you its own `/login` and `/logout` pages. If you want your own:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/register").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                // Your custom login page URL
                .loginPage("/my-login")
                // Where to redirect after successful login
                .defaultSuccessUrl("/dashboard", true)
                // Where to redirect after failed login
                .failureUrl("/my-login?error=true")
                // Make your custom login page public too
                .permitAll()
            )
            .logout(logout -> logout
                // Your custom logout URL
                .logoutUrl("/my-logout")
                // Where to go after logout
                .logoutSuccessUrl("/my-login?logout=true")
                .permitAll()
            );

        return http.build();
    }
}
```

```
┌──────────────────────────────────────────────────────────────────────┐
│              DEFAULT vs CUSTOM URLs                                  │
├────────────────────────┬───────────────────────────────────────────  │
│  DEFAULT               │  CUSTOM (you define these)                  │
├────────────────────────┼──────────────────────────────────────────── │
│  Login  → /login       │  Login  → /my-login (any URL you want)      │
│  Logout → /logout      │  Logout → /my-logout (any URL you want)     │
└────────────────────────┴─────────────────────────────────────────────┘
```

---

## Customization 3 — Password Encoder Bean

Whenever you're storing users in DB (production), you need to define which password encoder to use. This tells Spring Security:

> *"Don't use DelegatingPasswordEncoder — always use BCrypt directly."*

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Define which encoder to use globally
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/register").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(Customizer.withDefaults());

        return http.build();
    }
}
```

Once you define this bean, you no longer need to prefix passwords with `{bcrypt}` — Spring directly uses `BCryptPasswordEncoder` for all password operations.

---

## Customization 4 — CSRF Configuration

CSRF is **enabled by default** for form-based authentication. The instructor is very clear:

> *"Form-based authentication is vulnerable to CSRF attack. By default CSRF is enabled for form based login. We should NOT disable it."*

But for APIs (like when you're building REST APIs with JWT later), you'll disable it:

```java
// ✅ CORRECT for Form-Based Auth — CSRF enabled (default, no code needed)
.formLogin(Customizer.withDefaults())

// ❌ DO NOT DO THIS for Form-Based Auth
.csrf(csrf -> csrf.disable())

// ✅ OK for REST APIs / JWT (stateless — no session, no CSRF needed)
.csrf(csrf -> csrf.disable())
.sessionManagement(session -> session
    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
)
```

---

## Putting It All Together — Full Production-Ready Config

Here's the complete `SecurityConfig.java` combining everything the instructor covered:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Step 1 — Define password encoder
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Step 2 — Define the security rules
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {
        http
            // Step 3 — Define which endpoints are public vs protected
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/register").permitAll() // public
                .anyRequest().authenticated()                  // rest protected
            )

            // Step 4 — Use default form login
            // (or customize with .loginPage("/my-login") if needed)
            .formLogin(Customizer.withDefaults())

            // Step 5 — DO NOT disable CSRF for form-based auth
            // (it's enabled by default, so no code needed here)

            // Step 6 — Session management (optional customization)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            );

        return http.build();
    }
}
```

---

## The Supporting Classes for DB-Based User Storage

When you're storing users in DB (production), you need these classes. The instructor walked through all of them:

### UserAuthEntity.java — The User Table

```java
@Entity
@Table(name = "user_auth")
/*
 Implements UserDetails because during Authentication
 (form, basic, jwt etc.), security framework tries to fetch
 the user and return UserDetails only. If we don't implement
 it, we'd have to do manual mapping from UserAuthEntity to UserDetails.
*/
public class UserAuthEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String role;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    // These return true = account is active & valid
    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }

    @Override
    public String getPassword() { return password; }

    @Override
    public String getUsername() { return username; }

    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
```

### UserAuthEntityRepository.java — DB Access

```java
@Repository
public interface UserAuthEntityRepository
        extends JpaRepository<UserAuthEntity, Long> {

    // Spring Security calls loadUserByUsername() which needs this
    Optional<UserAuthEntity> findByUsername(String username);
}
```

### UserAuthEntityService.java — Bridges Spring Security & Your DB

```java
@Service
/*
 Implements UserDetailsService because during authentication,
 Spring Security doesn't know how to fetch your user from YOUR DB.
 So we override loadUserByUsername() to tell it how.
*/
public class UserAuthEntityService implements UserDetailsService {

    @Autowired
    private UserAuthEntityRepository userAuthEntityRepository;

    public UserDetails save(UserAuthEntity userAuth) {
        return userAuthEntityRepository.save(userAuth);
    }

    @Override
    public UserAuthEntity loadUserByUsername(String username)
            throws UsernameNotFoundException {

        return userAuthEntityRepository.findByUsername(username)
                .orElseThrow(() ->
                    new UsernameNotFoundException("User not found"));
    }
}
```

### UserAuthController.java — Registration Endpoint

```java
@RestController
@RequestMapping("/auth")
public class UserAuthController {

    @Autowired
    private UserAuthEntityService userAuthEntityService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<String> register(
            @RequestBody UserAuthEntity userAuthDetails) {

        // ALWAYS hash the password before saving to DB
        userAuthDetails.setPassword(
            passwordEncoder.encode(userAuthDetails.getPassword())
        );

        userAuthEntityService.save(userAuthDetails);

        return ResponseEntity.ok("User registered successfully!");
    }
}
```

---

## Full Picture — How All Classes Connect

```
┌──────────────────────────────────────────────────────────────────────┐
│              HOW ALL THE PIECES FIT TOGETHER                         │
│                                                                      │
│  POST /auth/register (public endpoint)                               │
│         │                                                            │
│         ▼                                                            │
│  UserAuthController                                                  │
│  → passwordEncoder.encode(rawPassword)  ← BCryptPasswordEncoder      │
│  → userAuthEntityService.save(user)                                  │
│         │                                                            │
│         ▼                                                            │
│  UserAuthEntityService                                               │
│  → userAuthEntityRepository.save(user)                               │
│         │                                                            │
│         ▼                                                            │
│  Database (user_auth table)                                          │
│  {id, username, hashed_password, role}                               │
│                                                                      │
│  ─────────────────────────────────────────────                       │
│                                                                      │
│  POST /login (Spring handles this — you write nothing)               │
│         │                                                            │
│         ▼                                                            │
│  DaoAuthenticationProvider                                           │
│  → calls UserDetailsService.loadUserByUsername()                     │
│  → UserAuthEntityService fetches from DB                             │
│  → Compares passwords using BCryptPasswordEncoder                    │
│  → Returns fully authenticated object                                │
│         │                                                            │
│         ▼                                                            │
│  HttpSession created → JSESSIONID sent to client 🍪                  │
└──────────────────────────────────────────────────────────────────────┘
```

---

## application.properties — Complete Reference

```properties
# ── USER CREATION (dev/testing only) ──────────────────────────────
spring.security.user.name=user
spring.security.user.password=pass
spring.security.user.roles=USER

# ── SESSION STORAGE IN DB ─────────────────────────────────────────
spring.session.store-type=jdbc
spring.session.jdbc.initialize-schema=always  # Spring creates tables

# ── SESSION TIMEOUT ───────────────────────────────────────────────
server.servlet.session.timeout=5m             # 5 min inactivity = expire
```

---

## 🎯 Interview Tips from the Instructor

> **Q: Why do we write our own SecurityFilterChain bean?**
>
> **A:** To override Spring Boot's default security configuration. The default requires all endpoints to be authenticated and uses form login with default URLs. When we write our own bean, we can define public endpoints, custom login/logout pages, role-based restrictions, and switch to other authentication methods like JWT. This is also the reason you'll always see a custom `SecurityFilterChain` in JWT-based apps — because you need to explicitly tell Spring NOT to use form login.

> **Q: Why does `UserAuthEntityService` implement `UserDetailsService`?**
>
> **A:** Spring Security's authentication mechanism internally calls `loadUserByUsername()` to fetch user details during login. Since our users are stored in our own DB, Spring doesn't know how to fetch them. By implementing `UserDetailsService` and overriding `loadUserByUsername()`, we tell Spring exactly how to find the user — it queries our `user_auth` table via the repository.

> **Q: Why does `UserAuthEntity` implement `UserDetails`?**
>
> **A:** During authentication, Spring Security expects a `UserDetails` object back from `loadUserByUsername()`. If our entity doesn't implement it, we'd have to write a manual mapping. Implementing it directly avoids that extra step and keeps the code clean.

---
# Step 6 — Authorization Filter

---

## Setting the Scene

At this point the user is **authenticated** — we know who they are. But authentication alone isn't enough. Just because you're logged into a system doesn't mean you can access everything in it.

Think of a real office building:
- Your ID badge lets you **enter the building** → Authentication
- But only certain badges let you **enter the server room** → Authorization

The instructor puts it simply:

> *"Once the user is authenticated, when user is trying to access any resource, authorization check is mandatory. It is done to make sure that user has permission to access it."*

---

## Two Phases of Authorization

This is something the instructor specifically highlights — authorization doesn't happen at just one place. It happens at **two different points:**

```
┌──────────────────────────────────────────────────────────────────────┐
│                  AUTHORIZATION — 2 PHASES                            │
│                                                                      │
│                                                                      │
│   Incoming Request                                                   │
│        │                                                             │
│        ▼                                                             │
│  ┌─────────────────────┐                                             │
│  │  Security Filter    │  ← PHASE 1: Authorization check             │
│  │  Chain              │    as part of Security Filter               │
│  │                     │    (AuthorizationFilter)                    │
│  │  Authorization      │                                             │
│  │  Filter             │    Checks: Does user have required          │
│  │                     │    role to access this endpoint?            │
│  └─────────────────────┘                                             │
│        │                                                             │
│        ▼                                                             │
│  ┌─────────────────────┐                                             │
│  │  Dispatcher Servlet │                                             │
│  │  → Interceptors     │                                             │
│  │  → Controller       │  ← PHASE 2: Authorization check             │
│  └─────────────────────┘    after request reaches Controller         │
│                              (using @PreAuthorize, @Secured etc.)    │
│                              This is COMMON for ALL auth methods     │
│                              (Form, Basic, JWT)                      │
│                              — covered later                         │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

The instructor is clear about scope:

> *"This part — security filter — is unique for each authentication method. But the controller-level authorization is common for all authentication methods. We will see that later."*

So in this step, we focus entirely on **Phase 1 — filter-level authorization.**

---

## The Default Behavior — No Restrictions

This is a very important point the instructor emphasizes:

> *"By default, Spring Boot Security does NOT put any restriction on any resource. We have to do it manually."*

```
┌──────────────────────────────────────────────────────────────────────┐
│              DEFAULT SPRING BOOT SECURITY BEHAVIOR                   │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Rule: Any authenticated user can access ANY endpoint                │
│                                                                      │
│  User1 (ROLE_USER)  → hits /admin  → ✅ Allowed (no restriction)      │
│  User2 (ROLE_ADMIN) → hits /users  → ✅ Allowed (no restriction)      │
│                                                                      │
│  This means: Authentication ✅ = Access to everything                 │
│                                                                      │
│  To add restrictions → you MUST configure it manually                │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## How Authorization Filter Works Internally

```
┌──────────────────────────────────────────────────────────────────────┐
│              AUTHORIZATION FILTER — INTERNAL FLOW                    │
│                                                                      │
│  Incoming request: GET /users                                        │
│  Cookie: JSESSIONID=abc123                                           │
│                    │                                                 │
│                    ▼                                                 │
│  SecurityContextHolder already has:                                  │
│  ┌────────────────────────────────────┐                              │
│  │  Authentication Object            │                               │
│  │  username      = "user"           │                               │
│  │  authenticated = TRUE             │                               │
│  │  roles         = ["ROLE_USER"]   │                                │
│  └────────────────────────────────────┘                              │
│                    │                                                 │
│                    ▼                                                 │
│  Authorization Filter checks:                                        │
│                                                                      │
│  Q1: Does /users endpoint have any role restriction?                 │
│  → YES → requires ROLE_USER                                          │
│  (defined in SecurityFilterChain config)                             │
│                    │                                                 │
│                    ▼                                                 │
│  Q2: Does the current user have ROLE_USER?                           │
│  → Check SecurityContext → roles = ["ROLE_USER"]                     │
│                    │                                                 │
│         ┌──────────┴──────────┐                                      │
│         │                     │                                      │
│    Role matches          Role missing                                │
│         │                     │                                      │
│         ▼                     ▼                                      │
│   Proceed to            403 FORBIDDEN ❌                              │
│   Controller ✅                                                       │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Configuring Authorization — All Variations

### Variation 1 — Single Role Restriction

Restrict an endpoint to users who have a specific role:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Only users with ROLE_USER can access /users
                .requestMatchers("/users").hasRole("USER")
                .anyRequest().authenticated()
            )
            .formLogin(Customizer.withDefaults());

        return http.build();
    }
}
```

**Corresponding `application.properties`:**

```properties
spring.security.user.name=user
spring.security.user.password=pass
# Assign ROLE_USER to this user
spring.security.user.roles=USER

spring.session.store-type=jdbc
spring.session.jdbc.initialize-schema=always
server.servlet.session.timeout=5m
```

---

### Variation 2 — Multiple Roles Allowed (Any One is Fine)

Allow access if the user has **any one** of the listed roles:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Users with EITHER ROLE_USER OR ROLE_ADMIN can access /users
                .requestMatchers("/users").hasAnyRole("USER", "ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(Customizer.withDefaults());

        return http.build();
    }
}
```

**Corresponding `application.properties`:**

```properties
spring.security.user.name=user
spring.security.user.password=pass
# Assign BOTH roles to this user (comma separated)
spring.security.user.roles=USER,ADMIN

spring.session.store-type=jdbc
spring.session.jdbc.initialize-schema=always
server.servlet.session.timeout=5m
```

---

### Variation 3 — Different Roles for Different Endpoints

Real applications have many endpoints with different access levels:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Fully public — no auth needed
                .requestMatchers("/auth/register").permitAll()

                // Any authenticated user can access
                .requestMatchers("/home").authenticated()

                // Only ROLE_USER
                .requestMatchers("/users").hasRole("USER")

                // Only ROLE_ADMIN
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // ROLE_USER or ROLE_ADMIN
                .requestMatchers("/orders").hasAnyRole("USER", "ADMIN")

                // Everything else needs authentication
                .anyRequest().authenticated()
            )
            .formLogin(Customizer.withDefaults());

        return http.build();
    }
}
```

---

## The ROLE_ Prefix — A Common Gotcha

The instructor specifically calls this out:

> *"You don't need to add ROLE_ yourself — it gets appended automatically. Internally it would become ROLE_USER. You can give anything, it's a string, but maintain proper naming conventions."*

```
┌──────────────────────────────────────────────────────────────────────┐
│                    THE ROLE_ PREFIX RULE                             │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  What YOU write:                                                     │
│  .hasRole("USER")          → Spring appends → ROLE_USER              │
│  .hasRole("ADMIN")         → Spring appends → ROLE_ADMIN             │
│  .hasAnyRole("USER","ADMIN")→ Spring appends → ROLE_USER, ROLE_ADMIN │
│                                                                      │
│  What you store in DB / properties:                                  │
│  spring.security.user.roles=USER  → stored as ROLE_USER internally   │
│                                                                      │
│  ⚠️  Common Mistake:                                                 │
│  .hasRole("ROLE_USER")  ← WRONG — Spring will make it ROLE_ROLE_USER │
│  .hasRole("USER")       ← CORRECT                                    │
│                                                                      │
│  BUT if you use hasAuthority() instead of hasRole():                 │
│  .hasAuthority("ROLE_USER") ← here you MUST write ROLE_ yourself     │
│  (hasAuthority does NOT auto-append ROLE_)                           │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## What Happens When Authorization Fails

When a user tries to access an endpoint they don't have permission for, Spring Security throws a **403 FORBIDDEN** error.

The instructor demonstrated this live:

```
┌──────────────────────────────────────────────────────────────────────┐
│                    AUTHORIZATION FAILURE SCENARIO                    │
│                                                                      │
│  Setup:                                                              │
│  → /users endpoint requires ROLE_USER                                │
│  → User is assigned ROLE_ADMIN (not ROLE_USER)                       │
│                                                                      │
│  Flow:                                                               │
│  User logs in ✅ (authentication passes)                              │
│  User hits /users                                                    │
│  Authorization Filter checks:                                        │
│  Required: ROLE_USER                                                 │
│  User has: ROLE_ADMIN                                                │
│  Match: ❌                                                            │
│                                                                      │
│  Result:                                                             │
│  HTTP 403 Forbidden                                                  │
│  "There was an unexpected error (type=Forbidden, status=403)"        │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Full Picture — Authorization in Context of Complete Flow

```
┌──────────────────────────────────────────────────────────────────────┐
│         WHERE AUTHORIZATION FITS IN THE COMPLETE FLOW                │
│                                                                      │
│  Request: GET /users                                                 │
│  Cookie: JSESSIONID=abc123                                           │
│                    │                                                 │
│                    ▼                                                 │
│  ┌─────────────────────────────────────────────────┐                 │
│  │             Security Filter Chain               │                 │
│  │                                                 │                 │
│  │  Filter 1: SecurityContextHolderFilter          │                 │
│  │  → Loads SecurityContext from HttpSession       │                 │
│  │  → Puts it in SecurityContextHolder             │                 │
│  │                    │                            │                 │
│  │                    ▼                            │                 │
│  │  Filter 2: AuthorizationFilter   ← WE ARE HERE  │                 │
│  │  → Reads SecurityContext from Holder            │                 │
│  │  → Checks endpoint's required role              │                 │
│  │  → Checks user's actual role                    │                 │
│  │  → Match ✅ → pass through                       │                 │
│  │  → No match ❌ → 403 Forbidden                   │                 │
│  └─────────────────────────────────────────────────┘                 │
│                    │                                                 │
│                    ▼                                                 │
│  Dispatcher Servlet → Controller → Fulfill Request ✅                 │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 🎯 Interview Tips from the Instructor

> **Q: What is the difference between Authentication and Authorization?**
>
> **A:** Authentication is verifying WHO you are — validating username and password, creating a session. Authorization is verifying WHAT you're allowed to do — checking if the authenticated user has the required role or permission to access a specific resource. Authentication always happens before authorization.

> **Q: By default, does Spring Boot Security restrict access to resources based on roles?**
>
> **A:** No. By default, Spring Boot Security only checks if a user is authenticated — not what role they have. Role-based restrictions must be configured manually using `.hasRole()`, `.hasAnyRole()`, or `.hasAuthority()` in the `SecurityFilterChain`.

> **Q: What is the difference between `hasRole()` and `hasAuthority()`?**
>
> **A:** `hasRole("USER")` automatically prepends `ROLE_` making it `ROLE_USER` internally. `hasAuthority("ROLE_USER")` does NOT prepend anything — you must write the full string yourself. Using `.hasRole("ROLE_USER")` is a common mistake that results in Spring looking for `ROLE_ROLE_USER`.

> **Q: At how many places does authorization happen in Spring Security?**
>
> **A:** Two places. Phase 1 — at the Security Filter Chain level via `AuthorizationFilter`, configured in `SecurityFilterChain`. Phase 2 — at the Controller level using method-level security annotations like `@PreAuthorize` or `@Secured`. The filter-level check is unique per authentication method, but the controller-level check is common across all methods (Form, Basic, JWT).

> **Q: What HTTP status code does Spring Security return when authorization fails?**
>
> **A:** 403 Forbidden — thrown by the `AuthorizationFilter` when the user's role doesn't match the required role for the endpoint.

---
# Step 7 — Session Management Controls

---

## Setting the Scene

Authentication ✅. Authorization ✅. But there's still one more thing to control — **how sessions themselves are managed.**

Two important questions the instructor addresses:

> *"What if one user keeps logging in from different browsers and creates 1000 sessions? We have to control sessions per user."*

> *"What is session creation policy? When exactly should a session be created — always, never, only when needed?"*

---

## Part A — Controlling Sessions Per User

### The Problem

```
┌──────────────────────────────────────────────────────────────────────┐
│              THE MULTIPLE SESSION PROBLEM                            │
│                                                                      │
│  Same user "john" logs in from:                                      │
│                                                                      │
│  Browser 1  → Session 1 created (JSESSIONID = aaa)                   │
│  Browser 2  → Session 2 created (JSESSIONID = bbb)                   │
│  Browser 3  → Session 3 created (JSESSIONID = ccc)                   │
│  Incognito  → Session 4 created (JSESSIONID = ddd)                   │
│  ...                                                                 │
│  Browser N  → Session N created (JSESSIONID = zzz)                   │
│                                                                      │
│  Problems:                                                           │
│  1. DB fills up with session records for same user                   │
│  2. Security risk — if credentials are stolen,                       │
│     attacker can keep creating sessions                              │
│  3. No control over where user is logged in                          │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

### The Solution — `maximumSessions`

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/users").hasRole("USER")
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                // Only 1 active session allowed per user
                .maximumSessions(1)
                // If limit reached, PREVENT new login
                // (instead of invalidating old session)
                .maxSessionsPreventsLogin(true)
            )
            .formLogin(Customizer.withDefaults());

        return http.build();
    }
}
```

### What Happens When Limit is Exceeded?

```
┌──────────────────────────────────────────────────────────────────────┐
│           BEHAVIOR WHEN maximumSessions(1) IS SET                    │
│                                                                      │
│  Scenario:                                                           │
│  → User "john" already logged in on Browser 1                        │
│  → john tries to login on Browser 2                                  │
│                                                                      │
│  WITH maxSessionsPreventsLogin(true):                                │
│  → New login is BLOCKED ❌                                            │
│  → Error: "Maximum sessions of 1 for this principal exceeded"        │
│  → Old session on Browser 1 stays ACTIVE                             │
│                                                                      │
│  WITH maxSessionsPreventsLogin(false) [default]:                     │
│  → New login is ALLOWED ✅                                            │
│  → OLD session on Browser 1 gets INVALIDATED                         │
│  → Only new session on Browser 2 is active                           │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

### Allowing More Than One Session

If your business needs allow multiple sessions (e.g., mobile + desktop):

```java
.sessionManagement(session -> session
    // Allow up to 3 concurrent sessions per user
    .maximumSessions(3)
    .maxSessionsPreventsLogin(true)
)
```

---

## Part B — Session Creation Policies

The instructor explains there are **four policies** that control WHEN an HttpSession gets created.

### The Four Policies — Visual Reference

```
┌──────────────────────────────────────────────────────────────────────┐
│                  SESSION CREATION POLICIES                           │
├─────────────────┬──────────────────────────────────────────────────  │
│   POLICY        │  BEHAVIOR                                          │
├─────────────────┼──────────────────────────────────────────────────  │
│                 │  Session created ONLY when needed                  │
│  IF_REQUIRED    │  Public APIs → NO session created                  │
│  (DEFAULT ✅)    │  Protected APIs → session created if required      │
│                 │  Most efficient — recommended for form-based auth  │
├─────────────────┼──────────────────────────────────────────────────  │
│                 │  Session ALWAYS created for every request          │
│  ALWAYS         │  Even public APIs get a session                    │
│                 │  Wasteful — not recommended                        │
│                 │  If session already exists, reuses it              │
├─────────────────┼──────────────────────────────────────────────────  │
│                 │  NEVER creates a new session                       │
│  NEVER          │  But USES existing session if present              │
│                 │  Niche use case                                    │
├─────────────────┼──────────────────────────────────────────────────  │
│                 │  NO session ever — completely stateless            │
│  STATELESS      │  Used for JWT / REST APIs                          │
│                 │  No session = no JSESSIONID                        │
│                 │  Perfect for microservices                         │
└─────────────────┴──────────────────────────────────────────────────  │
```

### Visual Comparison — IF_REQUIRED vs ALWAYS

```
┌──────────────────────────────────────────────────────────────────────┐
│          IF_REQUIRED (DEFAULT)        │        ALWAYS                │
├───────────────────────────────────────┼──────────────────────────────┤
│                                       │                              │
│  GET /home (public API)               │  GET /home (public API)      │
│  → No authentication needed           │  → No authentication needed  │
│  → NO HttpSession created ✅           │  → HttpSession created ❌     │
│    (efficient)                        │    (wasteful)                │
│                                       │                              │
│  GET /users (protected API)           │  GET /users (protected API)  │
│  → Authentication needed              │  → Authentication needed     │
│  → HttpSession created ✅              │  → HttpSession created       │
│                                       │    (or reused if exists)     │
│                                       │                              │
└───────────────────────────────────────┴──────────────────────────────┘
```

The instructor explains why IF_REQUIRED is preferred:

> *"For public APIs, authentication is not required. So generally you don't require you don't need to maintain the state because let's say public API is hit by millions of times, you don't have to maintain authentication. It's just informational. So that's why IF_REQUIRED is generally preferred. And it is default."*

### The STATELESS Policy — Important for JWT

```
┌──────────────────────────────────────────────────────────────────────┐
│                  STATELESS — JWT USE CASE                            │
│                                                                      │
│  Form-Based Auth:                                                    │
│  Client ──── JSESSIONID cookie ────► Server                          │
│              (server stores session)                                 │
│                                                                      │
│  JWT (Stateless):                                                    │
│  Client ──── JWT Token ────────────► Server                          │
│              (server stores NOTHING)                                 │
│              (token itself carries all info)                         │
│                                                                      │
│  With STATELESS policy:                                              │
│  → No HttpSession created ever                                       │
│  → No JSESSIONID cookie                                              │
│  → Server is completely stateless                                    │
│  → Perfect for distributed systems                                   │
│  → No DB needed for session storage                                  │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

### Code — Setting Session Creation Policy

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/users").hasRole("USER")
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                // Explicitly set (though IF_REQUIRED is already default)
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            .formLogin(Customizer.withDefaults());

        return http.build();
    }
}
```

---

## Combining Both — Max Sessions + Creation Policy

In a real application you'd combine both controls together:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/register").permitAll()
                .requestMatchers("/users").hasRole("USER")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                // Policy — only create session when needed
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                // Max 1 session per user
                .maximumSessions(1)
                // Block new login if limit exceeded
                .maxSessionsPreventsLogin(true)
            )
            .formLogin(Customizer.withDefaults());

        return http.build();
    }
}
```

---

## Full Picture — Session Management in Context

```
┌──────────────────────────────────────────────────────────────────────┐
│           SESSION MANAGEMENT — COMPLETE PICTURE                      │
│                                                                      │
│  User "john" tries to login (Browser 1)                              │
│              │                                                       │
│              ▼                                                       │
│  Authentication passes ✅                                             │
│              │                                                       │
│              ▼                                                       │
│  Session Management Check:                                           │
│  → How many active sessions does "john" already have?                │
│  → Spring looks up sessions by principal_name = "john"               │
│    in SPRING_SESSION table                                           │
│              │                                                       │
│    ┌─────────┴──────────┐                                            │
│    │                    │                                            │
│  Under limit         At/Over limit                                   │
│    │                    │                                            │
│    ▼                    ▼                                            │
│  New session        maxSessionsPreventsLogin?                        │
│  created ✅               │                                           │
│                  ┌───────┴────────┐                                  │
│                  │                │                                  │
│                TRUE            FALSE                                 │
│                  │                │                                  │
│                  ▼                ▼                                  │
│           Block new          Invalidate                              │
│           login ❌           oldest session                           │
│           403 error          Allow new login ✅                       │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Complete application.properties Reference for This Step

```properties
# ── USER CREATION (dev/testing only) ──────────────────────────────
spring.security.user.name=user
spring.security.user.password=pass
spring.security.user.roles=USER

# ── SESSION STORAGE IN DB ─────────────────────────────────────────
spring.session.store-type=jdbc
spring.session.jdbc.initialize-schema=always

# ── SESSION TIMEOUT ───────────────────────────────────────────────
# After this duration of INACTIVITY, session expires
server.servlet.session.timeout=5m
```

---

## 🎯 Interview Tips from the Instructor

> **Q: How do you restrict a user to only one active session at a time in Spring Boot?**
>
> **A:** Using `.sessionManagement()` with `.maximumSessions(1)` and `.maxSessionsPreventsLogin(true)`. The first setting limits sessions to 1 per user (identified by their username/principal). The second setting controls what happens when the limit is hit — `true` blocks the new login, `false` invalidates the old session.

> **Q: What are the four Session Creation Policies in Spring Security?**
>
> **A:** `IF_REQUIRED` — default, creates session only when needed (not for public APIs). `ALWAYS` — always creates a session even for public APIs. `NEVER` — never creates a new session but uses existing one if present. `STATELESS` — no session ever created, used for JWT/REST APIs where the application is completely stateless.

> **Q: What Session Creation Policy should you use for JWT?**
>
> **A:** `STATELESS` — because JWT is a stateless authentication mechanism. The token itself carries all authentication information, so there's no need for the server to maintain any session state. This also eliminates the need for a session database and makes the application truly scalable across multiple instances.

> **Q: What is the default Session Creation Policy in Spring Security?**
>
> **A:** `IF_REQUIRED` — HttpSession is created only when needed. For public endpoints where no authentication is required, no session is created. This is the most efficient policy for form-based authentication.

---
# Step 8 — Disadvantages of Form-Based Authentication

---

## Setting the Scene

This is where everything comes full circle. You now understand form-based authentication deeply — how it works, how sessions are created, how they're validated, how authorization happens.

Now the instructor asks — **if it works so well, why did we move away from it?**

> *"I hope by this point of time, form login authentication is totally clear. Do practice it with your hand even though it is not very frequently used nowadays. JWT is very popular, but this is the fundamental — if this flow is clear, all other authentication methods, we will get to know how they are working and why exactly we need those."*

Understanding these disadvantages is what makes JWT and OAuth make sense later. Every disadvantage here is a problem that JWT directly solves.

---

## Disadvantage 1 — Vulnerable to Security Attacks

### CSRF (Cross-Site Request Forgery)

Since form-based auth uses **cookies** to carry the session ID, it is inherently vulnerable to CSRF attacks.

```
┌──────────────────────────────────────────────────────────────────────┐
│                     HOW CSRF ATTACK WORKS                            │
│                                                                      │
│  1. User logs into bank.com → gets JSESSIONID cookie                 │
│                                                                      │
│  2. User visits evil.com (malicious site)                            │
│     while still logged into bank.com                                 │
│                                                                      │
│  3. evil.com secretly sends a request to bank.com:                   │
│     POST bank.com/transfer?to=attacker&amount=10000                  │
│     Cookie: JSESSIONID=abc123  ← browser auto-attaches this!         │
│                                                                      │
│  4. bank.com sees a valid session → processes the transfer ❌         │
│                                                                      │
│  Why it works:                                                       │
│  Browser automatically sends cookies with every request              │
│  to the domain — even requests triggered by other sites              │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

**Spring's Default Protection:**

```
┌──────────────────────────────────────────────────────────────────────┐
│                    CSRF PROTECTION IN SPRING                         │
│                                                                      │
│  By default — CSRF is ENABLED for form-based login                   │
│                                                                      │
│  How it works:                                                       │
│  → Spring generates a unique CSRF token per session                  │
│  → This token is embedded in every HTML form                         │
│  → Every state-changing request (POST, PUT, DELETE)                  │
│    must include this token                                           │
│  → evil.com cannot read this token (same-origin policy)              │
│  → So forged requests fail ✅                                         │
│                                                                      │
│  The instructor warns:                                               │
│  "We should NOT disable CSRF for form-based authentication.          │
│   Always keep it enabled."                                           │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

```java
// ✅ CORRECT — CSRF enabled by default, no code needed
.formLogin(Customizer.withDefaults())

// ❌ NEVER do this for form-based auth
.csrf(csrf -> csrf.disable())

// ✅ OK for stateless REST APIs (JWT) — no session = no CSRF risk
.csrf(csrf -> csrf.disable())
.sessionManagement(session -> session
    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
)
```

---

### Session Hijacking

Since the `JSESSIONID` travels in a cookie with every request, it can be stolen:

```
┌──────────────────────────────────────────────────────────────────────┐
│                   HOW SESSION HIJACKING WORKS                        │
│                                                                      │
│  1. User logs in → gets JSESSIONID=abc123 in cookie                  │
│                                                                      │
│  2. Attacker intercepts the cookie                                   │
│     (via network sniffing, XSS attack,                               │
│      or physical access to browser)                                  │
│                                                                      │
│  3. Attacker uses stolen JSESSIONID:                                 │
│     GET bank.com/accounts                                            │
│     Cookie: JSESSIONID=abc123                                        │
│                                                                      │
│  4. Server sees valid session → gives attacker full access ❌         │
│                                                                      │
│  Why form-based auth is vulnerable:                                  │
│  The session ID is the only proof of identity                        │
│  Whoever has it — gets access                                        │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Disadvantage 2 — Session Management is a Big Overhead

The instructor draws from personal experience here:

> *"In 2015-16 when I was working in a company, we were writing our own tables and managing those tables — whether to increase the time or not, whether to update this column or not. All this stuff. Session management is a big overhead."*

```
┌──────────────────────────────────────────────────────────────────────┐
│              SESSION MANAGEMENT OVERHEAD                             │
│                                                                      │
│  Things you need to manage:                                          │
│                                                                      │
│  1. Session Tables                                                   │
│     → SPRING_SESSION table                                           │
│     → SPRING_SESSION_ATTRIBUTES table                                │
│     → Custom tables if business needs grow                           │
│                                                                      │
│  2. Session Expiry                                                   │
│     → Track last_access_time                                         │
│     → Calculate expiry_time                                          │
│     → Clean up expired sessions from DB                              │
│     → Handle edge cases (concurrent requests,                        │
│       mid-session updates etc.)                                      │
│                                                                      │
│  3. Business-Specific Session Rules                                  │
│     → Max sessions per user                                          │
│     → Force logout on password change                                │
│     → Invalidate sessions on suspicious activity                     │
│     → Role changes mid-session                                       │
│                                                                      │
│  4. Session Security                                                 │
│     → Rotate session ID after login                                  │
│     → Secure cookie flags (HttpOnly, Secure, SameSite)               │
│     → Handle session fixation attacks                                │
│                                                                      │
│  Simple businesses → let framework manage it                         │
│  Complex businesses → must manage manually → HUGE overhead           │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Disadvantage 3 — Scalability Issues in Distributed Systems

This is the biggest and most important disadvantage. The instructor explains it through a very clear scenario:

```
┌──────────────────────────────────────────────────────────────────────┐
│           SCALABILITY PROBLEM — IN-MEMORY SESSION                    │
│                                                                      │
│                    ┌──────────────────┐                              │
│                    │   Load Balancer  │                              │
│                    └────────┬─────────┘                              │
│                             │                                        │
│              ┌──────────────┴──────────────┐                         │
│              │                             │                         │
│              ▼                             ▼                         │
│   ┌──────────────────┐         ┌──────────────────┐                  │
│   │    Server 1      │         │    Server 2       │                 │
│   │                  │         │                  │                  │
│   │  Session: abc123 │         │  No sessions!    │                  │
│   │  (in memory)     │         │  (empty memory)  │                  │
│   └──────────────────┘         └──────────────────┘                  │
│                                                                      │
│  Request 1 → goes to Server 1 → session found ✅                      │
│  Request 2 → goes to Server 2 → session NOT found ❌                  │
│  → "Please login again" → BAD user experience                        │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘


┌──────────────────────────────────────────────────────────────────────┐
│           SCALABILITY SOLUTION — DB SESSION STORAGE                  │
│                                                                      │
│                    ┌──────────────────┐                              │
│                    │   Load Balancer  │                              │
│                    └────────┬─────────┘                              │
│                             │                                        │
│              ┌──────────────┴──────────────┐                         │
│              │                             │                         │
│              ▼                             ▼                         │
│   ┌──────────────────┐         ┌──────────────────┐                  │
│   │    Server 1       │         │    Server 2       │                │
│   └────────┬─────────┘         └────────┬─────────┘                  │
│            │                            │                            │
│            └──────────┬─────────────────┘                            │
│                       │                                              │
│                       ▼                                              │ 
│            ┌──────────────────────┐                                  │
│            │      Database        │                                  │
│            │   SPRING_SESSION     │                                  │
│            │   abc123 → active ✅ │                                   │
│            └──────────────────────┘                                  │
│                                                                      │
│  Request 1 → Server 1 → queries DB → session found ✅                 │
│  Request 2 → Server 2 → queries DB → session found ✅                 │
│  → Consistent experience → but now you have DB overhead              │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Disadvantage 4 — Database Load & Latency

Even after solving the distributed system problem by storing sessions in DB, you introduce a new problem:

```
┌──────────────────────────────────────────────────────────────────────┐
│              DATABASE LOAD PROBLEM                                   │
│                                                                      │
│  Every single API request now requires:                              │
│                                                                      │
│  1. Read from DB → fetch HttpSession by JSESSIONID                   │
│  2. Validate session → check expiry                                  │
│  3. Read SecurityContext from session attributes                     │
│  4. Write to DB → update last_access_time                            │
│  5. Write to DB → recalculate expiry_time                            │
│                                                                      │
│  Impact:                                                             │
│  → Every request = multiple DB operations                            │
│  → High traffic = massive DB load                                    │
│  → DB becomes bottleneck                                             │
│  → Added latency on every single request                             │
│  → Need caching layer (Redis etc.) → more infrastructure             │
│                                                                      │
│  The instructor puts it clearly:                                     │
│  "Database load — if there are multiple servers then we              │
│   might need to store session in DB or cache, which again            │
│   requires memory and lookup time, which increases latency."         │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## All Disadvantages — Summary Table

```
┌──────────────────────────────────────────────────────────────────────┐
│         DISADVANTAGES OF FORM-BASED AUTHENTICATION                  │
├───┬─────────────────────────┬────────────────────────────────────── │
│ # │  Disadvantage           │  Details                              │
├───┼─────────────────────────┼────────────────────────────────────── │
│ 1 │  CSRF Vulnerability     │  Cookie auto-sent by browser          │
│   │                         │  enables cross-site request forgery   │
│   │                         │  Must keep CSRF enabled always        │
├───┼─────────────────────────┼────────────────────────────────────── │
│ 2 │  Session Hijacking      │  JSESSIONID stolen = full access      │
│   │                         │  to attacker                          │
│   │                         │  Session ID is single point of        │
│   │                         │  failure for security                 │
├───┼─────────────────────────┼────────────────────────────────────── │
│ 3 │  Session Management     │  Tracking expiry, cleanup,            │
│   │  Overhead               │  business rules, security rules       │
│   │                         │  all need manual management           │
│   │                         │  as business grows                    │
├───┼─────────────────────────┼────────────────────────────────────── │
│ 4 │  Distributed System     │  Multiple servers can't share         │
│   │  Scalability            │  in-memory sessions                   │
│   │                         │  Must use DB/cache for sharing        │
├───┼─────────────────────────┼────────────────────────────────────── │
│ 5 │  Database Load          │  Every request = DB read + write      │
│   │  & Latency              │  High traffic = DB bottleneck         │
│   │                         │  Adds latency to every request        │
└───┴─────────────────────────┴────────────────────────────────────── │
```

---

## How JWT Solves These Problems (Preview)

This is the bridge the instructor builds — every disadvantage above points directly to why JWT exists:

```
┌──────────────────────────────────────────────────────────────────────┐
│     FORM-BASED PROBLEM → JWT SOLUTION                               │
├─────────────────────────────┬────────────────────────────────────── │
│  Form-Based Problem         │  JWT Solution                         │
├─────────────────────────────┼────────────────────────────────────── │
│  CSRF vulnerable            │  Token in Authorization header        │
│  (cookie auto-sent)         │  not in cookie → no auto-send         │
│                             │  → no CSRF risk                       │
├─────────────────────────────┼────────────────────────────────────── │
│  Session hijacking          │  JWT is signed — even if stolen,      │
│                             │  cannot be modified without           │
│                             │  secret key                           │
├─────────────────────────────┼────────────────────────────────────── │
│  Session management         │  NO session to manage                 │
│  overhead                   │  Token is self-contained              │
│                             │  Server stores nothing                │
├─────────────────────────────┼────────────────────────────────────── │
│  Distributed system         │  No session DB needed                 │
│  scalability                │  Any server validates token           │
│                             │  independently — truly scalable       │
├─────────────────────────────┼────────────────────────────────────── │
│  DB load & latency          │  No DB lookup per request             │
│                             │  Token verified in memory             │
│                             │  → zero DB overhead                   │
└─────────────────────────────┴────────────────────────────────────── │
```

---

## 🎯 Final Interview Tips from the Instructor

> **Q: What are the main disadvantages of form-based authentication?**
>
> **A:** Three main ones. First, security vulnerabilities — it's susceptible to CSRF attacks because cookies are automatically sent by the browser, and session hijacking because stealing the JSESSIONID gives full access. Second, session management overhead — tracking expiry, cleanup, and custom business rules around sessions becomes complex as the application grows. Third, scalability in distributed systems — in-memory sessions don't work across multiple server instances, so sessions must be stored in a shared DB or cache, which adds latency to every single request because each request requires multiple DB operations to validate and update the session.

> **Q: Should you disable CSRF for form-based authentication?**
>
> **A:** No. CSRF must stay enabled for form-based authentication because it uses cookies which are vulnerable to CSRF attacks. CSRF can only be safely disabled for stateless REST APIs using JWT, where no cookies or sessions are involved.

> **Q: Why is form-based authentication not suitable for microservices?**
>
> **A:** Because it's stateful — the server must maintain session state. In a microservices architecture with multiple instances behind a load balancer, each instance would need access to the same session data. This requires a shared session store (DB or Redis), which becomes a bottleneck, adds latency to every request, and introduces a single point of failure. JWT's stateless nature solves this perfectly — any instance can validate a token independently without shared state.

---

## Complete Lecture Summary

```
┌──────────────────────────────────────────────────────────────────────┐
│         FORM-BASED AUTHENTICATION — COMPLETE PICTURE                 │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  WHAT IT IS                                                          │
│  → Stateful authentication — server maintains session                │
│  → Default authentication method in Spring Boot Security             │
│  → User proves identity once → server remembers via session          │
│                                                                      │
│  LOGIN FLOW                                                          │
│  → UsernamePasswordAuthenticationFilter                              │
│  → AuthenticationManager → DaoAuthenticationProvider                 │
│  → PasswordEncoder + UserDetailsService                              │
│  → SecurityContext created → HttpSession created                     │
│  → JSESSIONID sent to client in cookie                               │
│                                                                      │
│  SUBSEQUENT REQUEST FLOW                                             │
│  → SecurityContextHolderFilter                                       │
│  → HttpSessionSecurityContextRepository                              │
│  → SecurityContext loaded into SecurityContextHolder                 │
│  → AuthorizationFilter checks roles                                  │  
│  → Controller invoked                                                │
│                                                                      │
│  CONFIGURATION                                                       │
│  → SecurityFilterChain bean                                          │
│  → permitAll() for public APIs                                       │
│  → hasRole() / hasAnyRole() for role restrictions                    │
│  → formLogin() for login page                                        │
│  → CSRF enabled always                                               │
│                                                                      │
│  SESSION CONTROLS                                                    │
│  → maximumSessions() → limit sessions per user                       │
│  → maxSessionsPreventsLogin() → block or invalidate old              │
│  → SessionCreationPolicy → IF_REQUIRED / ALWAYS /                    │
│                             NEVER / STATELESS                        │
│                                                                      │
│  DISADVANTAGES                                                       │
│  → CSRF & Session hijacking vulnerabilities                          │
│  → Session management overhead                                       │
│  → Distributed system scalability issues                             │
│  → DB load & latency per request                                     │
│                                                                      │
│  WHY IT MATTERS                                                      │
│  → Every disadvantage here = reason JWT was invented                 │
│  → Understanding this makes JWT & OAuth make complete sense          │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

That completes the full lecture on **Form-Based Authentication** from Concept & Coding. Every concept the instructor covered — from what stateful auth means, all the way through to why we need something better — is now documented with diagrams, explanations, and code. You're fully set up to understand JWT when that lecture comes next.