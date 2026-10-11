**Commit**: e78977e505 · **Ticket**: KAFKA-19579
**Subsystem**: docs/ops.html
**Category**: Logic error in condition

**Root cause**
Add missing metrics for document tiered storage

-

kafka.log.remote:type=RemoteLogManager,name=RemoteLogReaderFetchRateAndTimeMs：Introduced
in

[KIP-1018](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1018%3A+Introduce+max+remote+fetch+timeout+config+for+DelayedRemoteFetch+requests)

-

kafka.server:type=DelayedRemoteListOffsetsMetrics,name=ExpiresPerSec,topic=([-.\w]),partition=([0-9])：Introduced
in

[KIP-1075](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1075%3A+Introduce+delayed+remote+list+offsets+purgatory+to+make+LIST_OFFSETS+async)

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, Lan Ding
 <isDing_L@163.com>, Kamal Chandraprakash
 <kamal.chandraprakash@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
      <tr>
        <td>RemoteLogReader Fetch Rate And Time</td>
        <td>The time to read data from remote storage by a broker</td>
        <td>kafka.log.remote:type=RemoteLogManager,name=RemoteLogReaderFetchRateAndTimeMs</td>
      </tr>
      <tr>
        <td>Delayed Remote List Offsets Expires Per Sec</td>
        <td>The number of expired remote list offsets per second. Omitting 'topic=(...), partition=(...)' will yield the all-topic rate</td>
        <td>kafka.server:type=DelayedRemoteListOffsetsMetrics,name=ExpiresPerSec,topic=([-.\w]+),partition=([0-9]+)</td>
      </tr>
```

**How to spot**
When reviewing docs/ops.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19579 add missing metrics for doc tiered storage (#20304).

**Test**
No test added.
