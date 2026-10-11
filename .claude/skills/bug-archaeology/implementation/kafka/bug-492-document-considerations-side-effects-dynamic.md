**Commit**: 57f8f66a28 · **Ticket**: KAFKA-20030
**Subsystem**: docs/operations
**Category**: Logic error in condition

**Root cause**
Add documentation for considerations and side effects of dynamic
partition expansion. Reviewers: TengYao Chi <frankvicky@apache.org>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before
Be aware that one use case for partitions is to semantically partition data, and adding partitions doesn't change the partitioning of existing data so this may disturb consumers if they rely on that partition. That is if data is partitioned by `hash(key) % number_of_partitions` then this partitioning will potentially be shuffled by adding partitions but Kafka will not attempt to automatically redistribute data in any way.

// After
**Note:** Dynamically increasing the number of partitions for a topic has several important considerations and potential side effects:

  * **Key Distribution Changes**: If data is partitioned by `hash(key) % number_of_partitions`, the default partitioner's mapping logic changes when the partition count increases. This means that messages with the same key may be routed to different partitions after the expansion, potentially affecting message ordering guarantees for existing keys. Kafka will not attempt to automatically redistribute existing data.
  * **Potential Data Loss with `auto.offset.reset=latest`**: Existing consumers configured with `auto.offset.reset=latest` might miss messages produced to the new partitions during the window between partition creation and consumer discovery. This occurs because consumers may not immediately detect the new partitions, and any messages produced to those partitions before the consumer rebalances will be skipped.
  * **Metadata Propagation Delay**: New partitions are not immediately visible to producers and consumers due to metadata refresh intervals (controlled by `metadata.max.age.ms`). There will be a brief period where clients are unaware of the new partitions, which may result in uneven distribution of messages or consumer lag.
  * **Risks with Internal Topics**: Users should **never** manually increase partitions for Kafka's internal state topics such as `__consumer_offsets`, `__transaction_state`, `__share_group_state`,  or `__cluster_metadata`. Doing so can break coordinator mapping logic, cause state inconsistencies, and lead to data corruption or system failures. These topics are managed automatically by Kafka and should not be modified manually.
```

**How to spot**
When reviewing docs/operations code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20030 document considerations and side effects of dynamic partition expansion (#21294).

**Test**
No test added.
