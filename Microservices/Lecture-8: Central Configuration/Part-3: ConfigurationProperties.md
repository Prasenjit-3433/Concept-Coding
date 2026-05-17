# Part 1 — The Problem: Why `@Value` Falls Short

---

## What is `@Value`?

Before understanding `@ConfigurationProperties`, let's first understand what problem it solves. The instructor starts here intentionally — **always understand the "why" before the "what".**

In Spring Boot, your `application.properties` file holds configuration values like database credentials, URLs, feature flags, etc. The `@Value` annotation lets you inject these individual properties directly into your Java fields.

Here's a simple example the instructor shows:

```java
@Component
public class DBConnection {

    @Value("${app.username}")
    private String username;

    @Value("${app.password}")
    private String password;

    @Value("${app.db.url}")
    private String dbUrl;
}
```

And in `application.properties`:
```properties
app.username=myUser
app.password=myPass
app.db.url=jdbc:mysql://localhost:3306/mydb
```

This works fine... **until your configuration starts growing.**

---

## The 2 Core Problems with `@Value`

### Problem 1 — Too Much Duplication

```
application.properties
───────────────────────────────────────────
app.username        → @Value("${app.username}")   → String username
app.password        → @Value("${app.password}")   → String password
app.db.url          → @Value("${app.db.url}")      → String dbUrl
app.timeout         → @Value("${app.timeout}")     → int timeout
app.retryCount      → @Value("${app.retryCount}")  → int retryCount
app.maxPool         → @Value("${app.maxPool}")     → int maxPool
...and so on
───────────────────────────────────────────
   It's always a strict 1-to-1 mapping.
   10 properties = 10 @Value annotations.
   100 properties = 100 @Value annotations.
```

The instructor makes a very important real-world point here:

> *"In big tech organizations, configuration continuously grows. A lot of things are configuration-driven. If you have to do 1-to-1 mapping every single time, just see how many places you have to repeat this annotation."*

So the first problem is **annotation clutter** — it scales very poorly.

---

### Problem 2 — No Validation Support

Let's say you have a password property coming from `application.properties`. You want to enforce:
- It must not be empty
- Its length must be between 10 and 25 characters

With `@Value`, **there is no built-in way to do this.** You'd have to manually write validation logic after injecting the value — which is error-prone, repetitive, and easy to forget.

```
                    ┌───────────────────────┐
application.props   │  @Value("${password}")│   then... manually check?
password=abc   ───► │  String password;     │ ──► if (password == null) { ... }
                    └───────────────────────┘     if (password.length() < 10) { ... }

   Ugly. Repetitive. No standard way. Easy to miss.
```

---

## Summary of Problems

```
┌────────────────────────────────────────────────────────┐
│                  @Value Limitations                    │
├────────────────────────────────────────────────────────┤
│  1. Strict 1-to-1 mapping                              │
│     → N properties = N @Value annotations              │
│     → Doesn't scale as config grows                    │
│                                                        │
│  2. No validation support                              │
│     → Can't enforce constraints on injected values     │
│     → Manual validation = error-prone & repetitive     │
└────────────────────────────────────────────────────────┘
```

---

These two problems are exactly what `@ConfigurationProperties` was designed to solve. In the next part, we'll look at what it is, how it works internally, and the full lifecycle of how Spring binds your properties file to a Java object.

---

# Part 2 — `@ConfigurationProperties` to the Rescue

---

## What is `@ConfigurationProperties`?

Instead of injecting properties one by one into individual fields, `@ConfigurationProperties` maps a **group of related properties** from `application.properties` into a **single Java object**.

The instructor puts it simply:

> *"It maps configuration from `application.properties` into a Java object — not to one field, but to a Java object."*

This gives you 3 big advantages:

```
┌─────────────────────────────────────────────────────────────┐
│            @ConfigurationProperties Advantages              │
├─────────────────────────────────────────────────────────────┤
│  1. STRUCTURED                                              │
│     Group related properties into one logical Java class.   │
│     e.g. all user-related config → UserConfiguration class  │
│                                                             │
│  2. REUSABLE                                                │
│     Once you create the Java object (bean), you can         │
│     @Autowired it anywhere in your codebase.                │
│                                                             │
│  3. VALIDATED                                               │
│     Add validation annotations directly on fields.          │
│     Spring validates BEFORE the app fully starts.           │
└─────────────────────────────────────────────────────────────┘
```

---

## How Does It Work? — The Internal Flow

This is the most important part. The instructor walks through the **exact internal sequence** of what happens when your Spring Boot app starts up. Understanding this sequence is key to everything that comes later (including the immutability interview question in Part 7).

### The 3-Step Sequence

```
  APP STARTUP
      │
      ▼
┌─────────────────────────────────────────────────┐
│  STEP 1: Spring IoC creates an EMPTY BEAN       │
│                                                 │
│  Because of @Component, Spring calls the        │
│  default no-arg constructor.                    │
│                                                 │
│  UserConfiguration {                            │
│      name   = null   ← default                  │
│      age    = 0      ← default                  │
│      active = false  ← default                  │
│  }                                              │
└────────────────────┬────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────┐
│  STEP 2: Configuration Binding kicks in         │
│                                                 │
│  It reads application.properties and calls      │
│  the SETTER METHODS on the empty bean           │
│  to fill up the fields.                         │
│                                                 │
│  setName("test_username")                       │
│  setAge(27)                                     │
│  setActive(true)                                │
│                                                 │
│  UserConfiguration {                            │
│      name   = "test_username" ← filled          │
│      age    = 27              ← filled          │
│      active = true            ← filled          │
│  }                                              │
└────────────────────┬────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────┐
│  STEP 3: Validation (if @Validated is added)    │
│                                                 │
│  Spring checks all validation annotations       │
│  on the fields AFTER binding is done.           │
│                                                 │
│  If any validation fails →                      │
│  Application REFUSES to start. ✗                │
│                                                 │
│  If all pass →                                  │
│  Bean is ready to use. ✓                        │
└─────────────────────────────────────────────────┘
```

