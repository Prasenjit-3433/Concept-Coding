# Lombok Feature: @Cleanup

## 📌 Overview

The `@Cleanup` annotation ensures that a given resource is automatically cleaned up (closed) before the execution path exits the current scope. It's Lombok's way of implementing automatic resource management.

---

## 🎯 Purpose

- **Problem:** We need to manually close resources (files, streams, connections) using try-catch-finally blocks, which is verbose
- **Solution:** Use `@Cleanup` to automatically generate the cleanup code in a finally block

---

## 💡 What @Cleanup Does

When you annotate a resource variable with `@Cleanup`:
1. Lombok wraps your code in a **try-catch-finally** block
2. In the **finally block**, it calls the `close()` method on the resource
3. This ensures the resource is cleaned up even if exceptions occur

---

## 🔍 Complete Example

### Your Code:
```java
public class FileReader {
    public void readFile() throws FileNotFoundException {
        @Cleanup 
        FileInputStream inputStream = new FileInputStream("test.txt");
        
        // Read data from file
        int data = inputStream.read();
        System.out.println(data);
        
        // No need to manually close inputStream!
    }
}
```

### What Lombok Generates (.class file):
```java
public class FileReader {
    public void readFile() throws FileNotFoundException {
        FileInputStream inputStream = new FileInputStream("test.txt");
        
        try {
            // Your original code
            int data = inputStream.read();
            System.out.println(data);
            
        } finally {
            // Lombok added this cleanup code!
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }
}
```

---

## 🎯 Key Features

### 1. **Automatic try-finally Generation**

You write:
```java
@Cleanup 
FileInputStream stream = new FileInputStream("file.txt");
```

Lombok generates:
```java
FileInputStream stream = new FileInputStream("file.txt");
try {
    // your code
} finally {
    if (stream != null) {
        stream.close();
    }
}
```

### 2. **Null-Safe Closing**

Lombok always checks for null before calling `close()`:
```java
if (inputStream != null) {
    inputStream.close();
}
```

### 3. **Scope-Based Cleanup**

The resource is cleaned up when execution exits the current scope (method, block, etc.)

---

## 💻 Practical Examples

### Example 1: Reading from a File
```java
public void readData() throws IOException {
    @Cleanup 
    FileInputStream fis = new FileInputStream("data.txt");
    
    @Cleanup
    BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
    
    String line;
    while ((line = reader.readLine()) != null) {
        System.out.println(line);
    }
    
    // Both fis and reader are automatically closed!
}
```

### Example 2: Database Connection
```java
public void queryDatabase() throws SQLException {
    @Cleanup 
    Connection connection = DriverManager.getConnection(url, user, password);
    
    @Cleanup
    Statement statement = connection.createStatement();
    
    @Cleanup
    ResultSet resultSet = statement.executeQuery("SELECT * FROM users");
    
    while (resultSet.next()) {
        System.out.println(resultSet.getString("name"));
    }
    
    // All resources automatically closed in reverse order!
}
```

### Example 3: Output Stream
```java
public void writeToFile() throws IOException {
    @Cleanup 
    FileOutputStream fos = new FileOutputStream("output.txt");
    
    @Cleanup
    BufferedOutputStream bos = new BufferedOutputStream(fos);
    
    bos.write("Hello, World!".getBytes());
    bos.flush();
    
    // Both streams automatically closed!
}
```

---

## 🔄 Before and After Comparison

### Without @Cleanup (Traditional Way):
```java
public void readFile() {
    FileInputStream inputStream = null;
    try {
        inputStream = new FileInputStream("test.txt");
        int data = inputStream.read();
        System.out.println(data);
        
    } catch (IOException e) {
        e.printStackTrace();
        
    } finally {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
```

### With @Cleanup (Lombok Way):
```java
public void readFile() throws IOException {
    @Cleanup 
    FileInputStream inputStream = new FileInputStream("test.txt");
    
    int data = inputStream.read();
    System.out.println(data);
    
    // That's it! Much cleaner!
}
```

**Reduced from ~15 lines to ~5 lines!**

---

## ⚙️ How It Works

### The Cleanup Process:

1. **Declaration:** Annotate the resource variable with `@Cleanup`
```java
   @Cleanup FileInputStream stream = new FileInputStream("file.txt");
```

2. **Usage:** Use the resource normally in your code
```java
   int data = stream.read();
```

3. **Automatic Cleanup:** When scope exits, Lombok-generated finally block closes it
```java
   finally {
       if (stream != null) {
           stream.close();
       }
   }
```

---

## 🎯 Custom Cleanup Methods

By default, `@Cleanup` calls the `close()` method. You can specify a different cleanup method:
```java
@Cleanup("dispose") 
SomeResource resource = new SomeResource();

// Lombok will call resource.dispose() instead of resource.close()
```

**Generated Code:**
```java
try {
    // your code
} finally {
    if (resource != null) {
        resource.dispose();  // Custom method
    }
}
```

---

## 📋 Common Use Cases

### 1. **File I/O Operations**
```java
@Cleanup FileInputStream fis = new FileInputStream("file.txt");
@Cleanup FileOutputStream fos = new FileOutputStream("output.txt");
```

