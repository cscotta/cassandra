**Commit**: b4be178599 · **Ticket**: KAFKA-17393
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Based on [KIP-724](https://cwiki.apache.org/confluence/display/KAFKA/KIP-724%3A+Drop+support+for+message+formats+v0+and+v1), the `log.message.format.version` and `message.format.version` can be removed in 4.0. These configs effectively a no-op with inter-broker protocol version 3.0 or higher
since Apache Kafka 3.0, so the impact should be minimal. Reviewers: Ismael Juma <ismael@juma.me.uk>

**Before/After**
```java
// Before
    /**
     * @deprecated since 3.0, removal planned in 4.0. The default value for this config is appropriate
     * for most situations.
     */
    @Deprecated
    public static final String MESSAGE_FORMAT_VERSION_CONFIG = "message.format.version";

    /**
     * @deprecated since 3.0, removal planned in 4.0. The default value for this config is appropriate
     * for most situations.
     */
    @Deprecated

// After
                    RecordBatch.CURRENT_MAGIC_VALUE,
import org.apache.kafka.common.config.ConfigResource
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17393: remove log.message.format.version/message.format.version (kip-724) (#18267).

**Test**
Test modified in DescribeTopicPartitionsRequestHandlerTest.java covering the corrected behaviour.
