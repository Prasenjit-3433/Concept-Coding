# Lombok Feature: @Getter and @Setter

## 📌 Overview

The `@Getter` and `@Setter` annotations automatically generate getter and setter methods for class fields, eliminating the need to write boilerplate code manually.

---

## 🎯 Purpose

- **Problem:** In POJO classes, we need to write getter and setter methods for each field, which is repetitive and time-consuming
- **Solution:** Annotate fields with `@Getter` and `@Setter`, and let Lombok generate these methods automatically during compilation

---

## 💡 Key Features

### 1. **Field-Level Usage**

You can annotate individual fields with `@Getter` and `@Setter`:
```java
public class Person {
    @Getter @Setter
    private String name;
    
    @Getter @Setter
    private boolean committeeMember;
}
```

**Generated Code:**
```java
public class Person {
    private String name;
    private boolean committeeMember;
    
    public String getName() {
        return this.name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public boolean isCommitteeMember() {  // Note: 'is' for boolean
        return this.committeeMember;
    }
    
    public void setCommitteeMember(boolean committeeMember) {
        this.committeeMember = committeeMember;
    }
}
```

**Note:** For boolean fields, getter method starts with `is` instead of `get`

---

### 2. **Default Access Level**

- **By default, generated getter and setter methods are `public`**

---

### 3. **Custom Access Levels**

You can control the access level of generated methods:
```java
public class Person {
    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PROTECTED)
    private String name;
    
    @Getter @Setter
    private boolean committeeMember;
}
```

**Generated Code:**
```java
public class Person {
    private String name;
    private boolean committeeMember;
    
    private String getName() {          // PRIVATE getter
        return this.name;
    }
    
    protected void setName(String name) { // PROTECTED setter
        this.name = name;
    }
    
    public boolean isCommitteeMember() {  // PUBLIC by default
        return this.committeeMember;
    }
    
    public void setCommitteeMember(boolean committeeMember) {
        this.committeeMember = committeeMember;
    }
}
```

**Available Access Levels:**
- `AccessLevel.PUBLIC` (default)
- `AccessLevel.PROTECTED`
- `AccessLevel.PRIVATE`
- `AccessLevel.PACKAGE` (package-private)

---

### 4. **Class-Level Usage**

Instead of annotating each field individually, you can use annotations at the class level:
```java
@Getter
@Setter
public class Person {
    private String name;
    private boolean committeeMember;
    private static int count;
}
```

#### Important Rules for Class-Level Annotations:

**For `@Getter`:**
- Applied to **all non-static fields**
- Static fields are skipped

**For `@Setter`:**
- Applied to **all non-static AND non-final fields**
- Static fields are skipped
- Final fields are skipped (makes sense - you can't change final values!)

**Example with static and final fields:**
```java
@Getter
@Setter
public class Person {
    private String name;              // ✅ getter + setter
    private boolean committeeMember;  // ✅ getter + setter
    private static int count;         // ❌ no getter, no setter (static)
    private final String someVar;     // ✅ getter only, ❌ no setter (final)
}
```

**Generated Code:**
```java
public class Person {
    private String name;
    private boolean committeeMember;
    private static int count;
    private final String someVar;
    
    public String getName() {
        return this.name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public boolean isCommitteeMember() {
        return this.committeeMember;
    }
    
    public void setCommitteeMember(boolean committeeMember) {
        this.committeeMember = committeeMember;
    }
    
    public String getSomeVar() {      // Only getter for final field
        return this.someVar;
    }
    
    // No getter/setter for static field 'count'
    // No setter for final field 'someVar'
}
```

---

### 5. **Skipping Generation with AccessLevel.NONE**

When using class-level annotations, you can exclude specific fields from getter/setter generation:
```java
@Getter
@Setter
public class Person {
    @Setter(AccessLevel.NONE)  // Override: Don't generate setter
    private String name;
    
    private boolean committeeMember;
}
```

**Generated Code:**
```java
public class Person {
    private String name;
    private boolean committeeMember;
    
    public String getName() {           // ✅ Getter generated
        return this.name;
    }
    
    // ❌ NO setter for 'name' - explicitly excluded
    
    public boolean isCommitteeMember() { // ✅ Getter generated
        return this.committeeMember;
    }
    
    public void setCommitteeMember(boolean committeeMember) { // ✅ Setter generated
        this.committeeMember = committeeMember;
    }
}
```

**Use Case:** Fine-grained control - apply annotations at class level but selectively disable for specific fields

---

## 🎯 Best Practices

1. **Use class-level annotations** for cleaner code when most fields need getters/setters
2. **Use field-level annotations** when only a few fields need them
3. **Use `AccessLevel.NONE`** to exclude specific fields from class-level generation
4. **Remember:** Final fields only get getters, never setters
5. **Remember:** Static fields are always excluded

---

## ✅ Industry Usage

This is **one of the most frequently used Lombok features** in production code. You'll see it extensively in:
- POJO/DTO classes
- Entity classes
- Model classes
- Data transfer objects

---

## 🔗 Related Features

- `@Data` - Includes `@Getter` and `@Setter` along with other annotations
- `@Value` - Creates immutable classes (getters only, no setters)

---

**Next Feature:** [@ToString](link-to-next-feature)