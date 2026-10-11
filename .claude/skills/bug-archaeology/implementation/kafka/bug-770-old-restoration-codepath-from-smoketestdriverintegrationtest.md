**Commit**: f7593db287 · **Ticket**: KAFKA-19666
**Subsystem**: streams/integration-tests
**Category**: State not cleaned up

**Root cause**
clean up `SmokeTestDriverIntegrationTest`

Reviewers: Lucas Brutschy <lucasbru@apache.org>

**Before/After**
```java
// Before
        "false, false, true",
        "true, false, true",
        "false, false, false",
        "true, false, false",
        "true, true, true",
        "true, true, false"
        final boolean stateUpdaterEnabled,
        props.put(InternalConfig.STATE_UPDATER_ENABLED, stateUpdaterEnabled);

// After
        "false, true",
        "false, false",
        "true, true",
        "true, false"
```

**How to spot**
When reviewing streams/integration-tests code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19666: remove old restoration codepath from smoketestdriverintegrationtest [3/n] (#20465).

**Test**
Test modified in SmokeTestDriverIntegrationTest.java covering the corrected behaviour.
