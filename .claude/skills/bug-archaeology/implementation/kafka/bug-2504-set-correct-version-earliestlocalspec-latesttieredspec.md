**Commit**: d67c18b4ae · **Ticket**: KAFKA-17331
**Subsystem**: checkstyle/import-control.xml
**Category**: Logic error in condition

**Root cause**
Add the version check to client side when building ListOffsetRequest for the specific timestamp:
1) the version must be >=8 if timestamp=-4L (EARLIEST_LOCAL_TIMESTAMP)
2) the version must be >=9 if timestamp=-5L (LATEST_TIERED_TIMESTAMP)

Reviewers: PoAn Yang <payang@apache.org>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            .anyMatch(key -> offsetTimestampsByPartition.get(key) == ListOffsetsRequest.EARLIEST_LOCAL_TIMESTAMP || offsetTimestampsByPartition.get(key) == ListOffsetsRequest.LATEST_TIERED_TIMESTAMP);

        return ListOffsetsRequest.Builder
            .forConsumer(true, options.isolationLevel(), supportsMaxTimestamp, requireTieredStorageTimestamp)
            .setTargetTimes(new ArrayList<>(topicsByName.values()));
                .forConsumer(requireTimestamp, isolationLevel, false, false)
                .forConsumer(requireTimestamps, isolationLevel, false, false)
        public static Builder forReplica(short allowedVersion, int replicaId) {
            return new Builder((short) 0, allowedVersion, replicaId, IsolationLevel.READ_UNCOMMITTED);

// After
    <allow class="org.apache.kafka.clients.NodeApiVersions" exact-match="true" />
        boolean requireEarliestLocalTimestamp = keys
                .stream()
                .anyMatch(key -> offsetTimestampsByPartition.get(key) == ListOffsetsRequest.EARLIEST_LOCAL_TIMESTAMP);

            .anyMatch(key -> offsetTimestampsByPartition.get(key) == ListOffsetsRequest.LATEST_TIERED_TIMESTAMP);

        return ListOffsetsRequest.Builder.forConsumer(true,
                        options.isolationLevel(),
                        supportsMaxTimestamp,
                        requireEarliestLocalTimestamp,
                        requireTieredStorageTimestamp)
```

**How to spot**
When reviewing checkstyle/import-control.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17331 set correct version for earliestlocalspec and latesttieredspec (#16876).

**Test**
Test modified in KafkaAdminClientTest.java covering the corrected behaviour.
