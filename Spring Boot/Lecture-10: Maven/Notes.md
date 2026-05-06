# Maven & Its Lifecycle — Lecture Notes

## How we'll approach this

I'll break the lecture into clear sections, one at a time, so each part gets the depth it deserves. Here's the plan:

1. **What is Maven & Why does it exist?** (the problem it solves)
2. **Maven Project Structure** (how it organizes your project)
3. **Understanding pom.xml** (reading it element by element)
4. **The Build Lifecycle** (all 7 phases, deep dive)
5. **Repositories — Local vs Remote** (how dependency resolution works)

---

Let's begin with **Part 1**.

---

# Part 1 — What is Maven & Why Does It Exist?

## The Problem Before Maven

Before Maven, there was a tool called **Ant**. With Ant, as a developer you had to tell it **two things** every single time:

- **What** to do
- **How** to do it

For example, if you wanted to compile your code in Ant, you'd write something like:

```xml
<project default="compile">
  <target name="compile">
    <javac destdir="{provide your destination directory}">
      <src>
        <pathelement location="src/main/java"/>
      </src>
    </javac>
  </target>
</project>
```

See what's happening here — you're telling Ant: "compile my code, use `javac`, put the output *here*." You're spelling out both *what* and *how*, every single time, for every task.

This gets tedious and error-prone very fast.

---

## Enter Maven — You Only Tell It "What"

Maven flips this. You only tell Maven **what** to do. The **how** is Maven's problem.

For example, you just say:

```
mvn compile
```

That's it. You didn't tell it to use `javac`. You didn't tell it where to put the output. Maven already knows all of that internally, through its plugins and defaults. It figures out the *how* on its own.

> **The instructor's words:** *"You only tell me what to do, how to do — that's now Maven's job."*

---

## So What Exactly is Maven?

Most people think Maven is just a **build tool** or a tool that generates a JAR file. That's a very incomplete picture.

Maven is a **Project Management Tool**. Build generation is just *one* of the things it does.

Here's what Maven helps you with:

**Build Generation** — compiles your code and packages it into a JAR/WAR file.

**Dependency Resolution** — automatically downloads the libraries (JARs) your project needs, so you don't have to manually hunt them down.

**Documentation** — can generate project documentation.

And a lot more through its plugin ecosystem.

---

## How Does Maven Know What to Do?

Maven uses something called **POM — Project Object Model**.

Every Maven project has a file called `pom.xml` sitting at the root. Whenever you run any Maven command, the very first thing Maven does is look for this `pom.xml` in your current directory and read its configuration.

```
You run: mvn install
       ↓
Maven looks for pom.xml
       ↓
Reads configuration
       ↓
Executes accordingly
```

`pom.xml` is the heart of every Maven project. Everything Maven does is driven by what's written in this file.

---

# Part 2 — Maven Project Structure

## The Standard Layout

When you create a Spring Boot project and select Maven as the build tool, Maven automatically generates a very specific folder structure. This isn't random — Maven enforces a **standard project structure** that every Maven project follows.

Here's exactly what it looks like:

```
learningspringboot/              ← your app name (root folder)
│
├── pom.xml                      ← the heart of your Maven project
│
└── src/
    ├── main/
    │   └── java/
    │       └── com/                        ← from Group ID
    │           └── conceptandcoding/       ← from Group ID
    │               └── learningspringboot/ ← from Artifact ID
    │                   └── Application.java
    │
    └── test/
        └── java/
            └── com/
                └── conceptandcoding/
                    └── learningspringboot/
                        └── ApplicationTest.java
```

---

## Breaking It Down

### The Root Folder
Named after your **app name** (whatever you typed when creating the Spring Boot project). Everything lives inside this.

### pom.xml
Sits directly inside the root folder. This is Maven's configuration file — you'll always find exactly one `pom.xml` per Maven project.

### src/main/java
This is where **all your actual application code** lives. Inside it, Maven creates a package structure based on what you provided while setting up the project:

When you created your Spring Boot project, you filled in three fields:
- **Group ID** → `com.conceptandcoding` (think of it as your company/organization name)
- **Artifact ID** → `learningspringboot` (your project/app name)
- **Version** → e.g., `0.0.1-SNAPSHOT`

Maven takes your Group ID + Artifact ID and turns them into a nested package folder structure:

```
com → conceptandcoding → learningspringboot → Application.java
```

### src/test/java
This runs **in parallel** to your main folder. It follows the exact same package structure, but this is where your **unit test cases** live.

So for every class in `main`, there's a corresponding test class in `test`:

```
Main:  ...learningspringboot/Application.java
Test:  ...learningspringboot/ApplicationTest.java
```

The instructor's point here is important — this parallel structure is not a coincidence. Maven enforces it so that test code is cleanly separated from production code, but mirrors it exactly.

