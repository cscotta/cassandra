**Commit**: 3fa998475b · **Ticket**: KAFKA-18539
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
Removed Optional for SharePartitionManager and ClientMetricsManager as zookeeper code is being removed. Also removed asScala and asJava conversion in KafkaApis.handleListClientMetricsResources, moved to java stream. Reviewers: Mickael Maison <mickael.maison@gmail.com>, Ismael Juma <ismael@juma.me.uk>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before


    private Optional<SharePartitionManager> sharePartitionManager = Optional.empty();
    private Optional<ClientMetricsManager> clientMetricsManager = Optional.empty();
    public KafkaApisBuilder setSharePartitionManager(Optional<SharePartitionManager> sharePartitionManager) {
    public KafkaApisBuilder setClientMetricsManager(Optional<ClientMetricsManager> clientMetricsManager) {
                             OptionConverters.toScala(sharePartitionManager),
                             OptionConverters.toScala(clientMetricsManager));
        sharePartitionManager = Some(sharePartitionManager),
        clientMetricsManager = Some(clientMetricsManager))

// After
    private SharePartitionManager sharePartitionManager = null;
    private ClientMetricsManager clientMetricsManager = null;
    public KafkaApisBuilder setSharePartitionManager(SharePartitionManager sharePartitionManager) {
    public KafkaApisBuilder setClientMetricsManager(ClientMetricsManager clientMetricsManager) {
    @SuppressWarnings({"CyclomaticComplexity"})
        if (sharePartitionManager == null) throw new RuntimeException("You must set sharePartitionManager");
        if (clientMetricsManager == null) throw new RuntimeException("You must set clientMetricsManager");
                             sharePartitionManager,
                             clientMetricsManager);
        sharePartitionManager = sharePartitionManager,
        clientMetricsManager = clientMetricsManager)
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18539 remove optional managers in kafkaapis (#18550).

**Test**
Test modified in KafkaApisTest.scala covering the corrected behaviour.
