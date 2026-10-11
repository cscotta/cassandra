**Commit**: 8aa5eae2f9 · **Ticket**: KAFKA-19457

**Subsystem**: Group Coordinator / ShareGroup initialization

**Category**: Wrong constant/default value

**Root cause**
The share group initialization retry interval was hard-coded as `offsetCommitTimeoutMs * 2`, coupling an unrelated timeout to the retry schedule and making it impossible to tune independently. Additionally, the partition set for the retry was computed using `IntStream.range(0, numPartitions)` which produced partition indexes rather than the correct set derived from `keySet()` of the initializing-topics map. The fix introduces a new config `group.share.initialize.retry.interval.ms` (default 30 s) and corrects the partition set computation.

**Before/After**
```java
// Before: hard-coded multiplier and wrong partition set
long retryInterval = offsetCommitTimeoutMs * 2;
Set<Integer> partitions = IntStream.range(0, numPartitions)
    .boxed().collect(Collectors.toSet());

// After: configurable interval and correct key set
long retryInterval = shareGroupInitializeRetryIntervalMs;
Set<Integer> partitions = initializingTopics.get(topicId).keySet();
```

**How to spot**
Magic-multiplier intervals derived from unrelated timeouts are a sign that a proper configuration key was never introduced. Search for `* 2` or similar scaling of timeout constants in coordinator initialization paths and evaluate whether a dedicated config is warranted.

**Test**
`GroupCoordinatorConfigTest` verifies that `group.share.initialize.retry.interval.ms` is correctly read and that values below `offsetCommitTimeoutMs` are clamped to `offsetCommitTimeoutMs`.
