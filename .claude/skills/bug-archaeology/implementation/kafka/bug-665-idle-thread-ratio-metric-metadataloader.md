**Commit**: 22efb4865e · **Ticket**: KAFKA-19791
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
This change adds the metric MetadataLoader::AvgIdleRatio, following the
same pattern as the existing ControllerEventManager::AvgIdleRatio
metric. This metric measures the average idle ratio of the metadata loader event
queue thread, indicating how much time the metadata loader spends
waiting for events versus actively processing them. The metric value
ranges from 0.0 (always busy processing) to 1.0 (always idle waiting for
events). Reviewers: José Armando García Sancio <jsancio@apache.org>

**Before/After**
```java
// Before
          new MetadataLoaderMetrics(Optional.of(KafkaYammerMetrics.defaultRegistry()),
          new MetadataLoaderMetrics(Optional.of(KafkaYammerMetrics.defaultRegistry()),
        A new metric <code>AvgIdleRatio</code> has been added to the <code>ControllerEventManager</code> group. This metric measures the average idle ratio of the controller event queue thread,
        providing visibility into how much time the controller spends waiting for events versus processing them. The metric value ranges from 0.0 (always busy) to 1.0 (always idle).
    public void updateIdleTime(long idleDurationMs) {
            avgIdleTimeRatio.record((double) idleDurationMs, time.milliseconds());
                metrics = new MetadataLoaderMetrics(Optional.empty(),
            Time.SYSTEM,
            new ShutdownEvent());

// After
          new MetadataLoaderMetrics(
            Optional.of(KafkaYammerMetrics.defaultRegistry()),
          new MetadataLoaderMetrics(
            Optional.of(KafkaYammerMetrics.defaultRegistry()),
        A new metric <code>AvgIdleRatio</code> has been added to the <code>ControllerEventManager</code> and <code>MetadataLoader</code> groups. These metrics measure the average idle ratio of their respective event queue threads,
        providing visibility into how much time each component spends waiting for events versus processing them. The metric value ranges from 0.0 (always busy) to 1.0 (always idle).
    public void updateIdleTime(long idleDurationMs, long currentTimeMs) {
            avgIdleTimeRatio.record((double) idleDurationMs, currentTimeMs);
                metrics = new MetadataLoaderMetrics(
                    Optional.empty(),
            time,
            new ShutdownEvent(),
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19791; add idle thread ratio metric to metadataloader (#20724).

**Test**
Test modified in QuorumControllerMetricsTest.java covering the corrected behaviour.
