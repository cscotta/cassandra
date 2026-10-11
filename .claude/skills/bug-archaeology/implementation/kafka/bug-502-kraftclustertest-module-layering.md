## Bug 502 — Test-only: KRaftClusterTest migrated to server module

**Commit:** d5df4d58a3
**Ticket:** KAFKA-19755
**Subsystem:** core / server (test infrastructure)
**Category:** Logic error in condition

### Root cause
`KRaftClusterTest` in the `core` module contained test utilities (such as helper methods for waiting on topic listings and cluster restarts) that directly imported `QuorumController` from the `controller` module, violating the layering rule that `server` tests should not reach back into `core`. When moving the test to the `server` module the import-control XML also needed updating (`allow class org.apache.kafka.controller.QuorumController` added to `import-control-server.xml`). Additionally `KafkaConfig#updateCurrentConfig` was package-private in `kafka.server` so the test could not call it from the new location; a `private[server]` accessor was added.

### Before
```scala
// core module — violated module boundary
import org.apache.kafka.controller.QuorumController
```

### After
```xml
<!-- import-control-server.xml -->
<allow class="org.apache.kafka.controller.QuorumController" />
```
```scala
// server module — explicit accessor for tests
private[server] def updateCurrentConfig(props: util.Map[_, _]): Unit = {
    this.currentConfig = new KafkaConfig(props)
}
```

### How to spot
Integration tests that mix `core` and `server` module classes without checkstyle import-control exceptions will fail at checkstyle time. Look for `import org.apache.kafka.controller.*` inside `kafka.server` test files.

### Test
Existing `KRaftClusterTest` test cases rewritten in Java under the `server` module.
