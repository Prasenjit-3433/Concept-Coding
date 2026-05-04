# Project Lombok in Java - Complete Guide

## 📚 Course Overview
**Source:** Concept & Coding YouTube Channel  
**Topic:** Project Lombok in Java - Top 10 Features  
**Duration:** 30+ minutes

---

## 🎯 What This Lecture Covers

This comprehensive tutorial focuses on **Project Lombok**, a highly popular Java library used extensively in the industry to reduce boilerplate code. The instructor covers practical implementations of Lombok's most frequently used features.

---

## 📋 Table of Contents

1. [Introduction to Lombok](#1-introduction-to-lombok)
2. [Setup & Configuration](#2-setup--configuration)
3. [Feature 1: val and var](#3-feature-1-val-and-var)
4. [Feature 2: @NonNull](#4-feature-2-nonnull)
5. [Feature 3: @Getter and @Setter](#5-feature-3-getter-and-setter)
6. [Feature 4: @ToString](#6-feature-4-tostring)
7. [Feature 5: Constructor Annotations](#7-feature-5-constructor-annotations)
8. [Feature 6: @EqualsAndHashCode](#8-feature-6-equalsandhashcode)
9. [Feature 7: @Data](#9-feature-7-data)
10. [Feature 8: @Value](#10-feature-8-value)
11. [Feature 9: @Builder](#11-feature-9-builder)
12. [Feature 10: @Cleanup](#12-feature-10-cleanup)

---

## 1. Introduction to Lombok

### 🤔 What is Lombok?

**Lombok** is a Java library that helps reduce boilerplate code using annotations.

### 🎯 Key Points

- **Purpose**: Reduce unnecessary code (boilerplate code) in Java
- **How it works**: Processes annotations during compilation and injects code into Java classes
- **Common Complaint**: Engineers from other languages often say Java requires writing too much unnecessary code beyond business logic
- **Initiative**: Lombok is a project aimed at reducing this boilerplate code

### ✅ Compatibility

- **Supported Versions**: Compatible with Java 6 and all later versions
- **Industry Usage**: Widely used in production projects across the industry

### 💡 Why Lombok?

> "If you talk with any engineer who is not working with Java, the first thing they will say is: 'Hey, in Java we have to write too much code, unnecessary code.' This is nothing but boilerplate code, apart from business logic."

---

## 2. Setup & Configuration

### 📦 Step 1: Add Dependency to pom.xml

Add the Lombok library to your Maven `pom.xml`:
```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>latest-version</version>
    <scope>provided</scope>
</dependency>
```

**Note**: Pick the latest Lombok version from Maven Central.

---

### 🔧 Step 2: Install Lombok Plugin in IntelliJ IDEA

**Problem**: Without the plugin, your IDE will show compilation errors (red lines) even though the code compiles and runs successfully.

**Why?**
- Lombok adds code during **compile time**
- Your IDE doesn't know how to treat Lombok annotations
- The code runs fine because Lombok dependency processes it during compilation

**Solution**:

1. Open IntelliJ IDEA Settings
2. Navigate to **Plugins**
3. Search for **"Lombok"**
4. Download and install the Lombok plugin

---

### ⚙️ Step 3: Enable Annotation Processing

1. Go to **Settings** → **Build, Execution, Deployment** → **Compiler** → **Annotation Processors**
2. **Enable** the checkbox: "Enable annotation processing"

---

### ✅ Verification

After completing these two steps:
- Your IDE will no longer show compilation errors for Lombok annotations
- The IDE becomes aware of what Lombok will do during compile time
- You can code smoothly with proper IDE support

### 📝 Summary Checklist

- [ ] Add Lombok dependency to `pom.xml` (**Mandatory**)
- [ ] Install Lombok plugin in IDE (*Optional but highly recommended*)
- [ ] Enable annotation processing in IDE settings (*Optional but highly recommended*)

> **Note**: Adding the plugin and enabling annotation processing is not mandatory for compilation but is very helpful during coding for better IDE support.

---
