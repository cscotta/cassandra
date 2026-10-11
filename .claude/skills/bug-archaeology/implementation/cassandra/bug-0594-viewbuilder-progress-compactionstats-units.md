# Bug 0594: ViewBuilder Reports Wrong Progress and CompactionStats Converts Non-Byte Units as Bytes

**Commit**: 9d498dced6 · **Ticket**: CASSANDRA-12244
**Subsystem**: `org.apache.cassandra.db.view`, `org.apache.cassandra.db.compaction`, `org.apache.cassandra.tools.nodetool`
**Category**: Logic error in condition

---

## Root Cause

Two independent but related bugs both affected progress reporting in `nodetool compactionstats`.

First, `ViewBuilder.getCompactionInfo()` computed progress by counting how many ranges remained to the left of the last-processed token (`rangesLeft`) and submitted that as the `completed` figure. Since `rangesLeft` started at the total and counted down, it would be zero at the exact point where `lastToken` was inside a range, giving the nonsensical result that 0 out of N ranges had been completed at various stages. The fix accumulates `rangesCompleted` by counting ranges whose right endpoint is strictly past `lastToken`.

Second, `CompactionStats` applied `FileUtils.stringifyFileSize` (a byte-to-human-readable converter) to all compaction tasks when `--human-readable` was passed, including tasks that report progress in `ranges` or `keys`. The string unit was free-form, so the tool had no reliable way to distinguish byte-based progress from count-based progress. The fix introduces a `CompactionInfo.Unit` enum and restricts humanization to tasks reporting `Unit.BYTES`.

### Before / After

```java
// BEFORE (ViewBuilder)
rangesLeft++;
rangesTotal++;
if (lastToken == null || range.contains(lastToken))
    rangesLeft = 0;
return new CompactionInfo(..., rangesLeft, rangesTotal, "ranges", ...);

// BEFORE (CompactionStats)
String completedStr = humanReadable ? FileUtils.stringifyFileSize(completed) : Long.toString(completed);
```

```java
// AFTER (ViewBuilder)
rangesTotal++;
if ((lastToken != null) && lastToken.compareTo(range.right) > 0)
    rangesCompleted++;
return new CompactionInfo(..., rangesCompleted, rangesTotal, Unit.RANGES, ...);

// AFTER (CompactionStats)
boolean toFileSize = humanReadable && Unit.isFileSize(unit);
String completedStr = toFileSize ? FileUtils.stringifyFileSize(completed) : Long.toString(completed);
```

---

## How to Spot

Progress fields named `*Left` or `*Remaining` are frequently misused as the `completed` argument in progress-reporting APIs that expect a monotonically increasing value. Any site passing a decreasing counter as "completed" deserves scrutiny. Separately, whenever a display formatter is gated only on a `humanReadable` flag without also checking the unit type, it will corrupt non-byte quantities.

---

## Test

No test added.
