**Commit**: 9eb05fc729 · **Ticket**: KAFKA-16223
**Subsystem**: connect/runtime
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before


// After
import java.util.HashSet;
    private static final Struct TARGET_STATE_STARTED = new Struct(KafkaConfigBackingStore.TARGET_STATE_V0).put("state", "STARTED");
    @Test
    public void testPutTaskConfigsZeroTasks() throws Exception {
        configStorage.setupAndCreateKafkaBasedLog(TOPIC, config);
        verifyConfigure();
        configStorage.start();
        verify(configLog).start();

        // Records to be read by consumer as it reads to the end of the log
        doAnswer(expectReadToEnd(new LinkedHashMap<>())).
                doAnswer(expectReadToEnd(Collections.singletonMap(COMMIT_TASKS_CONFIG_KEYS.get(0), CONFIGS_SERIALIZED.get(0))))
```

**How to spot**
When reviewing connect/runtime code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular:  kafka-16223 replace easymock/powermock with mockito for kafkaconfigbackingstoretest (#16164).

**Test**
Test modified in KafkaConfigBackingStoreMockitoTest.java covering the corrected behaviour.
