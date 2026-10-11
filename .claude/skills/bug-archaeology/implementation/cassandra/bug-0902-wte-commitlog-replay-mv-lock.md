# Bug 0902: WriteTimeoutException During CommitLog Replay Due to MV Lock

**Commit**: 5794fb3556 · **Ticket**: CASSANDRA-11891
**Subsystem**: CommitLog / Materialized Views
**Category**: Logic error in condition

---

## Root Cause
When applying mutations during commitlog replay, `Keyspace.apply()` attempted to acquire the per-partition MV lock with a bounded timeout. If the lock was not available, the code checked whether the mutation's wall-clock age exceeded `WriteRpcTimeout` and, if so, threw a `WriteTimeoutException`. During replay, mutation timestamps are historical — they can be arbitrarily old — so this age check always fired immediately for any replayed mutation that found the lock contended, causing replay to fail with a spurious WTE. The fix adds an `!isClReplay` guard so the timeout check is simply skipped when applying from the commit log.

### Before / After
```java
// BEFORE — WTE thrown for any old mutation during replay
if ((System.currentTimeMillis() - mutation.createdAt) > DatabaseDescriptor.getWriteRpcTimeout())
{
    throw new WriteTimeoutException(...);
}

// AFTER — guard against replay path
if (!isClReplay && (System.currentTimeMillis() - mutation.createdAt) > DatabaseDescriptor.getWriteRpcTimeout())
{
    throw new WriteTimeoutException(...);
}
```

The retry lambda was also simplified from a duplicated `writeCommitLog` branch to a single `apply(mutation, writeCommitLog, true, isClReplay, mark)` call.

---

## How to Spot
Any timeout or staleness check that uses `mutation.createdAt` (or any other original write timestamp) must be gated on whether the current execution context is a replay. During replay, mutations are applied from historical commit log segments and their timestamps have no relation to the current wall clock. Review all paths in `Keyspace.apply()` that compute elapsed time from `mutation.createdAt` and ensure each is protected by an `!isClReplay` guard.

---

## Test
No test added.
