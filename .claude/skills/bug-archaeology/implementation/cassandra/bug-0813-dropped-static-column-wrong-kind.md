# Bug 0813: Dropped static column deserialized as regular column, causing SSTable corrupt exception

**Commit**: 2f0e365dc4 · **Ticket**: CASSANDRA-12582
**Subsystem**: Storage / SSTable Deserialization
**Category**: Wrong serialization / deserialization

---

## Root Cause
When `SerializationHeader` encountered a column name that did not appear in the live schema, it called `CFMetaData.getDroppedColumnDefinition(name)` to obtain a synthetic `ColumnDefinition` for backward compatibility. That method always returned a `regularDef` regardless of whether the column had originally been a `static` column. If the dropped column was static, the fake definition declared it as a regular column, which caused a type mismatch during SSTable deserialization and triggered a corrupt-SSTable exception. The fix checks whether the column name appears in the serialization header's `staticColumns` map and passes that flag to a new overload so the correct `staticDef` is returned.

### Before / After
```java
// BEFORE — always produces a regular column definition
column = metadata.getDroppedColumnDefinition(name);

// AFTER — preserves the static/regular distinction
boolean isStatic = staticColumns.containsKey(name);
column = metadata.getDroppedColumnDefinition(name, isStatic);
```

```java
// BEFORE — single getDroppedColumnDefinition ignores isStatic
return ColumnDefinition.regularDef(this, name, type);

// AFTER — respects the flag
return isStatic
       ? ColumnDefinition.staticDef(this, name, type)
       : ColumnDefinition.regularDef(this, name, type);
```

---

## How to Spot
Any code path that synthesises a `ColumnDefinition` for a dropped column and uses a fixed kind (`regular`) without consulting the serialization context risks this bug. During review, trace every call to `getDroppedColumnDefinition`: if the call site has access to column-kind information (e.g. a `staticColumns` map) but does not pass it, the returned definition may be wrong for static columns.

---

## Test
No new dedicated test was added in this commit; the fix relies on the existing SSTable deserialization test suite to guard regression.
