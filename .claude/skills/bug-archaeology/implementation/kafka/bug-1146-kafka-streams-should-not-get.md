**Commit**: ee4debb9f0 · **Ticket**: KAFKA-19128
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
Kafka Streams calls `prepareCommit()` in `Taskmanager#closeTaskDirty()`. However, the dirty task must not get committed and therefore,
prepare-commit tasks such as getting offsets should not be needed as
well. The only thing needed before closing a task dirty is flushing. Therefore, separating `flush` and `prepareCommit` could be a good fix.

**Before/After**
```java
// Before
    public Map<TopicPartition, OffsetAndMetadata> prepareCommit() {
    public Map<TopicPartition, OffsetAndMetadata> prepareCommit() {
                log.debug("Prepared {} task for committing", state());
        return Collections.emptyMap();

    public Map<TopicPartition, OffsetAndMetadata> prepareCommit() {
    Map<TopicPartition, OffsetAndMetadata> prepareCommit();

// After
    public Map<TopicPartition, OffsetAndMetadata> prepareCommit(final boolean clean) {
    public Map<TopicPartition, OffsetAndMetadata> prepareCommit(final boolean clean) {
                if (!clean) {
                    log.debug("Skipped preparing {} standby task with id {} for commit since the task is getting closed dirty.", state(), id);
                } else {
                    log.debug("Prepared {} task for committing", state());
                }
        return clean ? Collections.emptyMap() : null;
    public Map<TopicPartition, OffsetAndMetadata> prepareCommit(final boolean clean) {
                    if (!clean) {
                        log.debug("Skipped preparing {} task with id {} for commit since the task is getting closed dirty.", state(), id);
                        return null;
```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19128: kafka streams should not get offsets when close dirty (#19450).

**Test**
Test modified in StandbyTaskTest.java covering the corrected behaviour.
