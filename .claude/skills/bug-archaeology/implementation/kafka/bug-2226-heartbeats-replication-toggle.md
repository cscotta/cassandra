**Commit**: 6d650ff9e5 · **Ticket**: KAFKA-17534
**Subsystem**: connect/mirror (MirrorSourceConnector)
**Category**: Missing override of default method

**Root cause**
`MirrorSourceConnector` always replicated the internal `__heartbeats` topic to the target cluster regardless of the configured topic filter. There was no way to opt out of heartbeat replication without patching the connector. The missing design element was a `heartbeats.replication.enabled` configuration knob (defaulting to `true` for backward compatibility) that the topic-selection predicate could consult.

**Before/After**
```java
// Before – heartbeats always included
return topics.stream()
    .filter(t -> !t.equals(config.heartbeatsTopic()))
    .collect(...);
// heartbeatsTopic appended unconditionally

// After – conditional on config
if (config.heartbeatsReplicationEnabled()) {
    topics.add(config.heartbeatsTopic());
}
```

**How to spot**
Internal connector-managed topics (heartbeats, checkpoints, offsets) that are appended outside the normal topic-filter pipeline bypass all user-configured filtering; each such topic should have an explicit enable/disable config.

**Test**
Unit tests added in `MirrorSourceConnectorTest` for the disabled case.
