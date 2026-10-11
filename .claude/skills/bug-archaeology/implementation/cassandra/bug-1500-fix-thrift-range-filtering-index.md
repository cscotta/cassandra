# Bug 1500: Fix thrift range filtering without 2ary index lookup

**Commit**: 59a8e1c6b6 · **Ticket**: CASSANDRA-7741
**Subsystem**: service/StorageProxy
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`StorageProxy.getRangeConcurrencyFactor` computed the estimated number of result rows per range to decide how many replicas to contact in parallel. When a `rowFilter` was present, the code assumed at least one `SecondaryIndexSearcher` would be found and asserted `!searchers.isEmpty()`. For Thrift queries that supply a row filter without a matching secondary index, the searchers list is legitimately empty, causing an `AssertionError`. The fix replaces the assertion with a conditional: fall back to the key-based estimate when no secondary index searcher is available.

### Before / After

```java
// BEFORE
List<SecondaryIndexSearcher> searchers = cfs.indexManager.getIndexSearchersForQuery(command.rowFilter);
assert !searchers.isEmpty() : "Got row filter with no matching SecondaryIndexSearchers";
for (SecondaryIndexSearcher searcher : searchers)
{
    SecondaryIndex highestSelectivityIndex = searcher.highestSelectivityIndex(command.rowFilter);
    resultRowsPerRange = Math.min(resultRowsPerRange, highestSelectivityIndex.estimateResultRows());
}
```

```java
// AFTER
List<SecondaryIndexSearcher> searchers = cfs.indexManager.getIndexSearchersForQuery(command.rowFilter);
if (searchers.isEmpty())
{
    resultRowsPerRange = calculateResultRowsUsingEstimatedKeys(cfs);
}
else
{
    for (SecondaryIndexSearcher searcher : searchers)
    {
        SecondaryIndex highestSelectivityIndex = searcher.highestSelectivityIndex(command.rowFilter);
        resultRowsPerRange = Math.min(resultRowsPerRange, highestSelectivityIndex.estimateResultRows());
    }
}
```

---

## How to Spot

Assertions used to guard "should never be empty" assumptions on data coming from external callers (Thrift, CQL) are dangerous; Thrift clients have different constraints than CQL clients and may produce states the code did not anticipate. Use conditional logic with a sensible fallback rather than assertions in shared code paths.

---

## Test

No test added.
