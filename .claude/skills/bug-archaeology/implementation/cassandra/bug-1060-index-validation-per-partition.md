# Bug 1060: Secondary Index Validated Multiple Times Per Partition in Batch

**Commit**: aad3ae2cbe · **Ticket**: CASSANDRA-10536
**Subsystem**: CQL / Secondary Index Validation
**Category**: State not cleaned up

---

## Root Cause
`UpdateParameters` previously held a reference to `SecondaryIndexManager` and called `indexManager.validate(update)` inline while building each individual mutation, once per statement within a batch. Because multiple statements in the same batch can target the same partition, the index validation ran redundantly once for each statement rather than once per partition after all mutations were assembled. The fix removes the validation responsibility from `UpdateParameters` entirely and moves it into `UpdatesCollector.validateIndexedColumns()`, which is called exactly once after all mutations are collected, iterating over each unique `PartitionUpdate`. `BatchStatement` calls this method before `toMutations()`.

### Before / After
```java
// BEFORE (UpdateParameters constructor)
if (validateIndexedColumns) {
    SecondaryIndexManager manager = Keyspace.openAndGetStore(metadata).indexManager;
    indexManager = manager.hasIndexes() ? manager : null;
}
// ... called per-statement:
public void validateIndexedColumns(PartitionUpdate update) {
    if (indexManager == null) return;
    indexManager.validate(update);
}

// AFTER (UpdatesCollector)
public void validateIndexedColumns() {
    for (Map<ByteBuffer, IMutation> perKsMutations : mutations.values())
        for (IMutation mutation : perKsMutations.values())
            for (PartitionUpdate update : mutation.getPartitionUpdates())
                Keyspace.openAndGetStore(update.metadata()).indexManager.validate(update);
}
// BatchStatement calls collector.validateIndexedColumns() once before toMutations()
```

---

## How to Spot
Any validation step placed inside an object that is instantiated per-statement (rather than per-batch) will fire multiple times when statements are grouped into a batch. Look for validation logic in `UpdateParameters` constructors or per-statement `execute` paths and confirm they deduplicate at the partition level. The correct insertion point for cross-statement validation is after the `UpdatesCollector` has merged all mutations.

---

## Test
`SecondaryIndexTest` and `CassandraIndexTest` were extended with batch insert scenarios that would previously trigger duplicate validation exceptions.
