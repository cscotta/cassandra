**Commit**: 36b9bb94f1 · **Ticket**: KAFKA-19474

**Subsystem**: Replication / ReplicaFetcherThread / UnifiedLog

**Category**: Logic error in condition

**Root cause**
`ReplicaFetcherThread.truncate` emitted a `WARN` log when `targetOffset < log.highWatermark`, but checked this condition *after* calling `log.truncateTo()`. At that point the log had already been modified, meaning the high-watermark comparison was made against the post-truncation state rather than the pre-truncation state, potentially missing the warning or comparing against an updated HWM value. Moving the check inside `UnifiedLog.truncateTo`, *before* truncation, ensures the warning fires at the correct moment and also catches the same scenario when truncation is triggered via `ReplicaAlterLogDirsThread`.

**Before/After**
```scala
// Before: check after truncation (Scala, ReplicaFetcherThread)
log.truncateTo(offsetTruncationState.offset)
if (offsetTruncationState.offset < log.highWatermark)
    warn(s"Truncating $tp to offset ... below high watermark ...")

// After: check inside UnifiedLog.truncateTo, before truncation (Java)
long hwm = highWatermark();
if (targetOffset < hwm) {
    logger.warn("Truncating {}{} to offset {} below high watermark {}",
        isFuture() ? "future " : "", topicPartition(), targetOffset, hwm);
}
// ... perform truncation ...
```

**How to spot**
Warning checks that reference mutable state (e.g., high-watermark) must be evaluated before the state-mutating operation, not after. Review all truncation/truncateTo call sites for post-mutation checks that should be pre-mutation.

**Test**
`UnifiedLogTest` adds a scenario where `truncateTo` is called with a target below the high-watermark and asserts that the `WARN` message is emitted with the correct pre-truncation HWM value.
