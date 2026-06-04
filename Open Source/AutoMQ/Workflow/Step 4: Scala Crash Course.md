# Step 4: Scala Crash Course for Java Developers

The goal here is not to make you a Scala developer. The goal is **surgical** — you need to read `ElasticLog.scala` confidently, understand what it's doing, and write a small focused fix without breaking anything.

We'll cover only what you'll actually encounter in that file.

---

## 4.1 The Mental Model — Scala vs Java

Before syntax, get this mental model right:

```
Scala is NOT a different language that happens to run on JVM.
It IS Java, but with a lot of ceremony removed + some
powerful additions.

Think of it like this:

Java:                           Scala:
─────────────────────────       ──────────────────────────
Verbose, explicit               Concise, inferred
You write everything            Compiler figures a lot out
Strongly typed (explicit)       Strongly typed (inferred)
OOP focused                     OOP + Functional mixed
.java files                     .scala files
Compiles to .class              Compiles to .class (same!)

KEY INSIGHT:
Scala and Java compile to the SAME bytecode.
They can call each other's code freely.
ElasticLog.scala calls Java classes.
Your Java code in other files calls Scala code.
They interoperate completely.
```

---

## 4.2 Variables — val vs var

```scala
// Java:
final String name = "orders";   // can't reassign
String name = "orders";         // can reassign

// Scala:
val name = "orders"             // like Java's final — can't reassign
var name = "orders"             // can reassign

// Type is INFERRED — you don't have to write it
// But you CAN be explicit:
val name: String = "orders"     // explicit type annotation
```

```
Rule of thumb in Scala codebases:
→ Use val everywhere by default
→ Use var only when you truly need to reassign
→ Seeing var in a file means "this changes — pay attention"
```

---

## 4.3 Methods — def

```scala
// Java:
public String greet(String name) {
    return "Hello " + name;
}

// Scala:
def greet(name: String): String = {
    "Hello " + name    // NO return keyword! Last expression is returned
}

// Even shorter (single expression — no braces needed):
def greet(name: String): String = "Hello " + name

// Return type can be inferred too:
def greet(name: String) = "Hello " + name
```

```
What you'll see in ElasticLog.scala:
─────────────────────────────────────────────────────────────

def destroy(): Unit = {           // Unit = Java's void
    // ... 
}

def append(records: MemoryRecords): LogAppendInfo = {
    // last expression is the return value
}

override def read(...): FetchDataInfo = {
    // override works exactly like Java's @Override
}
```

---

## 4.4 Classes and Objects

```scala
// Java class:
public class ElasticLog {
    private final String topicName;
    
    public ElasticLog(String topicName) {
        this.topicName = topicName;
    }
}

// Scala class (constructor is IN the class definition):
class ElasticLog(val topicName: String) {
    // topicName is automatically a field — no separate declaration!
}

// ─────────────────────────────────────────────────────────────

// Scala OBJECT (singleton — like Java's static class):
object ElasticLog {
    // Everything here is "static" in Java terms
    val DEFAULT_PREFIX = "elastic"
    
    def create(...): ElasticLog = {
        new ElasticLog(...)
    }
}
```

```
In ElasticLog.scala you'll see BOTH:

class ElasticLog(...) {
    // instance methods and fields
    // THIS is where destroy() lives
}

object ElasticLog {
    // companion object = static helpers/factories
    // factory methods for creating ElasticLog instances
}

The class and object have the same name — this is called
a "companion object" pattern. Very common in Scala.
```

---

## 4.5 Option — Scala's Null Safety

This is critical to understand because you'll see it everywhere:

```scala
// Java's null problem:
String name = getName();   // might return null
name.length();             // NullPointerException if null!

// Scala's solution — Option:
val name: Option[String] = getName()   // explicitly says "might be absent"

// Option has two states:
// Some("orders")   → value exists
// None             → value is absent (like null, but safe)

// How to use it:
name match {
    case Some(n) => println(n.length)   // safe — n is a String here
    case None    => println("no name")  // handled explicitly
}

// Or shorter:
name.foreach(n => println(n))          // only runs if Some
name.getOrElse("default")              // returns value or default
name.isDefined                         // true if Some, false if None
name.isEmpty                           // true if None
```

