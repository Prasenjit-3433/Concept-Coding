# Spring Boot `@Profile` Annotation — Study Notes

## How we'll go through this

I'll walk you through this lecture in **4 steps**, closely following how the instructor built up the concept:

1. **The Problem** — Why do we even need profiles? (The real-world pain point)
2. **What Profiling is** — How Spring Boot solves that problem with multiple `application.properties` files
3. **How to activate a profile** — Setting profiles statically and dynamically
4. **The `@Profile` annotation** — What it does, how it works, and the important interview distinction at the end

---

Let's start with **Step 1**.

---

## Step 1 — The Problem: Same Code, Different Environments

### The scenario

Imagine you have written a Spring Boot application. This application connects to a database. Now, you don't just run this application in one place — you run it in **multiple environments**:

```
Your Laptop (Dev)   →   QA / Stage Machine   →   Live / Production Server
```

Each of these environments needs to connect to **its own database**, with **its own credentials**. So the username and password are different in each environment.

```
┌─────────────────────────────────────────────────────────────┐
│                  Same Application Code                      │
├───────────────┬──────────────────┬──────────────────────────┤
│  Dev/Local    │   QA / Stage     │   Live / Production      │
│               │                  │                          │
│ user: devUser │ user: qaUser     │ user: prodUser           │
│ pass: devPass │ pass: qaPass     │ pass: prodPass           │
│               │                  │                          │
│     ↓         │       ↓          │         ↓                │
│    [DB]       │      [DB]        │        [DB]              │
└───────────────┴──────────────────┴──────────────────────────┘
```

### But username/password is just ONE example

The instructor is very clear here — don't fixate on just credentials. There are **many configurations** that differ across environments:

```
┌────────────────────────────────────────────────────┐
│         Configurations that differ per env         │
├──────────────────────┬─────────────────────────────┤
│ Config               │ Why it differs              │
├──────────────────────┼─────────────────────────────┤
│ DB username/password │ Each env has its own DB     │
│ URL & Port number    │ Services run on diff ports  │
│ Connection timeout   │ Prod is faster, dev is slow │
│ Request timeout      │ Same reason                 │
│ Throttle values      │ Prod handles more load      │
│ Retry values         │ More retries in dev than    │
│                      │ prod, since prod is stable  │
└──────────────────────┴─────────────────────────────┘
```

### So what's the actual problem?

You put all configurations inside `application.properties`. But there's **only one** such file. How do you manage **three different sets of configurations** in a single file for three different environments?

```
application.properties  ← Only ONE file, but THREE environments need 
                           THREE different configurations. 
                           How??
```

## Step 2 — What is Profiling in Spring Boot?

### The Core Idea

Spring Boot lets you create **multiple `application.properties` files** — one for each environment. Each of these files is called a **profile**.

> 💡 **In Spring Boot, a profile = an environment.**

### How to name these files

The naming convention is fixed:

```
application-{profileName}.properties
```

So for three environments, you'd have:

```
📁 src/main/resources/
│
├── application.properties              ← Default (fallback)
├── application-dev.properties          ← Dev / Local environment
├── application-qa.properties           ← QA / Stage environment
└── application-prod.properties         ← Live / Production environment
```

### What goes inside each file?

Each file holds **only the configuration relevant to that environment**:

```
application.properties          application-dev.properties
─────────────────────           ──────────────────────────
username=defaultUsername        username=devUsername
password=defaultPassword        password=devPassword


application-qa.properties       application-prod.properties
─────────────────────────       ───────────────────────────
username=qaUsername             username=prodUsername
password=qaPassword             password=prodPassword
```

### The Parent-Child Relationship — Very Important!

The instructor explains this with a **parent-child model**, and it's crucial to understand:

```
┌──────────────────────────────────────┐
│         application.properties       │  ← PARENT (always loaded)
│         (default / fallback)         │
└──────────────────┬───────────────────┘
                   │
       ┌───────────┴────────────┐
       │  Active Profile File   │  ← CHILD (loaded on top of parent)
       │  e.g. application-qa  │
       │  .properties           │
       └────────────────────────┘
```

The rules are simple:

```
┌──────────────────────────────────────────────────────────────────┐
│                     Priority Rules                               │
├──────────────────────────────────────────────────────────────────┤
│ 1. Key exists in BOTH parent & child  → Child wins (overrides)   │
│ 2. Key exists ONLY in parent          → Parent value is used     │
│ 3. Key exists ONLY in child           → Child value is used      │
└──────────────────────────────────────────────────────────────────┘
```

