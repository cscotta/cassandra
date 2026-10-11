# Bug 0250: ConcurrentModificationException in STCS/DTCS/TWCS `getSSTables()`

**Commit**: ee266d283e · **Ticket**: CASSANDRA-17977
**Subsystem**: `db/compaction/SizeTieredCompactionStrategy.java`, `DateTieredCompactionStrategy.java`, `TimeWindowCompactionStrategy.java`
**Category**: Race condition / missing synchronization

---

## Root Cause

All three compaction strategies override `getSSTables()` to return an `ImmutableSet.copyOf(sstables)`, but the `sstables` field is a mutable collection that can be modified concurrently by other threads adding or removing SSTables. Because the snapshot iteration inside `ImmutableSet.copyOf()` is not synchronized, a concurrent structural modification to `sstables` causes a `ConcurrentModificationException`. The fix adds `synchronized` to each `getSSTables()` override, matching the synchronization already present on the mutating methods.

### Before / After

```java
// BEFORE
@Override
protected Set<SSTableReader> getSSTables()
{
    return ImmutableSet.copyOf(sstables);
}
```

```java
// AFTER
@Override
protected synchronized Set<SSTableReader> getSSTables()
{
    return ImmutableSet.copyOf(sstables);
}
```

---

## How to Spot

When a method reads a shared mutable collection and all its writing counterparts are synchronized, the reader must also be synchronized even if it only iterates. Constructing an immutable copy is not inherently thread-safe when the source collection is being mutated concurrently. During review, check that every read path over a shared collection holds the same lock as the write paths.

---

## Test

No test added; the fix is a one-line `synchronized` addition on three strategy classes.
