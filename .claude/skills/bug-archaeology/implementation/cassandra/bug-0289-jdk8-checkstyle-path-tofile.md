# Bug 0289: JDK8 Checkstyle Failure from Path.toFile().getCanonicalPath() in StartupChecks

**Commit**: f4f04fbac0 · **Ticket**: CASSANDRA-17777
**Subsystem**: service / startup
**Category**: Wrong constant / default value

---

## Root Cause

`StartupChecks` used `dir.toFile().getCanonicalPath()` (calling `.toFile()` on a `java.nio.file.Path`) inside a `FileVisitor`, which violated the project's checkstyle rules banning direct `Path.toFile()` calls on JDK8. The build failed checkstyle on JDK8 even though the logic was correct. Additionally, `getCanonicalPath()` was called multiple times redundantly; the fix replaced all occurrences with a single call to `FileUtils.getCanonicalPath(new File(dir))` and cached the result in a local variable.

### Before / After

```java
// BEFORE
String[] nameParts = dir.toFile().getCanonicalPath().split(java.io.File.separator);
// ...
if (dir.toFile().getCanonicalPath().contains("snapshot"))
```

```java
// AFTER
String[] nameParts = FileUtils.getCanonicalPath(new File(dir)).split(java.io.File.separator);
// ...
String canonicalPath = FileUtils.getCanonicalPath(new File(dir));
if (canonicalPath.contains("snapshot"))
```

---

## How to Spot

Whenever new code introduces `Path.toFile()` in a project that enforces a `Path`-only API via checkstyle, the build will fail on the stricter JDK. The pattern to catch during review: search for `.toFile()` calls in any file that is part of the enforced path. Beyond checkstyle, calling `.toFile().getCanonicalPath()` more than once inside a loop or visitor body is a performance smell; caching the result also makes the code easier to read.

---

## Test

No new test added; the change is a build-correctness fix caught by the checkstyle gate itself.
