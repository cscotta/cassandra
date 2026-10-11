**Commit**: 6322bd165c · **Ticket**: KAFKA-18786

**Subsystem**: clients/src/main/java/org/apache/kafka/clients/admin/DescribeFeaturesOptions.java

**Category**: Missing null/bounds check

**Root cause**
`DescribeFeaturesOptions` had no `nodeId` field, so `KafkaAdminClient.describeFeatures()` always sent the request to the least-loaded broker rather than allowing callers to target a specific node. When a cluster has heterogeneous feature flags across nodes (e.g. during a rolling upgrade), describing a specific node's features was impossible. The fix adds an optional `nodeId` field to `DescribeFeaturesOptions` and threads it through `KafkaAdminClient` so the request is routed to the indicated node when set.

**Before**
```java
// Before: no nodeId, always routes to least-loaded broker
public class DescribeFeaturesOptions extends AbstractOptions<DescribeFeaturesOptions> {
    // nodeId absent
}
```

**After**
```java
// After: optional nodeId routes to specific node
public class DescribeFeaturesOptions extends AbstractOptions<DescribeFeaturesOptions> {
    private OptionalInt nodeId = OptionalInt.empty();
    public DescribeFeaturesOptions nodeId(int nodeId) { ... }
    public OptionalInt nodeId() { return nodeId; }
}
```

**How to spot**
In `KafkaAdminClient.describeFeatures()`, if the node-selection logic does not consult `options.nodeId()` before falling back to the least-loaded-broker path, targeted feature queries are impossible.

**Test**
In `DescribeFeaturesTest`, call `describeFeatures` with a specific `nodeId` and assert that the result reflects that node's feature set, including any per-node `unstable.api.versions.enable` override.
