# Spring Boot Security — Part 1: Architecture

## Before We Start — Why Security at All?

The instructor begins by grounding the whole topic in a very simple question: **why do we even need security in our application?**

The answer is attacks. Real, damaging attacks that can happen to any web application:

```
Types of Attacks (recap from a previous video):
┌─────────────────────────────────────────────────────────────┐
│  CSRF   → Cross-Site Request Forgery                        │
│  CORS   → Cross-Origin Resource Sharing (misuse)            │
│  SQLi   → SQL Injection                                     │
│  XSS    → Cross-Site Scripting                              │
└─────────────────────────────────────────────────────────────┘
```

The instructor says: *"If you haven't seen that video, please watch it first — it explains all these attacks in detail, along with CORS."* The point is — once you understand what can go wrong, you'll understand **why** we need to protect our resources.

---

## The Two Pillars of Protection

To protect our resources from these attacks, we need two things to be properly handled:

```
┌──────────────────────────────────────────────────────────────────┐
│                  TWO PILLARS OF SECURITY                         │
├────────────────────────┬─────────────────────────────────────────┤
│   AUTHENTICATION       │   AUTHORIZATION                         │
│                        │                                         │
│   "Verify WHO you are" │   "Check WHAT you are allowed to do"    │
│                        │                                         │
│   e.g. Username +      │   e.g. You're logged in, but you can    │
│   Password login       │   only VIEW — not UPDATE or DELETE      │
└────────────────────────┴─────────────────────────────────────────┘
```

**In the instructor's own words:**
- *"Authentication means — verify who you are. I provide my username and password and say, yes this is me."*
- *"Authorization is — check what you are allowed to do. Maybe I am authenticated, but I am only allowed to view, not allowed to update."*

Both of these must be properly handled in your application. That's exactly where **Spring Boot Security** comes in.

---

## Step 1 — The Base: How a Request Flows (Without Security)

The instructor builds on what was already taught in **Video #18 (Filters vs Interceptors)**. Here's the normal flow of a request in a Spring Boot app — before security enters the picture:

```
Normal Request Flow (no security):

  HTTP Request
       │
       ▼
┌─────────────┐
│   Tomcat    │  ← Servlet Container
│ (Servlet    │
│ Container)  │
└──────┬──────┘
       │
       ▼
┌─────────────────────────┐
│      Filter Chain       │
│  ┌────────┐             │
│  │Filter 1│             │
│  └───┬────┘             │
│      │                  │
│  ┌───▼────┐             │
│  │Filter 2│             │
│  └───┬────┘             │
│      │                  │
│  ┌───▼────┐             │
│  │Filter N│             │
│  └───┬────┘             │
└──────┼──────────────────┘
       │
       ▼
┌──────────────────┐
│ Dispatcher       │
│ Servlet          │
└──────┬───────────┘
       │
       ▼
┌──────────────────┐
│  Interceptors    │
└──────┬───────────┘
       │
       ▼
┌──────────────────┐
│   Controller     │  ← Your business logic / APIs live here
└──────────────────┘
```

This is the plain, un-secured flow. The instructor says — Spring Security is just an **extension** of this. It slots itself right into the Filter Chain.

---
# 🎯Step 2 — Where Does Spring Security Fit In?

---

The instructor makes this very clear and simple: **Spring Security is NOT something separate or magical. It just adds its own set of filters into your existing Filter Chain.**

When you add the Spring Security dependency to your project, Spring Boot automatically inserts a **Security Filter Chain** as one of the filters in your existing filter chain. That's it. Nothing more, nothing less at the outer level.

![image.png](attachment:74cced07-23f5-42f0-a736-841228e35295:image.png)

![image.png](attachment:479e23c6-a97f-4d06-8963-1b6e660d2b13:image.png)

So the big picture idea is — your Filter Chain already existed. Spring Security just **inserts itself as one of those filters**, but internally it is itself a chain of multiple security-specific filters.

---

## Step 3 — Not All Security Filters Run Every Time

This is an important nuance the instructor highlights. Inside the Security Filter Chain, there are **multiple filters** — but **not all of them run for every request.**

Which filters run depends on **which authentication method you've chosen** for your application.

