**Commit**: de92b200fc · **Ticket**: KAFKA-18699
**Subsystem**: metadata/src
**Category**: State not cleaned up

**Root cause**
https://issues.apache.org/jira/browse/KAFKA-18699

This PR aims at cleaning up the `metadata` module further by getting rid
of some extra code which can be replaced by record

Reviewers: Ken Huang <s7133700@gmail.com>, Ming-Yen Chung
<mingyen066@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import java.util.Objects;


class BrokerControlStates {
    private final BrokerControlState current;
    private final BrokerControlState next;

    BrokerControlStates(BrokerControlState current, BrokerControlState next) {
        this.current = current;
        this.next = next;
    }


// After
record BrokerControlStates(BrokerControlState current, BrokerControlState next) {
public record BrokerIdAndEpoch(int id, long epoch) {
        targetIsr = completedReassignment.isr();
        targetReplicas = completedReassignment.replicas();
    record CompletedReassignment(List<Integer> replicas, List<Integer> isr) {
    private record IneligibleReplica(int replicaId, String reason) {
```

**How to spot**
When reviewing metadata/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18699 cleanup metadata module (#20346).

**Test**
Test modified in PartitionReassignmentReplicasTest.java covering the corrected behaviour.
