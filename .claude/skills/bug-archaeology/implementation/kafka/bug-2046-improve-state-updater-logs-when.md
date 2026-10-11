**Commit**: c8f360c5f5 · **Ticket**: KAFKA-17744
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
The logs for Kafka Streams local state restoration incorrectly refer to the StreamThread instead of the StateUpdater thread, which is responsible for decoupling the restoration process. The restore consumer also references StreamThread instead of StateUpdater. This commit corrects the log message for more clarity. Reviewer: Bruno Cadonna <cadonna@apache.org>

**Before/After**
```java
// Before
        final Map<String, Object> restoreConsumerConfigs = config.getRestoreConsumerConfigs(restoreConsumerClientId(threadId));
            logContext,
        final boolean stateUpdaterEnabled = InternalConfig.stateUpdaterEnabled(config.originals());
        final String stateUpdaterId = threadId.replace(THREAD_ID_SUBSTRING, STATE_UPDATER_ID_SUBSTRING);

// After
        final boolean stateUpdaterEnabled = InternalConfig.stateUpdaterEnabled(config.originals());

        final String stateUpdaterId = threadId.replace(THREAD_ID_SUBSTRING, STATE_UPDATER_ID_SUBSTRING);
        final String restorationThreadId = stateUpdaterEnabled ? stateUpdaterId : threadId;
        final LogContext restorationLogContext = stateUpdaterEnabled ? new LogContext(String.format("state-updater [%s] ", restorationThreadId)) : logContext;
        final Map<String, Object> restoreConsumerConfigs = config.getRestoreConsumerConfigs(restoreConsumerClientId(restorationThreadId));
            restorationLogContext,
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17744: improve state updater logs when restorating state (#17714).

**Test**
No test added.
