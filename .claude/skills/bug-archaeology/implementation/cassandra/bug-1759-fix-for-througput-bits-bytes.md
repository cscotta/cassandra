# Bug 1759: Fix Streaming Throughput Unit Conversion (Bits to Bytes)

**Commit**: ea28d36892 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/streaming/StreamManager.java
**Category**: Wrong constant / default value

---

## Root Cause

`StreamManager.getRateLimiter()` converts the configured throughput value (in megabits per second) to bytes per second for the `RateLimiter`. The formula multiplied by 8 instead of dividing by 8, producing a rate limit 64× too high (8 × 8 = 64 error factor). As a result, the streaming throughput cap was effectively never enforced when a non-zero limit was configured.

### Before / After

```java
// BEFORE — multiplies by 8, giving bits/sec instead of bytes/sec
double currentThroughput = ((double) DatabaseDescriptor.getStreamThroughputOutboundMegabitsPerSec())
    * 1024 * 1024 * 8;
```

```java
// AFTER — divides by 8 to convert megabits → megabytes → bytes
double currentThroughput = (((double) DatabaseDescriptor.getStreamThroughputOutboundMegabitsPerSec())
    * 1024 * 1024) / 8;
```

---

## How to Spot

Unit conversions between bits and bytes are a common source of 8× errors. When converting "megabits per second" to "bytes per second" the correct operation is divide by 8, not multiply. Any throughput or bandwidth calculation should have a comment and a unit test that sanity-checks the magnitude of the result.

---

## Test

No test added.
