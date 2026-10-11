**Commit**: e3e3f25cc4 · **Ticket**: KAFKA-17500
**Subsystem**: clients/src
**Category**: State not cleaned up

**Root cause**
This PR implements the metadata redirection feature of the ShareFetch and ShareAcknowledge responses where an error code of NOT_LEADER_OR_FOLLOWER or FENCED_LEADER_EPOCH along with current leader information in the response is used to optimise handling of leadership changes in the client. This is applying the logic of KIP-951 to share group consumers. Reviewers: Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
@SuppressWarnings("NPathComplexity")
                            if (partitionError.exception() instanceof RetriableException) {

// After
import org.apache.kafka.clients.Metadata;
@SuppressWarnings({"NPathComplexity", "CyclomaticComplexity"})
            Map<TopicPartition, Metadata.LeaderIdAndEpoch> partitionsWithUpdatedLeaderInfo = new HashMap<>();
                Errors partitionError = Errors.forCode(partitionData.errorCode());
                if (partitionError == Errors.NOT_LEADER_OR_FOLLOWER || partitionError == Errors.FENCED_LEADER_EPOCH) {
                    log.debug("For {}, received error {}, with leaderIdAndEpoch {}", tip, partitionError, partitionData.currentLeader());
                    if (partitionData.currentLeader().leaderId() != -1 && partitionData.currentLeader().leaderEpoch() != -1) {
                        partitionsWithUpdatedLeaderInfo.put(tip.topicPartition(), new Metadata.LeaderIdAndEpoch(
                            Optional.of(partitionData.currentLeader().leaderId()), Optional.of(partitionData.currentLeader().leaderEpoch())));
                    }
                }

```

**How to spot**
When reviewing clients/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17500: metadata redirection for not_leader_or_follower (#17279).

**Test**
Test modified in ShareConsumeRequestManagerTest.java covering the corrected behaviour.
