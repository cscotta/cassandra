**Commit**: cf9d517d71 · **Ticket**: KAFKA-17142
**Subsystem**: storage/src
**Category**: Deadlock

**Root cause**
Reviewers: Luke Chen <showuon@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                scheduler.scheduleOnce("leader-epoch-cache-flush-" + topicPartition, this::writeToFileForTruncation);
                scheduler.scheduleOnce("leader-epoch-cache-flush-" + topicPartition, this::writeToFileForTruncation);

    private void writeToFileForTruncation() {
        List<EpochEntry> entries;
        lock.readLock().lock();
        try {
            entries = new ArrayList<>(epochs.values());
        } finally {
            lock.readLock().unlock();
        }
        checkpoint.writeForTruncation(entries);

// After
                List<EpochEntry> entries = new ArrayList<>(epochs.values());
                scheduler.scheduleOnce("leader-epoch-cache-flush-" + topicPartition, () -> checkpoint.writeForTruncation(entries));
                List<EpochEntry> entries = new ArrayList<>(epochs.values());
                scheduler.scheduleOnce("leader-epoch-cache-flush-" + topicPartition, () -> checkpoint.writeForTruncation(entries));
```

**How to spot**
When reviewing storage/src code, look for deadlock patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17142 fix deadlock caused by logmanagertest#testlogrecoverymetrics (#16614).

**Test**
No test added.
