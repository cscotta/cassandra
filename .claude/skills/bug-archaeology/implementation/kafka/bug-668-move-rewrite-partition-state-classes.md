**Commit**: 23b809a8ad · **Ticket**: KAFKA-19301
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
move following interface/class to
`org.apache.kafka.storage.internals.log`  and rewrtie to java

`PartitionListener‎`  `AlterPartitionListener`  `AssignmentState`
`OngoingReassignmentState`  `SimpleAssignmentState`  `PartitionState`
`PendingPartitionChange`  `PendingExpandIsr`  `PendingShrinkIsr`
`CommittedPartitionState`

Reviewers: TengYao Chi <frankvicky@apache.org>, TaiJuWu
 <tjwu1217@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import kafka.cluster.PartitionListener;
import org.apache.kafka.storage.internals.log.{AppendOrigin, AsyncOffsetReader, FetchDataInfo, LeaderHwChange, LogAppendInfo, LogOffsetMetadata, LogOffsetsListener, LogOffsetSnapshot, LogReadInfo, LogStartOffsetIncrementReason, OffsetResultHolder, UnifiedLog, VerificationGuard}
/**
 * Listener receives notification from an Online Partition.
 *
 * A listener can be (re-)registered to an Online partition only. The listener
 * is notified as long as the partition remains Online. When the partition fails
 * or is deleted, respectively `onFailed` or `onDeleted` are called once. No further
 * notifications are sent after this point on.
 *
 * Note that the callbacks are executed in the thread that triggers the change
 * AND that locks may be held during their execution. They are meant to be used

// After
import org.apache.kafka.storage.internals.log.PartitionListener;
import org.apache.kafka.storage.internals.log.{AlterPartitionListener, AppendOrigin, AssignmentState, AsyncOffsetReader, CommittedPartitionState, FetchDataInfo, LeaderHwChange, LogAppendInfo, LogOffsetMetadata, LogOffsetSnapshot, LogOffsetsListener, LogReadInfo, LogStartOffsetIncrementReason, OffsetResultHolder, OngoingReassignmentState, PartitionListener, PartitionState, PendingExpandIsr, PendingPartitionChange, PendingShrinkIsr, SimpleAssignmentState, UnifiedLog, VerificationGuard}
import java.util
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19301 move and rewrite partition state classes to java in org.apache.kafka.storage.internals.log (#20083).

**Test**
Test modified in AbstractPartitionTest.scala covering the corrected behaviour.