```
Why this matters for Issue #1842:

The KV store operations likely return Option types.
When you look up "does this KV entry exist?",
you'll get back Option[SomeType], not null.

You'll need to handle both Some and None cases.
```

---

## 4.6 Pattern Matching — Scala's Super Switch

```scala
// Java switch (limited):
switch (status) {
    case "open":  return 1;
    case "closed": return 2;
    default: return 0;
}

// Scala match (much more powerful):
status match {
    case "open"   => 1
    case "closed" => 2
    case _        => 0      // _ is the wildcard (like default)
}

// Match on types (like Java instanceof but cleaner):
result match {
    case Some(value) => process(value)
    case None        => handleMissing()
}

// Match with conditions:
x match {
    case n if n > 0  => "positive"
    case n if n < 0  => "negative"
    case _           => "zero"
}
```

---

## 4.7 Collections — Lists, Maps, Sequences

```scala
// Java:
List<String> names = new ArrayList<>();
names.add("orders");
names.add("payments");

// Scala:
val names = List("orders", "payments")    // immutable by default
val names = Seq("orders", "payments")     // Seq is more general

// Mutable (when you need it):
val names = scala.collection.mutable.ListBuffer[String]()
names += "orders"
names += "payments"

// Map:
val streamIds = Map("data" -> 42, "meta" -> 43)
val dataStreamId = streamIds("data")      // = 42
val dataStreamId = streamIds.get("data")  // = Some(42), safe

// Common operations (you'll see these in ElasticLog):
names.foreach(n => println(n))    // iterate
names.map(n => n.toUpperCase)     // transform
names.filter(n => n.length > 5)   // filter
names.find(n => n == "orders")    // returns Option[String]
```

---

## 4.8 Traits — Scala's Interface (But More Powerful)

```scala
// Java interface:
public interface Destroyable {
    void destroy();
    default String getName() { return "unnamed"; }
}

// Scala trait (similar, but more powerful):
trait Destroyable {
    def destroy(): Unit              // abstract — must implement
    def getName(): String = "unnamed" // concrete — can override
}

// Implementing:
class ElasticLog extends UnifiedLog with Destroyable {
    override def destroy(): Unit = {
        // your implementation
    }
}

// "with" = Java's "implements"
// A class can extend ONE class and mix in MANY traits
```

---

## 4.9 Implicit Parameters — The Confusing One

You'll see this in the codebase and it looks like magic:

```scala
// This looks like destroy() takes no parameters:
def destroy(): Unit = {
    cleanupStreams()
    deleteKVEntry()   // where does kvClient come from??
}

// But somewhere at the top of the file or class:
implicit val kvClient: KVClient = ...

// And deleteKVEntry is defined as:
def deleteKVEntry()(implicit client: KVClient): Unit = {
    client.delete(partitionKey)
}

// Scala automatically passes kvClient to deleteKVEntry
// because it's marked implicit — you don't see it at call site
```

```
For your purposes:
→ You don't need to fully understand implicit parameters
→ Just know: if a method seems to use something that
  wasn't passed to it, look for "implicit val" declarations
  near the top of the class
→ When you call those methods in your fix,
  the implicit values will be passed automatically
→ You don't need to pass them manually
```

---

## 4.10 Reading ElasticLog.scala — The Actual Patterns

Now let's apply everything. Here are the ACTUAL patterns you'll see in `ElasticLog.scala`:

