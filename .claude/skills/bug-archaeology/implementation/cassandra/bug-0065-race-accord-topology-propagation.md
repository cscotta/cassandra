# Bug 0065: Race Conditions in Accord Topology Propagation

**Commit**: 67e21ec4ba · **Ticket**: CASSANDRA-20316
**Subsystem**: `service/accord/AccordConfigurationService`
**Category**: Race condition / missing synchronization

---

## Root Cause

`AccordConfigurationService.fetchTopologyInternal` was a fire-and-forget operation: if a topology fetch failed (for any reason including `InterruptedException`), the failure was swallowed or rethrown without retrying, and there was no deduplication guard to prevent multiple concurrent fetches for the same epoch. If the fetch for epoch N was already in-flight and another notification arrived for the same epoch, a second fetch would start in parallel, leading to redundant and potentially interleaved topology reports. Topology propagation therefore had a race window where an epoch could be skipped or delivered out of order during node migration or bootstrap.

### Before / After

```java
// BEFORE – one-shot fire-and-forget, no deduplication, no retry
protected void fetchTopologyInternal(long epoch)
{
    Stage.ACCORD_MIGRATION.execute(() -> {
        if (ClusterMetadata.current().epoch.getEpoch() < epoch)
            ClusterMetadataService.instance().fetchLogFromCMS(Epoch.create(epoch));
        // no result returned; exception terminates silently
    });
}
```

```java
// AFTER – idempotent via ConcurrentHashMap; retries on failure
private final Map<Long, Future<Topology>> pendingTopologies = new ConcurrentHashMap<>();

private void fetchTopologyInternal(long epoch)
{
    pendingTopologies.computeIfAbsent(epoch, (epoch_) -> {
        AsyncPromise<Topology> future = new AsyncPromise<>();
        fetchTopologyAsync(epoch_,
                           (topology, throwable) -> {
                               if (topology != null) future.setSuccess(topology);
                               else {
                                   pendingTopologies.remove(epoch_);
                                   future.setFailure(throwable);
                                   fetchTopologyForEpoch(epoch_); // retry
                               }
                           });
        return future;
    });
}
```

---

## How to Spot

Async topology or configuration fetch operations should always be idempotent (deduplicated) and include a retry path on failure. If a method submits work to an executor and never tracks whether that work is already in-flight, a second caller triggers a parallel fetch, creating a race. Look for `Stage.X.execute(lambda)` calls without an accompanying guard (e.g., `computeIfAbsent`, `AtomicReference`, or similar) as a signal of this pattern.

---

## Test

`AccordMigrationWriteRaceTestBase` extended with post-truncation epoch-quiesce assertions to ensure all nodes agree on epoch before proceeding; `MigrationFromAccordWriteRaceTest` un-ignored to run the previously flaky test.
