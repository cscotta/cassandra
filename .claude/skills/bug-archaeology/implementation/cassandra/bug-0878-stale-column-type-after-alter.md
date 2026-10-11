# Bug 0878: Deserialization Error After Altering Column Type

**Commit**: e4c344c58f · **Ticket**: CASSANDRA-11820
**Subsystem**: Storage engine / row serialization (UnfilteredSerializer)
**Category**: Wrong serialization / deserialization

---

## Root Cause
When serializing intra-node messages (during reads and compaction), `UnfilteredSerializer` obtained the column's type by calling `cell.column()` directly on the cell. If the cell had been deserialized from an SSTable that pre-dated an `ALTER TABLE ... ALTER COLUMN ... TYPE` change, `cell.column()` returned the stale `ColumnDefinition` with the old type. Serializing with the old type but deserializing with the new type (from the current schema header) caused an `EOFException` or data corruption. The fix looks up the `ColumnDefinition` from the `SerializationHeader` (which carries the current schema) using a `SearchIterator`, and passes that current definition — rather than the cell's own stale definition — to `Cell.serializer.serialize()`.

### Before / After
```java
// BEFORE — uses cell's own (possibly stale) ColumnDefinition
for (ColumnData data : row)
{
    if (data.column.isSimple())
        Cell.serializer.serialize((Cell) data, out, pkLiveness, header);
    // header type looked up from data.column() inside serialize()
}

// AFTER — looks up current ColumnDefinition from header before serializing
SearchIterator<ColumnDefinition, ColumnDefinition> si = headerColumns.iterator();
for (ColumnData data : row)
{
    ColumnDefinition column = si.next(data.column());  // current definition
    assert column != null;
    if (data.column.isSimple())
        Cell.serializer.serialize((Cell) data, column, out, pkLiveness, header);
}
// Cell.Serializer.serialize() signature gained an explicit `column` parameter
```

---

## How to Spot
Whenever serialization code obtains type information from the object being serialized (e.g., `cell.column().type`) rather than from the schema context passed to the serializer, it is vulnerable to type staleness after DDL changes. The safe pattern is to pass the schema-authoritative type alongside the data object, keeping the data object's embedded metadata as read-only context.

---

## Test
`AlterTest` was extended with a test that alters a column type and then verifies that reads do not produce `EOFException` or corrupt values.
