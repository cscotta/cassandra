**Commit**: b3eed51023 · **Ticket**: KAFKA-19660
**Subsystem**: streams/integration-tests
**Category**: Logic error in condition

**Root cause**
The original test timeout when using new protocol, because it use
`ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG` as the exception's timeout,
which is 300s. Also the test for new protocol and old protocol use the
same group ID, so the failure will be hidden. What I do:
1. Set the timeout as 5 secs so it can be captured within 10s
2.

**Before/After**
```java
// Before
        STREAMS_CONFIG.put(StreamsConfig.APPLICATION_ID_CONFIG, APP_ID);

// After
        STREAMS_CONFIG.put(StreamsConfig.consumerPrefix(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG), MISSING_TOPIC_DETECTION_TIMEOUT_MS);
    private static final int MISSING_TOPIC_DETECTION_TIMEOUT_MS = 5000;
        final String appId = APP_ID + "-" + (useNewProtocol ? "new" : "old");
        STREAMS_CONFIG.put(StreamsConfig.APPLICATION_ID_CONFIG, appId);
```

**How to spot**
When reviewing streams/integration-tests code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19660: joinwithincompletemetadataintegrationtest fails in isolated run of one parameter (#20483).

**Test**
Test modified in JoinWithIncompleteMetadataIntegrationTest.java covering the corrected behaviour.