> **Key insight from the instructor:** Configuration binding uses **setter methods** to fill up the values. This is why getter AND setter methods are required on your configuration class. Without setters, binding cannot work.

---

## The Role of `prefix`

When you annotate a class with `@ConfigurationProperties(prefix = "user")`, you are telling the configuration binder:

> *"Hey, pick up ALL properties from `application.properties` that start with `user.` and try to map them to the fields of this class."*

```
application.properties              UserConfiguration.java
──────────────────────              ──────────────────────
user.name   = test_username   ───►  String name;
user.age    = 27              ───►  int age;
user.active = true            ───►  boolean active;

   prefix = "user"
   field name must match the part AFTER the prefix.
```

The instructor also mentions an important relaxation rule:

```
application.properties        Java field name
──────────────────────        ───────────────
user.is-active        ──►    boolean isActive;   ✓ (camelCase allowed)
user.is_active        ──►    boolean isActive;   ✓ (underscore allowed)
user.isactive         ──►    boolean isActive;   ✓ (lowercase allowed)

BUT → instructor's recommendation:
"Always try to keep the same name so that mapping is 100% guaranteed.
 Because this mapping is not controlled by us — it is done by 
 Configuration Binding internally."
```

---

## The Basic Setup — Code

### `application.properties`
```properties
user.name=test_username
user.age=27
user.active=true
```

### `UserConfiguration.java`
```java
@Component
@ConfigurationProperties(prefix = "user")
public class UserConfiguration {

    private String name;
    private int age;
    private boolean active;

    // Getters and Setters — MANDATORY for binding to work
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
```

### Using it anywhere in your codebase
```java
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    UserConfiguration userConfiguration;

    @GetMapping("/")
    public void getUserConfig() {
        System.out.println("Name: " + userConfiguration.getName());
        System.out.println("Age: " + userConfiguration.getAge());
        System.out.println("Is Active: " + userConfiguration.isActive());
    }
}
```

---

## Full Picture — Side by Side

```
application.properties                  Java (Spring Boot)
──────────────────────                  ──────────────────
                              @Component
                              @ConfigurationProperties(prefix = "user")
                              public class UserConfiguration {
user.name=test_username  ───►     private String name;
user.age=27              ───►     private int age;
user.active=true         ───►     private boolean active;
                                  // getters + setters
                              }
                                        │
                                        │ @Autowired
                                        ▼
                              Inject UserConfiguration
                              anywhere you need it.
                              Use like a normal Java object.
```

---

## Why This Is Better Than `@Value`

```
┌─────────────────────┬──────────────────────────────────────────┐
│     @Value          │       @ConfigurationProperties           │
├─────────────────────┼──────────────────────────────────────────┤
│ 1 annotation per    │ 1 class for ALL related properties       │
│ property            │                                          │
├─────────────────────┼──────────────────────────────────────────┤
│ No validation       │ Built-in validation via annotations      │
│ support             │ (@NotBlank, @Min, @Max, etc.)            │
├─────────────────────┼──────────────────────────────────────────┤
│ Hard to reuse       │ Inject the bean anywhere with @Autowired │
├─────────────────────┼──────────────────────────────────────────┤
│ No logical grouping │ Group related config into one class      │
└─────────────────────┴──────────────────────────────────────────┘
```

---

Now that you understand what `@ConfigurationProperties` is and how Spring processes it internally, in the next part we'll go deeper — handling **nested/complex configuration** (objects within objects) and the very important reason why nested classes **must be static**.

---

# Part 3 — Handling Nested / Complex Configuration

---

## Why Do We Need Nested Configuration?

So far, we've only mapped flat, simple properties like `name`, `age`, `active`. But in real-world applications, your configuration is rarely this simple. You'll often have **grouped sub-properties** like:

```properties
user.address.city=myCityName
user.address.country=myCountryName
```

Here, `address` itself is a group — it has its own fields inside it. So instead of treating `city` and `country` as flat fields on `UserConfiguration`, we should model `address` as its **own object** inside `UserConfiguration`. This is called **nested configuration**.

---

## The Setup — What We Want to Achieve

```
application.properties
──────────────────────
user.name=test_username
user.age=27
user.active=true
user.address.city=myCityName
user.address.country=myCountryName
```

We want this to map to:

```
UserConfiguration
│
├── name    → "test_username"
├── age     → 27
├── active  → true
└── address (AddressConfig object)
        ├── city    → "myCityName"
        └── country → "myCountryName"
```

---

## The Code

### `UserConfiguration.java`

```java
@Component
@ConfigurationProperties(prefix = "user")
public class UserConfiguration {

    private String name;
    private int age;
    private boolean active;
    private AddressConfig address;   // ← nested object

    // Why static? — explained in detail below
    public static class AddressConfig {
        private String city;
        private String country;

        // Getters and Setters
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
    }

    // Getters and Setters for outer class
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public AddressConfig getAddress() { return address; }
    public void setAddress(AddressConfig address) { this.address = address; }
}
```

### Accessing it in the Controller

```java
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    UserConfiguration userConfiguration;

    @GetMapping("/")
    public void getUserConfig() {
        System.out.println("Name: " + userConfiguration.getName());
        System.out.println("Age: " + userConfiguration.getAge());
        System.out.println("Is Active: " + userConfiguration.isActive());
        System.out.println("City: " + userConfiguration.getAddress().getCity());
        System.out.println("Country: " + userConfiguration.getAddress().getCountry());
    }
}
```

---

## The Most Important Question — Why Must the Nested Class Be `static`?

This is something the instructor spends a lot of time on — and for good reason. It's a **common interview question** too. Let's break it down properly.

### First, understand what happens with a non-static nested class in Java

In Java, when you create a **non-static** nested class (also called an inner class), it is **always tied to an instance of its outer class.** Java automatically adds a hidden constructor that takes the outer class as a parameter.

