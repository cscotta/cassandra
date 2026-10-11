# Bug 1785: Fix Minor Compilation Problem in ColumnFamilyStore

**Commit**: 811de14ecc · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db
**Category**: Wrong constant / default value

---

## Root Cause

A log statement in `ColumnFamilyStore` passed the bare string `columnFamily` as a format argument, but the field `columnFamily` had been replaced by `metadata.cfName` in a prior refactoring. The stale identifier caused a compilation failure; the correct expression to supply the column family name to the logger was `metadata.cfName`.

### Before / After

```java
// BEFORE
logger.debug("Removing compacted SSTable files from {} ...", columnFamily);
```

```java
// AFTER
logger.debug("Removing compacted SSTable files from {} ...", metadata.cfName);
```

---

## How to Spot

After renaming or removing a field, confirm all logging and diagnostic call sites that reference the old name have been updated — they are easy to miss because IDE inspections often don't highlight logger arguments as errors until compile time.

---

## Test

No test added. The fix is verified by successful compilation.
