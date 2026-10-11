**Commit**: 41beee508e · **Ticket**: KAFKA-16558
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Kirk True <kirk@kirktrue.pro>, Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
        @Override
        public String toStringBase() {
            return super.toStringBase() +
                    ", remainingMs=" + heartbeatTimer.remainingMs() +
                    ", heartbeatIntervalMs=" + heartbeatIntervalMs;
        }

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
    @Test
    public void testHeartBeatRequestStateToStringBase() {
        long retryBackoffMs = 100;
        long retryBackoffMaxMs = 1000;
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16558: implemented heartbeatrequeststate tostringbase() and added a test for it (#16373).

**Test**
Test modified in HeartbeatRequestManagerTest.java covering the corrected behaviour.
