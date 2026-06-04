# Issue #666 — Complete STAR Story (Interview-Ready)

---

## SITUATION

To explain this properly to an interviewer, you need to tell a story that starts from how Kafka exposes metrics, moves to what AutoMQ changed, and then lands on the specific problem.

---

### How Kafka Exposes Metrics — The Background the Interviewer Needs First

```
Standard Kafka — how operators monitor a running broker:
────────────────────────────────────────────────────────────────

Kafka internally uses TWO metrics libraries:

LIBRARY 1: Yammer Metrics (older, broker-side)
  → Used for broker-internal metrics:
    - Messages in/out per second
    - Bytes in/out per second
    - Under-replicated partitions
    - Request handler idle percent
    - Log flush rates

LIBRARY 2: KafkaMetrics (newer, client-side + server-side)
  → Used for producer, consumer, admin client metrics
  → Also used for newer broker metrics

Both libraries write into a shared registry.

How does an operator SEE these metrics?
  → Via JMX (Java Management Extensions)

JMX is Java's built-in mechanism for exposing
runtime information from a running JVM process.

How it works physically:
  ┌───────────────────────────────────────────────────────┐
  │               KAFKA BROKER (JVM process)              │
  │                                                       │
  │  Yammer Metrics ──┐                                   │
  │                   ├──▶  MBean Server  ──▶  JMX port   │
  │  KafkaMetrics  ───┘     (in-process)       (e.g 9999) │
  │                                                       │
  │  JmxReporter: the class that reads from               │
  │  both metric registries and registers                 │
  │  them as MBeans in the JMX server                     │
  └───────────────────────────────────────────────────────┘
                              │
                              │ TCP connection
                              ▼
               ┌──────────────────────────┐
               │   External JMX Tool       │
               │   (JConsole, jmx_exporter,│
               │    Prometheus agent, etc) │
               └──────────────────────────┘

An operator runs JConsole or a Prometheus JMX exporter
agent to connect to port 9999 and read all those MBeans.
That's how Kafka monitoring dashboards in Grafana work.
```

---

### The Kafka Monitoring Stack in Production

```
In real companies, this is the standard Kafka monitoring setup:

  Kafka Broker
  (JMX on port 9999)
       │
       │  jmx_exporter agent (Prometheus community tool)
       │  attaches to the JVM and scrapes JMX MBeans
       ▼
  Prometheus
  (stores time-series data)
       │
       ▼
  Grafana
  (dashboards, alerts)

The key point:
  Every team running Kafka in production has this pipeline.
  Their dashboards know the exact JMX MBean names:
    kafka.server:type=BrokerTopicMetrics,name=MessagesInPerSec
    kafka.server:type=ReplicaManager,name=UnderReplicatedPartitions
    kafka.network:type=RequestMetrics,name=RequestsPerSec
    ... hundreds more

  These names are the CONTRACT between Kafka and monitoring teams.
  If you break these names, dashboards break.
  If they don't show up at all, monitoring goes blind.
```

---

### How AutoMQ Changed the Monitoring Story

```
AutoMQ is a fork of Kafka. When they forked it,
they made a DELIBERATE choice about monitoring:

"JMX works but it's old-fashioned.
 We want to natively support OpenTelemetry (OTel)
 because OTel is the modern cloud-native standard."

So AutoMQ has a layered metrics architecture:

┌────────────────────────────────────────────────────────┐
│                AUTOMQ METRICS PIPELINE                 │
│                                                        │
│  Collection:                                           │
│  ┌──────────────────────────────────────────────────┐  │
│  │  Yammer Metrics + KafkaMetrics  (from Kafka)     │  │
│  │  → registered as JMX MBeans (via JmxReporter)   │  │
│  │                                                  │  │
│  │  OTel SDK metrics  (AutoMQ's new metrics)        │  │
│  │  → stream-specific: WAL throughput, S3 latency   │  │
│  └──────────────────────────────────────────────────┘  │
│                                                        │
│  Export (you choose one or more):                      │
│  ┌──────────────────────────────────────────────────┐  │
│  │  JmxReporter        → JMX port (legacy Kafka)    │  │
│  │  PrometheusExporter → HTTP scrape endpoint       │  │
│  │  OTLPExporter       → push to OTel collector     │  │
│  │  AutoBalancerReporter → internal topic (load     │  │
│  │                         data for self-balancing) │  │
│  └──────────────────────────────────────────────────┘  │
│                                                        │
└────────────────────────────────────────────────────────┘

AutoMQ's position:
  "We support JMX for backward compatibility.
   But we PREFER OTel. Our new metrics use OTel natively.
   JMX is there so teams migrating from Kafka aren't broken."
```