```scala
// Pattern 1: Class with constructor parameters
class ElasticLog(
    _dir: File,
    config: LogConfig,
    segments: LogSegments,
    ...
    val topicPartition: TopicPartition,   // val = public field
    val producerStateManager: ProducerStateManager,
    ...
) extends LocalLog(_dir, config, segments, ...) {

    // ↑ This is the constructor. All those params are fields.
    // extends = Java's extends
}


// Pattern 2: Companion object with factory method
object ElasticLog {

    def apply(
        dir: File,
        config: LogConfig,
        ...
    ): ElasticLog = {
        // create and return ElasticLog instance
        new ElasticLog(dir, config, ...)
    }
    
    // apply() is Scala's convention for factory methods
    // ElasticLog(...) calls ElasticLog.apply(...)
}


// Pattern 3: The destroy() method you'll modify
override def destroy(): Unit = {
    // Current code (approximate):
    streamMap.values.foreach { stream =>
        stream.close()       // close each stream
        stream.delete()      // delete from S3
    }
    // ← YOUR FIX GOES HERE
    // delete the KV entry that maps this partition
    // to its stream IDs
}


// Pattern 4: KV operations you'll likely call
// (look for how it's written elsewhere in the file)
kvClient.put(partitionKey, streamIds)    // written on create
kvClient.delete(partitionKey)           // what you'll add in destroy
```

---

## 4.11 The Key Syntax Differences — Quick Reference

```
┌────────────────────────────────────────────────────────────┐
│           JAVA vs SCALA — SIDE BY SIDE                     │
├──────────────────────────┬─────────────────────────────────┤
│ Java                     │ Scala                           │
├──────────────────────────┼─────────────────────────────────┤
│ String name = "x";       │ val name = "x"                  │
│ final String name = "x"; │ val name = "x"  (same)          │
│ int x = 5;               │ val x = 5                       │
├──────────────────────────┼─────────────────────────────────┤
│ void destroy() {}        │ def destroy(): Unit = {}        │
│ String get() {}          │ def get(): String = {}          │
│ return value;            │ value  (last expr returned)     │
├──────────────────────────┼─────────────────────────────────┤
│ null                     │ None  (use Option)              │
│ "value" or null          │ Some("value") or None           │
├──────────────────────────┼─────────────────────────────────┤
│ // comment               │ // comment  (same)              │
│ /* comment */            │ /* comment */  (same)           │
├──────────────────────────┼─────────────────────────────────┤
│ implements Interface     │ extends Trait                   │
│ implements A, B          │ extends A with B                │
├──────────────────────────┼─────────────────────────────────┤
│ instanceof               │ isInstanceOf[Type]              │
│ (String) obj             │ obj.asInstanceOf[String]        │
├──────────────────────────┼─────────────────────────────────┤
│ list.forEach(x -> f(x))  │ list.foreach(x => f(x))         │
│ list.stream()            │ list  (already functional)      │
│   .filter(x -> x > 0)    │   .filter(x => x > 0)           │
│   .collect(...)          │   (no collect needed)           │
├──────────────────────────┼─────────────────────────────────┤
│ new ArrayList<>()        │ List() or Seq()                 │
│ new HashMap<>()          │ Map()                           │
├──────────────────────────┼─────────────────────────────────┤
│ obj == null              │ obj == null OR obj.isEmpty      │
│ Objects.equals(a, b)     │ a == b  (safe equality)         │
└──────────────────────────┴─────────────────────────────────┘
```

---

## 4.12 Reading Scala Code — The 3-Pass Technique

When you open `ElasticLog.scala` for the first time, don't try to understand everything. Use this 3-pass approach:

```
PASS 1: Skim the structure (5 minutes)
────────────────────────────────────────────────────────────
→ Find the class definition at the top
→ Note what it extends
→ List all the def names (methods)
→ Don't read bodies yet

You're building a map of the file.


PASS 2: Find your target area (10 minutes)
────────────────────────────────────────────────────────────
→ Search for: "destroy"
→ Read 20 lines above it (context)
→ Read the destroy() body
→ Search for: where is the KV entry WRITTEN?
  (search "kvClient.put" or "kv.put" or similar)
→ That's your template for what to delete


PASS 3: Understand the specific change (20 minutes)
────────────────────────────────────────────────────────────
→ Read destroy() line by line using your Scala knowledge
→ Find the exact KV key format used on create
→ Plan exactly what one line (or a few lines)
  you need to add to destroy()
→ Read nearby code for error handling patterns
  and match them
```

