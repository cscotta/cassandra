**Commit**: e9a03b3a86 · **Ticket**: KAFKA-18314
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: TaiJuWu <tjwu1217@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
      val properties = TestUtils.createBrokerConfig(brokerId, "")
      properties.put(KRaftConfigs.CONTROLLER_LISTENER_NAMES_CONFIG, "SSL")
  @Test
  def testDescribeQuorumNotAllowedForZkClusters(): Unit = {
    val requestData = DescribeQuorumRequest.singletonRequest(KafkaRaftServer.MetadataPartition)
    val requestBuilder = new DescribeQuorumRequest.Builder(requestData)
    val request = buildRequest(requestBuilder.build(DescribeQuorumRequestData.HIGHEST_SUPPORTED_VERSION))

    when(clientRequestQuotaManager.maybeRecordAndGetThrottleTimeMs(any[RequestChannel.Request](),
      any[Long])).thenReturn(0)
    kafkaApis = createKafkaApis(enableForwarding = true)
    kafkaApis.handle(request, RequestLocal.withThreadConfinedCaching)

// After
      val properties = TestUtils.createBrokerConfig(brokerId, null)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18314 fix to kraft or remove tests associate with zk broker config in kafkaapistest (#18309).

**Test**
Test modified in KafkaApisTest.scala covering the corrected behaviour.
