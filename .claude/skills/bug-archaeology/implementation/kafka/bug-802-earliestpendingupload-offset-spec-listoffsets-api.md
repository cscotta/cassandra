**Commit**: 8d93d1096c · **Ticket**: KAFKA-17108
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This is the first part of the implementation of

[KIP-1023](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1023%3A+Follower+fetch+from+tiered+offset)

The purpose of this pull request is for the broker to start returning
the correct offset when it receives a -6 as a timestamp in a ListOffsets
API request. Added unit tests for the new timestamp. Reviewers: Kamal Chandraprakash <kamal.chandraprakash@gmail.com>

**Before/After**
```java
// Before
                        requireTieredStorageTimestamp)
            return forConsumer(requireTimestamp, isolationLevel, false, false, false);
                                          boolean requireTieredStorageTimestamp) {

// After
        } else if (offsetSpec instanceof OffsetSpec.EarliestPendingUploadSpec) {
            return ListOffsetsRequest.EARLIEST_PENDING_UPLOAD_TIMESTAMP;
    public static class EarliestPendingUploadSpec extends OffsetSpec { }

    /**
     * Used to retrieve the earliest offset of records that are pending upload to remote storage.
     * <br/>
     * Note: When tiered storage is not enabled, we will return unknown offset.
     */
    public static OffsetSpec earliestPendingUpload() {
        return new EarliestPendingUploadSpec();
    }
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17108: add earliestpendingupload offset spec in listoffsets api (#16584).

**Test**
Test modified in KafkaAdminClientTest.java covering the corrected behaviour.
