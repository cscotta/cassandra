**Commit**: 4fc9e442c3 · **Ticket**: KAFKA-17898
**Subsystem**: clients/src
**Category**: State not cleaned up

**Root cause**
With KAFKA-14562, we implemented epoch bump on both the client and the server. Mentioned below are the different epoch bump scenarios we have on hand after enabled tv2

Non-Transactional Producers
• Epoch bumping is always allowed. • Different code paths are used to handle epoch bumping. Transactional Producers

No Epoch Bump Allowed
• coordinatorSupportsBumpingEpoch = false when initPIDVersion < 3 or initPIDVersion = null.

**Before/After**
```java
// Before
    private volatile boolean epochBumpRequired = false;
        if (epochBumpRequired) {
    synchronized void requestEpochBumpForPartition(TopicPartition tp) {
        epochBumpRequired = true;
        epochBumpRequired = false;
            if (epochBumpRequired) {
            if (canBumpEpoch() && !isCompleting()) {
                epochBumpRequired = true;
            requestEpochBumpForPartition(batch.topicPartition);

// After
    private volatile boolean clientSideEpochBumpRequired = false;
        if (clientSideEpochBumpRequired) {
    /**
     * Transitions to an abortable error state if the coordinator can handle an abortable error or
     * to a fatal error if not.
     *
     * @param abortableException    The exception in case of an abortable error.
     * @param fatalException        The exception in case of a fatal error.
     */
    private void transitionToAbortableErrorOrFatalError(
        RuntimeException abortableException,
        RuntimeException fatalException
```

**How to spot**
When reviewing clients/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17898: refine epoch bumping logic (#17849).

**Test**
Test modified in TransactionManagerTest.java covering the corrected behaviour.
