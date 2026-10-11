**Commit**: 7ea32a0e93 · **Ticket**: KAFKA-19459

**Subsystem**: Kafka Streams / Admin client

**Category**: Missing error handler / unimplemented code path

**Root cause**
When the admin client attempted to delete a Streams consumer group and received `UnsupportedVersionException`, the error was propagated without any additional context. Operators had no way to know which internal topics were associated with the group and needed manual cleanup. The fix catches `UnsupportedVersionException`, lists the internal topics for the group, and includes their names in the exception message so operators can take informed action.

**Before/After**
```java
// Before: raw exception propagated
admin.deleteConsumerGroups(List.of(groupId)).all().get();

// After: catch UnsupportedVersionException, enrich message
try {
    admin.deleteConsumerGroups(List.of(groupId)).all().get();
} catch (ExecutionException e) {
    if (e.getCause() instanceof UnsupportedVersionException) {
        List<String> internalTopics = listInternalTopicsForGroup(groupId);
        throw new UnsupportedVersionException(
            "Cannot delete group " + groupId +
            ". Internal topics to clean up manually: " + internalTopics, e.getCause());
    }
    throw e;
}
```

**How to spot**
Error paths that catch version-related exceptions and rethrow without additional context leave operators without actionable information. Always include resource names (topics, group IDs) in exception messages so operators can perform manual remediation.

**Test**
`StreamsGroupCommandTest` simulates a broker that returns `UnsupportedVersionException` on group deletion and asserts that the enriched error message lists the relevant internal topics.
