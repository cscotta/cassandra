**Commit**: dd82542493 · **Ticket**: KAFKA-19504

**Subsystem**: Clients / KafkaAdminClient

**Category**: Resource leak

**Root cause**
`KafkaAdminClient.createInternal` already added `clientTelemetryReporter` to the reporters list during construction. The public `KafkaAdminClient` constructor then added it again, resulting in the reporter being registered twice. The duplicate registration caused the reporter's internal resources (threads, metrics subscriptions) to be started twice and never properly stopped, producing a resource/memory leak. Removing the second `add` call in the constructor eliminates the duplication.

**Before/After**
```java
// Before: duplicate add in constructor
public KafkaAdminClient(...) {
    // createInternal already added clientTelemetryReporter
    reporters.add(clientTelemetryReporter); // <-- second add, causes leak
}

// After: duplicate add removed
public KafkaAdminClient(...) {
    // createInternal handles reporter registration; no second add
}
```

**How to spot**
When a reporter or listener is added to a collection during factory construction (`createInternal`) and again in the object's constructor, the object is registered twice. Search for `reporters.add` (or equivalent) in both factory methods and constructors of the same class.

**Test**
`KafkaAdminClientTest` verifies that `clientTelemetryReporter` appears exactly once in the reporters list after `KafkaAdminClient` construction.
