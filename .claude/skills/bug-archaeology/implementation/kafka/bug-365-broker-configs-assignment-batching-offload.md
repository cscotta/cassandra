**Commit**: 8337d8368c · **Ticket**: KAFKA-20300
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Add group.{consumer,share,streams}.assignment.interval.ms config options
to control the delay between assignment calculation. These config
options are dynamic at the broker level. Add group.{consumer,share,streams}.{min,max}.assignment.interval.ms
config options to limit the
group.{consumer,share,streams}.assignment.interval.ms config values. Add group.{consumer,share,streams}.assignor.offload.enable config
options to control whether assignment calculation is offloaded to a
group coordinator background thread.

**Before/After**
```java
// Before
  def testCoordinatorCachedBufferMaxBytesUpdates(): Unit = {
    origProps.put(ShareCoordinatorConfig.CACHED_BUFFER_MAX_BYTES_CONFIG, "3145728")
    val ctx = new DynamicLogConfigContext(origProps)
    assertEquals(2 * 1024 * 1024, ctx.config.groupCoordinatorConfig.cachedBufferMaxBytes())
    assertEquals(3 * 1024 * 1024, ctx.config.shareCoordinatorConfig.shareCoordinatorCachedBufferMaxBytes())

// After
    KafkaConfig.clampDynamicConfigs(newProps.asJava)
  def clampDynamicConfigs(props: java.util.Map[String, String]): Unit = {
    GroupCoordinatorConfig.clampDynamicConfigs(props)
  }

  def testDynamicGroupCoordinatorConfig(): Unit = {
    assertTrue(GroupCoordinatorConfig.RECONFIGURABLE_CONFIGS.contains(GroupCoordinatorConfig.CACHED_BUFFER_MAX_BYTES_CONFIG))
    assertTrue(GroupCoordinatorConfig.RECONFIGURABLE_CONFIGS.contains(GroupCoordinatorConfig.CONSUMER_GROUP_ASSIGNMENT_INTERVAL_MS_CONFIG))
    assertTrue(GroupCoordinatorConfig.RECONFIGURABLE_CONFIGS.contains(GroupCoordinatorConfig.CONSUMER_GROUP_ASSIGNOR_OFFLOAD_ENABLE_CONFIG))
    assertTrue(GroupCoordinatorConfig.RECONFIGURABLE_CONFIGS.contains(GroupCoordinatorConfig.SHARE_GROUP_ASSIGNMENT_INTERVAL_MS_CONFIG))
    assertTrue(GroupCoordinatorConfig.RECONFIGURABLE_CONFIGS.contains(GroupCoordinatorConfig.SHARE_GROUP_ASSIGNOR_OFFLOAD_ENABLE_CONFIG))
    assertTrue(GroupCoordinatorConfig.RECONFIGURABLE_CONFIGS.contains(GroupCoordinatorConfig.STREAMS_GROUP_ASSIGNMENT_INTERVAL_MS_CONFIG))
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20300: add broker configs for assignment batching and offload (#21730).

**Test**
Test modified in DynamicBrokerConfigTest.scala covering the corrected behaviour.
