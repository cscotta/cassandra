**Commit**: d5ad9228cf · **Ticket**: KAFKA-17750
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
This patch extends the `kafka-consumer-groups` command line tool to support the new consumer group as described in KIP-1099. Signed-off-by: PoAn Yang <payang@apache.org>

Reviewers: David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
        private void printOffsets(Map<String, Entry<Optional<GroupState>, Optional<Collection<PartitionAssignmentState>>>> offsets) {
                    String format = printOffsetFormat(assignments);
                    System.out.printf(format, "GROUP", "TOPIC", "PARTITION", "CURRENT-OFFSET", "LOG-END-OFFSET", "LAG", "CONSUMER-ID", "HOST", "CLIENT-ID");
                            System.out.printf(format,
                                consumerAssignment.group,
                                consumerAssignment.topic.orElse(MISSING_COLUMN_VALUE), consumerAssignment.partition.map(Object::toString).orElse(MISSING_COLUMN_VALUE),
                                consumerAssignment.offset.map(Object::toString).orElse(MISSING_COLUMN_VALUE), consumerAssignment.logEndOffset.map(Object::toString).orElse(MISSING_COLUMN_VALUE),
                                consumerAssignment.lag.map(Object::toString).orElse(MISSING_COLUMN_VALUE), consumerAssignment.consumerId.orElse(MISSING_COLUMN_VALUE),
                                consumerAssignment.host.orElse(MISSING_COLUMN_VALUE), consumerAssignment.clientId.orElse(MISSING_COLUMN_VALUE)
                            );
        private static String printOffsetFormat(Optional<Collection<PartitionAssignmentState>> assignments) {

// After
        private void printOffsets(
            Map<String, Entry<Optional<GroupState>, Optional<Collection<PartitionAssignmentState>>>> offsets,
            boolean verbose
        ) {
                    String format = printOffsetFormat(assignments, verbose);
                    if (verbose) {
                        System.out.printf(format, "GROUP", "TOPIC", "PARTITION", "LEADER-EPOCH", "CURRENT-OFFSET", "LOG-END-OFFSET", "LAG", "CONSUMER-ID", "HOST", "CLIENT-ID");
                    } else {
                        System.out.printf(format, "GROUP", "TOPIC", "PARTITION", "CURRENT-OFFSET", "LOG-END-OFFSET", "LAG", "CONSUMER-ID", "HOST", "CLIENT-ID");
                    }
                            if (verbose) {
                                System.out.printf(format,
```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17750; extend kafka-consumer-groups command line tool to support new consumer group (part 3) (#18141).

**Test**
Test modified in ConsumerGroupCommandTestUtils.java covering the corrected behaviour.
