**Commit**: 597838ff2a · **Ticket**: KAFKA-20254
**Subsystem**: docs/getting-started
**Category**: Logic error in condition

**Root cause**
Fix internal documentation links that were using the old path format
(`/documentation/streams/...`) and `.html` extensions, which no longer
resolve correctly in the new docs site structure. Also reorder the
KAFKA-20254 migration warning note in the Streams upgrade guide to
appear after the GA announcement paragraph for better readability. Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
  * The Streams Rebalance Protocol ([KIP-1071](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1071%3A+Streams+Rebalance+Protocol)) is now production-ready for its core feature set. This broker-driven rebalancing system designed specifically for Kafka Streams applications provides faster, more stable rebalances and better observability. For more information about the supported feature set, usage, and migration, please refer to the [Streams developer guide](/{version}/documentation/streams/developer-guide/streams-rebalance-protocol.html). Due to a critical broker-side bug in the offline migration code ([KAFKA-20254](https://issues.apache.org/jira/browse/KAFKA-20254)), we recommend against doing migrations from classic to streams groups in 4.2.0. Newly created streams groups are not impacted. Users planning to migrate should upgrade their brokers to a later release that includes the fix.
If you are using **streams rebalance protocol** (available since AK 4.2), use the [Streams groups CLI](kafka-streams-group-sh#reset-offsets).
**Warning:** Due to a critical broker-side bug in the offline migration code ([KAFKA-20254](https://issues.apache.org/jira/browse/KAFKA-20254)), we recommend against doing migrations from classic to streams groups in 4.2.0. Newly created streams groups are not impacted. Users planning to migrate should upgrade their brokers to a later release that includes the fix.

// After
  * The Streams Rebalance Protocol ([KIP-1071](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1071%3A+Streams+Rebalance+Protocol)) is now production-ready for its core feature set. This broker-driven rebalancing system designed specifically for Kafka Streams applications provides faster, more stable rebalances and better observability. For more information about the supported feature set, usage, and migration, please refer to the [Streams developer guide](/{version}/streams/developer-guide/streams-rebalance-protocol). Due to a critical broker-side bug in the offline migration code ([KAFKA-20254](https://issues.apache.org/jira/browse/KAFKA-20254)), we recommend against doing migrations from classic to streams groups in 4.2.0. Newly created streams groups are not impacted. The fix will be targeted for a future release.
If you are using **streams rebalance protocol** (available since AK 4.2), use the [Streams groups CLI](/{version}/streams/developer-guide/kafka-streams-group-sh#reset-offsets).
**Warning:** Due to a critical broker-side bug in the offline migration code ([KAFKA-20254](https://issues.apache.org/jira/browse/KAFKA-20254)), we recommend against doing migrations from classic to streams groups in 4.2.0. Newly created streams groups are not impacted. The fix will be targeted for a future release.
```

**How to spot**
When reviewing docs/getting-started code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: minor: fix broken documentation links for streams rebalance protocol (#21685).

**Test**
No test added.
