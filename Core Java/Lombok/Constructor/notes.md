# Lombok Feature: Constructor Annotations

## 📌 Overview

Lombok provides three constructor-related annotations that automatically generate constructors based on different criteria:
- `@NoArgsConstructor` - No-argument constructor
- `@AllArgsConstructor` - Constructor with all fields
- `@RequiredArgsConstructor` - Constructor with required fields only

---

## 🎯 Purpose

- **Problem:** Writing constructors manually is repetitive, especially when classes have many fields
- **Solution:** Use Lombok's constructor annotations to automatically generate them during compilation

---

## 💡 The Three Constructor Annotations

### 1. **@NoArgsConstructor**

Generates a constructor with **no parameters** (default constructor).
```java
@NoArgsConstructor
public class Person {
    private String name;
    private boolean committeeMember;
    private Integer age;
}
```

**Generated Code:**
```java
public class Person {
    private String name;
    private boolean committeeMember;
    private Integer age;
    
    public Person() {
        // Empty constructor
    }
}
```

**Use Case:** Required for frameworks like JPA, Hibernate, Jackson that use reflection

---

### 2. **@AllArgsConstructor**

Generates a constructor with **all fields** as parameters.
```java
@AllArgsConstructor
public class Person {
    private String name;
    private boolean committeeMember;
    private Integer age;
}
```

**Generated Code:**
```java
public class Person {
    private String name;
    private boolean committeeMember;
    private Integer age;
    
    public Person(String name, boolean committeeMember, Integer age) {
        this.name = name;
        this.committeeMember = committeeMember;
        this.age = age;
    }
}
```

**Use Case:** When you want to initialize all fields at once during object creation

---

### 3. **@RequiredArgsConstructor**

Generates a constructor with **only required fields** as parameters.

**Required fields are:**
- Fields marked with `final`
- Fields marked with `@NonNull`
```java
@RequiredArgsConstructor
public class Person {
    private String name;
    private boolean committeeMember;
    
    @NonNull
    private Integer age;
}
```

**Generated Code:**
```java
public class Person {
    private String name;
    private boolean committeeMember;
    private Integer age;
    
    public Person(Integer age) {
        if (age == null) {
            throw new NullPointerException("age is marked non-null but is null");
        }
        this.age = age;
    }
}
```

**Notice:** Only `age` is in the constructor because it's marked with `@NonNull`

---

## 🔍 Complete Example - All Three Together
```java
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
public class Person {
    private String name;
    private boolean committeeMember;
    
    @NonNull
    private Integer age;
}
```

**Generated Code:**
```java
public class Person {
    private String name;
    private boolean committeeMember;
    private Integer age;
    
    // 1. No-args constructor
    public Person() {
    }
    
    // 2. All-args constructor
    public Person(String name, boolean committeeMember, Integer age) {
        if (age == null) {
            throw new NullPointerException("age is marked non-null but is null");
        }
        this.name = name;
        this.committeeMember = committeeMember;
        this.age = age;
    }
    
    // 3. Required-args constructor (only @NonNull fields)
    public Person(Integer age) {
        if (age == null) {
            throw new NullPointerException("age is marked non-null but is null");
        }
        this.age = age;
    }
}
```

---

## 🎓 Understanding @NonNull with Constructors

### How @NonNull Works with Fields

Remember from earlier lectures: `@NonNull` is used on **method/constructor parameters**.

**But how does it work with fields?**

When you use `@NonNull` on a field:
1. Lombok includes that field in `@RequiredArgsConstructor`
2. When generating the constructor, Lombok adds that field as a parameter
3. **NOW** `@NonNull` is applied to the constructor parameter
4. This generates the null check inside the constructor
```java
@RequiredArgsConstructor
public class Person {
    @NonNull
    private Integer age;  // @NonNull on field
}
```

**Generated:**
```java
public Person(Integer age) {  // @NonNull effectively on parameter now
    if (age == null) {
        throw new NullPointerException("age is marked non-null but is null");
    }
    this.age = age;
}
```

---

## 📊 Constructor Selection Rules

### @RequiredArgsConstructor includes:

| Field Type | Included? | Reason |
|------------|-----------|--------|
| `final` field | ✅ Yes | Must be initialized |
| `@NonNull` field | ✅ Yes | Marked as required |
| Regular field | ❌ No | Optional field |
| `static` field | ❌ No | Not instance-specific |

### Example with final fields:
```java
@RequiredArgsConstructor
public class Person {
    private final String name;  // final - included
    
    @NonNull
    private Integer age;        // @NonNull - included
    
    private String address;     // regular - NOT included
}
```

**Generated Constructor:**
```java
public Person(String name, Integer age) {
    if (age == null) {
        throw new NullPointerException("age is marked non-null but is null");
    }
    this.name = name;
    this.age = age;
}
```

---

## 🎯 Common Usage Patterns

### Pattern 1: Entity Classes (JPA/Hibernate)
```java
@NoArgsConstructor  // Required by JPA
@AllArgsConstructor // Convenient for testing
public class User {
    private Long id;
    private String username;
    private String email;
}
```

### Pattern 2: Immutable Objects
```java
@RequiredArgsConstructor
public class Point {
    private final int x;
    private final int y;
}
```

### Pattern 3: Builder Pattern Support
```java
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    private String name;
    private Double price;
}
```

---

## ⚠️ Important Notes

1. **@NonNull Integration:** When `@NonNull` is on a field, it automatically adds null checks in generated constructors
2. **Final Fields:** Must be initialized - they're always included in `@RequiredArgsConstructor`
3. **Order Matters:** Constructor parameters follow field declaration order
4. **Combining Annotations:** You can use multiple constructor annotations on the same class

---

## ✅ Best Practices

1. Use `@NoArgsConstructor` for JPA entities and framework compatibility
2. Use `@AllArgsConstructor` for complete initialization and testing
3. Use `@RequiredArgsConstructor` for mandatory fields (dependency injection, immutability)
4. Mark truly required fields with `final` or `@NonNull`
5. Consider using all three when you need flexibility

---

## 🔗 Related Features

- `@NonNull` - Works with constructors for null safety
- `@Data` - Includes `@RequiredArgsConstructor`
- `@Value` - Includes constructor for all fields (like `@AllArgsConstructor`)
- `@Builder` - Alternative to constructors for object creation

---

**Previous Feature:** [@ToString](link-to-previous-feature)  
**Next Feature:** [@EqualsAndHashCode](link-to-next-feature)