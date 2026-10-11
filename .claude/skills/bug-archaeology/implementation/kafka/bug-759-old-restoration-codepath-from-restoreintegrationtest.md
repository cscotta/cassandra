**Commit**: dd824a2e74 · **Ticket**: KAFKA-19666
**Subsystem**: streams/integration-tests
**Category**: State not cleaned up

**Root cause**
Clean up `RestoreIntegrationTest.java`

Reviewers: Lucas Brutschy <lbrutschy@confluent.io>

**Before/After**
```java
// Before
import org.apache.kafka.streams.StreamsConfig.InternalConfig;
import org.junit.jupiter.params.provider.CsvSource;
    private Properties props(final boolean stateUpdaterEnabled) {
        return props(mkObjectProperties(mkMap(mkEntry(InternalConfig.STATE_UPDATER_ENABLED, stateUpdaterEnabled))));
    @CsvSource({
        "true,true",
        "true,false",
        "false,true",
        "false,false"
    })
    public void shouldRestoreStateFromSourceTopicForReadOnlyStore(final boolean stateUpdaterEnabled, final boolean useNewProtocol) throws Exception {
        final Properties props = props(stateUpdaterEnabled);

// After
    private Properties props() {
        return props(mkObjectProperties(mkMap()));
    @ValueSource(booleans = {true, false})
    public void shouldRestoreStateFromSourceTopicForReadOnlyStore(final boolean useNewProtocol) throws Exception {
        final Properties props = props();
    @ValueSource(booleans = {true, false})
    public void shouldRestoreStateFromSourceTopicForGlobalTable(final boolean useNewProtocol) throws Exception {
        final Properties props = props();
    @ValueSource(booleans = {true, false})
    public void shouldRestoreStateFromChangelogTopic(final boolean useNewProtocol) throws Exception {
        final Properties props = props();
    @ValueSource(booleans = {true, false})
```

**How to spot**
When reviewing streams/integration-tests code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19666: remove old restoration codepath from restoreintegrationtest [4/n] (#20498).

**Test**
Test modified in RestoreIntegrationTest.java covering the corrected behaviour.
