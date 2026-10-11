# Bug 1917: sstable2json/sstableimport Crashes or Produces Wrong Output for CQL3 Tables

**Commit**: a886e34909 · **Ticket**: CASSANDRA-5852
**Subsystem**: src/java/org/apache/cassandra/config/CFMetaData.java, src/java/org/apache/cassandra/tools/SSTableImport.java
**Category**: Logic error in condition

---

## Root Cause

Two related problems prevented sstable2json from round-tripping CQL3 SSTable data.

First, `CFMetaData.getColumnDefinitionFromColumnName` iterated over column definitions and accessed `components[def.componentIndex]` without checking that `componentIndex` was within the bounds of the split composite column name array. For CQL3 row-marker columns the composite has fewer components than expected, causing an `ArrayIndexOutOfBoundsException` and crashing the tool.

Second, `SSTableImport` looked up the value validator using `meta.getValueValidator(name.duplicate())` which expected a raw `ByteBuffer` column name, but for CQL3 composites the correct call path is `getValueValidator(ColumnDefinition)`. The wrong overload was used, so validators were not found and data was misinterpreted.

### Before / After

```java
// BEFORE (CFMetaData)
ByteBuffer toCompare = def.componentIndex == null ? columnName : components[def.componentIndex];
```

```java
// AFTER (CFMetaData)
if (def.componentIndex == null) {
    toCompare = columnName;
} else {
    if (def.componentIndex >= components.length)
        break;
    toCompare = components[def.componentIndex];
}
```

```java
// BEFORE (SSTableImport)
: stringAsType((String) fields.get(1), meta.getValueValidator(name.duplicate()));
```

```java
// AFTER (SSTableImport)
: stringAsType((String) fields.get(1), meta.getValueValidator(meta.getColumnDefinitionFromColumnName(name)));
```

---

## How to Spot

Any code that splits a composite column name into components and then indexes into the array using a stored `componentIndex` must guard against the index being out of range—row markers and collection tombstones produce shorter composites than regular data columns. Also, when multiple overloads of a lookup method exist (by raw name vs. by definition), verify that the correct one is called for the table type in use.

---

## Test

No test added.
