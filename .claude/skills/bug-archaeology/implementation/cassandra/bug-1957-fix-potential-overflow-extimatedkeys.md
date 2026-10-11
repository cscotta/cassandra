# Bug 1957: Integer overflow in estimatedKeys() for large SSTables

**Commit**: 23d7bd8575 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/io/sstable/SSTableReader.java
**Category**: Off-by-one

---

## Root Cause

`SSTableReader.estimatedKeys()` computed the estimated row count by multiplying `indexSummary.size()` (an `int`) by `DatabaseDescriptor.getIndexInterval()` (also an `int`). Both operands are 32-bit integers, so the multiplication is performed in 32-bit arithmetic before the result is widened to `long`. For large SSTables with many index summary entries the product easily exceeds `Integer.MAX_VALUE`, wrapping around to a large negative number. The fix casts one operand to `long` before the multiplication so the arithmetic is done in 64-bit.

### Before / After

```java
// BEFORE
return indexSummary.size() * DatabaseDescriptor.getIndexInterval();
```

```java
// AFTER
return ((long) indexSummary.size()) * DatabaseDescriptor.getIndexInterval();
```

---

## How to Spot

Whenever the result of an integer multiplication is assigned to a `long`, verify that at least one operand is already a `long` or is explicitly cast to `long` before the multiplication. Java's promotion rules do not automatically widen the intermediate product.

---

## Test

No test added.
