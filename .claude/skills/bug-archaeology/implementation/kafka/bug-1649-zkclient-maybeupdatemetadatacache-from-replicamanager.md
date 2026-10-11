**Commit**: da0c3beffa · **Ticket**: KAFKA-18491
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import kafka.zk.KafkaZkClient;
    private Optional<KafkaZkClient> zkClient = Optional.empty();
    public ReplicaManagerBuilder setZkClient(KafkaZkClient zkClient) {
        this.zkClient = Optional.of(zkClient);
        return this;
    }

                             OptionConverters.toScala(zkClient),
        zkClient = None,
import kafka.server.metadata.ZkMetadataCache
import kafka.zk.KafkaZkClient
                     val zkClient: Option[KafkaZkClient] = None,

// After
// (no additions)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18491 remove zkclient & maybeupdatemetadatacache from replicamanager (#18507).

**Test**
Test modified in LocalLeaderEndPointTest.scala covering the corrected behaviour.
