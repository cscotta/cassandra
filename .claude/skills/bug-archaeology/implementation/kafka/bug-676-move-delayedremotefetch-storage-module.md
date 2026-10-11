**Commit**: 76a1c839cf · **Ticket**: KAFKA-19340
**Subsystem**: checkstyle/import-control-storage.xml
**Category**: Logic error in condition

**Root cause**
Move DelayedRemoteFetch to the storage module and rewrite it to java. Reviewers: Mickael Maison <mickael.maison@gmail.com>, Kamal
 Chandraprakash <kamal.chandraprakash@gmail.com>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before

import org.apache.kafka.server.LogReadResult;
                        Throwable error = remoteLogReadResult.error().get();
                                ReplicaManager.createLogReadResult(error).toFetchPartitionData(false)
import org.apache.kafka.server.LogReadResult;
import org.apache.kafka.common.requests.FetchRequest.PartitionData
import org.apache.kafka.storage.internals.log.LogOffsetMetadata
case class FetchPartitionStatus(startOffsetMetadata: LogOffsetMetadata, fetchInfo: PartitionData) {

  override def toString: String = {
    "[startOffsetMetadata: " + startOffsetMetadata +
      ", fetchInfo: " + fetchInfo +

// After

        <subpackage name="purgatory">
            <allow pkg="org.apache.kafka.server.storage.log" />
        </subpackage>
import org.apache.kafka.storage.internals.log.LogReadResult;
                                new LogReadResult(Errors.forException(remoteLogReadResult.error().get())).toFetchPartitionData(false)
import org.apache.kafka.storage.internals.log.LogReadResult;
import org.apache.kafka.storage.internals.log.{FetchPartitionStatus, LogOffsetMetadata}
```

**How to spot**
When reviewing checkstyle/import-control-storage.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19340 move delayedremotefetch to the storage module (#19876).

**Test**
Test modified in DelayedShareFetchTest.java covering the corrected behaviour.
