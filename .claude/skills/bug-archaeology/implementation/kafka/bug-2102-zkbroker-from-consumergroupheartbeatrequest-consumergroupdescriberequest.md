**Commit**: 7b41eedc1a · **Ticket**: KAFKA-17909
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  "listeners": ["zkBroker", "broker"],
  "listeners": ["zkBroker", "broker"],
    kafkaApis = createKafkaApis()
    kafkaApis = createKafkaApis()

// After
  "listeners": ["broker"],
  "listeners": ["broker"],
    metadataCache = MetadataCache.kRaftMetadataCache(brokerId, () => KRaftVersion.KRAFT_VERSION_1)
    kafkaApis = createKafkaApis(raftSupport = true)
    metadataCache = MetadataCache.kRaftMetadataCache(brokerId, () => KRaftVersion.KRAFT_VERSION_1)
    kafkaApis = createKafkaApis(raftSupport = true)
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17909 remove zkbroker from consumergroupheartbeatrequest and consumergroupdescriberequest (#17665).

**Test**
Test modified in KafkaApisTest.scala covering the corrected behaviour.
