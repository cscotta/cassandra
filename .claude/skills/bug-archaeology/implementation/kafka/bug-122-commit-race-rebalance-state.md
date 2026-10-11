**Commit**: 62bec20aef · **Ticket**: KAFKA-19242
**Subsystem**: clients / consumer / internals / ConsumerCoordinator
**Category**: Race condition / missing synchronization

**Root cause**
`ConsumerCoordinator.sendOffsetCommitRequest()` read the group generation and rebalance state with two unsynchronized calls: `generationIfStable()` (which holds the state lock internally and then releases it) followed by `rebalanceInProgress()` (which reads the volatile state again). The heartbeat thread could transition the group from `COMPLETING_REBALANCE` to `STABLE` in the gap between these two reads. The commit thread then saw `generationIfStable() == null` (state was `COMPLETING_REBALANCE` at the first read) and `rebalanceInProgress() == false` (state is now `STABLE`), causing a spurious `CommitFailedException` even though the commit could have succeeded.

**Before/After**
```java
// Before — two separate, unsynchronized reads
generation = generationIfStable();    // read 1 (COMPLETING_REBALANCE)
// heartbeat thread flips to STABLE here
if (generation == null) {
    if (rebalanceInProgress()) { ... } // read 2 (now STABLE → wrong branch)
    else { throw CommitFailedException; }
}

// After — both reads inside a single synchronized block
synchronized (ConsumerCoordinator.this) {
    generation = generationIfStable();
    if (generation == null) {
        if (rebalanceInProgress()) { ... }
        else { throw CommitFailedException; }
    }
}
```

**How to spot**
Any check-then-act sequence that reads a state machine variable more than once without a lock is vulnerable to TOCTOU races. In Kafka's consumer coordinator, the state is shared between the network thread (heartbeat) and the application thread (commit), so all multi-read sequences need synchronization.

**Test**
Regression test implicitly covered; this commit is primarily a synchronization fix without a dedicated new unit test, but the existing `ConsumerCoordinatorTest` exercises the commit path.
