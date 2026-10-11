# Bug 0904: NPE in Bootstrap Error Handler via getCause

**Commit**: f463fed234 · **Ticket**: CASSANDRA-11272
**Subsystem**: Bootstrap / StorageService
**Category**: Missing null / bounds check

---

## Root Cause
In the `onFailure` callback for the bootstrap future inside `StorageService`, the code called `e.getCause().getMessage()` and passed `e.getCause()` as the logger exception argument. `Throwable.getCause()` returns `null` when no chained cause was set, so any bootstrap failure where the top-level exception had no cause would immediately throw a secondary `NullPointerException`, hiding the real error. The fix replaces the entire expression with a direct log of the original exception `e`, which always exists.

### Before / After
```java
// BEFORE
logger.warn("Error during bootstrap: " + e.getCause().getMessage(), e.getCause());

// AFTER
logger.warn("Error during bootstrap.", e);
```

---

## How to Spot
Whenever a log or error-reporting path calls `getCause()` on a `Throwable` parameter without a null guard, it can produce a secondary NPE that masks the underlying problem entirely. In review, treat every bare `.getCause().` chain as a potential NPE and require either an `if (e.getCause() != null)` guard or replacement with the top-level exception itself, which is almost always what the caller really wants to log.

---

## Test
No test added.
