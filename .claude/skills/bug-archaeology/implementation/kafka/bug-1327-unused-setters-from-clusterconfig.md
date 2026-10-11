**Commit**: 1584d49470 · **Ticket**: KAFKA-18944
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Remove unused `saslServerProperties`, `saslClientProperties`,
`adminClientProperties`, `producerProperties`, and `consumerProperties`
in ClusterConfig. First, I quickly fixed the unused adminClientProperties, and then I will
move on to https://github.com/apache/kafka/pull/19094 to fix the related
issues. Pass AdminClientRebootstrapTest
<img width="1398" alt="Screenshot 2025-03-09 at 12 54 57 PM"
src="https://github.com/user-attachments/assets/73c50376-6602-493d-8abd-0eb2bb304114"
/>

Pass ClusterConfigTest
<img width="1117" alt="Screenshot 2025-03-09 at 12 55 28 PM"
src="https://github.com/user-attachments/assets/b4da59da-dfdf-4698-9077-5086854360ab"
/>

Reviewers: Chia-Ping Tsai <chia7712@g...

**Before/After**
```java
// Before
            .setAdminClientProperties(rebootstrapProperties)
    private final Map<String, String> producerProperties;
    private final Map<String, String> consumerProperties;
    private final Map<String, String> adminClientProperties;
    private final Map<String, String> saslServerProperties;
    private final Map<String, String> saslClientProperties;
                  MetadataVersion metadataVersion, Map<String, String> serverProperties, Map<String, String> producerProperties,
                  Map<String, String> consumerProperties, Map<String, String> adminClientProperties, Map<String, String> saslServerProperties,
                  Map<String, String> saslClientProperties, Map<Integer, Map<String, String>> perServerProperties, List<String> tags,
                  Map<Feature, Short> features) {
        this.producerProperties = Objects.requireNonNull(producerProperties);
        this.consumerProperties = Objects.requireNonNull(consumerProperties);

// After
                  MetadataVersion metadataVersion, Map<String, String> serverProperties,
                  Map<Integer, Map<String, String>> perServerProperties, List<String> tags, Map<Feature, Short> features) {
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18944 remove unused setters from clusterconfig (#19166).

**Test**
Test modified in AdminClientRebootstrapTest.java covering the corrected behaviour.
