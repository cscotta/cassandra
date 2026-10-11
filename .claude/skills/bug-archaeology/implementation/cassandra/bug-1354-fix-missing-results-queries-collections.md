# Bug 1354: Missing Results in Secondary Index Queries on Collections with ALLOW FILTERING

**Commit**: 2445d4d09f · **Ticket**: CASSANDRA-8421
**Subsystem**: src/java/org/apache/cassandra/db/index/composites/CompositesSearcher.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`CompositesSearcher` iterates index cells and groups them by partition key. When the partition key transitions from one row to the next, the code saved the previous key in `previousKey` but failed to reset `previousPrefix` to `null`. As a result, the prefix check on the first cell of the new partition compared against a prefix from the previous partition, causing the row-level filtering logic to incorrectly skip cells that should have been included. This produced silently missing rows when a secondary index query on a collection column was combined with additional filtering (`ALLOW FILTERING`).

### Before / After

```java
// BEFORE — previousPrefix not cleared on partition key transition
DecoratedKey previousKey = currentKey;
currentKey = dk;
// previousPrefix still holds the prefix from the old partition
indexCells.addFirst(cell);
```

```java
// AFTER — previousPrefix reset to null on key transition
DecoratedKey previousKey = currentKey;
currentKey = dk;
previousPrefix = null;   // clear stale prefix from prior partition
indexCells.addFirst(cell);
```

---

## How to Spot

Whenever an iterator accumulates per-row state (prefixes, column counts, flags) and the code explicitly detects a key boundary and resets *some* fields, audit whether *all* per-row state fields are also reset. A common pattern is to reset the "current key" but forget auxiliary state that was valid only within the scope of that key. In secondary index searchers this is especially dangerous because stale state silently drops qualifying rows rather than throwing an exception.

---

## Test

A regression test `testListContainsWithFiltering` was added to `ContainsRelationTest`: it creates a table with a list column, inserts rows split across two different secondary-index values, and asserts that a `CONTAINS … ALLOW FILTERING` query returns all matching rows from both partitions.
