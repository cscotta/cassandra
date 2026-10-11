**Commit**: 581e94840f · **Ticket**: KAFKA-18366
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Ismael Juma <ismael@juma.me.uk>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.server.common.MetadataVersion;
    private MetadataVersion interBrokerProtocolVersion = MetadataVersion.latestProduction();
    public LogManagerBuilder setInterBrokerProtocolVersion(MetadataVersion interBrokerProtocolVersion) {
        this.interBrokerProtocolVersion = interBrokerProtocolVersion;
        return this;
    }

                              interBrokerProtocolVersion,
import org.apache.kafka.server.common.MetadataVersion
                 interBrokerProtocolVersion: MetadataVersion,
                // loadLog is completed for all logs under the logDdir, mark it.
   * @param targetLogDirectoryId The directory Id that should host the the partition's topic.

// After
                // loadLog is completed for all logs under the logDir, mark it.
   * @param targetLogDirectoryId The directory Id that should host the partition's topic.
   * @param errorHandler The error handler that will be called when an exception for a particular
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18366 remove kafkaconfig.interbrokerprotocolversion (#18820).

**Test**
Test modified in AbstractPartitionTest.scala covering the corrected behaviour.