```
Non-static nested class:          What Java adds internally:
──────────────────────            ──────────────────────────
class UserConfiguration {         class AddressConfig {
    class AddressConfig {    →        AddressConfig(UserConfiguration outer) {
        String city;                      // hidden constructor
        String country;               }
    }                             }
}

To create AddressConfig, you MUST do:
    UserConfiguration uc = new UserConfiguration();
    AddressConfig ac = uc.new AddressConfig();   ← outer instance required
```

So a non-static inner class has **no default no-arg constructor.** Its only constructor requires the outer class instance.

---

### Now, what does Configuration Binding do internally?

When the binder sees the field `AddressConfig address` inside `UserConfiguration`, here is what it tries to do:

```
Configuration Binder sees:   private AddressConfig address;
                                        │
                                        ▼
                    It uses REFLECTION to create an object of AddressConfig.
                                        │
                                        ▼
                    It looks for a NO-ARG CONSTRUCTOR to instantiate it.
                                        │
                          ┌─────────────┴─────────────┐
                          │                           │
                   NON-STATIC                      STATIC
                  nested class                  nested class
                          │                           │
              No-arg constructor               No-arg constructor
                DOES NOT EXIST                   DOES EXIST
                          │                           │
                  Reflection FAILS              Reflection SUCCEEDS
                          │                           │
                  Binding FAILS ✗              Binding WORKS ✓
                  address = null                address is filled
```

The instructor explains it clearly:

> *"When you make it non-static, the default constructor that gets added is one that takes a reference of the parent object — not a no-arg constructor. But the binder uses reflection and tries to invoke the no-arg constructor. Since that doesn't exist for a non-static class, reflection fails, binding fails, and the address field stays null."*

---

### Static nested class solves this

When you make the nested class `static`, it is **no longer tied to the outer class instance.** It becomes an independent class that just happens to live inside `UserConfiguration`. Java then adds the proper **no-arg constructor** to it.

```
Static nested class:              What Java adds internally:
────────────────────              ──────────────────────────
class UserConfiguration {         class AddressConfig {
    static class AddressConfig {      AddressConfig() { }  ← no-arg constructor ✓
        String city;              }
        String country;
    }
}

To create AddressConfig now:
    AddressConfig ac = new AddressConfig();   ← no outer instance needed ✓
```

Configuration Binder can now call `new AddressConfig()` via reflection without any issue.

---

## Full Internal Flow With Nested Configuration

```
APP STARTUP
    │
    ▼
┌──────────────────────────────────────────────────────────┐
│  STEP 1: Spring IoC creates empty UserConfiguration bean │
│  (via @Component → calls default no-arg constructor)     │
│                                                          │
│  UserConfiguration {                                     │
│      name    = null                                      │
│      age     = 0                                         │
│      active  = false                                     │
│      address = null   ← not yet created                  │
│  }                                                       │
└───────────────────────────┬──────────────────────────────┘
                            │
                            ▼
┌──────────────────────────────────────────────────────────┐
│  STEP 2: Configuration Binder kicks in                   │
│                                                          │
│  → Reads application.properties                          │
│  → Calls setName(), setAge(), setActive()                │
│                                                          │
│  → Sees field: AddressConfig address                     │
│  → Uses REFLECTION to call new AddressConfig()           │
│    (works because it's a STATIC nested class)            │
│  → Creates empty AddressConfig object                    │
│  → Calls setCity(), setCountry() on it                   │
│  → Calls setAddress(addressObj) on UserConfiguration     │
│                                                          │
│  UserConfiguration {                                     │
│      name    = "test_username"  ← filled                 │
│      age     = 27               ← filled                 │
│      active  = true             ← filled                 │
│      address = AddressConfig {  ← filled                 │
│          city    = "myCityName"                          │
│          country = "myCountryName"                       │
│      }                                                   │
│  }                                                       │
└───────────────────────────┬──────────────────────────────┘
                            │
                            ▼
┌──────────────────────────────────────────────────────────┐
│  STEP 3: Bean is ready                                   │
│  Inject UserConfiguration anywhere via @Autowired        │
│  Access address.city, address.country like normal Java   │
└──────────────────────────────────────────────────────────┘
```

---

## Interview Tip 🎯

> **Q: Why must nested classes inside a `@ConfigurationProperties` class be declared `static`?**
>
> **A:** Configuration Binding uses Java Reflection to instantiate nested types. Reflection looks for a **no-arg constructor**. A non-static inner class in Java does not have a no-arg constructor — instead, Java implicitly adds a constructor that takes the outer class instance as a parameter. Since the binder cannot satisfy this, reflection fails and binding fails, leaving the field as `null`. When the nested class is `static`, it is independent of the outer class instance and Java adds a proper no-arg constructor, allowing the binder to instantiate it successfully via reflection.

---

## Quick Rule to Remember

```
┌────────────────────────────────────────────────────────────┐
│  RULE: Any nested class used for @ConfigurationProperties  │
│  binding MUST be declared as public static class.          │
│                                                            │
│  Reason: Configuration Binder uses reflection to call      │
│  the no-arg constructor. Only static nested classes        │
│  have a no-arg constructor by default.                     │
└────────────────────────────────────────────────────────────┘
```

---

We've now covered flat binding and nested object binding. In the next part, we'll extend this further — binding configuration as **Lists** (list of strings AND list of objects) and as **Maps** (map of strings AND map of objects).

---

# Part 4 — Configuration as List & Map

---

## Part A — Configuration as List

There are two flavors of list binding the instructor covers:
- List of Strings
- List of Objects

---

### List of Strings

#### `application.properties`
```properties
user.name=test_username
user.age=27
user.active=true

# List<String>
user.roles[0]=ADMIN
user.roles[1]=EDITOR
```

The index `[0]`, `[1]` in the property key tells Spring Boot the **order** of elements in the list. Spring will maintain this order when binding.

#### How it maps:

```
application.properties              UserConfiguration.java
──────────────────────              ──────────────────────
user.roles[0]=ADMIN      ───►      List<String> roles;
user.roles[1]=EDITOR     ───►         [0] = "ADMIN"
                                       [1] = "EDITOR"

         field name "roles" must match the property key "roles"
```

