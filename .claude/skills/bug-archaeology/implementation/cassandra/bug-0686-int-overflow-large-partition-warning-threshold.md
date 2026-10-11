# Bug 0686: Integer Overflow When Computing Large Partition Warning Threshold in Bytes

**Commit**: f3e38cb638 · **Ticket**: CASSANDRA-13172
**Subsystem**: `src/java/org/apache/cassandra/config/DatabaseDescriptor.java`
**Category**: Wrong constant / default value

---

## Root Cause

`getCompactionLargePartitionWarningThreshold()` multiplied the configuration value (in MiB, stored as an `int`) by the integer literals `1024 * 1024`. Because all three operands are `int`, the multiplication is performed in 32-bit arithmetic and overflows when the configured threshold exceeds ~2047 MiB. The result was a negative or truncated byte value used to decide when to warn about large partitions, effectively disabling the warning for large thresholds. The fix changes the return type to `long` and forces the multiplication to use 64-bit arithmetic by making one literal a `long` (`1024L`).

### Before / After

```java
// BEFORE
public static int getCompactionLargePartitionWarningThreshold() {
    return conf.compaction_large_partition_warning_threshold_mb * 1024 * 1024;
}
```

```java
// AFTER
public static long getCompactionLargePartitionWarningThreshold() {
    return conf.compaction_large_partition_warning_threshold_mb * 1024L * 1024L;
}
```

---

## How to Spot

Any conversion of a unit-bearing `int` configuration value to bytes via `* 1024 * 1024` (or `* 1_000_000`) without a `L` suffix on at least one literal will silently overflow for values above 2 GiB. A reviewer should check the return type is `long` and that the first multiplication operand is promoted to `long` before the chain continues.

---

## Test

No test added.
