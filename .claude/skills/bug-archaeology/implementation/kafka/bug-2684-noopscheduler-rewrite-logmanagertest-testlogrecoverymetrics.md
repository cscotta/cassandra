**Commit**: 44a44b753f · **Ticket**: KAFKA-17166
**Subsystem**: storage/src
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Okada Haruki <ocadaruma@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.server.util.{KafkaScheduler, MockTime}
        scheduler = mockTime.scheduler,
                List<EpochEntry> entries = new ArrayList<>(epochs.values());
                scheduler.scheduleOnce("leader-epoch-cache-flush-" + topicPartition, () -> checkpoint.writeForTruncation(entries));
                List<EpochEntry> entries = new ArrayList<>(epochs.values());
                scheduler.scheduleOnce("leader-epoch-cache-flush-" + topicPartition, () -> checkpoint.writeForTruncation(entries));

// After
import org.apache.kafka.server.util.{KafkaScheduler, MockTime, Scheduler}
        scheduler = mock(classOf[Scheduler]),
                scheduler.scheduleOnce("leader-epoch-cache-flush-" + topicPartition, this::writeToFileForTruncation);
                scheduler.scheduleOnce("leader-epoch-cache-flush-" + topicPartition, this::writeToFileForTruncation);

    private void writeToFileForTruncation() {
        lock.readLock().lock();
        try {
            checkpoint.writeForTruncation(epochs.values());
        } finally {
            lock.readLock().unlock();
        }
```

**How to spot**
When reviewing storage/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17166 use noopscheduler to rewrite logmanagertest#testlogrecoverymetrics (#16641).

**Test**
Test modified in LogManagerTest.scala covering the corrected behaviour.
