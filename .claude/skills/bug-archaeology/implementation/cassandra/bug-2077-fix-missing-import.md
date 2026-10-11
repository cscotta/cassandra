# Bug 2077: Missing FBUtilities Import in CommitLogSegment Prevents Compilation

**Commit**: 1fc218ff78 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/commitlog/CommitLogSegment.java
**Category**: Logic error in condition

---

## Root Cause

A refactoring or merge introduced a call to `FBUtilities` in `CommitLogSegment.java` without adding the corresponding import statement. Because `FBUtilities` is in a different package (`org.apache.cassandra.utils`), the class failed to compile, blocking any build that included the commit log segment code.

### Before / After

```java
// BEFORE — missing import
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Checksum;

import org.slf4j.Logger;
```

```java
// AFTER — import added
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Checksum;

import org.apache.cassandra.utils.FBUtilities;
import org.slf4j.Logger;
```

---

## How to Spot

After any merge or cherry-pick, confirm that every class referenced in the file has a corresponding import. IDEs catch this instantly, but patch-only reviews need to check that newly introduced class names are either in `java.lang` or already imported.

---

## Test

No test added.