```
Authentication Method → Which Filters Get Invoked:

┌────────────────────────────┬─────────────────────────────────────────┐
│  **Authentication Method** │  **Filters that get invoked**           │
├────────────────────────────┼─────────────────────────────────────────┤
│  Form Login (Stateful)     │  UsernamePasswordAuthenticationFilter   │
│                            │  + session-related filters              │
├────────────────────────────┼─────────────────────────────────────────┤
│  Basic Authentication      │  BasicAuthenticationFilter only         │
│  (Stateless)               │  (form login filter is SKIPPED)         │
├────────────────────────────┼─────────────────────────────────────────┤
│  JWT (Stateless)           │  Custom JWT Filter                      │
│                            │  (others are SKIPPED)                   │
├────────────────────────────┼─────────────────────────────────────────┤
│  OAuth2                    │  OAuth2-specific filters                │
│                            │  (others are SKIPPED)                   │
└────────────────────────────┴─────────────────────────────────────────┘
```

The instructor's exact point: *"It's not like each request goes through this filter, then this, then this, then this — no. If you are using Basic Authentication, this will be skipped, this will be skipped, only the relevant one will get executed."*

We will cover each authentication method's specific filters in detail later. For now — just understand this selection principle.

---

## Step 4 — The Full Internal Flow Inside the Security Filter Chain

![image.png](attachment:ca283756-e8d0-4b9c-a42c-6f4ba4b03aad:image.png)

Now the instructor walks through **what actually happens** once a request enters the Security Filter Chain. This is the core architecture. Let's go step by step exactly as he teaches it.

```
FULL SECURITY FILTER CHAIN — Internal Flow:

  Incoming Request
        │
        ▼
┌──────────────────────────────────┐
│  **Security Filter**             │  ← One of the security filters
│  (depends on auth method)        │    gets invoked based on method
│                                  │
│  Creates **"Authentication"**    │
│  object  →  isAuthenticated      │
│             = FALSE (partial)    │
│             roles = [ ]          │
└──────────────┬───────────────────┘
               │  passes Authentication object
               ▼
┌──────────────────────────────────────────────┐
│         **Authentication Manager**           │
│                                              │
│  Interface: AuthenticationManager            │
│  Default Implementation: **ProviderManager** │
│                                              │
│  Role: BRIDGE between Filter & Provider      │
│  Knows: "This request came from filter X,    │
│          so I must delegate to Provider Y"   │
└──────────────┬───────────────────────────────┘
               │  delegates to correct provider
               ▼
┌─────────────────────────────────────────────────┐
│              **Authentication Provider**        │
│                                                 │
│  ┌─────────────────────────────────────────┐    │
│  │  **DaoAuthenticationProvider**          │    │
│  │  (for Username + Password / Form Login) │    │
│  └──────────────────┬──────────────────────┘    │
│                     │                         │
│  ┌──────────────────▼──────────────────────┐    │
│  │  **JwtAuthenticationProvider**          │    │
│  │  (for JWT based auth)                   │    │
│  └─────────────────────────────────────────┘    │
│                                                 │
│  ┌─────────────────────────────────────────┐    │
│  │  **OAuth2LoginAuthenticationProvider**  │    │
│  │  (for OAuth2)                           │    │
│  └─────────────────────────────────────────┘    │
│                                                 │
│              ... Provider N                     │
└──────────────┬──────────────────────────────────┘
               │
               ▼
  (Inside **DaoAuthenticationProvider** — example)
┌───────────────────────────────────────────────────────────┐
│                                                           │
│  Step A: Hash the incoming raw password                   │
│          via **PasswordEncoder**                          │
│                                                           │
│  Step B: Fetch existing user details                      │
│          via **UserDetailsService** (interface)           │
│                                                           │
│       ┌──────────────────────────────────────────┐        │
│       │          **UserDetailsService**          │        │
│       │                                          │        │
│       │  **InMemoryUserDetailsManager**          │        │
│       │  → stores username/password in MEMORY    │        │
│       │  → temporary, not persisted              │        │
│       │                                          │        │
│       │  **JdbcUserDetailsManager**              │        │
│       │  → stores username/password in DB        │        │
│       │  → persistent, production-suitable       │        │
│       └──────────────────────────────────────────┘        │
│                                                           │
│  Step C: Compare hashed incoming password                 │
│          with stored hashed password                      │
│          → Match? → isAuthenticated = TRUE                │
│          → No match? → isAuthenticated = FALSE            │
│                                                           │
└──────────────┬────────────────────────────────────────────┘
               │  returns **fully populated** Authentication object
               ▼
┌──────────────────────────────────────────────────┐
│         Back to **Authentication Manager**       │
│  Returns fully authenticated object to Filter    │
└──────────────┬───────────────────────────────────┘
               │
       ┌───────┴────────┐
       │                │
  Authenticated?     Not Authenticated?
  YES                NO
       │                │
       ▼                ▼
┌────────────┐    ┌───────────────────┐
│  Store in  │    │  Throw            │
│  Security  │    │  BadCredentials   │
│  Context   │    │  Exception        │
└─────┬──────┘    └───────────────────┘
      │
      ▼
Dispatcher Servlet → Interceptors → Controller
(SecurityContext is now available to the Controller too)
```

