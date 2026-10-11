# Bug 0621: CommitLogReplayer CDC Completion Passes Wrong Array Index Instead of File

**Commit**: 882c28230e · **Ticket**: CASSANDRA-14066
**Subsystem**: `db/commitlog/CommitLogReplayer.java`
**Category**: Off-by-one

---

## Root Cause

In `CommitLogReplayer.replayFiles()`, the loop counter `i` was incremented with a pre-increment (`i++`) at the top of the loop body, so by the time `handleCDCReplayCompletion(clogs[i])` was reached, `i` pointed one element past the current file. For the last element of the array this caused an `ArrayIndexOutOfBoundsException`. The correct value was already available as the loop variable `file`, which `replayPath()` (the single-file variant) already used correctly. The fix simply passes `file` directly instead of re-indexing into `clogs`.

### Before / After

```java
// BEFORE
int i = 0;
for (File file : filteredLogs)
{
    i++;
    sawCDCMutation = false;
    commitLogReader.readCommitLogSegment(this, file, globalPosition, i == filteredLogs.size());
    if (sawCDCMutation)
        handleCDCReplayCompletion(clogs[i]);  // i already past current file
}
```

```java
// AFTER
    if (sawCDCMutation)
        handleCDCReplayCompletion(file);  // use loop variable directly
```

---

## How to Spot

Whenever a for-each loop maintains a manual integer counter that is incremented at the start of the body, any subsequent use of that counter as an array index is off by one relative to the iterator position. Reviewers should prefer using the loop variable itself rather than a separately maintained index, or use a traditional indexed for-loop to keep them in sync.

---

## Test

No test added.
