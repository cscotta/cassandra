# Bug 0838: LogAwareFileLister Misclassifies Files After Race With File Deletion

**Commit**: 5cda140bae · **Ticket**: CASSANDRA-11889
**Subsystem**: SSTable lifecycle / LogRecord
**Category**: Race condition / missing synchronization

---

## Root Cause
`LogRecord.make()` computed `lastModified` as the maximum of `File.lastModified()` across all files returned by `getExistingFiles()`. Because `File.lastModified()` returns `0` for a file that no longer exists, any file deleted between the `getExistingFiles()` call and the `lastModified()` call produced a zero that could silently lower the computed max or inflate `numFiles` (by counting the now-missing file as still present). `LogFile.verify()` then compared the computed `updateTime` against the on-disk record's `updateTime` using the condition `numFiles > 0`, which could fire spuriously when the only remaining file had a zero timestamp — triggering an "unexpected files" classification error that caused `LogAwareFileLister` to fail. The fix filters out non-positive `lastModified` values before computing the max, and changes the guard condition from `numFiles > 0` to `updateTime > 0` so that a record with all files deleted is treated as absent rather than erroneous.

### Before / After
```java
// BEFORE — LogRecord.make()
long lastModified = files.stream().map(File::lastModified).reduce(0L, Long::max);
return new LogRecord(type, absolutePath, lastModified, Math.max(minFiles, files.size()));

// AFTER — LogRecord.make()
List<Long> positiveModifiedTimes = files.stream()
    .map(File::lastModified).filter(lm -> lm > 0).collect(Collectors.toList());
long lastModified = positiveModifiedTimes.stream().reduce(0L, Long::max);
return new LogRecord(type, absolutePath, lastModified, Math.max(minFiles, positiveModifiedTimes.size()));

// BEFORE — LogFile.verify()
if (record.updateTime != record.status.onDiskRecord.updateTime
        && record.status.onDiskRecord.numFiles > 0)

// AFTER — LogFile.verify()
if (record.updateTime != record.status.onDiskRecord.updateTime
        && record.status.onDiskRecord.updateTime > 0)
```

---

## How to Spot
When computing aggregate metadata (max timestamp, file count) over a set of files, guard against TOCTOU races by filtering out entries whose existence cannot be confirmed at aggregation time. `File.lastModified()` returning `0` is the sentinel for "file not found"; treating it as a valid timestamp silently corrupts the aggregate. The companion condition change — checking `updateTime > 0` instead of `numFiles > 0` — is equally important: a count of zero does not distinguish "nothing to verify" from "everything was deleted".

---

## Test
No test added in this commit.
