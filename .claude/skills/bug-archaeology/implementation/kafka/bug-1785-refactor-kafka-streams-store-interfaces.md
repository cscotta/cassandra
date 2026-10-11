**Commit**: b3b40bb77b · **Ticket**: KAFKA-13722
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Refactor Segments and TimestampedSegments to not use old
ProcessorContext any longer. Reviewers: Bruno Cadonna <bruno@confluent.io>

**Before/After**
```java
// Before
import org.apache.kafka.streams.processor.ProcessorContext;
                                      final ProcessorContext context,
    public void openExisting(final ProcessorContext context, final long streamTime) {
import org.apache.kafka.streams.processor.ProcessorContext;
                                              final ProcessorContext context) {
                                                    final ProcessorContext context,
    public void openExisting(final ProcessorContext context, final long streamTime) {
import org.apache.kafka.streams.processor.ProcessorContext;
 * {@link #segmentForTimestamp(long)}, {@link #getOrCreateSegment(long, ProcessorContext)},
 * {@link #getOrCreateSegmentIfLive(long, ProcessorContext, long)},

// After
import org.apache.kafka.streams.processor.StateStoreContext;
                                      final StateStoreContext context,
    public void openExisting(final StateStoreContext context, final long streamTime) {
import org.apache.kafka.streams.processor.StateStoreContext;
                                              final StateStoreContext context) {
                                                    final StateStoreContext context,
    public void openExisting(final StateStoreContext context, final long streamTime) {
import org.apache.kafka.streams.processor.StateStoreContext;
 * {@link #segmentForTimestamp(long)}, {@link #getOrCreateSegment(long, StateStoreContext)},
 * {@link #getOrCreateSegmentIfLive(long, StateStoreContext, long)},
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-13722: refactor kafka streams store interfaces (#18243).

**Test**
Test modified in KeyValueStoreTestDriver.java covering the corrected behaviour.
