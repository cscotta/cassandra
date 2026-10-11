**Commit**: 8d66481a83 · **Ticket**: KAFKA-17897
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
The final part of KIP-1043 is to deprecate Admin.listConsumerGroups() in
favour of Admin.listGroups() which works for all group types. Reviewers: PoAn Yang <payang@apache.org>, Chia-Ping Tsai
<chia7712@gmail.com>

**Before/After**
```java
// Before
 * <p>

// After
     * @deprecated Since 4.1. Use {@link Admin#listGroups(ListGroupsOptions)} instead.
    @Deprecated(since = "4.1", forRemoval = true)
     * @deprecated Since 4.1. Use {@link Admin#listGroups(ListGroupsOptions)} instead.
    @Deprecated(since = "4.1", forRemoval = true)
 * @deprecated Since 4.1. Use {@link Admin#listGroups(ListGroupsOptions)} and {@link GroupListing} instead.
@Deprecated(since = "4.1")
    @Deprecated
    @SuppressWarnings("removal")
    @Deprecated
    @SuppressWarnings("removal")
    @Deprecated(since = "4.1", forRemoval = true)
 * @deprecated Since 4.1. Use {@link Admin#listGroups(ListGroupsOptions)} instead.
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17897 deprecate admin.listconsumergroups (#19477).

**Test**
Test modified in KafkaAdminClientTest.java covering the corrected behaviour.
