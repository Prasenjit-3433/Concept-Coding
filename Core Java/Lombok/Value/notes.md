# Lombok Feature: @Value

## 📌 Overview

`@Value` is the **immutable version of `@Data`**. It's designed to create immutable classes where all fields are final and cannot be changed after object creation.

---

## 🎯 Purpose

- **Problem:** Creating immutable classes requires making all fields final, removing setters, and making the class final
- **Solution:** Use `@Value` to automatically create a fully immutable class

---

## 💡 What @Value Does

`@Value` makes the class **immutable** by:

1. **Making all fields `private` and `final`**
2. **Making the class itself `final`** (cannot be subclassed)
3. **No setter methods generated** (because all fields are final)
4. **Generating getter methods for all fields**
5. **Generating `toString()` method**
6. **Generating `equals()` and `hashCode()` methods**
7. **Generating constructor with all fields** (equivalent to `@AllArgsConstructor`)

---

## 🔍 Complete Example

### Your Code:
```java
@Value
public class Person {
    String name;
    Integer age;
    String address;
}
```

### What Lombok Generates:
```java
public final class Person {  // Class is made FINAL
    private final String name;      // All fields are private final
    private final Integer age;
    private final String address;
    
    // Constructor with ALL fields (like @AllArgsConstructor)
    public Person(String name, Integer age, String address) {
        this.name = name;
        this.age = age;
        this.address = address;
    }
    
    // Getters for all fields
    public String getName() {
        return this.name;
    }
    
    public Integer getAge() {
        return this.age;
    }
    
    public String getAddress() {
        return this.address;
    }
    
    // NO SETTERS - because all fields are final!
    
    // toString() method
    public String toString() {
        return "Person(name=" + this.name + ", age=" + this.age + ", address=" + this.address + ")";
    }
    
    // equals() method
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Person)) return false;
        Person other = (Person) o;
        // ... proper equals implementation
        return true;
    }
    
    // hashCode() method
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        // ... proper hashCode implementation
        return result;
    }
}
```

---

## 📊 @Value vs @Data Comparison

| Feature | @Data | @Value |
|---------|-------|--------|
| Fields | Regular (mutable) | `private final` (immutable) |
| Class | Can be extended | `final` (cannot be extended) |
| Getters | ✅ Yes | ✅ Yes |
| Setters | ✅ Yes (non-final fields) | ❌ No (all fields final) |
| Constructor | `@RequiredArgsConstructor` | All fields constructor |
| Mutability | Mutable | **Immutable** |
| toString() | ✅ Yes | ✅ Yes |
| equals/hashCode | ✅ Yes | ✅ Yes |

---

## 🎯 What @Value Includes

Think of `@Value` as:
```java
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Getter
// Plus making the class itself final
```

---

## 🔒 Immutability Benefits

### Why Use Immutable Classes?

1. **Thread-Safe:** Can be safely shared between threads
2. **No Side Effects:** Cannot be modified after creation
3. **Predictable:** State cannot change unexpectedly
4. **Cache-Friendly:** Can be safely cached
5. **Hashcode Stability:** Safe to use as HashMap keys

---

## 💻 Practical Examples

### Example 1: Value Object
```java
@Value
public class Money {
    double amount;
    String currency;
}

// Usage
Money price = new Money(100.0, "USD");
System.out.println(price.getAmount());  // 100.0

// Cannot modify!
// price.setAmount(200.0);  // ❌ NO setter method exists!
```

### Example 2: Configuration Object
```java
@Value
public class DatabaseConfig {
    String host;
    int port;
    String database;
    String username;
    String password;
}

// Usage
DatabaseConfig config = new DatabaseConfig(
    "localhost", 
    5432, 
    "mydb", 
    "admin", 
    "secret"
);

// Config is immutable - safe to share across application
```

### Example 3: Coordinate/Point
```java
@Value
public class Point {
    int x;
    int y;
}

// Usage
Point p1 = new Point(10, 20);
Point p2 = new Point(10, 20);

System.out.println(p1.equals(p2));  // true
// Can safely use in HashMap/HashSet
```

---

## 🔧 Constructor Behavior

