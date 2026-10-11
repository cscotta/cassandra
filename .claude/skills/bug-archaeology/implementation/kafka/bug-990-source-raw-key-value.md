**Commit**: 3edb406f98 · **Ticket**: KAFKA-16505
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
This PR is part of the KIP-1034. It brings the support for the source raw key and the source raw
value in the `ErrorHandlerContext`. Required by the routing to DLQ implemented
by https://github.com/apache/kafka/pull/17942. Reviewers: Bruno Cadonna <cadonna@apache.org>

Co-authored-by: Damien Gasparina <d.gasparina@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.JoinWindows;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.StreamJoined;
import org.apache.kafka.streams.state.KeyValueStore;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16505: add source raw key and value (#18739).

**Test**
Test modified in ProcessingExceptionHandlerIntegrationTest.java covering the corrected behaviour.
