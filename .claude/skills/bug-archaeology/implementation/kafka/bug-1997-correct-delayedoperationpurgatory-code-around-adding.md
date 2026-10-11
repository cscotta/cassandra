**Commit**: aa7a3dbd30 · **Ticket**: KAFKA-18027
**Subsystem**: server-common/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>, Jun Rao <junrao@gmail.com>

**Before/After**
```java
// Before
            watchKeys.forEach(key -> watchForOperation(key, operation));

// After
            watchKeys.forEach(key -> {
                if (!operation.isCompleted())
                    watchForOperation(key, operation);
            });
```

**How to spot**
When reviewing server-common/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18027: minor: correct delayedoperationpurgatory code around adding of an already completed operation (#17842).

**Test**
No test added.
