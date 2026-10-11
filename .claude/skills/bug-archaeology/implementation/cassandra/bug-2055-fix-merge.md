# Bug 2055: SecondaryIndexManager Casts All Indexes to PerColumnSecondaryIndex Without Type Check

**Commit**: 162675983c · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/index/SecondaryIndexManager.java
**Category**: Logic error in condition

---

## Root Cause

`SecondaryIndexManager` iterated over all secondary indexes for a column and unconditionally cast each to `PerColumnSecondaryIndex` before calling `delete` and `insert`. When a `PerRowSecondaryIndex` was also registered for the same column, the cast would throw a `ClassCastException` at runtime. The fix adds an `instanceof` guard so that only `PerColumnSecondaryIndex` instances are handled in this code path, leaving per-row indexes to be notified through their own separate path.

### Before / After

```java
// BEFORE: unconditional cast
for (SecondaryIndex index : indexFor(column.name())) {
    ((PerColumnSecondaryIndex) index).delete(key.key, oldColumn);
    if (!column.isMarkedForDelete())
        ((PerColumnSecondaryIndex) index).insert(key.key, column);
}
```

```java
// AFTER: guarded with instanceof
for (SecondaryIndex index : indexFor(column.name())) {
    if (index instanceof PerColumnSecondaryIndex) {
        ((PerColumnSecondaryIndex) index).delete(key.key, oldColumn);
        if (!column.isMarkedForDelete())
            ((PerColumnSecondaryIndex) index).insert(key.key, column);
    }
}
```

---

## How to Spot

Whenever a loop iterates over a heterogeneous collection of objects sharing a common supertype and casts each element to a specific subtype without a preceding `instanceof` check, there is a latent `ClassCastException`. This is especially common after introducing a new subtype (here `PerRowSecondaryIndex`) that shares index registration infrastructure with an existing subtype.

---

## Test

No new test added; the existing `PerRowSecondaryIndexTest` was updated to use the corrected API and confirms per-row index notification still works correctly.
