**Commit**: ea5b5fec32 · **Ticket**: KAFKA-19432
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
* Log error message if `broker.heartbeat.interval.ms * 2` is large than
`broker.session.timeout.ms`. * Add test case

`testLogBrokerHeartbeatIntervalMsShouldBeLowerThanHalfOfBrokerSessionTimeoutMs`. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
    if (brokerHeartbeatIntervalMs * 2 > brokerSessionTimeoutMs) {
      error(s"${KRaftConfigs.BROKER_HEARTBEAT_INTERVAL_MS_CONFIG} ($brokerHeartbeatIntervalMs ms) must be less than or equal to half of the ${KRaftConfigs.BROKER_SESSION_TIMEOUT_MS_CONFIG} ($brokerSessionTimeoutMs ms). " +
        s"The ${KRaftConfigs.BROKER_SESSION_TIMEOUT_MS_CONFIG} is configured on controller. The ${KRaftConfigs.BROKER_HEARTBEAT_INTERVAL_MS_CONFIG} is configured on broker. " +
        s"If a broker doesn't send heartbeat request within ${KRaftConfigs.BROKER_SESSION_TIMEOUT_MS_CONFIG}, it loses broker lease. " +
        s"Please increase ${KRaftConfigs.BROKER_SESSION_TIMEOUT_MS_CONFIG} or decrease ${KRaftConfigs.BROKER_HEARTBEAT_INTERVAL_MS_CONFIG}.")
    }

import org.apache.kafka.common.utils.LogCaptureAppender
import org.apache.logging.log4j.Level
import scala.util.Using

  @Test
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19432 add an error log message if broker.heartbeat.interval.ms is too large (#20046).

**Test**
Test modified in KafkaConfigTest.scala covering the corrected behaviour.
