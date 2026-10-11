# Bug 1587: Revert KeyspaceMetrics to Correct Branch-Compatible Version

**Commit**: 593bba94f6 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/metrics/KeyspaceMetrics.java
**Category**: Wrong constant / default value

---

## Root Cause

A prior commit accidentally applied a patch intended for a newer branch (which uses the renamed `Keyspace` class and expanded metric set) to an older branch that still uses `Table`. The file referenced `Keyspace` instead of `Table`, exposed metrics that did not yet exist on `ColumnFamilyStore.metric` in that branch (`allMemtablesDataSize`, `pendingCompactions`), and contained a `release()` method that failed to unregister `MemtableColumnsCount`. This commit reverts `KeyspaceMetrics` to the version compatible with the `Table`-based branch.

### Before / After

```java
// BEFORE (wrong version imported from newer branch)
public KeyspaceMetrics(final Keyspace ks) { ... }
return Keyspace.switchLock.getQueueLength();
```

```java
// AFTER (correct version for this branch)
public KeyspaceMetrics(final Table ks) { ... }
return Table.switchLock.getQueueLength();
```

---

## How to Spot

When cherry-picking or merging across branches that have undergone class renames, verify that every import and type reference in the patch corresponds to the target branch's class hierarchy. A quick `javac` error — or an `import` referencing a class not present in the branch — is an immediate signal of a mis-targeted patch.

---

## Test

No test added.
