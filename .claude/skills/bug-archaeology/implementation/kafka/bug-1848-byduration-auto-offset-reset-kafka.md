**Commit**: 6cdb8c352a · **Ticket**: KAFKA-18015
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Part of KIP-1106. Adds support for "by_duration" and "none" reset strategy
to the Kafka Streams runtime. Reviewers: Bill Bejeck <bill@confluent.io>, Anna Sophie Blee-Goldman <ableegoldman@apache.org>

**Before/After**
```java
// Before
import org.apache.kafka.streams.Topology;
    private static final String STRING_SERDE_CLASSNAME = Serdes.String().getClass().getName();
    public void setUp() throws IOException {

// After
    public Optional<Duration> duration() {
        return duration;
    }

    <Match>
        <Class name="org.apache.kafka.streams.processor.internals.TopologyMetadata"/>
        <Method name="offsetResetStrategy"/>
        <Bug pattern="NP_OPTIONAL_RETURN_NULL"/>
    </Match>

import org.apache.kafka.streams.AutoOffsetReset;
import java.time.Duration;
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18015: add byduration auto.offset.reset to kafka streams (#18115).

**Test**
Test modified in FineGrainedAutoResetIntegrationTest.java covering the corrected behaviour.
