**Commit**: c93e379080 · **Ticket**: KAFKA-19924

**Subsystem**: streams/src/main/java/org/apache/kafka/streams/processor/internals/TaskManager.java

**Category**: Logic error in condition

**Root cause**
`TaskManager.handleTaskCorruption` collected corrupted tasks into a `HashSet`, whose iteration order is non-deterministic. When multiple tasks were corrupted simultaneously, different runs could process them in different orders, producing non-deterministic behaviour that was hard to reproduce and test. Replacing every relevant `HashSet<Task>` with `TreeSet<Task>(Comparator.comparing(Task::id))` ensures that tasks are always processed in a consistent, ascending ID order regardless of JVM hash-code randomisation.

**Before**
```java
// Before
final Set<Task> corruptedActiveTasks = new HashSet<>();
```

**After**
```java
// After
final Set<Task> corruptedActiveTasks =
    new TreeSet<>(Comparator.comparing(Task::id));
```

**How to spot**
In `TaskManager`, search for `new HashSet<Task>` or `new HashSet<StreamTask>` used to accumulate tasks before iterating over them for side-effectful operations; any such collection where order matters for correctness or testability should be a `TreeSet`.

**Test**
Inject two corrupted tasks with known IDs and verify they are processed in ascending ID order in both unit tests and multi-threaded stress runs.
