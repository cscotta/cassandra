# Bug 1784: Fix Build — Wrong Field Name in SelectStatement Memory Measurement

**Commit**: 5200d45a9e · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/cql3/statements
**Category**: Wrong constant / default value

---

## Root Cause

A prior refactoring renamed a field in `SelectStatement` from `restrictedNames` to `restrictedColumns`, but the `measureForPreparedCache` method was not updated to match. The stale field reference caused a compile error. Additionally, `ColumnFamilyStore.java` retained a now-unused import of `IndexExpression` from the Thrift package that was also removed.

### Before / After

```java
// BEFORE
+ meter.measureDeep(restrictedNames)
```

```java
// AFTER
+ meter.measureDeep(restrictedColumns)
```

---

## How to Spot

When a field is renamed during refactoring, search for all uses of the old name in the same class (including less-travelled utility methods like memory-measurement helpers) before committing.

---

## Test

No test added. The fix is verified by successful compilation.
