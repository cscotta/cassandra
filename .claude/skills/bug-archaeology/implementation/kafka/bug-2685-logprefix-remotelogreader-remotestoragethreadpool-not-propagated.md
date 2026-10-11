**Commit**: 7a8f89e0a0 · **Ticket**: KAFKA-17168
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Kamal Chandraprakash<kamal.chandraprakash@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        logger = new LogContext() {
            @Override
            public String logPrefix() {
                return "[" + Thread.currentThread().getName() + "]";
            }
        }.logger(RemoteLogReader.class);
        logger = new LogContext() {
            @Override
            public String logPrefix() {
                return "[" + Thread.currentThread().getName() + "]";
            }
        }.logger(RemoteStorageThreadPool.class);

// After
        logger = new LogContext("[" + Thread.currentThread().getName() + "]").logger(RemoteLogReader.class);
        logger = new LogContext("[" + Thread.currentThread().getName() + "]").logger(RemoteStorageThreadPool.class);
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17168 the logprefix of remotelogreader/remotestoragethreadpool is not propagated correctly (#16642).

**Test**
Test modified in RemoteStorageThreadPool.java covering the corrected behaviour.
