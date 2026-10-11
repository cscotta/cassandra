# Bug 0872: HDR Histogram Logging Skipped for Single-Operation Workloads

**Commit**: c1dcc9ce46 · **Ticket**: CASSANDRA-12145
**Subsystem**: cassandra-stress / metrics
**Category**: Logic error in condition

---

## Root Cause
In `StressMetrics`, HDR histogram logging (`logHistograms`) was nested inside the same `if` block that guarded the per-operation summary print line. That guard was `result.intervals.intervals().size() > 1` — i.e., it only fired when more than one operation type was present. For single-operation workloads there is exactly one operation type, so `logHistograms` was never called and no HDR data was recorded.

### Before / After
```java
// BEFORE
if (result.intervals.intervals().size() > 1)
{
    for (Map.Entry<String, TimingInterval> type : result.intervals.intervals().entrySet())
    {
        printRow(...);
        logHistograms(opName, opInterval);  // unreachable for single-op workloads
    }
}

// AFTER
final boolean logPerOpSummaryLine = result.intervals.intervals().size() > 1;
for (Map.Entry<String, TimingInterval> type : result.intervals.intervals().entrySet())
{
    if (logPerOpSummaryLine)
        printRow(...);      // still guarded
    logHistograms(opName, opInterval);  // always called
}
```

---

## How to Spot
When a logging or metric-recording call is placed inside a branch that exists for display purposes only, it silently produces no output even though the conceptual precondition (at least one operation ran) is satisfied. Review any metrics/logging call that is nested inside a count-or-size guard and ask whether the guard really applies to the recording, or only to the presentation.

---

## Test
No dedicated test added; the change is in the stress tool metrics path.
