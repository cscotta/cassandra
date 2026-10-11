# Bug 0831: SELECT DISTINCT paging and static-column detection broken in mixed clusters

**Commit**: 5847222d9b · **Ticket**: CASSANDRA-11126
**Subsystem**: CQL query execution / SELECT DISTINCT / mixed-version paging
**Category**: Logic error in condition

---

## Root Cause
Two independent bugs affected `SELECT DISTINCT` in mixed 2.x/3.x clusters. First, when serialising a range-slice command for an old node, the code used `||` to decide whether a query involves static columns: `!statics.isEmpty() || filter.requestedSlices().selects(STATIC_CLUSTERING)`. Because these two conditions are not redundant, using `||` could set `compositesToGroup = 0` (normal row grouping) for a distinct query that should have used `-2` (static column mode), causing the old node to group results incorrectly. The correct operator is `&&` — both conditions must be true for the query to genuinely select statics. Second, when building a 2.x paging mark for a row whose last returned cell has no cell data (i.e. the row marker itself is the last thing returned), the code encoded the clustering as a plain clustering name, but 2.x paging expects the row-marker cell name to be encoded as an empty column name within the row — `encodeCellName(..., EMPTY_BYTE_BUFFER, null)` — otherwise the next page starts in the wrong position.

### Before / After
```java
// BEFORE — wrong operator
boolean selectsStatics =
    !rangeCommand.columnFilter().fetchedColumns().statics.isEmpty()
    || filter.requestedSlices().selects(Clustering.STATIC_CLUSTERING);

// AFTER
boolean selectsStatics =
    !rangeCommand.columnFilter().fetchedColumns().statics.isEmpty()
    && filter.requestedSlices().selects(Clustering.STATIC_CLUSTERING);

// BEFORE — wrong cell name for row marker
mark = LegacyLayout.encodeClustering(metadata, row.clustering());

// AFTER — row marker encoded as empty column name
mark = LegacyLayout.encodeCellName(
    metadata, row.clustering(), ByteBufferUtil.EMPTY_BYTE_BUFFER, null);
```

---

## How to Spot
Boolean expressions that combine two independently-necessary conditions to test membership often require `&&` not `||`; when either check alone is sufficient to declare membership the result type is usually different from "selects statics". In paging code, any branch that encodes the last-returned position without inspecting whether the returned item is a row marker (no cells) versus a real cell is prone to encoding the wrong position. A test using a table with static columns and at least two pages of distinct-key results would expose both bugs.

---

## Test
No new test added; existing `select_distinct_with_deletions_test` was the repro test referenced in the ticket.
