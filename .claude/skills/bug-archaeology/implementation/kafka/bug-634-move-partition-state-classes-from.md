**Commit**: 68800a027b · **Ticket**: KAFKA-19301
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Follow up to the discussion in #20083. Move partition state management classes from
`org.apache.kafka.storage.internals.log` to
`org.apache.kafka.server.partition` package. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.storage.internals.log.PartitionListener;
import org.apache.kafka.storage.internals.log.{AlterPartitionListener, AppendOrigin, AssignmentState, AsyncOffsetReader, CommittedPartitionState, FetchDataInfo, LeaderHwChange, LogAppendInfo, LogOffsetMetadata, LogOffsetSnapshot, LogOffsetsListener, LogReadInfo, LogStartOffsetIncrementReason, OffsetResultHolder, OngoingReassignmentState, PartitionListener, PartitionState, PendingExpandIsr, PendingPartitionChange, PendingShrinkIsr, SimpleAssignmentState, UnifiedLog, VerificationGuard}
import org.apache.kafka.storage.internals.log.{AppendOrigin, LogConfig, PartitionListener, VerificationGuard}
import kafka.cluster.{Partition}

// After
import org.apache.kafka.server.partition.PartitionListener;
import org.apache.kafka.storage.internals.log.{AppendOrigin, AsyncOffsetReader, FetchDataInfo, LeaderHwChange, LogAppendInfo, LogOffsetMetadata, LogOffsetSnapshot, LogOffsetsListener, LogReadInfo, LogStartOffsetIncrementReason, OffsetResultHolder, UnifiedLog, VerificationGuard}
import org.apache.kafka.server.partition.{AlterPartitionListener, AssignmentState, CommittedPartitionState, OngoingReassignmentState, PartitionListener, PartitionState, PendingExpandIsr, PendingPartitionChange, PendingShrinkIsr, SimpleAssignmentState}
import org.apache.kafka.server.partition.PartitionListener
import org.apache.kafka.storage.internals.log.{AppendOrigin, LogConfig, VerificationGuard}
import kafka.cluster.Partition
import org.apache.kafka.server.partition.PartitionListener
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19301 move partition state classes from storage to server module (#20831).

**Test**
Test modified in AbstractPartitionTest.scala covering the corrected behaviour.
