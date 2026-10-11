# Bug 2201: Unresolved Merge Conflict Left Dead Imports in RowIteratorFactory

**Commit**: 565c57684b · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/RowIteratorFactory.java
**Category**: State not cleaned up

---

## Root Cause

A three-way merge between `HEAD` and `cassandra-1.1` left raw conflict markers (`<<<<<<< HEAD`, `|||||||`, `=======`, `>>>>>>>`) embedded in `RowIteratorFactory.java`. The conflict was in the import block: the `HEAD` side had `OnDiskAtomIterator` while the 1.1 side had `IColumnIterator` plus two additional imports. The markers were committed as literal source, making the file unparseable and breaking the build until this follow-up commit removed the conflict artifacts and retained the correct imports.

### Before / After

```java
// BEFORE (unresolved conflict markers in source)
<<<<<<< HEAD
import org.apache.cassandra.db.columniterator.OnDiskAtomIterator;
||||||| merged common ancestors
import org.apache.cassandra.db.columniterator.IColumnIterator;
=======
import org.apache.cassandra.db.columniterator.IColumnIterator;
import org.apache.cassandra.db.columniterator.IColumnIteratorFactory;
import org.apache.cassandra.db.columniterator.LazyColumnIterator;
>>>>>>> cassandra-1.1
```

```java
// AFTER
import org.apache.cassandra.db.columniterator.IColumnIteratorFactory;
import org.apache.cassandra.db.columniterator.LazyColumnIterator;
import org.apache.cassandra.db.columniterator.OnDiskAtomIterator;
```

---

## How to Spot

After any branch merge, scan all modified files for conflict marker strings (`<<<<<<<`, `=======`, `>>>>>>>`) before committing. CI can enforce this with a pre-commit hook or a build step that fails on these patterns.

---

## Test

No test added.
