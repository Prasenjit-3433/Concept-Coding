# Introduction to Spring Boot

## Why Learn This?

Before jumping into Spring Boot, we need to understand **why it exists**.
The best way to do that is to go back to where it all started — **Servlets**.

The instructor's philosophy: *"Without understanding what problem existed before, it's only partial knowledge."*

---

## Step 1 — The Foundation: Servlet & Servlet Container

### What is a Servlet?

A Servlet is simply a Java class that:
- Receives a client request (like an API call)
- Processes it
- Returns a response

Think of it as the thing that **handles your API**. Back in 2013-2015, Servlets were the foundation for building web applications. And here's an important thing to note — Spring and Spring Boot **internally use the same Servlet concept**. Servlets built the foundation, everything else is built on top of it.

---

### What is a Servlet Container?

Since there can be many Servlets in one application, something needs to **manage** all of them. That's the job of a **Servlet Container**.

**Tomcat** is the most popular Servlet Container.

Your entire application gets packaged as a **WAR file** and **deployed** into Tomcat. Tomcat then manages all your Servlets.

---

### How Did a Servlet Look in Code?

Here's a simple Servlet class:

```java
@WebServlet("/demoservletone/*")
public class DemoServlet1 extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) {

        String requestPathInfo = request.getPathInfo();

        if (requestPathInfo.equals("/")) {
            // do something
        } else if (requestPathInfo.equals("/firstendpoint")) {
            // do something
        } else if (requestPathInfo.equals("/secondendpoint")) {
            // do something
        }
    }

    @Override
    protected void doPut(HttpServletRequest request,
                         HttpServletResponse response) {
        // do something
    }
}
```

And here's a second Servlet for a different group of APIs:

```java
@WebServlet("/demoservlettwo/*")
public class DemoServlet2 extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) {
        // do something
    }

    @Override
    protected void doPut(HttpServletRequest request,
                         HttpServletResponse response) {
        // do something
    }
}
```

**Key things to notice:**

One Servlet can only have **one doGet, one doPost, one doPut, one doDelete** method.
So if you want to handle 10 different GET endpoints inside one Servlet,
you end up writing a long chain of if-else blocks inside that single `doGet()` method.
And for a completely different group of APIs, you create a whole new Servlet class.

---

### The Villain: web.xml

Now here's where things get really messy.

Every Servlet needed to be **mapped** to a URL pattern — meaning, you had to tell Tomcat:
*"Hey, if someone hits this URL, send it to this Servlet."*

All of that mapping lived in a single file called **web.xml**:

```xml
<!-- Servlet 1 configuration -->
<servlet>
    <servlet-name>DemoServlet1</servlet-name>
    <servlet-class>DemoServlet1</servlet-class>
</servlet>

<servlet-mapping>
    <servlet-name>DemoServlet1</servlet-name>
    <url-pattern>/demoservletone</url-pattern>
    <url-pattern>/demoservletone/firstendpoint</url-pattern>
    <url-pattern>/demoservletone/secondendpoint</url-pattern>
</servlet-mapping>

<!-- Servlet 2 configuration -->
<servlet>
    <servlet-name>DemoServlet2</servlet-name>
    <servlet-class>DemoServlet2</servlet-class>
</servlet>

<servlet-mapping>
    <servlet-name>DemoServlet2</servlet-name>
    <url-pattern>/demoservlettwo</url-pattern>
</servlet-mapping>
```

This is fine for two Servlets. But in a real production application,
you could easily have **hundreds of Servlets**.

This `web.xml` also held other information like **filtering** — which requests go through which filters and in what sequence. So it kept growing and growing, becoming **massive, hard to read, and nearly impossible to manage**.

---

### How the Full Servlet Flow Worked

```
Client Request
         |
         v
+-------------------------+
|        Tomcat           |
|  (Servlet Container)    |
|  our app deployed here  |
+-------------------------+
         |
         | 1. Reads web.xml to find which
         |    Servlet handles this URL
         v
+-------------------------+
|        web.xml          |
|    (Servlet Mapping)    |
+-------------------------+
         |
         | 2. Invokes the matched Servlet
         v
+-------------------------+
|        Servlet          |
|  (e.g. DemoServlet1)    |
|  doGet / doPost / etc.  |
+-------------------------+
         |
         | 3. Returns Response
         v
    Back to Client
```

