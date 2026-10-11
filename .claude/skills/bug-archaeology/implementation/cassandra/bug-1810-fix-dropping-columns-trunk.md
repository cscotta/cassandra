# Bug 1810: Fix dropping columns in trunk

**Commit**: 15963c80ff · **Ticket**: CASSANDRA-6520
**Subsystem**: src/java/org/apache/cassandra/db/CFRowAdder.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

`CFRowAdder.add()` serialized a column value by calling `def.type.decompose(value)` unconditionally. For collection-typed columns (`def.type` is a `CollectionType`), `decompose` expects a full serialized collection, not a bare element value. When a single element (e.g. a map value) was passed in, the wrong serializer was applied, corrupting the cell or triggering an error during schema mutations such as `DROP COLUMN`. The fix detects collection types and extracts the inner `valueComparator()` to decompose the element correctly.

### Before / After

```java
// BEFORE
cf.addColumn(new Cell(name, ((AbstractType)def.type).decompose(value), timestamp));
```

```java
// AFTER
AbstractType valueType = def.type.isCollection()
                       ? ((CollectionType) def.type).valueComparator()
                       : def.type;
cf.addColumn(new Cell(name, valueType.decompose(value), timestamp));
```

---

## How to Spot

Any call-site that calls `type.decompose(value)` without first checking whether `type` is a container type (collection, tuple, UDT) should be scrutinized. Container types require decomposing the inner element via the appropriate sub-type, not the outer collection type.

---

## Test

No test added.
