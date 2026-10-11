# Bug 0298: Integer Overflow When Converting IntMebibytesBound to Bytes

**Commit**: 495d609c89 · **Ticket**: CASSANDRA-17716
**Subsystem**: src/java/org/apache/cassandra/config/DataStorageSpec.java
**Category**: Wrong constant / default value

---

## Root Cause

`IntMebibytesBound.toBytes()` performed the mebibyte-to-byte conversion using 32-bit integer arithmetic, which silently overflowed for any value larger than ~2 GiB (roughly 2047 MiB). Configuration values such as `compaction_large_partition_warning_threshold`, `min_free_space_per_drive`, and `max_hints_file_size` are stored as `IntMebibytesBound` but returned as `long` byte counts; the overflow caused them to wrap to negative or nonsensically small values. The fix introduces a `toBytesInLong()` method that uses `long` arithmetic for the multiplication and updates all three `DatabaseDescriptor` getters to call it.

### Before / After

```java
// BEFORE
public static long getCompactionLargePartitionWarningThreshold() {
    return conf.compaction_large_partition_warning_threshold.toBytes();  // int overflow
}
```

```java
// AFTER
public static long getCompactionLargePartitionWarningThreshold() {
    return conf.compaction_large_partition_warning_threshold.toBytesInLong();  // long arithmetic
}
```

---

## How to Spot

Any method that converts a unit stored as a 32-bit integer (mebibytes, kibibytes, etc.) to bytes and returns a `long` must perform the multiplication in `long` arithmetic. A simple sign-extension of the result is not sufficient—the multiplication itself must be widened. Review every `toBytes()` style method on configuration spec types and confirm the intermediate product fits in the declared return type.

---

## Test

`DatabaseDescriptorTest#testWidenToLongInBytes` sets `compaction_large_partition_warning_threshold`, `min_free_space_per_drive`, and `max_hints_file_size` to `Integer.MAX_VALUE - 1` MiB and asserts that the returned byte values equal `(long)(Integer.MAX_VALUE - 1) * 1024 * 1024`.
