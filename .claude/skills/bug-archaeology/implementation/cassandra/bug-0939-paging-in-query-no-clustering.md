# Bug 0939: Paging broken for IN queries on tables without clustering columns

**Commit**: 60b23b1252 · **Ticket**: CASSANDRA-11208
**Subsystem**: CQL / Paging
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
`AbstractQueryPager` tracked whether a partition was fully consumed by testing `lastRow.clustering() == Clustering.STATIC_CLUSTERING`. For tables without clustering columns every row has `Clustering.EMPTY` as its clustering, not `STATIC_CLUSTERING`. When the pager received such a row it did not reset `remainingInPartition` to zero, so it believed there were still rows left in the current partition and included a stale paging position in the next `PagingState`. Subsequent pages therefore started from a wrong offset, returning duplicate or missing rows. A companion NPE in `PagingState.toString()` could also fire when `partitionKey` was null.

### Before / After
```java
// BEFORE — only STATIC_CLUSTERING resets remaining
if (lastRow != null && lastRow.clustering() == Clustering.STATIC_CLUSTERING)
    remainingInPartition = 0;

// AFTER — EMPTY clustering (no-clustering tables) also resets remaining
if (lastRow != null && (lastRow.clustering() == Clustering.STATIC_CLUSTERING
        || lastRow.clustering() == Clustering.EMPTY))
    remainingInPartition = 0;
```

---

## How to Spot
Paging logic that branches on a specific `Clustering` sentinel should enumerate all semantically equivalent sentinels. Tables without clustering columns use `Clustering.EMPTY` for their single-row entries; code that only checks for `Clustering.STATIC_CLUSTERING` will misbehave for those tables. Additionally, `PagingState.toString()` should null-guard `partitionKey` before calling `ByteBufferUtil.bytesToHex`.

---

## Test
No dedicated new test; the fix is validated by the broader paging and IN-query integration test suite.
