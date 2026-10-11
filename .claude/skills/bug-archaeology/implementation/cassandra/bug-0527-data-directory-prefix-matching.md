# Bug 0527: Data Directory Prefix Matching Uses String.startsWith on Raw Paths

**Commit**: eda5db28e3 · **Ticket**: CASSANDRA-13974
**Subsystem**: src/java/org/apache/cassandra/db/Directories.java, io/util/FileUtils.java
**Category**: Logic error in condition

---

## Root Cause

`Directories.getLocationForDisk()` checked whether a data directory path was a prefix of a candidate path using `dir.getAbsolutePath().startsWith(dataDirectory.location.getAbsolutePath())`. Because `String.startsWith()` operates on raw characters, a directory named `/data/aa` would incorrectly match a path under `/data/aaa/`, since the string `/data/aaa/ks/table` does start with the string `/data/aa`. Similarly, `FileUtils.isContained()` used string concatenation of canonical paths with the same false-prefix vulnerability. In environments where data directories have names that are prefixes of each other, both methods would return the wrong directory.

The fix replaces `String.startsWith()` with `java.nio.file.Path.startsWith()`, which performs component-wise comparison and is immune to the string-prefix ambiguity.

### Before / After

```java
// BEFORE — raw string prefix: "/tmp/aa".startsWith("/tmp/a") == true (wrong)
if (dir.getAbsolutePath().startsWith(dataDirectory.location.getAbsolutePath()))
    return dir;
```

```java
// AFTER — component-wise prefix using Path API
Path dirPath = Paths.get(dir.getAbsolutePath());
Path locationPath = Paths.get(dataDirectory.location.getAbsolutePath());
if (dirPath.startsWith(locationPath))
    return dir;
```

---

## How to Spot

File path containment checks written as `pathString.startsWith(prefixString)` are always incorrect unless a path separator is appended to the prefix, because `/foo/bar` and `/foo/barbaz` share the literal prefix `/foo/bar`. Code review should flag any `getAbsolutePath().startsWith(` or `getCanonicalPath().startsWith(` call — these should be replaced with `java.nio.file.Path.startsWith()`.

---

## Test

`FileUtilsTest.testIsContained` explicitly asserts that `/tmp/abcd` is not contained in `/tmp/abc`, catching the string-prefix false-positive. `DirectoriesTest.testGetLocationForDisk` uses paths `/tmp/a`, `/tmp/aa`, `/tmp/aaa` to verify each resolves to the correct distinct data directory, and `testGetLocationWithSymlinks` covers symbolic link scenarios.
