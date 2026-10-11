**Commit**: 2a2626b3d8 · **Ticket**: KAFKA-19244
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
This PR implements all the options for `--delete --group grpId` and
`--delete --all-groups`

Tests:  Integration tests and unit tests. Reviewers: Lucas Brutschy <lbrutschy@confluent.io>, Andrew Schofield
 <aschofield@confluent.io>

**Before/After**
```java
// Before
            long numberOfActions = Stream.of(opts.listOpt, opts.describeOpt).filter(opts.options::has).count();
                CommandLineUtils.printUsageAndExit(opts.parser, "Command must include exactly one action: --list, or --describe.");

// After
import org.apache.kafka.clients.admin.AbstractOptions;
import org.apache.kafka.clients.admin.DeleteStreamsGroupsOptions;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.errors.GroupNotEmptyException;
import org.apache.kafka.common.errors.UnsupportedVersionException;
import java.util.LinkedHashSet;
            long numberOfActions = Stream.of(opts.listOpt, opts.describeOpt, opts.deleteOpt).filter(opts.options::has).count();
                CommandLineUtils.printUsageAndExit(opts.parser, "Command must include exactly one action: --list, --describe, or --delete.");
            } else if (opts.options.has(opts.deleteOpt)) {
                streamsGroupService.deleteGroups();

```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19244: add support for kafka-streams-groups.sh options (delete all groups) [2/n]  (#19758).

**Test**
Test modified in DeleteStreamsGroupTest.java covering the corrected behaviour.
