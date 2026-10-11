# Bug 1789: Fix ArrayIndexOutOfBoundsException in Secondary Index Query (1.2 branch)

**Commit**: 0be42463b7 · **Ticket**: CASSANDRA-6470
**Subsystem**: src/java/org/apache/cassandra/db
**Category**: Missing null / bounds check

---

## Root Cause

Same root cause as Bug 1788 but applied to the 1.2 branch where `columnStart` and `columnFinish` are raw `ByteBuffer` values rather than `Composite` objects. `DataRange.Paging.slicesForKey` applied a paging boundary whenever the query key matched the start/stop key, even when the `ByteBuffer` was empty (zero remaining bytes). An empty `ByteBuffer` passed as a slice boundary triggered an `ArrayIndexOutOfBoundsException` in the downstream slice-building code. The fix adds a `hasRemaining()` check before using the buffer as a restriction.

### Before / After

```java
// BEFORE
ByteBuffer newStart  = equals(startKey(), key) ? columnStart  : null;
ByteBuffer newFinish = equals(stopKey(),  key) ? columnFinish : null;
```

```java
// AFTER
ByteBuffer newStart  = equals(startKey(), key) && columnStart.hasRemaining()  ? columnStart  : null;
ByteBuffer newFinish = equals(stopKey(),  key) && columnFinish.hasRemaining() ? columnFinish : null;
```

---

## How to Spot

When using `ByteBuffer` as an optional restriction, treat a buffer with no remaining bytes as absent (`null`) rather than present-but-empty — downstream consumers almost universally assume a provided buffer contains meaningful data.

---

## Test

No test added in this commit. Reproduction requires a secondary-index query whose paging range boundary coincides with a key and has an empty column start or finish buffer.
