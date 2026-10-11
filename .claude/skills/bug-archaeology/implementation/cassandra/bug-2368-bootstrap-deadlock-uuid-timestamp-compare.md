# Bug 2368: Bootstrap deadlock because schema UUID compared by identity, not by timestamp

**Commit**: 5434ac4a06 · **Ticket**: CASSANDRA-4159

## Detection Rule

When comparing two `UUID` values for "same schema version" equality in a readiness gate, verify that the comparison uses the UUID's embedded timestamp (`uuid.timestamp()`) rather than `compareTo` or `equals`, which compare all 128 bits including the random node component.

## Code Smell

`highestKnown.compareTo(Schema.instance.getVersion()) == 0` compares two `TimeUUID` values byte-by-byte; two separately generated UUIDs that represent logically the same schema version will have different node/sequence bits and will never compare equal, keeping `isReadyForBootstrap()` perpetually false and causing the bootstrap thread to wait forever.

## Tags

- scope: cassandra
- category: logic-error
- code-pattern: uuid-compared-by-full-value-not-timestamp
- severity: hang
- subsystem: bootstrap-startup

## Root Cause

`MigrationManager.isReadyForBootstrap()` gates bootstrap on whether the highest-seen schema UUID equals the local schema UUID. Two `TimeUUID` values can carry the same timestamp (i.e., same schema generation) but differ in their clock-sequence and node fields if generated on different nodes or at sub-millisecond intervals. Using `compareTo` requires an exact 128-bit match that never occurs for cross-node UUIDs, so the bootstrap node waits indefinitely. The fix compares only the time component via `uuid.timestamp()`.

## Example

```java
// BEFORE
public static boolean isReadyForBootstrap() {
    return highestKnown.compareTo(Schema.instance.getVersion()) == 0;
    // two TimeUUIDs from different nodes never compare equal even at same timestamp
}
```

```java
// AFTER
public static boolean isReadyForBootstrap() {
    return highestKnown.timestamp() == Schema.instance.getVersion().timestamp();
}
```
