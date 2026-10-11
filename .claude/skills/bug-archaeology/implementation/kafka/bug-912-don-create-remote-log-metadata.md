**Commit**: 14ea11dc31 · **Ticket**: KAFKA-19371
**Subsystem**: storage/src
**Category**: Logic error in condition

**Root cause**
* The CREATE_TOPIC request gets issued only when it is clear that the
topic does not exist in the cluster. * When the request to describe the topic gets timed-out or any exception
thrown other than UnknownTopicOrPartitionException, then the same gets
re-thrown and the describe/create topic request gets retried in the next
iteration until the initializationRetryMaxTimeoutMs gets breached. Fixes: https://issues.apache.org/jira/browse/KAFKA-19371

Reviewers: Luke Chen <showuon@gmail.com>, Kamal Chandraprakash
<kamal.chandraprakash@gmail.com>

---------

Co-authored-by: stroller.fu <stroller.fu@zoom.us>

**Before/After**
```java
// Before
    boolean doesTopicExist(Admin adminClient, String topic) {
            } else {
                log.info("Topic {} does not exist.", topic);
            return description != null;
            log.info("Topic {} does not exist. Error: {}", topic, ex.getCause().getMessage());
            return false;
                log.error("Encountered error while creating {} topic.", topic, e);
    public void testTopicDoesNotExist() {

// After
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
    boolean doesTopicExist(Admin adminClient, String topic) throws ExecutionException, InterruptedException {
            return true;
            if (ex.getCause() instanceof UnknownTopicOrPartitionException) {
                log.info("Topic {} does not exist", topic);
                return false;
            }
            throw ex;
                log.error("Encountered error while querying or creating {} topic.", topic, e);
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaFuture;
```

**How to spot**
When reviewing storage/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19371: don't create the __remote_log_metadata topic when it already exists during broker restarts (#19899).

**Test**
Test modified in TopicBasedRemoteLogMetadataManagerTest.java covering the corrected behaviour.
