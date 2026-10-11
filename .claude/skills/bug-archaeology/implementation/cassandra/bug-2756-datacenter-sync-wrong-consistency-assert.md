# Bug 2756: Wrong Consistency Level Asserted in DatacenterSyncWriteResponseHandler

**Commit**: bcb2c24122 · **Ticket**: CASSANDRA-2254

## Detection Rule
When a response handler is named for a specific consistency level (e.g., `DatacenterSyncWriteResponseHandler`), ensure the assertion inside its constructor checks the correct `ConsistencyLevel` enum value.

## Code Smell
`assert consistencyLevel == ConsistencyLevel.LOCAL_QUORUM` in `DatacenterSyncWriteResponseHandler` fires whenever `EACH_QUORUM` is used, which is exactly the consistency level this handler is designed for.

## Tags
- scope: cassandra
- category: wrong-constant
- code-pattern: wrong-enum-in-assertion
- severity: crash
- subsystem: network-messaging

## Root Cause
`DatacenterSyncWriteResponseHandler` handles `EACH_QUORUM` consistency but its constructor asserted `ConsistencyLevel.LOCAL_QUORUM`, causing an `AssertionError` for any `EACH_QUORUM` write when assertions were enabled. The fix changes the asserted constant to `ConsistencyLevel.EACH_QUORUM`.

## Example
```java
// BEFORE
assert consistencyLevel == ConsistencyLevel.LOCAL_QUORUM;
```
```java
// AFTER
assert consistencyLevel == ConsistencyLevel.EACH_QUORUM;
```
