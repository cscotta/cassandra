**Commit**: 54fe0f0135 · **Ticket**: KAFKA-16368
**Subsystem**: docs/upgrade.html
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Divij Vaidya <diviv@amazon.com>

**Before/After**
```java
// Before
        @ClusterConfigProperty(key = "log.segment.bytes", value = "100"),
      props.put(ServerLogConfigs.LOG_SEGMENT_BYTES_CONFIG, "2000") // low value to test log rolling on config update
    props.put(ServerLogConfigs.LOG_SEGMENT_BYTES_CONFIG, "4000")
    reconfigureServers(props, perBrokerConfig = false, (ServerLogConfigs.LOG_SEGMENT_BYTES_CONFIG, "4000"))
    TestUtils.waitUntilTrue(() => log.config.segmentSize == 4000, "Existing topic config using defaults not updated")

// After
import scala.Option;
            // force roll the segment so that cleaner can work on it
            server.logManager().getLog(topicPartition, false).get().roll(Option.empty());
      props.put(ServerLogConfigs.LOG_SEGMENT_BYTES_CONFIG, "1048576") // low value to test log rolling on config update
    props.put(ServerLogConfigs.LOG_SEGMENT_BYTES_CONFIG, "1048576")
    reconfigureServers(props, perBrokerConfig = false, (ServerLogConfigs.LOG_SEGMENT_BYTES_CONFIG, "1048576"))
        TestUtils.waitUntilTrue(() => server.config.originals.get(k) != null, "Configs not present")
    TestUtils.waitUntilTrue(() => log.config.segmentSize == 1048576, "Existing topic config using defaults not updated")
```

**How to spot**
When reviewing docs/upgrade.html code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16368: add a new constraint for segment.bytes to min 1mb for kip-1030 (#18140).

**Test**
Test modified in DeleteTopicTest.java covering the corrected behaviour.
