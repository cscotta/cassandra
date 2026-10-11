**Commit**: defcbb51ee · **Ticket**: KAFKA-17082
**Subsystem**: checkstyle/import-control.xml
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        Event(final String level, final String message, final Optional<String> throwableInfo) {
    public void setClassLoggerToDebug(final Class<?> clazz) {
        logLevelChanges.add(new LogLevelChange(Logger.getLogger(clazz).getLevel(), clazz));
        Logger.getLogger(clazz).setLevel(Level.DEBUG);
    }

    public void setClassLoggerToTrace(final Class<?> clazz) {
        Logger.getLogger(clazz).setLevel(Level.TRACE);
                result.add(new Event(event.getLevel().toString(), event.getRenderedMessage(), throwableString));
            connectorLogs.setClassLoggerToTrace(MirrorSourceConnector.class);

// After
    <allow pkg="org.apache.log4j"/>
    <allow pkg="org.apache.log4j" />
        private final Optional<String> throwableClassName;
        Event(final String level, final String message, final Optional<String> throwableInfo, final Optional<String> throwableClassName) {
            this.throwableClassName = throwableClassName;

        public Optional<String> getThrowableClassName() {
            return throwableClassName;
        }
    public void setClassLogger(final Class<?> clazz, Level level) {
        Logger.getLogger(clazz).setLevel(level);
                final Optional<String> throwableClassName;
```

**How to spot**
When reviewing checkstyle/import-control.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17082 replace kafka.utils.logcaptureappender with org.apache.kafka.common.utils.logcaptureappender (#16601).

**Test**
Test modified in LogCaptureAppender.java covering the corrected behaviour.
