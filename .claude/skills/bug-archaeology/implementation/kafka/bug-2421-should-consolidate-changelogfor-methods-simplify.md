**Commit**: 64b909b598 · **Ticket**: KAFKA-13588
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    public static String changelogFor(final ProcessorContext context, final String storeName, final Boolean newChangelogTopic) {
        final String prefix = topicNamePrefix(context.appConfigs(), context.applicationId());
        if (context instanceof InternalProcessorContext && !newChangelogTopic) {
            final String changelogTopic = ((InternalProcessorContext) context).changelogFor(storeName);
            if (changelogTopic != null)
                return changelogTopic;

        }
        return ProcessorStateManager.storeChangelogTopic(prefix, storeName, context.taskId().topologyName());
    }

import org.apache.kafka.streams.processor.ProcessorContext;

// After
// (no additions)
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-13588 we should consolidate `changelogfor` methods to simplify the generation of internal topic names (#17125).

**Test**
No test added.
