# Bug 1084: Static List Updates and Deletes Ignored Due to Missing Static Row Handling

**Commit**: 21065423c6 · **Ticket**: CASSANDRA-9838
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/ModificationStatement.java
**Category**: Silent skip on non-match

---

## Root Cause

`ModificationStatement` fetched existing rows to build a pre-condition map for conditional updates and list index operations. The code called `iter.next()` to retrieve the first CQL3 row but never consulted `iter.getStaticRow()`. For tables with static list columns, the list data lives in the static row, not in regular clustering rows, so the pre-condition map was always empty for that partition key. Subsequent element-level updates (`list[0] = X`) and deletes (`DELETE list[0]`) saw no prior state and silently produced wrong results.

### Before / After

```java
// BEFORE — static row never retrieved
Iterator<CQL3Row> iter = cfm.comparator.CQL3RowBuilder(cfm, now).group(...);
if (iter.hasNext())
    map.put(row.key.getKey(), iter.next());
```

```java
// AFTER — static row checked first, then clustering rows
CQL3Row.RowIterator iter = cfm.comparator.CQL3RowBuilder(cfm, now).group(...);
if (iter.getStaticRow() != null)
    map.put(row.key.getKey(), iter.getStaticRow());
if (iter.hasNext())
    map.put(row.key.getKey(), iter.next());
```

---

## How to Spot

In CQL3 result-processing code, any `Iterator<CQL3Row>` retrieved from `CQL3RowBuilder.group()` should be accompanied by a call to `getStaticRow()` before iterating clustering rows. If only `hasNext()`/`next()` are used, static-column state will be silently ignored. This pattern is especially dangerous for collection operations (list index reads/writes) applied to `static` columns, because no error is thrown—the operation simply sees an empty state and produces incorrect output.

---

## Test

`CollectionsTest.testUpdateStaticList` exercises append, index-update, and index-delete operations on a static list column, verifying the expected element sequence after each mutation.
