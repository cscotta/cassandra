# Bug 1284: `isSwapEnabled()` Returns Inverted Boolean — Swap Present Reported as Absent

**Commit**: e983956c28 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/utils/SigarLibrary.java
**Category**: Wrong constant / default value

---

## Root Cause

The `isSwapEnabled()` method in `SigarLibrary` used an inverted conditional: when `swapSize > 0` (swap is present and enabled) it returned `false`, and when swap was absent it returned `true`. This caused Cassandra's startup health check to suppress the swap-enabled warning on systems that actually had swap, and to spuriously warn (or fail silently) on systems without swap. The logic was structurally correct but the return values in each branch were simply swapped.

### Before / After

```java
// BEFORE — returns false when swap is present, true when absent
if (swapSize > 0)
{
    return false;
}
else
{
    return true;
}
```

```java
// AFTER — returns true when swap is present, false when absent
if (swapSize > 0)
{
    return true;
}
else
{
    return false;
}
```

---

## How to Spot

Diagnostic and health-check predicates named `isXEnabled()` or `isXPresent()` that read a size or count and return a boolean are highly susceptible to inversion bugs. Review them by mentally substituting extreme values: if `swapSize` is 1 GB, `isSwapEnabled()` should return `true`. If the code returns `false` for a non-zero size, the logic is inverted. Pay special attention to methods that were converted from a "no swap is good" framing (`swapSize == 0 → good → return false` for "not degraded") to a straightforward enabled/disabled predicate without updating both branches.

---

## Test

No test added.
