**Commit**: b803e8be97 · **Ticket**: KAFKA-20329
**Subsystem**: streams/integration-tests
**Category**: Logic error in condition

**Root cause**
This PR adds withHeaders parameterization to Kafka Streams integration
Store & Query tests to verify    that Interactive Query operations work
correctly with both DSL store format configurations (regular    and
headers-based format). Due to current IQv2 limitations with headers
stores, `headers` store types are not added to
`IQv2StoreIntegrationTest.StoresToTest`. Changes:    - Converted @Test methods to @ParameterizedTest with
withHeaders parameter    - When withHeaders=true, tests configure
StreamsConfig.DSL_STORE_FORMAT_CONFIG to
StreamsConfig.DSL_STORE_FORMAT_HEADERS

  This ensures comprehensive test coverage for Interactive Queries
across different    store formats.

**Before/After**
```java
// Before
                            values.add(Arguments.of(cacheEnabled, logEnabled, toTest.name(), kind, groupProtocol));
    public void setup(final boolean cache, final boolean log, final StoresToTest storeToTest, final String kind, final String groupProtocol) {
            groupProtocol
    public void verifyStore(final boolean cache, final boolean log, final StoresToTest storeToTest, final String kind, final String groupProtocol) {
        setup(cache, log, storeToTest, kind, groupProtocol);
                                                   final String supplier, final String kind, final String groupProtocol) {
                + "-" + kind + "-" + groupProtocol + "-" + RANDOM.nextInt();
import org.junit.jupiter.api.Test;

// After
                            for (final boolean withHeaders : Arrays.asList(true, false)) {
                                values.add(Arguments.of(cacheEnabled, logEnabled, toTest.name(), kind, groupProtocol, withHeaders));
                            }
    public void setup(final boolean cache, final boolean log, final StoresToTest storeToTest, final String kind, final String groupProtocol, final boolean withHeaders) {
            groupProtocol,
            withHeaders
    public void verifyStore(final boolean cache, final boolean log, final StoresToTest storeToTest, final String kind, final String groupProtocol, final boolean withHeaders) {
        setup(cache, log, storeToTest, kind, groupProtocol, withHeaders);
                                                   final String supplier, final String kind, final String groupProtocol, final boolean withHeaders) {
                + "-" + kind + "-" + groupProtocol + "-" + withHeaders + "-" + RANDOM.nextInt();
        if (withHeaders) {
            config.put(StreamsConfig.DSL_STORE_FORMAT_CONFIG, StreamsConfig.DSL_STORE_FORMAT_HEADERS);
```

**How to spot**
When reviewing streams/integration-tests code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20329: test headers dsl.store.format further (2/n) (#21812).

**Test**
Test modified in IQv2StoreIntegrationTest.java covering the corrected behaviour.
