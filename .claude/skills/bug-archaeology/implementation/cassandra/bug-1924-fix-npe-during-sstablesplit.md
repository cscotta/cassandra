# Bug 1924: Null tracker reference in SSTableDeletingTask causes NPE during sstablesplit

**Commit**: 0804b76100 · **Ticket**: CASSANDRA-6026
**Subsystem**: src/java/org/apache/cassandra/io/sstable/SSTableDeletingTask.java
**Category**: Missing null / bounds check

---

## Root Cause

`SSTableDeletingTask.run()` unconditionally called `tracker.notifyDeleting(referent)` at the start of the deletion run, but the `tracker` field can be `null` when an SSTable is opened outside of a live `ColumnFamilyStore` — as is the case with the sstablesplit tool. Without a null guard, any attempt to split an SSTable triggered an immediate `NullPointerException` before any files were touched.

### Before / After

```java
// BEFORE
public void run()
{
    tracker.notifyDeleting(referent);
    // ...
}
```

```java
// AFTER
public void run()
{
    if (tracker != null)
        tracker.notifyDeleting(referent);
    // ...
}
```

---

## How to Spot

When a field is set via a separate registration/setter path rather than in a constructor, treat it as potentially null everywhere it is dereferenced. Tool-mode code paths that open SSTables without a full `ColumnFamilyStore` context are particularly likely to leave such fields unset.

---

## Test

No test added.
