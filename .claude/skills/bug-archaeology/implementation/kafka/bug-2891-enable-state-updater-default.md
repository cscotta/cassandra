**Commit**: 39ffdea6d3 · **Ticket**: KAFKA-10199
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
We have already enabled the state updater by default once. However, we ran into issues that forced us to disable it again. We think that we fixed those issues. So we want to enable the
state updater again by default.

**Before/After**
```java
// Before
            return InternalConfig.getBoolean(configs, InternalConfig.STATE_UPDATER_ENABLED, false);
                    || !((boolean) properties.get(InternalConfig.STATE_UPDATER_ENABLED))) {
                assertEquals(Duration.ZERO, consumer.lastPollTimeout());
            } else {

// After
            return InternalConfig.getBoolean(configs, InternalConfig.STATE_UPDATER_ENABLED, true);
                    || (boolean) properties.get(InternalConfig.STATE_UPDATER_ENABLED)) {
            } else {
                assertEquals(Duration.ZERO, consumer.lastPollTimeout());
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-10199: enable state updater by default (#16107).

**Test**
Test modified in StoreChangelogReaderTest.java covering the corrected behaviour.
