**Commit**: d170b52362 · **Ticket**: KAFKA-17635
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Kafka Streams actively purges records from repartition topics. Prior to this PR, Kafka Streams would retrieve the offset from the consumedOffsets map, but here are a couple of edge cases where the consumedOffsets can get ahead of the commitedOffsets map. In these cases, this means Kafka Streams will potentially purge a repartition record before it's committed. Updated the current StreamTask test to cover this case

Reviewers: Matthias Sax <mjsax@apache.org>

**Before/After**
```java
// Before
        for (final Map.Entry<TopicPartition, Long> entry : consumedOffsets.entrySet()) {
                purgeableConsumedOffsets.put(tp, entry.getValue() + 1);
    @Test
    public void shouldReturnOffsetsForRepartitionTopicsForPurging() {
        assertThat(map, equalTo(singletonMap(repartition, 11L)));

// After
        for (final Map.Entry<TopicPartition, Long> entry : committedOffsets.entrySet()) {
                // committedOffsets map is initialized at -1 so no purging until there's a committed offset
                if (entry.getValue() > -1) {
                    purgeableConsumedOffsets.put(tp, entry.getValue());
                }
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldMaybeReturnOffsetsForRepartitionTopicsForPurging(final boolean doCommit) {
        if (doCommit) {
            task.updateCommittedOffsets(repartition, 10L);
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17635: ensure only committed offsets are returned for purging (#17686).

**Test**
Test modified in StreamTaskTest.java covering the corrected behaviour.
