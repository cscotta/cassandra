# Bug 1863: Repair AssertionError When All Columns Expire as Tombstones During Compaction

**Commit**: cf8fa6e11b · **Ticket**: CASSANDRA-6277
**Subsystem**: db/compaction (PrecompactedRow)
**Category**: Missing null / bounds check

---

## Root Cause

`PrecompactedRow.update(MessageDigest)` asserted that `compactedCf` was non-null before computing a digest for repair. When all columns in a row expired as tombstones during compaction the merge result was an empty `ColumnFamily`, and `compactedCf` was set to `null` to signal "nothing to write". The assert then fired as an `AssertionError` during the repair tree-building phase, aborting the repair rather than simply contributing an empty digest.

### Before / After

```java
// BEFORE
public void update(MessageDigest digest)
{
    assert compactedCf != null;
    ...
}
```

```java
// AFTER
public void update(MessageDigest digest)
{
    if (compactedCf == null)
        return;
    ...
}
```

---

## How to Spot

Any method that uses an `assert` to guard against a `null` that can legitimately arise from normal data lifecycle (tombstone expiry, compaction GC) should instead use a defensive `null` check with a safe early return. Assertions are not appropriate for expected runtime states.

---

## Test

No test added.
