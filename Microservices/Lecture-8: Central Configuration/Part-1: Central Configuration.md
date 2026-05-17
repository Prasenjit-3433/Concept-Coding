# Step 1 — The Problem: Why Centralized Configuration?

---

Before jumping into the solution, the instructor wants you to feel the pain first. Because once you understand the problems, the solution makes complete sense.

---

## What is "Configuration" in a Microservice?

Every Spring Boot microservice has an `application.properties` file that lives **inside** the service itself. It holds things like:

```properties
server.port=8081
spring.application.name=order-service
db.url=jdbc:mysql://localhost:3306/order-db
custom.message=hello world
```

And your Java code reads from it like this:

```java
@Service
public class OrderService {

    @Value("${custom.message}")
    private String message;

    public String getOrder() {
        return "fetch the Order details with message: " + message;
    }
}
```

Simple and clean. But as your system grows into multiple microservices, this approach starts breaking down badly. Here are the 4 problems the instructor walks through:

---

## Problem 1 — Rebuild and Redeploy for Every Config Change

Imagine your Order service is live in production and you need to change just **one property** — say, updating a URL or a message. Here's what you have to do:

```
1. Open application.properties
2. Edit the value
3. Rebuild the JAR
4. Redeploy the service
```

Just. To. Change. One. Line.

This is a massive pain, especially in production where deployments go through pipelines, approvals, and rollout procedures.

---

## Problem 2 — Inconsistent Config Across Services

Now imagine you split your Order service into two smaller services: **Pre-Order** and **Post-Order**. Both talk to the same database, so both have this in their `application.properties`:

```
Pre-Order service:
db.url=jdbc:mysql://localhost:3306/order-db

Post-Order service:
db.url=jdbc:mysql://localhost:3306/order-db
```

Now the DB changes. You update Pre-Order... but you forget Post-Order.

```
Pre-Order service:   db.url=jdbc:mysql://localhost:3306/new-db  ✅
Post-Order service:  db.url=jdbc:mysql://localhost:3306/order-db ❌ (stale!)
```

Your two services are now pointing to **different databases**. This is a silent, dangerous bug. And in real systems, there are dozens of such shared configs — not just one.

---

## Problem 3 — No Runtime Update

This one is critical. Your `application.properties` file is **loaded only once — at startup**. After that, the running application has no awareness of any changes made to that file.

```
App starts → reads application.properties → stores values in memory
                                                      ↓
         If you change application.properties now → app has NO idea
                                                      ↓
                              Only way to reflect changes = RESTART
```

So if your app is live and handling traffic, and you need to update even a single config value — you have no choice but to bring it down, rebuild, and bring it back up.

---

## Problem 4 — Time-Consuming Rollback

This is the real-world nightmare scenario. Say you push this config change to production across multiple services:

```properties
db.url=jdbc:mysql://prod-db:3306/order-db
```

But this new DB is misconfigured. All your services start crashing. Now to fix it, here's what you have to go through **for every affected service**:

```
1. Manually edit application.properties in each service
2. Rebuild the JAR for each service
3. Redeploy each service
4. Repeat for every service that was updated
```

The more services affected, the longer production stays broken.

---

## Summary of All 4 Problems

```
┌─────────────────────────────────────────────────────────────────┐
│               Problems with Local application.properties        │
├──────┬──────────────────────────┬───────────────────────────────┤
│  #   │  Problem                 │  Pain                         │
├──────┼──────────────────────────┼───────────────────────────────┤
│  1   │  Rebuild & Redeploy      │  Every tiny change needs a    │
│      │                          │  full build + deploy cycle    │
├──────┼──────────────────────────┼───────────────────────────────┤
│  2   │  Inconsistent Config     │  Same config duplicated       │
│      │  Across Services         │  across services → easy to    │
│      │                          │  miss updates in one service  │
├──────┼──────────────────────────┼───────────────────────────────┤
│  3   │  No Runtime Update       │  Properties loaded ONCE at    │
│      │                          │  startup. Change = restart.   │
├──────┼──────────────────────────┼───────────────────────────────┤
│  4   │  Time-Consuming Rollback │  Bad config in prod = manual  │
│      │                          │  fix + rebuild + redeploy     │
│      │                          │  for EVERY affected service   │
└──────┴──────────────────────────┴───────────────────────────────┘
```

---

All 4 problems come from the same root cause: **config lives inside each microservice**. The fix is to take the config out and put it somewhere central — which is exactly what **Spring Cloud Config** does.

---
# Step 2 — The Solution Architecture (Big Picture)

---

The core idea is simple: **take the config out of each microservice and put it in one central place**. Every microservice then fetches its config from that central place at startup.

Spring Cloud Config makes this happen through a **3-layer architecture**:

---

## The 3-Layer Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                                                                     │
│   LAYER 1 — Config Files Git Repository                             │
│                                                                     │
│   ┌───────────────────────────────────────────────────────────┐     │
│   │                   Git Repository                          │     │
│   │  (just a plain git repo — no Maven, no Spring project)    │     │
│   │                                                           │     │
│   │   /global/                    /orderservice/              │     │
│   │   ├── application.properties  ├── order-service.properties│     │
│   │   ├── application-dev.props   ├── order-service-dev.props │     │
│   │   ├── application-prod.props  ├── order-service-prod.props│     │
│   │   └── application-qa.props    └── order-service-qa.props  │     │
│   └───────────────────────────────────────────────────────────┘     │
│                          │                                          │
│                          │ fetches property files                   │
│                          ▼                                          │
│   LAYER 2 — Config Server                                           │
│                                                                     │
│   ┌─────────────────────────────────────────────────────────┐       │
│   │              Config Server (port: 8888)                 │       │
│   │                                                         │       │
│   │   → A Spring Boot microservice                          │       │
│   │   → Annotated with @EnableConfigServer                  │       │
│   │   → Fetches .properties files from the Git repo         │       │
│   │   → Caches them locally                                 │       │
│   │   → Exposes REST endpoints for microservices to call    │       │
│   └─────────────────────────────────────────────────────────┘       │
│              │                    │                   │             │
│    serves config              serves config      serves config      │
│              │                    │                   │             │
│              ▼                    ▼                   ▼             │
│   LAYER 3 — Your Microservices                                      │
│                                                                     │
│   ┌───────────────┐  ┌───────────────┐  ┌───────────────┐           │
│   │ Order Service │  │Payment Service│  │  User Service │  . . .    │
│   │  (port:8080)  │  │  (port:8081)  │  │  (port:8082)  │           │
│   └───────────────┘  └───────────────┘  └───────────────┘           │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## What Each Layer Does

