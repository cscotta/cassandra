**Commit**: f359908fcd · **Ticket**: KAFKA-15776
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Satish Duggana <satishd@apache.org>, Luke Chen <showuon@gmail.com>

**Before/After**
```java
// Before
    RemoteLogManagerConfig.REMOTE_LOG_INDEX_FILE_CACHE_TOTAL_SIZE_BYTES_PROP
    val remoteFetchMaxWaitMs = config.remoteLogManagerConfig.remoteFetchMaxWaitMs()

    public int remoteFetchMaxWaitMs() {
        return getInt(REMOTE_FETCH_MAX_WAIT_MS_PROP);
    }


// After
    RemoteLogManagerConfig.REMOTE_LOG_INDEX_FILE_CACHE_TOTAL_SIZE_BYTES_PROP,
    RemoteLogManagerConfig.REMOTE_FETCH_MAX_WAIT_MS_PROP
  def remoteFetchMaxWaitMs = getInt(RemoteLogManagerConfig.REMOTE_FETCH_MAX_WAIT_MS_PROP)

    val remoteFetchMaxWaitMs = config.remoteFetchMaxWaitMs.toLong
  @Test
  def testDynamicRemoteFetchMaxWaitMsConfig(): Unit = {
    val props = TestUtils.createBrokerConfig(0, TestUtils.MockZkConnect, port = 8181)
    val config = KafkaConfig(props)
    val kafkaBroker = mock(classOf[KafkaBroker])
    when(kafkaBroker.config).thenReturn(config)
    assertEquals(500, config.remoteFetchMaxWaitMs)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15776: support added to update remote.fetch.max.wait.ms dynamically (#16203).

**Test**
Test modified in DynamicBrokerConfigTest.scala covering the corrected behaviour.
