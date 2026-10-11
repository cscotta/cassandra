# Bug 1595: Fix Rare Potential NPE in HHOM Page Size Calculation

**Commit**: 1e49abaf9c · **Ticket**: CASSANDRA-7354
**Subsystem**: src/java/org/apache/cassandra/db/HintedHandOffManager.java
**Category**: Missing null / bounds check

---

## Root Cause

`HintedHandOffManager.calculatePageSize()` computed `averageColumnSize` by dividing `getMeanRowSize()` by `getMeanColumns()`. While there was a guard for `meanColumnCount <= 0`, the computed `averageColumnSize` itself could also be zero (e.g., if mean row size returns 0 or a very small value that integer-divides to zero), leading to a divide-by-zero in `DatabaseDescriptor.getInMemoryCompactionLimit() / averageColumnSize`. This NPE/ArithmeticException was rare but possible when the hint store had no meaningful statistics yet.

### Before / After

```java
// BEFORE: only guards meanColumnCount, not averageColumnSize
if (meanColumnCount > 0)
{
    int averageColumnSize = (int) (hintStore.getMeanRowSize() / meanColumnCount);
    return Math.max(2, Math.min(PAGE_SIZE,
        DatabaseDescriptor.getInMemoryCompactionLimit() / averageColumnSize));
}
else
{
    return PAGE_SIZE;
}
```

```java
// AFTER: also guards averageColumnSize against zero
if (meanColumnCount <= 0)
    return PAGE_SIZE;

int averageColumnSize = (int) (hintStore.getMeanRowSize() / meanColumnCount);
if (averageColumnSize <= 0)
    return PAGE_SIZE;

return Math.max(2, Math.min(PAGE_SIZE,
    DatabaseDescriptor.getInMemoryCompactionLimit() / averageColumnSize));
```

---

## How to Spot

Any integer division where the divisor is derived from a runtime measurement (mean, average, size) should be guarded against zero even if an earlier guard was applied to a related value. When refactoring guard conditions, confirm that every computed intermediate value used as a divisor is also checked.

---

## Test

No test added.
