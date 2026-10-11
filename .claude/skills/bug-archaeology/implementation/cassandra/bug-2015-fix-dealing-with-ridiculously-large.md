# Bug 2015: LeveledManifest Generation Count Shrinks to Zero with Large SSTable Sizes (1.2 branch)

**Commit**: 03f0116611 · **Ticket**: CASSANDRA-5589
**Subsystem**: src/java/org/apache/cassandra/db/compaction/LeveledManifest.java, src/java/org/apache/cassandra/db/compaction/LeveledCompactionStrategy.java
**Category**: Logic error in condition

---

## Root Cause

This is the same fix as commit 98b9cb47f9, applied to the 1.2 release branch. `LeveledManifest` allocated the `generations` array using `(int) Math.log10(1000 * 1000 * 1000 / maxSSTableSizeInMB)`. With a large `maxSSTableSizeInMB` (≥ 1000 MB), the integer division rounds to a value ≤ 1, causing `Math.log10()` to return 0 or negative, collapsing the array length to zero and breaking LCS compaction entirely. The fix hardcodes the divisor to assume a 1-MB SSTable size for generation count purposes, making the array always large enough regardless of configuration.

### Before / After

```java
// BEFORE
int n = (int) Math.log10(1000 * 1000 * 1000 / maxSSTableSizeInMB);
```

```java
// AFTER
int n = (int) Math.log10(1000 * 1000 * 1000);
```

---

## How to Spot

Array sizing formulas that divide a fixed capacity by a user-configured unit size must be protected against large config values that reduce the quotient below 1. When a `Math.log10()` call receives a value ≤ 1, it produces zero or negative, silently zeroing the allocation. Ensure any formula used to size a required data structure has a proven lower bound greater than zero.

---

## Test

No test added.
