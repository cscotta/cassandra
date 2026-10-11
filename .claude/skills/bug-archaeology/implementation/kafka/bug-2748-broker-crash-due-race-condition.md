**Commit**: 79244fe235 · **Ticket**: KAFKA-13403
**Subsystem**: clients/src
**Category**: Race condition / missing synchronization

**Root cause**
Update the walkFileTree override implementation to handle parallel file deletion. So as to prevent crashing of Kafka broker process when logs are deleted by
other threads due to retention expiry. Reviewers: Luke Chen <showuon@gmail.com>, Divij Vaidya <diviv@amazon.com>, Igor Soarez <soarez@apple.com>

**Before/After**
```java
// Before
                // If the root path did not exist, ignore the error; otherwise throw it.
                if (exc instanceof NoSuchFileException && path.toFile().equals(rootFile))
                    return FileVisitResult.TERMINATE;
                Files.delete(path);
                Files.delete(path);

// After
                if (exc instanceof NoSuchFileException) {
                    if (path.toFile().equals(rootFile)) {
                        // If the root path did not exist, ignore the error and terminate;
                        return FileVisitResult.TERMINATE;
                    } else {
                        // Otherwise, just continue walking as the file might already be deleted by other threads.
                        return FileVisitResult.CONTINUE;
                    }
                }
                Files.deleteIfExists(path);
                Files.deleteIfExists(path);
import java.nio.file.DirectoryStream;
```

**How to spot**
When reviewing clients/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-13403 fix broker crash due to race condition when deleting topics (#11438).

**Test**
Test modified in UtilsTest.java covering the corrected behaviour.
