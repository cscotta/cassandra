# Bug 0269: Read Abort Leaves Open Range Tombstone Bound, Triggering RTBoundValidator Exception

**Commit**: f4b69ba0e8 · **Ticket**: CASSANDRA-17810
**Subsystem**: src/java/org/apache/cassandra/db/ReadCommand.java
**Category**: State not cleaned up

---

## Root Cause

The old `CheckForAbort` transformation returned `null` from `applyToRow` to stop iteration when a query was aborted by timeout. Returning `null` from inside a range tombstone sequence (i.e. after an open `RangeTombstoneMarker` but before its matching close marker) left the row iterator in an inconsistent state—an open RT bound with no close. The `RTBoundValidator` applied later in the pipeline then detected this malformed sequence and threw an `IllegalStateException` ("open RT bound as its last item"), which appeared as an unexpected error in logs rather than a clean timeout. The fix introduces a dedicated `QueryCancellationChecker` transformation that throws `QueryCancelledException` (a `RuntimeException`) instead of returning `null`, so the iterator is unwound properly and the partial-response path never passes the broken stream through the validator.

### Before / After

```java
// BEFORE – returning null silently truncates the iterator mid-RT sequence
protected Row applyToRow(Row row)
{
    if (TEST_ITERATION_DELAY_MILLIS > 0) maybeDelayForTesting();
    return maybeAbort() ? null : row;
}
```

```java
// AFTER – throw to unwind the iterator cleanly
private void maybeCancel()
{
    if (isAborted())
    {
        stop();
        throw new QueryCancelledException(ReadCommand.this);
    }
}
```

---

## How to Spot

Transformations that operate on a streaming row iterator must not silently drop items mid-sequence when the sequence has paired open/close markers (range tombstone bounds, boundaries). Returning `null` from `applyToRow` while inside an open RT is always unsafe; the correct pattern is to throw or to propagate the abort signal in a way that the validator layer never sees the truncated stream.

---

## Test

`TimeoutAbortTest.timeoutTest` (distributed test) verifies that a timed-out read produces a `ReadTimeoutException` and that no "open RT bound" error appears in node logs.