**Walking through a concrete example:**

Say the client hits `GET /demoservletone/firstendpoint`

1. Request lands at **Tomcat**
2. Tomcat reads **web.xml** → sees that `/demoservletone` maps to `DemoServlet1`
3. Tomcat invokes `DemoServlet1`
4. It's a GET request → `doGet()` gets called
5. Inside `doGet()`, the if-else chain runs → `/firstendpoint` matches
6. Processing happens → Response is sent back to client

---

### Problems with Servlets (Summary)

| Problem | Description |
|---|---|
| **web.xml grows too big** | Hundreds of Servlets means a massive, unmanageable XML file |
| **Tight coupling** | Object creation is hardcoded with `new`, making unit testing very hard |
| **Messy API handling** | One Servlet = one doGet, forces ugly if-else chains for multiple endpoints |
| **Hard to unit test** | Can't mock dependencies since objects are created with `new` inside the class |

---
# Spring Framework (Spring MVC) — How it Solved Servlet Problems

## The Big Picture

Spring Framework is a family of features/tools built on top of the Servlet foundation.
Spring MVC came **before** Spring Boot, and it solved the major pain points of Servlets.

Let's go through each problem it solved, one by one.

---

## Problem 1 — web.xml was a Mess → Spring Solved it with Annotations

In Servlets, all URL mappings lived in `web.xml`, which became huge and unmanageable.

Spring Framework completely **removed the need for web.xml** and replaced it with **annotation-based configuration**.

Instead of writing XML mappings, you now write this directly on your class:

```java
@Controller
@RequestMapping("/paymentapi")
public class PaymentController {

    @GetMapping("/payment")
    public String getPaymentDetails() {
        return paymentService.getDetails();
    }
}
```

Clean, readable, and sits right next to your code. No separate XML file needed.

---

## Problem 2 — Tight Coupling → Spring Solved it with Dependency Injection (IoC)

This is the **most important concept** in Spring Framework.

### Without Dependency Injection (the Servlet way)

```java
public class Payment {

    User sender = new User();  // hardcoded object creation

    void getSenderDetails(String userID) {
        sender.getUserDetails(userID);
    }
}
```

**What's the problem here?**

Payment class is creating its own `User` object using `new`.
This creates **tight coupling** between `Payment` and `User`.

The real-world consequence of this:

Say you want to write a **unit test** for `getSenderDetails()`.
Unit testing means — you test ONLY this method, and you **mock** everything else it depends on.

But if `Payment` is creating `User` with `new` inside itself,
you **cannot mock** the `User` object. It will always create a real `User` and call its real methods.
So unit testing becomes very hard.

---

### With Dependency Injection (the Spring way)

```java
@Component
public class Payment {

    @Autowired
    User sender;   // Spring creates and injects this for you

    void getSenderDetails(String userID) {
        sender.getUserDetails(userID);
    }
}

@Component
public class User {

    public void getUserDetails(String id) {
        // do something
    }
}
```

**Two annotations doing all the heavy lifting:**

- `@Component` → tells Spring: *"Hey, you manage this class"*
- `@Autowired` → tells Spring: *"Hey, resolve and inject this dependency whenever this class is created"*

Now Spring is in charge of creating the `User` object and injecting it into `Payment`.
You are no longer doing `new User()` yourself.

**What does this give you?**

During unit testing, you can now tell Spring:
*"Don't create a real `User` object — create a mock one instead."*
And Spring will inject that mock object.
Now you can control what `sender.getUserDetails()` returns, without actually calling the real method.

This is **Dependency Injection**, and it is an implementation of **Inversion of Control (IoC)**.

> **IoC = you give control of object creation and lifecycle to Spring, instead of managing it yourself.**

> **Interview Tip:** People use IoC and Dependency Injection interchangeably.
> Technically, Dependency Injection is the **implementation** of IoC.
> Both mean the same thing in a Spring context.

---

## Problem 3 — Messy API Handling → Spring Solved it with Proper Request Mapping

In Servlets, one servlet had one `doGet()`, and if you had 10 GET endpoints,
you had 10 if-else blocks crammed inside that one method. Very ugly.

