# Bug 2254: ALTER TABLE ADD with Collection Type Uses Stale Comparator Component Index

**Commit**: ceca1997d0 · **Ticket**: CASSANDRA-4493
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/AlterTableStatement.java, src/java/org/apache/cassandra/db/ColumnFamilyStore.java
**Category**: State not cleaned up

---

## Root Cause

When `ALTER TABLE … ADD` is used to add a column of a collection type, the `AlterTableStatement` must insert a `ColumnToCollectionType` component into the table's `CompositeType` comparator and record the correct `componentIndex` in the resulting `ColumnDefinition`. The original code computed `componentIndex` as `((CompositeType)meta.comparator).types.size() - 1` once at the top of the block, then called `ctypes.add(newColType)` which extended the comparator's type list by one — but the previously computed index was never decremented to reflect the pre-extension size. The `ColumnDefinition` was therefore registered with a component index that was off by one (pointing past the end of the original comparator), corrupting the CF comparator metadata.

Additionally, after `reload()` updates the CF metadata, the in-memory `Memtable` still held a reference to the old comparator. If writes arrived before a flush, they would use the stale comparator. The fix forces a memtable switch immediately after `reload()`, ensuring the new comparator is adopted atomically.

### Before / After

```java
// BEFORE (AlterTableStatement)
// componentIndex computed after collection branch modifies ctypes:
cfm.addColumnDefinition(new ColumnDefinition(...,
    cfDef.isComposite ? ((CompositeType)meta.comparator).types.size() - 1 : null));

// BEFORE (ColumnFamilyStore.reload)
// No memtable switch; old memtable keeps stale comparator reference
```

```java
// AFTER (AlterTableStatement)
Integer componentIndex = cfDef.isComposite
    ? ((CompositeType)meta.comparator).types.size() - 1 : null;
if (type instanceof CollectionType) {
    componentIndex--;  // adjust for the collection component being added
    ...
}
cfm.addColumnDefinition(new ColumnDefinition(..., componentIndex));

// AFTER (ColumnFamilyStore.reload)
maybeSwitchMemtable(getMemtableThreadSafe(), true).get();  // adopt new comparator
```

---

## How to Spot

When a value derived from a mutable collection is captured before the collection is modified, verify whether the captured value remains valid after the mutation. Also, after schema changes that alter comparator types, check whether any in-memory structures (memtables, caches) still hold references to the old type and need to be invalidated.

---

## Test

No test added.
