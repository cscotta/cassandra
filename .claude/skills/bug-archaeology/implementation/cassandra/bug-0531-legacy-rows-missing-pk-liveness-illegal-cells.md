# Bug 0531: Legacy Rows Missing Primary Key Liveness When Containing Only Illegal Cells

**Commit**: 767a68cd00 · **Ticket**: CASSANDRA-15365
**Subsystem**: `src/java/org/apache/cassandra/db/LegacyLayout.java`
**Category**: Logic error in condition

---

## Root Cause

In Cassandra 2.1, cells whose column name matched a primary key column were written by offline SSTable generators and treated as present-but-unqueryable — their existence kept the CQL row visible even though the cell data was dropped. When 3.x's `CellGrouper` encountered these "illegal" cells during legacy SSTable reading, it simply warned and discarded them with no side effect. This meant that a partition that contained only such illegal cells (and nothing else) would produce an empty row with no primary key liveness info, making the row disappear entirely from query results, which violated the 2.1 behavior where such rows were still returned.

### Before / After

```java
// BEFORE: illegal PK cell just warns and returns
if (column.isPrimaryKeyColumn() && metadata.isCQLTable())
{
    noSpamLogger.warn("Illegal cell name for CQL3 table ...");
    return true;  // silently dropped, no liveness info saved
}
```

```java
// AFTER: illegal PK cell saves its liveness info for later use
else if (column.isPrimaryKeyColumn() && metadata.isCQLTable())
{
    noSpamLogger.warn("Illegal cell name for CQL3 table ...");
    // track the liveness info so getRow() can inject a row marker
    // if no other valid cells exist on this row
    invalidLivenessInfo = LivenessInfo.create(cell.timestamp, ...);
    return true;
}
// In getRow():
if (!hasValidCells && invalidLivenessInfo != null)
    builder.addPrimaryKeyLivenessInfo(invalidLivenessInfo);
```

---

## How to Spot

Whenever a compatibility layer silently skips a "bad" data item, ask whether that item's presence was semantically significant in the old version. Here the illegal cell was meaningless as data but meaningful as an existence indicator; dropping it without preserving its liveness info changed row visibility, a subtle behavioral regression reviewers can catch by tracing what each cell type contributed to row materialization in the old format.

---

## Test

The test loads legacy `ka`-format SSTables containing illegal cell names and asserts that rows are returned (not silently dropped) when queries are run against the upgraded table.
