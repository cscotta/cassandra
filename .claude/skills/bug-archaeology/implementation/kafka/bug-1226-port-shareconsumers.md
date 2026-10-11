**Commit**: 2d02f1d52d · **Ticket**: KAFKA-19084
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Currently for ShareConsumers, if we receive an
`UNKNOWN_TOPIC_OR_PARTITION` error code in the
`ShareAcknowledgeResponse`, then we retry sending the acknowledgements
until the timer expires. We ideally do not want this when a topic/partition is deleted, hence
like the
`CommitRequestManager`(https://github.com/apache/kafka/pull/15581), we
will treat this error as fatal and not retry the acknowledgements. PR also suppresses `InvalidTopicException` during `unsubscribe()` which
was also added in the
`AsyncKafkaConsumer`(https://github.com/apache/kafka/pull/16043). It was
later removed in the regular consumer
as they notified the background operations of metadata errors instead of
propagating them...

**Before/After**
```java
// Before
            if (partitionError == Errors.NOT_LEADER_OR_FOLLOWER || partitionError == Errors.FENCED_LEADER_EPOCH) {
                || e instanceof TopicAuthorizationException));

// After
            if (partitionError == Errors.NOT_LEADER_OR_FOLLOWER || partitionError == Errors.FENCED_LEADER_EPOCH || partitionError == Errors.UNKNOWN_TOPIC_OR_PARTITION) {
                // If the topic or partition has been deleted, we do not retry the failed acknowledgements.
                // Instead, these records will be re-delivered once they get timed out on the broker.
import org.apache.kafka.common.errors.InvalidTopicException;
                || e instanceof TopicAuthorizationException || e instanceof InvalidTopicException));
    @ParameterizedTest
    @EnumSource(value = Errors.class, names = {"FENCED_LEADER_EPOCH", "NOT_LEADER_OR_FOLLOWER", "UNKNOWN_TOPIC_OR_PARTITION"})
    public void testFatalErrorsAcknowledgementResponse(Errors error) {
        buildRequestManager();
        shareConsumeRequestManager.setAcknowledgementCommitCallbackRegistered(true);

        assignFromSubscribed(Collections.singleton(tp0));
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19084: port kafka-16224, kafka-16764 for shareconsumers (#19369).

**Test**
Test modified in ShareConsumeRequestManagerTest.java covering the corrected behaviour.