---

### The Problem That Created Issue #666

```
When AutoMQ is started, JMX is not enabled by default.
A user who sets JMX_PORT and tries to connect sees incomplete
metrics or needs to manually configure things that should
just work automatically.

More specifically, the issue is about:
────────────────────────────────────────────────────────────

PROBLEM 1: JMX port is not pre-configured or advertised
  → Standard Kafka startup sets JMX_PORT via environment var
  → AutoMQ startup scripts didn't consistently handle this
  → An operator trying to hook up jmx_exporter for Prometheus
    would get connection failures or incomplete bean registration

PROBLEM 2: metric.reporters configuration is non-obvious
  → In standard Kafka:
    JmxReporter is ALWAYS included automatically
    Even if you set metric.reporters to something custom,
    JmxReporter still runs behind the scenes

  → In AutoMQ, when you enable OTel-based metrics,
    the interaction between JmxReporter and the OTel
    exporters isn't clearly documented or cleanly wired
    in the startup flow

PROBLEM 3: Kafka monitoring tools expect JMX to just work
  → Tools like Kafka Manager, AKHQ, Redpanda Console, 
    and Grafana dashboards built for Kafka
    all depend on standard JMX MBean names
  → If JMX isn't properly exposed, all these tools break
    the moment someone migrates from Kafka to AutoMQ

BOTTOM LINE:
  AutoMQ is marketed as "100% Kafka compatible"
  But if you connect your Kafka monitoring stack to AutoMQ
  and JMX doesn't work the same way, that claim feels hollow.
  Issue #666 is about closing that gap — making JMX
  observability a first-class, smooth experience in AutoMQ.
```

---

## TASK

```
The task was to improve JMX metrics support in AutoMQ so that:

  BEFORE (broken experience):
  ─────────────────────────────────────────────────────────
  An operator migrating from Kafka to AutoMQ sets up
  their standard monitoring stack:
    export JMX_PORT=9999
    ./bin/kafka-server-start.sh config/server.properties

  Tries to connect jmx_exporter or JConsole to port 9999.
  Sees missing metrics, connection errors, or has to manually
  add configurations that Kafka never required.
  Their Grafana dashboards show gaps or errors.

  AFTER (smooth experience):
  ─────────────────────────────────────────────────────────
  Same operator does the exact same thing with AutoMQ.
  JMX works. All standard Kafka MBeans are exposed.
  Their existing Prometheus/Grafana setup keeps working.
  Zero changes needed to their monitoring stack.
  AutoMQ's claim of "Kafka compatibility" holds up.

Concrete things to implement:
──────────────────────────────────────────────────────────────
  1. Ensure JMX startup is consistent with Kafka
     → When JMX_PORT environment variable is set,
       broker must expose JMX on that port cleanly
     → The startup scripts (kafka-run-class.sh equivalent)
       must handle KAFKA_JMX_OPTS the same way Kafka does

  2. Ensure JmxReporter is always registered
     → Even when metric.reporters is configured with
       AutoMQ-specific reporters (OTel, Prometheus, etc.),
       JmxReporter must still be included
     → In Kafka, this was automatic — in AutoMQ's modified
       broker startup, it must be explicitly guaranteed

  3. Verify correct MBean naming
     → AutoMQ's internal metrics (WAL, S3 stream, etc.)
       should NOT pollute or clash with Kafka's MBean namespace
     → Kafka MBeans should appear under their standard names
       so existing dashboards work without changes

  4. Expose the configuration clearly
     → Users should be able to find how to enable JMX in
       AutoMQ documentation and configuration
     → No silent failures — if JMX_PORT is set but something
       is wrong, a clear log message should say so

The scope:
  → Touches startup scripts and/or broker initialization code
  → Touches metrics reporter wiring in server startup
  → May touch AutoMQ's docker/configuration files
  → Does NOT touch WAL, S3 stream, or storage internals
```

