# Bug 1813: Fix merge

**Commit**: 34235ad7b5 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/thrift/ThriftValidation.java
**Category**: Wrong constant / default value

---

## Root Cause

A bad merge left a reference to the old class name `org.apache.cassandra.db.Column.MAX_NAME_LENGTH` in `ThriftValidation`. The class `Column` was renamed to `Cell` in the composites refactoring, so the reference would not compile (or would resolve to a stale symbol on older classpaths). The fix updates the reference to `Cell.MAX_NAME_LENGTH`.

### Before / After

```java
// BEFORE
int maxNameLength = org.apache.cassandra.db.Column.MAX_NAME_LENGTH;
```

```java
// AFTER
int maxNameLength = Cell.MAX_NAME_LENGTH;
```

---

## How to Spot

After a merge, scan for fully-qualified class names that reference old type names in the diff. A compile error would also catch this immediately — enforcing that the test suite compiles cleanly as part of CI prevents stale symbol regressions.

---

## Test

No test added.