---

### List of Objects

```properties
# List<Object>
user.course[0].name=Java
user.course[0].enrolled=true

user.course[1].name=Springboot
user.course[1].enrolled=false
```

Here, each index `[0]`, `[1]` represents a **whole object** — with its own fields (`name`, `enrolled`). So this needs a `static` nested class `Course` inside `UserConfiguration`.

#### How it maps:

```
application.properties                  UserConfiguration.java
──────────────────────                  ──────────────────────
user.course[0].name=Java       ───►    List<Course> course;
user.course[0].enrolled=true              [0] = Course {
                                               name     = "Java"
                                               enrolled = true
                                          }
user.course[1].name=Springboot ───►       [1] = Course {
user.course[1].enrolled=false                  name     = "Springboot"
                                               enrolled = false
                                          }

         Course is a static nested class — same reason as AddressConfig
```

---

### Full Code — List of String + List of Object

#### `application.properties`
```properties
user.name=test_username
user.age=27
user.active=true
user.address.city=myCityName
user.address.country=myCountryName

# List<String>
user.roles[0]=ADMIN
user.roles[1]=EDITOR

# List<Object>
user.course[0].name=Java
user.course[0].enrolled=true

user.course[1].name=Springboot
user.course[1].enrolled=false
```

#### `UserConfiguration.java`
```java
@Component
@ConfigurationProperties(prefix = "user")
public class UserConfiguration {

    private String name;
    private int age;
    private boolean active;
    private AddressConfig address;
    private List<String> roles;       // ← List of String
    private List<Course> course;      // ← List of Object

    // Static nested class for Address
    public static class AddressConfig {
        private String city;
        private String country;

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
    }

    // Static nested class for Course
    public static class Course {
        private String name;
        private boolean enrolled;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public boolean isEnrolled() { return enrolled; }
        public void setEnrolled(boolean enrolled) { this.enrolled = enrolled; }
    }

    // Getters and Setters for outer class
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public AddressConfig getAddress() { return address; }
    public void setAddress(AddressConfig address) { this.address = address; }

    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }

    public List<Course> getCourse() { return course; }
    public void setCourse(List<Course> course) { this.course = course; }
}
```

#### `UserController.java`
```java
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    UserConfiguration userConfiguration;

    @GetMapping("/")
    public void getUserConfig() {
        System.out.println("Name: " + userConfiguration.getName());
        System.out.println("Age: " + userConfiguration.getAge());
        System.out.println("Is Active: " + userConfiguration.isActive());
        System.out.println("City: " + userConfiguration.getAddress().getCity());
        System.out.println("Country: " + userConfiguration.getAddress().getCountry());

        // List<String>
        System.out.println("Role 0: " + userConfiguration.getRoles().get(0));  // ADMIN
        System.out.println("Role 1: " + userConfiguration.getRoles().get(1));  // EDITOR

        // List<Object>
        System.out.println("Course 0: " + userConfiguration.getCourse().get(0).getName());  // Java
        System.out.println("Course 1: " + userConfiguration.getCourse().get(1).getName());  // Springboot
    }
}
```

---

## Part B — Configuration as Map

There are again two flavors:
- Map of Strings
- Map of Objects

---

### Map of Strings

```properties
# Map<String, String>
user.preferences.theme=dark
user.preferences.language=en
user.preferences.timezone=IST
```

Here, `preferences` becomes the **map field name**. Everything after `preferences.` becomes a **key**, and its value becomes the **map value**.

```
application.properties                  Java Map
──────────────────────                  ────────
user.preferences.theme=dark    ───►    Map<String, String> preferences
user.preferences.language=en              "theme"    → "dark"
user.preferences.timezone=IST             "language" → "en"
                                          "timezone" → "IST"

    field name = "preferences"
    key        = whatever comes after "preferences."
    value      = the property value
```

The instructor makes an important side note here:

> *"You can also handle this through a nested static class — like we did for address — where theme, language, timezone become fields. Both approaches work. Map is just an alternative way."*

---

### Map of Objects

```properties
# Map<String, Address>
user.locations.home.city=Jaipur
user.locations.home.country=India

user.locations.office.city=Delhi
user.locations.office.country=India
```

Here:
- `locations` → map field name
- `home`, `office` → map **keys**
- `city`, `country` → fields inside the **AddressConfig object** (map value)

```
application.properties                      Java Map
──────────────────────                      ────────
user.locations.home.city=Jaipur    ───►    Map<String, AddressConfig> locations
user.locations.home.country=India             "home"   → AddressConfig {
                                                              city    = "Jaipur"
                                                              country = "India"
                                                         }
user.locations.office.city=Delhi   ───►       "office" → AddressConfig {
user.locations.office.country=India                           city    = "Delhi"
                                                              country = "India"
                                                         }
```

We reuse the same `AddressConfig` static nested class we already created — no need to create a new one!

---

### Full Code — Map of String + Map of Object

#### `application.properties`
```properties
user.name=test_username
user.age=27
user.active=true
user.address.city=myCityName
user.address.country=myCountryName

# List<String>
user.roles[0]=ADMIN
user.roles[1]=EDITOR

# List<Object>
user.course[0].name=Java
user.course[0].enrolled=true

user.course[1].name=Springboot
user.course[1].enrolled=false

# Map<String, String>
user.preferences.theme=dark
user.preferences.language=en
user.preferences.timezone=IST

# Map<String, Address>
user.locations.home.city=Jaipur
user.locations.home.country=India

user.locations.office.city=Delhi
user.locations.office.country=India
```