**Example:**

```
application.properties          application-qa.properties
──────────────────────          ─────────────────────────
username=defaultUsername   →    username=qaUsername  ✅ child wins
password=defaultPassword   →    password=qaPassword  ✅ child wins
retryCount=3               →    (not present)        ✅ parent used
                           →    timeout=500          ✅ child used
```

### What happens if NO profile is set?

If you haven't told Spring Boot which profile to use, it simply **falls back to the default** `application.properties`:

```
No profile set?
      ↓
Spring Boot picks → application.properties (the default one)
      ↓
username = defaultUsername
password = defaultPassword
```

The instructor shows this in action — when the app starts without any profile set, the output is:

```
username: defaultUsername  password: defaultPassword
```

---

So now you understand what profiling is — multiple environment-specific property files, loaded on top of the default one.

## Step 3 — How to Activate a Profile

### The configuration key

To tell Spring Boot which profile to use, you set this property:

```
spring.profiles.active=<profileName>
```

The profile name here must **exactly match** the suffix in your properties file name. So if your file is `application-qa.properties`, the profile name is `qa`.

---

### Way 1 — Set it inside `application.properties` (Static)

The simplest way is to just add it directly in your default `application.properties`:

```
# application.properties
username=defaultUsername
password=defaultPassword
spring.profiles.active=qa        ← telling Spring Boot to use QA profile
```

Now when the app starts, Spring Boot reads this, finds `spring.profiles.active=qa`, and internally looks for `application-qa.properties`. It loads that on top of the default file.

You'll see this in the startup logs:

```
The following 1 profile is active: "qa"
username: qaUsername  password: qaPassword
```

**But there's a problem with this approach** — every time you want to switch environments, you have to manually come into this file and change the value. That's not practical for real applications. So we need a **dynamic** way.

---

### Way 2 — Pass it at runtime via Maven command (Dynamic)

You can pass the profile **while starting the application**, without touching any file:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

```
┌─────────────────────────────────────────────────────────┐
│  mvn spring-boot:run                                    │
│       -Dspring-boot.run.profiles=prod                   │
│                    │                                    │
│                    ↓                                    │
│   Spring Boot sets spring.profiles.active = prod        │
│                    │                                    │
│                    ↓                                    │
│   Picks application-prod.properties                     │
│                    │                                    │
│                    ↓                                    │
│   username: prodUsername  password: prodPassword        │
└─────────────────────────────────────────────────────────┘
```

> 💡 Even if `application.properties` has `spring.profiles.active=dev`, the **runtime command takes higher priority** and overrides it.

```
Priority order:
─────────────────────────────────────────────────
Runtime command flag   >   application.properties
(highest priority)         (lower priority)
─────────────────────────────────────────────────
```

---

### Way 3 — Configure it in `pom.xml` using Maven Profiles (Dynamic, Cleaner)

This is the approach the instructor says is **most preferred in real/live applications** because it's cleaner and more readable.

You define named profiles inside `pom.xml`:

```xml
<profiles>

    <profile>
        <id>local</id>                              ← profile name you'll use in command
        <properties>
            <spring-boot.run.profiles>dev</spring-boot.run.profiles>   ← sets Spring profile to "dev"
        </properties>
    </profile>

    <profile>
        <id>production</id>
        <properties>
            <spring-boot.run.profiles>prod</spring-boot.run.profiles>
        </properties>
    </profile>

    <profile>
        <id>stage</id>
        <properties>
            <spring-boot.run.profiles>qa</spring-boot.run.profiles>
        </properties>
    </profile>

</profiles>
```

Then you run with:

```bash
mvn spring-boot:run -Pproduction
```

```
┌──────────────────────────────────────────────────────────────┐
│  -Pproduction                                                │
│       │                                                      │
│       ↓                                                      │
│  Maven picks the <id>production</id> block from pom.xml      │
│       │                                                      │
│       ↓                                                      │
│  Sets spring-boot.run.profiles = prod                        │
│       │                                                      │
│       ↓                                                      │
│  Spring Boot activates "prod" profile                        │
│       │                                                      │
│       ↓                                                      │
│  Picks application-prod.properties                           │
└──────────────────────────────────────────────────────────────┘
```

Both Way 2 and Way 3 do the **exact same thing** internally. The difference is just how you write it:

