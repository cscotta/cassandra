# Bug 1932: Divide-by-Zero in HintedHandOffManager Page Size Calculation

**Commit**: 8cc28a1477 · **Ticket**: CASSANDRA-5990
**Subsystem**: src/java/org/apache/cassandra/db/HintedHandOffManager.java
**Category**: Missing null / bounds check

---

## Root Cause

`HintedHandOffManager.calculatePageSize()` computed the average column size as `hintStore.getMeanRowSize() / hintStore.getMeanColumns()`. The guard condition checked `hintStore.getMeanColumns() > 0` but called `getMeanColumns()` a second time inside the `if` block when computing the divisor. Between the check and the division, a concurrent compaction or stats update could change the mean column count to zero, causing a divide-by-zero arithmetic exception. The fix captures the mean column count in a local variable before the check, ensuring the same non-zero value is used for the division.

### Before / After

```java
// BEFORE
if (hintStore.getMeanColumns() > 0)
{
    int averageColumnSize = (int) (hintStore.getMeanRowSize() / hintStore.getMeanColumns());
    ...
}
```

```java
// AFTER
int meanColumnCount = hintStore.getMeanColumns();
if (meanColumnCount > 0)
{
    int averageColumnSize = (int) (hintStore.getMeanRowSize() / meanColumnCount);
    ...
}
```

---

## How to Spot

Whenever a method is called twice — once as a guard condition and once as a divisor — there is a TOCTOU window. Always capture volatile or stat-returning values in a local variable and use that variable for both the guard and the computation.

---

## Test

No test added.
