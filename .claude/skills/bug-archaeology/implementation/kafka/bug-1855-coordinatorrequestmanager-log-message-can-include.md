**Commit**: d09e222846 · **Ticket**: KAFKA-18189
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Fixed logic in markCoordinatorUnknown to ensure the warning log contains the correct number of milliseconds the client has been disconnected. Reviewers: Christo Lolov <lolovc@amazon.com>

**Before/After**
```java
// Before
     * Mark the current coordinator null.
     * @param cause         why the coordinator is marked unknown.
     * @param currentTimeMs the current time in ms.
        if (this.coordinator != null) {
            log.info("Group coordinator {} is unavailable or invalid due to cause: {}. "
                    + "Rediscovery will be attempted.", this.coordinator, cause);
            this.coordinator = null;
            if (currDisconnectMin > this.totalDisconnectedMin) {

// After
     * Mark the coordinator as "unknown" (i.e. {@code null}) when a disconnect is detected. This detection can occur
     * in one of two paths:
     * <ol>
     *     <li>The coordinator was discovered, but then later disconnected</li>
     *     <li>The coordinator has not yet been discovered and/or connected</li>
     * </ol>
     *
     * @param cause         String explanation of why the coordinator is marked unknown
     * @param currentTimeMs Current time in milliseconds
        if (coordinator != null || timeMarkedUnknownMs == -1) {
        }

```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18189: coordinatorrequestmanager log message can include incorrect coordinator disconnect time (#18109).

**Test**
Test modified in CoordinatorRequestManagerTest.java covering the corrected behaviour.
