**Commit**: 4e36368d08 · **Ticket**: KAFKA-18743
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Remove `leader.imbalance.per.broker.percentage` from config. Add `leader.imbalance.per.broker.percentage` to release note

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  val leaderImbalancePerBrokerPercentage = getInt(ReplicationConfigs.LEADER_IMBALANCE_PER_BROKER_PERCENTAGE_CONFIG)
        case ReplicationConfigs.LEADER_IMBALANCE_PER_BROKER_PERCENTAGE_CONFIG => assertPropertyInvalid(baseProperties, name, "not_a_number")
    public static final String LEADER_IMBALANCE_PER_BROKER_PERCENTAGE_CONFIG = "leader.imbalance.per.broker.percentage";
    public static final int LEADER_IMBALANCE_PER_BROKER_PERCENTAGE_DEFAULT = 10;
    public static final String LEADER_IMBALANCE_PER_BROKER_PERCENTAGE_DOC = "The ratio of leader imbalance allowed per broker. The controller would trigger a leader balance if it goes above this value per broker. The value is specified in percentage.";

    public static final String AUTO_LEADER_REBALANCE_ENABLE_DOC = String.format("Enables auto leader balancing. A background thread checks the distribution of partition leaders at regular intervals, configurable by %s. If the leader imbalance exceeds %s, leader rebalance to the preferred leader for partitions is triggered.",
           LEADER_IMBALANCE_CHECK_INTERVAL_SECONDS_CONFIG, LEADER_IMBALANCE_PER_BROKER_PERCENTAGE_CONFIG);

// After
        <li>
            <p>
                Removed the leader imbalance configuration used only in ZooKeeper. <code>leader.imbalance.per.broker.percentage</code>
                was used to limit the preferred leader election frequency in ZooKeeper.
            </p>
            <ul>
                <li><code>leader.imbalance.per.broker.percentage</code></li>
            </ul>
        </li>
    public static final String AUTO_LEADER_REBALANCE_ENABLE_DOC = String.forma
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18743 remove leader.imbalance.per.broker.percentage as it is not supported by kraft (#18821).

**Test**
Test modified in KafkaConfigTest.scala covering the corrected behaviour.
