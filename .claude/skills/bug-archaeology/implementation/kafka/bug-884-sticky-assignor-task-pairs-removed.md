**Commit**: 29cf97b9ad · **Ticket**: KAFKA-19478

**Subsystem**: Kafka Streams / StickyTaskAssignor

**Category**: Performance (quadratic complexity removed)

**Root cause**
`StickyTaskAssignor` maintained a `TaskPairs` data structure to track pairs of tasks co-located on the same thread, used to prefer assignments that keep previously co-located tasks together. The structure had O(n²) space complexity relative to the number of tasks and provided unclear benefit over the base stickiness logic. The optimization was removed along with its three associated tests, simplifying the assignor and eliminating the quadratic memory overhead for large topologies.

**Before/After**
```java
// Before: TaskPairs tracks all co-located task pairs
private final TaskPairs taskPairs = new TaskPairs(maxTaskPairs);
// ... populated during assignment, consulted in scoring ...

// After: TaskPairs removed entirely
// Stickiness maintained through simpler existing mechanisms
```

**How to spot**
Data structures with O(n²) growth that are initialized per assignment cycle will cause memory pressure and GC pauses for large Streams applications with many tasks. Profile heap allocations during `StickyTaskAssignor.assign()` and look for quadratically-sized collections.

**Test**
Three `TaskPairs`-specific unit tests are removed; remaining `StickyTaskAssignorTest` cases verify that stickiness behavior is preserved without the `TaskPairs` optimization.
