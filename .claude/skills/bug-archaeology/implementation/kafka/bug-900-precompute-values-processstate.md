**Commit**: a88fd01e74 · **Ticket**: KAFKA-19478

**Subsystem**: Kafka Streams / StickyTaskAssignor (ProcessState)

**Category**: Performance (recomputation on every access)

**Root cause**
`ProcessState` recalculated `totalTaskCount()`, `activeTaskCount()`, `assignedTasks`, and `load` by streaming over its task collections on every accessor call. Because these accessors are invoked inside deeply nested assignment loops, the O(n) recomputation per call made the overall assignment algorithm O(n²) or worse for large topologies. The fix precomputes `taskCount`, `activeTaskCount`, and the `assignedTasks` set at insertion time and caches `load`, reducing all accessors to O(1). The `hasTask()` check switches from a stream-based search to a set lookup.

**Before/After**
```java
// Before: recomputed on every call
public int totalTaskCount() {
    return assignedStandbyTasks().size() + assignedActiveTasks().size();
}
public boolean hasCapacity() { return totalTaskCount() < capacity; }
public boolean hasTask(TaskId id) {
    return assignedActiveTasks().contains(id) || assignedStandbyTasks().contains(id);
}

// After: cached fields, O(1) accessors
private int taskCount;
private int activeTaskCount;
private final Set<TaskId> assignedTasks = new HashSet<>();

public boolean hasCapacity() { return load < 1.0; }
public boolean hasTask(TaskId id) { return assignedTasks.contains(id); }
```

**How to spot**
Methods named `totalXCount()` or `hasX()` that recompute their result by scanning a collection are latent performance bugs when called in tight loops. Profile the assignment hot path; if `totalTaskCount` or `assignedActiveTasks` appear high in CPU samples, the values should be precomputed.

**Test**
`ProcessStateTest` verifies that `taskCount`, `activeTaskCount`, and `hasTask` return correct values after a sequence of add/remove operations, and benchmark results confirm the ~14x runtime improvement reported in the commit message.
