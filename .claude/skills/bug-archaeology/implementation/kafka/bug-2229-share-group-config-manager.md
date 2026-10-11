**Commit**: f1aa3a555e · **Ticket**: KAFKA-17532
**Subsystem**: coordinator/share (SharePartition, SharePartitionManager)
**Category**: Wrong constant / default value

**Root cause**
`SharePartition` hardcoded its acquisition lock duration from a static broker configuration at construction time. When group-level config overrides were introduced, the partition continued using the static value because it had no reference to `GroupConfigManager`. The fix injects `GroupConfigManager` into `SharePartitionManager` and `SharePartition`, allowing `scheduleAcquisitionLockTimeout` to look up per-group overrides at runtime.

**Before/After**
```java
// Before – static value from broker config
long lockDuration = config.shareGroupRecordLockDurationMs();

// After – dynamic per-group lookup
long lockDuration = groupConfigManager
    .groupConfig(groupId)
    .map(GroupConfig::shareGroupRecordLockDurationMs)
    .orElse(config.shareGroupRecordLockDurationMs());
```

**How to spot**
Any timeout or duration parameter that is set once at object construction from a static broker config will silently ignore dynamic per-group overrides; verify that all per-group tunables are fetched through `GroupConfigManager` at the point of use.

**Test**
Unit tests updated in `SharePartitionTest` and `SharePartitionManagerTest`.
