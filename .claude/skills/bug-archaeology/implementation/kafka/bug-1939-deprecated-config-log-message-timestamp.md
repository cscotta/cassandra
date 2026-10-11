**Commit**: 700bdd5fee · **Ticket**: KAFKA-17997
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Divij Vaidya <diviv@amazon.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    /**
     * @deprecated since 3.6, removal planned in 4.0.
     * Use message.timestamp.before.max.ms and message.timestamp.after.max.ms instead
     */
    @Deprecated
    public static final String MESSAGE_TIMESTAMP_DIFFERENCE_MAX_MS_CONFIG = "message.timestamp.difference.max.ms";

    /**
     * @deprecated since 3.6, removal planned in 4.0.
     * Use message.timestamp.before.max.ms and message.timestamp.after.max.ms instead
     */
    @Deprecated

// After
  def logMessageTimestampBeforeMaxMs: Long = getLong(ServerLogConfigs.LOG_MESSAGE_TIMESTAMP_BEFORE_MAX_MS_CONFIG)
  def logMessageTimestampAfterMaxMs: Long = getLong(ServerLogConfigs.LOG_MESSAGE_TIMESTAMP_AFTER_MAX_MS_CONFIG)
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17997 remove deprecated config log.message.timestamp.difference.max.ms (#17928).

**Test**
Test modified in PlaintextProducerSendTest.scala covering the corrected behaviour.
