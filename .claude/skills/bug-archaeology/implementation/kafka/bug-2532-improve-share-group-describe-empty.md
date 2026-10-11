**Commit**: f6bfa9406c · **Ticket**: KAFKA-17350
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
When you use kafka-share-groups.sh --describe for an empty group, it prints an empty table consisting of only the table header. kafka-consumer-groups.sh summarises the group status to make the output more informative and only prints the table if it contains more than zero rows. This PR applies this principle across all of the variants of describing share groups which makes the output much nicer where the output would otherwise be strangely empty. Reviewers:  Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
        private void describeGroups() throws ExecutionException, InterruptedException {
            if (offsets != null && !offsets.isEmpty()) {
            for (MemberDescription member : members) {
                maxConsumerIdLen = Math.max(maxConsumerIdLen, member.consumerId().length());
                maxHostLen = Math.max(maxHostLen, member.host().length());
                maxClientIdLen = Math.max(maxClientIdLen, member.clientId().length());
            }
            String fmt = "%" + -groupLen + "s %" + -maxConsumerIdLen + "s %" + -maxHostLen + "s %" + -maxClientIdLen + "s %s\n";
            System.out.printf(fmt, "GROUP", "CONSUMER-ID", "HOST", "CLIENT-ID", "ASSIGNMENT");
            for (MemberDescription member : members) {
                System.out.printf(fmt, description.groupId(), member.consumerId(), member.host(), member.clientId(),
                    member.assignment().topicPartitions().stream().map(part -> part.topic() + ":" + part.partition()).collect(Collectors.joining(",")));

// After
        /**
         * Prints a summary of the state for situations where the group is empty or dead.
         *
         * @return Whether the group detail should be printed
         */
        public static boolean maybePrintEmptyGroupState(String group, ShareGroupState state, int numRows) {
            if (state == ShareGroupState.DEAD) {
                printError("Share group '" + group + "' does not exist.", Optional.empty());
            } else if (state == ShareGroupState.EMPTY) {
                System.err.println("\nShare group '" + group + "' has no active members.");
            }

```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17350: improve share group describe for empty groups (#16897).

**Test**
Test modified in ShareGroupCommandTest.java covering the corrected behaviour.
