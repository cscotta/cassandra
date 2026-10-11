**Commit**: 434fe7c26e · **Ticket**: KAFKA-18081
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    if (isKRaftTest()) {
      recreateBrokers(startup = true)
      assertTrue(faultHandler.firstException().getCause.isInstanceOf[ConfigException])
      // Normally the exception is thrown as part of the TearDown method of the parent class(es). We would like to not do this.
      faultHandler.setIgnore(true)
    } else {
      assertThrows(classOf[ConfigException], () => recreateBrokers(startup = true))
    }
import org.apache.kafka.server.config.ReplicationConfigs
  override protected def brokerPropertyOverrides(properties: Properties): Unit = {
    // legacy message formats are only supported with IBP < 3.0
    // KRaft mode is not supported for inter.broker.protocol.version = 2.8, The minimum version required is 3.0-IV1"

// After
    recreateBrokers(startup = true)
    assertTrue(faultHandler.firstException().getCause.isInstanceOf[ConfigException])
    // Normally the exception is thrown as part of the TearDown method of the parent class(es). We would like to not do this.
    faultHandler.setIgnore(true)
    // legacy message formats are supported for IBP version < 3.0 and KRaft runs on minimum version 3.0-IV1
    val timestampTopic2P0 = timestampOffsets.get(new TopicPartition(topic2, 0))
    assertEquals(40, timestampTopic2P0.offset)
    assertEquals(40, timestampTopic2P0.timestamp)
    assertEquals(Optional.of(0), timestampTopic2P0.leaderEpoch)

    val timestampTopic2P1 = timestampOffsets.get(new TopicPartition(topic2, 1))
    assertEquals(60, timestampTopic2P1.offset)
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18081 remove iskrafttest from the kraft-only tests (#17934).

**Test**
Test modified in RemoteTopicCrudTest.scala covering the corrected behaviour.
