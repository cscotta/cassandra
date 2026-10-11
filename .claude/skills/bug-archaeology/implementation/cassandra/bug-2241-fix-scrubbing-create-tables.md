# Bug 2241: CQL3 Table Column Lookup Failed Due to Composite Column Name Mismatch

**Commit**: 2d76556742 · **Ticket**: CASSANDRA-4685
**Subsystem**: src/java/org/apache/cassandra/config/CFMetaData.java, src/java/org/apache/cassandra/thrift/ThriftValidation.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`CFMetaData.getColumnDefinition(ByteBuffer)` was a simple map lookup using the raw column name bytes. For CQL3 tables, columns are stored under a `CompositeType` comparator, so the raw column name passed in is a composite byte buffer that encodes multiple components. A direct map lookup against the per-column `ColumnDefinition` name (which is just the logical column name component) would always miss, returning `null`. The fix splits the composite column name into its components and compares each `ColumnDefinition` against its relevant component index, correctly resolving the definition. `ThriftValidation` previously duplicated this split logic locally; that duplicate was removed now that `getColumnDefinition` handles it correctly.

### Before / After

```java
// BEFORE — in CFMetaData
public ColumnDefinition getColumnDefinition(ByteBuffer name) {
    return column_metadata.get(name);  // raw lookup, fails for CompositeType
}
```

```java
// AFTER — in CFMetaData
public ColumnDefinition getColumnDefinition(ByteBuffer columnName) {
    if (comparator instanceof CompositeType) {
        CompositeType composite = (CompositeType)comparator;
        ByteBuffer[] components = composite.split(columnName);
        for (ColumnDefinition def : column_metadata.values()) {
            ByteBuffer toCompare = def.componentIndex == null ? columnName : components[def.componentIndex];
            if (def.name.equals(toCompare))
                return def;
        }
        return null;
    } else {
        return column_metadata.get(columnName);
    }
}
```

---

## How to Spot

Any `Map.get(rawColumnName)` lookup where the map key is a logical column name but the input may be a multi-component composite byte buffer will silently return `null` for CQL3 tables. Watch for column-definition resolution that bypasses the composite-aware splitting logic.

---

## Test

No test added.
