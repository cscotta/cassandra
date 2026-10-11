**Commit**: 023833fe1f · **Ticket**: KAFKA-18778
**Subsystem**: core / tools / StorageTool; tools / FeatureCommand
**Category**: Wrong constant / default value

**Root cause**
`MetadataVersion.fromVersionString()` internally uses `MetadataVersion.latestTesting()` as the ceiling when validating a version string, but the error message printed to the user quoted `MetadataVersion.LATEST_PRODUCTION` as the upper bound. A user attempting to specify an unstable (testing-only) version would be told it was out of range even though the code would actually accept it, and the ceiling in the error message was lower than the code's actual acceptance threshold.

**Before/After**
```java
// Before — LATEST_PRODUCTION in error message, but latestTesting() used internally
throw new TerseFailure("Unknown release version '" + releaseVersion +
    "'. Supported versions are: " + MetadataVersion.MINIMUM_VERSION.version +
    " to " + MetadataVersion.LATEST_PRODUCTION.version)

// After — consistent with internal behaviour
throw new TerseFailure("Unknown release version '" + releaseVersion +
    "'. Supported versions are: " + MetadataVersion.MINIMUM_VERSION.version +
    " to " + MetadataVersion.latestTesting().version())
```

**How to spot**
Error messages that quote a version range must always match the range the code actually accepts. When the acceptance ceiling is computed dynamically (e.g., `latestTesting()`), hard-coding a different constant in the message creates a misleading UX.

**Test**
`StorageToolTest` and `FeatureCommandTest` — assert that the exception message for an unknown version contains `MetadataVersion.latestTesting().version` (not `LATEST_PRODUCTION`).
