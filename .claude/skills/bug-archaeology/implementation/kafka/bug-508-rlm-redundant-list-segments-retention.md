## Bug 508 — RemoteLogManager: redundant listRemoteLogSegments calls during size-based retention

**Commit:** da9e0ddb66
**Ticket:** KAFKA-20026
**Subsystem:** storage / tiered storage / RemoteLogManager
**Category:** Logic error in condition

### Root cause
`RLMExpirationTask.buildRetentionSizeData` always called `listRemoteLogSegments(tpId, epoch)` for every leader epoch on every retention check cycle, even when the previous cycle had already validated all segments. This was wasteful: the expensive per-epoch RLMM call was made even when the total size was below the retention limit, burning CPU and RLMM RPC budget.

Additionally, `COPY_SEGMENT_STARTED` state segments were included in the `remoteLogSizeBytes` metric, polluting the metric during upload retries and inflating the apparent remote size.

### Before
```java
// always validated every epoch, regardless of whether needed
long remoteLogSizeBytes = 0;
for (Integer epoch : epochEntries.navigableKeySet()) {
    Iterator<RemoteLogSegmentMetadata> iter = rlmm.listRemoteLogSegments(tpId, epoch);
    …
}
```

### After
```java
// early exit: if local + remote already fits, skip per-epoch validation
if (retentionSize < 0 || (onlyLocalLogSegmentsSize + fullCopyFinishedSegmentsSizeInBytes) <= retentionSize) {
    return Optional.empty();
}
// cache isAllSegmentsValid flag; skip per-epoch calls when valid
if (!isAllSegmentsValid) { … }
```

### How to spot
Retention or cleanup methods that invoke `listRemoteLogSegments(tpId, epoch)` inside a loop over all epochs without first checking whether the total size already satisfies the retention limit are performing unnecessary work.

### Test
New unit tests in `RemoteLogManagerTest` covering the early-exit path and segment state filtering.
