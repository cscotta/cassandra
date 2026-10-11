**Commit**: 2880e04129 · **Ticket**: KAFKA-18779
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
- Currently if we received extraneous topic partitions in the response
or if the response was missing some partitions requested, we were
processing the response as it came and even populated the callback with
these partitions. - These invalid responses should be parsed at the
`ShareConsumeRequestManager`. - If the response missed any acknowledgements for partitions that were
requested, then we fail the request with `InvalidRecordStateException`
and populate the callbacks. - For any extraneous partitions in the response, we log an error and
ignore them.

**Before/After**
```java
// Before
            if (acknowledgeRequestState == null || (!acknowledgeRequestState.onClose() && acknowledgeRequestState.isEmpty())) {
        } else if (acknowledgeRequestState.onClose()) {
                    topicResponse.partitions().forEach(partition ->
                            responseData.put(new TopicIdPartition(topicResponse.topicId(),
                                    partition.partitionIndex(),
                                    metadata.topicNames().getOrDefault(topicResponse.topicId(),
                                            topicNamesMap.remove(new IdAndPartition(topicResponse.topicId(), partition.partitionIndex())))), partition))
                    log.debug("For {}, received error {}, with leaderIdAndEpoch {}", tip, partitionError, partitionData.currentLeader());

// After
import org.apache.kafka.common.errors.InvalidRecordStateException;
import org.apache.kafka.common.message.ShareAcknowledgeResponseData;
    private static final String INVALID_RESPONSE = "Acknowledgement not successful due to invalid response from broker";
            if (acknowledgeRequestState == null || (!acknowledgeRequestState.isCloseRequest() && acknowledgeRequestState.isEmpty())) {
        } else if (acknowledgeRequestState.isCloseRequest()) {
                    topicResponse.partitions().forEach(partition -> {
                        TopicIdPartition tip = lookupTopicId(topicResponse.topicId(), partition.partitionIndex());
                        if (tip != null) {
                            responseData.put(tip, partition);
                        }
                    })
                    log.debug("For {}, received error {}, with leaderIdAndEpoch {} in ShareFetch", tip, partitionError, partitionData.currentLeader());
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18779: validate responses from broker in client for sharefetch and shareacknowledge rpcs. (#18939).

**Test**
Test modified in ShareConsumeRequestManagerTest.java covering the corrected behaviour.
