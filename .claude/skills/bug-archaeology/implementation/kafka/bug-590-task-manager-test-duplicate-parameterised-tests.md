**Commit**: b2a9112681 · **Ticket**: KAFKA-19683

**Subsystem**: streams/src/test/java/org/apache/kafka/streams/processor/internals/TaskManagerTest.java

**Category**: State not cleaned up

**Root cause**
`TaskManagerTest` contained a parameterised-test pair that tested the same "compute offset sum from checkpoint" scenario once for the `CREATED` state and once for the `CLOSED` state, but each was a separate `@Test` method using the legacy `StateMachineTask` helper. The two nearly identical methods were merged into a single `@ParameterizedTest` with `@EnumSource`, and both methods were rewritten to use the Mockito-based `statefulTask(...).inState(state)` builder, removing the dependency on the deprecated `StateMachineTask` inner class.

**Before**
```java
// Before: two near-duplicate tests using StateMachineTask
@Test
public void shouldComputeOffsetSumFromCheckpointFileForUninitializedTask() { ... }
@Test
public void shouldComputeOffsetSumFromCheckpointFileForClosedTask() { ... }
```

**After**
```java
// After: single parameterised test using modern builder
@ParameterizedTest
@EnumSource(value = State.class, names = {"CREATED", "CLOSED"})
public void shouldComputeOffsetSumFromCheckpointFileForCreatedAndClosedTasks(State state) { ... }
```

**How to spot**
In `TaskManagerTest`, search for pairs of `@Test` methods whose names differ only by a state suffix (`ForUninitializedTask` vs `ForClosedTask`); these are strong candidates for consolidation into a `@ParameterizedTest`.

**Test**
Run the consolidated `@ParameterizedTest` with both `CREATED` and `CLOSED` enum values and confirm both variants produce the expected offset sums.
