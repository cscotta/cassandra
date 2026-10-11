**Commit**: 735218837c · **Ticket**: KAFKA-13819
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Add application.server to Kafka Streams assignor logs. Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
        logPrefix = String.format("stream-thread [%s] ", streamsConfig.getString(CommonClientConfigs.CLIENT_ID_CONFIG));

// After
        final String appServerPrefix;
        final String applicationServerEndpoint = streamsConfig.getString(StreamsConfig.APPLICATION_SERVER_CONFIG);
        if (applicationServerEndpoint != null && !applicationServerEndpoint.isEmpty()) {
            appServerPrefix = String.format("app-server [%s] ", applicationServerEndpoint);
        } else {
            appServerPrefix = "";
        }
        logPrefix = String.format("stream-thread [%s] %s", streamsConfig.getString(CommonClientConfigs.CLIENT_ID_CONFIG), appServerPrefix);
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-13819: add application.server to streams assignor logs when set (#21605).

**Test**
No test added.
