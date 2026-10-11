## Commit
53b1a77b59

## Ticket
KAFKA-19713

## Subsystem
Kafka Streams / RocksDB State Stores

## Category
State not cleaned up

## Root cause
After migrating offsets from `.checkpoint` files to a RocksDB offsets column family (KAFKA-20212), `ProcessorStateManager` still wrote positions to a `.position` file instead of reading/writing them from the offsets CF. On restart, positions were not found in the CF and defaulted to the beginning of the changelog, triggering unnecessary full restores.

## Before
```java
// position written to positionCheckpoint file
positionCheckpoint.write(positions);
// on restart: not found in CF -> default to start
```

## After
```java
// position stored in offsetColumnFamilyHandle under positionKey
rocksDB.put(offsetColumnFamilyHandle, positionKey, encode(position));
// on restart: read from CF
byte[] raw = rocksDB.get(offsetColumnFamilyHandle, positionKey);
```

## How to spot
After any checkpoint-format migration, grep for remaining uses of the old checkpoint file path (`positionCheckpoint`, `.position`). Any write to the old path is a regression.

## Test
`RocksDBStoreTest` verifies position survives a close/reopen cycle using the offsets CF.
