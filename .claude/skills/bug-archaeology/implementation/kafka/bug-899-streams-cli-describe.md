**Commit**: db1c6f31a3 · **Ticket**: KAFKA-18288

**Subsystem**: Kafka Streams / StreamsGroupCommand CLI

**Category**: Logic error in condition

**Root cause**
`StreamsGroupCommand.describeGroups()` iterated the list of requested group IDs but always called `getDescribeGroup(groups.get(0))` — passing the first group regardless of which iteration index was current. All describe output therefore reflected only the first group even when multiple groups were specified. Additionally, `GROUP_ID_NOT_FOUND` errors were silently converted to an empty DEAD group description instead of propagating a `GroupIdNotFoundException`, and `timeoutMsOpt` was not passed to `describeStreamsGroups()`, causing the call to use the default timeout.

**Before/After**
```java
// Before: always describes groups.get(0)
for (String groupId : groupIds) {
    StreamsGroupDescription description = getDescribeGroup(groups.get(0));
    // ...
}

// After: describes the current groupId in the loop
for (String groupId : groupIds) {
    StreamsGroupDescription description = getDescribeGroup(groupId);
    // ...
}
// GROUP_ID_NOT_FOUND now throws GroupIdNotFoundException
// timeoutMsOpt passed to describeStreamsGroups()
```

**How to spot**
In CLI command handlers that loop over user-supplied IDs, look for array/list accesses inside the loop that use a hard-coded index (`get(0)`) instead of the loop variable. This classic copy-paste error produces correct output only when exactly one ID is provided.

**Test**
`DescribeStreamsGroupTest` integration tests verify that describing multiple groups returns correct, distinct descriptions for each specified group ID.
