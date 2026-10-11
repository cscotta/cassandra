**Commit**: 5cf112dc39 · **Ticket**: KAFKA-17766
**Subsystem**: storage/src
**Category**: Deadlock

**Root cause**
KAFKA-17766: Issue Details: Inside TopicBasedRemoteLogMetadataManager::close, one thread(t1) is calling join on initializationThread thread after taking writeLock on "lock" object => t1 will wait for initializationThread to complete. Internally initializationThread is also using writeLock on "lock" object. This can cause deadlock in below situation

initializationThread is started
close has been invoked as part of a separate thread. But this thread is not yet scheduled by OS.

**Before/After**
```java
// Before
            lock.writeLock().lock();
            try {
                if (initializationThread != null) {
                    try {
                        initializationThread.join();
                    } catch (InterruptedException e) {
                        log.error("Initialization thread was interrupted while waiting to join on close.", e);
                    }

                Utils.closeQuietly(producerManager, "ProducerTask");
                Utils.closeQuietly(consumerManager, "RLMMConsumerManager");
                Utils.closeQuietly(remotePartitionMetadataStore, "RemotePartitionMetadataStore");

// After
                    initializationFailed = true;
            if (initializationThread != null) {
                try {
                    initializationThread.join();
                } catch (InterruptedException e) {
                    log.error("Initialization thread was interrupted while waiting to join on close.", e);

            Utils.closeQuietly(producerManager, "ProducerTask");
            Utils.closeQuietly(consumerManager, "RLMMConsumerManager");
            Utils.closeQuietly(remotePartitionMetadataStore, "RemotePartitionMetadataStore");
            log.info("Closed topic-based RLMM resources");
```

**How to spot**
When reviewing storage/src code, look for deadlock patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17766: fixing deadlock in topicbasedremotelogmetadatamanager (#17492).

**Test**
No test added.
