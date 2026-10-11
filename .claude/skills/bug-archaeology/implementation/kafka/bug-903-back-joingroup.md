**Commit**: 4b607616c7 · **Ticket**: KAFKA-19444
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This fixes librdkafka older than the recently released 2.11.0 with
Kerberos authentication and Apache Kafka 4.x. Even though this is a bug in librdkafka, a key goal of KIP-896 is not to
break the popular client libraries listed in it. Adding back JoinGroup
v0 & v1 is a very small change and worth it from that perspective. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  // Versions 0-1 were removed in Apache Kafka 4.0, Version 2 is the new baseline.
  //
  "validVersions": "2-9",
  // Versions 0-1 were removed in Apache Kafka 4.0, Version 2 is the new baseline.
  //
  "validVersions": "2-9",

// After
  "validVersions": "0-9",
  "validVersions": "0-9",
import org.apache.kafka.common.protocol.MessageUtil;
import static org.junit.jupiter.api.Assertions.assertEquals;

    @Test
    public void testRebalanceTimeoutDefaultsToSessionTimeoutV0() {
        int sessionTimeoutMs = 30000;
        short version = 0;

        var buffer = MessageUtil.toByteBufferAccessor(new JoinGroupRequestData()
                .setGroupId("groupId")
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19444: add back joingroup v0 & v1 (#20116).

**Test**
Test modified in JoinGroupRequestTest.java covering the corrected behaviour.
