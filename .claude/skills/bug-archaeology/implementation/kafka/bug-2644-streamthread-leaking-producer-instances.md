**Commit**: fd2cd046f8 · **Ticket**: KAFKA-17203
**Subsystem**: streams/src
**Category**: Missing null / bounds check

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            task.suspend();
            task.closeClean();

// After
            closeTaskClean(task, Collections.emptySet(), Collections.emptyMap());
```

**How to spot**
When reviewing streams/src code, look for missing null / bounds check patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17203 streamthread leaking producer instances (#16730).

**Test**
No test added.
