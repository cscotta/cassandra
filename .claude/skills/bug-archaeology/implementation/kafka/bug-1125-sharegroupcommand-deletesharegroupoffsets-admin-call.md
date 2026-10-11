**Commit**: fa17bd44b3 · **Ticket**: KAFKA-16718
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
This PR is the last in series to implement the DeleteShareGroupOffsets
request. This PR includes the changes in ShareGroupCommand which
internally calls the admin api to delete the offsets. Now, any enduser
will be able to delete share group offsets for topics subscribed by a
share group using kafka-share-groups.sh --delete-offsets command. Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
                throw new UnsupportedOperationException("--delete-offsets option is not yet implemented");

// After
import org.apache.kafka.clients.admin.DeleteShareGroupOffsetsOptions;
import org.apache.kafka.clients.admin.DeleteShareGroupOffsetsResult;
import org.apache.kafka.common.protocol.Errors;
                shareGroupService.deleteOffsets();
        void deleteOffsets() {
            String groupId = opts.options.valueOf(opts.groupOpt);
            List<String> topics = opts.options.valuesOf(opts.topicOpt);

            Entry<Throwable, Map<String, Throwable>> res = sendDeleteShareGroupOffsetsRequest(groupId, new HashSet<>(topics));

            Throwable topLevelResult = res.getKey();
            Map<String, Throwable> topicLevelResult = res.getValue();
```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16718-4/n: sharegroupcommand changes for deletesharegroupoffsets admin call (#19587).

**Test**
Test modified in ShareGroupCommandTest.java covering the corrected behaviour.