Spring MVC gives you a clean, organized way to handle this:

```java
@Controller
@RequestMapping("/paymentapi")
public class PaymentController {

    @GetMapping("/payment")
    public String getPaymentDetails() {
        // handle GET /paymentapi/payment
    }

    @GetMapping("/payment/history")
    public String getPaymentHistory() {
        // handle GET /paymentapi/payment/history
    }

    @PostMapping("/payment")
    public String createPayment() {
        // handle POST /paymentapi/payment
    }
}
```

Each endpoint gets its own method. Clean, readable, easy to understand.
No if-else chains. No cramming everything into one `doGet()`.

---

## How Spring MVC Handles a Request Internally

This is where the **DispatcherServlet** comes in.
It is also called the **Front Controller** — the first point of contact for every request.

```
Client Request
      |
      v
+-------------------------+
|        Tomcat           |
|  (Servlet Container)    |
|  our app deployed here  |
+-------------------------+
      |
      v
+-------------------------+
|   DispatcherServlet     |  <-- Front Controller
|   (First Controller)    |
+-------------------------+
      |
      | Step 1: Uses Handler Mapping (your annotations)
      |         to find which Controller to call
      v
+-------------------------+
|   Controller Class      |  e.g. PaymentController
|   (your code)           |
+-------------------------+
      |
      | Step 2: DispatcherServlet asks IoC to create
      |         an instance of the Controller
      |         and resolve all its dependencies (@Autowired)
      v
+-------------------------+
|        IoC              |
|  (creates object +      |
|   injects dependencies) |
+-------------------------+
      |
      | Step 3: The specific Controller method gets invoked
      v
+-------------------------+
|   Controller Method     |  e.g. getPaymentDetails()
|   processes request     |
+-------------------------+
      |
      | Step 4: Response
      v
   Back to Client
```

**Walking through an example:**

Say the client hits `GET /paymentapi/payment`

1. Request lands at **Tomcat**
2. Tomcat passes it to **DispatcherServlet**
3. DispatcherServlet uses **Handler Mapping** (reads your `@RequestMapping`, `@GetMapping` annotations) → finds `PaymentController`
4. DispatcherServlet asks **IoC** to create an instance of `PaymentController` and inject all its `@Autowired` dependencies
5. DispatcherServlet invokes the matched method → `getPaymentDetails()`
6. Method processes the request → Response goes back to client

---

## Other Advantages Spring Framework Brought

Spring also made it very easy to integrate with other technologies:

- **Unit Testing** → JUnit, Mockito
- **Data Access** → Hibernate, JDBC, JPA
- **Asynchronous Programming** → available
- **Caching, Messaging, Security** → all integrations available

Developers can pick and choose whatever technology fits their need,
and Spring has an integration ready for it.

---

## But Spring MVC Still Had Problems

Even though Spring MVC solved the Servlet problems,
writing a Spring MVC application still required a lot of **manual setup**:

In `pom.xml`, you had to add every dependency **separately** with its **exact version**:

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-webmvc</artifactId>
    <version>6.1.4</version>       <!-- you manage this version -->
</dependency>

<dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>servlet-api</artifactId>
    <version>2.5</version>         <!-- you manage this version -->
</dependency>

<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>4.13.2</version>      <!-- you manage this version -->
</dependency>
```

You also had to manually create these configuration classes:

**AppConfig.java** — to load Spring MVC dependencies and define component scan:
```java
@Configuration
@EnableWebMvc
@ComponentScan(basePackages = "com.conceptandcoding")
public class AppConfig {
    // add configuration here if required
}
```

**DispatcherServlet setup** — you had to manually extend and configure it:
```java
public class MyApplicationInitializer extends
        AbstractAnnotationConfigDispatcherServletInitializer {

    @Override
    protected Class<?>[] getRootConfigClasses() {
        return null;
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class[]{AppConfig.class};
    }

    @Override
    protected String[] getServletMappings() {
        return new String[]{"/"};
    }
}
```

So even for a **basic application**, you needed:
- A Controller class
- A `pom.xml` with all versions managed manually
- An `AppConfig` class
- A DispatcherServlet initializer class

This is a lot of boilerplate. And this is **exactly what Spring Boot came in to fix.**

---
