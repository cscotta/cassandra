**Commit**: d17a149205 · **Ticket**: KAFKA-17956
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
KIP-1043 introduced Admin.listGroups as the way to list all types of groups. As a result, Admin.listShareGroups has been removed. This PR is the final step of the removal. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    /**
     * List the share groups available in the cluster.
     *
     * @param options The options to use when listing the share groups.
     * @return The ListShareGroupsResult.
     */
    ListShareGroupsResult listShareGroups(ListShareGroupsOptions options);

    /**
     * List the share groups available in the cluster with the default options.
     * <p>
     * This is a convenience method for {@link #listShareGroups(ListShareGroupsOptions)} with default options.

// After
// (no additions)
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17956 remove admin.listsharegroups (#17912).

**Test**
Test modified in KafkaAdminClientTest.java covering the corrected behaviour.
