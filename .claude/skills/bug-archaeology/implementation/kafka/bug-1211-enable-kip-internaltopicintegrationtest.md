**Commit**: c65a161cd8 · **Ticket**: KAFKA-19118
**Subsystem**: streams/integration-tests
**Category**: Logic error in condition

**Root cause**
KIP-1071 creates internal topics broker-side, so this test checks
whether, when KIP-1071 is enabled, basically the same topics are
created. It also adds a little helper method in `EmbeddedKafkaCluster`, so that
fewer code changes are required to enable KIP-1071. We use that helper
in the already enabled SmokeTestDriverIntegrationTest and revert some of
the changes there (making the cluster `final` again). Reviewers: Bill Bejeck <bbejeck@apache.org>, PoAn Yang
 <payang@apache.org>

**Before/After**
```java
// Before
import org.junit.jupiter.api.Test;
    public static final EmbeddedKafkaCluster CLUSTER = new EmbeddedKafkaCluster(1);
    @Test
    public void shouldGetToRunningWithWindowedTableInFKJ() throws Exception {
        final String appID = APP_ID + "-windowed-FKJ";
        streamsProp.put(StreamsConfig.APPLICATION_ID_CONFIG, appID);
    @Test
    public void shouldCompactTopicsForKeyValueStoreChangelogs() throws Exception {
        final String appID = APP_ID + "-compact";
        streamsProp.put(StreamsConfig.APPLICATION_ID_CONFIG, appID);
    @Test
    public void shouldCompactAndDeleteTopicsForWindowStoreChangelogs() throws Exception {

// After
import org.apache.kafka.streams.GroupProtocol;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
    public static final EmbeddedKafkaCluster CLUSTER = EmbeddedKafkaCluster.withStreamsRebalanceProtocol(1);
    private void configureStreams(final boolean streamsProtocolEnabled, final String appID) {
        streamsProp.put(StreamsConfig.APPLICATION_ID_CONFIG, appID);
        if (streamsProtocolEnabled) {
            streamsProp.put(StreamsConfig.GROUP_PROTOCOL_CONFIG, GroupProtocol.STREAMS.name().toLowerCase(Locale.getDefault()));
        }
    }

    @ParameterizedTest
```

**How to spot**
When reviewing streams/integration-tests code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19118: enable kip-1071 in internaltopicintegrationtest (#19425).

**Test**
Test modified in InternalTopicIntegrationTest.java covering the corrected behaviour.
