**Commit**: cc269b0d43 · **Ticket**: KAFKA-15045
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
This PR adds the logic and wiring necessary to make the callback to
TaskAssignor::onAssignmentComputed with the necessary parameters. We also fixed some log statements in the actual assignment error
computation, as well as modified the ApplicationState::allTasks method
to return a Map instead of a Set of TaskInfos. Reviewers: Anna Sophie Blee-Goldman <ableegoldman@apache.org>

**Before/After**
```java
// Before
import java.util.Set;
    Set<TaskInfo> allTasks();
        final Set<TaskId> statefulTasks = applicationState.allTasks().stream()
        final Set<TaskId> statelessTasks = applicationState.allTasks().stream()
        final Set<TaskId> allTaskIds = applicationState.allTasks().stream()
            .map(TaskInfo::id).collect(Collectors.toSet());
        final Set<TaskInfo> statefulTasks = applicationState.allTasks().stream()
import org.apache.kafka.streams.processor.internals.assignment.ApplicationStateImpl;

// After
import org.apache.kafka.streams.processor.TaskId;
    Map<TaskId, TaskInfo> allTasks();
        final Set<TaskId> statefulTasks = applicationState.allTasks().values().stream()
        final Set<TaskId> statelessTasks = applicationState.allTasks().values().stream()
        final Set<TaskId> allTaskIds = applicationState.allTasks().keySet();
        final Set<TaskInfo> statefulTasks = applicationState.allTasks().values().stream()
import org.apache.kafka.streams.processor.internals.assignment.DefaultApplicationState;
    @FunctionalInterface
    public interface UserTaskAssignmentListener {
        void onAssignmentComputed(GroupAssignment assignment, GroupSubscription subscription);
    }

```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15045: (kip-924 pt. 14) callback to taskassignor::onassignmentcomputed (#16123).

**Test**
No test added.
