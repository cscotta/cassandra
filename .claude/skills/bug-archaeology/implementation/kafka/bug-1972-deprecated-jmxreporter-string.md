**Commit**: 54843e6e1e · **Ticket**: KAFKA-18077
**Subsystem**: clients/src
**Category**: Incorrect filtering / wrong query result

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        this("");
    }

    /**
     * Create a JMX reporter that prefixes all metrics with the given string.
     *  @deprecated Since 2.6.0. Use {@link JmxReporter#JmxReporter()}
     *  Initialize JmxReporter with {@link JmxReporter#contextChange(MetricsContext)}
     *  Populate prefix by adding _namespace/prefix key value pair to {@link MetricsContext}
     */
    @Deprecated
    public JmxReporter(String prefix) {
        this.prefix = prefix != null ? prefix : "";

// After
        this.prefix = "";
                        <li>The constructor <code>org.apache.kafka.common.metrics.JmxReporter</code> with string argument was removed.
                            See <a href="https://cwiki.apache.org/confluence/display/KAFKA/KIP-606%3A+Add+Metadata+Context+to+MetricsReporter">KIP-606</a> for details.
                        </li>
                        <li>The <code>use.incremental.alter.configs</code> configuration was removed from <code>MirrorSourceConnector</code>.
```

**How to spot**
When reviewing clients/src code, look for incorrect filtering / wrong query result patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18077 remove deprecated jmxreporter(string) (#17923).

**Test**
Test modified in JmxReporterTest.java covering the corrected behaviour.
