# Bug 1455: Nodetool streaming throughput reports wrong unit (MB/s vs Mb/s)

**Commit**: dd4da3e83f · **Ticket**: CASSANDRA-7375
**Subsystem**: src/java/org/apache/cassandra/tools/NodeTool.java
**Category**: Wrong constant / default value

---

## Root Cause

The `getstreamthroughput` and `setstreamthroughput` nodetool commands displayed and documented the throughput unit as `MB/s` (megabytes per second), but the underlying implementation throttles in `Mb/s` (megabits per second). This mismatch caused operators to set throttle values an order of magnitude off from their intent and misread the currently active limit. The fix updates all three occurrences — the command description annotations and the runtime output string — to `Mb/s`.

### Before / After

```java
// BEFORE — misleading unit in description and output
@Command(name = "getstreamthroughput", description = "Print the MB/s throughput cap ...")
System.out.println("Current stream throughput: " + probe.getStreamThroughput() + " MB/s");
```

```java
// AFTER — correct unit
@Command(name = "getstreamthroughput", description = "Print the Mb/s throughput cap ...")
System.out.println("Current stream throughput: " + probe.getStreamThroughput() + " Mb/s");
```

---

## How to Spot

Whenever a CLI tool or API exposes a numeric throughput or bandwidth value, verify that the unit string in the user-facing output matches the unit used by the underlying storage or computation. The distinction between `MB` (megabytes) and `Mb` (megabits) is a factor of eight and is easy to overlook; always cross-reference the unit string against the actual throttle implementation (`StreamManager`, `RateLimiter`, etc.) rather than trusting the display string alone.

---

## Test

No test added.