#### `UserConfiguration.java`
```java
@Component
@ConfigurationProperties(prefix = "user")
public class UserConfiguration {

    private String name;
    private int age;
    private boolean active;
    private AddressConfig address;
    private List<String> roles;
    private List<Course> course;
    private Map<String, String> preferences;             // ← Map of String
    private Map<String, AddressConfig> locations;        // ← Map of Object

    // Static nested class for Address
    public static class AddressConfig {
        private String city;
        private String country;

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
    }

    // Static nested class for Course
    public static class Course {
        private String name;
        private boolean enrolled;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public boolean isEnrolled() { return enrolled; }
        public void setEnrolled(boolean enrolled) { this.enrolled = enrolled; }
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public AddressConfig getAddress() { return address; }
    public void setAddress(AddressConfig address) { this.address = address; }

    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }

    public List<Course> getCourse() { return course; }
    public void setCourse(List<Course> course) { this.course = course; }

    public Map<String, String> getPreferences() { return preferences; }
    public void setPreferences(Map<String, String> preferences) { this.preferences = preferences; }

    public Map<String, AddressConfig> getLocations() { return locations; }
    public void setLocations(Map<String, AddressConfig> locations) { this.locations = locations; }
}
```

#### `UserController.java`
```java
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    UserConfiguration userConfiguration;

    @GetMapping("/")
    public void getUserConfig() {
        System.out.println("Name: " + userConfiguration.getName());
        System.out.println("Age: " + userConfiguration.getAge());
        System.out.println("Is Active: " + userConfiguration.isActive());
        System.out.println("City: " + userConfiguration.getAddress().getCity());
        System.out.println("Country: " + userConfiguration.getAddress().getCountry());

        // List<String>
        System.out.println("Role 0: " + userConfiguration.getRoles().get(0));
        System.out.println("Role 1: " + userConfiguration.getRoles().get(1));

        // List<Object>
        System.out.println("Course 0: " + userConfiguration.getCourse().get(0).getName());
        System.out.println("Course 1: " + userConfiguration.getCourse().get(1).getName());

        // Map<String, String>
        System.out.println("Theme: " + userConfiguration.getPreferences().get("theme"));

        // Map<String, AddressConfig>
        System.out.println("Home City: " + userConfiguration.getLocations().get("home").getCity());
        System.out.println("Office City: " + userConfiguration.getLocations().get("office").getCity());
    }
}
```

---

## Complete Picture — All Binding Types Together

```
application.properties          Java field type         Use case
──────────────────────          ───────────────         ────────
user.name=value                 String / int /          Simple flat
user.age=27                     boolean                 properties

user.address.city=x             Static nested           Grouped
user.address.country=y          class object            sub-properties

user.roles[0]=ADMIN             List<String>            Ordered list
user.roles[1]=EDITOR                                    of simple values

user.course[0].name=Java        List<StaticClass>       Ordered list
user.course[0].enrolled=true                            of objects

user.preferences.theme=dark     Map<String, String>     Key-value pairs
user.preferences.language=en                            (dynamic keys)

user.locations.home.city=x      Map<String,             Key → object
user.locations.office.city=y    StaticClass>            (dynamic keys)
```

---

## Key Rules to Always Remember

```
┌───────────────────────────────────────────────────────────────┐
│  1. Flat fields       → simple Java types (String, int, etc.) │
│  2. Nested objects    → public static nested class            │
│  3. Indexed list      → List<String> or List<StaticClass>     │
│  4. Dynamic key-value → Map<String, String>                   │
│  5. Dynamic key-obj   → Map<String, StaticClass>              │
│                                                               │
│  ALL nested classes used in binding MUST be static.           │
│  ALL fields need getters AND setters for binding to work.     │
└───────────────────────────────────────────────────────────────┘
```

---

We've now covered all the ways you can bind configuration — flat, nested, list, and map. In the next part we'll cover **Validations** — how to enforce constraints on your bound properties so that if something is wrong, the application refuses to start entirely.

---

# Part 5 — Validations

---

## Why Validation Matters Here

Recall from Part 1 — one of the two core problems with `@Value` was that there was **no built-in way to validate** the values coming from `application.properties`. You had to manually write checks after injection.

With `@ConfigurationProperties`, Spring Boot gives you a clean, annotation-driven way to validate your configuration values **before the application fully starts.** This is a very powerful guarantee — if your config is wrong, the app won't even boot. You catch the problem immediately, not after deployment.

The instructor puts it simply:

> *"Spring Boot automatically validates fields after binding, before the app fully starts. And if any validation fails, the application will not even start itself."*

---

## The 3-Step Sequence (Revisited with Validation)

```
APP STARTUP
    │
    ▼
┌──────────────────────────────────────────────────┐
│  STEP 1: Spring IoC creates EMPTY BEAN           │
│  (via @Component → default no-arg constructor)   │
│                                                  │
│  All fields have default values                  │
│  name = null, age = 0, active = false            │
└───────────────────────┬──────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────┐
│  STEP 2: Configuration BINDING                   │
│  Reads application.properties                    │
│  Calls setter methods to fill fields             │
│                                                  │
│  name   = "test_username"                        │
│  age    = 0   ← problematic value from props     │
│  active = true                                   │
└───────────────────────┬──────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────┐
│  STEP 3: VALIDATION (because of @Validated)      │
│                                                  │
│  Checks all annotation constraints on fields.    │
│                                                  │
│  @NotBlank on name  → "test_username" ✓          │
│  @Min(1) on age     → 0 FAILS ✗                  │
│                                                  │
│  ❌ APPLICATION FAILS TO START                    │
│  Reason: Age can not be 0                        │
└──────────────────────────────────────────────────┘
```

This is the full sequence — **Empty Bean → Binding → Validation.** Validation is always the last step, and it only runs if you've added `@Validated` on your class.

---

## What You Need to Set Up Validation

### Step 1 — Add the dependency in `pom.xml`

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

This pulls in the **Hibernate Validator** library internally. The instructor points this out:

> *"These validation annotations are nothing but Hibernate validations internally. If you've worked with Hibernate before, you'll find them very familiar — it's the same thing used under the hood."*

### Step 2 — Add `@Validated` on your `@ConfigurationProperties` class

Without `@Validated`, Spring will completely **ignore** all your validation annotations on the fields. This annotation is what tells Spring to actually run the validation step.

---

