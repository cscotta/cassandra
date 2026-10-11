**Commit**: 55d557cebb · **Ticket**: KAFKA-19942

**Subsystem**: streams/src/main/java/org/apache/kafka/streams/StreamsConfig.java

**Category**: Wrong constant/default value

**Root cause**
The `StateUpdater` in Kafka Streams was guarded by an internal config flag `__state.updater.enabled__` that defaulted to `true` but could still be disabled by setting the flag to `false`. Because the `StateUpdater` is now always required by the threading model introduced in recent Kafka Streams versions, the ability to disable it was an inconsistency that caused incorrect behaviour when disabled. The fix hard-codes the return value of `stateUpdaterEnabled` to `true` and removes all test paths that set the flag to `false`.

**Before**
```java
// Before
public static boolean stateUpdaterEnabled(final Map<String, Object> configs) {
    return InternalConfig.getBoolean(configs,
        InternalConfig.STATE_UPDATER_ENABLED, true);
}
```

**After**
```java
// After
public static boolean stateUpdaterEnabled(final Map<String, Object> configs) {
    return true; // always enabled
}
```

**How to spot**
Any internal boolean config that defaults to `true` but is never intentionally set to `false` in production code is a candidate for hard-coding; the presence of test-only paths that disable the flag suggests the flag no longer serves a real purpose.

**Test**
Remove the `InternalConfig.STATE_UPDATER_ENABLED` system property from all test setups and run the full Streams test suite to confirm no test relies on the disabled path.
