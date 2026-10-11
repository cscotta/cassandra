**Commit**: 4d3e366bc2 · **Ticket**: KAFKA-16772
**Subsystem**: clients/src
**Category**: Missing null / bounds check

**Root cause**
Introduce the KRaftVersion enum to describe the current value of kraft.version. Change a bunch of places in the code that were using raw shorts over to using this new enum. In BrokerServer.scala, fix a bug that could cause null pointer exceptions during shutdown if we tried to shut down before fully coming up. Do not send finalized features that are finalized as level 0, since it is a no-op.

**Before/After**
```java
// Before
    SupportedVersionRange(final short minVersion, final short maxVersion) {
            key.setName(feature.getKey());
            key.setMinVersionLevel(versionLevel);
            key.setMaxVersionLevel(versionLevel);
            converted.add(key);
    if (unstableFeatureVersionsEnabled) {
      features.put("kraft.version", new SupportedVersionRange(0, 1))
    }
      metadataCache = MetadataCache.kRaftMetadataCache(config.nodeId)
      // If the BrokerLifecycleManager's initial catch-up future fails, it means we timed out
      // or are shutting down before we could catch up. Therefore, also fail the firstPublishFuture.
      lifecycleManager.initialCatchUpFuture.whenComplete((_, e) => {

// After
    public SupportedVersionRange(final short minVersion, final short maxVersion) {
            if (versionLevel != 0) {
                key.setName(feature.getKey());
                key.setMinVersionLevel(versionLevel);
                key.setMaxVersionLevel(versionLevel);
                converted.add(key);
            }
      metadataCache = MetadataCache.kRaftMetadataCache(config.nodeId, () => raftManager.client.kraftVersion())
      // If the BrokerLifecycleManager's initial catch-up future fails, it means we timed out
      // or are shutting down before we could catch up. Therefore, also fail the firstPublishFuture.
      lifecycleManager.initialCatchUpFuture.whenComplete((_, e) => {
        if (e != null) brokerMetadataPublisher.firstPublishFuture.completeExceptionally(e)
```

**How to spot**
When reviewing clients/src code, look for missing null / bounds check patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16772: introduce kraft.version to support kip-853 (#16230).

**Test**
Test modified in DescribeTopicPartitionsRequestHandlerTest.java covering the corrected behaviour.
