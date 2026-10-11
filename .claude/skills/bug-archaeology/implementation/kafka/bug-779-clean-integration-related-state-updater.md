**Commit**: d68c41d2f3 · **Ticket**: KAFKA-19666
**Subsystem**: streams/integration-tests
**Category**: State not cleaned up

**Root cause**
Clean up `KafkaStreamsTelemetryIntegrationTest.java`

Reviewers: Lucas Brutschy <lucasbru@apache.org>

**Before/After**
```java
// Before
        streamsApplicationProperties = props(true, groupProtocol);
        streamsApplicationProperties  = props(true, groupProtocol);
    @MethodSource("singleAndMultiTaskParameters")
    public void shouldPassMetrics(final String topologyType, final boolean stateUpdaterEnabled, final String groupProtocol) throws Exception {
        streamsApplicationProperties = props(stateUpdaterEnabled, groupProtocol);
    @ParameterizedTest
    @MethodSource("multiTaskParameters")
    public void shouldPassCorrectMetricsDynamicInstances(final boolean stateUpdaterEnabled, final String groupProtocol) throws Exception {
        streamsApplicationProperties = props(stateUpdaterEnabled, groupProtocol);
        streamsSecondApplicationProperties = props(stateUpdaterEnabled, groupProtocol);
        streamsApplicationProperties = props(true, groupProtocol);
    private static Stream<Arguments> singleAndMultiTaskParameters() {

// After
import org.junit.jupiter.api.Test;
        streamsApplicationProperties = props(groupProtocol);
        streamsApplicationProperties  = props(groupProtocol);
    @MethodSource("topologyComplexityAndRebalanceProtocol")
    public void shouldPassMetrics(final String topologyType, final String groupProtocol) throws Exception {
        streamsApplicationProperties = props(groupProtocol);
    @Test
    public void shouldPassCorrectMetricsDynamicInstances() throws Exception {
        streamsApplicationProperties = props("classic");
        streamsSecondApplicationProperties = props("classic");
        streamsApplicationProperties = props(groupProtocol);
    private static Stream<Arguments> topologyComplexityAndRebalanceProtocol() {
```

**How to spot**
When reviewing streams/integration-tests code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19666: clean up integration tests related to state-updater (#20462).

**Test**
Test modified in KafkaStreamsTelemetryIntegrationTest.java covering the corrected behaviour.
