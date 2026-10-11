# Bug 2250: CLI Crashes with AssumptionError When No Keyspace Is Selected

**Commit**: 98708d817f · **Ticket**: CASSANDRA-4322
**Subsystem**: src/java/org/apache/cassandra/cli/CliClient.java
**Category**: Missing null / bounds check

---

## Root Cause

Two code paths in the Cassandra CLI assumed `keySpace` was non-null without checking. First, `executeAlterKeySpace` refreshed the keyspace metadata using a local variable `keySpace` (a method parameter) but then called `getKSMetaData(keySpace)` where `keySpace` referred to the instance field, which could be a different (or null) value. Second, `executeDescribeKeySpace` unconditionally called `getKSMetaData(keySpace)` even when no keyspace had been selected and no argument was provided — causing an assumption/assertion error at runtime.

Additionally, `replayAssumptions` called `hasKeySpace()` which printed an error message to stdout even in normal non-interactive flows where the absence of a keyspace is expected; this was changed to a silent check via `hasKeySpace(false)`.

### Before / After

```java
// BEFORE (executeAlterKeySpace)
keyspacesMap.remove(keySpace);          // uses field, may differ from param
getKSMetaData(keySpace);                // wrong variable

// BEFORE (executeDescribeKeySpace)
keyspacesMap.remove(keySpace);          // NPE if keySpace is null
KsDef currentKeySpace = getKSMetaData(keySpace);
```

```java
// AFTER (executeAlterKeySpace)
keyspacesMap.remove(keyspaceName);      // use the local param consistently
getKSMetaData(keyspaceName);

// AFTER (executeDescribeKeySpace)
if (keySpace == null && argCount == 0) {
    sessionState.out.println("Authenticate to a Keyspace first...");
    return;
}
KsDef currentKeySpace = null;
if (keySpace != null) {
    keyspacesMap.remove(keySpace);
    currentKeySpace = getKSMetaData(keySpace);
}
```

---

## How to Spot

When a method has a local variable that shadows or parallels an instance field of the same semantic type (e.g., a `keyspaceName` parameter alongside a `this.keySpace` field), verify every usage site uses the intended one. Also check that any code path that operates on a potentially-null field has a null guard before use.

---

## Test

No test added.
