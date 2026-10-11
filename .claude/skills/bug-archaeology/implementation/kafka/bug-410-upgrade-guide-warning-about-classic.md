**Commit**: 15036f8d96 · **Ticket**: KAFKA-20254
**Subsystem**: docs/getting-started
**Category**: Logic error in condition

**Root cause**
Due to a critical bug in the offline migration code (KAFKA-20254), we
recommend against doing migrations from classic to streams groups in
4.2.0. This adds a note to both the main upgrade guide and the Kafka
Streams upgrade guide, advising users to upgrade to a later release that
includes the fix before attempting the migration. Reviewers: Matthias J. Sax <matthias@confluent.io>, Christo Lolov
 <lolovc@amazon.com>

**Before/After**
```java
// Before
  * The Streams Rebalance Protocol ([KIP-1071](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1071%3A+Streams+Rebalance+Protocol)) is now production-ready for its core feature set. This broker-driven rebalancing system designed specifically for Kafka Streams applications provides faster, more stable rebalances and better observability. For more information about the supported feature set, usage, and migration, please refer to the [Streams developer guide](/{version}/documentation/streams/developer-guide/streams-rebalance-protocol.html).

// After
  * The Streams Rebalance Protocol ([KIP-1071](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1071%3A+Streams+Rebalance+Protocol)) is now production-ready for its core feature set. This broker-driven rebalancing system designed specifically for Kafka Streams applications provides faster, more stable rebalances and better observability. For more information about the supported feature set, usage, and migration, please refer to the [Streams developer guide](/{version}/documentation/streams/developer-guide/streams-rebalance-protocol.html). Due to a critical broker-side bug in the offline migration code ([KAFKA-20254](https://issues.apache.org/jira/browse/KAFKA-20254)), we recommend against doing migrations from classic to streams groups in 4.2.0. Newly created streams groups are not impacted. Users planning to migrate should upgrade their brokers to a later release that includes the fix.

**Warning:** Due to a critical broker-side bug in the offline migration code ([KAFKA-20254](https://issues.apache.org/jira/browse/KAFKA-20254)), we recommend against doing migrations from classic to streams groups in 4.2.0. Newly created streams groups are not impacted. Users planning to migrate should upgrade their brokers to a later release that includes the fix.
**Note:** Due to a critical broker-side bug in the offline migration code ([KAFKA-20254](https://issues.apache.org/jira/browse/KAFKA-20254)), we recommend against doing migrations from classic to streams groups in 4.2.0. Newly created streams groups are not impacted. Users planning to migrate should upgrade their brokers to a later release that includes the fix.

```

**How to spot**
When reviewing docs/getting-started code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20254: add upgrade guide warning about classic to streams group migration in 4.2.0 (#21638).

**Test**
No test added.
