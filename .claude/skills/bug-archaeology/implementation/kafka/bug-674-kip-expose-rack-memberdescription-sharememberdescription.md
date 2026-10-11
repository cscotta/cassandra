**Commit**: 7a8dba35a7 · **Ticket**: KAFKA-19784
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Hi Team:

Currently, the AdminClient’s describeConsumerGroups API returns a
MemberDescription that does not include rack information, even though
the underlying ConsumerGroupDescribeResponse protocol already supports a
rackId field. This causes users to be unable to retrieve member rack
information through the Admin API. Rack information is crucial for:

1. Monitoring and visualization tools
2.

**Before/After**
```java
// Before
     * @deprecated Since 4.0. Use {@link #MemberDescription(String, Optional, String, String, MemberAssignment, Optional, Optional, Optional)} instead.
    @Deprecated
     * @deprecated Since 4.0. Use {@link #MemberDescription(String, Optional, String, String, MemberAssignment, Optional, Optional, Optional)} instead.
    @Deprecated
     * @deprecated Since 4.0. Use {@link #MemberDescription(String, Optional, String, String, MemberAssignment, Optional, Optional, Optional)} instead.
    @Deprecated

// After
    private final Optional<String> rackId;
        Optional<String> rackId,
        this.rackId = rackId;
     * @deprecated Since 4.2. Use {@link #MemberDescription(String, Optional, Optional, String, String, MemberAssignment, Optional, Optional, Optional)} instead.
    @Deprecated(since = "4.2", forRemoval = true)
    public MemberDescription(
        String memberId,
        Optional<String> groupInstanceId,
        String clientId,
        String host,
        MemberAssignment assignment,
        Optional<MemberAssignment> targetAssignment,
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19784: kip-1227: expose rack id in memberdescription and sharememberdescription (#20691).

**Test**
Test modified in KafkaAdminClientTest.java covering the corrected behaviour.
