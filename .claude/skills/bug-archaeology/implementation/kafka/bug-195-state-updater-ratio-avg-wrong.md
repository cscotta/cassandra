**Commit**: 3f5b877b83 · **Ticket**: KAFKA-18369
**Subsystem**: streams/src/main/java/org/apache/kafka/streams/processor/internals/DefaultStateUpdater.java
**Category**: Wrong constant / default value

**Root cause**: The four state-updater ratio metrics (`idle-ratio`, `active-restore-ratio`, `standby-update-ratio`, `checkpoint-ratio`) were implemented using `Avg` — they computed the average of per-iteration ratios rather than the ratio of accumulated time over a rolling measurement window. The per-iteration ratio is dominated by very short or very long iterations, making the metrics noisy and misleading. The fix replaces `Avg` with `WindowedSum`-based arithmetic: accumulate each time component and the total latency into separate rolling sums, then divide the component sum by the total sum at reporting time.

**Before/After**:
```java
// BEFORE – per-iteration ratio averaged with Avg()
updaterMetrics.idleRatioSensor.record((double) totalWaitLatency / totalLatency, now);
// sensor configured with Avg

// AFTER – windowed sums, ratio computed across the whole window
idleTimeWindowedSum.record(metricsConfig, idleTime, now);
runOnceLatencyWindowedSum.record(metricsConfig, totalLatency, now);
// at report time:
double elapsedTime = windowedSum.measure(metricsConfig, now);
ratioSensor.record(elapsedTime / runOnceLatencyWindow, now);
```

**How to spot**: Any ratio metric built with `Avg` over per-iteration fractions is statistically incorrect when iteration durations vary. Look for `new Avg()` on sensor metric names ending in `-ratio`; they should use windowed-sum division instead.

**Test**: Existing `DefaultStateUpdaterTest` assertions on ratio values are updated to reflect the windowed-sum semantics. No new test scenario added; the correctness change is in the metric computation itself.
