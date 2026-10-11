**Commit**: dc396f47e8 · **Ticket**: KAFKA-17162
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Lucas Brutschy <lbrutschy@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
        awaitingThread.join();
        awaitingThread.join();
        awaitingThread.join();
        awaitingThread.join();
        awaitingThread.join();
        awaitingThread.join();
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17162: join() started thread in defaulttaskmanagertest (#18570).

**Test**
Test modified in DefaultTaskManagerTest.java covering the corrected behaviour.
