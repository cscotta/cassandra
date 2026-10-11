**Commit**: 1426e8e920 · **Ticket**: KAFKA-16764
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Propagate metadata error from background thread to application thread. So, this fix ensures that metadata errors are thrown to the user on consumer.poll()

Reviewers: Lucas Brutschy <lbrutschy@confluent.io>, Philip Nee <pnee@confluent.io>, Lianet Magrans <lianetmr@gmail.com>

**Before/After**
```java
// Before
                    clientTelemetryReporter.map(ClientTelemetryReporter::telemetrySender).orElse(null));
            client
            final KafkaClient client) {
                                                           final ClientTelemetrySender clientTelemetrySender) {

// After
                    clientTelemetryReporter.map(ClientTelemetryReporter::telemetrySender).orElse(null),
                    backgroundEventHandler);
            client,
            metadata,
            backgroundEventHandler
import org.apache.kafka.clients.Metadata;
import org.apache.kafka.clients.consumer.internals.events.BackgroundEventHandler;
import org.apache.kafka.clients.consumer.internals.events.ErrorEvent;
    private final BackgroundEventHandler backgroundEventHandler;
    private final Metadata metadata;
            final KafkaClient client,
            final Metadata metadata,
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16764: new consumer should throw invalidtopicexception on poll when invalid topic in metadata. (#16043).

**Test**
Test modified in ConsumerTestBuilder.java covering the corrected behaviour.
