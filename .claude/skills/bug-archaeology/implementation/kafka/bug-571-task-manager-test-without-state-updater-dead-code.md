**Commit**: ea88a8ec3f · **Ticket**: KAFKA-19683

**Subsystem**: streams/src/test/java/org/apache/kafka/streams/processor/internals/TaskManagerTest.java

**Category**: State not cleaned up

**Root cause**
`TaskManagerTest` relied on a legacy `StateMachineTask` helper class and a `setUpTaskManagerWithoutStateUpdater` setup method that both simulated the old (non-StateUpdater) code path. Since the `StateUpdater` is now always enabled, these helpers remained as dead scaffolding that made tests harder to read and occasionally required the `@SuppressWarnings("removal")` annotation to compile. The fix removes both the helper class and the without-state-updater setup, replacing them with the standard Mockito-based patterns.

**Before**
```java
// Before: legacy helper still present
private TaskManager setUpTaskManagerWithoutStateUpdater(...) { ... }
final StateMachineTask task = new StateMachineTask(...);
@SuppressWarnings("removal")
public void shouldCommit...WithEOS() { ... }
```

**After**
```java
// After: Mockito mock replaces StateMachineTask
private TaskManager setUpTaskManager(...) { ... }
final ConsumerGroupMetadata groupMetadata = mock(ConsumerGroupMetadata.class);
```

**How to spot**
Look for test setup methods whose name includes `WithoutStateUpdater` and for the `StateMachineTask` inner class; both are vestigial once the `StateUpdater` is always enabled and should be removed.

**Test**
Run the full `TaskManagerTest` suite after the cleanup and confirm all tests pass without `@SuppressWarnings("removal")` annotations.
