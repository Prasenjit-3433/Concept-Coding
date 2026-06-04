# Step 3: Writing Tests in AutoMQ/Kafka Style

Testing is the most common reason PRs get rejected or sent back for rework. Maintainers of mature projects like Kafka take tests very seriously. A PR without proper tests simply won't get merged.

---

## 3.1 The Testing Philosophy in Kafka/AutoMQ

First, understand the mindset:

```
Kafka has been in production at thousands of companies
for 10+ years. Every change carries real risk.

Maintainers think:
─────────────────────────────────────────────────────────────
"If I merge this, will it break something in production
 for a company processing 10 million messages/second?"

Tests are their safety net. If your tests are weak,
they can't trust your change. Period.

What reviewers look for in tests:
─────────────────────────────────────────────────────────────
✅ Does the happy path work?
✅ Do edge cases fail gracefully with clear errors?
✅ Is existing behavior unchanged? (regression tests)
✅ Are tests isolated? (no dependency on external services)
✅ Are tests fast? (unit tests, not integration tests)
✅ Are tests readable? (test name explains what it verifies)
```

---

## 3.2 The Three Types of Tests in Kafka/AutoMQ

```
┌──────────────────────────────────────────────────────────────┐
│                  TEST PYRAMID                                │
│                                                              │
│                      ▲ Integration Tests                     │
│                     ╱╲  (few, slow, test full flow)          │
│                    ╱  ╲  e.g. actual broker + producer       │
│                   ╱────╲                                     │
│                  ╱ Unit  ╲ Unit Tests                        │
│                 ╱  Tests  ╲ (many, fast, isolated)           │
│                ╱────────────╲ mock everything external       │
│               ╱   Unit Tests ╲                               │
│              ╱────────────────╲                              │
│                                                              │
│  For your 4 issues:                                          │
│  → #1244, #666, #835 → Unit tests are sufficient             │
│  → #1842 → Unit tests + possibly one integration test        │
└──────────────────────────────────────────────────────────────┘

Test types you'll write:
─────────────────────────────────────────────────────────────
TYPE 1: Unit Test
  → Tests one class/method in isolation
  → Mock all external dependencies
  → Should run in milliseconds
  → Lives in: src/test/java/.../<ClassName>Test.java

TYPE 2: Integration Test
  → Tests multiple components together
  → May use real Kafka broker or embedded broker
  → Slower (seconds to minutes)
  → Lives in: src/test/java/.../integration/

For your first PRs: focus entirely on unit tests.
```

---

## 3.3 The Anatomy of a Kafka/AutoMQ Test Class

Let's look at the exact structure you'll follow:

```java
/*
 * Standard Kafka license header (copy from any existing file)
 * Licensed to the Apache Software Foundation (ASF)...
 */
package org.apache.kafka.tools;   // same package as the class under test

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Kafka uses JUnit 5 (Jupiter) — NOT JUnit 4.
 * Watch out: @Test is from org.junit.jupiter.api.Test
 * NOT from org.junit.Test (that's JUnit 4 — wrong!)
 */
public class ProducerPerformanceTest {

    // ─── SETUP & TEARDOWN ──────────────────────────────────

    @BeforeEach
    void setUp() {
        // runs before each test
        // initialize shared objects here
    }

    @AfterEach
    void tearDown() {
        // runs after each test
        // clean up resources
    }

    // ─── TEST NAMING CONVENTION ────────────────────────────
    //
    // Kafka uses this pattern:
    // <methodUnderTest>_<scenario>_<expectedOutcome>
    //
    // Examples:
    //   partition_withEligibleBrokers_routesToCorrectPartition
    //   partition_withNoBrokerMatch_throwsException
    //   argParser_withBrokerParam_parsesCorrectly
    //   start_withoutBrokerParam_behaviorUnchanged

    @Test
    void argParser_withBrokerParam_parsesCorrectly() {
        // test body
    }

    @Test
    void partition_withEligibleBrokers_routesToCorrectPartition() {
        // test body
    }
}
```

---

## 3.4 The AAA Pattern — How Every Test Is Structured

Every single test follows this structure without exception:

```
AAA = Arrange → Act → Assert

ARRANGE: Set up everything the test needs
         (create objects, mock dependencies, prepare data)

ACT:     Call the ONE thing you're testing
         (one method call, one operation)

ASSERT:  Verify the result is what you expected
         (check return value, check state, check exceptions)
```

