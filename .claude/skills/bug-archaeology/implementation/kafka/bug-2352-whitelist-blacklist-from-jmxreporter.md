**Commit**: 8c2a0619e2 · **Ticket**: KAFKA-17528
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    public static final String EXCLUDE_CONFIG_ALIAS = METRICS_CONFIG_PREFIX + "blacklist";
    public static final String INCLUDE_CONFIG_ALIAS = METRICS_CONFIG_PREFIX + "whitelist";
                                                                         INCLUDE_CONFIG_ALIAS,
                                                                         EXCLUDE_CONFIG,
                                                                         EXCLUDE_CONFIG_ALIAS);
            originalConfig, new String[][]{{INCLUDE_CONFIG, INCLUDE_CONFIG_ALIAS},
                                           {EXCLUDE_CONFIG, EXCLUDE_CONFIG_ALIAS}});

// After
                                                                         EXCLUDE_CONFIG);
            originalConfig, new String[][]{{INCLUDE_CONFIG},
                                           {EXCLUDE_CONFIG}});
            <li>
                The <code>metrics.jmx.blacklist</code> was removed from the <code>org.apache.kafka.common.metrics.JmxReporter</code>
                Please use <code>metrics.jmx.exclude</code> instead.
            </li>
            <li>
                The <code>metrics.jmx.whitelist</code> was removed from the <code>org.apache.kafka.common.metrics.JmxReporter</code>
                Please use <code>metrics.jmx.include</code> instead.
            </li>

```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17528 remove whitelist/blacklist from jmxreporter (#17179).

**Test**
No test added.
