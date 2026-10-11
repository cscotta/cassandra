**Commit**: 5fcbf3d3b1 · **Ticket**: KAFKA-18853
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This PR aims to add documentation to `alterLogLevelConfigs` method to
remind users to use valid LogLevelConfig constants. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
 * <p>
 * <b>Note for Broker Logger Configuration:</b><br>
 * When altering broker logger levels (using {@link org.apache.kafka.common.config.ConfigResource.Type#BROKER_LOGGER}),
 * it is strongly recommended to use log level constants from {@link org.apache.kafka.common.config.LogLevelConfig} instead of string literals.
 * This ensures compatibility with Kafka's log level validation and avoids potential configuration errors.
 * <p>
 * Example:
 * <pre>
 * Recommended approach:
 * new AlterConfigOp(new ConfigEntry(loggerName, LogLevelConfig.DEBUG_LOG_LEVEL), OpType.SET)
 *
 * Avoid this:
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18853 add documentation to remind users to use valid loglevelconfig constants (#20249).

**Test**
No test added.
