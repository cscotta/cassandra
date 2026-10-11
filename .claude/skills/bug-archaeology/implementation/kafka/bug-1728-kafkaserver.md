**Commit**: 64bbdb1a03 · **Ticket**: KAFKA-17616
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, Ismael Juma <ismael@juma.me.uk>, Ken Huang <s7133700@gmail.com>

**Before/After**
```java
// Before
import kafka.server.{KafkaConfig, KafkaRaftServer, KafkaServer, Server}
    if (config.requiresZookeeper) {
      new KafkaServer(
        config,
        Time.SYSTEM,
        threadNamePrefix = None
      )
    } else {
      new KafkaRaftServer(
        config,
        Time.SYSTEM,
      )

// After
import kafka.server.{KafkaConfig, KafkaRaftServer, Server}
    new KafkaRaftServer(
      config,
      Time.SYSTEM,
    )
          // KafkaBroker.startup() calls shutdown() in case of exceptions, so we invoke `exit` to set the status code
import org.apache.kafka.common.Node
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17616: remove kafkaserver (#18384).

**Test**
No test added.
