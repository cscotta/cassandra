# Bug 1836: Fix Divide-by-Zero in PCI

**Commit**: e44d5d4f36 · **Ticket**: CASSANDRA-6403
**Subsystem**: src/java/org/apache/cassandra/db/compaction
**Category**: Missing null / bounds check

---

## Root Cause

`ParallelCompactionIterable`'s single-argument constructor computed the per-scanner in-memory budget by dividing `getInMemoryCompactionLimit()` by `scanners.size()`. When the scanner list was empty (e.g. on a no-op compaction triggered with no SSTables to compact), this produced a divide-by-zero `ArithmeticException`. The fix guards the denominator: if the list is empty, use 1 instead, making the per-scanner limit equal to the full limit — a safe no-op value since no scanners will actually use it.

### Before / After

```java
// BEFORE
this(type, scanners, controller,
     DatabaseDescriptor.getInMemoryCompactionLimit() / scanners.size());
```

```java
// AFTER
this(type, scanners, controller,
     DatabaseDescriptor.getInMemoryCompactionLimit() / (scanners.isEmpty() ? 1 : scanners.size()));
```

---

## How to Spot

Any integer division whose denominator is derived from a collection size is a potential divide-by-zero unless the calling site guarantees non-empty input. During review, verify whether callers are required to pass non-empty collections or whether the constructor must be defensive.

---

## Test

No test added.