### All Fields Are Final = All Fields in Constructor

Since `@Value` makes **ALL fields final**, they must be initialized in the constructor.

**This means the constructor includes ALL fields**, making it equivalent to `@AllArgsConstructor`.
```java
@Value
public class Person {
    String name;   // final
    Integer age;   // final
    String address; // final
}

// Generated constructor has ALL three fields
public Person(String name, Integer age, String address) {
    this.name = name;
    this.age = age;
    this.address = address;
}
```

**Why all fields?**
- `@RequiredArgsConstructor` only includes final and @NonNull fields
- Since ALL fields are now final (thanks to @Value)
- ALL fields must be in the constructor
- This is effectively the same as `@AllArgsConstructor`

---

## 🎯 Key Characteristics

### 1. **Class is Final**
```java
@Value
public class Person { ... }

// Generated as:
public final class Person { ... }

// Cannot extend!
// class Employee extends Person { }  // ❌ Compilation error!
```

### 2. **All Fields are Private Final**
```java
@Value
public class Person {
    String name;  // You write this
}

// Generated as:
public final class Person {
    private final String name;  // Lombok adds private final
}
```

### 3. **No Setters**
```java
@Value
public class Person {
    String name;
}

// NO setters generated because fields are final
// person.setName("John");  // ❌ Method doesn't exist!
```

### 4. **Getters for All Fields**
```java
@Value
public class Person {
    String name;
    Integer age;
}

// Getters are generated
person.getName();  // ✅ Works
person.getAge();   // ✅ Works
```

---

## ⚙️ Common Use Cases

### 1. **Domain Value Objects**
```java
@Value
public class Email {
    String address;
}

@Value
public class UserId {
    Long id;
}
```

### 2. **API Response Objects**
```java
@Value
public class ApiResponse {
    int statusCode;
    String message;
    Object data;
}
```

### 3. **Configuration Classes**
```java
@Value
public class ServerConfig {
    String host;
    int port;
    int timeout;
    boolean sslEnabled;
}
```

### 4. **Event Objects**
```java
@Value
public class UserRegisteredEvent {
    String userId;
    String email;
    LocalDateTime timestamp;
}
```

---

## 🔄 Modifying Immutable Objects

**Question:** How do you "change" an immutable object?

**Answer:** You don't! You create a **new** object with the changed values.

### Using @With Annotation (Bonus Feature):
```java
@Value
@With
public class Person {
    String name;
    Integer age;
}

// Usage
Person person1 = new Person("John", 25);
Person person2 = person1.withAge(26);  // Creates NEW object

System.out.println(person1.getAge());  // 25 (unchanged)
System.out.println(person2.getAge());  // 26 (new object)
```

---

## ⚠️ Important Notes

1. **Complete Immutability:** Object state cannot change after creation
2. **Thread-Safe by Default:** Safe to share across threads
3. **No Inheritance:** Class is final, cannot be extended
4. **All Fields Final:** Every field is made final automatically
5. **Constructor Required:** Must provide all values at creation time
6. **No Default Values:** All fields must be initialized via constructor

---

## ✅ Best Practices

1. **Use @Value for value objects** - Perfect for domain value objects
2. **Use @Value for DTOs** - When immutability is desired
3. **Use @Value for configuration** - Config objects should be immutable
4. **Use @Value for events** - Event objects should not change
5. **Combine with @Builder** - Makes object creation easier
6. **Use @With for "updates"** - Create modified copies when needed

---

## 🔗 Related Features

- `@Data` - Mutable version of `@Value`
- `@Builder` - Often combined with `@Value` for easier object creation
- `@With` - Creates copies with modified values
- `@AllArgsConstructor` - Similar constructor behavior
- Immutability patterns in Java

---

## 📌 Quick Summary
```java
// @Data = Mutable
@Data
class Person {
    String name;  // Can be changed via setter
}

// @Value = Immutable
@Value
class Person {
    String name;  // Cannot be changed, no setter exists
}
```

---

**Previous Feature:** [@Data](link-to-previous-feature)  
**Next Feature:** [@Builder](link-to-next-feature)