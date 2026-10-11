# Bug 1162: Keyspace Drop Fails When Materialized Views Exist

**Commit**: b5045356dc · **Ticket**: n/a
**Subsystem**: schema/SchemaKeyspace.java
**Category**: State not cleaned up

---

## Root Cause

The `makeDropKeyspaceMutation` helper in `SchemaKeyspace` built a mutation that deleted all schema tables in the `system_schema` keyspace for the dropped keyspace, but it also attempted to delete two system-table entries — `SystemKeyspace.BuiltMaterializedViews` and `SystemKeyspace.MaterializedViewsBuildsInProgress` — using the same decorated key derived from the schema keyspace name. These system tables live in `system`, not `system_schema`, so applying a `PartitionUpdate.fullPartitionDelete` against them via a mutation keyed to the schema keyspace partition would silently do nothing useful and could produce an inconsistent mutation, causing the drop operation to fail or leave orphaned state.

### Before / After

```java
// BEFORE
Mutation mutation = new Mutation(NAME, getSchemaKSDecoratedKey(keyspace.name));
for (CFMetaData schemaTable : All)
    mutation.add(PartitionUpdate.fullPartitionDelete(schemaTable, mutation.key(), timestamp, nowInSec));
mutation.add(PartitionUpdate.fullPartitionDelete(SystemKeyspace.BuiltMaterializedViews, mutation.key(), timestamp, nowInSec));
mutation.add(PartitionUpdate.fullPartitionDelete(SystemKeyspace.MaterializedViewsBuildsInProgress, mutation.key(), timestamp, nowInSec));
return mutation;
```

```java
// AFTER
Mutation mutation = new Mutation(NAME, getSchemaKSDecoratedKey(keyspace.name));
for (CFMetaData schemaTable : All)
    mutation.add(PartitionUpdate.fullPartitionDelete(schemaTable, mutation.key(), timestamp, nowInSec));

return mutation;
```

---

## How to Spot

When a mutation is constructed against a specific keyspace (here `system_schema`), adding partition deletes that reference tables belonging to a different keyspace (`system`) is a logical mismatch. Reviewers should verify that each `PartitionUpdate` added to a `Mutation` targets a table in the same keyspace as the mutation itself. Cleanup logic for system tables owned by a different keyspace must be handled in a separate mutation.

---

## Test

No test added.