## Full Code — Validation in Action

### `application.properties`
```properties
user.name=test_username
user.age=0          ← intentionally wrong to trigger validation failure
user.active=true
user.address.city=myCityName
user.address.country=myCountryName
```

### `UserConfiguration.java`
```java
@Component
@ConfigurationProperties(prefix = "user")
@Validated                                    // ← enables validation step
public class UserConfiguration {

    @NotBlank(message = "name must not be empty")   // ← field level constraint
    private String name;

    @Min(value = 1, message = "Age can not be 0")   // ← field level constraint
    private int age;

    private boolean active;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
```

### What happens when you start the app?

```
APPLICATION FAILED TO START

Description:
Binding to target com.concepts.UserConfiguration failed:

    Property: user.age
    Value: "0"
    Origin: class path resource [application.properties] - line 2
    Reason: Age can not be 0
```

The app refuses to start. The error message is clear, pinpointed, and comes with the exact property name, value, and reason. This is far better than discovering a bad config value at runtime.

---

## All Available Validation Annotations

```
┌─────────────────────────┬─────────────────────────────────────────┬────────────────────────────────┐
│  Annotation             │  Description                            │  Example                       │
├─────────────────────────┼─────────────────────────────────────────┼────────────────────────────────┤
│  @NotNull               │  Value cannot be null                   │  @NotNull                      │
│                         │                                         │  String name;                  │
├─────────────────────────┼─────────────────────────────────────────┼────────────────────────────────┤
│  @NotBlank              │  Not null AND not empty (for String)    │  @NotBlank                     │
│                         │  Whitespace-only strings also fail      │  String username;              │
├─────────────────────────┼─────────────────────────────────────────┼────────────────────────────────┤
│  @NotEmpty              │  Not null AND not empty                 │  @NotEmpty                     │
│                         │  Use on List, Map, etc.                 │  List<String> courses;         │
├─────────────────────────┼─────────────────────────────────────────┼────────────────────────────────┤
│  @Min(value)            │  Minimum numeric value (inclusive)      │  @Min(1)                       │
│                         │  Use on int, long, etc.                 │  int age;                      │
├─────────────────────────┼─────────────────────────────────────────┼────────────────────────────────┤
│  @Max(value)            │  Maximum numeric value (inclusive)      │  @Max(100)                     │
│                         │  Use on int, long, etc.                 │  int age;                      │
├─────────────────────────┼─────────────────────────────────────────┼────────────────────────────────┤
│  @Positive              │  Must be strictly greater than 0        │  @Positive                     │
│                         │                                         │  int retryCount;               │
├─────────────────────────┼─────────────────────────────────────────┼────────────────────────────────┤
│  @PositiveOrZero        │  Must be greater than or equal to 0     │  @PositiveOrZero               │
│                         │                                         │  int retryCount;               │
├─────────────────────────┼─────────────────────────────────────────┼────────────────────────────────┤
│  @Negative              │  Must be strictly less than 0           │  @Negative                     │
│                         │                                         │  int loss;                     │
├─────────────────────────┼─────────────────────────────────────────┼────────────────────────────────┤
│  @NegativeOrZero        │  Must be less than or equal to 0        │  @NegativeOrZero               │
│                         │                                         │  int loss;                     │
├─────────────────────────┼─────────────────────────────────────────┼────────────────────────────────┤
│  @Email                 │  Must be a valid email format           │  @Email                        │
│                         │                                         │  String email;                 │
├─────────────────────────┼─────────────────────────────────────────┼────────────────────────────────┤
│  @Pattern(regexp)       │  Must match the given regex             │  @Pattern(regexp="[A-Za-z]*")  │
│                         │                                         │  String userName;              │
├─────────────────────────┼─────────────────────────────────────────┼────────────────────────────────┤
│  @AssertTrue            │  Field must be true                     │  @AssertTrue                   │
│                         │                                         │  boolean isActive;             │
├─────────────────────────┼─────────────────────────────────────────┼────────────────────────────────┤
│  @AssertFalse           │  Field must be false                    │  @AssertFalse                  │
│                         │                                         │  boolean isExpired;            │
└─────────────────────────┴─────────────────────────────────────────┴────────────────────────────────┘
```

---

## Quick Validation Cheatsheet — Which to Use When

```
Type of field                     Recommended annotation
─────────────────────────────     ──────────────────────
String (must exist)           →   @NotNull
String (must have content)    →   @NotBlank  ← most commonly used for strings
List / Map (must not empty)   →   @NotEmpty
Number (lower bound)          →   @Min(value)
Number (upper bound)          →   @Max(value)
Number (must be > 0)          →   @Positive
Number (must be >= 0)         →   @PositiveOrZero
Email field                   →   @Email
Custom format                 →   @Pattern(regexp = "...")
Boolean must be true          →   @AssertTrue
Boolean must be false         →   @AssertFalse
```

---

## Important Things to Remember

```
┌────────────────────────────────────────────────────────────────┐
│  VALIDATION RULES                                              │
├────────────────────────────────────────────────────────────────┤
│  1. Add spring-boot-starter-validation dependency              │
│                                                                │
│  2. Add @Validated on the @ConfigurationProperties class       │
│     Without this, ALL validation annotations are IGNORED       │
│                                                                │
│  3. Validation runs AFTER binding, BEFORE app fully starts     │
│     Sequence: Empty Bean → Binding → Validation                │
│                                                                │
│  4. If ANY validation fails → app refuses to start             │
│     You get a clear error with property name + reason          │
│                                                                │
│  5. Annotations go on the FIELDS directly                      │
│     Same whether your class is mutable or immutable            │
└────────────────────────────────────────────────────────────────┘
```

---

## The Natural Question This Raises — Interview Alert 🎯

Now that you've seen how binding works (using setter methods), a very natural question arises:

> *"The entire binding process depends on setter methods. That means our configuration class can never be truly immutable — because anyone can call a setter and change the values after binding. How do we fix this?"*

This is exactly what the instructor builds up to next. And this is a **very common interview question** in Spring Boot:

