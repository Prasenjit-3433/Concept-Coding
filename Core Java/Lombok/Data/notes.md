# Lombok Feature: @Data

## 📌 Overview

`@Data` is a **powerful shortcut annotation** that combines multiple Lombok annotations into one. It's essentially a convenience annotation that bundles commonly used features together.

---

## 🎯 Purpose

- **Problem:** Often we need multiple Lombok annotations on a single class (`@Getter`, `@Setter`, `@ToString`, etc.)
- **Solution:** Use `@Data` as a single annotation that includes all of them

---

## 💡 What @Data Includes

`@Data` is equivalent to using the following annotations together:
```java
@ToString
@EqualsAndHashCode
@Getter (on all fields)
@Setter (on all non-final fields)
@RequiredArgsConstructor
```

**Think of it as:** `@Data` = `@ToString` + `@EqualsAndHashCode` + `@Getter` + `@Setter` + `@RequiredArgsConstructor`

---

## 🔍 Complete Example

### Your Code:
```java
@Data
public class Person {
    private String name;
    private final Integer age;
    
    @NonNull
    private String address;
}
```

### What Lombok Generates:
```java
public class Person {
    private String name;
    private final Integer age;
    private String address;
    
    // 1. toString() method
    public String toString() {
        return "Person(name=" + this.name + ", age=" + this.age + ", address=" + this.address + ")";
    }
    
    // 2. equals() method
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Person)) return false;
        Person other = (Person) o;
        // ... proper equals implementation for all fields
        return true;
    }
    
    // 3. hashCode() method
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        // ... proper hashCode implementation for all fields
        return result;
    }
    
    // 4. Getters for ALL fields
    public String getName() {
        return this.name;
    }
    
    public Integer getAge() {
        return this.age;
    }
    
    public String getAddress() {
        return this.address;
    }
    
    // 5. Setters for non-final fields only
    public void setName(String name) {
        this.name = name;
    }
    
    public void setAddress(String address) {
        if (address == null) {
            throw new NullPointerException("address is marked non-null but is null");
        }
        this.address = address;
    }
    
    // NO setter for 'age' because it's final
    
    // 6. RequiredArgsConstructor (for final and @NonNull fields)
    public Person(Integer age, String address) {
        if (address == null) {
            throw new NullPointerException("address is marked non-null but is null");
        }
        this.age = age;
        this.address = address;
    }
}
```

---

## 📊 Feature Breakdown

### 1. **@ToString**
- Generates `toString()` method
- Includes all fields by default

### 2. **@EqualsAndHashCode**
- Generates `equals()` and `hashCode()` methods
- Uses all non-static, non-transient fields

### 3. **@Getter (on all fields)**
- Generates getter methods for **ALL fields**
- Applies to static and non-static fields

### 4. **@Setter (on non-final fields)**
- Generates setter methods for **non-final fields only**
- Final fields don't get setters (makes sense - can't change final values!)

### 5. **@RequiredArgsConstructor**
- Generates constructor with:
    - All `final` fields
    - All `@NonNull` fields

---

## 🎯 Field-Specific Rules

| Field Type | Getter? | Setter? | In Constructor? |
|------------|---------|---------|-----------------|
| Regular field | ✅ Yes | ✅ Yes | ❌ No |
| `final` field | ✅ Yes | ❌ No | ✅ Yes |
| `@NonNull` field | ✅ Yes | ✅ Yes (with null check) | ✅ Yes |
| `static` field | ❌ No | ❌ No | ❌ No |

---

## 💻 Practical Example

### Before @Data (Manual Code):
```java
public class User {
    private String username;
    private String email;
    private final Long id;
    
    public User(Long id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public Long getId() {
        return id;
    }
    
    @Override
    public String toString() {
        return "User{username='" + username + "', email='" + email + "', id=" + id + '}';
    }
    
    @Override
    public boolean equals(Object o) {
        // ... equals implementation
    }
    
    @Override
    public int hashCode() {
        // ... hashCode implementation
    }
}
```

### After @Data (Lombok):
```java
@Data
public class User {
    private String username;
    private String email;
    private final Long id;
}
```

**That's it! Just 5 lines instead of 40+**

---

## 🔧 Customization

You can still customize individual features while using `@Data`:
```java
@Data
public class Person {
    private String name;
    
    @ToString.Exclude
    private String password;  // Excluded from toString()
    
    @EqualsAndHashCode.Exclude
    private String lastLogin;  // Excluded from equals/hashCode
    
    @Setter(AccessLevel.PROTECTED)
    private String email;  // Protected setter instead of public
}
```

---

## ⚙️ Amount of Generated Code

Looking at the `.class` file, you'll see that `@Data` generates a **significant amount of code**:
```java
// Your source code: Just 5 lines
@Data
public class Person {
    private String name;
    private final Integer age;
    private String address;
}

// Generated .class file: 50+ lines of code!
// - toString() method
// - equals() method  
// - hashCode() method
// - 3 getter methods
// - 2 setter methods
// - 1 constructor
// - 1 canEqual() method
```

**This demonstrates the power of Lombok** - massive code reduction!

---

## 🎯 Common Use Cases

### 1. **DTO (Data Transfer Object)**
```java
@Data
public class UserDTO {
    private String username;
    private String email;
    private Integer age;
}
```

### 2. **Entity Classes**
```java
@Data
public class Product {
    private final Long id;
    private String name;
    private Double price;
}
```

### 3. **API Request/Response Models**
```java
@Data
public class LoginRequest {
    @NonNull
    private String username;
    
    @NonNull
    private String password;
}
```

---

## ⚠️ Important Notes

1. **Immense Code Generation:** One annotation generates 50+ lines of code
2. **Setter Exclusion:** Final fields automatically excluded from setter generation
3. **Constructor Logic:** Only includes final and @NonNull fields in constructor
4. **No Setter for Static:** Static fields never get getters or setters
5. **Null Checks:** @NonNull fields get automatic null checks in setters and constructor

---

## ✅ Best Practices

1. **Use @Data for DTOs and POJOs** - Perfect for simple data classes
2. **Customize when needed** - Override specific features using field-level annotations
3. **Be aware of what's generated** - Understand all the methods being created
4. **Consider @Value for immutable classes** - Better alternative when immutability is needed
5. **Exclude sensitive fields from toString()** - Use `@ToString.Exclude` for passwords, etc.

---

## 🔗 Related Features

- `@Value` - Immutable version of `@Data`
- `@ToString` - Included in `@Data`
- `@EqualsAndHashCode` - Included in `@Data`
- `@Getter` - Included in `@Data`
- `@Setter` - Included in `@Data`
- `@RequiredArgsConstructor` - Included in `@Data`

---

## 📌 Quick Comparison

| Feature | @Data | Manual Code |
|---------|-------|-------------|
| Lines of code | ~5 | ~50+ |
| Maintenance | Easy | Tedious |
| Error-prone | No | Yes |
| Boilerplate | None | Heavy |

---

**Previous Feature:** [@EqualsAndHashCode](link-to-previous-feature)  
**Next Feature:** [@Value](link-to-next-feature)