Let's see this in practice:

```java
@Test
void partition_withEligibleBrokers_routesToCorrectPartition() {

    // ── ARRANGE ──────────────────────────────────────────────

    // Create the partitioner
    BrokerBoundPartitioner partitioner = new BrokerBoundPartitioner();

    // Configure it with target broker IDs
    Map<String, Object> configs = new HashMap<>();
    configs.put("broker.bound.partitioner.brokers", "1,3");
    partitioner.configure(configs);

    // Build a fake cluster with known topology
    // Broker 1 owns partition 0 and 2
    // Broker 2 owns partition 1 and 3
    // Broker 3 owns partition 4
    Node broker1 = new Node(1, "host1", 9092);
    Node broker2 = new Node(2, "host2", 9092);
    Node broker3 = new Node(3, "host3", 9092);

    List<PartitionInfo> partitions = Arrays.asList(
        new PartitionInfo("orders", 0, broker1, new Node[]{}, new Node[]{}),
        new PartitionInfo("orders", 1, broker2, new Node[]{}, new Node[]{}),
        new PartitionInfo("orders", 2, broker1, new Node[]{}, new Node[]{}),
        new PartitionInfo("orders", 3, broker2, new Node[]{}, new Node[]{}),
        new PartitionInfo("orders", 4, broker3, new Node[]{}, new Node[]{})
    );

    Cluster cluster = new Cluster(
        "clusterId",
        Arrays.asList(broker1, broker2, broker3),
        partitions,
        Collections.emptySet(),
        Collections.emptySet()
    );

    // ── ACT ──────────────────────────────────────────────────

    // Call partition() multiple times
    Set<Integer> selectedPartitions = new HashSet<>();
    for (int i = 0; i < 20; i++) {
        int partition = partitioner.partition(
            "orders",
            null, null,   // key
            null, null,   // value
            cluster
        );
        selectedPartitions.add(partition);
    }

    // ── ASSERT ───────────────────────────────────────────────

    // Should only have picked partitions 0, 2, 4
    // (leaders: broker1, broker1, broker3)
    // Should NEVER have picked partitions 1 or 3
    // (leader: broker2, which is not in [1,3])
    assertTrue(selectedPartitions.contains(0));
    assertTrue(selectedPartitions.contains(2));
    assertTrue(selectedPartitions.contains(4));
    assertFalse(selectedPartitions.contains(1));
    assertFalse(selectedPartitions.contains(3));
}
```

---

## 3.5 Testing Edge Cases — The Complete Set for Issue #1244

Here are ALL the tests you need to write. This is not optional — each one tests a real scenario:

