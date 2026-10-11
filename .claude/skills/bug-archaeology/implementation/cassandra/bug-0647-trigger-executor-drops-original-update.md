# Bug 0647: TriggerExecutor Drops the Original Partition Update

**Commit**: 51e6f2446e · **Ticket**: CASSANDRA-13894
**Subsystem**: triggers/TriggerExecutor
**Category**: Logic error in condition

---

## Root Cause

`TriggerExecutor.execute()` collects the additional `PartitionUpdate` mutations produced by registered triggers (`intermediate`) and then called `PartitionUpdate.merge()` on only those trigger-generated updates — it never included the original incoming `updates` object in the merge. The result was that any triggered write replaced the original write entirely: the column values supplied by the client were silently discarded, and only the trigger-injected columns were persisted.

### Before / After

```java
// BEFORE
return PartitionUpdate.merge(
    validateForSinglePartition(updates.metadata().cfId,
                               updates.partitionKey(),
                               intermediate));
```

```java
// AFTER
List<PartitionUpdate> augmented = validateForSinglePartition(
    updates.metadata().cfId, updates.partitionKey(), intermediate);
// concatenate augmented and origin
augmented.add(updates);
return PartitionUpdate.merge(augmented);
```

---

## How to Spot

Whenever a method collects "extra" mutations/updates produced by a side-channel (triggers, interceptors, CDC) and then merges them, check that the original input is included in the merge. The pattern `merge(transformedList)` without the original item is a red flag. Unit tests for trigger execution should assert that columns written by the original client mutation still appear in the final stored row, not just the columns added by the trigger.

---

## Test

`TriggerExecutorTest.sameKeySameCfColumnFamilies()` was updated to verify that both the trigger-injected column (`c2 = "trigger"`) and the original client-written column (`c1 = "v1"`) are present in the merged result row.