---

## Key Objects to Remember

The instructor doesn't go deep into each object right now (that happens per authentication method later), but he makes sure you know **what each piece is**:

```
┌────────────────────────┬────────────────────────────────────────────────┐
│  Component             │  What it is                                    │
├────────────────────────┼────────────────────────────────────────────────┤
│  Authentication        │  Object passed around the entire flow.         │
│  Object                │  Starts partial (isAuthenticated=false),       │
│                        │  ends fully populated (isAuthenticated=true)   │
├────────────────────────┼────────────────────────────────────────────────┤
│  AuthenticationManager │ Interface. Entry point from filter.            │
├────────────────────────┼────────────────────────────────────────────────┤
│  ProviderManager       │  Default implementation of                     │
│                        │  AuthenticationManager. Acts as a bridge.      │
├────────────────────────┼────────────────────────────────────────────────┤
│  AuthenticationProvider│ Does the actual authentication work.           │
│                        │  Different providers for different methods.    │
├────────────────────────┼────────────────────────────────────────────────┤
│  UserDetailsService    │  Interface. Loads user details from storage.   │
│                        │  Two flavors: InMemory or JDBC.                │
├────────────────────────┼────────────────────────────────────────────────┤
│  PasswordEncoder       │  Handles hashing of passwords.                 │
│                        │  Raw password is NEVER stored or compared      │
│                        │  directly — always hashed first.               │
├────────────────────────┼────────────────────────────────────────────────┤
│  SecurityContext       │  Stores the fully authenticated object.        │
│                        │  Available throughout the rest of the request. │
└────────────────────────┴────────────────────────────────────────────────┘
```

---

## Step 5 — Setting Up: Dependencies

The instructor now shows what you need to add to your project. Here are both scenarios exactly as shown in the PDF:

**If your Spring Boot project already exists — add to `pom.xml`:**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <!--
        Provides core feature like:
            @ Authentication
            @ Authorization
            @ Security filters etc.
    -->
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.session</groupId>
    <!--
        Enable persistent session management
        Using relational DB
    -->
    <artifactId>spring-session-jdbc</artifactId>
</dependency>
```

**If you are setting up a new project from scratch:**

![image.png](attachment:143f4de6-2a95-4cd9-9659-a690d3e21177:image.png)

Go to [start.spring.io](https://start.spring.io/) and add these two dependencies while creating the project:

- **Spring Web**
- **Spring Security**

And only if you want to store sessions in a relational DB, also add:

```xml
<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-jdbc</artifactId>
</dependency>
```

The instructor is clear: *"`spring-session-jdbc` is optional. It is only required if you want to maintain session-based authentication AND store it in the DB. For now, you don't need it. We will use it when we do stateful authentication."*

---

## The Big Picture — What Comes Next

The instructor closes by telling you exactly what this architecture is: **a base template.** For every authentication method going forward, you will come back to this same diagram, and trace through it — but with some steps added, some skipped, and some modified.

```
Authentication Methods to be covered (as listed in the PDF):

  1.  Form Login            → Stateful
  2.  Basic Authentication  → Stateless
  3.  JWT                   → Stateless
  4.  OAuth2
        a. Authorization Code  → Stateful or Stateless
        b. Client Credentials  → Stateless
        c. Password Grant      → Stateless
  5.  API Key Authentication → Stateless
  ...and more
```

For each one, the instructor promises to walk through:

- Which filter gets invoked, and which gets skipped
- What the Authentication object looks like at each stage
- Which provider gets delegated to
- What that provider does internally
- What gets stored in the SecurityContext
- What the Controller ultimately receives

---

That wraps up Part 1 completely. The notes follow the instructor's exact teaching flow — problem first, then the base architecture, then the internals, then setup. Ready for Part 2 whenever you share the next lecture!