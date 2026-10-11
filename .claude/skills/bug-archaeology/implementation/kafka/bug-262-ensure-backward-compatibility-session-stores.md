**Commit**: 02dca7adce · **Ticket**: KAFKA-20194
**Subsystem**: streams/src
**Category**: Incorrect filtering / wrong query result

**Root cause**
By default, the DSL should expose session state-stores as plain stores,
as  long as header format is not enabled; otherwise, it would be a
backward  incompatible change. This PR ensures that the builders are creating the correct state stores,
depending on the format, and we insert an adaptor to allow the DSL
Processors to work only against headers-store interface. Reviewers: Alieh Saeedi <asaeedi@confluent.io>, Bill Bejeck
<bill@confluent.io>

**Before/After**
```java
// Before
import org.apache.kafka.streams.processor.api.Processor;
            .process(() -> (Processor<Windowed<String>, Long, Object, Object>) record -> {
        produceSessionWindowData(producerConfig, withHeaders, t1, t2, t3, t4, t5, sessionGap);
            .process(() -> (Processor<Windowed<String>, String, Object, Object>) record -> {
                                           final long t4, final long t5,
                                           final long sessionGap) throws Exception {
            .process(() -> (Processor<Windowed<String>, Long, Object, Object>) record -> {

// After
            .process(() -> record -> {
        produceSessionWindowData(producerConfig, withHeaders, t1, t2, t3, t4, t5);
            .process(() -> record -> {
                                           final long t4, final long t5) throws Exception {
            .process(() -> record -> {
    @SuppressWarnings("resource")
    @SuppressWarnings("resource")
    @SuppressWarnings("resource")
```

**How to spot**
When reviewing streams/src code, look for incorrect filtering / wrong query result patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20194: ensure backward compatibility for session stores (#21934).

**Test**
Test modified in KStreamAggregationIntegrationTest.java covering the corrected behaviour.