---

## Why Does This Standard Structure Matter?

Because Maven's entire system — its commands, its plugins, its lifecycle — is built around **assuming** this structure exists.

When you run `mvn compile`, Maven already knows to look in `src/main/java` for your source code. You never have to tell it. That's the power of the convention — because everyone follows the same structure, Maven can make smart assumptions and you write less configuration.

> This is what the instructor means when he says Maven only needs to know **what** to do — the standard structure tells it **where** everything is.

---

## What If You Had Chosen Gradle Instead?

The instructor briefly mentions this — if you had selected **Gradle** instead of Maven when setting up your Spring Boot project, you'd get a different folder structure, one that Gradle expects. The structure is tied to the build tool you choose.

---

# Part 3 — Understanding pom.xml

## The Full Picture First

When your Spring Boot project gets downloaded, it comes with a `pom.xml` already written for you. The instructor walks through this file **element by element**. Let's do the same.

Here's the overall skeleton of what a typical Spring Boot `pom.xml` looks like:

```xml
<project xmlns="..." xmlns:xsi="..." xsi:schemaLocation="...">

    <parent> ... </parent>

    <groupId> ... </groupId>
    <artifactId> ... </artifactId>
    <version> ... </version>

    <properties> ... </properties>

    <repositories> ... </repositories>

    <dependencies> ... </dependencies>

    <build> ... </build>

</project>
```

Now let's go through each block one by one.

---

## 1. The `<project>` Tag & Schema

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.xsd">
```

This is the **root element** of your entire pom.xml. Everything else sits inside it.

The `schemaLocation` part might look intimidating, but it's simple. It points to an **XML schema** — basically a rulebook that defines:

- What elements are allowed inside `<project>`
- What elements are allowed inside each child element
- What the valid structure looks like

So if you try to write something random inside `<parent>` that isn't defined in the schema, Maven will reject it. The schema enforces correctness.

---

## 2. The `<parent>` Block

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.3</version>
</parent>
```

This is one of the most important concepts the instructor explains.

### Every pom.xml is a Child of Some Other pom.xml

In Maven, there's a **hierarchy of POMs**:

```
Super POM  (top of the hierarchy — built into Maven itself)
    ↑
spring-boot-starter-parent (pom.xml)
    ↑
Your project's pom.xml  ← this is what you see in your project
```

The `<parent>` block tells Maven: *"My project inherits configuration from this parent project."*

In your case, your project is a child of `spring-boot-starter-parent`. That parent itself may be a child of another POM, and ultimately everything traces back to the **Super POM**.

### What is the Super POM?

The Super POM is Maven's built-in base configuration. Every single pom.xml you ever write is ultimately a descendant of the Super POM, whether you define a `<parent>` or not.

```
If you define <parent>    → your POM inherits from that parent
                            (which itself inherits from Super POM)

If you don't define       → Maven automatically makes your POM
<parent> at all             inherit directly from Super POM
```

### Why Does This Matter Practically?

Look at your downloaded `pom.xml` — it's **very small**. There's barely anything in it. But your project still works perfectly, downloads dependencies, compiles, runs tests, etc.

Why? Because **it inherits a massive amount of configuration from its parent**. All the default plugin versions, dependency management, build settings — all of it comes from `spring-boot-starter-parent` without you having to write a single line.

---

## 3. The Unique Identity Block — groupId, artifactId, version

```xml
<groupId>com.conceptandcoding</groupId>
<artifactId>learningspringboot</artifactId>
<version>0.0.1-SNAPSHOT</version>
```

These three together are the **coordinates** of your project. They uniquely identify your project in the entire Maven universe — locally and on Maven Central.

| Field | Meaning | Example |
|---|---|---|
| `groupId` | Your company/organization name | `com.conceptandcoding` |
| `artifactId` | Your project/app name | `learningspringboot` |
| `version` | Current version of your project | `0.0.1-SNAPSHOT` |

Think of it like an address. If someone wants to use your JAR as a dependency in their project, they'll use exactly these three values to find it.

---

## 4. `<properties>` — Key-Value Configuration

```xml
<properties>
    <java.version>17</java.version>
</properties>
```

Properties are simply **key-value pairs** that you can define once and reuse anywhere in your pom.xml.

### How to Use a Property Elsewhere

Let's say you defined `java.version` as `17`. Now anywhere else in the pom.xml where you need that value, you reference it like this:

```xml
<version>${java.version}</version>
```

Maven sees `${java.version}`, looks it up in `<properties>`, finds the value `17`, and substitutes it in. This way, if you ever need to change your Java version, you change it in **one place only**, and it reflects everywhere.

---

## 5. `<repositories>` — Where to Download Dependencies From

