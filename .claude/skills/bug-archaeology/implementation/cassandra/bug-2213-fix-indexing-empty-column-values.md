# Bug 2213: SSTableWriter Rejects Empty Keys Used by Secondary Index Values

**Commit**: 72dcc298d3 · **Ticket**: CASSANDRA-4832
**Subsystem**: src/java/org/apache/cassandra/io/sstable/SSTableWriter.java
**Category**: Missing null / bounds check

---

## Root Cause

`SSTableWriter.beforeAppend()` contained an assertion `decoratedKey.key.remaining() > 0` that rejected any zero-length key. Secondary index SSTables encode indexed column values as the row key, and empty column values are valid in Cassandra. When a row with an empty column value was indexed, `beforeAppend` threw an `AssertionError`, preventing the index from being written. The fix removes the non-null empty-key assertion while retaining the null check, with a comment explaining that empty keys are intentional for indexed column values.

### Before / After

```java
// BEFORE
assert decoratedKey != null : "Keys must not be null";
assert decoratedKey.key.remaining() > 0 : "Keys must not be empty";
```

```java
// AFTER
assert decoratedKey != null : "Keys must not be null"; // empty keys ARE allowed b/c of indexed column values
```

---

## How to Spot

Blanket input-validation assertions in low-level write paths should be audited against all callers. An assertion that seems universally correct for primary row data may be violated by secondary-index or internal metadata writes that legitimately use empty byte buffers as keys.

---

## Test

No test added.
