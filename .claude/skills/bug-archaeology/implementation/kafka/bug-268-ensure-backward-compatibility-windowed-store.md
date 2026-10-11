**Commit**: 1dacfd29a3 · **Ticket**: KAFKA-20194
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
By default, the DSL should expose it's state-stores as ts-stores, as
long as header format is not enabled; otherwise, it would be a backward
incompatible change. This PR ensures that the builders are creating the correct state stores,
depending on the format, and we insert an adaptor to allow the DSL
Processors to work only against headers-store interface. Reviewers: Bill Bejeck <bill@confluent.io>, Alieh Saeedi
<asaeedi@confluent.io>

**Before/After**
```java
// Before
            .transformValues(() -> new ValueTransformerWithKey<String, String, String>() {

// After
import org.apache.kafka.streams.kstream.EmitStrategy;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.TimestampedWindowStore;
import org.apache.kafka.streams.state.WindowStore;
import java.time.Duration;
            .transformValues(() -> new ValueTransformerWithKey<>() {
    private void verifyWindow(final KTable<Windowed<String>, String> table) {
        verifyWindow(table, false);
    }

    private void verifyWindow(final KTable<Windowed<String>, String> table, final boolean requiresFlush) {
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20194: ensure backward compatibility for windowed store (#21922).

**Test**
Test modified in PapiDslIntegrationTest.java covering the corrected behaviour.
