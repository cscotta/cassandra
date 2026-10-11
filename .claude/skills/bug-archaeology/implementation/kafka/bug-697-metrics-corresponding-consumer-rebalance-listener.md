**Commit**: 616b0234c8 · **Ticket**: KAFKA-19691
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
- Background

  Add new metrics for TasksLost, TasksAssigned and TasksRevoked in
`StreamsRebalanceListener`. - Features Implemented

  Added three rebalance latency metrics to
DefaultStreamsRebalanceListener:    - `tasks-revoked-latency-avg/max` -
Average/max latency when revoking tasks    -
`tasks-assigned-latency-avg/max` - Average/max latency when assigning
tasks    - `tasks-lost-latency-avg/max` - Average/max latency when
losing tasks

  Main Changes

  1. Created `RebalanceMetrics.java`      - Follows existing Streams
ThreadMetrics pattern with static factory methods      - Provides three
methods to create sensors: `tasksRevokedSensor()`,
`tasksAssignedSensor()`, `tasksLostSensor()`    ...

**Before/After**
```java
// Before
                                           final TaskManager taskManager) {
            log.info("partition revocation took {} ms.", time.milliseconds() - start);
        taskManager.handleAssignment(activeTasksWithPartitions, standbyTasksWithPartitions);
        streamThread.setState(StreamThread.State.PARTITIONS_ASSIGNED);
        taskManager.handleRebalanceComplete();
        streamsRebalanceDat

// After
import org.apache.kafka.common.metrics.Sensor;
import org.apache.kafka.streams.processor.internals.metrics.RebalanceListenerMetrics;
import org.apache.kafka.streams.processor.internals.metrics.StreamsMetricsImpl;
    private final Sensor tasksRevokedSensor;
    private final Sensor tasksAssignedSensor;
    private final Sensor tasksLostSensor;
                                           final TaskManager taskManager,
                                           final StreamsMetricsImpl streamsMetrics,
                                           final String threadId) {

        // Create sensors for rebalance metrics
        this.tasksRevokedSensor = RebalanceListenerMetrics.tasksRevokedSensor(threadId, streamsMetrics);
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19691: add metrics corresponding to consumer rebalance listener metrics (#20464).

**Test**
Test modified in DefaultStreamsRebalanceListenerTest.java covering the corrected behaviour.