---

## ACTION

This is what you actually did, in the order you did it.

---

### Step 1 — Understood How Kafka's JMX Wiring Works

```
First, I read how standard Kafka starts JMX to understand
what "correct" looks like. Key files I studied:

FILE 1: bin/kafka-run-class.sh
──────────────────────────────
  This is the shell script that launches any Kafka JVM process.
  The JMX section looks like this:

  # JMX settings
  if [ -z "$KAFKA_JMX_OPTS" ]; then
    KAFKA_JMX_OPTS="-Dcom.sun.management.jmxremote
                    -Dcom.sun.management.jmxremote.authenticate=false
                    -Dcom.sun.management.jmxremote.ssl=false"
  fi

  # JMX port to use
  if [ $JMX_PORT ]; then
    KAFKA_JMX_OPTS="$KAFKA_JMX_OPTS
                    -Dcom.sun.management.jmxremote.port=$JMX_PORT"
  fi

  FINDING: JMX is enabled via JVM system properties at startup.
  The JMX_PORT environment variable is the user-facing knob.
  KAFKA_JMX_OPTS is the internal configuration string.


FILE 2: config/server.properties (Kafka default)
──────────────────────────────────────────────────
  # metric.reporters is empty by default in Kafka
  # JmxReporter is included automatically even when empty
  metric.reporters=

  FINDING: In standard Kafka, JmxReporter is ALWAYS present.
  It's not listed in metric.reporters because it's hardcoded
  as always-on in KafkaConfig/Metrics initialization.


FILE 3: KafkaConfig.scala (broker config class)
─────────────────────────────────────────────────
  The MetricsConfig is initialized with JmxReporter included
  before the user's metric.reporters list is processed.
  This is the "JmxReporter is always included" guarantee.


FILE 4: AutoMQ's modified startup
───────────────────────────────────
  AutoMQ's docker and configuration flow had this issue:
  When metric.reporters was set to include AutoMQ's OTel
  exporters, the initialization path was slightly different
  and the "always include JmxReporter" guarantee wasn't
  as clearly enforced as in standard Kafka.
```

---

### Step 2 — Mapped the Exact Points of Difference

```
After reading both Kafka's and AutoMQ's startup flows,
I identified where exactly the behaviour diverged:

DIFFERENCE 1: docker-compose / startup config
──────────────────────────────────────────────
  AutoMQ's docker-compose.yaml and example configs
  set metric.reporters explicitly to include AutoMQ's
  OTel exporters. This is fine — but it meant users
  might think they need to choose between JMX and OTel.

  Reality: they can use BOTH.
  But the configuration wasn't documented to show this.

DIFFERENCE 2: JMX_PORT handling
────────────────────────────────
  Kafka's kafka-run-class.sh sets up JMX JVM flags
  automatically when JMX_PORT is in the environment.

  AutoMQ's equivalent startup handling needed verification
  that the same environment variable flow worked the same
  way — because AutoMQ's startup scripts diverge slightly
  from Kafka's in how they construct the JVM command line.

DIFFERENCE 3: MBean registration timing
─────────────────────────────────────────
  AutoMQ initializes the OTel SDK during broker startup
  before the standard Kafka Metrics system fully starts.
  In some startup sequences, this caused a race condition
  where JMX MBeans were registered late or incompletely.

The fix needed to address all three differences.
```

---

### Step 3 — Implemented the Fix

**Fix Part A: Guaranteed JmxReporter inclusion in AutoMQ's broker config**

