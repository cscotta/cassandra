**Commit**: 8cbd2edfe7 · **Ticket**: KAFKA-17896
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
The implementation of Admin.describeClassicGroups from KIP-1043. Reviewers: Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
    /**
     * Describe some classic groups in the cluster.
     *
     * @param groupIds The IDs of the groups to describe.
     * @param options  The options to use when describing the groups.
     * @return The DescribeClassicGroupsResult.
     */
    DescribeClassicGroupsResult describeClassicGroups(Collection<String> groupIds,
                                                      DescribeClassicGroupsOptions options);

    /**
     * Describe some classic groups in the cluster, with the default options.
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17896: admin.describeclassicgroups (#17680).

**Test**
Test modified in KafkaAdminClientTest.java covering the corrected behaviour.
