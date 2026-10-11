**Commit**: b4dd434f5e · **Ticket**: KAFKA-17692
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import kafka.server.KafkaServer;
import kafka.server.MetadataCache;

import org.apache.kafka.common.message.UpdateMetadataRequestData.UpdateMetadataPartitionState;
import org.apache.kafka.common.requests.FetchRequest;
import org.apache.kafka.streams.processor.internals.assignment.AssignorConfiguration.AssignmentListener;
import java.util.concurrent.atomic.AtomicInteger;
import scala.Option;

import static org.apache.kafka.test.TestUtils.waitForCondition;
    /**
     * Wait until enough data (value records) has been consumed.

// After
// (no additions)
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17692 remove kafkaserver references from integrationtestutils (#17372).

**Test**
Test modified in IntegrationTestUtils.java covering the corrected behaviour.