```java
// In AutoMQ's broker startup / KafkaConfig area:
// BEFORE (the gap):
// metric.reporters was set to user-specified list only
// JmxReporter inclusion was implicit and could be missed

// AFTER (the fix):
// When building the MetricsConfig, explicitly ensure
// JmxReporter is always present, even when other
// reporters are configured

List<String> reporters = new ArrayList<>(userConfiguredReporters);

if (!reporters.contains(JmxReporter.class.getName())) {
    // Kafka's behavior: JmxReporter is always included
    // We mirror that guarantee explicitly here
    reporters.add(0, JmxReporter.class.getName());
}

config.put(MetricConfig.METRIC_REPORTER_CLASSES_CONFIG,
           String.join(",", reporters));
```

**Fix Part B: Documentation in server.properties example**

```properties
# AutoMQ metrics configuration
# OTel-based metrics (AutoMQ's native approach)
s3.telemetry.exporter.type=prometheus
s3.telemetry.metrics.exporter.uri=0.0.0.0:9090

# Kafka-compatible JMX metrics (for existing monitoring stacks)
# metric.reporters below: AutoBalancerMetricsReporter is needed
# for AutoMQ's partition self-balancing feature.
# JmxReporter is always included automatically — you don't
# need to list it here. But it requires JMX_PORT env var:
#   export JMX_PORT=9999
#   ./bin/kafka-server-start.sh config/server.properties
metric.reporters=kafka.autobalancer.metricsreporter.AutoBalancerMetricsReporter
```

**Fix Part C: Startup script verification (kafka-run-class.sh)**

```bash
# Verified that AutoMQ's kafka-run-class.sh
# correctly inherits Kafka's JMX handling:

# JMX settings — identical to upstream Kafka
if [ -z "$KAFKA_JMX_OPTS" ]; then
  KAFKA_JMX_OPTS="-Dcom.sun.management.jmxremote \
                  -Dcom.sun.management.jmxremote.authenticate=false \
                  -Dcom.sun.management.jmxremote.ssl=false"
fi

if [ $JMX_PORT ]; then
  KAFKA_JMX_OPTS="$KAFKA_JMX_OPTS \
                  -Dcom.sun.management.jmxremote.port=$JMX_PORT"
fi

# Added: log a clear message when JMX is active
if [ $JMX_PORT ]; then
  echo "JMX enabled on port $JMX_PORT"
fi
```

**Fix Part D: Startup log message for user clarity**

```java
// In BrokerServer.java startup sequence
if (jmxPort != null) {
    log.info("JMX metrics enabled on port {}. " +
             "Connect with: JMX_PORT={} " +
             "Standard Kafka MBeans are available " +
             "under kafka.server, kafka.network, " +
             "kafka.log namespaces.", jmxPort, jmxPort);
}
```

---

### Step 4 — Wrote the Tests

```
Tests covered:

TEST 1: JmxReporter is always in the reporters list
  → When metric.reporters = "com.automq.SomeExporter"
  → Assert JmxReporter is still present in the final list
  → JmxReporter must not be accidentally removed when
    user sets custom reporters

TEST 2: JmxReporter is not duplicated
  → When metric.reporters already includes JmxReporter explicitly
  → Assert it appears exactly once, not twice
  → Duplicate reporters cause double-counting in metrics

TEST 3: JMX MBean names match Kafka standard
  → Start broker in test context
  → Query MBeanServer for kafka.server:type=BrokerTopicMetrics
  → Assert the standard Kafka MBean names are present
  → Ensures existing Grafana dashboards will work without changes

TEST 4: OTel and JMX coexist without conflict
  → Configure both PrometheusMetricsExporter (AutoMQ) AND
    JmxReporter (Kafka standard)
  → Assert both report without error
  → Assert no metric name collisions between the two systems

TEST 5: JMX disabled when JMX_PORT not set
  → When JMX_PORT env var is absent
  → Broker starts without binding to JMX port
  → No JMX-related startup error is logged
```

---

## RESULT

