**Commit**: 96527be90d · **Ticket**: KAFKA-18243
**Subsystem**: connect/runtime
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        final LinkedList<String> result = new LinkedList<>();
import org.apache.logging.log4j.core.config.LoggerConfig;
     * The root logger's name is changed in log4j2 to empty string (see: [[LogManager.ROOT_LOGGER_NAME]]) but for backward-
     * compatibility. Kafka keeps its original root logger name. It is why here is a dedicated definition for the root logger name.
    private static final String ROOT_LOGGER_NAME = "root";
        if (ROOT_LOGGER_NAME.equalsIgnoreCase(logger)) {
            List<org.apache.logging.log4j.Logger> currentLoggers = currentLoggers();
        Map<String, LoggerLevel> result = new TreeMap<>();

        currentLoggers().stream()
                .filter(logger -> !logger.getLevel().equals(Level.OFF))
                .forEach(logger -> result.put(logger.getName(), loggerLevel(logger)));

// After
        final List<String> result = new LinkedList<>();
    private static final String ROOT_LOGGER_NAME = "root";

     * While log4j2 changed the root logger's name to empty string (see: [[LogManager.ROOT_LOGGER_NAME]]),
     * for backward-compatibility purposes, we accept both empty string and "root" as valid root logger names.
     * This is why we have a dedicated definition that includes both values.
    private static final List<String> VALID_ROOT_LOGGER_NAMES = List.of(LogManager.ROOT_LOGGER_NAME, ROOT_LOGGER_NAME);

    /**
     * Maps logger names to their last modification timestamps.
     * Note: The logger name "root" refers to the actual root logger of log4j2.
     */
```

**How to spot**
When reviewing connect/runtime code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18243 fix compatibility of loggers class between log4j and log4j2 (#18185).

**Test**
Test modified in LogCaptureAppender.java covering the corrected behaviour.
