# Bug 0672: ColumnMetadata.cellValueType() Wrong for Frozen Collections

**Commit**: 3960260472 · **Ticket**: CASSANDRA-13573
**Subsystem**: `src/java/org/apache/cassandra/config/ColumnDefinition.java`
**Category**: Logic error in condition

---

## Root Cause

`cellValueType()` unconditionally called `valueComparator()` on any `CollectionType`, including frozen ones. For a frozen collection the entire value is serialized as a single blob, so the cell's value type is the full collection type — not its element value comparator. This caused the wrong deserializer to be used when comparing or serializing frozen collection cells, which was especially visible in Materialized View maintenance and the `sstabledump` JSON output.

### Before / After

```java
// BEFORE
public AbstractType<?> cellValueType()
{
    return type instanceof CollectionType
         ? ((CollectionType)type).valueComparator()
         : type;
}
```

```java
// AFTER
public AbstractType<?> cellValueType()
{
    assert !(type instanceof UserType && type.isMultiCell());
    return type instanceof CollectionType && type.isMultiCell()
            ? ((CollectionType)type).valueComparator()
            : type;
}
```

---

## How to Spot

Whenever a method branches on a type being a `CollectionType`, reviewers should also check whether the collection is multi-cell (non-frozen). Frozen collections are stored as opaque blobs — treating them like non-frozen ones by extracting an element comparator silently applies the wrong type, causing corrupt serialization or incorrect equality checks. A similar audit should be applied to any helper that dispatches on `UserType`.

---

## Test

Tests a frozen `list<tuple<text,text>>` column in a Materialized View, inserting and re-inserting with the same value and asserting the JSON representation is correct after each operation.
