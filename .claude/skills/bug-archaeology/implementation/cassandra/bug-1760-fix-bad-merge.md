# Bug 1760: Fix Bad Merge — SecondaryIndexManager Variable Not Assigned

**Commit**: 4b08be49a3 · **Ticket**: n/a
**Subsystem**: db/index/SecondaryIndexManager
**Category**: Logic error in condition

---

## Root Cause

A bad git merge left the result of `baseCfs.metadata.getColumnDefinition(cell.name())` discarded — the return value was not assigned to any variable. The immediately following line then referenced the variable `cDef`, which was undefined (or referred to a stale binding from a prior scope), causing a compile or runtime error. Additionally, the lookup into `indexesByColumn` was using `cDef.name` (a `ColumnIdentifier`) instead of `cDef.name.bytes` (a `ByteBuffer`), producing the wrong key type for the map.

### Before / After

```java
// BEFORE
baseCfs.metadata.getColumnDefinition(cell.name());   // result thrown away
SecondaryIndex index = indexesByColumn.get(cDef.name); // wrong key type
```

```java
// AFTER
ColumnDefinition cDef = baseCfs.metadata.getColumnDefinition(cell.name());
SecondaryIndex index = indexesByColumn.get(cDef.name.bytes);
```

---

## How to Spot

Watch for call-statement lines (no assignment) where the called method has a non-void return type — especially right before a reference to an apparently-undeclared variable. Merge conflicts often produce this pattern when one side adds an assignment and the other doesn't.

---

## Test

No test added.
