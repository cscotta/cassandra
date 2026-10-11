**Commit**: b5f8aaf2ac · **Ticket**: KAFKA-18288
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
Implement `--describe` and its options: (`--state`, -`-offset`,
`--members` and the combination of them with `--verbose`) as described
in `KIP-1071`. Reviewers: Lucas Brutschy <lbrutschy@confluent.io>, PoAn Yang
<payang@apache.org>

**Before/After**
```java
// Before
            long numberOfActions = Stream.of(opts.listOpt).filter(opts.options::has).count();
                CommandLineUtils.printUsageAndExit(opts.parser, "Command must include exactly one action: --list.");
                    validStates.stream().map(GroupState::toString).collect(Collectors.joining(", ")));

// After
import org.apache.kafka.clients.admin.DescribeStreamsGroupsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsSpec;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.StreamsGroupDescription;
import org.apache.kafka.clients.admin.StreamsGroupMemberAssignment;
import org.apache.kafka.clients.admin.StreamsGroupMemberDescription;
import org.apache.kafka.clients.admin.StreamsGroupSubtopologyDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import java.util.HashMap;
import java.util.HashSet;
```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18288: add support kafka-streams-groups.sh --describe (#19433).

**Test**
Test modified in DescribeStreamsGroupTest.java covering the corrected behaviour.
