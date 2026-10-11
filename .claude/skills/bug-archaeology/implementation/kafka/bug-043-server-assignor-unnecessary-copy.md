**Commit**: a9dff623cc · **Ticket**: KAFKA-19955
**Subsystem**: group-coordinator/src/main/java/org/apache/kafka/coordinator/group/modern/MemberAssignmentImpl.java
**Category**: Wrong constant / default value

**Root cause**
`MemberAssignmentImpl`'s compact-constructor wrapped the `partitions` map in `Collections.unmodifiableMap()`. Server-side assignors are designed to reuse and lazily clone the previous assignment map, mutating it in place when possible. Because the map was now unmodifiable, every attempted mutation triggered a defensive copy, causing an O(n) allocation per change rather than O(1) in-place updates — a significant performance regression for large groups.

**Before/After**
```java
// Before – always makes map unmodifiable; forces clones in assignors
public record MemberAssignmentImpl(Map<Uuid, Set<Integer>> partitions) implements MemberAssignment {
    public MemberAssignmentImpl {
        partitions = Collections.unmodifiableMap(Objects.requireNonNull(partitions));
    }
}

// After – map stays mutable for in-place updates
public record MemberAssignmentImpl(Map<Uuid, Set<Integer>> partitions) implements MemberAssignment {
    public MemberAssignmentImpl {
        Objects.requireNonNull(partitions);
    }
}
```

**How to spot**
Immutability wrappers added to record compact constructors should be reviewed when the object is used in performance-sensitive mutation contexts. The defensive wrapper is safe from an API correctness standpoint but breaks the lazy-clone optimization.

**Test**
`MemberAssignmentImplTest#testPartitionsMutable`: asserts that `memberAssignment.partitions().put(...)` and `.get(...).add(...)` do not throw `UnsupportedOperationException`.
