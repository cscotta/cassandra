**Commit**: 3b1bd3812e · **Ticket**: KAFKA-16368
**Subsystem**: docs/upgrade.html
**Category**: Logic error in condition

**Root cause**
Reviewers: Divij Vaidya <diviv@amazon.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.config.ConfigException;
    public static final int DEFAULT_REMOTE_LOG_MANAGER_THREAD_POOL_SIZE = 10;
    private static final ConfigDef.Validator REMOTE_LOG_MANAGER_THREAD_POOL_SIZE_VALIDATOR = ConfigDef.LambdaValidator.with(
        (name, value) -> {
            if ((int) value < -1 || (int) value == 0) throw new ConfigException(name, value, "Value can be -1 or greater than 0");
        },
        () -> "[-1,1,...]"
    );
    public static final int DEFAULT_REMOTE_LOG_MANAGER_COPIER_THREAD_POOL_SIZE = -1;
    public static final int DEFAULT_REMOTE_LOG_MANAGER_EXPIRATION_THREAD_POOL_SIZE = -1;
                        REMOTE_LOG_MANAGER_THREAD_POOL_SIZE_VALIDATOR,

// After
                        <li>
                            The <code>remote.log.manager.copier.thread.pool.size</code> configuration default value was changed to 10 from -1.
                            Values of -1 are no longer valid. A minimum of 1 or higher is valid.
                            See <a href="https://cwiki.apache.org/confluence/display/KAFKA/KIP-1030%3A+Change+constraints+and+default+values+for+various+configurations">KIP-1030</a>
                        </li>
                        <li>
                            The <code>remote.log.manager.expiration.thread.pool.size</code> configuration default value was changed to 10 from -1.
                            Values of -1 are no longer valid. A minimum of 1 or higher is valid.
                            See <a href="https://cwiki.apache.org/confluence/display/KAFKA/KIP-1030%3A+Change+constraints+and+default+values+for+various+configurations">KIP-1030</a>
                        </li>
                        <li>
                            The <code>remote.log.manager.thread.pool.size</code> configuration default value was changed to 2 from 10.
```

**How to spot**
When reviewing docs/upgrade.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16368: update remote.log.manager.* default thread pool values for kip-1030 (#18137).

**Test**
Test modified in RemoteLogManagerConfigTest.java covering the corrected behaviour.
