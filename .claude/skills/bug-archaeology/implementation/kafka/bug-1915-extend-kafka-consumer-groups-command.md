**Commit**: fe88232b07 · **Ticket**: KAFKA-17750
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
1) Bump validVersions of ConsumerGroupDescribeRequest.json and ConsumerGroupDescribeResponse.json to "0-1". 2) Add MemberType field to ConsumerGroupDescribeResponse.json. Default value is -1 (unknown). 0 is for classic member and 1 is for consumer member.

**Before/After**
```java
// Before
  "validVersions": "0",
  "validVersions": "0",
              "about": "The target assignment." }
import org.apache.kafka.common.{ConsumerGroupState, Uuid}
import org.junit.jupiter.api.Assertions.assertEquals
                  ).asJava)),
                  ).asJava)),

// After
  // Version 1 adds MemberType field to ConsumerGroupDescribeResponse (KIP-1099).
  // For ConsumerGroupDescribeRequest, version 1 is same as version 0.
  "validVersions": "0-1",
  // Version 1 adds MemberType field (KIP-1099).
  "validVersions": "0-1",
              "about": "The target assignment." },
            { "name": "MemberType", "type": "int8", "versions": "1+", "default": "-1", "ignorable": true,
              "about": "-1 for unknown. 0 for classic member. +1 for consumer member." }
import org.apache.kafka.clients.consumer.ConsumerPartitionAssignor
import org.apache.kafka.clients.consumer.internals.ConsumerProtocol
import org.apache.kafka.common.{ConsumerGroupState, TopicPartition, Uuid}
import org.junit.jupiter.api.Assertions.{assertEquals, assertFalse}
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17750 extend kafka-consumer-groups command line tool to support new consumer group (part 1) (#17958).

**Test**
Test modified in ConsumerGroupDescribeRequestTest.scala covering the corrected behaviour.
