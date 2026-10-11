**Commit**: 9682e63c11 · **Ticket**: KAFKA-17109
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Reducing log messaging by removing stacktrace. Reviewer: Bruno Cadonna <cadonna@apache.org>

**Before/After**
```java
// Before
            log.info("Encountered lock exception. Reattempting locking the state in the next iteration.", lockException);

// After
            log.info("Encountered lock exception. Reattempting locking the state in the next iteration. Error message was: {}",
                     lockException.getMessage());
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17109: reduce log message load for failed locking (#16705).

**Test**
No test added.
