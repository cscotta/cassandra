**Commit**: 78372b6883 · **Ticket**: KAFKA-17013
**Subsystem**: raft/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                "ConnectionState(node=%s, state=%s, lastSendTimeMs=%d, lastFailTimeMs=%d, inFlightCorrelationId=%d)",
                inFlightCorrelationId

// After
                "ConnectionState(node=%s, state=%s, lastSendTimeMs=%d, lastFailTimeMs=%d, inFlightCorrelationId=%s)",
                inFlightCorrelationId.isPresent() ? inFlightCorrelationId.getAsLong() : "undefined"
```

**How to spot**
When reviewing raft/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17013 requestmanager#connectionstate#tostring() should use %s (#16413).

**Test**
No test added.
