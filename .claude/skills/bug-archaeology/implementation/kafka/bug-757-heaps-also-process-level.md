**Commit**: 8628d74c49 · **Ticket**: KAFKA-19661
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
In the current solution, we only use a heap to select the right process,
but resort to linear search for selecting a member within a process. This means use cases where a lot of threads run within the same process
can yield slow assignment. The number of threads in a process shouldn’t
scale arbitrarily (our assumed case for benchmarking of 50 threads in a
single process seems quite extreme already), however, we can optimize
for this case to reduce the runtime further. Other assignment algorithms assign directly on the member-level, but we
cannot do this in Kafka Streams, since we cannot assign tasks to
processes that already own the task.

**Before/After**
```java
// Before
 * @param activeTasks  Reconciled active tasks
 * @param standbyTasks Reconciled standby tasks
 * @param warmupTasks  Reconciled warm-up tasks

    public void addTask(final String memberId, final TaskId taskId, final boolean isActive) {

// After
 * @param activeTasks  Current target active tasks
 * @param standbyTasks Current target standby tasks
 * @param warmupTasks  Current target warm-up tasks
import java.util.AbstractMap;
import java.util.PriorityQueue;
    private PriorityQueue<Map.Entry<String, Integer>> membersByLoad;
        this.membersByLoad = null;
    /**
     * Assigns a task to a member of this process.
     *
     * @param memberId The member to assign to.
     * @param taskId   The task to assign.
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19661 [6/n]: use heaps also on the process-level (#20523).

**Test**
No test added.
