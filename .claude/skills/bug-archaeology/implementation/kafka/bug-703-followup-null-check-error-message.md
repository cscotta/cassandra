**Commit**: b1b330e664 · **Ticket**: KAFKA-18193
**Subsystem**: streams/src
**Category**: Missing null / bounds check

**Root cause**
This PR is a follow-up to KAFKA-18193. It addresses the need for a null
check and an improved error message. Please refer to the previous
comments and the review of
https://github.com/apache/kafka/pull/19955#pullrequestreview-3310028108
for more context. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    private boolean close(final Optional<Long> timeout, final org.apache.kafka.streams.CloseOptions.GroupMembershipOperation operation) {
        final String msgPrefix = prepareMillisCheckFailMsgPrefix(optionsInternal.timeout(), "timeout");
        final long timeoutMs = validateMillisecondDuration(optionsInternal.timeout().get(), msgPrefix);

// After
    // visible for testing
    boolean close(final Optional<Long> timeout, final org.apache.kafka.streams.CloseOptions.GroupMembershipOperation operation) {
        final Duration timeout = optionsInternal.timeout().orElseGet(() -> Duration.ofMillis(Long.MAX_VALUE));
        final String msgPrefix = prepareMillisCheckFailMsgPrefix(timeout, "timeout");
        final long timeoutMs = validateMillisecondDuration(timeout, msgPrefix);
import static org.mockito.Mockito.doReturn;
    @Test
    @SuppressWarnings("unchecked")
    public void shouldUseDefaultTimeoutForCloseWithNullTimeout() throws Exception {
        prepareStreams();
        prepareStreamThread(streamThreadOne, 1);
        prepareStreamThread(streamThreadTwo, 2);
```

**How to spot**
When reviewing streams/src code, look for missing null / bounds check patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: minor: followup kafka-18193 for null check and error message (#20650).

**Test**
Test modified in KafkaStreamsTest.java covering the corrected behaviour.
