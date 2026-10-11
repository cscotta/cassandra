**Commit**: 58d208f7b9 · **Ticket**: KAFKA-20282
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Add info msg if Consumer created using the classic protocol, to guide
users towards the next-gen Consumer rebalance protocol (GA). Show only
if using group rebalance capabilities (group.id defined). Exclude Streams applications to avoid showing the log on streams (should
follow  separate evolution to the new streams protocol). This is the first step in KIP-1274 (evolution path towards the new
Consumer protocol)

Reviewers: Andrew Schofield <aschofield@confluent.io>, Lan Ding
 <isDing_L@163.com>

**Before/After**
```java
// Before
// (no removals)

// After
import java.util.Locale;
            // If the classic rebalance protocol is used, log message to guide users towards upgrading to the
            // next-generation consumer rebalance protocol
            if (groupId.isPresent()) {
                boolean isStreamsConsumer = assignors.stream()
                        .anyMatch(a -> a.getClass().getName().contains("StreamsPartitionAssignor"));
                if (!isStreamsConsumer) {
                    log.info("\n" +
                            "****************************************************************\n" +
                            "* The consumer rebalance protocol (KIP-848) is production ready!\n" +
                            "* Set the consumer configuration {}={} to try it out.\n" +
                            "* See https://kafka.apache.org/documentation/#consumer_rebalance_protocol\n" +
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20282: add message to classic consumer startup guiding to next-gen protocol (#21900).

**Test**
Test modified in KafkaConsumerTest.java covering the corrected behaviour.
