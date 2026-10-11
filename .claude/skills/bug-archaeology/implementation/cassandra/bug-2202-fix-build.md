# Bug 2202: Wrong Method Name Called in AbstractCompositeType.getString

**Commit**: 0f255da32f · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/marshal/AbstractCompositeType.java
**Category**: Logic error in condition

---

## Root Cause

After the `getString` method was rewritten in commit 5d5207b911 (CASSANDRA-4842), the new implementation called a non-existent method name `getAndAppendNextComparator` instead of the correct `getAndAppendComparator`. The stale method name was carried over from the old implementation and caused a compilation failure. This follow-up commit corrected the method name to match the actual declared method.

### Before / After

```java
// BEFORE
AbstractType<?> comparator = getAndAppendNextComparator(i, bb, sb);
```

```java
// AFTER
AbstractType<?> comparator = getAndAppendComparator(i, bb, sb);
```

---

## How to Spot

When refactoring method names, use IDE-level rename refactoring (not text search-and-replace) to catch all call sites. A compilation step in CI will also catch this immediately.

---

## Test

No test added.