```java
// ── TEST 1: Happy path ────────────────────────────────────────
// Verified above in 3.4

// ── TEST 2: No matching partitions ───────────────────────────
@Test
void partition_withNoBrokerMatch_throwsRuntimeException() {

    // ARRANGE
    BrokerBoundPartitioner partitioner = new BrokerBoundPartitioner();
    Map<String, Object> configs = new HashMap<>();
    configs.put("broker.bound.partitioner.brokers", "99"); // broker 99 doesn't exist
    partitioner.configure(configs);

    Node broker1 = new Node(1, "host1", 9092);
    List<PartitionInfo> partitions = Arrays.asList(
        new PartitionInfo("orders", 0, broker1, new Node[]{}, new Node[]{})
    );
    Cluster cluster = new Cluster("id", List.of(broker1),
        partitions, emptySet(), emptySet());

    // ACT + ASSERT
    // When partition() is called with no eligible partitions,
    // it should throw a clear exception
    assertThrows(RuntimeException.class, () ->
        partitioner.partition("orders", null, null, null, null, cluster)
    );
}


// ── TEST 3: Single broker, single eligible partition ──────────
@Test
void partition_withSingleEligiblePartition_alwaysReturnsSamePartition() {

    // ARRANGE
    BrokerBoundPartitioner partitioner = new BrokerBoundPartitioner();
    Map<String, Object> configs = new HashMap<>();
    configs.put("broker.bound.partitioner.brokers", "1");
    partitioner.configure(configs);

    Node broker1 = new Node(1, "host1", 9092);
    Node broker2 = new Node(2, "host2", 9092);
    List<PartitionInfo> partitions = Arrays.asList(
        new PartitionInfo("orders", 0, broker1, new Node[]{}, new Node[]{}),
        new PartitionInfo("orders", 1, broker2, new Node[]{}, new Node[]{}),
        new PartitionInfo("orders", 2, broker2, new Node[]{}, new Node[]{})
    );
    Cluster cluster = new Cluster("id",
        List.of(broker1, broker2), partitions, emptySet(), emptySet());

    // ACT
    Set<Integer> seen = new HashSet<>();
    for (int i = 0; i < 10; i++) {
        seen.add(partitioner.partition(
            "orders", null, null, null, null, cluster));
    }

    // ASSERT
    // Only partition 0 is eligible, so always returns 0
    assertEquals(Set.of(0), seen);
}


// ── TEST 4: Argument parsing ──────────────────────────────────
@Test
void argParser_withBrokerArgument_parsesCommaSeparatedValues()
    throws ArgumentParserException {

    // ARRANGE
    ArgumentParser parser = ProducerPerformance.argParser();

    // ACT
    Namespace result = parser.parseArgs(new String[]{
        "--topic", "test",
        "--num-records", "100",
        "--record-size", "100",
        "--throughput", "-1",
        "--broker", "1,2,3"   // ← the new argument
    });

    // ASSERT
    assertEquals("1,2,3", result.getString("broker"));
}


// ── TEST 5: Backward compatibility ───────────────────────────
@Test
void argParser_withoutBrokerArgument_brokerIsNull()
    throws ArgumentParserException {

    // ARRANGE
    ArgumentParser parser = ProducerPerformance.argParser();

    // ACT
    Namespace result = parser.parseArgs(new String[]{
        "--topic", "test",
        "--num-records", "100",
        "--record-size", "100",
        "--throughput", "-1"
        // no --broker argument
    });

    // ASSERT
    // When --broker not specified, it should be null
    // (existing behavior unchanged)
    assertNull(result.getString("broker"));
}


// ── TEST 6: Round-robin distribution ─────────────────────────
@Test
void partition_withMultipleEligiblePartitions_distributesEvenly() {

    // ARRANGE
    BrokerBoundPartitioner partitioner = new BrokerBoundPartitioner();
    Map<String, Object> configs = new HashMap<>();
    configs.put("broker.bound.partitioner.brokers", "1");
    partitioner.configure(configs);

    Node broker1 = new Node(1, "host1", 9092);
    List<PartitionInfo> partitions = Arrays.asList(
        new PartitionInfo("orders", 0, broker1, new Node[]{}, new Node[]{}),
        new PartitionInfo("orders", 1, broker1, new Node[]{}, new Node[]{}),
        new PartitionInfo("orders", 2, broker1, new Node[]{}, new Node[]{})
    );
    Cluster cluster = new Cluster("id",
        List.of(broker1), partitions, emptySet(), emptySet());

    // ACT
    Map<Integer, Integer> counts = new HashMap<>();
    for (int i = 0; i < 30; i++) {
        int p = partitioner.partition(
            "orders", null, null, null, null, cluster);
        counts.merge(p, 1, Integer::sum);
    }

    // ASSERT
    // 30 messages across 3 partitions = 10 each (perfect round-robin)
    assertEquals(10, counts.get(0));
    assertEquals(10, counts.get(1));
    assertEquals(10, counts.get(2));
}
```

---

## 3.6 What NOT to Do in Tests

These are common mistakes that will get your PR sent back:

```
❌ MISTAKE 1: Testing implementation details, not behavior
──────────────────────────────────────────────────────────
BAD:
  verify(mockPartitioner, times(3)).partition(...);
  // This tests HOW it works internally, not WHAT it does

GOOD:
  assertEquals(expectedPartition, actualPartition);
  // This tests the observable output


❌ MISTAKE 2: Multiple assertions testing different things
──────────────────────────────────────────────────────────
BAD: One giant test that tests everything at once

GOOD: One test = one scenario = one focused assertion


❌ MISTAKE 3: Hardcoding magic numbers without explanation
──────────────────────────────────────────────────────────
BAD:
  assertEquals(42, result);

GOOD:
  // With 3 eligible partitions and 30 sends,
  // each partition should receive exactly 10 messages
  assertEquals(10, counts.get(0));


❌ MISTAKE 4: Tests that depend on each other
──────────────────────────────────────────────────────────
BAD:
  // Test 2 assumes Test 1 ran first and set some state

GOOD:
  Each test sets up its own state from scratch in @BeforeEach
  Tests can run in any order and still pass


❌ MISTAKE 5: Ignoring exceptions instead of asserting them
──────────────────────────────────────────────────────────
BAD:
  try {
      partitioner.partition(...);
  } catch (Exception e) {
      // ignored
  }

GOOD:
  assertThrows(RuntimeException.class, () ->
      partitioner.partition(...)
  );
```