```
┌──────────────────┬────────────────────────────────────────────────┐
│                  │ Command                                        │
├──────────────────┼────────────────────────────────────────────────┤
│ Without pom.xml  │ mvn spring-boot:run                            │
│                  │    -Dspring-boot.run.profiles=prod             │
├──────────────────┼────────────────────────────────────────────────┤
│ With pom.xml     │ mvn spring-boot:run -Pproduction               │
│ (preferred)      │ (cleaner, named properly, easy to read)        │
└──────────────────┴────────────────────────────────────────────────┘
```

The instructor prefers the `pom.xml` approach because the command is short, clean, and the naming (`local`, `production`, `stage`) makes it very obvious what environment you're targeting.

---

### Full picture so far

```
┌─────────────────────────────────────────────────────────────────────┐
│                   How Profile Activation Works                      │
│                                                                     │
│   3 Ways to set spring.profiles.active:                             │
│                                                                     │
│   1. Inside application.properties  → static, not ideal for prod    │
│   2. -Dspring-boot.run.profiles=X   → dynamic, no pom.xml needed    │
│   3. -PmavenProfileId (pom.xml)     → dynamic, cleanest approach    │
│                                                                     │
│   Priority:  Runtime flag  >  application.properties default        │
│                                                                     │
│   If nothing is set → default application.properties is used        │
└─────────────────────────────────────────────────────────────────────┘
```

---

Now that you know what profiling is and how to activate a profile, the instructor moves on to the actual annotation — **`@Profile`**. That's Step 4, which also includes the important interview question the instructor revisits at the end. Ready?

## Step 4 — The `@Profile` Annotation & The Interview Question

### What does `@Profile` do?

So far, profiling was about **which properties file to load**. The `@Profile` annotation takes it one step further — it controls **whether a bean gets created at all**, based on the active profile.

> 💡 `@Profile` tells Spring Boot — *"Only create this bean if a specific profile is active."*

---

### How it works — with an example

The instructor creates two classes:

```java
// Only created when "prod" profile is active
@Component
@Profile("prod")
public class MySQLConnection {
    @Value("${username}")
    String username;

    @Value("${password}")
    String password;

    @PostConstruct
    public void init() {
        System.out.println("MySQL username: " + username + " password: " + password);
    }
}
```

```java
// Only created when "dev" profile is active
@Component
@Profile("dev")
public class NoSQLConnection {
    @Value("${username}")
    String username;

    @Value("${password}")
    String password;

    @PostConstruct
    public void init() {
        System.out.println("NoSQL username: " + username + " password: " + password);
    }
}
```

Now let's trace through what happens in different scenarios:

---

### Scenario 1 — Active profile is `qa`

```
spring.profiles.active = qa
        │
        ↓
Spring Boot finds MySQLConnection → @Profile("prod")
Is "prod" == "qa"? ❌ NO → Bean NOT created

Spring Boot finds NoSQLConnection → @Profile("dev")
Is "dev" == "qa"? ❌ NO → Bean NOT created

Output: Nothing printed. Neither bean was created.
```

---

### Scenario 2 — Active profile is `prod`

```
spring.profiles.active = prod
        │
        ↓
Spring Boot finds MySQLConnection → @Profile("prod")
Is "prod" == "prod"? ✅ YES → Bean IS created
Picks application-prod.properties
→ username: prodUsername, password: prodPassword

Spring Boot finds NoSQLConnection → @Profile("dev")
Is "dev" == "prod"? ❌ NO → Bean NOT created

Output:
MySQL username: prodUsername password: prodPassword
```

---

### What about setting multiple profiles at once?

The instructor also covers this — you can set **multiple profiles simultaneously**, comma-separated:

```
spring.profiles.active=prod,qa
```

**Important rules here:**

```
┌────────────────────────────────────────────────────────────────────┐
│              Multiple Profiles — What happens?                     │
├────────────────────────────────────────────────────────────────────┤
│ For @Profile matching:                                             │
│   → ALL listed profiles are considered                             │
│   → A bean is created if its profile matches ANY of them           │
│                                                                    │
│ For properties file:                                               │
│   → Only the LAST listed profile's file is picked                  │
│   → spring.profiles.active=prod,qa → picks application-qa          │
│      .properties (the last one)                                    │
└────────────────────────────────────────────────────────────────────┘
```

**Example with `spring.profiles.active=prod,qa`:**

