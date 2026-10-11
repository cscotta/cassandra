**Commit**: 0412be9e9d · **Ticket**: KAFKA-19641
**Subsystem**: streams/integration-tests
**Category**: Logic error in condition

**Root cause**
What I observed is that if I run both combinations useNewProtocol=true,
useNewProtocol=false it would often fail the second time, but if I only
run the second variation useNewProtocol=false it works, and only the
first variation useNewProtocol=true also works. So this points to some
state that is not cleared between the tests  - and indeed, the test
creates a topic “inputTopic”, produces to it, but doesn’t delete it, so
the second variation will run with produce to it again and then run with
twice the data. I also reduced heartbeat interval and session timeout since some of the
tests need to wait for the old consumer to leave which (sigh) Kafka
Streams doesn't do, so we have to wait that it ...

**Before/After**
```java
// Before
// (no removals)

// After
        streamsConfiguration.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 500);
        streamsConfiguration.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 1000);
        CLUSTER.deleteAllTopics();
```

**How to spot**
When reviewing streams/integration-tests code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19641: fix flaky restoreintegrationtest#shouldinvokeuserdefinedglobalstaterestorelistener (#20419).

**Test**
Test modified in RestoreIntegrationTest.java covering the corrected behaviour.
