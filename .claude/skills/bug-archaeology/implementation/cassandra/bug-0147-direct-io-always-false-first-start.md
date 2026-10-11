# Bug 0147: Direct IO Support Always False on First Node Start

**Commit**: 5ab976d796 · **Ticket**: CASSANDRA-19779
**Subsystem**: config (DatabaseDescriptor)
**Category**: Logic error in condition

---

## Root Cause

`DatabaseDescriptor.applyCDCTablesConfiguration()` calls `FileUtils.getBlockSize(new File(getCommitLogLocation()))` to determine whether Direct IO is supported. On the very first start of a node the commit-log directory does not yet exist, so `getBlockSize` throws a `RuntimeException` (or its wrapped `IOError`). The exception was silently swallowed and `directIOSupported` was left `false`, meaning the node would never enable Direct IO even on hardware that supports it. The fix ensures the commit-log directory is created before the block-size probe and re-throws `IOError`/`ConfigurationException` instead of treating them as "unsupported".

Additionally, the helper `setCommitLogWriteDiskAccessMode()` was not updating the cached `commitLogWriteDiskAccessMode` field, so callers that read the cached field (e.g. the `FuzzTestBase`) would see a stale value after calling the setter.

### Before / After

```java
// BEFORE – directory may not exist; RuntimeException silently swallowed
directIOSupported = FileUtils.getBlockSize(new File(getCommitLogLocation())) > 0;
```

```java
// AFTER – directory is created first; real errors re-thrown
File commitLogLocationDir = new File(commitLogLocation);
PathUtils.createDirectoriesIfNotExists(commitLogLocationDir.toPath());
directIOSupported = FileUtils.getBlockSize(commitLogLocationDir) > 0;
```

---

## How to Spot

Capability probes that rely on the presence of a directory or file must ensure the target exists before the probe, especially during initialization where the data directories may not yet have been created. Catching a broad `RuntimeException` and silently treating it as "feature unavailable" hides real errors (permission denied, misconfiguration) and also masks legitimate feature availability.

---

## Test

Unit test mocks `PathUtils.createDirectoriesIfNotExists` and `FileUtils.getBlockSize` to throw a `RuntimeException`, then verifies that the correct `DiskAccessMode` cascade is applied based on the compression/encryption/strategy configuration; a separate `FuzzTestBase` tweak sets a valid mode after `daemonInitialization` to avoid the stale-field issue.
