# Lombok Feature: @Builder

## 📌 Overview

The `@Builder` annotation implements the **Builder Design Pattern** automatically for your class, allowing you to create objects part-by-part in a fluent, readable way.

---

## 🎯 Purpose

- **Problem:** Complex objects with many parameters are hard to construct, especially when some are optional
- **Solution:** Use the Builder pattern to construct objects step-by-step with a fluent API

---

## 📚 Background - Builder Design Pattern

The instructor has already explained the Builder Design Pattern in detail in the **Design Patterns playlist**.

### Two Main Benefits of Builder Pattern:

1. **Create objects part-by-part** - Don't need to provide all values at once
2. **Helps achieve immutability** - Object is created only when `build()` is called

**Reference:** Check the Design Patterns video for in-depth explanation of how Builder pattern works internally.

---

## 💡 Basic Usage

### Your Code:
```java
@Builder
public class TestPojo {
    private String name;
    private Integer age;
}
```

### What Lombok Generates:
```java
public class TestPojo {
    private String name;
    private Integer age;
    
    // Private constructor
    TestPojo(String name, Integer age) {
        this.name = name;
        this.age = age;
    }
    
    // Static builder() method to create builder instance
    public static TestPojoBuilder builder() {
        return new TestPojoBuilder();
    }
    
    // Inner Builder class
    public static class TestPojoBuilder {
        private String name;
        private Integer age;
        
        TestPojoBuilder() {
        }
        
        // Fluent setter for name (returns builder)
        public TestPojoBuilder name(String name) {
            this.name = name;
            return this;
        }
        
        // Fluent setter for age (returns builder)
        public TestPojoBuilder age(Integer age) {
            this.age = age;
            return this;
        }
        
        // build() method creates the actual object
        public TestPojo build() {
            return new TestPojo(this.name, this.age);
        }
        
        public String toString() {
            return "TestPojo.TestPojoBuilder(name=" + this.name + ", age=" + this.age + ")";
        }
    }
}
```

---

## 🔍 Key Components Generated

### 1. **Builder Class**
- Inner static class named `{ClassName}Builder`
- Holds temporary values during construction
- Example: `TestPojoBuilder` for `TestPojo`

### 2. **builder() Method**
- Static method to create builder instance
- Entry point for the builder pattern
```java
TestPojo.builder()  // Creates builder instance
```

### 3. **Fluent Setters**
- Methods named after each field
- Return the builder instance (for method chaining)
- Allow part-by-part construction
```java
.name("John")   // Returns builder
.age(25)        // Returns builder
```

### 4. **build() Method**
- Final method that creates the actual object
- Calls the constructor with accumulated values
```java
.build()  // Returns TestPojo object
```

---

## 💻 Usage Example
```java
@Builder
public class TestPojo {
    private String name;
    private Integer age;
}

// Using the builder
TestPojo obj = TestPojo.builder()      // 1. Create builder instance
                       .name("John")    // 2. Set name (returns builder)
                       .age(25)         // 3. Set age (returns builder)
                       .build();        // 4. Build final object

System.out.println(obj.getName());  // John
System.out.println(obj.getAge());   // 25
```

---

## 🎯 Builder Pattern Benefits

### 1. **Part-by-Part Construction**

You can set fields in any order and skip optional ones:
```java
// Set all fields
TestPojo obj1 = TestPojo.builder()
                        .name("John")
                        .age(25)
                        .build();

// Skip optional fields
TestPojo obj2 = TestPojo.builder()
                        .name("Jane")
                        .build();  // age remains null

// Different order
TestPojo obj3 = TestPojo.builder()
                        .age(30)
                        .name("Bob")
                        .build();
```

### 2. **Fluent/Readable API**

Method chaining creates readable, self-documenting code:
```java
User user = User.builder()
                .username("john_doe")
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .age(25)
                .active(true)
                .build();
```

Much more readable than:
```java
User user = new User("john_doe", "john@example.com", "John", "Doe", 25, true);
```

### 3. **Immutability**

Notice in the generated code: **No setter methods in TestPojo class!**
```java
@Builder
public class TestPojo {
    private String name;
    private Integer age;
    
    // NO setters generated!
}

// Usage
TestPojo obj = TestPojo.builder()
                       .name("John")
                       .age(25)
                       .build();

// Cannot modify after creation!
// obj.setName("Jane");  // ❌ Method doesn't exist!
```

**This achieves immutability** - object cannot be changed after `build()` is called.

---

## 🔧 Real-World Example

### Complex Configuration Object:
```java
@Builder
public class DatabaseConfig {
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    private int connectionTimeout;
    private int maxConnections;
    private boolean sslEnabled;
}

// Usage - much cleaner than constructor with 8 parameters!
DatabaseConfig config = DatabaseConfig.builder()
                                      .host("localhost")
                                      .port(5432)
                                      .database("myapp")
                                      .username("admin")
                                      .password("secret")
                                      .connectionTimeout(30)
                                      .maxConnections(100)
                                      .sslEnabled(true)
                                      .build();
```

