**Commit**: 1c582a4a35 · **Ticket**: KAFKA-18954
**Subsystem**: docs/ops.html
**Category**: Logic error in condition

**Root cause**
Add a metric to track the number of election is done using ELR. https://issues.apache.org/jira/browse/KAFKA-18954

Reviewers: Colin P. McCabe <cmccabe@apache.org>, Justine Olshan
<jolshan@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
      <tr>
        <td>Election from Eligible leader replicas rate</td>
        <td>kafka.controller:type=ControllerStats,name=ElectionFromEligibleLeaderReplicasPerSec</td>
        <td>0</td>
      </tr>
    private static final MetricName ELECTION_FROM_ELIGIBLE_LEADER_REPLICAS_PER_SEC = getMetricName(
        "ControllerStats", "ElectionFromEligibleLeaderReplicasPerSec");
    private Optional<Meter> electionFromEligibleLeaderReplicasMeter = Optional.empty();
        registry.ifPresent(r -> electionFromEligibleLeaderReplicasMeter =
                Optional.of(registry.get().newMeter(ELECTION_FROM_ELIGIBLE_LEADER_REPLICAS_PER_SEC, "elections", TimeUnit.SECONDS)));
    public void updateElectionFromEligibleLeaderReplicasCount(int count) {
        this.electionFromEligibleLeaderReplicasMeter.ifPresent(m -> m.mark(count));
```

**How to spot**
When reviewing docs/ops.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18954: add elr election rate metric (#19180).

**Test**
Test modified in ControllerMetadataMetricsTest.java covering the corrected behaviour.
