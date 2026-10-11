# Bug 0442: ClassCastException Querying Virtual Table with Composite Partition Key

**Commit**: 896baf6415 · **Ticket**: CASSANDRA-16155
**Subsystem**: src/java/org/apache/cassandra/db/virtual/SimpleDataSet.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

`SimpleDataSet.makeDecoratedKey()` called `CompositeType.decompose(ByteBufferAccessor.instance, partitionKeyValues)` — a generic accessor-based overload — when building the partition key for a virtual table with multiple partition key columns. `ByteBufferAccessor` expects its input to already be serialized `ByteBuffer` values, but the values passed were raw Java objects (e.g. `String`). The correct overload is `decompose(Object... values)`, which serializes each value through its respective component type. The mismatch caused a `ClassCastException` when the accessor tried to treat the raw object as a `ByteBuffer`.

### Before / After

```java
// BEFORE — wrong overload; raw objects passed to ByteBufferAccessor
: ((CompositeType) metadata.partitionKeyType)
    .decompose(ByteBufferAccessor.instance, partitionKeyValues);
```

```java
// AFTER — correct overload; each value serialized by its component type
: ((CompositeType) metadata.partitionKeyType)
    .decompose(partitionKeyValues);
```

---

## How to Spot

Whenever `ByteBufferAccessor`-parameterized methods are used, verify that the input data is already in `ByteBuffer` form. When building composite keys from higher-level objects, prefer the type-specific `decompose(Object...)` overload. Code reviews on virtual-table implementations should add test coverage for multi-column partition keys because they exercise a distinct code path from single-column keys.

---

## Test

`VirtualTableTest.testQueriesOnTableWithMultiplePks()` creates a virtual table with two partition key columns and two clustering columns, then executes SELECT, DISTINCT, and single-partition queries to assert correct rows are returned without a ClassCastException.
