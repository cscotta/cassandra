**Commit**: 04b4a8f571 · **Ticket**: KAFKA-19705
**Subsystem**: streams/integration-tests
**Category**: Logic error in condition

**Root cause**
Update IQv2 Integration tests for streams group protocol

Reviewers: Lucas Brutschy <lbrutschy@confluent.io>

**Before/After**
```java
// Before
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
    @BeforeEach
    public void beforeTest(final TestInfo testInfo) {

        kafkaStreams.close(Duration.ofSeconds(60));
        kafkaStreams.cleanUp();
    @Test
    public void shouldFailUnknownStore() {
    @Test
    public void shouldFailNotStarted() {
    @Test

// After
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
    private String groupProtocol;
    private void setup(final String groupProtocol, final TestInfo testInfo) {
        this.groupProtocol = groupProtocol;
        if (kafkaStreams != null) {
            kafkaStreams.close(Duration.ofSeconds(60));
            kafkaStreams.cleanUp();
        }
    @ParameterizedTest(name = "{1}")
```

**How to spot**
When reviewing streams/integration-tests code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19705: enable streams rebalance protocol in iqv2 integration test (#20541).

**Test**
Test modified in IQv2IntegrationTest.java covering the corrected behaviour.
