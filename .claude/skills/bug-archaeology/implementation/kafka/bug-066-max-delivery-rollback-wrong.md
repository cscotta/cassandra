**Commit**: dc96e29499 · **Ticket**: KAFKA-19476
**Subsystem**: server/src/main/java/org/apache/kafka/server/share/fetch/InFlightState.java
**Category**: Logic error in condition

**Root cause**
When an acquisition lock timeout occurred concurrently with a write-state RPC failure, `InFlightState.rollback()` used the rollback state's delivery count to determine what state to revert to, but it did not check whether the delivery count had reached the maximum. If `deliveryCount >= maxDeliveryCount`, the record should be `ARCHIVED`, not put back to `AVAILABLE`. Additionally, the rollback was reverting the `deliveryCount` field itself, which should never decrease — a delivery attempt that failed a write still happened. The fix captures `maxDeliveryCount` in the `RollbackState` record and uses it to decide whether to archive or make available after a lock timeout.

**Before/After**
```java
// Before: always reverts to AVAILABLE on lock-expired rollback; also resets deliveryCount
if (acquisitionLockTimeoutTask != null && acquisitionLockTimeoutTask.hasExpired()) {
    state = RecordState.AVAILABLE;
    memberId = EMPTY_MEMBER_ID;
    cancelAndClearAcquisitionLockTimeoutTask();
} else {
    state = rollbackState.state;
    memberId = rollbackState.memberId;
}
deliveryCount = rollbackState.deliveryCount();   // incorrectly reverts count

// After: archives when max delivery reached; preserves delivery count
state = previousState.deliveryCount() >= rollbackState.maxDeliveryCount ?
    RecordState.ARCHIVED : RecordState.AVAILABLE;
// deliveryCount NOT reverted
```

**How to spot**
In state-machine rollback paths, verify that (a) terminal conditions like "max delivery reached" are re-evaluated after rollback, not assumed unchanged, and (b) monotonically increasing counters like delivery counts are never reverted to an earlier value. A rollback that decrements a delivery count allows messages to circulate more times than `maxDeliveryCount`.

**Test**
`testRecordArchivedWithWriteStateRPCFailure` sets `maxDeliveryCount=2`, acknowledges batches, simulates two write-state RPC failures and an acquisition-lock expiry, then asserts records reach `ARCHIVED` state with `deliveryCount=2` as expected.
