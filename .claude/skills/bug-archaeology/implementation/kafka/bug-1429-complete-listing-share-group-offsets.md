**Commit**: 79e853d68e · **Ticket**: KAFKA-18761
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                printOffsets(offsets);
                            earliestResult.get(tp.getKey())
        private void printOffsets(TreeMap<String, Entry<ShareGroupDescription, Collection<SharePartitionOffsetInformation>>> offsets) {
                    String fmt = printOffsetFormat(groupId, offsetsInfo);
                    System.out.printf(fmt, "GROUP", "TOPIC", "PARTITION", "START-OFFSET");
                        System.out.printf(fmt,
                            groupId,
                            info.topic,
                            info.partition,
                            info.offset
                        );
        private static String printOffsetFormat(String groupId, Collection<SharePartitionOffsetInformation> offsetsInfo) {

// After
    static final String MISSING_COLUMN_VALUE = "-";

                printOffsets(offsets, opts.options.has(opts.verboseOpt));
                            Optional.ofNullable(earliestResult.get(tp.getKey()))
        private void printOffsets(TreeMap<String, Entry<ShareGroupDescription, Collection<SharePartitionOffsetInformation>>> offsets, boolean verbose) {
                    String fmt = printOffsetFormat(groupId, offsetsInfo, verbose);

                    if (verbose) {
                        System.out.printf(fmt, "GROUP", "TOPIC", "PARTITION", "LEADER-EPOCH", "START-OFFSET");
                    } else {
                        System.out.printf(fmt, "GROUP", "TOPIC", "PARTITION", "START-OFFSET");
                    }
```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18761: complete listing of share group offsets [1/n] (#18894).

**Test**
Test modified in ShareGroupCommandTest.java covering the corrected behaviour.
