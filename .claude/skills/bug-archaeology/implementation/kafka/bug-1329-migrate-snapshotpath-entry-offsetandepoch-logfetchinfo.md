**Commit**: 119d043c49 · **Ticket**: KAFKA-18700
**Subsystem**: raft/src
**Category**: State not cleaned up

**Root cause**
Migrate the following data carrier class to records to eliminate
constructors, `equals`, `hashCode`, and `toString`. * `Entry` in `LogHistory`
* `SnapshotPath`

Additionally, migrate the following classes as discussed:
* OffsetAndEpoch
* LogFetchInfo
* LogAppendInfo

In Java, accessing a field in record class requires parentheses. In Scala, parentheses are not needed because Scala allows omitting them
when calling parameterless methods; hence, there is no need to change
the Scala code. Reviewers: Ken Huang <s7133700@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                    listenerContext.fireHandleCommit(nextExpectedOffset, readInfo.records);
                if (state.updateReplicaState(replicaKey, currentTimeMs, info.startOffsetMetadata)) {
                records = info.records;
            kafkaRaftMetrics.updateFetchedRecords(info.lastOffset - info.firstOffset + 1);
        kafkaRaftMetrics.updateAppendRecords(info.lastOffset - info.firstOffset + 1);
        logger.trace("Leader appended records at base offset {}, new end offset is {}", info.firstOffset, endOffset);
            OffsetAndEpoch offsetAndEpoch = new OffsetAndEpoch(info.lastOffset, epoch);
public class LogAppendInfo {

    public final long firstOffset;
    public final long lastOffset;


// After
                    listenerContext.fireHandleCommit(nextExpectedOffset, readInfo.records());
                if (state.updateReplicaState(replicaKey, currentTimeMs, info.startOffsetMetadata())) {
                records = info.records();
            kafkaRaftMetrics.updateFetchedRecords(info.lastOffset() - info.firstOffset() + 1);
        kafkaRaftMetrics.updateAppendRecords(info.lastOffset() - info.firstOffset() + 1);
        logger.trace("Leader appended records at base offset {}, new end offset is {}", info.firstOffset(), endOffset);
            OffsetAndEpoch offsetAndEpoch = new OffsetAndEpoch(info.lastOffset(), epoch);
public record LogAppendInfo(long firstOffset, long lastOffset) { }
public record LogFetchInfo(Records records, LogOffsetMetadata startOffsetMetadata) { }
```

**How to spot**
When reviewing raft/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18700 migrate snapshotpath, entry, offsetandepoch, logfetchinfo, and logappendinfo to record classes (#19062).

**Test**
Test modified in KafkaRaftClientReconfigTest.java covering the corrected behaviour.
