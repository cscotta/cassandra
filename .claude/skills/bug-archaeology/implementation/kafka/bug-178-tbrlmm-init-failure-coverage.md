**Commit**: b9d5597b44 · **Ticket**: KAFKA-17830
**Subsystem**: storage/src/main/java/org/apache/kafka/server/log/remote/metadata/storage/TopicBasedRemoteLogMetadataManager.java
**Category**: Missing null / bounds check

**Root cause**: `TopicBasedRemoteLogMetadataManager` (TBRLMM) had an `initializationFailed` flag that was checked in `ensureInitializedAndNotClosed()`, but the flag had package-private visibility with no accessor, so external test code could not observe it. This made it impossible to write unit tests verifying broker shutdown is triggered correctly on admin-client initialization failures. A trivial visibility accessor was added to enable coverage of those paths.

**Before/After**:
```java
// BEFORE – flag only readable inside the class; no way to assert it externally
private boolean initializationFailed;

// AFTER – package-visible accessor added for testing
boolean isInitializationFailed() {
    return initializationFailed;
}
```

**How to spot**: When a private boolean sentinel is the sole observable effect of an error path, that path is untestable. Look for `private boolean` flags used in `if` guards without any public/package accessor; they are dead code from a test-coverage perspective.

**Test**: `TopicBasedRemoteLogMetadataManagerTest.testInitializationFailure` creates a `TopicBasedRemoteLogMetadataManager`, configures it without bootstrap servers, and calls `TestUtils.waitForCondition(rlmm::isInitializationFailed, ...)` to assert that initialization failure is detected asynchronously.
