**Commit**: 98bb79e732 · **Ticket**: KAFKA-17981
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Jhen-Yung Hsu <jhenyunghsu@gmail.com>, TaiJuWu <tjwu1217@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
    @ClusterTest
    public void testAddConfigKeyValuesUsingCommand() throws Exception {
        try (Admin client = cluster.admin()) {
            NewTopic newTopic = new NewTopic("topic", 1, (short) 1);
            client.createTopics(Collections.singleton(newTopic)).all().get();
            cluster.waitForTopic("topic", 1);
            Stream<String> command = Stream.concat(quorumArgs(), Stream.of(
                    "--entity-type", "topics",
                    "--entity-name", "topic",
                    "--alter", "--add-config", "cleanup.policy=[delete,compact]"));
            String message = captureStandardStream(false, run(command));
            assertEquals("Completed updating config for topic topic.", message);
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17981 add integration test for configcommand to add config `key=[val1,val2]` (#17771).

**Test**
Test modified in ConfigCommandIntegrationTest.java covering the corrected behaviour.
