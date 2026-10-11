# Bug 1721: Static Counter Columns Returned Raw Context Instead of Decoded Value

**Commit**: ed93ad4d8c · **Ticket**: CASSANDRA-6827
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/SelectStatement.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

When building the result row for a SELECT, static columns were dispatched to either `getCollectionValue` or `getSimpleValue` depending on `name.type.isCollection()`. Counter columns are neither collections nor plain simple types — they store a raw `CounterContext` byte buffer that must be decoded via `CounterContext.instance().total()` before being serialized back as a `CounterColumnType` long. Because counters were falling through to `getSimpleValue`, the raw context bytes were returned to the client instead of the actual counter value, producing garbled results.

### Before / After

```java
// BEFORE
staticValues.put(name,
    name.type.isCollection()
        ? getCollectionValue(name, group)
        : getSimpleValue(name, group));
```

```java
// AFTER
staticValues.put(name, getValue(name, group));

// getValue() routes counters to the new helper:
private static ByteBuffer getCounterValue(CFDefinition.Name name, ColumnGroupMap columns) {
    Column c = columns.getSimple(name.name.key);
    return c == null ? null
        : CounterColumnType.instance.decompose(CounterContext.instance().total(c.value()));
}
```

---

## How to Spot

When dispatching on column type, look for a binary `isCollection` check and ask whether counter (commutative) columns need separate handling. Any place that calls `getSimpleValue` on a column without first checking `isCommutative()` is a potential site for this class of bug.

---

## Test

No test added.
