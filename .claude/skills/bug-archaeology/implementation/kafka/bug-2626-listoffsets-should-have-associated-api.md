**Commit**: 6e324487fa · **Ticket**: KAFKA-16480
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
1. Use oldestAllowedVersion as 9 if using ListOffsetsRequest#EARLIEST_LOCAL_TIMESTAMP or ListOffsetsRequest#LATEST_TIERED_TIMESTAMP. 2. Add test cases to ListOffsetsRequestTest#testListOffsetsRequestOldestVersion to make sure requireTieredStorageTimestamp return 9 as minVersion.

**Before/After**
```java
// Before
            .forConsumer(true, options.isolationLevel(), supportsMaxTimestamp)
                .forConsumer(requireTimestamp, isolationLevel, false)

// After
        } else if (offsetSpec instanceof OffsetSpec.EarliestLocalSpec) {
            return ListOffsetsRequest.EARLIEST_LOCAL_TIMESTAMP;
        } else if (offsetSpec instanceof OffsetSpec.LatestTierSpec) {
            return ListOffsetsRequest.LATEST_TIERED_TIMESTAMP;
    public static class EarliestLocalSpec extends OffsetSpec { }
    public static class LatestTierSpec extends OffsetSpec { }
    /**
     * Used to retrieve the offset with the local log start offset,
     * log start offset is the offset of a log above which reads are guaranteed to be served
     * from the disk of the leader broker, when Tiered Storage is not enabled, it behaves the same
     * as the earliest timestamp
     */
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16480: listoffsets change should have an associated api/ibp version update (#16781).

**Test**
Test modified in KafkaAdminClientTest.java covering the corrected behaviour.