Compare with constructor approach:
```java
// ❌ Hard to read, easy to make mistakes with parameter order
DatabaseConfig config = new DatabaseConfig(
    "localhost", 5432, "myapp", "admin", "secret", 30, 100, true
);
```

---

## 🎯 Builder Pattern Flow
```
TestPojo.builder()          // Step 1: Create builder instance (TestPojoBuilder)
         ↓
    .name("John")            // Step 2: Set name field (returns builder)
         ↓
    .age(25)                 // Step 3: Set age field (returns builder)
         ↓
    .build()                 // Step 4: Create final TestPojo object
         ↓
    TestPojo object          // Final immutable object
```

---

## 🔄 Combining with Other Annotations

### @Builder with @Getter
```java
@Builder
@Getter
public class Person {
    private String name;
    private Integer age;
}

// Now you can read values
Person person = Person.builder()
                      .name("John")
                      .age(25)
                      .build();

System.out.println(person.getName());  // Getter available
```

### @Builder with @Value (Immutable Objects)
```java
@Value
@Builder
public class Person {
    String name;
    Integer age;
}

// Fully immutable with builder pattern
Person person = Person.builder()
                      .name("John")
                      .age(25)
                      .build();
```

### @Builder with @Data
```java
@Data
@Builder
public class Person {
    private String name;
    private Integer age;
}

// Both builder and traditional setters available
```

---

## 📋 Common Use Cases

### 1. **Test Data Builders**
```java
@Builder
public class TestUser {
    private String username;
    private String email;
    private String role;
}

// In tests
TestUser admin = TestUser.builder()
                         .username("admin")
                         .email("admin@test.com")
                         .role("ADMIN")
                         .build();
```

### 2. **API Request/Response Objects**
```java
@Builder
@Getter
public class ApiRequest {
    private String endpoint;
    private String method;
    private Map<String, String> headers;
    private String body;
}
```

### 3. **Complex Domain Objects**
```java
@Builder
@Getter
public class Order {
    private String orderId;
    private Customer customer;
    private List<OrderItem> items;
    private Address shippingAddress;
    private PaymentMethod paymentMethod;
    private LocalDateTime orderDate;
}
```

### 4. **Configuration Objects**
```java
@Builder
public class EmailConfig {
    private String smtpHost;
    private int smtpPort;
    private String username;
    private String password;
    private boolean tls;
}
```

---

## ⚙️ Default Values with @Builder.Default

You can set default values for fields:
```java
@Builder
public class Person {
    private String name;
    
    @Builder.Default
    private Integer age = 18;  // Default value
    
    @Builder.Default
    private boolean active = true;  // Default value
}

// Usage
Person person = Person.builder()
                      .name("John")
                      .build();  // age=18, active=true (defaults applied)
```

---

## ⚠️ Important Notes

1. **No Setters Generated:** Builder promotes immutability - no setter methods in main class
2. **Fluent API:** All builder methods return the builder instance for chaining
3. **Thread-Safe After Build:** Once built, object cannot be modified
4. **Optional Parameters:** Can skip fields - they'll be null or default values
5. **Readable Code:** Makes object construction self-documenting

---

## ✅ Best Practices

1. **Use for classes with many fields** (4+ parameters)
2. **Use when you have optional parameters**
3. **Combine with @Value or @Data** for complete functionality
4. **Use @Builder.Default** for sensible defaults
5. **Great for test data creation** - very readable in tests
6. **Consider for public APIs** - better than multiple constructors

---

## 🔗 Related Features

- `@Value` - Often combined with `@Builder` for immutable objects
- `@Data` - Can be combined with `@Builder`
- `@With` - Alternative for creating modified copies
- Design Patterns - Builder Pattern (see instructor's playlist)

---

## 📌 Comparison: Constructor vs Builder

### Constructor Approach (❌ Problems):
```java
// Hard to read, parameter order matters
Person p = new Person("John", 25, "john@example.com", "123 Street", true);
```

### Builder Approach (✅ Better):
```java
// Self-documenting, order doesn't matter, can skip optional fields
Person p = Person.builder()
                 .name("John")
                 .age(25)
                 .email("john@example.com")
                 .address("123 Street")
                 .active(true)
                 .build();
```

---

## 📚 Further Learning

For in-depth understanding of how the Builder pattern works internally, refer to the instructor's **Design Patterns playlist** where the Builder pattern is explained in detail.

---

**Previous Feature:** [@Value](link-to-previous-feature)  
**Next Feature:** [@Cleanup](link-to-next-feature)