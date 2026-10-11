**Commit**: f8acfa5257 · **Ticket**: KAFKA-17621
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
While running large scale performance tests, we noticed that the logging on the ConsumerGroupHeartbeat path took a significant amount of CPU. It is mainly due to the very large data structures that we print out. I made a pass on those logs and I switched some of them to debug. Reviewers: Lianet Magrans <lianetmr@gmail.com>

**Before/After**
```java
// Before
                log.info("[GroupId {}] Computed new subscription metadata: {}.",
                    groupId, subscriptionMetadata);
                log.info("[GroupId {}] Computed new subscription metadata: {}.",
                    groupId, subscriptionMetadata);
                log.info("[GroupId {}] Member {} updated its subscribed topics to: {}.",
                log.info("[GroupId {}] Member {} updated its subscribed regex to: {}.",
            log.info("[GroupId {}] Member {} new assignment state: epoch={}, previousEpoch={}, state={}, "
                     + "assignedPartitions={} and revokedPartitions={}.",
                groupId, updatedMember.memberId(), updatedMember.memberEpoch(), updatedMember.previousMemberEpoch(), updatedMember.state(),
                assignmentToString(updatedMember.assignedPartitions()), assignmentToString(updatedMember.partitionsPendingRevocation()));
            log.info("[GroupId {}] Computed a new target assignment for epoch {} with '{}' assignor in {}ms: {}.",
                group.groupId(), groupEpoch, preferredServerAssignor, assignorTimeMs, assignmentResult.targetAssignment());

// After
                if (log.isDebugEnabled()) {
                    log.debug("[GroupId {}] Computed new subscription metadata: {}.",
                        groupId, subscriptionMetadata);
                }
                if (log.isDebugEnabled()) {
                    log.debug("[GroupId {}] Computed new subscription metadata: {}.",
                        groupId, subscriptionMetadata);
                }
                log.debug("[GroupId {}] Member {} updated its subscribed topics to: {}.",
                log.debug("[GroupId {}] Member {} updated its subscribed regex to: {}.",
            if (log.isDebugEnabled()) {
                log.debug("[GroupId {}] Member {} new assignment state: epoch={}, previousEpoch={}, state={}, "
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17621; reduce logging verbosity on consumergroupheartbeat path (#17288).

**Test**
No test added.
