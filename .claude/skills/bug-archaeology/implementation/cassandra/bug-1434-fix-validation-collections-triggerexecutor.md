# Bug 1434: Fix Validation of Collections in TriggerExecutor

**Commit**: ec866fa164 · **Ticket**: CASSANDRA-8146
**Subsystem**: src/java/org/apache/cassandra/triggers/TriggerExecutor.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

`TriggerExecutor.validate()` iterated over column names in the trigger-produced mutation and called the legacy CQL 2 `QueryProcessor.validateColumn()` to check each cell's value. The legacy validator did not understand CQL 3 collection types and would throw an incorrect validation error when a trigger returned a mutation containing collection columns. The fix replaces the column-name iteration and manual value validation with `cell.validateFields(metadata)`, which delegates to the cell's own type-aware validation logic and correctly handles collections.

### Before / After

```java
// BEFORE
import org.apache.cassandra.cql.QueryProcessor;  // CQL 2 legacy class
...
for (CellName tName : tcf.getColumnNames())
    QueryProcessor.validateColumn(tcf.metadata(), tName, tcf.getColumn(tName).value());
```

```java
// AFTER
import org.apache.cassandra.cql3.QueryProcessor;  // CQL 3 class
...
for (Cell cell : tcf)
    cell.validateFields(tcf.metadata());
```

---

## How to Spot

When validation or processing code imports from `org.apache.cassandra.cql` (the legacy CQL 2 package) while the rest of the subsystem has moved to `org.apache.cassandra.cql3`, that import is a red flag. Legacy validators typically predate collection types and UDTs, so any code path that exercises them on CQL 3 data — especially plugin extension points like triggers — risks false validation failures. Look for `cql.QueryProcessor` usages in non-query-execution code.

---

## Test

No test added.
