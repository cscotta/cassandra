# Bug 2043: DeletionTime Constructor Package-Private After Merge

**Commit**: 317ab72a8c · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/DeletionTime.java
**Category**: Logic error in condition

---

## Root Cause

A merge conflict left `DeletionTime`'s two-argument constructor with package-private (default) visibility instead of `public`. Code in other packages (such as `ObjectSizeTest` in `org.apache.cassandra.cache`) that needed to instantiate `DeletionTime` directly would fail to compile. The fix widens the constructor's visibility to `public`. Simultaneously, the test's import for the old `DeletionInfo` class (which no longer existed after the merge) was replaced with an import of the new `DeletionTime` class.

### Before / After

```java
// BEFORE
DeletionTime(long markedForDeleteAt, int localDeletionTime)
```

```java
// AFTER
public DeletionTime(long markedForDeleteAt, int localDeletionTime)
```

---

## How to Spot

After merge commits, check that constructor and method visibility was not accidentally narrowed. A class used by tests in a different package must have `public` constructors; missing `public` will show up immediately as a compilation error when those test classes are built.

---

## Test

`ObjectSizeTest` is updated to use `DeletionTime` (replacing the now-removed `DeletionInfo` import) and to pass a `DeletionTime` instance to `RowIndexEntry.create`, confirming that the newly-public constructor is accessible from a different package.
