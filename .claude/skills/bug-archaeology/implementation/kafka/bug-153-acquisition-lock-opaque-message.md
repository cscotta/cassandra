**Commit**: b5f5265864 · **Ticket**: KAFKA-18796
**Subsystem**: core/src/test/java/kafka/server/share/SharePartitionTest.java
**Category**: Missing null / bounds check

**Root cause**
Test assertions for acquisition lock timeout scenarios used a static string constant `ACQUISITION_LOCK_NEVER_GOT_RELEASED` as the failure message supplier. When the assertion timed out, the message gave no information about the actual share partition state, making it impossible to diagnose why the lock was not released. The fix replaces the static string with a dynamically generated `assertionFailedMessage` that captures the current cached state and offset information at the time of failure. A new `id()` accessor was also added to the `RecordState` enum to support this message builder.

**Before/After**
```java
// Before: opaque static message
TestUtils.waitForCondition(
    () -> sharePartition.cachedState().get(0L).batchAcquisitionLockTimeoutTask() == null,
    DEFAULT_MAX_WAIT_ACQUISITION_LOCK_TIMEOUT_MS,
    () -> ACQUISITION_LOCK_NEVER_GOT_RELEASED);

// After: rich diagnostic message with current partition state
TestUtils.waitForCondition(
    () -> sharePartition.cachedState().get(0L).batchAcquisitionLockTimeoutTask() == null,
    DEFAULT_MAX_WAIT_ACQUISITION_LOCK_TIMEOUT_MS,
    () -> assertionFailedMessage(sharePartition, Map.of(0L, List.of())));
```

**How to spot**
This is a test-quality improvement rather than a production logic bug. Look for `waitForCondition` calls where the message supplier is a constant string that reveals nothing about the failing state. Any flaky test with an opaque timeout message is a candidate.

**Test**
No new production-logic test added; the commit improves existing acquisition lock timeout tests by enriching their failure messages using the new `RecordState.id()` accessor and a helper method.
