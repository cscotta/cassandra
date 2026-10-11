# Bug 1814: Fix AbstractCellName.makeCellName for collections

**Commit**: 486f0792d4 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/composites/AbstractCellNameType.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

`makeCellName(Object... components)` decomposed each component using `subtype(i)` directly. When the final subtype is a `ColumnToCollectionType` (the special marker component that tracks collection column definitions), that type cannot decompose an arbitrary key value — instead the correct serializer must be obtained by looking up the specific collection definition and calling `nameComparator()` on it. Without this lookup, the wrong type was used to serialize the collection key component, producing an incorrect cell name.

### Before / After

```java
// BEFORE
rawComponents[i] = c instanceof ByteBuffer ? (ByteBuffer)c : ((AbstractType)subtype(i)).decompose(c);
```

```java
// AFTER
AbstractType<?> type = (AbstractType)subtype(i);
if (type instanceof ColumnToCollectionType)
{
    assert i > 0;
    type = ((ColumnToCollectionType)type).defined.get(rawComponents[i-1]).nameComparator();
}
rawComponents[i] = ((AbstractType)type).decompose(c);
```

---

## How to Spot

When iterating over composite type components and calling `decompose`, any subtype that is itself a meta/marker type (like `ColumnToCollectionType`) requires indirection to the actual element type. Review every `subtype(i).decompose(...)` call to confirm the subtype is concrete and directly applicable to the value being serialized.

---

## Test

No test added.
