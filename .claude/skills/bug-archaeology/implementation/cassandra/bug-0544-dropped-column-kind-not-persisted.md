# Bug 0544: Dropped Column Kind Not Persisted, Allows Re-add With Different Kind

**Commit**: fc862e207b · **Ticket**: CASSANDRA-14948
**Subsystem**: `src/java/org/apache/cassandra/config/CFMetaData.java`, `cql3/statements/AlterTableStatement.java`, `schema/SchemaKeyspace.java`
**Category**: State not cleaned up

---

## Root Cause

When a column was dropped, the `DroppedColumn` record stored only the column's name and type — not its `kind` (regular vs. static). The schema table `dropped_columns` also lacked a `kind` column. Because kind was unknown at re-add validation time, Cassandra allowed a previously-dropped regular column to be re-added as a static column (or vice versa), which is unsafe: the on-disk tombstones and live cells from the old column would be interpreted under the new column's semantics.

### Before / After

```java
// BEFORE
public DroppedColumn(String name, AbstractType<?> type, long droppedTime)
{
    this.name = name;
    this.type = type;
    this.droppedTime = droppedTime;
}
// No kind stored; re-add validation only checked type compatibility
```

```java
// AFTER
public DroppedColumn(String name, ColumnDefinition.Kind kind, AbstractType<?> type, long droppedTime)
{
    this.name = name;
    this.kind = kind;   // nullable; null means legacy/unknown
    this.type = type;
    this.droppedTime = droppedTime;
}
// AlterTableStatement now checks: if droppedColumn.kind != toAdd.kind -> InvalidRequestException
```

---

## How to Spot

When a record about a past schema action is persisted (dropped columns, historical tokens, etc.), review every field of the live object and ask whether each is captured in the stored record. If any semantically-critical field is missing — particularly ones that distinguish column/object kinds or roles — a "round-trip" from disk back to validation will be unable to enforce invariants that were enforced when the original object was created.

---

## Test

Two new tests assert that `ALTER TABLE DROP v; ALTER TABLE ADD v static` (and the reverse) throw `InvalidRequestException`.
