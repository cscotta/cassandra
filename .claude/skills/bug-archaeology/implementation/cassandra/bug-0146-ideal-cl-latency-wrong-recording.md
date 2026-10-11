# Bug 0146: Ideal CL Latency Metric Recorded at Wrong Time and Under Wrong Condition

**Commit**: 6da9141d3c · **Ticket**: CASSANDRA-19651
**Subsystem**: service (AbstractWriteResponseHandler)
**Category**: Logic error in condition

---

## Root Cause

The ideal consistency-level write latency metric was recorded inside `logFailureOrTimeoutToIdealCLDelegate()` (formerly `expired()`) only after a request had timed out, rather than at the moment the ideal CL was actually satisfied. Conversely, the metric was also not recorded when an explicit failure response (`onFailure`) caused the ideal CL to become unachievable. There were two distinct errors: (1) the latency was recorded in the timeout path instead of immediately when the required number of acknowledgements arrived; and (2) `onFailure` did not call the failure-tracking logic at all, so a remote-DC failure would neither record a latency sample nor increment `writeFailedIdealCL`. The fix moves the latency recording to the `signal()` path (where the quorum is first satisfied) and calls `logFailureOrTimeoutToIdealCLDelegate()` from `onFailure`.

### Before / After

```java
// BEFORE – latency recorded inside expired(), after timeout
if (idealCLDelegate != null)
{
    idealCLDelegate.requestedCLAchieved = true;
    // latency recorded later in logFailureOrTimeoutToIdealCLDelegate...
}
// onFailure did not call logFailureOrTimeoutToIdealCLDelegate
```

```java
// AFTER – latency recorded inside signal() when quorum first satisfied
if (idealCLDelegate != null && blockFor() + failures <= candidateReplicaCount())
{
    idealCLDelegate.requestedCLAchieved = true;
    if (idealCLDelegate == this)
        replicaPlan.keyspace().metric.idealCLWriteLatency.addNano(System.nanoTime() - queryStartNanoTime);
}
// onFailure now calls logFailureOrTimeoutToIdealCLDelegate()
```

---

## How to Spot

Latency metrics must be recorded at the instant the condition being measured is satisfied, not at a later cleanup or timeout callback. Similarly, any code path that can prevent the ideal CL from being achieved (including explicit failure responses) must be wired to the same accounting logic as timeouts; omitting one path leaves the metric incomplete and can cause false-negative failure counters.

---

## Test

New unit tests verify: (1) `idealCLWriteLatency` count increments immediately after the EACH_QUORUM quorum is reached, before `expired()` is called; (2) `writeFailedIdealCL` increments when remote-DC failures make ideal CL unachievable but requested CL is met; (3) neither metric fires when the requested CL itself is not met.
