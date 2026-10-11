# Bug 1922: ArrayBackedSortedColumns used for trace session write causes AssertionError

**Commit**: 765d1e97ff · **Ticket**: CASSANDRA-6041
**Subsystem**: src/java/org/apache/cassandra/tracing/Tracing.java
**Category**: Wrong constant / default value

---

## Root Cause

The tracing code created a `ColumnFamily` for the trace sessions table using `ArrayBackedSortedColumns`, which enforces that columns are inserted in sorted order. The trace session write adds multiple columns in a sequence that is not guaranteed to be in ascending comparator order, triggering an `AssertionError` inside the array-backed implementation. Switching to `TreeMapBackedSortedColumns` allows arbitrary insertion order because the tree-map implementation sorts on access rather than enforcing insertion order.

### Before / After

```java
// BEFORE
ColumnFamily cf = ArrayBackedSortedColumns.factory.create(cfMeta);
addColumn(cf, buildName(cfMeta, bytes("coordinator")), FBUtilities.getBroadcastAddress());
addParameterColumns(cf, parameters);
addColumn(cf, buildName(cfMeta, bytes("request")), request);
```

```java
// AFTER
ColumnFamily cf = TreeMapBackedSortedColumns.factory.create(cfMeta);
addColumn(cf, buildName(cfMeta, bytes("coordinator")), FBUtilities.getBroadcastAddress());
addParameterColumns(cf, parameters);
addColumn(cf, buildName(cfMeta, bytes("request")), request);
```

---

## How to Spot

When constructing a `ColumnFamily` by adding columns individually, verify whether insertion order is guaranteed to match comparator order. If columns are added in a potentially non-sorted sequence (e.g., driven by a map or user-supplied parameters), `TreeMapBackedSortedColumns` is the safe default; `ArrayBackedSortedColumns` should only be used when insertion order is provably sorted.

---

## Test

No test added.
