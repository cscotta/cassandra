# Bug 2755: Hadoop ColumnFamilyOutputFormat Drops Mutations Due to Wrong Loop Condition

**Commit**: f659c6759d · **Ticket**: CASSANDRA-2255

## Detection Rule
When a batch-building loop uses `batch.size() < batchThreshold` as its continuation condition, verify the first mutation has been added before the size check; otherwise the loop may exit immediately when the map is empty, dropping the first entry.

## Code Smell
`while (batch.size() < batchThreshold)` checks size before the current mutation has been inserted, so if the threshold is 1 the loop body never executes and `mutation` is silently discarded.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: check-before-insert-in-loop
- severity: data-loss
- subsystem: tools-clients

## Root Cause
`ColumnFamilyRecordWriter.run()` used `while (batch.size() < batchThreshold)` as the loop guard, where the size check ran before the current mutation was added to the batch. For `batchThreshold == 1` this exited immediately, dropping all mutations. The fix changes the loop to `while (mutation != null)` and breaks when `batch.size() >= batchThreshold` after each insertion, ensuring every polled mutation is included.

## Example
```java
// BEFORE
while (batch.size() < batchThreshold) {
    // add mutation to batch ...
    if ((mutation = queue.poll()) == null)
        break;
}
```
```java
// AFTER
while (mutation != null) {
    // add mutation to batch ...
    if (batch.size() >= batchThreshold)
        break;
    mutation = queue.poll();
}
```
