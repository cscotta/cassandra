**Commit**: 68f1da8474 · **Ticket**: KAFKA-18185
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
JIRA: KAFKA-18185

This is a follow-up of #17614   The patch is to remove the
`internal.leave.group.on.close` config. Reviewers: Sophie Blee-Goldman <ableegoldman@gmail.com>, Chia-Ping Tsai
 <chia7712@gmail.com>, Bill Bejeck <bbejeck@apache.org>

**Before/After**
```java
// Before
    public final boolean leaveGroupOnClose;

        // Internal leave group config is only defined in Consumer.
        if (protocolType == ProtocolType.CONSUMER) {
            this.leaveGroupOnClose = config.getBoolean("internal.leave.group.on.close");
        } else {
            this.leaveGroupOnClose = true;
        }
                                long retryBackoffMaxMs,
                                boolean leaveGroupOnClose) {
        this.leaveGroupOnClose = leaveGroupOnClose;
    /**

// After
                                long retryBackoffMaxMs) {
        if (shouldSendLeaveGroupRequest(membershipOperation)) {
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18185: remove internal.leave.group.on.close config (#19400).

**Test**
Test modified in AbstractCoordinatorTest.java covering the corrected behaviour.
