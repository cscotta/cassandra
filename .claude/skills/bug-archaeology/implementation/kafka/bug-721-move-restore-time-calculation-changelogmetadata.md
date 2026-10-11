**Commit**: 28e7803037 · **Ticket**: KAFKA-19744
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
- Move restore time calculation to ChangelogMetadata. - Introduced a new interface to propagate the calculated value to the
stores to avoid modifications in the public interface. Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
}

// After
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldRecordRestoreMetrics(final boolean useNewProtocol) throws Exception {
        final AtomicInteger numReceived = new AtomicInteger(0);
        final StreamsBuilder builder = new StreamsBuilder();

        final Properties props = props();

        if (useNewProtocol) {
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19744: move restore time calculation to changelogmetadata (#20613).

**Test**
Test modified in RestoreIntegrationTest.java covering the corrected behaviour.
