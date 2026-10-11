**Commit**: 433f06a166 · **Ticket**: KAFKA-20329
**Subsystem**: streams/integration-tests
**Category**: State not cleaned up

**Root cause**
This PR adds withHeaders parameterization to Kafka Streams integration
Tests to verify  that stream operations work correctly with both DSL
store format configurations (regular  and headers-based format). Changes:    - Added `withHeaders` boolean parameter to test methods    -
Tests now run with both `withHeaders=false` and `withHeaders=true`    -
When `withHeaders=true`, tests configure
`StreamsConfig.DSL_STORE_FORMAT_CONFIG` to
`StreamsConfig.DSL_STORE_FORMAT_HEADERS`    - For tests that produce
data in @BeforeAll, moved data production to @BeforeEach to ensure fresh
data for each parameterized test run    - Added topic cleanup in
@AfterEach for proper isolation between test runs

  This e...

**Before/After**
```java
// Before
import org.junit.jupiter.api.Test;
    @Test
    public void shouldRecoverBufferAfterShutdown(final TestInfo testInfo) {
        KafkaStreams driver = getStartedStreams(streamsConfig, builder, true);
            driver = getStartedStreams(streamsConfig, builder, false);
import org.junit.jupiter.api.Test;
    @Test
    public void providingAJoinStoreNameShouldNotMakeTheJoinResultQueryable() throws InterruptedException {

// After
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void shouldRecoverBufferAfterShutdown(final boolean withHeaders, final TestInfo testInfo) {
        KafkaStreams driver = startStream(streamsConfig, builder, true, withHeaders);
            driver = startStream(streamsConfig, builder, false, withHeaders);

    private KafkaStreams startStream(
        final Properties streamsConfig,
        final StreamsBuilder builder,
        final boolean clean,
```

**How to spot**
When reviewing streams/integration-tests code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20329: test `headers` dsl.store.format further (1/n) (#21806).

**Test**
Test modified in JoinGracePeriodDurabilityIntegrationTest.java covering the corrected behaviour.
