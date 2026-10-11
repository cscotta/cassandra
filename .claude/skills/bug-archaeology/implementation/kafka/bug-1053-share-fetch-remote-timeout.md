## Commit

239dce3e04 — KAFKA-19291: Share fetch remote storage requests ignore remoteFetchMaxWaitMs

## Ticket

KAFKA-19291

## Subsystem

Share groups / remote log storage

## Category

Wrong constant / default value

## Root cause

`DelayedShareFetch` inherited the standard fetch `maxWaitMs` (default 500 ms) as its expiry timeout. When a share fetch triggered a remote-storage read, the delay operation expired after 500 ms regardless of the configured `remoteFetchMaxWaitMs`, which is intentionally larger to accommodate slower remote reads. The fetch was therefore prematurely expired and returned an empty response to the client before the remote data arrived. The fix registers a `PendingRemoteFetchTimerTask` that extends the deadline to `remoteFetchMaxWaitMs` once a remote fetch is detected, and also wires a completion callback on `PendingRemoteFetches` so the delay operation wakes up as soon as the remote read completes.

## Before / After

```java
// Before — fixed maxWaitMs regardless of remote fetch
delayedOperationPurgatory.tryCompleteElseWatch(
    new DelayedShareFetch(fetchParams, ...), keys);

// After — separate timer task for remote fetch timeout
if (pendingRemoteFetches != null) {
    pendingRemoteFetches.addCompletionCallback(this::forceComplete);
    purgatory.schedule(new PendingRemoteFetchTimerTask(
        remoteFetchMaxWaitMs, this::forceComplete));
}
```

## How to spot

Search for `DelayedShareFetch` or `DelayedFetch` constructions that pass `fetchParams.maxWaitMs` without checking whether a remote-storage fetch is in progress. Any purgatory delay that does not account for `remoteFetchMaxWaitMs` will cut short tiered-storage reads.

## Test

`DelayedShareFetchTest` was extended with a case that verifies the delay is not completed before `remoteFetchMaxWaitMs` when a pending remote fetch is outstanding.
