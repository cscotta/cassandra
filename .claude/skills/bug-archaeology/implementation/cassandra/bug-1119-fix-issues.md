# Bug 1119: Integer Overflow and Unsafe Charset in Hints Subsystem

**Commit**: 4cc2b67df3 · **Ticket**: CASSANDRA-6230
**Subsystem**: src/java/org/apache/cassandra/hints/HintsWriter.java, src/java/org/apache/cassandra/hints/LegacyHintsMigrator.java, src/java/org/apache/cassandra/config/DatabaseDescriptor.java
**Category**: Wrong constant / default value

---

## Root Cause

Three related Coverity-flagged defects existed in the hints subsystem. First, `getMaxHintsFileSize()` and two `maybeFsync`/`skipCache` size comparisons multiplied an `int` configuration value by `1024 * 1024` using `int` arithmetic, silently overflowing for any value over ~2 GB before the result was widened to `long`. Second, `HintsWriter` called `String.getBytes()` without specifying a charset, which is platform-dependent and can produce wrong bytes on non-UTF-8 JVMs. Third, `LegacyHintsMigrator` used `||` instead of `&&` to guard a division, meaning a `meanCellCount` of zero would still reach `meanPartitionSize / meanCellCount` and throw `ArithmeticException` when `meanPartitionSize` was non-zero.

### Before / After

```java
// BEFORE — int overflow before widening to long
return conf.max_hints_file_size_in_mb * 1024 * 1024;
if (position() >= lastSyncPosition + getTrickleFsyncIntervalInKb() * 1024)

// AFTER — long literal forces 64-bit arithmetic throughout
return conf.max_hints_file_size_in_mb * 1024L * 1024L;
if (position() >= lastSyncPosition + getTrickleFsyncIntervalInKb() * 1024L)
```

```java
// BEFORE — platform-default charset
out.write(Integer.toHexString((int) globalCRC.getValue()).getBytes());

// AFTER — explicit UTF-8
out.write(Integer.toHexString((int) globalCRC.getValue()).getBytes(StandardCharsets.UTF_8));
```

```java
// BEFORE — || allows division by zero when meanCellCount == 0 but meanPartitionSize != 0
if (meanCellCount != 0 || meanPartitionSize != 0) {
    int avgHintSize = (int) meanPartitionSize / meanCellCount;

// AFTER — && ensures both are non-zero before the division
if (meanCellCount != 0 && meanPartitionSize != 0) {
```

---

## How to Spot

Any multiplication of an `int` configuration field by `1024` or similar constants that ultimately populates a `long` variable should use a long literal (`1024L`) on at least one operand to prevent silent overflow — this is especially important for file-size and offset calculations. For `getBytes()` calls, a missing charset argument is always suspicious; enforce `StandardCharsets.UTF_8` as the standard. For guard conditions that precede a division, verify that the divisor is included in the non-zero check with `&&`, not `||`.

---

## Test

No test added.
