**Commit**: 23c459286b · **Ticket**: KAFKA-16368
**Subsystem**: docs/upgrade.html
**Category**: Logic error in condition

**Root cause**
Reviewers: Divij Vaidya <diviv@amazon.com>

**Before/After**
```java
// Before
    val logConfig = new LogConfig(new Properties())
    public static final long LOG_MESSAGE_TIMESTAMP_AFTER_MAX_MS_DEFAULT = Long.MAX_VALUE;
    public static final int NUM_RECOVERY_THREADS_PER_DATA_DIR_DEFAULT = 1;

// After
    val properties = new Properties()
    // This test uses future timestamps beyond the default of 1 hour.
    properties.put(TopicConfig.MESSAGE_TIMESTAMP_AFTER_MAX_MS_CONFIG, Long.MaxValue.toString)
    val logConfig = new LogConfig(properties)
    // This test uses future timestamps beyond the default of 1 hour.
    props.setProperty(TopicConfig.MESSAGE_TIMESTAMP_AFTER_MAX_MS_CONFIG, Long.MaxValue.toString)
            </li>
                    The default value of 'num.recovery.threads.per.data.dir' has been changed from 1 to 2. The impact of this is faster
                    recovery post unclean shutdown at the expense of extra IO cycles.
                    See <a href="https://cwiki.apache.org/confluence/display/KAFKA/KIP-1030%3A+Change+constraints+and+default+values+for+various+configurations">KIP-1030</a>
                </li>
                <li>
```

**How to spot**
When reviewing docs/upgrade.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16368: update defaults for log_message_timestamp_after_max_ms_default and num_recovery_threads_per_data_dir_config.

**Test**
Test modified in LogCleanerTest.scala covering the corrected behaviour.