---

## 4.13 Your Scala Survival Toolkit

When you're stuck reading Scala:

```
TOOL 1: IntelliJ IDEA (best for Scala)
────────────────────────────────────────
  → Install the Scala plugin
  → Open the automq project
  → IntelliJ shows types on hover — eliminates guesswork
  → Cmd+Click (or Ctrl+Click) jumps to definition
  → This is your #1 tool for navigating ElasticLog.scala

TOOL 2: Scala REPL for quick experiments
─────────────────────────────────────────
  # Install Scala (if not present)
  brew install scala  # Mac
  sdk install scala   # SDKMAN

  # Start REPL
  scala

  # Try syntax you're unsure about
  scala> val x = List(1, 2, 3)
  scala> x.filter(_ > 1)
  res0: List[Int] = List(2, 3)

TOOL 3: Scastie (online Scala playground)
───────────────────────────────────────────
  https://scastie.scala-lang.org
  → Paste any Scala snippet and run it instantly
  → No setup needed
  → Great for testing a pattern before using it

TOOL 4: When in doubt — read similar code in the file
───────────────────────────────────────────────────────
  Before writing any Scala, find a similar operation
  already in ElasticLog.scala and model yours after it.
  This is the safest approach for a non-Scala developer.
  Consistency with surrounding code is more important
  than "better" Scala style.
```

---

## Step 4 Summary

```
┌────────────────────────────────────────────────────────────┐
│         SCALA FOR JAVA DEVS — KEY TAKEAWAYS                │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  val = final, var = mutable. Use val by default.           │
│                                                            │
│  def method(): ReturnType = { last expr is return }        │
│                                                            │
│  No null — use Option (Some/None) instead                  │
│                                                            │
│  match = powerful switch on values, types, patterns        │
│                                                            │
│  class + object with same name = companion object          │
│  object = singleton (like Java static)                     │
│                                                            │
│  extends Trait = implements Interface (but more)           │
│                                                            │
│  implicit vals = passed automatically — don't panic        │
│                                                            │
│  Strategy for ElasticLog.scala:                            │
│  3-pass read → find destroy() → find where KV is written   │
│  → mirror that pattern for delete                          │
│                                                            │
│  Tools: IntelliJ + Scala plugin, Scastie for testing       │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

---

## 🎉 Foundation Complete!

```
┌────────────────────────────────────────────────────────────┐
│         FULL FOUNDATION — WHAT YOU NOW HAVE                │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  AutoMQ Knowledge:                                         │
│  ✅ Kafka's architecture and its 4 real problems            │
│  ✅ AutoMQ's S3-based architecture and why it works         │
│  ✅ AutoMQ's internals: Stream, ElasticLog, WAL, KV Store   │
│  ✅ Repo structure — know exactly where every file lives    │
│                                                            │
│  Workflow Knowledge:                                       │
│  ✅ Git + open source contribution workflow                 │
│  ✅ How to approach an issue (5-layer framework)            │
│  ✅ How to write tests in AutoMQ/Kafka style                │
│  ✅ Scala syntax for reading/writing ElasticLog.scala       │
│                                                            │
│  Ready for:                                                │
│  → Issue #1244 (--broker param)        ← start here        │
│  → Issue #666  (JMX metrics)                               │
│  → Issue #835  (OTel logs)                                 │
│  → Issue #1842 (metadata cleanup)      ← finish here       │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

Now we move to the real work. Each issue gets treated as a **complete STAR story** — Situation, Task, Action, Result — structured so you can explain it in any interview.

Which issue do we start with? Say **"Issue #1244"** and we dive in!