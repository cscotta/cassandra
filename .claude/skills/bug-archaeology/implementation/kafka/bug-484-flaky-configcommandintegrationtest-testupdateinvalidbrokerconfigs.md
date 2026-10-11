**Commit**: 5400a80de3 · **Ticket**: KAFKA-20059
**Subsystem**: tools/src
**Category**: State not cleaned up

**Root cause**
### Description
This PR fixes a flaky failure in
`ConfigCommandIntegrationTest.testUpdateInvalidBrokerConfigs`. The test updates an unknown broker dynamic config `(invalid=2)` and
immediately runs `kafka-configs --describe` to assert that the unknown
config is treated as sensitive `(sensitive=true)` and returned as
`invalid=null`. In KRaft, the alter request can return before the
change is fully visible to `--describe` because `dynamic config` updates
are applied asynchronously and published via `DynamicConfigPublisher` as
metadata updates are processed. As a result, the first describe may read stale state and miss the
expected fields, causing intermittent failures.

**Before/After**
```java
// Before
    public void testUpdateInvalidBrokerConfigs() {
    private void updateAndCheckInvalidBrokerConfig(Optional<String> brokerIdOrDefault) {
            Stream<String> describeCommand = Stream.concat(
                    Stream.concat(
                            Stream.of("--bootstrap-server", cluster.bootstrapServers()),
                            Stream.of(entityOp(brokerIdOrDefault).toArray(new String[0]))),
                    Stream.of("--entity-type", "brokers", "--describe"));
            String describeResult = captureStandardOut(run(describeCommand));

            // We will treat unknown config as sensitive
            assertTrue(describeResult.contains("sensitive=true"), describeResult);
            // Sensitive config will not return

// After
    public void testUpdateInvalidBrokerConfigs() throws InterruptedException {
    private void updateAndCheckInvalidBrokerConfig(Optional<String> brokerIdOrDefault) throws InterruptedException {
            AtomicReference<String> last = new AtomicReference<>("");
            TestUtils.waitForCondition(() -> {
                Stream<String> describeCommand = Stream.concat(
                        Stream.concat(
                                Stream.of("--bootstrap-server", cluster.bootstrapServers()),
                                Stream.of(entityOp(brokerIdOrDefault).toArray(new String[0]))),
                        Stream.of("--entity-type", "brokers", "--describe")
                );
                String describeResult = captureStandardOut(run(describeCommand));
                last.set(describeResult);
```

**How to spot**
When reviewing tools/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20059: fix flaky configcommandintegrationtest testupdateinvalidbrokerconfigs (#21280).

**Test**
Test modified in ConfigCommandIntegrationTest.java covering the corrected behaviour.
