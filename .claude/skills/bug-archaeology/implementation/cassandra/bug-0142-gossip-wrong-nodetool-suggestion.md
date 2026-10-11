# Bug 0142: Wrong nodetool Command Suggested When Gossip Mode Is Running

**Commit**: fe025c7f79 · **Ticket**: CASSANDRA-19905
**Subsystem**: src/java/org/apache/cassandra/tcm/migration/GossipProcessor.java
**Category**: Wrong constant / default value

---

## Root Cause

When `GossipProcessor.commit()` is invoked while the node is running in legacy gossip mode (i.e., the Cluster Metadata Service has not been initialized), it throws an `IllegalStateException` with instructions to run `nodetool addtocms`. That subcommand was either renamed or was never the correct one; the correct command to initialize the ClusterMetadataService is `nodetool cms initialize`. The error message is the only actionable guidance an operator receives, so pointing to a non-existent command leaves them unable to recover without reading additional documentation.

### Before / After

```java
// BEFORE
throw new IllegalStateException(
    "Can't commit transformations when running in gossip mode. " +
    "Enable the ClusterMetadataService with `nodetool addtocms`.");
```

```java
// AFTER
throw new IllegalStateException(
    "Can't commit transformations when running in gossip mode. " +
    "Enable the ClusterMetadataService with `nodetool cms initialize`.");
```

---

## How to Spot

Hard-coded command-line tool names in error messages and log lines are often left stale when the tool is renamed or restructured. During any CLI refactor, search for string literals that reference old command names in exception messages and comments.

---

## Test

No test added.