### 2. **Database Operations**
```java
@Cleanup Connection conn = getConnection();
@Cleanup PreparedStatement stmt = conn.prepareStatement(sql);
@Cleanup ResultSet rs = stmt.executeQuery();
```

### 3. **Network Streams**
```java
@Cleanup Socket socket = new Socket("localhost", 8080);
@Cleanup InputStream in = socket.getInputStream();
@Cleanup OutputStream out = socket.getOutputStream();
```

### 4. **BufferedReader/Writer**
```java
@Cleanup BufferedReader reader = new BufferedReader(new FileReader("input.txt"));
@Cleanup BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt"));
```

---

## 🆚 @Cleanup vs Try-With-Resources

### Java 7+ Try-With-Resources:
```java
try (FileInputStream fis = new FileInputStream("file.txt")) {
    int data = fis.read();
    System.out.println(data);
}
// Automatically closed
```

### Lombok @Cleanup:
```java
@Cleanup 
FileInputStream fis = new FileInputStream("file.txt");
int data = fis.read();
System.out.println(data);
// Automatically closed
```

### Key Differences:

| Feature | Try-With-Resources | @Cleanup |
|---------|-------------------|----------|
| Java Version | Java 7+ | Any Java version (6+) |
| Syntax | try ( ) block | Annotation |
| Resource Type | Must implement AutoCloseable | Any object with close() method |
| Scope | Try block only | Current scope/method |
| Multiple Resources | All in try() | Multiple @Cleanup annotations |

---

## ⚠️ Important Notes

1. **Scope-Based:** Resource is cleaned up when execution exits the current scope
2. **Null-Safe:** Always checks if resource is null before closing
3. **Exception Handling:** Finally block ensures cleanup even if exceptions occur
4. **Default Method:** Calls `close()` by default, but can specify custom method
5. **Multiple Resources:** Can use multiple `@Cleanup` annotations in same method
6. **Order:** Resources are closed in reverse order of declaration

---

## 🔍 Cleanup Order Example
```java
public void multipleResources() throws IOException {
    @Cleanup FileInputStream fis = new FileInputStream("file.txt");  // Closed 3rd
    @Cleanup BufferedReader br = new BufferedReader(new InputStreamReader(fis));  // Closed 2nd
    @Cleanup PrintWriter pw = new PrintWriter("output.txt");  // Closed 1st
    
    // Use resources
}
```

**Cleanup order:** Last declared, first closed (LIFO - Last In, First Out)

---

## ✅ Best Practices

1. **Use for any closeable resource** - Files, streams, connections, etc.
2. **Prefer over manual try-finally** - Less verbose, less error-prone
3. **Consider try-with-resources for Java 7+** - Native Java feature
4. **Use for backward compatibility** - When supporting older Java versions
5. **Annotate at declaration** - Must be at variable declaration, not later
6. **Check resource requirements** - Ensure the resource has a close/cleanup method

---

## ⚙️ Limitations

1. **Must be at declaration:** Cannot add `@Cleanup` to existing variable
```java
   // ❌ Wrong
   FileInputStream fis = new FileInputStream("file.txt");
   @Cleanup fis;  // Cannot do this!
   
   // ✅ Correct
   @Cleanup FileInputStream fis = new FileInputStream("file.txt");
```

2. **Cannot skip cleanup:** No way to conditionally skip the cleanup

3. **Method must exist:** Resource must have the cleanup method (default: close())

---

## 🔗 Related Features

- Try-with-resources (Java 7+) - Native alternative
- AutoCloseable interface - Java's standard for closeable resources
- Resource management patterns

---

## 📌 Quick Summary
```java
// Without @Cleanup - Manual cleanup (verbose)
FileInputStream fis = null;
try {
    fis = new FileInputStream("file.txt");
    // use fis
} finally {
    if (fis != null) {
        fis.close();
    }
}

// With @Cleanup - Automatic cleanup (concise)
@Cleanup FileInputStream fis = new FileInputStream("file.txt");
// use fis
// Automatically closed!
```

---

## 🎯 When to Use @Cleanup

✅ **Use @Cleanup when:**
- Working with older Java versions (pre-Java 7)
- Want cleaner syntax than try-with-resources
- Resource doesn't implement AutoCloseable
- Need custom cleanup method (not close())

❌ **Consider alternatives when:**
- Using Java 7+ and resource implements AutoCloseable → Use try-with-resources
- Need conditional cleanup → Manual finally block
- Complex cleanup logic required → Manual implementation

---

**Previous Feature:** [@Builder](link-to-previous-feature)  
**Back to:** [Table of Contents](link-to-toc)

---

## 🎉 Conclusion

This completes the **Top 10 Lombok Features**! These are the most frequently used features in production code:

1. ✅ `val` and `var`
2. ✅ `@NonNull`
3. ✅ `@Getter` and `@Setter`
4. ✅ `@ToString`
5. ✅ Constructor Annotations
6. ✅ `@EqualsAndHashCode`
7. ✅ `@Data`
8. ✅ `@Value`
9. ✅ `@Builder`
10. ✅ `@Cleanup`

---

**Happy Coding with Lombok! 🚀**