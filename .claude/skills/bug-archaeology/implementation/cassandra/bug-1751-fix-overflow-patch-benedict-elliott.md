# Bug 1751: Fix Overflow of memtable_total_space_in_mb

**Commit**: 0510d4e5b9 · **Ticket**: CASSANDRA-6753
**Subsystem**: src/java/org/apache/cassandra/config/DatabaseDescriptor.java
**Category**: Off-by-one

---

## Root Cause

`conf.memtable_total_space_in_mb` is declared as an `int`. The expression `conf.memtable_total_space_in_mb << 20` performs the left-shift in 32-bit integer arithmetic, so any value above 2047 MB silently overflows to a negative or truncated `int` before being widened to `long` for the `MemtablePool` constructor. The fix casts the operand to `long` before shifting, ensuring the arithmetic is performed in 64-bit space. Validation bounds for `memtable_cleanup_threshold` were also added in the same commit to guard adjacent configuration parameters.

### Before / After

```java
// BEFORE
.newInstance(conf.memtable_total_space_in_mb << 20, conf.memtable_cleanup_threshold, ...)
```

```java
// AFTER
.newInstance(((long) conf.memtable_total_space_in_mb) << 20, conf.memtable_cleanup_threshold, ...)
```

---

## How to Spot

Any `int` field representing a size in megabytes that is shifted left by 20 (or multiplied by 1048576) to convert to bytes will overflow for values above 2047. During review, flag all MB-to-bytes conversions and verify the intermediate type is `long` before the multiplication or shift.

---

## Test

No test added.
