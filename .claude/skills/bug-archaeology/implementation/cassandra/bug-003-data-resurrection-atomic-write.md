# Bug 003: Non-Atomic Heartbeat Write in DataResurrectionCheck

**Commit**: 20d19c6627 · **Ticket**: CASSANDRA-21290
**Subsystem**: `org.apache.cassandra.service.DataResurrectionCheck` — startup safety check for zombie row prevention
**Category**: Non-atomic write on a safety-critical state file; crash corrupts the file, causing the safety check itself to fail on restart

---

## Root Cause

Heartbeat was written in-place with Jackson's `writeValue(OutputStream)`, which auto-closes the stream — making a post-write `fsync` impossible. A crash mid-write leaves a partial/empty JSON file. On restart, parse failure was treated as fatal, blocking startup.

### Before / After

```java
// BEFORE — in-place overwrite, no fsync, no rename
try (FileOutputStreamPlus out = outputFile.newOutputStream(OVERWRITE)) {
    JSON_OBJECT_PRETTY_WRITER.writeValue((OutputStream) out, object);
    // Jackson auto-closes `out` here — sync() cannot be called after this
}
```

```java
// AFTER — bytes first (we control the stream), fsync, atomic rename
byte[] data = jsonMapper.writeValueAsBytes(object);
try (FileOutputStreamPlus out = tempFile.newOutputStream(OVERWRITE)) {
    out.write(data);
    out.sync();                          // fsync before close
}
tempFile.move(outputFile);               // atomic rename
SyncUtil.trySyncDir(outputFile.parent()); // fsync parent dir — makes rename durable
```

```java
// BEFORE — any parse failure → StartupException, node won't start
catch (IOException ex) {
    throw new StartupException(ERR_WRONG_DISK_STATE, "Failed to deserialize heartbeat file ...");
}

// AFTER — fall back to file mtime (last successful write is a safe proxy)
catch (IOException ex) {
    heartbeat = new Heartbeat(Instant.ofEpochMilli(heartbeatFile.lastModified()));
}
```

---

## How to Spot

Any file that gates startup, migration, or recovery needs the write-then-rename pattern: `bytes → .tmp → fsync → rename → fsync parent`. Jackson's `writeValue(OutputStream)` is unsafe for safety-critical files because it auto-closes. `OVERWRITE` mode is never atomic. A fatal error handler on parse failure without a fallback turns a corrupted write into an unrecoverable state — worst-case for the exact shutdown scenarios this check is meant to survive.

---

## Test

`StartupChecksTest.testDataResurrectionCheckLastModifiedFallback`: write empty string to heartbeat file (simulates corrupt write) → set mtime to now → assert `verifySuccess()`. Validates the fallback path.
