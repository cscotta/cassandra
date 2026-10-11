# Bug 1828: Wrong Type Parameter in CollectionType.enforceLimit After Merge

**Commit**: 1334f94e40 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/marshal
**Category**: Logic error in condition

---

## Root Cause

A merge conflict resolution left `enforceLimit` in `CollectionType` using the broader `IColumn` interface instead of the concrete `Column` type in its signature. This type mismatch meant callers passing `List<Pair<ByteBuffer, Column>>` would encounter a compile or runtime type mismatch, since the method signature required the interface. The fix corrects the parameter and return type to `Column`, matching the actual usage throughout the codebase.

### Before / After

```java
// BEFORE
protected List<Pair<ByteBuffer, IColumn>> enforceLimit(List<Pair<ByteBuffer, IColumn>> columns)
```

```java
// AFTER
protected List<Pair<ByteBuffer, Column>> enforceLimit(List<Pair<ByteBuffer, Column>> columns)
```

---

## How to Spot

After a merge, grep for uses of interface types in positions where the surrounding code consistently passes concrete types. A one-line change that switches an interface to a concrete type in a method signature is a common merge-artifact pattern and deserves extra scrutiny.

---

## Test

No test added.