---

## 3.7 Build and Run Tests Locally Before Pushing

Never push code with failing tests. This is non-negotiable.

```
Run tests for your specific module:
────────────────────────────────────────────────────────────
  ./gradlew :tools:test

Run a specific test class only:
────────────────────────────────────────────────────────────
  ./gradlew :tools:test \
    --tests "org.apache.kafka.tools.ProducerPerformanceTest"

Run a specific test method:
────────────────────────────────────────────────────────────
  ./gradlew :tools:test \
    --tests "org.apache.kafka.tools.ProducerPerformanceTest.partition_withEligibleBrokers_routesToCorrectPartition"

See test output in detail (not just pass/fail):
────────────────────────────────────────────────────────────
  ./gradlew :tools:test --info

What passing output looks like:
────────────────────────────────────────────────────────────
  > Task :tools:test
  ProducerPerformanceTest > argParser_withBrokerParam... PASSED
  ProducerPerformanceTest > partition_withEligibleBrokers... PASSED
  ProducerPerformanceTest > partition_withNoBrokerMatch... PASSED
  ProducerPerformanceTest > partition_withSingleEligible... PASSED
  ProducerPerformanceTest > argParser_withoutBroker... PASSED
  ProducerPerformanceTest > partition_distributes... PASSED

  BUILD SUCCESSFUL in 45s
```

---

## 3.8 Test Coverage Expectations Per Issue Type

Not every issue needs the same depth of tests. Here's the realistic expectation:

```
┌──────────────────────────────────────────────────────────────┐
│          TEST EXPECTATIONS BY ISSUE TYPE                     │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  Issue #1244 — New CLI feature                               │
│  Expected: 4-6 unit tests                                    │
│  Covering: arg parsing, happy path, edge cases,              │
│            backward compat                                   │
│                                                              │
│  Issue #666 — JMX metrics exposure                           │
│  Expected: 2-4 unit tests                                    │
│  Covering: metrics registered correctly,                     │
│            correct values reported                           │
│                                                              │
│  Issue #835 — OTel log routing                               │
│  Expected: 2-3 unit tests                                    │
│  Covering: OTel logs appear in SLF4J output,                 │
│            original logging behavior unchanged               │
│                                                              │
│  Issue #1842 — Metadata cleanup                              │
│  Expected: 3-5 unit tests                                    │
│  Covering: KV entry deleted after destroy(),                 │
│            streams also deleted,                             │
│            no-op if entry doesn't exist                      │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

---

## 3.9 The Test Quality Checklist

Before pushing your code, go through this:

```
┌────────────────────────────────────────────────────────────┐
│               TEST QUALITY CHECKLIST                       │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  □ Using JUnit 5 (org.junit.jupiter.api.Test)              │
│  □ Test names follow: method_scenario_expectedOutcome      │
│  □ Every test follows AAA: Arrange → Act → Assert          │
│  □ Each test tests exactly ONE thing                       │
│  □ Happy path tested                                       │
│  □ At least 2-3 edge cases tested                          │
│  □ Backward compatibility tested                           │
│  □ No tests depend on each other                           │
│  □ No magic numbers without explanation                    │
│  □ All tests pass locally before pushing                   │
│  □ ./gradlew :module:test runs clean                       │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

---

## Step 3 Summary

```
┌────────────────────────────────────────────────────────────┐
│          TESTING IN AUTOMQ — KEY PRINCIPLES                │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  Tests are not optional — no tests = no merge              │
│                                                            │
│  Framework: JUnit 5 (Jupiter) + Mockito                    │
│                                                            │
│  Every test = AAA pattern                                  │
│  Arrange → Act → Assert                                    │
│                                                            │
│  Naming: method_scenario_expectedOutcome                   │
│                                                            │
│  Cover: happy path + edge cases + backward compat          │
│                                                            │
│  Run locally and pass before every push                    │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

---

## What's Next

**Step 4: Scala Crash Course for Java Developers** — a focused, minimal guide to reading and writing Scala confidently enough to work on `ElasticLog.scala` for Issue #1842. We won't learn all of Scala — just the specific syntax patterns you'll actually encounter in that file.

Ready? Say **"next"**!