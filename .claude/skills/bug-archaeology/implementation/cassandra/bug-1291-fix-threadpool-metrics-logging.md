# Bug 1291: ThreadPool Metrics Logging Broken

**Commit**: 944e3be4e3 · **Ticket**: CASSANDRA-9019
**Subsystem**: src/java/org/apache/cassandra/metrics/ThreadPoolMetrics.java, src/java/org/apache/cassandra/utils/StatusLogger.java
**Category**: Missing null / bounds check

---

## Root Cause

Two separate issues combined to break ThreadPool metrics logging. First, `ThreadPoolMetrics.getJmxMetric` called `mbeanServerConn.getAttribute(oName, ...)` without first checking whether the MBean was registered; if a thread pool had not yet published its MBean (e.g., because it had not been used), the call threw `InstanceNotFoundException`, which was swallowed into an unhelpful `RuntimeException`. Second, `StatusLogger` used `System.out.printf` rather than the SLF4J logger, meaning thread pool status was written to stdout instead of the configured logging output, making it invisible in environments where stdout is not monitored.

### Before / After

```java
// BEFORE — no existence check before attribute access
ObjectName oName = new ObjectName(name);
switch (metricName) {
    case "ActiveTasks": ...
```

```java
// AFTER — guard with isRegistered
ObjectName oName = new ObjectName(name);
if (!mbeanServerConn.isRegistered(oName))
{
    return "N/A";
}
switch (metricName) {
    case "ActiveTasks": ...
```

```java
// BEFORE — output to stdout
System.out.printf("%-25s%10s...%n", stage.getJmxName(), ...);
```

```java
// AFTER — output to logger
logger.info(String.format("%-25s%10s...%n", stage.getJmxName(), ...));
```

---

## How to Spot

JMX `ObjectName` lookups should always be preceded by an `isRegistered` check when the MBean's lifecycle is not guaranteed to precede the query. Similarly, any diagnostic or status output path should route through the application's logging framework rather than `System.out` to ensure it respects log level configuration and reaches the correct sink. A quick search for `System.out.printf` or `System.out.println` in server-side code is a reliable way to spot this class of misrouted output.

---

## Test

No test added.
