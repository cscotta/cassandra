**Commit**: 3d49159c84 · **Ticket**: KAFKA-18598
**Subsystem**: docs/zk2kraft.html
**Category**: Logic error in condition

**Root cause**
Reviewers: Christo Lolov <lolovc@amazon.com>

**Before/After**
```java
// Before
    private static final MetricName MIGRATING_ZK_BROKER_COUNT = getMetricName(
        "KafkaController", "MigratingZkBrokerCount");
    private static final MetricName ZK_MIGRATION_STATE = getMetricName(
        "KafkaController", "ZkMigrationState");
    private final AtomicInteger migratingZkBrokerCount = new AtomicInteger(0);
    private final AtomicInteger zkMigrationState = new AtomicInteger(-1);
        registry.ifPresent(r -> r.newGauge(ZK_MIGRATION_STATE, new Gauge<Integer>() {
            @Override
            public Integer value() {
                return (int) zkMigrationState();
            }
        }));

// After
        <li>
            <p>
                Remove the metrics which is monitoring the number of Zookeeper migrations.
            </p>
            <ul>
                <li><code>kafka.controller:type=KafkaController,name=MigratingZkBrokerCount</code></li>
                <li><code>kafka.controller:type=KafkaController,name=ZkMigrationState</code></li>
            </ul>
            <p>
                Kafka remove all zookeeper dependencies, so the metrics is removed.
            </p>
        </li>
```

**How to spot**
When reviewing docs/zk2kraft.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18598: remove controllermetadatametrics zk-related metrics (#18629).

**Test**
Test modified in ControllerMetadataMetricsTest.java covering the corrected behaviour.
