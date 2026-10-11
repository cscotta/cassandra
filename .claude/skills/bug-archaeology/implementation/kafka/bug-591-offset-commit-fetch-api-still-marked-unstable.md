**Commit**: 9599143bfd · **Ticket**: KAFKA-19186

**Subsystem**: clients/src/main/resources/common/message/OffsetCommitRequest.json

**Category**: Wrong constant/default value

**Root cause**
The `OffsetCommitRequest` and `OffsetFetchRequest` JSON schemas had `"latestVersionUnstable": true`, which meant clients always negotiated a version one below the actual latest when `unstable.api.versions.enable` was false on the broker. As KIP-848 stabilised these APIs (versions 10), the `latestVersionUnstable` flag was no longer appropriate. The fix removes the flag from both schemas and updates the `Builder` factory methods to call `latestVersion()` instead of `latestVersion(enableUnstableLastVersion)`, ensuring clients now negotiate the full stable version 10.

**Before**
```java
// Before (schema had "latestVersionUnstable": true)
return new Builder(data,
    ApiKeys.OFFSET_COMMIT.oldestVersion(),
    ApiKeys.OFFSET_COMMIT.latestVersion(enableUnstableLastVersion));
```

**After**
```java
// After
return new Builder(data,
    ApiKeys.OFFSET_COMMIT.oldestVersion(),
    ApiKeys.OFFSET_COMMIT.latestVersion());
```

**How to spot**
Search `.json` message schemas for `"latestVersionUnstable": true`; once the associated KIP is marked stable, the flag must be removed and any callers passing the `enableUnstableLastVersion` boolean must be updated to call `latestVersion()` without the flag.

**Test**
Run `OffsetFetchRequestTest` and `RequestResponseTest` after removing the flag and confirm the negotiated version is 10 on clusters that have `unstable.api.versions.enable=false`.
