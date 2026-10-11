**Commit**: d07aa37412 · **Ticket**: KAFKA-19386
**Subsystem**: server-common/src
**Category**: Logic error in condition

**Root cause**
The PR: https://github.com/apache/kafka/pull/17636 migrated
DelayedOperationPurgatory from scala to java, and instatiated
`expirationReaper` at instance level where `purgatoryName` is still
`null` hence all expiration threads from different Purgatories has
incorrect names. <img width="216" alt="Screenshot 2025-06-07 at 01 28 58"

src="https://github.com/user-attachments/assets/fd1b8137-b290-42e0-9a95-258fde5737d2"
/>

The PR fixes the instatiation of ExpirationReaper, in constructor when
`purgatoryName` is defined. <img width="296" alt="Screenshot 2025-06-07 at 01 31 27"

src="https://github.com/user-attachments/assets/9912311b-ddf6-4554-8e04-d0b8ad208abc"
/>

This issue affects 4.0 version ...

**Before/After**
```java
// Before
    private final ExpiredOperationReaper expirationReaper = new ExpiredOperationReaper();

// After
    private final ExpiredOperationReaper expirationReaper;
        // The initialization of the expiration reaper thread is done after the brokerId and purgatoryName
        // are set, so that the thread name can include these values.
        this.expirationReaper = new ExpiredOperationReaper();
```

**How to spot**
When reviewing server-common/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19386: correcting expirationreaper thread names from purgatory (#19918).

**Test**
No test added.
