**Commit**: d8b15ecd40 · **Ticket**: KAFKA-17756
**Subsystem**: coordinator/group (GroupConfig, GroupMetadataManager)
**Category**: Wrong constant / default value

**Root cause**
Share group session timeout and heartbeat interval were not included in the set of dynamically configurable group configs. Both `share.session.timeout.ms` and `share.heartbeat.interval.ms` could only be set at broker startup, not per-group at runtime. Calls to `groupSessionTimeoutMs` and `groupHeartbeatIntervalMs` therefore always returned the static broker-level defaults instead of any per-group override.

**Before/After**
```java
// Before – no share-group helpers, static defaults only
int sessionTimeout = config.shareGroupSessionTimeoutMs();

// After – dynamic per-group lookup
int sessionTimeout = shareGroupSessionTimeoutMs(groupId);
// where:
private int shareGroupSessionTimeoutMs(String groupId) {
    return groupConfigManager.groupConfig(groupId)
        .map(GroupConfig::shareGroupSessionTimeoutMs)
        .orElse(config.shareGroupSessionTimeoutMs());
}
```

**How to spot**
When adding a new group type, verify that all timeout/interval parameters appear in the dynamic group config registry and that every call site goes through the per-group lookup helper rather than the static broker config.

**Test**
Unit tests added in `GroupMetadataManagerTest` for dynamic share-group config.
