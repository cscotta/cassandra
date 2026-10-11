**Commit**: 73afcc9b69 · **Ticket**: KAFKA-13610
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
JIRA: KAFKA-13610  This patch deprecates the `log.cleaner.enable`
configuration. It's part of
[KIP-1148](https://cwiki.apache.org/confluence/x/XAyWF). Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, PoAn Yang
 <payang@apache.org>, Ken Huang <s7133700@gmail.com>, Jhen-Yung Hsu
 <jhenyunghsu@gmail.com>

**Before/After**
```java
// Before
  val logCleanerEnable = getBoolean(CleanerConfig.LOG_CLEANER_ENABLE_PROP)
    public static final String LOG_CLEANER_ENABLE_DOC = "Enable the log cleaner process to run on the server. Should be enabled if using any topics with a cleanup.policy=compact including the internal offsets topic. If disabled those topics will not be compacted and continually grow in size.";

// After
            <li><b>Broker</b>
                <ul>
                    <li>
                        The configuration <code>log.cleaner.enable</code> is deprecated. Users should no longer set it to <code>false</code> to prepare for future removal.
                        For further details, please refer to <a href="https://cwiki.apache.org/confluence/x/XAyWF">KIP-1148</a>.
                    </li>
                </ul>
            </li>
    @Deprecated(since = "4.1", forRemoval = true)
    @Deprecated(since = "4.1", forRemoval = true)
    public static final String LOG_CLEANER_ENABLE_DOC = "This configuration has been deprecated and will be removed in Kafka 5.0. Users should not set it to false to prepare for its future removal. " +
            "Enable the log cleaner process to run on the server. Should be enabled if using any topics with a cleanup.policy=compact including the internal offsets topic. If disabled those topics will not be compacted and continually grow in size.";
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-13610: deprecate log.cleaner.enable configuration (#19472).

**Test**
No test added.
