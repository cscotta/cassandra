**Commit**: aac62a32d9 · **Ticket**: KAFKA-18698
**Subsystem**: server-common/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Ken Huang <s7133700@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>, Christo Lolov <lolovc@amazon.com>

**Before/After**
```java
// Before
public final class FinalizedFeatures {
    private final MetadataVersion metadataVersion;
    private final Map<String, Short> finalizedFeatures;
    private final long finalizedFeaturesEpoch;


    public MetadataVersion metadataVersion() {
        return metadataVersion;
    }

    public Map<String, Short> finalizedFeatures() {
        return finalizedFeatures;

// After
public record FinalizedFeatures(
    MetadataVersion metadataVersion,
    Map<String, Short> finalizedFeatures,
    long finalizedFeaturesEpoch
) {
 * @param topicId Universally unique Id representing this topic partition.
 * @param partitionId The partition Id.
public record TopicIdPartition(
    Uuid topicId,
    int partitionId
) {
                request.destination(),
```

**How to spot**
When reviewing server-common/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18698: migrate suitable classes to records in server and server-common modules (#18783).

**Test**
Test modified in InterBrokerSendThreadTest.java covering the corrected behaviour.
