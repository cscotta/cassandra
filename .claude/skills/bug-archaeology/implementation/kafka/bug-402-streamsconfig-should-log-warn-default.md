**Commit**: ec3694e83d · **Ticket**: KAFKA-19939
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
For backward compatibility, we keep the processing exception handler
disabled on the global thread by default. StreamsConfig should WARN
about it, to encourage users to enable it. Reviewers: Lucas Brutschy <lbrutschy@confluent.io>, Arpit goyal
 <goyal.arpit.91@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
        if (!getBoolean(StreamsConfig.PROCESSING_EXCEPTION_HANDLER_GLOBAL_ENABLED_CONFIG)) {
            log.warn("Processing exception handler is not enabled for the GlobalThread. " +
                "It's recommended to set `" + StreamsConfig.PROCESSING_EXCEPTION_HANDLER_GLOBAL_ENABLED_CONFIG + "` to true to enable it. " +
                "Enabling the processing exception handler for global state/KTable processing now, ensures future backward compatibility. " +
                "The processing exception handler will get enabled by default with Apache Kafka 5.0 release.");
        }
    @Test
    @Test
    public void shouldLogWarningWhenProcessingExceptionHandlerIsNotEnabledOnGlobalThread() {
        try (LogCaptureAppender streamsConfigLogs = LogCaptureAppender.createAndRegister(StreamsConfig.class)) {
            streamsConfigLogs.setClassLogger(StreamsConfig.class, Level.WARN);
            streamsConfig = new StreamsConfig(props);
```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19939 streamsconfig should log warn by default (#21682).

**Test**
Test modified in StreamsConfigTest.java covering the corrected behaviour.
