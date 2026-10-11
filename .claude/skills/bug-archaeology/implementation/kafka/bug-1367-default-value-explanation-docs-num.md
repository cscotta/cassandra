**Commit**: 2ccc73783e · **Ticket**: KAFKA-18868
**Subsystem**: server/src
**Category**: Logic error in condition

**Root cause**
Update the documentation for the `num.replica.alter.log.dirs.threads` configuration to clarify that its default value is determined by the number of log directories specified in the `log.dirs` configuration. Reviewers: Ken Huang <s7133700@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    public static final String NUM_REPLICA_ALTER_LOG_DIRS_THREADS_DOC = "The number of threads that can move replicas between log directories, which may include disk I/O";

// After
    public static final String NUM_REPLICA_ALTER_LOG_DIRS_THREADS_DOC = "The number of threads that can move replicas between log directories, which may include disk I/O. " +
            "The default value is equal to the number of directories specified in the <code>" + ServerLogConfigs.LOG_DIR_CONFIG + "</code> or <code>" + ServerLogConfigs.LOG_DIRS_CONFIG + "</code> configuration property.";
```

**How to spot**
When reviewing server/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18868 add the "default value" explanation to the docs of num.replica.alter.log.dirs.threads (#19038).

**Test**
No test added.
