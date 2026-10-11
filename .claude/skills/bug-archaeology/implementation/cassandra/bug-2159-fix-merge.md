# Bug 2159: Merge Conflict Artifacts Left in MigrationManager Imports

**Commit**: b4f369301a · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/service/MigrationManager.java
**Category**: Logic error in condition

---

## Root Cause

A previous merge of the `cassandra-1.1` branch into `trunk` left unresolved three-way merge conflict markers (`<<<<<<<`, `|||||||`, `>>>>>>>`) inside the import section of `MigrationManager.java`. The conflict block included a spurious `Callable` import and duplicated `ArrayList`/`Collection`/`UUID` imports that were already covered by the existing wildcard or specific imports. The fix removes the conflict markers and redundant imports, leaving only the correct, minimal set needed by the file.

### Before / After

```java
// BEFORE — conflict markers survived the merge
<<<<<<< HEAD
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
||||||| merged common ancestors
import java.util.concurrent.Callable;
=======
>>>>>>> cassandra-1.1
import java.util.concurrent.ExecutionException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
```

```java
// AFTER — clean imports
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
```

---

## How to Spot

After a merge, run `grep -r '<<<<<<\|>>>>>>>\|||||' src/` to catch any conflict markers that escaped review. CI builds should fail on such markers, but if merge tooling resolves conflicts automatically without building, markers can slip through.

---

## Test

No test added.
