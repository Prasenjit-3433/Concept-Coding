# Lombok Feature: @ToString

## 📌 Overview

The `@ToString` annotation automatically generates the `toString()` method for a class, which is commonly used for logging and debugging purposes.

---

## 🎯 Purpose

- **Problem:** We often need to print object contents for logging/debugging, requiring manual implementation of `toString()` method
- **Solution:** Use `@ToString` annotation to automatically generate the method during compilation

---

## 💡 Key Features

### 1. **Basic Usage**

Simply annotate your class with `@ToString`:
```java
@ToString
public class Person {
    private String name;
    private boolean committeeMember;
}
```

**Generated Code:**
```java
public class Person {
    private String name;
    private boolean committeeMember;
    
    public String toString() {
        return "Person(name=" + this.name + ", committeeMember=" + this.committeeMember + ")";
    }
}
```

**Default Format:**
```
ClassName(fieldName=value, fieldName=value)
```

---

### 2. **Excluding Specific Fields**

You can exclude fields from the `toString()` output using `@ToString.Exclude`:
```java
@ToString
public class Person {
    private String name;
    
    @ToString.Exclude
    private boolean committeeMember;
}
```

**Generated Code:**
```java
public String toString() {
    return "Person(name=" + this.name + ")";
}
```

**Use Case:** Exclude sensitive fields or fields you don't want in logs (e.g., passwords, large data)

---

### 3. **Excluding Field Names**

You can configure `@ToString` to exclude field names from the output, printing only values:
```java
@ToString(includeFieldNames = false)
public class Person {
    private String name;
    private boolean committeeMember;
}
```

**Generated Code:**
```java
public String toString() {
    return "Person(" + this.name + ", " + this.committeeMember + ")";
}
```

**Output Format:**
```
ClassName(value, value)
```

**Use Case:** Reduce log size when field names are not necessary or obvious from context

---

### 4. **Explicitly Selecting Fields (onlyExplicitlyIncluded)**

You can make `@ToString` include **only** fields that are explicitly marked:
```java
@ToString(onlyExplicitlyIncluded = true)
public class Person {
    @ToString.Include
    private String name;
    
    private boolean committeeMember;  // This will NOT be included
    
    private int age;  // This will NOT be included
}
```

**Generated Code:**
```java
public String toString() {
    return "Person(name=" + this.name + ")";
}
```

**How it works:**
1. Set `onlyExplicitlyIncluded = true` at class level
2. Only fields marked with `@ToString.Include` will be included
3. All other fields are automatically excluded

**Use Case:** Fine-grained control when you want to include only specific fields from a class with many fields

---

## 🎯 Common Use Cases

### Logging Example
```java
@ToString
public class User {
    private String username;
    
    @ToString.Exclude  // Don't log passwords!
    private String password;
    
    private String email;
}

// Usage in code
User user = new User();
System.out.println(user);  // User(username=john, email=john@example.com)
```

### Debugging Example
```java
@ToString(includeFieldNames = false)
public class Point {
    private int x;
    private int y;
}

// Output: Point(10, 20) - compact format for debugging
```

---

## 📊 Configuration Options Summary

| Option | Description | Default |
|--------|-------------|---------|
| `includeFieldNames` | Include field names in output | `true` |
| `onlyExplicitlyIncluded` | Include only explicitly marked fields | `false` |
| `@ToString.Exclude` | Exclude specific field | N/A |
| `@ToString.Include` | Explicitly include field | N/A |

---

## ⚠️ Important Notes

1. **Log Size Consideration:** Including too many fields or field names can increase log size significantly
2. **Security:** Always exclude sensitive data (passwords, tokens, etc.) using `@ToString.Exclude`
3. **Performance:** The generated `toString()` method is efficient and follows Java best practices
4. **Inheritance:** By default, doesn't include superclass fields (can be configured)

---

## ✅ Best Practices

1. **Always exclude sensitive fields** like passwords, API keys, tokens
2. **Use `includeFieldNames = false`** when log size is a concern and field names are obvious
3. **Use `onlyExplicitlyIncluded`** for classes with many fields where you only need to log a few
4. **Remember:** This is primarily for **logging and debugging**, not for production display

---

## 🔗 Related Features

- `@Data` - Includes `@ToString` along with other annotations
- `@Value` - Includes `@ToString` for immutable classes

---

**Previous Feature:** [@Getter and @Setter](link-to-previous-feature)  
**Next Feature:** [Constructor Annotations](link-to-next-feature)