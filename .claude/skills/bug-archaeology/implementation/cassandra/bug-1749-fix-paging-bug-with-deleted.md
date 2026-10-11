# Bug 1749: Fix Paging Bug with Deleted Columns

**Commit**: cd2c43884d · **Ticket**: CASSANDRA-6748
**Subsystem**: src/java/org/apache/cassandra/service/pager/
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

The query pager tracked its position across pages by recording the name of the last returned column. When determining whether the first column of the next page was a duplicate of that saved position, `containsPreviousLast()` compared column names but never checked whether the column was actually live. If the boundary column had been deleted (either by a tombstone or a range deletion), the pager still treated it as the "previous last" and skipped it on the next fetch, causing that page to have one fewer result than requested and eventually producing incorrect or incomplete results. The fix extracts the full `Column` object (not just the name) so liveness and deletion status can be checked before deciding whether to skip the column.

### Before / After

```java
// BEFORE (SliceQueryPager.containsPreviousLast)
protected boolean containsPreviousLast(Row first)
{
    return lastReturned != null && lastReturned.equals(isReversed() ? lastName(first.cf) : firstName(first.cf));
}
```

```java
// AFTER (SliceQueryPager.containsPreviousLast)
protected boolean containsPreviousLast(Row first)
{
    if (lastReturned == null)
        return false;

    Column firstColumn = isReversed() ? lastColumn(first.cf) : firstColumn(first.cf);
    return !first.cf.deletionInfo().isDeleted(firstColumn)
        && firstColumn.isLive(timestamp())
        && lastReturned.equals(firstColumn.name());
}
```

---

## How to Spot

Pagination state that tracks position by column name alone is fragile when tombstones are present. During review, verify that any "have I already returned this item?" check considers liveness/deletion, not just identity, especially in code paths where deleted cells can appear interleaved with live ones.

---

## Test

`SliceQueryWithTombstoneTest` in `QueryPagerTest`: inserts 5 rows with null values (producing tombstones) into `cql_keyspace.table2`, pages through them one result at a time, and asserts that exactly one live row marker is returned per page.
