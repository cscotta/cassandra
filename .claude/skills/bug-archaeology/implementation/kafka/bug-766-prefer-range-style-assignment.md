**Commit**: 620a01b74b · **Ticket**: KAFKA-19661
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
This is actually fixing a difference between the old and the new
assignor. Given the assignment ordering, the legacy assignor has a
preference for range-style assignments built in, that is, assigning

C1: 0_0, 1_0  C2: 0_1, 1_1

instead of

C1: 0_0, 0_1  C2: 1_0, 1_1

We add tests to both assignors to check for this behavior, and improve
the new assingor by enforcing corresponding orderings. Reviewers: Bill Bejeck <bill@confluent.io>

**Before/After**
```java
// Before
        //active
        final Set<TaskId> activeTasks = taskIds(topologyDescriber, true);
            final Set<TaskId> statefulTasks = taskIds(topologyDescriber, false);
    private Set<TaskId> taskIds(final TopologyDescriber topologyDescriber, final boolean isActive) {
        final Set<TaskId> ret = new HashSet<>();
    private void assignActive(final Set<TaskId> activeTasks) {
    private void assignStandby(final Set<TaskId> standbyTasks, final int numStandbyReplicas) {
        for (final TaskId task : standbyTasks) {

// After
import java.util.LinkedList;
        final LinkedList<TaskId> activeTasks = taskIds(topologyDescriber, true);
            final LinkedList<TaskId> statefulTasks = taskIds(topologyDescriber, false);
    private LinkedList<TaskId> taskIds(final TopologyDescriber topologyDescriber, final boolean isActive) {
        final LinkedList<TaskId> ret = new LinkedList<>();
    private void assignActive(final LinkedList<TaskId> activeTasks) {

        // Assuming our current assignment pairs same partitions (range-based), we want to sort by partition first
        activeTasks.sort(Comparator.comparing(TaskId::partition).thenComparing(TaskId::subtopologyId));
        // To achieve an initially range-based assignment, sort by subtopology
        activeTasks.sort(Comparator.comparing(TaskId::subtopologyId).thenComparing(TaskId::partition));

```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19661 [4/n]: prefer range-style assignment (#20486).

**Test**
Test modified in StickyTaskAssignorTest.java covering the corrected behaviour.
