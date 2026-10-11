# Bug 0663: Mutable Index Field in ReadCommand Causes Race During Serialization

**Commit**: 7f297bcf8a · **Ticket**: CASSANDRA-13363
**Subsystem**: src/java/org/apache/cassandra/db/ReadCommand.java, src/java/org/apache/cassandra/db/PartitionRangeReadCommand.java
**Category**: Race condition / missing synchronization

---

## Root Cause

`ReadCommand` stored the selected secondary index in a mutable, non-final field (`protected Optional<IndexMetadata> index`) along with a separate `indexManagerQueried` boolean flag to track lazy initialization. Because the object was shared across threads (e.g., constructed on the coordinator and then serialized for transmission to replicas), a concurrent read of the `index` field during serialization could observe an empty `Optional` while another thread was still populating it through `getBestIndexFor()`. The fix makes `index` final and moves index selection to a static `findIndex()` helper called during construction, so the field is set once and the object is effectively immutable.

### Before / After

```java
// BEFORE — mutable lazy field, racy under concurrency
protected Optional<IndexMetadata> index = Optional.empty();
private boolean indexManagerQueried = false;

public Index getIndex(ColumnFamilyStore cfs) {
    if (index.isPresent()) return cfs.indexManager.getIndex(index.get());
    if (indexManagerQueried) return null;
    Index selected = cfs.indexManager.getBestIndexFor(this);
    indexManagerQueried = true;
    if (selected != null) index = Optional.of(selected.getIndexMetadata());
    return selected;
}
```

```java
// AFTER — immutable, set at construction time
@Nullable
private final IndexMetadata index;

static IndexMetadata findIndex(CFMetaData table, RowFilter rowFilter) { ... }

public static PartitionRangeReadCommand create(...) {
    return new PartitionRangeReadCommand(..., findIndex(metadata, rowFilter));
}
```

---

## How to Spot

Any mutable field in a class that is meant to cache a lazily-computed value is a race condition candidate if the class is shared across threads without synchronization. Reviewers should flag non-final fields that follow the pattern of "compute once, cache indefinitely" and ask whether the object can be seen by another thread before initialization is complete. The safe alternative is to compute at construction time (immutable) or to use `AtomicReference`/double-checked locking.

---

## Test

No dedicated test added for the race itself; the refactoring makes the race structurally impossible.
