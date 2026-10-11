**Commit**: a8cdbaf4b3 · **Ticket**: KAFKA-18138
**Subsystem**: metadata/src
**Category**: Logic error in condition

**Root cause**
The controller must add all extant brokers to BrokerHeartbeatTracker when activating. Otherwise, we
could end up in a situation where a broker fails exactly as a controller failover occurs, and we
never fence it. Also, fix a bug where the slf4j logger object in PeriodicTaskControlManager was initialized as
though it belonged to OffsetControlManager. Reviewers: David Mao <dmao@confluent.io>, David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
        this.log = logContext.logger(OffsetControlManager.class);
                .setIncarnationId(Uuid.randomUuid()).setLogDirs(dirs);

// After
        long nowNs = time.nanoseconds();
            heartbeatManager.tracker().updateContactTime(
                new BrokerIdAndEpoch(registration.id(), registration.epoch()), nowNs);
        this.log = logContext.logger(PeriodicTaskControlManager.class);
import java.util.OptionalLong;
                .setIncarnationId(new Uuid(brokerId, brokerId))
                .setLogDirs(dirs);

    @Test
    public void testBrokerContactTimesAreUpdatedOnClusterControlActivation() {
        MockTime time = new MockTime(0L, 20L, 1000L);
        ClusterControlManager clusterControl = new ClusterControlManager.Builder().
```

**How to spot**
When reviewing metadata/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18138: the controller must add all extant brokers to brokerheartbeattracker when activating (#18009).

**Test**
Test modified in ClusterControlManagerTest.java covering the corrected behaviour.
