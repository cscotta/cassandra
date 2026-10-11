**Commit**: 24ea7e0b00 · **Ticket**: KAFKA-19666
**Subsystem**: streams/integration-tests
**Category**: State not cleaned up

**Root cause**
Clean up `PauseResumeIntegrationTest`

Reviewers: Lucas Brutschy <lucasbru@apache.org>

**Before/After**
```java
// Before
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
    private Properties props(final boolean stateUpdaterEnabled) {
        properties.put(StreamsConfig.InternalConfig.STATE_UPDATER_ENABLED, stateUpdaterEnabled);
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldPauseAndResumeKafkaStreams(final boolean stateUpdaterEnabled) throws Exception {
        kafkaStreams = buildKafkaStreams(OUTPUT_STREAM_1, stateUpdaterEnabled);
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldAllowForTopologiesToStartPaused(final boolean stateUpdaterEnabled) throws Exception {
        kafkaStreams = buildKafkaStreams(OUTPUT_STREAM_1, stateUpdaterEnabled);

// After
import org.junit.jupiter.api.Test;
    private Properties props() {
    @Test
    public void shouldPauseAndResumeKafkaStreams() throws Exception {
        kafkaStreams = buildKafkaStreams(OUTPUT_STREAM_1);
    @Test
    public void shouldAllowForTopologiesToStartPaused() throws Exception {
        kafkaStreams = buildKafkaStreams(OUTPUT_STREAM_1);
    @Test
    public void shouldPauseAndResumeKafkaStreamsWithNamedTopologies() throws Exception {
        streamsNamedTopologyWrapper = new KafkaStreamsNamedTopologyWrapper(props());
    @Test
```

**How to spot**
When reviewing streams/integration-tests code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19666: remove old restoration codepath from pauseresumeintegrationtest [2/n] (#20463).

**Test**
Test modified in PauseResumeIntegrationTest.java covering the corrected behaviour.
