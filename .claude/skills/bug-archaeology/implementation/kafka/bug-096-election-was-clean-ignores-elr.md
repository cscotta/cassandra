**Commit**: db38bef076 · **Ticket**: KAFKA-18940
**Subsystem**: metadata/src/main/java/org/apache/kafka/metadata/PartitionRegistration.java
**Category**: Logic error in condition

**Root cause**: `electionWasClean(int newLeader, int[] isr)` declared an election clean only if the new leader was in the ISR. It did not consider the Eligible Leader Replicas (ELR) list, which was introduced for KIP-966 as a mechanism to allow controlled unclean-leader-election from a trusted set. When a broker was elected via ELR, `electionWasClean` returned `false`, causing `ControllerMetricsChanges` to increment the `uncleanLeaderElection` metric and `maybeLogPartitionChange` to emit an "UNCLEAN partition change" log line, both incorrectly.

**Before/After**

```java
// Before – ELR not considered
public static boolean electionWasClean(int newLeader, int[] isr) {
    return newLeader == NO_LEADER || Replicas.contains(isr, newLeader);
}

// After – ELR also qualifies as clean
public static boolean electionWasClean(int newLeader, int[] isr, int[] elr) {
    return newLeader == NO_LEADER
        || Replicas.contains(isr, newLeader)
        || Replicas.contains(elr, newLeader);
}
```

**How to spot**: Whenever a new replica-set concept (ELR, LastKnownELR) is added, search for all existing "clean election" checks and ensure they account for the new set.

**Test**: `testElectionWasClean` — adds assertions that a leader in the ELR but not ISR returns `true`, and that a leader in neither ISR nor ELR still returns `false`.
