**Commit**: eb0a7358c4 · **Ticket**: KAFKA-19697
**Subsystem**: connect/runtime — Worker.ConnectorStatusMetricsGroup
**Category**: Missing null / bounds check

**Root cause**
`ConnectorStatusMetricsGroup.recordTaskRemoved()` was called once per task when a connector's tasks were removed. When all tasks of a connector failed to start (e.g., DNS resolution failure during producer setup), the *first* call to `recordTaskRemoved()` would remove the connector's `MetricGroup` from `connectorStatusMetrics`, then call `close()` on it. The *second* call (for the next failing task) found no entry in the map and tried to call `.close()` on the result of `connectorStatusMetrics.get(...)`, which was `null`, producing an NPE.

**Before/After**
```java
// Before — get then close; NPE if already removed
connectorStatusMetrics.get(connectorTaskId.connector()).close();
connectorStatusMetrics.remove(connectorTaskId.connector());

// After — atomic remove; null-check before close
MetricGroup metricGroup = connectorStatusMetrics.remove(connectorTaskId.connector());
if (metricGroup != null) metricGroup.close();
```

**How to spot**
Two-step get-then-remove patterns on a map are not atomic and fail when the same key is removed concurrently or in rapid succession. The correct idiom is `remove()` followed by a null check on the returned value. Search for `map.get(k).close(); map.remove(k)` patterns.

**Test**
`WorkerTest#testConnectorStatusMetricsGroup_tasksFailedToStart` — creates a `ConnectorStatusMetricsGroup` with an empty task map (simulating all tasks failing), adds two tasks, removes the first (which removes the connector metric), then asserts that removing the second does not throw.
