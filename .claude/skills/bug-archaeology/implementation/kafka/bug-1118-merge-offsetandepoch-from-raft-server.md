**Commit**: 979f49f967 · **Ticket**: KAFKA-19146
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
1. remove org.apache.kafka.raft.OffsetAndEpoch
2. rewrite org.apache.kafka.server.common.OffsetAndEpoch by record
keyword
3. rename OffsetAndEpoch#leaderEpoch to OffsetAndEpoch#epoch

Reviewers: PoAn Yang <payang@apache.org>, Xuan-Zhang Gong
 <gongxuanzhangmelt@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        int epoch = epochAndLeaderLocalStartOffset.leaderEpoch();
              .setLeaderEpoch(epochAndOffset.leaderEpoch)
import org.apache.kafka.raft.{Isolation, KafkaRaftClient, LogAppendInfo, LogFetchInfo, LogOffsetMetadata, MetadataLogConfig, OffsetAndEpoch, OffsetMetadata, ReplicatedLog, SegmentPosition, ValidOffsetAndEpoch}
        offsetAndEpoch.leaderEpoch == epoch) =>
        new OffsetAndEpoch(offsetAndEpoch.offset, offsetAndEpoch.leaderEpoch)
        val followerEpoch = offsetAndEpoch.leaderEpoch
import org.apache.kafka.raft.{KafkaRaftClient, LogAppendInfo, LogOffsetMetadata, MetadataLogConfig, OffsetAndEpoch, QuorumConfig, ReplicatedLog, SegmentPosition, ValidOffsetAndEpoch}

// After
        int epoch = epochAndLeaderLocalStartOffset.epoch();
              .setLeaderEpoch(epochAndOffset.epoch())
import org.apache.kafka.raft.{Isolation, KafkaRaftClient, LogAppendInfo, LogFetchInfo, LogOffsetMetadata, MetadataLogConfig, OffsetMetadata, ReplicatedLog, SegmentPosition, ValidOffsetAndEpoch}
import org.apache.kafka.server.common.OffsetAndEpoch
        offsetAndEpoch.epoch() == epoch) =>
        new OffsetAndEpoch(offsetAndEpoch.offset, offsetAndEpoch.epoch())
        val followerEpoch = offsetAndEpoch.epoch()
import org.apache.kafka.raft.{KafkaRaftClient, LogAppendInfo, LogOffsetMetadata, MetadataLogConfig, QuorumConfig, ReplicatedLog, SegmentPosition,
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19146 merge offsetandepoch from raft to server-common (#19475).

**Test**
Test modified in KafkaMetadataLogTest.scala covering the corrected behaviour.
