# Bug 0407: cassandra-stress JMX Connection Fails When Node Address Includes Port

**Commit**: 2d9d17894d · **Ticket**: CASSANDRA-16473
**Subsystem**: tools/stress/src/org/apache/cassandra/stress/report/StressMetrics.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

`StressMetrics` passed the raw node address strings from `settings.node.resolveAllPermitted()` directly to `JmxCollector`. Those strings can include a port suffix (e.g., `127.0.0.1:9042`), but JMX connection requires only the hostname or IP portion. `JmxCollector` would attempt to open a JMX socket to `127.0.0.1:9042` as a literal hostname, causing a connection failure. The fix strips the port component before passing the addresses to the JMX collector.

### Before / After

```java
// BEFORE
gcStatsCollector = new JmxCollector(settings.node.resolveAllPermitted(settings),
                                    settings.port.jmxPort);
```

```java
// AFTER
gcStatsCollector = new JmxCollector(toJmxNodes(settings.node.resolveAllPermitted(settings)),
                                    settings.port.jmxPort);

// new helper
public static Set<String> toJmxNodes(Set<String> nodes) {
    return nodes.stream().map(n -> n.split(":")[0]).collect(Collectors.toSet());
}
```

---

## How to Spot

When a set of address strings is used for multiple protocols (native client protocol with port, JMX without port), passing them unchanged between subsystems silently breaks the second consumer. During code review, look for places where address strings gathered for one protocol (e.g., CQL at port 9042) are reused for a different protocol (e.g., JMX at a separate port), and verify that port suffixes are stripped before use.

---

## Test

`StressSettingsTest.test16473()` calls `StressMetrics.toJmxNodes()` with a mix of `"127.0.0.1:9042"` and `"127.0.0.1"` and asserts that none of the returned strings contain a colon.
