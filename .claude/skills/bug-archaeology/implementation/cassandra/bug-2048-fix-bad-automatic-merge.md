# Bug 2048: Duplicate Constant Declaration in LeveledCompactionStrategy from Bad Merge

**Commit**: 260dc757fc · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/compaction/LeveledCompactionStrategy.java
**Category**: Logic error in condition

---

## Root Cause

A bad automatic merge introduced a duplicate `static final` field declaration for `SSTABLE_SIZE_OPTION` in `LeveledCompactionStrategy`. Java does not allow two fields with the same name in the same class, so this would cause a compile error rendering the class completely unusable.

### Before / After

```java
// BEFORE
private static final String SSTABLE_SIZE_OPTION = "sstable_size_in_mb";

private static final String SSTABLE_SIZE_OPTION = "sstable_size_in_mb";
```

```java
// AFTER
private static final String SSTABLE_SIZE_OPTION = "sstable_size_in_mb";
```

---

## How to Spot

After any merge, scan for duplicate field or variable declarations in the same scope. IDEs and compilers will catch this immediately, but it can slip through if the merged file is not compiled before being committed. Static analysis or a clean build gate would catch this class of error.

---

## Test

No test added.
