**Commit**: ed367cf224 · **Ticket**: KAFKA-20060
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
[KAFKA-20060](https://issues.apache.org/jira/browse/KAFKA-20060) This PR
refactors the JMH benchmarks module to remove dependencies on test
utilities from the `kafka-server` module and replaces them with
appropriate alternatives from the test infrastructure or new utility
classes. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    implementation project(':core').sourceSets.test.output
import kafka.utils.TestUtils;
        KafkaConfig config =  KafkaConfig.fromProps(TestUtils.createBrokerConfig(
            0, true, true, 9092, Option.empty(), Option.empty(),
            Option.empty(), true, false, 0, false, 0, false, 0, Option.empty(), 1, true, 1,
            (short) 1, false));
            setAlterPartitionManager(TestUtils.createAlterIsrManager()).
import kafka.utils.TestUtils;


// After
    <allow class="org.apache.kafka.raft.KRaftConfigs"/>
    <allow class="org.apache.kafka.test.TestUtils"/>
    <allow class="org.apache.kafka.jmh.util.BenchmarkConfigUtils"/>
import kafka.server.AlterPartitionManager;
import org.apache.kafka.jmh.util.BenchmarkConfigUtils;
        Properties configs = BenchmarkConfigUtils.createDummyBrokerConfig();
        KafkaConfig config =  KafkaConfig.fromProps(configs);
        AlterPartitionManager alterPartitionManager = Mockito.mock(AlterPartitionManager.class);
            setAlterPartitionManager(alterPartitionManager).
import org.apache.kafka.common.record.SimpleRecord;
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20060 drop core test output dependency in jmh-benchmarks (#21310).

**Test**
Test modified in StressTestLog.java covering the corrected behaviour.
