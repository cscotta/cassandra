# Bug 1276: PITR Commitlog Replay Incorrectly Skips Data Due to Stale Truncation Records

**Commit**: 1f6bf3651a · **Ticket**: CASSANDRA-9195
**Subsystem**: db/commitlog/CommitLogReplayer.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

Point-in-time restore (PITR) is meant to recover a table's state as of a specific past timestamp. However, `CommitLogReplayer` computed per-CF replay start positions at construction time by taking the maximum of the commitlog's own position and any truncation position recorded in `SystemKeyspace`. If a table was truncated *after* the PITR target time, the truncation record would push the replay start position past the restore point, causing all mutations in the recovery window to be silently skipped. The fix detects when the recorded truncation timestamp is later than the PITR restore time and clears the truncation record for tables included in the replay set before the position is computed, allowing their full history to be replayed.

### Before / After

```java
// BEFORE
ReplayPosition truncatedAt = SystemKeyspace.getTruncatedPosition(cfs.metadata.cfId);
if (truncatedAt != null)
    rp = replayPositionOrdering.max(Arrays.asList(rp, truncatedAt));
```

```java
// AFTER
ReplayPosition truncatedAt = SystemKeyspace.getTruncatedPosition(cfs.metadata.cfId);
if (truncatedAt != null)
{
    long restoreTime = CommitLog.instance.archiver.restorePointInTime;
    long truncatedTime = SystemKeyspace.getTruncatedAt(cfs.metadata.cfId);
    if (truncatedTime > restoreTime)
    {
        if (replayFilter.includes(cfs.metadata))
        {
            logger.info("Restore point in time is before latest truncation ...");
            SystemKeyspace.removeTruncationRecord(cfs.metadata.cfId);
        }
    }
    else
    {
        rp = replayPositionOrdering.max(Arrays.asList(rp, truncatedAt));
    }
}
```

---

## How to Spot

Any replay or recovery system that adjusts its start position based on persisted metadata (truncation records, checkpoints, watermarks) must validate that the stored metadata was recorded within the intended recovery window. When a PITR target is in play, all metadata that post-dates the target is invalid context for the recovery run. Review every place where a recovery position is advanced by reading from system tables and ask: "could this record have been written after our intended restore point?"

---

## Test

No test added.