### Layer 1 — Config Files Git Repository
- This is just a **plain Git repository** — not a Maven project, not a Spring project. Just a folder with `.properties` files in it.
- All config for **all microservices** lives here.
- You can organize files into folders (like `/global` and `/orderservice`) to keep things clean.
- This repo can be on GitHub, GitLab, Bitbucket — anywhere.

### Layer 2 — Config Server
- This is a **Spring Boot microservice** whose only job is to talk to the Git repo and serve config to other services.
- It fetches the `.properties` files from Git, caches them, and exposes REST endpoints.
- All other microservices talk to this server — **not directly to Git**.
- Typically runs on port **8888**.

### Layer 3 — Your Microservices (Config Clients)
- These are your actual business services — Order, Payment, User, etc.
- On startup, each service contacts the Config Server and says: *"Hey, I am `order-service` running with profile `dev` — give me my properties."*
- The Config Server looks up the right files from Git and sends them back.
- The microservice loads those properties **as if they were its own local `application.properties`**.

---

## The Flow — Step by Step

```
Step 1:
You push .properties files to the Git repo
        (one-time setup, update whenever needed)
                    │
                    ▼
Step 2:
Config Server starts up
→ connects to Git repo
→ downloads all .properties files
→ caches them in memory
                    │
                    ▼
Step 3:
Order Service starts up
→ contacts Config Server at localhost:8888
→ says: "I am order-service, profile = dev"
                    │
                    ▼
Step 4:
Config Server looks up the right properties
→ returns them to Order Service
                    │
                    ▼
Step 5:
Order Service loads those properties
into its environment automatically
→ works exactly like local application.properties
```

---

## How This Solves All 4 Problems

```
┌──────┬──────────────────────────┬───────────────────────────────────┐
│  #   │  Problem                 │  How Config Server Solves It      │
├──────┼──────────────────────────┼───────────────────────────────────┤
│  1   │  Rebuild & Redeploy      │  Change the .properties in Git.   │
│      │                          │  No rebuild. No redeploy.         │
├──────┼──────────────────────────┼───────────────────────────────────┤
│  2   │  Inconsistent Config     │  One place for shared config.     │
│      │  Across Services         │  Update once → all services       │
│      │                          │  get the same value.              │
├──────┼──────────────────────────┼───────────────────────────────────┤
│  3   │  No Runtime Update       │  With @RefreshScope (coming       │
│      │                          │  later), running services can     │
│      │                          │  pick up changes without restart. │
├──────┼──────────────────────────┼───────────────────────────────────┤
│  4   │  Time-Consuming Rollback │  Bad config? Just revert the      │
│      │                          │  Git commit. All services pick    │
│      │                          │  up the fix automatically.        │
└──────┴──────────────────────────┴───────────────────────────────────┘
```

---

## One Important Thing the Instructor Emphasizes

> *"Most companies dealing with Spring Boot are following the centralized configuration approach only for managing configurations."*

This is not an advanced or optional pattern. In the real world, **this is the standard**. If you're working with microservices in a Spring ecosystem, you will encounter this setup.

---

Now that you have the full picture, we go one layer at a time starting from the bottom of the Git repo.

# Step 3 — Setting Up the Config File Git Repository

---

The instructor says this step has **two parts** that you must be crystal clear on before writing a single line of code:
1. How to structure and name your files
2. The precedence rules (he repeats multiple times — *"this is where most confusion happens"*)

---

## What Kind of Repository Is This?

This is **not** a Maven project. Not a Spring project. Just a plain Git repository with `.properties` files in it. That's it.

```
centralconfigs/          ← plain Git repo
├── global/              ← folder for common/shared config
│   ├── application.properties
│   ├── application-dev.properties
│   ├── application-prod.properties
│   └── application-qa.properties
│
└── orderservice/        ← folder for order service specific config
    ├── order-service.properties
    ├── order-service-dev.properties
    ├── order-service-prod.properties
    └── order-service-qa.properties
```

> **Note:** Creating folders is optional. You can dump all `.properties` files flat in the root. But folders help you organize things cleanly, especially when you have many microservices. If you do use folders, you'll need to tell the Config Server which folders to scan — we'll see that in Step 4.

---

## File Naming Convention — This Is Critical

The filename is how the Config Server knows **which file belongs to which service and which environment**. So you must follow this naming pattern strictly.

### Global / Common Properties
These apply to **all services** across profiles:

```
application.properties              → all services, all profiles (dev, qa, prod)
application-dev.properties          → all services, dev profile only
application-qa.properties           → all services, qa profile only
application-prod.properties         → all services, prod profile only
```

### Service-Specific Properties
These apply to **one particular service** across profiles:

```
order-service.properties            → order service only, all profiles
order-service-dev.properties        → order service only, dev profile
order-service-qa.properties         → order service only, qa profile
order-service-prod.properties       → order service only, prod profile
```

> **Very important:** The name you use here (e.g. `order-service`) must **exactly match** the `spring.application.name` you set inside that microservice's own `application.properties`. This is how the Config Server knows which files to serve to which service.

```
Inside Order microservice → spring.application.name=order-service
In Git repo               → order-service.properties ✅ (names must match exactly)
```

---

## The Precedence Rules — Read This Very Carefully

