# Bug 1620: Fix CQL3 Paging Reader When Columns Are Reversed

**Commit**: 1b9f712eb8 · **Ticket**: CASSANDRA-7100
**Subsystem**: src/java/org/apache/cassandra/hadoop/cql3/CqlPagingRecordReader.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`CqlPagingRecordReader` pages through results by issuing WHERE clauses of the form `AND col > ?` to advance past the last-seen clustering key. For columns declared with `DESC` (reversed) clustering order, the correct comparison operator is `<` rather than `>` because the byte ordering is inverted. The reader never inspected the column comparator metadata to detect `ReversedType`, so it unconditionally used `>`. This caused paging to scan in the wrong direction, missing rows or looping indefinitely on reversed-order tables.

### Before / After

```java
// BEFORE - always uses '>' regardless of clustering direction
return Pair.create(position + 2,
    String.format(" AND %s > ? ", quote(column.get(position).name)));
```

```java
// AFTER - uses '<' when the column is reversed
return Pair.create(position + 2,
    String.format(" AND %s %s ? ",
        quote(column.get(position).name),
        column.get(position).reversed ? " < " : " >"));

// reversed flag is populated from comparator metadata:
if (comparatorValidator instanceof CompositeType)
    for (int i = 0; i < clusterColumns.size(); i++)
        clusterColumns.get(i).reversed =
            (((CompositeType) comparatorValidator).types.get(i) instanceof ReversedType);
else if (comparatorValidator instanceof ReversedType)
    clusterColumns.get(0).reversed = true;
```

---

## How to Spot

Any paging or cursor implementation that constructs a "greater than last seen" predicate must check whether the sort order of each clustering column is reversed. Look for hard-coded `>` or `<` in dynamically built WHERE clauses and verify they account for `ReversedType` / DESC columns.

---

## Test

No test added.
