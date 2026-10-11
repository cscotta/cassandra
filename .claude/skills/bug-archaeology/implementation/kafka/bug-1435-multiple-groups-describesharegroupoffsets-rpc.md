**Commit**: 952113e8e0 · **Ticket**: KAFKA-16720
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>, Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.requests.FindCoordinatorRequest;
import java.util.Collections;
    private final AdminApiLookupStrategy<CoordinatorKey> lookupStrategy;
    public ListShareGroupOffsetsHandler(
        Map<String, ListShareGroupOffsetsSpec> groupSpecs,
        LogContext logContext) {
        List<String> groupIds = keys.stream().map(key -> {
            if (key.type != FindCoordinatorRequest.CoordinatorType.GROUP) {
                throw new IllegalArgumentException("Invalid group coordinator key " + key +
                    " when building `DescribeShareGroupOffsets` request");
            return key.idValue;
        }).collect(Collectors.toList());

// After
     *
     * @param topicPartitions List of topic partitions to include
import org.apache.kafka.common.message.DescribeShareGroupOffsetsRequestData.DescribeShareGroupOffsetsRequestGroup;
import org.apache.kafka.common.message.DescribeShareGroupOffsetsRequestData.DescribeShareGroupOffsetsRequestTopic;
import org.apache.kafka.common.message.DescribeShareGroupOffsetsResponseData;
import java.util.ArrayList;
import java.util.LinkedList;
    private final CoordinatorStrategy lookupStrategy;
    public ListShareGroupOffsetsHandler(Map<String, ListShareGroupOffsetsSpec> groupSpecs,
                                        LogContext logContext) {
        validateKeys(keys);

```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16720: support multiple groups in describesharegroupoffsets rpc (#18834).

**Test**
Test modified in KafkaAdminClientTest.java covering the corrected behaviour.
