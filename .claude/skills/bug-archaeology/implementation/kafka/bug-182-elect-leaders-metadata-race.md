**Commit**: af2a8db3c6 · **Ticket**: KAFKA-18105
**Subsystem**: core/src/test/scala/integration/kafka/api/PlaintextAdminIntegrationTest.scala
**Category**: Race condition / missing synchronization

**Root cause**: `testElectPreferredLeaders` called `changePreferredLeader()` and `waitForBrokersOutOfIsr()` immediately after reassignment operations and broker kills. Kafka metadata propagates asynchronously — if a broker's metadata cache still holds the previous leader for a partition, the preferred-leader election request targets the wrong node, causing sporadic test failures. The fix introduces a `waitForBrokerMetadataPropagation` helper that polls all brokers until they agree on the same leader endpoint before proceeding.

**Before/After**:
```scala
// BEFORE – changePreferredLeader called with potentially stale metadata
changePreferredLeader(prefer1)
// ...later:
killBroker(1)
TestUtils.waitForBrokersOutOfIsr(client, ...)

// AFTER – wait for metadata consistency first
waitForBrokerMetadataPropagation(partition1)
waitForBrokerMetadataPropagation(partition2)
changePreferredLeader(prefer1)
killBroker(1)
waitForBrokerMetadataPropagation(partition1)
waitForBrokerMetadataPropagation(partition2)
TestUtils.waitForBrokersOutOfIsr(client, ...)
```

**How to spot**: Whenever an integration test reassigns partitions or kills a broker and then immediately issues a leader election, there is a window where metadata is inconsistent. Look for `changePreferredLeader` or `electLeaders` calls that are not preceded by a metadata-consistency poll.

**Test**: The fix is in the test itself. The `waitForBrokerMetadataPropagation` helper polls `broker.metadataCache.getPartitionLeaderEndpoint` on every broker and spins until all non-empty results agree on a single leader ID, eliminating the race.
