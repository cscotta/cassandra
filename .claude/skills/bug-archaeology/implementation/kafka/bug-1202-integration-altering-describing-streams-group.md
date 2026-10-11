**Commit**: 3e0276ebb7 · **Ticket**: KAFKA-19127
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
This PR introduces integration tests for verifying
- altering configs
- `describe` output

for a streams group. The configs have been defined in `KIP-1071`. Reviewers: Lucas Brutschy <lbrutschy@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
    @ClusterTest
    public void testDescribeStreamsGroupConfigs() {
        Stream<String> command = Stream.concat(quorumArgs(), Stream.of(
            "--entity-type", "groups",
            "--entity-name", "group",
            "--describe", "--all"));
        String message = captureStandardOut(run(command));

        assertTrue(message.contains("streams.heartbeat.interval.ms=5000 sensitive=false synonyms={DEFAULT_CONFIG:streams.heartbeat.interval.ms=5000}"));
        assertTrue(message.contains("streams.num.standby.replicas=0 sensitive=false synonyms={DEFAULT_CONFIG:streams.num.standby.replicas=0}"));
        assertTrue(message.contains("streams.session.timeout.ms=45000 sensitive=false synonyms={DEFAULT_CONFIG:streams.session.timeout.ms=45000}"));
    }
```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19127: integration test for altering and describing streams group configs (#19436).

**Test**
Test modified in ConfigCommandIntegrationTest.java covering the corrected behaviour.
