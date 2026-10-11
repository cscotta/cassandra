# Bug 2683: Counter increments lost after restart

**Commit**: 8ddb32cc24 · **Ticket**: CASSANDRA-2642

## Detection Rule
When deserializing a `RowMutation` from the commitlog during replay, pass `fromRemote=false` so that counter columns are treated as local (not pre-incremented remote) values and are replayed correctly.

## Code Smell
`ColumnFamily.serializer().deserialize(dis, true, true)` hardcoded with `fromRemote=true` in the commitlog replay path, causing counter columns to be deserialized as already-applied remote increments and discarded or misapplied on replay.

## Tags
- scope: cassandra
- category: wrong-constant
- code-pattern: hardcoded-remote-flag-replay
- severity: data-loss
- subsystem: commitlog-cdc

## Root Cause
The commitlog replay code unconditionally passed `fromRemote=true` when deserializing `ColumnFamily` instances. For counter column families this flag controls whether the values are interpreted as remote-already-applied increments (which should not be re-applied) or as local delta values (which should). Replaying with `fromRemote=true` discarded counter increments rather than reapplying them after a restart. The fix adds a `fromRemote` parameter to the deserializer and passes `false` from the commitlog replay path.

## Example
```java
// BEFORE (always fromRemote=true)
ColumnFamily cf = ColumnFamily.serializer().deserialize(dis, true, true);
```
```java
// AFTER (fromRemote controlled by caller)
ColumnFamily cf = ColumnFamily.serializer().deserialize(dis, true, fromRemote);
// commitlog calls: deserialize(dis, version, false)
```
