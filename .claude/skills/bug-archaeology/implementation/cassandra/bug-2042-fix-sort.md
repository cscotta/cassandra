# Bug 2042: ColumnIndex Missing Factory Method Needed by ObjectSizeTest

**Commit**: 811f82c5b1 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/ColumnIndex.java
**Category**: Missing null / bounds check

---

## Root Cause

After a refactor of `RowIndexEntry.create`, the signature changed to accept a `ColumnIndex` object rather than accepting `null` directly for the "no column index" case. The `ObjectSizeTest` still passed `null`, which caused a build (or runtime NPE) failure. The fix adds a `ColumnIndex.nothing()` factory method that constructs a properly-initialized empty `ColumnIndex`, and updates the test to call it instead of passing `null`. The factory is annotated `@VisibleForTesting` to signal that normal production paths should not need it.

### Before / After

```java
// BEFORE — no factory method existed; callers passed null
RowIndexEntry entry = RowIndexEntry.create(123, new DeletionTime(123, 123), null);
```

```java
// AFTER
@VisibleForTesting
public static ColumnIndex nothing()
{
    return new ColumnIndex(0);
}
// ...
RowIndexEntry entry = RowIndexEntry.create(123, new DeletionTime(123, 123), ColumnIndex.nothing());
```

---

## How to Spot

When a refactor changes a parameter type from a nullable object to a concrete type, scan all callers for `null` literals being passed in that position. Adding a static "empty" factory method is the standard safe replacement.

---

## Test

`ObjectSizeTest.testKeyCacheValueWithDelInfo` is updated to use the new factory method, ensuring the test compiles and the memory-size calculation for an entry with deletion info works correctly.