> **"How do you make a `@ConfigurationProperties` class immutable?"**

The answer is **Constructor Binding** — and that's exactly what we'll cover in the final part.

---

Whenever you're ready, say **"Next"** for Part 6 — Immutable Configuration with Constructor Binding!

# Part 6 — Immutable Configuration (Constructor Binding)

---

## The Problem With the Current Approach

Let's first clearly understand WHY the current approach (using `@Component` + setter-based binding) cannot give us immutability.

Here's the current flow we've been using:

```
CURRENT APPROACH (Mutable)
──────────────────────────
        │
        ▼
┌─────────────────────────────────────────┐
│  STEP 1: Spring IoC creates EMPTY BEAN  │
│  via @Component → no-arg constructor    │
│                                         │
│  UserConfiguration {                    │
│      name   = null   ← default          │
│      age    = 0      ← default          │
│      active = false  ← default          │
│  }                                      │
└──────────────────┬──────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────┐
│  STEP 2: Configuration Binding          │
│  calls SETTER methods to update fields  │
│                                         │
│  setName("test_username")  ← changes    │
│  setAge(27)                ← changes    │
│  setActive(true)           ← changes    │
└──────────────────┬──────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────┐
│  Fields were CHANGED after object       │
│  creation via setters.                  │
│                                         │
│  ∴ This is NOT immutable. ✗             │
│                                         │
│  Anyone can still call setAge(999)      │
│  at any point in the code.              │
└─────────────────────────────────────────┘
```

The instructor explains:

> *"When you have a setter method, definitely it's not an immutable class. Immutable means once you have defined the value, it cannot be changed. But here — first during the empty bean, age is zero. Then during binding it calls the setter and updates it to 27. You are changing it. Definitely it's not immutable."*

And in the real world, configuration **should** be immutable. Once you've read it from `application.properties` at startup, nobody should be able to change it. This is how it's actually done in the industry.

---

## The Solution — Constructor Binding

Instead of:
1. Creating an empty bean first
2. Then filling values via setters

We do it in **one shot** — pass all the values directly through the constructor at the time of object creation itself. No setters needed. Fields can be `final`.

```
CONSTRUCTOR BINDING APPROACH (Immutable)
─────────────────────────────────────────
        │
        ▼
┌───────────────────────────────────────────────────┐
│  Configuration Binder reads application.properties│
│  FIRST — before creating the object               │
│                                                   │
│  It knows:                                        │
│      name   = "test_username"                     │
│      age    = 27                                  │
│      active = true                                │
└─────────────────────┬─────────────────────────────┘
                      │
                      ▼
┌──────────────────────────────────────────────────┐
│  Binder invokes the CONSTRUCTOR directly         │
│  with the values it already knows                │
│                                                  │
│  new ImmutableUserConfiguration(                 │
│      "test_username",                            │
│      27,                                         │
│      true                                        │
│  )                                               │
└─────────────────────┬────────────────────────────┘
                      │
                      ▼
┌──────────────────────────────────────────────────┐
│  Object is created with values SET AT BIRTH      │
│                                                  │
│  ImmutableUserConfiguration {                    │
│      final name   = "test_username"  ✓           │
│      final age    = 27               ✓           │
│      final active = true             ✓           │
│  }                                               │
│                                                  │
│  No setters. Fields are final.                   │
│  Nobody can change these values. ✓               │
└──────────────────────────────────────────────────┘
```

---

## But Wait — Why Can't We Use `@Component` Here?

This is a very important question the instructor addresses carefully. Let's think about it:

```
WHY @Component DOESN'T WORK for Constructor Binding
────────────────────────────────────────────────────

@Component tells Spring IoC:
"Hey Spring, YOU manage the creation of this object."

But Spring IoC creates beans using the DEFAULT NO-ARG CONSTRUCTOR.

ImmutableUserConfiguration {
    final String name;
    final int age;
    final boolean active;

    ImmutableUserConfiguration(String name, int age, boolean active) {
        // ← This is the ONLY constructor
        // ← There is NO no-arg constructor
    }
}

Spring IoC tries to call: new ImmutableUserConfiguration()
                                                         ↑
                                        This doesn't exist! ✗

Spring IoC doesn't know:
    - What value to pass for name?
    - What value to pass for age?
    - What value to pass for active?

∴ @Component CANNOT work here.
  We must remove it.
```

The instructor says:

> *"As soon as you put `@Component`, you are telling Spring that hey, you manage the creation of an object. But Spring IOC doesn't know what value to pass here, what name to provide, what age to provide. So `@Component` won't work — and that's why I removed it."*

---

## So Who Creates the Bean Now?

We give the **responsibility of bean creation to Configuration Binder itself** — because it's the one that reads `application.properties` and already knows what values to pass to the constructor.

We tell Spring Boot about this through a single annotation on the main application class:

```
@ConfigurationPropertiesScan
```

This tells the configuration binder:

> *"Scan for all classes annotated with `@ConfigurationProperties` and take responsibility for creating their objects yourself — don't rely on Spring IoC for this."*

---

## Full Code — Immutable Configuration

### `ImmutableUserConfiguration.java`

```java
// Notice: NO @Component here
@ConfigurationProperties(prefix = "user")
@Validated                                    // validation works the same way
public class ImmutableUserConfiguration {

    // Fields are final — cannot be changed after creation
    private final String name;

    @Min(1)                                   // validation on final field
    private final int age;

    private final boolean active;

    // The ONLY constructor — binder will call this
    ImmutableUserConfiguration(String name, int age, boolean active) {
        this.name = name;
        this.age = age;
        this.active = active;
    }

    // ONLY getters — NO setters
    public String getName() { return name; }
    public int getAge() { return age; }
    public boolean isActive() { return active; }
}
```

### `ConfigurationPropertiesApplication.java` (Main Class)

```java
@SpringBootApplication
@ConfigurationPropertiesScan        // ← tells binder to manage bean creation
public class ConfigurationPropertiesApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigurationPropertiesApplication.class, args);
    }
}
```

