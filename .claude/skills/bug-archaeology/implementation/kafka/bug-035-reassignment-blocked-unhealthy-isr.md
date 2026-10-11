**Commit**: 3d267d4536 · **Ticket**: KAFKA-19975
**Subsystem**: metadata/src/main/java/org/apache/kafka/controller/PartitionReassignmentReplicas.java
**Category**: Logic error in condition

**Root cause**
`PartitionReassignmentReplicas.maybeCompleteReassignment` required that all target replicas be present in the ISR before completing a reassignment. This was too strict: a replica that existed in the assignment before the reassignment began and remained in the target set (neither added nor removed) could be unhealthy without blocking the ISR relative to the old assignment. The fix only blocks completion when (a) newly-added replicas are not yet in the ISR, or (b) the replication factor is being reduced (where completing could shrink the ISR unnecessarily).

**Before/After**
```java
// Before – blocks if ANY target replica is missing from ISR
if (!newTargetIsr.containsAll(newTargetReplicas)) return Optional.empty();

// After – only block on new (adding) replicas and RF-reducing cases
if (!newTargetIsr.containsAll(adding)) return Optional.empty();
if (adding.size() < removing.size() && !newTargetIsr.containsAll(newTargetReplicas))
    return Optional.empty();
```

**How to spot**
Reassignment completion conditions should be reasoned about in terms of whether the ISR *size* could decrease. A blanket `containsAll(targetReplicas)` conflates stable pre-existing replicas with newly-added ones and can leave reassignments permanently stuck.

**Test**
Multiple new unit tests in `PartitionReassignmentReplicasTest` cover: completing when an existing-target replica is absent (RF same or increasing), not completing when an adding replica is absent, and not completing when RF decreases with missing replicas.
