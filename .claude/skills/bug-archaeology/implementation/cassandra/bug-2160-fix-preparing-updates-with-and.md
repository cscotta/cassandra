# Bug 2160: Prepared statement bound names not resolved for collection operations in CQL3

**Commit**: a90b310e61 · **Ticket**: CASSANDRA-5017
**Subsystem**: src/java/org/apache/cassandra/cql3/operations/, src/java/org/apache/cassandra/cql3/statements/
**Category**: Wrong serialization / deserialization

---

## Root Cause

When preparing a CQL3 UPDATE with collection column operations (list, set, map), the `addBoundNames` path was missing from every `Operation` implementation. The `prepare()` path only knew how to iterate `operation.getValues()` and assign each bind marker directly to the column's `CFDefinition.Name`, which is too coarse: list index-by-position and map put-by-key each require typed `ColumnSpecification` descriptors so the driver can decode the bound values correctly. Additionally, the grammar could not statically disambiguate `col[?] = ?` as either a list-by-index or a map-put, so the parser always produced a `ListOperation.SetIndex`; at execution time the code now checks the actual column type and re-dispatches to `MapOperation.Put` when the column is a `MapType`.

### Before / After

```java
// BEFORE (UpdateStatement.prepare) - raw value loop, loses type info
for (Term t : operation.getValues())
    if (t.isBindMarker())
        boundNames[t.bindIndex] = name;  // name is CFDefinition.Name, not typed spec
```

```java
// AFTER - each Operation implementation fills in properly typed ColumnSpecification
operation.addBoundNames(name, boundNames);
// e.g. ListOperation.addBoundNames produces indexSpecOf/valueSpecOf with Int32Type/element type
// e.g. MapOperation.addBoundNames produces keySpecOf/valueSpecOf with actual key/value types
```

---

## How to Spot

Whenever an `Operation` interface gains a new execution path, check that a corresponding metadata/introspection path (here `addBoundNames`) exists and is implemented by every concrete class. If an interface method has a no-op or fallback that silently discards type information, prepared-statement metadata will be wrong or absent.

---

## Test

No new test added in this commit; coverage was provided by integration-level prepared-statement tests for collection columns.
