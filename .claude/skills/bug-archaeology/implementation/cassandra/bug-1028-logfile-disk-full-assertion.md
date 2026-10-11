# Bug 1028: LogFile Assertion Fails When Disk Is Full During Append

**Commit**: 3223927292 · **Ticket**: CASSANDRA-10538
**Subsystem**: Storage engine / lifecycle log
**Category**: Logic error in condition

---

## Root Cause
`LogFile.addRecord()` used `records.add(record)` both as the write-guard and as the return value. The `Set.add()` call returns `false` when the element is already present, so `!records.add(record)` was a correct duplicate guard — but it also silently swallowed the side effect: if `replicas.append(record)` threw an `IOException` (e.g. because the disk was full), the record had already been added to the `records` set, meaning a subsequent retry would be blocked by the guard and return `false` even though the on-disk append never succeeded.

Separately, `LogFile.remove()` called `records.remove(record)` before `deleteRecordFiles(record)`. The `assert records.contains(record)` that preceded it would pass, but if `deleteRecordFiles` threw, the record was already gone from the set, corrupting internal state. Also, `LogTransaction.doCommit/doAbort` called `txnFile.commit/abort` and then `complete()`, so a failure in `commit/abort` would be thrown directly rather than being accumulated through `Throwables.perform`.

### Before / After
```java
// BEFORE — duplicate-check and set-insertion collapsed into one call;
// append failure leaves records set inconsistent
private boolean addRecord(LogRecord record) {
    if (!records.add(record))
        return false;
    replicas.append(record);
    return true;
}

// AFTER — check containment first, then append, then add to set
private boolean addRecord(LogRecord record) {
    if (records.contains(record))
        return false;
    replicas.append(record);      // may throw; records still clean
    return records.add(record);
}

// BEFORE — remove before delete; exception leaves set inconsistent
records.remove(record);
deleteRecordFiles(record);

// AFTER — delete first, then remove from set
deleteRecordFiles(record);
records.remove(record);

// BEFORE — exception from commit escapes without being accumulated
protected Throwable doCommit(Throwable accumulate) {
    txnFile.commit();
    return complete(accumulate);
}
// AFTER — failure folded into accumulate
protected Throwable doCommit(Throwable accumulate) {
    return complete(Throwables.perform(accumulate, txnFile::commit));
}
```

---

## How to Spot
When a guard condition and a mutating side effect are combined in a single expression (like `!set.add(x)`), verify that failure of the mutation leaves the in-memory state consistent. Any code that removes/modifies an in-memory record *before* performing the corresponding disk operation is also suspect — always do I/O first, then update in-memory state on success.

---

## Test
No test added.
