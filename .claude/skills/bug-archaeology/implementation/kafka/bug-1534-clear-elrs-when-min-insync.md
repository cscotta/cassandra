**Commit**: 6235a73622 · **Ticket**: KAFKA-16540
**Subsystem**: metadata/src
**Category**: Logic error in condition

**Root cause**
In order to maintain the integrity of replication, we need to clear the ELRs of affected partitions when min.insync.replicas is changed. This could happen at the topic level, or at a global level if the cluster level default is changed. Reviewers: Colin P. McCabe <cmccabe@apache.org>

**Before/After**
```java
// Before
import static org.apache.kafka.clients.admin.AlterConfigOp.OpType.DELETE;
import static org.apache.kafka.clients.admin.AlterConfigOp.OpType.SET;
            FeatureControlManager featureControl) {
            new ApiError(INVALID_CONFIG, "Cluster-level " + MIN_IN_SYNC_REPLICAS_CONFIG +
                    " cannot be removed while ELR is enabled.");

// After
import org.apache.kafka.common.metadata.ClearElrRecord;
            FeatureControlManager featureControl
    ) {
        outputRecords.addAll(createClearElrRecordsAsNeeded(outputRecords));
    List<ApiMessageAndVersion> createClearElrRecordsAsNeeded(List<ApiMessageAndVersion> input) {
        if (!featureControl.isElrFeatureEnabled()) {
            return Collections.emptyList();
        }
        List<ApiMessageAndVersion> output = new ArrayList<>();
        for (ApiMessageAndVersion messageAndVersion : input) {
            if (messageAndVersion.message().apiKey() == CONFIG_RECORD.id()) {
                ConfigRecord record = (ConfigRecord) messageAndVersion.message();
```

**How to spot**
When reviewing metadata/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16540: clear elrs when min.insync.replicas is changed. (#18148).

**Test**
Test modified in ConfigurationControlManagerTest.java covering the corrected behaviour.
