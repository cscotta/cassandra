**Commit**: a663ce3f45 · **Ticket**: KAFKA-18265

**Subsystem**: Share Groups / SharePartition

**Category**: Refactoring correctness (extract class, lambda handler)

**Root cause**
`AcquisitionLockTimerTask` and `AcquisitionLockTimeoutHandler` were nested inside `SharePartition`, tightly coupling the timer logic to the partition class. Extracting them to the `share.fetch` package required converting the timeout callback from an anonymous inner-class reference to an explicit lambda and updating all constructor call sites. Leaving any call site referencing the old inner-class path would cause a `NoClassDefFoundError` or compilation failure.

**Before/After**
```java
// Before: anonymous inner class capturing SharePartition.this
AcquisitionLockTimerTask task = new AcquisitionLockTimerTask(
    new AcquisitionLockTimeoutHandler() {
        @Override public void handle(int batchId) {
            SharePartition.this.handleTimeout(batchId);
        }
    });

// After: lambda after extraction to share.fetch package
AcquisitionLockTimerTask task = new AcquisitionLockTimerTask(
    batchId -> handleTimeout(batchId));
```

**How to spot**
When extracting a nested class that holds a back-reference via an anonymous handler, verify that every instantiation site is updated to the new package-qualified name and that the callback no longer captures `OuterClass.this` implicitly.

**Test**
`SharePartitionTest` covers acquisition lock timeout paths to confirm the extracted handler still fires and transitions records to the correct state.
