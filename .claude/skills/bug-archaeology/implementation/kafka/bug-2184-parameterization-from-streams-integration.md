**Commit**: 6d39031958 · **Ticket**: KAFKA-17558
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Integration tests should run with either at-least-once or exactly-once. There is no need to run them twice. This PR removes the corresponding test parameters and picks either one of both. Reviewers: Matthias J.

**Before/After**
```java
// Before
import org.junit.jupiter.params.provider.CsvSource;
    @ParameterizedTest
    @ValueSource(strings = {StreamsConfig.AT_LEAST_ONCE, StreamsConfig.EXACTLY_ONCE_V2})
    public void shouldBeAbleToRunWithEosEnabled(final String eosConfig) throws Exception {
        runSimpleCopyTest(1, SINGLE_PARTITION_INPUT_TOPIC, null, SINGLE_PARTITION_OUTPUT_TOPIC, false, eosConfig);
    @ParameterizedTest
    @ValueSource(strings = {StreamsConfig.AT_LEAST_ONCE, StreamsConfig.EXACTLY_ONCE_V2})
    public void shouldCommitCorrectOffsetIfInputTopicIsTransactional(final String eosConfig) throws Exception {
        runSimpleCopyTest(1, SINGLE_PARTITION_INPUT_TOPIC, null, SINGLE_PARTITION_OUTPUT_TOPIC, true, eosConfig);
    @ParameterizedTest
    @ValueSource(strings = {StreamsConfig.AT_LEAST_ONCE, StreamsConfig.EXACTLY_ONCE_V2})
    public void shouldBeAbleToRestartAfterClose(final String eosConfig) throws Exception {

// After
import org.junit.jupiter.api.Test;
    @Test
    public void shouldBeAbleToRunWithEosEnabled() throws Exception {
        runSimpleCopyTest(1, SINGLE_PARTITION_INPUT_TOPIC, null, SINGLE_PARTITION_OUTPUT_TOPIC, false);
    @Test
    public void shouldCommitCorrectOffsetIfInputTopicIsTransactional() throws Exception {
        runSimpleCopyTest(1, SINGLE_PARTITION_INPUT_TOPIC, null, SINGLE_PARTITION_OUTPUT_TOPIC, true);
    @Test
    public void shouldBeAbleToRestartAfterClose() throws Exception {
        runSimpleCopyTest(2, SINGLE_PARTITION_INPUT_TOPIC, null, SINGLE_PARTITION_OUTPUT_TOPIC, false);
    @Test
    public void shouldBeAbleToCommitToMultiplePartitions() throws Exception {
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17558: remove parameterization from streams integration tests (#17315).

**Test**
Test modified in EosIntegrationTest.java covering the corrected behaviour.
