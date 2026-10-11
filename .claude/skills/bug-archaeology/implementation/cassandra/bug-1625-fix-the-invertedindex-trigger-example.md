# Bug 1625: Fix the InvertedIndex trigger example

**Commit**: 820f0eb60b · **Ticket**: CASSANDRA-7211
**Subsystem**: examples/triggers (InvertedIndex)
**Category**: Missing null / bounds check

---

## Root Cause

The `InvertedIndex` trigger example iterated over every column in the incoming `ColumnFamily` update and attempted to create an inverted-index row keyed on the column's value. It did not guard against columns whose value had no remaining bytes (e.g., the implicit row-marker or tombstone cells). Passing an empty `ByteBuffer` as a `RowMutation` key caused an error downstream because an empty key is not a valid Cassandra partition key.

### Before / After

```java
// BEFORE
for (Column cell : update)
{
    RowMutation mutation = new RowMutation(properties.getProperty("keyspace"), cell.value());
    mutation.add(...);
    mutations.add(mutation);
}
```

```java
// AFTER
for (Column cell : update)
{
    // Skip the row marker and other empty values, since they lead to an empty key.
    if (cell.value().remaining() > 0)
    {
        RowMutation mutation = new RowMutation(properties.getProperty("keyspace"), cell.value());
        mutation.add(...);
        mutations.add(mutation);
    }
}
```

---

## How to Spot

Any code that uses a cell's value as an index key or map key without first checking `remaining() > 0` risks creating invalid zero-length keys. During review, flag every `cell.value()` (or `ByteBuffer`) used as a primary/partition key without a non-emptiness guard.

---

## Test

No test added.
