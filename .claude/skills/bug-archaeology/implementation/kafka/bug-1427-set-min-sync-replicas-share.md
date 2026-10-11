**Commit**: 5235e11d4d · **Ticket**: KAFKA-18809
**Subsystem**: share-coordinator/src
**Category**: Logic error in condition

**Root cause**
- The share.coordinator.state.topic.min.isr config defined in ShareCoordinatorConfig was not being used in the AutoTopicCreationManager. - The AutoTopicCreationManager calls the ShareCoordinatorService.shareGroupStateTopicConfigs to configs for the topic to create. - The method ShareCoordinatorService.shareGroupStateTopicConfigs was not setting the supplied config value for share.coordinator.state.topic.min.isr to min.insync.replicas. - In this PR, we remedy the situation by setting the value
- A test has been added to ShareCoordinatorServiceTest so that this is not repeated for any configs.

**Before/After**
```java
// Before
// (no removals)

// After
        properties.put(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, config.shareCoordinatorStateTopicMinIsr());
import org.apache.kafka.common.config.TopicConfig;
import java.util.Properties;
    @Test
    public void testShareStateTopicConfigs() {
        CoordinatorRuntime<ShareCoordinatorShard, CoordinatorRecord> runtime = mockRuntime();
        MockTime time = new MockTime();
        MockTimer timer = new MockTimer(time);
        PartitionWriter writer = mock(PartitionWriter.class);

        Metrics metrics = new Metrics();
        ShareCoordinatorService service = spy(new ShareCoordinatorService(
```

**How to spot**
When reviewing share-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18809 set min in sync replicas for __share_group_state. (#18922).

**Test**
Test modified in ShareCoordinatorServiceTest.java covering the corrected behaviour.
