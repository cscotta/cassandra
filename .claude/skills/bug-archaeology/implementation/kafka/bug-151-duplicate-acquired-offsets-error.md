**Commit**: d6728d4b29 · **Ticket**: KAFKA-19789
**Subsystem**: clients/src/main/java/org/apache/kafka/clients/consumer/internals/ShareCompletedFetch.java
**Category**: Logic error in condition

**Root cause**
When a broker returns a `ShareFetchResponse` with overlapping `AcquiredRecords` ranges, the client previously added all offset entries to the list without deduplication — including duplicate offsets. The duplicate was then acknowledged with a `GAP` type, which misled the broker (already in a bad state) and silently obscured the problem. No error was logged, so operators had no visibility into the broker-side bug.

**Before/After**
```java
// Before: no deduplication, every offset from every range added unconditionally
List<OffsetAndDeliveryCount> acquiredRecordList = new LinkedList<>();
partitionAcquiredRecords.forEach(acquiredRecords -> {
    for (long offset = acquiredRecords.firstOffset(); offset <= acquiredRecords.lastOffset(); offset++) {
        acquiredRecordList.add(new OffsetAndDeliveryCount(offset, acquiredRecords.deliveryCount()));
    }
});

// After: HashSet detects duplicates; error is logged and duplicate is skipped
Set<Long> offsets = new HashSet<>();
partitionAcquiredRecords.forEach(acquiredRecords -> {
    for (long offset = acquiredRecords.firstOffset(); offset <= acquiredRecords.lastOffset(); offset++) {
        if (!offsets.add(offset)) {
            log.error("Duplicate acquired record offset {} found ...", offset, partition.topicPartition());
        } else {
            acquiredRecordList.add(new OffsetAndDeliveryCount(offset, acquiredRecords.deliveryCount()));
        }
    }
});
```

**How to spot**
Look for consumer-side processing loops that iterate over broker-provided ranges and accumulate entries into a collection without checking for duplicates. When the underlying protocol guarantees uniqueness but the guarantee can be violated by a broker bug, the absence of a membership check means the client silently propagates the bad data and may acknowledge offsets in a way that further corrupts broker state. The lack of any `ERROR` or `WARN` log makes the failure invisible at default log levels.

**Test**
`testOverlappingAcquiredRecordsLogsErrorAndRetainsFirstOccurrence`: creates two overlapping `AcquiredRecords` ranges ([0-9] and [5-14]), calls `fetchRecords`, asserts exactly 15 unique records are returned, verifies offset 5 retains deliveryCount from the first range, and confirms no duplicate offsets appear in the output.