### `UserController.java`

```java
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    ImmutableUserConfiguration immutableUserConfiguration;  // inject normally

    @GetMapping("/")
    public void getUserConfig() {
        // Use exactly like before — no difference from outside
        System.out.println("Name: " + immutableUserConfiguration.getName());
        System.out.println("Age: " + immutableUserConfiguration.getAge());
        System.out.println("Is Active: " + immutableUserConfiguration.isActive());
    }
}
```

### Output
```
immutable name: test_username
immutable Age: 27
immutable is active: true
```

---

## Side-by-Side Comparison — Mutable vs Immutable

```
┌─────────────────────────────┬──────────────────────────────────────────┐
│  MUTABLE (setter-based)     │  IMMUTABLE (constructor-based)           │
├─────────────────────────────┼──────────────────────────────────────────┤
│  @Component present         │  @Component REMOVED                      │
├─────────────────────────────┼──────────────────────────────────────────┤
│  Fields not final           │  Fields are final                        │
├─────────────────────────────┼──────────────────────────────────────────┤
│  Has getters + setters      │  Has ONLY getters                        │
├─────────────────────────────┼──────────────────────────────────────────┤
│  No-arg constructor present │  Only parameterized constructor          │
├─────────────────────────────┼──────────────────────────────────────────┤
│  Spring IoC creates bean    │  Configuration Binder creates bean       │
├─────────────────────────────┼──────────────────────────────────────────┤
│  Binding: setter methods    │  Binding: constructor arguments          │
├─────────────────────────────┼──────────────────────────────────────────┤
│  Values set in 2 steps      │  Values set in 1 shot at birth           │
│  (create empty → fill)      │  (create with values directly)           │
├─────────────────────────────┼──────────────────────────────────────────┤
│  @ConfigurationPropertiesScan NOT needed  │  @ConfigurationPropertiesScan on main class │
├─────────────────────────────┼──────────────────────────────────────────┤
│  Validation works same      │  Validation works same                   │
│  (@Validated + annotations) │  (@Validated + annotations on fields)    │
└─────────────────────────────┴──────────────────────────────────────────┘
```

---

## Full Internal Flow — Constructor Binding

```
APP STARTUP WITH @ConfigurationPropertiesScan
      │
      ▼
┌─────────────────────────────────────────────────────────────┐
│  @ConfigurationPropertiesScan tells binder:                 │
│  "Find all @ConfigurationProperties classes                 │
│   and handle their bean creation yourself."                 │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│  Configuration Binder reads application.properties FIRST    │
│                                                             │
│  user.name   = test_username                                │
│  user.age    = 27                                           │
│  user.active = true                                         │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│  Binder finds ImmutableUserConfiguration class              │
│  Sees it has a constructor with (name, age, active)         │
│  Invokes constructor with the values it read                │
│                                                             │
│  new ImmutableUserConfiguration("test_username", 27, true)  │
│                                                             │
│  Object is born with correct values already set             │
│  Fields are final — they can NEVER be changed again         │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│  @Validated runs                                            │
│  Checks @Min(1) on age → 27 passes ✓                        │
│  All validations pass → Bean is registered                  │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│  Bean available for @Autowired injection anywhere           │
│  Usage is identical to the mutable version from outside     │
└─────────────────────────────────────────────────────────────┘
```

---

## Interview Tip 🎯

This is one of the most commonly asked Spring Boot interview questions around `@ConfigurationProperties`:

> **Q: How do you make a `@ConfigurationProperties` class immutable?**
>
> **A:** By using Constructor Binding instead of setter-based binding. The key changes are:
> - Remove `@Component` — Spring IoC cannot invoke a parameterized constructor since it doesn't know what values to pass.
> - Make all fields `final` — so they cannot be changed after object creation.
> - Provide only a parameterized constructor — the Configuration Binder will read `application.properties` and invoke this constructor with the correct values.
> - Remove all setter methods — only getters are needed.
> - Add `@ConfigurationPropertiesScan` on the main class — this gives the bean creation responsibility to the Configuration Binder instead of Spring IoC.
> - Validation works exactly the same — just add `@Validated` and the constraint annotations on the fields as usual.

---

## Complete Summary — Everything in One Place

```
┌─────────────────────────────────────────────────────────────────┐
│              @ConfigurationProperties — Full Picture            │
├─────────────────────────────────────────────────────────────────┤
│  PROBLEM IT SOLVES                                              │
│  → @Value is 1-to-1, no validation, doesn't scale               │
│                                                                 │
│  WHAT IT DOES                                                   │
│  → Maps application.properties into a Java object               │
│  → Structured, Reusable, Validated                              │
│                                                                 │
│  BINDING TYPES                                                  │
│  → Flat fields      : String, int, boolean                      │
│  → Nested object    : public static nested class                │
│  → List of strings  : List<String>                              │
│  → List of objects  : List<StaticNestedClass>                   │
│  → Map of strings   : Map<String, String>                       │
│  → Map of objects   : Map<String, StaticNestedClass>            │
│                                                                 │
│  BINDING SEQUENCE                                               │
│  → Empty Bean → Binding (setters) → Validation                  │
│                                                                 │
│  VALIDATION                                                     │
│  → Add spring-boot-starter-validation dependency                │
│  → Add @Validated on class                                      │
│  → Add constraint annotations on fields                         │
│  → Failure = app refuses to start                               │
│                                                                 │
│  IMMUTABILITY                                                   │
│  → Remove @Component                                            │
│  → Make fields final                                            │
│  → Only parameterized constructor, no setters                   │
│  → Add @ConfigurationPropertiesScan on main class               │
│  → Binder creates object via constructor in one shot            │
└─────────────────────────────────────────────────────────────────┘
```

---

That's the complete lecture on `@ConfigurationProperties` — all 6 parts done! You now have a thorough understanding of the problem it solves, how it works internally, all binding types, validation, and immutability. Feel free to ask if you want anything clarified or expanded!