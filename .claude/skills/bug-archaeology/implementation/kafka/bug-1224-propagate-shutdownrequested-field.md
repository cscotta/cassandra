**Commit**: 391b258d50 · **Ticket**: KAFKA-19077
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
When a member indicates that the application should shut down, set a
soft-state flag on the streams group and continuously set the status
`SHUTDOWN_APPLICATION` to all members, until the group is empty, which
resets the flag. Reviewers: Matthias J. Sax <matthias@confluent.io>, Chia-Ping Tsai
<chia7712@gmail.com>, Jeff Kim <jeff.kim@confluent.io>

**Before/After**
```java
// Before
import org.apache.kafka.common.message.StreamsGroupHeartbeatRequestData.TaskOffset;
     * @param taskEndOffsets      Cumulative changelog offsets for tasks, or null.
     * @param taskOffsets         Cumulative changelog end-offsets for tasks, or null.
        List<TaskOffset> taskOffsets,
        List<TaskOffset> taskEndOffsets,
                request.taskOffsets(),
                request.taskEndOffsets(),

// After
        if (shutdownApplication) {
            group.setShutdownRequestMemberId(memberId);
        }
        group.getShutdownRequestMemberId().ifPresent(requestingMemberId -> returnedStatus.add(
            new Status()
                .setStatusCode(StreamsGroupHeartbeatResponse.Status.SHUTDOWN_APPLICATION.code())
                .setStatusDetail(
                    String.format("Streams group member %s encountered a fatal error and requested a shutdown for the entire application.",
                        requestingMemberId)
                )
        ));

```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19077: propagate shutdownrequested field (#19359).

**Test**
Test modified in GroupMetadataManagerTest.java covering the corrected behaviour.
