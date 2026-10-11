**Commit**: 34fa571ba6 · **Ticket**: KAFKA-20115
**Subsystem**: clients/clients-integration-tests (group coordinator)
**Category**: State not cleaned up

**Root cause**
The group coordinator failed to unload its metadata partition when it was no longer the leader or follower. The underlying defect was fixed in a prior ticket (KAFKA-19519); this commit adds a dedicated integration test to prevent future regressions. The scenario is a __consumer_offsets partition reassignment that transfers leadership from broker 0 to broker 1, after which broker 0 must drop its active-partition count to zero.

**Before/After**
No production-code change. The commit adds `testSingleCoordinatorOwnershipAfterPartitionReassignment` which reproduces the scenario.

**How to spot**
Watch for the `num-partitions` (state=active) metric in `GroupCoordinatorRuntimeMetrics` remaining non-zero on a broker that has lost the __consumer_offsets partition. Any test that reassigns a coordinator partition without asserting that the previous owner's metric drops to 0 leaves this regression undetected.

**Test**
`ConsumerIntegrationTest#testSingleCoordinatorOwnershipAfterPartitionReassignment`: creates a two-broker cluster, forces __consumer_offsets partition 0 onto broker 0, verifies `num-partitions(active)` is 1/0 on brokers 0/1, reassigns the partition to broker 1, then waits for the metric to flip to 0/1.
