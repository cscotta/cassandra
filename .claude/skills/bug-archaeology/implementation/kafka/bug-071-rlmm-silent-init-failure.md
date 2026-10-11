**Commit**: e61c297b73 · **Ticket**: KAFKA-19425
**Subsystem**: storage/src/main/java/org/apache/kafka/server/log/remote/metadata/storage/TopicBasedRemoteLogMetadataManager.java
**Category**: State not cleaned up

**Root cause**
`TopicBasedRemoteLogMetadataManager.initializeResources()` stored initialization failure in a `volatile boolean initializationFailed` field. When other code later called `ensureInitializedAndNotClosed()`, it checked the flag and threw `FatalExitError`. However, if the broker was slow to start and `initializeResources()` failed, the RLMM simply logged the error and continued running without stopping the broker. As a result the broker operated without remote log metadata capability: local segments were never uploaded or deleted, causing the disk to grow indefinitely. The fix calls `Exit.exit(1)` immediately when initialization fails, stopping the broker rather than leaving it in a half-initialized state.

**Before/After**
```java
// Before: failure recorded in field; broker continued running silently
} catch (KafkaException e) {
    log.error("Encountered error while initializing topic-based RLMM resources", e);
    initializationFailed = true;
}
// Only throws FatalExitError when another method checks the flag

// After: immediate broker shutdown on failure
} finally {
    if (initializationFailed) {
        log.error("Stopping the server as it failed to initialize topic-based RLMM resources");
        Exit.exit(1);
    }
}
```

**How to spot**
When a critical subsystem fails to initialize and sets a "failed" flag instead of immediately failing fast, check whether callers consistently check the flag before every operation. If any code path can proceed without checking, the system runs in an undefined state. Prefer fail-fast exit or a clear exception propagation over deferred-failure flags for non-recoverable initialisation errors.

**Test**
`testInitializationFailure` registers a custom `Exit.setExitProcedure` that records the exit code, then configures the RLMM without bootstrap servers to force failure, and asserts that `exitCalled` becomes true with code 1.
