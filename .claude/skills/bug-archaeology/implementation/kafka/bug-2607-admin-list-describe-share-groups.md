**Commit**: b9099301fb · **Ticket**: KAFKA-16716
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Adds the methods to the admin client for listing and describing share groups. There are some unit tests, but integration tests will be in a follow-on PR. Reviewers:  Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
     * Describe some group IDs in the cluster.
     * @return The DescribeConsumerGroupResult.
     * Describe some group IDs in the cluster, with the default options.
     * @return The DescribeConsumerGroupResult.

// After
     * Describe some consumer groups in the cluster.
     * @return The DescribeConsumerGroupsResult.
     * Describe some consumer groups in the cluster, with the default options.
     * @return The DescribeConsumerGroupsResult.
    /**
     * Describe some share groups in the cluster.
     *
     * @param groupIds The IDs of the groups to describe.
     * @param options  The options to use when describing the groups.
     * @return The DescribeShareGroupsResult.
     */
    DescribeShareGroupsResult describeShareGroups(Collection<String> groupIds,
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16716: add admin list and describe share groups (#16827).

**Test**
Test modified in KafkaAdminClientTest.java covering the corrected behaviour.
