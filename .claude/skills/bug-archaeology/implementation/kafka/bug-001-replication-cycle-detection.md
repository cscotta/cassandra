**Commit**: dc5f816403 · **Ticket**: KAFKA-9914
**Subsystem**: connect/mirror — MirrorSourceConnector
**Category**: Logic error in condition

**Root cause**
The cycle-detection predicate in `shouldReplicateTopic` only checked whether the derived topic source matched the *target* cluster alias but never checked whether it matched the *source* cluster alias. With the default `DefaultReplicationPolicy`, a topic like `source.topic1` (where "source" is the source alias) would derive `topicSource = "source"`, which equals the source alias, not the target — so the cycle was missed and the topic was incorrectly replicated. With `IdentityReplicationPolicy`, the policy never adds a prefix, so checking against either cluster alias would be wrong; the old logic still (coincidentally) blocked legitimate topics.

**Before/After**
```java
// Before
} else if (source.equals(sourceAndTarget.target())) {
    return true;

// After
final boolean condition;
if (replicationPolicy instanceof IdentityReplicationPolicy) {
    condition = source.equals(sourceAndTarget.target());
} else {
    condition = source.equals(sourceAndTarget.source()) || source.equals(sourceAndTarget.target());
}
if (condition) {
    return true;
```

**How to spot**
Look for boolean conditions in cycle-detection code that only compare against one side of a bidirectional relationship. The tell is that the original code compared `source` only to `.target()`, ignoring the case where a topic had already been produced from the source cluster. Any test that only verifies target-prefixed topics but not source-prefixed topics is missing half the cycle space.

**Test**
`MirrorSourceConnectorTest#testNoCycles` — added `assertFalse(connector.shouldReplicateTopic("source.topic1"))` for default policy, and a separate block verifying `IdentityReplicationPolicy` allows all topic names (no prefix semantics, no false positives).
