**Commit**: 40890faa1b · **Ticket**: KAFKA-18592
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
Reviewers: Ismael Juma <ismael@juma.me.uk>, Christo Lolov <lolovc@amazon.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import kafka.server.DelayedElectLeader;
    private Optional<DelayedOperationPurgatory<DelayedElectLeader>> delayedElectLeaderPurgatory = Optional.empty();
    public ReplicaManagerBuilder setIsShuttingDown(AtomicBoolean isShuttingDown) {
        this.isShuttingDown = isShuttingDown;
        return this;
    }

    public ReplicaManagerBuilder setDelayedProducePurgatory(DelayedOperationPurgatory<DelayedProduce> delayedProducePurgatory) {
        this.delayedProducePurgatory = Optional.of(delayedProducePurgatory);
        return this;
    }


// After
// (no additions)
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18592 cleanup replicamanager (#18621).

**Test**
Test modified in AbstractCoordinatorConcurrencyTest.scala covering the corrected behaviour.
