# Bug 1589: Remove Stale KeyspaceMetrics Field and Import from Keyspace After Merge

**Commit**: cc276fb198 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/Keyspace.java
**Category**: State not cleaned up

---

## Root Cause

A merge conflict resolution left `Keyspace.java` retaining both the `import org.apache.cassandra.metrics.KeyspaceMetrics;` import and a `public final KeyspaceMetrics metric;` field declaration that belonged to a different branch. With the metrics field and its initialization removed in the target branch, the leftover import and field reference caused a compile failure. The fix removes both the unused import and the orphaned field declaration.

### Before / After

```java
// BEFORE — import and field left from wrong merge side
import org.apache.cassandra.metrics.KeyspaceMetrics;
...
public final KeyspaceMetrics metric;
```

```java
// AFTER — both removed
// (import and field declaration deleted entirely)
```

---

## How to Spot

After resolving merge conflicts, run a clean build and check for unused-import warnings and dangling field references. Any `final` field that is declared but never initialized (because its initializer was removed during conflict resolution) will surface as a compile error.

---

## Test

No test added.
