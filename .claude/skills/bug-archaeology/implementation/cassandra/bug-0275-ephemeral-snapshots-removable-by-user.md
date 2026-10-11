# Bug 0275: Ephemeral Snapshots Removable by User via nodetool

**Commit**: 1e2b608213 · **Ticket**: CASSANDRA-17757
**Subsystem**: service/snapshot
**Category**: Logic error in condition

---

## Root Cause

The `clearSnapshot` code path called `Keyspace.clearSnapshot()`, which delegated directly to
`Directories.clearSnapshot()` using only a raw string tag match.  It had no knowledge of the
`TableSnapshot` metadata, so it could not check the `isEphemeral()` flag.  As a result, a user
or operator invoking `nodetool clearsnapshot` (or the equivalent JMX call) could delete ephemeral
snapshots that are owned by the system and should only be removed by Cassandra itself on restart.

### Before / After

```java
// BEFORE – in SnapshotVerbHandler, no ephemeral check
Keyspace.clearSnapshot(command.snapshot_name, command.keyspace);
```

```java
// AFTER – route through StorageService which loads TableSnapshot metadata and filters
StorageService.instance.clearSnapshot(command.snapshot_name, command.keyspace);

// new predicate in TableSnapshot
public static Predicate<TableSnapshot> shouldClearSnapshot(String tag)
{
    return ts -> {
        boolean clearAll = tag == null || tag.isEmpty();
        if (!clearAll && ts.isEphemeral())
            logger.info("Skipping deletion of ephemeral snapshot '{}' ...", tag, ts.keyspaceName);
        return !ts.isEphemeral() && (clearAll || ts.tag.equals(tag));
    };
}
```

---

## How to Spot

When a system introduces a new property on an entity (here, the `ephemeral` flag on snapshots)
every deletion path must be audited to ensure it consults that property.  The old path bypassed
the `TableSnapshot` abstraction entirely and operated on raw filesystem paths, making it
impossible to enforce any snapshot-level policy.  The review heuristic: if a new flag gates a
behavioral constraint, grep for every delete/clear call site and confirm each one loads the
full object before acting.

---

## Test

`EphemeralSnapshotTest#testEphemeralSnapshotIsNotClearableFromNodetool` verifies that
`nodetool clearsnapshot -t <name>` does not remove an ephemeral snapshot, and that an
appropriate log message is emitted.  A second test,
`testClearingAllSnapshotsFromNodetoolWillKeepEphemeralSnaphotsIntact`, confirms that
`clearsnapshot --all` also leaves ephemeral snapshots intact while removing non-ephemeral ones.