```xml
<repositories>
    <repository>
        <id>central</id>
        <url>https://repo.maven.apache.org/maven2</url>
    </repository>
</repositories>
```

This tells Maven: *"When you need to download a dependency JAR, go look here."*

The URL is the remote location — typically Maven Central, which is the public repository where millions of open-source libraries live.

**Important note from the instructor:** You won't actually see this block in your downloaded `pom.xml`. Why? Because Maven Central is already configured as the default in the **Super POM**. So even without writing it, Maven always knows to go to Maven Central by default.

The instructor added it in the lecture just to show you it exists and explain what it does.

---

## 6. `<dependencies>` — What Your Project Needs

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

This is where you declare every external library your project depends on. Maven reads this list, goes to the repository URL, downloads the JARs, and makes them available to your project.

Each dependency is again identified by the same three coordinates — `groupId`, `artifactId`, and optionally `version` (if inherited from parent, you may not need to specify it).

---

## 7. `<build>` — Adding Custom Tasks to the Lifecycle

```xml
<build>
    <plugins>
        <plugin>
            ...
        </plugin>
    </plugins>
</build>
```

The instructor says this clearly: *"Actual Maven starts when you see the build element."*

But to truly understand what `<build>` does and **why** you'd need it, you first need to understand the **Maven Build Lifecycle** — which is exactly what Part 4 is about.

So the instructor intentionally saves the deep dive into `<build>` for after the lifecycle explanation. We'll do the same.

---

## The Full pom.xml Picture So Far

```
pom.xml
│
├── <project>          → root, enforces schema/structure rules
├── <parent>           → inherit config from spring-boot-starter-parent
│                        (which inherits from Super POM)
├── <groupId>          → your organization name
├── <artifactId>       → your project name      } These 3 = unique
├── <version>          → your project version   } identity of your project
├── <properties>       → key-value pairs, reusable anywhere in pom
├── <repositories>     → where to download dependency JARs from
├── <dependencies>     → list of libraries your project needs
└── <build>            → add custom tasks/plugins to lifecycle phases
                         (covered in detail in Part 4)
```

---

# 🎯Part 5 — How Dependency Resolution Works

## The Problem Maven Solves Here

Before understanding how Maven resolves dependencies, think about what life was like without it.

You'd have to:

- Manually find the JAR file for every library you need
- Download it yourself
- Add it to your project manually
- If that library itself depends on other libraries — repeat the whole process

Maven completely eliminates this. You just declare what you need in `pom.xml`, and Maven handles the rest.

---

## The Two Repositories — Revisited in Depth

The instructor builds a clear mental model here. Let's lay it out properly:

```
┌──────────────────────────────────────────────────────────┐
│                      YOUR SYSTEM                         │
│                                                          │
│   ┌──────────────────────────────────────────────────┐   │
│   │            Local Repository                      │   │
│   │            (~/.m2/repository/)                   │   │
│   │                                                  │   │
│   │  Fast access — no network needed                 │   │
│   │  Everything downloaded before lives here         │   │
│   └──────────────────────────────────────────────────┘   │
│                                                          │
└──────────────────────────────────────────────────────────┘
                           ↕
                    Network call
                           ↕
┌──────────────────────────────────────────────────────────┐
│                  OUTSIDE YOUR SYSTEM                     │
│                                                          │
│   ┌─────────────────────┐  ┌─────────────────────────┐   │
│   │   Maven Central     │  │  Company's Internal     │   │
│   │   (public)          │  │  Repository (private)   │   │
│   │                     │  │                         │   │
│   │ repo.maven.apache   │  │ your-company.repo.com   │   │
│   │      .org/maven2    │  │                         │   │
│   └─────────────────────┘  └─────────────────────────┘   │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

---

## How Maven Resolves a Dependency — Step by Step

Every time Maven encounters a dependency in your `pom.xml`, it follows a very specific lookup order:

```
Step 1: Maven reads dependency from pom.xml
            ↓
Step 2: Check LOCAL repository (~/.m2/repository/)
            ↓
    ┌───────┴────────┐
    │                │
  FOUND           NOT FOUND
    │                │
    ↓                ↓
Use it directly   Go to REMOTE repository
(no network call) (Maven Central / Company repo)
                     ↓
                 Download the JAR
                     ↓
                 Save it to LOCAL repository
                     ↓
                 Use it in the project
                     ↓
          Next time → found in LOCAL
          (no network call needed again)
