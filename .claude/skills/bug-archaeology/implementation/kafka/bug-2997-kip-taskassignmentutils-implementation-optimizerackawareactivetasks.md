**Commit**: 5c08ee0062 · **Ticket**: KAFKA-15045
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
This PR implements the rack aware optimization of active tasks that can be used by the assignors themselves. It takes in the full output of the assignment and tries to reorganize it so as to minimize cross-rack traffic. Reviewers: Anna Sophie Blee-Goldman <ableegoldman@apache.org>

**Before/After**
```java
// Before
import static java.util.Collections.unmodifiableSet;

    private final Set<AssignedTask> assignment;
        this.assignment = unmodifiableSet(assignment);
        return assignment;
     * Assign standby tasks to KafkaStreams clients according to the default logic.
     * <p>
     * If rack-aware client tags are configured, the rack-aware standby task assignor will be used
     * @param applic

// After
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;
    private final Map<TaskId, AssignedTask> assignment;
        this.assignment = assignment.stream().collect(Collectors.toMap(AssignedTask::id, t -> t));
        // TODO change assignment to return a map so we aren't forced to copy this into a Set
        return new HashSet<>(assignment.values());
    }

    // TODO: merge this with #assignment by having it return a Map<TaskId, AssignedTask>
    public Set<TaskId> assignedTaskIds() {
        return assignment.keySet();
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15045: (kip-924 pt. 9) taskassignmentutils implementation of optimizerackawareactivetasks (#16033).

**Test**
No test added.
