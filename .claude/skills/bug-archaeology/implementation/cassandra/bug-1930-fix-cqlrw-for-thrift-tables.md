# Bug 1930: CqlRecordWriter Fails for Thrift Tables

**Commit**: 2ce932761a · **Ticket**: CASSANDRA-6002
**Subsystem**: src/java/org/apache/cassandra/hadoop/cql3/CqlRecordWriter.java
**Category**: Missing null / bounds check

---

## Root Cause

`CqlRecordWriter` handled Thrift-defined tables by iterating over `cfDefinition.keys` to populate `partitionKeyColumns`, but never allocated the array before the loop, causing a `NullPointerException` when writing to Thrift tables via the CQL Hadoop record writer. Additionally, `clusterColumns` was never initialized for Thrift tables, leaving it null and causing failures when the writer subsequently tried to use it. The fix allocates `partitionKeyColumns` with the correct size before the loop and initializes `clusterColumns` by iterating `cfDefinition.columns`.

### Before / After

```java
// BEFORE
for (ColumnIdentifier column : cfDefinition.keys.keySet())
{
    partitionKeyColumns[i] = column.toString();  // NPE: array never allocated
    i++;
}
// clusterColumns never initialized for thrift path
return;
```

```java
// AFTER
partitionKeyColumns = new String[cfDefinition.keys.keySet().size()];
for (ColumnIdentifier column : cfDefinition.keys.keySet())
{
    partitionKeyColumns[i] = column.toString();
    i++;
}
clusterColumns = new ArrayList<String>();
for (ColumnIdentifier column : cfDefinition.columns.keySet())
    clusterColumns.add(column.toString());
return;
```

---

## How to Spot

When a code path diverges to handle a special schema type (e.g., Thrift vs. CQL3), verify that all fields that the common code path initializes are also initialized in the special path. Missing initialization of arrays or collections before population is a common pattern.

---

## Test

No test added.
