**Commit**: bf42924126 · **Ticket**: KAFKA-19572
**Subsystem**: clients / consumer / internals / ShareConsumerImpl
**Category**: Missing null / bounds check

**Root cause**
`ShareConsumerImpl`'s constructor initializes several fields (`applicationEventHandler`, `backgroundEventProcessor`, `backgroundEventReaper`, `backgroundEventQueue`) in sequence. If the constructor fails partway through (e.g., an invalid `METRIC_REPORTER_CLASSES_CONFIG`), the catch block calls `close()`, which in turn calls `stopFindCoordinatorOnClose()`, `sendAcknowledgementsAndLeaveGroup()`, and `handleCompletedAcknowledgements()`. These methods dereferenced the uninitialized (null) fields unconditionally, producing a NPE that was logged but not surfaced clearly to the user.

**Before/After**
```java
// Before — unconditional field access in close helpers
private void stopFindCoordinatorOnClose() {
    applicationEventHandler.add(new StopFindCoordinatorOnCloseEvent()); // NPE if null
}

// After — null guard at method entry
private void stopFindCoordinatorOnClose() {
    if (applicationEventHandler == null) return;
    applicationEventHandler.add(new StopFindCoordinatorOnCloseEvent());
}
```

**How to spot**
Any `close()` / `cleanup()` method (and all helpers it calls) must tolerate partially constructed state. Check each field access for nullability when construction can fail at any point in a multi-step init sequence.

**Test**
`ShareConsumerImplTest.testConstructorWithInvalidClassConfig` — constructs a `ShareConsumerImpl` with a bogus `METRIC_REPORTER_CLASSES_CONFIG`; uses `LogCaptureAppender` to assert that no `NullPointerException` is logged during the failed construction.