This is the part the instructor spends the most time on. He says:

> *"Precedence is very very important. This is where most of the confusion happens. If you are clear with this precedence, you would be able to properly manage the git repository for config files."*

The question is: when a service is looking for a property, **in what order does Spring Cloud Config look through the files?**

### The Rule: Profile First, Then Application, Then Global

Let's say **Order Service** is looking for a property, and it's running with **profile = dev**.

Spring Cloud Config searches in this exact order:

```
┌────────────────────────────────────────────────────────────────────┐
│        Precedence Order (Highest → Lowest)                         │
│        Example: order-service looking for config with profile=dev  │
├─────┬──────────────────────────────────┬───────────────────────────┤
│  1  │  order-service-dev.properties    │ Specific service +        │
│     │  (Central Git Repo)              │ specific profile          │
│     │                                  │ ← checked FIRST           │
├─────┼──────────────────────────────────┼───────────────────────────┤
│  2  │  application-dev.properties      │ All services +            │
│     │  (Central Git Repo)              │ specific profile (global) │
├─────┼──────────────────────────────────┼───────────────────────────┤
│  3  │  order-service.properties        │ Specific service +        │
│     │  (Central Git Repo)              │ all profiles (default)    │
├─────┼──────────────────────────────────┼───────────────────────────┤
│  4  │  application.properties          │ All services +            │
│     │  (Central Git Repo)              │ all profiles (global      │
│     │                                  │ default)                  │
├─────┼──────────────────────────────────┼───────────────────────────┤
│  5  │  application-dev.properties      │ Local to the microservice │
│     │  (Local — inside Order service)  │ specific profile          │
├─────┼──────────────────────────────────┼───────────────────────────┤
│  6  │  application.properties          │ Local to the microservice │
│     │  (Local — inside Order service)  │ all profiles              │
│     │                                  │ ← checked LAST            │
└─────┴──────────────────────────────────┴───────────────────────────┘
```

### The Key Insight: Two Rules Drive Everything

**Rule 1 — Profile has higher priority than "default for all profiles"**
Even before falling back to a service-specific default, it tries to find the same profile in the global files first.

**Rule 2 — Central Git config always wins over local config**
Local `application.properties` (the one sitting inside your microservice) is the **last resort**. It only gets used if the property is missing everywhere in the central Git repo.

---

## Walking Through the Precedence With an Example

The instructor walks through this live. Let's trace it step by step.

**Scenario:** Order Service, running with `profile=dev`, is looking for `custom.message`.

```
Step 1:
Look in order-service-dev.properties (Git)
         ↓ found? → USE IT. Stop here.
         ↓ not found? → go to step 2

Step 2:
Look in application-dev.properties (Git — global but dev profile)
         ↓ found? → USE IT. Stop here.
         ↓ not found? → go to step 3

Step 3:
Look in order-service.properties (Git — order service but all profiles)
         ↓ found? → USE IT. Stop here.
         ↓ not found? → go to step 4

Step 4:
Look in application.properties (Git — global, all services, all profiles)
         ↓ found? → USE IT. Stop here.
         ↓ not found? → go to step 5

Step 5:
Look in application-dev.properties (LOCAL — inside Order service)
         ↓ found? → USE IT. Stop here.
         ↓ not found? → go to step 6

Step 6:
Look in application.properties (LOCAL — inside Order service)
         ↓ found? → USE IT.
         ↓ not found? → property is simply missing. Error or null.
```

---

## Proving It With the Instructor's Own Test Data

The instructor fills each file with a `custom.message` property so you can see exactly which file "wins" based on precedence. Here's what he puts in each file:

```
Git Repo — global/
├── application.properties          → custom.message=Hello from global default
├── application-dev.properties      → custom.message=Hello from global dev
├── application-prod.properties     → custom.message=Hello from global prod
└── application-qa.properties       → custom.message=Hello from global qa

Git Repo — orderservice/
├── order-service.properties        → custom.message=Hello from order default
├── order-service-dev.properties    → custom.message=Hello from order dev
├── order-service-prod.properties   → custom.message=Hello from order prod
└── order-service-qa.properties     → custom.message=Hello from order qa

Local — inside Order microservice/
├── application.properties          → custom.message=Hello from local default
└── application-dev.properties      → custom.message=Hello from local dev
```

When you hit the Config Server endpoint for `order-service` with profile `dev`, it returns an **array** of all matched property sources **in precedence order**:

```json
{
  "name": "order-service",
  "profiles": ["dev"],
  "propertySources": [
    {
      "name": "order-service-dev.properties",
      "source": { "custom.message": "Hello from order dev" }
    },
    {
      "name": "application-dev.properties",
      "source": { "custom.message": "Hello from global dev" }
    },
    {
      "name": "order-service.properties",
      "source": { "custom.message": "Hello from order default" }
    },
    {
      "name": "application.properties",
      "source": { "custom.message": "Hello from global default" }
    }
  ]
}
```

The **first item in the array = highest precedence**. So `order-service` running with `dev` profile will use `"Hello from order dev"` — coming from `order-service-dev.properties`.

> The Config Server endpoint format is:
> `http://localhost:8888/{application}/{profile}/{label}`
> Example: `http://localhost:8888/order-service/dev/main`

---

## Quick Reference Table — File Naming + Scope

```
┌─────────────────────────────────────┬──────────────┬──────────────┐
│  File Name                          │  Service     │  Profile     │
├─────────────────────────────────────┼──────────────┼──────────────┤
│  application.properties             │  ALL         │  ALL         │
│  application-dev.properties         │  ALL         │  dev only    │
│  application-qa.properties          │  ALL         │  qa only     │
│  application-prod.properties        │  ALL         │  prod only   │
├─────────────────────────────────────┼──────────────┼──────────────┤
│  order-service.properties           │  order only  │  ALL         │
│  order-service-dev.properties       │  order only  │  dev only    │
│  order-service-qa.properties        │  order only  │  qa only     │
│  order-service-prod.properties      │  order only  │  prod only   │
└─────────────────────────────────────┴──────────────┴──────────────┘
```

