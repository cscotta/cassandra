**Commit**: c3f0890f53 · **Ticket**: KAFKA-19075
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
This PR includes `share.session.timeout.ms` and
`share.heartbeat.interval.ms` in the `extractShareGroupConfigMap` method
in `ShareGroupConfig`. With this change, the default value of
`share.session.timeout.ms` and `share.heartbeat.interval.ms` for every
group will be set as the value of the static configs
`group.share.session.timeout.ms` and `group.share.heartbeat.interval.ms`
respectively

Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
        defaultConfigs.putAll(shareGroupConfig.extractShareGroupConfigMap());
     * Copy the subset of properties that are relevant to share group.
    public Map<String, Integer> extractShareGroupConfigMap() {
        return Map.of(GroupConfig.SHARE_RECORD_LOCK_DURATION_MS_CONFIG, shareGroupRecordLockDurationMs());

// After
        defaultConfigs.putAll(shareGroupConfig.extractShareGroupConfigMap(this));
     * Copy the subset of properties that are relevant to share group. These configs include those which can be set
     * statically (for all groups) or dynamically (for a specific group). In those cases, the default value for the
     * group specific dynamic config (Ex. share.session.timeout.ms) should be the value set for the static config
     * (Ex. group.share.session.timeout.ms).
    public Map<String, Integer> extractShareGroupConfigMap(GroupCoordinatorConfig groupCoordinatorConfig) {
        return Map.of(
            GroupConfig.SHARE_SESSION_TIMEOUT_MS_CONFIG, groupCoordinatorConfig.shareGroupSessionTimeoutMs(),
            GroupConfig.SHARE_HEARTBEAT_INTERVAL_MS_CONFIG, groupCoordinatorConfig.shareGroupHeartbeatIntervalMs(),
            GroupConfig.SHARE_RECORD_LOCK_DURATION_MS_CONFIG, shareGroupRecordLockDurationMs()
        );
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19075: included other share group dynamic configs in extractsharegroupconfigmap method in sharegroupconfig (#19348.

**Test**
No test added.
