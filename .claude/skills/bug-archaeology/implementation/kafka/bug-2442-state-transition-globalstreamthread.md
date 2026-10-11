**Commit**: 32f03a06be · **Ticket**: KAFKA-17474
**Subsystem**: streams/src
**Category**: Race condition / missing synchronization

**Root cause**
KAFKA-17100 changed the behavior of GlobalStreamThread introducing a race condition for state changes, that was exposed by failing (flaky) tests in GlobalStreamThreadTest. This PR moves the state transition to fix the race condition. Reviewers: Bill Bejeck <bbejeck@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        setState(RUNNING);

// After
            setState(RUNNING);
```

**How to spot**
When reviewing streams/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17474 fix state transition in globalstreamthread (#17078).

**Test**
No test added.
