**Commit**: e2b5eb6131 · **Ticket**: KAFKA-20329
**Subsystem**: streams/src
**Category**: Incorrect filtering / wrong query result

**Root cause**
Parametrized 28 DSL and processor test classes to validate functionality
with both `headers` and `default` store configurations. This ensures
backward compatibility and correctness as Kafka Streams transitions to
supporting record headers in state stores. Modified Tests:    - Materializer Tests    - Windowed KStream Tests
- Aggregation Tests    -  Cogroup Tests    -   Join Tests    -   KTable
Tests

Reviewers: Matthias J. Sax <matthias@confluent.io>, TengYao Chi
<frankvicky@apache.org>

**Before/After**
```java
// Before
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
    @BeforeEach
    public void setup() {
    @Test
    public void shouldThrowNPEInCogroupIfKGroupedStreamIsNull() {
    @Test
    public void shouldNotHaveNullAggregatorOnCogroup() {
    @Test
    public void shouldNotHaveNullInitializerOnAggregate() {
    @Test
    public void shouldNotHaveNullInitializerOnAggregateWitNamed() {

// After
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
    private void setup(final boolean withHeaders) {
        if (withHeaders) {
            props.put(StreamsConfig.DSL_STORE_FORMAT_CONFIG, StreamsConfig.DSL_STORE_FORMAT_HEADERS);
        }
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void shouldThrowNPEInCogroupIfKGroupedStreamIsNull(final boolean withHeaders) {
        setup(withHeaders);
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
```

**How to spot**
When reviewing streams/src code, look for incorrect filtering / wrong query result patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20329: test headers dsl.store.format further (4/n) (#21827).

**Test**
Test modified in CogroupedKStreamImplTest.java covering the corrected behaviour.
