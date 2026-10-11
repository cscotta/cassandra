**Commit**: 97c8c6b595 · **Ticket**: KAFKA-19733
**Subsystem**: clients/src/test/java/org/apache/kafka/clients/ (multiple test files)
**Category**: Logic error in condition

**Root cause**
Numerous `assertEquals` calls in the clients test module had the actual and expected arguments reversed — `assertEquals(actual, expected)` instead of the JUnit 5 convention `assertEquals(expected, actual)`. While this does not affect pass/fail outcomes when both values are equal, it produces misleading failure messages: the "expected" value shown in the diff would be the actual runtime value and vice versa, making test failures harder to diagnose.

**Before/After**
```java
// Before: actual first, expected second
assertEquals(connectionStates.connectionState(nodeId1), ConnectionState.AUTHENTICATION_FAILED);
assertEquals(metadata.lastSeenLeaderEpoch(tp).get().longValue(), 100);

// After: expected first, actual second (JUnit 5 convention)
assertEquals(ConnectionState.AUTHENTICATION_FAILED, connectionStates.connectionState(nodeId1));
assertEquals(100, metadata.lastSeenLeaderEpoch(tp).get().longValue());
```

**How to spot**
Run a static analysis pass (e.g., SpotBugs `JUA_DONT_ASSERT_INSTANCEOF_IN_TESTS`, or a custom rule) looking for `assertEquals(variable, CONSTANT)` patterns. The idiomatic JUnit 5 convention is always `assertEquals(EXPECTED_CONSTANT, actualExpression)`.

**Test**
This commit is a pure test-quality fix. No new test scenarios were added; existing tests were corrected to use the right argument order so that failure messages are meaningful.
