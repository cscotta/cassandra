**Commit**: 2f2d9b0172 · **Ticket**: KAFKA-20309
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
When a share consumer is initially joining a group but not yet fetching
records, the poll loop has no records to wait for. Each iteration of the
loop was creating an instance of `SharePollEvent` without considering
whether an in-flight event existed. As a result, many events could
briefly queue up for no good reason, only to be drained once the
consumer successfully joined the group. The PR follows a similar design as used in the `AsyncKafkaConsumer` for
managing its poll event, without that code's extra complication of
validating the position.

**Before/After**
```java
// Before
            do {
                // Make sure the network thread can tell the application is actively polling
                applicationEventHandler.add(new SharePollEvent(timer.currentTimeMs()));

// After
    private SharePollEvent inFlightPoll;
            // This distinguishes the first pass of the inner do/while loop from subsequent passes for the
            // in-flight poll event logic.
            boolean firstPass = true;
            do {
                // Make sure the network thread can tell the application is actively polling
                checkInFlightPoll(timer, firstPass);
                firstPass = false;
    private void checkInFlightPoll(Timer timer, boolean firstPass) {
        if (firstPass && inFlightPoll != null) {
            maybeClearPreviousInFlightPoll();
        }
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20309: limit sharepollevent to single instance (#21757).

**Test**
Test modified in ApplicationEventProcessorTest.java covering the corrected behaviour.
