**Commit**: 2b589a451a · **Ticket**: KAFKA-19244
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
This PR implements 
`--delete-offsets --all-input-topics`
`--delete-offsets --input-topic String: topicName`

Testing: integration test

Reviewers: Lucas Brutschy <lbrutschy@confluent.io>

**Before/After**
```java
// Before
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsSpec;
            long numberOfActions = Stream.of(opts.listOpt, opts.describeOpt, opts.deleteOpt).filter(opts.options::has).count();
                CommandLineUtils.printUsageAndExit(opts.parser, "Command must include exactly one action: --list, --describe, or --delete.");

// After
import org.apache.kafka.clients.admin.DeleteStreamsGroupOffsetsOptions;
import org.apache.kafka.clients.admin.DeleteStreamsGroupOffsetsResult;
import org.apache.kafka.clients.admin.DescribeTopicsOptions;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.ListStreamsGroupOffsetsSpec;
import org.apache.kafka.common.protocol.Errors;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.Iterator;
import java.util.function.ToIntFunction;
    static final String MISSING_COLUMN_VALUE = "-";

```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19244: add support for kafka-streams-groups.sh options (delete offsets) [4/n] (#19895).

**Test**
Test modified in DeleteStreamsGroupOffsetTest.java covering the corrected behaviour.
