# Bug 2184: Tracing log line emitted before loop completion, skipping relevant cells

**Commit**: f725ed1390 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/filter/SliceQueryFilter.java
**Category**: Logic error in condition

---

## Root Cause

In `SliceQueryFilter.collectReducedColumns`, the debug log statement that reports the count of live and tombstoned cells was placed inside the `if (columnCounter.live() > count)` branch that also executed the `break`. As a result, the log line was only emitted when the column count limit was exceeded mid-loop, and it reported the in-progress counter value rather than the final tally. When the loop exited normally (without hitting the count limit), no trace was emitted at all.

### Before / After

```java
// BEFORE
if (columnCounter.live() > count)
{
    logger.debug("Read %s live columns and %s tombstoned", columnCounter.live(), columnCounter.ignored());
    break;
}
```

```java
// AFTER
if (columnCounter.live() > count)
    break;

// ... (loop ends)

logger.debug("Read {} live cells and {} tombstoned", columnCounter.live(), columnCounter.ignored());
```

---

## How to Spot

Watch for log/trace statements that are gated inside an early-exit branch. If the message is meant to summarize a loop's results, it must live outside the loop (or at least outside the early-exit condition) so it fires unconditionally on every code path.

---

## Test

No test added.
