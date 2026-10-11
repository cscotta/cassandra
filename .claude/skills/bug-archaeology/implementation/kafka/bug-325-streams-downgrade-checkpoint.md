## Commit
599d55cdb7

## Ticket
KAFKA-19710

## Subsystem
Kafka Streams / State Store / Downgrade

## Category
State not cleaned up

## Root cause
Kafka Streams 4.3 migrated offsets from `.checkpoint` files to RocksDB column families. Downgrading from 4.3 back to an earlier version left the `.checkpoint` files absent, causing the older version to treat the task as corrupted. `LegacyCheckpointingStateStore.maybeDowngradeOffsets()` was added: when `upgrade.from` is set to a version prior to 4.3, it writes the current offsets back to the legacy `.checkpoint` file during shutdown.

## Before
```java
// No downgrade path: on rollback, .checkpoint absent -> TaskCorruptedException
```

## After
```java
// During shutdown, if upgrade.from < 4.3:
public void maybeDowngradeOffsets() {
    if (needsLegacyCheckpoint()) {
        checkpointFile.write(currentOffsets);
    }
}
```

## How to spot
Any migration that changes the checkpoint format must also provide a downgrade path. Add an integration test that starts on the new version, then restarts on the old version and verifies tasks resume cleanly.

## Test
`LegacyCheckpointingStateStoreTest` and `EosIntegrationTest` verify the downgrade path writes the checkpoint file.
