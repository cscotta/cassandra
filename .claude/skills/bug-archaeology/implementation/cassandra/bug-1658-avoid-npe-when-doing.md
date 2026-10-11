# Bug 1658: NPE in ColumnFamilyStore.scrubDataDirectories When I/O Error Occurs

**Commit**: 9002e76d69 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db
**Category**: Missing null / bounds check

---

## Root Cause

`ColumnFamilyStore.scrubDataDirectories()` calls `File.listFiles(filter)` on each column-family directory to find streaming lock files to clean up. The Java contract for `File.listFiles()` specifies that it returns `null` (not an empty array) when an I/O error occurs or the path does not denote a directory. The code unconditionally dereferenced the result, so any I/O error during startup produced a `NullPointerException` instead of a graceful skip.

### Before / After

```java
// BEFORE
File[] lockfiles = dir.listFiles(filter);
if (lockfiles.length == 0)
    continue;
```

```java
// AFTER
File[] lockfiles = dir.listFiles(filter);
// lock files can be null if I/O error happens
if (lockfiles == null || lockfiles.length == 0)
    continue;
```

---

## How to Spot

Every call to `File.listFiles()` must guard against a `null` return before accessing `.length` or iterating. A static analysis rule (e.g., FindBugs NP_NULL_ON_SOME_PATH) flags unchecked uses of `listFiles()` results.

---

## Test

No test added.
