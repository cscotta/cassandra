**Commit**: 88b9ff30ad · **Ticket**: KAFKA-15859
**Subsystem**: checkstyle/suppressions.xml
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    RemoteLogManagerConfig.REMOTE_LOG_MANAGER_FETCH_MAX_BYTES_PER_SECOND_PROP
                        REMOTE_FETCH_MAX_WAIT_MS_DOC);

// After
    <suppress checks="MethodLength" files="RemoteLogManagerConfig.java"/>
    RemoteLogManagerConfig.REMOTE_LOG_MANAGER_FETCH_MAX_BYTES_PER_SECOND_PROP,
    RemoteLogManagerConfig.REMOTE_LIST_OFFSETS_REQUEST_TIMEOUT_MS_PROP
  @Test
  def testDynamicRemoteListOffsetsRequestTimeoutMsConfig(): Unit = {
    val props = TestUtils.createBrokerConfig(0, TestUtils.MockZkConnect, port = 8181)
    val config = KafkaConfig(props)
    val kafkaBroker = mock(classOf[KafkaBroker])
    when(kafkaBroker.config).thenReturn(config)
    when(kafkaBroker.remoteLogManagerOpt).thenReturn(None)
    assertEquals(RemoteLogManagerConfig.DEFAULT_REMOTE_LIST_OFFSETS_REQUEST_TIMEOUT_MS,
      config.remoteLogManagerConfig.remoteListOffsetsRequestTimeoutMs)
```

**How to spot**
When reviewing checkstyle/suppressions.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15859 introduce remote.list.offsets.request.timeout.ms dynamic config (#17045).

**Test**
Test modified in DynamicBrokerConfigTest.scala covering the corrected behaviour.
