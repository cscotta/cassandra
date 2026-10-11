**Commit**: 370e5ea1f8 · **Ticket**: KAFKA-15045
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
This fills in the implementation details of the standby task assignment utility functions within TaskAssignmentUtils. Reviewers: Anna Sophie Blee-Goldman <ableegoldman@apache.org>

**Before/After**
```java
// Before
import java.util.HashSet;
    private final Map<TaskId, AssignedTask> assignment;
        return new KafkaStreamsAssignment(processId, assignment, Optional.empty());
        return new KafkaStreamsAssignment(this.processId(), this.assignment(), Optional.of(rebalanceDeadline));
                                   final Set<AssignedTask> assignment,
        this.assignment = assignment.stream().collect(Collectors.toMap(AssignedTask::id, t -> t));
     * @return a set of assigned tasks that are part of this {@code KafkaStreamsAssignment}
    public Set<AssignedTask> assignment() {
        // TODO change assignment to return a map so we aren't forced to copy this into a Set
        return new HashSet<>(assignment.values());
    }


// After
import static java.util.Collections.unmodifiableMap;

import java.util.function.Function;
    private final Map<TaskId, AssignedTask> tasks;
        final Map<TaskId, AssignedTask> tasks = assignment.stream().collect(Collectors.toMap(AssignedTask::id, Function.identity()));
        return new KafkaStreamsAssignment(processId, tasks, Optional.empty());
        return new KafkaStreamsAssignment(this.processId(), this.tasks(), Optional.of(rebalanceDeadline));
                                   final Map<TaskId, AssignedTask> tasks,
        this.tasks = tasks;
     * @return a read-only set of assigned tasks that are part of this {@code KafkaStreamsAssignment}
    public Map<TaskId, AssignedTask> tasks() {
        return unmodifiableMap(tasks);
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15045: (kip-924 pt. 15) implement #defaultstandbytaskassignment and finish rack-aware standby optimization (#16129.

**Test**
No test added.
