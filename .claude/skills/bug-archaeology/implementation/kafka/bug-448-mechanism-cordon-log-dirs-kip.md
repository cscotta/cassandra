**Commit**: a45d36ca5d · **Ticket**: KAFKA-19774
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Implements [KIP-1066](https://cwiki.apache.org/confluence/x/Lg_TEg)

Reviewers: Christo Lolov <lolovc@amazon.com>, PoAn Yang
 <payang@apache.org>

**Before/After**
```java
// Before
                "Unable to replicate the partition 4 time(s): The target replication factor of 4 cannot be reached because only 3 broker(s) are registered."
                logDirResult.usableBytes()));
        this(error, replicaInfos, UNKNOWN_VOLUME_BYTES, UNKNOWN_VOLUME_BYTES);
    public LogDirDescription(ApiException error, Map<TopicPartition, ReplicaInfo> replicaInfos, long totalBytes, long usableBytes) {
  "validVersions": "0-1",
      "about": "Log directories that failed and went offline." }

// After
                "Unable to replicate the partition 4 time(s): The target replication factor of 4 cannot be "
                    + "reached because only 3 broker(s) are registered or some brokers have all their log directories cordoned."
                logDirResult.usableBytes(),
                logDirResult.isCordoned()));
    private final boolean isCordoned;
        this(error, replicaInfos, UNKNOWN_VOLUME_BYTES, UNKNOWN_VOLUME_BYTES, false);
    public LogDirDescription(ApiException error, Map<TopicPartition, ReplicaInfo> replicaInfos, long totalBytes, long usableBytes, boolean isCordoned) {
        this.isCordoned = isCordoned;
    /**
     * Whether this log directory is cordoned or not.
     */
    public boolean isCordoned() {
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19774: mechanism to cordon log dirs (kip-1066) (#21273).

**Test**
Test modified in CreateTopicsRequestWithPolicyTest.java covering the corrected behaviour.
