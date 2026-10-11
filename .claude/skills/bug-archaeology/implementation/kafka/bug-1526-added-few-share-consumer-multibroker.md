**Commit**: b92cd9d236 · **Ticket**: KAFKA-18632
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
import kafka.server.KafkaBroker;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.consumer.KafkaShareConsumer;
import org.apache.kafka.common.Uuid;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.utils.Utils;
import org.apache.kafka.coordinator.group.modern.share.ShareGroupConfig;
import org.apache.kafka.server.share.SharePartitionKey;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18632: added few share consumer multibroker tests. (#18679).

**Test**
Test modified in ShareConsumerTest.java covering the corrected behaviour.
