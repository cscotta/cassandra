**Commit**: 5cf9872e8f · **Ticket**: KAFKA-18017
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        if (!coordinatorRequestManager.coordinator().isPresent() ||
                membershipManager().shouldSkipHeartbeat()) {
        membershipManager().onHeartbeatFailure(exception instanceof RetriableException);
        membershipManager().onHeartbeatFailure(false);

// After
        if (coordinatorRequestManager.coordinator().isEmpty() || membershipManager().shouldSkipHeartbeat()) {
        // Notify the group manager about the failure after all errors have been handled and propagated.
        membershipManager().onHeartbeatFailure(exception instanceof RetriableException);

        // Notify the group manager about the failure after all errors have been handled and propagated.
        membershipManager().onHeartbeatFailure(false);
import org.apache.kafka.common.errors.AuthenticationException;
import org.mockito.InOrder;
import static org.mockito.Mockito.inOrder;
    @Test
    public void testHeartbeatResponseErrorNotifiedToGroupManagerAfterErrorPropagated() {
        time.sleep(DEFAULT_HEARTBEAT_INTERVAL_MS);
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18017: fix call order for hb error and group manager (#17805).

**Test**
Test modified in ConsumerHeartbeatRequestManagerTest.java covering the corrected behaviour.