```
MySQLConnection → @Profile("prod")
Is "prod" in [prod, qa]? ✅ YES → Bean created

NoSQLConnection → @Profile("qa")
Is "qa" in [prod, qa]? ✅ YES → Bean created

Properties file picked → application-qa.properties (last one)

Output:
MySQL username: qaUsername password: qaPassword   ← qa properties
NoSQL username: qaUsername password: qaPassword   ← qa properties
```

Both beans get created, but both use QA credentials because the QA properties file is picked (being the last in the list).

---

### The Interview Question — `@Profile` vs `@ConditionalOnProperty`

The instructor started the lecture with this question and now revisits it with full context:

> *"You have 2 applications and 1 common code base. How do you make sure a bean is only created for one application, not the other?"*

Most people answer either:
- `@ConditionalOnProperty` ✅ — **correct and intended**
- `@Profile` ⚠️ — **works, but NOT the right tool for this job**

Here's how someone might try to use `@Profile` for this:

```
Common Codebase:
─────────────────────────────
@Component
@Profile("app1")              ← bean only for app1
public class NoSQLConnection { ... }


Application 1 (application.properties):
────────────────────────────────────────
spring.profiles.active=app1   ← bean gets created ✅


Application 2 (application.properties):
────────────────────────────────────────
spring.profiles.active=app2   ← bean NOT created ✅
```

**It works. But the instructor says — if he were the code reviewer, he would NOT accept this.** Here's why:

```
┌─────────────────────────────────────────────────────────────────────┐
│              @Profile vs @ConditionalOnProperty                     │
├─────────────────────────┬───────────────────────────────────────────┤
│ @Profile                │ @ConditionalOnProperty                    │
├─────────────────────────┼───────────────────────────────────────────┤
│ Intended for            │ Intended for                              │
│ ENVIRONMENT separation  │ CONDITIONAL bean creation                 │
│ (dev, qa, prod)         │ based on config values                    │
├─────────────────────────┼───────────────────────────────────────────┤
│ Profile names like      │ Clean, purpose-built for                  │
│ "app1", "app2" create   │ controlling whether a bean                │
│ confusion — these are   │ should exist or not                       │
│ NOT environments        │                                           │
├─────────────────────────┼───────────────────────────────────────────┤
│ Anyone reading the code │ Anyone reading the code                   │
│ expects profile names   │ immediately understands                   │
│ to be environments      │ the intent                                │
└─────────────────────────┴───────────────────────────────────────────┘
```

The instructor puts it simply:

> *"Profile is for environment separation. When you name profiles `app1` and `app2`, it creates confusion — nobody expects profile names to be application names. Use `@ConditionalOnProperty` for conditional bean creation. That's what it's built for."*

---

### Complete Mental Model — Everything together

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Spring Boot Profiling — Full Picture             │
│                                                                     │
│  PROBLEM: Same code, different config needed per environment        │
│                                                                     │
│  SOLUTION: Profiling                                                │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │  application.properties        ← default, always loaded  │       │
│  │  application-dev.properties    ← dev environment         │       │
│  │  application-qa.properties     ← QA environment          │       │
│  │  application-prod.properties   ← production environment  │       │
│  └──────────────────────────────────────────────────────────┘       │
│                                                                     │
│  ACTIVATE a profile (3 ways):                                       │
│  1. spring.profiles.active=qa  in application.properties            │
│  2. mvn spring-boot:run -Dspring-boot.run.profiles=prod             │
│  3. mvn spring-boot:run -Pproduction  (via pom.xml — preferred)     │
│                                                                     │
│  @Profile ANNOTATION:                                               │
│  → Controls whether a BEAN is created based on active profile       │
│  → Spring Boot checks: does active profile match @Profile value?    │
│  → Match ✅ → Bean created                                           │
│  → No match ❌ → Bean skipped                                        │
│                                                                     │
│  INTERVIEW DISTINCTION:                                             │
│  → @Profile = environment separation (dev/qa/prod)                  │
│  → @ConditionalOnProperty = conditional bean creation               │
│  → Don't misuse @Profile for application-specific bean control      │
└─────────────────────────────────────────────────────────────────────┘
```

---

That's the complete lecture! The instructor's core message is — understand the **purpose** of a tool before using it. `@Profile` and `@ConditionalOnProperty` can sometimes achieve the same result, but using the right tool for the right job is what separates good code from confusing code.