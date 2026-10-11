**Commit**: 05999c70c1 · **Ticket**: KAFKA-19746
**Subsystem**: tools/src/test/java/org/apache/kafka/tools/ (ConfigCommandTest, LeaderElectionCommandTest, ResetConsumerGroupOffsetTest, StreamsGroupCommandTest)
**Category**: Wrong constant / default value

**Root cause**: Multiple `assertEquals` calls in the tools test module had their arguments in the wrong order: actual value was passed as the first argument (expected position) and expected value as the second. While JUnit 5 does not enforce ordering at runtime, reversed arguments produce misleading failure messages (expected/actual labels are swapped) and violate the JUnit convention of `assertEquals(expected, actual)`.

**Before/After**:
```java
// BEFORE – actual in "expected" slot
assertEquals(createOpts.entityTypes().toSeq(), seq(expectedTypes));
assertEquals(res.type(), ConfigResource.Type.TOPIC);
assertEquals(offsetMetadata.offset(), expectedOffsets.get(tp));

// AFTER – expected first
assertEquals(seq(expectedTypes), createOpts.entityTypes().toSeq());
assertEquals(ConfigResource.Type.TOPIC, res.type());
assertEquals(expectedOffsets.get(tp), offsetMetadata.offset());
```

**How to spot**: Search for `assertEquals(<variable_or_call>, <literal_or_expected>)` patterns where the first argument is not a constant, expected value, or clearly named `expected*` variable. IDE inspections (e.g., IntelliJ's "Arguments to assertEquals are swapped") catch these automatically.

**Test**: Pure test correctness fix. No production code changed; no new scenario.
