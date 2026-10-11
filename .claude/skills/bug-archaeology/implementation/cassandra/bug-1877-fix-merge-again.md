# Bug 1877: Fix Merge — Wrong Class Reference in StorageService (replace path)

**Commit**: 99bef9dfe8 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/service/StorageService.java
**Category**: Wrong constant / default value

---

## Root Cause

During a branch merge, a call site in the token-replacement bootstrap path was left referencing the old class name `SystemTable` instead of the renamed `SystemKeyspace`. The call `SystemTable.removeEndpoint(...)` would fail at runtime (or compile time) because `SystemTable` no longer existed in the codebase after the rename. This is one of three one-line merge-artifact fixes applied to StorageService in quick succession.

### Before / After

```java
// BEFORE – stale class name from pre-merge branch
SystemTable.removeEndpoint(DatabaseDescriptor.getReplaceAddress());
```

```java
// AFTER – correct renamed class
SystemKeyspace.removeEndpoint(DatabaseDescriptor.getReplaceAddress());
```

---

## How to Spot

After a class rename refactor, search the entire codebase for the old name before merging. A global find-and-replace or IDE refactor tool would have caught all three occurrences atomically.

---

## Test

No test added.
