# Bug 0978: Missing Imports After Merge in StorageService

**Commit**: dba2e16c95 · **Ticket**: n/a
**Subsystem**: Storage service / build
**Category**: Logic error in condition

---

## Root Cause
A prior merge of StorageService.java dropped two import statements (`org.apache.cassandra.db.Directories` and `org.apache.cassandra.db.PartitionPosition`) that were required for code introduced in the merged branch. Without these imports the class would fail to compile, breaking the entire server build. The fix re-adds the two missing import lines; no runtime logic was changed.

### Before / After
```java
// BEFORE – missing imports caused compile failure
import org.apache.cassandra.db.Keyspace;

// AFTER – imports restored
import org.apache.cassandra.db.Directories;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.db.PartitionPosition;
```

---

## How to Spot
After a merge of long-lived branches, always compile the result before committing. Import sections in large classes like `StorageService` are a common victim of merge conflicts that get resolved by accidentally dropping lines. A clean `git diff` on imports after resolving conflicts is a quick safety check.

---

## Test
No test added.
