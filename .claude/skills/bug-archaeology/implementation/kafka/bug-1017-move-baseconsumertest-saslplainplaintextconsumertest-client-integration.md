**Commit**: 5e601b2b26 · **Ticket**: KAFKA-19042
**Subsystem**: clients/clients-integration-tests
**Category**: Logic error in condition

**Root cause**
Use Java to rewrite BaseConsumerTest, SaslPlainPlaintextConsumerTest by
new test infra and move it to client-integration-tests module, the
BaseConsumerTest test is still used, thus we should not remove now. Reviewers: PoAn Yang <payang@apache.org>, TengYao Chi
<frankvicky@apache.org>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.consumer.OffsetCommitCallback;
import org.apache.kafka.clients.consumer.RetriableCommitFailedException;
import org.apache.kafka.common.ClusterResource;
import org.apache.kafka.common.ClusterResourceListener;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.internals.Topic;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import java.util.Collection;
import java.util.HashMap;
```

**How to spot**
When reviewing clients/clients-integration-tests code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19042 move baseconsumertest, saslplainplaintextconsumertest to client-integration-tests module (#19651).

**Test**
Test modified in ClientsTestUtils.java covering the corrected behaviour.