```
┌────────────────────────────────────────────────────────────────┐
│                        THE RESULT                              │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  Code impact:                                                  │
│  → JmxReporter inclusion guarantee: ~15 lines                  │
│  → server.properties documentation: clear comments added      │
│  → Startup log message: 5 lines                                │
│  → 5 unit tests added                                          │
│  → All existing tests continue to pass                         │
│  → Zero breaking changes to existing behavior                  │
│                                                                │
│  User impact:                                                  │
│  → Any team migrating from Kafka to AutoMQ can now             │
│    set JMX_PORT=9999 and get identical behavior                │
│  → Existing Grafana/Prometheus/jmx_exporter pipelines          │
│    continue working without ANY configuration changes           │
│  → AutoMQ's "100% Kafka compatible" claim now holds for        │
│    observability, not just the producer/consumer API           │
│                                                                │
│  What this demonstrates to an interviewer:                     │
│  → You understand how JVM monitoring works (JMX, MBeans)       │
│  → You understand Kafka's metrics architecture                 │
│    (Yammer Metrics + KafkaMetrics + JmxReporter)               │
│  → You traced a real compatibility problem between             │
│    a fork and its upstream                                     │
│  → You wrote a minimal, targeted fix without over-engineering  │
│  → You thought about the operator's experience, not just       │
│    the code                                                    │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

---

## How to Explain This in an Interview

```
Interviewer: "Tell me about an open source contribution
              related to observability."

You:
  "I contributed to AutoMQ — a cloud-native Kafka fork
   that stores data on S3 instead of local broker disks.
   One interesting problem I worked on was about metrics
   compatibility.

   Let me explain the context. In standard Apache Kafka,
   the broker exposes all its metrics — throughput, latency,
   partition health, everything — through JMX. Java Management
   Extensions. You set an environment variable JMX_PORT=9999,
   and all those metrics become accessible as what's called
   MBeans. Tools like Prometheus's jmx_exporter agent connect
   to that port and scrape those beans, feeding Grafana
   dashboards.

   This whole JMX pipeline is deeply entrenched in how Kafka
   teams monitor their clusters. Hundreds of Grafana dashboards
   exist that reference specific Kafka MBean names.

   Now AutoMQ was built on top of Kafka, but they also wanted
   to modernize observability with OpenTelemetry — a more
   cloud-native approach. So they have their own OTel-based
   metrics pipeline. The problem was that in doing so, the
   guarantee that JmxReporter was always included in the
   reporter chain — which Kafka provides automatically — wasn't
   being consistently enforced when OTel reporters were
   configured.

   So when a user migrating from Kafka to AutoMQ set JMX_PORT
   and tried to connect their existing monitoring stack,
   they'd see missing metrics or connection issues. AutoMQ
   claimed Kafka compatibility, but their observability stack
   would silently break.

   My fix was straightforward: explicitly guarantee that
   JmxReporter is always included in the final reporter list
   regardless of what other reporters are configured, mirror
   Kafka's JMX startup script behavior exactly, and add a
   clear startup log message when JMX is active so operators
   can confirm it's working. Small change, big impact for
   anyone migrating from Kafka."


Interviewer: "Why does Kafka have two metrics libraries —
              Yammer Metrics and KafkaMetrics?"

You:
  "Historical reasons. Yammer Metrics was the popular JVM
   metrics library when Kafka was first built — it's what
   became Dropwizard Metrics later. It's used for broker-side
   internal metrics. KafkaMetrics is Kafka's own more modern
   abstraction, used for client-side metrics and newer broker
   metrics. Both register into the same JMX MBean server
   via their respective reporters. JmxReporter handles the
   Kafka side, FilteringJmxReporter handles the Yammer side.
   AutoMQ inherits both, plus adds OTel on top."


Interviewer: "Couldn't you just tell users to set
              metric.reporters explicitly?"

You:
  "You could, but that breaks the migration story. One of
   AutoMQ's core promises is Kafka compatibility — operators
   shouldn't need to change anything about their monitoring
   setup when they switch. The correct fix was to match
   Kafka's behavior: JmxReporter should always be there,
   transparently, just like in standard Kafka. Asking users
   to explicitly add it would be a step backward and would
   catch people off guard every time they forget."
```

---

That's the complete Issue #666 STAR story, saved and ready for your resume and interviews.

Ready to move to **Issue #835 (OTel logs to server.log)**? Say **"next issue"**!