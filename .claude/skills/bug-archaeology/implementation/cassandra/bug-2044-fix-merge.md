# Bug 2044: RowIndexEntry.memorySize References Wrong Field Name

**Commit**: 344f5fa5af · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/RowIndexEntry.java
**Category**: Wrong constant / default value

---

## Root Cause

Inside the `IndexedEntry` subclass of `RowIndexEntry`, the `memorySize()` method referenced `deletionInfo.memorySize()` — the old field name from before a merge refactored the field to `deletion`. This stale reference would cause a compilation failure immediately after the merge landed.

### Before / After

```java
// BEFORE
return ObjectSizes.getFieldSize(deletionInfo.memorySize() + listSize);
```

```java
// AFTER
return ObjectSizes.getFieldSize(deletion.memorySize() + listSize);
```

---

## How to Spot

After any merge that renames a field (e.g., `deletionInfo` to `deletion`), search all usages of the old name in the same file and adjacent files. IDE "find usages" or a simple grep for the old identifier will immediately surface stale references.

---

## Test

No test added.
