**Commit**: ca0cc355f6 · **Ticket**: KAFKA-12670
**Subsystem**: core/KafkaController (KRaft)
**Category**: Missing null/bounds check

**Root cause**
KRaft mode had no mechanism to trigger unclean leader election via static or dynamic configuration. The controller only elected unclean leaders in response to an explicit request; it did not periodically scan for leaderless partitions that could be recovered by enabling unclean election. As a result, partitions with no in-sync replicas could remain leaderless indefinitely even when `unclean.leader.election.enable=true`. The fix adds a periodic background scan in the KRaft controller that identifies leaderless partitions and schedules unclean elections when the config permits it.

**Before/After**
```scala
// Before: no periodic scan for leaderless partitions in KRaft
// unclean.leader.election.enable had no effect in KRaft mode

// After: background scan added to KafkaController
scheduler.schedule("unclean-leader-election", () => {
  electLeaders(UncleanLeaderElectionStrategy, leaderlessPartitions)
}, period = config.uncleanLeaderElectionIntervalMs)
```

**How to spot**
When porting a ZooKeeper-era feature to KRaft, check whether the ZK controller had a background timer or watcher that triggered the feature, and replicate that trigger in the KRaft controller. Missing the background trigger means the feature silently does nothing even when enabled.

**Test**
`kraftControllerConfigs()` test helper signature updated to accept `TestInfo`; new integration test verifies unclean election fires when configured.
