# Lombok Feature: @EqualsAndHashCode

## 📌 Overview

The `@EqualsAndHashCode` annotation automatically generates `equals()` and `hashCode()` methods for a class, following the proper contract between these two methods.

---

## 🎯 Purpose

- **Problem:** Manually implementing `equals()` and `hashCode()` is error-prone and tedious
- **Solution:** Use `@EqualsAndHashCode` to automatically generate both methods with proper implementation

---

## 📚 Background Knowledge

Before using this annotation, it's important to understand:
- The contract between `equals()` and `hashCode()` methods
- How they work internally (especially in HashMap)
- Why they must be consistent with each other

**Reference:** The instructor has explained this in detail in the "Java HashMap Internal Working" lecture.

---

## 💡 Basic Usage

### Simple Example
```java
@EqualsAndHashCode
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
    
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Person)) return false;
        Person other = (Person) o;
        if (!other.canEqual(this)) return false;
        
        Object this$name = this.name;
        Object other$name = other.name;
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) 
            return false;
        
        if (this.committeeMember != other.committeeMember) return false;
        
        Object this$age = this.age;
        Object other$age = other.age;
        if (this$age == null ? other$age != null : !this$age.equals(other$age)) 
            return false;
        
        return true;
    }
    
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        
        Object $name = this.name;
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        
        result = result * PRIME + (this.committeeMember ? 79 : 97);
        
        Object $age = this.age;
        result = result * PRIME + ($age == null ? 43 : $age.hashCode());
        
        return result;
    }
    
    protected boolean canEqual(Object other) {
        return other instanceof Person;
    }
}
```

---

## 🎯 Default Behavior

### Fields Included by Default:

| Field Type | Included in equals/hashCode? |
|------------|------------------------------|
| Non-static fields | ✅ Yes |
| Non-transient fields | ✅ Yes |
| Static fields | ❌ No |
| Transient fields | ❌ No |

**By default:** All **non-static** and **non-transient** fields are included

---

## 🔧 Excluding Specific Fields

You can exclude specific fields from `equals()` and `hashCode()` calculations:
```java
@EqualsAndHashCode
public class Person {
    private String name;
    
    @EqualsAndHashCode.Exclude
    private boolean committeeMember;
    
    private Integer age;
}
```

**Generated Code:**
```java
public class Person {
    private String name;
    private boolean committeeMember;  // This field is EXCLUDED
    private Integer age;
    
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Person)) return false;
        Person other = (Person) o;
        if (!other.canEqual(this)) return false;
        
        // Only 'name' and 'age' are compared
        // 'committeeMember' is NOT compared
        
        Object this$name = this.name;
        Object other$name = other.name;
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) 
            return false;
        
        Object this$age = this.age;
        Object other$age = other.age;
        if (this$age == null ? other$age != null : !this$age.equals(other$age)) 
            return false;
        
        return true;
    }
    
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        
        // Only 'name' and 'age' are used
        // 'committeeMember' is NOT used
        
        Object $name = this.name;
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        
        Object $age = this.age;
        result = result * PRIME + ($age == null ? 43 : $age.hashCode());
        
        return result;
    }
}
```

**Use Case:** Exclude fields that shouldn't affect equality (e.g., timestamps, cached values, derived fields)

---

## 📋 Field Selection Summary

### Example with Different Field Types:
```java
@EqualsAndHashCode
public class Person {
    private String name;              // ✅ Included
    
    @EqualsAndHashCode.Exclude
    private boolean committeeMember;  // ❌ Excluded (explicitly)
    
    private static int count;         // ❌ Excluded (static)
    
    private transient String cache;   // ❌ Excluded (transient)
    
    private Integer age;              // ✅ Included
}
```

---

## ⚙️ Contract Compliance

The generated code follows the **equals() and hashCode() contract**:

### Key Contract Rules:
1. **Consistency:** If two objects are equal according to `equals()`, they must have the same `hashCode()`
2. **Reflexive:** `x.equals(x)` must return `true`
3. **Symmetric:** If `x.equals(y)` returns `true`, then `y.equals(x)` must return `true`
4. **Transitive:** If `x.equals(y)` and `y.equals(z)` return `true`, then `x.equals(z)` must return `true`
5. **Consistent:** Multiple invocations of `equals()` must consistently return the same result
6. **Null handling:** `x.equals(null)` must return `false`

**Important:** Lombok's generated code properly implements all these rules, so you don't have to worry about violations.

---

## 🎯 Practical Example

### Using in HashMap/HashSet:
```java
@EqualsAndHashCode
public class User {
    private String username;
    private String email;
}

// Usage
User user1 = new User("john", "john@example.com");
User user2 = new User("john", "john@example.com");

// equals() check
System.out.println(user1.equals(user2));  // true

// HashMap usage
Map<User, String> userMap = new HashMap<>();
userMap.put(user1, "Value1");
System.out.println(userMap.get(user2));   // "Value1" - works because of proper hashCode()

// HashSet usage
Set<User> userSet = new HashSet<>();
userSet.add(user1);
System.out.println(userSet.contains(user2));  // true
```

---

## 🎓 Advanced Configuration

### Explicitly Including Fields (onlyExplicitlyIncluded)

Similar to `@ToString`, you can use explicit inclusion:
```java
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Person {
    @EqualsAndHashCode.Include
    private String name;
    
    private boolean committeeMember;  // Not included
    
    private Integer age;              // Not included
}
```

**Result:** Only `name` will be used in `equals()` and `hashCode()`

---

## ⚠️ Important Notes

1. **Proper Implementation:** Lombok generates code that follows the equals/hashCode contract correctly
2. **No Manual Coding Required:** You don't need to worry about implementing the contract manually
3. **Static Fields:** Automatically excluded (they're class-level, not instance-level)
4. **Transient Fields:** Automatically excluded (typically temporary/cached data)
5. **Consistency:** All fields used in `equals()` are also used in `hashCode()` and vice versa

---

## ✅ Best Practices

1. **Exclude derived/calculated fields** that don't represent object identity
2. **Exclude fields that change frequently** but don't affect equality (e.g., lastModifiedDate)
3. **Include fields that define object identity** (e.g., ID, username, email)
4. **Don't worry about the contract** - Lombok handles it correctly
5. **Use with collections** - Essential for proper HashMap/HashSet behavior

---

## 🔗 Related Features

- `@Data` - Includes `@EqualsAndHashCode` along with other annotations
- `@Value` - Includes `@EqualsAndHashCode` for immutable classes
- HashMap Internal Working - Understanding why these methods are important

---

## 📚 Further Reading

For deeper understanding of the equals/hashCode contract and HashMap internals, refer to the instructor's video: **"Java HashMap Internal Working"**

---

**Previous Feature:** [Constructor Annotations](link-to-previous-feature)  
**Next Feature:** [@Data](link-to-next-feature)