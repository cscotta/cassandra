**Commit**: 3623726c76 · **Ticket**: KAFKA-18286
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
Add support for streams groups in kafka-groups.sh. The change adds command-line options `--streams` to list only streams
groups, and value `--group-type streams`. Those two options are mutually
exclusive with other group type and protocol filters specified on the
command line. Includes a small integration test that spins up a kafka streams
application and lists the group.

**Before/After**
```java
// Before
            printGroupDetails(resources, opts.groupType(), opts.protocol(), opts.hasConsumerOption(), opts.hasShareOption());
                                       boolean shareGroupFilter) {
                if (combinedFilter(group, groupTypeFilter, protocolFilter, consumerGroupFilter, shareGroupFilter)) {
                                       boolean shareGroupFilter) {
                            + "Valid types are: 'classic', 'consumer' and 'share'.")
            CommandLineUtils.checkInvalidArgs(parser, options, consumerOpt, groupTypeOpt, protocolOpt, shareOpt);
            CommandLineUtils.checkInvalidArgs(parser, options, shareOpt, consumerOpt, groupTypeOpt, protocolOpt);

// After
            printGroupDetails(resources, opts.groupType(), opts.protocol(), opts.hasConsumerOption(), opts.hasShareOption(), opts.hasStreamsOption());
                                       boolean shareGroupFilter,
                                       boolean streamsGroupFilter) {
                if (combinedFilter(group, groupTypeFilter, protocolFilter, consumerGroupFilter, shareGroupFilter, streamsGroupFilter)) {
                                       boolean shareGroupFilter,
                                       boolean streamsGroupFilter) {
            } else if (streamsGroupFilter) {
                pass = groupType.filter(gt -> gt == GroupType.STREAMS).isPresent();
        private final OptionSpecBuilder streamsOpt;

                            + "Valid types are: 'classic', 'consumer', 'share' and 'streams'.")
            streamsOpt = parser.accepts("streams", "Filter the groups to show streams groups.");
```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18286: implement support for streams groups in kafka-groups.sh (#19423).

**Test**
Test modified in GroupsCommandTest.java covering the corrected behaviour.
