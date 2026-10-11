# Bug 1965: Hadoop CQL writers double-quote key columns in WHERE clauses

**Commit**: aa518998c9 · **Ticket**: CASSANDRA-5824
**Subsystem**: src/java/org/apache/cassandra/hadoop/cql3/CqlPagingRecordReader.java, src/java/org/apache/cassandra/hadoop/cql3/CqlRecordWriter.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`CqlPagingRecordReader` called the `quote()` helper on `partitionKey` and `clusterKey` strings that were already quoted (they came from a path that had previously applied quoting), resulting in doubly-quoted identifiers like `""key""` in the SELECT column list. The extra quoting caused CQL parse failures or no-match results. Separately, `CqlRecordWriter` built the WHERE clause for writes without quoting any column names at all, so identifiers containing uppercase letters or reserved words were sent unquoted and rejected by the parser. The fix removes the spurious `quote()` calls in `CqlPagingRecordReader`'s SELECT list and adds proper `quote()` calls in `CqlRecordWriter`'s WHERE-clause builder.

### Before / After

```java
// BEFORE – CqlPagingRecordReader (double-quoting)
columns = (clusterKey == null || "".equals(clusterKey))
    ? quote(partitionKey) + "," + columns
    : quote(partitionKey) + "," + quote(clusterKey) + "," + columns;
```

```java
// AFTER
columns = (clusterKey == null || "".equals(clusterKey))
    ? partitionKey + "," + columns
    : partitionKey + "," + clusterKey + "," + columns;
```

```java
// BEFORE – CqlRecordWriter (no quoting)
keyWhereClause += String.format("%s = ?", keyWhereClause.isEmpty()
    ? partitionKey : (" AND " + partitionKey));
```

```java
// AFTER
keyWhereClause += String.format("%s = ?", keyWhereClause.isEmpty()
    ? quote(partitionKey) : (" AND " + quote(partitionKey)));
```

---

## How to Spot

In any SQL/CQL query-building path, track exactly where identifier quoting is applied: apply it once, at the point where the identifier is first embedded in the query string. If the same identifier flows through multiple helper methods, one of which already quotes it, a second call to `quote()` will produce invalid double-quoted output.

---

## Test

No test added.
