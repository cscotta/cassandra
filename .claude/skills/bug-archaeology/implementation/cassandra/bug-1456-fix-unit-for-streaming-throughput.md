# Bug 1456: Wrong Unit Label for Streaming Throughput in nodetool

**Commit**: 9783777c2a · **Ticket**: CASSANDRA-7375
**Subsystem**: src/java/org/apache/cassandra/tools/NodeCmd.java
**Category**: Wrong constant / default value

---

## Root Cause

The nodetool commands `getstreamthroughput` and `setstreamthroughput` labelled their throughput values as `MB/s` (megabytes per second) when the underlying setting is actually in `Mb/s` (megabits per second). The print string and the YAML help text both used the wrong unit string. Because the label is a string literal with no compile-time enforcement, the discrepancy between what is displayed and what the value actually represents was invisible to the compiler.

### Before / After

```java
// BEFORE
outs.println("Current stream throughput: " + probe.getStreamThroughput() + " MB/s");
```

```java
// AFTER
outs.println("Current stream throughput: " + probe.getStreamThroughput() + " Mb/s");
```

---

## How to Spot

Whenever a diagnostic or CLI tool displays a numeric value with a unit suffix, review whether the suffix matches the actual unit of the underlying API or storage field. A common trap is copy-pasting from a related command (e.g. `compaction throughput` which is MB/s) and forgetting that the new command uses a different unit. Code review should cross-check the label against the corresponding setter/getter documentation or configuration definition.

---

## Test

No test added.
