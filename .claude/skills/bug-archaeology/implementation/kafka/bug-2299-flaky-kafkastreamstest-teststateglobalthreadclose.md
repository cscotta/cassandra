**Commit**: 4c90d3518b · **Ticket**: KAFKA-17646
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            globalStreamThread.join();
            waitForCondition(
                () -> streams.state() == KafkaStreams.State.PENDING_ERROR,
                "Thread never stopped."
            );
            streams.close();

            assertThat(appender.getMessages(), hasItem(containsString("ERROR")));

// After
            streams.close();
            assertEquals(streams.state(), KafkaStreams.State.ERROR, "KafkaStreams should remain in ERROR state after close.");
            assertThat(appender.getMessages(), hasItem(containsString("State transition from RUNNING to PENDING_ERROR")));
            assertThat(appender.getMessages(), hasItem(containsString("State transition from PENDING_ERROR to ERROR")));
            assertThat(appender.getMessages(), hasItem(containsString("Streams client is already in the terminal ERROR state")));
```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17646 fix flaky kafkastreamstest.teststateglobalthreadclose (#17310).

**Test**
Test modified in KafkaStreamsTest.java covering the corrected behaviour.
