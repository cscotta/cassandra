**Commit**: 618ea2c1ca · **Ticket**: KAFKA-18285
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Adds `describeStreamsGroup` to Admin API. This exposes the result of the `DESCRIBE_STREAMS_GROUP` RPC in the Admin
API. Reviewers: Bill Bejeck <bill@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
    /**
     * Describe streams groups in the cluster.
     *
     * @param groupIds The IDs of the groups to describe.
     * @param options  The options to use when describing the groups.
     * @return The DescribeStreamsGroupsResult.
     */
    DescribeStreamsGroupsResult describeStreamsGroups(Collection<String> groupIds,
                                                      DescribeStreamsGroupsOptions options);

    /**
     * Describe streams groups in the cluster, with the default options.
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18285: add describestreamsgroup to admin api (#19116).

**Test**
Test modified in KafkaAdminClientTest.java covering the corrected behaviour.
