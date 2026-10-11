**Commit**: e7a2af8414 · **Ticket**: KAFKA-18634
**Subsystem**: metadata / ReplicationControlManager / ImageWriterOptions
**Category**: Wrong constant / default value

**Root cause**
Whether the Eligible Leader Replicas (ELR) feature is active was gated on `MetadataVersion.isElrSupported()` (a static version threshold), but ELR can be independently enabled or disabled via the `EligibleLeaderReplicasVersion` feature flag at runtime. The two checks disagree once the cluster metadata version is >= 4.0IV1 but ELR is not explicitly enabled. The fix adds `featureControl.isElrFeatureEnabled()` (and the corresponding `FeaturesImage.isElrEnabled()` method) and threads it through `ImageWriterOptions` so every serialization / deserialization path uses the same, authoritative flag.

**Before/After**
```java
// Before — uses static MV threshold
if (featureControl.metadataVersion().isElrSupported()) { ... }

// After — uses the live feature flag
if (featureControl.isElrFeatureEnabled()) { ... }
// and in PartitionRegistration.toRecord():
if (options.isEligibleLeaderReplicasEnabled()) {
    if (elr.length > 0) record.setEligibleLeaderReplicas(Replicas.toList(elr));
}
```

**How to spot**
Any feature that has its own `FeatureLevelRecord` (i.e., it is independently upgradeable) must not be conditioned solely on `MetadataVersion`. Search for `isElrSupported()` / `isXxxSupported()` calls that are not paired with the corresponding feature-flag check.

**Test**
`ReplicationControlManagerTest` / `FeaturesImageTest` — verifies that ELR fields are written to snapshots and partition records only when the `EligibleLeaderReplicasVersion` feature is at level >= 1, irrespective of the raw metadata version.
