**Commit**: c446e799be · **Ticket**: KAFKA-17010
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.protocol.Errors;
import org.apache.kafka.common.requests.DescribeLogDirsResponse;
import java.util.stream.Collectors;
import java.util.stream.Stream;
    /**
     * Return a map from brokerId to future which can be used to check the information of partitions on each individual broker.
     * @deprecated Deprecated Since Kafka 2.7. Use {@link #descriptions()}.
     */
    @Deprecated
    public Map<Integer, KafkaFuture<Map<String, DescribeLogDirsResponse.LogDirInfo>>> values() {
        return descriptions().entrySet().stream()
            .collect(Collectors.toMap(

// After
// (no additions)
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17010 remove `describelogdirsresponse#logdirinfo`, `describelogdirsresponse#replicainfo`, and `describelogdirsresu.

**Test**
Test modified in KafkaAdminClientTest.java covering the corrected behaviour.
