**Commit**: 106fd5f601 · **Ticket**: KAFKA-18354
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
    private final List<LogLevelChange> logLevelChanges = new LinkedList<>();

    public static class LogLevelChange {
        private final Level originalLevel;
        private final Class<?> clazz;

        public LogLevelChange(final Level originalLevel, final Class<?> clazz) {
            this.originalLevel = originalLevel;
            this.clazz = clazz;

// After
import org.apache.logging.log4j.core.config.Configurator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
    private final Map<Class<?>, Level> logLevelChanges = new HashMap<>();

    public LogCaptureAppender() {
        super("LogCaptureAppender-" + UUID.randomUUID(), null, null, true, Property.EMPTY_ARRAY);
        final LogCaptureAppender logCaptureAppender = new LogCaptureAppender();
        final LogCaptureAppender logCaptureAppender = new LogCaptureAppender();
        if (!logLevelChanges.containsKey(clazz)) {
            Level currentLevel = LogManager.getLogger(clazz).getLevel();
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18354 use log4j2 apis to refactor logcaptureappender (#18338).

**Test**
Test modified in LogCaptureAppender.java covering the corrected behaviour.
