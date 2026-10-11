**Commit**: 6c651fbfa0 · **Ticket**: KAFKA-20329
**Subsystem**: streams/integration-tests
**Category**: Logic error in condition

**Root cause**
This PR parametrizes 10 integration test classes (49 test methods total)
to ensure comprehensive testing coverage for both default and
header-based DSL store    formats. Each test now runs twice: once with
the default store format and once with dsl.store.format=headers. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, Matthias J. Sax
<matthias@confluent.io>, TengYao Chi <frankvicky@apache.org>

**Before/After**
```java
// Before
import org.junit.jupiter.api.Test;
    @Test
    public void shouldKStreamGlobalKTableLeftJoin() throws Exception {
        startStreams();
    @Test
    public void shouldKStreamGlobalKTableJoin() throws Exception {
        startStreams();
    @Test
    public void shouldRestoreTransactionalMessages() throws Exception {
        startStreams();
    @Test
    public void shouldNotRestoreAbortedMessages() throws Exception {

// After
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void shouldKStreamGlobalKTableLeftJoin(final boolean withHeaders) throws Exception {
        startStreams(withHeaders);
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void shouldKStreamGlobalKTableJoin(final boolean withHeaders) throws Exception {
        startStreams(withHeaders);
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
```

**How to spot**
When reviewing streams/integration-tests code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20329: test headers dsl.store.format further (3/n) (#21820).

**Test**
Test modified in GlobalKTableEOSIntegrationTest.java covering the corrected behaviour.
