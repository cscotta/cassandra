**Commit**: c2f53a9760 · **Ticket**: KAFKA-17268
**Subsystem**: core/src
**Category**: Missing null / bounds check

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    CoreUtils.swallow(serverChannel.close(), this, Level.ERROR)

// After
    // The serverChannel will be null if Acceptor's thread is not started
    if (serverChannel != null) {
      CoreUtils.swallow(serverChannel.close(), this, Level.ERROR)
    }
```

**How to spot**
When reviewing core/src code, look for missing null / bounds check patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17268 fix npe when closing a non-started acceptor (#16818).

**Test**
No test added.
