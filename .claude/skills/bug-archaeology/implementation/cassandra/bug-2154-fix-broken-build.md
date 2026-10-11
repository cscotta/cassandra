# Bug 2154: Broken Build Due to Direct Field Access on ColumnFamilyStore

**Commit**: b241bcd08d · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/compaction/LeveledManifest.java
**Category**: Logic error in condition

---

## Root Cause

`LeveledManifest` accessed the `cfs.columnFamily` field directly in a logger call, but that field was removed or renamed, breaking compilation. The fix replaces the direct field access with the appropriate accessor method `cfs.getColumnFamilyName()`.

### Before / After

```java
// BEFORE
logger.warn("Manifest present but corrupt. Cassandra will re-level {} from scratch", cfs.columnFamily);
```

```java
// AFTER
logger.warn("Manifest present but corrupt. Cassandra will re-level {} from scratch", cfs.getColumnFamilyName());
```

---

## How to Spot

Direct field accesses on public mutable objects are fragile across refactors. Prefer accessor methods so that internal field renames do not silently introduce compilation errors in callers.

---

## Test

No test added.