---

## Key Takeaways from This Step

- The Git repo is **just a plain repo** with `.properties` files — no Spring, no Maven.
- File naming drives everything — **name must match `spring.application.name`** exactly.
- Folders are optional but recommended for organization. If used, tell Config Server to scan them.
- **Precedence order:** specific service + specific profile → global + specific profile → specific service default → global default → local specific profile → local default.
- The **local `application.properties`** inside your microservice is always the **last fallback**.
- Central Git config **always overrides** local config (as long as Config Server is reachable).

---

# Step 4 — Setting Up the Config Server

---

The Config Server is a **Spring Boot microservice** whose only job is to connect to the Git repo, fetch the `.properties` files, cache them, and serve them to other microservices via REST endpoints.

Let's build it from scratch.

---

## Step 4.1 — Creating the Project

Go to [https://start.spring.io](https://start.spring.io) and create a new Spring Boot project with **one dependency only**:

```
Dependency: Config Server
            (spring-cloud-config-server)
```

Your `pom.xml` should have this dependency:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-config-server</artifactId>
</dependency>
```

> This single dependency is what turns a plain Spring Boot app into a Config Server. Nothing else needed on the dependency side.

---

## Step 4.2 — Enabling the Config Server

Open your main application class and add **one annotation**: `@EnableConfigServer`

```java
@SpringBootApplication
@EnableConfigServer          // ← This is the key annotation
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
```

### What does `@EnableConfigServer` actually do?

Without this annotation, this is just a plain Spring Boot app. Adding it tells Spring:

```
"This app is a Config Server."
→ Load the .properties files from the central Git repo at startup
→ Initialize the necessary beans for config management
→ Expose REST endpoints so microservices can fetch their config
```

The endpoint it exposes follows this pattern:
```
GET http://localhost:8888/{application}/{profile}/{label}

Example:
GET http://localhost:8888/order-service/dev/main
```

---

## Step 4.3 — Configuring `application.properties`

This is where you wire the Config Server to your Git repository. Let's go through each property one by one:

### Case 1 — Public Git Repository

```properties
# Port for the Config Server
server.port=8888

# URL of your central config Git repository
spring.cloud.config.server.git.uri=https://gitlab.com/yourname/centralconfigs

# Tell Config Server which folders to scan inside the repo
# Only needed if you've organized files into folders
# If all .properties files are flat in root, skip this
spring.cloud.config.server.git.search-paths=global, orderservice

# Download all properties from Git at application startup
# If false → downloaded only when first request comes in
# Recommended: always keep true
spring.cloud.config.server.git.clone-on-start=true

# Which branch to fetch config from
# Default is 'main' even if you skip this line
# Use this if your config lives in a different branch
spring.cloud.config.server.git.default-label=main
```

### Case 2 — Private Git Repository

Most real-world repos are private. In that case you also need to provide credentials:

```properties
server.port=8888

spring.cloud.config.server.git.uri=https://gitlab.com/yourname/centralconfigs

# Credentials for private repo
# Never hardcode these values directly here!
# Always use environment variables
spring.cloud.config.server.git.username=${GIT_USERNAME}
spring.cloud.config.server.git.password=${GIT_ACCESS_TOKEN}

spring.cloud.config.server.git.search-paths=global, orderservice
spring.cloud.config.server.git.clone-on-start=true
spring.cloud.config.server.git.default-label=main
```

> The instructor is very clear here: **never hardcode your username and token directly in `application.properties`**. Always put them in environment variables.

---

## Step 4.4 — How to Generate the Access Token

Since you're using a token (not your actual password), here's how to get one:

### For GitLab:
```
Your Repo → Three dots (top right)
         → Project Settings
         → Access Tokens
         → Add New Token
         → Copy the token
```

### For GitHub:
```
Your Profile (top right)
         → Settings
         → Developer Settings
         → Personal Access Tokens
         → Fine-grained tokens
         → Generate new token
         → Copy the token
```

---

## Step 4.5 — Setting Environment Variables in IntelliJ

Since you're storing credentials in environment variables, here's how to set them in IntelliJ:

```
Run (top menu)
  → Edit Configurations
  → Environment Variables field
  → Add:

  GIT_USERNAME=your_git_username;GIT_ACCESS_TOKEN=your_token_here
```

```
┌──────────────────────────────────────────────────────────────┐
│                    IntelliJ Run Config                       │
├──────────────────────────────────────────────────────────────┤
│  Environment Variables:                                      │
│                                                              │
│  GIT_USERNAME=shrayansh8;GIT_ACCESS_TOKEN=glpat-xxxx...      │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

---

## Step 4.6 — Understanding `clone-on-start`

This one property makes a meaningful difference in behavior:

```
┌─────────────────────┬────────────────────────────────────────────┐
│  clone-on-start     │  Behavior                                  │
├─────────────────────┼────────────────────────────────────────────┤
│  true               │  Config Server downloads ALL .properties   │
│                     │  files from Git when it starts up.         │
│                     │  Caches them immediately.                  │
│                     │  First request is fast.                    │
│                     │  ✅ Recommended                             │
├─────────────────────┼────────────────────────────────────────────┤
│  false              │  Nothing downloaded at startup.            │
│                     │  Files downloaded only when the FIRST      │
│                     │  request comes in from a microservice.     │
│                     │  First request is slow.                    │
└─────────────────────┴────────────────────────────────────────────┘
```

Either way, the files get cached. The only question is **when** — at startup or at first request. Always use `true`.

---

## Step 4.7 — Full Picture of the Config Server

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Config Server                                │
│                       (port: 8888)                                  │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  ConfigServerApplication.java                               │    │
│  │                                                             │    │
│  │  @SpringBootApplication                                     │    │
│  │  @EnableConfigServer    ← turns this into a config server   │    │
│  │  public class ConfigServerApplication { ... }               │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  application.properties                                     │    │
│  │                                                             │    │
│  │  server.port=8888                                           │    │
│  │  git.uri=https://gitlab.com/yourname/centralconfigs         │    │
│  │  git.username=${GIT_USERNAME}                               │    │
│  │  git.password=${GIT_ACCESS_TOKEN}                           │    │
│  │  git.search-paths=global, orderservice                      │    │
│  │  git.clone-on-start=true                                    │    │
│  │  git.default-label=main                                     │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                          │                                          │
│              on startup, fetches & caches                           │
│                          │                                          │
│                          ▼                                          │
│         ┌─────────────────────────────────┐                         │
│         │      Git Repo (centralconfigs)  │                         │
│         │  /global   +   /orderservice    │                         │
│         └─────────────────────────────────┘                         │
│                                                                     │
│  Exposes endpoint:                                                  │
│  GET /{application}/{profile}/{label}                               │
│  e.g. GET /order-service/dev/main                                   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Step 4.8 — Testing the Config Server

Once you start the Config Server, you can hit its endpoint directly in the browser or Postman to verify it's working, **before even writing your microservices**.

```
GET http://localhost:8888/order-service/dev/main
```

The response will be a JSON array of property sources **in precedence order** (exactly as we discussed in Step 3):

```json
{
  "name": "order-service",
  "profiles": ["dev"],
  "label": "main",
  "propertySources": [
    {
      "name": "order-service-dev.properties",
      "source": { "custom.message": "Hello from order dev" }
    },
    {
      "name": "application-dev.properties",
      "source": { "custom.message": "Hello from global dev" }
    },
    {
      "name": "order-service.properties",
      "source": { "custom.message": "Hello from order default" }
    },
    {
      "name": "application.properties",
      "source": { "custom.message": "Hello from global default" }
    }
  ]
}
```

> The instructor explicitly says: **test this endpoint before moving to Step 3**. If this works, your Git repo and Config Server are wired correctly. Only then move to connecting your microservices.

---

## Complete Code Summary for Config Server

```java
// ConfigServerApplication.java

@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
```

```properties
# application.properties

server.port=8888

spring.cloud.config.server.git.uri=https://gitlab.com/yourname/centralconfigs
spring.cloud.config.server.git.username=${GIT_USERNAME}
spring.cloud.config.server.git.password=${GIT_ACCESS_TOKEN}
spring.cloud.config.server.git.search-paths=global, orderservice
spring.cloud.config.server.git.clone-on-start=true
spring.cloud.config.server.git.default-label=main
```

```xml
<!-- pom.xml dependency -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-config-server</artifactId>
</dependency>
```

---

## Key Takeaways from This Step

- Config Server is just a Spring Boot app with `@EnableConfigServer` and the `spring-cloud-config-server` dependency.
- It runs on port **8888** by convention.
- `search-paths` is only needed if you organized files into folders in the Git repo.
- `clone-on-start=true` is always recommended — downloads and caches at startup.
- For private repos, always use **environment variables** for credentials, never hardcode them.
- Test the endpoint `/{application}/{profile}/{label}` **before** wiring up microservices.

---

Ready for **Step 5 — Setting Up the Config Client (Order Microservice)**? (Dependencies, `application.properties` wiring, `optional:configserver`, profiles, and the `@Value` usage)

# Step 5 — Setting Up the Config Client (Order Microservice)

---

Now that the Git repo is ready and the Config Server is running, it's time to wire up your actual microservice — the **Config Client**. The instructor uses the Order Service as the example.

---

## Step 5.1 — Creating the Project

Go to [https://start.spring.io](https://start.spring.io) and create a new Spring Boot project with these dependencies:

```xml
<!-- This makes the service a Config Client -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>

<!-- For your REST endpoints -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

> Unlike the Config Server, the Config Client needs **no special annotation** in the main class. Just the dependency is enough. Spring Boot auto-detects it and contacts the Config Server at startup automatically.

---

## Step 5.2 — Project Structure

```
orderMicroservice/
└── src/main/
    ├── java/com/concepts/
    │   ├── controller/
    │   │   └── OrderController.java
    │   └── OrderserviceApplication.java
    └── resources/
        ├── application.properties          ← main config + config server url
        ├── application-dev.properties      ← local dev fallback
        └── application-qa.properties       ← local qa fallback
```

---

## Step 5.3 — Configuring `application.properties`

This is the most important file to get right. Let's go through every line:

```properties
# Must exactly match the filename prefix in your Git repo
# Git repo has: order-service.properties, order-service-dev.properties etc.
spring.application.name=order-service

# This tells Spring Boot where to find the Config Server
# optional:configserver → means if Config Server is down,
# don't crash. Use local properties as fallback instead.
spring.config.import=optional:configserver:http://localhost:8888

# Which environment/profile you're running in
spring.profiles.active=dev

# Local fallback — used only if Config Server is unreachable
# AND property is missing in all central Git files
custom.message=Hello from local default!
```

### The Most Important Part — `optional:configserver`

The instructor spends dedicated time explaining this prefix:

```
┌──────────────────────────────────────────────────────────────────┐
│              optional:configserver  vs  configserver             │
├───────────────────────┬──────────────────────────────────────────┤
│  optional:configserver│  If Config Server is DOWN or             │
│                       │  UNREACHABLE:                            │
│                       │  → App starts normally ✅                 │
│                       │  → Uses local application.properties     │
│                       │    as source of truth                    │
│                       │  → No crash, no failure                  │
├───────────────────────┼──────────────────────────────────────────┤
│  configserver         │  If Config Server is DOWN or             │
│  (without optional)   │  UNREACHABLE:                            │
│                       │  → App REFUSES to start ❌                │
│                       │  → Throws error at startup               │
│                       │  → Your service is completely broken     │
└───────────────────────┴──────────────────────────────────────────┘
```

> The instructor recommends always using `optional:configserver` in development. In production, whether to make it optional or mandatory is a team/architecture decision — but at least during development and testing, keep it optional so your service doesn't break just because Config Server is temporarily down.

---

## Step 5.4 — Local Fallback Properties

Inside the Order microservice, you also maintain local `.properties` files. These are the **last resort** fallback (as we discussed in Step 3 precedence rules):

```properties
# application.properties (local — inside order microservice)
spring.application.name=order-service
spring.config.import=optional:configserver:http://localhost:8888
spring.profiles.active=dev
custom.message=Hello from local default!
```

```properties
# application-dev.properties (local — inside order microservice)
custom.message=Hello from local dev!
```

```properties
# application-qa.properties (local — inside order microservice)
custom.message=Hello from local qa!
```

---

## Step 5.5 — Writing the Controller

This is where you actually **use** the config property fetched from the central Git repo:

```java
// OrderController.java

@RestController
@RequestMapping("/orders")
public class OrderController {

    // Spring injects the value of custom.message here
    // It could come from Git repo (via Config Server)
    // or from local application.properties (fallback)
    // The code doesn't care either way — it just works
    @Value("${custom.message}")
    private String message;

    @GetMapping("/fetch")
    public String getOrders() {
        return "fetched orders and message: " + message;
    }
}
```

> Notice there is **nothing special** about how you use the config value. `@Value` works exactly the same way whether the property came from the local file or from the central Git repo via Config Server. That's the beauty of this setup — your business code is completely unaware of where the config came from.

---

## Step 5.6 — The Full Startup Flow

Here's exactly what happens when you start the Order Service:

```
Order Service starts up
        │
        ▼
Reads application.properties
→ sees spring.config.import=optional:configserver:http://localhost:8888
→ sees spring.application.name=order-service
→ sees spring.profiles.active=dev
        │
        ▼
Contacts Config Server at localhost:8888
→ sends: "I am order-service, profile=dev"
        │
        ├── Config Server is UP?
        │         │
        │         ▼
        │   Config Server fetches from Git repo
        │   → searches in precedence order:
        │     1. order-service-dev.properties    (Git)
        │     2. application-dev.properties      (Git)
        │     3. order-service.properties        (Git)
        │     4. application.properties          (Git)
        │   → returns all matched property sources
        │         │
        │         ▼
        │   Order Service loads these properties
        │   into its environment automatically
        │   (as if they were local properties)
        │
        └── Config Server is DOWN?
                  │
                  ▼
            optional: prefix saves the day
            → App starts normally
            → Falls back to local properties:
              1. application-dev.properties  (local)
              2. application.properties      (local)
```

---

## Step 5.7 — Testing the Order Service

Start both the Config Server (port 8888) and the Order Service (port 8080), then hit:

```
GET http://localhost:8080/orders/fetch
```

Response:
```
fetched orders and message: Hello from order dev!
```

This value `"Hello from order dev!"` came from `order-service-dev.properties` in the central Git repo — the **highest precedence** file for this service and profile combination.

---

## Step 5.8 — Full Picture of Config Client

```
┌──────────────────────────────────────────────────────────────────────┐
│                     Order Microservice (port: 8080)                  │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │  application.properties                                        │  │
│  │                                                                │  │
│  │  spring.application.name=order-service  ← must match Git       │  │
│  │  spring.config.import=                                         │  │
│  │    optional:configserver:http://localhost:8888                 │  │
│  │  spring.profiles.active=dev                                    │  │
│  │  custom.message=Hello from local default! ← last resort        │  │
│  └────────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │  OrderController.java                                          │  │
│  │                                                                │  │
│  │  @Value("${custom.message}")  ← works regardless of source     │  │
│  │  private String message;                                       │  │
│  │                                                                │  │
│  │  GET /orders/fetch                                             │  │
│  │  → returns "fetched orders and message: Hello from order dev!" │  │
│  └────────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  On startup → contacts Config Server at localhost:8888               │
│             → fetches properties for order-service + dev profile     │
│             → loads them into environment automatically              │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
                              │
                    contacts at startup
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    Config Server (port: 8888)                        │
└──────────────────────────────────────────────────────────────────────┘
                              │
                   fetches & serves config
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────────┐
│                Central Git Repo (centralconfigs)                     │
│   /global + /orderservice — all .properties files live here          │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Complete Code Summary for Config Client

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

```java
// OrderserviceApplication.java
// No special annotation needed here!

@SpringBootApplication
public class OrderserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderserviceApplication.class, args);
    }
}
```

```java
// OrderController.java

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Value("${custom.message}")
    private String message;

    @GetMapping("/fetch")
    public String getOrders() {
        return "fetched orders and message: " + message;
    }
}
```

```properties
# application.properties
spring.application.name=order-service
spring.config.import=optional:configserver:http://localhost:8888
spring.profiles.active=dev
custom.message=Hello from local default!
```

```properties
# application-dev.properties
custom.message=Hello from local dev!
```

```properties
# application-qa.properties
custom.message=Hello from local qa!
```

---

## Key Takeaways from This Step

- Config Client needs only `spring-cloud-starter-config` dependency — **no annotation** needed in main class.
- `spring.application.name` must **exactly match** the filename prefix in the Git repo.
- `spring.config.import=optional:configserver:http://localhost:8888` — the `optional:` prefix is what prevents your service from crashing when Config Server is down.
- `@Value` works exactly the same whether the property comes from Git or local files — **your business code stays clean**.
- Local `application.properties` inside the microservice is always the **last fallback**.
- Always **start the Config Server first**, then start your microservices.

---
# 🎯Step 6 — Dynamic Refresh (Actuator + @RefreshScope)

---

At this point, everything works. But there's still one problem unsolved from our original list:

> *"Our Order Service is running. Someone updates the central config Git repo. Does the Order Service automatically pick up the change? Do I need to restart it every time?"*
>

The instructor's answer:

> *"NO — you don't need to restart. There is a concept called **@RefreshScope**, which can load properties dynamically without restarting the service. But to understand @RefreshScope first, we need to understand **Actuator**."*
>

So let's do exactly that — Actuator first, then RefreshScope.

---

## Part A — Spring Boot Actuator

---

### What is Actuator?

Actuator is a Spring Boot library that **exposes management and monitoring endpoints** for your running application. Think of it as a window into your live application — you can ask it things like:

```
→ Is this service healthy?
→ What properties is it currently running with?
→ What beans are loaded?
→ Can you reload the config without restarting?
```

All of this happens via **HTTP endpoints** that Actuator exposes automatically once you add its dependency.

---

### Adding Actuator to the Order Service

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Once added, Actuator exposes endpoints under `/actuator`. By default, only `/actuator/health` is exposed over HTTP. You need to explicitly enable others.

---

### Exposing the Actuator Endpoints

In your `application.properties`, add:

```
# Expose all actuator endpoints over HTTP
# '*' means expose everything
# You can also selectively expose: health,info,refresh
management.endpoints.web.exposure.include=*
```

Now you can hit endpoints like:

```
GET http://localhost:8080/actuator/health
→ tells you if the service is up

GET http://localhost:8080/actuator/env
→ shows ALL properties currently loaded in the environment
  (including ones fetched from Config Server)

POST http://localhost:8080/actuator/refresh
→ THIS is the key one — triggers a config reload
  without restarting the service
```

---

### The `/actuator/env` Endpoint — Very Useful for Debugging

```
GET http://localhost:8080/actuator/env
```

This shows you exactly which properties are loaded and **where they came from**:

```json
{
  "propertySources": [
    {
      "name": "configserver:order-service-dev.properties",
      "properties": {
        "custom.message": {
          "value": "Hello from order dev!"
        }
      }
    },
    {
      "name": "configserver:application.properties",
      "properties": {
        "custom.message": {
          "value": "Hello from global default!"
        }
      }
    },
    {
      "name": "applicationConfig: [classpath:/application.properties]",
      "properties": {
        "custom.message": {
          "value": "Hello from local default!"
        }
      }
    }
  ]
}
```

> This is incredibly useful during debugging — you can see at a glance exactly which value is active and which file it came from.
>

---

## Part B — @RefreshScope

---

### The Problem @RefreshScope Solves

Even with Actuator added, if you update a property in the Git repo and hit `/actuator/refresh`, the **beans that were already created at startup still hold the old value**. This is because Spring creates beans once at startup and injects values at that point.

```
App starts
    → Spring creates OrderController bean
    → Injects custom.message = "Hello from order dev!"
    → Bean is now FIXED with this value in memory

Someone updates Git repo
    → custom.message = "Hello from order dev UPDATED!"

Without @RefreshScope:
    → OrderController still holds "Hello from order dev!"
    → Change is NOT reflected ❌

With @RefreshScope:
    → POST /actuator/refresh
    → Spring RECREATES the bean
    → New value "Hello from order dev UPDATED!" is injected ✅
```

---

### How @RefreshScope Works

`@RefreshScope` is a special Spring scope. When you annotate a bean with it, you're telling Spring:

> *"This bean's properties can change at runtime. When a refresh is triggered, destroy this bean and recreate it with the latest property values."*
>

---

### Applying @RefreshScope

Add it to any class that uses `@Value` properties that you want to be refreshable:

```java
// OrderController.java

@RestController
@RequestMapping("/orders")
@RefreshScope                           // ← Add this annotation
public class OrderController {

    @Value("${custom.message}")
    private String message;

    @GetMapping("/fetch")
    public String getOrders() {
        return "fetched orders and message: " + message;
    }
}
```

That's the only code change needed. One annotation.

---

### The Complete Dynamic Refresh Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│              Dynamic Config Refresh — Full Flow                     │
└─────────────────────────────────────────────────────────────────────┘

Step 1: Order Service is running
        custom.message = "Hello from order dev!"
                │
                ▼
Step 2: Someone updates Git repo
        custom.message = "Hello from order dev UPDATED!"
                │
                ▼
Step 3: POST http://localhost:8080/actuator/refresh
        (triggered manually, or via webhook automatically)
                │
                ▼
Step 4: Actuator triggers the refresh mechanism
        → Contacts Config Server at localhost:8888
        → Config Server fetches latest files from Git
        → Returns updated properties
                │
                ▼
Step 5: @RefreshScope beans are DESTROYED and RECREATED
        → OrderController bean is recreated
        → New value injected: "Hello from order dev UPDATED!"
                │
                ▼
Step 6: GET http://localhost:8080/orders/fetch
        → returns "fetched orders and message: Hello from order dev UPDATED!"
        ✅ No restart needed
```

---

### Triggering Refresh Automatically With Git Webhooks

Manually hitting `/actuator/refresh` every time someone updates the Git repo is not practical in production. The real-world solution is to use **Git Webhooks**:

```
┌──────────────────────────────────────────────────────────────────┐
│                    Git Webhook Flow                              │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Developer pushes to Git repo                                    │
│          │                                                       │
│          ▼                                                       │
│  Git automatically sends a POST request (webhook)                │
│  to your Config Server or a Spring Cloud Bus endpoint            │
│          │                                                       │
│          ▼                                                       │
│  Refresh is triggered across ALL microservices                   │
│  automatically — no manual step needed                           │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

> The instructor mentions this briefly and notes that **Spring Cloud Bus** (with RabbitMQ or Kafka) is the production-grade way to broadcast refresh events to all microservices at once. That's a separate topic but worth knowing it exists.
>

---

## Complete Code Summary for Dynamic Refresh

```xml
<!-- pom.xml — add actuator dependency -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

```yaml
# application.properties — expose actuator endpoints
spring.application.name=order-service
spring.config.import=optional:configserver:http://localhost:8888
spring.profiles.active=dev
custom.message=Hello from local default!

# Expose all actuator endpoints
management.endpoints.web.exposure.include=*
```

```java
// OrderController.java — add @RefreshScope

@RestController
@RequestMapping("/orders")
@RefreshScope                    // ← only change needed in Java code
public class OrderController {

    @Value("${custom.message}")
    private String message;

    @GetMapping("/fetch")
    public String getOrders() {
        return "fetched orders and message: " + message;
    }
}
```

```
# Trigger refresh manually via Postman or curl
POST http://localhost:8080/actuator/refresh
Content-Type: application/json
```

---

## The Complete End-to-End Architecture — Final Picture

```
┌─────────────────────────────────────────────────────────────────────┐
│                                                                     │
│  LAYER 1 — Central Config Git Repo                                  │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │  /global/                     /orderservice/                  │  │
│  │  application.properties       order-service.properties        │  │
│  │  application-dev.properties   order-service-dev.properties    │  │
│  │  application-prod.properties  order-service-prod.properties   │  │
│  └───────────────────────────────────────────────────────────────┘  │
│            │                                                        │
│            │ fetches on startup + on refresh                        │
│            ▼                                                        │
│  LAYER 2 — Config Server (port: 8888)                               │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │  @EnableConfigServer                                          │  │
│  │  → caches all .properties files                               │  │
│  │  → exposes GET /{application}/{profile}/{label}               │  │
│  └───────────────────────────────────────────────────────────────┘  │
│            │                                                        │
│            │ serves config at startup                               │
│            │ serves updated config on refresh trigger               │
│            ▼                                                        │
│  LAYER 3 — Microservices (Config Clients)                           │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐      │
│  │  Order Service  │  │ Payment Service │  │   User Service  │      │
│  │  port: 8080     │  │  port: 8081     │  │   port: 8082    │      │
│  │                 │  │                 │  │                 │      │
│  │ @RefreshScope   │  │ @RefreshScope   │  │ @RefreshScope   │      │
│  │ @Value injects  │  │ @Value injects  │  │ @Value injects  │      │
│  │ config values   │  │ config values   │  │ config values   │      │
│  │                 │  │                 │  │                 │      │
│  │ POST            │  │ POST            │  │ POST            │      │
│  │ /actuator/      │  │ /actuator/      │  │ /actuator/      │      │
│  │ refresh         │  │ refresh         │  │ refresh         │      │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘      │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## How All 4 Original Problems Are Now Fully Solved

```
┌──────┬──────────────────────────┬───────────────────────────────────┐
│  #   │  Problem                 │  Solution                         │
├──────┼──────────────────────────┼───────────────────────────────────┤
│  1   │  Rebuild & Redeploy      │  Update Git repo only.            │
│      │                          │  POST /actuator/refresh.          │
│      │                          │  No rebuild. No redeploy. ✅       │
├──────┼──────────────────────────┼───────────────────────────────────┤
│  2   │  Inconsistent Config     │  One Git repo for all services.   │
│      │  Across Services         │  Shared config in global/         │
│      │                          │  application.properties.          │
│      │                          │  Update once → all services       │
│      │                          │  get it. ✅                        │
├──────┼──────────────────────────┼───────────────────────────────────┤
│  3   │  No Runtime Update       │  @RefreshScope + Actuator.        │
│      │                          │  Running service picks up         │
│      │                          │  latest config on refresh. ✅      │
├──────┼──────────────────────────┼───────────────────────────────────┤
│  4   │  Time-Consuming Rollback │  Revert the Git commit.           │
│      │                          │  POST /actuator/refresh.          │
│      │                          │  All services updated. ✅          │
└──────┴──────────────────────────┴───────────────────────────────────┘
```

---

## Key Takeaways from This Step

- **Actuator** exposes management endpoints for your running service — health, env, refresh and more.
- `management.endpoints.web.exposure.include=*` exposes all endpoints over HTTP.
- **`/actuator/env`** is very useful for debugging — shows all active properties and exactly which file each came from.
- **`@RefreshScope`** tells Spring to recreate the bean when a refresh is triggered — so the new property value gets injected.
- **`POST /actuator/refresh`** is what actually triggers the reload — no restart needed.
- In production, Git **webhooks** + **Spring Cloud Bus** automate the refresh broadcast to all services at once.
- Without `@RefreshScope`, even after hitting `/actuator/refresh`, the old value stays in memory.

---

## Interview Tips the Instructor Implicitly Covers

- **"Why use centralized config?"** — Talk about all 4 problems: rebuild/redeploy, inconsistency, no runtime update, slow rollback.
- **"What is Spring Cloud Config?"** — 3-layer architecture: Git repo → Config Server → Microservices.
- **"What is the precedence order in Spring Cloud Config?"** — This is a very common interview question. Walk through the 6-level precedence from Step 3.
- **"How do you update config without restarting a microservice?"** — Actuator + `@RefreshScope` + `POST /actuator/refresh`.
- **"What is `optional:configserver`?"** — Makes Config Server dependency non-fatal. Service starts even if Config Server is down, using local properties as fallback.
- **"What is `@EnableConfigServer`?"** — Turns a Spring Boot app into a central config server, initializes necessary beans, and exposes config endpoints.
- **"How would you handle config refresh across 50 microservices?"** — Spring Cloud Bus with RabbitMQ or Kafka broadcasts the refresh event to all services at once via a single trigger.

---

That completes the full lecture on **Centralized Configuration with Spring Cloud Config**. Here's a quick map of everything covered:

```
Step 1 → The Problem        (why local application.properties breaks down)
Step 2 → Big Picture        (3-layer architecture)
Step 3 → Git Repo Setup     (naming conventions + precedence rules)
Step 4 → Config Server      (@EnableConfigServer + application.properties)
Step 5 → Config Client      (spring-cloud-starter-config + optional:configserver)
Step 6 → Dynamic Refresh    (Actuator + @RefreshScope + /actuator/refresh)
```