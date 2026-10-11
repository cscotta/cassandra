**Commit**: 180112a4a9 · **Ticket**: KAFKA-18084
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>, poorv Mittal <apoorvmittal10@gmail.com>, Sushant Mahajan <sushant.mahajan88@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            // Initialize the share partition by reading the state from the persister.
            persister.readState(new ReadShareGroupStateParameters.Builder()
                .setGroupTopicPartitionData(new GroupTopicPartitionData.Builder<PartitionIdLeaderEpochData>()
                    .setGroupId(this.groupId)
                    .setTopicsData(Collections.singletonList(new TopicData<>(topicIdPartition.topicId(),
                        Collections.singletonList(PartitionFactory.newPartitionIdLeaderEpochData(topicIdPartition.partition(), leaderEpoch)))))
                    .build())
                .build()
            ).whenComplete((result, exception) -> {
            });
        } catch (Exception e) {
            log.error("Failed to initialize the share partition: {}-{}", groupId, topicIdPartition, e);

// After
        } catch (Exception e) {
            log.error("Failed to initialize the share partition: {}-{}", groupId, topicIdPartition, e);
            completeInitializationWithException(future, e);
            return future;
        } finally {
            lock.writeLock().unlock();
        }
        // Initialize the share partition by reading the state from the persister.
        persister.readState(new ReadShareGroupStateParameters.Builder()
            .setGroupTopicPartitionData(new GroupTopicPartitionData.Builder<PartitionIdLeaderEpochData>()
                .setGroupId(this.groupId)
                .setTopicsData(Collections.singletonList(new TopicData<>(topicIdPartition.topicId(),
```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18084 added write locks in sharepartition where locks were async calls were being made (#17957).

**Test**
Test modified in SharePartitionTest.java covering the corrected behaviour.
