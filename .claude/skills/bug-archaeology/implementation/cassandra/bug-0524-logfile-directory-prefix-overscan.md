# Bug 0524: LogFile.verify() Scans All Data Directories for Every SSTable Record

**Commit**: af963e3a73 · **Ticket**: CASSANDRA-15364
**Subsystem**: src/java/org/apache/cassandra/db/lifecycle/LogFile.java, LogRecord.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

The original `LogFile.verifyRecord()` called `record.withExistingFiles()` which in turn called `getExistingFiles(absolutePath)` per record. Inside `getExistingFiles(Set<String>)`, the filename filter used `Descriptor.fromFilename()` to parse every file name in every unique directory, then re-derived an absolute path to check against the set. This approach was both wasteful (parsing every file in the directory regardless of relevance) and fragile: the prefix comparison used `String.startsWith()`, which could incorrectly match directory `/tmp/aa` as a prefix of `/tmp/aaa/file` because `"/tmp/aaa".startsWith("/tmp/aa")` is true. The same bug affected `Directories.getLocationForDisk()` and `FileUtils.isContained()`.

The fix pre-builds a per-directory `TreeSet` of known filename prefixes and uses `TreeSet.floor(name)` to efficiently find the longest matching prefix, then verifies with an exact `String.startsWith()` check. For `verify()`, files are now batch-collected once across all records before the per-record verification loop, eliminating redundant directory scans.

### Before / After

```java
// BEFORE — string prefix match is incorrect for similar directory names
String folderPath = getCanonicalPath(folder);
String filePath = getCanonicalPath(file);
return filePath.startsWith(folderPath);  // "/tmp/aa" matches "/tmp/aaa/f"
```

```java
// AFTER — use Path.startsWith() which matches path components, not raw strings
Path folderPath = Paths.get(getCanonicalPath(folder));
Path filePath = Paths.get(getCanonicalPath(file));
return filePath.startsWith(folderPath);  // correct component-wise match
```

---

## How to Spot

Any code that tests whether a file path belongs to a directory using `String.startsWith()` is vulnerable to false positives when directory names share a common prefix. The fix is always to use `java.nio.file.Path.startsWith()`, which compares path components rather than raw characters. Search for `getAbsolutePath().startsWith(` or `getCanonicalPath().startsWith(` as indicators of this class of bug.

---

## Test

`FileUtilsTest.testIsContained` asserts that `/tmp/abcd` is not contained in `/tmp/abc`, and `DirectoriesTest.testGetLocationForDisk` verifies that paths `/tmp/a`, `/tmp/aa`, and `/tmp/aaa` are resolved to the correct distinct data directory.
