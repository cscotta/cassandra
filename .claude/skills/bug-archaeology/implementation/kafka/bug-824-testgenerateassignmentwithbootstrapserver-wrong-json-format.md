**Commit**: 18045c6ac3 · **Ticket**: KAFKA-19592
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
This PR do following:
1. Use correct json format to test. 2. make `PartitionReassignmentState` and `VerifyAssignmentResult.java`
become record

Reviewers: TengYao Chi <frankvicky@apache.org>, Ken Huang
 <s7133700@gmail.com>, PoAn Yang <payang@apache.org>

**Before/After**
```java
// Before
import java.util.Objects;
final class PartitionReassignmentState {
    public final List<Integer> currentReplicas;

    public final List<Integer> targetReplicas;

    public final boolean done;

    /**
     * @param currentReplicas The current replicas.
     * @param targetReplicas  The target replicas.
     * @param done            True if the reassignment is done.

// After
 *
 * @param currentReplicas The current replicas.
 * @param targetReplicas  The target replicas.
 * @param done            True if the reassignment is done.
record PartitionReassignmentState(
        List<Integer> currentReplicas,
        List<Integer> targetReplicas,
        boolean done
) { }
            if (state.done()) {
                if (state.currentReplicas().equals(state.targetReplicas())) {
                    String currentReplicaStr = state.currentReplicas().stream().map(String::valueOf).collect(Collectors.joining(","));
```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19592: testgenerateassignmentwithbootstrapserver uses wrong json format (#20336).

**Test**
Test modified in ReassignPartitionsCommandTest.java covering the corrected behaviour.
