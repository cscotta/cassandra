**Commit**: 11f932d083 · **Ticket**: KAFKA-20158
**Subsystem**: streams/src
**Category**: Incorrect filtering / wrong query result

**Root cause**
Add headers-enabled variants to SessionWindowedKStreamImplTest and
SessionWindowedCogroupedKStreamImplTest to verify session windowed
aggregations work correctly with the DSL_STORE_FORMAT_HEADERS config. Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
    @Test
    public void sessionWindowAggregateTest() {
    @Test
    public void sessionWindowAggregate2Test() {
    @Test
    public void sessionWindowAggregateTest2StreamsTest() {
    @Test
    public void sessionWindowMixAggregatorsTest() {
    @Test
    public void sessionWindowMixAggregatorsManyWindowsTest() {

// After
import org.apache.kafka.streams.StreamsConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
    private void enableHeaders() {
        props.put(StreamsConfig.DSL_STORE_FORMAT_CONFIG, StreamsConfig.DSL_STORE_FORMAT_HEADERS);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void sessionWindowAggregateTest(final boolean withHeaders) {
        if (withHeaders) {
            enableHeaders();
```

**How to spot**
When reviewing streams/src code, look for incorrect filtering / wrong query result patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20158: parameterize session store unit tests to run with headers-aware store format   (#21749).

**Test**
Test modified in SessionWindowedCogroupedKStreamImplTest.java covering the corrected behaviour.