```

### Why This Design Is Smart

The local repository acts as a **cache**. Once a dependency is downloaded, it never needs to be fetched again (unless the version changes). This means:

- Faster builds after the first download
- No dependency on internet connectivity for already-downloaded JARs
- Less load on remote repositories

---

## What the Local Repository Actually Looks Like

When Maven downloads a dependency, it doesn't just dump it in one folder. It creates a **structured hierarchy** based on the coordinates:

```
~/.m2/repository/
└── org/
    └── springframework/
        └── boot/
            └── spring-boot-starter-web/
                └── 3.2.3/
                    ├── spring-boot-starter-web-3.2.3.jar
                    ├── spring-boot-starter-web-3.2.3.pom
                    └── spring-boot-starter-web-3.2.3.jar.sha1
```

The path is built from:

```
groupId (dots → folders) / artifactId / version / files
```

This is the exact same structure Maven uses when you run `mvn install` to install your own project's JAR locally.

---

## Configuring Which Remote Repository to Use

By default Maven goes to Maven Central. But what if you're working in a company that has its own private repository (like Nexus or JFrog Artifactory)?

You configure this in two places:

### In pom.xml — Define the Repository

```xml
<repositories>
    <repository>
        <id>company-repo</id>
        <url>https://your-company.repo.com/repository/maven-releases</url>
    </repository>
</repositories>
```

### In settings.xml — Provide Credentials

```xml
<settings>
    <servers>
        <server>
            <id>company-repo</id>        ← must match id in pom.xml
            <username>your-username</username>
            <password>your-password</password>
        </server>
    </servers>
</settings>
```

The `id` field is what links these two together. Maven sees the repository ID in `pom.xml`, looks for a matching `<server>` entry in `settings.xml`, and uses those credentials to authenticate.

---

## The Complete Picture — settings.xml

The instructor mentions `settings.xml` multiple times across the lifecycle discussion. Let's bring it all together in one place.

`settings.xml` lives at `~/.m2/settings.xml` and controls Maven's **environment-level configuration**:

```xml
<settings>

    <!-- 1. Where your local repository is stored -->
    <localRepository>${user.home}/.m2/repository</localRepository>

    <!-- 2. Credentials for remote repositories -->
    <servers>
        <server>
            <id>remote-repository-id</id>
            <username>your-username</username>
            <password>your-password</password>
        </server>
    </servers>

    <!-- other configurations -->

</settings>
```

Think of `settings.xml` as the **personal/environment config** and `pom.xml` as the **project config**. Credentials always go in `settings.xml` — never in `pom.xml` — because `pom.xml` gets committed to version control and you don't want passwords exposed.

---

## Putting the Whole Lecture Together

Here's everything from all 5 parts in one unified flow:

```
┌─────────────────────────────────────────────────────┐
│                    pom.xml                          │
│                                                     │
│  <parent>        → inherit config from parent POM   │
│  <groupId>       →                                  │
│  <artifactId>    → unique identity of your project  │
│  <version>       →                                  │
│  <properties>    → reusable key-value config        │
│  <repositories>  → where to download JARs from      │
│  <dependencies>  → what your project needs          │
│  <build>         → custom goals for lifecycle       │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│              Maven Build Lifecycle                  │
│                                                     │
│  1. validate  → check project structure             │
│  2. compile   → .java → .class (target/classes/)    │
│  3. test      → run unit tests                      │
│  4. package   → .class → .jar (target/)             │
│  5. verify    → static analysis, integrity checks   │
│  6. install   → JAR → ~/.m2/repository/             │
│  7. deploy    → JAR → remote repository             │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│           Dependency Resolution                     │
│                                                     │
│  Check local (~/.m2/) → if missing → fetch remote   │
│  Remote = Maven Central OR company repo             │
│  Credentials for remote → stored in settings.xml    │
└─────────────────────────────────────────────────────┘
```

---

## Key Takeaways From the Entire Lecture

These are the things the instructor really wants you to walk away remembering:

**1. Maven is a project management tool, not just a build tool.** It handles builds, dependencies, documentation, and more.

**2. You only tell Maven what to do, not how.** The how is abstracted inside Maven's plugins and defaults.

**3. Every pom.xml is a child of some other pom.xml**, and ultimately a child of the Super POM at the top of the hierarchy.

**4. The build lifecycle is always sequential.** Running any phase automatically runs all previous phases first. You can never skip them.

**5. Local repository is your cache.** Maven always checks local first before going to remote. This makes repeated builds fast.

**6. `<build>` lets you plug custom goals into any phase.** Maven's phases have default goals, but you can always add more using plugins.

**7. Credentials always go in settings.xml, never pom.xml.** pom.xml is project config that gets shared; settings.xml is personal/environment config that stays on your machine.

---

That completes the full lecture notes for **Introduction to Maven and its Lifecycle**. Everything the instructor taught — from the problem Maven solves, to the project structure, to reading pom.xml, to all 7 lifecycle phases, to how dependency resolution works — is covered here in a clean, flowing format.