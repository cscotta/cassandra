**Commit**: 84ab3b9a5c · **Ticket**: KAFKA-17031
**Subsystem**: storage/src
**Category**: Logic error in condition

**Root cause**
According to KIP-950, remote.log.manager.thread.pool.size should be marked as deprecated and replaced by two new configurations: remote.log.manager.copier.thread.pool.size and remote.log.manager.expiration.thread.pool.size. Fix default handling so that -1 works as expected. Reviewers: Luke Chen <showuon@gmail.com>, Gaurav Narula <gaurav_narula2@apple.com>, Satish Duggana <satishd@apache.org>, Colin P. McCabe <cmccabe@apache.org>

**Before/After**
```java
// Before
    public static final String REMOTE_LOG_MANAGER_THREAD_POOL_SIZE_DOC = "Size of the thread pool used in scheduling tasks to copy " +
    public static final String REMOTE_LOG_MANAGER_COPIER_THREAD_POOL_SIZE_DOC = "Size of the thread pool used in " +
            "scheduling tasks to copy segments.";
    public static final int DEFAULT_REMOTE_LOG_MANAGER_COPIER_THREAD_POOL_SIZE = 10;
    public static final String REMOTE_LOG_MANAGER_EXPIRATION_THREAD_POOL_SIZE_DOC = "Size of the thread pool used in" +
            " scheduling tasks to clean up remote log segments.";
    public static final int DEFAULT_REMOTE_LOG_MANAGER_EXPIRATION_THREAD_POOL_SIZE = 10;


// After
        case RemoteLogManagerConfig.REMOTE_LOG_MANAGER_COPIER_THREAD_POOL_SIZE_PROP => assertPropertyInvalid(baseProperties, name, "not_a_number", 0, -2)
        case RemoteLogManagerConfig.REMOTE_LOG_MANAGER_EXPIRATION_THREAD_POOL_SIZE_PROP => assertPropertyInvalid(baseProperties, name, "not_a_number", 0, -2)
import org.apache.kafka.common.config.ConfigException;
    public static final String REMOTE_LOG_MANAGER_THREAD_POOL_SIZE_DOC = "Deprecated. Size of the thread pool used in scheduling tasks to copy " +
    private static final String REMOTE_LOG_MANAGER_THREAD_POOL_FALLBACK = "The default value of -1 means that this will be set to the configured value of " +
        REMOTE_LOG_MANAGER_THREAD_POOL_SIZE_PROP + ", if available; otherwise, it defaults to " + DEFAULT_REMOTE_LOG_MANAGER_THREAD_POOL_SIZE + ".";
    private static final ConfigDef.Validator REMOTE_LOG_MANAGER_THREAD_POOL_SIZE_VALIDATOR = ConfigDef.LambdaValidator.with(
        (name, value) -> {
            if ((int) value < -1 || (int) value == 0) throw new ConfigException(name, value, "Value can be -1 or greater than 0");
        },
        () -> REMOTE_LOG_MANAGER_THREAD_POOL_FALLBACK
    );
```

**How to spot**
When reviewing storage/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17031: make rlm thread pool configurations public and fix default handling (#17499).

**Test**
Test modified in KafkaConfigTest.scala covering the corrected behaviour.
